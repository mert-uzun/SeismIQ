import math
import requests
from bs4 import BeautifulSoup
from datetime import datetime, timezone, timedelta
import boto3
import os
import pandas as pd
import numpy as np
from sklearn.neighbors import BallTree
from shapely.geometry import Point
from shapely.strtree import STRtree
import geopandas as gpd
import json

WORLD_RADIUS_KM = 6371
LOCAL_GEOJSON_PATH = "/tmp/land.geojson"

earthquakes_table = boto3.resource("dynamodb").Table(os.getenv("EARTHQUAKES_TABLE_NAME"))
last_earthquake_date_table = boto3.resource("dynamodb").Table(os.getenv("LAST_EARTHQUAKE_DATE_TABLE_NAME"))

cities_data = None
ball_tree = None
_land_index = None

def _get_last_earthquake_date(table):
    response = table.get_item(Key={"tracker_id": "last_earthquake_date"})
    last_earthquake_date = response.get("Item", {}).get("value", None)

    if last_earthquake_date:
        last_earthquake_date = datetime.strptime(last_earthquake_date, "%Y.%m.%d")
        return last_earthquake_date
    
    return None

def _update_last_earthquake_date(date):
    last_earthquake_date_table.put_item(Item={"tracker_id": "last_earthquake_date", "value": date})

def load_settlement_data():
    global cities_data, ball_tree

    if not cities_data and not ball_tree:
        return

    try: 
        # Load data from S3
        bucket_name = os.getenv("GEO_BUCKET")
        key_name = os.getenv("GEO_KEY")

        cities_data = pd.read_parquet(f"s3://{bucket_name}/{key_name}")

        # Force conversion to numeric types just to be safe
        cities_data["latitude"] = pd.to_numeric(cities_data["latitude"], errors="coerce")
        cities_data["longitude"] = pd.to_numeric(cities_data["longitude"], errors="coerce")
        
        # Create Ball Tree
        coords_rad = np.radians(cities_data[["latitude", "longitude"]].values)
        ball_tree = BallTree(coords_rad, metric="haversine")


    except Exception as e:
        print(f"Error loading settlement data: {e}")
        cities_data = pd.DataFrame()
        ball_tree = None
        
def _distance_to_nearest_settlement(lat, lon) -> tuple[float, str]:
    global ball_tree, cities_data, WORLD_RADIUS_KM

    coord_rad = np.radians([[lat, lon]])
    distance_rad, city_index = ball_tree.query(coord_rad, k=1) # k = 1 because we only want the nearest city in this instance # NOTE: this will return the values in a 2D array format

    distance_km = distance_rad[0][0] * WORLD_RADIUS_KM
    city_name = cities_data.iloc[city_index[0][0]]["name"]

    return float(distance_km), str(city_name)

def _ensure_land_index():
    global _land_index

    if _land_index:
        return _land_index
    
    if not os.path.exists(LOCAL_GEOJSON_PATH):
        boto3.resource("s3").Bucket(os.getenv("GEOJSON_BUCKET")).download_file(os.getenv("GEOJSON_KEY"), LOCAL_GEOJSON_PATH)
    
    gdf = gpd.read_file(LOCAL_GEOJSON_PATH)
    if gdf.crs is None or str(gdf.crs).upper() not in ("EPSG:4326", "WGS84"):
        gdf = gdf.to_crs(epsg=4326)
    
    geoms = list(gdf.geometry)
    _land_index = STRtree(geoms)

    return _land_index

def _is_offshore(lat: float, lon: float) -> bool:
    p = Point(lon, lat)

    candidates = _ensure_land_index().query(p)

    for candidate in candidates:
        if candidate.contains(p):
            return False
    return True

def _beta_and_a7(M: float) -> tuple:
    """
    Compute the magnitude-dependent distance attenuation coefficient β(M)
    based on Akkar & Cagnan (2010) GMPE (PGA coefficients).

    β(M) = -(a5 + a6 * (M - c1)) * ln(10)

    - c1, a5, a6, a7 are regression coefficients from AC10 GMPE.
    - β(M) represents the magnitude-scaling slope of the logarithmic
      distance attenuation term in AC10.
    - a7 is the near-source saturation distance parameter (km), used
      in R* = sqrt(Rv^2 + a7^2).
    """

    general_data_bucket = os.getenv("GENERAL_DATA_BUCKET")
    ac10_pga_v1_key = os.getenv("AC10_PGA_V1_KEY")

    response = boto3.resource("s3").Object(general_data_bucket, ac10_pga_v1_key).get()
    ac10_pga_v1 = json.loads(response["Body"].read().decode("utf-8"))

    c1 = ac10_pga_v1.get("c1", None)
    a5 = ac10_pga_v1.get("a5", None)
    a6 = ac10_pga_v1.get("a6", None)
    a7 = ac10_pga_v1.get("a7", None)


    beta = -(a5 + a6 * (M - c1)) * math.log(10)

    return beta, a7

def _determine_earthquake_Svalue_and_ttl(quake: dict) -> float:
    """
    --- Operational intensity proxy ("S" score) ---
    S is an operational earthquake impact score, derived from the
    Akkar & Cagnan (2010, AC10) GMPE attenuation form.
    
    - M: Moment magnitude if available (Mw), else local magnitude (ML).
    - β(M): magnitude-dependent distance attenuation coefficient
            from AC10 GMPE (PGA coefficients).
    - Rv = sqrt(D^2 + h^2): hypocentral distance (km).
    - R* = sqrt(Rv^2 + a7^2): effective distance with near-source
            saturation (a7 ≈ 7.3 km from AC10).
    - O: offshore reduction factor (1.0 onshore, <1 offshore).
    
    Formula:
      S = ( M - β(M) * log10(R* + 1) ) * O
    
    Scientific context:
      - β(M) term: "magnitude-dependent distance attenuation slope"
        (from AC10 GMPE distance attenuation coefficient).
      - log10(R*): "distance scaling term with near-source saturation".
      - O: simple heuristic modifier for offshore vs onshore.
    
    TTL (time-to-live) is then derived from S using a logistic survival
    function, mapping operational intensity (S) to information lifetime.
    """

    dt = datetime.strptime(quake.get('date', "") + " " + quake.get('time', ""), "%Y.%m.%d %H:%M:%S")
    lat = float(quake.get('latitude', None))
    lon = float(quake.get('longitude', None))
    h = float(quake.get('depth_km', None))
    ML = float(quake.get('ML', None))
    Mw = float(quake.get('Mw', None))

    M = Mw if Mw else ML
    beta, a7 = _beta_and_a7(M)
    D, nearest_settlement_name = _distance_to_nearest_settlement(lat, lon)
    Rv = math.sqrt(D*D + h*h)
    Rstar = math.sqrt(Rv*Rv + a7*a7)
    O = .85 if _is_offshore(lat, lon) else 1 # TODO: Instead of magic number .85 we can come up with a continuous function of distance from the nearest big settlement

    S = (M - beta * math.log10(Rstar + 1)) * O

    # TTL values in minutes
    TTLmin = 30
    TTLmax = 10080
    S50 = 3.202 # S value in the middle of the graph, heuristic value for now, TODO: determine it later once we have enough data
    k = 2.4 # Steepnes of the curve around S50, heuristic value for now, TODO: determine it later once we have enough data
    TTL = TTLmin + (TTLmax - TTLmin) / (1 + math.pow(math.e, -k * (S - S50)))
    TTLfinal = max(TTLmax, TTL)

    # Datetime from Turkey's timezone to UTC for epoch
    dt = dt.replace(tzinfo=timezone(timedelta(hours=3)))

    quake_datetime_as_epoch = int(dt.timestamp())

    return S, quake_datetime_as_epoch + (TTLfinal * 60) # Return the value in seconds because we are using epoch time value

def calculate_earthquake_danger_radius(quake: dict) -> float:
    """ 
    --- Scientific Rationale for S_threshold Selection ---
    The S_threshold value defines the level at which an earthquake is considered
    to have "damage potential". This threshold is designed to be more sensitive
    than the threshold for "fatal" potential (typically Mw > 6.0, S > 4.0),
    in order to capture lower magnitude events (in the Mw 5.0-5.5 range)
    that can still cause structural damage.
    
    Determination of the Threshold Value:
    The value was determined by testing plausible, high-damage-potential scenarios
    using the S-score formula, which is based on the Akkar & Cagnan (2010) GMPE model.
    These scenarios model earthquakes that are shallow and close to populated areas.
    
    Sample Calculations:
    - Scenario 1 (Mw 5.0): A shallow (h=10km) and nearby (D=15km) earthquake yields S ≈ 3.5
    - Scenario 2 (Mw 5.5): Under the same conditions (h=10km, D=15km), S ≈ 4.0
    - Scenario 3 (Mw 5.5): At a moderate depth/distance (h=15km, D=25km), S ≈ 3.8

    The Mw 5.0-5.5 range was chosen to calibrate the S_threshold against a recognized
    benchmark for the onset of structural damage. This benchmark corresponds to Level VI 
    on the Modified Mercalli Intensity (MMI) scale, where light damage to structures is 
    first observed. According to empirical data from seismological bodies like the USGS, 
    shallow earthquakes within the Mw 5.0-5.5 magnitude range are precisely those capable 
    of generating MMI VI shaking near the epicenter. Anchoring our calculations to this range
    ensures our danger radius is based on a standard engineering and seismological damage 
    threshold, rather than an arbitrary magnitude figure.

    Formulas used to calculate the danger radius are:
        S_thresh = (M - beta * log10(Rstar + 1)) * O

        Solve for Rstar:
        log10(Rstar + 1) = (M - (S_thresh / O)) / beta
        Rstar + 1 = 10 ** ((M - (S_thresh / O)) / beta)
        Rstar = (10 ** ((M - (S_thresh / O)) / beta)) - 1

        Now, remember that Rstar depends on D:
        Rstar = sqrt(Rv*Rv + a7*a7) and Rv = sqrt(D*D + h*h)
        So, Rstar**2 = D**2 + h**2 + a7**2
        
        Solve for D:
        D**2 = Rstar**2 - h**2 - a7**2
        D = sqrt(Rstar**2 - h**2 - a7**2)

        This final D is the radius in kilometers from the epicenter where the shaking intensity 
        drops below your defined danger level.
    
    Conclusion:
    The calculations show that S-scores for earthquakes with damage potential
    are concentrated in the 3.5 to 4.0 range. The chosen threshold of 3.7
    represents a balanced midpoint in this range. This value is low enough to
    capture significant Mw 5.0+ events but high enough to filter out smaller tremors
    """

    lat = float(quake.get('latitude', None))
    lon = float(quake.get('longitude', None))
    h = float(quake.get('depth_km', None))
    M = float(quake.get('Mw', None)) if quake.get('Mw', None) else float(quake.get('ML', None))
    beta, a7 = _beta_and_a7(M)
    O = .85 if _is_offshore(lat, lon) else 1

    S_thresh = 3.7

    try:
        Rstar = (10 ** ((M - (S_thresh / O)) / beta)) - 1
        D = math.sqrt(Rstar**2 - h**2 - a7**2)
        return D
    except (ValueError, OverflowError, ZeroDivisionError) as e:
        print(f"Error calculating the danger radius: {e}")
        return 0.0

def find_settlements_within_danger_radius(quake: dict) -> list[str]:
    global cities_data, WORLD_RADIUS_KM

    if cities_data is None:
        load_settlement_data()

    quake_lat = float(quake.get('latitude', None))
    quake_lon = float(quake.get('longitude', None))
    danger_radius_in_radians = calculate_earthquake_danger_radius(quake) / WORLD_RADIUS_KM # Because ball tree queries expect radians both as coordinates and radius

    if danger_radius_in_radians == 0.0:
        return []

    near_settlements_data = cities_data[cities_data["feature_code"].isin(["PPLA", "PPLA2", "PPLC"])]
    near_settlements_data = near_settlements_data[near_settlements_data["country_code"] == "TR"]
    near_settlements_data["latitude"] = pd.to_numeric(near_settlements_data["latitude"], errors="coerce")
    near_settlements_data["longitude"] = pd.to_numeric(near_settlements_data["longitude"], errors="coerce")

    near_settlements_data = near_settlements_data.dropna(subset=["latitude", "longitude"])
    
    # Reset indexes to avoid issues with indexes mismatching between the dataframe and ball tree
    near_settlements_data = near_settlements_data.reset_index(drop=True)

    # Create a ball tree for the filtered settlements
    near_settlements_coords_rad = np.radians(near_settlements_data[["latitude", "longitude"]]).values
    near_settlements_ball_tree = BallTree(near_settlements_coords_rad, metric="haversine")

    center_coord_rad = np.radians([[quake_lat, quake_lon]])
    city_indices = near_settlements_ball_tree.query_radius(center_coord_rad, r=danger_radius_in_radians)[0]

    settlement_names = near_settlements_data.iloc[city_indices]["name"].tolist()
    
    return settlement_names

def get_quake_settlement_and_S_value_data() -> list[tuple[str, float]]:
    response = earthquakes_table.scan()
    earthquakes = response.get("Items", [])

    while "LastEvaluatedKey" in response:
        response = earthquakes_table.scan(ExclusiveStartKey=response["LastEvaluatedKey"])
        earthquakes.extend(response.get("Items", []))
    
    settlements_and_S_values = {}

    for earthquake in earthquakes:
        s_value = earthquake.get("S", None)
        settlements = earthquake.get("possibly_affected_settlements", None)

        if s_value and settlements:
            for settlement in settlements:
                settlements_and_S_values[settlement] = settlements_and_S_values.get(settlement, 0) + s_value
        else:
            continue

    settlements_and_S_values = sorted(settlements_and_S_values.items(), key=lambda x: x[1], reverse=True)

    return settlements_and_S_values

def get_the_twitter_query(settlements: list[tuple[str, float]]) -> str:
    base_query = "#deprem (#yardım OR ihtiyac OR yardım OR ihtiyaç OR enkaz OR erzak) lang:tr -is:retweet -has:media"
    settlements = [settlement[0].lower() for settlement in settlements]

    if not settlements:
        return base_query

    query_extension = settlements[0]

    for settlement in settlements[1:]:
        if len(query_extension + " OR " + settlement) >= 500 - len("() ") - len(base_query):
            break
        else:
            query_extension += " OR " + settlement

    return "(" + query_extension + ") " + base_query


def handle_earthquake_data(URL: str):
    try:
        r = requests.get(URL)
    except Exception as e:
        print(f"Error loading the earthquake data: {e}")
        return
    
    try:
        soup = BeautifulSoup(r.content, 'html.parser')
    except Exception as e:
        print(f"Error parsing the earthquake data: {e}")
        return
    
    try:
        pre_tag = soup.find('pre')
    except Exception as e:
        print(f"Error finding the earthquake data (pre_tag): {e}")
        return

    if pre_tag:
        text = pre_tag.get_text()
        lines = text.splitlines() # Every line is a list item, representing an earthquake
        data_start_index = None

        for i, line in enumerate(lines):
            if line.strip().startswith('----------'):
                data_start_index = i + 1
                break

        if data_start_index is None:
            print("Seperator line not found (----------), there is a problem with the earthquake data format.")
            return

        date_of_last_processed_earthquake = _get_last_earthquake_date(last_earthquake_date_table)
        dates = []

        for line in lines[data_start_index:]:
            if not line.strip():
                break

            parts = line.split()

            if len(parts) < 9:
                print(f"Line {line} is not a valid earthquake data line, skipping...")
                continue

            quake_date = parts[0]

            if quake_date < date_of_last_processed_earthquake: # Don't process data if they are already in the database
                print(f"Earthquake date {quake_date} is before the last processed earthquake date {date_of_last_processed_earthquake}, terminating the process...")
                break

            latitude_str = parts[2]
            longitude_str = parts[3]

            try:
                latitude = float(latitude_str)
                longitude = float(longitude_str)
            except ValueError:
                print(f"Latitude or longitude is not a valid number, skipping...")
                continue

            # Checks coordinates to see if the earthquake is close enough to affect Turkey
            # Explanation of why these coordinates where chosen is written below
            if 24.58 <= longitude <= 45 and 36 <= latitude <= 42:
                time = parts[1]
                depth_str = parts[4]
                MD = None if parts[5] == "-.-" else parts[5]
                ML = parts[6]
                Mw = None if parts[7] == "-.-" else parts[7]
                quality = parts[-1]
                location = " ".join(parts[8:-1])
                nearest_settlement_distance, nearest_settlement_name = _distance_to_nearest_settlement(latitude, longitude)
                quake = {
                    'date': quake_date,
                    'time': time,
                    'latitude': latitude_str,
                    'longitude': longitude_str,
                    'depth_km': depth_str,
                    'MD': MD,
                    'ML': ML,
                    'Mw': Mw,
                    'location': location,
                    'quality': quality,
                    'is_offshore': _is_offshore(latitude, longitude),
                    'nearest_settlement_distance': nearest_settlement_distance,
                    'nearest_settlement_name': nearest_settlement_name,
                }
                quake['possibly_affected_settlements'] = find_settlements_within_danger_radius(quake)
                quake["S"], quake["ttl"] = _determine_earthquake_Svalue_and_ttl(quake)

                quake["is_fatal"] = quake["S"] > 4.0 # This value is based on the danger level threshold calculations explained below

                earthquakes_table.put_item(Item=quake)
                dates.append(quake['date'])

        _update_last_earthquake_date(dates[0]) # First is chosen because we read the last earthquake first

        return {
            "success": True,
            "status_code": 200,
            "message": "Earthquake data processed successfully"
        }

    return {
        "success": False,
        "status_code": 400,
        "message": "Error processing the earthquake data"
    }


# The coordinates 42N, 36N, and 45E roughly gives the coordinates of Turkey and captures all major fault lines that could cause an earthquake. The coordinate 24.58E was chosen as it is the middle of the Helenic arc (center of Crete). Roughly 310km from the nearest Turkish coast. A major fault line exists near this longitude, which could pose a structural damage or tsunami risk if an earthquake larger than Mw 7.5 occurs.

"""
Sample data in format of l_earth for testing:

earthquake_data = [
    {'date': '2025.08.03', 'time': '13:43:07', 'latitude': '40.3658', 'longitude': '28.9940', 'depth_km': '8.1', 'MD': '-.-', 'ML': '2.8', 'Mw': '-.-', 'location': 'ALAKIR-SINDIRGI (BALIKESIR)', 'quality': 'İlksel'},
    {'date': '2025.08.03', 'time': '13:41:58', 'latitude': '36.2197', 'longitude': '36.2042', 'depth_km': '8.5', 'MD': '-.-', 'ML': '2.2', 'Mw': '6.2', 'location': 'SARAYKÖY (BALIKESIR)', 'quality': 'İlksel'},
    {'date': '2025.08.03', 'time': '13:11:51', 'latitude': '39.2502', 'longitude': '28.9685', 'depth_km': '11.3', 'MD': '-.-', 'ML': '1.9', 'Mw': '-.-', 'location': 'KARAKOYUNLU (BALIKESIR)', 'quality': 'İlksel'},
    {'date': '2025.08.03', 'time': '13:10:33', 'latitude': '39.0642', 'longitude': '25.7962', 'depth_km': '7.6', 'MD': '-.-', 'ML': '2.3', 'Mw': '7.1', 'location': 'KARAKOYUNLU (BALIKESIR)', 'quality': 'İlksel'},
    {'date': '2025.08.03', 'time': '12:55:01', 'latitude': '39.5011', 'longitude': '26.8837', 'depth_km': '9.8', 'MD': '-.-', 'ML': '3.5', 'Mw': '-.-', 'location': 'KARAKOYUNLU (BALIKESIR)', 'quality': 'İlksel'},
    {'date': '2025.08.03', 'time': '12:45:01', 'latitude': '38.5011', 'longitude': '30.8837', 'depth_km': '15.2', 'MD': '-.-', 'ML': '4.1', 'Mw': '5.9', 'location': 'KARAKOYUNLU (BALIKESIR)', 'quality': 'REVİZE01'}
]
"""



if __name__ == "__main__":
    URL = 'http://www.koeri.boun.edu.tr/scripts/lst1.asp'
    handle_earthquake_data(URL)

# The smallest deadly earthquake in Turkey in the last 50 years occurred in 1983 near Biga, with a magnitude of 6.1 (Ms). Today, the Mw (moment magnitude) 
# scale is more commonly used instead of Ms. Another deadly earthquake in 1986 in Malatya also had a Mw of 6.1. Furthermore, the 6.1 Mw Elazığ earthquake 
# in 2010 gave 41 casualties. This suggests that the minimum magnitude for fatal earthquakes in Turkey is 6.1 (Mw). Hence, the threshold has been set to 
# 6.1 Mw in the code, and it is advised that this threshold remain the same for later parts of the project.

# DANGER LEVEL THRESHOLD CALCULATION:
# To find S value corresponding to Mw 6.1 (minimum fatal earthquake threshold):
# Using formula: S = (M - beta * log10(Rv + 1)) * O
# Where: M=6.1, beta=1.1, Rv=sqrt(D²+h²), O=1.0 (onshore)
# 
# Example calculations for Mw 6.1:
# - Close to settlement (D=10km, h=10km): Rv=14.14km → S = (6.1 - 1.1*1.18)*1.0 = 4.80
# - Medium distance (D=25km, h=15km): Rv=29.15km → S = (6.1 - 1.1*1.48)*1.0 = 4.47  
# - Far from settlement (D=50km, h=20km): Rv=53.85km → S = (6.1 - 1.1*1.74)*1.0 = 4.19
# 
# Therefore, S ≥ 4.0 can be used as threshold for potentially fatal earthquakes (Mw ≥ 6.1), with a margin of error to be more inclusive.
# This allows classification even when Mw data is unavailable (most of the cases)




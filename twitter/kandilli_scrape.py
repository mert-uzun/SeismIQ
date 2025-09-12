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

WORLD_RADIUS_KM = 6371
LOCAL_GEOJSON_PATH = "/tmp/land.geojson"
url = 'http://www.koeri.boun.edu.tr/scripts/lst1.asp'


earthquakes_table = boto3.resource("dynamodb").Table(os.getenv("EARTHQUAKES_TABLE_NAME"))
last_earthquake_date_table = boto3.resource("dynamodb").Table(os.getenv("LAST_EARTHQUAKE_DATE_TABLE_NAME"))
r = requests.get(url)

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

def _determine_earthquake_ttl(quake: dict) -> float:
    dt = datetime.strptime(quake.get('date', "") + " " quake.get('time', ""), "%Y.%m.%d %H:%M:%S")
    lat = float(quake.get('latitude', ""))
    lon = float(quake.get('longtitude', ""))
    h = float(quake.get('depth', ""))
    ML = float(quake.get('ML', ""))
    Mw = float(quake.get('Mw', ""))


    # FORMULA
    M = Mw if Mw else ML
    D, nearest_settlement_name = _distance_to_nearest_settlement(lat, lon)
    Rv = math.sqrt(D*D + h*h)
    O = .85 if _is_offshore(lat, lon) else 1

    beta = 1.1
    S = (M - beta * math.log10(Rv + 1)) * O

    # TTL values in minutes
    TTLmin = 30
    TTLmax = 10080
    S50 = 3.202 # S value in the middle of the graph, heuristic value for now, TODO: determine it later once we have enough data
    k = 2.4 # Steepnes of the curve around S50, heuristic value for now, TODO: determine it later once we have enough data
    TTL = TTLmin + (TTLmax - TTLmin) / (1 + math.pow(math.e, -k * (S - S50)))
    TTLfinal = math.max(TTLmax, TTL)

    # Datetime from Turkey's timezone to UTC for epoch
    dt = dt.replace(tzinfo=timezone(timedelta(hours=3)))

    quake_datetime_as_epoch = int(dt.timestamp())

    return quake_datetime_as_epoch + (TTLfinal * 60) # Return the value in seconds because we are using epoch time value

# print(r.content) -> to check if the content is being returned

soup = BeautifulSoup(r.content, 'html.parser')

pre_tag = soup.find('pre')
# if pre_tag:
#     print(pre_tag.get_text())

if pre_tag:
    text = pre_tag.get_text()
    lines = text.splitlines() # Every line is a list item, representing an earthquake
    data_start_index = None

    for i, line in enumerate(lines):
        if line.strip().startswith('----------'):
            data_start_index = i + 1
            break

    if data_start_index is None:
        exit()

    date_of_last_processed_earthquake = _get_last_earthquake_date(last_earthquake_date_table)
    dates = []

    for line in lines[data_start_index:]:
        if not line.strip():
            break

        parts = line.split()

        if len(parts) < 9:
            continue

        quake_date = parts[0]

        if quake_date < date_of_last_processed_earthquake: # Don't process data if they are already in the database
            break

        latitude_str = parts[2]
        longitude_str = parts[3]

        try:
            latitude = float(latitude_str)
            longitude = float(longitude_str)
        except ValueError:
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
            }

            quake["ttl"] = _determine_earthquake_ttl(quake)

            earthquakes_table.put_item(Item=quake)
            dates.append(quake['date'])

    _update_last_earthquake_date(dates[0])


# The coordinates 42N, 36N, and 45E roughly gives the coordinates of Turkey and captures all major fault lines that could cause an earthquake. The coordinate 24.58E was chosen as it is the middle of the Helenic arc (center of Crete). Roughly 310km from the nearest Turkish coast. A major fault line exists near this longitude, which could pose a structural damage or tsunami risk if an earthquake larger than Mw 7.5 occurs.

"""
Sample data in format of l_earth for testing:

earthquake_data = [
    {'date': '2025.08.03', 'time': '13:43:07', 'latitude': '40.3658', 'longitude': '28.9940', 'depth_km': '8.1', 'MD': '-.-', 'ML': '2.8', 'Mw': '-.-'},
    {'date': '2025.08.03', 'time': '13:41:58', 'latitude': '36.2197', 'longitude': '36.2042', 'depth_km': '8.5', 'MD': '-.-', 'ML': '2.2', 'Mw': '6.2'},
    {'date': '2025.08.03', 'time': '13:11:51', 'latitude': '39.2502', 'longitude': '28.9685', 'depth_km': '11.3', 'MD': '-.-', 'ML': '1.9', 'Mw': '-.-'},
    {'date': '2025.08.03', 'time': '13:10:33', 'latitude': '39.0642', 'longitude': '25.7962', 'depth_km': '7.6', 'MD': '-.-', 'ML': '2.3', 'Mw': '7.1'},
    {'date': '2025.08.03', 'time': '12:55:01', 'latitude': '39.5011', 'longitude': '26.8837', 'depth_km': '9.8', 'MD': '-.-', 'ML': '3.5', 'Mw': '-.-'},
    {'date': '2025.08.03', 'time': '12:45:01', 'latitude': '38.5011', 'longitude': '30.8837', 'depth_km': '15.2', 'MD': '-.-', 'ML': '4.1', 'Mw': '5.9'}
]
"""

l_earth = []

for quake in earth_list:
    mw_value = quake.get('Mw')

    if mw_value and mw_value != '-.-':
        try:
            mw_magnitude = float(mw_value)

            # Check if the magnitude is greater than 6.1
            #Explanation of why 6.1 is threshold is written below
            if mw_magnitude > 6.1:
                l_earth.append(quake)

        except ValueError:
            continue

#To do: make data save to a file
# NOTE: We don't actually need to save the data to a file, we can save it in a cache database

# The smallest deadly earthquake in Turkey in the last 50 years occurred in 1983 near Biga, with a magnitude of 6.1 (Ms). Today, the Mw (moment magnitude) 
# scale is more commonly used instead of Ms. Another deadly earthquake in 1986 in Malatya also had a Mw of 6.1. Furthermore, the 6.1 Mw Elazığ earthquake 
# in 2010 gave 41 casualties. This suggests that the minimum magnitude for fatal earthquakes in Turkey is 6.1 (Mw). Hence, the threshold has been set to 
# 6.1 Mw in the code, and it is advised that this threshold remain the same for later parts of the project.





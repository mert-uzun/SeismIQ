#!/usr/bin/env python3
"""
SeismIQ Geocoding Service

A bidirectional geocoding service that works both ways:
1. Forward geocoding: City/Province name -> Coordinates  
2. Reverse geocoding: Coordinates -> City/Province name

Uses the same GeoNames Cities5000 data and 
BallTree approach as kandilli_scrape.py
for fast, accurate geocoding specifically optimized for Turkish locations.

Author: Sıla Bozkurt
"""

from ast import Dict
import os
from typing import Optional
import pandas as pd
import numpy as np
from sklearn.neighbors import BallTree
import re
from dataclasses import dataclass
from flask import Flask, request, jsonify

# Constants matching kandilli_scrape.py
WORLD_RADIUS_KM = 6371
REGIONS = {"TR"}  # Turkey only
KEEP_FEATURE_CODES = {"PPLA", "PPLA2", "PPLC", "PPL"}  # Important settlements

# Global variables for data storage (same pattern as kandilli_scrape.py)
cities_data = None
ball_tree = None
name_index = None


@dataclass
class LocationResult:
    """Result for reverse geocoding (coordinates -> location)"""
    city: str
    province: str
    distance_km: float
    feature_code: str
    population: int
    
    def to_dict(self):
        return {
            'city': self.city,
            'province': self.province,
            'distance_km': self.distance_km,
            'feature_code': self.feature_code,
            'population': self.population
        }


@dataclass 
class CoordinateResult:
    """Result for forward geocoding (location -> coordinates)"""
    latitude: float
    longitude: float
    name: str
    feature_code: str
    population: int
    admin1_code: str
    
    def to_dict(self):
        return {
            'latitude': self.latitude,
            'longitude': self.longitude,
            'name': self.name,
            'feature_code': self.feature_code,
            'population': self.population,
            'admin1_code': self.admin1_code
        }


def load_settlement_data():
    """
    Load Turkish settlement data from S3 (same as kandilli_scrape.py)
    Creates global cities_data DataFrame and ball_tree for fast spatial queries
    """
    global cities_data, ball_tree, name_index
    
    if cities_data is not None and ball_tree is not None:
        return  # Already loaded
    
    try:
        # Load GeoNames data from S3 (same source as Twitter processing)
        bucket_name = os.getenv("GEO_BUCKET", "seismiq-geo-data")
        key_name = os.getenv("GEO_KEY", "cities5000.tr-region.v1.parquet")
        
        print(f"Loading settlement data from s3://{bucket_name}/{key_name}")
        cities_data = pd.read_parquet(f"s3://{bucket_name}/{key_name}")
        
        # Filter for Turkish settlements only
        cities_data = cities_data[cities_data["country_code"].isin(REGIONS)]
        cities_data = cities_data[
            cities_data["feature_code"].isin(KEEP_FEATURE_CODES)]
        
        # Clean and validate coordinate data
        cities_data["latitude"] = pd.to_numeric(
            cities_data["latitude"], errors="coerce")
        cities_data["longitude"] = pd.to_numeric(
            cities_data["longitude"], errors="coerce")
        cities_data = cities_data.dropna(
            subset=["latitude", "longitude", "name"])
        
        # Create BallTree for fast spatial queries (same as kandilli_scrape.py)
        coords_rad = np.radians(cities_data[["latitude", "longitude"]].values)
        ball_tree = BallTree(coords_rad, metric="haversine")
        
        # Create name index for forward geocoding
        name_index = create_name_index(cities_data)
        
        print(f"Loaded {len(cities_data)} Turkish settlements for geocoding")
        
    except Exception as e:
        print(f"Error loading settlement data: {e}")
        # Fallback to hardcoded major cities
        cities_data = create_fallback_cities_data()
        if not cities_data.empty:
            coords_rad = np.radians(
                cities_data[["latitude", "longitude"]].values)
            ball_tree = BallTree(coords_rad, metric="haversine")
            name_index = create_name_index(cities_data)
            print(f"Using fallback data with {len(cities_data)} Turkish citis")


def create_name_index(df: pd.DataFrame) -> Dict[str, int]:
    """
    Create a normalized name index for fast city name lookups
    Handles Turkish character variations and common misspellings
    """
    name_index = {}
    
    for idx, row in df.iterrows():
        # Add exact name
        name = str(row['name']).lower().strip()
        name_index[name] = idx
        
        # Add normalized name (handle Turkish characters)
        normalized = normalize_turkish_text(name)
        name_index[normalized] = idx
        
        # Add ASCII name if different
        if 'asciiname' in row and pd.notna(row['asciiname']):
            ascii_name = str(row['asciiname']).lower().strip()
            if ascii_name != name:
                name_index[ascii_name] = idx
                name_index[normalize_turkish_text(ascii_name)] = idx
    
    return name_index


def normalize_turkish_text(text: str) -> str:
    """
    Normalize Turkish text for consistent matching
    Handles character substitutions and common variations
    """
    if not text:
        return ""
    
    # Turkish character mappings
    char_map = {
        'ç': 'c', 'ğ': 'g', 'ı': 'i', 'ö': 'o', 'ş': 's', 'ü': 'u',
        'â': 'a', 'î': 'i', 'û': 'u'
    }
    
    normalized = text.lower()
    for turkish_char, latin_char in char_map.items():
        normalized = normalized.replace(turkish_char, latin_char)
    
    # Remove non-alphanumeric characters
    normalized = re.sub(r'[^a-z0-9]', '', normalized)
    
    return normalized


def distance_to_nearest_settlement(lat: float, lon: float) -> LocationResult:
    """
    Find the nearest settlement to given coordinates (reverse geocoding)
    
    Args:
        lat: Latitude in decimal degrees
        lon: Longitude in decimal degrees
        
    Returns:
        LocationResult with nearest city information
    """
    global ball_tree, cities_data, WORLD_RADIUS_KM
    
    if ball_tree is None or cities_data is None:
        load_settlement_data()
    
    if ball_tree is None:
        raise Exception("Settlement data not available")
    
    # Convert to radians for BallTree query
    coord_rad = np.radians([[lat, lon]])
    distance_rad, city_index = ball_tree.query(coord_rad, k=1)
    
    # Convert distance back to kilometers
    distance_km = distance_rad[0][0] * WORLD_RADIUS_KM
    city_idx = city_index[0][0]
    
    # Get city information
    city_row = cities_data.iloc[city_idx]
    city_name = str(city_row["name"])
    feature_code = str(city_row.get("feature_code", ""))
    population = int(city_row.get("population", 0))
    
    # Get province from admin1_code
    admin1_code = str(city_row.get("admin1_code", ""))
    province = get_province_from_admin1(admin1_code)
    
    return LocationResult(
        city=city_name,
        province=province,
        distance_km=float(distance_km),
        feature_code=feature_code,
        population=population
    )


def coordinates_from_city_name(city_name: str, 
                               province_name: str = 
                               None) -> Optional[CoordinateResult]:
    """
    Find coordinates for a given city/province name (forward geocoding)
    
    Args:
        city_name: Name of the city to find
        province_name: Optional province name for disambiguation
        
    Returns:
        CoordinateResult with coordinates and city info, or None if not found
    """
    global cities_data, name_index
    
    if cities_data is None or name_index is None:
        load_settlement_data()
    
    if cities_data is None:
        return None
    
    # Try exact match first
    result = find_city_by_name(city_name, exact=True)
    if result:
        return result
    
    # Try normalized match
    result = find_city_by_name(city_name, exact=False)
    if result:
        return result
    
    # If province name provided, try province search
    if province_name:
        result = find_city_by_name(province_name, exact=False)
        if result:
            return result
    
    return None


def find_city_by_name(name: str, 
                      exact: bool = True) -> Optional[CoordinateResult]:
    """
    Find city by name in the index
    
    Args:
        name: City name to search for
        exact: Whether to use exact matching or fuzzy matching
        
    Returns:
        CoordinateResult if found, None otherwise
    """
    global cities_data, name_index
    
    if not name or not name_index:
        return None
    
    search_name = name.lower().strip()
    
    # Try exact match
    if search_name in name_index:
        idx = name_index[search_name]
        return create_coordinate_result_from_row(cities_data.iloc[idx])
    
    if not exact:
        # Try normalized match
        normalized = normalize_turkish_text(search_name)
        if normalized in name_index:
            idx = name_index[normalized]
            return create_coordinate_result_from_row(cities_data.iloc[idx])
        
        # Try fuzzy matching
        for indexed_name, idx in name_index.items():
            if normalized in indexed_name or indexed_name in normalized:
                return create_coordinate_result_from_row(cities_data.iloc[idx])
    
    return None


def create_coordinate_result_from_row(row) -> CoordinateResult:
    """Create CoordinateResult from pandas DataFrame row"""
    return CoordinateResult(
        latitude=float(row["latitude"]),
        longitude=float(row["longitude"]),
        name=str(row["name"]),
        feature_code=str(row.get("feature_code", "")),
        population=int(row.get("population", 0)),
        admin1_code=str(row.get("admin1_code", ""))
    )


def get_province_from_admin1(admin1_code: str) -> str:
    """
    Convert admin1_code to province name
    Maps Turkish province codes to province names
    """
    province_map = {
        "01": "Adana", "02": "Adıyaman", "03": "Afyonkarahisar", "04": "Ağrı", 
        "05": "Amasya",
        "06": "Ankara", "07": "Antalya", "08": "Artvin", "09": "Aydın", 
        "10": "Balıkesir",
        "11": "Bilecik", "12": "Bingöl", "13": "Bitlis", "14": "Bolu", 
        "15": "Burdur",
        "16": "Bursa", "17": "Çanakkale", "18": "Çankırı", "19": "Çorum", 
        "20": "Denizli",
        "21": "Diyarbakır", "22": "Edirne", "23": "Elazığ", "24": "Erzincan", 
        "25": "Erzurum",
        "26": "Eskişehir", "27": "Gaziantep", "28": "Giresun", 
        "29": "Gümüşhane", "30": "Hakkâri",
        "31": "Hatay", "32": "Isparta", "33": "Mersin", "34": "İstanbul", 
        "35": "İzmir",
        "36": "Kars", "37": "Kastamonu", "38": "Kayseri", "39": "Kırklareli", 
        "40": "Kırşehir",
        "41": "Kocaeli", "42": "Konya", "43": "Kütahya", "44": "Malatya", 
        "45": "Manisa",
        "46": "Kahramanmaraş", "47": "Mardin", "48": "Muğla", "49": "Muş", 
        "50": "Nevşehir",
        "51": "Niğde", "52": "Ordu", "53": "Rize", "54": "Sakarya", 
        "55": "Samsun",
        "56": "Siirt", "57": "Sinop", "58": "Sivas", "59": "Tekirdağ", 
        "60": "Tokat",
        "61": "Trabzon", "62": "Tunceli", "63": "Şanlıurfa", "64": "Uşak", 
        "65": "Van",
        "66": "Yozgat", "67": "Zonguldak", "68": "Aksaray", "69": "Bayburt", 
        "70": "Karaman",
        "71": "Kırıkkale", "72": "Batman", "73": "Şırnak", "74": "Bartın", 
        "75": "Ardahan",
        "76": "Iğdır", "77": "Yalova", "78": "Karabük", "79": "Kilis", 
        "80": "Osmaniye", "81": "Düzce"
    }
    
    return province_map.get(admin1_code, admin1_code)


def create_fallback_cities_data() -> pd.DataFrame:
    """
    Create fallback data with major Turkish cities if S3 data is not available
    """
    fallback_cities = [
        {"name": "İstanbul", "latitude": 41.0082, "longitude": 28.9784, 
         "feature_code": "PPLA", "admin1_code": "34", "population": 15462452},
        {"name": "Ankara", "latitude": 39.9334, "longitude": 32.8597, 
         "feature_code": "PPLC", "admin1_code": "06", "population": 5663322},
        {"name": "İzmir", "latitude": 38.4192, "longitude": 27.1287, 
         "feature_code": "PPLA", "admin1_code": "35", "population": 4367251},
        {"name": "Bursa", "latitude": 40.1826, "longitude": 29.0665, 
         "feature_code": "PPLA", "admin1_code": "16", "population": 3056120},
        {"name": "Antalya", "latitude": 36.8841, "longitude": 30.7056, 
         "feature_code": "PPLA", "admin1_code": "07", "population": 2548308},
        {"name": "Adana", "latitude": 37.0000, "longitude": 35.3213, 
         "feature_code": "PPLA", "admin1_code": "01", "population": 2274106},
        {"name": "Konya", "latitude": 37.8667, "longitude": 32.4833, 
         "feature_code": "PPLA", "admin1_code": "42", "population": 2232374},
        {"name": "Gaziantep", "latitude": 37.0662, "longitude": 37.3833, 
         "feature_code": "PPLA", "admin1_code": "27", "population": 2069364},
        {"name": "Şanlıurfa", "latitude": 37.1591, "longitude": 38.7969, 
         "feature_code": "PPLA", "admin1_code": "63", "population": 2073614},
        {"name": "Mersin", "latitude": 36.8121, "longitude": 34.6415, 
         "feature_code": "PPLA", "admin1_code": "33", "population": 1840425},
        {"name": "Diyarbakır", "latitude": 37.9144, "longitude": 40.2306, 
         "feature_code": "PPLA", "admin1_code": "21", "population": 1756353},
        {"name": "Kocaeli", "latitude": 40.8533, "longitude": 29.8815, 
         "feature_code": "PPLA", "admin1_code": "41", "population": 1953035},
        {"name": "Hatay", "latitude": 36.4018, "longitude": 36.3498, 
         "feature_code": "PPLA", "admin1_code": "31", "population": 1686043},
        {"name": "Manisa", "latitude": 38.6191, "longitude": 27.4289, 
         "feature_code": "PPLA", "admin1_code": "45", "population": 1429643},
        {"name": "Kayseri", "latitude": 38.7312, "longitude": 35.4787, 
         "feature_code": "PPLA", "admin1_code": "38", "population": 1421362},
        {"name": "Samsun", "latitude": 41.2928, "longitude": 36.3313, 
         "feature_code": "PPLA", "admin1_code": "55", "population": 1348542},
        {"name": "Balıkesir", "latitude": 39.6484, "longitude": 27.8826, 
         "feature_code": "PPLA", "admin1_code": "10", "population": 1257590},
        {"name": "Kahramanmaraş", "latitude": 37.5858, "longitude": 36.9371, 
         "feature_code": "PPLA", "admin1_code": "46", "population": 1168163},
        {"name": "Van", "latitude": 38.4891, "longitude": 43.4089, 
         "feature_code": "PPLA", "admin1_code": "65", "population": 1123784},
        {"name": "Aydın", "latitude": 37.8560, "longitude": 27.8416, 
         "feature_code": "PPLA", "admin1_code": "09", "population": 1119084},
        {"name": "Denizli", "latitude": 37.7765, "longitude": 29.0864, 
         "feature_code": "PPLA", "admin1_code": "20", "population": 1037208},
        {"name": "Malatya", "latitude": 38.3552, "longitude": 38.3095, 
         "feature_code": "PPLA", "admin1_code": "44", "population": 812580},
        {"name": "Erzurum", "latitude": 39.9000, "longitude": 41.2700, 
         "feature_code": "PPLA", "admin1_code": "25", "population": 767848},
        {"name": "Trabzon", "latitude": 41.0015, "longitude": 39.7178, 
         "feature_code": "PPLA", "admin1_code": "61", "population": 808974},
        {"name": "Ordu", "latitude": 40.9839, "longitude": 37.8764, 
         "feature_code": "PPLA", "admin1_code": "52", "population": 771932}
    ]
    
    return pd.DataFrame(fallback_cities)


app = Flask(__name__)


@app.route('/health', methods=['GET'])
def health_check():
    """Simple health check endpoint"""
    return jsonify({"status": "healthy", "service": "seismiq-geocoding"})


@app.route('/geocoding/reverse', methods=['GET'])
def reverse_geocode():
    """
    Reverse geocoding: coordinates -> city/province name
    
    Query parameters:
        lat: Latitude in decimal degrees
        lon: Longitude in decimal degrees
        
    Returns:
        JSON with city, province, distance, and other location info
    """
    try:
        lat = float(request.args.get('lat', 0))
        lon = float(request.args.get('lon', 0))
        
        if lat == 0 and lon == 0:
            return jsonify({"error": "Invalid coordinates"}), 400
            
        result = distance_to_nearest_settlement(lat, lon)
        return jsonify(result.to_dict())
        
    except ValueError as e:
        return jsonify({"error": f"Invalid coordinate values: {e}"}), 400
    except Exception as e:
        return jsonify({"error": f"Geocoding failed: {e}"}), 500


@app.route('/geocoding/forward', methods=['GET'])
def forward_geocode():
    """
    Forward geocoding: city/province name -> coordinates
    
    Query parameters:
        city: City name to search for
        province: Optional province name for disambiguation
        
    Returns:
        JSON with latitude, longitude, and city info
    """
    try:
        city_name = request.args.get('city', '').strip()
        province_name = request.args.get('province', '').strip()
        
        if not city_name:
            return jsonify({"error": "City name is required"}), 400
            
        result = coordinates_from_city_name(city_name, 
                                            province_name if province_name 
                                            else None)
        
        if result:
            return jsonify(result.to_dict())
        else:
            return jsonify({"error": f"City '{city_name}' not found"}), 404
            
    except Exception as e:
        return jsonify({"error": f"Geocoding failed: {e}"}), 500


@app.route('/geocoding/batch/reverse', methods=['POST'])
def batch_reverse_geocode():
    """
    Batch reverse geocoding for multiple coordinates
    
    POST body:
        {"coordinates": [{"lat": 41.0, "lon": 29.0}, 
        {"lat": 39.9, "lon": 32.8}]}
        
    Returns:
        JSON array with results for each coordinate
    """
    try:
        data = request.get_json()
        coordinates = data.get('coordinates', [])
        
        if not coordinates:
            return jsonify({"error": "No coordinates provided"}), 400
            
        results = []
        for coord in coordinates:
            try:
                lat = float(coord['lat'])
                lon = float(coord['lon'])
                result = distance_to_nearest_settlement(lat, lon)
                results.append(result.to_dict())
            except Exception as e:
                results.append({"error": str(e)})
                
        return jsonify({"results": results})
        
    except Exception as e:
        return jsonify({"error": f"Batch geocoding failed: {e}"}), 500


@app.route('/stats', methods=['GET'])
def get_stats():
    """Get statistics about loaded data"""
    global cities_data
    
    if cities_data is None:
        load_settlement_data()
    
    stats = {
        "total_cities": len(cities_data) if cities_data is not None else 0,
        "service": "seismiq-geocoding",
        "data_source": "GeoNames Cities5000",
        "regions": list(REGIONS),
        "feature_codes": list(KEEP_FEATURE_CODES)
    }
    
    return jsonify(stats)


if __name__ == '__main__':
    print("Starting SeismIQ Geocoding Service...")
    load_settlement_data()
    port = int(os.getenv('PORT', 8080))
    app.run(host='0.0.0.0', port=port, debug=False)
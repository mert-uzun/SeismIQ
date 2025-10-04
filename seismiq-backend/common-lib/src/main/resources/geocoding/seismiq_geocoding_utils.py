#!/usr/bin/env python3
"""
SeismIQ Geocoding Utilities

Simple utility functions for bidirectional geocoding:
- find_coordinates_from_name(city_name, province_name=None)
- find_location_from_coordinates(lat, lon)

Lightweight wrapper around the same GeoNames data used in kandilli_scrape.py
Can be imported and used in other Python modules.

Usage:
    from seismiq_geocoding_utils import find_coordinates_from_name, 
        find_location_from_coordinates
    
    # Forward geocoding
    coords = find_coordinates_from_name("Istanbul")
    if coords:
        print(f"Istanbul: {coords['latitude']}, {coords['longitude']}")
    
    # Reverse geocoding  
    location = find_location_from_coordinates(41.0082, 28.9784)
    if location:
        print(f"Location: {location['city']}, {location['province']}")
"""

import os
import pandas as pd
import numpy as np
from sklearn.neighbors import BallTree
import re
from typing import Optional, Dict

# Constants
WORLD_RADIUS_KM = 6371
REGIONS = {"TR"}
# Include districts (PPLA3, PPLA4) and administrative divisions (ADM2, ADM3)
KEEP_FEATURE_CODES = {"PPLA", "PPLA2", "PPLA3", "PPLA4", "PPLC", 
                      "PPL", "ADM2", "ADM3"}

# Global data storage
_cities_data = None
_ball_tree = None
_name_index = None


def _load_data():
    """Load settlement data including districts if not already loaded"""
    global _cities_data, _ball_tree, _name_index
    
    if _cities_data is not None and _ball_tree is not None:
        return
    
    try:
        # Try to load from S3
        bucket_name = os.getenv("GEO_BUCKET", "seismiq-geo-data")
        key_name = os.getenv("GEO_KEY", "cities5000.tr-region.v1.parquet")
        
        _cities_data = pd.read_parquet(f"s3://{bucket_name}/{key_name}")
        
        # Filter for Turkish settlements including districts
        _cities_data = _cities_data[_cities_data["country_code"].isin(REGIONS)]
        _cities_data = _cities_data[_cities_data
                                    ["feature_code"].isin(KEEP_FEATURE_CODES)]
        
        # Clean coordinates
        _cities_data["latitude"] = pd.to_numeric(_cities_data["latitude"], 
                                                 errors="coerce")
        _cities_data["longitude"] = pd.to_numeric(_cities_data["longitude"], 
                                                  errors="coerce")
        _cities_data = _cities_data.dropna(
            subset=["latitude", "longitude", "name"])
        
        # Create BallTree
        coords_rad = np.radians(_cities_data[["latitude", "longitude"]].values)
        _ball_tree = BallTree(coords_rad, metric="haversine")
        
        # Create name index
        _name_index = {}
        for idx, row in _cities_data.iterrows():
            name = str(row['name']).lower().strip()
            _name_index[name] = idx
            _name_index[_normalize_text(name)] = idx
            
            if 'asciiname' in row and pd.notna(row['asciiname']):
                ascii_name = str(row['asciiname']).lower().strip()
                if ascii_name != name:
                    _name_index[ascii_name] = idx
                    _name_index[_normalize_text(ascii_name)] = idx
        
    except Exception:
        # Fallback to major cities and districts
        _cities_data = _create_fallback_data()
        coords_rad = np.radians(_cities_data[["latitude", "longitude"]].values)
        _ball_tree = BallTree(coords_rad, metric="haversine")
        
        _name_index = {}
        for idx, row in _cities_data.iterrows():
            name = str(row['name']).lower().strip()
            _name_index[name] = idx
            _name_index[_normalize_text(name)] = idx


def _normalize_text(text: str) -> str:
    """Normalize Turkish text for matching"""
    if not text:
        return ""
    
    char_map = {
        'ç': 'c', 'ğ': 'g', 'ı': 'i', 'ö': 'o', 'ş': 's', 'ü': 'u',
        'â': 'a', 'î': 'i', 'û': 'u'
    }
    
    normalized = text.lower()
    for turkish_char, latin_char in char_map.items():
        normalized = normalized.replace(turkish_char, latin_char)
    
    return re.sub(r'[^a-z0-9]', '', normalized)


def _get_province_name(admin1_code: str) -> str:
    """Convert admin1_code to province name"""
    province_map = {
        "01": "Adana", "02": "Adıyaman", "03": "Afyonkarahisar", "04": "Ağrı", 
        "05": "Amasya", "06": "Ankara", "07": "Antalya", "08": "Artvin", 
        "09": "Aydın", "10": "Balıkesir", "11": "Bilecik", "12": "Bingöl", 
        "13": "Bitlis", "14": "Bolu", "15": "Burdur", "16": "Bursa", 
        "17": "Çanakkale", "18": "Çankırı", "19": "Çorum", "20": "Denizli", 
        "21": "Diyarbakır", "22": "Edirne", "23": "Elazığ", "24": "Erzincan", 
        "25": "Erzurum", "26": "Eskişehir", "27": "Gaziantep", "28": "Giresun", 
        "29": "Gümüşhane", "30": "Hakkâri", "31": "Hatay", "32": "Isparta", 
        "33": "Mersin", "34": "İstanbul", "35": "İzmir", "36": "Kars", 
        "37": "Kastamonu", "38": "Kayseri", "39": "Kırklareli", 
        "40": "Kırşehir", "41": "Kocaeli", "42": "Konya", "43": "Kütahya", 
        "44": "Malatya", "45": "Manisa", "46": "Kahramanmaraş", "47": "Mardin", 
        "48": "Muğla", "49": "Muş", "50": "Nevşehir", "51": "Niğde",
        "52": "Ordu", "53": "Rize", "54": "Sakarya", "55": "Samsun",
        "56": "Siirt", "57": "Sinop", "58": "Sivas", "59": "Tekirdağ", 
        "60": "Tokat", "61": "Trabzon", "62": "Tunceli", "63": "Şanlıurfa", 
        "64": "Uşak", "65": "Van", "66": "Yozgat", "67": "Zonguldak", 
        "68": "Aksaray", "69": "Bayburt", "70": "Karaman",
        "71": "Kırıkkale", "72": "Batman", "73": "Şırnak", "74": "Bartın",
        "75": "Ardahan", "76": "Iğdır", "77": "Yalova", "78": "Karabük", 
        "79": "Kilis", "80": "Osmaniye", "81": "Düzce"
    }
    return province_map.get(admin1_code, admin1_code)


def _create_fallback_data() -> pd.DataFrame:
    """Create fallback data with major Turkish cities and districts"""
    fallback_cities = [
        # Major cities (İl merkezleri)
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
        
        # İstanbul districts (İlçeler)
        {"name": "Kadıköy", "latitude": 40.9903, "longitude": 29.0301, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 467919},
        {"name": "Üsküdar", "latitude": 41.0214, "longitude": 29.0138, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 524452},
        {"name": "Beşiktaş", "latitude": 41.0422, "longitude": 29.0094, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 190033},
        {"name": "Şişli", "latitude": 41.0602, "longitude": 28.9890, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 263775},
        {"name": "Beyoğlu", "latitude": 41.0361, "longitude": 28.9769, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 233143},
        {"name": "Fatih", "latitude": 41.0186, "longitude": 28.9647, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 368227},
        {"name": "Bakırköy", "latitude": 40.9744, "longitude": 28.8719, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 218388},
        {"name": "Pendik", "latitude": 40.8782, "longitude": 29.2333, 
         "feature_code": "PPLA3", "admin1_code": "34", "population": 625365},
        
        # Ankara districts
        {"name": "Çankaya", "latitude": 39.9208, "longitude": 32.8541, 
         "feature_code": "PPLA3", "admin1_code": "06", "population": 919404},
        {"name": "Keçiören", "latitude": 39.9925, "longitude": 32.8206, 
         "feature_code": "PPLA3", "admin1_code": "06", "population": 915159},
        {"name": "Yenimahalle", "latitude": 39.9667, "longitude": 32.7833, 
         "feature_code": "PPLA3", "admin1_code": "06", "population": 656441},
        
        # İzmir districts  
        {"name": "Konak", "latitude": 38.4237, "longitude": 27.1428, 
         "feature_code": "PPLA3", "admin1_code": "35", "population": 373565},
        {"name": "Bornova", "latitude": 38.4639, "longitude": 27.2167, 
         "feature_code": "PPLA3", "admin1_code": "35", "population": 445415},
        {"name": "Karşıyaka", "latitude": 38.4594, "longitude": 27.1281, 
         "feature_code": "PPLA3", "admin1_code": "35", "population": 328008},
        
        # Other major cities
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
         "feature_code": "PPLA", "admin1_code": "31", "population": 1686043}
    ]
    return pd.DataFrame(fallback_cities)


def find_coordinates_from_name(city_name: str, 
                               province_name: str = None) -> Optional[Dict]:
    """
    Find coordinates for a city/district/province name (forward geocoding)
    
    Args:
        city_name: Name of the city/district to find
        province_name: Optional province name for disambiguation
        
    Returns:
        Dictionary with latitude, longitude, name, and other info, 
        or None if not found
        
    Example:
        result = find_coordinates_from_name("Kadıköy")
        if result:
            print(f"Lat: {result['latitude']}, Lon: {result['longitude']}")
    """
    global _cities_data, _name_index
    
    if not city_name or not city_name.strip():
        return None
    
    _load_data()
    
    if _cities_data is None or _name_index is None:
        return None
    
    search_name = city_name.lower().strip()
    
    # Try exact match first
    if search_name in _name_index:
        idx = _name_index[search_name]
        row = _cities_data.iloc[idx]
        return {
            'latitude': float(row['latitude']),
            'longitude': float(row['longitude']),
            'name': str(row['name']),
            'province': _get_province_name(str(row.get('admin1_code', ''))),
            'feature_code': str(row.get('feature_code', '')),
            'population': int(row.get('population', 0))
        }
    
    # Try normalized match
    normalized = _normalize_text(search_name)
    if normalized in _name_index:
        idx = _name_index[normalized]
        row = _cities_data.iloc[idx]
        return {
            'latitude': float(row['latitude']),
            'longitude': float(row['longitude']),
            'name': str(row['name']),
            'province': _get_province_name(str(row.get('admin1_code', ''))),
            'feature_code': str(row.get('feature_code', '')),
            'population': int(row.get('population', 0))
        }
    
    # Try fuzzy matching
    for indexed_name, idx in _name_index.items():
        if normalized in indexed_name or indexed_name in normalized:
            row = _cities_data.iloc[idx]
            return {
                'latitude': float(row['latitude']),
                'longitude': float(row['longitude']),
                'name': str(row['name']),
                'province': _get_province_name(str(row.get('admin1_code', 
                                                           ''))),
                'feature_code': str(row.get('feature_code', '')),
                'population': int(row.get('population', 0))
            }
    
    # If province name provided, try that too
    if province_name:
        return find_coordinates_from_name(province_name)
    
    return None


def find_location_from_coordinates(lat: float, lon: float) -> Optional[Dict]:
    """
    Find the nearest city/district/province for given coordinates
    
    Args:
        lat: Latitude in decimal degrees
        lon: Longitude in decimal degrees
        
    Returns:
        Dictionary with city, province, distance_km, and other info,
          or None if failed
        
    Example:
        result = find_location_from_coordinates(41.0082, 28.9784)
        if result:
            print(f"City: {result['city']}, Province: {result['province']}")
    """
    global _ball_tree, _cities_data
    
    _load_data()
    
    if _ball_tree is None or _cities_data is None:
        return None
    
    try:
        # Convert to radians for BallTree query
        coord_rad = np.radians([[lat, lon]])
        distance_rad, city_index = _ball_tree.query(coord_rad, k=1)
        
        # Convert distance back to kilometers
        distance_km = distance_rad[0][0] * WORLD_RADIUS_KM
        city_idx = city_index[0][0]
        
        # Get city information
        city_row = _cities_data.iloc[city_idx]
        
        return {
            'city': str(city_row['name']),
            'province': _get_province_name(str(city_row.get('admin1_code', 
                                                            ''))),
            'distance_km': float(distance_km),
            'feature_code': str(city_row.get('feature_code', '')),
            'population': int(city_row.get('population', 0)),
            'latitude': float(city_row['latitude']),
            'longitude': float(city_row['longitude'])
        }
        
    except Exception:
        return None


# Convenience aliases for easier usage
geocode_forward = find_coordinates_from_name
geocode_reverse = find_location_from_coordinates


if __name__ == '__main__':
    # Test the functions
    print("Testing SeismIQ Geocoding Utils...")
    
    # Test forward geocoding with cities and districts
    print("\n=== Forward Geocoding Tests ===")
    test_cities = ["Istanbul", "Ankara", "İzmir", "Kadıköy", "Çankaya", 
                   "Bornova"]
    
    for city in test_cities:
        result = find_coordinates_from_name(city)
        if result:
            print(f"{city}: {result['latitude']:.4f}, "
                  f"{result['longitude']:.4f} ({result['province']}) - "
                  f"{result['feature_code']}")
        else:
            print(f"{city}: Not found")
    
    # Test reverse geocoding
    print("\n=== Reverse Geocoding Tests ===")
    test_coords = [
        (41.0082, 28.9784),  # Istanbul
        (40.9903, 29.0301),  # Kadıköy
        (39.9334, 32.8597),  # Ankara
        (38.4192, 27.1287),  # Izmir
    ]
    
    for lat, lon in test_coords:
        result = find_location_from_coordinates(lat, lon)
        if result:
            print(f"({lat}, {lon}): {result['city']}, {result['province']} "
                  f"(distance: {result['distance_km']:.2f} km) - "
                  f"{result['feature_code']}")
        else:
            print(f"({lat}, {lon}): Not found")
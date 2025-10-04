"""
Integration Example: SeismIQ Geocoding with Java Backend

This example shows how to use the geocoding service with your Java backend
for the Reports service. You can call this from Java using:

1. HTTP API (run the Flask service)
2. Direct Python execution (recommended for AWS Lambda)
3. Subprocess calls from Java

This matches your existing Twitter processing geocoding system.
"""

import json
import sys
from seismiq_geocoding_utils import find_coordinates_from_name
from seimiq_geocoding_utils import find_location_from_coordinates


def geocode_report_data(report_data):
    """
    Geocode report data for the Reports service
    
    Args:
        report_data: Dictionary with report information
        
    Returns:
        Updated report data with geocoding information
    """
    
    # Example report data structure:
    # {
    #     "reportId": "123",
    #     "latitude": 41.0082,
    #     "longitude": 28.9784,
    #     "city": null,
    #     "province": null,
    #     "description": "Earthquake felt in the area"
    # }
    
    result = dict(report_data)
    
    # Case 1: We have coordinates but missing city/province
    if ('latitude' in result and 'longitude' in result and 
        result['latitude'] and result['longitude'] and
            (not result.get('city') or not result.get('province'))):
        
        location = find_location_from_coordinates(
            float(result['latitude']), 
            float(result['longitude'])
        )
        
        if location:
            result['city'] = location['city']
            result['province'] = location['province']
            result['geocoding_distance_km'] = location['distance_km']
            result['geocoding_method'] = 'reverse'
            
    # Case 2: We have city/province but missing coordinates  
    elif ('city' in result and result['city'] and
          (not result.get('latitude') or not result.get('longitude'))):
        
        coordinates = find_coordinates_from_name(
            result['city'], 
            result.get('province')
        )
        
        if coordinates:
            result['latitude'] = coordinates['latitude']
            result['longitude'] = coordinates['longitude']
            result['geocoding_method'] = 'forward'
            
            # Verify the province matches
            if coordinates['province'] and not result.get('province'):
                result['province'] = coordinates['province']
    
    return result


def batch_geocode_reports(reports_list):
    """
    Batch geocode multiple reports
    
    Args:
        reports_list: List of report dictionaries
        
    Returns:
        List of geocoded reports
    """
    geocoded_reports = []
    
    for report in reports_list:
        try:
            geocoded_report = geocode_report_data(report)
            geocoded_reports.append(geocoded_report)
        except Exception as e:
            report['geocoding_error'] = str(e)
            geocoded_reports.append(report)
    
    return geocoded_reports


def filter_reports_by_location(reports_list, city_name=None, 
                               province_name=None):
    """
    Filter reports by city or province name
    
    Args:
        reports_list: List of report dictionaries
        city_name: City name to filter by (optional)
        province_name: Province name to filter by (optional)
        
    Returns:
        Filtered list of reports
    """
    filtered_reports = []
    
    for report in reports_list:
        match = True
        
        if city_name:
            report_city = report.get('city', '').lower()
            if city_name.lower() not in report_city:
                match = False
                
        if province_name:
            report_province = report.get('province', '').lower()  
            if province_name.lower() not in report_province:
                match = False
        
        if match:
            filtered_reports.append(report)
    
    return filtered_reports


def find_nearby_reports(reports_list, lat, lon, radius_km=50):
    """
    Find reports within a certain radius of coordinates
    
    Args:
        reports_list: List of report dictionaries
        lat: Center latitude
        lon: Center longitude
        radius_km: Search radius in kilometers
        
    Returns:
        List of reports within the radius, sorted by distance
    """
    import math
    
    def haversine_distance(lat1, lon1, lat2, lon2):
        """Calculate haversine distance between two points"""
        R = 6371  # Earth radius in km
        
        lat1_rad = math.radians(lat1)
        lon1_rad = math.radians(lon1)
        lat2_rad = math.radians(lat2)
        lon2_rad = math.radians(lon2)
        
        dlat = lat2_rad - lat1_rad
        dlon = lon2_rad - lon1_rad
        
        a = (math.sin(dlat/2)**2 + 
             math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(dlon/2)**2)
        c = 2 * math.asin(math.sqrt(a))
        
        return R * c
    
    nearby_reports = []
    
    for report in reports_list:
        if 'latitude' in report and 'longitude' in report:
            try:
                report_lat = float(report['latitude'])
                report_lon = float(report['longitude'])
                
                distance = haversine_distance(lat, lon, report_lat, report_lon)
                
                if distance <= radius_km:
                    report_with_distance = dict(report)
                    report_with_distance['distance_from_center_km'] = distance
                    nearby_reports.append(report_with_distance)
                    
            except (ValueError, TypeError):
                continue
    
    # Sort by distance
    nearby_reports.sort(key=lambda x: x['distance_from_center_km'])
    
    return nearby_reports


def main():
    """
    Main function to demonstrate usage
    Can be called from Java with command line arguments
    """
    if len(sys.argv) < 2:
        print("Usage examples:")
        print("  python integration_example.py geocode "
              "'{\"latitude\":41.0082,\"longitude\":28.9784}'")
        print("  python integration_example.py reverse_geocode "
              "41.0082 28.9784")
        print("  python integration_example.py forward_geocode Istanbul")
        print("  python integration_example.py batch_geocode '[{...}, {...}]'")
        return
    
    command = sys.argv[1]
    
    try:
        if command == 'geocode':
            # Geocode a single report
            report_json = sys.argv[2]
            report_data = json.loads(report_json)
            result = geocode_report_data(report_data)
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
        elif command == 'reverse_geocode':
            # Reverse geocode coordinates
            lat = float(sys.argv[2])
            lon = float(sys.argv[3])
            result = find_location_from_coordinates(lat, lon)
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
        elif command == 'forward_geocode':
            # Forward geocode city name
            city_name = sys.argv[2]
            province_name = sys.argv[3] if len(sys.argv) > 3 else None
            result = find_coordinates_from_name(city_name, province_name)
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
        elif command == 'batch_geocode':
            # Batch geocode multiple reports
            reports_json = sys.argv[2]
            reports_list = json.loads(reports_json)
            results = batch_geocode_reports(reports_list)
            print(json.dumps(results, ensure_ascii=False, indent=2))
            
        elif command == 'test':
            # Run a quick test
            print("Testing SeismIQ Geocoding Integration...")
            
            # Test reverse geocoding
            print("\n1. Reverse Geocoding Test:")
            location = find_location_from_coordinates(41.0082, 28.9784)
            print(f"   ({41.0082:.4f}, {28.9784:.4f}) -> {location['city']}, "
                  f"{location['province']}")

            # Test forward geocoding
            print("\n2. Forward Geocoding Test:")
            coords = find_coordinates_from_name("Istanbul")
            print(f"   Istanbul -> ({coords['latitude']}, "
                  f"{coords['longitude']})")

            # Test report geocoding
            print("\n3. Report Geocoding Test:")
            test_report = {
                "reportId": "test-123",
                "latitude": 39.9334,
                "longitude": 32.8597,
                "description": "Test earthquake report"
            }
            geocoded = geocode_report_data(test_report)
            print(f"   Added: city={geocoded['city']}, "
                  f"province={geocoded['province']}")
            
        else:
            print(f"Unknown command: {command}")
            
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)



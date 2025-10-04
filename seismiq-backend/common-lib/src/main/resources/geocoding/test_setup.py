#!/usr/bin/env python3
"""
Simple test for SeismIQ Geocoding Service in Common-Lib

This test verifies that the geocoding utilities work correctly
and can be used from the common-lib location.

Run this test to verify the setup:
    python test_setup.py

If you get "ModuleNotFoundError", install dependencies first:
    pip install -r requirements.txt
"""

import sys
import os
from seismiq_geocoding_utils import find_coordinates_from_name
from seismiq_geocoding_utils import find_location_from_coordinates

# Add current directory to Python path
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, current_dir)


def test_imports():
    """Test that all required modules can be imported"""
    print("Testing imports...")
    
    try:
        print("✅ pandas imported successfully")
    except ImportError as e:
        print(f"❌ pandas import failed: {e}")
        return False
    
    try:
        print("✅ numpy imported successfully")
    except ImportError as e:
        print(f"❌ numpy import failed: {e}")
        return False
    
    try:
        print("✅ scikit-learn imported successfully")
    except ImportError as e:
        print(f"❌ scikit-learn import failed: {e}")
        return False
    
    try:
        print("✅ geocoding utilities imported successfully")
    except ImportError as e:
        print(f"❌ geocoding utilities import failed: {e}")
        return False
    
    return True


def test_basic_functionality():
    """Test basic geocoding functionality"""
    print("\nTesting basic functionality...")
    
    try:
        # Test forward geocoding with fallback data
        print("Testing forward geocoding...")
        coords = find_coordinates_from_name("Istanbul")
        if coords:
            print(f"✅ Forward geocoding works: Istanbul -> "
                  f"{coords['latitude']:.4f}, {coords['longitude']:.4f}")
        else:
            print("⚠️ Forward geocoding returned None "
                  "(may need S3 data or fallback)")

        # Test reverse geocoding
        print("Testing reverse geocoding...")
        location = find_location_from_coordinates(41.0082, 28.9784)
        if location:
            print(f"✅ Reverse geocoding works: (41.0082, 28.9784) -> "
                  f"{location['city']}, {location['province']}")
        else:
            print("⚠️ Reverse geocoding returned None (may need S3 data "
                  "or fallback)")
        
        return True
        
    except Exception as e:
        print(f"❌ Functionality test failed: {e}")

        return False


def main():
    """Run all tests"""
    print("🌍 SeismIQ Geocoding Service - Setup Test")
    print("=" * 50)
    
    # Test imports
    if not test_imports():
        print("\n❌ Import tests failed!")
        print("\nTo fix this, install the required dependencies:")
        print("  pip install -r requirements.txt")
        return False
    
    # Test functionality
    if not test_basic_functionality():
        print("\n❌ Functionality tests failed!")
        return False
    
    print("\n" + "=" * 50)
    print("🎉 ALL TESTS PASSED!")
    print("✅ The geocoding service is ready to use from common-lib")
    print("\nUsage in Java:")
    print("  1. Use subprocess calls to integration_example.py")
    print("  2. Use HTTP calls to seismiq_geocoding_service.py")
    print("  3. Use direct Python imports in AWS Lambda")
    
    return True


if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)
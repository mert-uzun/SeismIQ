package com.seismiq.common.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service for geocoding operations using the Python geocoding utilities.
 * Provides forward geocoding (city/province -> coordinates) and 
 * reverse geocoding (coordinates -> city/province).
 *
 * @author Sıla Bozkurt
 */
public class GeoNamesGeocodingService {
    private static final Logger LOGGER = Logger.getLogger(GeoNamesGeocodingService.class.getName());
    private final Gson gson;
    private final String pythonScriptPath;
    private final String pythonExecutable;

    public GeoNamesGeocodingService() {
        this.gson = new Gson();
        // Default path to the Python script in resources
        this.pythonScriptPath = getClass().getClassLoader()
            .getResource("geocoding/seismiq_geocoding_utils.py").getPath();
        this.pythonExecutable = "python3"; // or "python" depending on system
    }

    /**
     * Constructor for testing with custom paths
     */
    public GeoNamesGeocodingService(String pythonExecutable, String pythonScriptPath) {
        this.gson = new Gson();
        this.pythonExecutable = pythonExecutable;
        this.pythonScriptPath = pythonScriptPath;
    }

    /**
     * Find coordinates for a given city name (forward geocoding)
     * 
     * @param cityName The name of the city
     * @param provinceName Optional province name for better accuracy
     * @return GeocodingResult with coordinates or null if not found
     */
    public GeocodingResult findCoordinatesFromName(String cityName, String provinceName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return null;
        }

        try {
            String pythonCode = String.format(
                "from seismiq_geocoding_utils import find_coordinates_from_name\n" +
                "import json\n" +
                "result = find_coordinates_from_name('%s', %s)\n" +
                "print(json.dumps(result) if result else 'null')",
                escapePythonString(cityName),
                provinceName != null ? "'" + escapePythonString(provinceName) + "'" : "None"
            );

            String result = executePythonCode(pythonCode);
            if (result != null && !result.equals("null")) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> data = gson.fromJson(result, type);
                return mapToGeocodingResult(data);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error in forward geocoding for city: " + cityName, e);
        }

        return null;
    }

    /**
     * Find location name for given coordinates (reverse geocoding)
     * 
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return GeocodingResult with location info or null if not found
     */
    public GeocodingResult findLocationFromCoordinates(double latitude, double longitude) {
        try {
            String pythonCode = String.format(
                "from seismiq_geocoding_utils import find_location_from_coordinates\n" +
                "import json\n" +
                "result = find_location_from_coordinates(%f, %f)\n" +
                "print(json.dumps(result) if result else 'null')",
                latitude, longitude
            );

            String result = executePythonCode(pythonCode);
            if (result != null && !result.equals("null")) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> data = gson.fromJson(result, type);
                return mapToGeocodingResult(data);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error in reverse geocoding for coordinates: " + 
                      latitude + ", " + longitude, e);
        }

        return null;
    }

    /**
     * Execute Python code and return the output
     */
    private String executePythonCode(String pythonCode) {
        try {
            // Create a temporary directory to run the Python script
            File scriptDir = new File(pythonScriptPath).getParentFile();
            
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "-c", pythonCode);
            pb.directory(scriptDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.warning("Python process timed out");
                return null;
            }

            if (process.exitValue() != 0) {
                LOGGER.warning("Python process failed with exit code: " + process.exitValue());
                LOGGER.warning("Output: " + output.toString());
                return null;
            }

            return output.toString().trim();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing Python code", e);
            return null;
        }
    }

    /**
     * Escape strings for Python code
     */
    private String escapePythonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("'", "\\'")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    /**
     * Convert Python result map to GeocodingResult object
     */
    private GeocodingResult mapToGeocodingResult(Map<String, Object> data) {
        if (data == null) return null;

        GeocodingResult result = new GeocodingResult();
        
        if (data.containsKey("latitude")) {
            result.setLatitude(((Number) data.get("latitude")).doubleValue());
        }
        if (data.containsKey("longitude")) {
            result.setLongitude(((Number) data.get("longitude")).doubleValue());
        }
        if (data.containsKey("city") || data.containsKey("name")) {
            result.setCity((String) data.getOrDefault("city", data.get("name")));
        }
        if (data.containsKey("province")) {
            result.setProvince((String) data.get("province"));
        }
        if (data.containsKey("distance_km")) {
            result.setDistanceKm(((Number) data.get("distance_km")).doubleValue());
        }
        if (data.containsKey("feature_code")) {
            result.setFeatureCode((String) data.get("feature_code"));
        }
        if (data.containsKey("population")) {
            result.setPopulation(((Number) data.get("population")).intValue());
        }

        return result;
    }

    /**
     * Result class for geocoding operations
     */
    public static class GeocodingResult {
        private double latitude;
        private double longitude;
        private String city;
        private String province;
        private double distanceKm; // For reverse geocoding
        private String featureCode;
        private int population;

        // Constructors
        public GeocodingResult() {}

        public GeocodingResult(double latitude, double longitude, String city, String province) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.city = city;
            this.province = province;
        }

        // Getters and setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

        public String getFeatureCode() { return featureCode; }
        public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }

        public int getPopulation() { return population; }
        public void setPopulation(int population) { this.population = population; }

        @Override
        public String toString() {
            return String.format("GeocodingResult{city='%s', province='%s', lat=%f, lon=%f, distance=%f km}",
                    city, province, latitude, longitude, distanceKm);
        }
    }
}
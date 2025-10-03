package com.seismiq.app.api;

import com.seismiq.app.model.Earthquake;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EarthquakeApiService {
    
    @GET("earthquakes")
    Call<List<Earthquake>> getEarthquakes();
    
    @GET("earthquakes/recent")
    Call<List<Earthquake>> getRecentEarthquakes();
    
    @POST("earthquakes")
    Call<Earthquake> createEarthquake(@Body Earthquake earthquake);
    
    @GET("earthquakes/{earthquakeId}")
    Call<Earthquake> getEarthquakeById(@Path("earthquakeId") String earthquakeId);
    
    @GET("earthquakes/location")
    Call<List<Earthquake>> getEarthquakesByLocation(
            @Query("latitude") double latitude, 
            @Query("longitude") double longitude, 
            @Query("radius") double radius);
}

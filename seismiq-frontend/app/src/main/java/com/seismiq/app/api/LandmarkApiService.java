package com.seismiq.app.api;

import com.seismiq.app.model.Landmark;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LandmarkApiService {
    
    @GET("landmarks")
    Call<List<Landmark>> getLandmarks();
    
    @POST("landmarks")
    Call<Landmark> createLandmark(@Body Landmark landmark);
    
    @GET("landmarks/{landmarkId}")
    Call<Landmark> getLandmarkById(@Path("landmarkId") String landmarkId);
    
    @PUT("landmarks/{landmarkId}")
    Call<Landmark> updateLandmark(@Path("landmarkId") String landmarkId, @Body Landmark landmark);
    
    @DELETE("landmarks/{landmarkId}")
    Call<Void> deleteLandmark(@Path("landmarkId") String landmarkId);
    
    @GET("landmarks/category/{category}")
    Call<List<Landmark>> getLandmarksByCategory(@Path("category") String category);
    
    @GET("landmarks/report/{reportId}")
    Call<List<Landmark>> getLandmarksByReport(@Path("reportId") String reportId);
    
    @GET("landmarks/location")
    Call<List<Landmark>> getLandmarksByLocation(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radius") double radius);
}

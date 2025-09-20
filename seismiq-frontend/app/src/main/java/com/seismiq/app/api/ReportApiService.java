package com.seismiq.app.api;

import com.seismiq.app.model.Report;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReportApiService {
    
    @GET("reports")
    Call<List<Report>> getReports();
    
    @POST("reports")
    Call<Report> createReport(@Body Report report);
    
    @GET("reports/{reportId}")
    Call<Report> getReportById(@Path("reportId") String reportId);
    
    @PUT("reports/{reportId}")
    Call<Report> updateReport(@Path("reportId") String reportId, @Body Report report);
    
    @DELETE("reports/{reportId}")
    Call<Void> deleteReport(@Path("reportId") String reportId);
    
    @PUT("reports/{reportId}/status")
    Call<Report> updateReportStatus(@Path("reportId") String reportId, @Body String status);
    
    @PUT("reports/{reportId}/location")
    Call<Report> updateReportLocation(
            @Path("reportId") String reportId, 
            @Query("latitude") double latitude, 
            @Query("longitude") double longitude);
    
    @GET("users/{userId}/reports")
    Call<List<Report>> getReportsByUserId(@Path("userId") String userId);
    
    @GET("reports/category/{category}")
    Call<List<Report>> getReportsByCategory(@Path("category") String category);
    
    @GET("reports/status/{status}")
    Call<List<Report>> getReportsByStatus(@Path("status") String status);
}

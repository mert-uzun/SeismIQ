package com.seismiq.app.api;

import com.seismiq.app.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserApiService {
    
    @POST("users")
    Call<User> createUser(@Body User user);
    
    @POST("users/login")
    Call<User> loginUser(@Body User loginRequest);
    
    @GET("users/{userId}")
    Call<User> getUserById(@Path("userId") String userId);
    
    @PUT("users/{userId}")
    Call<User> updateUser(@Path("userId") String userId, @Body User user);
    
    @DELETE("users/{userId}")
    Call<Void> deleteUser(@Path("userId") String userId);
}

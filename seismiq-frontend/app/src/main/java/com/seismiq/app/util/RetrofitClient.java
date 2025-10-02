package com.seismiq.app.util;

import com.seismiq.app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://pl6r0bf3zk.execute-api.eu-north-1.amazonaws.com/Prod/"; // AWS API Gateway endpoint
    private static Retrofit retrofit = null;
    
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Create HTTP client with authentication
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            
            // Add logging interceptor in debug builds
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.addInterceptor(logging);
            }
            
            // Add authentication token if available
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                
                // Get auth token from local storage
                String token = AuthManager.getAuthToken();
                
                if (token != null && !token.isEmpty()) {
                    Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .method(original.method(), original.body())
                        .build();
                    return chain.proceed(request);
                }
                
                return chain.proceed(original);
            });
            
            // Set timeouts
            httpClient.connectTimeout(30, TimeUnit.SECONDS);
            httpClient.readTimeout(30, TimeUnit.SECONDS);
            httpClient.writeTimeout(30, TimeUnit.SECONDS);
            
            // Build Retrofit instance
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        }
        
        return retrofit;
    }
}

package com.seismiq.app.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://f1kv8hjhqk.execute-api.eu-north-1.amazonaws.com/Prod/";

    public static Retrofit getClient(final String token) {
        // Always create a new client with the current token (don't cache)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        if (token != null && !token.isEmpty()) {
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            });
        }

        httpClient.addInterceptor(logging);

        // Configure Gson to handle ISO 8601 date formats in UTC
        JsonDeserializer<Date> dateDeserializer = (json, typeOfT, context) -> {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                String dateString = json.getAsString();
                // Handle both with and without fractional seconds
                if (dateString.contains(".")) {
                    dateString = dateString.substring(0, dateString.indexOf('.'));
                }
                return format.parse(dateString);
            } catch (Exception e) {
                return null;
            }
        };

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, dateDeserializer)
                .create();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();
    }
}

package com.example.safewomen.api;



import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.safewomen.utils.PreferenceManager;

public class ApiClient {
    // Update this to your PHP server URL
    private static final String BASE_URL = "https://your-php-server.com/safewomen/api/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            // Add auth token to all requests if available
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                String token = PreferenceManager.getInstance().getAuthToken();

                if (token != null && !token.isEmpty()) {
                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                }

                return chain.proceed(original);
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }
}
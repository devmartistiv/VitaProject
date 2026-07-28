package com.martist.vitamove.core.domain.utils;

import android.util.Log;

import com.martist.vitamove.auth.AuthManager;
import com.martist.vitamove.core.data.remote.SupabaseClient;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;


public class TokenRefreshInterceptor implements Interceptor {
    private static final String TAG = "TokenRefreshInterceptor";
    private final SupabaseClient supabaseClient;
    private final AuthManager authManager;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public TokenRefreshInterceptor(SupabaseClient supabaseClient, AuthManager authManager) {
        this.supabaseClient = supabaseClient;
        this.authManager = authManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Response response = chain.proceed(originalRequest);


        if (response.code() == 401) {
            synchronized (this) {

                response.close();

                Log.d(TAG, "Получен 401 Unauthorized, обновляем токен");


                if (isRefreshing.compareAndSet(false, true)) {
                    try {

                        boolean refreshed = authManager.refreshToken();

                        if (refreshed) {

                            Request newRequest = originalRequest.newBuilder()
                                    .removeHeader("Authorization")
                                    .addHeader("Authorization", "Bearer " + supabaseClient.getUserToken())
                                    .build();

                            Log.d(TAG, "Токен обновлен, повторяем запрос");

                            isRefreshing.set(false);

                            return chain.proceed(newRequest);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обновлении токена: " + e.getMessage(), e);
                    } finally {
                        isRefreshing.set(false);
                    }
                } else {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }


                    Request newRequest = originalRequest.newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", "Bearer " + supabaseClient.getUserToken())
                            .build();

                    return chain.proceed(newRequest);
                }
            }
        }

        return response;
    }
} 
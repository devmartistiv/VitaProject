package com.martist.vitamove.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.remote.SupabaseClient;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class AuthManager {
    private static final String TAG = "AuthManager";
    private static AuthManager instance;
    private final SupabaseClient supabaseClient;
    private final ReadWriteLock tokenLock = new ReentrantReadWriteLock();
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private static final String PREFS_NAME = "VitaMovePrefs";

    private AuthManager(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
    }

    public static synchronized AuthManager getInstance(SupabaseClient supabaseClient) {
        if (instance == null) {
            instance = new AuthManager(supabaseClient);
        }
        return instance;
    }


    public boolean refreshToken() {

        if (isRefreshing.getAndSet(true)) {
            Log.d(TAG, "Обновление токена уже выполняется в другом потоке");


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            isRefreshing.set(false);
            return true;
        }


        tokenLock.writeLock().lock();
        try {

            supabaseClient.refreshAccessToken();
            Log.d(TAG, "Токен успешно обновлен");
            return true;
        } catch (SupabaseClient.TokenInvalidatedException e) {

            Log.e(TAG, "Refresh token недействителен, требуется повторная авторизация", e);
            clearAuthData();
            redirectToLogin();
            return false;
        } catch (SupabaseClient.AuthException | IOException | JSONException e) {
            Log.e(TAG, "Ошибка при обновлении токена: " + e.getMessage(), e);


            if (e.getMessage() != null &&
                    (e.getMessage().contains("token") ||
                            e.getMessage().contains("auth") ||
                            e.getMessage().contains("401"))) {
                clearAuthData();
                redirectToLogin();
            }

            return false;
        } finally {
            tokenLock.writeLock().unlock();
            isRefreshing.set(false);
        }
    }


    private void clearAuthData() {
        try {
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .remove("accessToken")
                        .remove("refreshToken")
                        .apply();
            }


            supabaseClient.setUserToken(null);
            supabaseClient.setRefreshToken(null);

            Log.d(TAG, "Данные аутентификации очищены");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке данных аутентификации", e);
        }
    }


    private void redirectToLogin() {
        try {
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                Intent loginIntent = new Intent(context, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(loginIntent);

                Log.d(TAG, "Перенаправление на экран входа");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при перенаправлении на экран входа", e);
        }
    }

} 
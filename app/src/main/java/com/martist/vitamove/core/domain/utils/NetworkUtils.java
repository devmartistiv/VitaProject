package com.martist.vitamove.core.domain.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;


public class NetworkUtils {
    private static final String TAG = "NetworkUtils";


    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            Log.w(TAG, "isNetworkAvailable: context is null");
            return false;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            Log.w(TAG, "isNetworkAvailable: ConnectivityManager is null");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                Log.d(TAG, "isNetworkAvailable: No active network");
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                Log.d(TAG, "isNetworkAvailable: NetworkCapabilities is null");
                return false;
            }

            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            Log.d(TAG, "isNetworkAvailable: " + hasInternet);
            return hasInternet;
        } else {

            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            Log.d(TAG, "isNetworkAvailable (legacy): " + isConnected);
            return isConnected;
        }
    }


    public static String getNetworkType(Context context) {
        if (context == null) {
            return "NONE";
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return "NONE";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return "NONE";
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return "NONE";
            }

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WIFI";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "MOBILE";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "ETHERNET";
            }
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    return "WIFI";
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    return "MOBILE";
                }
            }
        }

        return "UNKNOWN";
    }


    public static boolean isNetworkError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String lowerMessage = errorMessage.toLowerCase();
        return lowerMessage.contains("unable to resolve host") ||
                lowerMessage.contains("no address associated with hostname") ||
                lowerMessage.contains("connection reset") ||
                lowerMessage.contains("connection timeout") ||
                lowerMessage.contains("timeout") ||
                lowerMessage.contains("network") ||
                lowerMessage.contains("connection") ||
                lowerMessage.contains("unreachable") ||
                lowerMessage.contains("failed to connect") ||
                lowerMessage.contains("no internet");
    }
}

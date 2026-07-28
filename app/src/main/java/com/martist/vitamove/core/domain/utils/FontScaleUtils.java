package com.martist.vitamove.core.domain.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;


public class FontScaleUtils {
    private static final String TAG = "FontScaleUtils";


    public static Context wrapContextWithFixedFontScale(Context context) {
        return new FixedFontScaleContextWrapper(context);
    }


    public static void applyFixedFontScale(TextView textView) {
        if (textView == null) return;

        Context context = textView.getContext();
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (configuration.fontScale != 1.0f) {
            float originalSize = textView.getTextSize();
            float scaleFactor = 1.0f / configuration.fontScale;

            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, originalSize * scaleFactor);
            Log.d(TAG, "Применен фиксированный масштаб к TextView: " + scaleFactor);
        }
    }


    public static float getFixedFontSizeInPixels(Context context, float sizeInSp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float fontScale = context.getResources().getConfiguration().fontScale;


        return sizeInSp * (metrics.densityDpi / 160f) * (1.0f / fontScale);
    }


    public static class FixedFontScaleContextWrapper extends ContextWrapper {

        public FixedFontScaleContextWrapper(Context base) {
            super(base);
            Log.d(TAG, "Создан контекст с фиксированным масштабом шрифта");
        }

        @Override
        public Resources getResources() {
            Resources resources = super.getResources();
            if (resources != null) {
                Configuration configuration = new Configuration(resources.getConfiguration());

                if (configuration.fontScale != 1.0f) {
                    Log.d(TAG, "Изменение масштаба шрифта с " + configuration.fontScale + " на 1.0f");
                    configuration.fontScale = 1.0f;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        return createConfigurationContext(configuration).getResources();
                    } else {
                        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                    }
                }
            }
            return resources;
        }
    }
} 
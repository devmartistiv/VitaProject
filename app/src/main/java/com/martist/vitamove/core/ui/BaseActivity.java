package com.martist.vitamove.core.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.martist.vitamove.core.domain.utils.FontScaleUtils;


public class BaseActivity extends AppCompatActivity {


    @Override
    protected void attachBaseContext(Context newBase) {

        super.attachBaseContext(FontScaleUtils.wrapContextWithFixedFontScale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
} 
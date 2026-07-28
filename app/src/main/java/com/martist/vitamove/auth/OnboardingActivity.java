package com.martist.vitamove.auth;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.BaseActivity;


public class OnboardingActivity extends BaseActivity {

    private TextView appTitle, appDescription;
    private CardView imageCard;
    private MaterialButton startButton;
    private TextView loginText;
    private TextView privacyPolicyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);


        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isFirstRun", false);
        editor.apply();


        initViews();


        setupPrivacyPolicyText();


        appTitle.setAlpha(0f);
        appDescription.setAlpha(0f);
        startButton.setAlpha(0f);
        loginText.setAlpha(0f);
        privacyPolicyText.setAlpha(0f);


        animateElements();


        setupClickListeners();
    }

    private void initViews() {
        appTitle = findViewById(R.id.text_vitamove);
        appDescription = findViewById(R.id.text_app_description);
        startButton = findViewById(R.id.container_button);
        loginText = findViewById(R.id.container_button1);
        privacyPolicyText = findViewById(R.id.text_privacy_policy);
    }

    private void setupPrivacyPolicyText() {
        String fullText = "Продолжая использовать Vitamove, вы принимаете политику конфиденциальности";
        SpannableString spannableString = new SpannableString(fullText);


        int startIndex = fullText.indexOf("политику конфиденциальности");
        int endIndex = startIndex + "политику конфиденциальности".length();


        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://project13585281.tilda.ws/"));
                startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(Color.BLUE);

                ds.setUnderlineText(true);
            }
        };


        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);


        privacyPolicyText.setText(spannableString);


        privacyPolicyText.setMovementMethod(LinkMovementMethod.getInstance());


        privacyPolicyText.setHighlightColor(0);
    }

    private void animateElements() {

        ObjectAnimator titleFadeIn = ObjectAnimator.ofFloat(appTitle, "alpha", 0f, 1f);
        ObjectAnimator descFadeIn = ObjectAnimator.ofFloat(appDescription, "alpha", 0f, 1f);
        ObjectAnimator buttonFadeIn = ObjectAnimator.ofFloat(startButton, "alpha", 0f, 1f);
        ObjectAnimator loginFadeIn = ObjectAnimator.ofFloat(loginText, "alpha", 0f, 1f);
        ObjectAnimator policyFadeIn = ObjectAnimator.ofFloat(privacyPolicyText, "alpha", 0f, 1f);


        titleFadeIn.setDuration(300);
        descFadeIn.setDuration(300);
        buttonFadeIn.setDuration(300);
        loginFadeIn.setDuration(300);
        policyFadeIn.setDuration(300);


        AnimatorSet allAnimations = new AnimatorSet();
        allAnimations.playTogether(
                titleFadeIn,
                descFadeIn,
                buttonFadeIn,
                loginFadeIn,
                policyFadeIn
        );

        allAnimations.setInterpolator(new DecelerateInterpolator());
        allAnimations.start();
    }

    private void setupClickListeners() {

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SurveyActivity.class);
            startActivity(intent);

            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });


        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);

            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }
} 
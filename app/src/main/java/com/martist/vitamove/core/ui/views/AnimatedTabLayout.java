package com.martist.vitamove.core.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.tabs.TabLayout;
import com.martist.vitamove.R;

public class AnimatedTabLayout extends TabLayout {
    private static final String TAG = "AnimatedTabLayout";
    private Paint indicatorPaint;
    private RectF indicatorRect;
    private int selectedPosition = 0;
    private float indicatorX = 0;
    private float indicatorWidth = 0;
    private ValueAnimator valueAnimator;
    private boolean isInitialized = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean pendingInitialAnimation = false;


    private static final long ANIMATION_DURATION = 250;


    private final float cornerRadius = 25f;

    private final float POSITION_OFFSET = -2f;

    public AnimatedTabLayout(Context context) {
        super(context);
        init();
    }

    public AnimatedTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        indicatorPaint = new Paint();
        setIndicatorColor();
        indicatorPaint.setAntiAlias(true);
        indicatorRect = new RectF();


        setTabRippleColorResource(android.R.color.transparent);


        setTabGravity(TabLayout.GRAVITY_FILL);
        setTabMode(TabLayout.MODE_FIXED);


        addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
                if (!isInitialized) {

                    pendingInitialAnimation = true;
                    selectedPosition = tab.getPosition();
                    return;
                }

                animateIndicator(selectedPosition, tab.getPosition());
                selectedPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(Tab tab) {

            }

            @Override
            public void onTabReselected(Tab tab) {

                pulsateIndicator();
            }
        });


        post(() -> {
            if (!isInitialized && getTabCount() > 0) {
                isInitialized = true;

                updateIndicator(selectedPosition, false);


                View selectedTabView = getTabView(selectedPosition);
                if (selectedTabView != null) {
                    TextView selectedTabText = findTabTextView(selectedTabView);
                    if (selectedTabText != null) {
                        selectedTabText.setScaleX(1.05f);
                        selectedTabText.setScaleY(1.05f);
                    }
                }

                if (pendingInitialAnimation) {
                    pendingInitialAnimation = false;
                }
            }
        });
    }


    private void setIndicatorColor() {
        int nightMode = getContext().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {

            indicatorPaint.setColor(ContextCompat.getColor(getContext(), R.color.tab_indicator_color));
        } else {

            indicatorPaint.setColor(ContextCompat.getColor(getContext(), R.color.tab_indicator_color));
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setIndicatorColor();
        invalidate();
    }

    private void updateIndicator(int position, boolean animate) {
        if (getTabCount() == 0) return;


        View tabView = getTabView(position);

        if (tabView == null) return;


        float targetX = tabView.getLeft() + POSITION_OFFSET;
        float targetWidth = tabView.getWidth();

        if (!animate) {

            indicatorX = targetX;
            indicatorWidth = targetWidth;
            invalidate();
            return;
        }


        if (valueAnimator != null && valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }

        final float startX = indicatorX;
        final float startWidth = indicatorWidth;
        final float endX = targetX;
        final float endWidth = targetWidth;


        valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            indicatorX = startX + fraction * (endX - startX);
            indicatorWidth = startWidth + fraction * (endWidth - startWidth);
            postInvalidateOnAnimation();
        });
        valueAnimator.start();
    }

    private void animateIndicator(int oldPosition, int newPosition) {

        updateIndicator(newPosition, true);


        animateTabTextScale(newPosition, oldPosition);
    }


    private void animateTabTextScale(int newPosition, int oldPosition) {
        View newTabView = getTabView(newPosition);
        View oldTabView = getTabView(oldPosition);

        if (newTabView == null || oldTabView == null) return;


        TextView newTabText = findTabTextView(newTabView);
        TextView oldTabText = findTabTextView(oldTabView);

        if (newTabText == null || oldTabText == null) return;


        ValueAnimator scaleUpAnimator = ValueAnimator.ofFloat(1.0f, 1.05f);
        scaleUpAnimator.setDuration(200);
        scaleUpAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleUpAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            newTabText.setScaleX(value);
            newTabText.setScaleY(value);
        });
        scaleUpAnimator.start();


        ValueAnimator scaleDownAnimator = ValueAnimator.ofFloat(1.05f, 1.0f);
        scaleDownAnimator.setDuration(200);
        scaleDownAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleDownAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            oldTabText.setScaleX(value);
            oldTabText.setScaleY(value);
        });
        scaleDownAnimator.start();
    }


    private TextView findTabTextView(View tabView) {
        if (tabView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) tabView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof TextView) {
                    return (TextView) child;
                } else if (child instanceof ViewGroup) {
                    TextView textView = findTabTextView(child);
                    if (textView != null) {
                        return textView;
                    }
                }
            }
        }
        return null;
    }


    private void pulsateIndicator() {
        if (valueAnimator != null && valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }

        final float startScale = 1.0f;
        final float middleScale = 0.95f;
        final float endScale = 1.05f;

        valueAnimator = ValueAnimator.ofFloat(startScale, middleScale, endScale, startScale);
        valueAnimator.setDuration(300);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            float currentWidth = indicatorWidth * scale;
            float deltaWidth = (indicatorWidth - currentWidth) / 2;
            indicatorRect.left = indicatorX + deltaWidth;
            indicatorRect.right = indicatorX + indicatorWidth - deltaWidth;
            postInvalidateOnAnimation();
        });
        valueAnimator.start();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed && isInitialized) {
            updateIndicator(selectedPosition, false);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        if (isInitialized && getTabCount() > 0) {

            View tabView = getTabView(selectedPosition);
            if (tabView != null) {

                indicatorRect.left = indicatorX;
                indicatorRect.right = indicatorX + indicatorWidth + (-2 * POSITION_OFFSET);


                indicatorRect.top = 0;
                indicatorRect.bottom = getHeight();


                canvas.drawRoundRect(indicatorRect, cornerRadius, cornerRadius, indicatorPaint);
            }
        }


        super.dispatchDraw(canvas);
    }


    @Override
    public void selectTab(Tab tab, boolean updateIndicator) {
        if (tab != null) {
            int newPosition = tab.getPosition();
            if (newPosition != selectedPosition) {

                if (!isInitialized) {
                    selectedPosition = newPosition;
                } else {
                    animateIndicator(selectedPosition, newPosition);
                    selectedPosition = newPosition;
                }
            }
        }
        super.selectTab(tab, updateIndicator);
    }


    private View getTabView(int position) {
        if (position < 0 || position >= getTabCount()) {
            return null;
        }
        ViewGroup tabStrip = (ViewGroup) getChildAt(0);
        if (tabStrip == null || position >= tabStrip.getChildCount()) {
            return null;
        }
        return tabStrip.getChildAt(position);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        post(() -> {
            if (!isInitialized && getTabCount() > 0) {
                isInitialized = true;

                updateIndicator(selectedPosition, false);
            }
        });
    }
} 
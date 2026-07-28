package com.martist.vitamove.core.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.martist.vitamove.R;


public class DevelopmentOverlay extends FrameLayout {

    public DevelopmentOverlay(Context context) {
        super(context);
        init(context);
    }

    public DevelopmentOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DevelopmentOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_in_development_overlay, this, true);


        TextView devText = view.findViewById(R.id.development_text);
        AlphaAnimation blinkAnimation = new AlphaAnimation(0.7f, 1.0f);
        blinkAnimation.setDuration(800);
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        blinkAnimation.setRepeatCount(Animation.INFINITE);
        devText.startAnimation(blinkAnimation);
    }


    public static void applyToView(View targetView) {
        if (targetView == null) return;

        Context context = targetView.getContext();
        DevelopmentOverlay overlay = new DevelopmentOverlay(context);

        if (targetView instanceof FrameLayout) {
            ((FrameLayout) targetView).addView(overlay);
        } else {

            FrameLayout parent = (FrameLayout) targetView.getParent();
            int index = parent.indexOfChild(targetView);

            FrameLayout wrapper = new FrameLayout(context);
            parent.removeView(targetView);
            wrapper.addView(targetView);
            wrapper.addView(overlay);
            parent.addView(wrapper, index);
        }


    }
} 
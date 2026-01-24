package com.example.cooking.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class LoadingDots extends LinearLayout {
    private static final int DOT_COUNT = 3;
    private static final int DEFAULT_DOT_SIZE_DP = 6;
    private static final int DEFAULT_DOT_SPACING_DP = 4;
    private static final int ANIMATION_DURATION_MS = 900;
    private static final float MIN_SCALE = 0.6f;
    private static final float MAX_SCALE = 1.0f;
    private static final float MIN_ALPHA = 0.4f;
    private static final float MAX_ALPHA = 1.0f;
    private static final float MAX_TRANSLATION_DP = 4f;

    private final View[] dots = new View[DOT_COUNT];
    private @Nullable ValueAnimator animator;

    public LoadingDots(Context context) {
        super(context);
        init(context);
    }

    public LoadingDots(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingDots(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.START);
        setClipChildren(false);
        setClipToPadding(false);
        int dotMargin = dpToPx(DEFAULT_DOT_SPACING_DP);
        int dotSize = dpToPx(DEFAULT_DOT_SIZE_DP);
        int dotColor = resolveDotColor(context);
        for (int i = 0; i < DOT_COUNT; i++) {
            View dot = new View(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dotSize, dotSize);
            lp.setMargins(dotMargin, 0, dotMargin, 0);
            dot.setLayoutParams(lp);
            dot.setBackground(createDotDrawable(dotColor));
            dots[i] = dot;
            addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void startAnimation() {
        if (animator != null && animator.isRunning()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            for (int i = 0; i < DOT_COUNT; i++) {
                float phase = (t + (i * 0.18f)) % 1f;
                float wave = (float) Math.sin(phase * Math.PI * 2);
                float normalized = (wave + 1f) / 2f;
                float scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * normalized;
                float alpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * normalized;
                float translationY = -dpToPx((int) MAX_TRANSLATION_DP) * normalized;
                View dot = dots[i];
                dot.setScaleX(scale);
                dot.setScaleY(scale);
                dot.setAlpha(alpha);
                dot.setTranslationY(translationY);
            }
        });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private int resolveDotColor(Context context) {
        int[] attrs = new int[] { android.R.attr.textColorSecondary };
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int color = ta.getColor(0, 0xFF888888);
        ta.recycle();
        return color;
    }

    private GradientDrawable createDotDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }
}

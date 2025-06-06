package com.example.cooking.ui.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;

public class LoadingDots extends LinearLayout {
    private static final int DOT_COUNT = 3;
    private final TextView[] dots = new TextView[DOT_COUNT];
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int currentDot = 0;

    private final Runnable waveRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < DOT_COUNT; i++) {
                float translationY = (i == currentDot) ? -dpToPx(4) : 0;
                dots[i].animate().translationY(translationY).setDuration(300);
            }
            currentDot = (currentDot + 1) % DOT_COUNT;
            handler.postDelayed(this, 300);
        }
    };

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
        int dotMargin = dpToPx(4);
        for (int i = 0; i < DOT_COUNT; i++) {
            TextView dot = new TextView(context);
            dot.setText("•");
            dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            dot.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(dotMargin, 0, dotMargin, 0);
            dot.setLayoutParams(lp);
            dots[i] = dot;
            addView(dot);
        }
        handler.post(waveRunnable);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(waveRunnable);
    }
}

package odesk.johnlife.skylight.ui;

import android.content.Context;
import android.util.AttributeSet;

import odesk.johnlife.skylight.Constants;

public class HintTextView extends BlurTextView implements Constants {

    public HintTextView(Context context) {
        super(context);
    }

    public HintTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HintTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void show(String text, int time) {
        super.show(text);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, time);
    }

    @Override
    public void show(String text) {
        show(text, HINT_TIME);
    }

}

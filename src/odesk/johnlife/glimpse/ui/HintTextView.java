package odesk.johnlife.glimpse.ui;

import android.content.Context;
import android.util.AttributeSet;

import odesk.johnlife.glimpse.Constants;

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

    @Override
    public void show(String text) {
        super.show(text);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, HINT_TIME);
    }

}

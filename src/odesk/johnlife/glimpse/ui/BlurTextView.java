package odesk.johnlife.glimpse.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;

public class BlurTextView extends BlurLayout {

    private TextView textView;

    public BlurTextView(Context context) {
        super(context);
        createView(context);
    }

    public BlurTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView(context);
    }

    public BlurTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createView(context);
    }

    private void createView(Context context) {
        inflate(getContext(), R.layout.text_view, this);
        textView = (TextView)findViewById(R.id.text_view);
    }

    public void show(String text) {
        textView.setText(text);
        setVisibility(View.VISIBLE);
    }

    public String getText() {
        return textView.getText().toString();
    }

    public void show(int res) {
        show(getContext().getString(res));
    }

    public void hide() {
        setVisibility(View.GONE);
    }

}

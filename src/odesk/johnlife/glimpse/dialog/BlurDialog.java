package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;

public abstract class BlurDialog extends FrameLayout {

    private TextView title;
    protected LinearLayout content;
    protected FrameLayout container;
    protected Button positiveButton, negativeButton;

    public BlurDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createView(context);
    }

    public BlurDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView(context);
    }

    public BlurDialog(Context context) {
        super(context);
        createView(context);
    }

    protected void createView(Context context) {
        inflate(context, R.layout.dialog, this);
        setOnTouchListener(getOutsideListener());
        content = (LinearLayout) findViewById(R.id.content);
        title = (TextView) content.findViewById(R.id.title);
        container = (FrameLayout) content.findViewById(R.id.container);
        positiveButton = (Button) content.findViewById(R.id.ok);
        negativeButton = (Button) content.findViewById(R.id.cancel);
        negativeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
    }

    private OnTouchListener getOutsideListener() {
        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cancel();
                return true;
            }
        };
    }

    protected void setTitle(String text) {
        title.setText(text);
    }

    protected void setTitle(int res) {
        title.setText(res);
    }

    public void setPositiveButtonListener(OnClickListener listener) {
        positiveButton.setOnClickListener(listener);
    }

    protected void setPositiveButtonText(int stringId) {
        positiveButton.setText(stringId);
    }

    protected void setDrawable(Button button, int res) {
        button.setCompoundDrawablesWithIntrinsicBounds(res, 0, 0, 0);
    }

    public void show() {
        setVisibility(View.VISIBLE);
        content.setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
        content.setVisibility(View.GONE);
    }

    protected void cancel() {
        hide();
    }

}
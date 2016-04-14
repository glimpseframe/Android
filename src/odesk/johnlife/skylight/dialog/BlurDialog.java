package odesk.johnlife.skylight.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import odesk.johnlife.skylight.Constants;
import odesk.johnlife.skylight.R;
import odesk.johnlife.skylight.activity.PhotoActivity;
import odesk.johnlife.skylight.app.SkylightApp;

public abstract class BlurDialog extends FrameLayout implements Constants {

    public interface OnCloseListener {
        void onClose();
    }

    private OnCloseListener onCloseListener;

    private TextView title;
    protected LinearLayout content;
    protected FrameLayout container;
    protected Button positiveButton, negativeButton;
    protected View buttons;

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
        buttons = content.findViewById(R.id.buttons);
        positiveButton = (Button) buttons.findViewById(R.id.ok);
        negativeButton = (Button) buttons.findViewById(R.id.cancel);
        negativeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });
        addCallback();
    }

    protected boolean canCloseOnTouchOutside() {
        return true;
    }

    private OnTouchListener getOutsideListener() {
        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (canCloseOnTouchOutside()) cancel();
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
        SkylightApp.getFileHandler().setPause(true);
    }

    public void hide() {
        setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        SkylightApp.getFileHandler().setPause(false);
    }

    public void setOnCloseListener(OnCloseListener onCloseListener) {
        this.onCloseListener = onCloseListener;
    }

    public boolean cancel() {
        if (getVisibility() == View.VISIBLE) {
            hide();
            if (onCloseListener != null) onCloseListener.onClose();
            return true;
        }
        return false;
    }

    private void addCallback() {
        if (canCloseOnTouchOutside()) ((PhotoActivity)getContext()).addDialogToList(this);
    }

}
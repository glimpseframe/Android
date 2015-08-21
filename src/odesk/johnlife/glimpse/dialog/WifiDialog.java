package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.util.WifiReceiver;

public class WifiDialog extends BlurDialog {

    private EditText password;

    public WifiDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WifiDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WifiDialog(Context context) {
        super(context);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        View view = inflate(context, R.layout.wifi_dialog, container);
        password = (EditText) view.findViewById(R.id.password);
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    connect();
                    return true;
                }
                return false;
            }
        });
        password.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
                if (hasFocus) {
                    imm.showSoftInput(password, InputMethodManager.SHOW_FORCED);
                } else {
                    imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
                }
            }
        });
        negativeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
        positiveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        setPositiveButtonText(R.string.connect);
    }

    @Override
    public void show() {
        password.setText("");
        setTitle(WifiReceiver.getInstance().getSelectedNetwork().SSID);
        super.show();
        password.requestFocus();
    }

    @Override
    public boolean cancel() {
        boolean isCanceled = super.cancel();
        if (isCanceled) WifiReceiver.getInstance().scanWifi();
        return isCanceled;
    }

    private void connect() {
        hide();
        WifiReceiver.getInstance().connectToSelectedNetwork(password.getText().toString());
    }

}
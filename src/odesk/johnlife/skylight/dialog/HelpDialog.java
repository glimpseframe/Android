package odesk.johnlife.skylight.dialog;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import odesk.johnlife.skylight.R;
import odesk.johnlife.skylight.activity.PhotoActivity;

public class HelpDialog extends BlurDialog {

    private Context context;

    public HelpDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public HelpDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public HelpDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        this.context = context;
        //TODO
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(getScreenWidth(0.75), getScreenHeight(0.75), Gravity.CENTER);
//        content.setLayoutParams(params);
        setTitle(R.string.how_it_works);
        inflate(context, R.layout.dialog_help, container);
        negativeButton.setVisibility(View.GONE);
        setPositiveButtonListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
        String email = ((PhotoActivity) context).getUser();
        setEmail(email);
        String version = "";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Get app version", e.getMessage(), e);
        }
        ((TextView) findViewById(R.id.app_version)).setText(context.getString(R.string.app_name_version, context.getString(R.string.app_name), version));
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            String address = context.getString(R.string.mac, info.getMacAddress());
            ((TextView)findViewById(R.id.mac)).setText(address);
        } catch (Exception e) {
            Log.e("Mac-address", "Error while getting mac-address", e);
        }
    }

    public void setEmail(String email) {
        if (email == null) return;
        SpannableString fullEmailText = new SpannableString(context.getString(R.string.how_it_works_email, email));
        int startBold = (fullEmailText.length() - email.length());
        int endBold = fullEmailText.length();
        fullEmailText.setSpan(new StyleSpan(Typeface.BOLD), startBold, endBold, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.textEmail)).setText(fullEmailText);
    }
}
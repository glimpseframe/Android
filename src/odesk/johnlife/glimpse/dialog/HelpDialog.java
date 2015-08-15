package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.activity.PhotoActivity;

public class HelpDialog extends BlurDialog {

    public HelpDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HelpDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HelpDialog(Context context) {
        super(context);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
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
        SpannableStringBuilder string = new SpannableStringBuilder(context.getString(R.string.how_it_works_email, ((PhotoActivity) context).getUser()));
        string.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                context.getString(R.string.how_it_works_email).length(), string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.textEmail)).setText(string);
        String version = "";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        ((TextView) findViewById(R.id.app_version)).setText(context.getString(R.string.app_name_version, context.getString(R.string.app_name), version));
    }

}
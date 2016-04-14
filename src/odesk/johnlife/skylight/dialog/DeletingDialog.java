package odesk.johnlife.skylight.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import odesk.johnlife.skylight.R;

public class DeletingDialog extends BlurDialog {

    public DeletingDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DeletingDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeletingDialog(Context context) {
        super(context);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        setTitle(R.string.delete_photo);
        setPositiveButtonText(R.string.delete);
        container.setVisibility(View.GONE);
        setDrawable(positiveButton, android.R.drawable.ic_menu_delete);
        setDrawable(negativeButton, android.R.drawable.ic_menu_revert);
    }

}
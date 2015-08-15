package odesk.johnlife.glimpse.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.adapter.ScanResultAdapter;
import odesk.johnlife.glimpse.util.WifiReceiver;

public class BlurListView extends BlurLayout {

    private ScanResultAdapter adapter;
    private ListView list;

    public BlurListView(Context context) {
        super(context);
        createView(context);
    }

    public BlurListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView(context);
    }

    public BlurListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createView(context);
    }

    private void createView(Context context) {
        inflate(context, R.layout.list_view, this);
        list = (ListView) findViewById(R.id.list);
        adapter = new ScanResultAdapter(context);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hide(false);
                WifiReceiver.getInstance().connectToNetwork(adapter.getItem(position));
            }
        });
    }

    public void update(List<ScanResult> result) {
        TreeSet<ScanResult> sortedResults = new TreeSet<ScanResult>(
                new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult lhs, ScanResult rhs) {
                        return -WifiManager.compareSignalLevel(lhs.level, rhs.level);
                    }
                });
        sortedResults.addAll(result);
        ArrayList<ScanResult> scanResults = new ArrayList<ScanResult>(sortedResults.size());
        TreeSet<String> nameLans = new TreeSet<String>();
        for (ScanResult net : sortedResults) {
            if (!net.SSID.trim().isEmpty() && nameLans.add(net.SSID)) scanResults.add(net);
        }
        adapter.clear();
        adapter.addAll(scanResults);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide(boolean withAnimation) {
        if (withAnimation) {
            animate().translationX(getWidth()).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                    setTranslationX(0);
                    setAlpha(1);
                    animate().setListener(null).start();
                }
            }).start();
        } else {
            setVisibility(View.GONE);
        }
    }
}
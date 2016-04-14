package odesk.johnlife.skylight.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import odesk.johnlife.skylight.R;
import odesk.johnlife.skylight.adapter.ScanResultAdapter;
import odesk.johnlife.skylight.util.WifiReceiver;

public class BlurListView extends BlurLayout {
    private static final Pattern bsidPattern = Pattern.compile("([a-z0-9A-Z]{2}:){5}[a-z0-9A-Z]{2}");


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
        Button reset = new Button(context);
        reset.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        reset.setText(R.string.forget_wifi);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiReceiver.getInstance().resetCurrentWifi();
                adapter.clear();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
                SharedPreferences.Editor editor = prefs.edit();
                Set<String> keys = prefs.getAll().keySet();
                for (String key : keys) {
                    if (bsidPattern.matcher(key).matches()) {
                        editor.remove(key);
                    }
                }
                editor.apply();
                WifiReceiver.getInstance().scanWifi();
            }
        });
        list.addFooterView(reset);
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

    public boolean isEmpty() {
        return adapter.isEmpty();
    }
}
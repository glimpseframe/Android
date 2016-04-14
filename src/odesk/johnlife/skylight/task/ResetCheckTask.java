package odesk.johnlife.skylight.task;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimerTask;

import odesk.johnlife.skylight.Constants;
import odesk.johnlife.skylight.activity.PhotoActivity;

/**
 * Created by yanyu on 3/7/2016.
 */
public class ResetCheckTask extends TimerTask {
    private static RequestQueue q = null;
    private PhotoActivity activity;

    public ResetCheckTask(PhotoActivity activity) {
        this.activity = activity;
        if (null == q) {
            q = Volley.newRequestQueue(activity);
        }
    }

    @Override
    public void run() {
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            String userName = prefs.getString(Constants.PREF_USER_EMAIL, null);
            if (null == userName) return;
            userName = userName.substring(0, userName.lastIndexOf('@'));
            final JSONObject json = new JSONObject();
            json.put("name", userName);
            q.add(new JsonObjectRequest(Constants.RESET_CHECK_URL, json, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonResponse) {
                    boolean doReset = jsonResponse.optBoolean("reset", false);
                    if (doReset) {
                        prefs.edit().remove(Constants.PREF_USER_EMAIL).commit();
                        activity.startActivity(new Intent(activity, PhotoActivity.class));
                        activity.finish();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Log.w("ResetChecker", volleyError.toString());
                }
            }));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

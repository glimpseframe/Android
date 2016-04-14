package odesk.johnlife.skylight.task;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import odesk.johnlife.skylight.Constants;
import ru.johnlife.lifetools.reporter.UpmobileExceptionReporter;

public class WifiRedirectionTask extends AsyncTask<Object, Object, Boolean> implements Constants {

	private static final String LOCATION = "Location";

	@Override
	protected Boolean doInBackground(Object... params) {
		boolean result = false;
		Log.d("Redirect", "Connected, checking for redirection");
        try {
            Thread.sleep(1500);
        } catch (Exception e1) {}
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) (new URL(WIFI_REDIRECT_URL).openConnection());
			con.setInstanceFollowRedirects(true);
			con.setUseCaches(false);
			con.setConnectTimeout(CONNECTION_TIMEOUT);
			con.setReadTimeout(CONNECTION_TIMEOUT);
            int status = -1;
			for (int i=0; i<9; i++) {
				try {
					con.connect();
                    status = con.getResponseCode();
                    break;
				} catch (IOException e) {
					try {
						Thread.sleep(1500);
					} catch (Exception e1) {}
				}
			}
            if (status == -1) {
                result = false;
            } else {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuilder json_string = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
					json_string.append(inputLine);
				}
				in.close();
				try {
					JSONObject json = new JSONObject(json_string.toString());
					result = json.getBoolean("passed");
				} catch (JSONException e) {
					result = false;
				}
			}
		} catch (IOException e) {
			Log.e("Redirection Task", e.getMessage(), e);
			UpmobileExceptionReporter.logIfAvailable(e);
			result = false;
		}
		return result;
	}
}

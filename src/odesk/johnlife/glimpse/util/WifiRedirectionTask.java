package odesk.johnlife.glimpse.util;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import odesk.johnlife.glimpse.Constants;

public class WifiRedirectionTask extends AsyncTask<Object, Object, Boolean> implements Constants {

	private static final String LOCATION = "Location";

	@Override
	protected Boolean doInBackground(Object... params) {
		boolean result = false;
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) (new URL(WIFI_REDIRECT_URL).openConnection());
			con.setInstanceFollowRedirects(true);
			con.setUseCaches(false);
			con.connect();
			String location;
			int status = con.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				location = con.getHeaderField(LOCATION);
				if (location != null && location.contains(GOOGLE)) {
					result = true;
				}
			} else {
				location = con.getURL().toString();
				result = location.contains(GOOGLE);
				if (result) {
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuilder html = new StringBuilder();
					while ((inputLine = in.readLine()) != null) {
						html.append(inputLine);
					}
					in.close();
					String htmlString = html.toString().toLowerCase(Locale.ENGLISH);
					for (String string : REDIRECTION_STRINGS) {
						if (htmlString.contains(string)) return false;
					}
					return true;
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			result = false;
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}
}

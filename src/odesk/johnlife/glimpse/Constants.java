package odesk.johnlife.glimpse;

import java.util.Arrays;
import java.util.List;

public interface Constants {

	String ACTION_WIFI_SCAN = "action.wifi.scan";

	String PREF_WIFI_BSSID = "wifi.bssid.pref";
	String PREF_WIFI_PASSWORD = "wifi.password.pref";
	String PREF_FIRST_LAUNCH = "glimpse.first.launch";

	String SSL_FACTORY = "odesk.johnlife.glimpse.util.AlwaysTrustSSLContextFactory";
	String EMAIL_SERVER = "mx.glimpseframe.com"; //pop.mail.ru
	String EMAIL_SUBJECT = "Someone liked a photo you sent to Glimpse.";
	String EMAIL_TEXT = "In the body is some dummy text that we'll fill in later + a thumbnail of the liked photo.";

	String WIFI_REDIRECT_URL = "http://google.com/";
	String GOOGLE = "google";
	List<String> REDIRECTION_STRINGS = Arrays.asList("authorization", "redirect", "wifi", "wi-fi", "sms", "mobnum", "mobpass");
	String DOMAIN = "@glimpseframe.com";

	int REFRESH_RATE = 10000;
	int HINT_TIME = 3000;
	int SCREEN_PAGE_LIMIT = 2;
	int RESCHEDULE_REFRESH_RATE = 8000;
	double IMAGE_SIZE_PERCENTAGE = 0.3;

}

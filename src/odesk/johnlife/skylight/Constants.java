package odesk.johnlife.skylight;

public interface Constants {

	String ACTION_WIFI_SCAN = "action.wifi.scan";

	String PREF_WIFI_BSSID = "wifi.bssid.pref";
	String PREF_FIRST_LAUNCH = "skylight.first.launch";
	String PREF_USER_EMAIL = "user.email";

	String SSL_FACTORY = "odesk.johnlife.skylight.util.AlwaysTrustSSLContextFactory";
	String EMAIL_SERVER = "mx.ourskylight.com";
	String EMAIL_POSTFIX = "@ourskylight.com";

	String WIFI_REDIRECT_URL = "http://ourskylight.com/check.json";
	String RESET_CHECK_URL = "http://ourskylight.com/api/v1/resetStatus";
	String JSON_ERROR = "error";
	String JSON_EMAIL = "name";

	int REFRESH_RATE = 10000;
	int CONNECTION_TIMEOUT = 15000;
	int HINT_TIME = 6000;
	int ERROR_RECOGNIZE_TIME = 10000;
	int SCREEN_PAGE_LIMIT = 2;
	int RESCHEDULE_REFRESH_RATE = 8000;
	int SKYLIGHT_CODE_LENGTH = 6;
	int MEMORY_COEFFICIENT = 3;
}

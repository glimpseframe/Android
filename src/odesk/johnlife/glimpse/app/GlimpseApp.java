package odesk.johnlife.glimpse.app;

import java.io.File;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.data.FileHandler;
import odesk.johnlife.glimpse.data.PictureData;
import odesk.johnlife.glimpse.util.DeviceScreen;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.FileObserver;
import android.preference.PreferenceManager;

import com.lazydroid.autoupdateapk.SilentAutoUpdate;

public class GlimpseApp extends Application implements Constants {

	private static File picturesDir;
	private static File tempDir;
	private static DeviceScreen screen;
	private static FileHandler fileHandler;
	private SilentAutoUpdate autoUpdater;
	private FileObserver directoryObserver;

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (!prefs.contains(PREF_FIRST_LAUNCH)) {
			//TODO backend
			prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).commit();
		}
		autoUpdater = new SilentAutoUpdate(this);
		picturesDir = getExternalFilesDir("pictures");
		picturesDir.mkdirs();
		tempDir = getCacheDir();
		screen = new DeviceScreen(this);
		fileHandler = new FileHandler(this);
		directoryObserver = new FileObserver(GlimpseApp.getPicturesDir().getAbsolutePath()) {
			@Override
			public void onEvent(int event, String path) {
				if (event == FileObserver.DELETE) {
					fileHandler.delete(new PictureData(GlimpseApp.getPicturesDir().getAbsolutePath() + "/" + path, ""));
				}
			}
		};
		directoryObserver.startWatching();
	}

	public static File getPicturesDir() {
		return picturesDir;
	}

	public static File getTempDir() {
		return tempDir;
	}

	public static DeviceScreen getScreen() {
		return screen;
	}

	public static FileHandler getFileHandler() {
		return fileHandler;
	}
}

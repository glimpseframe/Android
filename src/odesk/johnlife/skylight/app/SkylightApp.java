package odesk.johnlife.skylight.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.FileObserver;
import android.preference.PreferenceManager;

import com.lazydroid.autoupdateapk.SilentAutoUpdate;

import java.io.File;

import odesk.johnlife.skylight.Constants;
import odesk.johnlife.skylight.data.FileHandler;
import odesk.johnlife.skylight.data.PictureData;
import odesk.johnlife.skylight.util.DeviceScreen;
import ru.johnlife.lifetools.reporter.UpmobileExceptionReporter;

public class SkylightApp extends Application implements Constants {

	private static File picturesDir;
	private static File tempDir;
	private static DeviceScreen screen;
	private static FileHandler fileHandler;
	private SilentAutoUpdate autoUpdater;
	private FileObserver directoryObserver;

	@Override
	public void onCreate() {
		super.onCreate();
		Context context = getApplicationContext();
		Thread.setDefaultUncaughtExceptionHandler(UpmobileExceptionReporter.getInstance(context));
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.contains(PREF_FIRST_LAUNCH)) {
			//TODO backend
			prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).commit();
		}
		autoUpdater = new SilentAutoUpdate(this);
		picturesDir = getExternalFilesDir("pictures");
		picturesDir.mkdirs();
		tempDir = getCacheDir();
		screen = new DeviceScreen(this);
		fileHandler = new FileHandler(this, screen);
		directoryObserver = new FileObserver(SkylightApp.getPicturesDir().getAbsolutePath()) {
			@Override
			public void onEvent(int event, String path) {
				if (event == FileObserver.DELETE) {
					fileHandler.delete(new PictureData(SkylightApp.getPicturesDir().getAbsolutePath() + "/" + path, ""));
				}
			}
		};
		directoryObserver.startWatching();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		screen = new DeviceScreen(this);
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

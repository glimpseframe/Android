package odesk.johnlife.glimpse.data;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import odesk.johnlife.glimpse.app.GlimpseApp;

public class FileHandler {

	private boolean locked;
	private DatabaseHelper databaseHelper;
	private List<PictureData> files;
	private DataSetObserver datasetObserver;
	private Comparator<PictureData> comparator;
	private int nextPosition;

	public FileHandler(Context context) {
		databaseHelper = DatabaseHelper.getInstance(context);
		files = databaseHelper.getPictures();
		comparator = PictureData.TIME_COMPARATOR;
	}

	private synchronized void addFile(File file, String from) {
		cleanup(file.length());
		try {
			Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
			if (null == bmp) return; //not an image
			Bitmap scaled = scaleAndRotate(bmp, file);
			String path = new File(GlimpseApp.getPicturesDir(), "pic"+System.currentTimeMillis()+".jpg").getAbsolutePath();
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(path));
				scaled.compress(CompressFormat.JPEG, 85, out);
			} catch (IOException e) {
				Log.e(getClass().getName(), "Error writing scaled bitmap", e);
			} finally {
				try {
					out.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				out = null;
			}
			locked = true;
			PictureData picture = new PictureData(path, from);
			picture = databaseHelper.addOrUpdate(picture);
			files.add(picture);
			file.delete();
			locked = false;
		} catch (IllegalStateException e) {
//			PushLink.sendAsyncException(e);
		}
	}

	public synchronized void add(List<File> files, String from) {
		for (File file : files) {
			addFile(file, from);
		}
		resetCurrentPicture();
		notifyObserver();
	}

	public synchronized void add(File file, String from) {
		addFile(file, from);
		resetCurrentPicture();
		notifyObserver();
	}

	private Bitmap scaleAndRotate(Bitmap bmp, File file) {
		float w = bmp.getWidth();
		float h = bmp.getHeight();
		float s = Math.min(w, h);
		float l = Math.max(w, h);
		float sw = GlimpseApp.getScreen().getSmallestWidth();
		float lw = GlimpseApp.getScreen().getLargestWidth();
		float ratio = Math.min(1f, Math.max(sw/s, lw/l));
		Bitmap scaled = Bitmap.createScaledBitmap(bmp, (int) (ratio*w), (int) (ratio*h), true);
		Bitmap rotated = scaled;
		try {
			ExifInterface ei = new ExifInterface(file.getAbsolutePath());
			int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			Matrix mtx = new Matrix();
			int scaledW = scaled.getWidth();
			int scaledH = scaled.getHeight();
			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					//rotate CCW
					mtx.preRotate(90);
					rotated = Bitmap.createBitmap(scaled, 0, 0, scaledW, scaledH, mtx, true);
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					//rotate CCW
					mtx.preRotate(180);
					rotated = Bitmap.createBitmap(scaled, 0, 0, scaledW, scaledH, mtx, true);
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					//rotate CW
					mtx.preRotate(-90);
					rotated = Bitmap.createBitmap(scaled, 0, 0, scaledW, scaledH, mtx, true);
					break;
			}
		} catch (IOException e) {}
		return rotated;
	}

	public synchronized int delete(PictureData picture) {
		locked = true;
		databaseHelper.delete(picture);
		File file = new File(picture.getPath());
		file.delete();
		int deleted = 0;
		while (files.remove(picture)) {
			deleted++;
		};
		notifyObserver();
		locked = false;
		return deleted;
	}

	public void resetCurrentPicture() {
		nextPosition = 0;
	}

	public void show(PictureData picture) {
		picture.shown();
		databaseHelper.addOrUpdate(picture);
	}

	private void notifyObserver() {
		if (null != datasetObserver) {
			datasetObserver.onChanged();
		}
	}

	public void setObserver(DataSetObserver datasetObserver) {
		this.datasetObserver = datasetObserver;
	}

	public synchronized boolean isEmpty() {
		return files == null || files.isEmpty();
	}

	public synchronized int size() {
		if (null == files) return 0;
		return files.size();
	}

	public synchronized PictureData getLightest() {
		locked = true;
		PictureData value = Collections.min(files, PictureData.WEIGHT_COMPARATOR);
		locked = false;
		return value;
	}

	public synchronized PictureData getNext() {
		locked = true;
		nextPosition %= files.size();
		PictureData value = Collections.max(files.subList(0, files.size() - nextPosition++), comparator);
		locked = false;
		return value;
	}

	public boolean haveNeverSeen() {
		for (PictureData data : files) {
			if (data.isNeverSeen()) {
				return true;
			}
		}
		return false;
	}

	public synchronized void cleanup(long size) {
		locked = true;
		while (GlimpseApp.getPicturesDir().getUsableSpace() < size) {
			if (!files.isEmpty()) {
				PictureData victim = Collections.min(files, PictureData.TIME_COMPARATOR);
				delete(victim);
			} else {
				break;
			}
		}
		notifyObserver();
		locked = false;
	}

	public boolean isLocked() {
		return locked;
	}

	public List<PictureData> getFiles() {
		return files;
	}

	public void rewind(PictureData pictureData) {
		for (int i = 0; i < files.size(); i++) {
			if (getNext().equals(pictureData)) {
				nextPosition--;
			}
		}
	}

}
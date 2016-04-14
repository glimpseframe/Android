package odesk.johnlife.skylight.data;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import odesk.johnlife.skylight.Constants;
import odesk.johnlife.skylight.app.SkylightApp;
import odesk.johnlife.skylight.util.DeviceScreen;
import ru.johnlife.lifetools.reporter.UpmobileExceptionReporter;

public class FileHandler implements Constants {

	private final UpmobileExceptionReporter logger;
	private boolean locked;
	private DatabaseHelper databaseHelper;
	private List<PictureData> files;
	private DataSetObserver datasetObserver;
	private Comparator<PictureData> comparator;
	private int currentPosition;
	private int width, height;
	private boolean pause;

	public FileHandler(Context context, DeviceScreen screen) {
		logger = UpmobileExceptionReporter.getInstance(context);
		databaseHelper = DatabaseHelper.getInstance(context);
		files = databaseHelper.getPictures();
		width = screen.getSmallestWidth();
		height = screen.getLargestWidth();
		comparator = PictureData.TIME_COMPARATOR;
		Collections.sort(files, comparator);
	}

	private Bitmap decodeFile(File file) {
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getAbsolutePath(), o);
		int scale = 1;
		while (o.outWidth / width / scale >= 2 && o.outHeight / height / scale >= 2) {
			scale *= 2;
		}
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = scale;
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}

	private synchronized PictureData addFile(File file, String from) {
		cleanup(file.length());
		try {
			Bitmap bmp = decodeFile(file);
			if (null == bmp) return null; //not an image
			Bitmap scaled = scaleAndRotate(bmp, file);
			String path = new File(SkylightApp.getPicturesDir(), "pic"+System.currentTimeMillis()+".jpg").getAbsolutePath();
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(path));
				scaled.compress(CompressFormat.JPEG, 85, out);
			} catch (IOException e) {
				Log.e(getClass().getName(), "Error writing scaled bitmap", e);
				logger.logException(e);
			} finally {
				if (out != null) {
					try {
						out.flush();
					} catch (Exception e) {
						Log.e("Flushing OutputStream", e.getMessage(), e);
					}
					try {
						out.close();
					} catch (Exception e) {
						Log.e("Closing OutputStream", e.getMessage(), e);
					}
					out = null;
				}
			}
			locked = true;
			PictureData picture = new PictureData(path, from);
			picture = databaseHelper.addOrUpdate(picture);
			file.delete();
			scaled.recycle();
			bmp.recycle();
			locked = false;
			return picture;
		} catch (IllegalStateException e) {
			Log.e("Adding file", e.getMessage(), e);
			locked = false;
			return null;
//			PushLink.sendAsyncException(e);
		}
	}

	public synchronized void add(List<File> files, String from) {
		List<PictureData> addedFiles = new ArrayList();
		for (File file : files) {
			PictureData picture = addFile(file, from);
			if (null != picture) {
				addedFiles.add(picture);
			}
		}
		this.files.addAll(addedFiles);
		Collections.sort(this.files, comparator);
		resetCurrentPicture();
	}

	public File addToCache(MimeBodyPart bodyPart) throws IOException, MessagingException {
		File f = new File(SkylightApp.getTempDir(), bodyPart.getFileName());
		cleanup(bodyPart.getSize() * MEMORY_COEFFICIENT);
		bodyPart.saveFile(f);
		return f;
	}

	private Bitmap scaleAndRotate(Bitmap bmp, File file) {
		float w = bmp.getWidth();
		float h = bmp.getHeight();
		float s = Math.min(w, h);
		float l = Math.max(w, h);
		float sw = SkylightApp.getScreen().getSmallestWidth();
		float lw = SkylightApp.getScreen().getLargestWidth();
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

	public synchronized void delete(PictureData picture) {
		locked = true;
		databaseHelper.delete(picture);
		File file = new File(picture.getPath());
		if (!file.exists()) {
			locked = false;
			return;
		}
		file.delete();
		currentPosition = files.indexOf(picture);
		files.remove(picture);
		Collections.sort(files, comparator);
		if (currentPosition >= files.size() || currentPosition == -1) {
			currentPosition = 0;
		}
		notifyObserver();
		locked = false;
	}

	public void resetCurrentPicture() {
		if (pause) return;
		currentPosition = 0;
		notifyObserver();
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

	public void setPause(boolean pause) {
		this.pause = pause;
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

	public synchronized PictureData getNext(int position) {
		locked = true;
		if (files.size() <= Math.abs(position)) {
			position %= files.size();
		}
		currentPosition += position;
		if (currentPosition < 0) {
			currentPosition += files.size();
		} else if (currentPosition >= files.size()) {
			currentPosition -= files.size();
		}
		locked = false;
		return files.get(currentPosition);
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
		while (SkylightApp.getPicturesDir().getUsableSpace() < size) {
			if (!files.isEmpty()) {
				PictureData victim = files.get(files.size() - 1);
				delete(victim);
			} else {
				break;
			}
		}
		locked = false;
	}

	public boolean isLocked() {
		return locked;
	}

	public List<PictureData> getFiles() {
		return files;
	}

	public void rewind(PictureData pictureData) {
		currentPosition = files.indexOf(pictureData);
		notifyObserver();
	}
}
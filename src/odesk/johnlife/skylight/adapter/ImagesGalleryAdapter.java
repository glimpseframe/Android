package odesk.johnlife.skylight.adapter;

import odesk.johnlife.skylight.Constants;
import odesk.johnlife.skylight.app.SkylightApp;
import odesk.johnlife.skylight.data.FileHandler;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagesGalleryAdapter extends BaseAdapter implements Constants {

	private Context context;
	private FileHandler fileHandler;
	private final int SMALL_BITMAP_SIZE = 500;
	private int horizontalSpacing;
	private ExecutorService service;

	private static final int cacheSize = (int)(Runtime.getRuntime().maxMemory() / 1024) / 4;

	private static LruCache<String, Bitmap> mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
		@Override
		protected int sizeOf(String key, Bitmap bitmap) {
			return bitmap.getByteCount() / 1024;
		}
	};

	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}

	public ImagesGalleryAdapter(Context context) {
		this.context = context;
		this.fileHandler = SkylightApp.getFileHandler();
		this.horizontalSpacing = dpToPx(16);
		this.service = Executors.newFixedThreadPool(20);
	}

	@Override
	public int getCount() {
		return fileHandler.getFiles().size();
	}

	@Override
	public Object getItem(int position) {
		return fileHandler.getFiles().get(position);
	}

	@Override
	public long getItemId(int position) {
		return fileHandler.getFiles().get(position).hashCode();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ImageView view = new ImageView(context);
		final String path = fileHandler.getFiles().get(position).getPath();
		Bitmap bitmap = getBitmapFromMemCache(path);
		if (null == bitmap) {
			view.setVisibility(view.INVISIBLE);
			service.submit(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = resizeToSmall(BitmapFactory.decodeFile(path));
					addBitmapToMemoryCache(path, bitmap);
					setBitmap(view, bitmap);
				}
			});
		} else {
			view.setImageBitmap(bitmap);
		}
		int sizeWidth = SkylightApp.getScreen().getWidth();
		sizeWidth = (sizeWidth - horizontalSpacing * 4) / 3;
		view.setLayoutParams(new GridView.LayoutParams(sizeWidth, (int) (sizeWidth/1.5)));
		view.setScaleType(ImageView.ScaleType.CENTER_CROP);
		return view;
	}

	private void setBitmap(final ImageView view, final Bitmap bitmap) {
		((Activity)context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setImageBitmap(bitmap);
				AlphaAnimation animation = new AlphaAnimation(0, 1);
				animation.setDuration(250);
				animation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						view.setVisibility(View.VISIBLE);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
				view.startAnimation(animation);
			}
		});
	}

	private Bitmap resizeToSmall(Bitmap original) {
		int originalSize = Math.max(original.getWidth(), original.getHeight());
		float scale = 1;
		while (originalSize > SMALL_BITMAP_SIZE) {
			originalSize /= 2;
			scale /= 2;
		}
		Matrix m = new Matrix();
		m.postScale(scale, scale);
		return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), m, false);
	}

	private int dpToPx(int dp) {
		return Math.round(dp * ((float) SkylightApp.getScreen().getDensityDpi() / (float) DisplayMetrics.DENSITY_DEFAULT));
	}
}
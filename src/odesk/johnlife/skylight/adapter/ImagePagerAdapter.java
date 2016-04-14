package odesk.johnlife.skylight.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import java.io.File;

import odesk.johnlife.skylight.R;
import odesk.johnlife.skylight.activity.PhotoActivity;
import odesk.johnlife.skylight.app.SkylightApp;
import odesk.johnlife.skylight.data.DatabaseHelper;
import odesk.johnlife.skylight.data.FileHandler;
import odesk.johnlife.skylight.data.LikeSender;
import odesk.johnlife.skylight.data.PictureData;
import ru.johnlife.lifetools.reporter.UpmobileExceptionReporter;

public class ImagePagerAdapter extends PagerAdapter {

	private final UpmobileExceptionReporter logger;
	private Context context;
	private FileHandler fileHandler;
	private SparseArray<PictureData> pictures = new SparseArray<>();
	private SparseArray<Bitmap> bitmaps = new SparseArray<>();
	private OnClickListener onClickListener;
	private DatabaseHelper dbHelper;
	private boolean hasNewPhotos = false;
	private int lastPosition = -1;

	public ImagePagerAdapter(final Activity activity, DatabaseHelper databaseHelper, OnClickListener onClickListener) {
		super();
		this.context = activity;
		logger = UpmobileExceptionReporter.getInstance(context);
		this.onClickListener = onClickListener;
		dbHelper = databaseHelper;
		this.fileHandler = SkylightApp.getFileHandler();
		Log.d("Start - adapter", "Filehadler ready " + (fileHandler.isEmpty() ? "and empty" : ", pics: " + fileHandler.size()));
		fileHandler.setObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						lastPosition = -1;
						notifyDataSetChanged();
					}
				});

			}
		});
	}

	@Override
	public int getCount() {
		return Integer.MAX_VALUE;
	}

	private PictureData getItem(int position) {
		return pictures.get(position);
	}

	@Override
	public Object instantiateItem(ViewGroup pager, int position) {
		if (lastPosition == -1) {
			lastPosition = position;
		}
		int pos = position - lastPosition;
		lastPosition = position;
		ImageView image = new ImageView(context);
		FrameLayout frame = new FrameLayout(context);
		frame.addView(image);
		Bitmap bitmap;
		if (fileHandler.isEmpty()) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wp1);
			Log.d("Start - adapter", "Created view for startup screen");
		} else {
			final PictureData pictureData = fileHandler.getNext(pos);
			if (!new File(pictureData.getPath()).exists()) {
				fileHandler.delete(pictureData);
			}
			pictureData.viewCreated();
//			if (pictureData.createdToday()) {
//				ImageView  poster = new ImageView(context);
//				poster.setImageResource(R.drawable.new_pane);
//				poster.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.RIGHT));
//				frame.addView(poster);
//			}
			if (pictureData.getSenderAddress() != null) {
				frame.addView(createLikeButton(pictureData));
			}
			pictures.put(position, pictureData);
			bitmap = BitmapFactory.decodeFile(pictureData.getPath());
			Log.d("Start - adapter", "Created view for " + pictureData.getPath());
		}
		image.setImageBitmap(bitmap);
		image.setOnClickListener(onClickListener);
		setScaleType(image, bitmap);
		bitmaps.put(position, bitmap);
		pager.addView(frame);
		return frame;
	}

	private ImageView createLikeButton(final PictureData pictureData) {
		ImageView like = new ImageView(context);
		like.setImageResource(pictureData.getHeartState() ? R.drawable.solid_heart : R.drawable.heart);
		like.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT));
		int heartPadding = (int) context.getResources().getDimension(R.dimen.heart_padding);
		like.setPadding(heartPadding, heartPadding, heartPadding, heartPadding);
		if (!pictureData.getHeartState()) {
			like.setBackgroundResource(R.drawable.clear_clickable);
			like.setOnClickListener(new OnClickListener() {
				// only//only for first click
				@Override
				public void onClick(View v) {
					v.setOnClickListener(null);
					pictureData.setHeartState(true);
					dbHelper.addOrUpdate(pictureData);
					((ImageView) v).setImageResource(R.drawable.solid_heart);
					if (fileHandler.size() == 2) {
						notifyDataSetChanged();
					}
					((PhotoActivity) context).showHint(context.getString(R.string.hint_like_is_clicked, pictureData.getSenderAddress()));
					new AsyncTask<Object, String, Boolean>() {
						@Override
						protected Boolean doInBackground(Object... params) {
							try {
								new LikeSender().sendLike(context, pictureData.getSenderAddress(), pictureData.getPath());
							} catch (Exception e) {
								Log.e("Sending like", e.getMessage(), e);
								logger.logException(e);
								return false;
							}
							return true;
						}
					}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			});
		}
		return like;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
		Bitmap bmp = bitmaps.get(position);
		if (null != bmp) {
			bmp.recycle();
			bitmaps.remove(position);
		}
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

	@Override
	public int getItemPosition(Object object){
		return PagerAdapter.POSITION_NONE;
	}


	private void setScaleType(ImageView imageView, Bitmap bitmap) {
		if (bitmap != null) {
			int height = bitmap.getHeight();
			int width = bitmap.getWidth();
			int currentOrientation = context.getResources().getConfiguration().orientation;
			if ((height > width && currentOrientation == Configuration.ORIENTATION_PORTRAIT)
					|| (height < width && currentOrientation == Configuration.ORIENTATION_LANDSCAPE)) {
				imageView.setScaleType(ScaleType.CENTER_CROP);
			} else {
				imageView.setScaleType(ScaleType.FIT_CENTER);
			}
		}
	}

	public void deleteCurrentItem(ViewPager pager) {
		if (fileHandler.isEmpty()) return;
		int position = pager.getCurrentItem();
		PictureData item = getItem(position);
		if (null == item) return;
		fileHandler.delete(item);
	}

	public void setImageShown(int position) {
		PictureData item = getItem(position);
		if (null == item) return;
		fileHandler.show(item);
	}

	public void setHasNewPhotos(boolean newPhotos) {
		hasNewPhotos = newPhotos;
	}

	public boolean hasNewPhotos() {
		return hasNewPhotos;
	}

	public void checkNewPhotos() {
		if (fileHandler.isEmpty()) {
			hasNewPhotos = false;
			return;
		}
		if (fileHandler.haveNeverSeen()) {
			hasNewPhotos = true;
		}
//		if (pictureData.createdToday()) {
//			hasNewPhotos = true;
//		}
	}
}

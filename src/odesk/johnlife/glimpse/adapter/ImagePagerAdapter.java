package odesk.johnlife.glimpse.adapter;

import java.io.File;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.activity.PhotoActivity;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.DatabaseHelper;
import odesk.johnlife.glimpse.data.FileHandler;
import odesk.johnlife.glimpse.data.PictureData;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class ImagePagerAdapter extends PagerAdapter {

	private Context context;
	private FileHandler fileHandler;
	private SparseArray<PictureData> pictures;
	private OnClickListener onClickListener;
	DatabaseHelper dbHelper;
	private boolean hasNewPhotos = false;
	
	public ImagePagerAdapter(final Activity activity, DatabaseHelper databaseHelper, OnClickListener onClickListener) {
		super();
		this.context = activity;
		this.onClickListener = onClickListener;
		dbHelper = databaseHelper;
		this.fileHandler = GlimpseApp.getFileHandler();
		Log.d("Start - adapter", "Filehadler ready "+(fileHandler.isEmpty() ? "and empty" : ", pics: "+fileHandler.size()));
		fileHandler.setObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				if (fileHandler.isEmpty()) {
					activity.startActivity(new Intent(activity, activity.getClass()));
					activity.finish();
				}/* else {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							notifyDataSetChanged();
						}
					});
				}*/
			}
		});
		this.pictures = new SparseArray<PictureData>(fileHandler.size());
		fileHandler.resetCurrentPicture();
	}

	@Override
	public int getCount() {
		return fileHandler.size() < 2 ? 1 : Integer.MAX_VALUE;
//		return Math.max(1, fileHandler.size());	
	}
	
	private PictureData getItem(int position) {
		return pictures.get(position);
	}
	
	@Override
	public Object instantiateItem(ViewGroup pager, int position) {
		ImageView image = new ImageView(context);
		FrameLayout frame = new FrameLayout(context);
		frame.addView(image);
		Bitmap bitmap;
		if (fileHandler.isEmpty()) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wp1);
			Log.d("Start - adapter", "Created view for startup screen");
		} else {
			final PictureData pictureData = fileHandler.getNext(PictureData.TIME_COMPARATOR);
			if (!new File(pictureData.getPath()).exists()) {
				fileHandler.delete(pictureData);
			}
			pictureData.viewCreated();
			if (pictureData.createdToday()) {
				ImageView poster = new ImageView(context);
				poster.setImageResource(R.drawable.new_pane);
				poster.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.RIGHT));
				frame.addView(poster);
			}
			ImageView like = new ImageView(context);
			like.setImageResource(pictureData.getHeartState() ? R.drawable.solid_heart : R.drawable.heart);
			like.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM|Gravity.RIGHT));
			frame.addView(like);
			if (!pictureData.getHeartState()) {
				like.setOnClickListener(new OnClickListener() {
					// only//only for first click
					@Override
					public void onClick(View v) {
						v.setOnClickListener(null);
						pictureData.setHeartState(true);
						dbHelper.addOrUpdate(pictureData);
						((ImageView) v).setImageResource(R.drawable.solid_heart);
						((PhotoActivity) context).showHint(R.string.hint_like_is_clicked);
					}
				});
			}
			pictures.put(position, pictureData);
			bitmap = BitmapFactory.decodeFile(pictureData.getPath());
			Log.d("Start - adapter", "Created view for "+pictureData.getPath());
		}
		image.setImageBitmap(bitmap);
		image.setOnClickListener(onClickListener);
		setScaleType(image, bitmap);
		pager.addView(frame);
		return frame;		
	}


	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
		pictures.remove(position);
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
			if (
				(height > width && currentOrientation == Configuration.ORIENTATION_PORTRAIT) || 
				(height < width && currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
			) {
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
		int deleted = fileHandler.delete(item);
		notifyDataSetChanged();
		pager.setCurrentItem(position-deleted);
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
		PictureData pictureData = fileHandler.getNext(PictureData.TIME_COMPARATOR);
		fileHandler.resetCurrentPicture();
		if (!new File(pictureData.getPath()).exists()) {
			fileHandler.delete(pictureData);
		}
		if (pictureData.createdToday()) {
			hasNewPhotos = true;
		}
	}
}
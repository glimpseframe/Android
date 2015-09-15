package odesk.johnlife.glimpse.adapter;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.FileHandler;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImagesGalleryAdapter extends BaseAdapter implements Constants {

	private Context context;
	private FileHandler fileHandler;

	public ImagesGalleryAdapter(Context context) {
		this.context = context;
		this.fileHandler = GlimpseApp.getFileHandler();
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
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView view = new ImageView(context);
		Bitmap bitmap = BitmapFactory.decodeFile(fileHandler.getFiles().get(position).getPath());
		view.setImageBitmap(bitmap);
		int sizeHeight = GlimpseApp.getScreen().getHeight();
		int sizeWidth = GlimpseApp.getScreen().getWidth();
		sizeHeight /= sizeHeight > sizeWidth ? 6 : 3;
		sizeWidth /= 3;
		view.setLayoutParams(new GridView.LayoutParams(sizeWidth, sizeHeight));
		view.setScaleType(ImageView.ScaleType.FIT_XY);
		return view;
	}
	
}
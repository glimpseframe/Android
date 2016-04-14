package odesk.johnlife.skylight.data;

import android.content.Context;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;

import odesk.johnlife.skylight.R;

public class LikeSender {

	private static final String EMAIL = "email";
	private static final String FILE = "file";

	public void sendLike(Context context, String recipient, String picturePath) throws Exception {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(context.getString(R.string.like_url));
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		entityBuilder.addTextBody(EMAIL, recipient);
		entityBuilder.addBinaryBody(FILE, new File(picturePath));
		httpPost.setEntity(entityBuilder.build());
		httpclient.execute(httpPost);
	}

}
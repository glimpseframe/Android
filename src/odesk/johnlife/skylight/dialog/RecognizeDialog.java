package odesk.johnlife.skylight.dialog;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import odesk.johnlife.skylight.R;
import ru.johnlife.lifetools.reporter.UpmobileExceptionReporter;

public class RecognizeDialog extends BlurDialog {

    private final UpmobileExceptionReporter logger;

    public interface OnCodeAssociatingListener {
        void onCodeAssociated(String email);
        void onCodeRefused(String error);
    }

    private EditText recognize;
    private View progress;

    public RecognizeDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        logger = UpmobileExceptionReporter.getInstance(context);
    }

    public RecognizeDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        logger = UpmobileExceptionReporter.getInstance(context);
    }

    public RecognizeDialog(Context context) {
        super(context);
        logger = UpmobileExceptionReporter.getInstance(context);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        inflate(context, R.layout.dialog_recognize, container);
        buttons.setVisibility(View.GONE);
        setTitle(R.string.recognize_title);
        progress = findViewById(R.id.progress);
        recognize = (EditText) findViewById(R.id.recognize);
        recognize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == SKYLIGHT_CODE_LENGTH) {
                    recognize.setVisibility(View.GONE);
                    progress.setVisibility(View.VISIBLE);
                    sendCode(s.toString());
                }
            }
        });
        recognize.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
                if (hasFocus) {
                    imm.showSoftInput(recognize, InputMethodManager.SHOW_FORCED);
                } else {
                    imm.hideSoftInputFromWindow(recognize.getWindowToken(), 0);
                }
            }
        });
    }

    private void sendCode(String code) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                HttpClient httpclient = new DefaultHttpClient();
                InputStream in = null;
                BufferedReader reader = null;
                try {
                    HttpGet httpGet = new HttpGet(getContext().getString(R.string.server_url, params[0]));
                    HttpResponse response = httpclient.execute(httpGet);
                    in = response.getEntity().getContent();
                    StringBuilder stringbuilder = new StringBuilder();
                    reader = new BufferedReader(new InputStreamReader(in), 1024);
                    String line;
                    while((line = reader.readLine()) != null) stringbuilder.append(line);
                    JSONObject json = new JSONObject(stringbuilder.toString());
                    if (json.has(JSON_EMAIL)) {
                        return json.getString(JSON_EMAIL) + EMAIL_POSTFIX;
                    } else if (json.has(JSON_ERROR)) {
                        return null;
                    }
                } catch (Exception e) {
                    Log.e("Sending code", e.getMessage(), e);
                    logger.logException(e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception e) {
                            Log.e("Closing InputStream", e.getMessage(), e);
                        }
                        in = null;
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            Log.e("Closing BufferedReader", e.getMessage(), e);
                        }
                        reader = null;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String email) {
                OnCodeAssociatingListener listener = ((OnCodeAssociatingListener) getContext());
                if (email == null) {
                    progress.setVisibility(View.GONE);
                    recognize.setText("");
                    recognize.setVisibility(View.VISIBLE);
                    listener.onCodeRefused(getContext().getString(R.string.recognize_error));
                } else {
                    hide();
                    listener.onCodeAssociated(email);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, code);
    }

    @Override
    protected boolean canCloseOnTouchOutside() {
        return false;
    }

    @Override
    public void show() {
        super.show();
        recognize.requestFocus();
    }

}
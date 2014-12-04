package com.intel.yamba;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;


public class StatusActivity extends Activity implements TextWatcher, View.OnClickListener {
    private static final int            MAX_STATUS_LEN = 140;
    private static final int            WARNING_STATUS_LEN = 10;

    private TextView            mRemainText;
    private EditText            mStatus;
    private Button              mSubmitBtn;
    private Button              mSettingsBtn;
    private int                 mDefaultTextColor;
    private YambaSubmitterTask  mTmpTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        mStatus = (EditText)findViewById(R.id.status_text);
        mRemainText = (TextView)findViewById(R.id.status_remain);
        mRemainText.setText(Integer.toString(MAX_STATUS_LEN));
        mDefaultTextColor = mRemainText.getCurrentTextColor();
        mStatus.addTextChangedListener(this);

        mSubmitBtn = (Button)findViewById(R.id.submit_status);
        mSubmitBtn.setOnClickListener(this);

        mSettingsBtn = (Button)findViewById(R.id.btn_settings);
        mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettingsActivity();
            }
        });
    }

    private void startSettingsActivity() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    @Override
    protected void onPause() {
        //  Make sure the AsyncTask (if it exists) is canceled)
        if (mTmpTask != null) {
            mTmpTask.cancel(true);
            mTmpTask = null;
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //  don't care
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //  don't care
    }

    @Override
    public void afterTextChanged(Editable s) {
        //  See how much is remaining
        int remain = MAX_STATUS_LEN - s.length();
        if (remain < WARNING_STATUS_LEN) {
            mRemainText.setTextColor(Color.RED);
            if (remain < 0) {
                mSubmitBtn.setEnabled(false);
            } else {
                mSubmitBtn.setEnabled(true);
            }
        } else {
            mRemainText.setTextColor(mDefaultTextColor);
        }

        mRemainText.setText(Integer.toString(remain));
    }

    @Override
    public void onClick(View v) {
        String statusText = mStatus.getText().toString();
        mSubmitBtn.setEnabled(false);
        mTmpTask = new YambaSubmitterTask();
        mTmpTask.execute(statusText);
    }

    private class YambaSubmitterTask extends AsyncTask<String, Void, String> {
        ProgressDialog          mProgDlg;

        @Override
        protected String doInBackground(String... params) {
            String status = params[0];
            String result = StatusActivity.this.getString(R.string.success);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StatusActivity.this);
            String tmp = StatusActivity.this.getString(R.string.username_key);
            String username = prefs.getString(tmp, "");
            tmp = StatusActivity.this.getString(R.string.password_key);
            String pw = prefs.getString(tmp, "");
            tmp = StatusActivity.this.getString(R.string.api_uri_key);
            String apiUri = prefs.getString(tmp, "");
            if ((pw.length() == 0) || (username.length() == 0) || (apiUri.length() == 0)) {
                startSettingsActivity();
                return StatusActivity.this.getString(R.string.fail);
            }

            YambaClient client = new YambaClient(username, pw, apiUri);
            try {
                client.postStatus(status);
            } catch (YambaClientException e) {
                e.printStackTrace();
                result = StatusActivity.this.getString(R.string.fail);
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String title = StatusActivity.this.getString(R.string.progress_title);
            String msg = StatusActivity.this.getString(R.string.progress_msg);
            mProgDlg = ProgressDialog.show(StatusActivity.this, title, msg, true);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgDlg.dismiss();
            Toast.makeText(StatusActivity.this, s, Toast.LENGTH_LONG).show();
            mStatus.setText("");
            mSubmitBtn.setEnabled(true);
            mTmpTask = null;
        }

        @Override
        protected void onCancelled() {
            mProgDlg.dismiss();
            mSubmitBtn.setEnabled(true);
            super.onCancelled();
        }
    }

}

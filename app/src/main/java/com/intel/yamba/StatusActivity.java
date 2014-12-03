package com.intel.yamba;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
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


public class StatusActivity extends ActionBarActivity implements TextWatcher,
                                                                 View.OnClickListener,
                                                                 Handler.Callback {
    private static final boolean        USE_ASYNC_TASK = false;
    private static final int            MAX_STATUS_LEN = 140;
    private static final int            WARNING_STATUS_LEN = 10;

    private static final int            MSG_UI_STATUS_DONE = 1;

    private TextView            mRemainText;
    private EditText            mStatus;
    private Button              mSubmitBtn;
    private int                 mDefaultTextColor;
    private YambaSubmitterTask  mTmpTask;
    ProgressDialog              mProgDlg;

    private Handler             mHandler = new Handler(this);
    private YambaPostWorkerThread mWorkerThread;

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

        if (!USE_ASYNC_TASK) {
            mWorkerThread = new YambaPostWorkerThread(this, "post_worker");
            mWorkerThread.start();
        }
    }

    @Override
    protected void onPause() {
        //  Make sure the AsyncTask (if it exists) is canceled)
        if (USE_ASYNC_TASK) {
            if (mTmpTask != null) {
                mTmpTask.cancel(true);
                mTmpTask = null;
            }
        } else {
            //  Stop the worker from posting if it is currently doing so
            mWorkerThread.interrupt();
        }

        if (mProgDlg != null) {
            mProgDlg.dismiss();
            mProgDlg = null;
            mSubmitBtn.setEnabled(true);
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (!USE_ASYNC_TASK) {
            mWorkerThread.shutdown();
        }

        super.onDestroy();
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

        //  TODO: Enforce the MAX_STATUS_LEN bounds check!

        if (USE_ASYNC_TASK) {
            mTmpTask = new YambaSubmitterTask();
            mTmpTask.execute(statusText);
        } else {
            mWorkerThread.postStatus(statusText);
        }

        String title = getString(R.string.progress_title);
        String msg = getString(R.string.progress_msg);
        mProgDlg = ProgressDialog.show(this, title, msg, true);
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean ret = false;

        switch (msg.what) {
            case MSG_UI_STATUS_DONE:
                String statusMsg = (String)msg.obj;
                Toast.makeText(this, statusMsg, Toast.LENGTH_LONG).show();
                mProgDlg.dismiss();
                mSubmitBtn.setEnabled(true);
                mStatus.setText("");
                ret = true;
                break;
        }

        return ret;
    }

    public void postDone(String result) {
        Message msg = mHandler.obtainMessage(MSG_UI_STATUS_DONE, result);
        msg.sendToTarget();
    }

    private class YambaSubmitterTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String status = params[0];
            String result = StatusActivity.this.getString(R.string.success);
            YambaClient client = new YambaClient("student", "password");
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
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(StatusActivity.this, s, Toast.LENGTH_LONG).show();
            mStatus.setText("");
            mSubmitBtn.setEnabled(true);
            mTmpTask = null;
        }

        @Override
        protected void onCancelled() {
            mSubmitBtn.setEnabled(true);
            super.onCancelled();
        }
    }

}

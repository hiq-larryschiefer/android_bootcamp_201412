package com.intel.yamba;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;

public class StatusFragment extends Fragment implements TextWatcher, View.OnClickListener {
    public static final String          TAG = "StatusFragment";

    /**
     * This interface is required for all hosting Activities.  Implement it. Be Happy.
     */
    public interface StatusRequestCallback  {
        public void needSettings();
    }

    private static final int            MAX_STATUS_LEN = 140;
    private static final int            WARNING_STATUS_LEN = 10;

    private TextView mRemainText;
    private EditText mStatus;
    private Button mSubmitBtn;
    private Button              mSettingsBtn;
    private int                 mDefaultTextColor;
    private YambaSubmitterTask  mTmpTask;
    private StatusRequestCallback   mCallback;

    public StatusFragment() {
    }

    public static StatusFragment createInstance() {
        return new StatusFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (StatusRequestCallback)activity;
        } catch (ClassCastException e) {
            Log.d(TAG, "Invalid host activity, no callback included");
            throw e;//new ClassCastException(e.getMessage());
        }
    }

    @Override
    public void onPause() {
        //  Make sure the AsyncTask (if it exists) is canceled)
        if (mTmpTask != null) {
            mTmpTask.cancel(true);
            mTmpTask = null;
        }

        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View                    root;

        root = inflater.inflate(R.layout.fragment_status, container, false);

        mStatus = (EditText)root.findViewById(R.id.status_text);
        mRemainText = (TextView)root.findViewById(R.id.status_remain);
        mRemainText.setText(Integer.toString(MAX_STATUS_LEN));
        mDefaultTextColor = mRemainText.getCurrentTextColor();
        mStatus.addTextChangedListener(this);

        mSubmitBtn = (Button)root.findViewById(R.id.submit_status);
        mSubmitBtn.setOnClickListener(this);

        mSettingsBtn = (Button)root.findViewById(R.id.btn_settings);
        mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.needSettings();
            }
        });

        return root;
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
        ProgressDialog mProgDlg;
        String mUsername;
        String mPw;
        String mUri;
        Activity mAct;

        @Override
        protected String doInBackground(String... params) {
            String status = params[0];
            String result = mAct.getString(R.string.fail);

            if (!isCancelled()) {
                YambaClient client = new YambaClient(mUsername, mPw, mUri);
                try {
                    client.postStatus(status);
                    result = mAct.getString(R.string.success);
                } catch (YambaClientException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mAct = getActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mAct);
            String tmp = mAct.getString(R.string.username_key);
            mUsername = prefs.getString(tmp, "");
            tmp = mAct.getString(R.string.password_key);
            mPw = prefs.getString(tmp, "");
            tmp = mAct.getString(R.string.api_uri_key);
            mUri = prefs.getString(tmp, "");
            if ((mPw.length() == 0) || (mUsername.length() == 0) || (mUri.length() == 0)) {
                mCallback.needSettings();
                cancel(true);
                return;
            }

            String title = mAct.getString(R.string.progress_title);
            String msg = mAct.getString(R.string.progress_msg);
            mProgDlg = ProgressDialog.show(mAct, title, msg, true);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgDlg.dismiss();
            Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
            mStatus.setText("");
            mSubmitBtn.setEnabled(true);
            mTmpTask = null;
        }

        @Override
        protected void onCancelled() {
            if (mProgDlg != null) {
                mProgDlg.dismiss();
            }
            mSubmitBtn.setEnabled(true);
            super.onCancelled();
        }
    }
}

package com.intel.yamba;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;

class YambaPostWorkerThread extends HandlerThread implements Handler.Callback {
    private static final int            MSG_BG_POST_STATUS = 1;
    private static final int            MSG_BG_SHUTDOWN = 2;

    private Handler         mHandler;
    private StatusActivity  mActivity;

    public YambaPostWorkerThread(StatusActivity ctx, String name) {
        super(name);
        mActivity = ctx;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new Handler(getLooper(), this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean                 ret = false;

        switch (msg.what) {
            case MSG_BG_POST_STATUS:
                String result = mActivity.getString(R.string.fail);
                YambaClient yc = new YambaClient("student", "password");
                String statusMsg = (String)msg.obj;
                try {
                    yc.postStatus(statusMsg);
                    result = mActivity.getString(R.string.success);
                    ret = true;
                } catch (YambaClientException e) {
                    e.printStackTrace();
                }

                mActivity.postDone(result);
                break;

            case MSG_BG_SHUTDOWN:
                getLooper().quit();
                ret = true;
                break;
        }

        return ret;
    }

    public void postStatus(String statusMsg) {
        Message msg = mHandler.obtainMessage(MSG_BG_POST_STATUS, statusMsg);
        msg.sendToTarget();
    }

    public void shutdown() {
        Message msg = mHandler.obtainMessage(MSG_BG_SHUTDOWN);
        msg.sendToTarget();
    }
}

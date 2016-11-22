package com.doov.tools;

import android.app.Activity;
import android.os.Bundle;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.widget.Toast;

public abstract class BasicActivity extends Activity {

	private static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;

    public static final String KEY_TYPE = "type";
    public static final int TYPE_PREVIEW = 0;
    public static final int TYPE_TAKE_PICTURE = 1;
    public static final int TYPE_CAMERA_DRIVER = 2;

    public static final int STATE_STOP = 0;
    public static final int STATE_START = 1;
    
    protected Resources mResources;

    protected int mType = -1;
    protected int mState = STATE_STOP;
    protected long mLastKeyBackPressTime = -1;
    protected long mCount = 0;
    protected long mCurrentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mResources = getResources();
    }
    
        @Override
    public void onAttachedToWindow() {
        getWindow().addFlags(FLAG_HOMEKEY_DISPATCHED);
        super.onAttachedToWindow();
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mLastKeyBackPressTime == -1) {
                mLastKeyBackPressTime = System.currentTimeMillis();
                Toast.makeText(this, R.string.exit_msg, Toast.LENGTH_SHORT).show();
            } else {
                if (System.currentTimeMillis() - mLastKeyBackPressTime < mResources.getInteger(R.integer.delayed_exit_test_by_key_back)) {
                    finish();
                }
                mLastKeyBackPressTime = -1;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return true;
    }

    public int getType() {
        return mType;
    }

    public long getCount() { return mCount; }

    public long getCurrentCount() { return mCurrentCount; }

    public int getState() { return mState; }
}

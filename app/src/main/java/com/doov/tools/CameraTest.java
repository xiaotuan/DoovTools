package com.doov.tools;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class CameraTest extends BasicActivity implements View.OnClickListener, CameraHelper.CameraCallback {

    private static final int MY_PERMISSIONS_REQUEST_DOOV_TOOLS = 0;

    private AutoFitTextureView mTextureView;
    // 设置按钮和计算TextView容器
    private LinearLayout mSettingsContainer;
    // 开始和停止按钮容器
    private LinearLayout mButtonsContainer;
    // 切换摄像头和打开关闭闪光灯按钮容器
    private LinearLayout mScaflContainer;
    private ImageButton mSettingsBtn;
    private TextView mCountTv;
    private Button mStartBtn;
    private Button mStopBtn;
    private ImageButton mSwitchCameraBtn;
    private ImageButton mFlashLightBtn;
    private ImageButton mPreviewSwitchCameraBtn;
    private View mDialogView;
    private EditText mCountEt;
    private CameraHelper mCameraHelper;
    private Dialog mSettingDialog;
    private Handler mHandler = new Handler();

    private boolean mEnabledAutoFocus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        Log.d(this, "onCreate()...");
        setContentView(R.layout.activity_camera_test);

        initViews();
        initValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(this, "onResume()...");
        requestPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(this, "onPause()...");
        mCameraHelper.closeCamera();
        mState = STATE_STOP;
        mCurrentCount = 0;
        mLastKeyBackPressTime = -1;
        updateViewsState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mResources.getBoolean(R.bool.exit_delet_picture_file)) {
            removeTestPicture();
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(this, "onClick=>id: " + v.getId());
        switch (v.getId()) {
            case R.id.settings:
                showSettingsDialog();
                break;

            case R.id.start:
                if (mType == TYPE_TAKE_PICTURE && mCount > 0) {
                    mCurrentCount = 0;
                    mCountTv.setText(mCurrentCount + " / " + mCount);
                    mCameraHelper.takePicture();
                    mState = STATE_START;
                    updateViewsState();
                } else if (mType == TYPE_CAMERA_DRIVER && mCount > 0) {
                    mCurrentCount = 0;
                    mCountTv.setText(mCurrentCount + " / " + mCount);
                    mCameraHelper.autoFocus(mHandler);
                    mState = STATE_START;
                    updateViewsState();
                }
                break;

            case R.id.stop:
                if (mType == TYPE_TAKE_PICTURE) {
                    mState = STATE_STOP;
                    updateViewsState();
                } else if (mType == TYPE_CAMERA_DRIVER) {
                    mCameraHelper.stopAutoFocus();
                    mState = STATE_STOP;
                    updateViewsState();
                }
                break;

            case R.id.switch_camera:
                mCameraHelper.switchCamera();
                break;

            case R.id.flash:
                mCameraHelper.switchFlashLight();
                updateViewsState();
                break;

            case R.id.preview_switch_camera:
                mCameraHelper.switchCamera();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        Log.d(this, "onRequestPermissionsResult, requestCode:" + requestCode);
        if (requestCode == MY_PERMISSIONS_REQUEST_DOOV_TOOLS) {
            int requestCount = 0;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    requestCount++;
                }
            }
            if (requestCount == grantResults.length) {
                mCameraHelper.setTextureView(mTextureView);
                mCameraHelper.setCallback(this);
                mCameraHelper.startPreview();
                updateViewsState();
            } else {
                Toast.makeText(this, R.string.grant_permission_fail, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initValues() {
        mCameraHelper = new CameraHelper(this);
        mCount = mResources.getInteger(R.integer.default_take_picture_count);
        mCountTv.setText(mCurrentCount + " / " + mCount);
        Intent intent = getIntent();
        if (intent != null) {
            mType = intent.getIntExtra(KEY_TYPE, -1);
            Log.d(this, "initValues=>type: " + mType);
            if (mType == TYPE_PREVIEW || mType == TYPE_TAKE_PICTURE || mType == TYPE_CAMERA_DRIVER) {
                updateViewsVisibility();
            } else {
                finish();
            }
        }
    }

    private void initViews() {
        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);
        mSettingsContainer = (LinearLayout) findViewById(R.id.settings_container);
        mButtonsContainer = (LinearLayout) findViewById(R.id.button_container);
        mScaflContainer = (LinearLayout) findViewById(R.id.switch_flash_container);
        mSettingsBtn = (ImageButton) findViewById(R.id.settings);
        mCountTv = (TextView) findViewById(R.id.count);
        mStartBtn = (Button) findViewById(R.id.start);
        mStopBtn = (Button) findViewById(R.id.stop);
        mSwitchCameraBtn = (ImageButton) findViewById(R.id.switch_camera);
        mFlashLightBtn = (ImageButton) findViewById(R.id.flash);
        mPreviewSwitchCameraBtn = (ImageButton) findViewById(R.id.preview_switch_camera);
        mDialogView = getLayoutInflater().inflate(R.layout.settings_dialog_view, null, false);
        mCountEt = (EditText) mDialogView.findViewById(R.id.count_editor);

        mSettingsBtn.setOnClickListener(this);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mSwitchCameraBtn.setOnClickListener(this);
        mFlashLightBtn.setOnClickListener(this);
        mPreviewSwitchCameraBtn.setOnClickListener(this);
    }

    private void updateViewsVisibility() {
        if (mType == TYPE_PREVIEW) {
            setTitle(R.string.preview_test);
            mSettingsContainer.setVisibility(View.GONE);
            mButtonsContainer.setVisibility(View.GONE);
            mPreviewSwitchCameraBtn.setVisibility(View.VISIBLE);
            mScaflContainer.setVisibility(View.GONE);
        } else if (mType == TYPE_TAKE_PICTURE) {
            setTitle(R.string.take_picture_test);
            mSettingsContainer.setVisibility(View.VISIBLE);
            mButtonsContainer.setVisibility(View.VISIBLE);
            mPreviewSwitchCameraBtn.setVisibility(View.GONE);
            mScaflContainer.setVisibility(View.VISIBLE);
        } else if (mType == TYPE_CAMERA_DRIVER) {
            setTitle(R.string.camera_driver_test);
            mSettingsContainer.setVisibility(View.VISIBLE);
            mButtonsContainer.setVisibility(View.VISIBLE);
            mPreviewSwitchCameraBtn.setVisibility(View.GONE);
            mScaflContainer.setVisibility(View.VISIBLE);
        }
    }

    private void updateViewsState() {
        if (mState == STATE_START) {
            mStartBtn.setEnabled(false);
            mStopBtn.setEnabled(true);
            mSettingsBtn.setEnabled(false);
            mSwitchCameraBtn.setEnabled(false);
            mFlashLightBtn.setEnabled(false);
        } else {
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
            mSettingsBtn.setEnabled(true);
            mSwitchCameraBtn.setEnabled(true);
            mFlashLightBtn.setEnabled(true);
        }
    }

    private void showSettingsDialog() {
        mCountEt.setHint(mCount + "");
        if (mSettingDialog == null) {
            mSettingDialog = createSettingsDialog();
            mSettingDialog.show();
        } else {
            mSettingDialog.show();
        }
    }

    private Dialog createSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.settings_title)
                .setView(mDialogView)
                .setNegativeButton(R.string.btn_cancel_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.btn_ok_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String countStr = mCountEt.getText().toString().trim();
                        Log.d(CameraTest.this, "onPositiveButtonClick=>countStr: " + countStr);
                        if (!TextUtils.isEmpty(countStr)) {
                            mCount = Long.parseLong(countStr);
                            mCountTv.setText(mCurrentCount + " / " + mCount);
                        }
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

    private void requestPermissions() {
        ArrayList<String> permissions = new ArrayList<String>();
        if (!hasPermission(Manifest.permission.CAMERA)) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        Log.d(this, "requestPermissions=>list: " + permissions.toString());
        if (permissions.size() > 0) {
            requestPermissions(permissions.toArray(new String[]{}), MY_PERMISSIONS_REQUEST_DOOV_TOOLS);
        } else {
            mCameraHelper.setTextureView(mTextureView);
            mCameraHelper.setCallback(this);
            mCameraHelper.startPreview();
            updateViewsState();
        }
    }

    public boolean hasPermission(String permission) {
        return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    private void removeTestPicture() {
        Log.d(this, "removeTestPicture()...");
        File file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                f.delete();
            }
        }
        file.delete();
    }

    @Override
    public void onStartPreview() {
        if (mType == TYPE_TAKE_PICTURE && mState == STATE_START) {
            if (mCurrentCount < mCount) {
                mCameraHelper.takePicture();
            } else {
                mState = STATE_STOP;
                updateViewsState();
            }
        }
    }

    @Override
    public void onSwitchCamera(int oldCamera, int newCamera) {

    }

    @Override
    public void onFlashlightModelChanged(boolean isOn) {
        if (isOn) {
            mFlashLightBtn.setImageResource(R.drawable.ic_flash_auto);
        } else {
            mFlashLightBtn.setImageResource(R.drawable.ic_flash_off);
        }
    }

    @Override
    public void onTakingPictrue() {

    }

    @Override
    public void onTakePictureCompleted() {
        ++mCurrentCount;
        mCountTv.setText(mCurrentCount + " / " + mCount);
    }

    @Override
    public void onAutoFocus(boolean success) {
        Log.d(this, "onAutoFocus=>success: " + success);
        if (mType == TYPE_CAMERA_DRIVER && mState == STATE_START) {
            if (mCurrentCount < mCount) {
                if (mEnabledAutoFocus) {
                    mEnabledAutoFocus = false;
                    ++mCurrentCount;
                    mCountTv.setText(mCurrentCount + " / " + mCount);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mCameraHelper.autoFocus(mHandler);
                        }
                    }, 500);
                }
            } else {
                mCameraHelper.stopAutoFocus();
                mState = STATE_STOP;
                updateViewsState();
            }
        }
    }

    @Override
    public void onStartAutoFocus() {
        mEnabledAutoFocus = true;
    }

    @Override
    public void onCameraClose() {

    }
}

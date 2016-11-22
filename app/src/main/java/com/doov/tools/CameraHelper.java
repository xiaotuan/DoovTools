package com.doov.tools;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.TextureView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class CameraHelper {

    private static final String KEY_FOCUS_DISTANCES = "focus-distances";

    private BasicActivity mActivity;
    private AutoFitTextureView mTextureView;
    private Camera mCamera;
    private Resources mResources;
    private CameraCallback mCallback;

    private int mCameraId = -1;
    private int mBackCameraId = -1;
    private int mFrontCameraId = -1;
    private int mDisplayRotation;
    private int mDisplayOrientation;
    private boolean mPreviewing;
    private boolean mCanTakePicture;
    private String mFlashMode;
    private int mZoomValue = -1;

    public CameraHelper(BasicActivity activity) {
        mActivity = activity;
        mResources = activity.getResources();
        mFlashMode = mResources.getString(R.string.default_flash_light_model);
        mPreviewing = false;
        mCanTakePicture = false;
        initCameraId();
        initDefaultCameraId();
    }

    private void initCameraId() {
        int mNumberOfCameras = Camera.getNumberOfCameras();
        CameraInfo[] mInfo = new CameraInfo[mNumberOfCameras];
        for (int i = 0; i < mNumberOfCameras; i++) {
            mInfo[i] = new CameraInfo();
            Camera.getCameraInfo(i, mInfo[i]);
        }

        // get the first (smallest) back and first front camera id
        for (int i = 0; i < mNumberOfCameras; i++) {
            if (mBackCameraId == -1 && mInfo[i].facing == CameraInfo.CAMERA_FACING_BACK) {
                mBackCameraId = i;
            } else if (mFrontCameraId == -1 && mInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT) {
                mFrontCameraId = i;
            }
        }

        Log.d(this, "getCameraId=>back: " + mBackCameraId + " front: " + mFrontCameraId);
    }

    private void initDefaultCameraId() {
        String defaultId = mResources.getString(R.string.default_camera);
        if ("back".equals(defaultId)) {
            if (mBackCameraId != -1) {
                mCameraId = mBackCameraId;
            } else if (mFrontCameraId != -1) {
                mCameraId = mFrontCameraId;
            }
        } else if ("front".equals(defaultId)) {
            if (mFrontCameraId != -1) {
                mCameraId = mFrontCameraId;
            } else if (mBackCameraId != -1) {
                mCameraId = mBackCameraId;
            }
        } else {
            if (mBackCameraId != -1) {
                mCameraId = mBackCameraId;
            } else if (mFrontCameraId != -1) {
                mCameraId = mFrontCameraId;
            }
        }
        Log.d(this, "initDefaultCameraId=>cameraId: " + mCameraId);
    }

    public void setTextureView(AutoFitTextureView textureView) {
        if (textureView != null) {
            mTextureView = textureView;
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void startPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        try {
            mCamera = Camera.open(mCameraId);
            if (mCamera != null) {
                setCameraParameters();
                setPreviewDisplay(mTextureView.getSurfaceTexture());
                try {
                    mCamera.startPreview();
                    mPreviewing = true;
                    mCanTakePicture = true;
                    if (mCallback != null) {
                        mCallback.onStartPreview();
                    }
                } catch (Exception e) {
                    Log.d(this, "startPreview(2)=>error: ", e);
                    closeCamera();
                }
            }
        } catch (Exception e) {
            Log.d(this, "startPreview(1)=>error: ", e);
        }
    }

    public void autoFocus(Handler handler) {
        if (mCamera != null) {
            Parameters parameters = mCamera.getParameters();
            if (mZoomValue == -1) {
                parameters.setZoom(parameters.getMaxZoom());
                mZoomValue = parameters.getMaxZoom();
            } else if (mZoomValue == parameters.getMaxZoom()) {
                parameters.setZoom(0);
                mZoomValue = 0;
            } else if (mZoomValue == 0){
                parameters.setZoom(parameters.getMaxZoom());
                mZoomValue = parameters.getMaxZoom();
            }
            mCamera.setParameters(parameters);
            Log.d(this, "autoFocus=>zoom: " + mZoomValue);
            handler.removeCallbacks(mAutoFocusRunnable);
            handler.postDelayed(mAutoFocusRunnable, 500);
        }
    }

    private Runnable mAutoFocusRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("CameraTest", "run()...");
            if (mCallback != null) {
                mCallback.onStartAutoFocus();
            }
            mCamera.autoFocus(mAutoFocusCallback);
        }
    };

    public void stopAutoFocus() {
        Parameters parameters = mCamera.getParameters();
        parameters.setZoom(0);
        mCamera.setParameters(parameters);
        mZoomValue = -1;
    }

    public void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mPreviewing = false;
        mCanTakePicture = false;
        if (mCallback != null) {
            mCallback.onCameraClose();
        }
    }

    public void takePicture() {
        if (mPreviewing && mCanTakePicture) {
            mCamera.takePicture(null, null, mPictureCallback);
            mPreviewing = false;
            mCanTakePicture = false;
            if (mCallback != null) {
                mCallback.onTakingPictrue();
            }
        }
    }

    public void switchCamera() {
        int oldCamera = mCameraId;
        if (mCameraId == mBackCameraId) {
            mCameraId = mFrontCameraId;
        } else {
            mCameraId = mBackCameraId;
        }
        if (mCallback != null) {
            mCallback.onSwitchCamera(oldCamera, mCameraId);
        }
        closeCamera();
        startPreview();
    }

    public void switchFlashLight() {
        if (mCamera != null) {
            boolean isOn = false;
            Parameters parameters = mCamera.getParameters();
            String model = parameters.getFlashMode();
            if (Parameters.FLASH_MODE_AUTO.equals(model)
                    || Parameters.FLASH_MODE_ON.equals(model)
                    || Parameters.FLASH_MODE_RED_EYE.equals(model)
                    || Parameters.FLASH_MODE_TORCH.equals(model)) {
                mFlashMode = Parameters.FLASH_MODE_OFF;
            } else {
                mFlashMode = Parameters.FLASH_MODE_AUTO;
                isOn = true;
            }
            if (mCallback != null) {
                mCallback.onFlashlightModelChanged(isOn);
            }
            closeCamera();
            startPreview();
        }
    }

    public void setCallback(CameraCallback callback) {
        mCallback = callback;
    }

    private void setCameraParameters() {
        Camera.Parameters mParameters = mCamera.getParameters();
        List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
        if (frameRates != null) {
            Integer max = (Integer) Collections.max(frameRates);
            mParameters.setPreviewFrameRate(max.intValue());
        }
        mParameters.setPictureFormat(PixelFormat.JPEG);
        mParameters.setPictureSize(mResources.getInteger(R.integer.default_picture_width_size),
                mResources.getInteger(R.integer.default_picture_height_size));
        if (isFlashlightSupported(mParameters)) {
            if (mActivity.getType() == BasicActivity.TYPE_PREVIEW) {
                mParameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
            } else if (mActivity.getType() == BasicActivity.TYPE_CAMERA_DRIVER) {
                mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            } else {
                mParameters.setFlashMode(mFlashMode);
                if (mCallback != null) {
                	mCallback.onFlashlightModelChanged(Parameters.FLASH_MODE_AUTO.equals(mFlashMode) ? true : false);
                }
            }
        }
        if (isFocusAreaSupported(mParameters)) {
            if (mActivity.getType() == BasicActivity.TYPE_CAMERA_DRIVER) {
                mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            } else {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }
        Camera.Size size = mParameters.getPictureSize();
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = getOptimalPreviewSize(sizes, ((double) size.width / (double) size.height));
        if (optimalSize != null) {
            mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
        }
        mParameters.setPreviewFrameRate(mResources.getInteger(R.integer.default_preview_frame_rate));
        mCamera.setParameters(mParameters);
        mCamera.cancelAutoFocus();
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, double targetRatio) {
        Camera.Size result = null;
        if (result != null) {
            Camera.Size size = null;
            Double d = Double.MAX_VALUE;
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            int min = Math.min(display.getHeight(), display.getWidth());
            if (min <= 0) {
                min = display.getHeight();
            }
            for (int i = 0; i < sizes.size(); i++) {
                size = sizes.get(i);
                if ((Math.abs(size.width / size.height - targetRatio) <= 0.05) && (Math.abs(size.height - i) < d)) {
                    result = size;
                    d = (double) Math.abs(size.height - i);
                }

            }
        }

        return result;
    }

    public int getDisplayOrientation(int degrees) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        Log.d(this, "getDisplayOrientation=>degrees: " + degrees + " camera: " + info.orientation);
        if (info.facing == 1) {
            return ((info.orientation + degrees + 180) % 0x168);
        } else {
            int result = ((info.orientation - degrees) + 0x168) % 0x168;
            return result;
        }
    }

    private void setPreviewDisplay(SurfaceTexture texture) {
        try {
            mCamera.setPreviewTexture(texture);
            mDisplayRotation = getDisplayRotation();
            mDisplayOrientation = getDisplayOrientation(mDisplayRotation);
            mCamera.setDisplayOrientation(mDisplayOrientation);
            return;
        } catch (IOException e) {
            Log.d(this, "setPreviewDisplay=>error: ", e);
            closeCamera();
        }
    }

    public int getDisplayRotation() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case 0:
                return 0;

            case 1:
                return 90;

            case 2:
                return 180;

            case 3:
                return 270;

            default:
                return 0;
        }
    }


    public static boolean isFocusAreaSupported(Camera.Parameters params) {
        return (params.getMaxNumFocusAreas() > 0
                && isSupported(Camera.Parameters.FOCUS_MODE_AUTO,
                params.getSupportedFocusModes()));
    }

    public static boolean isFlashlightSupported(Camera.Parameters params) {
        return isSupported(Camera.Parameters.FLASH_MODE_AUTO,
                params.getSupportedFocusModes());
    }

    public static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    public File getSaveFile() {
        String name = Calendar.getInstance().getTimeInMillis()+ ".jpg";
        return new File(mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name);
    }

    public Bitmap rotateBitmapByDegree(byte[] data) {
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0x0, data.length);;
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            matrix.postRotate(90);
        } else {
            matrix.postRotate(270);
            matrix.postScale(-1, 1);
        }

        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    public void fileScan(String filePath){
        Uri data = Uri.parse("file://"+filePath);
        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, data));
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setAutoExposureLock(false);
            parameters.setAutoWhiteBalanceLock(false);
            mCamera.setParameters(parameters);
            if (mResources.getBoolean(R.bool.save_picture)) {
                Bitmap bm = rotateBitmapByDegree(data);
                File myCaptureFile = getSaveFile();
                Log.d(this, "onPictureTake=>path: " + myCaptureFile.getAbsolutePath());
                try {
                    if (!myCaptureFile.exists()) {
                        myCaptureFile.createNewFile();
                    }
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                    bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    bm.recycle();
                    bos.flush();
                    bos.close();
                } catch (Exception e) {
                    Log.d(this, "onPictureTaken=>error: ", e);
                }
                fileScan(myCaptureFile.getPath());
            }
            if (mCallback != null) {
                mCallback.onTakePictureCompleted();
            }
            closeCamera();
            startPreview();
        }
    };

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (mCallback != null) {
                mCallback.onAutoFocus(success);
            }
        }
    };

    public interface  CameraCallback {
        void onStartPreview();
        void onSwitchCamera(int oldCamera, int newCamera);
        void onFlashlightModelChanged(boolean isOn);
        void onTakingPictrue();
        void onTakePictureCompleted();
        void onStartAutoFocus();
        void onAutoFocus(boolean success);
        void onCameraClose();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mCamera.cancelAutoFocus();
            closeCamera();
            startPreview();
        }
    };

}

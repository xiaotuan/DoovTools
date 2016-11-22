package com.doov.tools;

import com.android.internal.telephony.TelephonyIntents;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class DoovToolsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Uri uri = intent.getData();
		Log.d(this, "onReceive=>action: " + action + " uri: " + uri);
		if (TelephonyIntents.SECRET_CODE_ACTION.equals(action)) {
			Intent testIntent = null;
			Uri imeiUri = Uri.parse("android_secret_code://" + context.getString(R.string.imei_query_code));
			Uri allImeiUri = Uri.parse("android_secret_code://" + context.getString(R.string.all_imei_query_code));
			Uri softwareVersionUri = Uri.parse("android_secret_code://" + context.getString(R.string.software_version_query_code));
			Uri hardwareVersionUri = Uri.parse("android_secret_code://" + context.getString(R.string.hardware_version_query_code));
			Uri cameraPreviewUri = Uri.parse("android_secret_code://" + context.getString(R.string.camera_preview_test_code));
			Uri cameraTakePictureUri = Uri.parse("android_secret_code://" + context.getString(R.string.camera_take_picture_test_code));
			Uri cameraDriverUri = Uri.parse("android_secret_code://" + context.getString(R.string.camera_driver_test_code));
			
			if (imeiUri.equals(uri)) {
				testIntent = new Intent(context, InfoDialog.class);
				testIntent.putExtra(InfoDialog.KEY_TYPE, InfoDialog.SHOW_IMEI_TYPE);
				testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			} else if (allImeiUri.equals(uri)) {
				testIntent = new Intent(context, InfoDialog.class);
				testIntent.putExtra(InfoDialog.KEY_TYPE, InfoDialog.SHOW_ALL_IMEI_TYPE);
				testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			} else if (softwareVersionUri.equals(uri)) {
				testIntent = new Intent(context, InfoDialog.class);
				testIntent.putExtra(InfoDialog.KEY_TYPE, InfoDialog.SHOW_SOFTWARE_VERSION_TYPE);
				testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			} else if (hardwareVersionUri.equals(uri)) {
				testIntent = new Intent(context, InfoDialog.class);
				testIntent.putExtra(InfoDialog.KEY_TYPE, InfoDialog.SHOW_HARDWARE_VERSION_TYPE);
				testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			} else if (cameraPreviewUri.equals(uri)) {
				testIntent = new Intent(context, CameraTest.class);
				testIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				testIntent.putExtra(CameraTest.KEY_TYPE, CameraTest.TYPE_PREVIEW);
			} else if (cameraTakePictureUri.equals(uri)) {
				testIntent = new Intent(context, CameraTest.class);
				testIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				testIntent.putExtra(CameraTest.KEY_TYPE, CameraTest.TYPE_TAKE_PICTURE);
			} else if (cameraDriverUri.equals(uri)) {
				testIntent = new Intent(context, CameraTest.class);
				testIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				testIntent.putExtra(CameraTest.KEY_TYPE, CameraTest.TYPE_CAMERA_DRIVER);
			}
			if (testIntent != null) {
				context.startActivity(testIntent);
			}
		}
	}

}

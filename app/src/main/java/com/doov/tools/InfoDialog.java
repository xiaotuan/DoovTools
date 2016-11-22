package com.doov.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class InfoDialog extends AlertActivity implements
    DialogInterface.OnClickListener {

    protected static final String KEY_TYPE = "type";

    private int mType;

    public static final int SHOW_IMEI_TYPE = 0;
    public static final int SHOW_ALL_IMEI_TYPE = 1;
    public static final int SHOW_SOFTWARE_VERSION_TYPE = 2;
    public static final int SHOW_HARDWARE_VERSION_TYPE = 3;
    
    private static final int END_TYPE = SHOW_HARDWARE_VERSION_TYPE;
    
    private static final int[] sDialogTitle = {
    		R.string.title_imei,
    		R.string.title_imei,
    		R.string.title_software_verion,
    		R.string.title_hardware_verion
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mType = intent.getIntExtra(KEY_TYPE, -1);
        Log.d(this, "onCreate, mType is " + mType);
        if (mType >= SHOW_IMEI_TYPE  && mType <= END_TYPE) {
            showWarningDialog(mType);
        } else {
            finish();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private void showWarningDialog(int type) {
            warningMessageDialog(sDialogTitle[type]);
    }

    /**
     *
     * @param context
     *            The Context that had been passed to
     *            {@link #warningMessageDialog(Context, int, int, int)}
     * @param titleResId
     *            Set the title using the given resource id.
     * @param messageResId
     *            Set the message using the given resource id.
     * @return Creates a {@link AlertDialog} with the arguments supplied to this
     *         builder.
     */
    private void warningMessageDialog(int titleResId) {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(titleResId);
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.btn_ok_msg);
        p.mPositiveButtonListener = this;
        //p.mNegativeButtonText = getString(R.string.btn_cancel_msg);
        //p.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.info_dialog, null);
        TextView mMessageView = (TextView) view.findViewById(R.id.subtitle);
        mMessageView.setText(getDialogMessage());
        return view;
    }

   private String getDialogMessage() {
	   String message = "";
	   switch (mType) {
	   case SHOW_IMEI_TYPE:
		   message = getIMEI();
		   break;
		   
	   case SHOW_ALL_IMEI_TYPE:
		   message = getAllIMEI();
		   break;
		   
	   case SHOW_SOFTWARE_VERSION_TYPE:
		   message = getSoftwareVersion();
		   break;
		   
	   case SHOW_HARDWARE_VERSION_TYPE:
		   message = getHardwareVersion();
		   break;
	   }
	   return message;
   }

    public void onClick(DialogInterface dialogInterface, int button) {
        Log.d(this, "onClick");
        if (button == DialogInterface.BUTTON_POSITIVE) {
            return;
        } else {
        	return;
        }
    }
    
    private String getIMEI() {
    	String imei = "";
    	TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    	if (tm.getPhoneCount() > 0) {
    		imei = getString(R.string.imei_msg, tm.getDeviceId(0));
    	}
    	return imei;
    }
    
    private String getAllIMEI() {
    	TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    	StringBuilder sb = new StringBuilder();
    	for (int slot = 0; slot < tm.getPhoneCount(); slot++) {
    		if (tm.getPhoneCount() >= 2) {
    			sb.append(getString(R.string.all_imei_msg, slot + 1, tm.getDeviceId(slot)));
    		} else {
    			sb.append(getString(R.string.imei_msg, tm.getDeviceId(slot)));
    		}
    		if (slot + 1 < tm.getPhoneCount()) {
    			sb.append("\n");
    		}
        }
    	return sb.toString();
    }
    
    private String getSoftwareVersion() {
    	return SystemProperties.get("ro.xh.display.version", "Unknown");
    }
    
    private String getHardwareVersion() {
    	return SystemProperties.get("ro.hardware.version", "Unknown");
    }
}

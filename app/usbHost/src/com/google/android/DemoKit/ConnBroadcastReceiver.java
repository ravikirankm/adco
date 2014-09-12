package com.google.android.DemoKit;

import java.io.FileOutputStream;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class ConnBroadcastReceiver extends BroadcastReceiver{
	
	boolean isConnected;

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if(ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
			if(intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)){
				Toast.makeText(context, "Disconnected - No networks available", Toast.LENGTH_SHORT).show();
				isConnected = false;
				return;
			} else if(!isConnected) {
				// Check for network connectivity
				ConnectivityManager cm =
						(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			 
				NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
				isConnected = activeNetwork != null &&
						activeNetwork.isConnectedOrConnecting();		

				if(isConnected) {
					Toast.makeText(context, "Internet Connection Detected", Toast.LENGTH_SHORT).show();
//					FileOutputStream videoOut;
//				File targetFile = new File("/storage/emulated/legacy/myfolder" + "/" + "test.bin");
				
					FtpMgr ftpMgr= new FtpMgr(context);
					ftpMgr.start();
			    
				} else {
					Toast.makeText(context, "No Internet Connection Detected", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
}

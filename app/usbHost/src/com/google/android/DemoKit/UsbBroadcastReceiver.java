package com.google.android.DemoKit;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class UsbBroadcastReceiver extends BroadcastReceiver {

	AccessoryController mAccessoryController;
	
	public UsbBroadcastReceiver(AccessoryController controller){
		mAccessoryController = controller;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Toast.makeText(context, "Inside onReceive of UsbBroadcastReceiver (action = " + action + ")", Toast.LENGTH_SHORT).show();

		UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		
		if (DemoKitActivity.ACTION_USB_PERMISSION.equals(action)) {
//			synchronized (this) {
				
				UsbAccessory[] accessories = mUsbManager.getAccessoryList();
				UsbAccessory accessory = (accessories == null ? null : accessories[0]);
				if (intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					Toast.makeText(context, "Got USB permission", Toast.LENGTH_SHORT).show();
					mAccessoryController.openAccessoryIO(accessory);
				} else {
					Toast.makeText(context, "EXTRA_PERMISSION_GRANTED is false", Toast.LENGTH_SHORT).show();
				}
//			}
		} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
			UsbAccessory[] accessories = mUsbManager.getAccessoryList();
			UsbAccessory accessory = (accessories == null ? null : accessories[0]);
//			if (accessory != null && accessory.equals(mAccessoryController.getAccessory())) {
				mAccessoryController.closeAccessory();
//			}
		}
	}
}

package com.google.android.DemoKit;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.view.View;

public abstract class AccessoryController {

	protected DemoKitActivity mHostActivity;
	protected UsbAccessory mAccessory;
	protected UsbManager mUsbManager;
	
	public AccessoryController(DemoKitActivity activity) {
		mHostActivity = activity;
		mUsbManager = (UsbManager) mHostActivity.getSystemService(Context.USB_SERVICE);
	}

	protected View findViewById(int id) {
		return mHostActivity.findViewById(id);
	}

	protected Resources getResources() {
		return mHostActivity.getResources();
	}

	void accessoryAttached() {
		onAccesssoryAttached();
	}

	abstract protected void onAccesssoryAttached();
	
	protected boolean initiateHostXfer(AccessoryPacket req, AccessoryPacket rsp) {
		return false;
	}
	
	protected boolean isControllerBusy() {
		return false;
	}
	
	public void openAccessory() {
		
	}
	
	public void openAccessoryIO(UsbAccessory accessory) {
		
	}
	
	public void closeAccessory() {
		
	}
	
	public UsbAccessory getAccessory() {
		return mAccessory;
	}

}
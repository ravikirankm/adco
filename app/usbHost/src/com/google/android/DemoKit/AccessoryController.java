package com.google.android.DemoKit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.Toast;

public class AccessoryController extends ServiceBase implements Runnable {

//	protected DemoKitActivity mHostActivity;
//	protected UsbAccessory mAccessory;
//	protected UsbManager mUsbManager;

    
    // Binder given to clients
//    private final IBinder mBinder = new AccessoryBinder();
    
    PeriodicScheduler mAccessoryFsm;
    protected Object mAccessoryFsmLock = new Object();
    
	
	public AccessoryController() {
//		mHostActivity = activity;
//		mUsbManager = (UsbManager) mHostActivity.getSystemService(Context.USB_SERVICE);
	}

	    
   /**
    * Class used for the client Binder.  Because we know this service always
    * runs in the same process as its clients, we don't need to deal with IPC.
    */
/*   public class AccessoryBinder extends Binder {
	   AccessoryController getService() {
		   // Return this instance of LocalService so clients can call public methods
		   return AccessoryController.this;
	   }
   }
*/   
/*	protected Resources getResources() {
		return mHostActivity.getResources();
	}
*/
	void accessoryAttached() {
		onAccesssoryAttached();
	}

	protected void onAccesssoryAttached() {
		
	}
	
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

	
/*	public UsbAccessory getAccessory() {
		return mAccessory;
	}
*/

	public void run() {
//		sendClientDebugMsg("Trying to start accessoryFSM");
		synchronized(mAccessoryFsmLock) {
			accessoryFSM();
		}
	}
	
	public void accessoryFSM() {
		
	}
}
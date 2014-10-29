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

public abstract class AccessoryController extends Service{

//	protected DemoKitActivity mHostActivity;
//	protected UsbAccessory mAccessory;
//	protected UsbManager mUsbManager;

    protected Messenger mClientMessenger;
    protected Messenger mMsgHandler;
    
    // Binder given to clients
    private final IBinder mBinder = new AccessoryBinder();
	
	public AccessoryController(DemoKitActivity activity) {
//		mHostActivity = activity;
//		mUsbManager = (UsbManager) mHostActivity.getSystemService(Context.USB_SERVICE);
	}

/*	protected View findViewById(int id) {
		return findViewById(id);
	}
*/
	public void doOnIncomingMsg(Message msg) {
		switch (msg.what) {
		}
	}
	
   protected class IncomingHandler extends Handler {
	   @Override
	   public void handleMessage(Message msg) {
		   doOnIncomingMsg(msg);
		   mClientMessenger = msg.replyTo;
	   }    	
   }

   @Override
   public IBinder onBind(Intent arg0) {
	   // TODO Auto-generated method stub
	   mMsgHandler = new Messenger(new IncomingHandler());
	   return mMsgHandler.getBinder();
   }
	    
   /**
    * Class used for the client Binder.  Because we know this service always
    * runs in the same process as its clients, we don't need to deal with IPC.
    */
   public class AccessoryBinder extends Binder {
	   AccessoryController getService() {
		   // Return this instance of LocalService so clients can call public methods
		   return AccessoryController.this;
	   }
   }
   
/*	protected Resources getResources() {
		return mHostActivity.getResources();
	}
*/
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

	protected void sendClientDebugMsg(String str) {
		if(mClientMessenger != null) {
			Message msg = Message.obtain(null, UsbMessages.DBG_MSG, 0, 0);
			Bundle msgBundle = new Bundle(); 
			msgBundle.putString("debug", str);
			msg.setData(msgBundle);
			try {
				mClientMessenger.send(msg);
			} catch (RemoteException e) {}
		}
	}
	
/*	public UsbAccessory getAccessory() {
		return mAccessory;
	}
*/
}
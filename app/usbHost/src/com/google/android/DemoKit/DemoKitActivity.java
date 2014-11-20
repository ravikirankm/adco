/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.DemoKit;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.net.ftp.FTPClient;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.os.RemoteException;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

//import com.google.android.DemoKit.BluetoothService.BluetoothBinder;
//import com.google.android.DemoKit.NocAccessService.LocalBinder;

public class DemoKitActivity extends Activity implements Runnable {
	private static final String TAG = "DemoKit";
	private boolean mPermissionRequestPending;
	private static final String siteId = "EGYPTD3322";
	private static final String nocUrl = "75.177.179.58";
	public static final String localHexFP = "/storage/emulated/legacy/myfolder" + "/" + "new.bin";
	private static final int REQUEST_BT_ENABLE = 1;
	private static final int REQUEST_BT_START_DISCOVERY = 2;
	private static final int BT_MSG_HEX_FILE_AVAIL= 0;
	private static final int BT_MSG_DEBUG= 1;
	
	ConnBroadcastReceiver mConnReceiver;
    Context context;

    public boolean mNocServiceBound;
    NocAccessService mNocAccessService;
    Messenger mNocAccessServiceMessenger;
    Messenger mNocMsgHandler = null;

    public boolean mBluetoothServiceBound;
    Messenger mBTService= null;
    Messenger mBTMsgHandler = null;

    public boolean mAccessoryBound;
    Messenger mAccessoryController = null; 
    Messenger mAccessoryMsgHandler = null;
    
    PeriodicScheduler mNocServiceRequestor;

    protected class ServiceMsgHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    		case MessagesBase.DBG_MSG:
      			toastHandler(msg.getData().getString("debug"));
    			break;
    		default:
    			super.handleMessage(msg);
    			break;
    		}
    	}
    }
    
    protected class BluetoothMsgHandler extends ServiceMsgHandler {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    		case BT_MSG_HEX_FILE_AVAIL:
    			toastHandler("Got Hex file from BT Service");
    			break;
    		default:
    			super.handleMessage(msg);
    			break;
    		}
    	}
    }
    
	public void toastHandler(String str){
		final String msg = str;
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		context = getApplicationContext();
		
//		Toast.makeText(this, "inside onCreate of DemoKitActivity", Toast.LENGTH_SHORT).show();
		
		IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		mConnReceiver = new ConnBroadcastReceiver();
		
		registerReceiver(mConnReceiver, connFilter);

		setContentView(R.layout.main);
		
		enableControls(true);
		
		mNocServiceRequestor = new PeriodicScheduler(new Runnable () {
			@Override
			public void run() {
				if(mNocAccessServiceMessenger != null) {
					toastHandler("Trying to upload Sensor Data");
//					if(mConnReceiver.isConnected) {
					Message msg = Message.obtain(null, NocMessages.URL_MSG, 0, 0);
					Bundle msgBundle = new Bundle(); 
					msgBundle.putString("url", "http://98.26.23.101/update.php?req=UI:WLDCTLAB1;FV:1.36;E1:0;EC1:0;E2:0;EC2:0;FL:2937;FC:0;BA:1218;TO:2181;TC:2181;FL20:0;FE:0;BL:0;HTO:0;HTC:0;BP1:0;BP2:0;GF:0;UF:1;IN:0;DT:0;TCF:0;TOF:0");
					msg.setData(msgBundle);
					try {
						mNocAccessServiceMessenger.send(msg);
					} catch (RemoteException e) {
						
					}
					
				} else {
					toastHandler("mNocAccessServiceMessenger is null!");
				}
				
			}
		}, 5000);
		
		
		mBTMsgHandler = new Messenger(new BluetoothMsgHandler());
		mAccessoryMsgHandler = new Messenger(new ServiceMsgHandler());
		mNocMsgHandler = new Messenger (new ServiceMsgHandler());
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		
		return super.onRetainNonConfigurationInstance();
		
	}


    @Override
    protected void onStart() {
        super.onStart();

        Intent nocServiceIntent = new Intent(this, NocAccessService.class);
		bindService(nocServiceIntent, mNocConnection, Context.BIND_AUTO_CREATE);
		
/*		Intent btServiceIntent = new Intent(this, BluetoothService.class);
		bindService(btServiceIntent, mBTConnection, Context.BIND_AUTO_CREATE);
*/
		Intent accessoryIntent = new Intent(this, MegaADKController.class);
		bindService(accessoryIntent, mAccessoryConnection, Context.BIND_AUTO_CREATE);
    
    }
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		mNocServiceRequestor.startUpdates();
		
		if(mAccessoryController == null) {
//			Toast.makeText(this, "onResume of DemoKitActivity - Accessory Controller is null", Toast.LENGTH_SHORT).show();
		} else {
			try {
				mAccessoryController.send(Message.obtain(null, UsbMessages.START_CONTROLLER, 0, 0));
			} catch (RemoteException e) {};
		}
		
/*		SmsManager smsMgr = SmsManager.getDefault();
		
		try {
			Toast.makeText(context, "Trying to send SMS...", Toast.LENGTH_SHORT).show();
			smsMgr.sendTextMessage("9197445298", null, "Test message from Galaxy S4", null, null);
		} catch (IllegalArgumentException e) {
			Toast.makeText(context, "Got Exception when trying to send SMS", Toast.LENGTH_SHORT).show();
		}
*/		
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	
		String action = intent.getAction();
		if(action.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) {
			try {
				mAccessoryController.send(Message.obtain(null, UsbMessages.ACCESSORY_ATTACHED, 0, 0));
			} catch (RemoteException e) {};
		}

	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mNocConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
    		Toast.makeText(context, "inside onServiceConnected of mNocConnection", Toast.LENGTH_SHORT).show();    		

        	mNocAccessServiceMessenger = new Messenger (service);
        	
    		Message msg = Message.obtain(null, MessagesBase.SET_CLIENT_MSG,0,0);
    		msg.replyTo = mNocMsgHandler;
    		
    		try {
    			mNocAccessServiceMessenger.send(msg);
    		} catch (RemoteException e) {
    			
    		}
            mNocServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mNocServiceBound = false;
        }
    };
	
    private ServiceConnection mBTConnection = new ServiceConnection() {
    	
    	@Override
    	public void onServiceConnected(ComponentName className, IBinder service) {
    		mBTService= new Messenger (service);
    		Message msg = Message.obtain(null, MessagesBase.SET_CLIENT_MSG,0,0);
    		msg.replyTo = mBTMsgHandler;
    		try {
    			mBTService.send(msg);
    		} catch (RemoteException e) {
    			
    		}
    		mBluetoothServiceBound = true;
    	}
    	
    	@Override
    	public void onServiceDisconnected(ComponentName arg0) {
    		mBluetoothServiceBound = false;
    	}
    };
    
    private ServiceConnection mAccessoryConnection = new ServiceConnection() {
    	
    	@Override
    	public void onServiceConnected(ComponentName className, IBinder service) {
//    		Toast.makeText(context, "inside onServiceConnected of AccessoryConnection", Toast.LENGTH_SHORT).show();    		
    		mAccessoryController = new Messenger (service);
    		Message msg = Message.obtain(null, UsbMessages.SET_CLIENT_MSG,0,0);
    		msg.replyTo = mAccessoryMsgHandler;
    		try {
    			mAccessoryController.send(msg);
    		} catch (RemoteException e) {
    			
    		}
    		mAccessoryBound = true;
    	}
    	
    	@Override
    	public void onServiceDisconnected(ComponentName arg0) {
    		mAccessoryBound = false;
    	}
    };

    
    @Override
	public void onPause() {
		super.onPause();
		if(mAccessoryController == null) {
//			Toast.makeText(this, "onPause of DemoKitActivity - Accessory Controller is null", Toast.LENGTH_SHORT).show();
			return;
		} else {

			try { 
				mAccessoryController.send(Message.obtain(null, UsbMessages.STOP_CONTROLLER, 0, 0));
			} 
			catch (RemoteException e) {};
		}

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mNocServiceBound) {
            unbindService(mNocConnection);
            mNocServiceBound = false;
        }
        mNocServiceRequestor.stopUpdates();
        
    }
	
	@Override
	public void onDestroy() {
		Toast.makeText(this, "inside onDestroy of DemoKitActivity", Toast.LENGTH_SHORT).show();

		unregisterReceiver(mConnReceiver);
		super.onDestroy();
	}


	protected void enableControls(boolean enable) {
	}
	
	public String getSiteId() {
		if(siteId == null) {
			// TODO request user to input a siteID
		} 

		return siteId;
	}
	
	public String getNocUrl() {
		if(nocUrl == null) {
			// TODO request user to input a NOC Url
		}
		
		return nocUrl;
	}

	public void run() {
	}

	public void onClickSendHexFile(View v) {
		Toast.makeText(this, "Click received from Send Hex File", Toast.LENGTH_SHORT).show();
		try {
			mAccessoryController.send(Message.obtain(null, UsbMessages.SEND_HEX, 0, 0));
		} catch (RemoteException e) {};
	}

	public void onClickSetSettings(View v) {
		Toast.makeText(this, "Click received from Set Settings", Toast.LENGTH_SHORT).show();
		try {
			mAccessoryController.send(Message.obtain(null, UsbMessages.SET_SETTINGS, 0, 0));
		} catch (RemoteException e) {};
	}
}

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
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import com.google.android.DemoKit.BluetoothService.BluetoothBinder;
import com.google.android.DemoKit.NocAccessService.LocalBinder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class DemoKitActivity extends Activity implements Runnable {
	private static final String TAG = "DemoKit";

	private boolean mPermissionRequestPending;
	
	private static final String siteId = "EGYPTD3322";
	private static final String nocUrl = "75.177.179.58";
	public static final String localHexFP = "/storage/emulated/legacy/myfolder" + "/" + "new.bin";

	private static final int BT_MSG_HEX_FILE_AVAIL= 0;
	private static final int BT_MSG_DEBUG= 1;
	
	
	UsbBroadcastReceiver mUsbReceiver;

	AccessoryUtils mUtils;
//	MegaADKController mAccessoryController;
	
	ConnBroadcastReceiver mConnReceiver;
	
	private static final int MESSAGE_SWITCH = 1;
	private static final int MESSAGE_TEMPERATURE = 2;
	private static final int MESSAGE_LIGHT = 3;
	private static final int MESSAGE_JOY = 4;
	private static final int MESSAGE_ACCESSORY_NOT_READY = 5;

	public static final byte LED_SERVO_COMMAND = 2;
	public static final byte RELAY_COMMAND = 3;

	private static final int REQUEST_BT_ENABLE = 1;
	private static final int REQUEST_BT_START_DISCOVERY = 2;

    Context context;
    NocAccessService mNocAccessService;
    public boolean mNocServiceBound;

    public boolean mBluetoothServiceBound;
    Messenger mBTService= null;
    Messenger mBTMsgHandler = null;

    public boolean mAccessoryBound;
    Messenger mAccessoryController = null; 
    Messenger mAccessoryMsgHandler = null;
    
    PeriodicScheduler mNocServiceRequestor;
    
    protected class BluetoothMsgHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    		case BT_MSG_HEX_FILE_AVAIL:
    			toastHandler("Got Hex file from BT Service");
    			break;
    		case BT_MSG_DEBUG:
  //  			toastHandler(msg.getData().getString("debug"));
    			break;
    		default:
    			super.handleMessage(msg);
    			break;
    		}
    	}
    }
    
    protected class AccessoryMsgHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    		case UsbMessages.DBG_MSG:
    			toastHandler(msg.getData().getString("debug"));
    			break;
    		default:
    			super.handleMessage(msg);
    			break;
    		}
    	}
    }
    
    
	protected class SwitchMsg {
		private byte sw;
		private byte state;

		public SwitchMsg(byte sw, byte state) {
			this.sw = sw;
			this.state = state;
		}

		public byte getSw() {
			return sw;
		}

		public byte getState() {
			return state;
		}
	}

	protected class TemperatureMsg {
		private int temperature;

		public TemperatureMsg(int temperature) {
			this.temperature = temperature;
		}

		public int getTemperature() {
			return temperature;
		}
	}

	protected class LightMsg {
		private int light;

		public LightMsg(int light) {
			this.light = light;
		}

		public int getLight() {
			return light;
		}
	}

	protected class JoyMsg {
		private int x;
		private int y;

		public JoyMsg(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}
	}

	public void setPermissionRequestPending(boolean val) {
		mPermissionRequestPending = val;
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
		
		// Create the USB Accessory Controller
//		mAccessoryController = new MegaADKController(this);
		
		
//		mUsbManager = UsbManager.getInstance(this);

		IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		mConnReceiver = new ConnBroadcastReceiver();
		
		// 2. ConnectivityManager
		registerReceiver(mConnReceiver, connFilter);

		setContentView(R.layout.main);

		enableControls(false);
		
		mUtils = new AccessoryUtils(this);

		mNocServiceRequestor = new PeriodicScheduler(new Runnable () {
			@Override
			public void run() {
				
			}
		});
		
		
/*		// Generate a blue-tooth instance
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter != null) {
		    // Device does not support Bluetooth
			Log.d(TAG, "Got valid mBluetoothAdapter");
       		Toast toast = Toast.makeText(getApplicationContext(), "Got valid Bluetooth Adapter",Toast.LENGTH_SHORT);
    		toast.show();                    
			
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
			startActivityForResult(discoverableIntent, REQUEST_BT_START_DISCOVERY);
		
		}
*/
/*		if (!mBluetoothAdapter.isEnabled()) {
		    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
*/	
		mBTMsgHandler = new Messenger(new BluetoothMsgHandler());
		
		mAccessoryMsgHandler = new Messenger(new AccessoryMsgHandler());
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		
/*		UsbAccessory mAccessory = mAccessoryController.getAccessory();
		
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
*/	
		return super.onRetainNonConfigurationInstance();
		
	}


    @Override
    protected void onStart() {
        super.onStart();

        Intent nocServiceIntent = new Intent(this, NocAccessService.class);
		bindService(nocServiceIntent, mNocConnection, Context.BIND_AUTO_CREATE);
		
		Intent btServiceIntent = new Intent(this, BluetoothService.class);
		bindService(btServiceIntent, mBTConnection, Context.BIND_AUTO_CREATE);

		Intent accessoryIntent = new Intent(this, AccessoryController.class);
		bindService(accessoryIntent, mAccessoryConnection, Context.BIND_AUTO_CREATE);
    
    }
	
	
	@Override
	public void onResume() {
		super.onResume();

//		Toast.makeText(this, "inside onResume of DemoKitActivity", Toast.LENGTH_SHORT).show();

		Intent intent = getIntent();
		
	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mNocConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mNocAccessService = binder.getService();
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
/*    		BluetoothBinder binder = (BluetoothBinder) service;
    		mBluetoothService = binder.getService();
    		Thread mBluetoothServiceThread = new Thread(mBluetoothService);
    		mBluetoothServiceThread.start();
*/    		
    		mBTService= new Messenger (service);
    		Message msg = Message.obtain(null, 0,0,0);
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
    		mAccessoryController = new Messenger (service);
    		Message msg = Message.obtain(null, 0,0,0);
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

    
    @Override
	public void onPause() {
		super.onPause();
//		Toast.makeText(this, "inside onPause of DemoKitActivity", Toast.LENGTH_SHORT).show();
//		mAccessoryFsm.stopUpdates();
	}

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mNocServiceBound) {
            unbindService(mNocConnection);
            mNocServiceBound = false;
        }
    }
	
	@Override
	public void onDestroy() {
		Toast.makeText(this, "inside onDestroy of DemoKitActivity", Toast.LENGTH_SHORT).show();

//		unregisterReceiver(mUsbReceiver);
		unregisterReceiver(mConnReceiver);
//		mAccessoryController.closeAccessory();
		
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

	private int composeInt(byte hi, byte lo) {
		int val = (int) hi & 0xff;
		val *= 256;
		val += (int) lo & 0xff;
		return val;
	}

	
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

/*		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				switch (buffer[i]) {
				case 0x1:
					if (len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_SWITCH);
						m.obj = new SwitchMsg(buffer[i + 1], buffer[i + 2]);
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				case 0x4:
					
					if (len >= 3) {
						Message m = Message.obtain(mHandler,
								MESSAGE_TEMPERATURE);
						m.obj = new TemperatureMsg(composeInt(buffer[i + 1],
								buffer[i + 2]));
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				case 0x5:
					if (len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_LIGHT);
						m.obj = new LightMsg(composeInt(buffer[i + 1],
								buffer[i + 2]));
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				case 0x6:
					if (len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_JOY);
						m.obj = new JoyMsg(buffer[i + 1], buffer[i + 2]);
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
*/	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SWITCH:
				SwitchMsg o = (SwitchMsg) msg.obj;
				handleSwitchMessage(o);
				break;

			case MESSAGE_TEMPERATURE:
				TemperatureMsg t = (TemperatureMsg) msg.obj;
				handleTemperatureMessage(t);
				break;

			case MESSAGE_LIGHT:
				LightMsg l = (LightMsg) msg.obj;
				handleLightMessage(l);
				break;

			case MESSAGE_JOY:
				JoyMsg j = (JoyMsg) msg.obj;
				handleJoyMessage(j);
				break;

			}
		}
	};

	public boolean sendCommand() {
		boolean status = false;
		
/*		switch (cmd) {
		case (USB_CMD_GET_ALIVENESS):
			
			break;
		case (USB_CMD_SET_SETTINGS): 
			break;
		case (USB_CMD_GET_SETTINGS): 
			break;
		case (USB_CMD_RESET_ACCESSORY): 
			break;
		case (USB_CMD_GET_DATA):
			break;
		case (USB_CMD_SEND_HEX):
			break;
		}
*/		
		return status;
	}
	
	public void sendHexFile() {
		
	}
	
	public void sendPacket(byte [] packet) {
		
	}
	
	public byte[] getRspPacket() {
		
		byte[] rsp = new byte[10];
		
		return rsp;
	}

	protected void handleJoyMessage(JoyMsg j) {
	}

	protected void handleLightMessage(LightMsg l) {
	}

	protected void handleTemperatureMessage(TemperatureMsg t) {
	}

	protected void handleSwitchMessage(SwitchMsg o) {
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}
}

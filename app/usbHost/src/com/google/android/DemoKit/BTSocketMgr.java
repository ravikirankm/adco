package com.google.android.DemoKit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class BTSocketMgr extends Thread {
	private final BluetoothServerSocket mmServerSocket;
	
	Activity mActivity = null;

	public BTSocketMgr(BluetoothAdapter mBluetoothAdapter, Activity activity) {
		// Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("TEST_BT_XFER", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        } catch (IOException e) { 
    		Log.d("BTMGR", "Exception when trying to get server socket");
        }
        mmServerSocket = tmp;
        
        mActivity = activity;
	}
	
    public void run() {
        BluetoothSocket socket = null;
    	byte[] buffer = new byte[4];
        
		Log.d("BTMGR", "Started BT Mgr");
		
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(mActivity, "Started BTMgr", Toast.LENGTH_SHORT).show();
			}
		});
		
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
        		Log.d("BTMGR", "Exception when trying to get socket");
            	break;
            }
            // If a connection was accepted
            if (socket != null) {
            	// Do work to manage the connection (in a separate thread)
                // Do work to manage the connection (in a separate thread)
                InputStream tmpIn = null;
                
                // Get the input and output streams, using temp objects because
                // member streams are final
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) { 
            		Log.d("BTMGR", "Exception when trying to get Input Stream");
            		break;
                }
         
                try {
                    tmpIn.read(buffer);
                } catch (IOException e) { 
            		Log.d("BTMGR", "Exception when trying to read from input stream");
            		break;
                }
                     	
        		mActivity.runOnUiThread(new Runnable() {
        			public void run() {
        				Toast.makeText(mActivity, "Got Data from remote device", Toast.LENGTH_SHORT).show();
        			}
        		});

        		Log.d("BTSocketMgr", String.valueOf(buffer[0]));
                Log.d("BTSocketMgr", String.valueOf(buffer[1]));
                Log.d("BTSocketMgr", String.valueOf(buffer[2]));
                Log.d("BTSocketMgr", String.valueOf(buffer[3]));
                
            	
                try {
                	mmServerSocket.close();
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}

package com.google.android.DemoKit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.android.DemoKit.NocAccessService.LocalBinder;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class BluetoothService extends Service implements Runnable {

    // Binder given to clients
    private final IBinder mBinder = new BluetoothBinder();

	private static final int REQUEST_BT_START_DISCOVERY = 1;
	
	private static final int IDLE = 0;
	private static final int WAITING_FOR_DISCOVERY_START = 1;
	private static final int WAITING_FOR_REMOTE_CLIENT = 2;
	private static final int REMOTE_CLIENT_DATA_XFER = 3;
	private static final int REMOTE_CLIENT_DISCONNECT = 4;
	private static final int START_DISCOVERY = 5;
	
	public static final String tempHexFP = "/storage/emulated/legacy/myfolder" + "/" + "new.bin.temp";
	public static final String HexFP = "/storage/emulated/legacy/myfolder" + "/" + "new.bin";

	BluetoothAdapter mBluetoothAdapter;
    BluetoothServerSocket mmServerSocket;
    BluetoothSocket mSocket;
    
    private int curr_state;
    private int next_state;
    
    InputStream in;
    OutputStream out;
    
    Messenger mLocalClientMessenger;
    Messenger mServiceMessenger;
    
    Thread mFSMThread = new Thread(this);
    
    protected class IncomingHandler extends Handler {
    	@Override
       	public void handleMessage(Message msg) {
    		mLocalClientMessenger = msg.replyTo;
    		mFSMThread.start();
    	}    	
    }
    
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BluetoothBinder extends Binder {
        BluetoothService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothService.this;
        }
    }	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		mServiceMessenger = new Messenger(new IncomingHandler());
		return mServiceMessenger.getBinder();
	}

	private void sendLocalClientDebugMsg(String str) {
		if(mLocalClientMessenger != null) {
			Message msg = Message.obtain(null, 1, 0, 0);
			Bundle msgBundle = new Bundle(); 
			msgBundle.putString("debug", str);
			msg.setData(msgBundle);
			try {
				mLocalClientMessenger.send(msg);
			} catch (RemoteException e) {}
		}
	}
	
	public void run() {
		curr_state = IDLE;
		next_state = IDLE;
			
		while (true) {
			
			switch (curr_state) {
/*			case (IDLE):
				// Wait till local CLIENT is connected
				if(mLocalClientMessenger == null) {
					try { Thread.currentThread().sleep(1000);} 
					catch (Exception e) {}
				} else {
					next_state = START_DISCOVERY;
				}
				break;
			case (START_DISCOVERY):
*/
			case (IDLE):
				sendLocalClientDebugMsg("BT Service: curr_state = IDLE");
				makeDiscoverable();
				next_state = WAITING_FOR_DISCOVERY_START;
				break;
			case (WAITING_FOR_DISCOVERY_START):
				sendLocalClientDebugMsg("BT Service: curr_state = WAITING_FOR_DISCOVERY_START");
				if(mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
					next_state = WAITING_FOR_REMOTE_CLIENT;
				} else {
					try {
						Thread.currentThread().sleep(1000);
					} catch (Exception e) {
						
					}
				}
				break;
				
			case (WAITING_FOR_REMOTE_CLIENT):
				sendLocalClientDebugMsg("BT Service: curr_state = WAITING_FOR_CLIENT");
				if(connect()) {
					next_state = REMOTE_CLIENT_DATA_XFER;
				} else {
					next_state = IDLE;
				}
				break;
			case (REMOTE_CLIENT_DATA_XFER):
				sendLocalClientDebugMsg("BT Service: curr_state = REMOTE_CLIENT_DATA_XFERT");
				FileOutputStream tempFileO = null;
	
				File tempFile = new File(tempHexFP);
				try {
					tempFileO = new FileOutputStream(tempFile);
				} catch (FileNotFoundException e) {
					
				}
				boolean downloadStatus = downloadFromClient(tempFileO);
				try {	
				tempFileO.close();
				} catch (Exception e) {
				}
				if(downloadStatus) {
					tempFile.renameTo(new File(HexFP));
				} else {
				}
				tempFile.delete();
				next_state = REMOTE_CLIENT_DISCONNECT;
				break;
			case (REMOTE_CLIENT_DISCONNECT) :
				disconnect();
				next_state = IDLE;
				break;
			}
			curr_state = next_state;
		}
	}
	
	public void makeDiscoverable() {
		if(mBluetoothAdapter == null) {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				if (mBluetoothAdapter != null) {
					// Device does not support Bluetooth
//					Toast.makeText(getApplicationContext(), "Got valid Bluetooth Adapter",Toast.LENGTH_SHORT).show();
					sendLocalClientDebugMsg("Got valid Bluetooth Adapter");
					
					Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
					discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(discoverableIntent);
			}
		}
	}

	public boolean connect() {

        try {
            // MY_UUID is the app's UUID string, also used by the client code
            mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("TEST_BT_XFER", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        } catch (IOException e) { 
//    		Toast.makeText(getApplicationContext(), "Exception when trying to get server socket", Toast.LENGTH_SHORT).show();
    		sendLocalClientDebugMsg("Exception when trying to get server socket");
    		return false;
        }
        
        try {
            mSocket = mmServerSocket.accept();
            mmServerSocket.close();
        } catch (IOException e) {
        	return false;
        }
        
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            in = mSocket.getInputStream();
            out = mSocket.getOutputStream();
        } catch (IOException e) { 
    		return false;
        }
        
        return true;

	}
	
	public void disconnect() {
		try {
			if(in != null) {
				in.close();
			}
			if(out != null) {
				out.close();
			}
			if(mSocket != null) {
				mSocket.close();
			}
		}
		catch(IOException e) {
    		// Toast.makeText(getApplicationContext(), "Exception when trying to close IO streams", Toast.LENGTH_SHORT).show();
    		sendLocalClientDebugMsg("Exception when trying to close IO streams");
		}
		finally {
			in = null;
			out = null;
			mSocket = null;
		}
	}
	
	public boolean downloadFromClient(FileOutputStream tempFileO) {
		
		final int PACKET_HEADER = 0;
		final int PACKET_LENGTH = 1;
		final int PACKET_PAYLOAD = 2;
		final int PACKET_CRC = 3;
		final int CHECK_CRC = 4;
		
		int packet_state = PACKET_HEADER;
		int packet_length = 0;
		int length_byte_count = 0;
		int payload_count = 0;
		int crc_byte_count = 0;
		int crc = 0;
		int exp_crc = 0;
		
		while (true) {
			byte [] readBuffer = new byte[1];
			boolean readStatus = readByteFromClient(readBuffer);

			if(!readStatus) {
	    		sendLocalClientDebugMsg("Got resp of :" + readStatus + ", exiting State machine");
				break;
			}

			exp_crc += readBuffer[0];
			
			switch (packet_state) {
			case (PACKET_HEADER) :
				if(readBuffer[0] == 0xf) {
					packet_state = PACKET_LENGTH;
				} else {
					return false;
				}
				sendLocalClientDebugMsg("BT download PACKET_HEADER: Got header: " + readBuffer[0]);			
				break;	
			case (PACKET_LENGTH) :
				packet_length = packet_length | (readBuffer[0] << 8*length_byte_count);
				
				sendLocalClientDebugMsg("BT download PACKET_LENGTH: Got length[: " + length_byte_count + "] = " + readBuffer[0] + "; packet_length = " + packet_length);			
				length_byte_count++;
				
				if(length_byte_count > 3) {
					packet_state = PACKET_PAYLOAD;
					length_byte_count = 0;
				}
				break;
			case (PACKET_PAYLOAD) :
				sendLocalClientDebugMsg("BT State PACKET_PAYLOAD");			
/*				try {
					tempFileO.write(readBuffer[0]);
				} catch (Exception e) {
					sendLocalClientDebugMsg("BT download PACKET_PAYLOAD: Exception, Failed to write to file");			
					return false;
				}
*/
				payload_count++;

				if(payload_count > packet_length) {
			    	packet_state = PACKET_CRC;
				}
				break;
			case (PACKET_CRC) :
				sendLocalClientDebugMsg("BT download PACKET_CRC: Got crc[: " + crc_byte_count + "] = " + readBuffer[0] + "; crc = " + crc);			
				crc = crc | (readBuffer[0] << 8*crc_byte_count);
			
				crc_byte_count++;

				if(crc_byte_count > 3) {
					crc_byte_count = 0;
					if(crc == exp_crc) {
						// Send message to handler to indicate that file has
						// been received correctly
						sendLocalClientDebugMsg("BT download CHECK_CRC: Matched crc");			
						return true;
					} else {
						// discard the file we have accumulated as we have bad crc
						sendLocalClientDebugMsg("BT download CHECK_CRC: Mis-matched crc");			
						return false;
					}
				}
				break;
			}
		}		
		return false;
	}
	
	public boolean readByteFromClient(final byte [] readBuffer) {
		// Create an Async Task
		AsyncTask<InputStream, Void, Boolean> clientReadTask = new AsyncTask<InputStream, Void, Boolean> () {
			protected Boolean doInBackground(InputStream... ins) {
				int count = ins.length;
				for(int i = 0; i< count; i++) {
					try {
						if(ins[i] != null) {
							readBuffer[0] = (byte) ins[i].read();
							return true;
						}
					}
					catch (IOException e) {
					}
				}
				return false;
			}
		};
		
		InputStream [] inStreams = new InputStream[1];
		inStreams[0] = in;
		clientReadTask.execute(inStreams);

		try {
			return clientReadTask.get(60, TimeUnit.SECONDS);
		} 
		catch (Exception e){
			sendLocalClientDebugMsg("BT Client Read Timeout");			
			return false;
		}
		
	}
}

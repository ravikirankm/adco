package com.google.android.DemoKit;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

public class MegaADKController extends AccessoryController implements Runnable {
	
	public static final int USB_CMD_SET_SETTINGS = 0x1;
	public static final int USB_CMD_GET_SETTINGS = 0x2;
	public static final int USB_CMD_GET_DATA = 0x3;
	public static final int USB_CMD_GET_ALIVENESS = 0x4;
	public static final int USB_CMD_RESET_ACCESSORY = 0x5;
	public static final int USB_CMD_SEND_HEX = 0x6;
	public static final int USB_CMD_ERROR = 0x7;

	private static final int USB_STATE_IDLE = 0x1;
	private static final int USB_STATE_CHECK_ALIVENESS = 0x2;
	private static final int USB_STATE_GET_DATA = 0x3;
	private static final int USB_STATE_UPDATE_SETTINGS = 0x4;
	private static final int USB_STATE_UPDATE_HEX = 0x5;
	
	private static final int USB_STATUS_DATA_AVAILABLE_MASK = 0x1;

	private int next_state = USB_STATE_IDLE;
	private int curr_state = USB_STATE_IDLE;
	
	private boolean mAccessoryReady;
	private boolean mHexXferInProg;


	private Object usbIOLock = new Object();

	
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	private UsbManager mUsbManager;
	
	public MegaADKController(DemoKitActivity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onAccesssoryAttached() {
		// TODO Auto-generated method stub

	}

	private void sendCmdPacket(AccessoryPacket req) {
		byte [] buffer;
		// overhead is 1 for cmd, 4 for length, 2 for crc
		int overhead = 1 + 4 + 2;
		
		// get the length of the payload and add it to the pkt header
		int length = req.length + overhead;
		
		buffer = new byte[length];
		
		buffer[0] = (byte) req.cmd;
		buffer[1] = (byte) length;
		buffer[2] = (byte) (length >> 8);
		buffer[3] = (byte) (length >> 16);
		buffer[4] = (byte) (length >> 24);
		
		for(int i = 0; i < req.length; i++) {
			buffer[5 + i] = (byte) req.payld[i];
		}

		// Generate the CRC here
		buffer[req.length + 5] = 0;
		buffer[req.length + 5 + 1] = 0;
		
		for (int i = 0; i < buffer.length; i++){
			byte [] temp = new byte[1];
			temp[0] = buffer[i];
			usbWrite(temp);
		}
		
	}
	
	private void sendAckPacket() {
		
	}
	
	private void getCmdRspPacket(AccessoryPacket rsp){
		byte [] buffer = new byte[16384];
		int status = usbRead(buffer);
		if(status > 0) {
			int length = status;
			int readPtr = 0;
			int i;
			rsp = new AccessoryPacket();
			Toast.makeText(mHostActivity, "Got CMD response from Accessory of length " + length, Toast.LENGTH_SHORT).show();
			rsp.cmd = buffer[0];
			rsp.length = buffer[1] | (buffer[2] << 8) | (buffer[3] << 16) | (buffer[4] << 24);
			readPtr = readPtr + 5;
			for(i = readPtr; i < length - 2; i++) {
				rsp.payld[i - readPtr] = buffer[i];
			}
			readPtr = readPtr + i;
			rsp.crc = buffer[readPtr] + (buffer[readPtr + 1] << 8); 
		}
	}
	
	@Override
	protected boolean initiateHostXfer(AccessoryPacket req, AccessoryPacket rsp) {

		switch(req.cmd) {
		case (USB_CMD_GET_ALIVENESS):
			sendCmdPacket(req);
			getCmdRspPacket(rsp);
			break;
		case (USB_CMD_SET_SETTINGS):
			sendCmdPacket(req);
			getCmdRspPacket(rsp);
			break;
		
		case (USB_CMD_GET_SETTINGS):
			sendCmdPacket(req);
			getCmdRspPacket(rsp);
			sendAckPacket();
			break;
		case (USB_CMD_RESET_ACCESSORY): 
			sendCmdPacket(req);
			getCmdRspPacket(rsp);
			break;
		case (USB_CMD_GET_DATA):
			sendCmdPacket(req);
			getCmdRspPacket(rsp);
			sendAckPacket();
			break;
		case (USB_CMD_SEND_HEX):
			sendCmdPacket(req);
			getCmdRspPacket(rsp);
			break;
 		}
		
		return false;
	}
	
	public void run() {

		if(!mAccessoryReady) {
			Toast.makeText(mHostActivity, "USB Accessory is not ready", Toast.LENGTH_SHORT).show();
			return;
		}

		Toast.makeText(mHostActivity, "Invoking USB Accessory State Machine", Toast.LENGTH_SHORT).show();
		
		while(true) {
		
			switch (curr_state) {
		
			case(USB_STATE_IDLE):
				next_state = USB_STATE_CHECK_ALIVENESS;
				Toast.makeText(mHostActivity, "USB state machine STATE_IDLE", Toast.LENGTH_SHORT).show();
				break;
			
			case(USB_STATE_CHECK_ALIVENESS) :
				AccessoryPacket req = new AccessoryPacket();
				AccessoryPacket rsp = null;
				req.cmd = USB_CMD_GET_ALIVENESS;
				req.length = 0;

				Toast.makeText(mHostActivity, "USB state machine STATE_CHECK_ALIVENESS", Toast.LENGTH_SHORT).show();
			
				initiateHostXfer(req, rsp);
				// Check if we got a successful response from Accessory
				if(rsp != null) {

					Toast.makeText(mHostActivity, "Got Rsp of " + rsp.cmd + " - Length: " + rsp.length + ", CRC: " + rsp.crc, Toast.LENGTH_SHORT).show();
					
					// check if we got an error response
					if(rsp.cmd == USB_CMD_ERROR) {
						Toast.makeText(mHostActivity, "Got error response from Accessory", Toast.LENGTH_SHORT).show();
						break;
					}
					if((rsp.payld[0] & USB_STATUS_DATA_AVAILABLE_MASK) != 0) {
						next_state = USB_STATE_GET_DATA;
					} 
				}
				next_state = USB_STATE_IDLE;
				break;
		
			case(USB_STATE_GET_DATA) :
				break;
			}
		
			curr_state = next_state;
			
			if(next_state == USB_STATE_IDLE) {
				break;
			}
		}
	}
	
	public void openAccessory() {
		// if accessory is already connected, no need to do anything just return
		if (mInputStream != null && mOutputStream != null) {
			Toast.makeText(mHostActivity, "IO streams are non-null returning", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if(mUsbManager == null) {
			mUsbManager = (UsbManager) mHostActivity.getSystemService(Context.USB_SERVICE);
		}
		
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				Toast.makeText(mHostActivity, "Opening accessory", Toast.LENGTH_SHORT).show();
				openAccessoryIO(accessory);
			} else {
//				synchronized (mUsbReceiver) {
				PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mHostActivity, 0, new Intent(
						DemoKitActivity.ACTION_USB_PERMISSION), 0);

				Toast.makeText(mHostActivity, "Requesting Permission", Toast.LENGTH_SHORT).show();
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
//				}
			}
		} else {
			Toast.makeText(mHostActivity, "Accessory is null", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	public void openAccessoryIO(UsbAccessory accessory) {
		
		if(mFileDescriptor == null) {
			mFileDescriptor = mUsbManager.openAccessory(accessory);
			Toast.makeText(mHostActivity, "Obtaining new File Descriptor from UsbManager", Toast.LENGTH_SHORT).show();
		}

		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			mAccessoryReady = true;
			mHostActivity.enableControls(true);
		} else {
			Toast.makeText(mHostActivity, "File Descriptor is null", Toast.LENGTH_SHORT).show();
		}
		
	}

	public void closeAccessory() {
		mHostActivity.enableControls(false);
		mAccessoryReady = false;
		Toast.makeText(mHostActivity, "Closing accessory", Toast.LENGTH_SHORT).show();

		try {
			if (mFileDescriptor != null) {
				Toast.makeText(mHostActivity, "Closing FileDescriptor", Toast.LENGTH_SHORT).show();
				mFileDescriptor.close();
			}
			if(mInputStream != null) {
				mInputStream.close();
			}
			if(mOutputStream != null) {
				synchronized(usbIOLock) {
					mOutputStream.close();
				}
			}

		} catch (IOException e) {
			Toast.makeText(mHostActivity, "Close accessory IOException", Toast.LENGTH_SHORT).show();
			
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
			mInputStream = null;
			synchronized(usbIOLock) {
				mOutputStream = null;
			}
		}
	}

	public boolean usbWrite(byte [] writeBuffer) {
		synchronized(usbIOLock) {
			try {
				if(mOutputStream != null) {
					mOutputStream.write(writeBuffer);
					mOutputStream.flush();
					return true;
				} else {
					return false;
				}
			}
			catch (IOException e) {
				return false;
			}
		}
	}
	
	public int usbRead(byte [] readBuffer) {
		synchronized(usbIOLock) {
			try {
				if(mInputStream != null) {
					int status = mInputStream.read(readBuffer);
					return status;
				} else {
					return -1;
				}
			}
			catch (IOException e) {
				return -1;
			}
		}
	}

	public void sendHexFile() {

		if(!mAccessoryReady) {
	   		Toast.makeText(mHostActivity, "Accessory is not ready",Toast.LENGTH_SHORT).show();
			return;
		}
		
		if(mHexXferInProg) {
	   		Toast.makeText(mHostActivity, "Thread already running",Toast.LENGTH_SHORT).show();
			return;
		}

		mHexXferInProg = true;
		
		new Thread(
			new Runnable() {
				public void run() {
					
					byte[] buffer = new byte[5];
					int failed_write_bytes = 0, bytes_sent = 0;

					int currByte;
					FileInputStream hexFileFIO = null;
					File hexFile = new File(mHostActivity.localHexFP);
					long hexFileLen = hexFile.length();

					try {
						hexFileFIO = new FileInputStream(hexFile);
						
					} catch (FileNotFoundException e) {
						mHexXferInProg = false;
						mHostActivity.toastHandler("Unable to open hex file for reading");
						return;
						
					}
					
					mHostActivity.toastHandler("Sending Hex File... " + String.valueOf(hexFileLen) + "bytes");
					
					buffer[0] = 0x1;
					buffer[1] = (byte) hexFileLen;
					buffer[2] = (byte) (hexFileLen >> 8);
					buffer[3] = (byte) (hexFileLen >> 16);
					buffer[4] = (byte) (hexFileLen >> 24);
					try {
						if(!usbWrite(buffer)) { 
							hexFileFIO.close();
							mHexXferInProg = false;
							return;
						}
						
						while((currByte = hexFileFIO.read()) != -1) {
							byte[] payld = new byte[1];
							payld[0] = (byte) currByte;

							if(bytes_sent > hexFileLen) {
								mHostActivity.toastHandler("Sent bytes = " + bytes_sent + " currByte = " + currByte);
						   		break;
							}
							
							if(usbWrite(payld)) {
								bytes_sent++;
							} else {
								mHostActivity.toastHandler("Write to output stream failed");
								failed_write_bytes++;
								hexFileFIO.close();
								mHexXferInProg = false;
								return;
							}
						}

						mHostActivity.toastHandler("Done sending Hex File! (missed bytes = " + 
				   				failed_write_bytes +
				   				"bytes_sent = " +
				   				bytes_sent +
				   				")");
						
					} catch (IOException e){
						mHostActivity.toastHandler("Reading Hex file failed:" + e);
						mHexXferInProg = false;
						return;
					}
					mHexXferInProg = false;
					mAccessoryReady = false;

				}					
			}
		).start();

	}
	
	
	
}

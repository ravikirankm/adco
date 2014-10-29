package com.google.android.DemoKit;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

public class MegaADKController extends AccessoryController implements Runnable {
	
	public static final int USB_CMD_SET_SETTINGS = 0x1;
	public static final int USB_CMD_GET_SETTINGS = 0x2;
	public static final int USB_CMD_GET_DATA = 0x3;
	public static final int USB_CMD_GET_ALIVENESS = 0x4;
	public static final int USB_CMD_RESET_ACCESSORY = 0x5;
	public static final int USB_CMD_SEND_HEX = 0x6;
	public static final int USB_CMD_ERROR = 0x7;
	public static final int USB_CMD_ACK = 0x8;

	private static final int USB_STATE_IDLE = 0x1;
	private static final int USB_STATE_CHECK_ALIVENESS = 0x2;
	private static final int USB_STATE_GET_DATA = 0x3;
	private static final int USB_STATE_UPDATE_SETTINGS = 0x4;
	private static final int USB_STATE_UPDATE_HEX = 0x5;
	
	private static final int USB_STATUS_DATA_AVAILABLE_MASK = 0x1;

	private static final int USB_READ_TIMEOUT = 1000;
	public static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	public static final String localHexFP = "/storage/emulated/legacy/myfolder" + "/" + "new.bin";

	private int next_state = USB_STATE_IDLE;
	private int curr_state = USB_STATE_IDLE;
	
	private boolean mAccessoryReady;
	private boolean mHexXferInProg;


	private Object usbIOLock = new Object();

	
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	private UsbManager mUsbManager;
	private UsbBroadcastReceiver mUsbReceiver;
    PeriodicScheduler mAccessoryFsm;
	
	public MegaADKController(DemoKitActivity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onCreate() {
		IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
		usbFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		usbFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		
		mUsbReceiver = new UsbBroadcastReceiver(this);

		// Register all the broadcast receivers
		// 1. USB
		registerReceiver(mUsbReceiver, usbFilter);
		
		mAccessoryFsm = new PeriodicScheduler(this, 15000);
		
		openAccessory();
		
		// Start the periodic Accessory checker
		mAccessoryFsm.startUpdates();
		
	}

	@Override
	public void onDestroy() {
		mAccessoryFsm.stopUpdates();
		unregisterReceiver(mUsbReceiver);
		
	}
	

	@Override
	protected void onAccesssoryAttached() {
		// TODO Auto-generated method stub

	}

	private void sendCmdPacket(AccessoryPacket req) {
		byte [] buffer;
		int crc = 0;
		
		// overhead is 1 for cmd, 4 for length, 2 for crc
		int overhead = 1 + 4 + 2;
		
		// get the length of the payload and add it to the pkt header
		int length = req.length + overhead;
		
		buffer = new byte[length];
		
		buffer[0] = (byte) req.cmd;
		buffer[1] = (byte) req.length;
		buffer[2] = (byte) (req.length >> 8);
		buffer[3] = (byte) (req.length >> 16);
		buffer[4] = (byte) (req.length >> 24);
		
		crc += req.cmd + req.length;
		
		for(int i = 0; i < req.length; i++) {
			buffer[5 + i] = (byte) req.payld[i];
			crc += req.payld[i];
		}

		// Generate the CRC here
		buffer[req.length + 5] = (byte) crc;
		buffer[req.length + 5 + 1] = (byte) (crc>>8);
		
		for (int i = 0; i < buffer.length; i++){
			byte [] temp = new byte[1];
			temp[0] = buffer[i];
			usbWrite(temp);
		}
	}
	
	private void sendAckPacket() {
		
	}

	
	private boolean getCmdRspPacket(AccessoryPacket req, AccessoryPacket rsp){
		byte [] cmdBuffer = new byte[512];
		int bPtr = 0;
		int status = 0;
		int length = 0;
		int crc = 0;
		int pktLen;
		
		
		sendClientDebugMsg("Trying to get rsp from accessory...");
		pktLen = usbRead(cmdBuffer);

		if(pktLen >= 7) {
			// check that the rsp cmd matches the req
			if(req.cmd == cmdBuffer[bPtr]) {
				
//				sendClientDebugMsg("Got CMD response from Accessory " + cmdBuffer[bPtr]);
				
				rsp = new AccessoryPacket();
				rsp.cmd = cmdBuffer[bPtr];
				crc += cmdBuffer[bPtr];
				bPtr++;
				// Get the length, which is the next four bytes
				for(int i = 0; i < 4; i++) {
					byte [] lenBuffer = new byte[1]; 
					length = length | (cmdBuffer[bPtr] << (8*i));
					crc += cmdBuffer[bPtr];
					bPtr++;
				}
//				sendClientDebugMsg("Got CMD length from Accessory " + length);
				
				if(pktLen < length + 7) {
					sendClientDebugMsg("CMD length less than pkt length - discarding packet");
					rsp = null;
					return false;
				}
				
				rsp.length = length;
				rsp.payld = new int[length];
				
				// Get the payload, which is the number of bytes given by length
				for(int i = 0; i < length; i++) {
					byte [] payldBuffer = new byte[1];
					rsp.payld[i] = (int) cmdBuffer[bPtr];
					crc += cmdBuffer[bPtr];
					bPtr++;
				}
				
				// Get the crc, which is the next 2 bytes
				for(int i = 0; i < 2; i++) {
					byte [] crcBuffer = new byte[1];
					rsp.crc = rsp.crc | (cmdBuffer[bPtr] << (8*i));
					bPtr++;
				}
//				sendClientDebugMsg("Got CRC from Accessory " + rsp.crc);
				
				// Discard response if crc fails
				if(crc != rsp.crc) {
					sendClientDebugMsg("Got CRC error, discarding response");
				}
				return true;
			}
			else {
				sendClientDebugMsg("Got CMD that is a mismatch: " + cmdBuffer[0]);
			}
		}
		else {
			sendClientDebugMsg("Got bad USB packet, discarding");
		}
		return false;
		
	}
	
	@Override
	protected boolean initiateHostXfer(AccessoryPacket req, AccessoryPacket rsp) {

		switch(req.cmd) {
		case (USB_CMD_GET_ALIVENESS):
			sendCmdPacket(req);
			return getCmdRspPacket(req, rsp);
		case (USB_CMD_SET_SETTINGS):
			sendCmdPacket(req);
			return getCmdRspPacket(req,rsp);
		case (USB_CMD_GET_SETTINGS):
			sendCmdPacket(req);
			return getCmdRspPacket(req,rsp);
		case (USB_CMD_RESET_ACCESSORY): 
			sendCmdPacket(req);
			return getCmdRspPacket(req,rsp);
		case (USB_CMD_GET_DATA):
			sendCmdPacket(req);
			if(getCmdRspPacket(req,rsp)) {
				sendAckPacket();
				return true;
			}
		case (USB_CMD_SEND_HEX):
			sendCmdPacket(req);
			getCmdRspPacket(req,rsp);
			break;
 		}
		
		return false;
	}
	
	public void run() {

		if(!mAccessoryReady) {
//			sendClientDebugMsg("USB Accessory is not ready");
			return;
		}
		
		if(curr_state != USB_STATE_IDLE) {
			sendClientDebugMsg("USB FSM is busy, retring later");
			return;
		}

//		sendClientDebugMsg("Invoking USB Accessory State Machine");
		while(true) {
		
			switch (curr_state) {
			
				case(USB_STATE_IDLE): 
				{
					next_state = USB_STATE_CHECK_ALIVENESS;
//				sendClientDebugMsg("USB state machine STATE_IDLE");
					break;
				}
				case(USB_STATE_CHECK_ALIVENESS) :
				{
					AccessoryPacket req = new AccessoryPacket();
					AccessoryPacket rsp = new AccessoryPacket();
					next_state = USB_STATE_IDLE;
	
					req.cmd = USB_CMD_GET_ALIVENESS;
					req.length = 0;
	
	//				sendClientDebugMsg("USB state machine STATE_CHECK_ALIVENESS");
					boolean status = initiateHostXfer(req, rsp);
					
					// Check if we got a successful response from Accessory
					if(status) {
						sendClientDebugMsg("Got Rsp of " + rsp.cmd + " - Length: " + rsp.length + ", CRC: " + rsp.crc);
						
						// check if we got an error response
						if(rsp.cmd == USB_CMD_ERROR) {
							sendClientDebugMsg("Got error response from Accessory");
							break;
						}
						if((rsp.payld[0] & USB_STATUS_DATA_AVAILABLE_MASK) != 0) {
							next_state = USB_STATE_GET_DATA;
						} 
					}
					break;
				}
				case(USB_STATE_GET_DATA) :
				{
					AccessoryPacket req = new AccessoryPacket();
					AccessoryPacket rsp = new AccessoryPacket();
					next_state = USB_STATE_IDLE;
	
					req.cmd = USB_CMD_GET_DATA;
					req.length = 0;
	
	//				sendClientDebugMsg("USB state machine STATE_GET_DATA");
					boolean status = initiateHostXfer(req, rsp);
					
					// Check if we got a successful response from Accessory
					if(status) {
						sendClientDebugMsg("Got Rsp of " + rsp.cmd + " - Length: " + rsp.length + ", CRC: " + rsp.crc);
						
						// check if we got an error response
						if(rsp.cmd == USB_CMD_ERROR) {
							sendClientDebugMsg("Got error response from Accessory");
							break;
						}
					}
					break;
				}
				case (USB_STATE_UPDATE_SETTINGS) :
				{
					AccessoryPacket getSettingReq = new AccessoryPacket();
					AccessoryPacket getSettingRsp = new AccessoryPacket();
					AccessoryPacket setSettingReq = new AccessoryPacket();
					AccessoryPacket setSettingRsp = new AccessoryPacket();
					
					getSettingReq.cmd = USB_CMD_GET_SETTINGS;
					getSettingReq.length = 0;
					
					boolean getStatus = initiateHostXfer(getSettingReq, getSettingRsp);
					
					if(getStatus) {
						sendClientDebugMsg("Got Rsp of " + getSettingRsp.cmd + " - Length: " + getSettingRsp.length + ", CRC: " + getSettingRsp.crc);

						// check if we got an error response
						if(getSettingRsp.cmd == USB_CMD_ERROR) {
							sendClientDebugMsg("Got error response from Accessory");
							break;
						}
						
						setSettingReq.cmd = USB_CMD_SET_SETTINGS;
						setSettingReq.length = getSettingRsp.length;
						setSettingReq.payld = getSettingRsp.payld;
						
						boolean setStatus = initiateHostXfer(setSettingReq, setSettingRsp);
						if(setStatus) {
							sendClientDebugMsg("Got Rsp of " + setSettingRsp.cmd + " - Length: " + setSettingRsp.length + ", CRC: " + setSettingRsp.crc);

							// check if we got an error response
							if(setSettingRsp.cmd == USB_CMD_ERROR) {
								sendClientDebugMsg("Got error response from Accessory");
								break;
							}
						}
						// Signal that update settings was successful
						sendClientDebugMsg("Set settings successfully!");
					}
				}
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
//			sendClientDebugMsg("IO streams are non-null returning");
			return;
		}
		
		if(mUsbManager == null) {
			mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		}
		
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		
		if (accessory != null) {
//			mAccessory = accessory;
			if (mUsbManager.hasPermission(accessory)) {
				sendClientDebugMsg("Opening accessory");
				openAccessoryIO(accessory);
			} else {
				PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
						ACTION_USB_PERMISSION), 0);

//				sendClientDebugMsg("Requesting Permission");
				mUsbManager.requestPermission(accessory, mPermissionIntent);
			}
		} else {
			sendClientDebugMsg("Accessory is null");
		}
		
	}
	
	public void openAccessoryIO(UsbAccessory accessory) {
		
		if(mFileDescriptor == null) {
			mFileDescriptor = mUsbManager.openAccessory(accessory);
			sendClientDebugMsg("Obtaining new File Descriptor from UsbManager");
		}

		if (mFileDescriptor != null) {
//			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			mAccessoryReady = true;
//			mHostActivity.enableControls(true);
		} else {
			sendClientDebugMsg("File Descriptor is null");
		}
		
	}

	public void closeAccessory() {
//		mHostActivity.enableControls(false);
		mAccessoryReady = false;
		sendClientDebugMsg("Closing accessory");

		try {
			if (mFileDescriptor != null) {
				sendClientDebugMsg("Closing FileDescriptor");
				mFileDescriptor.close();
			}
			if(mInputStream != null) {
				synchronized(usbIOLock) {
					mInputStream.close();
				}
			}
			if(mOutputStream != null) {
				synchronized(usbIOLock) {
					mOutputStream.close();
				}
			}

		} catch (IOException e) {
			sendClientDebugMsg("Close accessory IOException");
		} finally {
			mFileDescriptor = null;
//			mAccessory = null;
			synchronized(usbIOLock) {
				mOutputStream = null;
				mInputStream = null;
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
	
	public int usbRead(final byte [] readBuffer) {
		
		// Create an Async Task
		AsyncTask<FileInputStream, Void, Integer> usbReadTask = new AsyncTask<FileInputStream, Void, Integer> () {
			protected Integer doInBackground(FileInputStream... ins) {
				int count = ins.length;
				for(int i = 0; i< count; i++) {
					try {
						if(ins[i] != null) {
//							readBuffer[0] = (byte) ins[i].read();
							return ins[i].read(readBuffer);
//							return 1;
						}
					}
					catch (IOException e) {
						sendClientDebugMsg("Got IO Exception when trying to read from Accessory");
					}
				}
				return -1;
			}
		};
		
		FileInputStream [] inStreams = new FileInputStream[1];
		inStreams[0] = mInputStream;
		AsyncTask<FileInputStream, Void, Integer> usbReadTaskAct = usbReadTask.execute(inStreams);
		
		try {
			return usbReadTaskAct.get(10, TimeUnit.SECONDS);
		} 
		catch (Exception e){
			sendClientDebugMsg("Got Timeout when trying to read from Accessory");

//			usbReadTaskAct.cancel(true);
			return -1;
		}
	}		

	public void emptyRxBuffer() {
		byte [] rxBuffer = new byte[1];
		int status;
		// loop till we get either a timeout or an exception
		while (true) {
			status = usbRead(rxBuffer);
			if(status == -1) {
				break;
			}
		}
	}

	public void sendHexFile() {

		if(!mAccessoryReady) {
//	   		Toast.makeText(mHostActivity, "Accessory is not ready",Toast.LENGTH_SHORT).show();
			return;
		}
		
		if(mHexXferInProg) {
//	   		Toast.makeText(mHostActivity, "Thread already running",Toast.LENGTH_SHORT).show();
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
					File hexFile = new File(localHexFP);
					long hexFileLen = hexFile.length();

					try {
						hexFileFIO = new FileInputStream(hexFile);
						
					} catch (FileNotFoundException e) {
						mHexXferInProg = false;
						sendClientDebugMsg("Unable to open hex file for reading");
						return;
						
					}
					
					sendClientDebugMsg("Sending Hex File... " + String.valueOf(hexFileLen) + "bytes");
					
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
								sendClientDebugMsg("Sent bytes = " + bytes_sent + " currByte = " + currByte);
						   		break;
							}
							
							if(usbWrite(payld)) {
								bytes_sent++;
							} else {
								sendClientDebugMsg("Write to output stream failed");
								failed_write_bytes++;
								hexFileFIO.close();
								mHexXferInProg = false;
								return;
							}
						}

						sendClientDebugMsg("Done sending Hex File! (missed bytes = " + 
				   				failed_write_bytes +
				   				"bytes_sent = " +
				   				bytes_sent +
				   				")");
						
					} catch (IOException e){
						sendClientDebugMsg("Reading Hex file failed:" + e);
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

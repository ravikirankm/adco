package com.google.android.DemoKit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class FtpMgr extends Thread {

	DemoKitActivity mActivity = null;
	String remoteFP = "/home/ravi/workspace/boardHexTransfer/Debug/" + "boardHexTransfer.bin";	
	public FtpMgr(Context context) {
		mActivity = (DemoKitActivity) context;
	}
	
	public void run(){

		mActivity.toastHandler("Started FtpMgr");
/*		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(mActivity, "Started FtpMgr", Toast.LENGTH_SHORT).show();
			}
		});
*/	    FileOutputStream videoOut;
		File targetFile = new File(mActivity.localHexFP);
//		FTPClientConfig ftpClientConfig = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
//	    ftpClientConfig.setServerLanguageCode("fr");
	    
		try {
		    FTPClient ftpClient = new FTPClient();
//		    ftpClient.configure(ftpClientConfig);
//	        ftpClient.connect("192.168.0.20", 21);

//		    ftpClient.connect(InetAddress.getByName("192.168.0.20"), 21);
		    ftpClient.connect(InetAddress.getByName("98.26.23.101"), 21);
	        ftpClient.enterLocalPassiveMode();
	        boolean result = ftpClient.login("ravi", "hkrk1314");
	        if(result) {
	    		mActivity.runOnUiThread(new Runnable() {
	    			public void run() {
	    				Toast.makeText(mActivity, "Logged into FTP server", Toast.LENGTH_SHORT).show();
	    			}
	    		});
	        } else {
	    		mActivity.runOnUiThread(new Runnable() {
	    			public void run() {
	    				Toast.makeText(mActivity, "Cannot login into FTP server", Toast.LENGTH_SHORT).show();
	    			}
	    		});
	        }

	        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);// Used for video
	        targetFile.createNewFile();
	        videoOut = new FileOutputStream(targetFile);

/*	        File file = new File(Environment.getExternalStoragePublicDirectory(
	                Environment.DIRECTORY_PICTURES), "test.jpg");
*/
//	        videoOut = new FileOutputStream("/storage/emulated/legacy/myfolder" + "/" + "test.bin");
//	        videoOut = new FileOutputStream(file);
	        
	        FTPFile ftpFiles[] = ftpClient.listFiles(remoteFP);
	        for (FTPFile ftpFile: ftpFiles) {
	        	String timestamp = ftpFile.getTimestamp().getTime().toString();
	        	mActivity.toastHandler("Got timestamp as " + timestamp);
	        }
	        
	        result = ftpClient.retrieveFile(remoteFP, videoOut);

//	        InputStream in = ftpClient.retrieveFileStream("/home/ravi/workspace/boardHexTransfer/Debug/" + "new.bin");
	        
	        if(result) {
	    		mActivity.runOnUiThread(new Runnable() {
	    			public void run() {
	    				Toast.makeText(mActivity, "Retrieved FTP file", Toast.LENGTH_SHORT).show();
	    			}
	    		});
	        } else {
	    		mActivity.runOnUiThread(new Runnable() {
	    			public void run() {
	    				Toast.makeText(mActivity, "Could not retrieve FTP file", Toast.LENGTH_SHORT).show();
	    			}
	    		});
	        }
					        
	        ftpClient.disconnect();
	        videoOut.flush();
	        videoOut.close();    
    		mActivity.runOnUiThread(new Runnable() {
    			public void run() {
    				Toast.makeText(mActivity, "Closed File Handle", Toast.LENGTH_SHORT).show();
    			}
    		});

	    } catch (IOException e) {
    		mActivity.runOnUiThread(new Runnable() {
    			public void run() {
    				Toast.makeText(mActivity, "IOException when trying to create test.bin", Toast.LENGTH_SHORT).show();
    			}
    		});
//	        e.printStackTrace();
	    } catch (Exception e){
    		mActivity.runOnUiThread(new Runnable() {
    			public void run() {
    				Toast.makeText(mActivity, "Exception when trying to create test.bin", Toast.LENGTH_SHORT).show();
    			}
    		});
//	        e.printStackTrace();
	    }						
		
	}
}

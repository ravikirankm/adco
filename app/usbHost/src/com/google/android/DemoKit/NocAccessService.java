package com.google.android.DemoKit;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.widget.Toast;

public class NocAccessService extends InternetAccessService {
	
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
	
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        NocAccessService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NocAccessService.this;
        }
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public boolean uploadSensorData(String url) {
		
		// check for internet connection
		if(isConnected) {
			
			HttpClient httpclient = new DefaultHttpClient();

			// Prepare a request object
			HttpGet httpget = new HttpGet(url); 

			// Execute the request
			HttpResponse response;
			try {
				Toast.makeText(getApplicationContext(), "Sending Req: " + url, Toast.LENGTH_SHORT).show();
				
				response = httpclient.execute(httpget);
				// Examine the response status
				Toast.makeText(getApplicationContext(), "Got Response: " + response.getStatusLine().toString(), Toast.LENGTH_SHORT).show();

				// Get hold of the response entity
				HttpEntity entity = response.getEntity();
				// If the response does not enclose an entity, there is no need
				// to worry about connection release

				if (entity != null) {

					// A Simple JSON Response Read
					InputStream instream = entity.getContent();
					String result = convertStreamToString(instream);
					Toast.makeText(getApplicationContext(), "Got Result: " + result, Toast.LENGTH_SHORT).show();
					// now you have the string representation of the HTML request
					instream.close();
				}


			} catch (Exception e) {}
			
		} else {
			Toast.makeText(getApplicationContext(), "isConnected is false", Toast.LENGTH_SHORT).show();
			return false;
		}

		return false;

	}
	
	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}	
	
	public void downloadSettings() {
	}
	
	

}

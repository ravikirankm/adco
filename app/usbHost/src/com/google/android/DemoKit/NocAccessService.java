package com.google.android.DemoKit;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;

public class NocAccessService extends InternetAccessService {
	
    public void doOnIncomingMsg(Message msg) {
    	switch (msg.what) {
    		case (NocMessages.URL_MSG): 
    			String url = msg.getData().getString("url");
    			uploadSensorData(url);
    			break;
    		default:  
    			super.doOnIncomingMsg(msg);
    			break;
    	}
    }
	
	public boolean uploadSensorData(String url) {
		
		// check for internet connection
//		if(isConnected) {
//		HttpResponse response = null;
			
		// Create an Async Task
		AsyncTask<String, Void, Boolean> urlRequestTask = new AsyncTask<String, Void, Boolean> () {
			protected Boolean doInBackground(String... urls) {
				int count = urls.length;
				for(int i = 0; i< count; i++) {
					try {
						if(urls[i] != null) {
							sendClientDebugMsg("Getting default HttpClient... ");
							HttpClient httpclient = new DefaultHttpClient();
							// Prepare a request object
							sendClientDebugMsg("Getting HttpGet... ");
							HttpGet httpget = new HttpGet(urls[i]); 
							sendClientDebugMsg("Sending Req: " + urls[i]);
							HttpResponse response = httpclient.execute(httpget);

							if(response != null) {
								sendClientDebugMsg("Got Response: " + response.getStatusLine().toString());
							} else {
								sendClientDebugMsg("Got Null Response for HTTP request");
								return false;
							}
							
							// Execute the request
							try {
								// Get hold of the response entity
								HttpEntity entity = response.getEntity();
									// If the response does not enclose an entity, there is no need
									// to worry about connection release

									if (entity != null) {

										// A Simple JSON Response Read
										InputStream instream = entity.getContent();
										String result = convertStreamToString(instream);
										sendClientDebugMsg("Got Result: " + result);
										// now you have the string representation of the HTML request
										instream.close();
										return true;
									}
							} catch (Exception e) {
								sendClientDebugMsg("Got Exception: " + e);
							}
						}
					}
					catch (IOException e) {
						sendClientDebugMsg("Got IO Exception when trying to Http to NOC");
					}
				}
				return false;
			}
		};
		
		String [] urls = new String[1];
		urls[0] = url;
		
		urlRequestTask.execute(urls);
		
/*		try {
			return urlRequestTask.get(10, TimeUnit.SECONDS);
			// Examine the response status
		} 
		catch (Exception e){
			sendClientDebugMsg("Got Timeout when trying to do an Http request");
		}
*/		return false;

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

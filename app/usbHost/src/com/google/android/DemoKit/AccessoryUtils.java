package com.google.android.DemoKit;

import java.util.Set;

import android.os.Bundle;
import android.os.Parcel;

public class AccessoryUtils {
	
	DemoKitActivity mActivity;
	
	public AccessoryUtils(DemoKitActivity activity) {
		mActivity = activity;
	}
	
	public String getUpdateUrlPrefix() {
		
		String urlPrefix = "http://" + 
							mActivity.getNocUrl() +
							"/update.php?req=UI:" +
							mActivity.getSiteId();
		return urlPrefix;
	}
	
	public String getUrl(Parcel req) {
		
		String URL = "";
		
		Bundle map = req.readBundle();
		Set<String> mapKeySet = map.keySet();
		
		// Apply the Site ID for this site
		URL = URL + getUpdateUrlPrefix();
		
		for(String key: mapKeySet) {
			String val = map.getString(key);
			URL = URL + key + ":" + val + ";";
			
		}
		return URL;
	}

}

package com.google.android.DemoKit;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

public class DemoKitLaunch extends Activity {
	static final String TAG = "DemoKitLaunch";

	static Intent createIntent(Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		int maxExtent = Math.max(display.getWidth(), display.getHeight());

		Intent intent;
		if (maxExtent > 1200) {
			Log.i(TAG, "starting tablet ui");
			intent = new Intent(activity, DemoKitTablet.class);
		} else {
			Log.i(TAG, "starting phone ui");
			intent = new Intent(activity, DemoKitPhone.class);
		}
		return intent;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = createIntent(this);
		
		Toast.makeText(this, "inside onCreate of DemoKitLaunch", Toast.LENGTH_SHORT).show();

/*		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
*/
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);		
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "unable to start DemoKit activity", e);
		}
		finish();
	}
	
	public void onResume() {
		super.onResume();
		
		Toast.makeText(this, "inside onResume of DemoKitLaunch", Toast.LENGTH_SHORT).show();
	}	
	
}

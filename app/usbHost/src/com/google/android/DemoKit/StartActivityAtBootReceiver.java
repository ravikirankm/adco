package com.google.android.DemoKit;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class StartActivityAtBootReceiver extends BroadcastReceiver {

	@Override
    public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "Inside onReceive of StartActivityAtBootReceiver", Toast.LENGTH_SHORT).show();
		
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent activityIntent = new Intent(context, DemoKitLaunch.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

			try {
				context.startActivity(activityIntent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(context, "Could not Find activity" + e, Toast.LENGTH_SHORT).show();
			}
        }
    }	
}

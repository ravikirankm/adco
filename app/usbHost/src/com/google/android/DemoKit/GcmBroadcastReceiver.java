package com.google.android.DemoKit;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class GcmBroadcastReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

    	Toast.makeText(context, "inside onReceive of GcmBroadcastReceiver", Toast.LENGTH_SHORT).show();
    	
    	// Explicitly specify that GcmIntentService will handle the intent.
/*        ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
*/
    }

}

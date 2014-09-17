package com.google.android.DemoKit;

import android.os.Handler;
import android.os.Looper;

public class PeriodicScheduler {

    // Create a Handler that uses the Main Looper to run in
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mStatusChecker;
    private int UPDATE_INTERVAL = 2000;

    /**
     * Creates an UIUpdater object, that can be used to
     * perform UIUpdates on a specified time interval.
     * 
     * @param uiUpdater A runnable containing the update routine.
     */
    public PeriodicScheduler(final Runnable task) {
        mStatusChecker = new Runnable() {
            @Override
            public void run() {

            	if(task != null) {

            		Thread taskThread = new Thread(task);
            		
            		// Start the runnable in a separate thread
                    taskThread.start();
                    
                    // Re-run it after the update interval
                    mHandler.postDelayed(this, UPDATE_INTERVAL);
            	}
            	
            }
        };
    }

    /**
     * The same as the default constructor, but specifying the
     * intended update interval.
     * 
     * @param uiUpdater A runnable containing the update routine.
     * @param interval  The interval over which the routine
     *                  should run (milliseconds).
     */
    public PeriodicScheduler(Runnable task, int interval){
        this(task);
        UPDATE_INTERVAL = interval;
    }

    /**
     * Starts the periodical update routine (mStatusChecker 
     * adds the callback to the handler).
     */
    public synchronized void startUpdates(){
        mStatusChecker.run();
    }

    /**
     * Stops the periodical update routine from running,
     * by removing the callback.
     */
    public synchronized void stopUpdates(){
        mHandler.removeCallbacks(mStatusChecker);
    }	
}

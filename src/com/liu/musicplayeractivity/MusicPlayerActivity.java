package com.liu.musicplayeractivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MusicPlayerActivity extends Activity {
	private static final String TAG = "MusicPlayerActivity";
	private static final boolean DEBUG = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG){
    		Log.d(TAG, "onCreate()");
    	}
		
		super.onCreate(savedInstanceState);
		
		int activeTab = MusicUtils.getIntPref(this, "activetab", R.id.artisttab);
        if (activeTab != R.id.artisttab
                && activeTab != R.id.albumtab
                && activeTab != R.id.songtab
                && activeTab != R.id.playlisttab) {
            activeTab = R.id.artisttab;
        }
        MusicUtils.activateTab(this, activeTab);
	}
}

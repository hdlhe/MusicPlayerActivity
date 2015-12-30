package com.liu.musicplayeractivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class MusicUtils {
	private static final String TAG = "MusicUtils";
	private static final boolean DEBUG = true;
	
	static int getIntPref(Context context, String name, int def) {
		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		return prefs.getInt(name, def);
	}
	
	static void setIntPref(Context context, String name, int value) {
		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putInt(name, value);
		SharedPreferencesCompat.apply(ed);
	}
	
	static void activateTab(Activity a, int id) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		switch (id) {
		case R.id.artisttab:
			if(DEBUG)Log.d(TAG, "activateTab->" + "artisttab");
//			intent.setDataAndType(Uri.EMPTY,
//					"vnd.android.cursor.dir/artistalbum");
			break;
		case R.id.albumtab:
			if(DEBUG)Log.d(TAG, "activateTab->" + "albumtab");
//			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
			break;
		case R.id.songtab:
			if(DEBUG)Log.d(TAG, "activateTab->" + "songtab");
//			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			break;
		case R.id.playlisttab:
			if(DEBUG)Log.d(TAG, "activateTab->" + "playlisttab");
//			intent.setDataAndType(Uri.EMPTY,
//					MediaStore.Audio.Playlists.CONTENT_TYPE);
			break;
//		case R.id.nowplayingtab:
//			if(DEBUG)Log.d(TAG, "activateTab->" + "nowplayingtab");
//			intent = new Intent(a, MediaPlaybackActivity.class);
//			a.startActivity(intent);
			// fall through and return
		default:
			return;
		}
		intent.putExtra("withtabs", true);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		a.startActivity(intent);
		a.finish();
		a.overridePendingTransition(0, 0);
	}
}

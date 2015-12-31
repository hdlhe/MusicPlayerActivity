package com.liu.musicplayeractivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TabWidget;

public class MusicUtils {
	private static final String TAG = "MusicUtils";
	private static final boolean DEBUG = true;
	
	public static class ServiceToken {
		ContextWrapper mWrappedContext;

		ServiceToken(ContextWrapper context) {
			mWrappedContext = context;
		}
	}
	
	public static ServiceToken bindToService(Activity context,
			ServiceConnection callback){
		if(DEBUG){
			Log.i(TAG, "bindToService");
		}
		
		Activity realActivity = context.getParent();
		if (realActivity == null) {
			realActivity = context;
		}
		ContextWrapper cw = new ContextWrapper(realActivity);
//		cw.startService(new Intent(cw, MediaPlaybackService.class));
//		ServiceBinder sb = new ServiceBinder(callback);
//		if (cw.bindService(
//				(new Intent()).setClass(cw, MediaPlaybackService.class), sb, 0)) {
//			sConnectionMap.put(cw, sb);
//			return new ServiceToken(cw);
//		}
		Log.e("Music", "Failed to bind to service");
		return null;
	}
	
	public static Cursor query(Context context, Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder,
			int limit) {
		try {
			ContentResolver resolver = context.getContentResolver();
			if (resolver == null) {
				return null;
			}
			if (limit > 0) {
				uri = uri.buildUpon().appendQueryParameter("limit", "" + limit)
						.build();
			}
			return resolver.query(uri, projection, selection, selectionArgs,
					sortOrder);
		} catch (UnsupportedOperationException ex) {
			return null;
		}

	}

	public static Cursor query(Context context, Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		return query(context, uri, projection, selection, selectionArgs,
				sortOrder, 0);
	}
	
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
	
	static int sActiveTabIndex = -1;
	
	static boolean updateButtonBar(Activity a, int highlight) {
		final TabWidget ll = (TabWidget) a.findViewById(R.id.buttonbar);
		boolean withtabs = false;
		Intent intent = a.getIntent();
		if (intent != null) {
			withtabs = intent.getBooleanExtra("withtabs", false);
		}
		if(DEBUG){
			Log.d(TAG, "withtabs:" + withtabs + ", highlight:" + highlight);
		}
		if (highlight == 0 || !withtabs) {
			ll.setVisibility(View.GONE);
			return withtabs;
		} else if (withtabs) {
			ll.setVisibility(View.VISIBLE);
		}
		if(DEBUG){
			Log.d(TAG, "ll.getChildCount():" + ll.getChildCount());
		}
		for (int i = ll.getChildCount() - 1; i >= 0; i--) {

			View v = ll.getChildAt(i);
			boolean isActive = (v.getId() == highlight);
			if (isActive) {
				ll.setCurrentTab(i);
				sActiveTabIndex = i;
			}
			v.setTag(i);
			v.setOnFocusChangeListener(new View.OnFocusChangeListener() {

				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						for (int i = 0; i < ll.getTabCount(); i++) {
							if (ll.getChildTabViewAt(i) == v) {
								ll.setCurrentTab(i);
//								processTabClick((Activity) ll.getContext(), v,
//										ll.getChildAt(sActiveTabIndex).getId());
								break;
							}
						}
					}
				}
			});

			v.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
//					processTabClick((Activity) ll.getContext(), v, ll
//							.getChildAt(sActiveTabIndex).getId());
				}
			});
		}
		return withtabs;
	}
	
	static void activateTab(Activity a, int id) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		switch (id) {
		case R.id.artisttab:
			if(DEBUG)Log.d(TAG, "activateTab->" + "artisttab");
			intent.setDataAndType(Uri.EMPTY,
					"vnd.android.cursor.dir/liu/artistalbum");
			break;
		case R.id.albumtab:
			if(DEBUG)Log.d(TAG, "activateTab->" + "albumtab");
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/liu/album");
			break;
		case R.id.songtab:
			if(DEBUG)Log.d(TAG, "activateTab->" + "songtab");
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/liu/track");
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

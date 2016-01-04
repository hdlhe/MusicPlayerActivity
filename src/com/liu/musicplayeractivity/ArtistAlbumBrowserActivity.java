package com.liu.musicplayeractivity;

import android.app.ExpandableListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

import com.liu.musicplayeractivity.MusicUtils.ServiceToken;

public class ArtistAlbumBrowserActivity extends ExpandableListActivity {
	private static final String TAG = "ArtistAlbumBrowserActivity";
	private static final boolean DEBUG = true;

	private ArtistAlbumListAdapter mAdapter;
	private ServiceToken mToken;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (DEBUG) {
			Log.d(TAG, "onCreate" + ", savedInstanceState = "
					+ savedInstanceState);
		}

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

//		mToken = MusicUtils.bindToService(this, this);

		setContentView(R.layout.media_picker_activity_expanding);
		MusicUtils.updateButtonBar(this, R.id.artisttab);

		mAdapter = new ArtistAlbumListAdapter(getApplication(), this,
				null, // cursor
				R.layout.track_list_item_group, new String[] {}, new int[] {},
				R.layout.track_list_item_child, new String[] {}, new int[] {});
		setListAdapter(mAdapter);
		setTitle(R.string.working_artists);
		getArtistCursor(mAdapter.getQueryHandler(), null);
	}

	private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {

		String[] cols = new String[] { MediaStore.Audio.Artists._ID,
				MediaStore.Audio.Artists.ARTIST,
				MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
				MediaStore.Audio.Artists.NUMBER_OF_TRACKS };

		Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
		if (!TextUtils.isEmpty(filter)) {
			uri = uri.buildUpon()
					.appendQueryParameter("filter", Uri.encode(filter)).build();
		}

		Cursor ret = null;
		if (async != null) {
			async.startQuery(0, null, uri, cols, null, null,
					MediaStore.Audio.Artists.ARTIST_KEY);
		} else {
			ret = MusicUtils.query(this, uri, cols, null, null,
					MediaStore.Audio.Artists.ARTIST_KEY);
		}
		return ret;
	}

	public void init(Cursor c) {

		if (mAdapter == null) {
			return;
		}
		mAdapter.changeCursor(c); // also sets mArtistCursor

		// if (mArtistCursor == null) {
		// MusicUtils.displayDatabaseError(this);
		// closeContextMenu();
		// mReScanHandler.sendEmptyMessageDelayed(0, 1000);
		// return;
		// }
		//
		// // restore previous position
		// if (mLastListPosCourse >= 0) {
		// ExpandableListView elv = getExpandableListView();
		// elv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
		// mLastListPosCourse = -1;
		// }
		//
		// MusicUtils.hideDatabaseError(this);
		// MusicUtils.updateButtonBar(this, R.id.artisttab);
		// setTitle();
	}

	static class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter {

		private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultAlbumIcon;
        private int mGroupArtistIdIdx;
        private int mGroupArtistIdx;
        private int mGroupAlbumIdx;
        private int mGroupSongIdx;
        private final Context mContext;
        private final Resources mResources;
        private final String mAlbumSongSeparator;
        private final String mUnknownAlbum;
        private final String mUnknownArtist;
        private final StringBuilder mBuffer = new StringBuilder();
        private final Object[] mFormatArgs = new Object[1];
        private final Object[] mFormatArgs3 = new Object[3];
//        private MusicAlphabetIndexer mIndexer;
        private ArtistAlbumBrowserActivity mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;

		static class ViewHolder {
			TextView line1;
			TextView line2;
			ImageView play_indicator;
			ImageView icon;
		}

		class QueryHandler extends AsyncQueryHandler {

			public QueryHandler(ContentResolver cr) {
				super(cr);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				super.onQueryComplete(token, cookie, cursor);
				mActivity.init(cursor);
			}
		}

		@SuppressWarnings("deprecation")
		public ArtistAlbumListAdapter(Context context,
				ArtistAlbumBrowserActivity currentactivity, Cursor cursor,
				int groupLayout, String[] groupFrom, int[] groupTo,
				int childLayout, String[] childFrom, int[] childTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo,
					childLayout, childFrom, childTo);
			mActivity = currentactivity;
			mQueryHandler = new QueryHandler(context.getContentResolver());
			Resources r = context.getResources();
			mNowPlayingOverlay = r
					.getDrawable(R.drawable.indicator_ic_mp_playing_list);
			mDefaultAlbumIcon = (BitmapDrawable) r
					.getDrawable(R.drawable.albumart_mp_unknown_list);
			// no filter or dither, it's a lot faster and we can't tell the
			// difference
			mDefaultAlbumIcon.setFilterBitmap(false);
			mDefaultAlbumIcon.setDither(false);
			mContext = context;
			getColumnIndices(cursor);
			mResources = context.getResources();
			mAlbumSongSeparator = context
					.getString(R.string.albumsongseparator);
			mUnknownAlbum = context.getString(R.string.unknown_album_name);
			mUnknownArtist = context.getString(R.string.unknown_artist_name);
		}

		private void getColumnIndices(Cursor cursor) {
			if (cursor != null) {
				mGroupArtistIdIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
				mGroupArtistIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
				mGroupAlbumIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
				mGroupSongIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
				// if (mIndexer != null) {
				// mIndexer.setCursor(cursor);
				// } else {
				// mIndexer = new MusicAlphabetIndexer(cursor,
				// mGroupArtistIdx,
				// mResources.getString(R.string.fast_scroll_alphabet));
				// }
			}
		}

		public void setActivity(ArtistAlbumBrowserActivity newactivity) {
			mActivity = newactivity;
		}

		public AsyncQueryHandler getQueryHandler() {
			return mQueryHandler;
		}

		@Override
		public View newGroupView(Context context, Cursor cursor,
				boolean isExpanded, ViewGroup parent) {
			View v = super.newGroupView(context, cursor, isExpanded, parent);
			if (DEBUG) {
				// Log.d(TAG, "newGroupView" + ", v:" + v);
			}
			ImageView iv = (ImageView) v.findViewById(R.id.icon);
			ViewGroup.LayoutParams p = iv.getLayoutParams();
			p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
			p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.icon.setPadding(0, 0, 1, 0);
			v.setTag(vh);
			return v;
		}

		@Override
		public View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			View v = super.newChildView(context, cursor, isLastChild, parent);
			if (DEBUG) {
				// Log.d(TAG, "newChildView" + ", v:" + v);
			}
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
			vh.icon.setPadding(0, 0, 1, 0);
			v.setTag(vh);
			return v;
		}

		@Override
		public void bindGroupView(View view, Context context, Cursor cursor,
				boolean isexpanded) {
			ViewHolder vh = (ViewHolder) view.getTag();
			if (DEBUG) {
				// Log.d(TAG, "bindGroupView" + ", view:" + view);
			}
			String artist = cursor.getString(mGroupArtistIdx);
			String displayartist = artist;
			boolean unknown = artist == null
					|| artist.equals(MediaStore.UNKNOWN_STRING);
			if (unknown) {
				displayartist = mUnknownArtist;
			}
			vh.line1.setText(displayartist);

			int numalbums = cursor.getInt(mGroupAlbumIdx);
			int numsongs = cursor.getInt(mGroupSongIdx);

			 String songs_albums = MusicUtils.makeAlbumsLabel(context,
			 numalbums, numsongs, unknown);
			
			 vh.line2.setText(songs_albums);
			//
			// long currentartistid = MusicUtils.getCurrentArtistId();
			// long artistid = cursor.getLong(mGroupArtistIdIdx);
			// if (currentartistid == artistid && !isexpanded) {
			// vh.play_indicator.setImageDrawable(mNowPlayingOverlay);
			// } else {
			// vh.play_indicator.setImageDrawable(null);
			// }
		}

		@SuppressWarnings("deprecation")
		@Override
		public void bindChildView(View view, Context context, Cursor cursor,
				boolean islast) {

			ViewHolder vh = (ViewHolder) view.getTag();

			if (DEBUG) {
				// Log.d(TAG, "bindChildView" + ", view:" + view + ", cursor:" +
				// cursor);
			}
			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
			String displayname = name;
			boolean unknown = name == null
					|| name.equals(MediaStore.UNKNOWN_STRING);
			if (unknown) {
				displayname = mUnknownAlbum;
			}
			vh.line1.setText(displayname);

			int numsongs = cursor
					.getInt(cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
			int numartistsongs = cursor
					.getInt(cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST));

			final StringBuilder builder = mBuffer;
			builder.delete(0, builder.length());
			if (unknown) {
				numsongs = numartistsongs;
			}

			if (numsongs == 1) {
				builder.append(context.getString(R.string.onesong));
			} else {
				if (numsongs == numartistsongs) {
					final Object[] args = mFormatArgs;
					args[0] = numsongs;
					builder.append(mResources.getQuantityString(
							R.plurals.Nsongs, numsongs, args));
				} else {
					final Object[] args = mFormatArgs3;
					args[0] = numsongs;
					args[1] = numartistsongs;
					args[2] = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
					builder.append(mResources.getQuantityString(
							R.plurals.Nsongscomp, numsongs, args));
				}
			}
			vh.line2.setText(builder.toString());

			ImageView iv = vh.icon;
			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			String art = cursor.getString(cursor
					.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART));
			if (unknown || art == null || art.length() == 0) {
				iv.setBackgroundDrawable(mDefaultAlbumIcon);
				iv.setImageDrawable(null);
			} else {
				long artIndex = cursor.getLong(0);
				Drawable d = MusicUtils.getCachedArtwork(context, artIndex,
						mDefaultAlbumIcon);
				iv.setImageDrawable(d);
			}

//			long currentalbumid = MusicUtils.getCurrentAlbumId();
//			long aid = cursor.getLong(0);
//			iv = vh.play_indicator;
//			if (currentalbumid == aid) {
//				iv.setImageDrawable(mNowPlayingOverlay);
//			} else {
//				iv.setImageDrawable(null);
//			}
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {

			long id = groupCursor.getLong(groupCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));

			String[] cols = new String[] { MediaStore.Audio.Albums._ID,
					MediaStore.Audio.Albums.ALBUM,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
					MediaStore.Audio.Albums.ALBUM_ART };
			Cursor c = MusicUtils.query(mActivity,
					MediaStore.Audio.Artists.Albums.getContentUri("external",
							id), cols, null, null,
					MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

			class MyCursorWrapper extends CursorWrapper {
				String mArtistName;
				int mMagicColumnIdx;

				MyCursorWrapper(Cursor c, String artist) {
					super(c);
					mArtistName = artist;
					if (mArtistName == null
							|| mArtistName.equals(MediaStore.UNKNOWN_STRING)) {
						mArtistName = mUnknownArtist;
					}
					mMagicColumnIdx = c.getColumnCount();
				}

				@Override
				public String getString(int columnIndex) {
					if (columnIndex != mMagicColumnIdx) {
						return super.getString(columnIndex);
					}
					return mArtistName;
				}

				@Override
				public int getColumnIndexOrThrow(String name) {
					if (MediaStore.Audio.Albums.ARTIST.equals(name)) {
						return mMagicColumnIdx;
					}
					return super.getColumnIndexOrThrow(name);
				}

				@Override
				public String getColumnName(int idx) {
					if (idx != mMagicColumnIdx) {
						return super.getColumnName(idx);
					}
					return MediaStore.Audio.Albums.ARTIST;
				}

				@Override
				public int getColumnCount() {
					return super.getColumnCount() + 1;
				}
			}
			MyCursorWrapper wrapper = new MyCursorWrapper(c,
					groupCursor.getString(mGroupArtistIdx));

			return wrapper;
		}

		@Override
		public void changeCursor(Cursor cursor) {
			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mArtistCursor) {
				mActivity.mArtistCursor = cursor;
				getColumnIndices(cursor);
				super.changeCursor(cursor);
			}
		}
	}
	
	private Cursor mArtistCursor;
}

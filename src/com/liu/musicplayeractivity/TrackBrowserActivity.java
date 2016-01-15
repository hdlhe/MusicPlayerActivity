package com.liu.musicplayeractivity;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Playlists;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.liu.musicplayeractivity.MusicUtils.ServiceToken;

public class TrackBrowserActivity extends ListActivity implements
		ServiceConnection {
	private static final String TAG = "TrackBrowserActivity";
	private static final boolean DEBUG = true;

	// private static final int Q_SELECTED = CHILD_MENU_BASE;
	// private static final int Q_ALL = CHILD_MENU_BASE + 1;
	// private static final int SAVE_AS_PLAYLIST = CHILD_MENU_BASE + 2;
	// private static final int PLAY_ALL = CHILD_MENU_BASE + 3;
	// private static final int CLEAR_PLAYLIST = CHILD_MENU_BASE + 4;
	// private static final int REMOVE = CHILD_MENU_BASE + 5;
	// private static final int SEARCH = CHILD_MENU_BASE + 6;

	private static final String LOGTAG = "TrackBrowser";

	private String[] mCursorCols;
	private String[] mPlaylistMemberCols;
	private boolean mDeletedOneRow = false;
	private boolean mEditMode = false;
	private String mCurrentTrackName;
	private String mCurrentAlbumName;
	private String mCurrentArtistNameForAlbum;
	private ListView mTrackList;
	private Cursor mTrackCursor;
	private TrackListAdapter mAdapter;
	private boolean mAdapterSent = false;
	private String mAlbumId;
	private String mArtistId;
	private String mPlaylist;
	private String mGenre;
	private String mSortOrder;
	private int mSelectedPosition;
	private long mSelectedId;
	private static int mLastListPosCourse = -1;
	private static int mLastListPosFine = -1;
	private boolean mUseLastListPos = false;
	private ServiceToken mToken;

	@Override
	protected void onCreate(Bundle icicle) {
		if (DEBUG) {
			Log.i(TAG, "onCreate");
		}
		super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.getBooleanExtra("withtabs", false)) {
				requestWindowFeature(Window.FEATURE_NO_TITLE);
			}
		}
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if (icicle != null) {
			mSelectedId = icicle.getLong("selectedtrack");
			mAlbumId = icicle.getString("album");
			mArtistId = icicle.getString("artist");
			mPlaylist = icicle.getString("playlist");
			mGenre = icicle.getString("genre");
			mEditMode = icicle.getBoolean("editmode", false);
		} else {
			mAlbumId = intent.getStringExtra("album");
			// If we have an album, show everything on the album, not just stuff
			// by a particular artist.
			mArtistId = intent.getStringExtra("artist");
			mPlaylist = intent.getStringExtra("playlist");
			mGenre = intent.getStringExtra("genre");
			mEditMode = intent.getAction().equals(Intent.ACTION_EDIT);
		}

		mCursorCols = new String[] { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ARTIST_ID,
				MediaStore.Audio.Media.DURATION };
		mPlaylistMemberCols = new String[] {
				MediaStore.Audio.Playlists.Members._ID,
				MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ARTIST_ID,
				MediaStore.Audio.Media.DURATION,
				MediaStore.Audio.Playlists.Members.PLAY_ORDER,
				MediaStore.Audio.Playlists.Members.AUDIO_ID,
				MediaStore.Audio.Media.IS_MUSIC };

		setContentView(R.layout.media_picker_activity);
		mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
		mTrackList = getListView();
		mTrackList.setOnCreateContextMenuListener(this);
		mTrackList.setCacheColorHint(0);
		if (mEditMode) {
			// ((TouchInterceptor) mTrackList).setDropListener(mDropListener);
			// ((TouchInterceptor)
			// mTrackList).setRemoveListener(mRemoveListener);
			mTrackList.setDivider(null);
			// mTrackList.setSelector(R.drawable.list_selector_background);
		} else {
			mTrackList.setTextFilterEnabled(true);
		}
		mAdapter = (TrackListAdapter) getLastNonConfigurationInstance();

		if (mAdapter != null) {
			mAdapter.setActivity(this);
			setListAdapter(mAdapter);
		}
		mToken = MusicUtils.bindToService(this, this);

		// don't set the album art until after the view has been layed out
		mTrackList.post(new Runnable() {

			public void run() {
				 setAlbumArtBackground();
			}
		});
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		if (DEBUG) {
			Log.d(TAG, "onServiceConnected");
		}
		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		// registerReceiver(mScanListener, f);

		if (mAdapter == null) {
			// Log.i("@@@", "starting query");
			mAdapter = new TrackListAdapter(
					getApplication(), // need to use application context to avoid leaks
					this,
					mEditMode ? R.layout.edit_track_list_item
							: R.layout.track_list_item,
					null, // cursor
					new String[] {}, new int[] {},
					"nowplaying".equals(mPlaylist),
					mPlaylist != null
							&& !(mPlaylist.equals("podcasts") || mPlaylist
									.equals("recentlyadded")));
			setListAdapter(mAdapter);
			setTitle(R.string.working_songs);
			getTrackCursor(mAdapter.getQueryHandler(), null, true);
		} else {
			mTrackCursor = mAdapter.getCursor();
			// If mTrackCursor is null, this can be because it doesn't have
			// a cursor yet (because the initial query that sets its cursor
			// is still in progress), or because the query failed.
			// In order to not flash the error dialog at the user for the
			// first case, simply retry the query when the cursor is null.
			// Worst case, we end up doing the same query twice.
			if (mTrackCursor != null) {
				// init(mTrackCursor, false);
			} else {
				setTitle(R.string.working_songs);
				getTrackCursor(mAdapter.getQueryHandler(), null, true);
			}
		}
		if (!mEditMode) {
			// MusicUtils.updateNowPlaying(this);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

	}

	static class TrackListAdapter extends SimpleCursorAdapter {
		boolean mIsNowPlaying;
		boolean mDisableNowPlayingIndicator;

		int mTitleIdx;
		int mArtistIdx;
		int mDurationIdx;
		int mAudioIdIdx;

		private final StringBuilder mBuilder = new StringBuilder();
		private final String mUnknownArtist;
		private final String mUnknownAlbum;

		private AlphabetIndexer mIndexer;

		private TrackBrowserActivity mActivity = null;
		private TrackQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;

		static class ViewHolder {
			TextView line1;
			TextView line2;
			TextView duration;
			ImageView play_indicator;
			CharArrayBuffer buffer1;
			char[] buffer2;
		}

		class TrackQueryHandler extends AsyncQueryHandler {
			class QueryArgs {
				public Uri uri;
				public String[] projection;
				public String selection;
				public String[] selectionArgs;
				public String orderBy;
			}

			public TrackQueryHandler(ContentResolver cr) {
				super(cr);
			}

			public Cursor doQuery(Uri uri, String[] projection,
					String selection, String[] selectionArgs, String orderBy,
					boolean async) {
				if (DEBUG)
					Log.i(TAG, "doQuery" + async);
				if (async) {
					// Get 100 results first, which is enough to allow the user
					// to start scrolling,
					// while still being very fast.
					Uri limituri = uri.buildUpon()
							.appendQueryParameter("limit", "100").build();
					QueryArgs args = new QueryArgs();
					args.uri = uri;
					args.projection = projection;
					args.selection = selection;
					args.selectionArgs = selectionArgs;
					args.orderBy = orderBy;

					startQuery(0, args, limituri, projection, selection,
							selectionArgs, orderBy);
					return null;
				}
				return MusicUtils.query(mActivity, uri, projection, selection,
						selectionArgs, orderBy);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				if (DEBUG)
					Log.i(TAG, "query complete: " + cursor.getCount() + "   "
							+ mActivity);
				mActivity.init(cursor, cookie != null);
				if (token == 0 && cookie != null && cursor != null
						&& !cursor.isClosed() && cursor.getCount() >= 100) {
					QueryArgs args = (QueryArgs) cookie;
					startQuery(1, null, args.uri, args.projection,
							args.selection, args.selectionArgs, args.orderBy);
				}
			}
		}

		@SuppressWarnings("deprecation")
		public TrackListAdapter(Context context,
				TrackBrowserActivity currentactivity, int layout,
				Cursor cursor, String[] from, int[] to, boolean isnowplaying,
				boolean disablenowplayingindicator) {
			super(context, layout, cursor, from, to);
			mActivity = currentactivity;
			getColumnIndices(cursor);
			mIsNowPlaying = isnowplaying;
			mDisableNowPlayingIndicator = disablenowplayingindicator;
			mUnknownArtist = context.getString(R.string.unknown_artist_name);
			mUnknownAlbum = context.getString(R.string.unknown_album_name);

			mQueryHandler = new TrackQueryHandler(context.getContentResolver());
		}

		public void setActivity(TrackBrowserActivity newactivity) {
			mActivity = newactivity;
		}

		public TrackQueryHandler getQueryHandler() {
			return mQueryHandler;
		}

		private void getColumnIndices(Cursor cursor) {
			if (cursor != null) {
				mTitleIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
				mArtistIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
				mDurationIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
				try {
					mAudioIdIdx = cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
				} catch (IllegalArgumentException ex) {
					mAudioIdIdx = cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
				}

				if (mIndexer != null) {
					mIndexer.setCursor(cursor);
				} else if (!mActivity.mEditMode && mActivity.mAlbumId == null) {
					String alpha = mActivity
							.getString(R.string.fast_scroll_alphabet);

					// mIndexer = new MusicAlphabetIndexer(cursor, mTitleIdx,
					// alpha);
				}
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			ImageView iv = (ImageView) v.findViewById(R.id.icon);
			iv.setVisibility(View.GONE);

			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.duration = (TextView) v.findViewById(R.id.duration);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.buffer1 = new CharArrayBuffer(100);
			vh.buffer2 = new char[200];
			v.setTag(vh);
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder vh = (ViewHolder) view.getTag();

			cursor.copyStringToBuffer(mTitleIdx, vh.buffer1);
			vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);

			int secs = cursor.getInt(mDurationIdx) / 1000;
			if (secs == 0) {
				vh.duration.setText("");
			} else {
				 vh.duration.setText(MusicUtils.makeTimeString(context, secs));
			}

			final StringBuilder builder = mBuilder;
			builder.delete(0, builder.length());

			String name = cursor.getString(mArtistIdx);
			if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
				builder.append(mUnknownArtist);
			} else {
				builder.append(name);
			}
			int len = builder.length();
			if (vh.buffer2.length < len) {
				vh.buffer2 = new char[len];
			}
			builder.getChars(0, len, vh.buffer2, 0);
			vh.line2.setText(vh.buffer2, 0, len);

			ImageView iv = vh.play_indicator;
			long id = -1;
			if (MusicUtils.sService != null) {
				// TODO: IPC call on each bind??
				try {
					if (mIsNowPlaying) {
						id = MusicUtils.sService.getQueuePosition();
					} else {
						id = MusicUtils.sService.getAudioId();
					}
				} catch (RemoteException ex) {
				}
			}

			// Determining whether and where to show the "now playing indicator
			// is tricky, because we don't actually keep track of where the
			// songs
			// in the current playlist came from after they've started playing.
			//
			// If the "current playlists" is shown, then we can simply match by
			// position,
			// otherwise, we need to match by id. Match-by-id gets a little
			// weird if
			// a song appears in a playlist more than once, and you're in
			// edit-playlist
			// mode. In that case, both items will have the "now playing"
			// indicator.
			// For this reason, we don't show the play indicator at all when in
			// edit
			// playlist mode (except when you're viewing the "current playlist",
			// which is not really a playlist)
			if ((mIsNowPlaying && cursor.getPosition() == id)
					|| (!mIsNowPlaying && !mDisableNowPlayingIndicator && cursor
							.getLong(mAudioIdIdx) == id)) {
				iv.setImageResource(R.drawable.indicator_ic_mp_playing_list);
				iv.setVisibility(View.VISIBLE);
			} else {
				iv.setVisibility(View.GONE);
			}
		}
		
		@Override
		public void changeCursor(Cursor cursor) {
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != mActivity.mTrackCursor) {
                mActivity.mTrackCursor = cursor;
                super.changeCursor(cursor);
                getColumnIndices(cursor);
            }
        }
	}

	private Cursor getTrackCursor(
			TrackListAdapter.TrackQueryHandler queryhandler, String filter,
			boolean async) {

		if (queryhandler == null) {
			throw new IllegalArgumentException();
		}

		Cursor ret = null;
		mSortOrder = MediaStore.Audio.Media.TITLE_KEY;
		StringBuilder where = new StringBuilder();
		where.append(MediaStore.Audio.Media.TITLE + " != ''");

		if (mGenre != null) {
			Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external",
					Integer.valueOf(mGenre));
			if (!TextUtils.isEmpty(filter)) {
				uri = uri.buildUpon()
						.appendQueryParameter("filter", Uri.encode(filter))
						.build();
			}
			mSortOrder = MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER;
			ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
					null, mSortOrder, async);
		} else if (mPlaylist != null) {
			if (mPlaylist.equals("nowplaying")) {
				if (MusicUtils.sService != null) {
					// ret = new NowPlayingCursor(MusicUtils.sService,
					// mCursorCols);
					if (ret.getCount() == 0) {
						finish();
					}
				} else {
					// Nothing is playing.
				}
			} else if (mPlaylist.equals("podcasts")) {
				where.append(" AND " + MediaStore.Audio.Media.IS_PODCAST + "=1");
				Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				if (!TextUtils.isEmpty(filter)) {
					uri = uri.buildUpon()
							.appendQueryParameter("filter", Uri.encode(filter))
							.build();
				}
				ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
						null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER, async);
			} else if (mPlaylist.equals("recentlyadded")) {
				// do a query for all songs added in the last X weeks
				Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				if (!TextUtils.isEmpty(filter)) {
					uri = uri.buildUpon()
							.appendQueryParameter("filter", Uri.encode(filter))
							.build();
				}
				int X = MusicUtils.getIntPref(this, "numweeks", 2)
						* (3600 * 24 * 7);
				where.append(" AND " + MediaStore.MediaColumns.DATE_ADDED + ">");
				where.append(System.currentTimeMillis() / 1000 - X);
				ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
						null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER, async);
			} else {
				Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
						"external", Long.valueOf(mPlaylist));
				if (!TextUtils.isEmpty(filter)) {
					uri = uri.buildUpon()
							.appendQueryParameter("filter", Uri.encode(filter))
							.build();
				}
				mSortOrder = MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER;
				ret = queryhandler.doQuery(uri, mPlaylistMemberCols,
						where.toString(), null, mSortOrder, async);
			}
		} else {
			if (mAlbumId != null) {
				where.append(" AND " + MediaStore.Audio.Media.ALBUM_ID + "="
						+ mAlbumId);
				mSortOrder = MediaStore.Audio.Media.TRACK + ", " + mSortOrder;
			}
			if (mArtistId != null) {
				where.append(" AND " + MediaStore.Audio.Media.ARTIST_ID + "="
						+ mArtistId);
			}
			where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");
			Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			if (!TextUtils.isEmpty(filter)) {
				uri = uri.buildUpon()
						.appendQueryParameter("filter", Uri.encode(filter))
						.build();
			}
			ret = queryhandler.doQuery(uri, mCursorCols, where.toString(),
					null, mSortOrder, async);
		}

		// This special case is for the "nowplaying" cursor, which cannot be
		// handled
		// asynchronously using AsyncQueryHandler, so we do some extra
		// initialization here.
		if (ret != null && async) {
			init(ret, false);
			setTitle();
		}
		return ret;
	}

	public void init(Cursor newCursor, boolean isLimited) {

		if (mAdapter == null) {
			return;
		}
		mAdapter.changeCursor(newCursor); // also sets mTrackCursor

		if (mTrackCursor == null) {
			// MusicUtils.displayDatabaseError(this);
			// closeContextMenu();
			// mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		// MusicUtils.hideDatabaseError(this);
		 mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
		 setTitle();

		// Restore previous position
		if (mLastListPosCourse >= 0 && mUseLastListPos) {
			ListView lv = getListView();
			// this hack is needed because otherwise the position doesn't change
			// for the 2nd (non-limited) cursor
			lv.setAdapter(lv.getAdapter());
			lv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
			if (!isLimited) {
				mLastListPosCourse = -1;
			}
		}

		// When showing the queue, position the selection on the currently
		// playing track
		// Otherwise, position the selection on the first matching artist, if
		// any
		IntentFilter f = new IntentFilter();
		// f.addAction(MediaPlaybackService.META_CHANGED);
		// f.addAction(MediaPlaybackService.QUEUE_CHANGED);
		if ("nowplaying".equals(mPlaylist)) {
			try {
				int cur = MusicUtils.sService.getQueuePosition();
				setSelection(cur);
				// registerReceiver(mNowPlayingListener, new IntentFilter(f));
				// mNowPlayingListener.onReceive(this, new
				// Intent(MediaPlaybackService.META_CHANGED));
			} catch (RemoteException ex) {
			}
		} else {
			String key = getIntent().getStringExtra("artist");
			if (key != null) {
				int keyidx = mTrackCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
				mTrackCursor.moveToFirst();
				while (!mTrackCursor.isAfterLast()) {
					String artist = mTrackCursor.getString(keyidx);
					if (artist.equals(key)) {
						setSelection(mTrackCursor.getPosition());
						break;
					}
					mTrackCursor.moveToNext();
				}
			}
			// registerReceiver(mTrackListListener, new IntentFilter(f));
			// mTrackListListener.onReceive(this, new
			// Intent(MediaPlaybackService.META_CHANGED));
		}
	}
	
	private void setAlbumArtBackground() {
        if (!mEditMode) {
            try {
                long albumid = Long.valueOf(mAlbumId);
                Bitmap bm = MusicUtils.getArtwork(TrackBrowserActivity.this, -1, albumid, false);
                if (bm != null) {
                    MusicUtils.setBackground(mTrackList, bm);
                    mTrackList.setCacheColorHint(0);
                    return;
                }
            } catch (Exception ex) {
            }
        }
        mTrackList.setBackgroundColor(0xff000000);
        mTrackList.setCacheColorHint(0);
    }
	
	private void setTitle() {

        CharSequence fancyName = null;
        if (mAlbumId != null) {
            int numresults = mTrackCursor != null ? mTrackCursor.getCount() : 0;
            if (numresults > 0) {
                mTrackCursor.moveToFirst();
                int idx = mTrackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                fancyName = mTrackCursor.getString(idx);
                // For compilation albums show only the album title,
                // but for regular albums show "artist - album".
                // To determine whether something is a compilation
                // album, do a query for the artist + album of the
                // first item, and see if it returns the same number
                // of results as the album query.
                String where = MediaStore.Audio.Media.ALBUM_ID + "='" + mAlbumId +
                        "' AND " + MediaStore.Audio.Media.ARTIST_ID + "=" + 
                        mTrackCursor.getLong(mTrackCursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.ARTIST_ID));
                Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] {MediaStore.Audio.Media.ALBUM}, where, null, null);
                if (cursor != null) {
                    if (cursor.getCount() != numresults) {
                        // compilation album
                        fancyName = mTrackCursor.getString(idx);
                    }    
                    cursor.deactivate();
                }
                if (fancyName == null || fancyName.equals(MediaStore.UNKNOWN_STRING)) {
                    fancyName = getString(R.string.unknown_album_name);
                }
            }
        } else if (mPlaylist != null) {
            if (mPlaylist.equals("nowplaying")) {
//                if (MusicUtils.getCurrentShuffleMode() == MediaPlaybackService.SHUFFLE_AUTO) {
//                    fancyName = getText(R.string.partyshuffle_title);
//                } else {
                    fancyName = getText(R.string.nowplaying_title);
//                }
            } else if (mPlaylist.equals("podcasts")){
                fancyName = getText(R.string.podcasts_title);
            } else if (mPlaylist.equals("recentlyadded")){
                fancyName = getText(R.string.recentlyadded_title);
            } else {
                String [] cols = new String [] {
                MediaStore.Audio.Playlists.NAME
                };
                Cursor cursor = MusicUtils.query(this,
                        ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, Long.valueOf(mPlaylist)),
                        cols, null, null, null);
                if (cursor != null) {
                    if (cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        fancyName = cursor.getString(0);
                    }
                    cursor.deactivate();
                }
            }
        } else if (mGenre != null) {
            String [] cols = new String [] {
            MediaStore.Audio.Genres.NAME
            };
            Cursor cursor = MusicUtils.query(this,
                    ContentUris.withAppendedId(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, Long.valueOf(mGenre)),
                    cols, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    fancyName = cursor.getString(0);
                }
                cursor.deactivate();
            }
        }

        if (fancyName != null) {
            setTitle(fancyName);
        } else {
            setTitle(R.string.tracks_title);
        }
    }
}

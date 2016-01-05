package com.liu.musicplayeractivity;

import java.lang.ref.WeakReference;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class MediaPlaybackService extends Service {
	private static final String TAG = "MediaPlaybackService";
	private static final boolean DEBUG = true;	

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/*
	 * By making this a static class with a WeakReference to the Service, we
	 * ensure that the Service can be GCd even when the system process still has
	 * a remote reference to the stub.
	 */
	static class ServiceStub extends IMediaPlaybackService.Stub{
		WeakReference<MediaPlaybackService> mService;
		
		ServiceStub(MediaPlaybackService service) {
			mService = new WeakReference<MediaPlaybackService>(service);
		}
		
		public void openFile(String path) {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "openFile");
			}
//			mService.get().open(path);
		}

		public void open(long[] list, int position) {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "open");
			}
//			mService.get().open(list, position);
		}

		public int getQueuePosition() {
			return 0;
//			return mService.get().getQueuePosition();
		}

		public void setQueuePosition(int index) {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "openFile");
			}
//			mService.get().setQueuePosition(index);
		}

		public boolean isPlaying() {
			return false;
//			return mService.get().isPlaying();
		}

		public void stop() {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "stop");
			}
//			mService.get().stop();
		}

		public void pause() {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "pause");
			}
//			mService.get().pause();
		}

		public void play() {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "play");
			}
//			mService.get().play();
		}

		public void prev() {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "prev");
			}
//			mService.get().prev();
		}

		public void next() {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "next");
			}
//			mService.get().gotoNext(true);
		}

		public String getTrackName() {
			return null;
//			return mService.get().getTrackName();
		}

		public String getAlbumName() {
			return null;
//			return mService.get().getAlbumName();
		}

		public long getAlbumId() {
			return 0;
//			return mService.get().getAlbumId();
		}

		public String getArtistName() {
			return null;
//			return mService.get().getArtistName();
		}

		public long getArtistId() {
			return 0;
//			return mService.get().getArtistId();
		}

		public void enqueue(long[] list, int action) {
			if (DEBUG) {
				Log.i(TAG, "IMediaPlaybackService.Stub->" + "enqueue");
			}
//			mService.get().enqueue(list, action);
		}

		public long[] getQueue() {
			return null;
//			return mService.get().getQueue();
		}

		public void moveQueueItem(int from, int to) {
//			mService.get().moveQueueItem(from, to);
		}

		public String getPath() {
			return null;
//			return mService.get().getPath();
		}

		public long getAudioId() {
			return 0;
//			return mService.get().getAudioId();
		}

		public long position() {
			return 0;
//			return mService.get().position();
		}

		public long duration() {
			return 0;
//			return mService.get().duration();
		}

		public long seek(long pos) {
			return pos;
//			return mService.get().seek(pos);
		}

		public void setShuffleMode(int shufflemode) {
//			mService.get().setShuffleMode(shufflemode);
		}

		public int getShuffleMode() {
			return 0;
//			return mService.get().getShuffleMode();
		}

		public int removeTracks(int first, int last) {
			return last;
//			return mService.get().removeTracks(first, last);
		}

		public int removeTrack(long id) {
			return 0;
//			return mService.get().removeTrack(id);
		}

		public void setRepeatMode(int repeatmode) {
//			mService.get().setRepeatMode(repeatmode);
		}

		public int getRepeatMode() {
			return 0;
//			return mService.get().getRepeatMode();
		}

		public int getMediaMountedCount() {
			return 0;
//			return mService.get().getMediaMountedCount();
		}

		public int getAudioSessionId() {
			return 0;
//			return mService.get().getAudioSessionId();
		}
		
	}
}

/**
 * 
 */
package com.felix.rsslisten.rssfeeds;

import java.net.URI;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.felix.rsslisten.NewsDBAdapter;
import com.felix.rsslisten.RSSSpeaker;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

/**
 * @author Stefan
 * 
 */
public class CheckRssThread implements Runnable {

	private static final String TAG = "CheckRssThread";

	public static final int UPDATE_DEFAULT = 30;

	/**
	 * interval in minutes between between feed update, 0 deactivates automatic
	 * update
	 */
	public int updateIntervall;
	/** maximum number of articles to fetch and store */
	public int maxArticles;

	private boolean _running;
	private long _lastUpdate;
	RSSSpeaker _rssSpeaker;

	public CheckRssThread(RSSSpeaker rssSpeaker) {
		super();
		_running = false;
		_rssSpeaker = rssSpeaker;
		updateIntervall = UPDATE_DEFAULT;
		_lastUpdate = 0;
	}

	public boolean isRunning() {
		return _running;
	}

	public void stopUpdate() {
		_running = false;
	}

	public void run() {

		long updInt = updateIntervall * 60000;

		if (Log.isLoggable(TAG, Log.DEBUG))
			Log.d(TAG, "RSS THREAD updatePeriod = " + updInt + "ms");

		_lastUpdate = System.currentTimeMillis();
		try {
			_rssSpeaker.loadFeeds();
		} catch (Exception e) {
			e.printStackTrace();
		}
		_running = true;

		while (_running) {
			if ((System.currentTimeMillis() - _lastUpdate) > updInt) {
				_lastUpdate = System.currentTimeMillis();
				try {
					_rssSpeaker.loadFeeds();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			synchronized (this) {
				try {
					this.wait(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

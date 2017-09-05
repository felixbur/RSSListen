package com.felix.rsslisten;

import java.util.HashMap;

import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.felix.rsslisten.rssfeeds.Feed;
import com.felix.rsslisten.util.Preprocessor;

public class ReadRSSThread implements Runnable {
	// return vector of articles
	private Feed _currentFeed = null;
	private boolean _running = false;
	private String _displayTitle, _displayDescription;
	private int _lastArticleIndex = 0;
	private TextToSpeech _tts;
	private NewsDBAdapter _newsAdapter;
	private Cursor _articleCursor;
	private Preprocessor _newsProcessor;
	private RSSSpeaker _rssSpeaker;
	private String _imgUrl;

	public ReadRSSThread(NewsDBAdapter newsAdapter, RSSSpeaker rs) {
		super();
		_rssSpeaker = rs;
		_newsAdapter = newsAdapter;
		_lastArticleIndex = 0;
		try {
			_newsProcessor = Preprocessor.getInstance(
					Constants.TYPE_NEWS,
					MyApplication.getAssetManager().open(
							"preprocessor/preprocessor.properties"),
					MyApplication.getContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setTTS(TextToSpeech tts) {
		this._tts = tts;
	}

	public void setFeed(Feed currentFeed) {
		_currentFeed = currentFeed;
		_lastArticleIndex = 0;
	}

	public boolean isFeedSet() {
		if (null != _currentFeed)
			return true;
		return false;
	}

	public void setCursor(Cursor c) {
		_articleCursor = c;
		_lastArticleIndex = 0;
	}

	public void run() {
		_running = true;
		_rssSpeaker.startsTalking();
		try {
			if (_currentFeed != null) {
				_articleCursor = _newsAdapter.getAllEntries(_currentFeed.title);
			}
			int count = _articleCursor.getCount();
			_articleCursor.moveToPosition(_lastArticleIndex);
			Log.d(Constants.TAG, "cursor count = " + count);
			for (int i = _lastArticleIndex; i <= count; i++) {
				readCursor();
				if (!_running) {
					break;
				}
				_articleCursor.moveToNext();
				_lastArticleIndex++;
			}
			_rssSpeaker.stopsTalking();
			_running = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readCursor() {
		String title = _articleCursor.getString(NewsDBAdapter.COLUMN_TITLE);
		String description = _articleCursor
				.getString(NewsDBAdapter.COLUMN_DESCRIPTION);
		if(_currentFeed!=null) {
		Log.d(Constants.TAG, _currentFeed.title + ": " + title + " | "
				+ description);
		} else {
			Log.d(Constants.TAG, "article: " + title + " | "
					+ description);			
		}
		String processedTitle = _newsProcessor.process(title);
		String processedDescription = _newsProcessor.process(description);
		_displayTitle = processedTitle;
		_displayDescription = processedDescription;
		String toBeSpoken = processedTitle + ". " + processedDescription;
		speak(toBeSpoken);
	}

	public boolean isRunning() {
		return _running;
	}

	public void increaseIndex() {
		_lastArticleIndex++;
	}

	public void decreaseIndex() {
		_lastArticleIndex--;
		if (_lastArticleIndex < 0)
			_lastArticleIndex = 0;
	}

	public void stopMe() {
		_running = false;
		_rssSpeaker.stopsTalking();
		if (_tts != null) {
			_tts.stop();
		}
	}

	private void speak(String text) {
		try {
			handler.sendEmptyMessage(0);
			if (_tts != null && _running) {
				HashMap<String, String> params = new HashMap<String, String>();
				params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
						"theUtId");
				_tts.playEarcon("chimes", TextToSpeech.QUEUE_ADD, params);
				_tts.speak(text, TextToSpeech.QUEUE_ADD, params);
				while (_tts.isSpeaking()) {
					try {
						Thread.sleep(500);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			_rssSpeaker._ttsTitle.setText(_displayTitle);
			_rssSpeaker._ttsText.setText(_displayDescription);
		}
	};

}
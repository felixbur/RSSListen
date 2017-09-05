package com.felix.rsslisten;

/**
 * RSS Speaker
 * App to search for and read RSS Feeds in an Android Phone.
 * 
 * 
 * @todo save pictures in database
 * @todo make Feed URLs congigurable
 * 
 * 
 */
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.felix.rsslisten.rssfeeds.Article;
import com.felix.rsslisten.rssfeeds.CheckRssThread;
import com.felix.rsslisten.rssfeeds.Feed;
import com.felix.rsslisten.rssfeeds.RSSHandler;
import com.felix.rsslisten.util.AndroidHelper;
import com.felix.rsslisten.util.DateTimeUtil;
import com.felix.rsslisten.util.FileUtil;
import com.felix.rsslisten.util.StringUtil;

public class RSSSpeaker extends Activity implements OnInitListener,
		OnClickListener, OnItemSelectedListener {
	private final int SPEECH_SYNTHESIS_REQUEST_CODE = 0,
			VOICE_RECOGNITION_REQUEST_CODE = 1, MENU_OPTION_REMOVEENTRIES = 0,
			MENU_OPTION_LOADENTRIES = 1, MENU_OPTION_LOGENTRIES = 2,
			MENU_OPTION_SHOW_INFO = 3;

	private Button _readRssButton, _stopButton, _nextButton, _searchButton,
			_prevButton;
	public TextView _ttsText, _ttsTitle;
	private ReadRSSThread _readRSSThread;
	private TextToSpeech _tts;
	NewsDBAdapter _newsAdapter;
	private ArrayList<String> _matches = null;
	// private final String languageModel = "free_form";
	private final String languageModel = "web_search";
	// private ImageView _imageView;
	private CheckRssThread _checkRssThread;
	private Spinner _spinner;
	private Vector<Feed> _feeds = null;
	private String _selectedFeed = "";
	private boolean _first = true;
	private final String folderPath = Environment.getExternalStorageDirectory()
			.toString()
			+ System.getProperty("file.separator")
			+ Constants.FOLDER_NAME;

	// private Spinner ttsSpinner;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rssspeaker);
		TelephonyManager _telephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		_telephonyMgr.listen(new TeleListener(),
				PhoneStateListener.LISTEN_CALL_STATE);

		_newsAdapter = new NewsDBAdapter(MyApplication.getContext());
		_newsAdapter.open();
		_ttsText = (TextView) findViewById(R.id.ttsText);
		_ttsText.setMovementMethod(new ScrollingMovementMethod());
		_ttsTitle = (TextView) findViewById(R.id.ttsTitle);
		// _imageView = (ImageView) findViewById(R.id.image);
		_stopButton = (Button) findViewById(R.id.stop);
		_stopButton.setOnClickListener(this);
		_prevButton = (Button) findViewById(R.id.prev);
		_prevButton.setOnClickListener(this);
		_nextButton = (Button) findViewById(R.id.next);
		_nextButton.setOnClickListener(this);
		_searchButton = (Button) findViewById(R.id.search);
		_searchButton.setOnClickListener(this);
		_readRssButton = (Button) findViewById(R.id.readRss);
		_readRssButton.setOnClickListener(this);


		initRSSFeeds();
		initSpinner();
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, SPEECH_SYNTHESIS_REQUEST_CODE);

		_readRSSThread = new ReadRSSThread(_newsAdapter, this);
		try {
			_checkRssThread = new CheckRssThread(this);
			new Thread(_checkRssThread).start();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Constants.TAG, e.getMessage());
		}
	}

	public void initSpinner() {
		_spinner = (Spinner) findViewById(R.id.spinner);
		List<String> list = new ArrayList<String>();
		for (Feed f : _feeds) {
			list.add(f.title);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		_spinner.setAdapter(dataAdapter);
		_spinner.setOnItemSelectedListener(this);

	}

	public void onItemSelected(AdapterView<?> parent, View arg1, int pos,
			long arg3) {
		if (_first) {
			_first = false;
			return;
		}
		_selectedFeed = parent.getItemAtPosition(pos).toString();
		startReading();
	}

	private void startReading() {
		stopTTS();
		wipeTexts();
		readRSS();

	}

	protected void startsTalking() {
		Log.d(Constants.TAG, "starts talking");
		_talking = true;
		try {
			runOnUiThread(switchStopText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	boolean _talking = false;
	Runnable switchStopText = new Runnable() {
		public void run() {
			if (_talking)
				_stopButton.setText("stop");
			else
				_stopButton.setText("cont");
		}
	};

	protected void stopsTalking() {
		_talking = false;
		Log.d(Constants.TAG, "stops talking");
		try {
			runOnUiThread(switchStopText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	}

	private Feed getFeed(String title) {
		for (Feed f : _feeds)
			if (f.title.compareTo(title) == 0)
				return f;
		return null;
	}

	class TeleListener extends PhoneStateListener {
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				// CALL_STATE_IDLE;
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				// CALL_STATE_OFFHOOK;
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				stopTTS();
				break;
			default:
				break;
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.menu, menu);
		menu.add(Menu.NONE, MENU_OPTION_SHOW_INFO, Menu.NONE,
				getString(R.string.showInfo));
		menu.add(Menu.NONE, MENU_OPTION_REMOVEENTRIES, Menu.NONE,
				getString(R.string.removeEntries));
		menu.add(Menu.NONE, MENU_OPTION_LOADENTRIES, Menu.NONE,
				getString(R.string.loadEntries));
		menu.add(Menu.NONE, MENU_OPTION_LOGENTRIES, Menu.NONE,
				getString(R.string.logEntries));
		return true;

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case MENU_OPTION_SHOW_INFO:
			Toast.makeText(this, "Reads RSS feeds, set the feeds on your SD-card/internal memory in the rssspeaker/rssUrls.txt file\nRSS feeds are loaded into the android database in the background when app is started.",
					Toast.LENGTH_LONG).show();
			return true;
		case MENU_OPTION_REMOVEENTRIES:
			_newsAdapter.removeAllEntries(null);
			return true;
		case MENU_OPTION_LOADENTRIES:
			try {
				loadFeeds();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		case MENU_OPTION_LOGENTRIES:
			try {
				logEntries();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View v) {
		if (v.getId() == R.id.readRss) {
			readRSS();
		} else if (v.getId() == R.id.prev) {
			prevItem();
		} else if (v.getId() == R.id.next) {
			nextItem();
		} else if (v.getId() == R.id.stop) {
			if (_talking) {
				stopTTS();
			} else {
				continueReading();
			}
		} else if (v.getId() == R.id.search) {
			stopTTS();
			wipeTexts();
			startVoiceRecognitionActivity();
		}
	}

	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			Toast.makeText(this, "Text-To-Speech engine is initialized",
					Toast.LENGTH_LONG).show();
			// speakAndShow(_welcomeText);
			
			_tts.addEarcon("chimes", "com.felix.rsslisten", R.raw.chimes);
			_readRSSThread.setTTS(_tts);
		} else if (status == TextToSpeech.ERROR) {
			Toast.makeText(this,
					"Error occurred while initializing Text-To-Speech engine",
					Toast.LENGTH_LONG).show();
		}
	}

	public void speak(String text) {
		_tts.speak(text, TextToSpeech.QUEUE_ADD, null);
	}

	public void speakAndShow(String text) {
		speak(text);
		_ttsText.setText(text);
	}

	private void search() {
		try {
			String searchString = _matches.get(0);
			// speakAndShow("suche nach " + searchString);
			Cursor c = null;
			if (searchString.indexOf(" oder ") >= 0) {
				searchString = StringUtil.removeAllStopwords(searchString,
						new String[] { " oder" });
				Log.d(Constants.TAG, "searching for " + searchString);
				c = _newsAdapter.getArticleforKeywordORConnected(searchString);
			} else if (searchString.indexOf(" und ") >= 0) {
				searchString = StringUtil.removeAllStopwords(searchString,
						new String[] { " und" });
				Log.d(Constants.TAG, "searching for " + searchString);
				c = _newsAdapter.getArticleforKeywordORConnected(searchString);
			} else {
				Log.d(Constants.TAG, "searching for " + searchString);
				c = _newsAdapter.getArticleforKeyword(searchString);
			}
			if (c != null) {
				Log.d(Constants.TAG,
						"found article title: "
								+ c.getString(NewsDBAdapter.COLUMN_TITLE));
				_ttsText.setText("Erkannt: " + searchString);
				_readRSSThread.setFeed(null);
				_readRSSThread.setCursor(c);
				new Thread(_readRSSThread).start();
			} else {
				speakAndShow("Kein Artikel gefunden zu: " + searchString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
		// RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
		// _entryField.setText(result);

		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Using GoogleASR");
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	/**
	 * Handle the results from the recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			// Fill the list view with the strings the recognizer thought it
			// could have heard
			_matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			search();
		} else if (requestCode == SPEECH_SYNTHESIS_REQUEST_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// success, create the TTS instance
				_tts = new TextToSpeech(this, this);
			} else {
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void getMail() {
		try {
			_ttsText.setText("looking for new mails");
			Uri uri = Uri.parse(Constants.SYNC_EMAILS_REQUEST_URI);
			ContentResolver cr = this.getContentResolver();
			cr.query(uri, null, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void stopTTS() {
		if (_readRSSThread!=null) {
			_readRSSThread.stopMe();
		}
	}

	private void nextItem() {
		if (null != _readRSSThread && _readRSSThread.isRunning()) {
			stopTTS();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			_readRSSThread.increaseIndex();
			new Thread(_readRSSThread).start();
		} else if (null != _readRSSThread && _readRSSThread.isFeedSet()) {
			_readRSSThread.increaseIndex();
			new Thread(_readRSSThread).start();
		}
	}

	private void prevItem() {
		if (null != _readRSSThread && _readRSSThread.isRunning()) {
			stopTTS();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			_readRSSThread.decreaseIndex();
			new Thread(_readRSSThread).start();
		} else if (null != _readRSSThread && _readRSSThread.isFeedSet()) {
			_readRSSThread.decreaseIndex();
			new Thread(_readRSSThread).start();
		}
	}

	private void readRSS() {
		if(_talking)
		stopTTS();
		Feed selection = getFeed(_selectedFeed);
		try {
			_readRSSThread.setFeed(new Feed(selection.title, selection.uri));
			new Thread(_readRSSThread).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void continueReading() {
		try {
			new Thread(_readRSSThread).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void wipeTexts() {
		_ttsText.setText("");
		_ttsTitle.setText("");
	}

	public void loadFeeds() throws Exception {

		String output = "";
		for (Feed f : _feeds) {

			RSSHandler rh = new RSSHandler();
			Vector<Article> articles = rh.updateArticles(f);
			int newArticleCount = 0;
			if (articles != null && articles.size() > 0) {
				for (Article article : articles) {
					article.setId(article.getTitle().replaceAll(" ", ""));
					if (_newsAdapter.updateOrInsertEntry(f, article) != 0) {
						newArticleCount++;
					}
				}
				String log = "loaded " + articles.size() + " articles from "
						+ f.uri.toString() + ", new  article number: "
						+ newArticleCount;
				Log.d(Constants.TAG, log);
				output += log + "\n";
			} else {
				String mesg = "no articles loaded from " + f.uri.toString();
				Log.e(Constants.TAG, mesg);
				_ttsText.setText(mesg);

			}
		}

		_ttsText.setTag(output);
	}

	public void logEntries() throws Exception {
		for (Feed f : _feeds) {

			Cursor _articleCursor = _newsAdapter.getAllEntries(f.title);
			int count = _articleCursor.getCount();
			Log.i(Constants.TAG, "logging " + count + " entries for feed "
					+ f.title);
			while (_articleCursor.moveToNext()) {
				String title = _articleCursor
						.getString(NewsDBAdapter.COLUMN_TITLE);
				String description = _articleCursor
						.getString(NewsDBAdapter.COLUMN_DESCRIPTION);
				long date = Long.parseLong(_articleCursor
						.getString(NewsDBAdapter.COLUMN_UPDTIME));

				String log = "date [" + DateTimeUtil.getDate(date)
						+ "], title [" + title + "]";
				Log.i(Constants.TAG, log);
			}
		}
	}

	@Override
	protected void onDestroy() {
		stopTTS();
		_checkRssThread.stopUpdate();
		_checkRssThread = null;
		_newsAdapter.close();
		if(_tts!=null) {
			_tts.stop();
			_tts.shutdown();
			_tts = null;
		}
		super.onDestroy();
	}

	private void initRSSFeeds() {
		Vector<String> rssUrls = null;
		boolean useSdcard = AndroidHelper.hasStorage(true);

		try {
			String rssUrlsPath = folderPath
					+ System.getProperty("file.separator")
					+ Constants.RSSURLS_NAME;
			File rssUrlsFile = new File(rssUrlsPath);
			ensureSDFolder();
			rssUrls = FileUtil.getFileLines(MyApplication.getAssetManager()
					.open(Constants.RSSURLS_NAME), Constants.CHAR_ENC);
			if (useSdcard) {
				if (rssUrlsFile.exists()) {
					rssUrls = FileUtil.getFileLines(rssUrlsFile,
							Constants.CHAR_ENC);
				} else {
					FileUtil.writeFileContent(rssUrlsPath, rssUrls,
							Constants.CHAR_ENC);
				}
			}
			_feeds = new Vector<Feed>();
			boolean first = true;
			for (String line : rssUrls) {
				String title = line.split(",")[0];
				if (first) {
					_selectedFeed = title;
					first = false;
				}
				String url = line.split(",")[1];
				_feeds.add(new Feed(title, new URI(url)));
			}

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("RSSSpeaker", "error: " + e.getMessage());
		}

	}

	private void ensureSDFolder() {
		if (!FileUtil.existPath(folderPath)) {
			FileUtil.createDir(folderPath);
		}
	}
}
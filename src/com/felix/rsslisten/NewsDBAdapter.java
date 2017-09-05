/**
 * 
 */
package com.felix.rsslisten;

import java.net.MalformedURLException;
import java.net.URL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.felix.rsslisten.rssfeeds.Article;
import com.felix.rsslisten.rssfeeds.Feed;
import com.felix.rsslisten.util.Preprocessor;
import com.felix.rsslisten.util.StringUtil;

/**
 * Wrapper / Helper Klasse zum speichern der News in einer Datenbank zur
 * spaeteren Abfrage aus den anderen Programmteilen heraus.
 * 
 * @author Stefan Seide
 * @version 0.1
 */
public class NewsDBAdapter {

	private static final String DATABASE_NAME = "rssspeaker.db";
	private static final String DATABASE_TABLE = "news_table";
	private static final int DATABASE_VERSION = 5;

	public static final String KEY_ID = "id";

	// Namen und Nummer aller Spalten der news Tabelle
	public static final String KEY_FEED = "feed";
	public static final int COLUMN_FEED = 1;
	public static final String KEY_ARTICLE = "article";
	public static final int COLUMN_ARTICLE = 2;
	public static final String KEY_TITLE = "title";
	public static final int COLUMN_TITLE = 3;
	public static final String KEY_LINK = "link";
	public static final int COLUMN_LINK = 4;
	public static final String KEY_UNREAD = "unread";
	public static final int COLUMN_UNREAD = 5;
	public static final String KEY_DESCRIPTION = "description";
	public static final int COLUMN_DESCRIPTION = 6;
	public static final String KEY_UPDTIME = "last_update";
	public static final int COLUMN_UPDTIME = 7;
	public static final String KEY_IMAGEURL = "imageurl";
	public static final int COLUMN_IMAGEURL = 8;
	public static final int OLDNEWS_DAYS = 2;

	private SQLiteDatabase newsDb;
	private MyDbHelper dbHelper;
	private Preprocessor _newsProcessor = null;

	public NewsDBAdapter(Context ctx) {
		try {
			_newsProcessor = Preprocessor.getInstance(
					Constants.TYPE_NEWS,
					MyApplication.getAssetManager().open(
							"preprocessor/preprocessor.properties"),
					MyApplication.getContext());
		} catch (Exception e) {
			e.printStackTrace();
		}

		dbHelper = new MyDbHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		try {
			newsDb = dbHelper.getWritableDatabase();
		} catch (Exception e) {
			// manchmal auf Dell Exception - einzige Loesung Loeschen und neu
			// hilft aber oft auch nicht - permission denied - dann uninstall
			// SQLiteDatabase open path:
			// /data/data/com.tlabs.cld/databases/cldNews.db
			// sqlite3_open_v2("/data/data/com.tlabs.cld/databases/cldNews.db",
			// &handle, 6, NULL) failed
			dbHelper.close();
			if (null == ctx || !ctx.deleteDatabase(DATABASE_NAME)) {
				Log.e(Constants.TAG, "Cannot delete news database: "
						+ DATABASE_NAME);
			} else {
				newsDb = dbHelper.getWritableDatabase();
			}
		}
	}

	public NewsDBAdapter open() throws SQLException {
		newsDb = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		if (newsDb != null) {
			newsDb.close();
			newsDb = null;
		}
	}

	public long insertEntry(Feed feed, Article article) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_FEED, feed.title);
		cv.put(KEY_ARTICLE, article.getId());
		cv.put(KEY_TITLE, article.getTitle());

		if (article.hasLink()) {
			cv.put(KEY_LINK, article.getLink().toString());
		} else {
			cv.put(KEY_LINK, "");
		}
		cv.put(KEY_UNREAD, article.isUnread());
		cv.put(KEY_DESCRIPTION, _newsProcessor.process(article.getDescription()));
		cv.put(KEY_UPDTIME, article.getPubDate());
		if (article.hasImage()) {
			cv.put(KEY_IMAGEURL, article.getImageUrl());
		} else {
			cv.put(KEY_IMAGEURL, "");
		}

		try {
			return newsDb.insert(DATABASE_TABLE, null, cv);
		} catch (SQLiteException se) {
			Log.i(Constants.TAG,
					"Problem insert new news entry: " + se.getMessage());
			return 0;
		}
	}

	public boolean removeEntry(long rowIndex) {
		try {
			return newsDb.delete(DATABASE_TABLE, KEY_ID + "=?",
					new String[] { String.valueOf(rowIndex) }) > 0;
		} catch (Exception e) {
			Log.w(Constants.TAG, "cannot delete entry with row index "
					+ rowIndex + " from db: " + e.getMessage());
			return false;
		}
	}

	public boolean removeOldEntry() {
		long deleteTime = System.currentTimeMillis() - OLDNEWS_DAYS * 24 * 3600
				* 1000;

		if (Log.isLoggable(Constants.TAG, Log.DEBUG))
			Log.d(Constants.TAG,
					"database deleteTime : " + String.valueOf(deleteTime));

		try {
			return newsDb.delete(DATABASE_TABLE, KEY_UPDTIME + "<?",
					new String[] { String.valueOf(deleteTime) }) > 0;
		} catch (Exception e) {
			Log.i(Constants.TAG,
					"cannot delete old entries because database locked");
			return false;
		}
	}

	public boolean removeAllEntries(String feed) {

		try {
			if (feed == null) {
				Log.d(Constants.TAG, "deleting all messeages");
				return newsDb.delete(DATABASE_TABLE, null, null) > 0;
			} else {
				Log.d(Constants.TAG, "deleting all messeages for feed: " + feed);
				return newsDb.delete(DATABASE_TABLE, KEY_FEED + "=?",
						new String[] { feed }) > 0;
			}
		} catch (Exception e) {
			Log.w(Constants.TAG,
					"cannot delete all entries from db: " + e.getMessage());
			return false;
		}
	}

	public Cursor getAllEntries(String feed) {
		String whereClause = null;
		String[] whereArgs = null;
		if (feed != null) {
			whereClause = "feed=?";
			whereArgs = new String[] { feed };
		}

		return newsDb.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_FEED,
				KEY_ARTICLE, KEY_TITLE, KEY_LINK, KEY_UNREAD, KEY_DESCRIPTION,
				KEY_UPDTIME, KEY_IMAGEURL }, whereClause, whereArgs, null,
				null, KEY_UPDTIME + " DESC");
	}

	public Cursor getEntryCursor(long rowIndex) {
		return newsDb.query(DATABASE_TABLE, new String[] { KEY_TITLE, KEY_LINK,
				KEY_UNREAD, KEY_DESCRIPTION, KEY_IMAGEURL }, KEY_ID + "=?",
				new String[] { String.valueOf(rowIndex) }, null, null, null,
				null);
	}

	public Article getEntry(long rowIndex) {
		Article a = new Article();
		Cursor c = null;
		try {
			c = getEntryCursor(rowIndex);
			if (c.moveToFirst()) {
				a.setTitle(c.getString(0));
				try {
					a.setLink(new URL(c.getString(1)));
				} catch (MalformedURLException e) {
					Log.w(Constants.TAG,
							"no link or misformatted one for news with id "
									+ rowIndex);
					a.setLink(null);
				}
				a.setUnread((c.getInt(2) == 0) ? false : true);
				a.setDescription(c.getString(3));
			}
		} finally {
			if (c != null)
				c.close();
			c = null;
		}
		return a;
	}

	public int updateEntryAsRead(long rowIndex) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_UNREAD, Boolean.TRUE);

		try {
			return newsDb.update(DATABASE_TABLE, cv, KEY_ID + "=?",
					new String[] { String.valueOf(rowIndex) });
		} catch (Exception e) {
			Log.w(Constants.TAG, "Cannot update read flag of entry " + rowIndex
					+ ": " + e.getMessage());
			return 0;
		}
	}

	public long updateOrInsertEntry(Feed feed, Article article) {
		Cursor c = null;
		try {
			// c = newsDb.query(DATABASE_TABLE, new String[] { KEY_ID },
			// KEY_FEED
			// + "=? and " + KEY_ARTICLE + "=?", new String[] {
			// feed.title, article.getId() }, null, null, null, null);
			c = newsDb.query(DATABASE_TABLE, new String[] { KEY_ID },
					KEY_ARTICLE + "=?", new String[] { article.getId() }, null,
					null, null, null);
			if (c.moveToFirst()) {
				return 0;
			}
		} catch (NullPointerException e) {
			Log.e(Constants.TAG, e.getMessage() + " c=" + c + ", newsDB="
					+ newsDb, e);
		} finally {
			if (c != null)
				c.close();
			c = null;
		}
		return insertEntry(feed, article);
	}

	public Cursor getArticleforKeyword(String keyword) {
		Cursor c = null;
		try {

			Cursor retC = newsDb.query(DATABASE_TABLE, new String[] { KEY_ID,
					KEY_FEED, KEY_ARTICLE, KEY_TITLE, KEY_LINK, KEY_UNREAD,
					KEY_DESCRIPTION, KEY_UPDTIME, KEY_IMAGEURL },
					KEY_DESCRIPTION + " LIKE ? OR " + KEY_TITLE + " LIKE ?",
					new String[] { "%" + keyword + "%", "%" + keyword + "%" },
					null, null, KEY_UPDTIME + " DESC");
			if (retC == null) {
				Log.d(Constants.TAG, "nothing found for: " + keyword);
				return null;
			}
			if (retC.getCount() == 0) {
				Log.d(Constants.TAG, "nothing found, count ==0: " + keyword);
				return null;
			}
			retC.moveToFirst();
			Log.d(Constants.TAG, "c: " + retC.getString(COLUMN_DESCRIPTION));
			return retC;

		} catch (NullPointerException e) {
			Log.e(Constants.TAG, e.getMessage() + " c=" + c + ", newsDB="
					+ newsDb, e);
		} finally {
			if (c != null)
				c.close();
			c = null;
		}
		return null;
	}

	public Cursor getArticleforKeywordORConnected(String keyword) {
		Cursor c = null;
		String[] words = StringUtil.stringToArray(keyword);
		String selStm = "";
		String[] whereArgs = new String[words.length * 2];
		for (int i = 0; i < words.length; i++) {
			String w = words[i];
			if (i == 0) {
				selStm = KEY_DESCRIPTION + " LIKE ? OR " + KEY_TITLE
						+ " LIKE ?";
			} else {
				selStm += " OR " + KEY_DESCRIPTION + " LIKE ? OR " + KEY_TITLE
						+ " LIKE ?";
			}
			whereArgs[i * 2] = "%" + w + "%";
			whereArgs[i * 2 + 1] = "%" + w + "%";
		}
		try {

			Cursor retC = newsDb.query(DATABASE_TABLE, new String[] { KEY_ID,
					KEY_FEED, KEY_ARTICLE, KEY_TITLE, KEY_LINK, KEY_UNREAD,
					KEY_DESCRIPTION, KEY_UPDTIME, KEY_IMAGEURL }, selStm,
					whereArgs, null, null, KEY_UPDTIME + " DESC");
			// Cursor retC = newsDb.query(DATABASE_TABLE, new String[] { KEY_ID,
			// KEY_FEED, KEY_ARTICLE, KEY_TITLE, KEY_LINK, KEY_UNREAD,
			// KEY_DESCRIPTION, KEY_UPDTIME, KEY_IMAGEURL },
			// KEY_DESCRIPTION + " LIKE ? OR " + KEY_TITLE + " LIKE ?",
			// new String[] { "%" + keyword + "%", "%" + keyword + "%" },
			// null, null, KEY_UPDTIME + " DESC");
			if (retC == null) {
				Log.d(Constants.TAG, "nothing found for: " + keyword);
				return null;
			}
			if (retC.getCount() == 0) {
				Log.d(Constants.TAG, "nothing found, count ==0: " + keyword);
				return null;
			}
			retC.moveToFirst();
			Log.d(Constants.TAG, "c: " + retC.getString(COLUMN_DESCRIPTION));
			return retC;

		} catch (NullPointerException e) {
			Log.e(Constants.TAG, e.getMessage() + " c=" + c + ", newsDB="
					+ newsDb, e);
		} finally {
			if (c != null)
				c.close();
			c = null;
		}
		return null;
	}

	public Cursor getArticleforKeywordANDConnected(String keyword) {
		Cursor c = null;
		String[] words = StringUtil.stringToArray(keyword);
		String selStm = "";
		String[] whereArgs = new String[words.length * 2];
		for (int i = 0; i < words.length; i++) {
			String w = words[i];
			if (i == 0) {
				selStm = "(" + KEY_DESCRIPTION + " LIKE ? OR " + KEY_TITLE
						+ " LIKE ?)";
			} else {
				selStm += " AND (" + KEY_DESCRIPTION + " LIKE ? OR "
						+ KEY_TITLE + " LIKE ?)";
			}
			whereArgs[i * 2] = "%" + w + "%";
			whereArgs[i * 2 + 1] = "%" + w + "%";
		}
		try {

			Cursor retC = newsDb.query(DATABASE_TABLE, new String[] { KEY_ID,
					KEY_FEED, KEY_ARTICLE, KEY_TITLE, KEY_LINK, KEY_UNREAD,
					KEY_DESCRIPTION, KEY_UPDTIME, KEY_IMAGEURL }, selStm,
					whereArgs, null, null, KEY_UPDTIME + " DESC");
			if (retC == null) {
				Log.d(Constants.TAG, "nothing found for: " + keyword);
				return null;
			}
			if (retC.getCount() == 0) {
				Log.d(Constants.TAG, "nothing found, count ==0: " + keyword);
				return null;
			}
			retC.moveToFirst();
			Log.d(Constants.TAG, "c: " + retC.getString(COLUMN_DESCRIPTION));
			return retC;

		} catch (NullPointerException e) {
			Log.e(Constants.TAG, e.getMessage() + " c=" + c + ", newsDB="
					+ newsDb, e);
		} finally {
			if (c != null)
				c.close();
			c = null;
		}
		return null;
	}

	private static class MyDbHelper extends SQLiteOpenHelper {

		// sql statement zum erstellen
		private static final String DATABASE_CREATE = "create table "
				+ DATABASE_TABLE + "(" + KEY_ID
				+ " integer primary key autoincrement, " + KEY_FEED
				+ " text not null, " + KEY_ARTICLE + " text not null, "
				+ KEY_TITLE + " text not null, " + KEY_LINK
				+ " text not null, " + KEY_UNREAD + " text not null, "
				+ KEY_DESCRIPTION + " text not null, " + KEY_UPDTIME
				+ " long not null, " + KEY_IMAGEURL + " text not null);";

		public MyDbHelper(Context ctx, String name, CursorFactory cursorFac,
				int version) {
			super(ctx, name, cursorFac, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(Constants.TAG, "upgrading database from version "
					+ oldVersion + " to " + newVersion);
			// existiert eh noch keine alter Version, also drop + create
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);
		}
	}
}

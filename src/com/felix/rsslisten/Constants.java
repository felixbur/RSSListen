package com.felix.rsslisten;

public class Constants {
	public final static String CHAR_ENC = "UTF-8";
	public final static String MIME_IMG = "image/";
	public final static int DISPLAY_WIDTH = 90;
	public final  static int WAIT_BEFORE_BEEP=1000;
	public final  static int WAIT_AFTER_BEEP=1000;
	public final  static int WAIT_AFTER_TITLE=500;
	public final static int TYPE_NEWS=2;
	public final static int TYPE_GENERAL=3;
	public final static int TYPE_SMS=4;
	public final static String FOLDER_NAME = "rsslisten";
	public final static String UNREAD_EMAILS_REQUEST_URI = "content://com.fsck.k9.mailprovider/unread_mails";
	public final static String READ_EMAILS_REQUEST_URI = "content://com.fsck.k9.mailprovider/read_mails";
	public final static String SYNC_EMAILS_REQUEST_URI = "content://com.fsck.k9.mailprovider/check_mail";
	public final static String TAG = "rsslisten";
	public static final String RSSURLS_NAME = "rssUrls.txt";
}

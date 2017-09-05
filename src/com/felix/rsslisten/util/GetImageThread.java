package com.felix.rsslisten.util;

import java.net.URI;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.felix.rsslisten.Constants;

/**
 * @author felix
 * 
 */
public class GetImageThread implements Runnable {

	String _url = null;
	String _thumbUrl = null;
	ImageReceiverInterface _imageReceiver;
	private int _displayWidth;
	private boolean _stop = false;

	public GetImageThread(String url, String thumbUrl,
			ImageReceiverInterface imageReceiver, int displayWidth) {
		_url = url;
		_thumbUrl = thumbUrl;
		_imageReceiver = imageReceiver;
		_displayWidth = displayWidth;
	}

	public void run() {
		_stop = false;
		if (_url != null) {
			if (Log.isLoggable(Constants.TAG, Log.DEBUG))
				Log.d(Constants.TAG, "getting image from " + _url);
			try {
				PictureDownloader pd = new PictureDownloader();
				Bitmap picBitmap = pd.downloadNewPicture(new URI(_url),
						_displayWidth);

				if ((picBitmap == null) && (_thumbUrl != null)) {
					if (Log.isLoggable(Constants.TAG, Log.DEBUG))
						Log.d(Constants.TAG, "getting image from ThumbUrl : "
								+ _thumbUrl);
					picBitmap = pd.downloadNewPicture(new URI(_thumbUrl),
							_displayWidth);
				}
				Drawable image = null;
				if (picBitmap != null) {

					image = new BitmapDrawable(picBitmap);
				}
				if (!_stop)
					_imageReceiver.foundImage(image);
			} catch (Exception e) {
				e.printStackTrace();
				if (e.getMessage() != null) {
					Log.e(Constants.TAG, e.getMessage());
				}
			}
		}
	}

	public void stopMe() {
		_stop = true;
	}

}

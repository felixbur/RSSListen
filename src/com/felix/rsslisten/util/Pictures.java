package com.felix.rsslisten.util;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.felix.rsslisten.Constants;

public class Pictures {
	public static Bitmap downloadNewPicture(URI uri, int width) {
		Log.d(Constants.TAG, "downloadNewPicture, start download new picture: "
				+ uri);
		Bitmap bm = null;
		HttpResponse response = null;
		DefaultHttpClient httpclient = new DefaultHttpClient();

		try {
			HttpGet httpget = new HttpGet(uri);
			response = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			Log.e(Constants.TAG, "Protocol-Error from downloadNewPicture == "
					+ e.getMessage());
			return null;
		} catch (IllegalArgumentException iae) {
			Log.e(Constants.TAG,
					"URL-Error from downloadNewPicture == " + iae.getMessage());
			return null;
		} catch (IOException e) {
			Log.e(Constants.TAG,
					"IO-Error from downloadNewPicture == " + e.getMessage());
			return null;
		}

		try {
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				Log.e(Constants.TAG,
						"Error from downloadNewPicture == response entity is empty");
				return null;
			}

			bm = BitmapFactory.decodeStream(entity.getContent());
			entity.consumeContent();
			entity = null;
			if (bm == null) {
				Log.e(Constants.TAG,
						"Error from downloadNewPicture == bitmap is empty");
				return null;
			}
			int bheight = bm.getHeight();
			int bwidth=bm.getWidth();
			int newHeight = (int) ((double)width * (double)(bheight/bwidth));
			bm = Bitmap.createScaledBitmap(bm, width, newHeight, true);
			Log.d(Constants.TAG,
					"downloadNewPicture, finished download new picture: " + uri
							+ " got " + bm.getHeight() + "x" + bm.getWidth()
							+ " pixel ");
		} catch (IOException e1) {
			Log.e(Constants.TAG,
					"Error from downloadNewPicture == " + e1.getMessage());
		} catch (OutOfMemoryError ome) {
			Log.e(Constants.TAG, "Error downloadNewPicture, picture too large",
					ome);
		} finally {
		}
		return bm;
	}
}

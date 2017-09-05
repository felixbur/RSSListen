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

public class PictureDownloader {

	public  static Bitmap downloadNewPicture(URI uri, int destWidth) {
		Bitmap bm = null;
		HttpResponse response = null;
		DefaultHttpClient httpclient = new DefaultHttpClient();

		try {
			HttpGet httpget = new HttpGet(uri);
			response = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			return null;
		} catch (IllegalArgumentException iae) {
			return null;
		} catch (IOException e) {
			return null;
		}

		try {
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return null;
			}
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inTempStorage = new byte[16*1024];
			bm = BitmapFactory.decodeStream(entity.getContent(),null,options);
			entity.consumeContent();
			entity = null;
			int bheight = bm.getHeight();
			int bwidth=bm.getWidth();		
			int newHeight = (int) ((double)destWidth * (double)(bheight/bwidth));
			bm = Bitmap.createScaledBitmap(bm, destWidth, newHeight, true);
			if (bm == null) {
				return null;
			}
		} catch (IOException e1) {
		} catch (OutOfMemoryError ome) {
		} finally {
		}
		return bm;
	}

}

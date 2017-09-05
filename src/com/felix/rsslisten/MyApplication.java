package com.felix.rsslisten;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

public class MyApplication extends Application {

	private static MyApplication instance;

	public MyApplication() {
		instance = this;
	}

	public static Context getContext() {
		return instance;
	}

	public static AssetManager getAssetManager() {
		return instance.getAssets();
	}
}

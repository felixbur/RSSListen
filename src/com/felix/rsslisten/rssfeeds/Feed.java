package com.felix.rsslisten.rssfeeds;

import java.net.URI;


public class Feed extends Object {
    public long feedId;
    public String title;
    public URI uri;
    public void clear() {
    	feedId = 0;
    	title = null;
    	uri = null;
    }
	public Feed(String title, URI uri) {
		super();
		this.title = title;
		this.uri = uri;
	}

}

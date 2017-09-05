package com.felix.rsslisten.rssfeeds;

import java.net.URL;

public class Article extends Object {
	private String _title;
	private URL _link;
	private String _description;
	private boolean _unread = false, _hasImage = false, _hasLink = false;
	private String _imageUrl;
	private String _id = "";
	private long _pubDate;

	public Article() {
		_title = "";
		_link = null;
		_description = "";
		_imageUrl = "";
		_unread = true;
		_id = "";
		_pubDate = System.currentTimeMillis();
	}

	public String toString() {
		return _title + ",\n" + _id + ",\n" + _pubDate + ",\n" + _link.getPath()
				+ ",\n" + _description + ",\n" + _imageUrl + "\n"
				+ String.valueOf(_unread);
	}

	public String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		this._title = title;
	}

	public URL getLink() {
		return _link;
	}

	public void setLink(URL link) {
		this._link = link;
		if (link != null)
			_hasLink = true;
		else
			_hasLink = false;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		this._description = description;
	}

	public boolean isUnread() {
		return _unread;
	}

	public void setUnread(boolean unread) {
		this._unread = unread;
	}

	public boolean hasImage() {
		return _hasImage;
	}

	public boolean hasLink() {
		return _hasLink;
	}

	public String getImageUrl() {
		return _imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this._imageUrl = imageUrl;
		if (imageUrl != null && imageUrl.length() > 0)
			_hasImage = true;
		else
			_hasImage = false;
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		this._id = id;
	}

	public long getPubDate() {
		return _pubDate;
	}

	public void setPubDate(long pubDate) {
		this._pubDate = pubDate;
	}

}

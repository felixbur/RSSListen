package com.felix.rsslisten.rssfeeds;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.felix.rsslisten.Constants;

public class RSSHandler extends DefaultHandler {

	// Used to define what elements we are currently in
	private boolean inItem = false;
	private boolean inTitle = false;
	private boolean inLink = false;
	private boolean inDescription = false;
	private StringBuffer currentChars = new StringBuffer(1000);

	// return vector of articles
	private Vector<Article> articles;
	private Article currentArticle;

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		if (name.trim().equals("title")) {
			inTitle = true;
			currentChars.delete(0, currentChars.length());
		} else if (name.trim().equals("item")) {
			inItem = true;
			currentArticle = new Article();
			currentChars.delete(0, currentChars.length());
		} else if (name.trim().equals("link")) {
			inLink = true;
			currentChars.delete(0, currentChars.length());
		} else if (name.trim().equals("description")) {
			inDescription = true;
			currentChars.delete(0, currentChars.length());
		} else if (name.trim().equals("enclosure")) {
			int length = atts.getLength();
			// Process each attribute
			boolean image = false;
			for (int i = 0; i < length; i++) {
				// Get names and values for each attribute
				String aName = atts.getQName(i);
				String value = atts.getValue(i);
				if (aName.compareToIgnoreCase("type") == 0) {
					if (value.startsWith(Constants.MIME_IMG))
						image = true;
				}
				if (aName.compareToIgnoreCase("url") == 0) {
					if (image) {
						currentArticle.setImageUrl(value);
					}
				}
			}
		}
	}

	public void endElement(String uri, String name, String qName)
			throws SAXException {
		if (name.trim().equals("title")) {
			inTitle = false;
			if (inItem)
				currentArticle.setTitle(currentChars.toString());
		} else if (name.trim().equals("item")) {
			inItem = false;
		} else if (name.trim().equals("link")) {
			try {
				inLink = false;
				if (inItem)
					currentArticle.setLink(new URL(currentChars.toString()));
			} catch (MalformedURLException e) {
				Log.e("RSSHandler", e.toString());
			}
		} else if (name.trim().equals("description")) {
			inDescription = false;
			if (inItem)
				currentArticle.setDescription(currentChars.toString());
		}
		if (inItem == false && currentArticle != null) {
			articles.add(currentArticle);
			currentArticle = null;
		}
	}

	public void characters(char ch[], int start, int length) {

		if (inItem) {
			if (inLink || inTitle || inDescription) {
				currentChars.append(ch, start, length);
			}
		}
	}

	public Vector<Article> updateArticles(Feed feed) {
		try {
			articles = new Vector<Article>();
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);
			InputSource is = new InputSource(feed.uri.toURL().openStream());
			if (feed.title.regionMatches(true, 0, "spiegel", 0,
					feed.title.length())) {
				// workaround android sax parser bug
				is.setEncoding("ISO-8859-1");
			}
			xr.parse(is);
			return articles;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}

package com.felix.rsslisten.rssfeeds;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.CharArrayBuffer;

import android.util.Log;

/**
 * Class capable of sending HTTPRequest to some given URI.
 * The retrieved content is passed over to the caller.
 * 
 * @author Jakob Sachse
 *
 */
public class HTTPRequest {

	public final static String HTTP_METHOD_POST = "POST";
	public final static String HTTP_METHOD_GET = "GET";
	private final static int mBufferSize = 256;
	private URI mUri = null;
	private DefaultHttpClient client = null;
	private LinkedList<NameValuePair> mPairs = null;
	private String mMethod = null;
	private HttpUriRequest mHtUriRequest;
	private HttpResponse mResponse = null;
	
	/**
	 * Default Constructor that could be used to fill in
	 * in Request information (uri, method) later
	 */
	public HTTPRequest(){
		client = new DefaultHttpClient();
		mPairs = new LinkedList<NameValuePair>();
	}
	
	/**
	 * Creates a HTTPRequest Object capable of retrieving content via HTTP.
	 * 
	 * @param uri the URI to connect to
	 * @param method one of the constant fields in HTTPRequest (POST, GET, PUT, DELETE)
	 * @throws Exception
	 */
	public HTTPRequest(URI uri, final String method) throws Exception{
		this();
		//setters throw exceptions if something isn't valid
		setUri(uri);
		setRequestMethod(method);
	}
	
	/**
	 * @return if Request has all necessary (valid method, valid uri) properties to execute
	 */
	public boolean isReadyToExecute(){
		if(mUri != null && mHtUriRequest != null){
			return true;
		}
		return false;
	}

	/**
	 * Sends a Request to the given URI. 
	 * The method blocks until it has retrieved
	 * a Response or a Timeout occurs.
	 * 
	 * @return HttpResponse Object that was returned when executing the request.
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse doRequestForResponse() throws ClientProtocolException, IOException{

		if(!isReadyToExecute())
			throw new IOException("HTTPRequest laggs necessary properties (method + uri)");
		
		if(mHtUriRequest instanceof HttpGet){
			mUri = HTTPRequest.appendGetParameters(mUri, mPairs);
			try {
				setRequestMethod(mMethod);
			} catch (Exception e) {
				Log.println(Thread.NORM_PRIORITY, "HTTPRequest", e.getMessage());
			}
		}else if(mHtUriRequest instanceof HttpPost){
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(mPairs);
			((HttpPost)mHtUriRequest).setEntity(entity);
		}

		mHtUriRequest.setHeader("user-agent", "Google Android 1.0 r1");
		return client.execute(mHtUriRequest);
	}
	
	/**
	 * Puts parameters at the end of a given URI. 
	 * Retains existing parameters.
	 * 
	 * @param originalURI the URI to be modified
	 * @param nameValuePairs the modified URI
	 * @return the URI containing the appended parameters
	 */
	private static final URI appendGetParameters(URI originalURI, LinkedList<NameValuePair> nameValuePairs){
		try{
			String query = originalURI.toURL().getQuery();
			StringBuffer sb = new StringBuffer();
			if(query != null)
				sb.append(query);
			for(NameValuePair pair : nameValuePairs){
				sb.append("&"+pair.getName()+"="+pair.getValue());
			}if(sb.length() > 0 && sb.charAt(0) != '?'){
				sb.replace(0, 1, "?");
			}
			String oldUrl = originalURI.toString();
			if(sb.length() > 0){
				int position;
				if((position = oldUrl.lastIndexOf('?')) != -1){
					oldUrl = oldUrl.substring(0, position-1);
				}
				if(oldUrl.charAt(oldUrl.length()-1) == '/'){
					oldUrl = oldUrl.substring(0, oldUrl.length()-1);
				}
				Log.println(Thread.NORM_PRIORITY, "HTTPRequest", oldUrl + sb.toString());
				return new URL(oldUrl + sb.toString()).toURI();
			}
		}catch(MalformedURLException e){
			Log.println(Thread.MAX_PRIORITY, "HTTPRequest", e.toString());
		}catch (URISyntaxException e) {
			Log.println(Thread.MAX_PRIORITY, "HTTPRequest", e.toString());
		}
		return originalURI;
	}
	
	/**
	 * Sends the request to a previous given URI and returns content of response as a String.
	 * If Status is not HTTP 200 (ok) it returns the reason for not having received HTTP 200.
	 * The method blocks until it has retrieved a Response or a Timeout occurs.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @return the returned http message as a String or in case the Reason of Failure
	 */
	public String doRequestForString() throws ClientProtocolException, IOException{
		InputStream is = doRequestForStream();
		CharArrayBuffer cb = new CharArrayBuffer(mBufferSize);
		byte[] b = new byte[mBufferSize];
		int bytesInBuffer;
		StringBuffer sb = new StringBuffer();

		while((bytesInBuffer = is.read(b)) != -1){
			cb.append(b, 0, bytesInBuffer);
			sb.append(cb.toCharArray());
			cb.clear();
		}
		return sb.toString();
	}
	
	public InputStream doRequestForStream() throws ClientProtocolException, IOException{
		mResponse = doRequestForResponse();
		StatusLine status = mResponse.getStatusLine();

		if(status.getStatusCode() == HttpURLConnection.HTTP_OK){
		
			return mResponse.getEntity().getContent();
		}else 
			throw new IOException("Status is not HTTP_OK: "+status.getStatusCode()+" "+status.getReasonPhrase());
	}
	
	/**
	 * Supported Methods: GET, POST
	 * @param method
	 * @throws Exception
	 */
	public void setRequestMethod(String method) throws Exception{
		if(HTTP_METHOD_GET.equals(method)){
			mMethod = HTTP_METHOD_GET;
			mHtUriRequest = new HttpGet(mUri);
		}else if(HTTP_METHOD_POST.equals(method)){
			mMethod = HTTP_METHOD_POST;
			mHtUriRequest = new HttpPost(mUri);
		}else{
			throw new Exception("Method is not supported, see HTTPRequest Constant Fields.");
		}
	}
	
	/**
	 * Sets an Array of Request Parameters
	 * 
	 * @param pair the array
	 */
	public void setParams(NameValuePair[] pair){
		for(int i = 0; i<pair.length; i++)
			mPairs.add(pair[i]);
	}
	
	/**
	 * Sets a Request Parameter
	 * 
	 * @param param the parameter to set
	 */
	public void setParam(NameValuePair param){
		mPairs.add(param);
	}
	
	/**
	 * Sets a Request Parameter, uses name and value as provided
	 * 
	 * @param name the parameters name
	 * @param value the value
	 */
	public void setParam(String name, String value){
		mPairs.add(new BasicNameValuePair(name, value));
	}
	
	/**
	 * @return the URI that the Request is set to
	 * */
	public URI getMUri() {
		return mUri;
	}

	/**
	 * Sets the URI that the Request points to
	 * */
	public void setUri(URI uri) {
		mUri = uri;
	}

	/**
	 * @return the method to use when doing the Request
	 */
	public String getRequestMethod() {
		return mMethod;
	}
	/**
	 * @return the Response if doRequestForString() was executed, or null if not set
	 */
	public HttpResponse getResponse() {
		return mResponse;
	}
}

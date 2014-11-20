package com.kapx.ucms;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

public class HttpUCMSClient {	
	public static final String AMAZON_DOMAIN_IDENTIFIER = "s3.amazonaws.com";
	public static final String UCMS_DOMAIN_IDENTIFIER = "appspot.com";
	public static final String DAM_DOMAIN_IDENTIFIER  = "alfresco";
	public static final String PROP_BVCID = "brightcoveProviderId";
	public static final String PROP_DURATION = "durationSeconds";
	public static final String PROP_KAPX_SLIDE_COUNT = "kapxSlideCount";
	public static final String PROP_KAPX_ID = "kapxSlideDeckURL";
	public static final String PROP_USERNAME = "username";
	public static final String PROP_PWD = "password";
	public static final String PROP_DAM_HOST = "damhost";
	public static final String PROP_DAM_PORT = "damport";
	public static final String PROP_PROTOCOL = "http";
	public static final String PROP_UCMSNOTIFY_URL = "ucmsnotifyurl";
	public static final String PROP_UCMSNOTIFYDOWNLOAD_URI = "ucmsnotifydownload";
	public static final String PROP_UCMSNOTIFYPUBLISH_URI = "ucmsnotifypublish";	
	public static final String PROP_LOGINAPI_URI = "loginapirequest";
	public static final String PROP_PUBAPI_URI = "publishapirequest";
	public static final String PROP_PUB_ERROR = "publishError";
	
	private final static String BRIGHTCOVECHANNEL = "UCMSBrightcoveChannel";
	private final static String KAPXCHANNEL = "UCMSKAPXChannel";
	
	
	HttpResponse response = null;
	Properties props = new Properties();
	
	
	public HttpResponse executeRequest(String strResourceURL) throws ClientProtocolException, IOException{
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpGet = null;
		if(strResourceURL.toLowerCase().contains(UCMS_DOMAIN_IDENTIFIER)){
			System.out.println("Download from UCMS Domain");
			UCMSServiceHelper ucmService = new UCMSServiceHelper();
			httpGet = ucmService.createUCMSHeader(strResourceURL,UCMS_DOMAIN_IDENTIFIER);
		}else{
			System.out.println("Download from Public Site");
			httpGet = new HttpGet(strResourceURL);
		}					
        response = httpclient.execute(httpGet);
        return response;                     
    }	
	
	public String getAlfrescoTicket(){		
		
		String Ticket	= "";
		try {
			props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
			JSONObject loginObj	= new JSONObject();		
			loginObj.put(PROP_USERNAME, props.getProperty(PROP_USERNAME).trim());		
			loginObj.put(PROP_PWD, props.getProperty(PROP_PWD).trim());
		
			String host = props.getProperty(PROP_DAM_HOST).trim();
			int port = Integer.parseInt(props.getProperty(PROP_DAM_PORT).trim());	
			String loginAPI_URI	= props.getProperty(PROP_LOGINAPI_URI).trim();
			
			System.out.println("LOGIN API:"+PROP_PROTOCOL+":/"+host+":"+port+"/"+loginAPI_URI);
			URI urilogin =  URIUtils.createURI(PROP_PROTOCOL, host, port, loginAPI_URI,null, null);		
			HttpPost httpLoginPost = new HttpPost(urilogin.toString());			
			StringEntity entityLogin = new StringEntity(loginObj.toString(), "UTF-8");			
			entityLogin.setContentType("application/json");
			httpLoginPost.setEntity(entityLogin);			
			HttpClient httploginclient = new DefaultHttpClient();
			HttpResponse responseLogin = httploginclient.execute(httpLoginPost);;			
			if(responseLogin.getStatusLine().getStatusCode() == 200){				
				String result = EntityUtils.toString(responseLogin.getEntity());				
				String[] temp = result.split("\"ticket\":\""); 
				System.out.println(temp[1]);
				String[] temp1 = temp[1].split("\"");
				Ticket = temp1[0];
				System.out.println("ALF_TICKET:"+Ticket);
			}else{
				throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,"Alfresco Tokens are not valid.");
			}
		}catch(Exception e){
			
		}
		return Ticket;
	}
	
	public String publishToChannel(NodeRef assetRef,String target) throws URISyntaxException, IOException{
		props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
		String publishAPI_URI	= props.getProperty(PROP_PUBAPI_URI).trim();
		String alfTicket	= getAlfrescoTicket();	
		String host = props.getProperty(PROP_DAM_HOST).trim();
		int port = Integer.parseInt(props.getProperty(PROP_DAM_PORT).trim());
		List<BasicNameValuePair> qparams = new ArrayList<BasicNameValuePair>();
		qparams.add(new BasicNameValuePair("alf_ticket",alfTicket));	
		
		URI uri =  URIUtils.createURI(PROP_PROTOCOL, host, port, publishAPI_URI,URLEncodedUtils.format(qparams,"UTF-8"), null);
		
		System.out.println("URI is:"+uri.toString());
		HttpPost httpPost  = new HttpPost(uri);				
		HttpResponse response = null;
		String message = "";		
		try {
			JSONObject publishObj = new JSONObject();		
			JSONArray jsonNodesList	= new JSONArray();
			String strAssetRef = assetRef.toString();
			System.out.println("Asset for publish via api:"+strAssetRef);
			jsonNodesList.put(strAssetRef);	
			
			String channelNodeRef = getChannelRef(alfTicket,target);
			if(channelNodeRef.length() >0){
				publishObj.put("channelId",channelNodeRef);			
				publishObj.put("publishNodes",jsonNodesList);		
				String strJSON = publishObj.toString();		
				System.out.println("Post Data:"+strJSON);			
				
				StringEntity entity = new StringEntity(strJSON, "UTF-8");			
				entity.setContentType("application/json");
				httpPost.setEntity(entity);
				HttpClient httpclient = new DefaultHttpClient();
				System.out.println("Publish Asset to Queue...");
				response = httpclient.execute(httpPost);
				System.out.println("Response from Publishing Status Code:"+response.getStatusLine().getStatusCode());
				System.out.println("Response from Publishing Reason:"+response.getStatusLine().getReasonPhrase());				
			}else{							
				message = "No publishing channel found for:"+channelNodeRef;
			}		
			
			if(response.getStatusLine().getStatusCode() == 200){
				message = "Success";
			}else{
				message = response.getStatusLine().getReasonPhrase();								
			}	
		} catch (Exception e ) {			
			e.printStackTrace();
		}	
		return message;
	}
	public String getChannelRef(String alfTicket,String target) throws UnsupportedEncodingException {
		String 	ALFTICKET = alfTicket.trim();
		String  URL	= "http://localhost:8080/alfresco/service/api/publishing/channels?alf_ticket="+ALFTICKET;
		HttpClient httpclient1 = new DefaultHttpClient();
		HttpGet httpChannel = new HttpGet(URL);	
		String channelNodeRef	= "";
		try {
			HttpResponse responseChannels = httpclient1.execute(httpChannel);			
			System.out.println("Ticket Response:"+responseChannels.getStatusLine());
			if(responseChannels.getStatusLine().getStatusCode() == 200){				
				String result = EntityUtils.toString(responseChannels.getEntity());				
				JSONObject jsonObject	= new JSONObject(result);
				JSONObject jsonData		= jsonObject.getJSONObject("data");
				JSONArray jsonChannels		= jsonData.getJSONArray("publishChannels");
				System.out.println("No of channels present:"+jsonChannels.length());					
				for(int i = 0, size = jsonChannels.length(); i < size; i++){
				      JSONObject objectInArray = jsonChannels.getJSONObject(i);
				      String channel = (String) objectInArray.getString("name");				      
				      if(channel.equalsIgnoreCase(target)){				    	  
				    	  channelNodeRef = (String) objectInArray.getString("id");
				      }			      
				}
				System.out.println("Channel Node ref:"+channelNodeRef);				
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return channelNodeRef;
	}	
	
	public HttpResponse notifyUCMSMediaDownload(String ucmsID) throws JSONException, IOException{
		props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
		String hostURL = props.getProperty(PROP_UCMSNOTIFY_URL).trim();
		String hostURI = props.getProperty(PROP_UCMSNOTIFYDOWNLOAD_URI).trim();		
		String URL	= hostURL+ucmsID+hostURI;
		System.out.println("UCMS Create Asset Notify URL:"+URL);		
		HttpClient httpclient = new DefaultHttpClient();
		UCMSServiceHelper ucmService = new UCMSServiceHelper();
		HttpPost httpPost = ucmService.createHttpPostUCMSHeader(URL,UCMS_DOMAIN_IDENTIFIER);			
		response = httpclient.execute(httpPost);			
		return response;  
	}
	public HttpResponse notifyUCMSMediaPublish(String ucmsID, String provfiderID, double duration, boolean isKapx, String pubError) throws IOException, JSONException{
		System.out.println("BVC ID from Notify Method:"+provfiderID);
		System.out.println("Duration:"+duration);
		props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));		
		String hostURL = props.getProperty(PROP_UCMSNOTIFY_URL).trim();
		String hostURI = props.getProperty(PROP_UCMSNOTIFYPUBLISH_URI).trim();		
		String URL	= hostURL+ucmsID+hostURI;
		System.out.println("UCMS PUblish Asset Notify URL:"+URL);		
		StringEntity entityNotify = null;
		if(isKapx == false){
			int intDuration = (int) duration;
			JSONObject notifyObj	= new JSONObject();		
			notifyObj.put(PROP_BVCID, provfiderID.trim());		
			notifyObj.put(PROP_DURATION, intDuration);
			notifyObj.put(PROP_PUB_ERROR,pubError);
			System.out.println("JSON Properties for Notify:"+notifyObj.toString());		
			entityNotify = new StringEntity(notifyObj.toString(), "UTF-8");			
			entityNotify.setContentType("application/json");
		}else{
			int slideCount = (int) duration;
			JSONObject notifyObj	= new JSONObject();		
			notifyObj.put(PROP_KAPX_ID, provfiderID.trim());		
			notifyObj.put(PROP_KAPX_SLIDE_COUNT, slideCount);
			notifyObj.put(PROP_PUB_ERROR,pubError);
			System.out.println("JSON Properties for Notify:"+notifyObj.toString());		
			entityNotify = new StringEntity(notifyObj.toString(), "UTF-8");			
			entityNotify.setContentType("application/json");			
		}
		HttpClient httpclient = new DefaultHttpClient();
		UCMSServiceHelper ucmService = new UCMSServiceHelper();
		HttpPost httpPost = ucmService.createHttpPostUCMSHeader(URL,UCMS_DOMAIN_IDENTIFIER);		
		httpPost.setEntity(entityNotify);			
		response = httpclient.execute(httpPost);
		return response;  
	}
	/*
	public HttpResponse publishAsset(String ucmsID, String channel) throws IOException{
		props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
		String alfTicket	= getAlfrescoTicket();		
		String host = props.getProperty(PROP_DAM_HOST).trim();
		int port = Integer.parseInt(props.getProperty(PROP_DAM_PORT).trim());
		List<BasicNameValuePair> qparams = new ArrayList<BasicNameValuePair>();
		qparams.add(new BasicNameValuePair("alf_ticket",alfTicket));
		String UCMSPUBLISHAPI = "alfresco/service/asset/"+ucmsID+"/publish/"+channel;	
		HttpResponse response = null;
		try {
			URI uri =  URIUtils.createURI(PROP_PROTOCOL, host, port, UCMSPUBLISHAPI,URLEncodedUtils.format(qparams,"UTF-8"), null);
			HttpClient httpclient1 = new DefaultHttpClient();
			HttpGet httpPublish = new HttpGet(uri);				
			response = httpclient1.execute(httpPublish);				
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;  
	}*/
}

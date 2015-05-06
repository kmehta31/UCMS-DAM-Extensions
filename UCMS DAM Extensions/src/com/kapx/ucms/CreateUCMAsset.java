package com.kapx.ucms;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletResponse;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.collections.JsonUtils;
import org.apache.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.kapx.ucms.model.UCMSPublishingModel;
import com.kapx.ucms.publishing.brightcove.BrightcovePublishingHelper;

public class CreateUCMAsset extends DeclarativeWebScript {	
	private static final String PARAM_UCMS_ID = "id";
	private static final String PARAM_FILENAME = "fileName";
	private static final String PARAM_TITLE = "title";
	private static final String PARAM_DESCRIPTION = "description";
	private static final String PARAM_TAGS = "tags";
	private static final String PARAM_PRODUCTLINE = "productLine";
	private static final String PARAM_CONTENTGROUP = "contentGroup";
	private static final String PARAM_CUEPOINTS = "cuePoints";
	private static final String PARAM_RESOURCE_URL = "resourceURL";
	private static final String PARAM_RESOURCE_TYPE = "resourceType";
	private static final String PARAM_DURATIONSECONDS = "durationSeconds";
	private static final String PARAM_TARGET = "publishTarget";	
	private static final String PROP_SITE_MEDIA_PATH = "sitemediapath";	
	private static final String PROP_RESOURCE_TYPE = "ucmsresourceType";
	private ServiceRegistry registry; 
	private Repository repository;
	private BrightcovePublishingHelper publishingHelper;	
	
	// for Spring injection
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	// for Spring injection 
	public void setServiceRegistry(ServiceRegistry registry) {
		this.registry = registry;
	}
	
	public String ERROR = "";
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {	      
		//put all the objects that you need in renditions to the model map
		Map<String, Object> model = new HashMap<String, Object>();		
	    JSONObject json;	
	    Properties props = new Properties();
        try
        {
        	System.out.println("Debug from JAVA Webscript on...\\."); 
        	props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));			
        	json = getJSONObject(req);
        	String strUCMSID = "";
        	String strFileName	= "";
        	String strTitle	= "";
        	String strDescription	= "";        	
        	List<String> strTags	= null;
        	String strProductLine	= "";
        	String strContentGroup	= "";
        	String strCuePoints	= "";
        	String strResourceURL	= "";
        	String strResourceType	= "";
        	int intDuractionSecs	= 0;
        	String strPublishTarget	= ""; 
        	boolean ifLargeFile = false;
        	Map<QName, Serializable> nodeProps = new HashMap<QName, Serializable>();
        	
        	if(json.has(PARAM_UCMS_ID)){
        		strUCMSID =	json.getString(PARAM_UCMS_ID).trim();
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_ID, strUCMSID);
        	}
        	if(json.has(PARAM_FILENAME)){
        		strFileName =	json.getString(PARAM_FILENAME).trim();
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_FILENAME, strFileName);
        	}
        	if(json.has(PARAM_TITLE)){
        		strTitle =	json.getString(PARAM_TITLE);
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_TITLE, strTitle);
        		nodeProps.put(ContentModel.PROP_TITLE, strTitle);
        	}
        	if(json.has(PARAM_DESCRIPTION)){
        		strDescription =	json.getString(PARAM_DESCRIPTION);
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_DESC, strDescription);
        	}
        	if(json.has(PARAM_TAGS)){
        		//JSONArray jsonArray = json.getJSONArray(PARAM_TAGS);
        		strTags = (List<String>) JsonUtils.toListOfStrings(json.getJSONArray(PARAM_TAGS));       		
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_TAGS, (Serializable) strTags);
        		
        	}
        	if(json.has(PARAM_PRODUCTLINE)){
        		strProductLine =	json.getString(PARAM_PRODUCTLINE);
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_PRODUCTLINE, strProductLine);
        	}
        	if(json.has(PARAM_CONTENTGROUP)){
        		strContentGroup =	json.getString(PARAM_CONTENTGROUP);
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_CONTENTGRP, strContentGroup);
        	}
        	if(json.has(PARAM_CUEPOINTS)){
        		strCuePoints =	json.getString(PARAM_CUEPOINTS);
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_CUEPOINTS, strCuePoints);
        	}        	
        	if(json.has(PARAM_RESOURCE_URL)){
        		strResourceURL =	json.getString(PARAM_RESOURCE_URL);
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_RESOURCEURL, strResourceURL);
        	}
        	if(json.has(PARAM_RESOURCE_TYPE)){
        		strResourceType = json.getString(PARAM_RESOURCE_TYPE);        		
        	}else{
        		//default to public
        		strResourceType = props.getProperty(PROP_RESOURCE_TYPE);
        	}
        	if(json.has(PARAM_DURATIONSECONDS)){
        		intDuractionSecs =	json.getInt(PARAM_DURATIONSECONDS);
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_DURATIONSECS, intDuractionSecs);
        	}
        	if(json.has(PARAM_TARGET)){
        		strPublishTarget =	json.getString(PARAM_TARGET);   
        		if(strPublishTarget.equalsIgnoreCase("kapx")){
        			strPublishTarget = "KAPx";
        		}
        		if(strPublishTarget.equalsIgnoreCase("BrightcoveFTP")){
        			strPublishTarget = "BrightcoveFTP";
        			ifLargeFile = true;
        		}
        		if(strPublishTarget.equalsIgnoreCase("BrightcoveHTTP")){
        			strPublishTarget = "BrightcoveHTTP";
        		}
        		nodeProps.put(UCMSPublishingModel.PROPERTY_UCM_PUBLISHTARGET, strPublishTarget);
        	}
        	if(strUCMSID.length() > 0){
        		String strCreateFileName = "";
        		int lastIndexofExtension = 0;
        		String extension = "";
        		if(strFileName != null && strFileName.length() >0){
        			lastIndexofExtension = strFileName.lastIndexOf(".");
        			extension	= strFileName.substring(lastIndexofExtension,strFileName.length());
        		}        			
        		strCreateFileName = strUCMSID+extension;        		
        		NodeRef	assetRef	= 	createAsset(strCreateFileName, nodeProps, props);
        		String strAssetRef	=  assetRef.toString();        		
        		model.put("ucmsMediaRef", strAssetRef);        		
	        	if(json.getString(PARAM_RESOURCE_URL).length() > 0){        		
	        		System.out.println("Resource URL:"+strResourceURL);	        			        		
	        		try
	                {  
	        			MediaDownloadHelper mediaDownloadHelper = 
	        					new MediaDownloadHelper(strResourceURL,getNodeService(), getContentService(), assetRef, registry);	        			
	        			if(strResourceURL.toLowerCase().contains(HttpUCMSClient.AMAZON_DOMAIN_IDENTIFIER) && strResourceType.equalsIgnoreCase("private")){
	        				mediaDownloadHelper.downloadFromS3(ifLargeFile,strResourceType);	        				
	        			}else if(strResourceURL.toLowerCase().contains(HttpUCMSClient.UCMS_DOMAIN_IDENTIFIER) || strResourceURL.toLowerCase().contains(HttpUCMSClient.UCMS_DOMAIN_IDENTIFIER1)){	        				
	        				mediaDownloadHelper.downloadFromURL(ifLargeFile);	        				
	        			}else{
	        				System.out.println("Download Media from Public Site Else block");	        				
	        				mediaDownloadHelper.downloadFromURL(ifLargeFile);
	        			}
	        			
	        			HttpUCMSClient ucmsClient = new HttpUCMSClient();
	        			String hostURL = "";
	        			if(strResourceURL.contains(HttpUCMSClient.UCMS_DOMAIN_IDENTIFIER1)){
	        				hostURL = props.getProperty(HttpUCMSClient.PROP_UCMSNOTIFY_URL_NEW).trim();
	        			}else{
	        				hostURL = props.getProperty(HttpUCMSClient.PROP_UCMSNOTIFY_URL).trim();
	        			}
	        			HttpResponse httpResponse = ucmsClient.notifyUCMSMediaDownload(strUCMSID,hostURL);
	        			System.out.println("Create Notify Response:"+httpResponse.getStatusLine().getStatusCode());
	        			if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
	        	            System.out.println("UCMS Notification Successful");		                        
	        	        } else{
	        	        	System.out.println("UCMS Notification failed."+httpResponse.getStatusLine().getReasonPhrase());
	        			}
	        			
	                }catch (Exception e){
	                     throw new AlfrescoRuntimeException(e.getLocalizedMessage());
	                 }       	
	        	
	            }else{
	            	ERROR	= ERROR + "Resource URL Missing\n";
	        		model.put("error", ERROR);
	            }      
        	}
	   }catch (JSONException | IOException jErr){
           throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
                   "Unable to parse JSON POST body: " + jErr.getMessage());
       } 
        return model;
	}
	private JSONObject getJSONObject(WebScriptRequest req){
		JSONObject json = null;
		Object jsonO 	= req.parseContent();		
		if (jsonO instanceof JSONObject && jsonO != null)
		{
			json = (JSONObject)jsonO;
		}
		return json;	     
	}
	//createAsset method
	private NodeRef createAsset(String fileName, Map<QName, Serializable> nodeProps, Properties props){
		// Create a map to contain the values of the properties of the node       
        NodeService	nodeService =	getNodeService();              
        // build a path elements list
     	List<String> pathElements = new ArrayList<String>();
     	StringTokenizer tokenizer = new StringTokenizer(props.getProperty(PROP_SITE_MEDIA_PATH).trim(), "/");
     	while (tokenizer.hasMoreTokens()) {
     			String childName = tokenizer.nextToken();
     			pathElements.add(childName);
     	}
 		NodeRef parentNodeRef = null;
 		NodeRef mediaNodeRef = null;
 		try {
 			NodeRef companyHomeRef = repository.getCompanyHome();
 			parentNodeRef = registry.getFileFolderService().resolveNamePath(companyHomeRef, pathElements).getNodeRef();			
	 		if (parentNodeRef == null) {
	 			throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST , "Unable to locate Site Default path");
	 		}
	 		List<String> pathElements1 = new ArrayList<String>();
	 		pathElements1.add(fileName);
	 		try{
	 			mediaNodeRef = getFileFolderService().resolveNamePath(parentNodeRef, pathElements1).getNodeRef(); 
	 			if (mediaNodeRef != null) {
	 				throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST , "File with same name already Exist.");
	 			}			
	 		}catch (Exception ex) {
	 			System.out.println("Create Asset.........:");	 			
	 			nodeProps.put(ContentModel.PROP_NAME, fileName);	 					
	 				mediaNodeRef = nodeService.createNode(
		        				parentNodeRef, 
		                        ContentModel.ASSOC_CONTAINS, 
		                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fileName),
		                        UCMSPublishingModel.TYPE_DIGITAL_ASSET, 
		                        nodeProps).getChildRef();	 				
	 				System.out.println("Asset created successfully and node is:"+mediaNodeRef);	 			
	 		}
 			return mediaNodeRef; 		
 		}catch (Exception ex) {
 			throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST , ex.getMessage());
 		}
	}
	private ContentService getContentService() { 
		return this.registry.getContentService();
	}
	private NodeService getNodeService() {
		return this.registry.getNodeService();
	}
	private FileFolderService getFileFolderService() {
		return this.registry.getFileFolderService();
	}	
}

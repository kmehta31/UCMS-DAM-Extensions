package com.kapx.ucms.action;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.kapx.ucms.model.UCMSPublishingModel;
import com.kapx.ucms.publishing.brightcove.ClientHttpRequest;

public class CorrectFTPAssetsActionExecutor extends ActionExecuterAbstractBase
{
   public static final String NAME = "correctFTPAssets";      
   public static final String WRITETOKENQA = "";
   private NodeService nodeService;
   private ServiceRegistry registry;
   private Repository repository;
   
   public void setNodeService(NodeService nodeService){
      this.nodeService = nodeService;
   }   	
   // for Spring injection
   public void setRepository(Repository repository) {
		this.repository = repository;
   }
   // for Spring injection 
   public void setServiceRegistry(ServiceRegistry registry) {
	   this.registry = registry;
   }	
   protected void executeImpl(Action action, NodeRef actionedUponNodeRef){
	   ResultSet results = null;
	   try{	
		   	
		   	NodeRef assetRef = null;
		   	Properties props = new Properties();
	 	   	props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
	 	   	String BVREADTOKEN = props.getProperty("bcreadtoken").trim();
	 	   	
	 	   	int i = 0;
	        int j = 0;		   	
	        SearchParameters sp = new SearchParameters();	   	
		   	sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
	    	sp.setLanguage(SearchService.LANGUAGE_LUCENE);    	
	 	   	sp.setQuery("@ucm\\:publishTarget:\"BrightcoveFTP\" AND NOT ASPECT:\"pub:published\"");	  	    	     
	        results = registry.getSearchService().query(sp);	        
	        System.out.println("Step 1: Total FTP Records found:"+results.length());	        
	        for(ResultSetRow row : results){	        	
		    	assetRef= row.getNodeRef();	        	
		    	String cmsBVCID = (String) nodeService.getProperty(assetRef, UCMSPublishingModel.PROPERTY_BVC_ID);
		    	String UCMSID = (String) nodeService.getProperty(assetRef, UCMSPublishingModel.PROPERTY_UCM_ID);
			   	cmsBVCID = cmsBVCID.trim();
			   	UCMSID = UCMSID.trim();		  		   			  		   	
			   	String URL = "http://api.brightcove.com/services/library?command=find_video_by_reference_id&reference_id="+UCMSID+"&video_fields=id,name&token="+BVREADTOKEN; 				   	
			   	HttpClient httpClient = new DefaultHttpClient();
			   	HttpGet httpGet = new HttpGet(URL.trim());  		
			   	HttpResponse response = httpClient.execute(httpGet);		  		   	
				if(response.getStatusLine().getStatusCode() == 200){				
					String result = EntityUtils.toString(response.getEntity());				
					JSONObject jsonObject	= new JSONObject(result);
					String BVCID = (String) jsonObject.getString("id").trim();
					if(cmsBVCID.equalsIgnoreCase(BVCID)){							
						i++;
					}else{
						j++;
						nodeService.setProperty(assetRef, UCMSPublishingModel.PROPERTY_BVC_ID,BVCID);
						System.out.println("UCMS ID updated for:"+UCMSID+" from cmsBVCID:"+cmsBVCID+" to BVCID:"+BVCID);
					}					
					//updateBrightVideo(BVCID,assetRef);
				}
			}
	        System.out.println("Total match found:"+i);
			System.out.println("Total records updated:"+j);
			System.out.println("Total Records:"+(i+j));
		  	   
	   }catch (Exception e) {				
		   e.printStackTrace();
	   }
  		   
  }
   public void updateBrightVideo(String videoID, NodeRef nodeToPublish) throws MalformedURLException, IOException, URISyntaxException, JSONException{
	   System.out.println("Update Video in Brightcove:"+videoID);
	   Object[] params = getWriteParams(videoID);
	   if(params!=null){
		   String host ="api.brightcove.com";
		   int port = 80;	   
		   URI uri = URIUtils.createURI("http", host, port, "services/post",null, null); 
           InputStream in = ClientHttpRequest.post(new java.net.URL(uri.toString()),params);                   
           int bytesRead = 0;
           byte[] buffer = new byte[1024];
           ByteArrayOutputStream ret = new ByteArrayOutputStream();
           while ((bytesRead = in.read(buffer)) > 0){
        	   ret.write(buffer, 0, bytesRead);
           }                   
           String response = new String(ret.toByteArray());
           
           JSONObject jObject = new JSONObject(response);
           if(jObject.isNull("error")){
				JSONObject jsonData	= jObject.getJSONObject("result");
				String id	= jsonData.getString("id");
				System.out.println("Brightcove Record updated. Id Returned from Results:"+id);				
           }else{
	        	throw new AlfrescoRuntimeException("Error in response from Brightcove on update:"+response);
           }                                  
       }
   }
   
   public Object[] getWriteParams(String strBVCID) throws IOException{
	   String strJSONRequestParams = "";
	   Object[] params = null;	   
	   if(strBVCID.length() > 0){   			
		   long bvcid = Long.parseLong(strBVCID);   		        
		   strJSONRequestParams	= "{" +
   	       		"\"method\":\"update_video\"" +
   	       		",\"params\":{" +
   	               	"\"token\":\""+WRITETOKENQA+"\", " +
   	               	"\"video\":{\"id\":"+bvcid+"}," +
   	               	"\"create_multiple_renditions\":true}} " +  		    	                      
   	               "}}";
		   params = new Object[] { "JSON-RPC", strJSONRequestParams};
   		}  		
   		System.out.println("Update Video JSON:"+strJSONRequestParams);          
   		return params;
   }
  
   
   @Override
   protected void addParameterDefinitions(List<ParameterDefinition> paramList)
   {
      // there are no parameters
   }



}
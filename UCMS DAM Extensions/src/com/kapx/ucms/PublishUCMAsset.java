package com.kapx.ucms;


import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletResponse;


import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;

import com.kapx.ucms.model.UCMSPublishingModel;


public class PublishUCMAsset extends DeclarativeWebScript {		
	private ServiceRegistry registry; 
	// for Spring injection 
	public void setServiceRegistry(ServiceRegistry registry) {
		this.registry = registry;
	}	
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {	      
		//put all the objects that you need in renditions to the model map
		Map<String, Object> model = new HashMap<String, Object>();		
	    Properties props = new Properties(); 
	    String message = "";
	    ResultSet results = null;
	    String UCMSID = "";
	    NodeService nodeService = this.registry.getNodeService();
	    NodeRef assetRef = null; 	  
    	try {   		
	    	HttpUCMSClient client = new HttpUCMSClient();
	    	Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
	    	UCMSID = templateArgs.get("ucmsid");
	    	String strPublishTarget = templateArgs.get("publishtarget");
	    	
	    	System.out.println("UCMS ID:"+UCMSID);
	    	System.out.println("Publish Target:"+strPublishTarget);
	
	    	SearchParameters sp = new SearchParameters();
	    	sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
	    	sp.setLanguage(SearchService.LANGUAGE_LUCENE);
	    	sp.setQuery("@ucm\\:id:\""+UCMSID+"\" AND NOT ASPECT:\"pub:published\"");	  	    	     
	        results = registry.getSearchService().query(sp);	        
	        for(ResultSetRow row : results)
	        {
	        	assetRef= row.getNodeRef();                
	        }
	        if(assetRef!=null){	        	
	        	//nodeService.setProperty(assetRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, "");		
	        	message = client.publishToChannel(assetRef,strPublishTarget);
	        	if(message.equalsIgnoreCase("success")){
	        		message = "Publishing to Channel is successful";
	        		System.out.println(message);
	        	}else{
	        		 throw new AlfrescoRuntimeException(message);
	        	}	        	
		    		
	    	}else{
	    		System.out.println("No record found for UCMS ID:"+UCMSID);
	    		message = "No node found for UCMS ID:"+UCMSID;
	    		throw new AlfrescoRuntimeException(message);
	    	}	         
    	} catch (Exception e) {
    		if(assetRef!=null){
    			nodeService.setProperty(assetRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, e.getLocalizedMessage());
    		}
    		throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST , e.getLocalizedMessage());			
		}finally
        {
            if(results != null)
            {
                results.close();
            }
        }
    	model.put("ucmsMessage", message);      	
        return model;
	}
}

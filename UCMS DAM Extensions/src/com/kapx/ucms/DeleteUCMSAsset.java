package com.kapx.ucms;
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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;



public class DeleteUCMSAsset extends DeclarativeWebScript {	
	private static final String PROP_SITE_MEDIA_DELETED_PATH = "sitemediadeletedpath";	
	private ServiceRegistry registry;
	private Repository repository;	
	// for Spring injection
	public void setRepository(Repository repository) {
		this.repository = repository;
	}
	// for Spring injection 
	public void setServiceRegistry(ServiceRegistry registry) {
		this.registry = registry;
	}	
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {	      
		//put all the objects that you need in renditions to the model map
		Map<String, Object> model = new HashMap<String, Object>();	    
	    String message = "";
	    ResultSet results = null;
	    String UCMSID = "";
	    NodeService nodeService = this.registry.getNodeService();
	    NodeRef assetRef = null;	    
    	try {   		
    		Properties props = new Properties();
    	    props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));    	    
    	    List<String> pathElements = new ArrayList<String>();
    	    StringTokenizer tokenizer = new StringTokenizer(props.getProperty(PROP_SITE_MEDIA_DELETED_PATH).trim(), "/");
         	while (tokenizer.hasMoreTokens()) {
         			String childName = tokenizer.nextToken();
         			pathElements.add(childName);
         	}
         	NodeRef companyHomeRef = repository.getCompanyHome();
         	NodeRef parentNodeRef = registry.getFileFolderService().resolveNamePath(companyHomeRef, pathElements).getNodeRef();	
         	if (parentNodeRef == null) {
	 			throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST , "Unable to locate Site Default path");
	 		}
         	
	    	Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
	    	UCMSID = templateArgs.get("ucmsid").trim();   	
	    	
	    	SearchParameters sp = new SearchParameters();
	    	sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
	    	sp.setLanguage(SearchService.LANGUAGE_LUCENE);
	    	sp.setQuery("@ucm\\:id:'"+UCMSID+"' AND NOT ASPECT:\"pub:published\"");	  	    	     
	        results = registry.getSearchService().query(sp);	     
	        System.out.println("To be deleted Item founds for UCMSID:"+UCMSID+" ="+results.length());
	        String assetName = null;
	        for(ResultSetRow row : results)
	        {
	        	assetRef= row.getNodeRef();
	        	assetName = (String) nodeService.getProperty(assetRef, ContentModel.PROP_NAME);
	        }
	        if(assetRef!=null){	        	
	        	nodeService.moveNode(assetRef, parentNodeRef, ContentModel.ASSOC_CONTAINS, 
	        			QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, assetName));	        	
	        	message = "Media item moved to deleted space";
	    	}else{
	    		message = "No node found for UCMS ID:"+UCMSID;
	    		
	    	}	         
    	} catch (Exception e) {
    		message = e.getLocalizedMessage();
    		System.out.println("Error is:"+message);
    		throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST , message);			
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

package com.kapx.ucms.publishing.brightcove;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.node.encryption.MetadataEncryptor;
import org.alfresco.repo.publishing.AbstractChannelType;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.BrightcoveFTPPublishingModel;
import com.kapx.ucms.model.BrightcovePublishingModel;
import com.kapx.ucms.model.UCMSPublishingModel;

/**
 * Channel definition for publishing/unpublishing Video content to Brightcove Server
 * 
 * @author Kalpesh
 * 
 */
public class BrightcoveFTPChannelType extends AbstractChannelType
{
    private static final Log log = LogFactory.getLog(BrightcoveFTPChannelType.class);
    public static final String ID = BrightcoveFTPPublishingModel.PUBLISH_FTP_ID;
    private static final int BVCDESCRIPTIONLENGTH = 250;
    private ContentService contentService;    
    private Set<String> supportedMimeTypes = BrightcovePublishingModel.DEFAULT_SUPPORTED_MIME_TYPES;	
    private MetadataEncryptor encryptor;    
    
   	public void setEncryptor(MetadataEncryptor encryptor){
           this.encryptor = encryptor;
    }
    
	public void setContentService(ContentService contentService) {
	        this.contentService = contentService;
	}	

    public void setSupportedMimeTypes(Set<String> mimeTypes){
        supportedMimeTypes = Collections.unmodifiableSet(new TreeSet<String>(mimeTypes));
    }
  
    @Override
    public boolean canPublish(){
        return true;
    }
    
    @Override
    public boolean canPublishStatusUpdates(){
        return false;
    }
    
    @Override
    public boolean canUnpublish(){
        return false;
    }
    
    @Override
    public QName getChannelNodeType(){
        return BrightcoveFTPPublishingModel.TYPE_FTP_DELIVERY_CHANNEL;
    }
    
    @Override
    public String getId(){
        return ID;
    }

    @Override
    public Set<String> getSupportedMimeTypes(){
        return supportedMimeTypes;
    }

    @SuppressWarnings("unchecked")
	@Override
    public void publish(NodeRef nodeToPublish, Map<QName, Serializable> channelProperties){    
    	NodeService nodeService = getNodeService();      
        NodeRef publishNodeRef	= getPublishingNodeRef(nodeToPublish,nodeService);        
        boolean isError = false;
    	String strError = "";
    	String ucmsID = "";
        try{
        	System.out.println("Publish to BrightcoveFTP...");
	        String fileName = (String) nodeService.getProperty(publishNodeRef, ContentModel.PROP_NAME);	        
	        String description = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_DESC);
	        ucmsID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);	        
	        String title = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_TITLE);
	        List<String> tagsList = (ArrayList<String>) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_TAGS);	        
	        String tags = "";
	        if(tagsList!=null){
	        	if(tagsList.size()>0){
	        		tags = tagsList.get(0);
	        		System.out.println("Tags:"+tags);
	        	}
	        }
	        
	        String shortDesc = "";
	        Map<String, Object> paramMap	= new HashMap();
	        paramMap.put("filename", fileName);
	        if(ucmsID != null){
	        	paramMap.put("referenceId",ucmsID);        	
	        }	        
	        if(description != null && description.length() >0){
	        	if(description.length() > BVCDESCRIPTIONLENGTH){
	        		description = description.substring(0,BVCDESCRIPTIONLENGTH);
	        	}
	        	paramMap.put("shortDescription",description);
	        	shortDesc = description;
	        }else{	        	
	        	paramMap.put("shortDescription",fileName);
	        	shortDesc=fileName; 
	        }	        
	        ContentReader reader = contentService.getReader(publishNodeRef, ContentModel.PROP_CONTENT);
	        if (reader.exists()){
	            File contentFile = null;           
	            if (FileContentReader.class.isAssignableFrom(reader.getClass())){
	            	 System.out.println("Upload Content....");
	                // Grab the content straight from the content store if we can...            	
	            	contentFile = ((FileContentReader) reader).getFile();
	            	BrightcoveFTPManager ftpManager = new BrightcoveFTPManager(channelProperties,encryptor);
	            	ftpManager.upload(contentFile, fileName, title, ucmsID, shortDesc, tags);
	            	System.out.println("Final FTP Upload Successful");
	            }	                                    
	        }            
        }catch(Exception e){
			nodeService.setProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, e.getLocalizedMessage());
			isError = true;
			strError = e.getLocalizedMessage();			
        }finally{				
			if(isError == true){					
				nodeService.setProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, strError);
				HttpUCMSClient ucmsClient = new HttpUCMSClient();
    			try {
    				//Notify UCMS App for Publishing Error
					HttpResponse httpResponse = ucmsClient.notifyUCMSMediaPublish(ucmsID,"",0,false,strError);
					if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
	    	            System.out.println("UCMS Notification for Publish Error Successful");		                        
	    	        } else{
	    	        	System.out.println("UCMS Notification for Publish Error failed:"+httpResponse.getStatusLine().getReasonPhrase());
	    			}
				} catch (Exception e) {						
					e.printStackTrace();
				}	    	
			}
		} 
        
    }   
    
    public NodeRef getPublishingNodeRef(NodeRef nodeToPublish, NodeService nodeService){
    	List<AssociationRef> listTargetChilds = nodeService.getTargetAssocs(nodeToPublish,UCMSPublishingModel.PROPERTY_PUB_SOURCE);       
    	NodeRef srcfileRef = null;  
    	NodeRef tgtfileRef = null;       
        for (AssociationRef child : listTargetChilds) {        	
        	srcfileRef = child.getSourceRef();  
        	tgtfileRef	= child.getTargetRef();        	    	            
        }
        return tgtfileRef;
    }  
}

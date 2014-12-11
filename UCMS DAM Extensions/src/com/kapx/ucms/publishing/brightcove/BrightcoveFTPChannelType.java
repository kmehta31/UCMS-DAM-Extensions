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

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.extensions.webscripts.WebScriptException;

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
    private BrightcovePublishingHelper publishingHelper;
    private ContentService contentService;    
    private Set<String> supportedMimeTypes = BrightcovePublishingModel.DEFAULT_SUPPORTED_MIME_TYPES;	
    private MetadataEncryptor encryptor;    
    
   	public void setEncryptor(MetadataEncryptor encryptor){
           this.encryptor = encryptor;
    }
    
   	public void setPublishingHelper(BrightcovePublishingHelper brightcovePublishingHelper){
        this.publishingHelper = brightcovePublishingHelper;
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
        return true;
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
    	String strBVCID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_BVC_ID); 
    	String strUCMSID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);    	
    	if(nodeService.hasAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_BRIGHTCOVE_UPDATE)){
    		try{
    			String publishError = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR);	        	
	        	HttpUCMSClient ucmsClient = new HttpUCMSClient();
	        	HttpResponse httpResponse = null;	        	
    			if(StringUtils.isNotEmpty(strBVCID)){    				
	        		//Notify UCMS for Publish Successful for BrightcoveFTP
    				Double durationObj = (Double) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_DURATIONSECS); 
    	 	        //Handle null pointer exception 
    	 	        if(durationObj==null){
    	 	        	durationObj = 0.0;
    	 	        }
    	 	        double duration = durationObj;
	        		httpResponse = ucmsClient.notifyUCMSMediaPublish(strUCMSID,strBVCID,duration,false,"");	        		
	        	}else{
	        		//Notify UCMS for Publish Failure for BrightcoveFTP        		
	        		httpResponse = ucmsClient.notifyUCMSMediaPublish(strUCMSID,"",0,false,publishError);
	        	}	
				
				if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
		            System.out.println("UCMS Notification Successful");		                        
		        } else{
		        	System.out.println("UCMS Notification failed."+httpResponse.getStatusLine().getReasonPhrase());
				}
				nodeService.removeAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_BRIGHTCOVE_UPDATE);
				if(nodeService.hasAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH)){
		           	nodeService.removeAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH);
		        }
        	}catch(Exception e){
        		System.out.println("UCMS Notification Error"+e);        		
        	}        	
    	}else{
    		if(StringUtils.isEmpty(strBVCID)){
    			boolean isError = false;
    	    	String strError = "";
    	    	String ucmsID = "";
		        try{
		        	System.out.println("Publish to BrightcoveFTP for Create...");
			        String fileName = (String) nodeService.getProperty(publishNodeRef, ContentModel.PROP_NAME);	        
			        String description = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_DESC);
			        ucmsID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);	        
			        String title = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_TITLE);
			        List<String> tagsList = (ArrayList<String>) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_TAGS);	        
			                
			        String shortDesc = "";
			        Map<String, Object> paramMap	= new HashMap<String, Object>();
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
			            	ftpManager.upload(contentFile, fileName, title, ucmsID, shortDesc, tagsList);
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
    
    @Override
    public void unpublish(NodeRef nodeToUnpublish, Map<QName, Serializable> channelProperties)
    {
    	NodeService nodeService = getNodeService();
    	String strError = "";
    	boolean isError = false;
    	NodeRef srcfileRef = null;  
    	NodeRef tgtfileRef = null; 
    	NodeRef fileRef	= null;  
    	String strUCMSID = "";
    	try{   	
	    	List<AssociationRef> listTargetChilds = nodeService.getTargetAssocs(nodeToUnpublish,UCMSPublishingModel.PROPERTY_PUB_SOURCE);        
	        for (AssociationRef child : listTargetChilds) {        	
	        	srcfileRef = child.getSourceRef();  
	        	tgtfileRef	= child.getTargetRef();
	        	fileRef	= tgtfileRef;        	      
	        }
	        strUCMSID = (String) nodeService.getProperty(fileRef, UCMSPublishingModel.PROPERTY_UCM_ID);
	        String strBVCID	=	(String)nodeService.getProperty(fileRef, UCMSPublishingModel.PROPERTY_BVC_ID);	        
	        long bvcid = 0;
	        if(strBVCID!=null && strBVCID.length()>0){
	        	bvcid	= Long.parseLong(strBVCID);	        	
	        }else{	        	
	        	isError = true;
	        	strError = "No Brightcove ID found for the node to Delete"; 
	        	throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST , strError);	
	        }
	        BrightcoveFTPManager ftpManager = new BrightcoveFTPManager(channelProperties,encryptor);
	        String writeToken	= ftpManager.getWriteTokenChannelProperties(channelProperties); 
	    	Object[] params = getDeleteParams(bvcid, channelProperties,writeToken);    	
	    	if(params!=null){
	         	URI uriPut;					
				uriPut = ftpManager.getURIFromChannelProperties(channelProperties);
				InputStream in = ClientHttpRequest.post(new java.net.URL(uriPut.toString()), params);
				
				int bytesRead = 0;
				byte[] buffer = new byte[1024];
				ByteArrayOutputStream ret = new ByteArrayOutputStream();
	         
				while ((bytesRead = in.read(buffer)) > 0){
					ret.write(buffer, 0, bytesRead);
				}			            
				String response = new String(ret.toByteArray());			      
				updateNodePropertiesAfterUnPublish(response, nodeService, fileRef);                       	
			}else{
				isError = true;
				strError = "No WRITE TOKEN FOR Brightcove Provided";	         	
	         }
    	}catch(Exception e){			
			isError = true;
			strError = e.getLocalizedMessage();			
        }finally{				
			if(isError == true){			
				HttpUCMSClient ucmsClient = new HttpUCMSClient();
    			try {
    				//Notify UCMS App for UnPublishing Error
					HttpResponse httpResponse = ucmsClient.notifyUCMSMediaUnPublish(strUCMSID,"fail",strError);
					if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
	    	            System.out.println("UCMS Notification for Publish Error Successful for:"+strUCMSID);		                        
	    	        } else{
	    	        	System.out.println("UCMS Notification for Publish Error failed:"+httpResponse.getStatusLine().getReasonPhrase());
	    			}
				} catch (Exception e) {						
					e.printStackTrace();
				}	    	
			}
		}
    	if(isError==false){ 	            	
 	        HttpUCMSClient ucmsClient = new HttpUCMSClient(); 			
 			try {
 				HttpResponse httpResponse = ucmsClient.notifyUCMSMediaUnPublish(strUCMSID,"success",strError);	
 				if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
 					System.out.println("UCMS Notification Successful for Unpublishing Asset:"+strUCMSID);		                        
 				} else{
 					System.out.println("UCMS Notification for Unpublished failed."+httpResponse.getStatusLine().getReasonPhrase());
 				}
 			} catch (IOException | JSONException e ) {			
 				e.printStackTrace();
 			} 		
        }
    	    
    }
    
    public Object[] getDeleteParams(Long fileId, Map<QName, Serializable> channelProperties, String writeToken){    	   	
    	if(StringUtils.isEmpty(writeToken)){
    		return null;
    	}else{    		
    		String strJSONRequestParams	= "{" +
        		"\"method\":\"delete_video\"" +
        		",\"params\":{" +
                	"\"token\":\""+writeToken+"\", " +
                	"\"video_id\":"+fileId+", " +                    
                "}}";
    		System.out.println("JSON String:"+strJSONRequestParams);
    		Object[] params;            
            params = new Object[] { "JSON-RPC", strJSONRequestParams };            
    		return params;    		
    	}
    }
    
    public void updateNodePropertiesAfterUnPublish(String resp,NodeService nodeService,NodeRef nodeToPublish){   	
		try {    	
	        JSONObject jObject = new JSONObject(resp);
	        if(jObject.isNull("error")){
	        	//delete node in Alfresco after successful unpublish
	        	nodeService.deleteNode(nodeToPublish);	           	           
	        }else{
	        	throw new AlfrescoRuntimeException("UnPublishing Error from Brightcove:"+resp);                	
	        }  
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			throw new AlfrescoRuntimeException("Error Creating JSON Response Object. JSON String:"+resp);
		}    
    } 
}

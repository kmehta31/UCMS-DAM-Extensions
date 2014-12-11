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
import com.kapx.ucms.model.BrightcovePublishingModel;
import com.kapx.ucms.model.UCMSPublishingModel;

/**
 * Channel definition for publishing/unpublishing Video content to Brightcove Server
 * 
 * @author Kalpesh
 * 
 */
public class BrightcoveChannelType extends AbstractChannelType
{
    private static final Log log = LogFactory.getLog(BrightcoveChannelType.class);
    public static final String ID = BrightcovePublishingModel.PUBLISH_ID;    
    public static final String ERROR = "ERROR";       
    public static final int BVCDESCRIPTIONLENGTH = 250;
    public static final long FILESIZELIMIT = 2147483647;
    private BrightcovePublishingHelper publishingHelper;
    private ContentService contentService;    
    private Set<String> supportedMimeTypes = BrightcovePublishingModel.DEFAULT_SUPPORTED_MIME_TYPES;	
	public void setContentService(ContentService contentService) {
	        this.contentService = contentService;
	}	

    public void setSupportedMimeTypes(Set<String> mimeTypes){
        supportedMimeTypes = Collections.unmodifiableSet(new TreeSet<String>(mimeTypes));
    }

    public void setPublishingHelper(BrightcovePublishingHelper brightcovePublishingHelper){
        this.publishingHelper = brightcovePublishingHelper;
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
        return BrightcovePublishingModel.TYPE_DELIVERY_CHANNEL;
    }
    
    @Override
    public String getId(){
        return ID;
    }

    @Override
    public Set<String> getSupportedMimeTypes(){
        return supportedMimeTypes;
    }

    @Override
    public void publish(NodeRef nodeToPublish, Map<QName, Serializable> channelProperties){
    	NodeService nodeService = getNodeService(); 
    	boolean isError = false;
    	String strError = "";
        ContentReader reader = contentService.getReader(nodeToPublish, ContentModel.PROP_CONTENT);        
        NodeRef publishNodeRef	= getPublishingNodeRef(nodeToPublish,nodeService);
        
        String fileName = (String) nodeService.getProperty(publishNodeRef, ContentModel.PROP_NAME);
        String description = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_DESC);
        String ucmsID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);
        List<String> tagsList =  (ArrayList<String>) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_TAGS);   
        String strUCMSID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);
        String strBVCID = "";       
        
        System.out.println("Description is:"+description);
        System.out.println("UCMS ID:"+ucmsID);
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
        }else{
        	System.out.println("Short Desc:"+fileName);
        	paramMap.put("shortDescription",fileName);
        }
        if(tagsList !=null){
        	if(tagsList.size()>0){
        		paramMap.put("tags",tagsList);        	
        	}
        }
        if (reader.exists()){
            File contentFile = null;           
            if (FileContentReader.class.isAssignableFrom(reader.getClass())){
                // Grab the content straight from the content store if we can...            	
            	contentFile = ((FileContentReader) reader).getFile();            	
            } else{
                // ...otherwise throw exception   
            	isError = true;
            	strError = "Some Error with file to be Published. No content found.";
            }               
            try{
            	if (log.isDebugEnabled()){                	
                	log.debug("Publishing node: " + nodeToPublish);
                }               
                strBVCID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_BVC_ID);                
                if(StringUtils.isEmpty(strBVCID)){                	
                	strBVCID = "";
                }
                Object[] params = getWriteParams(strBVCID, paramMap, channelProperties,contentFile);                 
                if(params!=null){
                	URI uriPut = publishingHelper.getURIFromNodeRefAndChannelProperties(nodeToPublish, channelProperties);                  
                    InputStream in = ClientHttpRequest.post(new java.net.URL(uriPut.toString()),params);                   
                    int bytesRead = 0;
                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream ret = new ByteArrayOutputStream();
                    while ((bytesRead = in.read(buffer)) > 0){
                      ret.write(buffer, 0, bytesRead);
                    }                   
                    String response = new String(ret.toByteArray());  
                    System.out.println("Response:"+response);         
                    strBVCID = updateNodePropertiesAfterPublish(strBVCID, response, nodeService, publishNodeRef,channelProperties);                  
                }else{               	
                	isError = true;
                	strError = "No WRITE TOKEN FOR Brightcove Provided";                	              	
                }
                
            }catch(Exception e){            	
            	System.out.println("Exception Local Message:"+e.getLocalizedMessage());
            	isError = true;
				strError = e.getLocalizedMessage();
				//throw new AlfrescoRuntimeException(strError);			
			}finally{				
				if(isError == true){					
					nodeService.setProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, strError);
					if(nodeService.hasAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH)){
			           	nodeService.removeAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH);
			        }
					//Notify UCMS for Publish Failure 
					HttpUCMSClient ucmsClient = new HttpUCMSClient();
	    			try {
	    				//Notify UCMS App for Publishing Error
						HttpResponse httpResponse = ucmsClient.notifyUCMSMediaPublish(strUCMSID,"",0,false,strError);
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
        if(isError==false){
	        //Notify UCMS for Publish Successful                
	        Double durationObj = (Double) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_DURATIONSECS); 
	        //Handle null pointer exception 
	        if(durationObj==null){
	        	durationObj = 0.0;
	        }
	        double duration = durationObj;	        	
	        HttpUCMSClient ucmsClient = new HttpUCMSClient();
			HttpResponse httpResponse = null;
			try {
				httpResponse = ucmsClient.notifyUCMSMediaPublish(strUCMSID,""+strBVCID,duration,false,"");		
				if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
					System.out.println("UCMS Notification Successful");		                        
				} else{
					System.out.println("UCMS Notification failed."+httpResponse.getStatusLine().getReasonPhrase());
				}
			} catch (IOException | JSONException e ) {			
				e.printStackTrace();
			} 		
        }
    }

    @Override
    public void unpublish(NodeRef nodeToUnpublish, Map<QName, Serializable> channelProperties)
    {
    	NodeService nodeService = getNodeService();    	
    	NodeRef srcfileRef = null;  
    	NodeRef tgtfileRef = null; 
    	NodeRef fileRef	= null;   
    	boolean isError = false;
    	String strError = "";
    	List<AssociationRef> listTargetChilds = nodeService.getTargetAssocs(nodeToUnpublish,UCMSPublishingModel.PROPERTY_PUB_SOURCE);   
        
        for (AssociationRef child : listTargetChilds) {        	
        	srcfileRef = child.getSourceRef();  
        	tgtfileRef	= child.getTargetRef();
        	fileRef	= tgtfileRef;        	      
        }
        
        String strUCMSID = "";
        strUCMSID = (String) nodeService.getProperty(fileRef, UCMSPublishingModel.PROPERTY_UCM_ID);
        String strBVCID	=	(String)nodeService.getProperty(fileRef, UCMSPublishingModel.PROPERTY_BVC_ID);       
        long bvcid = 0;        
        if(strBVCID!=null && strBVCID.length()>0){
        	bvcid	= Long.parseLong(strBVCID);        	
        }else{
        	isError = true;
        	strError = "No Brightcove ID found for the node to Delete";        	
        }
    	Object[] params = getDeleteParams(bvcid, channelProperties);    	
    	try{
    		if(params!=null){
	         	URI uriPut;								
				uriPut = publishingHelper.getURIFromNodeRefAndChannelProperties(nodeToUnpublish, channelProperties);            
				InputStream in = ClientHttpRequest.post(new java.net.URL(uriPut.toString()), params );
				
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
		}catch (Exception e) {
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
    		//Notify UCMS for Publish Successful     
    		HttpUCMSClient ucmsClient = new HttpUCMSClient(); 			
  			try {
  				HttpResponse httpResponse = ucmsClient.notifyUCMSMediaUnPublish(strUCMSID,"success",strError);	
  				if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
  					System.out.println("UCMS Notification Successful for Unpublishing Asset:"+strUCMSID);		                        
  				} else{
  					System.out.println("UCMS Notification for Unpublishing Asset failed."+httpResponse.getStatusLine().getReasonPhrase());
  				}
  			}catch (IOException | JSONException e ) {			
  				e.printStackTrace();
  			}	       
        }
    }
    
    public Object[] getWriteParams(String strBVCID, Map<String, Object> paramMap, Map<QName, Serializable> channelProperties,File contentFile){    	
    	String writeToken	= publishingHelper.getWriteTokenChannelProperties(channelProperties);   
    	
    	if(StringUtils.isEmpty(writeToken)){
    		return null;
    	}else{
    		String fileName = ((String) paramMap.get("filename")).trim();
    		String referenceId = "";
    		String jsonRefId = "";
    		if(paramMap.containsKey("referenceId")){
    			referenceId = ((String) paramMap.get("referenceId")).trim();
    			jsonRefId	=	"\"referenceId\":\""+referenceId+"\"" ;
    		}
    		
    		String tags = "[";
    		String jsonTags = "";
    		if(paramMap.containsKey("tags")){
    			@SuppressWarnings("unchecked")
				ArrayList<String> tagsList = (ArrayList<String>) paramMap.get("tags");    			
		        for(int i=0;i< tagsList.size();i++){
		        	tags = tags+ tagsList.get(i);
		        	if(i< (tagsList.size() -1)){
		        		tags = tags + ",";
		        	}		        	
		        }
		        tags = tags + "]";		            			
    			jsonTags	=	", \"tags\":"+tags;
    			
    		}
    		
    		String shortDescription = "";
    		String jsonShortDescription = "";   		
    		
    		if(paramMap.containsKey("shortDescription")){    			
    			shortDescription = ((String) paramMap.get("shortDescription")).trim();
    			jsonShortDescription	=	", \"shortDescription\":\""+shortDescription+"\"" ;
    		}
    		
    		boolean create_multiple_renditions = true;
    		boolean preserve_source_rendition = false;
    		String encode_to = "MP4"; 
    		
    		if(fileName.contains(".flv") || fileName.contains(".FLV")){
    			encode_to = "FLV";
    			create_multiple_renditions = false;    			
    		}
    		String strJSONRequestParams = "";
    		Object[] params;
    		if(strBVCID.length() > 0){
    			jsonShortDescription = jsonShortDescription + "} ";
    			long bvcid = Long.parseLong(strBVCID);   		        
    			strJSONRequestParams	= "{" +
    	        		"\"method\":\"update_video\"" +
    	        		",\"params\":{" +
    	                	"\"token\":\""+writeToken+"\", " +
    	                	"\"video\":" + 
    	                     	"{\"id\":"+bvcid+", " +
    	                     		jsonRefId +
    	                     		jsonTags+
    	                      		jsonShortDescription+    	                      
    	                "}}";
    			params = new Object[] { "JSON-RPC", strJSONRequestParams};
    		}else{
    			jsonShortDescription = jsonShortDescription + "}, ";
    			strJSONRequestParams	= "{" +
    	        		"\"method\":\"create_video\"" +
    	        		",\"params\":{" +
    	                	"\"token\":\""+writeToken+"\", " +
    	                	"\"video\":" + 
    	                     	"{\"name\":\""+fileName+"\", " +
    	                     		jsonRefId +
    	                     		jsonTags+
    	                      		jsonShortDescription+
    	                      "\"filename\":\""+fileName+"\", " +
    	                      "\"encode_to\":\""+encode_to+"\", " +		
    	                      "\"create_multiple_renditions\":"+create_multiple_renditions+", " +
    	                      "\"preserve_source_rendition\":"+preserve_source_rendition+", " +
    	                      "\"maxsize\":\"2000000000\"" +
    	                "}}";
    			params = new Object[] {"JSON-RPC", strJSONRequestParams,paramMap.get("filename"), contentFile}; 
    		}
    		
    		System.out.println("JSON String:"+strJSONRequestParams);          
    		return params;
    	}
    }
    public Object[] getDeleteParams(Long fileId, Map<QName, Serializable> channelProperties){    	
    	String writeToken	= publishingHelper.getWriteTokenChannelProperties(channelProperties);    	
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
    public NodeRef getPublishingNodeRef(NodeRef nodeToPublish, NodeService nodeService){
    	List<AssociationRef> listTargetChilds = nodeService.getTargetAssocs(nodeToPublish,UCMSPublishingModel.PROPERTY_PUB_SOURCE);       
    	NodeRef srcfileRef = null;  
    	NodeRef tgtfileRef = null;     	   
        //List<AssociationRef> listTargetChilds = nodeService.getTargetAssocs(nodeToPublish, RegexQNamePattern.MATCH_ALL);
        System.out.println("list Target Childs size:"+listTargetChilds.size());
        for (AssociationRef child : listTargetChilds) {        	
        	srcfileRef = child.getSourceRef();  
        	tgtfileRef	= child.getTargetRef();        	
        	System.out.println("TGT File Ref:"+tgtfileRef);        	            
        }
        return tgtfileRef;
    }    
    public String updateNodePropertiesAfterPublish(String strBVCID, String resp,NodeService nodeService,NodeRef fileRef,Map<QName, Serializable> channelProperties) throws IOException, JSONException{       
      	System.out.println("Brightcove response:"+resp);
		JSONObject jObject = new JSONObject(resp);		
		String returnBVCID = "";
		if(nodeService.hasAspect(fileRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH)){
           	nodeService.removeAspect(fileRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH);
        }
		if(strBVCID.length() >0){
			if(jObject.isNull("error")){
				JSONObject jsonData	= jObject.getJSONObject("result");
				String id	= jsonData.getString("id");
				System.out.println("Brightcove Record updated. Id Returned from Results:"+id);
				returnBVCID = strBVCID;
			}else{
	        	throw new AlfrescoRuntimeException("Error in response from Brightcove:"+resp);
			}
		}else if(jObject.isNull("error")){					
				long BVCID = ((Long)jObject.get("result")).longValue();	
				returnBVCID = ""+BVCID;				
	            if(nodeService.hasAspect(fileRef, UCMSPublishingModel.ASPECT_BRIGHTCOVE_PUBLISHABLE)){           	
	            	if(fileRef!=null){            		
	            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_BVC_ID, ""+BVCID);                       		
	            		nodeService.setProperty(fileRef, PublishingModel.PROP_ASSET_ID, BVCID);
	            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, "");
	            	}
	            }else{            	
	            	if(fileRef!=null){
	            		nodeService.setType(fileRef, UCMSPublishingModel.TYPE_DIGITAL_ASSET);
	            		nodeService.addAspect(fileRef,UCMSPublishingModel.ASPECT_BRIGHTCOVE_PUBLISHABLE,null);	            		
	            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_BVC_ID, ""+BVCID);
	            		nodeService.setProperty(fileRef, PublishingModel.PROP_ASSET_ID, BVCID);               
	            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, "");
	            	}     
	            }	        
            System.out.println("Properties updated after BVC Publish");           
        }else{
        	throw new AlfrescoRuntimeException("Error in response from Brightcove:"+resp);                	
        }
		return returnBVCID;
    }
    public void updateNodePropertiesAfterUnPublish(String resp,NodeService nodeService,NodeRef nodeToPublish){   	
    	try {    	
	        JSONObject jObject = new JSONObject(resp);
	        if(jObject.isNull("error")){
	        	//delete node in Alfresco after successful unpublish
	        	nodeService.deleteNode(nodeToPublish);	           	           
	        }else{
	        	throw new AlfrescoRuntimeException("UnPublishing Error from Brightcove");                	
	        }  
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			throw new AlfrescoRuntimeException("Error Creating JSON Response Object");
		}     
    }   
}

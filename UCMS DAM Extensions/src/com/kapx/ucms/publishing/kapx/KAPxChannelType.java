package com.kapx.ucms.publishing.kapx;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.publishing.AbstractChannelType;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.AssociationRef;

import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.BrightcovePublishingModel;
import com.kapx.ucms.model.KAPxPublishingModel;
import com.kapx.ucms.model.UCMSPublishingModel;
import com.mysql.jdbc.StringUtils;

/**
 * Channel definition for publishing/unpublishing Video content to KAPx Server
 * 
 * @author Kalpesh
 * 
 */
public class KAPxChannelType extends AbstractChannelType
{
    private static final Log log = LogFactory.getLog(KAPxChannelType.class);
    public static final String ID = KAPxPublishingModel.PUBLISH_ID;    
    public static final String ERROR = "ERROR";    
    public static final String KAPXCHANNEL = "Kapx";
    private KAPxPublishingHelper publishingHelper;
    private ContentService contentService;
    private FileFolderService fileFolderService;
    private Set<String> supportedMimeTypes = KAPxPublishingModel.DEFAULT_SUPPORTED_MIME_TYPES;	
    Properties props = new Properties();
    private ServiceRegistry serviceRegistry;
    
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
       this.serviceRegistry = serviceRegistry;
    }
    
	public void setContentService(ContentService contentService) {
	        this.contentService = contentService;
	}	

    public void setSupportedMimeTypes(Set<String> mimeTypes){
        supportedMimeTypes = Collections.unmodifiableSet(new TreeSet<String>(mimeTypes));
    }

    public void setPublishingHelper(KAPxPublishingHelper kapxPublishingHelper){
        this.publishingHelper = kapxPublishingHelper;
    }
    
    public void setFileFolderService(FileFolderService fileFolderService){
        this.fileFolderService = fileFolderService;
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
        return KAPxPublishingModel.TYPE_DELIVERY_CHANNEL;
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
        NodeRef publishNodeRef	= getPublishingNodeRef(nodeToPublish,nodeService);
        String strUCMSID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);
        String strResourceURL = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_RESOURCEURL);
        if(strResourceURL==null){
        	strResourceURL = "";
        }
        boolean isError = false;
    	String strError = "";
        //There is no Publish for KAPx during Update. Just Notification
        //Else Create or Update Request for KAPx. Publish Item
        if(nodeService.hasAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_KAPX_UPDATE)){
        	try{        		        	
	        	String KAPxSlideDeckURL = ""+ nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_KAP_SLIDEDECKURL);
	        	int KAPxSlideCount = (Integer) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_KAP_SLIDE_CNT);
	        	String publishError = ""+ nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR);	        	
	        	HttpUCMSClient ucmsClient = new HttpUCMSClient();
	        	HttpResponse httpResponse = null;
	        	if(KAPxSlideDeckURL.length()>0){
	        		//Notify UCMS for Publish Successful for KAPx
	        		httpResponse = ucmsClient.notifyUCMSMediaPublish(strUCMSID,KAPxSlideDeckURL,KAPxSlideCount,true,"",strResourceURL);	        		
	        	}else{
	        		//Notify UCMS for Publish Failure for KAPx	        		
	        		httpResponse = ucmsClient.notifyUCMSMediaPublish(strUCMSID,"",0,true,publishError,strResourceURL);
	        	}	
				
				if(httpResponse.getStatusLine().getStatusCode() == 204){        	        		        	
		            System.out.println("UCMS Notification Successful");		                        
		        } else{
		        	System.out.println("UCMS Notification failed."+httpResponse.getStatusLine().getReasonPhrase());
				}
				nodeService.removeAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_KAPX_UPDATE);
				if(nodeService.hasAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH)){
		           	nodeService.removeAspect(publishNodeRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH);
		        }
        	}catch(Exception e){
        		System.out.println("UCMS Notification Error"+e);        		
        	}
        }else{       
	        System.out.println("Original Node to Publish:"+nodeToPublish.toString());
	        System.out.println("Node to Publish:"+publishNodeRef);
	        
	        String fileName = (String) nodeService.getProperty(publishNodeRef, ContentModel.PROP_NAME);       
	        String WEBDAVURL = "";
	        try {        	
				List<FileInfo> paths = fileFolderService.getNamePath(null, publishNodeRef);
				// build up the webdav url
	            
				StringBuilder path = new StringBuilder(128);                       
	            // build up the path skipping the first path as it is the root folder
	            for (int i=1; i<paths.size(); i++){
	            	String encodeURL = URLEncoder.encode(paths.get(i).getName(),"UTF-8");
	                path.append("/").append(encodeURL);
	            }
	            props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
				String damhost = (String) channelProperties.get(KAPxPublishingModel.PROP_DAM_HOST);
				int damport = Integer.parseInt((String) channelProperties.get(KAPxPublishingModel.PROP_DAM_PORT));
			
	            String alf_ticket = new HttpUCMSClient().getAlfrescoTicket();			
	            List< NameValuePair > qparams = new ArrayList< NameValuePair >();
	            qparams.add( new BasicNameValuePair("ticket",alf_ticket));               
				URI uri = URIUtils.createURI("http", damhost,damport, "alfresco/webdav"+path.toString(),URLEncodedUtils.format(qparams,"UTF-8" ),null);           
				WEBDAVURL = uri.toString();
				System.out.println("WEBDAV URL:"+WEBDAVURL);		
				String ucmsID = (String) nodeService.getProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);	   
		        URI uriPost = publishingHelper.getURIFromNodeRefAndChannelProperties(nodeToPublish, channelProperties);     
		        
		        HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(uriPost);
				List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
				urlParameters.add(new BasicNameValuePair("AssetUrl",WEBDAVURL));      			
				urlParameters.add(new BasicNameValuePair("ExternalAssetID",ucmsID));
		 
				httppost.setEntity(new UrlEncodedFormEntity(urlParameters));
				HttpResponse response = httpclient.execute(httppost,publishingHelper.getHttpContextFromChannelProperties(channelProperties));
				String result = EntityUtils.toString(response.getEntity());				
				if(response.getStatusLine().getStatusCode() == 200){
					System.out.println("Node Published successfully to KAPx...");				
					System.out.println("Response from KAPx API:"+result);
					updateNodePropertiesAfterPublish(result, nodeService, publishNodeRef);					
				}else{
					System.out.println("Publish Response Error:"+response.getStatusLine().getReasonPhrase());
					isError = true;
					strError = response.getStatusLine().getReasonPhrase();										
				}	   			
			}catch(Exception e){
				System.out.println("Publish Error:"+e.getLocalizedMessage());		
				isError = true;
				strError = e.getLocalizedMessage();
				//throw new AlfrescoRuntimeException(strError);
							
			}finally{				
				if(isError == true){					
					nodeService.setProperty(publishNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, strError);
					HttpUCMSClient ucmsClient = new HttpUCMSClient();
	    			try {
	    				//Notify UCMS App for Publishing Error
						HttpResponse httpResponse = ucmsClient.notifyUCMSMediaPublish(strUCMSID,"",0,true,strError,strResourceURL);
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
        }//else for create ends
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
    
    public void updateNodePropertiesAfterPublish(String resp,NodeService nodeService,NodeRef fileRef) throws JSONException{              
        JSONObject jObject = new JSONObject(resp);
        System.out.println("KAPx response:"+resp.toString());
        if(jObject.isNull("message")){
        	if(nodeService.hasAspect(fileRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH)){
	           	nodeService.removeAspect(fileRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH);
	        }
			String KAPXID = (String) jObject.get("ConversionKey");				
            if(nodeService.hasAspect(fileRef, UCMSPublishingModel.ASPECT_KAPX_PUBLISHABLE)){            	
            	if(fileRef!=null){                        		
            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_KAP_ID, KAPXID);                       		
            		nodeService.setProperty(fileRef, PublishingModel.PROP_ASSET_ID, KAPXID);     
            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, "");
            	}
            }else{           	
            	if(fileRef!=null){
            		nodeService.setType(fileRef, UCMSPublishingModel.TYPE_DIGITAL_ASSET);
            		nodeService.addAspect(fileRef,UCMSPublishingModel.ASPECT_KAPX_PUBLISHABLE,null);                            	
            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_KAP_ID, KAPXID);                       		
            		nodeService.setProperty(fileRef, PublishingModel.PROP_ASSET_ID, KAPXID); 
            		nodeService.setProperty(fileRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHERROR, "");
            	}                        	                    
            }	            
            System.out.println("Properties updated");           
        }else{
        	//Error in Response
        	throw new AlfrescoRuntimeException("Error in response from KAPx:"+resp);  
        }        
    }   
}
package com.kapx.ucms.publishing.brightcove;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import org.alfresco.repo.node.encryption.MetadataEncryptor;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIUtils;

import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.BrightcovePublishingModel;

/**
 * Helper Methods for Brightcove Publishing
 * 
 * @author Kalpesh Mehta
 * 
 */
public class BrightcovePublishingHelper
{
	private ContentService contentService;    
    private MetadataEncryptor encryptor;    
	
	public void setContentService(ContentService contentService) {
	        this.contentService = contentService;
	}	

    public void setEncryptor(MetadataEncryptor encryptor)
    {
        this.encryptor = encryptor;
    }
    
    /**
     * Get Write Token from channel properties & 
     * 
     * @param channelProperties
     * @return
     */
    public String getWriteTokenChannelProperties(Map<QName, Serializable> channelProperties)
    {
    	final String BRIGHTCOVE_WRITE_TOKEN	= (String)	channelProperties.get(BrightcovePublishingModel.PROP_WRITE_TOKEN);
    	System.out.println("Write Token:"+BRIGHTCOVE_WRITE_TOKEN);
    	return BRIGHTCOVE_WRITE_TOKEN;        
    }
    /**
     * Get Read Token from channel properties & 
     * 
     * @param channelProperties
     * @return
     */
    public String getReadTokenChannelProperties(Map<QName, Serializable> channelProperties)
    {
    	final String BRIGHTCOVE_READ_TOKEN	= (String)	channelProperties.get(BrightcovePublishingModel.PROP_READ_TOKEN);
    	System.out.println("READ TOKEN:"+BRIGHTCOVE_READ_TOKEN);
    	return BRIGHTCOVE_READ_TOKEN;        
    }
    
    /**
     * Get ReadURL Token from channel properties & 
     * 
     * @param channelProperties
     * @return
     */
    public String getReadURLTokenChannelProperties(Map<QName, Serializable> channelProperties)
    {
    	final String BRIGHTCOVE_READURL_TOKEN	= (String)	channelProperties.get(BrightcovePublishingModel.PROP_READURL_TOKEN);
    	System.out.println(BRIGHTCOVE_READURL_TOKEN);
    	return BRIGHTCOVE_READURL_TOKEN;        
    }

    /**
     * Build URI for a nodeRef into MarkLogic Server using the channel properties
     * 
     * @param nodeToPublish
     * @param channelProperties
     * @return
     * @throws URISyntaxException
     */
    public URI getURIFromNodeRefAndChannelProperties(NodeRef nodeToPublish, Map<QName, Serializable> channelProperties)
            throws URISyntaxException
    {
    	
    	String host	= (String) channelProperties.get(BrightcovePublishingModel.PROP_HOST);
    	Integer portObj	= ((Integer) channelProperties.get(BrightcovePublishingModel.PROP_PORT));
    	int port = 0;
    	if(portObj!=null){
    		port = portObj.intValue();
    	}
    	URI uri = URIUtils.createURI("http", host, port, "services/post",null, null);   	
        System.out.println("Publish URI is:"+uri.toString());
        return uri;
    }

}

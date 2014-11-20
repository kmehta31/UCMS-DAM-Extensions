package com.kapx.ucms.publishing.kapx;

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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.BrightcovePublishingModel;
import com.kapx.ucms.model.KAPxPublishingModel;

/**
 * Helper Methods for KAPx Publishing
 * 
 * @author Kalpesh Mehta
 * 
 */
public class KAPxPublishingHelper
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
     * Build a httpContext from channel properties
     * 
     * @param channelProperties
     * @return
     */
    public HttpContext getHttpContextFromChannelProperties(Map<QName, Serializable> channelProperties)
    {
        String kapxUsername = (String) encryptor.decrypt(PublishingModel.PROP_CHANNEL_USERNAME,
                channelProperties.get(PublishingModel.PROP_CHANNEL_USERNAME));
        String kapxPassword = (String) encryptor.decrypt(PublishingModel.PROP_CHANNEL_PASSWORD,
                channelProperties.get(PublishingModel.PROP_CHANNEL_PASSWORD));
        System.out.println("Username:"+kapxUsername+" Password:"+kapxPassword);
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(kapxUsername, kapxPassword);
        HttpContext context = new BasicHttpContext();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, creds);
        context.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
        return context;
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
    	
    	String protocol	= (String) channelProperties.get(KAPxPublishingModel.PROP_PROTOCOL);
    	String host	= (String) channelProperties.get(KAPxPublishingModel.PROP_HOST);
    	Integer portObj	= ((Integer) channelProperties.get(KAPxPublishingModel.PROP_PORT));    	
    	int port = 0;
    	if(portObj!=null){
    		port = portObj.intValue();
    	}
    	URI uri = URIUtils.createURI(protocol, host, port, "api/conversion/",null, null);   	
        System.out.println("Publish URI is:"+uri.toString());
        return uri;
    }

}

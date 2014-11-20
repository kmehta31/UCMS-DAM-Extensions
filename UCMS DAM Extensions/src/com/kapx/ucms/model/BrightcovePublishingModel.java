package com.kapx.ucms.model;
import java.util.Set;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.collections.CollectionUtils;

/**
 * Brightcove Publishing Model
 * 
 * @author Kalpesh Mehta
 * 
 */
public interface BrightcovePublishingModel{
    public static final String NAMESPACE = "http://www.alfresco.org/model/publishing/brightcove/1.0";   
    public static final String PUBLISH_ID = "brightcove";  
    
    /*Brightcove Channel Properties*/
    public static final QName PROP_HOST = QName.createQName(NAMESPACE, "host");
    public static final QName PROP_PORT = QName.createQName(NAMESPACE, "port");
    public static final QName PROP_READ_TOKEN = QName.createQName(NAMESPACE, "readToken");
    public static final QName PROP_READURL_TOKEN = QName.createQName(NAMESPACE, "readURLToken");
    public static final QName PROP_WRITE_TOKEN = QName.createQName(NAMESPACE, "writeToken");    
    
    public static final QName TYPE_DELIVERY_CHANNEL = QName.createQName(NAMESPACE, "DeliveryChannel");   
    public static final Set<String> DEFAULT_SUPPORTED_MIME_TYPES = 
    		CollectionUtils.unmodifiableSet(MimetypeMap.MIMETYPE_VIDEO_MPG,
					MimetypeMap.MIMETYPE_VIDEO_MP4,
					MimetypeMap.MIMETYPE_MP3,
					MimetypeMap.MIMETYPE_AUDIO_MP4,
					MimetypeMap.MIMETYPE_VIDEO_FLV,
					MimetypeMap.MIMETYPE_VIDEO_3GP,
					MimetypeMap.MIMETYPE_VIDEO_AVI,
					MimetypeMap.MIMETYPE_VIDEO_QUICKTIME,
					MimetypeMap.MIMETYPE_VIDEO_WMV,
					MimetypeMap.MIMETYPE_VIDEO_MPG,
					MimetypeMap.MIMETYPE_FLASH					
					);
}

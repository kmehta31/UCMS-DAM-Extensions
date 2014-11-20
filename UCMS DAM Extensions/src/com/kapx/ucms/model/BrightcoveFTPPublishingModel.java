package com.kapx.ucms.model;
import java.util.Set;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.collections.CollectionUtils;

/**
 * BrightcoveFTP Publishing Model
 * 
 * @author Kalpesh Mehta
 * 
 */
public interface BrightcoveFTPPublishingModel{    
    public static final String FTP_NAMESPACE = "http://www.alfresco.org/model/publishing/brightcove/ftp/1.0";  
    public static final String PUBLISH_FTP_ID = "brightcoveftp";    
    
    public static final QName PROP_SERVER = QName.createQName(FTP_NAMESPACE, "server");
    public static final QName PROP_PORT = QName.createQName(FTP_NAMESPACE, "port");
    public static final QName PROP_PUBLISHER_ID = QName.createQName(FTP_NAMESPACE, "publisherID");
    public static final QName PROP_PREPARER = QName.createQName(FTP_NAMESPACE, "preparer");
    public static final QName PROP_EMAIL = QName.createQName(FTP_NAMESPACE, "email");
    public static final QName PROP_REPORT_SUCCESS = QName.createQName(FTP_NAMESPACE, "reportSuccess");   
    public static final QName PROP_CALLBACK_URL = QName.createQName(FTP_NAMESPACE, "callbackURL");   
    
    public static final QName TYPE_FTP_DELIVERY_CHANNEL = QName.createQName(FTP_NAMESPACE, "DeliveryChannel"); 
    
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

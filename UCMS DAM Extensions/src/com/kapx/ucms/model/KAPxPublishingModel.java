package com.kapx.ucms.model;
import java.util.Set;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.collections.CollectionUtils;

/**
 * KAPx Publishing Model
 * 
 * @author Kalpesh Mehta
 * 
 */
public interface KAPxPublishingModel{
    public static final String NAMESPACE = "http://www.alfresco.org/model/publishing/kapx/1.0";
    public static final String PREFIX = "kapx";
    public static final String PUBLISH_ID = "kapx";

    public static final QName PROP_PROTOCOL = QName.createQName(NAMESPACE, "protocol");
    public static final QName PROP_HOST = QName.createQName(NAMESPACE, "host");
    public static final QName PROP_PORT = QName.createQName(NAMESPACE, "port");
    public static final QName PROP_DAM_HOST = QName.createQName(NAMESPACE, "damhost");
    public static final QName PROP_DAM_PORT = QName.createQName(NAMESPACE, "damport");

    public static final QName TYPE_DELIVERY_CHANNEL = QName.createQName(NAMESPACE, "DeliveryChannel");
    public static final QName ASPECT_DELIVERY_CHANNEL = QName.createQName(NAMESPACE, "DeliveryChannelAspect");
    public static final Set<String> DEFAULT_SUPPORTED_MIME_TYPES = 
    
    CollectionUtils.unmodifiableSet(MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE,
    				MimetypeMap.MIMETYPE_PPT,
    				MimetypeMap.MIMETYPE_OPENXML_PRESENTATION,
    				MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION);
}

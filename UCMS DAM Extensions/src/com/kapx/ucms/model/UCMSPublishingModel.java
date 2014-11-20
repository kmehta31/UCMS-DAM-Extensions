package com.kapx.ucms.model;
import org.alfresco.service.namespace.QName;

/**
 * Brightcove Publishing Model
 * 
 * @author Kalpesh Mehta
 * 
 */
public interface UCMSPublishingModel
{
    public static final String UCMS_NAMESPACE 	= "http:/ucms.kaplan.com/model/content/1.0";
    public static final String BVC_NAMESPACE 	= "http:/brightcove.video.cloud/model/content/1.0";
    public static final String KAPX_NAMESPACE 	= "http:/www.kapx.com/model/content/1.0";
    public static final String PUB_NAMESPACE 	= "http://www.alfresco.org/model/publishing/1.0";
    public static final QName  ASPECT_PUBLISHED = QName.createQName(PUB_NAMESPACE, "published");
    public static final QName  PROPERTY_PUB_SOURCE = QName.createQName(PUB_NAMESPACE, "source");
    public static final String UCMS_PREFIX 		= "ucm";
    public static final String BVC_PREFIX 		= "bvc";
    public static final String KAPX_PREFIX 		= "kap";

    public static final QName TYPE_DIGITAL_ASSET = QName.createQName(UCMS_NAMESPACE, "digitalAsset");
    public static final String DIGITAL_ASSET_TYPE = "digitalAsset";
    public static final QName PROPERTY_UCM_ID = QName.createQName(UCMS_NAMESPACE, "id");
    public static final QName PROPERTY_UCM_FILENAME = QName.createQName(UCMS_NAMESPACE, "fileName");
    public static final QName PROPERTY_UCM_TITLE = QName.createQName(UCMS_NAMESPACE, "title");
    public static final QName PROPERTY_UCM_DESC = QName.createQName(UCMS_NAMESPACE, "description");
    public static final QName PROPERTY_UCM_TAGS = QName.createQName(UCMS_NAMESPACE, "tags");
    public static final QName PROPERTY_UCM_PRODUCTLINE = QName.createQName(UCMS_NAMESPACE, "productLine");
    public static final QName PROPERTY_UCM_CONTENTGRP = QName.createQName(UCMS_NAMESPACE, "contentGroup");
    public static final QName PROPERTY_UCM_CUEPOINTS = QName.createQName(UCMS_NAMESPACE, "cuePoints");
    public static final QName PROPERTY_UCM_RESOURCEURL = QName.createQName(UCMS_NAMESPACE, "resourceURL");
    public static final QName PROPERTY_UCM_DURATIONSECS = QName.createQName(UCMS_NAMESPACE, "durationSeconds");
    public static final QName PROPERTY_UCM_PUBLISHTARGET = QName.createQName(UCMS_NAMESPACE, "publishTarget");
    public static final QName PROPERTY_UCM_PUBLISHERROR = QName.createQName(UCMS_NAMESPACE, "publishError");
    public static final QName PROPERTY_UCM_ACTIVEPROVIDERKEY = QName.createQName(UCMS_NAMESPACE, "activeProviderKey");
    public static final QName PROPERTY_UCM_ARCHIVEDATE = QName.createQName(UCMS_NAMESPACE, "archivedAt");
    
    public static final QName ASPECT_IS_READY_TO_PUBLISH = QName.createQName(UCMS_NAMESPACE, "isReadyToPublish");
    public static final QName ASPECT_IS_KAPX_UPDATE = QName.createQName(UCMS_NAMESPACE, "isKapxUpdate");
    
    //Brightcove Publishable Aspect & Properties
    public static final QName ASPECT_BRIGHTCOVE_PUBLISHABLE = QName.createQName(BVC_NAMESPACE, "brightcovePub");
    public static final QName PROPERTY_BVC_ID = QName.createQName(BVC_NAMESPACE, "id");
    public static final QName PROPERTY_BVC_URL = QName.createQName(BVC_NAMESPACE, "url");
    public static final QName PROPERTY_BVC_LIVEAT = QName.createQName(BVC_NAMESPACE, "liveAt");
    public static final QName PROPERTY_BVC_CREATEDAT = QName.createQName(BVC_NAMESPACE, "createdAt");
    public static final QName PROPERTY_BVC_UPDATEDAT = QName.createQName(BVC_NAMESPACE, "updatedAt");
    public static final QName PROPERTY_BVC_ARCHVIEDAT = QName.createQName(BVC_NAMESPACE, "archivedAt");
    
  //KAPx Publishable Aspect & Properties
    public static final QName ASPECT_KAPX_PUBLISHABLE = QName.createQName(KAPX_NAMESPACE, "kapxPub");
    public static final QName PROPERTY_KAP_ID = QName.createQName(KAPX_NAMESPACE, "kapxSlideConversionId");
    public static final QName PROPERTY_KAP_SLIDEDECKURL = QName.createQName(KAPX_NAMESPACE, "kapxSlideDeckUrl");
    public static final QName PROPERTY_KAP_SLIDE_CNT = QName.createQName(KAPX_NAMESPACE, "kapxSlideCount");
    public static final QName PROPERTY_KAP_LIVEAT = QName.createQName(KAPX_NAMESPACE, "liveAt");
    public static final QName PROPERTY_KAP_CREATEDAT = QName.createQName(KAPX_NAMESPACE, "createdAt");
    public static final QName PROPERTY_KAP_UPDATEDAT = QName.createQName(KAPX_NAMESPACE, "updatedAt");
    public static final QName PROPERTY_KAP_ARCHVIEDAT = QName.createQName(KAPX_NAMESPACE, "archivedAt");
    
}

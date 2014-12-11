package com.kapx.ucms.publishing.brightcove;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.node.encryption.MetadataEncryptor;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.client.utils.URIUtils;

import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.BrightcoveFTPPublishingModel;
import com.kapx.ucms.model.BrightcovePublishingModel;
import com.kapx.ucms.xml.Asset;
import com.kapx.ucms.xml.Callback;
import com.kapx.ucms.xml.Notify;
import com.kapx.ucms.xml.PublisherUploadManifest;
import com.kapx.ucms.xml.Title;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;


public class BrightcoveFTPManager {
	Map<QName, Serializable> channelProperties;
	MetadataEncryptor encryptor;
	public BrightcoveFTPManager(Map<QName, Serializable> channelProperties, MetadataEncryptor encryptor){
		this.channelProperties = channelProperties;
		this.encryptor=encryptor;
	}
	
	public void upload(File contentFile,String fileName,String title, String ucmsID, String shortDesc,List<String> tagList){		
		String ServerName	= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_SERVER);		
        String FTPUsername = (String) encryptor.decrypt(PublishingModel.PROP_CHANNEL_USERNAME,
                channelProperties.get(PublishingModel.PROP_CHANNEL_USERNAME));
        String FTPPassword = (String) encryptor.decrypt(PublishingModel.PROP_CHANNEL_PASSWORD,
                channelProperties.get(PublishingModel.PROP_CHANNEL_PASSWORD));
        int FTPPort =   (Integer) channelProperties.get(BrightcoveFTPPublishingModel.PROP_FTP_PORT);       
		System.out.println("Server:"+ServerName+"Port:"+FTPPort+" User:"+FTPUsername+" Pwd:"+FTPPassword);
		
		FTPClient ftpClient = new FTPClient();
        try {
 
            ftpClient.connect(ServerName, FTPPort);
            ftpClient.login(FTPUsername, FTPPassword);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setConnectTimeout(900000); 
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);         
            
            InputStream inputStream = new FileInputStream(contentFile); 
            System.out.println("Start uploading first file");
            boolean done = ftpClient.storeFile(fileName, inputStream);
            inputStream.close();
            if(done){           	
                System.out.println("The file is uploaded successfully.");
                createAndPublishManifest(ftpClient, fileName, title, ucmsID, shortDesc,tagList);                
            }else{
            	throw new AlfrescoRuntimeException("Error uploading Media file to Brightcove FTP");
            }
            
        }catch (Exception ex) {                    
            throw new AlfrescoRuntimeException("Error uploading file to Brightcove FTP."+ex.getLocalizedMessage());
        } finally {
            try {            	
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {            	
                ex.printStackTrace();
            }
        }		
	}
	public void createAndPublishManifest(FTPClient ftpClient, String fileName, String title, String ucmsID, String shortDesc,List<String> tagsList) throws IOException, JAXBException{
		final String VTYPE = "VIDEO_FULL";
		String ManifestXML = ucmsID+"_MANIFEST.xml";       
        String PublisherID	= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_PUBLISHER_ID);
        String EMAIL		= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_EMAIL);
        String PREPARER		= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_PREPARER);
        boolean reportSuccess	= (Boolean) channelProperties.get(BrightcoveFTPPublishingModel.PROP_REPORT_SUCCESS);
        String callbackURL	= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_CALLBACK_URL);
        String encode_to	= "";
        
        List<Notify> emailList = new ArrayList<Notify>();
        boolean create_multiple_renditions = true;
        
        if(fileName.contains(".mp4") || fileName.contains(".MP4")){
        	encode_to = "MP4";
        }
        if(fileName.contains(".flv") || fileName.contains(".FLV")){        	
        	encode_to = "FLV";
        	create_multiple_renditions = false;
        }      
        Notify notify = null;       
    	String[] emailArray = EMAIL.split(",");
    	for(String email:emailArray){ 
    		notify = new Notify();         	
         	notify.setEmail(email);
         	emailList.add(notify);            	
        }
    		
    	
		HttpUCMSClient httpClient = new HttpUCMSClient();
		String alfTicket = httpClient.getAlfrescoTicket();
		if(alfTicket.length()>0){
			callbackURL = callbackURL + "?alf_ticket="+alfTicket;
		}		
		Callback callback = new Callback();
		callback.setEntityurl(callbackURL);	
        Asset asset = new Asset();
		asset.setFilename(fileName);
		asset.setRefid(ucmsID);
		asset.setType(VTYPE);		
		if(encode_to.length()>0){			
			asset.setEncodeTo(encode_to);
			asset.setEncodeMultiple(create_multiple_renditions);
		}	
		
		Title titleObj = new Title();		
		titleObj.setName(fileName);
		titleObj.setRefid(ucmsID);		
		titleObj.setVideoFullRefid(ucmsID);
		titleObj.setShortDesc(shortDesc);
		if(tagsList!=null){
        	if(tagsList.size()>0){
        		titleObj.setTags(tagsList);
        	}
        }
		
		titleObj.setActive(true);
		
		PublisherUploadManifest manifest = new PublisherUploadManifest();
		manifest.setReportSuccess(reportSuccess);
		manifest.setPublisherID(PublisherID);
		manifest.setPreparer(PREPARER);
		manifest.setAsset(asset);
		manifest.setNotify(emailList);
		manifest.setCallback(callback);
		manifest.setTitle(titleObj);
        
    	JAXBContext jaxbContext = JAXBContext.newInstance(PublisherUploadManifest.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		Properties props = new Properties();
		props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));			
		String tempFilePath = props.getProperty("tempfolderpath").trim()+ManifestXML;		
		File file = new File(tempFilePath.trim());
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);	                   
        jaxbMarshaller.marshal(manifest, file);		
        
        InputStream inputXMLStream = new FileInputStream(file);
        boolean done = ftpClient.storeFile(ManifestXML, inputXMLStream);	        
        inputXMLStream.close();
        if(done){       	
            System.out.println("The XML file uploaded successfully.");
            //delete temp file
            if(props.getProperty("iftempfiledelete").equalsIgnoreCase("true")){
            	file.delete();            	
            }                
        }else{
        	throw new AlfrescoRuntimeException("Error uploading XML file to Brightcove FTP");
        }					
	}
	
	 /**
     * Build URI for a nodeRef using the channel properties
     * 
     * @param channelProperties
     * @return
     * @throws URISyntaxException
     */
    public URI getURIFromChannelProperties(Map<QName, Serializable> channelProperties) throws URISyntaxException{
    	
    	String host	= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_HOST);
    	Integer portObj	= ((Integer) channelProperties.get(BrightcoveFTPPublishingModel.PROP_PORT));
    	//default to http port
    	int port = 80;
    	if(portObj!=null){
    		port = portObj.intValue();
    	}
    	URI uri = URIUtils.createURI("http", host, port, "services/post",null, null);        
        return uri;
    }
    public String getWriteTokenChannelProperties(Map<QName, Serializable> channelProperties){
    	final String BRIGHTCOVE_WRITE_TOKEN	= (String)	channelProperties.get(BrightcoveFTPPublishingModel.PROP_WRITE_TOKEN);    	
    	return BRIGHTCOVE_WRITE_TOKEN;        
    }
}

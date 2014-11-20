package com.kapx.ucms.publishing.brightcove;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.node.encryption.MetadataEncryptor;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.service.namespace.QName;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.BrightcoveFTPPublishingModel;
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
	private static String OS = System.getProperty("os.name").toLowerCase();
	public BrightcoveFTPManager(Map<QName, Serializable> channelProperties, MetadataEncryptor encryptor){
		this.channelProperties = channelProperties;
		this.encryptor=encryptor;
	}
	
	public void upload(File contentFile,String fileName,String title, String ucmsID, String shortDesc,String tags){		
		String ServerName	= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_SERVER);		
        String FTPUsername = (String) encryptor.decrypt(PublishingModel.PROP_CHANNEL_USERNAME,
                channelProperties.get(PublishingModel.PROP_CHANNEL_USERNAME));
        String FTPPassword = (String) encryptor.decrypt(PublishingModel.PROP_CHANNEL_PASSWORD,
                channelProperties.get(PublishingModel.PROP_CHANNEL_PASSWORD));
        int FTPPort =   (Integer) channelProperties.get(BrightcoveFTPPublishingModel.PROP_PORT);       
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
                createAndPublishManifest(ftpClient, fileName, title, ucmsID, shortDesc,tags);                
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
	public void createAndPublishManifest(FTPClient ftpClient, String fileName, String title, String ucmsID, String shortDesc,String tags) throws IOException, JAXBException{
		final String VTYPE = "VIDEO_FULL";
		String ManifestXML = ucmsID+"_MANIFEST.xml";       
        String PublisherID	= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_PUBLISHER_ID);
        String EMAIL		= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_EMAIL);
        String PREPARER		= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_PREPARER);
        boolean reportSuccess	= (Boolean) channelProperties.get(BrightcoveFTPPublishingModel.PROP_REPORT_SUCCESS);
        String callbackURL	= (String) channelProperties.get(BrightcoveFTPPublishingModel.PROP_CALLBACK_URL);
        String encode_to	= "";
        List<String> tagList = new ArrayList<String>();
        List<Notify> emailList = new ArrayList<Notify>();
        
        
        if(fileName.contains(".mp4") || fileName.contains(".MP4")){
        	encode_to = "MP4";
        }
        if(fileName.contains(".flv") || fileName.contains(".FLV")){
        	encode_to = "FLV";
        }      
        Notify notify = null;       
    	String[] emailArray = EMAIL.split(",");
    	for(String email:emailArray){ 
    		notify = new Notify();
         	//System.out.println("Email:"+email);
         	notify.setEmail(email);
         	emailList.add(notify);            	
        }
    		
    	
		HttpUCMSClient httpClient = new HttpUCMSClient();
		String alfTicket = httpClient.getAlfrescoTicket();
		if(alfTicket.length()>0){
			callbackURL = callbackURL + "?alf_ticket="+alfTicket;
		}
		System.out.println("Callback URL:"+callbackURL);
		Callback callback = new Callback();
		callback.setEntityurl(callbackURL);	
        Asset asset = new Asset();
		asset.setFilename(fileName);
		asset.setRefid(ucmsID);
		asset.setType(VTYPE);		
		if(encode_to.length()>0){			
			asset.setEncodeTo(encode_to);
		}
		
		if(tags.length()>0){
        	tags = tags.replace("[", "");
            tags = tags.replace("]", "");
            tags = tags.replace("\"","");           
            String[] tagArray = tags.split(",");            
            for(String tag:tagArray){ 
            	//System.out.println("Tag:"+tag);
            	tagList.add(tag);            	
            }        	
        }
		
		Title titleObj = new Title();		
		titleObj.setName(fileName);
		titleObj.setRefid(ucmsID);		
		titleObj.setVideoFullRefid(ucmsID);
		titleObj.setShortDesc(shortDesc);
		titleObj.setTags(tagList);
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
		System.out.println("Temp File Path:"+tempFilePath);
		File file = new File(tempFilePath.trim());
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);	                   
        jaxbMarshaller.marshal(manifest, file);		
        
        InputStream inputXMLStream = new FileInputStream(file);
        boolean done = ftpClient.storeFile(ManifestXML, inputXMLStream);	        
        inputXMLStream.close();
        if(done){       	
            System.out.println("The XML file is uploaded successfully.");
            //delete temp file
            if(props.getProperty("iftempfiledelete").equalsIgnoreCase("true")){
            	file.delete();            	
            }                
        }else{
        	throw new AlfrescoRuntimeException("Error uploading XML file to Brightcove FTP");
        }					
	}
}

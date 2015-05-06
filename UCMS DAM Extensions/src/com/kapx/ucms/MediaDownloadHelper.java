package com.kapx.ucms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.kapx.ucms.model.UCMSPublishingModel;
public class MediaDownloadHelper {	
	private String strResourceURL	=  "";
	private NodeService  nodeService = null;
	private ContentService  contentService = null;
	private ServiceRegistry registry = null;
	private NodeRef assetRef	= null;
	private Properties props = null;
	
	public MediaDownloadHelper(String strResourceURL,NodeService nodeService, ContentService contentService, NodeRef nodeRef, ServiceRegistry registry) throws IOException{
		this.strResourceURL = strResourceURL;
		this.nodeService = nodeService;
		this.contentService = contentService;
		this.assetRef = nodeRef;
		this.registry = registry;
		this.props = new Properties();
		this.props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties")); 
	}
	public void downloadFromS3(boolean ifLargeFile,String strResourceType) throws IOException{	
		AmazonS3 S3Client = null;
		if(strResourceURL.toLowerCase().contains(HttpUCMSClient.AMAZON_DOMAIN_IDENTIFIER)){
			String S3AMAZONURI = "";
			String S3URI = "";
			String bucket = "";
			System.out.println("S3 Download URL..");			        				
			// logon to S3		
			String[] temp	= strResourceURL.split(HttpUCMSClient.AMAZON_DOMAIN_IDENTIFIER, 0);
			if(temp[0].contains(".")){
				//bucket part of subdomain		
				//e.g. https://bucketname.s3.amazonaws.com/pathToAsset
				String tempStr = temp[0];
				String tempURI = temp[1];
				String[] strURI = tempURI.split("/",2);
				S3URI = strURI[1];	
				System.out.println("S3 URI:"+S3URI);
				String[] strBucket	= tempStr.split("//");
				String tempBuck = strBucket[1];							
				String[] strBucketTemp = tempBuck.split("\\.");
				bucket = strBucketTemp[0];
				System.out.println("Bucket from URI:"+bucket);				
			}else{
				//bucket part of URL				
				//e.g. https://s3.amazonaws.com/bucketname/pathToAsset				
				S3AMAZONURI	=	temp[1];					        				 
				String[] strBucketTemp = S3AMAZONURI.split("/");	        				
				String[] strBucket	= strBucketTemp[1].split("/");	        				
				bucket = strBucket[0].trim();
				System.out.println("Bucket from URI:"+bucket);
				String[] temp1	= S3AMAZONURI.split(bucket+"/",0);
				S3URI	= temp1[1];
				System.out.println("Split URI:"+S3URI);	
			}			
			String strAccessKey = "";
			String strSecretKey = "";
			if(bucket.toLowerCase().contains("hesser")){				
				strAccessKey = "accessKey";
				strSecretKey = "secretKey";
			}else{
				strAccessKey = "ucmsaccessKey";
				strSecretKey = "ucmssecretKey";
			}
			if(strResourceType.equalsIgnoreCase("public")){
				System.out.println("Public AWS Access");
				BasicAWSCredentials awsCredentials = null;
				S3Client = new AmazonS3Client(awsCredentials);
				System.out.println("S3 Public Client created...");
			}else{				
				System.out.println("Private AWS Access");
				S3Client = new AmazonS3Client(new BasicAWSCredentials(props.getProperty(strAccessKey).trim(),props.getProperty(strSecretKey).trim()));
			}
			//String bucket	= props.getProperty("bucketName").trim();
			System.out.println("Client Ready to fetch object from bucket:");				
			S3Object object = S3Client.getObject(bucket, S3URI);			
			if(object!= null){      
				ObjectMetadata objMetadata = object.getObjectMetadata();
				String strContentType	=	objMetadata.getContentType();
				String mimeType = getMimetype(strContentType,strResourceURL); 
				
        		ContentData contentData = (ContentData)nodeService.getProperty(assetRef, ContentModel.PROP_CONTENT);                        
        		contentData = ContentData.setMimetype(contentData, mimeType);
        		nodeService.setProperty(assetRef, ContentModel.PROP_CONTENT, contentData);
	            			
				ContentWriter writer = null;
				OutputStream outputStream = null;
				InputStream objectDataStream = null;
				try{
					objectDataStream = object.getObjectContent();							
					if(ifLargeFile){
						 System.out.println("Large File to download");
						 writer = contentService.getWriter(assetRef, ContentModel.PROP_CONTENT, true);
						 outputStream = writer.getContentOutputStream();							 
			             IOUtils.copyLarge(objectDataStream, outputStream);
					}else{
						byte[] imageByteArray = IOUtils.toByteArray(objectDataStream); 
						writer = contentService.getWriter(assetRef, ContentModel.PROP_CONTENT, true);
						writer.putContent(new ByteArrayInputStream(imageByteArray));
					}
					}catch(IOException io){
						System.out.println();
						io.printStackTrace();
						throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,"Error reading/writing to file");
					}finally{
						 IOUtils.closeQuietly(objectDataStream);
						 if(outputStream !=null){
							 IOUtils.closeQuietly(outputStream);
						 }
						 System.out.println("Content Written Successfully to Stream");	
						 if(mimeType.contains("mp4") || mimeType.contains("quicktime")){
							 //fetch duration for mp4 and quicktime videos
							 updateMetadata(mimeType);							
						 }
					}					                                
			}else{
				 throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,"No such resource:"+strResourceURL+" found under "+props.getProperty("bucketName")+"bucket.");
			}	        					
		}
	}
	
	public void downloadFromURL(boolean ifLargeFile) throws IllegalStateException, IOException, SignatureException{		
		System.out.println("Media Download via URL..");	        				
		HttpUCMSClient ucmsClient = new HttpUCMSClient();		
        HttpResponse response = ucmsClient.executeRequest(strResourceURL);
        System.out.println("Response Status:"+response.getStatusLine().getStatusCode());                     
        if(response.getStatusLine().getStatusCode() == 200){       	
        	String contentType = response.getEntity().getContentType().getValue();
        	String mimeType=getMimetype(contentType,strResourceURL); 
        	
        	ContentData contentData = (ContentData)nodeService.getProperty(assetRef, ContentModel.PROP_CONTENT);                
            contentData = ContentData.setMimetype(contentData, mimeType);
        	nodeService.setProperty(assetRef, ContentModel.PROP_CONTENT, contentData);
        	
        	ContentWriter writer = null;
			OutputStream outputStream = null;
			InputStream objectDataStream = null;
			try{
	        	if(ifLargeFile){
	        		 objectDataStream = response.getEntity().getContent();
					 System.out.println("Large File to download");
					 writer = contentService.getWriter(assetRef, ContentModel.PROP_CONTENT, true);
					 outputStream = writer.getContentOutputStream();							 
		             IOUtils.copyLarge(objectDataStream, outputStream);			             
				}else{
		            byte[] imageByteArray = IOUtils.toByteArray(response.getEntity().getContent()); 
		            writer = contentService.getWriter(assetRef, ContentModel.PROP_CONTENT, true);
		            writer.putContent(new ByteArrayInputStream(imageByteArray));	            
		            System.out.println("Content Written Successfully to Stream");	
				}
			}catch(IOException io){					
				io.printStackTrace();
				 throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,"Error:"+response.getStatusLine().getReasonPhrase());
			}finally{
				 IOUtils.closeQuietly(objectDataStream);
				 if(outputStream !=null){
					 IOUtils.closeQuietly(outputStream);
				 }
				 System.out.println("Content Written Successfully to Stream");	
				 if(mimeType.contains("mp4") || mimeType.contains("quicktime")){
					 //fetch duration for mp4 and quicktime videos
					 updateMetadata(mimeType);				 
				 }
			}
        } else{
			 throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,"Error:"+response.getStatusLine().getReasonPhrase());
		}
	}
	
	public String getMimetype(String contentType,String strResourceURL){
		String mimeType =  "";		
		String fileExtension = "";
		
    	if(contentType != null){
    		mimeType = contentType.split(";")[0].trim();    	
    		System.out.println("Initial Mimetype from response:"+mimeType);
    	}else{
    		mimeType = "application/octet-stream";
    		System.out.println("Content Type is null. Set it to application/octet-stream");
    	}
    	    	
		if(mimeType.equalsIgnoreCase("application/octet-stream")){
			fileExtension = strResourceURL.substring(strResourceURL.lastIndexOf("/")+1);
			fileExtension = fileExtension.substring(fileExtension.lastIndexOf("."));			
			System.out.println("File Extension from URL is:"+fileExtension.toLowerCase());
			if(fileExtension.toLowerCase().contains("mp4")){
				mimeType = MimetypeMap.MIMETYPE_VIDEO_MP4; 
			}else if(fileExtension.toLowerCase().contains("mov")){
				mimeType = MimetypeMap.MIMETYPE_VIDEO_QUICKTIME;
			}else if(fileExtension.toLowerCase().contains("flv")){
				mimeType = MimetypeMap.MIMETYPE_VIDEO_FLV;
			}else if(fileExtension.toLowerCase().contains("mpg") || fileExtension.toLowerCase().contains("mpeg")){
				mimeType = MimetypeMap.MIMETYPE_VIDEO_MPG;
			}
			System.out.println("Mimetype from file Extension:"+mimeType);
		}
		return mimeType;
	}
	public void updateMetadata(String mimeType) throws IOException{
		double lengthInSeconds = 0.0;
		FileChannel channel = null;
		System.out.println("Update Metadata for mimetype:"+mimeType);
		try {					
			if(contentService==null){					
				contentService = this.registry.getContentService();
			}
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");				
			Date date = new Date();
			System.out.println("Sleep timer starts at"+dateFormat.format(date));				
			Thread.sleep(8000);
			date = new Date();
			System.out.println("Sleep timer ends at"+dateFormat.format(date));
			
			ContentReader reader = contentService.getReader(assetRef, ContentModel.PROP_CONTENT);			
			if(reader!=null){
				channel = reader.getFileChannel();
				IsoFile isoFile;							
				DataSource fd = new FileDataSourceImpl(channel);					
		        isoFile = new IsoFile(fd);			        
		        lengthInSeconds = (double) 
		        		isoFile.getMovieBox().getMovieHeaderBox().getDuration() /
		                isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
		        channel.close();					
		        System.out.println("Fetched Duration:"+lengthInSeconds);			        
			}else{
				System.out.println("Error Reading content. Content Reader returns null.");
			}			
			
		} catch (Exception e) {
			System.out.println(e);			
		}finally{
			nodeService.setProperty(assetRef, UCMSPublishingModel.PROPERTY_UCM_DURATIONSECS, lengthInSeconds);	
			System.out.println("Property Updated successfully");					
		}
	}	
}

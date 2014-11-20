package com.kapx.ucms.xml;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="publisher-upload-manifest")
public class PublisherUploadManifest {
	String publisherID;
	String preparer = "ucms_manager";
	boolean reportSuccess = false;
	List<Notify> notify;
	Asset asset;
	Callback callback;
	Title title;
	
	@XmlElement
	public void setNotify(List<Notify> notify) {
		this.notify = notify;		
	}
	
	public List<Notify> getNotify(){
		return notify;
	}
	
	@XmlElement
	public void setAsset(Asset asset) {
		this.asset = asset;		
	}
	
	public Asset getAsset(){
		return asset;
	}
	
	@XmlElement
	public void setTitle(Title title) {
		this.title = title;		
	}
	
	public Title getTitle(){
		return title;
	}
	
	@XmlElement
	public void setCallback(Callback callback) {
		this.callback = callback;		
	}
	
	public Callback getCallback(){
		return callback;
	}
	
	@XmlAttribute(name="publisher-id")
	public void setPublisherID(String publisherID) {
		this.publisherID = publisherID;
	}
	
	public String getPublisherID(){
		return publisherID;
	}
	
	@XmlAttribute
	public void setPreparer(String preparer) {
		this.preparer = preparer;
	}
	
	public String getPreparer(){
		return preparer;
	}
	
	@XmlAttribute(name="report-success")
	public void setReportSuccess(boolean reportSuccess) {
		this.reportSuccess = reportSuccess;
	}
	
	public boolean getReportSuccess(){
		return reportSuccess;
	}

}

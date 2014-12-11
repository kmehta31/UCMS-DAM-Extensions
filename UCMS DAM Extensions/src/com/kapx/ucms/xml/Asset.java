package com.kapx.ucms.xml;
import javax.xml.bind.annotation.XmlAttribute;
//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;

public class Asset {
	String filename;
	String refid;
	String size;
	String hashcode;
	String displayName;
	String type;
	String encodeTo;	
	boolean encodeMultiple;

	@XmlAttribute
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getFilename(){
		return filename;
	}
	
	@XmlAttribute
	public void setRefid(String refid) {
		this.refid = refid;
	}
	
	public String getRefid(){
		return refid;
	}
	
	@XmlAttribute
	public void setSize(String size) {
		this.size = size;
	}
	
	public String getSize(){
		return size;
	}
	
	@XmlAttribute(name="hash-code")
	public void setHashcode(String hashcode) {
		this.hashcode = hashcode;
	}
	
	public String getHashcode() {
		return hashcode;
	}
	
	@XmlAttribute(name="encode-to")
	public void setEncodeTo(String encodeTo) {
		this.encodeTo = encodeTo;
	}
	
	public String getEncodeTo() {
		return encodeTo;
	}
	
	public boolean getEncodeMultiple() {
		return encodeMultiple;
	}
	
	@XmlAttribute(name="encode-multiple")
	public void setEncodeMultiple(boolean encodeMultiple) {
		this.encodeMultiple = encodeMultiple;
	}	
	
	@XmlAttribute(name="display-name")
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getDisplayName(){
		return displayName;
	}
	
	@XmlAttribute
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType(){
		return type;
	}
}

package com.kapx.ucms.xml;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

public class Title {
	
	String name;
	String refid;	
	String videoFullRefid;
	String shortDesc;
	List<String> tags;
	boolean active;
	
	@XmlAttribute
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	@XmlAttribute
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public boolean getActive(){
		return active;
	}
	
	@XmlAttribute
	public void setRefid(String refid) {
		this.refid = refid;
	}
	
	public String getRefid(){
		return refid;
	}
	
	@XmlAttribute(name="video-full-refid")
	public void setVideoFullRefid(String videoFullRefid) {
		this.videoFullRefid = videoFullRefid;
	}
	
	public String getVideoFullRefid(){
		return videoFullRefid;
	}
	
	@XmlElement(name="short-description")
	public String getShortDesc() {
		return shortDesc;
	}
 
	
	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}	
	
	public List<String> getTags() {
		return tags;
	} 
	
	@XmlElement(name="tag")
	public void setTags(List<String> tags) {
		this.tags = tags;
	}	
	
}

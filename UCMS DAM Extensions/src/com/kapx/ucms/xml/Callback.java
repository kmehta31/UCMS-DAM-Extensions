package com.kapx.ucms.xml;
import javax.xml.bind.annotation.XmlAttribute;
//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;

public class Callback {
	String entityurl;
	
	@XmlAttribute(name="entity-url")
	public void setEntityurl(String entityurl) {
		this.entityurl = entityurl;
	}
	
	public String getEntityurl(){
		return entityurl;
	}
}

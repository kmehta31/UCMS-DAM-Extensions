package com.kapx.ucms.xml;
import javax.xml.bind.annotation.XmlAttribute;
//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;

public class Notify{	
	String email;
	
	@XmlAttribute
	public void setEmail(String email) {
		this.email = email;
	}	
	
	public String getEmail(){
		return email;
	}
}

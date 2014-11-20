package com.kapx.ucms;
import java.io.IOException;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

 
public class UCMSServiceHelper {
	private final static Log log = LogFactory.getLog(UCMSServiceHelper.class);	
	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";	
	private static final String AUTHPREFIX = "KAP";
	private static final String REQUESTTYPE_GET	= "GET";
	private static final String REQUESTTYPE_POST	= "POST";
	private static final String CONTENTTYPE	= "application/json";
	private static final String NEWLINECHAR	= "\n";
	private static final String KAPHEADERKEY = "X-Kap-Date:";	
	private Properties props;		
	
	public HttpGet createUCMSHeader(String strResourceURL, String UCMS_DOMAIN_IDENTIFIER){
		props = new Properties();		
		HttpGet httpGet = null;
		try {
			props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
			
			String ACCESSKEY = props.getProperty("ucmskey").trim();
			String SECRETKEY = props.getProperty("ucmssecret").trim();
			
			//String UCMSTESTURL	= "http://4.kaplandevucms.appspot.com/file/ag9zfmthcGxhbmRldnVjbXNyEQsSBEZpbGUYgICAgKawggoM";
			//String UCMSTESURI = "/file/ag9zfmthcGxhbmRldnVjbXNyEQsSBEZpbGUYgICAgKawggoM";
			String UCMURL = strResourceURL.trim();
			String UCMURI = "";
			
			String[] temp	= strResourceURL.split(UCMS_DOMAIN_IDENTIFIER.trim(), 0);
			System.out.println("Split URL:"+temp[0]);
			UCMURI	=	temp[1];	
			System.out.println("Split URI:"+UCMURI);		
			
			
			String TIMESTAMP    =  timestamp();
			String KAPHEADER = KAPHEADERKEY.toLowerCase()+TIMESTAMP;
			
			httpGet = new HttpGet(UCMURL);		
			String strSignature	= 	REQUESTTYPE_GET +
									NEWLINECHAR +	
									NEWLINECHAR +	
									CONTENTTYPE +
									NEWLINECHAR +	
									NEWLINECHAR +	
									KAPHEADER+
									NEWLINECHAR +							  
									UCMURI;
			
			System.out.println("Test URL:"+UCMURL);
			System.out.println("KAP Date Header:"+KAPHEADER);
			System.out.println("Signrature starts:\n"+strSignature);
			System.out.println("Signrature ends:");		
			String strSignatureHash = "";			

			// get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(SECRETKEY.getBytes(), HMAC_SHA1_ALGORITHM);			
			
			// get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);

			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(strSignature.getBytes());
			byte[] encoded = Base64.encodeBase64(rawHmac);
			strSignatureHash =  new String(encoded);
			System.out.println("Signature HASH:"+strSignatureHash);
			String HEADER = AUTHPREFIX + " " + ACCESSKEY +":" + strSignatureHash;
			System.out.println("Auth Header:"+HEADER);			
			httpGet.setHeader("X-Kap-Date", TIMESTAMP);
			httpGet.setHeader("Accept", CONTENTTYPE);
			httpGet.setHeader("Content-Type", CONTENTTYPE);
			httpGet.setHeader("Authorization", HEADER);			
			}catch (IOException e1) {				
				e1.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}	     
		return httpGet;
	}
	
	public HttpPost createHttpPostUCMSHeader(String strResourceURL, String UCMS_DOMAIN_IDENTIFIER){
		props = new Properties();		
		HttpPost httpPost = null;
		try {
			props.load(this.getClass().getResourceAsStream("/com/kapx/ucms/pace.properties"));
			
			String ACCESSKEY = props.getProperty("ucmskey").trim();
			String SECRETKEY = props.getProperty("ucmssecret").trim();
			
			//String UCMSTESTURL	= "http://4.kaplandevucms.appspot.com/file/ag9zfmthcGxhbmRldnVjbXNyEQsSBEZpbGUYgICAgKawggoM";
			//String UCMSTESURI = "/file/ag9zfmthcGxhbmRldnVjbXNyEQsSBEZpbGUYgICAgKawggoM";
			String UCMURL = strResourceURL.trim();
			String UCMURI = "";
			
			String[] temp	= strResourceURL.split(UCMS_DOMAIN_IDENTIFIER.trim(), 0);
			System.out.println("Split URL:"+temp[0]);
			UCMURI	=	temp[1];	
			System.out.println("Split URI:"+UCMURI);		
			
			
			String TIMESTAMP    =  timestamp();
			String KAPHEADER = KAPHEADERKEY.toLowerCase()+TIMESTAMP;
			
			httpPost = new HttpPost(UCMURL);		
			String strSignature	= 	REQUESTTYPE_POST +
									NEWLINECHAR +	
									NEWLINECHAR +	
									CONTENTTYPE +
									NEWLINECHAR +	
									NEWLINECHAR +	
									KAPHEADER+
									NEWLINECHAR +							  
									UCMURI;
			
			System.out.println("Test URL:"+UCMURL);
			System.out.println("KAP Date Header:"+KAPHEADER);
			System.out.println("Signrature starts:\n"+strSignature);
			System.out.println("Signrature ends:");		
			String strSignatureHash = "";			

			// get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(SECRETKEY.getBytes(), HMAC_SHA1_ALGORITHM);			
			
			// get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);

			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(strSignature.getBytes());
			byte[] encoded = Base64.encodeBase64(rawHmac);
			strSignatureHash =  new String(encoded);
			System.out.println("Signature HASH:"+strSignatureHash);
			String HEADER = AUTHPREFIX + " " + ACCESSKEY +":" + strSignatureHash;
			System.out.println("Auth Header:"+HEADER);			
			httpPost.setHeader("X-Kap-Date", TIMESTAMP);
			httpPost.setHeader("Accept", CONTENTTYPE);
			httpPost.setHeader("Content-Type", CONTENTTYPE);
			httpPost.setHeader("Authorization", HEADER);			
			}catch (IOException e1) {				
				e1.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}	     
		return httpPost;
	}
	
	private String timestamp() {
	    String timestamp = null;
	    Calendar cal = Calendar.getInstance();
	    DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z");
	    dfm.setTimeZone(TimeZone.getTimeZone("EST"));
	    timestamp = dfm.format(cal.getTime());
	    return timestamp;
	  }
}
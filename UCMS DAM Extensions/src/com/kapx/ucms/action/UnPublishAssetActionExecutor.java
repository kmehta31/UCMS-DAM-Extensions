package com.kapx.ucms.action;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.UCMSPublishingModel;

public class UnPublishAssetActionExecutor extends ActionExecuterAbstractBase{
   public static final String NAME = "unPublishAsset";
   private NodeService nodeService;
   public void setNodeService(NodeService nodeService){
      this.nodeService = nodeService;
   }   
   protected void executeImpl(Action action, NodeRef actionedUponNodeRef){
	   System.out.println("Action Listener for Unpublishing Asset from Brightcove");	  
	   if (this.nodeService.exists(actionedUponNodeRef) == true){
		   HttpUCMSClient ucmsClient = new HttpUCMSClient();		   
		   try {			  
			   String TARGET = (String) nodeService.getProperty(actionedUponNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHTARGET);
			   if(TARGET.length() > 0){				  
				   String message = ucmsClient.unPublishToChannel(actionedUponNodeRef,TARGET);
				   if(!message.equalsIgnoreCase("success")){
					   System.out.println("Error Unpublishing item from Brightcove:"+message);				
				   }else{
					   System.out.println("Item unpublished from Brightcove successfully..");					
				   }
			   }		   
		   }catch (IOException | URISyntaxException e) {				
			   e.printStackTrace();
		   }  		
	   }      
  }
   
   @Override
   protected void addParameterDefinitions(List<ParameterDefinition> paramList)
   {
      // there are no parameters
   }



}
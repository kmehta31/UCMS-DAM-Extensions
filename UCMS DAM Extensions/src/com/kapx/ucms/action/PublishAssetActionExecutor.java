package com.kapx.ucms.action;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import com.kapx.ucms.HttpUCMSClient;
import com.kapx.ucms.model.UCMSPublishingModel;

public class PublishAssetActionExecutor extends ActionExecuterAbstractBase
{
   public static final String NAME = "publishAsset";
   private NodeService nodeService;
   public void setNodeService(NodeService nodeService) 
   {
      this.nodeService = nodeService;
   }   
   protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
   {
	   System.out.println("Action Listener for Publish Asset (On Create/Update)");	   
	   //Sleep for 8 Seconds before publishing when node is just created. To allow enough time for indexing.	   
	   if(!nodeService.hasAspect(actionedUponNodeRef, UCMSPublishingModel.ASPECT_IS_READY_TO_PUBLISH)){
		   try {			   
			   DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			   Date date = new Date();
			   System.out.println("Sleep timer starts at"+dateFormat.format(date));
			   Thread.sleep(5000);
			   date = new Date();
			   System.out.println("Sleep timer ends at"+dateFormat.format(date));
		   } catch (InterruptedException e1) {		
			   e1.printStackTrace();
		   }
	   }
	   if (this.nodeService.exists(actionedUponNodeRef) == true){		   		   
		   try{
			   HttpUCMSClient ucmsClient = new HttpUCMSClient();			   
			   String TARGET = (String) nodeService.getProperty(actionedUponNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHTARGET);			   
			   ucmsClient.publishToChannel(actionedUponNodeRef,TARGET);				   
			   
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
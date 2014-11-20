package com.kapx.ucms;

import java.io.IOException;
import java.net.URISyntaxException;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import com.kapx.ucms.model.UCMSPublishingModel;

public class CreateAssetBehavior implements NodeServicePolicies.OnCreateNodePolicy  {
	
	private PolicyComponent policyComponent;
	private Behaviour onCreateNode;
	private ServiceRegistry registry; 
	
	// for Spring injection 
	public void setServiceRegistry(ServiceRegistry registry) {
		this.registry = registry;
	}
	
	public void init() {
		this.onCreateNode = new JavaBehaviour(this,"onCreateNode",NotificationFrequency.TRANSACTION_COMMIT);
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI,"onCreateNode"), 
				QName.createQName(UCMSPublishingModel.UCMS_NAMESPACE,UCMSPublishingModel.DIGITAL_ASSET_TYPE), this.onCreateNode);
	}	
	

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	

	@Override
	public void onCreateNode(ChildAssociationRef childRef) {		
		NodeRef childNodeRef = childRef.getChildRef();
		NodeRef parentNodeRef = childRef.getParentRef();
		System.out.println("Child Node:"+childNodeRef);
		System.out.println("Parent Node:"+parentNodeRef);
		NodeService nodeService = registry.getNodeService();
		String UCMSID = (String) nodeService.getProperty(childNodeRef, UCMSPublishingModel.PROPERTY_UCM_ID);
		String TARGET = (String) nodeService.getProperty(childNodeRef, UCMSPublishingModel.PROPERTY_UCM_PUBLISHTARGET);
		/*System.out.println("UCMS ID from behavior:"+UCMSID);
		System.out.println("UCMS ID from TARGET:"+TARGET);
		if(StringUtils.isNotEmpty(UCMSID) && StringUtils.isNotEmpty(TARGET)){
			HttpUCMSClient ucmsClient = new HttpUCMSClient();
			HttpResponse httpResponse;
			try {
				String message = ucmsClient.publishToChannel(childNodeRef,TARGET);							
				System.out.println("Message"+message);
			} catch (IOException | URISyntaxException e) {				
				e.printStackTrace();
			}
		}*/
	}

}

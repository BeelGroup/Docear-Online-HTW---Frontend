package models.frontend.formdata;

import play.data.validation.Constraints.Required;

public class ChangeNodeData {
	@Required
	private String nodeId;
	
	@Required
	private String attributeValueMapJson;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getAttributeValueMapJson() {
		return attributeValueMapJson;
	}

	public void setAttributeValueMapJson(String attributeValueMapJson) {
		this.attributeValueMapJson = attributeValueMapJson;
	}


	
}

package models.frontend.formdata;

import play.data.validation.Constraints.Required;

public class RemoveNodeData {

	@Required
	private String nodeId;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}

package models.frontend;

import play.data.validation.Constraints.Required;

public class ChangeNodeData {
	@Required
	private String nodeJson;

	public String getNodeJson() {
		return nodeJson;
	}

	public void setNodeJson(String nodeJson) {
		this.nodeJson = nodeJson;
	}
	
	
}

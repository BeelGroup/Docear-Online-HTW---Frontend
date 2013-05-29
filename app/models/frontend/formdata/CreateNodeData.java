package models.frontend.formdata;

import play.data.validation.Constraints.Required;

public class CreateNodeData {
	@Required
	private String parentNodeId;
	
	public String getParentNodeId() {
		return parentNodeId;
	}

	public void setParentNodeId(String parentNodeId) {
		this.parentNodeId = parentNodeId;
	}
}

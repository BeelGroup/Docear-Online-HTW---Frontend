package models.frontend.formdata;

import play.data.validation.Constraints.Required;

public class CreateNodeData {
	@Required
	private String parentNodeId;
	
	private String side;
	
	public String getParentNodeId() {
		return parentNodeId;
	}

	public void setParentNodeId(String parentNodeId) {
		this.parentNodeId = parentNodeId;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}
	
}

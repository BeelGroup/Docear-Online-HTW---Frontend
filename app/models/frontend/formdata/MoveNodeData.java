package models.frontend.formdata;

import play.data.validation.Constraints.Required;

public class MoveNodeData {
	@Required
	private String newParentNodeId;
	@Required
	private String nodetoMoveId;
	@Required
	private Integer newIndex;

	public String getNewParentNodeId() {
		return newParentNodeId;
	}

	public void setNewParentNodeId(String newParentNodeId) {
		this.newParentNodeId = newParentNodeId;
	}

	public String getNodetoMoveId() {
		return nodetoMoveId;
	}

	public void setNodetoMoveId(String nodetoMoveId) {
		this.nodetoMoveId = nodetoMoveId;
	}

	public Integer getNewIndex() {
		return newIndex;
	}

	public void setNewIndex(Integer newIndex) {
		this.newIndex = newIndex;
	}

}

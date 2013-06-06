package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class MoveData {
	@Required
	private String currentPath;
	
	@Required
	private String moveToPath;

	public String getCurrentPath() {
		return currentPath;
	}

	public void setCurrentPath(String currentPath) {
		this.currentPath = currentPath;
	}

	public String getMoveToPath() {
		return moveToPath;
	}

	public void setMoveToPath(String moveToPath) {
		this.moveToPath = moveToPath;
	}
	
	
}

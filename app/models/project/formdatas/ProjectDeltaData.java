package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class ProjectDeltaData {
	@Required
	private String projectId;
	@Required
	private String cursor;

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		this.cursor = cursor;
	}
}

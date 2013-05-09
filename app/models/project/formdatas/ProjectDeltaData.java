package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class ProjectDeltaData {
	@Required
	private Long projectId;
	@Required
	private String cursor;

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		this.cursor = cursor;
	}
}

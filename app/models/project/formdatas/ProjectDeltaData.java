package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class ProjectDeltaData {
	@Required
	private Long projectRevision;

	public Long getProjectRevision() {
		return projectRevision;
	}

	public void setProjectRevision(Long projectRevision) {
		this.projectRevision = projectRevision;
	}

}

package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class CreateFolderData {
	@Required
	private Long projectId;
	@Required
	private String path;

	private String locale;


	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

}

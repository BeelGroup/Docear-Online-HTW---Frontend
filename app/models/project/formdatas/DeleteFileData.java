package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class DeleteFileData {
	@Required
	private String path;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}

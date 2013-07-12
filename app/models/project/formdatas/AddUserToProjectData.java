package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class AddUserToProjectData {

	@Required
	private String username;

	public AddUserToProjectData() {
		super();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}

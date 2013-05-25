package models.project.formdatas;

public class RemoveUserFromProjectData {

	private String projectId;
	private String username;

	public RemoveUserFromProjectData() { 
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}

package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class CreateProjectData {
	@Required
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
}

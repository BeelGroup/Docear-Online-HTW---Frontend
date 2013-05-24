package models.project.formdatas;

import play.data.validation.Constraints.Required;

public class ProjectDeltaData {
	@Required
	private Long cursor;

	public Long getCursor() {
		return cursor;
	}

	public void setCursor(Long cursor) {
		this.cursor = cursor;
	}
}

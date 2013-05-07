package models.project;

import org.joda.time.DateTime;

public class ProjectFile extends ProjectEntry {

	private final String mime_type;

	public ProjectFile(String path, Long bytes, DateTime modified, String icon, String projectId, String rev, String mime_type) {
		super(path, bytes, modified, false, icon, projectId, rev);
		this.mime_type = mime_type;
	}

	public String getMime_type() {
		return mime_type;
	}

}

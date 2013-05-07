package models.project;

import org.joda.time.DateTime;

public abstract class ProjectEntry {

	private final String path;
	private final String size;
	private final Long bytes;
	private final String modified;
	private final boolean is_dir;
	private final String icon;
	private final String projectId;
	private final String rev;

	public ProjectEntry(String path, Long bytes, DateTime modified, boolean is_dir, String icon, String projectId, String rev) {
		this.path = path;
		this.bytes = bytes;
		this.size = bytes + " bytes";
		this.modified = modified.toString();
		this.is_dir = is_dir;
		this.icon = icon;
		this.projectId = projectId;
		this.rev = rev;
	}

	public String getPath() {
		return path;
	}

	public String getSize() {
		return size;
	}

	public Long getBytes() {
		return bytes;
	}

	public String getModified() {
		return modified;
	}

	public boolean isIs_dir() {
		return is_dir;
	}

	public String getIcon() {
		return icon;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getRev() {
		return rev;
	}
}

package models.project;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.joda.time.DateTime;

@JsonSerialize(include=Inclusion.NON_NULL)
public class ProjectFolder extends ProjectEntry {

	private List<ProjectEntry> contents = null;

	public ProjectFolder(String path, Long bytes, DateTime modified, String icon, String projectId, String rev) {
		super(path, bytes, modified, true, icon, projectId, rev);
	}

	public List<ProjectEntry> getContents() {
		return contents;
	}

	public void setContents(List<ProjectEntry> contents) {
		this.contents = contents;
	}
}

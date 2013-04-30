package models.project.updates;

import models.backend.ProjectEntry;

public class AddEntryUpdate extends ProjectUpdate {
	private final String parentFolder;
	private final ProjectEntry newEntry;

	public AddEntryUpdate(String username, String source, String parentFolder, ProjectEntry newEntry) {
		super(Type.AddEntry, username, source);
		this.parentFolder = parentFolder;
		this.newEntry = newEntry;
	}

	public String getParentFolder() {
		return parentFolder;
	}

	public ProjectEntry getNewEntry() {
		return newEntry;
	}
}

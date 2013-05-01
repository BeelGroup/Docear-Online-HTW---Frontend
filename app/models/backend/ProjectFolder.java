package models.backend;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectFolder extends ProjectEntry {

	private final List<ProjectEntry> files;

	public ProjectFolder(String name, String path, File folder) {
		super(name, Type.Folder, path);
		files = new ArrayList<ProjectEntry>();
		loadFolderStructure(folder);
	}

	private void loadFolderStructure(File folder) {
		for (File file : folder.listFiles()) {
			final String filename = file.getName();
			final String path = this.getPath() + "/" + filename;

			if (file.isDirectory()) {
				files.add(new ProjectFolder(filename, path, file));
			} else {
				files.add(new ProjectFile(filename, path));
			}
		}
	}

	public List<ProjectEntry> getFiles() {
		return files;
	}

}

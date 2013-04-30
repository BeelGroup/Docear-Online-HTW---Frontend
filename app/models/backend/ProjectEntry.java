package models.backend;

public abstract class ProjectEntry {
	public enum Type {
		Folder, File
	};

	private final String name;
	private final Type type;
	private final String path;

	public ProjectEntry(String name, Type type, String path) {
		super();
		this.name = name;
		this.type = type;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public String getPath() {
		return path;
	}

}

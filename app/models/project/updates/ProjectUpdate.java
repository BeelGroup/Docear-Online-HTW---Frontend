package models.project.updates;

import org.joda.time.DateTime;

public abstract class ProjectUpdate {
	public static enum Type {
		AddEntry, ChangeEntry, MoveEntry, DeleteEntry
	}

	private final Type type;
	private final String username;
	private final String source;
	private final DateTime time;

	public ProjectUpdate(Type type, String username, String source) {
		super();
		this.type = type;
		this.username = username;
		this.source = source;
		this.time = DateTime.now();
	}

	public Type getType() {
		return type;
	}

	public String getUsername() {
		return username;
	}

	public String getSource() {
		return source;
	}

	public DateTime getTime() {
		return time;
	}

}

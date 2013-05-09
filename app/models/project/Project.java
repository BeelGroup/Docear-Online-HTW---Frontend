package models.project;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import models.project.updates.AddEntryUpdate;
import models.project.updates.ProjectUpdate;
import models.project.updates.ProjectUpdate.Type;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class Project {
	private final Long id;
	private final String name;
	private final Long revision;
	private final ProjectEntry root;

	public Project(Long id, File projectFolder) {
		super();
		this.id = id;

		final File metaFolder = getFileFromFolder(projectFolder, "_projectmetadata");
		final File configFile = getFileFromFolder(metaFolder, "config.json");
		final File updatesFolder = getFileFromFolder(metaFolder, "updates");

		JsonNode config;
		try {
			config = new ObjectMapper().readTree(configFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this.name = config.get("name").asText();
		this.revision = (long) updatesFolder.list().length;
		root = null;//new ProjectFolder(name, "", filesFolder);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Long getRevision() {
		return revision;
	}

	public ProjectEntry getRoot() {
		return root;
	}

	private static File getFileFromFolder(final File folder, final String filename) {
		return folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.equals(filename);
			}
		})[0];
	}

	public static File getUpdatesFolder(File projectFolder) {
		final File metaFolder = getFileFromFolder(projectFolder, "_projectmetadata");
		return getFileFromFolder(metaFolder, "updates");
	}

	public static List<ProjectUpdate> getUpdatesSince(File projectFolder, final int sinceRevision) {
		final File updatesFolder = getUpdatesFolder(projectFolder);
		final File[] updateFiles = updatesFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				final int revisionNumber = Integer.parseInt(name);
				return revisionNumber > sinceRevision;
			}
		});
		final ObjectMapper mapper = new ObjectMapper();
		final List<ProjectUpdate> updates = new ArrayList<ProjectUpdate>();
		for (File updateFile : updateFiles) {
			try {
				final JsonNode updateAsJson = mapper.readTree(updateFile);
				final ProjectUpdate.Type type = ProjectUpdate.Type.valueOf(updateAsJson.get("type").asText());
				ProjectUpdate update = null;
				if (type == Type.AddEntry)
					update = mapper.readValue(updateAsJson, AddEntryUpdate.class);

				updates.add(update);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return updates;
	}

	public static String getUpdatesSinceJson(File projectFolder, final int sinceRevision) {
		final File updatesFolder = getUpdatesFolder(projectFolder);
		final File[] updateFiles = updatesFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				final int revisionNumber = Integer.parseInt(name);
				return revisionNumber > sinceRevision;
			}
		});
		final ObjectMapper mapper = new ObjectMapper();
		try {
			final List<JsonNode> updates = new ArrayList<JsonNode>();
			for (File updateFile : updateFiles) {

				updates.add(mapper.readTree(updateFile));

			}

			return mapper.writeValueAsString(updates);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

package services.backend.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import models.backend.exceptions.sendResult.SendResultException;
import models.project.ProjectEntry;
import models.project.ProjectFile;
import models.project.ProjectFolder;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;

@Profile("projectMock")
@Component
public class MockProjectService implements ProjectService {
	private final ObjectMapper mapper = new ObjectMapper();

	// @Override
	// public Promise<JsonNode> listProject(Long projectId) {
	// try {
	// final File projectRoot = new
	// File(Play.application().resource("rest/v1/project/" +
	// projectId).toURI());
	// final Project project = new Project(projectId, projectRoot);
	// return Promise.pure(mapper.valueToTree(project));
	// } catch (URISyntaxException e) {
	// throw new RuntimeException(e);
	// }
	// }

	@Override
	public Promise<InputStream> getFile(Long projectId, String path) {
		if (!path.startsWith("/"))
			path = "/" + path;

		return Promise.pure(Play.application().resourceAsStream("rest/v1/project/" + projectId + "/files" + path));
	}

	@Override
	public Promise<JsonNode> putFile(Long projectId, String path, byte[] content) {
		path = addLeadingSlash(path);
		final String pathOfParentFolder = path.substring(0, path.lastIndexOf("/"));
		final String filename = path.substring(path.lastIndexOf("/"));
		OutputStream out = null;
		try {
			final File parentFolder = new File(Play.application().resource("rest/v1/project/" + projectId + "/files" + pathOfParentFolder).toURI());
			if (!parentFolder.exists())
				throw new SendResultException("Parentfolder not present", 400);
			final File newfile = new File(parentFolder.getAbsolutePath() + "/" + filename);

			Logger.debug(newfile.getAbsolutePath());
			if (newfile.exists())
				newfile.delete();

			newfile.createNewFile();
			out = new FileOutputStream(newfile);
			IOUtils.write(content, out);

			final ProjectEntry pf = metadataIntern(projectId, path, false);
			return Promise.pure(mapper.valueToTree(pf));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	private String addLeadingSlash(String path) {
		return path.startsWith("/") ? path : "/" + path;
	}

	@Override
	public Promise<JsonNode> metadata(Long projectId, String path) {
		return Promise.pure(new ObjectMapper().valueToTree(metadataIntern(projectId, path, true)));
	}

	@Override
	public Promise<JsonNode> createFolder(Long projectId, String path) {
		path = addLeadingSlash(path);
		final String pathOfParentFolder = path.substring(0, path.lastIndexOf("/"));
		final String folderName = path.substring(path.lastIndexOf("/"));
		try {
			final File parentFolder = new File(Play.application().resource("rest/v1/project/" + projectId + "/files" + pathOfParentFolder).toURI());
			if (!parentFolder.exists())
				throw new SendResultException("Parentfolder not present", 400);
			final File newFolder = new File(parentFolder.getAbsolutePath() + "/" + folderName);
			Logger.debug(newFolder.getAbsolutePath());
			if (newFolder.exists())
				throw new SendResultException("Folder is already present", 400);

			newFolder.mkdirs();
			final ProjectEntry pf = metadataIntern(projectId, path, false);
			return Promise.pure(new ObjectMapper().valueToTree(pf));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private ProjectEntry metadataIntern(Long projectId, String path, boolean loadContents) {
		path = addLeadingSlash(path);
		ProjectEntry result = null;
		try {
			final File entity = new File(Play.application().resource("rest/v1/project/" + projectId + "/files" + path).toURI());
			if (entity.isDirectory()) {
				result = folderMetadata(projectId, entity, loadContents);
			} else {
				result = fileMetadata(projectId, entity);
			}
			return result;

		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private ProjectEntry folderMetadata(Long projectId, File folder, boolean loadContents) {
		final String path = uriToPath(projectId, folder.getAbsolutePath());

		final DateTime modified = new DateTime(folder.lastModified());
		final ProjectFolder pf = new ProjectFolder(path, 0L, modified, "", projectId + "", "ef314d");

		if (loadContents) {
			final List<ProjectEntry> folderContents = new ArrayList<ProjectEntry>();
			for (File file : folder.listFiles()) {
				folderContents.add(metadataIntern(projectId, path + "/" + file.getName(), false));
			}
			pf.setContents(folderContents);
		}

		return pf;
	}

	private ProjectEntry fileMetadata(Long projectId, File file) {
		final String path = uriToPath(projectId, file.getAbsolutePath());

		final DateTime modified = new DateTime(file.lastModified());
		final Long size = file.length();
		final ProjectFile pf = new ProjectFile(path, size, modified, "", projectId + "", "ef314d", "text/plain");

		return pf;
	}

	private String uriToPath(Long projectId, String uriPath) {
		uriPath = uriPath.replace("\\", "/");
		final String seperator = "/";
		final String searchString = seperator + projectId + seperator + "files";
		Logger.debug("pId: " + projectId + "sString: " + searchString);
		final String substring = uriPath.substring(uriPath.indexOf(searchString) + searchString.length());
		Logger.debug(substring);
		return substring;
	}

	@Override
	public Promise<Boolean> listenIfUpdateOccurs(Long projectId) {
		Promise<Boolean> promise = Akka.future(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Thread.sleep((long) (Math.random() * 30000));
				return true;
			}
		});

		return promise;
	}

	@Override
	public Promise<String> versionDelta(Long projectId, String cursor) {
		final int sinceRevision = Integer.parseInt(cursor);
		try {
			final File updatesFolder = new File(Play.application().resource("rest/v1/project/" + projectId + "/_projectmetadata/updates").toURI());
			final File[] updateFiles = updatesFolder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String name) {
					final int revisionNumber = Integer.parseInt(name);
					return revisionNumber > sinceRevision;
				}
			});
			final ObjectMapper mapper = new ObjectMapper();
			final List<JsonNode> updates = new ArrayList<JsonNode>();
			for (File updateFile : updateFiles) {

				updates.add(mapper.readTree(updateFile));

			}

			return Promise.pure(mapper.writeValueAsString(updates));

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
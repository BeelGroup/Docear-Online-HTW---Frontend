package services.backend.project;

import static services.backend.project.filestore.PathFactory.path;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import models.backend.exceptions.sendResult.NotFoundException;
import models.project.persistance.Changes;
import models.project.persistance.EntityCursor;
import models.project.persistance.FileIndexStore;
import models.project.persistance.FileMetaData;
import models.project.persistance.Project;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import services.backend.project.filestore.FileStore;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService implements ProjectService {
	/**
	 * TODO UpdateCallables could cause performance issue. 
	 * Implementing an Actor for the action might be better.
	 */
	private final Map<String, List<UpdateCallable>> projectListenersMap = new HashMap<String, List<UpdateCallable>>();

	@Autowired
	private FileStore fileStore;

	@Autowired
	private FileIndexStore fileIndexStore;

	@Override
	public Promise<JsonNode> createProject(String username, String name) throws IOException {
		final Project project = fileIndexStore.createProject(name, username);
		return Promise.pure(new ObjectMapper().valueToTree(project));
	}

	@Override
	public Promise<Boolean> addUserToProject(String projectId, String usernameToAdd) throws IOException {
		fileIndexStore.addUserToProject(projectId, usernameToAdd);
		return Promise.pure(true);
	}

	@Override
	public Promise<Boolean> removeUserFromProject(String projectId, String usernameToRemove) throws IOException {
		fileIndexStore.removeUserFromProject(projectId, usernameToRemove);
		return Promise.pure(true);
	}

	@Override
	public Promise<JsonNode> getProjectById(String projectId) throws IOException {
		final Project project = fileIndexStore.findProjectById(projectId);
		return Promise.pure(new ObjectMapper().valueToTree(project));
	}

	@Override
	public Promise<JsonNode> getProjectsFromUser(String username) throws IOException {
		final EntityCursor<Project> projects = fileIndexStore.findProjectsFromUser(username);
		final List<Project> projectList = convertEntityCursorToList(projects);
		return Promise.pure(new ObjectMapper().valueToTree(projectList));
	}

	@Override
	public F.Promise<InputStream> getFile(String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		try {
			// look for file in fileIndexStore
			final FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
			if (metadata == null) {
				throw new NotFoundException("File not found!");
			}

			final String fileHash = metadata.getHash();

			return Promise.pure((InputStream) fileStore.open(path().hash(fileHash).zipped()));

		} catch (FileNotFoundException e) {
			throw new NotFoundException("File not found!", e);
		}
	}

	@Override
	public F.Promise<JsonNode> metadata(String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		final FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
		if (metadata == null) {
			throw new NotFoundException("File not found!");
		}

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode metadataJson = (ObjectNode) mapper.valueToTree(metadata);

		// get children for dir
		if (metadata.isDir()) {
			List<FileMetaData> childrenData = new ArrayList<FileMetaData>();
			final EntityCursor<FileMetaData> iterable = fileIndexStore.getMetaDataOfDirectChildren(projectId, path, 5000);
			Iterator<FileMetaData> it = iterable.iterator();
			while (it.hasNext()) { // only add non deleted entries
				final FileMetaData childMetadata = it.next();
				if (!childMetadata.isDeleted())
					childrenData.add(childMetadata);
			}
			final JsonNode contentsJson = mapper.valueToTree(childrenData);
			metadataJson.put("contents", contentsJson);
		}
		return Promise.pure((JsonNode) metadataJson);
	}

	@Override
	public F.Promise<JsonNode> createFolder(String projectId, String path) throws IOException {
		path = addLeadingSlash(path);
		upsertFoldersInPath(projectId, path);

		final FileMetaData metadata = FileMetaData.folder(path, false);
		fileIndexStore.upsertFile(projectId, metadata);

		callListenersForChange(projectId);

		return Promise.pure(new ObjectMapper().valueToTree(metadata));
	}

	private void upsertFoldersInPath(String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		// check that root exists
		if (fileIndexStore.getMetaData(projectId, "/") == null) {
			fileIndexStore.upsertFile(projectId, FileMetaData.folder("/", false));
		}
		final String[] folders = path.split("/");
		// dont check before first slash, dont check last part (new resoource)
		String currentPath = "";
		for (int i = 1; i < folders.length - 1; i++) {
			currentPath += "/" + folders[i];
			final FileMetaData metadata = fileIndexStore.getMetaData(projectId, currentPath);
			if (metadata == null || !metadata.isDir() || metadata.isDeleted())
				fileIndexStore.upsertFile(projectId, FileMetaData.folder(currentPath, false));
		}

	}

	@Override
	public F.Promise<JsonNode> putFile(String projectId, String path, byte[] fileBytes, boolean isZip) throws IOException {
		path = addLeadingSlash(path);
		upsertFoldersInPath(projectId, path);

		Integer bytes = 0;
		final String fileHash = isZip ? putFileInStoreWithZippedFileBytes(fileBytes, bytes) : putFileInStoreWithFileBytes(fileBytes, bytes);

		// update file in index
		final FileMetaData metadata = FileMetaData.file(path, fileHash, bytes, false);
		fileIndexStore.upsertFile(projectId, metadata);

		callListenersForChange(projectId);

		return Promise.pure(new ObjectMapper().valueToTree(metadata));
	}

	/**
	 * 
	 * @param zippedFileBytes
	 * @return fileHash
	 * @throws IOException
	 */
	private String putFileInStoreWithZippedFileBytes(byte[] zippedFileBytes, Integer outBytes) throws IOException {
		OutputStream out = null;
		ZipInputStream zipStream = null;
		DigestInputStream digestIn = null;
		String fileHash = null;

		try {
			final String zippedTmpPath = path().tmp();
			final String tmpPath = path().tmp();

			// copy zipfile to tmp dir
			out = fileStore.create(zippedTmpPath);
			IOUtils.write(zippedFileBytes, out);
			IOUtils.closeQuietly(out);

			// get unzipped file
			zipStream = new ZipInputStream(new ByteArrayInputStream(zippedFileBytes));
			zipStream.getNextEntry();
			digestIn = new DigestInputStream(zipStream, createMessageDigest());
			out = fileStore.create(tmpPath);
			// write to temp location
			outBytes = IOUtils.copy(digestIn, out);
			IOUtils.closeQuietly(out);
			// get hash
			fileHash = getFileCheckSum(digestIn);
			final String unzippedPath = path().hash(fileHash).raw();
			final String zippedPath = path().hash(fileHash).zipped();

			fileStore.move(tmpPath, unzippedPath);
			fileStore.move(zippedTmpPath, zippedPath);

		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(zipStream);
			IOUtils.closeQuietly(digestIn);
		}
		return fileHash;
	}

	private String putFileInStoreWithFileBytes(byte[] fileBytes, Integer outBytes) throws IOException {
		OutputStream out = null;
		ZipOutputStream zipStream = null;
		DigestInputStream digestIn = null;
		String fileHash = null;

		try {
			final String tmpPath = path().tmp();

			// copy file to tmp dir
			digestIn = new DigestInputStream(new ByteArrayInputStream(fileBytes), createMessageDigest());
			out = fileStore.create(tmpPath);
			outBytes = IOUtils.copy(digestIn, out);
			IOUtils.closeQuietly(out);

			// get hash
			fileHash = getFileCheckSum(digestIn);

			final String unzippedPath = path().hash(fileHash).raw();
			final String zippedPath = path().hash(fileHash).zipped();

			// write zipped file
			out = fileStore.create(zippedPath);
			zipStream = new ZipOutputStream(out);
			zipStream.putNextEntry(new ZipEntry("file"));
			IOUtils.write(fileBytes, zipStream);
			zipStream.closeEntry();
			IOUtils.closeQuietly(zipStream);
			IOUtils.closeQuietly(out);

			fileStore.move(tmpPath, unzippedPath);
		} finally {
			IOUtils.closeQuietly(zipStream);
			IOUtils.closeQuietly(digestIn);
			IOUtils.closeQuietly(out);
		}
		return fileHash;
	}

	@Override
	public F.Promise<JsonNode> listenIfUpdateOccurs(Map<String, Long> projectRevisionMap) throws IOException {
		boolean hasUpdates = false;

		final UpdateCallable callable = new UpdateCallable(fileIndexStore, projectRevisionMap);
		final Promise<JsonNode> promise = Akka.future(callable);// Akka.timeout(callable,2L,TimeUnit.SECONDS);

		// put in listener maps and check of update already happened
		for (Map.Entry<String, Long> entry : projectRevisionMap.entrySet()) {
			final String projectId = entry.getKey();
			final Long revision = entry.getValue();

			// check if projectId has entry in listener map
			if (!projectListenersMap.containsKey(projectId)) {
				projectListenersMap.put(projectId, Collections.synchronizedList(new ArrayList<UpdateCallable>()));
			}
			projectListenersMap.get(projectId).add(callable);

			final Project project = fileIndexStore.findProjectById(projectId);
			if (project.getRevision() != revision)
				hasUpdates = true;
		}

		if (hasUpdates)
			callable.send();

		return promise;
	};

	private void callListenersForChange(String projectId) {
		if (projectListenersMap.containsKey(projectId)) {
			final List<UpdateCallable> callableList = projectListenersMap.get(projectId);
			while (callableList.size() > 0) {
				final UpdateCallable callable = callableList.get(0);
				if (!callable.hasBeenCalled())
					callable.send();

				callableList.remove(callable);
			}
		}
	}

	@Override
	public F.Promise<JsonNode> versionDelta(String projectId, Long cursor) throws IOException {
		final Project project = fileIndexStore.findProjectById(projectId);
		Changes changes = null;
		if (project.getRevision() > cursor) {
			changes = fileIndexStore.getProjectChangesSinceRevision(projectId, cursor);
		} else {
			changes = new Changes(new ArrayList<String>());
		}

		return Promise.pure(new ObjectMapper().valueToTree(changes.getChangedPaths()));
	}

	@Override
	public boolean userBelongsToProject(String username, String projectId) {
		return fileIndexStore.userBelongsToProject(username, projectId);
	}

	@Override
	public Promise<JsonNode> delete(String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		final FileMetaData oldMetadata = fileIndexStore.getMetaData(projectId, path);

		// check if already deleted
		if (oldMetadata.isDeleted())
			return Promise.pure(new ObjectMapper().valueToTree(oldMetadata));

		FileMetaData metadata = null;
		if (oldMetadata.isDir())
			metadata = FileMetaData.folder(path, true);
		else
			metadata = FileMetaData.file(path, "", 0L, true);

		fileIndexStore.upsertFile(projectId, metadata);

		callListenersForChange(projectId);

		return Promise.pure(new ObjectMapper().valueToTree(metadata));
	}

	@Override
	public String toString() {
		return "HashBasedProjectService{" + "fileStore=" + fileStore + '}';
	}

	/**
	 * taken from http://www.mkyong.com/java/java-sha-hashing-example/
	 * 
	 * @return
	 */
	private static String getFileCheckSum(DigestInputStream inputStream) {
		final MessageDigest md = inputStream.getMessageDigest();
		final byte[] mdbytes = md.digest();
		Logger.debug("length: " + mdbytes.length + ";\n" + mdbytes.toString());

		// convert the byte to hex format
		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		IOUtils.closeQuietly(inputStream);
		return sb.toString();
	}

	private static MessageDigest createMessageDigest() {
		try {
			return MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Invalid Crypto algorithm! ", e);
		}
	}

	private <A> List<A> convertEntityCursorToList(EntityCursor<A> cursor) throws IOException {
		final List<A> list = new ArrayList<A>();
		final Iterator<A> it = cursor.iterator();

		while (it.hasNext()) {
			list.add(it.next());
		}
		cursor.close();
		return list;
	}

	private String addLeadingSlash(String path) {
		if (!path.startsWith("/"))
			return "/" + path;

		return path;
	}
}

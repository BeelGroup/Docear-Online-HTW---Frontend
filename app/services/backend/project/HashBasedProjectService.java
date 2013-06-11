package services.backend.project;

import models.backend.exceptions.sendResult.NotFoundException;
import models.backend.exceptions.sendResult.SendResultException;
import models.project.persistance.*;
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

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static services.backend.project.filestore.PathFactory.path;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService implements ProjectService {
	/**
	 * TODO UpdateCallables could cause performance issue. Implementing an Actor
	 * for the action might be better.
	 */
	private final Map<String, List<UpdateCallable>> projectListenersMap = new HashMap<String, List<UpdateCallable>>();
	private final Map<String, List<UpdateCallable>> userListenerMap = new HashMap<String, List<UpdateCallable>>();

	@Autowired
	private FileStore fileStore;

	@Autowired
	private FileIndexStore fileIndexStore;

	@Override
	public Promise<JsonNode> createProject(String username, String name) throws IOException {
		final Project project = fileIndexStore.createProject(name, username);
		callListenerForChangeForUser(username);
		return Promise.pure(new ObjectMapper().valueToTree(project));
	}

	@Override
	public Promise<Boolean> addUserToProject(String projectId, String usernameToAdd) throws IOException {
		fileIndexStore.addUserToProject(projectId, usernameToAdd);
		callListenerForChangeForUser(usernameToAdd);
		return Promise.pure(true);
	}

	@Override
	public Promise<Boolean> removeUserFromProject(String projectId, String usernameToRemove) throws IOException {
		fileIndexStore.removeUserFromProject(projectId, usernameToRemove);
		callListenerForChangeForUser(usernameToRemove);
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
        Logger.debug("HashBasedProjectService.getFile => projectId: "+projectId+"; path: "+path);
		path = addLeadingSlash(path);

		try {
            Logger.debug("HashBasedProjectService.getFile => test");
			// look for file in fileIndexStore
			final FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
			if (metadata == null) {
				throw new NotFoundException("File not found!");
			}

			final String fileHash = metadata.getHash();
            Logger.debug("HashBasedProjectService.getFile => fileHash: "+fileHash);

			return Promise.pure((InputStream) fileStore.open(path().hash(fileHash).zipped()));

		} catch (FileNotFoundException e) {
			throw new NotFoundException("File not found!", e);
		}
	}

	@Override
	public F.Promise<JsonNode> metadata(String projectId, String path) throws IOException {
		path = addLeadingSlash(path);
		Logger.debug("HashBasedProjectService => projectId: " + projectId + "; path: " + path);

		final FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
		if (metadata == null) {
			throw new NotFoundException("File not found!");
		}

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode metadataJson = (ObjectNode) mapper.valueToTree(metadata);

		// get children for dir
		if (metadata.isDir()) {
			final List<FileMetaData> childrenData = new ArrayList<FileMetaData>();
			final EntityCursor<FileMetaData> childrenMetadatas = fileIndexStore.getMetaDataOfDirectChildren(projectId, path, 5000);
			for (FileMetaData childMetadata : childrenMetadatas) {
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

        final FileMetaData newMetaData = fileIndexStore.getMetaData(projectId,path);


		callListenersForChangeInProject(projectId);

		return Promise.pure(new ObjectMapper().valueToTree(newMetaData));
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
	public F.Promise<JsonNode> putFile(String projectId, String path, byte[] fileBytes, boolean isZip, Long parentRevision, boolean forceOverride) throws IOException {
		String actualPath = addLeadingSlash(path);
		upsertFoldersInPath(projectId, actualPath);

		// check if file is present, not deleted and not forced to be overriden
		final FileMetaData currentServerMetaData = fileIndexStore.getMetaData(projectId, actualPath);
		if (currentServerMetaData != null && !currentServerMetaData.isDeleted() && !forceOverride) {
			final Long currentServerRevision = currentServerMetaData.getRevision();
			// when file is present it is important that parentRev has been
			if (parentRevision == null)
				throw new SendResultException("parentRevision is mandatory, because file is present", 400);
			if (currentServerRevision > parentRevision) {
				// Conflict! change path to a conflicted version path
				Logger.debug("Conflict");
				int indexOfLastDot = actualPath.lastIndexOf('.');
				final int indexOfLastSignBeforeExtension = indexOfLastDot != -1 ? indexOfLastDot : actualPath.length();

				String conflictedPath;
				boolean resourcePresent;
				int currentConflictedNumber = 0;
				do {
					currentConflictedNumber++;
					conflictedPath = new StringBuffer(actualPath).insert(indexOfLastSignBeforeExtension, "(Conflicted Version " + currentConflictedNumber + ")").toString();
					final FileMetaData meta = fileIndexStore.getMetaData(projectId, conflictedPath);
					resourcePresent = meta == null ? false : meta.isDeleted() == true ? false : true;
				} while (resourcePresent);

				actualPath = conflictedPath;
			} else if (parentRevision > currentServerRevision) {
				throw new SendResultException("Given Revision is too big!", 400);
			}
		}

		Integer bytes = 0;
		final String fileHash = isZip ? putFileInStoreWithZippedFileBytes(fileBytes, bytes) : putFileInStoreWithFileBytes(fileBytes, bytes);

		// update file in index
		Logger.debug("actualPath: " + actualPath);
		final FileMetaData metadata = FileMetaData.file(actualPath, fileHash, bytes, false);
		Logger.debug(metadata.toString());
		fileIndexStore.upsertFile(projectId, metadata);
        final FileMetaData newMetaData = fileIndexStore.getMetaData(projectId,actualPath);
		callListenersForChangeInProject(projectId);

		return Promise.pure(new ObjectMapper().valueToTree(newMetaData));
	}

	@Override
	public Promise<JsonNode> moveFile(String projectId, String oldPath, String newPath) throws IOException {
		oldPath = addLeadingSlash(oldPath);
		newPath = addLeadingSlash(newPath);

		// check that old resource is present
		final FileMetaData oldMeta = fileIndexStore.getMetaData(projectId, oldPath);
		if (oldMeta == null || oldMeta.isDeleted()) {
			throw new NotFoundException("file not found");
		}

		upsertFoldersInPath(projectId, newPath);

		if (oldMeta.isDir()) {
			// directory handling
			// check if resource is already a folder
			final FileMetaData currentNewMeta = fileIndexStore.getMetaData(projectId, newPath);
			final FileMetaData newMeta = FileMetaData.folder(newPath, false);
			if (currentNewMeta == null || currentNewMeta.isDeleted() || !currentNewMeta.isDir()) {
				// when not present or no folder, make it a folder
				fileIndexStore.upsertFile(projectId, newMeta);
			}

			moveFileRecursion(projectId, oldMeta, newMeta);
			fileIndexStore.upsertFile(projectId, FileMetaData.folder(oldPath, true));
		} else {
			// file handling
			final FileMetaData newMeta = FileMetaData.file(newPath, oldMeta.getHash(), oldMeta.getBytes(), false);
			fileIndexStore.upsertFile(projectId, newMeta);
			fileIndexStore.upsertFile(projectId, FileMetaData.file(oldPath, "", 0, true));
		}

		return Promise.pure(new ObjectMapper().readTree("[\"success\"]"));
	}

	private void moveFileRecursion(String projectId, FileMetaData folderMetadata, FileMetaData newFolderMetadata) throws IOException {
		final EntityCursor<FileMetaData> oldChildrenMetadatas = fileIndexStore.getMetaDataOfDirectChildren(projectId, folderMetadata.getPath(), Integer.MAX_VALUE);
		Logger.debug("HashBasedProjectService.moveFileRecursion => folderMeta: " + folderMetadata + "; newFolderMeta: " + newFolderMetadata);

		final String oldBasePath = folderMetadata.getPath();
		final String newBasePath = newFolderMetadata.getPath();
		for (final FileMetaData oldMetadata : oldChildrenMetadatas) {
			// ignore deleted files
			if (oldMetadata.isDeleted())
				continue;

			final String newPath = oldMetadata.getPath().replace(oldBasePath, newBasePath);
			// create and upsert on new position
			final FileMetaData newMetadata = oldMetadata.isDir() ? FileMetaData.folder(newPath, false) : FileMetaData.file(newPath, oldMetadata.getHash(), oldMetadata.getBytes(), false);
			fileIndexStore.upsertFile(projectId, newMetadata);

			// trigger recursion for folders
			if (newMetadata.isDir()) {
				// delete on old position
				fileIndexStore.upsertFile(projectId, FileMetaData.folder(oldMetadata.getPath(), true));
				moveFileRecursion(projectId, oldMetadata, newMetadata);
			} else {
				// delete on old position
				fileIndexStore.upsertFile(projectId, FileMetaData.file(oldMetadata.getPath(), "", 0, true));
			}
		}
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
	public F.Promise<JsonNode> listenIfUpdateOccurs(String username, Map<String, Long> projectRevisionMap) throws IOException {
		final UpdateCallable callable = new UpdateCallable(fileIndexStore, projectRevisionMap, username);
		final Promise<JsonNode> promise = Akka.future(callable);

		// put in listener maps and check if update already happened
		for (Map.Entry<String, Long> entry : projectRevisionMap.entrySet()) {
			final String projectId = entry.getKey();

			// check if projectId has entry in listener map
			if (!projectListenersMap.containsKey(projectId)) {
				projectListenersMap.put(projectId, Collections.synchronizedList(new ArrayList<UpdateCallable>()));
			}
			projectListenersMap.get(projectId).add(callable);
		}

		// put in User listener map
		if (!userListenerMap.containsKey(username))
			userListenerMap.put(username, new ArrayList<UpdateCallable>());
		userListenerMap.get(username).add(callable);

		return promise;
	};

	private void callListenersForChangeInProject(String projectId) {
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

	private void callListenerForChangeForUser(String username) {
		if (userListenerMap.containsKey(username)) {
			final List<UpdateCallable> callableList = userListenerMap.get(username);
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
		final Long currentRevision = project.getRevision();
		final Map<String, FileMetaData> resources = new HashMap<String, FileMetaData>();
		Changes changes = null;
		if (project.getRevision() > cursor) {
			changes = fileIndexStore.getProjectChangesSinceRevision(projectId, cursor);
		} else {
			changes = new Changes(new ArrayList<String>());
		}

		for (String resource : changes.getChangedPaths()) {
			final FileMetaData metadata = fileIndexStore.getMetaData(projectId, resource);
			resources.put(resource, metadata);
		}

		final VersionDeltaResponse response = new VersionDeltaResponse(currentRevision, resources);
		return Promise.pure(new ObjectMapper().valueToTree(response));
	}

	public static class VersionDeltaResponse {
		private final Long currentRevision;
		private final Map<String, FileMetaData> resources;

		public VersionDeltaResponse(Long currentRevision, Map<String, FileMetaData> resources) {

			this.currentRevision = currentRevision;
			this.resources = resources;
		}

		public Long getCurrentRevision() {
			return currentRevision;
		}

		public Map<String, FileMetaData> getResources() {
			return resources;
		}

	}

	@Override
	public boolean userBelongsToProject(String username, String projectId) {
		return fileIndexStore.userBelongsToProject(username, projectId);
	}

	@Override
	public Promise<JsonNode> delete(String projectId, String path) throws IOException {
		path = addLeadingSlash(path);
        Logger.debug("HashBasedProjectService => projectId: "+ projectId + "; path: "+path);

		final FileMetaData oldMetadata = fileIndexStore.getMetaData(projectId, path);

        if(oldMetadata == null) {
            throw new NotFoundException("File is not present");
        }

		// check if already deleted
		if (oldMetadata.isDeleted())
			return Promise.pure(new ObjectMapper().valueToTree(oldMetadata));

		FileMetaData metadata = null;
		if (oldMetadata.isDir())
			metadata = FileMetaData.folder(path, true);
		else
			metadata = FileMetaData.file(path, "", 0L, true);

		fileIndexStore.upsertFile(projectId, metadata);

		callListenersForChangeInProject(projectId);

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

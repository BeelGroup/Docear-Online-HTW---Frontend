package services.backend.project;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import models.backend.exceptions.sendResult.NotFoundException;
import models.backend.exceptions.sendResult.UnauthorizedException;
import models.project.persistance.Changes;
import models.project.persistance.EntityCursor;
import models.project.persistance.FileIndexStore;
import models.project.persistance.FileMetaData;
import models.project.persistance.Project;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import services.backend.project.filestore.FileStore;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService implements ProjectService {
	private final String FOLDER_UNZIPPED = "filesUnzipped";
	private final String FOLDER_ZIPPED = "filesZipped";

	@Autowired
	private FileStore fileStore;

	@Autowired
	private FileIndexStore fileIndexStore;

	@Override
	public Promise<JsonNode> createProject(String username, String name) throws IOException {
		final Project project = fileIndexStore.createProject(name, username);
		fileIndexStore.upsertFile(project.getId(), FileMetaData.folder("/", false));
		return Promise.pure(new ObjectMapper().valueToTree(project));
	}

	@Override
	public Promise<Boolean> addUserToProject(String username, String projectId, String usernameToAdd) throws IOException {
		fileIndexStore.addUserToProject(projectId, usernameToAdd);
		return Promise.pure(true);
	}

	@Override
	public Promise<Boolean> removeUserFromProject(String username, String projectId, String usernameToRemove) throws IOException {
		fileIndexStore.removeUserFromProject(projectId, usernameToRemove);
		return Promise.pure(true);
	}

	@Override
	public Promise<JsonNode> getProjectById(String username, String projectId) throws IOException {
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
	public F.Promise<InputStream> getFile(String username, String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		try {
			// look for file in fileIndexStore
			final FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
			if (metadata == null) {
				throw new NotFoundException("File not found!");
			}

			final String fileHash = metadata.getHash();

			return Promise.pure((InputStream) fileStore.open(FOLDER_ZIPPED + "/" + fileHash + ".zip"));

		} catch (FileNotFoundException e) {
			throw new NotFoundException("File not found!", e);
		}
	}

	@Override
	public F.Promise<JsonNode> metadata(String username, String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		final FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
		if (metadata == null) {
			throw new NotFoundException("File not found!");
		}
		final JsonNode metadataJson = new ObjectMapper().valueToTree(metadata);
		return Promise.pure(metadataJson);
	}

	@Override
	public F.Promise<JsonNode> createFolder(String username, String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		final FileMetaData metadata = FileMetaData.folder(path, false);
		fileIndexStore.upsertFile(projectId, metadata);

		return Promise.pure(new ObjectMapper().valueToTree(metadata));
	}

	@Override
	public F.Promise<JsonNode> putFile(String username, String projectId, String path, byte[] fileBytes, boolean isZip) throws IOException {
		path = addLeadingSlash(path);

		Integer bytes = 0;
		final String fileHash = isZip ? putFileInStoreWithZippedFileBytes(fileBytes, bytes) : putFileInStoreWithFileBytes(fileBytes, bytes);

		// update file in index
		final FileMetaData metadata = FileMetaData.file(path, fileHash, bytes, false);
		fileIndexStore.upsertFile(projectId, metadata);

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
			final String zippedTmpPath = "tmp/" + (new Random().nextInt(899999999) + 100000000) + ".zip";
			final String tmpPath = "tmp/" + (new Random().nextInt(899999999) + 100000000);

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
			final String unzippedPath = FOLDER_UNZIPPED + "/" + fileHash;
			final String zippedPath = FOLDER_ZIPPED + "/" + fileHash + ".zip";

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
			final String tmpPath = "tmp/" + (new Random().nextInt(899999999) + 100000000);

			// copy file to tmp dir
			digestIn = new DigestInputStream(new ByteArrayInputStream(fileBytes), createMessageDigest());
			out = fileStore.create(tmpPath);
			outBytes = IOUtils.copy(digestIn, out);
			IOUtils.closeQuietly(out);

			// get hash
			fileHash = getFileCheckSum(digestIn);

			final String unzippedPath = FOLDER_UNZIPPED + "/" + fileHash;
			final String zippedPath = FOLDER_ZIPPED + "/" + fileHash + ".zip";

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
	public F.Promise<Boolean> listenIfUpdateOccurs(String username, String projectId) throws IOException {
		throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
	}

	@Override
	public F.Promise<JsonNode> versionDelta(String username, String projectId, String cursor) throws IOException {
		final Changes changes = fileIndexStore.getProjectChangesSinceRevision(projectId, Integer.parseInt(cursor));
		return Promise.pure(new ObjectMapper().valueToTree(changes.getChangedPaths()));
	}

    @Override
    public boolean userBelongsToProject(String username, String projectId) {
        return fileIndexStore.userBelongsToProject(username, projectId);
    }

    @Override
	public Promise<JsonNode> delete(String username, String projectId, String path) throws IOException {
		path = addLeadingSlash(path);

		final FileMetaData metadata = FileMetaData.folder(path, true);
		fileIndexStore.upsertFile(projectId, metadata);
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

	private <A> List<A> convertEntityCursorToList(EntityCursor<A> cursor) {
		final List<A> list = new ArrayList<A>();
		final Iterator<A> it = cursor.iterator();

		while (it.hasNext()) {
			list.add(it.next());
		}

		return list;
	}

	private String addLeadingSlash(String path) {
		if (!path.startsWith("/"))
			return "/" + path;

		return path;
	}
}

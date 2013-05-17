package services.backend.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import models.backend.exceptions.sendResult.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.libs.F;
import play.libs.F.Promise;
import services.backend.project.filestore.FileStore;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService extends ProjectService {

	@Autowired
	private FileStore fileStore;

	@Override
	public F.Promise<InputStream> getFile(String username, String projectId, String path) throws IOException {
		/**
		 * for now, just save the file hash for a path in hadoop. If hash is
		 * present load file from hadoop
		 */
		InputStream in = null;
		try {
			in = fileStore.open(generateProjectResourcePath(projectId, path));
			final String fileHash = IOUtils.toString(in);
			IOUtils.closeQuietly(in);

			return Promise.pure((InputStream) fileStore.open(fileHash));

		} catch (FileNotFoundException e) {
			throw new NotFoundException("File not found!", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	@Override
	public F.Promise<JsonNode> metadata(String username, String projectId, String path) throws IOException {
		throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
	}

	@Override
	public F.Promise<JsonNode> createFolder(String username, String projectId, String path) throws IOException {
		throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
	}

	@Override
	public F.Promise<JsonNode> putFile(String username, String projectId, String path, byte[] content) throws IOException {
		/**
		 * For now generates hash, saves it for the path and saves the file
		 */
		OutputStream out = null;
		try {
			//generate hash
			final String fileHash = getFileCheckSum(content);
			final String fullPath = generateProjectResourcePath(projectId, path);
			out = fileStore.create(fullPath);
			IOUtils.write(fileHash, out);
			IOUtils.closeQuietly(out);
			
			out = fileStore.create(fileHash);
			IOUtils.write(content, out);
			
			return Promise.pure(new ObjectMapper().readTree("{\"hash\":\""+fileHash+"\"}"));
		} catch (FileNotFoundException e) {
			throw new NotFoundException("File not found!", e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	public F.Promise<Boolean> listenIfUpdateOccurs(String username, String projectId) throws IOException {
		throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
	}

	@Override
	public F.Promise<String> versionDelta(String username, String projectId, String cursor) throws IOException {
		throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
	}

	@Override
	public Promise<JsonNode> delete(String username, String projectId, String path) throws IOException {
		throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
	}

	@Override
	public String toString() {
		return "HashBasedProjectService{" + "fileStore=" + fileStore + '}';
	}

	private String generateProjectResourcePath(String projectId, String path) {
		return projectId + "/" + path;
	}
}

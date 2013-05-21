package services.backend.project;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.zip.ZipInputStream;

import models.backend.exceptions.sendResult.NotFoundException;

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

			return Promise.pure((InputStream) fileStore.open(FOLDER_ZIPPED+"/"+fileHash+".zip"));

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
	public F.Promise<JsonNode> putFile(String username, String projectId, String path, byte[] zipFileBytes) throws IOException {
		/**
		 * For now generates hash, saves it for the path and saves the file
		 */
		OutputStream out = null;
		ZipInputStream zipStream = null;
		DigestInputStream digestIn = null;
		String fileHash = null;
		try {			
			final String zippedTmpPath = "tmp/"+(new Random().nextInt(899999999)+100000000)+".zip";
			final String tmpPath = "tmp/"+(new Random().nextInt(899999999)+100000000);
			//copy zipfile to tmp dir
			out = fileStore.create(zippedTmpPath);
			IOUtils.write(zipFileBytes, out);
			IOUtils.closeQuietly(out);
			
			//get unzipped file
			zipStream = new ZipInputStream(new ByteArrayInputStream(zipFileBytes));
			zipStream.getNextEntry();
			digestIn = new DigestInputStream(zipStream, createMessageDigest());
			out = fileStore.create(tmpPath);
			// write to temp location
			IOUtils.copy(digestIn, out);
			IOUtils.closeQuietly(out);
			//get hash
			fileHash = getFileCheckSum(digestIn);
			final String unzippedPath = FOLDER_UNZIPPED+"/"+fileHash;
			final String zippedPath = FOLDER_ZIPPED+"/"+fileHash+".zip";
			
			fileStore.move(tmpPath, unzippedPath);
			fileStore.move(zippedTmpPath, zippedPath);
			
			
			final String fullPath = generateProjectResourcePath(projectId, path);
			out = fileStore.create(fullPath);
			IOUtils.write(fileHash, out);

			
		} catch (FileNotFoundException e) {
			throw new NotFoundException("File not found!", e);
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(zipStream);
		}
		
		return Promise.pure(new ObjectMapper().readTree("{\"hash\":\"" + fileHash + "\"}"));
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

	/**
	 * taken from http://www.mkyong.com/java/java-sha-hashing-example/
	 * 
	 * @param content
	 * @return
	 */
	private static String getFileCheckSum(DigestInputStream inputStream) {
		final MessageDigest md = inputStream.getMessageDigest();
		final byte[] mdbytes = md.digest();
		Logger.debug("length: "+mdbytes.length+";\n"+mdbytes.toString());

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
}

package services.backend.project;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;

/**
 * The defined projectservice-API is inspired by the Dropbox-API
 * (https://www.dropbox.com/developers/core/docs).<br>
 * 
 */
public abstract class ProjectService {
	public abstract Promise<InputStream> getFile(String username, String projectId, String path) throws IOException;

	public abstract Promise<JsonNode> metadata(String username, String projectId, String path) throws IOException;

	public abstract Promise<JsonNode> createFolder(String username, String projectId, String path) throws IOException;

	public abstract Promise<JsonNode> putFile(String username, String projectId, String path, byte[] content) throws IOException;

	public abstract Promise<JsonNode> delete(String username, String projectId, String path) throws IOException;

	public abstract Promise<Boolean> listenIfUpdateOccurs(String username, String projectId) throws IOException;

	public abstract Promise<String> versionDelta(String username, String projectId, String cursor) throws IOException;

	/**
	 * taken from http://www.mkyong.com/java/java-sha-hashing-example/
	 * 
	 * @param content
	 * @return
	 */
	protected static String getFileCheckSum(byte[] content) {
		try {
			final MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(content);

			final byte[] mdbytes = md.digest();

			// convert the byte to hex format
			final StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}

			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Invalid Crypto algorithm! ", e);
		}
	}
}

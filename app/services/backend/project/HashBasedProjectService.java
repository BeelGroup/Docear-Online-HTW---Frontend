package services.backend.project;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import play.libs.F;
import play.libs.F.Promise;
import services.backend.project.filestore.FileStore;

import java.io.IOException;
import java.io.InputStream;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService extends ProjectService {

    @Autowired
    private FileStore fileStore;

    @Override
    public F.Promise<InputStream> getFile(String username, String projectId, String path) throws IOException {
		InputStream in = null;
		try {
			in = fileStore.open(path);
		} finally {
			IOUtils.closeQuietly(in);
		}
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
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
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
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
        return "HashBasedProjectService{" +
                "fileStore=" + fileStore +
                '}';
    }
}

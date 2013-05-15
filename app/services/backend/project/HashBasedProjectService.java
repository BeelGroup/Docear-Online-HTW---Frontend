package services.backend.project;

import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import play.libs.F;
import services.backend.project.filestore.FileStore;

import java.io.InputStream;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService implements ProjectService {

    @Autowired
    private FileStore fileStore;

    @Override
    public F.Promise<InputStream> getFile(String projectId, String path) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<JsonNode> metadata(String projectId, String path) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<JsonNode> createFolder(String projectId, String path) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<JsonNode> putFile(String projectId, String path, byte[] content) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<Boolean> listenIfUpdateOccurs(String projectId) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<String> versionDelta(String projectId, String cursor) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public String toString() {
        return "HashBasedProjectService{" +
                "fileStore=" + fileStore +
                '}';
    }
}

package controllers;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import play.mvc.Controller;
import play.mvc.Results;
import services.backend.project.persistance.FileMetaData;

public class DocearController extends Controller {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static Results.Status ok(FileMetaData fileMetaData) {
        final JsonNode jsonNode = new ObjectMapper().valueToTree(fileMetaData);
        return ok(jsonNode);
    }
}

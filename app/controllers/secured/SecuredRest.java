package controllers.secured;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class SecuredRest extends SecuredBase {
    @Override
    public Result onUnauthorized(Http.Context ctx) {
        final ObjectNode jsonNode = org.codehaus.jackson.node.JsonNodeFactory.instance.objectNode();
        jsonNode.put("type", "error");
        jsonNode.put("message", "User authentication failed");

        return Results.status(401, jsonNode);
    }
}

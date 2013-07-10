package models.backend;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class MessageToFrontend {
	public enum Type {
		warning, error, info
	};

	private final Type type;
	private final String message;

	public MessageToFrontend(Type type, String message) {
		super();
		this.type = type;
		this.message = message;
	}

	public Type getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public JsonNode toJsonNode() {
		return new ObjectMapper().valueToTree(this);
	}
}

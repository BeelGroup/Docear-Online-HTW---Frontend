package models.backend.exceptions.sendResult;

@SuppressWarnings("serial")
public class SendResultException extends RuntimeException {
	
	private final int statusCode;
	
	public SendResultException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}
	
	public SendResultException(String message, int statusCode, Throwable t) {
		super(message,t);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}

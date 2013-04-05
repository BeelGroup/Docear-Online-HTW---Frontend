package models.backend.exceptions;


@SuppressWarnings("serial")
public class UnauthorizedException extends RuntimeException {
	public UnauthorizedException(String message, Throwable t) {
		super(message,t);
	}
	
	public UnauthorizedException(String message) {
		super(message);
	}
}

package models.backend.exceptions.sendResult;

import play.mvc.Controller;

@SuppressWarnings("serial")
public class UnauthorizedException extends SendResultException {
	public UnauthorizedException(String message, Throwable t) {
		super(message, Controller.UNAUTHORIZED, t);
	}

	public UnauthorizedException(String message) {
		super(message, Controller.UNAUTHORIZED);
	}
}

package models.backend.exceptions.sendResult;

import play.mvc.Controller;

@SuppressWarnings("serial")
public class NotFoundException extends SendResultException {
	public NotFoundException(String message) {
		super(message, Controller.NOT_FOUND);
	}

	public NotFoundException(String message, Throwable t) {
		super(message, Controller.NOT_FOUND, t);
	}
}

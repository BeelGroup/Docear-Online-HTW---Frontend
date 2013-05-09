package models.backend.exceptions.sendResult;

import play.mvc.Controller;

@SuppressWarnings("serial")
public class PreconditionFailedException extends SendResultException {
	public PreconditionFailedException(String message) {
		super(message, Controller.PRECONDITION_FAILED);
	}

	public PreconditionFailedException(String message, Throwable t) {
		super(message, Controller.PRECONDITION_FAILED, t);
	}
}

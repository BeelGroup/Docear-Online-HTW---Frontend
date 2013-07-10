package models.project.exceptions;

public class InvalidFileNameException extends Exception {
	private static final long serialVersionUID = 1L;
	
	
	public InvalidFileNameException(String message) {
		super(message);
	}
}

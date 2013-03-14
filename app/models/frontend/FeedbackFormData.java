package models.frontend;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;

public class FeedbackFormData {

	@Email(message = "No valid email entered.")
	private String feedbackEmail;
	private String feedbackSubject;
	@Required(message = "Text is required.")
	private String feedbackText;
	
	
	public String getFeedbackEmail() {
		return feedbackEmail;
	}
	public void setFeedbackEmail(String feedbackEmail) {
		this.feedbackEmail = feedbackEmail;
	}
	public String getFeedbackSubject() {
		return feedbackSubject;
	}
	public void setFeedbackSubject(String feedbackSubject) {
		this.feedbackSubject = feedbackSubject;
	}
	public String getFeedbackText() {
		return feedbackText;
	}
	public void setFeedbackText(String feedbackText) {
		this.feedbackText = feedbackText;
	}
	
	
}

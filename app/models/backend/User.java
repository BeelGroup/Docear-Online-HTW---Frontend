package models.backend;

import org.apache.commons.lang.Validate;

public class User {
	private final String accessToken;
	private final String username;
	private final String source;

	public User(final String username, final String accessToken) {
		Validate.notNull(username);
		Validate.notNull(accessToken);

		this.accessToken = accessToken;
		this.username = username;
		this.source = null;
	}

	public User(final String username, final String accessToken, final String source) {
		Validate.notNull(username);
		Validate.notNull(accessToken);

		this.accessToken = accessToken;
		this.username = username;
		this.source = source;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getUsername() {
		return username;
	}

	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		return "User{" + "accessToken='" + accessToken + '\'' + ", username='" + username + '\'' + '}';
	}
}

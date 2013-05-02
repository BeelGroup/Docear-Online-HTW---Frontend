package controllers;

import models.backend.User;
import play.cache.Cache;

public class Session {

	private static final String CACHE_KEY_VALID = "user-obj.valid.";

	public static Boolean isValid(User user) {
		final String key = generateIsValidKey(user);
		Boolean valid = (Boolean) Cache.get(key);
		return valid;
	}

	public static void setValid(User user, boolean isValid) {
		final String key = generateIsValidKey(user);
		Cache.set(key, isValid);
	}

	private static String generateIsValidKey(User user) {
		return CACHE_KEY_VALID + user.getUsername() + "." + user.getAccessToken();
	}

}

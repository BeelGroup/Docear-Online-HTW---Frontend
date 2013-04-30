package controllers;

import javax.annotation.Nullable;

import models.backend.User;

import org.apache.commons.lang.Validate;

import play.Logger;
import play.cache.Cache;
import services.backend.user.UserService;

public class Session {

	private static final String CACHE_KEY_NAME = "user-obj.name.";
	private static final String CACHE_KEY_VALID = "user-obj.valid.";

	public static void createSession(String username, String accessToken) {
        Validate.notEmpty(username);
        Validate.notEmpty(accessToken);
        User user = new User(username, accessToken);
        Cache.set(CACHE_KEY_NAME + username, user);
        User.upsert(user);
	}
	
	public static User getUser(@Nullable String username) {
        User user = (User) Cache.get(CACHE_KEY_NAME + username);
        if (user == null && username != null) {
            Logger.debug("user '" + username + "' not in Cache, obtaining from database");
            user = User.findByName(username);
        }
        return user;
    }
	
	public static boolean isValid(User user, UserService userService) {
		final String key = CACHE_KEY_VALID+user.getUsername()+"."+user.getAccessToken();
		Boolean valid = (Boolean) Cache.get(CACHE_KEY_VALID+user.getUsername()+"."+user.getAccessToken()); 
        if(valid == null) {
        	valid = userService.isValid(user);
        	Cache.set(key, valid);
        }
        return valid;
	}

}

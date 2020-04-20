package de.icytv.scribble.http;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.icytv.scribble.utils.JWT;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PermissionCheck {
	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static boolean hasPermission(String jwts, String perm) {
		JsonObject jwt = JWT.parseJwt(jwts);
		JsonArray perms = jwt.getJsonArray("perms");
		return perms.contains(perm);
	}

	public static void hasPermissionHard(String jwts, String perm) throws AccessViolationException {
		JsonObject jwt = JWT.parseJwt(jwts);
		JsonArray perms = jwt.getJsonArray("perms");
		if (!perms.contains(perm)) {
			throw new AccessViolationException("User has no permission to access this endpoint!");
		}
	}

	public static void hasPermissionHard(RoutingContext c, String perm) throws AccessViolationException {
		try {
			String jwts = c.request().getHeader("Authorization").replace("Bearer ", "");
			JsonObject jwt = JWT.parseJwt(jwts);
			JsonArray perms = jwt.getJsonArray("perms");
			if (!perms.contains(perm)) {
				throw new AccessViolationException("User has no permission to access this endpoint!");
			}
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new AccessViolationException("User has no permission to access this endpoint!");
		}

	}

	public static boolean isSelf(RoutingContext c) {
		String name = c.request().getParam("username");
		JsonObject jwt = JWT.parseJwt(c);
		return jwt.getString("sub").equals(name);
	}

	public static void isSelfHard(RoutingContext c) throws AccessViolationException {
		if (!isSelf(c)) {
			throw new AccessViolationException("User is not self!");
		}
	}

}
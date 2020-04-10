package utils;

import static utils.Constants.JWT_KEY_PAIR;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import http.UserHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.vertx.core.json.JsonObject;

/**
 * JWT
 */
public class JWT {
	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static boolean verifyJWT(String jwt) {
		try {
			JwtParserBuilder builder = Jwts.parserBuilder();
			builder.requireIssuer(UserHandler.ISSUER);
			builder.setSigningKey(JWT_KEY_PAIR.getPublic());
			builder.build().parse(jwt);
			log.info("Parse success!");
			return true;

		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return false;
		}

	}

	public static JsonObject parseJwt(String s) throws UnsupportedJwtException, ExpiredJwtException,
			MalformedJwtException, SignatureException, IllegalArgumentException {
		JwtParserBuilder builder = Jwts.parserBuilder();
		builder.requireIssuer(UserHandler.ISSUER);
		builder.setSigningKey(JWT_KEY_PAIR.getPublic());
		Claims c = builder.build().parseClaimsJws(s).getBody();
		log.trace(c);
		return JsonObject.mapFrom((Map<String, Object>) c);
	}

	public static JsonObject parseJwtSoft(String s) {
		try {
			return parseJwt(s);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return null;
		}
	}

	public static void main(String[] args) {
		// JWT.verifyJWT();
	}

}
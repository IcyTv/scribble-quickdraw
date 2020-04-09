package utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import http.HTTPServer;
import http.UserHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtHandler;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.io.EncodingException;
import io.jsonwebtoken.security.Keys;
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
			builder.setSigningKey(HTTPServer.KEY_PAIR.getPublic());
			builder.build().parse(jwt);
			log.info("Parse success!");
			return true;

		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return false;
		}

	}

	public static JsonObject parseJwt(String s) {
		try {
			JwtParserBuilder builder = Jwts.parserBuilder();
			builder.requireIssuer(UserHandler.ISSUER);
			builder.setSigningKey(HTTPServer.KEY_PAIR.getPublic());
			Claims c = builder.build().parseClaimsJws(s).getBody();
			log.trace(c);
			return JsonObject.mapFrom((Map<String, Object>) c);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return null;
		}
	}

	public static void main(String[] args) {
		// JWT.verifyJWT();
	}

}
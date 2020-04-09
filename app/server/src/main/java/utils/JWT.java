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
			log.warn(e);
			return false;
		}

	}

	private static PrivateKey getPriv(String filename) throws Exception {

		byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	private static PublicKey getPub(String filename) throws Exception {

		byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

	public static void main(String[] args) {
		// JWT.verifyJWT();
	}

}
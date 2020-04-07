package utils;

import java.security.Key;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JWT {
	public JWT() {
		Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

		String jws = Jwts.builder().setSubject("Test").signWith(key).compact();
		System.out.println(jws);
	}

	public static void main(String[] args) {
		new JWT();
	}
}
package de.icytv.scribble.http;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class User {

	public String name;
	public String pwHash;
	public String[] perms;

	public User(String name, String pwHash, String[] perms) {
		this.name = name;
		this.pwHash = pwHash;
		this.perms = perms;
	}

	public User() {}

	public static String encPw(String password) {
		final PasswordEncoder pw = new BCryptPasswordEncoder();
		return pw.encode(password);
	}

}
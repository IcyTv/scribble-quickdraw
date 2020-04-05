package http;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class HTTPServer {

	private final Connection conn;

	public HTTPServer(int port) throws Exception {
		conn = DriverManager.getConnection("jdbc:postgresql://192.168.178.97:5432/scribble", "unpriv",
				"gMvDapsv586HZ7K74a9i");
	}

	private void newUser(String name, String password, String ip) throws Exception {
		PasswordEncoder pw = new BCryptPasswordEncoder();
		String encpw = pw.encode(password);
		UUID uuid = UUID.nameUUIDFromBytes(ip.getBytes());

		Statement st = conn.createStatement();
		String sql = String.format("INSERT INTO public.users(id, name, password, ips, uuid)"
				+ "VALUES(DEFAULT, '%s', '%s', ARRAY ['%s'::INET], '%s');", name, encpw, ip, uuid.toString());
		st.executeUpdate(sql);
	}

	public static void main(String[] args) throws Exception {
		HTTPServer s = new HTTPServer(12345);
		s.newUser("Merlin", "passwort", "127.0.0.1");
	}

}
package socket;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SocketServer {

	public static void main(String[] args) {
		Connection c = null;
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://192.168.178.97:5432/scribble", "unpriv",
					"gMvDapsv586HZ7K74a9i");

			Statement stmt = c.createStatement();
			String sql = "SELECT * FROM users;";
			ResultSet res = stmt.executeQuery(sql);
			while (res.next()) {
				int id = res.getInt("id");
				String name = res.getString("name");
				String passwd = res.getString("password");
				String ip = res.getString("ips");
				String uuid = res.getString("uuid");

				System.out.printf("%d %s %s %s %s %s\n", id, name, passwd, ip, uuid);
			}
			stmt.close();
			c.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		System.out.println("Connected to db");
	}

}
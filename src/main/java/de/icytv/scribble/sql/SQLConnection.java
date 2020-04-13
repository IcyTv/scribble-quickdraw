package de.icytv.scribble.sql;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.icytv.scribble.utils.Toolbox;

public class SQLConnection {

	private static SQLConnection active;
	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Connection conn;

	private SQLConnection() {
		try {
			// conn = DriverManager.getConnection(HTTPServer.POSTGRES_URL,
			// HTTPServer.POSTGRES_USER,
			// HTTPServer.POSTGRES_PW);
			String port = (System.getenv("POSTGRES_PORT") == null) ? "5432" : System.getenv("POSTGRES_PORT");
			String db = (System.getenv("POSTGRES_DB") == null) ? "scribble" : System.getenv("POSTGRES_DB");
			String url = (System.getenv("POSTGRES_URL") == null) ? "jdbc:postgresql://localhost:" + port + "/" + db
					: "jdbc:postgresql://" + System.getenv("POSTGRES_URL") + ":" + port + "/" + db;
			String user = (System.getenv("POSTGRES_USER") == null) ? "postgres" : System.getenv("POSTGRES_USER");
			String pw = (System.getenv("POSTGRES_PW") == null) ? "postgres" : System.getenv("POSTGRES_PW");
			System.out.println(url + "   " + user + " " + pw);
			conn = DriverManager.getConnection(url, user, pw);
			conn.setAutoCommit(true);

			setUpTables();
		} catch (SQLException e) {
			// Catch statement because of use in static final context
			e.printStackTrace();
			// throw new IllegalStateException(e.getMessage());
		}
	}

	public Connection getConnection() {
		return conn;
	}

	public Statement createStatement() throws SQLException {
		return conn.createStatement();
	}

	public static SQLConnection init() {
		if (active == null) {
			active = new SQLConnection();
		}
		return active;
	}

	private void setUpTables() throws SQLException {
		Statement stmt = conn.createStatement();
		String sql = Toolbox.readFromFile("postgres-bak.sql");
		stmt.execute(sql);
	}

}
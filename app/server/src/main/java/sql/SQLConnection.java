package sql;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import http.HTTPServer;

public class SQLConnection {

	private static SQLConnection active;
	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Connection conn;

	private SQLConnection() {
		try {
			conn = DriverManager.getConnection(HTTPServer.POSTGRES_URL, HTTPServer.POSTGRES_USER,
					HTTPServer.POSTGRES_PW);
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			// Catch statement because of use in static final context
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
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

}
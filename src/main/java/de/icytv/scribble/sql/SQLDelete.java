package de.icytv.scribble.sql;

import java.sql.SQLException;
import java.sql.Statement;

public class SQLDelete {

	private static final SQLConnection conn = SQLConnection.init();

	public static void executeDelete(String sql) throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("DELETE FROM " + sql);
	}

	public static void delete(String table, String wh) throws SQLException {
		executeDelete(table + " WHERE " + wh);
	}

}
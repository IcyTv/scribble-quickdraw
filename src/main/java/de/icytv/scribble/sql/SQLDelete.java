package de.icytv.scribble.sql;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SQLDelete {
	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final SQLConnection conn = SQLConnection.init();

	public static void executeDelete(String sql) throws SQLException {
		Statement stmt = conn.createStatement();
		log.trace("DELETE FROM " + sql);
		stmt.executeUpdate("DELETE FROM " + sql);
	}

	public static void delete(String table, String wh) throws SQLException {
		executeDelete(table + " WHERE " + wh);
	}

}
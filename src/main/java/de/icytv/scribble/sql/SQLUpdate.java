package de.icytv.scribble.sql;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SQLUpdate {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final SQLConnection conn = SQLConnection.init();

	/**
	 * UPDATE array in column
	 * 
	 * @param table  Table to update
	 * @param column Column to update
	 * @param wh     WHERE statement (i.e. where name = "test")
	 * @param add    What to add to the array
	 * @throws SQLException Wrong sql syntax or args
	 */
	public static void updateArray(String table, String column, String wh, String... add) throws SQLException {
		String adds = String.join("m", add);
		executeUpdate(String.format(
				"UPDATE %s SET %s = (SELECT array_agg(distinct e) FROM unnest(%s || '{%s}') e) WHERE NOT %s @> '{%s}' AND %s;",
				table, column, column, adds, column, adds, wh));
	}

	/**
	 * @param sql
	 * @throws SQLException
	 */
	public static void executeUpdate(String sql) throws SQLException {
		Statement stmt = conn.createStatement();
		log.trace(sql);
		stmt.executeUpdate(sql);
	}

}
package de.icytv.scribble.sql;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SQLInsert {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final SQLConnection conn = SQLConnection.init();

	public static void insert(String table, ValuePair... tvals) throws SQLException {
		ArrayList<String> vals = new ArrayList<String>();
		ArrayList<String> vals2 = new ArrayList<String>();

		for (ValuePair s : tvals) {
			vals.add(s.val1);
			vals2.add(s.val2);
		}
		executeInsert(String.format("INSERT INTO %s (%s) VALUES (%s)", table, String.join(",", vals),
				String.join(",", vals2)));

	}

	public static void executeInsert(String sql) throws SQLException {
		Statement stmt = conn.createStatement();
		log.trace(sql);
		stmt.executeUpdate(sql);

	}

}
package de.icytv.scribble.sql;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SQLQuery {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static SQLConnection conn = SQLConnection.init();

	/**
	 * SELECT column FROM table
	 * 
	 * @param table  Table to execute query
	 * @param column Selected column(s)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet query(String table, String column) throws SQLException {
		return executeQuery(String.format("SELECT %s FROM %s;", table, column));
	}

	/**
	 * SELECT * FROM table
	 * 
	 * @param table Table to execute query
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet queryAll(String table) throws SQLException {
		return query(table, "*");
	}

	/**
	 * SELECT column FROM table WHERE wh
	 * 
	 * @param table  Table to execute query
	 * @param column Selected column(s)
	 * @param wh     WHERE statement in Query (i.e. WHERE id = 1)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet queryWhere(String table, String column, String wh) throws SQLException {
		return executeQuery(String.format("SELECT %s FROM %s WHERE %s;", column, table, wh));
	}

	/**
	 * SELECT * FROM table WHERE wh
	 * 
	 * @param table Table to execute query
	 * @param wh    WHERE statement in Query (i.e. WHERE id = 1)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet queryAllWhere(String table, String wh) throws SQLException {
		return queryWhere(table, "*", wh);
	}

	/**
	 * SELECT column FROM table1 JOIN table2 ON on1=on2 WHERE wh
	 * 
	 * @param table1 Table 1 to join
	 * @param table2 Table 2 to join
	 * @param on1    What to join on (Table 1)
	 * @param on2    What to join on (Table 2)
	 * @param column Selected column(s)
	 * @param wh     WHERE statement in Query (i.e. WHERE id = 1)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet queryJoinWhere(String table1, String table2, String on1, String on2, String column,
			String wh) throws SQLException {
		return executeQuery(
				String.format("SELECT %s FROM %s JOIN %s ON %s=%s WHERE=%s;", column, table1, table2, on1, on2, wh));
	}

	/**
	 * SELECT * FROM table1 JOIN table2 ON on1=on2 WHERE wh
	 * 
	 * @param table1 Table 1 to join
	 * @param table2 Table 2 to join
	 * @param on1    What to join on (Table 1)
	 * @param on2    What to join on (Table 2)
	 * @param wh     WHERE statement in Query (i.e. WHERE id = 1)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet queryJoinAllWhere(String table1, String table2, String on1, String on2, String wh)
			throws SQLException {
		return queryJoinWhere(table1, table2, on1, on2, "*", wh);
	}

	/**
	 * SELECT column FROM table1 JOIN table2 ON on1=on2
	 * 
	 * @param table1 Table 1 to join
	 * @param table2 Table 2 to join
	 * @param on1    What to join on (Table 1)
	 * @param on2    What to join on (Table 2)
	 * @param column Selected column(s)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet queryJoin(String table1, String table2, String on1, String on2, String column)
			throws SQLException {
		return executeQuery(String.format("SELECT %s FROM %s JOIN %s ON %s=%s;", column, table1, table2, on1, on2));
	}

	/**
	 * SELECT * FROM table1 JOIN table2 ON on1=on2
	 * 
	 * @param table1 Table 1 to join
	 * @param table2 Table 2 to join
	 * @param on1    What to join on (Table 1)
	 * @param on2    What to join on (Table 2)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet queryJoinAll(String table1, String table2, String on1, String on2) throws SQLException {
		return queryJoin(table1, table2, on1, on2, "*");
	}

	/**
	 * SELECT column FROM table1 JOIN table2 ON on WHERE wh GROUP BY gb
	 * 
	 * @param table1 Table 1 to join
	 * @param table2 Table 2 to join
	 * @param on     What to join on (MUST be a boolean statement, i.e. 1=1)
	 * @param column Selected column(s)
	 * @param wh     WHERE statement in Query (i.e. WHERE id = 1)
	 * @param gb     GROUP BY statement (must be column of joined table)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 * 
	 * @see <a href= "https://www.postgresql.org/docs/9.4/tutorial-agg.html">See
	 *      here</a> for more info
	 */
	public static ResultSet queryJoinNbGroupWhere(String table1, String table2, String on, String column, String wh,
			String gb) throws SQLException {
		return executeQuery(String.format("SELECT %s FROM %s JOIN %s ON %s WHERE %s GROUP BY %s;", column, table1,
				table2, on, wh, gb));
	}

	/**
	 * {@link queryJoinNbGroupWhere}, but with JOIN type specification
	 * 
	 * @param table1 Table to execute query 1
	 * @param table2 Table to execute query 2
	 * @param on     What to join on (MUST be a boolean statement, i.e. 1=1)
	 * @param type   JOIN type (i.e. INNER, LEFT, OUTER, RIGHT, FULL)
	 * @param column Selected column(s)
	 * @param wh     WHERE statement in Query (i.e. WHERE id = 1)
	 * @param gb     GROUP BY statement (must be column of joined table)
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 * 
	 * @see <a href="https://www.techonthenet.com/postgresql/joins.php">See here</a>
	 *      for mor infos
	 */
	public static ResultSet queryJoinTypeNbGroupWhere(String table1, String table2, String on, String type,
			String column, String wh, String gb) throws SQLException {
		return executeQuery(String.format("SELECT %s FROM %s %s JOIN %s ON %s WHERE %s GROUP BY %s;", column, table1,
				type, table2, on, wh, gb));
	}

	/**
	 * Execute passed query (i.e. SELECT * FROM etc)
	 * 
	 * @param query Query to be executed
	 * @return {@link ResultSet} of the query
	 * @throws SQLException on SQL formatting Error
	 */
	public static ResultSet executeQuery(String query) throws SQLException {
		Statement stmt = conn.createStatement();
		log.trace(query);
		return stmt.executeQuery(query);
	}
}
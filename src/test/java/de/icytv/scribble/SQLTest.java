package de.icytv.scribble;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.icytv.scribble.sql.SQLQuery;

@DisplayName("Sql tests")
public class SQLTest {

	@Test
	@DisplayName("Query")
	public void testQuery() throws SQLException {
		ResultSet res = SQLQuery.query("users", "users");
		assertThat(res.next()).isTrue();
	}

	@Test
	@DisplayName("Query All")
	public void testQueryAll() throws SQLException {
		assertThat(SQLQuery.queryAll("users").next()).isTrue();
	}

}
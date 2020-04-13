package de.icytv.scribble;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import de.icytv.scribble.http.UserHandler;
import de.icytv.scribble.sql.SQLDelete;
import de.icytv.scribble.sql.SQLQuery;

@DisplayName("Sql tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLTest {

	@BeforeAll
	public void setUp() throws Exception {
		UserHandler.newUser("test", "password", "127.0.0.1");
	}

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

	@AfterAll
	public void tearDown() throws SQLException {
		SQLDelete.delete("users", "name='test'");
	}
}
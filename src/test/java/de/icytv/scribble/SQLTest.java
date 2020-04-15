package de.icytv.scribble;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import de.icytv.scribble.http.UserHandler;
import de.icytv.scribble.sql.SQLConnection;
import de.icytv.scribble.sql.SQLDelete;
import de.icytv.scribble.sql.SQLQuery;

@DisplayName("Sql tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLTest {

	@BeforeAll
	public void setUp() throws Exception {
		UserHandler.newUser("test", "password", "127.0.0.1");
		Class.forName("com.qwazr.jdbc.cache.Driver");
	}

	@Test
	@DisplayName("Connection test")
	public void testConnection() {
		assertThat(SQLConnection.init().getConnection()).isNotNull();
	}

	@Test
	@DisplayName("Query")
	public void testQuery() throws SQLException {
		ResultSet res = SQLQuery.query("users", "users");
		assertThat(res.next()).isTrue();
	}

	@Test
	@DisplayName("Test Driver")
	public void testDriver() throws SQLException {
		Driver driver = DriverManager.getDriver("jdbc:cache:mem:postgres-cache");
		assertThat(driver).isNotNull();
		assertThat(driver.getMajorVersion()).isEqualTo(1);
		assertThat(driver.getMinorVersion()).isEqualTo(3);
		assertThat(driver.getParentLogger()).isNotNull();
		assertThat(driver.jdbcCompliant()).isFalse();
		DriverPropertyInfo[] infos = driver.getPropertyInfo(null, null);
		
		assertThat(infos).isNotNull();
		assertThat(infos.length).isEqualTo(3);
		assertThat(infos[0].name).isEqualTo(com.qwazr.jdbc.cache.Driver.CACHE_DRIVER_URL);
		assertThat(infos[1].name).isEqualTo(com.qwazr.jdbc.cache.Driver.CACHE_DRIVER_CLASS);
		assertThat(infos[2].name).isEqualTo(com.qwazr.jdbc.cache.Driver.CACHE_DRIVER_ACTIVE);
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
package http;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class RoomHandler {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final Connection conn = getConnection();

	private static Connection getConnection() {
		try {
			return DriverManager.getConnection(HTTPServer.POSTGRES_URL, HTTPServer.POSTGRES_USER,
					HTTPServer.POSTGRES_PW);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void handleListUsers(RoutingContext c) {
		String sql = "SELECT users.name FROM user_rooms JOIN users ON users.id = user_rooms.user_id WHERE user_rooms.room_id=(SELECT room_id FROM users JOIN user_rooms ON users.id = user_rooms.user_id WHERE users.name = '%s');";
		HttpServerResponse res = c.response();
		String uname = StringEscapeUtils.escapeSql(c.request().getParam("username"));
		log.info("Querying room for " + uname);
		try {
			ResultSet query = queryDb(String.format(sql, uname));
			ArrayList<String> names = new ArrayList<String>();
			while (query.next()) {
				names.add(query.getString(1));
			}
			JsonObject json = new JsonObject();
			json.put("users", names);
			res.setStatusCode(200);
			res.putHeader("content-type", "application/json; charset=utf-8");
			res.end(json.encodePrettily());
		} catch (SQLException e) {
			log.warn(e.getMessage());
			res.setStatusCode(500).end();
		}
	}

	private static ResultSet queryDb(String sql) throws SQLException {
		Statement st = conn.createStatement();
		return st.executeQuery(sql);
	}
}
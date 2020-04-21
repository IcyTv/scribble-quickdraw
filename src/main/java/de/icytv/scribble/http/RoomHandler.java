package de.icytv.scribble.http;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.icytv.scribble.sql.SQLConnection;
import de.icytv.scribble.sql.SQLDelete;
import de.icytv.scribble.sql.SQLInsert;
import de.icytv.scribble.sql.ValuePair;
import de.icytv.scribble.utils.JWT;
import de.icytv.scribble.utils.Toolbox;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class RoomHandler {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final SQLConnection conn = SQLConnection.init();

	// TODO implement on socket in db, socket checking to db...

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

	public static void handleAddUser(RoutingContext c) {
		HttpServerResponse res = c.response();
		try {
			log.trace("Adding user " + c.request().getParam("username"));

			if (Toolbox.isMissingParam(c, "username") && !PermissionCheck.isSelf(c)) {
				PermissionCheck.hasPermissionHard(c, "room-edit");
			}

			String name = "";
			if (Toolbox.isMissingParam(c, "username")) {
				name = c.request().getParam("username");
			} else {
				name = JWT.parseJwt(c).getString("sub");
			}
			
			int id = Toolbox.getUID(name);
			String room = c.request().getParam("room");

			SQLInsert.insertIfNotExists("user_rooms", new ValuePair("user_id", "" + id), new ValuePair("room_id", room));
		
			res.setStatusCode(200).end("Success");
		} catch (AccessViolationException e) {
			res.setStatusCode(403).end(e.getMessage());
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			res.setStatusCode(500).end();
		}
	}

	public static void handleUserRemove(RoutingContext c) {
		HttpServerResponse res = c.response();
		try {
			if(!PermissionCheck.isSelf(c)) {
				PermissionCheck.hasPermissionHard(c, "room-edit");
			}
			String name = JWT.parseJwt(c).getString("sub");

			int id = Toolbox.getUID(name);

			SQLDelete.delete("user_rooms", "user_id=" + id);

			res.setStatusCode(200).end("Success");
		} catch(AccessViolationException e) {
			res.setStatusCode(403).end(e.getMessage());
		} catch(Exception e) {
			log.warn(e.getMessage(), e);
			res.setStatusCode(500).end();
		}
	}

	public static void handleRoomRemove(RoutingContext c) {
		HttpServerResponse res = c.response();
		try {
			if (!PermissionCheck.isSelf(c)) {
				PermissionCheck.hasPermissionHard(c, "room-edit");
			}

			res.setStatusCode(503).end("Under construction");

		} catch (AccessViolationException e) {
			res.setStatusCode(403).end(e.getMessage());
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			res.setStatusCode(500).end();
		}
	}

	private static ResultSet queryDb(String sql) throws SQLException {
		Statement st = conn.createStatement();
		return st.executeQuery(sql);
	}

}
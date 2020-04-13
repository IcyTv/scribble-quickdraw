package de.icytv.scribble.http;

import static de.icytv.scribble.sql.SQLTools.str;
import static de.icytv.scribble.utils.Constants.JWT_KEY_PAIR;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.icytv.scribble.sql.SQLInsert;
import de.icytv.scribble.sql.SQLQuery;
import de.icytv.scribble.sql.SQLUpdate;
import de.icytv.scribble.sql.ValuePair;
import de.icytv.scribble.utils.Toolbox;
//Maybe at some point change to io.vertx.jwt, but conflict with user etc...
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserHandler {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String ISSUER = "scribble-server";

	static public void handleUserLogin(RoutingContext c) {
		HttpServerResponse res = c.response();
		JsonObject body = c.getBodyAsJson();
		String name = StringEscapeUtils.escapeSql(body.getString("username"));
		String pw = body.getString("password");
		try {
			new Thread(() -> handleIp(name, Toolbox.getIp(c))).start();
			log.info("Running");
			User u = getUserInfo(name);
			if (checkPassword(pw, u.pwHash)) {
				res.setStatusCode(200);
				res.putHeader("content-type", "text/plain");
				res.end(getJWT(u));
			} else {
				res.setStatusCode(403).end("Wrong password");
			}
		} catch (IllegalArgumentException e) {
			res.setStatusCode(403).end(e.getMessage());
		} catch (Exception e) {
			log.fatal("Error while logging in");
			log.fatal(e.getMessage());
			res.setStatusCode(500).end("Internal Server Error on login");
		}
	}

	static public void handleUserAuth(RoutingContext c) {
		JsonObject body = c.getBodyAsJson();
		HttpServerResponse res = c.response();
		String scope = StringEscapeUtils.escapeSql(c.request().getParam("scope"));
		try {
			Jws<Claims> jwt = Jwts.parserBuilder().setSigningKey(JWT_KEY_PAIR.getPublic()).build()
					.parseClaimsJws(body.getString("jwt"));
			Claims claims = jwt.getBody();
			if (claims.getExpiration().compareTo(new Date()) < 0) {
				SimpleDateFormat form = new SimpleDateFormat();
				log.info("Current time: " + form.format(new Date()));
				log.warn("Expired jwt on " + form.format(claims.getExpiration()));
				c.fail(401);
				return;
			}
			if (!claims.getIssuer().equals(ISSUER)) {
				log.warn("Wrong issuer!");
				c.fail(401);
				return;
			}
			boolean found = false;
			for (Object perm : claims.get("perms", ArrayList.class)) {
				if (perm.equals(scope)) {
					found = true;
					break;
				}
			}
			if (!found) {
				log.warn("Wrong scope");
				c.fail(403);
				return;
			}
			res.setStatusCode(200).end("authenticated");

		} catch (SignatureException e) {
			c.fail(401);
		}
	}

	public static void handleUserLogout(RoutingContext c) {
		c.response().setStatusCode(307).end("/logout.html");
	}

	public static void handleUserRoom(RoutingContext c) {
		String uname = StringEscapeUtils.escapeSql(c.request().getParam("username"));
		HttpServerResponse res = c.response();
		try {
			// ResultSet query = queryDb(String.format(
			// "SELECT user_rooms.room_id FROM user_rooms JOIN users ON users.id =
			// user_rooms.user_id WHERE users.name='%s';",
			// uname));
			ResultSet query = SQLQuery.queryJoinWhere("user_rooms", "users", "users.id", "user_rooms.user_id",
					"users.name", "users.name=" + str(uname));
			ArrayList<Integer> rooms = new ArrayList<Integer>();
			while (query.next()) {
				rooms.add(query.getInt("room_id"));
			}
			res.putHeader("content-type", "application/json; charset=utf-8");
			JsonObject json = new JsonObject();
			json.put("rooms", rooms);
			res.end(json.encodePrettily());
		} catch (SQLException e) {
			log.warn(e.getMessage());
			res.setStatusCode(500).end();
		}

	}

	public static void handleUserRegister(RoutingContext c) {
		log.info("New user registration");
		HttpServerResponse res = c.response();
		JsonObject body = c.getBodyAsJson();
		log.info(body.toString());
		String name = StringEscapeUtils.escapeSql(body.getString("username"));
		if (!Pattern.matches("[A-Za-z0-9-]{3,12}", name)) {
			log.warn("Malformed username: " + name);
			res.setStatusCode(403).end("Malformed username, please only use A-z, 0-9 and '-'!");
			return;
		}
		String pw = body.getString("password");
		String ip = Toolbox.getIp(c);
		log.info(ip);
		try {
			if (isRegistered(name)) {
				res.setStatusCode(403).end("User already exists");
			} else {
				log.info("registered user " + name);
				newUser(name, pw, ip);
				res.setStatusCode(200).end("Registered user");
			}
		} catch (Exception e) {
			log.fatal("Failed to register user");
			log.fatal(e.getMessage());
			res.setStatusCode(500).end("A server error occured on register, please contact administrator");
		}
	}

	public static void refreshJWT(RoutingContext c) {
		HttpServerResponse res = c.response();
		try {
			JsonObject body = c.getBodyAsJson();
			Jws<Claims> jwt = Jwts.parserBuilder().setSigningKey(JWT_KEY_PAIR.getPublic()).build()
					.parseClaimsJws(body.getString("jwt"));
			Claims claims = jwt.getBody();
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.HOUR, 24);
			claims.setExpiration(cal.getTime());
			JwtBuilder builder = Jwts.builder();
			builder.setClaims(claims);
			builder.signWith(JWT_KEY_PAIR.getPrivate());
			res.setStatusCode(200).end(builder.compact());
		} catch (Exception e) {
			log.warn(e.getMessage());
			res.setStatusCode(500).end("Internal Server error");
		}
	}

	public static void newUser(final String name, final String password, final String ip) throws Exception {
		String ename = StringEscapeUtils.escapeSql(name);
		final PasswordEncoder pw = new BCryptPasswordEncoder();
		final String encpw = pw.encode(password);

		// final Statement st = conn.createStatement();
		// final String sql = String.format("INSERT INTO public.users(id, name,
		// password, ips)"
		// + "VALUES(DEFAULT, '%s', '%s', ARRAY ['%s'::INET]);", ename, encpw, ip);
		// st.executeUpdate(sql);
		SQLInsert.insert("users", new ValuePair("name", str(ename)), new ValuePair("password", str(encpw)),
				new ValuePair("ips", "ARRAY [" + str(ip) + "::INET]"));
	}

	private static boolean checkPassword(String pw, String hash) {
		PasswordEncoder pe = new BCryptPasswordEncoder();
		return pe.matches(pw, hash);

	}

	private static boolean isRegistered(String name) throws SQLException {
		log.info("Checking user registration for " + name);
		// Statement st = conn.createStatement();
		// String sql = String.format("SELECT name FROM public.users WHERE name='%s'",
		// StringEscapeUtils.escapeSql(name));
		// return st.executeQuery(sql).next();
		return SQLQuery.queryAllWhere("users", "name=" + str(name)).next();
	}

	private static User getUserInfo(String name) throws Exception {
		log.info("Getting user info for: " + name);
		// Statement st = conn.createStatement();
		// String sql = String.format(
		// "SELECT users.name, users.password, array_agg(permissions.name) FROM users
		// LEFT JOIN permissions ON users.permissions & permissions.bit != 0"
		// + "WHERE users.name='%s' GROUP BY users.id;",
		// StringEscapeUtils.escapeSql(name));
		// ResultSet res = st.executeQuery(sql);
		ResultSet res = SQLQuery.queryJoinTypeNbGroupWhere("users", "permissions",
				"users.permissions & permissions.bit != 0", "LEFT",
				"users.name, users.password, array_agg(permissions.name)", "users.name=" + str(name), "users.id");

		User u = new User();
		if (res.next()) {
			u.name = res.getString("name");
			u.pwHash = res.getString("password");
			u.perms = (String[]) res.getArray(3).getArray();
		}
		return u;
	}

	private static String getPwHash(String name) throws Exception {
		log.info("Getting hash for " + name);
		// Statement st = conn.createStatement();
		// String sql = String.format("SELECT password FROM public.users WHERE
		// name='%s'",
		// StringEscapeUtils.escapeSql(name));
		// ResultSet res = st.executeQuery(sql);
		ResultSet res = SQLQuery.queryWhere("users", "password", "name=" + str(name));
		if (res.next()) {
			return res.getString(1);
		} else {
			log.warn("No user with the name " + name + " found");
			throw new IllegalArgumentException("No user with the name " + name + " found");
		}
	}

	private static void handleIp(String name, String ip) {
		try {
			SQLUpdate.updateArray("users", "ips", "name = " + str(name), ip);
		} catch (SQLException e) {
			log.warn("Could not handle Ip");
			log.warn(e.getMessage());
		}

	}

	// private static void handleIp(String name, String ip) {
	// // TODO Change:
	// //
	// https://stackoverflow.com/questions/43628837/postgres-append-or-set-each-elementsif-not-exists-of-an-array-to-an-array-colu
	// String sql = "UPDATE public.users SET ips=? WHERE name=?;";
	// try {
	// ResultSet r = queryDb(
	// "SELECT ips FROM public.users WHERE name='" +
	// StringEscapeUtils.escapeSql(name) + "';");
	// if (r.next()) {
	// Array ips = r.getArray(1);
	// ArrayList<Object> ipal = new ArrayList<Object>();
	// for (Object s : (Object[]) ips.getArray()) {
	// if (s.toString().equals(ip)) {
	// return;
	// }
	// ipal.add(s);
	// }
	// log.info("Adding new ip for user " + name + " " + ip);
	// ipal.add(ip);
	// PreparedStatement st = conn.prepareStatement(sql);
	// st.setArray(1, conn.createArrayOf("INET", ipal.toArray()));
	// st.setString(2, StringEscapeUtils.escapeSql(name));
	// st.executeUpdate();
	// }
	// } catch (Exception e) {
	// log.warn("Could not handle Ip");
	// log.warn(e.getMessage());
	// }
	// }

	private static String getJWT(User u) {
		JwtBuilder jwt = Jwts.builder();
		jwt.setIssuer(ISSUER);
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.HOUR, 24);
		jwt.setIssuedAt(new Date());
		jwt.setExpiration(c.getTime());
		jwt.setSubject(u.name);
		jwt.claim("perms", u.perms);
		jwt.signWith(JWT_KEY_PAIR.getPrivate());
		return jwt.compact();
	}

}
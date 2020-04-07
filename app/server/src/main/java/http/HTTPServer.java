package http;

import java.security.KeyPair;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class HTTPServer extends AbstractVerticle {

	private static final Logger log = Logger.getLogger(HTTPServer.class.getName());

	public static final String POSTGRES_URL = "jdbc:postgresql://192.168.178.97:5432/scribble";
	public static final String POSTGRES_USER = "unpriv";
	public static final String POSTGRES_PW = "gMvDapsv586HZ7K74a9i";

	public static final String ISSUER = "scribble-server";

	public static final KeyPair KEY_PAIR = Keys.keyPairFor(SignatureAlgorithm.RS256);

	// public static final RSAKey PUBLIC_KEY = (RSAKey)
	// PemUtils.readPublicKeyFromFile(".\\keys\\public_key.pub", "RSA");
	// public static final RSAKey PRIVATE_KEY = (RSAKey)
	// PemUtils.readPrivateKeyFromFile(".\\keys\\jwt-keys.der", "RSA");

	private final Connection conn;
	private Router router;
	private final int port;

	public HTTPServer(final int port) throws Exception {
		this.port = port;
		conn = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PW);
		log.info("private: " + KEY_PAIR.getPrivate());
		log.info("public: " + KEY_PAIR.getPublic());

		// log.info(PUBLIC_KEY.toString());
		// log.info(PRIVATE_KEY.toString());
	}

	@Override
	public void start(Promise<Void> prom) {
		log.info("Starting server");
		router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.post("/users/register").handler(this::handleUserRegister);
		router.post("/users/login").handler(this::handleUserLogin);
		Route auth = router.post("/auth/:scope").handler(this::handleUserAuth);
		router.post("/api/refresh-jwt").handler(this::refreshJWT);

		router.get("/logout").handler(this::handleUserLogout);

		// ERROR HANDLING
		auth.failureHandler(c -> {
			HttpServerResponse res = c.response();
			int statusCode = c.statusCode();
			if (statusCode == 401) {
				log.warning("Invalid jwt");
				res.setStatusCode(401).end("Your jwt token is invalid, please try logging in again");
			} else if (statusCode == 403) {
				res.setStatusCode(403).end("You do not have access to that scope");
			} else {
				log.severe("Different error!");
				try {
					throw new Exception(c.failure());
				} catch (Exception e) {
					log.severe(e.getMessage());
					e.printStackTrace();
				}
			}
		});

		router.errorHandler(500, cont -> {
			log.warning("Error 500");
			log.warning(cont.normalisedPath());
			try {
				throw new Exception(cont.failure());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		vertx.createHttpServer().requestHandler(router).listen(port, result -> {
			if (result.succeeded()) {
				log.info("Started server on port " + port);
				prom.complete();
			} else {
				log.severe("Failed to start server");
				log.severe(result.cause().getMessage());
				prom.fail(result.cause());
			}
		});
	}

	private void handleUserRegister(RoutingContext c) {
		log.info("New user registration");
		HttpServerResponse res = c.response();
		JsonObject body = c.getBodyAsJson();
		log.info(body.toString());
		String name = body.getString("username");
		if (!Pattern.matches("[A-Za-z0-9-]{3,12}", name)) {
			log.warning("Malformed username: " + name);
			res.setStatusCode(403).end("Malformed username, please only use A-z, 0-9 and '-'!");
			return;
		}
		String pw = body.getString("password");
		String ip = getIp(c);
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
			log.severe("Failed to register user");
			log.severe(e.getMessage());
			res.setStatusCode(500).end("A server error occured on register, please contact administrator");
		}
	}

	private String getIp(RoutingContext c) {
		String ip = c.request().headers().get("X-Real-Ip");
		if (ip == null || ip == "") {
			return c.request().connection().remoteAddress().toString();
			// return "127.0.0.1";
		} else {
			return ip;
		}
	}

	private void handleUserLogin(RoutingContext c) {
		HttpServerResponse res = c.response();
		JsonObject body = c.getBodyAsJson();
		String name = body.getString("username");
		String pw = body.getString("password");
		try {
			new Thread(() -> handleIp(name, getIp(c))).start();
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
			log.severe("Error while logging in");
			log.severe(e.getMessage());
			res.setStatusCode(500).end("Internal Server Error on login");
		}
	}

	private void handleUserAuth(RoutingContext c) {
		JsonObject body = c.getBodyAsJson();
		HttpServerResponse res = c.response();
		String scope = c.request().getParam("scope");
		try {
			Jws<Claims> jwt = Jwts.parserBuilder().setSigningKey(KEY_PAIR.getPublic()).build()
					.parseClaimsJws(body.getString("jwt"));
			Claims claims = jwt.getBody();
			if (claims.getExpiration().compareTo(new Date()) < 0) {
				SimpleDateFormat form = new SimpleDateFormat();
				log.info("Current time: " + form.format(new Date()));
				log.warning("Expired jwt on " + form.format(claims.getExpiration()));
				c.fail(401);
				return;
			}
			if (!claims.getIssuer().equals(ISSUER)) {
				log.warning("Wrong issuer!");
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
				log.warning("Wrong scope");
				c.fail(403);
				return;
			}
			res.setStatusCode(200).end("authenticated");

		} catch (SignatureException e) {
			c.fail(401);
		}
	}

	private void handleUserLogout(RoutingContext c) {
		c.response().setStatusCode(307).end("/logout.html");
	}

	private void refreshJWT(RoutingContext c) {
		HttpServerResponse res = c.response();
		try {
			JsonObject body = c.getBodyAsJson();
			Jws<Claims> jwt = Jwts.parserBuilder().setSigningKey(KEY_PAIR.getPublic()).build()
					.parseClaimsJws(body.getString("jwt"));
			Claims claims = jwt.getBody();
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.HOUR, 24);
			claims.setExpiration(cal.getTime());
			JwtBuilder builder = Jwts.builder();
			builder.setClaims(claims);
			builder.signWith(KEY_PAIR.getPrivate());
			res.setStatusCode(200).end(builder.compact());
		} catch (Exception e) {
			log.warning(e.getMessage());
			res.setStatusCode(500).end("Internal Server error");
		}
	}

	private void newUser(final String name, final String password, final String ip) throws Exception {
		String ename = StringEscapeUtils.escapeSql(name);
		final PasswordEncoder pw = new BCryptPasswordEncoder();
		final String encpw = pw.encode(password);
		final UUID uuid = UUID.nameUUIDFromBytes(ip.getBytes());

		final Statement st = conn.createStatement();
		final String sql = String.format("INSERT INTO public.users(id, name, password, ips, uuid)"
				+ "VALUES(DEFAULT, '%s', '%s', ARRAY ['%s'::INET], '%s');", ename, encpw, ip, uuid.toString());
		st.executeUpdate(sql);
	}

	private boolean checkPassword(String pw, String hash) {
		PasswordEncoder pe = new BCryptPasswordEncoder();
		return pe.matches(pw, hash);

	}

	private boolean isRegistered(String name) throws SQLException {
		log.info("Checking user registration for " + name);
		Statement st = conn.createStatement();
		String sql = String.format("SELECT name FROM public.users WHERE name='%s'", StringEscapeUtils.escapeSql(name));
		return st.executeQuery(sql).next();
	}

	private User getUserInfo(String name) throws Exception {
		log.info("Getting user info for: " + name);
		Statement st = conn.createStatement();
		String sql = String.format(
				"SELECT users.name, users.password, array_agg(permissions.name) FROM users LEFT JOIN permissions ON users.permissions & permissions.bit != 0"
						+ "WHERE users.name='%s' GROUP BY users.id;",
				StringEscapeUtils.escapeSql(name));
		ResultSet res = st.executeQuery(sql);
		User u = new User();
		if (res.next()) {
			u.name = res.getString("name");
			u.pwHash = res.getString("password");
			u.perms = (String[]) res.getArray(3).getArray();
		}
		return u;
	}

	private String getPwHash(String name) throws Exception {
		log.info("Getting hash for " + name);
		Statement st = conn.createStatement();
		String sql = String.format("SELECT password FROM public.users WHERE name='%s'",
				StringEscapeUtils.escapeSql(name));
		ResultSet res = st.executeQuery(sql);
		if (res.next()) {
			return res.getString(1);
		} else {
			log.warning("No user with the name " + name + " found");
			throw new IllegalArgumentException("No user with the name " + name + " found");
		}
	}

	private void handleIp(String name, String ip) {
		String sql = "UPDATE public.users SET ips=? WHERE name=?;";
		try {
			ResultSet r = queryDb(
					"SELECT ips FROM public.users WHERE name='" + StringEscapeUtils.escapeSql(name) + "';");
			if (r.next()) {
				Array ips = r.getArray(1);
				ArrayList<Object> ipal = new ArrayList<Object>();
				for (Object s : (Object[]) ips.getArray()) {
					if (s.toString().equals(ip)) {
						return;
					}
					ipal.add(s);
				}
				log.info("Adding new ip for user " + name + " " + ip);
				ipal.add(ip);
				PreparedStatement st = conn.prepareStatement(sql);
				st.setArray(1, conn.createArrayOf("INET", ipal.toArray()));
				st.setString(2, StringEscapeUtils.escapeSql(name));
				st.executeUpdate();
			}
		} catch (Exception e) {
			log.warning("Could not handle Ip");
			log.warning(e.getMessage());
		}
	}

	private String getJWT(User u) {
		JwtBuilder jwt = Jwts.builder();
		jwt.setIssuer(ISSUER);
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.HOUR, 24);
		jwt.setIssuedAt(new Date());
		jwt.setExpiration(c.getTime());
		jwt.setSubject(u.name);
		jwt.claim("perms", u.perms);
		jwt.signWith(KEY_PAIR.getPrivate());
		return jwt.compact();
	}

	private ResultSet queryDb(String sql) throws SQLException {
		Statement st = conn.createStatement();
		return st.executeQuery(sql);
	}

	public static void main(final String[] args) throws Exception {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
		// System.setProperties("vertexweb.environment", "development");
		final HTTPServer s = new HTTPServer(8080);
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(s);
		// s.newUser("Merlin", "passwort", "127.0.0.1");
	}

}
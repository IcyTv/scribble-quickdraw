package de.icytv.scribble.http;

import java.lang.invoke.MethodHandles;
import java.security.KeyPair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.icytv.scribble.socket.SocketServer;
import de.icytv.scribble.utils.Toolbox;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class HTTPServer extends AbstractVerticle {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String POSTGRES_URL = "jdbc:postgresql://192.168.178.97:5432/scribble";
	public static final String POSTGRES_USER = "unpriv";
	public static final String POSTGRES_PW = "gMvDapsv586HZ7K74a9i";
	public static final KeyPair KEY_PAIR = Toolbox.keyPairNoEx();

	// public static final RSAKey PUBLIC_KEY = (RSAKey)
	// PemUtils.readPublicKeyFromFile(".\\keys\\public_key.pub", "RSA");
	// public static final RSAKey PRIVATE_KEY = (RSAKey)
	// PemUtils.readPrivateKeyFromFile(".\\keys\\jwt-keys.der", "RSA");

	private Router router;
	private final int port;

	public HTTPServer(final int port) throws Exception {
		this.port = port;

		// log.info(PUBLIC_KEY.toString());
		// log.info(PRIVATE_KEY.toString());
	}

	@Override
	public void start(Promise<Void> prom) {
		log.info("Starting server");
		router = Router.router(vertx);

		// API ROUTES
		router.route().handler(BodyHandler.create());
		router.post("/users/register").handler(UserHandler::handleUserRegister);
		router.post("/users/login").handler(UserHandler::handleUserLogin);
		router.get("/users/get-room/:username").handler(UserHandler::handleUserRoom);
		router.get("/rooms/list-users/:username").handler(RoomHandler::handleListUsers);
		router.get("/rooms/join").handler(RoomHandler::handleJoin);
		router.post("/rooms/add/:user").handler(RoomHandler::handleAddUser);
		// router.get("/")

		router.get("/auth/socket").handler(SocketTicketHandler::getTicket);

		Route auth = router.post("/auth/:scope").handler(UserHandler::handleUserAuth);
		router.post("/api/refresh-jwt").handler(UserHandler::refreshJWT);

		router.get("/logout").handler(UserHandler::handleUserLogout);

		router.route("/*").handler(StaticHandler.create("static"));

		router.route().failureHandler(c -> {
			Throwable err = c.failure();
			log.fatal(err.getMessage(), err);
		});

		// ERROR HANDLING
		auth.failureHandler(c -> {
			HttpServerResponse res = c.response();
			int statusCode = c.statusCode();
			if (statusCode == 401) {
				log.warn("Invalid jwt");
				res.setStatusCode(401).end("Your jwt token is invalid, please try logging in again");
			} else if (statusCode == 403) {
				res.setStatusCode(403).end("You do not have access to that scope");
			} else {
				log.fatal(c.normalisedPath(), c.failure());
			}
		});

		router.errorHandler(500, cont -> {
			log.fatal("Error 500");
			log.fatal(cont.normalisedPath(), cont.failure());
		});

		HttpServer s = vertx.createHttpServer();
		s.websocketHandler(new SocketServer(vertx));
		s.requestHandler(router).listen(port, result -> {
			log.info(result);
			if (result.succeeded()) {
				log.info("Started server on port " + port);
				prom.complete();
			} else {
				log.fatal("Failed to start server", result.cause());
				prom.fail(result.cause());
			}
		});

		vertx.exceptionHandler(ev -> {
			log.fatal(ev.getMessage(), ev);
		});

	}

	public static void main(final String[] args) throws Exception {
		// System.setProperty("log4j2.debug", "false");
		System.setProperty("log4j.configurationFile", "./log4j2.xml");
		final HTTPServer s = new HTTPServer(8080);
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(s);
		log.trace("Main done");
	}

}
package socket;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.SharedData;
import utils.JWT;

public class SocketServer implements Handler<ServerWebSocket> {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final Pattern path;
	private final Vertx vertx;
	private final EventBus bus;

	public SocketServer(Vertx vertx) {
		path = Pattern.compile("/socket/([0-9]+)");
		this.vertx = vertx;
		this.bus = this.vertx.eventBus();

		bus.addOutboundInterceptor(ev -> {
			log.trace("Outbound " + ev.message().address());
			ev.next();
		});

	}

	private void registerEventBus(int room, ServerWebSocket event) {
		try {
			bus.<JsonObject>consumer("game.room." + room, msg -> {
				log.debug("Consumer Publishing to " + msg.address());
				event.writeTextMessage(msg.body().encodePrettily());
			});
		} catch (Exception e) {
			log.fatal("exception on send", e);
		}
	}

	@Override
	public void handle(ServerWebSocket event) {
		Matcher m = path.matcher(event.path());
		if (!m.matches()) {
			log.warn("Wrong path");
			event.reject();
			return;
		}
		int room = Integer.parseInt(m.group(1));
		log.trace(room);
		String id = event.textHandlerID();
		log.info("New client with id {} for room {}", id, room);

		registerEventBus(room, event);

		String jwtString;
		try {
			jwtString = parseQuery(event.query(), "jwt");
		} catch (NoSuchFieldException e) {
			log.warn("Jwt token not found in query params", e);
			event.reject(401);
			return;
		}
		log.trace(jwtString);
		JsonObject jwt = getJwtFromString(jwtString);
		log.trace("Jwt: " + jwt.encodePrettily());

		if (JWT.verifyJWT(jwtString) && !isUserInRoom(jwt, room)) {
			log.trace("Valid jwt, logging in");
			storeJwt(jwtString, id);
			vertx.sharedData().<String, Object>getLocalMap("game.room." + room + ".users").put(jwt.getString("sub"), 0);

			event.accept();
			log.trace("Accepted handshake");
		} else {
			log.warn("Invalid jwt or user already in room");
			event.reject(403);
			return;
		}

		// String jwt = event.headers().get("Sec-WebSocket-Protocol").replace("Bearer ",
		// "");
		// log.info(jwt);
		// if (JWT.verifyJWT(jwt)) {
		// data.getLocalMap(id).put("jwt", jwt);
		// event.accept();
		// } else {
		// log.warn("Invalid jwt");
		// event.reject(403);
		// return;
		// }

		event.closeHandler(handleClose(id, room, jwt));

		event.endHandler(handleClose(id, room, jwt));

		event.handler(handleData(id, room, event));

		event.exceptionHandler(handleException(id));

	}

	private JsonObject getJwtFromString(String jwtString) {
		return JWT.parseJwt(jwtString);
	}

	private JsonObject getJwt(String id) {
		return vertx.sharedData().<String, JsonObject>getLocalMap(id).get("jwt");
	}

	private JsonObject storeJwt(String jwtString, String id) {
		JsonObject jwt = JWT.parseJwt(jwtString);
		vertx.sharedData().getLocalMap(id).put("jwt", jwt);
		return jwt;
	}

	private String parseQuery(String query, String paramName) throws NoSuchFieldException {
		String[] params = query.split("\\?");
		for (String param : params) {
			if (param.contains(paramName)) {
				return param.split("=")[1];
			}
		}
		throw new NoSuchFieldException("Query param not found");

	}

	private Handler<Throwable> handleException(String id) {
		return ev -> {
			log.error("Exception on socket", ev);
		};
	}

	private Handler<Buffer> handleData(String id, int room, ServerWebSocket event) {
		return ev -> {
			try {
				JsonObject json = ev.toJsonObject();
				json.put("recieved", Instant.now());
				json.put("currentPlayer", "Merlin");
				DeliveryOptions opts = new DeliveryOptions();
				opts.setSendTimeout(1000);
				bus.publish("game.room." + room, json, opts);
			} catch (Exception e) {
				log.warn("Failed to handle data", e);
				event.end();
			}
		};
	}

	private Handler<Void> handleClose(String id, int room, JsonObject jwt) {
		return ev -> {
			log.info("Disconnecting user " + id + " from room " + room);
			vertx.sharedData().getLocalMap("game.room." + room + ".users").remove(jwt.getString("sub"));
		};

	}

	private boolean isUserInRoom(JsonObject jwt, int room) {
		log.trace(vertx.sharedData().<String, Object>getLocalMap("game.room." + room + ".users").keySet());
		return vertx.sharedData().<String, Object>getLocalMap("game.room." + room + ".users").keySet()
				.contains(jwt.getString("sub"));
	}

}

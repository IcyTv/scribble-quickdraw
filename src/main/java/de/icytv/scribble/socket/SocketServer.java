package de.icytv.scribble.socket;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.icytv.scribble.utils.JWT;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

public class SocketServer implements Handler<ServerWebSocket> {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final Pattern path;
	private Pattern data;
	private final Vertx vertx;
	private final EventBus bus;

	public SocketServer(Vertx vertx) {
		path = Pattern.compile("/socket/([0-9]+)");
		data = Pattern.compile("\\[(\\{\"x\":[0-9.]+,\\s*\"y\":[0-9.]+\\},?\\s*)+\\]");
		log.trace(data.pattern());
		this.vertx = vertx;
		this.bus = this.vertx.eventBus();

		//For debugging purposes
		bus.addOutboundInterceptor(ev -> {
			// log.trace("Outbound " + ev.message().address());
			ev.next();
		});

	}

	private MessageConsumer<JsonObject> registerEventBus(int room, ServerWebSocket event) {
		try {
			MessageConsumer<JsonObject> c = bus.<JsonObject>consumer("game.room." + room, msg -> {
				// log.debug("Consumer Publishing to " + msg.address());
				event.writeTextMessage(msg.body().encodePrettily());
			});
			c.exceptionHandler(ex -> {
				log.error(ex.getMessage(), ex);
			});
			return c;
		} catch (Exception e) {
			log.fatal("exception on send", e);
			return null;
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

		MessageConsumer<JsonObject> c = registerEventBus(room, event);

		String jwtString;
		try {
			jwtString = parseQuery(event.query(), "jwt");
		} catch (NoSuchFieldException e) {
			log.warn("Jwt token not found in query params", e);
			event.reject(401);
			return;
		}
		log.trace(jwtString);
		JsonObject jwt = null;
		try {
			jwt = getJwtFromString(jwtString);
		} catch (Exception e) {
			event.writeTextMessage("{\"exception\":\"" + e.getMessage() + "\"}");
			event.reject();
			return;
		}
		log.trace("Jwt: " + jwt.encodePrettily());

		if (JWT.verifyJWT(jwtString) && !isUserInRoom(jwt, room)) {
			log.trace("Valid jwt, logging in");
			// storeJwt(jwtString, id);
			if (getSet("game.room." + room + ".current").isEmpty()) {
				vertx.sharedData().<String, String>getLocalMap("game.room." + room + ".current").put(id,
						jwt.getString("sub"));
			}
			this.<String, Object>addData(jwt.getString("sub"), 0, "game.room." + room + ".users");

			event.accept();
			log.trace("Accepted handshake");
		} else {
			log.warn("Invalid jwt or user already in room");
			event.reject(403);
			return;
		}

		//event.frameHandler(handleFrame(id, room, jwt));

		event.closeHandler(handleClose(id, room, jwt, c));

		event.endHandler(handleClose(id, room, jwt, c));

		event.handler(handleData(id, room, event));

		event.exceptionHandler(handleException(id));

	}

	private Handler<WebSocketFrame> handleFrame(String id, int room, JsonObject jwt) {
		return ev -> {
			if(ev.isText()) {
				log.debug("Text" , ev);
			} else if(ev.isContinuation()) {
				log.debug("Continuation", ev);
			}
		};
	}

	private JsonObject getJwtFromString(String jwtString) {
		return JWT.parseJwt(jwtString);
	}

	private JsonObject getJwt(String id) {
		return this.<String, JsonObject>getValue("jwt", id);
	}

	private JsonObject storeJwt(String jwtString, String id) {
		JsonObject jwt = JWT.parseJwt(jwtString);
		addData("jwt", jwt, id);
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
				if (!validateData(json)) {
					throw new Exception("Invalid data");
				}
				json.put("recieved", Instant.now());
				String cU = this.<String, String>getMap("game.room." + room + ".current").values().iterator().next();
				json.put("currentPlayer", cU);
				json.put("append", json.getInteger("index") != 0);
				DeliveryOptions opts = new DeliveryOptions();
				bus.publish("game.room." + room, json, opts);
			} catch (Exception e) {
				log.warn("Failed to handle data", e);
				event.writeTextMessage("{\"exception\":\"" + e.getMessage() + "\"}");
				// event.end();
			}
		};
	}

	//TODO actually validate the data
	private boolean validateData(JsonObject json) {
		if (json.containsKey("data")) {
			//return data.matcher(json.getJsonArray("data").encode()).matches();
			return true;
		} else {
			log.warn("NO DATA KEY");
			return false;
		}
		// try {
		// JsonArray arr = json.getJsonArray("data");
		// for (Object obj : arr) {
		// JsonObject jobj = (JsonObject) obj;
		// if (!jobj.containsKey("x") || !jobj.containsKey("y")) {
		// return false;
		// }
		// }
		// log.info("Validated " + arr.encode());
		// log.info(data.matcher(arr.encode()).matches());
		// return true;
		// } catch (ClassCastException | NullPointerException e) {
		// log.info("Data invalid, because " + e.getMessage());
		// return false;
		// }
	}

	public void changeCurrentUser(int room, String id, String name) {
		LocalMap<String, String> map = getMap("game.room." + room + ".current");
		map.clear();
		map.put(id, name);
	}

	private <K, V> void addData(K key, V val, String map) {
		vertx.sharedData().<K, V>getLocalMap(map).put(key, val);
	}

	private <K, V> V getValue(K key, String map) {
		return vertx.sharedData().<K, V>getLocalMap(map).get(key);
	}

	private <K, V> Set<Entry<K, V>> getSet(String map) {
		return vertx.sharedData().<K, V>getLocalMap(map).entrySet();
	}

	private <K, V> LocalMap<K, V> getMap(String map) {
		return vertx.sharedData().<K, V>getLocalMap(map);
	}

	private <K, V> void remove(K key, String map) {
		vertx.sharedData().<K, V>getLocalMap(map).remove(key);
	}

	private <K, V> void removeAll(String map) {
		vertx.sharedData().<K,V>getLocalMap(map).clear();
	}

	private Handler<Void> handleClose(String id, int room, JsonObject jwt, MessageConsumer<JsonObject> c) {
		return ev -> {
			log.info("Disconnecting user " + id + " from room " + room);
			// vertx.sharedData().getLocalMap("game.room." + room +
			// ".users").remove(jwt.getString("sub"));
			c.unregister();
			removeAll(id);
			remove(jwt.getString("sub"), "game.room." + room + ".users");
		};

	}

	private boolean isUserInRoom(JsonObject jwt, int room) {
		log.trace(vertx.sharedData().<String, Object>getLocalMap("game.room." + room + ".users").keySet());
		// return vertx.sharedData().<String, Object>getLocalMap("game.room." + room +
		// ".users").keySet()
		// .contains(jwt.getString("sub"));
		return this.<String, Object>getMap("game.room." + room + ".users").keySet().contains(jwt.getString("sub"));
	}

}

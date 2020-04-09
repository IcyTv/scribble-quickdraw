package socket;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
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
			log.fatal(e);
		}
	}

	@Override
	public void handle(ServerWebSocket event) {
		Matcher m = path.matcher(event.path());
		if (!m.matches()) {
			event.reject();
			return;
		}
		int room = Integer.parseInt(m.group(1));
		String id = event.textHandlerID();
		log.info("New client with id {} for room {}", id, room);

		registerEventBus(room, event);

		SharedData data = vertx.sharedData();
		log.trace(event.headers());
		String jwt = event.headers().get("Sec-WebSocket-Protocol").replace("Bearer ", "");
		log.info(jwt);
		if (JWT.verifyJWT(jwt)) {
			data.getLocalMap(id).put("jwt", jwt);
			event.accept();
		} else {
			log.warn("Invalid jwt");
			event.reject(403);
			return;
		}
		event.closeHandler(handleClose(id, room));

		event.handler(handleData(id, room, event));

		event.exceptionHandler(handleException(id));

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
				DeliveryOptions opts = new DeliveryOptions();
				opts.setSendTimeout(1000);
				bus.publish("game.room." + room, json, opts);
			} catch (Exception e) {
				log.warn("Failed to handle data", e);
				event.reject();
			}
		};
	}

	private Handler<Void> handleClose(String id, int room) {
		return ev -> {
			log.info("Disconnecting user " + id + " from room " + room);
		};

	}

}

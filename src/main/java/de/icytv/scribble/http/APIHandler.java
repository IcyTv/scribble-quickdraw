package de.icytv.scribble.http;

import java.lang.invoke.MethodHandles;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class APIHandler {
	
	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public final Router router;
	private final BodyHandler bodyHandler;

	public APIHandler(Vertx vertx) {
		this(Router.router(vertx));
	}

	public APIHandler(Router router) {
		this.router = router;
		this.bodyHandler = BodyHandler.create();
		this.router.route().handler(this.bodyHandler);

		this.router.errorHandler(403, err -> {
			String errMsg = new String(Base64.getEncoder().encode("You are not authenticated, please try logging in again".getBytes()));
			err.response().setStatusCode(302).end("/index.html?error=" + errMsg);
		});

	}

	public void initUserRoutes() {
		router.post("/api/users/register").handler(UserHandler::handleUserRegister);
		router.post("/api/users/login").handler(UserHandler::handleUserLogin);
		router.get("/api/users/logout").handler(UserHandler::handleUserLogout);
		router.get("/api/users/rooms/:username").handler(UserHandler::handleUserRoom);
		router.get("/api/users/refresh").handler(UserHandler::refreshJWT);
		router.get("/api/users/list").handler(UserHandler::handleListUsers);
		router.put("/api/users/change-password/:username").handler(UserHandler::handlePwReset);

		router.get("/api/users/scope/:scope").handler(UserHandler::handleUserScope);
		router.get("/api/users/scope").handler(UserHandler::handleUserScope);

		router.get("/api/users/:username").handler(UserHandler::handleGetUserInfo);

	}

	public void initRoomRoutes() {
		router.get("/api/rooms/:username").handler(RoomHandler::handleListUsers);

		router.get("/api/rooms/users/:room");
		router.get("/api/rooms/add/:room/:username").handler(RoomHandler::handleAddUser);
		router.get("/api/rooms/add/:room").handler(RoomHandler::handleAddUser);
		router.get("/api/rooms/remove/:room/:username").handler(RoomHandler::handleUserRemove);
		router.get("/api/rooms/remove/:room").handler(RoomHandler::handleRoomRemove);
	}

	public Router getRouter() {
		return router;
	}

}
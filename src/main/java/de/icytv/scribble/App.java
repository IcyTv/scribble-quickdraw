package de.icytv.scribble;

import de.icytv.scribble.http.HTTPServer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

public class App {
	public static void main(String[] args) throws Exception {
		System.setProperty("log4j.configurationFile", "./log4j2.xml");
		final HTTPServer s = new HTTPServer(8080);
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(s);
	}

	public static void test(Handler<AsyncResult<HttpServer>> t) {
		
	}
}
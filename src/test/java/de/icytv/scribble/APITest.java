package de.icytv.scribble;

import static io.vertx.junit5.web.TestRequest.statusCode;
import static io.vertx.junit5.web.TestRequest.testRequest;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.icytv.scribble.http.HTTPServer;
import de.icytv.scribble.utils.JWT;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.junit5.web.VertxWebClientExtension;
import io.vertx.junit5.web.WebClientOptionsInject;

@ExtendWith({ VertxExtension.class, VertxWebClientExtension.class })
@DisplayName("Api tests")
public class APITest {

	public static int PORT = 8080;

	@WebClientOptionsInject
	public WebClientOptions opts = new WebClientOptions().setDefaultPort(PORT);

	@BeforeEach
	@DisplayName("Deploy Server")
	public void start(Vertx vertx, VertxTestContext testContext) throws Throwable {
		vertx.deployVerticle(new HTTPServer(PORT, false, true, false), testContext.completing());
	}

	@Test
	@DisplayName("Test user login")
	public void testLogin(WebClient client, VertxTestContext testContext) {
		JsonObject data = new JsonObject();
		data.put("username", "test");
		data.put("password", "password");
		testRequest(client, HttpMethod.POST, "/users/login").sendJson(data, testContext).onComplete((dt) -> {
			assertThat(JWT.verifyJWT(dt.result().bodyAsString())).isTrue();
		});
	}

	@Test
	@DisplayName("Test")
	public void testUserRoom(WebClient client, VertxTestContext testContext) {
		testRequest(client, HttpMethod.GET, "/users/get-room/test").expect(statusCode(200)).send(testContext);
	}

	@AfterEach
	@DisplayName("Verticle still up?")
	public void lastChecks(Vertx vertx) {
		assertThat(vertx.deploymentIDs()).isNotEmpty().hasSize(1);
	}

}
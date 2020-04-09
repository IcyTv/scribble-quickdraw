package http;

import java.util.Calendar;
import java.util.Date;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import utils.JWT;
import utils.Toolbox;

public class SocketTicketHandler {

	public static final String ISSUER = "scribble-socket-ticket";
	public static final String SUBJECT = "socket-auth";

	public static void getTicket(RoutingContext c) {
		HttpServerResponse res = c.response();
		String jwt = c.request().getHeader("Authorization");
		if (!jwt.contains("Bearer ")) {
			c.fail(401);
			return;
		}
		if (JWT.verifyJWT(jwt.replace("Bearer ", ""))) {
			JwtBuilder builder = Jwts.builder();
			builder.setIssuer(ISSUER);
			builder.setSubject(SUBJECT);
			builder.setAudience(Toolbox.getIp(c));
			builder.setIssuedAt(new Date());

			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.HOUR, 3);
			builder.setExpiration(cal.getTime());
			builder.signWith(HTTPServer.KEY_PAIR.getPrivate());
			res.setStatusCode(200);
			res.end(builder.compact());

		} else {
			c.fail(401);
		}

	}

}

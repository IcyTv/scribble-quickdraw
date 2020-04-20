package de.icytv.scribble.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.security.KeyPair;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.icytv.scribble.sql.SQLQuery;
import io.vertx.ext.web.RoutingContext;

public abstract class Toolbox {

	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static String readFromInputStream(InputStream inputStream) throws IOException {
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

	public static String readFromFile(String path) {
		File f = new File(path);
		if (!f.exists()) {
			throw new IllegalArgumentException("Path does not exist!");
		}
		try {
			return readFromInputStream(new FileInputStream(f));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public static void writeToFile(String path, String data) throws IOException {
		File f = new File(path);
		f.createNewFile();
		FileWriter writer = new FileWriter(f);
		writer.write(data);
		writer.close();
	}

	public static boolean exists(String path) {
		File f = new File(path);
		return f.exists();
	}

	public static KeyPair keyPairNoEx() {
		try {
			return KeyUtils.getKeyPair();
		} catch (Exception e) {
			log.warn("Error on keys!", e);
			return null;
		}
	}

	public static String getIp(RoutingContext c) {
		String ip = c.request().headers().get("X-Real-Ip");
		InetAddressValidator validator = new InetAddressValidator();
		try {
			if (ip == null || ip == "" || !validator.isValid(ip)) {
				ip = c.request().connection().remoteAddress().toString();
				// return "127.0.0.1";
			}
			log.info(validator.isValid(ip) ? "valid" : "not valid " + ip);
			return ip;
		} catch (Exception e) {
			log.warn(e.getMessage());
			return "127.0.0.1";
		}
	}

	public static boolean isMissingParam(RoutingContext c, String param) {
		return c.request().getParam(param) == null || "".equals(c.request().getParam(param));
	}

	public static int getUID(String name) throws SQLException {
		ResultSet set = SQLQuery.queryWhere("users", "id", "name='" + name + "'");
		log.info(set.next() ? "Getting uid": "Getting uid failed");
		return set.getInt(1);

	}
}
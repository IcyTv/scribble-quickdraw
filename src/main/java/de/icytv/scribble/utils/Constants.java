package de.icytv.scribble.utils;

import java.lang.invoke.MethodHandles;
import java.security.KeyPair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Constants {
	private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final KeyPair JWT_KEY_PAIR = Toolbox.keyPairNoEx();

	public static final String ISSUER = "scribble-server";

	private Constants() {
		log.error("Called the Constants constructor!!");
		throw new AssertionError();
	}

}
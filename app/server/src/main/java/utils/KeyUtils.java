package utils;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public class KeyUtils {

	private static KeyPair active = null;

	public static KeyPair getKeyPair() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		if (active == null) {
			PemReader parser = new PemReader(new StringReader(Toolbox.readFromFile("keys/private_key.pem")));
			PemReader pubParser = new PemReader(new StringReader(Toolbox.readFromFile("keys/public_key.pem")));
			PemObject pem = parser.readPemObject();
			PemObject pubPem = pubParser.readPemObject();
			KeyFactory kf = KeyFactory.getInstance("RSA"); // or "EC" or whatever
			PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pem.getContent()));
			PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubPem.getContent()));
			active = new KeyPair(pubKey, privateKey);
		}
		return active;
	}
}
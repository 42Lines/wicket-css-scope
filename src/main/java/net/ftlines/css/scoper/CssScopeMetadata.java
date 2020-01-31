package net.ftlines.css.scoper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Random;
import java.util.function.Supplier;

public class CssScopeMetadata {

	private Properties properties;

	public CssScopeMetadata(Properties properties) {
		this.properties = properties;
	}

	public String getValue(String key, Supplier<String> defaultIfMissing) {
		String newToken = properties.getProperty(key);
		if (newToken == null) {
			newToken = defaultIfMissing.get();
			properties.setProperty(key, newToken);
		}

		return newToken;
	}

	public void setValue(String key, String value) {
		properties.setProperty(key, value);
	}

	public static String generateRandomString() {
		int leftLimit = 97; // letter 'a'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 5;
		Random random = new Random();

		return random.ints(leftLimit, rightLimit + 1).limit(targetStringLength)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	public static String hashString(String message) {

		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] hashedBytes = digest.digest(message.strip().getBytes("UTF-8"));

			return convertByteArrayToHexString(hashedBytes);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
			throw new RuntimeException("Could not generate hash from String", ex);
		}
	}

	private static String convertByteArrayToHexString(byte[] arrayBytes) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < arrayBytes.length; i++) {
			stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuffer.toString();
	}

	public static String getMetaDataAsString(CssScopeMetadata metaData) {
		StringWriter writer = new StringWriter();
		try {
			metaData.properties.store(writer, "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return writer.getBuffer().toString();
	}

}

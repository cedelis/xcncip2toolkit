package org.extensiblecatalog.ncip.v2.koha.util;

public class KohaException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2866811087095922747L;

	private static final String DEFAULT_SHORT_MESSAGE = "Internal connector error";
	private static final String TOO_MANY_LOGIN_ATTEMPTS = "Too many login attempts";

	public static final String INVALID_ITEM_IDENTIFIER_VALUE_FORMAT = "Invalid ItemIdentifierValue format";
	public static final String ITEM_NOT_FOUND = "Item not found";
	public static final String BAD_REQUEST_400 = "400 Bad Request";

	private String shortMessage;

	public KohaException(String message) {
		this(DEFAULT_SHORT_MESSAGE, message);
	}

	public KohaException(String shortMessage, String message) {
		super(message);
		this.shortMessage = shortMessage;
	}

	public String getShortMessage() {
		return shortMessage;
	}

	public static KohaException create400BadRequestException(String message) {
		return new KohaException(
				KohaException.BAD_REQUEST_400,
				"Invalid syntax provided. This is usually caused by invalid toolkit's logic or by ILS' updates. Either way needs to solve this problem by toolkit's code renewal.\n\nError description: "
						+ message);
	}

	public static KohaException createCommonException(int status, String message) {
		return new KohaException(String.valueOf(status), message);
	}

	public static KohaException createTooManyLoginAttempts() {
		return new KohaException(KohaException.TOO_MANY_LOGIN_ATTEMPTS,
				"There has been too many login attempts. Please verify your admin credentials specified in toolkit.properties.");
	}
}

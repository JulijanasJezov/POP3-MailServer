import java.util.ArrayList;
import java.util.Collections;

public class CommandInterpreter implements CommandInterpreterInterface {

	private static final String AUTHORIZATION = "AUTHORIZATION";
	private static final String TRANSACTION = "TRANSACTION";
	private static final String UPDATE = "UPDATE";
	private static final String SUCCESSFUL = "+OK";
	private static final String UNSUCCESSFUL = "-ERR";

	private String state = AUTHORIZATION;
	DatabaseInterface db = DatabaseBackend.getSingletonObject();

	private String authorizedUser = "";
	ArrayList<Integer> markedMessages = new ArrayList<Integer>();

	public String getState() {
		return state;
	}

	/*
	 * handleInput method is responsible for checking a command passed if the
	 * commands isn't under the current state it will fail command will be
	 * passed to its method and return a response based on command and arguments
	 * each switch case will also check if the arguments passed are valid or no
	 * arguments required
	 */
	public String handleInput(String input) {
		int msg = 0;
		if (input.indexOf("\r\n") != -1) {
			input = input.substring(0, input.indexOf("\r\n"));
		}
		String rest = null;
		String splitString[] = input.split(" ", 2);
		String command = splitString[0];
		command = command.toUpperCase();
		if (splitString.length > 1) {
			rest = splitString[1];
		}

		switch (state) {
		case AUTHORIZATION:
			switch (command) {
			case "USER":
				if (rest == null) {
					return UNSUCCESSFUL + " " + input;
				}
				authorizedUser = rest;
				return userCommand(rest);

			case "PASS":
				if (rest == null) {
					return UNSUCCESSFUL + " " + input;
				}
				return passCommand(authorizedUser, rest);

			case "QUIT":
				if (rest != null) {
					return UNSUCCESSFUL + " " + input;
				}
				return quitCommand();

			default:
				return UNSUCCESSFUL + " command not recognized";
			}

		case TRANSACTION:
			switch (command) {
			case "STAT":
				if (rest != null) {
					return UNSUCCESSFUL + " " + input;
				}
				return statCommand();

			case "LIST":
				if (rest != null && checkIfNumeric(rest) && !rest.equals("")) {
					msg = Integer.parseInt(rest);
					if (msg != 0) {
						return listCommand(msg);
					}
				} else if (rest == null) {
					return listCommand(0);
				} else {
					return UNSUCCESSFUL + " " + input;
				}

			case "RETR":
				if (rest != null && checkIfNumeric(rest) && !rest.equals("")) {
					msg = Integer.parseInt(rest);
					if (msg != 0) {
						return retrCommand(msg);
					}
				}
				return UNSUCCESSFUL + " " + input;

			case "DELE":
				if (rest != null && checkIfNumeric(rest) && !rest.equals("")) {
					msg = Integer.parseInt(rest);
					if (msg != 0) {
						return deleCommand(msg);
					}
				}
				return UNSUCCESSFUL + " " + input;

			case "NOOP":
				if (rest != null) {
					return UNSUCCESSFUL + " " + input;
				}
				return SUCCESSFUL;

			case "RSET":
				if (rest != null) {
					return UNSUCCESSFUL + " " + input;
				}
				return rsetCommand();

			case "TOP":
				if (rest == null) {
					return UNSUCCESSFUL + " " + input;
				}
				return topCommand(rest);

			case "UIDL":
				if (rest != null && checkIfNumeric(rest) && !rest.equals("")) {
					msg = Integer.parseInt(rest);
					if (msg != 0) {
						return uidlCommand(msg);
					}
				} else if (rest == null) {
					return uidlCommand(0);
				} else {
					return UNSUCCESSFUL + " " + input;
				}

			case "QUIT":
				if (rest != null) {
					return UNSUCCESSFUL + " " + input;
				}
				return quitCommand();

			default:
				return UNSUCCESSFUL + " command not recognized";
			}
		default:
			return UNSUCCESSFUL + " no such state";
		}
	}

	/*
	 * each command method is responsible to provide response message based on
	 * the arguments passed and also changing state where required
	 */
	private String userCommand(String name) {
		if (db.existsUser(name)) {
			return SUCCESSFUL
					+ " welcome nice to meet you USER "
					+ name;
		} else {
			return UNSUCCESSFUL
					+ " no USER "
					+ name;
		}
	}

	private String passCommand(String user, String pass) {
		if (db.checkPassword(user, pass)) {
			state = TRANSACTION;
			return SUCCESSFUL + " mail is unclocked";
		} else {
			return UNSUCCESSFUL + " invalid password";
		}
	}

	private String quitCommand() {
		if (state == AUTHORIZATION) {
			return SUCCESSFUL + " POP3 server signing off";
		} else {
			state = UPDATE;
			if (markedMessages == null) {
				if (db.checkMessages(authorizedUser)[0] == 0) {
					db.lockUser(false, authorizedUser);
					return SUCCESSFUL
							+ " POP3 server signing off (maildrop empty)";
				} else {
					db.lockUser(false, authorizedUser);
					return SUCCESSFUL + " POP3 server signing off ("
							+ db.checkMessages(authorizedUser)[0] + " messages left)";
				}
			} else {
				int totalMessages = db.checkMessages(authorizedUser)[0];
				Collections.sort(markedMessages);
				Collections.reverse(markedMessages);
				db.deleteMessages(markedMessages, authorizedUser);
				if (db.checkMessages(authorizedUser)[0] != totalMessages
						- markedMessages.size()) {
					state = TRANSACTION;
					return UNSUCCESSFUL + " some deleted messages not removed";
				}

				if (db.checkMessages(authorizedUser)[0] == 0) {
					db.lockUser(false, authorizedUser);
					return SUCCESSFUL
							+ " POP3 server signing off (maildrop empty)";
				} else {
					db.lockUser(false, authorizedUser);
					return SUCCESSFUL + " POP3 server signing off ("
							+ db.checkMessages(authorizedUser)[0] + " messages left)";
				}
			}
		}

	}

	private String statCommand() {
		int[] messages = db.checkMessages(authorizedUser);
		int remainingMsg = messages[0] - markedMessages.size();
		int remainingOctets = messages[1];
		for (int msg : markedMessages) {
			remainingOctets -= db.getMessageOctets(msg, authorizedUser);
		}
		return SUCCESSFUL + " " + remainingMsg + " " + remainingOctets;
	}

	private String listCommand(int msg) {
		int[] messages = db.checkMessages(authorizedUser);
		StringBuilder returnMessage = new StringBuilder();

		if (msg > messages[0]) {
			return UNSUCCESSFUL + " no such message, only " + messages[0]
					+ " messages in maildrop";

		} else if (msg < messages[0] && msg != 0) {
			return SUCCESSFUL + " " + msg + " " + db.getMessageOctets(msg, authorizedUser);

		} else {
			int remainingMsg = messages[0] - markedMessages.size();
			int remainingOctets = messages[1];
			for (int message : markedMessages) {
				remainingOctets -= db.getMessageOctets(message, authorizedUser);
			}
			returnMessage.append(SUCCESSFUL + " " + remainingMsg + " messages ("
					+ remainingOctets + " octets)\r\n");
			
			for (msg = 1; msg <= messages[0]; msg++) {
				if (!checkMarkedMessages(msg))
					returnMessage.append(msg + " " + db.getMessageOctets(msg, authorizedUser)
							+ "\r\n");
			}
			String output = returnMessage.toString();
			output += ".";
			return output;
		}
	}

	private String retrCommand(int msg) {
		String returnMessage = "";
		if (msg <= db.checkMessages(authorizedUser)[0]) {
			returnMessage = SUCCESSFUL + " " + db.getMessageOctets(msg, authorizedUser)
					+ " octets\r\n";
			returnMessage +=  db.getMessage(msg, authorizedUser) + "\r\n.";
			return returnMessage;
		} else {
			return UNSUCCESSFUL + " no such message";
		}
	}

	private String deleCommand(int msg) {
		if (msg <= db.checkMessages(authorizedUser)[0] && !checkMarkedMessages(msg)) {
			markMessage(msg);
			return SUCCESSFUL + " message" + msg + " deleted";
		} else if (checkMarkedMessages(msg)) {
			return UNSUCCESSFUL + " message" + msg + " already deleted";
		} else {
			return UNSUCCESSFUL + " no such message";
		}
	}

	private String rsetCommand() {
		unmarkMessages();
		int[] messages = db.checkMessages(authorizedUser);
		return SUCCESSFUL + " " + messages[0] + " messages (" + messages[1]
				+ " octets)";
	}

	private String topCommand(String args) {
		String splitString[] = args.split(" ", 2);
		String sMsg = splitString[0];
		String sNumLines = null;
		if (splitString.length > 1) {
			sNumLines = splitString[1];
		}
		if (sNumLines == null || sNumLines.equals("")) {
			return UNSUCCESSFUL + " TOP " + args;
		}
		if (!checkIfNumeric(sMsg) || !checkIfNumeric(sNumLines)) {
			return UNSUCCESSFUL + " TOP " + args;
		}
		int msg = Integer.parseInt(sMsg);
		int numLines = Integer.parseInt(sNumLines);
		String returnMessage = "";

		if (!checkMarkedMessages(msg) && numLines >= 0
				&& msg <= db.checkMessages(authorizedUser)[0]) {
			returnMessage = SUCCESSFUL + "\r\n";
			return returnMessage + db.getMessageLines(msg, numLines, authorizedUser)
					+ "\r\n.";
		}

		return UNSUCCESSFUL + " no such message";
	}

	private String uidlCommand(int msg) {
		int[] messages = db.checkMessages(authorizedUser);
		String returnMsg = "";

		if (msg <= messages[0] && !checkMarkedMessages(msg) && msg != 0) {
			return SUCCESSFUL + " " + msg + " " + db.getUIDL(msg, authorizedUser);
		} else if (msg > messages[0]) {
			return UNSUCCESSFUL + " no such message";
		} else {
			returnMsg = SUCCESSFUL + "\r\n";
			for (int count = 1; count <= messages[0]; count++) {
				if (!checkMarkedMessages(count)) {
					returnMsg += count + " " + db.getUIDL(count,authorizedUser)
							+ "\r\n";
				}	
			}
			returnMsg += ".";
			return returnMsg;
		}
	}

	// this will mark messages that user wants to delete
	private void markMessage(int msg) {
		markedMessages.add(msg);
	}

	// this will be called in rset command to unmark all the messages
	private void unmarkMessages() {
		for (int msg = 0; msg < markedMessages.size(); msg++) {
			markedMessages.remove(msg);
		}
	}

	// this method checks the message passed if it's marked or not
	private boolean checkMarkedMessages(int msg) {
		if (markedMessages.contains(msg)) {
			return true;
		}
		return false;
	}

	// this method will check if the string is numeric
	private boolean checkIfNumeric(String rest) {
		for (char c : rest.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}
	
	public void unlockUser() {
		db.lockUser(false, authorizedUser);
	}
}


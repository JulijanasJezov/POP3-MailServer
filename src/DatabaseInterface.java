import java.util.ArrayList;

public interface DatabaseInterface {
	/**
	 * This method checks if username exists in the database
	 * 
	 * @param user
	 *            user's username
	 * @return true/false if username exists
	 */
	boolean existsUser(String user);

	/**
	 * This method checks if password matches the username in the database
	 * 
	 * @param user
	 *            user's username
	 * @param pass
	 *            user's password
	 * @return true/false if password matches the username
	 */
	boolean checkPassword(String user, String pass);

	/**
	 * This method gets the number of messages and size in octets of all the
	 * messages
	 * 
	 * @param user
	 *            user's username
	 * @return array of two integers consisting of number of messages and octets
	 *         size
	 */
	int[] checkMessages(String user);

	/**
	 * This method gets the size of mail messages in octets
	 * 
	 * @param user
	 *            user's username
	 * @return size in octets of all the messages
	 */
	int getMailOctets(String user);

	/**
	 * This method gets one message's size in octets
	 * 
	 * @param msg
	 *            message ID given in CI class
	 * @param user
	 *            user's username
	 * @return particular message's size in octets
	 */
	int getMessageOctets(int msg, String user);

	/**
	 * This method gets the content of a message that is passed
	 * 
	 * @param msg
	 *            message ID given in CI class
	 * @param user
	 *            user's username
	 * @return string containing particular message's content
	 */
	String getMessage(int msg, String user);

	/**
	 * This method gets the number of lines of content of a message
	 * 
	 * @param msg
	 *            message ID given in CI class
	 * @param lines
	 *            number of lines for TOP command
	 * @param user
	 *            user's username
	 * @return string of the message's content containing only the number of
	 *         lines passed
	 */
	String getMessageLines(int msg, int lines, String user);

	/**
	 * This method deletes marked messages from the database
	 * 
	 * @param msgs
	 *            list of message IDs given in CI to be deleted
	 * @param user
	 *            user's username
	 */
	void deleteMessages(ArrayList<Integer> msgs, String user);

	/**
	 * This method gets the UIDL of a message passed
	 * 
	 * @param msg
	 *            message ID given in CI class
	 * @param user
	 *            user's username
	 * @return UIDL string from the database
	 */
	String getUIDL(int msg, String user);

	/**
	 * This method locks/unlocks user's account
	 * 
	 * @param lock
	 *            boolean value to lock or unlock the user's account
	 * @param user
	 *            user's username
	 */
	void lockUser(boolean lock, String user);
	
	/**
	 * This method closes the database connection
	 */
	void closeConnection();
}

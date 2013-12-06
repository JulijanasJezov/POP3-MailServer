public interface CommandInterpreterInterface {

	/**
	 * This method is responsible for handling all the POP3 commands
	 * 
	 * @param input
	 *            string of a command
	 * @return string output of a processed command
	 */
	String handleInput(String input);

	/**
	 * This method unlocks the current user
	 * this is used when connection times out
	 */
	void unlockUser();
}

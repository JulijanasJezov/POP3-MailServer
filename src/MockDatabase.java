import java.util.ArrayList;


public class MockDatabase {
	
	public static String user = "test password";
	String splitString[] = user.split(" ", 2);
	String username = splitString[0];
	String password = splitString[1];		
			
	public boolean existsUser(String user) {
		boolean bool;
		if (user.equals(username)) {
			bool = true;
		} else {
			bool = false;
		}
		return bool;
	}
	
	public boolean checkPassword(String user, String pass) {
		boolean bool;
		if (pass.equals(password) && user.equals(username)) {
			bool = true;
		} else {
			bool = false;
		}
		return bool;
	}

	public int[] checkMessages() {
		int messages = 0;
		int octets = getMailOctets(messages);
		
		int[] both = {messages, octets};
		return both;
	}
	
	public int getMailOctets(int totalMsg){
		int totalOctets = 0;
		int msg = 0;
		
		for (msg = 0; msg < totalMsg; msg++) {
			totalOctets += getMessageOctets(msg);
		}
		return totalOctets;
	}
	
	public int getMessageOctets(int msg) {
		return 0;
	}
	
	public String getMessage(int msg) {
		return "";
	}
	
	public String getMessageLines(int msg, int lines) {
		String message = getMessage(msg);
		String newLine = "\n";
		int ind;
		String returnMessage = "";
		
		for (int i = 0; i < lines; i++) {
			ind = message.indexOf(newLine, i);
			returnMessage += message.substring(0, ind);
			message = message.substring(ind);
		}
		
		
		return returnMessage;
	}
	
	public void deleteMessages (ArrayList<Integer> msgs) {
		
	}
}

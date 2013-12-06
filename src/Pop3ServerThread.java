import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Pop3ServerThread extends Thread {
	private static final boolean CONNECTION_CLOSED = true;
	private static final boolean CONNECTION_TIMEOUT = false;
	private static final String EXIT_MESSAGE = "+OK POP3 server signing off";
	private static final String ERROR_CONNECTION_CLOSED = " connecion has been closed";
	private static final String ERROR_CONNECTION_TIMEOUT = " connecion has timed out";
	private static final String ERROR_STREAM = "Stream error occured";
	private static final String ERROR_SOCKET_CLOSE = "Could not close the socket";
	private static final String ERROR_TIMER = "Setting timer error occured";
	private static final String GREETING = "+OK POP3 server ready";
	private String connectionIP;
	private Socket socket = null;
	
	Pop3ServerThread(Socket socket, int timeOut) {
		this.socket = socket;
		connectionIP = socket.getRemoteSocketAddress().toString();
		try {
			socket.setSoTimeout(timeOut * 1000);
		}
		catch (SocketException e) {
			System.err.println(ERROR_TIMER);
		}
	}

	/* Pop3ServerThread class allows client to communicate with the command interpreter
	 * it reads the input from a client and passes it to the command interpreter to print out what it returns
	 * it also sets a timeout for a connection and ends the connection if time has ran out
	 */
	
	public void run() {
		CommandInterpreter cmdint = new CommandInterpreter();
		try {
			InputStreamReader inputStream = new InputStreamReader(socket.getInputStream());
			PrintWriter printOut = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader printIn = new BufferedReader(inputStream);
			
			String input, output;
			printOut.println(GREETING);
			
			while ((input = printIn.readLine()) != null) {
				output = cmdint.handleInput(input);
				printOut.println(output);
				if (output.startsWith(EXIT_MESSAGE))
					break;
			}
			printOut.close();
			printIn.close();
			inputStream.close();
			closeConnection(CONNECTION_CLOSED);
		} catch (SocketTimeoutException e) {
			closeConnection(CONNECTION_TIMEOUT);
			cmdint.unlockUser();
		} catch (IOException e) {
			System.err.println(ERROR_STREAM);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.err.println(ERROR_SOCKET_CLOSE);
			}
		}
	}
	
	public void closeConnection(boolean reason) {
		if (reason) {
			System.err.println(connectionIP + ERROR_CONNECTION_CLOSED);
		} else {
			System.err.println(connectionIP + ERROR_CONNECTION_TIMEOUT);
		}
		
	}
}
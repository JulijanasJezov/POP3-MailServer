import java.io.IOException;
import java.net.ServerSocket;

/* I have tested the network using the telnet from schools unix server
 * Tests included: multiple connections, timeouts, closed connections.
 */
public class Pop3Server {
	private final static int DEFAULT_TIMEOUT = 600;
	private final static String ERROR_ARGUMENTS = "Wrong arguments";
	private final static String ERROR_NUM_ARGUMENTS = "Wrong number of arguments";
	private final static String ERROR_CONNECTION = "Cannot establish connection";
	private int port;
	private int timeOut;

	Pop3Server(int port, int timeOut) {
		this.port = port;
		this.timeOut = timeOut;
	}

	/* Pop3Server class is responsible for opening a server socket with the passed port in the arguments
	 * it has an infinite loop to listen to the connections made to that created socket
	 * when the connection has been made it creates a new thread passing the connection socket and 'timeout' time
	 * it allows to make connections for multiple users by creating a new thread with different connection socket
	 * main method is checking if the arguments passed are valid
	 */
	
	public static void main(String[] args) {
		int port = 0;
		int timeOut = 0;

		try {
			if (args.length == 2) {
				port = Integer.parseInt(args[0]);
				timeOut = Integer.parseInt(args[1]);
				if (timeOut < 0 || port < 0) {
					System.err.println(ERROR_ARGUMENTS);
					System.exit(1);
				}
			} else if (args.length == 1) {
				port = Integer.parseInt(args[0]);
				timeOut = DEFAULT_TIMEOUT;
				if(port < 0) {
					System.err.println(ERROR_ARGUMENTS);
					System.exit(1);
				}
			} else {
				System.err.println(ERROR_NUM_ARGUMENTS);
				System.exit(1);
			}
		} catch (NumberFormatException e) {
			System.err.println(ERROR_ARGUMENTS);
			System.exit(1);
		}
		Pop3Server server = new Pop3Server(port, timeOut);
		server.startServer();
	}

	public void startServer() {
		boolean running = true;

		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			while (running) {
				Pop3ServerThread serverThread = new Pop3ServerThread(
						serverSocket.accept(), timeOut);
				serverThread.start();
			}
		} catch (IOException e) {
			System.err.println(ERROR_CONNECTION);
			System.exit(1);
		} finally {
			DatabaseInterface db = DatabaseBackend.getSingletonObject();
			db.closeConnection();
		}
	}
	
}

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/*
 * DatabaseBackend Class
 * ============================================================
 * DatabaseBackend class is used to handle the database queries, updating the database
 * or returning data from it
 * This uses a singleton pattern to allow only one instance of this object to be created
 * to avoid multiple database connections
 * It's using PreparedStatement over Statement for security reasons, to avoid SQL injections
 * All the methods work in similar way by having a mysql query in a string and executing it
 * putting the result into ResulSet and getting the data extracted from there
 * 
 * Command Interpreter Changes
 * ===========================================================
 * I added to pass user parameter to all the database methods called in CI class
 * Fixed a little bug in retr and top commands to not display the marked messages
 * Fixed the number of octets change when the message gets marked for deletion
 * Added a method to unlock the user, this is used by Networking class for the timeout
 * 
 * Pop3ServerThread
 * ============================================================
 * Added a call for a method when timeout exception is caught that will unlock the user 
 * that has timed out
 * 
 * Pop3Server
 * ============================================================
 * Added a call for method to close the database connection if the loop breaks
 * 
 * Testing
 * =============================================================
 * I have tested the server connecting it to the Thunderbird and creating a log using
 * Eclipse by printing input/output. The log is included in .txt file.
 * I have tested that when multiple users are connected there is only one database connection
 * made by connecting to mysql and running "show processlist;" command that shows current connections
 * I have tested all the output for correct/incorrect input using telnet
 */
public class DatabaseBackend implements DatabaseInterface {
	private static final String ERROR_QUERY = "Error executing query";
	private static final String ERROR_CONNECTION = "Could not establish database connection";
	private static final String ERROR_CONNECTION_CLOSE = "Could not close database connection";
	private static final boolean LOCK = true;
	private static final String LOCKED = "1";
	private static final String UNLOCKED = "0";
	private Connection conn;
	private ResultSet rs;
	private static DatabaseBackend dbsObject;

	private DatabaseBackend() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager
					.getConnection("jdbc:mysql://localhost/"
							+ "user=uname&password=psw");
		} catch (SQLException | InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			System.err.println(ERROR_CONNECTION);
		}
		unlockUsers();
	}

	public static synchronized DatabaseBackend getSingletonObject() {
		if (dbsObject == null) {
			dbsObject = new DatabaseBackend();
		}
		return dbsObject;
	}

	@Override
	public boolean existsUser(String user) {
		String query = "SELECT vchUsername FROM m_Maildrop WHERE vchUsername = ?";

		try {
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, user);
			rs = st.executeQuery();
			if (rs.next()) {
				if (rs.getString(1).equals(user)) {
					return true;
				}
			}
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}
		return false;
	}

	@Override
	public boolean checkPassword(String user, String pass) {
		String query = "SELECT vchPassword, tiLocked FROM m_Maildrop WHERE vchUsername = ?";
		if (user == "")
			return false;

		try {
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, user);
			rs = st.executeQuery();
			if (rs.next()) {
				if (rs.getString("vchPassword").equals(pass)
						&& rs.getString("tiLocked").equals(UNLOCKED)) {
					lockUser(LOCK, user);
					return true;
				}
			}
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}
		return false;
	}

	@Override
	public int[] checkMessages(String user) {
		int messages = 0;
		String query = "SELECT COUNT(*) AS msg FROM m_Mail WHERE iMaildropID = ?";
		String getMaildropID = "SELECT iMaildropID FROM m_Maildrop WHERE vchUsername = ?";
		PreparedStatement st;
		try {
			st = conn.prepareStatement(getMaildropID);
			st.setString(1, user);
			rs = st.executeQuery();
			st = conn.prepareStatement(query);
			if (rs.next())
				st.setString(1, rs.getString("iMaildropID"));
			rs = st.executeQuery();
			if (rs.next())
				messages = Integer.parseInt(rs.getString("msg"));
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}

		int octets = getMailOctets(user);
		int[] both = { messages, octets };
		return both;
	}

	@Override
	public int getMailOctets(String user) {
		byte[] bytes;
		String message = null;
		int octets = 0;
		String query = "SELECT txMailContent FROM m_Mail WHERE iMaildropID = (SELECT iMaildropID FROM m_Maildrop WHERE vchUsername = ?)";
		PreparedStatement st;
		try {
			st = conn.prepareStatement(query);
			st.setString(1, user);
			rs = st.executeQuery();
			while (rs.next()) {
				message = rs.getString(1);
				bytes = message.getBytes();
				octets += bytes.length;
			}
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}

		return octets;
	}

	@Override
	public int getMessageOctets(int msg, String user) {
		byte[] bytes;
		String message = null;
		int octets = 0;
		int count = 0;
		String query = "SELECT txMailContent FROM m_Mail WHERE iMaildropID = (SELECT iMaildropID FROM m_Maildrop WHERE vchUsername = ?)";
		PreparedStatement st;
		try {
			st = conn.prepareStatement(query);
			st.setString(1, user);
			rs = st.executeQuery();

			for (count = 1; count <= msg; count++) {
				rs.next();
				message = rs.getString(1);
				bytes = message.getBytes();
				octets = bytes.length;
			}
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}
		return octets;
	}

	@Override
	public String getMessage(int msg, String user) {
		int count = 0;
		String message = null;
		String query = "SELECT txMailContent FROM m_Mail WHERE iMaildropID = (SELECT iMaildropID FROM m_Maildrop WHERE vchUsername = ?)";
		PreparedStatement st;
		try {
			st = conn.prepareStatement(query);
			st.setString(1, user);
			rs = st.executeQuery();

			for (count = 1; count <= msg; count++) {
				rs.next();
				message = rs.getString(1);
			}
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}
		return message;
	}

	@Override
	public String getMessageLines(int msg, int lines, String user) {
		String message = getMessage(msg, user);
		String[] headerBody = message.split("\n\n", 2);
		String[] sLines = headerBody[1].split("\n");
		String returnMsg = "";
		int count = 0;
		returnMsg += headerBody[0];
		for (count = 0; count < lines; count++) {
			if (count == sLines.length)
				break;
			returnMsg += "\n" + sLines[count];
		}
		return returnMsg;
	}

	@Override
	public void deleteMessages(ArrayList<Integer> msgs, String user) {
		String getMailID = "SELECT iMailID FROM m_Mail WHERE iMaildropID = (SELECT iMaildropID FROM m_Maildrop WHERE vchUsername = ?)";
		String query = "DELETE FROM m_Mail WHERE iMailID = ?";
		PreparedStatement st;
		String mailID = null;
		int count = 0;
		for (int msg : msgs) {
			try {
				st = conn.prepareStatement(getMailID);
				st.setString(1, user);
				rs = st.executeQuery();

				for (count = 1; count <= msg; count++) {
					rs.next();
					mailID = rs.getString(1);
				}

				st = conn.prepareStatement(query);
				st.setString(1, mailID);
				st.executeUpdate();
				st.close();
			} catch (SQLException e) {
				System.err.println(ERROR_QUERY);
			}
		}

	}

	@Override
	public String getUIDL(int msg, String user) {
		int count = 0;
		String UIDL = null;
		String query = "SELECT vchUIDL FROM m_Mail WHERE iMaildropID = (SELECT iMaildropID FROM m_Maildrop WHERE vchUsername = ?)";
		PreparedStatement st;
		try {
			st = conn.prepareStatement(query);
			st.setString(1, user);
			rs = st.executeQuery();

			for (count = 1; count <= msg; count++) {
				rs.next();
				UIDL = rs.getString(1);
			}
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}
		return UIDL;
	}

	@Override
	public void lockUser(boolean lock, String user) {
		String query = "UPDATE m_Maildrop SET tiLocked = ? WHERE vchUsername = ?";
		PreparedStatement st;
		try {
			st = conn.prepareStatement(query);
			if (lock) {
				st.setString(1, LOCKED);
			} else {
				st.setString(1, UNLOCKED);
			}
			st.setString(2, user);
			st.executeUpdate();
			st.close();
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}
	}

	@Override
	public void closeConnection() {
		try {
			conn.close();
		} catch (SQLException e) {
			System.err.println(ERROR_CONNECTION_CLOSE);
		}
	}

	/*
	 * Unlocks all the users on database instance creation in case server has
	 * been killed and some accounts are locked
	 */
	private void unlockUsers() {
		String query = "UPDATE m_Maildrop SET tiLocked = ?";
		PreparedStatement st;
		try {
			st = conn.prepareStatement(query);
			st.setString(1, UNLOCKED);
			st.executeUpdate();
			st.close();
		} catch (SQLException e) {
			System.err.println(ERROR_QUERY);
		}
	}
}

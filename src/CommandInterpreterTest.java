import static org.junit.Assert.*;

import org.junit.Test;
/*
 * Test will have failures due to that they were based on Mock Database
 */

public class CommandInterpreterTest {
	CommandInterpreter c = new CommandInterpreter();
	
	@Test
	public void acceptUser() {
		String var = c.handleInput("USER test\r\n");
		assertEquals(var, "+OK anything you want to say, welcome test nice to meet you USER alex");
	}
	
	@Test
	public void declineUser() {
		String var = c.handleInput("USER dave\r\n");
		assertEquals(var, "-ERR Anything you want to say, Dave is not real x y z USER dave");
	}
	
	@Test
	public void declinePassword() {
		String var = c.handleInput("USER test");
		assertEquals(var, "+OK anything you want to say, welcome test nice to meet you USER alex");
		
		var = c.handleInput("PASS incorrect\r\n");
		assertEquals(var, "-ERR invalid password");
	}
	
	@Test
	public void acceptPassword() {
		acceptUser();
		String var = c.handleInput("PASS password");
		assertEquals(var, "+OK mail is unclocked");
	}
	
	@Test
	public void transactionState() {
		acceptPassword();
		assertEquals(c.getState(), "TRANSACTION");
	}
	
	@Test
	public void authorizationQuit() {
		String var = c.handleInput("QUIT\r\n");
		assertEquals(var, "+OK POP3 server signing off");
	}
	
	@Test
	public void statCommand() {
		acceptPassword();
		assertEquals(c.handleInput("STAT\r\n"), "+OK 0 0");
	}
	
	@Test
	public void listCommand() {
		acceptPassword();
		assertEquals(c.handleInput("LIST\r\n"), "+OK 0 messages (0 octets)\r\n.");
	}
	
	@Test
	public void errListCommand() {
		acceptPassword();
		assertEquals(c.handleInput("LIST 2\r\n"), "-ERR no such message, only 0 messages in maildrop");
	}
	
	@Test
	public void errRetrCommand() {
		acceptPassword();
		assertEquals(c.handleInput("RETR 2\r\n"), "-ERR no such message");
	}
	
	@Test
	public void errDeleCommand() {
		acceptPassword();
		assertEquals(c.handleInput("DELE 2\r\n"), "-ERR no such message");
	}
	
	@Test
	public void noopCommand() {
		acceptPassword();
		assertEquals(c.handleInput("NOOP\r\n"), "+OK");
	}
	
	@Test
	public void rsetCommand() {
		acceptPassword();
		assertEquals(c.handleInput("RSET\r\n"), "+OK 0 messages (0 octets)");
	}
	
	@Test
	public void topCommand() {
		acceptPassword();
		assertEquals(c.handleInput("TOP 1 2\r\n"), "-ERR no such message");
	}
	
	@Test
	public void uidlCommand() {
		acceptPassword();
		assertEquals(c.handleInput("UIDL\r\n"), "+OK\r\n");
	}
	
	@Test
	public void errUidlCommand() {
		acceptPassword();
		assertEquals(c.handleInput("UIDL 2\r\n"), "-ERR no such message");
	}
	
	@Test
	public void updateState() {
		acceptPassword();
		c.handleInput("QUIT\r\n");
		assertEquals(c.getState(), "UPDATE");
	}
	
	@Test
	public void errNoArgPassed() {
		String var = c.handleInput("USER");
		assertEquals(var, "-ERR USER");
		
		var = c.handleInput("PASS");
		assertEquals(var, "-ERR PASS");
		
		acceptPassword();
		var = c.handleInput("TOP");
		assertEquals(var, "-ERR TOP");
	}
	
	@Test
	public void errArgPassed() {
		String var = c.handleInput("QUIT abc");
		assertEquals(var, "-ERR QUIT abc");
		
		acceptPassword();
		var = c.handleInput("STAT abc");
		assertEquals(var, "-ERR STAT abc");
		
		var = c.handleInput("RSET abc");
		assertEquals(var, "-ERR RSET abc");
		
		var = c.handleInput("NOOP abc");
		assertEquals(var, "-ERR NOOP abc");
		
		var = c.handleInput("UIDL abc");
		assertEquals(var, "-ERR UIDL abc");
		
		var = c.handleInput("TOP abc");
		assertEquals(var, "-ERR TOP abc");
		
		var = c.handleInput("TOP 1 a");
		assertEquals(var, "-ERR TOP 1 a");
		
		var = c.handleInput("TOP a 2");
		assertEquals(var, "-ERR TOP a 2");
		
		var = c.handleInput("LIST abc");
		assertEquals(var, "-ERR LIST abc");
		
		var = c.handleInput("RETR abc");
		assertEquals(var, "-ERR RETR abc");
		
		var = c.handleInput("DELE abc");
		assertEquals(var, "-ERR DELE abc");
		
		var = c.handleInput("RETR ");
		assertEquals(var, "-ERR RETR ");
		
		var = c.handleInput("STAT ");
		assertEquals(var, "-ERR STAT ");
		
		var = c.handleInput("DELE ");
		assertEquals(var, "-ERR DELE ");
		
		var = c.handleInput("LIST ");
		assertEquals(var, "-ERR LIST ");
		
		var = c.handleInput("UIDL ");
		assertEquals(var, "-ERR UIDL ");
		
		var = c.handleInput("TOP  ");
		assertEquals(var, "-ERR TOP  ");
	}
	
	@Test
	public void negNumArgs() {
		acceptPassword();
		String var = c.handleInput("LIST -1");
		assertEquals(var, "-ERR LIST -1");
		
		var = c.handleInput("RETR -1");
		assertEquals(var, "-ERR RETR -1");
		
		var = c.handleInput("DELE -1");
		assertEquals(var, "-ERR DELE -1");
		
		var = c.handleInput("UIDL -1");
		assertEquals(var, "-ERR UIDL -1");
		
		var = c.handleInput("TOP a -2");
		assertEquals(var, "-ERR TOP a -2");
	}
}

/**
 * 
 */
package cmet.ac.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Graham
 *
 */
public class Server implements Runnable {

	// reference variable for server socket. 
	private ServerSocket 			serverSocket;

	// boolean flag to indicate the server stop. 
	private volatile boolean 		stopServer;

	// reference variabale for the Thread
	private Thread 					serverListenerThread;

	// variable to store server's port number
	int port;
	
	
	// ArrayList to hold all connected client details. This can also be replaced by a ThreadGroup.
	ArrayList<ClientManager> clientList;
	
	
	private static final int SO_TIMEOUT    = 5000; //5 seconds

	
	//	// reference variable for ThreadGroup when handling multiple clients
	//	private ThreadGroup 			clientThreadGroup;	
	
	
	/**
	 * Constructor.
	 * 
	 */
	public Server() {
		
		this.stopServer = false;

		/**
		 * Initializes the ThreadGroup. 
		 * Use of a ThreadGroup is easier when handling multiple clients, although it is not a must. 
		 */
		
		this.clientList = new ArrayList<ClientManager>();
//		this.clientThreadGroup = new ThreadGroup("ClientManager threads");

	}
	
	
	/**
	 * handles messages from each client. 
	 * Modified to prepare a response and send back to the same client. If shared among multiple clients, make it synchronized.
	 * 
	 * 
	 * @param msg
	 * @param client
	 */
	public synchronized void handleMessagesFromClient(String msg, ClientManager client) {
		
		// creating Date object 
		Date date = new Date(); 
		
		// write on output stream based on the 
		// answer from the client 
		String response;
		DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd"); 
		DateFormat fortime = new SimpleDateFormat("hh:mm:ss"); 

		// Generate response based on the client request.
		switch (msg) { 
			case "Date" : 
				response = fordate.format(date); 
				break; 				
			case "Time" : 
				response = fortime.format(date); 
				break; 			
			default: 
				response = new String("Invalid client request...");
				break; 
		} 
        
        //prepare a response for the client. 
		response = "[server says]: " + response;	
		
		client.sendMessageToClient(response);
		
	}
	
	
	/**
	 * Initializes the server. Takes port number, creates a new serversocket instance. 
	 * Starts the server's listening thread. 
	 * @param port
	 * @throws IOException
	 */
	public void initializeServer(int port) throws IOException {

		this.port = port;
		if (serverSocket == null) {
			serverSocket = new ServerSocket(port);
			//serverSocket.setSoTimeout(SO_TIMEOUT);
		}

		stopServer = false;
		serverListenerThread = new Thread(this);
		serverListenerThread.start();

	}
	
	/**
	 * Represents the thread that listens to the port, and creates client connections. 
	 * Here, each client connection is treated as a separate thread. 
	 * 
	 */
	@Override
	public void run() {
		
		// increments when a client connects. 
		int clientCount = 0;

		// loops until stopserver flag is set to true. 
		while (!this.stopServer) {

			System.out.println("[server: ] starting server: listening @ port: " + port);
			
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();				
			} 
			catch(NullPointerException | IOException ex) {
				System.err.println("[server: ] Error when handling client connections on port " + port);
				if(clientSocket != null) {
					try {
						clientSocket.close();
					} catch (IOException e) {
						System.err.println("[server: ] closing server's client socket. ");
					}				
				}
				clientSocket = null; 
				System.err.println("[server: ] closing server..");
				break;
			}

			ClientManager cm = new ClientManager(clientSocket, clientCount, this);
			this.clientList.add(cm);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("[server: ] server listner thread interruped..");
			}

			clientCount++;

		}		
	}
	
	/**
	 * Can perform any pre-processing or checking of the user input before sending it to server. 
	 * In this case, the same message is sent to all the clients..
	 * 
	 * @param userResponse
	 */
	public void handleUserInput(String userResponse) {

		if (!this.stopServer) {
			for (ClientManager client : clientList) {
				client.sendMessageToClient(userResponse);
			}				
		}
	}
	
	/**
	 * Handle the console user input in the server. 
	 * i.e., commands to be send to the client, stop command to the server.
	 */
	public void runServerConsole() {
		try {
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			String message = null;

			while (true) {
				message = fromConsole.readLine();
				handleUserInput(message);
				if(message.equals("over")) {
					this.stopServer = true;
					break;
				}
					
			}
			
			System.out.println("[server: ] stopping server...");
			fromConsole.close();
			
			closeAll();
			
		} catch (Exception ex) {
			System.out.println("[server: ] unexpected error while reading from console!");
		}
	}
	
	/**
	 * Class the server socket and stop the server component.
	 */
	public void closeAll() {
	
		if (this.serverSocket == null)
			return;

		try {

			// How to handle the exception thrown from the accept() method -?
			this.serverSocket.close();
			// You can also use ThreadGroups - to achieve this. 
			for (ClientManager client : clientList) {
				client.closeClientConnection();
			}

		} catch (IOException e) {
			System.err.println("[server: ] Error in closing server connection...");
		} finally {

			this.serverSocket = null;			
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		Server server = new Server();
		
		if(args.length != 1) {
			System.err.println("[server: ] Error in the command line arguments. Usage ./DateTimeServer port");
			return;
		}
		
		// port number to listen
		int port;
		try {
			port = Integer.parseInt(args[0]);
			
			if(port < 1024) {
				System.err.println("[server: ] Port number must be an integer > 1024...");
				return;
			}
			
		}
		catch(NumberFormatException e) {
			System.err.println("[server: ] Port number must be an integer...");
			return;
		}
		

		try {
			server.initializeServer(port);

		} catch (IOException e) {
			System.err.println("[server: ] Error in initializing the server on port " + port);
		}
		
		
		
		
		
		System.out.println("main thread continues..");
		
		
		
		
		
		// Main thread continues...
		server.runServerConsole();
		
		
	}
	
	

}

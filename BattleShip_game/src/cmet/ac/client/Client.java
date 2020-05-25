/**
 * 
 */
package cmet.ac.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * @author Graham
 *
 */
public class Client implements Runnable {

	// reference variable for client socket
	private Socket 					clientSocket;

	// reference variable to store object IO streams, 
	// should be used when working with serialized objects.
	private ObjectOutputStream 		output;
	private ObjectInputStream 		input;

	// boolean variable to store stopclient flag.
	private boolean 				stopClient;

	// reference variable for Thread
	private Thread 					clientReader;

	// variables to store Host IP and port number
	private String 					host;
	private int 					port;
	
	
	/**
	 * Constructor, initiates a client, and calls for openConnection.
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public Client(String host, int port) throws IOException {
		this.host = host;
		this.port = port;
		openConnection();
	}
	
	/**
	 * opens a connection to the server
	 * setup Object IO streams for the socket.
	 * 
	 * @throws IOException
	 */
	public void openConnection() throws IOException {

		// Create the sockets and the data streams
		try {

			this.clientSocket = new Socket(this.host, this.port);
			this.output = new ObjectOutputStream(this.clientSocket.getOutputStream());
			this.input = new ObjectInputStream(this.clientSocket.getInputStream());

		} catch (IOException ex) {
			try {
				closeAll();
			} catch (Exception exc) {
				System.err.println("[client: ] error in opening a connection to: " + this.host + " on port: " + this.port);
			}

			throw ex; // Rethrow the exception.
		}
		
		// creates a Thread instance and starts the thread.
		this.clientReader = new Thread(this);
		this.stopClient = false;
		this.clientReader.start();

	}
	
	/**
	 * Handles sending a message to server. In this case, it is a String. 
	 * @param msg
	 * @throws IOException
	 */
	public void sendMessageToServer(String msg) throws IOException {
		if (this.clientSocket == null || this.output == null)
			throw new SocketException("socket does not exist");

		this.output.writeObject(msg);
	}

	/**
	 * Handle message from the server. In this case, simply display them. 
	 * @param msg
	 */
	public void handleMessageFromServer(String msg) {
		display(msg);

	}
	
	/**
	 * Simply display a String message in the terminal. 
	 * @param message
	 */
	public void display(String message) {
		System.out.println("> " + message);
	}
	
	/**
	 * handles user inputs from the terminal. 
	 * This should run as a separate thread. In this case, main thread. 
	 * 
	 */
	public void runClientConsole() {
		try {
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			String message = null;
			
			while (true) {
				message = fromConsole.readLine();
				handleUserInput(message);
				if(message.equals("over")) {
					this.stopClient = true;
					break;
				}					
			}
			
			System.out.println("[client: ] stopping client...");
			fromConsole.close();			
			closeAll();
			
		} catch (Exception ex) {
			System.out.println("[client: ] unexpected error while reading from console!");
		}

	}

	/**
	 * Can perform any pre-processing or checking of the user input before sending it to server. 
	 * 
	 * @param userResponse
	 */
	public void handleUserInput(String userResponse) {

		if (!this.stopClient) {
			try {
				sendMessageToServer(userResponse);
			} catch (IOException e) {
				System.err.println("[client: ] error when sending message to server: " + e.toString());

				try {
					closeAll();
				} catch (IOException ex) {
					System.err.println("[client: ] error closing the client connections: " + ex.toString());
				}
			}
		}
	}
	
	/**
	 * Close all connections
	 * @throws IOException
	 */
	private void closeAll() throws IOException {
		try {
			// Close the socket
			if (this.clientSocket != null)
				this.clientSocket.close();

			// Close the output stream
			if (this.output != null)
				this.output.close();

			// Close the input stream
			if (this.input != null)
				this.input.close();
			
		} finally {
			// Set the streams and the sockets to NULL no matter what.
			this.output = null;
			this.input = null;
			this.clientSocket = null;
		}
	}
	
	/**
	 * The thread that communicates with the server. 
	 * receives a message from the server, passes it to handleMessageFromServer(). 
	 * 
	 */
	@Override
	public void run() {

		String msg;

		// Loop waiting for data
		try {
			while (!this.stopClient) {
				// Get data from Server and send it to the handler
				// The thread waits indefinitely at the following
				// statement until something is received from the server
				msg = (String) input.readObject();

				// Concrete subclasses do what they want with the
				// msg by implementing the following method
				handleMessageFromServer(msg);
			}
			
			System.out.println("[client: ] client stopped..");
		} catch (Exception exception) {
			if (!this.stopClient) {
				try {
					closeAll();
				} catch (Exception ex) {
					System.err.println("[client: ] error in closing the client connection...");
				}
			}
		} finally {
			clientReader = null;
		}
		
		System.out.println("[client: ] exiting thread...");
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// hardcoded server IP and port number. 
		String ip = "";
		int port;
		
		
		if(args.length != 2) {
			System.err.println("[server: ] Error in the command line arguments. Usage ./DateTimeClient IPAddress port");
			return;
		}
		
		try {
			ip = args[0];
			port = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e) {
			System.err.println("[server: ] Port number must be an integer...");
			return;
		}
		
		

		Client client = null;
		
		// thread to communicate with the server starts here.
		try {
			client = new Client(ip, port);
		} catch (IOException e) {
			System.err.println("[client: ] error in openning the client connection to " + ip + " on port: " + port);
		}

		
		// Main thread continues and in this case used to handle user inputs from the terminal.
		client.runClientConsole();
	}



}

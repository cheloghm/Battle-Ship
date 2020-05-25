/**
 * 
 */
package cmet.ac.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;


/**
 * @author thanuja
 *
 */
public class ClientManager extends Thread {
	
	// reference variable to store client socket
	private Socket 					clientSocket;
	
	// reference for the Sever
	private Server			server;
	
	// boolean flag to indicate whether to stop the connection
	private boolean					stopConnection;
	
	// Input Output streams to communicate with the client using Serialized objects
	private ObjectOutputStream 		out;
	private ObjectInputStream 		in;
	
	// store an incrementing ID for the client. 
	private int 					clientID;
	
	
	/**
	 * Constructor to be called
	 * Initializes the parameters, and input/output streams for the socket. 
	 * Finally, start the thread to manage the client connection
	 * 
	 * @param socket
	 * @param clientID
	 * @param server
	 */
	public ClientManager(Socket socket, int clientID, Server server) {
		super((Runnable) null, new String("Client-"+clientID));
		
		this.clientSocket = socket;
		this.server = server;
		this.stopConnection = false;
		this.clientID = clientID;
		
		System.out.println("[ClientManager: ] new client request received, port " 
				+ socket.getPort());
		try {
			this.out = new ObjectOutputStream(this.clientSocket.getOutputStream());
			this.in = new ObjectInputStream(this.clientSocket.getInputStream());			
		}
		catch(IOException e) {
			System.err.println("[ClientManager: ] error when establishing IO streams on client socket.");
			try {
				closeClientConnection();
			} catch (IOException e1) {
				System.err.println("[ClientManager: ] error when closing connections..." + e1.toString());

			}
		}		
		start();	
	}
	
	
	
	/**
	 * Receive messages (String) from the client, passes the message to Sever's handleMessagesFromClient() method.
	 * Works in a loop until the boolean flag to stop connection is set to true. 
	 */
	@Override
	public void run() {
		
		// The message from the client
		String msg = "";
		try {
			while (!this.stopConnection) {
				// This block waits until it reads a message from the client
				// and then sends it for handling by the server,
				// thread indefinitely waits at the following
				// statement until something is received from the client
				
				BattleShips BS = new BattleShips();
				
				sendMessageToClient(serverinitRes);
				
				msg = (String)this.in.readObject();
				if(msg.equals("over")) {
					this.stopConnection = true;		
					break;
				}
				this.server.handleMessagesFromClient(msg, this);
				
				
			}
			
			System.out.println("[ClientManager: ] stopping the client connection ID: " + this.clientID);
		} catch (Exception e) {
			System.err.println("[ClientManager: ] error when reading message from client.." + e.toString());
			/**
			 * If there is an error, while the connection is not stopped, close all. 
			 */
			if (!this.stopConnection) {
				try {
					closeClientConnection();
				} 
				catch (Exception ex) 
				{
					System.err.println("[ClientManager: ] error when closing the connections.." + ex.toString());
				}
			}
		}
		finally {
			if(this.stopConnection) {
				try {
					closeClientConnection();
				} catch (IOException e) {
					System.err.println("[ClientManager: ] error when closing the connections.." + e.toString());
				}				
			}
		}
					
	}
	
	
	/**
	 * Performs the function of sending a message from Server to remote Client#
	 * Uses ObectOutputStream 
	 * 
	 * @param msg
	 * @throws IOException
	 */
	public void sendMessageToClient(String msg){
		try {
			if (this.clientSocket == null || this.out == null)
				throw new SocketException("socket does not exist");
			
			this.out.writeObject(msg);
		}
		catch(IOException eio) {
			System.err.println("[ClientManager: ] Error in sending message to the client..");
			try {
				closeClientConnection();
			} catch (IOException e) {
				System.err.println("[ClientManager: ] Error in closing the connections..");
				e.printStackTrace();
			}
		}
		
	}
	
	
	/**
	 * Closes all connections for the client. 
	 * @throws IOException
	 */
	public void closeClientConnection() throws IOException {
		try {
			// Close the socket
			if (this.clientSocket != null)
				this.clientSocket.close();

			// Close the output stream
			if (this.out != null)
				this.out.close();

			// Close the input stream
			if (this.in != null)
				this.in.close();
		} finally {
			// Set the streams and the sockets to NULL no matter what.

			this.in = null;
			this.out = null;
			this.clientSocket = null;
			
		}
	}
	
	
	
	
	/**
	 * 
	 * @return client ID
	 */
	public int getClientID() {
		return this.clientID;
	}

	
	/**
	 * @return a description of the client, including IP address and host name
	 */
	@Override
	public String toString() {
		return this.clientSocket == null ? null : this.clientSocket.getInetAddress().getHostName() + " ("
				+ this.clientSocket.getInetAddress().getHostAddress() + ")";
	}
	
}

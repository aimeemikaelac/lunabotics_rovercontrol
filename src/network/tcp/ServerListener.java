package network.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
/**
 * 
 * @author Michael Coughlin
 *
 */
public class ServerListener implements Runnable {

	private ServerSocket servSocket;
	private Socket connectedSocket;
	private int port;
	public InputStream receivedInputStream;
	private NetworkInterface parentServer;
	/**
	 * 
	 * @param port
	 * @param address
	 * @param server
	 */
	public ServerListener(int port, String address, NetworkInterface server) {
		this.port=port;
//		this.address=address;
		this.servSocket=null;
		this.connectedSocket=null;
		this.receivedInputStream=null;
		this.parentServer=server;
	}
	
	/**
	 * 
	 */
	@Override
	public void run() {
		createSocketConection();		
	}
	
	/**
	 * 
	 */
	private void createSocketConection() {
		try {
			while(true) {
				servSocket = new ServerSocket(port);
				System.out.println("created socket on port:" + port);
				connectedSocket = servSocket.accept();
				receivedInputStream=connectedSocket.getInputStream();
				readDataFromClient();
				System.out.println("Received data on: "+port);
				closeConnection();
			}
		} catch (IOException e) {
			System.out.println("Could not open ServerSocket on port"+port);
			e.printStackTrace();
		}
		
	}

	/**
	 * 
	 */
	private void closeConnection() {
		try {
			receivedInputStream.close();
			connectedSocket.close();
			servSocket.close();
			receivedInputStream=null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	/**
	 * 
	 */
	private void readDataFromClient() {
		if(receivedInputStream==null) {
			throw new IllegalArgumentException("Input stream from client is null");
		}
		LinkedList<byte[]> localBuffer=new LinkedList<byte[]>();
		try {
			int numBytes=receivedInputStream.available();
			byte[] bytes=new byte[numBytes];
			receivedInputStream.read(bytes);
			localBuffer.add(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(!localBuffer.isEmpty()) {
			byte[] current=localBuffer.pop();
			parentServer.storeByteData(current);
		}
	}
}

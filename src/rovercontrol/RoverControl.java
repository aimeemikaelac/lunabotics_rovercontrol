package rovercontrol;
import graph.Graph;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import network.serial.USBCommunicator;


public class RoverControl {
	private ServerSocket serverSocket;
	private Socket connectedSocket;
	private int port;
	
	private USBCommunicator locomotionCommunicator;
	private USBCommunicator excavationCommunicator;
	private USBCommunicator encoderCommunicator;
	private Graph mapper;
	private final boolean STARTING_AUTO_MANUAL_MODE = false;
	private boolean autoManualMode;
	Thread graphThread;
	
	private ArrayList<byte[]> locomotionBuffer;
	
	public RoverControl(int port) {
		this.port = port;
		locomotionCommunicator = new USBCommunicator("COM10");
		locomotionCommunicator.start();
		//excavationCommunicator = new USBCommunicator("COM5");
		//encoderCommunicator = new USBCommunicator("COM6");
		locomotionBuffer = new ArrayList<byte[]>();
		autoManualMode = STARTING_AUTO_MANUAL_MODE;
		this.mapper = new Graph(2,locomotionCommunicator);
		graphThread = new Thread(mapper);
	}
	/**
	 * 
	 * @param args array of arguments. args[0] is the tcp/ip communication port selected
	 */
	public static void main(String[] args) {
		RoverControl controller =  new RoverControl(Integer.parseInt(args[0]));
		controller.runNetworkListener();
		controller.graphThread.start();
	}
	private void runNetworkListener() {
		while(true) {
			try {
				serverSocket = new ServerSocket(port);
				System.out.println("created port");
				connectedSocket = serverSocket.accept();
				System.out.println("accepted connection");
				InputStream receivedStream = connectedSocket.getInputStream();
				int numBytes = receivedStream.available();
				byte[] bytes = new byte[numBytes];
				if(numBytes > 0){
					receivedStream.read(bytes);
					locomotionCommunicator.writeBytes(bytes);
					System.out.println("wrote bytes");
					int networkCode = bytes[0];
					if(networkCode == 0x24)
					{
						autoManualMode = !autoManualMode;
						if(autoManualMode) {
							mapper.setAutoMode(true);
						}
						else {
							mapper.setAutoMode(false);
						}
					}
				}
				serverSocket.close();
				connectedSocket.close();
			} catch (IOException e) {
//				e.printStackTrace();
				System.out.println("Could not open socket on port: " + port);
			}
		}
	}
}

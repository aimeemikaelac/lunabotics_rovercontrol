package rovercontrol;
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
	
	private ArrayList<byte[]> locomotionBuffer;
	
	public RoverControl(int port) {
		this.port = port;
		locomotionCommunicator = new USBCommunicator("COM4");
		excavationCommunicator = new USBCommunicator("COM5");
		encoderCommunicator = new USBCommunicator("COM6");
		locomotionBuffer = new ArrayList<byte[]>();
	}
	
	public static void main(String[] args) {
		
	}
}

package network.tcp;

import java.util.ArrayList;


public class NetworkRunner{// implements Runnable{
	private NetworkInterface localNetwork;
	private NetworkInterface remoteNetwork;
	public NetworkRunner(String remoteIp, int localPort, int remotePort){
		localNetwork=new NetworkInterface("localhost", localPort, 50, 50);
		remoteNetwork=new NetworkInterface(remoteIp, remotePort, 50, 50);
		Thread localNetworkThread=new Thread(localNetwork);
		localNetworkThread.start();
		Thread remoteNetworkThread=new Thread(remoteNetwork);
		remoteNetworkThread.start();
	}
	
	
	public boolean writeData(byte[] data) {
		remoteNetwork.sendData(data);
		return true;
	}

/*
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}*/
	
	public static void main(String[] args){
		//args[0]==remoteIp, args[1]==localPort, args[2]==remotePort
		String remoteIp=args[0];
		int localPort=Integer.parseInt(args[1]);
		int remotePort=Integer.parseInt(args[2]);
		NetworkRunner runner=new NetworkRunner(remoteIp, localPort, remotePort);
		ArrayList<byte[]> dummyInBuffer = new ArrayList<byte[]>();
		ArrayList<byte[]> dummyOutBuffer = new ArrayList<byte[]>();
		runner.runNetwork(dummyInBuffer, dummyOutBuffer, runner);
		
	}


	public void runNetwork(ArrayList<byte[]> outputBuffer, ArrayList<byte[]> inputBuffer, Object bufferOwner) {
		while(true) {
			if(localNetwork.dataAvailableInReceivedBuffer()) {
				byte[] data=localNetwork.removeMostRecentData();
				if(data.length>0) {
					synchronized(bufferOwner) {
						bufferOwner.notify();
						inputBuffer.add(data);
					}
					System.out.println("received in runner");
					remoteNetwork.sendData(data);
				}
			}
			if(remoteNetwork.dataAvailableInReceivedBuffer()) {
				byte[] data=remoteNetwork.removeMostRecentData();
				if(data.length>0) {
					synchronized(bufferOwner) {
						bufferOwner.notify();
						outputBuffer.add(data);
					}
					localNetwork.sendData(data);
					System.out.println("sending data remote in runners");
				}
			}
			try {
				synchronized(this) {
					wait(50);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

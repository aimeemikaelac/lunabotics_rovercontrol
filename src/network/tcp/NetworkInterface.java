package network.tcp;

import java.util.LinkedList;
import java.util.Queue;
/**
 * 
 * @author Michael Coughlin
 *
 */
public class NetworkInterface implements Runnable {
	private Queue<byte[]> receivedDataBuffer;
	private Queue<byte[]> sendingDataBuffer;
	private ServerListener reader;
	private ClientWriter writer;
		
	/**
	 * 
	 * @param address
	 * @param port
	 */
	public NetworkInterface(String address, int port, long clientWaitDelay, long interfaceDelay) {
		this.receivedDataBuffer = new LinkedList<byte[]>();
		this.sendingDataBuffer = new LinkedList<byte[]>();
		writer=new ClientWriter(port, address, clientWaitDelay);
		reader=new ServerListener(port, address, this);
//		createSocketConection();
	}
	
	public synchronized boolean dataAvailableInReceivedBuffer() {
		return !receivedDataBuffer.isEmpty();
	}
	/**
	 * 
	 * @param message
	 * @return
	 */
	private synchronized void writeBufferToClient() {
		while(!sendingDataBuffer.isEmpty()) {
			byte[] current=sendingDataBuffer.poll();
			writer.addBytesToQueue(current);
//			System.out.println("adding to buffer");
		}
	}
	
	public synchronized void sendData(byte[] message){
		notify();
		sendingDataBuffer.add(message);
	}
	
	
	/**
	 * 
	 * @param data
	 */
	public synchronized void storeByteData(byte[] data) {
		notify();
		receivedDataBuffer.add(data);		
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized byte[] getMostRecentData() {
		notify();
		return receivedDataBuffer.peek();
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized byte[] removeMostRecentData() {
		notify();
		return receivedDataBuffer.poll();
	}
	
	/**
	 * 
	 */
	@Override
	public void run() {
		Thread clientThread = new Thread(writer);
		clientThread.start();
		Thread serverThread = new Thread(reader);
		serverThread.start();
		while(true) {
			boolean isEmpty=false;
			synchronized(this) {
				isEmpty=sendingDataBuffer.isEmpty();
			}
			if(isEmpty) {
				try {
//					wait(delay);
					synchronized(this){
						wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				writeBufferToClient();
//				System.out.println("sendingdatatoclient");
			}
		}
		
	}
	
}
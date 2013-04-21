package network.tcp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

public class ClientWriter implements Runnable{
	private int port;
	private String address;
	private boolean transmit;
	private boolean stop;
	private Queue<byte[]> messageQueue;
	
	public ClientWriter(int port, String address, long delay) {
		this.port=port;
		this.address=address;
		messageQueue=new LinkedList<byte[]>();
		transmit=true;
		stop=false;
	}

	@Override
	public void run() {
		boolean needsStop=false;
		boolean transmittable=true;
		while(true) {
			
			synchronized(this) {
				transmittable=transmit;
				needsStop=stop;
			}
			if(transmittable) {
				transmitBufferContents();
			}
			else if(needsStop) {
				return;
			}
			else {
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
		}
	}
	
	public synchronized void addBytesToQueue(byte[] message) {
		notify();
		messageQueue.add(message);
		System.out.println("received data in client to send to port: "+port);
		transmit=true;
	}
	
	public synchronized void stopThread() {
		stop=true;
	}
	
	private synchronized void transmitBufferContents() {
		if(messageQueue.isEmpty()) {
			transmit=false;
			return;
		}
		try {
			System.out.println("am transmitting");
			Socket soc = new Socket(address, port);
			OutputStream writer = soc.getOutputStream();
			int length=0;
			while(!messageQueue.isEmpty()) {
				byte[] current = messageQueue.poll();
				writer.write(current);
				length=current.length;
			}
			System.out.println("wrote "+length+" bytes to: "+address+":"+port);
			writer.flush();
			writer.close();
			soc.close();
			transmit=false;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Position extends Thread {

	public Graph graph;
	
	public Position(Graph graph)
	{
		this.graph = graph;
	}
	
	public void run(){
		try{
			while(true)
			{
				loadPositionFromFile("Position.txt");
			}
		} catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public boolean loadPositionFromFile(String filename)
	{
		File f = new File(filename);
		if(f.exists())
		{
			Scanner scanner = null;
			try {

				try {
					synchronized(this)
					{
						wait(100);
					}
				}catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				scanner = new Scanner(f);
				double angle = Math.toDegrees(scanner.nextDouble());
				//System.out.println(angle);
				int x = scanner.nextInt();
				//System.out.println(x/ARENA_WIDTH*66);
				int y = scanner.nextInt();
				System.out.println(y/graph.ARENA_LENGTH*126);
				//TODO: Double check the robot Node status is correct.
				//TODO: Get rid of the hard coded WIDTH and LENGTH Parameters.
				synchronized(graph){
					graph.notify();
					graph.setRobotNode(new Node(4, (int)(x/graph.ARENA_WIDTH*66), (int)(y/graph.ARENA_LENGTH*126), angle));
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			scanner.close();
			try{
				synchronized(this){
					wait(500);
				}
			}catch(Exception ex)
			{
				ex.printStackTrace();
			}
			f.delete();
			return true;
		}
		return false;
	}
}

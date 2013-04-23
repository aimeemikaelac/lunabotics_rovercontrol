package graph;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

import network.serial.USBCommunicator;



public class Graph {
	private ArrayList<ArrayList<Node>> map;
	private ArrayList<Node> path;
	private ArrayList<Segment> vectorizedPath;
	private int goalValue;
	private int rootValue;
	private int blockedValue;
	private Node goalNode;
	private Node rootNode;
	private boolean autoMode;
	private Node robotNode;
	private USBCommunicator locomotionController;
	
	public Graph(int rootSymbol, int goalSymbol, USBCommunicator locomotionController)
	{
		map = new ArrayList<ArrayList<Node>>();
		path = new ArrayList<Node>();
		vectorizedPath = new ArrayList<Segment>();
		goalValue = goalSymbol;
		rootValue = rootSymbol;
		blockedValue = 1;
		goalNode = null;
		rootNode = null;
		autoMode = false;
		this.locomotionController = locomotionController; 
	}
	public synchronized void setAutoMode(boolean newMode) {
		autoMode = newMode;
	}
	
	public void Run()
	{
		boolean inAutonomyMode =false;
		synchronized(this) {
			inAutonomyMode = autoMode;
		}
		Thread receivedData = new Thread() 
		{
			public void run() 
			{
				while(true) 
				{
					if(loadPositionFromFile("positionData.txt")) 
					{
						sendPathCorrection();
					}
					else 
					{
						try 
						{
							synchronized(this) 
							{
								wait(20);
							}
						} 
						catch (InterruptedException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		};
		Thread receivedMapData = new Thread() {
			public void run()
			{
				while(true)
				{
					if(loadMapFromFile("ThomasMapHere.txt"))
					{
						planPath();
						backTrace();
						vectorizePath();
					}
					else
					{
						try
						{
							synchronized(this)
							{
								wait(1000);
							}
						}
						catch (InterruptedException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		};

		receivedData.start();
	}
	
	public void sendPathCorrection()
	{
		
	}
	
	public boolean loadPositionFromFile(String filename)
	{
		File f = new File(filename);
		if(f.exists())
		{
			Scanner scanner = null;
			try {
				scanner = new Scanner(f);
				int x = scanner.nextInt();
				int y = scanner.nextInt();
				double angle = scanner.nextDouble();
				robotNode = new Node(0, x, y);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			scanner.close();
			f.delete();
			return true;
		}
		return false;
	}
	
	public boolean loadMapFromFile(String filename)
	{
		File f = new File(filename);
		if(f.exists())
		{
			Scanner scanner = null;
			try {
				scanner = new Scanner(f);
				int x = scanner.nextInt();
				int y = scanner.nextInt();
				int rowsMade = 0;
				int colsMade = 0;
				while(scanner.hasNextInt())
				{
					if(rowsMade < y && colsMade == 0)
					{
						map.add(new ArrayList<Node>());
						rowsMade++;
					}
					int value = scanner.nextInt();
					map.get(rowsMade-1).add(new Node(value, colsMade, rowsMade-1));
					if(value == goalValue && goalNode == null)
					{
						goalNode = map.get(rowsMade-1).get(map.get(rowsMade-1).size()-1);
					}
					if(value == rootValue && rootNode == null)
					{
						rootNode = map.get(rowsMade-1).get(map.get(rowsMade-1).size()-1);
					}
					colsMade = (colsMade+1)%x;
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			scanner.close();
			f.delete();
			return true;
		}
		return false;
	}
	
	public void printMapToConsole()
	{
		for (ArrayList<Node> row : map)
		{
			for (Node node : row)
			{
				System.out.print(node.status);
			}
			System.out.println();
		}
	}
	
	public Boolean planPath()
	{
		Queue<Node> q = new LinkedList<Node>();
		q.add(rootNode);
		while(!q.isEmpty())
		{
			Node currentNode = q.poll();
			if(! currentNode.visited)
			{
				if(currentNode == goalNode)
				{
					System.out.println("Found a working path.");
					return true;
				}
				Node nextNode = map.get(currentNode.getY()).get(currentNode.getX()+1);
				if(nextNode.status == 0 || nextNode.status == 3)
				{
					if(nextNode.status != 3)
					{
						nextNode.status = 5;
					}
					nextNode.setPrevNode(currentNode);
					q.add(nextNode);
				}
				nextNode = map.get(currentNode.getY()).get(currentNode.getX()-1);
				if(nextNode.status == 0 || nextNode.status == 3)
				{
					if(nextNode.status != 3)
					{
						nextNode.status = 5;
					}
					nextNode.setPrevNode(currentNode);
					q.add(nextNode);
				}
				nextNode = map.get(currentNode.getY()+1).get(currentNode.getX());
				if(nextNode.status == 0 || nextNode.status == 3)
				{
					if(nextNode.status != 3)
					{
						nextNode.status = 5;
					}
					nextNode.setPrevNode(currentNode);
					q.add(nextNode);
				}
				nextNode = map.get(currentNode.getY()-1).get(currentNode.getX());
				if(nextNode.status == 0 || nextNode.status == 3)
				{
					if(nextNode.status != 3)
					{
						nextNode.status = 5;
					}
					nextNode.setPrevNode(currentNode);
					q.add(nextNode);
				}
			}
		}
		return false;
	}
	
	public void backTrace()
	{
		Stack<Node> s = new Stack<Node>();
		Node currentNode = goalNode;
		s.add(currentNode);
		while(currentNode != rootNode)
		{
			currentNode = currentNode.getPrevNode();
			s.add(currentNode);
		}
		while(! s.empty())
		{
			path.add(s.pop());
		}
	}
	
	public void printPath()
	{
		System.out.println("Path:");
		for (Node node : path)
		{
			System.out.println("(" + node.getX() + "," + node.getY() + ")");
		}
	}
	
	public void printVectorizedPath()
	{
		System.out.println("Vectorized Path:");
		for (Segment segment : vectorizedPath)
		{
			System.out.println("(" + (segment.getEndX()-segment.getStartX()) + "," + (segment.getStartY()-segment.getEndY()) + ")");
		}
	}
	
	public void vectorizePath()
	{
		Segment oldSegment = null;
		Segment newSegment = null;
		while(!path.isEmpty())
		{
			
			if(oldSegment == null && path.size() >= 3)
			{
				Node a = path.remove(0);
				Node b = path.get(0);
				Node c = path.get(1);
				
				oldSegment = new Segment(a.getX(), a.getY(), b.getX(), b.getY());
				newSegment = new Segment(a.getX(), a.getY(), c.getX(), c.getY());

				if(vectorIsBlocked(newSegment))
				{
					vectorizedPath.add(oldSegment);
					oldSegment = null;
				}
			}
			else if(oldSegment == null && path.size() < 3)
			{
				Node a = path.remove(0);
				Node b = path.remove(0);
				vectorizedPath.add(new Segment(a.getX(), a.getY(), b.getX(), b.getY()));
			}
			else if(path.get(0).status == goalValue)
			{
				path.remove(0);
				vectorizedPath.add(newSegment);
			}
			else
			{
				Node a = path.get(0);
				Node b = path.get(1);
				oldSegment = newSegment;
				newSegment = new Segment(newSegment.getStartX(), newSegment.getStartY(), b.getX(), b.getY());
				if(vectorIsBlocked(newSegment))
				{
					vectorizedPath.add(oldSegment);
					oldSegment = null;
				}
				else
				{
					path.remove(0);
				}
			}
		}
	}
	
	private Boolean vectorIsBlocked(Segment s)
	{
		ArrayList<Node> blocked = new ArrayList<Node>();
		
		for (ArrayList<Node> list : map)
		{
			for (Node node : list)
			{
				if(node.getX() <= Math.max(s.getEndX(), s.getStartX()) 
						&& node.getX() >= Math.min(s.getEndX(), s.getStartX()))
				{
					if(node.getY() <= Math.max(s.getEndY(), s.getStartY())
							&& node.getY() >= Math.min(s.getEndY(), s.getStartY()))
					{
						if(node.status == blockedValue)
						{
							blocked.add(node);
						}
					}
				}
			}
		}
		
		for (Node node : blocked)
		{
			double distance = Math.abs(errorDistance(s, node.getX(), node.getY()));
			if(distance < 1)
			{
				return true;
			}
		}
		return false;	
	}
	
	public double errorDistance(Segment vector, int pointX, int pointY) {
		double slope = vector.getSlope();
		double yIntercept = -slope*vector.getStartX() - vector.getStartY();
		double distance = Math.abs(slope*pointX + pointY + yIntercept)/Math.sqrt(Math.pow(slope, 2) + 1);
		return distance;
	}
}

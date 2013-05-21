package graph;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;
import graph.State;

import network.serial.USBCommunicator;



public class Graph implements Runnable {
	private ArrayList<ArrayList<Node>> map;
	private ArrayList<Node> path;
	private ArrayList<Segment> vectorizedPath;
	private int rootValue;
	private int goalValue;
	//private int blockedValue;
	private Node goalNode;
	private Node rootNode;
	private boolean autoMode;

	public synchronized void setRobotNode(Node robotNode) {
		this.robotNode = robotNode;
	}

	private Node robotNode;
	private USBCommunicator locomotionController;
	protected graph.State state;
	private double error;
	public final double ERROR_TOLERANCE = 10.0;
	public final double ARENA_WIDTH = 3880.0;
	public final double ARENA_LENGTH = 7880.0;
	public final double EXCAVATION_AREA_BOUNDARY = 4440.0;
	public final double OBSTACLE_AREA_BOUNDARY = 1500.0;
	public final double FORWARD_DIRECTION = 0.0;
	public final double ANGLE_TOLERANCE = 5.0;
	public final double DISTANCE_TOLERANCE = 1.0;
	public final double PATH_PID_WIDTH = 2;
	public final int MAP_HEIGHT = 126;
	public final int MAP_WIDTH = 66;
	public enum CompetitionAreas {
		STARTING, OBSTACLE, EXCAVATION;
	}
	private Position position;
	private int y;
	private int x;
	
	public Graph(int rootSymbol, USBCommunicator locomotionController)
	{
		map = new ArrayList<ArrayList<Node>>();
		/*for(int i = 0; i < 126; i++)
		{
			map.add(new ArrayList<Node>());
			for (int j = 0; j < 66; j++)
			{
				if(i == 0 || j == 0 || j == 65 || i == 123)
				{
					map.get(map.size()-1).add(new Node(2, j, i, 0));
				}
				else if(i == 33 && j == 33)
				{
					map.get(map.size()-1).add(new Node(6, j, i, 0));
				}
				else
				{
					map.get(map.size()-1).add(new Node(1, j, i, 0));
				}
			}
		}
		map.get(34).get(34).setStatus(6);*/
		path = new ArrayList<Node>();
		vectorizedPath = new ArrayList<Segment>();
		rootValue = rootSymbol;
		goalValue = 6;
		//blockedValue = 1;
		goalNode = null;
		rootNode = null;
		autoMode = false;
		this.locomotionController = locomotionController; 
		state = graph.State.START;
		error = 0;
		this.position = new Position(this);
	}
	public synchronized void setAutoMode(boolean newMode) {
		autoMode = newMode;
	}
	
	public void run()
	{
		System.out.println("running graph thread---------------------");
		/*synchronized(this) {
			inAutonomyMode = autoMode;
		}*/
		
		//TODO: Create a Thread to run a check on the autonomous mode change.
		
		//Thread to pull in the data from the position file.
		/*Thread receivedData = new Thread() 
		{
			public void run() 
			{
				while(true) 
				{
					/*try {
						Runtime.getRuntime().exec("acquire_data.txt");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					/*loadPositionFromFile("Position.txt");
				}
			}
		};*/
		
		
		//Ten minute timer for the autonomy mode to stop.
		Thread stopAutonomyAfter10 = new Thread()
		{
			public void run()
			{
				//TODO: Set this back to 600000
				waitFor(30000);
				state = graph.State.END;
			}
		};
		
		stopAutonomyAfter10.start();
		
		while(true) {
			//Switch statement of ultimate power
			//TODO: Make sure all states are defined and do create a loop.
			switch(state) {
				case START:
					//Remove the receivedData.start() command. It is turned on after the robot is oriented.
					position.start();
					//MOVE ROBOT TEST
					byte[] byteArray = {0x24};
					locomotionController.writeBytes(byteArray);
					loadMapFromFile("output.txt", 6);
					printMapToConsole();
					planPath(6);
					backTrace();
					vectorizePath(6);
					moveRobot(40);
					/*synchronized(this){
						rotateRobot(0-robotNode.getAngle(),60);
					}*/
					/*rotateRobot(-45,30);
					vectorizedPath.add(new Segment(robotNode.getX(),33,robotNode.getY(),33));
					moveRobot(30);
					vectorizedPath.remove(0);/*
					/*planPath(6);
					while(vectorizedPath.size() > 0)
					{
						rotateRobot(getNextSegmentAngle() - robotNode.getAngle());
						moveRobot(true,30);
						vectorizedPath.remove(0);
					}*/
					//remove the number.
					//vectorizedPath.add(new Segment(robotNode.getX(),robotNode.getX(),robotNode.getY(),robotNode.getY()-1195/126*2));
					//moveRobot(false,40);
					//vectorizedPath.add(new Segment(robotNode.getX(),robotNode.getX(),robotNode.getY(),robotNode.getY()+(int)(100/ARENA_LENGTH*126)));
					//moveRobot(25);
					//vectorizedPath.remove(0);
					//MOVE ROBOT for 3 seconds test.
					//moveRobot(true, 25.0, 3000);
					//excavate();
					//ROTATE ROBOT TEST:
					//getOriented();
					/*try {
						synchronized(this) {
							wait();
							rotateRobot(0-robotNode.getAngle(),60);
							rotateRobot(0-robotNode.getAngle(),40);
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					//Dumping Test.
					//getOriented();
					//backupAndDump();
					//state = State.END;
					state = State.ORIENTATION;
					break;
				case ORIENTATION:
					getOriented();
					state = State.END;
					//state = State.COLLECT_SNAPSHOTS;
					break;
				case MOVE_TO_CENTER:
					planPath(6);
					backTrace();
					vectorizePath(6);
					moveToCenter();
					synchronized(this){
						rotateRobot(0 - robotNode.getAngle(),30);
					}
					state = State.END;
					//state = State.MOVE_TO_EXCAVATE;
					break;
				case COLLECT_SNAPSHOTS:
					//moveToCenter();
					/*synchronized(this){
						rotateRobot(0- robotNode.getAngle(),30);
					}*/
					collectSnapshots();
					state = State.MOVE_TO_EXCAVATE;
					break;
				case MOVE_TO_EXCAVATE:
					driveToExcavate();
					synchronized(this) {
						rotateRobot(0 - robotNode.getAngle(),30);
					}
					state = State.EXCAVATE;
					break;
				case EXCAVATE:
					excavate();
					break;
				case MOVE_TO_DUMPING:
					moveToCenter();
					synchronized(this)
					{
						rotateRobot(0 - robotNode.getAngle(), 30);
					}
					break;
				case BACKUP_AND_DUMP:
					backupAndDump();
					state = State.MOVE_TO_CENTER;
					break;
				case END:
					System.exit(1);
					break;
			}
		}
	}
	
	private void waitForStartSignal() {
		// TODO Auto-generated method stub
		
	}
	private void collectSnapshots() {
		// TODO: ensure that the program names are correct for what get called.
		System.out.println("Snap!");
		try {
			Runtime.getRuntime().exec("SingleSnapshot.exe");
			waitFor(1000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*while(!loadMapFromFile("Map.txt", goalValue))
		{
			waitFor(300);
		}*/
		waitFor(5000);
		System.out.println("Turning.");
		rotateRobot(47,60);
		//waitFor(5000);
		System.out.println("Snap!");
		try {
			Runtime.getRuntime().exec("SingleSnapshot.exe");
			waitFor(1000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*while(!loadMapFromFile("Map.txt", goalValue))
		{
			waitFor(300);
		}*/
		waitFor(5000);
		rotateRobot(-94,60);
		waitFor(1000);
		System.out.println("Snap!");
		try {
			Runtime.getRuntime().exec("SingleSnapshot.exe");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*while(!loadMapFromFile("Map.txt", goalValue))
		{
			waitFor(300);
		}*/
		System.out.println("Turning...");
		synchronized(this){
			System.out.println(robotNode.getAngle());
		}
		waitFor(5000);
		synchronized(this)
		{
			rotateRobot(0-robotNode.getAngle(),60);
		}
		waitFor(1000);
		loadMapFromFile("output.txt",6);
	}
	private void backupAndDump() {
		//TODO: Ensure that the last parameter is the correct distance
		synchronized(this) {
			//vectorizedPath.add(new Segment(robotNode.getX(), robotNode.getX(), robotNode.getY(), (int)(.889/ARENA_LENGTH*126)));
			vectorizedPath.add(new Segment(robotNode.getX(), robotNode.getX(), robotNode.getY(), (int)(.991/ARENA_LENGTH*126)));
		}
		moveRobot(30);
		startConveyorRobot();
		//vectorizedPath.removeAll(vectorizedPath);
		//Change time to 15 seconds or whatever we deem is correct.
		waitFor(3000);
		stopRobot();
	}
	private void moveToCenter() {
		//Path Plan to the center coordinate [6]
		//planPath(6);
		//backTrace();
		//vectorizePath(6);
		synchronized(this){			
			vectorizedPath.add(new Segment(robotNode.getX(),robotNode.getX(),robotNode.getY(),(int)(1200/ARENA_LENGTH*126)));
			vectorizedPath.add(new Segment(robotNode.getX(),33,robotNode.getY(),(int)(1500/ARENA_LENGTH*126)));
		}
		while(vectorizedPath.size() > 0)
		{
			synchronized(this){
				System.out.println("x " + robotNode.getX());
				System.out.println("y " + robotNode.getY());
			}
			System.out.println("Rotating " + getNextSegmentAngle() + " Degrees.");
			rotateRobot(getNextSegmentAngle(), 60);
			moveRobot(40);
			vectorizedPath.remove(0);
			System.out.println("Size of Path = " + vectorizedPath.size());
			//TODO: Check to make sure that the function behaves how it is expected to.
			//vectorizedPath.removeAll(vectorizedPath);
			//planPath(6);
			//backTrace();
			//vectorizePath(6);
		}
		stopRobot();
	}
	private void lowerLadderRobot()
	{
		byte[] byteArray = {0x41};
		locomotionController.writeBytes(byteArray);
	}
	
	private void raiseLadderRobot()
	{
		byte[] byteArray = {0x42};
		locomotionController.writeBytes(byteArray);
	}
	
	private void startLadderRobot()
	{
		byte[] byteArray = {0x44};
		locomotionController.writeBytes(byteArray);
	}
	
	private void startConveyorRobot()
	{
		byte[] byteArray = {0x48};
		locomotionController.writeBytes(byteArray);
	}
	
	private void waitFor(int milliseconds)
	{
		try {
			synchronized(this) {
				wait(milliseconds);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void excavate() {
		//Movement speed is should be about 16% speed or 40/255 
		final double angle = 10.0;
		synchronized(this) {
			map.get((int)(4440/ARENA_LENGTH*126)).get(robotNode.getX()).status = 1;
		}
		//TODO: Be sure to uncomment the lower ladder function.
		//lowerLadderRobot();
		startLadderRobot();
		// Dead reckoning for a period of time that should not provide too much error. Tweak times to get the values to work out properly.
		moveRobot(true,20,10000);
		//Shift the BP-1 up a little.
		startConveyorRobot();
		waitFor(1000);
		stopRobot();
		startLadderRobot();
		//Move back for a period of time that is less than the original forward period.
		moveRobot(false,20,8000);
		startLadderRobot();
		//Get to the 
		synchronized(this) {
			vectorizedPath.add(new Segment(robotNode.getX(),robotNode.getX(), robotNode.getY(), (int)(4440/ARENA_LENGTH*MAP_HEIGHT)));
		}
		moveRobot(20);
		raiseLadderRobot();
		startLadderRobot();
		waitFor(6000);
		//Shift the BP-1 up a little.
		startConveyorRobot();
		waitFor(1000);
		stopRobot();
	}

	/*private synchronized boolean reachedDestination(CompetitionAreas area) {
		double currentY = robotNode.getY();
		switch(area) {
			case EXCAVATION:
				if(currentY > EXCAVATION_AREA_BOUNDARY) {
					return true;
				}
				return false;
			case OBSTACLE:
				if(currentY < EXCAVATION_AREA_BOUNDARY && currentY > OBSTACLE_AREA_BOUNDARY) {
					return true;
				}
				return false;
			case STARTING:
				if(currentY < OBSTACLE_AREA_BOUNDARY) {
					return true;
				}
				return false;
			default: 
				return false;
		}
	}*/
	
	/*private boolean acceptableError() {
		//TODO: change to correct error tolerance
		if(error < ERROR_TOLERANCE ) {
			return false;
		}
		return false;
	}*/
	
	private void driveToExcavate() {
		//TODO: Test this function...
		planPath(7);
		backTrace();
		vectorizePath(7);
		while(vectorizedPath.size() > 0)
		{
			//TODO put getSegmentAngle inside of Segment class.
			synchronized(this) {
				rotateRobot(robotNode.getAngle() - getNextSegmentAngle(),30);
			}
			moveRobot(30);
			vectorizedPath.removeAll(vectorizedPath);
			planPath(7);
			backTrace();
			vectorizePath(7);
		}
	}
	private synchronized boolean orientedCorrectly() {
		if(robotNode.getAngle() <= FORWARD_DIRECTION + ANGLE_TOLERANCE && robotNode.getAngle() >= FORWARD_DIRECTION - ANGLE_TOLERANCE) {
			return true;
		}
		return false;
	}
	public void sendPathCorrection()
	{
		if(state == State.MOVE_TO_CENTER || state == State.MOVE_TO_EXCAVATE) {
			synchronized(this) {
				error = errorDistance(vectorizedPath.get(0), robotNode.getX(), robotNode.getY());
			}
			byte[] byteArray = new byte[5];
			byteArray[0] = 0x14;
			byteArray[1] = 0x50;
			byteArray[2] = (byte) (127 - error);
			byteArray[3] = 0x50;
			byteArray[4] = (byte) (127 + error);
			locomotionController.writeBytes(byteArray);
		}
		else if(state == State.MOVE_TO_DUMPING || state == State.BACKUP_AND_DUMP)
		{
			synchronized(this){
				error = errorDistance(vectorizedPath.get(0), robotNode.getX(), robotNode.getY());
			}
			byte[] byteArray = new byte[5];
			byteArray[0] = 0x14;
			byteArray[1] = 0x50;
			byteArray[2] = (byte) (127 + error);
			byteArray[3] = 0x50;
			byteArray[4] = (byte) (127 - error);
			locomotionController.writeBytes(byteArray);
		}
	}
	
	/*public boolean loadPositionFromFile(String filename)
	{
		File f = new File(filename);
		if(f.exists())
		{
			Scanner scanner = null;
			try {
				scanner = new Scanner(f);
				double angle = Math.toDegrees(scanner.nextDouble());
				//System.out.println(angle);
				int x = scanner.nextInt();
				//System.out.println(x/ARENA_WIDTH*66);
				int y = scanner.nextInt();
				System.out.println(y/ARENA_LENGTH*126);
				//TODO: Double check the robot Node status is correct.
				//TODO: Get rid of the hard coded WIDTH and LENGTH Parameters.
				robotNode = new Node(4, (int)(x/ARENA_WIDTH*66), (int)(y/ARENA_LENGTH*126), angle);
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
	}*/
	
	public boolean loadMapFromFile(String filename, int goal)
	{
		File f = new File(filename);
		if(f.exists())
		{
			Scanner scanner = null;
			try {
				scanner = new Scanner(f);
				y = scanner.nextInt();
				x = scanner.nextInt();
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
					//TODO: use a real value for angle----------------------------|-------------------------------------------------------------------
					map.get(rowsMade-1).add(new Node(value, colsMade, rowsMade-1, 0));
					//------------------------------------------------------------|-------------------------------------------------------------------
					if(value == goal && goalNode == null)
					{
						System.out.println("Found Goal node.");
						goalNode = map.get(rowsMade-1).get(map.get(rowsMade-1).size()-1);
					}
					if(value == rootValue && rootNode == null)
					{
						rootNode = map.get(rowsMade-1).get(map.get(rowsMade-1).size()-1);
					}
					colsMade = (colsMade+1)%x;
				}
				//Add the center point for the starting area.
//				map.get((int) (y/ARENA_LENGTH*OBSTACLE_AREA_BOUNDARY)).get((int) (x/2)).status = 6;
				for(Node node : map.get((int) (y/ARENA_LENGTH*EXCAVATION_AREA_BOUNDARY)))
				{
					node.status = 7;
				}
//				ArrayList<ArrayList<Node>> temp = new ArrayList<ArrayList<Node>>();
//				for(int i = 0; i<map.size(); i++) {
//					temp.add(map.remove(0));
//				}
//				map = temp;
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			scanner.close();
			//TODO: Do not make the same mistake again.
			//f.delete();
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
	
	public Boolean planPath(int goal)
	{
		try {
			synchronized(this){
					wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/*while(robotNode == null)
		{
			try{
				synchronized(this){
					wait(2000);
				}
			} catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}*/
			
		Queue<Node> q = new LinkedList<Node>();
		
		synchronized(this) {
			rootNode = robotNode;
			q.add(rootNode);
			while(!q.isEmpty())
			{
				Node currentNode = q.poll();
				if(! currentNode.visited)
				{
					//System.out.println(goalNode.status + "," + goalNode.getX() + "," + goalNode.getY());
					//if(currentNode.status == 6)
					//{
					//System.out.print(goalNode.status);
					//System.out.println("," + currentNode.status);
					//}
//					if(currentNode.status == goalNode.status)
					if(currentNode == goalNode)
					{
						System.out.println("Found a working path.");
						return true;
					}
	
					/*synchronized(this){
						System.out.print("x: " + currentNode.getX());
						System.out.println("y: " + currentNode.getY());
					}*/
					if(currentNode.getX()+1 < x)
					{
						Node nextNode = map.get(currentNode.getY()).get(currentNode.getX()+1);
						if(planningNodeLocationState(goal, nextNode))
						{
							if(nextNode.status != goal)
							{
								nextNode.status = 10;
							}
							nextNode.setPrevNode(currentNode);
							q.add(nextNode);
						}
					}
					
					if(currentNode.getX()-1 > 0)
					{
						Node nextNode = map.get(currentNode.getY()).get(currentNode.getX()-1);
						if(planningNodeLocationState(goal, nextNode))
						{
							if(nextNode.status == goal)
							{
								System.out.println("Found Goal");
							}
							if(nextNode.status != goal)
							{
								nextNode.status = 10;
							}
							nextNode.setPrevNode(currentNode);
							q.add(nextNode);
						}
					}
					
					if(currentNode.getY()+1 < y)//126)
					{
						Node nextNode = map.get(currentNode.getY()+1).get(currentNode.getX());
						if(planningNodeLocationState(goal, nextNode))
						{
							if(nextNode.status == goal)
							{
								System.out.println("Found Goal");
							}
							if(nextNode.status != goal)
							{
								nextNode.status = 10;
							}
							nextNode.setPrevNode(currentNode);
							q.add(nextNode);
						}
					}
					if(currentNode.getY()-1 > 0)
					{
						Node nextNode = map.get(currentNode.getY()-1).get(currentNode.getX());
						if(planningNodeLocationState(goal, nextNode))
						{
							if(nextNode.status == goal)
							{
								System.out.println("Found Goal");
							}
							if(nextNode.status != goal)
							{
								nextNode.status = 10;
							}
							nextNode.setPrevNode(currentNode);
							q.add(nextNode);
						}
					}
				}
			}
		}
		System.out.println("Could not find a working path.");
		return false;
	}
	
	private boolean planningNodeLocationState(int goal, Node nextNode) {
		return nextNode.status == 0 || nextNode.status == goal || nextNode.status == 1 || nextNode.status == 7;
	}
	
	public synchronized void backTrace()
	{
		Stack<Node> s = new Stack<Node>();
		Node currentNode = goalNode;
		s.add(currentNode);
		while(currentNode != rootNode && currentNode != null)
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
	
	public void vectorizePath(int goal)
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
			else if(path.get(0).status == goal)
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
						if(node.status == 2 || node.status == 3 || node.status == 0)
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
		double distance = (slope*pointX + pointY + yIntercept)/Math.sqrt(Math.pow(slope, 2) + 1);
		return distance;
	}
	
	private double getNextSegmentAngle()
	{
		Segment seg = vectorizedPath.get(0);
		return Math.toDegrees(Math.tan((seg.getEndX()-seg.getStartX())/(seg.getEndY()-seg.getStartY())));
	}
	
	private void rotateRobot(boolean right, double speed)
	{
		if(right)
		{
			int movementForwardSpeed = (int)(speed*127/100 + 127);
			int movementBackwardSpeed = (int)(127-speed*127/100);
			byte[] byteArray = {0x11, 0x14, 0x50, (byte)movementForwardSpeed, 0x50, (byte)movementBackwardSpeed};
			locomotionController.writeBytes(byteArray);
		}
		else
		{
			int movementForwardSpeed = (int)(speed*127/100 + 127);
			int movementBackwardSpeed = (int)(127-speed*127/100);
			byte[] byteArray = {0x11, 0x14, 0x50, (byte)movementBackwardSpeed, 0x50, (byte)movementForwardSpeed};
			locomotionController.writeBytes(byteArray);
		}
	}
	
	private synchronized boolean robotIsCloseEnough()
	{
		return
				robotNode.getX() > (vectorizedPath.get(0).getEndX() - DISTANCE_TOLERANCE) &&
				robotNode.getX() < (vectorizedPath.get(0).getEndX() + DISTANCE_TOLERANCE) &&
				robotNode.getY() > (vectorizedPath.get(0).getEndY() - DISTANCE_TOLERANCE) &&
				robotNode.getY() < (vectorizedPath.get(0).getEndY() + DISTANCE_TOLERANCE);
	}
	
	//Speed is a percentage of maximum.
	private void moveRobot(double speed)
	{
		//Note: Arduinos automatically divide the speed by 2.
		//TODO: Test out the change in the direction of functionality.
		boolean forward = vectorizedPath.get(0).getEndY() > vectorizedPath.get(0).getStartY();
		if(forward)
		{
			int movementSpeed = (int)(speed*127/100 + 127);
			byte bytizedSpeed = (byte)movementSpeed;
			System.out.println("Speed = " + movementSpeed);
			byte[] byteArray = {0x11, 0x14, 0x50, bytizedSpeed, 0x50, bytizedSpeed};
			locomotionController.writeBytes(byteArray);
			int robY = 0;
			synchronized(this){
				robY = robotNode.getY();
			}
			while(robY < vectorizedPath.get(0).getEndY())
			{
				try{
					synchronized(this){
						wait();
						robY = robotNode.getY();
					}
				} catch (Exception ex){
					ex.printStackTrace();
				}
				//TODO: Get the error of the vectors corrected.
				/*int change = (int)(errorDistance(vectorizedPath.get(0), robotNode.getX(), robotNode.getY())/PATH_PID_WIDTH);
				byte right = (byte) (change + 127);
				byte left = (byte) (-change + 127);
				byte[] byteArray = {0x14, 0x60, right, 0x60, left};
				locomotionController.writeBytes(byteArray);*/
			}
		}
		else
		{
			//TODO: Add rotation when moving the robot.
			int movementSpeed = (int)(127-speed*127/100);
			System.out.println("Speed =" + movementSpeed);
			byte bytizedSpeed = (byte)movementSpeed;
			if(bytizedSpeed == 0x00)
			{
				bytizedSpeed = 0x01;
			}
			byte[] byteArray = {0x11, 0x14, 0x50, bytizedSpeed, 0x50, bytizedSpeed};
			locomotionController.writeBytes(byteArray);
			int robY = 0;
			synchronized(this)
			{
				robY = robotNode.getY();
			}
			while(robY > vectorizedPath.get(0).getEndY())
			{
				try{
					synchronized(this){
						wait();
						robY = robotNode.getY();
					}
				} catch (Exception ex){
					ex.printStackTrace();
				}
				/*int change = (int)(errorDistance(vectorizedPath.get(0), robotNode.getX(), robotNode.getY())/PATH_PID_WIDTH);
				byte right = (byte) (change + 127);
				byte left = (byte) (-change + 127);
				byte[] byteArray = {0x14, 0x60, right, 0x60, left};
				locomotionController.writeBytes(byteArray);*/
			}
		}
		//vectorizedPath.remove(0);
		stopRobot();
		return;
	}
	
	private void moveRobot(boolean forward, double speed, int milliseconds)
	{
		//Arduinos automatically divide the speed by 2.
		if(forward)
		{
			//TODO: Add rotation when moving the robot.
			//rotateRobot()
			int movementSpeed = (int)(speed*127/100 + 127);
			byte bytizedSpeed = (byte)movementSpeed;
			byte[] byteArray = {0x11, 0x14, 0x50, bytizedSpeed, 0x50, bytizedSpeed};
			locomotionController.writeBytes(byteArray);
		}
		else
		{
			//TODO: Add rotation when moving the robot.
			int movementSpeed = (int)(127-speed*127/100);
			byte bytizedSpeed = (byte)movementSpeed;
			if(bytizedSpeed == 0x00)
			{
				bytizedSpeed = 0x01;
			}
			byte[] byteArray = {0x11, 0x14, 0x50, bytizedSpeed, 0x50, bytizedSpeed};
			locomotionController.writeBytes(byteArray);
		}
		try{
			synchronized(this){
				wait(milliseconds);
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
		stopRobot();
		return;
	}
	
	private void rotateRobot(double deg, double speed)
	{
		try{
			synchronized(this){
				wait();
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
		double endingAngle = 0.0;
		synchronized(this){
			//double startingAngle = robotNode.getAngle();
			endingAngle = robotNode.getAngle() + deg;
		}
		int movementForwardSpeed = (int)(speed*127/100 + 127);
		int movementBackwardSpeed = (int)(127-speed*127/100);
		byte forward = (byte)movementForwardSpeed;
		byte backward = (byte)movementBackwardSpeed;
		if(deg < 0)
		{
			byte[] byteArray = {0x11, 0x14, 0x50, backward, 0x50, forward};
			locomotionController.writeBytes(byteArray);
			double robAng = 0.0;
			synchronized(this) {
				robAng = robotNode.getAngle();
			}
			while(robAng > endingAngle)
			{
				try{
					synchronized(this){
						wait();
						robAng = robotNode.getAngle();
					}
				} catch (Exception ex){
					ex.printStackTrace();
				}
			}
		}
		else if (deg > 0)
		{
			byte[] byteArray = {0x11, 0x14, 0x50, forward, 0x50, backward};
			locomotionController.writeBytes(byteArray);
			double robAng = 0.0;
			synchronized(this) {
				robAng = robotNode.getAngle();
			}
			while(robAng < endingAngle)
			{
				try{
					synchronized(this){
						wait();
						robAng = robotNode.getAngle();
					}
				} catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		stopRobot();
		return;
	}
	
	private void stopRobot() {
		byte[] byteArray = {0x12};
		locomotionController.writeBytes(byteArray);
		
	}
	/*private boolean readOrientationFile()
	{
		int orientation = 0;
		//TODO: Define this function to pull in data for the first orientation routine.
		File f = new File("North.txt");
		if(f.exists())
		{
			Scanner scanner = null;
			try {
				scanner = new Scanner(f);
				orientation = scanner.nextInt();
				scanner.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			waitFor(300);
			f.delete();
		}
		if(orientation == 1)
		{
			return true;
		}
		
		return false;
	}*/
	
	private void getOriented()
	{
		boolean orientedNorth = false;
		//TODO: Try to figure out a way to set the direction easily.
		System.out.println("Rotating...");
		rotateRobot(true, 60);
		try{
			synchronized(this){
				wait();
			}
		} catch(Exception ex)
		{
			ex.printStackTrace();
		}
		stopRobot();
		System.out.println("Facing North!!!!!");
		try{
		synchronized(this){
			wait();
			rotateRobot(0-robotNode.getAngle(),50);
			rotateRobot(0-robotNode.getAngle(),40);
		}
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return;
	}
}

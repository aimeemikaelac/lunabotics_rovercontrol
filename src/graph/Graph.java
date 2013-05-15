package graph;
import java.io.File;
import java.io.FileNotFoundException;
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
	private int goalValue;
	private int rootValue;
	private int blockedValue;
	private Node goalNode;
	private Node rootNode;
	private boolean autoMode;
	private Node robotNode;
	private USBCommunicator locomotionController;
	protected graph.State state;
	private double error;
	//TODO: Setup the Center Node to be inside of the graph.
	public final Node CENTER = new Node(5, 1940, 954, 0);
	public final double ERROR_TOLERANCE = 10.0;
	public final double EXCAVATION_AREA_BOUNDARY = 4440.0;
	public final double OBSTACE_AREA_BOUNDARY = 1500.0;
	public final double FORWARD_DIRECTION = 0.0;
	public final double ANGLE_TOLERANCE = 5.0;
	public final double DISTANCE_TOLERANCE = 50.0;
	public final double PATH_PID_WIDTH = 2;
	public enum CompetitionAreas {
		STARTING, OBSTACLE, EXCAVATION;
	}
	
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
		state = graph.State.START;
		error = 0;
	}
	public synchronized void setAutoMode(boolean newMode) {
		autoMode = newMode;
	}
	
	public void run()
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
					boolean readInFile = loadPositionFromFile("Position.txt");
					if (!readInFile)
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
					if(loadMapFromFile("Map.txt"))
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
		while(true) {
			//Switch statement of ultimate power
			//TODO: implement all states
			switch(state) {
				case START:
					state = State.ORIENTATION;
					break;
				case ORIENTATION:
					getOriented();
					state = State.ROTATE_CENTER;
					break;
				case MOVE_CENTER:
//					moveToCenter();
					state = State.WAIT_MAP;
					break;
				case ROTATE_CENTER:
//					rotateCenter();
					state = State.MOVE_CENTER;
					break;
				case WAIT_MAP:
//					waitForMap();
					state = State.DRIVE_TO_EXCAVATE;
					break;
				case DRIVE_TO_EXCAVATE:
					driveToExcavate();
					break;
//				case EXCAVATE_STRAIGHTISH:
//					driveStraighishExcavate();
//					break;
//				case EXCAVATE_ROTATE:
//					rotateExcavate();
//					break;
				case EXCAVATE:
					excavate();
					break;
				case DRIVE_TO_STARTING_AREA:
//					driveStartingArea();
					break;
				case RETURN_STRAIGHTISH:
//					returnStraightish();
					break;
				case RETURN_ROTATE:
//					returnRotate();
					break;
				case DUMPING_CENTER:
//					dumpingCenter();
					break;
				case BACK_IN:
//					backIn();
					break;
				case DUMP:
//					dump();
					break;
			}
		}
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
	
	private void excavate() {
		final double angle = 15.0;
		// TODO Auto-generated method stub
		// Rotate to 0 degrees
		rotateRobot(0-robotNode.getAngle());
		// Lower Ladder
		lowerLadderRobot();
		startLadderRobot();
		// Set new destination to 5 meter limit plus .25 meters.
		vectorizedPath.add(new Segment(robotNode.getX(),robotNode.getX(),robotNode.getY(),5));
		moveRobot(true);
		//Angle 15 degrees
		rotateRobot(angle);
		//vectorizedPath.add(newSegment(robotNode.getX(),robotNode.getX());
		//
		
	}
	private void driveStraighishExcavate() {
		if(!acceptableError()) {
			state = State.EXCAVATE_ROTATE;
		}
		//Y value of excavation region in millimeters is: 4440.0 mm
		if(reachedDestination(CompetitionAreas.EXCAVATION)) {
			state = State.EXCAVATE;
		}
		
	}
	private synchronized boolean reachedDestination(CompetitionAreas area) {
		double currentY = robotNode.getY();
		switch(area) {
			case EXCAVATION:
				if(currentY > EXCAVATION_AREA_BOUNDARY) {
					return true;
				}
				return false;
			case OBSTACLE:
				if(currentY < EXCAVATION_AREA_BOUNDARY && currentY > OBSTACE_AREA_BOUNDARY) {
					return true;
				}
				return false;
			case STARTING:
				if(currentY < OBSTACE_AREA_BOUNDARY) {
					return true;
				}
				return false;
			default: 
				return false;
		}
	}
	private boolean acceptableError() {
		//TODO: change to correct error tolerance
		if(error < ERROR_TOLERANCE ) {
			return false;
		}
		return false;
	}
	private void driveToExcavate() {
		if(orientedCorrectly()) {
			state = State.EXCAVATE_STRAIGHTISH;
		}
		else {
			state = State.EXCAVATE_ROTATE;
		}
		
	}
	private boolean orientedCorrectly() {
		if(robotNode.getAngle() <= FORWARD_DIRECTION + ANGLE_TOLERANCE && robotNode.getAngle() >= FORWARD_DIRECTION - ANGLE_TOLERANCE) {
			return true;
		}
		return false;
	}
	public void sendPathCorrection()
	{
		if(state == State.EXCAVATE_STRAIGHTISH || state == State.RETURN_STRAIGHTISH) {
			error = errorDistance(vectorizedPath.get(0), robotNode.getX(), robotNode.getY());
			byte[] byteArray = new byte[5];
			byteArray[0] = 0x14;
			byteArray[1] = 0x50;
			byteArray[2] = (byte) (127 - error);
			byteArray[3] = 0x50;
			byteArray[4] = (byte) (127 + error);
			locomotionController.writeBytes(byteArray);
		}
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
				//TODO: Double check the robot Node status is correct.
				robotNode = new Node(0, x, y, angle);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			scanner.close();
			f.delete();
			state = graph.State.EXCAVATE_STRAIGHTISH;
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
					//TODO: use a real value for angle----------------------------|-------------------------------------------------------------------
					map.get(rowsMade-1).add(new Node(value, colsMade, rowsMade-1, 0));
					//------------------------------------------------------------|-------------------------------------------------------------------
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
				if(/*nextNode.status == 0 ||*/ nextNode.status == 6 || nextNode.status == 1)
				{
					if(nextNode.status != 6)
					{
						nextNode.status = 10;
					}
					nextNode.setPrevNode(currentNode);
					q.add(nextNode);
				}
				nextNode = map.get(currentNode.getY()).get(currentNode.getX()-1);
				if(/*nextNode.status == 0 ||*/ nextNode.status == 6 || nextNode.status == 1)
				{
					if(nextNode.status != 6)
					{
						nextNode.status = 10;
					}
					nextNode.setPrevNode(currentNode);
					q.add(nextNode);
				}
				nextNode = map.get(currentNode.getY()+1).get(currentNode.getX());
				if(/*nextNode.status == 0 ||*/ nextNode.status == 6 || nextNode.status == 1)
				{
					if(nextNode.status != 6)
					{
						nextNode.status = 10;
					}
					nextNode.setPrevNode(currentNode);
					q.add(nextNode);
				}
				nextNode = map.get(currentNode.getY()-1).get(currentNode.getX());
				if(/*nextNode.status == 0 ||*/ nextNode.status == 6 || nextNode.status == 1)
				{
					if(nextNode.status != 6)
					{
						nextNode.status = 10;
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
	
	private void rotateRobot()
	{
		byte[] byteArray = {0x11, 0x14, 0x50, 0x01, 0x50, (byte) 0xFE};
		locomotionController.writeBytes(byteArray);
	}
	
	private boolean robotIsCloseEnough()
	{
		return
				robotNode.getX() > (vectorizedPath.get(0).getEndX() - DISTANCE_TOLERANCE) &&
				robotNode.getX() < (vectorizedPath.get(0).getEndX() + DISTANCE_TOLERANCE) &&
				robotNode.getY() > (vectorizedPath.get(0).getEndY() - DISTANCE_TOLERANCE) &&
				robotNode.getY() < (vectorizedPath.get(0).getEndY() + DISTANCE_TOLERANCE);
	}
	
	private void moveRobot(boolean forward)
	{
		if(forward)
		{
			byte[] byteArray = {0x11, 0x14, 0x50, (byte) 0xFE, 0x50, (byte) 0xFE};
			locomotionController.writeBytes(byteArray);
		}
		else
		{
			byte[] byteArray = {0x11, 0x14, 0x50, 0x01, 0x50, 0x01};
			locomotionController.writeBytes(byteArray);
		}
		while(!robotIsCloseEnough())
		{
			try{
				synchronized(this){
					wait(20);
				}
			} catch (Exception ex){
				ex.printStackTrace();
			}
			int change = (int)(errorDistance(vectorizedPath.get(0), robotNode.getX(), robotNode.getY())/PATH_PID_WIDTH);
			byte right = (byte) (change + 127);
			byte left = (byte) (-change + 127);
			byte[] byteArray = {0x14, 0x60, right, 0x60, left};
			locomotionController.writeBytes(byteArray);
		}
		vectorizedPath.remove(0);
		stopRobot();
		return;
	}
	
	private void rotateRobot(double deg)
	{
		double startingAngle = robotNode.getAngle();
		double endingAngle = robotNode.getAngle() + deg;
		if(deg < 0)
		{
			byte[] byteArray = {0x11, 0x14, 0x50, 0x01, 0x50, (byte) 0xFE};
			locomotionController.writeBytes(byteArray);
			while(robotNode.getAngle() > endingAngle)
			{
				try{
					synchronized(this){
						wait(20);
					}
				} catch (Exception ex){
					ex.printStackTrace();
				}
			}
		}
		else if (deg > 0)
		{
			byte[] byteArray = {0x11, 0x14, 0x50, (byte) 0xFE, 0x50, 0x01};
			locomotionController.writeBytes(byteArray);
			while(robotNode.getAngle() < endingAngle)
			{
				try{
					synchronized(this){
						wait(20);
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
	private boolean readOrientationFile()
	{
		//TODO: Define this function to pull in data for the first orientation routine.
		return true;
	}
	
	private void getOriented()
	{
		boolean orientationNotBackwards = true;
		while(orientationNotBackwards)
		{
			//TODO: Check with Thomas to see if he can give me a direction that would be best to turn, and an approximate degree.
			rotateRobot();
			orientationNotBackwards = readOrientationFile();
		}
		/*while(robotNode == null)
		{
			try{
				synchronized(this){
					wait(1);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		return;
	}
}

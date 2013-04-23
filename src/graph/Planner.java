package graph;

import network.serial.USBCommunicator;

public class Planner {
	

	public static void main(String[] args) {
		USBCommunicator com = new USBCommunicator("COM3");
		Graph graph = new Graph(2,3, com);
		graph.loadMapFromFile("test.txt");
		graph.printMapToConsole();
		
		if (graph.planPath())
		{
			graph.printMapToConsole();
			graph.backTrace();
			graph.printPath();
			graph.vectorizePath();
			graph.printVectorizedPath();
		}	
	}

}

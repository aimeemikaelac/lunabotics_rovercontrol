package graph;

import network.serial.USBCommunicator;

public class Planner {
	

	public static void main(String[] args) {
		USBCommunicator com = new USBCommunicator("COM3");
		Graph graph = new Graph(4,com);
		graph.loadMapFromFile("ToDerek.txt", 6);
		graph.printMapToConsole();
		
		if (graph.planPath(6))
		{
			graph.printMapToConsole();
			graph.backTrace();
			graph.printPath();
			graph.vectorizePath(6);
			graph.printVectorizedPath();
		}	
	}

}

package graph;
public class Node {
	public int status;
	public Boolean visited;
	private Node prevNode;
	private int x, y;
	private double angle;
	
	Node(int s, int x, int y, double angle)
	{
		status = s;
		visited = false;
		this.x = x;
		this.y = y;
		this.angle = angle;
		prevNode = null;
	}

	public double getAngle() {
		return angle;
	}

	public int getX() {
		return x;
	}

	public Node getPrevNode() {
		return prevNode;
	}

	public void setPrevNode(Node prevNode) {
		this.prevNode = prevNode;
	}

	public int getY() {
		return y;
	}
}

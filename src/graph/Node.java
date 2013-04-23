package graph;
public class Node {
	public int status;
	public Boolean visited;
	private Node prevNode;
	private int x, y;
	
	Node(int s, int x, int y)
	{
		status = s;
		visited = false;
		this.x = x;
		this.y = y;
		prevNode = null;
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

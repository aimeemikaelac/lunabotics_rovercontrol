package graph;
public class Segment {
	private int startX, startY;
	private int endX, endY;
	
	Segment(int startx, int starty, int endx, int endy)
	{
		startX = startx;
		startY = starty;
		endX = endx;
		endY = endy;
	}

	public int getStartX() {
		return startX;
	}

	public int getStartY() {
		return startY;
	}

	public int getEndX() {
		return endX;
	}

	public int getEndY() {
		return endY;
	}
	
	public double getSlope()
	{
		return ((double)startY-(double)endY)/((double)endX-(double)startX);
	}
}

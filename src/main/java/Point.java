
public class Point {
    //fields
    private double _x;
    private double _y;
    //getters
    public double getX() {
        return _x;
    }
    public double getY() {
        return _y;
    }
    //setters
    public void setX(double newx){
        _x = newx;
    }
    public void setY(double newy){
        _y = newy;
    }
    //constructor
    public Point(double x, double y){
        _x = x;
        _y = y;
    }
    //Calculates the distance from myPoint to the line defined by lineStart and lineEnd
    public static double PerpendicularDistance(Point lineStart, Point lineEnd, Point myPoint){
        double area = Math.abs(0.5*(lineStart.getX()*(lineEnd.getY()-myPoint.getY()) + lineEnd.getX() * (myPoint.getY()-lineStart.getY()) + myPoint.getX() * (lineStart.getY() - lineEnd.getY())));
        double base = Math.sqrt(Math.pow(lineStart.getX()-lineEnd.getX(),2)+Math.pow(lineStart.getY()-lineEnd.getY(),2));
        return (area/base * 2); //height
    }
}


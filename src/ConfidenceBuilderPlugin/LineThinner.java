package ConfidenceBuilderPlugin;

public class LineThinner {
    public static Line VisvaligamWhyattSimplify(int numToKeep, Line myLine) {
        //Thins a line down to the specified amount of points based on the VW algorithm.
        //Implementation based on the description of the method here: http://bost.ocks.org/mike/simplify/

        int removeLimit = (myLine.getVerticesCount()) - numToKeep;
        int minIndex = 1;
        for(int i = 0; i < removeLimit; i++) {
            float minArea = Polygon.AreaOfTriangle(myLine.getPoint(0), myLine.getPoint(1), myLine.getPoint(2)); // This is the baseline we'll start our first comparison to.
            for(int j=2; j< myLine.getVerticesCount()-2;j++) // starting at 2, because we're gonna start calculating areas, and we already calculated area 1.
            { //ending at minus 2 because we don't want the last point, and the size property uses counting numbers, so we have to reduce by an extra -1 to account for 0 index
                float tmpArea = Polygon.AreaOfTriangle(myLine.getPoint(j-1), myLine.getPoint(j), myLine.getPoint(j+1));
                if (tmpArea < minArea) {
                    minIndex = j;
                    minArea = tmpArea;
                }
            }
            myLine.RemovePoint(minIndex);
        }
        return myLine;
    }
}

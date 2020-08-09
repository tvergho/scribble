package edu.dartmouth.cs.scribble.models;

import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SketchModel {
    private List<Point> points;
    private List<Integer> colors;
    private int lastPathIndex; // Starting index of the last path drawn.
    private int lastPoint; // Last point in lastPath that was drawn onto the screen.
    private List<Point> lastPath;

    public SketchModel() {
        points = new ArrayList<>();
        colors = new ArrayList<>();
        lastPath = new ArrayList<>();
        lastPathIndex = 0;
        lastPoint = 0;
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Integer> getColors() {
        return colors;
    }

    public int getLastPathIndex() { return lastPathIndex; }

    public List<Point> getLastPath() { return lastPath; }

    public int getLastPoint() { return lastPoint; }

    public void addPath(Point p, int c) {
        lastPathIndex = points.size();
        lastPath = new ArrayList<>();
        lastPath.add(p);
        lastPoint = 0;
        points.add(p);
        colors.add(c);
    }

    public void modifyLastPath(Point p) {
        points.add(p);
        lastPath.add(p);
    }

    public void clear() {
        points = new ArrayList<>();
        colors = new ArrayList<>();
        lastPath = new ArrayList<>();
        lastPathIndex = 0;
        lastPoint = 0;
    }

    // Calculates and returns a path representing all the points not yet drawn.
    public Path calcLastPath() {
        Path path = new Path();

        if (lastPath.size() > 0) {
            int mX = lastPath.get(0).x;
            int mY = lastPath.get(0).y;
            path.moveTo(mX, mY);

            for (int i = lastPoint; i < lastPath.size(); i++) {
                int x = 1000;
                int y = 1000;

                if (i + lastPathIndex < points.size()) {
                    x = points.get(i + lastPathIndex).x;
                    y = points.get(i  + lastPathIndex).y;
                }

                if (x < 1000 && y < 1000) { // Check if not boundary point.
                    path.quadTo(mX, mY, (x + mX) / 2.0f, (y + mY) / 2.0f);

                    mX = x;
                    mY = y;
                }
            }

            lastPoint = lastPath.size();
        }

        return path;
    }

    // Returns a list of all the paths in the entire drawing (less efficient).
    public List<Path> calcPaths() {
        ArrayList<Path> paths = new ArrayList<>();
        Path curPath = new Path();
        int mX = 0;
        int mY = 0;

        for (int i = 0; i < points.size(); i++) { // Go through all the points, match up with colors.
            // First point.
            if (i == 0) {
                mX = points.get(i).x;
                mY = points.get(i).y;
                curPath.moveTo(mX, mY);
                paths.add(curPath);
            }

            // Placeholder point, marks end of current path.
            if (points.get(i).x == 1000 && points.get(i).y == 1000) {
                curPath = new Path();

                if (i + 1 < points.size()) {
                    mX = points.get(i+1).x;
                    mY = points.get(i+1).y;
                    curPath.moveTo(mX, mY);
                }

                paths.add(curPath);
            }

            else { // Draw the point.
                int x = points.get(i).x;
                int y = points.get(i).y;
                curPath.quadTo(mX, mY, (x + mX) / 2.0f, (y + mY) / 2.0f);
                paths.set(paths.size() - 1, curPath);

                mX = x;
                mY = y;
            }
        }

        return paths;
    }
}

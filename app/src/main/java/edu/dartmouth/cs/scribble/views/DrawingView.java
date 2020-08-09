package edu.dartmouth.cs.scribble.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

import edu.dartmouth.cs.scribble.GameActivity;
import edu.dartmouth.cs.scribble.ml.SketchPredictor;
import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.SketchModel;

/*
    Code adapted from: https://stackoverflow.com/questions/16650419/draw-in-canvas-by-finger-android
*/
public class DrawingView extends View {
    private int width;
    private int height;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    Context context;

    private Path mPath;
    private Paint mBitmapPaint;
    private Paint circlePaint;
    private Path circlePath;
    private Paint mPaint;
    private int mColor;

    private String gameId;
    private SketchModel sketchModel;
    private DatabaseReference myRef;
    private int lastPath = 0; // Reference to the starting index of the last path drawn.
    private boolean listening = false;

    public DrawingView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;
        sketchModel = new SketchModel();

        mColor = Color.BLACK;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        circlePaint = new Paint();
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(4f);
    }

    public void initializeDatabase(String gameId) {
        this.gameId = gameId;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference(gameId).child(Constants.CANVAS);
    }

    public void startDraw() {
        Log.d("receiving", "removed");
        myRef.removeEventListener(canvasListener);
        listening = false;

        // sketchModel = new SketchModel();
        // myRef.setValue(sketchModel); // Update the database.
    }

    public void startGuess() {
        Log.d("receiving", "started");
        myRef.addValueEventListener(canvasListener); // Sets up updates to receive sketches.
        listening = true;
    }

    // Handles receiving a sketch from the database.
    ValueEventListener canvasListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            sketchModel = dataSnapshot.getValue(SketchModel.class);
            Log.d("receiving", "from database");
            if (mCanvas != null && sketchModel != null) {

                // New drawing or it was just erased.
                if (sketchModel.getLastPathIndex() < lastPath || lastPath == 0) {
                    mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // Re-sketch all the paths.
                    for (int i = 0; i < sketchModel.calcPaths().size(); i++) {
                        if (i < sketchModel.getColors().size()) {
                            mColor = sketchModel.getColors().get(i);
                        }

                        mPaint.setColor(mColor);

                        // Check for eraser mode.
                        if (mColor == -1) mPaint.setStrokeWidth(35);
                        else mPaint.setStrokeWidth(12);

                        mCanvas.drawPath(sketchModel.calcPaths().get(i), mPaint);
                    }

                    invalidate();
                }

                // Continuation of current drawing.
                else if (sketchModel.getLastPathIndex() >= lastPath) {
                    mColor = sketchModel.getColors().get(sketchModel.getColors().size() - 1);

                    // Check for eraser mode.
                    if (mColor == -1) mPaint.setStrokeWidth(35);
                    else mPaint.setStrokeWidth(12);

                    mPaint.setColor(mColor);
                    mCanvas.drawPath(sketchModel.calcLastPath(), mPaint);
                    invalidate();
                }

                // Reset last path index.
                lastPath = sketchModel.getLastPathIndex();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    };

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w > 0 && h > 0 && oldw == 0 && oldh == 0) {
            Log.d("resize", w + " " + h);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            width = w;
            height = h;
            mCanvas = new Canvas(mBitmap);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.d("receiving", String.valueOf(GameActivity.isDrawing));
        // Sets up listener once the canvas is drawn.
        if (!GameActivity.isDrawing && mCanvas != null && myRef != null && !listening) {
            startGuess();
        }
        else if (GameActivity.isDrawing) {
            startDraw();
        }

        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
            canvas.drawPath(circlePath, circlePaint);
        }
    }

    // Handles drawing a sketch and updating the database.
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mX = x;
        mY = y;

        if (GameActivity.isErasing) {
            mColor = Color.WHITE;
            mPaint.setColor(mColor);
            mPaint.setStrokeWidth(35);
        }

        mPath.reset();
        mPath.moveTo(x, y);

        sketchModel.addPath(new Point((int)mX, (int)mY), mColor);
        myRef.setValue(sketchModel); // Update the database.
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);

            mX = x;
            mY = y;

            sketchModel.modifyLastPath(new Point((int)mX, (int)mY));
            myRef.setValue(sketchModel); // Update the database.

            circlePath.reset();

            if (GameActivity.isErasing) {
                circlePath.addCircle(mX, mY, 50, Path.Direction.CW);
            }
            else {
                circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
            }
        }
    }

    private void touch_up() {
         circlePath.reset();
         mPath.lineTo(mX, mY);
         sketchModel.modifyLastPath(new Point(1000, 1000)); // Demarcates end of path.
         myRef.setValue(sketchModel); // Update the database.

         // commit the path to our offscreen
         if (mCanvas != null) mCanvas.drawPath(mPath, mPaint);
         // kill this so we don't double draw
         mPath.reset();

         // Goes back to non-erase mode.
         mPaint.setStrokeWidth(12);
    }

    public void setColor(int color) {
        mColor = color;
        mPaint.setColor(mColor);
    }

    public void clearCanvas() {
        if (mCanvas != null && sketchModel != null) {
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            invalidate();
            sketchModel.clear();
            myRef.setValue(sketchModel); // Update the database.
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (GameActivity.isDrawing) { // Disable touch when not drawing.
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
        }

        return true;
    }

    // Asks SketchPredictor to make a prediction.
    public void makePrediction(SketchPredictor sketchPredictor) {
        new InferenceTask(mBitmap).execute(sketchPredictor);
    }

    @Override
    protected void onDetachedFromWindow() {
        myRef.removeEventListener(canvasListener);
        listening = false;
        super.onDetachedFromWindow();
    }

    private class InferenceTask extends AsyncTask<SketchPredictor, Void, Void> {
        private Bitmap bitmap;

        public InferenceTask(Bitmap bitmap) {
            this.bitmap = bitmap.copy(bitmap.getConfig(), true);
        }

        @Override
        protected Void doInBackground(SketchPredictor... predictor) {
            int leftBound = 0, rightBound = 0, topBound = 0, bottomBound = 0;
            Log.d("airunnable", "doinbackground");

            // Find the bounds.
            for (int x = 0; x < bitmap.getWidth(); x++) {
                for (int y = 0; y < bitmap.getHeight(); y++) {
                    int pixel = bitmap.getPixel(x, y);
                    boolean isWhite = Color.alpha(pixel) == 0 || pixel == Color.WHITE;

                    if (leftBound == 0 && !isWhite) leftBound = x;
                    if (x > rightBound && !isWhite) rightBound = x;
                    if (topBound == 0 && !isWhite) topBound = y;
                    if (y > bottomBound && !isWhite) bottomBound = y;

                    if (!isWhite) bitmap.setPixel(x, y, Color.WHITE);
                    else bitmap.setPixel(x, y, Color.BLACK);
                }
            }


            // Nothing's been drawn yet.
            if (!(leftBound == 0 && rightBound == 0 && topBound == 0 && bottomBound == 0)) {
                leftBound = Math.max(leftBound, 0);
                rightBound = Math.max(rightBound, 0);
                topBound = Math.max(topBound, 0);
                bottomBound = Math.max(bottomBound, 0);

                int dimen = Math.max(rightBound - leftBound, bottomBound - topBound); // Find width and height.

                if (dimen + leftBound > bitmap.getWidth() - 1) leftBound = (bitmap.getWidth() - 1) - dimen;
                if (dimen + topBound > bitmap.getHeight() - 1) topBound = (bitmap.getHeight() - 1) - dimen;

                Log.d("bounds", leftBound + " " + rightBound + " " + topBound + " " + bottomBound);

                Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, Math.max(leftBound - 5, 0), Math.max(topBound - 5, 0), dimen, dimen);
                predictor[0].makeInference(resizedBitmap);
            }

            return null;
        }
    }
}
/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.snake;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * SnakeView: implementation of a simple game of Snake
 *
 *
 */
public class SnakeView extends TileView {
    private GestureDetectorCompat gDetect = new GestureDetectorCompat(getContext(), new GestureListener());
    //private MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.hiss);

    private GameThread gameThread;
    private SurfaceHolder holder;

    private static final String TAG = "SnakeView";

    /**
     * Current mode of application: READY to run, RUNNING, or you have already
     * lost. static final ints are used instead of an enum for performance
     * reasons.
     */
    private int mMode = READY;
    public static final int PAUSE = 0;
    public static final int READY = 1;
    public static final int RUNNING = 2;
    public static final int LOSE = 3;
    public static final int WIN = 4;

    /**
     * Current direction the snake is headed.
     */
    private int mDirection = NORTH;
    private int mNextDirection = NORTH;
    private static final int NORTH = 1;
    private static final int SOUTH = 2;
    private static final int EAST = 3;
    private static final int WEST = 4;

    /**
     * Labels for the drawables that will be loaded into the TileView class
     */
    private static final int RED_STAR = 1;
    private static final int YELLOW_STAR = 2;
    private static final int GREEN_STAR = 3;

    /**
     * mScore: used to track the number of apples captured mMoveDelay: number of
     * milliseconds between snake movements. This will decrease as apples are
     * captured.
     */
    private long mScore;
    private long mMoveDelay;
    private float flingMin = 50;
    private float velocityMin = 50;
    /**
     * mLastMove: tracks the absolute time when the snake last moved, and is used
     * to determine if a move should be made based on mMoveDelay.
     */
    private long mLastMove = 0;

    /**
     * mStatusText: text shows to the user in some run states
     */
    private TextView mStatusText;
    private ImageView mStatusImage;

    /**
     * mSnakeTrail: a list of Coordinates that make up the snake's body
     * mAppleList: the secret location of the juicy apples the snake craves.
     */
    private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();
    private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>();

    /**
     * Everyone needs a little randomness in their life
     */
    private static final Random RNG = new Random();

    /**
     * Create a simple handler that we can use to cause animation to happen.  We
     * set ourselves as a target and we can use the sleep()
     * function to cause an update/invalidate to occur at a later date.
     */
    //private RefreshHandler mRedrawHandler = new RefreshHandler();

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            SnakeView.this.update();
            SnakeView.this.invalidate();
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }

    ;


    /**
     * Constructs a SnakeView based on inflation from XML
     *
     * @param context
     * @param attrs
     */
    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSnakeView();
    }

    public SnakeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSnakeView();
    }

    private void initSnakeView() {
        setFocusable(true);

        Resources r = this.getContext().getResources();

        resetTiles(4);
        loadTile(RED_STAR, r.getDrawable(R.drawable.redstar));
        loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
        loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar));

        gameThread = new GameThread(this);
        holder = getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                boolean retry = true;
                gameThread.setRunning(false);
                while (retry) {
                    try {
                        gameThread.join();
                        retry = false;
                    } catch (InterruptedException e) {
                    }
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                gameThread.setRunning(true);
                setWillNotDraw(false);
                gameThread.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }
        });
    }
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public void setBackgroundPic(String src) {
        ConnectionTask task = new ConnectionTask();
        String[] params = new String[1];
        params[0] = src;
        //task.execute(params);
        try {
            task.execute(params).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        //class content
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mMode == READY | mMode == LOSE | mMode == WIN) {

           /*  * At the beginning of the game, or the end of a previous one,
             * we should start a new game.*/

                initNewGame();
                //setBackgroundPic("http://lorempixel.com/720/1280/");
                setMode(RUNNING);
                //update();
            }

            if (mMode == PAUSE) {

            /* * If the game is merely paused, we should just continue where
             * we left off.*/

                setMode(RUNNING);
                //update();
            }
            return true;
        }
        //http://www.sharewallpapers.org/d/400307-1/2008+Harold+and+Kumar+Escape+From+Guantanamo+Bay+020.jpg
        //http://i.imgur.com/3sw7QcP.jpg
        public boolean onDoubleTap(MotionEvent e) {
            if (mMode == READY | mMode == LOSE | mMode == WIN) {
                initNewGame();
                if (randInt(1,100) < 50)
                    setBackgroundPic("http://www.sharewallpapers.org/d/400307-1/2008+Harold+and+Kumar+Escape+From+Guantanamo+Bay+020.jpg");
                else
                    setBackgroundPic("http://i.imgur.com/3sw7QcP.jpg");
                setMode(RUNNING);
                //update();
            }
            return true;
        }
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            //determine what happens on fling events
            //calculate the change in X position within the fling gesture
            float horizontalDiff = event2.getX() - event1.getX();
            //calculate the change in Y position within the fling gesture
            float verticalDiff = event2.getY() - event1.getY();
            float absHDiff = Math.abs(horizontalDiff);
            float absVDiff = Math.abs(verticalDiff);
            float absVelocityX = Math.abs(velocityX);
            float absVelocityY = Math.abs(velocityY);
            if(absHDiff>absVDiff && absHDiff>flingMin && absVelocityX>velocityMin){
                //move forward or backward
                if(horizontalDiff>0) {
                    if (mDirection != WEST)
                        mNextDirection = EAST;
                }
                else {
                    if (mDirection != EAST) {
                        mNextDirection = WEST;
                    }
                }
            }
            else if(absVDiff>flingMin && absVelocityY>velocityMin){
                if(verticalDiff>0) {
                    if (mDirection != NORTH)
                        mNextDirection = SOUTH;
                }
                else {
                    if (mDirection != SOUTH)
                        mNextDirection = NORTH;
                }
            }
            return true;
        }
    }
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private class ConnectionTask extends AsyncTask<String, String, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            //publishProgress("Loading...");
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(input), mBackground.getWidth(), mBackground.getHeight(), true);
                return myBitmap;
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }
        @Override
        protected void onProgressUpdate(String... text) {
            mStatusText.setText(text[0]);
            //mStatusText.setVisibility(View.VISIBLE);
        }
        @Override
        protected void onPreExecute() {
            //mStatusText.setText("Loading...");
            //mStatusText.setVisibility(View.VISIBLE);
            //setMode(LOADING);
        }
        @Override
        protected void onPostExecute(Bitmap result) {
            // result is what you got from your connection
            mSourceBitmap = result;
            MAX_WIDTH = mSourceBitmap.getWidth();
            MAX_HEIGHT = mSourceBitmap.getHeight();
        }

    }
    public boolean onTouchEvent(MotionEvent event){
        this.gDetect.onTouchEvent(event);
        //return super.onTouchEvent(event);
        return true;
    }

    private void initNewGame() {
        mSnakeTrail.clear();
        mAppleList.clear();

        // For now we're just going to load up a short default eastbound snake
        // that's just turned north


        mSnakeTrail.add(new Coordinate(7, 7));
        mSnakeTrail.add(new Coordinate(6, 7));
        mSnakeTrail.add(new Coordinate(5, 7));
        mSnakeTrail.add(new Coordinate(4, 7));
        mSnakeTrail.add(new Coordinate(3, 7));
        mSnakeTrail.add(new Coordinate(2, 7));
        mNextDirection = NORTH;

        // Two apples to start with
        addRandomApple();
        addRandomApple();

        mMoveDelay = 300;
        mScore = 0;

    }


    /**
     * Given a ArrayList of coordinates, we need to flatten them into an array of
     * ints before we can stuff them into a map for flattening and storage.
     *
     * @param cvec : a ArrayList of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates
     * as [x1,y1,x2,y2,x3,y3...]
     */
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int count = cvec.size();
        int[] rawArray = new int[count * 2];
        for (int index = 0; index < count; index++) {
            Coordinate c = cvec.get(index);
            rawArray[2 * index] = c.x;
            rawArray[2 * index + 1] = c.y;
        }
        return rawArray;
    }

    /**
     * Save game state so that the user does not lose anything
     * if the game process is killed while we are in the
     * background.
     *
     * @return a Bundle with this view's state
     */
    public Bundle saveState() {
        Bundle map = new Bundle();

        map.putIntArray("mAppleList", coordArrayListToArray(mAppleList));
        map.putInt("mDirection", Integer.valueOf(mDirection));
        map.putInt("mNextDirection", Integer.valueOf(mNextDirection));
        map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
        map.putLong("mScore", Long.valueOf(mScore));
        map.putIntArray("mSnakeTrail", coordArrayListToArray(mSnakeTrail));

        return map;
    }

    /**
     * Given a flattened array of ordinate pairs, we reconstitute them into a
     * ArrayList of Coordinate objects
     *
     * @param rawArray : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }

    /**
     * Restore game state if our process is being relaunched
     *
     * @param icicle a Bundle containing the game state
     */
    public void restoreState(Bundle icicle) {
        setMode(PAUSE);

        mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
        mDirection = icicle.getInt("mDirection");
        mNextDirection = icicle.getInt("mNextDirection");
        mMoveDelay = icicle.getLong("mMoveDelay");
        mScore = icicle.getLong("mScore");
        mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail"));
    }

    /**
     * Sets the TextView that will be used to give information (such as "Game
     * Over" to the user.
     *
     * @param newView
     */
    public void setTextView(TextView newView) {
        mStatusText = newView;
    }



    public void setImageView(ImageView newView) {
        mStatusImage = newView;
        mStatusImage.setImageResource(R.drawable.snek);
        mStatusImage.setVisibility(View.INVISIBLE);
    }
    private ImageView mBackground;

    public void setBackgroundView(ImageView newView) {
        mBackground = newView;
    }

    private Bitmap mSourceBitmap = null;
    //Bitmap mSourceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.snek);
    //Bitmap mSourceBitmap = getBitmapFromURL("http://i.imgur.com/DerJ7DA.jpg");

    private int MAX_WIDTH = 1000, MAX_HEIGHT = 1000;
    private final int
            START_WIDTH = 0,
            START_HEIGHT = 0,
            WIDTH_INCR = 36,
            HEIGHT_INCR = 64;
    private int mWidth = START_WIDTH, mHeight = START_HEIGHT;
    private boolean mFirstBlockHit = false;
    public void displayNewPortion() {
        if (mWidth == (MAX_WIDTH-START_WIDTH) && mHeight == (MAX_HEIGHT-START_HEIGHT))
            setMode(WIN);
        if (!mFirstBlockHit) {
            mFirstBlockHit = true;
            mBackground.setVisibility(View.VISIBLE);
        }

        if (mWidth+WIDTH_INCR <= MAX_WIDTH)
            mWidth += WIDTH_INCR;
        else
            mWidth = MAX_WIDTH-START_WIDTH;
        if (mHeight+HEIGHT_INCR <= MAX_HEIGHT)
            mHeight += HEIGHT_INCR;
        else
            mHeight = MAX_HEIGHT-START_HEIGHT;
        String except;
        try {
            //mBackground.setImageBitmap((mSourceBitmap));
            Bitmap cropped = Bitmap.createBitmap(mSourceBitmap, START_WIDTH, START_HEIGHT, mWidth, mHeight, null, false);
            mBackground.setImageBitmap(cropped);
        }
        catch (java.lang.IllegalArgumentException e) {
            e.printStackTrace();
            except = e.toString();
            return;
        }
    }
    public void endGame() {
        mFirstBlockHit = false;
        boolean retry = true;
        //gameThread.setRunning(false);
        //mBackground.setVisibility(View.INVISIBLE);
        mWidth = START_WIDTH; mHeight = START_HEIGHT;
        //mediaPlayer.start();
        //mStatusImage.setVisibility(View.VISIBLE);
    }
    /**
     * Updates the current mode of the application (RUNNING or PAUSED or the like)
     * as well as sets the visibility of textview for notification
     *
     * @param newMode
     */
    private CharSequence str = "";
    public void setMode(int newMode) {
        int oldMode = mMode;
        mMode = newMode;

        if (newMode == RUNNING & oldMode != RUNNING) {
            //mStatusImage.setVisibility(View.INVISIBLE);
            mStatusText.setVisibility(View.INVISIBLE);
            //update();
            return;
        }

        Resources res = getContext().getResources();

        if (newMode == PAUSE) {

            str = res.getText(R.string.mode_pause);
        }
        if (newMode == READY) {
            str = res.getText(R.string.mode_ready);
        }
        if (newMode == LOSE) {
            endGame();
            str = res.getString(R.string.mode_lose_prefix) + mScore
                    + res.getString(R.string.mode_lose_suffix);
        }
        if (newMode == WIN) {
            endGame();
            str = res.getString(R.string.mode_win_prefix) + mScore
                    + res.getString(R.string.mode_win_suffix);
        }
        mStatusText.post(new Runnable() {
            @Override
            public void run() {
                mStatusText.setText(str);
                mStatusText.setVisibility(View.VISIBLE);
            }
        });
        /*
        mStatusText.setText(str);
        mStatusText.setVisibility(View.VISIBLE);
        */
    }

    /**
     * Selects a random location within the garden that is not currently covered
     * by the snake. Currently _could_ go into an infinite loop if the snake
     * currently fills the garden, but we'll leave discovery of this prize to a
     * truly excellent snake-player.
     *
     */
    private void addRandomApple() {
        Coordinate newCoord = null;
        boolean found = false;
        while (!found) {
            // Choose a new location for our apple
            int newX = 1 + RNG.nextInt(mXTileCount - 2);
            int newY = 1 + RNG.nextInt(mYTileCount - 2);
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            boolean collision = false;
            int snakelength = mSnakeTrail.size();
            for (int index = 0; index < snakelength; index++) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }
            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            found = !collision;
        }
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        mAppleList.add(newCoord);
    }


    /**
     * Handles the basic update loop, checking to see if we are in the running
     * state, determining if a move should be made, updating the snake's location.
     */
    public void update() {
/*
        if (mMode == RUNNING) {
            clearTiles();
            updateWalls();
            updateSnake();
            updateApples();
        }
*/

        if (mMode == RUNNING) {
            long now = System.currentTimeMillis();

            if (now - mLastMove > mMoveDelay) {
                clearTiles();
                updateWalls();
                updateSnake();
                updateApples();
                mLastMove = now;
            }
            //mRedrawHandler.sleep(mMoveDelay);
        }


    }

    /**
     * Draws some walls.
     *
     */
    private void updateWalls() {
        for (int x = 0; x < mXTileCount; x++) {
            setTile(GREEN_STAR, x, 0);
            setTile(GREEN_STAR, x, mYTileCount - 1);
        }
        for (int y = 1; y < mYTileCount - 1; y++) {
            setTile(GREEN_STAR, 0, y);
            setTile(GREEN_STAR, mXTileCount - 1, y);
        }
    }

    /**
     * Draws some apples.
     *
     */
    private void updateApples() {
        for (Coordinate c : mAppleList) {
            setTile(YELLOW_STAR, c.x, c.y);
        }
    }
    public static int randInt(int min, int max) {
        int randomNum = RNG.nextInt((max - min) + 1) + min;
        return randomNum;
    }
    /**
     * Figure out which way the snake is going, see if he's run into anything (the
     * walls, himself, or an apple). If he's not going to die, we then add to the
     * front and subtract from the rear in order to simulate motion. If we want to
     * grow him, we don't subtract from the rear.
     *
     */
    //private int frameCount = 0;
    private void updateSnake() {
        /*
        frameCount++;
        if (frameCount%5 == 0)
            displayNewPortion();
        */
        boolean growSnake = false;
        // grab the snake by the head
        Coordinate head = mSnakeTrail.get(0);
        Coordinate newHead = new Coordinate(1, 1);

        mDirection = mNextDirection;

        switch (mDirection) {
            case EAST: {
                newHead = new Coordinate(head.x + 1, head.y);
                break;
            }
            case WEST: {
                newHead = new Coordinate(head.x - 1, head.y);
                break;
            }
            case NORTH: {
                newHead = new Coordinate(head.x, head.y - 1);
                break;
            }
            case SOUTH: {
                newHead = new Coordinate(head.x, head.y + 1);
                break;
            }
        }

        // Collision detection
        // For now we have a 1-square wall around the entire arena
        if ((newHead.x < 1) || (newHead.y < 1) || (newHead.x > mXTileCount - 2)
                || (newHead.y > mYTileCount - 2)) {
            setMode(LOSE);
            return;

        }

        // Look for collisions with itself
        int snakelength = mSnakeTrail.size();
        for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
            Coordinate c = mSnakeTrail.get(snakeindex);
            if (c.equals(newHead)) {
                setMode(LOSE);
                return;
            }
        }

        // Look for apples
        int applecount = mAppleList.size();
        for (int appleindex = 0; appleindex < applecount; appleindex++) {
            Coordinate c = mAppleList.get(appleindex);
            if (c.equals(newHead)) {
                mAppleList.remove(c);
                addRandomApple();
                mScore++;
                mMoveDelay *= 0.9;
                //displayNewPortion();
                growSnake = true;
            }
        }

        // push a new head onto the ArrayList and pull off the tail
        mSnakeTrail.add(0, newHead);
        // except if we want the snake to grow
        if (!growSnake) {
            mSnakeTrail.remove(mSnakeTrail.size() - 1);
        }

        int index = 0;
        for (Coordinate c : mSnakeTrail) {
            if (index == 0) {
                setTile(YELLOW_STAR, c.x, c.y);
            } else {
                setTile(RED_STAR, c.x, c.y);
            }
            index++;
        }
    }

    /**
     * Simple class containing two integer values and a comparison function.
     * There's probably something I should use instead, but this was quick and
     * easy to build.
     *
     */
    private class Coordinate {
        public int x;
        public int y;

        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }

}

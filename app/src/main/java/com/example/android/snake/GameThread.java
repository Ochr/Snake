package com.example.android.snake;

import android.graphics.Canvas;

/**
 * Created by Nadir on 2/24/2015.
 */
public class GameThread extends Thread {
    static final long FPS = 10;
    private SnakeView view;
    private boolean running = false;

    public GameThread(SnakeView view) {
        this.view = view;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    @Override
    public void run() {
        long ticksPS = 1000 / FPS;
        long startTime;
        long sleepTime;
        while (running) {
            Canvas c = null;
            startTime = System.currentTimeMillis();
            //view.update();

            try {
                c = view.getHolder().lockCanvas();
                synchronized (view.getHolder()) {
                    view.update();
                    view.postInvalidate();

                }
            } finally {
                if (c != null) {
                    view.getHolder().unlockCanvasAndPost(c);
                }
            }

            sleepTime = ticksPS-(System.currentTimeMillis() - startTime);
            try {
                if (sleepTime > 0)
                    sleep(sleepTime);
                else
                    sleep(10);
            } catch (Exception e) {}
        }
    }
}

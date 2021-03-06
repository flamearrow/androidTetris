package com.ml.gb.tetris.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.ml.gb.R;
import com.ml.gb.tetris.TetrisConstants;
import com.ml.gb.tetris.listener.TetrisGestureListener;
import com.ml.gb.tetris.views.MenuView;
import com.ml.gb.tetris.views.TetrisView;

public class TetrisActivity extends Activity {
    private GestureDetector _gesDect;
    private TetrisView _tetrisView;
    private TetrisGestureListener _tetrisGestureListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("BGLM", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tetris);
        _tetrisView = (TetrisView) findViewById(R.id.tetrisActivity);
        _tetrisView.setHighScores(getSharedPreferences(
                TetrisConstants.TETRIS_SHAREDPREFENCES_NAME, MODE_PRIVATE));
        // can also let TetrisActivity implements OnGestureListener, but that
        // will leave some blank methods
        boolean ttl = getIntent().getBooleanExtra(
                MenuView.TUTORIAL_INTENT_NAME, false);
        _tetrisGestureListener = new TetrisGestureListener(this, ttl);
        _gesDect = new GestureDetector(this, _tetrisGestureListener);

    }

    // called when this method is sent background, should either pause the
    // thread or release resources
    @Override
    protected void onPause() {
        Log.d("BGLM", "onPause");
        super.onPause();
        _tetrisView.pauseHandler();
        _tetrisGestureListener.clearTutorialToast();
    }

    public TetrisView getView() {
        return _tetrisView;
    }

    @Override
    protected void onDestroy() {
        Log.d("BGLM", "onDestroy");
        super.onDestroy();
    }

    // now support this: move left/right when single table left/right part of
    // the screen
    // rotate clockwise when double tap
    // drop when swipe down
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Toast can be used to make a little quick text message box
        // Toast.makeText(this, "mlgb don't touch me!!", Toast.LENGTH_SHORT)
        // .show();
        // explicitly call onTouchEvent - it's a bit weird
        _tetrisView.shake();
        return _gesDect.onTouchEvent(event);
    }

}

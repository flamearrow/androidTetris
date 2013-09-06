package com.ml.gb.tetris;


import android.app.Activity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.ml.gb.R;
import com.ml.gb.tetris.listener.TetrisGestureListener;

public class TetrisActivity extends Activity {
	private GestureDetector _gesDect;
	private TetrisView _tetrisView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tetris);
		_tetrisView = (TetrisView) findViewById(R.id.tetrisActivity);
		// can also let TetrisActivity implements OnGestureListener, but that
		// will leave some blank methods
		_gesDect = new GestureDetector(this, new TetrisGestureListener(this));
	}

	// called when this method is sent background, should either pause the
	// thread or release resources
	@Override
	protected void onPause() {
		super.onPause();
		_tetrisView.pause();
	}

	public TetrisView getView() {
		return _tetrisView;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		_tetrisView.releaseResources();
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
		return _gesDect.onTouchEvent(event);
	}
}

package com.ml.gb.tetris.listener;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;

import com.ml.gb.tetris.TetrisActivity;
import com.ml.gb.tetris.TetrisView;

public class TetrisGestureListener extends SimpleOnGestureListener {
	private TetrisActivity _act;
	private TetrisView _tetrisView;

	// we need max sensibility
	public static final int SWIPE_THRESHOLD = 100;
	public static final int SWIPE_VELOCITY_THRESHOLD = 50;

	public TetrisGestureListener(TetrisActivity act) {
		_act = act;
		_tetrisView = act.getView();
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		Toast.makeText(_act, "doubleTap!!", Toast.LENGTH_SHORT).show();
		_tetrisView.rotate();
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		Toast.makeText(_act, "singleTap!!", Toast.LENGTH_SHORT).show();
		return super.onSingleTapConfirmed(e);
	}

	// extend onFling() method to detect swipe events
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		float diffY = e2.getY() - e1.getY();

		float diffX = e2.getX() - e1.getX();

		// might be swiping left/right
		if (Math.abs(diffX) > Math.abs(diffY)) {
			// if (Math.abs(diffX) > SWIPE_THRESHOLD
			// && velocityX > SWIPE_VELOCITY_THRESHOLD) {
			if (diffX > 0)
				onSwipeRight();
			else
				onSwipeLeft();
		}
		// might be swiping up/down
		else {
			// if (Math.abs(diffY) > SWIPE_THRESHOLD
			// && velocityY > SWIPE_VELOCITY_THRESHOLD) {
			if (diffY > 0)
				onSwipeDown();
			else
				onSwipeUp();
		}

		return true;
	}

	public void onSwipeRight() {
		_tetrisView.moveRight();
	}

	public void onSwipeLeft() {
		_tetrisView.moveLeft();
	}

	public void onSwipeUp() {
		_tetrisView.rotate();
	}

	public void onSwipeDown() {
		_tetrisView.drop();
	}
}

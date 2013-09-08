package com.ml.gb.tetris.listener;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.ml.gb.tetris.TetrisActivity;
import com.ml.gb.tetris.TetrisView;

public class TetrisGestureListener extends SimpleOnGestureListener {
	private TetrisView _tetrisView;

	// sensitivity to detect swiping down gesture
	public static final int SWIPE_THRESHOLD = 100;
	// sensitivity to detect scrolling left/right gesture
	public static final int SCROLL_THRESHOLD = 15;

	public TetrisGestureListener(TetrisActivity act) {
		_tetrisView = act.getView();
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		_tetrisView.rotate();
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		// Toast.makeText(_act, "singleTap!!", Toast.LENGTH_SHORT).show();
		// _tetrisView.rotate();
		return super.onSingleTapConfirmed(e);
	}

	// extend onFling() method to detect swipe events
	// swipe is only used for fast dropping, for moving left/right, use
	// onScroll()
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		float diffY = e2.getY() - e1.getY();

		float diffX = e2.getX() - e1.getX();

		// might be swiping up/down
		if (Math.abs(diffY) > Math.abs(diffX)) {
			if (diffY > SWIPE_THRESHOLD)
				_tetrisView.setFastDropping();
			else if (diffY < 0)
				_tetrisView.rotate();
		}

		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		if (Math.abs(distanceX) > SCROLL_THRESHOLD) {
			if (distanceX > 0)
				_tetrisView.moveLeft();
			else
				_tetrisView.moveRight();
		}
		return true;
	}
}

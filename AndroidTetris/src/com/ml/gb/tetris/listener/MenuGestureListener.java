package com.ml.gb.tetris.listener;

import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;

public class MenuGestureListener extends SimpleOnGestureListener {
	public static final int SCROLL_THRESHOLD = 15;

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		float absX = Math.abs(distanceX);
		float absY = Math.abs(distanceY);
		if (absX > SCROLL_THRESHOLD || absY > SCROLL_THRESHOLD) {
			if (absX > absY) {
				if (distanceX > 0) {
					// move right
				} else {
					// move left
				}
			} else {
				if (distanceY > 0) {
					// move down
				} else {
					// move up
				}
			}
		}
		return true;
	}
}

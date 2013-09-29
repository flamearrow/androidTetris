package com.ml.gb.tetris.listener;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.ml.gb.tetris.views.MenuView;

public class MenuGestureListener extends SimpleOnGestureListener {
	public static final int SCROLL_THRESHOLD = 20;
	public static final int DIAGNAL_THRESHOLD = 5;
	private MenuView _menuView;

	public MenuGestureListener(MenuView menuView) {
		_menuView = menuView;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		float absX = Math.abs(distanceX);
		float absY = Math.abs(distanceY);
		if (absX > SCROLL_THRESHOLD || absY > SCROLL_THRESHOLD) {
			if (Math.abs(absX - absY) < DIAGNAL_THRESHOLD) {
				if (distanceX > 0) {
					if (distanceY > 0) {
						_menuView.moveUpLeft();
					} else {
						_menuView.moveDownLeft();
					}
				} else {
					if (distanceY > 0) {
						_menuView.moveUpRight();
					} else {
						_menuView.moveDownRight();
					}
				}
			} else {

				if (absX > absY) {
					if (distanceX > 0) {
						// move left
						_menuView.moveLeft();
					} else {
						// move right
						_menuView.moveRight();
					}
				} else {
					if (distanceY > 0) {
						// move up
						_menuView.moveUp();
					} else {
						// move down
						_menuView.moveDown();
					}
				}
			}
		}
		return true;
	}
}

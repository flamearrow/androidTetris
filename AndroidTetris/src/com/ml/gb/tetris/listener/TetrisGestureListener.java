package com.ml.gb.tetris.listener;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;

import com.ml.gb.tetris.activities.TetrisActivity;
import com.ml.gb.tetris.views.TetrisView;

public class TetrisGestureListener extends SimpleOnGestureListener {
	private TetrisView _tetrisView;

	private static final String TUTORIAL_SWIPE_LEFT_RIGHT = "Swipe ←/→  to move";
	private static final String TUTORIAL_SWIPE_UP = "Swipe ↑ to rotate";
	private static final String TUTORIAL_SIWPE_DOWN_OR_DOUBLE_TAP = "Swipe ↓ or double tap to drop";
	private static final String TUTORIAL_DONE = "You're good to go!";
	private int _tutorialMask;
	private static final int LEFT_SWIPED = 1 << 0;
	private static final int RIGHT_SWIPED = 1 << 1;
	private static final int UP_SWIPED = 1 << 2;
	private static final int DOWN_SWIPED = 1 << 3;
	private static final int DOUBLE_TAPPED = 1 << 4;
	private boolean _tutorialEnabled;
	private Toast _tutorialToast;

	// sensitivity to detect swiping down gesture
	public static final int SWIPE_THRESHOLD = 100;
	// sensitivity to detect scrolling left/right gesture
	public static final int SCROLL_THRESHOLD = 15;

	public TetrisGestureListener(TetrisActivity act, boolean enableTutorial) {
		_tetrisView = act.getView();

		_tutorialMask = 0;
		_tutorialEnabled = enableTutorial;
		if (_tutorialEnabled) {
			_tutorialToast = Toast.makeText(act, TUTORIAL_SWIPE_LEFT_RIGHT,
					Toast.LENGTH_LONG);
			_tutorialToast.show();
		}
	}

	public void clearTutorialToast() {
		if (_tutorialToast != null)
			_tutorialToast.cancel();
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (_tutorialEnabled) {
			if (hasDoneThese(LEFT_SWIPED, RIGHT_SWIPED, UP_SWIPED)) {
				addMask(DOUBLE_TAPPED);
				_tetrisView.setFastDropping();
				checkTutorialEnd();
			} else {
				_tutorialToast.show();
			}
		} else {
			_tetrisView.setFastDropping();
		}
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
			if (diffY > SWIPE_THRESHOLD) {
				if (_tutorialEnabled) {
					if (hasDoneThese(LEFT_SWIPED, RIGHT_SWIPED, UP_SWIPED)) {
						addMask(DOWN_SWIPED);
						_tetrisView.setFastDropping();
						checkTutorialEnd();
					} else {
						_tutorialToast.show();
						return false;
					}
				} else {
					_tetrisView.setFastDropping();
				}
			} else if (diffY < 0)
				if (_tutorialEnabled) {
					if (hasDoneThese(LEFT_SWIPED, RIGHT_SWIPED)) {
						addMask(UP_SWIPED);
						_tetrisView.rotate();
						if (hasDoneThese(LEFT_SWIPED, RIGHT_SWIPED, UP_SWIPED)) {
							if (hasNotDoneThis(DOUBLE_TAPPED)
									&& hasNotDoneThis(DOWN_SWIPED)) {
								showTutorial(TUTORIAL_SIWPE_DOWN_OR_DOUBLE_TAP);
							} else {
								_tutorialToast.show();
							}
						}
					} else {
						_tutorialToast.show();
						return false;
					}
				} else {
					_tetrisView.rotate();
				}

		}

		return true;
	}

	// when finger drags on screen, onScroll() will be called multiple times
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		if (Math.abs(distanceX) > SCROLL_THRESHOLD) {
			if (distanceX > 0) {
				if (_tutorialEnabled) {
					addMask(LEFT_SWIPED);
					_tetrisView.moveLeft();
					if (hasDoneThese(LEFT_SWIPED, RIGHT_SWIPED)) {
						if (hasNotDoneThis(UP_SWIPED)) {
							showTutorial(TUTORIAL_SWIPE_UP);
						} else {
							_tutorialToast.show();
						}
					}
				} else {
					_tetrisView.moveLeft();
				}
			} else {
				if (_tutorialEnabled) {
					addMask(RIGHT_SWIPED);
					_tetrisView.moveRight();
					if (hasDoneThese(LEFT_SWIPED, RIGHT_SWIPED)) {
						if (hasNotDoneThis(UP_SWIPED)) {
							showTutorial(TUTORIAL_SWIPE_UP);
						} else {
							_tutorialToast.show();
						}
					}
				} else {
					_tetrisView.moveRight();
				}

			}
		}
		return true;
	}

	private void addMask(int mask) {
		_tutorialMask |= mask;
	}

	private boolean hasDoneThese(int... masks) {
		for (int mask : masks) {
			if ((_tutorialMask & mask) == 0) {
				return false;
			}
		}
		return true;
	}

	private boolean hasNotDoneThis(int mask) {
		return (_tutorialMask & mask) == 0;
	}

	private void checkTutorialEnd() {
		if (hasDoneThese(LEFT_SWIPED, RIGHT_SWIPED, UP_SWIPED, DOWN_SWIPED,
				DOUBLE_TAPPED)) {
			_tutorialEnabled = false;
			showTutorial(TUTORIAL_DONE);
		}
	}

	private void showTutorial(String s) {
		_tutorialToast.setText(s);
		_tutorialToast.show();
	}

}

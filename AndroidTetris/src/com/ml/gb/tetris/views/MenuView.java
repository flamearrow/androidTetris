package com.ml.gb.tetris.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.ml.gb.tetris.activities.HighScoreListActivity;
import com.ml.gb.tetris.activities.MenuActivity;
import com.ml.gb.tetris.activities.TetrisActivity;
import com.ml.gb.tetris.listener.MenuGestureListener;

/**
 * A Tetris flavor menu providing user the ability to choose
 * newgame/highscore/tutorial/exit by dragging the 'O' in side a matrix to four
 * corners
 * 
 * @author flamearrow
 * 
 */
public class MenuView extends SurfaceView implements Callback {
	private MenuActivity _menuActivity;

	private int _viewWidth;
	private int _viewHeight;
	private int _edgeLength;
	private static final int MENU_MATRIX_EDGE = 10;
	private GestureDetector _gesDect;

	private static String DEBUG_TAG = "MenuView";

	private Paint _menuPaint;
	private Paint _backgroundPaint;

	private Set<Point> _currentBlockPoints;
	// represents the point of four corners
	private Set<Point> _upperLeftCornerPoints;
	private Set<Point> _upperRightCornerPoints;
	private Set<Point> _lowerLeftCornerPoints;
	private Set<Point> _lowerRightCornerPoints;

	private SurfaceHolder _holder;

	private List<Point> _backList;

	private static final int BACKGROUND_COLOR = Color.WHITE;

	private static final int SQUARE_EDGE_COLOR = Color.RED;

	private static final int INITIAL_BLOCK_COLOR = Color.WHITE;

	private static final int SET_BLOCK_COLOR = Color.YELLOW;

	private static final int SQUARE_EDGE_WIDTH = 2;

	private boolean _isDragging;

	private Rect _currentHighlightRect;

	public MenuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		_menuPaint = new Paint();
		_backgroundPaint = new Paint();
		_backgroundPaint.setColor(BACKGROUND_COLOR);
		_currentBlockPoints = new HashSet<Point>();
		_upperLeftCornerPoints = new HashSet<Point>();
		_upperRightCornerPoints = new HashSet<Point>();
		_lowerLeftCornerPoints = new HashSet<Point>();
		_lowerRightCornerPoints = new HashSet<Point>();

		_backList = new ArrayList<Point>();
		_gesDect = new GestureDetector(context, new MenuGestureListener(this));
		_isDragging = false;
		_currentHighlightRect = new Rect();

	}

	// record the size of the current view - note this is not full screen!
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(DEBUG_TAG, "onSizeChanged");
		super.onSizeChanged(w, h, oldw, oldh);
		_viewWidth = w;
		_viewHeight = h;
		_edgeLength = _viewWidth / MENU_MATRIX_EDGE;

		// resetHighlightBlock();

		// initialize four corner point sets
		_upperLeftCornerPoints.add(new Point(0, 0));
		_upperLeftCornerPoints.add(new Point(0, 1));
		_upperLeftCornerPoints.add(new Point(1, 0));
		_upperLeftCornerPoints.add(new Point(1, 1));

		_upperRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 1, 0));
		_upperRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 2, 0));
		_upperRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 1, 1));
		_upperRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 2, 1));

		_lowerLeftCornerPoints.add(new Point(0, MENU_MATRIX_EDGE - 1));
		_lowerLeftCornerPoints.add(new Point(0, MENU_MATRIX_EDGE - 2));
		_lowerLeftCornerPoints.add(new Point(1, MENU_MATRIX_EDGE - 1));
		_lowerLeftCornerPoints.add(new Point(1, MENU_MATRIX_EDGE - 2));

		_lowerRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 1,
				MENU_MATRIX_EDGE - 1));
		_lowerRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 1,
				MENU_MATRIX_EDGE - 2));
		_lowerRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 2,
				MENU_MATRIX_EDGE - 1));
		_lowerRightCornerPoints.add(new Point(MENU_MATRIX_EDGE - 2,
				MENU_MATRIX_EDGE - 2));

	}

	/**
	 * reset the initial position is to center, also need to initialize
	 * _currentHighlightRect here
	 */
	private void resetHighlightBlock() {
		int middLeft = MENU_MATRIX_EDGE / 2 - 1;
		int middRight = middLeft + 1;

		// set initial highLightRect
		_currentHighlightRect.set(middLeft * _edgeLength, middLeft
				* _edgeLength, (middRight + 1) * _edgeLength, (middRight + 1)
				* _edgeLength);

		_currentBlockPoints.clear();
		_currentBlockPoints.add(new Point(middLeft, middLeft));
		_currentBlockPoints.add(new Point(middLeft, middRight));
		_currentBlockPoints.add(new Point(middRight, middRight));
		_currentBlockPoints.add(new Point(middRight, middLeft));
	}

	/**
	 * redraw the entire screen
	 */
	private void drawAll() {
		// make sure one call only request lock once, otherwise it will flash
		// drawMenuBackground();
		drawMenuBlocks();
	}

	private void drawMenuBlocks() {
		Canvas canvas = null;
		try {
			canvas = _holder.lockCanvas();
			Point drawingP = new Point(0, 0);
			Point setP = new Point();
			synchronized (_holder) {
				canvas.drawRect(0, 0, _viewWidth, _viewHeight, _backgroundPaint);
				for (int i = 0; i < MENU_MATRIX_EDGE; i++) {
					for (int j = 0; j < MENU_MATRIX_EDGE; j++) {
						setP.set(j, i);
						_menuPaint.setColor(SQUARE_EDGE_COLOR);
						canvas.drawRect(drawingP.x, drawingP.y, drawingP.x
								+ _edgeLength, drawingP.y + _edgeLength,
								_menuPaint);
						_menuPaint
								.setColor(_currentBlockPoints.contains(setP) ? SET_BLOCK_COLOR
										: INITIAL_BLOCK_COLOR);
						canvas.drawRect(drawingP.x + SQUARE_EDGE_WIDTH,
								drawingP.y + SQUARE_EDGE_WIDTH, drawingP.x
										+ _edgeLength - SQUARE_EDGE_WIDTH,
								drawingP.y + _edgeLength - SQUARE_EDGE_WIDTH,
								_menuPaint);
						drawingP.offset(_edgeLength, 0);
					}
					drawingP.offset(-MENU_MATRIX_EDGE * _edgeLength,
							_edgeLength);
				}
			}
		} finally {
			if (canvas != null) {
				_holder.unlockCanvasAndPost(canvas);
			}
		}
	}

	// catch the holder
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		_holder = holder;
		resetHighlightBlock();
		drawAll();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// Nothing
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Nothing

	}

	public boolean moveLeft() {
		return moveAndRedraw(new Point(-1, 0));
	}

	public boolean moveRight() {
		return moveAndRedraw(new Point(1, 0));
	}

	public boolean moveUp() {
		return moveAndRedraw(new Point(0, -1));
	}

	public boolean moveDown() {
		return moveAndRedraw(new Point(0, 1));
	}

	public boolean moveUpLeft() {
		return moveAndRedraw(new Point(-1, -1));
	}

	public boolean moveUpRight() {
		return moveAndRedraw(new Point(1, -1));
	}

	public boolean moveDownLeft() {
		return moveAndRedraw(new Point(-1, 1));
	}

	public boolean moveDownRight() {
		return moveAndRedraw(new Point(1, 1));
	}

	// check where the current block is, if it's on any corner, perform action,
	// otherwise reset the block to initial place
	public void checkPositionAndReset() {
		_isDragging = false;
		if (_currentBlockPoints.equals(_upperLeftCornerPoints)) {
			// start game
			Log.d(DEBUG_TAG, "should start game");
			getContext().startActivity(
					new Intent(getContext(), TetrisActivity.class));
		} else if (_currentBlockPoints.equals(_upperRightCornerPoints)) {
			// highscore
			Log.d(DEBUG_TAG, "should print highscore");
			getContext().startActivity(
					new Intent(getContext(), HighScoreListActivity.class));
		} else if (_currentBlockPoints.equals(_lowerLeftCornerPoints)) {
			// start game with turorial
			Log.d(DEBUG_TAG, "should start game with tutorial");
		} else if (_currentBlockPoints.equals(_lowerRightCornerPoints)) {
			// exit
			_menuActivity.finish();
		} else {
			// reset the initial block
			resetHighlightBlock();
			drawAll();
		}

	}

	private boolean moveAndRedraw(Point delta) {
		boolean succeed = true;
		Point tmpPoint = new Point();
		for (Point p : _currentBlockPoints) {
			tmpPoint.set(p.x + delta.x, p.y + delta.y);
			// if we hit boundary or the new block is already set then we
			// can't
			// continue dropping
			if ((tmpPoint.x < 0 || tmpPoint.y < 0 || tmpPoint.y >= MENU_MATRIX_EDGE)
					|| (tmpPoint.x >= MENU_MATRIX_EDGE)) {
				succeed = false;
				break;
			}
		}

		if (succeed) {
			_backList.clear();
			for (Point p : _currentBlockPoints) {
				p.offset(delta.x, delta.y);
			}
			for (Point p : _currentBlockPoints) {
				_backList.add(p);
			}
			// important: we need to update Points index in the hashset
			_currentBlockPoints.clear();
			_currentBlockPoints.addAll(_backList);

			if (delta.x == 1) {
				_currentHighlightRect.offset(_edgeLength, 0);
			} else if (delta.x == -1) {
				_currentHighlightRect.offset(-_edgeLength, 0);
			}

			if (delta.y == 1) {
				_currentHighlightRect.offset(0, _edgeLength);
			} else if (delta.y == -1) {
				_currentHighlightRect.offset(0, -_edgeLength);
			}

			drawAll();
		}

		return succeed;
	}

	// note: if onTouchEvent returns false, then it means this event is not
	// consumed by the view, which means it's not done yet, so the
	// MotionEvent.ACTION_UP will not be caught here - will be handled by
	// Activity.onTouchEvent
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = MotionEventCompat.getActionMasked(event);
		if (action == MotionEvent.ACTION_UP) {
			checkPositionAndReset();
		}
		if (action == MotionEvent.ACTION_DOWN) {
			// TODO: check if the event happens on the current highlight block
			if (_currentHighlightRect.contains((int) event.getX(),
					(int) event.getY())) {
				_isDragging = true;
			}
		}
		_gesDect.onTouchEvent(event);
		return true;
	}

	/**
	 * call back at the end of a scroll event, should calculate the new position
	 * of highlight area and redraw
	 */
	public void handleScrollEndEvent(MotionEvent e2) {
		if (_isDragging) {
			// dragged outside, should redraw
			int e2X = (int) e2.getX();
			int e2Y = (int) e2.getY();
			if (!_currentHighlightRect.contains(e2X, e2Y)) {
				if (e2X < _currentHighlightRect.left) {
					if (e2Y < _currentHighlightRect.top) {
						moveUpLeft();
					} else if (e2Y > _currentHighlightRect.bottom) {
						moveDownLeft();
					} else {
						moveLeft();
					}
				} else if (e2X > _currentHighlightRect.right) {
					if (e2Y < _currentHighlightRect.top) {
						moveUpRight();
					} else if (e2Y > _currentHighlightRect.bottom) {
						moveDownRight();
					} else {
						moveRight();
					}
				} else {
					if (e2Y < _currentHighlightRect.top) {
						moveUp();
					} else {
						moveDown();
					}
				}
			}
		}
	}

	public MenuActivity getMenuActivity() {
		return _menuActivity;
	}

	public void setMenuActivity(MenuActivity _menuActivity) {
		this._menuActivity = _menuActivity;
	}
}

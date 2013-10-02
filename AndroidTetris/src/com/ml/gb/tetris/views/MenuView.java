package com.ml.gb.tetris.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

import com.ml.gb.R;
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
	private int _edgeGapWidth;
	private static final int MENU_MATRIX_EDGE = 10;
	private GestureDetector _gesDect;

	private static String DEBUG_TAG = "MenuView";

	private Paint _menuPaint;
	private Paint _backgroundPaint;
	private Paint _optionPaint;

	private Set<Point> _currentBlockPoints;
	// represents the point of four corners
	private Set<Point> _upperLeftCornerPoints;
	private Set<Point> _upperRightCornerPoints;
	private Set<Point> _lowerLeftCornerPoints;
	private Set<Point> _lowerRightCornerPoints;

	private SurfaceHolder _holder;

	private List<Point> _backList;

	private static final int BACKGROUND_COLOR = Color.WHITE;

	private static final int SQUARE_EDGE_COLOR = Color.WHITE;

	private static final int INITIAL_BLOCK_COLOR = Color.WHITE;

	private static final int SET_BLOCK_COLOR = Color.GRAY;

	private static final int CORNER_HIGHLIGHT_COLOR = Color.YELLOW;

	private static final int OPTION_EDGE_COLOR = Color.LTGRAY;

	private static final String MSG_INCORRECT_TOUCH = "Drag the block to a corner!";

	private static final String MSG_NEW_GAME = "NEW GAME";

	private static final String MSG_HIGH_SCORE = "HIGH SCORES";

	private static final String MSG_TUTORIAL = "TUTORIAL";

	private static final String MSG_EXIT = "EXIT";

	private boolean _isDragging;

	private Rect _currentHighlightRect;

	private Toast _promtToast;

	private boolean _justCreated;

	@SuppressLint("ShowToast")
	public MenuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		_menuPaint = new Paint();
		_optionPaint = new Paint();
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

		// supress warning, show toast later
		_promtToast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);

		_justCreated = true;

	}

	// record the size of the current view - note this is not full screen!
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(DEBUG_TAG, "onSizeChanged");
		super.onSizeChanged(w, h, oldw, oldh);
		_viewWidth = w;
		_viewHeight = h;
		_edgeLength = _viewWidth / MENU_MATRIX_EDGE;
		_edgeGapWidth = _edgeLength / TetrisView.EDGE_GAP_RATIO;
		_promtToast.setGravity(Gravity.TOP, 0, _viewHeight / 4);
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

	private Option checkAtCorner(Point p) {
		if (_upperLeftCornerPoints.contains(p)) {
			return Option.NEW_GAME;
		} else if (_upperRightCornerPoints.contains(p)) {
			return Option.HIGH_SCORE;
		} else if (_lowerLeftCornerPoints.contains(p)) {
			return Option.TUTORIAL;
		} else if (_lowerRightCornerPoints.contains(p)) {
			return Option.EXIT;
		} else {
			return Option.NONE;
		}
	}

	private void showPrompt(String s) {
		_promtToast.setText(s);
		_promtToast.show();
	}

	private void drawMenuBlocks() {
		Canvas canvas = null;
		try {
			canvas = _holder.lockCanvas();
			Point drawingP = new Point(0, 0);
			Point setP = new Point();
			synchronized (_holder) {
				// draw background
				canvas.drawRect(0, 0, _viewWidth, _viewHeight, _backgroundPaint);

				// draw the highlighted block
				for (int i = 0; i < MENU_MATRIX_EDGE; i++) {
					for (int j = 0; j < MENU_MATRIX_EDGE; j++) {
						setP.set(j, i);
						_menuPaint.setColor(SQUARE_EDGE_COLOR);
						canvas.drawRect(drawingP.x, drawingP.y, drawingP.x
								+ _edgeLength, drawingP.y + _edgeLength,
								_menuPaint);
						if (_currentBlockPoints.contains(setP)) {

							switch (checkAtCorner(setP)) {
							case NEW_GAME:
								_menuPaint.setColor(CORNER_HIGHLIGHT_COLOR);
								showPrompt(MSG_NEW_GAME);
								break;
							case HIGH_SCORE:
								_menuPaint.setColor(CORNER_HIGHLIGHT_COLOR);
								showPrompt(MSG_HIGH_SCORE);
								break;
							case TUTORIAL:
								_menuPaint.setColor(CORNER_HIGHLIGHT_COLOR);
								showPrompt(MSG_TUTORIAL);
								break;
							case EXIT:
								_menuPaint.setColor(CORNER_HIGHLIGHT_COLOR);
								showPrompt(MSG_EXIT);
								break;
							case NONE:
								_menuPaint.setColor(SET_BLOCK_COLOR);
								break;
							}

						} else {
							_menuPaint.setColor(INITIAL_BLOCK_COLOR);
						}
						canvas.drawRect(drawingP.x + _edgeGapWidth, drawingP.y
								+ _edgeGapWidth, drawingP.x + _edgeLength
								- _edgeGapWidth, drawingP.y + _edgeLength
								- _edgeGapWidth, _menuPaint);
						drawingP.offset(_edgeLength, 0);
					}
					drawingP.offset(-MENU_MATRIX_EDGE * _edgeLength,
							_edgeLength);
				}

				// draw options at four corners
				_optionPaint.setColor(OPTION_EDGE_COLOR);
				_optionPaint.setStrokeWidth(_edgeGapWidth);
				_optionPaint.setStyle(Paint.Style.STROKE);
				canvas.drawRect(0, 0, 2 * _edgeLength, 2 * _edgeLength,
						_optionPaint);
				canvas.drawRect(_viewWidth - 2 * _edgeLength, 0, _viewWidth,
						2 * _edgeLength, _optionPaint);
				canvas.drawRect(0, _viewWidth - 2 * _edgeLength,
						2 * _edgeLength, _viewWidth, _optionPaint);
				canvas.drawRect(_viewWidth - 2 * _edgeLength, _viewWidth - 2
						* _edgeLength, _viewWidth, _viewWidth, _optionPaint);
				// draw text
				_optionPaint.setColor(getResources().getColor(
						R.color.light_orange));
				_optionPaint.setStrokeWidth(0);
				_optionPaint.setTextSize(_edgeLength);
				_optionPaint.setTextAlign(Align.CENTER);
				float offset = _edgeLength;
				canvas.drawText(getResources().getString(R.string.new_game),
						offset, offset * 4 / 3, _optionPaint);
				canvas.drawText(
						getResources().getString(R.string.high_score_option),
						_viewWidth - offset, offset * 4 / 3, _optionPaint);
				canvas.drawText(getResources().getString(R.string.tutorial),
						offset, _viewWidth - offset * 2 / 3, _optionPaint);
				canvas.drawText(getResources().getString(R.string.exit),
						_viewWidth - offset, _viewWidth - offset * 2 / 3,
						_optionPaint);

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
		if (_justCreated) {
			_justCreated = false;
			showPrompt(MSG_INCORRECT_TOUCH);
		}
	}

	public void clearToast() {
		_promtToast.cancel();
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
			_promtToast.cancel();
			// start game
			getContext().startActivity(
					new Intent(getContext(), TetrisActivity.class));
		} else if (_currentBlockPoints.equals(_upperRightCornerPoints)) {
			_promtToast.cancel();
			// highscore
			getContext().startActivity(
					new Intent(getContext(), HighScoreListActivity.class));
		} else if (_currentBlockPoints.equals(_lowerLeftCornerPoints)) {
			_promtToast.cancel();
			// TODO: start game with turorial
			Log.d(DEBUG_TAG, "should start game with tutorial");
		} else if (_currentBlockPoints.equals(_lowerRightCornerPoints)) {
			_promtToast.cancel();
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
			if (_currentHighlightRect.contains((int) event.getX(),
					(int) event.getY())) {
				_isDragging = true;
				_promtToast.cancel();
			} else {
				showPrompt(MSG_INCORRECT_TOUCH);
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

	private enum Option {
		NEW_GAME, TUTORIAL, EXIT, HIGH_SCORE, NONE;
	}
}

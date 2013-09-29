package com.ml.gb.tetris.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

/**
 * A Tetris flavor menu providing user the ability to choose
 * newgame/highscore/tutorial/exit by dragging the 'O' in side a matrix to four
 * corners
 * 
 * @author flamearrow
 * 
 */
public class MenuView extends SurfaceView implements Callback {
	private int _viewWidth;
	private int _viewHeight;
	private int _edgeLength;
	private static final int MENU_MATRIX_EDGE = 6;

	private Paint _menuPaint;
	private Paint _backgroundPaint;

	private Set<Point> _currentBlockPoints;

	private SurfaceHolder _holder;

	private List<Point> _backList;

	private static final int BACKGROUND_COLOR = Color.WHITE;

	private static final int SQUARE_EDGE_COLOR = Color.RED;

	private static final int INITIAL_BLOCK_COLOR = Color.WHITE;

	private static final int SET_BLOCK_COLOR = Color.YELLOW;

	private static final int SQUARE_EDGE_WIDTH = 2;

	public MenuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		_menuPaint = new Paint();
		_backgroundPaint = new Paint();
		_currentBlockPoints = new HashSet<Point>();
		// the initial position is in center
		int middLeft = MENU_MATRIX_EDGE / 2 - 1;
		int middRight = middLeft + 1;
		_currentBlockPoints.add(new Point(middLeft, middLeft));
		_currentBlockPoints.add(new Point(middLeft, middRight));
		_currentBlockPoints.add(new Point(middRight, middRight));
		_currentBlockPoints.add(new Point(middRight, middLeft));
		_backList = new ArrayList<Point>();
	}

	// record the size of the current view - note this is not full screen!
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		_viewWidth = w;
		_viewHeight = h;
		_edgeLength = _viewWidth / MENU_MATRIX_EDGE;
	}

	private void drawAll() {
		drawMenuBackground();
		drawMenuBlocks();
	}

	private void drawMenuBackground() {
		Canvas canvas = null;
		try {
			canvas = _holder.lockCanvas();
			synchronized (_holder) {
				_backgroundPaint.setColor(BACKGROUND_COLOR);
				canvas.drawRect(0, 0, _viewWidth, _viewHeight, _backgroundPaint);
			}
		} finally {
			if (canvas != null) {
				_holder.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void drawMenuBlocks() {
		Canvas canvas = null;
		try {
			canvas = _holder.lockCanvas();
			Point drawingP = new Point(0, 0);
			Point setP = new Point();
			synchronized (_holder) {
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
			drawAll();
		}

		return succeed;
	}
}

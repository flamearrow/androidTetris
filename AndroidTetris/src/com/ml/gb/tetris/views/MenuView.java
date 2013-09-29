package com.ml.gb.tetris.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * A Tetris flavor menu providing user the ability to choose
 * newgame/highscore/tutorial/exit
 * 
 * @author flamearrow
 * 
 */
public class MenuView extends SurfaceView {
	private int _viewWidth;
	private int _viewHeight;

	private static final int MENU_MATRIX_EDGE = 16;
	private int[][] _menuMatrix;

	public MenuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		_menuMatrix = new int[MENU_MATRIX_EDGE][MENU_MATRIX_EDGE];
		
	}

	// record the size of the current view - note this is not full screen!
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		_viewWidth = w;
		_viewHeight = h;
	}
}

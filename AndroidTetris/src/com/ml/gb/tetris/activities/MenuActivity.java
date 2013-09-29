package com.ml.gb.tetris.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.ml.gb.R;
import com.ml.gb.tetris.listener.MenuGestureListener;
import com.ml.gb.tetris.views.MenuView;

public class MenuActivity extends Activity {
	private MenuView _menuView;
	private GestureDetector _gesDect;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		_menuView = (MenuView) findViewById(R.id.menuview);
		_gesDect = new GestureDetector(this, new MenuGestureListener(_menuView));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = MotionEventCompat.getActionMasked(event);
		if (action == MotionEvent.ACTION_UP) {
			Log.d("MenuActivity", "should detect location and reset");
			_menuView.checkPositionAndReset();
		}
		return _gesDect.onTouchEvent(event);
	}
}

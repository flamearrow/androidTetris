package com.ml.gb.tetris.activities;

import android.app.Activity;
import android.os.Bundle;

import com.ml.gb.R;
import com.ml.gb.tetris.views.MenuView;

public class MenuActivity extends Activity {
	private MenuView _mV;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		_mV = (MenuView) findViewById(R.id.menuview);
		_mV.setMenuActivity(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		_mV.clearToast();
	}
}

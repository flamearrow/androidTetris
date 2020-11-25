package com.ml.gb.tetris.activities;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.ml.gb.R;
import com.ml.gb.tetris.NameScorePair;
import com.ml.gb.tetris.TetrisConstants;

public class HighScoreListActivity extends Activity {
	private TableLayout _scoreListLayout;
	private SharedPreferences _highScores;

	private LinkedList<HighScoreEntry> _highScoreEntries;

	private class HighScoreEntry implements Comparable<HighScoreEntry> {
		int rank;
		NameScorePair nameScorePair;

		public HighScoreEntry(int argRank, NameScorePair argNSP) {
			rank = argRank;
			nameScorePair = argNSP;
		}

		@Override
		public int compareTo(HighScoreEntry another) {
			return another.rank - rank;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.high_score_list);
		_scoreListLayout = (TableLayout) findViewById(R.id.high_score_layout);
		_highScoreEntries = new LinkedList<HighScoreEntry>();

		// populate the scoreList
		_highScores = getSharedPreferences(
				TetrisConstants.TETRIS_SHAREDPREFENCES_NAME, MODE_PRIVATE);

		@SuppressWarnings("unchecked")
		Map<String, String> nameScoreMap = (Map<String, String>) _highScores
				.getAll();
		for (Map.Entry<String, String> entry : nameScoreMap.entrySet()) {
			int rank = Integer.parseInt(entry.getKey());
			String value = entry.getValue();
			int seperatorIndex = value
					.indexOf(TetrisConstants.NAME_SCORE_SEPERATOR);
			String name = value.substring(0, seperatorIndex);
			int score = Integer.parseInt(value.substring(seperatorIndex + 1));
			_highScoreEntries.add(new HighScoreEntry(rank, new NameScorePair(
					score, name)));
		}
		Collections.sort(_highScoreEntries);
		populateView();
	}

	private void populateView() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		for (HighScoreEntry e : _highScoreEntries) {
			TableRow newRow = (TableRow) inflater.inflate(
					R.layout.new_high_score_entry, null);
			((TextView) (newRow.findViewById(R.id.newEntryName)))
					.setText(e.nameScorePair.getName());

			((TextView) (newRow.findViewById(R.id.newEntryRank))).setText(""
					+ e.rank);

			((TextView) (newRow.findViewById(R.id.newEntryScore))).setText(""
					+ e.nameScorePair.getScore());

			_scoreListLayout.addView(newRow, 1);
		}
	}

	public void clearHighScores(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				HighScoreListActivity.this);
		builder.setTitle(R.string.clear_confirm);
		builder.setPositiveButton(R.string.clear_high_score,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						_scoreListLayout.removeViews(1,
								_highScoreEntries.size());
						SharedPreferences.Editor highScoreEditor = _highScores
								.edit();
						highScoreEditor.clear();
						highScoreEditor.apply();

					}
				});
		builder.setMessage(R.string.clear_confirm_msg);
		builder.setCancelable(false);
		builder.setNegativeButton(R.string.cancel, null);
		builder.create().show();

	}
}

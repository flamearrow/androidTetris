package com.ml.gb.tetris;

public class NameScorePair implements Comparable<NameScorePair> {
	String name;
	int score;

	public String getName() {
		return name;
	}

	public int getScore() {
		return score;
	}

	public NameScorePair(int argScore, String argName) {
		score = argScore;
		name = argName;
	}

	@Override
	public int compareTo(NameScorePair another) {
		return score - another.score;
	}

	@Override
	public String toString() {
		return name + TetrisConstants.NAME_SCORE_SEPERATOR + score;
	}

}

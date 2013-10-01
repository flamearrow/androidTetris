package com.ml.gb.tetris.views;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ml.gb.R;
import com.ml.gb.tetris.NameScorePair;
import com.ml.gb.tetris.TetrisConstants;

/**
 * Tetris will be represented by a 18 * 10 color matrix
 * 
 * @author flamearrow
 * 
 */
public class TetrisView extends SurfaceView implements Callback {
	private Random rand = new Random();
	static final int EDGE_GAP_RATIO = 16;
	private static final int MATRIX_HEIGHT = 26;
	private static final int MATRIX_WIDTH = 12;
	private static final int PREVIEW_EDGE = 4;
	private static final double GOLDEN_RATIO = 0.618;
	private static final float SCORE_BAR_DELTA = 2.0f;
	// we try to refresh the screen every MIN_GRANULARITY milis
	private static final int MIN_GRANULARITY = 50;

	private static final int BACKGROUND_COLOR = Color.WHITE;
	private static final int FONT_COLOR = Color.BLACK;
	private static final int SQUARE_EDGE_COLOR = Color.WHITE;
	private static final int SEPARATOR_COLOR = Color.GRAY;
	private static final int SCORE_BAR_COLOR = Color.RED;
	private static final int INITIAL_BLOCK_COLOR = Color.WHITE;
	private static final int PREVIEW_DROPPED_BLOCK_COLOR = 0xFFEEEEEE;
	public static final int HIGH_SCORE_MAX_COUNT = 3;

	private static final String INVALID_NAME_WARNING = "ERROR!\nname can't be null and shouldn't contain \""
			+ TetrisConstants.NAME_SCORE_SEPERATOR + "\"";

	private TetrisThread _thread;

	// Android Color are all int!
	private int[][] _gameMatrix;
	private int[][] _previewMatrix;

	SurfaceHolder _holder;

	private int _screenWidth;
	private int _screenHeight;

	// Paint object is used to draw stuff
	private Paint _backgroundPaint;
	private Paint _gameMatrixPaint;
	private Paint _previewMatrixPaint;
	private Paint _separatorPaint;
	private Paint _statisticPaint;
	private Paint _scoreBarPaint;

	private int _level;
	private int _score;
	private int _scoreToLevelUp;
	// score to level up for each level is _level * multiplier
	private static final int SCORE_MULTIPLIER = 10;

	private Block _currentBlock;
	private Block _nextBlock;
	// _currentBlockPoints is used to represent the blocks occupied by current
	// dropping block
	private Set<Point> _currentBlockPoints;
	private Set<Point> _currentPreviewBlockPoints;
	private List<Point> _backList;

	private Point _upperLeft;

	// fastDropping is triggered by swiping down
	private boolean _isFastDropping;

	// repaint score bar when the score is changed
	private boolean _shouldRepaintScoreBar;
	private float _scoreBarCurrentLength;

	// when we want to move the current block by swipping/scrolling, the canvas
	// needs to be re drawn immediately
	private boolean _currentBlockMoved;

	private Animation shakeAnimation;

	// a squre's edge length, should accommodate with width
	// the total width is shared by gameSection width, previewSection with
	// and a separator
	private int _blockEdgeLength;
	private int _squareGapWidth;

	private Rect _scoreBarRect;
	private Rect _gameMatrixRect;

	private boolean _gameOver;

	private LinkedList<NameScorePair> _highScoreBuffer;
	private SharedPreferences _highScores;
	private boolean _newHighScore;

	public void setHighScores(SharedPreferences highScores) {
		this._highScores = highScores;
	}

	private int getRandomColor() {
		int ret = 0xFF000000;
		for (int i = 0; i < 30; i++) {
			ret |= (rand.nextInt(2) << i);
		}
		return ret;
	}

	public Block.Direction getRandomDirection() {
		switch (rand.nextInt(4)) {
		case 0:
			return Block.Direction.Down;
		case 1:
			return Block.Direction.Up;
		case 2:
			return Block.Direction.Left;
		case 3:
			return Block.Direction.Right;
		default:
			return null;
		}
	}

	/**
	 * @return a block with random value and color
	 */
	private Block getRandomBlock() {
		switch (rand.nextInt(7)) {
		case 0:
			return new Block(Block.Value.I, getRandomColor(),
					getRandomDirection());
		case 1:
			return new Block(Block.Value.L, getRandomColor(),
					getRandomDirection());
		case 2:
			return new Block(Block.Value.O, getRandomColor(),
					getRandomDirection());
		case 3:
			return new Block(Block.Value.rL, getRandomColor(),
					getRandomDirection());
		case 4:
			return new Block(Block.Value.rS, getRandomColor(),
					getRandomDirection());
		case 5:
			return new Block(Block.Value.S, getRandomColor(),
					getRandomDirection());
		case 6:
			return new Block(Block.Value.T, getRandomColor(),
					getRandomDirection());
		default:
			return null;
		}
	}

	// Note: customized View needs to implement this two param constructor pi
	public TetrisView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// this call is ensuring surfaceCreated() method will be called
		getHolder().addCallback(this);

		_gameMatrix = new int[MATRIX_HEIGHT][MATRIX_WIDTH];
		_previewMatrix = new int[PREVIEW_EDGE][PREVIEW_EDGE];
		_backgroundPaint = new Paint();
		_gameMatrixPaint = new Paint();
		_previewMatrixPaint = new Paint();
		_separatorPaint = new Paint();
		_statisticPaint = new Paint();
		_scoreBarPaint = new Paint();
		_scoreBarPaint.setColor(SCORE_BAR_COLOR);
		_currentBlockPoints = new HashSet<Point>();
		_currentPreviewBlockPoints = new HashSet<Point>();
		_backList = new LinkedList<Point>();
		_upperLeft = new Point(-1, -1);
		shakeAnimation = AnimationUtils.loadAnimation(context,
				R.anim.level_up_shake);
		shakeAnimation.setRepeatCount(3);

		_scoreBarRect = new Rect();
		_gameMatrixRect = new Rect();
		_highScoreBuffer = new LinkedList<NameScorePair>();
		_gameOver = false;

	}

	// pause the game, we want to save game state after returning to game
	// setting thread running to false actually kills the thread
	public void pause() {
		if (_thread != null) {
			_thread.setRunning(false);
		}
	}

	public void setFastDropping() {
		_isFastDropping = true;
	}

	public void releaseResources() {

	}

	private void updateComponents() {

		boolean addAnotherBlock = updateGameMatrix();
		if (addAnotherBlock) {
			_isFastDropping = false;
			boolean gameOver = addBlockToMatrix(_gameMatrix, MATRIX_HEIGHT - 1,
					4, _nextBlock, true);
			updateDroppedLocation();
			if (gameOver) {
				stopGame();
			}
			_currentBlock = _nextBlock;
			updatePreviewMatrix();
		}
		updateTimer();
		updateScoreBoard();
	}

	/**
	 * stop game, prompt for new game or not
	 */
	private void stopGame() {
		// first kill the rendering thread
		pause();
		_gameOver = true;
		// then prompt for newGame or back to menu
		((Activity) getContext()).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showGameOverDialog();
			}

		});
	}

	// first query smallest high score, if current score is higher that
	// then prompt with name field and add the current score
	// otherwise prompt without name field

	// name-value pairs are not unique items, need to map this:
	// String rank - Set<String> [name, value]
	private void showGameOverDialog() {
		@SuppressWarnings("unchecked")
		Map<String, String> nameScoreMap = (Map<String, String>) _highScores
				.getAll();
		_newHighScore = false;
		_highScoreBuffer.clear();
		for (String nameScorePair : nameScoreMap.values()) {
			int seperatorIndex = nameScorePair
					.indexOf(TetrisConstants.NAME_SCORE_SEPERATOR);
			String name = nameScorePair.substring(0, seperatorIndex);
			int score = Integer.parseInt(nameScorePair
					.substring(seperatorIndex + 1));
			_highScoreBuffer.add(new NameScorePair(score, name));
		}
		// if we haven't get enough high score add it anyway
		if (nameScoreMap.size() < HIGH_SCORE_MAX_COUNT) {
			_newHighScore = true;
		}
		// otherwise we replace the one with smallest score
		else {
			Collections.sort(_highScoreBuffer);
			// after sorting the first is smallest history score, if current
			// score is higher that that then remove the first and add the
			// current later
			if (_score > _highScoreBuffer.getFirst().getScore()) {
				_highScoreBuffer.removeFirst();
				_newHighScore = true;
			}
		}

		Log.d("TetrisView", nameScoreMap.toString());

		LayoutInflater inflater = (LayoutInflater) ((Activity) getContext())
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View gameOverView = _newHighScore ? inflater.inflate(
				R.layout.new_high_score, null) : inflater.inflate(
				R.layout.game_over, null);
		TextView scoreText = (TextView) gameOverView
				.findViewById(R.id.game_over_score_id);
		scoreText.setText("" + _score);

		final Dialog gameOverDialog = new Dialog(getContext());
		gameOverDialog.setContentView(gameOverView);
		gameOverDialog.setCancelable(false);
		gameOverDialog.setTitle(getResources().getString(R.string.game_over));
		gameOverDialog.show();
		((Button) (gameOverView.findViewById(R.id.restart_button)))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (tryUpdateHighScore(gameOverView)) {
							gameOverDialog.dismiss();
							newGame();
						} else {
							Toast t = Toast.makeText(getContext(),
									INVALID_NAME_WARNING, Toast.LENGTH_SHORT);
							t.setGravity(Gravity.TOP, 0, _screenHeight / 4);
							t.show();
						}
					}
				});

		((Button) (gameOverView.findViewById(R.id.back_button)))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (tryUpdateHighScore(gameOverView)) {
							gameOverDialog.dismiss();
							((Activity) getContext()).finish();
						} else {
							Toast t = Toast.makeText(getContext(),
									INVALID_NAME_WARNING, Toast.LENGTH_SHORT);
							t.setGravity(Gravity.TOP, 0, _screenHeight / 4);
							t.show();
						}
					}
				});
		gameOverDialog.show();

	}

	/**
	 * if need to add high score, then gameOverView should contain
	 * high_score_name, check if it contains SEPERATOR, if it does then don't do
	 * anything
	 * 
	 * @param gameOverView
	 * @return whether the caller dialog should be dismissed
	 */
	private boolean tryUpdateHighScore(View gameOverView) {
		if (_newHighScore) {
			String newName = ((EditText) (gameOverView
					.findViewById(R.id.high_score_name))).getText().toString();
			if (newName.contains(TetrisConstants.NAME_SCORE_SEPERATOR)
					|| newName.length() == 0) {
				return false;
			} else {
				SharedPreferences.Editor highScoreEditor = _highScores.edit();
				highScoreEditor.clear();
				_highScoreBuffer.add(new NameScorePair(_score, newName));
				Collections.sort(_highScoreBuffer);
				int rank = _highScoreBuffer.size();
				for (NameScorePair nsp : _highScoreBuffer) {
					highScoreEditor.putString("" + rank--, nsp.toString());
				}
				highScoreEditor.apply();
				return true;
			}
		} else
			return true;

	}

	/**
	 * First dropping the floating block one line if applicable
	 * 
	 * then remove all full lines
	 * 
	 * @return whether we need to add another block to the game
	 */
	private boolean updateGameMatrix() {
		// first move the floating block one line down
		boolean droppedOneLine = moveCurrentBlock(new Point(-1, 0));

		// we only remove line when a block hits ground
		if (!droppedOneLine) {
			// then check if we need to remove lines from bottom to
			// _currentHeight
			int rowRemoved = 0;
			int currentRow = 0;
			int currentBottom = Integer.MAX_VALUE;
			here: while (currentRow < MATRIX_HEIGHT) {
				for (int i = 0; i < MATRIX_WIDTH; i++) {
					if (_gameMatrix[currentRow][i] == INITIAL_BLOCK_COLOR) {
						// if this line can't be removed, we either skip it or
						// drop
						// it to currentBottom

						// currentBottom is not empty, need to drop
						if (currentBottom != Integer.MAX_VALUE) {
							moveRow(_gameMatrix, currentRow, currentBottom);
							currentBottom++;
						}
						currentRow++;
						continue here;
					}
				}
				// if we can reach here then currentRow needs to be removed
				if (currentBottom > currentRow)
					currentBottom = currentRow;
				// clear the row
				clearRow(_gameMatrix, currentRow);
				rowRemoved++;
				currentRow++;
			}
			switch (rowRemoved) {
			case 1:
				_shouldRepaintScoreBar = true;
				_score += 1;
				break;
			case 2:
				_shouldRepaintScoreBar = true;
				_score += 3;
				break;
			case 3:
				_shouldRepaintScoreBar = true;
				_score += 5;
				break;
			case 4:
				_shouldRepaintScoreBar = true;
				_score += 7;
				break;
			default:
				break;
			}

			if (_score >= _scoreToLevelUp) {
				_level++;
				_scoreToLevelUp = _level * SCORE_MULTIPLIER;
			}

		}

		return !droppedOneLine;
	}

	// move the currentRow to the currentBottom row of the matrix, clear
	// currentRow
	private void moveRow(int[][] matrix, int currentRow, int currentBottom) {
		for (int i = 0; i < MATRIX_WIDTH; i++) {
			matrix[currentBottom][i] = matrix[currentRow][i];
			matrix[currentRow][i] = INITIAL_BLOCK_COLOR;
		}
	}

	private void clearRow(int[][] matrix, int rowToClear) {
		for (int i = 0; i < MATRIX_WIDTH; i++) {
			matrix[rowToClear][i] = INITIAL_BLOCK_COLOR;
		}
	}

	private void updatePreviewMatrix() {
		for (int i = 0; i < PREVIEW_EDGE; i++)
			for (int j = 0; j < PREVIEW_EDGE; j++)
				_previewMatrix[i][j] = INITIAL_BLOCK_COLOR;
		_nextBlock = getRandomBlock();
		addBlockToMatrix(_previewMatrix, PREVIEW_EDGE - 1, 0, _nextBlock, false);
	}

	/**
	 * add a new block to specified color matrix, if there's no space for the
	 * given block, return false
	 * 
	 * @param colorMatrix
	 * @param x
	 * @param y
	 * @param newBlock
	 * @return whether game is over
	 * @return updateUpperLeft whether to update the upperLeft point denoting
	 *         the current floating block
	 */
	private boolean addBlockToMatrix(int[][] colorMatrix, int x, int y,
			Block newBlock, boolean updateUpperLeft) {
		// with hex value defined, just apply it to a 4*4 matrix
		int count = 0;
		int value = newBlock.getHexValue();
		int color = newBlock.color;

		// apply the value to colorMatrix
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				// we find a collision, game over
				if (colorMatrix[x - i][y + j] != INITIAL_BLOCK_COLOR) {
					return true;
				} else if (((value >> (count++)) & 1) > 0) {
					colorMatrix[x - i][y + j] = color;
				}

			}
		}
		if (updateUpperLeft) {
			_upperLeft.set(x, y);
			updateCurrentBlockPoints();
		}
		return false;
	}

	private void updateTimer() {

	}

	private void updateScoreBoard() {

	}

	private void drawComponents(Canvas canvas) {
		drawBackgroundAndSeperator(canvas);

		// first draw the game section
		drawGameBlocks(canvas);

		// then draw the left-right separator
		drawPreviewBlocks(canvas);

		// then draw Level and score
		drawScoreAndLevel(canvas);

		// then draw scorebarLength
		_shouldRepaintScoreBar = !drawScoreBarGradually(canvas);
	}

	// background and Separator are not always updated
	private void drawBackgroundAndSeperator(Canvas canvas) {
		_backgroundPaint.setColor(BACKGROUND_COLOR);
		canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
				_backgroundPaint);

		// draw left-right separator
		Point currentPoint = new Point(MATRIX_WIDTH * _blockEdgeLength
				+ _blockEdgeLength / 2, 0);
		_separatorPaint.setColor(SEPARATOR_COLOR);
		_separatorPaint.setStrokeWidth(_blockEdgeLength / 2);
		canvas.drawLine(currentPoint.x, 0, currentPoint.x, _screenHeight,
				_separatorPaint);

		// then draw the top-bottom separator, should separate it in golden
		// ratio
		currentPoint.offset(0, (int) (_screenHeight * (1 - GOLDEN_RATIO)));
		canvas.drawLine(currentPoint.x, currentPoint.y, _screenWidth,
				currentPoint.y, _separatorPaint);

	}

	private void drawGameBlocks(Canvas canvas) {
		// starting from bottom left, draw a MATRIX_HEIGHT x MATRIX_WIDTH matrix
		// each block has a black frame and filled with color at
		// _colorMatrix[i][j]
		Point currentPoint = new Point(0, _screenHeight);
		// for (int i = MATRIX_HEIGHT - 1; i >= 0; i--) {
		for (int i = 0; i < MATRIX_HEIGHT - 1; i++) {
			for (int j = 0; j < MATRIX_WIDTH; j++) {
				// draw edge
				_gameMatrixPaint.setColor(SQUARE_EDGE_COLOR);
				canvas.drawRect(currentPoint.x, currentPoint.y
						- _blockEdgeLength, currentPoint.x + _blockEdgeLength,
						currentPoint.y, _gameMatrixPaint);
				// draw square
				_gameMatrixPaint.setColor(_gameMatrix[i][j]);
				canvas.drawRect(currentPoint.x + _squareGapWidth,
						currentPoint.y - _blockEdgeLength + _squareGapWidth,
						currentPoint.x + _blockEdgeLength - _squareGapWidth,
						currentPoint.y - _squareGapWidth, _gameMatrixPaint);
				currentPoint.offset(_blockEdgeLength, 0);
				// if we just drew a preview block, need to reset its color
				if (_gameMatrix[i][j] == PREVIEW_DROPPED_BLOCK_COLOR) {
					_gameMatrix[i][j] = INITIAL_BLOCK_COLOR;
				}

			}
			// move to the start of next line
			currentPoint.offset(-MATRIX_WIDTH * _blockEdgeLength,
					-_blockEdgeLength);
		}
	}

	// need to first clear the level and score area then draw text
	private void drawScoreAndLevel(Canvas canvas) {
		Point currentPoint = new Point((MATRIX_WIDTH + 1) * _blockEdgeLength,
				(int) (_screenHeight * (1 - GOLDEN_RATIO / 2)));
		_statisticPaint.setColor(BACKGROUND_COLOR);
		canvas.drawRect(currentPoint.x, currentPoint.y, currentPoint.x + 4
				* _blockEdgeLength, currentPoint.y + 5 * _blockEdgeLength,
				_statisticPaint);
		_statisticPaint.setColor(FONT_COLOR);
		currentPoint.offset(_blockEdgeLength, -2 * _blockEdgeLength);
		canvas.drawText(getResources().getString(R.string.level) + _level,
				currentPoint.x, currentPoint.y, _statisticPaint);
		currentPoint.offset(0, 4 * _blockEdgeLength);
		canvas.drawText(getResources().getString(R.string.score) + _score,
				currentPoint.x, currentPoint.y, _statisticPaint);
	}

	private void drawPreviewBlocks(Canvas canvas) {
		Point currentPoint = new Point(MATRIX_WIDTH * _blockEdgeLength
				+ _blockEdgeLength / 2, (int) (_screenHeight
				* (1 - GOLDEN_RATIO) / 2));
		currentPoint.offset(0, 3 * _blockEdgeLength);

		// then draw the preview section, should be in center of top part
		currentPoint.set(currentPoint.x + _blockEdgeLength / 2,
				currentPoint.y / 2);
		currentPoint.offset(0, -PREVIEW_EDGE / 2 * _blockEdgeLength);
		for (int i = PREVIEW_EDGE - 1; i >= 0; i--) {
			for (int j = 0; j < PREVIEW_EDGE; j++) {
				// draw edge
				_previewMatrixPaint.setColor(SQUARE_EDGE_COLOR);
				canvas.drawRect(currentPoint.x, currentPoint.y
						- _blockEdgeLength, currentPoint.x + _blockEdgeLength,
						currentPoint.y, _previewMatrixPaint);
				// draw square
				_previewMatrixPaint.setColor(_previewMatrix[i][j]);
				canvas.drawRect(currentPoint.x + _squareGapWidth,
						currentPoint.y - _blockEdgeLength + _squareGapWidth,
						currentPoint.x + _blockEdgeLength - _squareGapWidth,
						currentPoint.y - _squareGapWidth, _previewMatrixPaint);
				currentPoint.offset(_blockEdgeLength, 0);
			}
			// move to the start of next line
			currentPoint.offset(-PREVIEW_EDGE * _blockEdgeLength,
					_blockEdgeLength);
		}
	}

	public void shake() {
		this.startAnimation(shakeAnimation);
	}

	/**
	 * each time draw a SCORE_BAR_DELTA till we draw the length we need
	 * 
	 * @param canvas
	 * @return done or not
	 */
	private boolean drawScoreBarGradually(Canvas canvas) {
		// calculate the x coordinate of score bar
		int scoreBarX = MATRIX_WIDTH * _blockEdgeLength + _blockEdgeLength / 8;
		float scoreBarLength = (_score - (_level - 1) * SCORE_MULTIPLIER)
				* _screenHeight / _scoreToLevelUp;

		// we just draw all the way up to _screenHeight, need to clear the score
		// bar
		if (_scoreBarCurrentLength == _screenHeight) {
			_scoreBarCurrentLength = 0;
			_scoreBarPaint.setColor(BACKGROUND_COLOR);
			canvas.drawLine(scoreBarX, _screenHeight, scoreBarX, 0,
					_scoreBarPaint);
			_scoreBarPaint.setColor(SCORE_BAR_COLOR);
			return false;
		}
		// we have finished drawing
		else if (_scoreBarCurrentLength == scoreBarLength) {
			canvas.drawLine(scoreBarX, _screenHeight, scoreBarX, _screenHeight
					- _scoreBarCurrentLength, _scoreBarPaint);
			_scoreBarPaint.setColor(BACKGROUND_COLOR);
			canvas.drawLine(scoreBarX, _screenHeight - _scoreBarCurrentLength,
					scoreBarX, 0, _scoreBarPaint);
			_scoreBarPaint.setColor(SCORE_BAR_COLOR);
			return true;
		}
		// should level up, first draw all the way to screen top then start from
		// zero
		else if (_scoreBarCurrentLength > scoreBarLength) {
			if (_scoreBarCurrentLength + SCORE_BAR_DELTA < _screenHeight) {
				_scoreBarCurrentLength += SCORE_BAR_DELTA;
			} else {
				_scoreBarCurrentLength = _screenHeight;
			}
		} else if (_scoreBarCurrentLength + SCORE_BAR_DELTA < scoreBarLength) {
			_scoreBarCurrentLength += SCORE_BAR_DELTA;
		} else {
			_scoreBarCurrentLength = scoreBarLength;
		}
		canvas.drawLine(scoreBarX, _screenHeight, scoreBarX, _screenHeight
				- _scoreBarCurrentLength, _scoreBarPaint);
		_scoreBarPaint.setColor(BACKGROUND_COLOR);
		canvas.drawLine(scoreBarX, _screenHeight - _scoreBarCurrentLength,
				scoreBarX, 0, _scoreBarPaint);
		_scoreBarPaint.setColor(SCORE_BAR_COLOR);
		return false;
	}

	// called when first added to the View, used to record screen width and
	// height
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		_screenHeight = h;
		_screenWidth = w;
		_blockEdgeLength = _screenWidth / (MATRIX_WIDTH + PREVIEW_EDGE + 1);

		_squareGapWidth = _blockEdgeLength / EDGE_GAP_RATIO;
		_scoreBarPaint.setStrokeWidth(_blockEdgeLength / 4);
		float scoreBarRight = _blockEdgeLength / 4 + MATRIX_WIDTH
				* _blockEdgeLength;
		_scoreBarRect.set(MATRIX_WIDTH * _blockEdgeLength, 0,
				(int) scoreBarRight, _screenHeight);
		_gameMatrixRect.set(0, 0, MATRIX_WIDTH * _blockEdgeLength,
				_screenHeight);
		newGame();
	}

	/**
	 * check if the current Block contained in _currentBlockPoints could be
	 * applied to the delta
	 * 
	 * @param delta
	 * @return succeed
	 */
	private boolean moveCurrentBlock(Point delta) {
		synchronized (_holder) {
			boolean succeed = true;
			Point tmpPoint = new Point();
			for (Point p : _currentBlockPoints) {
				tmpPoint.set(p.x + delta.x, p.y + delta.y);
				if (_currentBlockPoints.contains(tmpPoint)) {
					continue;
				}
				// if we hit boundary or the new block is already set then we
				// can't
				// continue dropping
				if ((tmpPoint.x < 0 || tmpPoint.y < 0 || tmpPoint.y >= MATRIX_WIDTH)
						|| (_gameMatrix[tmpPoint.x][tmpPoint.y] != INITIAL_BLOCK_COLOR)) {
					succeed = false;
					break;
				}
			}

			if (succeed) {
				_backList.clear();
				for (Point p : _currentBlockPoints) {
					_gameMatrix[p.x][p.y] = INITIAL_BLOCK_COLOR;
					p.offset(delta.x, delta.y);
				}
				for (Point p : _currentBlockPoints) {
					_gameMatrix[p.x][p.y] = _currentBlock.color;
					_backList.add(p);
				}
				// important: we need to update Points index in the hashset
				_currentBlockPoints.clear();
				_currentBlockPoints.addAll(_backList);

				_upperLeft.offset(delta.x, delta.y);

			}
			updateDroppedLocation();

			return succeed;
		}
	}

	public void updateDroppedLocation() {
		Point tmpPoint = new Point();
		int probDeltaX = 0;
		here: while (true) {
			for (Point p : _currentBlockPoints) {
				tmpPoint.set(p.x - probDeltaX, p.y);
				// this point is occupied by the dropping block
				if (_currentBlockPoints.contains(tmpPoint))
					continue;
				// if the current block already hit bottom, we will
				// increment an additional line
				// if we hit bottom boundary then stop probing
				if (p.x - probDeltaX < 0
						|| ((_gameMatrix[p.x - probDeltaX][p.y] != INITIAL_BLOCK_COLOR) && (_gameMatrix[p.x
								- probDeltaX][p.y] != PREVIEW_DROPPED_BLOCK_COLOR))) {
					probDeltaX--;
					break here;
				}
			}
			// otherwise continue probing down
			probDeltaX++;
		}
		// now prbDeltaY is in place, need to set the preview block
		// points, we need to first clear the previous preview block points
		for (Point p : _currentPreviewBlockPoints) {
			if (_gameMatrix[p.x][p.y] == PREVIEW_DROPPED_BLOCK_COLOR)
				_gameMatrix[p.x][p.y] = INITIAL_BLOCK_COLOR;
		}
		_currentPreviewBlockPoints.clear();
		for (Point p : _currentBlockPoints) {
			tmpPoint.set(p.x - probDeltaX, p.y);
			// if currentBlock overlaps with previewPoint, draw
			// curentBlock
			if (_currentBlockPoints.contains(tmpPoint))
				continue;
			// otherwise set this block to preview color
			_currentPreviewBlockPoints.add(new Point(p.x - probDeltaX, p.y));
			_gameMatrix[tmpPoint.x][tmpPoint.y] = PREVIEW_DROPPED_BLOCK_COLOR;
		}
	}

	// move the current active block to left/right if according whether this tap
	// happens at left/right half of the screen
	// ideally we should first check if it's eligible to move then set
	// _shouldReDrawComponents = true
	public void moveLeft() {
		if (!_isFastDropping) {
			_currentBlockMoved = moveCurrentBlock(new Point(0, -1));
		}
	}

	public void moveRight() {
		if (!_isFastDropping) {
			_currentBlockMoved = moveCurrentBlock(new Point(0, 1));
		}
	}

	/**
	 * rotate the current active block
	 * 
	 * @return succeed or not
	 */
	public boolean rotate() {
		if (!_isFastDropping) {
			_currentBlock.rotate();

			int count = 0;
			int value = _currentBlock.getHexValue();
			int color = _currentBlock.color;
			Point tmpP = new Point(-1, -1);

			for (int i = _upperLeft.x; i > _upperLeft.x - 4; i--) {
				for (int j = _upperLeft.y; j < _upperLeft.y + 4; j++) {
					if (((value >> (count++)) & 1) > 0) {
						tmpP.set(i, j);
						// if it's outside of screen or it's already occupied
						// then
						// we can't rotate

						if (j < 0
								|| j >= MATRIX_WIDTH
								|| (!_currentBlockPoints.contains(tmpP) && _gameMatrix[i][j] != INITIAL_BLOCK_COLOR)) {
							_currentBlock.rRotate();
							return false;
						}
					}
				}
			}

			// safe to update now, needs to be reDrawn immediately
			for (Point p : _currentBlockPoints) {
				_gameMatrix[p.x][p.y] = INITIAL_BLOCK_COLOR;
			}

			_currentBlockPoints.clear();
			count = 0;
			for (int i = _upperLeft.x; i > _upperLeft.x - 4; i--) {
				for (int j = _upperLeft.y; j < _upperLeft.y + 4; j++) {
					if (((value >> (count++)) & 1) > 0) {
						_gameMatrix[i][j] = color;
						_currentBlockPoints.add(new Point(i, j));
					}
				}
			}
			updateDroppedLocation();
			_currentBlockMoved = true;

			return true;
		} else
			return false;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		_holder = holder;
		// once the thread is setRunning(false) the for loop will end the
		// thread, therefore we need to create a new thread

		_thread = new TetrisThread(holder);
		_thread.setRunning(true);
		_thread.start();
	}

	private void newGame() {
		initializeParams();
		initializeMatrix();
		if (_gameOver) {
			_gameOver = false;
			_thread = new TetrisThread(getHolder());
			_thread.setRunning(true);
			_thread.start();
		}
	}

	private void initializeParams() {
		_level = 1;
		_score = 0;
		_scoreToLevelUp = _level * SCORE_MULTIPLIER;
		_isFastDropping = false;
		_currentBlockMoved = false;
		_shouldRepaintScoreBar = false;
		_scoreBarCurrentLength = 0;
	}

	// this is called when a new game is started, add a random block in
	// _gameMatrix and a random block in _previewMatrix
	private void initializeMatrix() {
		for (int i = 0; i < MATRIX_HEIGHT; i++)
			for (int j = 0; j < MATRIX_WIDTH; j++)
				_gameMatrix[i][j] = INITIAL_BLOCK_COLOR;
		for (int i = 0; i < PREVIEW_EDGE; i++)
			for (int j = 0; j < PREVIEW_EDGE; j++)
				_previewMatrix[i][j] = INITIAL_BLOCK_COLOR;

		_currentBlock = getRandomBlock();
		addBlockToMatrix(_gameMatrix, MATRIX_HEIGHT - 1, 4, _currentBlock, true);

		updateCurrentBlockPoints();
		updateDroppedLocation();

		_nextBlock = getRandomBlock();
		addBlockToMatrix(_previewMatrix, PREVIEW_EDGE - 1, 0, _nextBlock, false);
	}

	/**
	 * add four points to the current Blocks
	 */
	private void updateCurrentBlockPoints() {
		_currentBlockPoints.clear();
		for (int i = MATRIX_HEIGHT - 1; i > MATRIX_HEIGHT - 5; i--) {
			for (int j = 0; j < MATRIX_WIDTH; j++) {
				if (_gameMatrix[i][j] != INITIAL_BLOCK_COLOR) {
					_currentBlockPoints.add(new Point(i, j));
				}
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// Nothing
	}

	// This is called when you exit the game, should stop the thread. Otherwise
	// it will continue trying to draw on a Null canvas and cause NPE
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		_thread.setRunning(false);
		boolean retry = true;
		while (retry) {
			try {
				_thread.join();
				retry = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class TetrisThread extends Thread {
		// _holder is used to retrieve Canvas object of the current SurfaceView
		private SurfaceHolder _myHolder;
		private boolean _running;
		private boolean _justStart;

		public TetrisThread(SurfaceHolder holder) {
			_myHolder = holder;
			_justStart = true;
		}

		public void setRunning(boolean running) {
			_running = running;
		}

		@Override
		public void run() {
			Canvas canvas = null;
			long previousFrameTime = System.currentTimeMillis();
			while (_running) {
				long currentFrameTime = System.currentTimeMillis();
				long elapsed = currentFrameTime - previousFrameTime;

				// finest granularity to update is 100 milis
				if (elapsed < MIN_GRANULARITY)
					continue;

				// if the thread is just brought to front/game just started, we
				// need to redraw the entire screen
				// if the entire screen is not redrawn but the scoreBar redraw
				// is called, the dirty rect will be set to entire screen by
				// _myHolder.lockCanvas(_scoreBarRect)-
				// because all other parts are not drawn yet, the entire screen
				// is considered 'dirty'
				if (_justStart) {
					try {
						canvas = _myHolder.lockCanvas(null);
						synchronized (_myHolder) {
							updateDroppedLocation();
							drawComponents(canvas);
							_justStart = false;
						}
					} finally {
						if (canvas != null) {
							_myHolder.unlockCanvasAndPost(canvas);
						}
					}
				}

				// if we are moving the block, then we need to reDraw
				// immediately (within the next 100 mili time window)
				if (_currentBlockMoved) {
					try {
						canvas = _myHolder.lockCanvas(_gameMatrixRect);
						synchronized (_myHolder) {
							drawGameBlocks(canvas);
						}
					} finally {
						if (canvas != null) {
							_myHolder.unlockCanvasAndPost(canvas);
						}
						_currentBlockMoved = false;
					}
				}

				if (_shouldRepaintScoreBar) {
					try {
						canvas = _myHolder.lockCanvas(_scoreBarRect);
						synchronized (_myHolder) {
							// if we are not done then we should continue
							// drawing
							_shouldRepaintScoreBar = !drawScoreBarGradually(canvas);
						}
					} finally {
						if (canvas != null) {
							_myHolder.unlockCanvasAndPost(canvas);
						}
					}
				}

				// normal dropping, should wait for enough time span to draw
				// if it's fast dropping we want to update every 100 milis
				if ((elapsed < 1000 - _level * 50) && !_isFastDropping) {
					continue;
				}
				try {
					canvas = _myHolder.lockCanvas(null);
					synchronized (_myHolder) {
						previousFrameTime = currentFrameTime;
						updateComponents();
						drawComponents(canvas);
					}
				} finally {
					if (canvas != null) {
						_myHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}
	}
}

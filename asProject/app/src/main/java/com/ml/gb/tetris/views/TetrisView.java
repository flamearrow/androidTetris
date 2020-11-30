package com.ml.gb.tetris.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Tetris will be represented by a 18 * 10 color matrix
 *
 * @author flamearrow
 */
public class TetrisView extends SurfaceView implements Callback {
    private final Random rand = new Random();
    static final int EDGE_GAP_RATIO = 16;
    private static final int MATRIX_HEIGHT = 26;
    private static final int MATRIX_WIDTH = 12;
    private static final int PREVIEW_EDGE = 4;
    private static final double GOLDEN_RATIO = 0.618;
    private static final float SCORE_BAR_DELTA = 2.0f;

    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int FONT_COLOR = Color.BLACK;
    private static final int FONT_SIZE = 25;
    private static final int SEPARATOR_COLOR = Color.GRAY;
    private static final int SCORE_BAR_COLOR = Color.RED;
    private static final int INITIAL_BLOCK_COLOR = Color.WHITE;
    private static final int PREVIEW_DROPPED_BLOCK_COLOR = 0xFFEEEEEE;
    public static final int HIGH_SCORE_MAX_COUNT = 10;

    private static final int BASE_SPEED = 1000;
    private static final int FAST_DROP_SPEED = 50;
    private static final int SPEED_MULTIPLIER = 130;

    private static final String INVALID_NAME_WARNING = "ERROR!\nname can't be null and shouldn't " +
            "contain \""
            + TetrisConstants.NAME_SCORE_SEPERATOR + "\"";

    // Android Color are all int!
    private final int[][] _gameMatrix;
    private final int[][] _previewMatrix;

    SurfaceHolder _holder;

    private int _screenWidth;
    private int _screenHeight;

    // Paint object is used to draw stuff
    private final Paint _backgroundPaint = new Paint();
    private final Paint _gameMatrixPaint = new Paint();
    private final Paint _previewMatrixPaint = new Paint();
    private final Paint _separatorPaint = new Paint();
    private final Paint _statisticPaint = new Paint();
    private final Paint _scoreBarPaint = new Paint();

    private int _level;
    private int _score;
    private int _scoreToLevelUp;
    // score to level up for each level is _level * multiplier
    private static final int SCORE_MULTIPLIER = 10;

    private Block _currentBlock;
    private Block _nextBlock;
    // _currentBlockPoints is used to represent the blocks occupied by current
    // dropping block
    private final Set<Point> _currentBlockPoints;
    private final Set<Point> _currentPreviewBlockPoints;
    private final List<Point> _backList;

    private final Point _upperLeft;

    private float _scoreBarCurrentLength;

    private final Animation shakeAnimation;

    // a squre's edge length, should accommodate with width
    // the total width is shared by gameSection width, previewSection with
    // and a separator
    private int _blockEdgeLength;
    private int _separatorWidth;
    private int _squareGapWidth;

    private final Rect _scoreBarRect = new Rect();
    private final Rect _gameMatrixRect = new Rect();
    private final Rect _scoreLevelRect = new Rect();
    private final Rect _previewRect = new Rect();

    private final LinkedList<NameScorePair> _highScoreBuffer = new LinkedList<>();
    private SharedPreferences _highScores;
    private boolean _newHighScore;

    private int _scoreBarX;

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
                return Block.DEFAULT_BLOCK;
        }
    }

    TetrisHandler tetrisHandler;

    // Note: customized View needs to implement this two param constructor pi
    public TetrisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // this call is ensuring surfaceCreated() method will be called
        getHolder().addCallback(this);

        _gameMatrix = new int[MATRIX_HEIGHT][MATRIX_WIDTH];
        _previewMatrix = new int[PREVIEW_EDGE][PREVIEW_EDGE];
        _statisticPaint.setTextSize(FONT_SIZE);
        _statisticPaint.setTypeface(Typeface.create("SERIF", Typeface.BOLD));
        _scoreBarPaint.setColor(SCORE_BAR_COLOR);
        _currentBlockPoints = new HashSet<>();
        _currentPreviewBlockPoints = new HashSet<>();
        _backList = new LinkedList<>();
        _upperLeft = new Point(-1, -1);
        shakeAnimation = AnimationUtils.loadAnimation(context,
                R.anim.level_up_shake);
        shakeAnimation.setRepeatCount(3);

        // initialize handler
        HandlerThread handlerThread = new HandlerThread(TetrisHandler.HANDLER_THREAD_NAME);
        handlerThread.start();
        tetrisHandler = new TetrisHandler(handlerThread.getLooper());

    }

    public void pauseHandler() {
        tetrisHandler.pause();
    }

    /**
     * stop game, prompt for new game or not
     */
    private void stopGame() {
        // then prompt for newGame or back to menu
        ((Activity) getContext()).runOnUiThread(this::showGameOverDialog);
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
                .setOnClickListener((OnClickListener) v -> {
                    if (tryUpdateHighScore(gameOverView)) {
                        gameOverDialog.dismiss();
                        tetrisHandler.startGame();
                    } else {
                        Toast t = Toast.makeText(getContext(),
                                INVALID_NAME_WARNING, Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.TOP, 0, _screenHeight / 4);
                        t.show();
                    }
                });

        ((Button) (gameOverView.findViewById(R.id.back_button)))
                .setOnClickListener((OnClickListener) v -> {
                    if (tryUpdateHighScore(gameOverView)) {
                        gameOverDialog.dismiss();
                        ((Activity) getContext()).finish();
                    } else {
                        Toast t = Toast.makeText(getContext(),
                                INVALID_NAME_WARNING, Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.TOP, 0, _screenHeight / 4);
                        t.show();
                    }
                });
        gameOverDialog.show();

    }

    /**
     * if need to add high score, then gameOverView should contain
     * high_score_name, check if it contains SEPERATOR, if it does then don't do
     * anything
     *
     * @param gameOverView view for the user to input game over information
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

    private boolean tryRemoveFullLinesAndUpdateScoreAndLevel() {
        // then check if we need to remove lines from bottom to
        // _currentHeight
        int rowRemoved = 0;
        int currentRow = 0;
        int currentBottom = Integer.MAX_VALUE;
        here:
        while (currentRow < MATRIX_HEIGHT) {
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
        boolean ret = false;
        switch (rowRemoved) {
            case 1:
                ret = true;
                _score += 1;
                break;
            case 2:
                ret = true;
                _score += 3;
                break;
            case 3:
                ret = true;
                _score += 5;
                break;
            case 4:
                ret = true;
                _score += 7;
                break;
            default:
                break;
        }

        if (_score >= _scoreToLevelUp) {
            _level++;
            _scoreToLevelUp += _level * SCORE_MULTIPLIER;
        }
        return ret;
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
     * @param colorMatrix target matrix
     * @param x           x coordinate of the target matrix
     * @param y           y coordinate of the target matrix
     * @param newBlock    block to add
     * @return updateUpperLeft whether to update the upperLeft point denoting
     * the current floating block
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
                if (colorMatrix[x - i][y + j] != INITIAL_BLOCK_COLOR
                        && colorMatrix[x - i][y + j] != PREVIEW_DROPPED_BLOCK_COLOR) {
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

    private void drawEverything(Canvas canvas) {
        drawBackgroundAndSeperator(canvas);

        // first draw the game section
        drawGameBlocks(canvas);

        // then draw the left-right separator
        drawPreviewBlocks(canvas);

        // then draw Level and score
        drawScoreAndLevel(canvas);

        // then draw scorebarLength
        drawScoreBarGradually(canvas);
    }

    private void drawEverything() {
        drawOnRect(null, this::drawEverything);
    }

    private interface DrawOnCanvasInterface {
        void draw(Canvas canvas);
    }

    private void drawOnRect(@Nullable Rect rect, DrawOnCanvasInterface drawOnCanvasInterface) {
        Canvas canvas = null;
        try {
            canvas = _holder.lockCanvas(rect);
            synchronized (_holder) {
                drawOnCanvasInterface.draw(canvas);
            }
        } finally {
            if (null != canvas) {
                _holder.unlockCanvasAndPost(canvas);
            }
        }
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
        _separatorPaint.setStrokeWidth(_separatorWidth);
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
                _gameMatrixPaint.setColor(BACKGROUND_COLOR);
                canvas.drawRect(currentPoint.x,
                        currentPoint.y - _blockEdgeLength,
                        currentPoint.x + _blockEdgeLength,
                        currentPoint.y, _gameMatrixPaint);
                // draw square
                _gameMatrixPaint.setColor(_gameMatrix[i][j]);
                canvas.drawRect(currentPoint.x + _squareGapWidth,
                        currentPoint.y - _blockEdgeLength + _squareGapWidth,
                        currentPoint.x + _blockEdgeLength - _squareGapWidth,
                        currentPoint.y - _squareGapWidth, _gameMatrixPaint);
                currentPoint.offset(_blockEdgeLength, 0);

            }
            // move to the start of next line
            currentPoint.offset(-MATRIX_WIDTH * _blockEdgeLength,
                    -_blockEdgeLength);
        }
    }

    private void drawGameBlocks() {
        drawOnRect(_gameMatrixRect, this::drawGameBlocks);
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

    // need to first clear the level and score area then draw text
    private void drawScoreAndLevel() {
        drawOnRect(_scoreLevelRect, this::drawScoreAndLevel);
    }

    private void drawPreviewBlocks(Canvas canvas) {
        _backgroundPaint.setColor(Color.BLACK);

        canvas.drawRect(_previewRect, _backgroundPaint);

        Point currentPoint = new Point((MATRIX_WIDTH + 1) * _blockEdgeLength,
                (int) (_screenHeight * (1 - GOLDEN_RATIO) / 2));

        for (int i = PREVIEW_EDGE - 1; i >= 0; i--) {
            for (int j = 0; j < PREVIEW_EDGE; j++) {
                // draw square
                _previewMatrixPaint.setColor(BACKGROUND_COLOR);
                canvas.drawRect(currentPoint.x,
                        currentPoint.y - _blockEdgeLength,
                        currentPoint.x + _blockEdgeLength,
                        currentPoint.y, _previewMatrixPaint);

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

    private void drawPreviewBlocks() {
        drawOnRect(_previewRect, this::drawPreviewBlocks);
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
        float levelScore = _score - ((_level - 1) * SCORE_MULTIPLIER * _level >> 1);
        float scoreBarLength = levelScore * _screenHeight
                / (_level * SCORE_MULTIPLIER);

        // we just draw all the way up to _screenHeight, need to clear the score
        // bar
        if (_scoreBarCurrentLength == _screenHeight) {
            _scoreBarCurrentLength = 0;
            _scoreBarPaint.setColor(BACKGROUND_COLOR);
            canvas.drawLine(_scoreBarX, _screenHeight, _scoreBarX, 0,
                    _scoreBarPaint);
            _scoreBarPaint.setColor(SCORE_BAR_COLOR);
            return false;
        }
        // we have finished drawing
        else if (_scoreBarCurrentLength == scoreBarLength) {
            canvas.drawLine(_scoreBarX, _screenHeight, _scoreBarX,
                    _screenHeight - _scoreBarCurrentLength, _scoreBarPaint);
            _scoreBarPaint.setColor(BACKGROUND_COLOR);
            canvas.drawLine(_scoreBarX, _screenHeight - _scoreBarCurrentLength,
                    _scoreBarX, 0, _scoreBarPaint);
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
        canvas.drawLine(_scoreBarX, _screenHeight, _scoreBarX, _screenHeight
                - _scoreBarCurrentLength, _scoreBarPaint);
        _scoreBarPaint.setColor(BACKGROUND_COLOR);
        canvas.drawLine(_scoreBarX, _screenHeight - _scoreBarCurrentLength,
                _scoreBarX, 0, _scoreBarPaint);
        _scoreBarPaint.setColor(SCORE_BAR_COLOR);
        return false;
    }

    private boolean drawScoreBar() {
        Canvas canvas = null;
        try {
            canvas = _holder.lockCanvas(_scoreBarRect);
            synchronized (_holder) {
                return drawScoreBarGradually(canvas);
            }
        } finally {
            if (null != canvas) {
                _holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // called when first added to the View, used to record screen width and
    // height
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("BGLM", "onSizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
        _screenHeight = h;
        _screenWidth = w;
        _blockEdgeLength = _screenWidth / (MATRIX_WIDTH + PREVIEW_EDGE + 1);
        _separatorWidth = _blockEdgeLength / 2;
        _scoreBarX = MATRIX_WIDTH * _blockEdgeLength + _blockEdgeLength / 8;
        _squareGapWidth = _blockEdgeLength / EDGE_GAP_RATIO;
        _scoreBarPaint.setStrokeWidth(_blockEdgeLength >> 2);
        float scoreBarRight = (_blockEdgeLength >> 2) + MATRIX_WIDTH
                * _blockEdgeLength;
        _scoreBarRect.set(MATRIX_WIDTH * _blockEdgeLength, 0,
                (int) scoreBarRight, _screenHeight);

        _gameMatrixRect.set(0, _screenHeight - (MATRIX_HEIGHT - 1) * _blockEdgeLength,
                MATRIX_WIDTH * _blockEdgeLength,
                _screenHeight);

        _previewRect.set(
                (MATRIX_WIDTH + 1) * _blockEdgeLength,
                (int) (_screenHeight * (1 - GOLDEN_RATIO) / 2 - _blockEdgeLength),
                (MATRIX_WIDTH + 1) * _blockEdgeLength + PREVIEW_EDGE * _blockEdgeLength,
                (int) (_screenHeight * (1 - GOLDEN_RATIO) / 2 - _blockEdgeLength) + PREVIEW_EDGE * _blockEdgeLength);

        _scoreLevelRect.set(_previewRect.left, _previewRect.bottom + _separatorWidth,
                _screenWidth, _screenHeight);
    }

    /**
     * check if the current Block contained in _currentBlockPoints could be
     * applied to the delta
     *
     * @return succeed
     */
    private boolean moveCurrentBlock(int x, int y) {
        boolean succeed = true;
        Point tmpPoint = new Point();
        for (Point p : _currentBlockPoints) {
            tmpPoint.set(p.x + x, p.y + y);
            if (_currentBlockPoints.contains(tmpPoint)) {
                continue;
            }
            // if we hit boundary or the new block is already set or the new
            // block is preview block then we
            // can't
            // continue dropping
            if ((tmpPoint.x < 0 || tmpPoint.y < 0 || tmpPoint.y >= MATRIX_WIDTH)
                    || ((_gameMatrix[tmpPoint.x][tmpPoint.y] != INITIAL_BLOCK_COLOR) && (_gameMatrix[tmpPoint.x][tmpPoint.y] != PREVIEW_DROPPED_BLOCK_COLOR))) {
                succeed = false;
                break;
            }
        }

        if (succeed) {
            _backList.clear();
            for (Point p : _currentBlockPoints) {
                _gameMatrix[p.x][p.y] = INITIAL_BLOCK_COLOR;
                p.offset(x, y);
            }
            for (Point p : _currentBlockPoints) {
                _gameMatrix[p.x][p.y] = _currentBlock.color;
                _backList.add(p);
            }
            // important: we need to update Points index in the hashset
            _currentBlockPoints.clear();
            _currentBlockPoints.addAll(_backList);

            _upperLeft.offset(x, y);

            // we only update DroppedLocation when moving left/right
            if (Math.abs(y) > 0)
                updateDroppedLocation();
        }

        return succeed;
    }

    public void updateDroppedLocation() {
        Point tmpPoint = new Point();
        int probDeltaX = 0;
        here:
        while (true) {
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

    /**
     * rotate the current active block
     *
     * @return succeed or not
     */
    public boolean rotate() {
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

        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        _holder = holder;
        if (!tetrisHandler.tryResume()) {
            tetrisHandler.startGame();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    private void newGame() {
        initializeParams();
        initializeMatrix();
    }

    private void initializeParams() {
        _level = 1;
        _score = 0;
        _scoreToLevelUp = _level * SCORE_MULTIPLIER;
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

    private long getCurrentDroppingSpeed() {
        return BASE_SPEED - _level * SPEED_MULTIPLIER;
    }

    public void requestRotate() {
        tetrisHandler.rotateBlock();
    }

    public void requestMoveLeft() {
        tetrisHandler.moveBlockLeft();
    }


    public void requestMoveRight() {
        tetrisHandler.moveBlockRight();
    }

    public void requestFastDrop() {
        tetrisHandler.fastDrop();
    }

    /**
     * This Handler runs off of main, no need to post UI events back to main as we're using
     * SurfaceView
     */
    private class TetrisHandler extends Handler {
        TetrisHandler(Looper looper) {
            super(looper);
        }

        private static final String HANDLER_THREAD_NAME = "TetrisHandlerThread";

        private static final int NEW_GAME = 0;
        private static final int BLOCK_DROP = 1;
        private final Object SLOW_OBJ = new Object();
        private final Object FAST_OBJ = new Object();
        private static final int BLOCK_MOVE_LEFT = 101;
        private static final int BLOCK_MOVE_RIGHT = 102;
        private static final int BLOCK_ROTATE = 103;
        private static final int PREVIEW = 200;
        private static final int SCORE_AND_LEVEL = 300;
        private static final int SCORE_BAR = 400;
        private static final int PAUSE = 600;
        private static final int RESUME = 601;

        public static final int PAUSED_WITH_SLOW_DROP = 0;
        public static final int PAUSED_WITH_FAST_DROP = 1;
        public static final int GAME_OVER = 2;
        private final Object GAME_OVER_OBJ = new Object();
        public static final int NOT_PAUSED = 3;

        private int pauseState = NOT_PAUSED;

        void startGame() {
            sendMessage(this.obtainMessage(NEW_GAME));
        }

        /**
         * Move block left asap
         */
        void moveBlockLeft() {
            sendEmptyMessage(BLOCK_MOVE_LEFT);
        }

        /**
         * Move block right asap
         */
        void moveBlockRight() {
            sendEmptyMessage(BLOCK_MOVE_RIGHT);

        }

        /**
         * Rotate block asap
         */
        void rotateBlock() {
            sendEmptyMessage(BLOCK_ROTATE);
        }

        /**
         * Start slow drop, keep dropping one step at {@link #getCurrentDroppingSpeed()}
         */
        void slowDrop() {
            sendMessageDelayed(this.obtainMessage(BLOCK_DROP, SLOW_OBJ), getCurrentDroppingSpeed());
        }

        /**
         * Start fast drop, keep dropping one step at {@link TetrisView#FAST_DROP_SPEED}
         */
        void fastDrop() {
            sendMessageDelayed(this.obtainMessage(BLOCK_DROP, FAST_OBJ), FAST_DROP_SPEED);
        }

        void pause() {
            if (pauseState == NOT_PAUSED) {
                pauseState = hasMessages(BLOCK_DROP, FAST_OBJ) ? PAUSED_WITH_FAST_DROP :
                        PAUSED_WITH_SLOW_DROP;
                sendEmptyMessage(PAUSE);
            }
        }

        // try resume the game if possible, returns if it's successfully resumed or not.
        // A game is resumable if the app is minimized and brought back to front.
        // A game is not resumable if it's just started
        // If it's already game over don't try resume
        boolean tryResume() {
            if (pauseState != NOT_PAUSED) {
                switch (pauseState) {
                    case PAUSED_WITH_FAST_DROP:
                        sendMessage(this.obtainMessage(RESUME, FAST_OBJ));
                        break;
                    case PAUSED_WITH_SLOW_DROP:
                        sendMessage(this.obtainMessage(RESUME, SLOW_OBJ));
                        break;
                    case GAME_OVER:
                        sendMessage(this.obtainMessage(RESUME, GAME_OVER_OBJ));
                        break;
                }
                pauseState = NOT_PAUSED;

                return true;
            } else {
                return false;
            }

        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case NEW_GAME:
                    Log.d("MLGB", "NEW_GAME");
                    // initalize gameMatrix, previewMatrix, statistics, backgroud, separator,
                    // draw
                    // everything
                    newGame(); // updateDroppedLocation called
                    drawEverything(); // TODO: don't draw scorebar? send separate messages
                    // instead?
                    slowDrop();
                    break;
                case BLOCK_ROTATE:
                    Log.d("MLGB", "BLOCK_ROTATE");
                    rotate();
                    drawGameBlocks();
                    break;
                case BLOCK_DROP:
                    Log.d("MLGB", "BLOCK_DROP");
                    boolean success = moveCurrentBlock(-1, 0);
                    // successfully dropped one line, the current block is still dropping, keep
                    // posting drop events
                    if (success) {
                        drawGameBlocks(); // TODO: only draw the dropped block part
                        if (msg.obj == SLOW_OBJ) {
                            slowDrop();
                        } else if (msg.obj == FAST_OBJ) {
                            // if we're fast dropping, remove all pending dropping messages and
                            // start drop fast
                            removeMessages(BLOCK_DROP);
                            fastDrop();
                        }
                    }
                    // the block reaches end, should clear full lines and decide if it's game
                    // over
                    else {
                        // clear full lines, decide if we should animate score bar
                        if (tryRemoveFullLinesAndUpdateScoreAndLevel()) {
                            sendMessage(this.obtainMessage(SCORE_BAR));
                        }

                        // try add a new block to drop to game matrix
                        boolean gameOver = addBlockToMatrix(_gameMatrix, MATRIX_HEIGHT - 1,
                                4, _nextBlock, true);
                        // calculate the dropping location of the new block
                        // TODO: merge this into addBlockToMatrix and inline addBlockToMatrix
                        //  call
                        updateDroppedLocation();

                        // assign _nextBLock to _currentBlock
                        _currentBlock = _nextBlock;
                        // generate a new _nextBlock
                        updatePreviewMatrix();

                        // draw game blocks, preview blocks and score and level
                        drawScoreAndLevel();
                        drawPreviewBlocks();
                        drawGameBlocks();

                        if (gameOver) {
                            // clear message in the looper?
                            pauseState = GAME_OVER;
                            stopGame();
                        } else {
                            // continue drop
                            slowDrop();
                        }

                    }
                    break;
                case BLOCK_MOVE_LEFT:
                    Log.d("MLGB", "BLOCK_MOVE_LEFT");
                    moveCurrentBlock(0, -1);
                    drawGameBlocks();
                    break;
                case BLOCK_MOVE_RIGHT:
                    Log.d("MLGB", "BLOCK_MOVE_RIGHT");
                    moveCurrentBlock(0, 1);
                    drawGameBlocks();
                    break;
                case PREVIEW: // TODO: remove?
                    // update preview data
                    updatePreviewMatrix();
                    drawPreviewBlocks();
                    break;
                case SCORE_AND_LEVEL: // TODO: remove?
                    // update score and level
                    drawScoreAndLevel();
                    break;
                case SCORE_BAR:
                    Log.d("MLGB", "SCORE_BAR");
                    // update scoreBar, keep drawing until we're done
                    if (!drawScoreBar()) {
                        sendMessage(this.obtainMessage(SCORE_BAR));
                    }
                    break;
                case PAUSE:
                    Log.d("MLGB", "PAUSE");
                    removeMessages(BLOCK_DROP);
                    break;
                case RESUME:
                    drawEverything(); // TODO: don't draw scorebar? send separate messages
                    if (msg.obj == FAST_OBJ) {
                        fastDrop();
                    } else if (msg.obj == SLOW_OBJ) {
                        slowDrop();
                    } // otherwise it's GAME_OVER_OBJ, don't do anything
                    Log.d("MLGB", "RESUME");
                    break;

            }
        }
    }
}

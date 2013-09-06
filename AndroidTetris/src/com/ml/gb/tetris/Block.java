package com.ml.gb.tetris;


/**
 * denotes different block shapes on Tetris game
 * 
 * @author flamearrow
 * 
 */
public class Block {
	// hex value for each letter:
	/*
	 * L: 
	 * 0x4460
	 * 0100
	 * 0100
	 * 0110
	 * 0000 
	 * 
	 * 0x0740
	 * 0000
	 * 0111
	 * 0100
	 * 0000
	 * 
	 * 0x6220
	 * 0110
	 * 0010
	 * 0010
	 * 0000
	 * 
	 * 0x02E0
	 * 0000
	 * 0010
	 * 1110
	 * 0000
	 * 
	 *
	 * rL: 
	 * 0x2260
	 * 0010
	 * 0010
	 * 0110
	 * 0000
	 * 
	 * 0x0470
	 * 0000
	 * 0100
	 * 0111
	 * 0000
	 * 
	 * 0x6440
	 * 0110
	 * 0100
	 * 0100
	 * 0000
	 * 
	 * 0x0E20
	 * 0000
	 * 1110
	 * 0010
	 * 0000
	 * 
	 * 
	 * S: 
	 * 0x0360
	 * 0000
	 * 0011
	 * 0110
	 * 0000
	 * 
	 * 0x4620
	 * 0100
	 * 0110
	 * 0010
	 * 0000
	 * 
	 * 
	 * rS: 
	 * 0x0C60
	 * 0000
	 * 1100
	 * 0110
	 * 0000
	 * 
	 * 0x2640
	 * 0010
	 * 0110
	 * 0100
	 * 0000
	 * 
	 * 
	 * T: 
	 * 0x0E40
	 * 0000
	 * 1110
	 * 0100
	 * 0000
	 * 
	 * 0x4C40
	 * 0100
	 * 1100
	 * 0100
	 * 0000
	 * 
	 * 0x4E00
	 * 0100
	 * 1110
	 * 0000
	 * 0000
	 * 
	 * 0x4640
	 * 0100
	 * 0110
	 * 0100
	 * 0000
	 * 
	 * O: 0x0660
	 * 0000
	 * 0110
	 * 0110
	 * 0000
	 * 
	 * I: 
	 * 0x4444
	 * 0100
	 * 0100
	 * 0100
	 * 0100
	 * 
	 * 0x0F00
	 * 0000
	 * 1111
	 * 0000
	 * 0000
	 */
	
	// define hexValues for each letter and direction
	public static final int L_UP = 0x4460;
	public static final int L_RIGHT = 0x0740;
	public static final int L_DOWN = 0x6220;
	public static final int L_LEFT = 0x02E0;
	public static final int rL_UP = 0x2260;
	public static final int rL_RIGHT = 0x0470;
	public static final int rL_DOWN = 0x6440;
	public static final int rL_LEFT = 0x0E20;
	public static final int S_UP = 0x0360;
	public static final int S_RIGHT = 0x4620;
	public static final int S_DOWN = 0x0360;
	public static final int S_LEFT = 0x4620;
	public static final int rS_UP = 0x0C60;
	public static final int rS_RIGHT = 0x2640;
	public static final int rS_DOWN = 0x0C60;
	public static final int rS_LEFT = 0x2640;
	public static final int T_UP = 0x0E40;
	public static final int T_RIGHT = 0x4C40;
	public static final int T_DOWN = 0x4E00;
	public static final int T_LEFT = 0x4640;
	public static final int O_UP = 0x0660;
	public static final int O_RIGHT = 0x0660;
	public static final int O_DOWN = 0x0660;
	public static final int O_LEFT = 0x0660;
	public static final int I_UP = 0x4444;
	public static final int I_RIGHT = 0x0F00;
	public static final int I_DOWN = 0x4444;
	public static final int I_LEFT = 0x0F00;

	public enum Value {
		L(L_UP, L_RIGHT, L_DOWN, L_LEFT), rL(rL_UP, rL_RIGHT, rL_DOWN, rL_LEFT), S(
				S_UP, S_RIGHT, S_DOWN, S_LEFT), rS(rS_UP, rS_RIGHT, rS_DOWN,
				rS_LEFT), T(T_UP, T_RIGHT, T_DOWN, T_LEFT), O(O_UP, O_RIGHT,
				O_DOWN, O_LEFT), I(I_UP, I_RIGHT, I_DOWN, I_LEFT);
		private final int _upHex;
		private final int _rightHex;
		private final int _downHex;
		private final int _leftHex;

		Value(int upHex, int rightHex, int downHex, int leftHex) {
			_upHex = upHex;
			_rightHex = rightHex;
			_downHex = downHex;
			_leftHex = leftHex;
		}

		public int gethexV(Direction dir) {
			switch (dir) {
			case Up:
				return _upHex;
			case Down:
				return _downHex;
			case Left:
				return _leftHex;
			case Right:
				return _rightHex;
			}
			return 0;
		}
	}

	public enum Direction {
		Up, Down, Left, Right
	}
	
	Value value;
	Direction direction;
	int color;

	public Block(Value argV, int argC, Direction argD) {
		this.value = argV;
		this.color = argC;
		this.direction = argD;
	}
	
	public int getHexValue() {
		return value.gethexV(direction);
	}

	public void rotate() {
		switch (direction) {
		case Up:
			direction = Direction.Right;
			break;
		case Right:
			direction = Direction.Down;
			break;
		case Down:
			direction = Direction.Left;
			break;
		case Left:
			direction = Direction.Up;
			break;
		}
	}
	
	/**
	 * rotate reversely
	 */
	public void rRotate() {
		switch (direction) {
		case Up:
			direction = Direction.Left;
			break;
		case Left:
			direction = Direction.Down;
			break;
		case Down:
			direction = Direction.Right;
			break;
		case Right:
			direction = Direction.Up;
			break;
		}
	}
}

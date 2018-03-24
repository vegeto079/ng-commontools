package com.github.vegeto079.ngcommontools.example;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import com.github.vegeto079.ngcommontools.main.DoublePoint;
import com.github.vegeto079.ngcommontools.main.Game;
import com.github.vegeto079.ngcommontools.main.JoystickHandler;
import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.JoystickHandler.JoystickListener;

@SuppressWarnings("serial")
public class ExampleNoComments extends Game {

	static ExampleNoComments game = null;
	JoystickHandler joystickHandler = null;
	DoublePoint location = new DoublePoint(0, 0);
	boolean buttonPressed = false;
	Color insideColor = Color.WHITE;
	double up, down, left, right;
	final int ovalWidth = 10, ovalHeight = 10;

	public ExampleNoComments(Logger logger, String[] args, int ticksPerSecond,
			int paintTicksPerSecond, String title, int width, int height) {
		super(logger, args, ticksPerSecond, paintTicksPerSecond, title, width,
				height);
	}

	public static void main(String[] args) {
		game = new ExampleNoComments(new Logger(true), args, 60, 120,
				"Example Game", 100, 100);
		game.startThread();
	}

	@Override
	public void firstLoad() {
		super.firstLoad();
		location = new DoublePoint(getWidth() / 2 - ovalWidth / 2, getHeight()
				/ 2 - ovalHeight / 2);
		joystickHandler = new JoystickHandler(logger);
		joystickHandler.setJoystickListener(new ExampleJoystickListener());
		joystickHandler.start();
	}

	@Override
	public void paintTick(Graphics2D g) {
		super.paintTick(g);
		g.setColor(insideColor);
		g.fillOval((int) location.x - ovalWidth / 2, (int) location.y
				- ovalHeight / 2, ovalWidth, ovalHeight);
		g.setColor(Color.WHITE);
		g.drawOval((int) location.x - ovalWidth / 2, (int) location.y
				- ovalHeight / 2, ovalWidth, ovalHeight);
		g.drawString("fps: " + getFps(), 10, 10);
		g.drawString("game tick rate: " + getUps(), 10, 25);
	}

	@Override
	public void gameTick() {
		super.gameTick();
		if (buttonPressed)
			insideColor = Color.RED;
		else
			insideColor = Color.WHITE;
		if (up > 0) {
			location.adjust(0, -up);
			up = 0;
		}
		if (down > 0) {
			location.adjust(0, down);
			down = 0;
		}
		if (left > 0) {
			location.adjust(-left, 0);
			left = 0;
		}
		if (right > 0) {
			location.adjust(right, 0);
			right = 0;
		}
		if (location.x < -6)
			location.adjust(getWidth(), 0);
		if (location.x > getWidth() + 6)
			location.adjust(-getWidth(), 0);
		if (location.y < -6)
			location.adjust(0, getHeight());
		if (location.y > getHeight() + 6)
			location.adjust(0, -getHeight());
	}

	@Override
	public void keyPressed(int keyCode) {
		buttonPressed = true;
		if (keyCode == KeyEvent.VK_UP) {
			up += 5.5;
		} else if (keyCode == KeyEvent.VK_DOWN) {
			down += 5.5;
		} else if (keyCode == KeyEvent.VK_LEFT) {
			left += 5.5;
		} else if (keyCode == KeyEvent.VK_RIGHT) {
			right += 5.5;
		}
	}

	@Override
	public void keyReleased(int keyCode) {
		super.keyReleased(keyCode);
		buttonPressed = false;
	}

	public class ExampleJoystickListener extends JoystickListener {

		@Override
		public void pressedUp(int joystick, boolean pressed) {
			buttonPressed = pressed;
			if (pressed)
				up += 5.5;
		}

		@Override
		public void pressedDown(int joystick, boolean pressed) {
			buttonPressed = pressed;
			if (pressed)
				down += 5.5;
		}

		@Override
		public void pressedLeft(int joystick, boolean pressed) {
			buttonPressed = pressed;
			if (pressed)
				left += 5.5;
		}

		@Override
		public void pressedRight(int joystick, boolean pressed) {
			buttonPressed = pressed;
			if (pressed)
				right += 5.5;
		}

		@Override
		public void pressedL1(int joystick, boolean pressed) {
			buttonPressed = pressed;
		}

		@Override
		public void pressedR1(int joystick, boolean pressed) {
			buttonPressed = pressed;
		}

		@Override
		public void pressedL2(int joystick, boolean pressed) {
			buttonPressed = pressed;
		}

		@Override
		public void pressedR2(int joystick, boolean pressed) {
			buttonPressed = pressed;
		}

		@Override
		public void pressedButton(int joystick, int button, boolean pressed) {
			buttonPressed = pressed;
		}

	}

}

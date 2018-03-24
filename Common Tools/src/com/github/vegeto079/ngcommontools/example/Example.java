package com.github.vegeto079.ngcommontools.example;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import com.github.vegeto079.ngcommontools.main.DoublePoint;
import com.github.vegeto079.ngcommontools.main.Game;
import com.github.vegeto079.ngcommontools.main.JoystickHandler;
import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.JoystickHandler.JoystickListener;

/**
 * <p>
 * An example class used to demonstrate various classes in commonTools.
 * </p>
 * <p>
 * An extension of {@link Game}, we start ourselves and have a simple game where
 * we can move a dot on the screen with the keyboard or a joystick.
 * </p>
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: {@link #paintTicksPerSecond} changed from 60 to 120 to
 *          demonstrate that they can be different but FPS should be higher than
 *          UPS.
 * 
 */
@SuppressWarnings("serial")
public class Example extends Game {

	// This class: static required to be accessed by the static method
	// main(String[]).
	static Example game = null;
	// Our JoystickHandler, initialized in firstLoad().
	JoystickHandler joystickHandler = null;
	// DoublePoint of our oval's location on the screen. This point can
	// be easily accessed with Double numbers rather than only Integers
	DoublePoint location = new DoublePoint(0, 0);
	// Set to true when we press a button
	boolean buttonPressed = false;
	// Color of the inside of the oval
	Color insideColor = Color.WHITE;
	// Pressing these on the keyboard/joystick
	double up, down, left, right;
	// Hard coded oval size
	final int ovalWidth = 10, ovalHeight = 10;

	//
	// Necessary method required to initiate any Game.
	// #logger: determines what types of messages we
	// want our Game to display in this release.
	// #args: The arguments passed on from {@link #main(String[])}.
	// #ticksPerSecond: How many times per second to run gameTick() (should be
	// the same as or below <b>paintTicksPerSecond</b>).
	// #paintTicksPerSecond: How many times per second to run paintTick(g)
	// #title: Title of the window
	// #width: Width of the window
	// #height: Height of the window

	public Example(Logger logger, String[] args, int ticksPerSecond, int paintTicksPerSecond, String title, int width,
			int height) {
		super(logger, args, ticksPerSecond, paintTicksPerSecond, title, width, height);
	}

	// The starting method, called when this class is ran. Creates a new game
	// with hard-coded variables and starts the game's thread.
	public static void main(String[] args) {
		game = new Example(new Logger(true), args, 60, 120, "Example Game", 100, 100);
		game.startThread();
	}

	// Automatically called only once: the very first time this Game is created.
	// Use to initialize variables and otherwise run things before anything
	// else.
	@Override
	public void firstLoad() {
		super.firstLoad(); // Always call the super class first
		location = new DoublePoint(getWidth() / 2 - ovalWidth / 2, getHeight() / 2 - ovalHeight / 2);
		// Create listener for joysticks
		joystickHandler = new JoystickHandler(logger);
		// Set our custom listener ExampleJoystickListener to determine what the
		// joystick button presses will do.
		joystickHandler.setJoystickListener(new ExampleJoystickListener());
		joystickHandler.start(); // Start listening
	}

	// Painting method.
	@Override
	public void paintTick(Graphics2D g) {
		super.paintTick(g); // Always call the super class first
		// draw the oval
		g.setColor(insideColor);
		g.fillOval((int) location.x - ovalWidth / 2, (int) location.y - ovalHeight / 2, ovalWidth, ovalHeight);
		g.setColor(Color.WHITE);
		g.drawOval((int) location.x - ovalWidth / 2, (int) location.y - ovalHeight / 2, ovalWidth, ovalHeight);
		// draw fps/ups info
		g.drawString("fps: " + getFps(), 10, 10);
		g.drawString("game tick rate: " + getUps(), 10, 25);
	}

	// Game ticking method.
	@Override
	public void gameTick() {
		super.gameTick(); // Always call the super class first
		if (buttonPressed)
			insideColor = Color.RED;
		else
			insideColor = Color.WHITE;
		// We could, instead of using the buttonPressed variable to store the
		// status of buttons being pushed, directly change insideColor from
		// inside the joystick and key handlers.
		//
		// However, I strongly recommend not doing this and instead handling
		// most pressing through the game ticking method. The reason for this is
		// the input handlers are handled more often than our requested
		// ticksPerSecond and are on a very important thread.
		//
		// What this means is if we do something that takes up even a little
		// processing time on our input handlers, our whole java application
		// will slow down during that processing time. If you handle things in
		// the game ticking method instead, they are in a dedicated thread for
		// processing and will not affect any other threads if what the input is
		// trying to do takes any processing time at all.
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

	// Method called when keyboard keys are pressed.
	@Override
	public void keyPressed(int keyCode) {
		buttonPressed = true; // Light up oval
		// Compare the keyCode to various keyboard buttons
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
		super.keyReleased(keyCode);// Always call the super class first
		buttonPressed = false; // Turn off oval
	}

	// Our custom JoystickListener to tell the JoystickHandler how to handle
	// joystick interaction.
	public class ExampleJoystickListener extends JoystickListener {

		@Override
		public void pressedUp(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
			if (pressed)
				up += 5.5; // Move oval
		}

		@Override
		public void pressedDown(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
			if (pressed)
				down += 5.5; // Move oval
		}

		@Override
		public void pressedLeft(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
			if (pressed)
				left += 5.5; // Move oval
		}

		@Override
		public void pressedRight(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
			if (pressed)
				right += 5.5; // Move oval
		}

		@Override
		public void pressedL1(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
		}

		@Override
		public void pressedR1(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
		}

		@Override
		public void pressedL2(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
		}

		@Override
		public void pressedR2(int joystick, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
		}

		@Override
		public void pressedButton(int joystick, int button, boolean pressed) {
			buttonPressed = pressed; // Light up or turn off oval
		}

	}

}

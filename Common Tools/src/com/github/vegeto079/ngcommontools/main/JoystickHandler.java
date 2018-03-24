package com.github.vegeto079.ngcommontools.main;

import java.io.File;
import java.util.ArrayList;

import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 * Class created to easily handle all types of {@link JInputJoystick}s.<br>
 * Requires a custom {@link JoystickListener} and {@link Logger}.<br>
 * See {@link #main(String[])} for example on how to use.
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Set all abstract methods to public.
 * @version 1.02: Replaced all instances of 'up' in {@link JoystickListener} to
 *          'pressed' for better clarity.
 * @version 1.03: Added {@link getConnectedJoysticksAmt()}.
 * @version 1.04: Added the <b>joystick</b> parameter to all methods in
 *          {@link JoystickListener} to determine which joystick (out of
 *          multiple, when possible) the button press originated from.
 * @version 1.05: Added {@link #forceResetJoysticks} and support via
 *          {@link #run()}.
 * @version 1.06: Added {@link #joystickHandler}, enclosing it within this
 *          class. We now run that {@link Thread} instead of our own, so we can
 *          name the {@link Thread} appropriately. What used to be our
 *          {@link #run()} method is now accessed the same way, but redirects to
 *          {@link JoystickHandlerRunner} and
 *          {@link JoystickHandlerRunner#run()}.
 * @version 1.07: Now tries multiple times to copy the DLL files into whatever
 *          library paths we can find. {@link #attempt} is the current attempt,
 *          while {@link #maxAttempts} is the max amount of tries. If we run out
 *          of different paths to try (regardless of {@link #maxAttempts}) we
 *          will stop trying.
 * @version 1.08: Poll time is now selectable by {@link #pollTime} rather than a
 *          hard-coded 1 second timer.
 * @version 1.09: Shutdown hook now added when creating this
 *          {@link JoystickHandler}, hopefully preventing it from trying to
 *          initiate again, hanging JVM on exit.
 * @version 1.091: Added {@link #getPollTime()}.
 * @version 1.092: Now tells what path we found the DLL files in.
 * @version 1.093: Added {@link #foundDllFiles}.
 */
public class JoystickHandler extends Thread {
	/**
	 * The encased {@link Thread} that runs when {@link #start()} is called (and
	 * therefore {@link #run()} gets called).
	 */
	private Thread joystickHandler = null;
	/**
	 * An {@link ArrayList} of {@link JInputJoystick}s. These
	 * {@link JInputJoystick}s are used for all input. Initialized in
	 * {@link #init()} and handled in {@link #run()}, all input from connected
	 * {@link JInputJoystick}s is sent through {@link #listener}.
	 */
	private ArrayList<JInputJoystick> joysticks = null;
	/**
	 * An {@link ArrayList} of {@link Boolean}s, being whether or not the given
	 * joystick (determined by placement in the {@link ArrayList}) is still
	 * connected.
	 */
	private ArrayList<Boolean> connected;
	/**
	 * An {@link ArrayList} of an {@link ArrayList} of {@link Integer}s. Each
	 * joystick has one {@link ArrayList} of {@link Integer}s for their buttons.
	 * These {@link Integer}s represent whether or not buttons are pushed, as
	 * determined by {@link #run()}. These {@link Integer}s help prevent
	 * {@link #run()} from constantly telling the {@link #listener} that the
	 * button is being pushed when it is being held down.
	 */
	private ArrayList<ArrayList<Integer>> buttonsPressed = null;
	/**
	 * If <b>true</b>, the {@link #run()} loop will cease, causing this
	 * {@link JoystickHandler} to stop functioning and end.
	 */
	private boolean stop = false;
	/**
	 * How many devices are currently connected. Used heavily by {@link #run()}
	 * to determine whether or not devices are still connected.
	 */
	private int connectedDeviceAmt;
	/**
	 * The {@link Logger} used for logging.
	 */
	private Logger logger = null;
	/**
	 * The custom {@link JoystickListener} used for input interpretation via
	 * {@link #run()}.
	 */
	private JoystickListener listener = null;
	/**
	 * The amount of time in ms to sleep after {@link #run()}.
	 */
	private final int sleepAmt = 10;
	/**
	 * If <b>true</b>, {@link #run()} (on it's next loop) will not loop
	 * normally, and instead act as if all controllers have disconnected and we
	 * need to reestablish the connection from scratch.
	 */
	private boolean forceResetJoysticks = false;
	/**
	 * The current attempt we are on (in trying to copy the DLL files).
	 * 
	 */
	private int attempt = 0;
	/**
	 * Time to sleep before checking for joysticks again if none are connected.
	 */
	private long pollTime = 1000;
	/**
	 * Whether or not we have successfully found the DLL files already.
	 */
	private boolean foundDllFiles = false;

	/**
	 * Sets {@link #forceResetJoysticks} to <b>true</b>.
	 */
	public void forceResetJoysticks() {
		forceResetJoysticks = true;
	}

	/**
	 * For testing purposes.
	 */
	public static void main(String[] args) {
		JoystickHandler us = new JoystickHandler(new Logger(true));
		us.setName("JoystickHandler");
		JoystickListenerLogger listen = us.new JoystickListenerLogger();
		us.setJoystickListener(listen);
		us.start();
	}

	/**
	 * Custom {@link JoystickListener} used for testing purposes. Logs all
	 * information from the joystick and nothing else.
	 * 
	 */
	private class JoystickListenerLogger extends JoystickListener {
		public void pressedUp(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " Up!");
		}

		public void pressedDown(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " Down!");
		}

		public void pressedLeft(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " Left!");
		}

		public void pressedRight(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " Right!");
		}

		public void pressedButton(int joystick, int button, boolean pressed) {
			logger.log("We " + (pressed ? "pressed " : "released ") + button + "!");
		}

		public void pressedL1(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " L1!");
		}

		public void pressedR1(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " R1!");
		}

		public void pressedL2(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " L2!");
		}

		public void pressedR2(int joystick, boolean pressed) {
			logger.log("We " + (pressed ? "pressed" : "released") + " R2!");
		}

	}

	/**
	 * Sets the {@link logger} and initializes this {@link JoystickHandler} via
	 * {@link #init()}. Keep in mind that you still need to set a
	 * {@link JoystickListener} with
	 * {@link #setJoystickListener(JoystickListener)} for {@link #run()} to
	 * start looping properly.
	 * 
	 * @param logger
	 */
	public JoystickHandler(Logger logger) {
		this.logger = logger;
		init();
	}

	/**
	 * Sets {@link listener}, which will allow {@link #run()} to start looping
	 * properly if everything else is initialized.
	 * 
	 * @param listener
	 */
	public void setJoystickListener(JoystickListener listener) {
		this.listener = listener;
	}

	/**
	 * @return The amount of joysticks currently connected.
	 */
	public int getConnectedJoysticksAmt() {
		int amt = 0;
		for (int i = 0; i < joysticks.size(); i++)
			if (connected.get(i))
				amt++;
		return amt;
	}

	/**
	 * Initializes this {@link JoystickHandler}. Firstly makes sure all relevant
	 * DLL files are copied to the library path so they are accessible. Then
	 * attempts to connect to all joysticks connected to the system and sorts
	 * them into {@link #joysticks} in order of finding them.
	 */
	public void init() {
		boolean controllerFound = false;
		joysticks = new ArrayList<JInputJoystick>();
		connected = new ArrayList<Boolean>();
		buttonsPressed = new ArrayList<ArrayList<Integer>>();
		if (!foundDllFiles) {
			String[] dllNames = { "jinput-dx8.dll", "jinput-dx8_64.dll", "jinput-raw.dll", "jinput-raw_64.dll",
					"jinput-wintab.dll" };
			File dll = Tools.findFileOnLibraryPath(dllNames[0]);
			for (int j = 1; j < dllNames.length && dll != null && dll.exists(); j++)
				dll = Tools.findFileOnLibraryPath(dllNames[j]);
			String dllsLocation = System.getProperty("user.dir") + System.getProperty("file.separator") + "res"
					+ System.getProperty("file.separator") + "dll" + System.getProperty("file.separator");
			if (!ResourceHandler.runningFromJar())
				dllsLocation = System.getProperty("user.dir") + System.getProperty("file.separator") + "src"
						+ System.getProperty("file.separator") + "res" + System.getProperty("file.separator") + "dll"
						+ System.getProperty("file.separator");
			ArrayList<File> dlls = new ArrayList<File>();
			for (int i = 0; i < dllNames.length; i++)
				dlls.add(new File(dllsLocation + dllNames[i]));
			logger.log(LogLevel.DEBUG, "dllsLocation: " + dllsLocation);
			String libraryPath = System.getProperty("java.library.path");
			logger.log(LogLevel.DEBUG, "Got library path: " + libraryPath);
			String[] allPaths = libraryPath.split(System.getProperty("path.separator"));
			ArrayList<String> paths = new ArrayList<String>();
			for (String path : allPaths)
				if (!paths.contains(path) && path.length() > 5)
					paths.add(path);
			logger.log(LogLevel.DEBUG, "Got paths. Amount: " + paths.size());
			int lastCheckedNum = -1;
			for (int i = paths.size() - 1; i > 0; i--)
				if (paths.get(i).contains("java") && paths.get(i).contains("jre") && paths.get(i).contains("bin")) {
					if (lastCheckedNum == i)
						break;
					else {
						logger.log(LogLevel.DEBUG, "Moved path to higher priority (" + i + " to 0): " + paths.get(i));
						paths.add(0, paths.get(i));
						paths.remove(i + 1);
						i++;
					}
				}
			String path = null;
			while (dll == null || !dll.exists()) {
				attempt++;
				logger.log(LogLevel.DEBUG, "Didn't find joystick DLL files in librarypath! Attempt #" + attempt);
				boolean allExist = true;
				for (int i = 0; i < dlls.size(); i++)
					if (dlls.get(i) == null || !dlls.get(i).exists()) {
						allExist = false;
						break;
					}
				if (attempt >= paths.size()) {
					logger.log(LogLevel.DEBUG, "Ran out of java library paths to try to put files in.");
					break;
				} else
					path = paths.get(attempt);
				String finalChar = path.substring(path.length());
				if (finalChar != System.getProperty("file.separator"))
					path += System.getProperty("file.separator");
				logger.log(LogLevel.DEBUG, "Got attempt #" + attempt + " path: " + path);
				if (allExist) {
					for (int i = 0; i < dlls.size(); i++) {
						File file = dlls.get(i);
						File destination = new File(path + file.getName());
						if (destination.exists()) {
							logger.log(LogLevel.DEBUG,
									"File #" + i + " (" + file.getName() + ") already exists, not going to copy it.");
						} else
							try {
								Tools.copyFile(file, destination);
								logger.log(LogLevel.DEBUG,
										"Copied file #" + i + " (" + file.getName() + ") successfully.");
							} catch (Exception e) {
								// e.printStackTrace();
								logger.log(LogLevel.DEBUG,
										"Problem copying file #" + i + " (" + file.getName() + ") to destination!!");
								break;
							}
					}
				} else {
					logger.log(LogLevel.DEBUG, "Problem getting dlls?");
				}
				if (dll == null || !dll.exists()) {
					dll = Tools.findFileOnLibraryPath(dllNames[0]);
					for (int j = 1; j < dllNames.length && dll != null && dll.exists(); j++)
						dll = Tools.findFileOnLibraryPath(dllNames[j]);
				}
			}
			if (dll != null && dll.exists()) {
				logger.log(LogLevel.DEBUG, "Found DLL files in library path: " + dll.getAbsolutePath());
				foundDllFiles = true;
				logger.log(LogLevel.DEBUG, "foundDllFiles set to true.");
			} else {
				logger.log(LogLevel.DEBUG, "We had some issue in locating our DLL files. Stopping JoystickHandler.");
				stop = true;
				return;
			}
		} else
			logger.log(LogLevel.DEBUG, "We've already found the DLL files.");
		while (true) {
			JInputJoystick joystick = new JInputJoystick(Controller.Type.STICK, Controller.Type.GAMEPAD,
					joysticks.size() + 1);
			if (!joystick.isControllerConnected()) {
				if (controllerFound)
					logger.log(LogLevel.DEBUG, "No more controllers found.");
				else
					logger.log(LogLevel.DEBUG, "No controllers found.");
				break;
			} else {
				logger.log(LogLevel.WARNING, "Controller found! " + joystick.getControllerName());
				controllerFound = true;
				joysticks.add(joystick);
				connected.add(true);
				connectedDeviceAmt++;
				buttonsPressed.add(new ArrayList<Integer>());
			}
		}
		// if (!controllerFound) {
		// logger.log(LogLevel.DEBUG,
		// "No controllers found, stopping the JoystickHandler thread.");
		// stop = true;
		// } else {
		logger.log(LogLevel.DEBUG, "Waiting for start() call to begin.");
		// }
	}

	/**
	 * Sets the amount of time to wait before trying to connect to a Joystick
	 * again.
	 * 
	 * @param pollTime
	 */
	public void setPollTime(long pollTime) {
		this.pollTime = pollTime;
	}

	/**
	 * @return {@link #pollTime}.
	 */
	public long getPollTime() {
		return pollTime;
	}

	/**
	 * Runs the enclosed {@link #joystickHandler}, starting
	 * {@link JoystickHandlerRunner}.
	 */
	public void run() {
		joystickHandler = new Thread(new JoystickHandlerRunner(), "Joystick Handler");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				stop = true;
				interrupt();
			}
		});
		logger.log(LogLevel.DEBUG, "Added JoystickHandler shutdown hook.");
		joystickHandler.start();
	}

	/**
	 * Not a full-fledged class: merely an enclosure for {@link #run()} so we
	 * can name our {@link JoystickHandler}'s {@link Thread}.
	 * 
	 * @author Nathan
	 * 
	 */
	public class JoystickHandlerRunner implements Runnable {
		/**
		 * <p>
		 * Runs this {@link JoystickHandler}, determining all joystick input and
		 * relaying it to the given {@link #listener} to handle. Also calls
		 * {@link #init()} once per second if no joysticks are found to
		 * re-establish connection of joysticks.
		 * </p>
		 * <p>
		 * As of version 1.06 we no longer attempt to re-establish a connection.
		 * Will be kept this way until a better connection solution is found.
		 * Right now each connection attempt creates a Thread that refuses to
		 * disappear.
		 * </p>
		 */
		public void run() {
			logger.log(LogLevel.DEBUG, "JoystickHandler loop starting.");
			while (!stop && !Thread.interrupted()) {
				/*
				 * if (attempt >= maxAttempts) { logger.log( LogLevel.ERROR,
				 * "Could not copy DLL files to proper library locations. Probably need permissions. Cancelling JoystickHandler."
				 * ); stop = true; continue; }
				 */
				try {
					if (connectedDeviceAmt == 0 || forceResetJoysticks) {
						if (!forceResetJoysticks) {
							logger.log(LogLevel.DEBUG,
									"All devices disconnected. Retrying to connect to available joysticks. Sleeping "
											+ pollTime + "ms (pollTime) first.");
							sleep(pollTime);
						} else
							logger.log(LogLevel.WARNING, "We were told to reset joysticks immediately. Doing so now.");
						forceResetJoysticks = false;
						init();
						continue;
					}
					if (connectedDeviceAmt == 0)
						continue;
					connectedDeviceAmt = 0;
					for (int i = 0; i < joysticks.size(); i++) {
						JInputJoystick joystick = joysticks.get(i);
						String name = joystick.getControllerName();
						ArrayList<Integer> buttons = buttonsPressed.get(i);
						if (!joystick.pollController()) {
							if (connected.get(i)) {
								logger.log(LogLevel.WARNING, "Joystick #" + (i + 1) + " (" + name + ") disconnected!");
								connected.set(i, false);
							}
						} else {
							connectedDeviceAmt++;
							if (!connected.get(i)) {
								logger.log(LogLevel.WARNING, "Joystick #" + (i + 1) + " (" + name + ") reconnected!");
								connected.set(i, true);
							}
							if (listener == null) {
								// logger.log(LogLevel.DEBUG,
								// "Skipping joystick: no JoystickListener
								// found.");
								continue;
							}
							if (name.toLowerCase().contains("ireless controlle")) {
								logger.log(LogLevel.DEBUG, "Found controller preset: PS4 Controller.");
								while (buttons.size() < 20)
									buttons.add(0);
								// ::POV START::
								if (buttons.get(1) == 0 && buttons.get(2) == 0) {
									float hatSwitchPosition = joystick.getHatSwitchPosition();
									if (Float.compare(hatSwitchPosition, Component.POV.OFF) == 0) {
										if (buttons.get(0) != 0) {
											if (buttons.get(0) == 1)
												listener.pressedUp(i, false);
											else if (buttons.get(0) == 2)
												listener.pressedDown(i, false);
											else if (buttons.get(0) == 3)
												listener.pressedLeft(i, false);
											else if (buttons.get(0) == 4)
												listener.pressedRight(i, false);
											else if (buttons.get(0) == 5) {
												listener.pressedUp(i, false);
												listener.pressedLeft(i, false);
											} else if (buttons.get(0) == 6) {
												listener.pressedUp(i, false);
												listener.pressedRight(i, false);
											} else if (buttons.get(0) == 7) {
												listener.pressedDown(i, false);
												listener.pressedLeft(i, false);
											} else if (buttons.get(0) == 8) {
												listener.pressedDown(i, false);
												listener.pressedRight(i, false);
											}
											buttons.set(0, 0);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 1) {
											if (buttons.get(0) != 5 && buttons.get(0) != 6)
												listener.pressedUp(i, true);
											buttons.set(0, 1);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 2) {
											if (buttons.get(0) != 7 && buttons.get(0) != 8)
												listener.pressedDown(i, true);
											buttons.set(0, 2);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.LEFT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 3) {
											if (buttons.get(0) != 5 && buttons.get(0) != 7)
												listener.pressedLeft(i, true);
											buttons.set(0, 3);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.RIGHT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
										}
										if (buttons.get(0) != 4) {
											if (buttons.get(0) != 8 && buttons.get(0) != 6)
												listener.pressedRight(i, true);
											buttons.set(0, 4);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP_LEFT) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 6)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 7)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 5) {
											if (buttons.get(0) != 1 && buttons.get(0) != 6)
												listener.pressedUp(i, true);
											if (buttons.get(0) != 3 && buttons.get(0) != 7)
												listener.pressedLeft(i, true);
											buttons.set(0, 5);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP_RIGHT) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8)
											listener.pressedDown(i, false);
										if (buttons.get(0) != 6) {
											if (buttons.get(0) != 1 && buttons.get(0) != 5)
												listener.pressedUp(i, true);
											if (buttons.get(0) != 4 && buttons.get(0) != 8)
												listener.pressedRight(i, true);
											buttons.set(0, 6);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN_LEFT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 8)
											listener.pressedRight(i, false);
										if (buttons.get(0) != 7) {
											if (buttons.get(0) != 2 && buttons.get(0) != 8)
												listener.pressedDown(i, true);
											if (buttons.get(0) != 3 && buttons.get(0) != 5)
												listener.pressedLeft(i, true);
											buttons.set(0, 7);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN_RIGHT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 7)
											listener.pressedLeft(i, false);
										if (buttons.get(0) != 8) {
											if (buttons.get(0) != 2 && buttons.get(0) != 7)
												listener.pressedDown(i, true);
											if (buttons.get(0) != 4 && buttons.get(0) != 6)
												listener.pressedRight(i, true);
											buttons.set(0, 8);
										}
									}
								}
								// ::ANALOG START::
								if (buttons.get(0) == 0) {
									int xValuePercentageLeftJoystick = joystick.getXAxisPercentage();
									int yValuePercentageLeftJoystick = joystick.getYAxisPercentage();
									// logger.log(xValuePercentageLeftJoystick +
									// ","
									// + yValuePercentageLeftJoystick);
									boolean pressedUp = false, pressedDown = false, pressedLeft = false,
											pressedRight = false;
									if (yValuePercentageLeftJoystick < 20) {
										pressedUp = true;
										if (buttons.get(1) != 1) {
											buttons.set(1, 1);
											listener.pressedUp(i, true);
										}
									}
									if (yValuePercentageLeftJoystick > 80) {
										pressedDown = true;
										if (buttons.get(1) != 2) {
											buttons.set(1, 2);
											listener.pressedDown(i, true);
										}
									}
									if (xValuePercentageLeftJoystick < 20) {
										pressedLeft = true;
										if (buttons.get(2) != 1) {
											buttons.set(2, 1);
											listener.pressedLeft(i, true);
										}
									}
									if (xValuePercentageLeftJoystick > 80) {
										pressedRight = true;
										if (buttons.get(2) != 2) {
											buttons.set(2, 2);
											listener.pressedRight(i, true);
										}
									}
									if (buttons.get(1) == 1 && !pressedUp) {
										buttons.set(1, 0);
										listener.pressedUp(i, false);
									}
									if (buttons.get(1) == 2 && !pressedDown) {
										buttons.set(1, 0);
										listener.pressedDown(i, false);
									}
									if (buttons.get(2) == 1 && !pressedLeft) {
										buttons.set(2, 0);
										listener.pressedLeft(i, false);
									}
									if (buttons.get(2) == 2 && !pressedRight) {
										buttons.set(2, 0);
										listener.pressedRight(i, false);
									}
								}
								// ::BUTTONS START::
								int buttonsBeforeThis = 4;
								for (int j = 0; j < 6; j++) {
									int[] switchFrom = { 0, 1 };
									int[] switchTo = { 1, 0 };
									int newNum = j;
									for (int k = 0; k < switchFrom.length; k++)
										if (k == j)
											newNum = switchTo[k];
									if (buttons.get(j + buttonsBeforeThis) == 0 && joystick.getButtonValue(j)) {
										buttons.set(j + buttonsBeforeThis, 1);
										listener.pressedButton(i, newNum + 1, true);
									} else if (buttons.get(j + buttonsBeforeThis) == 1 && !joystick.getButtonValue(j)) {
										buttons.set(j + buttonsBeforeThis, 0);
										listener.pressedButton(i, newNum + 1, false);
									}
								}
								// ::TRIGGERS START::
								if (buttons.get(13) == 0 && joystick.getButtonValue(6)) {
									listener.pressedL1(i, true);
									buttons.set(13, 1);
								} else if (buttons.get(13) == 1 && !joystick.getButtonValue(6)) {
									listener.pressedL1(i, false);
									buttons.set(13, 0);
								}
								if (buttons.get(14) == 0 && joystick.getButtonValue(7)) {
									listener.pressedR1(i, true);
									buttons.set(14, 1);
								} else if (buttons.get(14) == 1 && !joystick.getButtonValue(7)) {
									listener.pressedR1(i, false);
									buttons.set(14, 0);
								}
							} else if (name.toLowerCase().contains("xbox one")) {
								logger.log(LogLevel.DEBUG, "Found controller preset: Xbox One Controller.");
								while (buttons.size() < 20)
									buttons.add(0);
								// ::POV START::
								if (buttons.get(1) == 0 && buttons.get(2) == 0) {
									float hatSwitchPosition = joystick.getHatSwitchPosition();
									if (Float.compare(hatSwitchPosition, Component.POV.OFF) == 0) {
										if (buttons.get(0) != 0) {
											if (buttons.get(0) == 1)
												listener.pressedUp(i, false);
											else if (buttons.get(0) == 2)
												listener.pressedDown(i, false);
											else if (buttons.get(0) == 3)
												listener.pressedLeft(i, false);
											else if (buttons.get(0) == 4)
												listener.pressedRight(i, false);
											else if (buttons.get(0) == 5) {
												listener.pressedUp(i, false);
												listener.pressedLeft(i, false);
											} else if (buttons.get(0) == 6) {
												listener.pressedUp(i, false);
												listener.pressedRight(i, false);
											} else if (buttons.get(0) == 7) {
												listener.pressedDown(i, false);
												listener.pressedLeft(i, false);
											} else if (buttons.get(0) == 8) {
												listener.pressedDown(i, false);
												listener.pressedRight(i, false);
											}
											buttons.set(0, 0);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 1) {
											if (buttons.get(0) != 5 && buttons.get(0) != 6)
												listener.pressedUp(i, true);
											buttons.set(0, 1);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 2) {
											if (buttons.get(0) != 7 && buttons.get(0) != 8)
												listener.pressedDown(i, true);
											buttons.set(0, 2);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.LEFT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 3) {
											if (buttons.get(0) != 5 && buttons.get(0) != 7)
												listener.pressedLeft(i, true);
											buttons.set(0, 3);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.RIGHT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
										}
										if (buttons.get(0) != 4) {
											if (buttons.get(0) != 8 && buttons.get(0) != 6)
												listener.pressedRight(i, true);
											buttons.set(0, 4);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP_LEFT) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 6)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 7)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 5) {
											if (buttons.get(0) != 1 && buttons.get(0) != 6)
												listener.pressedUp(i, true);
											if (buttons.get(0) != 3 && buttons.get(0) != 7)
												listener.pressedLeft(i, true);
											buttons.set(0, 5);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP_RIGHT) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8)
											listener.pressedDown(i, false);
										if (buttons.get(0) != 6) {
											if (buttons.get(0) != 1 && buttons.get(0) != 5)
												listener.pressedUp(i, true);
											if (buttons.get(0) != 4 && buttons.get(0) != 8)
												listener.pressedRight(i, true);
											buttons.set(0, 6);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN_LEFT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 8)
											listener.pressedRight(i, false);
										if (buttons.get(0) != 7) {
											if (buttons.get(0) != 2 && buttons.get(0) != 8)
												listener.pressedDown(i, true);
											if (buttons.get(0) != 3 && buttons.get(0) != 5)
												listener.pressedLeft(i, true);
											buttons.set(0, 7);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN_RIGHT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 7)
											listener.pressedLeft(i, false);
										if (buttons.get(0) != 8) {
											if (buttons.get(0) != 2 && buttons.get(0) != 7)
												listener.pressedDown(i, true);
											if (buttons.get(0) != 4 && buttons.get(0) != 6)
												listener.pressedRight(i, true);
											buttons.set(0, 8);
										}
									}
								}
								// ::ANALOG START::
								if (buttons.get(0) == 0) {
									int xValuePercentageLeftJoystick = joystick.getXAxisPercentage();
									int yValuePercentageLeftJoystick = joystick.getYAxisPercentage();
									// logger.log(xValuePercentageLeftJoystick +
									// ","
									// + yValuePercentageLeftJoystick);
									boolean pressedUp = false, pressedDown = false, pressedLeft = false,
											pressedRight = false;
									if (yValuePercentageLeftJoystick < 20) {
										pressedUp = true;
										if (buttons.get(1) != 1) {
											buttons.set(1, 1);
											listener.pressedUp(i, true);
										}
									}
									if (yValuePercentageLeftJoystick > 80) {
										pressedDown = true;
										if (buttons.get(1) != 2) {
											buttons.set(1, 2);
											listener.pressedDown(i, true);
										}
									}
									if (xValuePercentageLeftJoystick < 20) {
										pressedLeft = true;
										if (buttons.get(2) != 1) {
											buttons.set(2, 1);
											listener.pressedLeft(i, true);
										}
									}
									if (xValuePercentageLeftJoystick > 80) {
										pressedRight = true;
										if (buttons.get(2) != 2) {
											buttons.set(2, 2);
											listener.pressedRight(i, true);
										}
									}
									if (buttons.get(1) == 1 && !pressedUp) {
										buttons.set(1, 0);
										listener.pressedUp(i, false);
									}
									if (buttons.get(1) == 2 && !pressedDown) {
										buttons.set(1, 0);
										listener.pressedDown(i, false);
									}
									if (buttons.get(2) == 1 && !pressedLeft) {
										buttons.set(2, 0);
										listener.pressedLeft(i, false);
									}
									if (buttons.get(2) == 2 && !pressedRight) {
										buttons.set(2, 0);
										listener.pressedRight(i, false);
									}
								}
								// ::TRIGGERS START::
								int leftTriggerPercentage = joystick.getZAxisPercentage();
								int rightTriggerPercentage = joystick.getZRotationPercentage();
								if (leftTriggerPercentage > 50 && buttons.get(3) != 1) {
									buttons.set(3, 1);
									listener.pressedL1(i, true);
								} else if (leftTriggerPercentage < 30 && buttons.get(3) != 2) {
									buttons.set(3, 2);
									listener.pressedL1(i, false);
								}
								if (rightTriggerPercentage > 50 && buttons.get(4) != 1) {
									buttons.set(4, 1);
									listener.pressedR1(i, true);
								} else if (rightTriggerPercentage < 30 && buttons.get(4) != 2) {
									buttons.set(4, 2);
									listener.pressedR1(i, false);
								}
								// ::BUTTONS START::
								int buttonsBeforeThis = 5;
								for (int j = 0; j < 10; j++) {
									if (buttons.get(j + buttonsBeforeThis) == 0 && joystick.getButtonValue(j)) {
										buttons.set(j + buttonsBeforeThis, 1);
										listener.pressedButton(i, j + 1, true);
									} else if (buttons.get(j + buttonsBeforeThis) == 1 && !joystick.getButtonValue(j)) {
										buttons.set(j + buttonsBeforeThis, 0);
										listener.pressedButton(i, j + 1, false);
									}
								}
							} else if (true) {
								if ((name.toLowerCase().contains("xbox") && (name.contains("360"))))
									logger.log(LogLevel.DEBUG, "Found controller preset: Xbox 360 Controller.");
								else
									logger.log(LogLevel.DEBUG,
											"Could not find controller preset, defaulting to Xbox 360.");
								while (buttons.size() < 20)
									buttons.add(0);
								// ::POV START::
								if (buttons.get(1) == 0 && buttons.get(2) == 0) {
									float hatSwitchPosition = joystick.getHatSwitchPosition();
									if (Float.compare(hatSwitchPosition, Component.POV.OFF) == 0) {
										if (buttons.get(0) != 0) {
											if (buttons.get(0) == 1)
												listener.pressedUp(i, false);
											else if (buttons.get(0) == 2)
												listener.pressedDown(i, false);
											else if (buttons.get(0) == 3)
												listener.pressedLeft(i, false);
											else if (buttons.get(0) == 4)
												listener.pressedRight(i, false);
											else if (buttons.get(0) == 5) {
												listener.pressedUp(i, false);
												listener.pressedLeft(i, false);
											} else if (buttons.get(0) == 6) {
												listener.pressedUp(i, false);
												listener.pressedRight(i, false);
											} else if (buttons.get(0) == 7) {
												listener.pressedDown(i, false);
												listener.pressedLeft(i, false);
											} else if (buttons.get(0) == 8) {
												listener.pressedDown(i, false);
												listener.pressedRight(i, false);
											}
											buttons.set(0, 0);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 1) {
											if (buttons.get(0) != 5 && buttons.get(0) != 6)
												listener.pressedUp(i, true);
											buttons.set(0, 1);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 2) {
											if (buttons.get(0) != 7 && buttons.get(0) != 8)
												listener.pressedDown(i, true);
											buttons.set(0, 2);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.LEFT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 3) {
											if (buttons.get(0) != 5 && buttons.get(0) != 7)
												listener.pressedLeft(i, true);
											buttons.set(0, 3);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.RIGHT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
										} else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
										}
										if (buttons.get(0) != 4) {
											if (buttons.get(0) != 8 && buttons.get(0) != 6)
												listener.pressedRight(i, true);
											buttons.set(0, 4);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP_LEFT) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 6)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 7)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 8) {
											listener.pressedDown(i, false);
											listener.pressedRight(i, false);
										}
										if (buttons.get(0) != 5) {
											if (buttons.get(0) != 1 && buttons.get(0) != 6)
												listener.pressedUp(i, true);
											if (buttons.get(0) != 3 && buttons.get(0) != 7)
												listener.pressedLeft(i, true);
											buttons.set(0, 5);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.UP_RIGHT) == 0) {
										if (buttons.get(0) == 2)
											listener.pressedDown(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 7) {
											listener.pressedDown(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 8)
											listener.pressedDown(i, false);
										if (buttons.get(0) != 6) {
											if (buttons.get(0) != 1 && buttons.get(0) != 5)
												listener.pressedUp(i, true);
											if (buttons.get(0) != 4 && buttons.get(0) != 8)
												listener.pressedRight(i, true);
											buttons.set(0, 6);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN_LEFT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 4)
											listener.pressedRight(i, false);
										else if (buttons.get(0) == 5)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 6) {
											listener.pressedUp(i, false);
											listener.pressedRight(i, false);
										} else if (buttons.get(0) == 8)
											listener.pressedRight(i, false);
										if (buttons.get(0) != 7) {
											if (buttons.get(0) != 2 && buttons.get(0) != 8)
												listener.pressedDown(i, true);
											if (buttons.get(0) != 3 && buttons.get(0) != 5)
												listener.pressedLeft(i, true);
											buttons.set(0, 7);
										}
									} else if (Float.compare(hatSwitchPosition, Component.POV.DOWN_RIGHT) == 0) {
										if (buttons.get(0) == 1)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 3)
											listener.pressedLeft(i, false);
										else if (buttons.get(0) == 5) {
											listener.pressedUp(i, false);
											listener.pressedLeft(i, false);
										} else if (buttons.get(0) == 6)
											listener.pressedUp(i, false);
										else if (buttons.get(0) == 7)
											listener.pressedLeft(i, false);
										if (buttons.get(0) != 8) {
											if (buttons.get(0) != 2 && buttons.get(0) != 7)
												listener.pressedDown(i, true);
											if (buttons.get(0) != 4 && buttons.get(0) != 6)
												listener.pressedRight(i, true);
											buttons.set(0, 8);
										}
									}
								}
								// ::ANALOG START::
								if (buttons.get(0) == 0) {
									int xValuePercentageLeftJoystick = joystick.getXAxisPercentage();
									int yValuePercentageLeftJoystick = joystick.getYAxisPercentage();
									// logger.log(xValuePercentageLeftJoystick +
									// ","
									// + yValuePercentageLeftJoystick);
									boolean pressedUp = false, pressedDown = false, pressedLeft = false,
											pressedRight = false;
									if (yValuePercentageLeftJoystick < 20) {
										pressedUp = true;
										if (buttons.get(1) != 1) {
											buttons.set(1, 1);
											listener.pressedUp(i, true);
										}
									}
									if (yValuePercentageLeftJoystick > 80) {
										pressedDown = true;
										if (buttons.get(1) != 2) {
											buttons.set(1, 2);
											listener.pressedDown(i, true);
										}
									}
									if (xValuePercentageLeftJoystick < 20) {
										pressedLeft = true;
										if (buttons.get(2) != 1) {
											buttons.set(2, 1);
											listener.pressedLeft(i, true);
										}
									}
									if (xValuePercentageLeftJoystick > 80) {
										pressedRight = true;
										if (buttons.get(2) != 2) {
											buttons.set(2, 2);
											listener.pressedRight(i, true);
										}
									}
									if (buttons.get(1) == 1 && !pressedUp) {
										buttons.set(1, 0);
										listener.pressedUp(i, false);
									}
									if (buttons.get(1) == 2 && !pressedDown) {
										buttons.set(1, 0);
										listener.pressedDown(i, false);
									}
									if (buttons.get(2) == 1 && !pressedLeft) {
										buttons.set(2, 0);
										listener.pressedLeft(i, false);
									}
									if (buttons.get(2) == 2 && !pressedRight) {
										buttons.set(2, 0);
										listener.pressedRight(i, false);
									}
								}
								// ::TRIGGERS START::
								int triggerPercentage = joystick.getZAxisPercentage();
								if (triggerPercentage > 70 && buttons.get(3) != 1) {
									if (buttons.get(3) == 2)
										listener.pressedR1(i, false);
									buttons.set(3, 1);
									listener.pressedL1(i, true);
								} else if (triggerPercentage < 30 && buttons.get(3) != 2) {
									if (buttons.get(3) == 1)
										listener.pressedL1(i, false);
									buttons.set(3, 2);
									listener.pressedR1(i, true);
								} else if (triggerPercentage <= 70 && triggerPercentage >= 30) {
									if (buttons.get(3) == 1)
										listener.pressedL1(i, false);
									else if (buttons.get(3) == 2)
										listener.pressedR1(i, false);
									buttons.set(3, 0);
								}
								// ::BUTTONS START::
								int buttonsBeforeThis = 4;
								for (int j = 0; j < 10; j++) {
									if (buttons.get(j + buttonsBeforeThis) == 0 && joystick.getButtonValue(j)) {
										buttons.set(j + buttonsBeforeThis, 1);
										listener.pressedButton(i, j + 1, true);
									} else if (buttons.get(j + buttonsBeforeThis) == 1 && !joystick.getButtonValue(j)) {
										buttons.set(j + buttonsBeforeThis, 0);
										listener.pressedButton(i, j + 1, false);
									}
								}
							}
						}
					}
					sleep(sleepAmt);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.log(LogLevel.DEBUG, "JoystickHandler thread stopping.");
		}
	}

	public static abstract class JoystickListener {

		/**
		 * Pressing up on either POV or the left stick.
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedUp(int joystick, boolean pressed);

		/**
		 * Pressing down on either POV or the left stick.
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedDown(int joystick, boolean pressed);

		/**
		 * Pressing left on either POV or the left stick.
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedLeft(int joystick, boolean pressed);

		/**
		 * Pressing right on either POV or the left stick.
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedRight(int joystick, boolean pressed);

		/**
		 * Pressing the first L button
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedL1(int joystick, boolean pressed);

		/**
		 * Pressing the first R button
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedR1(int joystick, boolean pressed);

		/**
		 * Pressing the second L button
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedL2(int joystick, boolean pressed);

		/**
		 * Pressing the second R button
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedR2(int joystick, boolean pressed);

		/**
		 * Pressing a button on the joystick.
		 * 
		 * @param joystock
		 *            Which joystick in {@link #joysticks} this button press
		 *            originated from.
		 * @param button
		 *            The button we pressed, numbered 1-10, changes based on
		 *            controller type.
		 * @param pressed
		 *            Whether the button is being pushed or, rather, released.
		 */
		public abstract void pressedButton(int joystick, int button, boolean pressed);
	}
}

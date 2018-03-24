package com.github.vegeto079.ngcommontools.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

/**
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: When FPS is more than 10% below our expected rate (
 *          {@link #ticksPerSecond}), no forced sleeping time will happen for
 *          the next frame. This may cause the game's frame rate to temporarily
 *          run faster than {@link #ticksPerSecond} until it averages back to at
 *          least 90% of the expected rate.
 * @version 1.02: Updated Javadocs of mouse-related event handler calls.
 * @version 1.03: {@link RepaintThread} no longer controls
 *          {@link #paint(Graphics)}, rather, a new class, {@link #paintThread}
 *          does. This allows the two to be on different cores, improving
 *          performance significantly.
 * @version 1.031: Screenshots to imgur are now under
 *          {@link Logger.LogLevel.WARNING} instead of
 *          {@link Logger.LogLevel.NORMAL} so they will always show.
 * @version 1.04: Added {@link Pause} and the ability to 'pause' various threads
 *          by temporarily not running their respected methods (but not
 *          literally pausing the threads, which can produce unexpected
 *          results). Use {@link #setPause(Pause)} to actively change
 *          {@link #pause}.
 * @version 1.05: Added {@link #fpsCache}, {@link #upsCache},
 *          {@link #getFpsCache()}, and {@link #getUpsCache()}, so we can access
 *          fps/ups without requiring constantly calling
 *          {@link Tools#getAverage(ArrayList)} (which takes CPU time).
 * @version 1.051: Added {@link #stopThread()}.
 * @version 1.06: We now toggle through resized modes, helpful with multiple
 *          monitor setups. ALT+number (1-9) can now be used to set how many
 *          monitors are in our setup, the default is 1.
 * @version 1.07: Instead of using OS to get frame decoration size, we now get
 *          it by the actual size. Game is now guaranteed to start on top of
 *          other windows.
 * @version 1.08: Added {@link #recentlyStarted()} to determine if this instance
 *          is more than 2 seconds old. If it isn't, we will sleep like normal
 *          regardless of UPS/FPS performance when ticking, since the average
 *          will not have been established yet. Before this, when the Game
 *          started, it would tick very fast until the average UPS/FPS reached
 *          {@link #ticksPerSecond} and {@link #paintTicksPerSecond}.
 * @version 1.081: Argument "ups:#" or "fps:#" - where # is a number - added.
 *          Allows the user to select their own framerate for the paint thread,
 *          mainly useful if the framerate is unstable at the default values on
 *          a per-user basis.
 * @version 1.082: ALT+# - where # is a number - in
 *          {@link #keyPressed(KeyEvent)} no longer goes through to
 *          {@link #keyPressed(int)}. Changed log messages for
 *          {@link #firstLoad()}; it now has a starting and ending log messages
 *          for debugging. Operating System variable removed, irrelevant as of
 *          version 1.07.
 * @version 1.083: {@link #fps} size now limited to 5.
 * @version 1.084: <b>mouseDragged(int, int, int)</b> changed to
 *          {@link #mouseDragged(int, int, int, int, int)} to show at which
 *          point the drag began. Added {@link #mouseDragOrigin} to store this
 *          information.
 * @version 1.09: {@link #getFps()} and {@link #getUps()} no longer rely on
 *          calculating averages with {@link Tools#getAverage(ArrayList)}.
 *          Instead, a constant tally is kept as {@link #fpsTotal} and
 *          {@link #upsTotal} which is far less resource intensive.
 * @version 1.091: Now if ups is underperforming we will reset it half of the
 *          time to make sure our ups value is correct - hopefully we won't
 *          overperform too much now.
 * @version 1.092: Underperforming adjustment removed. It's just too unreliable,
 *          we don't ever want the game to run faster than the requested ups. If
 *          a better solution comes up we can re-instate this, but we'd have to
 *          somehow make sure the ups never goes above requested during
 *          adjustments.
 * @version 1.093: Above also removed for paint thread.
 * @version 1.094: Added {@link #argsOneLine} which takes the place of old
 *          {@link #args}. Now, <b>args</b> is simply set from first running.
 * @version 1.095: Added {@link #keyReleased(int)}, similar to
 *          {@link #keyPressed(int)}.
 * @version 1.1: {@link #dontResize} now properly works, and allows fullscreen,
 *          as well as multi-monitor fullscreen support.
 * @version 1.101: Removed Title being affected by args.
 * @version 1.102: Added MouseWheelListener,
 *          {@link #mouseWheelMoved(int, int, int)}, and
 *          {@link #mouseWheelMoved(MouseWheelEvent)}.
 * @version 1.103: If window size is larger than screen size, instead of trying
 *          to center the frame on the screen, we just set it to 0,0.
 * @version 1.104: Revised sleeping patterns for main game and paint thread.
 * @version 1.105: Added {@link #sleep(long)}.
 * @version 1.106: Added {@link #getWidth()} and {@link #getHeight()}.
 * @version 1.107: Added {@link #isRunning()}.
 * @version 1.108: Added setFocusTraversalKeysEnabled(false) to {@link #jFrame}
 *          and <b>this</b> so that TAB events are handled by the KeyListener.
 * @version 1.109: {@link #mouseDragged(MouseEvent)} now properly passes on the
 *          right button pressed.
 */
@SuppressWarnings("serial")
public abstract class Game extends JComponent
		implements Runnable, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
	/**
	 * Custom {@link Logger} used to display messages with this {@link Game}.
	 */
	public Logger logger = null;
	/**
	 * How many times per second we want to run the {@link Game}'s game logic.
	 * <br>
	 * Note: this is separate from {@link Game#paintTicksPerSecond}.
	 */
	public int ticksPerSecond = 24;
	/**
	 * How many times per second we want to run the {@link Game}'s paint logic.
	 * <br>
	 * Note: this is separate from {@link Game#ticksPerSecond}.
	 */
	public int paintTicksPerSecond = 60;
	/**
	 * Whether or not this {@link Game} is currently on and running. If turned
	 * to <b>false</b> while running, the {@link Game} will end.
	 */
	private boolean running = false;
	/**
	 * The value for this {@link Game}'s width.
	 */
	private int width = -1;
	/**
	 * The value for this {@link Game}'s height.
	 */
	private int height = -1;
	/**
	 * The window title that the main {@link JFrame} of this {@link Game} has.
	 */
	private String title = "";
	/**
	 * Our current {@link Pause} status.
	 */
	public Pause pause = Pause.NONE;

	public JFrame jFrame = null;

	protected double resizeX = 1;
	protected double resizeY = 1;

	public Resizer resizer = null;
	protected Thread resizerThread = null;
	protected Thread paintThread = null;
	public BufferedImage painted = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	private ArrayList<Float> fps = new ArrayList<Float>();
	private int fpsTotal;
	private ArrayList<Float> ups = new ArrayList<Float>();
	private int upsTotal;
	private long lastSecondFps;
	private float tickCountFps = 0;
	private long lastSecondUps;
	private float tickCountUps = 0;
	public int tickcounter = 0;
	public int fadecounter = 0;
	public int fadecounterenabled = 0;

	protected boolean altPressed = false;
	protected boolean enterPressed = false;
	protected boolean isFullscreen = false;
	protected int monitorAmt = 1;

	public String[] args = null;
	public String argsOneLine = null;
	public boolean dontResize = false;

	public boolean allowScreenshots = true;
	public ScreenshotThread screenshotThread = null;

	public long seed = (long) (Math.random() * 1000000d);
	public Random random = new Random(seed);
	public long randomCount = 0;
	private RuntimeMXBean runetimeMXBean = null;

	protected Point mouseDragOrigin;

	/**
	 * Initates a {@link Game}.
	 * 
	 * @param logger
	 *            See {@link Game#logger}.
	 * @param args
	 *            Arguments from to be interpreted by
	 *            {@link #mainSuper(String[])}.
	 * @param ticksPerSecond
	 *            See {@link Game#ticksPerSecond}.
	 * @param paintTicksPerSecond
	 *            See {@link Game#paintTicksPerSecond}.
	 * @param title
	 *            See {@link Game#title}.
	 * @param width
	 *            See {@link Game#width}.
	 * @param height
	 *            See {@link Game#height}.
	 */
	public Game(Logger logger, String[] args, int ticksPerSecond, int paintTicksPerSecond, String title, int width,
			int height) {
		this.logger = logger;
		this.ticksPerSecond = ticksPerSecond;
		this.paintTicksPerSecond = paintTicksPerSecond;
		runetimeMXBean = ManagementFactory.getRuntimeMXBean();
		if (ticksPerSecond > 1000 || paintTicksPerSecond > 1000 || ticksPerSecond < 1 || paintTicksPerSecond < 1) {
			try {
				throw new IllegalArgumentException("ticksPerSecond and paintTicksPerSecond must be >=1 && <=1000.");
			} catch (IllegalArgumentException uoe) {
				uoe.printStackTrace();
			}
			System.exit(1);
		}
		this.title = title;
		if (width > 0 && height > 0) {
			JFrame frame = new JFrame();
			frame.pack();
			Insets insets = frame.getInsets();
			frame.dispose();
			frame = null;
			logger.log(LogLevel.DEBUG, "[Game] Got insets: " + insets);
			width += insets.left - 2;
			height += insets.top - 2;
		} else {
			logger.log(LogLevel.DEBUG, "Less than one width or height. Disabling display!");
			dontResize = true;
		}
		this.width = width;
		this.height = height;
		logger.log(LogLevel.DEBUG, "Running from jar: " + ResourceHandler.runningFromJar());
		if (ResourceHandler.runningFromJar()) {
			// We are running from a jar, extract all res files
			ResourceHandler.extractAll(logger,
					new java.io.File(Game.class.getProtectionDomain().getCodeSource().getLocation().getPath())
							.getName());
		}
		argsOneLine = null;
		if (args != null && args.length > 0)
			argsOneLine = args[0];
		for (int i = 0; args != null && i < args.length; i++) {
			logger.log(LogLevel.DEBUG, "Checking argument: " + args[i]);
			if (i > 0)
				argsOneLine += " " + args[1];
			if (args[i].contains("noResize")) {
				logger.log(LogLevel.DEBUG, "noResize found");
				dontResize = true;
			} else if (args[i].contains("noScreenshots")) {
				logger.log(LogLevel.DEBUG, "noScreenshots found");
				allowScreenshots = false;
			} else if (args[i].toLowerCase().startsWith("ups:")) {
				logger.log(LogLevel.DEBUG, "ups custom value found");
				try {
					this.ticksPerSecond = Integer.parseInt(args[i].split(":")[1]);
				} catch (Exception e) {
					logger.log("ups is not a number as expected");
				}
			} else if (args[i].toLowerCase().startsWith("fps:")) {
				logger.log(LogLevel.DEBUG, "fps custom value found");
				try {
					this.paintTicksPerSecond = Integer.parseInt(args[i].split(":")[1]);
				} catch (Exception e) {
					logger.log("fps is not a number as expected");
				}
			}
		}
		this.args = args;
		logger.log(LogLevel.DEBUG, "Initiated Game.");
	}

	public void stopThread() {
		logger.log(LogLevel.DEBUG, "Ran stopThread()");
		running = false;
		if (resizer != null) {
			resizer.stopThread();
		}
		if (jFrame != null) {
			jFrame.setVisible(false);
			jFrame.dispose();
		}
	}

	public void startThread() {
		logger.log(LogLevel.DEBUG, "Ran startThread()");
		if (!running) {
			logger.log(LogLevel.DEBUG, "Time to run!");
			running = true;
			if (width <= 0 || height <= 0) {
				logger.log(LogLevel.DEBUG, "Not going to turn on display because width or height is less than 1.");
				logger.log(LogLevel.DEBUG, "Game ready to go (no display). Starting thread.");
				new Thread(this, "Game Logic Thread").start();
				return;
			} else {
				// if (args != null)
				// jFrame = new JFrame(title + " " + args);
				// else
				jFrame = new JFrame(title);
				jFrame.setContentPane(this);
				jFrame.setResizable(false);
				jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				Dimension size = new Dimension(width, height);

				jFrame.setPreferredSize(size);
				jFrame.setMinimumSize(size);
				setPreferredSize(size);
				setMinimumSize(size);
				setFocusTraversalKeysEnabled(false);
				jFrame.setFocusTraversalKeysEnabled(false);

				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				if (screenSize.width < jFrame.getWidth() || screenSize.height < jFrame.getHeight())
					jFrame.setLocation(0, 0);
				else
					jFrame.setLocation((screenSize.width - jFrame.getWidth()) / 2,
							(screenSize.height - jFrame.getHeight()) / 2);

				jFrame.pack();
				jFrame.setAlwaysOnTop(true);
				jFrame.setVisible(true);
				jFrame.setAlwaysOnTop(false);
				setVisible(true);
				jFrame.setVisible(true);
			}
			logger.log(LogLevel.DEBUG, "Game ready to go. Starting thread.");
			new Thread(this, "Game Logic Thread").start();
			while (getWidth() + 50 < width) {
				// System.out.println(width + "," + height + "," + getWidth()
				// + "," + getHeight());
				sleep(5);
			}
			if (!dontResize) {
				resizer = new Resizer(this, title, getWidth(), getHeight());
				resizer.init();
				if (width > 0 && height > 0) {
					resizerThread = new Thread(new Resizer(this, "Resizer", 0, 0).new ResizerThread(this),
							"Resizer Thread");
					resizerThread.start();
				}
			} else {
				if (jFrame != null) {
					jFrame.addKeyListener(this);
					jFrame.addMouseListener(this);
					jFrame.addMouseMotionListener(this);
					jFrame.addMouseWheelListener(this);
				}
			}
			if (width > 0 && height > 0) {
				paintThread = new Thread(new PaintThread(), "Paint Thread");
				paintThread.start();
			}
		}
	}

	protected void firstLoad() {
		logger.log(LogLevel.DEBUG, "firstLoad()");
	}

	/**
	 * The method called by the game thread. Override this and create all of
	 * your game-related methods here, but remember to super call it.<br>
	 * If not called by the overriding class, {@link #ups} will not be properly
	 * calculated, meaning {@link #getUps()} will return erroneous values,
	 * possibly causing the game thread to act abnormal as it depends on it.<br>
	 * {@link #tickcounter} and {@link #screenshotThread} also depend on this
	 * method.
	 */
	protected void gameTick() {
		// logger.log(LogLevel.DEBUG, "gameTick()");
		if (screenshotThread != null) {
			if (!screenshotThread.ran)
				screenshotThread.run();
			else if (!screenshotThread.isAlive())
				screenshotThread = null;
		}
		if (tickcounter == 99)
			tickcounter = 0;
		else
			tickcounter++;
		addUps();
	}

	/**
	 * The method called by the paint thread. Override this and create all of
	 * your paint-related methods here.<br>
	 * Overriding this method however is not required (like {@link #gameTick()})
	 * as this method currently doesn't do anything by itself: necessary paint
	 * methods are instead handled automatically through
	 * {@link #paint(Graphics)} (which is not to be overridden).
	 */
	protected void paintTick(Graphics2D g) {

	}

	public void paint(Graphics gOld) {
		if (jFrame == null)
			return;
		super.paint(gOld);
		if (gOld == null)
			return;
		// BufferedImage paintTo = new BufferedImage(getWidth(), getHeight(),
		// BufferedImage.TYPE_INT_ARGB);
		BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		boolean stopResize = false;
		double newResizeX = 0;
		try {
			newResizeX = (double) resizer.frame.getWidth() / (double) width;
		} catch (Exception e) {
			stopResize = true;
		}
		if (!stopResize) {
			double newResizeY = (double) resizer.frame.getHeight() / (double) height;
			if (newResizeX < 1)
				newResizeX = 1;
			if (newResizeY < 1)
				newResizeY = 1;
			resizeX = newResizeX;
			resizeY = newResizeY;
		}
		Color tempColor = g.getColor();
		g.setColor(Color.BLACK);
		g.fillRect(-10, -10, getWidth() + 10, getHeight() + 10);
		g.setColor(tempColor);
		if (pause.paint()) {
			gOld.drawImage(painted, 0, 0, null);
		} else {
			paintTick(g);
			gOld.drawImage(img, 0, 0, null);
			painted = img;
		}
		addFps();
		Toolkit.getDefaultToolkit().sync();
		g.dispose();
		gOld.dispose();
		img.flush();
		img = null;
	}

	public void run() {
		logger.log(LogLevel.DEBUG, "Starting game thread!");

		logger.log(LogLevel.DEBUG, "Calling firstLoad()");
		firstLoad();

		int lastGameTick = -1;
		// int renderedFrames = 0;

		double time = System.nanoTime() / 1000000000.0;
		double now = time;
		double averagePassedTime = 0;

		boolean naiveTiming = true;
		while (running) {
			double lastTime = time;
			time = System.nanoTime() / 1000000000.0;
			double passedTime = time - lastTime;

			if (passedTime < 0)
				naiveTiming = false; // Stop relying on nanotime if it starts
			// skipping around in time (ie running
			// backwards at least once). This
			// sometimes happens on dual core amds.
			averagePassedTime = averagePassedTime * 0.9 + passedTime * 0.1;

			if (naiveTiming) {
				now = time;
			} else {
				now += averagePassedTime;
			}
			int gameTick = (int) (now * ticksPerSecond);
			if (lastGameTick == -1)
				lastGameTick = gameTick;
			while (lastGameTick < gameTick) {
				lastGameTick++;
				if (pause.game()) {
					addUps();
				} else {
					gameTick();
				}
			}
			sleep(1);
			try {
				// if (!pause.game() && getUps() > ticksPerSecond)
				// Thread.sleep((long) (1000d / (double) ticksPerSecond));
			} catch (Exception e) {
			}
		}

	}

	/**
	 * Do not override this method, only override
	 * {@link #mouseDragged(int, int, int, int, int)}.
	 */
	@Deprecated
	public void mouseDragged(MouseEvent e) {
		if (mouseDragOrigin == null)
			mouseDragOrigin = e.getPoint();
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		int button = 0;
		if (SwingUtilities.isLeftMouseButton(e))
			button = 1;
		if (SwingUtilities.isRightMouseButton(e))
			button = 3;
		if (SwingUtilities.isMiddleMouseButton(e))
			button = 2;
		mouseDragged(x, y, button, mouseDragOrigin.x, mouseDragOrigin.y);
	}

	/**
	 * Do not override this method, only override
	 * {@link #mouseMoved(int, int, int)}.
	 */
	@Deprecated
	public void mouseMoved(MouseEvent e) {
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		mouseMoved(x, y, e.getButton());
	}

	/**
	 * Do not override this method, only override
	 * {@link #mouseClicked(int, int, int)}.
	 */
	@Deprecated
	public void mouseClicked(MouseEvent e) {
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		mouseClicked(x, y, e.getButton());
	}

	/**
	 * Do not override this method, only override
	 * {@link #mousePressed(int, int, int)}.
	 */
	@Deprecated
	public void mousePressed(MouseEvent e) {
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		mousePressed(x, y, e.getButton());
	}

	/**
	 * Do not override this method, only override
	 * {@link #mouseReleased(int, int, int)}.
	 */
	@Deprecated
	public void mouseReleased(MouseEvent e) {
		mouseDragOrigin = null;
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		mouseReleased(x, y, e.getButton());
	}

	/**
	 * Do not override this method, only override
	 * {@link #mouseEntered(int, int, int)}.
	 */
	@Deprecated
	public void mouseEntered(MouseEvent e) {
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		mouseEntered(x, y, e.getButton());
	}

	/**
	 * Do not override this method, only override
	 * {@link #mouseExited(int, int, int)}.
	 */
	@Deprecated
	public void mouseExited(MouseEvent e) {
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		mouseExited(x, y, e.getButton());
	}

	/**
	 * Do not override this method, only override
	 * {@link #mouseWheelMoved(int, int, int)}.
	 */
	@Deprecated
	public void mouseWheelMoved(MouseWheelEvent e) {
		int x = e.getPoint().x;
		int y = e.getPoint().y;
		if (dontResize) {
			x -= jFrame.getInsets().left - jFrame.getInsets().right;
			y -= jFrame.getInsets().top - jFrame.getInsets().bottom;
		}
		mouseWheelMoved(x, y, e.getWheelRotation());
	}

	/**
	 * @param x
	 *            Current x position of the drag.
	 * @param y
	 *            Current y position of the drag.
	 * @param button
	 *            Which button pressed during the drag.
	 * @param xOrigin
	 *            Original x position of the drag.
	 * @param yOrigin
	 *            Original y position of the drag.
	 */
	public void mouseDragged(int x, int y, int button, int xOrigin, int yOrigin) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void mouseMoved(int x, int y, int button) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void mouseClicked(int x, int y, int button) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void mousePressed(int x, int y, int button) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void mouseReleased(int x, int y, int button) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void mouseEntered(int x, int y, int button) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void mouseExited(int x, int y, int button) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void mouseWheelMoved(int x, int y, int wheelRotation) {
		// To be overridden by an extension of this Game class if necessary.
	}

	public void keyTyped(KeyEvent e) {
		// To be overridden by an extension of this Game class if necessary.
	}

	/**
	 * Do not override this method, only override {@link #keyPressed(int)}.
	 */
	@Deprecated
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		boolean throughKey = true;
		int monitorAmtBefore = monitorAmt;
		if (code == KeyEvent.VK_ALT)
			altPressed = true;
		if (code == KeyEvent.VK_ENTER)
			enterPressed = true;
		if (altPressed && enterPressed) {
			throughKey = false;
			if (dontResize) {
				// logger.err(LogLevel.WARNING, "Cannot go fullscreen in
				// noResize mode.");
				// Tools.displayDialog("Cannot go fullscreen in noResize
				// mode.");
				Resizer.toggleFullscreenNoResize(this);
				altPressed = false;
				enterPressed = false;
			} else {
				resizer.toggleFullscreen(this);
				altPressed = false;
				enterPressed = false;
			}
		}
		// Adjust monitor amount with ALT+number. When monitorAmt>1, alt+enter
		// will cycle through the separate monitors.
		else if (altPressed && code == KeyEvent.VK_1)
			monitorAmt = 1;
		else if (altPressed && code == KeyEvent.VK_2)
			monitorAmt = 2;
		else if (altPressed && code == KeyEvent.VK_3)
			monitorAmt = 3;
		else if (altPressed && code == KeyEvent.VK_4)
			monitorAmt = 4;
		else if (altPressed && code == KeyEvent.VK_5)
			monitorAmt = 5;
		else if (altPressed && code == KeyEvent.VK_6)
			monitorAmt = 6;
		else if (altPressed && code == KeyEvent.VK_7)
			monitorAmt = 7;
		else if (altPressed && code == KeyEvent.VK_8)
			monitorAmt = 8;
		else if (altPressed && code == KeyEvent.VK_9)
			monitorAmt = 9;
		if (monitorAmtBefore != monitorAmt) {
			logger.log(LogLevel.NORMAL, "Monitor Amount set to " + monitorAmt);
			throughKey = false;
		}
		if (throughKey)
			keyPressed(code);
	}

	/**
	 * To be overridden by an extension of this Game class if necessary.<br>
	 * Keep in mind that usage of ALT commands is not recommended, as this is
	 * the key used for built-in Game commands and may interfere.<br>
	 * 
	 * @param key
	 *            keyCode of the key pressed.
	 */
	public void keyPressed(int key) {
	}

	/**
	 * Do not override this method, only override {@link #keyReleased(int)}.
	 */
	@Deprecated
	public void keyReleased(KeyEvent e) {
		boolean throughKey = true;
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			altPressed = false;
			throughKey = false;
		}
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			enterPressed = false;
			throughKey = false;
		}
		if (e.getKeyCode() == KeyEvent.VK_PRINTSCREEN && altPressed && allowScreenshots) {
			screenshotThread = new ScreenshotThread();
			throughKey = false;
		}
		if (throughKey)
			keyReleased(e.getKeyCode());
	}

	/**
	 * To be overridden by an extension of this Game class if necessary.<br>
	 * Keep in mind that usage of ALT commands is not recommended, as this is
	 * the key used for built-in Game commands and may interfere.<br>
	 * 
	 * @param key
	 *            keyCode of the key pressed.
	 */
	public void keyReleased(int key) {
	}

	public class ScreenshotThread extends Thread {
		public boolean ran = false;

		public void run() {
			ran = true;
			logger.log(LogLevel.WARNING, "Sending screenshot to Imgur.");
			String url = Tools.uploadToImgur(painted, logger);
			logger.log(LogLevel.WARNING, "Screenshot taken: " + url);
			try {
				// Desktop.getDesktop().browse(new URI(url));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
				logger.log(LogLevel.WARNING, "Screenshot copied to clipboard.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addFps() {
		while (fps.size() > 5) {
			int ran = Tools.random(null, 0, fps.size() - 1, null);
			fpsTotal -= fps.get(ran);
			fps.remove(ran);
		}
		float multiply = 1f;

		// if (lastSecond + 4999999995f < System.nanoTime()) {
		if (lastSecondFps + 999999999f * multiply < System.nanoTime()) {
			// Common.log(this,"tickCount before: " + tickCount);
			tickCountFps = tickCountFps / multiply;
			// Common.log(this,"tickCount: " + tickCount);

			lastSecondFps = System.nanoTime();

			if (tickCountFps != 0) {
				fps.add(tickCountFps);
				fpsTotal += tickCountFps;
			}
			tickCountFps = 0;
		}
		tickCountFps++;
	}

	public void addUps() {
		while (ups.size() > 5) {
			int ran = Tools.random(null, 0, ups.size() - 1, null);
			upsTotal -= ups.get(ran);
			ups.remove(ran);
		}
		float multiply = 1f;

		// if (lastSecond + 4999999995f < System.nanoTime()) {
		if (lastSecondUps + 999999999f * multiply < System.nanoTime()) {
			// Common.log(this,"tickCount before: " + tickCount);
			tickCountUps = tickCountUps / multiply;
			// Common.log(this,"tickCount: " + tickCount);

			lastSecondUps = System.nanoTime();

			if (tickCountUps != 0) {
				ups.add(tickCountUps);
				upsTotal += tickCountUps;
			}
			tickCountUps = 0;
		}
		tickCountUps++;
	}

	public int getFps() {
		if (fps.size() == 0)
			return 0;
		return fpsTotal / fps.size();
	}

	public int getUps() {
		if (ups.size() == 0)
			return 0;
		return upsTotal / ups.size();
	}

	public void resetFps() {
		fps = new ArrayList<Float>();
		fpsTotal = 0;
	}

	public void resetUps() {
		ups = new ArrayList<Float>();
		upsTotal = 0;
	}

	public void setSeed(long seed, long randomCount) {
		if (this.seed == seed && this.randomCount == randomCount)
			return;
		this.seed = seed;
		this.randomCount = 0;
		random = new Random(seed);
		while (this.randomCount < randomCount)
			Tools.random(this, "Set seed, going to right randomCount value.");
	}

	public class PaintThread extends Thread {
		public void run() {
			logger.log(LogLevel.DEBUG, "Starting paint thread!");

			int lastPaintTick = -1;
			// int renderedFrames = 0;

			double time = System.nanoTime() / 1000000000.0;
			double now = time;
			double averagePassedTime = 0;

			boolean naiveTiming = true;
			while (running) {
				double lastTime = time;
				time = System.nanoTime() / 1000000000.0;
				double passedTime = time - lastTime;

				if (passedTime < 0)
					naiveTiming = false;
				// Stop relying on nanotime if it starts
				// skipping around in time (ie running
				// backwards at least once). This
				// sometimes happens on dual core amds.
				averagePassedTime = averagePassedTime * 0.9 + passedTime * 0.1;

				if (naiveTiming) {
					now = time;
				} else {
					now += averagePassedTime;
				}
				int paintTick = (int) (now * paintTicksPerSecond);
				if (lastPaintTick == -1)
					lastPaintTick = paintTick;
				while (lastPaintTick < paintTick) {
					Graphics g = painted.getGraphics();
					if (dontResize)
						repaint();
					else
						paint(g);
					g.dispose();
					g = null;
					lastPaintTick++;
					Toolkit.getDefaultToolkit().sync();
				}
				Tools.sleep(1);
				try {
					// if (!pause.paint() && getUps() > paintTicksPerSecond)
					// Thread.sleep((long) (1000d / (double)
					// paintTicksPerSecond));
				} catch (Exception e) {
				}
			}

		}
	}

	/**
	 * Used to represent different pausing types in a {@link Game}.
	 * 
	 * @author Nathan
	 * 
	 */
	public static enum Pause {
		/**
		 * No pausing in effect.
		 */
		NONE,
		/**
		 * Game thread ({@link Game#gameTick()}) will be paused.
		 */
		GAME,
		/**
		 * Paint thread ({@link Game#paintTick()}) will be paused.<br>
		 * Note that this does not pause a {@link Resizer} thread.
		 */
		PAINT,
		/**
		 * Both game ({@link Game#gameTick()}) and paint thread (
		 * {@link Game#paintTick()}) will be paused.<br>
		 * Note that this does not pause a {@link Resizer} thread.
		 */
		ALL;
		/**
		 * @return Whether or not this {@link Pause} value indicates that we
		 *         should pause the game thread ({@link Game#gameTick()}).
		 */
		public boolean game() {
			return equals(Pause.GAME) || equals(Pause.ALL);
		}

		/**
		 * @return Whether or not this {@link Pause} value indicates that we
		 *         should pause the paint thread ({@link Game#paintTick()}).
		 */
		public boolean paint() {
			return equals(Pause.PAINT) || equals(Pause.ALL);
		}

		/**
		 * @return Whether or not this {@link Pause} value indicates that we
		 *         should pause both the game thread ({@link Game#gameTick()})
		 *         and the paint thread ({@link Game#paintTick()}).
		 */
		public boolean all() {
			return equals(Pause.ALL);
		}
	}

	/**
	 * Sets {@link #pause} to <b>pauseValue</b>.
	 * 
	 * @param pauseValue
	 *            The type of {@link Pause} to set.
	 */
	public void setPause(Pause pauseValue) {
		pause = pauseValue;
	}

	/**
	 * @return Whether or not this instance of Game was only recently created -
	 *         ie. less than 2 seconds ago.
	 */
	public boolean recentlyStarted() {
		return System.currentTimeMillis() - runetimeMXBean.getStartTime() < 2000;
	}

	/**
	 * Calls {@link Tools#sleep(long)}.
	 */
	public void sleep(long millis) {
		Tools.sleep(millis);
	}

	/**
	 * If {@link JFrame#getWidth()} is available, this returns that value.
	 * Otherwise, returns {@link #width}.
	 */
	@Override
	public int getWidth() {
		if (super.getWidth() == 0)
			return width;
		else
			return super.getWidth();
	}

	/**
	 * If {@link JFrame#getHeight()} is available, this returns that value.
	 * Otherwise, returns {@link #height}.
	 */
	@Override
	public int getHeight() {
		if (super.getHeight() == 0)
			return height;
		else
			return super.getHeight();
	}

	/**
	 * @return {@link #running}.
	 */
	public boolean isRunning() {
		return running;
	}
}

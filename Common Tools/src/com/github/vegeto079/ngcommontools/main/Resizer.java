package com.github.vegeto079.ngcommontools.main;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

/**
 * Handles displaying a resizable version of a {@link Game} window.
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: originalImage null-check added to
 *          {@link #resize(Image, double, double)}.
 * @version 1.02: {@link ResizerThread#run()} now skips sleeping if we are
 *          underperforming, similar to the paint thread.
 * @version 1.03: {@link #running} and {@link #stopThread()} added, so we can be
 *          safely stopped.
 * @version 1.04: Dynamically grabs window decoration size.
 *          {@link #toggleFullscreen(Game)} added (instead of setFullscreen()),
 *          and the way we handle changing fullscreen changed greatly. We now
 *          cycle between fullscreen modes (useful for multiple monitors, as set
 *          by {@link Game#monitorAmt}). JFrame is guaranteed to be on top on
 *          start and when changing modes.
 * @version 1.05: Uses {@link Game#recentlyStarted()} to determine if this
 *          instance is more than 2 seconds old. If it isn't, we will sleep like
 *          normal regardless of FPS performance when ticking, since the average
 *          will not have been established yet. Before this, when the Game
 *          started, it would tick very fast until the average FPS reached
 *          {@link Game#paintTicksPerSecond}.
 * @version 1.051: Now, when using multi-monitor setups with
 *          {@link #toggleFullscreen(Game)}, the screen will be always on top.
 * @version 1.052: Removed underperforming ticking, too unreliable.
 * @version 1.06: Added {@link #toggleFullscreenNoResize(Game)} for toggling
 *          fullscreen when {@link Game#dontResize} is <b>true</b>.
 * @version 1.061: Messing around with other ways of changing into fullscreen.
 * @version 1.062: {@link #resize(Image, int, int)} used to take doubles, now
 *          takes ints, since it was just casting them to int anyway.
 * @version 1.063: Added MouseWheelListener and
 *          {@link #mouseWheelMoved(MouseWheelEvent)}.
 * @version 1.064: If window size is larger than screen size, instead of trying
 *          to center the frame on the screen, we just set it to 0,0.
 * @version 1.065: Revised sleeping patterns for main thread.
 * @version 1.066: Added setFocusTraversalKeysEnabled(false) to {@link #frame}
 *          so that TAB events are handled by the KeyListener.
 * @version 1.067: {@link #mouseDragged(MouseEvent)} now properly passes on the
 *          right button pressed.
 */
public class Resizer extends JComponent implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
	private static final long serialVersionUID = 1L;

	private int width = 0, height = 0, trueWidth = 0, trueHeight = 0;
	private BufferedImage image = null;
	public JFrame frame = null;
	private Game game = null;
	private String title = null;
	private boolean running = false;

	public Resizer(Game game, String title, int width, int height) {
		this.game = game;
		this.title = title;
		this.width = width;
		this.height = height;
	}

	public void init() {
		setFocusable(true);
		setEnabled(true);

		trueWidth = width;
		trueHeight = height;
		JFrame testframe = new JFrame();
		testframe.pack();
		Insets insets = testframe.getInsets();
		testframe.dispose();
		testframe = null;
		game.logger.log(LogLevel.DEBUG, "[Resizer] Got insets: " + insets);
		trueWidth += insets.left + insets.right;
		trueHeight += insets.bottom + insets.top;
		Dimension size = new Dimension(width, height);
		setPreferredSize(size);
		setSize(size);
		// setMaximumSize(size);
		Dimension minSze = new Dimension(trueWidth, trueHeight);
		setMinimumSize(minSze);

		frame = new JFrame(title);
		frame.setContentPane(this);
		frame.setMinimumSize(minSze);
		frame.setMaximumSize(getToolkit().getScreenSize());
		frame.pack();
		frame.setResizable(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setFocusTraversalKeysEnabled(false);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.width < frame.getWidth() || screenSize.height < frame.getHeight())
			frame.setLocation(0, 0);
		else
			frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);

		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		game.jFrame.setVisible(false);
		game.jFrame.dispose();
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);
		frame.setAlwaysOnTop(false);
	}

	public static boolean toggleFullscreenNoResize(Game game) {
		game.jFrame.dispose();
		game.jFrame.setVisible(false);
		boolean undecorated = true;
		int startX = 0, startY = 0, width = game.getWidth(), height = game.getHeight();
		if (game.monitorAmt > 1) {
			System.out.println("width: " + width);
			Point p = game.jFrame.getLocation();
			System.out.println("p: " + p);
			int addX = 0;
			int gameWidth = width / game.monitorAmt;
			if (game.isFullscreen)
				addX = gameWidth;
			System.out.println("addX: " + addX);
			if (game.jFrame.getWidth() == game.getWidth()) {
				System.out.println("Starting monitor view 1");
				width = width / game.monitorAmt;
				game.isFullscreen = true;
				game.jFrame.setExtendedState(JFrame.NORMAL);
				game.jFrame.setAlwaysOnTop(true);
			} else if (p.x + addX >= game.getWidth()) {
				System.out.println("Getting out of fullscreen");
				undecorated = false;
				game.isFullscreen = false;
				game.jFrame.setExtendedState(JFrame.NORMAL);
				game.jFrame.setAlwaysOnTop(false);
			} else if (!game.isFullscreen) {
				System.out.println("Going to fullscreen");
				startX = 0;
				game.isFullscreen = true;
				// frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
				game.jFrame.setAlwaysOnTop(false);
			} else {
				System.out.println("Moving to the right");
				width = width / game.monitorAmt;
				startX = p.x + addX;
				game.isFullscreen = true;
				game.jFrame.setExtendedState(JFrame.NORMAL);
				game.jFrame.setAlwaysOnTop(true);
			}
		} else {
			game.isFullscreen = !game.isFullscreen;
			game.jFrame.setAlwaysOnTop(false);
		}
		if (game.isFullscreen) {
			System.out
					.println("startX: " + startX + ", startY: " + startY + ", width: " + width + ", height: " + height);
			// frame.setMaximizedBounds(new Rectangle(0, 0, width, height));
			// frame.setBounds(startX, startY, width, height);
			game.jFrame.setSize(width, height);
			game.jFrame.setLocation(startX, startY);
		} else {
			game.jFrame.setSize(game.jFrame.getMinimumSize());
			game.jFrame.setMaximizedBounds(null);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			if (screenSize.width > game.jFrame.getWidth() && screenSize.height > game.jFrame.getHeight())
				game.jFrame.setLocation(0, 0);
			else
				game.jFrame.setLocation((screenSize.width - game.jFrame.getWidth()) / 2,
						(screenSize.height - game.jFrame.getHeight()) / 2);
			undecorated = false;
		}
		game.jFrame.setUndecorated(undecorated);
		// frame.pack();
		boolean alwaysOnTop = game.jFrame.isAlwaysOnTop();
		if (!alwaysOnTop)
			game.jFrame.setAlwaysOnTop(true);
		game.jFrame.setVisible(true);
		game.jFrame.setAlwaysOnTop(alwaysOnTop);
		return game.isFullscreen;

	}

	GraphicsDevice graphicsDevice;

	public boolean toggleFullscreen(Game game) {
		frame.dispose();
		frame.setVisible(false);
		boolean resizable = false, undecorated = true;
		Dimension maxBounds = getToolkit().getScreenSize();
		int startX = 0, startY = 0, width = maxBounds.width, height = maxBounds.height;
		if (game.monitorAmt > 1) {
			// TODO: fix
			System.out.println("maxBounds.width: " + maxBounds.width);
			Point p = frame.getLocation();
			System.out.println("p: " + p);
			int addX = 0;
			int gameWidth = width / game.monitorAmt;
			if (game.isFullscreen)
				addX = gameWidth;
			System.out.println("addX: " + addX);
			if (frame.getWidth() == maxBounds.width) {
				System.out.println("Starting monitor view 1");
				width = width / game.monitorAmt;
				game.isFullscreen = true;
				frame.setExtendedState(JFrame.NORMAL);
				frame.setAlwaysOnTop(true);
			} else if (p.x + addX >= maxBounds.width) {
				System.out.println("Getting out of fullscreen");
				resizable = true;
				undecorated = false;
				game.isFullscreen = false;
				frame.setExtendedState(JFrame.NORMAL);
				frame.setAlwaysOnTop(false);
			} else if (!game.isFullscreen) {
				System.out.println("Going to fullscreen");
				startX = 0;
				game.isFullscreen = true;
				// frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
				frame.setAlwaysOnTop(false);
			} else {
				System.out.println("Moving to the right");
				width = width / game.monitorAmt;
				startX = p.x + addX;
				game.isFullscreen = true;
				frame.setExtendedState(JFrame.NORMAL);
				frame.setAlwaysOnTop(true);
			}
		} else {
			game.isFullscreen = !game.isFullscreen;
			resizable = !game.isFullscreen;
			undecorated = !resizable;
			frame.setAlwaysOnTop(false);
		}
		if (game.isFullscreen) {
			System.out
					.println("startX: " + startX + ", startY: " + startY + ", width: " + width + ", height: " + height);
			// frame.setMaximizedBounds(new Rectangle(0, 0, width, height));
			// frame.setBounds(startX, startY, width, height);
			// frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			frame.setUndecorated(true);
			frame.setResizable(false);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(frame);
			return true;
			// frame.setSize(width, height);
			// frame.setLocation(startX, startY);
		} else {
			frame.setExtendedState(JFrame.NORMAL);
			frame.setSize(frame.getMinimumSize());
			// frame.setMaximizedBounds(null);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
		}
		frame.setResizable(resizable);
		frame.setUndecorated(undecorated);
		// frame.pack();
		boolean alwaysOnTop = frame.isAlwaysOnTop();
		if (!alwaysOnTop)
			frame.setAlwaysOnTop(true);
		frame.setVisible(true);
		frame.setAlwaysOnTop(alwaysOnTop);
		return game.isFullscreen;
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (image != null)
			g.drawImage(resize(image, getWidth(), getHeight()), 0, 0, null);
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	public static BufferedImage resize(Image originalImage, int scaledWidth, int scaledHeight) {
		BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaledBI.createGraphics();
		g.setComposite(AlphaComposite.Src);
		// g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		// RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		// g.setRenderingHint(RenderingHints.KEY_RENDERING,
		// RenderingHints.VALUE_RENDER_QUALITY);
		// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
		g.dispose();
		if (originalImage != null)
			originalImage.flush();
		originalImage = null;
		return scaledBI;
	}

	/**
	 * @return A screenshot of the {@link Game}.
	 */
	public Image screenshot() {
		BufferedImage bufImage = new BufferedImage(game.getWidth(), game.getHeight(), BufferedImage.TYPE_INT_RGB);
		game.paint(bufImage.createGraphics());
		return toImage(bufImage);
	}

	private static Image toImage(BufferedImage bufferedImage) {
		return Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
	}

	@SuppressWarnings("deprecation")
	public void keyPressed(KeyEvent e) {
		game.keyPressed(e);
	}

	@SuppressWarnings("deprecation")
	public void keyReleased(KeyEvent e) {
		game.keyReleased(e);
	}

	public void keyTyped(KeyEvent e) {
		game.keyTyped(e);
	}

	public void mouseDragged(MouseEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		if (game.mouseDragOrigin == null)
			game.mouseDragOrigin = new Point(p);
		int button = 0;
		if (SwingUtilities.isLeftMouseButton(e))
			button = 1;
		if (SwingUtilities.isRightMouseButton(e))
			button = 3;
		if (SwingUtilities.isMiddleMouseButton(e))
			button = 2;
		game.mouseDragged(p.x, p.y, button, game.mouseDragOrigin.x, game.mouseDragOrigin.y);
	}

	public void mouseMoved(MouseEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		game.mouseMoved(p.x, p.y, e.getButton());
	}

	public void mouseClicked(MouseEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		game.mouseClicked(p.x, p.y, e.getButton());
	}

	public void mousePressed(MouseEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		game.mousePressed(p.x, p.y, e.getButton());
	}

	public void mouseReleased(MouseEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		game.mouseDragOrigin = null;
		game.mouseReleased(p.x, p.y, e.getButton());
	}

	public void mouseEntered(MouseEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		game.mouseEntered(p.x, p.y, e.getButton());
	}

	public void mouseExited(MouseEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		game.mouseExited(p.x, p.y, e.getButton());
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		double wChange = (double) game.getWidth() / (double) getWidth();
		double hChange = (double) game.getHeight() / (double) getHeight();
		Point p = new Point((int) (e.getPoint().x * wChange), (int) (e.getPoint().y * hChange));
		game.mouseWheelMoved(p.x, p.y, e.getWheelRotation());
	}

	public class ResizerThread extends Thread {
		Game game = null;

		public ResizerThread(Game game) {
			this.game = game;
		}

		public void run() {
			int lastPaintTick = -1;
			double time = System.nanoTime() / 1000000000.0;
			double now = time;
			double averagePassedTime = 0;
			boolean naiveTiming = true;
			running = true;
			while (running) {
				// new Logger(true).log("YES running");
				double lastTime = time;
				time = System.nanoTime() / 1000000000.0;
				double passedTime = time - lastTime;

				if (passedTime < 0)
					naiveTiming = false; // Stop relying on nanotime if it
											// starts
				// skipping around in time (ie running
				// backwards at least once). This
				// sometimes happens on dual core amds.
				averagePassedTime = averagePassedTime * 0.9 + passedTime * 0.1;

				if (naiveTiming) {
					now = time;
				} else {
					now += averagePassedTime;
				}

				int paintTick = (int) (now * game.paintTicksPerSecond);
				if (lastPaintTick == -1)
					lastPaintTick = paintTick;
				while (lastPaintTick < paintTick) {
					if (game.pause.paint()) {
						// do nothing
					} else {
						paintTick();
					}
					lastPaintTick++;
				}
				try {
					Thread.sleep(1);
				} catch (Exception e) {
				}
				try {
					if (!game.pause.paint() && game.getFps() > game.paintTicksPerSecond) {
						Thread.sleep((long) (1000d / (double) game.paintTicksPerSecond));
					}
				} catch (Exception e) {
				}
			}
		}
	}

	private void paintTick() {
		if (game.painted.getWidth(null) != 1) {
			// game.repaint();
			if (game.resizer != null) {
				// Graphics g = game.painted.getGraphics();
				// game.paint(g); direct to another thread
				// g.dispose();
				// g = null;
				if (game.painted != null) {
					if (game.resizer.image != null) {
						game.resizer.image.flush();
						game.resizer.image = null;
					}
					game.resizer.setImage(game.painted);
				}
				game.resizer.repaint();
			} else
				game.repaint();
		}
	}

	public void stopThread() {
		running = false;
		frame.setVisible(false);
		frame.dispose();
	}
}

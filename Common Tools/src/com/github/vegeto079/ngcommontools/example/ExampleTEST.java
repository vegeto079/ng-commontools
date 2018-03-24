package com.github.vegeto079.ngcommontools.example;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import com.github.vegeto079.ngcommontools.main.DoublePoint;
import com.github.vegeto079.ngcommontools.main.Game;
import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;
import com.github.vegeto079.ngcommontools.networking.Client;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork;
import com.github.vegeto079.ngcommontools.networking.Server;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork.P2PClientMessageHandler;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork.P2PServerMessageHandler;

@SuppressWarnings("serial")
public class ExampleTEST extends Game {

	static ExampleTEST game = null;
	boolean buttonPressed = false;
	Color insideColor = Color.WHITE;
	final int ovalWidth = 10, ovalHeight = 10;

	int clientNum = -1;
	public final int MAX_CLIENTS = 4;
	boolean setup = false;
	boolean serverSetup = false;
	Server server;
	Client client;

	long connectionSpeed = 1000;// ms
	
	int port = 1000;

	DoublePoint[] location = new DoublePoint[MAX_CLIENTS];
	double[] up = new double[MAX_CLIENTS], down = new double[MAX_CLIENTS], left = new double[MAX_CLIENTS],
			right = new double[MAX_CLIENTS];
	P2PNetwork p2pNetwork;

	List<String> usernames = null;
	smHandler SMHandler = new smHandler();
	cmHandler CMHandler = new cmHandler();

	public ExampleTEST(Logger logger, String[] args, int ticksPerSecond, int paintTicksPerSecond, String title,
			int width, int height) {
		super(logger, args, ticksPerSecond, paintTicksPerSecond, title, width, height);
	}

	public static void main(String[] args) {
		game = new ExampleTEST(new Logger(LogLevel.NORMAL, LogLevel.WARNING, LogLevel.ERROR), args, 60, 120,
				"Example Game", 300, 300);
		game.startThread();
	}

	@Override
	public void firstLoad() {
		super.firstLoad();
		for (int i = 0; i < location.length; i++) {
			location[i] = new DoublePoint(getWidth() / 2 - ovalWidth / 2, getHeight() / 2 - ovalHeight / 2);
		}
		logger.log(LogLevel.NORMAL, "Waiting for client number selection. Press #0-" + (MAX_CLIENTS - 1) + ".");
	}

	@Override
	public void paintTick(Graphics2D g) {
		super.paintTick(g);
		for (int i = 0; i < location.length; i++) {
			if (location == null || location[i] == null)
				continue;
			g.setColor(insideColor);
			g.fillOval((int) location[i].x - ovalWidth / 2, (int) location[i].y - ovalHeight / 2, ovalWidth,
					ovalHeight);
			g.setColor(Color.WHITE);
			g.drawOval((int) location[i].x - ovalWidth / 2, (int) location[i].y - ovalHeight / 2, ovalWidth,
					ovalHeight);
		}
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
		DoublePoint before = new DoublePoint(location[0]);
		for (int i = 0; i < up.length; i++) {
			if (up[i] > 0) {
				location[i].adjust(0, -up[i]);
				up[i] = 0;
			}
			if (down[i] > 0) {
				location[i].adjust(0, down[i]);
				down[i] = 0;
			}
			if (left[i] > 0) {
				location[i].adjust(-left[i], 0);
				left[i] = 0;
			}
			if (right[i] > 0) {
				location[i].adjust(right[i], 0);
				right[i] = 0;
			}
			if (location[i].x < -6)
				location[i].adjust(getWidth(), 0);
			if (location[i].x > getWidth() + 6)
				location[i].adjust(-getWidth(), 0);
			if (location[i].y < -6)
				location[i].adjust(0, getHeight());
			if (location[i].y > getHeight() + 6)
				location[i].adjust(0, -getHeight());
		}
		if (before.distance(location[0]) != 0)
			moveNetwork(location[0]);
		if (SMHandler == null)
			SMHandler = new smHandler();
		if (CMHandler == null)
			CMHandler = new cmHandler();
		if (!setup && clientNum != -1) {
			setup = true;
			if (clientNum == 0) {
				server = new Server(SMHandler, logger, port, connectionSpeed, 10000, 1, "Example Server");
				server.openIncomingClientConnection();
			} else {
				client = new Client(CMHandler, logger, connectionSpeed, "client #" + clientNum, SMHandler, CMHandler);
				client.connectToServer("localhost", port, -1);
			}
		} else if (setup && client != null && client.getP2PNetwork() != null) {
			client.getP2PNetwork().serverMessageHandler = SMHandler;
			client.getP2PNetwork().clientMessageHandler = CMHandler;
		}
		if (!serverSetup && server != null && server.getConnectedClientAmt() == MAX_CLIENTS - 1) {
			serverSetup = true;
			server.closeIncomingClientConnection();
			server.startClientTalkingConnections();
			p2pNetwork = server.changeToP2P(SMHandler, CMHandler,port+1);
			// try {
			// p2pNetwork = P2PNetwork.startP2PNetwork(server, logger,
			// connectionSpeed, SMHandler, CMHandler);
			// } catch (Exception e) {
			// }
		}
	}

	public class smHandler extends P2PServerMessageHandler {

		@Override
		public void process(String message) {
			logger.log("smHandler got a message! " + message + " passing to super.");
			super.process(message);
			logger.log("smHandler back in action! Time to take this message ourselves.");
			doProcess(message);
		}

	}

	public class cmHandler extends P2PClientMessageHandler {

		@Override
		public void process(String message) {
			logger.log("cmHandler got a message! (" + message + ") passing to super.");
			super.process(message);
			logger.log("cmHandler back in action! Time to take this message ourselves.");
			doProcess(message);
		}

	}

	private void doProcess(String message) {
		logger.log(LogLevel.DEBUG, "TEST Got message: " + message);
		boolean foundUsername = false;
		String user = message.split(Client.USERNAME_SPLITTER)[1];
		if (usernames == null) {
			usernames = new ArrayList<String>();
			logger.log(LogLevel.DEBUG, "Found new username (1): " + user);
			usernames.add(user);
		} else {
			for (int i = 0; i < usernames.size(); i++) {
				if (usernames.get(i).equals(user))
					foundUsername = true;
			}
			if (!foundUsername) {
				logger.log(LogLevel.DEBUG, "Found new username (" + (usernames.size() + 1) + "): " + user);
				usernames.add(user);
			}
		}
		if (message.contains("MOVE_US")) {
			String username = message.split(Client.USERNAME_SPLITTER)[1];
			logger.log(LogLevel.DEBUG, "Moving their oval! Username: " + username);
			for (int i = 0; i < usernames.size(); i++) {
				logger.log(LogLevel.DEBUG, "Checking username " + i + ": " + usernames.get(i));
				if (usernames.get(i).contains("client #") || usernames.get(i).contains("Example")) {
					logger.log(LogLevel.DEBUG, "Found username to kill!");
					usernames.remove(i);
					i = 0;
					continue;
				}
				if (username.equals(usernames.get(i))) {
					logger.log(LogLevel.DEBUG, "Username matches! Finding location x/y.");
					double x = Double.parseDouble(message.split(Client.SPLITTER)[1]);
					logger.log(LogLevel.DEBUG, "x found: " + x);
					double y = Double.parseDouble(message.split(Client.SPLITTER)[2].split(Client.USERNAME_SPLITTER)[0]);
					logger.log(LogLevel.DEBUG, "y found: " + y);
					location[i + 1] = new DoublePoint(x, y);
					logger.log(LogLevel.DEBUG, "Moved them!");
				}
			}
		}
	}

	@Override
	public void keyPressed(int keyCode) {
		buttonPressed = true;
		if (keyCode == KeyEvent.VK_UP) {
			up[0] += 5.5;
		} else if (keyCode == KeyEvent.VK_DOWN) {
			down[0] += 5.5;
		} else if (keyCode == KeyEvent.VK_LEFT) {
			left[0] += 5.5;
		} else if (keyCode == KeyEvent.VK_RIGHT) {
			right[0] += 5.5;
		}
		if (clientNum == -1) {
			if (keyCode == KeyEvent.VK_0)
				clientNum = 0;
			if (keyCode == KeyEvent.VK_1)
				clientNum = 1;
			if (keyCode == KeyEvent.VK_2)
				clientNum = 2;
			if (keyCode == KeyEvent.VK_3)
				clientNum = 3;
			if (clientNum != -1) {
				logger.log(LogLevel.NORMAL, "Client Number set to " + clientNum);
			}
		}
	}

	public void moveNetwork(DoublePoint loc) {
		logger.log(LogLevel.DEBUG, "Checking moveNetwork(" + loc + ")");
		if (p2pNetwork != null) {
			logger.log(LogLevel.DEBUG, "Telling P2PNetwork that we moved.");
			p2pNetwork.sendMessageToEveryone("MOVE_US" + Client.SPLITTER + loc.x + Client.SPLITTER + loc.y);
		} else {
			if (client.getP2PNetwork() != null)
				p2pNetwork = client.getP2PNetwork();
			else if (server.getP2PNetwork() != null)
				p2pNetwork = server.getP2PNetwork();
			if (p2pNetwork != null)
				moveNetwork(loc);
		}
	}

	@Override
	public void keyReleased(int keyCode) {
		super.keyReleased(keyCode);
		buttonPressed = false;
	}

}

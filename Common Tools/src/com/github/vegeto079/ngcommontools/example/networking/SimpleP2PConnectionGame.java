package com.github.vegeto079.ngcommontools.example.networking;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import com.github.vegeto079.ngcommontools.main.Game;
import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.Tools;
import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;
import com.github.vegeto079.ngcommontools.networking.Client;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork;
import com.github.vegeto079.ngcommontools.networking.Server;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork.P2PClientMessageHandler;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork.P2PServerMessageHandler;

@SuppressWarnings("serial")
public class SimpleP2PConnectionGame extends Game {
	static SimpleP2PConnectionGame us = null;
	Server server;
	Client client;
	int ourPos = -1;
	final int TOTAL_CLIENTS = 3;
	P2PNetwork p2pNetwork;
	smHandler SMHandler = new smHandler();
	cmHandler CMHandler = new cmHandler();
	long connectionSpeed = 1000;
	String display = "";
	String ip = null;
	boolean askingForIP = false;
	int port = 1000;

	public SimpleP2PConnectionGame(Logger logger, String[] args, int ticksPerSecond, int paintTicksPerSecond,
			String title, int width, int height) {
		super(logger, args, ticksPerSecond, paintTicksPerSecond, title, width, height);
	}

	public static void main(String[] args) {
		us = new SimpleP2PConnectionGame(new Logger(true), args, 60, 60, "Simple P2P Connection Game", 400, 400);
		for (int i = 0; i < args.length; i++)
			if (args[0].startsWith("ip:")) {
				us.ip = args[0].split(":")[1];
			}
		us.startThread();
	}

	public void firstLoad() {
		super.firstLoad();
		logger.log(LogLevel.NORMAL, "Please press 0-" + (TOTAL_CLIENTS - 1) + " to pick your position.");
	}

	public void gameTick() {
		if (askingForIP && ip == null)
			return;
		if (ourPos != -1 && server == null && client == null) {
			if (ourPos == 0) {
				server = new Server(SMHandler, logger, port, connectionSpeed, 10000, 1, "Example Server");
				server.openIncomingClientConnection();
			} else {
				if (ip == null) {
					askingForIP = true;
					ip = Tools.displayInputDialog("Type the IP address to connect to.");
					askingForIP = false;
				} else {
					client = new Client(CMHandler, logger, connectionSpeed, "client #" + ourPos, SMHandler, CMHandler);
					client.connectToServer(ip, port, -1);
				}
			}
		} else if (p2pNetwork == null && server != null && TOTAL_CLIENTS - server.getConnectedClientAmt() == 1) {
			server.closeIncomingClientConnection();
			server.startClientTalkingConnections();
			p2pNetwork = server.changeToP2P(SMHandler, CMHandler, port + 1);
		} else if (p2pNetwork == null && client != null && client.getP2PNetwork() != null) {
			p2pNetwork = client.getP2PNetwork();
		}
	}

	public void paintTick(Graphics2D g) {
		g.drawString(display, 50, 100);
	}

	public void keyPressed(int keyCode) {
		logger.log("keyPressed:" + keyCode + " (" + KeyEvent.getKeyText(keyCode) + ")");
		logger.log("p2pNetwork == null? " + (p2pNetwork == null));
		if (p2pNetwork != null)
			logger.log("p2pNetwork.isConnected()? " + (p2pNetwork.isConnected()));
		if (ourPos == -1) {
			if (keyCode == KeyEvent.VK_0) {
				ourPos = 0;
			}
			if (keyCode == KeyEvent.VK_1) {
				ourPos = 1;
			}
			if (keyCode == KeyEvent.VK_2) {
				ourPos = 2;
			}
			if (keyCode == KeyEvent.VK_3) {
				ourPos = 3;
			}
		} else if (p2pNetwork != null && p2pNetwork.isConnected()) {
			logger.log("sending!");
			p2pNetwork.sendMessageToEveryone("KEY:" + KeyEvent.getKeyText(keyCode));
			display = KeyEvent.getKeyText(keyCode);
		}
	}

	public class smHandler extends P2PServerMessageHandler {

		@Override
		public void process(String message) {
			super.process(message);
			doProcess(message);
		}

	}

	public class cmHandler extends P2PClientMessageHandler {

		@Override
		public void process(String message) {
			super.process(message);
			doProcess(message);
		}

	}

	public void doProcess(String message) {
		if (message.startsWith("KEY:")) {
			display = message.split(":")[1];
		}
	}

}

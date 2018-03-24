package com.github.vegeto079.ngcommontools.example.networking;

import com.github.vegeto079.ngcommontools.main.Game;
import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.Tools;
import com.github.vegeto079.ngcommontools.networking.Client;
import com.github.vegeto079.ngcommontools.networking.Server;
import com.github.vegeto079.ngcommontools.networking.Client.ClientMessageHandler;
import com.github.vegeto079.ngcommontools.networking.Server.ServerMessageHandler;

@SuppressWarnings("serial")
public class SimplestConnectionTest extends Game {


	public SimplestConnectionTest(Logger logger, String[] args, int ticksPerSecond, int paintTicksPerSecond,
			String title, int width, int height) {
		super(logger, args, ticksPerSecond, paintTicksPerSecond, title, width, height);
	}

	public static void main(String[] args) {
		SimplestConnectionTest t = new SimplestConnectionTest(new Logger(true), args, 5, 5, null, 0, 0);
		t.startThread();
		int port = 1000;
		int num = Integer.parseInt(Tools.displayInputDialog("Type 0 for server, 1 for client"));
		if (num == 0) {
			Server server = new Server(new OurServerMessageHandler(), t.logger, port, 1500, -1, 1, "MasterServer");
			server.openIncomingClientConnection();
			int tries = 100;
			while (server.getConnectedClientAmt() == 0 && tries >= 0) {
				try {
					Thread.sleep(100);
					tries--;
				} catch (Exception e) {
				}
			}
			if (tries < 2)
				Tools.displayDialog("Failed to connect.");
			else
				Tools.displayDialog("Connected to Client (" + server.getHandlers().get(0).getIP() + ","
						+ server.getHandlers().get(0).getName() + ") successfully. Shutting down.");
		} else if (num == 1) {
			Client client = new Client(new OurClientMessageHandler(), t.logger, 1500, "SlaveClient");
			client.connectToServer(Tools.displayInputDialog("Type IP address"), port, -1);
			int tries = 100;
			while (!client.isConnected() && tries >= 0) {
				try {
					Thread.sleep(100);
					tries--;
				} catch (Exception e) {
				}
			}
			if (tries < 2)
				Tools.displayDialog("Failed to connect.");
			else
				Tools.displayDialog("Connected to Server (" + client.getConnector().getIP() + ","
						+ client.getTheirName() + ") successfully. Shutting down.");
		}
		System.exit(1);
	}

	public static class OurServerMessageHandler extends ServerMessageHandler {

		public void process(String message) {
		}

	}

	public static class OurClientMessageHandler extends ClientMessageHandler {

		public void process(String message) {
		}

	}
}

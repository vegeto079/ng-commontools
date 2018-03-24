package com.github.vegeto079.ngcommontools.networking;

import java.util.ArrayList;
import java.util.List;

import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;
import com.github.vegeto079.ngcommontools.networking.Client.ClientMessageHandler;
import com.github.vegeto079.ngcommontools.networking.Server.ServerMessageHandler;

/**
 * An extension of the {@link Server} and {@link Client} classes, using them in
 * tandem to create a web of connectivity between them, as in a peer-to-peer
 * network.<br>
 * <br>
 * This class relies on a Server that is already connected to every Client we
 * wish to build the peer-to-peer web with. Using those existing connections, we
 * tell them to shut down what they are doing and start a p2p network.
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Fully setup as it seems to work now.
 * @version 1.02: Lots of stuff.
 * 
 */
public class P2PNetwork {
	/**
	 * The IP addresses of all Client/Server connections. Established by
	 * {@link #P2PNetwork(String)}.
	 */
	private List<String> IPs = new ArrayList<String>();
	/**
	 * Logger passed on from the Client or Server.
	 */
	private Logger logger = null;
	/**
	 * Our place in the P2P network in reference to the others in the network. Helps
	 * identify who we need to connect to and how.
	 */
	private int ourID;
	/**
	 * The amount of clients that are going to be in this network.
	 */
	private int maxClients;
	/**
	 * The starting port for connections. We will use ports starting at this number
	 * and raising.
	 */
	private int port = -1;
	/**
	 * The currently connecting number, as related to {@link #ourIndex}.
	 */
	private int establishingConnectionNum = -2;
	/**
	 * The server in which other P2PNetworks connect to this one, if applicable.
	 */
	private Server server;
	/**
	 * A list of Clients that we will use to connect to other P2PNetworks.
	 */
	private ArrayList<Client> clients;
	/**
	 * A list alongside {@link #clients} that has each of their respective
	 * {@link #ourID}s.
	 */
	private ArrayList<Integer> clientIndentification;
	/**
	 * @see {@link Server#timeBetweenConnectionAttempts}
	 */
	private long timeBetweenConnectionAttempts = 100;

	// any lower connection attempts than 100ms and this doesn't seem to connect
	// properly...
	/**
	 * Whether or not we are currently attempting to connect to other
	 * Clients/Servers, therefore, whether or not {@link #connect()} is currently
	 * running.
	 */
	private boolean connecting;
	/**
	 * Whether or not the Peer-to-Peer connection is fully connected and set up.
	 */
	private boolean connected;
	/**
	 * When using P2PNetwork, the server message handler must be made as a
	 * {@link P2PServerMessageHandler} and passed through this class to function
	 * properly.
	 */
	public P2PServerMessageHandler serverMessageHandler;
	/**
	 * When using P2PNetwork, the client message handler must be made as a
	 * {@link P2PClientMessageHandler} and passed through this class to function
	 * properly.
	 */
	public P2PClientMessageHandler clientMessageHandler;

	/**
	 * Sets up this P2PNetwork.
	 * 
	 * @param IPlist
	 *            IP addresses (as sent by {@link #startP2PNetwork(Server, Logger)})
	 *            to connect to.
	 * @param startingPort
	 *            The starting port - multiple connections will use the ports
	 *            following this port.
	 * @param logger
	 * @param timeBetweenConnectionAttempts
	 *            {@link #timeBetweenConnectionAttempts}
	 * @param serverMessageHandler
	 *            {@link #serverMessageHandler}
	 * @param clientMessageHandler
	 *            {@link #clientMessageHandler}
	 */
	public P2PNetwork(String IPlist, int startingPort, Logger logger, long timeBetweenConnectionAttempts,
			P2PServerMessageHandler serverMessageHandler, P2PClientMessageHandler clientMessageHandler) {
		logger.log(LogLevel.DEBUG, "New P2PNetwork instance created. IPlist: " + IPlist);
		port = startingPort;
		this.logger = logger;
		this.timeBetweenConnectionAttempts = timeBetweenConnectionAttempts;
		if (serverMessageHandler != null)
			this.serverMessageHandler = serverMessageHandler;
		else
			this.serverMessageHandler = new defaultServerMessageHandler();
		if (clientMessageHandler != null)
			this.clientMessageHandler = clientMessageHandler;
		else
			this.clientMessageHandler = new defaultClientMessageHandler();
		logger.log(LogLevel.DEBUG, "Client and Server Message Handlers 50% set.");
		this.serverMessageHandler.p2pnetwork = this;
		this.clientMessageHandler.p2pnetwork = this;
		logger.log(LogLevel.DEBUG, "Client and Server Message Handlers 100% set.");
		ourID = Integer.parseInt(IPlist.split("==--==")[1]);
		maxClients = Integer.parseInt(IPlist.split("==--==")[2]);
		String[] list = IPlist.split("==--==")[0].split("@@@");
		for (int i = 0; i < list.length; i++) {
			logger.log(LogLevel.DEBUG, "Adding IP to list: " + list[i]);
			IPs.add(list[i]);
		}
		logger.log(LogLevel.DEBUG, "P2PNetwork initiated, starting connectors.");
		startConnectors();
	}

	/**
	 * Used by the original {@link Server} to tell all {@link Client}s to start
	 * connecting via p2p. Tells all connected Clients to start connecting this way,
	 * closing their original connections to us, while closing our own original
	 * Server.
	 * 
	 * @param originalHost
	 *            The Server with all Clients already attached.
	 * @param startingPort
	 *            The starting port - multiple connections will use the ports
	 *            following this port.
	 * @param logger
	 * @param timeBetweenConnectionAttempts
	 *            {@link #timeBetweenConnectionAttempts}
	 * @param serverMessageHandler
	 *            {@link #serverMessageHandler}
	 * @param clientMessageHandler
	 *            {@link #clientMessageHandler}
	 * @return P2PNetwork that the <b>originalHost</b> will refer to.
	 * @throws Exception
	 *             If p2p network was not successfully restored.
	 * @see {@link Server#changeToP2P()}.
	 */
	public static P2PNetwork startP2PNetwork(Server originalHost, int startingPort, Logger logger,
			long timeBetweenConnectionAttempts, P2PServerMessageHandler serverMessageHandler,
			P2PClientMessageHandler clientMessageHandler) throws Exception {
		logger.log(LogLevel.DEBUG, "Starting new P2P Network!");
		List<Server.Handler> handlers = originalHost.getHandlers();
		String IPaddresses = null;
		for (Server.Handler handler : handlers) {
			if (IPaddresses == null)
				IPaddresses = handler.getIP();
			else
				IPaddresses += "@@@" + handler.getIP();
		}
		IPaddresses += "==--==0==--==" + (handlers.size() + 1);
		List<String> ips = new ArrayList<String>();
		for (Server.Handler handler : handlers)
			ips.add(handler.getIP());
		for (int i = 0; i < handlers.size(); i++) {
			String allIPsExceptHandler = "MOVE_TO_P2P:";
			for (int j = 0; j < ips.size(); j++)
				if (j != i)
					allIPsExceptHandler += "@@@" + ips.get(j);
			// Add 1 to every i (will be set to each P2PNetwork's ourID), so
			// that the Server (this instance) can be 0.
			handlers.get(i).sendMessageToClient(allIPsExceptHandler + "==--==" + (i + 1) + "==--=="
					+ (handlers.size() + 1) + Client.USERNAME_SPLITTER + originalHost.username);
		}
		int attempts = 1000;
		boolean allHandlersTalkedTo = false;
		boolean[] talkedToHandler = new boolean[handlers.size()];
		while (attempts > 0 && !allHandlersTalkedTo) {
			attempts--;
			logger.log(LogLevel.DEBUG, "Waiting on Handlers to get the message across.. Attempt #" + (1000 - attempts));
			allHandlersTalkedTo = true;
			for (int i = 0; i < handlers.size(); i++) {
				if (talkedToHandler[i])
					continue;
				talkedToHandler[i] = true;
				List<String> queue = handlers.get(i).getQueue();
				for (String str : queue)
					if (str.contains("MOVE_TO_P2P")) {
						logger.log(LogLevel.DEBUG, "We haven't sent the message to handler #" + i + " yet! Waiting..");
						talkedToHandler[i] = false;
						allHandlersTalkedTo = false;
					}
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
		}
		String exitString;
		if (attempts == 0) {
			exitString = "FATAL ERROR: P2P Network could not be restored.";
		} else if (allHandlersTalkedTo) {
			logger.log(LogLevel.DEBUG,
					"All handlers talked to successfully! P2P Network framework ready to go. Killing Server.");
			originalHost.disconnect();
			return new P2PNetwork(IPaddresses, startingPort, logger, timeBetweenConnectionAttempts,
					serverMessageHandler, clientMessageHandler);
		} else {
			exitString = "FATAL ERROR: P2P Network encounted an unknown problem.";
		}
		logger.log(LogLevel.ERROR, exitString);
		System.err.println(exitString);
		throw new Exception(exitString);
	}

	/**
	 * Establishes all the connectors for this P2PNetwork. This must be ran first.
	 */
	public void startConnectors() {
		logger.log(LogLevel.DEBUG, "Starting connectors..");
		establishingConnectionNum = maxClients - 1;
		int tempPort = port + ourID;
		clients = new ArrayList<Client>();
		clientIndentification = new ArrayList<Integer>();
		for (int i = 0; i < maxClients; i++)
			clientIndentification.add(-1);
		if (ourID != 0) { // First client is only client to have no server;
							// we connect to everyone via clients.
			server = new Server(serverMessageHandler, logger, tempPort, timeBetweenConnectionAttempts, -1, 1,
					"P2PNetwork" + ourID + "server");
			String clientsToConnectTo = "";
			for (int i = 0; i < maxClients; i++)
				if (i == ourID || i >= ourID)
					continue;
				else
					clientsToConnectTo += i + ", ";
			logger.log(LogLevel.DEBUG, "Created server connector (on port " + tempPort + ") for Client(s): "
					+ clientsToConnectTo.substring(0, clientsToConnectTo.length() - 2));
		} else {
			logger.log(LogLevel.DEBUG, "NOT creating server because we're the first client (only non-server client).",
					"We'll connect to everyone via clients, will not handle incoming connections as a server.");
		}
		for (int i = 0; i < maxClients; i++) {
			// All clients below us must connect to our server
			// And we connect to all clients above us
			if (i == ourID || i < ourID)
				continue;
			Client client = new Client(clientMessageHandler, logger, timeBetweenConnectionAttempts,
					"P2PNetworkID" + ourID + "toClient" + i);
			clients.add(client);
			logger.log(LogLevel.DEBUG, "Created client connector to Client " + i);
		}
		if (clients.size() == 0)
			logger.log(LogLevel.DEBUG, "No client connectors created as we're the last client.",
					"Server connectors created, our P2P Network ready to go.");
		else
			logger.log(LogLevel.DEBUG,
					"Client " + (ourID != 0 ? "and Server " : "") + "connectors created, our P2P Network ready to go.");
		connect();
	}

	/**
	 * Attempts to connect to all other clients/servers. This must be ran several
	 * times to complete the Peer-to-Peer connection process, until
	 * {@link #establishingConnectionNum} becomes 0.
	 */
	public void connect() {
		if (connecting)
			return;
		connecting = true;
		logger.log(LogLevel.NORMAL, "Connecting...");
		if (establishingConnectionNum == ourID) {
			if (server != null) {
				server.openIncomingClientConnection();
				logger.log(LogLevel.DEBUG, "Incoming Client connections opened.");
				do {
					try {
						Thread.sleep(timeBetweenConnectionAttempts);
						// Wait for all Client connections
					} catch (Exception e) {
					}
				} while (server.getConnectedClientAmt() != ourID);
				if (server.getConnectedClientAmt() == ourID) {
					logger.log(LogLevel.NORMAL,
							"Connected to all clients (" + server.getConnectedClientAmt() + " total)");
					server.closeIncomingClientConnection();
					establishingConnectionNum--;
					server.sendMessageToAllClients("ESTABLISHING_CONNECTION_NUM:" + establishingConnectionNum
							+ Client.USERNAME_SPLITTER + server.username);
					for (int i = 0; i < clients.size(); i++)
						clients.get(i).sendMessageToServer("ESTABLISHING_CONNECTION_NUM:" + establishingConnectionNum);
					if (establishingConnectionNum == 0) {
						for (int i = 5; i >= 0; i--)
							logger.log(LogLevel.ERROR, "Successfully connected to everyone!");
						connected = true;
					} else
						logger.log(LogLevel.DEBUG,
								"Queued message to tell all clients and servers we're ready for the next set of p2p connections.");

				} else {
					logger.log(LogLevel.ERROR, "Didn't connect to all Clients? (" + server.getConnectedClientAmt() + "/"
							+ (ourID) + ") on port " + server.getPort());
				}
			}
		} else {
			if (clientIndentification.get(establishingConnectionNum) == -1) {
				for (int i = clients.size() - 1; i >= 0; i--)
					if (!clients.get(i).isConnected()) {
						clientIndentification.set(establishingConnectionNum, i);
						break;
					}
			}
			int clientIndexToUse = clientIndentification.get(establishingConnectionNum);
			if (clientIndexToUse == -1) {
				logger.log(LogLevel.ERROR, "clientIndexToUse == -1, no more open clients left.");
			} else if (clients.get(clientIndexToUse).isConnected()) {
				logger.log(LogLevel.DEBUG, "We're already connected, waiting on Server response.");
			} else {
				int clientPort = (port + establishingConnectionNum);
				logger.log(LogLevel.DEBUG,
						"Attempting to connect to Client[" + establishingConnectionNum + "] with clientIndex["
								+ clientIndexToUse + "] on port(" + clientPort + ") (already connecting:"
								+ clients.get(clientIndexToUse).isConnecting() + ")");
				if (!clients.get(clientIndexToUse).isConnecting()) {
					String ipAddress = IPs.get(0);
					logger.log(LogLevel.NORMAL,
							"Starting first connection attempt to Server (" + ipAddress + ", " + clientPort + ").");
					clients.get(clientIndexToUse).connectToServer(ipAddress, clientPort, -1);
				}
			}
		}
		connecting = false;
	}

	private static class defaultServerMessageHandler extends P2PServerMessageHandler {

		public void process(String message) {
			if (p2pnetwork != null)
				p2pnetwork.logger.log(LogLevel.DEBUG, "Default Server Message Handling.");
		}
	}

	private static class defaultClientMessageHandler extends P2PClientMessageHandler {
		public void process(String message) {
			if (p2pnetwork != null)
				p2pnetwork.logger.log(LogLevel.DEBUG, "Default Client Message Handling.");
		}
	}

	public abstract static class P2PServerMessageHandler extends ServerMessageHandler {
		/**
		 * The network to pass messages (and logs) through. Not required to be set,
		 * although before it gets set, nothing will get done.<br>
		 * <br>
		 * If extending this class, don't forget to call super!
		 */
		public P2PNetwork p2pnetwork = null;

		public void process(String message) {
			if (p2pnetwork != null) {
				p2pnetwork.logger.log(LogLevel.WARNING, "Got message from Client: " + message);
				p2pnetwork.processMessage(message);
			}
		}
	}

	public abstract static class P2PClientMessageHandler extends ClientMessageHandler {
		/**
		 * The network to pass messages (and logs) through. Not required to be set,
		 * although before it gets set, nothing will get done.<br>
		 * <br>
		 * If extending this class, don't forget to call super!
		 */
		public P2PNetwork p2pnetwork = null;

		public void process(String message) {
			if (p2pnetwork != null) {
				p2pnetwork.logger.log(LogLevel.WARNING, "Got message from Server: " + message);
				p2pnetwork.processMessage(message);
			}
		}
	}

	/**
	 * Processes messages from {@link P2PClientMessageHandler#process(String)} and
	 * {@link P2PServerMessageHandler#process(String)}.
	 * 
	 * @param message
	 */
	private void processMessage(String message) {
		logger.log(LogLevel.DEBUG, "Processing message: " + message);
		if (message.startsWith("ESTABLISHING_CONNECTION_NUM:")) {
			logger.log(LogLevel.DEBUG, "Establishing connection num detected");
			int num = Integer.parseInt(message.split(Client.USERNAME_SPLITTER)[0].split(":")[1]);
			if (num == 0) {
				for (int i = 5; i >= 0; i--)
					logger.log(LogLevel.ERROR, "Successfully connected to everyone!");
				connected = true;
			} else {
				establishingConnectionNum = num;
				logger.log(LogLevel.DEBUG,
						"Got word that everyone connected in this stage! Advancing to " + establishingConnectionNum);
				connect();
			}
		}
	}

	/**
	 * @return {@link #connected}.
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Sends a message to every connected client (from {@link #server}) and server
	 * (from {@link #clients}).
	 * 
	 * @param message
	 * @see {@link Server#sendMessageToAllClients(String)}
	 *      {@link Client#sendMessageToServer(String)}.
	 */
	public void sendMessageToEveryone(String message) {
		logger.log("P2PNetwork.sendMessageToEveryone()");
		if (server != null) {
			logger.log("P2PNetwork.sendingToClient");
			server.sendMessageToAllClients(message);
		}
		if (clients != null) {
			logger.log("P2PNetwork.sendingToServer");
			for (int i = 0; i < clients.size(); i++) {
				logger.log("P2PNetwork.sendingToClient(" + (i + 1) + "/" + clients.size() + ")");
				clients.get(i).sendMessageToServer(message);
			}
		}
	}

	/**
	 * Sends a <b>message</b> to every connected client (from {@link #server}) and
	 * server (from {@link #clients}), <i>excluding</i> any with names in
	 * <b>excludeNames</b>.
	 * 
	 * @param message
	 * @param excludedNames
	 * @see {@link #sendMessageToEveryone(String)}
	 *      {@link #sendMessageIncludeOnlyNames(String, String...)}.
	 */
	public void sendMessageExcludeNames(String message, String... excludedNames) {
		logger.log("P2PNetwork.sendMessageExcludeNames()");
		if (server != null) {
			logger.log("P2PNetwork.sendingToClient");
			server.sendMessageToAllClientsExcludingNames(message, excludedNames);
		}
		if (clients != null) {
			logger.log("P2PNetwork.sendingToServer");
			for (int i = 0; i < clients.size(); i++) {
				logger.log("P2PNetwork.sendingToClient(" + (i + 1) + "/" + clients.size() + ")");
				boolean exclude = false;
				Client client = clients.get(i);
				for (int j = 0; j < excludedNames.length; j++)
					if (excludedNames[j].equals(client.getTheirName()))
						exclude = true;
				if (!exclude)
					client.sendMessageToServer(message);
			}
		}
	}

	/**
	 * Sends a <b>message</b> to every connected client (from {@link #server}) and
	 * server (from {@link #clients}) that has a name that matches one of
	 * <b>includedNames</b>.
	 * 
	 * @param message
	 * @param excludedNames
	 * @see {@link #sendMessageToEveryone(String)}
	 *      {@link #sendMessageExcludeNames(String, String...)}.
	 */
	public void sendMessageIncludeOnlyNames(String message, String... includedNames) {
		logger.log("P2PNetwork.sendMessageIncludeOnlyNames()");
		if (server != null) {
			logger.log("P2PNetwork.sendingToClient");
			server.sendMessageToAllClientsIncludeOnlyNames(message, includedNames);
		}
		if (clients != null) {
			logger.log("P2PNetwork.sendingToServer");
			for (int i = 0; i < clients.size(); i++) {
				logger.log("P2PNetwork.sendingToClient(" + (i + 1) + "/" + clients.size() + ")");
				boolean include = false;
				Client client = clients.get(i);
				for (int j = 0; j < includedNames.length; j++)
					if (includedNames[j].equals(client.getTheirName()))
						include = true;
				if (include)
					client.sendMessageToServer(message);
			}
		}
	}

	/**
	 * @see {@link #ourID}.
	 * @return
	 */
	public int getOurID() {
		return ourID;
	}
}

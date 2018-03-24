package com.github.vegeto079.ngcommontools.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.github.vegeto079.ngcommontools.main.Game;
import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.Tools;
import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

/**
 * At attempt to create an easy-to-understand, well-documented Server used for
 * connecting to a {@link Client}, with games in mind.<br>
 * <br>
 * To start a new basic {@link Server}, initiate one with
 * {@link #Server(ServerMessageHandler, Logger, int, long, long)}. <br>
 * For the {@link ServerMessageHandler}, create a new class <b>extending</b> it,
 * and create {@link ServerMessageHandler#process(String)} how you want to
 * process incoming {@link Client} messages.<br>
 * <br>
 * For the {@link Logger}, just create a new temporary {@link Logger} that
 * displays what messages you want it to display.<br>
 * <br>
 * Once the {@link Server} is created, you can start asking for connections by
 * calling {@link #openIncomingClientConnection()} once. This will start a new
 * {@link Thread} where it looks for {@link Client}s.<br>
 * <br>
 * Once you have all wanted {@link Client}s connected, call
 * {@link #startClientTalkingConnections()} to start communication between the
 * {@link Server} and {@link Client}s.<br>
 * <br>
 * And that's all! All communication will be handled through the
 * {@link ServerMessageHandler} you created.<br>
 * Currently dropped connections are not re-established.
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Changed {@link #getConnectedClientAmt()} to use total amount
 *          of handlers, instead of adding one (originally with the idea of
 *          including oneself in the count).
 * @version 1.02: {@link ServerMessageHandler#preProcess(Client, String)} is
 *          void instead of boolean, as what it returned didn't affect anything.
 * @version 1.03: Added {@link #port} and {@link #getPort()}.
 * @version 1.031: Added {@link #removeDuplicateConnections}.
 * @version 1.04: Added {@link Handler#getQueue()} and {@link Handler#getIP()}.
 * @version 1.05: Added {@link #p2pNetwork}, {@link #changeToP2P()}, and
 *          {@link #getP2PNetwork()}.
 * @version 1.051: Removed 'connected' variable, as it wasn't being used.
 * @version 1.06: {@link #removeHandler(String)} deprecated, as removing one
 *          based on IP could result in the wrong Handler being removed if two
 *          Clients are connected with the same IP.
 *          {@link #removeHandler(Handler)} added to remove any given instance
 *          of Handler (instead of guessing based on IP), and
 *          {@link #removeHandler(int)} for exact placement in the list
 *          {@link #handlers}.
 * @version 1.061: Splitters now public and obtained from Client rather than
 *          hard-coded here as well.
 * @version 1.07: Various changes to code to make sure {@link P2PNetwork} works
 *          correctly. Now requires a username from the Client and refers to
 *          them using it (as well as IP).
 * @version 1.08: Added {@link #onExit()}.
 * @version 1.081: Variables changed from <b>private</b> to <b>protected</b> so
 *          that they can be accessed on a class extending Client.
 * @version 1.1: <b>waiting</b> variable removed. {@link #incomingConnector} and
 *          {@link #talkingConnector} added to replace generic <b>connector</b>
 *          variable. It is now possible to be connected to a {@link Client}
 *          while still having connections open for new Clients, therefore
 *          allowing quick jumping in and out of Clients.
 * @version 1.11: {@link #getHandlerIndex(String, String)} and
 *          {@link #removeHandler(String, String)} changed from just (String),
 *          no longer deprecated as they use IP and Username instead of just IP
 *          to get the Handler.
 * @version 1.12 {@link #listener} is now initiated in
 *          {@link #openIncomingClientConnection()} instead of
 *          {@link #Server(ServerMessageHandler, Logger, int, long, long, long, String)}
 *          . <b>listener</b> also now gets properly ended in
 *          {@link #disconnect()}, which should open up the port.
 * @version 1.13 Added {@link #getClientPing(Handler)} and
 *          {@link #getClientPing(String)}.
 * @version 1.14: {@link Handler#getPing()} now follows standards set by
 *          {@link Game} to use {@link Handler#pingTotal} instead of running
 *          costly {@link Tools#getAverage(ArrayList)} calls.
 * @version 1.141: {@link Handler#sendMessageToClient(String)} set to Public so
 *          it can be accessed by other classes.
 * @version 1.142: {@link #closeIncomingClientConnection()} now properly sets
 *          {@link #incomingConnector} to null.
 * @version 1.2: {@link #connect()} set to return a boolean on whether or not it
 *          connected to a Client. If it returns true, {@link Connector#run()}
 *          will keep running until it is false, to get all possible Clients.
 * @version 1.21 Added {@link Handler#pingListSize} and increased from 5 to 8.
 */
public class Server {

	/**
	 * Handles connection to all {@link Client}s.
	 */
	protected ServerSocket listener;
	/**
	 * Custom {@link ServerMessageHandler} used when receiving input from a
	 * {@link Client}.
	 */
	protected ServerMessageHandler messageHandler = null;
	/**
	 * Custom {@link Logger} used to display messages with this {@link Server}.
	 */
	protected Logger logger = null;
	/**
	 * A {@link List} of {@link Server.Handler}s that are connected to this
	 * {@link Server}.
	 */
	protected ArrayList<Handler> handlers = new ArrayList<Handler>();
	/**
	 * Whether or not we should end all {@link Client} connections.
	 */
	protected boolean stop = true;
	/**
	 * Whether or not to remove duplicate connections (based on IP) when detected.
	 */
	protected boolean removeDuplicateConnections = false;
	/**
	 * This instance of {@link Server}.
	 */
	protected Server me = null;
	/**
	 * {@link Connector} used in this {@link Server} to attempt to establish a
	 * connection with new {@link Client}s.
	 */
	protected Connector incomingConnector = null;
	/**
	 * {@link Connector} used in this {@link Server} to talk to connected
	 * {@link Client}s.
	 */
	protected Connector talkingConnector = null;
	/**
	 * The number of milliseconds between reattempting a connection via
	 * {@link #connectToServer(String, int)} when a failed connection occurs. <br>
	 * <br>
	 * Once a connection is established, this is the number of milliseconds between
	 * connections to the {@link Client}.
	 */
	protected long timeBetweenConnectionAttempts = -1;
	/**
	 * The number of milliseconds before we say that a {@link Client} is officially
	 * timed out from our {@link Server}, and remove them from the list of
	 * {@link Handler}s. -1 is indefinite.
	 */
	protected long clientTimeout = -1;
	/**
	 * Used to tell different parts of a message apart.
	 */
	public final static String SPLITTER = Client.SPLITTER;
	/**
	 * Used to tell the Server who this message is coming from.
	 */
	public final static String USERNAME_SPLITTER = Client.USERNAME_SPLITTER;
	/**
	 * Used to split several messages apart when using {@link #talkToClient()} so
	 * the {@link Client} can decode them separately. Also used by {@link Handler}
	 * to decode messages incoming from the {@link Client}.
	 */
	public final static String MESSAGE_SPLITTER = Client.MESSAGE_SPLITTER;
	/**
	 * Used to tell the {@link Client} that the message containing this @[link
	 * String} also contains a ping value, to determine latency.
	 */
	public final static String PING_SPLITTER = Client.PING_SPLITTER;
	/**
	 * Used to tell the {@link Client} that the message containing this
	 * {@link String} is a message meant to be read in a chatbox or similar, for
	 * viewing of persons. Also used by {@link Handler} for the {@link Server} to
	 * determine these same types of messages from the {@link Client}.
	 */
	private final static String CHAT_SPLITTER = Client.CHAT_SPLITTER;
	/**
	 * Username used to identify ourselves to the {@link Client}s so they know who
	 * the messages are coming from.
	 */
	public String username = null;
	/**
	 * Seed to keep all {@link Server}'s and {@link Client}'s
	 * {@link java.util.Random} class in sync.
	 */
	public long seed = -1;
	/**
	 * The port we are using to connect to {@link Client}s.
	 */
	protected int port = -1;

	/**
	 * Used when we move from a Client-Server structure to Peer-to-Peer
	 * connectivity. Can refer to this instead of any further Server references.
	 * 
	 * @see {@link #changeToP2P()}
	 */
	protected P2PNetwork p2pNetwork = null;
	/**
	 * The amount of time to sleep in-between unsuccessful {@link #connect()}
	 * attempts.
	 */
	public long sleepTime = 2000;

	/**
	 * Initiates {@link Server} and opens a {@link ServerSocket} on <b>port</b>.
	 * 
	 * @param messageHandler
	 *            See {@link #messageHandler}
	 * @param logger
	 *            See {@link #logger}
	 * @param port
	 *            The port we wish to accept connections from.
	 * @param timeBetweenConnectionAttempts
	 *            See {@link #timeBetweenConnectionAttempts}
	 * @param clientTimeout
	 *            See {@link #clientTimeout}
	 * @param seed
	 *            See {@link #seed}
	 * @param username
	 *            See {@link #username}
	 */
	public Server(ServerMessageHandler messageHandler, Logger logger, int port, long timeBetweenConnectionAttempts,
			long clientTimeout, long seed, String username) {
		this.messageHandler = messageHandler;
		this.logger = logger;
		this.port = port;
		this.timeBetweenConnectionAttempts = timeBetweenConnectionAttempts;
		this.clientTimeout = clientTimeout;
		this.seed = seed;
		this.username = username;
		if (port == 0)
			return;
		me = this;
	}

	/**
	 * @return Whether or not we are connected to any amount of {@link Client}s.
	 */
	public boolean isConnected() {
		return getConnectedClientAmt() > 0;
	}

	/**
	 * @return How many {@link Client}s we are connected to, determined by the size
	 *         of {@link #handlers}.
	 */
	public int getConnectedClientAmt() {
		return getHandlers().size();
	}

	/**
	 * @return {@link #handlers}.
	 */
	public ArrayList<Handler> getHandlers() {
		return handlers;
	}

	/**
	 * @see Server#port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Puts <b>message</b> for every {@link Handler} in {@link #handlers} into
	 * {@link #sendMessageToClient(String)}.
	 * 
	 * @param message
	 *            {@link String} of the message we wish to send to all clients.
	 */
	public void sendMessageToAllClients(String message) {
		logger.log(LogLevel.DEBUG, "Queueing message to send to all clients: (" + message + ")");
		for (int i = 0; i < handlers.size(); i++)
			sendMessageToClient(message, handlers.get(i));
	}

	/**
	 * Puts all <b>messages</b> in a {@link List} into every {@link Handler#queue}
	 * in {@link #handlers}.<br>
	 * All messages sent through this message will be noted as being put into every
	 * {@link Handler#queue} at the same exact time (or near, depending on
	 * processing time), via each {@link Handler}'s {@link Handler#queueTimes}. <br>
	 * <br>
	 * To put it simply, takes the {@link List} of {@link String}s and submits them
	 * individually to {@link #sendMessageToAllClients(String)}.
	 * 
	 * @param messages
	 *            {@link List}<{@link String}> of all messages we want to send, in
	 *            the order we want to send them.
	 */
	public void sendMessagesToAllClients(List<String> messages) {
		for (int i = 0; i < messages.size(); i++)
			sendMessageToAllClients(messages.get(i));
	}

	/**
	 * Sends a <b>message</b> to every connected {@link Client}, except those
	 * <i>excluded</i> by <b>excludedNames</b>.
	 * 
	 * @param message
	 * @param excludedNames
	 * @see {@link #sendMessageToAllClients(String)}
	 *      {@link #sendMessageToAllClientsIncludeOnlyNames(String, String...)}.
	 */
	public void sendMessageToAllClientsExcludingNames(String message, String... excludedNames) {
		for (int i = 0; i < handlers.size(); i++) {
			Handler handler = handlers.get(i);
			boolean exclude = false;
			for (int j = 0; j < excludedNames.length && !exclude; j++)
				if (excludedNames[j].equals(handler.getTheirName()))
					exclude = true;
			if (!exclude)
				handler.sendMessageToClient(message);
		}
	}

	/**
	 * Sends a <b>message</b> to every connected {@link Client} whose name is
	 * <i>included</i> in <b>includedNames</b>.
	 * 
	 * @param message
	 * @param includedNames
	 * @see {@link #sendMessageToAllClients(String)}
	 *      {@link #sendMessageToAllClientsExcludingNames(String, String...).
	 */
	public void sendMessageToAllClientsIncludeOnlyNames(String message, String... includedNames) {
		for (int i = 0; i < handlers.size(); i++) {
			Handler handler = handlers.get(i);
			boolean include = false;
			for (int j = 0; j < includedNames.length && !include; j++)
				if (includedNames[j].equals(handler.getTheirName()))
					include = true;
			if (include)
				handler.sendMessageToClient(message);
		}
	}

	/**
	 * Queues a <b>message</b> with {@link Handler#queue} to a given {@link Handler}
	 * in {@link #handlers}, as indicated by <b>handlerIdx</b>.
	 * 
	 * @param message
	 *            {@link String} of the message we want to send to the
	 *            {@link Client}.
	 * @param handlerIdx
	 *            The index of the {@link Handler} we want to send <b>message</b>
	 *            through. Their position in {@link #handlers}.
	 */
	public void sendMessageToClient(String message, Handler handler) {
		logger.log(LogLevel.DEBUG, "server.queueMessage(" + message + "," + getHandlerIndex(handler) + ") (idx)");
		handler.sendMessageToClient(message);
	}

	/**
	 * Queues a <b>message</b> with {@link #sendMessageToClient(String, int)}, using
	 * <b>handlerName</b> to figure out which {@link Handler} we are sending
	 * through.
	 * 
	 * @param message
	 *            {@link String} of the message we want to send to the
	 *            {@link Client}.
	 * @param handlerName
	 *            {@link Handler#theirName}.
	 */
	public void sendMessageToClient(String message, String handlerName) {
		logger.log(LogLevel.DEBUG, "server.queueMessage(" + handlerName + ") (name)");
		sendMessageToClient(message, handlers.get(getHandlerIndex(handlerName)));
	}

	/**
	 * Queues a <b>message</b> with {@link #sendMessageToClient(String, int)}, using
	 * <b>handlerIP</b> and <b>handlerName</b> to figure out which {@link Handler}
	 * we are sending through.
	 * 
	 * @param message
	 *            {@link String} of the message we want to send to the
	 *            {@link Client}.
	 * @param handlerIP
	 *            {@link Handler#ip}.
	 * @param handlerName
	 *            {@link Handler#theirName}.
	 */
	public void sendMessageToClient(String message, String handlerIP, String handlerName) {
		logger.log(LogLevel.DEBUG,
				"server.queueMessage(" + message + "," + handlerIP + ", " + handlerName + ") (ip, name)");
		sendMessageToClient(message, handlers.get(getHandlerIndex(handlerIP, handlerName)));
	}

	/**
	 * Gets a {@link Handler} from {@link #handlers}'s index position, located by
	 * it's IP address and name.
	 * 
	 * @param IP
	 *            IP address of the Handler we're looking for.
	 * @param theirName
	 *            The username of the Handler we're looking for.
	 * @return The index position in {@link #handlers} of the {@link Handler}. -1 if
	 *         not found.
	 */
	public int getHandlerIndex(String IP, String theirName) {
		for (int i = 0; i < handlers.size(); i++)
			if (handlers.get(i).ip.equals(IP) && handlers.get(i).theirName.equals(theirName))
				return i;
		return -1;
	}

	/**
	 * Gets a {@link Handler} from {@link #handlers}'s index position, located by
	 * it's name.
	 * 
	 * @param theirName
	 *            The username of the Handler we're looking for.
	 * @return The index position in {@link #handlers} of the {@link Handler}. -1 if
	 *         not found.
	 */
	public int getHandlerIndex(String theirName) {
		for (int i = 0; i < handlers.size(); i++)
			if (handlers.get(i).theirName.equals(theirName))
				return i;
		return -1;
	}

	/**
	 * Gets a {@link Handler} from {@link #handlers}'s index position, located by
	 * it's instance.
	 * 
	 * @param handler
	 *            The Handler to remove.
	 * @return The index position in {@link #handlers} of the {@link Handler}. -1 if
	 *         not found.
	 */
	public int getHandlerIndex(Handler handler) {
		int tryCount = 50;
		do {
			try {
				for (int i = 0; i < handlers.size(); i++)
					if (handlers.get(i).equals(handler))
						return i;
			} catch (Exception e) {
				// be safe!
			}
			tryCount--;
		} while (tryCount >= 0);
		return -1;
	}

	/**
	 * @see {@link #handlers}.
	 */
	public Handler getHandler(int handlerIndex) {
		return handlers.get(handlerIndex);
	}

	/**
	 * Safely closes and removes a given {@link Handler} from {@link #handlers}.
	 * 
	 * @param Handler
	 *            The {@link Handler} to remove.
	 * @return Whether or not a the Handler was found and removed. .
	 */
	private boolean removeHandler(Handler handler) {
		return removeHandler(getHandlerIndex(handler));
	}

	/**
	 * Safely closes and removes a given {@link Handler} from {@link #handlers}.
	 * 
	 * @param IP
	 *            The IP address of the Handler.
	 * @param IP
	 *            The username of the Handler.
	 * @return Whether or not a Handler was found and removed based on <b>IP</b> .
	 */
	@SuppressWarnings("unused")
	private boolean removeHandler(String IP, String theirName) {
		return removeHandler(getHandlerIndex(IP, theirName));
	}

	/**
	 * Safely closes and removes a given {@link Handler} based on it's <b>index</b>.
	 * 
	 * @param index
	 *            The Handler's location based on {@link #handlers}.
	 * @return Whether or not the Handler was removed.
	 */
	private boolean removeHandler(int index) {
		if (handlers.isEmpty())
			return false;
		if (index != -1) {
			onExit(handlers.get(index), index);
			handlers.get(index).override = true;
			try {
				handlers.get(index).socket.close();
			} catch (Exception e) {
			}
			try {
				handlers.get(index).in.close();
			} catch (Exception e) {
			}
			try {
				handlers.get(index).out.close();
			} catch (Exception e) {
			}
			try {
				handlers.set(index, null);
				handlers.remove(index);
				return true;
			} catch (Exception e) {
				return false;
			}
		} else {
			logger.log(LogLevel.WARNING, "removeHandler(-1) called?");
			return false;
		}
	}

	/**
	 * @param handlerName
	 * @return The ping of the given Client, found by <b>handlerName</b>. Returns -1
	 *         if no handler is found.
	 */
	public int getClientPing(String handlerName) {
		int index = getHandlerIndex(handlerName);
		if (index == -1)
			return -1;
		else
			return getClientPing(getHandler(index));
	}

	/**
	 * @param handler
	 * @return The ping of the given Client, found by Handler.
	 */
	public int getClientPing(Handler handler) {
		return handler.getPing();
	}

	/**
	 * Runs when a Handler is being removed from the list, before it is removed. You
	 * can override this method to run anything you want with that Handler's
	 * information before it is destroyed.
	 * 
	 * @param handler
	 *            The Handler that is being removed.
	 * @param index
	 *            The placement of this <b>handler</b> in the list of
	 *            {@link #handlers}.
	 */
	public void onExit(Handler handler, int index) {

	}

	/**
	 * Attempts to establish a connection with a {@link Client}.
	 */
	private boolean connect() {
		Handler handler;
		logger.log(LogLevel.NORMAL, "Waiting for connection..");
		Socket newSocket = null;
		boolean complete = false;
		try {
			if (listener == null || listener.isClosed()) {
				try {
					listener = new ServerSocket(port, 100);
					listener.setSoTimeout((int) sleepTime);
				} catch (Exception e) {
					logger.err(LogLevel.ERROR,
							"Could not start server. The port is likely in use (" + port + "). Otherwise, who knows?\n"
									+ e.getStackTrace()[0] + "\n" + e.getStackTrace()[1] + "\n" + e.getStackTrace()[2]);
					e.printStackTrace();
					return false;
				}
			}
			newSocket = listener.accept();
			if (newSocket.getRemoteSocketAddress().toString().replace("/", "").split(":")[0].length() < 8) {
				if (newSocket != null)
					try {
						newSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				newSocket = null;
			}
			complete = true;
		} catch (SocketTimeoutException ignore) {
		} catch (Exception e) {
			// e.printStackTrace();
		} finally {
			if (!complete) {
				if (newSocket != null)
					try {
						newSocket.close();
					} catch (IOException e1) {
						// e1.printStackTrace();
					}
				newSocket = null;
				try {
					listener.close();
				} catch (Exception e) {
					// e.printStackTrace();
				}
				listener = null;
			}
		}
		if (newSocket != null) {
			handler = new Handler(newSocket);
			handlers.add(handler);
			handler.start();
			logger.log(LogLevel.NORMAL, "Connected to a Client!");
			return true;
		} else {
			logger.log(LogLevel.DEBUG, "No connection found, sleeping.");
			try {
				Thread.sleep(sleepTime);
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * Starts a {@link Connector} to attempt to connect to new incoming
	 * {@link Client} connections.
	 */
	public void openIncomingClientConnection() {
		if (incomingConnector == null) {
			logger.log(LogLevel.NORMAL, "Opening incoming client connections.");
			stop = false;
			Timer timer = new Timer();
			incomingConnector = new Connector(timer);
			timer.scheduleAtFixedRate(incomingConnector, 0, timeBetweenConnectionAttempts);
		} else {
			logger.log(LogLevel.WARNING, "We tried to open incoming client connections when we already had it open..");
		}
	}

	/**
	 * Closes incoming {@link Client} connections via {@link #incomingConnector} .
	 * 
	 * @see {@link #openIncomingClientConnection()}.
	 */
	public void closeIncomingClientConnection() {
		if (incomingConnector != null) {
			logger.log(LogLevel.NORMAL, "Closing incoming client connections.");
			incomingConnector.stop();
			incomingConnector = null;
		} else {
			logger.log(LogLevel.WARNING,
					"We tried to close incoming client connections when we hadn't ever opened them..");
		}
	}

	/**
	 * Starts {@link talkingConnector} to use {@link #talkToClients()} to start
	 * talking to all connected {@link Client}s.
	 */
	public void startClientTalkingConnections() {
		stop = false;
		Timer timer = new Timer();
		talkingConnector = new Connector(timer);
		timer.scheduleAtFixedRate(talkingConnector, 0, timeBetweenConnectionAttempts);
	}

	/**
	 * Used internally with {@link #clientTimeout} to remove any {@link Client}s
	 * that have exceeded the timeout time.
	 */
	private void disconnectFromIdleClients() {
		for (int i = 0; i < handlers.size(); i++)
			if (System.currentTimeMillis() - handlers.get(i).lastMessageReceived >= clientTimeout
					&& clientTimeout != -1) {
				// TODO: Wait for reconnection?
				sendMessageToClient("EXITING", handlers.get(i));
				sendMessageToAllClients("Server" + CHAT_SPLITTER + "Client timed out: " + handlers.get(i).theirName
						+ " (" + handlers.get(i).ip + ")");
				logger.log(LogLevel.WARNING,
						"Client timed out: " + handlers.get(i).theirName + " (" + handlers.get(i).ip + ")");
				removeHandler(handlers.get(i));
				i--;
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
				}
			}
	}

	/**
	 * Disconnects the {@link Server} entirely. Turns off a {@link Connector} if on,
	 * and ends all {@link Handler}s and removes them from {@link #handlers} .
	 */
	public void disconnect() {
		logger.log(LogLevel.NORMAL, "Stopping server, disconnecting.");
		stop = true;
		if (listener != null) {
			logger.log(LogLevel.DEBUG, "Closing listener.");
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			listener = null;
		}
		if (incomingConnector != null) {
			logger.log(LogLevel.DEBUG, "Closing incoming connector.");
			incomingConnector.stop();
			incomingConnector = null;
		}
		if (talkingConnector != null) {
			logger.log(LogLevel.DEBUG, "Closing talking connector.");
			talkingConnector.stop();
			talkingConnector = null;
		}
		while (handlers.size() > 0) {
			logger.log(LogLevel.DEBUG, "Killing Handler");
			try {
				handlers.get(0).end();
			} catch (Exception e) {
			}
		}
		logger.log(LogLevel.DEBUG, "Completely disconnected.");
	}

	/**
	 * Communicates with all {@link Client}s: sends and receives queued messages.
	 * Also does hard-coded methods for handling some {@link Server} output and
	 * {@link Client} input.
	 */
	private void talkToClients() {
		logger.log(LogLevel.ERROR, "talkToClients()");
		if (stop) {
			logger.log(LogLevel.WARNING,
					"We tried to communicate with all clients but we were told to stop the connection. Disconnecting.");
			disconnect();
			return;
		} else {
			if (removeDuplicateConnections) {
				List<String> ipList = new ArrayList<String>();
				for (int i = 0; i < handlers.size(); i++) {
					boolean found = false;
					for (int j = 0; j < ipList.size(); j++) {
						if (ipList.get(j).equals(handlers.get(i).ip)) {
							logger.log(LogLevel.ERROR, "Found duplicate connection, removing one.");
							removeHandler(handlers.get(i));
							found = true;
							break;
						}
					}
					if (!found)
						ipList.add(handlers.get(i).ip);
				}
			}
			for (int i = 0; i < handlers.size(); i++)
				handlers.get(i).run();
		}
	}

	/**
	 * An individual 'talker' to a specific {@link Client}. A {@link List} of these
	 * is gathered in {@link #handlers} to talk to every {@link Client}.
	 * 
	 * @author Nathan
	 * 
	 */
	public class Handler extends Thread {

		/**
		 * Handles connection to this {@link Handler}'s {@link Client}.
		 */
		private Socket socket = null;
		/**
		 * Reads incoming data from this {@link Handler}'s {@link Client}.
		 */
		private BufferedReader in = null;
		/**
		 * Sends outgoing data to this {@link Handler}'s {@link Client}.
		 */
		private PrintWriter out = null;
		/**
		 * A queue of all messages to be sent to this {@link Handler}'s {@link Client}.
		 */
		private List<String> queue = new ArrayList<String>();
		/**
		 * A queue of the times messages were put in {@link Handler#queue}.
		 */
		private List<Long> queueTimes = new ArrayList<Long>();
		/**
		 * The IP address of the {@link Client} this {@link Handler} is connected to.
		 * Used to identify this {@link Handler}'s {@link Client} from others.
		 */
		private String ip = null;
		/**
		 * The username set by the {@link Client} used to identify the {@link Client}
		 * from others.
		 */
		private String theirName = null;
		/**
		 * If set to <b>true</b>, this {@link Handler} will return instead of running,
		 * effectively disabling it.
		 */
		boolean override = false;
		/**
		 * Whether or not {@link Handler#run()} is currently running.
		 */
		boolean running = false;
		/**
		 * The time we requested a ping from this {@link Handler}'s {@link Client}, to
		 * determine our latency with the {@link Client}.<br>
		 * <br>
		 * Will be <b>-1</b> if we are not currently waiting for a ping back from our
		 * {@link Client}.
		 */
		public long pingTime = -1;
		/**
		 * A (pruned) list of pings used to determine the speed of our connection to
		 * this {@link Handler}"s {@link Client}.
		 */
		protected ArrayList<Integer> pingList = new ArrayList<Integer>();
		/**
		 * The total amount of everything in {@link #pingList} added together, kept in
		 * track by {@link #addPing(int)}.
		 */
		protected int pingTotal = 0;
		/**
		 * The highest size {@link #pingList} is allowed to be.
		 */
		protected int pingListSize = 8;
		/**
		 * The last time we communicated with this {@link Handler}'s {@link Client}.
		 * Used to check for connection timeouts.
		 */
		protected long lastMessageReceived = System.currentTimeMillis();

		/**
		 * Initiates this {@link Handler}. Also sets {@link Handler#ip}, determined by
		 * <b>socket</b>. Also uses {@link Handler#addPing(int)} to add a 'ping' of -1
		 * to initiate {@link Handler#pingList}.
		 * 
		 * @param socket
		 *            The {@link Socket} connection for the {@link Client} connected to
		 *            this {@link Handler}.
		 */
		public Handler(Socket socket) {
			this.socket = socket;
			this.ip = socket.getRemoteSocketAddress().toString().replace("/", "").split(":")[0];
			queueTimes.add(System.currentTimeMillis());
			addPing(-1);
		}

		/**
		 * @see {@link #queue}.
		 */
		public List<String> getQueue() {
			return queue;
		}

		/**
		 * @see {@link #ip}.
		 */
		public String getIP() {
			return ip;
		}

		/**
		 * Adds a ping to {@link Handler#pingList}. <br>
		 * Also, prunes {@link Handler#pingList} before adding <b>pingToAdd</b>.
		 * 
		 * @param pingToAdd
		 *            {@link Integer} ping to add.
		 */
		protected void addPing(int pingToAdd) {
			while (pingList.size() > pingListSize) {
				int ran = Tools.random(null, 0, pingList.size() - 1, null);
				pingTotal -= pingList.get(ran);
				pingList.remove(ran);
				ran = 0;
				pingTotal -= pingList.get(ran);
				pingList.remove(ran);
			}
			pingList.add(pingToAdd);
			pingTotal += pingToAdd;
		}

		/**
		 * @return The average of {@link Handler#pingList} using
		 *         {@link Tools#getAverage(ArrayList)}.
		 */
		public int getPing() {
			if (pingList.size() == 0)
				return -1;
			return pingTotal / pingList.size();
		}

		/**
		 * @return {@link Handler#theirName}.
		 */
		public String getTheirName() {
			return theirName;
		}

		/**
		 * Checks to see whether or not a given message is currently in
		 * {@link Handler#queue} to send to this {@link Handler}'s {@link Client}, by
		 * {@link String#contains} (so doesn't have to be equal).<br>
		 * <br>
		 * Note that if a message is sent, it will no longer be in the
		 * {@link Handler#queue}, and therefore this will return false for it. There is
		 * no log, history, or {@link List} for messages <i>already</i> sent to
		 * {@link Client}s, only waiting.
		 * 
		 * @param message
		 *            {@link String} of the message we want to check.
		 * @return Whether or not the message is still in the queue.
		 */
		public boolean hasMessageQueued(String message) {
			try {
				ArrayList<String> newQueue = new ArrayList<String>(queue);
				for (int i = 0; i < newQueue.size(); i++)
					if (newQueue.get(i).contains(message))
						return true;
			} catch (Exception e) {
			}
			return false;
		}

		/**
		 * Checks to see whether or not any message is currently in
		 * {@link Handler#queue} to send to this {@link Handler}'s {@link Client}. <br>
		 * Note that if a message is sent, it will no longer be in the
		 * {@link Handler#queue}, and therefore this will return false for it. There is
		 * no log, history, or {@link List} for messages <i>already</i> sent to
		 * {@link Client}s, only waiting.
		 * 
		 * @return Whether or not the queue is empty.
		 */
		public boolean hasMessageQueued() {
			return queue.size() != 0;
		}

		/**
		 * Puts a given message into the {@link Handler#queue} of messages to send to
		 * the {@link Client}.<br>
		 * Also logs what time this message was put in the queue to
		 * {@link Handler#queueTimes}, in case the {@link Server} needs to tell the
		 * difference in time between messages.
		 * 
		 * @param message
		 *            {@link String} of the message we wish to send to the server.
		 */
		public void sendMessageToClient(String message) {
			logger.log(LogLevel.DEBUG, "Sending message to Client (" + ip + "," + getHandlerIndex(this) + ","
					+ theirName + ") from Handler. (" + message + ")");
			queue.add(message);
			queueTimes.add(System.currentTimeMillis());
		}

		/**
		 * 
		 * @param ip
		 *            IP address.
		 * @return Whether or not this {@link Handler} is connected to a {@link Client}
		 *         with the IP address of <b>ip</b>.
		 * @see Handler#ip
		 */
		public boolean isConnectedTo(String ip) {
			return this.ip.equals(ip);
		}

		/**
		 * See {@link #removeHandler(Handler)}
		 */
		public void end() {
			override = true;
			logger.log(LogLevel.DEBUG, "Ending Handler (" + ip + ")");
			try {
				out.close();
			} catch (Exception e) {
			}
			try {
				in.close();
			} catch (Exception e) {
			}
			try {
				socket.close();
			} catch (Exception e) {
			}
			out = null;
			in = null;
			socket = null;
			me.removeHandler(this);
		}

		/**
		 * Manages all talking between {@link Client} and {@link Server}, including
		 * sending queued messages from {@link Handler#queue} and receiving messages to
		 * {@link #messageHandler}.
		 */
		public void run() {
			if (running)
				return;
			running = true;
			if (override)
				return;
			logger.log(LogLevel.DEBUG, "Running listener (" + ip + ")");
			try {
				if (in == null || out == null) {
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					out = new PrintWriter(socket.getOutputStream(), true);
				}
				int index = -1;
				for (int i = 0; i < handlers.size(); i++)
					if (isConnectedTo(handlers.get(i).ip))
						index = i;

				if (index == -1) {
					logger.log(LogLevel.ERROR,
							"Couldn't find the handlers that corresponded to the IP we're connected to.. (" + ip + ")",
							"All handlers:");
					for (int i = 0; i < handlers.size(); i++)
						logger.log(LogLevel.ERROR, "handlers[" + i + "]: " + handlers.get(i).ip);
				}
				while (!override) {
					String input = null;
					try {
						input = in.readLine();
					} catch (Exception e) {
						continue;
					}
					// logger.log(LogLevel.DEBUG, "Received raw input from
					// client: " + input);
					lastMessageReceived = System.currentTimeMillis();
					if (input == null) {
						logger.log(LogLevel.WARNING,
								"Raw input is null, something is wrong. Disconnecting Server from Client.");
						end();
						try {
							Thread.sleep(100);
						} catch (Exception e) {
						}
						continue;
					}
					if (input.contains(MESSAGE_SPLITTER)) {
						logger.log(LogLevel.DEBUG, "Got multiple message pack");
						String[] splitInput = input.split(MESSAGE_SPLITTER);
						for (int i = 0; i < splitInput.length; i++) {
							int waitBefore = -1, waitAfter = -1;
							String waitBeforeStr = "[WAITBEFORE]";
							if (splitInput[i].contains(waitBeforeStr)) {
								waitBefore = Integer.parseInt(splitInput[i].split(waitBeforeStr)[1]);
								splitInput[i] = splitInput[i].replace(waitBeforeStr + waitBefore + waitBeforeStr, "");
							}
							String waitAfterStr = "[WAITAFTER]";
							if (splitInput[i].contains(waitAfterStr)) {
								waitAfter = Integer.parseInt(splitInput[i].split(waitAfterStr)[1]);
								splitInput[i] = splitInput[i].replace(waitAfterStr + waitBefore + waitAfterStr, "");
							}
							if (!splitInput[i].contains(Client.USERNAME_SPLITTER))
								splitInput[i] += Client.USERNAME_SPLITTER + theirName;
							logger.log(LogLevel.DEBUG, "Got message #" + i + ": " + splitInput[i]);
							if (waitBefore > 0) {
								logger.log(LogLevel.DEBUG, "Waiting before: " + waitBefore);
								try {
									Thread.sleep(waitBefore);
								} catch (Exception e) {
								}
								logger.log(LogLevel.DEBUG, "Waited before.");
							}
							messageHandler.preProcess(me,
									socket.getRemoteSocketAddress().toString().split(":")[0].replace("/", ""),
									theirName, getHandlerIndex(this), splitInput[i]);
							if (waitAfter > 0) {
								logger.log(LogLevel.DEBUG, "Waiting after: " + waitAfter);
								try {
									Thread.sleep(waitAfter);
								} catch (Exception e) {
								}
								logger.log(LogLevel.DEBUG, "Waited after.");
							}
						}
						try {
							// Sleep a very small time between processing every
							// message
							Thread.sleep(1);
						} catch (Exception e) {
						}
					} else {
						if (!input.startsWith("Ping pong"))
							logger.log(LogLevel.DEBUG, "Got one message: " + input);
						messageHandler.preProcess(me,
								socket.getRemoteSocketAddress().toString().split(":")[0].replace("/", ""), theirName,
								getHandlerIndex(this), input);
					}
					boolean saidSomething = false;
					if (input.startsWith("CONNECTING")) {
						String nameToSet = input.split(USERNAME_SPLITTER)[1];
						theirName = nameToSet;
						out.println("CONNECTING:" + seed + USERNAME_SPLITTER + username);
						// logger.log(LogLevel.DEBUG, "Sending to client:
						// CONNECTING:SEED (seed is " + seed + ")");
						saidSomething = true;
						continue;
					}
					boolean first = true;
					if (queue.size() > 0 && !saidSomething) {
						String send = "", thisMsg = "";
						while (queue.size() > 0) {
							try {
								thisMsg = queue.get(0);
								queue.remove(0);
								if (first) {
									first = false;
									send = thisMsg;
								} else
									send += MESSAGE_SPLITTER + thisMsg;
							} catch (Exception e) {
								e.printStackTrace();
								System.err.println("Uh oh.. something going wrong in netcode!!!");
							}
						}
						if (pingTime == -1)
							pingTime = System.currentTimeMillis();
						send = send + PING_SPLITTER + getPing() + USERNAME_SPLITTER + username;
						if (index == -1) {
							for (int i = 0; i < handlers.size(); i++)
								if (isConnectedTo(handlers.get(i).ip))
									index = i;
						}
						if (index == -1) {
							logger.log(LogLevel.ERROR, "Problem getting correct handler..", "All listeners:");
							for (int i = 0; i < handlers.size(); i++)
								logger.log(LogLevel.ERROR, "handlers[" + i + "]: " + handlers.get(i).ip);
						}
						logger.log(LogLevel.DEBUG, "Sending to client: " + send);
						out.println(send);
						lastMessageReceived = System.currentTimeMillis();
						saidSomething = true;
					} else if (!saidSomething) {
						// logger.log(LogLevel.DEBUG, "list size is zero,
						// nothing to send to client.");
					} else {
						// logger.log(LogLevel.DEBUG, "We already said something
						// to the client.");
					}
					if (!saidSomething) {
						if (pingTime == -1)
							pingTime = System.currentTimeMillis();
						String msg = "RECEIVED";
						int amt = getPing();
						msg += PING_SPLITTER + amt + USERNAME_SPLITTER + username;
						out.println(msg);
						// logger.log(LogLevel.DEBUG, "Sending to client: " +
						// msg);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// This client is going down! Remove its name and its print
				// writer from the sets, and close its socket.
				logger.log(LogLevel.WARNING, "Client disappeared. Cleaning up. (" + ip + ")");
				try {
					out.println("EXITING");
				} catch (Exception e) {
					// Client probably already gone or something, don't worry
					// about it.
				}
				end();
			}
			running = false;
		}
	}

	/**
	 * Attempts to open and establish a connection with {@link Client}s via
	 * {@link #connect()}, meant for being used with a {@link Timer} to attempt
	 * connection until a connection is successfully established.<br>
	 * <br>
	 * Once a connection is established, this class instead uses
	 * {@link #talkToClients()} to communicate with the all {@link Client} s.
	 * 
	 * @author Nathan
	 * 
	 */
	public class Connector extends TimerTask {
		/**
		 * If set to <b>true</b>, this {@link Connector} will return instead of running,
		 * effectively disabling it.
		 */
		public boolean override = false;
		/**
		 * Whether or not we are currently running this {@link Connector}.
		 */
		private boolean running = false;
		/**
		 * The timer that is activating this {@link Connector}.
		 */
		private Timer ourTimer = null;

		/**
		 * Initiates {@link Connector}.
		 * 
		 * @param ourTimer
		 *            {@link Connector#ourTimer}
		 */
		public Connector(Timer ourTimer) {
			this.ourTimer = ourTimer;
			logger.log(LogLevel.DEBUG, "Connector (TimerTask) created.");
		}

		/**
		 * @param override
		 *            The value to set {@link Connector#override} to.
		 */
		public void override(boolean override) {
			this.override = override;
		}

		/**
		 * When {@link #waiting} is <b>true</b>, uses {@link #connect()} to connect to
		 * any incoming Clients.<br>
		 * <br>
		 * When {@link #waiting} is <b>false</b>, uses {@link #talkToClients()} to
		 * communicate with all {@link Client} s.
		 */
		public void run() {
			logger.log(LogLevel.DEBUG, "Running...");
			if (!override && !running && !stop) {
				running = true;
				if (this.equals(incomingConnector)) {
					logger.log(LogLevel.NORMAL, "Trying to connect to potential Clients...");
					disconnectFromIdleClients();
					boolean connected = true;
					while (connected && !override && !stop) {
						connected = me.connect();
					}
				} else if (this.equals(talkingConnector)) {
					logger.log(LogLevel.NORMAL, "Trying to connect and talk to existing Clients...");
					disconnectFromIdleClients();
					talkToClients();
				}
			} else {
				logger.err(LogLevel.WARNING, "Server run error?? " + override + ":" + running + ":" + stop);
			}
			running = false;
		}

		/**
		 * @return {@link Connector#running}
		 */
		public boolean isRunning() {
			return running;
		}

		/**
		 * Stops this {@link Connector} and it's {@link Timer}s.
		 */
		public void stop() {
			logger.log(LogLevel.DEBUG, "Stopping Connector.");
			override(true);
			if (ourTimer != null) {
				ourTimer.cancel();
				ourTimer.purge();
				ourTimer = null;
			}
			cancel();
		}
	}

	/**
	 * Abstract class meant to be used to decode all messages sent from a
	 * {@link Client} through a {@link Handler}. This is where all the net-code
	 * decoding happens.
	 * 
	 * @author Nathan
	 * 
	 */
	public abstract static class ServerMessageHandler {
		public abstract void process(String message);

		/**
		 * Hard-coded pre-processing of {@link Client} input where necessary.
		 * 
		 * @param server
		 *            The {@link Server} in which this message handling is taking place.
		 * @param ip
		 *            IP address of the {@link Client} this message is from.
		 * @param username
		 *            Username of the {@link Client} this message is from.
		 * @param index
		 *            Index of the {@link Client} this message is from.
		 * @param message
		 *            input from a {@link Client} through a {@link Handler}.
		 * @return {@link ServerMessageHandler#process(String)}.
		 */
		protected void preProcess(Server server, String ip, String username, int index, String message) {
			Handler handler = server.handlers.get(index);
			if (message.startsWith("Ping pong") && handler.pingTime != -1) {
				long ping = System.currentTimeMillis() - handler.pingTime;
				// server.logger.log(LogLevel.DEBUG,
				// "Got ping of Client (" + ip + ", " + username + ")! Ping
				// time: " + ping + ".");
				handler.addPing((int) ping);
				// server.logger.log(LogLevel.DEBUG,
				// "Average ping of Client (" + ip + ", " + username + "): " +
				// handler.getPing());
				handler.pingTime = -1;
			} else if (message.startsWith("SYNCTIME")) {// TODO
				server.logger.log(LogLevel.DEBUG, "Client (" + ip + ", " + username + ") Requested SyncTime");
				handler.sendMessageToClient("SYNCTIME:" + System.currentTimeMillis());
			}
			process(message);
		}
	}

	/**
	 * Call this to discard this Server and instead move to the Peer-to-Peer
	 * connections. Sets {@link #P2PNetwork} with the network to replace this
	 * Server.
	 * 
	 * @param p2pServerMessageHandler
	 *            The server message handler to pass on to the P2PNetwork.
	 * @param p2pClientMessageHandler
	 *            The client message handler to pass on to the P2PNetwork.
	 * @param startingPort
	 *            The starting port - multiple connections will use the ports
	 *            following this port.
	 */
	public P2PNetwork changeToP2P(P2PNetwork.P2PServerMessageHandler p2pServerMessageHandler,
			P2PNetwork.P2PClientMessageHandler p2pClientMessageHandler, int startingPort) {
		try {
			p2pNetwork = P2PNetwork.startP2PNetwork(this, startingPort, logger, timeBetweenConnectionAttempts,
					p2pServerMessageHandler, p2pClientMessageHandler);
			return p2pNetwork;
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(LogLevel.ERROR, "Disregarding request to change to P2P: we couldn't contact all Clients..");
			return null;
		}
	}

	/**
	 * 
	 * @return The P2PNetwork that this Server abandoned to.
	 * @see {@link #changeToP2P()}
	 */
	public P2PNetwork getP2PNetwork() {
		return p2pNetwork;
	}
}

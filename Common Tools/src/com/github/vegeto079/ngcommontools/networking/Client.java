package com.github.vegeto079.ngcommontools.networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.github.vegeto079.ngcommontools.main.Game;
import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.Tools;
import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork.P2PClientMessageHandler;
import com.github.vegeto079.ngcommontools.networking.P2PNetwork.P2PServerMessageHandler;
import com.github.vegeto079.ngcommontools.networking.Server.Handler;

/**
 * At attempt to create an easy-to-understand, well-documented Client used for
 * connecting to a {@link Server}, with games in mind.<br>
 * <br>
 * To start a new basic {@link Client}, initiate one with
 * {@link #Client(ClientMessageHandler, Logger, long, String)}. For the
 * {@link ClientMessageHandler}, create a new class <b>extending</b> it, and
 * create {@link ClientMessageHandler#process(String)} how you want to process
 * incoming {@link Server} messages.<br>
 * <br>
 * For the {@link Logger}, just create a new temporary {@link Logger} that
 * displays what messages you want it to display.<br>
 * <br>
 * Once the {@link Server} is created, you can start looking for a
 * {@link Server} with {@link #connectToServer(String, int)}. <br>
 * <br>
 * And that's all! All communication will be handled through the
 * {@link ClientMessageHandler} you created.<br>
 * Currently dropped connections are not re-established.
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Added {@link #connecting} and {@link #isConnecting()} to
 *          determine whether or not we are actively trying to connect to a
 *          {@link Server} at this time.
 * @version 1.02: {@link ClientMessageHandler#preProcess(Client, String)} is
 *          void instead of boolean, as what it returned didn't affect anything.
 * @version 1.03: Added {@link #p2pNetwork} and {@link #getP2PNetwork()}
 * @version 1.031: Splitters now public.
 * @version 1.04: Added {@link #onExit()}.
 * @version 1.041: Variables changed from <b>private</b> to <b>protected</b> so
 *          that they can be accessed on a class extending Client.
 * @version 1.05: Added {@link #getPing()}. Pings now correctly added via
 *          {@link #addPing(int)}.
 * @version 1.06: {@link #getPing()} now follows standards set by {@link Game}
 *          to use {@link #pingTotal} instead of running costly
 *          {@link Tools#getAverage(ArrayList)} calls.
 * @version 1.07: {@link #talkToServer} now returns a boolean to determine
 *          whether or not messaging went through normally. Method also set to
 *          protected so extending classes can override it.
 * @version 1.08: Added {@link #lag} to simulate network latency.
 * @version 1.09: {@link #connect(String, int)} now connects to the
 *          {@link Server} using
 *          {@link Socket#connect(java.net.SocketAddress, int)} to specify a
 *          timeout.
 * @version 1.091: Timeout was causing issues so removed implementation. Added
 *          {@link #pingListSize} and increased from 5 to 8.
 * @version 1.1: Added {@link #timeOffset}, {@link #timeOffsetCheck},
 *          {@link #connectionTime} and {@link #currentTimeMillis()}.
 * @version 1.11: Removed all ping-related things as the {@link #Server} handles
 *          the computing. Now just uses {@link #ping}.
 */
public class Client {
	/**
	 * Reads incoming data from a {@link Server}.
	 */
	protected BufferedReader in;
	/**
	 * Sends outgoing data to a {@link Server}.
	 */
	protected PrintWriter out;
	/**
	 * Handles connection to a {@link Server}.
	 */
	protected Socket socket;
	/**
	 * Custom {@link ClientMessageHandler} used when receiving input from a
	 * {@link Server}.
	 */
	protected ClientMessageHandler messageHandler = null;
	/**
	 * Custom {@link Logger} used to display messages with this {@link Client}.
	 */
	protected Logger logger = null;
	/**
	 * A queue of all messages to be sent to the {@link Server}.
	 */
	protected ArrayList<String> queue = new ArrayList<String>();
	/**
	 * A queue of the times messages were put in {@link #queue}.
	 */
	protected ArrayList<Long> queueTimes = new ArrayList<Long>();
	/**
	 * Whether or not we are connected to a {@link Server}.
	 */
	protected boolean connected = false;
	/**
	 * Whether or not we are attempting to connect to a {@link Server}.
	 */
	protected boolean connecting = false;
	/**
	 * Whether or not we should end the connection to a {@link Server}.<br>
	 * Is set to <b>false</b> when establishing a new connection to a server with
	 * {@link #connectToServer(String, int)}.
	 */
	protected boolean stop = true;
	/**
	 * This instance of {@link Client}.
	 */
	protected Client me = null;
	/**
	 * {@link Connector} used in this {@link Client} to attempt to establish a
	 * connection with a {@link Server}.
	 */
	protected Connector connector = null;
	/**
	 * The ping value given to us by the {@link Server}.
	 */
	protected int ping = 0;
	/**
	 * The number of milliseconds between reattempting a connection via
	 * {@link #connectToServer(String, int)} when a failed connection occurs. <br>
	 * <br>
	 * Once a connection is established, this is the number of milliseconds between
	 * connections to the {@link Server}.
	 */
	protected long timeBetweenConnectionAttempts = -1;
	/**
	 * Used to tell different parts of a message apart.
	 */
	public final static String SPLITTER = "";
	/**
	 * Used to tell the Server who this message is coming from.
	 */
	public final static String USERNAME_SPLITTER = "-=USER=-";
	/**
	 * Used to split several messages apart when using {@link #talkToServer()} so
	 * the {@link Server} can decode them separately.
	 */
	public final static String MESSAGE_SPLITTER = "-=__=-";
	/**
	 * Used to tell that the message containing this @[link String} from the
	 * {@link Server} also contains a ping value, to determine latency.
	 */
	public final static String PING_SPLITTER = "-=PING=-";
	/**
	 * Used to tell that the message containing this {@link String} from a
	 * {@link Server} is a message meant to be read in a chatbox or similar, for
	 * viewing of persons.
	 */
	public final static String CHAT_SPLITTER = "-=CHAT=-";
	/**
	 * Username used to identify ourselves to the {@link Server} so they know who
	 * the messages are coming from.
	 */
	public String username = null;
	/**
	 * Seed to be retrieved from a {@link Server} to keep all {@link Client}'s and
	 * {@link Server}'s {@link java.util.Random} class in sync.
	 */
	public long seed = -1;
	/**
	 * The username set by the {@link Server} used to identify the {@link Server}
	 * from others.
	 */
	protected String theirName = null;
	/**
	 * How many times we will attempt to connect to the {@link Server}. If set to
	 * -1, it will run until {@link #disconnect()} is ran.
	 */
	protected long maxConnectionAttempts = -1;
	/**
	 * A tracker to keep track of how many times we've attempted to connect to a
	 * {@link Server}.
	 * 
	 * @see {@link #maxConnectionAttempts}.
	 */
	protected long currentConnectionAttempt = 0;

	/**
	 * Used when we move from a Client-Server structure to Peer-to-Peer
	 * connectivity. Can refer to this instead of any further Client references.
	 * 
	 * @see {@link ClientMessageHandler#preProcess(Client, String)}).
	 */
	protected P2PNetwork p2pNetwork = null;

	/**
	 * Server Message Handler to be used when using {@link P2PNetwork}.
	 */
	protected P2PServerMessageHandler p2pServerMessageHandler = null;
	/**
	 * Client Message Handler to be used when using {@link P2PNetwork}.
	 */
	protected P2PClientMessageHandler p2pClientMessageHandler = null;

	/**
	 * The time that a connection was established, or <b>-1</b> if a connection has
	 * not been established or is disconnected.
	 */
	private long connectionTime = -1;
	/**
	 * Calculated time offset between the {@link Server} and us, using the Server as
	 * the master clock. Retrieved via {@link #currentTimeMillis()}.
	 */
	private int timeOffset = 0;
	/**
	 * Whether or not {@link #timeOffset} has been checked for and set with the
	 * {@link Server}.
	 */
	private boolean timeOffsetCheck = false;

	/**
	 * Amount of time (in ms) to delay between messages to simulate network
	 * latency.<br>
	 * This is the total one-way lag, so the ping will be doubled as this lag is
	 * introduced twice (sending and receiving).
	 */
	public long lag = 0;

	/**
	 * Initiates {@link Client}. Also uses {@link #addPing(int)} to add a 'ping' of
	 * -1 to initiate {@link #pingList}.
	 * 
	 * @param messageHandler
	 *            See {@link #messageHandler}
	 * @param logger
	 *            See {@link #logger}
	 * @param timeBetweenConnectionAttempts
	 *            See {@link #timeBetweenConnectionAttempts}
	 * @param username
	 *            See {@link #username}
	 */
	public Client(ClientMessageHandler messageHandler, Logger logger, long timeBetweenConnectionAttempts,
			String username) {
		this.messageHandler = messageHandler;
		this.logger = logger;
		this.timeBetweenConnectionAttempts = timeBetweenConnectionAttempts;
		this.username = username;
		this.me = this;
		queueTimes.add(System.currentTimeMillis());
		ping = -1;
		logger.log(LogLevel.DEBUG, "Client created (" + username + ").");
	}

	/**
	 * @param p2pServerMessageHandler
	 * @param p2pClientMessageHandler
	 * @see #Client(ClientMessageHandler, Logger, long, String)
	 */
	public Client(ClientMessageHandler messageHandler, Logger logger, long timeBetweenConnectionAttempts,
			String username, P2PServerMessageHandler p2pServerMessageHandler,
			P2PClientMessageHandler p2pClientMessageHandler) {
		this.p2pServerMessageHandler = p2pServerMessageHandler;
		this.p2pClientMessageHandler = p2pClientMessageHandler;
		this.messageHandler = messageHandler;
		this.logger = logger;
		this.timeBetweenConnectionAttempts = timeBetweenConnectionAttempts;
		this.username = username;
		this.me = this;
		queueTimes.add(System.currentTimeMillis());
		ping = -1;
		logger.log(LogLevel.DEBUG, "Client created (" + username + ").");
	}

	/**
	 * @return Whether or not we are currently connected to a {@link Server}.
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * @return Whether or not we are currently attempting to connect to a
	 *         {@link Server}.
	 */
	public boolean isConnecting() {
		return connecting;
	}

	/**
	 * @return Averaged ping from {@link #pingList} using
	 *         {@link Tools#getAverage(ArrayList)}.
	 */
	public int getPing() {
		return ping;
	}

	/**
	 * Communicates with the {@link Server}: sends and receives queued messages.
	 * 
	 * @return <b>false</b> if we are disconnecting for some reason. Otherwise,
	 *         <b>true</b>.
	 */
	protected boolean talkToServer() {
		if (stop) {
			logger.log(LogLevel.WARNING,
					"We tried to communicate with the Server but we were told to stop the connection. Disconnecting.");
			disconnect();
			return false;
		} else {
			if (!connected) {
				logger.log(LogLevel.ERROR, "We tried to talk to the server but we aren't connected to one!");
			} else {
				// We aren't told to stop and we are connected, so talk to the
				// Server
				try {
					if (queue.size() > 0) {
						// If we have any messages to send
						String send = queue.get(0);
						queue.remove(0);
						while (queue.size() > 0) {
							// Add all messages together with a message splitter
							// so the Server can read them separately
							send += MESSAGE_SPLITTER + queue.get(0);
							queue.remove(0);
						}
						send = send + USERNAME_SPLITTER + username;
						logger.log(LogLevel.DEBUG, "Sending to server: " + send);
						send = "Ping pong" + MESSAGE_SPLITTER + send;
						out.println(send);
					} else {
						// Don't have any messages to send, just ping the
						// server
						// logger.log(LogLevel.DEBUG, "No messages to send.
						// Pinging server.");
						out.println("Ping pong" + USERNAME_SPLITTER + username);
					}
					if (lag != 0)
						try {
							Thread.sleep(lag);
						} catch (Exception e) {
						}
					// Get input from the Server
					String line = in.readLine();
					// logger.log(LogLevel.DEBUG, "Got raw input: " + line);
					if (line == null) {
						logger.log(LogLevel.WARNING,
								"Raw input is null, something is wrong. Disconnecting Client from Server.");
						disconnect();
						return false;
					}
					// If it contains the message splitter they have
					// multiple messages to give to us
					String[] splitInput = null;
					if (line.contains(MESSAGE_SPLITTER)) {
						// logger.log(LogLevel.DEBUG, "Got multiple message
						// pack.");
						splitInput = line.split(USERNAME_SPLITTER)[0].split(MESSAGE_SPLITTER);
					} else {
						// logger.log(LogLevel.DEBUG, "Got single message.");
						splitInput = new String[] { line.split(USERNAME_SPLITTER)[0] };
					}
					for (int i = 0; i < splitInput.length; i++) {
						// Go through all messages separately
						splitInput[i] += USERNAME_SPLITTER + line.split(USERNAME_SPLITTER)[1];
						if (!splitInput[i].startsWith("RECEIVED-=PING"))
							logger.log(LogLevel.DEBUG, "Got message #" + i + ": " + splitInput[i]);
						// Process the message from the Server
						messageHandler.preProcess(me, splitInput[i], connector.port + 1);
					}
					try {
						// Sleep a very small time between processing every
						// message to avoid lockups/weirdness
						Thread.sleep(1);
					} catch (Exception e) {
					}
					if (!timeOffsetCheck && connectionTime != -1
							&& System.currentTimeMillis() - connectionTime >= 10000) {
						// Wait 10 seconds after connection to sync time, for
						// ping to fully settle
						// Do this after the connection so all pings are
						// properly timed
						sendMessageToServer("SYNCTIME");
						timeOffsetCheck = true;
					}
				} catch (SocketException e) {
					logger.log(LogLevel.WARNING, "Looks like we lost connection to the host! Disconnecting..");
					disconnect();
				} catch (Exception e) {
				}
			}
		}
		return true;

	}

	/**
	 * Attempts to start a connection with a given <b>ip</b> and <b>port</b>. <br>
	 * Will continue to attempt to connect until a connection is established or
	 * {@link #disconnect()} is called.<br>
	 * <br>
	 * Once a connection is established, connects and talks to the {@link Server} in
	 * a separate thread.
	 * 
	 * @param ip
	 *            {@link String} representation of the IP we wish to connect to.
	 * @param port
	 *            {@link Integer} representation of the port for the IP.
	 * @param connectionAttempts
	 *            {@Link Integer} of how many times we are going to try to connect.
	 *            After this amount is reached, if a connection isn't established,
	 *            {@link #disconnect()} is ran. If set to -1, indefinite.
	 */
	public void connectToServer(String ip, int port, int connectionAttempts) {
		logger.log(LogLevel.NORMAL, "Connecting to server. " + ip + ":" + port);
		connecting = true;
		stop = false;
		maxConnectionAttempts = connectionAttempts;
		currentConnectionAttempt = 0;
		Timer timer = new Timer();
		connector = new Connector(timer, ip, port);
		timer.scheduleAtFixedRate(connector, 0, timeBetweenConnectionAttempts);
	}

	/**
	 * The underbelly of {@link #connectToServer(String, int)}, accessed by
	 * {@link Connector}.
	 * 
	 * @param ip
	 *            {@link String} representation of the IP we wish to connect to.
	 * @param port
	 *            {@link Integer} representation of the port for the IP.
	 */
	private void connect(String ip, int port) {
		try {
			logger.log(LogLevel.DEBUG, "Attempting to connect to server.");
			socket = new Socket();
			// socket.connect(new InetSocketAddress(ip, port));
			InetSocketAddress socketAddress = null;
			int triesLeft = 5;
			do {
				socketAddress = new InetSocketAddress(ip, port);
				triesLeft--;
				logger.log(LogLevel.DEBUG, "Creating socket address.");
			} while (socketAddress.isUnresolved() && triesLeft > 0);
			socket.connect(socketAddress, 0);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			out.println("CONNECTING" + USERNAME_SPLITTER + username);
			String input = in.readLine();
			logger.log(LogLevel.DEBUG, "Got raw input from server: " + input);
			if (input.startsWith("CONNECTING") && input.contains(":")) {
				connecting = false;
				logger.log(LogLevel.DEBUG, "Found connecting server.");
				seed = Long.parseLong(input.split(":")[1].split(USERNAME_SPLITTER)[0]);
				logger.log(LogLevel.DEBUG, "Got seed: " + seed);
				theirName = input.split(":")[1].split(USERNAME_SPLITTER)[1];
				logger.log(LogLevel.DEBUG, "Got their name: " + theirName);
				connected = true;
				logger.log(LogLevel.NORMAL, "Successfully connected to server!");
				maxConnectionAttempts = -1;
			}
		} catch (Exception e) {
			logger.err(LogLevel.WARNING, "Could not connect to server.");
			e.printStackTrace();
		}
	}

	/**
	 * Puts a given message into the {@link #queue} of messages to send to the
	 * {@link Server}.<br>
	 * Also logs what time this message was put in the queue to {@link #queueTimes},
	 * in case the {@link Client} needs to tell the difference in time between
	 * messages.
	 * 
	 * @param message
	 *            {@link String} of the message we wish to send to the server.
	 */
	public void sendMessageToServer(String message) {
		logger.log(LogLevel.DEBUG, "Queueing message to send to server: (" + message + ")");
		queue.add(message);
		queueTimes.add(System.currentTimeMillis());
	}

	/**
	 * Puts all <b>messages</b> in a {@link List} into the {@link #queue} of
	 * messages to send to the {@link Server}.<br>
	 * All messages sent through this message will be noted as being put into the
	 * {@link #queue} at the same exact time (or near, depending on processing
	 * time), via {@link #queueTimes}.<br>
	 * <br>
	 * To put it simply, takes the {@link List} of {@link String}s and submits them
	 * individually to {@link #sendMessageToServer(String)}.
	 * 
	 * @param messages
	 *            {@link List}<{@link String}> of all messages we want to send, in
	 *            the order we want to send them.
	 */
	public void sendMessagesToServer(List<String> messages) {
		for (int i = 0; i < messages.size(); i++)
			sendMessageToServer(messages.get(i));
	}

	/**
	 * Checks to see whether or not a given message is currently in {@link #queue}
	 * to send to the {@link Server}.<br>
	 * <br>
	 * Note that if a message is sent, it will no longer be in the {@link #queue},
	 * and therefore this will return false for it. There is no log, history, or
	 * {@link List} for messages <i>already</i> sent to the {@link Server}, only
	 * waiting.<br>
	 * <br>
	 * This method uses {@link String#equals(Object)} to determine if the message
	 * matches or not.
	 * 
	 * @param message
	 *            {@link String} of the message we want to check.
	 * @return Whether or not the message is still in the queue.
	 */
	public boolean hasMessageQueued(String message) {
		try {
			ArrayList<String> newQueue = new ArrayList<String>(queue);
			for (int i = 0; i < newQueue.size(); i++)
				if (newQueue.get(i).equals(message))
					return true;
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * Checks to see whether or not a given message is currently in {@link #queue}
	 * to send to the {@link Server}.<br>
	 * <br>
	 * Note that if a message is sent, it will no longer be in the {@link #queue},
	 * and therefore this will return false for it. There is no log, history, or
	 * {@link List} for messages <i>already</i> sent to the {@link Server}, only
	 * waiting.<br>
	 * <br>
	 * This method uses {@link String#contains(CharSequence)} to determine if the
	 * message matches or not.
	 * 
	 * @param message
	 *            {@link String} of the message we want to check.
	 * @return Whether or not the message is still in the queue.
	 */
	public boolean containsMessageQueued(String message) {
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
	 * Disconnects from the {@link Server} we are currently connected to.<br>
	 * <br>
	 * If we are not connected to a {@link Server}, returns without taking any
	 * action.
	 */
	public void disconnect() {
		onExit();
		logger.log(LogLevel.NORMAL, "Stopping client, disconnecting.");
		connectionTime = -1;
		if (connector != null) {
			connector.stop();
			connector = null;
		}
		connecting = false;
		stop = true;
		// if (!connected)
		// return;
		try {
			out.close();
		} catch (Exception e) {
		}
		try {
			in.close();
		} catch (Exception e) {
		}
		try {
			// socket.close();
			// Closing socket makes it hard to reconnect?
		} catch (Exception e) {
		}
		out = null;
		in = null;
		socket = null;
		connected = false;
	}

	/**
	 * Runs when {@link #disconnect()} is called, before we are disconnected (if we
	 * aren't already). You can override this method to run something before we
	 * destroy the connection and socket.
	 */
	public void onExit() {

	}

	/**
	 * Attempts to establish a connection with the {@link Server} via
	 * {@link #connect(String, int)}, meant for being used with a {@link Timer} to
	 * attempt connection until a connection is successfully established.<br>
	 * <br>
	 * Once a connection is established, this class instead uses
	 * {@link #connectToServer(String, int)} to communicate with the {@link Server}.
	 * 
	 * @author Nathan
	 * 
	 */
	public class Connector extends TimerTask {
		/**
		 * The {@link Thread}, used for handling actions in a separate Thread than the
		 * rest of the {@link Client}.
		 */
		public ConnectorThread thread = null;
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
		 * The IP address of the {@link Server} we wish to connect to.
		 */
		private String IP = null;
		/**
		 * The port of the {@link Server} we wish to connect to.
		 */
		private int port = -1;

		/**
		 * Initiates {@link Connector}.
		 * 
		 * @param ourTimer
		 *            {@link Connector#ourTimer}
		 * @param IP
		 *            {@link Connector#IP}
		 * @param port
		 *            {@link Connector#port}
		 */
		public Connector(Timer ourTimer, String IP, int port) {
			this.ourTimer = ourTimer;
			this.IP = IP;
			this.port = port;
			thread = new ConnectorThread();
			thread.start();
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
		 * @see ConnectorThread#run()
		 */
		public void run() {
			thread.go = true;
		}

		public class ConnectorThread extends Thread {
			/**
			 * Whether or not to run - changed by {@link Connector#run()}.
			 */
			public boolean go = false;

			/**
			 * Before we are connected to a {@link Server}, uses
			 * {@link #connect(String, int)} to connect to a {@link Server}.<br>
			 * <br>
			 * Once a connection is established, uses {@link #talkToServer()} to communicate
			 * with the {@link Server}.
			 */
			@Override
			public void run() {
				while (!override) {
					if (go) {
						go = false;
						// logger.log(LogLevel.DEBUG, "Running...");
						if (currentConnectionAttempt > maxConnectionAttempts && maxConnectionAttempts != -1) {
							logger.log(LogLevel.NORMAL, "Attempted to connect max amount of times! Disconnecting.");
							disconnect();
						} else {
							if (IP == null || port == -1) {
								logger.log(LogLevel.ERROR, "Connector attempting to connect, but no IP/port was set!");
								return;
							} else if (!override && !running && !stop) {
								running = true;
								currentConnectionAttempt++;
								if (!me.connected) {
									connectionTime = -1;
									logger.log(LogLevel.DEBUG,
											"Trying to connect to Server (" + IP + "/" + port + ")...");
									me.connect(IP, port);
								} else {
									if (connectionTime == -1)
										connectionTime = System.currentTimeMillis();
									// logger.log(LogLevel.DEBUG, "Trying to
									// connect and talk...");
									talkToServer();
								}
							} else {
								logger.err(LogLevel.WARNING, "?? " + override + ":" + running + ":" + stop);
							}
						}
						running = false;
					} else
						try {
							sleep(1);
						} catch (InterruptedException e) {
						}
				}
			}
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
			if (ourTimer != null) {
				ourTimer.cancel();
				ourTimer.purge();
				ourTimer = null;
			}
			override(true);
			thread.interrupt();
			cancel();
		}

		/**
		 * 
		 * @return {@link #IP}.
		 */
		public String getIP() {
			return IP;
		}
	}

	/**
	 * @param newUsername
	 *            The new username to set {@link #username} to.
	 */
	public void setUsername(String newUsername) {
		username = newUsername;
	}

	/**
	 * @return {@link #seed}.
	 */
	public long getSeed() {
		return seed;
	}

	/**
	 * Abstract class meant to be used to decode all messages sent from a
	 * {@link Server} through a {@link Handler}. This is where all the net-code
	 * decoding happens.
	 * 
	 * @author Nathan
	 * 
	 */
	public abstract static class ClientMessageHandler {
		public abstract void process(String message);

		/**
		 * Hard-coded pre-processing of {@link Server} input where necessary. <br>
		 * If message.startsWith(EXITING), disconnect Client.<br>
		 * If message.startsWith(MOVE_TO_P2P), we are moving to a Peer-to-Peer
		 * connection, so disconnect this Client and set {@link #p2pNetwork}.<br>
		 * If message.startsWith(SYNCTIME:), we are receiving a response from the Server
		 * with their currentTimeMillis, set {@link Client#timeOffset} accordingly.
		 * 
		 * @param client
		 *            The {@link Client} in which this message handling is taking place.
		 * @param message
		 *            input from a {@link Server}.
		 */
		public void preProcess(Client client, String message, int port) {
			if (message.contains(PING_SPLITTER)) {
				String username = message.split(USERNAME_SPLITTER)[1];
				String ping = message.split(PING_SPLITTER)[1].split(USERNAME_SPLITTER)[0];
				client.ping = Integer.parseInt(ping);
				message = message.split(PING_SPLITTER)[0] + USERNAME_SPLITTER + username;
			}
			if (message.startsWith("EXITING")) {
				client.disconnect();
				return;
			} else if (message.startsWith("MOVE_TO_P2P:")) {
				client.logger.log(LogLevel.WARNING,
						"Received message to move to Peer-to-Peer connectivity, shutting this Client down.",
						"Use client.getP2PNetwork() to retrieve the new P2PNetwork instance that is replacing this Client.",
						"If you want to use your own message handler, please add it to the P2PNetwork now.");
				String serverIP = client.connector.IP;
				Logger logger = client.logger.clone();
				client.disconnect();
				client.p2pNetwork = new P2PNetwork(
						serverIP + "@@@" + message.split(":")[1].substring(3).split(USERNAME_SPLITTER)[0], port, logger,
						client.timeBetweenConnectionAttempts, client.p2pServerMessageHandler,
						client.p2pClientMessageHandler);
			} else if (message.startsWith("SYNCTIME:")) {
				long time = Long.parseLong(message.split(":")[1].split(USERNAME_SPLITTER)[0]);
				long ourTime = System.currentTimeMillis();
				int delay = client.getPing();
				client.logger.log(LogLevel.ERROR, "Got SyncTime response from Server: " + time);
				long difference = time + delay - ourTime;
				client.logger.log(LogLevel.ERROR,
						"Our time is: " + ourTime + ", delay (rtt): " + delay + ", difference=" + difference);
				if (Math.abs(difference) < 20) {
					client.logger.log(LogLevel.ERROR, "Difference<20, so it's probably on-time, just leave it.");
				} else {
					client.logger.log(LogLevel.ERROR, "Difference was >20! (" + difference + ") Updated our time.");
					client.timeOffset = (int) difference;
				}
			}
			process(message);
		}
	}

	/**
	 * @return The P2PNetwork that this Client abandoned to, or <b>null</b> if not
	 *         in p2p network.
	 * @see {@link ClientMessageHandler#preProcess(Client, String)}).
	 */
	public P2PNetwork getP2PNetwork() {
		return p2pNetwork;
	}

	/**
	 * @return {@link #connector}.
	 */
	public Connector getConnector() {
		return connector;
	}

	/**
	 * @return {@link #theirName}.
	 */
	public String getTheirName() {
		return theirName;
	}

	/**
	 * @return {@link #socket}.
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * @return The current time, using the last-connected {@link Server} as a master
	 *         clock.
	 */
	public long currentTimeMillis() {
		return System.currentTimeMillis() + (long) timeOffset;
	}

}

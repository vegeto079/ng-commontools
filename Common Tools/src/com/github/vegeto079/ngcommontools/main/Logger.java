package com.github.vegeto079.ngcommontools.main;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logs messages to system output. Requires {@link LogLevel} to be passed in
 * every message to determine whether or not we want to display the given
 * message.<br>
 * <br>
 * For example, we can choose to display only {@link LogLevel#ERROR}. This means
 * any message said to be under any other {@link LogLevel} will be discarded.
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Added {@link #clone()}.
 * @version 1.02: Added {@link Logger#Logger(LogLevel...)}.
 * @version 1.03: Added {@link #getLevelsShown()}.
 * @version 1.04: Added
 *          {@link #levelsShownEqual(boolean, boolean, boolean, boolean)} and
 *          {@link #allLevelsShownEqual(boolean)}.
 */
public class Logger {
	/**
	 * The default value for {@link Logger#levelsShown}.
	 */
	private final static boolean[] DEFAULT_LEVELS_SHOWN = { false, true, true,
			true };
	/**
	 * Keeps track of what messages we should display, based on the
	 * {@link LogLevel} of the message.
	 */
	private boolean[] levelsShown = null;

	/**
	 * Enumerator to pass along with all logged messages. Used in determining
	 * whether or not we will discard the message rather than displaying it,
	 * depending on {@link Logger} initialization options.
	 * 
	 * @author Nathan
	 * 
	 */
	public static enum LogLevel {
		/**
		 * {@link LogLevel} of messages that are meant for debugging purposes
		 * only. Likely to be much more messages sent through this
		 * {@link LogLevel}. Likely to be filtered out for a release version.
		 */
		DEBUG,
		/**
		 * {@link LogLevel} of messages that are considered 'normal', in that
		 * they are not involved with debugging, they are not an important
		 * warning, and they are not an error. Just a general message. Could be
		 * filtered out for a release version.
		 */
		NORMAL,
		/**
		 * {@link LogLevel} of messages that are warnings. Not exactly an error
		 * in that it likely won't cause any issues, but it is something that
		 * should be noted. Could be used for things that may potentially cause
		 * an error if not taken care of.
		 */
		WARNING,
		/**
		 * {@link LogLevel} of messages that are an error. Shouldn't be filtered
		 * out in a release version, for use with debugging.
		 */
		ERROR;
	}

	/**
	 * Makes an unrelated clone of this Logger instance, using the same
	 * {@link #levelsShown} values.
	 * 
	 * @param logger
	 *            The Logger to clone.
	 */
	public Logger clone() {
		Logger logger = new Logger(true);
		logger.levelsShown = levelsShown;
		logger.levelsShown[0] = new Boolean(levelsShown[0]);
		logger.levelsShown[1] = new Boolean(levelsShown[1]);
		logger.levelsShown[2] = new Boolean(levelsShown[2]);
		logger.levelsShown[3] = new Boolean(levelsShown[3]);
		return logger;
	}

	/**
	 * Initiates {@link Logger}. Options decide which messages will be discarded
	 * or displayed, depending on their given {@link LogLevel}.
	 * 
	 * @param debugMessagesShown
	 *            Whether or not we want all debugging messages to be displayed.
	 * @param normalMessagesShown
	 *            Whether or not we want normal messages to be displayed.
	 * @param warningMessagesShown
	 *            Whether or not we want warning messages to be displayed.
	 * @param errorMessagesShown
	 *            Whether or not we want error messages to be displayed
	 *            (recommended always true).
	 */
	public Logger(boolean debugMessagesShown, boolean normalMessagesShown,
			boolean warningMessagesShown, boolean errorMessagesShown) {
		levelsShown = DEFAULT_LEVELS_SHOWN;
		levelsShown[0] = debugMessagesShown;
		levelsShown[1] = normalMessagesShown;
		levelsShown[2] = warningMessagesShown;
		levelsShown[3] = errorMessagesShown;
	}

	/**
	 * Initiates {@link Logger}. Options decide which messages will be discarded
	 * or displayed, depending on their given {@link LogLevel}.
	 * 
	 * @param showAllMessages
	 *            Whether or not to display every message, regardless of it's
	 *            {@link LogLevel}.
	 */
	public Logger(boolean showAllMessages) {
		levelsShown = DEFAULT_LEVELS_SHOWN;
		for (int i = 0; i < levelsShown.length; i++)
			levelsShown[i] = showAllMessages;
	}

	/**
	 * Initiates {@link Logger}. Any {@link LogLevel}s passed through this will
	 * be enabled. The rest will be disabled.
	 * 
	 * @param levels
	 *            The {@link LogLevel}s to enable.
	 */
	public Logger(LogLevel... levels) {
		levelsShown = DEFAULT_LEVELS_SHOWN;
		for (int i = 0; i < levelsShown.length; i++)
			levelsShown[i] = false;
		for (LogLevel level : levels) {
			if (level.equals(LogLevel.DEBUG))
				levelsShown[0] = true;
			else if (level.equals(LogLevel.NORMAL))
				levelsShown[1] = true;
			else if (level.equals(LogLevel.WARNING))
				levelsShown[2] = true;
			else if (level.equals(LogLevel.ERROR))
				levelsShown[3] = true;
		}
	}

	/**
	 * Prints any number of {@link String}s to the system output.<br>
	 * <br>
	 * Will not print the {@link String} if it's given {@link LogLevel} was set
	 * to not be shown in system output.<br>
	 * <br>
	 * If <b>logLevel</b> is a {@link LogLevel#WARNING} or
	 * {@link LogLevel#ERROR} the message will be sent through
	 * {@link Logger#err(LogLevel, String...)} instead.
	 * 
	 * @param logLevel
	 *            The level of 'importance' given to this message via
	 *            {@link LogLevel}.
	 * @param strings
	 *            The {@link String}s to print to system output.
	 */
	public void log(LogLevel logLevel, String... strings) {
		if (!levelsShown[LogLevel.valueOf(logLevel.name()).ordinal()])
			return;
		if (logLevel.equals(LogLevel.WARNING)
				|| logLevel.equals(LogLevel.ERROR)) {
			err(logLevel, strings);
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("(hh:mm:ss) ");
		Date date = new Date();
		for (int i = 0; i < strings.length; i++)
			System.out.println(sdf.format(date) + strings[i]);
	}

	/**
	 * Runs {@link #log(LogLevel, String...)} using {@link LogLevel#NORMAL}.
	 * 
	 * @param strings
	 *            The {@link String}s to print to system output.
	 */
	public void log(String... strings) {
		log(LogLevel.NORMAL, strings);
	}

	/**
	 * Prints any number of {@link String}s to the system output as an error
	 * message (by default, in red).<br>
	 * <br>
	 * Will not print the {@link String} if it's given {@link LogLevel} was set
	 * to not be shown in system output.<br>
	 * <br>
	 * If <b>logLevel</b> is a {@link LogLevel#DEBUG} or {@link LogLevel#NORMAL}
	 * the message will be sent through {@link Logger#log(LogLevel, String...)}
	 * instead.
	 * 
	 * @param logLevel
	 *            The level of 'importance' given to this message via
	 *            {@link LogLevel}.
	 * @param strings
	 *            The {@link String}s to print to system output.
	 */
	public void err(LogLevel logLevel, String... strings) {
		if (!levelsShown[LogLevel.valueOf(logLevel.name()).ordinal()])
			return;
		if (logLevel.equals(LogLevel.DEBUG) || logLevel.equals(LogLevel.NORMAL)) {
			log(logLevel, strings);
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("(hh:mm:ss) ");
		Date date = new Date();
		for (int i = 0; i < strings.length; i++) {
			System.err.println(sdf.format(date) + strings[i]);
		}
	}

	/**
	 * @see {@link #levelsShown}.
	 */
	public boolean[] getLevelsShown() {
		return levelsShown;
	}

	/**
	 * @param debug
	 * @param normal
	 * @param warning
	 * @param error
	 * @return Whether or not all variables equal their counterpart in this
	 *         class.
	 * @see {@link #levelsShown}.
	 */
	public boolean levelsShownEqual(boolean debug, boolean normal,
			boolean warning, boolean error) {
		return levelsShown[0] == debug && levelsShown[1] == normal
				&& levelsShown[2] == warning && levelsShown[3] == error;
	}

	/**
	 * @param allLevels
	 * @return Whether or not <b>allLevels</b> is the same value as every value
	 *         in {@link #levelsShown}.
	 */
	public boolean allLevelsShownEqual(boolean allLevels) {
		return levelsShownEqual(allLevels, allLevels, allLevels, allLevels);
	}
}

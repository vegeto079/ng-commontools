package com.github.vegeto079.ngcommontools.audio;

import java.io.File;
import java.util.ArrayList;

import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.ResourceHandler;
import com.github.vegeto079.ngcommontools.main.Tools;
import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

/**
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Made file-getting linux-safe.
 * @version 1.02: Replaced all instances of 'sound' with 'song' since this is a
 *          music player, not a sound player.
 */
public class MusicHandler {
	private File[] songs = null;
	private AudioCore audioCore = null;
	private boolean stopping = false;
	private float volume = 0.5f;

	private boolean debug = true;

	private Logger logger = null;

	public MusicHandler(Logger logger) {
		this.logger = logger;
		try {
			String mainPath = ResourceHandler.get(logger, "music", Tools.getJarName());
			if (System.getProperty("os.name").contains("Linux")) {
				if (debug)
					logger.log(LogLevel.DEBUG, "On linux, adjusting path accordingly.");
				mainPath = "/" + mainPath;
			}
			if (debug)
				logger.log(LogLevel.DEBUG, "Got main path: " + mainPath);
			if (debug)
				logger.log(LogLevel.DEBUG, "mainPath.exists(): " + new File(mainPath).exists());
			File folder = new File(mainPath);
			songs = folder.listFiles();
			for (int i = 0; i < songs.length; i++)
				if (debug)
					logger.log(LogLevel.DEBUG, "Found song #" + i + ": " + songs[i].getName());
		} catch (Exception e) {
		}
		if (debug)
			logger.log(LogLevel.DEBUG, "Music Handler created.");
	}

	public void stop() {
		if (debug)
			logger.log(LogLevel.DEBUG, "stop() called.");
		if (audioCore != null)
			audioCore.stop();
		audioCore = null;
	}

	public boolean loop(String songName) {
		return loop(songName, 0);
	}

	public boolean loop(String songName, int amt) {
		if (stopping)
			return false;
		if (debug)
			logger.log(LogLevel.DEBUG, "loop(" + songName + "," + amt + ") called.");
		if (isPlaying(songName))
			return false;
		int songIdx = findIdx(songName);
		if (songIdx == -1)
			return false;
		if (songIdx == -2) {
			if (debug)
				logger.err(LogLevel.WARNING, "No songs files found, ignoring play request.");
			return false;
		} else {
			try {
				if (debug)
					logger.log(LogLevel.DEBUG, "Going to start playing now!");
				stop();
				audioCore = new AudioCore(logger, songs[songIdx].getAbsolutePath(), volume);
				audioCore.loop(amt);

			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	public boolean play(String songName) {
		return play(findIdx(songName));
	}

	public boolean play(int idx) {
		if (stopping)
			return false;
		if (debug)
			logger.log(LogLevel.DEBUG, "play(" + idx + ") called.");
		if (idx == -1)
			return false;
		else {
			try {
				stop();
				if (debug)
					logger.log(LogLevel.DEBUG, "Playing song: " + songs[idx].getAbsolutePath());
				audioCore = new AudioCore(logger, songs[idx].getAbsolutePath(), volume);
				audioCore.play();
				audioCore.setVolume(volume);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	public void changeVolume(float gain) {
		setVolume(volume + gain);
	}

	public void setVolume(float gain) {
		if (gain > 1) {
			if (debug)
				logger.log(LogLevel.DEBUG, "Volume cannot go higher than 1 gain. (" + gain + ")");
			else
				logger.log(LogLevel.WARNING, "Volume cannot go any higher.");
			gain = 1;
		} else if (gain < 0) {
			if (debug)
				logger.log(LogLevel.DEBUG, "Volume cannot go lower than 0 gain. (" + gain + ")");
			else
				logger.log(LogLevel.WARNING, "Volume cannot go any lower.");
			gain = 0;
		} else {
			volume = gain;
			if (audioCore != null)
				audioCore.setVolume(gain);
		}
	}

	public int getVolume() {
		return (int) (volume * 100f);
	}

	private int findIdx(String songName) {
		if (songs == null)
			return -2;
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		for (int i = 0; i < songs.length; i++)
			if (songs[i].getName().toLowerCase().contains(songName.toLowerCase())) {
				indexList.add(i);
				break;
			}
		if (indexList.size() > 0)
			return indexList.get(0);
		// If there's multiple audio files of this name, pick a random one:
		// return indexList.get(Common.random(clue, 0, indexList.size() - 1,
		// "songHandler findIdx"));
		return -1;
	}

	public boolean isPlaying() {
		// logger.log(LogLevel.DEBUG,"isPlaying() called.");
		if (songs == null) // If song files don't exist, return true
			return true; // So the game will still play correctly
		return audioCore != null && audioCore.isPlaying();
	}

	public boolean isPlaying(String title) {
		// logger.log(LogLevel.DEBUG,"isPlaying(" + title + ") called.");
		if (songs == null) // If song files don't exist, return true
			return true; // So the game will still play correctly
		if (title.equals(""))
			return isPlaying();
		return audioCore != null && audioCore.isPlaying(title);
	}

	public String getPlaying() {
		if (audioCore == null)
			return "null";
		// logger.log(LogLevel.DEBUG,"getPlaying() called.");
		return audioCore.getPlaying();
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}

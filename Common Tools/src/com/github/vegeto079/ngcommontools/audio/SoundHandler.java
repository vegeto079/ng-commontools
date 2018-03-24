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
 * @version 1.01: Waits 10ms between playing audio clips, since they seem to
 *          interfere with each other if played at the same exact time. May need
 *          to increase this number, but that may introduce slight audio lag.
 * @version 1.011: Changed to 50ms, we'll see if it introduces too much lag.
 *          Might this actually make it worse by backing up the queue of
 *          sounds..? I think so.
 * @version 1.02: Changed to 30ms, a reasonable compromise it seems. Now using
 *          {@link #lastPlayedAudioIdx} to determine the last played sound, and
 *          if it's the same as the previous one (within a 30ms time), just try
 *          to play it anyway. If it has a problem playing that's ok, since we
 *          did just hear the sound less than 30ms ago.
 * @version 1.03: Made file-retrieval linux-safe.
 * @version 1.04: Sound playing is now handled through threads in this class
 *          rather than directly in {@link AudioCore}. {@link CorePlayer} is a
 *          {@link Thread} that handles running the sound.
 * @version 1.05: Changing volume no longer changes already-running sound's
 *          volume. This was causing a weird bug where for a moment it would go
 *          to full volume. Instead, just change volume of all new sounds.
 */
public class SoundHandler {
	private File[] sounds = null;
	private ArrayList<AudioCore> audioCores = new ArrayList<AudioCore>();
	private boolean stopping = false;
	private float volume = 0.5f;

	private boolean debug = true;

	private Logger logger = null;

	private long lastPlayedAudio = -1;
	private long lastPlayedAudioIdx = -1;

	public SoundHandler(Logger logger) {
		this.logger = logger;
		try {
			String mainPath = ResourceHandler.get(logger, "audio", Tools.getJarName());
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
			sounds = folder.listFiles();
			for (int i = 0; i < sounds.length; i++)
				if (debug)
					logger.log(LogLevel.DEBUG, "Found sound #" + i + ": " + sounds[i].getName());
		} catch (Exception e) {
		}
		if (debug)
			logger.log(LogLevel.DEBUG, "Sound Handler created.");
	}

	public void stopAll() {
		if (debug)
			logger.log(LogLevel.DEBUG, "stopAll() called.");
		while (audioCores.size() > 0) {
			if (audioCores.get(0) != null)
				audioCores.get(0).stop();
			audioCores.set(0, null);
			audioCores.remove(0);
		}
	}

	/**
	 * Stops a given sound.
	 * 
	 * @param title
	 * @return Whether we found and stopped a clip with <b>title</b>.
	 */
	public boolean stop(String title) {
		if (debug)
			logger.log(LogLevel.DEBUG, "stop(" + title + ") called.");
		for (int i = 0; i < audioCores.size(); i++)
			if (audioCores.get(i).isPlaying(title)) {
				audioCores.get(i).stop();
				return true;
			}
		return false;
	}

	public void play(String soundName) {
		play(findIdx(soundName), volume);
	}

	public void play(String soundName, float volume) {
		play(findIdx(soundName), volume);
	}

	public void play(int idx, float volume) {
		Thread player = new Thread(new CorePlayer(idx, volume), "Sound Playing: " + idx);
		player.start();
	}

	public class CorePlayer extends Thread {
		boolean ran = false, running = false, returnValue = false;
		int idx;
		float volume;
		Thread thread;

		public CorePlayer(int idx, float volume) {
			this.idx = idx;
			this.volume = volume;
			thread = new Thread(new CoreTask(), "CorePlayer[" + idx + "," + volume + "]");
		}

		public void run() {
			thread.start();
		}

		public class CoreTask implements Runnable {
			public void run() {
				ran = true;
				running = true;
				while (System.currentTimeMillis() - lastPlayedAudio < 30 && lastPlayedAudioIdx != idx) {
					// Wait if we just played another audio, it's likely to be
					// skipped if we try to play them both at the same exact
					// time for some reason. Ignore this problem if it's the
					// same sound, it should be OK if you don't hear the same
					// sound within 30ms.
				}
				lastPlayedAudio = System.currentTimeMillis();
				lastPlayedAudioIdx = idx;
				int indexAudioToUse = -1;
				for (int i = 0; i < audioCores.size() && indexAudioToUse == -1; i++)
					try {
						if (!audioCores.get(i).isPlaying()) {
							indexAudioToUse = i;
							audioCores.get(i).playing = true;
						}
					} catch (Exception e) {
					}
				removeFinishedAudio();
				if (stopping) {
					returnValue = false;
					return;
				}
				if (debug)
					logger.log(LogLevel.DEBUG, "play(" + idx + ") called.");
				if (idx == -1) {
					returnValue = false;
					return;
				} else {
					try {
						// stopAll();
						if (debug)
							logger.log(LogLevel.DEBUG, "Playing sound: " + sounds[idx].getAbsolutePath());
						AudioCore audioCore;
						if (indexAudioToUse == -1) {
							audioCore = new AudioCore(logger, sounds[idx].getAbsolutePath(), volume);
							audioCore.play();
							audioCores.add(audioCore);
						} else {
							audioCores.get(indexAudioToUse).stop();
							audioCore = new AudioCore(logger, sounds[idx].getAbsolutePath(), volume);
							audioCore.play();
							audioCores.set(indexAudioToUse, audioCore);
						}
					} catch (Exception e) {
						// e.printStackTrace();
						returnValue = false;
						return;
					}
					returnValue = true;
				}
				running = false;
			}
		}
	}

	private void removeFinishedAudio() {
		boolean done = false;
		int i = 0;
		int removed = 0;
		while (!done) {
			try {
				if (i >= audioCores.size())
					break;
				if (!audioCores.get(i).isPlaying()) {
					audioCores.get(i).stop();
					audioCores.remove(i);
					removed++;
				} else
					i++;
			} catch (Exception e) {
				// Concurrent exceptions can occur here, just ignore, not too
				// important
			}
		}
		if (debug && removed > 0)
			logger.log(LogLevel.DEBUG, "Removed " + removed + " finished audios.");
	}

	public void changeVolume(float gain) {
		setVolume(volume + gain);
	}

	public void setVolume(float gain) {
		if (gain > 1) {
			if (debug)
				logger.log(LogLevel.DEBUG, "Volume cannot go higher than 1 gain.");
			else
				logger.log(LogLevel.WARNING, "Volume cannot go any higher.");
			gain = 1;
		} else if (gain < 0) {
			if (debug)
				logger.log(LogLevel.DEBUG, "Volume cannot go lower than 0 gain.");
			else
				logger.log(LogLevel.WARNING, "Volume cannot go any lower.");
			gain = 0;
		} else {
			// for (AudioCore audioCore : audioCores)
			// audioCore.changeVolume(gain);
			// Bug: when changing audios mid-play, they go full volume for a
			// second. Very annoying. Instead, lets just make every new sound
			// the correct volume.
		}
		volume = gain;
	}

	public int getVolume() {
		return (int) (volume * 100f);
	}

	public float getVolumeFloat() {
		return volume;
	}

	private int findIdx(String soundName) {
		if (sounds == null)
			return -2;
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		for (int i = 0; i < sounds.length; i++)
			if (sounds[i].getName().toLowerCase().contains(soundName.toLowerCase())) {
				indexList.add(i);
				break;
			}
		if (indexList.size() > 0)
			return indexList.get(0);
		// If there's multiple audio files of this name, pick a random one:
		// return indexList.get(Common.random(clue, 0, indexList.size() - 1,
		// "SoundHandler findIdx"));
		return -1;
	}

	public boolean isPlaying() {
		// logger.log(LogLevel.DEBUG,"isPlaying() called.");
		if (sounds == null) // If sound files don't exist, return true
			return true; // So the game will still play correctly
		for (AudioCore audioCore : audioCores)
			if (audioCore.isPlaying())
				return true;
		return false;
	}

	public boolean isPlaying(String title) {
		// logger.log(LogLevel.DEBUG,"isPlaying(" + title + ") called.");
		if (sounds == null) // If sound files don't exist, return true
			return true; // So the game will still play correctly
		if (title.equals(""))
			return isPlaying();
		ArrayList<AudioCore> cores = new ArrayList<AudioCore>(audioCores);
		for (AudioCore audioCore : cores)
			if (audioCore != null && audioCore.isPlaying(title))
				return true;
		return false;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}

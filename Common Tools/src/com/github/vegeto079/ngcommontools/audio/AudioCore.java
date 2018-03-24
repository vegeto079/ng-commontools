package com.github.vegeto079.ngcommontools.audio;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingWorker;

import com.github.vegeto079.ngcommontools.main.Logger;
import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

/**
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Thread.sleep() changed to 5ms in the looping method. Shouldn't
 *          be too draining (like 1ms), but might help the problem of two audios
 *          trying to play at once, cancelling one out.
 * @version 1.02: Added {@link CorePlayer}, an extension of {@link Thread}. This
 *          means all audio played is handled in a separate thread rather than
 *          the thread that called this {@link SwingWorker}.
 * @version 1.03: {@link #isPlaying()} changed to just returning
 *          {@link #playing}, since that should be the ultimate indicator,
 *          rather than checking the clip's play state.
 * @version 1.04: {@link CorePlayer} now checks if {@link #thisAudio} is
 *          <b>null</b> before trying to stop it.
 * @version 1.05: We are no longer a {@link SwingWorker}, since we want to
 *          handle all threading ourselves.
 * @version 1.06: Removed all traces of using any Timers to play audio in the
 *          background. Instead, {@link CorePlayer} handles all
 *          background-playing.
 * @version 1.07: {@link System#gc()} no longer called when playing a sound: it
 *          takes up a bit of cpu when sounds play.
 */
public class AudioCore {
	private File file = null;
	private boolean stopping = false;
	private OggClip clip = null;
	private AudioCore thisAudio = null;
	boolean playing = false;

	private float volume = -10f;

	private boolean doLoop = false;
	private int loopAmt = -1;
	private int currentLoop = -1;
	private boolean playStaticCall = false;

	private boolean debug = false;

	private Logger logger = null;

	private Thread corePlayer = null;

	public AudioCore(Logger logger, String fileURL, float volume)
			throws Exception {
		this.logger = logger;
		if (debug)
			logger.log(LogLevel.DEBUG, "Created sound.");
		file = new File(fileURL);
		if (debug)
			logger.log(LogLevel.DEBUG, "fileURL: " + fileURL);
		if (volume != -1 && volume != 0)
			this.volume = volume;
		if (fileURL.toLowerCase().contains("guile"))
			this.volume += 10;
		if (this.volume > 1)
			this.volume = 1;
		if (this.volume < 0)
			this.volume = 0;
		if (debug)
			logger.log(LogLevel.DEBUG, "volume: " + volume);
		thisAudio = this;
	}

	public void loop() {
		loop(0);
	}

	public void loop(int amt) {
		if (debug)
			logger.log(LogLevel.DEBUG, "loop(" + amt + ") called");
		loopAmt = amt;
		doLoop = true;
		try {
			run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void play() {
		if (debug)
			logger.log(LogLevel.DEBUG, "play() called");
		play(true);
	}

	public void play(boolean staticCall) {
		if (debug)
			logger.log(LogLevel.DEBUG, "play(" + staticCall + ") called");
		playStaticCall = staticCall;
		try {
			run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setVolume(float gain) {
		if (debug)
			logger.log(LogLevel.DEBUG, "setVolume(" + gain + ") called");
		try {
			volume = gain;
			try {
				clip.setGain(volume);
			} catch (Exception e) {
			}
			if (debug)
				logger.log(LogLevel.DEBUG, "Volume now changed to " + volume);
		} catch (Exception e) {
			logger.err(LogLevel.WARNING, "Problem changing volume.");
			// if (debug)
			e.printStackTrace();
		}
	}

	public void changeVolume(float gain) {
		setVolume(volume + gain);
	}

	public class CorePlayer extends Thread {
		boolean staticCall = false;

		public CorePlayer(boolean staticCall) {
			this.staticCall = staticCall;
		}

		public void run() {
			if (debug)
				logger.log(LogLevel.DEBUG,
						"realplay() called. file: " + file.getAbsolutePath());
			if (stopping)
				return;
			// System.gc();
			playing = true;
			// AudioInputStream audioInputStream = AudioSystem
			// .getAudioInputStream(file);
			try {
				clip = new OggClip(file.getAbsolutePath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			clip.setGain(volume);
			// log("Got the listener.");
			try {
				// log("Going to get clip..");
				// clip = AudioSystem.getClip();
				// clip.addLineListener(listener);
				// clip.open(audioInputStream);
				// log("Clip retrieved.");
				// changeVolume(0);
				try {
					// log("Starting clip..");
					// clip.start();
					if (debug)
						logger.log("Playing!");
					clip.play();
					while (!clip.stopped())
						try {
							if (stopping)
								break;
							if (debug)
								logger.log(LogLevel.DEBUG, "Waiting for "
										+ file.getName() + " sound to end..");
							Thread.sleep(5);
						} catch (Exception e) {
						}
				} finally {
					if (staticCall && !doLoop)
						stopping = true;
				}
			} finally {
				if (!doLoop || stopping && thisAudio != null)
					try {
						thisAudio.stop();
					} catch (Exception e) {
						// ignore?
					}
				playing = false;
			}
			if (!stopping)
				try {
					run();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

	public void stop() {
		stopping = true;
		if (debug)
			logger.log(LogLevel.DEBUG, "stop() called");
		if (clip != null) {
			clip.stop();
			clip.close();
			if (debug)
				logger.log(LogLevel.DEBUG, "clip.close() called");
		}
		if (corePlayer != null) {
			corePlayer.interrupt();
		}
		corePlayer = null;
		thisAudio = null;
		playing = false;
		doLoop = false;
		if (debug)
			logger.log(LogLevel.DEBUG, "realstop() done");
	}

	public boolean isStopping() {
		return stopping;
	}

	public String getFileURL() {
		return file.getAbsolutePath();
	}

	public void setFileURL(String fileURL) {
		file = new File(fileURL);
	}

	public boolean isPlaying() {
		// return playing || (clip != null && !clip.stopped());
		return playing;
	}

	public boolean isPlaying(String title) {
		return isPlaying() && file != null
				&& file.getName().toLowerCase().contains(title.toLowerCase());
	}

	public String getPlaying() {
		if (file == null)
			return "null";
		String path = file.getAbsolutePath().replace("\\", "/");
		String[] splitPath = path.split("/");
		String fileName = splitPath[splitPath.length - 1];
		return fileName.toLowerCase();
	}

	public void realplay(boolean staticCall) {
		playing = true;
		thisAudio = this;
		corePlayer = new Thread(new CorePlayer(staticCall), "Audio Core Player");
		corePlayer.start();
	}

	protected void run() throws Exception {
		if (playing)
			return;
		if (!doLoop) {
			if (debug)
				logger.log(LogLevel.DEBUG, "doLoop = false");
			realplay(playStaticCall);
		} else {
			if (debug)
				logger.log(LogLevel.DEBUG, "doLoop = true");
			if (loopAmt < 0) {
				stop();
				throw new Exception("Loop amount cannot be below zero.");
			} else if (loopAmt == 0)
				realplay(false);
			else {
				currentLoop++;
				if (currentLoop < loopAmt)
					realplay(false);
				else
					stop();
			}
		}
		return;
	}

}

package com.github.vegeto079.ngcommontools.main;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.newdawn.slick.util.ResourceLoader;

import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

/**
 * Miscellanious useful tools
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: {@link #getAverage(ArrayList)} fixed to recreate the
 *          {@link ArrayList} rather than trying to access it while it may
 *          change.
 * @version 1.021: Added {@link #copyFile(File, File)} and
 *          {@link #findFileOnLibraryPath(String)}.
 * @version 1.03: Added {@link #getTextHeight(Graphics)},
 *          {@link #getTextHeight(Graphics, Font)}, and
 *          {@link #getTextWidth(Graphics, String)}.
 * @version 1.04: Added {@link #imageEquals(BufferedImage, BufferedImage)}.
 * @version 1.05: Added {@link #getResourceAsStream(String)}.
 * @version 1.06: Added {@link #displayInputDialog(String...)}.
 * @version 1.07: Added {@link #getTextWidth(Graphics, Font, String)} to match
 *          {@link #getTextHeight(Graphics, Font)}.
 * @version 1.08: {@link #getAverage(ArrayList)} now returns -1 if it runs into
 *          any Exception. For some reason it was getting NullPointerExceptions
 *          here or there, figure it's best to safely ignore those.
 * @version 1.09: {@link #copyFile(File, File)} didn't close file input/output
 *          streams, could lead to memory leaks.
 * @version 1.91: Added
 *          {@link #displayInputDialog(String, String, int, String...)}.
 * @version 1.92: Added {@link #displayPasswordDialog(String...)}.
 * @version 1.93: Added
 *          {@link #displayMultipleChoiceDialog(String, String, int, String...)}
 *          .
 * @version 1.94: Added {@link #round(double, int)}.
 * @version 1.941: Updated {@link #getTextHeight(Graphics, Font, String)}.
 */
public class Tools {
	/**
	 * @param number
	 * @return Whether or not <b>number</b> is even.
	 */
	public static boolean isEven(int number) {
		return number % 2 == 0;
	}

	/**
	 * @param number
	 * @return Whether or not <b>number</b> is odd.
	 */
	public static boolean isOdd(int number) {
		return !isEven(number);
	}

	/**
	 * @param avg
	 *            An {@link ArrayList}<{@link E}> of all numbers you wish to find
	 *            the average to.
	 * @return Calculates the average of <b>avg</b>.
	 */
	public static <E> double getAverage(ArrayList<E> avg) {
		try {
			ArrayList<E> newAvg = new ArrayList<E>(avg);
			if (newAvg.size() == 0)
				return 0;
			double all = 0;
			for (int i = 0; i < newAvg.size(); i++)
				all += Double.parseDouble(newAvg.get(i).toString());
			all = all / (double) newAvg.size();
			return all;
		} catch (Exception e) {
		}
		return -1;
	}

	public static <E> int getMedian(ArrayList<E> med) {
		try {
			ArrayList<E> newMed = new ArrayList<E>(med);
			int[] intMed = new int[newMed.size()];
			for (int i = 0; i < newMed.size(); i++)
				intMed[i] = Integer.parseInt(newMed.get(i).toString());
			Arrays.sort(intMed);
			int middle = intMed.length / 2;
			if (intMed.length % 2 == 0) {
				int medianA = intMed[middle];
				int medianB = intMed[middle - 1];
				return (medianA + medianB) / 2;
			} else
				return intMed[middle + 1];
		} catch (Exception e) {
		}
		return -1;
	}

	public static boolean displayConfirmDialog(final String... whatToSay) {
		String endMessage = "";
		for (int x = 0; x < whatToSay.length; x++)
			endMessage = endMessage + "\n" + whatToSay[x];
		return JOptionPane.showConfirmDialog(null, endMessage) == JOptionPane.YES_OPTION;
	}

	public static void displayDialog(String... whatToSay) {
		String endMessage = "";
		for (int x = 0; x < whatToSay.length; x++)
			endMessage = endMessage + "\n" + whatToSay[x];
		JOptionPane.showMessageDialog(null, endMessage);
	}

	public static String displayPasswordDialog(String title, String... whatToSay) {
		String endMessage = "";
		for (int x = 0; x < whatToSay.length; x++)
			endMessage = endMessage + "\n" + whatToSay[x];
		JPanel p = new JPanel();
		JLabel l = new JLabel(endMessage);
		final JPasswordField jpf = new JPasswordField(10);
		p.add(l);
		p.add(jpf);
		JOptionPane jop = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dlg = jop.createDialog(title);
		dlg.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				jpf.requestFocusInWindow();
			}
		});
		dlg.setVisible(true);
		while (jop.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
			try {
				Thread.sleep(1);
			} catch (Exception e) {
			}
		}
		if (jop.getValue() == null)
			return null;
		else
			return new String(jpf.getPassword());
	}

	public static String displayInputDialog(String... whatToSay) {
		String endMessage = "";
		for (int x = 0; x < whatToSay.length; x++)
			endMessage = endMessage + "\n" + whatToSay[x];
		return JOptionPane.showInputDialog(null, endMessage);
	}

	public static String displayInputDialog(String title, String defaultText, int messageType, String... whatToSay) {
		String endMessage = "";
		for (int x = 0; x < whatToSay.length; x++)
			endMessage = endMessage + "\n" + whatToSay[x];
		return (String) JOptionPane.showInputDialog(null, endMessage, title, messageType, null, null, defaultText);
	}

	public static String displayMultipleChoiceDialog(String title, String text, int messageType, String... choices) {
		String[] newChoices = new String[choices.length];
		for (int i = 0; i < choices.length; i++)
			newChoices[i] = choices[i];
		return (String) JOptionPane.showInputDialog(null, text, title, messageType, null, newChoices, newChoices[0]);
	}

	public static String getJarName() {
		return new java.io.File(Tools.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
	}

	public static String[] changeArraySafely(String[] old) {
		String[] newArray = new String[old.length];
		String line = null;
		for (int i = 0; i < newArray.length; i++) {
			line = old[i];
			newArray[i] = line;
		}
		return newArray;
	}

	public static ArrayList<Point> getPoints(Shape s) {
		ArrayList<Point> points = new ArrayList<Point>();
		FlatteningPathIterator iter = new FlatteningPathIterator(s.getPathIterator(new AffineTransform()), 1);
		float[] coords = new float[6];
		while (!iter.isDone()) {
			iter.currentSegment(coords);
			points.add(new Point((int) coords[0], (int) coords[1]));
			iter.next();
		}
		return points;
	}

	public static Point getPointAtPercent(ArrayList<Point> list, float percent) {
		return list.get((int) (percent * ((float) list.size() - 1f)));
	}

	public static Point getPointAtListPercent(Point[] list, float percent) {
		return list[getIntegerAtListPercent(list, percent)];
	}

	public static int getIntegerAtListPercent(Point[] list, float percent) {
		// if (percent > 1)
		// percent = 1;
		// if (percent < 0)
		// percent = 0;
		return ((int) (percent * ((float) list.length - 1f)));
	}

	public static Point[] toArray(ArrayList<Point> list) {
		Point[] newList = new Point[list.size()];
		for (int i = 0; i < newList.length; i++)
			newList[i] = list.get(i);
		return newList;
	}

	/**
	 * Creates an oval/ellipse with given parameters (first line), then writes down
	 * every single Point in the shape, sorted in a circular fashion.
	 */
	public static void main2(String[] args) {
		// Ellipse2D ellipse = new Ellipse2D.Float(12, 32, 604, 80);
		// Ellipse2D ellipse = new Ellipse2D.Float(63, 60, 610, 187);
		Ellipse2D ellipse = new Ellipse2D.Float(-125, 60, 850, 165);
		double belowZero = 0;
		if (ellipse.getX() <= 5) {
			belowZero = Math.abs(ellipse.getX() + 5);
			ellipse = new Ellipse2D.Double(ellipse.getX() + belowZero, ellipse.getY(), ellipse.getWidth(),
					ellipse.getHeight());
		}
		ArrayList<Point> pList = new ArrayList<Point>();
		BufferedImage img = new BufferedImage((int) (ellipse.getX() + ellipse.getWidth() + 250),
				(int) (ellipse.getY() + ellipse.getHeight() + 250), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.draw(ellipse);
		for (int i = 0; i < img.getWidth(); i++)
			for (int j = 0; j < img.getHeight(); j++)
				if (img.getRGB(i, j) != 0) {
					System.out.println("Found non-black color: (" + i + "," + j + "): " + img.getRGB(i, j));
					pList.add(new Point((int) (i - belowZero), j));
				}

		System.out.println("Getting bottom point.");
		Point bottomPoint = new Point(0, Integer.MIN_VALUE);
		for (int i = 0; i < pList.size(); i++)
			if (pList.get(i).y > bottomPoint.y)
				bottomPoint = pList.get(i);

		System.out.println("Getting all bottom points.");
		ArrayList<Point> bottomPoints = new ArrayList<Point>();
		for (int i = 0; i < pList.size(); i++)
			if (pList.get(i).y == bottomPoint.y)
				bottomPoints.add(pList.get(i));

		Point middleBottomPoint = bottomPoints.get(bottomPoints.size() / 2);
		Point nextMiddleBottomPoint = bottomPoints.get(bottomPoints.size() / 2 + 1);

		System.out.println("Got middle bottom point: " + middleBottomPoint);

		System.out.println("Going to sort oval.. (size: " + pList.size() + ")");

		ArrayList<Point> newList = new ArrayList<Point>();
		// newList.add(pList.get(0));
		// Point lastPoint = pList.get(0);
		// pList.remove(0);
		newList.add(middleBottomPoint);
		Point lastPoint = nextMiddleBottomPoint;
		pList.remove(middleBottomPoint);
		while (pList.size() > 0) {
			Point nearest = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
			int i = 0;
			for (i = 0; i < pList.size(); i++) {
				if (pList.get(i).distance(lastPoint) < nearest.distance(lastPoint)) {
					nearest = pList.get(i);
				}
			}
			newList.add(nearest);
			lastPoint = nearest;
			pList.remove(nearest);
		}
		System.out.println("Sorted (size: " + newList.size() + ")");

		int remove = 2, removeCount = 0;
		ArrayList<Integer> removeIndex = new ArrayList<Integer>();
		System.out.println("Removing every 1 in every " + remove + " point");
		for (int i = 0; i < newList.size(); i++) {
			if (remove == removeCount) {
				removeCount = 0;
				removeIndex.add(i);
			}
			removeCount++;
		}
		// System.out.println("Starting removal");
		// for (int i = removeIndex.size() - 1; i >= 0; i--) {
		// System.out.println("Removing: " + removeIndex.get(i));
		// newList.remove(removeIndex.get(i));
		// }
		// System.out.println("Removed (size: " + newList.size() + ")");

		String print = "public static Point[] oval = {new Point(" + newList.get(0).x + "," + newList.get(0).y + ")";
		for (int i = 1; i < newList.size(); i++)
			print += ",new Point(" + newList.get(i).x + "," + newList.get(i).y + ")";
		print += "};";
		System.out.println(print);
		System.out.println("");
	}

	public static int random(Game game, int min, int max, String purpose) {
		int num = 0;
		if (game == null) {
			num = new Random().nextInt(max - min + 1);
		} else {
			num = game.random.nextInt(max - min + 1);
			game.randomCount++;
			// game.logger.log(LogLevel.DEBUG,"Random #" + game.randomCount +
			// ": " + num + " (" + purpose + ")");
		}
		return num + min;
	}

	public static int random(Game game, int max, String purpose) {
		return random(game, 0, max, purpose);
	}

	public static float random(Game game, String purpose) {
		return ((float) random(game, 0, 1000000, purpose)) / 1000000f;
	}

	/**
	 * Uploads an image with Nathan's address and clientID
	 */
	public static String uploadToImgur(BufferedImage image, Logger logger) {
		String address = "https://api.imgur.com/3/image";
		String clientID = "dd5a681e028223c";

		// Create HTTPClient and post
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(address);
		String all = "Something went wrong..";
		String url = "";

		try {
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
			ImageIO.write(image, "png", byteArray);
			byte[] byteImage = byteArray.toByteArray();
			String dataImage = new Base64().encodeAsString(byteImage);

			// add header
			post.addHeader("Authorization", "Client-ID " + clientID);
			// add image
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("image", dataImage));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// execute
			HttpResponse response = client.execute(post);

			// read response
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String read = rd.readLine();
			boolean first = true;

			all = read;

			// loop through response
			while (read != null) {
				if (!first)
					all = all + " : " + read;
				read = rd.readLine();
				first = false;
			}
			logger.log(LogLevel.DEBUG, "Got return from Imgur: " + all);
			url = all.split("link\":\"")[1].split("\"}")[0].replace("\\/", "/");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return url;
	}

	public static double getRotation(double ticks, boolean negative) {
		double number = (ticks <= 0) ? 1 : (System.currentTimeMillis() % (360 * ticks)) / ticks;
		return (negative) ? (0 - 1) * number : number;
	}

	/**
	 * Searches for a given file name in the library path.
	 * 
	 * @param fileName
	 *            The name of the file to find.
	 * @return The file found. Null if no file found.
	 */
	public static File findFileOnLibraryPath(final String fileName) {
		final String classpath = System.getProperty("java.library.path");
		final String pathSeparator = System.getProperty("path.separator");
		final StringTokenizer tokenizer = new StringTokenizer(classpath, pathSeparator);
		while (tokenizer.hasMoreTokens()) {
			final String pathElement = tokenizer.nextToken();
			final File directoryOrJar = new File(pathElement);
			final File absoluteDirectoryOrJar = directoryOrJar.getAbsoluteFile();
			if (absoluteDirectoryOrJar.isFile()) {
				final File target = new File(absoluteDirectoryOrJar.getParent(), fileName);
				if (target.exists())
					return target;
			} else {
				final File target = new File(directoryOrJar, fileName);
				if (target.exists())
					return target;
			}
		}
		return null;
	}

	/**
	 * Copies a file from point A to point B.
	 * 
	 * @param sourceFile
	 *            The file to copy.
	 * @param destFile
	 *            Where to copy the file.
	 * @throws Exception
	 */
	public static void copyFile(File sourceFile, File destFile) throws Exception {
		if (!destFile.exists())
			destFile.createNewFile();
		FileChannel source = null;
		FileChannel destination = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(sourceFile);
			fos = new FileOutputStream(destFile);
			source = fis.getChannel();
			destination = fos.getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null)
				source.close();
			if (destination != null)
				destination.close();
			if (fis != null)
				fis.close();
			if (fos != null)
				fos.close();
		}
	}

	/**
	 * Force copies files from point A to point B.<br>
	 * Note: Only works on Windows.
	 * 
	 * @param sourceFiles
	 *            The files to copy.
	 * @param destFiles
	 *            Where to copy the files.
	 * @throws Exception
	 */
	public static void copyFilesForce(File[] sourceFiles, File[] destFiles) throws Exception {
		String adminPermissions = System.getProperty("java.io.tmpdir") + "AdminPermissions.cmd";
		if (!new File(adminPermissions).exists())
			new File(adminPermissions).createNewFile();
		FileWriter fw = new FileWriter(adminPermissions);
		fw.write("@echo Set objShell = CreateObject(\"Shell.Application\") > %temp%\\sudo.tmp.vbs\n");
		fw.write("@echo args = Right(\"%*\", (Len(\"%*\") - Len(\"%1\"))) >> %temp%\\sudo.tmp.vbs\n");
		fw.write("@echo objShell.ShellExecute \"%1\", args, \"\", \"runas\" >> %temp%\\sudo.tmp.vbs\n");
		fw.write("@cscript %temp%\\sudo.tmp.vbs");
		fw.close();
		// System.out.println("Wrote file: " + adminPermissions);
		String copy = System.getProperty("java.io.tmpdir") + "CopyFile.bat";
		if (!new File(copy).exists())
			new File(copy).createNewFile();
		FileWriter fw2 = new FileWriter(copy);
		for (int i = 0; i < sourceFiles.length; i++) {
			fw2.write("copy /y \"" + sourceFiles[i].getAbsolutePath() + "\" \"" + destFiles[i].getAbsolutePath()
					+ "\"\n");
		}
		fw2.close();
		// System.out.println("Wrote file: " + copy);
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(adminPermissions + " " + copy);
		pr.waitFor();
	}

	/**
	 * Force copies a file from point A to point B.<br>
	 * Note: Only works on Windows.
	 * 
	 * @param sourceFile
	 *            The file to copy.
	 * @param destFile
	 *            Where to copy the file.
	 * @throws Exception
	 */
	public static void copyFileForce(File sourceFile, File destFile) throws Exception {
		copyFilesForce(new File[] { sourceFile }, new File[] { destFile });
	}

	/**
	 * Gets the text width of a given String based on current <b>Graphics</b>
	 * settings.
	 * 
	 * @param g
	 *            {@link Graphics} that includes the font and metrics <b>text</b>
	 *            would be displayed in.
	 * @param text
	 *            The text that would be displayed.
	 * @return Width <b>text</b> would be when displayed.
	 */
	public static int getTextWidth(Graphics g, String text) {
		return getTextWidth(g, g.getFont(), text);
	}

	/**
	 * Gets the text width of a given String based on current <b>Font</b> settings.
	 * 
	 * @param g
	 *            {@link Graphics}, disregarding it's own information, merely used
	 *            to view <b>font</b>.
	 * @param font
	 *            The {@link Font} that would be used to display some text.
	 * @param text
	 *            The text that would be displayed.
	 * @return Width <b>text</b> would be when displayed.
	 */
	public static int getTextWidth(Graphics g, Font font, String text) {
		FontMetrics fm = g.getFontMetrics(font);
		if (fm != null && text != null)
			return fm.stringWidth(text);
		else
			return -1;
	}

	/**
	 * Gets the text height of a any String based on current <b>Graphics</b>
	 * settings.
	 * 
	 * @param g
	 *            {@link Graphics} that includes the font and metrics some text
	 *            would be displayed in.
	 * @param text
	 *            The text that would be displayed.
	 * @return Height of any text that would be displayed by <b>g</b>.
	 */
	public static int getTextHeight(Graphics g, String text) {
		return getTextHeight(g, g.getFont(), text);
	}

	/**
	 * Gets the text height of a any String based on current <b>Font</b> settings.
	 * 
	 * @param g
	 *            {@link Graphics}, disregarding it's own information, merely used
	 *            to view <b>font</b>.
	 * @param font
	 *            The {@link Font} that would be used to display some text.
	 * @param text
	 *            The text that would be displayed.
	 * @return Height of the text that would be displayed in the {@link Font}
	 *         <b>font</b>.
	 */
	public static int getTextHeight(Graphics g, Font font, String text) {
		FontMetrics fm = g.getFontMetrics(font);
		if (fm != null)
			return fm.getHeight();
		// return -fm.getAscent() / 2;
		else
			return -1;
	}

	/**
	 * @param one
	 *            The first image
	 * @param two
	 *            The second image
	 * @return Whether or not the images are equal.
	 */
	public static boolean imageEquals(BufferedImage one, BufferedImage two) {
		try {
			DataBuffer dbActual = one.getRaster().getDataBuffer();
			DataBuffer dbExpected = two.getRaster().getDataBuffer();
			DataBufferInt actualDBAsDBInt = (DataBufferInt) dbActual;
			DataBufferInt expectedDBAsDBInt = (DataBufferInt) dbExpected;
			for (int bank = 0; bank < actualDBAsDBInt.getNumBanks(); bank++) {
				int[] actual = actualDBAsDBInt.getData(bank);
				int[] expected = expectedDBAsDBInt.getData(bank);
				if (!(Arrays.equals(actual, expected))) {
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a resource as an {@link InputStream}. This method just calls
	 * {@link ResourceLoader#getResourceAsStream(String)} from slick.util libraries.
	 * 
	 * @param location
	 *            Location of the resource we want to get the stream of.
	 * @return An {@link InputStream} of the resource.
	 */
	public static InputStream getResourceAsStream(String location) {
		return ResourceLoader.getResourceAsStream(location);
	}

	public static void writeExceptionToFile(Exception e, String fileName) {
		try {
			FileWriter fstream = new FileWriter(fileName, true);
			BufferedWriter out = new BufferedWriter(fstream);
			PrintWriter pWriter = new PrintWriter(out, true);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aa");
			pWriter.append("\n" + dateFormat.format(new Date(System.currentTimeMillis())) + "\n");
			e.printStackTrace(pWriter);
			fstream.flush();
			fstream.close();
			try {
				out.flush();
				out.close();
			} catch (Exception ie) {
			}
			pWriter.flush();
			pWriter.close();
		} catch (Exception ie) {
			ie.printStackTrace();
			displayDialog("Couldn't write Exception to file!");
			throw new RuntimeException("Could not write Exception to file", ie);
		}
	}

	public static void writeToFile(String fileName, String... write) {
		try {
			FileWriter fstream = new FileWriter(fileName, true);
			BufferedWriter out = new BufferedWriter(fstream);
			for (int i = 0; i < write.length; i++)
				out.write("\n" + write[i]);
			fstream.flush();
			fstream.close();
			try {
				out.flush();
				out.close();
			} catch (Exception e) {
			}
		} catch (Exception e) {
			e.printStackTrace();
			displayDialog("Couldn't write to file!");
			throw new RuntimeException("Could not write to file", e);
		}
	}

	/**
	 * Sleeps for specific time, ignoring any thrown exceptions.
	 * 
	 * @param millis
	 *            Time (in millis) to sleep.
	 */
	public static void sleep(long millis) {
		long sleptFor = 0;
		long timeBefore = System.currentTimeMillis();
		while (sleptFor < millis) {
			try {
				Thread.sleep(1);
			} catch (Exception e) {
			}
			sleptFor += System.currentTimeMillis() - timeBefore;
			timeBefore = System.currentTimeMillis();
		}
	}

	/**
	 * Rounds a number to a certain amount of decimal places.
	 * 
	 * @param value
	 *            The number to round.
	 * @param places
	 *            The amount of decimal places to round to.
	 * @return The rounded number.
	 */
	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	/**
	 * Grabs a resource, reads it, and returns the result in an Array line-by-line.
	 * 
	 * @param resource
	 *            The directory and filename of the resource.
	 * @return An array of the read file.
	 * @throws FileNotFoundException
	 */
	public static ArrayList<String> readResourceFile(String resource) throws FileNotFoundException {
		ArrayList<String> result = new ArrayList<String>();
		ClassLoader classLoader = Tools.class.getClassLoader();
		File file = new File(classLoader.getResource(resource).getFile());

		Scanner scanner = new Scanner(file);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			result.add(line);
		}
		scanner.close();
		return result;
	}
}

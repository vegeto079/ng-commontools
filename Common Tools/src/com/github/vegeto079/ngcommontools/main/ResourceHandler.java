package com.github.vegeto079.ngcommontools.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import org.newdawn.slick.util.ResourceLoader;

import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

/**
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: No longer throws an error when we don't have a /res/ folder,
 *          rather it assumes that we ARE running from a JAR file, as you don't
 *          want this to be messed up for a release version. Can manually edit
 *          for debugging purposes.
 * @version 1.02: Added an exception because of resources not existing in
 *          {@link #extractAll(Logger, String)} for the files not being found.
 * @version 1.03: resExists no longer requires a font folder to exist. Added
 *          {@link #resAlreadyExists()}.
 * @version 1.04: We now use System.getProperty("file.separator") instead of
 *          just a hard-coded "/".
 * @version 1.05: Fixed some cases where file.separator doesn't work right.
 * @version 1.06: Added {@link #deleteEmptyResFolders(Logger, File...)}.
 */
public class ResourceHandler {

	private static boolean debug = false;
	private static boolean debug2 = false;

	public static void extractAll(Logger logger, String jarName) {
		boolean resExists = new File("res").exists();
		if (resExists) {
			File exists = new File("res" + System.getProperty("file.separator") + "");
			if (!exists.exists())
				try {
					exists.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		logger.log(LogLevel.DEBUG, "Extracting from jar: " + jarName + ". Res already exists: " + resExists);
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jarName);
			String dir = getDir(logger);
			for (Enumeration<JarEntry> aenum = jarFile.entries(); aenum.hasMoreElements();) {
				JarEntry entry = aenum.nextElement();
				String name = entry.getName();
				String[] nameSplit = name.split(System.getProperty("file.separator").replaceAll("\\\\", "/"));
				if (nameSplit[0].equals("res")) {
					if (nameSplit[nameSplit.length - 1].contains(".")) {
						if (debug)
							logger.log(LogLevel.DEBUG, "Resource found: " + name);
						String temp = "";
						for (int i = 0; i < nameSplit.length - 1; i++)
							temp += nameSplit[i] + System.getProperty("file.separator");
						File f = new File(dir + temp);
						if (!f.exists()) {
							f.mkdir();
							if (!resExists)
								f.deleteOnExit();
						}
						extract(logger, jarFile.getName(), name);
						File f2 = new File(dir + name);
						if (!resExists)
							f2.deleteOnExit();
					} else {
						if (debug)
							logger.log(LogLevel.DEBUG, "Resource folder found: " + name);
						File f = new File(dir + name);
						f.mkdir();
						if (!resExists)
							f.deleteOnExit();
					}
				}
			}
			if (debug)
				logger.log(LogLevel.DEBUG, "All resources extracted.");
		} catch (FileNotFoundException fnfe) {
			logger.log(LogLevel.DEBUG, "File or folder of resources do not exist.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			jarFile.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	private static void extract(Logger logger, String jarName, String entryName) throws Exception {
		JarFile jar = new JarFile(jarName);
		if (debug)
			logger.log(LogLevel.DEBUG, jarName + " opened.");
		try {
			JarEntry entry = jar.getJarEntry(entryName);
			if (entry != null) {
				InputStream entryStream = jar.getInputStream(entry);
				try {
					String[] nameSplit = entryName.split(System.getProperty("file.separator").replaceAll("\\\\", "/"));
					String temp = "";
					for (int i = 0; i < nameSplit.length - 1; i++)
						temp += nameSplit[i] + System.getProperty("file.separator");
					File f = new File(getDir(logger) + temp);
					f.mkdir();
					FileOutputStream file = new FileOutputStream(entry.getName());
					try {
						byte[] buffer = new byte[1024];
						int bytesRead;
						while ((bytesRead = entryStream.read(buffer)) != -1) {
							file.write(buffer, 0, bytesRead);
						}
						if (debug)
							logger.log(LogLevel.DEBUG, entry.getName() + " extracted.");
					} finally {
						file.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.err(LogLevel.ERROR, "Error occurred when trying to extract files.",
							"Perhaps you're running from a read-only location?",
							"This file needs writing permissions to run.");
					Tools.displayDialog("Error occurred when trying to extract files.",
							"Perhaps you're running from a read-only location?",
							"This file needs writing permissions to run.");
					System.exit(0);
				} finally {
					entryStream.close();
				}
			} else {
				if (debug)
					logger.log(LogLevel.DEBUG, entryName + " not found.");
			}
		} finally {
			jar.close();
			if (debug)
				logger.log(LogLevel.DEBUG, jarName + " closed.");
		}
	}

	public static String getDir(Logger logger) {
		String res = ResourceLoader.getResource("").toString();
		String newRes = "";
		String[] resSplit = res.split(System.getProperty("file.separator").replaceAll("\\\\", "/"));
		for (int i = 1; i < resSplit.length; i++) {
			newRes += resSplit[i] + System.getProperty("file.separator");
		}
		newRes = newRes.replace("%20", " ");
		if (debug)
			logger.log(LogLevel.DEBUG, "getDir(): " + newRes);
		return newRes;
	}

	public static String get(Logger logger, String resourceName, String jarName) {
		if (debug2)
			logger.log(LogLevel.DEBUG, "getResource(" + resourceName + "," + jarName + ") called.");
		String res = ResourceLoader.getResource("res" + System.getProperty("file.separator") + resourceName).toString();
		String[] resSplit = res.split(System.getProperty("file.separator").replaceAll("\\\\", "/"));
		String newRes = resSplit[1];
		for (int i = 2; i < resSplit.length; i++)
			if (!resSplit[i].equals("."))
				newRes += System.getProperty("file.separator") + resSplit[i];
		if (runningFromJar()) {
			res = res.replace("%20", " ");
			if (debug2)
				logger.log(LogLevel.DEBUG, "We ARE running from a jar.");
			if (debug2)
				logger.log(LogLevel.DEBUG, "res: " + res);
			newRes = res.replace(System.getProperty("file.separator").replaceAll("\\\\", "/") + jarName + "!", "")
					.replace("jar:file:" + System.getProperty("file.separator").replaceAll("\\\\", "/"), "");
			if (debug2)
				logger.log(LogLevel.DEBUG, "newRes: " + res);
		} else {
			if (debug2)
				logger.log(LogLevel.DEBUG, "We are NOT running from a jar.");
		}
		newRes = newRes.replace("%20", " ");
		if (debug2)
			logger.log(LogLevel.DEBUG, "got newRes: " + newRes);
		return newRes;
	}

	public static boolean runningFromJar() {
		try {
			return ResourceLoader.getResource("res").toString().contains(".jar!");
		} catch (Exception RuntimeException) {
			return true;
		}
	}

	public static boolean resAlreadyExists() {
		return new File("res").exists();
	}

	public static void deleteEmptyResFolders(Logger logger, File... file) {
		String mainPath = null;
		if (file == null || file.length == 0) {
			mainPath = ResourceHandler.get(logger, "", Tools.getJarName());
			if (System.getProperty("os.name").contains("Linux")) {
				if (debug)
					logger.log(LogLevel.DEBUG, "On linux, adjusting path accordingly.");
				mainPath = "/" + mainPath;
			}
		} else {
			mainPath = file[0].getAbsolutePath();
		}
		File folder = new File(mainPath);
		File[] list = folder.listFiles();
		if (list == null)
			return;
		for (File f : list) {
			if (f.isDirectory()) {
				File[] folderContents = f.listFiles();
				if (folderContents == null || folderContents.length == 0)
					f.delete();
				else
					deleteEmptyResFolders(logger, f);
			}
		}
	}

	public static BufferedImage findAndLoadImage(Logger logger, String fileName) {
		System.gc();
		try {
			String mainPath = ResourceHandler.get(logger, "pictures", Tools.getJarName());
			if (System.getProperty("os.name").contains("Linux")) {
				mainPath = "/" + mainPath;
			}
			File foundFile = findFilesAndFolders(new File(mainPath), fileName);
			if (foundFile == null) {
			} else {
				BufferedImage image = getImage(foundFile.getAbsolutePath());
				return image;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static BufferedImage getImage(String imageName) throws IOException {
		File file = new File(imageName);
		BufferedImage image = ImageIO.read(file);
		if (file.exists() && ResourceHandler.runningFromJar() && !ResourceHandler.resAlreadyExists())
			file.delete();
		return image;
	}

	public static File findFile(Logger logger, String fileName) {
		System.gc();
		try {
			String mainPath = ResourceHandler.get(logger, "config", Tools.getJarName());
			if (System.getProperty("os.name").contains("Linux")) {
				logger.log(LogLevel.DEBUG, "On linux, adjusting path accordingly.");
				mainPath = "/" + mainPath;
			}
			File foundFile = findFilesAndFolders(new File(mainPath), fileName);
			if (foundFile == null) {
				logger.err(LogLevel.DEBUG, "We couldn't find the file: " + fileName);
			} else {
				return foundFile;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.err(LogLevel.DEBUG, "We couldn't find the file, returning null: " + fileName);
		return null;
	}

	public static File findFilesAndFolders(File folder, String fileToFind) {
		File[] list = folder.listFiles();
		File foundFile = null;
		for (int i = 0; i < list.length; i++) {
			if (list[i].isDirectory()) {
				foundFile = findFilesAndFolders(list[i], fileToFind);
				if (foundFile != null)
					return foundFile;
			} else {
				String fileName = list[i].getName().split("\\.")[0].replace("-", " ");
				if (fileName.equalsIgnoreCase(fileToFind.replace("-", " ")))
					return list[i];
			}
		}
		return null;
	}
}

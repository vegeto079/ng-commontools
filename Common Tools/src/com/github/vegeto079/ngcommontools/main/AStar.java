package com.github.vegeto079.ngcommontools.main;

import java.awt.Point;
import java.util.ArrayList;

import com.github.vegeto079.ngcommontools.main.Logger.LogLevel;

/**
 * 
 * @author Nathan
 * @version 0.1: Started tracking version.
 * @version 0.11: Added {@link #allowWalkingOnCurrentTile}.
 * @version 0.12: Added {@link #maxAttempts}.
 */
public class AStar {
	public static class Settings {
		/**
		 * The maximum value that will be registered as a moving tile; anything
		 * above this value will be considered unreachable by any means.
		 */
		int maxCost = Integer.MAX_VALUE;
		/**
		 * If true, we will return an incomplete path if we cannot find a whole
		 * one, rather than returning a null path.
		 */
		boolean returnIncompletePath = false;
		/**
		 * If true, instead of returning the A Star path from start to finish,
		 * we will return a path that contains all tiles that we touched in
		 * finding the A Star path.
		 */
		boolean returnTouchedPath = false;
		/**
		 * If true, A* will not perform as expected and instead will quit
		 * looking for a path once it has found one - regardless of it's
		 * inefficiency.
		 */
		boolean takeFirstPathFound = false;
		/**
		 * If true, we cannot go through a square that 'cornered off', whereas a
		 * normal lookup with A* would see that square like any other.
		 */
		boolean cantGoThroughCorners = true;
		/**
		 * If true, if the tile we start on is considered unwalkable due to
		 * {@link #maxCost}, the tile will be set under <b>maxCost</b> and
		 * considered walkable (at a very high cost, just <b>1</b> under
		 * <b>maxCost</b> - will have to walk on it anyway).
		 */
		boolean allowWalkingOnCurrentTile = false;
		/**
		 * The added G cost of any tile that is diagonal. 14 is the 'correct'
		 * answer in that it accurately represents near-actual walking distance.
		 * If this is above {@link #maxCost} then diagonal moves will be
		 * ignored.
		 */
		float diagonalCost = 14;
		/**
		 * The added G cost of any tile that is directly beside us. 10 is the
		 * default value. If this is above {@link #maxCost} then adjacent moves
		 * will be ignored.
		 */
		float directCost = 10;
		/**
		 * The amount of time slept in between every tile iteration. This can be
		 * a non-whole number. I.E. 0.5ms means it will sleep 1ms every other
		 * tile iteration.
		 */
		float sleepTime = 0;
		/**
		 * The maximum amount of times we will loop through to try to find the
		 * answer.
		 */
		int maxAttempts = Integer.MAX_VALUE;
		Logger logger = new Logger(false);

		/**
		 * Initializes Settings with default values
		 */
		public Settings() {

		}

		/**
		 * 
		 * @param maxCost
		 * @param returnIncompletePath
		 * @param returnTouchedPath
		 * @param takeFirstPathFound
		 * @param cantGoThroughCorners
		 * @param allowWalkingThroughWallsIfAlreadyInOne
		 * @param diagonalCost
		 * @param directCost
		 * @param sleepTime
		 * @param maxAttempts
		 * @param logger
		 */
		public Settings(int maxCost, boolean returnIncompletePath, boolean returnTouchedPath,
				boolean takeFirstPathFound, boolean cantGoThroughCorners, boolean allowWalkingOnCurrentTile,
				float diagonalCost, float directCost, float sleepTime, int maxAttempts, Logger logger) {
			this.maxCost = maxCost;
			this.returnIncompletePath = returnIncompletePath;
			this.returnTouchedPath = returnTouchedPath;
			this.takeFirstPathFound = takeFirstPathFound;
			this.cantGoThroughCorners = cantGoThroughCorners;
			this.allowWalkingOnCurrentTile = allowWalkingOnCurrentTile;
			this.diagonalCost = diagonalCost;
			this.sleepTime = sleepTime;
			this.directCost = directCost;
			this.maxAttempts = maxAttempts;
			// this.logger = logger;
		}
	}

	public static Path getPath(int[][] map, Point start, Point end, float... numberWeights) {
		AStarPath path = new AStarPath(map, start, end, numberWeights);
		path.start();
		return path.get();
	}

	public static AStarPath getAStarPath(int[][] map, Point start, Point end, float... numberWeights) {
		return new AStarPath(map, start, end, numberWeights);
	}

	public static AStarPath getAStarPath(Settings settings, int[][] map, Point start, Point end,
			float... numberWeights) {
		AStarPath path = new AStarPath(map, start, end, numberWeights);
		path.setSettings(settings);
		return path;
	}

	public static class Path {
		public ArrayList<Point> tiles = new ArrayList<Point>();;

		public Path() {

		}

		public Path(ArrayList<Point> tiles) {
			this.tiles = tiles;
		}

		public void log() {
			if (tiles != null)
				for (Point p : tiles) {
					System.out.println(p);
				}
		}

		public Point getNextPathPoint(Point currentLocation) {
			if (tiles.size() == 0)
				return null;
			int next = 0;
			for (int i = 0; i < tiles.size(); i++)
				if (currentLocation.distance(tiles.get(i)) == 0 && tiles.size() > 1)
					return tiles.get(i + 1);
				else if (tiles.get(i).distance(currentLocation) < tiles.get(next).distance(currentLocation))
					next = i;
			if (next == 0)
				return tiles.get(next);
			else
				return tiles.get(next + 1);
		}

		public Point getFinalPathPoint() {
			if (tiles.size() == 0)
				return null;
			return tiles.get(tiles.size() - 1);
		}
	}

	public static class AStarPath extends Path {
		public int[][] map = null;
		public Point start = null;
		public Point end = null;
		public ArrayList<Tile> open = new ArrayList<Tile>();
		public ArrayList<Tile> closed = new ArrayList<Tile>();
		public ArrayList<Float> numbers = new ArrayList<Float>();
		public ArrayList<Float> weights = new ArrayList<Float>();
		public Settings settings = new Settings();
		public boolean pathFound = false;
		/**
		 * This variable has different values depending on the state of
		 * path-finding.<br>
		 * <b>Before finding path</b>: -1.<br>
		 * <b>Currently finding path</b>: Time path-finding was started.<br>
		 * <b>Finished finding path</b>: The amount of time it took to get the
		 * path.
		 */
		long time = -1;
		/**
		 * The amount of time we need to sleep. Adds up every tile iteration,
		 * until above 1, where it will sleep for this duration.
		 */
		float needToSleep = 0;

		public AStarPath(int[][] map, Point start, Point end, float... numberWeights) {
			settings.logger.log(LogLevel.DEBUG, "AStarPath(map," + start + "," + end + ")");
			this.map = map;
			logMap();
			this.start = start;
			this.end = end;
			for (int i = 0; i < numberWeights.length; i += 2) {
				numbers.add(numberWeights[i]);
				weights.add(numberWeights[i + 1]);
			}
		}

		public void logMap() {
			for (int i = 0; i < map[0].length; i++) {
				String mapLine = "logMap [" + i + "]{";
				for (int j = 0; j < map.length; j++) {
					if (j == 0)
						mapLine += "" + map[j][i];
					else
						mapLine += ", " + map[j][i];
				}
				// settings.logger.log(LogLevel.NORMAL, mapLine + "}");
				settings.logger.log(LogLevel.DEBUG, mapLine + "}");
			}
		}

		public void setSettings(Settings settings) {
			this.settings = settings;
		}

		public long getTime() {
			return time;
		}

		/**
		 * Starts the path finding process. This method is not threaded:
		 * whichever thread called this method will be stuck until this is
		 * finished.
		 */
		public void start() {
			time = System.currentTimeMillis();
			START();
			time = System.currentTimeMillis() - time;
		}

		private void START() {
			settings.logger.log(LogLevel.DEBUG, "trying to get to end: " + end);
			Tile tile = new Tile(start, null, 0);
			open.add(tile);
			Tile foundEnd = null;
			int attempts = settings.maxAttempts;
			Tile bestTile = null;
			while (foundEnd == null && attempts > 0) {
				if (needToSleep > 1) {
					try {
						long sleepAmt = (long) Math.floor(needToSleep);
						needToSleep -= sleepAmt;
						Thread.sleep(sleepAmt);
					} catch (Exception e) {
					}
				}
				needToSleep += settings.sleepTime;
				attempts--;
				settings.logger.log(LogLevel.DEBUG, "looping again... (" + (settings.maxAttempts - attempts) + ")");
				if (attempts < 1) {
					settings.logger.log(LogLevel.DEBUG, "We failed, took over " + attempts + " attempts.");
					break;
				}
				for (int i = 0; i < closed.size(); i++)
					if (closed.get(i).point.distance(end) == 0)
						foundEnd = closed.get(i);
				if (foundEnd != null) {
					break;
				} else {
					if (open.size() == 0) {
						settings.logger.log(LogLevel.DEBUG,
								"We failed, no open tiles. (attempts: " + (Integer.MAX_VALUE - attempts) + ")");
						if (settings.returnIncompletePath) {
							settings.logger.log(LogLevel.DEBUG, "Returning incomplete path.");
							Tile oldTile = tile;
							tiles = new ArrayList<Point>();
							while (oldTile.hasParent()) {
								// System.out
								// .println("Adding point: " + oldTile.point);
								tiles.add(oldTile.point);
								oldTile = oldTile.parent;
							}
							tiles.add(start);
							tiles = reversePoints(tiles);
						}
						break;
					}
					Tile oldTile = tile;
					tile = new Tile(null, null, 99999);
					float tileF = 99999;
					if (bestTile != null) {
						tile = bestTile;
						bestTile = null;
					} else
						for (int i = 0; i < open.size(); i++) {
							float newF = open.get(i).getF();
							if (newF < tileF) {
								tile = open.get(i);
								tileF = newF;
							}
						}
					if (tile.point == null) {
						settings.logger.log(LogLevel.DEBUG, "Something went wrong.. couldn't get an open tile!");
						if (settings.returnIncompletePath) {
							settings.logger.log(LogLevel.DEBUG, "Returning incomplete path.");
							settings.logger.log(LogLevel.DEBUG, "");
							tiles = new ArrayList<Point>();
							while (oldTile.hasParent()) {
								settings.logger.log(LogLevel.DEBUG, "Adding point: " + oldTile.point);
								tiles.add(oldTile.point);
								oldTile = oldTile.parent;
							}
							tiles.add(start);
							tiles = reversePoints(tiles);
						}
						return;
					}
					open.remove(tile);
					closed.add(tile);
					Point[] neighbors = new Point[8];
					float[] cost = new float[] { settings.diagonalCost, settings.directCost, settings.diagonalCost,
							settings.directCost, settings.directCost, settings.diagonalCost, settings.directCost,
							settings.diagonalCost };
					neighbors[0] = new Point(tile.point.x - 1, tile.point.y - 1);
					neighbors[1] = new Point(tile.point.x - 1, tile.point.y);
					neighbors[2] = new Point(tile.point.x - 1, tile.point.y + 1);
					neighbors[3] = new Point(tile.point.x, tile.point.y - 1);
					neighbors[4] = new Point(tile.point.x, tile.point.y + 1);
					neighbors[5] = new Point(tile.point.x + 1, tile.point.y - 1);
					neighbors[6] = new Point(tile.point.x + 1, tile.point.y);
					neighbors[7] = new Point(tile.point.x + 1, tile.point.y + 1);
					if (settings.directCost >= settings.maxCost) {
						neighbors[1] = null;
						neighbors[3] = null;
						neighbors[4] = null;
						neighbors[6] = null;
					}
					if (settings.diagonalCost >= settings.maxCost) {
						neighbors[0] = null;
						neighbors[2] = null;
						neighbors[5] = null;
						neighbors[7] = null;
					}
					for (int i = 0; i < neighbors.length; i++) {
						if (neighbors[i] == null)
							continue;
						Point p = neighbors[i];
						float weight = 1f;
						if (p.x < 0 || p.y < 0 || p.x >= map.length || p.y >= map[0].length) {
							// If the point is off the map, ignore it
							continue;
						}
						for (int j = 0; j < numbers.size(); j++)
							if (numbers.get(j) == map[p.x][p.y]) {
								// If the point's number is in the weighted
								// numbers list, add weight to it's cost
								// settings.logger.log(LogLevel.DEBUG,
								// "Found a cost! Adding weight: "
								// + weights.get(j));
								weight = weights.get(j);
							}
						settings.logger.log(LogLevel.DEBUG, "early cost: " + cost[i] + ", weight: " + weight);
						cost[i] = getCost(cost[i], map[p.x][p.y], weight);
						settings.logger.log(LogLevel.DEBUG, "adjusted cost: " + cost[i]);
						if (settings.allowWalkingOnCurrentTile && neighbors[i].distance(start) == 0) {
							settings.logger.log(LogLevel.DEBUG, "Found starting tile! Making sure it's walkable.");
							cost[i] = 0;
						}
						settings.logger.log(LogLevel.DEBUG,
								"Got cost for neighbor[" + i + "] (" + neighbors[i] + "): " + cost[i]);
					}
					if (settings.cantGoThroughCorners) {
						if (cost[1] >= settings.maxCost && cost[3] >= settings.maxCost)
							neighbors[0] = null;
						if (cost[6] >= settings.maxCost && cost[4] >= settings.maxCost)
							neighbors[7] = null;
						if (cost[1] >= settings.maxCost && cost[4] >= settings.maxCost)
							neighbors[2] = null;
						if (cost[6] >= settings.maxCost && cost[3] >= settings.maxCost)
							neighbors[5] = null;
					}
					for (int i = 0; i < neighbors.length; i++) {
						settings.logger.log(LogLevel.DEBUG, "Checking neighbor[" + i + "] (" + neighbors[i] + ")");
						if (neighbors[i] == null)
							continue;
						Point p = neighbors[i];
						boolean closedAlready = false;
						for (int j = 0; j < closed.size(); j++)
							if (closed.get(j).point.distance(p) == 0) {
								settings.logger.log(LogLevel.DEBUG, "Ignore this tile, it's closed already.");
								// If this tile is closed, ignore it
								closedAlready = true;
							}
						if (closedAlready)
							continue;
						if (p.x < 0 || p.y < 0 || p.x >= map.length || p.y >= map[0].length) {
							settings.logger.log(LogLevel.DEBUG, "Point is off map, ignoring it");
							// If the point is off the map, ignore it
							continue;
						}
						if (cost[i] > settings.maxCost) {
							settings.logger.log(LogLevel.DEBUG, "Cost is too high! (" + cost[i] + ")");
							continue;
						}
						settings.logger.log(LogLevel.DEBUG, "Cost is good");
						Tile neighborTile = new Tile(p, tile, cost[i]);
						float neighborTileG = getG(neighborTile);
						for (int j = 0; j < open.size(); j++)
							if (open.get(j).point.distance(p) == 0) {
								settings.logger.log(LogLevel.DEBUG, "Tile is already open");
								// If this tile is already open
								if (neighborTileG < getG(open.get(j))) {
									// If this G is smaller than the previous
									// tile's G, replace it as this is a better
									// path to get to that tile
									Tile replaceTile = open.get(j);
									replaceTile.parent = neighborTile;
									open.remove(j);
									open.add(replaceTile);
									settings.logger.log(LogLevel.DEBUG, "Found better path for this tile");
								}
								if (p.distance(end) == 0)
									pathFound = true;
								break;
							}
						if (neighborTileG <= settings.maxCost) {
							settings.logger.log(LogLevel.DEBUG, "Adding tile to open");
							// && (!pathFound || neighborTile.h < tile.h)) {
							open.add(neighborTile);
							if (settings.returnTouchedPath && !tiles.contains(neighborTile.point))
								tiles.add(neighborTile.point);
						}
						if (settings.takeFirstPathFound && (bestTile == null || bestTile.getF() > neighborTile.getF()))
							bestTile = neighborTile;
					}
				}
			}
			if (attempts > 0) {
				settings.logger.log(LogLevel.DEBUG, "Got path!");
				if (settings.returnTouchedPath) {
					settings.logger.log(LogLevel.DEBUG, "Showing all touched tiles");
					while (open.size() > 0)
						open.remove(0);
					while (foundEnd != null && foundEnd.hasParent()) {
						settings.logger.log(LogLevel.DEBUG, "Adding point: " + foundEnd);
						open.add(foundEnd);
						foundEnd = foundEnd.parent;
					}
					open.add(new Tile(start, null, 0));
					open = reverseTiles(open);
				} else {
					while (foundEnd != null && foundEnd.hasParent()) {
						settings.logger.log(LogLevel.DEBUG, "Adding point: " + foundEnd);
						tiles.add(foundEnd.point);
						foundEnd = foundEnd.parent;
					}
					tiles.add(start);
					tiles = reversePoints(tiles);
					settings.logger.log(LogLevel.DEBUG,
							"start (" + tiles.get(0) + ") end (" + tiles.get(tiles.size() - 1) + ")");
				}
			}
			pathFound = true;
		}

		protected float getCost(float currentCost, int mapOriginalCost, float weight) {
			float cost = currentCost * weight;
			if (cost >= settings.maxCost || cost < -1000000) // overflow
				cost = settings.maxCost + 1;
			return cost;
		}

		private static ArrayList<Point> reversePoints(ArrayList<Point> list) {
			ArrayList<Point> newList = new ArrayList<Point>();
			while (list.size() > 0) {
				newList.add(list.get(list.size() - 1));
				list.remove(list.size() - 1);
			}
			return newList;
		}

		private static ArrayList<Tile> reverseTiles(ArrayList<Tile> list) {
			ArrayList<Tile> newList = new ArrayList<Tile>();
			while (list.size() > 0) {
				newList.add(list.get(list.size() - 1));
				list.remove(list.size() - 1);
			}
			return newList;
		}

		protected float getG(Tile tile) {
			float amt = tile.g;
			while (tile.hasParent()) {
				tile = tile.parent;
				amt += tile.g;
			}
			return amt;
		}

		public Path get() {
			return new Path(tiles);
		}

		public class Tile {
			public Point point = null;
			public Tile parent = null;
			public float g = 99999;
			public float h = 99999;

			public Tile(Point point, Tile parent, float g) {
				this.point = point;
				this.parent = parent;
				this.g = g;
				if (point != null)
					h = Math.abs(end.x - point.x) + Math.abs(end.y - point.y);
			}

			public boolean hasParent() {
				return parent != null;
			}

			public float getF() {
				return getG(this) + h;
			}

			public float getF(float g) {
				return g + h;
			}

			public String toString() {
				return "point (" + point + ") g (" + g + ") h (" + h + ") f (" + getF() + ")";
			}

			public void log() {
				settings.logger.log(LogLevel.DEBUG, toString());
			}
		}
	}
}

package com.github.vegeto079.ngcommontools.main;

import java.awt.Point;
import java.text.DecimalFormat;

/**
 * An easy-to-use {@link Point}-like class, except handled simply with
 * {@link Double}s.
 * 
 * @author Nathan
 * @version 1.0: Started tracking version.
 * @version 1.01: Added {@link #adjust(double, double)}.
 * @version 1.02: Added {@link #DoublePoint(DoublePoint)}.
 */
public class DoublePoint {
	public double x, y;

	/**
	 * Creates this {@link DoublePoint} using given points.
	 * 
	 * @param x
	 *            Sets {@link #x}
	 * @param y
	 *            Sets {@link #y}
	 */
	public DoublePoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Creates this {@link DoublePoint} using <b>p</b>.
	 * 
	 * @param p
	 *            The {@link Point} to derive {@link #x} and {@link #y} from.
	 */
	public DoublePoint(Point p) {
		this.x = p.getX();
		this.y = p.getY();
	}

	/**
	 * Creates this {@link DoublePoint} using <b>dp</b>, without association to.
	 * 
	 * @param p
	 *            The {@link DoublePoint} to derive {@link #x} and {@link #y}
	 *            from.
	 */
	public DoublePoint(DoublePoint dp) {
		this.x = dp.x;
		this.y = dp.y;
	}

	/**
	 * Determines the distance between this {@link DoublePoint} and <b>p</b>.
	 * 
	 * @param p
	 *            The {@link DoublePoint} to determine distance to.
	 * @return Our distance, as a {@link Double}.
	 */
	public double distance(DoublePoint p) {
		return Point.distance(x, y, p.x, p.y);
	}

	/**
	 * Determines the distance between this {@link DoublePoint} and <b>p</b>.
	 * 
	 * @param p
	 *            The {@link Point} to determine distance to.
	 * @return Our distance, as a {@link Double}.
	 */
	public double distance(Point p) {
		return Point.distance(x, y, p.getX(), p.getY());
	}

	/**
	 * Increments {@link #x} and {@link #y} by <b>x</b> and <b>y</b>. Can leave
	 * either at <b>0</b> to avoid affecting it.
	 * 
	 * @param x
	 *            Value to add to {@link #x}.
	 * @param y
	 *            Value to add to {@link #y}.
	 */
	public void adjust(double x, double y) {
		this.x += x;
		this.y += y;
	}

	/**
	 * Converts this {@link #DoublePoint} into a {@link Point}.
	 * 
	 * @return A new {@link Point} with our {@link #x} and {@link #y} values.
	 */
	public Point toPoint() {
		return new Point((int) x, (int) y);
	}

	public String toString() {
		return "DoublePoint[" + x + "," + y + "]";
	}

	/**
	 * Rounds {@link #x} and {@link #y} to <b>digits</b> decimal points.
	 * 
	 * @param digits
	 */
	public void round(int digits) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(digits);
		x = Double.parseDouble(df.format(x));
		y = Double.parseDouble(df.format(y));
	}
}

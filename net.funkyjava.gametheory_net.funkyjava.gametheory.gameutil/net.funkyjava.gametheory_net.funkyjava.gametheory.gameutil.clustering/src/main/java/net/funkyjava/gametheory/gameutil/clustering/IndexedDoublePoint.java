package net.funkyjava.gametheory.gameutil.clustering;

import org.apache.commons.math3.ml.clustering.DoublePoint;

public class IndexedDoublePoint extends DoublePoint {

	private final int index;

	public IndexedDoublePoint(final double[] point, final int index) {
		super(point);
		this.index = index;
	}

	public IndexedDoublePoint(int[] point, final int index) {
		super(point);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

}

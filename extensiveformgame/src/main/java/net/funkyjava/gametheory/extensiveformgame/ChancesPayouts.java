package net.funkyjava.gametheory.extensiveformgame;

public interface ChancesPayouts {

	// Implementation should be thread safe
	double[] getPayouts(final int[][] chances);
}

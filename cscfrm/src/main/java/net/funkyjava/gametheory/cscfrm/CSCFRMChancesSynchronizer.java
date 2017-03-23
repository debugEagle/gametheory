package net.funkyjava.gametheory.cscfrm;

import java.util.List;

public interface CSCFRMChancesSynchronizer {

	int[][] getChances() throws InterruptedException;

	void endUsing(final int[][] usedChances) throws InterruptedException;

	void stop();

	void reset();

	List<Runnable> getProducers();
}

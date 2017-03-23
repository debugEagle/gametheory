package net.funkyjava.gametheory.cscfrm;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import com.google.common.util.concurrent.Monitor;

public class CSCFRMMutexChancesSynchronizer implements CSCFRMChancesSynchronizer {

	private final CSCFRMChancesProducer producer;
	private final Monitor monitor = new Monitor();
	private final int nbRounds;
	private final BitSet[][] inUseBits;

	public CSCFRMMutexChancesSynchronizer(final CSCFRMChancesProducer producer, final int[][] chancesSizes) {
		this.producer = producer;
		final int nbRounds = this.nbRounds = chancesSizes.length;
		this.inUseBits = new BitSet[nbRounds][];
		for (int i = 0; i < nbRounds; i++) {
			final int[] playersChances = chancesSizes[i];
			final int nbPlayers = playersChances.length;
			final BitSet[] roundBits = inUseBits[i] = new BitSet[nbPlayers];
			for (int j = 0; j < nbPlayers; j++) {
				roundBits[j] = new BitSet(playersChances[j]);
			}
		}
	}

	@Override
	public int[][] getChances() throws InterruptedException {
		final Monitor monitor = this.monitor;
		final CSCFRMChancesProducer producer = this.producer;
		monitor.enter();
		try {
			while (true) {
				final int[][] chances = producer.produceChances();
				if (hasCollision(chances)) {
					producer.endedUsing(chances);
					continue;
				}
				reserve(chances);
				return chances;
			}
		} finally {
			monitor.leave();
		}
	}

	@Override
	public void endUsing(int[][] usedChances) throws InterruptedException {
		final Monitor monitor = this.monitor;
		final CSCFRMChancesProducer producer = this.producer;
		monitor.enter();
		try {
			endReserving(usedChances);
			producer.endedUsing(usedChances);
		} finally {
			monitor.leave();
		}
	}

	private final void reserve(final int[][] usedChances) {
		final int nbRounds = this.nbRounds;
		final BitSet[][] inUseBits = this.inUseBits;
		for (int i = 0; i < nbRounds; i++) {
			final BitSet[] roundBits = inUseBits[i];
			final int[] roundChances = usedChances[i];
			final int nbPlayers = roundChances.length;
			for (int j = 0; j < nbPlayers; j++) {
				roundBits[j].set(roundChances[j]);
			}
		}
	}

	private final void endReserving(final int[][] usedChances) {
		final int nbRounds = this.nbRounds;
		final BitSet[][] inUseBits = this.inUseBits;
		for (int i = 0; i < nbRounds; i++) {
			final BitSet[] roundBits = inUseBits[i];
			final int[] roundChances = usedChances[i];
			final int nbPlayers = roundChances.length;
			for (int j = 0; j < nbPlayers; j++) {
				roundBits[j].clear(roundChances[j]);
			}
		}
	}

	private final boolean hasCollision(final int[][] chances) {
		final int nbRounds = this.nbRounds;
		final BitSet[][] inUseBits = this.inUseBits;
		for (int i = 0; i < nbRounds; i++) {
			final BitSet[] roundBits = inUseBits[i];
			final int[] roundChances = chances[i];
			final int nbPlayers = roundChances.length;
			for (int j = 0; j < nbPlayers; j++) {
				if (roundBits[j].get(roundChances[j])) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void stop() {

	}

	@Override
	public void reset() {
	}

	@Override
	public List<Runnable> getProducers() {
		return Collections.emptyList();
	}

}

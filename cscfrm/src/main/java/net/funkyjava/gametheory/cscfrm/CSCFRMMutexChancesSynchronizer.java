package net.funkyjava.gametheory.cscfrm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.util.concurrent.Monitor;

public class CSCFRMMutexChancesSynchronizer implements CSCFRMChancesSynchronizer {

	private final CSCFRMChancesProducer producer;
	private final Monitor monitor = new Monitor();
	private final int nbRounds;
	private final BitSet[][] inUseBits;
	private final List<int[][]> collisionChances = new ArrayList<>();
	private final List<int[][]> availableChances = new LinkedList<>();
	private boolean stop = false;

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
		final List<int[][]> availableChances = this.availableChances;
		final Monitor monitor = this.monitor;
		final CSCFRMChancesProducer producer = this.producer;
		monitor.enter();
		try {
			if (stop) {
				return null;
			}
			if (!availableChances.isEmpty()) {
				return availableChances.remove(0);
			}
			final List<int[][]> collisionChances = this.collisionChances;
			while (true) {
				final int[][] chances = producer.produceChances();
				if (hasCollision(chances)) {
					collisionChances.add(chances);
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
		final List<int[][]> collisionChances = this.collisionChances;
		final List<int[][]> availableChances = this.availableChances;
		monitor.enter();
		try {
			endReserving(usedChances);
			producer.endedUsing(usedChances);
			if (stop) {
				return;
			}
			int nbCollision = collisionChances.size();
			for (int i = 0; i < nbCollision;) {
				final int[][] chances = collisionChances.get(i);
				if (!hasCollision(chances)) {
					availableChances.add(chances);
					reserve(chances);
					collisionChances.remove(i);
					nbCollision--;
				} else {
					i++;
				}
			}
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
		final List<int[][]> collisionChances = this.collisionChances;
		final List<int[][]> availableChances = this.availableChances;
		final CSCFRMChancesProducer producer = this.producer;
		final Monitor monitor = this.monitor;
		monitor.enter();
		this.stop = true;
		for (int[][] chances : collisionChances) {
			producer.endedUsing(chances);
		}
		collisionChances.clear();
		for (int[][] chances : availableChances) {
			endReserving(chances);
			producer.endedUsing(chances);
		}
		availableChances.clear();
		monitor.leave();
	}

	@Override
	public void reset() {
		final Monitor monitor = this.monitor;
		monitor.enter();
		try {
			stop = false;
		} finally {
			monitor.leave();
		}
	}

	@Override
	public List<Runnable> getProducers() {
		return Collections.emptyList();
	}

}

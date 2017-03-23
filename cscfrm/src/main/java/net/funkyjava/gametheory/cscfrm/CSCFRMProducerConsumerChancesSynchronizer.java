package net.funkyjava.gametheory.cscfrm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.Monitor.Guard;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CSCFRMProducerConsumerChancesSynchronizer implements CSCFRMChancesSynchronizer {

	@Setter
	private int availableChancesFloor = 50;
	@Setter
	private int numberToProduceEachTime = 50;
	private final CSCFRMChancesProducer producer;
	private final int nbRounds;
	private final Runnable producerRunnable = new Runnable() {

		@Override
		public void run() {
			try {
				while (!stop) {
					producerProcess();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};;
	private final List<int[][]> producedChances = new LinkedList<>();
	private final List<int[][]> availableChances = new LinkedList<>();
	private final List<int[][]> collisionChances = new ArrayList<>();
	private final List<int[][]> freedChances = new LinkedList<>();
	private final BitSet[][] inUseBits;
	private boolean stop = false;
	private final Monitor monitor = new Monitor();

	private final Guard consumerGuard = new Guard(monitor) {

		@Override
		public boolean isSatisfied() {
			if (stop) {
				return true;
			}
			if (!availableChances.isEmpty()) {
				return true;
			} else {
				log.info("{} STARVING", this.getClass().getName());
				return false;
			}
		}
	};

	private final Guard producerGuard = new Guard(monitor) {

		@Override
		public boolean isSatisfied() {
			return stop || availableChances.size() < availableChancesFloor;
		}
	};

	public CSCFRMProducerConsumerChancesSynchronizer(final CSCFRMChancesProducer producer, final int[][] chancesSizes) {
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

	public final int[][] getChances() throws InterruptedException {
		final Monitor monitor = this.monitor;
		monitor.enterWhen(consumerGuard);
		try {
			if (stop) {
				return null;
			}
			return availableChances.remove(0);
		} finally {
			monitor.leave();
		}
	}

	public final void endUsing(final int[][] usedChances) {
		final List<int[][]> freedChances = this.freedChances;
		synchronized (freedChances) {
			freedChances.add(usedChances);
		}
	}

	public final void stop() {
		final Monitor monitor = this.monitor;
		monitor.enter();
		stop = true;
		monitor.leave();
	}

	public final void reset() {
		stop = false;
	}

	private final void producerProcess() throws InterruptedException {
		final Monitor monitor = this.monitor;
		monitor.enterWhen(this.producerGuard);
		monitor.leave();
		if (stop) {
			return;
		}
		releaseFreedChances();
		checkCollisions();
		produce();
		sortProducedChances();
	}

	private final void produce() {
		final Monitor monitor = this.monitor;
		monitor.enter();
		final int numberToProduce = Math.max(this.numberToProduceEachTime - this.availableChances.size(), 0);
		monitor.leave();
		final List<int[][]> producedChances = this.producedChances;
		final CSCFRMChancesProducer producer = this.producer;
		for (int i = 0; i < numberToProduce; i++) {
			producedChances.add(producer.produceChances());
		}
	}

	private final void sortProducedChances() {
		final Monitor monitor = this.monitor;
		final List<int[][]> producedChances = this.producedChances;
		final List<int[][]> availableChances = this.availableChances;
		final List<int[][]> collisionChances = this.collisionChances;
		for (int[][] chances : producedChances) {
			if (hasCollision(chances)) {
				collisionChances.add(chances);
			} else {
				reserve(chances);
				monitor.enter();
				availableChances.add(chances);
				monitor.leave();
			}
		}
		producedChances.clear();
	}

	private final void releaseFreedChances() {
		final List<int[][]> freedChances = this.freedChances;
		final List<int[][]> chancesToRelease = new LinkedList<>();
		final CSCFRMChancesProducer producer = this.producer;
		synchronized (freedChances) {
			chancesToRelease.addAll(freedChances);
			freedChances.clear();
		}
		for (int[][] chances : chancesToRelease) {
			endReserving(chances);
			producer.endedUsing(chances);
		}
	}

	private final void checkCollisions() {
		final Monitor monitor = this.monitor;
		final List<int[][]> collisionChances = this.collisionChances;
		final List<int[][]> availableChances = this.availableChances;
		int nbCollision = collisionChances.size();
		int i = 0;
		while (i < nbCollision) {
			final int[][] chances = collisionChances.get(i);
			if (hasCollision(chances)) {
				i++;
				continue;
			}
			collisionChances.remove(i);
			reserve(chances);
			monitor.enter();
			availableChances.add(chances);
			monitor.leave();
			nbCollision--;
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
	public List<Runnable> getProducers() {
		return Collections.singletonList(producerRunnable);
	}

}

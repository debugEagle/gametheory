package net.funkyjava.gametheory.cscfrm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.NonNull;

public class CSCFRMRunner {

	@Getter
	private final CSCFRMData data;
	@Getter
	private final int[][] chancesSizes;
	@Getter
	private final int nbTrainerThreads;

	private ExecutorService executor = null;
	private final CSCFRMChancesSynchronizer chancesSynchronizer;
	private boolean stop = false;
	private Runnable[] trainerRunnables;
	private final List<Exception> exceptions = Collections.synchronizedList(new LinkedList<Exception>());

	private final class TrainerRunnable implements Runnable {

		private final CSCFRMTrainer trainer = new CSCFRMTrainer(data);

		@Override
		public void run() {
			final CSCFRMTrainer trainer = this.trainer;
			final CSCFRMChancesSynchronizer chancesSynchronizer = CSCFRMRunner.this.chancesSynchronizer;
			try {
				while (!stop) {
					final int[][] chances = chancesSynchronizer.getChances();
					if (chances == null) {
						return;
					}
					trainer.train(chances);
				}
			} catch (Exception e) {
				e.printStackTrace();
				exceptions.add(e);
			}
		}

	}

	public CSCFRMRunner(@NonNull final CSCFRMData data, @NonNull final CSCFRMChancesProducer chancesProducer,
			@NonNull final int[][] chancesSizes, final int nbTrainerThreads) {
		checkArgument(nbTrainerThreads > 0, "The number of trainer threads must be > 0");
		this.data = data;
		this.chancesSizes = chancesSizes;
		this.nbTrainerThreads = nbTrainerThreads;
		this.chancesSynchronizer = new CSCFRMChancesSynchronizer(chancesProducer, chancesSizes);
		final Runnable[] trainerRunnables = this.trainerRunnables = (Runnable[]) new Object[nbTrainerThreads];
		for (int i = 0; i < nbTrainerThreads; i++) {
			trainerRunnables[i] = new TrainerRunnable();
		}
	}

	public synchronized final void start() {
		checkState(executor == null, "An executor is already running");
		exceptions.clear();
		final int nbTrainerThreads = this.nbTrainerThreads;
		final Runnable[] trainerRunnables = this.trainerRunnables;
		chancesSynchronizer.reset();
		final ExecutorService executor = this.executor = Executors.newFixedThreadPool(nbTrainerThreads + 1);
		executor.execute(chancesSynchronizer.getProducerRunnable());
		for (int i = 0; i < nbTrainerThreads; i++) {
			executor.execute(trainerRunnables[i]);
		}
	}

	public synchronized final List<Exception> stopAndAwaitTermination() throws InterruptedException {
		checkState(executor != null, "No executor is running");
		stop = true;
		chancesSynchronizer.stop();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		executor = null;
		return exceptions;
	}
}

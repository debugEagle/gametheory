package net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import org.apache.commons.math3.ml.neuralnet.MapUtils;
import org.apache.commons.math3.ml.neuralnet.Network;
import org.apache.commons.math3.ml.neuralnet.Neuron;

import net.funkyjava.gametheory.gameutil.cards.Cards52Strings;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms.HoldemHistograms.HoldemHistogramsStreets;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms.HoldemHistograms.HoldemHistogramsValues;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

public abstract class PreflopHistogramsKohonenRunConfigurationOld implements HistogramsKohonenRunConfiguration {

	private static final class PreflopHistogramIndexGenerator implements HistogramIndexGenerator {

		private final WaughIndexer indexer = new WaughIndexer(new int[] { 2 });
		private final Random random = new Random();
		private final int[][] cardsGroups = new int[][] { new int[2] };
		private final int[] cards = cardsGroups[0];

		@Override
		public int getHistogramIndex() {
			final int firstCard = cards[0] = random.nextInt(52);
			final int secondCard = cards[1] = random.nextInt(51);

			if (secondCard >= firstCard) {
				cards[1]++;
			}
			return indexer.indexOf(cardsGroups);
		}

	}

	private final int featuresSize;
	private final int samplesSize;
	private final int nbTasks;
	private final DistanceMeasure distance;
	private final double[][] histograms;

	public PreflopHistogramsKohonenRunConfigurationOld(final int featureSize, final int sampleSize, final int nbTasks)
			throws IOException, ClassNotFoundException {
		this.featuresSize = featureSize;
		this.samplesSize = sampleSize;
		this.nbTasks = nbTasks;
		histograms = HoldemHistograms.generateHistograms(HoldemHistogramsStreets.PREFLOP, HoldemHistogramsValues.EHS,
				featuresSize);
		distance = new EarthMoversDistance();
	}

	public int[] getBuckets(Network network) {
		final int[] res = new int[169];
		for (int i = 0; i < 169; i++) {
			final Neuron neuron = MapUtils.findBest(histograms[i], network, distance);
			res[i] = (int) neuron.getIdentifier();
		}
		return res;
	}

	public int[][] get2DBuckets(Network network) {
		final int[][] buckets = new int[13][13];
		final WaughIndexer indexer = new WaughIndexer(new int[] { 2 });
		final IntCardsSpec cardsSpec = indexer.getCardsSpec();
		final int[][] cardsGroups = new int[][] { { 0, 0 } };
		final int[] cards = cardsGroups[0];
		for (int rank1 = 0; rank1 < 13; rank1++) {
			for (int rank2 = 0; rank2 < 13; rank2++) {
				if (rank1 <= rank2) {
					// Off suite or pair
					cards[0] = cardsSpec.getCard(rank1, 0);
					cards[1] = cardsSpec.getCard(rank2, 1);
				} else if (rank2 < rank1) {
					// Suited
					cards[0] = cardsSpec.getCard(rank1, 0);
					cards[1] = cardsSpec.getCard(rank2, 0);
				}
				final Neuron neuron = MapUtils.findBest(histograms[indexer.indexOf(cardsGroups)], network, distance);
				buckets[rank1][rank2] = (int) neuron.getIdentifier();
			}
		}
		return buckets;
	}

	public void print2DBuckets(Network network) {
		final int[][] buckets = get2DBuckets(network);
		final WaughIndexer indexer = new WaughIndexer(new int[] { 2 });
		final IntCardsSpec cardsSpec = indexer.getCardsSpec();
		final Cards52Strings strings = new Cards52Strings(cardsSpec);
		System.out.print("\t");
		for (int topRank = 0; topRank < 13; topRank++) {
			System.out.print(strings.getRankStr(cardsSpec.getCard(topRank, 0)) + "\t");
		}
		System.out.println();
		for (int rank2 = 0; rank2 < 13; rank2++) {
			System.out.print(strings.getRankStr(cardsSpec.getCard(rank2, 0)) + "\t");
			for (int rank1 = 0; rank1 < 13; rank1++) {
				System.out.print(buckets[rank1][rank2] + "\t");
			}
			System.out.println();
		}
	}

	@Override
	public DistanceMeasure getDistance() {
		return distance;
	}

	@Override
	public double[][] getHistograms() {
		return histograms;
	}

	@Override
	public int getSamplesSize() {
		return samplesSize;
	}

	@Override
	public int getNbTasks() {
		return nbTasks;
	}

	@Override
	public HistogramIndexGenerator getIndexGenerator() {
		return new PreflopHistogramIndexGenerator();
	}

}

package net.funkyjava.gametheory.gameutil.poker.he.clustering;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import org.apache.commons.math3.ml.neuralnet.FeatureInitializer;
import org.apache.commons.math3.ml.neuralnet.FeatureInitializerFactory;
import org.apache.commons.math3.ml.neuralnet.MapUtils;
import org.apache.commons.math3.ml.neuralnet.Network;
import org.apache.commons.math3.ml.neuralnet.Neuron;

import net.funkyjava.gametheory.gameutil.cards.Cards52Strings;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.clustering.kohonen.KohonenRunConfiguration;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HoldemHSHistograms;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HoldemHSHistograms.HoldemHSHistogramsStreets;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HoldemHSHistograms.HoldemHSHistogramsValues;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

public abstract class HoldemKohonenRunConfiguration implements KohonenRunConfiguration {

	private final int featuresSize;
	private final int samplesSize;
	private final int nbTasks;
	private final DistanceMeasure distance;
	private final double[][] vectors;
	private final HoldemHSHistogramsStreets street;

	public HoldemKohonenRunConfiguration(final HoldemHSHistogramsStreets street,
			final HoldemHSHistogramsValues nextStreetValue, final int featureSize, final int nbSamplesPerTask,
			final int nbTasks) throws IOException, ClassNotFoundException {
		this.street = street;
		this.featuresSize = featureSize;
		this.samplesSize = nbSamplesPerTask;
		this.nbTasks = nbTasks;
		vectors = HoldemHSHistograms.generateHistograms(street, nextStreetValue, featuresSize);
		distance = new EarthMoversDistance();
	}

	@Override
	public DistanceMeasure getDistance() {
		return distance;
	}

	@Override
	public double[][] getVectors() {
		return vectors;
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
	public FeatureInitializer[] getFeaturesInitializers() {
		final FeatureInitializer[] res = new FeatureInitializer[featuresSize];
		for (int i = 0; i < featuresSize; i++) {
			res[i] = FeatureInitializerFactory.uniform(0.1, 1);
		}
		return res;
	}

	public int[][] getPreflop2DBuckets(Network network, boolean standardized) {
		if (street != HoldemHSHistogramsStreets.PREFLOP) {
			throw new IllegalStateException("Wrong street " + street);
		}
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
				final Neuron neuron = MapUtils.findBest(vectors[indexer.indexOf(cardsGroups)], network, distance);
				buckets[rank1][rank2] = (int) neuron.getIdentifier();
			}
		}
		if (!standardized) {
			return buckets;
		}
		final int[][] res = new int[13][13];
		int bucketNum = 0;
		final Map<Integer, Integer> permutations = new HashMap<>();
		for (int i = 0; i < 13; i++) {
			for (int j = 0; j < 13; j++) {
				final int num = buckets[i][j];
				Integer permNum = permutations.get(num);
				if (permNum == null) {
					permNum = bucketNum;
					permutations.put(num, permNum);
					bucketNum++;
				}
				res[i][j] = permNum;
			}
		}
		return res;
	}

	public void printPreflop2DBuckets(Network network, boolean standardized) {
		if (street != HoldemHSHistogramsStreets.PREFLOP) {
			throw new IllegalStateException("Wrong street " + street);
		}
		final int[][] buckets = getPreflop2DBuckets(network, standardized);
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
}

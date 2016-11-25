package net.funkyjava.gametheory.gameutil.poker.he.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;

import net.funkyjava.gametheory.gameutil.cards.Cards52Strings;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables.HSType;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables.Streets;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HoldemHSHistograms;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

public class HoldemHSClusterer {

	private final List<DoublePoint> points;
	private final Clusterer<DoublePoint> clusterer;
	private final Streets street;

	private HoldemHSClusterer(final Streets street, final HSType nextStreetHSType, final int nbBars,
			final Clusterer<DoublePoint> clusterer) {
		this.street = street;
		this.clusterer = clusterer;
		final double[][] vectors = HoldemHSHistograms.generateHistograms(street, nextStreetHSType, nbBars);
		final int nbPoints = vectors.length;
		final List<DoublePoint> points = new ArrayList<>(nbPoints);
		for (int i = 0; i < nbPoints; i++) {
			points.add(new DoublePoint(vectors[i]));
		}
		this.points = Collections.unmodifiableList(points);
	}

	private HoldemHSClusterer(final Streets street, final HSType hsType, final Clusterer<DoublePoint> clusterer) {
		this.street = street;
		this.clusterer = clusterer;
		final double[] table = AllHoldemHSTables.getTable(street, hsType);
		final int nbPoints = table.length;
		final List<DoublePoint> points = new ArrayList<>(nbPoints);
		for (int i = 0; i < nbPoints; i++) {
			points.add(new DoublePoint(new double[] { table[i] }));
		}
		this.points = Collections.unmodifiableList(points);
	}

	public List<? extends Cluster<DoublePoint>> cluster() {
		return clusterer.cluster(points);
	}

	public static HoldemHSClusterer clustererForNextStreetHSHistograms(final Streets street,
			final HSType nextStreetHSType, final int nbBars, final Clusterer<DoublePoint> clusterer) {
		return new HoldemHSClusterer(street, nextStreetHSType, nbBars, clusterer);
	}

	public static HoldemHSClusterer clustererForStreetHS(final Streets street, final HSType hsType,
			final Clusterer<DoublePoint> clusterer) {
		return new HoldemHSClusterer(street, hsType, clusterer);
	}

	public <T extends Clusterable> int[][] getCanonicalPreflop2DBuckets(final List<? extends Cluster<T>> clusters,
			final List<T> points) {
		if (street != Streets.PREFLOP) {
			throw new IllegalStateException("Wrong street " + street);
		}
		final int nbClusters = clusters.size();
		final int[][] buckets = new int[13][13];
		final WaughIndexer indexer = new WaughIndexer(new int[] { 2 });
		final IntCardsSpec cardsSpec = indexer.getCardsSpec();
		final int[][] cardsGroups = new int[][] { { 0, 0 } };
		final int[] cards = cardsGroups[0];
		final Map<Integer, Integer> permutations = new HashMap<>();
		int count = 0;
		for (int rank1 = 0; rank1 < 13; rank1++) {
			pointsLoop: for (int rank2 = 0; rank2 < 13; rank2++) {
				if (rank1 <= rank2) {
					// Off suite or pair
					cards[0] = cardsSpec.getCard(rank1, 0);
					cards[1] = cardsSpec.getCard(rank2, 1);
				} else if (rank2 < rank1) {
					// Suited
					cards[0] = cardsSpec.getCard(rank1, 0);
					cards[1] = cardsSpec.getCard(rank2, 0);
				}
				final T point = points.get(indexer.indexOf(cardsGroups));
				for (int j = 0; j < nbClusters; j++) {
					if (clusters.get(j).getPoints().contains(point)) {
						Integer bucket = permutations.get(j);
						if (bucket == null) {
							bucket = count++;
							permutations.put(j, bucket);
						}
						buckets[rank1][rank2] = bucket;
						continue pointsLoop;
					}
				}
				;
			}
		}
		return buckets;
	}

	public <T extends Clusterable> void printPreflop2DBuckets(final List<? extends Cluster<T>> clusters,
			final List<T> points) {
		if (street != Streets.PREFLOP) {
			throw new IllegalStateException("Wrong street " + street);
		}
		final int[][] buckets = getCanonicalPreflop2DBuckets(clusters, points);
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

	public List<DoublePoint> getPoints() {
		return points;
	}
}

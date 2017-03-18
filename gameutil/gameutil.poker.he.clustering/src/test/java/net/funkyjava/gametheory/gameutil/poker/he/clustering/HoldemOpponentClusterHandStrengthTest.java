package net.funkyjava.gametheory.gameutil.poker.he.clustering;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.indexing.CardsGroupsIndexer;
import net.funkyjava.gametheory.gameutil.clustering.ClustersToBuckets;
import net.funkyjava.gametheory.gameutil.clustering.IndexedDoublePoint;
import net.funkyjava.gametheory.gameutil.clustering.MultiClusterer;
import net.funkyjava.gametheory.gameutil.clustering.evaluation.SumOfClusterVariancesGT;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables.HSType;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables.Streets;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.Holdem7CardsEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.twoplustwo.TwoPlusTwoEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

@Slf4j
public class HoldemOpponentClusterHandStrengthTest {
	private static boolean runLongTest = false;
	private AllHoldemHSTables<WaughIndexer, WaughIndexer, WaughIndexer, WaughIndexer> all;

	@Test
	public void test() throws IOException, URISyntaxException {
		if (!runLongTest) {
			log.info("Not running HoldemOpponentClusterHandStrengthTest  (takes oo long). "
					+ "Set this test class runLongTest boolean to true to execute it.");
			return;
		}

		all = AllHoldemHSTables.getTablesWithWaughIndexersTwoPlusTwoEval();
		try {
			all.readFrom(Paths.get("/Users/pitt/ALL_HE_HS.zip"));
		} catch (Exception e) {
			log.warn("Unable to load Holdem HS Tables", e);
			return;
		}
		final int nbBuckets = 8;
		final int[] preflopBuckets = getPreflopBuckets(nbBuckets);
		final CardsGroupsIndexer preflopIndexer = all.getHoleCardsIndexer();
		final WaughIndexer riverIndexer = all.getRiverCardsIndexer();
		log.info("River size : {}", riverIndexer.getIndexSize());
		final Holdem7CardsEvaluator eval = new TwoPlusTwoEvaluator();
		all = null;
		HoldemOpponentClusterHandStrength.opponentClusterHandStrength(preflopIndexer, preflopBuckets, nbBuckets,
				Streets.RIVER, riverIndexer, eval);
	}

	private int[] getPreflopBuckets(int nbBuckets) {
		log.info("Computing preflop buckets");
		final int nbBars = 10;

		final int taskSamplesSize = 10_000;
		final int maxTasks = 100;
		final DistanceMeasure distance = new EarthMoversDistance();

		final Clusterer<IndexedDoublePoint> baseClusterer = new KMeansPlusPlusClusterer<>(nbBuckets,
				taskSamplesSize * maxTasks);
		final MultiClusterer<IndexedDoublePoint> multiClusterer = new MultiClusterer<>(baseClusterer, 4, 4);
		final HoldemHSClusterer clusterer = HoldemHSClusterer.clustererForNextStreetHSHistograms(all, Streets.PREFLOP,
				HSType.EHS, nbBars, multiClusterer);
		final SumOfClusterVariancesGT<IndexedDoublePoint> evaluator = new SumOfClusterVariancesGT<>(distance);
		log.debug("Starting preflop's flops EHS histograms clustering with k-means++");
		List<? extends Cluster<IndexedDoublePoint>> clusters = clusterer.cluster();
		log.debug("Obtained buckets :");
		log.debug("");
		clusterer.printPreflop2DBuckets(clusters, clusterer.getPoints());
		final double score = evaluator.score(clusters);
		log.debug("Score : {}", score);
		return ClustersToBuckets.getBucketsForIndexedPoints(clusters, clusterer.getPoints(), true);
	}
}

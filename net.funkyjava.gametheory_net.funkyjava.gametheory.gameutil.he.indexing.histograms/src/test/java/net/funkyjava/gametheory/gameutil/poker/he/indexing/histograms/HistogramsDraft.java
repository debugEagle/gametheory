package net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.math3.ml.neuralnet.FeatureInitializer;
import org.apache.commons.math3.ml.neuralnet.FeatureInitializerFactory;
import org.apache.commons.math3.ml.neuralnet.SquareNeighbourhood;
import org.apache.commons.math3.ml.neuralnet.sofm.LearningFactorFunction;
import org.apache.commons.math3.ml.neuralnet.sofm.LearningFactorFunctionFactory;
import org.apache.commons.math3.ml.neuralnet.sofm.NeighbourhoodSizeFunction;
import org.apache.commons.math3.ml.neuralnet.sofm.NeighbourhoodSizeFunctionFactory;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms.HoldemHistograms.HoldemHistogramsStreets;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms.HoldemHistograms.HoldemHistogramsValues;

@Slf4j
public class HistogramsDraft {

	@Test
	public void draft() throws IOException, ClassNotFoundException, InterruptedException {
		AllHoldemHSTables.readFrom(Paths.get("/Users/pitt/ALL_HE_HS.zip"));
		final HoldemHistogramsStreets street = HoldemHistogramsStreets.PREFLOP;
		final int featureSize = 10;
		final int nbSamplesPerTask = 10_000;
		final int nbTasks = 30;
		HoldemHistogramsKohonenRunConfiguration conf = new HoldemHistogramsKohonenRunConfiguration(street,
				HoldemHistogramsValues.EHS, featureSize, nbSamplesPerTask, nbTasks) {

			@Override
			public HistogramsKohonenNetworkConfiguration getNetworkConf() {
				FeatureInitializer init = FeatureInitializerFactory.uniform(0, 1);
				FeatureInitializer[] inits = new FeatureInitializer[featureSize];
				for (int i = 0; i < featureSize; i++) {
					inits[i] = init;
				}
				return HistogramsKohonenNetworkConfiguration.getSquareMesh2DConfiguration(4, true, 4, true,
						SquareNeighbourhood.MOORE, inits);
			}

			@Override
			public NeighbourhoodSizeFunction getNeighbourhoodSizeFunction() {
				return NeighbourhoodSizeFunctionFactory.exponentialDecay(2, 1, 8_000_000 - 1);
			}

			@Override
			public LearningFactorFunction getLearningFactorFunction() {
				return LearningFactorFunctionFactory.exponentialDecay(1, 0.05, 2_000_000 - 1);
			}
		};
		final HistogramsKohonenRunner runner = new HistogramsKohonenRunner(conf);
		final boolean areBucketsStable = runner.compareFullRuns(4, 15);
		conf.printPreflop2DBuckets(runner.getLastNetwork());
		log.debug("Buckets are stable : {}", areBucketsStable);
	}
}

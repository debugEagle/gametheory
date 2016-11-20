package net.funkyjava.gametheory.gameutil.poker.he.clustering;

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
import net.funkyjava.gametheory.gameutil.clustering.kohonen.KohonenNetworkConfiguration;
import net.funkyjava.gametheory.gameutil.clustering.kohonen.KohonenRunner;
import net.funkyjava.gametheory.gameutil.clustering.kohonen.KohonenRunner.FullRunsResult;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HoldemHSHistograms.HoldemHSHistogramsStreets;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HoldemHSHistograms.HoldemHSHistogramsValues;

@Slf4j
public class PreflopKohonenTest {

	@Test
	public void draft() throws IOException, ClassNotFoundException, InterruptedException {
		try {
			AllHoldemHSTables.readFrom(Paths.get("/Users/pitt/ALL_HE_HS.zip"));
		} catch (Exception e) {
			log.warn("Unable to load Holdem HS Tables", e);
			return;
		}
		final HoldemHSHistogramsStreets street = HoldemHSHistogramsStreets.PREFLOP;
		final int featureSize = 10;
		final int nbSamplesPerTask = 10_000;
		final int nbTasks = 30;
		final int baseForDecay = 600_000;
		HoldemKohonenRunConfiguration conf = new HoldemKohonenRunConfiguration(street, HoldemHSHistogramsValues.EHS,
				featureSize, nbSamplesPerTask, nbTasks) {

			@Override
			public KohonenNetworkConfiguration getNetworkConf() {
				FeatureInitializer init = FeatureInitializerFactory.uniform(0, 0.2);
				FeatureInitializer[] inits = new FeatureInitializer[featureSize];
				for (int i = 0; i < featureSize; i++) {
					inits[i] = init;
				}
				return KohonenNetworkConfiguration.getSquareMesh2DConfiguration(4, false, 4, false,
						SquareNeighbourhood.MOORE, inits);
			}

			@Override
			public NeighbourhoodSizeFunction getNeighbourhoodSizeFunction() {
				return NeighbourhoodSizeFunctionFactory.exponentialDecay(2, 1, baseForDecay * 4);
			}

			@Override
			public LearningFactorFunction getLearningFactorFunction() {
				return LearningFactorFunctionFactory.exponentialDecay(1, 0.1, baseForDecay);
			}
		};

		final KohonenRunner runner = new KohonenRunner(conf);
		final FullRunsResult result = runner.compareFullRuns(3, 3);
		log.debug("Max equal runs results {}", result.getMaxEqualResults());
	}
}

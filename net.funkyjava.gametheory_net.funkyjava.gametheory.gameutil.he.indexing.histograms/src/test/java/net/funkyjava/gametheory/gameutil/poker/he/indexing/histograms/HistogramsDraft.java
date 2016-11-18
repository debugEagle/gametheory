package net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.math3.ml.neuralnet.sofm.LearningFactorFunction;
import org.apache.commons.math3.ml.neuralnet.sofm.NeighbourhoodSizeFunction;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables;

@Slf4j
public class HistogramsDraft {

	@Test
	public void draft() throws IOException, ClassNotFoundException, InterruptedException {
		AllHoldemHSTables.readFrom(Paths.get("/Users/pitt/ALL_HE_HS.zip"));
		PreflopHistogramsKohonenRunConfigurationOld workstation = new PreflopHistogramsKohonenRunConfigurationOld(101, 1000,
				100) {

			@Override
			public HistogramsKohonenNetworkConfiguration getNetworkConf() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public NeighbourhoodSizeFunction getNeighbourhoodSizeFunction() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public LearningFactorFunction getLearningFactorFunction() {
				// TODO Auto-generated method stub
				return null;
			}

		};
		final long start = System.currentTimeMillis();
		final HistogramsKohonenRunner runner = new HistogramsKohonenRunner(workstation);
		// final long nbSamples = runner.runUntilNoChange();
		// final double time = System.currentTimeMillis() - start;
		// final double timeSeconds = time / 1000;
		// log.debug("PreflopHistogramsKohonenWorkstation Took {} seconds to
		// process {} samples, it is {} samples/second",
		// timeSeconds, nbSamples, nbSamples / (double) timeSeconds);
		// workstation.printBuckets(runner.getLastNetwork());
		final boolean areBucketsStable = runner.compareFullRuns(4, 7);
		workstation.print2DBuckets(runner.getLastNetwork());
		log.debug("Buckets are stable : {}", areBucketsStable);
	}
}

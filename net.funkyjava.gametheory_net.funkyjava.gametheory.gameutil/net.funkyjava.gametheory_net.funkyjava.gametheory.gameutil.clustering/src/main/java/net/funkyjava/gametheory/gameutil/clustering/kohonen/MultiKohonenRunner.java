package net.funkyjava.gametheory.gameutil.clustering.kohonen;

import java.util.Arrays;

import org.apache.commons.math3.ml.neuralnet.Network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiKohonenRunner {

	private final KohonenRunConfiguration conf;

	public MultiKohonenRunner(KohonenRunConfiguration conf) {
		this.conf = conf;
	}

	public RunsResults run(int nbFullRuns, int nbEqualitiesRequired) throws InterruptedException {
		final int[][] buckets = new int[nbFullRuns][];
		final Network[] networks = new Network[nbFullRuns];
		for (int i = 0; i < nbFullRuns; i++) {
			log.debug("Full run {}", i + 1);
			final long start = System.currentTimeMillis();
			final KohonenRunner runner = new KohonenRunner(conf);
			runner.runUntilNoChange(nbEqualitiesRequired);
			final long time = (System.currentTimeMillis() - start) / 1000;
			log.debug("Took {} seconds", time);
			buckets[i] = runner.getCanonicalBuckets();
			networks[i] = runner.getNetwork();
		}
		return new RunsResults(buckets, networks);
	}

	@Data
	@AllArgsConstructor
	public static class RunsResults {

		private final int[][] runsBuckets;
		private final Network[] networks;

		public int getMaxEqualResultsCount() {
			int max = 0;
			final int nb = runsBuckets.length;
			final boolean[] done = new boolean[nb];
			for (int i = 0; i < nb; i++) {
				if (done[i]) {
					continue;
				}
				final int[] buckets = runsBuckets[i];
				int count = 1;
				for (int j = i + 1; j < nb; j++) {
					if (done[j]) {
						continue;
					}
					if (Arrays.equals(buckets, runsBuckets[j])) {
						count++;
						done[j] = true;
					}
				}
				if (count > max) {
					max = count;
				}
			}
			return max;
		}
	}
}

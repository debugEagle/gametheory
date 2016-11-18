package net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.neuralnet.MapUtils;
import org.apache.commons.math3.ml.neuralnet.Network;
import org.apache.commons.math3.ml.neuralnet.Neuron;
import org.apache.commons.math3.ml.neuralnet.oned.NeuronString;
import org.apache.commons.math3.ml.neuralnet.sofm.KohonenTrainingTask;
import org.apache.commons.math3.ml.neuralnet.sofm.KohonenUpdateAction;
import org.apache.commons.math3.ml.neuralnet.twod.NeuronSquareMesh2D;

import com.google.common.collect.Iterators;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HistogramsKohonenRunner {

	private final double[][] histograms;
	private final Network network;
	private final DistanceMeasure distance;
	private final int nbTasks;
	private final int samplesSize;
	private final HistogramIndexGenerator indexGenerator;
	private final KohonenUpdateAction updateAction;
	private final HistogramsKohonenRunConfiguration runConf;

	private Network lastNetwork;

	public HistogramsKohonenRunner(HistogramsKohonenRunConfiguration runConf) {
		this.runConf = runConf;
		histograms = runConf.getHistograms();
		network = createNetwork(runConf);
		distance = runConf.getDistance();
		nbTasks = runConf.getNbTasks();
		samplesSize = runConf.getSamplesSize();
		indexGenerator = runConf.getIndexGenerator();
		updateAction = new KohonenUpdateAction(runConf.getDistance(), runConf.getLearningFactorFunction(),
				runConf.getNeighbourhoodSizeFunction());
		lastNetwork = network;
	}

	public void run() throws InterruptedException {
		final ExecutorService exe = Executors
				.newFixedThreadPool(Math.min(1, Runtime.getRuntime().availableProcessors() - 1));

		for (int task = 0; task < nbTasks; task++) {
			final double[][] features = new double[samplesSize][];
			for (int i = 0; i < samplesSize; i++) {
				features[i] = histograms[indexGenerator.getHistogramIndex()];
			}
			final KohonenTrainingTask trainingTask = new KohonenTrainingTask(network, Iterators.forArray(features),
					updateAction);
			exe.execute(trainingTask);
		}
		exe.shutdown();
		exe.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public long runUntilNoChange(int nbEqualitiesRequired) throws InterruptedException {
		checkArgument(nbEqualitiesRequired > 0);
		int nbRun = 0;
		long nbSamples = 0;
		int[] buckets = null;
		final HistogramsKohonenRunner runner = new HistogramsKohonenRunner(runConf);
		int nbEqualities = 0;
		while (true) {
			runner.run();
			lastNetwork = runner.network;
			nbSamples += samplesSize * nbTasks;
			nbRun++;
			if (buckets == null) {
				buckets = runner.getBuckets();
				continue;
			}
			log.debug("Trained on {} samples ({} runs)", nbSamples, nbRun);
			final int[] newBuckets = runner.getBuckets();
			if (areBucketsEqual(buckets, newBuckets, false)) {
				nbEqualities++;
				if (nbEqualities == nbEqualitiesRequired) {
					return nbSamples;
				}
			}
			buckets = newBuckets;
		}
	}

	public boolean compareFullRuns(int maxNbFullRuns, int nbEqualitiesRequired) throws InterruptedException {
		int[] buckets = null;
		for (int i = 0; i < maxNbFullRuns; i++) {
			log.debug("Full run {}", i + 1);
			final HistogramsKohonenRunner runner = new HistogramsKohonenRunner(runConf);
			runner.runUntilNoChange(nbEqualitiesRequired);
			if (buckets == null) {
				buckets = runner.getBuckets();
				continue;
			}
			final int[] newBuckets = runner.getBuckets();
			if (!areBucketsEqual(buckets, newBuckets, true)) {
				log.debug(Arrays.toString(buckets));
				log.debug(Arrays.toString(newBuckets));
				return false;
			}
			buckets = newBuckets;
			lastNetwork = runner.getLastNetwork();
		}
		return true;
	}

	private static boolean areBucketsEqual(final int[] buckets1, final int[] buckets2, final boolean permute) {
		if (buckets1.length != buckets2.length) {
			return false;
		}
		final Map<Integer, Integer> permutations = new HashMap<>();
		final int length = buckets1.length;
		for (int i = 0; i < length; i++) {
			final int num1 = buckets1[i];
			final int num2 = buckets2[i];
			if (num1 != num2) {
				if (!permute) {
					return false;
				}
				final Integer permuted = permutations.get(num1);
				if (permuted == null) {
					permutations.put(num1, num2);
				} else if (num2 != permuted) {
					return false;
				}
			}
		}
		return true;
	}

	private static Network createNetwork(HistogramsKohonenRunConfiguration runConf) {
		final HistogramsKohonenNetworkConfiguration netConf = runConf.getNetworkConf();
		if (netConf.isTwoD()) {
			return new NeuronSquareMesh2D(netConf.getNumRows(), netConf.isWrapRowDim(), netConf.getNumCols(),
					netConf.isWrapColDim(), netConf.getNeighbourhoodType(), runConf.getFeaturesInitializers())
							.getNetwork();
		} else {
			return new NeuronString(netConf.getNumCols(), netConf.isWrapColDim(), runConf.getFeaturesInitializers())
					.getNetwork();
		}
	}

	public int[] getBuckets() {
		final int nbNeurons = runConf.getNetworkConf().getNbNeurons();
		final int[] res = new int[nbNeurons];
		for (int i = 0; i < nbNeurons; i++) {
			final Neuron neuron = MapUtils.findBest(histograms[i], lastNetwork, distance);
			res[i] = (int) neuron.getIdentifier();
		}
		return res;
	}

	public Network getLastNetwork() {
		return lastNetwork;
	}
}

package net.funkyjava.gametheory.gameutil.clustering.kohonen;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
public class KohonenRunner {

	private final double[][] vectors;
	private Network network;
	private final DistanceMeasure distance;
	private final int nbTasks;
	private final int samplesSize;
	private final KohonenUpdateAction updateAction;
	private final KohonenRunConfiguration runConf;
	private final Random random = new Random();

	public KohonenRunner(KohonenRunConfiguration runConf) {
		this.runConf = runConf;
		vectors = runConf.getVectors();
		network = createNetwork(runConf);
		distance = runConf.getDistance();
		nbTasks = runConf.getNbTasks();
		samplesSize = runConf.getSamplesSize();
		updateAction = new KohonenUpdateAction(runConf.getDistance(), runConf.getLearningFactorFunction(),
				runConf.getNeighbourhoodSizeFunction());
	}

	public void run() throws InterruptedException {
		final ExecutorService exe = Executors
				.newFixedThreadPool(Math.min(1, Runtime.getRuntime().availableProcessors() - 1));
		final int nbVect = vectors.length;
		for (int task = 0; task < nbTasks; task++) {
			final double[][] features = new double[samplesSize][];
			for (int i = 0; i < samplesSize; i++) {
				features[i] = vectors[random.nextInt(nbVect)];
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
		int nbEqualities = 0;
		while (true) {
			run();
			nbSamples += samplesSize * nbTasks;
			nbRun++;
			log.debug("Trained on {} samples ({} runs)", nbSamples, nbRun);
			if (buckets == null) {
				buckets = getBuckets();
				continue;
			}
			final int[] newBuckets = getBuckets();
			if (Arrays.equals(buckets, newBuckets)) {
				nbEqualities++;
				if (nbEqualities == nbEqualitiesRequired) {
					return nbSamples;
				}
			}
			buckets = newBuckets;
		}
	}

	private static Network createNetwork(KohonenRunConfiguration runConf) {
		final KohonenNetworkConfiguration netConf = runConf.getNetworkConf();
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
		final int nbVectors = vectors.length;
		final int[] res = new int[nbVectors];
		for (int i = 0; i < nbVectors; i++) {
			final Neuron neuron = MapUtils.findBest(vectors[i], network, distance);
			res[i] = (int) neuron.getIdentifier();
		}
		return res;
	}

	public int[] getCanonicalBuckets() {
		final Map<Integer, Integer> permutations = new HashMap<>();
		final int nbVectors = vectors.length;
		final int[] res = new int[nbVectors];
		int count = 0;
		for (int i = 0; i < nbVectors; i++) {
			final Neuron neuron = MapUtils.findBest(vectors[i], network, distance);
			final int num = (int) neuron.getIdentifier();
			Integer bucket = permutations.get(num);
			if (bucket == null) {
				bucket = count++;
				permutations.put(num, bucket);
			}
			res[i] = bucket;
		}
		return res;
	}

	public Network getNetwork() {
		return network;
	}

}

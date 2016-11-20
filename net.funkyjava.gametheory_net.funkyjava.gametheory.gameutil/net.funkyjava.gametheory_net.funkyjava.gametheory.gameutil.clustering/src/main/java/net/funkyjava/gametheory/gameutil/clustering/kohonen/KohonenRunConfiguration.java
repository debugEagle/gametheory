package net.funkyjava.gametheory.gameutil.clustering.kohonen;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.neuralnet.FeatureInitializer;
import org.apache.commons.math3.ml.neuralnet.sofm.LearningFactorFunction;
import org.apache.commons.math3.ml.neuralnet.sofm.NeighbourhoodSizeFunction;

public interface KohonenRunConfiguration {

	KohonenNetworkConfiguration getNetworkConf();

	DistanceMeasure getDistance();

	LearningFactorFunction getLearningFactorFunction();

	NeighbourhoodSizeFunction getNeighbourhoodSizeFunction();

	double[][] getVectors();

	int getSamplesSize();

	int getNbTasks();

	FeatureInitializer[] getFeaturesInitializers();
}

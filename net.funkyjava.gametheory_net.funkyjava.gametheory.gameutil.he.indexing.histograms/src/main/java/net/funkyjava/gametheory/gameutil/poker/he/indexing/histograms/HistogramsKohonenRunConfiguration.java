package net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.neuralnet.FeatureInitializer;
import org.apache.commons.math3.ml.neuralnet.sofm.LearningFactorFunction;
import org.apache.commons.math3.ml.neuralnet.sofm.NeighbourhoodSizeFunction;

public interface HistogramsKohonenRunConfiguration {

	HistogramsKohonenNetworkConfiguration getNetworkConf();

	DistanceMeasure getDistance();

	LearningFactorFunction getLearningFactorFunction();

	NeighbourhoodSizeFunction getNeighbourhoodSizeFunction();

	double[][] getHistograms();

	int getSamplesSize();

	int getNbTasks();

	HistogramIndexGenerator getIndexGenerator();

	FeatureInitializer[] getFeaturesInitializers();
}

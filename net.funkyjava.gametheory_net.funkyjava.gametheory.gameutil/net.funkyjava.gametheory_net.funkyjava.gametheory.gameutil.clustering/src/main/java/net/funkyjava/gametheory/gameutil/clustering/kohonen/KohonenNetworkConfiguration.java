package net.funkyjava.gametheory.gameutil.clustering.kohonen;

import org.apache.commons.math3.ml.neuralnet.FeatureInitializer;
import org.apache.commons.math3.ml.neuralnet.SquareNeighbourhood;

import lombok.Getter;

public class KohonenNetworkConfiguration {
	@Getter
	private boolean twoD;
	@Getter
	private int numRows;
	@Getter
	private boolean wrapRowDim;
	@Getter
	private int numCols;
	@Getter
	private boolean wrapColDim;
	@Getter
	private SquareNeighbourhood neighbourhoodType;
	@Getter
	private int nbNeurons;

	private KohonenNetworkConfiguration() {

	}

	public static KohonenNetworkConfiguration getSquareMesh2DConfiguration(int numRows, boolean wrapRowDim,
			int numCols, boolean wrapColDim, SquareNeighbourhood neighbourhoodType, FeatureInitializer[] featureInit) {
		KohonenNetworkConfiguration res = new KohonenNetworkConfiguration();
		res.twoD = true;
		res.numCols = numCols;
		res.neighbourhoodType = neighbourhoodType;
		res.numRows = numRows;
		res.wrapColDim = wrapColDim;
		res.wrapRowDim = wrapRowDim;
		res.nbNeurons = numRows * numCols;
		return res;
	}

	public static KohonenNetworkConfiguration getStringNetConfiguration(int num, boolean wrap,
			FeatureInitializer[] featureInit) {
		KohonenNetworkConfiguration res = new KohonenNetworkConfiguration();
		res.twoD = false;
		res.numCols = num;
		res.wrapColDim = wrap;
		res.numRows = num;
		res.wrapRowDim = wrap;
		return res;
	}

}

package net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms;

import org.apache.commons.math3.ml.neuralnet.FeatureInitializer;
import org.apache.commons.math3.ml.neuralnet.SquareNeighbourhood;

import lombok.Getter;

public class HistogramsKohonenNetworkConfiguration {
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

	private HistogramsKohonenNetworkConfiguration() {

	}

	public static HistogramsKohonenNetworkConfiguration getSquareMesh2DConfiguration(int numRows, boolean wrapRowDim,
			int numCols, boolean wrapColDim, SquareNeighbourhood neighbourhoodType, FeatureInitializer[] featureInit) {
		HistogramsKohonenNetworkConfiguration res = new HistogramsKohonenNetworkConfiguration();
		res.twoD = true;
		res.numCols = numCols;
		res.neighbourhoodType = neighbourhoodType;
		res.numRows = numRows;
		res.wrapColDim = wrapColDim;
		res.wrapRowDim = wrapRowDim;
		res.nbNeurons = numRows * numCols;
		return res;
	}

	public static HistogramsKohonenNetworkConfiguration getStringNetConfiguration(int num, boolean wrap,
			FeatureInitializer[] featureInit) {
		HistogramsKohonenNetworkConfiguration res = new HistogramsKohonenNetworkConfiguration();
		res.twoD = false;
		res.numCols = num;
		res.wrapColDim = wrap;
		res.numRows = num;
		res.wrapRowDim = wrap;
		return res;
	}

}

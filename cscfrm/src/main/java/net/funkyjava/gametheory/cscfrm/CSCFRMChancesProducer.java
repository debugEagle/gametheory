package net.funkyjava.gametheory.cscfrm;

public interface CSCFRMChancesProducer {

	int[][] produceChances();

	void endedUsing(int[][] chances);
}

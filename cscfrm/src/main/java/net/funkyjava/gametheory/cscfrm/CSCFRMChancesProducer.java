package net.funkyjava.gametheory.cscfrm;

public interface CSCFRMChancesProducer<Chances extends CSCFRMChances> {

	Chances produceChances();

	void endedUsing(Chances chances);
}

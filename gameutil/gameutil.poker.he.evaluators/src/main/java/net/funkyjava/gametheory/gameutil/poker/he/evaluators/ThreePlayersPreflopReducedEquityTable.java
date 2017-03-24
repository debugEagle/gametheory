package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import lombok.Getter;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

public class ThreePlayersPreflopReducedEquityTable implements Serializable {

	private static final long serialVersionUID = -5919338838662704815L;
	private static final int[] onePlayerGroupsSize = { 2 };
	@Getter
	private final WaughIndexer holeCardsIndexer = new WaughIndexer(onePlayerGroupsSize);
	@Getter
	private final int nbHoleCards = holeCardsIndexer.getIndexSize();

	public static final int heroVilain1Vilain2Index = 0;
	public static final int heroVilain1Index = 1;
	public static final int heroVilain2Index = 2;
	public static final int vilain1Vilain2Index = 3;

	@Getter
	private final double[][][][][] reducedEquities;
	@Getter
	private final int[][][] reducedCounts;

	public ThreePlayersPreflopReducedEquityTable(final ThreePlayersPreflopEquityTables tables) {
		this.reducedEquities = tables.getReducedEquities();
		this.reducedCounts = tables.getReducedCounts();
	}

	public static void main(String[] args) {
		ThreePlayersPreflopReducedEquityTable reduced = null;
		try (final FileInputStream fis = new FileInputStream("/Users/pitt/PokerData/Good3Players.dat");
				final ObjectInputStream ois = new ObjectInputStream(fis)) {
			final ThreePlayersPreflopEquityTables tables = (ThreePlayersPreflopEquityTables) ois.readObject();
			reduced = new ThreePlayersPreflopReducedEquityTable(tables);
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (reduced != null) {
			try (final FileOutputStream fos = new FileOutputStream("/Users/pitt/PokerData/Reduced3Players.dat");
					final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
				oos.writeObject(reduced);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

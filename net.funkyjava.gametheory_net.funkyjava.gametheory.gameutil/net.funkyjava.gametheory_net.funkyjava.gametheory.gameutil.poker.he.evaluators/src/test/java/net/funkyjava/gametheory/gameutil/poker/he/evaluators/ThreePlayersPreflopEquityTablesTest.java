package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Cards52Strings;

@Slf4j
public class ThreePlayersPreflopEquityTablesTest {

	static final Path path = Paths.get("/Users/pitt/HE_3PLAYERS_EQUITY.dat");

	private static final boolean testWrite = false;
	private static final boolean testRead = false;

	@Test
	public void testWriteRead() throws IOException, InterruptedException, ClassNotFoundException {
		if (testWrite) {
			final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path.toFile()));
			final ThreePlayersPreflopEquityTables tables = new ThreePlayersPreflopEquityTables();
			tables.compute();
			oos.writeObject(tables);
			oos.flush();
			oos.close();
		}
		if (testRead) {
			final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path.toFile()));
			final ThreePlayersPreflopEquityTables tables = (ThreePlayersPreflopEquityTables) ois.readObject();
			ois.close();
			String c1 = "Ad";
			String c2 = "Ac";
			String c3 = "Ah";
			String c4 = "Kc";
			String c5 = "Ks";
			String c6 = "Qh";
			Cards52Strings cString = new Cards52Strings(tables.getCardsSpec());
			final int[] heroCards = { cString.getCard(c1), cString.getCard(c2) };
			final int[] vilain1Cards = { cString.getCard(c3), cString.getCard(c4) };
			final int[] vilain2Cards = { cString.getCard(c5), cString.getCard(c6) };
			final double[][] equities = tables.getEquities(heroCards, vilain1Cards, vilain2Cards);
			log.info("0 array is hero / vilain1 / vilain2 equities when no one folds");
			log.info("1 array is hero / vilain1 / vilain2 equities when vilain2 folds");
			log.info("2 array is hero / vilain1 / vilain2 equities when vilain1 folds");
			log.info("3 array is hero / vilain1 / vilain2 equities when hero folds");
			log.info("Equities for \nCards {}{} vs {}{}  vs {}{} \n{}", c1, c2, c3, c4, c5, c6, equities);

			final double[][] reducedEquities = tables.getReducedEquities(heroCards, vilain1Cards, vilain2Cards);
			log.info("Reduced mean\nCards {}{} vs {}{} vs {}{} (= AA vs AKo vs KQo)\n{}", c1, c2, c3, c4,
					reducedEquities);
		}
	}

}

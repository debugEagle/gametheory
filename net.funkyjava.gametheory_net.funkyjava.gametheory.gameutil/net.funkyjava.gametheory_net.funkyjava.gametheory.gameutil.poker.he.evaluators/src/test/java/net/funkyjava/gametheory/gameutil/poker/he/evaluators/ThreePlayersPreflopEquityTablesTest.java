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
			final int[][] winLoseTie2Tie3 = tables.getReducedWinLoseTie2Tie3(heroCards, vilain1Cards, vilain2Cards);
			log.info("For each player Win / Lose / Tie 2 / Tie 3 \nCards {}{} vs {}{}  vs {}{} \n{}", c1, c2, c3, c4,
					c5, c6, winLoseTie2Tie3);

			final int[][] reducedWinLoseTie2Tie3 = tables.getReducedWinLoseTie2Tie3(heroCards, vilain1Cards,
					vilain2Cards);
			log.info(
					"For each player Win / Lose / Tie 2 / Tie 3\nReduced mean\nCards {}{} vs {}{} vs {}{} (= AA vs AKo vs KQo)\n{}",
					c1, c2, c3, c4, reducedWinLoseTie2Tie3);
		}
	}

}

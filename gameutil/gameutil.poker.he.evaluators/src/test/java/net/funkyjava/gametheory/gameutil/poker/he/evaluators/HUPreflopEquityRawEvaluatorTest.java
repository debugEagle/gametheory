package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Cards52Strings;
import net.funkyjava.gametheory.gameutil.cards.DefaultIntCardsSpecs;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.twoplustwo.TwoPlusTwoEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

@Slf4j
public class HUPreflopEquityRawEvaluatorTest {
  @Test
  public void testTimeOnePreflopHand() {
    log.info("" + new WaughIndexer(new int[] {2, 2, 2}).getIndexSize());
    IntCardsSpec specs = new DefaultIntCardsSpecs();
    HUPreflopEquityRawEvaluator eval =
        new HUPreflopEquityRawEvaluator(specs, new TwoPlusTwoEvaluator());
    long start = System.currentTimeMillis();
    String c1 = "Ad";
    String c2 = "Ac";
    String c3 = "Ah";
    String c4 = "Kc";
    Cards52Strings cString = new Cards52Strings(specs);
    int[][] hand =
        {{cString.getCard(c1), cString.getCard(c2)}, {cString.getCard(c3), cString.getCard(c4)}};
    double res = eval.getValue(hand);
    log.info("Equity for one hand {}{} vs {}{} took : {} ms and its equity is {}", c1, c2, c3, c4,
        System.currentTimeMillis() - start, res);
  }
}

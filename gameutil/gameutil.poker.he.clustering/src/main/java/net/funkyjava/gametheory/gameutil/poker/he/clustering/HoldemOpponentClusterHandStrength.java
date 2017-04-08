package net.funkyjava.gametheory.gameutil.poker.he.clustering;

import net.funkyjava.gametheory.gameutil.cards.Cards52SpecTranslator;
import net.funkyjava.gametheory.gameutil.cards.CardsGroupsDrawingTask;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.cards.DefaultIntCardsSpecs;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.cards.indexing.CardsGroupsIndexer;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.AllHoldemHSTables.Streets;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.Holdem7CardsEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

/**
 * 
 * Class to build histograms of hand strength versus opponent clustered hole cards (OCHS) as shown
 * here : http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.295.2143&rep=rep1& type=pdf
 * 
 * @author Pierre Mardon
 *
 */
public class HoldemOpponentClusterHandStrength {

  private HoldemOpponentClusterHandStrength() {}

  public static double[][] opponentClusterHandStrength(final CardsGroupsIndexer preflopIndexer,
      final int[] preflopBuckets, final int nbBuckets, final Streets street,
      final WaughIndexer streetIndexer, final Holdem7CardsEvaluator eval) {
    final IntCardsSpec cardsSpec = DefaultIntCardsSpecs.getDefault();
    final Cards52SpecTranslator translateToEval =
        new Cards52SpecTranslator(cardsSpec, eval.getCardsSpec());
    final Deck52Cards deck = new Deck52Cards(0);
    final int streetIndexSize = streetIndexer.getIndexSize();
    final double[][] res = new double[streetIndexSize][nbBuckets];
    int tmpNbBoardCards = 0;
    switch (street) {
      case FLOP:
        tmpNbBoardCards = 3;
        break;
      case PREFLOP:
        break;
      case RIVER:
        tmpNbBoardCards = 5;
        break;
      case TURN:
        tmpNbBoardCards = 4;
        break;
    }
    final int nbBoardCards = tmpNbBoardCards;
    final int nbMissingBoardCards = 5 - nbBoardCards;
    final int[] heroHoleCards = new int[2];
    final int[] streetBoardCards = new int[nbBoardCards];
    final int[][] streetHeroCards = {heroHoleCards, streetBoardCards};
    final int[] heroCardsEval = new int[7];
    final int[] oppCardsEval = new int[7];
    for (int i = 0; i < streetIndexSize; i++) {
      streetIndexer.unindex(i, streetHeroCards);
      heroCardsEval[0] = translateToEval.translate(heroHoleCards[0]);
      heroCardsEval[1] = translateToEval.translate(heroHoleCards[1]);
      for (int j = 0; j < nbBoardCards; j++) {
        heroCardsEval[2 + j] = translateToEval.translate(streetBoardCards[j]);
        oppCardsEval[2 + j] = translateToEval.translate(streetBoardCards[j]);
      }
      final long[] win = new long[nbBuckets];
      final long[] lose = new long[nbBuckets];
      final long[] tie = new long[nbBuckets];
      if (nbMissingBoardCards > 0) {
        deck.drawAllGroupsCombinations(new int[] {2, nbMissingBoardCards},
            new CardsGroupsDrawingTask() {

              @Override
              public boolean doTask(int[][] cardsGroups) {
                final int[] opHole = cardsGroups[0];
                final int bucket = preflopBuckets[preflopIndexer.indexOf(new int[][] {opHole})];
                oppCardsEval[0] = translateToEval.translate(opHole[0]);
                oppCardsEval[1] = translateToEval.translate(opHole[1]);
                final int[] missingBoard = cardsGroups[1];
                for (int j = 0; j < nbMissingBoardCards; j++) {
                  final int index = j + nbBoardCards;
                  oppCardsEval[index] =
                      heroCardsEval[index] = translateToEval.translate(missingBoard[j]);
                }
                final int heroEval = eval.get7CardsEval(heroCardsEval);
                final int oppEval = eval.get7CardsEval(oppCardsEval);
                if (heroEval < oppEval) {
                  lose[bucket]++;
                } else if (heroEval > oppEval) {
                  win[bucket]++;
                } else {
                  tie[bucket]++;
                }
                return true;
              }
            }, streetHeroCards);
      } else {
        deck.drawAllGroupsCombinations(new int[] {2}, new CardsGroupsDrawingTask() {

          @Override
          public boolean doTask(int[][] cardsGroups) {
            final int[] opHole = cardsGroups[0];
            final int bucket = preflopBuckets[preflopIndexer.indexOf(new int[][] {opHole})];
            oppCardsEval[0] = translateToEval.translate(opHole[0]);
            oppCardsEval[1] = translateToEval.translate(opHole[1]);
            final int heroEval = eval.get7CardsEval(heroCardsEval);
            final int oppEval = eval.get7CardsEval(oppCardsEval);
            if (heroEval < oppEval) {
              lose[bucket]++;
            } else if (heroEval > oppEval) {
              win[bucket]++;
            } else {
              tie[bucket]++;
            }
            return true;
          }
        }, streetHeroCards);
      }
      final double[] streetRes = res[i];
      for (int j = 0; j < nbBuckets; j++) {
        streetRes[j] = (win[j] + tie[j] / 2.0d) / (win[j] + tie[j] + lose[j]);
      }
    }
    return res;
  }
}

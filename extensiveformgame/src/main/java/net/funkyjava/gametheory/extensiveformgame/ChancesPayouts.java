package net.funkyjava.gametheory.extensiveformgame;

public interface ChancesPayouts<Chances> {

  // Implementation should be thread safe
  double[] getPayouts(final Chances chances);
}

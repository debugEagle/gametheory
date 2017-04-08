package net.funkyjava.gametheory.cscfrm;

import java.util.List;

public interface CSCFRMChancesSynchronizer<Chances extends CSCFRMChances> {

  Chances getChances() throws InterruptedException;

  void endUsing(final Chances used) throws InterruptedException;

  void stop();

  void reset();

  List<Runnable> getProducers();
}

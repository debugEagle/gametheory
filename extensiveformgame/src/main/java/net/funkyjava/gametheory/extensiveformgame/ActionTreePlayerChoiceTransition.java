package net.funkyjava.gametheory.extensiveformgame;

public interface ActionTreePlayerChoiceTransition<Id, Chances> {
  ActionTreeNode<Id, Chances> nodeForAction(final int actionIndex);
}

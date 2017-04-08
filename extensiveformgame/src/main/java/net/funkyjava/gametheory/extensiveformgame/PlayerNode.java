package net.funkyjava.gametheory.extensiveformgame;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PlayerNode {

  public final int player;
  public final int round;
  public final int numberOfActions;
}

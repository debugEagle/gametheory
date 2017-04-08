/**
 * 
 */
package net.funkyjava.gametheory.gameutil.poker.bets.rounds.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author Pierre Mardon
 * 
 */
@AllArgsConstructor
public class NoBetPlayerData<PlayerId> {

  @Getter
  @NonNull
  private final PlayerId playerId;

  @Getter
  private final int stack;

  @Getter
  private final boolean inHand;

  public PlayerData<PlayerId> getPlayerData() {
    return new PlayerData<>(playerId, stack, inHand, 0);
  }
}

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
public class PlayerData<PlayerId> {

	@Getter
	@NonNull
	private final PlayerId playerId;

	@Getter
	private final int stack;

	@Getter
	private final boolean inHand;

	@Getter
	private final int bet;
}

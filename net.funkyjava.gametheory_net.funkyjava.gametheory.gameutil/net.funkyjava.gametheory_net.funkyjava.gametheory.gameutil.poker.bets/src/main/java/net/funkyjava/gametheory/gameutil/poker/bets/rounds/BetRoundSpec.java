package net.funkyjava.gametheory.gameutil.poker.bets.rounds;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class BetRoundSpec<PlayerId> {

	@Getter
	private final PlayerId firstPlayerId;
	@Getter
	private final int bigBlindValue;

}

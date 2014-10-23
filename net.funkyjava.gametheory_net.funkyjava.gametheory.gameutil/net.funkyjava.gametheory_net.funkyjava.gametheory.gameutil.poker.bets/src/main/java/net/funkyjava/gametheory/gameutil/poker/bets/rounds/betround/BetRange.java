/**
 * 
 */
package net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.BetRangeSlicer;

/**
 * Abstraction of a bet range to be sliced by a {@link BetRangeSlicer}
 * 
 * @author Pierre Mardon
 * 
 */
@Data
@AllArgsConstructor
public class BetRange {

	private int min;
	private int max;

	/**
	 * Check if this range exists
	 * 
	 * @return true when it exists
	 */
	public boolean exists() {
		return min > 0 && max >= min;
	}

	/**
	 * Get a non-existing bet range
	 * 
	 * @return a non-existing bet range
	 */
	public static BetRange getNoRange() {
		return new BetRange(-1, -1);
	}

	/**
	 * Get a singleton as a bet range
	 * 
	 * @param singleValue
	 *            the singleton value
	 * @return the bet range
	 */
	public static BetRange getSingleton(int singleValue) {
		return new BetRange(singleValue, singleValue);
	}

}

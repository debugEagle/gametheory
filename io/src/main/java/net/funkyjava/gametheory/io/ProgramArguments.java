package net.funkyjava.gametheory.io;

import com.google.common.base.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgramArguments {

	private ProgramArguments() {
	}

	public static Optional<String> getArgument(String[] args, String prefix) {
		for (String arg : args) {
			if (arg.startsWith(prefix)) {
				return Optional.of(arg.substring(prefix.length(), arg.length()));
			}
		}
		log.error("Argument {} not found", prefix);
		return Optional.absent();
	}

	public static Optional<Integer> getIntArgument(String[] args, String prefix) {
		final Optional<String> strOpt = getArgument(args, prefix);
		if (!strOpt.isPresent()) {
			return Optional.absent();
		}
		try {
			final Integer res = Integer.parseInt(strOpt.get());
			return Optional.of(res);
		} catch (NumberFormatException e) {
			log.error("Unable to parse integer arg {}", prefix);
			return Optional.absent();
		}
	}

	public static Optional<Integer> getStrictlyPositiveIntArgument(String[] args, String prefix) {
		final Optional<Integer> intOpt = getIntArgument(args, prefix);
		if (intOpt.isPresent()) {
			if (intOpt.get() <= 0) {
				log.error("Argument {} expected to be strictly positive", prefix);
				return Optional.absent();
			}
		}
		return intOpt;
	}

	public static Optional<Integer> getPositiveIntArgument(String[] args, String prefix) {
		final Optional<Integer> intOpt = getIntArgument(args, prefix);
		if (intOpt.isPresent()) {
			if (intOpt.get() < 0) {
				log.error("Argument {} expected to be positive", prefix);
				return Optional.absent();
			}
		}
		return intOpt;
	}

}

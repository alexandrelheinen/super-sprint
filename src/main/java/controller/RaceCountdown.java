package controller;

/**
 * 3 → 2 → 1 → GO! sequence used before physics start. Progress within each
 * step drives the on-screen pop/fade animation.
 */
public final class RaceCountdown {

	public static final int NUMBER_STEP_MS = 750;
	public static final int GO_STEP_MS = 600;

	private static final String[] LABELS = {"3", "2", "1", "GO!"};
	private static final int[] STEP_DURATIONS_MS = {
			NUMBER_STEP_MS,
			NUMBER_STEP_MS,
			NUMBER_STEP_MS,
			GO_STEP_MS
	};

	private int stepIndex;
	private int elapsedInStepMs;
	private boolean finished;

	public RaceCountdown() {
		stepIndex = 0;
		elapsedInStepMs = 0;
		finished = false;
	}

	/**
	 * Advances the countdown by {@code deltaMs}. Returns {@code false} once the
	 * sequence has completed (after GO!).
	 */
	public boolean advance(int deltaMs) {
		if (finished) {
			return false;
		}
		elapsedInStepMs += Math.max(0, deltaMs);
		while (!finished && elapsedInStepMs >= STEP_DURATIONS_MS[stepIndex]) {
			elapsedInStepMs -= STEP_DURATIONS_MS[stepIndex];
			stepIndex++;
			if (stepIndex >= LABELS.length) {
				finished = true;
				elapsedInStepMs = 0;
				return false;
			}
		}
		return !finished;
	}

	public boolean isFinished() {
		return finished;
	}

	public String label() {
		if (finished) {
			return "";
		}
		return LABELS[stepIndex];
	}

	/** 0 at the start of the current step, approaching 1 at the end. */
	public float progress() {
		if (finished) {
			return 1f;
		}
		int duration = STEP_DURATIONS_MS[stepIndex];
		if (duration <= 0) {
			return 1f;
		}
		return Math.min(1f, elapsedInStepMs / (float) duration);
	}

	public boolean isGoStep() {
		return !finished && stepIndex == LABELS.length - 1;
	}
}

package cufy.concurrent;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A loop to do code just one time.
 *
 * @author LSaferSE
 * @version 1 release (13-Feb-2020)
 * @since 13-Feb-2020
 */
public class Do extends Loop<Consumer<Do>, Object> {
	/**
	 * Construct a new 'do' loop.
	 */
	public Do() {
	}

	/**
	 * Construct a new 'do' loop with the given parameters.
	 *
	 * @param code the first looping code
	 * @throws NullPointerException if the given code is null
	 */
	public Do(Consumer<Do> code) {
		Objects.requireNonNull(code, "code");
		this.append(code);
	}

	@Override
	protected void loop() {
		this.next(null);
	}
}

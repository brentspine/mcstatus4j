package de.brentspine.mcstatus4j.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;

/**
 * Retries an action up to {@code tries} times, raising the <em>last</em> failure if every attempt
 * fails.
 *
 * <p>Mirrors Python mcstatus's {@code @retry} decorator (see {@code _utils/retry.py}), but as a
 * plain functional-style helper rather than a decorator - Java has no decorator syntax, so every
 * call site wraps its body in {@link #call} explicitly instead of annotating a method. This also
 * means the "tries overridable per call" behavior Python gets from a keyword argument is just
 * whatever {@code tries} value the caller passes here directly.
 */
public final class Retry {

  private Retry() {}

  /**
   * Run {@code action} up to {@code tries} times, returning the first successful result.
   *
   * @throws RuntimeException the last exception raised by {@code action}, if every attempt failed.
   *     A thrown checked {@link IOException} is wrapped in {@link UncheckedIOException}; any other
   *     checked exception is wrapped in a plain {@link RuntimeException}.
   */
  public static <T> T call(int tries, Callable<T> action) {
    Exception lastException = null;
    for (int i = 0; i < tries; i++) {
      try {
        return action.call();
      } catch (Exception e) {
        lastException = e;
      }
    }
    throw toRuntimeException(lastException);
  }

  private static RuntimeException toRuntimeException(Exception e) {
    if (e instanceof RuntimeException re) {
      return re;
    }
    if (e instanceof IOException ioe) {
      return new UncheckedIOException(ioe);
    }
    return new RuntimeException(e);
  }
}

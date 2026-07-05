package de.brentspine.mcstatus4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/utils/test_retry.py} (sync cases only). */
class RetryTest {

  @Test
  void succeedsOnSecondTry() {
    int[] attempts = {0};
    int result =
        Retry.call(
            2,
            () -> {
              attempts[0]++;
              if (attempts[0] < 2) {
                throw new ArithmeticException("/ by zero");
              }
              return 5;
            });

    assertEquals(2, attempts[0]);
    assertEquals(5, result);
  }

  @Test
  void raisesLastExceptionNotFirst() {
    int[] attempts = {0};
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Retry.call(
                    2,
                    () -> {
                      attempts[0]++;
                      if (attempts[0] == 1) {
                        throw new java.io.IOException("First error");
                      }
                      throw new RuntimeException("Second error");
                    }));

    assertEquals("Second error", ex.getMessage());
  }

  @Test
  void wrapsCheckedIOExceptionAsUnchecked() {
    java.io.UncheckedIOException ex =
        assertThrows(
            java.io.UncheckedIOException.class,
            () ->
                Retry.call(
                    1,
                    () -> {
                      throw new java.io.IOException("boom");
                    }));
    assertEquals("boom", ex.getCause().getMessage());
  }

  @Test
  void doesNotRetryOnSuccess() {
    int[] attempts = {0};
    int result =
        Retry.call(
            5,
            () -> {
              attempts[0]++;
              return 42;
            });

    assertEquals(1, attempts[0]);
    assertEquals(42, result);
  }
}

// This file is part of IBC.
// For conditions of distribution and use, see copyright notice in COPYING.txt

package ibcalpha.ibc.spi;

/**
 * SPI for code that should run once IBC has reached the LOGGED_IN state
 * (i.e. TWS/Gateway has finished logging in and is fully loaded).
 *
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * Register the fully-qualified implementation class name in a file at
 * {@code META-INF/services/ibcalpha.ibc.spi.PostLoginHook} on the runtime
 * classpath.
 *
 * Hooks are invoked on a dedicated background thread, not the Swing EDT,
 * so they may block. Exceptions thrown by a hook are caught and logged;
 * they will not prevent other hooks from running.
 */
public interface PostLoginHook {
    void onLoginCompleted();
}

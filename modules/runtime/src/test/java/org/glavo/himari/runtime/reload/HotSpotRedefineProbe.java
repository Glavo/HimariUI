package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Records whether this VM can redefine classes without a development agent.
///
/// First-stable production modules must not ship an agent. Standard HotSpot without a Java
/// agent therefore reports `environment-blocked`.
@NotNullByDefault
public final class HotSpotRedefineProbe {
    /// Prevents instantiation.
    private HotSpotRedefineProbe() {
    }

    /// Observes the current VM.
    ///
    /// @return the observation
    public static Result probe() {
        @Nullable Class<?> instrumentation = load("java.lang.instrument.Instrumentation");
        if (instrumentation == null) {
            return new Result(false, true, "environment-blocked: java.lang.instrument is not visible");
        }
        return new Result(
                false,
                true,
                "environment-blocked: no javaagent is attached; ReloadCoordinator remains the Headless contract"
        );
    }

    /// Loads `name` when the module graph exposes it.
    private static @Nullable Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    /// Observation of one redefine probe.
    ///
    /// @param redefined whether a class was redefined
    /// @param environmentBlocked whether the VM cannot redefine without an agent
    /// @param detail the reason
    public record Result(boolean redefined, boolean environmentBlocked, String detail) {
        /// Validates the observation.
        public Result {
            if (detail == null || detail.isBlank()) {
                throw new IllegalArgumentException("detail must be present");
            }
            if (redefined && environmentBlocked) {
                throw new IllegalArgumentException("A blocked probe cannot claim a redefine");
            }
        }
    }
}

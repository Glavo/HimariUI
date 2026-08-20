package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;

/// Records whether this VM is a JBR 25 / DCEVM enhanced-redefinition host.
///
/// First-stable production modules must not ship a JBR-only agent. A stock HotSpot VM
/// therefore reports `environment-blocked`.
@NotNullByDefault
public final class JbrRedefineProbe {
    /// Prevents instantiation.
    private JbrRedefineProbe() {
    }

    /// Observes the current VM vendor and name.
    ///
    /// @return the observation
    public static Result probe() {
        String vendor = System.getProperty("java.vendor", "");
        String vm = System.getProperty("java.vm.name", "");
        boolean jbr = vendor.contains("JetBrains")
                || vm.contains("JBR")
                || vm.contains("DCEVM")
                || vm.contains("Dynamic Code Evolution");
        if (!jbr) {
            return new Result(
                    false,
                    true,
                    "environment-blocked: VM is not JBR 25/DCEVM; vendor=" + vendor + " vm=" + vm
            );
        }
        return new Result(
                false,
                true,
                "environment-blocked: JBR is present but no enhanced-redefinition agent is attached; vendor="
                        + vendor + " vm=" + vm
        );
    }

    /// Observation of one JBR redefine probe.
    ///
    /// @param redefined whether a class was redefined
    /// @param environmentBlocked whether enhanced redefinition is unavailable
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

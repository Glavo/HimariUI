package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Records the execution environment needed to compare benchmark evidence responsibly.
///
/// @param javaRuntimeVersion the Java runtime version string
/// @param vmName the virtual-machine name
/// @param osName the operating-system name
/// @param osArchitecture the operating-system architecture
/// @param availableProcessors the JVM-reported processor count
@NotNullByDefault
public record ComparisonEnvironmentRecord(
        String javaRuntimeVersion,
        String vmName,
        String osName,
        String osArchitecture,
        int availableProcessors
) {
    /// Creates a validated environment record.
    public ComparisonEnvironmentRecord {
        javaRuntimeVersion = ComparisonContracts.requireText(javaRuntimeVersion, "javaRuntimeVersion");
        vmName = ComparisonContracts.requireText(vmName, "vmName");
        osName = ComparisonContracts.requireText(osName, "osName");
        osArchitecture = ComparisonContracts.requireText(osArchitecture, "osArchitecture");
        if (availableProcessors <= 0) {
            throw new IllegalArgumentException("availableProcessors must be positive");
        }
    }

    /// Captures the current process environment without timestamps or machine-specific paths.
    ///
    /// @return the environment record
    public static ComparisonEnvironmentRecord current() {
        return new ComparisonEnvironmentRecord(
                System.getProperty("java.runtime.version"),
                System.getProperty("java.vm.name"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors()
        );
    }
}

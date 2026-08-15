/// Probes the production macOS backend for M0 feasibility.
module org.glavo.himari.spikes.macos {
    requires org.glavo.himari.platform.macos;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.macos;
}

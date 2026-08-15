/// Probes the production Metal backend for M0 feasibility.
module org.glavo.himari.spikes.metal {
    requires org.glavo.himari.rhi.metal;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.metal;
}

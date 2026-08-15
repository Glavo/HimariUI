/// Probes the production Wayland backend for M0 feasibility.
module org.glavo.himari.spikes.wayland {
    requires org.glavo.himari.platform.wayland;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.wayland;
}

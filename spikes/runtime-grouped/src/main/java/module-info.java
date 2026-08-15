/// Defines the explicit grouped-recomposition M1 runtime candidate.
module org.glavo.himari.spikes.runtime.grouped {
    requires transitive org.glavo.himari.spikes.runtime.sample;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.runtime.grouped;
}

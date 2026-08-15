/// Defines the fine-grained binding and small structural-scope M1 runtime candidate.
module org.glavo.himari.spikes.runtime.hybrid {
    requires transitive org.glavo.himari.spikes.runtime.sample;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.runtime.hybrid;
}

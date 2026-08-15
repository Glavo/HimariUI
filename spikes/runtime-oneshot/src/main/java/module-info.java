/// Defines the one-shot signal-owner M1 runtime candidate.
module org.glavo.himari.spikes.runtime.oneshot {
    requires transitive org.glavo.himari.spikes.runtime.sample;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.runtime.oneshot;
}

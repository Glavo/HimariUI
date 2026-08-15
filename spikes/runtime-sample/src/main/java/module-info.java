/// Defines the neutral HimariUI structural-runtime comparison protocol and fixtures.
module org.glavo.himari.spikes.runtime.sample {
    requires jdk.management;
    requires transitive org.glavo.himari.platform.headless;
    requires transitive org.glavo.himari.state;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.runtime.sample;
}

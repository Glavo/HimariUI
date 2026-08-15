/// Defines the HimariUI declarative runtime module.
module org.glavo.himari.runtime {
    requires transitive org.glavo.himari.platform.api;
    requires transitive org.glavo.himari.state;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.runtime;
    exports org.glavo.himari.runtime.animation;
    exports org.glavo.himari.runtime.effect;
    exports org.glavo.himari.runtime.mount;
    exports org.glavo.himari.runtime.sample;
    exports org.glavo.himari.runtime.structure;
    exports org.glavo.himari.runtime.trace;
    exports org.glavo.himari.runtime.transition;
}

/// Defines the deterministic HimariUI Headless platform.
module org.glavo.himari.platform.headless {
    requires transitive org.glavo.himari.platform.api;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.platform.headless;
}

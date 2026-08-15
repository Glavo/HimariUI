/// Defines the V0 Headless CounterApp sample.
module org.glavo.himari.samples.counter {
    requires org.glavo.himari.font;
    requires org.glavo.himari.graphics;
    requires org.glavo.himari.layout;
    requires org.glavo.himari.render.software;
    requires org.glavo.himari.runtime;
    requires org.glavo.himari.text;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.samples.counter;
}

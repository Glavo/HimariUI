/// Exercises the generated Win32 and DXGI bindings for M0 feasibility evidence.
module org.glavo.himari.spikes.windows {
    exports org.glavo.himari.spikes.win32;

    requires java.management;
    requires org.glavo.himari.ffi;
    requires static transitive org.jetbrains.annotations;
}

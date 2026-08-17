package org.glavo.himari.controls;

import org.glavo.himari.platform.api.ImeSession;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.input.gesture.GestureArena;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.glavo.himari.layout.semantics.TextDirection;
import org.glavo.himari.runtime.animation.AnimationMotionDisposition;
import org.glavo.himari.runtime.animation.AnimationTransaction;
import org.glavo.himari.runtime.animation.MotionImportance;
import org.glavo.himari.runtime.animation.MotionPolicy;
import org.glavo.himari.runtime.animation.SnapMotionSpec;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.glavo.himari.runtime.reload.ResourceKind;
import org.glavo.himari.runtime.reload.ResourceReload;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies unstyled button, toggle, slider, scroll, lazy-list, and text-field interactions.
@NotNullByDefault
final class ControlGalleryTest {
    /// Activates the gallery button and toggle through pointer and keyboard.
    @Test
    void activatesButtonAndToggle() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        SemanticsNode button = first(tree, SemanticsRole.BUTTON);
        click(tree, button);
        assertEquals(1, gallery.button().activations());
        assertEquals(1, gallery.externalClicks());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER)));
        assertEquals(2, gallery.button().activations());
        assertEquals(2, gallery.externalClicks());
        gallery.button().setDisabled(true);
        gallery.button().press();
        assertEquals(2, gallery.button().activations());
        click(tree, first(tree, SemanticsRole.BUTTON));
        assertEquals(2, gallery.button().activations());
        assertTrue(first(tree, SemanticsRole.BUTTON).disabled());
        gallery.button().setDisabled(false);
        SemanticsNode toggle = first(tree, SemanticsRole.TOGGLE);
        assertEquals(Boolean.FALSE, toggle.selected());
        click(tree, toggle);
        assertTrue(gallery.toggle().isOn());
        assertEquals(Boolean.TRUE, first(tree, SemanticsRole.TOGGLE).selected());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.SPACE));
        assertFalse(gallery.toggle().isOn());
        gallery.toggle().setOn(true);
        assertTrue(gallery.toggle().isOn());
        assertEquals(Boolean.TRUE, first(tree, SemanticsRole.TOGGLE).selected());
        gallery.toggle().setOn(false);
        assertFalse(gallery.toggle().isOn());
        assertEquals(Boolean.FALSE, first(tree, SemanticsRole.TOGGLE).selected());
        gallery.toggle().setDisabled(true);
        gallery.toggle().setOn(true);
        assertTrue(gallery.toggle().isOn());
        click(tree, first(tree, SemanticsRole.TOGGLE));
        assertTrue(gallery.toggle().isOn());
        assertTrue(first(tree, SemanticsRole.TOGGLE).disabled());
        gallery.toggle().setDisabled(false);
        gallery.toggle().setOn(false);
        gallery.checkbox().setChecked(true);
        assertTrue(gallery.checkbox().isChecked());
        gallery.checkbox().setDisabled(true);
        assertTrue(gallery.checkbox().disabled());
        assertEquals("logo", first(tree, SemanticsRole.IMAGE).label());
        assertEquals("Sketch", first(tree, SemanticsRole.CANVAS).label());
        gallery.image().setDisabled(true);
        gallery.canvas().setDisabled(true);
        assertTrue(gallery.image().disabled());
        assertTrue(gallery.canvas().disabled());
        assertTrue(first(tree, SemanticsRole.IMAGE).disabled());
        assertTrue(first(tree, SemanticsRole.CANVAS).disabled());
        gallery.image().setDisabled(false);
        gallery.canvas().setDisabled(false);
        assertEquals(8.0f, gallery.spacer().size().width());
        assertEquals(48.0f, gallery.canvas().size().width());
    }

    /// Steps the slider through keyboard increment and decrement.
    @Test
    void stepsSliderFromKeyboard() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        SemanticsNode slider = first(tree, SemanticsRole.SLIDER);
        tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                slider.bounds().x() + 1.0f,
                slider.bounds().y() + 1.0f
        ));
        assertEquals(3.0, slider.rangeValue());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT));
        assertEquals(4.0f, gallery.slider().value());
        assertEquals(4.0, first(tree, SemanticsRole.SLIDER).rangeValue());
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_LEFT));
        assertEquals(3.0f, gallery.slider().value());
        gallery.slider().setDisabled(true);
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT));
        assertEquals(3.0f, gallery.slider().value());
        assertTrue(first(tree, SemanticsRole.SLIDER).disabled());
        gallery.slider().setDisabled(false);
    }

    /// Scrolls the viewport and advances the lazy-list window.
    @Test
    void scrollsAndPagesLazyList() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        gallery.scroll().scrollForward();
        assertEquals(16.0f, gallery.scroll().offset());
        gallery.scroll().setDisabled(true);
        gallery.scroll().scrollForward();
        assertEquals(16.0f, gallery.scroll().offset());
        assertTrue(gallery.scroll().disabled());
        gallery.scroll().setDisabled(false);
        SemanticsNode list = first(tree, SemanticsRole.LIST);
        tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                list.bounds().x() + 1.0f,
                list.bounds().y() + 1.0f
        ));
        tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_DOWN));
        assertEquals(1, gallery.list().firstVisible());
        gallery.list().setDisabled(true);
        gallery.list().scrollTo(4);
        gallery.list().insert(0);
        assertEquals(1, gallery.list().firstVisible());
        assertEquals(20, gallery.list().itemCount());
        assertTrue(gallery.list().disabled());
        gallery.list().setDisabled(false);
    }

    /// Materializes only a window of a 100,000-item list and scrolls by index.
    @Test
    void virtualizesOneHundredThousandItems() {
        LazyList list = new LazyList(100_000, 16);
        LayoutTree tree = new LayoutTree();
        LayoutNode root = list.create(new LayoutFactory(tree), "huge");
        tree.setRoot(root);
        tree.measure(Constraints.loose(200.0f, 400.0f));
        tree.place();
        assertEquals(16, root.children().size());
        assertEquals(0, list.firstVisible());
        assertEquals(0, list.overscan());
        LazyList overscanned = new LazyList(100, 4, 2);
        LayoutNode overscanRoot = overscanned.create(new LayoutFactory(tree), "overscan");
        overscanned.scrollTo(10);
        overscanRoot = overscanned.create(new LayoutFactory(tree), "overscan");
        assertEquals(10, overscanned.firstVisible());
        assertEquals(8, overscanned.materializedFirst());
        assertEquals(16, overscanned.materializedLast());
        assertEquals(8, overscanRoot.children().size());
        overscanned.correctHeight(10, 40.0f);
        assertEquals(40.0f, overscanned.heightAt(10));
        assertEquals(20.0f, overscanned.heightAt(11));
        LayoutNode tall = overscanned.create(new LayoutFactory(tree), "tall");
        tree.setRoot(tall);
        tree.measure(Constraints.loose(200.0f, 400.0f));
        tree.place();
        assertEquals(40.0f, tall.children().get(2).size().height());
        assertFalse(overscanned.unmountedLabels().contains("Item 10"));
        assertTrue(overscanned.unmountedLabels().contains("Item 0"));
        list.scrollTo(99_990);
        LayoutNode scrolled = list.create(new LayoutFactory(tree), "huge-end");
        assertEquals(16, scrolled.children().size());
        assertEquals(99_984, list.firstVisible());
        assertEquals("Item 99984", scrolled.children().getFirst().label());
        assertEquals("Item 99999", scrolled.children().getLast().label());
        list.page(-1);
        assertEquals(99_968, list.firstVisible());
        list.page(1);
        assertEquals(99_984, list.firstVisible());
        list.insert(0);
        assertEquals(99_985, list.firstVisible());
        assertEquals(100_001, list.itemCount());
        list.remove(0);
        assertEquals(99_984, list.firstVisible());
        assertEquals(100_000, list.itemCount());
        list.remove(99_990);
        assertEquals(99_983, list.firstVisible());
        assertEquals(99_999, list.itemCount());
    }

    /// Publishes progress range semantics without increment actions.
    @Test
    void publishesProgressRange() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        assertEquals(0.25f, gallery.progress().value());
        SemanticsNode progress = first(tree, SemanticsRole.PROGRESS);
        assertEquals(0.25, progress.rangeValue(), 1.0e-6);
        gallery.progress().setValue(0.8f);
        assertEquals(0.8f, gallery.progress().value());
        assertEquals(0.8f, first(tree, SemanticsRole.PROGRESS).rangeValue(), 1.0e-6);
    }

    /// Exposes labels for unmounted lazy-list items.
    @Test
    void listsUnmountedLazyListLabels() {
        ControlGallery gallery = new ControlGallery();
        assertEquals(20, gallery.list().logicalLabels().size());
        assertEquals(16, gallery.list().unmountedLabels().size());
        assertEquals("Item 4", gallery.list().unmountedLabels().getFirst());
        assertEquals("Item 19", gallery.list().unmountedLabels().getLast());
    }

    /// Materializes the gallery table through shipped TABLE semantics.
    @Test
    void materializesGalleryTable() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
        assertEquals("r0", gallery.table().firstMaterializedKey());
        SemanticsNode table = first(tree, SemanticsRole.TABLE);
        assertTrue(table.bounds().height() > 0.0f);
        gallery.table().insertRow(0, "r-new", 20.0f);
        assertEquals("r0", gallery.table().firstMaterializedKey());
        gallery.table().setDisabled(true);
        gallery.table().insertRow(0, "r-blocked", 20.0f);
        gallery.table().scrollTo(3);
        assertEquals("r0", gallery.table().firstMaterializedKey());
        assertTrue(gallery.table().disabled());
        gallery.table().setDisabled(false);
    }

    /// Commits IME composition into the text field.
    @Test
    void commitsTextFieldComposition() {
        TextField field = new TextField();
        field.updateComposition("ni");
        assertEquals("ni", field.composition());
        assertEquals("ni", field.commitComposition());
        assertEquals("ni", field.text());
        field.updateComposition("hao");
        field.cancelComposition();
        assertEquals("ni", field.text());
        assertEquals(null, field.composition());
        assertTrue(field.undo());
        assertEquals("", field.text());
        assertTrue(field.redo());
        assertEquals("ni", field.text());
        field.setSelection(0, 2);
        field.replaceRange(0, 2, "hao");
        field.replaceRange(0, 3, "hello world");
        field.selectWordAt(1);
        assertEquals(0, field.selectionStart());
        assertEquals(5, field.selectionEnd());
        field.selectLineAt(7);
        assertEquals(0, field.selectionStart());
        assertEquals(11, field.selectionEnd());
        TextArea area = new TextArea();
        area.replaceRange(0, 0, "ab\ncd");
        area.selectWordAt(4);
        assertEquals(3, area.selectionStart());
        assertEquals(5, area.selectionEnd());
        area.selectLineAt(4);
        assertEquals(3, area.selectionStart());
        assertEquals(5, area.selectionEnd());
        field.replaceRange(0, field.text().length(), "e\u0301e");
        field.setSelection(0, 0);
        field.moveCaretByGrapheme(1);
        assertEquals(2, field.caret());
        field.moveCaretByGrapheme(1);
        assertEquals(3, field.caret());
        field.moveCaretByGrapheme(-1);
        assertEquals(2, field.caret());
        field.replaceRange(0, field.text().length(), "hao");
        assertEquals("hao", field.text());
        assertEquals(3, field.caret());
        field.setSelection(3, 3);
        field.moveToLineStart();
        assertEquals(0, field.caret());
        field.moveToLineEnd();
        assertEquals(3, field.caret());
        field.setSelection(3, 3);
        field.deleteBackward();
        assertEquals("ha", field.text());
        field.setSelection(0, 0);
        field.deleteForward();
        assertEquals("a", field.text());
        field.replaceRange(0, field.text().length(), "ab\ncd");
        field.setSelection(4, 4);
        field.moveToLineStart();
        assertEquals(3, field.caret());
        field.moveToLineEnd();
        assertEquals(5, field.caret());
        EditorClipboard clipboard = new EditorClipboard();
        field.setSelection(0, 2);
        field.copy(clipboard);
        assertEquals("ab", clipboard.text());
        assertEquals("<div>ab</div>", clipboard.html());
        assertEquals("{\\rtf1 ab}", clipboard.rtf());
        field.setReadOnly(true);
        field.replaceRange(0, 2, "zz");
        assertEquals("ab\ncd", field.text());
        field.setReadOnly(false);
        field.setDisabled(true);
        field.replaceRange(0, 2, "zz");
        assertEquals("ab\ncd", field.text());
        assertTrue(field.disabled());
        field.setDisabled(false);
        LayoutNode fieldNode = field.create(new LayoutFactory(new LayoutTree()), "hint-field");
        fieldNode.setHint("Type a greeting");
        fieldNode.setReadOnly(true);
        assertEquals("Type a greeting", fieldNode.hint());
        assertTrue(fieldNode.readOnly());
        field.setSelection(3, 5);
        field.cut(clipboard);
        assertEquals("cd", clipboard.text());
        assertEquals("<div>cd</div>", clipboard.html());
        assertEquals("ab\n", field.text());
        field.setSelection(3, 3);
        field.paste(clipboard);
        assertEquals("ab\ncd", field.text());
        field.setPassword(true);
        field.replaceRange(0, field.text().length(), "secret");
        assertEquals("secret", field.text());
        assertEquals("••••••", field.displayedText());
        LayoutNode secretNode = field.create(new LayoutFactory(new LayoutTree()), "secret-field");
        assertEquals("Password", secretNode.label());
        assertTrue(secretNode.password());
        EditorClipboard leak = new EditorClipboard();
        leak.setText("keep");
        leak.setHtml("<div>keep</div>");
        field.setSelection(0, 6);
        field.copy(leak);
        assertEquals("keep", leak.text());
        assertEquals("<div>keep</div>", leak.html());
        field.cut(leak);
        assertEquals("keep", leak.text());
        assertEquals("<div>keep</div>", leak.html());
        assertEquals("secret", field.text());
        field.setPassword(false);
        TextArea areaHome = new TextArea();
        areaHome.replaceRange(0, 0, "xy\nz");
        areaHome.setSelection(4, 4);
        areaHome.moveToLineStart();
        assertEquals(3, areaHome.caret());
        areaHome.deleteBackward();
        assertEquals("xyz", areaHome.text());
        EditorClipboard areaClip = new EditorClipboard();
        areaHome.setSelection(0, 3);
        areaHome.copy(areaClip);
        assertEquals("xyz", areaClip.text());
        assertEquals("<div>xyz</div>", areaClip.html());
        field.replaceRange(0, field.text().length(), "hao");
        field.updateComposition("!");
        assertEquals("!", field.rejectComposition());
        assertEquals("hao", field.text());
        assertEquals("!", field.lastRejected());
        assertEquals(null, field.composition());
        ImeSession ime = new ImeSession();
        ime.setSurroundingText(field.text(), field.caret());
        ime.updateComposition("!");
        field.apply(ime);
        assertEquals("!", field.composition());
        assertEquals("!", ime.commit());
        field.apply(ime);
        assertEquals("hao!", field.text());
        assertEquals(null, field.composition());
    }

    /// Shows and dismisses the in-window popup and reads theme tokens.
    @Test
    void popupAndThemeAreExercisable() {
        ControlGallery gallery = new ControlGallery();
        assertFalse(gallery.popup().isOpen());
        gallery.popup().show();
        assertTrue(gallery.popup().isOpen());
        gallery.popup().dismiss();
        assertFalse(gallery.popup().isOpen());
        gallery.popup().setDisabled(true);
        gallery.popup().show();
        assertFalse(gallery.popup().isOpen());
        assertTrue(gallery.popup().disabled());
        gallery.popup().setDisabled(false);
        gallery.menu().setDisabled(true);
        gallery.menu().show();
        assertFalse(gallery.menu().isOpen());
        assertTrue(gallery.menu().disabled());
        gallery.menu().setDisabled(false);
        gallery.dialog().setDisabled(true);
        gallery.dialog().show();
        assertFalse(gallery.dialog().isOpen());
        assertTrue(gallery.dialog().disabled());
        gallery.dialog().setDisabled(false);
        gallery.tooltip().setDisabled(true);
        gallery.tooltip().show();
        assertFalse(gallery.tooltip().isOpen());
        assertTrue(gallery.tooltip().disabled());
        gallery.tooltip().setDisabled(false);
        assertFalse(gallery.theme().highContrast());
        assertEquals(TextDirection.LTR, gallery.theme().textDirection());
        assertFalse(gallery.theme().reducedMotion());
        assertEquals(0xFF9E9E9E, gallery.theme().disabledArgb());
        assertEquals(0xFF1565C0, gallery.theme().focusArgb());
        assertEquals(0xFFBBDEFB, gallery.theme().selectionArgb());
        assertEquals(0xFFC62828, gallery.theme().errorArgb());
        assertEquals(0xFFE3F2FD, gallery.theme().hoverArgb());
        assertEquals(0xFFE0E0E0, gallery.theme().borderArgb());
        assertEquals(1.0f, gallery.theme().fontScale(), 0.0001f);
        assertEquals(1.0f, gallery.theme().density(), 0.0001f);
        assertTrue(ThemeTokens.highContrastTheme().highContrast());
    }

    /// Installs a published theme generation through [`ControlGallery#applyThemeReload`].
    @Test
    void applyThemeReloadInstallsPublishedTokens() {
        ControlGallery gallery = new ControlGallery();
        ResourceReload reload = new ResourceReload();
        assertFalse(gallery.applyThemeReload(reload, "gallery"));
        reload.publish(
                ResourceKind.THEME,
                "gallery",
                java.lang.foreign.MemorySegment.ofArray(ThemeTokens.highContrastTheme().encode())
        );
        assertTrue(gallery.applyThemeReload(reload, "gallery"));
        assertTrue(gallery.theme().highContrast());
        assertEquals("high-contrast", gallery.theme().name());
        assertEquals(ThemeTokens.highContrastTheme(), ThemeTokens.decode(ThemeTokens.highContrastTheme().encode()));
        ResourceReload styles = new ResourceReload();
        styles.publish(
                ResourceKind.STYLE,
                "gallery",
                java.lang.foreign.MemorySegment.ofArray("reducedMotion=true;rtl=true".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        assertTrue(gallery.applyStyleReload(styles, "gallery"));
        assertTrue(gallery.theme().reducedMotion());
        assertEquals(TextDirection.RTL, gallery.theme().textDirection());
        ResourceReload images = new ResourceReload();
        assertFalse(gallery.applyImageReload(images, "gallery"));
        images.publish(
                ResourceKind.IMAGE,
                "gallery",
                java.lang.foreign.MemorySegment.ofArray("star".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        assertTrue(gallery.applyImageReload(images, "gallery"));
        assertEquals("star", gallery.iconButton().icon());
        assertEquals("star", gallery.image().source());
        ResourceReload fonts = new ResourceReload();
        assertFalse(gallery.applyFontReload(fonts, "gallery"));
        fonts.publish(
                ResourceKind.FONT,
                "gallery",
                java.lang.foreign.MemorySegment.ofArray("HimariSans".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        assertTrue(gallery.applyFontReload(fonts, "gallery"));
        assertEquals("HimariSans", gallery.fontFamily());
    }

    /// Commits IME composition into the multiline text area.
    @Test
    void commitsTextAreaComposition() {
        TextArea area = new TextArea();
        area.updateComposition("hello");
        assertEquals("hello", area.commitComposition());
        area.updateComposition("world");
        assertEquals("world", area.commitComposition());
        assertEquals("hello\nworld", area.text());
        assertTrue(area.undo());
        assertEquals("hello", area.text());
        area.setSelection(0, 5);
        area.replaceRange(0, 5, "hi");
        assertEquals("hi", area.text());
        area.setDisabled(true);
        area.replaceRange(0, 2, "zz");
        assertEquals("hi", area.text());
        assertTrue(area.disabled());
        area.setDisabled(false);
        area.updateComposition("!");
        assertEquals("!", area.rejectComposition());
        assertEquals("hi", area.text());
        assertEquals("!", area.lastRejected());
    }

    /// Publishes a polite live-region announcement through the gallery status.
    @Test
    void announcesPoliteLiveRegion() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        LayoutNode published = gallery.status().create(new LayoutFactory(tree), "status");
        gallery.status().announce("Saved");
        assertEquals("Saved", published.label());
        rebuild(tree, gallery);
        SemanticsNode status = first(tree, SemanticsRole.STATUS);
        assertEquals("Saved", status.label());
        assertEquals(SemanticsLiveRegion.POLITE, status.liveRegion());
        gallery.status().setLiveRegion(SemanticsLiveRegion.ASSERTIVE);
        gallery.status().announce("Alert");
        rebuild(tree, gallery);
        SemanticsNode alert = first(tree, SemanticsRole.STATUS);
        assertEquals("Alert", alert.label());
        assertEquals(SemanticsLiveRegion.ASSERTIVE, alert.liveRegion());
        assertEquals(SemanticsLiveRegion.OFF, first(tree, SemanticsRole.BUTTON).liveRegion());
        gallery.status().setDisabled(true);
        assertTrue(gallery.status().disabled());
        assertTrue(first(tree, SemanticsRole.STATUS).disabled());
        gallery.status().setDisabled(false);
    }

    /// Publishes UTF-16 selection and caret ranges on editor semantics.
    @Test
    void publishesEditorTextRanges() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.field().updateComposition("ni");
        gallery.area().replaceRange(0, 0, "ab");
        gallery.area().setSelection(1, 2);
        rebuild(tree, gallery);
        SemanticsNode field = first(tree, SemanticsRole.TEXT_FIELD);
        SemanticsNode area = first(tree, SemanticsRole.TEXT_AREA);
        assertEquals(new SemanticsTextRange(0, 2, 2), field.textRange());
        assertEquals(new SemanticsTextRange(1, 2, 2), area.textRange());
        assertEquals(null, first(tree, SemanticsRole.BUTTON).textRange());
    }

    /// Packs gallery children to the end when the theme is RTL.
    @Test
    void rtlThemePacksChildrenToEnd() {
        LayoutTree ltr = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        rebuild(ltr, gallery);
        float startX = first(ltr, SemanticsRole.BUTTON).bounds().x();
        gallery.setTheme(ThemeTokens.standard().withTextDirection(TextDirection.RTL));
        LayoutTree rtl = new LayoutTree();
        rebuild(rtl, gallery);
        assertEquals(TextDirection.RTL, gallery.theme().textDirection());
        assertTrue(first(rtl, SemanticsRole.BUTTON).bounds().x() > startX);
    }

    /// Scrolls the viewport when a drag wins the gallery gesture arena.
    @Test
    void dragGestureScrollsViewport() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        rebuild(tree, gallery);
        float before = gallery.scroll().offset();
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.DOWN, 20.0f, 40.0f), 0L);
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.MOVE, 20.0f, 20.0f), 16_000_000L);
        assertTrue(gallery.gestures().dragAccepted());
        assertEquals(before + 20.0f, gallery.scroll().offset());
    }

    /// Announces a long press through the live-region status.
    @Test
    void longPressAnnouncesStatus() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        rebuild(tree, gallery);
        gallery.dispatchPointer(tree, new PointerEvent(PointerEventType.DOWN, 12.0f, 12.0f), 0L);
        gallery.gestures().tick(GestureArena.LONG_PRESS_NANOS);
        gallery.dispatchPointer(
                tree,
                new PointerEvent(PointerEventType.UP, 12.0f, 12.0f),
                GestureArena.LONG_PRESS_NANOS
        );
        assertTrue(gallery.gestures().longPressAccepted());
        assertEquals("Long press", gallery.status().message());
    }

    /// Resolves reduced-motion theme tokens into effective animation specifications.
    @Test
    void reducedMotionThemeTransformsMotionSpecs() {
        ControlGallery gallery = new ControlGallery();
        TweenSpec requested = TweenSpec.linear(1_000_000_000L);
        AnimationTransaction standard = gallery.resolveMotion(1L, requested, MotionImportance.NONESSENTIAL);
        assertSame(requested, standard.effectiveMotion());
        gallery.setTheme(ThemeTokens.standard().withReducedMotion(true));
        AnimationTransaction snapped = gallery.resolveMotion(2L, requested, MotionImportance.NONESSENTIAL);
        assertSame(SnapMotionSpec.INSTANCE, snapped.effectiveMotion());
        assertSame(AnimationMotionDisposition.DISABLED, snapped.motionDisposition());
        AnimationTransaction essential = gallery.resolveMotion(3L, requested, MotionImportance.ESSENTIAL);
        assertTrue(essential.effectiveMotion() instanceof TweenSpec);
        assertEquals(
                MotionPolicy.REDUCED_TWEEN_MAX_NANOS,
                ((TweenSpec) essential.effectiveMotion()).durationNanos()
        );
        assertSame(AnimationMotionDisposition.REDUCED, essential.motionDisposition());
    }

    /// Dismisses an open menu with Escape after the overlay is placed.
    @Test
    void escapeDismissesOpenMenu() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.menu().show();
        rebuild(tree, gallery);
        assertTrue(first(tree, SemanticsRole.MENU).bounds().height() > 0.0f);
        assertTrue(gallery.dispatchKey(tree, new KeyEvent(KeyEventType.DOWN, LogicalKey.ESCAPE)));
        assertFalse(gallery.menu().isOpen());
    }

    /// Dismisses an open dialog when a pointer-down lands outside it.
    @Test
    void outsidePointerDismissesOpenDialog() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.dialog().show();
        rebuild(tree, gallery);
        SemanticsNode dialog = first(tree, SemanticsRole.DIALOG);
        assertTrue(dialog.bounds().height() > 0.0f);
        assertTrue(gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.DOWN,
                dialog.bounds().x() - 4.0f,
                dialog.bounds().y() - 4.0f
        )));
        assertFalse(gallery.dialog().isOpen());
    }

    /// Activates a menu item and dismisses the menu.
    @Test
    void menuItemActivationDismissesMenu() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        gallery.menu().show();
        rebuild(tree, gallery);
        SemanticsNode item = first(tree, SemanticsRole.MENU_ITEM);
        assertTrue(gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.DOWN,
                item.bounds().x() + 1.0f,
                item.bounds().y() + 1.0f
        )));
        assertTrue(gallery.dispatchPointer(tree, new PointerEvent(
                PointerEventType.UP,
                item.bounds().x() + 1.0f,
                item.bounds().y() + 1.0f
        )));
        assertEquals(1, gallery.menu().items().getFirst().activations());
        assertFalse(gallery.menu().isOpen());
    }

    /// Rebuilds and places the gallery tree.
    ///
    /// @param tree the tree
    /// @param gallery the gallery
    private static void rebuild(LayoutTree tree, ControlGallery gallery) {
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 800.0f));
        tree.place();
    }

    /// Returns the first node with the role.
    ///
    /// @param tree the tree
    /// @param role the role
    /// @return the node
    private static SemanticsNode first(LayoutTree tree, SemanticsRole role) {
        for (SemanticsNode node : tree.semantics().nodes()) {
            if (node.role() == role) {
                return node;
            }
        }
        throw new AssertionError("Missing " + role);
    }

    /// Clicks the center of one node.
    ///
    /// @param tree the tree
    /// @param node the target
    private static void click(LayoutTree tree, SemanticsNode node) {
        float x = node.bounds().x() + 1.0f;
        float y = node.bounds().y() + 1.0f;
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, x, y));
        tree.dispatch(new PointerEvent(PointerEventType.UP, x, y));
    }
}

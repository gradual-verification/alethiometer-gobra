package edu.cmu.alethiometer;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import org.jetbrains.annotations.NotNull;
import viper.silicon.logger.MemberSymbExLog;

import java.util.Map;

public class Controller implements EditorMouseListener {
    private final Map<MemberSymbExLog, Method> myMethods;

    public Controller(@NotNull Map<MemberSymbExLog, Method> methods) {
        myMethods = methods;
    }

    public void renderMethods(@NotNull Editor editor) {
        for (final var method : myMethods.values()) {
            method.renderInlays(editor);
        }

        final var gutterProvider = new GutterProvider(editor.getDocument().getLineCount(), myMethods);
        editor.getGutter().registerTextAnnotation(gutterProvider);
    }

    @Override
    public void mouseClicked(@NotNull EditorMouseEvent event) {
        final var editor = event.getEditor();

        if (event.getArea() != EditorMouseEventArea.ANNOTATIONS_AREA) {
            return;
        }

        final var scrollingModel = editor.getScrollingModel();
        final var h = scrollingModel.getHorizontalScrollOffset();
        final var v = scrollingModel.getVerticalScrollOffset();

        U.cleanUpAfterClick(editor);

        // find method line belongs to, set path number to path clicked, and
        // redraw inlays
        final var lineClicked = event.getLogicalPosition().line;
        for (final var method : myMethods.values()) {
            // method should always have TranslatedPosition with end object,
            // except for methods in libraries
            final var pos = method.getPos();
            final var startLine = U.toIJ(pos.line());
            final var endLine = U.toIJ(pos.end().get().line());
            if (startLine <= lineClicked && lineClicked <= endLine) {
                // find out which path was clicked from the x coordinate
                final var f = editor.getColorsScheme().getFont(EditorFontType.BOLD_ITALIC);
                final var fontMetrics = editor.getComponent().getFontMetrics(f);
                final var singleCharWidth = fontMetrics.stringWidth(" ");
                final var lineCount = editor.getDocument().getLineCount();
                final var x = event.getMouseEvent().getX();
                // empirically determined that this is the way the UI is laid out
                final var pathNumber = (x - 8 - (singleCharWidth*U.numberOfChars(lineCount))) / singleCharWidth;
                method.setPathNumber(pathNumber);
            }
        }

        renderMethods(editor);

        // this fixes a bug in the IntelliJ editor, where the editor scrolls
        // away from the caret after the inlays are deleted
        scrollingModel.disableAnimation();
        scrollingModel.scroll(h, v);
        scrollingModel.enableAnimation();
    }
}

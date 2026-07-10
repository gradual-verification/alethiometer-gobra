package edu.cmu.alethiometer;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class InlayRenderer implements EditorCustomElementRenderer {
    private final JBColor myColor;
    private final String myLabel;

    public InlayRenderer(@NotNull JBColor color, @NotNull String label) {
        myColor = color;
        myLabel = label;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        final var editor = inlay.getEditor();
        final var f = editor.getColorsScheme().getFont(EditorFontType.BOLD_ITALIC);
        final var fontMetrics = editor.getComponent().getFontMetrics(f);
        return fontMetrics.stringWidth(myLabel);
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics2D g,
                      @NotNull Rectangle2D r, @NotNull TextAttributes t) {
        final var editor = inlay.getEditor();
        final var y = ((int) r.getY()) + editor.getAscent();
        final var f = editor.getColorsScheme().getFont(EditorFontType.BOLD_ITALIC);
        g.setFont(f);
        g.setColor(myColor);
        g.drawString(myLabel, (int) r.getX(), y);
    }
}

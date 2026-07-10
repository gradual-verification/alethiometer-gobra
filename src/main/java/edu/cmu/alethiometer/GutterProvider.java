package edu.cmu.alethiometer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorFontType;
import org.jetbrains.annotations.Nullable;
import viper.silicon.logger.MemberSymbExLog;
import viper.silver.ast.TranslatedPosition;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GutterProvider implements TextAnnotationGutterProvider {
    private final String[][] myPathDiagram;

    public GutterProvider(int lineCount, Map<MemberSymbExLog, Method> methods) {
        myPathDiagram = new String[lineCount][1];
        for (var x = 0; x < lineCount; x += 1) {
            myPathDiagram[x][0] = " ";
        }
        for (final var method : methods.values()) {
            final var paths = method.getPaths();
            for (var i = 0; i < paths.size(); i += 1) {
                for (final var statement : paths.get(i).statements()) {
                    // only statements with valid positions have been added
                    final var pos = (TranslatedPosition) statement.value().pos();
                    if (i == method.getPathNumber()) {
                        U.grow(myPathDiagram, U.toIJ(pos.line()), i, "\u2593");
                    } else {
                        U.grow(myPathDiagram, U.toIJ(pos.line()), i, "\u2591");
                    }
                }
            }
        }
    }

    @Override
    @Nullable
    public String getLineText(int line, Editor editor) {
        return String.join("", myPathDiagram[line]);
    }

    @Override
    @Nullable
    public String getToolTip(int line, Editor editor) {
        return null;
    }

    @Override
    public EditorFontType getStyle(int line, Editor editor) {
        return EditorFontType.BOLD;
    }

    @Override
    @Nullable
    public ColorKey getColor(int line, Editor editor) {
        return null;
    }

    @Override
    @Nullable
    public Color getBgColor(int line, Editor editor) {
        return null;
    }

    @Override
    public List<AnAction> getPopupActions(final int line, final Editor editor) {
        return new ArrayList<>();
    }

    @Override
    public void gutterClosed() { }

    @Override
    public boolean useMargin() {
        return false;
    }
}

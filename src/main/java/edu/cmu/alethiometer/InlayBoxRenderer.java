package edu.cmu.alethiometer;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import scala.collection.Seq;
import scala.collection.immutable.ListSet;
import scala.jdk.javaapi.CollectionConverters;
import viper.silicon.logger.MemberSymbExLog;
import viper.silicon.state.State;
import viper.silicon.state.terms.Term;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class InlayBoxRenderer implements EditorCustomElementRenderer {
    public static final int MAX_LINE_LENGTH = 80;
    public static final JBColor BG_HEAP_COLOR = new JBColor(0xFFECFF, 0xFFECFF);
    public static final JBColor BG_PC_COLOR = new JBColor(0xECFFFF, 0xECFFFF);
    public static final JBColor HEAP_COLOR = new JBColor(0x800060, 0x800060);
    public static final JBColor PC_COLOR = new JBColor(0x0000C0, 0x0000C0);

    private final String myLabel;
    private final int myLongest;
    private final State myState;
    private final ListSet<Term> myPCs;
    private final ArrayList<StringBuilder> myHeapList;
    private final ArrayList<StringBuilder> myPCsList;

    private static void addToList(ArrayList<StringBuilder> list, Seq<String> strings) {
        for (final var string : CollectionConverters.asJava(strings)) {
            if (list.isEmpty()) {
                list.add(new StringBuilder(string));
            } else if (list.get(list.size() - 1).length() + string.length() < MAX_LINE_LENGTH) {
                list.get(list.size() - 1).append(string);
            } else {
                list.add(new StringBuilder(string));
            }
        }
    }

    private static int getMaxLineLength(ArrayList<StringBuilder> list) {
        int maxLineLength = 0;
        for (final var stringBuilder : list) {
            final var length = stringBuilder.length();
            if (length > maxLineLength) {
                maxLineLength = length;
            }
        }
        return maxLineLength;
    }

    public InlayBoxRenderer(@NotNull String label,
                            int longest,
                            @NotNull State state,
                            @NotNull ListSet<Term> previousPCs,
                            @NotNull ListSet<Term> currentPCs,
                            @NotNull MemberSymbExLog memberSymbExLog) {
        myLabel = label;
        myLongest = longest;
        myState = state;
        myPCs = currentPCs;
        final var chunks = state.h().values().toSeq();
        final var fieldAndPredicateChunks = memberSymbExLog.partitionChunks(chunks);
        final var fieldChunks$ = memberSymbExLog.formatChunks(fieldAndPredicateChunks._1(), state);
        final var predicateChunks$ = memberSymbExLog.formatChunks(fieldAndPredicateChunks._2(), state);
        final var thePCs$ = memberSymbExLog.formatPCs(previousPCs, currentPCs, state);
        myHeapList = new ArrayList<>();
        myPCsList = new ArrayList<>();
        addToList(myHeapList, fieldChunks$);
        addToList(myHeapList, predicateChunks$);
        addToList(myPCsList, thePCs$);
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        final var editor = inlay.getEditor();
        final var f = editor.getColorsScheme().getFont(EditorFontType.BOLD_ITALIC);
        final var fontMetrics = editor.getComponent().getFontMetrics(f);
        return fontMetrics.stringWidth(" ".repeat(myLongest + MAX_LINE_LENGTH));
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        final var lines = myHeapList.size() + myPCsList.size() + 1;
        return lines * inlay.getEditor().getLineHeight();
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics2D g,
                      @NotNull Rectangle2D r, @NotNull TextAttributes t) {
        final var editor = inlay.getEditor();
        final var f = editor.getColorsScheme().getFont(EditorFontType.BOLD_ITALIC);
        final var fontMetrics = editor.getComponent().getFontMetrics(f);
        final var x = ((int) r.getX()) + fontMetrics.stringWidth(" ".repeat(myLongest));
        var y = ((int) r.getY()) + editor.getAscent();
        g.setFont(f);

        final var heapY = y - editor.getAscent();
        g.setColor(BG_HEAP_COLOR);
        g.fillRect(x, heapY, fontMetrics.stringWidth(" ".repeat(getMaxLineLength(myHeapList))), myHeapList.size() * editor.getLineHeight());

        g.setColor(HEAP_COLOR);
        for (final var stringBuilder : myHeapList) {
            g.drawString(stringBuilder.toString(), x, y);
            y += editor.getLineHeight();
        }

        // the correct way to calculate the vertical position is y - ascent
        final var mainY = y - editor.getAscent();
        g.setColor(BG_PC_COLOR);
        g.fillRect(x, mainY, fontMetrics.stringWidth(" ".repeat(getMaxLineLength(myPCsList))), myPCsList.size() * editor.getLineHeight());

        g.setColor(PC_COLOR);
        for (final var stringBuilder : myPCsList) {
            g.drawString(stringBuilder.toString(), x, y);
            y += editor.getLineHeight();
        }

        // draw label
        g.setColor(JBColor.BLACK);
        g.fillRect(x, y - editor.getAscent(), fontMetrics.stringWidth(myLabel), editor.getLineHeight());
        g.setColor(JBColor.WHITE);
        g.drawString(myLabel, x, y);
        y += editor.getLineHeight();

        // draw line
        final var document = editor.getDocument();
        final var offset = inlay.getOffset();
        final var lineLength = fontMetrics.stringWidth(" ".repeat(document.getLineEndOffset(document.getLineNumber(offset)) - offset));
        g.setColor(JBColor.BLACK);
        g.drawLine(lineLength, (int) r.getY() + (int) r.getHeight(), x, (int) r.getY() + (int) r.getHeight());
    }
}

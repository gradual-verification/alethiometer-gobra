package edu.cmu.alethiometer;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import scala.collection.Seq;
import scala.collection.immutable.ListSet;
import scala.collection.immutable.ListSet$;
import scala.jdk.javaapi.CollectionConverters;
import viper.silicon.logger.MemberSymbExLog;
import viper.silicon.logger.records.SymbolicRecord;
import viper.silicon.logger.records.data.*;
import viper.silicon.logger.records.structural.BranchingRecord;
import viper.silicon.state.terms.Term;
import viper.silver.ast.Not;
import viper.silver.ast.TranslatedPosition;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Method {
    // each path is uniquely identified by the set of branches that were taken
    // and the branches that were not taken
    // for convenience, path records also keep track of which statements
    // were encountered in this path
    public record Path(Map<BranchingRecord, Boolean> forks, Set<ExecuteRecord> statements) { }

    private final MemberSymbExLog myMemberSymbExLog;
    private final Seq<SymbolicRecord> myLog;
    private final TranslatedPosition myPos;
    private final int myLongest;
    private final List<Path> myPaths;
    private int myPathNumber;

    public Method(MemberSymbExLog memberSymbExLog, TranslatedPosition pos, int longest) {
        myMemberSymbExLog = memberSymbExLog;
        myLog = memberSymbExLog.log();
        myPos = pos;
        myLongest = longest;
        myPaths = new ArrayList<>();
        traverse(false, myLog, new HashMap<>(), new HashSet<>());
        myPathNumber = 0;
    }

    public TranslatedPosition getPos() { return myPos; }
    public List<Path> getPaths() { return myPaths; }
    public int getPathNumber() { return myPathNumber; }

    public void setPathNumber(int pathNumber) {
        if (0 <= pathNumber && pathNumber < myPaths.size()) {
            myPathNumber = pathNumber;
        }
    }

    public void traverse(boolean ended,
                         Seq<SymbolicRecord> records,
                         Map<BranchingRecord, Boolean> forks,
                         Set<ExecuteRecord> statements) {
        for (final var record : CollectionConverters.asJava(records)) {
            if (record instanceof BranchingRecord b) {
                final var forks0 = new HashMap<>(forks);
                final var forks1 = new HashMap<>(forks);
                forks0.put(b, true);
                traverse(ended, b.getBranches().apply(0), forks0, new HashSet<>(statements));
                forks1.put(b, false);
                traverse(ended, b.getBranches().apply(1), forks1, new HashSet<>(statements));
            } else if (record instanceof EndRecord) {
                ended = true;
                break;
            } else if (record instanceof ErrorRecord) {
                ended = true;
                break;
            } else if (record instanceof LoopOutRecord ignored) {
                ended = true;
                break;
            } else if (record instanceof ExecuteRecord x &&
                    x.value().pos() instanceof TranslatedPosition) {
                statements.add(x);
            }
        }
        if (ended) {
            myPaths.add(new Path(forks, statements));
        }
    }

    public void renderInlays(ListSet<Term> previousPCs,
                             Seq<SymbolicRecord> records,
                             @NotNull Editor editor) {
        final var document = editor.getDocument();
        final var inlayModel = editor.getInlayModel();
        final var markupModel = editor.getMarkupModel();
        for (final var record : CollectionConverters.asJava(records)) {
            if (record instanceof BranchingRecord b &&
                    myPaths.get(myPathNumber).forks.containsKey(b)) {

                if (myPaths.get(myPathNumber).forks.get(b)) {
                    renderInlays(previousPCs, b.getBranches().apply(0), editor);
                } else {
                    renderInlays(previousPCs, b.getBranches().apply(1), editor);
                }

            } else if (record instanceof ConditionalEdgeRecord c &&
                    c.value().pos() instanceof TranslatedPosition pos) {

                final var offset0 = document.getLineStartOffset(U.toIJ(pos.line())) + U.toIJ(pos.column());
                final var end = pos.end().get();
                final var offset1 = document.getLineStartOffset(U.toIJ(end.line())) + U.toIJ(end.column());
                var color = JBColor.GREEN;
                if (c.value() instanceof Not not && not.pos().equals(c.value().pos())) {
                    color = JBColor.LIGHT_GRAY;
                }
                final var attr = new TextAttributes(JBColor.BLACK, color, color, EffectType.BOXED, Font.BOLD);
                markupModel.addRangeHighlighter(offset0, offset1,
                        U.LAYER_CONDITIONAL, attr, HighlighterTargetArea.EXACT_RANGE);

            } else if (record instanceof EndRecord e) {

                final var offset = document.getLineStartOffset(U.toIJ(myPos.end().get().line()));
                final var renderer = new InlayBoxRenderer("end", myLongest, e.state(), previousPCs, e.pcs(), myMemberSymbExLog);
                inlayModel.addBlockElement(offset, false, false, 1, renderer);

            } else if (record instanceof ErrorRecord r &&
                    r.error().pos() instanceof TranslatedPosition pos) {

                final var offset0 = document.getLineStartOffset(U.toIJ(pos.line())) + U.toIJ(pos.column());
                final var end = pos.end().get();
                final var offset1 = document.getLineStartOffset(U.toIJ(end.line())) + U.toIJ(end.column());
                markupModel.addRangeHighlighter(offset0, offset1,
                        U.LAYER_ERROR, U.BAD, HighlighterTargetArea.EXACT_RANGE);
                inlayModel.addAfterLineEndElement(offset0, false,
                        new InlayRenderer(JBColor.RED, r.error().readableMessage()));
                // Need to display state right before error happened as well
                // This state is displayed below the error since the state
                // of the last executed statement is displayed above
                final var renderer = new InlayBoxRenderer("error", myLongest, r.state(), previousPCs, r.pcs(), myMemberSymbExLog);
                inlayModel.addBlockElement(offset0, false, false, 1, renderer);

            } else if (record instanceof ExecuteRecord x &&
                    x.value().pos() instanceof TranslatedPosition pos) {

                final var offset = document.getLineStartOffset(U.toIJ(pos.line()));
                final var renderer = new InlayBoxRenderer(x.id() + ": " + x.value().toString(), myLongest, x.state(), previousPCs, x.pcs(), myMemberSymbExLog);
                inlayModel.addBlockElement(offset, false, true, x.id(), renderer);
                previousPCs = x.pcs();

            } else if (record instanceof LoopInRecord i) {

                final var pos = (TranslatedPosition) myMemberSymbExLog.whileLoops().get(i.value()).get().pos();
                final var offset = document.getLineStartOffset(U.toIJ(pos.line()));
                final var renderer = new InlayBoxRenderer("entering loop", myLongest, i.state(), previousPCs, i.pcs(), myMemberSymbExLog);
                inlayModel.addBlockElement(offset, false, true, 1, renderer);
                previousPCs = i.pcs();

            } else if (record instanceof LoopOutRecord o) {

                final var pos = (TranslatedPosition) myMemberSymbExLog.whileLoops().get(o.value()).get().pos();
                final var offset = document.getLineStartOffset(U.toIJ(pos.end().get().line()));
                final var renderer = new InlayBoxRenderer("between iterations", myLongest, o.state(), previousPCs, o.pcs(), myMemberSymbExLog);
                inlayModel.addBlockElement(offset, false, true, 1, renderer);
                previousPCs = o.pcs();
            }
        }
    }

    public void renderInlays(@NotNull Editor editor) {
        if (!myPaths.isEmpty()) {
            renderInlays(ListSet$.MODULE$.empty(), myLog, editor);
        }
    }
}

package edu.cmu.alethiometer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBColor;
import scala.jdk.javaapi.CollectionConverters;
import viper.gobra.GobraRunner;
import viper.gobra.GobraRunner$;
import org.jetbrains.annotations.NotNull;
import viper.silicon.logger.MemberSymbExLog;
import viper.silicon.logger.SymbExLogger;
import viper.silicon.logger.records.data.MethodRecord;
import viper.silver.ast.TranslatedPosition;

import java.awt.Font;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class U {
    public static final int LAYER_CHECK = 4200;
    public static final int LAYER_CONDITIONAL = 5432;
    public static final int LAYER_ERROR = 5678;
    public static final TextAttributes BAD = new TextAttributes(JBColor.BLACK,
            JBColor.PINK,
            JBColor.PINK,
            EffectType.BOXED,
            Font.BOLD);

    // VIPER/SILICON HAS 1-INDEX LINE COLUMN NUMBERS, BUT INTELLIJ HAS 0-INDEX LINE NUMBERS!!!
    // Silver expressions should always have a TranslatedPosition, but inserted statements
    // will have a NoPosition$
    public static int toIJ(int viperLine) {
        return viperLine - 1;
    }

    public static int numberOfChars(int lineCount) {
        var i = 0;
        do {
            lineCount /= 10;
            i += 1;
        } while (lineCount != 0);
        return i;
    }

    // sets the value at arbitrary index of array, replacing the array with
    // a bigger array if necessary
    public static void grow(@NotNull String[] @NotNull [] array,
                            int row,
                            int column,
                            @NotNull String e) {
        if (column >= array[row].length) {
            final var oldArray = array[row];
            array[row] = Arrays.copyOf(oldArray, column + 1);
            for (var i = oldArray.length; i < array[row].length; i += 1) {
                array[row][i] = " ";
            }
        }
        array[row][column] = e;
    }

    // all heap state and PC inlays are block inlays
    // the dummy inlay and error message inlays are after line end inlays
    public static void cleanUpAfterClick(@NotNull Editor editor) {
        // dispose of all block inlays (displaying heap state and PCs)
        final var document = editor.getDocument();
        final var inlayModel = editor.getInlayModel();
        for (final var inlay : inlayModel.getBlockElementsInRange(0,
                document.getTextLength()-1, InlayBoxRenderer.class)) {
            Disposer.dispose(inlay);
        }
        // then retrieve and dispose of all after line end inlays, EXCEPT
        // dummy inlay
        for (final var inlay : inlayModel.getAfterLineEndElementsInRange(1,
                document.getTextLength()-1, InlayRenderer.class)) {
            Disposer.dispose(inlay);
        }
        // remove all of our highlighters
        final var markupModel = editor.getMarkupModel();
        for (final var highlighter : markupModel.getAllHighlighters()) {
            // find our highlighters, which all have specific layer numbers
            final var layer = highlighter.getLayer();
            if (layer == LAYER_CHECK || layer == LAYER_CONDITIONAL || layer == LAYER_ERROR) {
                markupModel.removeHighlighter(highlighter);
            }
        }
        // reset gutter
        editor.getGutter().closeAllAnnotations();
    }

    public static void reset(@NotNull Editor editor) {
        // retrieve and dispose of dummy inlay, which causes the disposer to
        // remove controller as well
        final var inlayModel = editor.getInlayModel();
        final var _0Or1Inlays = inlayModel.getAfterLineEndElementsInRange(0,
                0,
                InlayRenderer.class);
        if (_0Or1Inlays.size() == 1) { // there must be either 0 or 1 inlays
            Disposer.dispose(_0Or1Inlays.get(0));
        }
        U.cleanUpAfterClick(editor);
    }

    public static @NotNull FileEditorManagerEx resetAndUnsplit(@NotNull Editor editor,
                                                               @NotNull Project project,
                                                               @NotNull EditorWindow window) {
        U.reset(editor);
        final var managerEx = FileEditorManagerEx.getInstanceEx(project);
        // close all temporary files
        for (final var file : managerEx.getOpenFiles()) {
            if (file instanceof LightVirtualFile) {
                managerEx.closeFile(file);
                break;
            }
        }
        window.unsplitAll();
        return managerEx;
    }

    public static void verify(@NotNull Document document, @NotNull Editor editor) {
        // TODO: not sure if this is correct
        ApplicationManager.getApplication().runWriteAction(() ->
                FileDocumentManager.getInstance().saveAllDocuments());
        final var virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            return;
        }
        // regular silicon should be thread-safe now
        // all captured variables are read-only
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // now in a background thread (BGT)
            GobraRunner$.MODULE$.main(new String[]{ "--disablePureFunctsTerminationRequirement", "-i", virtualFile.getPath() });
            ApplicationManager.getApplication().invokeLater(() -> {
                // back in the Event Dispatch Thread (EDT) again
                final var inlayModel = editor.getInlayModel();
                final var markupModel = editor.getMarkupModel();

                /*
                if (result instanceof Main.GVC0ParserError parserError) {
                    final var i = parserError.failure().index();
                    markupModel.addRangeHighlighter(i, i, U.LAYER_ERROR, U.BAD, HighlighterTargetArea.LINES_IN_RANGE);
                    inlayModel.addAfterLineEndElement(0, false,
                            new InlayRenderer(JBColor.RED, "Syntax error"));
                    return;
                } else if (result instanceof Main.GVC0ValidatorError validatorError) {
                    for (final var error : JavaConverters.asJavaIterable(validatorError.errors())) {
                        final int s;
                        final int e;
                        if (error.node() != null) {
                            s = error.node().span().start().index();
                            e = error.node().span().end().index();
                        } else {
                            s = 0;
                            e = 0;
                        }
                        markupModel.addRangeHighlighter(s, e, U.LAYER_ERROR, U.BAD, HighlighterTargetArea.EXACT_RANGE);
                        inlayModel.addAfterLineEndElement(e, false,
                                new InlayRenderer(JBColor.RED, error.message()));
                    }
                    return;
                }
                 */

                final Map<MemberSymbExLog, Method> methods = new HashMap<>();

                for (final var symbExLogger : CollectionConverters.asJava(SymbExLogger.loggers()).keySet()) {
                    if (symbExLogger instanceof MemberSymbExLog memberSymbExLog &&
                            SymbExLogger.m(symbExLogger) instanceof MethodRecord methodRecord &&
                            methodRecord.value().pos() instanceof TranslatedPosition pos) {
                        if (!pos.file().getFileName().endsWith(virtualFile.getName())) {
                            continue;
                        }
                        memberSymbExLog.populateWhileLoops(methodRecord.value().bodyOrAssumeFalse().ss());
                        // find the longest line in method
                        var longest = 0;
                        for (var i = U.toIJ(pos.line()); i <= U.toIJ(pos.end().get().line()); i += 1) {
                            var length = document.getLineEndOffset(i) - document.getLineStartOffset(i);
                            if (longest < length) {
                                longest = length;
                            }
                        }
                        final var method = new Method(memberSymbExLog, pos, longest);
                        methods.put(memberSymbExLog, method);
                    }
                }

                // create new controller and new dummy inlay
                final var controller = new Controller(methods);
                final var r = GobraRunner.verifierErrors().isEmpty() ?
                        new InlayRenderer(JBColor.GREEN, "No verification errors") :
                        new InlayRenderer(JBColor.RED, "Verification has errors");
                final var dummy = inlayModel.addAfterLineEndElement(0, false, r);
                assert dummy != null;
                // addEditorMouseListener takes a 2nd argument with a parentDisposable
                // object, the dummy inlay. When the parentDisposable is disposed of
                // the controller is removed as well
                editor.addEditorMouseListener(controller, dummy);
                controller.renderMethods(editor);
            });
        });
    }
}

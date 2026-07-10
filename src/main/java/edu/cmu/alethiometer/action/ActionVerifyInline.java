package edu.cmu.alethiometer.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.DumbAwareAction;
import edu.cmu.alethiometer.U;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ActionVerifyInline extends DumbAwareAction {
    public ActionVerifyInline() { super(); }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final var editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) { return; }
        final var project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) { return; }
        final var window = event.getData(EditorWindow.DATA_KEY);
        if (window == null) { return; }
        final var managerEx = U.resetAndUnsplit(editor, project, window);
        final var document = editor.getDocument();

        U.verify(document, Objects.requireNonNullElse(managerEx.getSelectedTextEditor(), editor));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        final var project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null);
    }
}

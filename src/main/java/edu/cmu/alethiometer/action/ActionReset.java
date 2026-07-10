package edu.cmu.alethiometer.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.DumbAwareAction;
import edu.cmu.alethiometer.U;
import org.jetbrains.annotations.NotNull;

public class ActionReset extends DumbAwareAction {
    public ActionReset() { super(); }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final var editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) { return; }
        final var project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) { return; }
        final var window = event.getData(EditorWindow.DATA_KEY);
        if (window == null) { return; }
        U.resetAndUnsplit(editor, project, window);
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

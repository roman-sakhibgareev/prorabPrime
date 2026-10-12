package ru.prorabprime.feature.tasks

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.ObjectField
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.TaskDraft
import ru.prorabprime.domain.model.TaskId
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.domain.usecase.DeleteTaskUseCase
import ru.prorabprime.domain.usecase.SaveTaskUseCase
import ru.prorabprime.feature.tasks.resources.Res
import ru.prorabprime.feature.tasks.resources.tasks_delete
import ru.prorabprime.feature.tasks.resources.tasks_delete_message
import ru.prorabprime.feature.tasks.resources.tasks_delete_title
import ru.prorabprime.feature.tasks.resources.tasks_saved
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.UiText
import ru.prorabprime.ui.launchCatching

/** The task form: opening, typing, saving, and asking before a delete. */
internal class TaskEditorHandler(
    private val scope: CoroutineScope,
    private val stateHolder: ITasksStateHolder,
    private val errorHandler: ITasksErrorHandler,
    private val saveTask: SaveTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
) {
    fun onEvent(event: TaskEditorEvent) {
        when (event) {
            TaskEditorEvent.Add -> stateHolder.state.value.let {
                stateHolder.setEditor(TaskEditorUi(day = it.day, objectId = it.objectFilter))
            }

            is TaskEditorEvent.Edit -> open(event.taskId)

            is TaskEditorEvent.TitleChanged -> edit {
                it.copy(title = event.text, errors = (it.errors - ObjectField.TASK_TITLE).toImmutableMap())
            }

            is TaskEditorEvent.DayChanged -> edit {
                it.copy(day = event.day, errors = (it.errors - ObjectField.TASK_DAY).toImmutableMap())
            }

            is TaskEditorEvent.TimeChanged -> edit {
                it.copy(minutes = event.minutes, errors = (it.errors - ObjectField.TASK_TIME).toImmutableMap())
            }

            is TaskEditorEvent.ObjectChanged -> edit { it.copy(objectId = event.objectId) }

            TaskEditorEvent.Save -> save()

            is TaskEditorEvent.Delete -> {
                stateHolder.setEditor(null)
                stateHolder.askToDelete(DELETE_DIALOG, event.taskId)
            }

            TaskEditorEvent.Dismiss -> stateHolder.setEditor(null)
        }
    }

    /** Runs once the user confirmed the delete. */
    fun delete(taskId: String) {
        scope.launchCatching(TAG, onFailure = { errorHandler.onActionFailure(it.asAppError()) }) {
            deleteTask(TaskId(taskId)).onFailure { errorHandler.onActionFailure(it.asAppError()) }
        }
    }

    private fun edit(transform: (TaskEditorUi) -> TaskEditorUi) = stateHolder.editForm(transform)

    private fun open(taskId: String) {
        val state = stateHolder.state.value
        val task = (state.tasks + state.overdue).find { it.id == taskId }?.task ?: return
        stateHolder.setEditor(
            TaskEditorUi(
                taskId = taskId,
                title = task.title,
                day = task.day,
                minutes = task.remindAtMinutes,
                done = task.done,
                objectId = task.objectId?.value,
            ),
        )
    }

    private fun save() {
        val editor = stateHolder.state.value.editor ?: return
        if (editor.isSaving) return
        val draft = TaskDraft(editor.title, editor.day, editor.minutes, editor.done, editor.objectId?.let(::ObjectId))
        edit { it.copy(isSaving = true) }
        scope.launchCatching(TAG, onFailure = { onSaveFailure(it.asAppError()) }) {
            val result = if (editor.taskId == null) {
                saveTask.create(draft)
            } else {
                saveTask.update(TaskId(editor.taskId), draft)
            }
            result
                .onSuccess {
                    stateHolder.setEditor(null)
                    errorHandler.onSaved(UiText.Resource(Res.string.tasks_saved))
                }
                .onFailure { onSaveFailure(it.asAppError()) }
        }
    }

    private suspend fun onSaveFailure(error: AppError) {
        edit { it.copy(isSaving = false) }
        if (error is AppError.Validation) {
            edit { it.copy(errors = error.fieldErrors) }
        } else {
            errorHandler.onActionFailure(error)
        }
    }

    private companion object {
        const val TAG = "TaskEditorHandler"
        val DELETE_DIALOG = DialogModel.Confirmation(
            title = UiText.Resource(Res.string.tasks_delete_title),
            message = UiText.Resource(Res.string.tasks_delete_message),
            confirmLabel = UiText.Resource(Res.string.tasks_delete),
            destructive = true,
        )
    }
}

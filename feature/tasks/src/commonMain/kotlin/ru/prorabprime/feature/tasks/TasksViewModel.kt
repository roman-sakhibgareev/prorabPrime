package ru.prorabprime.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.ObjectQuery
import ru.prorabprime.domain.model.ObjectSort
import ru.prorabprime.domain.model.Task
import ru.prorabprime.domain.model.TaskDraft
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.ui.StateOwner
import ru.prorabprime.ui.launchCatching

internal class TasksViewModel(
    private val stateHolder: ITasksStateHolder,
    private val errorHandler: ITasksErrorHandler,
    private val actions: TasksActions,
) : ViewModel(),
    StateOwner<TasksState> by stateHolder {

    private val editor = TaskEditorHandler(
        viewModelScope,
        stateHolder,
        errorHandler,
        actions.saveTask,
        actions.deleteTask,
    )
    private var loadJob: Job? = null
    private var marksJob: Job? = null
    private var nextJob: Job? = null

    /** The failure last reported, so one outage seen by both flows is said once. */
    private var reportedFailure: AppError? = null

    init {
        load()
    }

    fun onEvent(event: TasksEvent) {
        when (event) {
            TasksEvent.Retry -> retry()
            TasksEvent.PreviousDay -> moveTo(state.value.day.plusDays(-1))
            TasksEvent.NextDay -> moveTo(state.value.day.plusDays(1))
            TasksEvent.TodayClicked -> moveTo(state.value.today)
            TasksEvent.MonthToggled -> toggleMonth()
            is TasksEvent.ObjectFilterPicked -> filterBy(event.objectId)
            TasksEvent.PreviousMonth -> showMonth(state.value.month.previousMonth())
            TasksEvent.NextMonth -> showMonth(state.value.month.nextMonth())
            is TasksEvent.DayPicked -> moveTo(event.day)
            is TasksEvent.DoneToggled -> toggle(event.taskId)
            TasksEvent.DialogConfirmed -> deletePending()
            TasksEvent.DialogDismissed -> stateHolder.dismissDialog()
            is TaskEditorEvent -> editor.onEvent(event)
        }
    }

    /**
     * The tasks of the day on screen and, on today, the open ones left over from earlier days. The
     * repository reloads both after any write, so the screen never asks for a refresh.
     */
    private fun load() {
        loadJob?.cancel()
        val shown = state.value
        watchNextPlan(shown.day)
        val overdue: Flow<Result<ImmutableList<Task>>> =
            if (shown.isToday) actions.observeOverdue(shown.day) else flowOf(Result.success(persistentListOf()))
        val day = actions.observeDay(shown.day).matching(shown.objectFilter)
        loadJob = combine(day, overdue.matching(shown.objectFilter), actions.observeChoices()) { today, late, choices ->
            Triple(today, late, choices)
        }.onEach { (today, late, choices) -> render(shown.day, today, late, choices) }
            .launchIn(viewModelScope)
    }

    /** The nearest later plan, so an empty day can point at it. */
    private fun watchNextPlan(day: LocalDay) {
        nextJob?.cancel()
        nextJob = actions.observeNext(day, state.value.objectFilter?.let(::ObjectId))
            .onEach { result ->
                result.onSuccess { task ->
                    stateHolder.showNextPlan(task?.let { NextPlanUi(it.day, it.title, it.day.epochDay - day.epochDay) })
                }
            }.launchIn(viewModelScope)
    }

    private fun render(
        shownDay: LocalDay,
        day: Result<ImmutableList<Task>>,
        overdue: Result<ImmutableList<Task>>,
        choices: ImmutableList<ObjectChoiceUi>,
    ) {
        val failure = (day.exceptionOrNull() ?: overdue.exceptionOrNull())?.asAppError()
        if (failure == null) {
            reportedFailure = null
            // Where one object's plan is shown, its name on every row would say nothing.
            val titles = if (state.value.isScoped) emptyMap() else choices.associate { it.id to it.title }
            stateHolder.showObjects(choices)
            stateHolder.showTasks(day.getOrThrow().toUi(shownDay, titles), overdue.getOrThrow().toUi(shownDay, titles))
        } else if (failure != reportedFailure) {
            reportedFailure = failure
            viewModelScope.launch { errorHandler.onLoadFailure(failure) }
        }
    }

    private fun moveTo(day: LocalDay) {
        if (day != state.value.day) {
            stateHolder.showDay(day)
            load()
        }
        if (state.value.view == TasksView.MONTH && day.firstOfMonth() != state.value.month) {
            showMonth(day.firstOfMonth())
        }
    }

    /** The plan of one object, or of all; a screen opened for one object keeps to it. */
    private fun filterBy(objectId: String?) {
        if (state.value.isScoped || objectId == state.value.objectFilter) return
        stateHolder.showObjectFilter(objectId)
        load()
        if (state.value.view == TasksView.MONTH) showMonth(state.value.month)
    }

    private fun toggleMonth() {
        if (state.value.view == TasksView.MONTH) {
            marksJob?.cancel()
            stateHolder.showMonth(null)
        } else {
            showMonth(state.value.day.firstOfMonth())
        }
    }

    /** The marks come from the same copy as the list, so a task added under the grid shows up in it. */
    private fun showMonth(first: LocalDay) {
        stateHolder.showMonth(first)
        marksJob?.cancel()
        marksJob = actions.observeRange(first, first.lastOfMonth()).matching(state.value.objectFilter)
            .onEach { result -> result.onSuccess { stateHolder.showMarks(it.toMarks()) } }
            .launchIn(viewModelScope)
    }

    private fun retry() {
        stateHolder.showLoading()
        load()
    }

    /** A tick saves the task with its new state right away; the list reloads with it. */
    private fun toggle(taskId: String) {
        val state = state.value
        val task = (state.tasks + state.overdue).find { it.id == taskId }?.task ?: return
        val draft = TaskDraft(task.title, task.day, task.remindAtMinutes, done = !task.done, objectId = task.objectId)
        launchCatching(onFailure = { errorHandler.onActionFailure(it.asAppError()) }) {
            actions.saveTask.update(task.id, draft).onFailure { errorHandler.onActionFailure(it.asAppError()) }
        }
    }

    private fun deletePending() {
        val id = state.value.pendingDeleteId
        stateHolder.dismissDialog()
        if (id != null) editor.delete(id)
    }
}

/** The objects a plan can be narrowed to or a task assigned to; a failure to read them leaves the list empty. */
private fun TasksActions.observeChoices(): Flow<ImmutableList<ObjectChoiceUi>> =
    observeObjects(ObjectQuery(sort = ObjectSort.ADDRESS_ASC))
        .map { result -> result.getOrNull()?.toChoices() ?: persistentListOf() }

/** Only the tasks of [objectId]; all of them when it is null. */
private fun Flow<Result<ImmutableList<Task>>>.matching(objectId: String?): Flow<Result<ImmutableList<Task>>> =
    if (objectId == null) {
        this
    } else {
        map { result -> result.map { tasks -> tasks.filter { it.objectId?.value == objectId }.toImmutableList() } }
    }

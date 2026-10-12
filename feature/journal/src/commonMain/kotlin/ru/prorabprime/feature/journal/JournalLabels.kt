package ru.prorabprime.feature.journal

import org.jetbrains.compose.resources.StringResource
import ru.prorabprime.domain.model.FieldProblem
import ru.prorabprime.feature.journal.resources.Res
import ru.prorabprime.feature.journal.resources.journal_error_invalid
import ru.prorabprime.feature.journal.resources.journal_error_required
import ru.prorabprime.feature.journal.resources.journal_error_too_long

internal val FieldProblem.message: StringResource
    get() = when (this) {
        FieldProblem.REQUIRED -> Res.string.journal_error_required
        FieldProblem.TOO_LONG -> Res.string.journal_error_too_long
        FieldProblem.INVALID -> Res.string.journal_error_invalid
    }

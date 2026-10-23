package ru.prorabprime.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.prorabprime.domain.model.AnalyticsEvent
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.AttachmentKind
import ru.prorabprime.domain.model.ExtraWorkDraft
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.LocalImageRef
import ru.prorabprime.domain.model.ObjectDraft
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.domain.model.PaymentDraft
import ru.prorabprime.domain.model.PhotoId
import ru.prorabprime.domain.model.TaskDraft
import ru.prorabprime.testing.FakeAnalyticsTracker
import ru.prorabprime.testing.FakeFinanceRepository
import ru.prorabprime.testing.FakeImageCompressor
import ru.prorabprime.testing.FakeObjectsRepository
import ru.prorabprime.testing.FakePhotosRepository
import ru.prorabprime.testing.FakeTasksRepository
import ru.prorabprime.testing.anObjectDetails

/** What the core actions tell the tracker: the event when the write went through, and nothing when it did not. */
class AnalyticsTrackingTest {

    private val tracker = FakeAnalyticsTracker()
    private val objects = FakeObjectsRepository()
    private val finance = FakeFinanceRepository()
    private val tasks = FakeTasksRepository()
    private val photos = FakePhotosRepository()
    private val id = ObjectId("a")
    private val day = LocalDay.of(2026, 9, 25)

    @Test
    fun `a created object says which status it started in`() = runTest {
        CreateObjectUseCase(objects, tracker)(ObjectDraft(address = "Тверская, 5", status = ObjectStatus.PLANNED))

        assertThat(tracker.events).containsExactly(AnalyticsEvent.ObjectCreated(ObjectStatus.PLANNED))
    }

    @Test
    fun `an object that was not created, for a bad draft or a failed write, says nothing`() = runTest {
        val create = CreateObjectUseCase(objects, tracker)
        create(ObjectDraft(address = " "))
        objects.writeError = AppError.Network
        create(ObjectDraft(address = "Тверская, 5"))

        assertThat(tracker.events).isEmpty()
    }

    @Test
    fun `a changed status says the new one, an unchanged one says nothing`() = runTest {
        objects.details.value = mapOf(id to anObjectDetails(id = "a", status = ObjectStatus.PLANNED))
        val change = ChangeObjectStatusUseCase(objects, tracker)

        change(id, ObjectStatus.DONE)
        change(id, ObjectStatus.PLANNED)
        objects.writeError = AppError.Network
        change(id, ObjectStatus.PAUSED)

        assertThat(tracker.events).containsExactly(AnalyticsEvent.ObjectStatusChanged(ObjectStatus.DONE))
    }

    @Test
    fun `a deleted object is told, a failed delete is not`() = runTest {
        val delete = DeleteObjectUseCase(objects, tracker)

        delete(id)
        objects.writeError = AppError.Network
        delete(id)

        assertThat(tracker.events).containsExactly(AnalyticsEvent.ObjectDeleted)
    }

    @Test
    fun `an uploaded picture says whether it was a photo or a receipt`() = runTest {
        val upload = UploadPhotoUseCase(FakeImageCompressor(), photos, tracker)

        upload(id, LocalImageRef("content://1"))
        upload(id, LocalImageRef("content://2"), AttachmentKind.RECEIPT)
        photos.error = AppError.Network
        upload(id, LocalImageRef("content://3"))

        assertThat(tracker.events).containsExactly(
            AnalyticsEvent.PhotoAdded(AttachmentKind.PHOTO),
            AnalyticsEvent.PhotoAdded(AttachmentKind.RECEIPT),
        ).inOrder()
    }

    @Test
    fun `marking a receipt paid or unpaid is told, a refused mark is not`() = runTest {
        val setPaid = SetReceiptPaidUseCase(photos, tracker)

        setPaid(PhotoId("p1"), true)
        setPaid(PhotoId("p1"), false)
        photos.error = AppError.NotFound
        setPaid(PhotoId("p1"), true)

        assertThat(tracker.events).containsExactly(
            AnalyticsEvent.ReceiptPaidChanged(paid = true),
            AnalyticsEvent.ReceiptPaidChanged(paid = false),
        ).inOrder()
    }

    @Test
    fun `a picture that cannot be compressed says nothing`() = runTest {
        val compressor = FakeImageCompressor().apply { error = AppError.Unknown }

        UploadPhotoUseCase(compressor, photos, tracker)(id, LocalImageRef("content://1"))

        assertThat(tracker.events).isEmpty()
    }

    @Test
    fun `a new payment and a new extra work are told, an edit of one is not`() = runTest {
        val savePayment = SavePaymentUseCase(finance, tracker)
        val saveWork = SaveExtraWorkUseCase(finance, tracker)

        savePayment.create(id, PaymentDraft(amountKopecks = 100, paidOn = day))
        saveWork.create(id, ExtraWorkDraft(title = "Штробление", amountKopecks = 500))
        savePayment.create(id, PaymentDraft(amountKopecks = null, paidOn = day))
        finance.writeError = AppError.Network
        saveWork.create(id, ExtraWorkDraft(title = "Ещё", amountKopecks = 500))

        assertThat(tracker.events)
            .containsExactly(AnalyticsEvent.PaymentAdded, AnalyticsEvent.ExtraWorkAdded)
            .inOrder()
    }

    @Test
    fun `a new task says whether it has a reminder, and nothing about what it is called`() = runTest {
        val save = SaveTaskUseCase(tasks, tracker)

        save.create(TaskDraft("Позвонить Ивану", day, 570))
        save.create(TaskDraft("Купить плитку", day, null))
        save.create(TaskDraft("", day, null))

        assertThat(tracker.events).containsExactly(
            AnalyticsEvent.TaskCreated(withReminder = true),
            AnalyticsEvent.TaskCreated(withReminder = false),
        ).inOrder()
        assertThat(tracker.events.flatMap { it.props.values }).containsExactly("yes", "no")
    }

    @Test
    fun `without a tracker the use cases work as they did`() = runTest {
        assertThat(CreateObjectUseCase(objects)(ObjectDraft(address = "Тверская, 5")).isSuccess).isTrue()
    }
}

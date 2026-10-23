package ru.prorabprime.feature.expenses

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import ru.prorabprime.domain.ShareResult
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.ExpenseItem
import ru.prorabprime.domain.model.ExpenseKind
import ru.prorabprime.domain.model.Expenses
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.Money
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.PhotoId
import ru.prorabprime.domain.model.ServerFilePath
import ru.prorabprime.domain.model.asFailure
import ru.prorabprime.domain.usecase.MAX_SHARED_IMAGES
import ru.prorabprime.domain.usecase.ObserveExpensesUseCase
import ru.prorabprime.domain.usecase.ObserveObjectsUseCase
import ru.prorabprime.domain.usecase.SavePaymentUseCase
import ru.prorabprime.domain.usecase.SetReceiptPaidUseCase
import ru.prorabprime.domain.usecase.SetReceiptUseCase
import ru.prorabprime.domain.usecase.ShareImagesUseCase
import ru.prorabprime.domain.usecase.UploadPhotoUseCase
import ru.prorabprime.testing.FakeExpensesRepository
import ru.prorabprime.testing.FakeFinanceRepository
import ru.prorabprime.testing.FakeImageCompressor
import ru.prorabprime.testing.FakeImageSharer
import ru.prorabprime.testing.FakeObjectsRepository
import ru.prorabprime.testing.FakePhotosRepository
import ru.prorabprime.testing.FakeSnackbarNotifier
import ru.prorabprime.testing.MainDispatcherRule

class ExpensesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExpensesRepository()
    private val objects = FakeObjectsRepository()
    private val finance = FakeFinanceRepository()
    private val photos = FakePhotosRepository()
    private val notifier = FakeSnackbarNotifier()
    private val sharer = FakeImageSharer()
    private val today = LocalDay.of(2026, 10, 15)

    private fun viewModel() = ExpensesViewModel(
        ExpensesActions(
            observeExpenses = ObserveExpensesUseCase(repository),
            observeObjects = ObserveObjectsUseCase(objects),
            savePayment = SavePaymentUseCase(finance),
            uploadPhoto = UploadPhotoUseCase(FakeImageCompressor(), photos),
            setReceipt = SetReceiptUseCase(photos),
            setReceiptPaid = SetReceiptPaidUseCase(photos),
            shareImages = ShareImagesUseCase(sharer),
        ),
        notifier,
    ) { today }

    private fun receipt(
        id: String,
        objectId: String,
        day: LocalDay,
        kopecks: Long?,
        paid: Boolean = false,
        path: String? = "/files/$id.jpg",
    ) = ExpenseItem(
        id,
        ExpenseKind.RECEIPT,
        ObjectId(objectId),
        "Объект $objectId",
        day,
        kopecks,
        photoId = PhotoId(id),
        photoPath = path?.let(::ServerFilePath),
        isPaid = paid,
    )

    private fun crew(
        id: String,
        objectId: String,
        day: LocalDay,
        kopecks: Long,
    ) = ExpenseItem(id, ExpenseKind.CREW, ObjectId(objectId), "Объект $objectId", day, kopecks, note = "Аванс")

    private val items = listOf(
        crew("c1", "1", LocalDay.of(2026, 10, 10), 2_100_000),
        receipt("r1", "1", LocalDay.of(2026, 10, 3), 7_900_000),
        receipt("r2", "2", LocalDay.of(2026, 9, 28), 50_000_000, paid = true),
        receipt("r3", "2", LocalDay.of(2026, 9, 1), null, path = null),
    )

    @Test
    fun `it waits for the copy of the data before showing anything`() {
        assertThat(viewModel().state.value.status).isEqualTo(ExpensesStatus.Loading)
    }

    @Test
    fun `the figures, the bars and the rows are made from the expenses`() {
        repository.result.value = Result.success(Expenses(items))

        val state = viewModel().state.value

        assertThat(state.status).isEqualTo(ExpensesStatus.Content)
        assertThat(state.summary.total).isEqualTo(Money.format(60_000_000))
        assertThat(state.summary.thisMonth).isEqualTo(Money.format(10_000_000))
        assertThat(state.summary.materials).isEqualTo(Money.format(57_900_000))
        assertThat(state.summary.crew).isEqualTo(Money.format(2_100_000))
        assertThat(state.summary.withoutAmount).isEqualTo(1)
        assertThat(state.byObject.map { it.label }).containsExactly("Объект 2", "Объект 1").inOrder()
        assertThat(state.byObject.first().fraction).isEqualTo(1f)
        assertThat(state.byMonth.map { it.label }).containsExactly("Октябрь 2026", "Сентябрь 2026").inOrder()
        assertThat(state.rows).hasSize(4)
        assertThat(state.allRows).isEqualTo(4)
    }

    @Test
    fun `a receipt row opens its photo, and shows no sum when there is none`() {
        repository.result.value = Result.success(Expenses(items))

        val rows = viewModel().state.value.rows.associateBy { it.id }

        assertThat(rows.getValue("r1").photoId).isEqualTo("r1")
        assertThat(rows.getValue("r1").amount).isEqualTo(Money.format(7_900_000))
        assertThat(rows.getValue("r3").amount).isNull()
        assertThat(rows.getValue("c1").photoId).isNull()
        assertThat(rows.getValue("c1").isReceipt).isFalse()
        assertThat(rows.getValue("c1").day).isEqualTo("10.10.2026")
    }

    @Test
    fun `the filter narrows the rows and leaves the figures`() {
        repository.result.value = Result.success(Expenses(items))
        val viewModel = viewModel()

        viewModel.onEvent(ExpensesEvent.FilterSelected(ExpenseFilter.CREW))

        val state = viewModel.state.value
        assertThat(state.rows.map { it.id }).containsExactly("c1")
        assertThat(state.summary.total).isEqualTo(Money.format(60_000_000))
        assertThat(state.filter).isEqualTo(ExpenseFilter.CREW)

        viewModel.onEvent(ExpensesEvent.FilterSelected(ExpenseFilter.MATERIALS))
        assertThat(viewModel.state.value.rows.map { it.id }).containsExactly("r1", "r2", "r3")
    }

    @Test
    fun `the status filter makes the paid receipts an archive and the unpaid ones the list of what is owed`() {
        repository.result.value = Result.success(Expenses(items))
        val viewModel = viewModel()

        viewModel.onEvent(ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.UNPAID))
        assertThat(viewModel.state.value.rows.map { it.id }).containsExactly("r1", "r3")

        viewModel.onEvent(ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.PAID))
        assertThat(viewModel.state.value.rows.map { it.id }).containsExactly("r2")

        viewModel.onEvent(ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.ALL))
        assertThat(viewModel.state.value.rows).hasSize(4)
    }

    @Test
    fun `the counts are of all receipts, whatever the filters`() {
        repository.result.value = Result.success(Expenses(items))
        val viewModel = viewModel()

        viewModel.onEvent(ExpensesEvent.FilterSelected(ExpenseFilter.CREW))
        viewModel.onEvent(ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.PAID))

        val state = viewModel.state.value
        assertThat(state.paidCount).isEqualTo(1)
        assertThat(state.unpaidCount).isEqualTo(2)
        assertThat(state.rows).isEmpty()
    }

    @Test
    fun `the kind and the status filters narrow together, and a new reading keeps both`() {
        val viewModel = viewModel()
        repository.result.value = Result.success(Expenses(items))
        viewModel.onEvent(ExpensesEvent.FilterSelected(ExpenseFilter.MATERIALS))
        viewModel.onEvent(ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.UNPAID))

        repository.result.value = Result.success(Expenses(items + receipt("r4", "1", today, 100_000)))

        val state = viewModel.state.value
        assertThat(state.rows.map { it.id }).containsExactly("r4", "r1", "r3")
        assertThat(state.filter).isEqualTo(ExpenseFilter.MATERIALS)
        assertThat(state.paymentFilter).isEqualTo(PaymentStatusFilter.UNPAID)
    }

    @Test
    fun `marking a receipt sets it on the photo, and unmarking it from the archive does the same`() {
        repository.result.value = Result.success(Expenses(items))
        val viewModel = viewModel()

        viewModel.onEvent(ExpensesEvent.ReceiptPaidChanged("r1", paid = true))
        viewModel.onEvent(ExpensesEvent.ReceiptPaidChanged("r2", paid = false))

        assertThat(photos.paid).containsExactly(PhotoId("r1") to true, PhotoId("r2") to false).inOrder()
    }

    @Test
    fun `a refused mark is reported`() {
        repository.result.value = Result.success(Expenses(items))
        photos.error = AppError.NotFound
        val viewModel = viewModel()

        viewModel.onEvent(ExpensesEvent.ReceiptPaidChanged("r1", paid = true))

        assertThat(notifier.errors).hasSize(1)
    }

    @Test
    fun `a new reading keeps the filter`() {
        val viewModel = viewModel()
        repository.result.value = Result.success(Expenses(items))
        viewModel.onEvent(ExpensesEvent.FilterSelected(ExpenseFilter.CREW))

        repository.result.value = Result.success(Expenses(items + crew("c2", "1", today, 100_000)))

        assertThat(viewModel.state.value.rows.map { it.id }).containsExactly("c2", "c1")
    }

    @Test
    fun `nothing spent is said by an empty list of rows`() {
        repository.result.value = Result.success(Expenses(emptyList()))

        assertThat(viewModel().state.value.allRows).isEqualTo(0)
    }

    @Test
    fun `a failure to read is an error`() {
        repository.result.value = AppError.Unknown.asFailure()

        assertThat(viewModel().state.value.status).isInstanceOf(ExpensesStatus.Error::class.java)
    }

    private fun picked(vararg ids: String) = viewModel().also { vm ->
        repository.result.value = Result.success(Expenses(items))
        ids.forEachIndexed { index, id ->
            vm.onEvent(if (index == 0) ExpensesEvent.RowLongPressed(id) else ExpensesEvent.RowToggled(id))
        }
    }

    @Test
    fun `a long press on a receipt with a picture starts picking with it, a tap adds and removes`() {
        val vm = picked("r1")
        assertThat(vm.state.value.selecting).isTrue()
        assertThat(vm.state.value.selected).containsExactly("r1")

        vm.onEvent(ExpensesEvent.RowToggled("r2"))
        assertThat(vm.state.value.selected).containsExactly("r1", "r2")

        vm.onEvent(ExpensesEvent.RowToggled("r1"))
        assertThat(vm.state.value.selected).containsExactly("r2")
        assertThat(vm.state.value.selecting).isTrue()
    }

    @Test
    fun `the select button starts picking with nothing picked, and nothing to pick starts nothing`() {
        val vm = viewModel()
        repository.result.value = Result.success(Expenses(items))

        vm.onEvent(ExpensesEvent.SelectionStarted)
        assertThat(vm.state.value.selecting).isTrue()
        assertThat(vm.state.value.selected).isEmpty()

        vm.onEvent(ExpensesEvent.SelectionClosed)
        repository.result.value = Result.success(Expenses(emptyList()))
        vm.onEvent(ExpensesEvent.SelectionStarted)
        assertThat(vm.state.value.selecting).isFalse()
    }

    @Test
    fun `a payment and a receipt without a picture cannot be picked`() {
        repository.result.value = Result.success(Expenses(items))
        val vm = viewModel()
        val rows = vm.state.value.rows.associateBy { it.id }
        assertThat(rows.getValue("r1").isSelectable).isTrue()
        assertThat(rows.getValue("r3").isSelectable).isFalse()
        assertThat(rows.getValue("c1").isSelectable).isFalse()

        vm.onEvent(ExpensesEvent.RowLongPressed("c1"))
        vm.onEvent(ExpensesEvent.RowLongPressed("r3"))
        assertThat(vm.state.value.selecting).isFalse()

        vm.onEvent(ExpensesEvent.RowLongPressed("r1"))
        vm.onEvent(ExpensesEvent.RowToggled("c1"))
        vm.onEvent(ExpensesEvent.RowToggled("r3"))
        assertThat(vm.state.value.selected).containsExactly("r1")
    }

    @Test
    fun `a tap outside picking picks nothing, and closing leaves picking`() {
        repository.result.value = Result.success(Expenses(items))
        val vm = viewModel()

        vm.onEvent(ExpensesEvent.RowToggled("r1"))
        assertThat(vm.state.value.selecting).isFalse()
        assertThat(vm.state.value.selected).isEmpty()

        vm.onEvent(ExpensesEvent.RowLongPressed("r1"))
        vm.onEvent(ExpensesEvent.SelectionClosed)
        assertThat(vm.state.value.selecting).isFalse()
        assertThat(vm.state.value.selected).isEmpty()
    }

    @Test
    fun `no more than the most a share carries can be picked, and the user is told`() {
        val many = List(MAX_SHARED_IMAGES + 1) { receipt("m$it", "1", today, 100) }
        repository.result.value = Result.success(Expenses(many))
        val vm = viewModel()

        vm.onEvent(ExpensesEvent.RowLongPressed("m0"))
        many.drop(1).forEach { vm.onEvent(ExpensesEvent.RowToggled(it.id)) }

        assertThat(vm.state.value.selected).hasSize(MAX_SHARED_IMAGES)
        assertThat(vm.state.value.selected).doesNotContain("m$MAX_SHARED_IMAGES")
        assertThat(notifier.errors).hasSize(1)
    }

    @Test
    fun `sharing sends the full-size pictures in the order of the list, and clears the picking`() {
        val vm = picked("r2", "r1")

        vm.onEvent(ExpensesEvent.ShareClicked)

        assertThat(sharer.shared).containsExactly(
            listOf(ServerFilePath("/files/r1.jpg"), ServerFilePath("/files/r2.jpg")),
        )
        assertThat(vm.state.value.selecting).isFalse()
        assertThat(vm.state.value.selected).isEmpty()
        assertThat(vm.state.value.isSharing).isFalse()
        assertThat(notifier.errors).isEmpty()
    }

    @Test
    fun `sharing with nothing picked does nothing`() {
        val vm = picked("r1")
        vm.onEvent(ExpensesEvent.RowToggled("r1"))

        vm.onEvent(ExpensesEvent.ShareClicked)

        assertThat(sharer.shared).isEmpty()
    }

    @Test
    fun `a share that cannot be made keeps the picking and says why`() {
        val results = listOf(ShareResult.Failed, ShareResult.Unsupported, ShareResult.NothingToShare)
        results.forEach { result ->
            sharer.result = { result }
            val vm = picked("r1")

            vm.onEvent(ExpensesEvent.ShareClicked)

            assertThat(vm.state.value.selecting).isTrue()
            assertThat(vm.state.value.selected).containsExactly("r1")
            assertThat(vm.state.value.isSharing).isFalse()
        }
        assertThat(notifier.errors).hasSize(results.size)
    }

    @Test
    fun `a filter that hides a picked receipt drops it from the picking`() {
        val vm = picked("r1", "r2")

        vm.onEvent(ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.PAID))

        assertThat(vm.state.value.selected).containsExactly("r2")
        assertThat(vm.state.value.selecting).isTrue()
    }

    @Test
    fun `a new reading keeps the picking`() {
        val vm = picked("r1")

        repository.result.value = Result.success(Expenses(items + crew("c2", "1", today, 100_000)))

        assertThat(vm.state.value.selecting).isTrue()
        assertThat(vm.state.value.selected).containsExactly("r1")
    }

    @Test
    fun `marking the picked receipts sets them all paid, or all unpaid when they already are`() {
        val vm = picked("r1", "r2")

        vm.onEvent(ExpensesEvent.SelectionPaidClicked)

        assertThat(photos.paid).containsExactly(PhotoId("r1") to true, PhotoId("r2") to true)
        assertThat(vm.state.value.selecting).isFalse()

        repository.result.value = Result.success(
            Expenses(
                items.map {
                    if (it.id ==
                        "r1"
                    ) {
                        receipt("r1", "1", LocalDay.of(2026, 10, 3), 7_900_000, true)
                    } else {
                        it
                    }
                },
            ),
        )
        vm.onEvent(ExpensesEvent.RowLongPressed("r1"))
        vm.onEvent(ExpensesEvent.RowToggled("r2"))
        vm.onEvent(ExpensesEvent.SelectionPaidClicked)

        assertThat(photos.paid.takeLast(2)).containsExactly(PhotoId("r1") to false, PhotoId("r2") to false)
    }

    @Test
    fun `a refused bulk mark is reported and keeps the picking`() {
        val vm = picked("r1")
        photos.error = AppError.NotFound

        vm.onEvent(ExpensesEvent.SelectionPaidClicked)

        assertThat(notifier.errors).hasSize(1)
        assertThat(vm.state.value.selecting).isTrue()
    }

    @Test
    fun `months are named in Russian, and a strange one is left as it came`() {
        assertThat(monthLabel("2026-01")).isEqualTo("Январь 2026")
        assertThat(monthLabel("2026-12")).isEqualTo("Декабрь 2026")
        assertThat(monthLabel("2026-13")).isEqualTo("2026-13")
        assertThat(monthLabel("soon")).isEqualTo("soon")
    }
}

package ru.prorabprime.feature.finance

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.prorabprime.domain.model.ExtraWork
import ru.prorabprime.domain.model.Finance
import ru.prorabprime.domain.model.Money
import ru.prorabprime.domain.model.Payment
import ru.prorabprime.domain.model.PaymentPurpose
import ru.prorabprime.domain.model.PaymentRevision
import ru.prorabprime.domain.model.PaymentSide
import ru.prorabprime.domain.model.SideSummary

internal fun Finance.toUi() = FinanceUi(
    clientTotalKopecks = clientTotalKopecks,
    crewTotalKopecks = crewTotalKopecks,
    client = client.toCard(breakdown = breakdown()),
    crew = crew.toCard(),
    clientPayments = payments.filter { it.side == PaymentSide.CLIENT }.map { it.toUi() }.toImmutableList(),
    crewPayments = payments.filter { it.side == PaymentSide.CREW }.map { it.toUi() }.toImmutableList(),
    extraWorks = extraWorks.map { it.toUi() }.toImmutableList(),
    extrasAgreed = Money.format(extrasAgreedKopecks),
    extrasPending = extrasPendingKopecks.takeIf { it > 0 }?.let(Money::format),
)

private fun Finance.breakdown(): BreakdownUi {
    fun received(purpose: PaymentPurpose) = payments
        .filter { it.side == PaymentSide.CLIENT && it.purpose == purpose }
        .sumOf { it.amountKopecks }
    return BreakdownUi(
        contract = clientTotalKopecks?.let(Money::format),
        extras = Money.format(extrasAgreedKopecks),
        receivedContract = Money.format(received(PaymentPurpose.CONTRACT)),
        receivedExtras = Money.format(received(PaymentPurpose.EXTRAS)),
    )
}

private fun SideSummary.toCard(breakdown: BreakdownUi? = null) = SideCardUi(
    agreed = agreedKopecks?.let(Money::format),
    paid = Money.format(paidKopecks),
    remaining = remainingKopecks?.let { Money.format(if (it < 0) -it else it) },
    isOverpaid = (remainingKopecks ?: 0) < 0,
    breakdown = breakdown,
)

private fun Payment.toUi() = PaymentUi(this, Money.format(amountKopecks), paidOn.format())

private fun ExtraWork.toUi() = ExtraWorkUi(this, Money.format(amountKopecks))

internal fun List<PaymentRevision>.toRevisionsUi(): ImmutableList<RevisionUi> = map {
    RevisionUi(it.id, it.action, it.side, Money.format(it.amountKopecks), it.method, it.paidOn.format(), it.note)
}.toImmutableList()

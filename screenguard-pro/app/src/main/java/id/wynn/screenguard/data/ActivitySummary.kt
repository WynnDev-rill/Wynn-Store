package id.wynn.screenguard.data

import java.time.LocalDate

data class ActivitySummary(val from: LocalDate, val until: LocalDate, val requestedDays: Int, val days: List<UsageDay>) {
    val available get() = days.filter { it.available }
    val screenMs get() = available.sumOf { it.totalMs }
    val apps get() = available.flatMap { it.apps.entries }.groupBy({it.key},{it.value}).mapValues {it.value.sum()}.filterValues {it>0}
    val appMs get() = apps.values.sum()
    val unlocks get() = available.sumOf {it.unlocks}
    val partial get() = available.size < requestedDays || available.any {it.estimated}
    companion object {
        fun of(all: List<UsageDay>, until: LocalDate, count: Int): ActivitySummary {
            require(count in listOf(1,7,30))
            val from=until.minusDays(count.toLong()-1)
            return ActivitySummary(from,until,count,all.filter {!it.date.isBefore(from)&&!it.date.isAfter(until)}.sortedBy {it.date})
        }
    }
}

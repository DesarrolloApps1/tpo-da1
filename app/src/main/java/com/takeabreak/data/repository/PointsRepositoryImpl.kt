package com.takeabreak.data.repository

import com.takeabreak.data.local.SessionStore
import com.takeabreak.data.remote.DailyPointsDto
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.data.remote.withServerErrors
import com.takeabreak.domain.repository.PointsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class PointsRepositoryImpl(
    private val api: TakeABreakApi,
    private val session: SessionStore,
) : PointsRepository {

    override val lastKnownBalance: Flow<Int?> = session.lastKnownBalance

    override val reportedDays: Flow<Map<LocalDate, Int>> =
        session.reportedDays.map { days -> days.mapKeys { (day, _) -> LocalDate.parse(day) } }

    override suspend fun report(days: Map<LocalDate, Int>) {
        val byDay = days.mapKeys { (day, _) -> day.toString() }
        val response = withServerErrors {
            api.reportDailyPoints(byDay.map { (day, points) -> DailyPointsDto(day, points) })
        }
        session.savePointsReport(byDay, response.balance)
    }
}

package com.takeabreak.data.repository

import com.takeabreak.data.local.BreakDao
import com.takeabreak.data.mapper.toDomain
import com.takeabreak.data.mapper.toEntity
import com.takeabreak.data.mapper.toEpochMillis
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.repository.BreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class BreakRepositoryImpl(private val dao: BreakDao) : BreakRepository {

    override fun observeSince(day: LocalDate): Flow<List<BreakSession>> =
        dao.observeFrom(day.atStartOfDay().toEpochMillis()).map { list -> list.map { it.toDomain() } }

    override fun observeTotalPoints(): Flow<Int> = dao.observeTotalPoints()

    override suspend fun pointsOn(day: LocalDate): Int =
        dao.pointsBetween(day.atStartOfDay().toEpochMillis(), day.plusDays(1).atStartOfDay().toEpochMillis())

    override suspend fun save(sessions: List<BreakSession>) = dao.upsert(sessions.map { it.toEntity() })
}

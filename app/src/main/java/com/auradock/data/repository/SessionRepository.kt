package com.auradock.data.repository

import com.auradock.data.db.SessionDao
import com.auradock.data.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(private val dao: SessionDao) {

    fun getAllSessions(): Flow<List<SessionEntity>> = dao.getAllSessions()

    suspend fun getSessionById(id: Long): SessionEntity? = dao.getSessionById(id)

    suspend fun insertSession(session: SessionEntity): Long = dao.insertSession(session)

    suspend fun updateSession(session: SessionEntity) = dao.updateSession(session)
}

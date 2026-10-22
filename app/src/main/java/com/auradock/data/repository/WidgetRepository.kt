package com.auradock.data.repository

import com.auradock.data.db.WidgetDao
import com.auradock.data.entity.WidgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(private val dao: WidgetDao) {

    fun getAllWidgets(): Flow<List<WidgetEntity>> = dao.getAllWidgets()

    suspend fun getWidgetById(id: String): WidgetEntity? = dao.getWidgetById(id)

    suspend fun insertWidget(widget: WidgetEntity) = dao.insertWidget(widget)

    suspend fun updateWidget(widget: WidgetEntity) = dao.updateWidget(widget)

    suspend fun deleteWidget(widget: WidgetEntity) = dao.deleteWidget(widget)
}

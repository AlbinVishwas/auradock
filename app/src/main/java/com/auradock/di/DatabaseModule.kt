package com.auradock.di

import android.content.Context
import androidx.room.Room
import com.auradock.data.AuraDockDatabase
import com.auradock.data.MIGRATION_1_2
import com.auradock.data.MIGRATION_2_3
import com.auradock.data.MIGRATION_3_4
import com.auradock.intention.IntentionDao
import com.auradock.sessions.FocusSessionDao
import com.auradock.tasks.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Tells Hilt how to build the database and the DAO, once for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AuraDockDatabase =
        Room.databaseBuilder(context, AuraDockDatabase::class.java, "auradock.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    @Provides
    fun provideTaskDao(db: AuraDockDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideFocusSessionDao(db: AuraDockDatabase): FocusSessionDao = db.focusSessionDao()

    @Provides
    fun provideIntentionDao(db: AuraDockDatabase): IntentionDao = db.intentionDao()
}

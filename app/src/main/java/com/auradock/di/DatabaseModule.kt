package com.auradock.di

import android.content.Context
import androidx.room.Room
import com.auradock.data.AuraDockDatabase
import com.auradock.data.MIGRATION_1_2
import com.auradock.data.MIGRATION_2_3
import com.auradock.data.MIGRATION_3_4
import com.auradock.data.MIGRATION_4_5
import com.auradock.data.MIGRATION_5_6
import com.auradock.data.MIGRATION_6_7
import com.auradock.data.MIGRATION_7_8
import com.auradock.data.DatabaseSplit
import com.auradock.data.LocalDatabase
import com.auradock.analytics.AppEventDao
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

    /** `auradock.db`, the backed-up one. On first use after an update from 0.3.1 or older the usage tables are moved out of it. */
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context, local: LocalDatabase): AuraDockDatabase {
        val db = Room.databaseBuilder(context, AuraDockDatabase::class.java, "auradock.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .build()
        runCatching {
            local.openHelper.writableDatabase // creates auradock_local.db with its tables
            DatabaseSplit.run(db.openHelper.writableDatabase, context.getDatabasePath(LOCAL_DB).path)
        }.onFailure { android.util.Log.w("DatabaseSplit", "split skipped, will retry next start", it) }
        return db
    }

    /** `auradock_local.db`: sessions, checkpoint, intention, event log. Never backed up. */
    @Singleton
    @Provides
    fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase =
        Room.databaseBuilder(context, LocalDatabase::class.java, LOCAL_DB).build()

    private const val LOCAL_DB = "auradock_local.db"

    @Provides
    fun provideTaskDao(db: AuraDockDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideFocusSessionDao(db: LocalDatabase): FocusSessionDao = db.focusSessionDao()

    @Provides
    fun provideAppEventDao(db: LocalDatabase): AppEventDao = db.appEventDao()

    @Provides
    fun provideIntentionDao(db: LocalDatabase): IntentionDao = db.intentionDao()
}

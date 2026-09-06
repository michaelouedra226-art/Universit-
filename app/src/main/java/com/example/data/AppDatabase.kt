package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CourseModule::class,
        SubTask::class,
        ResourceLink::class,
        Deadline::class,
        FreeNote::class,
        StudySession::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun resourceDao(): ResourceDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun noteDao(): NoteDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "geoparcours_uvbf.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.moduleDao().insertAllModules(CurriculumData.initialModules)
                            database.subTaskDao().insertAll(CurriculumData.initialSubTasks)
                            database.settingsDao().saveSettings(UserSettings())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

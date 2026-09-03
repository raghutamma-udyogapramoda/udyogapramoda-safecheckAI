package com.safecheck.android.data.store.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CaseEntity::class, AuditEntity::class, ContactEntity::class], version = 2, exportSchema = false)
abstract class SafeCheckDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun auditDao(): AuditDao
    abstract fun contactDao(): ContactDao
}

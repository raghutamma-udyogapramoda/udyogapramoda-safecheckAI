package com.safecheck.android.data.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY isPrimary DESC, name ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM trusted_contacts ORDER BY isPrimary DESC, name ASC")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT * FROM trusted_contacts WHERE contactId = :contactId LIMIT 1")
    suspend fun get(contactId: String): ContactEntity?

    @Query("SELECT * FROM trusted_contacts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun update(contact: ContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE contactId = :contactId")
    suspend fun delete(contactId: String)

    @Query("UPDATE trusted_contacts SET isPrimary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE trusted_contacts SET isPrimary = 1 WHERE contactId = :contactId")
    suspend fun setPrimary(contactId: String)
}

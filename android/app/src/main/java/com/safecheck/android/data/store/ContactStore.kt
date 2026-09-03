package com.safecheck.android.data.store

import com.safecheck.android.data.store.db.ContactDao
import com.safecheck.android.data.store.db.ContactEntity
import com.safecheck.android.domain.model.TrustedContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Room-backed trusted-contacts store for the Safety Circle (R-7.1.1, P1 architecture).
 * Persists contacts in SQLite. Seeds default demo contacts if database is empty.
 */
class ContactStore(
    private val contactDao: ContactDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

    val contacts: StateFlow<List<TrustedContact>> = contactDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (contactDao.getAll().isEmpty()) {
                val seed = listOf(
                    ContactEntity("c_mom", "Mom", "Parent", "WhatsApp / SMS Link", "+919876543210", true),
                    ContactEntity("c_ravi", "Ravi (Brother)", "Sibling", "SMS Link", "+919812345678", false),
                )
                contactDao.insertAll(seed)
            }
        }
    }

    fun add(name: String, relationship: String, channel: String, phoneNumber: String = "", isPrimary: Boolean = false) {
        val contactId = "c_" + UUID.randomUUID().toString().take(6)
        scope.launch {
            if (isPrimary) {
                contactDao.clearPrimary()
            }
            contactDao.insert(
                ContactEntity(
                    contactId = contactId,
                    name = name,
                    relationship = relationship,
                    verifiedChannel = channel,
                    phoneNumber = phoneNumber,
                    isPrimary = isPrimary,
                )
            )
        }
    }

    fun update(contactId: String, name: String, relationship: String, channel: String, phoneNumber: String, isPrimary: Boolean) {
        scope.launch {
            if (isPrimary) {
                contactDao.clearPrimary()
            }
            contactDao.update(
                ContactEntity(
                    contactId = contactId,
                    name = name,
                    relationship = relationship,
                    verifiedChannel = channel,
                    phoneNumber = phoneNumber,
                    isPrimary = isPrimary,
                )
            )
        }
    }

    fun delete(contactId: String) {
        scope.launch {
            contactDao.delete(contactId)
        }
    }

    fun setPrimary(contactId: String) {
        scope.launch {
            contactDao.clearPrimary()
            contactDao.setPrimary(contactId)
        }
    }

    suspend fun get(contactId: String): TrustedContact? = contactDao.get(contactId)?.toDomain()

    suspend fun getPrimary(): TrustedContact? = contactDao.getPrimary()?.toDomain()
}

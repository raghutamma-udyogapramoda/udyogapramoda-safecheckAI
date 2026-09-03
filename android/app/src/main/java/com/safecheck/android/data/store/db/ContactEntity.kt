package com.safecheck.android.data.store.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.safecheck.android.domain.model.TrustedContact

@Entity(tableName = "trusted_contacts")
data class ContactEntity(
    @PrimaryKey val contactId: String,
    val name: String,
    val relationship: String,
    val verifiedChannel: String,
    val phoneNumber: String = "",
    val isPrimary: Boolean = false,
) {
    fun toDomain() = TrustedContact(
        contactId = contactId,
        name = name,
        relationship = relationship,
        verifiedChannel = verifiedChannel,
        phoneNumber = phoneNumber,
        isPrimary = isPrimary,
    )

    companion object {
        fun fromDomain(c: TrustedContact) = ContactEntity(
            contactId = c.contactId,
            name = c.name,
            relationship = c.relationship,
            verifiedChannel = c.verifiedChannel,
            phoneNumber = c.phoneNumber,
            isPrimary = c.isPrimary,
        )
    }
}

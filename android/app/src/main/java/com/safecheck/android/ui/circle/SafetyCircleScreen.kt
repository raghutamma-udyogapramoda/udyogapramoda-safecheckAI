package com.safecheck.android.ui.circle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safecheck.android.domain.model.ReviewDecision
import com.safecheck.android.domain.model.TrustedContact
import com.safecheck.android.ui.components.BannerKind
import com.safecheck.android.ui.components.SafeCheckButton
import com.safecheck.android.ui.components.SafeCheckOutlinedButton
import com.safecheck.android.ui.components.StatusBanner
import com.safecheck.android.ui.theme.NeutralMuted
import com.safecheck.android.ui.theme.RiskLowContainer
import com.safecheck.android.ui.theme.RiskMediumContainer

/**
 * Safety Circle screen (R-7). Serves as both:
 * 1) A Standalone Trusted Contacts Hub (Add/Edit/Delete/Primary contacts)
 * 2) Case Advisory Consultation (generates 2-hr signed summary link, simulates advisory opinion)
 */
@Composable
fun SafetyCircleScreen(
    state: SafetyCircleUiState,
    onShare: (TrustedContact) -> Unit,
    onSimulateAdvisory: (TrustedContact) -> Unit,
    onSimulateNoResponse: () -> Unit,
    onAddContact: (name: String, rel: String, channel: String, phone: String, isPrimary: Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateContact: (id: String, name: String, rel: String, channel: String, phone: String, isPrimary: Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteContact: (id: String) -> Unit = {},
    onSetPrimaryContact: (id: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<TrustedContact?>(null) }
    var contactToDelete by remember { mutableStateOf<TrustedContact?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Safety Circle", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Your trusted circle of family or mentors. When you encounter high-risk or uncertain situations, " +
                "they review a sanitized summary — never your OTPs, PINs, or private chat logs.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )

        Spacer(Modifier.height(16.dp))

        // --- CASE CONSULTATION SECTION (Visible when opened from a specific case) ---
        if (state.case != null) {
            StatusBanner(
                message = "Consulting on Case: ${state.case.title} (Risk Score: ${state.case.score}/100)",
                kind = BannerKind.INFO,
            )

            Spacer(Modifier.height(12.dp))
            Text("Sanitized Summary (What your contact sees)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Text(state.sanitizedSummary, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            when {
                state.sharing -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Generating secure 2-hour review link…", modifier = Modifier.padding(start = 12.dp))
                }
                state.awaitingResponse -> AwaitingResponse(
                    reviewLink = state.reviewLink,
                    expires = state.expiresInMinutes,
                    onSimulateAdvisory = { state.contacts.firstOrNull()?.let(onSimulateAdvisory) },
                    onSimulateNoResponse = onSimulateNoResponse,
                )
                state.advisory != null -> AdvisoryCard(state)
                state.noResponse -> StatusBanner(
                    "No response yet. Machine risk score (${state.case.score}/100) remains active — follow the safe recommendation.",
                    kind = BannerKind.WARNING,
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // --- TRUSTED CONTACTS MANAGEMENT (Always accessible) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Trusted Contacts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Text(
            "Contacts with opt-in verification. The primary contact is prioritized for 1-tap alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = NeutralMuted,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(12.dp))

        if (state.contacts.isEmpty()) {
            Text(
                "No trusted contacts added yet. Tap 'Add' to add your primary safety contact.",
                style = MaterialTheme.typography.bodyMedium,
                color = NeutralMuted,
            )
        } else {
            state.contacts.forEach { contact ->
                ContactCard(
                    contact = contact,
                    isCaseActive = state.case != null,
                    onShare = { onShare(contact) },
                    onEdit = { contactToEdit = contact },
                    onDelete = { contactToDelete = contact },
                    onSetPrimary = { onSetPrimaryContact(contact.contactId) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // --- ADD / EDIT DIALOG ---
    if (showAddDialog || contactToEdit != null) {
        val editing = contactToEdit
        ContactEditDialog(
            initialContact = editing,
            onDismiss = {
                showAddDialog = false
                contactToEdit = null
            },
            onSave = { name, rel, channel, phone, isPrimary ->
                if (editing != null) {
                    onUpdateContact(editing.contactId, name, rel, channel, phone, isPrimary)
                } else {
                    onAddContact(name, rel, channel, phone, isPrimary)
                }
                showAddDialog = false
                contactToEdit = null
            }
        )
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (contactToDelete != null) {
        val target = contactToDelete!!
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Remove Contact") },
            text = { Text("Remove ${target.name} from your Safety Circle? They will no longer receive advisory alerts.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteContact(target.contactId)
                    contactToDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactCard(
    contact: TrustedContact,
    isCaseActive: Boolean,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (contact.isPrimary) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Primary Contact",
                            tint = Color(0xFFFDD663),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    "${contact.relationship} · ${contact.verifiedChannel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMuted,
                )
                if (contact.phoneNumber.isNotBlank()) {
                    Text(
                        "Phone: ${contact.phoneNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeutralMuted,
                    )
                }
            }

            if (isCaseActive) {
                OutlinedButton(onClick = onShare) {
                    Text("Send Case")
                }
            } else {
                IconButton(onClick = onSetPrimary) {
                    Icon(
                        if (contact.isPrimary) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Set Primary",
                        tint = if (contact.isPrimary) Color(0xFFFDD663) else NeutralMuted,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = NeutralMuted)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ContactEditDialog(
    initialContact: TrustedContact?,
    onDismiss: () -> Unit,
    onSave: (name: String, rel: String, channel: String, phone: String, isPrimary: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialContact?.name.orEmpty()) }
    var relationship by remember { mutableStateOf(initialContact?.relationship.orEmpty()) }
    var channel by remember { mutableStateOf(initialContact?.verifiedChannel ?: "WhatsApp / SMS Link") }
    var phone by remember { mutableStateOf(initialContact?.phoneNumber.orEmpty()) }
    var isPrimary by remember { mutableStateOf(initialContact?.isPrimary ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialContact == null) "Add Trusted Contact" else "Edit Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g., Parent, Sibling, Child)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (e.g. +91 98765 43210)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = channel,
                    onValueChange = { channel = it },
                    label = { Text("Verification Channel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), relationship.trim(), channel.trim(), phone.trim(), isPrimary)
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AwaitingResponse(
    reviewLink: String?,
    expires: Int,
    onSimulateAdvisory: () -> Unit,
    onSimulateNoResponse: () -> Unit,
) {
    Column {
        StatusBanner(
            "Sent a secure, signed review link (expires in $expires minutes).",
            kind = BannerKind.INFO,
        )
        if (reviewLink != null) {
            Text(reviewLink, style = MaterialTheme.typography.labelMedium, color = NeutralMuted, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Demo controls (simulated response):", style = MaterialTheme.typography.labelMedium, color = NeutralMuted)
        Spacer(Modifier.height(6.dp))
        SafeCheckButton("Simulate contact reply", onClick = onSimulateAdvisory)
        Spacer(Modifier.height(8.dp))
        SafeCheckOutlinedButton("Simulate no response", onClick = onSimulateNoResponse)
    }
}

@Composable
private fun AdvisoryCard(state: SafetyCircleUiState) {
    val advisory = state.advisory ?: return
    val bg = when (advisory.decision) {
        ReviewDecision.LOOKS_SAFE -> RiskLowContainer
        else -> RiskMediumContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${advisory.reviewerName}'s opinion" + if (advisory.simulated) " (advisory)" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(advisory.note, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                "Advisory feedback helps the user make a safe decision. The deterministic machine score of " +
                    "${state.case?.score}/100 remains unchanged.",
                style = MaterialTheme.typography.labelMedium,
                color = NeutralMuted,
            )
        }
    }
}


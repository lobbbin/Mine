package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.IntakeFormData

@Composable
fun IntakeFormDialog(
    initialData: IntakeFormData? = null,
    onDismiss: () -> Unit,
    onFinalize: (IntakeFormData) -> Unit,
    onSuggestAiAutofill: (String, (IntakeFormData) -> Unit) -> Unit,
    onSyncBedside: ((IntakeFormData) -> Unit) -> Unit
) {
    var formData by remember { mutableStateOf(initialData ?: IntakeFormData()) }
    var rawMemoText by remember { mutableStateOf("") }
    var isOperatingAi by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Pane
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Automated Intake",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Patient Registration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI assisted intake & clinical alignment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Inner Context
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // --- SECTION 1: AI COGNITIVE AUTOMATION PANEL ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Registration Co-Pilot",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "Scribble down raw notes or click Sync to extract clinical registration profiles automatically using Gemini's contextual memory.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = rawMemoText,
                                    onValueChange = { rawMemoText = it },
                                    label = { Text("EHR Dictation Memo / Quick Notes") },
                                    placeholder = { Text("Example: Patient Gidley Thompson, male born in 1961. Living in Sector 4. On Elysium Elite medical aid scheme, allergic to NSAIDs.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (isOperatingAi) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isOperatingAi = true
                                                onSuggestAiAutofill(rawMemoText) { parsedData ->
                                                    formData = parsedData
                                                    isOperatingAi = false
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Parse Memo", style = MaterialTheme.typography.bodySmall)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                isOperatingAi = true
                                                onSyncBedside { parsedData ->
                                                    formData = parsedData
                                                    isOperatingAi = false
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sync Bedside", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- SECTION 2: PERSONAL IDENTITY DETAILS ---
                    item {
                        Text(
                            text = "👤 Personal Demographics",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = formData.firstName,
                                onValueChange = { formData = formData.copy(firstName = it) },
                                label = { Text("First Name(s)") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = formData.surname,
                                onValueChange = { formData = formData.copy(surname = it) },
                                label = { Text("Surname / Family Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = formData.idNumber,
                                onValueChange = { formData = formData.copy(idNumber = it) },
                                label = { Text("National ID / Passport / MRN") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = formData.dob,
                                    onValueChange = { formData = formData.copy(dob = it) },
                                    label = { Text("Date of Birth") },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = formData.gender,
                                    onValueChange = { formData = formData.copy(gender = it) },
                                    label = { Text("Gender") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // --- SECTION 3: CONTACT & EMERGENCY DETAILS ---
                    item {
                        Text(
                            text = "📞 Contact & Address Information",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = formData.address,
                                onValueChange = { formData = formData.copy(address = it) },
                                label = { Text("Primary Residential Address") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = formData.phone,
                                onValueChange = { formData = formData.copy(phone = it) },
                                label = { Text("Cell/Mobile Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = formData.email,
                                onValueChange = { formData = formData.copy(email = it) },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = formData.emergencyContact,
                                onValueChange = { formData = formData.copy(emergencyContact = it) },
                                label = { Text("Emergency Contact (Name & Phone)") },
                                leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // --- SECTION 4: MEDICAL COVERAGE & HISTORY ---
                    item {
                        Text(
                            text = "🩺 Clinical Records & Aid",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = formData.medicalAid,
                                onValueChange = { formData = formData.copy(medicalAid = it) },
                                label = { Text("Medical Aid Scheme Name & Option") },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                                placeholder = { Text("e.g. Elysium Elite Private, or Out-of-Pocket Cash") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = formData.allergies,
                                onValueChange = { formData = formData.copy(allergies = it) },
                                label = { Text("Known Allergies & Contraindications") },
                                placeholder = { Text("None known / Penicillin etc.") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = formData.chronicConditions,
                                onValueChange = { formData = formData.copy(chronicConditions = it) },
                                label = { Text("Declared Chronic Conditions") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Dismiss")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onFinalize(formData)
                            onDismiss()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalize and Save")
                    }
                }
            }
        }
    }
}

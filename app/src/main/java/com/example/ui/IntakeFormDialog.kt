package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.IntakeFormData

@Composable
fun IntakeFormDialog(
    initialData: IntakeFormData? = null,
    onDismiss: () -> Unit,
    onFinalize: (IntakeFormData) -> Unit
) {
    var formData by remember { mutableStateOf(initialData ?: IntakeFormData()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Patient Registration Form") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                item {
                    OutlinedTextField(value = formData.surname, onValueChange = { formData = formData.copy(surname = it) }, label = { Text("Surname") })
                    OutlinedTextField(value = formData.firstName, onValueChange = { formData = formData.copy(firstName = it) }, label = { Text("First Name") })
                    OutlinedTextField(value = formData.idNumber, onValueChange = { formData = formData.copy(idNumber = it) }, label = { Text("ID Number") })
                    OutlinedTextField(value = formData.dob, onValueChange = { formData = formData.copy(dob = it) }, label = { Text("Date of Birth") })
                    OutlinedTextField(value = formData.gender, onValueChange = { formData = formData.copy(gender = it) }, label = { Text("Gender") })
                    OutlinedTextField(value = formData.address, onValueChange = { formData = formData.copy(address = it) }, label = { Text("Address") })
                    OutlinedTextField(value = formData.phone, onValueChange = { formData = formData.copy(phone = it) }, label = { Text("Phone") })
                    OutlinedTextField(value = formData.email, onValueChange = { formData = formData.copy(email = it) }, label = { Text("Email") })
                    OutlinedTextField(value = formData.medicalAid, onValueChange = { formData = formData.copy(medicalAid = it) }, label = { Text("Medical Aid") })
                    OutlinedTextField(value = formData.emergencyContact, onValueChange = { formData = formData.copy(emergencyContact = it) }, label = { Text("Emergency Contact") })
                    OutlinedTextField(value = formData.allergies, onValueChange = { formData = formData.copy(allergies = it) }, label = { Text("Allergies") })
                    OutlinedTextField(value = formData.chronicConditions, onValueChange = { formData = formData.copy(chronicConditions = it) }, label = { Text("Chronic Conditions") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onFinalize(formData); onDismiss() }) {
                Text("Finalize")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

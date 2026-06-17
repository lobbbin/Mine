package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val vitalsAdapter = moshi.adapter(Vitals::class.java)
    private val chatListAdapter = moshi.adapter<List<ChatMessage>>(
        Types.newParameterizedType(List::class.java, ChatMessage::class.java)
    )

    @TypeConverter
    fun fromVitals(vitals: Vitals?): String {
        return vitals?.let { vitalsAdapter.toJson(it) } ?: ""
    }

    @TypeConverter
    fun toVitals(json: String): Vitals? {
        if (json.isBlank()) return null
        return try {
            vitalsAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromChatList(list: List<ChatMessage>?): String {
        return list?.let { chatListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toChatList(json: String): List<ChatMessage> {
        if (json.isBlank()) return emptyList()
        return try {
            chatListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val intakeFormAdapter = moshi.adapter(IntakeFormData::class.java)

    @TypeConverter
    fun fromIntakeFormData(data: IntakeFormData?): String {
        return data?.let { intakeFormAdapter.toJson(it) } ?: ""
    }

    @TypeConverter
    fun toIntakeFormData(json: String): IntakeFormData? {
        if (json.isBlank()) return null
        return try {
            intakeFormAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}

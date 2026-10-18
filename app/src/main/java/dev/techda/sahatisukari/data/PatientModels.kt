package dev.techda.sahatisukari.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable data class GlucoseRequest(val glucoseValue: Double, val glucoseUnit: String, val measurementDate: String, val measurementTime: String, val measurementContext: String, val minutesAfterMeal: Int? = null, val source: String? = null, val symptoms: String? = null, val notes: String? = null, val confirmedUnusual: Boolean = false)
@Serializable data class MedicationRequest(val medicationName: String, val category: String, val route: String, val frequency: String, val strength: Double? = null, val strengthUnit: String? = null, val prescribedDose: Double? = null, val doseUnit: String? = null, val usualTime: String? = null, val startDate: String? = null, val endDate: String? = null, val prescriberName: String? = null, val insulinFormulation: String? = null, val notes: String? = null, val status: String? = null)
@Serializable data class AdministrationRequest(val medicationId: String, val actualDose: Double? = null, val doseUnit: String? = null, val administrationDate: String, val administrationTime: String, val status: String, val notes: String? = null)
@Serializable data class MealRequest(val mealType: String, val description: String? = null, val carbohydrateGrams: Double? = null, val mealDate: String, val mealTime: String, val notes: String? = null)
@Serializable data class ActivityRequest(val activityType: String, val durationMinutes: Int, val intensity: String, val activityDate: String, val activityTime: String, val notes: String? = null)
@Serializable data class SleepRequest(val durationMinutes: Int, val quality: String, val sleepDate: String, val notes: String? = null)
@Serializable data class SymptomRequest(val symptomType: String, val severity: String, val symptomDate: String, val symptomTime: String, val notes: String? = null)
@Serializable data class FactorRequest(val factorType: String, val description: String? = null, val factorDate: String, val factorTime: String? = null)
@Serializable data class WeightRequest(val weightKg: Double, val measurementDate: String, val notes: String? = null)
@Serializable data class LabRequest(val testType: String, val value: Double, val valueSecondary: Double? = null, val unit: String, val testDate: String, val referenceRange: String? = null, val notes: String? = null)
@Serializable data class ProfileExtrasRequest(val heightCm: Double? = null, val preferredGlucoseUnit: String? = null, val numeralSystem: String? = null)
@Serializable data class DiabetesProfileRequest(val diabetesType: String? = null, val diagnosisDate: String? = null, val diagnosisYear: Int? = null, val monitoringMethod: String? = null, val hypoglycemiaHistory: String? = null, val hypoglycemiaAwareness: String? = null, val treatmentApproach: String? = null, val notes: String? = null)
@Serializable data class ConditionRequest(val conditionType: String, val status: String, val notes: String? = null)
@Serializable data class ConditionsRequest(val conditions: List<ConditionRequest>)
@Serializable data class ReproductiveProfileRequest(val pregnancyStatus: String, val gestationalDiabetesHistory: String, val notes: String? = null)
@Serializable data class AdditionalProfileRequest(val allergies: String? = null, val smokingStatus: String? = null, val familyHistoryDiabetes: String? = null, val healthNotes: String? = null)
@Serializable data class ContactPreferencesRequest(val phone: String? = null, val email: String? = null, val numeralSystem: String? = null, val preferredGlucoseUnit: String? = null)

object PatientRequestCodec {
    fun encode(json: Json, endpoint: String, body: JsonObject): JsonObject = when (endpoint) {
        "glucose" -> json.encodeToJsonElement(json.decodeFromJsonElement<GlucoseRequest>(body)).jsonObject
        "medications" -> json.encodeToJsonElement(json.decodeFromJsonElement<MedicationRequest>(body)).jsonObject
        "administrations" -> json.encodeToJsonElement(json.decodeFromJsonElement<AdministrationRequest>(body)).jsonObject
        "meals" -> json.encodeToJsonElement(json.decodeFromJsonElement<MealRequest>(body)).jsonObject
        "activities" -> json.encodeToJsonElement(json.decodeFromJsonElement<ActivityRequest>(body)).jsonObject
        "sleep" -> json.encodeToJsonElement(json.decodeFromJsonElement<SleepRequest>(body)).jsonObject
        "symptoms" -> json.encodeToJsonElement(json.decodeFromJsonElement<SymptomRequest>(body)).jsonObject
        "factors" -> json.encodeToJsonElement(json.decodeFromJsonElement<FactorRequest>(body)).jsonObject
        "weight" -> json.encodeToJsonElement(json.decodeFromJsonElement<WeightRequest>(body)).jsonObject
        "labs" -> json.encodeToJsonElement(json.decodeFromJsonElement<LabRequest>(body)).jsonObject
        "profile" -> json.encodeToJsonElement(json.decodeFromJsonElement<ProfileExtrasRequest>(body)).jsonObject
        "profile:diabetes" -> json.encodeToJsonElement(json.decodeFromJsonElement<DiabetesProfileRequest>(body)).jsonObject
        "profile:conditions" -> json.encodeToJsonElement(json.decodeFromJsonElement<ConditionsRequest>(body)).jsonObject
        "profile:reproductive" -> json.encodeToJsonElement(json.decodeFromJsonElement<ReproductiveProfileRequest>(body)).jsonObject
        "profile:additional" -> json.encodeToJsonElement(json.decodeFromJsonElement<AdditionalProfileRequest>(body)).jsonObject
        "settings/contact" -> json.encodeToJsonElement(json.decodeFromJsonElement<ContactPreferencesRequest>(body)).jsonObject
        else -> body
    }
}

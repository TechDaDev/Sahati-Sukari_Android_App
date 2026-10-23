package dev.techda.sahatisukari.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Field-specific categories keep overlapping backend enum codes unambiguous. */
internal enum class MedicalEnumField {
    GLUCOSE_UNIT,
    MEASUREMENT_CONTEXT,
    GLUCOSE_SOURCE,
    MEDICATION_CATEGORY,
    DOSE_UNIT,
    ADMINISTRATION_ROUTE,
    MEDICATION_FREQUENCY,
    MEDICATION_STATUS,
    ADMINISTRATION_STATUS,
    MEAL_TYPE,
    ACTIVITY_INTENSITY,
    SLEEP_QUALITY,
    SYMPTOM_TYPE,
    SYMPTOM_SEVERITY,
    DAILY_FACTOR_TYPE,
    LAB_TEST_TYPE,
    GENDER,
    DIABETES_TYPE,
    MONITORING_METHOD,
    YES_NO_UNSURE,
    HYPOGLYCEMIA_AWARENESS,
    TREATMENT_APPROACH,
    CONDITION_TYPE,
    CONDITION_STATUS,
    SMOKING_STATUS,
    PREGNANCY_STATUS,
    NUMERAL_SYSTEM,
}

internal data class MedicalFieldPresentation(val label: String, val value: String)

internal object MedicalPresentation {
    const val UNKNOWN_ENUM = "قيمة محفوظة غير معروفة"
    const val UNAVAILABLE_DATE = "تاريخ غير متاح"
    private const val DEFAULT_TIME_ZONE = "Asia/Baghdad"

    private val labels: Map<MedicalEnumField, Map<String, String>> = mapOf(
        MedicalEnumField.GLUCOSE_UNIT to mapOf("MG_DL" to "mg/dL", "MMOL_L" to "mmol/L"),
        MedicalEnumField.MEASUREMENT_CONTEXT to mapOf(
            "FASTING" to "صائم", "BEFORE_BREAKFAST" to "قبل الإفطار", "AFTER_BREAKFAST" to "بعد الإفطار",
            "BEFORE_LUNCH" to "قبل الغداء", "AFTER_LUNCH" to "بعد الغداء", "BEFORE_DINNER" to "قبل العشاء",
            "AFTER_DINNER" to "بعد العشاء", "BEFORE_SLEEP" to "قبل النوم", "OTHER" to "سياق آخر",
        ),
        MedicalEnumField.GLUCOSE_SOURCE to mapOf(
            "FINGERSTICK_METER" to "جهاز قياس من الإصبع", "CGM" to "مراقبة مستمرة (CGM)",
            "LAB" to "مختبر", "OTHER" to "مصدر آخر", "NOT_SPECIFIED" to "غير محدد",
        ),
        MedicalEnumField.MEDICATION_CATEGORY to mapOf(
            "INSULIN" to "إنسولين", "BIGUANIDE" to "بيغوانيد", "SULFONYLUREA" to "سلفونيل يوريا",
            "DPP4_INHIBITOR" to "مثبط DPP-4", "SGLT2_INHIBITOR" to "مثبط SGLT2", "GLP1_AGONIST" to "ناهض GLP-1",
            "TZD" to "ثيازوليدين ديون", "MEGLITINIDE" to "ميغليتينيد", "ALPHA_GLUCOSIDASE_INHIBITOR" to "مثبط ألفا غلوكوزيداز",
            "ANTIHYPERTENSIVE" to "دواء لضغط الدم", "STATIN" to "ستاتين", "OTHER" to "فئة دواء أخرى",
        ),
        MedicalEnumField.DOSE_UNIT to mapOf(
            "UNIT" to "U", "MG" to "mg", "MCG" to "mcg", "G" to "g", "ML" to "mL",
            "TABLET" to "قرص", "CAPSULE" to "كبسولة", "DROP" to "قطرة", "PUFF" to "بخة", "SACHET" to "كيس",
        ),
        MedicalEnumField.ADMINISTRATION_ROUTE to mapOf(
            "SUBCUTANEOUS" to "تحت الجلد", "INTRAVENOUS" to "عن طريق الوريد", "INTRAMUSCULAR" to "في العضل",
            "ORAL" to "عن طريق الفم", "INHALED" to "عن طريق الاستنشاق", "TOPICAL" to "موضعي", "OTHER" to "طريقة أخرى",
        ),
        MedicalEnumField.MEDICATION_FREQUENCY to mapOf(
            "ONCE_DAILY" to "مرة يوميًا", "TWICE_DAILY" to "مرتين يوميًا", "THREE_TIMES_DAILY" to "ثلاث مرات يوميًا",
            "FOUR_TIMES_DAILY" to "أربع مرات يوميًا", "WITH_MEALS" to "مع الوجبات", "BEFORE_MEALS" to "قبل الوجبات",
            "AT_BEDTIME" to "قبل النوم", "AS_NEEDED" to "عند الحاجة", "WEEKLY" to "أسبوعيًا", "OTHER" to "تكرار آخر",
        ),
        MedicalEnumField.MEDICATION_STATUS to mapOf("ACTIVE" to "نشط", "INACTIVE" to "غير نشط"),
        MedicalEnumField.ADMINISTRATION_STATUS to mapOf("TAKEN" to "تم تناولها", "MISSED" to "فاتت", "DELAYED" to "متأخرة"),
        MedicalEnumField.MEAL_TYPE to mapOf("BREAKFAST" to "الإفطار", "LUNCH" to "الغداء", "DINNER" to "العشاء", "SNACK" to "وجبة خفيفة", "OTHER" to "وجبة أخرى"),
        MedicalEnumField.ACTIVITY_INTENSITY to mapOf("LIGHT" to "خفيف", "MODERATE" to "متوسط", "VIGOROUS" to "شديد"),
        MedicalEnumField.SLEEP_QUALITY to mapOf("GOOD" to "جيد", "FAIR" to "متوسط", "POOR" to "ضعيف", "NOT_SPECIFIED" to "غير محدد"),
        MedicalEnumField.SYMPTOM_TYPE to mapOf(
            "DIZZINESS" to "دوخة", "SWEATING" to "تعرق", "TREMOR" to "رجفة", "EXCESSIVE_THIRST" to "عطش شديد",
            "FATIGUE" to "إرهاق", "HEADACHE" to "صداع", "BLURRED_VISION" to "تشوش الرؤية", "PALPITATIONS" to "خفقان",
            "NAUSEA" to "غثيان", "OTHER" to "عَرَض آخر",
        ),
        MedicalEnumField.SYMPTOM_SEVERITY to mapOf("MILD" to "خفيف", "MODERATE" to "متوسط", "SEVERE" to "شديد"),
        MedicalEnumField.DAILY_FACTOR_TYPE to mapOf("ILLNESS" to "مرض", "STRESS" to "توتر نفسي", "TRAVEL" to "سفر", "MENSTRUATION" to "الدورة الشهرية", "OTHER" to "عامل آخر"),
        MedicalEnumField.LAB_TEST_TYPE to mapOf(
            "HBA1C" to "السكر التراكمي (HbA1c)", "CREATININE" to "الكرياتينين", "EGFR" to "معدل الترشيح الكبيبي المقدر (eGFR)",
            "URINE_ACR" to "نسبة الألبومين إلى الكرياتينين في البول", "BLOOD_PRESSURE" to "ضغط الدم", "TOTAL_CHOLESTEROL" to "الكوليسترول الكلي",
            "LDL" to "الكوليسترول الضار (LDL)", "HDL" to "الكوليسترول النافع (HDL)", "TRIGLYCERIDES" to "الدهون الثلاثية", "OTHER" to "تحليل آخر",
        ),
        MedicalEnumField.GENDER to mapOf("MALE" to "ذكر", "FEMALE" to "أنثى"),
        MedicalEnumField.DIABETES_TYPE to mapOf("TYPE_1" to "النوع الأول", "TYPE_2" to "النوع الثاني", "OTHER" to "نوع آخر", "UNKNOWN" to "غير معروف"),
        MedicalEnumField.MONITORING_METHOD to mapOf("FINGERSTICK" to "جهاز قياس من الإصبع", "CGM" to "مراقبة مستمرة (CGM)", "BOTH" to "كلاهما", "NOT_SPECIFIED" to "غير محدد"),
        MedicalEnumField.YES_NO_UNSURE to mapOf("YES" to "نعم", "NO" to "لا", "UNSURE" to "غير متأكد"),
        MedicalEnumField.HYPOGLYCEMIA_AWARENESS to mapOf("NORMAL" to "طبيعي", "REDUCED" to "منخفض", "UNSURE" to "غير متأكد", "NOT_SPECIFIED" to "غير محدد"),
        MedicalEnumField.TREATMENT_APPROACH to mapOf(
            "INSULIN" to "أنسولين", "NON_INSULIN_MEDICATION" to "أدوية غير أنسولينية", "BOTH" to "أنسولين وأدوية أخرى",
            "LIFESTYLE_ONLY" to "نمط حياة فقط", "NOT_SPECIFIED" to "غير محدد",
        ),
        MedicalEnumField.CONDITION_TYPE to mapOf(
            "HYPERTENSION" to "ارتفاع ضغط الدم", "HEART_DISEASE" to "أمراض القلب", "KIDNEY_DISEASE" to "أمراض الكلى",
            "HIGH_CHOLESTEROL" to "ارتفاع الكوليسترول", "RETINOPATHY" to "اعتلال الشبكية السكري", "NEUROPATHY" to "اعتلال الأعصاب السكري",
            "FOOT_PROBLEMS" to "مشاكل القدم السكرية", "OTHER" to "حالات طبية أخرى",
        ),
        MedicalEnumField.CONDITION_STATUS to mapOf("DIAGNOSED" to "مُشخّصة", "NOT_DIAGNOSED" to "غير مُشخّصة", "UNKNOWN" to "غير معروف"),
        MedicalEnumField.SMOKING_STATUS to mapOf("NEVER" to "لم أدخّن", "CURRENT" to "مدخّن حاليًا", "FORMER" to "مدخّن سابق", "NOT_SPECIFIED" to "غير محدد"),
        MedicalEnumField.PREGNANCY_STATUS to mapOf("NOT_APPLICABLE" to "لا ينطبق", "NOT_PREGNANT" to "غير حامل", "PREGNANT" to "حامل", "BREASTFEEDING" to "مرضعة", "NOT_SPECIFIED" to "أفضّل عدم الذكر"),
        MedicalEnumField.NUMERAL_SYSTEM to mapOf("LATIN" to "الأرقام الغربية", "ARABIC_INDIC" to "الأرقام العربية الهندية"),
    )

    private val genericLabels: Map<String, String> = mapOf(
        "MG_DL" to "mg/dL", "MMOL_L" to "mmol/L", "FASTING" to "صائم",
        "BEFORE_BREAKFAST" to "قبل الإفطار", "AFTER_BREAKFAST" to "بعد الإفطار", "BEFORE_LUNCH" to "قبل الغداء", "AFTER_LUNCH" to "بعد الغداء",
        "BEFORE_DINNER" to "قبل العشاء", "AFTER_DINNER" to "بعد العشاء", "BEFORE_SLEEP" to "قبل النوم", "FINGERSTICK_METER" to "جهاز قياس من الإصبع",
        "CGM" to "مراقبة مستمرة (CGM)", "LAB" to "مختبر", "NOT_SPECIFIED" to "غير محدد", "BREAKFAST" to "الإفطار", "LUNCH" to "الغداء",
        "DINNER" to "العشاء", "SNACK" to "وجبة خفيفة", "LIGHT" to "خفيف", "MODERATE" to "متوسط", "VIGOROUS" to "شديد",
        "GOOD" to "جيد", "FAIR" to "متوسط", "POOR" to "ضعيف", "DIZZINESS" to "دوخة", "SWEATING" to "تعرق", "TREMOR" to "رجفة",
        "EXCESSIVE_THIRST" to "عطش شديد", "FATIGUE" to "إرهاق", "HEADACHE" to "صداع", "BLURRED_VISION" to "تشوش الرؤية",
        "PALPITATIONS" to "خفقان", "NAUSEA" to "غثيان", "MILD" to "خفيف", "SEVERE" to "شديد", "ILLNESS" to "مرض", "STRESS" to "توتر نفسي",
        "TRAVEL" to "سفر", "MENSTRUATION" to "الدورة الشهرية", "INSULIN" to "إنسولين", "BIGUANIDE" to "بيغوانيد", "SULFONYLUREA" to "سلفونيل يوريا",
        "DPP4_INHIBITOR" to "مثبط DPP-4", "SGLT2_INHIBITOR" to "مثبط SGLT2", "GLP1_AGONIST" to "ناهض GLP-1", "TZD" to "ثيازوليدين ديون",
        "MEGLITINIDE" to "ميغليتينيد", "ALPHA_GLUCOSIDASE_INHIBITOR" to "مثبط ألفا غلوكوزيداز", "ANTIHYPERTENSIVE" to "دواء لضغط الدم",
        "STATIN" to "ستاتين", "UNIT" to "U", "MG" to "mg", "MCG" to "mcg", "G" to "g", "ML" to "mL", "TABLET" to "قرص",
        "CAPSULE" to "كبسولة", "DROP" to "قطرة", "PUFF" to "بخة", "SACHET" to "كيس", "ORAL" to "عن طريق الفم", "SUBCUTANEOUS" to "تحت الجلد",
        "INTRAVENOUS" to "عن طريق الوريد", "INTRAMUSCULAR" to "في العضل", "INHALED" to "عن طريق الاستنشاق", "TOPICAL" to "موضعي",
        "ONCE_DAILY" to "مرة يوميًا", "TWICE_DAILY" to "مرتين يوميًا", "THREE_TIMES_DAILY" to "ثلاث مرات يوميًا", "FOUR_TIMES_DAILY" to "أربع مرات يوميًا",
        "WITH_MEALS" to "مع الوجبات", "BEFORE_MEALS" to "قبل الوجبات", "AT_BEDTIME" to "قبل النوم", "AS_NEEDED" to "عند الحاجة", "WEEKLY" to "أسبوعيًا",
        "ACTIVE" to "نشط", "INACTIVE" to "متوقف", "TAKEN" to "تم تناولها", "MISSED" to "فاتت", "DELAYED" to "متأخرة", "OTHER" to "أخرى",
        "HBA1C" to "السكر التراكمي (HbA1c)", "CREATININE" to "الكرياتينين", "EGFR" to "معدل الترشيح الكبيبي المقدر (eGFR)",
        "URINE_ACR" to "نسبة الألبومين إلى الكرياتينين في البول", "BLOOD_PRESSURE" to "ضغط الدم", "TOTAL_CHOLESTEROL" to "الكوليسترول الكلي",
        "LDL" to "الكوليسترول الضار (LDL)", "HDL" to "الكوليسترول النافع (HDL)", "TRIGLYCERIDES" to "الدهون الثلاثية",
    )

    private val profileFields = mapOf(
        "gender" to MedicalEnumField.GENDER,
        "conditionType" to MedicalEnumField.CONDITION_TYPE,
        "conditionStatus" to MedicalEnumField.CONDITION_STATUS,
        "status:" to MedicalEnumField.CONDITION_STATUS,
        "diabetesType" to MedicalEnumField.DIABETES_TYPE,
        "monitoringMethod" to MedicalEnumField.MONITORING_METHOD,
        "hypoglycemiaHistory" to MedicalEnumField.YES_NO_UNSURE,
        "gestationalDiabetesHistory" to MedicalEnumField.YES_NO_UNSURE,
        "familyHistoryDiabetes" to MedicalEnumField.YES_NO_UNSURE,
        "hypoglycemiaAwareness" to MedicalEnumField.HYPOGLYCEMIA_AWARENESS,
        "treatmentApproach" to MedicalEnumField.TREATMENT_APPROACH,
        "pregnancyStatus" to MedicalEnumField.PREGNANCY_STATUS,
        "smokingStatus" to MedicalEnumField.SMOKING_STATUS,
        "preferredGlucoseUnit" to MedicalEnumField.GLUCOSE_UNIT,
        "numeralSystem" to MedicalEnumField.NUMERAL_SYSTEM,
    )

    private val fieldLabels = mapOf(
        "fullName" to "الاسم", "email" to "البريد الإلكتروني", "phone" to "الهاتف", "phoneNumber" to "رقم الهاتف", "gender" to "الجنس",
        "dateOfBirth" to "تاريخ الميلاد", "age" to "العمر", "heightCm" to "الطول", "timezone" to "المنطقة الزمنية", "diabetesType" to "نوع السكري",
        "diagnosisDate" to "تاريخ التشخيص", "diagnosisYear" to "سنة التشخيص", "monitoringMethod" to "طريقة المتابعة", "treatmentApproach" to "أسلوب العلاج",
        "preferredGlucoseUnit" to "وحدة السكر المفضلة", "numeralSystem" to "نظام الأرقام", "testType" to "نوع التحليل", "value" to "النتيجة",
        "valueSecondary" to "قيمة إضافية", "unit" to "الوحدة", "testDate" to "تاريخ التحليل", "measurementDate" to "تاريخ القياس", "weightKg" to "الوزن",
        "notes" to "ملاحظات", "referenceRange" to "المدى المرجعي", "glucoseValue" to "قيمة السكر", "glucoseUnit" to "وحدة السكر",
        "measurementDatetime" to "التاريخ والوقت", "measurementDateTime" to "التاريخ والوقت", "measurementTime" to "وقت القياس",
        "measurementContext" to "سياق القياس", "minutesAfterMeal" to "الدقائق بعد الوجبة", "source" to "مصدر القياس", "symptoms" to "الأعراض",
        "medicationName" to "اسم الدواء", "category" to "فئة الدواء", "strength" to "تركيز الدواء", "strengthUnit" to "وحدة التركيز",
        "prescribedDose" to "الجرعة الموصوفة", "actualDose" to "الجرعة المسجلة", "doseUnit" to "وحدة الجرعة", "route" to "طريقة الاستخدام",
        "frequency" to "التكرار", "usualTime" to "الوقت المعتاد", "startDate" to "تاريخ البدء", "endDate" to "تاريخ الانتهاء",
        "prescriberName" to "اسم الواصف", "insulinFormulation" to "تركيبة الإنسولين", "status" to "الحالة", "administrationDatetime" to "تاريخ ووقت الجرعة",
        "symptomsText" to "الأعراض", "strengthUnit" to "وحدة التركيز",
        "administrationDateTime" to "التاريخ والوقت", "administrationDate" to "تاريخ الجرعة", "administrationTime" to "وقت الجرعة",
        "mealType" to "نوع الوجبة", "description" to "الوصف", "carbohydrateGrams" to "الكربوهيدرات", "mealDate" to "تاريخ الوجبة", "mealTime" to "وقت الوجبة", "mealDatetime" to "التاريخ والوقت",
        "activityType" to "نوع النشاط", "durationMinutes" to "المدة", "intensity" to "الشدة", "activityDate" to "تاريخ النشاط", "activityTime" to "وقت النشاط", "activityDatetime" to "التاريخ والوقت",
        "sleepDate" to "تاريخ النوم", "quality" to "جودة النوم", "symptomType" to "العَرَض", "severity" to "الشدة", "symptomDate" to "تاريخ العَرَض", "symptomDatetime" to "التاريخ والوقت",
        "symptomTime" to "وقت العَرَض", "factorType" to "العامل", "factorDate" to "تاريخ العامل", "factorTime" to "وقت العامل",
        "emailVerified" to "حالة البريد الإلكتروني", "phoneVerified" to "حالة الهاتف", "allergies" to "الحساسية", "smokingStatus" to "حالة التدخين",
        "familyHistoryDiabetes" to "تاريخ عائلي للسكري", "healthNotes" to "معلومات صحية إضافية", "bmi" to "مؤشر كتلة الجسم",
        "currentWeightKg" to "الوزن الحالي", "nationalIdMasked" to "الرقم الوطني المسجّل", "lastUsedAt" to "آخر استخدام",
    )

    fun label(field: MedicalEnumField, value: String?): String {
        if (value.isNullOrBlank()) return "غير متوفر"
        return labels[field]?.get(value) ?: UNKNOWN_ENUM
    }

    fun knownCodes(field: MedicalEnumField): Set<String> = labels[field].orEmpty().keys

    fun profileEnumLabel(field: String, code: String?): String {
        val category = profileFields[field] ?: return UNKNOWN_ENUM
        return when {
            field == "conditionType" && code !in labels.getValue(category) -> "حالة طبية غير معروفة"
            field in setOf("conditionStatus", "status:") && code !in labels.getValue(category) -> "حالة محفوظة غير معروفة"
            else -> label(category, code)
        }
    }

    fun genericEnumLabel(code: String?): String {
        if (code.isNullOrBlank()) return "غير متوفر"
        return genericLabels[code] ?: UNKNOWN_ENUM
    }

    fun localizedDigits(value: String, numeralSystem: String? = null): String =
        if (numeralSystem == "ARABIC_INDIC") value.map { if (it in '0'..'9') ('٠'.code + (it - '0')).toChar() else it }.joinToString("") else value

    /** Rounds only to the field's stated display precision, then removes insignificant zeros. */
    fun formatDecimal(value: String?, maxFractionDigits: Int, numeralSystem: String? = null, minFractionDigits: Int = 0): String {
        val parsed = value?.trim()?.toBigDecimalOrNull() ?: return "—"
        val scaled = parsed.setScale(maxFractionDigits.coerceAtLeast(minFractionDigits), RoundingMode.HALF_UP)
        val trimmed = scaled.stripTrailingZeros()
        val result = if (trimmed.scale() < minFractionDigits) trimmed.setScale(minFractionDigits).toPlainString() else trimmed.toPlainString()
        return localizedDigits(result, numeralSystem)
    }

    fun formatGlucose(value: String?, unit: String?, numeralSystem: String? = null): String {
        val parsed = value?.trim()?.toBigDecimalOrNull() ?: return "—"
        return when (unit) {
            "MG_DL" -> "${formatDecimal(parsed.toPlainString(), 2, numeralSystem)} mg/dL"
            "MMOL_L" -> "${formatDecimal(parsed.toPlainString(), 1, numeralSystem)} mmol/L"
            else -> UNKNOWN_ENUM
        }
    }

    /** Record-card variant preserves the exact supplied measurement precision. */
    fun formatGlucoseRecord(value: String?, unit: String?, numeralSystem: String? = null): String {
        val parsed = value?.trim()?.toBigDecimalOrNull() ?: return "—"
        val number = localizedDigits(parsed.stripTrailingZeros().toPlainString(), numeralSystem)
        val unitLabel = when (unit) {
            "MG_DL" -> "mg/dL"
            "MMOL_L" -> "mmol/L"
            else -> return number
        }
        return "$number $unitLabel"
    }

    /** Converts a standardized mg/dL value only when presenting in the configured mmol/L unit. */
    fun formatGlucoseFromMgDl(value: String?, unit: String?, numeralSystem: String? = null): String {
        val parsed = value?.trim()?.toBigDecimalOrNull() ?: return "—"
        return when (unit) {
            "MG_DL" -> "${formatDecimal(parsed.toPlainString(), 2, numeralSystem)} mg/dL"
            "MMOL_L" -> {
                val converted = parsed.divide(BigDecimal("18.0182"), 1, RoundingMode.HALF_UP)
                "${formatDecimal(converted.toPlainString(), 1, numeralSystem)} mmol/L"
            }
            else -> UNKNOWN_ENUM
        }
    }

    fun formatWeight(value: String?, numeralSystem: String? = null): String =
        formatDecimal(value, 2, numeralSystem).let { if (it == "—") it else "$it كغم" }

    fun formatHbA1c(value: String?, numeralSystem: String? = null): String =
        formatDecimal(value, 1, numeralSystem).let { if (it == "—") it else "$it%" }

    fun formatBmi(value: String?, numeralSystem: String? = null): String =
        formatDecimal(value, 1, numeralSystem).let { if (it == "—") it else "$it kg/m²" }

    /** Computes display-only BMI using current profile height; historical height is not stored per weight record. */
    fun calculateBmi(weightKg: String?, heightCm: String?, numeralSystem: String? = null): String {
        val weight = weightKg?.trim()?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return "غير متاح"
        val height = heightCm?.trim()?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return "غير متاح"
        val heightMeters = height.divide(BigDecimal("100"), 8, RoundingMode.HALF_UP)
        val heightSquared = heightMeters.multiply(heightMeters)
        if (heightSquared.signum() <= 0) return "غير متاح"
        val bmi = weight.divide(heightSquared, 8, RoundingMode.HALF_UP)
        return formatBmi(bmi.toPlainString(), numeralSystem)
    }

    fun formatCarbohydrates(value: String?, numeralSystem: String? = null): String =
        formatDecimal(value, 1, numeralSystem).let { if (it == "—") it else "$it g" }

    /** Lab values retain their supplied precision; only insignificant trailing zeros are removed. */
    fun formatLabValue(value: String?, numeralSystem: String? = null): String {
        val parsed = value?.trim()?.toBigDecimalOrNull() ?: return "—"
        return localizedDigits(parsed.stripTrailingZeros().toPlainString(), numeralSystem)
    }

    fun formatMedicationDose(value: String?, unit: String?, numeralSystem: String? = null): String {
        val dose = formatDecimal(value, 3, numeralSystem)
        if (dose == "—") return dose
        val unitLabel = label(MedicalEnumField.DOSE_UNIT, unit)
        return if (unit.isNullOrBlank()) dose else "$dose $unitLabel"
    }

    fun formatBloodPressure(systolic: String?, diastolic: String?, numeralSystem: String? = null): String {
        val sys = formatLabValue(systolic, numeralSystem)
        val dia = formatLabValue(diastolic, numeralSystem)
        return if (sys == "—" || dia == "—") "—" else "$sys/$dia mmHg"
    }

    fun formatActivityDuration(minutes: String?, numeralSystem: String? = null): String = formatDuration(minutes, numeralSystem)

    fun formatSleepDuration(minutes: String?, numeralSystem: String? = null): String = formatDuration(minutes, numeralSystem)

    private fun formatDuration(minutes: String?, numeralSystem: String?): String {
        val total = minutes?.toIntOrNull()?.takeIf { it >= 0 } ?: return "—"
        val hours = total / 60
        val remainder = total % 60
        if (hours == 0) return countedUnit(total, minuteUnit(total), numeralSystem)
        val hourText = countedUnit(hours, hourUnit(hours), numeralSystem)
        return if (remainder == 0) hourText else "$hourText و ${countedUnit(remainder, minuteUnit(remainder), numeralSystem)}"
    }

    private fun countedUnit(value: Int, unit: String, numeralSystem: String?): String =
        if (value in 1..2) unit else "${localizedDigits(value.toString(), numeralSystem)} $unit"

    private fun hourUnit(value: Int): String = when (value) {
        1 -> "ساعة"
        2 -> "ساعتان"
        in 3..10 -> "ساعات"
        else -> "ساعة"
    }

    private fun minuteUnit(value: Int): String = when (value) {
        1 -> "دقيقة واحدة"
        2 -> "دقيقتان"
        in 3..10 -> "دقائق"
        else -> "دقيقة"
    }

    fun formatDateOnly(value: String?, numeralSystem: String? = null): String {
        val date = runCatching { LocalDate.parse(value?.trim().orEmpty()) }.getOrNull() ?: return UNAVAILABLE_DATE
        val text = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT).format(date)
        return localizedDigits(text, numeralSystem)
    }

    fun formatTimeOnly(value: String?, numeralSystem: String? = null): String {
        val time = runCatching { LocalTime.parse(value?.trim().orEmpty()) }.getOrNull() ?: return "وقت غير متاح"
        val hour = (time.hour % 12).let { if (it == 0) 12 else it }
        val clock = "${hour}:${time.minute.toString().padStart(2, '0')}"
        return "${localizedDigits(clock, numeralSystem)} ${if (time.hour < 12) "صباحًا" else "مساءً"}"
    }

    fun formatInstant(value: String?, timeZoneId: String? = null, numeralSystem: String? = null): String {
        val raw = value?.trim().orEmpty()
        val instant = runCatching { Instant.parse(raw) }.getOrElse {
            runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        } ?: return UNAVAILABLE_DATE
        val zone = runCatching { ZoneId.of(timeZoneId ?: DEFAULT_TIME_ZONE) }.getOrElse { ZoneId.of(DEFAULT_TIME_ZONE) }
        val local = instant.atZone(zone)
        val date = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT).format(local)
        val hour = (local.hour % 12).let { if (it == 0) 12 else it }
        val clock = "$hour:${local.minute.toString().padStart(2, '0')}"
        val period = if (local.hour < 12) "صباحًا" else "مساءً"
        return "${localizedDigits(date, numeralSystem)} – ${localizedDigits(clock, numeralSystem)} $period"
    }

    fun formatToday(timeZoneId: String?, numeralSystem: String? = null): String {
        val zone = runCatching { ZoneId.of(timeZoneId ?: DEFAULT_TIME_ZONE) }.getOrElse { ZoneId.of(DEFAULT_TIME_ZONE) }
        val date = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT).format(LocalDate.now(zone))
        return localizedDigits(date, numeralSystem)
    }

    /** Known fields get Arabic labels and typed formatting; unknown values remain visible without leaking API keys. */
    fun presentRecordField(
        key: String,
        value: String?,
        timeZoneId: String? = null,
        numeralSystem: String? = null,
        glucoseUnit: String? = null,
        doseUnit: String? = null,
        strengthUnit: String? = null,
    ): MedicalFieldPresentation? {
        if (key in setOf("id", "patientId", "medicationId", "createdAt", "updatedAt", "deletedAt", "recordedAt", "standardizedValueMgdl", "enteredByPatient")) return null
        if (key.endsWith("Id")) return null
        val raw = value?.takeIf(String::isNotBlank) ?: return null
        val label = fieldLabels[key] ?: "معلومات إضافية"
        val category = recordEnumField(key)
        val formatted = when {
            category != null -> label(category, raw)
            key == "status" && raw in setOf("ACTIVE", "INACTIVE") -> label(MedicalEnumField.MEDICATION_STATUS, raw)
            key == "status" && raw in setOf("TAKEN", "MISSED", "DELAYED") -> label(MedicalEnumField.ADMINISTRATION_STATUS, raw)
            key == "status" && raw in setOf("DIAGNOSED", "NOT_DIAGNOSED", "UNKNOWN") -> label(MedicalEnumField.CONDITION_STATUS, raw)
            key in dateOnlyFields -> formatDateOnly(raw, numeralSystem)
            key in timeOnlyFields -> formatTimeOnly(raw, numeralSystem)
            key in instantFields -> formatInstant(raw, timeZoneId, numeralSystem)
            key == "glucoseValue" -> if (glucoseUnit == null) formatDecimal(raw, 1, numeralSystem) else formatGlucose(raw, glucoseUnit, numeralSystem)
            key == "weightKg" || key == "currentWeightKg" -> formatWeight(raw, numeralSystem)
            key == "hba1c" -> formatHbA1c(raw, numeralSystem)
            key == "bmi" -> formatBmi(raw, numeralSystem)
            key == "carbohydrateGrams" -> formatCarbohydrates(raw, numeralSystem)
            key == "durationMinutes" -> formatActivityDuration(raw, numeralSystem)
            key == "prescribedDose" || key == "actualDose" -> formatMedicationDose(raw, doseUnit, numeralSystem)
            key == "strength" -> formatMedicationDose(raw, strengthUnit, numeralSystem)
            key == "value" || key == "valueSecondary" -> formatLabValue(raw, numeralSystem)
            key == "age" || key == "heightCm" || key == "diagnosisYear" || key == "minutesAfterMeal" -> localizedDigits(raw, numeralSystem)
            key in setOf("emailVerified", "phoneVerified") -> if (raw == "true") "تم التحقق" else "لم يتم التحقق"
            isEnumCode(raw) -> UNKNOWN_ENUM
            else -> raw
        }
        return MedicalFieldPresentation(label, formatted)
    }

    fun fieldLabel(key: String): String = fieldLabels[key] ?: "معلومات إضافية"

    private fun isEnumCode(value: String): Boolean = value.matches(Regex("[A-Z][A-Z0-9_]*"))

    private fun recordEnumField(key: String): MedicalEnumField? = when (key) {
        "glucoseUnit", "preferredGlucoseUnit" -> MedicalEnumField.GLUCOSE_UNIT
        "measurementContext" -> MedicalEnumField.MEASUREMENT_CONTEXT
        "source" -> MedicalEnumField.GLUCOSE_SOURCE
        "category" -> MedicalEnumField.MEDICATION_CATEGORY
        "doseUnit", "strengthUnit" -> MedicalEnumField.DOSE_UNIT
        "route" -> MedicalEnumField.ADMINISTRATION_ROUTE
        "frequency" -> MedicalEnumField.MEDICATION_FREQUENCY
        "mealType" -> MedicalEnumField.MEAL_TYPE
        "intensity" -> MedicalEnumField.ACTIVITY_INTENSITY
        "quality" -> MedicalEnumField.SLEEP_QUALITY
        "symptomType" -> MedicalEnumField.SYMPTOM_TYPE
        "severity" -> MedicalEnumField.SYMPTOM_SEVERITY
        "factorType" -> MedicalEnumField.DAILY_FACTOR_TYPE
        "testType" -> MedicalEnumField.LAB_TEST_TYPE
        "gender" -> MedicalEnumField.GENDER
        "diabetesType" -> MedicalEnumField.DIABETES_TYPE
        "monitoringMethod" -> MedicalEnumField.MONITORING_METHOD
        "hypoglycemiaHistory", "gestationalDiabetesHistory", "familyHistoryDiabetes" -> MedicalEnumField.YES_NO_UNSURE
        "hypoglycemiaAwareness" -> MedicalEnumField.HYPOGLYCEMIA_AWARENESS
        "treatmentApproach" -> MedicalEnumField.TREATMENT_APPROACH
        "conditionType" -> MedicalEnumField.CONDITION_TYPE
        "conditionStatus" -> MedicalEnumField.CONDITION_STATUS
        "smokingStatus" -> MedicalEnumField.SMOKING_STATUS
        "pregnancyStatus" -> MedicalEnumField.PREGNANCY_STATUS
        "numeralSystem" -> MedicalEnumField.NUMERAL_SYSTEM
        else -> null
    }

    private val dateOnlyFields = setOf("dateOfBirth", "diagnosisDate", "measurementDate", "testDate", "startDate", "endDate", "mealDate", "activityDate", "sleepDate", "symptomDate", "factorDate", "administrationDate")
    private val timeOnlyFields = setOf("measurementTime", "usualTime", "mealTime", "activityTime", "symptomTime", "factorTime", "administrationTime")
    private val instantFields = setOf("measurementDatetime", "measurementDateTime", "administrationDatetime", "administrationDateTime", "mealDatetime", "activityDatetime", "symptomDatetime", "recordedAt", "lastUsedAt")
}

internal fun profileMedicalEnumLabel(field: String, code: String?): String = MedicalPresentation.profileEnumLabel(field, code)

internal fun medicalValueTextStyle(style: TextStyle): TextStyle = style.copy(textDirection = TextDirection.ContentOrLtr)

@Composable
internal fun ArabicFieldLabel(label: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) {
    Text(label, modifier = modifier, style = style.copy(textDirection = TextDirection.ContentOrRtl), textAlign = TextAlign.Start)
}

@Composable
internal fun MedicalValueText(value: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium, textAlign: TextAlign = TextAlign.Start) {
    Text(value, modifier = modifier, style = medicalValueTextStyle(style), textAlign = textAlign)
}

@Composable
internal fun MedicalDateText(value: String?, timeZoneId: String? = null, numeralSystem: String? = null, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    MedicalValueText(MedicalPresentation.formatInstant(value, timeZoneId, numeralSystem), modifier, style)
}

@Composable
internal fun MedicalLabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelModifier: Modifier = Modifier,
    valueModifier: Modifier = Modifier,
    labelWeight: Float? = null,
    valueWeight: Float? = null,
    labelStyle: TextStyle = MaterialTheme.typography.bodySmall,
    valueStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val weightedLabelModifier = if (labelWeight == null) labelModifier else labelModifier.weight(labelWeight)
        val weightedValueModifier = if (valueWeight == null) valueModifier else valueModifier.weight(valueWeight)
        ArabicFieldLabel(label, weightedLabelModifier, labelStyle)
        MedicalValueText(value, weightedValueModifier, valueStyle, TextAlign.Start)
    }
}

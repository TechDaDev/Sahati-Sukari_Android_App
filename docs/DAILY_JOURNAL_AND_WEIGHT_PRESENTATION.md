# Daily Journal and weight presentation

Daily Journal cards use the shared Arabic medical presentation helpers and RTL label/value rows. Dates remain date-only where the API stores calendar dates; timestamp and time values use the patient's configured timezone and numeral preference. Weight cards put the formatted weight in the heading, then show a display-only BMI, measurement date, and notes without repeating the weight. The Health Profile keeps laboratory history; weight history and entry are available under **يومياتي → الوزن**.

BMI is calculated as `weightKg / (heightCm / 100)²`, rounded to one decimal place. It is not persisted and has no diagnostic category or interpretation. Missing, invalid, zero, or negative height/weight produces **غير متاح**. Weight history records do not store a height snapshot, so historical BMI is recomputed using the current height in the Health Profile; changing that height can change the displayed historical BMI.

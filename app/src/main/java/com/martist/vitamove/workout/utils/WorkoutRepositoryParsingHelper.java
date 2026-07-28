package com.martist.vitamove.workout.utils;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseMedia;
import com.martist.vitamove.workout.data.model.WorkoutPlan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WorkoutRepositoryParsingHelper {
    public Exercise parseExerciseFromJson(JSONObject json) {
        try {
            String id = json.getString("id");
            String name = json.getString("name");
            String description = json.optString("description", "");


            String difficulty = json.optString("difficulty", "Легкое");


            String exerciseType = json.optString("exercise_type", "Силовое");


            List<String> categories = new ArrayList<>();
            if (json.has("category") && !json.isNull("category")) {
                String categoryStr = json.optString("category", "");
                if (!categoryStr.isEmpty()) {
                    categories.add(categoryStr);
                }
            }


            if (json.has("categories") && !json.isNull("categories")) {
                try {
                    JSONArray categoriesArray = json.getJSONArray("categories");
                    for (int i = 0; i < categoriesArray.length(); i++) {
                        String category = categoriesArray.getString(i);
                        if (!categories.contains(category)) {
                            categories.add(category);
                        }
                    }
                } catch (Exception e) {

                    String categoriesStr = json.optString("categories", "");
                    if (!categoriesStr.isEmpty()) {

                        if (categoriesStr.startsWith("[") && categoriesStr.endsWith("]")) {
                            try {
                                JSONArray categoriesArray = new JSONArray(categoriesStr);
                                for (int i = 0; i < categoriesArray.length(); i++) {
                                    String category = categoriesArray.getString(i);
                                    if (!categories.contains(category)) {
                                        categories.add(category);
                                    }
                                }
                            } catch (Exception jsonEx) {

                                if (!categories.contains(categoriesStr)) {
                                    categories.add(categoriesStr);
                                }
                            }
                        } else {

                            if (!categories.contains(categoriesStr)) {
                                categories.add(categoriesStr);
                            }
                        }
                    }
                }
            }


            ExerciseMedia media = new ExerciseMedia();
            if (json.has("media") && !json.isNull("media")) {
                JSONObject mediaObj = json.getJSONObject("media");
                String animationUrl = mediaObj.optString("animation_url", null);
                String previewImage = mediaObj.optString("preview_image", null);
                media = new ExerciseMedia(previewImage, animationUrl, null);
            }


            int defaultSets = 3;
            String defaultReps = "12";
            int defaultRestSeconds = 60;


            List<String> equipmentRequired = new ArrayList<>();
            if (json.has("equipment_required") && !json.isNull("equipment_required")) {
                JSONArray equipmentArray = json.getJSONArray("equipment_required");
                for (int i = 0; i < equipmentArray.length(); i++) {
                    String equipmentStr = equipmentArray.getString(i);
                    equipmentRequired.add(equipmentStr);
                }
            }


            List<String> muscleGroups = new ArrayList<>();
            if (json.has("primary_muscles") && !json.isNull("primary_muscles")) {
                JSONArray muscleGroupsArray = json.getJSONArray("primary_muscles");
                for (int i = 0; i < muscleGroupsArray.length(); i++) {
                    String muscleGroupStr = muscleGroupsArray.getString(i);
                    muscleGroups.add(muscleGroupStr);
                }
            }


            List<String> secondaryMuscles = new ArrayList<>();
            if (json.has("secondary_muscles") && !json.isNull("secondary_muscles")) {
                JSONArray secondaryMusclesArray = json.getJSONArray("secondary_muscles");
                for (int i = 0; i < secondaryMusclesArray.length(); i++) {
                    String muscleGroupStr = secondaryMusclesArray.getString(i);
                    secondaryMuscles.add(muscleGroupStr);
                }
            }


            List<String> stabilizerMuscles = new ArrayList<>();
            if (json.has("stabilizer_muscles") && !json.isNull("stabilizer_muscles")) {
                JSONArray stabilizerMusclesArray = json.getJSONArray("stabilizer_muscles");
                for (int i = 0; i < stabilizerMusclesArray.length(); i++) {
                    String muscleGroupStr = stabilizerMusclesArray.getString(i);
                    stabilizerMuscles.add(muscleGroupStr);
                }
            }


            String instructions = json.optString("instructions", "");


            List<String> commonMistakes = new ArrayList<>();
            if (json.has("common_mistakes") && !json.isNull("common_mistakes")) {
                JSONArray mistakesArray = json.getJSONArray("common_mistakes");
                for (int i = 0; i < mistakesArray.length(); i++) {
                    commonMistakes.add(mistakesArray.getString(i));
                }
            }


            String contraindications = json.optString("contraindications", "");
            var list = new ArrayList<String>();
            list.add(contraindications);

            float met = 0f;
            if (json.has("met") && !json.isNull("met")) {
                met = (float) json.getDouble("met");
            }


            Exercise.Builder builder = new Exercise.Builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .difficulty(difficulty)
                    .exerciseType(exerciseType)
                    .categories(categories)
                    .muscleGroups(muscleGroups)
                    .secondaryMuscles(secondaryMuscles)
                    .stabilizerMuscles(stabilizerMuscles)
                    .equipmentRequired(equipmentRequired)
                    .met(met)
                    .instructions(instructions)
                    .commonMistakes(commonMistakes)
                    .contraindications(list)
                    .media(media);

            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }


    public List<WorkoutPlan> parseWorkoutPlans(JSONArray jsonArray) throws JSONException {
        List<WorkoutPlan> workoutPlans = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            workoutPlans.add(parseWorkoutPlan(jsonObject));
        }
        return workoutPlans;
    }

    public WorkoutPlan parseWorkoutPlan(JSONObject json) throws JSONException {

        WorkoutPlan workoutPlan = new WorkoutPlan();

        workoutPlan.setId(json.getString("id"));
        workoutPlan.setUserId(json.getString("user_id"));
        workoutPlan.setName(json.optString("name", "Тренировка"));


        if (json.has("planned_date") && !json.isNull("planned_date")) {
            try {
                String dateStr = json.getString("planned_date");


                Date date = null;
                Exception lastException = null;

                String[] formats = {
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        "yyyy-MM-dd'T'HH:mm:ssXXX",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'"
                };

                for (String formatStr : formats) {
                    try {
                        SimpleDateFormat format = new SimpleDateFormat(formatStr, Locale.US);
                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                        date = format.parse(dateStr);
                        if (date != null) {

                            break;
                        }
                    } catch (Exception e) {
                        lastException = e;
                    }
                }

                if (date != null) {
                    workoutPlan.setPlannedDate(date.getTime());

                } else {

                    workoutPlan.setPlannedDate(System.currentTimeMillis());
                }
            } catch (Exception e) {
                workoutPlan.setPlannedDate(System.currentTimeMillis());
            }
        } else {
            workoutPlan.setPlannedDate(System.currentTimeMillis());
        }


        if (json.has("program_id") && !json.isNull("program_id")) {
            String programId = json.getString("program_id");
            workoutPlan.setProgramId(programId);
        }

        if (json.has("program_day_id") && !json.isNull("program_day_id")) {
            String programDayId = json.getString("program_day_id");
            workoutPlan.setProgramDayId(programDayId);
        } else {
        }


        String status = json.optString("status", "planned");
        workoutPlan.setStatus(status);

        String notes = json.optString("notes", "");
        workoutPlan.setNotes(notes);


        workoutPlan.setCompleted("completed".equals(status));
        workoutPlan.setMissed("missed".equals(status));


        if (json.has("created_at") && !json.isNull("created_at")) {
            try {
                String createdAtStr = json.getString("created_at");
                workoutPlan.setCreatedAt(parseIsoDateTime(createdAtStr));
            } catch (Exception e) {
                workoutPlan.setCreatedAt(System.currentTimeMillis());
            }
        } else {
            workoutPlan.setCreatedAt(System.currentTimeMillis());
        }

        if (json.has("updated_at") && !json.isNull("updated_at")) {
            try {
                String updatedAtStr = json.getString("updated_at");
                workoutPlan.setUpdatedAt(parseIsoDateTime(updatedAtStr));
            } catch (Exception e) {
                workoutPlan.setUpdatedAt(System.currentTimeMillis());
            }
        } else {
            workoutPlan.setUpdatedAt(System.currentTimeMillis());
        }

        return workoutPlan;
    }

    public long parseIsoDateTime(String isoDateTimeString) throws Exception {
        if (isoDateTimeString == null || isoDateTimeString.isEmpty()) {
            throw new Exception("Строка даты-времени не может быть пустой");
        }

        try {


            String normalizedDateString = isoDateTimeString;


            if (isoDateTimeString.contains(".") && isoDateTimeString.matches(".*\\.\\d{4,}.*")) {

                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})\\.(\\d{3})(\\d*)([+-]\\d{2}:\\d{2}|Z)"
                );
                java.util.regex.Matcher matcher = pattern.matcher(isoDateTimeString);

                if (matcher.matches()) {


                    normalizedDateString = matcher.group(1) + "." + matcher.group(2) + matcher.group(4);
                }
            }


            String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss"
            };

            Exception lastException = null;
            for (String format : formats) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = dateFormat.parse(normalizedDateString);
                    if (date != null) {
                        return date.getTime();
                    }
                } catch (java.text.ParseException e) {
                    lastException = e;

                }
            }


            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME;
                java.time.OffsetDateTime dateTime = java.time.OffsetDateTime.parse(normalizedDateString, formatter);
                return dateTime.toInstant().toEpochMilli();
            } catch (Exception e) {

            }


            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d+)([+-]\\d{2}:\\d{2}|Z)"
                );
                java.util.regex.Matcher matcher = pattern.matcher(isoDateTimeString);

                if (matcher.matches()) {
                    int year = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2)) - 1;
                    int day = Integer.parseInt(matcher.group(3));
                    int hour = Integer.parseInt(matcher.group(4));
                    int minute = Integer.parseInt(matcher.group(5));
                    int second = Integer.parseInt(matcher.group(6));

                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal.set(year, month, day, hour, minute, second);
                    cal.set(Calendar.MILLISECOND, 0);


                    String millisStr = matcher.group(7);
                    if (millisStr.length() > 3) {
                        millisStr = millisStr.substring(0, 3);
                    }
                    while (millisStr.length() < 3) {
                        millisStr += "0";
                    }
                    cal.set(Calendar.MILLISECOND, Integer.parseInt(millisStr));


                    String timezone = matcher.group(8);
                    if (timezone.equals("Z")) {

                    } else {

                        java.util.regex.Pattern tzPattern = java.util.regex.Pattern.compile("([+-])(\\d{2}):(\\d{2})");
                        java.util.regex.Matcher tzMatcher = tzPattern.matcher(timezone);
                        if (tzMatcher.matches()) {
                            String sign = tzMatcher.group(1);
                            int tzHour = Integer.parseInt(tzMatcher.group(2));
                            int tzMinute = Integer.parseInt(tzMatcher.group(3));
                            int offsetMillis = (tzHour * 60 + tzMinute) * 60 * 1000;
                            if (sign.equals("-")) {
                                offsetMillis = -offsetMillis;
                            }

                            cal.add(Calendar.MILLISECOND, -offsetMillis);
                        }
                    }

                    return cal.getTimeInMillis();
                }
            } catch (Exception ignored) {
            }


            if (lastException != null) {
                throw new Exception("Не удалось распарсить дату: " + isoDateTimeString);
            } else {
                throw new Exception("Неверный формат даты: " + isoDateTimeString);
            }
        } catch (Exception e) {
            throw new Exception("Не удалось преобразовать строку даты: " + e.getMessage());
        }
    }
}

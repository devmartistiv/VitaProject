package com.martist.vitamove.core.data.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.entityextraction.Entity;
import com.google.mlkit.nl.entityextraction.EntityAnnotation;
import com.google.mlkit.nl.entityextraction.EntityExtraction;
import com.google.mlkit.nl.entityextraction.EntityExtractor;
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions;
import com.martist.vitamove.core.domain.utils.PluralizationUtil;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.data.repository.SupabaseFoodRepository;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Portion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VoiceInputService implements RecognitionListener {
    private static final String TAG = "VoiceInputService";

    private final Context context;
    private final SpeechRecognizer speechRecognizer;
    private final EntityExtractor entityExtractor;
    private final FoodManager foodManager;
    private final SupabaseFoodRepository foodRepository;


    public interface VoiceInputCallback {
        void onListeningStarted();

        void onTextRecognized(String text);

        void onFoodRecognized(List<RecognizedFood> foods);

        void onError(String error);

        void onComplete();
    }


    public static class RecognizedFood {
        private final String name;
        private final float quantity;
        private final String unit;
        private final Food foundFood;
        private final Portion foundPortion;


        public RecognizedFood(String name, float quantity, String unit, Food foundFood) {
            this(name, quantity, unit, foundFood, null);
        }


        public RecognizedFood(String name, float quantity, String unit, Food foundFood, Portion foundPortion) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.foundFood = foundFood;
            this.foundPortion = foundPortion;
        }

        public String getName() {
            return name;
        }

        public float getQuantity() {
            return quantity;
        }

        public String getUnit() {
            return unit;
        }

        public Food getFoundFood() {
            return foundFood;
        }

        public Portion getFoundPortion() {
            return foundPortion;
        }


        public float getTotalWeightInGrams() {
            if (foundPortion != null) {

                return foundPortion.getWeight() * quantity;
            } else {

                return quantity;
            }
        }


        public String getDisplayQuantity() {
            if (foundPortion != null) {
                if (quantity == 1) {
                    return foundPortion.getName();
                } else {

                    String pluralForm = PluralizationUtil.getPlural(quantity, foundPortion.getName());
                    return String.format(Locale.getDefault(), "%.0f %s", quantity, pluralForm);
                }
            } else {
                return String.format(Locale.getDefault(), "%.1f %s", quantity, unit);
            }
        }

        @Override
        public String toString() {
            if (foundPortion != null) {
                return String.format(Locale.getDefault(), "%s: %s (%.0fг)", name, getDisplayQuantity(), getTotalWeightInGrams());
            } else {
                return String.format(Locale.getDefault(), "%s: %.1f %s", name, quantity, unit);
            }
        }
    }

    private VoiceInputCallback callback;
    private boolean isListening = false;


    public VoiceInputService(Context context, FoodManager foodManager, SupabaseFoodRepository foodRepository) {
        this.context = context;
        this.foodManager = foodManager;
        this.foodRepository = foodRepository;


        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);


        this.entityExtractor = EntityExtraction.getClient(
                new EntityExtractorOptions.Builder(EntityExtractorOptions.RUSSIAN)
                        .build()
        );


        downloadModelIfNeeded();
    }


    private void downloadModelIfNeeded() {
        entityExtractor.downloadModelIfNeeded()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "ML Kit модель для извлечения сущностей загружена успешно");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Ошибка загрузки ML Kit модели: " + e.getMessage(), e);
                    }
                });
    }


    public void startVoiceInput(VoiceInputCallback callback) {
        this.callback = callback;

        if (isListening) {
            Log.w(TAG, "Голосовой ввод уже активен");
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (callback != null) {
                callback.onError("Распознавание речи недоступно на этом устройстве");
            }
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите, какие продукты вы съели...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        Log.d(TAG, "Начинаем голосовой ввод");
        speechRecognizer.startListening(intent);
        isListening = true;
    }


    public void stopVoiceInput() {
        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "Голосовой ввод остановлен");
        }
    }


    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (entityExtractor != null) {
            entityExtractor.close();
        }
        isListening = false;
    }


    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "Готов к прослушиванию речи");
        if (callback != null) {
            callback.onListeningStarted();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Начало речи");
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "Конец речи");
        isListening = false;
    }

    @Override
    public void onError(int error) {
        isListening = false;
        String errorMessage = getErrorMessage(error);
        Log.e(TAG, "Ошибка распознавания речи: " + errorMessage);

        if (callback != null) {
            callback.onError(errorMessage);
        }
    }

    @Override
    public void onResults(Bundle results) {
        isListening = false;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches != null && !matches.isEmpty()) {
            String recognizedText = matches.get(0);
            Log.d(TAG, "Распознанный текст: " + recognizedText);

            if (callback != null) {
                callback.onTextRecognized(recognizedText);
            }


            processRecognizedText(recognizedText);
        } else {
            if (callback != null) {
                callback.onError("Не удалось распознать речь");
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            Log.d(TAG, "Промежуточный результат: " + matches.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }


    private void processRecognizedText(String text) {
        Log.d(TAG, "Обработка текста с помощью ML Kit: " + text);

        entityExtractor.annotate(text)
                .addOnSuccessListener(new OnSuccessListener<List<EntityAnnotation>>() {
                    @Override
                    public void onSuccess(List<EntityAnnotation> entityAnnotations) {
                        Log.d(TAG, "ML Kit извлечение сущностей завершено успешно. Найдено аннотаций: " + entityAnnotations.size());
                        parseEntitiesAndFindFoods(text, entityAnnotations);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Ошибка извлечения сущностей ML Kit: " + e.getMessage(), e);

                        parseTextSimple(text);
                    }
                });
    }


    private void parseEntitiesAndFindFoods(String originalText, List<EntityAnnotation> annotations) {
        List<RecognizedFood> recognizedFoods = new ArrayList<>();

        Log.d(TAG, "🎯 DEBUG: Начинаем обработку текста: '" + originalText + "'");


        List<Float> numbers = new ArrayList<>();
        for (EntityAnnotation annotation : annotations) {
            for (Entity entity : annotation.getEntities()) {
                String entityText = annotation.getAnnotatedText();
                if (isNumber(entityText)) {
                    try {
                        float number = Float.parseFloat(entityText);
                        numbers.add(number);
                        Log.d(TAG, "✅ DEBUG: Найдено число через ML Kit: " + number);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "❌ DEBUG: Не удалось парсить число: " + entityText);
                    }
                }
            }
        }


        if (numbers.isEmpty()) {
            numbers = extractNumbersFromText(originalText);
            Log.d(TAG, "🔍 DEBUG: Числа через ML Kit не найдены, используем regex. Найдено: " + numbers.size());
        } else {
            Log.d(TAG, "✅ DEBUG: Числа найдены через ML Kit: " + numbers);
        }


        List<String> foodCandidates = extractFoodCandidates(originalText, annotations);


        List<String> lemmatizedCandidates = lemmatizeFoodCandidates(foodCandidates);


        List<String> portionCandidates = extractPortionCandidates(originalText, annotations);

        Log.d(TAG, "📊 DEBUG: Итого найдено - чисел: " + numbers.size() +
                ", кандидатов в продукты: " + foodCandidates.size() +
                ", после лемматизации: " + lemmatizedCandidates.size() +
                ", кандидатов в порции: " + portionCandidates.size());
        Log.d(TAG, "📊 DEBUG: Числа: " + numbers + ", Продукты: " + lemmatizedCandidates + ", Порции: " + portionCandidates);


        searchFoodsWithPortions(lemmatizedCandidates, portionCandidates, numbers, recognizedFoods, originalText);
    }


    private List<Float> extractNumbersFromText(String text) {
        List<Float> numbers = new ArrayList<>();


        List<String> percentages = extractPercentages(text);
        Set<Float> percentageNumbers = new HashSet<>();

        for (String percentage : percentages) {
            try {
                String numberPart = percentage.replace("%", "").replace(',', '.');
                float percentNumber = Float.parseFloat(numberPart);
                percentageNumbers.add(percentNumber);
                Log.d(TAG, "⚠️ DEBUG: Исключаем число " + percentNumber + " (часть процента " + percentage + ")");
            } catch (NumberFormatException e) {

            }
        }


        Pattern numberPattern = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)\\b");
        Matcher matcher = numberPattern.matcher(text);

        while (matcher.find()) {
            try {
                String numberStr = matcher.group(1).replace(',', '.');
                float number = Float.parseFloat(numberStr);

                if (!percentageNumbers.contains(number)) {
                    numbers.add(number);
                    Log.d(TAG, "✅ DEBUG: Найдено число для количества (цифра): " + number);
                } else {
                    Log.d(TAG, "❌ DEBUG: Пропускаем число " + number + " (часть процента)");
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Не удалось парсить число: " + matcher.group(1));
            }
        }


        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");

        for (String word : words) {
            String cleanWord = word.replaceAll("[^а-яё]", "");
            if (RUSSIAN_NUMBERS.containsKey(cleanWord)) {
                float number = RUSSIAN_NUMBERS.get(cleanWord);
                if (!percentageNumbers.contains(number)) {
                    numbers.add(number);
                    Log.d(TAG, "✅ DEBUG: Найдено число для количества (слово): '" + cleanWord + "' = " + number);
                } else {
                    Log.d(TAG, "❌ DEBUG: Пропускаем число " + number + " (часть процента)");
                }
            }
        }

        Log.d(TAG, "🔢 DEBUG: Итого чисел для количества (исключая проценты): " + numbers.size());
        return numbers;
    }


    private static final Map<String, Float> RUSSIAN_NUMBERS = new HashMap<String, Float>() {{
        put("ноль", 0f);
        put("один", 1f);
        put("одна", 1f);
        put("одно", 1f);
        put("два", 2f);
        put("две", 2f);
        put("три", 3f);
        put("четыре", 4f);
        put("пять", 5f);
        put("шесть", 6f);
        put("семь", 7f);
        put("восемь", 8f);
        put("девять", 9f);
        put("десять", 10f);
        put("одиннадцать", 11f);
        put("двенадцать", 12f);
        put("тринадцать", 13f);
        put("четырнадцать", 14f);
        put("пятнадцать", 15f);
        put("шестнадцать", 16f);
        put("семнадцать", 17f);
        put("восемнадцать", 18f);
        put("девятнадцать", 19f);
        put("двадцать", 20f);
        put("тридцать", 30f);
        put("сорок", 40f);
        put("пятьдесят", 50f);
        put("полтора", 1.5f);
        put("половина", 0.5f);
        put("четверть", 0.25f);
    }};


    private Map<Integer, Float> extractNumbersWithPositions(String text) {
        Map<Integer, Float> numbersWithPositions = new HashMap<>();

        Log.d(TAG, "🔢 DEBUG: Извлекаем числа с позициями из текста: '" + text + "'");


        List<String> percentages = extractPercentages(text);
        Set<Float> percentageNumbers = new HashSet<>();

        for (String percentage : percentages) {
            try {
                String numberPart = percentage.replace("%", "").replace(',', '.');
                float percentNumber = Float.parseFloat(numberPart);
                percentageNumbers.add(percentNumber);
                Log.d(TAG, "⚠️ DEBUG: Исключаем число " + percentNumber + " из позиций (часть процента " + percentage + ")");
            } catch (NumberFormatException e) {

            }
        }


        Pattern numberPattern = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)\\b");
        Matcher matcher = numberPattern.matcher(text);

        while (matcher.find()) {
            try {
                String numberStr = matcher.group(1).replace(',', '.');
                float number = Float.parseFloat(numberStr);
                int position = matcher.start();

                if (!percentageNumbers.contains(number)) {
                    numbersWithPositions.put(position, number);
                    Log.d(TAG, "✅ DEBUG: Найдено число-цифра с позицией: " + number + " на позиции " + position);
                } else {
                    Log.d(TAG, "❌ DEBUG: Пропускаем число " + number + " на позиции " + position + " (часть процента)");
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "❌ DEBUG: Не удалось парсить число: " + matcher.group(1));
            }
        }


        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");
        int currentPos = 0;

        for (String word : words) {
            int wordStart = lowerText.indexOf(word, currentPos);
            String cleanWord = word.replaceAll("[^а-яё]", "");

            if (RUSSIAN_NUMBERS.containsKey(cleanWord)) {
                float number = RUSSIAN_NUMBERS.get(cleanWord);
                if (!percentageNumbers.contains(number)) {
                    numbersWithPositions.put(wordStart, number);
                    Log.d(TAG, "✅ DEBUG: Найдено число-слово с позицией: '" + cleanWord + "' = " + number + " на позиции " + wordStart);
                } else {
                    Log.d(TAG, "❌ DEBUG: Пропускаем число-слово " + number + " на позиции " + wordStart + " (часть процента)");
                }
            }

            currentPos = wordStart + word.length();
        }

        Log.d(TAG, "🔢 DEBUG: Итого найдено чисел с позициями (исключая проценты): " + numbersWithPositions.size());
        return numbersWithPositions;
    }


    private Map<Integer, String> extractPortionsWithPositions(String text) {
        Map<Integer, String> portionsWithPositions = new HashMap<>();

        Log.d(TAG, "🥄 DEBUG: Извлекаем порции с позициями из текста: '" + text + "'");

        if (text == null || text.trim().isEmpty()) {
            Log.d(TAG, "🥄 DEBUG: Текст пустой, возвращаем пустую карту");
            return portionsWithPositions;
        }

        String normalizedText = text.toLowerCase();


        String[] words = normalizedText.split("\\s+");
        int currentPos = 0;

        Log.d(TAG, "🥄 DEBUG: Разбили текст на слова: " + java.util.Arrays.toString(words));

        for (String word : words) {
            int wordStart = normalizedText.indexOf(word, currentPos);
            String cleanWord = word.replaceAll("[^а-яё]", "");

            Log.d(TAG, "🥄 DEBUG: Проверяем слово '" + word + "' (очищенное: '" + cleanWord + "') на позиции " + wordStart);


            if (isStandardUnit(cleanWord)) {
                portionsWithPositions.put(wordStart, cleanWord);
                Log.d(TAG, "✅ DEBUG: Найдена стандартная единица: '" + cleanWord + "' на позиции " + wordStart);
            } else if (cleanWord.length() > 2) {

                String lemma = FOOD_LEMMAS.get(cleanWord);
                if (lemma != null && isPortionWord(lemma)) {
                    portionsWithPositions.put(wordStart, cleanWord);
                    Log.d(TAG, "✅ DEBUG: Найдена порция через словарь: '" + cleanWord + "' → '" + lemma + "' на позиции " + wordStart);
                } else if (isPortionWord(cleanWord)) {
                    portionsWithPositions.put(wordStart, cleanWord);
                    Log.d(TAG, "✅ DEBUG: Найдена порция напрямую: '" + cleanWord + "' на позиции " + wordStart);
                } else {
                    Log.d(TAG, "❌ DEBUG: Слово '" + cleanWord + "' не является порцией");
                }
            } else {
                Log.d(TAG, "❌ DEBUG: Слово '" + cleanWord + "' слишком короткое (< 3 символов)");
            }

            currentPos = wordStart + word.length();
        }

        Log.d(TAG, "🥄 DEBUG: Итого найдено порций с позициями: " + portionsWithPositions.size());
        return portionsWithPositions;
    }


    private boolean isStandardUnit(String word) {
        if (word == null || word.trim().isEmpty()) return false;

        String lowerWord = word.toLowerCase();


        String[] standardUnits = {

                "г", "грамм", "граммов", "грамма", "гр",
                "кг", "килограмм", "килограммов", "килограмма", "кило",


                "мл", "миллилитр", "миллилитров", "миллилитра",
                "л", "литр", "литров", "литра",


                "шт", "штук", "штуки", "штука", "штучек", "штучки", "штучка",
                "кусок", "кусков", "куска", "куски", "кусочек", "кусочков", "кусочка", "кусочки",


                "порция", "порций", "порции"
        };

        for (String unit : standardUnits) {
            if (lowerWord.equals(unit)) {
                Log.d(TAG, "📏 DEBUG: Слово '" + word + "' является стандартной единицей: '" + unit + "'");
                return true;
            }
        }

        return false;
    }


    private Map<String, Float> matchNumbersToPortions(String text, List<String> portionCandidates) {
        Map<String, Float> portionQuantities = new HashMap<>();

        Log.d(TAG, "🧠 DEBUG: Начинаем умное сопоставление для текста: '" + text + "'");
        Log.d(TAG, "🧠 DEBUG: Кандидаты в порции: " + portionCandidates);

        Map<Integer, Float> numbersWithPositions = extractNumbersWithPositions(text);
        Map<Integer, String> portionsWithPositions = extractPortionsWithPositions(text);

        Log.d(TAG, "🧠 DEBUG: Числа с позициями: " + numbersWithPositions);
        Log.d(TAG, "🧠 DEBUG: Порции с позициями: " + portionsWithPositions);


        for (Map.Entry<Integer, String> portionEntry : portionsWithPositions.entrySet()) {
            int portionPos = portionEntry.getKey();
            String portion = portionEntry.getValue();

            Log.d(TAG, "🔍 DEBUG: Ищем число для порции '" + portion + "' на позиции " + portionPos);

            Float closestNumber = null;
            int closestDistance = Integer.MAX_VALUE;


            for (Map.Entry<Integer, Float> numberEntry : numbersWithPositions.entrySet()) {
                int numberPos = numberEntry.getKey();
                float number = numberEntry.getValue();

                int distance = Math.abs(portionPos - numberPos);


                if (numberPos < portionPos) {
                    distance = distance / 2;
                    Log.d(TAG, "🎯 DEBUG: Число " + number + " на позиции " + numberPos + " слева от порции, расстояние с бонусом: " + distance);
                } else {
                    Log.d(TAG, "➡️ DEBUG: Число " + number + " на позиции " + numberPos + " справа от порции, расстояние: " + distance);
                }

                if (distance < closestDistance && distance < 50) {
                    closestDistance = distance;
                    closestNumber = number;
                    Log.d(TAG, "✅ DEBUG: Новое ближайшее число: " + number + ", расстояние: " + distance);
                }
            }

            if (closestNumber != null) {
                portionQuantities.put(portion, closestNumber);
                Log.d(TAG, "🎉 DEBUG: Сопоставлено: '" + portion + "' с количеством " + closestNumber);
            } else {
                portionQuantities.put(portion, 1f);
                Log.d(TAG, "⚠️ DEBUG: Для порции '" + portion + "' не найдено число, используем 1");
            }
        }

        Log.d(TAG, "🏁 DEBUG: Результат умного сопоставления: " + portionQuantities);
        return portionQuantities;
    }


    private List<String> extractFoodCandidates(String text, List<EntityAnnotation> annotations) {
        List<String> candidates = new ArrayList<>();


        for (EntityAnnotation annotation : annotations) {
            String annotatedText = annotation.getAnnotatedText().trim();
            if (annotatedText.length() > 2 && !isNumber(annotatedText)) {
                candidates.add(annotatedText);
                Log.d(TAG, "Найден кандидат в продукты (ML Kit): " + annotatedText);
            }
        }


        if (candidates.isEmpty()) {

            List<String> percentages = extractPercentages(text);
            candidates = extractFoodCandidatesSimple(text, percentages);
        }

        return candidates;
    }


    private List<String> extractFoodCandidatesSimple(String text, List<String> percentages) {
        List<String> candidates = new ArrayList<>();


        Set<String> percentageWords = createPercentageWordsSet(percentages, text);
        Log.d(TAG, "🚫 DEBUG: Слова процентов для исключения: " + percentageWords);


        String cleanText = text.toLowerCase()
                .replaceAll("\\b\\d+(?:[.,]\\d+)?\\b", "")
                .replaceAll("\\b(грамм|г|килограмм|кг|миллилитр|мл|литр|л|штук|штуки|штука|шт)\\b", "")
                .replaceAll("\\b(съел|съела|выпил|выпила|ел|ела|пил|пила|добавить|добавил|добавила)\\b", "")
                .replaceAll("\\b(и|а|но|или|да|нет|ещё|еще|также|тоже)\\b", "")
                .replaceAll("[.,!?;:]", "")
                .trim();


        String[] words = cleanText.split("\\s+");
        Set<String> usedWords = new HashSet<>();


        Set<String> quantities = new HashSet<>();
        quantities.addAll(Arrays.asList("один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять", "десять"));

        Set<String> portions = new HashSet<>();
        portions.addAll(Arrays.asList("стакан", "стакана", "стаканов", "чашка", "чашки", "чашек",
                "ложка", "ложки", "ложек", "тарелка", "тарелки", "тарелок",
                "кусок", "куска", "кусков", "ломтик", "ломтика", "ломтиков"));

        Set<String> modifiers = new HashSet<>();
        modifiers.addAll(Arrays.asList("пшеничный", "ржаной", "черный", "белый", "красный",
                "зеленый", "желтый", "синий", "большой", "маленький",
                "свежий", "сухой", "мокрый", "горячий", "холодный"));


        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i].trim();
            String word2 = words[i + 1].trim();

            if (word1.length() > 2 && word2.length() > 2 &&
                    !word1.matches("\\d+") && !word2.matches("\\d+")) {


                boolean isValidBigram = false;
                String bigramType = "";


                if (!quantities.contains(word1) && !portions.contains(word1) && modifiers.contains(word2)) {
                    isValidBigram = true;
                    bigramType = "продукт + модификатор";
                } else if (modifiers.contains(word1) && !quantities.contains(word2) && !portions.contains(word2)) {
                    isValidBigram = true;
                    bigramType = "модификатор + продукт";
                } else if (!quantities.contains(word1) && !portions.contains(word1) &&
                        !modifiers.contains(word1) && word2.matches(".*%.*")) {
                    isValidBigram = true;
                    bigramType = "продукт + процент";
                }

                if (isValidBigram) {
                    String bigram = word1 + " " + word2;
                    candidates.add(bigram);


                    usedWords.add(word1);
                    usedWords.add(word2);

                    Log.d(TAG, "✅ Найден кандидат-биграм (" + bigramType + "): " + bigram);
                    Log.d(TAG, "🚫 Исключаем слова из отдельного поиска: '" + word1 + "', '" + word2 + "'");
                } else {
                    Log.d(TAG, "❌ Пропущен невалидный биграм: " + word1 + " " + word2 +
                            " (количество=" + quantities.contains(word1) + "/" + quantities.contains(word2) +
                            ", порция=" + portions.contains(word1) + "/" + portions.contains(word2) +
                            ", модификатор=" + modifiers.contains(word1) + "/" + modifiers.contains(word2) +
                            ", процент=" + word2.matches(".*%.*") + ")");
                }
            }
        }


        for (String word : words) {
            word = word.trim();
            if (word.length() > 3 && !word.matches("\\d+") &&
                    !word.matches(".*%.*") &&
                    !percentageWords.contains(word) &&
                    !modifiers.contains(word) &&
                    !quantities.contains(word) &&
                    !portions.contains(word) &&
                    !usedWords.contains(word)) {
                candidates.add(word);
                Log.d(TAG, "✅ Найден кандидат в продукты (отдельное слово): " + word);
            } else {
                String reason = "";
                if (usedWords.contains(word)) reason = "уже использовано в биграмме";
                else if (modifiers.contains(word)) reason = "модификатор";
                else if (quantities.contains(word)) reason = "количество";
                else if (portions.contains(word)) reason = "порция";
                else if (word.matches(".*%.*")) reason = "содержит процент";
                else if (percentageWords.contains(word)) reason = "часть процента";
                else if (word.length() <= 3) reason = "слишком короткое";
                else if (word.matches("\\d+")) reason = "число";

                Log.d(TAG, "🚫 Слово '" + word + "' пропущено - " + reason);
            }
        }

        return candidates;
    }


    private void searchFoodsInDatabase(List<String> foodCandidates, List<Float> numbers, List<RecognizedFood> recognizedFoods) {

        new Thread(() -> {
            int foundCount = 0;


            Set<String> foundProductNames = new HashSet<>();

            for (int i = 0; i < foodCandidates.size(); i++) {
                String candidate = foodCandidates.get(i);

                Log.d(TAG, "🔍 Простой поиск по кандидату: '" + candidate + "' (позиция " + i + ")");


                List<Food> foundFoods = foodRepository.searchFoodsByQuery(candidate);

                if (foundFoods != null && !foundFoods.isEmpty()) {


                    if (candidate.contains(" ")) {

                        foundFoods = filterByExactMatch(foundFoods, candidate);
                    }


                    foundFoods = excludeAlreadyFoundProducts(foundFoods, foundProductNames);

                    if (foundFoods.isEmpty()) {
                        Log.d(TAG, "⚠️ Продукт '" + candidate + "' отфильтрован (неточное совпадение или уже найден)");
                        continue;
                    }

                    Food bestMatch = findBestMatch(candidate, foundFoods);

                    if (bestMatch != null) {

                        float quantity = 100f;
                        if (i < numbers.size()) {
                            quantity = numbers.get(i);
                        } else if (!numbers.isEmpty()) {
                            quantity = numbers.get(0);
                        }

                        RecognizedFood recognizedFood = new RecognizedFood(
                                bestMatch.getName(),
                                quantity,
                                "г",
                                bestMatch
                        );

                        recognizedFoods.add(recognizedFood);
                        foundCount++;


                        foundProductNames.add(bestMatch.getName().toLowerCase().trim());

                        Log.d(TAG, "✅ Найден продукт: " + bestMatch.getName() +
                                ", количество: " + quantity + "г (добавлен в исключения)");
                    }
                }
            }

            Log.d(TAG, "Найдено продуктов в базе: " + foundCount + " из " + foodCandidates.size() + " кандидатов");


            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onFoodRecognized(recognizedFoods);
                });
            }

            callback.onComplete();
        }).start();
    }


    private void searchFoodsWithPortions(List<String> foodCandidates, List<String> portionCandidates,
                                         List<Float> numbers, List<RecognizedFood> recognizedFoods, String originalText) {

        new Thread(() -> {
            int foundCount = 0;


            List<String> percentages = extractPercentages(originalText);


            Set<String> foundProductNames = new HashSet<>();

            for (int i = 0; i < foodCandidates.size(); i++) {
                String candidate = foodCandidates.get(i);

                Log.d(TAG, "🔍 Ищем продукт по кандидату: '" + candidate + "' (позиция " + i + ")");


                List<Food> foundFoods = foodRepository.searchFoodsByQuery(candidate);

                if (foundFoods != null && !foundFoods.isEmpty()) {

                    List<Food> rankedFoods = filterAndRankFoodsByPercentage(foundFoods, percentages, foodCandidates, originalText);


                    if (candidate.contains(" ")) {

                        rankedFoods = filterByExactMatch(rankedFoods, candidate);
                    }


                    rankedFoods = excludeAlreadyFoundProducts(rankedFoods, foundProductNames);

                    if (rankedFoods.isEmpty()) {
                        Log.d(TAG, "⚠️ Продукт '" + candidate + "' отфильтрован (неточное совпадение или уже найден)");
                        continue;
                    }


                    Food bestMatch = rankedFoods.get(0);

                    if (bestMatch != null) {

                        float quantity = 1f;
                        if (i < numbers.size()) {
                            quantity = numbers.get(i);
                        } else if (!numbers.isEmpty()) {
                            quantity = numbers.get(0);
                        }


                        RecognizedFood baseFood = new RecognizedFood(
                                bestMatch.getName(),
                                quantity,
                                "г",
                                bestMatch
                        );

                        recognizedFoods.add(baseFood);
                        foundCount++;


                        foundProductNames.add(bestMatch.getName().toLowerCase().trim());

                        Log.d(TAG, "✅ Найден продукт: " + bestMatch.getName() +
                                ", количество: " + quantity + " (добавлен в исключения)");
                    }
                }
            }


            List<RecognizedFood> foodsWithPortions = findPortionsForFoods(recognizedFoods, portionCandidates, numbers, originalText);


            recognizedFoods.clear();
            recognizedFoods.addAll(foodsWithPortions);


            int portionCount = 0;
            for (RecognizedFood food : foodsWithPortions) {
                if (food.getFoundPortion() != null) {
                    portionCount++;
                }
            }

            Log.d(TAG, "Поиск завершен. Найдено " + foundCount + " продуктов, " +
                    "из них с порциями: " + portionCount);


            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onFoodRecognized(recognizedFoods);
                });
            }

            callback.onComplete();
        }).start();
    }


    private Food findBestMatch(String searchTerm, List<Food> foundFoods) {
        if (foundFoods.isEmpty()) return null;

        String searchLower = searchTerm.toLowerCase();
        Food bestMatch = foundFoods.get(0);
        int bestScore = 0;

        for (Food food : foundFoods) {
            String foodName = food.getName().toLowerCase();
            int score = 0;


            if (foodName.equals(searchLower)) {
                score = 100;
            } else if (foodName.startsWith(searchLower)) {
                score = 80;
            } else if (foodName.contains(searchLower)) {
                score = 60;
            } else if (searchLower.contains(foodName)) {
                score = 40;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = food;
            }
        }

        return bestScore > 0 ? bestMatch : null;
    }


    private void parseTextSimple(String text) {
        Log.d(TAG, "Используем простой метод обработки текста с лемматизацией и порциями");

        List<Float> numbers = extractNumbersFromText(text);
        List<String> percentages = extractPercentages(text);
        List<String> foodCandidates = extractFoodCandidatesSimple(text, percentages);


        List<String> lemmatizedCandidates = lemmatizeFoodCandidates(foodCandidates);


        List<String> portionCandidates = extractPortionCandidates(text, new ArrayList<>());

        Log.d(TAG, "Простой метод: найдено " + foodCandidates.size() +
                " кандидатов, после лемматизации: " + lemmatizedCandidates.size() +
                ", кандидатов в порции: " + portionCandidates.size() +
                ", процентов: " + percentages.size());

        List<RecognizedFood> recognizedFoods = new ArrayList<>();
        searchFoodsWithPortions(lemmatizedCandidates, portionCandidates, numbers, recognizedFoods, text);
    }


    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Ошибка записи аудио";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Ошибка клиента";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Недостаточно разрешений для записи аудио";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Ошибка сети";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Таймаут сети";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Не удалось распознать речь";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Сервис распознавания занят";
            case SpeechRecognizer.ERROR_SERVER:
                return "Ошибка сервера";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Таймаут речи";
            default:
                return "Неизвестная ошибка: " + error;
        }
    }


    private boolean isNumber(String text) {
        try {
            Float.parseFloat(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private static final java.util.Map<String, String> FOOD_LEMMAS = new java.util.HashMap<String, String>() {{

        put("молока", "молоко");
        put("молочка", "молоко");
        put("творога", "творог");
        put("творожка", "творог");
        put("сметаны", "сметана");
        put("сметанки", "сметана");
        put("кефира", "кефир");
        put("кефирчика", "кефир");
        put("йогурта", "йогурт");
        put("сыра", "сыр");
        put("сырка", "сыр");
        put("масла", "масло");
        put("маслица", "масло");
        put("сливок", "сливки");
        put("айрана", "айран");
        put("ряженки", "ряженка");


        put("мяса", "мясо");
        put("мясца", "мясо");
        put("курицы", "курица");
        put("курочки", "курица");
        put("куриного", "куриный");
        put("говядины", "говядина");
        put("говяжьего", "говяжий");
        put("свинины", "свинина");
        put("свиного", "свиной");
        put("рыбы", "рыба");
        put("рыбки", "рыба");
        put("колбасы", "колбаса");
        put("колбаски", "колбаса");
        put("ветчины", "ветчина");
        put("бекона", "бекон");
        put("сосисок", "сосиски");
        put("сарделек", "сардельки");
        put("индейки", "индейка");
        put("утки", "утка");
        put("баранины", "баранина");
        put("лосося", "лосось");
        put("семги", "семга");
        put("трески", "треска");
        put("горбуши", "горбуша");
        put("тунца", "тунец");
        put("скумбрии", "скумбрия");
        put("селедки", "селедка");
        put("сельди", "сельдь");
        put("анчоусов", "анчоусы");
        put("креветок", "креветки");
        put("мидий", "мидии");
        put("кальмаров", "кальмары");
        put("устриц", "устрицы");
        put("икры", "икра");
        put("икорки", "икра");


        put("каши", "каша");
        put("кашки", "каша");
        put("риса", "рис");
        put("рисика", "рис");
        put("гречки", "гречка");
        put("гречневой", "гречневый");
        put("овсянки", "овсянка");
        put("овсяной", "овсяный");
        put("пшенки", "пшено");
        put("перловки", "перловка");
        put("ячневой", "ячневый");
        put("манки", "манка");
        put("манной", "манный");
        put("булгура", "булгур");
        put("киноа", "киноа");
        put("кускуса", "кускус");
        put("полбы", "полба");
        put("амаранта", "амарант");


        put("макарон", "макароны");
        put("макаронов", "макароны");
        put("пасты", "паста");
        put("спагетти", "спагетти");
        put("пенне", "пенне");
        put("фарфалле", "фарфалле");
        put("хлеба", "хлеб");
        put("хлебушка", "хлеб");
        put("батона", "батон");
        put("багета", "багет");
        put("лаваша", "лаваш");
        put("питы", "пита");
        put("булочки", "булочка");
        put("круассана", "круассан");
        put("бриоши", "бриошь");


        put("картошки", "картошка");
        put("картофеля", "картофель");
        put("картошечки", "картошка");
        put("моркови", "морковь");
        put("морковки", "морковь");
        put("капусты", "капуста");
        put("капустки", "капуста");
        put("лука", "лук");
        put("лучка", "лук");
        put("помидоров", "помидор");
        put("помидорчиков", "помидор");
        put("томатов", "томат");
        put("огурцов", "огурец");
        put("огурчиков", "огурец");
        put("перца", "перец");
        put("перчика", "перец");
        put("баклажанов", "баклажан");
        put("баклажанчиков", "баклажан");
        put("кабачков", "кабачок");
        put("кабачка", "кабачок");
        put("свеклы", "свекла");
        put("свеколки", "свекла");
        put("редиски", "редиска");
        put("редьки", "редька");
        put("чеснока", "чеснок");
        put("чесночка", "чеснок");
        put("зелени", "зелень");
        put("петрушки", "петрушка");
        put("укропа", "укроп");
        put("салата", "салат");
        put("шпината", "шпинат");
        put("руколы", "рукола");
        put("базилика", "базилик");
        put("кинзы", "кинза");
        put("брокколи", "брокколи");
        put("цветной", "цветной");
        put("спаржи", "спаржа");
        put("артишока", "артишок");
        put("тыквы", "тыква");
        put("кукурузы", "кукуруза");
        put("кукурузки", "кукуруза");
        put("горошка", "горошек");
        put("фасоли", "фасоль");
        put("бобов", "бобы");
        put("нута", "нут");
        put("чечевицы", "чечевица");


        put("яблок", "яблоко");
        put("яблочек", "яблоко");
        put("груш", "груша");
        put("грушек", "груша");
        put("бананов", "банан");
        put("бананчиков", "банан");
        put("апельсинов", "апельсин");
        put("апельсинчиков", "апельсин");
        put("мандаринов", "мандарин");
        put("мандаринчиков", "мандарин");
        put("лимонов", "лимон");
        put("лимончиков", "лимон");
        put("лаймов", "лайм");
        put("грейпфрутов", "грейпфрут");
        put("киви", "киви");
        put("ананаса", "ананас");
        put("ананасика", "ананас");
        put("персиков", "персик");
        put("персичков", "персик");
        put("абрикосов", "абрикос");
        put("абрикосиков", "абрикос");
        put("слив", "слива");
        put("сливок", "слива");
        put("вишни", "вишня");
        put("вишенки", "вишня");
        put("черешни", "черешня");
        put("клубники", "клубника");
        put("клубнички", "клубника");
        put("малины", "малина");
        put("малинки", "малина");
        put("черники", "черника");
        put("чернички", "черника");
        put("голубики", "голубика");
        put("смородины", "смородина");
        put("крыжовника", "крыжовник");
        put("винограда", "виноград");
        put("виноградинок", "виноград");
        put("арбуза", "арбуз");
        put("арбузика", "арбуз");
        put("дыни", "дыня");
        put("дынки", "дыня");
        put("авокадо", "авокадо");
        put("манго", "манго");
        put("папайи", "папайя");
        put("граната", "гранат");
        put("гранатика", "гранат");
        put("хурмы", "хурма");
        put("инжира", "инжир");
        put("фиников", "финики");
        put("изюма", "изюм");
        put("кураги", "курага");
        put("чернослива", "чернослив");


        put("орехов", "орехи");
        put("орешков", "орехи");
        put("грецких", "грецкий");
        put("миндаля", "миндаль");
        put("фундука", "фундук");
        put("арахиса", "арахис");
        put("кешью", "кешью");
        put("фисташек", "фисташки");
        put("семечек", "семечки");
        put("семян", "семена");
        put("кунжута", "кунжут");


        put("сока", "сок");
        put("сочка", "сок");
        put("воды", "вода");
        put("водички", "вода");
        put("чая", "чай");
        put("чайка", "чай");
        put("кофе", "кофе");
        put("кофейка", "кофе");
        put("компота", "компот");
        put("морса", "морс");
        put("лимонада", "лимонад");
        put("газировки", "газировка");
        put("колы", "кола");
        put("пепси", "пепси");
        put("спрайта", "спрайт");
        put("фанты", "фанта");
        put("квасца", "квас");
        put("пива", "пиво");
        put("вина", "вино");


        put("конфет", "конфеты");
        put("конфеток", "конфеты");
        put("шоколада", "шоколад");
        put("шоколадки", "шоколад");
        put("печенья", "печенье");
        put("печенюшки", "печенье");
        put("пряников", "пряники");
        put("вафель", "вафли");
        put("вафелек", "вафли");
        put("тортика", "торт");
        put("пирожного", "пирожное");
        put("блинов", "блины");
        put("блинчиков", "блины");
        put("оладий", "оладьи");
        put("сырника", "сырники");
        put("варенья", "варенье");
        put("повидла", "повидло");
        put("джема", "джем");
        put("меда", "мед");
        put("медка", "мед");
        put("сахара", "сахар");
        put("сахарка", "сахар");


        put("пиццы", "пицца");
        put("пиццки", "пицца");
        put("бургера", "бургер");
        put("чизбургера", "чизбургер");
        put("гамбургера", "гамбургер");
        put("роллов", "роллы");
        put("суши", "суши");
        put("наггетсов", "наггетсы");
        put("картошки фри", "картофель фри");
        put("фри", "картофель фри");
        put("пельменей", "пельмени");
        put("пельмешек", "пельмени");
        put("вареников", "вареники");
        put("мантов", "манты");
        put("котлет", "котлеты");
        put("котлеток", "котлеты");
        put("сосисок", "сосиски");
        put("сосисочек", "сосиски");
        put("салата", "салат");
        put("салатика", "салат");
        put("супа", "суп");
        put("супчика", "суп");
        put("борща", "борщ");
        put("щей", "щи");
        put("солянки", "солянка");
        put("окрошки", "окрошка");
        put("ухи", "уха");


        put("яиц", "яйцо");
        put("яичек", "яйцо");
        put("яйца", "яйцо");
        put("омлета", "омлет");
        put("яичницы", "яичница");


        put("масла", "масло");
        put("маслица", "масло");
        put("сливочного", "сливочный");
        put("растительного", "растительный");
        put("оливкового", "оливковый");
        put("подсолнечного", "подсолнечный");
        put("маргарина", "маргарин");
        put("спреда", "спред");
        put("майонеза", "майонез");
        put("сметаны", "сметана");


        put("соли", "соль");
        put("солички", "соль");
        put("перца", "перец");
        put("перчика", "перец");
        put("соуса", "соус");
        put("кетчупа", "кетчуп");
        put("горчицы", "горчица");
        put("хрена", "хрен");
        put("уксуса", "уксус");
        put("специй", "специи");
        put("приправы", "приправа");


        put("свежего", "свежий");
        put("свежей", "свежий");
        put("жареного", "жареный");
        put("жареной", "жареный");
        put("вареного", "вареный");
        put("вареной", "вареный");
        put("тушеного", "тушеный");
        put("тушеной", "тушеный");
        put("печеного", "печеный");
        put("печеной", "печеный");
        put("запеченного", "запеченный");
        put("запеченной", "запеченный");
        put("копченого", "копченый");
        put("копченой", "копченый");
        put("соленого", "соленый");
        put("соленой", "соленый");
        put("маринованного", "маринованный");
        put("маринованной", "маринованный");
        put("консервированного", "консервированный");
        put("консервированной", "консервированный");
        put("замороженного", "замороженный");
        put("замороженной", "замороженный");
        put("сушеного", "сушеный");
        put("сушеной", "сушеный");
        put("домашнего", "домашний");
        put("домашней", "домашний");
        put("магазинного", "магазинный");
        put("магазинной", "магазинный");
        put("покупного", "покупной");
        put("покупной", "покупной");
        put("деревенского", "деревенский");
        put("деревенской", "деревенский");
        put("фермерского", "фермерский");
        put("фермерской", "фермерский");
        put("органического", "органический");
        put("органической", "органический");
        put("диетического", "диетический");
        put("диетической", "диетический");
        put("низкокалорийного", "низкокалорийный");
        put("низкокалорийной", "низкокалорийный");
        put("обезжиренного", "обезжиренный");
        put("обезжиренной", "обезжиренный");


        put("даниссимо", "Danone Даниссимо");
        put("простоквашино", "Простоквашино");
        put("вкуснотеево", "Вкуснотеево");
        put("савушкин", "Савушкин");
        put("президент", "Президент");
        put("валио", "Valio");
        put("кампина", "Campina");
        put("эрмигурт", "Ehrmann Эрмигурт");
        put("фрутис", "Fruttis");
        put("активиа", "Активиа");
        put("данон", "Danone");
        put("юбилейное", "Юбилейное");
        put("мария", "Мария");
        put("орео", "Орео");
        put("китката", "KitKat");
        put("сникерса", "Snickers");
        put("марса", "Mars");
        put("баунти", "Bounty");
        put("твикса", "Twix");
        put("милки", "Milky Way");
        put("кока", "Кока-Кола");
        put("колы", "Кока-Кола");
        put("пепси", "Пепси");
        put("спрайта", "Спрайт");
        put("фанты", "Фанта");
        put("макдоналдс", "McDonald's");
        put("бургер", "Burger King");
        put("кфс", "KFC");
        put("макмаффин", "МакМаффин");
        put("биг", "Биг Мак");
        put("воппер", "Воппер");


        put("картошечки", "картошка");
        put("помидорки", "помидор");
        put("огурчики", "огурец");
        put("яблочки", "яблоко");
        put("творожок", "творог");
        put("молочко", "молоко");
        put("хлебушек", "хлеб");
        put("мясцо", "мясо");
        put("рыбка", "рыба");
        put("кашка", "каша");
        put("супчик", "суп");
        put("чаек", "чай");
        put("кофеек", "кофе");
        put("водичка", "вода");
        put("сладенького", "сладкое");
        put("вкусненького", "вкусное");
        put("полезного", "полезное");


        put("дуриана", "дуриан");
        put("рамбутана", "рамбутан");
        put("лонгана", "лонган");
        put("личи", "личи");
        put("мангостина", "мангостин");
        put("питахайи", "питахайя");
        put("карамболы", "карамбола");
        put("гуавы", "гуава");
        put("маракуйи", "маракуйя");
        put("кумквата", "кумкват");
        put("фейхоа", "фейхоа");
        put("джекфрута", "джекфрут");
        put("саподиллы", "саподилла");
        put("черимойи", "черимойя");
        put("анноны", "аннона");
        put("тамаринда", "тамаринд");
        put("физалиса", "физалис");
        put("кивано", "кивано");
        put("акебии", "акебия");
        put("лукумы", "лукума");
        put("купуасу", "купуасу");
        put("бабако", "бабако");
        put("канистели", "канистель");
        put("сапоты", "сапота");
        put("асаи", "асаи");
        put("годжи", "годжи");
        put("ацеролы", "ацерола");


        put("дайкона", "дайкон");
        put("пастернака", "пастернак");
        put("репы", "репа");
        put("брюквы", "брюква");
        put("турнепса", "турнепс");
        put("кольраби", "кольраби");
        put("романеско", "романеско");
        put("патиссона", "патиссон");
        put("цукини", "цукини");
        put("цуккини", "цуккини");
        put("мангольда", "мангольд");
        put("бамии", "бамия");
        put("окры", "окра");
        put("момордики", "момордика");
        put("люффы", "люффа");
        put("лагенарии", "лагенария");
        put("чайота", "чайот");
        put("батата", "батат");
        put("топинамбура", "топинамбур");
        put("артишока", "артишок");
        put("эндивия", "эндивий");
        put("радиккио", "радиккио");
        put("фризе", "фризе");
        put("руколы", "рукола");
        put("кресса", "кресс");
        put("корна", "корн");
        put("мизуны", "мизуна");
        put("черемши", "черемша");
        put("одуванчика", "одуванчик");
        put("портобелло", "портобелло");
        put("шиитаке", "шиитаке");
        put("вешенок", "вешенки");
        put("лисичек", "лисички");
        put("опят", "опята");
        put("белых", "белые");
        put("шампиньонов", "шампиньоны");


        put("макадамии", "макадамия");
        put("пекана", "пекан");
        put("пили", "пили");
        put("кедровых", "кедровые");
        put("бразильского", "бразильский");
        put("можжевельника", "можжевельник");
        put("маша", "маш");
        put("адзуки", "адзуки");
        put("пинто", "пинто");
        put("фавы", "фава");
        put("эдамаме", "эдамаме");
        put("сои", "соя");
        put("чиа", "чиа");
        put("льна", "лен");
        put("тыквенных", "тыквенные");
        put("подсолнечника", "подсолнечник");


        put("базилика", "базилик");
        put("орегано", "орегано");
        put("душицы", "душица");
        put("тимьяна", "тимьян");
        put("чабреца", "чабрец");
        put("розмарина", "розмарин");
        put("шалфея", "шалфей");
        put("мелиссы", "мелисса");
        put("мяты", "мята");
        put("эстрагона", "эстрагон");
        put("тархуна", "тархун");
        put("любистока", "любисток");
        put("иссопа", "иссоп");
        put("фенхеля", "фенхель");
        put("корицы", "корица");
        put("куркумы", "куркума");
        put("имбиря", "имбирь");
        put("кардамона", "кардамон");
        put("аниса", "анис");
        put("бадьяна", "бадьян");
        put("гвоздики", "гвоздика");
        put("мускатного", "мускатный");
        put("ванили", "ваниль");
        put("шафрана", "шафран");


        put("трюфелей", "трюфели");
        put("каперсов", "каперсы");
        put("анчоусов", "анчоусы");
        put("каламарей", "каламары");
        put("осьминогов", "осьминоги");
        put("лангустинов", "лангустины");
        put("лангустов", "лангусты");
        put("лобстеров", "лобстеры");
        put("омаров", "омары");
        put("гребешков", "гребешки");
        put("ежей", "ежи");
        put("трепангов", "трепанги");
        put("вакаме", "вакаме");
        put("нори", "нори");
        put("ламинарии", "ламинария");
        put("спирулины", "спирулина");
        put("хлореллы", "хлорелла");
        put("строганины", "строганина");
        put("буженины", "буженина");
        put("корейки", "корейка");
        put("карбонада", "карбонад");
        put("окорока", "окорок");
        put("грудинки", "грудинка");
        put("антрекота", "антрекот");
        put("рибая", "рибай");
        put("филе-миньона", "филе-миньон");
        put("стриплойна", "стриплойн");
        put("оссобуко", "оссобуко");


        put("батончика", "батончик");
        put("протеина", "протеин");
        put("бомбара", "Bombbar");
        put("корни", "Corny");
        put("фитнеса", "Fitness");
        put("рекса", "ProteinRex");


        put("моццареллы", "моцарелла");
        put("пармезана", "пармезан");
        put("гауды", "гауда");
        put("чеддера", "чеддер");
        put("бри", "бри");
        put("камамбера", "камамбер");
        put("рокфора", "рокфор");
        put("дорблю", "дорблю");
        put("фетки", "фета");
        put("рикотты", "рикотта");
        put("маскарпоне", "маскарпоне");
        put("филадельфии", "филадельфия");
        put("эмменталя", "эмменталь");
        put("грюйера", "грюйер");
        put("пошехонского", "пошехонский");
        put("российского", "российский");
        put("голландского", "голландский");
        put("костромского", "костромской");
        put("ярославского", "ярославский");
        put("советского", "советский");


        put("киселя", "кисель");
        put("желе", "желе");
        put("пудинга", "пудинг");
        put("муссов", "мусс");
        put("суфле", "суфле");
        put("панакоты", "панакота");
        put("тирамису", "тирамису");
        put("чизкейка", "чизкейк");
        put("штруделя", "штрудель");
        put("эклеров", "эклеры");
        put("профитролей", "профитроли");
        put("макарунов", "макаруны");
        put("безе", "безе");
        put("зефира", "зефир");
        put("пастилы", "пастила");
        put("мармелада", "мармелад");
        put("карамели", "карамель");
        put("ирисок", "ириски");
        put("леденцов", "леденцы");
        put("жвачки", "жвачка");


        put("стакана", "стакан");
        put("стаканы", "стакан");
        put("стаканов", "стакан");
        put("стаканчика", "стакан");
        put("стаканчики", "стакан");
        put("стаканчиков", "стакан");
        put("стакашка", "стакан");

        put("кружки", "кружка");
        put("кружек", "кружка");
        put("кружечки", "кружка");
        put("кружечек", "кружка");

        put("чашки", "чашка");
        put("чашек", "чашка");
        put("чашечки", "чашка");
        put("чашечек", "чашка");

        put("миски", "миска");
        put("мисок", "миска");
        put("мисочки", "миска");
        put("мисочек", "миска");

        put("тарелки", "тарелка");
        put("тарелок", "тарелка");
        put("тарелочки", "тарелка");
        put("тарелочек", "тарелка");

        put("блюдца", "блюдце");
        put("блюдец", "блюдце");
        put("блюдечка", "блюдце");
        put("блюдечек", "блюдце");

        put("пиалы", "пиала");
        put("пиал", "пиала");
        put("пиалки", "пиала");
        put("пиалок", "пиала");

        put("ложки", "ложка");
        put("ложек", "ложка");
        put("ложечки", "ложка");
        put("ложечек", "ложка");

        put("столовой", "столовый");
        put("столовых", "столовый");
        put("чайной", "чайный");
        put("чайных", "чайный");

        put("половника", "половник");
        put("половники", "половник");
        put("половников", "половник");

        put("поварешки", "поварешка");
        put("поварешек", "поварешка");


        put("штуки", "штука");
        put("штучки", "штука");
        put("штук", "штука");

        put("кусочка", "кусочек");
        put("кусочки", "кусочек");
        put("кусочков", "кусочек");
        put("кусочек", "кусочек");

        put("куска", "кусок");
        put("куски", "кусок");
        put("кусков", "кусок");
        put("кусищи", "кусок");

        put("ломтика", "ломтик");
        put("ломтики", "ломтик");
        put("ломтиков", "ломтик");

        put("дольки", "долька");
        put("долек", "долька");
        put("долечки", "долька");

        put("половинки", "половинка");
        put("половинок", "половинка");
        put("половиночки", "половинка");

        put("четвертинки", "четвертинка");
        put("четвертинок", "четвертинка");
        put("четвертушки", "четвертинка");

        put("горсти", "горсть");
        put("горстей", "горсть");
        put("горстки", "горсть");

        put("пригоршни", "пригоршня");
        put("пригоршней", "пригоршня");

        put("щепотки", "щепотка");
        put("щепоток", "щепотка");
        put("щепоточки", "щепотка");

        put("капли", "капля");
        put("капель", "капля");
        put("капелек", "капля");
        put("капельки", "капля");


        put("маленького", "маленький");
        put("маленькой", "маленький");
        put("небольшого", "небольшой");
        put("небольшой", "небольшой");
        put("среднего", "средний");
        put("средней", "средний");
        put("большого", "большой");
        put("большой", "большой");
        put("огромного", "огромный");
        put("огромной", "огромный");
        put("крупного", "крупный");
        put("крупной", "крупный");
        put("мелкого", "мелкий");
        put("мелкой", "мелкий");
        put("тонкого", "тонкий");
        put("тонкой", "тонкий");
        put("толстого", "толстый");
        put("толстой", "толстый");


        put("упаковки", "упаковка");
        put("упаковок", "упаковка");
        put("упаковочки", "упаковка");
        put("упаковочек", "упаковка");

        put("пачки", "пачка");
        put("пачек", "пачка");
        put("пачечки", "пачка");
        put("пачечек", "пачка");

        put("коробки", "коробка");
        put("коробок", "коробка");
        put("коробочки", "коробка");
        put("коробочек", "коробка");

        put("банки", "банка");
        put("банок", "банка");
        put("баночки", "банка");
        put("баночек", "банка");

        put("пакета", "пакет");
        put("пакеты", "пакет");
        put("пакетов", "пакет");
        put("пакетика", "пакет");
        put("пакетики", "пакет");
        put("пакетиков", "пакет");

        put("бутылки", "бутылка");
        put("бутылок", "бутылка");
        put("бутылочки", "бутылка");
        put("бутылочек", "бутылка");

        put("флакона", "флакон");
        put("флаконы", "флакон");
        put("флаконов", "флакон");
        put("флакончика", "флакон");
        put("флакончики", "флакон");
        put("флакончиков", "флакон");

        put("тубы", "туба");
        put("туб", "туба");
        put("тубика", "туба");
        put("тубики", "туба");
        put("тубиков", "туба");

        put("контейнера", "контейнер");
        put("контейнеры", "контейнер");
        put("контейнеров", "контейнер");
        put("контейнерчика", "контейнер");
        put("контейнерчики", "контейнер");
        put("контейнерчиков", "контейнер");


        put("порции", "порция");
        put("порций", "порция");
        put("порционка", "порция");
        put("порционки", "порция");
        put("порционок", "порция");
        put("порциончик", "порция");
        put("порциончики", "порция");
        put("порциончиков", "порция");

        put("подачи", "подача");
        put("подач", "подача");

        put("сервировки", "сервировка");
        put("сервировок", "сервировка");

        put("блюда", "блюдо");
        put("блюд", "блюдо");
        put("блюдечко", "блюдо");
        put("блюдечки", "блюдо");
        put("блюдечек", "блюдо");

        put("тостика", "тостик");
        put("тостики", "тостик");
        put("тостиков", "тостик");

        put("бутерброда", "бутерброд");
        put("бутерброды", "бутерброд");
        put("бутербродов", "бутерброд");
        put("бутербродика", "бутерброд");
        put("бутербродики", "бутерброд");
        put("бутербродиков", "бутерброд");

        put("сэндвича", "сэндвич");
        put("сэндвичи", "сэндвич");
        put("сэндвичей", "сэндвич");
        put("сэндвичика", "сэндвич");
        put("сэндвичики", "сэндвич");
        put("сэндвичиков", "сэндвич");

        put("гамбургера", "гамбургер");
        put("гамбургеры", "гамбургер");
        put("гамбургеров", "гамбургер");

        put("чизбургера", "чизбургер");
        put("чизбургеры", "чизбургер");
        put("чизбургеров", "чизбургер");

        put("хот-дога", "хот-дог");
        put("хот-доги", "хот-дог");
        put("хот-догов", "хот-дог");

        put("роллика", "ролл");
        put("роллы", "ролл");
        put("роллов", "ролл");


        put("глотка", "глоток");
        put("глотки", "глоток");
        put("глотков", "глоток");
        put("глоточка", "глоток");
        put("глоточки", "глоток");
        put("глоточков", "глоток");

        put("сипа", "сип");
        put("сипы", "сип");
        put("сипов", "сип");

        put("литра", "литр");
        put("литры", "литр");
        put("литров", "литр");
        put("литрика", "литр");
        put("литрики", "литр");
        put("литриков", "литр");

        put("миллилитра", "миллилитр");
        put("миллилитры", "миллилитр");
        put("миллилитров", "миллилитр");
        put("милилитрика", "миллилитр");


        put("граненого", "граненый");
        put("граненной", "граненый");
        put("граненых", "граненый");

        put("стопки", "стопка");
        put("стопок", "стопка");
        put("стопочки", "стопка");
        put("стопочек", "стопка");

        put("рюмки", "рюмка");
        put("рюмок", "рюмка");
        put("рюмочки", "рюмка");
        put("рюмочек", "рюмка");

        put("фужера", "фужер");
        put("фужеры", "фужер");
        put("фужеров", "фужер");
        put("фужерчика", "фужер");
        put("фужерчики", "фужер");
        put("фужерчиков", "фужер");

        put("бокала", "бокал");
        put("бокалы", "бокал");
        put("бокалов", "бокал");
        put("бокальчика", "бокал");
        put("бокальчики", "бокал");
        put("бокальчиков", "бокал");


        put("зубчика", "зубчик");
        put("зубчики", "зубчик");
        put("зубчиков", "зубчик");
        put("зубочка", "зубчик");
        put("зубочки", "зубчик");
        put("зубочков", "зубчик");

        put("головки", "головка");
        put("головок", "головка");
        put("головочки", "головка");
        put("головочек", "головка");

        put("стручка", "стручок");
        put("стручки", "стручок");
        put("стручков", "стручок");
        put("стручочка", "стручок");
        put("стручочки", "стручок");
        put("стручочков", "стручок");

        put("веточки", "веточка");
        put("веточек", "веточка");

        put("листика", "листик");
        put("листики", "листик");
        put("листиков", "листик");
        put("листочка", "листок");
        put("листочки", "листок");
        put("листочков", "листок");

        put("пучка", "пучок");
        put("пучки", "пучок");
        put("пучков", "пучок");
        put("пучочка", "пучок");
        put("пучочки", "пучок");
        put("пучочков", "пучок");

        put("связки", "связка");
        put("связок", "связка");
        put("связочки", "связка");
        put("связочек", "связка");
    }};


    private String lemmatizeWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return word;
        }

        String normalizedWord = word.trim().toLowerCase();


        String lemma = FOOD_LEMMAS.get(normalizedWord);
        if (lemma != null) {
            Log.d(TAG, "Лемматизация (словарь): '" + word + "' -> '" + lemma + "'");
            return lemma;
        }


        String simpleLemma = applySimpleLemmatization(normalizedWord);
        if (!simpleLemma.equals(normalizedWord)) {
            Log.d(TAG, "Лемматизация (правила): '" + word + "' -> '" + simpleLemma + "'");
            return simpleLemma;
        }

        Log.d(TAG, "Лемматизация не применена для: '" + word + "'");
        return normalizedWord;
    }


    private String applySimpleLemmatization(String word) {

        if (word.endsWith("ов") && word.length() > 3) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("ей") && word.length() > 3) {
            return word.substring(0, word.length() - 2);
        }


        if (word.endsWith("ы") && word.length() > 3) {
            String stem = word.substring(0, word.length() - 1);

            if (stem.endsWith("к") || stem.endsWith("г") || stem.endsWith("х")) {
                return stem + "а";
            } else {
                return stem + "а";
            }
        }


        if (word.endsWith("и") && word.length() > 3) {
            String stem = word.substring(0, word.length() - 1);
            if (stem.endsWith("ц")) {
                return stem + "а";
            } else if (stem.endsWith("к")) {
                return stem + "а";
            } else {
                return stem + "я";
            }
        }


        if (word.endsWith("а") && word.length() > 3) {
            String stem = word.substring(0, word.length() - 1);

            if (stem.endsWith("ок") || stem.endsWith("ек")) {
                return stem;
            } else if (stem.endsWith("к")) {
                return stem + "о";
            }
        }


        if (word.endsWith("ка") && word.length() > 4) {
            String stem = word.substring(0, word.length() - 2);
            if (stem.endsWith("оч") || stem.endsWith("еч")) {
                return stem.substring(0, stem.length() - 2) + "о";
            } else if (stem.endsWith("ошк") || stem.endsWith("ешк")) {
                return stem.substring(0, stem.length() - 3);
            }
            return stem;
        }

        if (word.endsWith("ик") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }

        if (word.endsWith("чик") && word.length() > 5) {
            return word.substring(0, word.length() - 4);
        }

        if (word.endsWith("ок") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }

        if (word.endsWith("ко") && word.length() > 4) {
            return word.substring(0, word.length() - 2) + "о";
        }

        if (word.endsWith("цо") && word.length() > 4) {
            return word.substring(0, word.length() - 2) + "о";
        }


        if (word.endsWith("ого") && word.length() > 5) {
            return word.substring(0, word.length() - 3) + "ый";
        }

        if (word.endsWith("ой") && word.length() > 4) {
            return word.substring(0, word.length() - 2) + "ый";
        }

        if (word.endsWith("ей") && word.length() > 4) {
            String stem = word.substring(0, word.length() - 2);
            if (stem.endsWith("н") || stem.endsWith("ж") || stem.endsWith("щ")) {
                return stem + "ий";
            }
        }

        return word;
    }


    private List<String> lemmatizeFoodCandidates(List<String> candidates) {
        List<String> lemmatizedCandidates = new ArrayList<>();

        for (String candidate : candidates) {
            String lemma = lemmatizeWord(candidate);
            lemmatizedCandidates.add(lemma);


            if (!lemma.equals(candidate.toLowerCase().trim())) {
                lemmatizedCandidates.add(candidate.toLowerCase().trim());
                Log.d(TAG, "Добавлены для поиска: лемма '" + lemma + "' и оригинал '" + candidate + "'");
            }
        }

        return lemmatizedCandidates;
    }


    private String normalizePortion(String portionWord, float quantity) {
        if (portionWord == null || portionWord.trim().isEmpty()) {
            return portionWord;
        }


        String lemmatized = lemmatizeWord(portionWord);


        String[] commonPortions = {
                "стакан", "чашка", "кружка", "миска", "тарелка", "ложка", "блюдце", "пиала",
                "кусочек", "кусок", "ломтик", "долька", "половинка", "четвертинка", "штука",
                "горсть", "щепотка", "капля", "пригоршня",
                "упаковка", "пачка", "коробка", "банка", "пакет", "бутылка", "флакон", "туба", "контейнер",
                "порция", "подача", "сервировка", "блюдо", "тостик", "бутерброд", "сэндвич",
                "гамбургер", "чизбургер", "хот-дог", "ролл",
                "глоток", "сип", "литр", "миллилитр",
                "стопка", "рюмка", "фужер", "бокал",
                "зубчик", "головка", "стручок", "веточка", "листик", "листок", "пучок", "связка"
        };

        String lowerLemma = lemmatized.toLowerCase();


        for (String basePortion : commonPortions) {
            if (basePortion.equals(lowerLemma)) {
                return basePortion;
            }


            String expectedForm = PluralizationUtil.getPlural(quantity, basePortion);
            if (expectedForm.toLowerCase().equals(portionWord.toLowerCase()) ||
                    expectedForm.toLowerCase().equals(lowerLemma)) {
                return basePortion;
            }
        }

        Log.d(TAG, "Нормализация порции: '" + portionWord + "' (количество: " + quantity + ") -> '" + lowerLemma + "'");
        return lowerLemma;
    }


    private List<String> extractPortionCandidates(String text, List<EntityAnnotation> annotations) {
        List<String> portionCandidates = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return portionCandidates;
        }

        String normalizedText = text.toLowerCase().trim();
        String[] words = normalizedText.split("\\s+");


        for (String word : words) {

            String cleanWord = word.replaceAll("[^а-яё]", "");

            if (cleanWord.length() > 2) {

                String lemma = FOOD_LEMMAS.get(cleanWord);
                if (lemma != null && isPortionWord(lemma)) {
                    portionCandidates.add(cleanWord);
                    Log.d(TAG, "Найден кандидат в порции: '" + cleanWord + "' -> '" + lemma + "'");
                } else if (isPortionWord(cleanWord)) {

                    portionCandidates.add(cleanWord);
                    Log.d(TAG, "Найден кандидат в порции (прямое совпадение): '" + cleanWord + "'");
                }
            }
        }


        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i].replaceAll("[^а-яё]", "");
            String word2 = words[i + 1].replaceAll("[^а-яё]", "");
            String compound = word1 + " " + word2;

            if (isCompoundPortion(word1, word2)) {
                portionCandidates.add(compound);
                Log.d(TAG, "Найдена составная порция: '" + compound + "'");
            }
        }

        return portionCandidates;
    }


    private boolean isPortionWord(String word) {

        String[] portions = {
                "стакан", "кружка", "чашка", "миска", "тарелка", "блюдце", "пиала",
                "ложка", "столовый", "чайный", "половник", "поварешка",
                "штука", "кусочек", "кусок", "ломтик", "долька", "половинка", "четвертинка",
                "горсть", "пригоршня", "щепотка", "капля",
                "маленький", "небольшой", "средний", "большой", "огромный", "крупный", "мелкий", "тонкий", "толстый",
                "упаковка", "пачка", "коробка", "банка", "пакет", "бутылка", "флакон", "туба", "контейнер",
                "порция", "подача", "сервировка", "блюдо", "тостик", "бутерброд", "сэндвич",
                "гамбургер", "чизбургер", "хот-дог", "ролл",
                "глоток", "сип", "литр", "миллилитр",
                "граненый", "стопка", "рюмка", "фужер", "бокал",
                "зубчик", "головка", "стручок", "веточка", "листик", "листок", "пучок", "связка"
        };

        for (String portion : portions) {
            if (portion.equals(word)) {
                return true;
            }
        }

        return false;
    }


    private boolean isCompoundPortion(String word1, String word2) {

        String[] adjectives = {"граненого", "граненный", "столовой", "столовый", "чайной", "чайный",
                "маленького", "маленький", "небольшого", "небольшой", "среднего", "средний",
                "большого", "большой", "огромного", "огромный", "крупного", "крупный",
                "мелкого", "мелкий", "тонкого", "тонкий", "толстого", "толстый"};

        String[] nouns = {"стакан", "стакана", "кружка", "кружки", "чашка", "чашки", "миска", "миски",
                "тарелка", "тарелки", "ложка", "ложки", "кусок", "куска", "кусочек", "кусочка",
                "ломтик", "ломтика", "долька", "дольки"};


        String lemma1 = lemmatizeWord(word1);
        String lemma2 = lemmatizeWord(word2);

        for (String adj : adjectives) {
            for (String noun : nouns) {
                if ((adj.equals(word1) || adj.equals(lemma1)) &&
                        (noun.equals(word2) || noun.equals(lemma2))) {
                    return true;
                }
            }
        }

        return false;
    }


    private List<RecognizedFood> findPortionsForFoods(List<RecognizedFood> recognizedFoods, List<String> portionCandidates, List<Float> numbers, String originalText) {
        List<RecognizedFood> updatedFoods = new ArrayList<>();

        Log.d(TAG, "🔄 DEBUG: Начинаем поиск порций для продуктов. Продуктов: " + recognizedFoods.size());
        Log.d(TAG, "🔄 DEBUG: Кандидаты в порции: " + portionCandidates);


        Map<String, Float> portionQuantities = matchNumbersToPortions(originalText, portionCandidates);

        Log.d(TAG, "🔄 DEBUG: Результат умного сопоставления: " + portionQuantities);


        Map<String, Float> processedPortions = new HashMap<>();

        for (int i = 0; i < recognizedFoods.size(); i++) {
            RecognizedFood recognizedFood = recognizedFoods.get(i);
            Food food = recognizedFood.getFoundFood();

            Log.d(TAG, "🍽️ DEBUG: Обрабатываем продукт: " + (food != null ? food.getName() : "null"));

            if (food != null && food.getPortions() != null && !food.getPortions().isEmpty()) {
                Log.d(TAG, "🍽️ DEBUG: У продукта '" + food.getName() + "' есть " + food.getPortions().size() + " порций");


                Portion matchedPortion = null;
                float portionQuantity = 1f;


                for (Map.Entry<String, Float> portionEntry : portionQuantities.entrySet()) {
                    String portionCandidate = portionEntry.getKey();
                    float candidateQuantity = portionEntry.getValue();

                    Log.d(TAG, "🔍 DEBUG: Проверяем кандидата '" + portionCandidate + "' с количеством " + candidateQuantity);


                    String normalizedPortion = normalizePortion(portionCandidate, candidateQuantity);
                    Log.d(TAG, "🔍 DEBUG: Нормализованная порция: '" + normalizedPortion + "'");


                    for (Portion portion : food.getPortions()) {
                        String portionName = portion.getName();

                        Log.d(TAG, "🔍 DEBUG: Сравниваем с порцией продукта: '" + portionName + "'");

                        if (portionName != null) {

                            boolean matches = portionName.toLowerCase().equals(normalizedPortion.toLowerCase()) ||
                                    portionName.toLowerCase().contains(normalizedPortion.toLowerCase()) ||
                                    normalizedPortion.toLowerCase().contains(portionName.toLowerCase());

                            if (matches) {
                                matchedPortion = portion;
                                portionQuantity = candidateQuantity;

                                Log.d(TAG, "✅ DEBUG: Умное сопоставление успешно: '" + portionCandidate + "' (количество: " + candidateQuantity +
                                        ") → порция '" + portionName + "' для продукта '" + food.getName() + "'");
                                break;
                            } else {
                                Log.d(TAG, "❌ DEBUG: Не совпадает: '" + normalizedPortion + "' vs '" + portionName + "'");
                            }
                        }
                    }

                    if (matchedPortion != null) {
                        break;
                    }
                }

                if (matchedPortion != null) {

                    RecognizedFood updatedFood = new RecognizedFood(
                            recognizedFood.getName(),
                            portionQuantity,
                            matchedPortion.getName(),
                            food,
                            matchedPortion
                    );

                    updatedFoods.add(updatedFood);


                    for (Map.Entry<String, Float> portionEntry : portionQuantities.entrySet()) {
                        String portionCandidate = portionEntry.getKey();
                        float candidateQuantity = portionEntry.getValue();

                        String normalizedPortion = normalizePortion(portionCandidate, candidateQuantity);
                        if (matchedPortion.getName().toLowerCase().equals(normalizedPortion.toLowerCase()) ||
                                matchedPortion.getName().toLowerCase().contains(normalizedPortion.toLowerCase()) ||
                                normalizedPortion.toLowerCase().contains(matchedPortion.getName().toLowerCase())) {
                            processedPortions.put(portionCandidate, candidateQuantity);
                            Log.d(TAG, "📋 DEBUG: Отмечаем порцию '" + portionCandidate + "' как обработанную через БД для '" + food.getName() + "'");
                            break;
                        }
                    }

                    Log.d(TAG, "🎉 DEBUG: Создан RecognizedFood с порцией: '" + matchedPortion.getName() + "' (" +
                            matchedPortion.getWeight() + "г) для продукта '" + food.getName() +
                            "', количество: " + portionQuantity);
                    Log.d(TAG, "🎉 DEBUG: getDisplayQuantity() вернет: '" + updatedFood.getDisplayQuantity() + "'");
                } else {

                    RecognizedFood standardUnitFood = findStandardUnitFood(recognizedFood, food, portionQuantities, originalText);
                    if (standardUnitFood != null) {
                        updatedFoods.add(standardUnitFood);
                        Log.d(TAG, "📏 DEBUG: Создан RecognizedFood со стандартной единицей для продукта '" + food.getName() + "'");
                    } else {

                        String unit;
                        float defaultQuantity = 100f;

                        if (isLiquidProduct(food.getName())) {
                            unit = "мл";
                            Log.d(TAG, "🥛 DEBUG: Продукт '" + food.getName() + "' определен как жидкость, ставим 100мл");
                        } else {
                            unit = "г";
                            Log.d(TAG, "🍞 DEBUG: Продукт '" + food.getName() + "' определен как твердый, ставим 100г");
                        }


                        RecognizedFood standardFood = new RecognizedFood(
                                recognizedFood.getName(),
                                defaultQuantity,
                                unit,
                                food,
                                null
                        );

                        updatedFoods.add(standardFood);
                        Log.d(TAG, "📦 DEBUG: У продукта '" + food.getName() + "' есть порции, но не найдена подходящая, установили " + defaultQuantity + unit);
                        Log.d(TAG, "⚠️ DEBUG: Доступные порции: " + food.getPortions().size());
                        for (Portion p : food.getPortions()) {
                            Log.d(TAG, "📝 DEBUG: Доступная порция: '" + p.getName() + "' (" + p.getWeight() + "г)");
                        }
                    }
                }
            } else {

                String unit;
                float defaultQuantity = 100f;

                if (isLiquidProduct(food.getName())) {
                    unit = "мл";
                    Log.d(TAG, "🥛 DEBUG: Продукт '" + food.getName() + "' определен как жидкость, ставим 100мл");
                } else {
                    unit = "г";
                    Log.d(TAG, "🍞 DEBUG: Продукт '" + food.getName() + "' определен как твердый, ставим 100г");
                }


                RecognizedFood standardFood = new RecognizedFood(
                        recognizedFood.getName(),
                        defaultQuantity,
                        unit,
                        food,
                        null
                );

                updatedFoods.add(standardFood);
                Log.d(TAG, "📦 DEBUG: У продукта '" + food.getName() + "' нет порций, установили " + defaultQuantity + unit);
            }
        }


        for (Map.Entry<String, Float> portionEntry : portionQuantities.entrySet()) {
            String portionCandidate = portionEntry.getKey();
            float quantity = portionEntry.getValue();


            if (processedPortions.containsKey(portionCandidate)) {
                Log.d(TAG, "⏭️ DEBUG: Порция '" + portionCandidate + "' уже обработана через БД, пропускаем автоконвертацию");
                continue;
            }

            if (isVolumetricPortion(portionCandidate)) {
                Log.d(TAG, "🥛 DEBUG: Пробуем автоконвертацию объемной порции: '" + portionCandidate + "' с количеством " + quantity);


                RecognizedFood liquidProduct = findUnprocessedLiquidProduct(recognizedFoods, updatedFoods);
                if (liquidProduct != null) {
                    Food food = liquidProduct.getFoundFood();
                    float volumeInMl = convertVolumetricPortionToMl(portionCandidate, quantity);


                    RecognizedFood volumetricFood = new RecognizedFood(
                            liquidProduct.getName(),
                            volumeInMl,
                            "мл",
                            food,
                            null
                    );

                    updatedFoods.add(volumetricFood);
                    processedPortions.put(portionCandidate, quantity);

                    Log.d(TAG, "🎉 DEBUG: Автоконвертация объемной порции применена: '" + portionCandidate +
                            "' (" + quantity + " шт) = " + volumeInMl + "мл для продукта '" + food.getName() + "'");
                }
            }
        }

        return updatedFoods;
    }


    private RecognizedFood findStandardUnitFood(RecognizedFood recognizedFood, Food food, Map<String, Float> portionQuantities, String originalText) {
        Log.d(TAG, "📏 DEBUG: Ищем стандартные единицы для продукта '" + food.getName() + "'");


        for (Map.Entry<String, Float> portionEntry : portionQuantities.entrySet()) {
            String portionCandidate = portionEntry.getKey();
            float candidateQuantity = portionEntry.getValue();

            if (isStandardUnit(portionCandidate)) {
                String unit = normalizeStandardUnit(portionCandidate);
                Log.d(TAG, "📏 DEBUG: Найдена стандартная единица '" + portionCandidate + "' → '" + unit + "' с количеством " + candidateQuantity);

                RecognizedFood standardUnitFood = new RecognizedFood(
                        recognizedFood.getName(),
                        candidateQuantity,
                        unit,
                        food,
                        null
                );

                Log.d(TAG, "📏 DEBUG: Создан RecognizedFood: " + candidateQuantity + " " + unit);
                return standardUnitFood;
            }
        }

        Log.d(TAG, "📏 DEBUG: Стандартные единицы не найдены");
        return null;
    }


    private String normalizeStandardUnit(String unit) {
        if (unit == null) return "г";

        String lowerUnit = unit.toLowerCase();


        if (lowerUnit.matches("г|грамм|граммов|грамма|гр")) return "г";
        if (lowerUnit.matches("кг|килограмм|килограммов|килограмма|кило")) return "кг";


        if (lowerUnit.matches("мл|миллилитр|миллилитров|миллилитра")) return "мл";
        if (lowerUnit.matches("л|литр|литров|литра")) return "л";


        if (lowerUnit.matches("шт|штук|штуки|штука|штучек|штучки|штучка")) return "шт";
        if (lowerUnit.matches("кусок|кусков|куска|куски|кусочек|кусочков|кусочка|кусочки"))
            return "кусок";


        if (lowerUnit.matches("порция|порций|порции")) return "порция";


        return unit;
    }


    private List<String> extractPercentages(String text) {
        List<String> percentages = new ArrayList<>();

        Log.d(TAG, "🔢 DEBUG: Ищем проценты в тексте: '" + text + "'");


        String digitRegex = "(\\d+[.,]?\\d*)\\s*(%|процент[ао]?)";
        Log.d(TAG, "🔢 DEBUG: Используем regex для цифр: '" + digitRegex + "'");

        Pattern digitPercentPattern = Pattern.compile(digitRegex, Pattern.CASE_INSENSITIVE);
        Matcher digitMatcher = digitPercentPattern.matcher(text);

        Log.d(TAG, "🔢 DEBUG: Начинаем поиск цифровых совпадений...");

        while (digitMatcher.find()) {
            Log.d(TAG, "🔍 DEBUG: Найдено цифровое совпадение: '" + digitMatcher.group() + "'");
            Log.d(TAG, "🔍 DEBUG: Группа 1 (число): '" + digitMatcher.group(1) + "'");
            Log.d(TAG, "🔍 DEBUG: Группа 2 (знак %): '" + (digitMatcher.groupCount() >= 2 ? digitMatcher.group(2) : "null") + "'");

            String numberPart = digitMatcher.group(1).replace(',', '.');
            String percentage = numberPart + "%";
            percentages.add(percentage);
            Log.d(TAG, "✅ DEBUG: Найден цифровой процент: '" + percentage + "'");
        }


        Log.d(TAG, "🔤 DEBUG: Ищем словесные проценты...");
        String[] wordsArray = text.toLowerCase().split("\\s+");

        for (int i = 0; i < wordsArray.length - 1; i++) {
            String currentWord = wordsArray[i].replaceAll("[^а-яё.]", "");
            String nextWord = wordsArray[i + 1].replaceAll("[^а-яё]", "");


            if (nextWord.startsWith("процент") && RUSSIAN_NUMBERS.containsKey(currentWord)) {
                float numberValue = RUSSIAN_NUMBERS.get(currentWord);
                String percentage = numberValue + "%";
                percentages.add(percentage);
                Log.d(TAG, "✅ DEBUG: Найден словесный процент: '" + currentWord + " " + nextWord + "' = '" + percentage + "'");
            }


            if (i < wordsArray.length - 2) {
                String thirdWord = wordsArray[i + 2].replaceAll("[^а-яё]", "");


                if (nextWord.equals("с") && thirdWord.equals("половиной") &&
                        i < wordsArray.length - 3 && wordsArray[i + 3].startsWith("процент") &&
                        RUSSIAN_NUMBERS.containsKey(currentWord)) {

                    float baseNumber = RUSSIAN_NUMBERS.get(currentWord);
                    float finalNumber = baseNumber + 0.5f;
                    String percentage = finalNumber + "%";
                    percentages.add(percentage);
                    Log.d(TAG, "✅ DEBUG: Найден дробный словесный процент: '" +
                            currentWord + " с половиной процента' = '" + percentage + "'");
                }
            }
        }

        if (percentages.isEmpty()) {
            Log.d(TAG, "⚠️ DEBUG: Regex не сработал. Попробуем простой поиск...");


            if (text.contains("%")) {
                Log.d(TAG, "📍 DEBUG: Найден символ % в тексте");
                String[] words = text.split("\\s+");
                for (String word : words) {
                    if (word.contains("%")) {
                        Log.d(TAG, "📍 DEBUG: Слово с %: '" + word + "'");


                        String numberOnly = word.replaceAll("[^\\d.,]", "");
                        if (!numberOnly.isEmpty()) {
                            String cleanNumber = numberOnly.replace(',', '.');
                            percentages.add(cleanNumber + "%");
                            Log.d(TAG, "✅ DEBUG: Fallback найден процент: '" + cleanNumber + "%'");
                        }
                    }
                }
            }

            if (percentages.isEmpty()) {
                Log.d(TAG, "⚠️ DEBUG: И fallback не помог. Проверим текст по символам:");
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    Log.d(TAG, "🔤 DEBUG: Позиция " + i + ": '" + c + "' (код: " + (int) c + ")");
                }
            }
        }

        Log.d(TAG, "🔢 DEBUG: Итого найдено процентов: " + percentages.size());
        return percentages;
    }


    private List<Food> filterByExactMatch(List<Food> foods, String searchTerm) {
        List<Food> filtered = new ArrayList<>();
        String searchLower = searchTerm.toLowerCase().trim();

        Log.d(TAG, "🔍 Фильтрация " + foods.size() + " продуктов по точному соответствию: '" + searchTerm + "'");

        for (Food food : foods) {
            String foodName = food.getName().toLowerCase().trim();


            String[] searchWords = searchLower.split("\\s+");
            boolean containsAllWords = true;

            for (String searchWord : searchWords) {
                if (!foodName.contains(searchWord)) {
                    containsAllWords = false;
                    break;
                }
            }

            if (containsAllWords) {
                filtered.add(food);
                Log.d(TAG, "✅ Продукт '" + food.getName() + "' прошел фильтрацию точного соответствия");
            } else {
                Log.d(TAG, "❌ Продукт '" + food.getName() + "' НЕ прошел фильтрацию точного соответствия");
            }
        }

        Log.d(TAG, "🏁 После фильтрации точного соответствия осталось: " + filtered.size() + " продуктов");
        return filtered;
    }


    private List<Food> excludeAlreadyFoundProducts(List<Food> foods, Set<String> foundProductNames) {
        List<Food> filtered = new ArrayList<>();

        Log.d(TAG, "🚫 Исключаем уже найденные продукты. Найдено ранее: " + foundProductNames.size());
        Log.d(TAG, "🚫 Список исключений: " + foundProductNames);

        for (Food food : foods) {
            String foodNameLower = food.getName().toLowerCase().trim();

            if (!foundProductNames.contains(foodNameLower)) {
                filtered.add(food);
                Log.d(TAG, "✅ Продукт '" + food.getName() + "' НЕ найден ранее - добавляем");
            } else {
                Log.d(TAG, "🚫 Продукт '" + food.getName() + "' уже найден ранее - ИСКЛЮЧАЕМ");
            }
        }

        Log.d(TAG, "🏁 После исключения дубликатов осталось: " + filtered.size() + " из " + foods.size() + " продуктов");
        return filtered;
    }


    private List<Food> filterAndRankFoodsByPercentage(List<Food> foods, List<String> percentages, List<String> foodCandidates, String originalText) {
        if (percentages.isEmpty()) {
            Log.d(TAG, "🔢 DEBUG: Проценты не найдены, возвращаем все продукты как есть");
            return foods;
        }

        Log.d(TAG, "🔢 DEBUG: Фильтруем " + foods.size() + " продуктов по процентам: " + percentages);

        List<Food> rankedFoods = new ArrayList<>();


        Map<String, String> percentageToProductMapping = createPercentageToProductMapping(percentages, foodCandidates, originalText);

        Log.d(TAG, "🎯 DEBUG: Карта сопоставления процентов с продуктами: " + percentageToProductMapping);


        for (Map.Entry<String, String> mapping : percentageToProductMapping.entrySet()) {
            String percentage = mapping.getKey();
            String targetProductName = mapping.getValue();

            for (Food food : foods) {
                String foodName = food.getName().toLowerCase();
                String searchPercentage = percentage.toLowerCase();


                boolean containsPercentage = foodName.contains(searchPercentage) ||
                        foodName.contains(searchPercentage.replace("%", "")) ||
                        foodName.contains(searchPercentage.replace("%", " процент")) ||
                        foodName.contains(searchPercentage.replace("%", "процент"));

                boolean matchesTargetProduct = foodName.contains(targetProductName.toLowerCase()) ||
                        targetProductName.toLowerCase().contains(foodName.toLowerCase()) ||
                        areSimilarProducts(foodName, targetProductName);

                if (containsPercentage && matchesTargetProduct) {
                    if (!rankedFoods.contains(food)) {
                        rankedFoods.add(food);
                        Log.d(TAG, "✅ DEBUG: Продукт '" + food.getName() + "' содержит процент '" + percentage +
                                "' и соответствует целевому продукту '" + targetProductName + "' - приоритет!");
                    }
                }
            }
        }


        if (rankedFoods.isEmpty()) {
            Log.d(TAG, "⚠️ DEBUG: Позиционное сопоставление не дало результатов, используем fallback");

            for (String percentage : percentages) {
                for (Food food : foods) {
                    String foodName = food.getName().toLowerCase();
                    String searchPercentage = percentage.toLowerCase();

                    if (foodName.contains(searchPercentage) ||
                            foodName.contains(searchPercentage.replace("%", "")) ||
                            foodName.contains(searchPercentage.replace("%", " процент")) ||
                            foodName.contains(searchPercentage.replace("%", "процент"))) {

                        if (!rankedFoods.contains(food)) {
                            rankedFoods.add(food);
                            Log.d(TAG, "✅ DEBUG: Fallback - Продукт '" + food.getName() + "' содержит процент '" + percentage + "' - приоритет!");
                        }
                    }
                }
            }
        }


        for (Food food : foods) {
            if (!rankedFoods.contains(food)) {
                rankedFoods.add(food);
                Log.d(TAG, "➕ DEBUG: Добавлен продукт без точного соответствия: '" + food.getName() + "'");
            }
        }

        Log.d(TAG, "🏆 DEBUG: После ранжирования: " + rankedFoods.size() + " продуктов");
        return rankedFoods;
    }


    private boolean isLiquidProduct(String productName) {
        if (productName == null) return false;

        String lowerName = productName.toLowerCase();


        String[] liquidKeywords = {

                "молоко", "кефир", "ряженка", "сметана", "йогурт", "айран", "тан", "кумыс",
                "сок", "нектар", "компот", "морс", "лимонад", "квас", "вода",


                "пиво", "вино", "водка", "коньяк", "виски", "ром", "джин", "ликер",


                "кока", "пепси", "спрайт", "фанта", "миринда", "тархун", "байкал", "буратино",
                "газировка", "лимонад",


                "энергетический", "изотоник", "протеин", "гейнер",


                "кофе", "чай", "какао", "горячий шоколад", "капучино", "латте", "эспрессо",
                "американо", "мокка", "фраппучино", "цикорий",


                "соус", "кетчуп", "майонез", "уксус", "масло", "сироп",


                "суп", "бульон", "борщ", "солянка", "рассольник", "уха", "харчо", "окрошка",


                "сливки", "сыворотка", "пахта", "простокваша", "варенец", "катык", "мацони"
        };

        for (String keyword : liquidKeywords) {
            if (lowerName.contains(keyword)) {
                Log.d(TAG, "🥛 DEBUG: Продукт '" + productName + "' содержит ключевое слово '" + keyword + "' - жидкость");
                return true;
            }
        }

        Log.d(TAG, "🍞 DEBUG: Продукт '" + productName + "' не содержит ключевых слов жидкостей - твердый");
        return false;
    }


    private Portion findBestPortionMatch(List<String> portionCandidates, List<Portion> availablePortions) {
        for (String candidate : portionCandidates) {

            String lemmatizedCandidate = lemmatizeWord(candidate);

            for (Portion portion : availablePortions) {
                String portionName = portion.getName();

                if (portionName != null) {

                    if (portionName.toLowerCase().equals(candidate.toLowerCase()) ||
                            portionName.toLowerCase().equals(lemmatizedCandidate.toLowerCase())) {
                        return portion;
                    }


                    if (portionName.toLowerCase().contains(candidate.toLowerCase()) ||
                            candidate.toLowerCase().contains(portionName.toLowerCase()) ||
                            portionName.toLowerCase().contains(lemmatizedCandidate.toLowerCase()) ||
                            lemmatizedCandidate.toLowerCase().contains(portionName.toLowerCase())) {
                        return portion;
                    }
                }
            }
        }

        return null;
    }


    private boolean isVolumetricPortion(String portionCandidate) {
        if (portionCandidate == null) return false;

        String lowerPortion = portionCandidate.toLowerCase();
        String normalizedPortion = FOOD_LEMMAS.getOrDefault(lowerPortion, lowerPortion);


        String[] volumetricPortions = {
                "стакан", "стакана", "стаканов", "стаканчик", "стаканчика", "стаканчиков",
                "кружка", "кружки", "кружек", "кружечка", "кружечки", "кружечек",
                "чашка", "чашки", "чашек", "чашечка", "чашечки", "чашечек",
                "миска", "миски", "мисок", "мисочка", "мисочки", "мисочек",
                "бокал", "бокала", "бокалов", "рюмка", "рюмки", "рюмок",
                "стопка", "стопки", "стопок", "фужер", "фужера", "фужеров"
        };

        for (String volumetric : volumetricPortions) {
            if (lowerPortion.equals(volumetric) || normalizedPortion.equals(volumetric)) {
                return true;
            }
        }

        return false;
    }


    private RecognizedFood findLiquidProductForVolumetricPortion(List<RecognizedFood> recognizedFoods) {
        for (RecognizedFood recognizedFood : recognizedFoods) {
            Food food = recognizedFood.getFoundFood();
            if (food != null && isLiquidProduct(food.getName())) {
                Log.d(TAG, "🥛 DEBUG: Найден жидкий продукт для объемной порции: '" + food.getName() + "'");
                return recognizedFood;
            }
        }

        Log.d(TAG, "🚫 DEBUG: Жидкий продукт для объемной порции не найден");
        return null;
    }


    private RecognizedFood findUnprocessedLiquidProduct(List<RecognizedFood> recognizedFoods, List<RecognizedFood> processedFoods) {
        for (RecognizedFood recognizedFood : recognizedFoods) {
            Food food = recognizedFood.getFoundFood();
            if (food != null && isLiquidProduct(food.getName())) {

                boolean alreadyProcessed = false;
                for (RecognizedFood processedFood : processedFoods) {
                    if (processedFood.getFoundFood() != null &&
                            processedFood.getFoundFood().getId() == food.getId()) {
                        alreadyProcessed = true;
                        break;
                    }
                }

                if (!alreadyProcessed) {
                    Log.d(TAG, "🥛 DEBUG: Найден необработанный жидкий продукт для объемной порции: '" + food.getName() + "'");
                    return recognizedFood;
                }
            }
        }

        Log.d(TAG, "🚫 DEBUG: Необработанный жидкий продукт для объемной порции не найден");
        return null;
    }


    private float convertVolumetricPortionToMl(String portionCandidate, float quantity) {
        if (portionCandidate == null) return 100f * quantity;

        String lowerPortion = portionCandidate.toLowerCase();
        String normalizedPortion = FOOD_LEMMAS.getOrDefault(lowerPortion, lowerPortion);


        float volumePerUnit = 200f;

        if (normalizedPortion.contains("стакан") || lowerPortion.contains("стакан")) {
            volumePerUnit = 250f;
        } else if (normalizedPortion.contains("кружка") || lowerPortion.contains("кружка")) {
            volumePerUnit = 250f;
        } else if (normalizedPortion.contains("чашка") || lowerPortion.contains("чашка")) {
            volumePerUnit = 150f;
        } else if (normalizedPortion.contains("миска") || lowerPortion.contains("миска")) {
            volumePerUnit = 300f;
        } else if (normalizedPortion.contains("бокал") || lowerPortion.contains("бокал")) {
            volumePerUnit = 200f;
        } else if (normalizedPortion.contains("рюмка") || lowerPortion.contains("рюмка")) {
            volumePerUnit = 50f;
        } else if (normalizedPortion.contains("стопка") || lowerPortion.contains("стопка")) {
            volumePerUnit = 50f;
        } else if (normalizedPortion.contains("фужер") || lowerPortion.contains("фужер")) {
            volumePerUnit = 150f;
        }

        float totalVolume = volumePerUnit * quantity;

        Log.d(TAG, "🔢 DEBUG: Конвертация объемной порции: '" + portionCandidate + "' × " + quantity +
                " = " + volumePerUnit + "мл × " + quantity + " = " + totalVolume + "мл");

        return totalVolume;
    }


    private Map<String, String> createPercentageToProductMapping(List<String> percentages, List<String> foodCandidates, String originalText) {
        Map<String, String> mapping = new HashMap<>();

        if (percentages.isEmpty() || foodCandidates.isEmpty()) {
            return mapping;
        }

        Log.d(TAG, "🎯 DEBUG: Создаем позиционное сопоставление для процентов: " + percentages + " и продуктов: " + foodCandidates);


        Map<Integer, String> percentPositions = findPercentagePositions(percentages, originalText);


        Map<Integer, String> productPositions = findProductPositions(foodCandidates, originalText);

        Log.d(TAG, "🎯 DEBUG: Позиции процентов: " + percentPositions);
        Log.d(TAG, "🎯 DEBUG: Позиции продуктов: " + productPositions);


        for (Map.Entry<Integer, String> percentEntry : percentPositions.entrySet()) {
            int percentPosition = percentEntry.getKey();
            String percentage = percentEntry.getValue();

            String closestProduct = null;
            int minDistance = Integer.MAX_VALUE;

            for (Map.Entry<Integer, String> productEntry : productPositions.entrySet()) {
                int productPosition = productEntry.getKey();
                String product = productEntry.getValue();


                int distance = Math.abs(percentPosition - productPosition);
                if (productPosition < percentPosition) {
                    distance = distance / 2;
                }

                if (distance < minDistance) {
                    minDistance = distance;
                    closestProduct = product;
                }
            }

            if (closestProduct != null) {
                mapping.put(percentage, closestProduct);
                Log.d(TAG, "✅ DEBUG: Сопоставление: '" + percentage + "' → '" + closestProduct + "' (расстояние: " + minDistance + ")");
            }
        }

        return mapping;
    }


    private Map<Integer, String> findPercentagePositions(List<String> percentages, String originalText) {
        Map<Integer, String> positions = new HashMap<>();

        if (originalText == null) {
            return positions;
        }

        String lowerText = originalText.toLowerCase();

        for (String percentage : percentages) {

            String numericPercent = percentage.replace("%", "");
            int digitPosition = lowerText.indexOf(numericPercent + "%");
            if (digitPosition >= 0) {
                positions.put(digitPosition, percentage);
                Log.d(TAG, "📍 DEBUG: Найден цифровой процент '" + percentage + "' на позиции " + digitPosition);
                continue;
            }


            String percentValue = percentage.replace("%", "");
            try {
                float value = Float.parseFloat(percentValue);


                for (Map.Entry<String, Float> entry : RUSSIAN_NUMBERS.entrySet()) {
                    if (Math.abs(entry.getValue() - value) < 0.1f) {
                        String wordNumber = entry.getKey();


                        String[] patterns = {
                                wordNumber + " процент",
                                wordNumber + " процента",
                                wordNumber + " процентов"
                        };

                        for (String pattern : patterns) {
                            int wordPosition = lowerText.indexOf(pattern);
                            if (wordPosition >= 0) {
                                positions.put(wordPosition, percentage);
                                Log.d(TAG, "📍 DEBUG: Найден словесный процент '" + percentage + "' ('" + pattern + "') на позиции " + wordPosition);
                                break;
                            }
                        }
                    }
                }
            } catch (NumberFormatException e) {

            }
        }

        return positions;
    }


    private Map<Integer, String> findProductPositions(List<String> foodCandidates, String originalText) {
        Map<Integer, String> positions = new HashMap<>();

        if (originalText == null) {
            return positions;
        }

        String lowerText = originalText.toLowerCase();

        for (String product : foodCandidates) {
            String lowerProduct = product.toLowerCase();


            int position = lowerText.indexOf(lowerProduct);
            if (position >= 0) {
                positions.put(position, product);
                Log.d(TAG, "📍 DEBUG: Найден продукт '" + product + "' на позиции " + position);
                continue;
            }


            String[] productWords = lowerProduct.split("\\s+");
            if (productWords.length > 1) {

                boolean allWordsFound = true;
                int firstWordPosition = -1;

                for (String word : productWords) {
                    if (word.length() > 2) {
                        int wordPos = lowerText.indexOf(word);
                        if (wordPos >= 0) {
                            if (firstWordPosition < 0) {
                                firstWordPosition = wordPos;
                            }
                        } else {
                            allWordsFound = false;
                            break;
                        }
                    }
                }

                if (allWordsFound && firstWordPosition >= 0) {
                    positions.put(firstWordPosition, product);
                    Log.d(TAG, "📍 DEBUG: Найден составной продукт '" + product + "' начиная с позиции " + firstWordPosition);
                }
            } else if (productWords.length == 1 && productWords[0].length() > 2) {

                int wordPos = lowerText.indexOf(productWords[0]);
                if (wordPos >= 0) {
                    positions.put(wordPos, product);
                    Log.d(TAG, "📍 DEBUG: Найден одиночный продукт '" + product + "' на позиции " + wordPos);
                }
            }
        }

        return positions;
    }


    private boolean areSimilarProducts(String foodName1, String foodName2) {
        if (foodName1 == null || foodName2 == null) {
            return false;
        }

        String lower1 = foodName1.toLowerCase();
        String lower2 = foodName2.toLowerCase();


        String[][] similarityGroups = {
                {"молоко", "молочный", "молочная", "молочное"},
                {"сливки", "сливочный", "сливочная", "сливочное"},
                {"творог", "творожный", "творожная", "творожное"},
                {"кефир", "кефирный", "кефирная"},
                {"йогурт", "йогуртовый", "йогуртовая"},
                {"сметана", "сметанный", "сметанная"},
                {"масло", "масляный", "масляная", "масляное", "маслянистый"},
                {"хлеб", "хлебный", "хлебная", "хлебобулочный", "булочка", "булка", "батон"}
        };

        for (String[] group : similarityGroups) {
            boolean found1 = false, found2 = false;

            for (String keyword : group) {
                if (lower1.contains(keyword)) found1 = true;
                if (lower2.contains(keyword)) found2 = true;
            }

            if (found1 && found2) {
                Log.d(TAG, "🔗 DEBUG: Продукты '" + foodName1 + "' и '" + foodName2 + "' похожи (группа: " + String.join(",", group) + ")");
                return true;
            }
        }

        return false;
    }


    private Set<String> createPercentageWordsSet(List<String> percentages, String originalText) {
        Set<String> percentageWords = new HashSet<>();

        if (percentages.isEmpty() || originalText == null) {
            return percentageWords;
        }

        String lowerText = originalText.toLowerCase();

        for (String percentage : percentages) {

            if (percentage.matches("\\d+[.,]?\\d*%")) {

                continue;
            }


            String percentValue = percentage.replace("%", "");
            try {
                float value = Float.parseFloat(percentValue);


                for (Map.Entry<String, Float> entry : RUSSIAN_NUMBERS.entrySet()) {
                    if (Math.abs(entry.getValue() - value) < 0.1f) {
                        String wordNumber = entry.getKey();


                        String[] percentPatterns = {"процент", "процента", "процентов"};

                        for (String percentWord : percentPatterns) {

                            String pattern = wordNumber + "\\s+" + percentWord;
                            if (lowerText.matches(".*\\b" + pattern + "\\b.*")) {
                                percentageWords.add(wordNumber);
                                percentageWords.add(percentWord);
                                Log.d(TAG, "🚫 DEBUG: Добавлены слова процента для исключения: '" + wordNumber + "', '" + percentWord + "'");
                                break;
                            }
                        }


                        for (String percentWord : percentPatterns) {
                            String drobPattern = wordNumber + "\\s+с\\s+половиной\\s+" + percentWord;
                            if (lowerText.matches(".*\\b" + drobPattern + "\\b.*")) {
                                percentageWords.add(wordNumber);
                                percentageWords.add("с");
                                percentageWords.add("половиной");
                                percentageWords.add(percentWord);
                                Log.d(TAG, "🚫 DEBUG: Добавлены слова дробного процента: '" + wordNumber + " с половиной " + percentWord + "'");
                                break;
                            }
                        }
                    }
                }
            } catch (NumberFormatException e) {

            }
        }

        return percentageWords;
    }
}

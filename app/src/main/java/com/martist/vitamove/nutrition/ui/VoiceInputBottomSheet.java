package com.martist.vitamove.nutrition.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.martist.vitamove.R;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.data.services.VoiceInputService;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.domain.utils.PluralizationUtil;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.data.repository.SupabaseFoodRepository;
import com.martist.vitamove.nutrition.ui.adapter.RecognizedFoodAdapter;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Portion;

import java.util.ArrayList;
import java.util.List;


public class VoiceInputBottomSheet extends BottomSheetDialogFragment
        implements VoiceInputService.VoiceInputCallback {

    private static final String TAG = "VoiceInputBottomSheet";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 200;


    private ImageButton micButton;
    private TextView statusText;
    private TextView recognizedText;
    private RecyclerView recognizedFoodsRecyclerView;
    private TextView emptyProductsText;
    private ChipGroup mealTypeChipGroup;
    private MaterialButton addFoodsButton;


    private VoiceInputService voiceInputService;
    private RecognizedFoodAdapter recognizedFoodAdapter;
    private FoodManager foodManager;
    private SupabaseFoodRepository foodRepository;


    private boolean isListening = false;
    private boolean isAddingFoods = false;
    private List<VoiceInputService.RecognizedFood> recognizedFoods = new ArrayList<>();
    private String selectedMealType = Constants.MEAL_TYPE_BREAKFAST;


    public interface OnFoodsAddedListener {
        void onFoodsAdded(List<VoiceInputService.RecognizedFood> foods, String mealType);
    }

    private OnFoodsAddedListener onFoodsAddedListener;


    private ActivityResultLauncher<Intent> portionSizeLauncher;

    public static VoiceInputBottomSheet newInstance() {
        return new VoiceInputBottomSheet();
    }


    public void setOnFoodsAddedListener(OnFoodsAddedListener listener) {
        this.onFoodsAddedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetStyle);


        foodManager = FoodManager.getInstance(requireContext());


        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );
        foodRepository = new SupabaseFoodRepository(supabaseClient, requireContext());

        voiceInputService = new VoiceInputService(requireContext(), foodManager, foodRepository);


        setupPortionSizeLauncher();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);


        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_voice_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupMealTypeSelection();
        setupClickListeners();
        updateUI();
    }

    @Override
    public void onStart() {
        super.onStart();
        setupBottomSheetBackground();
    }

    @Override
    public void onResume() {
        super.onResume();

        setupBottomSheetBackground();
    }

    private void setupBottomSheetBackground() {
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bottom_sheet_background_simple);


                if (getDialog().getWindow() != null) {
                    getDialog().getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    getDialog().getWindow().setDimAmount(0.5f);
                }


                bottomSheet.post(() -> {
                    bottomSheet.setBackgroundResource(R.drawable.bottom_sheet_background_simple);
                });
            }
        }
    }


    private void initViews(View view) {
        micButton = view.findViewById(R.id.mic_button);
        statusText = view.findViewById(R.id.status_text);
        recognizedText = view.findViewById(R.id.recognized_text);
        recognizedFoodsRecyclerView = view.findViewById(R.id.recognized_foods_recyclerview);
        emptyProductsText = view.findViewById(R.id.empty_products_text);
        mealTypeChipGroup = view.findViewById(R.id.meal_type_chip_group);
        addFoodsButton = view.findViewById(R.id.add_foods_button);
    }


    private void setupRecyclerView() {
        recognizedFoodAdapter = new RecognizedFoodAdapter(recognizedFoods);
        recognizedFoodsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recognizedFoodsRecyclerView.setAdapter(recognizedFoodAdapter);


        recognizedFoodAdapter.setOnItemClickListener(this::onFoodItemClick);
        recognizedFoodAdapter.setOnDeleteClickListener(this::onDeleteFoodClick);
    }


    private void setupMealTypeSelection() {

        Chip breakfastChip = mealTypeChipGroup.findViewById(R.id.chip_breakfast);
        if (breakfastChip != null) {
            breakfastChip.setChecked(true);
        }

        mealTypeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);

                if (checkedId == R.id.chip_breakfast) {
                    selectedMealType = Constants.MEAL_TYPE_BREAKFAST;
                } else if (checkedId == R.id.chip_lunch) {
                    selectedMealType = Constants.MEAL_TYPE_LUNCH;
                } else if (checkedId == R.id.chip_dinner) {
                    selectedMealType = Constants.MEAL_TYPE_DINNER;
                } else if (checkedId == R.id.chip_snack) {
                    selectedMealType = Constants.MEAL_TYPE_SNACK;
                }
            }
        });
    }


    private void setupClickListeners() {
        micButton.setOnClickListener(v -> {
            if (isListening) {
                stopVoiceInput();
            } else {
                startVoiceInput();
            }
        });

        addFoodsButton.setOnClickListener(v -> addSelectedFoods());
    }


    private void startVoiceInput() {
        if (!checkMicrophonePermission()) {
            requestMicrophonePermission();
            return;
        }


        recognizedText.setText("");

        voiceInputService.startVoiceInput(this);
    }


    private void stopVoiceInput() {
        voiceInputService.stopVoiceInput();
    }


    private void addSelectedFoods() {
        if (isAddingFoods) {

            return;
        }

        if (recognizedFoods.isEmpty()) {
            Toast.makeText(requireContext(), "Нет продуктов для добавления", Toast.LENGTH_SHORT).show();
            return;
        }

        isAddingFoods = true;
        addFoodsButton.setEnabled(false);


        Log.d(TAG, "Начинаем добавление " + recognizedFoods.size() + " продуктов в " + selectedMealType);

        for (int i = 0; i < recognizedFoods.size(); i++) {
            VoiceInputService.RecognizedFood recognizedFood = recognizedFoods.get(i);
            Food food = recognizedFood.getFoundFood();
            if (food != null) {

                float quantity;
                String unit;

                if (recognizedFood.getFoundPortion() != null) {

                    quantity = recognizedFood.getQuantity();
                    unit = recognizedFood.getUnit();

                    Log.d(TAG, "[" + (i + 1) + "/" + recognizedFoods.size() + "] Добавляем продукт с порцией: " + food.getName() +
                            ", порция: " + recognizedFood.getDisplayQuantity() +
                            " (" + quantity + " " + unit + "), итоговый вес: " + recognizedFood.getTotalWeightInGrams() + "г, ID: " + food.getId());
                } else {

                    quantity = recognizedFood.getQuantity();
                    unit = recognizedFood.getUnit();

                    Log.d(TAG, "[" + (i + 1) + "/" + recognizedFoods.size() + "] Добавляем продукт без порции: " + food.getName() +
                            ", количество: " + quantity + " " + unit + ", ID: " + food.getId());
                }


                try {
                    foodManager.addFoodToMeal(selectedMealType, food, quantity, unit);

                    foodManager.addToRecents(food, quantity, unit);
                    Log.d(TAG, "Успешно добавлен продукт: " + food.getName());
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка добавления продукта: " + food.getName(), e);
                }
            }
        }

        Log.d(TAG, "Завершено добавление всех продуктов");


        StringBuilder message = new StringBuilder("Добавлено " + recognizedFoods.size());
        if (recognizedFoods.size() == 1) {
            message.append(" продукт");
        } else {
            message.append(" продуктов");
        }
        message.append(" в ").append(getMealTypeName(selectedMealType));


        int portionCount = 0;
        for (VoiceInputService.RecognizedFood recognizedFood : recognizedFoods) {
            if (recognizedFood.getFoundPortion() != null) {
                portionCount++;
            }
        }

        if (portionCount > 0) {
            message.append(" (").append(portionCount).append(" с порциями)");
        }

        Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_SHORT).show();


        if (onFoodsAddedListener != null) {
            Log.d(TAG, "Уведомляем listener об обновлении UI");
            onFoodsAddedListener.onFoodsAdded(new ArrayList<>(recognizedFoods), selectedMealType);
        }


        isAddingFoods = false;
        Log.d(TAG, "Сброшен флаг добавления продуктов");

        dismiss();
    }


    private void updateUI() {
        if (isListening) {
            micButton.setImageResource(R.drawable.ic_mic_off);
            statusText.setText("Говорите...");
            addFoodsButton.setEnabled(false);
        } else {
            micButton.setImageResource(R.drawable.ic_mic);
            statusText.setText("Готов к записи");

            addFoodsButton.setEnabled(!recognizedFoods.isEmpty() && !isAddingFoods);
        }


        boolean hasProducts = !recognizedFoods.isEmpty();
        recognizedFoodsRecyclerView.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
        emptyProductsText.setVisibility(hasProducts ? View.GONE : View.VISIBLE);

        if (isAddingFoods) {
            addFoodsButton.setText("Добавляю...");
        } else {
            if (recognizedFoods.isEmpty()) {
                addFoodsButton.setText("Добавить продукты");
            } else {
                int count = recognizedFoods.size();
                String pluralProducts = PluralizationUtil.getPlural(count, "продукт");
                addFoodsButton.setText("Добавить " + count + " " + pluralProducts);
            }
        }
    }


    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }


    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(requireContext(),
                        "Для голосового ввода необходимо разрешение на использование микрофона",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    private String getMealTypeName(String mealType) {
        switch (mealType) {
            case Constants.MEAL_TYPE_BREAKFAST:
                return "Завтрак";
            case Constants.MEAL_TYPE_LUNCH:
                return "Обед";
            case Constants.MEAL_TYPE_DINNER:
                return "Ужин";
            case Constants.MEAL_TYPE_SNACK:
                return "Перекус";
            default:
                return mealType;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (voiceInputService != null) {
            voiceInputService.destroy();
        }
    }


    @Override
    public void onListeningStarted() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                isListening = true;
                updateUI();
            });
        }
    }

    @Override
    public void onTextRecognized(String text) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                recognizedText.setText(text);
                statusText.setText("Ищу продукты...");
            });
        }
    }

    @Override
    public void onFoodRecognized(List<VoiceInputService.RecognizedFood> foods) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {

                recognizedFoods.addAll(foods);
                recognizedFoodAdapter.notifyDataSetChanged();

                if (foods.isEmpty()) {
                    statusText.setText("Продукты не найдены. Попробуйте еще раз.");
                } else {
                    statusText.setText("Найдено продуктов: " + foods.size() + " (всего: " + recognizedFoods.size() + ")");
                }


                updateUI();
            });
        }
    }

    @Override
    public void onError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {

                isListening = false;

                statusText.setText("Ошибка: " + error);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();


                updateUI();
            });
        }
    }

    @Override
    public void onComplete() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                isListening = false;
                updateUI();
            });
        }
    }


    private void onFoodItemClick(VoiceInputService.RecognizedFood recognizedFood, int position) {
        if (recognizedFood.getFoundFood() != null) {
            openPortionDialog(recognizedFood, position);
        } else {
            Toast.makeText(requireContext(), "Продукт не найден в базе", Toast.LENGTH_SHORT).show();
        }
    }


    private void onDeleteFoodClick(VoiceInputService.RecognizedFood recognizedFood, int position) {
        recognizedFoods.remove(position);
        recognizedFoodAdapter.notifyItemRemoved(position);
        recognizedFoodAdapter.notifyItemRangeChanged(position, recognizedFoods.size());
        updateUI();
    }


    private void setupPortionSizeLauncher() {
        portionSizeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Получен результат из PortionSizeActivity. ResultCode: " + result.getResultCode());

                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {

                            int position = data.getIntExtra("recognized_food_position", -1);
                            float newQuantity = data.getFloatExtra("portion_quantity", 0f);
                            String newPortionName = data.getStringExtra("portion_name");

                            Log.d(TAG, "Полученные данные: position=" + position +
                                    ", quantity=" + newQuantity + ", portionName=" + newPortionName);
                            Log.d(TAG, "Размер списка recognizedFoods: " + recognizedFoods.size());

                            if (position >= 0 && position < recognizedFoods.size()) {
                                VoiceInputService.RecognizedFood oldFood = recognizedFoods.get(position);

                                Log.d(TAG, "Обновляем продукт: " + oldFood.getName() +
                                        " (старые данные: " + oldFood.getQuantity() + " " + oldFood.getUnit() + ")");


                                Portion newPortion = null;
                                if (newPortionName != null && oldFood.getFoundFood() != null &&
                                        oldFood.getFoundFood().getPortions() != null) {

                                    for (Portion portion : oldFood.getFoundFood().getPortions()) {
                                        if (portion.getName() != null &&
                                                (portion.getName().toLowerCase().equals(newPortionName.toLowerCase()) ||
                                                        portion.getName().toLowerCase().contains(newPortionName.toLowerCase()) ||
                                                        newPortionName.toLowerCase().contains(portion.getName().toLowerCase()))) {
                                            newPortion = portion;
                                            Log.d(TAG, "Найдена новая порция: " + portion.getName() + " (" + portion.getWeight() + "г)");
                                            break;
                                        }
                                    }
                                }

                                if (newPortion == null) {
                                    Log.d(TAG, "Новая порция не найдена в БД, используем единицу измерения: " + newPortionName);
                                }


                                VoiceInputService.RecognizedFood updatedFood = new VoiceInputService.RecognizedFood(
                                        oldFood.getName(),
                                        newQuantity,
                                        newPortionName != null ? newPortionName : "г",
                                        oldFood.getFoundFood(),
                                        newPortion
                                );

                                Log.d(TAG, "Создан новый RecognizedFood: " + updatedFood.getName() +
                                        " (новые данные: " + updatedFood.getQuantity() + " " + updatedFood.getUnit() + ")" +
                                        " displayQuantity=" + updatedFood.getDisplayQuantity() +
                                        " totalWeight=" + updatedFood.getTotalWeightInGrams() + "г");


                                recognizedFoods.set(position, updatedFood);


                                recognizedFoodAdapter.notifyDataSetChanged();

                                Toast.makeText(requireContext(), "Порция обновлена", Toast.LENGTH_SHORT).show();

                                Log.d(TAG, "Продукт успешно обновлен: " + updatedFood.getName() +
                                        " (новые данные: " + updatedFood.getQuantity() + " " + updatedFood.getUnit() + ")");
                            } else {
                                Log.e(TAG, "Неверная позиция продукта: " + position +
                                        " (размер списка: " + recognizedFoods.size() + ")");
                            }
                        } else {
                            Log.e(TAG, "Данные результата равны null");
                        }
                    } else {
                        Log.d(TAG, "Результат не RESULT_OK: " + result.getResultCode());
                    }
                });
    }


    private void openPortionDialog(VoiceInputService.RecognizedFood recognizedFood, int position) {
        Log.d(TAG, "Открываем PortionSizeActivity для продукта: " + recognizedFood.getName() +
                " на позиции " + position);
        Log.d(TAG, "Текущие данные продукта: quantity=" + recognizedFood.getQuantity() +
                ", unit=" + recognizedFood.getUnit());


        Intent intent = new Intent(requireContext(), PortionSizeActivity.class);
        intent.putExtra(Constants.EXTRA_FOOD, recognizedFood.getFoundFood());
        intent.putExtra("is_ingredient_selection", true);
        intent.putExtra("portion_quantity", recognizedFood.getQuantity());
        intent.putExtra("portion_name", recognizedFood.getUnit());
        intent.putExtra(Constants.EXTRA_PORTION_SIZE, (int) recognizedFood.getQuantity());
        intent.putExtra("recognized_food_position", position);

        Log.d(TAG, "Запускаем PortionSizeActivity с recognized_food_position=" + position);
        portionSizeLauncher.launch(intent);
    }
}

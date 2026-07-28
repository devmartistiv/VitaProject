package com.martist.vitamove.user;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.BMICalculator;
import com.martist.vitamove.core.domain.utils.ImageUtils;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.core.ui.views.DevelopmentOverlay;
import com.martist.vitamove.databinding.FragmentProfileBinding;
import com.martist.vitamove.measurement.BodyMeasurementsActivity;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.ui.CaloriesExplanationActivity;
import com.martist.vitamove.water.ui.WaterBalanceFragment;
import com.martist.vitamove.weight.ui.WeightHistoryActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_EDIT_PROFILE = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    private static final int REQUEST_IMAGE_CAPTURE = 1003;
    private static final String PREFS_USER_DATA = "user_data";
    private static final String AVATAR_FILE_NAME = "profile_avatar.jpg";
    private static final String TEMP_PHOTO_FILE_NAME = "temp_profile_photo.jpg";


    private UserProfile userProfile;

    private FragmentProfileBinding binding;

    private Uri cameraPhotoUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        initViews(view);
        loadProfileData(view);


        applyDevelopmentOverlaysToAchievements(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusbar_color));


            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    public void onResume() {
        super.onResume();


        if (getView() != null) {
            loadProfileData(getView());
        }


        Log.d("ProfileFragment", "onResume: обновление профиля");
    }

    private void initViews(View view) {
        binding.profileImage.setOnClickListener(v -> openGallery());
        binding.settingsButton.setOnClickListener(v -> openSettings());
        binding.profileWater.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new WaterBalanceFragment()).commit();
        });
        binding.viewAllAchievementsButton.setOnClickListener(v -> openAchievements());
        binding.physicalParamsCard.setOnClickListener(v -> openBodyMeasurements());
        binding.caloriesCard.setOnClickListener(v -> openCaloriesExplanation());
    }

    public void loadProfileData(View view) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);


        String name = prefs.getString("name", "Пользователь");
        int age = prefs.getInt("age", 30);
        String gender = prefs.getString("gender", "Мужчина");
        float currentWeightFloat = prefs.getFloat("current_weight", 0);
        float targetWeightFloat = prefs.getFloat("target_weight", 0);
        float height = prefs.getFloat("height", 0);
        float bodyFat = prefs.getFloat("body_fat", 0);
        float waist = prefs.getFloat("waist", 0);


        userProfile = new UserProfile(name, age, gender, currentWeightFloat, targetWeightFloat, height, bodyFat, waist);


        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");
        String currentGoal = prefs.getString("fitness_goal", "weight_loss");


        if (!fitnessGoal.equals(currentGoal)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("fitness_goal", fitnessGoal);
            editor.apply();
            Log.d("ProfileFragment", "Обновлена цель тренировки в профиле: " + fitnessGoal);
        }


        boolean dataFromSupabase = prefs.getBoolean("is_synchronized", false);
        int targetCalories = prefs.getInt("target_calories", 0);
        float targetWater = prefs.getFloat("target_water", 0);


        if (dataFromSupabase && targetCalories > 0) {

            userProfile.setTargetCalories(targetCalories);
            userProfile.setTargetWater(targetWater > 0 ? targetWater : userProfile.calculateTargetWater());

            Log.d("ProfileFragment", "Используем данные из Supabase без перерасчета: калории=" +
                    targetCalories + ", вода=" + targetWater);
        } else {

            userProfile.updateTargetCalories();
            userProfile.updateTargetWater();


            int updatedTargetCalories = userProfile.getTargetCalories();
            float updatedTargetWater = userProfile.getTargetWater();


            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("target_calories", updatedTargetCalories);
            editor.putFloat("target_water", updatedTargetWater);
            editor.apply();

            Log.d("ProfileFragment", "Данные рассчитаны локально: калории=" +
                    updatedTargetCalories + ", вода=" + updatedTargetWater);
        }


        DashboardManager dashboardManager =
                DashboardManager.getInstance(requireContext());
        dashboardManager.updateWaterGoalFromProfile();
        dashboardManager.updateCaloriesGoalFromProfile();


        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNavigationHeader();
        }


        binding.userName.setText(userProfile.getName());
        binding.userGender.setText(userProfile.getGender());
        binding.userAge.setText(String.format("%d %s", userProfile.getAge(), getAgeString(userProfile.getAge())));
        binding.currentWeightValue.setText(String.format("%.1f кг", userProfile.getCurrentWeight()));
        binding.targetWeightValue.setText(String.format("%.1f кг", userProfile.getTargetWeight()));
        binding.heightValue.setText(String.format("%.0f см", userProfile.getHeight()));


        updateBMIData(view);
        updateEnergyData();
        updateWaterData();


        loadProfileAvatar();
    }

    private void updateBMIData(View view) {

        float bmi = BMICalculator.calculateBMI(userProfile.getCurrentWeight(), userProfile.getHeight());


        DecimalFormat df = new DecimalFormat("#.#");
        binding.bmiValue.setText(df.format(bmi));


        View bmiMarkerCard = view.findViewById(R.id.bmiMarkerCard);


        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bmiMarkerCard.getLayoutParams();


        float markerPosition;
        if (bmi < 18.5f) {

            markerPosition = (bmi / 18.5f) * 20.0f;
            binding.bmiValue.setTextColor(getResources().getColor(R.color.green_500));
            binding.bmiDescription.setText("У Вас недостаточный вес. Рекомендуется набрать вес для улучшения здоровья.");
        } else if (bmi < 25.0f) {

            markerPosition = 20.0f + ((bmi - 18.5f) / (25.0f - 18.5f)) * 20.0f;
            binding.bmiValue.setTextColor(getResources().getColor(R.color.light_green_500));
            binding.bmiDescription.setText("У Вас нормальный вес. Продолжайте поддерживать здоровый образ жизни.");
        } else if (bmi < 30.0f) {

            markerPosition = 40.0f + ((bmi - 25.0f) / (30.0f - 25.0f)) * 20.0f;
            binding.bmiValue.setTextColor(getResources().getColor(R.color.yellow_500));
            binding.bmiDescription.setText("У Вас есть избыточный вес. Работайте над снижением веса для улучшения здоровья.");
        } else if (bmi < 35.0f) {

            markerPosition = 60.0f + ((bmi - 30.0f) / (35.0f - 30.0f)) * 20.0f;
            binding.bmiValue.setTextColor(getResources().getColor(R.color.orange_500));
            binding.bmiDescription.setText("У Вас ожирение I степени. Рекомендуется обратиться к специалисту для снижения веса.");
        } else {

            markerPosition = 80.0f + Math.min(((bmi - 35.0f) / 15.0f) * 20.0f, 20.0f);
            binding.bmiValue.setTextColor(getResources().getColor(R.color.red_500));
            binding.bmiDescription.setText("У Вас выраженное ожирение. Необходима консультация врача и план по снижению веса.");
        }


        final View container = (View) bmiMarkerCard.getParent();


        if (container.getWidth() <= 0) {

            container.post(() -> updateBMIMarkerPosition(bmiMarkerCard, container, markerPosition));
        } else {

            updateBMIMarkerPosition(bmiMarkerCard, container, markerPosition);
        }
    }


    private void updateBMIMarkerPosition(View markerCard, View container, float markerPosition) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) markerCard.getLayoutParams();

        float containerWidth = container.getWidth();
        float markerWidth = markerCard.getWidth();


        float absolutePosition = (containerWidth - markerWidth) * (markerPosition / 100f);


        params.setMarginStart((int) absolutePosition);
        markerCard.setLayoutParams(params);
    }


    private void updateEnergyData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);


        int customCalories = prefs.getInt("custom_calories", 0);
        int targetCalories;

        if (customCalories > 0) {

            targetCalories = customCalories;
            Log.d("ProfileFragment", "Используем пользовательскую цель: " + targetCalories + " ккал");
        } else {

            userProfile.updateTargetCalories();
            targetCalories = userProfile.getTargetCalories();
            Log.d("ProfileFragment", "Используем рассчитанную цель: " + targetCalories + " ккал");
        }


        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("target_calories", targetCalories);
        editor.apply();
        binding.dailyCaloriesValue.setText(String.format("%d ккал", targetCalories));

        try {
            FoodManager foodManager = FoodManager.getInstance(requireContext());
            foodManager.updateTargetCalories(targetCalories);
        } catch (Exception e) {
            Log.e("ProfileFragment", "Ошибка при обновлении целевых калорий в FoodManager", e);
        }
    }

    private float calculateBMR() {

        boolean isMale = userProfile.getGender().equalsIgnoreCase("Мужчина");
        float bmr;

        if (isMale) {
            bmr = 10 * userProfile.getCurrentWeight() + 6.25f * userProfile.getHeight() - 5 * userProfile.getAge() + 5;
        } else {
            bmr = 10 * userProfile.getCurrentWeight() + 6.25f * userProfile.getHeight() - 5 * userProfile.getAge() - 161;
        }

        return bmr;
    }

    private String getAgeString(int age) {
        int lastDigit = age % 10;
        int lastTwoDigits = age % 100;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
            return "лет";
        }

        if (lastDigit == 1) {
            return "год";
        }

        if (lastDigit >= 2 && lastDigit <= 4) {
            return "года";
        }

        return "лет";
    }


    private void openSettings() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }

    private void openWeightHistory() {
        Intent intent = new Intent(getActivity(), WeightHistoryActivity.class);
        startActivity(intent);
    }


    private void openBodyMeasurements() {
        Intent intent = new Intent(getActivity(), BodyMeasurementsActivity.class);
        startActivity(intent);
    }


    private void openCaloriesExplanation() {
        Intent intent = new Intent(getActivity(), CaloriesExplanationActivity.class);
        startActivity(intent);
    }

    private void openAchievements() {

    }


    private void openGallery() {

        String[] options = {"Камера", "Галерея"};

        new MaterialAlertDialogBuilder(requireActivity(), R.style.MyAlertDialogTheme)
                .setTitle("Выберите источник")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {

                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_DENIED)
                            takePictureFromCamera();
                        else
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                    } else {

                        pickImageFromGallery();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void takePictureFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = new File(requireContext().getExternalCacheDir(), TEMP_PHOTO_FILE_NAME);
            } catch (Exception e) {
                Log.e("ProfileFragment", "Ошибка при создании файла для камеры: " + e.getMessage(), e);
            }


            if (photoFile != null) {

                cameraPhotoUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", photoFile);


                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(requireContext(), "Не удалось создать файл для фото", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "На устройстве нет приложения камеры", Toast.LENGTH_SHORT).show();
        }
    }


    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == Activity.RESULT_OK) {

            if (getView() != null) {
                loadProfileData(getView());
            }


            Toast.makeText(requireContext(), "Нормы калорий и потребления воды пересчитаны с учетом новых данных", Toast.LENGTH_SHORT).show();


            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNavigationHeader();
            }
        } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                saveAndDisplayAvatar(selectedImageUri);
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            if (cameraPhotoUri != null) {
                saveAndDisplayAvatar(cameraPhotoUri);
            }
        }
    }


    private void saveAndDisplayAvatar(Uri imageUri) {
        try {

            boolean saveSuccess = ImageUtils.saveImageToInternalStorage(requireContext(), imageUri, AVATAR_FILE_NAME);

            if (saveSuccess) {

                Bitmap avatarBitmap = ImageUtils.loadImageFromInternalStorage(requireContext(), AVATAR_FILE_NAME);
                if (avatarBitmap != null) {

                    binding.profileImage.setImageBitmap(avatarBitmap);


                    SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("profile_image", imageUri.toString());
                    editor.apply();

                    Toast.makeText(requireContext(), "Аватар успешно обновлен", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Не удалось сохранить аватар", Toast.LENGTH_SHORT).show();
                Log.e("ProfileFragment", "Не удалось сохранить аватар во внутреннее хранилище");
            }
        } catch (Exception e) {
            Log.e("ProfileFragment", "Ошибка при обработке аватара: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Ошибка при обработке фото", Toast.LENGTH_SHORT).show();
        }
    }


    private void applyDevelopmentOverlaysToAchievements(View view) {

        View achievementsCard = view.findViewById(R.id.achievements_card);
        if (achievementsCard != null) {
            DevelopmentOverlay.applyToView(achievementsCard);
        }


    }


    private void updateWaterData() {
        if (userProfile != null) {
            float targetWater = userProfile.getTargetWater();
            binding.waterIntakeValue.setText(String.format("%.1f л", targetWater));


            SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");


            Log.d("ProfileFragment", "Отображение нормы воды: " + targetWater + " л для цели " + fitnessGoal);
        } else {
            Log.e("ProfileFragment", "Не удалось обновить данные о потреблении воды: компонент или профиль равны null");
        }
    }


    private void loadProfileAvatar() {
        try {

            if (ImageUtils.imageExists(requireContext(), AVATAR_FILE_NAME)) {

                Bitmap avatarBitmap = ImageUtils.loadImageFromInternalStorage(requireContext(), AVATAR_FILE_NAME);
                if (avatarBitmap != null) {
                    binding.profileImage.setImageBitmap(avatarBitmap);
                    Log.d("ProfileFragment", "Аватар успешно загружен из внутреннего хранилища");
                    return;
                }
            }


            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
            String avatarUriString = prefs.getString("profile_image", null);

            if (avatarUriString != null) {
                try {
                    Uri avatarUri = Uri.parse(avatarUriString);

                    try (InputStream inputStream = requireContext().getContentResolver().openInputStream(avatarUri)) {
                        if (inputStream != null) {

                            if (ImageUtils.saveImageToInternalStorage(requireContext(), avatarUri, AVATAR_FILE_NAME)) {

                                Bitmap avatarBitmap = ImageUtils.loadImageFromInternalStorage(requireContext(), AVATAR_FILE_NAME);
                                if (avatarBitmap != null) {
                                    binding.profileImage.setImageBitmap(avatarBitmap);
                                    Log.d("ProfileFragment", "Аватар успешно перенесен из URI в хранилище");
                                }
                            }
                        } else {

                            prefs.edit().remove("profile_image").apply();
                        }
                    } catch (IOException e) {

                        prefs.edit().remove("profile_image").apply();
                    }
                } catch (Exception e) {

                    prefs.edit().remove("profile_image").apply();
                }
            }
        } catch (Exception e) {
            Log.e("ProfileFragment", "Ошибка при загрузке аватара: " + e.getMessage(), e);
        }
    }

} 
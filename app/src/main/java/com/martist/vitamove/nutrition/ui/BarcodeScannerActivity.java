package com.martist.vitamove.nutrition.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.gigachat.GigaChatProductService;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.data.repository.SupabaseBarcodeRepository;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.openFoodFactsApi.OpenFoodFactsService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerActivity extends BaseActivity {
    private static final String TAG = "BarcodeScannerActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private PreviewView cameraPreview;
    private ExecutorService cameraExecutor;
    private final boolean isScanning = true;
    private boolean isSearching = false;
    private OpenFoodFactsService openFoodFactsService;
    private View scannerLine;
    private ValueAnimator lineAnimator;
    private FoodManager foodManager;
    private AlertDialog loadingDialog;
    private AlertDialog productNameDialog;
    private String lastScannedBarcode;
    private boolean isScanOnlyMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);


        String mode = getIntent().getStringExtra("MODE");
        isScanOnlyMode = "SCAN_ONLY".equals(mode);

        cameraPreview = findViewById(R.id.camera_preview);

        scannerLine = findViewById(R.id.scanner_line);

        findViewById(R.id.close_button).setOnClickListener(v -> finish());


        foodManager = FoodManager.getInstance(this);


        foodManager.loadFoodsAsync(() -> {
            Log.d(TAG, "Продукты успешно загружены в фоновом потоке");
        });


        openFoodFactsService = new OpenFoodFactsService();


        setupScannerAnimation();


        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupScannerAnimation() {
        View barcodeFrame = findViewById(R.id.barcode_frame);


        barcodeFrame.post(() -> {

            int frameHeight = barcodeFrame.getHeight();


            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) scannerLine.getLayoutParams();
            params.topMargin = 0;
            scannerLine.setLayoutParams(params);


            scannerLine.setAlpha(0f);


            scannerLine.animate()
                    .alpha(0.9f)
                    .setDuration(300)
                    .withEndAction(() -> {

                        startScanLineAnimation(barcodeFrame, frameHeight);
                    })
                    .start();
        });
    }

    private void startScanLineAnimation(View barcodeFrame, int frameHeight) {

        lineAnimator = ValueAnimator.ofFloat(40, frameHeight - 40);
        lineAnimator.setDuration(2500);
        lineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        lineAnimator.setRepeatMode(ValueAnimator.REVERSE);
        lineAnimator.setInterpolator(new LinearInterpolator());


        lineAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            scannerLine.setY(barcodeFrame.getY() + animatedValue);
        });


        lineAnimator.start();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Требуется разрешение на использование камеры",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);


        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E)
                .build();


        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();


                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());


                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();


                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);


                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Ошибка при инициализации камеры: " + e.getMessage(), e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy image) {
        if (!isScanning || isSearching) {
            image.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(), image.getImageInfo().getRotationDegrees());

        BarcodeScanning.getClient()
                .process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty() && isScanning && !isSearching) {
                        String barcode = barcodes.get(0).getRawValue();
                        Log.d(TAG, "Обнаружен штрихкод: " + barcode);
                        isSearching = true;


                        lastScannedBarcode = barcode;


                        if (isScanOnlyMode) {
                            Log.d(TAG, "Режим SCAN_ONLY: возвращаем штрихкод " + barcode);
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("BARCODE", barcode);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                            return;
                        }

                        showLoadingDialog();


                        new Thread(() -> {

                            SupabaseBarcodeRepository barcodeRepository = foodManager.getBarcodeRepository();
                            if (barcodeRepository != null) {

                                Food food = barcodeRepository.findFoodByBarcode(barcode);

                                if (food != null) {

                                    runOnUiThread(() -> {
                                        Log.d(TAG, "Продукт найден в локальной базе данных: " + food.getName());
                                        hideLoadingDialog();
                                        openPortionSizeActivity(food);
                                    });
                                    return;
                                }
                            }


                            runOnUiThread(() -> {
                                Log.d(TAG, "Продукт не найден в локальной базе, ищем в OpenFoodFacts");


                                openFoodFactsService.getProductByBarcode(barcode, new OpenFoodFactsService.ProductListener() {
                                    @Override
                                    public void onProductFound(Food food) {
                                        runOnUiThread(() -> {


                                            foodManager.findFoodByExactNameAsync(food.getName(), localFood -> {
                                                if (localFood != null) {
                                                    Log.d(TAG, "Найден локальный продукт с именем '" + food.getName() +
                                                            "', используем его вместо данных из OpenFoodFacts");


                                                    Food combinedFood = new Food.Builder()
                                                            .id(localFood.getId())
                                                            .name(food.getName())
                                                            .category(food.getCategory())
                                                            .subcategory(food.getSubcategory())
                                                            .calories(food.getCalories())
                                                            .proteins(food.getProteins())
                                                            .fats(food.getFats())
                                                            .carbs(food.getCarbs())
                                                            .popularity(localFood.getPopularity())
                                                            .calcium(food.getCalcium())
                                                            .iron(food.getIron())
                                                            .magnesium(food.getMagnesium())
                                                            .phosphorus(food.getPhosphorus())
                                                            .potassium(food.getPotassium())
                                                            .sodium(food.getSodium())
                                                            .zinc(food.getZinc())
                                                            .vitaminA(food.getVitaminA())
                                                            .vitaminB1(food.getVitaminB1())
                                                            .vitaminB2(food.getVitaminB2())
                                                            .vitaminB3(food.getVitaminB3())
                                                            .vitaminB5(food.getVitaminB5())
                                                            .vitaminB6(food.getVitaminB6())
                                                            .vitaminB9(food.getVitaminB9())
                                                            .vitaminB12(food.getVitaminB12())
                                                            .vitaminC(food.getVitaminC())
                                                            .vitaminD(food.getVitaminD())
                                                            .vitaminE(food.getVitaminE())
                                                            .vitaminK(food.getVitaminK())
                                                            .cholesterol(food.getCholesterol())
                                                            .saturatedFats(food.getSaturatedFats())
                                                            .transFats(food.getTransFats())
                                                            .fiber(food.getFiber())
                                                            .sugar(food.getSugar())
                                                            .usefulness_index(food.getUsefulnessIndex())
                                                            .build();

                                                    hideLoadingDialog();
                                                    openPortionSizeActivity(combinedFood);
                                                } else {

                                                    Log.d(TAG, "Продукт с именем '" + food.getName() + "' не найден в локальной базе, используем данные из OpenFoodFacts");
                                                    hideLoadingDialog();
                                                    openPortionSizeActivity(food);
                                                }
                                            });
                                        });
                                    }

                                    @Override
                                    public void onProductNotFound() {
                                        runOnUiThread(() -> {
                                            hideLoadingDialog();
                                            showProductNameDialog();
                                        });
                                    }

                                    @Override
                                    public void onError(String message) {
                                        runOnUiThread(() -> {
                                            hideLoadingDialog();
                                            showProductNameDialog();
                                        });
                                    }

                                    @Override
                                    public void onProductFoundWithoutNutrients(String productName) {
                                        runOnUiThread(() -> {
                                            Log.d(TAG, "Найдено название продукта без нутриентов: " + productName);


                                            foodManager.findFoodByExactNameAsync(productName, localFood -> {
                                                if (localFood != null) {
                                                    Log.d(TAG, "Найден локальный продукт с именем '" + productName +
                                                            "', используем его вместо поиска через GigaChat");

                                                    hideLoadingDialog();
                                                    openPortionSizeActivity(localFood);
                                                } else {

                                                    Log.d(TAG, "Продукт с именем '" + productName +
                                                            "' не найден в локальной базе, ищем через GigaChat");
                                                    searchProductWithGigaChat(productName);
                                                }
                                            });
                                        });
                                    }
                                });
                            });
                        }).start();
                    }
                    image.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка сканирования штрихкода", e);
                    image.close();
                });
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        loadingDialog = builder.setView(dialogView)
                .setCancelable(false)
                .create();

        cancelButton.setOnClickListener(v -> {
            isSearching = false;
            if (loadingDialog != null) {
                loadingDialog.dismiss();
            }
            finish();
        });

        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showProductNameDialog() {
        showProductNameDialog(null);
    }

    private void showProductNameDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_product_name, null);


        EditText productNameInput = dialogView.findViewById(R.id.product_name_input);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button searchButton = dialogView.findViewById(R.id.search_button);


        builder.setView(dialogView)
                .setCancelable(false);

        productNameDialog = builder.create();


        if (errorMessage != null && !errorMessage.isEmpty()) {
            productNameInput.setError(errorMessage);
        }


        cancelButton.setOnClickListener(v -> {
            isSearching = false;
            productNameDialog.dismiss();
            finish();
        });

        searchButton.setOnClickListener(v -> {
            String productName = productNameInput.getText().toString().trim();


            if (productName.isEmpty()) {
                productNameInput.setError("Введите название продукта");
                return;
            }

            if (productName.length() < 3) {
                productNameInput.setError("Название продукта должно содержать минимум 3 символа");
                return;
            }


            if (!productName.matches("[a-zA-Zа-яА-Я0-9\\s\\-,\\.()%\"']+")) {
                productNameInput.setError("Название содержит недопустимые символы");
                return;
            }

            Log.d(TAG, "Отправляем запрос на поиск продукта: '" + productName + "'");
            productNameDialog.dismiss();
            showLoadingDialog();
            searchProductWithGigaChat(productName);
        });


        productNameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchButton.performClick();
                return true;
            }
            return false;
        });

        productNameDialog.show();


        productNameInput.requestFocus();
        productNameInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(productNameInput, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }

    private void searchProductWithGigaChat(String productName) {

        foodManager.findFoodByExactNameAsync(productName, localFood -> {
            if (localFood != null) {
                Log.d(TAG, "Найден локальный продукт с именем '" + productName +
                        "', используем его вместо поиска через GigaChat");
                hideLoadingDialog();
                openPortionSizeActivity(localFood);
                return;
            }


            Log.d(TAG, "Продукт с именем '" + productName +
                    "' не найден в локальной базе, ищем через GigaChat");

            foodManager.searchProductWithGigaChat(productName, new GigaChatProductService.ProductListener() {
                @Override
                public void onProductFound(Food food) {
                    runOnUiThread(() -> {

                        Food localFood = foodManager.findFoodByExactName(food.getName());
                        if (localFood != null) {
                            Log.d(TAG, "Найден локальный продукт с именем '" + food.getName() +
                                    "', используем его вместо данных из GigaChat");


                            Food combinedFood = new Food.Builder()
                                    .id(localFood.getId())
                                    .name(food.getName())
                                    .category(food.getCategory())
                                    .subcategory(food.getSubcategory())
                                    .calories(food.getCalories())
                                    .proteins(food.getProteins())
                                    .fats(food.getFats())
                                    .carbs(food.getCarbs())
                                    .popularity(localFood.getPopularity())
                                    .calcium(food.getCalcium())
                                    .iron(food.getIron())
                                    .magnesium(food.getMagnesium())
                                    .phosphorus(food.getPhosphorus())
                                    .potassium(food.getPotassium())
                                    .sodium(food.getSodium())
                                    .zinc(food.getZinc())
                                    .vitaminA(food.getVitaminA())
                                    .vitaminB1(food.getVitaminB1())
                                    .vitaminB2(food.getVitaminB2())
                                    .vitaminB3(food.getVitaminB3())
                                    .vitaminB5(food.getVitaminB5())
                                    .vitaminB6(food.getVitaminB6())
                                    .vitaminB9(food.getVitaminB9())
                                    .vitaminB12(food.getVitaminB12())
                                    .vitaminC(food.getVitaminC())
                                    .vitaminD(food.getVitaminD())
                                    .vitaminE(food.getVitaminE())
                                    .vitaminK(food.getVitaminK())
                                    .cholesterol(food.getCholesterol())
                                    .saturatedFats(food.getSaturatedFats())
                                    .transFats(food.getTransFats())
                                    .fiber(food.getFiber())
                                    .sugar(food.getSugar())
                                    .usefulness_index(food.getUsefulnessIndex())
                                    .build();

                            hideLoadingDialog();
                            openPortionSizeActivity(combinedFood);
                        } else {

                            Log.d(TAG, "Продукт с именем '" + food.getName() + "' не найден в локальной базе, используем данные из GigaChat");
                            hideLoadingDialog();
                            openPortionSizeActivity(food);
                        }
                    });
                }

                @Override
                public void onProductNotFound() {
                    runOnUiThread(() -> {
                        hideLoadingDialog();

                        openCreateProductActivity(productName, lastScannedBarcode);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        hideLoadingDialog();

                        openCreateProductActivity(productName, lastScannedBarcode);
                    });
                }
            });
        });
    }


    private void openCreateProductActivity(String productName, String barcode) {
        Log.d(TAG, "Открываем CreateProductActivity для продукта: " + productName + ", штрихкод: " + barcode);

        Intent intent = new Intent(this, CreateProductActivity.class);
        intent.putExtra("PRODUCT_NAME", productName);
        if (barcode != null && !barcode.isEmpty()) {
            intent.putExtra("BARCODE", barcode);
        }
        intent.putExtra("FROM_NOT_FOUND", true);

        startActivity(intent);
        finish();
    }

    private void openPortionSizeActivity(Food food) {

        if (isScanOnlyMode) {
            Log.d(TAG, "Режим SCAN_ONLY: возвращаем штрихкод " + lastScannedBarcode);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("BARCODE", lastScannedBarcode);
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        Log.d(TAG, "Открываем PortionSizeActivity с продуктом: " + food.getName());
        Log.d(TAG, "Детали продукта: id=" + food.getId() +
                ", calories=" + food.getCalories() +
                ", proteins=" + food.getProteins() +
                ", fats=" + food.getFats() +
                ", carbs=" + food.getCarbs());


        Intent intent = new Intent(this, PortionSizeActivity.class);


        String mealType = getIntent().getStringExtra(Constants.EXTRA_MEAL_TYPE);
        if (mealType == null) {

            mealType = "breakfast";
            Log.d(TAG, "mealType не был передан, используем значение по умолчанию: " + mealType);
        } else {
            Log.d(TAG, "Получен mealType из Intent: " + mealType);
        }


        intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);


        if (lastScannedBarcode != null && !lastScannedBarcode.isEmpty()) {
            intent.putExtra(Constants.EXTRA_BARCODE, lastScannedBarcode);
            Log.d(TAG, "Передаем штрихкод в PortionSizeActivity: " + lastScannedBarcode);
        }


        intent.putExtra(Constants.EXTRA_FOOD, food);

        Log.d(TAG, "Данные добавлены в Intent: " + intent.getExtras());


        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (lineAnimator != null) {
            lineAnimator.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (lineAnimator != null && isScanning) {
            lineAnimator.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (lineAnimator != null) {
            lineAnimator.cancel();
        }
        cameraExecutor.shutdown();
    }
} 
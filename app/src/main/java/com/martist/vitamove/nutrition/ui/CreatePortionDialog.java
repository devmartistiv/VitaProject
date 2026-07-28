package com.martist.vitamove.nutrition.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.ui.model.Portion;


public class CreatePortionDialog extends Dialog {

    private TextInputLayout tilPortionName;
    private TextInputEditText etPortionName;
    private TextInputLayout tilPortionWeight;
    private TextInputEditText etPortionWeight;
    private TextInputLayout tilPortionUnit;
    private AutoCompleteTextView etPortionUnit;
    private MaterialButton btnCancel;
    private MaterialButton btnCreate;

    private OnPortionCreatedListener listener;

    public CreatePortionDialog(@NonNull Context context, OnPortionCreatedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_portion, null);
        setContentView(view);


        if (getWindow() != null) {
            android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            getWindow().setAttributes(params);
        }

        initViews(view);
        setupUnitDropdown();
        setupButtons();
    }

    private void initViews(View view) {
        tilPortionName = view.findViewById(R.id.til_portion_name);
        etPortionName = view.findViewById(R.id.et_portion_name);
        tilPortionWeight = view.findViewById(R.id.til_portion_weight);
        etPortionWeight = view.findViewById(R.id.et_portion_weight);
        tilPortionUnit = view.findViewById(R.id.til_portion_unit);
        etPortionUnit = view.findViewById(R.id.et_portion_unit);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnCreate = view.findViewById(R.id.btn_create);
    }

    private void setupUnitDropdown() {
        String[] units = {"г", "мл"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                units
        );
        etPortionUnit.setAdapter(adapter);
        etPortionUnit.setText("г", false);
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnCreate.setOnClickListener(v -> {
            if (validateInput()) {
                createPortion();
            }
        });
    }

    private boolean validateInput() {
        clearErrors();
        boolean isValid = true;


        String name = etPortionName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            tilPortionName.setError("Введите название порции");
            isValid = false;
        }


        String weightStr = etPortionWeight.getText().toString().trim();
        if (TextUtils.isEmpty(weightStr)) {
            tilPortionWeight.setError("Введите эквивалент");
            isValid = false;
        } else {
            try {
                float weight = Float.parseFloat(weightStr);
                if (weight <= 0) {
                    tilPortionWeight.setError("Эквивалент должен быть больше 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilPortionWeight.setError("Неверный формат числа");
                isValid = false;
            }
        }


        String unit = etPortionUnit.getText().toString().trim();
        if (TextUtils.isEmpty(unit)) {
            tilPortionUnit.setError("Выберите единицу");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        tilPortionName.setError(null);
        tilPortionWeight.setError(null);
        tilPortionUnit.setError(null);
    }

    private void createPortion() {
        String name = etPortionName.getText().toString().trim();
        String weightStr = etPortionWeight.getText().toString().trim();
        String unit = etPortionUnit.getText().toString().trim();

        int weight = (int) Float.parseFloat(weightStr);


        Portion portion = new Portion();
        portion.setName(name + " (" + weightStr + " " + unit + ")");
        portion.setWeight(weight);

        if (listener != null) {
            listener.onPortionCreated(portion);
        }

        dismiss();
    }


    public interface OnPortionCreatedListener {
        void onPortionCreated(Portion portion);
    }
}

package com.martist.vitamove.set;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExerciseSetAdapter extends RecyclerView.Adapter<ExerciseSetAdapter.SetViewHolder> {
    private static final String TAG = "ExerciseSetAdapter";
    private static final int TYPE_ACTIVE = 1;
    private static final int TYPE_INACTIVE = 2;

    private final List<ExerciseSet> sets = new ArrayList<>();
    private OnSetClickListener listener;
    private OnDeleteClickListener deleteListener;


    public interface OnDataChangeListener {
        void onDataChanged(ExerciseSet set, int position);
    }

    private OnDataChangeListener dataChangeListener;


    public interface OnDeleteClickListener {
        void onDeleteClick(ExerciseSet set, int position);
    }

    public interface OnSetClickListener {
        void onSetClick(ExerciseSet set, int position, boolean isCompleted);
    }

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.dataChangeListener = listener;
    }

    public void setOnSetClickListener(OnSetClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public ExerciseSet getSet(int position) {
        if (position >= 0 && position < sets.size()) {
            return sets.get(position);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < sets.size()) {
            return TYPE_ACTIVE;
        }
        return TYPE_INACTIVE;
    }

    @NonNull
    @Override
    public SetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ACTIVE) {
            View view = inflater.inflate(R.layout.item_exercise_set_editable, parent, false);
            SetViewHolder holder = new ActiveSetViewHolder(view);
            viewHolders.add(holder);
            return holder;
        } else {
            View view = inflater.inflate(R.layout.item_exercise_set_inactive, parent, false);
            SetViewHolder holder = new InactiveSetViewHolder(view);
            viewHolders.add(holder);
            return holder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SetViewHolder holder, int position) {
        if (holder instanceof ActiveSetViewHolder) {
            ((ActiveSetViewHolder) holder).bind(sets.get(position), position);
        } else if (holder instanceof InactiveSetViewHolder) {
            ((InactiveSetViewHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return sets.size() + 1;
    }


    public void updateSets(List<ExerciseSet> newSets) {
        if (newSets == null) {
            Log.e(TAG, "updateSets: список подходов равен null");
            return;
        }

        try {

            final List<ExerciseSet> setsCopy = new ArrayList<>(newSets);


            new Handler(Looper.getMainLooper()).post(() -> {
                try {

                    RecyclerView parent = getRecyclerView();
                    if (parent == null || parent.isComputingLayout()) {

                        Log.d(TAG, "updateSets: отложенное обновление, т.к. RecyclerView вычисляет макет");
                        parent.post(() -> safeUpdateList(setsCopy));
                    } else {

                        safeUpdateList(setsCopy);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "updateSets: ошибка при обновлении в UI потоке: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "updateSets: ошибка при обновлении списка подходов: " + e.getMessage(), e);
        }
    }


    private void safeUpdateList(List<ExerciseSet> newSets) {
        if (newSets == null) {
            Log.e(TAG, "safeUpdateList: newSets равен null");
            return;
        }

        try {

            for (int i = 0; i < sets.size(); i++) {
                if (sets.get(i) == null) {
                    Log.e(TAG, "safeUpdateList: найден null элемент в текущем списке на позиции " + i);
                    sets.set(i, new ExerciseSet());
                }
            }


            for (int i = 0; i < newSets.size(); i++) {
                if (newSets.get(i) == null) {
                    Log.e(TAG, "safeUpdateList: найден null элемент в новом списке на позиции " + i);
                    newSets.set(i, new ExerciseSet());
                }
            }


            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return sets.size();
                }

                @Override
                public int getNewListSize() {
                    return newSets.size();
                }

                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    String oldId = sets.get(oldPos).getId();
                    String newId = newSets.get(newPos).getId();


                    if (oldId == null && newId == null) {
                        return oldPos == newPos;
                    } else if (oldId == null || newId == null) {
                        return false;
                    }

                    return oldId.equals(newId);
                }

                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    ExerciseSet oldSet = sets.get(oldPos);
                    ExerciseSet newSet = newSets.get(newPos);

                    boolean sameCompleted = oldSet.isCompleted() == newSet.isCompleted();


                    boolean sameWeight = (oldSet.getWeight() == null && newSet.getWeight() == null) ||
                            (oldSet.getWeight() != null && newSet.getWeight() != null &&
                                    oldSet.getWeight().equals(newSet.getWeight()));


                    boolean sameReps = (oldSet.getReps() == null && newSet.getReps() == null) ||
                            (oldSet.getReps() != null && newSet.getReps() != null &&
                                    oldSet.getReps().equals(newSet.getReps()));

                    return sameCompleted && sameWeight && sameReps;
                }
            });


            sets.clear();
            sets.addAll(newSets);


            diffResult.dispatchUpdatesTo(this);

            Log.d(TAG, "safeUpdateList: список обновлен, размер: " + sets.size());
        } catch (Exception e) {
            Log.e(TAG, "safeUpdateList: ошибка при безопасном обновлении списка: " + e.getMessage(), e);


            try {
                sets.clear();
                sets.addAll(newSets);
                notifyDataSetChanged();
            } catch (Exception ex) {
                Log.e(TAG, "safeUpdateList: ошибка при резервном обновлении: " + ex.getMessage(), ex);
            }
        }
    }


    private RecyclerView getRecyclerView() {
        RecyclerView.ViewHolder holder = viewHolders.size() > 0 ? viewHolders.get(0) : null;
        return holder != null ? (RecyclerView) holder.itemView.getParent() : null;
    }


    private final List<SetViewHolder> viewHolders = new ArrayList<>();


    public void savePendingChangesIfAny() {
        Log.d(TAG, "savePendingChangesIfAny: Attempting to save pending changes.");
        for (SetViewHolder holder : viewHolders) {
            if (holder instanceof ActiveSetViewHolder) {
                ActiveSetViewHolder activeHolder = (ActiveSetViewHolder) holder;

                if (activeHolder.weightEdit.hasFocus()) {
                    Log.d(TAG, "savePendingChangesIfAny: weightEdit has focus at position " + activeHolder.getAdapterPosition());
                    activeHolder.saveWeightData();
                }

                if (activeHolder.repsEdit.hasFocus()) {
                    Log.d(TAG, "savePendingChangesIfAny: repsEdit has focus at position " + activeHolder.getAdapterPosition());
                    activeHolder.saveRepsData();
                }
            }
        }
    }


    abstract class SetViewHolder extends RecyclerView.ViewHolder {
        public SetViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    class ActiveSetViewHolder extends SetViewHolder {
        private final TextView setNumberText;
        private final EditText weightEdit;
        private final EditText repsEdit;
        private final ImageButton actionButton;

        ActiveSetViewHolder(@NonNull View itemView) {
            super(itemView);
            setNumberText = itemView.findViewById(R.id.set_number);
            weightEdit = itemView.findViewById(R.id.weight_input);
            repsEdit = itemView.findViewById(R.id.reps_input);
            actionButton = itemView.findViewById(R.id.action_button);


            actionButton.setOnClickListener(v -> {
                int currentPosition = getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    ExerciseSet clickedSet = sets.get(currentPosition);


                    if (deleteListener != null) {
                        deleteListener.onDeleteClick(clickedSet, currentPosition);
                    }
                }
            });


            weightEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    saveWeightData();
                }
            });


            weightEdit.setOnEditorActionListener((v, actionId, event) -> {
                int position = getAdapterPosition();

                if (position == sets.size() - 1) {

                    repsEdit.requestFocus();
                    return true;
                }

                return false;
            });


            repsEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    saveRepsData();
                }
            });


            repsEdit.setOnEditorActionListener((v, actionId, event) -> {
                int position = getAdapterPosition();

                if (position == sets.size() - 1) {


                    repsEdit.clearFocus();
                    return true;
                }

                return false;
            });
        }


        private void saveWeightData() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && position < sets.size()) {
                Editable s = weightEdit.getText();
                try {
                    float weight = s.length() > 0 ? Float.parseFloat(s.toString()) : 0f;
                    ExerciseSet currentSet = sets.get(position);
                    if (currentSet != null) {

                        if (currentSet.getWeight() == null || currentSet.getWeight() != weight) {
                            currentSet.setWeight(weight);
                            Log.d(TAG, "Weight data saved: " + weight + " for set at position " + position);
                            if (dataChangeListener != null) {
                                dataChangeListener.onDataChanged(currentSet, position);
                                notifyItemChanged(position);
                                Log.d(TAG, "notifyItemChanged called for position " + position + " after weight change");
                            }
                        } else {
                            Log.d(TAG, "Weight data not changed, no save needed for position " + position);
                        }
                    } else {
                        Log.e(TAG, "Set is null at position " + position + " when trying to save weight data.");
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing weight: " + s, e);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "IndexOutOfBoundsException while getting set at position " + position + " for weight. Sets size: " + sets.size(), e);
                }
            }
        }


        private void saveRepsData() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && position < sets.size()) {
                Editable s = repsEdit.getText();
                try {
                    int reps = s.length() > 0 ? Integer.parseInt(s.toString()) : 0;
                    ExerciseSet currentSet = sets.get(position);
                    if (currentSet != null) {

                        if (currentSet.getReps() != reps) {
                            currentSet.setReps(reps);
                            Log.d(TAG, "Reps data saved: " + reps + " for set at position " + position);
                            if (dataChangeListener != null) {
                                dataChangeListener.onDataChanged(currentSet, position);
                                notifyItemChanged(position);
                                Log.d(TAG, "notifyItemChanged called for position " + position + " after reps change");
                            }
                        } else {
                            Log.d(TAG, "Reps data not changed, no save needed for position " + position);
                        }
                    } else {
                        Log.e(TAG, "Set is null at position " + position + " when trying to save reps data.");
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing reps: " + s, e);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "IndexOutOfBoundsException while getting set at position " + position + " for reps. Sets size: " + sets.size(), e);
                }
            }
        }

        void bind(ExerciseSet set, int position) {
            setNumberText.setText(String.valueOf(position + 1));

            weightEdit.setText(String.format(Locale.getDefault(), "%.1f", set.getWeight()));
            repsEdit.setText(String.valueOf(set.getReps()));


            boolean isCompleted = set.isCompleted();


            Log.d("ExerciseSetAdapter", String.format(
                    "bind: подход %d, isCompleted=%b, устанавливаем иконку %s",
                    position + 1,
                    isCompleted,
                    isCompleted ? "галочка" : "мусорка"
            ));


            actionButton.setImageResource(isCompleted ?
                    R.drawable.ic_check : R.drawable.ic_delete);
            actionButton.setColorFilter(itemView.getContext().getColor(
                    isCompleted ? R.color.green_500 : R.color.gray_500));


            actionButton.setEnabled(!isCompleted);


        }
    }

    class InactiveSetViewHolder extends SetViewHolder {
        private final TextView setNumberText;
        private final TextView weightText;
        private final TextView repsText;
        private final ImageButton actionButton;

        InactiveSetViewHolder(@NonNull View itemView) {
            super(itemView);
            setNumberText = itemView.findViewById(R.id.set_number);
            weightText = itemView.findViewById(R.id.weight_text);
            repsText = itemView.findViewById(R.id.reps_text);
            actionButton = itemView.findViewById(R.id.action_button);


            itemView.setOnClickListener(v -> addNewSet());
        }

        void bind() {

            setNumberText.setText("");
            weightText.setText("0.0");
            repsText.setText("0");

            actionButton.setOnClickListener(v -> addNewSet());
        }


        private void addNewSet() {
            if (listener != null) {

                ExerciseSet newSet = new ExerciseSet();
                newSet.setWeight(0f);
                newSet.setReps(10);
                newSet.setCompleted(false);

                Log.d(TAG, "addNewSet: Создание нового подхода через адаптер, передача в onSetClick");


                listener.onSetClick(newSet, sets.size(), false);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull SetViewHolder holder) {
        viewHolders.remove(holder);
        super.onViewRecycled(holder);
    }
} 
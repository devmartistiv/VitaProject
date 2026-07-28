package com.martist.vitamove.set;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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

public class RepsOnlySetAdapter extends RecyclerView.Adapter<RepsOnlySetAdapter.SetViewHolder> {
    private static final String TAG = "RepsOnlySetAdapter";
    private static final int TYPE_ACTIVE = 1;
    private static final int TYPE_INACTIVE = 2;

    private final List<ExerciseSet> sets = new ArrayList<>();
    private OnSetClickListener listener;
    private OnDeleteClickListener deleteListener;


    public interface OnDataChangeListener {
        void onDataChanged(ExerciseSet set, int position);
    }

    private OnDataChangeListener dataChangeListener;

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.dataChangeListener = listener;
    }

    public interface OnSetClickListener {
        void onSetClick(ExerciseSet set, int position, boolean isCompleted);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(ExerciseSet set, int position);
    }

    public void setOnSetClickListener(OnSetClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
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
            View view = inflater.inflate(R.layout.item_exercise_set_reps_only, parent, false);
            SetViewHolder holder = new ActiveSetViewHolder(view);
            viewHolders.add(holder);
            return holder;
        } else {
            View view = inflater.inflate(R.layout.item_exercise_set_reps_only_inactive, parent, false);
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

                    if (parent != null && parent.isComputingLayout()) {

                        Log.d(TAG, "updateSets: отложенное обновление, т.к. RecyclerView вычисляет макет");
                        parent.post(() -> safeUpdateList(setsCopy));
                    } else {

                        safeUpdateList(setsCopy);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "updateSets: ошибка при обновлении в UI потоке: " + e.getMessage(), e);

                    safeUpdateList(setsCopy);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "updateSets: ошибка при обновлении списка подходов: " + e.getMessage(), e);
        }
    }


    private void safeUpdateList(List<ExerciseSet> newSets) {
        try {

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

                    if (oldId == null || newId == null) {
                        return oldId == newId;
                    }
                    return oldId.equals(newId);
                }

                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    ExerciseSet oldSet = sets.get(oldPos);
                    ExerciseSet newSet = newSets.get(newPos);


                    boolean completedSame = oldSet.isCompleted() == newSet.isCompleted();


                    Integer oldReps = oldSet.getReps();
                    Integer newReps = newSet.getReps();
                    boolean repsSame;

                    if (oldReps == null && newReps == null) {
                        repsSame = true;
                    } else if (oldReps == null || newReps == null) {
                        repsSame = false;
                    } else {
                        repsSame = oldReps.equals(newReps);
                    }

                    return completedSame && repsSame;
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
        try {
            if (viewHolders.isEmpty()) {
                return null;
            }

            for (SetViewHolder holder : viewHolders) {
                if (holder != null && holder.itemView != null && holder.itemView.getParent() instanceof RecyclerView) {
                    return (RecyclerView) holder.itemView.getParent();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getRecyclerView: ошибка при получении RecyclerView: " + e.getMessage(), e);
        }
        return null;
    }


    private final List<SetViewHolder> viewHolders = new ArrayList<>();


    abstract class SetViewHolder extends RecyclerView.ViewHolder {
        public SetViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    class ActiveSetViewHolder extends SetViewHolder {
        private final TextView setNumberText;
        private final EditText repsEdit;
        private final ImageButton actionButton;

        ActiveSetViewHolder(@NonNull View itemView) {
            super(itemView);
            setNumberText = itemView.findViewById(R.id.set_number);
            repsEdit = itemView.findViewById(R.id.reps_input);
            actionButton = itemView.findViewById(R.id.action_button);

            repsEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < sets.size()) {
                        try {
                            int reps = s.length() > 0 ? Integer.parseInt(s.toString()) : 0;
                            sets.get(position).setReps(reps);

                            if (dataChangeListener != null) {
                                dataChangeListener.onDataChanged(sets.get(position), position);
                            }
                        } catch (NumberFormatException e) {
                            repsEdit.setText("0");
                        }
                    }
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

            actionButton.setImageResource(R.drawable.ic_check);
            actionButton.setClickable(true);
            repsEdit.setEnabled(true);
            actionButton.setColorFilter(itemView.getContext().getColor(R.color.green_500));

            actionButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < sets.size()) {
                    ExerciseSet currentSet = sets.get(position);

                    if (deleteListener != null) {
                        deleteListener.onDeleteClick(currentSet, position);
                    }
                }
            });
        }

        void bind(ExerciseSet set, int position) {
            setNumberText.setText(String.valueOf(set.getSetNumber()));

            if (set.getReps() != null) {
                repsEdit.setText(String.valueOf(set.getReps()));
            } else {
                repsEdit.setText("");
            }

            actionButton.setImageResource(set.isCompleted() ? R.drawable.ic_check : R.drawable.ic_delete);

            actionButton.setEnabled(!set.isCompleted());
            repsEdit.setEnabled(true);
            actionButton.setColorFilter(itemView.getContext().getColor(
                    set.isCompleted() ? R.color.green_500 : R.color.gray_500));

            boolean isFirstUncompleted = false;
            for (int i = 0; i < sets.size(); i++) {
                if (!sets.get(i).isCompleted()) {
                    if (i == position) {
                        isFirstUncompleted = true;
                    }
                    break;
                }
            }

            if (isFirstUncompleted && !set.isCompleted()) {
                repsEdit.requestFocus();
            } else {
                repsEdit.clearFocus();
            }
        }
    }

    class InactiveSetViewHolder extends SetViewHolder {
        private final TextView setNumberText;
        private final TextView addSetText;
        private final ImageButton actionButton;

        InactiveSetViewHolder(@NonNull View itemView) {
            super(itemView);
            setNumberText = itemView.findViewById(R.id.set_number);
            addSetText = itemView.findViewById(R.id.add_set_text);
            actionButton = itemView.findViewById(R.id.action_button);

            itemView.setOnClickListener(v -> addNewSet());
            actionButton.setOnClickListener(v -> addNewSet());
        }

        void bind() {

        }

        private void addNewSet() {
            if (listener != null) {
                ExerciseSet newSet = new ExerciseSet();


                if (!sets.isEmpty()) {
                    ExerciseSet firstSet = sets.get(0);
                    if (firstSet != null && firstSet.getExerciseId() != null) {
                        newSet.setExerciseId(firstSet.getExerciseId());
                    }
                }

                listener.onSetClick(newSet, sets.size(), false);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull SetViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof ActiveSetViewHolder) {
            ActiveSetViewHolder activeHolder = (ActiveSetViewHolder) holder;
            activeHolder.repsEdit.removeTextChangedListener(null);
            activeHolder.actionButton.setOnClickListener(null);
        } else if (holder instanceof InactiveSetViewHolder) {
            InactiveSetViewHolder inactiveHolder = (InactiveSetViewHolder) holder;
            inactiveHolder.itemView.setOnClickListener(null);
            inactiveHolder.actionButton.setOnClickListener(null);
        }
    }
} 
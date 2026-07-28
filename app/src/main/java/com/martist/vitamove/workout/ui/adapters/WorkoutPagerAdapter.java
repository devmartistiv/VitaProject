package com.martist.vitamove.workout.ui.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.martist.vitamove.history.HistoryFragment;
import com.martist.vitamove.programs.ui.fragment.ProgramsFragment;
import com.martist.vitamove.workout.ui.fragments.ActiveWorkoutFragment;
import com.martist.vitamove.workout.ui.fragments.WorkoutFragment;

public class WorkoutPagerAdapter extends FragmentStateAdapter {

    public WorkoutPagerAdapter(@NonNull WorkoutFragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HistoryFragment();
            case 1:
                return new ActiveWorkoutFragment();
            case 2:
                return new ProgramsFragment();
            default:
                throw new IllegalStateException("Unexpected position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
} 
package com.martist.vitamove.workout.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.martist.vitamove.R;
import com.martist.vitamove.analytics.AnalyticsFragment;
import com.martist.vitamove.core.ui.views.AnimatedTabLayout;
import com.martist.vitamove.history.HistoryFragment;
import com.martist.vitamove.programs.ui.fragment.ProgramsFragment;
import com.martist.vitamove.workout.domain.WorkoutRepeatEvent;
import com.martist.vitamove.workout.domain.WorkoutStartedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WorkoutFragment extends Fragment {
    private final Fragment[] fragments = new Fragment[]{
            new HistoryFragment(),
            new ActiveWorkoutFragment(),
            new ProgramsFragment()
    };
    private static final String TAG = "WorkoutFragment";
    private AnimatedTabLayout tabLayout;
    private ViewPager2 viewPager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int TAB_WORKOUT = 1;
    private static final int TAB_HISTORY = 0;
    private static final int TAB_PROGRAMS = 2;
    private static final int TAB_COUNT = 3;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
            Log.d(TAG, "Зарегистрирован на EventBus в onCreate");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        tabLayout = view.findViewById(R.id.workout_tab_layout);
        viewPager = view.findViewById(R.id.workout_view_pager);


        setupTopButtons(view);


        setupTabLayout();


        setupViewPager();


        view.post(() -> selectInitialTab());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.background_color));
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_NO) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }

    }

    private void setupViewPager() {

        viewPager.setUserInputEnabled(true);


        viewPager.setAdapter(new FragmentStateAdapter(getChildFragmentManager(), getLifecycle()) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case TAB_HISTORY:
                        return fragments[0];
                    case TAB_WORKOUT:
                        return fragments[1];
                    case TAB_PROGRAMS:
                        return fragments[2];
                    default:
                        throw new IllegalArgumentException("Invalid tab position");
                }
            }

            @Override
            public int getItemCount() {
                return TAB_COUNT;
            }
        });
        viewPager.setCurrentItem(1, false);


        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case TAB_HISTORY:
                    tab.setText("ИСТОРИЯ");
                    break;
                case TAB_WORKOUT:
                    tab.setText("ТРЕНИРОВКА");
                    break;
                case TAB_PROGRAMS:
                    tab.setText("ПРОГРАММА");
                    break;
            }
        }).attach();


        ViewGroup tabStrip = (ViewGroup) tabLayout.getChildAt(0);
        if (tabStrip != null) {
            for (int i = 0; i < tabStrip.getChildCount(); i++) {
                View tabView = tabStrip.getChildAt(i);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) tabView.getLayoutParams();
                params.setMarginEnd(0);
                params.setMarginStart(0);
                params.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
                tabView.requestLayout();


                tabView.setBackground(null);
            }
        }
    }

    private void setupTabLayout() {

        tabLayout.setTabRippleColor(null);


        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }


    private void selectWorkoutTab() {
        viewPager.setCurrentItem(TAB_WORKOUT, false);
    }


    private void selectInitialTab() {
        if (viewPager != null) {

            int tabIndex = TAB_WORKOUT;


            SharedPreferences prefs = requireActivity().getSharedPreferences("VitaMovePrefs", 0);
            int savedTabIndex = prefs.getInt("workout_tab_index", -1);


            if (savedTabIndex >= 0 && savedTabIndex < TAB_COUNT) {
                tabIndex = savedTabIndex;

                prefs.edit().remove("workout_tab_index").apply();
            }


            viewPager.setCurrentItem(tabIndex, false);

            Log.d(TAG, "Установлена начальная вкладка с индексом " + tabIndex);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWorkoutStartedEvent(WorkoutStartedEvent event) {
        Log.d(TAG, "Получено событие WorkoutStartedEvent, переключаемся на вкладку активной тренировки");
        selectWorkoutTab();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWorkoutRepeatEvent(WorkoutRepeatEvent event) {
        Log.d(TAG, "Получено событие WorkoutRepeatEvent, переключаемся на вкладку активной тренировки");
        selectWorkoutTab();
    }


    private void setupTopButtons(View view) {
        View analyticsButton = view.findViewById(R.id.analytics_button);
        analyticsButton.setOnClickListener(v -> {
            animateButtonClick(v);
            openWorkoutAnalytics();
        });

        View settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            animateButtonClick(v);
            openWorkoutSettings();
        });
    }


    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start())
                .start();
    }


    private void openWorkoutAnalytics() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AnalyticsFragment())
                .addToBackStack(null)
                .commit();
    }


    private void openWorkoutSettings() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new WorkoutSettingsFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }


        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
            Log.d(TAG, "Отписан от EventBus в onDestroy");
        }
    }
}

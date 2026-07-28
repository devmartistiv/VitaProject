package com.martist.vitamove.auth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;


public class HeightRulerAdapter extends RecyclerView.Adapter<HeightRulerAdapter.RulerViewHolder> {

    private static final int MIN_HEIGHT_CM = 120;
    private static final int MAX_HEIGHT_CM = 270;

    private final Context context;
    private final HeightSelectedListener listener;


    public interface HeightSelectedListener {
        void onHeightSelected(int heightValue, boolean isMetric);
    }


    public HeightRulerAdapter(Context context, boolean isMetric, HeightSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RulerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ruler_mark, parent, false);
        return new RulerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RulerViewHolder holder, int position) {
        setupMetricRuler(holder, position);
    }


    private void setupMetricRuler(RulerViewHolder holder, int position) {
        int heightValue = position + MIN_HEIGHT_CM;


        if (heightValue % 10 == 0) {
            holder.line.setLayoutParams(new LinearLayout.LayoutParams(
                    2,
                    dpToPx(context, 35)
            ));
            holder.line.setBackgroundColor(Color.BLACK);
            holder.valueText.setVisibility(View.VISIBLE);
            holder.valueText.setText(String.valueOf(heightValue));
            holder.valueText.setTypeface(null, Typeface.BOLD);
        } else if (heightValue % 5 == 0) {
            holder.line.setLayoutParams(new LinearLayout.LayoutParams(
                    1,
                    dpToPx(context, 25)
            ));
            holder.line.setBackgroundColor(ContextCompat.getColor(context, R.color.gray_700));
            holder.valueText.setVisibility(View.VISIBLE);
            holder.valueText.setText(String.valueOf(heightValue));
            holder.valueText.setTextSize(10);
        } else {
            holder.line.setLayoutParams(new LinearLayout.LayoutParams(
                    1,
                    dpToPx(context, 15)
            ));
            holder.line.setBackgroundColor(ContextCompat.getColor(context, R.color.gray_500));
            holder.valueText.setVisibility(View.INVISIBLE);
        }
    }


    private int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    @Override
    public int getItemCount() {
        return MAX_HEIGHT_CM - MIN_HEIGHT_CM + 1;
    }

    static class RulerViewHolder extends RecyclerView.ViewHolder {
        View line;
        TextView valueText;

        public RulerViewHolder(@NonNull View itemView) {
            super(itemView);
            line = itemView.findViewById(R.id.rulerLine);
            valueText = itemView.findViewById(R.id.rulerValue);
        }
    }
} 
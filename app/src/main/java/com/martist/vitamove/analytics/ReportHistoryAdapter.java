package com.martist.vitamove.analytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.report.ReportSummary;

import java.util.ArrayList;
import java.util.List;

public class ReportHistoryAdapter extends RecyclerView.Adapter<ReportHistoryAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(ReportSummary report);
    }

    private final List<ReportSummary> reports = new ArrayList<>();
    private final OnReportClickListener clickListener;

    public ReportHistoryAdapter(OnReportClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setReports(List<ReportSummary> items) {
        reports.clear();
        if (items != null) {
            reports.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void addReport(ReportSummary report) {
        reports.add(0, report);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_history, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ReportSummary report = reports.get(position);
        holder.title.setText(report.getTitle());
        holder.subtitle.setText(report.getSubtitle());
        holder.itemView.setOnClickListener(v -> clickListener.onReportClick(report));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.reportTitle);
            subtitle = itemView.findViewById(R.id.reportSubtitle);
        }
    }
}

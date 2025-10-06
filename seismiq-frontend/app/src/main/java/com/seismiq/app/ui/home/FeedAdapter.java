package com.seismiq.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.seismiq.app.R;
import com.seismiq.app.model.Earthquake;
import com.seismiq.app.model.Report;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<FeedItem> feedItems = new ArrayList<>();

    public void setItems(List<FeedItem> items) {
        feedItems.clear();
        feedItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return feedItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == FeedItem.TYPE_REPORT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report_card, parent, false);
            return new ReportViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_earthquake_card, parent, false);
            return new EarthquakeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FeedItem item = feedItems.get(position);
        
        if (holder instanceof ReportViewHolder) {
            ((ReportViewHolder) holder).bind(item.getReport());
        } else if (holder instanceof EarthquakeViewHolder) {
            ((EarthquakeViewHolder) holder).bind(item.getEarthquake());
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    // ViewHolder for Reports
    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private final Chip chipCategory;
        private final TextView tvDescription;
        private final TextView tvLocation;
        private final TextView tvUserName;
        private final TextView tvTimestamp;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        public void bind(Report report) {
            // Set category with color
            String category = mapCategoryToDisplay(report.getCategoryType());
            chipCategory.setText(category);
            chipCategory.setChipBackgroundColorResource(getCategoryColor(report.getCategoryType()));

            // Set description
            tvDescription.setText(report.getDescription() != null ? report.getDescription() : "No description");

            // Set location
            if (report.getLocation() != null && !report.getLocation().isEmpty()) {
                tvLocation.setText(report.getLocation());
            } else {
                tvLocation.setText(String.format(Locale.US, "%.4f, %.4f", 
                        report.getLatitude(), report.getLongitude()));
            }

            // Set user name
            if (report.getUser() != null && report.getUser().getName() != null) {
                tvUserName.setText("Posted by " + report.getUser().getName());
            } else {
                tvUserName.setText("Posted by User");
            }

            // Set relative timestamp
            if (report.getCreatedAt() != null) {
                tvTimestamp.setText(getRelativeTime(report.getCreatedAt()));
            } else {
                tvTimestamp.setText("Just now");
            }
        }

        private String mapCategoryToDisplay(String category) {
            if (category == null) return "Other";
            return switch (category.toUpperCase()) {
                case "RESCUE" -> "🚨 Rescue";
                case "MEDICAL_HELP" -> "🏥 Medical";
                case "SHELTER" -> "🏠 Shelter";
                case "FOOD_WATER" -> "🍲 Food & Water";
                case "INFRASTRUCTURE" -> "🏗️ Infrastructure";
                default -> category;
            };
        }

        private int getCategoryColor(String category) {
            if (category == null) return android.R.color.darker_gray;
            return switch (category.toUpperCase()) {
                case "RESCUE" -> android.R.color.holo_red_dark;
                case "MEDICAL_HELP" -> android.R.color.holo_red_light;
                case "SHELTER" -> android.R.color.holo_blue_dark;
                case "FOOD_WATER" -> android.R.color.holo_orange_dark;
                case "INFRASTRUCTURE" -> android.R.color.holo_purple;
                default -> android.R.color.darker_gray;
            };
        }
    }

    // ViewHolder for Earthquakes
    static class EarthquakeViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMagnitude;
        private final TextView tvEarthquakeLocation;
        private final TextView tvEarthquakeTime;
        private final TextView tvDepth;

        public EarthquakeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMagnitude = itemView.findViewById(R.id.tvMagnitude);
            tvEarthquakeLocation = itemView.findViewById(R.id.tvEarthquakeLocation);
            tvEarthquakeTime = itemView.findViewById(R.id.tvEarthquakeTime);
            tvDepth = itemView.findViewById(R.id.tvDepth);
        }

        public void bind(Earthquake earthquake) {
            // Set magnitude
            tvMagnitude.setText(String.format(Locale.US, "%.1f", earthquake.getMagnitude()));

            // Set location
            tvEarthquakeLocation.setText(earthquake.getLocation() != null ? 
                    earthquake.getLocation() : "Unknown Location");

            // Set relative time
            if (earthquake.getTimestamp() != null) {
                tvEarthquakeTime.setText(getRelativeTime(earthquake.getTimestamp()));
            } else {
                tvEarthquakeTime.setText("Unknown time");
            }

            // Set depth
            if (earthquake.getDepth() > 0) {
                tvDepth.setText(String.format(Locale.US, "Depth: %.1f km", earthquake.getDepth()));
            } else {
                tvDepth.setText("Depth: Unknown");
            }
        }
    }

    private static String getRelativeTime(Date timestamp) {
        try {
            Date now = new Date();
            long diffInMillis = now.getTime() - timestamp.getTime();
            long seconds = diffInMillis / 1000;

            if (seconds < 60) {
                return "Just now";
            } else if (seconds < 3600) {
                long minutes = seconds / 60;
                return minutes + "m ago";
            } else if (seconds < 86400) {
                long hours = seconds / 3600;
                return hours + "h ago";
            } else {
                long days = seconds / 86400;
                return days + "d ago";
            }
        } catch (Exception e) {
            return "Recently";
        }
    }
}


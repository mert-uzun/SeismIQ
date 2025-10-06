package com.seismiq.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.seismiq.app.R;
import com.seismiq.app.model.MyPost;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyPostsAdapter extends RecyclerView.Adapter<MyPostsAdapter.MyPostViewHolder> {

    private final List<MyPost> posts = new ArrayList<>();
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(MyPost post);
    }

    public void setDeleteListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public void setPosts(List<MyPost> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    public void removePost(MyPost post) {
        int position = posts.indexOf(post);
        if (position != -1) {
            posts.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public MyPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_post_card, parent, false);
        return new MyPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyPostViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class MyPostViewHolder extends RecyclerView.ViewHolder {
        private final Chip chipCategory;
        private final TextView tvTimestamp;
        private final TextView tvType;
        private final TextView tvDescription;
        private final TextView tvLocation;
        private final ImageButton btnDelete;

        public MyPostViewHolder(@NonNull View itemView) {
            super(itemView);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvType = itemView.findViewById(R.id.tvType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(MyPost post) {
            // Set category
            String category = mapCategoryToDisplay(post.getCategoryType());
            chipCategory.setText(category);
            chipCategory.setChipBackgroundColorResource(getCategoryColor(post.getCategoryType()));

            // Set type
            tvType.setText(post.getTypeString());

            // Set description
            tvDescription.setText(post.getDescription() != null ? post.getDescription() : "No description");

            // Set location
            if (post.getLocation() != null && !post.getLocation().isEmpty()) {
                tvLocation.setText(post.getLocation());
            } else {
                tvLocation.setText(String.format(Locale.US, "%.4f, %.4f", 
                        post.getLatitude(), post.getLongitude()));
            }

            // Set relative timestamp
            if (post.getCreatedAt() != null) {
                tvTimestamp.setText(getRelativeTime(post.getCreatedAt()));
            } else {
                tvTimestamp.setText("Just now");
            }

            // Set delete button click listener
            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(post);
                }
            });
        }

        private String mapCategoryToDisplay(String category) {
            if (category == null) return "Other";
            return switch (category.toUpperCase()) {
                case "RESCUE" -> "🚨 Rescue";
                case "MEDICAL_HELP", "MEDICAL_STATION" -> "🏥 Medical";
                case "SHELTER" -> "🏠 Shelter";
                case "FOOD_WATER", "FOOD_DISTRIBUTION" -> "🍲 Food & Water";
                case "INFRASTRUCTURE" -> "🏗️ Infrastructure";
                default -> category;
            };
        }

        private int getCategoryColor(String category) {
            if (category == null) return android.R.color.darker_gray;
            return switch (category.toUpperCase()) {
                case "RESCUE" -> android.R.color.holo_red_dark;
                case "MEDICAL_HELP", "MEDICAL_STATION" -> android.R.color.holo_blue_dark;
                case "SHELTER" -> android.R.color.holo_green_dark;
                case "FOOD_WATER", "FOOD_DISTRIBUTION" -> android.R.color.holo_orange_dark;
                case "INFRASTRUCTURE" -> android.R.color.holo_purple;
                default -> android.R.color.darker_gray;
            };
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


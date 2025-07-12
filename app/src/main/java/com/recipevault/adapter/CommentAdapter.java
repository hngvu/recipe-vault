package com.recipevault.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.recipevault.R;
import com.recipevault.model.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private String currentUserId;
    private OnLikeClickListener likeClickListener;

    public interface OnLikeClickListener {
        void onLikeClick(Comment comment, int position, boolean liked);
    }

    public CommentAdapter() {
        this.comments = new ArrayList<>();
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public void setOnLikeClickListener(OnLikeClickListener listener) {
        this.likeClickListener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        this.comments.add(0, comment); // Add to the beginning
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivUserAvatar, ivLike;
        private TextView tvUserName, tvCommentDate, tvCommentText, tvCommentTime, tvLikeCount;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvCommentDate = itemView.findViewById(R.id.tv_comment_date);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvCommentTime = itemView.findViewById(R.id.tv_comment_time);
            ivLike = itemView.findViewById(R.id.iv_like);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
        }

        public void bind(Comment comment) {
            tvUserName.setText(comment.getUsername());
            tvCommentText.setText(comment.getText());
            // Date (absolute)
            String formattedDate = android.text.format.DateFormat.format("MMM dd, yyyy", new Date(comment.getTimestamp())).toString();
            tvCommentDate.setText(formattedDate);
            // Time (relative)
            CharSequence relTime = DateUtils.getRelativeTimeSpanString(comment.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            tvCommentTime.setText(relTime);
            // Avatar
            if (comment.getUserAvatarUrl() != null && !comment.getUserAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(comment.getUserAvatarUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivUserAvatar);
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_person);
            }
            // Like count
            tvLikeCount.setText(String.valueOf(comment.getLikeCount()));
            // Like icon state
            boolean liked = comment.getLikedUserIds() != null && currentUserId != null && comment.getLikedUserIds().contains(currentUserId);
            ivLike.setImageResource(liked ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
            // Like click
            ivLike.setOnClickListener(v -> {
                if (likeClickListener != null) {
                    likeClickListener.onLikeClick(comment, getAdapterPosition(), !liked);
                }
            });
        }
    }
} 
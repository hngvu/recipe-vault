package com.recipevault.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.recipevault.R;
import com.recipevault.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class FavoriteRecipeAdapter extends RecyclerView.Adapter<FavoriteRecipeAdapter.FavoriteViewHolder> {

    private List<Recipe> favoriteRecipes;
    private OnFavoriteRecipeClickListener listener;
    private OnFavoriteToggleListener favoriteToggleListener;

    public interface OnFavoriteRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    public interface OnFavoriteToggleListener {
        void onFavoriteToggle(Recipe recipe, boolean isFavorite);
    }

    public FavoriteRecipeAdapter() {
        this.favoriteRecipes = new ArrayList<>();
    }

    public void setOnFavoriteRecipeClickListener(OnFavoriteRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteToggleListener(OnFavoriteToggleListener listener) {
        this.favoriteToggleListener = listener;
    }

    public void setFavoriteRecipes(List<Recipe> recipes) {
        this.favoriteRecipes = recipes != null ? recipes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeRecipe(Recipe recipe) {
        int position = favoriteRecipes.indexOf(recipe);
        if (position != -1) {
            favoriteRecipes.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_recipe, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Recipe recipe = favoriteRecipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        return favoriteRecipes.size();
    }

    class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private ImageView ivRecipeImage;
        private ImageView ivFavoriteHeart;
        private TextView tvTitle;
        private TextView tvDescription;
        private TextView tvCookingTime;
        private TextView tvDifficulty;
        private TextView tvServings;
        private TextView tvAuthor;
        private TextView tvRating;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image);
            ivFavoriteHeart = itemView.findViewById(R.id.iv_favorite_heart);
            tvTitle = itemView.findViewById(R.id.tv_recipe_title);
            tvDescription = itemView.findViewById(R.id.tv_recipe_description);
            tvCookingTime = itemView.findViewById(R.id.tv_cooking_time);
            tvDifficulty = itemView.findViewById(R.id.tv_difficulty);
            tvServings = itemView.findViewById(R.id.tv_servings);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvRating = itemView.findViewById(R.id.tv_rating);

            // Set click listeners
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onRecipeClick(favoriteRecipes.get(position));
                    }
                }
            });

            ivFavoriteHeart.setOnClickListener(v -> {
                if (favoriteToggleListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Recipe recipe = favoriteRecipes.get(position);
                        favoriteToggleListener.onFavoriteToggle(recipe, false); // Remove from favorites
                        removeRecipe(recipe);
                    }
                }
            });
        }

        public void bind(Recipe recipe) {
            tvTitle.setText(recipe.getTitle());
            tvDescription.setText(recipe.getDescription());
            tvCookingTime.setText(recipe.getCookingTime());
            tvDifficulty.setText(recipe.getDifficulty());
            tvServings.setText(recipe.getServings() + " servings");
            tvAuthor.setText("By " + recipe.getAuthorName());
            tvRating.setText(String.format("%.1f", recipe.getRating()));

            // Set favorite heart as filled (since these are favorite recipes)
            ivFavoriteHeart.setImageResource(R.drawable.ic_favorite_filled);

            // Load recipe image using Glide
            if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(recipe.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.placeholder_recipe)
                        .into(ivRecipeImage);
            } else {
                ivRecipeImage.setImageResource(R.drawable.placeholder_recipe);
            }
        }
    }
}
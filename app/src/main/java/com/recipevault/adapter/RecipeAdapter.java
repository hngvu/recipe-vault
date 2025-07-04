package com.recipevault.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.recipevault.R;
import com.recipevault.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    public RecipeAdapter() {
        this.recipes = new ArrayList<>();
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivRecipeImage;
        private TextView tvTitle;
        private TextView tvDescription;
        private TextView tvCookingTime;
        private TextView tvDifficulty;
        private TextView tvServings;
        private TextView tvAuthor;
        private TextView tvRating;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image);
            tvTitle = itemView.findViewById(R.id.tv_recipe_title);
            tvDescription = itemView.findViewById(R.id.tv_recipe_description);
            tvCookingTime = itemView.findViewById(R.id.tv_cooking_time);
            tvDifficulty = itemView.findViewById(R.id.tv_difficulty);
            tvServings = itemView.findViewById(R.id.tv_servings);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvRating = itemView.findViewById(R.id.tv_rating);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onRecipeClick(recipes.get(position));
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

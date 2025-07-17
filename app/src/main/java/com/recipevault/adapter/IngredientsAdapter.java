package com.recipevault.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.recipevault.R;
import com.recipevault.model.IngredientInput;

import java.util.ArrayList;
import java.util.List;

public class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder> {

    private List<IngredientInput> ingredients;

    public IngredientsAdapter() {
        this.ingredients = new ArrayList<>();
    }

    public void setIngredients(List<IngredientInput> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addIngredient(IngredientInput ingredient) {
        ingredients.add(ingredient);
        notifyItemInserted(ingredients.size() - 1);
    }

    public void removeIngredient(int position) {
        if (position >= 0 && position < ingredients.size()) {
            ingredients.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<IngredientInput> getIngredients() {
        return ingredients;
    }

    public IngredientInput getIngredientAt(int position) {
        if (ingredients != null && position >= 0 && position < ingredients.size()) {
            return ingredients.get(position);
        }
        return new IngredientInput("", "");
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        IngredientInput ingredient = ingredients.get(position);
        holder.tvStepNumber.setText(String.valueOf(position + 1));
        holder.etName.setText(ingredient.getName());
        holder.etAmount.setText(ingredient.getAmount());
        holder.btnRemove.setOnClickListener(v -> {
            removeIngredient(holder.getAdapterPosition());
            // Update step numbers for remaining ingredients
            notifyItemRangeChanged(holder.getAdapterPosition(), ingredients.size());
        });
        holder.etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) ingredient.setName(holder.etName.getText().toString());
        });
        holder.etAmount.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) ingredient.setAmount(holder.etAmount.getText().toString());
        });
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        TextView tvStepNumber;
        TextInputEditText etName, etAmount;
        ImageButton btnRemove;
        IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepNumber = itemView.findViewById(R.id.tv_step_number);
            etName = itemView.findViewById(R.id.et_ingredient_name);
            etAmount = itemView.findViewById(R.id.et_ingredient_amount);
            btnRemove = itemView.findViewById(R.id.btn_remove_ingredient);
        }
    }
}

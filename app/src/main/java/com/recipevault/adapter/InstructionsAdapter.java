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
import com.recipevault.model.InstructionInput;

import java.util.ArrayList;
import java.util.List;

public class InstructionsAdapter extends RecyclerView.Adapter<InstructionsAdapter.InstructionViewHolder> {

    private List<InstructionInput> instructions;

    public InstructionsAdapter() {
        this.instructions = new ArrayList<>();
    }

    public void setInstructions(List<InstructionInput> instructions) {
        this.instructions = instructions != null ? instructions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addInstruction(InstructionInput instruction) {
        instructions.add(instruction);
        notifyItemInserted(instructions.size() - 1);
    }

    public void removeInstruction(int position) {
        if (position >= 0 && position < instructions.size()) {
            instructions.remove(position);
            notifyItemRemoved(position);
            // Update step numbers for remaining instructions
            notifyItemRangeChanged(position, instructions.size());
        }
    }

    public List<InstructionInput> getInstructions() {
        return instructions;
    }

    @NonNull
    @Override
    public InstructionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_instruction, parent, false);
        return new InstructionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstructionViewHolder holder, int position) {
        InstructionInput instruction = instructions.get(position);
        holder.tvStepNumber.setText(String.valueOf(position + 1));
        holder.etInstructionText.setText(instruction.getText());
        holder.btnRemove.setOnClickListener(v -> removeInstruction(holder.getAdapterPosition()));
        holder.etInstructionText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) instruction.setText(holder.etInstructionText.getText().toString());
        });
    }

    @Override
    public int getItemCount() {
        return instructions.size();
    }

    static class InstructionViewHolder extends RecyclerView.ViewHolder {
        TextView tvStepNumber;
        TextInputEditText etInstructionText;
        ImageButton btnRemove;

        InstructionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepNumber = itemView.findViewById(R.id.tv_step_number);
            etInstructionText = itemView.findViewById(R.id.et_instruction_text);
            btnRemove = itemView.findViewById(R.id.btn_remove_instruction);
        }
    }
}

package com.recipevault.model;

public class InstructionInput {
    private String text;

    public InstructionInput() {}

    public InstructionInput(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}


package com.example.cosmod.codex;

public class CodexPixels {
    public static int[] get(String id) {
        int[] r = CodexPixelsFood.get(id);
        if (r != null) return r;
        return CodexPixelsMineral.get(id);
    }
}

package com.kheera.internal;

import android.util.Log;

import java.util.Stack;

/**
 * Created by andrewc on 16/2/17.
 */
public class Benchmarker {
    private static Stack<BenchmarkEntry> entries = new Stack<BenchmarkEntry>();
    public static void start(String name) {
        entries.push(new BenchmarkEntry(name));
    }

    public static void stop() {
        BenchmarkEntry s = entries.pop();

        long duration = System.nanoTime() - s.start;
        Log.d("Benchmarker", s.name + " - took: " + (duration/1000) + " ms.");
    }

    private static class BenchmarkEntry {
        private final String name;
        private final long start;

        public BenchmarkEntry(String name) {
            this.name = name;
            this.start = System.nanoTime();
        }
    }
}

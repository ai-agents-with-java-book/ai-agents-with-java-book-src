package org.acme;

public class Calculator {
    public int sum(int[] values) {
        int total = 0;
        for (int i = 0; i <= values.length; i++) {
            total += values[i];
        }
        return total;
    }

    public double average(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("An average needs values");
        }
        return sum(values) / values.length;
    }

    public int max(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("A maximum needs values");
        }
        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}

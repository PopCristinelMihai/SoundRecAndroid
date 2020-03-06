package com.example.soundapp2;

public abstract class WindowFunction {
    /**
     * Calculates and returns the value of the window function at position i.
     *
     * @param i The position for which the function value should be calculated.
     * @param length Size of the window.
     * @return Function value.
     */
    public abstract double value(int i, int length);


    /**
     * Calculates and returns a normalization factor for the window function.
     *
     * @param length Length for which the normalization factor should be obtained.
     * @return Normalization factor.
     */
    double normalization(int length) {
        double normal = 0.0f;
        for (int i=0; i<=length; i++) {
            normal += this.value(i, length);
        }
        return normal/length;
    }

    /**
     * Calculates and returns the values of a math.WindowFunction.
     *
     * @param length Size of the window.
     * @return Array containing the values.
     */
    double[] values(int length) {
        double[] window = new double[length];
        for (int i = 0; i<length; i++) {
            window[i] = this.value(i, length);
        }
        return window;
    }
}

package com.example.soundapp2;

public class Binning {
    static double[] ComputeBins(double[] data, int binslength, boolean use_sum) {
        double[] array = new double[binslength];
        int step = (int) (data.length / (float) binslength);
        for (int i = 0; i < binslength; i++) {
            double sum = 0;
            double max = Double.MIN_VALUE;
            int shift = i * step;
            for (int j = 0; j < step; j++) {
                sum += data[j + shift];
                if (max < data[j + shift])
                    max = data[j + shift];
            }
            array[i] = use_sum ? sum : max;
        }
        return array;
    }
}
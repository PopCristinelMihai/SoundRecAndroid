package com.example.soundapp2;

import com.example.soundapp2.Spectrum;
import com.example.soundapp2.WindowFunction;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

class Pair<K, V> {

    public K first;
    public V second;

    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

}

class RectangularWindow extends WindowFunction {
    /**
     * Returns the value of the windowing function at position i.
     *
     * @param i      The position for which the function value should be calculated.
     * @param length Size of the window.
     * @return Function value.
     */
    @Override
    public final double value(int i, int length) {
        if (i >= 0 && i <= length - 1) {
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public final double normalization(int length) {
        return 1.0;
    }


    /**
     * Returns a normalization factor for the window function.
     *
     * @param length Length for which the normalization factor should be obtained.
     * @return Normalization factor.
     */

}

class FFTUtil {

    private FFTUtil() {
    }

    /**
     * Returns frequency labels in Hz for a FFT of the specified size and samplingrate.
     *
     * @param size         Size of the FFT (i.e. number of frequency bins).
     * @param samplingrate Rate at which the original data has been sampled.
     * @return Array containing the frequency labels in Hz in ascending order.
     */
    public static float[] binCenterFrequencies(int size, float samplingrate) {
        float[] labels = new float[size / 2];
        for (int bin = 0; bin < labels.length; bin++) {
            labels[bin] = FFTUtil.binCenterFrequency(bin, size, samplingrate);
        }
        return labels;
    }

    /**
     * Returns the center frequency associated with the provided bin-index for the given
     * window-size and samplingrate.
     *
     * @param index        Index of the bin in question.
     * @param size         Size of the FFT (i.e. number of frequency bins).
     * @param samplingrate Rate at which the original data has been sampled.
     */
    public static float binCenterFrequency(int index, int size, float samplingrate) {
        if (index > size) {
            throw new IllegalArgumentException("The index cannot be greater than the window-size of the FFT.");
        }
        double bin_width = (samplingrate / size);
        double offset = bin_width / 2.0;
        return (float) ((index * bin_width) + offset);
    }

    /**
     * Returns the bin-index associated with the provided frequency at the given samplingrate
     * and window-size.
     *
     * @param frequency
     * @param size         Size of the FFT (i.e. number of frequency bins).
     * @param samplingrate Rate at which the original data has been sampled.
     * @return
     */
    public static int binIndex(float frequency, int size, float samplingrate) {
        if (frequency > samplingrate / 2) {
            throw new IllegalArgumentException("The frequency cannot be greater than half the samplingrate.");
        }
        double bin_width = (samplingrate / size);
        return (int) Math.floor(frequency / bin_width);
    }


    /**
     * Returns time labels in seconds for a STFT of given width using the provided
     * window size and samplerate.
     *
     * @param width        Size of the STFT (i.e. number of time bins).
     * @param windowsize   Used for FFT (i.e. number of samples per time bin)
     * @param overlap      Overlap in samples between two adjacent windows during the FFT.
     * @param padding      Zeropadding, i.e. how many zeros have been added before and after the actual sample starts
     *                     (Assumption: padding happens within the fixed windowsize)
     * @param samplingrate Rate at which the original data has been sampled.
     * @return Array containing the time labels for the STFT in seconds in ascending order.
     */
    public static float[] time(int width, int windowsize, int overlap, int padding, float samplingrate) {
        float[] labels = new float[width];
        float stepsize = FFTUtil.timeStepsize(windowsize, overlap, padding, samplingrate);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = i * stepsize;
        }
        return labels;
    }


    /**
     * Returns the width in seconds of a single FFT in an STFT i.e. how many seconds one
     * progresses in the original signal when moving to the next FFT.
     *
     * @param windowsize   Windowsize used for the FFT.
     * @param overlap      Overlap in samples between two adjacent windows during the FFT.
     * @param padding      Zeropadding, i.e. how many zeros have been added before and after the actual sample starts
     *                     (Assumption: padding happens within the fixed windowsize)
     * @param samplingrate Rate at which the original signal has been sampled.
     * @return Time step-size in seconds.
     */
    public static float timeStepsize(int windowsize, int overlap, int padding, float samplingrate) {
        return ((windowsize - overlap - 2 * padding) / samplingrate);
    }

    /**
     * Method that can be used to find the closest power of two value greater than or equal to the provided value.
     * Used to introduce a zero-padding on input-data whose length is not equal to 2^n.
     *
     * @param number Value for which a power of two must be found.
     * @return Next value which is greater than or equal to the provided number and a power of two.
     */
    public static int nextPowerOf2(int number) {
        return (int) Math.pow(2, Math.ceil(Math.log(number) / Math.log(2)));
    }


    /**
     * Calculates and returns the windowsize in samples and the zero-padding in samples so as to achieve
     * a certain window-duration in seconds.
     *
     * @param samplingrate   The samplingrate of the original data.
     * @param windowduration Duration of the window in seconds.
     * @return math.Pair of integers; first integer determines the windowsize and the second determines the padding.
     */
    public static Pair<Integer, Integer> parametersForDuration(float samplingrate, float windowduration) {
        int samples = (int) (samplingrate * windowduration);
        int windowsize = nextPowerOf2(samples);
        return new Pair<>(windowsize, (windowsize - samples) / 2);
    }

    /**
     * Checks if the provided number is a power of two and return true if so and false otherwise.
     *
     * @param number Number to check.
     * @return true if number is a power of two, false otherwise.
     */
    public static boolean isPowerOf2(int number) {
        double value = Math.log(number) / Math.log(2);
        return Math.ceil(value) == value;
    }
}


public class ApacheFFT {


    /**
     * This class wraps the Apache Commons FastFourierTransformer and extends it with some additional functionality.
     *
     * <ol>
     *     <li>It allows to apply WindowFunctions for forward-transformation. See math.WindowFunction interface!</li>
     *     <li>It provides access to some important derivatives of the FFT, like the power-spectrum. </li>
     *     <li>All derivatives are calculated in a lazy way i.e. the values are on access.</li>
     * </ol>
     *
     * The same instance of the FFT class can be re-used to process multiple samples. Every call to forward() will replace
     * all the existing data in the instance.
     *
     * The inspiration for this class comes from the FFT class found in the jAudio framework (see
     * https://github.com/dmcennis/jaudioGIT)
     *
     * @see WindowFunction
     *
     * @author rgasser
     * @version 1.0
     * @created 02.02.17
     */

    /**
     * Data obtained by forward FFT.
     */
    private Complex[] data;

    /**
     * Magnitude spectrum of the FFT data. May be null if it has not been obtained yet.
     */
    private Spectrum magnitudeSpectrum;

    /**
     * Power spectrum of the FFT data. May be null if it has not been obtained yet.
     */
    private Spectrum powerSpectrum;

    /**
     * math.WindowFunction to apply before forward transformation. Defaults to IdentityWindows (= no window).
     */
    private WindowFunction windowFunction = new RectangularWindow();

    /**
     * Samplingrate of the last chunk of data that was processed by FFT.
     */
    private float samplingrate;

    /**
     * Performs a forward fourier transformation on the provided, real valued data. The method makes sure,
     * that the size of the array is a power of two (for which the FFT class has been optimized) and pads
     * the data with zeros if necessary. Furthermore, one can provide a WindowingFunction that will be applied
     * on the data.
     *
     * <strong>Important: </strong>Every call to forward() replaces all the existing data in the current instance. I.e.
     * the same instance of FFT can be re-used.
     *
     * @param data   Data to be transformed.
     * @param window math.WindowFunction to use for the transformation.
     */
    public void forward(double[] data, float samplingrate, WindowFunction window) {
        this.windowFunction = window;
        this.samplingrate = samplingrate;

        int actual_length = data.length;
        int valid_length = FFTUtil.nextPowerOf2(actual_length);
        double[] reshaped = new double[valid_length];
        for (int i = 0; i < reshaped.length; i++) {
            if (i < actual_length) {
                reshaped[i] = data[i] * this.windowFunction.value(i, valid_length);
            } else {
                reshaped[i] = 0;
            }
        }

        /* Perform FFT using FastFourierTransformer library. */
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        this.data = transformer.transform(reshaped, TransformType.FORWARD);

        /* Reset the calculated properties. */
        this.powerSpectrum = null;
        this.magnitudeSpectrum = null;
    }

    /**
     * Returns the magnitude spectrum of the transformed data. If that spectrum has not been
     * calculated yet it will be upon invocation of the method.
     *
     * @return Array containing the magnitude for each frequency bin.
     */
    public Spectrum getMagnitudeSpectrum() {
        if (this.magnitudeSpectrum == null) {
            this.magnitudeSpectrum = Spectrum.createMagnitudeSpectrum(this.data, this.samplingrate, this.windowFunction);
        }

        return this.magnitudeSpectrum;
    }

    /**
     * Returns the power spectrum of the transformed data. If that spectrum has not been
     * calculated yet it will be upon invocation of the method.
     *
     * @return Array containing the power for each frequency bin.
     */
    public Spectrum getPowerSpectrum() {
        if (this.powerSpectrum == null) {
            this.powerSpectrum = Spectrum.createPowerSpectrum(this.data, this.samplingrate, this.windowFunction);
        }
        return this.powerSpectrum;
    }

    /**
     * Getter for the transformed data.
     *
     * @return Array containing the raw FFT data.
     */
    public final Complex[] getValues() {
        return this.data;
    }

    /**
     * Can be used to directly access a FFT coefficient at the
     * specified index.
     *
     * @param index Index of the coefficient that should be retrieved.
     * @return Fourier coefficient.
     */
    public final Complex get(int index) {
        return this.data[index];
    }

    /**
     * Getter for samplingrate.
     *
     * @return Rate at which the original signal has been sampled.
     */
    public final float getSamplingrate() {
        return this.samplingrate;
    }

    /**
     * Getter for samplingrate.
     *
     * @return Rate at which the original signal has been sampled.
     */
    public final int getWindowsize() {
        return this.data.length;
    }

    /**
     * Returns true if the FFT only contains zeros and false
     * otherwise
     */
    public final boolean isZero() {
        for (Complex coefficient : this.data) {
            if (coefficient.abs() > 0) {
                return false;
            }
        }
        return true;
    }

}


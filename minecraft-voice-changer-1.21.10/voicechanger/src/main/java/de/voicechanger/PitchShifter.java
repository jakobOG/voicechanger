package de.voicechanger;

/**
 * Phasen-Vocoder (nach dem bekannten "smbPitchShift"-Verfahren) mit getrennter Steuerung
 * von Tonhöhe und Stimmfarbe (Formanten). Arbeitet in Echtzeit mit ca. 32 ms Verzögerung.
 */
final class PitchShifter {
    private static final int N = 2048;
    private static final int OSAMP = 4;
    private static final int STEP = N / OSAMP;
    private static final int N2 = N / 2;
    private static final int LAT = N - STEP;
    private static final double FS = 48000.0;
    private static final double FREQ_PER_BIN = FS / N;
    private static final double EXPCT = 2.0 * Math.PI * STEP / N;
    private static final int ENV_W = 5;

    private final float[] inFifo = new float[N];
    private final float[] outFifo = new float[N];
    private final float[] outAccum = new float[2 * N];
    private final double[] re = new double[N];
    private final double[] im = new double[N];
    private final double[] lastPhase = new double[N2 + 1];
    private final double[] sumPhase = new double[N2 + 1];
    private final double[] anaMagn = new double[N2 + 1];
    private final double[] anaFreq = new double[N2 + 1];
    private final double[] synMagn = new double[N2 + 1];
    private final double[] synFreq = new double[N2 + 1];
    private final double[] env = new double[N2 + 2];
    private final double[] window = new double[N];
    private final double[] cosT = new double[N / 2];
    private final double[] sinT = new double[N / 2];
    private final int[] rev = new int[N];
    private int rover = LAT;

    PitchShifter() {
        for (int k = 0; k < N; k++) window[k] = -0.5 * Math.cos(2.0 * Math.PI * k / N) + 0.5;
        for (int k = 0; k < N / 2; k++) {
            cosT[k] = Math.cos(2.0 * Math.PI * k / N);
            sinT[k] = Math.sin(2.0 * Math.PI * k / N);
        }
        int bits = Integer.numberOfTrailingZeros(N);
        for (int i = 0; i < N; i++) rev[i] = Integer.reverse(i) >>> (32 - bits);
    }

    private void fft(boolean inverse) {
        for (int i = 0; i < N; i++) {
            int j = rev[i];
            if (i < j) {
                double t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        double sign = inverse ? 1.0 : -1.0;
        for (int len = 2; len <= N; len <<= 1) {
            int half = len / 2;
            int step = N / len;
            for (int i = 0; i < N; i += len) {
                for (int j = 0; j < half; j++) {
                    double wr = cosT[j * step];
                    double wi = sign * sinT[j * step];
                    int a = i + j, b = a + half;
                    double tr = re[b] * wr - im[b] * wi;
                    double ti = re[b] * wi + im[b] * wr;
                    re[b] = re[a] - tr;
                    im[b] = im[a] - ti;
                    re[a] += tr;
                    im[a] += ti;
                }
            }
        }
    }

    /**
     * @param pitch   Tonhöhen-Faktor (2^(Halbtöne/12))
     * @param formant Formanten-Faktor (1.0 = Stimmfarbe unverändert)
     */
    void process(float[] in, float[] out, int n, double pitch, double formant) {
        for (int i = 0; i < n; i++) {
            inFifo[rover] = in[i];
            out[i] = outFifo[rover - LAT];
            rover++;
            if (rover >= N) {
                rover = LAT;
                frame(pitch, formant);
            }
        }
    }

    private void frame(double pitch, double formant) {
        for (int k = 0; k < N; k++) {
            re[k] = inFifo[k] * window[k];
            im[k] = 0.0;
        }
        fft(false);

        // Analyse
        for (int k = 0; k <= N2; k++) {
            double r = re[k], m = im[k];
            double magn = 2.0 * Math.sqrt(r * r + m * m);
            double phase = Math.atan2(m, r);
            double tmp = phase - lastPhase[k];
            lastPhase[k] = phase;
            tmp -= k * EXPCT;
            long qpd = (long) (tmp / Math.PI);
            if (qpd >= 0) qpd += qpd & 1; else qpd -= qpd & 1;
            tmp -= Math.PI * qpd;
            tmp = OSAMP * tmp / (2.0 * Math.PI);
            tmp = k * FREQ_PER_BIN + tmp * FREQ_PER_BIN;
            anaMagn[k] = magn;
            anaFreq[k] = tmp;
        }

        // grobe Spektralhülle (Formanten) durch gleitenden Mittelwert
        for (int k = 0; k <= N2; k++) {
            int lo = Math.max(0, k - ENV_W), hi = Math.min(N2, k + ENV_W);
            double s = 0;
            for (int j = lo; j <= hi; j++) s += anaMagn[j];
            env[k] = s / (hi - lo + 1);
        }
        env[N2 + 1] = env[N2];

        // Tonhöhe verschieben (Feinstruktur ohne Hülle)
        java.util.Arrays.fill(synMagn, 0.0);
        java.util.Arrays.fill(synFreq, 0.0);
        for (int k = 0; k <= N2; k++) {
            int idx = (int) (k * pitch);
            if (idx <= N2) {
                synMagn[idx] += anaMagn[k] / (env[k] + 1e-9);
                synFreq[idx] = anaFreq[k] * pitch;
            }
        }
        // Hülle (Stimmfarbe) wieder aufprägen, verschoben um den Formanten-Faktor
        for (int k = 0; k <= N2; k++) {
            double src = k / formant;
            int i0 = (int) src;
            double e;
            if (i0 >= N2) {
                e = 0.0;
            } else {
                double frac = src - i0;
                e = env[i0] * (1.0 - frac) + env[i0 + 1] * frac;
            }
            synMagn[k] *= e;
        }

        // Synthese
        for (int k = 0; k <= N2; k++) {
            double magn = synMagn[k];
            double tmp = synFreq[k];
            tmp -= k * FREQ_PER_BIN;
            tmp /= FREQ_PER_BIN;
            tmp = 2.0 * Math.PI * tmp / OSAMP;
            tmp += k * EXPCT;
            sumPhase[k] += tmp;
            double phase = sumPhase[k];
            re[k] = magn * Math.cos(phase);
            im[k] = magn * Math.sin(phase);
        }
        for (int k = N2 + 1; k < N; k++) {
            re[k] = 0.0;
            im[k] = 0.0;
        }
        fft(true);

        for (int k = 0; k < N; k++) {
            outAccum[k] += (float) (2.0 * window[k] * re[k] / (N2 * OSAMP));
        }
        System.arraycopy(outAccum, 0, outFifo, 0, STEP);
        System.arraycopy(outAccum, STEP, outAccum, 0, N);
        System.arraycopy(inFifo, STEP, inFifo, 0, LAT);
    }
}

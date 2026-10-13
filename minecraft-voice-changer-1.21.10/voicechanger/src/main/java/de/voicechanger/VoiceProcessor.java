package de.voicechanger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Verarbeitet die Mikrofon-Frames und gibt die veränderte Stimme zurück. */
public final class VoiceProcessor {
    private static PitchShifter shifter = new PitchShifter();
    private static long lastCall = 0;

    private static final Object MON_LOCK = new Object();
    private static SourceDataLine monitorLine;

    private VoiceProcessor() {}

    /** Gibt null zurück, wenn das Signal unverändert bleiben soll. */
    public static short[] process(short[] in) {
        VcConfig c = VcConfig.get();
        if (!c.enabled || in == null || in.length == 0) return null;
        if (c.semitones < 0.05f && Math.abs(c.formant - 1f) < 0.01f && Math.abs(c.volume - 1f) < 0.01f) return null;

        // Nach einer Sprechpause den Puffer leeren, damit keine alten Reste zu hören sind
        long now = System.nanoTime();
        if (now - lastCall > 300_000_000L) shifter = new PitchShifter();
        lastCall = now;

        int n = in.length;
        float[] f = new float[n];
        for (int i = 0; i < n; i++) f[i] = in[i] / 32768f;
        float[] o = new float[n];
        double pitch = Math.pow(2.0, c.semitones / 12.0);
        shifter.process(f, o, n, pitch, Math.max(0.5, c.formant));

        short[] out = new short[n];
        float vol = c.volume;
        for (int i = 0; i < n; i++) {
            float x = o[i] * vol;
            float ax = Math.abs(x);
            if (ax > 0.8f) x = Math.signum(x) * (0.8f + 0.2f * (float) Math.tanh((ax - 0.8f) / 0.2f));
            out[i] = (short) Math.max(-32767, Math.min(32767, Math.round(x * 32767f)));
        }
        monitor(out, c.monitor);
        return out;
    }

    private static void monitor(short[] f, boolean on) {
        synchronized (MON_LOCK) {
            if (!on) {
                if (monitorLine != null) {
                    try { monitorLine.stop(); monitorLine.close(); } catch (Exception ignored) { }
                    monitorLine = null;
                }
                return;
            }
            try {
                if (monitorLine == null) {
                    AudioFormat fmt = new AudioFormat(48000f, 16, 1, true, false);
                    monitorLine = AudioSystem.getSourceDataLine(fmt);
                    monitorLine.open(fmt, 960 * 2 * 8);
                    monitorLine.start();
                }
                byte[] b = new byte[f.length * 2];
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(f);
                if (monitorLine.available() >= b.length) monitorLine.write(b, 0, b.length);
            } catch (Exception e) {
                VcConfig.get().monitor = false;
                monitorLine = null;
            }
        }
    }
}

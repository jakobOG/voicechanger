package de.voicechanger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Einstellungen, gespeichert in config/voicechanger/config.json. */
public final class VcConfig {
    public boolean enabled = true;
    /** Tonhöhe in Halbtönen (0 bis 14). */
    public float semitones = 8f;
    /** Stimmfarbe (Formanten), 1.0 = unverändert, höher = heller/weiblicher. */
    public float formant = 1.15f;
    public float volume = 1.0f;
    /** Eigene verzerrte Stimme zum Testen mithören (Kopfhörer benutzen!). */
    public boolean monitor = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile VcConfig instance;

    public static VcConfig get() {
        VcConfig c = instance;
        if (c == null) {
            synchronized (VcConfig.class) {
                if (instance == null) instance = load();
                c = instance;
            }
        }
        return c;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("voicechanger").resolve("config.json");
    }

    private static VcConfig load() {
        try {
            Path p = file();
            if (Files.exists(p)) {
                VcConfig c = GSON.fromJson(Files.readString(p), VcConfig.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) {
        }
        return new VcConfig();
    }

    public static synchronized void save() {
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(get()));
        } catch (IOException ignored) {
        }
    }
}

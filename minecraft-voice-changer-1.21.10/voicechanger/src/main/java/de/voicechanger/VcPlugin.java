package de.voicechanger;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/** Simple-Voice-Chat-Plugin: verändert das Mikrofonsignal, bevor es gesendet wird. */
public class VcPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "voicechanger";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, event -> {
            short[] in = event.getRawAudio();
            if (in == null || in.length == 0) return;
            short[] out = VoiceProcessor.process(in);
            if (out != null) {
                event.setRawAudio(out);
            }
        });
    }
}

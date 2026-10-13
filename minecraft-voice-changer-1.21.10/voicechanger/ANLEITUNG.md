# Minecraft Voice Changer (Stimmenverzerrer) – Anleitung

Verändert deine Stimme im **Simple Voice Chat**, bevor sie gesendet wird. Ziel: **Minecraft 1.21.10, Fabric**.

## 1. JAR bauen (GitHub)
1. Erstelle bei https://github.com/new ein **neues** Repository, z. B. `voicechanger`.
2. **uploading an existing file** → entpackten Inhalt dieses Ordners hineinziehen → **Commit changes**.
3. Der Ordner `.github` kommt beim Upload oft nicht mit. Dann:
   **Add file → Create new file**, Name `.github/workflows/build.yml`, den Inhalt der Datei `build.yml` (liegt auch im Ordner) einfügen → **Commit changes**.
4. Reiter **Actions** → Build abwarten → unter **Artifacts** `voicechanger-jar` herunterladen. In der ZIP liegt `voicechanger-1.0.0.jar`.

## 2. Installieren
- JAR in den `mods`-Ordner deines Profils legen (zusammen mit Fabric API und Simple Voice Chat, die du schon hast).
- Der Server braucht ebenfalls Simple Voice Chat.

## 3. Benutzen
- Taste **K** öffnet das Menü (änderbar unter Steuerung → „Minecraft Voice Changer“).
- Voreinstellungen: **Mädchen leicht**, **Mädchen**, **Mädchen hoch**, **Kind**.
- Regler: **Tonhöhe** (Halbtöne), **Stimmfarbe** (Formanten, höher = heller) und **Lautstärke**.
- **Selbst hören (Test)** nur mit Kopfhörern einschalten, sonst gibt es Rückkopplung.
- Einstellungen werden in `config/voicechanger/config.json` gespeichert.

## Tipps für einen natürlichen Klang
- Tonhöhe ca. +6 bis +9 und Stimmfarbe ca. x1.15 bis x1.25 klingt am glaubwürdigsten.
- Zu hohe Werte klingen nach „Chipmunk“. Sprich eher klar und etwas leiser.
- Die Verzögerung beträgt ca. 30–40 ms und ist im Spiel kaum bemerkbar.

## Hinweis zusammen mit der YouTube-Audio-Mod
Beide Mods gleichzeitig zu nutzen kann dazu führen, dass die Musik ebenfalls verändert wird, je nachdem, in welcher Reihenfolge der Voice-Chat die Events abarbeitet. Zum Testen am besten einzeln benutzen.

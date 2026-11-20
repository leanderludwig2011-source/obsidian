# Auto Obsidian Surround – Fabric 1.21.11

**Dies ist der Quellcode, noch keine installierbare JAR.**

- **G**: Obsidian-Schutzhülle bauen / Vorgang abbrechen.
- Benötigt Obsidian in der **Hotbar** sowie Fabric API und Fabric Loader für Minecraft **1.21.11**.
- Versucht die 8 Nachbarfelder in Höhe der Füße und des Kopfes sowie 9 Dachfelder direkt darüber zu schließen (maximal 25 Blöcke). Das Feld unter dir wird **nicht** verändert.
- Blöcke werden nur per normalem Vanilla-Interaktionsaufruf platziert, mit Reichweitenlimit und nur bei gültiger Nachbarfläche; kann nicht frei in der Luft bauen. Wenn kein platzierbarer Nachbar existiert, werden einige Felder ausgelassen. Auf Servern können Anti-Cheat/Schutzmechanismen Platzierungen verhindern.
- Wechsel in den Obsidian-Hotbar-Slot; versucht nach Abschluss den ursprünglichen Slot wiederherzustellen.
- Es ist eine erste ungetestete Implementierung. Keine Garantie für erfolgreiches Kompilieren oder Funktionieren im NoRisk Client.

## Ohne Terminal mit GitHub Actions
1. ZIP entpacken und **alle** Dateien inklusive `.github` ins Repository hochladen.
2. GitHub Actions → `Build Fabric JAR`; bei erfolgreichem Build das Artefakt `AutoObsidianSurround-JAR` herunterladen.
3. Die heruntergeladene Artefakt-ZIP entpacken und die Datei `auto-obsidian-surround-1.0.0.jar` in den `mods`-Ordner deines **Fabric-Profils** legen. **Nicht** als Modpack importieren.
4. Fabric API muss ebenfalls installiert sein.

## Technischer Hinweis
Keine direkte Weltmanipulation; `ClientPlayerInteractionManager.interactBlock` sendet gewöhnliche Platzierungsinteraktionen. Weder Flugbau noch Platzierung ohne Obsidian wird unterstützt.

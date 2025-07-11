# TranslationChecker

TranslationChecker ist ein leistungsstarkes Tool zur Überprüfung und Verwaltung von Übersetzungsdateien in mehrsprachigen Java-Anwendungen. Es identifiziert fehlende Übersetzungen (leere Einträge oder mit Zielsprache markierte Einträge, z.B.: "(en)" für Englisch) und bietet Funktionen zum einfachen Bearbeiten und Übersetzen dieser Einträge.

## Funktionen

- **Dynamische Suche**: Findet Übersetzungsdateien und zeigt Ergebnisse in Echtzeit an
- **Fortschrittsanzeige pro Sprache**: Zeigt den Fortschritt für jede Sprache während des Scannens
- **Abbrechen-Funktion**: Ermöglicht das Abbrechen laufender Suchvorgänge
- **Automatische Übersetzung**: Übersetzt leere Felder automatisch aus deutschen Quellübersetzungen
- **Filterung**: Filtert Übersetzungen nach Sprache
- **Batch-Übersetzung**: Übersetzt mehrere Einträge gleichzeitig
- **Konfigurierbare Einstellungen**: Anpassbare Optionen für Pfade, API-Keys und mehr
- **Fortschrittliches Logging**: Konfigurierbare Log-Levels und Log-Viewer

## Installation

### Voraussetzungen

- Java 21 oder höher
- Maven für die Abhängigkeitsverwaltung

### Schritte

1. Klone das Repository:
   ```bash
   git clone https://github.com/yourusername/TranslationChecker.git
   cd TranslationChecker
   ```

2. Baue das Projekt mit Maven:
   ```bash
   mvn clean install
   ```

3. Starte die Anwendung:
   ```bash
   java -jar target/translation-checker.jar
   ```

## Verwendung

### Grundlegende Verwendung

1. **Einstellungen konfigurieren**:
   - Klicke auf "Settings" und gib den Basispfad zu deinen Übersetzungsdateien ein
   - Optional: Gib einen DeepL API-Schlüssel für automatische Übersetzungen ein
   - Wähle das gewünschte Log-Level aus

2. **Suche starten**:
   - Klicke auf "Search", um nach Übersetzungsdateien zu suchen
   - Die Ergebnisse werden in Echtzeit angezeigt, während die Dateien gescannt werden
   - Der Fortschritt für jede Sprache wird separat angezeigt

3. **Übersetzungen bearbeiten**:
   - Doppelklicke auf einen Eintrag, um ihn direkt zu bearbeiten
   - Wähle mehrere Einträge aus und klicke auf "Translate", um sie automatisch zu übersetzen
   - Klicke auf "Edit all Translations", um alle Übersetzungen in einem Dialog zu bearbeiten

4. **Filtern**:
   - Klicke auf "Filter", um Übersetzungen nach Sprache zu filtern

### Fortgeschrittene Funktionen

- **Automatische Übersetzung leerer Felder**:
  Die Anwendung kann leere Übersetzungsfelder automatisch aus deutschen Quellübersetzungen übersetzen.

- **Log-Viewer**:
  Klicke auf "Logs", um den Log-Viewer zu öffnen und die Anwendungslogs anzuzeigen.

## Konfiguration

### Einstellungen

Die folgenden Einstellungen können über den Settings-Dialog konfiguriert werden:

| Einstellung | Beschreibung | Standardwert |
|------------|-------------|-------------|
| Base Path | Basispfad zu den Übersetzungsdateien | PATH_TO_PROJECT |
| DeepL API Key | API-Schlüssel für DeepL-Übersetzungen | KEY |
| Search Unset Only | Nur nach nicht gesetzten Übersetzungen suchen | true |
| Convert Files | Dateien automatisch konvertieren | false |
| Language Detection | Spracherkennung aktivieren | true |
| Log Level | Log-Level für die Anwendung | INFO |
| Auto Translate Empty | Leere Felder automatisch übersetzen | true |

### Dateiformate

Die Anwendung unterstützt Java-Properties-Dateien mit folgendem Namensschema:

```
messages_<sprachcode>.properties
```

Beispiele:
- `messages_de.properties` (Deutsch)
- `messages_en.properties` (Englisch)
- `messages_fr.properties` (Französisch)

## Für Entwickler

### Projektstruktur

- `src/main/java/com/gui/`: Hauptquellcode
  - `contsants/`: Konstanten und Enums
  - `core/`: Kernfunktionalität (TranslationCheck)
  - `manager/`: Manager-Klassen (Settings, Logging, etc.)
  - `model/`: Datenmodelle
  - `services/`: Dienste (DeepL, AutoTranslation, etc.)
  - `ui/`: UI-Komponenten und Dialoge

### Wichtige Klassen

- `TranslationCheckerApp`: Hauptanwendungsklasse mit UI
- `TranslationCheck`: Kernklasse für die Suche und Verarbeitung von Übersetzungsdateien
- `SettingsManager`: Verwaltet die Anwendungseinstellungen
- `LogManager`: Verwaltet das Logging-System
- `TranslationManager`: Verwaltet Übersetzungsfunktionen

### Erweiterung

#### Neue Sprachen hinzufügen

Um eine neue Sprache hinzuzufügen, erweitere das `LanguagesConstant`-Enum in `com.gui.contsants.LanguagesConstant.java`:

```java
public enum LanguagesConstant {
    ENGLISH(new Locale("en"), StandardCharsets.UTF_8),
    GERMAN(new Locale("de"), StandardCharsets.UTF_8),
    // Neue Sprache hinzufügen:
    NEW_LANGUAGE(new Locale("code"), StandardCharsets.UTF_8);
    
    // Rest des Enums...
}
```

#### Leistungsoptimierung

Die Anwendung verwendet bereits mehrere Optimierungen:

- **Batch-Updates**: UI-Updates werden in Batches durchgeführt, um die Leistung zu verbessern
- **Parallele Verarbeitung**: Dateien werden parallel verarbeitet
- **Speicheroptimierung**: Properties-Objekte werden nach dem UI-Update freigegeben

Für weitere Optimierungen könnten folgende Ansätze verfolgt werden:

- Dynamische Thread-Pool-Größe basierend auf der Systemleistung
- Lazy Loading für sehr große Datensätze
- Caching von Übersetzungen für häufig verwendete Schlüssel

## Fehlerbehebung

### Häufige Probleme

1. **Keine Dateien gefunden**
   - Überprüfe den Basispfad in den Einstellungen
   - Stelle sicher, dass die Dateien dem richtigen Namensschema folgen

2. **Übersetzungsfehler**
   - Überprüfe den DeepL API-Schlüssel
   - Stelle sicher, dass du über ausreichend API-Kontingent verfügst

3. **Leistungsprobleme bei großen Ordnern**
   - Erhöhe den verfügbaren Speicher mit `-Xmx` JVM-Option
   - Verwende Filter, um die Anzahl der angezeigten Ergebnisse zu reduzieren

## Lizenz

Dieses Projekt steht unter der MIT-Lizenz - siehe die LICENSE-Datei für Details.

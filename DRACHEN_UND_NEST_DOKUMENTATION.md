# 🐉 Drachen und Nest - Vollständige Dokumentation

## 📋 Inhaltsverzeichnis
1. [Übersicht](#übersicht)
2. [Das Drachennest](#das-drachennest)
3. [Die Drachen-Entity](#die-drachen-entity)
4. [KI-Verhalten (AI Goals)](#ki-verhalten-ai-goals)
5. [Weltgenerierung](#weltgenerierung)
6. [Anpassungsmöglichkeiten](#anpassungsmöglichkeiten)

---

## 🎯 Übersicht

Das Drachen-System besteht aus mehreren zusammenhängenden Komponenten:
- **Drachennest-Block**: Ein spezieller Block, der als Heimat für Drachen dient
- **Drachen-Entity**: Eine fliegende Kreatur mit komplexem Verhalten
- **AI Goals**: Verschiedene Verhaltensweisen (Fliegen, Landen, Rasten, Verteidigen)
- **Weltgenerierung**: Automatische Platzierung von Nestern auf Berggipfeln

---

## 🏠 Das Drachennest

### Beschreibung
Das Drachennest ist ein spezieller Block, der als Spawn-Punkt und Heimat für 1-2 Drachen dient. Drachen kehren regelmäßig zu ihrem Nest zurück und verteidigen es gegen Eindringlinge.

### Eigenschaften

#### 🔒 Schutz & Abbau
- **Nur mit Silk Touch abbaubar**: Das Nest kann nur mit einer Spitzhacke abgebaut werden, die mit Silk Touch verzaubert ist
- **Explosionsimmun**: Völlig immun gegen Explosionen (TNT, Creeper, etc.)
- **Code-Datei**: [`DragonNestBlock.java`](src/main/java/de/davidvogt/hkbmod/block/custom/DragonNestBlock.java)

#### 🐉 Drachen-Management
Das Nest spawnt und verwaltet automatisch Drachen:
- **Initial Spawn**: 1-2 Drachen werden beim ersten Erstellen gespawnt
- **Respawn-System**: Tote Drachen werden nach 10 Minuten (12.000 Ticks) respawnt
- **UUID-Tracking**: Jedes Nest speichert die UUIDs seiner zugehörigen Drachen
- **Code-Datei**: [`DragonNestBlockEntity.java`](src/main/java/de/davidvogt/hkbmod/block/entity/DragonNestBlockEntity.java)

#### 🔧 Technische Details
```java
// Konfiguration im DragonNestBlockEntity
MIN_DRAGONS = 1                    // Minimum Anzahl Drachen
MAX_DRAGONS = 2                    // Maximum Anzahl Drachen
RESPAWN_COOLDOWN_TICKS = 12000     // 10 Minuten Respawn-Zeit
SPAWN_RADIUS = 10                  // Spawn-Radius um das Nest
CHECK_INTERVAL = 100               // Überprüfung alle 5 Sekunden
```

### Nest-Struktur (Weltgenerierung)
Das Nest wird mit folgenden Elementen generiert:
- **7x7 Plattform** aus Mossy Cobblestone Slabs
- **Treppen** am äußeren Rand (nach innen gerichtet)
- **Nest-Block** in der Mitte
- **4 Dracheneier** auf doppelten Slabs in den 4 Himmelsrichtungen (N, O, S, W)

---

## 🐲 Die Drachen-Entity

### Allgemeine Eigenschaften
- **Name**: `DragonEntity`
- **Typ**: Monster (aber neutral, greift nur bei Bedrohung an)
- **Größe**: Kleiner als der Ender Dragon
- **Fähigkeiten**: Fliegen, Feuerbälle schießen, Feueratem
- **Code-Datei**: [`DragonEntity.java`](src/main/java/de/davidvogt/hkbmod/item/entity/custom/DragonEntity.java)

### Zustände (States)
Der Drache hat verschiedene Zustände, die sein Verhalten beeinflussen:

#### 1. **Fliegend** (`!isLanded`)
- Normaler Flugmodus
- Patroulliert in der Umgebung
- Kann landen oder ruhen

#### 2. **Gelandet** (`isLanded`)
- Steht auf dem Boden
- Begrenzte Flugdauer (zufällig zwischen 30-120 Sekunden)
- Kann vom Boden aus angreifen

#### 3. **Rastend** (`isResting`)
- Liegt zusammengerollt wie ein Eisbär
- Pose: SLEEPING
- Ruht für 2 Minuten
- Hat kleineren Erkennungsradius (10 Blöcke statt 50)

### Angriffsfähigkeiten

#### 🔥 Explosive Feuerbälle
- **Reichweite**: 20-40 Blöcke
- **Cooldown**: 40 Ticks (2 Sekunden)
- **Effekt**: TNT-ähnliche Explosion
- **Verhalten**: Drache hält Abstand und schießt aus der Luft

#### 🔥 Feueratem
- **Reichweite**: Unter 10 Blöcke
- **Cooldown**: 20 Ticks (1 Sekunde)
- **Effekt**: Kegelförmiger Feuerstrahl
- **Verhalten**: Nahkampf-Angriff bei nahen Gegnern

### Attribute
```java
Attributes.MAX_HEALTH: 100.0        // Lebensenergie
Attributes.MOVEMENT_SPEED: 0.8      // Bewegungsgeschwindigkeit
Attributes.FLYING_SPEED: 0.8        // Fluggeschwindigkeit
Attributes.ATTACK_DAMAGE: 10.0      // Direkter Schaden (falls implementiert)
Attributes.FOLLOW_RANGE: 48.0       // Erkennungsreichweite
```

---

## 🤖 KI-Verhalten (AI Goals)

Die Drachen haben ein komplexes Verhaltenssystem mit mehreren AI Goals in Prioritätsreihenfolge:

### Priority 0: **Return to Nest Goal** (Höchste Priorität)
- **Datei**: [`ReturnToNestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/ReturnToNestGoal.java)
- **Funktion**: Kehrt zum Nest zurück
- **Auslöser**:
  - Zu weit vom Nest entfernt (> 128 Blöcke)
  - Gesundheit unter 30%
- **Verhalten**: Fliegt direkt zum Nest und landet in 10-Block-Radius

### Priority 1: **Defend Nest Goal**
- **Datei**: [`DefendNestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DefendNestGoal.java)
- **Funktion**: Verteidigt das Nest gegen Spieler
- **Reichweite**:
  - Normal: 50 Blöcke
  - Beim Rasten: 10 Blöcke
- **Kampfstrategie**:
  - **Fliegend**: Schießt Feuerbälle aus 20-40 Blöcken Entfernung
  - **Nah**: Verwendet Feueratem unter 10 Blöcken
  - **15% Chance**: Landet für Bodenangriff

### Priority 2: **Dragon Rest Goal**
- **Datei**: [`DragonRestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonRestGoal.java)
- **Funktion**: Ruht gelegentlich aus
- **Dauer**: 2 Minuten (2000 Ticks)
- **Verhalten**:
  - Fliegt entspannt zum Nest (60% normale Geschwindigkeit)
  - Rollt sich zusammen (SLEEPING Pose)
  - 50% Chance nach dem Landen zu rasten
  - 50% Chance direkt wieder abzuheben

### Priority 3: **Dragon Flying Goal**
- **Datei**: [`DragonFlyingGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonFlyingGoal.java)
- **Funktion**: Standardflugverhalten
- **Features**:
  - Wählt zufällige Flugziele
  - Kollisionserkennung mit Notfallausweichmanöver
  - Zufällige Flug- und Landedauern:
    - Flugzeit: 30-120 Sekunden
    - Landezeit: 20-90 Sekunden
  - Zufällige Entscheidung nach Landung (50% fliegen, 50% ruhen)

### Zusätzlich: **Dragon Move Control**
- **Datei**: [`DragonMoveControl.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonMoveControl.java)
- **Funktion**: Steuert die Flugbewegung und verhindert Interferenzen mit der Rotation

---

## 🌍 Weltgenerierung

### Spawn-Locations
Drachennester spawnen auf **Berggipfeln** in folgenden Biomen:

#### Konfigurierte Biome
- Alle Mountain-Varianten (für einfacheres Testen)
- Höhenbereich: **100-256 Blöcke**
- **Code-Dateien**:
  - Feature: [`DragonNestFeature.java`](src/main/java/de/davidvogt/hkbmod/worldgen/feature/DragonNestFeature.java)
  - Biome: [`ModBiomeModifiers.java`](src/main/java/de/davidvogt/hkbmod/worldgen/ModBiomeModifiers.java)

### Generierungs-Prozess
1. **Surface Scan**: Scannt von y=256 abwärts nach festem Block
2. **Höhenprüfung**: Überprüft, ob zwischen y=100 und y=256
3. **Strukturbau**:
   - 7x7 Plattform aus Stein als Basis
   - Mossy Cobblestone Slabs (Layer 1)
   - Treppen am Rand (nach innen gerichtet)
   - Nest-Block in der Mitte
   - 4 Dracheneier auf doppelten Slabs

### Seltenheit
- **Aktuell**: 1 Nest pro 64 Chunks (für Testing)
- **Empfohlen für Release**: 500-2000 Chunks
- **Konfiguration**: [`ModPlacedFeatures.java`](src/main/java/de/davidvogt/hkbmod/worldgen/ModPlacedFeatures.java)

---

## ⚙️ Anpassungsmöglichkeiten

Hier findest du alle wichtigen Werte, die du anpassen kannst, mit direkten Links zum Code:

### 🐉 Drachen-Eigenschaften

#### Attribute
📁 **Datei**: [`HKBMod.java`](src/main/java/de/davidvogt/hkbmod/HKBMod.java) (ca. Zeile 193)
```java
event.put(ModEntities.DRAGON.get(), DragonEntity.createAttributes()
    .add(Attributes.MAX_HEALTH, 100.0)           // ← Lebensenergie
    .add(Attributes.MOVEMENT_SPEED, 0.8)         // ← Bewegungsgeschwindigkeit
    .add(Attributes.FLYING_SPEED, 0.8)           // ← Fluggeschwindigkeit
    .add(Attributes.ATTACK_DAMAGE, 10.0)         // ← Angriffschaden
    .add(Attributes.FOLLOW_RANGE, 48.0)          // ← Erkennungsreichweite
    .build());
```

### 🏠 Nest-Konfiguration

#### Drachen-Anzahl & Respawn
📁 **Datei**: [`DragonNestBlockEntity.java`](src/main/java/de/davidvogt/hkbmod/block/entity/DragonNestBlockEntity.java) (Zeile 28-32)
```java
private static final int MIN_DRAGONS = 1;                      // ← Minimum Drachen
private static final int MAX_DRAGONS = 2;                      // ← Maximum Drachen
private static final int RESPAWN_COOLDOWN_TICKS = 12000;       // ← Respawn-Zeit (12000 = 10 Min)
private static final int SPAWN_RADIUS = 10;                    // ← Spawn-Radius
private static final int CHECK_INTERVAL = 100;                 // ← Überprüfungsintervall
```

### 🎯 Verhalten & KI

#### Return to Nest Goal
📁 **Datei**: [`ReturnToNestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/ReturnToNestGoal.java) (Zeile 19-22)
```java
private static final double MAX_DISTANCE_FROM_NEST = 128.0D;   // ← Max. Entfernung
private static final double NEST_ARRIVAL_DISTANCE = 10.0D;     // ← Ankunfts-Radius
private static final double LOW_HEALTH_THRESHOLD = 0.3D;       // ← Rückkehr bei 30% HP
private static final int CHECK_EVERY_N_TICKS = 40;             // ← Prüf-Intervall
```

#### Defend Nest Goal
📁 **Datei**: [`DefendNestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DefendNestGoal.java) (Zeile 18-26)
```java
private static final double NEST_DEFENSE_RADIUS = 50.0D;       // ← Verteidigungs-Radius
private static final double RESTING_DETECTION_RADIUS = 10.0D;  // ← Erkennungs-Radius (rastend)
private static final double MIN_ATTACK_DISTANCE = 20.0D;       // ← Min. Angriffs-Distanz
private static final double MAX_ATTACK_DISTANCE = 40.0D;       // ← Max. Angriffs-Distanz
private static final double FIRE_BREATH_DISTANCE = 10.0D;      // ← Feueratem-Distanz
private static final int FIREBALL_COOLDOWN = 40;               // ← Feuerball-Cooldown (Ticks)
private static final int FIRE_BREATH_COOLDOWN = 20;            // ← Feueratem-Cooldown (Ticks)
private static final int CHECK_INTERVAL = 20;                  // ← Überprüfungs-Intervall
private static final float LAND_TO_ATTACK_CHANCE = 0.15F;      // ← Chance zu landen (15%)
```

#### Dragon Rest Goal
📁 **Datei**: [`DragonRestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonRestGoal.java) (Zeile 15-19)
```java
private static final int REST_DURATION = 2000;                 // ← Rast-Dauer (2000 = 2 Min)
private static final int MIN_TIME_BETWEEN_RESTS = 0;           // ← Zeit zwischen Rasten
private static final int CHECK_INTERVAL = 20;                  // ← Prüf-Intervall
private static final double NEST_POSITION_THRESHOLD = 3.0;     // ← Nest-Positions-Toleranz
private static final double RELAXED_FLIGHT_SPEED = 0.6D;       // ← Entspannte Fluggeschw.
```

#### Dragon Flying Goal
📁 **Datei**: [`DragonFlyingGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonFlyingGoal.java) (Zeile 27-32)
```java
private static final int MIN_FLYING_DURATION = 200;            // ← Min. Flugdauer (30 Sek)
private static final int MAX_FLYING_DURATION = 1000;           // ← Max. Flugdauer (120 Sek)
private static final int MIN_LANDING_DURATION = 100;           // ← Min. Landedauer (20 Sek)
private static final int MAX_LANDING_DURATION = 800;           // ← Max. Landedauer (90 Sek)
private static final int STUCK_THRESHOLD = 40;                 // ← Steckengeblieben-Schwelle
private static final double MIN_MOVEMENT = 0.05;               // ← Min. Bewegung pro Tick
```

### 🌍 Weltgenerierung

#### Nest-Seltenheit
📁 **Datei**: [`ModPlacedFeatures.java`](src/main/java/de/davidvogt/hkbmod/worldgen/ModPlacedFeatures.java) (Zeile 47)
```java
dragonNestModifiers.add(RarityFilter.onAverageOnceEvery(64));  // ← SELTENHEIT HIER ÄNDERN!
// Empfohlen für Release: 500-2000
// Aktuell für Testing: 64
```

#### Höhenbeschränkung
📁 **Datei**: [`DragonNestFeature.java`](src/main/java/de/davidvogt/hkbmod/worldgen/feature/DragonNestFeature.java) (Zeile 44-47)
```java
if (surfacePos.getY() < 100 || surfacePos.getY() > 256) {     // ← Höhenbereich
    HKBMod.LOGGER.info("Dragon nest rejected: height {} out of range (100-256)", surfacePos.getY());
    return false;
}
```

#### Nest-Struktur-Größe
📁 **Datei**: [`DragonNestFeature.java`](src/main/java/de/davidvogt/hkbmod/worldgen/feature/DragonNestFeature.java) (Zeile 124-147)
```java
// 7x7 Plattform - Größe durch Schleifengrenzen definiert
for (int x = -3; x <= 3; x++) {                               // ← -3 bis +3 = 7x7 Plattform
    for (int z = -3; z <= 3; z++) {
        // ...
    }
}
```

#### Biome für Nest-Spawn
📁 **Datei**: [`ModBiomeModifiers.java`](src/main/java/de/davidvogt/hkbmod/worldgen/ModBiomeModifiers.java) (Zeile 29-41)
```java
// Hier kannst du die Biome ändern, in denen Nester spawnen
register(context, ADD_DRAGON_NEST, new AddFeaturesBiomeModifier(
    biomes.getOrThrow(BiomeTags.IS_MOUNTAIN),                 // ← Biome-Tag hier ändern
    HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.DRAGON_NEST_PLACED_KEY)),
    GenerationStep.Decoration.SURFACE_STRUCTURES
));
```

---

## 🔍 Nützliche Commands

### Nest finden
Im Spiel kannst du den Befehl `/locate_dragon_nest` verwenden, um das nächste Drachennest zu finden.

### Debug-Logging
Die meisten Klassen haben ausführliche Debug-Logs. Schau in die Konsole oder die Logs, um das Verhalten zu verfolgen:
- `[DragonNest]` - Nest-bezogene Nachrichten
- `[DRAGON]` - Drachen-Zustands-Änderungen
- `[FLYING-GOAL]` - Flugverhalten
- `[RETURN-TO-NEST]` - Rückkehr zum Nest
- `[DRAGON-REST-DEBUG]` - Rast-Verhalten

---

## 📝 Zusammenfassung der Hauptdateien

| Komponente | Datei | Beschreibung |
|------------|-------|--------------|
| **Nest Block** | [`DragonNestBlock.java`](src/main/java/de/davidvogt/hkbmod/block/custom/DragonNestBlock.java) | Block-Definition mit Silk Touch-Schutz |
| **Nest Entity** | [`DragonNestBlockEntity.java`](src/main/java/de/davidvogt/hkbmod/block/entity/DragonNestBlockEntity.java) | Drachen-Spawn & Management |
| **Drache** | [`DragonEntity.java`](src/main/java/de/davidvogt/hkbmod/item/entity/custom/DragonEntity.java) | Hauptklasse der Drachen-Entity |
| **Flug-KI** | [`DragonFlyingGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonFlyingGoal.java) | Flugverhalten & Bewegung |
| **Verteidigung** | [`DefendNestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DefendNestGoal.java) | Nest-Verteidigung & Kampf |
| **Rückkehr** | [`ReturnToNestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/ReturnToNestGoal.java) | Rückkehr zum Nest |
| **Rasten** | [`DragonRestGoal.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonRestGoal.java) | Rast-Verhalten |
| **Bewegung** | [`DragonMoveControl.java`](src/main/java/de/davidvogt/hkbmod/item/entity/ai/DragonMoveControl.java) | Flug-Steuerung |
| **Weltgen Feature** | [`DragonNestFeature.java`](src/main/java/de/davidvogt/hkbmod/worldgen/feature/DragonNestFeature.java) | Nest-Generierung |
| **Platzierung** | [`ModPlacedFeatures.java`](src/main/java/de/davidvogt/hkbmod/worldgen/ModPlacedFeatures.java) | Spawn-Rate Konfiguration |
| **Biomes** | [`ModBiomeModifiers.java`](src/main/java/de/davidvogt/hkbmod/worldgen/ModBiomeModifiers.java) | Biome-Zuordnung |

---

## 💡 Tipps für Anpassungen

1. **Drachen stärker machen**: Erhöhe `MAX_HEALTH` und `ATTACK_DAMAGE` in `HKBMod.java`
2. **Mehr Drachen pro Nest**: Erhöhe `MAX_DRAGONS` in `DragonNestBlockEntity.java`
3. **Seltenere Nester**: Erhöhe den Wert in `RarityFilter.onAverageOnceEvery()` in `ModPlacedFeatures.java`
4. **Aggressivere Drachen**: Erhöhe `NEST_DEFENSE_RADIUS` in `DefendNestGoal.java`
5. **Längere Flugzeiten**: Erhöhe `MAX_FLYING_DURATION` in `DragonFlyingGoal.java`
6. **Schnellerer Respawn**: Verringere `RESPAWN_COOLDOWN_TICKS` in `DragonNestBlockEntity.java`

---

## 📜 Lizenz & Credits

Teil des HKB Mods für Minecraft NeoForge.

---

**Viel Erfolg beim Anpassen deines Drachen-Systems! 🐉🔥**


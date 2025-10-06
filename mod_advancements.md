# Advancements hinzufügen — Schritt für Schritt (hkbmod)

Dieses Markdown erklärt dir **konkret und praxisnah**, wie du mit der vorhandenen Klasse `ModAdvancementProvider` neue Advancements hinzufügst, testest und typischen Problemen vorbeugst. Alle Beispiele beziehen sich auf den Code, den du gepostet hast.

---

## Voraussetzungen

* Ein lauffähiges Mod-Development-Environment (Gradle + Minecraft mappings / Fabric/Forge workspace).
* Deine Items/Blocks/Rezepte sind im Projekt registriert (`ModBlocks`, `ModItems` o.ä.).
* Die Klasse `ModAdvancementProvider` ist als `AdvancementSubProvider` registriert (Data-Generator-Setup).

---

## Kurzübersicht der Helper-Methoden (in `ModAdvancementProvider`)

* `addSimpleAdvancement(saver, path, parent, displayItem, triggerItem, type, experience)`

    * Fügt ein einfaches Advancement mit einem Inventory-Trigger (Aufheben/Inventar-Änderung) und XP-Belohnung hinzu.
* `addSimpleAdvancement(..., experience, recipeReward)`

    * Gleich wie oben, zusätzlich kann ein Rezept als Belohnung freigeschaltet werden.
* `addPickupAdvancement(saver, path, parent, item, experience)`

    * Shortcut für ein Advancement, das ein bestimmtes Item aufheben soll.
* `addCraftingAdvancement(saver, path, parent, craftedItem, recipeReward)`

    * Shortcut für ein Crafting-Advancement mit Rezept-Belohnung.
* `addChallengeAdvancement(...)`

    * Für schwerere / besondere Advancements (Challenge-Frame).
* `addCustomAdvancement(...)`

    * Vollständig anpassbares Advancement — du kannst beliebige `Criterion`-Instanzen und Rewards übergeben.

---

## Schritt 1 — Neues Advancement in `generate(...)` eintragen

Öffne `ModAdvancementProvider.generate(...)` und füge dort einen Aufruf einer der Helper-Methoden hinzu.

**Beispiele** (einfügen in `generate`):

```java
// Einfaches Pickup-Advancement
addPickupAdvancement(saver, "mining/first_iron", "minecraft:story/mine_stone",
        Items.IRON_ORE, 50);

// Crafting-Advancement (und Rezept-Belohnung)
addCraftingAdvancement(saver, "crafting/research_table", "research/root",
        ModBlocks.RESEARCH_TABLE, "research_table");

// Challenge-Advancement
addChallengeAdvancement(saver, "nether/enter_portal", "minecraft:adventure/root",
        Items.NETHER_STAR, Items.NETHER_STAR, 500);
```

**Hinweis:** `ModBlocks.RESEARCH_TABLE` muss ein `ItemLike` sein (meist ist ein Block ein `ItemLike`, weil `Block#asItem()` existiert).

---

## Schritt 2 — Parameter verstehen

* **path**: Pfad unter `data/<namespace>/advancements/`. Beispiel: `research/root` → Resource-ID `hkbmod:research/root`.
* **parent**: Parent-Advancement. Wenn der String einen Doppelpunkt enthält (z. B. `minecraft:story/root`), wird er direkt verwendet. Ohne Doppelpunkt wird `hkbmod:` vorangestellt.
* **displayItem**: Item, das im Advancement-Icon angezeigt wird (typischerweise `ItemLike` oder `ItemStack`).
* **triggerItem**: Item, das als Kriterium benutzt wird (z. B. beim Aufheben). Wird in der Helper-Methode als `InventoryChangeTrigger` gesetzt.
* **type**: `AdvancementType.TASK`, `AdvancementType.GOAL`, `AdvancementType.CHALLENGE`.
* **experience**: XP-Belohnung (ganze Zahl).
* **recipeReward**: (optional) Name des Rezepts in deinem Namespace; die Methode erstellt eine `ResourceKey` für `hkbmod:<recipeReward>`.

---

## Schritt 3 — Lokalisierung (Titel + Beschreibung)

Die Klasse benutzt `Component.translatable("advancements.hkbmod." + path.replace("/",".") + ".title")` bzw. `.description()`.

Für `path = "research/root"` lauten die Keys also:

```
advancements.hkbmod.research.root.title
advancements.hkbmod.research.root.description
```

Lege deine Sprachdateien an, z. B. `src/main/resources/assets/hkbmod/lang/en_us.json`:

```json
{
  "advancements.hkbmod.research.root.title": "Research Table",
  "advancements.hkbmod.research.root.description": "Craft a research table to start new discoveries."
}
```

Für Deutsch: `de_de.json` entsprechend übersetzen.

---

## Schritt 4 — Data-Generator ausführen (Generierung der JSON-Dateien)

* Führe die in deinem Workspace konfigurierte Data-Generator-Task aus. Oft lautet die Task `runData` oder ähnlich (z. B. `./gradlew runData`).
* Nach dem Laufen des Generators findest du die erzeugten JSON-Dateien typischerweise in einem `generated`-Ordner (z. B. `build/generated/resources/data/hkbmod/advancements/` oder im `run`-Ordner je nach Setup).
* Kopiere bei Bedarf die generierten Dateien nach `src/main/resources/data/hkbmod/advancements/`, wenn du sie dauerhaft ins Mod-JAR packen willst.

---

## Schritt 5 — In-Game testen

* Starte den Client mit den generierten Ressourcen (oder aus dem `run`-Ordner).
* Öffne das Advancements-Menü (Standard: `L` in Minecraft) und navigiere zu deinem Advancement.
* Du kannst ein Advancement auch per Befehl gewähren, um Tests zu vereinfachen:

```
/advancement grant @s only hkbmod:research/root
```

(Beachte: Advancement-ID enthält den Pfad mit Schrägstrich, z. B. `hkbmod:research/root`.)

---

## Schritt 6 — Häufige Fehler & Debugging

* **Kein Titel / Beschreibung**: Prüfe deine Sprachdatei und den Key (Punkte statt Slashes!).
* **Advancement erscheint nicht**: Wurde die Data-Generator-Task tatsächlich ausgeführt? Wurden die JSON-Dateien in den `run`-Ordner / `resources` gelegt?
* **Kriterium löst nicht aus**: `InventoryChangeTrigger` erwartet genau passende Itemstacks — prüfe Item/Count/NBT. Teste mit `/advancement grant`.
* **Recipe-Reward wird nicht freigeschaltet**: Achte darauf, dass das Rezept mit der ID `hkbmod:<recipeReward>` existiert (d.h. `data/hkbmod/recipes/<recipeReward>.json`).

---

## Schritt 7 — Fortgeschritten: Custom-Criterion und Rewards

Wenn du einen eigenen Criterion brauchst, nutze `addCustomAdvancement(...)` und übergebe ein `Criterion<?>`-Objekt, z. B. `InventoryChangeTrigger.TriggerInstance.hasItems(...)` oder andere Trigger aus `net.minecraft.advancements.critereon`.

**Beispiel**:

```java
// Custom-Advancement mit benutzerlichem Criterion
addCustomAdvancement(saver,
    "custom/collect_5_books",
    "research/root",
    new ItemStack(Items.BOOKSHELF),
    Component.translatable("advancements.hkbmod.custom.collect_5_books.title"),
    Component.translatable("advancements.hkbmod.custom.collect_5_books.description"),
    "has_5_books",
    InventoryChangeTrigger.TriggerInstance.hasItems(Items.BOOK), // hier kannst du komplexer bauen
    AdvancementType.GOAL,
    AdvancementRewards.Builder.experience(100).build() // Rewards-Objekt
);
```

> Achte darauf, das rewards-Objekt korrekt zu bauen (je nach deiner MC-Version kann die API leicht variieren).

---

## Tipps & Verbesserungen am bestehenden Code

* **Robusteren Item-Namen verwenden**: `getItemName(ItemLike)` nutzt aktuell `item.asItem().toString().replace(":","_")` — das ist nicht immer die stabilste Methode. Besser wäre die Registry-ID (`Registry.ITEM.getKey(item.asItem())`) zu verwenden, falls verfügbar.
* **Icon / Hintergrund setzen**: Der `display(...)`-Aufruf hat einen `background`-Parameter (du übergibst aktuell `null`). Du kannst dort ein `ResourceLocation` für eine custom Hintergrundgrafik setzen, wenn gewünscht.
* **Mehrere Kriterien**: Für komplexe Advancements kannst du mehrere Kriterien hinzufügen und z. B. `requirements` als AND/OR-Konfiguration setzen — dazu müsstest du den `Advancement.Builder` manuell konfigurieren (nicht nur `addCriterion` einmal).

---

## Schnell-Checkliste bevor du ein Advancement einfügst

1. Item/Block/Recipe existieren und sind registriert.
2. Lokalisierungen (title + description) in `assets/<namespace>/lang/*.json` vorhanden.
3. `path` sinnvoll gewählt (z. B. `category/name`).
4. Den Data-Generator ausführen und die erzeugten JSONs kontrollieren.
5. In-Game testen (ggf. mit `/advancement grant`).

---

Wenn du willst, schreibe ich dir jetzt **ein konkretes Beispiel-Advancement (inkl. Lokalisierung und Rezept-Check)**, das ich direkt in deine `generate(...)`-Methode einfüge — oder ich verbessere die `getItemName(...)`-Methode, damit sie stabilere IDs erzeugt. Sag mir kurz, welche Variante du möchtest (z.B. "crafting/research_table" oder "mining/first_iron").

---

Viel Erfolg — wenn du möchtest, füge ich gern ein Beispiel-Advancement direkt in deinen `generate(...)`-Code ein oder helfe beim Übersetzen/Debuggen.

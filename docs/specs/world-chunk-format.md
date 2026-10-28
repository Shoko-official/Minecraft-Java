# Chunks : sections, conteneurs à palette et schéma NBT

Statut : brouillon
Jalon : M3
Risque : B

## Périmètre

Couvre, pour `engine/world` :

- la géométrie : sections, hauteur par dimension, conversions de coordonnées (négatives comprises) ;
- les conteneurs à palette stockés sur disque (blocs 4096 entrées, biomes 64 entrées) : palette, bits par entrée, empaquetage dans les longs, ordre des index ;
- la lumière (tableaux de demi-octets, section absente, sections hors hauteur) et les heightmaps (types, encodage, état inconnu) ;
- la réconciliation de la liste de sections et le rôle de `yPos` ;
- le schéma NBT d'un chunk dans un fichier de région en 26.3 : champs, types, présence ;
- les statuts de génération et leur ordre.

Ne couvre pas :

- le fichier de région lui-même (en-tête, secteurs, compression) : `world-anvil-region.md` ;
- les états de blocs et leurs identifiants : `world-block-states.md` ;
- le format réseau des chunks (jalon M8), sauf pour situer les types de palette ;
- le contenu des block entities, entités, ticks et structures au-delà de leur enveloppe (ADR 0005 : NBT opaque) ;
- le calcul de la lumière et des heightmaps (M5) et la génération (M4) ;
- la mise à niveau des chunks d'anciennes data versions, et le stockage des biomes par bloc annoncé pour 26.4.

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `resources/data/minecraft/dimension_type/*.json` (`min_y`, `height`), `generated/reports/registries.json` (registre `minecraft:chunk_status`), `resources/data/minecraft/worldgen/biome/` (67 biomes), `resources/data/minecraft/worldgen/world_preset/flat.json` et `debug_all_block_states.json` (sondes), `version.json` (`world_version` 5023).
- **Calculs** : `world_ref.py` (annexe de `world-vectors.md`), sous-commandes `dims`, `pack`, `coords`, `light`, `region`, `check`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki "Chunk format" (structure NBT, empaquetage, heightmaps, lumière, statuts, historique jusqu'à 26.4 Snapshot 1 ; page marquée "à mettre à jour") ; Minecraft Wiki "Minecraft Wiki:Projects/wiki.vg merge/Chunk Format" (format réseau : types de palette, empaquetage, ordre des index) ; Minecraft Wiki "Heightmap" ; Minecraft Wiki "Java Edition 26.3 Snapshot 7" et "Java Edition 26.3 Snapshot 10".
- **Rapports locaux** (indices) : rapport Overworld section 1.2 (division vers le bas des coordonnées négatives, persistance des structures, ticks, lumière, statut) ; rapport d'architecture section 4.3 (principe des palettes) ; rapport 1.21.1 (biomes 4 x 4 x 4). Aucune règle n'en vient seule ; ils ne donnent aucun détail de format.
- **Révision du 2026-10-28** (décisions D1 à D10 de `world-decisions-log.md`) : Minecraft Wiki "Chunk format", révision du 2026-10-25, relue pour `PostProcessing` (ordre `0ZYX` donné à la section "ToBeTicked", partagé par `PostProcessing`), les sections de lumière au-dessus et au-dessous du monde, `SkyLight` omis, `yPos` (ajouté en 21w43a), longueur des heightmaps (37 longs, 36 avant 1.16) et "toutes les sections de la hauteur du monde sont présentes". La page reste marquée "à mettre à jour" et se contredit sur le nombre de listes de `PostProcessing` (24) et de `ToBeTicked` (16) ; aucune autre source publique autorisée n'a été trouvée pour ces points.
- **Sondes de l'oracle** : aucune exécutée. Sondes PC1 à PC7, PB2 et PB3 décrites dans `world-vectors.md`.

Niveaux : **D** valeur lue dans un fichier extrait ; **C** calcul reproductible (`world_ref.py`) à partir de règles D, ou à partir de règles S (la valeur calculée hérite alors du S, signalé "C sur S") ; **M** non utilisé ; **S** documentation publique seule (même concordante entre deux pages du wiki), rapport local ou hypothèse ; une règle S énonce elle-même le comportement **provisoire** que le moteur applique en attendant la sonde.

## Règles

### Géométrie et coordonnées

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Hauteurs par type de dimension : `overworld` et `overworld_caves` `min_y` -64, `height` 384 ; `the_nether` 0, 256 (`logical_height` 128, sans effet sur le stockage) ; `the_end` 0, 256. | D |
| R2 | Une section fait 16 x 16 x 16 blocs (taille S : aucun fichier extrait ne la fixe). Nombre de sections = `height / 16` : 24 pour l'Overworld (Y de section -4 à 19), 16 pour le Nether et l'End (0 à 15) ; les hauteurs de R1 sont toutes multiples de 16. | C sur S |
| R3 | Conversions, toujours par décalage arithmétique et masque, jamais par division entière vers zéro : chunk `cx = x >> 4`, `cz = z >> 4` ; Y de section `sy = y >> 4` ; coordonnées locales `x & 15`, `y & 15`, `z & 15`. Rang de la section dans la liste : `sy - (min_y >> 4)`. Exemples : x = -1 -> chunk -1, local 15 ; x = -17 -> chunk -2, local 15 ; y = -64 -> section -4, local 0. Le décalage de 4 découle de la taille de R2. | C sur S |
| R4 | Index d'un bloc dans sa section : `(ly * 16 + lz) * 16 + lx`, de 0 à 4095 (x varie le plus vite, puis z, puis y). | S |
| R5 | Biomes en 26.3 : une section contient 4 x 4 x 4 = 64 cellules de 4 x 4 x 4 blocs. Index : `(qy * 4 + qz) * 4 + qx` avec `q = local >> 2`. | S |

### Conteneurs à palette sur disque

| Réf | Règle | Preuve |
|---|---|---|
| R6 | Un conteneur est un Compound à deux clés : `palette` (List, au moins une entrée) et `data` (Long_Array). Si la palette a une seule entrée, `data` est absent et l'entrée remplit tout le conteneur (palette "unique"). Sinon `data` porte les index dans la palette, un par position (4096 pour les blocs, 64 pour les biomes). | S |
| R7 | Bits par entrée sur disque, `n` = taille de la palette, `n > 1` : blocs `max(4, ceil(log2(n)))`, biomes `ceil(log2(n))` sans minimum. Bornes : `n <= 4096` pour les blocs (12 bits au plus), `n <= 64` pour les biomes (6 bits au plus). Le nombre de bits n'est pas stocké : il se déduit de `n`. | S |
| R8 | Empaquetage : `p = floor(64 / bits)` entrées par long. L'entrée `i` est dans le long `floor(i / p)`, aux bits `bits * (i mod p)` à `bits * (i mod p) + bits - 1`, comptés depuis le bit de poids faible. Une entrée ne chevauche jamais deux longs ; les bits de poids fort inutilisés valent 0 à l'écriture et sont ignorés à la lecture (provisoire). Nombre de longs : `ceil(count / p)`. Les longs sont des Long NBT signés (gros-boutistes, `core-nbt.md` R3) : seul le motif de bits compte. | S |
| R9 | Conséquences chiffrées de R7 et R8 (table complète en PK1 de `world-vectors.md`) : blocs 4 bits -> 256 longs, 5 bits -> 342, 6 -> 410, 8 -> 512, 9 -> 586, 12 -> 820 ; biomes 1 bit -> 1 long, 2 -> 2, 3 -> 4, 6 -> 7. Seuils : palette de 16 -> 4 bits, 17 -> 5, 33 -> 6, 256 -> 8, 257 -> 9. | C sur S |
| R10 | Données incohérentes : longueur de `data` différente de R9, index supérieur ou égal à `n`, palette vide. **Provisoire** : le chunk est refusé (sens défini dans `world-anvil-region.md`, Entrées / sorties), l'erreur nomme la section et le conteneur. | S |
| R11 | Palette de blocs : entrées selon `world-block-states.md` R19 à R21. Palette de biomes : **provisoire** List de String (identifiants de biomes). Le wiki montre chaque entrée sous l'étiquette `Name` sans préciser si c'est une String ou un Compound ; le lecteur accepte aussi un Compound `{Name}`. Biome inconnu : **provisoire** `minecraft:plains` avec un avertissement. | S |
| R12 | Types de palette. Sur disque, seules deux formes sont observables : unique (R6, pas de `data`) et indexée (palette locale + index). Les distinctions "linéaire", "hachée" et "globale" ne concernent que la représentation en mémoire et le réseau : le moteur choisit librement sa représentation en mémoire, à condition de produire R6 à R8 sur disque. | S |
| R13 | Réseau (jalon M8, pour mémoire) : palette unique 0 bit ; indirecte 4 à 8 bits pour les blocs, 1 à 3 pour les biomes ; directe au-delà, avec les identifiants globaux. Largeur directe : 16 bits pour les blocs (35 723 états, `world-block-states.md` R12) et 7 bits pour les biomes (67 biomes, `ceil(log2(67))`). | C sur S |

### Lumière

| Réf | Règle | Preuve |
|---|---|---|
| R14 | `BlockLight` et `SkyLight` sont des Byte_Array de 2048 octets : 4096 valeurs de 0 à 15, deux par octet, index de R4. Index pair -> 4 bits de poids faible de l'octet `index >> 1` ; index impair -> 4 bits de poids fort. | S |
| R15 | Présence. `BlockLight` absent : aucune lumière de bloc dans la section (4096 valeurs à 0). `SkyLight` absent : règle de dérivation et moment d'application en R30. Sections hors de la hauteur du monde qui ne portent que de la lumière : R31. | S |

### Heightmaps

| Réf | Règle | Preuve |
|---|---|---|
| R16 | Types : `WORLD_SURFACE`, `WORLD_SURFACE_WG`, `OCEAN_FLOOR`, `OCEAN_FLOOR_WG`, `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`. Le Compound `Heightmaps` associe un nom de type à un Long_Array. | S |
| R17 | Encodage : 256 valeurs (une par colonne, index `lz * 16 + lx`), largeur `b = ceil(log2(height + 1))` bits, empaquetage de R8, donc `ceil(256 / floor(64 / b))` longs attendus. Pour les quatre types de dimension de 26.3 : 9 bits, 7 valeurs par long, 1 bit inutilisé, 37 longs. La longueur de 37 n'est donc pas une constante : un type de dimension d'une autre hauteur (jeu de données tiers) a une autre longueur attendue. | C sur S |
| R18 | Valeur stockée : **provisoire** `(Y du plus haut bloc retenu + 1) - min_y`, 0 si aucun bloc de la colonne n'est retenu ; plage 0 à `height` (0 à 384 dans l'Overworld, 384 = bloc retenu à Y 319). | S |
| R19 | Types présents. **Provisoire** : chunk complet -> `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`, `OCEAN_FLOOR`, `WORLD_SURFACE` ; proto-chunk -> au moins les types `_WG`. Type absent ou invalide (longueur, type NBT ou valeur hors plage, R34) : état INCONNU, "à recalculer" (le calcul, et les prédicats de blocs retenus par chaque type, relèvent de M5) ; d'ici là, aucune valeur n'est inventée et le type n'est pas réécrit à la sauvegarde. Comment l'appelant le sait : R34. | S |

### Schéma NBT d'un chunk (fichier `region/`)

Racine : Compound nommé de nom vide (`core-nbt.md` R14 et R15), après décompression (`world-anvil-region.md`). Types NBT de `core-nbt.md` ; Byte_Array (7) et Long_Array (12) y sont S et sont indispensables ici.

| Réf | Règle | Preuve |
|---|---|---|
| R20 | Champs de la racine, tous **provisoires** sauf la valeur de `DataVersion` (D, `version.json`) : `DataVersion` Int, 5023 pour un chunk écrit par 26.3 ; `xPos` Int et `zPos` Int, coordonnées absolues du chunk ; `yPos` Int, Y de la section la plus basse (-4 dans l'Overworld) ; `Status` String, statut de génération avec espace de noms (le nom passe à `status` en 26.4, hors cible) ; `LastUpdate` Long, tick de jeu de la dernière sauvegarde ; `InhabitedTime` Long, ticks cumulés de présence de joueurs ; `sections` List de Compound ; `Heightmaps` Compound (R16 à R19) ; `block_entities` List de Compound ; `block_ticks` et `fluid_ticks` List de Compound ; `PostProcessing` List de List de Short ; `structures` Compound. | S |
| R21 | Champs conditionnels, **provisoires** : `entities` List de Compound, seulement dans un proto-chunk (un chunk complet range ses entités dans `entities/`, voir `world-anvil-region.md`) ; `blending_data` Compound (`heights` List de 16 Double, `min_section` Int, `max_section` Int), pour le raccord avec un ancien terrain ; `below_zero_retrogen` Compound, chunks mis à niveau depuis l'ancienne hauteur ; `isLightOn` Byte et masques de creusement, non documentés pour 26.3. Le lecteur ignore toute clé inconnue ; l'écrivain n'écrit que les clés de R20 et celles de R21 qu'il sait produire. | S |
| R22 | Section : Compound avec `Y` Byte (Y de section, obligatoire, R32), `block_states` (conteneur de blocs), `biomes` (conteneur de biomes), `BlockLight` et `SkyLight` optionnels (R14, R15). À l'écriture, les sections sont rangées de bas en haut et la liste contient toutes les sections de la hauteur du monde, même vides (le wiki indique que toutes les sections de la hauteur du monde sont présentes, même celles remplies d'air). Section de la hauteur du monde sans `block_states` : **provisoire** air partout ; sans `biomes` : **provisoire** `minecraft:plains`. Liste incomplète, en double, dans le désordre ou avec des Y hors plage : R32. | S |
| R23 | `block_ticks` et `fluid_ticks` : chaque entrée a `i` String (identifiant de bloc ou de fluide), `p` Int (priorité, plus petit d'abord), `t` Int (délai en ticks, négatif si en retard), `x`, `y`, `z` Int (position absolue). | S |
| R24 | `PostProcessing` : une liste par section de la hauteur du monde (24 dans l'Overworld), de bas en haut : la liste de rang `k` désigne la section de Y `(min_y >> 4) + k`. Chaque Short désigne une position locale de la section. **Provisoire** : `lx | (ly << 4) | (lz << 8)`, soit l'ordre d'empaquetage `0ZYX` du wiki lu du quartet de poids fort au quartet de poids faible (z en haut, y au milieu, x en bas, les 4 bits de poids fort à 0 ; lecture cohérente avec R4, où `YZX` place `ly` en haut). Cet ordre diffère de celui de R4 dès que `ly` et `lz` diffèrent : (3, 5, 9) donne 2387 ici et 1427 pour l'index de bloc (vecteur PP1). Il est aussi celui de `core-math.md` R32. L'ancienne valeur de cette règle, `lx | (lz << 4) | (ly << 8)` ("même ordre que R4"), est abandonnée : elle contredisait le wiki, qui ne donne que les lettres `0ZYX`. Lecture : un Short dont les 4 bits de poids fort ne sont pas 0 est ignoré avec un avertissement ; moins de listes que de sections : les listes manquantes sont vides ; plus de listes : les listes en trop sont ignorées avec un avertissement (le wiki donne 24 listes pour `PostProcessing` et 16 pour `ToBeTicked`, sans les relier). Écriture : une liste par section de la hauteur du monde, vides comprises ; les Short d'une liste gardent l'ordre de lecture. | S |
| R25 | `structures` : Compound avec `starts` (Compound, un par structure dont le départ est dans ce chunk ; une structure terminée a `id` = `"INVALID"`) et les références (Compound : identifiant de structure -> Long_Array de positions de chunk). Nom exact de la clé des références : `References` ou `references`, **provisoire** `References` à l'écriture, les deux à la lecture. Position de chunk dans un long : `(cz << 32) | (cx & 0xFFFFFFFF)` (x dans les 32 bits de poids faible). | S |
| R26 | `block_entities` : chaque entrée est un Compound avec au moins `id` String et `x`, `y`, `z` Int absolus ; le reste est conservé tel quel (NBT opaque, ADR 0005). Une entrée hors du chunk : **provisoire**, ignorée avec un avertissement. | S |

### Lecture et écriture : décisions de la révision du 2026-10-28

Ces règles tranchent des ambiguïtés de R15, R19, R22 et R24 révélées par l'implémentation du squelette. Aucune ne vient d'une observation de l'oracle : toutes sont S, avec le comportement **provisoire** que le moteur applique. Leur justification et l'alternative écartée sont dans `world-decisions-log.md`.

| Réf | Règle | Preuve |
|---|---|---|
| R30 | `SkyLight` absent : moment d'application. L'absence est un état de la section, distinct de "présent, tout à 0" : elle est conservée telle quelle à la lecture (aucun tableau n'est matérialisé) et à l'écriture (un chunk lu puis réécrit sans modification redonne les mêmes sections avec et sans `SkyLight`). La lumière du ciel effective d'une section est une vue calculée à la demande, sans effet de bord, sur l'état courant du chunk : si `SkyLight` est présent, c'est ce tableau ; sinon, la vue de la section de Y + 1 si elle existe dans le chunk après R32 (section de lumière de R31 comprise), dont on prend la couche du bas (index 0 à 255) répétée 16 fois, une fois par `ly` ; si cette section n'existe pas, 15 partout (2048 octets `ff`). La dérivation est donc récursive vers le haut. Elle ne s'applique ni à la lecture (pas de remplissage), ni à l'écriture (un tableau déduit n'est jamais écrit). Le calcul de lumière de M5 qui modifie une section lui donne un `SkyLight` explicite. Le wiki dit seulement de se reporter aux données de lumière de la section juste au-dessus et de traiter la section du haut comme pleinement éclairée ; "couche du bas répétée" plutôt que "tableau entier recopié" est une interprétation, que le vecteur LI2 sépare (octet 128 : `10` ou `33`) et que la sonde PC7 observe. | S |
| R31 | Sections de lumière hors hauteur. Avec `sMin = min_y >> 4` et `sMax = ((min_y + height) >> 4) - 1` (Overworld -4 et 19 ; Nether et End 0 et 15), les sections de Y `sMin - 1` et `sMax + 1` (Overworld -5 et 20 ; Nether et End -1 et 16) sont des sections de lumière : le wiki indique qu'il peut y en avoir une en haut et/ou en bas du monde, utilisée pour stocker la lumière. **Provisoire** : le lecteur les accepte, avec ou sans `BlockLight` et `SkyLight` ; elles ne comptent pas parmi les sections de la hauteur du monde, ne portent aucun bloc ni biome (`block_states` et `biomes`, s'ils existent, sont ignorés : un bloc hors du monde reste de l'air) et ne servent qu'à la lumière (R30 pour celle du haut, M5 pour le reste). **Écriture** : une section de lumière lue est réécrite telle quelle (même Y, tous les champs de son Compound d'origine), à sa place de bas en haut ; le moteur n'en crée aucune. Un Y plus éloigné : R32. | S |
| R32 | Réconciliation de la liste `sections`, `sMin` et `sMax` de R31. **Provisoire** : (a) clé `sections` absente ou valeur qui n'est pas une List de Compound : chunk refusé (INVALID_FIELD, `world-anvil-region.md` R26) ; (b) entrée sans `Y`, ou dont `Y` n'est pas un Byte : chunk refusé (INVALID_SECTION), l'erreur donne le rang de l'entrée dans la liste ; (c) deux entrées de même Y, où qu'elles soient dans la liste : chunk refusé (INVALID_SECTION), l'erreur donne le Y ; (d) l'ordre des entrées est indifférent à la lecture, l'écriture range de bas en haut ; (e) Y dans `[sMin, sMax]` : section normale ; `sMin - 1` ou `sMax + 1` : R31 ; tout autre Y : entrée ignorée avec un avertissement, non réécrite ; (f) Y de la hauteur du monde absent de la liste : section vide synthétisée (air, `minecraft:plains`, `BlockLight` et `SkyLight` absents) avec un avertissement, le chunk reste présent, et la liste écrite à la sauvegarde suivante est complète ; (g) `sections` List vide : (f) pour toutes les sections (un seul avertissement). Principe : la lecture tolère ce qui est redondant ou reconstructible (sections vides, ordre, `yPos`), et refuse ce qui rendrait une position ambiguë (Y manquant ou en double) ou le chunk méconnaissable (liste absente). Le cas (g) tolère une liste vide explicite mais refuse l'absence de la clé : écart assumé, à revoir avec l'inventaire des statuts précoces de PC6. | S |
| R33 | `yPos`. **Provisoire** : le Y de la section la plus basse se déduit du type de dimension (`min_y >> 4`, R1 et R3) ; `yPos` n'est qu'une redondance. Absent : accepté, sans avertissement. Présent et différent de `min_y >> 4` : accepté avec un avertissement, la valeur lue est ignorée (les sections portent leur Y, R32). Présent et pas un Int : ignoré avec un avertissement. Écriture : toujours `min_y >> 4` du type de dimension du chunk. | S |
| R34 | État d'un type de heightmap (précise R19). Pour chacun des six types de R16, le chunk garde un état VALIDE (256 valeurs de 0 à `height`) ou INCONNU ("à recalculer"). **Provisoire** : un type est INCONNU s'il est absent ; présent mais pas un Long_Array ; d'un nombre de longs différent de celui de R17 (36, longueur de l'ancien empaquetage continu, est donc invalide dans l'Overworld) ; ou s'il contient une valeur décodée supérieure à `height` (385 à 511 tiennent sur 9 bits). Une clé qui n'est pas l'un des six types est ignorée et non réécrite. L'appelant le sait par trois canaux : (1) la consultation d'un type INCONNU rend un résultat explicite "inconnu", jamais un tableau de zéros ni une exception ; (2) le chunk expose l'ensemble de ses types INCONNU ; (3) la lecture ajoute un avertissement par type présent mais invalide (aucun pour un type simplement absent, normal dans un proto-chunk, R19). Le recalcul de M5 fait passer le type à VALIDE. Écriture : seuls les types VALIDE sont écrits, et la clé `Heightmaps` l'est toujours (Compound éventuellement vide) : un chunk lu avec un type INCONNU puis réécrit sans recalcul perd ce type, une perte assumée (donnée dérivable). | S (longueur attendue : C sur S) |
| R35 | Champs obligatoires de la racine (R20). **Provisoire** : `DataVersion` (Int) absent ou d'un autre type : chunk refusé (UNSUPPORTED_VERSION) ; `Status` (String) et `sections` (R32 a) absents ou d'un autre type : chunk refusé (INVALID_FIELD) ; `xPos` ou `zPos` (Int) absents ou d'un autre type : position invérifiable, traitée comme un désaccord de position, chunk absent (POSITION_MISMATCH, `world-anvil-region.md` R21). Tous les autres champs de R20 et R21 sont facultatifs : absent, ou d'un autre type que celui de R20 (avertissement dans ce cas), vaut `LastUpdate` et `InhabitedTime` à 0, listes vides, `structures` vide, heightmaps INCONNUES (R34). Les champs facultatifs d'un proto-chunk ou d'un chunk complet attendus par M4 et M5 ne sont pas encore listés : à reprendre avec PC6. | S |

### Statuts de génération

| Réf | Règle | Preuve |
|---|---|---|
| R27 | Le registre `minecraft:chunk_status` contient exactement 10 statuts, `protocol_id` : `empty` 0, `structure_starts` 1, `structure_references` 2, `biomes` 3, `terrain` 4, `features` 5, `initialize_light` 6, `light` 7, `spawn` 8, `full` 9 ; défaut `minecraft:empty`. | D |
| R28 | L'ordre de génération est l'ordre des `protocol_id` de R27. Seul `minecraft:full` désigne un chunk complet ; tout autre statut désigne un proto-chunk. | S |
| R29 | Les statuts `noise`, `surface` et `carvers` des versions précédentes n'existent plus (absents de R27 ; remplacés par `terrain` en 26.3 Snapshot 10 selon le wiki, changement absent du journal officiel). Un chunk qui en porte un vient d'une version antérieure : **provisoire**, refusé (mise à niveau hors périmètre). | D (absence) / S (historique) |

## Entrées / sorties

- **Lecture d'un chunk** : entrée = arbre NBT décodé (racine de R20) et type de dimension (R1) ; sortie = l'une des trois issues de `world-anvil-region.md` R26. PRÉSENT : chunk avec sections (R32), palettes résolues en états, lumière (R30, R31), heightmaps VALIDES ou INCONNUES (R34), données opaques (block entities, ticks, structures, post-traitement R24), statut, et la liste des avertissements. REFUSÉ, avec une cause imposée : R10 (INVALID_CONTAINER), R29 (UNSUPPORTED_STATUS), R32 a, b, c (INVALID_FIELD, INVALID_SECTION), R35, version. ABSENT : R35 pour `xPos` et `zPos`, et `world-anvil-region.md` R21. Les échecs de lecture du fichier lui-même (octets, compression, NBT) sont décrits dans `world-anvil-region.md`.
- **Avertissements** (liste portée par le résultat, sans effet sur l'issue) : R11 (biome inconnu), R24 (Short ou listes en trop), R32 e et f, R33, R34, R35 (type erroné d'un champ facultatif), `world-block-states.md` R21 et R22.
- **Version** : le moteur ne lit que `DataVersion` = 5023. Toute autre valeur : chunk refusé avec l'erreur "version non prise en charge" (cause UNSUPPORTED_VERSION, choix du moteur en M3, la mise à niveau est hors périmètre ; "refusé" est défini dans `world-anvil-region.md`).
- **Écriture** : arbre NBT de R20 à R26, R31 et R32 (liste de sections complète et rangée de bas en haut), R33 (`yPos`), R34 (heightmaps VALIDES seulement) ; palettes compactées (aucune entrée inutilisée), bits de R7, longs de R8. L'ordre des entrées de palette n'est pas imposé : les comparaisons avec l'oracle portent sur les états décodés par position, pas sur les longs.
- **Coordonnées** : entiers signés 32 bits pour `xPos`, `zPos` et les positions absolues ; aucune division vers zéro (R3).

## Cas limites

- Coordonnées négatives : bloc x = -1 -> chunk -1, local 15 ; x = -16 -> chunk -1, local 0 ; x = -17 -> chunk -2, local 15 (R3).
- Bords verticaux : y = -64 -> section -4, index 0 ; y = 319 -> section 19, local 15, index jusqu'à 4095 ; y = 320 et y = -65 sont hors du monde.
- Seuils de bits : palettes de 16/17, 32/33, 256/257 entrées (blocs) ; 2, 3, 4, 5, 8, 9 entrées (biomes) ; un long à 4 bits rempli de 15 vaut -1 en Long signé.
- Chevauchement : avec 5 bits, l'entrée 12 commence au bit 0 du long 1 (bits 60 à 63 du long 0 à zéro). Un empaquetage continu (format antérieur à 1.16) donnerait 320 longs au lieu de 342 et un autre long 1 (vecteur PK3).
- Heightmap à 384 (bloc retenu à Y 319) : tient sur 9 bits ; 385 ne peut pas apparaître.
- Section de lumière hors hauteur (R15) : présente sans `block_states`.
- Palette d'une seule entrée et `data` présent quand même : **provisoire**, `data` ignoré.
- `PostProcessing` : (15, 0, 15) donne 3855 (`0x0f0f`) et (0, 15, 0) donne 240 ; l'index de bloc de R4 donne 255 et 3840 pour les mêmes positions (R24, PP1).
- Section de Y 19 sans `SkyLight` : la section 20 (lumière) si elle existe, sinon 15 partout ; une section sans `SkyLight` sous une section dont la couche du bas n'est pas uniforme (vecteur LI2).
- Chunk lu puis réécrit sans modification : mêmes sections avec et sans `SkyLight` (R30), sections de lumière conservées (R31), `yPos` recalculé (R33), heightmaps INCONNUES absentes (R34).
- Liste de 23 sections (une manquante), de 25 (une en double), de Y -6 ou 21 en plus, dans le désordre, sans clé `sections` : R32 (vecteurs RG4).
- Heightmap de 36 longs, de 38 longs, ou contenant la valeur 400 : INCONNU (R34, vecteur HM4).

## Vecteurs de test

Voir `world-vectors.md` : coordonnées (CO1), empaquetage (PK1 à PK5), heightmaps (HM1 à HM4), lumière (LI1, LI2), `PostProcessing` (PP1), références de structures (ST1), chunks synthétiques et variantes altérées (RG1 à RG4), sondes PC1 à PC7, PB2, PB3.

## Questions ouvertes

Toutes les règles S ci-dessus attendent une sonde de l'oracle ; correspondance règle -> sonde (toutes les sondes appliquent l'attente W de chargement des chunks) :

- R2 à R4, R6 à R8, R20, R22 : PC1 (monde plat, structure complète d'un chunk réel, longueurs de `data`, contenu de la section -4 décodé position par position).
- R5, R11 (forme de la palette de biomes) : PC4 (`/fillbiome` sur une demi-section, motif attendu `0x3333333333333333` ou son complément selon l'ordre de palette).
- R7 au-delà de 8 bits, R9 : PC3 (261 états distincts dans une section, 9 bits et 586 longs attendus ; 17 entrées, 5 bits et 342 longs).
- R12, R13 : non observables sur disque ; R13 relève du jalon M8.
- R14, R15 : PC5 (bloc lumineux posé, demi-octets attendus) et PC1 (`SkyLight`, sections de lumière Y -5 et 20).
- R16 à R18 : PC1 (valeur 4 attendue dans un monde plat) ; R19 : PC6 (types présents selon le statut).
- R21, R23, R26, R28 : PC6 (monde normal, chunks de tous statuts, inventaire des clés et des statuts rencontrés).
- R24 : PC6, décodage des Short de `PostProcessing` selon les six ordres possibles des trois quartets (le provisoire `x|y<<4|z<<8` doit désigner le plus de blocs non air, avec des fluides) ; le wiki est la seule source, et elle ne donne que les lettres `0ZYX`. Si un autre ordre l'emporte, R24, `core-math.md` R32 (deuxième puce) et V4 changent ensemble.
- R30 : PC7 (sections sans `SkyLight` des mondes plat et normal, comparées à la couche du bas de la section au-dessus ; réécriture par le serveur d'un fichier `skylight_chain`). R31 : PB3, variantes `light_sections` et `far_sections`, puis `dump` après `save-all flush` (sections -5 et 20 conservées ou non). R32 et R33 : PB3, variantes `missing_section`, `duplicate_section`, `no_section_y`, `unsorted_sections`, `no_sections_key`, `ypos_wrong`, `ypos_absent`. R34 : PB3, variantes `bad_heightmap_length` et `heightmap_overflow` (le serveur régénère-t-il ou recalcule-t-il les heightmaps ?). R35 : PC6, inventaire des clés présentes selon le statut.
- R25 : PC6, décodage des longs de références (x dans les bits de poids faible, puis contre-épreuve), distance au chunk porteur et présence du départ correspondant ; nom exact `References` ou `references`.
- R10 : PB2, variantes `bad_length`, `empty_palette`, `index_overflow`.
- R11 (biome inconnu) : PB2, variante `unknown_biome`.
- R22 (section incomplète) : PB2, variantes `missing_block_states` et `missing_biomes`.
- R29 : aucune sonde (le serveur 26.3 n'écrit pas les anciens statuts) ; reste S tant qu'un monde d'une version antérieure n'a pas été observé.

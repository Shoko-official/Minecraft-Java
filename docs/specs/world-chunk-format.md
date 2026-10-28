# Chunks : sections, conteneurs à palette et schéma NBT

Statut : brouillon
Jalon : M3
Risque : B

## Périmètre

Couvre, pour `engine/world` :

- la géométrie : sections, hauteur par dimension, conversions de coordonnées (négatives comprises) ;
- les conteneurs à palette stockés sur disque (blocs 4096 entrées, biomes 64 entrées) : palette, bits par entrée, empaquetage dans les longs, ordre des index ;
- la lumière (tableaux de demi-octets) et les heightmaps (types, encodage) ;
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
- **Sondes de l'oracle** : aucune exécutée. Sondes PC1 à PC6 décrites dans `world-vectors.md`.

Niveaux : **D** valeur lue dans un fichier extrait ; **C** calcul reproductible (`world_ref.py`) à partir de règles D, ou à partir de règles S (la valeur calculée hérite alors du S, signalé "C sur S") ; **M** non utilisé ; **S** documentation publique seule (même concordante entre deux pages du wiki), rapport local ou hypothèse, avec comportement **provisoire**.

## Règles

### Géométrie et coordonnées

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Hauteurs par type de dimension : `overworld` et `overworld_caves` `min_y` -64, `height` 384 ; `the_nether` 0, 256 (`logical_height` 128, sans effet sur le stockage) ; `the_end` 0, 256. | D |
| R2 | Une section fait 16 x 16 x 16 blocs. Nombre de sections = `height / 16` : 24 pour l'Overworld (Y de section -4 à 19), 16 pour le Nether et l'End (0 à 15). | C |
| R3 | Conversions, toujours par décalage arithmétique et masque, jamais par division entière vers zéro : chunk `cx = x >> 4`, `cz = z >> 4` ; Y de section `sy = y >> 4` ; coordonnées locales `x & 15`, `y & 15`, `z & 15`. Rang de la section dans la liste : `sy - (min_y >> 4)`. Exemples : x = -1 -> chunk -1, local 15 ; x = -17 -> chunk -2, local 15 ; y = -64 -> section -4, local 0. | C |
| R4 | Index d'un bloc dans sa section : `(ly * 16 + lz) * 16 + lx`, de 0 à 4095 (x varie le plus vite, puis z, puis y). | S |
| R5 | Biomes en 26.3 : une section contient 4 x 4 x 4 = 64 cellules de 4 x 4 x 4 blocs. Index : `(qy * 4 + qz) * 4 + qx` avec `q = local >> 2`. | S |

### Conteneurs à palette sur disque

| Réf | Règle | Preuve |
|---|---|---|
| R6 | Un conteneur est un Compound à deux clés : `palette` (List, au moins une entrée) et `data` (Long_Array). Si la palette a une seule entrée, `data` est absent et l'entrée remplit tout le conteneur (palette "unique"). Sinon `data` porte les index dans la palette, un par position (4096 pour les blocs, 64 pour les biomes). | S |
| R7 | Bits par entrée sur disque, `n` = taille de la palette, `n > 1` : blocs `max(4, ceil(log2(n)))`, biomes `ceil(log2(n))` sans minimum. Bornes : `n <= 4096` pour les blocs (12 bits au plus), `n <= 64` pour les biomes (6 bits au plus). Le nombre de bits n'est pas stocké : il se déduit de `n`. | S |
| R8 | Empaquetage : `p = floor(64 / bits)` entrées par long. L'entrée `i` est dans le long `floor(i / p)`, aux bits `bits * (i mod p)` à `bits * (i mod p) + bits - 1`, comptés depuis le bit de poids faible. Une entrée ne chevauche jamais deux longs ; les bits de poids fort inutilisés valent 0 à l'écriture et sont ignorés à la lecture (provisoire). Nombre de longs : `ceil(count / p)`. Les longs sont des Long NBT signés (gros-boutistes, `core-nbt.md` R3) : seul le motif de bits compte. | S |
| R9 | Conséquences chiffrées de R7 et R8 (table complète en BS/PK1 de `world-vectors.md`) : blocs 4 bits -> 256 longs, 5 bits -> 342, 6 -> 410, 8 -> 512, 9 -> 586, 12 -> 820 ; biomes 1 bit -> 1 long, 2 -> 2, 3 -> 4, 6 -> 7. Seuils : palette de 16 -> 4 bits, 17 -> 5, 33 -> 6, 256 -> 8, 257 -> 9. | C sur S |
| R10 | Données incohérentes : longueur de `data` différente de R9, index supérieur ou égal à `n`, palette vide. **Provisoire** : le chunk est refusé à la lecture (erreur portant la section et le conteneur), sans réécriture du fichier. | S |
| R11 | Palette de blocs : entrées selon `world-block-states.md` R19 à R21. Palette de biomes : **provisoire** List de String (identifiants de biomes). Le wiki montre chaque entrée sous l'étiquette `Name` sans préciser si c'est une String ou un Compound ; le lecteur accepte aussi un Compound `{Name}`. Biome inconnu : **provisoire** `minecraft:plains` avec un avertissement. | S |
| R12 | Types de palette. Sur disque, seules deux formes sont observables : unique (R6, pas de `data`) et indexée (palette locale + index). Les distinctions "linéaire", "hachée" et "globale" ne concernent que la représentation en mémoire et le réseau : le moteur choisit librement sa représentation en mémoire, à condition de produire R6 à R8 sur disque. | S |
| R13 | Réseau (jalon M8, pour mémoire) : palette unique 0 bit ; indirecte 4 à 8 bits pour les blocs, 1 à 3 pour les biomes ; directe au-delà, avec les identifiants globaux. Largeur directe : 16 bits pour les blocs (35 723 états, `world-block-states.md` R12) et 7 bits pour les biomes (67 biomes, `ceil(log2(67))`). | C sur S |

### Lumière

| Réf | Règle | Preuve |
|---|---|---|
| R14 | `BlockLight` et `SkyLight` sont des Byte_Array de 2048 octets : 4096 valeurs de 0 à 15, deux par octet, index de R4. Index pair -> 4 bits de poids faible de l'octet `index >> 1` ; index impair -> 4 bits de poids fort. | S |
| R15 | Présence. `BlockLight` absent : aucune lumière de bloc dans la section. `SkyLight` absent : **provisoire**, la section prend la couche du bas de la section au-dessus répétée 16 fois, ou 15 partout s'il n'y a pas de section au-dessus. Des sections hors de la hauteur du monde (Y de section `min - 1` et `max + 1`) peuvent exister pour ne porter que de la lumière : **provisoire**, le lecteur les accepte et ne les utilise que pour la lumière. | S |

### Heightmaps

| Réf | Règle | Preuve |
|---|---|---|
| R16 | Types : `WORLD_SURFACE`, `WORLD_SURFACE_WG`, `OCEAN_FLOOR`, `OCEAN_FLOOR_WG`, `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`. Le Compound `Heightmaps` associe un nom de type à un Long_Array. | S |
| R17 | Encodage : 256 valeurs (une par colonne, index `lz * 16 + lx`), largeur `ceil(log2(height + 1))` bits, empaquetage de R8. Pour les quatre types de dimension de 26.3 : 9 bits, 7 valeurs par long, 1 bit inutilisé, 37 longs. | C sur S |
| R18 | Valeur stockée : **provisoire** `(Y du plus haut bloc retenu + 1) - min_y`, 0 si aucun bloc de la colonne n'est retenu ; plage 0 à `height` (0 à 384 dans l'Overworld, 384 = bloc retenu à Y 319). | S |
| R19 | Types présents. **Provisoire** : chunk complet -> `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`, `OCEAN_FLOOR`, `WORLD_SURFACE` ; proto-chunk -> au moins les types `_WG`. Type absent à la lecture : recalculé à partir des blocs. Longueur différente de 37 : traité comme absent. | S |

### Schéma NBT d'un chunk (fichier `region/`)

Racine : Compound nommé de nom vide (`core-nbt.md` R14 et R15), après décompression (`world-anvil-region.md`). Types NBT de `core-nbt.md` ; Byte_Array (7) et Long_Array (12) y sont S et sont indispensables ici.

| Réf | Règle | Preuve |
|---|---|---|
| R20 | Champs de la racine, tous **provisoires** sauf la valeur de `DataVersion` (D, `version.json`) : `DataVersion` Int, 5023 pour un chunk écrit par 26.3 ; `xPos` Int et `zPos` Int, coordonnées absolues du chunk ; `yPos` Int, Y de la section la plus basse (-4 dans l'Overworld) ; `Status` String, statut de génération avec espace de noms (le nom passe à `status` en 26.4, hors cible) ; `LastUpdate` Long, tick de jeu de la dernière sauvegarde ; `InhabitedTime` Long, ticks cumulés de présence de joueurs ; `sections` List de Compound ; `Heightmaps` Compound (R16 à R19) ; `block_entities` List de Compound ; `block_ticks` et `fluid_ticks` List de Compound ; `PostProcessing` List de List de Short ; `structures` Compound. | S |
| R21 | Champs conditionnels, **provisoires** : `entities` List de Compound, seulement dans un proto-chunk (un chunk complet range ses entités dans `entities/`, voir `world-anvil-region.md`) ; `blending_data` Compound (`heights` List de 16 Double, `min_section` Int, `max_section` Int), pour le raccord avec un ancien terrain ; `below_zero_retrogen` Compound, chunks mis à niveau depuis l'ancienne hauteur ; `isLightOn` Byte et masques de creusement, non documentés pour 26.3. Le lecteur ignore toute clé inconnue ; l'écrivain n'écrit que les clés de R20 et celles de R21 qu'il sait produire. | S |
| R22 | Section : Compound avec `Y` Byte (Y de section), `block_states` (conteneur de blocs), `biomes` (conteneur de biomes), `BlockLight` et `SkyLight` optionnels (R14, R15). Les sections sont rangées de bas en haut ; un chunk complet contient toutes les sections de la hauteur du monde, même vides. Section de la hauteur du monde sans `block_states` : **provisoire** air partout ; sans `biomes` : **provisoire** `minecraft:plains`. | S |
| R23 | `block_ticks` et `fluid_ticks` : chaque entrée a `i` String (identifiant de bloc ou de fluide), `p` Int (priorité, plus petit d'abord), `t` Int (délai en ticks, négatif si en retard), `x`, `y`, `z` Int (position absolue). | S |
| R24 | `PostProcessing` : une liste par section de la hauteur du monde (24 dans l'Overworld), de bas en haut ; chaque Short désigne une position locale de la section. **Provisoire** : `lx | (lz << 4) | (ly << 8)`, même ordre que R4. | S |
| R25 | `structures` : Compound avec `starts` (Compound, un par structure dont le départ est dans ce chunk ; une structure terminée a `id` = `"INVALID"`) et les références (Compound : identifiant de structure -> Long_Array de positions de chunk). Nom exact de la clé des références : `References` ou `references`, **provisoire** `References` à l'écriture, les deux à la lecture. Position de chunk dans un long : `(cz << 32) | (cx & 0xFFFFFFFF)` (x dans les 32 bits de poids faible). | S |
| R26 | `block_entities` : chaque entrée est un Compound avec au moins `id` String et `x`, `y`, `z` Int absolus ; le reste est conservé tel quel (NBT opaque, ADR 0005). Une entrée hors du chunk : **provisoire**, ignorée avec un avertissement. | S |

### Statuts de génération

| Réf | Règle | Preuve |
|---|---|---|
| R27 | Le registre `minecraft:chunk_status` contient exactement 10 statuts, `protocol_id` : `empty` 0, `structure_starts` 1, `structure_references` 2, `biomes` 3, `terrain` 4, `features` 5, `initialize_light` 6, `light` 7, `spawn` 8, `full` 9 ; défaut `minecraft:empty`. | D |
| R28 | L'ordre de génération est l'ordre des `protocol_id` de R27. Seul `minecraft:full` désigne un chunk complet ; tout autre statut désigne un proto-chunk. | S |
| R29 | Les statuts `noise`, `surface` et `carvers` des versions précédentes n'existent plus (absents de R27 ; remplacés par `terrain` en 26.3 Snapshot 10 selon le wiki, changement absent du journal officiel). Un chunk qui en porte un vient d'une version antérieure : **provisoire**, refusé (mise à niveau hors périmètre). | D (absence) / S (historique) |

## Entrées / sorties

- **Lecture d'un chunk** : entrée = arbre NBT décodé (racine de R20) et type de dimension (R1) ; sortie = chunk avec sections, palettes résolues en états, lumière, heightmaps, données opaques (block entities, ticks, structures, post-traitement), statut ; ou erreur de format qui nomme le champ fautif.
- **Version** : le moteur ne lit que `DataVersion` = 5023. Toute autre valeur donne une erreur "version non prise en charge" (choix du moteur en M3, la mise à niveau est hors périmètre).
- **Écriture** : arbre NBT de R20 à R26 ; palettes compactées (aucune entrée inutilisée), bits de R7, longs de R8. L'ordre des entrées de palette n'est pas imposé : les comparaisons avec l'oracle portent sur les états décodés par position, pas sur les longs.
- **Coordonnées** : entiers signés 32 bits pour `xPos`, `zPos` et les positions absolues ; aucune division vers zéro (R3).

## Cas limites

- Coordonnées négatives : bloc x = -1 -> chunk -1, local 15 ; x = -16 -> chunk -1, local 0 ; x = -17 -> chunk -2, local 15 (R3).
- Bords verticaux : y = -64 -> section -4, index 0 ; y = 319 -> section 19, local 15, index jusqu'à 4095 ; y = 320 et y = -65 sont hors du monde.
- Seuils de bits : palettes de 16/17, 32/33, 256/257 entrées (blocs) ; 2, 3, 4, 5, 8, 9 entrées (biomes) ; un long à 4 bits rempli de 15 vaut -1 en Long signé.
- Chevauchement : avec 5 bits, l'entrée 12 commence au bit 0 du long 1 (bits 60 à 63 du long 0 à zéro). Un empaquetage continu (format antérieur à 1.16) donnerait 320 longs au lieu de 342 et un autre long 1 (vecteur PK3).
- Heightmap à 384 (bloc retenu à Y 319) : tient sur 9 bits ; 385 ne peut pas apparaître.
- Section de lumière hors hauteur (R15) : présente sans `block_states`.
- Palette d'une seule entrée et `data` présent quand même : **provisoire**, `data` ignoré.

## Vecteurs de test

Voir `world-vectors.md` : coordonnées (CO1), empaquetage (PK1 à PK5), heightmaps (HM1 à HM3), lumière (LI1), références de structures (ST1), chunks synthétiques du fichier de région (RG1), sondes PC1 à PC6.

## Questions ouvertes

Toutes les règles S ci-dessus attendent une sonde de l'oracle ; correspondance règle -> sonde :

- R4, R6 à R8, R20, R22 : PC1 (monde plat, structure complète d'un chunk réel, longueurs de `data`, contenu de la section -4).
- R5, R11 (biomes) : PC4 (`/fillbiome` sur une demi-section, motif attendu `0x3333333333333333` ou son complément selon l'ordre de palette).
- R7 au-delà de 8 bits : PC3 (261 états distincts dans une section, 9 bits et 586 longs attendus).
- R14, R15 : PC5 (bloc lumineux posé, demi-octets attendus).
- R16 à R19 : PC1 (valeur 4 attendue dans un monde plat) et PC6 (types présents selon le statut).
- R21, R23 à R26, R28 : PC6 (monde normal, chunks de tous statuts, inventaire des clés et des statuts rencontrés).
- R10, R11 (biome inconnu), R22 (section incomplète) : PB2 (chunks altérés relus par le serveur).
- R25 : nom exact `References` ou `references`, PC6.

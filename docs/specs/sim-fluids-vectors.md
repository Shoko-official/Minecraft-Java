# Vecteurs et sondes : fluides

Complète `sim-fluids.md`. Les vecteurs SFV sont des calculs sur les données extraites ou sur les règles (preuve D ou C sur S). Les sondes SF sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée par le propriétaire). Harnais SK0, canaux O1, O3, O4 et annexe `sim_ref.py` : `sim-ticks-updates-vectors.md`.

## Outil et commandes

`REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`.

| Sous-commande | Sortie | Empreinte | Vecteur |
|---|---|---|---|
| `tags` | tailles directes et résolues de 58 étiquettes (blocs, fluides, événements de jeu) | 421e04a44c01fd4a | SFV1 |
| `props` | propriétés de 78 blocs choisis | 602def4ad2e6590c | SFV2 |
| `propscan` | effectifs de `level`, `waterlogged`, `drag`... | ac278bdf0a53564a | SFV3 |
| `dims` | types de dimension et attributs `fast_lava`, `water_evaporates` | 26744735e26248d5 | SFV4 |
| `rules` | gamerules M5 | 5e9475808ba24ed4 | SFV5 |
| `floodwater` | cellules par distance sur sol plat | bf49316d0b095508 | SFV6 |
| `fluidclasses` | recouvrements des étiquettes `replaceable`, `washed_away_by_fluids`, `blocks_fluid_flow`, `impermeable` | a615b0daa1f8012c | SFV7 |

## SFV : vecteurs calculés

**SFV1 (R1, R4) : étiquettes.** Extrait de `tags` :

```
block/air direct=3 resolved=3 first=['minecraft:air', 'minecraft:cave_air', 'minecraft:void_air']
block/replaceable direct=30 resolved=30 first=['minecraft:air', 'minecraft:bubble_column', 'minecraft:bush']
block/washed_away_by_fluids direct=134 resolved=254 first=['minecraft:acacia_button', 'minecraft:acacia_sapling', 'minecraft:activator_rail']
block/impermeable direct=19 resolved=19 first=['minecraft:barrier', 'minecraft:black_stained_glass', 'minecraft:blue_stained_glass']
block/blocks_fluid_flow direct=2 resolved=1026 first=['minecraft:acacia_door', 'minecraft:acacia_fence', 'minecraft:acacia_fence_gate']
block/all_signs direct=2 resolved=52 first=['minecraft:acacia_hanging_sign', 'minecraft:acacia_sign', 'minecraft:acacia_wall_hanging_sign']
fluid/water direct=2 resolved=2 first=['minecraft:flowing_water', 'minecraft:water']
fluid/lava direct=2 resolved=2 first=['minecraft:flowing_lava', 'minecraft:lava']
fluid/supports_sugar_cane_adjacently direct=1 resolved=2 first=['minecraft:flowing_water', 'minecraft:water']
fluid/supports_lily_pad direct=1 resolved=1 first=['minecraft:water']
fluid/bubble_column_can_occupy direct=1 resolved=1 first=['minecraft:water']
```

**SFV2 (R2) : blocs de fluide.** Extrait de `props` :

```
water states=16 level=0..15(16)
lava states=16 level=0..15(16)
sea_pickle states=8 pickles={1,2,3,4} waterlogged={true,false}
bubble_column states=2 drag={true,false}
water_cauldron states=3 level={1,2,3}
oak_stairs states=80 facing={north,south,west,east} half={top,bottom} shape=straight..outer_right(5) waterlogged={true,false}
oak_slab states=6 type={top,bottom,double} waterlogged={true,false}
oak_trapdoor states=64 facing={north,south,west,east} half={top,bottom} open={true,false} powered={true,false} waterlogged={true,false}
```

**SFV3 (R2, R3) : effectifs.** Extrait de `propscan` :

```
level blocks=6 value_sets=['{1,2,3}', '0..8(9)', '0..15(16)']
waterlogged blocks=505 value_sets=['{true,false}']
drag blocks=1 value_sets=['{true,false}']
```

**SFV4 (R5) : attributs.** Lignes `attribute` de `dims` :

```
  attribute minecraft:gameplay/fast_lava true
  attribute minecraft:gameplay/sky_light_level 4.0
  attribute minecraft:gameplay/snow_golem_melts true
  attribute minecraft:gameplay/water_evaporates true
the_end skybox "end" visual sky attributes {'minecraft:visual/sky_light_color': '#ac60cd', 'minecraft:visual/sky_light_factor': 0.0}
```

**SFV5 (R6) : gamerules.** Extrait de `rules` :

```
game_rule registry entries 59
water_source_conversion protocol_id=58 parser=brigadier:bool props={}
lava_source_conversion protocol_id=19 parser=brigadier:bool props={}
```

**SFV6 (R10) : nappe sur sol plat** (C sur S : prémisses R9, R10, R15). Sortie de `floodwater` :

```
water reach 7: {0: 1, 1: 4, 2: 8, 3: 12, 4: 16, 5: 20, 6: 24, 7: 28} total 113
lava reach 3 (levels 2,4,6): {0: 1, 1: 4, 2: 8, 3: 12} total 25
lava reach 4 (alternative reading of the public infobox): {0: 1, 1: 4, 2: 8, 3: 12, 4: 16} total 41
nether lava reach 7: {0: 1, 1: 4, 2: 8, 3: 12, 4: 16, 5: 20, 6: 24, 7: 28} total 113
```

Lecture : pour l'eau, 1 source, 4 cellules à la distance 1, 8 à la distance 2, ... 28 à la distance 7, soit 113 cellules ; pour la lave de l'Overworld, 25 cellules (portée 3) ou 41 (portée 4, H-F1).

**SFV7 (R4, R21a, R21b) : classes de blocs prédites par les étiquettes.** Sortie de `fluidclasses` :

```
air resolved 3 unknown_to_registry 0
replaceable resolved 30 unknown_to_registry 0
washed_away_by_fluids resolved 254 unknown_to_registry 0
blocks_fluid_flow resolved 1026 unknown_to_registry 0
impermeable resolved 19 unknown_to_registry 0
washed and blocks_fluid_flow 1 replaceable and blocks_fluid_flow 0 washed and replaceable 28
washed and blocks_fluid_flow examples ['minecraft:straw_bed']
replaceable only (not washed) ['minecraft:bubble_column', 'minecraft:structure_void']
impermeable and blocks_fluid_flow 19
blocks in none of air, replaceable, washed_away, blocks_fluid_flow 5 of 1286
examples ['minecraft:end_gateway', 'minecraft:end_portal', 'minecraft:ladder', 'minecraft:nether_portal', 'minecraft:sugar_cane']
```

## Montages communs

Mêmes montages que `sim-light-vectors.md` (SL1) pour l'isolation ; en plus :

- **M-FLOOR(n)** : plancher de pierre de (2n + 1) x (2n + 1) positions à Y 59, entouré d'un rebord de pierre de 2 blocs de haut, centre (cx, 59, cz) ; air au-dessus. n vaut 10 par défaut (ni la portée 7 ni la zone d'étalement ne touchent le rebord).
- **Observation d'une nappe** : à chaque pas, `execute if blocks` d'une zone de référence contre la zone observée ou SNAP de la zone ; pour un décompte par distance, lecture de `block_states` du chunk (propriétés `level`) à la distance de taxi de la source.
- **Dimensions** : `execute in minecraft:the_nether run <commande>` pour le Nether ; plancher et rebord reconstruits par `fill` (le plancher naturel n'est pas utilisé).

## Sondes SF

**SF1 (R8) : niveaux 9 à 15.** M-FLOOR(10). Pour n de 0 à 15 et pour l'eau et la lave : `S(c, water[level=n])` (puis lave) au centre, STEP(0), 1, 5, 10, 50, 100 avec SNAP ; consigner l'état de la cellule, de ses quatre voisins et des cellules en dessous (blocs et `level`). Critère de succès : table (fluide, n, pas, états) complète ; pour n de 9 à 15 un des comportements {identique à 8, identique à 0, disparition, autre} est identifié et reproductible sur deux passages ; la règle R8 est réécrite avec lui.

**SF2 (R11) : délais.** M-FLOOR(10), source d'eau posée en (cx, 60, cz) à un tick connu. SNAP à chaque tick (de 0 à 40) : entrée `fluid_ticks` (`t`) et blocs de l'anneau 1, 2, 3. Mêmes mesures pour la lave dans l'Overworld (jusqu'à 120 ticks) et dans le Nether (jusqu'à 40). Critère de succès : première apparition de l'anneau k au tick k fois le délai (5, 30, 10) et `t` initial égal au délai ; si le décompte diffère d'un tick, la valeur observée remplace R11.

**SF3 (R7, R9, R10, R15) : nappe sur sol plat.** M-FLOOR(10), une source ; attendre la stabilisation (150 ticks pour l'eau, 400 pour la lave de l'Overworld, 120 pour la lave du Nether) ; SNAP ; lecture de `level` par cellule et décompte par distance de taxi. Critère de succès : décomptes égaux à SFV6 pour la portée relevée ; `level` égal à la distance pour l'eau et la lave du Nether, à 2 fois la distance pour l'autre lave ; si la portée de l'Overworld est 4 (H-F1), la ligne « 41 » de SFV6 s'applique.

**SF4 (R7, R13, R14) : chute.** M-FLOOR(10), source en (cx, 70, cz) en l'air (10 blocs au-dessus du sol), SNAP à chaque tick pendant 100 ticks : positions et `level` des cellules. Critère de succès : la colonne se remplit de haut en bas, `level` 8 sous la source, l'anneau horizontal n'apparaît qu'après le contact du sol ; niveaux de l'anneau 1 du pied (1 pour l'eau, 2 pour la lave de l'Overworld) ; portée au sol égale à celle de SF3. Mêmes mesures pour la lave.

**SF5 (R16) : recherche d'une chute.** M-FLOOR(10), source en (cx, 60, cz) ; un trou d'une cellule de profondeur 3 (air) à la distance d de 1 à 6 dans la direction +x ; deuxième trou à la distance d + 2 en -z (lui n'est pas le plus proche) ; STEP(40 pour l'eau, 120 pour la lave de l'Overworld) ; directions où le fluide s'est étalé au premier anneau. Même mesure pour la lave du Nether. Critère de succès : le fluide ne part que vers +x quand d est dans le rayon relevé ; part dans les quatre directions quand aucun trou n'est dans le rayon ; rayon relevé (4 ? 5 ?) consigné pour l'eau, la lave de l'Overworld, la lave du Nether.

**SF6 (R15, R18, R19, R20) : sources.** M-FLOOR(10) : (a) deux sources d'eau opposées avec une cellule d'air entre elles, sol plein : état du centre après STEP(15) ; (b) même chose sur plancher d'air (trou) ; (c) même chose sur une source d'eau ; (d) `gamerule water_source_conversion false` puis (a) ; (e) deux sources de lave, `lava_source_conversion true`, puis (f) avec `false` ; (g) une cellule coulante avec une source horizontale et une source au-dessus. Pour R15 : bassin à marche de 6 x 6 avec une marche de hauteur 1 : niveaux relevés. Critère de succès : (a) source ; (b) coulante ; (c) source ; (d) coulante ; (e) source ; (f) coulante ; (g) état consigné ; niveaux du bassin à marche égaux à distance de taxi bornée par la portée.

**SF7 (R25) : retrait d'une source.** SF3 stabilisée, puis `S(source, air)` ; SNAP à chaque tick jusqu'à l'absence de fluide ; décompte des cellules par distance. Mêmes mesures pour la lave dans l'Overworld et le Nether. Critère de succès : trace consignée ; la disparition totale arrive à un tick répétable sur 5 passages.

**SF8 (R26a à R26d) : mélange.** Cas : (1) lave source, eau placée par le côté, par le haut ; (2) lave coulante, eau par le côté ; (3) eau source, lave coulante qui descend dessus ; (4) lave sur terre d'âme avec glace bleue à côté ; (5) terre d'âme seule, (6) glace bleue seule ; chaque cas avec les deux ordres de pose (eau d'abord, lave d'abord), les fluides posés par `S` à la main ou par écoulement depuis une source distante. SNAP à STEP(30). Critère de succès : bloc obtenu au point de rencontre égal à obsidienne (1), pavé (2), pierre (3), basalte (4) et inchangé pour (5), (6), sans dépendre de l'ordre de pose ; tout cas divergent est consigné avec ses coordonnées.

**SF9 (R21a, R21b, R22) : classes de blocs.** Pour chacun des 1286 blocs (état par défaut) : M-FLOOR avec une source d'eau en (0, 60, 0), le bloc X en (1, 60, 0) posé en mode `strict`, STEP(15) ; relever (a) X toujours en place, (b) cellule remplacée par de l'eau, (c) nombre d'entités `item` apparues (`execute store result ... run execute if entity @e[type=item]`) ; même balayage avec une source de lave dans l'Overworld. Critère de succès : classe de chaque bloc égale à la prédiction de R21a (SFV7), taux d'accord relevé, écarts listés ; classe des 34 blocs de R21b consignée ; pour la lave aucun item.

**SF10 (R23, R24a à R24c) : immersion.** (a) Pour chacun de dalle (bas, haut, double), escalier (4 orientations, 2 moitiés), trappe (ouverte, fermée, 4 orientations), clôture, échelle, chaîne, lanterne, panneau, barres de fer : source d'eau voisine par chacun des 4 côtés et par le haut, STEP(30), état `waterlogged` obtenu. (b) Bloc déjà immergé voisin d'une cellule d'air : l'eau en sort-elle, et par quelles faces ? (c) Les 19 blocs de l'étiquette `impermeable` et la pierre : même mesure. Critère de succès : table (bloc, face d'entrée, `waterlogged` obtenu) complète ; escalier et trappe : face carrée bloquante ; (c) tout écart avec la pierre consigné.

**SF11 (R27, R28) : Nether.** `execute in minecraft:the_nether run` pour tout. (a) lave : SF2 et SF3 dans le Nether ; contrôle dans l'Overworld. (b) `setblock minecraft:water` posé dans le Nether : état après STEP(0), 1, 5, 20. Critère de succès : (a) délai 10 et portée 7 dans le Nether, 30 et 3 ou 4 dans l'Overworld ; (b) l'eau reste, s'étale ou disparaît : observation consignée.

**SF12 (R12, R17) : stabilisation.** SF3 stabilisée ; SNAP : `fluid_ticks` vides ; un bloc de pierre posé contre la nappe : `fluid_ticks` ; un bloc retiré sous le rebord : écoulement relancé. Critère de succès : file vide à la stabilisation, entrées présentes après le bloc ajouté (R12), plus de changement après 100 ticks (R17) ; bassin plein de sources : aucun tick.

**SF13 (R29) : bordure de chunk.** Source à 3 blocs de la bordure du chunk (0, 0) vers (1, 0) ; chunk (1, 0) déchargé (`forceload remove`) ; STEP(60) ; SNAP de (0, 0) et (1, 0) ; `forceload add` de (1, 0) ; STEP(60) ; SNAP. Variante : chunk chargé (contrôle). Critère de succès : aucun bloc écrit dans (1, 0) tant qu'il est déchargé ; reprise après rechargement (ou observation consignée) ; contrôle : écoulement identique à SF3 de part et d'autre de la bordure.

## Récapitulatif

- Sondes : SF1 à SF13 ; vecteurs SFV1 à SFV7.
- Hypothèses nommées : H-F1 (portée de la lave de l'Overworld), H-F2 (niveau au pied d'une chute), H-F3 (rayon de recherche), H-F4 (variante source au-dessus), H-F5 (bordure de chunk non chargé), H-F6 (disparition d'une cellule sans alimentation), H-F7 (niveaux 9 à 15 traités comme 8).
- Prérequis : EULA acceptée, lecteur `.mca` indépendant, harnais SK0, accès au Nether pour SF11.

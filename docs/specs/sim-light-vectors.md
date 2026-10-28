# Vecteurs et sondes : lumière

Complète `sim-light.md`. Les vecteurs SLV sont des calculs sur les données extraites ou sur les règles (preuve D ou C sur S). Les sondes SL sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée par le propriétaire). Le harnais commun SK0, les canaux d'observation O1, O3, O4 et l'annexe `sim_ref.py` sont dans `sim-ticks-updates-vectors.md`.

## Outil et commandes

`REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`. Commandes `python -I sim_ref.py REF <sous-commande>` ou `python -I sim_ref.py - <sous-commande>` ; texte du script et son SHA-256 dans `sim-ticks-updates-vectors.md`.

| Sous-commande | Sortie | Empreinte | Vecteur |
|---|---|---|---|
| `dims` | 4 types de dimension (champs utiles à la lumière), attributs de dimension, piste du ciel de `minecraft:day` | 26744735e26248d5 | SLV1 |
| `props` | bloc `light`, lampes, torches, etc. (état par bloc) | 602def4ad2e6590c | SLV2 |
| `propscan` | effectifs des propriétés `lit`, `candles`, `pickles`, `charges`, `berries`, `bloom`... | ac278bdf0a53564a | SLV3 |
| `lightball` | positions éclairées par niveau pour une source seule en espace libre | a87c56dab5d0d42b | SLV4 |

## SLV : vecteurs calculés

**SLV1 (R1, R2, R5, R6) : dimensions et piste du ciel.** Sortie de `dims` :

```
overworld has_skylight=true has_ceiling=false min_y=-64 height=384 logical_height=384 infiniburn="#minecraft:infiniburn_overworld" ambient_light=0.0 monster_spawn_block_light_limit=0 monster_spawn_light_level={"max_inclusive": 7, "min_inclusive": 0, "type": "minecraft:uniform"}
overworld_caves has_skylight=true has_ceiling=true min_y=-64 height=384 logical_height=384 infiniburn="#minecraft:infiniburn_overworld" ambient_light=0.0 monster_spawn_block_light_limit=0 monster_spawn_light_level={"max_inclusive": 7, "min_inclusive": 0, "type": "minecraft:uniform"}
the_nether has_skylight=false has_ceiling=true min_y=0 height=256 logical_height=128 infiniburn="#minecraft:infiniburn_nether" ambient_light=0.1 monster_spawn_block_light_limit=15 monster_spawn_light_level=7
  attribute minecraft:gameplay/fast_lava true
  attribute minecraft:gameplay/sky_light_level 4.0
  attribute minecraft:gameplay/snow_golem_melts true
  attribute minecraft:gameplay/water_evaporates true
the_end has_skylight=true has_ceiling=false min_y=0 height=256 logical_height=256 infiniburn="#minecraft:infiniburn_end" ambient_light=0.25 monster_spawn_block_light_limit=0 monster_spawn_light_level=15
the_end skybox "end" visual sky attributes {'minecraft:visual/sky_light_color': '#ac60cd', 'minecraft:visual/sky_light_factor': 0.0}
timeline day clock minecraft:overworld period_ticks 24000
  track sky_light_level modifier multiply ease None
  keyframes [(133, 1.0), (11867, 1.0), (13670, 0.26666668), (22330, 0.26666668)]
  markers [('minecraft:day', 1000), ('minecraft:midnight', 18000), ('minecraft:night', 13000), ('minecraft:noon', 6000), ('minecraft:roll_village_siege', 18000), ('minecraft:wake_up_from_sleep', 0)]
  other gameplay tracks ['minecraft:gameplay/bees_stay_in_hive', 'minecraft:gameplay/cat_waking_up_gift_chance', 'minecraft:gameplay/creaking_active', 'minecraft:gameplay/eyeblossom_open', 'minecraft:gameplay/monsters_burn', 'minecraft:gameplay/sky_light_level', 'minecraft:gameplay/turtle_egg_hatch_chance']
```

**SLV2 (R3) : bloc `light` et émetteurs à état.** Extrait de `props` :

```
light states=32 level=0..15(16) waterlogged={true,false}
redstone_torch states=2 lit={true,false}
redstone_wall_torch states=8 facing={north,south,west,east} lit={true,false}
redstone_lamp states=2 lit={true,false}
copper_bulb states=4 lit={true,false} powered={true,false}
sea_pickle states=8 pickles={1,2,3,4} waterlogged={true,false}
candle states=16 candles={1,2,3,4} lit={true,false} waterlogged={true,false}
campfire states=32 facing={north,south,west,east} lit={true,false} signal_fire={true,false} waterlogged={true,false}
redstone_ore states=2 lit={true,false}
respawn_anchor states=5 charges=0..4(5)
sculk_catalyst states=2 bloom={true,false}
```

**SLV3 (R4) : effectifs des propriétés.** Extrait de `propscan` :

```
level blocks=6 value_sets=['{1,2,3}', '0..8(9)', '0..15(16)']
lit blocks=52 value_sets=['{true,false}']
waterlogged blocks=505 value_sets=['{true,false}']
candles blocks=17 value_sets=['{1,2,3,4}']
pickles blocks=1 value_sets=['{1,2,3,4}']
charges blocks=1 value_sets=['0..4(5)']
berries blocks=2 value_sets=['{true,false}']
bloom blocks=1 value_sets=['{true,false}']
```

**SLV4 (R13) : boule de lumière en espace libre** (C sur S : prémisse R12). Sortie de `lightball` :

```
emitter 15 cells_by_level {15: 1, 14: 6, 13: 18, 12: 38, 11: 66, 10: 102, 9: 146, 8: 198, 7: 258, 6: 326, 5: 402, 4: 486, 3: 578, 2: 678, 1: 786} lit_cells 4089
emitter 14 cells_by_level {14: 1, 13: 6, 12: 18, 11: 38, 10: 66, 9: 102, 8: 146, 7: 198, 6: 258, 5: 326, 4: 402, 3: 486, 2: 578, 1: 678} lit_cells 3303
emitter 10 cells_by_level {10: 1, 9: 6, 8: 18, 7: 38, 6: 66, 5: 102, 4: 146, 3: 198, 2: 258, 1: 326} lit_cells 1159
emitter 7 cells_by_level {7: 1, 6: 6, 5: 18, 4: 38, 3: 66, 2: 102, 1: 146} lit_cells 377
emitter 1 cells_by_level {1: 1} lit_cells 1
```

Lecture : pour e = 15, il y a 1 position à 15, 6 à 14, 18 à 13 (distance de taxi 0, 1, 2, ...), 4089 positions de niveau au moins 1.

## Montages communs (SL1)

- **M-OPEN** : espace libre sous ciel ouvert, plancher de pierre à Y 59 (harnais SK0) ; les émetteurs sont posés à Y 100 pour que le plancher ne coupe pas la boule (la boule de e = 15 couvre Y 86 à 114).
- **M-BOX** : boîte fermée de pierre de 5 x 5 x 5 positions intérieures, parois d'au moins 2 blocs ; aucun ciel ne l'atteint (toit de pierre à plus de 15 positions de tout trou).
- **M-ROW** : dans une M-BOX, une rangée de trois cellules L-X-E (émetteur, état mesuré, cellule de lecture) ; toutes les autres cellules sont de la pierre. Variante verticale pour le ciel : M-SHAFT, un puits d'une seule colonne dont toutes les parois sont de la pierre, ouvert vers le haut jusqu'à Y 319, avec [X] puis [E] puis un plancher de pierre.
- **M-TRI** : cube de 3 x 3 x 3 de pierre dont le centre est l'état mesuré X ; deux des six voisins sont des cellules d'air (N_in porte un bloc `light[level=15]`, N_out reste de l'air).
- Les blocs de sonde sont posés avec le mode `strict` pour qu'aucune réaction n'altère l'état mesuré ; un état qui n'existe pas sans support reste donc tel quel. La lumière est recalculée par le jeu à la pose (SL9 mesure quand).
- Dans tout balayage, les cellules mesurées sont espacées de 32 positions dans les trois axes (aucune source ne s'étend à plus de 15 positions), l'attribution des états aux cellules est tirée par graine fixée et un deuxième passage utilise une autre graine : un état dont la mesure change entre les passages invalide la sonde.

## Sondes SL

**SL1 (montages).** Vérification des montages avant toute autre sonde : M-ROW avec un état transparent connu (`air`) donne `BlockLight` de E égal à 13 avec L = `light[level=15]` ; M-TRI avec `stone` en X donne 0 en N_out ; M-SHAFT avec `air` en X donne `SkyLight` de E égal à 15. Critère de succès : les trois mesures de contrôle sont exactes, sinon les montages sont corrigés avant SL2.

**SL2 (R8, R9, R10) : table d'émission.** Balayage des 35 723 états de `blocks.json` (identifiants 0 à 35722 dans l'ordre de l'inventaire) : chaque état est posé isolé dans M-OPEN, cellules espacées de 32 (12 couches de Y -48 à Y 304 par pas de 32, 55 x 55 cellules par couche, zone d'environ 110 x 110 chunks forcés par lots de 256 chunks) ; STEP(5) (ou le délai de SL9 plus 2) ; SNAP ; lecture de `BlockLight` à la position de chaque état. Deux passages avec attributions différentes. Sortie : table (identifiant d'état, émission 0 à 15) et sa fusion en classes (bloc, propriétés qui font varier l'émission). Critère de succès : (a) les deux passages donnent la même table ; (b) toutes les valeurs de R8 et R10 relevées ; (c) `light[level=n]` donne n pour n de 0 à 15, avec `waterlogged` vrai et faux (R9) ; (d) la table est enregistrée dans `.cache/oracle/26.3/light/emission.json` (non commitée) avec son SHA-256 dans le rapport.

**SL3 (R14, R17) : table de filtre.** Pour chaque état du jeu de données (35 723) : (a) canal bloc par M-ROW : f_bloc = 13 - `BlockLight`(E), 13 si E vaut 0 signifie « arrêt » ; (b) canal ciel par M-SHAFT : f_ciel = 15 - `SkyLight`(E) ; (c) canal ciel latéral : rangée [A sous le puits, 15][X][E] sous toit, f = 13 - `SkyLight`(E). Les états d'eau et de lave sont mesurés pour chaque niveau de 0 à 15 (R14 : le filtre de l'eau est-il indépendant du niveau ?). Sortie : table (état, f_bloc, f_ciel_bas, f_ciel_côté) et classes. Critère de succès : (a) la table est complète et stable entre deux passages ; (b) air : 0 ; eau, glace, feuilles : valeurs consignées (1 attendu pour le ciel, 0 supplémentaire pour le bloc selon R14) ; (c) pierre : arrêt dans les trois canaux ; (d) classes comptées et consignées (nombre d'états par valeur de filtre).

**SL4 (R7, R12, R13) : propagation en espace libre.** M-OPEN, émetteurs `light[level=e]` pour e = 15, 14, 10, 7, 1, un par montage à Y 100, aux positions (8, 8) du chunk (0, 0), (15, 15) du chunk (0, 0) (bordure), (0, 0) du chunk (-1, -1), (8, 8) du chunk (-3, -3). SNAP des chunks concernés ; lecture de `BlockLight` sur tout le cube de côté 2e + 1 ; histogramme (niveau, nombre) et comparaison position par position à e - d. Contrôle d'indépendance (R7) : `SkyLight` du même cube. Critère de succès : égalité exacte à max(e - d, 0) pour chaque position ; histogramme égal à SLV4 ; `SkyLight` inchangé par la source.

**SL5 (R12, R15) : obstacles.** M-OPEN, source `light[level=15]` à Y 100 ; scènes : (a) bloc de pierre contre la source (valeur dans la pierre, R15) ; (b) mur de pierre 11 x 11 à 3 positions de la source avec un trou à l'écart de 0, 1, 3 positions du centre ; (c) tunnel en spirale de 40 blocs ; (d) deux sources (15 et 10) séparées de 7 positions ; (e) source à côté de glace, de verre, de feuilles et d'eau sur 8 positions ; (f) ombres diagonales : un seul bloc de pierre sur la diagonale. Mesure : `BlockLight` complet. Les valeurs attendues sont celles d'une propagation par distance de taxi à travers les cellules qui laissent passer, calculée par `tools/diff` (comparateur du `tooling-engineer`) ; la sonde enregistre les valeurs de l'oracle. Critère de succès : égalité exacte, position par position, avec l'oracle pour les six scènes ; à défaut, le premier écart (position, attendu, obtenu) et l'étape fautive (`light`).

**SL6 (R16a, R16b) : occlusion par face.** Étape 1 : M-TRI pour l'état par défaut de chacun des 1286 blocs et pour chacune des 30 paires (face d'entrée, face de sortie) : `BlockLight`(N_out) égal à 13 (passe) ou 0 (ne passe pas), ou autre valeur consignée. Étape 2 : mêmes mesures pour tous les états des blocs pour lesquels l'étape 1 ou la liste de la page "Light" (pistons, capteurs de lumière du jour, tables d'enchantement, terre labourée, pupitres, tailleurs de pierre, chemins de terre, neige, cadres de portail de l'End, dalles, escaliers) montre une asymétrie, et pour un échantillon de 1000 autres états (graine fixée). Critère de succès : table de passage (état, entrée, sortie) -> booléen ; vérification d'une loi de symétrie (le passage de entrée e vers sortie s égale celui de s vers e) sur tout l'échantillon, résultat consigné ; liste des états qui ne suivent pas le modèle « face pleine » (H-LU2) consignée.

**SL7 (R11, R17, R18) : lumière du ciel.** (a) M-OPEN : `SkyLight` en 4 chunks (16 x 16 colonnes chacun) : 15 sur toute la colonne ouverte, y compris Y 319 et sous le plafond d'un tunnel ouvert aux deux bouts. (b) M-SHAFT avec 20 blocs d'eau empilés, 5 blocs de feuilles, 40 blocs de verre, 10 blocs de glace, 1 bloc de toile d'araignée, 1 dalle, 1 couche de neige : `SkyLight` en bas du puits. (c) Même mesure (b) à `time set` 6000 puis 18000 puis `weather thunder` : valeurs stockées identiques. Critère de succès : (a) 15 partout ; (b) valeurs égales à celles d'un modèle « 15 moins le filtre de chaque bloc, descendant libre pour le filtre 0 » consignées avec l'écart éventuel ; (c) identiques en 6000, 18000 et orage.

**SL8 (R21, R22) : mises à jour.** (a) 20 suites de 30 opérations aléatoires (graine fixée) dans un cube de 20 positions de côté : pose de `light[level=l]` (l tiré de 1 à 15), retrait, pose de pierre, retrait ; après chaque suite, STEP(délai de SL9 plus 2) et SNAP. Référence : la configuration finale est construite d'un coup dans une zone distante (même contenu, autre chunk) et SNAP. (b) Émetteurs 15 et 10 voisins (distance 3) : retrait du 15 ; la lumière finale est celle du 10 seul. Critère de succès : `BlockLight` et `SkyLight` de la zone égaux à ceux de la référence pour les 20 suites ; (b) valeurs égales à l'état stable du 10 seul.

**SL9 (R23) : délai de mise à jour.** Gel. (a) `S(8,Y+10,8, light[level=15])` ; SNAP sans pas ; STEP(1) ; SNAP ; STEP(2) ; SNAP ; STEP(3) ; SNAP. (b) retrait de la source : même suite. (c) pose d'un obstacle entre la source et un point lit : même suite. Critère de succès : pour chaque cas, le plus petit nombre de pas après lequel `BlockLight` est égal à l'état stable, identique sur 20 répétitions ; si l'état n'est jamais atteint avant le dixième pas, consigner « jamais » (invalide pour les autres sondes).

**SL10 (R24, R25) : chunks voisins.** (a) Émetteurs `light[level=15]` à x = 15, 14, 1, 0 modulo 16 sur les côtés et aux coins des chunks (0, 0), (-1, -1) et (-1, 0) : `BlockLight` du voisin sur 16 positions et 15 couches, égal à e - d. (b) Chunk (1, 0) non chargé (`forceload remove`), émetteur 15 à x = 14 du chunk (0, 0) ; SNAP de (0, 0) ; `forceload add` de (1, 0) ; attente W ; SNAP de (1, 0). Critère de succès : (a) égalité exacte ; (b) `BlockLight` de (1, 0) = e - d, c'est-à-dire la lumière a franchi la bordure à la charge (ou, sinon, la non-propagation est consignée comme observation).

**SL11 (R20) : bornes verticales.** Émetteurs `light[level=15]` à Y 319, 318, -64, -63 ; colonne ouverte ; lecture des sections de lumière Y -5 et Y 20 si elles existent (`world-chunk-format.md` R15). Critère de succès : valeurs égales à e - d dans la hauteur du monde ; existence et contenu des sections hors hauteur consignés.

**SL12 (R19, R31) : dimensions.** `execute in minecraft:the_nether run forceload add 0 0` et `execute in minecraft:the_end run forceload add 0 0` (condition W comprise). (a) Nether : cavité ouverte (`fill` de bedrock du plafond en air jusqu'à Y 255) : `SkyLight` tout à 0 ou section absente. (b) End : colonne ouverte au-dessus d'un sol de pierre : `SkyLight` en colonne ouverte. (c) Nether : luminosité effective sous plafond ouvert lue par le prédicat de SL13 pour n = 0 à 15. Critère de succès : (a) 0 ou absent ; (b) 15 attendu, sinon observation consignée ; (c) valeur maximale n telle que le prédicat est vrai, consignée.

**SL13 (R26, R27, R28, R29, R30, R32) : luminosité effective et génération.** (a) Prédicat de sonde `probe:light_ge_n` pour n de 1 à 15 : `{"condition":"minecraft:location_check","predicate":{"light":{"light":{"min":n}}}}` (format du champ à vérifier au chargement du datapack ; un message d'erreur est consigné) ; `execute if predicate probe:light_ge_n` à un point de ciel ouvert, `time set` de 11800 à 13700 par pas de 10 (191 relevés), puis de 22300 à 23990 par pas de 10 et de 0 à 200 par pas de 10 (`time set` prend un temps du jour, le tick d'horloge est pris modulo 24000 ; 191 relevés) pour la remontée du matin (R32), puis aux repères 6000, 18000, 22330 ; luminosité effective = plus grand n vrai. (b) Mêmes relevés à midi pour `weather clear`, `rain`, `thunder`. (c) Cavité fermée sans source : 0 ; cavité avec torche : valeur de bloc. (d) Génération : W-NORMAL, graine 12345, 100 chunks forcés pris à `full` (`worldgen-pipeline.md` WP1 pour le statut) ; `BlockLight` et `SkyLight` lus ; l'état stable est recalculé par `tools/diff` à partir des seuls blocs stockés et des tables de SL2, SL3, SL6 ; comparaison. (e) Un chunk laissé à `initialize_light` puis à `light` (`worldgen-pipeline.md` WP7) : `isLightOn` et tableaux. Critères de succès : (a) 15 à midi, 4 de 13670 à 22330 attendus, descente du soir et remontée du matin consignées en tick et en valeur, monotones ; (b) réduction par état météo consignée ; (c) 0 puis valeur de torche ; (d) zéro écart sur 100 chunks, ou écarts classés avec leur nombre ; (e) valeurs consignées.

## Récapitulatif

- Sondes : SL1 à SL13 ; vecteurs SLV1 à SLV3 et SLV4.
- Livrables de référence extraits de l'oracle (non commités) : tables d'émission, de filtre et de passage.
- Hypothèses nommées : H-LU1 (point fixe), H-LU2 (faces pleines), H-LU3 (ciel), H-LU4 (indépendance de l'historique), H-LU5 (délai), H-LU6 (chargement), H-LU7 (interpolation du ciel), H-LU8 (piste cyclique).
- Prérequis : EULA acceptée, lecteur `.mca` indépendant, harnais SK0 et `tools/diff` capable de recalculer la lumière de façon indépendante.

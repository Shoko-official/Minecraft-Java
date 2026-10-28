# Vecteurs et sondes : croissance, propagation, redstone de base

Complète `sim-growth-redstone.md`. Les vecteurs SGV (données de croissance) et SRV (calculs de redstone) sont de preuve D ou C sur S. Les sondes SG (croissance) et SR (redstone) sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée par le propriétaire). Harnais SK0, canaux O1, O3, O4 et annexe `sim_ref.py` : `sim-ticks-updates-vectors.md`.

## Outil et commandes

`REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`.

| Sous-commande | Sortie | Empreinte | Vecteur |
|---|---|---|---|
| `props` | propriétés de 78 blocs choisis (cultures, feuilles, feu, redstone, pistons) | 602def4ad2e6590c | SGV1 |
| `propscan` | effectifs de `age`, `stage`, `moisture`, `distance`, `snowy`, `layers`, `delay`... | ac278bdf0a53564a | SGV1 |
| `tags` | étiquettes de blocs et de fluides (58), absence d'étiquette de conductivité, features d'arbre | d984925c6f3f0f76 | SGV2 |
| `rules` | gamerules M5 | 5e9475808ba24ed4 | SGV3 |
| `dims` | types de dimension (champ `infiniburn`) | b868e24f6f3fd4c6 | SGV4 |
| `comparator` | sorties des 4096 triplets pour les deux modes | 3db3e6feedb49d34 | SRV1 |
| `container` | signal de comparateur de conteneurs d'exemple | 477cdd543bd266f7 | SRV2 |

## SGV, SRV : vecteurs calculés

**SGV1 (R1, R3, R38) : propriétés.** Extrait de `props` et `propscan` :

```
redstone_wire states=1296 east={up,side,none} north={up,side,none} power=0..15(16) south={up,side,none} west={up,side,none}
repeater states=64 delay={1,2,3,4} facing={north,south,west,east} locked={true,false} powered={true,false}
comparator states=16 facing={north,south,west,east} mode={compare,subtract} powered={true,false}
redstone_lamp states=2 lit={true,false}
observer states=12 facing=north..down(6) powered={true,false}
piston states=12 extended={true,false} facing=north..down(6)
wheat states=8 age=0..7(8)
carrots states=8 age=0..7(8)
potatoes states=8 age=0..7(8)
beetroots states=4 age={0,1,2,3}
sugar_cane states=16 age=0..15(16)
cactus states=16 age=0..15(16)
oak_sapling states=2 stage={0,1}
farmland states=8 moisture=0..7(8)
grass_block states=2 snowy={true,false}
oak_leaves states=28 distance=1..7(7) persistent={true,false} waterlogged={true,false}
fire states=512 age=0..15(16) east={true,false} north={true,false} south={true,false} up={true,false} west={true,false}
age blocks=23 value_sets=['{0,1}', '{0,1,2}', '{0,1,2,3}', '0..4(5)', '0..5(6)', '0..7(8)', '0..15(16)', '0..25(26)']
power blocks=7 value_sets=['0..15(16)']
distance blocks=15 value_sets=['1..7(7)', '0..7(8)']
persistent blocks=14 value_sets=['{true,false}']
moisture blocks=1 value_sets=['0..7(8)']
stage blocks=11 value_sets=['{0,1}']
snowy blocks=3 value_sets=['{true,false}']
layers blocks=1 value_sets=['1..8(8)']
delay blocks=1 value_sets=['{1,2,3,4}']
locked blocks=1 value_sets=['{true,false}']
mode blocks=3 value_sets=['{compare,subtract}', '{save,load,corner,data}', '{start,log,fail,accept}']
extended blocks=2 value_sets=['{true,false}']
```

**SGV2 (R2, R5, R6, R39) : étiquettes.** Extrait de `tags` :

```
block/infiniburn_overworld direct=2 resolved=2 first=['minecraft:magma_block', 'minecraft:netherrack']
block/infiniburn_nether direct=1 resolved=2 first=['minecraft:magma_block', 'minecraft:netherrack']
block/infiniburn_end direct=2 resolved=3 first=['minecraft:bedrock', 'minecraft:magma_block', 'minecraft:netherrack']
block/fire direct=2 resolved=2 first=['minecraft:fire', 'minecraft:soul_fire']
block/ice direct=4 resolved=4 first=['minecraft:blue_ice', 'minecraft:frosted_ice', 'minecraft:ice']
block/snow direct=3 resolved=3 first=['minecraft:powder_snow', 'minecraft:snow', 'minecraft:snow_block']
block/leaves direct=14 resolved=14 first=['minecraft:acacia_leaves', 'minecraft:azalea_leaves', 'minecraft:birch_leaves']
block/prevents_nearby_leaf_decay direct=1 resolved=48 first=['minecraft:acacia_log', 'minecraft:acacia_wood', 'minecraft:birch_log']
block/crops direct=8 resolved=8 first=['minecraft:beetroots', 'minecraft:carrots', 'minecraft:melon_stem']
block/saplings direct=12 resolved=12 first=['minecraft:acacia_sapling', 'minecraft:azalea', 'minecraft:birch_sapling']
block/supports_crops direct=1 resolved=1 first=['minecraft:farmland']
block/supports_sugar_cane direct=2 resolved=13 first=['minecraft:coarse_dirt', 'minecraft:dirt', 'minecraft:grass_block']
block/supports_sugar_cane_adjacently direct=1 resolved=1 first=['minecraft:frosted_ice']
block/sand direct=3 resolved=3 first=['minecraft:red_sand', 'minecraft:sand', 'minecraft:suspicious_sand']
block/dirt direct=3 resolved=3 first=['minecraft:coarse_dirt', 'minecraft:dirt', 'minecraft:rooted_dirt']
block/grass_blocks direct=3 resolved=3 first=['minecraft:grass_block', 'minecraft:mycelium', 'minecraft:podzol']
block/buttons direct=2 resolved=15 first=['minecraft:acacia_button', 'minecraft:bamboo_button', 'minecraft:birch_button']
block/pressure_plates direct=4 resolved=17 first=['minecraft:acacia_pressure_plate', 'minecraft:bamboo_pressure_plate', 'minecraft:birch_pressure_plate']
block/rails direct=4 resolved=4 first=['minecraft:activator_rail', 'minecraft:detector_rail', 'minecraft:powered_rail']
block/doors direct=10 resolved=22 first=['minecraft:acacia_door', 'minecraft:bamboo_door', 'minecraft:birch_door']
block tags whose name mentions ('conduct', 'piston', 'push', 'movable', 'immovable') -> ['enables_bubble_column_push_up']
tree configured features present 18 ['acacia', 'azalea_tree', 'birch', 'cherry', 'dark_oak', 'fancy_oak', 'jungle_tree', 'mangrove', 'mega_jungle_tree', 'mega_pine', 'mega_spruce', 'oak', 'orange_poplar', 'pale_oak', 'pine', 'red_
```

**SGV3 (R4) : gamerules.** Extrait de `rules` :

```
random_tick_speed protocol_id=40 parser=brigadier:integer props={"min": 0}
fire_spread_radius_around_player protocol_id=13 parser=brigadier:integer props={"min": -1}
spread_vines protocol_id=54 parser=brigadier:bool props={}
max_snow_accumulation_height protocol_id=28 parser=brigadier:integer props={"max": 8, "min": 0}
```

**SGV4 (R5) : champ `infiniburn`.** Extrait de `dims` :

```
overworld has_skylight=true has_ceiling=false min_y=-64 height=384 logical_height=384 infiniburn="#minecraft:infiniburn_overworld" ambient_light=0.0 monster_spawn_block_light_limit=0 monster_spawn_light_level={"max_inclusive": 7, 
overworld_caves has_skylight=true has_ceiling=true min_y=-64 height=384 logical_height=384 infiniburn="#minecraft:infiniburn_overworld" ambient_light=0.0 monster_spawn_block_light_limit=0 monster_spawn_light_level={"max_inclusive"
the_nether has_skylight=false has_ceiling=true min_y=0 height=256 logical_height=128 infiniburn="#minecraft:infiniburn_nether" ambient_light=0.1 monster_spawn_block_light_limit=15 monster_spawn_light_level=7
the_end has_skylight=true has_ceiling=false min_y=0 height=256 logical_height=256 infiniburn="#minecraft:infiniburn_end" ambient_light=0.25 monster_spawn_block_light_limit=0 monster_spawn_light_level=15
```

(le champ `infiniburn=` figure à la fin de chacune des quatre premières lignes de la sortie complète.)

**SRV1 (R56) : sorties du comparateur** (C sur S, prémisse : formules de la page "Redstone Comparator"). Sortie de `comparator` :

```
compare triples 4096 nonzero 1495 table_sha256_16 dfbf9fb5be689d6d
subtract triples 4096 nonzero 1240 table_sha256_16 139cdccc8a308c56
examples [('subtract', 9, 5, 2, 4), ('compare', 9, 5, 2, 9), ('compare', 9, 10, 0, 0), ('subtract', 3, 15, 0, 0)]
```

**SRV2 (R59) : signal de conteneur** (C sur S). Sortie de `container` :

```
dispenser 9 slots, 300 items of stack 64 -> 8
chest 27 slots, 1 item -> 1
chest 27 slots, full of stack 64 -> 15
chest 27 slots, 14 of 27 stacks full -> 8
hopper 5 slots, 1 item of stack 16 -> 1
double chest 54 slots, 54 stacks -> 15
chest 27 slots, n full stacks 0..27 -> [0, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 15]
```

## Montages communs

Mêmes montages que `sim-light-vectors.md` (SL1) et `sim-fluids-vectors.md` (M-FLOOR) ; en plus :

- **M-FARM(c, s)** : carré de 16 x 16 positions de `farmland[moisture=7]` sur de la terre, un bloc d'eau enfermé dans de la pierre tous les 8 blocs (hydratation certaine), culture `c` plantée à l'`age` 0 à intervalle `s` (1 : champ plein ; 2 : un plant sur deux dans chaque axe) ; ciel ouvert à midi (`time set noon`, `advance_time false`).
- **Mesure de probabilité par tick aléatoire** : `random_tick_speed 4096`, un seul `STEP(1)` ; le nombre moyen de tirages par case et par tick est alors 1 (4096 tirages pour 4096 cases, avec remise : SKV6 de `sim-ticks-updates-vectors.md`), et chaque tirage réussi ajoute 1 à l'âge ; l'âge moyen après un pas est donc la probabilité d'incrément par tirage tant que l'âge maximal n'est pas atteint. Chaque mesure est répétée 80 fois (remise à l'âge 0 par `fill`) pour 20 000 tirages au moins, et la précision demandée est de 1 point.
- **Chronologie de croissance** : `execute store result` du nombre de blocs d'`age` n par O4 plutôt que par SNAP quand la sonde ne l'exige pas.
- **M-PISTON(n)** : rangée de n blocs de pierre devant un piston, bloc de redstone adjacent mis en place comme dernière commande ; observation par SNAP à chaque tick.
- **M-WIRE(n)** : ligne de n fils sur le plancher, levier en bout.

## Sondes SG : croissance et propagation

**SG1 (R7, R14) : taux de croissance des cultures.** Plan factoriel complet : culture parmi blé, carottes, pommes de terre, betteraves ; humidité de la terre 0 (hors de portée de l'eau, `moisture=0` posé, vérifié à chaque lecture) et 7 ; luminosité 9 et 15 (ciel de midi pour 15 ; pour 9, boîte fermée avec bloc `light`) ; voisinage isolé (s = 2) ou champ plein (s = 1). 32 configurations. Mesure de probabilité de SK ci-dessus. Critère de succès : pour chaque configuration, probabilité estimée à 1 point près et consignée dans une table `growth_p[culture][humidité][lumière][voisinage]` ; les 20 000 tirages ne montrent aucune dépendance à la position dans le champ (chi-deux sur 4 quadrants) ; l'implémentation reproduit la table à 3 écarts types.

**SG2 (R8, R9, R10) : lumière et support.** (a) M-FARM dans une boîte fermée avec un bloc `light[level=n+1]` voisin donnant la luminosité n à la culture pour n de 0 à 14, plus midi sous ciel ouvert pour 15 ; mesure de probabilité de SG1 pour le blé ; position de la lecture : même montage avec la lumière de la case de la culture et de la case au-dessus différentes (toit de verre teinté contre bloc lumineux). (b) Survie : culture posée dans le même montage, `setblock stone` puis `air` sur une case voisine (mise à jour de voisin), `STEP(1)`, culture en place ou non, pour n de 0 à 14. (c) Support : blé posé sur `farmland`, `dirt`, `grass_block`, `sand`, `soul_sand`, `moss_block` en mode `strict` puis mise à jour de voisin. Critère de succès : (a) probabilité nulle pour n < 9 et positive pour n >= 9 ; case de lecture identifiée ; (b) plus grande luminosité qui détruit la culture consignée ; (c) seule la terre labourée garde la culture.

**SG3 (R11, R12, R13) : terre labourée.** (a) Une terre `farmland[moisture=0]` au centre ; un bloc d'eau source enfermé dans la pierre à chaque offset (dx, dy, dz), dx et dz de -5 à 5, dy de -2 à 2 (605 essais) ; `random_tick_speed 4096`, `STEP(5)` ; `moisture` lu. Même chose avec de l'eau coulante, puis `weather rain` sans eau. (b) `farmland[moisture=7]` sans eau : `moisture` lu à chaque tick pendant 100 ticks à vitesse 4096, puis à vitesse 1 sur 4096 ticks (comptage des paliers) ; puis eau ajoutée : le saut. (c) Bloc de pierre posé sur la terre : terre ou non sans pas ; terre sèche sans culture, vitesse 4096, 500 ticks : délai du retour à la terre ; avec une culture : 2000 ticks. Critère de succès : (a) carte des offsets qui donnent `moisture=7` égale à |dx| <= 4, |dz| <= 4, dy dans {0, 1} ; (b) trace consignée, saut de 0 à 7 immédiat, rythme de décroissance déterminé ; (c) terre sans pas pour la pierre ; délai consigné ; culture : jamais.

**SG4 (R15, R18) : pousses.** (a) Pour chêne, bouleau, sapin, jungle, acacia, peuplier : champ de 256 pousses `stage=0` espacées de 5 blocs sous ciel de midi, espace au-dessus bloqué par un bloc de pierre (aucun arbre ne peut pousser) : proportion de passages à `stage=1` par tick aléatoire (mesure de probabilité de SK) ; puis `stage=1` avec l'espace libre : proportion de pousses remplacées par un tronc par tick aléatoire ; luminosité 8 et 9. (b) Carrés 2 x 2 de sapin dans une rangée de 3 x 3 pousses de sorte que quatre carrés possibles se recouvrent : quel carré pousse (quatre orientations de la rangée, les cases du carré qui pousse sont identifiées par la disparition des pousses). Critère de succès : (a) probabilités consignées, nulles à 8 ; (b) carré choisi égal à celui de l'ordre sud-est, nord-est, sud-ouest, nord-ouest de R18.

**SG5 (R16, R17) : arbres.** (a) Pour chaque espèce et chaque bloc d'obstacle (`stone`) posé à une position du prisme requis (hauteur h de 1 à la hauteur requise plus 1, décalage horizontal de -3 à 3) et hors du prisme, `sapling[stage=1]` à vitesse 4096 pendant 200 ticks : l'arbre pousse-t-il ? Variantes : obstacle de feuilles, de bûche, d'herbe haute (étiquette `replaceable_by_trees`). (b) 200 pousses `stage=1` par espèce dans un vaste espace libre ; mesure de la distribution des hauteurs, du nombre de troncs et de feuilles ; comparaison aux features candidates `/place feature minecraft:oak` etc. répétées 200 fois ; test du chi-deux de 1 pour cent. (c) Peuplier : prisme et feature par la même méthode. Critère de succès : (a) prisme relevé égal à R17 pour chaque espèce ; (b) feature non rejetée trouvée pour chaque espèce ; (c) prisme du peuplier consigné et H-CR5 réécrite.

**SG6 (R19, R20, R21) : feuilles.** (a) Bûche et rangées de feuilles à 1, ..., 8 blocs de distance le long d'un axe, en diagonale de coin, à travers un bloc de pierre : `distance` de chaque feuille, `random_tick_speed 0`, `STEP(10)` pour la stabilisation. (b) Feuilles à `distance` n, bûche voisine retirée : `STEP(1)`, `distance` ; n de 1 à 6. (c) Feuilles isolées de `distance` 7 (non persistantes) à vitesse 4096 : fraction détruite par tick sur 20 000 tirages ; `persistent=true` sur 2000 ticks. Critère de succès : (a) `distance` égal à la règle R19 ou écart consigné ; (b) changement exactement 1 tick après ; (c) probabilité de disparition par tirage consignée, aucune disparition des feuilles persistantes.

**SG7 (R22 à R25) : herbe.** (a) Un bloc d'herbe au centre d'un champ de `dirt` de 7 x 7 x 7 au ciel de midi ; `random_tick_speed 4096` ; STEP(1) répété ; comptage des conversions par position relative sur 20 000 tirages (remise du champ) ; positions atteintes en dx, dz dans [-1, 1], dy dans [-3, 1] ; coarse dirt et rooted dirt dans le champ : jamais convertis. (b) Herbe sous pierre, sous 1 à 4 blocs d'eau, verre, feuilles, sous luminosité contrôlée n (boîte fermée, `light`) : transformation en terre ou non à vitesse 4096 en 200 ticks. (c) `snowy` pour 1 à 8 couches, neige poudreuse, bloc de neige, glace, air, sur herbe, mycélium, podzol. (d) mycélium et podzol : mesures (a) et (b). Critère de succès : (a) support = 45 positions, uniforme au chi-deux de 1 pour cent, aucune conversion de coarse/rooted ; (b) seuil de luminosité consigné ; (c) `snowy` vrai exactement avec neige, poudreuse, bloc de neige ; (d) résultats consignés.

**SG8 (R26 à R28) : canne et cactus.** (a) Champ de 256 cannes `age=0` sur du sable à côté d'eau, vitesse 1, 4096 ticks d'attente, relevés de l'âge de 20 cannes tick par tick (incrément déterministe ?), croissance en bloc supérieur au 16e tick aléatoire, hauteur finale. (b) Supports : canne sur chacun des 13 blocs de `supports_sugar_cane`, avec eau adjacente ou non, eau en diagonale, eau couverte d'un bloc, eau retirée : survie après une mise à jour de voisin et après 200 ticks aléatoires. (c) Cactus : même mesure de croissance ; survie contre pierre, verre, échelle, dalle, lave, eau en haut ; sur sable et terre. Critère de succès : (a) 16 ticks aléatoires par croissance, hauteur 3 ; (b) table de survie égale à R27 ; (c) croissance 16 ticks, survie égale à R28.

**SG9 (R29 à R34) : feu.** (a) 2000 feux posés par `setblock` sur du `netherrack` (pas d'extinction) ; SNAP immédiat ; histogramme du champ `t` de chaque entrée de feu ; support. (b) Même montage, 20 000 ticks de feu : proportion de ticks où l'âge augmente de 1. (c) Feu sur terre (sans bloc inflammable) à chaque âge de 0 à 15 : proportion d'extinction par tick ; avec une dalle de bois dessous ; sous `weather rain` ouvert au ciel : proportion par âge. (d) `fire_spread_radius_around_player` à 0, 128 (joueur à 100 et à 200 blocs : joueur simulé par un fake player si l'outil le permet, sinon par un vrai client), -1 : feu à côté de planches : propagation, extinction. (e) Pour chaque bloc des 1286 posé à côté d'un feu entretenu (`netherrack`) à difficulté normale avec joueur proche : fréquence d'allumage et de combustion sur 2000 feux. (f) Feu sur `netherrack`, `magma_block`, `bedrock`, pierre dans l'Overworld, le Nether et l'End : durée de vie sur 2000 ticks. Critère de succès : (a) histogramme consigné, support dans 30 à 40 ; (b) 1/3 à 3 écarts types ; (c) conformes à R31 ; (d) conformes à R32 ; (e) table de référence produite, valeurs d'exemple de R33 retrouvées ; (f) durée infinie pour les blocs de l'étiquette `infiniburn` de la dimension, finie sinon.

**SG10 (R35) : lave et feu.** Bassin de lave sous une couche de planches de chêne, avec de l'air entre les deux sur 0 à 4 couches ; `random_tick_speed` 4096 ; nombre de feux apparus en 10 000 ticks aléatoires, par offset (dx, dy, dz) ; avec et sans joueur proche. Critère de succès : carte des offsets où le feu apparaît consignée ; comparée aux régions 3 x 1 x 3 et 5 x 1 x 5 de R35.

**SG11 (R36, R37) : glace.** (a) Glace avec une source de lumière de niveau n voisine (torche 14, lanterne 15, bloc `light[level=n]` de 9 à 15), distance 0 à 3 ; vitesse 4096 ; fonte en eau ou en air ; délai. (b) 400 sources d'eau en biome neigeux (taïga enneigée via `fillbiome`) par condition : lumière 9 et 10, voisin d'air ou d'eau, ciel ouvert ou couvert, `weather clear`, 2000 ticks, vitesse 4096 ; plaines comme contrôle. Critère de succès : (a) seuil de lumière de bloc (11 inclus ou exclu) consigné ; (b) fraction gelée par condition consignée, nulle hors des conditions de R37.

## Sondes SR : redstone de base

**SR1 (R41) : conductivité.** Pour chacun des 1286 blocs (état par défaut) : levier posé sur la face ouest du bloc X, lampe de redstone sur la face est de X, tous trois isolés par de la pierre et séparés de 6 positions ; X posé en mode `strict` ; levier actionné ; `STEP(2)` ; O1 sur la lampe en `lit=true`. Même mesure avec un répéteur allumé derrière X. Critère de succès : table (bloc -> conducteur) complète et stable entre deux passages ; comparée à la règle provisoire de R41 (cubes opaques hors exceptions), écarts consignés ; livrable de référence.

**SR2 (R44 à R47) : fil.** (a) M-WIRE(20) : puissance de chaque fil à `STEP(0)` et `STEP(1)` ; répéteur à mi-chemin. (b) Matrice de configurations autour d'un fil central : voisin de fil à même hauteur, un niveau au-dessus (bloc support conducteur et non conducteur), un niveau en dessous, bloc conducteur ou non conducteur au-dessus du fil bas, bloc de redstone voisin, levier voisin : états des quatre côtés du fil central. (c) Lampes sur les six voisins d'un fil isolé, d'une ligne, d'un point (clic droit simulé par `setblock` d'un état `north=none,...`). (d) Synchronisme : sous gel, levier basculé par `setblock` ; puissances de 15 fils lues sans pas ; réseau avec boucle de rétroaction (deux torches croisées) : états finaux dépendants de l'ordre ? Critère de succès : (a) 15, 14, ..., 1, 0, ... et 15 restauré ; (b) états égaux à R45 ; (c) lampes allumées selon R46 ; (d) toutes les puissances à jour sans pas ; pour la boucle, états finaux consignés.

**SR3 (R48 à R51) : torche.** (a) Torche posée contre un bloc de pierre alimenté par un levier, murale et au sol, contre un bloc de redstone : état à chaque pas ; pose sans alimentation. (b) Levier basculé 8, 9, 10 fois à 2 ticks d'intervalle sur une torche : état et fumée. (c) Torche grillée : attendre 200 ticks sans mise à jour puis ajouter une mise à jour. Critère de succès : (a) changement exactement 2 ticks après ; (b) grillage à la 9e extinction ; (c) état après 200 ticks et après la mise à jour consignés.

**SR4 (R52 à R55) : répéteur.** (a) Pour `delay` de 1 à 4 : temps entre le levier et la sortie. (b) Répéteur alimenté par fil, bloc alimenté, répéteur, comparateur, bloc de redstone, levier sur chacun de ses six voisins : allumé seulement par l'arrière ? (c) Verrouillage par répéteur, comparateur, fil, bloc de redstone, levier sur chaque côté : état figé ? (d) Impulsions de 1 à 8 ticks (levier ou bloc de redstone posé et retiré à des instants exacts) pour `delay` de 1 à 4 : sortie observée tick par tick. Critère de succès : (a) 2, 4, 6, 8 ; (b) arrière seulement ; (c) verrouillage seulement par répéteur et comparateur ; (d) sorties égales à R55 ou matrice consignée.

**SR5 (R56 à R60) : comparateur.** (a) 4096 triplets (arrière, gauche, droite) x 2 modes : sources de puissance 0 à 15 par fils longs depuis un bloc de redstone et des répéteurs (puissance 15 uniquement : les valeurs intermédiaires viennent de fils de longueur connue) ; sortie lue par un fil devant. (b) Composants qui comptent comme entrée latérale : fil pointant, répéteur, comparateur, bloc de redstone, levier, torche. (c) Délai 2 ticks, `OutputSignal` dans l'instantané, impulsions de 2 ticks. (d) Coffre de 0 à 27 piles de 64 ; distributeur de piles de 16 et de 1 ; entonnoir ; coffre double ; coffre bloqué (bloc plein dessus, chat). (e) Coffre derrière une pierre alimentée à 0, 7, 15. Critère de succès : (a) égalité avec SRV1 pour 8192 mesures ; (b) liste consignée ; (c) 2 ticks ; (d) égalité avec SRV2 ; (e) 15 pour la pierre alimentée à 15, le signal du coffre sinon.

**SR6 (R65, R69 et `sim-ticks-updates.md` R32, R33) : pistons, temps.** (a) M-PISTON(1) avec piston et bloc de redstone : `piston_head`, `moving_piston` et blocs à chaque tick de 0 à 6. (b) Quatre sources d'alimentation : `setblock` de bloc de redstone (commande), sortie de répéteur (tick planifié), bloc de redstone poussé par un autre piston (événement de bloc), pression d'une plaque par un objet lâché (phase des entités) ; début du mouvement au tick près, 10 répétitions chacune. (c) Chaîne de 5 pistons qui poussent chacun un bloc de redstone vers le suivant : écart entre les débuts. (d) FIFO : deux pistons concurrents pour un bloc, alimentés par la même mise à jour de voisin, dans les 15 paires de directions. Critère de succès : (a) extension et rétraction de 2 ticks ; (b) délai 0 ou 1 selon la phase de R65, répétable ; (c) écarts de 3 ticks ; (d) le premier piston mis à jour (ordre de `sim-ticks-updates.md` R24) déplace le bloc ; blocs transitoires listés.

**SR7 (R63, R64, R66, R67, R68) : pistons, limites et classes.** (a) Rangées de 12 et de 13 blocs de pierre ; rangées avec slime. (b) Pour chacun des 1286 blocs : piston, bloc X devant, case libre derrière X, alimentation ; X est-il poussé, détruit (et butin), ou bloque-t-il l'extension ? (c) Piston collant avec pierre, slime, obsidienne, sable en chute : tire-t-il ? (d) Impulsions de 1, 2, 3 ticks sur un piston collant poussant un bloc. (e) Dix configurations de blocs collants (slime, miel, terre cuite émaillée, blocs immobiles). Critère de succès : (a) extension à 12 seulement ; (b) table de classes complète, stable entre deux passages ; (c) tire pierre et slime seulement ; (d) position finale au tick près ; (e) ensembles déplacés égaux à R68.

**SR8 (R61, R62) : observateur.** (a) Observateur tourné vers : bloc posé, retiré, changement d'état (levier, lampe, porte), croissance de culture, écoulement d'eau : impulsion ou non ; délai entre le changement et le début de l'impulsion, durée. (b) Deux changements en 1 tick. (c) Observateur posé par `setblock` avec et sans `strict` : impulsion à la pose ? Critère de succès : (a) liste des changements détectés ; délai et durée relevés, durée de l'impulsion 2 ticks ; (c) impulsion ou non consignée (H-RS2).

**SR9 (R70) : quasi-connectivité.** (a) Piston, piston collant, distributeur, dropper et crafter, chacun avec un bloc de redstone en (x+1, y+1, z), (x, y+1, z) ou (x, y+2, z) ou diagonale : état après la pose, puis après une mise à jour de voisin (bloc posé sur une case voisine). (b) Crafter : même montage. (c) `triggered` du distributeur. Critère de succès : (a) piston non étendu à la pose puis étendu après la mise à jour pour (x+1, y+1, z) ; aucun effet pour (x, y+2, z) ; (b) crafter sans effet ; (c) `triggered` égal à l'état du piston.

**SR10 (R40, R42, R43) : modèle de puissance.** (a) Lampe et piston alimentés par du fil à puissance 1 et 15 : même réaction. (b) Bloc de pierre alimenté par un levier : alimente un fil voisin et une lampe voisine ; alimenté par du fil : alimente la lampe mais pas un fil voisin. (c) Pour levier, bouton, bloc de redstone, torche, répéteur, observateur, comparateur : lampe et fil sur chacun des 6 voisins ; état. Critère de succès : (a) identiques ; (b) conforme à R42 ; (c) conforme à R43.

**SR11 (R71, R72, R73) : composants de base.** (a) Boutons de pierre, de bois, de bambou : durée allumée au tick près, 10 répétitions ; pression par `setblock` du bloc `powered=true` (mode commande) et durée. (b) Lampe : retrait de l'alimentation, tick d'extinction au tick près, 10 répétitions ; allumage immédiat. (c) Levier et bloc de redstone : effet immédiat sans pas ; file de ticks vide. Critère de succès : (a) 20 et 30 ticks ; (b) délai 4 (ou 2, H-RS3) répétable ; (c) lampe allumée à `STEP(0)`, file vide.

## Récapitulatif

- Sondes : SG1 à SG11 (11) et SR1 à SR11 (11) ; vecteurs SGV1 à SGV4, SRV1, SRV2.
- Hypothèses nommées : H-CR1 (position de la lumière de croissance), H-CR2 (seuil de survie), H-CR3 (probabilité de disparition des feuilles), H-CR4 (intervalle du feu), H-CR5 (espace du peuplier), H-RS1 (propagation synchrone du fil), H-RS2 (observateur posé par commande), H-RS3 (délai d'extinction de la lampe).
- Prérequis : EULA acceptée, lecteur `.mca` indépendant, harnais SK0, joueur simulé pour SG9 (d) si l'outil le permet, accès au Nether et à l'End pour SG9 (f).

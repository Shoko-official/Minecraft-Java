# Vecteurs et sondes : règles de matière, veines, aquifères, carvers

Complète `worldgen-surface-carvers.md`. Les vecteurs SV sont des calculs sur les données extraites (preuve C). Les sondes sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée) ; chacune dit la commande, l'entrée et la sortie attendue.

## Outil et commandes

`wg_ref.py` (annexe de `worldgen-features-structures-vectors.md`). `REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`.

```
python -I wg_ref.py REF ruletypes        # SV3 (types, compteurs, conditions nommées)
python -I wg_ref.py REF surface overworld overworld/surface overworld/biome_surface
python -I wg_ref.py REF carvers          # SV7
python -I wg_ref.py REF inventory        # liste de carvers par biome (R55)
```

## SV : vecteurs calculés

**SV1 (R7, R28, R76) : ancres résolues** pour `min_y` -64, `height` 384.

| Ancre | Y |
|---|---|
| `above_bottom 0` | -64 |
| `above_bottom 5` | -59 |
| `above_bottom 8` | -56 |
| `above_bottom 80` / `-80` | 16 / -144 |
| `below_top 0` | 319 |
| `below_top 5` | 314 |
| `absolute n` | n (même hors du monde) |

**SV2 (R33, R77) : probabilité de bedrock puis de deepslate**, par la formule publique `(false_at - Y) / (false_at - true_at)`.

| Y | -64 | -63 | -62 | -61 | -60 | -59 |
|---|---|---|---|---|---|---|
| P(bedrock) | 1 | 0.8 | 0.6 | 0.4 | 0.2 | 0 |

| Y | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|---|
| P(deepslate) | 1 | 7/8 | 6/8 | 5/8 | 4/8 | 3/8 | 2/8 | 1/8 | 0 |

L'espérance de couches de bedrock par colonne est 1 + 0.8 + 0.6 + 0.4 + 0.2 = 3.0.

**SV3 (R2, R3, R6) : arbre de la règle `overworld`** (sortie de `wg_ref.py surface overworld`) :

```
SEQ
  @bedrock_floor
  @overworld/copper_ore_vein
  @overworld/iron_ore_vein
  IF above_preliminary_surface
    @overworld/surface
  @overworld/underground
```

Compteurs de `ruletypes` : 42 fichiers ; `condition` 163, `block` 91, `sequence` 49, `biome` 48, `noise_threshold` 43, `y_above` 23, `not` 14, `hole` 6, `steep` 5, `vertical_gradient` 3, `ore_vein` 2, `bandlands` 2, `above_preliminary_surface` 1.

**SV4 (R17) : seuil de `toggle` selon Y.** Cuivre (condition `toggle >= s(Y)`, bande 0..49) :

| Y | 0 | 1 | 5 | 10 | 19 | 20 à 30 | 40 | 45 | 49 |
|---|---|---|---|---|---|---|---|---|---|
| s(Y) | 0.60 | 0.59 | 0.55 | 0.50 | 0.41 | 0.40 | 0.50 | 0.55 | 0.59 |

Fer (condition `-toggle >= s(Y)`, bande -60..-9) :

| Y | -60 | -59 | -55 | -50 | -41 | -40 à -30 | -20 | -15 | -10 | -9 |
|---|---|---|---|---|---|---|---|---|---|---|
| s(Y) | 0.60 | 0.59 | 0.55 | 0.50 | 0.41 | 0.40 | 0.48 | 0.53 | 0.58 | 0.59 |

Calcul : `s = 0.6 - 0.01 x clamp(min(Y_haut - Y, Y - Y_bas), 0, 20)`. Aux bornes de bande : Y = 50 et Y = -8 sont hors bande (`max_exclusive`), Y = -61 hors bande.

**SV5 (R10, R11) : squelette de la surface.** La sortie de `wg_ref.py surface overworld/surface` est la référence ; les 29 biomes cités par les règles de l'Overworld sont : `badlands`, `beach`, `dappled_forest`, `deep_frozen_ocean`, `deep_lukewarm_ocean`, `desert`, `dripstone_caves`, `eroded_badlands`, `frozen_ocean`, `frozen_peaks`, `grove`, `ice_spikes`, `jagged_peaks`, `lukewarm_ocean`, `mangrove_swamp`, `mushroom_fields`, `old_growth_pine_taiga`, `old_growth_spruce_taiga`, `snowy_beach`, `snowy_slopes`, `stony_peaks`, `stony_shore`, `sulfur_caves`, `swamp`, `warm_ocean`, `windswept_gravelly_hills`, `windswept_hills`, `windswept_savanna`, `wooded_badlands`.

Bloc de dessus attendu selon la branche par défaut (R11, hors eau) : herbe puis terre ; sous l'eau : terre. Pour les trois océans chauds et les plages : sable (plafond : grès). Pour `stony_peaks`, `jagged_peaks` : pierre. Ces lignes sont des prédictions C sur S, à confirmer par L2.

**SV6 (R39 à R41) : contrôle des aquifères.** Sorties de lecture des données : 6 champs ; échelles Y 0.5, 0.67, 0.7142857142857143, 1.0 ; `exclusion` strictement positive pour erosion < -0.225 et depth > 0.9. Valeurs de test de l'expression : erosion -0.3 et depth 1.0 donnent min(0.075, 0.1) = 0.075 (exclu) ; erosion -0.3 et depth 0.9 donnent min(0.075, 0) = 0 (non exclu, inclusivité R41) ; erosion -0.2 et depth 1.0 donnent min(-0.025, 0.1) = -0.025 (non exclu).

**SV7 (R56, R57) : plages résolues des carvers** (commande `wg_ref.py REF carvers`).

| Carver | probabilité | Y entier | count | autres |
|---|---|---|---|---|
| `cave` | 0.15 | -56 à 180 | 0 à 14, très biaisé vers le bas | rayon horizontal [0.7,1.4), vertical [0.8,1.3), salle [0.1,0.9), plancher [-1.0,-0.4), épaisseur trapèze 0..3 plateau 1, `weird_thickness_bias` vrai |
| `cave_extra_underground` | 0.07 | -56 à 47 | idem | idem |
| `canyon` | 0.01 | 10 à 67 | sans objet | rotation verticale [-0.125,0.125), `distance_factor` et `horizontal_radius_factor` [0.75,1.0), épaisseur trapèze 0..6 plateau 2, lissage 3, `y_scale` 3.0 |

Probabilité qu'un chunk source déclenche au moins un carver (indépendance supposée, S) : 1 - 0.85 x 0.93 x 0.99 = 0.2174 (valeur C sur S, à comparer à WK7).

## Harnais commun des sondes

Les sondes W** supposent la sonde WP0 (`worldgen-pipeline.md`) acquise : le remplacement par datapack de ressources vanilla à la création du monde fonctionne, et `tools/oracle` lit les `.mca` avec son propre lecteur. Conventions :

- Graine de sonde : 12345 sauf indication. Zone : chunks (-4..4, -4..4) sauf indication ; ajouter une zone à cheval sur zéro est obligatoire pour toute sonde sur la coordonnée.
- Génération : `/forceload add x1 z1 x2 z2`, attente W (chargement, `world-vectors.md`), `/save-all flush`, arrêt du serveur, lecture de `region/r.*.mca`.
- Les datapacks de sonde sont **produits à la volée** par `tools/oracle` depuis `.cache/reference/26.3` (copie modifiée), jamais commités.
- Montage "règle de sonde" **MR(C)** : remplace `noise_settings/overworld.json` par une copie dont `material_rule` désigne `probe:mr`, où `probe:mr` = `condition(C) -> block(minecraft:gold_block)`. Un bloc d'or dans la sortie signifie "C vraie". Montage **MR0** : la règle est `block(gold)` seule.
- Montage **FLAT** : copie de `noise_settings/overworld` dont `final_density` est un gradient linéaire en Y, positif sous Y 127 (la fonction exacte relève du premier tiers) ; `aquifers` conservé ou retiré selon la sonde ; biomes conservés.
- Montage **CLEAN** : tous les biomes sans `carvers` ni `features` (copie des 56 biomes avec les listes vides) et `structure_set` vides.

## Sondes de surface (WS)

**WS1 (R22, R23).** MR0, FLAT puis monde normal, CLEAN. Sortie attendue : le ou les ensembles de positions en or. Lecture : l'air est-il évalué ? l'eau ? la pierre ? Critère : liste de types de blocs présents autour d'une côte (océan). Résultat attendu provisoire : or partout où la densité était solide, rien dans l'air ni l'eau. Variante (R74) : `result_state` écrit en objet `{Name, Properties}` (bloc à propriété, par exemple une bûche d'axe x) : l'état relevé porte la propriété. Critère de succès : égalité exacte du tableau de vérité avec la prédiction sur 4 096 colonnes.

**WS2 (R24, R26).** MR(`stone_depth` floor, décalage k, `add_surface_depth` faux, plage 0) pour k de -1 à 4 ; MR avec `ceiling` sur une grotte de FLAT carvers (CLEAN sans carvers : utiliser le monde normal). Sortie : épaisseur d'or par colonne = 1 + k attendue. Comparer sur 1024 colonnes.

**WS3 (R27).** MR(`stone_depth` floor, décalage 0, `add_surface_depth` vrai, plage 0) : l'épaisseur d'or en chaque colonne vaut `profondeur_de_surface + 1`, qui donne la profondeur mesurée. Comparer à `floor(surface x 2.75 + 3.0 + a x 0.25)` où `surface` est calculé par notre implémentation du bruit (premier tiers). Statistique attendue : écart (mesure - 2.75 x surface - 3.0) toujours dans [0, 0.25) avant la partie entière ; compter les colonnes où la mesure n'est pas dans { floor(2.75 s + 3), floor(2.75 s + 3.25) }. Tout autre écart réfute la formule.

**WS4 (R26).** Même chose avec plage secondaire 6, puis 30, `add_surface_depth` faux : l'épaisseur d'or ajoute `floor(surface_secondary mappé)`. Conclusion attendue : distribution de la valeur secondaire.

**WS5 (R28, R29).** MR(`y_above` ancre absolue 62, 63, 64, `add_stone_depth` faux, multiplicateur 0), puis multiplicateurs -1, 1, 2, `add_stone_depth` vrai, sur un océan et une côte. Sortie : tableau de vérité ; vérifier "strictement au-dessus". MR(`not_underwater`), MR(`not_under_deep_water`) sur un lit marin de profondeur 1 à 20 : relever les profondeurs où la condition bascule (inclusivité de R29).

**WS6 (R30, R31).** MR(`steep`) sur 4 montagnes (`jagged_peaks`, `frozen_peaks`, `snowy_slopes`, `stony_peaks`) et sur terrain plat ; MR(`hole`) sur un océan gelé. Sortie : carte des colonnes vraies. Le critère de `steep` se déduit en corrélant la carte avec la différence de hauteur de surface entre voisins (4 directions) : chercher le seuil et les directions.

**WS7 (R25).** MR(`noise_threshold` bruit `surface`, min a, max b) pour une grille de seuils. La valeur du bruit en une colonne se retrouve par bissection (30 mondes par précision 1e-9) ; commencer à 12 niveaux de bissection sur 64 colonnes. Vérifier : borne basse incluse ? borne haute exclue ? Test d'égalité : choisir une colonne dont la valeur calculée est connue et poser exactement min = valeur. Version 3D : bruit `sulfur_cave_gradient`, `is_3d` vrai : bissection sur quelques positions de `sulfur_caves`.

**WS8 (R33).** MR(`vertical_gradient`) copiant `bedrock_floor` (même `random_name`) puis un nom différent : compter, par Y, la proportion de positions vraies sur 16 x 16 x 100 colonnes. Attendu : proportions de SV2. Vérifier le déterminisme : même graine, mêmes positions. Vérifier la dépendance au nom : deux noms différents donnent des motifs différents.

**WS9 (R34, R84).** MR(`biome` `minecraft:plains`) à la limite d'un biome de grotte (monde normal) : l'or doit suivre la grille de biomes en cellules de 4 x 4 x 4 (position (x,y,z) -> cellule (x>>2, y>>2, z>>2)). Vérifier en Y que le biome de la position est utilisé, pas celui de la surface.

**WS10 (R36).** MR(`bandlands`) sur une colonne `badlands`, 10 colonnes à Y -64..319 : relever la suite de blocs par Y, sa période et sa dépendance à (x, z). Aucune hypothèse de palette n'est présupposée.

**WS11 (R32).** MR(`above_preliminary_surface`), océan, plaine, montagne : relever la hauteur de transition par colonne et la comparer à notre `overworld/preliminary_surface_level`. Vérifier l'inclusivité à la transition.

**WS12 (R21).** MR sur séquences contrôlées : `sequence[condition(C1)->block(or), condition(C2)->block(diamant)]` pour C1 et C2 se recouvrant. Attendu : un bloc par position, premier résultat prioritaire ; position où C1 est vrai et `then_run` ne produit rien (condition imbriquée fausse) doit passer à l'élément suivant. Variante (R73) : la même séquence écrite en ligne puis par référence à un fichier. Critère de succès : un seul bloc par position, égal à la prédiction sur 1 024 colonnes ; sorties identiques pour les deux écritures.

**WS13 (R18, R37).** Règle réelle de `overworld` sans bedrock puis sans veines : isoler l'effet de chaque élément sur Y -64..-59 et sur les bandes de veines.

## Sondes de veines (WO)

**WO1 (R19).** Monde normal CLEAN, règle `overworld` seule : relever, sur un bloc de 64 x 64 x 120, les cellules de cuivre / granite / fer / tuff. Vérifier R17 (bandes, seuil de `toggle` : comparaison avec notre `toggle`).

**WO2 (R19).** Densité de minerai : proportion de minerai dans une veine selon `richness` (0.1 à 0.3) : tracer la proportion en fonction de notre `richness` par intervalles de 0.02 sur 10 000 cellules de veine.

**WO3 (R19).** `filler_gap` : proportion de trous et signe. Remplacer `filler_gap` par une constante -1 puis 1 (copie de la règle) : relever ce qui change.

**WO4 (R19).** Blocs bruts : sur un très grand échantillon (100 000 minerais), proportion de blocs bruts : 0.02 attendu ; position de ces blocs dans la veine (coeur ? bord ?). Remplacer `raw_ore_chance` par 0 puis 1.

**WO5 (R20).** Veine dans une région creusée par un carver (monde normal CLEAN sauf carvers) : les cavités ne contiennent pas de minerai de veine ; une veine qui traverse la bordure d'une grotte garde ses blocs hors de la cavité.

## Sondes d'aquifères (WA)

Montage commun : FLAT avec `final_density` creusant des cavités simples (fonction de bruit choisie), CLEAN (pas de carver). Chaque sonde est répétée avec et sans `aquifers`.

**WA0 (R42, R44).** Sans `aquifers` : où est l'eau ? relever le niveau de remplissage des cavités en fonction de Y (attendu : eau jusqu'à Y 63, lave sous Y -54).

**WA1 (R43, R45).** Avec `aquifers`, `fluid_level_floodedness` remplacé par une constante 1 (toujours inondé) et `lava` par -1 : relever la carte des états en 3D ; les frontières donnent la grille de cellules et les décalages (5, 1, 5), l'étendue des centres.

**WA2 (R46, R47, R48).** `floodedness` constant 1 puis 0.5 puis 0 puis -0.5 : relever, par cellule, les cas plein / partiel / sec ; en partiel : le niveau mesuré et sa dépendance à `fluid_level_spread` (constantes -10 à 10 par pas de 3).

**WA3 (R44, R48).** `lava` constant 1 : état lave attendu pour les niveaux <= -10 ; relever le seuil exact (-10 ? -9 ?). `lava` constant 0.3 puis 0.31 pour le seuil 0.3 en valeur absolue.

**WA4 (R41).** `exclusion` remplacé par une constante positive puis négative : aucun aquifère / aquifères partout.

**WA5 (R49).** Deux cellules adjacentes à états forcés (via `floodedness` constant, `lava`, constants par zones d'une fonction de densité en escalier) : relever l'épaisseur de pierre au-dessus du niveau supérieur (attendu <= 5) et sous le niveau inférieur (<= 23) ; courbe de pression en fonction de la distance. Critère de succès : épaisseur de pierre au-dessus du niveau supérieur inférieure ou égale à 5 et sous le niveau inférieur inférieure ou égale à 23 pour 100 % des paires de cellules relevées, et courbe de pression reproduite à 1 bloc près.

**WA6 (R51).** Lire `fluid_ticks` du chunk écrit : positions planifiées, priorité, délai. Comparer au voisinage des frontières relevées en WA1.

## Sondes de carvers (WK)

Montage commun FLAT + CLEAN sauf carvers (donc pas de grotte de bruit ; FLAT pose une plaque de pierre pleine jusqu'à Y 127) ; aquifères retirés sauf indication.

**WK1 (R65).** Un seul carver actif (`cave`) avec `probability` 1.0 et `count` constant 1, `y` constant : chaque chunk reçoit exactement une tentative. Relever la forme (salle, troncs, branches), les dimensions, les positions de départ en fonction du chunk source (le chunk le plus proche d'un départ, par minimisation). Comparer avec les valeurs publiques de R65. Critère de succès : sur 1 000 tentatives, hauteur et diamètre de salle, tailles de tronc et de branche relevés inclus dans les plages publiques de R65 pour 95 % des tentatives ou plus ; les écarts sont consignés.

**WK2 (R60, R61).** Même montage, `probability` 1.0 sur un seul chunk source (rendre les autres chunks sources sans tentative : `count` constant 0 par datapack ne distingue pas un chunk ; utiliser une zone où seul un chunk est forcé et vérifier le voisinage par comparaison de deux ensembles de chunks forcés). Mesure : étendue maximale du creusement autour du chunk source -> portée de R61.

**WK3 (R63).** Monde en couches : `material_rule` posant une couche de 4 blocs d'un type candidat tous les 4 Y (terre, herbe, sable, sable rouge, gravier, neige, glace, grès, terracotta, minerais, deepslate, tuff, bedrock, eau, lave, obsidienne, béton ...) dans un tube creusé de façon constante (probabilité 1.0). Sortie : liste des blocs creusés et non creusés. Critère de succès : chaque type candidat est classé creusé ou non creusé sans ambiguïté sur 10 tubes, et la liste est stable entre deux graines.

**WK4 (R62).** Tunnel traversant Y -64 à -50 : relever la lave et son niveau (Y -56 ?), avec et sans aquifères.

**WK5 (R64).** Océan (FLAT avec 40 blocs d'eau) : tunnel qui touche le fond : est-il rempli d'eau ? Le fond sableux est-il creusé ? Relever la règle sur la colonne d'eau au-dessus d'une position creusée. Critère de succès : pour chaque épaisseur d'eau de 1 à 40, l'état creusé ou non de la position est consigné et la règle de R64 reproduit 100 % des cas.

**WK6 (R24, R52, R70).** Avec aquifères et surface normale : positions creusées au niveau de la surface (blocs de dessus). Les blocs de dessus (herbe) sont-ils retirés sans modification du bloc dessous ? Cavités sous Y 63 : eau, air ou lave selon l'aquifère ? Comparer WK6 avec et sans aquifères.

**WK7 (R60).** Statistique : sur 10 000 chunks sans montage de probabilité (probabilité vanilla, `count` vanilla), proportion de chunks ayant au moins un départ visible (via WK1). Comparer à 0.2174 (SV7).

**WK8 (R66).** Canyon seul (`probability` 1.0) : forme, direction, largeur le long de la trajectoire ; vérifier `width_smoothness`, `y_scale`, rotation verticale. Critère de succès : longueur, largeur maximale et hauteur maximale de 100 ravins relevées et comparées aux plages de R66 ; les écarts sont consignés.

## Correspondance règle -> sonde

R18 WS13 ; R19 WO1 à WO4 ; R20 WO5 ; R21 WS12 ; R22 et R23 WS1 ; R24 WS2 et WK6 ; R25 WS7 ; R26 WS2 et WS4 ; R27 WS3 ; R28 et R29 WS5 ; R30 et R31 WS6 ; R32 WS11 ; R33 WS8 ; R34 et R84 WS9 ; R36 WS10 ; R42 WA0 ; R43 WA1 ; R44 WA0 et WA3 ; R45 WA1 et WA2 ; R46 et R47 WA2 ; R48 WA2 et WA3 ; R41 WA4 ; R49 WA5 ; R51 WA6 ; R52 WK6 ; R60 WK2 et WK7 ; R61 WK2 ; R62 WK4 ; R63 WK3 ; R64 WK5 ; R65 WK1 ; R66 WK8 ; R67 `worldgen-pipeline.md` WP2 ; R68 WP5 ; R71 WP4.

## Récapitulatif

- Sondes : WS 13 (WS1 à WS13), WO 5, WA 7 (WA0 à WA6), WK 8. Total 33.
- Prérequis : WP0 (`worldgen-pipeline.md`), EULA acceptée par le propriétaire, lecteur `.mca` indépendant.
- Résultats à écrire dans `reports/probes/` ; vecteurs dorés issus des sondes à ajouter ensuite à ce fichier.

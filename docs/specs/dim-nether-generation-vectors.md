# Vecteurs et sondes : génération du Nether

Complète `dim-nether-generation.md`. Les vecteurs DX sont des calculs sur les données extraites (preuve C), produits par `dim_ref.py` (annexe de [dim-definitions-portals-vectors.md](dim-definitions-portals-vectors.md), SHA-256 `ac173f0213d2b995bae6e74a0302b4c84f3c81331ebc55792970fb45e1fada2b`). Les sondes DN sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée) ; chacune dit la commande, l'entrée et la sortie attendue.

Les tableaux volumineux ne sont pas reproduits : la commande est donnée avec l'empreinte SHA-256 complète de la sortie et quelques lignes d'exemple. `REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`.

## Commandes

| Commande (`python -I dim_ref.py REF ...`) | Sortie | SHA-256 de la sortie |
|---|---|---|
| `noise nether` | réglages, routeur, rampes, bruits (R1 à R4) | `313fad8385997e37a0a9882c294dab4074002b94039da9b32a0e18277230be34` |
| `surface-nether` | arbre de la règle `nether` et conditions nommées (R12, R13) | `ce207e449550eee6a5bcb6df580ced33b24aa146ef2fd9c3e44f582a9f1787f8` |
| `bedrock` | probabilités de bedrock, plage du carver (R6, R10) | `9354a8eb9d347cd23643fc4451da5da79538293914c7291068b5fb3a3a33e318` |
| `biomes` | table de 5 points, listes de features, créatures (R16, R19, R21, R22, R38) | `69818cd9a61bb8d4721fa80de2ae9bff6a2715527dfef7f5ee40cf31719cee90` |
| `features` | 42 placed features avec Y résolus sur 0 à 127 (R38, R40) | `646367d45fb2b03a75fd0b732318e6b8ed1ef4e9a415bd20b53c7d386b31486b` |
| `order` | ordre partiel par étape dans les 5 biomes (R39) | `a2ade2d170aff7d8e640bac04dbbab9cbfc42891363ff278a337e9924ad4803b` |
| `structures` | ensembles, structures, tags, gabarits, pièces (R26 à R36) | `121ff06ca97a0f9c6baaf6c4e3447e46e9ad3b5bba32f60f48b85b45a1ece1cd` |
| `bastion` | 60 pools, 176 éléments, 167 gabarits (R32) | `a26b5ba8fda0e74cd86e4001f39f7dc9edfdef29102fa3fdbe78b2e6653671cf` |
| `vectors` | profil du Nether (DX7), régions (DX9) | `bb91b74c001302f7ae79f332baf6bdbd162ff9e88994f05919d3f5ebca62c2f6` |

## DX : vecteurs calculés

**DX7 (R5) : profil sans bruit du Nether** (sortie de `vectors`, section DX7). `n` est la valeur du bruit hérité ; la colonne donne le seuil au-delà duquel un bloc est solide.

| Y | a_bas | a_haut | densité pour n = 0 | solide si |
|---|---|---|---|---|
| 0 | 0.25 | 1 | +1.2 | n > -7.5 |
| 8 | 0.5 | 1 | +0.8 | n > -2.5 |
| 16 | 0.75 | 1 | +0.4 | n > -0.833333 |
| 23 | 0.96875 | 1 | +0.05 | n > -0.080645 |
| 24 à 104 | 1 | 1 | 0 | n > 0 |
| 105 | 1 | 0.958333 | +0.025 | n > -0.040761 |
| 116 | 1 | 0.5 | +0.3 | n > -0.9375 |
| 122 | 1 | 0.25 | +0.45 | n > -2.8125 |
| 127 | 1 | 0.041667 | +0.575 | n > -21.5625 |

**DX8 (R10) : bedrock** (sortie de `bedrock`) : plage de génération 0 à 127 ; sol : Y 0 : 1, Y 1 : 0,8, Y 2 : 0,6, Y 3 : 0,4, Y 4 : 0,2, Y 5 : 0 ; plafond : Y 122 : 0, Y 123 : 0,2, Y 124 : 0,4, Y 125 : 0,6, Y 126 : 0,8, Y 127 : 1. Espérance de couches de bedrock par colonne : 3,0 au sol et 3,0 au plafond (1 + 0,8 + 0,6 + 0,4 + 0,2 chacune). Plage du carver : `absolute 0` à `below_top 1` = 0 à 126.

**DX9 (R26, R27) : régions de structures** (forme condensée des sorties de `structures` et de `vectors`, section DX9) :

```
nether_complexes spacing 27 separation 4 -> region 432 blocks, 23 candidate positions per axis (368 blocks)
nether_fossils   spacing 2  separation 1 -> region 32 blocks,  1 candidate position per axis (16 blocks)
ruined_portals   spacing 40 separation 15 -> region 640 blocks, 25 candidate positions per axis (400 blocks)
```

**DX10 (R12) : début de l'arbre de la règle `nether`** (sortie de `surface-nether`, 87 lignes au total ; empreinte ci-dessus) :

```
SEQ
  @bedrock_floor
  @bedrock_roof
  IF y_above(below_top=5,sdm=0,asd=False)
    BLOCK netherrack
  IF biome(basalt_deltas)
    SEQ
      IF @under_ceiling
        BLOCK basalt
```

**DX11 (R36, R38, R39, R40) : comptes d'inventaire** : listes de features par biome (`biomes`) : `nether_wastes` 10 listes, `soul_sand_valley` 10, `crimson_forest` 10, `warped_forest` 10, `basalt_deltas` 8 ; features non vides : `nether_wastes` étape 7 : 15, étape 9 : 3 ; `soul_sand_valley` étape 2 : 1, 7 : 15, 9 : 1 ; `crimson_forest` 7 : 12, 9 : 6 ; `warped_forest` 7 : 13, 9 : 7 ; `basalt_deltas` 4 : 3, 7 : 15. Ordre (`order`) : étape 7 : 23 features, 27 arêtes, 23 paires non comparables sur 253 ; étape 9 : 10 features, 9 arêtes, 12 paires sur 45 ; étape 4 : 3, 2, 0.

## Montages

Conventions de `worldgen-surface-carvers-vectors.md` (graine 12345 sauf indication ; zone (-4..4, -4..4) plus une zone à cheval sur 0 ; `forceload`, attente W, `save-all flush`, arrêt, lecture de `dimensions/minecraft/the_nether/region/r.*.mca`) ; datapacks produits à la volée, jamais commités.

- **MN0** : monde normal, Nether intact. **MN1** : listes `features` vides sur les 5 biomes. **MN2** : MN1 et `structure_set` `nether_complexes`, `nether_fossils`, `ruined_portals` vidés. **MN3** : MN2 et `carvers` vides sur les 5 biomes. **MN4** : MN3 et règle de matière `nether` remplacée par `block(netherrack)` (ni bedrock, ni surface). La suite MN4 -> MN0 isole une étape par différence : MN3 - MN4 : règle de matière ; MN2 - MN3 : carvers ; MN1 - MN2 : structures ; MN0 - MN1 : features (`worldgen-pipeline.md` R37, version du Nether).
- **MRN(C)** : `noise_settings/nether.json` remplacé par une copie dont `material_rule` est `condition(C) -> block(gold_block)` (règle `probe:mr`) ; un bloc d'or signifie « C vraie ». **MRN0** : règle `block(gold_block)` seule. Les conditions se composent par imbrication de `condition` (il n'existe pas de conjonction) ; le biome se teste par une `condition` `biome` extérieure.
- **FLATN(Y)** : `final_density` remplacée par un dégradé en Y positif sous Y et négatif au-dessus ; biome fixe d'un datapack (`fixed`) ; carvers et features selon la sonde.
- **ONEFEAT-N(X, s)** : dimension `probe:n` de type `the_nether`, générateur `noise` avec les réglages FLATN(40), biome fixe de la feature (`fixed`), listes `features` vides sauf l'étape `s` contenant la placed feature X, aucun carver, aucune structure ; la sortie est la trace de X seule (différence avec la même zone sans X).
- **ONESTRUCT-N(S)** : tags `has_structure/*` remplacés par les 5 biomes, ensemble réduit à S, fréquences à 1.0 quand la sonde ne les mesure pas ; placement par `/place structure <S> <x> <y> <z>` pour les statistiques de forme et par génération de monde pour la fréquence.
- **BIOMES-N** : source `multi_noise` en ligne dans un datapack de dimension, liste `biomes` de 2 points au choix (sans bruit : `temperature` et `vegetation` du routeur remplacés par des constantes) pour mesurer la métrique de distance.

## Sondes (DN)

**DN0 (prérequis, comme WP0 de `worldgen-pipeline.md`).** Monde neuf, graine 12345, MRN0 : le chunk (0, 0) du Nether est en or là où il y avait du solide ; aucune erreur. Variante : remplacement d'un fichier de `noise_settings/nether.json` après la création d'un monde : pas d'effet sur les chunks déjà générés.

**DN1 (R5, R7).** MN4 : 200 chunks ; par Y de 0 à 127, fraction de blocs solides. Attendu : courbe `P(solide | Y) = P(n > seuil(Y))` avec les seuils de DX7 ; ajuster l'amplitude de n de notre implémentation. Critère de succès : fraction égale à la nôtre à 0,5 point près pour chaque Y. Variante : positions des blocs solides isolés (colonnes non connexes) et surface à Y 31/32 (lave).

**DN2 (R8).** MRN(`noise_threshold` bruit B, minimum a, maximum +infini) pour B dans {`nether_state_selector`, `patch`, `netherrack`, `nether_wart`, `soul_sand_layer`, `gravel_layer`}, a parcourant 30 valeurs par bissection ; 64 colonnes ; les bruits en trois dimensions (aucun des six) ne sont pas employés. Valeurs reconstruites pour chaque colonne. Pour `nether/temperature` et `nether/vegetation`, lecture indirecte par BIOMES-N : biome de deux points `a` et `b` de température 0 et 0,4 et d'humidité 0 : le biome change quand la température du point franchit le milieu des deux températures (si la distance est monotone). Variante égalité : `minimum` posé exactement à la valeur mesurée d'une colonne, borne haute à +infini et à cette valeur. Critère de succès : valeurs de B égales à celles de notre implémentation à 1e-6 près sur 64 colonnes pour 3 graines (dont -1 et `Long.MIN_VALUE`).

**DN3 (R6, R23).** MN0 sur 200 chunks : (a) Y maximal de bedrock (attendu 127) et Y minimal de bedrock de plafond (attendu 122 ou 123) ; (b) Y maximal des minerais de quartz (attendu 117 sous H-NT1, 245 sous H-NT1b) et de pépites d'or ; (c) MN2 contre MN3 : Y maximal d'une cavité de carver (attendu 126) ; (d) Y minimal et maximal de la lave de source (`spring_open`, 4 à 123). Critère de succès : extrêmes égaux à la prédiction de H-NT1.

**DN4 (R9).** MN4 : liste des blocs de chaque colonne de 25 chunks ; positions non solides à Y 30, 31, 32, 33. Critère de succès : lave exactement à Y <= 31, air à partir de Y 32.

**DN5 (R11).** MRN(`vertical_gradient` copiant `bedrock_floor`, puis `bedrock_roof` en `not`) puis avec un nom aléatoire différent : proportions par Y sur 16 x 16 x 100 colonnes. Critère de succès : probabilités de DX8 à 2 points près ; motifs différents pour deux noms ; même motif pour une même graine.

**DN6 (R14).** MRN(C) pour C dans {`under_ceiling`, `under_floor`, `on_floor`, `on_ceiling`, `hole`, `y_above(absolute 30, asd vrai)`, `not(y_above(absolute 35, asd vrai))`, chaque `biome(B)`} sur 25 chunks de MN3 ; carte de colonnes vraies. Critère de succès : égale à celle de notre implémentation (bruit imposé par DN2) pour chaque condition.

**DN7 (R15).** MN4 avec la règle `nether` intacte : bloc de surface (le plus haut bloc solide de chaque colonne dont la face supérieure est à l'air) et profil de la colonne, 25 chunks, 3 graines ; état canonique des blocs. Critère de succès : égalité exacte avec notre implémentation (stratification de `worldgen-pipeline.md` R35 : après DN1 et DN2 identiques).

**DN8 (R17, R18, R20) et DN8b.** DN8 : MN4, carte des biomes de 64 x 64 chunks, 3 graines, lue dans les `.mca` ; vérification de l'invariance en Y (R17) sur 1 000 colonnes ; proportions (R20) ; carte bloc par cellule comparée à la nôtre. DN8b : BIOMES-N avec deux points aux paramètres (température, humidité, offset) choisis pour que la bissection d'un paramètre donne la distance (euclidienne au carré ou autre, offset compris). Critère de succès : 16 384 cellules de quart par graine égales ; ou métrique mesurée.

**DN9 (R24, R25).** MN3 contre MN4 sur 25 chunks de 5 graines : différences = cavités. Relever : Y des cavités, rayon, nombre de grottes par chunk, ensemble des blocs remplacés (poser avant creusement du netherrack, basalte, blackstone, nylium, sable des âmes, gravier, glowstone), contenu des cavités à Y <= 31 et > 31, bedrock au sol (Y 0 à 4) et au plafond (Y 123 à 126). Critère de succès : cavités égales aux nôtres (WK de `worldgen-surface-carvers-vectors.md` pour la méthode).

**DN10 (R28, R31, R37).** ONESTRUCT-N(`fortress`) : 100 forteresses par `/place structure minecraft:fortress` ; relever par forteresse le nombre de pièces par identifiant (16 `ne*`), la boîte englobante, la boîte et le Y de la croisée de départ, la position dans le chunk, le nombre de générateurs, le nombre de coffres, l'enfouissement ; et en génération de monde (MN2 sans autre structure) la fréquence par biome de candidat sur 500 régions. Critère de succès : distributions égales aux nôtres à 3 écarts types (fréquences) et égalité exacte de la croisée de départ (position, Y).

**DN11 (R28, R33, R34, R37).** ONESTRUCT-N(`bastion_remnant`) : 100 bastions ; relever la variante (4 attendues de probabilité 1/4), le Y de la pièce de départ, le nombre de pièces, la boîte englobante, la présence de supports sous les pièces au-dessus de la lave. Variante : `/place template minecraft:bastion/units/air_base ... integrity ... seed ...` pour une pièce seule. Critère de succès : égalité exacte de la structure pour 5 graines (`/place structure` ne fixe pas la graine : comparaison par génération monde) ; fréquences à 3 écarts types.

**DN12 (R35).** ONESTRUCT-N(`nether_fossil`) en `soul_sand_valley` : acceptation par candidat, histogramme de Y (32 à 125), fréquence des 14 gabarits, rotation, miroir, 500 fossiles. Critère de succès : conformité à R35.

**DN13 (R36).** ONESTRUCT-N(`ruined_portal_nether`) : fréquence par région de 640 blocs, Y de pose, 13 gabarits, fraction de blocs de pierre remplacés par `blackstone` (R36 : `replace_with_blackstone`). Critère de succès : conformité à R36.

**DN14 (R41, R40).** ONEFEAT-N pour les 8 minerais et les 2 débris, 10 000 essais chacun : nombre de blocs par veine, distribution de Y, voisins de l'air. Critère de succès : veines égales aux nôtres à l'essai près (trace sur 20 positions).

**DN15 (R42).** ONEFEAT-N pour `spring_open`, `spring_closed`, `spring_delta`, `spring_lava` : fraction de sources posées par tentative, configuration des 6 voisins. Critère de succès : trace égale sur 20 positions par feature.

**DN16 (R43).** ONEFEAT-N pour `glowstone` et `glowstone_extra` : taille des amas, boîte englobante, nombre par chunk. Critère de succès : trace égale sur 20 positions.

**DN17 (R44).** ONEFEAT-N pour les 14 features de végétation et de champignons (grands champignons, vignes, racines, `nether_sprouts`) : trace complète. Critère de succès : trace égale à celle de l'oracle sur 20 positions par feature.

**DN18 (R45).** ONEFEAT-N pour `delta`, `basalt_blobs`, `blackstone_blobs`, `small_basalt_columns`, `large_basalt_columns`, `basalt_pillar` : trace complète. Critère de succès : égale sur 20 positions.

**DN19 (R46).** ONEFEAT-N pour `patch_fire`, `patch_soul_fire`, `brown_mushroom_nether`, `red_mushroom_nether` : trace. Critère de succès : égale sur 20 positions.

**DN20 (R39, R47).** Ordre de décoration : sur MN0 puis MN1 avec deux features de la même étape en conflit (deux features qui posent des blocs différents au même endroit), relevé de l'ordre sur les étapes 7 et 9 pour 5 biomes composés par un datapack de biome mixte ; WP4 de `worldgen-pipeline.md` sur 81 chunks du Nether pour l'indépendance de l'ordre de génération. Critère de succès : ordre total égal à une des hypothèses H-O1 à H-O3 (`worldgen-features-structures.md` R10) ; états canoniques identiques entre les trois ordres.

**DN21 (R21).** Datapack de biome (`nether_wastes` copié) avec 3 listes `features`, puis 12 listes ; chargement. Critère de succès : acceptation pour 3 ; refus pour 12.

## Récapitulatif

- Sondes : DN 23 (DN0 à DN21 et DN8b) ; prérequis : DN0, WP0, lecteur `.mca` indépendant, EULA acceptée.
- Les DN10 et DN11 donnent les statistiques de calage des specs filles (forteresse, bastion) : elles n'en sont pas les tests de conformité, qui sont la comparaison bloc par bloc de `/place structure` entre notre implémentation et l'oracle à graine égale.

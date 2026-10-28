# Vecteurs et sondes : features, structures, heightmaps

Complète `worldgen-features-structures.md` (et `worldgen-surface-carvers.md`). Les vecteurs FV sont des calculs sur les données extraites (preuve C). Les sondes WF, WT, WH sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée par le propriétaire).

Pour ne pas recopier les données de Mojang dans git, les tableaux volumineux ne sont pas reproduits : la commande qui les produit est donnée, avec l'empreinte SHA-256 (16 premiers caractères) de la sortie complète pour détecter une dérive des données extraites, et quelques lignes d'exemple.

## Outil et commandes

L'annexe en fin de fichier est le texte intégral de `wg_ref.py` (SHA-256 du texte de l'annexe, fins de ligne LF : `2e998c851052702dcfe6cb2e81688e9a079c09586db57aef5a2c718a6bd27287`). `REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`. Toutes les commandes sont `python -I wg_ref.py REF <sous-commande>`.

| Sous-commande | Sortie | Empreinte |
|---|---|---|
| `inventory` | nombre de placed et configured features, compteurs de modificateurs et de types, noms de heightmap cités, listes de carvers par biome | 0088bc147d125f39 |
| `steps` | rattachements par étape (2629), features distinctes (171) | 18195797815d65f8 |
| `order` | par étape : features, arêtes, cycle, paires non comparables | b82085e6d41b9a65 |
| `chains` | signatures de chaînes de modificateurs (49 signatures) | c4c087f1e0fe2eea |
| `ores` | les 30 minerais de l'Overworld liés à un biome | e5e3b2b4c8ff8710 |
| `sets` | les 21 ensembles de structures | 1387a4f0cab4afa0 |
| `structures` | les 52 structures | 25405a85163f34b1 |
| `poolrefs` | pools, éléments, projections, listes de processeurs | 507054c337927c06 |
| `jigsaw` | gabarits à jigsaw, joints, priorités | a9b1b95ca0c2f990 |
| `ruletypes` | compteurs des règles de matière (voir l'autre fichier) | 148ca4b2f22af9d2 |
| `carvers` | les trois configurations compactes | 2c3220bd8312f1c1 |
| `surface ...` | arbres de règles | 097bccdae9f64b42 (avec `overworld overworld/surface overworld/biome_surface`) |

## FV : vecteurs calculés

**FV1 (R1, R3, R4, R6, R7, R9) : inventaire.** Sortie de `inventory`, `steps`, `order` :

```
placed_feature 273 feature 240
distinct placed features 171 ; biomes 56 ; lists per biome {11}
bindings per step {0: 0, 1: 112, 2: 63, 3: 118, 4: 5, 5: 0, 6: 1635, 7: 16, 8: 114, 9: 510, 10: 56} total 2629
step 9 features 109 edges 164 cycle False incomparable_pairs 2991 pairs 5886
step 6 features 34 edges 38 cycle False incomparable_pairs 5 pairs 561
step 7 features 7 edges 3 cycle False incomparable_pairs 18 pairs 21
step 2 features 5 edges 4 cycle False incomparable_pairs 1 pairs 10
step 4 features 4 edges 1 cycle False incomparable_pairs 5 pairs 6
```

**FV2 (R3, R31) : chaînes les plus fréquentes** (`chains`) : 53 de `count > in_square > height_range > biome` ; 50 de `block_predicate_filter` seul ; 28 de `rarity_filter > in_square > heightmap > biome > count > offset > block_predicate_filter` ; 19 de `count > in_square > surface_water_depth_filter > heightmap > biome` ; 10 de `count > in_square > heightmap > biome > count > offset > block_predicate_filter`.

**FV3 (R39) : minerais.** Exemples de lignes de `ores` : `ore_andesite_lower ore_andesite size=64 air=0.0 count=2 in_square y=uniform[absolute0..absolute60] biome` ; `ore_diamond_buried ore_diamond_buried size=8 air=1.0 count=4 in_square y=trapezoid[above_bottom-80..above_bottom80] biome` ; `ore_granite_upper ore_granite size=64 air=0.0 rarity=1/6 in_square y=uniform[absolute64..absolute128] biome`. Contrôle de recoupement avec le rapport Overworld 6.1 : 30 lignes, mêmes essais, tailles, probabilités d'air et bornes (aucun écart relevé sur les lignes lues : diamant -144..16, fer haut 80..384, redstone profonde -96..-32, émeraude -16..480 après résolution d'ancres, SV1 de l'autre fichier).

La loi du trapèze (R36, R38) n'a pas de vecteur calculé : sa forme est à mesurer par WF8.

**FV4 (R49, R61) : ensembles.** Exemples de lignes de `sets` : `villages random_spread 34 8 10387312 - - - - - [5 membres de poids 1]` ; `ocean_monuments random_spread 32 5 10387313 triangular` ; `strongholds concentric_rings` ; `pillager_outposts random_spread 32 8 165745296 - 0.2 legacy_type_1 - ('villages', 10)` ; `mineshafts ... spacing 1 separation 0 frequency 0.004 legacy_type_3` ; `buried_treasures ... frequency 0.01 legacy_type_2 locate_offset [9, 0, 9]`. Les 21 ensembles : 18 avec des membres de l'Overworld, plus `nether_complexes` (poids 2 et 3), `nether_fossils`, `end_cities`.

**FV5 (R51, R62) : candidats `random_spread`.** Retiré : les valeurs précédentes reposaient sur une formule de dérivation qui n'est pas retenue (R51) et sur un calcul d'entiers incorrect. Vecteur à produire par l'oracle (WT1) : pour chaque couple (graine, ensemble) de la sonde, la liste des chunks candidats par région, y compris les régions -2 à 1 en X et en Z. Emplacement réservé, sans valeur tant que WT1 n'a pas été exécutée.

**FV6 (R57, R58) : anneaux des strongholds** (C sur S). Tailles des anneaux : 3, 6, 10, 15, 21, 28, 36, 9 (somme 128). Plages publiques de la page « Stronghold » du Minecraft Wiki : anneau 1 de 1280 à 2816 blocs (centre 2048, demi-largeur 768), anneau 8 de 22784 à 24320 blocs (centre 23552, demi-largeur 768). Sous l'hypothèse d'une progression linéaire des centres (S, ajustée sur ces deux seules plages), le pas est (23552 - 2048) / 7 = 3072 blocs et les centres des anneaux 1 à 8 valent 2048, 5120, 8192, 11264, 14336, 17408, 20480, 23552 blocs. Aucun décalage aléatoire ni formule de rayon n'est retenu : WT6 mesure les plages des anneaux 2 à 7.

**FV7 (R9, R10) : ordre de décoration.** Contrôle de cohérence sur un biome : la liste de l'étape 6 de `plains` (29 minerais et disques) respecte l'ordre partiel de l'union ; les 5 paires non comparables de l'étape 6 sont décrites par `order`. Procédure de test de l'implémentation : toute liste globale produite doit (a) contenir exactement les features distinctes de l'étape, (b) respecter chaque arête, (c) coïncider avec WF2.

**FV8 (R68 à R71) : jigsaw.** Sortie de `jigsaw` : 1291 gabarits avec jigsaw, 4272 blocs jigsaw, joints `rollable` 2427 et `aligned` 1845 ; jeux de clés NBT : (`final_state`, `id`, `joint`, `name`, `pool`, `target`) 2925 ; avec `placement_priority` et `selection_priority` 834 ; avec en plus `components` 513 ; priorités (selection, placement) : (0,0) 1297, (1,1) 34, (0,1) 10, (2,2) 3, (0,2) 2, (0,3) 1 (parmi les 1347 blocs qui en portent ; 2925 blocs n'en portent pas). `poolrefs` : 245 pools, 223 repliés sur `minecraft:empty` ; éléments `legacy_single_pool_element` 1665, `single_pool_element` 531, `feature_pool_element` 57, `empty_pool_element` 31, `list_pool_element` 3 ; projections `rigid` 2073, `terrain_matching` 183 ; 40 listes de processeurs (`rule` 39, `protected_blocks` 7, `block_rot` 6, `capped` 4).

## Harnais commun

Voir `worldgen-surface-carvers-vectors.md` (section Harnais commun) et `worldgen-pipeline.md` (WP0). Montages additionnels :

- **ONEFEAT(X, s, k)** : biome unique (préréglage `single_biome_surface` ou `fixed`) dont la liste `features` est vide partout sauf l'étape `s` qui contient `k` placed features `no_op` puis la placed feature X ; carvers vides ; ensembles de structures vides ; terrain FLAT (stone jusqu'à 127) avec règle de matière simple. La sortie est la trace de X seule, à l'indice `k` dans l'étape `s`. Pour comparer, on génère la même zone sans X et on prend la différence bloc par bloc.
- **ONESTRUCT(S)** : tags `has_structure/*` remplacés par tous les biomes, ensemble réduit à S, fréquences remplacées par 1.0 quand la sonde ne mesure pas la fréquence.
- Les jeux de données de sonde sont produits à la volée depuis `.cache/reference/26.3` ; les gabarits `.nbt` de sonde (jigsaw) sont écrits par nous (octets propres), jamais copiés.

## Sondes de features (WF)

**WF1 (R1, R10 à R14, R36).** ONEFEAT pour trois features simples (`ore_dirt`, `spring_water`, `disk_sand` : trouver leurs identifiants dans les données) à l'indice 0 de leurs étapes. Sortie : liste (x, y, z, bloc) des blocs posés dans 17 x 17 chunks. Elle fixe d'abord la forme du placement (essais, hauteurs) avant les graines.

**WF2 (R10).** Test par paire : biomes synthétiques A (liste étape 6 : f1, f2) et B (f3, f2) où f1 et f3 sont incomparables ; deux ordres d'enregistrement des biomes (identifiants alphabétiquement croissants puis décroissants) ; zone à cheval sur la frontière. L'indice de f1 est 0 ou 1 selon la règle de départage : on compare la trace de f1 à ONEFEAT(f1, 6, 0) puis ONEFEAT(f1, 6, 1). Répéter avec trois biomes et chaînes plus longues pour distinguer H-O1, H-O2 et H-O3 (R10) et tout autre candidat ; variante (R96) : ajouter à un biome absent de la source de biomes (`the_end`) une liste d'étape 6 contradictoire, l'ordre observé doit rester identique ; conclusion attendue : une règle de départage qui reproduit les 5 paires de l'étape 6 puis les 2991 paires de l'étape 9 (comparaison de 16 chunks de végétation du monde normal à la graine 12345).

**WF3 (R13).** Identification de la combinaison (étape, indice) : pour une feature X simple, relever la trace de ONEFEAT(X, s, k) pour s dans 0 à 3 et k dans 0 à 40, puis chercher les coïncidences exactes entre traces de couples différents (par exemple ONEFEAT(X, 0, k') égale à ONEFEAT(X, 1, 0) pour un k' à trouver par balayage adaptatif de k' jusqu'à 100 000). Si une coïncidence existe, la combinaison est additive et la constante est le k' trouvé ; sinon, elle ne l'est pas. Comparer aussi ONEFEAT(X, 0, 1) à ONEFEAT(X, 0, 0) (traces différentes attendues) et deux chunks à coordonnées opposées (symétrie des coefficients de la position du chunk). Critère de succès : une relation d'équivalence entre couples (étape, indice) reproduite sur trois features X différentes ; l'absence de coïncidence jusqu'à 100 000 est un résultat négatif à consigner.

**WF4 (R11).** Biome frontière : deux biomes A (avec X) et B (sans X), frontière en travers d'un chunk ; vérifier que X n'apparaît que sur les positions finales de A, y compris quand le décalage de `offset` ou `in_square` fait passer la position de B vers A.

**WF5 (R8, R12).** Étapes : deux features qui écrivent la même position, la première à l'étape s et la seconde à s+1, puis l'ordre inverse : l'état final doit refléter l'ordre des étapes ; vérifier le rôle des structures de l'étape s (WT8) et de l'étape 5 vide.

**WF6 (R14, cas limites).** `count` = 2 sur une position versus deux features successives dans la même étape : la consommation aléatoire partagée est établie si les deux résultats diffèrent d'une façon prévue ; feature qui échoue (hôte inadéquat) : comparer la trace du placement suivant à celle d'une configuration où le premier ne s'exécute pas.

**WF7 (R15).** Feature large (`huge_red_mushroom`, géode) avec `offset` poussant le centre aux positions extrêmes du chunk ; relever les blocs écrits en dehors de la zone 3 x 3.

**WF8 (R35, R36, R37, R38).** Lois des fournisseurs de hauteur et d'entiers : (a) `count` = `very_biased_to_bottom` de 0 à 14 sur 20 000 chunks ONEFEAT (feature = `simple_block` sur la dalle) : histogramme des comptes ; (b) placed feature d'essai dont `height_range` est un `trapezoid` à bornes internes au monde (0 et 100) et plateaux 0, 2 et supérieur à l'écart : histogramme des Y sur 20 000 essais ; (c) diamant isolé (ONEFEAT de `ore_diamond`) : histogramme des Y de la moitié observable. Mesure du nombre de tirages consommés par la différence de trace d'un `count` constant suivant. Critères de succès : support et mode relevés, test du chi-deux contre la loi retenue à la suite de la mesure au seuil de 1 pour cent.

**WF9 (R44, R45).** Dalle de pierre ; ONEFEAT d'un `ore` de `size` 4, 8, 9, 12, 17, 33, 64 avec `discard_chance_on_air_exposure` 0 ; ensuite avec une poche d'air (air creusé à côté), probabilités 0, 0.5, 1 : relever le nombre de blocs conservés, la forme (ellipsoïdes le long d'un segment) et la présence de tirages pour 0 et 1 (comparaison de la trace d'une feature suivante). Critères de succès : voir R44 (égalité exacte à 0 et à 1, 0,5 à 2 pour cent près) et R45 (taille et boîte moyennes à 2 pour cent près sur 1 000 filons).

**WF10 (R16 à R23, R25 à R30).** Modificateurs de placement, un par un. Montage : ONEFEAT d'une placed feature de sonde qui pose un bloc d'or (`simple_block`), dont la chaîne de modificateurs est [M] seul, sur dalle de pierre, zone de 17 x 17 chunks ; la trace est l'ensemble des blocs d'or. (a) chaîne vide : une position par chunk, relever (x, y, z) (R17) ; (b) `count` constant 1, 2 et 4096, `count` uniforme, deux `count` successifs (R18) ; (c) `rarity_filter` de chance 1, 2 et 10 sur 10 000 chunks (R19) ; (d) `in_square` : histogramme des 256 cases sur 10 000 essais (R20) ; (e) `height_range` uniforme et trapèze avec ancres, bornes incluses (R21) ; (f) `heightmap` des cinq types sur des colonnes de sommet connu (herbe, feuillage, eau, glace, verre) (R22) ; (g) `offset` (R23) ; (h) `surface_water_depth_filter` avec une eau de profondeur n - 1, n et n + 1 (R25) ; (i) `environment_scan` vers le haut et le bas, `max_steps` 1, 16 et 32, obstacle (R26) ; (j) `count_on_every_layer` avec trois couches séparées d'air (R27) ; (k) `noise_based_count` et `noise_threshold_count` : corrélation spatiale du nombre d'essais, valeurs aux seuils (R28) ; (l) `surface_relative_threshold_filter` et `fixed_placement` (R29) ; (m) `biome` à plusieurs rangs de la chaîne, avec frontière de biome (R30, voir WF4). Critères de succès : positions et comptes déterministes égaux à la prédiction ; grandeurs aléatoires : chi-deux au seuil de 1 % ; bornes et inclusivité : présence ou absence sans ambiguïté sur 100 000 essais.

**WF11 (R5, R92, R93, R94).** Chargement. Datapack de sonde, un défaut à la fois : (a) placed feature citée par un biome mais absente ; (b) configured feature absente ; (c) type de feature inconnu ; (d) pool, liste de processeurs ou structure cités mais absents ; (e) tag `has_structure/*` manquant. Attendu : le chargement du monde échoue. Critère de succès : le message d'erreur contient l'identifiant ou le chemin de la ressource fautive dans 5 cas sur 5, et aucun fichier de région n'est écrit.

**WF12 (R42, R43).** Cibles de minerai. ONEFEAT d'un `ore` de sonde à deux cibles qui reconnaissent toutes deux la pierre, d'états or puis diamant, puis dans l'ordre inverse ; cibles à test de hauteur sur la plage de recouvrement Y 0 à 8 (R42). Critère de succès : l'état posé est celui de la première cible qui réussit dans 100 % des blocs, pour chaque Y de la plage.

## Sondes de structures (WT)

**WT1 (R51, R52, R61, R62, R63).** ONESTRUCT(`villages`) puis (`monuments`, `mansion`) : graines 0 et 12345 et deux négatives ; lecture de `structures.starts` dans les fichiers de région sur 80 x 80 chunks autour de l'origine. Attendu : au plus un départ par région et par ensemble, dans la plage de R51 ; les candidats relevés alimentent FV5 (vecteur doré). Critère de succès : une fonction de dérivation candidate reproduit 100 pour cent des candidats relevés (au moins 2 000 régions par ensemble, quatre graines).

**WT2 (R53).** Mêmes ensembles avec la fréquence et la méthode d'origine : le sous-ensemble de candidats conservé, en fonction de la graine et de la position, identifie la fonction de filtre. Variante : forcer `frequency_reduction_method` à chacune des quatre valeurs sur un même ensemble (`buried_treasures`).

**WT3 (R55).** `villages` (5 membres) : fréquence relative de chaque membre sur 500 régions, et lien avec la position (graine, région). Variante : retirer un membre invalide (biome) et observer le choix de remplacement.

**WT4 (R54).** `pillager_outposts` avec `frequency` 1.0 en monde où `villages` est actif ; vérifier la distance minimale (en Chebyshev) entre un avant-poste et un village ; faire varier le `chunk_count` de 9 à 11 (inclusivité).

**WT5 (R56).** Source de biomes `checkerboard` de pas minimal (cellules de 4 blocs), deux biomes ; balayer la graine pour obtenir beaucoup de candidats ; le départ existe si le biome au point de référence est valide : la dépendance en (x, y, z) donne le point de référence. Variante : biomes différents au sommet et à la base de la colonne (source multi-biome 3D).

**WT6 (R57 à R59).** Positions de strongholds : pour la graine 12345, `/locate structure minecraft:stronghold` depuis des points choisis (`/execute positioned x y z run locate structure ...`) pour les trois positions de l'anneau 1 (attendu à environ 1400 à 2700 blocs), en désactivant `preferred_biomes` (tag vide) pour lire les positions sans déplacement de biome ; puis avec le tag d'origine pour mesurer le déplacement (rayon et pas de la recherche inconnus). Étendre aux anneaux 2 et 8 (positions publiques attendues, FV6). Critère de succès (R59) : déplacement maximal mesuré (entre la position sans et avec `preferred_biomes`) consigné pour les 128 positions, position finale dans un biome du tag préféré pour toutes les positions déplacées, et règle de recherche ajustée qui reproduit 100 % des positions.

**WT7 (R64).** ONESTRUCT(`ocean_monuments`) : pour un départ connu, lire les `References` des chunks voisins jusqu'à 12 chunks ; relever la distance maximale et si l'emprise du départ (boîte englobante de la pièce) commande la présence (réponse attendue : seulement les chunks rencontrés par la boîte). Variante avec village (emprise large).

**WT8 (R65, R66, R88).** ONESTRUCT avec et sans la structure : différence de blocs ; vérifier la troncature à la boîte du chunk et les écritures successives selon l'étape. Pour `terrain_adaptation` : comparer le terrain (blocs hors pièces) avec et sans structure au voisinage immédiat de villages (`beard_thin`), d'ancient city (`beard_box`), de stronghold (`bury`), de trial chambers (`encapsulate`).

**WT9 (R72, R73).** Village (ensemble `villages` réduit à `village_plains`) : lire la liste `Children` du départ (id, boîte, rotation, élément de pool). Reconstruire la sélection avec nos pools. Attendu : mêmes pièces dans le même ordre. En parallèle, jeu de pools et de gabarits **écrits par nous** : un gabarit à 4 connecteurs de priorités différentes et un pool de deux pièces à poids connus ; vérifier ordre de traitement des connecteurs (`selection_priority` plus grand en premier), ordre de placement (`placement_priority`), `fallback`, profondeur `size`, `joint` rollable ou aligned.

**WT10 (R73, R74, R76, R77).** Boîte englobante et collisions : jeux de gabarits de sondes dont certaines pièces se recouvrent forcément ; vérifier les rejets, la limite `max_distance_from_center`, `use_expansion_hack`, `dimension_padding`, `liquid_settings`, `start_jigsaw_name` avec un gabarit de départ à plusieurs connecteurs de même nom.

**WT11 (R78, R79).** `/place template` (RCON) d'un gabarit de sonde avec rotation (none, 90, 180, 270) et miroir, pour comparer transformation de position et d'état de bloc ; processeurs : une `processor_list` de sonde avec chaque type (`rule`, `block_rot`, `protected_blocks`, `capped`, `gravity`, `jigsaw_replacement`...) appliquée à un gabarit en damier.

## Sondes de heightmaps (WH)

**WH1 (R22, R23, R85 à R88, R91).** Lecture de `Heightmaps` dans des proto-chunks de chaque statut (`Status` du chunk écrit, voir `worldgen-pipeline.md` WP1) : types présents et valeurs en fonction des blocs de la colonne (herbe, feuillages, eau, glace, neige, buisson, toile d'araignée, bambou).

**WH2 (R89).** Colonne creusée par un carver (CLEAN sauf carver, probabilité 1.0) : la valeur de chaque type avant et après.

## Récapitulatif

- Sondes : WF 12, WT 11, WH 2 ; total 25 (hors les 33 de l'autre fichier et les sondes de `worldgen-pipeline.md`).
- Prérequis : WP0, lecteur `.mca` indépendant, EULA acceptée.

## Annexe : `wg_ref.py`

Le texte intégral du script suit (copie de `tools/wg_ref.py` de l'export). Il ne lit que des fichiers du cache d'extraction, ne copie aucune donnée dans git et ne lance pas le jeu.

```python
#!/usr/bin/env python3
"""Read-only inventory of the extracted 26.3 worldgen data (specs worldgen-*.md).

Usage: python -I wg_ref.py REF_DIR SUBCOMMAND [ARGS]
  REF_DIR = .cache/reference/26.3
Subcommands: inventory, surface [RULE...], ruletypes, ores, order, steps, sets, structures,
             jigsaw, poolrefs, carvers, chains
The tool only reads and counts. It copies no Mojang data.
"""
import collections
import glob
import gzip
import json
import os
import struct
import sys

REF = sys.argv[1]
W = os.path.join(REF, "resources/data/minecraft/worldgen")
D = os.path.join(REF, "resources/data/minecraft")


def jl(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def load_dir(sub, base=W):
    out = {}
    root = os.path.join(base, sub)
    for f in glob.glob(root + "/**/*.json", recursive=True):
        out[os.path.relpath(f, root).replace(os.sep, "/")[:-5]] = jl(f)
    return out


def short(x):
    return str(x).replace("minecraft:", "")


def ow_biomes():
    d = jl(os.path.join(REF, "generated/reports/biome_parameters/minecraft/overworld.json"))
    return sorted({e["biome"].split(":")[1] for e in d["biomes"]})


def anchor(a):
    k, v = next(iter(a.items()))
    return "%s=%s" % (k, v)


def cond(c):
    if isinstance(c, str):
        return "@" + short(c)
    t = short(c["type"])
    if t == "biome":
        b = c["biome_is"]
        return "biome(%s)" % (",".join(map(short, b)) if isinstance(b, list) else short(b))
    if t == "noise_threshold":
        return "noise(%s%s,%g..%g)" % (short(c["noise"]), "/3d" if c.get("is_3d") else "",
                                       c["min_threshold"], c["max_threshold"])
    if t == "y_above":
        return "y_above(%s,sdm=%s,asd=%s)" % (anchor(c["anchor"]), c["surface_depth_multiplier"],
                                              c["add_stone_depth"])
    if t == "not":
        return "not(%s)" % cond(c["invert"])
    if t == "vertical_gradient":
        return "vgrad(%s,true<=%s,false>=%s)" % (short(c["random_name"]), anchor(c["true_at_and_below"]),
                                                 anchor(c["false_at_and_above"]))
    if t in ("hole", "steep", "above_preliminary_surface"):
        return t
    if t == "stone_depth":
        return "stone_depth(%s,off=%s,asd=%s,sec=%s)" % (c["surface_type"], c["offset"],
                                                         c["add_surface_depth"], c["secondary_depth_range"])
    if t == "water":
        return "water(off=%s,asd=%s,sdm=%s)" % (c["offset"], c["add_stone_depth"], c["surface_depth_multiplier"])
    return json.dumps(c, sort_keys=True)


def rule(r, ind=0):
    p = "  " * ind
    if isinstance(r, str):
        return p + "@" + short(r) + "\n"
    t = short(r["type"])
    if t == "block":
        return p + "BLOCK " + short(r["result_state"]) + "\n"
    if t == "sequence":
        return p + "SEQ\n" + "".join(rule(x, ind + 1) for x in r["sequence"])
    if t == "condition":
        return p + "IF " + cond(r["if_true"]) + "\n" + rule(r["then_run"], ind + 1)
    if t == "bandlands":
        return p + "BANDLANDS\n"
    if t == "ore_vein":
        return p + "ORE_VEIN " + json.dumps({k: short(v) for k, v in r.items() if k != "type"}, sort_keys=True) + "\n"
    return p + json.dumps(r, sort_keys=True) + "\n"


def cmd_surface(names):
    for n in names or ["overworld", "overworld/surface"]:
        print("=====", n)
        print(rule(jl(os.path.join(W, "material_rule", n + ".json"))))


def cmd_ruletypes():
    types = collections.Counter()
    keys = collections.defaultdict(set)
    files = load_dir("material_rule")

    def walk(n):
        if isinstance(n, dict):
            if "type" in n:
                types[n["type"]] += 1
                keys[n["type"]] |= set(n)
            for v in n.values():
                walk(v)
        elif isinstance(n, list):
            for v in n:
                walk(v)
    for v in files.values():
        walk(v)
    print("files", len(files))
    for t, c in types.most_common():
        print(c, t, sorted(keys[t] - {"type"}))
    for k, v in sorted(load_dir("material_condition").items()):
        print("cond", k, json.dumps(v, sort_keys=True))


def prov(h):
    t = short(h["type"])
    if t == "constant":
        return "C(%s)" % h.get("value")

    def f(a):
        return "%s%s" % (short(next(iter(a))), next(iter(a.values())))
    if "min_inclusive" in h:
        extra = "" if "plateau" not in h else ",plateau=%s" % h["plateau"]
        return "%s[%s..%s]%s" % (t, f(h["min_inclusive"]), f(h["max_inclusive"]), extra)
    return json.dumps(h, sort_keys=True)


def cmd_ores():
    pf = load_dir("placed_feature")
    cf = load_dir("feature")
    bound = {x.split(":")[1] for d in biome_data().values() for lst in d["features"] for x in lst}
    for k in sorted(pf):
        f = cf.get(pf[k]["feature"].split(":")[1])
        if not f or f["type"] != "minecraft:ore" or k not in bound:
            continue
        mods = []
        for m in pf[k]["placement"]:
            t = short(m["type"])
            if t == "count":
                mods.append("count=%s" % json.dumps(m["count"], sort_keys=True))
            elif t == "rarity_filter":
                mods.append("rarity=1/%s" % m["chance"])
            elif t == "height_range":
                mods.append("y=" + prov(m["height"]))
            elif t in ("in_square", "biome"):
                mods.append(t)
            else:
                mods.append(json.dumps(m, sort_keys=True))
        print(k, pf[k]["feature"].split(":")[1], "size=%s" % f["size"],
              "air=%s" % f["discard_chance_on_air_exposure"], " ".join(mods))


def biome_data():
    return {b: jl(os.path.join(W, "biome", b + ".json")) for b in ow_biomes()}


def cmd_order():
    bio = biome_data()
    for st in range(11):
        feats = collections.OrderedDict()
        edges = set()
        for b in sorted(bio):
            lst = bio[b]["features"][st]
            for i, f in enumerate(lst):
                feats.setdefault(f, 0)
                feats[f] += 1
                if i:
                    edges.add((lst[i - 1], f))
        succ = collections.defaultdict(set)
        for a, b in edges:
            succ[a].add(b)
        reach = {}
        state = {"cyc": False}

        def dfs(x, stack):
            if x in reach:
                return reach[x]
            if x in stack:
                state["cyc"] = True
                return set()
            stack.add(x)
            r = set()
            for y in succ[x]:
                r |= {y} | dfs(y, stack)
            stack.discard(x)
            reach[x] = r
            return r
        n = list(feats)
        for f in n:
            dfs(f, set())
        inc = sum(1 for i, a in enumerate(n) for b in n[i + 1:] if b not in reach[a] and a not in reach[b])
        print("step", st, "features", len(n), "edges", len(edges), "cycle", state["cyc"],
              "incomparable_pairs", inc, "pairs", len(n) * (len(n) - 1) // 2)


def cmd_chains():
    pf = load_dir("placed_feature")
    sig = collections.Counter(" > ".join(short(m["type"]) for m in v["placement"]) for v in pf.values())
    for sg, c in sig.most_common():
        print(c, sg or "(empty)")


def cmd_steps():
    bio = biome_data()
    per = collections.Counter()
    for d in bio.values():
        for st, lst in enumerate(d["features"]):
            per[st] += len(lst)
    print("bindings per step", dict(sorted(per.items())), "total", sum(per.values()))
    print("distinct placed features", len({f for d in bio.values() for lst in d["features"] for f in lst}))
    print("biomes", len(bio), "lists per biome", {len(d["features"]) for d in bio.values()})


def cmd_inventory():
    pf = load_dir("placed_feature")
    cf = load_dir("feature")
    mods = collections.Counter(m["type"] for v in pf.values() for m in v["placement"])
    print("placed_feature", len(pf), "feature", len(cf))
    print("modifiers", dict(mods.most_common()))
    print("feature types", dict(collections.Counter(v["type"] for v in cf.values()).most_common()))
    hm = collections.Counter()
    names = {"MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES", "OCEAN_FLOOR", "OCEAN_FLOOR_WG",
             "WORLD_SURFACE", "WORLD_SURFACE_WG"}
    for t in (pf, load_dir("structure"), cf):
        for s in json.dumps(t).split('"'):
            if s in names:
                hm[s] += 1
    print("heightmap names used (placed_feature+structure+feature)", dict(hm))
    cs = collections.Counter(tuple(jl(os.path.join(W, "biome", b + ".json"))["carvers"]) for b in ow_biomes())
    print("carver lists by OW biome", dict(cs))


def cmd_carvers():
    for n in ("cave", "cave_extra_underground", "canyon"):
        print(n, json.dumps(jl(os.path.join(W, "carver", n + ".json")), sort_keys=True, separators=(",", ":")))


def cmd_sets():
    for k, d in sorted(load_dir("structure_set").items()):
        p = d["placement"]
        ex = p.get("exclusion_zone")
        print(k, short(p["type"]), p.get("spacing"), p.get("separation"), p.get("salt"), p.get("spread_type", "-"),
              p.get("frequency", "-"), p.get("frequency_reduction_method", "-"), p.get("locate_offset", "-"),
              (short(ex["other_set"]), ex["chunk_count"]) if ex else "-",
              [(short(s["structure"]), s["weight"]) for s in d["structures"]])


def cmd_structures():
    for k, d in sorted(load_dir("structure").items()):
        print(k, short(d["type"]), d["step"], d.get("terrain_adaptation", "none"), d.get("size", "-"),
              d.get("max_distance_from_center", "-"), json.dumps(d.get("start_height", "-"), sort_keys=True),
              d.get("project_start_to_heightmap", "-"), d.get("use_expansion_hack", "-"),
              d.get("start_jigsaw_name", "-"), d.get("biomes"))


def nbt(b):
    p = [0]

    def u(fmt):
        n = struct.calcsize(fmt)
        v = struct.unpack_from(fmt, b, p[0])
        p[0] += n
        return v[0]

    def s():
        n = u(">H")
        r = b[p[0]:p[0] + n].decode("utf8", "replace")
        p[0] += n
        return r

    def pay(t):
        if t in (1, 2, 3, 4, 5, 6):
            return u(">" + "bhiqfd"[t - 1])
        if t == 8:
            return s()
        if t == 9:
            tt = u(">b")
            n = u(">i")
            return [pay(tt) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = u(">b")
                if tt == 0:
                    return d
                k = s()
                d[k] = pay(tt)
        if t == 11:
            n = u(">i")
            return [u(">i") for _ in range(n)]
        raise ValueError(t)
    t = u(">b")
    s()
    return pay(t)


def cmd_jigsaw():
    joints, keys, nt, nj = collections.Counter(), collections.Counter(), 0, 0
    pri = collections.Counter()
    for f in glob.glob(D + "/structure/**/*.nbt", recursive=True):
        d = nbt(gzip.open(f).read())
        pal = (d.get("palettes") or [d["palette"]])[0]
        ids = {i for i, p in enumerate(pal) if p.get("id") == "minecraft:jigsaw"}
        c = 0
        for bl in d["blocks"]:
            if bl["state"] in ids:
                c += 1
                n = bl.get("nbt", {})
                joints[n.get("joint")] += 1
                keys[tuple(sorted(n))] += 1
                pri[(n.get("selection_priority"), n.get("placement_priority"))] += 1
        if c:
            nt += 1
            nj += c
    print("templates with jigsaw", nt, "jigsaw blocks", nj, "joints", dict(joints))
    print("nbt key sets", dict(keys))
    print("(selection,placement) priorities", dict(pri.most_common(10)))


def cmd_poolrefs():
    pools = load_dir("template_pool")
    et = collections.Counter()
    proj = collections.Counter()

    def walk(e):
        et[e["element_type"]] += 1
        proj[e.get("projection")] += 1
        for x in e.get("elements", []):
            walk(x)
    for v in pools.values():
        for el in v["elements"]:
            walk(el["element"])
    print("pools", len(pools), "elements", dict(et), "projection", dict(proj))
    print("fallback empty", sum(1 for v in pools.values() if v["fallback"] == "minecraft:empty"))
    pl = load_dir("processor_list")
    pt = collections.Counter()

    def pw(x):
        if isinstance(x, dict):
            if "processor_type" in x:
                pt[x["processor_type"]] += 1
            for v in x.values():
                pw(v)
        elif isinstance(x, list):
            for v in x:
                pw(v)
    pw(pl)
    print("processor lists", len(pl), "processor types", dict(pt))


COMMANDS = {"inventory": lambda a: cmd_inventory(), "surface": cmd_surface, "ruletypes": lambda a: cmd_ruletypes(),
            "ores": lambda a: cmd_ores(), "order": lambda a: cmd_order(), "steps": lambda a: cmd_steps(),
            "sets": lambda a: cmd_sets(), "structures": lambda a: cmd_structures(),
            "jigsaw": lambda a: cmd_jigsaw(), "poolrefs": lambda a: cmd_poolrefs(),
            "carvers": lambda a: cmd_carvers(), "chains": lambda a: cmd_chains()}

if __name__ == "__main__":
    COMMANDS[sys.argv[2]](sys.argv[3:])
```

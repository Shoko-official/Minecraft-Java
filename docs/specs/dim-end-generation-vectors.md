# Vecteurs et sondes : génération de l'End

Complète `dim-end-generation.md`. Les vecteurs DX sont des calculs sur les données extraites (preuve C), produits par `dim_ref.py` (annexe de [dim-definitions-portals-vectors.md](dim-definitions-portals-vectors.md), SHA-256 `9170d0b16e1ff59b8813b9799d61503a290621ce94ffbaac827322a141253ff6`). Les sondes DE sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée) ; chacune dit la commande, l'entrée et la sortie attendue.

Les tableaux volumineux ne sont pas reproduits : la commande est donnée avec l'empreinte SHA-256 complète de la sortie et quelques lignes d'exemple. `REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`.

## Commandes

| Commande (`python -I dim_ref.py REF ...`) | Sortie | SHA-256 de la sortie |
|---|---|---|
| `noise end` | réglages, routeur, rampes, `islands`, `sloped_cheese` (R1 à R4) | `9b7030f7ada06e5d9357a14125758fd3a8172ba32dbd422af6308e0aa22363ed` |
| `surface-nether` | inclut la règle `end` (un seul bloc, R11) | `ce207e449550eee6a5bcb6df580ced33b24aa146ef2fd9c3e44f582a9f1787f8` |
| `biomes` | 5 biomes de l'End, listes de features, créatures (R13, R16) | `69818cd9a61bb8d4721fa80de2ae9bff6a2715527dfef7f5ee40cf31719cee90` |
| `features` | 5 placed features de l'End avec leurs modificateurs (R16, R18, R25, R28, R29) | `646367d45fb2b03a75fd0b732318e6b8ed1ef4e9a415bd20b53c7d386b31486b` |
| `structures` | `end_cities`, `end_city`, tag, 20 gabarits, pièce `ecp`, butin (R31, R32) | `121ff06ca97a0f9c6baaf6c4e3447e46e9ad3b5bba32f60f48b85b45a1ece1cd` |
| `portal` | placed features `end_platform`, `end_gateway_return`, `end_spike` (R18, R20, R25) | `353052e5640516ccdfe336f7484eb8263167be86aaed6cb6a575e438d10de6fe` |
| `vectors` | profil de l'End (DX13), régions (DX14), anneau (DX16), piliers (DX17) | `303e51f3f85ea325ecdcf5d77594d02a71f0726a89ec2fd54868f70960914497` |

## DX : vecteurs calculés

**DX12 (R1 à R4) : réglages** (sortie de `noise end`) : `noise` min_y 0 height 128 ; `sea_level` 0 ; routeur : six constantes 0.0 et `erosion` = `minecraft:end/islands` ; cellules 8 x 4 ; rampes : Y 4 -> 32 (0.0 -> 1.0), `first` -0.234375 ; Y 56 -> 312 (1.0 -> 0.0), `first` -23.4375 ; facteur 0.64.

**DX13 (R6, R7) : terme central et profil de la plaque** (sortie de `vectors`, section DX13). `A(d) = (clamp(100 - d, -100, 80) - 8) / 128` :

| d | 0 à 20 | 21 | 50 | 80 | 90 | 92 | 93 | 100 | 150 | 200 et plus |
|---|---|---|---|---|---|---|---|---|---|---|
| A | 0.5625 | 0.554688 | 0.328125 | 0.09375 | 0.015625 | 0 | -0.007812 | -0.0625 | -0.453125 | -0.84375 |

Densité de la plaque (islands = 0.5625, bruit nul), avant `squeeze` :

| Y | 3 | 4 | 8 | 12 | 13 | 16 | 32 à 56 | 60 | 61 | 62 | 63 | 70 | 100 | 127 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| densité | -0.15 | -0.15 | -0.077143 | -0.004286 | +0.013929 | +0.068571 | +0.36 | +0.12 | +0.06 | 0 | -0.06 | -0.48 | -2.28 | -3.9 |

Bande solide sans bruit ni interpolation : Y 13 à 61 (Y 62 : zéro analytique, non utilisable).

**DX14 (R31) : régions de cités** (sortie de `structures`) : `end_cities` : espacement 20, séparation 11, sel 10387313, triangulaire -> région de 320 blocs, 9 positions candidates par axe (144 blocs), décalage de chunk de 0 à 8.

**DX15 (R13, R16, R25, R28, R29, R32) : inventaire** (sorties de `biomes`, `features`, `structures`, `portal`) : 5 biomes ; listes de features : `the_end` 11 (étapes 4 et 10), `small_end_islands` 1 (étape 0), `end_highlands` 10 (étapes 4 et 9), `end_midlands` 0, `end_barrens` 0 ; 5 placed features ; `end_gateway_return` : `rarity_filter` 700, `in_square`, `heightmap` `MOTION_BLOCKING`, `offset`, `biome` ; feature `exact` vrai, `exit` [100, 50, 0] ; `end_platform` : `fixed_placement` [100, 49, 0] ; `end_spike` : `spikes` = [] ; `end_island_decorated` : rarity 14, count pondéré 1 (3) et 2 (1), Y 55 à 70 ; `chorus_plant` : count 0 à 4 ; 20 gabarits de cité.

**DX16 (R30) : anneau de 20 passerelles** (sortie de `vectors`, section DX16), (x, z) à Y 75 pour k de 1 à 20, arrondi par plancher puis par troncature (14 positions diffèrent, marquées `*`) :

```
k= 1 (91,29)   k= 2 (77,56)   k= 3 (56,77)   k= 4 (29,91)   k= 5 (0,96)
k= 6 (-30,91)* k= 7 (-57,77)* k= 8 (-78,56)* k= 9 (-92,29)*  k=10 (-96,0)
k=11 (-92,-30)* k=12 (-78,-57)* k=13 (-57,-78)* k=14 (-30,-92)* k=15 (-1,-96)*
k=16 (29,-92)* k=17 (56,-78)*  k=18 (77,-57)*  k=19 (91,-30)*  k=20 (96,-1)*
```

(positions de plancher ; la troncature donne par exemple k = 6 : (-29, 91), k = 15 : (0, -96), k = 20 : (96, 0)). Les 4 positions sur les axes (k = 5, 10, 15, 20) dépendent de l'erreur d'arrondi : DE13 les tranche.

**DX17 (R19) : piliers** (sortie de `vectors`, section DX17) : 10 centres (x, z) tirés de [W2] : (-42, -1), (-34, -25), (-34, 24), (-13, -40), (-13, 39), (12, -40), (12, 39), (33, -25), (33, 24), (42, 0) ; distances à l'origine de 40,804 à 42,202 ; sommets Y 76, 79, 82, 85, 88, 91, 94, 97, 100, 103 ; cages aux sommets 79 et 82.

## Montages

Conventions de `worldgen-surface-carvers-vectors.md` (graine 12345 sauf indication ; zones à cheval sur 0 ; `forceload`, attente W, `save-all flush`, arrêt, lecture de `dimensions/minecraft/the_end/region/r.*.mca`) ; datapacks produits à la volée, jamais commités.

- **ME0** : monde normal, End intact. **ME1** : listes `features` vides sur les 5 biomes. **ME2** : ME1 et `structure_set` `end_cities` vidé. Différence ME0 - ME1 : features (piliers, plateforme, îles décorées, chorus, passerelles de retour) ; ME1 - ME2 : cités.
- **MEV(F, mode)** : `noise_settings/end.json` remplacé par une copie dont le routeur est inchangé et dont `final_density` n'a ni `interpolated`, ni `beardifier`, ni `blend_density` : (mode R, rampe) `add(F, gradient(axe y, de 0 valeur 1.0 à 127 valeur -1.0))` : une colonne est solide pour Y < 127 (1 + F) / 2, d'où F au demi-pas de 0,0079 ; (mode C, constante) `add(F, c)` pour c dans un réseau : un bit par bloc et par monde, bissection sur 24 mondes pour une précision de 1e-6 ; `F` est une référence de fonction (`minecraft:end/islands`, une fonction à `end_outer_islands` inline, `minecraft:end/base_3d_noise`) éventuellement multipliée par une constante pour tenir dans [-1, 1]. C'est la **règle graduée** de `worldgen-noise-density.md` (vocabulaire).
- **MRE(C)** : `material_rule` de l'End remplacée par `condition(C) -> block(gold_block)` : un bloc d'or signifie « C vraie » (C est `biome(B)` pour la lecture des biomes). **MRE0** : `block(gold_block)` seul.
- **FLATE(Y, bloc)** : `final_density` remplacée par un dégradé positif sous Y ; règle de matière `end` ; sol d'End plat ; **FLATE(-1)** : tout est de l'air.
- **ONEFEAT-E(X, s)** : dimension `probe:e` de type `the_end`, biome fixe du biome de la feature, FLATE, listes `features` vides sauf l'étape `s` contenant la placed feature X ; sortie = trace de X seule (différence avec la même zone sans X).
- **ONESTRUCT-E(S)** : tag `has_structure/end_city` remplacé par le biome fixe, ensemble réduit ; placement par `/place structure minecraft:end_city`.

## Sondes (DE)

**DE0 (prérequis).** Monde neuf, graine 12345, MRE0 : le chunk (0, 0) de l'End est en or là où il y avait du solide, aucune erreur ; variante : remplacement de `noise_settings/end.json` après la création : pas d'effet sur les chunks déjà générés.

**DE1 (R5, R6).** MEV(`end/islands`, mode R) puis mode C : lecture de `end/islands` en 50 points de d = 0 à 300 (axes et diagonales), 3 graines. Critère de succès : valeur lue égale à `max(A(d), end_outer_islands)` avec A de DX13 à 1e-5 près là où l'autre terme est plus petit ; le terme central ne dépend pas de la graine.

**DE2 (R8).** MEV(`end_outer_islands`, mode R puis mode C) : valeurs sur une grille de pas 8 couvrant (-2 048..2 048)² (512 x 512 = 262 144 points), 3 graines dont -1 et `Long.MIN_VALUE` ; cartes enregistrées dans `reports/probes/` (non commitées). Plage mesurée : minimum et maximum (attendus -0.84375 et 0.5625 [W7]). Analyses prévues : structure de la carte (support, isotropie, périodicité, îlots connexes, corrélation entre graines), dépendance à Y (lecture à Y 0 et Y 60). Coût : 65 536 chunks et 24 mondes par graine. Critère de succès : une implémentation candidate reproduit les 262 144 valeurs de chaque graine à 1e-4 près ; l'échec de toute hypothèse testée confirme B-E1.

**DE3 (R9, R12, R38).** ME2, 3 graines (dont 0 et -1) : sur les 1 256 colonnes de la plaque (d <= 20), fraction de blocs solides par Y de 4 à 127 ; en parallèle MEV(`end/base_3d_noise`, mode C) en 2 000 positions de la plaque. Critère de succès : fraction égale à la nôtre à 0,5 point près pour chaque Y ; valeur du bruit égale à 1e-6 près ; `A` identique pour les 3 graines.

**DE4 (R11).** ME2 : 100 chunks autour de (0, 0) et de (1 100, 0) ; liste des types de blocs. Critère de succès : seulement `end_stone` et `air`.

**DE5 (R14, R15).** Pour 10 000 colonnes de quart réparties sur (-3 000..3 000)² : valeur de `end/islands` (DE1/DE2, mode C) et biome (MRE(`biome(B)`) pour B dans les 5 biomes, une lecture par biome) ; distance à l'origine. Variante : biomes à tous les Y de 5 colonnes. Critère de succès : (a) pour d >= R0 la relation biome / valeur est une fonction à 4 intervalles disjoints ; (b) les seuils t1, t2, t3 donnés à 1e-4 près ; (c) R0 à un chunk près ; (d) le biome est le même sur les 64 couches de quart.

**DE6 (R19).** ME2 (les piliers sont de l'étape 4, ME1 les retirerait) sur 20 graines : pour chaque graine, centres, sommets, rayons, cages et position du cristal des 10 piliers (lecture des chunks de (-4..4, -4..4)) ; deux mondes de même graine. Critère de succès : positions des centres identiques entre graines ; permutation de chaque graine identique entre deux mondes ; DX17 vérifié (sommets, rayons, cages).

**DE7 (R20).** ME0 : volume de 7 x 7 x 7 centré en (100, 49, 0), chunks (6, -1) et (6, 0) ; mêmes mesures avec ME1 (plateforme absente). Critère de succès : 25 blocs d'obsidienne à Y 48 et trois couches d'air au-dessus, ou la valeur relevée.

**DE8 (R21, R24).** ME0 : `execute in minecraft:the_end run forceload add 0 0` sans entité : dragon ? fichier ? Puis `execute in minecraft:the_end run summon armor_stand 0 80 0` : lecture à 0, 10, 19, 20, 21, 40 ticks de `execute if entity @e[type=ender_dragon]`, de `ender_dragon_fight.dat` (clés), de `chunk_tickets.dat`, de la structure de bedrock en (0, ~64, 0). Critère de succès : instant d'apparition du dragon à un tick près, clés de l'état, tickets `dragon` (rayon, durée).

**DE9 (R22, R23).** Suite de DE8 : `kill @e[type=ender_dragon]` ; lecture à 0, 100, 200, 201, 300, 600 ticks de tous les blocs non air d'un volume de 11 x 12 x 11 centré en (0, 64, 0) ; Y du portail ; œuf ; état `previously_killed` ; 5 graines. Variante : seconde mort après réinvocation (DE13). Critère de succès : liste de blocs du portail de sortie relevée et identique d'une graine à l'autre à la translation en Y près ; œuf à la première mort seulement.

**DE10 (R26, R27).** ONEFEAT-E(`end_gateway_return`, 4) sur FLATE(60, end_stone), 10 positions : forme dans un volume de 9 x 15 x 9 ; `place feature minecraft:end_gateway_return` ; fréquence par chunk de hautes terres dans ME0 (20 000 chunks de hautes terres, carte DE5 requise). Critère de succès : forme identique à celle de l'oracle ; fréquence dans l'intervalle de Poisson de 1/700.

**DE11 (R28).** ONEFEAT-E(`end_island_decorated`, 0) en biome fixe `small_end_islands` sur FLATE(-1) : 1 000 positions ; nombre de blocs, boîte englobante, profil par Y. Critère de succès : trace égale sur 30 positions ; fréquence par chunk 0,0893 attendue (C).

**DE12 (R29).** ONEFEAT-E(`chorus_plant`, 9) en biome fixe `end_highlands` sur FLATE(60, end_stone) : 200 essais ; plantes, fleurs, boîte. Critère de succès : trace égale sur 30 essais ; 2 essais par chunk en moyenne (C).

**DE13 (R30).** ME0 : réinvocation sans joueur : 4 `summon end_crystal` aux quatre bords du portail de sortie (positions relevées par DE9) ; `tick step` 604 ; `kill @e[type=ender_dragon]` ; répéter 21 fois ; après chaque mort, lecture des blocs `end_gateway` de l'anneau (`execute if block <x> 75 <z> minecraft:end_gateway` sur les 20 positions de DX16 sous les deux arrondis) ; 5 graines. Critère de succès : une passerelle créée par mort, aucune à la 21e ; positions égales à DX16 (plancher ou troncature, tranche les 4 positions d'axe) ; permutation d'ordre par graine.

**DE14 (R33, R34, R35).** ONESTRUCT-E(`end_city`) : 100 cités par `/place structure minecraft:end_city` sur des sols plats de hauteur variable ; relever par cité le nombre de pièces par gabarit (20), la présence et la position du vaisseau, la boîte englobante, la rotation, le Y de départ contre la heightmap, les shulkers posés. En monde normal : `execute in minecraft:the_end run locate structure minecraft:end_city` depuis 20 points de départ, distance minimale à l'origine, 100 départs lus dans les fichiers de région (`structures.starts`). Critère de succès : fréquence de vaisseau (chaque cité a au plus un), égalité de la structure à graine égale, distance minimale conforme à R34.

**DE15 (R37).** WP4 de `worldgen-pipeline.md` sur 81 chunks centrés sur (0, 0) : trois ordres de génération (un forceload, spirale, aléatoire), deux nombres de fils ; état canonique du pilier (-42, -1), de la plateforme, de la passerelle de retour si présente. Critère de succès : états identiques.

## Récapitulatif

- Sondes : DE 16 (DE0 à DE15) ; prérequis : DE0, WP0, lecteur `.mca` indépendant, EULA acceptée.
- Les sondes DE2 et DE5 sont celles qui décident de B-E1 : si les cartes mesurées ne s'expliquent par aucune hypothèse simple, l'algorithme des îles extérieures reste une dérogation à demander au propriétaire.

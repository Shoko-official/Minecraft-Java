# Génération de l'End : île centrale, îles extérieures, biomes, piliers, passerelles, cités

Statut : brouillon
Jalon : M10
Risque : A
Règles : 38 (R1 à R38). D 9, D/S 6 (valeur lue, effet S), D/C 1, C 2, C sur S 6, M 0, S 14.
Mise à jour : 2026-10-28 (première version)

Troisième spec du jalon M10. Comme `dim-nether-generation.md`, elle ne répète pas l'Overworld (`worldgen-noise-density.md`, `worldgen-surface-carvers.md`, `worldgen-features-structures.md`, `worldgen-pipeline.md`) et ne spécifie que ce qui est propre à l'End. La définition de la dimension, le portail de l'End et le comportement des passerelles à l'usage sont dans `dim-definitions-portals.md`.

## Périmètre

Couvre :

- les réglages de bruit `end`, la fonction d'îles (île centrale calculable, îles extérieures), la forme du terrain ;
- la source de biomes `the_end` et la grille de biomes ;
- l'absence de carvers, de bedrock et de surface ;
- les piliers d'obsidienne (positions, hauteurs, cages, cristaux), la plateforme d'arrivée, la passerelle de retour, les îles décorées, les plantes de chorus ;
- l'état du monde que laisse le combat du dragon : création de l'état, portail de sortie, œuf, anneau de 20 passerelles ;
- la cité de l'End et le vaisseau (placement, étape, composition observable) ;
- les différences de pipeline.

Ne couvre pas :

- l'IA, les attaques, la santé et la mort du dragon, les cristaux comme entités, la réinvocation (M7, spec d'entités) ; seules sont spécifiées les écritures de blocs et les fichiers d'état que ces événements produisent ;
- le comportement d'une passerelle à l'usage (`dim-definitions-portals.md` R51 à R56) ;
- les gabarits de la cité (20 `.nbt`) et leur assemblage procédural : spec fille `dim-end-city` ;
- le butin, les créatures des gabarits (shulkers), les coffres (ADR 0004, M7) ;
- le ciel, les étoiles, le brouillard (M9).

## Sources

Niveaux de preuve : D, C, M, S, **C sur S** comme dans `dim-nether-generation.md`. Toute règle S porte un comportement **provisoire** (non fidèle tant que la sonde n'a pas répondu) et sa **sonde** avec critère de succès.

Données extraites (`$REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3\`, jeu 26.3, data version 5023) :

- [E1] `resources/data/minecraft/worldgen/noise_settings/end.json`, `density_function/end/{islands,sloped_cheese,base_3d_noise}.json`, `material_rule/end.json`.
- [E2] `generated/reports/registries.json` : registres `worldgen/biome_source` (`the_end`), `worldgen/density_function_type` (dont `end_outer_islands`, `slice`, `distance_to_point`), `worldgen/feature_type` (`end_spike`, `end_platform`, `end_gateway`, `end_island`, `chorus_plant`), `ticket_type` (`dragon`), `worldgen/structure_type` (`end_city`), `worldgen/structure_piece` (`ecp`).
- [E3] `worldgen/biome/{the_end,small_end_islands,end_midlands,end_highlands,end_barrens}.json`, `tags/worldgen/biome/is_end.json`.
- [E4] `worldgen/placed_feature/{end_spike,end_platform,end_island_decorated,end_gateway_return,chorus_plant}.json` et `worldgen/feature/{end_spike,end_platform,end_island,end_gateway_return,chorus_plant}.json`.
- [E5] `worldgen/structure_set/end_cities.json`, `worldgen/structure/end_city.json`, `tags/worldgen/biome/has_structure/end_city.json`, `structure/end_city/*.nbt` (20), `loot_table/chests/end_city_treasure.json`.

Documentation publique, consultée le 2026-10-28 (pages lues par un résumé automatique : valeurs indicatives) :

- [W1] https://minecraft.wiki/w/The_End (plateforme (100, 49, 0), portail de sortie (0, 64, 0), 10 piliers, vide d'environ 1000 blocs avant les îles extérieures, 5 biomes ; ne donne ni rayon de l'île centrale ni règle de biome).
- [W2] https://minecraft.wiki/w/Obsidian_Pillar (10 piliers, positions fixes, hauteurs et rayons par pilier, cages, sommet de bedrock, ordre aléatoire).
- [W3] https://minecraft.wiki/w/Ender_Dragon (apparition 20 ticks après la première entrée, portail de sortie après 200 ticks de mort, œuf à la première mort seulement, anneau de passerelles ; ne donne pas la hauteur exacte du portail ni les piliers).
- [W4] https://minecraft.wiki/w/End_gateway (anneau, cooldown, destination).
- [W5] https://minecraft.wiki/w/End_City (grille de 20 chunks, décalage 0 à 8, composition, 12,5 % de pont vers un vaisseau, au plus un vaisseau).
- [W6] https://minecraft.wiki/w/Configured_feature (`end_spike` : liste `spikes` vide = piliers aléatoires par défaut ; `end_gateway` : `exact`, `exit` ; `end_island`, `end_platform`, `chorus_plant` : configuration vide, aucune description).
- [W7] https://minecraft.wiki/w/Density_function (`end_outer_islands`, ex-`end_islands` : bruit d'îles de l'End, sans autre description ; plage de sortie -0.84375 à 0.5625, rien d'autre ; `distance_to_point`, `slice`).
- [W8] https://minecraft.wiki/w/World_generation (End : l'érosion seule ; dans un rayon de 1024 blocs de l'origine le biome est `the_end`, au-delà l'érosion choisit les quatre autres ; pierre d'End partout).
- [W9] https://minecraft.wiki/w/Biome (les cinq biomes de l'End et leur rôle ; rayon de 1000 blocs pour `the_end`).
- [W10] https://minecraft.wiki/w/End_Highlands (chorus et cités seulement dans ce biome ; passerelles ; pas de seuil).

Les pages se contredisent sur le rayon du biome central (1000 contre 1024), sur le rayon de l'anneau des piliers (42 pour [W1], 43 pour [W2] dont les positions donnent 41 à 42) et sur le Y de la plateforme (48 ou 49). Ces points sont des questions de sonde (R14, R19, R20).

Relectures ciblées le 2026-10-29 : End_gateway ([W4], anneau de R30), World_generation ([W8], seuils de R14), The_End ([W1], plateforme de R20 : 5 x 5 x 1 d'obsidienne et trois couches d'air).

Rapports locaux : non utilisés (ADR 0002, 2b).

Sondes : DE0 à DE15, dans [dim-end-generation-vectors.md](dim-end-generation-vectors.md) avec les montages ME0 à ME2, MEV et MRE, les vecteurs et les commandes de `dim_ref.py` (annexe de [dim-definitions-portals-vectors.md](dim-definitions-portals-vectors.md)).

## Règles

### A. Réglages de bruit et forme du terrain

| Réf | Règle | Preuve |
|---|---|---|
| R1 | `noise_settings/end.json` : `noise.min_y` 0, `noise.height` 128 (Y 0 à 127) ; `sea_level` 0 ; bloc par défaut `end_stone` ; fluide par défaut `air` ; `legacy_random_source` vrai ; `disable_mob_generation` vrai ; `spawn_target` vide ; `material_rule` `minecraft:end` ; pas d'objet `aquifers`. Commande : `dim_ref.py REF noise end`. | D |
| R2 | Routeur : `chunk_surface_level`, `continents`, `depth`, `ridges`, `temperature`, `vegetation` valent la constante 0.0 ; `erosion` = la référence `minecraft:end/islands` ; `final_density` en ligne. | D |
| R3 | `final_density` = `add(squeeze(interpolated(cellules 8 x 4, mul(blend_density(lerp_B), 0.64))), beardifier)`. `lerp_B` : alpha = dégradé en Y de 4 (valeur 0.0) à 32 (valeur 1.0), `first` = -0.234375, `second` = `lerp_H`. `lerp_H` : alpha = dégradé en Y de 56 (valeur 1.0) à 312 (valeur 0.0), `first` = -23.4375, `second` = `minecraft:end/sloped_cheese` = `add(end/islands, end/base_3d_noise)`. `end/base_3d_noise` : `old_blended_noise`, `xz_scale` 0.25, `y_scale` 0.25, `xz_factor` 80.0, `y_factor` 160.0, `smear_scale_multiplier` 4.0. Sémantique des nœuds : `worldgen-noise-density.md` R12 à R17, R22, R23, R30. | D |
| R4 | `end/islands` = `cache(max(A, end_outer_islands))` avec `A` = `slice(axe y, coordonnée 0, mul(sub(clamp(sub(100.0, distance_to_point(point (0, 0, 0), métrique euclidienne)), -100.0, 80.0), 8.0), 0.0078125))`. Les types `slice`, `distance_to_point` et `end_outer_islands` sont enregistrés mais absents du graphe de l'Overworld (`worldgen-noise-density.md` R5). Commande : `noise end`. | D |
| R5 | Sémantique de `slice` : évalue `input` avec la coordonnée de l'axe fixée à `coordinate` (ici Y = 0) : `A` ne dépend donc que de X et Z. `distance_to_point` euclidienne : racine de la somme des carrés des écarts aux trois coordonnées ([W7]). Défaut de métrique absent de la page. Provisoire : celle de [W7] ; la valeur de `A` en (x, y, z) est celle en (x, 0, z), distance horizontale d = racine(x² + z²). Sonde : DE1 ; critère de succès : valeur lue de `end/islands` (règle graduée MEV) en 50 points d de 0 à 300 sur 3 graines égale à `max(A(d), end_outer_islands)` avec A de R6 à 1e-5 près là où `end_outer_islands` est inférieure à `A`. | S [W7] |
| R6 | Valeurs de `A` (C, commande `vectors`, section DX13) : 0.5625 pour d <= 20 ; décroissance linéaire de 0.5625 (d = 20) à -0.84375 (d = 200) à raison de 1/128 par bloc, nulle à d = 92 ; -0.84375 pour d >= 200 (avec le calcul `(clamp(100 - d, -100, 80) - 8) / 128`). Exemples : d = 21 : 0.554688 ; d = 50 : 0.328125 ; d = 80 : 0.09375 ; d = 93 : -0.0078125 (exact ; la table DX13 affiche 6 décimales) ; d = 150 : -0.453125. | C |
| R7 | Profil sans bruit de la plaque centrale (C sur S ; `lerp` et `gradient` de `worldgen-noise-density.md` R15, R17 ; `base_3d_noise` pris nul ; d <= 20, donc `islands` = 0.5625 si `end_outer_islands` est inférieure) : densité avant `squeeze` = 0.64 fois `(1 - a_bas) (-0.234375) + a_bas [(1 - a_haut) (-23.4375) + a_haut 0.5625]` avec a_bas = clamp((Y - 4) / 28), a_haut = clamp((312 - Y) / 256). Elle est strictement positive pour Y de 13 à 61, nulle à Y 62 (0 analytique : indéterminé), négative en dessous de 13 et au-dessus de 62. Hors plaque, l'île est plus mince : la densité à Y fixé suit `islands` + n (n = bruit hérité). À d = 92 le terme `A` s'annule : l'île sans bruit a un rayon de l'ordre de 92 blocs à Y de 32 à 56 (a_bas = a_haut = 1). Table DX13. Provisoire : ce profil sur la plaque. Sonde : DE3 ; critère de succès : voir R9 (mêmes seuils, fraction de blocs solides par Y égale à celle de notre implémentation à 0,5 point près). | C sur S |
| R8 | `end_outer_islands` : plage de sortie -0.84375 à 0.5625 ([W7]) ; algorithme, paramètres et dépendance à la graine inconnus : aucun fichier extrait ne les contient et aucune page publique ne les décrit. Observable : en dehors d'un rayon d'environ 1000 blocs (R14), le terrain compte des îles extérieures de formes variées ([W1]). Provisoire (non fidèle, H-EN1) : `end_outer_islands` vaut -0.84375 partout (aucune île extérieure) ; l'île centrale seule est générée. Sonde : DE2 (cartes mesurées de la fonction sur 4 096 x 4 096 blocs par pas de 8, 3 graines, montage MEV) ; critère de succès : toute implémentation candidate reproduit les 262 144 valeurs mesurées par graine à 1e-4 près. **Bloquant B-E1 pour la fidélité des îles extérieures** (Questions ouvertes). | S |
| R9 | `old_blended_noise` dans l'End : paramètres de R3 (D) ; contenu inconnu (B2 de `worldgen-noise-density.md`, WN14). Provisoire : celui de `worldgen-noise-density.md` R21 avec ces paramètres. Sonde : DE3 ; critère de succès : sur la plaque (d <= 20) où `islands` est constant (R6), la fraction de blocs solides par Y (Y 4 à 127, les 1 256 colonnes de la plaque par graine, 3 graines) égale à celle de notre implémentation à 0,5 point près ; les seuils de R7 donnent la fonction de répartition de n en 45 points. | S |
| R10 | Grilles d'interpolation (C sur S, `worldgen-noise-density.md` R24 hypothèse H-I1) : cellules de 8 x 8 en X et Z et 4 en Y, soit 2 x 32 x 2 cellules et 3 x 33 x 3 = 297 coins par chunk (commande `grids`, section DX19). Provisoire : ces cellules. Sonde : WN5 de `worldgen-noise-density.md` (variante cellules de 8 x 8 x 4) ; critère de succès : celui de WN5. | C sur S |
| R11 | Règle de matière `end` : le seul bloc `end_stone` (aucune condition, aucun bedrock, aucune surface). Pas de carver (aucun biome n'en liste), pas d'aquifère, pas de veine. Donc (C) tout bloc solide est de la pierre d'End, tout bloc non solide est de l'air (fluide par défaut `air`), la génération n'écrit aucune bedrock (sauf structures, R22, R25). Provisoire : `worldgen-surface-carvers.md` R22 (un bloc que la règle ne produit pas conserve le remplissage). Sonde : DE4 ; critère de succès : liste des types de blocs sur 100 chunks MEV : seulement `end_stone` et `air`. | D (règle) / S (effet) |
| R12 | Source de hasard hérité : les nœuds aléatoires de l'End sont `old_blended_noise` et `end_outer_islands` ; aucun nœud `noise` nommé n'existe dans le graphe de l'End. Provisoire : dérivation de `worldgen-noise-density.md` R39 pour `old_blended_noise` ; `end_outer_islands` : inconnue (R8). La plaque centrale (R6) ne dépend pas de la graine. Sonde : DE3 ; critère de succès : A mesuré identique sur 3 graines (R38). | S |

### B. Biomes

| Réf | Règle | Preuve |
|---|---|---|
| R13 | Source de biomes : type `the_end`, sans paramètre (préréglage `normal`) ; 5 biomes (tag `is_end`) : `the_end`, `end_highlands`, `end_midlands`, `small_end_islands`, `end_barrens`. Tous : `temperature` 0.5, `downfall` 0.5, pas de précipitation, eau `#3f76e4`, aucun carver, une seule catégorie d'apparition `monster` (`enderman`, poids 10, groupe 4). Commande : `biomes`. | D |
| R14 | Attribution (S [W8], [W9]) : dans un rayon de 1024 blocs ([W8]) ou 1000 blocs ([W9]) de l'origine, le biome est `the_end` ; au-delà, la valeur d'érosion v (= `end/islands`, R2) choisit le biome ([W8]) : `small_end_islands` pour v de -1 à -0.21875, `end_barrens` de -0.21875 à -0.0625, `end_midlands` de -0.0625 à 0.25, `end_highlands` de 0.25 à 1. [W8] précise que la distance de 1024 est horizontale, comptée de l'origine du chunk à l'origine du monde, sans unité, et ne dit pas à quelle bande appartient une valeur égale à une borne partagée. Provisoire (H-EN2) : `the_end` si d < R0 avec R0 = 1024 blocs ; sinon seuils t1 = -0.21875, t2 = -0.0625, t3 = 0.25, borne basse incluse (`small_end_islands` si v < t1, `end_barrens` si v < t2, `end_midlands` si v < t3, sinon `end_highlands`) ; mesure de d à fixer (centre de cellule, origine de chunk). Sonde : DE5 ; critère de succès : 4 intervalles disjoints de l'axe des valeurs lus sur 10 000 cellules (montage MRE), R0 à un chunk près, la relation de biome en fonction de (d, v) est une fonction (aucune cellule de même couple avec deux biomes). | S [W8] |
| R15 | Grille : la fonction d'érosion ne dépend pas de Y (tranche en Y = 0 pour `A` ; sans doute aussi `end_outer_islands`, H-EN6) ; le biome d'une cellule de 4 x 4 x 4 est donc le même sur toute la colonne (16 sections, 64 couches de quart, Y 0 à 255). Provisoire : calculé une fois par colonne de 4 x 4. Sonde : DE5 (variante : biomes lus à tous les Y de 5 colonnes) ; critère de succès : biome identique sur les 64 couches de quart. | C sur S |
| R16 | Listes `features` des biomes de l'End (D) : `the_end` : 11 listes, `end_spike` (étape 4) et `end_platform` (étape 10) ; `small_end_islands` : 1 liste, `end_island_decorated` (étape 0) ; `end_highlands` : 10 listes, `end_gateway_return` (étape 4) et `chorus_plant` (étape 9) ; `end_midlands` et `end_barrens` : 0 liste. Cinq placed features au total. Longueurs de listes inférieures à 11 : voir `dim-nether-generation.md` R21. | D |
| R17 | Cités : tag `has_structure/end_city` = {`end_highlands`, `end_midlands`}. Aucune autre structure ne vise l'End. Le tag `has_structure/stronghold` ne contient pas de biome de l'End. | D |

### C. Île centrale

| Réf | Règle | Preuve |
|---|---|---|
| R18 | Placed feature `end_spike` : un seul modificateur `biome`, étape 4, biome `the_end` ; feature `end_spike` avec `spikes` = liste vide. Une liste vide fait employer des piliers par défaut tirés au hasard ([W6]). Provisoire : une liste vide fait générer les 10 piliers de R19 sans autre donnée. Sonde : DE6 ; critère de succès : 10 piliers présents, sans autre fichier de données. | D (données) / S [W6] |
| R19 | Piliers ([W2], [W1], pages à recouper) : 10 piliers sur un cercle d'environ 42 blocs autour de (0, 0) ; centres fixes (x, z) : (-42, -1), (-34, -25), (-34, 24), (-13, -40), (-13, 39), (12, -40), (12, 39), (33, -25), (33, 24), (42, 0) (distances 40,8 à 42,2 : table DX17) ; sommets Y 76, 79, 82, 85, 88, 91, 94, 97, 100, 103 de rayon 3, 3, 3, 4, 4, 4, 5, 5, 5, 6 ; pilier de sommet 79 et 82 entouré d'une cage de barres de fer ; sommet : un bloc de bedrock, du feu et un cristal d'End ; base à Y 0 ; l'attribution des types aux positions est aléatoire, les positions sont fixes. Provisoire (H-EN3) : les positions et la liste des types sont fixes ; l'attribution est une permutation qui ne dépend que de la graine du monde ; non fidèle tant que DE6 n'a pas répondu : attribution dans l'ordre de la liste ci-dessus. Sonde : DE6 ; critère de succès : pour 20 graines, positions des 10 centres identiques entre graines, Y du sommet, rayon, cage de chaque pilier relevés ; la permutation de chaque graine est une fonction de la graine seule (même graine, deux mondes : même permutation). | S [W2] |
| R20 | Plateforme d'arrivée : placed feature `end_platform` à position fixe (100, 49, 0), biome `the_end`, étape 10 (D). Forme ([W1] ; la page du portail de l'End, citée dans `dim-definitions-portals.md` [W3], donne Y 48 pour l'obsidienne) : carré d'obsidienne de 5 x 5 à Y 48 centré en (100, 0), trois couches d'air au-dessus. Provisoire : 25 blocs d'obsidienne à Y 48 (x de 98 à 102, z de -2 à 2), blocs de Y 49 à 51 sur la même emprise remplacés par de l'air, rien d'autre. L'emprise franchit la frontière de chunk z = 0 / -1 : écritures des deux côtés. Sonde : DE7 ; critère de succès : liste des blocs d'un volume de 7 x 7 x 7 centré en (100, 49, 0) après génération (ONEFEAT et monde MEV à biome central), égale à la prédiction ; chunks (6, -1) et (6, 0) également. | D (position) / S |
| R21 | Création du combat. `has_ender_dragon_fight` vrai pour l'End seul (`dim-definitions-portals.md` DX1). Le jeu crée un fichier d'état `dimensions/minecraft/the_end/data/minecraft/ender_dragon_fight.dat` (`server-game-loop.md` R51) dont les champs publics sont `dragon_killed`, `previously_killed`, `gateways` ([W3], renommés en 26.1 snapshot 6). Le dragon apparaît 20 ticks après la première entrée d'une entité dans l'End, avec le cadre de bedrock du portail de sortie ([W3]). Provisoire : l'état n'est créé qu'à la première entrée d'une entité ; dragon et cadre apparaissent après 20 ticks. Sonde : DE8 ; critère de succès : fichier absent avant, présent après ; clés relevées ; tick d'apparition du dragon (`execute if entity @e[type=ender_dragon]`) relevé à ±1 tick. | S [W3] |
| R22 | Portail de sortie : bâti à la mort du dragon, 200 ticks après le début de l'animation de mort ([W3]), en (0, Y, 0) où Y est lié au bloc le plus haut de la colonne (0, 0) (la page dit que c'est en général le sommet du portail et ne donne pas le Y) ; la structure centrale de bedrock est posée avec le dragon à la création (R21), quatre torches s'y ajoutent ([W1]) ; le centre se remplit de blocs `end_portal` à la mort. Forme exacte (positions, hauteur, torches) inconnue. Provisoire : forme et hauteur à lire sur l'oracle (aucune valeur codée avant DE9). Sonde : DE9 ; critère de succès : liste complète des blocs non air dans un volume de 11 x 12 x 11 centré en (0, 64, 0) avant et après la mort (`kill @e[type=ender_dragon]` après l'apparition), Y du portail pour 5 graines (la hauteur du sol central varie). | S [W3] |
| R23 | Œuf et expérience : un œuf de dragon est posé sur le portail de sortie à la première mort seulement ([W3]) ; l'état `previously_killed` passe à vrai. Provisoire : œuf à la première mort, jamais aux suivantes ; sa position est un bloc au-dessus du bloc le plus haut de (0, 0), et rien s'il est à la limite de hauteur. Sonde : DE9 (variante : seconde mort après réinvocation) ; critère de succès : présence et position de l'œuf aux deux morts. | S [W3] |
| R24 | Chargement : type de ticket `dragon` (D, registre `ticket_type`). Provisoire : tant que le combat est actif, la zone de l'île centrale reste chargée par un ticket `dragon` visible dans `chunk_tickets.dat` de l'End ; après la mort, il disparaît. Sonde : DE8 ; critère de succès : entrées `dragon` relevées à 0, 20, 400 ticks et après la mort (rayon et durée). | D (type) / S |

### D. Passerelles de retour

| Réf | Règle | Preuve |
|---|---|---|
| R25 | Placed feature `end_gateway_return` : biome `end_highlands`, étape 4 ; modificateurs : `rarity_filter` 1 sur 700, `in_square`, `heightmap` `MOTION_BLOCKING`, `offset` Y uniforme de 3 à 9, `biome`. Feature : `end_gateway` avec `exact` vrai et `exit` (100, 50, 0). Commandes : `portal`, `features`. | D |
| R26 | Forme de la passerelle posée ([W4]) : un bloc `end_gateway` entouré de bedrock (le texte parle de 12 blocs de bedrock à l'entour) et d'une colonne de bedrock au-dessus et en dessous ; la forme exacte n'est pas décrite. Provisoire : forme à lire sur l'oracle (aucune valeur codée avant DE10). Sonde : DE10 (`place feature minecraft:end_gateway_return` sur un sol d'End plat, 10 positions) ; critère de succès : liste des blocs d'un volume de 9 x 15 x 9 autour du bloc de passerelle, identique entre positions et d'un sol à l'autre. | S [W4] |
| R27 | Fréquence : un essai par chunk avec la probabilité 1/700 si la position est dans `end_highlands` (le test de biome est celui du point final). Attente : 1/700 par chunk de hautes terres, soit environ 1 passerelle par 700 chunks de hautes terres (C, commande `grids`, section DX21). Provisoire : celle-ci. Sonde : DE10 (variante monde normal, 20 000 chunks de hautes terres) ; critère de succès : nombre de passerelles dans l'intervalle de Poisson à 3 écarts types. | C sur S |

### E. Îles et plantes décorées

| Réf | Règle | Preuve |
|---|---|---|
| R28 | `end_island_decorated` : biome `small_end_islands`, étape 0 (`raw_generation`) ; modificateurs : `rarity_filter` 1 sur 14, `count` pondéré (1 avec le poids 3, 2 avec le poids 1), `in_square`, `height_range` uniforme de 55 à 70, `biome`. Feature `end_island` à configuration vide. C : espérance de 1/14 x 1,25 = 0,0893 essai par chunk (commande `grids`, section DX21). Forme de l'île : aucune description publique. Provisoire : spec fille à partir de traces ; chaque essai pose une île de pierre d'End de forme à relever. Sonde : DE11 (ONEFEAT en biome fixe `small_end_islands` sur un monde d'air, 100 positions) ; critère de succès : pour chaque position, nombre de blocs, boîte englobante, profil par Y relevés ; une implémentation candidate est correcte quand sa trace est égale sur 30 positions. | D (placement) / S (forme) |
| R29 | `chorus_plant` : biome `end_highlands`, étape 9 ; `count` uniforme de 0 à 4 (espérance 2 par chunk, C, commande `grids`, section DX21), `in_square`, `heightmap` `MOTION_BLOCKING`, `biome` ; feature à configuration vide. Les arbres de chorus sont courants sur les îles extérieures ([W1]) et n'apparaissent que dans ce biome ([W10]). Forme et âge des fleurs : inconnus. Provisoire : spec fille à partir de traces. Sonde : DE12 (ONEFEAT sur sol d'End plat, 200 essais) ; critère de succès : trace (blocs, boîte englobante, nombre de fleurs) égale à celle de l'oracle sur 30 essais. | D (placement) / S |

### F. Anneau de passerelles

| Réf | Règle | Preuve |
|---|---|---|
| R30 | Anneau central ([W4], [W3]) : à chaque mort du dragon, une passerelle est créée sur un cercle de rayon 96 autour de (0, 0), à Y 75, jusqu'à 20 ; positions (x, z) = `(floor(96 cos(k pi / 10)), floor(96 sin(k pi / 10)))` ([W4] : la page écrit k entier avec k <= 20 sans dire de quel indice on part, et donne trois exemples : passerelle 1 en (96, 0), passerelle 6 en (-1, 96), passerelle 20 en (91, -30)). Les exemples 1 et 20 s'obtiennent avec k = 0 à 19 (passerelle n = k + 1), pas avec k = 1 à 20 (la passerelle 1 serait en (91, 29)). Le plancher et la troncature diffèrent sur 14 positions (DX16). Les quatre positions sur les axes (k = 0, 5, 10, 15) dépendent de l'erreur d'arrondi à zéro : l'exemple de la passerelle 6 donne x = -1 alors que le calcul en double précision de `floor(96 cos(pi / 2))` donne 0 (DX16, ligne MISMATCH). Ordre de création : fonction de la graine. Les passerelles de l'anneau n'ont pas de destination (R54 de `dim-definitions-portals.md`). Provisoire (H-EN4 pour les positions, H-EN5 pour l'ordre) : k = 0 à 19, plancher de `96 cos`, `96 sin` calculés en double, sauf la passerelle 6 posée en (-1, 96) d'après la page (indexation alternative H-EN4b : k = 1 à 20 ; les trois exemples la réfutent, DE13 tranche) ; ordre = permutation fonction de la graine seule, relevée par DE13 ; au plus 20. Sonde : DE13 (réinvocation par 4 cristaux, 21 morts) ; critère de succès : 20 positions relevées égales à DX16 (plancher, k = 0 à 19, passerelle 6 comprise) ou H-EN4 corrigée, ordre relevé sur 5 graines, aucune passerelle à la 21e mort. | S [W4] |

### G. Cités de l'End

| Réf | Règle | Preuve |
|---|---|---|
| R31 | Ensemble `end_cities` : `random_spread`, espacement 20, séparation 11, sel 10387313, `spread_type` `triangular`, membre `end_city` de poids 1. C : région de 320 blocs, 9 positions candidates par axe (décalage de chunk de 0 à 8, 144 blocs), distribution triangulaire (plus de candidats au centre de la plage) ; cohérent avec la page [W5] (décalage de 0 à 8 par pas de 20). Mécanisme de placement : `worldgen-features-structures.md` R51 à R56. | D / C |
| R32 | Structure `end_city` : type `end_city`, biomes {`end_highlands`, `end_midlands`}, étape `surface_structures`, pas de champ de projection ni d'adaptation au terrain, `spawn_overrides` vide. Pièce enregistrée `ecp` ; 20 gabarits `end_city/*.nbt` : `base_floor`, `base_roof`, `bridge_end`, `bridge_gentle_stairs`, `bridge_piece`, `bridge_steep_stairs`, `fat_tower_base`, `fat_tower_middle`, `fat_tower_top`, `second_floor_1`, `second_floor_2`, `second_roof`, `ship`, `third_floor_1`, `third_floor_2`, `third_roof`, `tower_base`, `tower_floor`, `tower_piece`, `tower_top`. Table de butin `end_city_treasure`. | D |
| R33 | Composition ([W5], observable) : tours (petite tour creuse à escalier en spirale, gratte-ciel à double spirale), salles (base, bannières, vide, parcours, butin à deux coffres dont l'un a un coffre d'End), ponts entre tours (chaque direction de tour a 50 % de pont, chaque pont 12,5 % de mener à un vaisseau), au plus un vaisseau par cité (poupe vers la cité, proue au large, coffres, cadre à élytres, tête de dragon), chiffres de shulkers par pièce. Le générateur est une spec fille. Provisoire (non fidèle) : seule la pièce de départ est posée. Sonde : DE14 (100 cités par `place structure minecraft:end_city`, montage ME2) ; critère de succès : nombre de pièces par gabarit sur 100 cités, fréquence des cités avec vaisseau (attendu entre 0 et 1 par cité), boîte englobante, rotation, Y de départ relevés. | S [W5] |
| R34 | Distance minimale (C sur S) : une cité exige un biome de hautes ou moyennes terres, présent seulement là où la fonction d'îles donne des valeurs hautes ; autour de l'île centrale (R6, rayon environ 92) il n'y a que `the_end` (R14) ; donc aucun départ de cité à moins du rayon de R14 de l'origine. Provisoire : aucun départ à moins de 1024 blocs de (0, 0) (H-EN2). Sonde : DE14 ; critère de succès : distance minimale à l'origine des 100 premiers départs. | C sur S |
| R35 | Hauteur de la cité : la structure n'a pas de projection déclarée dans ses données ; la base est posée sur le sol de l'île par le générateur. Provisoire : Y de la pièce de départ = hauteur du sol de la colonne de référence du chunk candidat (heightmap `WORLD_SURFACE_WG`, `worldgen-pipeline.md` R23). Sonde : DE14 ; critère de succès : écart entre le Y de départ et la heightmap du chunk de départ (0 sur 100 cités, sinon l'écart relevé devient la règle). | S |

### H. Pipeline

| Réf | Règle | Preuve |
|---|---|---|
| R36 | Différences de pipeline avec l'Overworld (`worldgen-pipeline.md` R12 à R22) : pas de carver, d'aquifère, de veine ni de règle de surface ; `terrain` = remplissage de densité (R3) puis règle de matière (R11) puis heightmaps `_WG` ; `features` : étapes 0 (îles décorées), 4 (piliers, passerelles de retour), 9 (chorus), 10 (plateforme) ; `spawn` : `disable_mob_generation` vrai donc aucune créature initiale (D). Les mêmes dix statuts s'appliquent. | C |
| R37 | Écritures hors du chunk : pilier (rayon jusqu'à 6, cage), plateforme (z de -2 à 2) et passerelle franchissent des frontières de chunk ; elles suivent la zone de 3 x 3 (`worldgen-features-structures.md` R15). Provisoire : mêmes règles. Sonde : DE15 ; critère de succès : états canoniques identiques entre les 3 ordres de génération de `worldgen-pipeline.md` WP4 sur 81 chunks centrés sur (0, 0) (piliers, plateforme, île centrale). | S |
| R38 | Dépendance à la graine : l'île centrale (A, R6) est identique pour toutes les graines ; seuls le bruit hérité (R9), les îles extérieures (R8), l'attribution des piliers (R19) et l'ordre des passerelles (R30) en dépendent. Provisoire : mêmes conséquences. Sonde : DE3 et DE6 ; critère de succès : A identique sur 3 graines, les quatre autres éléments différents. | C sur S |

## Entrées / sorties

- **Entrée** : graine (long), réglages de bruit `end`, source de biomes `the_end`, jeux de structures, coordonnées de chunk, état du combat.
- **Sortie** : chunk au statut cible (état canonique, `worldgen-pipeline.md` R34) à 16 sections de 16 x 16 x 16 ; grille de biomes ; départs de cités ; fichier d'état du combat.
- **Coordonnées négatives** : la distance de l'île centrale est symétrique (A dépend de racine(x² + z²)) ; les régions de cités et les cellules de grille utilisent le plancher (R31, R15).

## Cas limites

- Chunk (-1, -1) et (0, 0) : plateforme et pilier (-42, -1) à cheval sur des bordures de chunk (R19, R20, R37).
- Plaque centrale d = 20 : `A` vaut 0.5625 exactement ; au-delà, pente de 1/128 par bloc (R6).
- Y 62 sur la plaque : densité nulle analytique : à ne pas utiliser comme valeur de sonde (R7).
- Rayon d'environ 1000 à 1024 blocs : changement de biome central / extérieur ; trois valeurs en concurrence (R14).
- Grandes distances (10^6 blocs) : précision des flottants de la fonction d'îles inconnue (`worldgen-noise-density.md` R40, WN19).
- Graines extrêmes (`Long.MIN_VALUE`, `Long.MAX_VALUE`, -1, 0) : tous les tests de pilier et d'anneau les emploient.

## Plan de tests

- **L0** : empreintes de R1 à R4, R13, R16, R18, R25, R31, R32 par `dim_ref.py` contre `$REF`.
- **L1** : `A(d)` de R6 (table DX13) ; profil de R7 ; tables DX16 (anneau, deux arrondis) et DX17 (piliers) ; régions de cités de R31 ; chargement des réglages `end` ; refus d'une fonction `slice` sans axe.
- **L2** : DE1 à DE5 (forme et biomes), DE6 à DE10 (île centrale), DE11 à DE12 (îles, chorus), DE13 (anneau), DE14 (cités) ; montages ME2, ME1, ME0 stratifiés (`worldgen-pipeline.md` R35).
- **L3** : vue de dessus de l'île centrale et des 10 piliers ; carte des îles extérieures 4 096 x 4 096.
- **L4** : scénario : entrée dans l'End, apparition du dragon, mort, portail de sortie, retour à l'Overworld (spec `dim-definitions-portals.md`).

## Vecteurs de test

Voir [dim-end-generation-vectors.md](dim-end-generation-vectors.md) : DX12 (réglages), DX13 (profil sans bruit), DX14 (régions de cités), DX15 (inventaire), DX16 (anneau de passerelles sous deux arrondis), DX17 (piliers), DX19 et DX21 (coins d'interpolation, espérances par chunk), tous des sorties de `dim_ref.py` avec empreintes. Les valeurs des îles extérieures, des seuils de biome, de la forme des piliers, du portail et des passerelles sont des sorties de l'oracle (DE2, DE5 à DE13) : emplacements réservés.

## Questions ouvertes

- **B-E1 (R8) : fonction des îles extérieures.** Ni les données ni une page publique n'en donnent l'algorithme ; DE2 donne des valeurs mesurées, pas une formule. Mitigation : hypothèses candidates testées contre les cartes mesurées. Si aucune ne s'ajuste, **bloquant** pour la fidélité de tout l'End au-delà de 1000 blocs (et donc des cités, des chorus, des passerelles extérieures, des biomes extérieurs) ; l'ADR 0002 2a-bis (signatures publiques) ne donnerait pas un algorithme. Décision du propriétaire attendue si l'écart persiste.
- **Q2 (R9)** : bruit hérité commun à l'Overworld (B2) ; DE3 en donne la fonction de répartition dans un cas à valeur centrale connue.
- **Q3 (R14)** : seuils de biome et rayon central : valeurs de [W8] posées en H-EN2, inclusivité aux bornes et mesure de la distance inconnues ; DE5 mesure directement.
- **Q4 (R19, R30)** : permutation des piliers et ordre des passerelles en fonction de la graine : S ; DE6, DE13 ; si non reproductible par hypothèse simple, on ne peut que tabuler (inacceptable pour une graine arbitraire).
- **Q5 (R22, R26)** : formes du portail de sortie et de la passerelle : relevé de l'oracle ; golden vectors par DE9 et DE10.
- **Q6 (R28, R29, R33)** : algorithmes de `end_island`, `chorus_plant`, cité de l'End : specs filles.
- **Q7 (R21 à R24)** : le combat réel (IA du dragon) est hors périmètre ; seules les écritures de blocs et l'état sont spécifiés. Un test de bout en bout exige le jalon M7.
- **Aucun blocage 2a-bis** ailleurs que B-E1.

## Proposition de mise à jour de la matrice (appliquée par l'orchestrateur)

Ligne « End » : `non-spécifié` -> `spécifié` (D/S, M10, A) ; sous-lignes à créer : Réglages et île centrale de l'End, Îles extérieures de l'End (**bloqué B-E1**), Biomes de l'End, Piliers et plateforme, État du combat (portail de sortie, œuf, anneau de passerelles), Cités et vaisseaux de l'End, Chorus et îles décorées.

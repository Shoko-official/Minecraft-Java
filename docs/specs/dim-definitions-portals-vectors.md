# Vecteurs et sondes : dimensions, portails, voyage

Complète `dim-definitions-portals.md`, et fournit l'outil commun (`dim_ref.py`, en annexe) aux deux autres specs de M10. Les vecteurs DX sont des calculs sur les données extraites (preuve C). Les sondes DP (définitions et portails) et DV (voyage) sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée par le propriétaire) ; chacune dit la commande, l'entrée et la sortie attendue. Les sondes marquées **J** exigent un joueur connecté (client vanilla) et ne tournent pas sans lui.

Pour ne pas recopier les données de Mojang, les tableaux volumineux ne sont pas reproduits : la commande qui les produit est donnée avec l'empreinte SHA-256 de la sortie complète, pour détecter une dérive des données extraites, et quelques lignes d'exemple.

## Outil et commandes

L'annexe en fin de fichier est le texte intégral de `dim_ref.py` (SHA-256 du texte de l'annexe, fins de ligne LF, 31 513 octets : `9e47732a125ced2b5b2ba026bce4d6a272cab1025ff97052b8d56dda2cc266ab`). `REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3` (jeu 26.3, `serverSha256` de `state.json` : `d052f14d7a173734fba553711e5b570162e2f2a313267ee31a21b975a679be64`). Toutes les commandes sont `python -I dim_ref.py REF <sous-commande>` ; la sortie est en ASCII, fins de ligne LF, indépendante du chemin de `REF` ; les empreintes sont les SHA-256 complets des sorties.

| Sous-commande | Sortie | Lignes | SHA-256 de la sortie |
|---|---|---|---|
| `dimtypes` | champs scalaires et 22 attributs des 4 types (DX1, R11, R12) | 129 | `1e8d88068cc04dc93e1a05495fd92b087bd2b4ad10041866df1a70b9da0d7b2e` |
| `presets` | dimensions des préréglages `normal` et `flat_all_dimensions` | 8 | `70be9f7a53a5b5485ebc61b66f67d6fe019e635e14e879d9fac9223367417c68` |
| `noise nether` | réglages et bruits du Nether | 32 | `313fad8385997e37a0a9882c294dab4074002b94039da9b32a0e18277230be34` |
| `noise end` | réglages, `islands` et `sloped_cheese` de l'End | 26 | `9b7030f7ada06e5d9357a14125758fd3a8172ba32dbd422af6308e0aa22363ed` |
| `surface-nether` | arbre des règles `nether`, `end`, bedrock, conditions nommées | 87 | `ce207e449550eee6a5bcb6df580ced33b24aa146ef2fd9c3e44f582a9f1787f8` |
| `bedrock` | probabilités de bedrock du Nether, plage du carver | 6 | `9354a8eb9d347cd23643fc4451da5da79538293914c7291068b5fb3a3a33e318` |
| `biomes` | table du Nether, listes de features, créatures, tags | 34 | `69818cd9a61bb8d4721fa80de2ae9bff6a2715527dfef7f5ee40cf31719cee90` |
| `features` | 42 placed features (37 Nether, 5 End) avec Y résolus sur 0 à 127 | 44 | `646367d45fb2b03a75fd0b732318e6b8ed1ef4e9a415bd20b53c7d386b31486b` |
| `order` | ordre partiel des features par étape dans les 5 biomes du Nether | 11 | `a2ade2d170aff7d8e640bac04dbbab9cbfc42891363ff278a337e9924ad4803b` |
| `structures` | ensembles, structures, tags, gabarits, pièces, butins | 22 | `121ff06ca97a0f9c6baaf6c4e3447e46e9ad3b5bba32f60f48b85b45a1ece1cd` |
| `bastion` | statistiques des 60 pools et des 167 gabarits | 7 | `a26b5ba8fda0e74cd86e4001f39f7dc9edfdef29102fa3fdbe78b2e6653671cf` |
| `portal` | tags, états de blocs, POI, tickets, règles, déclencheurs, features | 32 | `353052e5640516ccdfe336f7484eb8263167be86aaed6cb6a575e438d10de6fe` |
| `vectors` | profils sans bruit (Nether DX7, End DX13), régions (DX9, DX14), conversion 8:1 (DX4), anneau (DX16), piliers (DX17) | 120 | `3b7d3d788cf5c14d7395cfa2a048e2b2e96b2ac4d6a005494e02043f7f1b5901` |
| `grids` | sections, positions, cellules de biomes (DX18), coins d'interpolation (DX19), indépendance en Y du climat du Nether (DX20), espérances par chunk des features de l'End (DX21), poids de `nether_complexes` (DX22) | 18 | `970d585eed8a376b172cced6a50250a0daa894be0e699cf672aff097626e9a0c` |
| `portalvec` | tailles de cadre (DX3), yeux de l'End (DX5), longueurs de passerelle (DX6) | 26 | `ecdf50b4ef777993f12846e97668961aca96013e35a2fea5d6bd06dd5ffeee7d` |

## DX : vecteurs calculés

**DX1 (R2, R11) : types de dimension.** Le tableau DX1 de la spec est la sortie de `dimtypes` (16 champs scalaires, 22 attributs). Les écarts entre les 4 types (15 champs sur 16 diffèrent entre l'Overworld et le Nether, seul `has_ender_dragon_fight` est commun) sont la base du test de chargement : un type chargé doit redonner exactement ces valeurs.

**DX2 (R22, R45) : blocs de portail** (sortie de `portal`) :

```
block nether_portal properties {"axis":["x","z"]} states 2
block end_portal properties null states 1
block end_gateway properties null states 1
block end_portal_frame properties {"eye":["true","false"],"facing":["north","south","west","east"]} states 8
block respawn_anchor properties {"charges":["0","1","2","3","4"]} states 5
poi types ['lodestone', 'nether_portal']
ticket types ['dragon', 'ender_pearl', 'forced', 'player_loading', 'player_simulation', 'player_spawn', 'portal', 'spawn_search', 'unknown']
triggers ['changed_dimension', 'nether_travel', 'used_ender_eye']
```

**DX3 (R23) : tailles de cadre** (sortie de `portalvec`, section DX3) : 380 intérieurs valides (largeur 2 à 21, hauteur 3 à 21) ; cadres extérieurs de 4 x 5 à 23 x 23 ; obsidienne avec angles `2 (w + h) + 4` (14 pour 2 x 3, 22 pour 4 x 5, 88 pour 21 x 21), sans angles `2 (w + h)` (10, 18, 84).

**DX4 (R34) : conversion 8:1 sous H-PT1** (sortie de `vectors`, section DX4) : plancher de `x s_source / s_cible`. Exemples : Overworld vers Nether : x = -9 -> -2, -8 -> -1, -1 -> -1, 0 -> 0, 7 -> 0, 8 -> 1, 15 -> 1, 16 -> 2, 100 -> 12, -100 -> -13. Nether vers Overworld : x = -9 -> -72, -1 -> -8, 1 -> 8, 100 -> 800.

**DX5 (R47) : yeux préposés** (sortie de `portalvec`, section DX5) : binomiale (12, 0,1) : P(0) = 0,282430, P(1) = 0,376573, P(2) = 0,230128, P(3) = 0,085233, P(4) = 0,021308, P(5 ou plus) = 0,004329, espérance 1,20. Recoupement avec le wiki [W3] (le plus probable : un œil, environ 38 %, puis zéro, 28 %, deux, 23 %) : cohérent.

**DX6 (R54) : longueurs de passerelle** (sortie de `portalvec`, section DX6) : 33 valeurs de 768 à 1280 par pas de 16 ; 1024 est la 17e (16 pas de chaque côté).

**DX18 à DX22 (sortie de `grids`, 18 lignes)** : DX18 : Overworld et `overworld_caves` 24 sections, 98 304 positions, 1 536 cellules de biomes, sections Y -4 à 19 ; Nether et End 16 sections, 65 536 positions, 1 024 cellules, sections Y 0 à 15. DX19 : coins d'interpolation par chunk, Nether 5 x 17 x 5 = 425 (cellules 4 x 8), End 3 x 33 x 3 = 297 (cellules 8 x 4). DX20 : le climat du Nether ne dépend pas de Y (quatre constantes, deux nœuds de bruit à `y_scale` 0.0 sans décalage en Y) : vrai. DX21 : espérances par chunk : `end_island_decorated` 0,0893 essai, `chorus_plant` 2,00 essais, `end_gateway_return` 1/700. DX22 : `nether_complexes` : forteresse 0,40, bastion 0,60 ; pool de départ du bastion : 4 éléments de poids égal.

## Harnais commun

Conventions de `worldgen-surface-carvers-vectors.md` (graine 12345 sauf indication, `forceload`, attente W, `save-all flush`, arrêt, lecture de `dimensions/minecraft/<dimension>/region/r.*.mca`) ; les datapacks de sonde sont produits à la volée par `tools/oracle` depuis `.cache/reference/26.3`, jamais commités.

- **FLATALL** : monde créé avec le préréglage `flat_all_dimensions` (D, R1 de la spec : trois dimensions plates, sans bruit, sans feature, sans structure ; Overworld : 1 couche de bedrock (Y -64) et 67 de grès (Y -63 à 3), biome désert ; Nether : bedrock Y 0 et basalte Y 1 à 3, biome `basalt_deltas` ; End : bedrock Y 0 et pierre d'End Y 1 à 3, biome `the_end` ; le dernier bloc plein est donc à Y 3 dans les trois dimensions, et un cadre de portail posé sur le sol a sa rangée basse à Y 4). Réglage du préréglage à la création : propriété `level-type` (nom exact à confirmer par DP0).
- **VOID** : dimension personnalisée `probe:void` : fichier `data/probe/dimension/void.json` de forme `{"type": "minecraft:the_nether", "generator": {"type": "minecraft:flat", "settings": {"biome": "minecraft:the_void", "features": true, "lakes": false, "layers": [{"block": "minecraft:air", "height": 1}], "structure_overrides": []}}}` (réglage du préréglage plat `the_void`, D ; le biome `the_void` n'a aucune feature). Aucun sol, aucune structure. Le champ `type` varie selon la sonde.
- **DIMX(champ, valeur)** : copie de `dimension_type/overworld.json` avec un champ modifié, référencée par une dimension plate `probe:x`.
- **PORTAL(axe, w, h, coin)** : `fill` d'obsidienne pour un cadre de w x h intérieur (avec angles si `coin`), puis `setblock <x> <y> <z> minecraft:fire` à l'intérieur. Lecture : `execute if block`, `data get block`.
- **OBS-DIM(étiquette)** : `execute as @e[tag=<T>,limit=1] if dimension <clé> run data get entity @s Pos` ; renvoie la position quand l'entité est dans la dimension demandée, aucun résultat sinon. Valide par DP0.
- **SPAWN(type, étiquette, NBT)** : `summon <type> <x> <y> <z> {Tags:["<T>"],...}` ; les entités sont rendues sans IA (`NoAI:1b`) et sans gravité (`NoGravity:1b`) quand la sonde n'étudie pas le mouvement ; elles sont posées à l'intérieur d'un bloc de portail par `tp` plutôt que marchées.
- **MRE / MEV** : voir `dim-end-generation-vectors.md` ; **MRN** : voir `dim-nether-generation-vectors.md`.

## Sondes de définition (DP)

**DP0 (R9, R10 ; prérequis de toutes les sondes DP et DV).** FLATALL (création du monde par préréglage), puis `execute in <d> run summon armor_stand 0 80 0 {Tags:["t<d>"]}` pour les trois dimensions (positions chargées par `forceload add 0 0` dans chacune, attente W). Mesures : (a) `execute if entity @e[tag=tnether]` depuis l'Overworld (la recherche `@e` traverse-t-elle les dimensions ?) ; (b) `execute in minecraft:the_nether if entity @e[tag=tnether,distance=..1000000]` ; (c) `execute as @e[tag=tnether,limit=1] if dimension minecraft:the_nether run data get entity @s Pos` ; (d) `forceload query` dans chaque dimension ; (e) `dimensions/minecraft/<d>/data/minecraft/chunk_tickets.dat`. Attendu : R9 et R10 de la spec. Critère de succès : réponses (a) à (e) égales à la prédiction ; sinon R10 et OBS-DIM sont réécrits.

**DP1 (R5, R6).** Monde normal, graine 12345, `forceload` de 10 x 10 chunks dans le Nether et dans l'End ; lecture des sections : (a) blocs non air de Y 128 à 255 ; (b) `setblock <x> <y> <z> minecraft:stone` pour Y -1, 0, 127, 128, 255, 256 dans chaque dimension, résultat de la commande ; (c) liste des sections écrites par chunk (Y de section 0 à 15). Critère de succès : R5 et R6 ; aucun bloc non air hors structures au-dessus de Y 127.

**DP2 (R8).** FLATALL : `execute in minecraft:the_nether run tp @e[tag=t,limit=1] 800 70 800`, puis lecture de `Pos` dans le Nether ; le sens inverse avec 100 100 depuis le Nether vers l'Overworld ; `execute in minecraft:the_nether run forceload add 100 100` : quel chunk est chargé dans le Nether. Critère de succès : positions finales égales aux positions écrites ; aucune division ni multiplication par 8.

**DP3 (R3).** Dimension personnalisée `probe:x` de type `probe:t1` = copie d'`overworld` (a) sans `has_fixed_time` ; (b) sans `min_y` ; (c) avec un champ inconnu `foo` ; (d) avec `has_fixed_time` vrai sans `default_clock`. Résultats : acceptation, message d'erreur et nom du champ, comportement du temps (`time query daytime` dans la dimension). Critère de succès : R3.

**DP4 (R18).** Monde normal : 100 chunks du Nether, de l'End et de l'Overworld générés, lecture de `SkyLight` de chaque section. Critère de succès : 0 section du Nether avec `SkyLight` ; sections de l'End et de l'Overworld avec tableau quand elles contiennent des blocs ; présence de `BlockLight` dans les trois.

**DP5 (R19).** Dimensions plates `probe:ceil0` (type `overworld`) et `probe:ceil1` (type `overworld_caves` : copie d'`overworld` avec `has_ceiling` vrai) : (a) `weather rain` puis `execute in probe:ceil1 run weather query` ; précipitation ; (b) position d'apparition d'un chunk neuf (`setworldspawn` dans la dimension, `spreadplayers` n'est pas utilisable sans joueur) ; (c) `SkyLight` ; (d) `execute positioned over world_surface` et `... over motion_blocking` à (0, 0). Critère de succès : aucun écart entre les deux ; sinon un écart = une règle nouvelle.

**DP6 (R20).** Datapack avec `data/probe/dimension/x.json` = `{"type":"minecraft:overworld","generator":{...plat...}}` ; démarrage ; `execute in probe:x run forceload add 0 0` ; `save-all flush` ; liste de `dimensions/probe/x/`. Critère de succès : R20.

**DP7 (R23, R25).** FLATALL (Overworld) : PORTAL pour w de 1 à 23, h de 2 à 23, avec et sans angles, axe x puis axe z (4 x 22 x 22 cadres de 1 000 blocs d'écart) ; pour chaque cadre, feu en bas à gauche, puis lecture du bloc au centre (`execute if block <x> <y> <z> minecraft:nether_portal[axis=x]`) et de la propriété `axis`. Sortie : table des (w, h, coin, axe) allumés et de l'axe lu. Critère de succès : égale à DX3 ; `axis` égal à l'axe du cadre. Variante : cadre avec une case d'obsidienne pleureuse (`crying_obsidian`) en haut, à lire en négatif.

**DP8 (R24).** Cadres 4 x 5 avec angles : (a) feu sur la ligne du bas, (b) du haut, (c) au milieu, tous par `setblock` ; (d) briquet par un joueur (J) ; (e) boule de feu (`summon fireball`) ; (f) foudre (`summon lightning_bolt`) ; (g) feu posé avant la dernière obsidienne ; (h) cadre dont l'intérieur contient un bloc de pierre. Critère de succès : issue conforme à R24.

**DP9 (R26).** Portail 4 x 5 allumé ; retrait par `setblock air` d'un bloc d'obsidienne en 4 positions (angle, côté, haut, bas) ; lecture de tous les blocs de portail 1, 2 et 20 ticks après (`tick freeze`, `tick step`). Variante : deux portails partageant une colonne de cadre. Critère de succès : état des blocs conforme à R26.

**DP10 (R46 ; J).** Anneaux de 12 `end_portal_frame[eye=true]` posés par `setblock` dans les orientations horizontale, debout selon X et debout selon Z, chacune avec les 4 rotations (24 en tout) ; un joueur pose l'œil manquant dans le dernier cadre ; lecture du centre 3 x 3. Critère de succès : remplissage ou non conforme à R46 pour les 24 ; variantes : 11 yeux, œil mal orienté, bedrock dans le centre.

**DP11 (R13, R14 ; J).** Dans chacune des trois dimensions : un joueur utilise un lit (jour, nuit), un lit de paille, une ancre chargée à 0 et 4 charges ; mesure : explosion et puissance (blocs détruits), lit détruit, point d'apparition (`data get entity @s respawn`), charge. Critère de succès : R12 à R14.

**DP12 (R16, R17).** FLATALL : (a) `setblock` de feu sur bedrock, netherrack, pierre dans chaque dimension, lecture à 6 000 ticks sous `tick rate` élevé ; (b) eau par `setblock` et par seau (joueur, J) dans le Nether ; (c) lave posée sur sol plat dans le Nether et dans l'Overworld : distance d'écoulement et temps (`tick step`). Critère de succès : R16 (a), R17 (b), (c).

**DP13 (R31).** Portail 4 x 5 de l'Overworld (FLATALL), `gamerule random_tick_speed 3`, `difficulty` easy / normal / hard, `spawn_mobs true`, 100 000 ticks par difficulté ; comptage des `zombified_piglin` apparus. Critère de succès : taux par difficulté, rapports de R31 (x2 normal, x3 difficile par rapport à facile) ; aucun apparu dans le Nether. Variante (R32) : un `hoglin` avec IA placé à 6 blocs d'un portail actif, position relevée à chaque tick pendant 200 ticks, avec et sans portail.

**DP14 (R47).** Monde normal, 200 positions : `place structure minecraft:stronghold` dans la zone de génération ; lecture des 12 `end_portal_frame` de la salle (propriété `eye`). Critère de succès : fréquence de chaque nombre d'yeux égale à DX5 à 3 écarts types.

## Sondes de voyage (DV)

Toutes en FLATALL sauf indication, avec SPAWN et OBS-DIM ; chaque transfert s'attend sous `tick freeze` / `tick step` ; les entités n'ont pas d'IA.

**DV1 (R33, R34, R44).** Pour x = z parmi {-9, -8, -1, 0, 1, 7, 8, 15, 16, 100} (et séparément X seul), un objet (`item`) est posé dans un portail de l'Overworld construit autour de (x, 5, z) (rangée basse du cadre à Y 4, sol à Y 3) avec 20 chunks de dégagement ; relever la position du portail créé dans le Nether (liste des blocs `nether_portal` dans un cube de 64) ; comparer à DX4. Variante nombre de chunks nouveaux écrits sur disque (R44). Critère de succès : position du centre du portail créé égale à `floor(x / 8)` en X et Z (DX4), sinon H-PT1 est remplacée.

**DV2 (R35).** Portails pré-construits dans le Nether à (4, 70, 4) + (d, 0, 0), (0, 0, d), (d, 0, d), (0, d, 0) pour d de 10 à 20 en pas de 1 ; objet entrant dans le portail de l'Overworld dont la cible est (4, y, 4) ; lien (arrivée dans le portail existant) ou création (nouveau portail à côté). Variante Nether vers Overworld pour d de 120 à 136. Critère de succès : seuil de lien (borne incluse ou exclue) égal à la prédiction de R35.

**DV3 (R28).** Objet, vache, chariot, bateau, flèche (`arrow`) posés dans un portail ; relever `PortalCooldown` (`data get entity`) 1 tick après l'arrivée ; relever aussi après 5, 10, 20, 300 ticks et le moment où une seconde entrée est possible. Critère de succès : valeurs de R28.

**DV4 (R29).** 40 types d'entité (créatures, objets, projectiles, chariots, bateaux, armor stand, cristal d'End, dragon, wither, creaking lié ou non) posés dans un portail ; lecture de la dimension (OBS-DIM). Critère de succès : exclusions de R29.

**DV5 (R30).** `gamerule allow_entering_nether_using_portals false` puis objet dans un portail de l'Overworld ; puis `true`, objet dans un portail du Nether avec la règle à `false`. Critère de succès : R30.

**DV6 (R36, R38).** Deux portails de destination équidistants (en X, en Z, en Y, en diagonale) et un plus proche d'un bloc ; 20 couples ; portail large de 21 x 5 : entrée par les deux extrémités. Critère de succès : portail choisi égal à la prédiction, ordre d'égalité relevé.

**DV7 (R37).** Un bloc `nether_portal[axis=x]` posé par `setblock` sans cadre dans la cible, (posé à Y 4) puis objet entrant ; variante : portail réel puis `setblock air` de l'obsidienne du cadre sans retirer les blocs de portail. Critère de succès : lien ou création conforme à R37.

**DV8 (R7, R39, R40).** Création forcée : (a) FLATALL (zone libre, sol à Y 3, demande à Y 4) ; (b) zone étroite creusée par `fill` (couloir de 1 x 4) ; (c) VOID (type `the_nether`, sans sol) ; (d) demande à des Y source de 100, 110, 120, 128, 200, 250 ; (e) portail d'entrée selon X, puis selon Z. Pour chaque cas, liste des blocs écrits dans un volume de 7 x 9 x 7 autour du centre. Critère de succès : liste de blocs égale à R39 pour les 3 terrains, axe égal à celui du portail d'entrée, Y maximal de la plateforme pour (d) (H-PT4).

**DV9 (R41).** Un objet entre par chacun des 4 côtés horizontaux dans un portail d'axe x puis z avec `Motion` non nulle ; lecture de `Pos`, `Rotation`, `Motion` avant (au tick d'entrée) et après. Critère de succès : valeurs de R41 à 1e-3 près.

**DV10 (R42).** Après un transfert, lecture de `chunk_tickets.dat` de la dimension cible à 0, 100, 300, 301, 600 ticks, et après un second transfert à 200 ticks. Critère de succès : type `portal`, rayon et durée de R42.

**DV11 (R43).** Chariot avec un cochon et un poulet entrant dans un portail ; puis bateau avec une vache ; lecture de la relation de monture (`Passengers`) et de l'UUID. Critère de succès : R43.

**DV12 (R48, R50).** Portail de l'End posé par `setblock minecraft:end_portal` (3 x 3) dans l'Overworld puis dans le Nether ; objet, vache et chariot entrant ; `Pos`, dimension. Critère de succès : (100.5, 49, 0.5) ou valeur relevée, identique des deux origines.

**DV13 (R48).** Avant l'entrée : bloc de pierre en (100, 49, 0), de la terre à (98, 48, -2), de l'eau à (102, 50, 2) ; après : les 5 x 5 x 4 blocs ; entrée de la seconde entité : la plateforme est-elle recomposée ? Variantes : entrée d'un joueur (J) puis d'une entité ; obsidienne détruite avant l'entrée ; bloc posé sur les trois couches d'air (objets lâchés). Critère de succès : état des 100 blocs conforme à R48 (H-PT5).

**DV14 (R49).** Un `end_portal` posé dans l'End par `setblock` ; objet et vache entrant ; `Pos` et dimension à l'arrivée ; puis `setworldspawn` en (50, 70, 50) dans le Nether (puis dans l'End) et nouvelle entrée. Critère de succès : arrivée égale à `spawn` de `level.dat` dans la dimension qu'il nomme (R49).

**DV15 (R52, R53).** `data get block <x> <y> <z>` d'une passerelle posée par `place feature minecraft:end_gateway_return` (hautes terres) et d'une passerelle posée par `setblock minecraft:end_gateway{ExactTeleport:1b,exit_portal:{X:0,Y:64,Z:0}}` ; objet entrant à chaque tick pendant 200 ticks ; instants de téléportation. Critère de succès : clés de R52 ; intervalle de 40 ticks.

**DV16 (R54).** Les 20 passerelles de l'anneau (DE13) ; pour chacune, un objet entrant ; lecture de `exit_portal` (`data get block`). Critère de succès : distance à l'origine et position de sortie conformes à R54, ou règle remplacée.

**DV17 (R55, R56).** Dix passerelles dont la destination est préparée (sol plat, haies de blocs, absence de sol) ; objet entrant avec `ExactTeleport` 0 et 1 ; `Pos` à l'arrivée. Variante : passerelle naturelle (R56), plateforme avant / après. Critère de succès : positions égales à la prédiction.

**DV18 (R57).** Vache (nom, `Tags`, `Health` 7, `ActiveEffects`, `Fire` 100, `Air` 100, `FallDistance` 8.0, `CustomNameVisible`, `PersistenceRequired`, `Leash`), objet (`Age` 100, `PickupDelay` 20, `Owner`), armor stand (`Pose`, `Tags`, équipement) : arbre NBT complet (`data get entity @s`) avant le transfert (arrêt de 1 tick avant l'entrée) et après ; différence clé par clé (ordre ignoré). Critère de succès : seules les clés de R57 diffèrent.

**DV19 (R59).** Vache attachée par une laisse à un poteau (`leash_knot`) puis entrant dans un portail ; cheval sellé monté par un armor stand ; chariot lié à un autre ; lecture des liens après. Critère de succès : R59 ; message de `ride` sur dimension différente.

**DV20 (R60, R61).** (a) Vache à Y -63, -64, -65 (Nether, End), -127, -128, -129 (Overworld) avec `NoGravity` ; santé à 100 ticks ; (b) objet au sol sous le seuil ; (c) `execute in minecraft:the_nether run worldborder set 1000` seul ; `worldborder get` dans les trois dimensions ; (d) un objet envoyé d'une position hors de la bordure du Nether. Critère de succès : seuils et dégâts de R60 ; bordure de R61.

**DV21 (J ; R14, R27, R49, R58, R62).** Un joueur vanilla connecté : (a) délai de portail 0, 1, 80, 200 par `gamerule` : tick de changement ; (b) `players/data/<uuid>.dat` avant / après un aller-retour Overworld-Nether-Overworld avec inventaire, XP, effets, faim, santé 10 ; (c) retour de l'End par le portail de sortie avec lit puis avec ancre, puis sans point d'apparition ; `seenCredits` ; (d) chute dans le vide de l'End et du Nether. Critère de succès : conformité aux règles citées.

## Récapitulatif

- Sondes : DP 15 (dont J : DP10, DP11), DV 21 (dont J : DV21) ; total 36.
- Prérequis : WP0 de `worldgen-pipeline.md`, lecteur `.mca` indépendant, EULA acceptée, DP0 acquise avant tout.
- Aucune sonde n'emploie une classe ou une méthode du jeu ; toutes les interfaces sont des commandes, des fichiers de région et des fichiers `.dat`.

## Annexe : `dim_ref.py`

Le texte intégral du script suit (copie de `tools/dim_ref.py` de l'export). Il ne lit que des fichiers du cache d'extraction, ne copie aucune donnée dans git et ne lance pas le jeu.

```python
#!/usr/bin/env python3
"""Read-only inventory of the extracted 26.3 data for dimensions, Nether, End and portals.

Usage: python -I dim_ref.py REF_DIR SUBCOMMAND [ARGS]
  REF_DIR = .cache/reference/26.3
Subcommands: dimtypes, presets, noise nether|end, surface-nether, bedrock, biomes, features,
             structures, bastion, portal, order, vectors, portalvec, grids
The tool only reads and counts, and computes small closed-form tables from values it reads.
It copies no Mojang data beyond a few example values.
"""
import collections
import glob
import json
import math
import os
import sys

REF = sys.argv[1]
D = os.path.join(REF, "resources/data/minecraft")
W = os.path.join(D, "worldgen")
sys.stdout.reconfigure(newline="\n")


def jl(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def short(x):
    return str(x).replace("minecraft:", "")


def dump(x):
    return json.dumps(x, sort_keys=True, separators=(",", ":"))


def registries():
    return jl(os.path.join(REF, "generated/reports/registries.json"))


DIMS = ["overworld", "overworld_caves", "the_nether", "the_end"]


def cmd_dimtypes():
    dts = {d: jl(os.path.join(D, "dimension_type", d + ".json")) for d in DIMS}
    scalar = sorted({k for v in dts.values() for k in v if k != "attributes"})
    print("files", len(glob.glob(os.path.join(D, "dimension_type", "*.json"))))
    for k in scalar:
        print("%-32s" % k, " | ".join("%s=%s" % (d, dump(dts[d].get(k, "(absent)"))) for d in DIMS))
    attrs = sorted({k for v in dts.values() for k in v["attributes"]})
    print("attributes", len(attrs))
    for k in attrs:
        print(short(k))
        for d in DIMS:
            print("   %-16s %s" % (d, dump(dts[d]["attributes"].get(k, "(absent)"))))
    reg = registries()["minecraft:environment_attribute"]["entries"]
    print("registry environment_attribute", len(reg), "used by dimension types", len(attrs))


def cmd_presets():
    for n in ["normal", "flat_all_dimensions"]:
        p = jl(os.path.join(W, "world_preset", n + ".json"))
        print("==", n)
        for k, v in sorted(p["dimensions"].items()):
            g = v["generator"]
            line = [short(k), "type=" + short(v["type"]), "generator=" + short(g["type"])]
            if "biome_source" in g:
                line.append("biome_source=" + dump(g["biome_source"]))
                line.append("settings=" + short(g["settings"]))
            else:
                line.append("flat=" + dump(g["settings"]))
            print(" ", " ".join(line))


def tree_nodes(n, acc):
    if isinstance(n, dict):
        if "type" in n:
            acc[short(n["type"])] += 1
        for v in n.values():
            tree_nodes(v, acc)
    elif isinstance(n, list):
        for v in n:
            tree_nodes(v, acc)


def cmd_noise(which):
    ns = jl(os.path.join(W, "noise_settings", which + ".json"))
    print("settings", which, "keys", sorted(ns))
    for k in ["noise", "sea_level", "default_block", "default_fluid", "legacy_random_source",
              "disable_mob_generation", "material_rule", "spawn_target"]:
        print(" ", k, dump(ns.get(k)))
    print("  aquifers present", "aquifers" in ns, "ore_veins in rule", "ore_vein" in dump(ns.get("material_rule")))
    r = ns["noise_router"]
    for k in sorted(r):
        v = r[k]
        print("  router", k, "=", v if not isinstance(v, dict) else "<" + short(v["type"]) + ">" if "type" in v else v)
    acc = collections.Counter()
    tree_nodes(r["final_density"], acc)
    print("  final_density node types", dict(sorted(acc.items())))
    fd = r["final_density"]
    inter = fd["left"]["input"]
    print("  interpolated cell_size_xz", inter["cell_size_xz"], "cell_size_y", inter["cell_size_y"])
    mul = inter["input"]
    print("  scale factor (mul right)", mul["right"])
    lerp1 = mul["left"]["input"]
    print("  outer lerp: alpha", dump(lerp1["alpha"]), "first", lerp1["first"])
    lerp2 = lerp1["second"]
    print("  inner lerp: alpha", dump(lerp2["alpha"]), "first", lerp2["first"], "second", dump(lerp2["second"]))
    for ref in sorted({lerp2["second"]} if isinstance(lerp2["second"], str) else []):
        name = short(ref)
        f = jl(os.path.join(W, "density_function", name + ".json"))
        print("  function", name, dump(f))
    if which == "end":
        for n in ["end/islands", "end/sloped_cheese"]:
            print("  function", n, dump(jl(os.path.join(W, "density_function", n + ".json"))))
    if which == "nether":
        for nz in sorted(glob.glob(os.path.join(W, "noise", "nether", "*.json"))):
            print("  noise nether/" + os.path.basename(nz)[:-5], dump(jl(nz)))
        for n in ["nether_state_selector", "nether_wart", "netherrack", "soul_sand_layer", "gravel_layer", "patch"]:
            print("  noise", n, dump(jl(os.path.join(W, "noise", n + ".json"))))


def anchor(a):
    k, v = next(iter(a.items()))
    return "%s=%s" % (k, v)


def cond(c):
    if isinstance(c, str):
        return "@" + short(c)
    t = short(c["type"])
    if t == "biome":
        return "biome(%s)" % short(c["biome_is"])
    if t == "noise_threshold":
        hi = c["max_threshold"]
        return "noise(%s,%g..%s)" % (short(c["noise"]), c["min_threshold"], "inf" if hi > 1e300 else "%g" % hi)
    if t == "y_above":
        return "y_above(%s,sdm=%s,asd=%s)" % (anchor(c["anchor"]), c["surface_depth_multiplier"], c["add_stone_depth"])
    if t == "not":
        return "not(%s)" % cond(c["invert"])
    if t == "vertical_gradient":
        return "vgrad(%s,true<=%s,false>=%s)" % (short(c["random_name"]), anchor(c["true_at_and_below"]),
                                                 anchor(c["false_at_and_above"]))
    if t == "hole":
        return "hole"
    return dump(c)


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
    return p + dump(r) + "\n"


def cmd_surface(names):
    for n in names:
        print("=====", n)
        print(rule(jl(os.path.join(W, "material_rule", n + ".json"))), end="")
    print("=====", "conditions")
    for n in ["on_floor", "under_floor", "on_ceiling", "under_ceiling"]:
        print(n, dump(jl(os.path.join(W, "material_condition", n + ".json"))))


def cmd_bedrock():
    """Closed-form table from the vertical_gradient anchors of the two bedrock rules."""
    for which in ["nether"]:
        ns = jl(os.path.join(W, "noise_settings", which + ".json"))
        mn, h = ns["noise"]["min_y"], ns["noise"]["height"]
        print("== settings", which, "min_y", mn, "height", h, "top block", mn + h - 1)

        def res(a):
            k, v = next(iter(a.items()))
            return {"above_bottom": mn + v, "below_top": mn + h - 1 - v, "absolute": v}[k]
        for n in ["bedrock_floor", "bedrock_roof"]:
            vg = jl(os.path.join(W, "material_rule", n + ".json"))["if_true"]
            neg = vg["type"] == "minecraft:not"
            if neg:
                vg = vg["invert"]
            t, f = res(vg["true_at_and_below"]), res(vg["false_at_and_above"])
            print(n, "negated" if neg else "direct", "true_at_and_below", t, "false_at_and_above", f)
            row = []
            for y in range(min(t, f) - 1, max(t, f) + 2):
                p = 1.0 if y <= t else 0.0 if y >= f else (f - y) / (f - t)
                if neg:
                    p = 1.0 - p
                row.append("y%d:%g" % (y, round(p, 6)))
            print("  P(bedrock) " + " ".join(row))
    print("carver nether_cave y range", dump(jl(os.path.join(W, "carver", "nether_cave.json"))["y"]),
          "-> absolute 0 .. below_top 1 on noise height 128 =", 0, "..", 128 - 1 - 1)


def cmd_biomes():
    nb = jl(os.path.join(REF, "generated/reports/biome_parameters/minecraft/nether.json"))["biomes"]
    print("nether biome table", len(nb), "entries")
    for e in nb:
        p = e["parameters"]
        print(" ", short(e["biome"]), "temperature", p["temperature"], "humidity", p["humidity"], "offset", p["offset"],
              "others", sorted({p[k] for k in p if k not in ("temperature", "humidity", "offset")}))
    names = ["nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas",
             "the_end", "small_end_islands", "end_midlands", "end_highlands", "end_barrens"]
    steps = ["raw_generation", "lakes", "local_modifications", "underground_structures", "surface_structures",
             "strongholds", "underground_ores", "underground_decoration", "fluid_springs", "vegetal_decoration",
             "top_layer_modification"]
    for n in names:
        b = jl(os.path.join(W, "biome", n + ".json"))
        attr = b.get("attributes", {})
        sp = attr.get("minecraft:gameplay/natural_mob_spawns", {}).get("argument", {}).get("spawns_by_category", {})
        print("==", n, "temperature", b["temperature"], "downfall", b["downfall"], "carvers", dump(b.get("carvers")),
              "lists", len(b["features"]), "spawn entries", {k: len(v) for k, v in sorted(sp.items())})
        for i, lst in enumerate(b["features"]):
            if lst:
                print("   step", i, steps[i], len(lst), [short(x) for x in lst])
    print("tags is_nether", dump(jl(os.path.join(D, "tags/worldgen/biome/is_nether.json"))["values"]))
    print("tags is_end", dump(jl(os.path.join(D, "tags/worldgen/biome/is_end.json"))["values"]))


def resolve(a, mn, h):
    k, v = next(iter(a.items()))
    return {"above_bottom": mn + v, "below_top": mn + h - 1 - v, "absolute": v}[k]


def modsum(m):
    t = short(m["type"])
    if t == "count":
        return "count=" + dump(m["count"])
    if t == "rarity_filter":
        return "rarity=1/%s" % m["chance"]
    if t == "height_range":
        h = m["height"]
        return "y=%s[%s..%s]=%d..%d(gen 0,128)" % (short(h["type"]), dump(h.get("min_inclusive")), dump(h.get("max_inclusive")),
                                                    resolve(h["min_inclusive"], 0, 128), resolve(h["max_inclusive"], 0, 128))
    if t == "heightmap":
        return "heightmap=" + m["heightmap"]
    if t in ("in_square", "biome"):
        return t
    if t == "count_on_every_layer":
        return "count_on_every_layer=%s" % dump(m["count"])
    if t == "fixed_placement":
        return "fixed=" + dump(m["positions"])
    return t


def cmd_features():
    names = ["nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas",
             "the_end", "small_end_islands", "end_midlands", "end_highlands", "end_barrens"]
    seen = collections.OrderedDict()
    for n in names:
        b = jl(os.path.join(W, "biome", n + ".json"))
        for i, lst in enumerate(b["features"]):
            for f in lst:
                seen.setdefault(short(f), set()).add((n, i))
    types = collections.Counter()
    print("distinct placed features", len(seen))
    for pf in sorted(seen):
        p = jl(os.path.join(W, "placed_feature", pf + ".json"))
        cfn = short(p["feature"])
        c = jl(os.path.join(W, "feature", cfn + ".json"))
        types[short(c["type"])] += 1
        users = sorted({b for b, _ in seen[pf]})
        print("%-26s feature=%-28s type=%-26s %s | biomes=%d" % (pf, cfn, short(c["type"]),
              " ".join(modsum(m) for m in p["placement"]), len(users)))
    print("configured feature types", dict(sorted(types.items())))


def cmd_order():
    """Partial order of the per-step feature lists across the five Nether biomes (as in the Overworld spec, R9)."""
    names = ["nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas"]
    bio = {n: jl(os.path.join(W, "biome", n + ".json")) for n in names}
    for st in range(11):
        feats = collections.OrderedDict()
        edges = set()
        for n in sorted(bio):
            lst = bio[n]["features"][st] if st < len(bio[n]["features"]) else []
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
        fl = list(feats)
        for f in fl:
            dfs(f, set())
        inc = sum(1 for i in range(len(fl)) for j in range(i + 1, len(fl))
                  if fl[j] not in reach[fl[i]] and fl[i] not in reach[fl[j]])
        print("step", st, "features", len(fl), "edges", len(edges), "cycle", state["cyc"],
              "incomparable_pairs", inc, "pairs", len(fl) * (len(fl) - 1) // 2)


def cmd_portalvec():
    """Closed-form tables that depend only on rules R23, R46, R54 of dim-definitions-portals.md (S)."""
    print("== DX3 interior sizes under R23: width 2..21, height 3..21")
    ws, hs = range(2, 22), range(3, 22)
    print("valid interiors", len(ws) * len(hs), "; frame outer sizes from", (2 + 2, 3 + 2), "to", (21 + 2, 21 + 2))
    for w, h in [(2, 3), (2, 4), (3, 3), (4, 5), (10, 10), (21, 21)]:
        print("  interior %2dx%2d obsidian with corners %3d without corners %3d" % (w, h, 2 * (w + h) + 4, 2 * (w + h)))
    print("  rejected examples: width 1, height 2, width 22, height 22 (first excluded value on each side)")
    print("== DX5 pre-placed eyes in a stronghold portal room: Binomial(12, 0.1) (rule R47)")
    tot = 0.0
    for k in range(13):
        pk = math.comb(12, k) * 0.1 ** k * 0.9 ** (12 - k)
        print("  k=%2d P=%.9f" % (k, pk))
        if k >= 5:
            tot += pk
    print("  P(k>=5) = %.6f ; E[k] = %.2f" % (tot, 12 * 0.1))
    print("== DX6 gateway link lengths (rule R54): 768 to 1280 by steps of 16")
    ls = list(range(768, 1281, 16))
    print("  count", len(ls), "; default 1024 at index", ls.index(1024), "; steps down", (1024 - 768) // 16, "steps up", (1280 - 1024) // 16)


def cmd_grids():
    """Counts and expectations that follow from values read in the data (rules C of the three dim specs)."""
    print("== DX18 sections, positions and biome cells per chunk")
    for d in DIMS:
        t = jl(os.path.join(D, "dimension_type", d + ".json"))
        secs = t["height"] // 16
        print("  %-16s height %d sections %d positions %d biome cells %d section y %d..%d" % (
            d, t["height"], secs, secs * 4096, secs * 64, t["min_y"] // 16, t["min_y"] // 16 + secs - 1))
    print("== DX19 interpolation corners per chunk, from the cell sizes of the interpolated node (rule: cells tile the chunk)")
    for which in ["nether", "end"]:
        ns = jl(os.path.join(W, "noise_settings", which + ".json"))
        inter = ns["noise_router"]["final_density"]["left"]["input"]
        cx, cy = inter["cell_size_xz"], inter["cell_size_y"]
        h = ns["noise"]["height"]
        nx, ny = 16 // cx + 1, h // cy + 1
        print("  %-6s cell %d x %d x %d, noise height %d -> corners %d x %d x %d = %d" % (which, cx, cy, cx, h, nx, ny, nx, nx * ny * nx))
    print("== DX20 Nether climate does not depend on y")
    ns = jl(os.path.join(W, "noise_settings", "nether.json"))
    r = ns["noise_router"]
    ok = True
    for k in ["chunk_surface_level", "continents", "depth", "erosion", "ridges"]:
        ok = ok and isinstance(r[k], float)
    for k in ["temperature", "vegetation"]:
        ok = ok and r[k]["type"] == "minecraft:noise" and r[k]["y_scale"] == 0.0 and "shift_y" not in r[k]
    print("  four constants, two noise nodes with y_scale 0.0 and no y shift:", ok)
    print("== DX21 per-chunk expectations of End features (rarity and count read in the placed features)")
    f = jl(os.path.join(W, "placed_feature", "end_island_decorated.json"))
    rar = [m for m in f["placement"] if m["type"] == "minecraft:rarity_filter"][0]["chance"]
    cnt = [m for m in f["placement"] if m["type"] == "minecraft:count"][0]["count"]["distribution"]
    mean = sum(e["data"] * e["weight"] for e in cnt) / sum(e["weight"] for e in cnt)
    print("  end_island_decorated: 1/%d x mean count %.2f = %.4f attempts per chunk" % (rar, mean, mean / rar))
    f = jl(os.path.join(W, "placed_feature", "chorus_plant.json"))
    c = [m for m in f["placement"] if m["type"] == "minecraft:count"][0]["count"]
    print("  chorus_plant: uniform %d..%d -> mean %.2f attempts per chunk" % (c["min_inclusive"], c["max_inclusive"],
                                                                         (c["min_inclusive"] + c["max_inclusive"]) / 2))
    f = jl(os.path.join(W, "placed_feature", "end_gateway_return.json"))
    rar = [m for m in f["placement"] if m["type"] == "minecraft:rarity_filter"][0]["chance"]
    print("  end_gateway_return: 1/%d per chunk, i.e. %.5f" % (rar, 1.0 / rar))
    print("== DX22 structure set weights to probabilities")
    d = jl(os.path.join(W, "structure_set", "nether_complexes.json"))
    tot = sum(x["weight"] for x in d["structures"])
    for x in d["structures"]:
        print("  %s weight %d of %d = %.2f" % (short(x["structure"]), x["weight"], tot, x["weight"] / tot))
    print("  start pool of bastion_remnant: %d equal-weight elements" % len(jl(os.path.join(W, "template_pool/bastion/starts.json"))["elements"]))


def cmd_structures():
    for s in ["nether_complexes", "nether_fossils", "end_cities", "ruined_portals"]:
        d = jl(os.path.join(W, "structure_set", s + ".json"))
        print("set", s, dump(d["placement"]), [(short(x["structure"]), x["weight"]) for x in d["structures"]])
        pl = d["placement"]
        sp, se = pl["spacing"], pl["separation"]
        print("   region", sp * 16, "blocks; candidate offset range 0..%d chunks (%d positions, %d blocks);"
              " excluded strip %d chunks" % (sp - se - 1, sp - se, (sp - se) * 16, se))
    for s in ["fortress", "bastion_remnant", "nether_fossil", "ruined_portal_nether", "end_city"]:
        d = jl(os.path.join(W, "structure", s + ".json"))
        print("structure", s, dump({k: v for k, v in d.items() if k != "spawn_overrides"}))
        so = d.get("spawn_overrides", {})
        for cat, v in sorted(so.items()):
            print("   spawn_overrides", cat, "box", v["bounding_box"], "entries", len(v["spawns"]),
                  "total weight", sum(x["weight"] for x in v["spawns"]))
    for t in ["nether_fortress", "bastion_remnant", "nether_fossil", "ruined_portal_nether", "end_city"]:
        print("tag has_structure/" + t, dump(jl(os.path.join(D, "tags/worldgen/biome/has_structure", t + ".json"))["values"]))
    n = len(glob.glob(os.path.join(D, "structure/end_city/*.nbt")))
    f = len(glob.glob(os.path.join(D, "structure/nether_fossils/*.nbt")))
    rp = len(glob.glob(os.path.join(D, "structure/ruined_portal/*.nbt")))
    print("templates: end_city", n, "nether_fossils", f, "ruined_portal", rp, "bastion",
          len(glob.glob(os.path.join(D, "structure/bastion/**/*.nbt"), recursive=True)))
    reg = registries()
    pieces = [short(x) for x in reg["minecraft:worldgen/structure_piece"]["entries"]]
    print("pieces ne*", [p for p in pieces if p.startswith("ne")], "pieces ec*", [p for p in pieces if p.startswith("ec")])
    print("loot tables", sorted(os.path.basename(x)[:-5] for x in glob.glob(os.path.join(D, "loot_table/chests/*.json"))
                                if any(k in x for k in ("bastion", "nether_bridge", "end_city", "ruined_portal"))))


def cmd_bastion():
    pools = glob.glob(os.path.join(W, "template_pool", "bastion", "**", "*.json"), recursive=True)
    pools.append(os.path.join(W, "template_pool", "bastion", "starts.json"))
    pools = sorted(set(pools))
    et = collections.Counter()
    proj = collections.Counter()
    procs = collections.Counter()
    fallback = collections.Counter()
    total_el = 0
    for p in pools:
        d = jl(p)
        fallback[short(d.get("fallback", "-"))] += 1
        for e in d["elements"]:
            el = e["element"]
            et[short(el["element_type"])] += 1
            proj[el.get("projection", "-")] += 1
            procs[short(el.get("processors", "-"))] += 1
            total_el += 1
    print("bastion pools", len(pools), "elements", total_el)
    print("element types", dict(sorted(et.items())))
    print("projections", dict(sorted(proj.items())))
    print("processor lists", dict(sorted(procs.items())))
    print("fallbacks", dict(sorted(fallback.items())))
    pl = jl(os.path.join(W, "processor_list", "bastion_generic_degradation.json"))
    print("processor_list bastion_generic_degradation: processors", len(pl["processors"]),
          "types", dict(collections.Counter(short(x["processor_type"]) for x in pl["processors"])),
          "rule count", sum(len(x.get("rules", [])) for x in pl["processors"]))
    by_dir = collections.Counter()
    for f in glob.glob(os.path.join(D, "structure/bastion/**/*.nbt"), recursive=True):
        by_dir[os.path.relpath(f, os.path.join(D, "structure/bastion")).replace(os.sep, "/").split("/")[0]] += 1
    print("templates by top folder", dict(sorted(by_dir.items())))


def cmd_portal():
    reg = registries()
    print("tags nether_portal_frame", dump(jl(os.path.join(D, "tags/block/nether_portal_frame.json"))["values"]))
    print("tags portals", dump(jl(os.path.join(D, "tags/block/portals.json"))["values"]))
    for t in ["infiniburn_overworld", "infiniburn_nether", "infiniburn_end"]:
        print("tags", t, dump(jl(os.path.join(D, "tags/block", t + ".json"))["values"]))
    print("tags dragon_immune", len(jl(os.path.join(D, "tags/block/dragon_immune.json"))["values"]), "entries")
    blocks = jl(os.path.join(REF, "generated/reports/blocks.json"))
    for k in ["nether_portal", "end_portal", "end_gateway", "end_portal_frame", "respawn_anchor", "obsidian",
              "crying_obsidian", "dragon_egg", "bedrock", "nether_portal"]:
        b = blocks["minecraft:" + k]
        print("block", k, "properties", dump(b.get("properties")), "states", len(b["states"]))
    print("poi types", [short(x) for x in reg["minecraft:point_of_interest_type"]["entries"] if "portal" in x or "lodestone" in x])
    print("ticket types", [short(x) for x in reg["minecraft:ticket_type"]["entries"]])
    print("block entity types", [short(x) for x in reg["minecraft:block_entity_type"]["entries"]
                                 if any(s in x for s in ("portal", "gateway"))])
    print("entity types", [short(x) for x in reg["minecraft:entity_type"]["entries"]
                           if any(s in x for s in ("dragon", "crystal", "pearl", "eye_of"))])
    print("triggers", [short(x) for x in reg["minecraft:trigger_type"]["entries"]
                       if any(s in x for s in ("dimension", "nether_travel", "used_ender"))])
    for g in ["allow_entering_nether_using_portals", "players_nether_portal_default_delay",
              "players_nether_portal_creative_delay", "ender_pearls_vanish_on_death", "respawn_radius"]:
        print("game_rule present", g, "minecraft:" + g in reg["minecraft:game_rule"]["entries"])
    print("placed end_platform", dump(jl(os.path.join(W, "placed_feature/end_platform.json"))))
    print("configured end_gateway_return", dump(jl(os.path.join(W, "feature/end_gateway_return.json"))))
    print("configured end_spike", dump(jl(os.path.join(W, "feature/end_spike.json"))))
    for n in ["world_clock/overworld", "world_clock/the_end"]:
        print(n, dump(jl(os.path.join(D, n + ".json"))))
    print("tags timeline in_*", {t: jl(os.path.join(D, "tags/timeline/in_%s.json" % t))["values"] for t in ["overworld", "nether", "end"]})


def lerp(a, f, s):
    return f * (1 - a) + s * a


def grad(y, y0, v0, y1, v1):
    if y <= y0:
        return v0
    if y >= y1:
        return v1
    return v0 + (v1 - v0) * (y - y0) / (y1 - y0)


def cmd_vectors():
    """Closed-form tables, rule semantics of lerp/gradient/clamp from worldgen-noise-density R15 R17 (S)."""
    print("== DX7 Nether: final_density as a function of y, with n = value of the blended noise (unknown amplitude)")
    ns = jl(os.path.join(W, "noise_settings", "nether.json"))
    fd = ns["noise_router"]["final_density"]["left"]["input"]["input"]
    scale = fd["right"]
    l1 = fd["left"]["input"]
    g1 = l1["alpha"]
    l2 = l1["second"]
    g2 = l2["alpha"]
    print("scale", scale, "bottom ramp", g1["from_coordinate"], "->", g1["to_coordinate"], "first", l1["first"],
          "top ramp", g2["from_coordinate"], "->", g2["to_coordinate"], "first", l2["first"])
    for y in [0, 4, 8, 12, 16, 20, 23, 24, 64, 104, 105, 110, 116, 122, 126, 127]:
        a1 = grad(y, g1["from_coordinate"], g1["from_value"], g1["to_coordinate"], g1["to_value"])
        a2 = grad(y, g2["from_coordinate"], g2["from_value"], g2["to_coordinate"], g2["to_value"])
        c0 = scale * lerp(a1, l1["first"], lerp(a2, l2["first"], 0.0))
        k = scale * a1 * a2
        if k > 0:
            thr = -scale * (1 - a1) * l1["first"] / k - scale * a1 * (1 - a2) * l2["first"] / k
            thr_s = "%+.6f" % thr if abs(thr) > 1e-12 else "0 (strictly positive)"
        else:
            thr_s = "none"
        print("y=%3d a_bottom=%.6f a_top=%.6f density(n=0)=%+.6f ; solid iff n > %s" % (y, a1, a2, c0, thr_s))
    print("== DX13 End: noise-free profile above the central plateau (islands = max value)")
    es = jl(os.path.join(W, "noise_settings", "end.json"))
    fd = es["noise_router"]["final_density"]["left"]["input"]["input"]
    scale = fd["right"]
    l1 = fd["left"]["input"]
    g1 = l1["alpha"]
    l2 = l1["second"]
    g2 = l2["alpha"]
    isl = jl(os.path.join(W, "density_function/end/islands.json"))["input"]["left"]["input"]
    ra = isl["left"]
    clamp = ra["left"]
    hi, lo = clamp["max"], clamp["min"]
    sub = ra["right"]
    mulc = isl["right"]
    base = clamp["input"]["left"]
    print("islands central term: clamp(%s - dist, %s, %s) - %s) * %s" % (base, lo, hi, sub, mulc))

    def central(d):
        return (min(max(base - d, lo), hi) - sub) * mulc
    for d in [0, 10, 20, 21, 50, 80, 90, 92, 93, 100, 150, 200, 300]:
        print("  d=%4d central=%+.6f" % (d, central(d)))
    zero_d = base - sub
    print("  central term is 0 at d =", zero_d, "; plateau up to d =", base - hi, "; floor from d =", base - lo)
    print("  bottom ramp", g1["from_coordinate"], "->", g1["to_coordinate"], "first", l1["first"],
          "; top ramp", g2["from_coordinate"], "->", g2["to_coordinate"], "first", l2["first"], "scale", scale)
    s0 = central(0)
    for y in [3, 4, 8, 12, 13, 16, 20, 32, 56, 60, 61, 62, 63, 64, 70, 100, 127]:
        a1 = grad(y, g1["from_coordinate"], g1["from_value"], g1["to_coordinate"], g1["to_value"])
        a2 = grad(y, g2["from_coordinate"], g2["from_value"], g2["to_coordinate"], g2["to_value"])
        v = scale * lerp(a1, l1["first"], lerp(a2, l2["first"], s0))
        print("  y=%3d a_bottom=%.6f a_top=%.6f density(islands=%.4f, other=0)=%+.6f %s" % (
            y, a1, a2, s0, v, "solid" if v > 0 else "not solid"))
    lo_y = next(y for y in range(0, 128) if scale * lerp(grad(y, g1["from_coordinate"], g1["from_value"], g1["to_coordinate"], g1["to_value"]),
                l1["first"], lerp(grad(y, g2["from_coordinate"], g2["from_value"], g2["to_coordinate"], g2["to_value"]), l2["first"], s0)) > 0)
    hi_y = max(y for y in range(0, 128) if scale * lerp(grad(y, g1["from_coordinate"], g1["from_value"], g1["to_coordinate"], g1["to_value"]),
               l1["first"], lerp(grad(y, g2["from_coordinate"], g2["from_value"], g2["to_coordinate"], g2["to_value"]), l2["first"], s0)) > 0)
    print("  noise-free solid band at the plateau (integer y, no cell interpolation): %d .. %d" % (lo_y, hi_y))
    print("== DX9 (Nether) and DX14 (End) region grids (random_spread) from the sets")
    for s, sp, se in [("nether_complexes", 27, 4), ("nether_fossils", 2, 1), ("end_cities", 20, 11)]:
        print("  %s spacing %d separation %d -> region %d blocks, %d candidate positions per axis (%d blocks)" % (
            s, sp, se, sp * 16, sp - se, (sp - se) * 16))
    print("== DX4 Nether portal scale table (hypothesis H-PT1: target = floor(coordinate * source_scale / target_scale))")
    for src, dst in [(1.0, 8.0), (8.0, 1.0)]:
        for x in [-9, -8, -1, 0, 1, 7, 8, 15, 16, 100, -100]:
            print("  scale %g -> %g : x=%4d -> %d" % (src, dst, x, math.floor(x * src / dst)))
    print("== DX16 End gateway ring (wiki End_gateway, 2026-10-29): 20 positions, radius 96, y 75; index k = 0..19 (gateway n = k + 1)")
    amb = []
    pos = {}
    for k in range(0, 20):
        a = math.pi * k / 10.0
        x, z = 96 * math.cos(a), 96 * math.sin(a)
        fl = (math.floor(x), math.floor(z))
        tr = (int(x), int(z))
        flag = "" if fl == tr else " DIFFERS"
        if abs(x) < 1e-9 or abs(z) < 1e-9:
            amb.append(k)
        pos[k] = fl
        print("  k=%2d gateway %2d floor=%s trunc=%s%s" % (k, k + 1, fl, tr, flag))
    print("  axis-aligned k:", amb)
    print("  alternative indexing k = 1..20 would give gateway 1 at", pos[1], "and a 20th gateway at", (math.floor(96 * math.cos(2 * math.pi)), math.floor(96 * math.sin(2 * math.pi))), "instead of", pos[0])
    print("  page examples (End_gateway): gateway 1 (96,0), gateway 6 (-1,96), gateway 20 (91,-30)")
    for n, ex in [(1, (96, 0)), (6, (-1, 96)), (20, (91, -30))]:
        print("   gateway %2d computed floor %s page %s %s" % (n, pos[n - 1], ex, "match" if pos[n - 1] == ex else "MISMATCH"))
    print("== DX17 End spikes (wiki 'Obsidian Pillar', 2026-10-28): centres, top y, distance of centre to origin")
    centres = [(-42, -1), (-34, -25), (-34, 24), (-13, -40), (-13, 39), (12, -40), (12, 39), (33, -25), (33, 24), (42, 0)]
    for x, z in sorted(centres, key=lambda c: math.atan2(c[1], c[0])):
        print("  centre (%d,%d) distance %.3f" % (x, z, math.hypot(x, z)))
    print("  distances min %.3f max %.3f" % (min(math.hypot(x, z) for x, z in centres), max(math.hypot(x, z) for x, z in centres)))
    tops = [76 + 3 * i for i in range(10)]
    print("  top y list", tops, "cages at tops", [tops[1], tops[2]])


def main():
    cmd = sys.argv[2]
    rest = sys.argv[3:]
    if cmd == "dimtypes":
        cmd_dimtypes()
    elif cmd == "presets":
        cmd_presets()
    elif cmd == "noise":
        cmd_noise(rest[0])
    elif cmd == "surface-nether":
        cmd_surface(rest or ["nether", "end", "bedrock_floor", "bedrock_roof"])
    elif cmd == "bedrock":
        cmd_bedrock()
    elif cmd == "biomes":
        cmd_biomes()
    elif cmd == "features":
        cmd_features()
    elif cmd == "grids":
        cmd_grids()
    elif cmd == "portalvec":
        cmd_portalvec()
    elif cmd == "order":
        cmd_order()
    elif cmd == "structures":
        cmd_structures()
    elif cmd == "bastion":
        cmd_bastion()
    elif cmd == "portal":
        cmd_portal()
    elif cmd == "vectors":
        cmd_vectors()
    else:
        sys.exit("unknown subcommand " + cmd)


main()
```

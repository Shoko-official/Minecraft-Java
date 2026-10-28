# Vecteurs et sondes : apparition, disparition, spawners, IA

Complète `entity-ai-spawning.md`. L'outil `ent_ref.py`, le harnais commun et les empreintes sont dans `entity-model-physics-vectors.md`. Les vecteurs V-EA sont des calculs sur les données extraites (D, C). Les sondes EA0 à EA17 sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée). **Presque toutes les sondes d'apparition naturelle exigent un joueur** : un client de protocole minimal (jalon M8, règle R78) est le prérequis commun ; les sondes qui n'en ont pas besoin sont EA1, EA11 (variante sans joueur), EA12 à EA16 (cibles `NoAI` ou mobs entre eux).

## Commandes de l'outil utilisées

`python -I ent_ref.py "$REF" <sous-commande>` pour `spawns`, `dims`, `btags`, `etags`, `spawners`, `timelines`, `registry`. Les empreintes figurent dans le tableau de `entity-model-physics-vectors.md`.

## V-EA : vecteurs

**V-EA1 (R1 à R4) : listes d'apparition (D/C).** Sortie de `spawns` (extraits) :

```
biomes 67
monster biomes_with_entries 64 entries 480 types 22
creature biomes_with_entries 44 entries 176 types 20
ambient biomes_with_entries 55 entries 55 types 1 ['bat']
underground_water_creature biomes_with_entries 54 entries 54 types 1 ['glow_squid']
water_ambient biomes_with_entries 13 entries 20 types 4
water_creature biomes_with_entries 11 entries 25 types 3
axolotls biomes_with_entries 1 entries 1 types 1
distinct monster lists 21 ; the most common is shared by 28 biomes
```

La liste commune : araignée 100, zombie 95, villageois zombie 5, squelette 100, creeper 100, slime 100, enderman 10, sorcière 5 ; probabilité de sélection d'un villageois zombie par paquet : 5 / 515 (somme des poids 515), soit 0.97 pour cent par paquet de monstres (C) ; le « 5 pour cent des zombies » de R4 est le rapport des poids zombie et villageois zombie (5 sur 100).

**V-EA2 (R6) : surcharges de structure (D).** Sortie de `spawns`, section « structure spawn_overrides » : `fortress monster piece [('blaze', 10, {uniform 2..3}), ('zombified_piglin', 5, 4), ('wither_skeleton', 8, 5), ('skeleton', 2, 5), ('magma_cube', 3, 4)]` ; `monument monster full [('guardian', 1, {uniform 2..4})]` ; `pillager_outpost monster full [('pillager', 1, 1)]` ; `swamp_hut creature piece [('cat', 1, 1)]` et `monster piece [('witch', 1, 1)]` ; `ancient_city` et `trial_chambers` : huit catégories à liste vide.

**V-EA3 (R7) : lumière (D).** Sortie de `dims` : Overworld `monster_spawn_light_level {uniform 0..7}`, `monster_spawn_block_light_limit 0` ; Nether `7` et `15` ; End `15` et `0`.

**V-EA4 (R13, R14) : chronologies (D).** Sortie de `timelines` (extraits) :

```
day period 24000 gameplay/monsters_burn modifier or [(12542, False), (23460, True)]
day period 24000 gameplay/sky_light_level modifier multiply [(133, 1.0), (11867, 1.0), (13670, 0.26666668), (22330, 0.26666668)]
moon period 192000 gameplay/surface_slime_spawn_chance [(0, 0.5), (24000, 0.375), (48000, 0.25), (72000, 0.125), (96000, 0.0), (120000, 0.125), (144000, 0.25), (168000, 0.375)]
early_game gameplay/can_pillager_patrol_spawn modifier and [(0, False), (120000, True)]
villager_schedule gameplay/villager_activity [(10, idle), (2000, work), (9000, meet), (11000, idle), (12000, rest)]
derived: ticks before the end of the period at which monsters_burn turns true 540
```

**V-EA5 (R15) : spawners d'épreuve (D).** Sortie de `spawners` : 28 fichiers, clés `ticks_between_spawn` 28, `simultaneous_mobs_added_per_player` 28, `simultaneous_mobs` 25, `total_mobs` 6 (2, 4, 12 x4), `loot_tables_to_eject` 14 ; `trial_chamber/breeze/normal` : `simultaneous_mobs` 1.0, `total_mobs` 2.0 ; `trial_chamber/melee/zombie/normal` : `simultaneous_mobs` 3.0, `ticks_between_spawn` 20.

## Sondes (EA)

Rappel : cadre commun et seuils statistiques dans la spec (R75). « Client » = client de protocole minimal immobile à position fixe (R78).

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Critère de succès | Règles levées |
|---|---|---|---|---|
| EA0 | (client) Monde plat, `forceload add`, client à (0, -60, 0) ; `gamerule spawn_mobs true / false`, `spawn_monsters true / false`, `difficulty peaceful / easy / normal / hard` ; relevé de `@e` toutes les 20 ticks pendant 6000 ticks ; compteur de la catégorie `creature` | créatures seulement toutes les 400 ticks ; plus aucun monstre en Pacifique | les cinq règles (H-EA10 comprise) lues : aucune apparition quand `spawn_mobs` est faux, aucun monstre quand `spawn_monsters` est faux, créatures par salves espacées de 400 ticks (phase relevée), monstres retirés au tick suivant le passage en Pacifique | R17, R18, R30 |
| EA1 | Deux mondes neufs de même graine (cinq graines : 0, 12345, -1, un nombre négatif de 63 bits, un grand positif), 9 x 9 chunks forcés autour de l'origine ; après génération, `save-all` ; lecture des fichiers d'entités par le lecteur indépendant de `tools/oracle` (type, position, rotation, variante, santé, équipement) ; répéter dans 5 biomes imposés (`badlands`, `wooded_badlands`, `ice_spikes`, `snowy_plains`, `plains`) ; variante avec structures activées | listes d'entités par chunk | (a) listes identiques entre les deux mondes pour 5 graines (H-EA12 tenue) ; (b) fréquence de chunks avec au moins un paquet égale aux probabilités de R8 pour les 5 biomes (proportion à 5 écarts-types, 2 000 chunks par biome) et à 0.1 par défaut ; (c) format de stockage de R37 relevé | R8, R36, R37, R77 |
| EA2 | (client) Biome unique (`plains`, `forest`, `desert`, `jungle`, `nether_wastes`, `soul_sand_valley`, `deep_dark`), plafonds levés si possible (hors plafond : spawn via `summon` de contrôle) ; relever le type de chaque entité apparue pendant 200 000 paquets (ou 100 heures simulées avec `tick sprint`) | proportions = poids de V-EA1 | khi-deux entre effectifs observés et poids de R4 pour la liste commune, p >= 1e-4 ; 0 apparition de type absent de la liste ; villageois zombie à 5 sur 100 de (zombie + villageois zombie) | R23, R29, R31 |
| EA3 | (client) Plat de 400 x 400 blocs ; relever le nombre de mobs par catégorie toutes les 200 ticks sur 72 000 ticks avec 1 puis 2 puis 4 clients à écarts de 0, 16 et 100 blocs (chunks de l'union) ; nombre de chunks lu | maxima par catégorie = `plafond x chunks / 289` | maximum observé dans chaque catégorie égal à `floor` ou `ceil` du plafond de R20 (arrondi levé) ; décompte des persistants confirmé (R21) ; distances de 24 et 128 blocs mesurées par le plus proche et le plus éloigné apparus (R19) ; variante Nether pour les coûts (H-EA2) | R5, R19, R20, R21 |
| EA4 | (client) Salle de 15 x 15 sans ciel, éclairée par blocs de lumière de niveau 0 à 15 (`setblock minecraft:light[level=n]`), puis surface en plein jour, nuit, pluie ; Nether et End (chunks forcés, joueur téléporté) ; relever (lumière du ciel, lumière de bloc, apparu ou non) pour 100 000 tentatives | loi de H-EA4 | probabilité d'apparition à lumière L proportionnelle à la fraction des tirages de l'intervalle qui ne la dépassent pas (khi-deux) ; limite de lumière de bloc 0 confirmée (0 apparition à 1 ou plus) dans l'Overworld ; valeurs 7 et 15 (Nether) et 15 (End) vérifiées ; divergence de R7 tranchée | R7, R25 |
| EA5 | (client) Relever les paquets : positions des mobs apparus dans une même fenêtre de 2 ticks, taille de chaque paquet, écart à la première position ; 2 000 paquets par entrée de liste : zombie (`count` 4), enderman (uniforme 1 à 4), cheval (uniforme 2 à 6), loup, lapin, blaze, strider | taille et décalages | distribution des tailles égale à H-EA1 (ou à l'hypothèse qui la remplace) au khi-deux ; décalages : Kolmogorov-Smirnov contre la loi triangulaire de plus ou moins 5 ; nombre de tentatives par cycle égal à 3 | R3, R22 |
| EA6 | (client puis éloigné) Mobs `NoAI` non persistants, joueur à 20, 31, 33, 100, 129 blocs ; durées de vie sur 500 mobs ; variantes : nommé, passager de bateau, objet ramassé, `PersistenceRequired`, dans un wagonnet, dans une structure ; monstre à lumière 13 | disparitions | disparition immédiate à plus de 128 (64 pour les poissons) ; sinon, après 600 ticks (200 pour un monstre à lumière supérieure à 12), loi géométrique 1/800 (Kolmogorov-Smirnov) ; persistants jamais retirés | R39 à R42, R43 |
| EA7 | (client) Cabane de sorcière, forteresse, monument, avant-poste de pillards, cité ancienne, salles d'épreuve (`/place structure`) ; chunks forcés ; relever les types apparus dans et hors de la boîte (`full` et `piece`) pendant 100 000 ticks | types et fréquences | tous les types apparus dans la boîte appartiennent à la liste de la structure, avec fréquences = poids (khi-deux) ; aucune apparition pour les catégories vides ; hors boîte : liste du biome | R6 |
| EA8 | (client) Pour chaque étiquette de R10 : plateau de chaque bloc de l'étiquette et de blocs voisins non listés, lumière 15 pour les créatures; eau à y 50-63 et hors de cette plage pour les aquatiques; grotte de pierre pour les chauves-souris; relever les types apparus | table (type, bloc, lumière, ordonnée) | les créatures n'apparaissent que sur les blocs de leur étiquette; seuils de lumière 9 et 4 retrouvés; plages d'ordonnée 50-63 et <= 30 retrouvées; blocs `invalid_spawn_inside` et `prevent_mob_spawning_inside` rejetés | R24, R26 à R28 |
| EA9 | (client) `setblock <pos> minecraft:spawner` puis `data get block <pos>` (les clés NBT du spawner sont relevées ainsi, puis écrites par `data merge block`) ; mesurer délai, essais, rayon, voisins maximaux, portée du joueur; spawner d'épreuve de chaque famille (copier une configuration par `setblock` + `data merge block`) avec 1 à 3 clients, modes normal et ominous | clés NBT et statistiques | clés NBT listées; valeurs des paramètres relevées (R44) ; nombres simultanés et totaux = H-EA14 ou loi ajustée; défaut de `total_mobs` relevé ; `spawner_blocks_work` falsifie l'apparition | R44 à R49 |
| EA10 | (client) Surface de plat et marais, phases de lune imposées par `time set` multiples de 24000 (pleine à nouvelle), 100 000 tentatives par phase ; patrouille : `time set` autour de 120000 avec `gamerule spawn_patrols` | taux de slimes par phase; patrouille avant ou après 120000 | taux proportionnel à 0.5, 0.375, ..., 0.0 (test de proportion) ; patrouille impossible avant 120000 | R33, R34 |
| EA11 | Zombie à IA, villageois `NoAI` aux distances 5, 10, 20, 30, 35, 40, 50 (sans obstacle, puis avec une vitre, un mur, une lave), `Silent`, `PersistenceRequired` ; baby zombie; sous `tick freeze` (aucun déplacement) ; 200 exécutions par distance ; relever le tick du premier déplacement et la position à +200 ticks ; variantes client : joueur créatif, invisible, accroupi, avec appât pour un animal | seuil de détection; cible retenue ou non | seuil de détection (distance au-dessus de laquelle la proportion d'exécutions où le zombie approche tombe à 0) égal à `follow_range` lu par EM3 à 1 bloc près; la vitre ne bloque pas la vue mais le mur oui (ou l'inverse, consigné); abandon mesuré; aucun déplacement sous gel; reproductibilité des traces testée (R68) | R51 à R53, R61, R62, R67, R68, R50, R64 |
| EA12 | Labyrinthe de référence (9 x 9 cases de 3 blocs) avec cible `NoAI` à la sortie; zombie, araignée, noyé (dans l'eau), villageois, lapin, cochon (pas de cible : errance); portes en bois, en cuivre, trappes, échelles, toile, lave au sol, vide; 200 exécutions par cas; relever tick d'arrivée et nombre de cases distinctes visitées | atteignabilité et temps | taux d'arrivée par type, distribution du temps d'arrivée (Kolmogorov-Smirnov entre deux exécutions de l'oracle puis contre le moteur); absence d'entrée en lave ou dans le vide; portes : villageois ouvre, zombie brise en Difficile seulement; table des capacités par type | R54, R55, R63, R65 |
| EA13 | Zombie à IA contre villageois `NoAI` en Facile, Normal, Difficile; squelette contre villageois; creeper contre armure fixée (support d'armure) : relever les dégâts de chaque coup (`Health`), l'intervalle entre coups, le tick de mèche et de l'explosion; creeper chargé (`powered:1b`); foudre à moins de 4 blocs | intervalles et valeurs | dégâts d'un coup de zombie 2.5 / 3 / 4.5 selon la difficulté (`entity-damage-effects.md` R25); intervalle de coups constant et relevé (H-EA18) ; mèche 30 ticks et puissance 3 / 6 ; cadence et dispersion de tir relevées | R56 à R58, R66 |
| EA14 | Zombie, squelette, noyé, cheval zombie, phantom en plein air, sous toit, à l'eau, casque en fer; `time set` de 23400 à 23600 par pas de 10 avec `gamerule advance_time false`, lecture de `Fire` ; hiver et pluie | tick d'ignition | `Fire` positif à partir du temps 23460 pour les 10 types de `burn_in_daylight`, jamais pour les autres types (zombie piglin...) ; casque protège; eau éteint; pluie protège ou non (consigné) | R13, R59 |
| EA15 | Villageois adulte et bébé dans un village forcé (`place structure`), `time set` de 0 à 24000 par pas de 100 avec `advance_time false` puis dérouler 40 ticks à chaque pas; lecture de la clé de cerveau de `data get entity` (nom relevé par EM0); autres types à cerveau : piglin, hoglin, grenouille, gardien, axolotl, allay, chameau, renifleur, chèvre, breeze, tatou | activité observée et liste des types à cerveau | activité = piste de V-EA4 à 100 ticks près (tolérance de déplacement) ; liste des types dont `data get` contient un cerveau; capteurs et mémoires présents tabulés | R14, R69 à R72 |
| EA16 | Vache, mouton, poule, cochon, cheval, lapin, villageois `NoAI` retirés de `NoAI` après `damage` : `damage @e[tag=probe] 1 minecraft:<type>` pour chacun des 29 types de `panic_causes` et pour 10 types hors de l'étiquette; observation du déplacement à +40 ticks | fuite ou non | fuite si et seulement si le type est dans `panic_causes` sur 39 types (H-EA20) ; durée de fuite mesurée; types environnementaux séparés | R60, R73 |
| EA17 | Génération (EA1) dans les biomes qui commandent les variantes (neuf variantes de loup, grenouille, cochon, vache, poulet, chat dans un village / marais / par lune) ; relever `variant` | variante par biome | chaque variante observée est permise par `spawn_conditions` ; la priorité tranche les cas à plusieurs variantes permises ; fréquences par variante relevées | R12 |

## Inventaire des scénarios d'IA par famille de mobs

Contrat de R50 et R74 : un scénario est défini par famille ; les types qui n'apparaissent pas dans ce tableau n'ont aucune fidélité d'IA revendiquée. Grammaire de `criterion` : voir R74 ; seuils : R75. Les tailles `n_min` sont calculées par le `test-engineer` au moment d'écrire le scénario.

| Id | Famille (types) | Montage | Grandeur observée | Test | Sonde |
|---|---|---|---|---|---|
| AI-01 | zombie, husk, noyé, villageois zombie | zombie à IA, villageois `NoAI` à 5, 10, 20, 30, 35, 40, 50 blocs | proportion d'exécutions où la distance diminue en 200 ticks ; tick du premier déplacement | proportion (seuil de détection) ; ks (premier déplacement) | EA11 |
| AI-02 | zombie (bébé) | idem avec `IsBaby:1b` | vitesse moyenne d'approche en blocs par tick | ks | EA11 |
| AI-03 | zombie, villageois | porte en bois entre les deux, Facile et Difficile | proportion de portes brisées en 600 ticks | proportion | EA12 |
| AI-04 | araignée, araignée bleue | mur de 3 blocs entre l'araignée et la cible | proportion d'arrivées ; tick d'arrivée | proportion ; ks | EA12 |
| AI-05 | squelette, stray, bogged | tireur contre villageois `NoAI` à 5, 10, 15 blocs | intervalle entre tirs ; proportion de tirs qui touchent | ks ; proportion | EA13 |
| AI-06 | creeper | creeper contre support d'armure à 2, 3, 4, 7, 8 blocs | tick de la mèche et de l'explosion ; distance d'annulation | exact (mèche) ; proportion | EA13 |
| AI-07 | vache, cochon, mouton, poule, cheval, lapin | `damage` de chacun des 29 types de `panic_causes` et de 10 types hors liste | proportion de fuites ; distance parcourue en 40 ticks | proportion ; ks | EA16 |
| AI-08 | vache, cochon, mouton, poule | errance sans cible dans un enclos de 20 x 20 | distribution de la distance au point de départ après 600 ticks | ks | EA12 |
| AI-09 | villageois adulte et bébé | `time set` de 0 à 24000 par pas de 100 | activité observée à chaque pas | exact (piste D de R14, tolérance de 100 ticks) | EA15 |
| AI-10 | zombie, squelette, noyé, cheval zombie | plein air, sous toit, casque, eau, à 23459 et 23460 | tick d'allumage de `Fire` | exact (R13) | EA14 |
| AI-11 | morue, saumon, calmar, dauphin | bassin de 15 x 15 x 5 | distribution de la profondeur et de la distance au bord | ks | EA12 |
| AI-12 | zombie, squelette, araignée, villageois | labyrinthe de 9 x 9 cases avec cible | taux d'arrivée ; tick d'arrivée | proportion ; ks | EA12 |
| AI-13 | piglin, hoglin, zoglin | village de test (Nether forcé) | clés de cerveau et activité à t donné | exact | EA15 |

## Couverture des règles S par les sondes

R17, R18, R30 : EA0 ; R8, R36, R37, R77 : EA1 ; R23, R29, R31 : EA2 ; R5, R19 à R21 : EA3 ; R7, R25 : EA4 ; R3, R22 : EA5 ; R39 à R42 : EA6 ; R6 : EA7 ; R24, R26 à R28 : EA8 ; R44 à R49 : EA9 ; R33, R34 : EA10 ; R51 à R53, R61, R62, R67, R68 : EA11 ; R54, R55, R63, R65 : EA12 ; R56 à R58, R66 : EA13 ; R13, R59 : EA14 ; R14, R69 à R72 : EA15 ; R60, R73 : EA16 ; R12 : EA17 ; R43 : EM13 (spec du modèle) ; R64 : EP9 (`entity-player-vectors.md`) ; R50, R57 (valeurs de base) : EA11 et EM3. R38, R74 à R76 et R78 sont des décisions de conception (S-arch). R1, R2, R4, R9 à R11, R15, R16, R32, R35 sont D ou C. Aucune autre règle S n'est sans sonde.

## Récapitulatif

- Sondes EA0 à EA17 (18). Prérequis : harnais commun, lecteur `.mca` indépendant, client protocole minimal (jalon M8) pour EA0 à EA10 et les variantes de EA11 et EA15, EULA acceptée.
- Seule EA1 compare bit à bit (si H-EA12 tient) ; toutes les autres sont des mesures de distributions ou de propriétés.

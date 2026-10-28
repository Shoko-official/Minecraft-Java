# Vecteurs et sondes : joueur, faim, expérience, véhicules

Complète `entity-player.md`. L'outil `ent_ref.py`, le harnais commun et les empreintes sont dans `entity-model-physics-vectors.md`. Les vecteurs V-EP sont des calculs sur les données extraites (D, C) ou des **prédictions** d'hypothèses (C sur S). Les sondes EP0 à EP12 sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée). Les sondes EP0 à EP8 et EP12 exigent un client de protocole minimal (jalon M8) ; EP7 exige à terme le client réel (jalon M9) pour la cinétique ; EP9 à EP11 ont une partie sans client.

## Commandes de l'outil utilisées

`python -I ent_ref.py "$REF" <sous-commande>` pour `food`, `armor`, `damage`, `vehicles`, `player`, `calc`, `cmds`, `packets`, `registry`. Les empreintes figurent dans le tableau de `entity-model-physics-vectors.md`.

## V-EP : vecteurs

**V-EP1 (R33, R35, R37) : aliments (D).** Sortie de `food` (extraits) :

```
items with food 44
keys [('can_always_eat', 5), ('nutrition', 44), ('saturation', 44)]
nutrition [(1, 7), (2, 12), (3, 4), (4, 5), (5, 4), (6, 8), (8, 3), (10, 1)]
bread {'nutrition': 5, 'saturation': 6.0}
cooked_beef {'nutrition': 8, 'saturation': 12.8}
golden_apple {'can_always_eat': True, 'nutrition': 4, 'saturation': 9.6}
rotten_flesh {'nutrition': 4, 'saturation': 0.8}, effect hunger duration 600 probability 0.8
```

`food` : 43 objets portent un `consumable` (38 aux valeurs par défaut, `dried_kelp` 0.8 s, `honey_bottle` 2.0 s, trois boissons : `milk_bucket`, `ominous_bottle`, `potion`) ; 4 seaux de poisson portent `food` sans `consumable` ; 11 objets ont des effets de consommation. Armures de joueur (`armor`) : diamant 3/8/6/3 (robustesse 2), netherite 3/8/6/3 (robustesse 3, résistance au recul 0.1).

**V-EP2 (R14 à R16) : vitesses et saut (C sur S).** Sortie de `player` :

```
per tick walk 0.21585 sprint 0.28060 sneak 0.06500 sprint_jump 0.35635
sprint / walk 1.3000 ; sneak / walk 0.3011 (the attribute sneaking_speed has default 0.3; 0.3 x 4.317 = 1.2951, a value the pages do not give)
minecart cap 8 b/s = 0.400 b/tick ; boat water 8 b/s = 0.400 ; boat ice 40 b/s = 2.000 ; blue ice 72.72 b/s = 3.636
order A apex 1.252203
order B apex 0.832203
order A back to the start height after 12 ticks
```

**V-EP3 (R42, R44, R48) : expérience (C sur S).** `calc` : `total(L+1)-total(L) == next(L)` sans exception pour L de 0 à 299 ; totaux aux niveaux 1, 16, 17, 30, 31, 32, 100 : 7, 352, 394, 1395, 1507, 1628, 30970. `player` : découpage de 100 points : 73, 17, 7, 3 ; de 1395 : 1237, 149, 7, 1, 1 ; de 5000 : 2477, 2477, 37, 7, 1, 1. Mort : niveau 5 : 35 points, niveau 14 : 98, niveau 15 : 100. Cuisson : 116 recettes avec champ `experience` (fusion 73, haut fourneau 25, feu de camp 9, fumoir 9) de valeurs 0.1 (47 recettes), 0.35 (25), 0.7 (16), 1.0 (17), 0.15, 0.2, 0.3, 2.0.

**V-EP4 (R50, R51, R60) : véhicules (D/C).** Sortie de `vehicles` :

```
minecart types ['chest_minecart', 'command_block_minecart', 'furnace_minecart', 'hopper_minecart', 'minecart', 'spawner_minecart', 'tnt_minecart']
boat and raft types 22 of which chest 11
can_equip_saddle 11 ; can_float_while_ridden 6 ; dismounts_underwater 13 ; cannot_be_pushed_onto_boats 14
non_controlling_rider ['magma_cube', 'slime', 'sulfur_cube']
rail {'shape': 10, 'waterlogged': 2} states 20 ; powered_rail {'powered': 2, 'shape': 6, 'waterlogged': 2} states 24
max_minecart_speed argument brigadier:integer {'max': 1000, 'min': 1}
```

**V-EP5 (R27) : comptabilité de la faim (C sur S).** Sortie de `player`, section faim : `minus meters until food 19: 240` et `reset meters until food 19: 240` (les deux candidats coïncident sur des pas de 0.1) ; `minus single event of 6.0 exhaustion leaves 2.0` et `reset single event of 6.0 exhaustion leaves 0.0` (ils divergent sur un coût de 6.0). Exemples de repas : nourriture 10, saturation 2.0, pain -> 15 et 8.0 ; nourriture 18, saturation 5.0, boeuf cuit -> 20 et 17.8 ; nourriture 20, saturation 20.0, pomme -> 20 et 20.

## Sondes (EP)

Cadre : client de protocole minimal sauf mention. Un « joueur d'essai » est le client placé à (0.5, -60, 0.5) en monde plat, `difficulty normal`, `gamerule` du harnais commun. Les actions sont des scripts d'entrées par tick (R72).

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Critère de succès | Règles levées |
|---|---|---|---|---|
| EP0 | (client) `attribute @s minecraft:<a> base get` pour les 40 attributs ; `data get entity @s` complet (inventaire des clés, y compris expérience) ; `data merge entity @s {Health:1f}` (refus attendu) ; `summon minecraft:player` ; commandes de R11 sur le joueur | base du joueur par attribut ; clés du joueur | table (attribut, base) complète pour le joueur et comparaison au générique de V-EM1 ; liste des clés de l'expérience et de la faim ; refus de `data merge` sur un joueur confirmé ou infirmé (H-EP9) ; valeurs initiales de R2 relevées | R1, R2, R11 |
| EP1 | (client) `gamemode survival / creative / adventure / spectator` ; dans chaque mode : `damage @s 4 minecraft:generic`, `damage @s 4 minecraft:generic_kill`, vol (entrées), traversée d'un bloc, faim sur 2000 ticks ; relever `player_abilities` | capacités par mode | pour chaque mode : Health perdue ou non pour les deux types, vol autorisé, collision ; contenu de `player_abilities` relevé (R5) ; vitesses de vol 11.0 et 22.0 mesurées (R21) ; spectateur : `teleport_to_entity` | R4, R5, R21, R25, R39 |
| EP2 | (client) Survie ; script : courir 100 mètres, sauter 20 fois, sauter en courant 20 fois, nager 50 mètres, casser 50 blocs, attaquer 20 fois une entité `NoAI` ; relever `foodExhaustionLevel`, `foodSaturationLevel`, `foodLevel` à chaque tick ; (b) `gamerule natural_health_regeneration true` avec nourriture 18 et santé 10 : relever l'épuisement avant et après chaque point de vie rendu | épuisement par action et comptabilité | montants par action égaux à R28 (ou table mesurée) ; marche : épuisement mesuré (H-EP6) ; retrait de 4.0 ou remise à 0 tranché (H-EP5a ou b, critère : après un coût de 6.0, résidu 2.0 ou 0.0) ; course interdite à nourriture 6 et reprise à plus de 7 (R22) | R22, R26, R27, R28 |
| EP3 | (client) Survie : régénération naturelle de nourriture 20 à 0 par paliers (20, 19, 18, 17, 10) avec santé 10 et saturation 0, 2, 5 ; famine en Facile, Normal, Difficile, Pacifique ; relever `Health`, `foodTickTimer`, à chaque tick sur 2000 ticks | intervalles de régénération et de famine | intervalles de 80 ticks (nourriture >= 18) et de 10 ticks (nourriture 20 avec saturation) relevés ou corrigés; coût de saturation 1.5 mesuré; famine : 1 point toutes les 80 ticks avec planchers 10, 1, aucun; Pacifique : nourriture constante; absorption et plancher (H-EP15) | R30, R31, R32, R41 |
| EP4 | (client) Pour chacun des 51 types : `damage @s 1 minecraft:<type>` (avec `by` si nécessaire) en survie, relever `foodExhaustionLevel` avant et après ; variantes avec et sans armure | épuisement par type | épuisement gagné = champ `exhaustion` du type pour 51 sur 51 (0.1 pour 32, 0.0 pour 19) ; si un type ne suit pas, la différence est consignée (H-EP12) ; casse d'un bloc : 0.005 | R29, R13 de entity-damage-effects.md |
| EP5 | (client) Chaque aliment de V-EP1 (44) avec nourriture et saturation de départ variées ; repas à nourriture 20 avec et sans `can_always_eat` ; délai entre début et fin de l'usage ; effets de consommation | nourriture et saturation après repas | résultat = règle H-EP13 pour 44 x 5 états de départ (plafond de saturation par la nourriture compris) ; refus de manger à 20 pour les 39 aliments sans `can_always_eat` ; délai par défaut mesuré, 0.8 s et 2.0 s retrouvés ; effets de la pomme dorée et de la chair putréfiée (fréquence 0.8 à 5 écarts-types sur 2 000 repas) | R33, R34, R35, R37 |
| EP6 | (client) `effect give @s minecraft:hunger 30 <n>` pour n de 0 à 3 ; `effect give @s minecraft:saturation 5 <n>` ; relever l'épuisement par tick et les restitutions | débit d'épuisement par niveau; restitution par tick | débit par niveau relevé et tabulé ; restitution de la nourriture et de la saturation par tick et par niveau relevée et comparée à R36 | R36 |
| EP7 | (client, puis client réel) Entrées scriptées : marche, course, accroupi, saut, saut en course, nage de surface et immergée, échelle, bord de bloc accroupi, vol créatif, sous `tick freeze` ; relever `Pos` et `Motion` à chaque tick, `attributes` pendant la course ; puis un client qui annonce des positions trop rapides : 0.3, 0.35, 0.5, 1.0, 2.0 b/tick | trajectoires et seuils | vitesses moyennes égales à R14 à 1e-3 près (accroupi : 1.2951 ou 1.3 b/s tranché à 1e-4 près); rapport course / marche = 1.3 (attribut observé, H-EP4) ; apogée du saut 1.252203 (ordre A) ou autre, mesuré à 1e-6 près ; impulsion du saut en course; seuil de rejet par le serveur (R13) en b/tick par pas de 0.01; boîte de pose mesurée; les joueurs avancent sous gel (R23) | R3, R12 à R20, R23, R24 |
| EP8 | (client) `experience set @s 0 points` puis `experience add @s <n> points/levels` pour n variés ; lire le niveau et la fraction par `experience query` et `data get entity @s` ; `summon minecraft:experience_orb ~ ~ ~ {Value:<v>s}` pour v dans les 11 bases et 100 valeurs de test ; orbes à 7, 7.25 et 8 blocs ; 20 orbes simultanées ; fusion ; temps de vie 6000 ticks ; fourneau et recettes de cuisson ; `Score` | niveaux, orbes, attraction | formules R42 retrouvées pour N de 0 à 100 ; découpage glouton R44 retrouvé (exact) pour 200 totaux ; rayon 7.25 mesuré à 1e-2 près; cadence de ramassage; disparition à 6000 ticks; comportement de `add levels` et `set levels` (H-EP16); expérience de cuisson et fraction aléatoire (H-EP21) | R42 à R47, R49 |
| EP9 | Cheval, âne, mule, cochon, strider, chameau (avec et sans selle ; `NoAI`) lâchés avec `Motion` ; (client) montés par le joueur d'essai ; `/ride <cible> mount <véhicule>` ; passager de passager ; types de `non_controlling_rider` ; sous l'eau pour les 13 types de `dismounts_underwater` | trace passive ; contrôle | trace passive du cheval égale à celle de la spec du modèle (EM5) ; position du passager = véhicule + décalage mesuré par type ; contrôle seulement avec selle ; `non_controlling_rider` ne contrôlent pas; démontage sous l'eau pour les 13 types; `jump_strength` des montures lu | R10, R52, R64, R65, R66, R68 |
| EP10 | Bateau (`oak_boat`) avec `Motion` sur eau, glace, glace compacte, glace bleue, dans une eau qui coule, au-dessus d'une colonne de bulles, lâché en l'air : trace sur 200 ticks ; (client) pagayer : accélération, virage; `damage @e[type=oak_boat] <n>` puis compteur; passagers : 2 ; mobs de `cannot_be_pushed_onto_boats` | traces et dégâts | vitesses terminales 0.4 (eau), 2.0 (glace), 3.636 (glace bleue) retrouvées au plus près; accélérations 0.04 et 0.005; lacet 10 et 50; chute libre du bateau (R55) mesurée; flottaison et coulée conformes; compteur de dégâts +10 par point, -1 par tick, destruction à plus de 40 (ou valeurs mesurées) ; H-EP18 tranchée | R53, R55 à R58, R67, R68, R70, R69 |
| EP11 | Wagonnet sur rails droits, courbes, en pente et alimentés (rails de puissance) avec `Motion` initial 0.1, 0.4, 1.0 ; `gamerule max_minecart_speed` à 1, 8, 100, 1000 ; hors rails ; collision avec un mob (support d'armure, vache, chauve-souris, golem de fer) ; trace sur 200 ticks ; descente d'un passager (client) | traces | plafond par tick = règle / 20 (H-EP19) à 1e-9 près; demi-vitesse hors rails; lois de pente et d'alimentation tabulées; boîte de ramassage élargie de 0.2; liste des types non ramassables; mouvement le long du rail égal à celui de `move_minecart_along_track` | R54, R59 à R63, R68, R70 |
| EP12 | (client) Mort du joueur avec et sans `keep_inventory`, avec `immediate_respawn` ; `spawnpoint`, lit, `respawn_radius` 0, 10, 50 ; niveaux 0 à 100 avant la mort pour l'orbe lâchée ; sommeil : `players_sleeping_percentage` 0, 50, 100 avec 1 à 4 joueurs | état à la réapparition | objets et expérience conformes à R8 et R48 ; point de réapparition conforme à H-EP8 ; état de faim à la réapparition (R40) ; nuit passée selon la règle | R7, R8, R9, R40, R48 |

## Couverture des règles S par les sondes

R1, R2, R11 : EP0 ; R4, R5, R21, R25, R39 : EP1 ; R22, R26 à R28 : EP2 ; R30 à R32, R41 : EP3 ; R29 : EP4 ; R33 à R35, R37 : EP5 ; R36 : EP6 ; R3, R12 à R20, R23, R24 : EP7 ; R42 à R47, R49 : EP8 ; R10, R52, R64 à R66 : EP9 ; R53, R55 à R58, R67 : EP10 ; R54, R59 à R63 : EP11 ; R7 à R9, R40, R48 : EP12 ; R68, R70 : EP9 à EP11 ; R69 : EM1 (spec du modèle). R72 à R74 sont des décisions de conception. R6, R38, R50, R51, R71 sont D ou C. Aucune autre règle S n'est sans sonde.

## Récapitulatif

- Sondes EP0 à EP12 (13). Prérequis : harnais commun, client de protocole (jalon M8), client réel pour la cinétique (jalon M9), EULA acceptée.
- Les sondes sans client : la partie traces de EP9 à EP11.

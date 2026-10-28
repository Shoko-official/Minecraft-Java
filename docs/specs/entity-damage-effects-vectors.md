# Vecteurs et sondes : santé, dégâts, effets, combat

Complète `entity-damage-effects.md`. L'outil `ent_ref.py`, le harnais commun (serveur, réglages, relevé) et les empreintes des sous-commandes sont dans `entity-model-physics-vectors.md` (annexe et section Harnais commun) ; ce fichier ne les répète pas. Les vecteurs V-ED sont des calculs sur les données extraites (D, C) ou des **prédictions** d'hypothèses (C sur S). Les sondes ED0 à ED15 sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée). Les sondes marquées « client » exigent un client de protocole minimal (jalon M8) car la recharge d'attaque, le bouclier et l'expérience supposent un joueur.

## Commandes de l'outil utilisées

`python -I ent_ref.py "$REF" <sous-commande>` pour `damage`, `calc`, `armor`, `weapons`, `food`, `registry`, `loot`, `cmds` ; les empreintes figurent dans le tableau de `entity-model-physics-vectors.md`. Les sorties citées ci-dessous sont des extraits.

## V-ED : vecteurs

**V-ED1 (R11 à R17) : inventaire des types de dégâts (D).** Sortie de `damage` :

```
count 51
scaling [('always', 4), ('when_caused_by_living_non_player', 47)]
effects [('(absent)', 39), ('burning', 8), ('drowning', 1), ('freezing', 1), ('poking', 1), ('thorns', 1)]
exhaustion [0.0 x19, 0.1 x32]
tags 36
bypasses_armor 19 ; bypasses_shield 30 ; no_knockback 30 ; is_fire 8 ; bypasses_cooldown 0
exhaustion 0.0 bypasses_armor True  : 19 types ; exhaustion 0.1 bypasses_armor False : 32 types
```

Extraits d'appartenance : `fall` appartient à `bypasses_armor`, `bypasses_shield`, `is_fall`, `no_knockback`, `sulfur_cube_with_block_immune_to` ; `lava` à `bypasses_shield`, `is_fire`, `no_knockback`, `panic_causes`, `panic_environmental_causes` (pas `bypasses_armor`) ; `mob_attack` à `panic_causes` et `sulfur_cube_with_block_immune_to` seulement.

**V-ED2 (R28) : armure (C sur S).** `calc`, section armure :

```
damage 10 armor 0  toughness 0 result 10.000000
damage 10 armor 20 toughness 0 result 4.000000
damage 10 armor 20 toughness 8 result 3.000000
damage 3  armor 20 toughness 0 result 0.780000
damage 3  armor 20 toughness 8 result 0.690000
damage 40 armor 20 toughness 8 result 24.000000
damage 4  armor 8  toughness 2 result 2.976000
damage 1  armor 30 toughness 20 result 0.200000
diamond set armor 20 toughness 8.0 ; netherite set toughness 12.0 kb resistance 0.4
```

**V-ED3 (R21, R35) : ordres de réduction (C sur S).** Dégât 10, armure 10, robustesse 0, résistance I, protection 8 points : O1 4.352000 ; O2 4.134400 ; O3 4.352000 ; O4 4.003840. Les valeurs diffèrent, donc ED3 sépare O2 et O4 de O1 et O3, mais pas O1 de O3 qui ne diffèrent que par la commutation des deux derniers facteurs (équivalents numériquement à l'arrondi près). Croisements de R21 (`calc`) : dégât 3, armure 20, robustesse 0, Difficile : difficulté puis armure 1.3050, armure puis difficulté 1.1700 ; fenêtre, coups bruts 10 puis 14 avec armure 20 : premier coup 4.0000, excédent brut réduit 1.1200, différence des montants réduits 2.7200.

**V-ED4 (R25, R36, R57, R58, R59, R66, R67) : difficulté, chute, périodiques, recharge (C sur S).**

```
damage 3.0 easy 2.5 normal 3.0 hard 4.5
fall distance 4.0 damage 1 ; 5.99 damage 2 ; 6.0 damage 3 ; 25.5 damage 22 (safe 3, mult 1)
regeneration ticks per hp levels I..VII [50, 25, 12, 6, 3, 1, 1] ; poison [25, 12, 6, 3, 1, 1, 1]
cooldown attack_speed 1.6 : t 0 0.201280 ; t 5 0.354880 ; t 10 0.764480 ; t 11 0.877120 ; t 12 1.000000
charge at t 10 = 0.8400 ; at t 11 = 0.9200 (seuil 0.848)
experience formulas: total(L+1)-total(L) == next(L) for L in 0..299 (aucune exception)
```

**V-ED5 (R81, R83) : explosions (C sur S, hypothèse H-EXP1).** Dégât à exposition 1 : puissance 3 : 43.0 au centre, 33.0833 à 1, 24.3333 à 2, 16.75 à 3, 1.0 à 6 ; puissance 4 : 57.0, 46.9375, 37.75, 22.0 (à 4), 1.0 à 8 ; puissance 6 : 85.0 au centre, 32.5 à 6. Recul puissance 4 : 1.0 (centre), 0.75 (2), 0.5 (4), 0.0125 (7.9).

**V-ED6 (R63) : effets de consommation (D).** `food` : pomme dorée `regeneration` amplificateur 1 durée 100 plus `absorption` durée 2400 ; chair putréfiée `hunger` durée 600 probabilité 0.8 ; 44 objets ont le composant `food` (valeurs de `nutrition` de 1 à 10).

## Sondes (ED)

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Critère de succès | Règles levées |
|---|---|---|---|---|
| ED0 | Zombie `NoAI` : `damage @e[tag=probe] 5 minecraft:generic`, lecture de `Health`, `HurtTime`, `invulnerable_time` ; montant 0 ; `Invulnerable:1b` puis `damage ... generic` et `damage ... generic_kill` ; `attribute ... max_health base set 10` avec `Health` 20 ; `data merge entity ... {Health:0.5f}` ; `kill` ; texte de retour de la commande | `Health` 15 ; montant 0 sans changement ; invulnérable inchangé sauf `generic_kill` ; `Health` ramenée à 10 ; mort | les six valeurs relues égales aux attendues de R1, R6, R7, R10, R24, R92 ; sinon valeur observée consignée | R1, R6, R7, R10, R24, R92 |
| ED1 | Zombie : `damage` D1 puis `tick step t` puis `damage` D2 ; D1 dans {3, 5, 10}, D2 dans {D1 - 1, D1, D1 + 2} ; t de 0 à 25 ; relever `Health`, `HurtTime`, `invulnerable_time` à chaque tick ; variantes de type : `cactus`, `hot_floor`, `in_wall`, `lava`, `generic_kill`, `out_of_world` | frontière t* de la fenêtre ; excédent seul appliqué | t* identique pour toutes les paires ; règle de l'excédent (différence) confirmée sur 30 paires ; suites de `HurtTime` relevées ; H-ED9a ou H-ED9b tranchée ; types de `bypasses_invulnerability` hors fenêtre ; **croisement fenêtre x armure** : zombie avec `attribute @e[tag=probe] minecraft:armor base set 20`, `damage` 10 puis `damage` 14 de type `mob_attack` à t = 1 et t = 5 ; la `Health` perdue vaut 4.0 plus 1.12 si la fenêtre retranche l'excédent brut avant l'armure, 4.0 plus 2.72 si elle retranche la différence des montants réduits (V-ED3), 4.0 seul si le second coup est ignoré ; critère : une de ces trois valeurs pour 20 couples (D1, D2), la même hypothèse pour tous | R3, R21, R22, R23 |
| ED2 | Zombie `NoAI` ; `attribute @e[tag=probe] minecraft:armor base set <AP>` et `armor_toughness` ; AP dans {0, 1, 5, 10, 15, 20, 25, 30, 40}, T dans {0, 2, 8, 12, 20, 30}, D dans {1, 2, 3, 4, 5, 6, 8, 10, 15, 20, 30, 50, 100} par `damage @e[tag=probe] <D> minecraft:mob_attack` ; usure : `item replace entity @e[tag=probe] armor.chest with minecraft:diamond_chestplate` puis relecture du composant de dégâts de la pièce | `Health` après = 20 - D x facteur (V-ED2) ; usure `max(1, floor(D / 4))` par pièce | `Health` égale à la prédiction, comparée comme float, sur 100 pour cent des combinaisons ; arrondi consigné ; usure égale pour D de 1 à 40 ; casque seul pour `falling_block` (H-ED11) | R28, R29 |
| ED3 | Grille : résistance niveau 0 à 5 (`effect give ... minecraft:resistance 1000 <n-1>`), armure diamant ou fer complète avec protection I à IV sur les quatre pièces (`item replace ... [minecraft:enchantments={...}]`), absorption 0, 4, 8, D dans {1, 3, 5, 10, 20, 50} ; types `mob_attack` (armure et enchantements actifs), `magic` (contourne l'armure), `explosion` (protection contre les explosions), `generic_kill` | valeurs de V-ED3 | un seul des quatre ordres (arrondis compris) égal à l'oracle sur 500 combinaisons ; consommation de l'absorption avant ou après les réductions tranchée (H-ED14) ; niveau 5 : immunité sauf `bypasses_resistance` ; **croisement difficulté x armure** : `difficulty easy / normal / hard`, `attribute armor base set 20`, `damage @e[tag=victim] 3 minecraft:mob_attack by @e[tag=attacker]` avec un attaquant zombie `NoAI`, puis D dans {1, 2, 4, 6, 8, 20} ; en Difficile la `Health` perdue vaut 1.305 (difficulté puis armure) ou 1.17 (armure puis difficulté) pour D = 3 ; critère : la même hypothèse pour 18 couples (difficulté, D), égalité exacte au float près | R2, R21, R30, R31, R33, R35 |
| ED4 | Pour chacun des 51 types : `damage @e[tag=victim] 4 minecraft:<type>` et la forme `by @e[tag=attacker]` ; victime zombie `NoAI` nue puis en armure de diamant ; relever `Health`, `Motion`, `HurtTime` | contournement et recul conformes aux étiquettes | pour 51 types : contournement de l'armure = appartenance à `bypasses_armor` ; recul absent si `no_knockback` ; liste des écarts éventuels (H-ED7) ; types `bypasses_effects`, `bypasses_enchantments` testés avec résistance et protection | R17, R19, R48, R13, R21 |
| ED5 | `difficulty easy / normal / hard / peaceful` ; zombie avec IA adjacent à un villageois cible `NoAI` : premier coup ; zombie `NoAI` attaquant d'un `damage ... minecraft:mob_attack by` ; `damage ... minecraft:explosion` ; mob contre mob ; relever `Health` | 2.5 / 3 / 4.5 pour une attaque de zombie ; `explosion` modulé par la difficulté | coup de zombie lu = V-ED4 pour les trois difficultés ; `scaling` `always` et l'autre valeur distingués (H-ED4) ; Pacifique supprime le zombie | R9, R12, R25, R26, R75 |
| ED6 | Cochon `NoAI` (ou animal vivant sans réaction, si H-EM26 est fausse) lâché de 2.9 à 30 blocs par pas de 0.1 ; `attribute safe_fall_distance base set 0 / 3 / 6` et `fall_damage_multiplier` 0, 0.5, 1, 2 ; atterrissage sur eau, toile, neige poudreuse, miel, botte de foin, lit, bloc de slime, échelle, baie, échafaudage ; `gamerule fall_damage false` ; effets Jump Boost 1 à 3, Slow Falling, Resistance ; types de `fall_damage_immune` ; cheval, âne, chameau, chèvre, grenouille, renard | `Health` perdue | égalité exacte avec `floor((d - s) x m)` sur 300 cas; table des réductions par bloc et par effet; seuils par type (R38) | R36, R37, R38 |
| ED7 | Vache `NoAI` (ne respire pas sous l'eau) : bloc de feu, feu d'âme, lave, cactus, bloc de magma, feu de camp et feu de camp d'âme, buisson de baies (avec `Motion`), `setblock` d'un bloc plein sur la tête, descente sous la limite du vide dans l'Overworld (y = -129 ; limite -128), dans le Nether et l'End (y = -65 et -64 ; `execute in minecraft:the_nether run tp`, hypothèse H-ED22 : `min_y - 64`), neige poudreuse (`TicksFrozen`), `summon minecraft:lightning_bolt`, enclume et stalactite qui tombent, eau profonde (`Air`) ; `gamerule fire_damage / drowning_damage / freeze_damage false` ; 200 ticks, `tick step 1`, relevé de `Health`, `Fire`, `Air`, `TicksFrozen` | montants et intervalles de R39 à R46 | pour chaque source : montant et intervalle égaux à ceux de la règle, sinon table de calibrage; constantes de l'air (maximum 300 pour une vache selon la page, consommation, regain, intervalle et montant du dégât) relevées ; seuil du vide mesuré dans les trois dimensions (H-ED22) ; H-ED15 et H-ED16 tranchées ; règles de jeu respectées | R8, R39 à R46 |
| ED8 | Cochon `NoAI`, `Health` à 10, `max_health` 100 : `effect give ... minecraft:regeneration 30 <n>` pour n de 0 à 6, de même poison et wither ; cheval avec `Health` 5 pendant 400 ticks ; zombie, squelette, araignée, araignée bleue, dragon en parallèle | suites de `Health` | intervalles égaux à la table de R57 à R59 (50, 25, 12, 6, 3, 1 ; 25, 12, 6, 3, 1 ; 40, 20, ...) ; tick du premier effet relevé ; plancher du poison à 1 ; liste des immunités (zombies exclus par l'étiquette ; araignées ?) ; taux de régénération naturelle du cheval | R5, R57, R58, R59 |
| ED9 | 40 effets x amplificateur 0 à 3 : `effect give @e[tag=probe] minecraft:<e> 100 <n>`, lecture de `attributes` et de `active_effects` ; `instant_health` et `instant_damage` aux amplificateurs 0 à 5 avec `Health` 50 de 100 ; absorption (`AbsorptionAmount`), `health_boost` (`max_health`) | table (effet, amplificateur) -> modificateurs d'attributs et variation immédiate | 160 lignes complètes ; force +3 par niveau, absorption 4 par niveau confirmés ou corrigés ; effets sans attribut classés ; inversion `inverted_healing_and_harm` sur un zombie | R2, R54 à R56, R60, R61 |
| ED10 | `effect give` : durée par défaut (sans secondes), 1 s, 1000000 s, `infinite`, amplificateurs 0, 127, 128, 255 ; matrice de réapplication 3 forces x 3 durées (27 cas) avec relevé de `active_effects` et de `hidden_effect` ; décrément sur 40 ticks ; `effect clear` 3 formes ; sorcière `NoAI` contre `magic` ; dragon, wither, villageois contre `effect give` ; effets de consommation par client | durée en ticks = 20 x secondes ; remplacement et prolongation | durée par défaut mesurée (H-ED17) ; matrice de 27 cas égale à H-ED18 ou consignée ; octet signé (H-ED20) ; facteur de la sorcière mesuré (page : 85 pour cent) ; immunités confirmées | R51 à R53, R62 à R65 |
| ED11 | (client) Bot qui attaque un zombie `NoAI` (`max_health` 1000) : épée en diamant à t = 0 à 14 ticks après la dernière attaque ; poing ; hache ; Tranchant I à V ; Force I et II, Faiblesse ; critique (saut puis chute) avec les conditions de R68 basculées une par une ; attaque en courant ; balayage avec 1 à 3 voisins ; portée de 2.9 à 3.1 et de 4.4 à 4.6 ; lance ; masse | courbe de dégâts de V-ED4 | multiplicateur égal à R67 à 0 ulp sur 15 ticks ; seuil de 84.8 pour cent confirmé ; chacune des conditions du critique démontrée ; ratio de balayage tabulé ; portée mesurée à 1e-3 | R66 à R74, R34 |
| ED12 | `damage @e[tag=victim] 4 minecraft:mob_attack by @e[tag=attacker]` avec attaquant en 8 directions et 3 distances, victime au sol puis en l'air (`NoGravity:1b` puis `Motion:[0d,0.5d,0d]`) ; `knockback_resistance` -1, 0, 0.25, 0.5, 1 ; attaquant `NoAI` armé d'une épée avec Recul I et II ; `attack_knockback` 0 à 5 ; positions confondues ; types de `no_knockback` ; montant 0 ; relever `Motion` juste après la commande, sans pas | vecteur de recul | table de vecteurs ; direction = éloignement horizontal; composante verticale au sol et en l'air; `A = v x (1 - r)` à 1e-9 ; force de base, facteur d'enchantement et de l'attribut relevés; cas des positions confondues consigné | R18, R24, R69, R76 à R79 |
| ED13 | `gamerule tnt_explodes true` ; `summon minecraft:tnt ~ ~ ~ {fuse:0s}` (nom de la clé relevé par EM0) ; zombie `NoAI,NoGravity:1b` à `max_health` 200 à des distances de 0 à 2 x puissance par pas de 0.25 dans 6 directions ; écrans en bedrock pour l'exposition 0, 1/3, 1 ; creeper (`Fuse:0s,ignited:1b`) normal et `powered:1b` ; ghast ; armure et protection contre les explosions ; `explosion_knockback_resistance` 0 à 1 ; flèche à 6 vitesses | `Health`, `Motion` | `Health` égale à H-EXP1 (V-ED5) sur 200 points ou modèle ajusté ; minimum de 1 dans le rayon ; exposition : nombre de points de la grille ⌈2w + 1⌉ x ⌈2h + 1⌉ x ⌈2l + 1⌉ vérifié avec des écrans de blocs de 1/n de couverture pour n = 3 et 9 pour deux largeurs de boîte (zombie 0.6, vache 0.9), grille ajustée sinon (R82) ; seul le plus fort dégât d'explosion par tick appliqué (deux dynamites simultanées) ; recul égal à R83 ; puissances de R80 confirmées ; dégât de flèche tabulé (R49) | R49, R80 à R86 |
| ED14 | `damage` létal à un cochon (`NoAI`) ; relever `DeathTime` à chaque tick et le tick de disparition ; `gamerule mob_drops false` / `entity_drops false` ; mouton de chaque couleur ; creeper chargé tué par un squelette ; 20 000 morts avec `drop_chances` 0.085 pour l'équipement ; (client) mort sous coup de joueur à 0, 50, 99, 100, 101 ticks : orbes | suite de `DeathTime` ; objets au sol | `DeathTime` croît jusqu'à la disparition (H-ED3) ; butin conforme aux tables de `loot-tables.md` ; fréquence d'équipement lâché 0.085 à 5 écarts-types ; fenêtre de 100 ticks et montants de R88 confirmés | R4, R87 à R91 |
| ED15 | (client) Bouclier levé à t = 0 à 8 ticks avant un `damage ... by` ; angles de -180 à 180 par pas de 10 ; types dans et hors de `bypasses_shield` ; dégâts 1 à 20 ; hache contre bouclier | blocage ou non ; durabilité perdue | seuil du délai en ticks, demi-angle de l'arc et loi de perte de durabilité relevés (H-ED13) | R27, R32 |

## Couverture des règles S par les sondes

R1, R6, R7, R10, R24, R92 : ED0 ; R2, R21 (difficulté x armure), R30, R31, R33, R35 : ED3 ; R3, R21 (fenêtre x armure), R22, R23 : ED1 ; R4, R87 à R91 : ED14 ; R5, R57 à R59 : ED8 ; R8, R39 à R46 : ED7 ; R9, R12, R25, R26, R75 : ED5 ; R17, R19, R48 : ED4 ; R18, R69, R76 à R79 : ED12 ; R21 : ED1 à ED3 ; R28, R29 : ED2 ; R32 et R27 : ED15 ; R34 : ED11 et EM12 ; R36 à R38 : ED6 ; R49, R80 à R86 : ED13 ; R50 est D ; R51 à R53, R62 à R65 : ED10 ; R54 à R56, R60, R61 : ED9 ; R66 à R74 : ED11. R13 : EP4 (`entity-player-vectors.md`). Les autres règles sont D ou C.

## Récapitulatif

- Sondes ED0 à ED15 (16), dont ED11, ED14 (partie joueur) et ED15 qui exigent un client protocole.
- Prérequis : harnais commun, EULA acceptée ; H-EM26 (physique des `NoAI`) conditionne ED6.

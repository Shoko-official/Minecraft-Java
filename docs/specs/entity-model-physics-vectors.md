# Vecteurs et sondes : modèle d'entité et physique

Complète `entity-model-physics.md` et sert de base aux trois autres documents du jalon M7 (`entity-damage-effects.md`, `entity-ai-spawning.md`, `entity-player.md`), qui réutilisent l'outil `ent_ref.py` et le harnais commun ci-dessous. Les vecteurs V-EM sont des calculs sur les données extraites (preuve D ou C) ou des prédictions d'hypothèses (C sur S, étiquetées telles). Les sondes EM0 à EM14 sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée par le propriétaire).

Pour ne pas recopier les données de Mojang dans git, les tableaux volumineux ne sont pas reproduits : la commande qui les produit est donnée, avec l'empreinte SHA-256 (16 premiers caractères) de la sortie complète pour détecter une dérive des données extraites, et quelques lignes d'exemple.

## Outil et commandes

L'annexe en fin de fichier est le texte intégral de `ent_ref.py` (SHA-256 du texte de l'annexe, c'est-à-dire du contenu du bloc de code suivi d'un saut de ligne, fins de ligne LF : `85e2aff7cf679531f789d0d72dcd9b1ef9dfbda2fa7bbbad5e72285142bb59af`). `REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`. Toutes les commandes sont `python -I ent_ref.py REF <sous-commande>`. Sortie en UTF-8, fins de ligne LF ; l'empreinte est celle de la sortie complète. Pour recalculer : `python -I ent_ref.py "$REF" <sous-commande> | sha256sum | cut -c1-16`.

| Sous-commande | Sortie | Empreinte | Spec |
|---|---|---|---|
| `registry` | tailles et contiguïté de 10 registres, noms des attributs, effets, règles de jeu, activités et capteurs, absence de définitions d'entité dans les données, butins d'entité | d53ee41cd63f45f2 | model-physics R1, R2, R6 |
| `etags` | 49 étiquettes `entity_type` : effectifs directs et développés, membres de 12 étiquettes | 1c33d1e927b2b8fc | model-physics R3 |
| `btags` | étiquettes de blocs et de fluides lues par les entités | 72ca1ff0d726a34b | model-physics R51, R54, R61 à R68 ; ai-spawning |
| `damage` | 51 types de dégâts : champs, 36 étiquettes, appartenance de chaque type | bcb149ec1f55ed0d | damage-effects |
| `armor` | pièces d'armure par matériau et modificateurs d'objets | 1932e967c529c579 | model-physics R14, R15 |
| `weapons` | modificateurs d'attaque et composants d'arme | 741ee4755c09b4da | model-physics R16 ; damage-effects |
| `food` | composant de nourriture, 43 composants de consommation et leurs paramètres, effets de consommation | 484d19c710103e2c | player |
| `spawns` | attribut de spawn des 67 biomes, surcharges de structures, variantes | 3913363f3c1d5edf | ai-spawning |
| `dims` | champs de spawn des 4 types de dimension | 0354483082b563ae | ai-spawning |
| `loot` | butins d'entité | c068acce82f29d9a | damage-effects |
| `cmds` | chemins exécutables des commandes d'entité | 6489b5a6f956728d | toutes (harnais) |
| `packets` | paquets relatifs aux entités | 1c848cc4de2c20ad | model-physics R30 |
| `archetypes` | 12 archétypes de cube de soufre | 0e339561c9881731 | model-physics R18 |
| `spawners` | 28 configurations de spawner d'épreuve | 2d821b2264b29b7e | ai-spawning |
| `timelines` | pistes de jeu des chronologies (brûlure, lumière du ciel, slime, patrouille, horaires) | ceeb07640f832ae0 | ai-spawning R13, R14 |
| `vehicles` | wagonnets, bateaux, étiquettes de monture, états de rails, règle de vitesse | d8c25fc369b14a0f | player R50 à R60 |
| `player` | recettes à expérience, vitesses (accroupi 1.3), saut, orbes, mort, faim, repas | 401eb9a0b98e319e | player |
| `fall` | vitesses terminales de la table de mouvement et ordres de même point fixe, suites de chute sous cinq ordres | f7a0881462bd9570 | model-physics R39 à R41 |
| `calc` | armure, ordres de réduction, croisements difficulté x armure et fenêtre x armure, recharge, chute, intervalles, expérience, exhaustion | d5ed6e9b1a8a81c7 | damage-effects, player |

## V-EM : vecteurs

**V-EM1 (R8) : attributs, défaut et bornes (S, page "Attribute" du 2026-10-28, 40 lignes).** Format : identifiant (défaut ; minimum ; maximum). Le nombre de lignes (40) égale celui du registre et les noms sont ceux du registre (C). Les valeurs sont celles de la page (extraction automatique, S).

```
air_drag_modifier (1; 0; 2048)            armor (0; 0; 30)
armor_toughness (0; 0; 20)                attack_damage (2; 0; 2048)
attack_knockback (0; 0; 5)                attack_speed (4; 0; 1024)
below_name_distance (10; 0; 512)          block_break_speed (1; 0; 1024)
block_interaction_range (4.5; 0; 64)      bounciness (0; 0; 1)
burning_time (1; 0; 1024)                 camera_distance (4; 0; 32)
entity_interaction_range (3; 0; 64)       explosion_knockback_resistance (0; 0; 1)
fall_damage_multiplier (1; 0; 100)        flying_speed (0.4; 0; 1024)
follow_range (32; 0; 2048)                friction_modifier (1; 0; 2048)
gravity (0.08; -1; 1)                     jump_strength (0.42; 0; 32)
knockback_resistance (0; -2; 1)           luck (0; -1024; 1024)
max_absorption (0; 0; 2048)               max_health (20; 1; 1024)
mining_efficiency (0; 0; 1024)            movement_efficiency (0; 0; 1)
movement_speed (0.7; 0; 1024)             name_tag_distance (64; 0; 512)
oxygen_bonus (0; 0; 1024)                 safe_fall_distance (3; -1024; 1024)
scale (1; 0.0625; 16)                     sneaking_speed (0.3; 0; 1)
spawn_reinforcements (0; 0; 1)            step_height (0.6; 0; 10)
submerged_mining_speed (0.2; 0; 20)       sweeping_damage_ratio (0; 0; 1)
tempt_range (10; 0; 2048)                 water_movement_efficiency (0; 0; 1)
waypoint_receive_range (0; 0; 60000000)   waypoint_transmit_range (0; 0; 60000000)
```

Ces défauts sont ceux du type générique : le joueur et chaque mob redéfinissent des bases (par exemple la vitesse du joueur n'est pas 0.7), levées par EM3.

**V-EM2 (R21) : dimensions d'exemple (S, page "Hitbox").** Largeur x hauteur ; yeux : joueur 0.6 x 1.8 ; 1.62 | zombie 0.6 x 1.95 ; 1.74 | creeper 0.6 x 1.7 ; 1.445 | araignée 1.4 x 0.9 ; 0.65 | vache 0.9 x 1.4 ; 1.3 | objet 0.25 x 0.25 ; 0.2125 | bloc qui tombe 0.98 x 0.98 ; 0.833 | support d'armure 0.5 x 1.975 ; 1.7775 | slime moyen 1.0404 x 1.04. Ces entrées servent d'étalons de départ : EM2 doit les retrouver ou les corriger.

**V-EM3 (R39 à R41) : suites de chute (C sur S).** `python -I ent_ref.py "$REF" fall` (première section : vitesse terminale documentée de chaque ligne de la table de mouvement et ordres de même point fixe ; puis suites). Un vivant (g 0.08, d 0.98) lâché de y = 100 avec v = 0, premiers ticks (les doubles et leurs motifs binaires sont dans la sortie) :

```
order A tick 1 y 100.0              vy -0.0784
order A tick 2 y 99.9216            vy -0.15523199999999998
order B tick 1 y 99.9216            vy -0.0784
order B tick 2 y 99.766368          vy -0.15523199999999998
order C tick 1 y 99.92              vy -0.08
order C tick 2 y 99.7616            vy -0.15839999999999999
order D tick 1 y 99.92              vy -0.0784
order D tick 2 y 99.7616            vy -0.15523199999999998
order E tick 1 y 100.0              vy -0.08
order E tick 2 y 99.92              vy -0.15839999999999999
```

Un bloc qui tombe (g 0.04, d 0.98), trois premiers ticks, ordre D (celui de la page) : `(99.96, -0.0392)`, `(99.8808, -0.07761599999999999)`, `(99.763184, -0.11526368)`. Ordres avec le point fixe de la vitesse terminale documentée : A, B et D pour les vivants, blocs, objets, orbes, wagonnets, lancers, potions, fioles et flotteur ; C et E pour les flèches (5.0) et le crachat de lama (6.0). L'ordre B atteint |vy| >= 3.9 au tick 262 pour un vivant.

**V-EM4 (R1, R2, R3, R6) : inventaires D.** `registry` : `entity_type 161 contiguous`, `attribute 40`, `mob_effect 40`, `potion 46`, `game_rule 59`, `entity types with loot_table entities/<id> 93 without 68` ; `etags` : `undead ... expanded 17`, `aquatic 14`, `fall_damage_immune 18`, `can_breathe_under_water 32` ; `btags` : `climbable direct 9 expanded 9`, `fall_damage_resetting 11`, `valid_spawn 2`.

**V-EM5 (R14, R15) : armure et modificateurs d'objets (D).** `armor` : `pieces by slot [('body', 12), ('chest', 7), ('feet', 7), ('head', 8), ('legs', 7)]` ; `minecraft:iron armor {'body': 5.0, 'chest': 6.0, 'feet': 2.0, 'head': 2.0, 'legs': 5.0}` ; `minecraft:netherite ... toughness [3.0] kbres [0.1]` ; `weapons` : `distinct (damage, speed) modifier pairs 34`.

## Harnais commun

Ce harnais est celui de tous les documents d'entités ; les autres vecteurs ne le répètent pas.

- **Serveur** : 26.3 headless, `tools/oracle`, RCON, monde plat sans structures ni créatures, couche de pierre pleine de y = -64 à y = -61 (surface à y = -60), `forceload add -64 -64 64 64` pour garder la zone chargée sans joueur (hypothèse H-EM30 : les entités d'un chunk forcé reçoivent leur tick sans joueur ; EM0 le lève, sinon un client est nécessaire).
- **Réglages avant chaque scénario** : `gamerule advance_time false`, `gamerule advance_weather false`, `gamerule spawn_mobs false`, `gamerule spawn_monsters false`, `gamerule spawn_patrols false`, `gamerule spawn_phantoms false`, `gamerule spawn_wandering_traders false`, `gamerule spawn_wardens false`, `gamerule mob_griefing false`, `gamerule random_tick_speed 0`, `difficulty <valeur du scénario>`, puis `tick freeze` (R79 de la spec du modèle).
- **Mise en place** : `summon <type> <x> <y> <z> {Tags:["probe","<nom>"],NoAI:1b,PersistenceRequired:1b,Silent:1b,...}` ; `fill` et `setblock` pour le décor. Les clés NBT de mise en place citées dans les sondes (`Age`, `Size`, `Value`, `PickupDelay`, `Fuse`, `Time`, `Pose`...) viennent de la documentation ou d'un usage courant : EM0 les confirme sur l'oracle, et une clé renommée en 26.3 (la page "Entity format" écrit déjà `fall_distance` et `invulnerable_time`) est corrigée avant la sonde qui l'emploie.
- **Pas** : `tick step 1` puis relevé ; le compteur de ticks est `time query gametime`.
- **Relevé** : `data get entity @e[tag=<nom>,limit=1] <chemin>` pour chaque chemin ETS (R77) ; la réponse est conservée telle quelle (texte exact) dans `reports/probes/`.
- **Lecteur indépendant** : `tools/oracle` lit les réponses avec son propre lecteur SNBT, sans importer `engine/*`.
- **Comptes** : tous les nombres de répétitions sont des minimums ; une sonde est reproductible quand ses commandes sont dans `reports/probes/<id>/commands.txt`.

## Sondes (EM)

Chaque sonde donne : montage et commandes, sortie attendue provisoire, **critère de succès**, règles levées. L'oracle n'est pas lancé par le `spec-analyst` ni par le `test-engineer` (docs/VERIFICATION.md, Sondes et vecteurs).

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Critère de succès | Règles levées |
|---|---|---|---|---|
| EM0 | (a) 10 000 doubles tirés avec 17 chiffres significatifs : `data modify storage sonde:t v set value <x>d` puis `data get storage sonde:t v` ; (b) pour chacun des 161 types : `summon` (si accepté) puis `data get entity @e[tag=probe,limit=1]` en entier ; (c) `time query gametime`, `tick step 5`, `time query gametime` ; (d) `data merge entity` de `Motion:[20d,0d,0d]`, `Rotation:[370f,100f]` puis lecture sans pas ; (e) objet invoqué en l'air `Item:{id:"minecraft:stone",count:1b},PickupDelay:32767s,Age:-32768s`, lecture avant le premier pas puis après 1 pas ; (f) le même scénario 10 fois ; (g) forceload sans joueur | texte décimal relisible à l'identique ; liste de clés par type conforme à R28 aux noms près ; compteur +5 ; `Motion` hors bornes remis à zéro ; lacet normalisé, tangage borné ; un premier pas qui applique le tick ; traces identiques ; entités ticquées en chunk forcé | (a) 10 000 sur 10 000 relus identiques (sinon consigner la forme du texte) ; (b) 161 listes de clés enregistrées ; (c) différence exacte de 5 ; (d) valeurs relues égales aux attendues de R46 et R49 ; (e) la suite après 1 pas est celle d'un des cinq ordres de V-EM3 (ils sont séparés dès le premier tick) ; (f) 10 traces égales sauf UUID ; (g) la position d'un objet lâché change sous `tick step` sans joueur | R27 à R29, R34, R35 à R37, R46, R49, R77 à R82 |
| EM1 | Pour chaque type : `summon` à (0.5, -60, 0.5) avec `Tags:["probe"],NoAI:1b` ; enregistrer succès ou message d'échec ; `attribute @e[tag=probe,limit=1] minecraft:max_health get` ; `data get entity ... NoAI` ; `data get entity ... Health` | table 161 lignes : invocable, vivant, mob, santé | 161 lignes complètes ; chaque type des étiquettes `undead`, `aquatic`, `boat`, `raiders` est classé ; les échecs d'invocation (`player`...) sont listés avec leur message | R4, R5, R69 de entity-player.md |
| EM2 | Pour chaque type (et pour `scale` 0.0625, 0.5, 2, 16 ; bébé `Age:-24000` ; `Size` 0, 1, 3, 126 pour slime et cube de magma) : entité à Pos (0.5, -60.0, 0.5), `NoGravity:1b,NoAI:1b` ; bissection sur chaque axe (20 pas) du plus petit éloignement `d` tel que `execute if entity @e[tag=probe,x=<x>,y=-60,z=0.5,dx=0,dy=0,dz=0]` réussit, côtés gauche et droit, bas et haut ; yeux : `execute as @e[tag=probe] at @s anchored eyes positioned ^ ^ ^ run summon marker ~ ~ ~ {Tags:["eye"]}` puis `data get entity @e[tag=eye,limit=1] Pos[1]` | largeurs et hauteurs de V-EM2 ; boîte centrée ; rapport égal à `scale` | demi-largeur gauche = droite à 1e-4 (H-EM6) ; largeur et hauteur de V-EM2 retrouvées à 1e-3 (sinon la valeur mesurée remplace celle de la page) ; rapport mesuré / rapport à l'échelle 1 = `scale` à 1e-3 (H-EM7) ; table complète des 161 types | R19 à R25 |
| EM3 | Pour chaque type vivant et chacun des 40 attributs : `attribute @e[tag=probe,limit=1] minecraft:<a> base get` (erreur si l'attribut est absent) ; puis `base set 1000000000`, `get`, `base get`, `base set -1000000000`, `get`, `base get`, `base reset` | table (type, attribut, base) ; bornes de chaque attribut | table complète ; pour chacun des 40 attributs (sur un type qui le porte) minimum et maximum égaux à V-EM1, sinon liste des écarts ; comportement de `base get` après écriture hors bornes consigné (H-EM4) | R8, R9, R13 |
| EM4 | Zombie `NoAI`, attribut `armor` (et `max_health`, `movement_speed`) : (a) un modificateur par opération ; (b) trois opérations ensemble ; (c) 10 modificateurs `add_value` de montants {0.1, 1e-9, 1e9, -1e9, 0.3, ...} ajoutés dans 6 ordres ; (d) ajout d'un id existant ; (e) retrait d'un id absent ; (f) bornage ; (g) équipement : `item replace entity @e[tag=probe] armor.chest with minecraft:iron_chestplate`, lecture, retrait ; lecture par `get`, `modifier value get`, `data get entity ... attributes` | valeur = formule R11 ; ordre d'insertion ; erreur sur doublon ; armure 6 avec le plastron | 200 combinaisons tirées : égalité exacte (texte) avec la formule de R11 ; H-EM2 résolue (somme dans l'ordre d'insertion, ou triée) ; H-EM3 résolue ; après `item replace`, `armor` augmente de 6 puis revient | R10 à R13, R17 |
| EM5 | Types de mouvement libre : objet (`Item`, `PickupDelay:32767s`, `Age:-32768s`), bloc qui tombe (état du bloc, temps de chute et lâcher d'objet : clés NBT relevées par EM0), dynamite (`fuse:32767s` ; le nom de la clé est relevé par EM0), support d'armure, zombie `NoAI`, orbe d'expérience (`Value:1s`), cube de soufre, bateau, wagonnet (hors rails) ; départ y = 300 (sol à -60 ou colonne sans bloc jusqu'au vide), `Motion` nul, puis `[0,0.5,0]` et `[0.3,0,0.2]` ; variantes `NoGravity:1b`, `attribute gravity base set 0.04 / 0 / -0.04` ; 300 ticks ; ETS | suites de y, vy, vx | pour chaque type, égalité exacte du texte (y, vy) avec exactement un des ordres A à E de R40 sur 300 ticks (attendu : A pour les vivants, D pour le bloc qui tombe et la dynamite, B pour le bateau ; objet, orbe et wagonnet : à relever) ; les points fixes mesurés égalent R39 ; sinon la suite est consignée comme donnée de calibrage ; H-EM26 tranchée (un `NoAI` garde-t-il la gravité ?) | R38 à R42, R48, R75 |
| EM6 | Objets glissant à `Motion:[1d,0d,0d]` sur chaque bloc de la liste (tous les blocs à `blocks_motion` plus glace, glace compacte, glace bleue, slime, miel, sable d'âme, boue, neige, tapis) ; relever vx à chaque tick ; entité à cheval sur deux blocs ; vivant avec `attribute friction_modifier / air_drag_modifier base set` à 0, 0.5, 1, 2 ; en l'air avec `NoGravity:1b` | table de friction par bloc ; coefficient 0.91 en l'air | table complète des coefficients (texte exact) ; coefficient aérien 0.91 retrouvé ; rapport des décélérations pour les attributs = valeur de H-EM16 à 1e-12 ; règle de la position de référence H-EM17 résolue | R43 à R45 |
| EM7 | Objets et supports d'armure lancés (`Motion`) contre : un sol, un plafond, un mur sur chaque face, un coin, un bord, dalle, escalier, clôture, vitre, porte, trappe, échelle, lit, coffre (au moins 300 états de bloc) ; balayage à pas de 1/64 de bloc de la face d'impact ; cube de soufre lâché de 10 blocs ; zombie `NoAI` poussé vers un vide ; `setblock` dans une boîte ; `Motion` sur un cadre ; vexes, dragon, oeil d'ender | position finale, `OnGround`, `Motion` après impact, hauteur des surfaces, rebond | égalité exacte de la position finale pour 1000 jets avec un seul des ordres d'axes (H-EM20) ; forme de collision de chaque état échantillonné consignée à 1/64 de bloc (R51) ; rebond = H-EM18 à 1e-9 ; H-EM27 à H-EM29 tranchées ; liste des types sans collision de bloc complète | R47, R51 à R53, R56, R59, R60, R76 |
| EM8 | Colonne d'eau de 10 blocs, lave, colonnes de bulles (montante et descendante), toile, miel, neige poudreuse, échelle, lianes, échafaudage, canal d'eau coulante de longueur 8 ; objet, support d'armure, zombie `NoAI`, bloc qui tombe ; chute de 10 blocs dans chaque bloc de `fall_damage_resetting` puis lecture de `fall_distance` ; zombie `NoAI` avec l'effet Weaving dans la toile : `effect give @e[tag=probe] minecraft:weaving infinite 0` | traces vx, vy ; `fall_distance` | table des vitesses terminales et décélérations par fluide ; remise à zéro de `fall_distance` exacte pour les 11 blocs et seulement eux ; rapport du ralentissement de la toile avec et sans Weaving = 0.75 selon la page (H-EM25) ; H-EM24 résolue | R54, R61 à R67 |
| EM9 | Zombie `NoAI` (sinon un joueur via un client) poussé par `Motion:[0.2d,0d,0d]` contre des marches de hauteur 0.5, 0.5625, 0.6, 0.625, 1.0, 1.5 ; `attribute step_height base set` à 0, 0.6, 1.0, 1.5 ; `scale` 2 contre 1 | franchissement ou arrêt | hauteur franchie maximale mesurée à 1/64 près égale à `step_height` ; indépendance de `scale` (H-EM8) ; sinon consigner la loi | R26, R55 |
| EM10 | 2 à 30 supports d'armure et zombies `NoAI` superposés dans une boîte de la taille d'un bloc ; `gamerule max_entity_cramming` à 24, 2, 0 ; `difficulty normal` ; relevé de `Health` et de `Pos` sur 40 ticks ; deux entités décalées de 0.0 à 0.6 | déplacements, dégâts d'entassement | dégâts relevés = 6 avec fréquence 25 pour cent par tick à l'échelle statistique (khi-deux, p >= 1e-4) ; seuil de 24 ; vecteur de répulsion en fonction du recouvrement (R57) tabulé avec constante et plafond | R57, R58 |
| EM11 | 3 objets de même type (Item stone) très proches puis séparés de 0.1, invoqués dans 6 ordres de `summon` ; 40 ticks ; lecture de `Count` et `Age` ; deux supports d'armure se poussant | quel objet absorbe l'autre ; quel support bouge | l'ordre de tick déduit est le même sur les 6 permutations et sur 10 répétitions ; H-EM13 tranchée | R34 |
| EM12 | Client protocole minimal (jalon M8) à Pos connue ; capture des paquets du tableau R30 pour : support d'armure en mouvement, 20 types d'entités, un joueur s'éloignant de 16 à 300 blocs | contenu de `add_entity`, indices de `set_entity_data`, périodicité de `entity_position_sync`, portée de suivi par type | tables consignées ; H-EM10 à H-EM12 tranchées ; bloquant tant qu'aucun client n'existe | R31 à R33 |
| EM13 | Objet à `Age` 5990 et à PickupDelay variés, `tick step 20` ; bloc qui tombe sans support ; dynamite ; fusion d'objets (distance, tick, taille de pile) ; objet en lave (netherite et autre) ; entité franchissant x = 15.999 vers 16.001 puis `save-all` et lecture du fichier d'entités de région par le lecteur indépendant | tick de disparition ; fusion ; chunk de stockage | disparition à l'`Age` 6000 exactement ; temps de chute de 600 ticks ; distance de fusion de R69 ; chunk de stockage = division euclidienne ; objets netherite intacts dans la lave | R50, R69 à R72, R43 de entity-ai-spawning.md |
| EM14 | Flèche (`Motion:[1d,1d,0d]`), trident, boule de neige, oeuf, perle, potion lancée, fiole d'expérience, crachat de lama : 100 ticks ; flèche contre bloc | suites de position | égalité exacte avec exactement un des ordres A à E sur 100 ticks (attendu : B pour boule de neige, oeuf, perle, potion et fiole avec les gravités 0.03, 0.05, 0.07 ; E pour le crachat ; C ou E pour les flèches) ; position d'arrêt contre un bloc consignée ; durée de vie d'une flèche plantée mesurée | R73, R74 |

## Couverture des règles S par les sondes

R4, R5 : EM1 ; R8, R9, R13 : EM3 ; R11, R12, R17 : EM4 ; R19 à R25 : EM2 ; R26 : EM9 ; R27 à R29, R35 à R37, R46, R49, R77 à R82 : EM0 ; R31 à R33 : EM12 ; R34 : EM11 ; R38 à R42, R48, R75 : EM5 ; R43 à R45 : EM6 ; R47, R51 à R53, R56, R59, R60, R76 : EM7 ; R54, R61 à R67 : EM8 ; R55 : EM9 ; R57, R58 : EM10 ; R50, R69 à R72 : EM13 ; R73, R74 : EM14 ; R39 et R41 (C sur S) : EM5, EM14. R83, R84 sont des décisions de conception (R83, R84 de la spec). Aucune autre règle S n'est sans sonde.

## Récapitulatif

- Sondes : EM0 à EM14, soit 15. Prérequis : harnais commun, lecteur `.mca` indépendant, EULA acceptée ; EM12 exige un client protocole (jalon M8).
- Questions bloquantes pour l'oracle : (i) les entités ticquent-elles en chunk forcé sans joueur (H-EM30) ; (ii) les `NoAI` gardent-ils la physique (H-EM26) ; sinon, il faut un client pour pousser des joueurs.

## Annexe : `ent_ref.py`

Le texte intégral du script suit. Il ne lit que des fichiers du cache d'extraction, ne copie aucune donnée dans git et ne lance pas le jeu. À copier hors du dossier de données avant exécution.

```python
#!/usr/bin/env python3
"""Read-only inventory of the extracted 26.3 entity-related data (specs entity-*.md).

Usage: python -I ent_ref.py REF_DIR SUBCOMMAND
  REF_DIR = .cache/reference/26.3
Subcommands: registry etags btags damage armor weapons food spawns dims loot cmds packets archetypes spawners timelines vehicles fall calc player
Reads extracted files only. Standard library only. Output is deterministic (sorted, LF).
"""
import collections
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", newline="\n")
REF = sys.argv[1]
REP = os.path.join(REF, "generated", "reports")
DATA = os.path.join(REF, "resources", "data", "minecraft")
ITEMS = os.path.join(REP, "minecraft", "components", "item")


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def tree(sub):
    out = {}
    base = os.path.join(DATA, sub)
    for root, _, files in os.walk(base):
        for fn in files:
            if fn.endswith(".json"):
                rel = os.path.relpath(os.path.join(root, fn), base).replace(os.sep, "/")[:-5]
                out[rel] = load(os.path.join(root, fn))
    return dict(sorted(out.items()))


def reg(name):
    return [e.split(":", 1)[1] for e in load(os.path.join(REP, "registries.json"))["minecraft:" + name]["entries"]]


def items():
    out = {}
    for fn in sorted(os.listdir(ITEMS)):
        out[fn[:-5]] = load(os.path.join(ITEMS, fn))["components"]
    return out


def resolve_tag(tags, name, seen=None):
    """Expand a tag to a sorted set of concrete ids (strings without namespace)."""
    seen = seen or set()
    if name in seen:
        return set()
    seen.add(name)
    out = set()
    for v in tags[name]["values"]:
        v = v["id"] if isinstance(v, dict) else v
        if v.startswith("#"):
            out |= resolve_tag(tags, v[len("#minecraft:"):], seen)
        else:
            out.add(v.split(":", 1)[1])
    return out


def section(name):
    print("== " + name)


def cmd_registry():
    section("registries")
    r = load(os.path.join(REP, "registries.json"))
    for k in ("entity_type", "attribute", "mob_effect", "potion", "game_rule", "activity",
              "sensor_type", "memory_module_type", "villager_profession", "point_of_interest_type"):
        e = r["minecraft:" + k]["entries"]
        ids = sorted(v["protocol_id"] for v in e.values())
        print(k, len(e), "contiguous" if ids == list(range(len(ids))) else "gaps")
    for k in ("attribute", "mob_effect", "game_rule", "activity", "sensor_type", "block_entity_type"):
        names = reg(k)
        print(k, "names", names if k != "block_entity_type" else [n for n in names if "spawn" in n])
    lang = load(os.path.join(REF, "resources", "assets", "minecraft", "lang", "en_us.json"))
    miss = [a for a in reg("attribute") if "attribute.name." + a not in lang]
    print("attributes without lang name", miss)
    print("datapack damage_type files", len(tree("damage_type")))
    print("data/minecraft top-level directories", sorted(os.listdir(DATA)))
    print("data/minecraft files named like entity type definitions", sorted(d for d in os.listdir(DATA) if "entity" in d))
    print("blocks.json keys of one block", sorted(next(iter(load(os.path.join(REP, "blocks.json")).values()))))
    ets = reg("entity_type")
    loot = tree("loot_table")
    with_loot = [e for e in ets if "entities/" + e in loot]
    print("entity types with loot_table entities/<id>", len(with_loot), "without", len(ets) - len(with_loot))
    print("without", [e for e in ets if e not in with_loot])
    print("entities/* tables not matching an entity type", sorted(k for k in loot if k.startswith("entities/") and k[9:] not in ets))


def cmd_etags():
    section("entity_type tags")
    tags = tree("tags/entity_type")
    ets = set(reg("entity_type"))
    for name in tags:
        mem = resolve_tag(tags, name)
        unknown = sorted(m for m in mem if m not in ets)
        print(name, "direct", len(tags[name]["values"]), "expanded", len(mem), "unknown", unknown)
    for name in ("undead", "aquatic", "arthropod", "illager", "raiders", "boat", "fall_damage_immune",
                 "powder_snow_walkable_mobs", "can_breathe_under_water", "burn_in_daylight", "skeletons", "zombies"):
        print(name, sorted(resolve_tag(tags, name)))


BLOCK_TAGS = ("valid_spawn", "animals_spawnable_on", "wolves_spawnable_on", "rabbits_spawnable_on",
              "foxes_spawnable_on", "goats_spawnable_on", "mooshrooms_spawnable_on", "parrots_spawnable_on",
              "polar_bears_spawnable_on_alternate", "frogs_spawnable_on", "bats_spawnable_on",
              "camels_spawnable_on", "armadillo_spawnable_on", "axolotls_spawnable_on",
              "invalid_spawn_inside", "prevent_mob_spawning_inside", "climbable", "fall_damage_resetting",
              "suppresses_bounce", "causes_suffocation", "blocks_motion", "soul_speed_blocks",
              "enables_bubble_column_push_up", "enables_bubble_column_drag_down", "can_glide_through",
              "default_immune_to", "wither_immune", "dragon_immune", "dangerous_for_teleportation",
              "entities_can_teleport_to", "enderman_holdable", "mob_interactable_doors", "ice",
              "snow", "cushion_uses_collision_shape", "happy_ghast_avoids")


def cmd_btags():
    section("block tags used by entities")
    tags = tree("tags/block")
    for name in BLOCK_TAGS:
        if name not in tags:
            print(name, "ABSENT")
            continue
        mem = resolve_tag(tags, name)
        print(name, "direct", len(tags[name]["values"]), "expanded", len(mem))
    for name in ("valid_spawn", "animals_spawnable_on", "climbable", "fall_damage_resetting", "suppresses_bounce",
                 "invalid_spawn_inside", "prevent_mob_spawning_inside", "soul_speed_blocks", "bats_spawnable_on",
                 "wolves_spawnable_on", "rabbits_spawnable_on", "polar_bears_spawnable_on_alternate"):
        print(name, sorted(resolve_tag(tags, name))[:12])
    ftags = tree("tags/fluid")
    for name in sorted(ftags):
        print("fluid", name, sorted(resolve_tag(ftags, name)))


def cmd_damage():
    section("damage_type files")
    dts = tree("damage_type")
    print("count", len(dts))
    for field in ("scaling", "effects", "death_message_type", "exhaustion"):
        c = collections.Counter(str(v.get(field, "(absent)")) for v in dts.values())
        print(field, sorted(c.items()))
    print("keys", sorted(collections.Counter(k for v in dts.values() for k in v).items()))
    tags = tree("tags/damage_type")
    print("tags", len(tags))
    names = set(dts)
    for t in tags:
        mem = sorted(resolve_tag(tags, t))
        print(t, len(mem), mem if len(mem) <= 12 else mem[:6] + ["..."] + mem[-3:], "unknown", [m for m in mem if m not in names])
    section("per damage type tag membership")
    for d in sorted(dts):
        print(d, ",".join(sorted(t for t in tags if d in resolve_tag(tags, t))))
    section("damage types in none of the tags")
    print(sorted(d for d in dts if not any(d in resolve_tag(tags, t) for t in tags)))
    section("exhaustion distribution by value")
    by = collections.defaultdict(list)
    for d, v in sorted(dts.items()):
        by[v.get("exhaustion")].append(d)
    for k in sorted(by, key=lambda x: (x is None, x)):
        print(k, len(by[k]), by[k] if len(by[k]) < 8 else by[k][:4] + ["..."])
    section("exhaustion against membership of bypasses_armor and scaling")
    ba = resolve_tag(tags, "bypasses_armor")
    cross = collections.Counter((v.get("exhaustion"), d in ba, v["scaling"]) for d, v in dts.items())
    for k in sorted(cross):
        print("exhaustion", k[0], "bypasses_armor", k[1], "scaling", k[2], cross[k])
    print("always-scaling types", sorted(d for d, v in dts.items() if v["scaling"] == "always"))
    print("types with effects", sorted((d, v["effects"]) for d, v in dts.items() if "effects" in v))
    print("types with death_message_type", sorted((d, v["death_message_type"]) for d, v in dts.items() if "death_message_type" in v))


SLOTS = ("head", "chest", "legs", "feet", "body", "mainhand", "offhand", "hand", "armor", "any")


def cmd_armor():
    section("armor pieces (equippable items with armor attribute modifiers)")
    its = items()
    by = collections.defaultdict(dict)
    cnt = collections.Counter()
    for name, comp in its.items():
        eq = comp.get("minecraft:equippable")
        mods = comp.get("minecraft:attribute_modifiers", [])
        a = {m["type"].split(":")[1]: (m["amount"], m["operation"]) for m in mods if m.get("slot") in ("head", "chest", "legs", "feet", "body")}
        if eq and "armor" in a:
            cnt[eq["slot"]] += 1
            by[eq.get("asset_id", "?")][eq["slot"]] = (a["armor"][0], a.get("armor_toughness", (0, 0))[0], a.get("knockback_resistance", (0, 0))[0], name)
    print("pieces by slot", sorted(cnt.items()))
    for mat in sorted(by):
        row = by[mat]
        print(mat, "armor", {s: row[s][0] for s in sorted(row)}, "toughness", sorted({row[s][1] for s in row}), "kbres", sorted({row[s][2] for s in row}))
    section("other attribute modifier types on items (type, operation, slot) counts")
    c = collections.Counter()
    for name, comp in its.items():
        for m in comp.get("minecraft:attribute_modifiers", []):
            c[(m["type"].split(":")[1], m["operation"], m.get("slot"))] += 1
    for k, v in sorted(c.items()):
        print(k, v)


def cmd_weapons():
    section("attack_damage and attack_speed modifiers on mainhand")
    its = items()
    rows = collections.defaultdict(list)
    for name, comp in its.items():
        d = s = None
        for m in comp.get("minecraft:attribute_modifiers", []):
            t = m["type"].split(":")[1]
            if m.get("slot") == "mainhand" and t == "attack_damage":
                d = m["amount"]
            if m.get("slot") == "mainhand" and t == "attack_speed":
                s = m["amount"]
        if d is not None or s is not None:
            rows[(d, s)].append(name)
    print("distinct (damage, speed) modifier pairs", len(rows))
    for k in sorted(rows, key=lambda x: (str(x[0]), str(x[1]))):
        v = rows[k]
        print(k, len(v), v if len(v) <= 3 else v[:2] + ["..."])
    section("weapon-like components")
    c = collections.Counter()
    for name, comp in its.items():
        for k in ("minecraft:weapon", "minecraft:kinetic_weapon", "minecraft:piercing_weapon", "minecraft:blocks_attacks",
                  "minecraft:swing_animation", "minecraft:attack_range", "minecraft:damage_type", "minecraft:minimum_attack_charge"):
            if k in comp:
                c[k] += 1
    print(sorted(c.items()))
    for key in ("minecraft:blocks_attacks", "minecraft:kinetic_weapon", "minecraft:piercing_weapon"):
        for name, comp in its.items():
            if key in comp:
                print(key, name, json.dumps(comp[key], sort_keys=True)[:420])
                break


def cmd_food():
    section("food component")
    its = items()
    foods = {n: c["minecraft:food"] for n, c in its.items() if "minecraft:food" in c}
    print("items with food", len(foods))
    print("keys", sorted(collections.Counter(k for f in foods.values() for k in f).items()))
    print("nutrition", sorted(collections.Counter(f["nutrition"] for f in foods.values()).items()))
    print("saturation", sorted(collections.Counter(f["saturation"] for f in foods.values()).items()))
    for n in ("bread", "cooked_beef", "golden_apple", "rotten_flesh", "apple", "enchanted_golden_apple"):
        print(n, foods.get(n), json.dumps(its[n].get("minecraft:consumable"), sort_keys=True))
    cons = collections.defaultdict(list)
    for n, c in sorted(its.items()):
        x = c.get("minecraft:consumable")
        if x is not None:
            key = json.dumps({k: v for k, v in x.items() if k != "on_consume_effects"}, sort_keys=True)
            cons[key].append(n)
    print("items with a consumable component", sum(len(v) for v in cons.values()), "parameter sets without on_consume_effects", len(cons))
    for k, v in sorted(cons.items(), key=lambda kv: (-len(kv[1]), kv[0])):
        print(len(v), k, v if len(v) <= 3 else v[:2] + ["..."])
    print("items with a food component but no consumable", sorted(n for n, c in its.items() if "minecraft:food" in c and "minecraft:consumable" not in c))
    print("items with on_consume_effects", sum(1 for c in its.values() if c.get("minecraft:consumable", {}).get("on_consume_effects")))


def cmd_spawns():
    section("natural_mob_spawns attribute per biome")
    biomes = tree("worldgen/biome")
    cats = collections.defaultdict(lambda: {"biomes": 0, "entries": 0, "types": set(), "weights": collections.Counter(), "counts": collections.Counter()})
    probs = collections.Counter()
    costs = collections.Counter()
    for b, v in biomes.items():
        at = v["attributes"]
        arg = at["minecraft:gameplay/natural_mob_spawns"]["argument"]
        for cat, lst in arg.get("spawns_by_category", {}).items():
            if lst:
                cats[cat]["biomes"] += 1
            for e in lst:
                cats[cat]["entries"] += 1
                cats[cat]["types"].add(e["type"].split(":")[1])
                cats[cat]["weights"][e["weight"]] += 1
                cats[cat]["counts"][json.dumps(e["count"], sort_keys=True)] += 1
        for t, c in arg.get("spawn_costs", {}).items():
            costs[(t, c["charge"], c["energy_budget"])] += 1
        if "minecraft:gameplay/creature_world_gen_spawn_probability" in at:
            probs[(b, at["minecraft:gameplay/creature_world_gen_spawn_probability"])] += 1
    print("biomes", len(biomes))
    for cat in sorted(cats):
        c = cats[cat]
        print(cat, "biomes_with_entries", c["biomes"], "entries", c["entries"], "types", len(c["types"]), sorted(c["types"]))
        print("  weights", sorted(c["weights"].items()))
        print("  counts", sorted(c["counts"].items()))
    print("spawn_costs", sorted(costs.items()))
    print("creature_world_gen_spawn_probability", sorted(probs.items()))
    section("spawn entries by biome for monsters in overworld preset (distinct lists)")
    lists = collections.Counter()
    for b, v in biomes.items():
        arg = v["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]
        lst = arg.get("spawns_by_category", {}).get("monster", [])
        lists[json.dumps([(e["type"], e["weight"]) for e in lst])] += 1
    print("distinct monster lists", len(lists))
    for k, n in sorted(lists.items(), key=lambda kv: (-kv[1], kv[0]))[:3]:
        print(n, k)
    section("structure spawn_overrides")
    structs = tree("worldgen/structure")
    for s, v in structs.items():
        for cat, o in v.get("spawn_overrides", {}).items():
            print(s, cat, o["bounding_box"], [(e["type"].split(":")[1], e["weight"], e["count"]) for e in o["spawns"]])
    section("other spawn related data")
    ms = tree("worldgen/biome")
    print("has_precipitation", sorted(collections.Counter(str(v["has_precipitation"]) for v in ms.values()).items()))
    print("trial_spawner files", len(tree("trial_spawner")))
    print("spawn_condition_type registry", reg("spawn_condition_type"))
    for kind in ("cat_variant", "frog_variant", "pig_variant", "wolf_variant", "cow_variant", "chicken_variant"):
        t = tree(kind)
        print(kind, len(t), sorted(collections.Counter(json.dumps([c.get("condition", {}).get("type") for c in x.get("spawn_conditions", [])]) for x in t.values()).items()))


def cmd_dims():
    section("dimension_type spawn fields")
    for n, v in tree("dimension_type").items():
        print(n, {k: v[k] for k in sorted(v) if k.startswith("monster_spawn") or k in ("min_y", "height", "logical_height", "has_skylight", "has_ceiling", "ambient_light", "coordinate_scale", "has_fixed_time", "bed_works", "piglin_safe", "natural")})


def cmd_loot():
    section("entity loot tables and their types")
    loot = tree("loot_table")
    ets = reg("entity_type")
    rows = collections.Counter()
    for k, t in loot.items():
        if k.startswith("entities/"):
            rows[t["type"]] += 1
    print(sorted(rows.items()))
    e_without = [e for e in ets if "entities/" + e not in loot]
    print("entity types without table", len(e_without), e_without)
    print("sheep and shulker colour tables", sorted(k for k in loot if k.startswith("entities/sheep/") or k.startswith("entities/shulker")))
    print("charged_creeper", sorted(k for k in loot if k.startswith("charged_creeper")))


def cmd_archetypes():
    section("sulfur_cube_archetype files (entity attribute modifiers carried by data)")
    arch = tree("sulfur_cube_archetype")
    print("files", len(arch), "keys", sorted(collections.Counter(k for v in arch.values() for k in v).items()))
    for n, v in arch.items():
        mods = [(m["attribute"].split(":")[1], m["operation"], round(m["amount"], 6)) for m in v["attribute_modifiers"]]
        print(n, "buoyant", v.get("buoyant"), "knockback_modifiers", v["knockback_modifiers"], "modifiers", mods)
    print("attribute/operation pairs", sorted(collections.Counter((m["attribute"].split(":")[1], m["operation"]) for v in arch.values() for m in v["attribute_modifiers"]).items()))


def cmd_spawners():
    section("trial_spawner configurations")
    ts = tree("trial_spawner")
    print("files", len(ts), "keys", sorted(collections.Counter(k for v in ts.values() for k in v).items()))
    for k in sorted(ts):
        v = ts[k]
        ents = sorted({p["data"]["entity"]["id"].split(":")[1] for p in v.get("spawn_potentials", [])})
        print(k, {x: v[x] for x in sorted(v) if x in ("simultaneous_mobs", "simultaneous_mobs_added_per_player", "total_mobs", "total_mobs_added_per_player", "ticks_between_spawn", "spawn_range")}, ents)
    section("spawner related block entity types and blocks")
    print([b for b in reg("block_entity_type") if "spawn" in b])


def render(node, path, out):
    kids = node.get("children", {})
    if node.get("executable"):
        out.append(" ".join(path))
    for k in sorted(kids):
        v = kids[k]
        if v["type"] == "literal":
            render(v, path + [k], out)
        else:
            p = v.get("parser", "?").replace("minecraft:", "")
            pr = v.get("properties", {})
            extra = ""
            if "registry" in pr:
                extra = ":" + pr["registry"].replace("minecraft:", "")
            elif "min" in pr or "max" in pr:
                extra = ":%s..%s" % (pr.get("min", ""), pr.get("max", ""))
            render(v, path + ["<%s%s>" % (k, "" if p == "?" else "=" + p + extra)], out)


def cmd_cmds():
    section("executable paths of entity-related commands")
    c = load(os.path.join(REP, "commands.json"))["children"]
    for name in ("tick", "summon", "effect", "attribute", "damage", "kill", "ride", "xp", "experience", "teleport",
                 "tp", "rotate", "spreadplayers", "spawnpoint", "difficulty", "gamemode", "swing", "spectate"):
        out = []
        render(c[name], [name], out)
        print(name, len(out))
        for line in out:
            print("  " + line)
    section("data get/modify entity forms")
    out = []
    render(c["data"], ["data"], out)
    print("data forms", len(out), [l for l in out if "entity" in l][:8])
    section("execute conditions on entity")
    out = []
    render(c["execute"], ["execute"], out)
    print([l for l in out if l.startswith("execute if <") or l.startswith("execute if entity")][:2])
    print("execute if literals", sorted(c["execute"]["children"]["if"]["children"]))
    print("execute on literals", sorted(c["execute"]["children"]["on"]["children"]))
    print("execute anchored literals", sorted(c["execute"]["children"]["anchored"]["children"]))


def cmd_packets():
    section("entity related play packets")
    p = load(os.path.join(REP, "packets.json"))["play"]
    for side in ("clientbound", "serverbound"):
        names = [n.split(":", 1)[1] for n in p[side]]
        pick = sorted(n for n in names if any(w in n for w in ("entity", "move", "rotate", "teleport", "velocity", "effect", "attribute", "damage", "hurt", "health", "experience", "vehicle", "player_abilities", "player_input", "player_command", "interact", "animate", "attack", "spawn", "set_passengers", "take_item", "death", "respawn", "status")))
        print(side, len(names), pick)


def cmd_timelines():
    section("gameplay tracks of timelines (data driven environment values read by entities)")
    tl = tree("timeline")
    for name, t in tl.items():
        for track, v in t["tracks"].items():
            if "/gameplay/" in "/" + track.split(":", 1)[1]:
                kf = [(f["ticks"], f["value"]) for f in v["keyframes"]]
                print(name, "period", t.get("period_ticks"), track.split(":", 1)[1], "modifier", v.get("modifier"), "ease", v.get("ease"), kf)
    section("derived: ticks before the end of the period at which monsters_burn turns true")
    day = tl["day"]["tracks"]["minecraft:gameplay/monsters_burn"]["keyframes"]
    print("keyframes", [(f["ticks"], f["value"]) for f in day], "ticks before the end of the period", 24000 - day[1]["ticks"])
    section("registered clocks and time markers")
    print("world_clock files", sorted(tree("world_clock")))
    print("time markers", sorted((k, v["ticks"] if isinstance(v, dict) else v) for k, v in tl["day"]["time_markers"].items()))


def cmd_vehicles():
    section("vehicle and rider related types, tags and rail states")
    ets = reg("entity_type")
    print("minecart types", [e for e in ets if "minecart" in e])
    print("boat and raft types", len([e for e in ets if "boat" in e or "raft" in e]), "of which chest", len([e for e in ets if "chest_boat" in e or "chest_raft" in e]))
    tags = tree("tags/entity_type")
    for name in ("boat", "can_equip_saddle", "can_float_while_ridden", "dismounts_underwater", "cannot_be_pushed_onto_boats",
                 "can_turn_in_boats", "non_controlling_rider", "cannot_be_dismounted_by_item_usage", "can_equip_harness"):
        print(name, sorted(resolve_tag(tags, name)))
    b = load(os.path.join(REP, "blocks.json"))
    for name in ("rail", "powered_rail", "detector_rail", "activator_rail"):
        pr = b["minecraft:" + name]["properties"]
        print(name, {k: len(v) for k, v in sorted(pr.items())}, "states", len(b["minecraft:" + name]["states"]))
    print("rail shapes", b["minecraft:rail"]["properties"]["shape"])
    print("powered_rail shapes", b["minecraft:powered_rail"]["properties"]["shape"])
    g = load(os.path.join(REP, "commands.json"))["children"]["gamerule"]["children"]["max_minecart_speed"]["children"]["value"]
    print("max_minecart_speed argument", g.get("parser"), g.get("properties"))


def step_a(y, v, g, d):
    """Order A (position, acceleration, drag): move with the current velocity, then v = (v - g) * d."""
    return y + v, (v - g) * d


def step_b(y, v, g, d):
    """Order B (acceleration, drag, position): v = (v - g) * d, then move."""
    v = (v - g) * d
    return y + v, v


def step_c(y, v, g, d):
    """Order C (drag, acceleration, position): v = v * d - g, then move."""
    v = v * d - g
    return y + v, v


def step_d(y, v, g, d):
    """Order D (acceleration, position, drag): v = v - g, move, then v = v * d."""
    v = v - g
    return y + v, v * d


def step_e(y, v, g, d):
    """Order E (position, drag, acceleration): move, then v = v * d - g."""
    return y + v, v * d - g


ORDERS = (("A", step_a), ("B", step_b), ("C", step_c), ("D", step_d), ("E", step_e))


def hexbits(x):
    import struct
    return "%016x" % struct.unpack(">Q", struct.pack(">d", x))[0]


def end_speed(fn, g, d):
    y, v = 0.0, 0.0
    for _ in range(4000):
        y, v = fn(y, v, g, d)
    return abs(v)


def cmd_fall():
    section("terminal speeds documented per row of the motion table against the fixed point of each order (predictions)")
    kinds = (("living", 0.08, 0.98, 3.92), ("falling_block_and_tnt", 0.04, 0.98, 1.96), ("item", 0.04, 0.98, 1.96),
             ("orb", 0.03, 0.98, 1.47), ("minecart", 0.04, 0.95, 0.76), ("thrown_egg_snowball_pearl", 0.03, 0.99, 2.97),
             ("thrown_potion", 0.05, 0.99, 4.95), ("bottle_of_enchanting", 0.07, 0.99, 6.93), ("arrow_trident", 0.05, 0.99, 5.00),
             ("llama_spit", 0.06, 0.99, 6.00), ("bobber", 0.03, 0.92, 0.345))
    for name, g, d, doc in kinds:
        fits = [lab for lab, fn in ORDERS if abs(round(end_speed(fn, g, d), 3) - doc) < 0.0006]
        print(name, "g", g, "d", d, "documented", doc, "orders with this fixed point", "".join(fits))
    section("free fall of a living entity (g 0.08, d 0.98) from y=100, v=0, five update orders")
    for label, fn in ORDERS:
        y, v = 100.0, 0.0
        print("order", label)
        for t in range(1, 7):
            y, v = fn(y, v, 0.08, 0.98)
            print("  tick", t, "y", repr(y), hexbits(y), "vy", repr(v), hexbits(v))
    section("free fall of a falling block (g 0.04, d 0.98) from y=100, v=0, first ticks")
    for label, fn in ORDERS:
        y, v = 100.0, 0.0
        row = []
        for t in range(1, 4):
            y, v = fn(y, v, 0.04, 0.98)
            row.append((repr(y), repr(v)))
        print("order", label, row)
    y, v = 100.0, 0.0
    n = 0
    while v > -3.9 and n < 1000:
        y, v = step_b(y, v, 0.08, 0.98)
        n += 1
    print("order B ticks to reach |vy| >= 3.9:", n, "y", repr(y), "vy", repr(v))


def armor_factor(damage, ap, tough):
    ap = min(ap, 30.0)
    tough = min(tough, 20.0)
    red = min(20.0, max(ap / 5.0, ap - 4.0 * damage / (tough + 8.0)))
    return 1.0 - red / 25.0


def cooldown_mult(t, speed):
    period = 20.0 / speed
    return min(1.0, 0.2 + 0.8 * min(1.0, max(0.0, (t + 0.5) / period)) ** 2)


def xp_total(lv):
    if lv <= 16:
        return lv * lv + 6 * lv
    if lv <= 31:
        return 2.5 * lv * lv - 40.5 * lv + 360
    return 4.5 * lv * lv - 162.5 * lv + 2220


def xp_next(lv):
    if lv <= 15:
        return 2 * lv + 7
    if lv <= 30:
        return 5 * lv - 38
    return 9 * lv - 158


def cmd_calc():
    section("armor reduction (documented formula, toughness clamp 20, armor clamp 30)")
    for dmg, ap, tg in ((10, 0, 0), (10, 20, 0), (10, 20, 8), (3, 20, 0), (3, 20, 8), (40, 20, 8), (4, 8, 2), (1, 30, 20)):
        print("damage", dmg, "armor", ap, "toughness", tg, "factor %.6f" % armor_factor(dmg, ap, tg), "result %.6f" % (dmg * armor_factor(dmg, ap, tg)))
    print("diamond set armor", 3 + 8 + 6 + 3, "toughness", 4 * 2.0, "netherite set toughness", 4 * 3.0, "kb resistance", 4 * 0.1)
    section("order candidates for reductions (raw 10, armor 10, toughness 0, resistance I, protection EPF 8)")
    raw, ap, tg, res, epf = 10.0, 10.0, 0.0, 1, 8
    r = 1 - 0.2 * res
    p = 1 - min(epf, 20) / 25.0
    o1 = raw * armor_factor(raw, ap, tg) * r * p
    o2 = raw * r
    o2 = o2 * armor_factor(o2, ap, tg) * p
    o3 = raw * armor_factor(raw, ap, tg) * p * r
    o4 = raw * p
    o4 = o4 * armor_factor(o4, ap, tg) * r
    print("O1 armor,resistance,protection %.6f" % o1)
    print("O2 resistance,armor,protection %.6f" % o2)
    print("O3 armor,protection,resistance %.6f (same as O1: factors commute) " % o3)
    print("O4 protection,armor,resistance %.6f" % o4)
    section("attack cooldown multiplier, attack_speed 1.6 (a sword: 4 - 2.4)")
    for t in range(0, 14):
        print("t", t, "%.6f" % cooldown_mult(t, 1.6), "charge %.4f" % min(1.0, (t + 0.5) / 12.5))
    section("fall damage floor((d - safe) * mult)")
    import math
    for d in (2.9, 3.0, 3.5, 4.0, 5.99, 6.0, 23.0, 25.5):
        print("distance", d, "damage", max(0, math.floor((d - 3.0) * 1.0)), "with safe 6 mult 0.5:", max(0, math.floor((d - 6.0) * 0.5)))
    section("periodic effects: interval fits (documented tables)")
    print("regeneration ticks per hp by level 1..7 for candidate 50 >> amplifier:", [max(1, 50 >> a) for a in range(7)])
    print("poison ticks per hp by level 1..7 for candidate 25 >> amplifier:", [max(1, 25 >> a) for a in range(7)])
    section("experience formulas: totals and next-level steps consistency")
    bad = [lv for lv in range(0, 300) if xp_total(lv + 1) - xp_total(lv) != xp_next(lv)]
    print("levels where total(L+1)-total(L) != next(L), L in 0..299:", bad)
    print("total at 1, 16, 17, 30, 31, 32, 100:", [xp_total(x) for x in (1, 16, 17, 30, 31, 32, 100)])
    section("difficulty scaling candidates (documented): easy min(D, D/2+1), normal D, hard 1.5 D")
    for d in (1.0, 2.0, 3.0, 4.0, 6.0, 8.0):
        print("damage", d, "easy", min(d, d / 2 + 1), "normal", d, "hard", 1.5 * d)
    section("order of difficulty and armor (damage 3, armor 20, toughness 0, hard)")
    print("difficulty then armor %.4f ; armor then difficulty %.4f" % (3 * 1.5 * armor_factor(3 * 1.5, 20, 0), 3 * armor_factor(3, 20, 0) * 1.5))
    section("window excess candidates (armor 20, toughness 0): first hit 10, second hit 14 inside the window")
    first = 10 * armor_factor(10, 20, 0)
    print("first hit applies %.4f" % first)
    print("excess on the raw amount then armor: %.4f ; difference of the reduced amounts: %.4f" % ((14 - 10) * armor_factor(14 - 10, 20, 0), 14 * armor_factor(14, 20, 0) - first))
    section("explosion damage candidate H-EXP1: i = (1 - dist/(2 p)) * exposure, damage = 7 p (i*i + i) + 1")
    for power in (1.0, 3.0, 4.0, 6.0):
        row = []
        for dist in (0.0, 1.0, 2.0, power, 2 * power - 0.5, 2 * power):
            i = max(0.0, (1 - dist / (2 * power)) * 1.0)
            row.append((dist, round(7 * power * (i * i + i) + 1, 4)))
        print("power", power, "exposure 1.0", row)
    section("explosion knockback magnitude candidate: (1 - dist/(2 p)) * exposure * (1 - resistance)")
    for dist in (0.0, 2.0, 4.0, 7.9):
        print("p 4 dist", dist, "magnitude %.6f" % ((1 - dist / 8.0) * 1.0), "with resistance 0.5: %.6f" % ((1 - dist / 8.0) * 0.5))
    section("hunger exhaustion bookkeeping example")
    ex = 0.0
    sat = 5.0
    food = 20
    steps = 0
    while food == 20 and steps < 100000:
        ex += 0.1
        if ex >= 4.0:
            ex -= 4.0
            if sat > 0:
                sat = max(0.0, sat - 1.0)
            else:
                food -= 1
        steps += 1
    print("sprint meters (0.1 each) from food 20 saturation 5 until food 19:", steps)


def cmd_player():
    section("recipes that carry an experience field (cooking)")
    rec = tree("recipe")
    withxp = {k: v for k, v in rec.items() if "experience" in v}
    print("recipes", len(rec), "with experience", len(withxp), sorted(collections.Counter(v["type"].split(":")[1] for v in withxp.values()).items()))
    print("experience values", sorted(collections.Counter(v["experience"] for v in withxp.values()).items()))
    section("speeds: documented blocks per second divided by 20 ticks (predictions and ratios)")
    walk, sprint, sneak, sprint_jump = 4.317, 5.612, 1.3, 7.127
    print("per tick walk %.5f sprint %.5f sneak %.5f sprint_jump %.5f" % (walk / 20, sprint / 20, sneak / 20, sprint_jump / 20))
    print("sprint / walk %.4f ; sneak / walk %.4f (the attribute sneaking_speed has default 0.3; 0.3 x 4.317 = %.4f, a value the pages do not give)" % (sprint / walk, sneak / walk, 0.3 * 4.317))
    print("minecart cap 8 b/s = %.3f b/tick ; boat water 8 b/s = %.3f ; boat ice 40 b/s = %.3f ; blue ice 72.72 b/s = %.3f" % (8 / 20, 8 / 20, 40 / 20, 72.72 / 20))
    section("jump height from jump_strength 0.42 and gravity 0.08, drag 0.98 (two update orders)")
    for label, fn in (("A", step_a), ("B", step_b)):
        y, v, top = 0.0, 0.42, 0.0
        for t in range(1, 40):
            y, v = fn(y, v, 0.08, 0.98)
            top = max(top, y)
        print("order", label, "apex %.6f" % top)
    y, v, n = 0.0, 0.42, 0
    while True:
        y, v = step_a(y, v, 0.08, 0.98)
        n += 1
        if y <= 0.0 and n > 1:
            break
    print("order A back to the start height after", n, "ticks")
    section("experience orb values: base values and a greedy split (largest first)")
    sizes = (2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1)
    for total in (1, 5, 7, 100, 1395, 5000):
        left, parts = total, []
        for sz in sizes:
            while left >= sz:
                parts.append(sz)
                left -= sz
        print("total", total, "->", parts)
    section("death drop and levels")
    for lv in (0, 1, 5, 10, 14, 15, 16, 20, 30, 100):
        print("level", lv, "drop min(7 * level, 100) =", min(7 * lv, 100), "points for next level", xp_next(lv))
    section("hunger bookkeeping candidates (exhaustion 4.0 reset or minus 4)")
    for label in ("minus", "reset"):
        ex, sat, food, n = 0.0, 5.0, 20, 0
        while food == 20 and n < 100000:
            ex += 0.1
            n += 1
            if ex >= 4.0:
                ex = ex - 4.0 if label == "minus" else 0.0
                if sat > 0:
                    sat = max(0.0, sat - 1.0)
                else:
                    food -= 1
        print(label, "meters until food 19:", n, "residual exhaustion %.6f" % ex)
        ex2 = 6.0
        ex2 = ex2 - 4.0 if label == "minus" else 0.0
        print(label, "single event of 6.0 exhaustion leaves %.1f and removes one saturation point" % ex2)
    section("eating: saturation capped by food level (examples with data values)")
    for food, sat, nut, satv in ((10, 2.0, 5, 6.0), (18, 5.0, 8, 12.8), (20, 20.0, 4, 2.4)):
        nf = min(20, food + nut)
        ns = min(nf, sat + satv)
        print("food", food, "saturation", sat, "eat nutrition", nut, "saturation value", satv, "->", nf, ns)


COMMANDS = {n[4:]: f for n, f in globals().items() if n.startswith("cmd_")}

if __name__ == "__main__":
    sub = sys.argv[2]
    COMMANDS[sub]()
```

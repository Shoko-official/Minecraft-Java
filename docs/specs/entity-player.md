# Entités : joueur, faim, expérience, déplacement, véhicules

Statut : brouillon
Jalon : M7
Risque : A
Règles : R1 à R74 (D 4, C 1, C sur S 8, D avec sens S 31, S 27, décisions de conception 3)

Quatrième document du jalon M7. Il décrit ce qui est propre au joueur (modes de jeu, attributs, mort et réapparition), la validation de son déplacement par le serveur, la faim, la saturation et l'épuisement, l'expérience, puis les véhicules (bateaux, wagonnets, montures) et les passagers. Presque tout ce qui est propre au joueur exige un client de protocole (jalon M8) ou le client réel (jalon M9) pour être observé ; les véhicules passifs (bateaux et wagonnets lancés avec une vitesse, chevaux sans cavalier) se comparent par traces sans client.

## Périmètre

Couvre :

- le joueur : attributs, modes de jeu, capacités, hauteur de pose, mort, point d'apparition, sommeil (R1 à R11) ;
- le déplacement : autorité, validation serveur, vitesses documentées, saut, accroupissement, nage (R12 à R25) ;
- la faim : variables, épuisement, régénération, famine, repas, effets (R26 à R41) ;
- l'expérience : niveaux, orbes, commandes, mort (R42 à R49) ;
- les véhicules et les passagers : types, étiquettes, bateaux, wagonnets, rails, montures (R50 à R71) ;
- le protocole de scénarios propre au joueur (R72 à R74).

Ne couvre pas :

- le modèle d'entité, la physique libre, la collision (`entity-model-physics.md`), les dégâts, les effets et le combat (`entity-damage-effects.md`), l'apparition et l'IA (`entity-ai-spawning.md`) ;
- les menus, l'inventaire, le crafting, les enchantements (`item-components-inventory.md`, `item-recipes.md`, `item-enchantments.md`) ;
- les élytres, la pêche, le tir à l'arc, la nage avancée (second lot), les portails, les dimensions (jalon M10) ;
- le rendu, l'audio, l'interface (jalon M9).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `generated/reports/registries.json` (entités, attributs, effets) ; `generated/reports/commands.json` (commandes `gamemode`, `experience`, `spawnpoint`, `ride`, `difficulty`, `effect`, `attribute`, `teleport`, `spreadplayers`, `execute on`, `gamerule max_minecart_speed`) ; `generated/reports/packets.json` (paquets de déplacement et de véhicule) ; `generated/reports/minecraft/components/item/*.json` (composants `food`, `consumable`, `equippable`) ; `generated/reports/blocks.json` (états des rails) ; `resources/data/minecraft/tags/entity_type/**`, `tags/damage_type/**`, `damage_type/*.json` (champ `exhaustion`).
- **Documentation publique**, consultée le 2026-10-28 (lecture partielle, extraction automatique ; toute valeur qui n'a que cette source reste S) : Minecraft Wiki "Hunger" (https://minecraft.wiki/w/Hunger : variables, épuisement, régénération, famine ; la page se contredit sur le retrait de 4.0), "Experience" (https://minecraft.wiki/w/Experience : formules, orbes, mort), "Sprinting" et "Transportation" (https://minecraft.wiki/w/Transportation : vitesses), "Boat" (https://minecraft.wiki/w/Boat), "Minecart" (https://minecraft.wiki/w/Minecart), "Entity" (https://minecraft.wiki/w/Entity), "Health", "Damage" (modes de jeu invulnérables), "Attribute".
- **Rapports locaux** : non consultés. Aucune valeur n'en provient.
- **Sondes de l'oracle** : aucune exécutée (EULA non acceptée). Sondes EP0 à EP12 dans `entity-player-vectors.md`. Les sondes EP0 à EP8 et EP12 exigent un client (jalon M8) ; EP7 exige à terme le client réel (jalon M9) ; EP9 à EP11 ont une partie sans client.
- **Spécs voisines** : `entity-model-physics.md` (R11 attributs, R38 à R45 physique, R79 scénario), `entity-damage-effects.md` (R13 exhaustion des types de dégâts, R57 effets, R88 expérience des mobs), `server-game-loop.md` (R15 gel et joueurs, R40 gamerules, R57 données de joueur), `net-protocol-777.md`, `item-components-inventory.md`.

Reproduction : `python -I ent_ref.py "$REF" <sous-commande>` avec `food`, `armor`, `damage`, `vehicles`, `player`, `calc`, `cmds`, `packets`, `registry` ; l'annexe et les empreintes sont dans `entity-model-physics-vectors.md`. Les calculs de `player` et de `calc` appliquent des formules S à des valeurs d'essai : ce sont des **prédictions** (C sur S).

Convention de preuve : D, C, C sur S, M (aucun), S, comme dans `entity-model-physics.md`. Hypothèses nommées H-EP. Chaque règle S porte son comportement provisoire et une sonde EP dont le critère de succès est dans le fichier de vecteurs.

## Règles

### Le joueur

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le joueur est le type `player` (D : présent dans le registre `entity_type` et dans les étiquettes `cannot_be_pushed_onto_boats` ; l'invocation par `summon` est S, EM1). Ses attributs de base diffèrent du type générique : la page "Attribute" donne le défaut générique `movement_speed` 0.7, alors que la vitesse de marche documentée du joueur est 4.317 blocs par seconde, soit 0.21585 par tick. Provisoire (H-EP1) : la base du joueur est un jeu d'attributs propre (santé, force d'attaque, vitesse de marche, portées, minage) à lire par `attribute @s <attribut> base get`. Sonde EP0. Commande : `ent_ref.py registry` et `ent_ref.py vehicles`. | D (type) / S |
| R2 | Clés propres du joueur lues par `data get entity` (S) : celles du fichier de joueur (`server-game-loop.md` R57 : `foodLevel`, `foodSaturationLevel`, `foodExhaustionLevel`, `foodTickTimer`, `playerGameType`, `Dimension`) plus, pour l'expérience, des clés dont le nom est relevé par EP0. Les valeurs initiales d'un nouveau joueur : santé 20, nourriture 20, saturation 5 (page "Hunger"), expérience 0. Provisoire. Sonde EP0. | S |
| R3 | Boîte du joueur selon la pose (page "Hitbox") : debout 0.6 x 1.8, yeux 1.62 ; accroupi 0.6 x 1.5 ; nage et vol plané 0.6 x 0.6 ; endormi 0.2 x 0.2 (`entity-model-physics.md` R22). Provisoire : voir `entity-model-physics.md` R22. Sonde EP7 (client). | S |
| R4 | Modes de jeu : survie, créatif, aventure, spectateur (argument `gamemode` de la commande `gamemode <mode> [<cible>]`, D ; commande `defaultgamemode` D). Page "Damage" : créatif et spectateur sont invulnérables à la plupart des dégâts. Provisoire (H-EP2) : créatif : invulnérable sauf types de `bypasses_invulnerability`, vol, casse instantanée, pas de faim ; spectateur : traverse les blocs, aucune interaction ; aventure : pas de casse sans outil adapté (hors M7). Sonde EP1. | D (commande) / S |
| R5 | Le paquet `player_abilities` (D, nom, deux sens) transporte les capacités (invulnérable, vol, vol autorisé, créatif instantané, vitesses de vol et de marche) ; le contenu exact est dans `net-protocol-777.md`. Provisoire : le serveur envoie `player_abilities` à chaque changement de mode. Sonde EP1. Commande : `ent_ref.py packets`. | D (nom) / S |
| R6 | Règles de jeu qui touchent le joueur (D, noms) : `keep_inventory`, `immediate_respawn`, `respawn_radius`, `natural_health_regeneration`, `pvp`, `locator_bar`, `show_death_messages`, `players_sleeping_percentage`, `players_nether_portal_default_delay`, `players_nether_portal_creative_delay`, `spectators_generate_chunks`, `player_movement_check`, `elytra_movement_check`. Défauts S : `server-game-loop.md` R40. Commande : `ent_ref.py registry`. | D (noms) |
| R7 | Point d'apparition : commande `spawnpoint [<cibles> [<position> [<rotation>]]]` (D, 4 formes) ; règle `respawn_radius` (D, entier, défaut 10 S). Provisoire (H-EP8) : à la mort le joueur réapparaît au point défini s'il est valide, sinon au point d'apparition du monde, dans un rayon de `respawn_radius` autour de ce dernier. Sonde EP12. | D (commande) / S |
| R8 | Mort du joueur : `Health` à 0 (spec des dégâts R4) ; les objets de l'inventaire tombent sauf si `keep_inventory` est vraie ; l'expérience tombe (R48) ; `immediate_respawn` saute l'écran de mort; le paquet `respawn` (D, nom) est envoyé. Provisoire. Sonde EP12. Commande : `ent_ref.py registry`. | D (règles, nom) / S |
| R9 | Sommeil : la règle `players_sleeping_percentage` (D, entier 0 et plus, défaut 100 S) fixe la part de joueurs endormis qui fait passer la nuit (phase TK4.d, `server-game-loop.md` R19, jalon M7). Provisoire : la nuit passe quand la part de joueurs endormis atteint la règle; le détail (durée du lit, réveil) est du second lot. Sonde EP12. Commande : `ent_ref.py registry`. | D (règle) / S |
| R10 | Collision entre joueurs et entités : un joueur est poussé par d'autres entités (depuis 15w36a, page "Entity") ; étiquette `cannot_be_pushed_onto_boats` contient `player` (D). Provisoire. Sonde EP9. Commande : `ent_ref.py vehicles`. | D (étiquette) / S |
| R11 | Un joueur peut être ciblé par les commandes `damage`, `effect`, `attribute`, `experience`, `gamemode`, `teleport`, `rotate`, `spawnpoint`, `ride` (D, formes de `ent_ref.py cmds`) ; il ne peut pas être modifié par `data merge entity` (hypothèse H-EP9, la commande refuse les joueurs). Sonde EP0. Provisoire : H-EP9 ; sonde EP0. | D (formes) / S |

### Déplacement

| Réf | Règle | Preuve |
|---|---|---|
| R12 | Autorité : le client calcule le mouvement du joueur et le serveur le valide. Paquets (D, noms) : client vers serveur `move_player_pos`, `move_player_pos_rot`, `move_player_rot`, `move_player_status_only`, `player_input`, `player_command`, `accept_teleportation`, `teleport_to_entity` ; serveur vers client `player_position`, `player_rotation`, `player_look_at`, `teleport_entity`, `entity_position_sync`, `set_entity_motion`. Provisoire (H-EP10) : le serveur accepte la position annoncée quand elle respecte la validation (R13), sinon renvoie le joueur à la dernière position valide par `player_position` et attend `accept_teleportation`. Sonde EP7. Commande : `ent_ref.py packets`. | D (noms) / S |
| R13 | Validation : les règles `player_movement_check` et `elytra_movement_check` (D) désactivent respectivement la vérification du déplacement et celle des élytres. Seuils (vitesse maximale tolérée par tick, tolérance de collision) : non lus. Provisoire : mesurer la vitesse annoncée la plus élevée acceptée, par pas de 0.01 bloc par tick, marche, sprint, chute, vol. Sonde EP7. Commande : `ent_ref.py registry`. | D (règles) / S |
| R14 | Vitesses de déplacement documentées (page "Transportation", "Sprinting"), en blocs par seconde : marche 4.317, course 5.612, accroupi 1.295 (page "Transportation" : 1.3), course avec saut (moyenne) 7.127, nage en surface 2.20, nage sous l'eau 1.97, vol créatif 11.0 horizontal et 22.0 en course ; bateau sur la glace 40, wagonnet 8, cheval moyen 9.49 (le plus rapide 14.57), élytres 30 à 0 degré d'inclinaison. Par tick (division par 20, C sur S) : marche 0.21585, course 0.28060, accroupi 0.06475, course avec saut 0.35635. Commande : `ent_ref.py player`. Provisoire : ces valeurs servent à borner les mesures de EP7, pas de référence. Sonde EP7. | C sur S |
| R15 | Rapports (C sur S) : course / marche = 1.3000 (page "Sprinting" : « 30 pour cent plus rapide ») ; accroupi / marche = 0.3000, égal à la valeur par défaut de l'attribut `sneaking_speed` (0.3, page "Attribute"). Provisoire (H-EP4) : la course est un modificateur de `movement_speed` de montant +0.3 en `add_multiplied_total` ; l'accroupissement multiplie par `sneaking_speed`. Sonde EP7 (lecture de `attributes` pendant la course). Commande : `ent_ref.py player`. | C sur S |
| R16 | Saut : l'attribut `jump_strength` (défaut 0.42, [0, 32]) donne la vitesse verticale initiale. Prédiction (C sur S, `ent_ref.py player`) avec gravité 0.08 et traînée 0.98 : l'ordre A de la spec du modèle (R40) donne une hauteur d'apogée de 1.252203 bloc et un retour à la hauteur de départ après 12 ticks ; l'ordre B donne 0.832203. Provisoire : le joueur suit l'ordre A (la page "Entity" l'indique pour les entités vivantes). Sonde EP7. | C sur S |
| R17 | Saut en course : ajoute une impulsion horizontale (valeur non lue ; effet décrit par la vitesse moyenne 7.127 b/s de R14). Provisoire : mesurer l'impulsion par tick de saut. Sonde EP7. | S |
| R18 | Accroupissement : hauteur 1.5, vitesse de R15, et le joueur ne tombe pas du bord d'un bloc (« bord sûr ») sur une hauteur inférieure à `step_height` ; s'il n'y a pas assez de place au-dessus, il reste accroupi. Provisoire (H-EP11) : le bord sûr est un test de collision à la position suivante qui supprime la composante horizontale quand l'appui disparaît. Sonde EP7. | S |
| R19 | Nage : pose de hauteur 0.6, vitesses de R14, modulées par `water_movement_efficiency` (défaut 0, [0, 1]) et `movement_efficiency` ; apnée et noyade : spec des dégâts R39. Provisoire. Sonde EP7. | S |
| R20 | Échelles et lianes : étiquette `climbable` (9 blocs, D) ; vitesse de montée et de descente à mesurer. Provisoire. Sonde EP7. Commande : `ent_ref.py btags`. | D (étiquette) / S |
| R21 | Vol (créatif, spectateur) : vitesse 11.0 b/s horizontale, 22.0 en course (R14) ; le serveur n'applique pas la gravité à un joueur en vol autorisé. Provisoire : vitesses de `player_abilities`. Sonde EP1. | S |
| R22 | Course interdite quand la nourriture est de 6 ou moins ; réautorisée au-dessus de 7 (page "Hunger") ; l'état de course est annoncé au serveur par `player_command`. Provisoire. Sonde EP2. | S |
| R23 | Sous `tick freeze`, les joueurs continuent à recevoir leur tick (`server-game-loop.md` R15) : leur position progresse avec leurs entrées pendant que les autres entités sont figées. Provisoire. Sonde EP7. | S |
| R24 | Poussée par les entités et par les explosions (spec des dégâts R83) : le joueur reçoit les impulsions par `set_entity_motion` (D, nom) ; l'impulsion est ajoutée à la vitesse du client. Provisoire. Sonde EP7. Commande : `ent_ref.py packets`. | D (nom) / S |
| R25 | Un joueur en spectateur traverse les blocs et n'a pas de collision d'entité ; il peut s'accrocher à une entité (`teleport_to_entity`, commande `spectate`, D). Provisoire. Sonde EP1. | D (commande) / S |

### Faim, saturation, épuisement

| Réf | Règle | Preuve |
|---|---|---|
| R26 | Variables (page "Hunger") : nourriture entière de 0 à 20 (20 au départ), saturation flottante qui ne dépasse jamais la nourriture (5 au départ), épuisement flottant. Clés (server-game-loop R57, S) `foodLevel`, `foodSaturationLevel`, `foodExhaustionLevel`, `foodTickTimer`. Provisoire. Sonde EP2. | S |
| R27 | Comptabilité de l'épuisement : quand il atteint 4.0, il est retiré (de 4.0, ou remis à 0 : la page se contredit) puis la saturation baisse de 1, ou la nourriture de 1 si la saturation est nulle. Provisoire : deux candidats, H-EP5a (retrait de 4.0) et H-EP5b (remise à 0). Un événement unique de 6.0 laisse 2.0 sous H-EP5a et 0.0 sous H-EP5b (`ent_ref.py player`, section faim) ; les séquences de 0.1 ne les distinguent pas. Sonde EP2, critère : après un tick de régénération naturelle (6.0), lire `foodExhaustionLevel`. | C sur S |
| R28 | Sources d'épuisement (page "Hunger") : nage 0.01 par mètre, casse d'un bloc 0.005, course 0.1 par mètre, saut 0.05, saut en course 0.2, attaque d'une entité 0.1, dégât normalement protégé par l'armure 0.1, régénération naturelle 6.0 par point de vie. La marche n'est pas listée (hypothèse H-EP6 : 0). Provisoire : appliquer. Sonde EP2. | S |
| R29 | Épuisement par les dégâts : la page parle des dégâts protégés par l'armure ; la donnée est le champ `exhaustion` de chaque type de dégâts (0.0 pour 19 types, 0.1 pour 32, croisement C avec `bypasses_armor` dans `entity-damage-effects.md` R13). Provisoire (H-EP12) : un joueur blessé (dégât appliqué par la chaîne de réduction) gagne l'épuisement de son type. Sonde EP4. Commande : `ent_ref.py damage`. | D (champ) / S |
| R30 | Régénération naturelle (page "Hunger" et "Health", contradictoires sur le détail) : nourriture au moins 18 et règle `natural_health_regeneration` vraie : 1 point de vie toutes les 80 ticks (4 s), coût 6.0 d'épuisement; à nourriture 20 avec saturation positive : 1 point de vie tous les 10 ticks (0.5 s), coût de saturation 1.5 par point (« saturation boost »). Provisoire (H-EP7) : les deux modes, le second prioritaire; `foodTickTimer` est le compteur. Sonde EP3. Commande : `ent_ref.py registry`. | D (règle) / S |
| R31 | Famine : nourriture à 0 : 1 point de dégâts tous les 80 ticks, type `starve` (contourne l'armure et les effets, D) ; Facile s'arrête à 10 points de vie, Normal à 1, Difficile sans plancher (page). Provisoire. Sonde EP3. Commande : `ent_ref.py damage`. | D (type) / S |
| R32 | Pacifique : la nourriture ne baisse pas et reste à 20 (page). Provisoire. Sonde EP3. | S |
| R33 | Repas : 44 objets portent un composant `food` (D) : `nutrition` entier de 1 à 10, `saturation` flottante de 0.2 à 14.4 (16 valeurs distinctes), `can_always_eat` pour 5 d'entre eux. Exemples : pain 5 et 6.0 ; boeuf cuit 8 et 12.8 ; pomme 4 et 2.4 ; chair putréfiée 4 et 0.8 ; pomme dorée 4 et 9.6 avec `can_always_eat`. Provisoire (H-EP13) : manger ajoute `nutrition` à la nourriture (plafond 20) et `saturation` à la saturation, plafonnée à la nourriture finale (C sur S : nourriture 10, saturation 2.0, pain donne 15 et 8.0 ; nourriture 18, saturation 5.0, boeuf cuit donne 20 et 17.8). Sonde EP5. Commande : `ent_ref.py food`. | D (valeurs) / S |
| R34 | On ne mange pas à nourriture 20 en survie sauf `can_always_eat` (page "Hunger" : pomme dorée, pomme dorée enchantée, fruit de chorus, bouteille de miel, ragoût). Provisoire. Sonde EP5. Commande : `ent_ref.py food`. | D (champ) / S |
| R35 | Durée de consommation : 43 objets ont un composant `consumable` (D) ; 38 le gardent à ses valeurs par défaut (`{}` ou seulement des effets de consommation), `dried_kelp` fixe `consume_seconds` 0.8, `honey_bottle` fixe 2.0 secondes avec animation de boisson, `milk_bucket`, `ominous_bottle` et `potion` ont l'animation de boisson sans particules. La durée par défaut est S. Provisoire : mesurer le délai entre le début de l'usage et l'effet. Sonde EP5. Commande : `ent_ref.py food`. | D (champs) / S |
| R36 | Effet Faim : ajoute de l'épuisement périodiquement (intervalle illisible dans l'extraction) ; effet Saturation : par niveau, restitue de la nourriture et de la saturation à chaque tick (page "Hunger" : 1 de nourriture et 2 de saturation par niveau et par tick). Provisoire : mesurer le débit d'épuisement par niveau et les restitutions. Sonde EP6. | S |
| R37 | Les effets de consommation (D, `food`) : pomme dorée : régénération niveau II 100 ticks et absorption 2400 ticks ; chair putréfiée : faim 600 ticks avec probabilité 0.8 ; autres dans `ent_ref.py food`. Provisoire : appliquer par l'effet de la spec des dégâts R63. Sonde EP5. | D (valeurs) / S |
| R38 | Un repas rapporte aussi à d'autres entités (`villager_food` : nutrition 4 pour le pain...) : second lot. Commande : `ent_ref.py food`. | D (champ) |
| R39 | La nourriture n'agit pas sur les modes créatif et spectateur : pas de baisse. Provisoire (H-EP14). Sonde EP1. | S |
| R40 | Mort et réapparition : nourriture 20, saturation 5, épuisement 0 (page "Hunger", création et réapparition). Provisoire. Sonde EP12. | S |
| R41 | Dégâts de faim sur Facile et Normal en présence d'effets : le plancher s'applique à la santé effective (absorption exclue, hypothèse H-EP15). Sonde EP3. Provisoire : H-EP15. Sonde EP3 (déjà citée). | S |

### Expérience

| Réf | Règle | Preuve |
|---|---|---|
| R42 | Formules (page "Experience") : points totaux pour atteindre le niveau N : `N^2 + 6N` (N de 0 à 16), `2.5 N^2 - 40.5 N + 360` (17 à 31), `4.5 N^2 - 162.5 N + 2220` (32 et plus) ; points pour passer du niveau N au suivant : `2N + 7` (0 à 15), `5N - 38` (16 à 30), `9N - 158` (31 et plus). Contrôle (C sur S) : pour N de 0 à 299 la différence des totaux est égale au pas, sans exception ; niveau 1 : 7, niveau 16 : 352, niveau 30 : 1395, niveau 100 : 30970 (`ent_ref.py calc`). Provisoire : appliquer; la valeur de progression (fraction du niveau) est flottante. Sonde EP8. | C sur S |
| R43 | Commande `experience` (D, 8 formes) : `add <cible> <montant> [levels ou points]`, `set <cible> <montant 0..> [levels ou points]`, `query <cible> levels` et `query <cible> points` ; `xp` est une redirection vers `experience` (D). Provisoire (H-EP16) : `add ... levels` conserve la fraction de progression; `set ... levels` la remet à zéro; `points` ajoute aux points totaux. Sonde EP8. | D (formes) / S |
| R44 | Orbes (page "Experience") : onze classes de valeur (1, 3, 7, 17, 37, 73, 149, 307, 617, 1237, 2477 comme bases) ; une récompense est découpée en commençant par les plus grosses valeurs. Contrôle (C sur S, `ent_ref.py player`) : 100 points donnent des orbes de 73, 17, 7, 3 ; 1395 points : 1237, 149, 7, 1, 1. Provisoire : appliquer ce découpage glouton. Sonde EP8. | C sur S |
| R45 | Attraction et ramassage (page) : une orbe à moins de 7.25 blocs du joueur dérive vers lui en accélérant; le ramassage se fait à raison d'une orbe à la fois, 10 par seconde; la toile ralentit; l'eau courante pousse. Provisoire : appliquer; vitesse d'attraction à mesurer. Sonde EP8. | S |
| R46 | Vie d'une orbe : disparition à 6000 ticks (5 minutes); fusion d'orbes de même valeur; la fusion ne crée pas de valeur plus grande (page). Boîte 0.5 x 0.5, gravité 0.03, traînée 0.98 (spec du modèle R21, R75). Provisoire. Sonde EP8. | S |
| R47 | Score : total d'expérience gagné depuis la dernière mort, sans baisse à la dépense (page). Provisoire. Sonde EP8. | S |
| R48 | Mort : le joueur lâche des orbes pour `min(7 x niveau, 100)` points; le reste est perdu (page) ; avec `keep_inventory` vraie, l'expérience est gardée. Exemples (C sur S) : niveau 5 : 35 ; niveau 14 : 98 ; niveau 15 et plus : 100. Provisoire. Sonde EP12. Commande : `ent_ref.py player`. | C sur S |
| R49 | Les recettes de cuisson portent un champ `experience` (D : 116 recettes sur 4 types : fusion 73, haut fourneau 25, feu de camp 9, fumoir 9 ; valeurs 0.1, 0.15, 0.2, 0.3, 0.35, 0.7, 1.0, 2.0). Provisoire (H-EP21) : retirer le résultat d'un fourneau crée des orbes pour la somme de `experience` des recettes exécutées, la partie fractionnaire étant tirée au hasard (valeur à confirmer); l'expérience des mobs : `entity-damage-effects.md` R88. Sonde EP8 (client, fourneau). Commande : `ent_ref.py player`. | D (champ) / S |

### Véhicules et passagers

| Réf | Règle | Preuve |
|---|---|---|
| R50 | Types (C, `ent_ref.py vehicles`) : 7 types de wagonnet (`minecart`, `chest_minecart`, `furnace_minecart`, `hopper_minecart`, `tnt_minecart`, `spawner_minecart`, `command_block_minecart`), 22 types de bateau ou radeau dont 11 avec coffre; l'étiquette `boat` en liste 11 (sans coffre). | C |
| R51 | Étiquettes de monture et de passager (D) : `can_equip_saddle` 11 types (chameau, chameau-husk, âne, cheval, mule, nautile, cochon, cheval squelette, strider, cheval zombie, nautile zombie) ; `can_float_while_ridden` 6 ; `dismounts_underwater` 13 ; `cannot_be_pushed_onto_boats` 14 (dont `player`) ; `can_turn_in_boats` 1 (`breeze`) ; `non_controlling_rider` 3 (`magma_cube`, `slime`, `sulfur_cube`) ; `can_equip_harness` 1 (`happy_ghast`) ; `cannot_be_dismounted_by_item_usage` 1 (`interaction`). Provisoire : le moteur lit les étiquettes. Commande : `ent_ref.py vehicles`. | D |
| R52 | Passagers : clé `Passengers` (liste d'entités, page "Entity format") ; paquets `set_passengers`, `set_entity_link` (D, noms) ; commande `ride <cible> mount <véhicule>` et `ride <cible> dismount` (D) ; sélecteurs `execute on passengers`, `on vehicle`, `on controller` (D). Provisoire (H-EP17) : un passager n'a pas de mouvement propre; sa position suit celle du véhicule avec un décalage propre au type de véhicule et de passager. Sonde EP9. | D (formes) / S |
| R53 | Capacité : un bateau porte 2 passagers, un wagonnet 1 (pages "Boat" et "Minecart"). Provisoire. Sonde EP10, EP11. | S |
| R54 | Montage et démontage : on monte en utilisant le véhicule; les mobs montent le bateau en entrant en collision avec son côté; on descend en s'accroupissant; le wagonnet cherche parmi huit blocs adjacents un emplacement sûr et éjecte le passager d'un bloc vers le haut (page "Minecart"). Provisoire. Sonde EP11. | S |
| R55 | Bateau, mouvement passif (page "Entity") : gravité 0.04 par tick, traînée 0.90 (Java), vitesse terminale 9 (valeur douteuse : le point fixe d'une chute avec ces constantes serait 0.36 selon la spec du modèle R39; l'écart est signalé). Provisoire : mesurer la chute libre d'un bateau lâché dans l'air. Sonde EP10. | S |
| R56 | Bateau, vitesses (page "Boat") : sur l'eau 0.4 bloc par tick (8 b/s), lacet maximal 10 degrés par tick ; sur la glace, la glace compacte, la glace gelée et la glace fragile 2 blocs par tick (40 b/s), lacet 50 degrés par tick ; sur la glace bleue 3.63 blocs par tick (72.72 b/s) ; accélération avant 0.04 par tick au carré, arrière 0.005 ; accélération de rotation 1 degré par tick au carré. Cohérence (C sur S, `ent_ref.py player`) : 8 b/s = 0.4 par tick, 40 b/s = 2.0 (valeur de la page "Transportation"), 72.72 b/s = 3.636. Provisoire : appliquer; avec un pilote (client) uniquement. Sonde EP10. | C sur S |
| R57 | Bateau, flottaison : il flotte sur l'eau immobile ou courante; il coule dans une eau qui coule vers le bas et ne remonte que par une colonne de bulles ou en le cassant; il annule les dégâts de chute de ses passagers (page "Boat"). Étiquettes de bulles (D) : `enables_bubble_column_push_up`, `enables_bubble_column_drag_down` (1 bloc chacune). Provisoire. Sonde EP10. Commande : `ent_ref.py btags`. | D (étiquettes) / S |
| R58 | Dégâts d'un véhicule (pages "Boat", "Minecart") : chaque point de dégât ajoute 10 à un compteur qui baisse de 1 par tick; au-dessus de 40 le véhicule est détruit et lâche son objet; une attaque de plus de 4 points le détruit d'un coup; le cactus, la lave et le feu aussi. Provisoire : la commande `damage` s'applique aux véhicules (hypothèse H-EP18; sinon client). Sonde EP10, EP11. | S |
| R59 | Wagonnet, vitesse (page "Minecart") : 8 blocs par seconde par axe (11.314 en diagonale), plafonnée par la règle `max_minecart_speed` (D : entier de 1 à 1000, défaut 8 S). Contrôle (C sur S) : 8 / 20 = 0.4 par tick, valeur de la page "Entity" (vitesse horizontale maximale 0.4) ; hypothèse H-EP19 : plafond par tick = règle / 20. Hors rails : demi-vitesse (4 b/s). Sonde EP11. Provisoire : H-EP19. Sonde EP11 (déjà citée). Commande : `ent_ref.py vehicles` et `ent_ref.py cmds`. | D (règle) / C sur S |
| R60 | Rails (D, `blocks.json`) : `rail` : propriétés `shape` (10 valeurs : `north_south`, `east_west`, quatre `ascending_*`, quatre courbes) et `waterlogged` : 20 états ; `powered_rail`, `detector_rail`, `activator_rail` : `powered`, `shape` (6 valeurs, sans courbes), `waterlogged` : 24 états chacun. Étiquette `rails` : les 4 blocs (D). Le mouvement sur rail est contraint par la forme; la logique des rails (forme, alimentation) relève du jalon M5. Provisoire : le wagonnet suit la forme du rail sous lui. Sonde EP11. Commande : `ent_ref.py vehicles`. | D (états) / S |
| R61 | Wagonnet, comportement : pente sans rail alimenté (descente seule); rails alimentés accélèrent; rails de détection; pickup de mobs par un wagonnet en mouvement dans une boîte élargie de 0.2 bloc sur x et z (sauf chauve-souris et golem de fer, dragon, gardien, wither) (page "Minecart"). Provisoire : mesurer la loi d'accélération et de décélération par tick. Sonde EP11. | S |
| R62 | Wagonnet, mouvements : paquets `move_minecart_along_track` et `move_vehicle` (D, noms). Provisoire : le serveur envoie `move_minecart_along_track` pour un wagonnet sur rail. Sonde EP11 (client). Commande : `ent_ref.py packets`. | D (noms) / S |
| R63 | Types de wagonnet à fonction (7 types, D) : coffre et entonnoir (inventaires : `item-components-inventory.md`), four, dynamite (explosion à l'impact), générateur, bloc de commande. Provisoire : seulement le mouvement et la collision dans M7. Sonde EP11. | D (types) / S |
| R64 | Montures : un joueur monte un cheval, âne, mule, chameau, cochon, strider (selle, étiquette `can_equip_saddle`) ; la direction suit le regard, la vitesse suit `movement_speed` de la monture, le saut suit `jump_strength` (attributs D). Provisoire (H-EP20) : une monture sans selle ne se contrôle pas; les types de `non_controlling_rider` ne contrôlent pas. Sonde EP9. Commande : `ent_ref.py vehicles`. | D (étiquettes) / S |
| R65 | Chevaux sans cavalier : mouvement passif de `entity-model-physics.md` ; saut : l'attribut `jump_strength` donne la vitesse verticale initiale (défaut 0.42, la monture le redéfinit : table EM3). Provisoire. Sonde EP9 (cheval `NoAI` poussé par `Motion`). | S |
| R66 | Les types de `dismounts_underwater` (13) quittent leur cavalier sous l'eau; ceux de `can_float_while_ridden` (6) flottent avec un cavalier. Provisoire. Sonde EP9. Commande : `ent_ref.py vehicles`. | D (étiquettes) / S |
| R67 | Les entités de l'étiquette `cannot_be_pushed_onto_boats` ne sont pas poussées dans un bateau par collision. Provisoire. Sonde EP10. Commande : `ent_ref.py vehicles`. | D (étiquette) / S |
| R68 | Chute avec véhicule : un joueur dans un bateau ne subit pas de chute; sur un rail il n'en subit pas non plus (page "Minecart") ; un cavalier subit la chute de sa monture. Provisoire. Sonde EP9, EP10, EP11. | S |
| R69 | Un véhicule est une entité non vivante : pas de santé, pas d'attributs (`attribute` échoue), `damage` selon H-EP18. Provisoire : table EM1. Sonde EM1 de la spec du modèle. | S |
| R70 | Les véhicules lancés avec `Motion` se comparent par trace sans client (bateau sur l'eau et la glace, wagonnet sur rail avec vitesse initiale); le pilotage exige un client. Provisoire. Sondes EP10, EP11. | S |
| R71 | L'entité `interaction` (étiquette `cannot_be_dismounted_by_item_usage`, D) et les types de décor ne sont pas des véhicules; le transport par un véhicule d'une entité `player` suit R52. Commande : `ent_ref.py vehicles`. | D (étiquette) |

### Scénarios du joueur

| Réf | Règle | Preuve |
|---|---|---|
| R72 | Décision de conception : un scénario de joueur est un **script d'entrées par tick** (avance, recul, gauche, droite, saut, accroupi, course, lacet, tangage, utilisation) envoyé par le client de protocole; la trace relevée est celle du serveur (`data get entity @p Pos`, `Motion`, `foodLevel`...) à chaque tick. Les comparaisons de cinétique du joueur ne sont possibles qu'avec le client réel, car le calcul de la position est côté client. | S (décision) |
| R73 | Décision de conception : sous `tick freeze`, un scénario de joueur avance avec `tick step`; les entités gelées ne bougent pas, le joueur oui (R23). | S (décision) |
| R74 | Décision de conception : les vitesses, hauteurs et délais documentés ne servent pas de référence de test; seule la comparaison du moteur à l'oracle sur la même mesure fait foi. | S (décision) |

## Entrées / sorties

- **Entrée d'un scénario de joueur** : script d'entrées (R72) ; **sortie** : trace côté serveur.
- **Faim** : entrée = (actions du tick : distance, sauts, attaques, dégâts, repas, difficulté, règles) ; sortie = (nourriture, saturation, épuisement, santé).
- **Expérience** : entrée = (gains en points, niveaux, mort) ; sortie = (niveau, progression, total, orbes).
- **Véhicule** : entrée = (vitesse initiale, entrées du pilote, bloc sous le véhicule) ; sortie = trace ETS.
- **Unités** : blocs par tick, points de vie, ticks, secondes pour les commandes. **Coordonnées négatives** : aucune dépendance de signe sauf les coordonnées des rails et les blocs de friction.

## Cas limites

- Nourriture 6 puis 7 (seuil de course); 17 puis 18 (seuil de régénération); 0 en Facile avec 10 points de vie.
- Saturation égale à la nourriture; épuisement exactement 4.0 et 6.0.
- Repas de nourriture 19 avec saturation au plafond; aliment `can_always_eat` à nourriture 20.
- Niveaux 15, 16, 17, 30, 31, 32 pour les formules; expérience négative refusée par `experience set` (D : minimum 0).
- Orbe de valeur 2477 et plus; 32767 (page : plus grande classe).
- Bateau sur glace bleue; bateau sur une dalle de glace entre deux blocs d'eau.
- Wagonnet à 1000 pour `max_minecart_speed`; rail en courbe à grande vitesse; rail en pente.
- Passager de passager; monture qui meurt; passager qui change de dimension.

## Vecteurs de test

`entity-player-vectors.md` : V-EP1 (aliments et armures, D), V-EP2 (vitesses, saut, C sur S), V-EP3 (expérience, C sur S), V-EP4 (véhicules, D/C), V-EP5 (comptabilité de la faim, C sur S) ; sondes EP0 à EP12 ; couverture.

## Plan de tests

- **Unitaire (L1)** : table des aliments (R33) et leurs effets (R37) ; formules d'expérience (R42) et découpage en orbes (R44) ; machine d'épuisement pour les deux candidats (R27) ; étiquettes de véhicule et de monture (R51) ; états de rails (R60) ; calcul du plafond de vitesse d'un wagonnet (R59).
- **Différentiel (L2)** : traces de bateau et de wagonnet sans client (EP10, EP11) ; faim et expérience par client (EP2 à EP8) ; mort et réapparition (EP12).
- **Fonctionnel (L4)** : un parcours d'un joueur simulé (marche, course, saut, repas, montée d'un bateau et d'un cheval) comparé au client réel.
- **Niveau A** : tests écrits en aveugle par le `test-engineer`, relecture par un second agent.

## Questions ouvertes

- Attributs de base du joueur (R1) : à lire (EP0) ; la page ne donne que les défauts génériques.
- Cinétique du joueur (accélération, saut en course, nage, échelle) (R16 à R20) : exige le client réel; sans lui, seule la validation serveur est mesurable.
- Candidats de la comptabilité d'épuisement et de la régénération (R27, R30) : EP2, EP3.
- Seuils de validation du déplacement (R13) : EP7.
- Fonctionnement de `damage` et de `attribute` sur les véhicules (R58, R69).
- Valeurs de gravité et de traînée d'un bateau (R55) : la page est ambiguë.
- Wagonnets : lois de pente, d'accélération et de collisions avec les mobs (R59 à R63) : mesure par EP11 ; la fonction des rails (M5) est un prérequis.
- Bloquants possibles pour 2a-bis : aucun démontré; les points durs sont le client réel (L3/M9) et un client de protocole (M8).

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Joueur : modes, attributs, mort, réapparition | M7 | A | spécifié | `docs/specs/entity-player.md` R1 à R11 (S, EP0, EP1, EP12) |
| Déplacement du joueur | M7 | A | spécifié | idem R12 à R25 (C sur S/S, EP7) |
| Faim, saturation, repas | M7 | A | spécifié | idem R26 à R41 (D/S, EP2 à EP6) |
| Expérience | M7 | A | spécifié | idem R42 à R49 (C sur S/S, EP8, EP12) |
| Véhicules, rails, montures | M7 | A | spécifié | idem R50 à R71 (D/C/S, EP9 à EP11) |

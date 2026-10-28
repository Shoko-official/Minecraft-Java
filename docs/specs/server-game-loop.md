# Boucle de jeu du serveur, gamerules et persistance

Statut : brouillon
Jalon : M8
Risque : B
Règles : R1 à R78, numéros regroupés par thème (R1-R9 cadence, R10-R18 `/tick`, R19-R22 phases, R30-R34 temps, R40-R46 gamerules, R50-R65 persistance, R70-R78 configuration).

## Périmètre

Couvre, pour `server` (et ses interfaces avec `engine/*`) :

- la cadence du serveur : 20 ticks par seconde, retard, rattrapage, pause, arrêt, chien de garde ;
- l'ordre des phases d'un tick ;
- la commande `/tick` (état, gel, pas, sprint, cadence) et la synchronisation de l'état de tick avec les clients ;
- les gamerules : registre, types, bornes, valeurs par défaut, commande `/gamerule`, stockage, synchronisation ;
- le temps de jeu : horloges, ligne de temps (jour/nuit), âge du monde ;
- la persistance côté serveur : `level.dat`, fichiers de `data/`, données de joueur, statistiques, sauvegarde automatique, `save-all`, `save-off`, `save-on`, verrou de session, arrêt propre ;
- les fichiers de configuration du serveur : `server.properties`, `ops.json`, `whitelist.json`, listes de bannissement.

Ne couvre pas :

- le protocole réseau, l'authentification et le mode hors ligne : `net-protocol-777.md` ;
- la sémantique des commandes autres que `/tick`, `/gamerule`, `/save-*`, `/stop` et `/time` (voir `server-commands.md`) ;
- le format Anvil et la disposition générale des dossiers d'un monde : `world-anvil-region.md` (R1 à R23), le NBT : `core-nbt.md` ;
- le contenu des tick de blocs, fluides, entités (jalons M5 à M7) : seul l'ordre entre phases est décrit ici ;
- la mise à niveau d'un monde d'une version antérieure, RCON, Query, protocole de gestion du serveur.

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `generated/reports/registries.json` (registre `minecraft:game_rule`, 59 entrées, identifiants 0 à 58) ; `generated/reports/commands.json` (arbre de `/tick`, `/gamerule`, `/time`, `/save-all`, `/stop` : formes, types, bornes, permissions) ; `resources/assets/minecraft/lang/en_us.json` (textes et clés des messages) ; `resources/data/minecraft/world_clock/*.json`, `timeline/*.json`, `dimension_type/*.json` ; `version.json` (data version 5023, protocole 777).
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Tick" (cadence, ordre des phases du serveur Java, ticks aléatoires), "Commands/tick" (syntaxe, résultats, ajouts), "Game rule" (liste, valeurs par défaut, renommage en identifiants), "Java Edition level format" (disposition des dossiers, `level.dat`, déplacement des champs vers `data/`), "Player.dat format", "Server.properties", "Ops.json format", "Whitelist.json", "Permission level". Les pages ont été lues par un outil de résumé automatique : à relire sur la source.
- **Rapports locaux** : `rapport_architecture_minecraft.md` chapitre 3 (cadence de 20 ticks, budget de 50 ms, TPS et MSPT, pseudo-code d'ordre explicitement "choisi pour l'explication", ne reproduit pas l'ordre exact) et chapitre 10 (sauvegarde, déchargement). Indices seulement : le rapport dit lui-même que l'ordre exact doit être vérifié dans la version étudiée. Aucune règle ci-dessous n'en vient seule.
- **Calculs** : tableaux de la section Vecteurs, reproductibles avec `cmd_ref.py` (annexe de `server-commands-vectors.md`) et l'arithmétique donnée.
- **Sondes de l'oracle** : aucune exécutée (EULA non acceptée). PG1 à PG10 et PD1 à PD8 décrites plus bas.

Niveaux de preuve : **D** = valeur lue dans un fichier extrait ; **C** = calcul reproductible à partir de règles D ; **M** = non utilisé ; **S** = documentation publique seule, rapport ou hypothèse. Une règle S énonce son comportement **provisoire** et sa sonde.

## Règles

### Cadence, retard et arrêt

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le serveur avance par pas de temps de jeu, les ticks, à la cadence cible par défaut de 20 par seconde, soit 50 ms par tick. | S |
| R2 | La cadence cible se règle par `/tick rate <rate>` : flottant de 1.0 à 10000.0 inclus (arbre de commandes). Intervalle entre deux débuts de tick = 1000 / cadence millisecondes (cadence 20 : 50 ms ; 10000 : 0,1 ms ; 1 : 1000 ms). Vecteur T01. | D (plage) / C |
| R3 | Le temps de jeu est compté en ticks, pas en secondes : deux actions espacées de 20 ticks restent à 20 ticks quelle que soit la durée réelle ; un serveur lent ralentit le jeu, pas l'horloge de jeu. La cadence réelle atteinte (TPS) est `min(cadence cible, 1000 / MSPT)` où MSPT est le temps de calcul moyen d'un tick en millisecondes. Vecteur T03. | S (rapport 3.1, 3.4) / C |
| R4 | Retard : si le serveur a plus de 2 s (2000 ms) de retard sur son échéance, il remet l'échéance suivante à "maintenant" au lieu de rattraper : les ticks perdus ne sont pas rejoués. Provisoire : seuil 2000 ms, ticks perdus non rejoués, un avertissement est journalisé avec le retard en ms et en ticks (texte non vérifié : sonde PG4). | S |
| R5 | `/tick query` rapporte l'état "en retard" avec la clé D `commands.tick.status.lagging` ("The game is running, but can't keep up with the target tick rate") quand le serveur n'atteint pas la cadence cible. Le seuil qui déclenche cet état n'est pas documenté. Provisoire : moyenne glissante du temps de tick sur les 100 derniers ticks supérieure à la cadence cible (50 ms). Sonde PG4. | S |
| R6 | Chien de garde : `max-tick-time` (défaut 60000 ms, `-1` le désactive). Si un seul tick dépasse la valeur, le serveur est considéré comme planté, s'arrête et sort avec le code 1. Provisoire : fil de surveillance indépendant, qui journalise l'état puis arrête le processus avec le code 1. Sonde PG4. | S |
| R7 | Pause sans joueur : `pause-when-empty-seconds` (défaut 60) : après ce nombre de secondes sans joueur connecté, le serveur suspend ses ticks ; un joueur qui se connecte les reprend. La valeur à partir de laquelle la fonction est désactivée (0, négatif) et ce qui reste actif en pause (connexions, statut, RCON) ne sont pas documentés. Provisoire : 0 ou moins désactive la pause ; en pause, les connexions et le statut restent servis, les commandes de console aussi. Sonde PG6. | S |
| R8 | Arrêt : `/stop` (niveau `owners`, D) affiche `commands.stop.stopping` ("Stopping the server"). Provisoire : (1) refuser les nouvelles connexions ; (2) déconnecter chaque joueur avec la clé D `multiplayer.disconnect.server_shutdown` ("Server closed") ; (3) sauvegarder joueurs et monde (R63) ; (4) libérer le verrou (R55) ; (5) sortir avec le code 0. Ni l'ordre de ces étapes ni leur temps ne sont documentés. Sonde PD8. | D (permission, clés) / S |
| R9 | La boucle de tick est une fonction de l'état du monde et des entrées (paquets reçus, commandes, temps) : l'horloge de la boucle est injectable pour les tests ; aucune règle de la spec n'exige de lire l'heure du système ailleurs que pour planifier l'échéance (R1, R4) et pour le chien de garde (R6). | S |

### `/tick`

| Réf | Règle | Preuve |
|---|---|---|
| R10 | Formes (9 chemins exécutables, D) : `tick query` ; `tick rate <rate:float 1.0..10000.0>` ; `tick freeze` ; `tick unfreeze` ; `tick step` ; `tick step <time>` ; `tick step stop` ; `tick sprint <time>` ; `tick sprint stop`. Il n'existe pas de `tick sprint` sans argument. Permission : `admins` (niveau 3), partout dans l'arbre. | D |
| R11 | Argument `<time>` : analyseur `minecraft:time`, minimum 1 tick (D). Suffixes d'unité `d` = 24000 ticks, `s` = 20 ticks, `t` = 1 tick (défaut quand le suffixe est omis) ; la valeur est un flottant, arrondie à l'entier le plus proche ; `tick step` sans argument vaut 1 tick. Vecteur T02. Provisoire : arrondi à la moitié supérieure (`1.5t` donne 2), valeur arrondie inférieure à 1 refusée. Sonde PG2. | D (minimum) / S |
| R12 | Rôle de l'état de tick : quatre états observables, par la clé D de `/tick query` : `commands.tick.status.running` ("The game is running normally"), `status.frozen` ("The game is frozen"), `status.sprinting` ("The game is sprinting"), `status.lagging` (R5). | D (textes) / S (usage) |
| R13 | `tick query` : écrit l'état (R12) puis, en marche normale ou en retard, `commands.tick.query.rate.running` ("Target tick rate: %s per second." puis une ligne "Average time per tick: %sms (Target: %sms)"), en sprint `commands.tick.query.rate.sprinting` ("Target tick rate: %s per second (ignored, reference only)." puis "Average time per tick: %sms"), puis `commands.tick.query.percentiles` ("Percentiles: P50: %sms P95: %sms P99: %sms. Sample: %s"). Le nombre de décimales de chaque `%s` et la taille de l'échantillon ne sont pas documentés. Provisoire : une décimale pour les ms, cadence formatée comme un flottant (`20.0`), échantillon = nombre de ticks mesurés (au plus 100). Sonde PG1. | D (clés, textes) / S (usage) |
| R14 | `tick rate <rate>` : fixe la cadence cible ; message `commands.tick.rate.success` ("Set the target tick rate to %s per second") ; résultat de commande = la cadence. Les joueurs ne sont pas affectés par une cadence supérieure à 20 : le client ne reçoit que `ticking_state`. Provisoire : la valeur de `%s` est la cadence au format flottant. Sonde PG1. | D (texte) / S |
| R15 | `tick freeze` : arrête le tick de tous les éléments de jeu sauf les joueurs et l'entité que chacun chevauche (monture, bateau, wagonnet). Le résultat de la commande est 1 ; `tick unfreeze` reprend, résultat 0. La documentation signale que ne sont pas affectés : l'ouverture et la fermeture de portes, les leviers, la mise sous tension de la poussière de redstone. Provisoire : freeze journalise comme `status.frozen`, unfreeze comme `status.running` (les clés `commands.tick.*` ne comportent aucun texte propre à freeze/unfreeze : C, absence de clé). Sonde PG2, PG8. | D (absence de clé) / S |
| R16 | `tick step [<time>]` : possible seulement quand le jeu est gelé ; fait avancer de `time` ticks puis regèle ; message `commands.tick.step.success` ("Stepping %s tick(s)"). Hors gel : échec avec `commands.tick.step.fail` ("Unable to step the game - the game must be frozen first"). `tick step stop` interrompt un pas en cours (`commands.tick.step.stop.success`, "Interrupted the current tick step") ; sans pas en cours : `commands.tick.step.stop.fail` ("No tick step in progress"). Depuis 26.3 snapshot 1, les échecs sont de vraies erreurs de commande (résultat d'échec, message rouge) et non plus un résultat 0 avec message. Provisoire : appliquer ces erreurs. Sonde PG2. | D (clés, textes) / S |
| R17 | `tick sprint <time>` : exécute les ticks aussi vite que possible, sans respecter la cadence cible, pendant `time` ticks (la documentation précise "plus un" : le nombre exact de ticks exécutés est à mesurer), puis rétablit l'état précédent (marche ou gel) et écrit `commands.tick.sprint.report` ("Sprint completed with %s ticks per second, or %s ms per tick"). `tick sprint stop` interrompt (`commands.tick.sprint.stop.success`, "Interrupted the current tick sprint") ; sans sprint en cours : erreur `commands.tick.sprint.stop.fail` ("No tick sprint in progress"). Provisoire : exécuter `time` ticks exactement (écart d'un tick à lever par PG2), report à la fin. | D (clés, textes) / S |
| R18 | Synchronisation : le serveur envoie `ticking_state` (cadence Float, gelé Boolean) à la connexion et à chaque changement de cadence ou de gel ; `ticking_step` (VarInt) quand un pas est lancé (`net-protocol-777.md` R94). Les champs sont S. Sonde PG9. | S |

### Ordre des phases d'un tick

| Réf | Règle | Preuve |
|---|---|---|
| R19 | Ordre documenté pour le serveur Java (page "Tick"). Chaque ligne est une phase ; l'ordre est S pour toutes, avec les sondes PG3 (entre catégories) et PG5 (sauvegarde). Provisoire : l'implémenter dans cet ordre. | S |

| Phase | Contenu | Jalon propriétaire |
|---|---|---|
| TK1 | Réinitialiser l'échéance suivante si le retard dépasse 2 s ; calculer l'échéance ; incrémenter le compteur de ticks | M8 |
| TK2 | Exécuter les fonctions des tags de fonctions `tick` (et `load` quand il y a lieu) | M8 (+ commandes) |
| TK3 | Toutes les 20 ticks, envoyer l'heure (`set_time`) à tous les joueurs | M8 |
| TK4 | Pour chaque dimension (Overworld, Nether, End, puis dimensions personnalisées) : voir TK4.a à TK4.r | |
| TK4.a | Mettre à jour la bordure du monde | M5 |
| TK4.b | Faire avancer la météo | M5 |
| TK4.c | Mettre à jour le temps du jour (horloge, R30 à R33) | M8 |
| TK4.d | Logique de sommeil des joueurs | M7 |
| TK4.e | Exécuter les commandes planifiées (`/schedule`) | M8 (+ commandes) |
| TK4.f | Exécuter les ticks planifiés de blocs | M5 |
| TK4.g | Exécuter les ticks planifiés de fluides | M5 |
| TK4.h | Logique des raids | M7 |
| TK4.i | Mettre à jour les niveaux de chargement des chunks | M3 |
| TK4.j | Pour chaque chunk dans un ordre aléatoire : apparition de mobs, glace et neige, ticks aléatoires | M5, M7 |
| TK4.k | Envoyer les changements de blocs aux joueurs | M8 |
| TK4.l | Mettre à jour les points d'intérêt | M7 |
| TK4.m | Décharger les chunks | M3 |
| TK4.n | Exécuter les événements de blocs | M5 |
| TK4.o | Combat du dragon (End) | M10 |
| TK4.p | Entités qui ne sont pas passagers : contrôle de despawn, tick de l'entité, tick de ses passagers | M7 |
| TK4.q | Ticks des block entities | M5 |
| TK4.r | Traiter les événements de jeu | M5 |
| TK5 | Envoyer les paquets en file, traiter les paquets entrants, envoyer les informations de joueurs, sauvegarde automatique tous les 6000 ticks, exécuter les tâches en attente | M8 |

| Réf | Règle | Preuve |
|---|---|---|
| R20 | Les ticks aléatoires s'appliquent à chaque section de 16x16x16 de chaque chunk entièrement chargé, `random_tick_speed` fois par section et par tick (défaut 3), un bloc pouvant être tiré plusieurs fois ; depuis 25w06a tous les chunks entièrement chargés les reçoivent, qu'un joueur soit proche ou non. La météo (glace, neige, remplissage de chaudron) a, par tick de chunk, 1 chance sur 16 d'examiner une colonne ; la foudre 1 sur 100 000 par chunk et par tick d'orage. L'apparition de mobs et la foudre exigent un joueur à moins de 8 chunks. Détails de la simulation : M5, M7. | S |
| R21 | Les chunks qui subissent le tick d'entités et de blocs ("chunks de tick d'entités") sont ceux dans la distance de simulation (`simulation-distance`, défaut 10, plage 3 à 32) autour des joueurs. Un chunk chargé n'est pas forcément actif (rapport 5.1). Détails : M3, M5. | S |
| R22 | Gel (R15) : les phases TK4.a à TK4.r ne s'exécutent pas pour les éléments gelés, sauf pour les joueurs et la monture d'un joueur ; TK1, TK3 et TK5 continuent. Le sort de TK2 et de TK4.e en gel n'est pas documenté. Provisoire : en gel, TK2 (fonctions de tick) et TK4.e (commandes planifiées) sont suspendus avec le reste de la simulation ; TK3 et TK5 continuent. Sonde PG8. | S |

### Temps de jeu et horloges

| Réf | Règle | Preuve |
|---|---|---|
| R30 | En 26.3 le temps passe par des **horloges** : le registre `minecraft:world_clock` compte 2 entrées, `minecraft:overworld` et `minecraft:the_end` (fichiers vides, D) ; chaque type de dimension déclare son horloge par défaut (`overworld` et `overworld_caves` : `minecraft:overworld` ; `the_end` : `minecraft:the_end` ; `the_nether` : aucune, D). L'âge du monde (`Time`) est un compteur distinct, en ticks depuis la création. | D |
| R31 | Les **lignes de temps** (`minecraft:timeline`, 4 entrées : `day`, `early_game`, `moon`, `villager_schedule`, D) lisent une horloge. `day` : horloge `minecraft:overworld`, période 24000 ticks, repères de temps `minecraft:day` = 1000, `minecraft:noon` = 6000, `minecraft:night` = 13000, `minecraft:midnight` = 18000 (affichables par commande), plus `roll_village_siege` = 18000 et `wake_up_from_sleep` = 0, et des pistes de valeurs qui dépendent du temps (sons, comportements). 24000 ticks = 20 minutes à 20 ticks par seconde (C). | D / C |
| R32 | Avancement : chaque horloge avance d'un tick par tick de serveur à vitesse 1.0 (`/time rate` change la vitesse, `/time pause` et `/time resume` l'arrêtent et la reprennent) tant que la gamerule `advance_time` est vraie. Provisoire : `advance_time` faux met en pause les horloges de toutes les dimensions ; le sommeil et `/time set` agissent sur l'horloge par défaut de la dimension de l'exécutant. Sonde PG7 (cas combinés avec PG3). | S |
| R33 | Forme de `/time` (19 chemins, D) : `time query gametime`, `time query time`, `time query <timeline>` [`repetition`], `time set <time>`, `time set <repère>`, `time add <time>`, `time pause`, `time resume`, `time rate <rate>`, et les mêmes sous `time of <clock:resource> ...`. Textes (D) : `commands.time.set` ("Set the time to %s"), `commands.time.query.gametime` ("The game time is %s tick(s)"), `query.absolute`, `query.timeline`, `pause`, `resume`, `rate`, et les erreurs `no_default_clock`, `no_time_marker_found`, `wrong_timeline_for_clock`, `pause.already_paused`, `pause.already_running`, `rate.already_same`, `set.already_at_time`. Permission `gamemasters`. Sémantique détaillée : sondes PG10 et `server-commands.md`. | D (formes, textes) / S |
| R34 | `set_time` (clientbound, play) envoie l'âge du monde et, par horloge, {identifiant de horloge, temps VarLong, fraction Float, vitesse Float} (`net-protocol-777.md` R78), toutes les 20 ticks (phase TK3) et après tout changement de temps. | S |

### Gamerules

| Réf | Règle | Preuve |
|---|---|---|
| R40 | Le registre `minecraft:game_rule` compte 59 règles, identifiants de protocole 0 à 58 (contigus), noms sous `minecraft:` en snake_case (renommées depuis les noms camelCase en 1.21.11, 25w44a, selon la documentation publique). Types (D, arbre de `/gamerule`) : 47 booléennes et 12 entières. Bornes (D) des entières : `fire_spread_radius_around_player` min -1 ; `max_block_modifications` min 1 ; `max_command_forks` min 0 ; `max_command_sequence_length` min 0 ; `max_entity_cramming` min 0 ; `max_minecart_speed` 1 à 1000 ; `max_snow_accumulation_height` 0 à 8 ; `players_nether_portal_creative_delay` min 0 ; `players_nether_portal_default_delay` min 0 ; `players_sleeping_percentage` min 0 ; `random_tick_speed` min 0 ; `respawn_radius` min 0. La documentation publique donne 1 comme minimum de `max_command_forks` : l'arbre fait foi (0). Tableau complet ci-dessous (`cmd_ref.py gamerules`). | D |
| R41 | Valeurs par défaut (colonne "Défaut"), noms historiques et inversions (`raids`, `elytra_movement_check`, `player_movement_check` sont les inverses d'anciennes règles `disable*`) : documentation publique seule. Provisoire : appliquer les défauts de la colonne ; sonde PG7 (`/gamerule <règle>` sur monde neuf, 59 interrogations) les lève. | S |
| R42 | `/gamerule` : `gamerule <règle>` interroge (`commands.gamerule.query`, "Game rule %s is currently set to %s") ; `gamerule <règle> <valeur>` modifie (`commands.gamerule.set`, "Game rule %s is now set to %s") ; si la valeur est déjà celle demandée, le jeu connaît le texte `commands.gamerule.not_set` ("Game rule %s is already set to %s"). Chaque règle est un littéral enfant de `gamerule`, présent deux fois dans l'arbre : `advance_time` et `minecraft:advance_time` (118 littéraux, D), suivi d'un argument `value` booléen (analyseur `brigadier:bool`) ou entier (`brigadier:integer` avec bornes). Permission `gamemasters` (niveau 2). Une valeur hors bornes est refusée par l'analyseur. Provisoire : la valeur renvoyée par la commande (résultat) est la nouvelle valeur (1 ou 0 pour un booléen) ; le cas "déjà à cette valeur" utilise `not_set` et un échec. Sonde PG7. | D (arbre, textes) / S |
| R43 | Stockage : dans `data/minecraft/game_rules.dat` (déplacé depuis `level.dat` à 26.1 snapshot 6). Format du fichier : R56. Provisoire : toutes les règles dont la valeur diffère du défaut y sont écrites ; une règle absente prend son défaut. Sonde PD3. | S |
| R44 | Synchronisation avec les clients : `game_rule_values` (clientbound, play, tableau de {nom Identifier, valeur String}) répond à `client_command` action 2 ; `set_game_rule` (serverbound, play) permet à un opérateur de les changer depuis l'interface. Certaines règles sont répercutées dans `login` (`reduced_debug_info`, `immediate_respawn`, `limited_crafting`) et par `game_event` 11 et 12. Provisoire : M8 implémente la réponse à l'action 2, le décodage de `set_game_rule` (niveau requis `gamemasters`) et les trois champs de `login` ; la forme exacte de `set_game_rule` est la question Q7. Sonde PG9. | S |
| R45 | Portée : les règles s'appliquent à tout le serveur (une instance de monde). La documentation ne précise pas de portée par dimension. Provisoire : valeur unique par serveur. Sonde PD3 (la présence de plusieurs fichiers `game_rules.dat` par dimension la contredirait). | S |
| R46 | Propriétaires : chaque règle est lue par le sous-système qu'elle concerne (M5 : `random_tick_speed`, `water_source_conversion`, `lava_source_conversion`, `fire_spread_radius_around_player`, `spread_vines`, `tnt_explodes` ; M6 : `block_drops`, `entity_drops`, `mob_drops` ; M7 : `mob_griefing`, `spawn_*`, `pvp`, `fall_damage`, `keep_inventory`...). Ce document ne fixe que le registre, le stockage, la commande et la synchronisation. | S |

### Tableau des gamerules (R40 à R42)

Colonnes : identifiant de protocole (D), règle (D), type, bornes (D), défaut et ancien nom (S, documentation publique). Les colonnes D se reproduisent avec `cmd_ref.py gamerules` (identifiants : `cmd_ref.py registry minecraft:game_rule`) ; la colonne Défaut et les anciens noms sont recopiés de la page "Game rule" (S). Contrôle croisé fait à la main : un seul écart relevé entre la documentation et l'arbre : le minimum de `max_command_forks`.

| ID | Règle | Type | Min | Max | Défaut (S) | Ancien nom (S) |
|---|---|---|---|---|---|---|
| 0 | `advance_time` | bool |  |  | true | `doDaylightCycle` |
| 1 | `advance_weather` | bool |  |  | true | `doWeatherCycle` |
| 2 | `allow_entering_nether_using_portals` | bool |  |  | true | `allowEnteringNetherUsingPortals` |
| 3 | `block_drops` | bool |  |  | true | `doTileDrops` |
| 4 | `block_explosion_drop_decay` | bool |  |  | true | `blockExplosionDropDecay` |
| 5 | `command_blocks_work` | bool |  |  | true | `commandBlocksEnabled` |
| 6 | `command_block_output` | bool |  |  | true | `commandBlockOutput` |
| 7 | `drowning_damage` | bool |  |  | true | `drowningDamage` |
| 8 | `elytra_movement_check` | bool |  |  | true | `disableElytraMovementCheck (inverse)` |
| 9 | `ender_pearls_vanish_on_death` | bool |  |  | true | `enderPearlsVanishOnDeath` |
| 10 | `entity_drops` | bool |  |  | true | `doEntityDrops` |
| 11 | `fall_damage` | bool |  |  | true | `fallDamage` |
| 12 | `fire_damage` | bool |  |  | true | `fireDamage` |
| 13 | `fire_spread_radius_around_player` | int | -1 |  | 128 |  |
| 14 | `forgive_dead_players` | bool |  |  | true | `forgiveDeadPlayers` |
| 15 | `freeze_damage` | bool |  |  | true | `freezeDamage` |
| 16 | `global_sound_events` | bool |  |  | true | `globalSoundEvents` |
| 17 | `immediate_respawn` | bool |  |  | false | `doImmediateRespawn` |
| 18 | `keep_inventory` | bool |  |  | false | `keepInventory` |
| 19 | `lava_source_conversion` | bool |  |  | false | `lavaSourceConversion` |
| 20 | `limited_crafting` | bool |  |  | false | `doLimitedCrafting` |
| 21 | `locator_bar` | bool |  |  | true | `locatorBar` |
| 22 | `log_admin_commands` | bool |  |  | true | `logAdminCommands` |
| 23 | `max_block_modifications` | int | 1 |  | 32768 | `command_modification_block_limit` |
| 24 | `max_command_forks` | int | 0 |  | 65536 | `maxCommandForkCount` |
| 25 | `max_command_sequence_length` | int | 0 |  | 65536 | `maxCommandChainLength` |
| 26 | `max_entity_cramming` | int | 0 |  | 24 | `maxEntityCramming` |
| 27 | `max_minecart_speed` | int | 1 | 1000 | 8 | `minecartMaxSpeed` |
| 28 | `max_snow_accumulation_height` | int | 0 | 8 | 1 | `snowAccumulationHeight` |
| 29 | `mob_drops` | bool |  |  | true | `doMobLoot` |
| 30 | `mob_explosion_drop_decay` | bool |  |  | true | `mobExplosionDropDecay` |
| 31 | `mob_griefing` | bool |  |  | true | `mobGriefing` |
| 32 | `natural_health_regeneration` | bool |  |  | true | `naturalRegeneration` |
| 33 | `player_movement_check` | bool |  |  | true | `disablePlayerMovementCheck (inverse)` |
| 34 | `players_nether_portal_creative_delay` | int | 0 |  | 0 | `playersNetherPortalCreativeDelay` |
| 35 | `players_nether_portal_default_delay` | int | 0 |  | 80 | `playersNetherPortalDefaultDelay` |
| 36 | `players_sleeping_percentage` | int | 0 |  | 100 | `playersSleepingPercentage` |
| 37 | `projectiles_can_break_blocks` | bool |  |  | true | `projectilesCanBreakBlocks` |
| 38 | `pvp` | bool |  |  | true | `pvp` |
| 39 | `raids` | bool |  |  | true | `disableRaids (inverse)` |
| 40 | `random_tick_speed` | int | 0 |  | 3 | `randomTickSpeed` |
| 41 | `reduced_debug_info` | bool |  |  | false | `reducedDebugInfo` |
| 42 | `respawn_radius` | int | 0 |  | 10 | `spawnRadius` |
| 43 | `send_command_feedback` | bool |  |  | true | `sendCommandFeedback` |
| 44 | `show_advancement_messages` | bool |  |  | true | `announceAdvancements` |
| 45 | `show_death_messages` | bool |  |  | true | `showDeathMessages` |
| 46 | `spawner_blocks_work` | bool |  |  | true | `spawnerBlocksEnabled` |
| 47 | `spawn_mobs` | bool |  |  | true | `doMobSpawning` |
| 48 | `spawn_monsters` | bool |  |  | true | `spawnMonsters` |
| 49 | `spawn_patrols` | bool |  |  | true | `doPatrolSpawning` |
| 50 | `spawn_phantoms` | bool |  |  | true | `doInsomnia` |
| 51 | `spawn_wandering_traders` | bool |  |  | true | `doTraderSpawning` |
| 52 | `spawn_wardens` | bool |  |  | true | `doWardenSpawning` |
| 53 | `spectators_generate_chunks` | bool |  |  | true | `spectatorsGenerateChunks` |
| 54 | `spread_vines` | bool |  |  | true | `doVinesSpread` |
| 55 | `tnt_explodes` | bool |  |  | true | `tntExplodes` |
| 56 | `tnt_explosion_drop_decay` | bool |  |  | false | `tntExplosionDropDecay` |
| 57 | `universal_anger` | bool |  |  | false | `universalAnger` |
| 58 | `water_source_conversion` | bool |  |  | true | `waterSourceConversion` |

### Persistance : disposition et fichiers

| Réf | Règle | Preuve |
|---|---|---|
| R50 | Disposition du monde : celle de `world-anvil-region.md` R1 à R3 (dossiers `dimensions/<espace de noms>/<chemin>/` avec `region/`, `entities/`, `poi/`, `data/` ; à la racine `level.dat`, `level.dat_old`, `session.lock`, `data/`, `players/`, `datapacks/`). Ajouts de cette spec, S (page "Java Edition level format") : `level.dat_new` (temporaire d'écriture), `resourcepacks/resources.zip`, `generated/<espace>/structure/`. | S |
| R51 | Fichiers de `data/minecraft/` (partagés par les dimensions), S : `game_rules.dat`, `random_sequences.dat`, `scoreboard.dat`, `stopwatches.dat`, `wandering_trader.dat`, `world_clocks.dat`, `world_gen_settings.dat`, `scheduled_events.dat`, `custom_boss_events.dat`, `weather.dat`, `maps/`, `last_id.dat`. Fichiers par dimension : `dimensions/minecraft/overworld/data/minecraft/` : `raids.dat`, `world_border.dat`, `chunk_tickets.dat` ; mêmes dossiers pour le Nether et l'End (l'End utilise aussi `raids.dat`), et `ender_dragon_fight.dat` sous l'End. Provisoire : le moteur lit et écrit ceux dont le sous-système existe (M8 : `game_rules.dat`, `world_clocks.dat`, `weather.dat`, `world_gen_settings.dat`, `world_border.dat`, `chunk_tickets.dat`, `random_sequences.dat`, `scoreboard.dat`, `last_id.dat`) ; un fichier inconnu présent dans un monde est conservé tel quel (non supprimé à l'écriture). Sonde PD1. | S |
| R52 | `level.dat` : NBT gzip (`core-nbt.md` R17). Racine : Compound contenant un Compound `Data`. Champs de `Data` documentés pour 26.1 et plus, types NBT : `allowCommands` Byte (booléen), `DataPacks` Compound {`Enabled` List de String, défaut `["vanilla"]`, `Disabled` List de String}, `DataVersion` Int, `difficulty_settings` Compound {`difficulty` String (`peaceful`, `easy`, `normal`, `hard`), `hardcore` Byte, `locked` Byte}, `enabled_features` List de String (absent sans fonctionnalité expérimentale), `GameType` Int (0 à 3, mode au premier spawn seulement), `initialized` Byte, `LastPlayed` Long (ms Unix), `LevelName` String, `singleplayer_uuid` Int_Array (UUID du joueur solo), `ServerBrands` List de String, `spawn` Compound {`dimension` String, `pitch` Int, `yaw` Int, `pos` Int_Array de 3}, `Time` Long (ticks depuis la création), `version` Int (19133 = Anvil), `Version` Compound {`Id` Int, `Name` String, `Series` String, `Snapshot` Byte}, `version_history` List d'Int, `WasModded` Byte. Valeurs pour notre moteur (D, `version.json`) : `DataVersion` et `Version.Id` = 5023, `Version.Name` = `26.3`, `Version.Series` = `main`, `Version.Snapshot` = 0 (stable : vrai). Les champs `spawn.pitch` et `spawn.yaw` sont indiqués Int par la page ; provisoire : Float si la sonde PD1 le montre. | D (valeurs de version) / S (schéma) |
| R53 | Champs déplacés hors de `level.dat` à 26.1 snapshot 6 (S) : `Player` remplacé par `singleplayer_uuid` ; `Difficulty` et `DifficultyLocked` dans `difficulty_settings` ; `game_rules` -> `data/minecraft/game_rules.dat` ; `WorldGenSettings` -> `world_gen_settings.dat` (`generate_features` devient `generate_structures`) ; météo (`clear_weather_time`, `rain_time`, `thunder_time`) -> `weather.dat` ; marchand ambulant (`spawn_chance`, `spawn_delay`) -> `wandering_trader.dat` ; `ScheduledEvents` -> `scheduled_events.dat` (champs `id`, `trigger_time`, `callback`, `type`) ; `CustomBossEvents` -> `custom_boss_events.dat` ; `DragonFight` -> `ender_dragon_fight.dat` ; horloges -> `world_clocks.dat`. Les champs `SpawnX`, `SpawnY`, `SpawnZ`, `DayTime`, `RandomSeed`, `raining`, `thundering`, les bordures et `Player` ne figurent plus dans le schéma documenté : leur emplacement actuel est à relever (sonde PD1). Provisoire : n'écrire dans `level.dat` que les champs de R52. | S |
| R54 | Écriture de `level.dat` : provisoire (R52 documente `level.dat_new` et `level.dat_old` sans le protocole) : écrire le contenu complet dans `level.dat_new`, puis renommer l'ancien `level.dat` en `level.dat_old`, puis `level.dat_new` en `level.dat`. À l'ouverture : si `level.dat` manque ou est illisible et que `level.dat_old` est lisible, utiliser `level.dat_old`. Sonde PD4. | S |
| R55 | Verrou : `session.lock` contient 3 octets `e2 98 83` (U+2603 en UTF-8, `world-anvil-region.md` R2) ; le serveur détient le verrou tant qu'il tourne. Provisoire : un second serveur sur le même dossier refuse de démarrer avec une erreur journalisée, sans toucher aux fichiers du monde. Sonde PD5. | S |
| R56 | Fichiers `*.dat` de `data/` : NBT gzip avec une racine Compound portant un champ `data` (Compound, contenu propre au fichier) et un champ `DataVersion` Int (forme habituelle des données sauvegardées en 1.17 et après ; non confirmée pour 26.3 par les pages consultées). Provisoire : lire en acceptant `data` seul ou le contenu à la racine ; écrire `{data: ..., DataVersion: 5023}`. Sonde PD3. | S |
| R57 | Données de joueur : `players/data/<uuid>.dat` (UUID en minuscules avec tirets), NBT gzip, racine Compound. Champs documentés (S) : `DataVersion` Int, `Dimension` String, `foodLevel` Int, `foodSaturationLevel` Float, `foodExhaustionLevel` Float, `foodTickTimer` Int, `playerGameType` Int, `previousPlayerGameType` Int, `SelectedItemSlot` Int (0 à 8), `Score` Int, `seenCredits` Byte, `SleepTimer` Short, `spawn_extra_particles_on_fall` Byte, `ignore_fall_damage_from_current_explosion` Byte, `XpLevel` Int, `XpP` Float, `XpSeed` Int, `XpTotal` Int, `Inventory` List de Compound {`Slot` Byte, `id` String, `count` Int, `components`}, `EnderItems` (`Slot` 0 à 26), `abilities` Compound {`flying`, `instabuild`, `invulnerable`, `mayBuild`, `mayfly` Byte ; `flySpeed` Float 0.05, `walkSpeed` Float 0.1}, `respawn` Compound {`pos` Int_Array, `yaw` Float, `pitch` Float, `dimension` String, `forced` Byte}, `LastDeathLocation` Compound {`dimension`, `pos`}, `entered_nether_pos` List de 3 Double, `RootVehicle` Compound {`Attach` Int_Array, `Entity`}, `recipeBook`, `warden_spawn_tracker` Compound {`warning_level`, `cooldown_ticks`, `ticks_since_last_warning` Int}, `ShoulderEntityLeft` et `ShoulderEntityRight`. Les balises communes à toutes les entités (`Pos`, `Motion`, `Rotation`, `Health`, `UUID`, `Air`, `Fire`, `FallDistance`, `OnGround`, `Invulnerable`, `PortalCooldown`, `Tags`, `Attributes`, `active_effects`...) ne sont pas décrites par la page consultée : elles relèvent de la spec des entités (M7). Les équipements ne sont plus dans `Inventory` à partir de 1.21.5 (la page parle des emplacements spéciaux jusqu'à 1.21.4) : question Q4. Provisoire : M8 lit et écrit `Pos`, `Rotation`, `Dimension`, `playerGameType`, `Inventory`, `EnderItems`, `abilities`, `respawn`, `XpLevel`, `XpP`, `XpTotal`, `foodLevel`, `Health`, `SelectedItemSlot`, `UUID`, `DataVersion` et conserve les autres balises lues sans les altérer. Sonde PD2. | S |
| R58 | Statistiques et avancements : `players/stats/<uuid>.json` et `players/advancements/<uuid>.json` (déplacés depuis `stats/` et `advancements/` à 26.1 snapshot 6), JSON ; schéma non relevé. Provisoire : M8 n'écrit pas ces fichiers et ne supprime pas ceux qu'il trouve. Question Q5. | S |
| R59 | Solo : en jeu solo (hors périmètre du serveur dédié), les données du joueur sont dans un fichier `players/data/` désigné par `singleplayer_uuid`. Non utilisé en M8. | S |

### Persistance : sauvegarde

| Réf | Règle | Preuve |
|---|---|---|
| R60 | Sauvegarde automatique toutes les 6000 ticks (5 minutes à 20 ticks par seconde, C), phase TK5 de R19 ; elle écrit les chunks modifiés ("sales"), les données de joueurs, `level.dat` et les fichiers de `data/`. `save-off` la désactive (`commands.save.disabled`, "Automatic saving is now disabled"), `save-on` la réactive (`commands.save.enabled`) ; répétitions : `commands.save.alreadyOff`, `commands.save.alreadyOn`. Permission `owners` (niveau 4) pour `save-all`, `save-off`, `save-on`. Provisoire : même périmètre pour la sauvegarde automatique et pour `save-all`. Sonde PG5. | D (permissions, textes) / S |
| R61 | `save-all` et `save-all flush` (2 chemins, D) : sauvegarde immédiate. Messages D : `commands.save.saving` ("Saving the game (this may take a moment!)"), `commands.save.success` ("Saved the game"), `commands.save.failed` ("Unable to save the game (is there enough disk space?)"). `flush` demande l'écriture effective sur disque de tout ce qui est en attente avant de rendre la main. Provisoire : sans `flush`, la sauvegarde est lancée et la commande revient quand les données sont remises à l'écriture ; avec `flush`, après l'écriture complète des fichiers de région (tampons vidés). Sonde PD1. | D (textes) / S |
| R62 | `sync-chunk-writes` (défaut vrai) : écritures de chunks synchrones, qui évitent la corruption après un plantage au prix des performances. Provisoire : écritures de région atomiques au niveau du secteur ; l'ordre d'écriture est celui de `world-anvil-region.md`. | S |
| R63 | Arrêt propre : sauvegarde de tous les chunks sales, de toutes les données de joueurs, de `level.dat` (R54) et des fichiers de `data/`, avec `flush`, puis libération du verrou (R55). Un plantage (processus tué) ne doit jamais laisser `level.dat` à la fois absent et illisible (R54). | S |
| R64 | Données de joueur : écrites à la déconnexion, à `save-all` et à la sauvegarde automatique, lues à la connexion (`net-protocol-777.md` R44, R60). Un joueur sans fichier est un nouveau joueur : apparition au `spawn` du monde (`level.dat`), mode `GameType` ou `gamemode` de `server.properties` si `force-gamemode`. Provisoire : même ordre. Sonde PD2. | S |
| R65 | Chunks : un chunk modifié est écrit au déchargement et à la sauvegarde ; un chunk non modifié n'est pas réécrit ; la compression des écrits suit `region-file-compression` (`deflate`, `lz4`, `none` ; défaut `deflate`) ; les chunks existants ne sont pas recompressés en masse (`world-anvil-region.md` R4 à R20). | S |

### Configuration du serveur

| Réf | Règle | Preuve |
|---|---|---|
| R70 | `server.properties` : fichier UTF-8 au format Properties (`clé=valeur`, `#` pour les commentaires) dans le dossier de travail ; lu d'abord en UTF-8, avec repli en ISO-8859-1 ; réécrit au démarrage : les valeurs existantes sont conservées, les commentaires perdus, les clés absentes ou invalides remises à leur défaut ; les changements ne s'appliquent qu'au redémarrage. Les clés `enable-command-block`, `pvp`, `spawn-monsters` et `allow-nether` ont été supprimées en 25w35a et remplacées par des gamerules. La clé du chat sécurisé est `enforce-secure-profile`. Provisoire : même comportement. Sonde PD6. | S |
| R71 | Clés pertinentes pour M8 (défauts, S) : `server-port` 25565 ; `server-ip` vide (toutes les adresses) ; `online-mode` vrai ; `network-compression-threshold` 256 (-1 désactive, 0 compresse tout) ; `max-players` 20 ; `motd` "A Minecraft Server" ; `level-name` `world` ; `level-seed` vide (aléatoire) ; `level-type` `minecraft:normal` ; `generate-structures` vrai ; `gamemode` `survival` ; `difficulty` `easy` ; `hardcore` faux ; `force-gamemode` faux ; `view-distance` 10 (3 à 32) ; `simulation-distance` 10 (3 à 32) ; `spawn-protection` 16 ; `op-permission-level` 4 (0 à 4) ; `function-permission-level` 2 (1 à 4) ; `max-tick-time` 60000 ; `pause-when-empty-seconds` 60 ; `player-idle-timeout` 0 (minutes) ; `white-list` vrai (défaut vrai depuis 26.3 snapshot 2) ; `enforce-whitelist` faux ; `enforce-secure-profile` vrai ; `enable-status` vrai ; `hide-online-players` faux ; `accepts-transfers` faux ; `prevent-proxy-connections` faux ; `rate-limit` 0 ; `entity-broadcast-range-percentage` 100 (10 à 1000) ; `max-world-size` 29999984 (1 à 29999984) ; `region-file-compression` `deflate` ; `sync-chunk-writes` vrai ; `enable-rcon` faux (RCON port 25575) ; `enable-query` faux ; `initial-enabled-packs` `vanilla` ; `initial-disabled-packs` vide. Les autres clés de la page (gestion du serveur, texte filtré, ressource pack, JMX, code de conduite) sont hors M8. Provisoire : lire ces clés avec ces défauts ; `white-list` à vrai par défaut signifie qu'un serveur neuf n'admet que les joueurs de `whitelist.json` : le propriétaire peut devoir le régler (Q8). Sonde PD6. | S |
| R72 | `ops.json` : tableau JSON d'objets {`uuid` String avec tirets, `name` String, `level` Int, `bypassesPlayerLimit` Boolean}. Si `level` manque, `op-permission-level` s'applique. Le niveau est un entier de 0 à 4 ; la page ne mentionne aucun changement de format à 26.x (les niveaux ont reçu des noms en 26.x : `all`, `moderators`, `gamemasters`, `admins`, `owners`, `server-commands.md` R8). Sonde PD7. | S |
| R73 | `whitelist.json` : tableau d'objets {`uuid` String avec tirets, `name` String}, relu par `/whitelist reload` (une modification manuelle n'est pas prise en compte avant). `banned-players.json` et `banned-ips.json` : formes non relevées dans cette passe (champs attendus : `uuid`, `name`, `created`, `source`, `expires`, `reason` pour les joueurs ; `ip`, `created`, `source`, `expires`, `reason` pour les adresses ; format de date `yyyy-MM-dd HH:mm:ss Z`) : ces champs sont rappelés de l'usage public et ne sont pas vérifiés. Sonde PD7. | S |
| R74 | Cache de profils : le jeu garde `usercache.json` (nom, UUID, date d'expiration). Forme non relevée ; provisoire : non utilisé en M8 (le UUID hors ligne se calcule, `net-protocol-777.md` R58). | S |
| R75 | Console et RCON : les commandes saisies sur la console du serveur n'ont pas de `/` initial et s'exécutent avec tous les droits. RCON (`enable-rcon`, port 25575, mot de passe obligatoire) est hors périmètre mais sert à l'oracle (`docs/VERIFICATION.md`). | S |
| R76 | `player-idle-timeout` (minutes, 0 = jamais) : un joueur inactif plus longtemps est expulsé avec la clé D `multiplayer.disconnect.idling` ("You have been idle for too long!") ; `/setidletimeout <minutes>` le modifie (`commands.setidletimeout.success`, "The player idle timeout is now %s minute(s)", ou `.success.disabled`). Provisoire : activité = tout paquet de mouvement, d'action ou de chat. Sonde PG6. | D (clés) / S |
| R77 | Ouverture d'un monde : `DataVersion` du `level.dat` supérieure à 5023 -> refus de démarrer (monde d'une version plus récente) ; égale -> normal ; inférieure -> hors périmètre (`world-anvil-region.md` R3). Provisoire : même comportement. | S |
| R78 | Génération du monde au premier démarrage : si le dossier du monde n'existe pas, il est créé avec les paramètres de `server.properties` (`level-seed`, `level-type`, `generate-structures`, `hardcore`, `gamemode`, `difficulty`), `level.dat` est écrit et les fichiers `data/` sont initialisés. La graine aléatoire est un entier 64 bits tiré d'une source non déterministe. | S |

## Entrées / sorties

- **Boucle** : entrée = état du monde, paquets reçus des joueurs, commandes de la console, instant courant ; sortie = nouvel état, paquets émis, fichiers écrits. Le tick est la plus petite unité de simulation ; il n'y a pas de demi-tick.
- **Types et plages** : cadence Float de 1.0 à 10000.0 ; pas et sprint en ticks entiers >= 1 ; `Time` du monde Long (ticks depuis la création, non négatif) ; temps d'horloge VarLong sur le réseau ; seuil de retard 2000 ms ; période de sauvegarde 6000 ticks ; `max-tick-time` entier en millisecondes (-1 = désactivé) ; `pause-when-empty-seconds` entier.
- **Gamerules** : 47 booléennes (`true` / `false`) et 12 entières dans les bornes de R40. Valeur booléenne hors `true`/`false` : refus par l'analyseur de commande.
- **Coordonnées négatives** : `spawn.pos` et `respawn.pos` sont des Int_Array signés ; `Pos` du joueur est une liste de 3 Double ; rien dans cette spec ne suppose des coordonnées positives. Pour l'ordre des chunks d'une dimension (TK4.j) l'ordre est aléatoire (R19) et ne dépend donc pas du signe.
- **Fichiers** : `level.dat`, `data/**/*.dat`, `players/data/*.dat` en NBT gzip ; erreurs de lecture d'un fichier de données : voir `core-nbt.md` R33, R61 ; un fichier de données de joueur illisible est mis de côté sans bloquer la connexion (provisoire : copie renommée `<uuid>.dat.corrupt`, le joueur est traité comme nouveau, avec un avertissement journalisé ; Q9).

## Cas limites

- `tick rate` aux deux bornes (1.0 et 10000.0), hors bornes (0.99, 10000.01), NaN ; `tick step` et `tick sprint` avec `1t`, `0.5t`, `0t`, `1s`, `1d`, valeurs énormes ; `tick step` hors gel ; `tick sprint` pendant un sprint ; `tick freeze` répété ; `tick freeze` pendant un sprint ; `tick step stop` sans pas ; deux `tick step` à la suite.
- Retard : exactement 2000 ms de retard (reste-t-on dans le rattrapage ?), 2001 ms, un tick de 59 999 ms et 60 001 ms avec `max-tick-time` à 60000, `-1`.
- Pause : joueur qui se connecte pendant la pause, qui se déconnecte au moment où le compteur expire, `pause-when-empty-seconds` à 0 et négatif.
- Gamerules : chaque borne basse et haute, une valeur à la borne -1 et +1, la règle sous les deux écritures (`random_tick_speed` et `minecraft:random_tick_speed`), règle inconnue, `fire_spread_radius_around_player` à -1 (illimité) et 0 (désactivé) selon la documentation.
- Temps : `Time` à 0 ; `time set` à une valeur supérieure à la période ; `time add` négatif ; horloge en pause avec `advance_time` faux ; dimension sans horloge (le Nether : `commands.time.no_default_clock`).
- Persistance : monde sans `level.dat` ; `level.dat` illisible avec `level.dat_old` lisible ; données de joueur absentes ; fichier de joueur d'une version plus récente ; `session.lock` présent sans processus ; sauvegarde interrompue par un plantage ; disque plein (`commands.save.failed`) ; nom de monde avec espaces ou caractères non ASCII.
- Configuration : `server.properties` absent, vide, avec clé inconnue, clé en double, valeur de mauvais type, hors plage (`view-distance=1`, `max-players=-1`), fin de ligne CRLF, encodage invalide.

## Vecteurs de test

Aucun octet n'est produit par ce domaine ; les vecteurs sont des tables d'arithmétique et d'état, calculées à la main ou par les commandes données. Tous sont C par rapport aux règles citées ; les comportements du jeu eux-mêmes sont S jusqu'aux sondes.

**T01 : intervalle entre ticks (R2)** : `python -c "print([(r, 1000/r) for r in (1, 10, 20, 40, 100, 10000)])"` -> 1 : 1000 ms ; 10 : 100 ms ; 20 : 50 ms ; 40 : 25 ms ; 100 : 10 ms ; 10000 : 0,1 ms.

**T02 : lecture de `<time>` (R11)** : entrée -> ticks : `1` -> 1 ; `1t` -> 1 ; `20t` -> 20 ; `1s` -> 20 ; `2.5s` -> 50 ; `0.5s` -> 10 ; `1d` -> 24000 ; `0.5d` -> 12000 ; `1.5t` -> 2 (arrondi supérieur provisoire) ; `0.4t` -> refusé (0 est sous le minimum 1) ; `0t` -> refusé ; `-1t` -> refusé.

**T03 : TPS atteignable (R3)** : MSPT 10 -> 20 TPS ; 40 -> 20 ; 50 -> 20 ; 80 -> 12,5 ; 100 -> 10 (cadence cible 20). Avec cadence cible 100 et MSPT 4 : 100 TPS ; avec MSPT 20 : 50 TPS.

**T04 : sauvegarde automatique (R60)** : 6000 ticks = 300 s à 20 ticks par seconde ; à la cadence 100 : 60 s ; à la cadence 10000 : 0,6 s.

**T05 : repères de temps (R31, D)** : `jour` 1000, `midi` 6000, `nuit` 13000, `minuit` 18000, période 24000 ; 1 minute de jeu réel à 20 TPS = 1200 ticks ; 20 minutes = 24000.

**T06 : machine d'états de `/tick` (R12, R15 à R17), provisoire** :

| État | Commande | Nouvel état | Message / erreur |
|---|---|---|---|
| marche | `tick freeze` | gelé | `status.frozen` (provisoire) |
| marche | `tick unfreeze` | marche | `status.running` (provisoire), résultat 0 |
| gelé | `tick unfreeze` | marche | idem |
| marche | `tick step` | marche | erreur `step.fail` |
| gelé | `tick step 5` | pas (5 ticks) puis gelé | `step.success` avec 5 |
| pas | `tick step stop` | gelé | `step.stop.success` |
| gelé | `tick step stop` | gelé | erreur `step.stop.fail` |
| marche | `tick sprint 100` | sprint puis marche | `sprint.report` à la fin |
| gelé | `tick sprint 100` | sprint puis gelé | `sprint.report` à la fin |
| sprint | `tick sprint stop` | état précédent | `sprint.stop.success` |
| marche | `tick sprint stop` | marche | erreur `sprint.stop.fail` |
| sprint | `tick query` | sprint | `status.sprinting` + `query.rate.sprinting` |

**T07 : permissions (R10)** : `tick` niveau `admins` ; `gamerule`, `time` niveau `gamemasters` ; `save-all`, `save-off`, `save-on`, `stop` niveau `owners` ; chaque ligne se vérifie dans `commands.json` : `cmd_ref.py levels`.

## Plan de tests

| Niveau | Contenu | Règles |
|---|---|---|
| L1 | Planificateur de ticks avec horloge simulée : cadence 20 (échéances de 50 ms), changement de cadence, retard de 1999 ms (rattrapage) puis 2001 ms (remise à zéro), compteur de ticks, gel (aucun tick de simulation, les phases TK1 et TK3 poursuivent), pas, sprint ; tests de propriétés : le nombre de ticks exécutés en N ms de temps simulé est `floor(N * cadence / 1000)` hors retard | R1 à R4, R9, R11 à R17 |
| L1 | Machine d'états de `/tick` : table T06 complète, erreurs et messages, résultats | R10 à R18 |
| L1 | Analyseur de `<time>` : table T02 | R11 |
| L1 | Registre des gamerules : 59 règles ; ids, types, bornes lus dans l'arbre de commandes ; défauts de R41 ; lecture/écriture de `game_rules.dat` par aller-retour avec `core-nbt` | R40 à R45 |
| L1 | Horloges : avance, pause, vitesse, repères (T05), dimension sans horloge | R30 à R34 |
| L1 | Persistance : écriture de `level.dat` (R54) avec simulation de plantage à chaque étape ; lecture de `level.dat_old` ; verrou ; `server.properties` (défauts, plages, clés inconnues, réécriture) | R50 à R56, R62 à R63, R70 à R78 |
| L1 | Données de joueur : aller-retour de tous les champs de R57 ; conservation des balises inconnues | R57, R64 |
| L2 | PG1 à PG10, PD1 à PD8 comparés à l'oracle : textes exacts, ordres, fichiers | tous |
| L2 | Relecture croisée : un monde sauvé par notre serveur est ouvert par l'oracle (`level.dat` accepté, joueur retrouvé, gamerules lues par `/gamerule`) et réciproquement ; `/execute if block`, `/data get entity` | R50 à R65 |
| L4 | Scénario `tools/diff/scenarios/m8-persist.json` : démarrer, se connecter, poser un bloc, se déplacer, `/gamerule keep_inventory true`, `/time set night`, `save-all flush`, `stop`, redémarrer, vérifier bloc, position, règle, temps ; puis `/tick freeze`, `/tick step 20`, `/tick sprint 1200`, `/tick query` | critère de sortie M8 |

## Sondes

Les sondes utilisent le serveur de référence (exécuté en boîte noire, RCON pour les commandes, `net-probe` de `net-protocol-777-vectors.md` pour les connexions) avec une graine fixe. **Bloquant** : l'EULA du serveur de référence n'est pas acceptée.

| Sonde | Ce qu'elle observe | Procédure | Règles levées |
|---|---|---|---|
| PG1 | Textes de `/tick query` | Monde neuf, juste après le démarrage : `tick query` ; puis `tick rate 40`, `tick query` ; `tick freeze`, `tick query` ; pendant un `tick sprint 600` (deuxième connexion RCON) `tick query`. Enregistrer les chaînes exactes, les retours à la ligne, le format des nombres, la taille de l'échantillon. | R13, R14, Q2 |
| PG2 | Machine d'états de `/tick` | Séquences de T06 par RCON ; pour chaque commande, enregistrer le texte, le résultat (`execute store result score ... run tick ...`) et le delta de `time query gametime` pour `step N` et `sprint N` (N = 1, 2, 20, 100) ; arrondis de T02 (`0.4t`, `0.5t`, `1.5t`, `2.5t`). | R11, R14 à R17, Q3 |
| PG3 | Ordre des phases | Pack de données avec : une fonction `#minecraft:tick` qui écrit `gametime` et l'état observé dans un stockage `log_a` ; une fonction replanifiée par `/schedule ... 1t` vers `log_b` ; un bloc de commandes répétable en activation permanente vers `log_c` ; sous `/tick freeze` puis `/tick step 1` répété 40 fois, relever à chaque hook (a) l'âge d'une entité objet posée, (b) l'état d'un bouton en pierre pressé (tick planifié de bloc), (c) le temps de combustion d'un four (block entity), (d) l'âge du monde, (e) l'état de la météo forcée. Le classement des hooks selon l'état déjà mis à jour ou non donne l'ordre relatif des phases TK2, TK4.e, TK4.f, TK4.p, TK4.q. Variantes avec `randomTickSpeed` élevé et un plant de pousse sans aléa observable pour TK4.j. | R19, R22, Q1 |
| PG4 | Retard et chien de garde | Suspendre le processus du serveur 1, 3, 10 s et comparer le temps réel à la variation de `gametime` (rattrapage borné à 2 s ?) ; lire le journal ; `tick query` après chaque ; `max-tick-time=5000` et suspension de 10 s : arrêt avec code de sortie 1 ? | R4 à R6 |
| PG5 | Sauvegarde automatique | `tick rate 1000` puis 13000 ticks avec observation (mtime des fichiers, journal) toutes les 100 ms ; avec `save-off` ; avec un joueur connecté et un bloc modifié avant chaque période. | R60, R61, R19 (TK5) |
| PG6 | Pause sans joueur et inactivité | `pause-when-empty-seconds=5` : relever `gametime` toutes les secondes pendant 20 s sans joueur, puis connecter une sonde, puis déconnecter ; RCON reste-t-il actif ? `player-idle-timeout=1` : joueur inactif 70 s, puis joueur qui envoie un paquet de mouvement toutes les 30 s. | R7, R76 |
| PG7 | Gamerules | Pour chaque règle (59) : `gamerule <règle>` sur monde neuf (défaut et texte), `gamerule <règle> <valeur>` (texte, résultat), la même valeur (`not_set`), hors bornes, les deux écritures du nom. | R40 à R42, R45 |
| PG8 | Gel | `tick freeze` avec : un cochon (IA), une entité objet qui tombe, une horloge de comparateurs/répéteurs, un four allumé, un fluide en écoulement, des plants (random ticks), un joueur qui se déplace et pose un bloc, `forceload add` d'une zone lointaine, une fonction `tick`, `/schedule`. Relever ce qui avance. | R15, R22 |
| PG9 | Synchronisation avec les clients | `net-probe` connecté : enregistrer `ticking_state` et `ticking_step` à la connexion et après `tick rate`, `freeze`, `step`, `sprint` ; envoyer `client_command` action 2 et noter `game_rule_values` ; changer une règle par `/gamerule` et noter ce qui est reçu ; avec un opérateur, envoyer `set_game_rule` (formes candidates). | R18, R44, Q7 |
| PG10 | `/time` | `time set` avec day, noon, night, midnight, 0, 24000, 30000 ; `add` positif et négatif ; `query` sous toutes ses formes ; `pause`, `resume`, `rate 2` ; `time of minecraft:the_end ...` ; dans le Nether ; `set_time` reçu par `net-probe` avant et après. | R31 à R34 |
| PD1 | Disposition et `level.dat` | Monde neuf, `save-all flush`, arrêt ; liste récursive des fichiers (noms, tailles, en-tête gzip ou non) ; décodage de `level.dat` et de chaque `.dat` par un lecteur NBT indépendant (types d'étiquettes comprises) ; recherche de l'emplacement de `SpawnX`..., `DayTime`, `RandomSeed`, `raining`, `Player`. | R50 à R53, R61, Q6 |
| PD2 | Données de joueur | Connexion d'une sonde hors ligne, déplacements, pose d'un bloc, `give`, changement de mode, déconnexion : décoder `players/data/<uuid>.dat` (champs, types, équipement, `Inventory`) ; nouvelle connexion ; instants d'écriture (déconnexion, `save-all`, sauvegarde automatique). | R57, R64, Q4 |
| PD3 | Fichiers de `data/` | Après chaque action (règle non défaut, `weather`, `time set`, `scoreboard objectives add`, `worldborder set`, `forceload add`, `random value`) : quels fichiers changent, forme de la racine (`data`, `DataVersion`), contenu de `game_rules.dat` (règles non défaut seulement ?). | R43, R45, R56 |
| PD4 | Protocole d'écriture | Surveillance de répertoire pendant `save-all flush` (création, renommage, suppression de `level.dat`, `level.dat_new`, `level.dat_old`) ; tuer le processus (SIGKILL) à des instants variés pendant une sauvegarde ; redémarrer et noter ce qui est lu. | R54, R63 |
| PD5 | Verrou | Deuxième serveur sur le même dossier : message, code de sortie, fichiers touchés ; `session.lock` : octets, instant de modification ; verrou orphelin après SIGKILL. | R55 |
| PD6 | `server.properties` | Premier démarrage (contenu produit, ordre, commentaires) ; fichiers altérés (valeurs invalides, clés inconnues et dupliquées, CRLF, octets non UTF-8, valeurs hors plage) ; relire le fichier après démarrage. | R70, R71 |
| PD7 | Fichiers d'accès | `op`, `deop`, `whitelist add/remove/on/off/reload`, `ban`, `ban-ip`, `pardon` ; contenu des JSON ; `ops.json` édité sans `level` ; liste blanche modifiée à la main sans `reload`. | R72, R73 |
| PD8 | Arrêt | Sonde connectée ; `stop` par RCON : paquet de déconnexion reçu (clé), journal, durée, code de sortie, fichiers écrits (instants) ; `stop` pendant un sprint. | R8, R63 |

## Questions ouvertes

| Réf | Question | Comment la lever |
|---|---|---|
| Q1 | Ordre exact des phases d'un tick (R19) : la page publique est la seule source. | PG3 |
| Q2 | Format exact de `/tick query` et de ses nombres (R13) | PG1 |
| Q3 | Nombre exact de ticks d'un `tick sprint N` ("plus un") et d'un `tick step N` (R16, R17) | PG2 |
| Q4 | Où est stocké l'équipement du joueur depuis 1.21.5 (R57) : l'`Inventory` seul ne suffit plus selon la page | PD2 |
| Q5 | Schéma de `players/stats/*.json` et `players/advancements/*.json` (R58) | PD2 (avec avancements et statistiques non nuls) |
| Q6 | Où sont maintenant `SpawnX`, `DayTime`, `RandomSeed`, la météo, la bordure (R53) | PD1, PD3 |
| Q7 | Forme exacte de `set_game_rule` et niveau requis (R44) | PG9 |
| Q8 | `white-list` vrai par défaut depuis 26.3 snapshot 2 (R71) : le comportement d'un monde neuf (liste vide : personne ne peut se connecter ?) | PD6, PD7, `net-protocol-777-vectors.md` PA3 |
| Q9 | Traitement d'un fichier de joueur ou de `data/` corrompu (R57 et Entrées / sorties) | PD2 avec fichier altéré |
| Q10 | Seuil exact de l'état "en retard" de `/tick query` (R5) et durée de l'échantillon | PG4 |
| Q11 | Ce qui continue en pause sans joueur (R7) | PG6 |
| Q12 | Format de date de `banned-*.json`, forme de `usercache.json` (R73, R74) | PD7 |

Règles à lever : toutes sauf R2 (plage), R10, R40, R42 (partie arbre), R30, R31 (D). Tant que PG3 n'a pas tourné, la spec ne passe pas à `figé`.

## Proposition pour la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Boucle de tick, `/tick`, gamerules, temps | M8 | B | spécifié | `docs/specs/server-game-loop.md` (R2, R10, R30, R31, R40 en D ; ordre des phases et `/tick` en S) |
| Persistance du monde et des joueurs, configuration | M8 | B | spécifié | idem (R50 à R78 en S) |

# Commandes : arbre, permissions, analyse et exécution

Statut : brouillon
Jalon : M8
Risque : B
Règles : R1 à R33.

## Périmètre

Couvre, pour `server` :

- l'arbre de commandes du jeu comme donnée (94 commandes racines, 2474 nœuds), sa fidélité, ses redirections et alias ;
- les permissions : niveaux, vérification, visibilité dans l'arbre envoyé aux clients ;
- l'analyse d'une ligne de commande, les analyseurs d'arguments (62 déclarés, 59 utilisés), les sélecteurs de cibles, les coordonnées relatives ;
- l'exécution : sources, contexte, résultat, succès, retour d'information, limites ;
- l'envoi de l'arbre au client (`commands`), les suggestions (`command_suggestion`) ;
- le catalogue complet des commandes avec rang d'implémentation et jalon de la sémantique ;
- pour les commandes de rang A (indispensables à un serveur utilisable au jalon M8) : syntaxe, permission, textes de retour d'information et comportement provisoire.

Ne couvre pas :

- la sémantique des commandes de rang B et C, qui appartient au jalon qui possède la fonctionnalité (`setblock` : M3, `give` : M6, `summon` : M7...) ; ce document donne leur place dans l'arbre et leur permission ;
- `/tick`, `/gamerule`, `/time`, `/save-all`, `/save-off`, `/save-on`, `/stop` : sémantique dans `server-game-loop.md` (R10 à R18, R40 à R46, R30 à R34, R60 à R63, R8) ; leur arbre et leur permission sont ici ;
- le format SNBT (`core-nbt.md` R39 à R51), les composants de texte, les prédicats et fonctions de datapacks (jalon M2) ;
- le format réseau des paquets : `net-protocol-777.md` (R25, R26, R79, R80) ;
- RCON et le protocole de gestion du serveur.

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `generated/reports/commands.json` (arbre complet : nœuds, analyseurs, propriétés, redirections, permissions) ; `generated/reports/registries.json` (`minecraft:command_argument_type` : 62 analyseurs, `minecraft:permission_type`, `minecraft:permission_check_type`) ; `resources/assets/minecraft/lang/en_us.json` (clés de traduction et textes anglais des messages, des erreurs de commande et des sélecteurs) ; `version.json`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Commands" (niveaux de permission de chaque commande, notion de résultat et de succès), "Commands/tick", "Commands/teleport", "Commands/weather", "Commands/list", "Argument types", "Target selectors", "Permission level", "Ops.json format", "Java Edition protocol/Command data", "Server.properties". Pages lues par un outil de résumé automatique : à relire sur la source.
- **Rapports locaux** : aucun ne décrit les commandes (recherche des mots `command`, `brigadier`, `tellraw` : seule une mention des commandes de diagnostic de `/tick` dans le chapitre 17.8 du rapport d'architecture). Aucune règle n'en vient. Le rapport Overworld embarque des copies de rapports du générateur de données (arbre de commandes, registres) : non utilisées, les fichiers extraits font foi.
- **Calculs** : `cmd_ref.py` (annexe de `server-commands-vectors.md`), sous-commandes `table`, `usage`, `gamerules`, `parsers`, `levels`, `lang`, `registry`.
- **Sondes de l'oracle** : aucune exécutée (EULA non acceptée). PX1 à PX10 sont décrites dans `server-commands-vectors.md`.

Niveaux de preuve : **D** = lu dans un fichier extrait ; **C** = calcul reproductible à partir de règles D ; **M** = non utilisé ; **S** = documentation publique seule ou hypothèse. Une règle S énonce son comportement **provisoire** et sa sonde.

## Règles

### L'arbre comme donnée

| Réf | Règle | Preuve |
|---|---|---|
| R1 | `commands.json` décrit l'arbre de commandes complet : un nœud racine de type littéral dont les 94 enfants sont les commandes. Sous la racine : 2474 nœuds, 1197 littéraux et 1277 arguments ; 1424 nœuds exécutables ; 114 redirections ; 59 analyseurs distincts utilisés (sur les 62 de `minecraft:command_argument_type` : `brigadier:long`, `minecraft:angle` et `minecraft:item_slots` ne sont pas utilisés). Commandes : `python -I cmd_ref.py table`. | D |
| R2 | Attributs d'un nœud dans le rapport : `type` (`literal` ou `argument`), `children` (objet nom -> nœud), `executable` (booléen, absent = faux), `parser` et `properties` (arguments), `redirect` (liste de noms : chemin depuis la racine vers le nœud cible), `permissions`. Les propriétés sont rendues sous forme de texte (`{"type": "players", "amount": "multiple"}`) et s'encodent selon `net-protocol-777.md` R26. | D |
| R3 | Redirections (114) : 109 vers `["execute"]` (les sous-commandes de `execute` qui rebouclent), 2 vers `["msg"]` (alias `tell` et `w`), 1 vers `["teammsg"]` (alias `tm`), 1 vers `["teleport"]` (alias `tp`), 1 vers `["experience"]` (alias `xp`). Un alias est un littéral racine avec `redirect` ; il n'a ni nœud exécutable propre ni enfant, et porte la même permission que sa cible (`tell`, `w`, `tm` : aucune ; `tp`, `xp` : `gamemasters`). | D |
| R4 | `execute ... run` est un littéral sans enfant et sans `redirect` dans le rapport. Sur le réseau, une commande de relais comme `run` pointe vers la racine (le client continue l'analyse depuis la racine). Provisoire : l'arbre envoyé encode `execute run` avec le drapeau de redirection et la racine comme cible. Sonde PX1. | D (rapport) / S (réseau) |
| R5 | `gamerule` porte 118 littéraux : chacune des 59 règles sous deux écritures, `advance_time` et `minecraft:advance_time`. Un serveur qui expose `gamerule` expose les deux écritures de chaque règle. | D |
| R6 | Fidélité et complétude : l'arbre exact du jeu est la référence des tests (niveau L2, sonde PX1). Provisoire : l'arbre envoyé aux clients est le sous-arbre des commandes implémentées par le serveur (rang A au jalon M8, puis les rangs suivants à mesure des jalons) ; une commande non implémentée est absente de l'arbre et rejetée comme inconnue (`command.unknown.command`, R13), jamais acceptée puis ignorée. Un test automatique vérifie que le sous-arbre implémenté est égal, nœud pour nœud, à la restriction de l'arbre du jeu à ces commandes. | S |

### Permissions

| Réf | Règle | Preuve |
|---|---|---|
| R7 | Chaque nœud peut porter une exigence de permission : `{"type": "minecraft:require", "permission": {"type": "minecraft:command_level", "level": "<nom>"}}`. Niveaux nommés présents dans l'arbre : `gamemasters`, `admins`, `owners` ; l'absence d'exigence est le niveau `all`. Répartition des 94 commandes racines (D, `cmd_ref.py levels`) : `all` 10 (`help`, `list`, `me`, `msg`, `random`, `teammsg`, `tell`, `tm`, `trigger`, `w`), `gamemasters` 63, `admins` 13 (`ban`, `ban-ip`, `banlist`, `debug`, `deop`, `kick`, `op`, `pardon`, `pardon-ip`, `setidletimeout`, `tick`, `transfer`, `whitelist`), `owners` 8 (`jfr`, `perf`, `publish`, `save-all`, `save-off`, `save-on`, `stop`, `unpublish`). Exigences imbriquées (5, D) : `datapack create` `owners` ; `debug function` `admins` ; `random reset`, `random roll ... sequence`, `random value ... sequence` `gamemasters`. | D |
| R8 | Correspondance avec les entiers 0 à 4 (S, page "Permission level") : `all` 0, `moderators` 1, `gamemasters` 2, `admins` 3, `owners` 4. Le nom `moderators` n'apparaît pas dans l'arbre. La page publique donne `tick`, `ban`, `kick`, `op`, `whitelist` au niveau 3 et `stop`, `save-all` au niveau 4, ce qui concorde avec l'arbre. Les registres `minecraft:permission_type` (`atom`, `command_level`) et `minecraft:permission_check_type` (`always_pass`, `require`) existent (D) : l'arbre n'utilise que `require` et `command_level`. Provisoire : modèle à niveau entier 0 à 4 ; les "atomes" de permission (second type) ne sont pas utilisés en M8. Sonde PX5. | D (noms, registres) / S (entiers) |
| R9 | Niveau d'un exécutant (S) : un joueur a le niveau de son entrée de `ops.json` (champ `level`) ou, si l'entrée n'a pas de `level`, `op-permission-level` (défaut 4) ; un joueur sans entrée a le niveau `all` (0). La console et RCON ont le niveau `owners`. Un bloc de commandes exécute au niveau `gamemasters` (2). Les commandes d'une fonction s'exécutent au niveau de `function-permission-level` (défaut 2, plage 1 à 4). `/op` et `/deop` changent le niveau d'un joueur connecté sans reconnexion et provoquent un nouvel envoi de l'arbre (R22). `execute as` / `at` ne changent pas le niveau. Provisoire : appliquer ces niveaux. Sonde PX5. | S |
| R10 | Une exigence échouée rend le chemin invisible : la commande est traitée comme inconnue (`command.unknown.command`) ; le texte de `help` confond d'ailleurs les deux cas (clé D `commands.help.failed`, "Unknown command or insufficient permissions"). Vérification à l'analyse et à l'exécution (une exigence imbriquée est testée au nœud qui la porte). Provisoire : même comportement. Sonde PX5. | D (texte de `help`) / S |
| R11 | Visibilité : le paquet `commands` envoyé à un joueur ne contient que les nœuds que son niveau permet d'atteindre (une racine dont l'exigence échoue est omise ; un enfant imbriqué dont l'exigence échoue aussi). Le drapeau "restreint" (`0x20`, `net-protocol-777.md` R25) est posé sur un nœud qui porte une exigence au-dessus de `all`. Provisoire : omission des nœuds interdits au niveau du joueur, drapeau sur tout nœud exigeant, au plus, `moderators`. Sonde PX1 (arbres reçus à 5 niveaux de permission). | S |

### Analyse

| Réf | Règle | Preuve |
|---|---|---|
| R12 | Entrées : le client envoie la commande sans `/` initial (`chat_command` ou `chat_command_signed`, String d'au plus 32767) ; la console et RCON la saisissent sans `/` ; une fonction ou un bloc de commandes peut la préfixer d'un `/` facultatif. Provisoire : un `/` initial est retiré une fois pour les fonctions et les blocs de commandes, jamais pour le chat (un double `//` est une commande inconnue). Sonde PX3. | S |
| R13 | Messages d'erreur d'analyse (clés D, texte anglais) : `command.unknown.command` ("Unknown or incomplete command. See below for error"), `command.unknown.argument` ("Incorrect argument for command"), `command.exception` ("Could not parse command: %s"), `command.context.here` ("<--[HERE]"), `command.context.parse_error` ("%s at position %s: %s"), `command.failed` ("An unexpected error occurred while trying to execute that command"), `permissions.requires.entity` ("An entity is required to run this command here"), `permissions.requires.player` ("A player is required to run this command here"). Provisoire : une commande inconnue donne `command.unknown.command`, suivie du contexte de la ligne avec `command.context.here` à la position de l'erreur ; une erreur de valeur d'argument donne le message propre à l'analyseur avec la position (`command.context.parse_error`). Position : décalage en unités de code UTF-16 depuis le début de la ligne sans `/`. Sonde PX3. | D (clés) / S |
| R14 | Appariement (S) : les littéraux se comparent exactement (sensibles à la casse) ; un argument consomme ce que son analyseur accepte ; la ligne est valide si l'analyse s'arrête sur un nœud exécutable avec toute l'entrée consommée ; il faut un séparateur d'un espace entre arguments ; une entrée en trop ou en moins est une erreur (`command.unknown.argument` ou `command.unknown.command`). Ordre d'essai des enfants quand plusieurs conviennent, et traitement des espaces multiples : non relevés. Provisoire : essayer les littéraux avant les arguments, puis les arguments dans l'ordre de l'arbre ; refuser les espaces multiples et finaux. Sonde PX3. | S |
| R15 | Analyseurs : table complète des 62 (identifiants D, occurrences D, grammaire S) dans le § Analyseurs. Les propriétés (bornes, `amount`, `type`, `registry`) viennent de l'arbre. Rang M8 : `brigadier:bool`, `float`, `double`, `integer`, `string`, `minecraft:entity`, `game_profile`, `block_pos`, `column_pos`, `vec2`, `vec3`, `rotation`, `message`, `component`, `time`, `gamemode`, `entity_anchor`, `resource_location`, `function`, `int_range`, `float_range`, `uuid`, `team_color`, `hex_color`, `style`, `swizzle`, `dimension`, `resource`, `resource_key`, `resource_or_tag`, `resource_or_tag_key`, `resource_selector` ; ceux qui exigent des données d'autres jalons (`block_state`, `block_predicate`, `item_stack`, `item_predicate`, `nbt_*`, `particle`, `loot_*`, `slot_source`, `dialog`, `feature`, `heightmap`, `template_*`, `context_*`, `item_slot`, `swing_animation`) sont reconnus syntaxiquement par la commande qui les utilise au jalon de cette commande (R6). Provisoire : reconnaître la syntaxe avec la commande qui l'emploie ; PX3 la lève. | D (liste) / S (grammaire) |
| R16 | Sélecteurs (S + D pour les clés) : variables `@p` (joueur le plus proche, limite 1, tri `nearest`), `@r` (joueur aléatoire, limite 1, tri `random`), `@a` (tous les joueurs en ligne, vivants ou morts, tri `arbitrary`), `@e` (toutes les entités vivantes des chunks chargés et les joueurs, tri `arbitrary`), `@s` (l'exécutant, rien si ce n'est pas une entité), `@n` (entité vivante la plus proche). Arguments : `x y z distance dx dy dz x_rotation y_rotation scores tag team name type predicate nbt level gamemode advancements limit sort` (21 clés, D par `argument.entity.options.*.description`). Syntaxe `@v[arg=valeur,...]`, arguments liés par un ET, `!` inverse, plages `a..b`, espaces autorisés autour de `[`, `=`, `,` sauf entre la variable et `[`, valeurs sensibles à la casse. Erreurs D : `argument.entity.options.unknown`, `.valueless`, `.unterminated`, `.inapplicable`, `argument.entity.selector.unknown`, `.missing`, `.not_allowed`, `argument.entity.notfound.entity`, `.player`, `argument.entity.toomany` ("Only one entity is allowed, but the provided selector allows more than one"), `argument.entity.invalid`. Les six variables ont des clés D `argument.entity.selector.{allEntities,allPlayers,nearestEntity,nearestPlayer,randomPlayer,self}`. Provisoire : implémentation complète en M8 pour `@s`, `@p`, `@a`, `@r`, `@e` et `@n` sur les joueurs ; sur les autres entités à partir de M7. Sonde PX7. | D (clés) / S |
| R17 | Coordonnées : `~` relatif à la position d'exécution, `^` local (axes de la vue, `^ ^ ^1` = un bloc devant) ; `block_pos` et `column_pos` en entiers, `vec3` et `vec2` en doubles ; mélange de `~` et `^` interdit (S, non relevé par la page) ; sans nombre derrière `~` : décalage 0. Limites de téléportation : x et z dans [-30000000, 30000000), y dans [-20000000, 20000000). Lacet entre -180.0 et 179.9 (réduit modulo 360), tangage entre -90.0 et 90.0. Provisoire : appliquer ; le centrage des `vec3` sans décimale (par exemple `~` ou `0` donnant le milieu du bloc) est la question Q3. Sonde PX9. | S |
| R18 | Suggestions : `command_suggestion` (identifiant de transaction, texte jusqu'au curseur, `/` compris) reçoit `command_suggestions` (même identifiant, début et longueur du segment remplacé, correspondances triées par le client, info-bulle facultative). Types de suggestion personnalisés d'un argument (drapeau `0x10`) : `minecraft:ask_server`, `minecraft:all_recipes`, `minecraft:available_sounds`, `minecraft:summonable_entities` (un type inconnu du client se replie sur `ask_server`). Provisoire : le serveur répond pour les littéraux (préfixe, sensible à la casse), les noms de joueurs, les gamemodes, les identifiants de registre ; il renvoie une liste vide ailleurs. Sonde PX8. | S |

### Exécution

| Réf | Règle | Preuve |
|---|---|---|
| R19 | Sources d'exécution : joueur (chat), console, RCON, bloc de commandes, chariot à commandes, fonction, `execute run`. Contexte : position (Double x3), dimension, rotation (lacet, tangage), entité exécutante facultative, ancre (`feet` par défaut, `eyes`), niveau de permission (R9), nom d'affichage, indicateur de sortie. Les modificateurs de `execute` (`as`, `at`, `in`, `positioned`, `rotated`, `anchored`, `facing`, `align`, `on`, `summon`, `store`, `if`, `unless`) changent ce contexte pour la suite ; `run` revient à la racine avec ce contexte. Provisoire : M8 implémente les sources joueur, console et RCON ; bloc de commandes, chariot à commandes, fonction et `execute` complet viennent avec leurs jalons (R6). PX2 (retours par chat et par RCON) et PX5 (bloc de commandes, fonction, console) la lèvent. | S |
| R20 | Résultat : chaque exécution renvoie un résultat entier et un succès. Le succès d'une commande est 0 ou 1 (sauf `execute`, qui renvoie le nombre d'exécutions réussies) ; échec = succès 0 ; erreur = exception de commande avec message rouge. Catégories de sortie de la page publique : non analysable, échec, succès, vide, interrompu, erreur. `execute store success` et `execute store result` capturent ces deux entiers. Provisoire : implémenter ce modèle ; les valeurs de résultat de chaque commande de rang A sont dans le § Commandes de rang A. PX2 la lève (résultat par `execute store result score`, succès par `store success`). | S |
| R21 | Retour d'information : le texte de succès est envoyé à l'exécutant sauf si `send_command_feedback` est faux (joueurs) ; un bloc de commandes mémorise sa dernière sortie si `command_block_output` est vrai ; si `log_admin_commands` est vrai, la commande est annoncée aux opérateurs en ligne et au journal avec la clé D `chat.type.admin` ("[%s: %s]", nom de l'exécutant puis texte). Les sorties de la console et de RCON sont relayées aux opérateurs selon `broadcast-console-to-ops` et `broadcast-rcon-to-ops` (défaut vrai). Sonde PX6. Provisoire : appliquer. | D (clé) / S |
| R22 | Mise à jour de l'arbre : le serveur renvoie `commands` à un joueur quand son niveau change (`/op`, `/deop`) et après `/reload`. Provisoire : même comportement. Sonde PX1. | S |
| R23 | Limites de travail (gamerules, bornes D) : `max_command_forks` (défaut 65536, nombre de bifurcations d'une chaîne `execute`), `max_command_sequence_length` (défaut 65536, longueur d'une chaîne de commandes d'une fonction), `max_block_modifications` (défaut 32768, nombre de blocs modifiables par commande, min 1). Défauts S (`server-game-loop.md` R40, R41). Provisoire : appliquer ; le dépassement donne une erreur de commande dont le texte est relevé par PX9. | S |
| R24 | Messages de traduction : le serveur envoie des composants `translate` (clé et arguments) que le client traduit avec son `en_us.json` (ou autre langue). Les clés et arguments de chaque commande de rang A sont donnés ci-dessous ; l'anglais est un repère pour les tests et pour la console du serveur, pas ce qui est envoyé. Un argument de type nom d'entité est un composant d'entité (nom + info-bulle) ; un argument de position un nombre formaté ; un argument de mode de jeu un composant `translate` `gameMode.<nom>`. Provisoire : composants `translate` avec les clés et arguments de R26 à R30 ; PX2 (paquet reçu) la lève. | D (clés) / S (arguments) |

### Rangs d'implémentation

| Réf | Règle | Preuve |
|---|---|---|
| R25 | Rang A : commandes du critère de sortie M8 et d'un serveur utilisable (36 commandes ; 95 nœuds exécutables hors `gamerule`, 236 pour `gamerule`) ; rang B : arbre complet, exécution déléguée au jalon propriétaire (51 commandes) ; rang C : commandes de diagnostic, de profilage et de test, ou propres au solo (7 commandes : `debug`, `fetchprofile`, `jfr`, `perf`, `publish`, `unpublish`, `test`). Les rangs sont une décision de planification (S) ; le catalogue ci-dessous les liste. `publish` et `unpublish` (solo) figurent dans l'arbre du rapport mais ne devraient pas figurer dans celui d'un serveur dédié : sonde PX1. Provisoire : appliquer ces rangs. | S |

### Commandes de rang A

Syntaxes (D) : `python -I cmd_ref.py usage NOM` (liste complète dans `server-commands-vectors.md`). Les clés de message sont D (fichier `en_us.json`, texte anglais entre guillemets) ; qui les envoie, à qui, avec quels arguments et la valeur de résultat sont S sauf mention contraire.

| Réf | Règle | Preuve |
|---|---|---|
| R26 | **Messagerie.** `help` et `help <command:string greedy>` (niveau `all`) : liste les commandes que l'exécutant peut utiliser, ou les usages de la commande demandée ; commande inconnue ou interdite : `commands.help.failed` ("Unknown command or insufficient permissions"). Format de la liste : non relevé ; provisoire : une ligne `/<nom>` par commande accessible suivie de ses usages. `list` et `list uuids` (niveau `all`) : `commands.list.players` ("There are %s of a max of %s players online: %s", nombre, maximum, noms séparés par ", "), avec `commands.list.nameAndId` ("%s (%s)", nom et UUID) pour chaque joueur si `uuids` ; résultat = nombre de joueurs, succès toujours. `say <message:message>` (niveau `gamemasters`) : diffuse `chat.type.announcement` ("[%s] %s", nom de l'exécutant, message) à tous les joueurs comme message déguisé (`disguised_chat`) ; les sélecteurs du message sont remplacés par les noms. `me <action:message>` (niveau `all`) : diffuse `chat.type.emote` ("* %s %s"). `msg`, `tell`, `w <targets:entity> <message:message>` (niveau `all`) : chaque destinataire reçoit `commands.message.display.incoming` ("%s whispers to you: %s") et l'émetteur `commands.message.display.outgoing` ("You whisper to %s: %s") ; résultat = nombre de destinataires. `teammsg <message>` et son alias `tm` (niveau `all`) : sans équipe, `commands.teammsg.failed.noteam` ("You must be on a team to message your team") ; sinon `chat.type.team.text` ("%s <%s> %s", équipe, nom, message) aux membres et `chat.type.team.sent` ("-> %s <%s> %s") à l'émetteur. `tellraw <targets:entity> <message:component>` (niveau `gamemasters`) : envoie le composant tel quel, après résolution des sélecteurs et des scores, comme message système. Sondes PX2, PX4. | D (syntaxe, clés, niveaux) / S |
| R27 | **Serveur.** `stop`, `save-all` (+ `flush`), `save-off`, `save-on` (niveau `owners`) : comportements et clés dans `server-game-loop.md` R8, R60, R61. `version` (niveau `gamemasters`) : `commands.version.header` ("Server version info:") suivi des lignes `commands.version.id` ("id = %s"), `name`, `series`, `protocol` ("protocol = %s (%s)"), `data`, `pack.data` ("pack_data = %s"), `pack.resource` ("pack_resource = %s"), `build_time`, `stable.yes` ou `stable.no`. Valeurs de la version cible (D, `version.json`) : id et nom `26.3`, série `main`, protocole 777, data 5023, pack de données `121.0`, pack de ressources `97.1`, construction `2026-09-15T11:20:48+00:00`, stable. Le second argument de la ligne `protocol`, l'ordre des lignes et le format de la date sont à lever (Q5). `seed` (niveau `gamemasters` dans l'arbre) : `commands.seed.success` ("Seed: %s") avec la graine du monde comme entier signé de 64 bits. Sondes PX4. Provisoire : appliquer. | D (niveaux, clés, valeurs de version) / S |
| R28 | **Accès.** `op <targets:game_profile>` (niveau `admins`) : crée ou met à jour l'entrée de `ops.json` avec `op-permission-level` ; `commands.op.success` ("Made %s a server operator") ; déjà opérateur : échec `commands.op.failed` ("Nothing changed. The player is already an operator") ; le niveau du joueur connecté change tout de suite (R9, R22). `deop` : `commands.deop.success` ("Made %s no longer a server operator"), `commands.deop.failed`. `kick <targets:entity players> [reason:message]` : déconnecte avec le motif, par défaut la clé D `multiplayer.disconnect.kicked` ("Kicked by an operator") ; `commands.kick.success` ("Kicked %s: %s") ; clés d'échec `commands.kick.owner.failed` et `commands.kick.singleplayer.failed` (réseau local et solo, hors périmètre). `ban <targets:game_profile> [reason]` : entrée de `banned-players.json`, expulsion du joueur connecté avec `multiplayer.disconnect.banned.reason` ("You are banned from this server.\nReason: %s") ou `banned.reason.default` ("Banned by an operator."), `commands.ban.success` ("Banned %s: %s"), `commands.ban.failed`. `ban-ip <target:string> [reason]` : nom de joueur ou adresse ; `commands.banip.success` ("Banned IP %s: %s"), `.failed`, `.invalid`, `.info` ("This ban affects %s player(s): %s") ; expulsion avec `ip_banned`. `banlist` (avec `ips` ou `players` facultatif) : `commands.banlist.list` ("There are %s ban(s):"), `.none`, `.entry` ("%s was banned by %s: %s"), `.entry.unknown` ("(Unknown)"). `pardon`, `pardon-ip` : `commands.pardon.success` ("Unbanned %s"), `.failed` ; `commands.pardonip.success`, `.failed`, `.invalid`. `whitelist add` et `whitelist remove` (`<targets:game_profile>`), `list`, `on`, `off`, `reload` : textes `commands.whitelist.add.success`, `.add.failed`, `.remove.success`, `.remove.failed`, `.list` ("There are %s whitelisted player(s): %s"), `.none`, `.enabled`, `.disabled`, `.alreadyOn`, `.alreadyOff`, `.reloaded` ; `on` avec `enforce-whitelist` expulse les joueurs hors liste (`not_whitelisted`). `setidletimeout <minutes:integer min 0>` : `commands.setidletimeout.success` ("The player idle timeout is now %s minute(s)") ou `.success.disabled` pour 0. `transfer <hostname:string> [port:integer] [players:entity]` : envoie `transfer` aux joueurs cibles (par défaut l'exécutant), `commands.transfer.success.single` ou `.multiple` ("Transferring %s to %s:%s"), `commands.transfer.error.no_players` ; les fichiers de configuration : `server-game-loop.md` R72, R73. Niveau `admins` pour toutes (D). Sondes PX2, PD7. Provisoire : appliquer. | D (syntaxe, niveaux, clés) / S |
| R29 | **Jeu.** `gamemode <gamemode> [target:entity]` (niveau `gamemasters`) : change le mode de la cible (soi par défaut) ; `commands.gamemode.success.self` ("Set own game mode to %s") ou `.other` ("Set %s's game mode to %s") ; effets réseau : `game_event` événement 3, `player_info_update` (mode), `player_abilities`. `defaultgamemode <gamemode>` : `commands.defaultgamemode.success` ("The default game mode is now %s") ; ne change pas le mode des joueurs en ligne ; persistance : question Q6. `difficulty` (interroge : `commands.difficulty.query`, "The difficulty is %s") et `difficulty` suivi de `peaceful`, `easy`, `normal` ou `hard` : `commands.difficulty.success` ("The difficulty has been set to %s") ou, si inchangée, `commands.difficulty.failure` ("The difficulty did not change; it is already set to %s") ; écrit `difficulty_settings.difficulty` de `level.dat` (`server-game-loop.md` R52) et envoie `change_difficulty`. Sondes PX2, PX9. Provisoire : appliquer. | D (syntaxe, clés) / S |
| R30 | **Monde.** `teleport` et son alias `tp` (niveau `gamemasters`, 8 syntaxes : destination entité ; destination position ; cibles + entité ; cibles + position ; cibles + position + `facing` position ; + `facing entity` [ancre] ; cibles + position + rotation) : déplace les cibles ; sans cibles explicites l'exécutant ; avec une position seule et une exécution par entité, la rotation de l'exécution s'applique ; avec cibles explicites la rotation est conservée sauf si elle est donnée ; destinations hors des limites de R17 : échec ; le chunk de destination est généré avant la téléportation ; messages `commands.teleport.success.entity.single` ou `.multiple` ("Teleported %s to %s", "Teleported %s entities to %s"), `.location.single` ou `.multiple` ("Teleported %s to %s, %s, %s", "Teleported %s entities to %s, %s, %s"), `commands.teleport.invalidPosition` ("Invalid position for teleport") ; résultat = 1 ou le nombre d'entités ; effets réseau : `player_position` (identifiant de téléportation) pour un joueur. `setworldspawn [pos:block_pos] [rotation]` (niveau `gamemasters`) : seulement dans l'Overworld (`commands.setworldspawn.failure.not_overworld`, "Can only set the world spawn for the Overworld") ; `commands.setworldspawn.success` ("Set the world spawn point to %s, %s, %s [%s]") ou `.success.new` avec la rotation et la dimension ; écrit `spawn` de `level.dat` et envoie `set_default_spawn_position`. `spawnpoint [targets] [pos] [rotation]` : `commands.spawnpoint.success.single` ou `.multiple` (+ `.new`) ; écrit `respawn` des données du joueur. `weather` suivi de `clear`, `rain` ou `thunder` et d'une durée `[duration:time min 1]` (niveau `gamemasters`) : `commands.weather.set.clear` ("Set the weather to clear"), `.rain`, `.thunder` ("Set the weather to rain and thunder") ; durée par défaut tirée au hasard : clair 12000 à 180000 ticks, pluie 12000 à 24000, orage 3600 à 15600 ; résultat = la durée donnée ou -1 ; à la fin, un temps clair passe à l'orage, la pluie et l'orage passent au clair ; effets réseau : `game_event` 7 et 8 (niveaux 0 à 1) ; interaction avec la gamerule `advance_weather` non documentée. Sondes PX2, PX9. Provisoire : appliquer. | D (syntaxe, clés) / S |
| R31 | **Déléguées.** `tick` (R10 à R18 de `server-game-loop.md`), `gamerule` (R40 à R46), `time` (R30 à R34) : l'arbre, les permissions et les textes sont dans ces règles. | D |
| R32 | Ordonnancement : une commande reçue d'un joueur est exécutée sur le fil de simulation, dans la phase TK5 de `server-game-loop.md` R19 (paquets entrants) ; une commande de console ou RCON s'exécute entre deux ticks, dans les "tâches en attente" ; les fonctions de tag `tick` dans TK2 ; les commandes planifiées par `/schedule` dans TK4.e ; les blocs de commandes dans TK4.q. Provisoire : ces emplacements. Sonde PG3 (`server-game-loop.md`). | S |
| R33 | Journal du serveur : chaque commande exécutée par un joueur est journalisée (nom, ligne) ; la console voit les retours d'information en clair (texte anglais traduit côté serveur avec `en_us.json`). Provisoire : le format du journal n'est pas spécifié ; il n'a pas de valeur de règle. Sans effet observable sur le protocole ni sur les fichiers : acceptée telle quelle ; PX6 relève le journal sans en faire une exigence. | S |

### Analyseurs

Les 62 analyseurs de `minecraft:command_argument_type` (D) avec leur nombre d'occurrences dans l'arbre (D, `cmd_ref.py parsers`) et leur grammaire (S, pages "Argument types" et "Command data"). La page "Command data" (protocole) en donne 57, avec `color` en 16 : elle est en retard sur les données (`net-protocol-777.md`, tableau d'écarts).

| ID | Analyseur | Occurrences dans l'arbre | Grammaire (S) |
|---|---|---|---|
| 0 | `brigadier:bool` | 114 | `true` ou `false` |
| 1 | `brigadier:float` | 46 | flottant 32 bits, bornes facultatives |
| 2 | `brigadier:double` | 47 | flottant 64 bits, bornes facultatives |
| 3 | `brigadier:integer` | 168 | entier 32 bits, bornes facultatives |
| 4 | `brigadier:long` | 0 | entier 64 bits (non utilisé par l'arbre) |
| 5 | `brigadier:string` | 17 | `word` (un mot), `phrase` (citable), `greedy` (reste de la ligne) |
| 6 | `minecraft:entity` | 146 | nom de joueur, sélecteur ou UUID ; propriétés `amount` single/multiple et `type` players/entities |
| 7 | `minecraft:game_profile` | 6 | nom de joueur ou sélecteur de joueurs |
| 8 | `minecraft:block_pos` | 116 | trois entiers ou notation `~` et `^` |
| 9 | `minecraft:column_pos` | 5 | deux entiers ou `~` (pas de `^`) |
| 10 | `minecraft:vec3` | 22 | trois doubles, `~` et `^` |
| 11 | `minecraft:vec2` | 2 | deux doubles, `~` (pas de `^`) |
| 12 | `minecraft:block_state` | 2 | `id[états]{nbt}` |
| 13 | `minecraft:block_predicate` | 11 | `id[états]{nbt}` ou `#tag` |
| 14 | `minecraft:item_stack` | 21 | `id[composants]` |
| 15 | `minecraft:item_predicate` | 5 | `type[tests]`, type = id, `#tag` ou `*` |
| 16 | `minecraft:team_color` | 2 | couleur de texte hors codes hexadécimaux (remplace `color`) |
| 17 | `minecraft:hex_color` | 1 | 3 ou 6 chiffres hexadécimaux |
| 18 | `minecraft:component` | 16 | composant de texte (chaîne ou JSON) |
| 19 | `minecraft:style` | 2 | objet JSON de style |
| 20 | `minecraft:message` | 7 | reste de la ligne, sélecteurs remplacés par les noms |
| 21 | `minecraft:nbt_compound_tag` | 5 | SNBT Compound (`core-nbt.md` R39 à R51) |
| 22 | `minecraft:nbt_tag` | 15 | SNBT de tout type |
| 23 | `minecraft:nbt_path` | 114 | chemin NBT (`a.b[0]."c d"`) |
| 24 | `minecraft:objective` | 28 | mot de `- + . _ A-Z a-z 0-9` |
| 25 | `minecraft:objective_criteria` | 1 | critère de tableau des scores |
| 26 | `minecraft:operation` | 1 | `=` `+=` `-=` `*=` `/=` `%=` `><` `<` `>` |
| 27 | `minecraft:particle` | 1 | `id` ou `id{configuration}` |
| 28 | `minecraft:angle` | 0 | lacet Float de -180.0 à 179.9, `~` relatif (non utilisé par l'arbre) |
| 29 | `minecraft:rotation` | 5 | deux doubles lacet puis tangage (lacet -180 à 179.9, tangage -90 à 90), `~` relatif |
| 30 | `minecraft:scoreboard_slot` | 1 | emplacement d'affichage |
| 31 | `minecraft:score_holder` | 27 | sélecteur, nom, UUID ou `*` ; propriété `amount` |
| 32 | `minecraft:swizzle` | 1 | combinaison sans répétition de `x`, `y`, `z` |
| 33 | `minecraft:team` | 5 | mot de `- + . _ A-Z a-z 0-9` |
| 34 | `minecraft:item_slot` | 2 | `type` ou `type.numéro` |
| 35 | `minecraft:item_slots` | 0 | comme `item_slot` avec `*` (non utilisé par l'arbre) |
| 36 | `minecraft:resource_location` | 79 | identifiant d'espace de noms optionnel |
| 37 | `minecraft:function` | 5 | identifiant de fonction ou `#tag` |
| 38 | `minecraft:entity_anchor` | 4 | `feet` ou `eyes` |
| 39 | `minecraft:int_range` | 4 | `N`, `..N`, `N..`, `N..M` |
| 40 | `minecraft:float_range` | 2 | idem avec des flottants |
| 41 | `minecraft:dimension` | 6 | identifiant de dimension |
| 42 | `minecraft:gamemode` | 2 | `survival`, `creative`, `adventure`, `spectator` |
| 43 | `minecraft:time` | 18 | flottant suivi de `d`, `s` ou `t` ; propriété `min` en ticks |
| 44 | `minecraft:resource_or_tag` | 5 | identifiant ou `#tag` d'un registre (propriété `registry`) |
| 45 | `minecraft:resource_or_tag_key` | 1 | idem, registre rechargeable |
| 46 | `minecraft:resource` | 11 | identifiant d'un registre (propriété `registry`) |
| 47 | `minecraft:resource_key` | 12 | identifiant résolu en clé de registre rechargeable |
| 48 | `minecraft:resource_selector` | 4 | identifiant avec jokers `*` et `?` |
| 49 | `minecraft:template_mirror` | 1 | `none`, `front_back`, `left_right` |
| 50 | `minecraft:template_rotation` | 1 | `none`, `clockwise_90`, `counterclockwise_90`, `180` |
| 51 | `minecraft:heightmap` | 1 | `world_surface`, `motion_blocking`, `motion_blocking_no_leaves`, `ocean_floor` |
| 52 | `minecraft:loot_table` | 14 | identifiant ou SNBT de table de butin |
| 53 | `minecraft:loot_predicate` | 2 | identifiant ou SNBT de prédicat |
| 54 | `minecraft:loot_modifier` | 14 | identifiant ou SNBT de modificateur d'objet |
| 55 | `minecraft:context_float_provider` | 48 | non documenté |
| 56 | `minecraft:context_int_provider` | 48 | non documenté |
| 57 | `minecraft:slot_source` | 28 | identifiant ou SNBT de source d'emplacements |
| 58 | `minecraft:dialog` | 1 | identifiant ou SNBT de dialogue |
| 59 | `minecraft:feature` | 1 | identifiant ou SNBT de feature |
| 60 | `minecraft:swing_animation` | 2 | `whack`, `stab`, `none` |
| 61 | `minecraft:uuid` | 1 | UUID avec tirets |

### Catalogue des 94 commandes

Niveau, nombre de nœuds exécutables et de nœuds (D), rang et jalon de la sémantique (S, décision de planification). Généré par `build_cmds.py` (annexe de `server-commands-vectors.md`) à partir de `commands.json`.

| Commande | Niveau | Nœuds exécutables | Nœuds | Alias | Rang | Sémantique (jalon) |
|---|---|---|---|---|---|---|
| `advancement` | gamemasters (2) | 12 | 25 |  | B | M6 |
| `attribute` | gamemasters (2) | 12 | 24 |  | B | M7 |
| `ban` | admins (3) | 2 | 3 |  | A | M8 |
| `ban-ip` | admins (3) | 2 | 3 |  | A | M8 |
| `banlist` | admins (3) | 3 | 3 |  | A | M8 |
| `bossbar` | gamemasters (2) | 25 | 39 |  | B | M8 (cadre) |
| `clear` | gamemasters (2) | 4 | 4 |  | B | M6 |
| `clone` | gamemasters (2) | 104 | 123 |  | B | M3 |
| `compute` | gamemasters (2) | 9 | 21 |  | B | M8 (cadre) |
| `damage` | gamemasters (2) | 5 | 10 |  | B | M7 |
| `data` | gamemasters (2) | 390 | 752 |  | B | M8 (cadre) |
| `datapack` | gamemasters (2) | 10 | 17 |  | B | M8 (cadre) |
| `debug` | admins (3) | 3 | 5 |  | C | hors M8 |
| `defaultgamemode` | gamemasters (2) | 1 | 2 |  | A | M8 |
| `deop` | admins (3) | 1 | 2 |  | A | M8 |
| `dialog` | gamemasters (2) | 2 | 6 |  | B | M9 |
| `difficulty` | gamemasters (2) | 5 | 5 |  | A | M8 |
| `effect` | gamemasters (2) | 10 | 13 |  | B | M7 |
| `enchant` | gamemasters (2) | 2 | 4 |  | B | M6 |
| `execute` | gamemasters (2) | 44 | 289 |  | B | M8 (cadre) |
| `experience` | gamemasters (2) | 8 | 15 |  | B | M7 |
| `fetchprofile` | gamemasters (2) | 3 | 7 |  | C | hors M8 |
| `fill` | gamemasters (2) | 12 | 15 |  | B | M3 |
| `fillbiome` | gamemasters (2) | 2 | 6 |  | B | M4 |
| `forceload` | gamemasters (2) | 7 | 10 |  | B | M3 |
| `function` | gamemasters (2) | 8 | 13 |  | B | M8 (cadre) |
| `gamemode` | gamemasters (2) | 2 | 3 |  | A | M8 |
| `gamerule` | gamemasters (2) | 236 | 237 |  | A | M8 |
| `give` | gamemasters (2) | 2 | 4 |  | B | M6 |
| `help` | all (0) | 2 | 2 |  | A | M8 |
| `item` | gamemasters (2) | 38 | 103 |  | B | M6 |
| `jfr` | owners (4) | 2 | 3 |  | C | hors M8 |
| `kick` | admins (3) | 2 | 3 |  | A | M8 |
| `kill` | gamemasters (2) | 2 | 2 |  | B | M7 |
| `list` | all (0) | 2 | 2 |  | A | M8 |
| `locate` | gamemasters (2) | 3 | 7 |  | B | M4 |
| `loot` | gamemasters (2) | 70 | 121 |  | B | M6 |
| `me` | all (0) | 1 | 2 |  | A | M8 |
| `msg` | all (0) | 1 | 3 |  | A | M8 |
| `op` | admins (3) | 1 | 2 |  | A | M8 |
| `pardon` | admins (3) | 1 | 2 |  | A | M8 |
| `pardon-ip` | admins (3) | 1 | 2 |  | A | M8 |
| `particle` | gamemasters (2) | 7 | 10 |  | B | M9 |
| `perf` | owners (4) | 2 | 3 |  | C | hors M8 |
| `place` | gamemasters (2) | 13 | 20 |  | B | M4 |
| `playsound` | gamemasters (2) | 67 | 68 |  | B | M9 |
| `posteffect` | gamemasters (2) | 4 | 11 |  | B | M9 |
| `publish` | owners (4) | 3 | 3 |  | C | hors M8 |
| `random` | all (0) | 12 | 16 |  | B | M8 (cadre) |
| `recipe` | gamemasters (2) | 4 | 9 |  | B | M6 |
| `reload` | gamemasters (2) | 1 | 1 |  | B | M8 (cadre) |
| `return` | gamemasters (2) | 2 | 4 |  | B | M8 (cadre) |
| `ride` | gamemasters (2) | 2 | 5 |  | B | M7 |
| `rotate` | gamemasters (2) | 4 | 8 |  | B | M7 |
| `save-all` | owners (4) | 2 | 2 |  | A | M8 |
| `save-off` | owners (4) | 1 | 1 |  | A | M8 |
| `save-on` | owners (4) | 1 | 1 |  | A | M8 |
| `say` | gamemasters (2) | 1 | 2 |  | A | M8 |
| `schedule` | gamemasters (2) | 4 | 8 |  | B | M8 (cadre) |
| `scoreboard` | gamemasters (2) | 30 | 70 |  | B | M8 (cadre) |
| `seed` | gamemasters (2) | 1 | 1 |  | A | M8 |
| `setblock` | gamemasters (2) | 5 | 7 |  | B | M3 |
| `setidletimeout` | admins (3) | 1 | 2 |  | A | M8 |
| `setworldspawn` | gamemasters (2) | 3 | 3 |  | A | M8 |
| `spawnpoint` | gamemasters (2) | 4 | 4 |  | A | M8 |
| `spectate` | gamemasters (2) | 3 | 3 |  | B | M7 |
| `spreadplayers` | gamemasters (2) | 2 | 10 |  | B | M7 |
| `stop` | owners (4) | 1 | 1 |  | A | M8 |
| `stopsound` | gamemasters (2) | 24 | 26 |  | B | M9 |
| `stopwatch` | gamemasters (2) | 5 | 10 |  | B | M8 (cadre) |
| `summon` | gamemasters (2) | 3 | 4 |  | B | M7 |
| `swing` | gamemasters (2) | 8 | 8 |  | B | M7 |
| `tag` | gamemasters (2) | 3 | 7 |  | B | M8 (cadre) |
| `team` | gamemasters (2) | 28 | 45 |  | B | M8 (cadre) |
| `teammsg` | all (0) | 1 | 2 |  | B | M8 (cadre) |
| `teleport` | gamemasters (2) | 8 | 12 |  | A | M8 |
| `tell` | all (0) | 0 | 1 | alias de `msg` | A | M8 |
| `tellraw` | gamemasters (2) | 1 | 3 |  | A | M8 |
| `test` | gamemasters (2) | 41 | 48 |  | C | hors M8 |
| `tick` | admins (3) | 9 | 12 |  | A | M8 |
| `time` | gamemasters (2) | 19 | 30 |  | A | M8 |
| `title` | gamemasters (2) | 6 | 14 |  | B | M8 (cadre) |
| `tm` | all (0) | 0 | 1 | alias de `teammsg` | B | M8 (cadre) |
| `tp` | gamemasters (2) | 0 | 1 | alias de `teleport` | A | M8 |
| `transfer` | admins (3) | 3 | 4 |  | A | M8 |
| `trigger` | all (0) | 3 | 6 |  | B | M8 (cadre) |
| `unpublish` | owners (4) | 1 | 1 |  | C | hors M8 |
| `version` | gamemasters (2) | 1 | 1 |  | A | M8 |
| `w` | all (0) | 0 | 1 | alias de `msg` | A | M8 |
| `waypoint` | gamemasters (2) | 6 | 13 |  | B | M9 |
| `weather` | gamemasters (2) | 6 | 7 |  | A | M5 (état), M8 (commande) |
| `whitelist` | admins (3) | 6 | 9 |  | A | M8 |
| `worldborder` | gamemasters (2) | 10 | 20 |  | B | M5 |
| `xp` | gamemasters (2) | 0 | 1 | alias de `experience` | B | M7 |

## Entrées / sorties

- **Entrée** : une ligne de commande (texte Unicode, au plus 32767 unités de code UTF-16 depuis le chat) et un contexte d'exécution (R19) ; **sortie** : un résultat entier, un succès 0/1, des messages traduisibles pour l'exécutant et les opérateurs, des effets sur l'état du monde et des paquets, ou une erreur avec position (R13).
- **Types** : arguments décrits dans le § Analyseurs ; entier 32 bits signé pour `brigadier:integer` avec bornes de l'arbre ; temps `minecraft:time` flottant 32 bits arrondi au tick entier le plus proche (minimum de l'arbre, 1 pour `/tick`, `/weather`, 0 pour `setidletimeout`).
- **Coordonnées négatives** : `~-1`, `-30000000`, `^-5` doivent être analysées ; `-0`, `+5` et `.5` sont des flottants valides pour `vec3` ; `block_pos` n'admet que des entiers (S, PX3).
- **Unicode** : les noms de joueurs, les messages et les composants peuvent contenir tout caractère ; les identifiants de ressources sont limités à `a-z 0-9 . - _ /`.

## Cas limites

- Ligne vide, `/` seul, `//time`, espaces en tête, en queue, multiples ; casse (`TIME`, `Tick`), entrée de 32767 unités et de 32768.
- Arguments : nombre juste au-dessus et au-dessous des bornes (`tick rate 0.99`, `10000.01`, `setidletimeout -1`), NaN et infini, `1e3`, `1_000`, `0x10`, booléen `True`, chaîne citée avec échappements, chaîne gloutonne vide.
- Sélecteurs : `@`, `@x`, `@e[`, `@e[type=]`, `@e[type=pig,type=cow]`, `@a[limit=0]`, `@p` sans joueur, `@s` depuis la console, `@e[distance=..-1]`, doublons de `scores`, noms de joueurs hors ligne, UUID inconnu.
- Permissions : un joueur de niveau 0 devant une commande de niveau 4 ; niveau modifié pendant la session ; commande imbriquée (`random reset`) ; fonction exécutée à un niveau supérieur ou inférieur à celui de l'exécutant ; alias (`tp`) d'une commande interdite.
- Résultats : `execute store` d'une commande qui échoue, qui lève une erreur, qui n'a pas de sortie ; `list` avec zéro joueur ; `banlist` vide ; `whitelist add` d'un joueur déjà présent ; `op` d'un joueur hors ligne (résolution de profil : le UUID hors ligne, `net-protocol-777.md` R58).
- Limites de travail : chaîne `execute` qui dépasse `max_command_forks`, fonction qui dépasse `max_command_sequence_length`.

## Vecteurs de test

Le fichier compagnon `server-commands-vectors.md` contient les sorties de `cmd_ref.py` (niveaux, décomptes, chemins de syntaxe des commandes de rang A, clés de traduction, sélecteurs, exigences imbriquées), le catalogue et les sondes PX1 à PX10. Vecteurs pour les tests unitaires :

| Id | Entrée | Résultat provisoire |
|---|---|---|
| VC1 | `tick rate 10000` | accepté |
| VC2 | `tick rate 10000.01` | erreur d'analyse sur la valeur (borne de l'arbre) |
| VC3 | `tick step 1s` | accepté, 20 ticks (`server-game-loop.md` T02) |
| VC4 | `tick sprint` | erreur d'analyse : commande incomplète (aucun nœud exécutable) |
| VC5 | `gamerule random_tick_speed -1` | erreur d'analyse (minimum 0) |
| VC6 | `gamerule minecraft:random_tick_speed 3` | accepté, même effet que sans préfixe |
| VC7 | `gamerule fire_spread_radius_around_player -1` | accepté (minimum -1) |
| VC8 | `weather rain 0t` | erreur d'analyse (minimum 1) |
| VC9 | `setidletimeout 0` | accepté, `commands.setidletimeout.success.disabled` |
| VC10 | `tp @s 30000000 0 0` | échec (x hors de [-30000000, 30000000)) |
| VC11 | `tp @s 29999999.5 0 0` | accepté |
| VC12 | `execute run` | commande incomplète (R4) |
| VC13 | `msg` | erreur : commande incomplète |
| VC14 | `w Notch bonjour` | alias de `msg` : message privé à `Notch` |
| VC15 | `op Notch` par un joueur de niveau 2 | commande inconnue pour lui (niveau `admins` requis) |
| VC16 | `list` | `commands.list.players` avec 0 sur 20 et liste vide, résultat 0 (aucun joueur) |
| VC17 | `/time set day` envoyé par `chat_command` avec `/` | commande inconnue (le `/` est retiré par le client, R12) |
| VC18 | `difficulty hard` quand elle est déjà `hard` | `commands.difficulty.failure` |

Les résultats de VC1 à VC18 sont provisoires, sauf VC1, VC2, VC4, VC5, VC6, VC7, VC8, VC9, VC12, VC13 qui découlent de l'arbre (D). VC3, VC10, VC11, VC14 à VC18 sont S.

## Plan de tests

| Niveau | Contenu | Règles |
|---|---|---|
| L1 | Chargement de `commands.json` : décomptes de R1 (94, 2474, 1197, 1277, 1424, 114, 59), répartition des niveaux (R7), redirections (R3), exigences imbriquées (R7) | R1 à R3, R7 |
| L1 | Analyse : pour chaque nœud exécutable du sous-arbre implémenté, une entrée valide synthétique (valeur type par analyseur) est acceptée ; la même entrée amputée du dernier argument, avec un argument en trop, ou une valeur hors des bornes de l'arbre est refusée avec position d'erreur | R12 à R15 |
| L1 | Permissions : pour chaque niveau 0 à 4 et chaque commande implémentée, visible dans l'arbre si et seulement si permise ; alias ; exigence imbriquée | R7 à R11 |
| L1 | Encodage de `commands` : décodage de ce qu'on a encodé redonne l'arbre ; CT01 de `net-protocol-777-vectors.md` ; chaque enfant et redirection désignent un nœud déjà déclaré | R4, R11 |
| L1 | Messages : pour chaque commande de rang A, le composant produit utilise la clé et le nombre d'arguments attendus (table des clés D) | R26 à R30 |
| L1 | Sélecteurs : tri, limites, négations, doublons, erreurs, sur une scène synthétique | R16 |
| L2 | PX1 (arbre reçu à 5 niveaux), PX2 (retours d'information), PX3 (corpus d'analyse), PX4 (`help`, `version`, `list`, `seed`), PX5 (permissions), PX6 (retour d'information et journal), PX7 (sélecteurs), PX8 (suggestions), PX9 (sémantique de `gamemode`, `teleport`, `weather`, `difficulty`, `setworldspawn`, `spawnpoint`), PX10 (fichiers d'accès) comparés à l'oracle | toutes |
| L4 | Scénario `tools/diff/scenarios/m8-commands.json` : un client vanilla se connecte, tape `/` (complétion de l'arbre), `/time set day`, `/gamemode creative`, `/tp @s 0 100 0`, `/weather clear`, `/list`, `/say bonjour`, `/msg` à un second client, `/kick` du second, `/stop` depuis la console | critère de sortie M8 |

## Questions ouvertes

| Réf | Question | Comment la lever |
|---|---|---|
| Q1 | Ordre d'essai des enfants et traitement des espaces multiples ou finaux (R14) | PX3 |
| Q2 | Format de sortie de `help` (R26) | PX4 |
| Q3 | Centrage des `vec3` sans décimale ; mélange `~` et `^` (R17) | PX9 |
| Q4 | Niveau effectif de `seed`, visibilité et drapeau "restreint" dans le paquet `commands` (R11, R27) | PX1, PX5 |
| Q5 | Second argument de `protocol = %s (%s)`, ordre et format de `version` (R27) | PX4 |
| Q6 | Où `defaultgamemode` est conservé (R29) | PX9 (relecture après redémarrage) |
| Q7 | Moment d'exécution des commandes dans le tick (R32) | PG3 |
| Q8 | `publish` et `unpublish` figurent-ils dans l'arbre d'un serveur dédié (R25) ? | PX1 |
| Q9 | Les "atomes" de permission (`minecraft:atom`) sont-ils utilisés par le serveur dédié, et comment `ops.json` les exprime (R8) ? | PX5, PD7 |
| Q10 | Encodage réseau de `execute run` (R4) | PX1 |
| Q11 | Textes d'erreur des analyseurs (valeur hors bornes, nombre mal formé) : clés `argument.*` et `parsing.*` à relever | PX3 |
| Q12 | Valeurs de résultat des commandes de rang A non relevées ici (`op`, `kick`, `whitelist`, `difficulty`) | PX2 |

Règles à lever : toutes sauf R1 à R3, R5, R7 (D) et la partie arbre de R13 (D). Tant que PX1 n'a pas tourné, la spec ne passe pas à `figé`.

## Proposition pour la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Commandes | M8 | B | spécifié | `docs/specs/server-commands.md`, `server-commands-vectors.md` (arbre, permissions, clés de message en D ; syntaxe d'analyse, exécution en S) |

# Protocole réseau 777 (Java Edition 26.3)

Statut : brouillon
Jalon : M8
Risque : B
Règles : R1 à R102 (numérotation continue, regroupées par thème).

## Périmètre

Couvre, pour `net` et la partie réseau de `server` :

- la connexion TCP, le découpage en trames, la compression, le chiffrement, VarInt et VarLong, les types de données du protocole ;
- la machine à états : handshake, status, login, configuration, play, transfert, reconfiguration ;
- l'authentification en ligne (chiffrement, empreinte de session) et le mode hors ligne (UUID, contrôles d'accès) ;
- les registres synchronisés en configuration, les tags, les drapeaux de fonctionnalités, les paquets d'état de configuration ;
- les 260 paquets des cinq états avec identifiant, nom et champs ;
- le comportement du serveur pour la séquence de connexion et pour les paquets du critère de sortie M8 : se connecter, charger le monde, se déplacer, poser et casser des blocs, parler, lancer une commande ;
- le format réseau de l'arbre de commandes (nœuds, analyseurs) ; la sémantique des commandes est dans `server-commands.md`.

Ne couvre pas :

- la boucle de jeu, les gamerules, la persistance : `server-game-loop.md` ;
- les commandes et leurs permissions : `server-commands.md` ;
- le NBT lui-même (`core-nbt.md`), le format de chunk sur disque (`world-chunk-format.md`), la génération ;
- le contenu sémantique des paquets de gameplay hors critère M8 (inventaires, scoreboard, avancements, cartes, marchands, structure blocks, tests) : seule leur forme est donnée, et elle reste S ;
- la vérification des signatures de chat et des clés de profil de Mojang : la forme des paquets est donnée, la validation est la question ouverte Q4 ;
- le protocole de gestion du serveur (JSON-RPC, `json-rpc-api-schema.json`), RCON, Query, Realms, LAN, proxys ;
- le rendu et le client (M9).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `version.json` (protocole 777, data version 5023) ; `generated/reports/packets.json` (états, sens, noms et identifiants des 260 paquets) ; `registries.json` (95 registres avec leurs identifiants de protocole : `command_argument_type`, `data_component_type`, `game_event`, `menu`, `mob_effect`, `entity_type`...) ; `blocks.json` (35 723 états de blocs) ; `datapack.json` (registres pilotés par les données) ; `commands.json` ; `resources/data/minecraft/dimension_type/*.json` ; `resources/data/minecraft/**` pour les décomptes de fichiers ; `resources/assets/minecraft/lang/en_us.json` (clés de traduction des messages de déconnexion).
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Java Edition protocol/Packets" (annoncée pour 26.3, protocole 777), "Java Edition protocol/FAQ" (séquence de connexion), "Java Edition protocol/Encryption", "Java Edition protocol/Server List Ping", "Java Edition protocol/Registry data", "Java Edition protocol/Command data", "Java Edition protocol/Slot data", "Java Edition protocol/Chunk format", "Java Edition protocol/Entity metadata", "Java Edition protocol/Data types", "Server.properties". L'extraction des pages a été faite par un outil de lecture automatique qui résume : une valeur lue ainsi peut être mal recopiée. C'est une raison de plus de les garder à S et de les confronter à la transcription PN4/PN5.
- **Rapports locaux** : `rapport_architecture_minecraft.md`, chapitre 12 (serveur intégré, suivi des entités par joueur, positions calculées côté client, paquets de deltas). Indice seulement : aucune règle n'en vient seule ; il ne donne aucun octet. Les autres rapports ne décrivent pas le protocole. Le rapport Overworld embarque des copies de rapports du générateur de données (liste de paquets, arbre de commandes, registres) : non utilisées, les fichiers extraits font foi.
- **Calculs** : `net_ref.py` (annexe de `net-protocol-777-vectors.md`), sous-commandes `varint`, `frames`, `invalid`, `lpvec`, `position`, `palette`, `hash`, `uuid`, `cfb8`, `commands`, `dims`.
- **Sondes de l'oracle** : aucune exécutée (EULA du serveur de référence non acceptée à ce jour). PN1 à PN13 et PA1 à PA4 sont décrites dans `net-protocol-777-vectors.md`.

Niveaux de preuve dans cette spec : **D** = valeur lue dans un fichier extrait, ou algorithme normatif public (RFC 1950 zlib, FIPS 180 SHA-1, RFC 8017 PKCS#1 v1.5, X.509 en DER, AES) ; **C** = propriété établie par un calcul reproductible (`net_ref.py`) à partir de règles D ; **M** = non utilisé (oracle pas encore lancé) ; **S** = documentation publique seule, ou hypothèse. Une règle S énonce son comportement **provisoire** et sa sonde. Une valeur de vecteur calculée à partir d'une règle S reste S.

Aucun nom de classe, de méthode ni de champ interne du jeu n'apparaît ici. Les noms de paquets (`add_entity`...) sont ceux du rapport `packets.json`, donnée extraite autorisée. Les noms de champs des tableaux sont des descriptions de l'auteur de la spec d'après la documentation publique, pas des identifiants du jeu.

### Écarts entre la documentation publique et les données extraites

La page "Packets" se présente comme celle de 26.3, mais plusieurs pages voisines ou tables internes sont en retard. Ces écarts ont été mesurés ; ils fixent la hiérarchie de confiance (R4).

| Sujet | Documentation publique | Données extraites | Conséquence |
|---|---|---|---|
| Analyseurs de commandes | 57 (0 à 56) ; 16 = `color` ; 55 = `dialog`, 56 = `uuid` | 62 (0 à 61) ; 16 = `team_color` ; 55 `context_float_provider`, 56 `context_int_provider`, 57 `slot_source`, 58 `dialog`, 59 `feature`, 60 `swing_animation`, 61 `uuid` | les identifiants viennent de `registries.json` (D) |
| Composants de données | 111 noms, ordre propre | 122 ; absents de la doc : `attack_animation`, `interact_animation`, `block_transformer`, `villager_food`, `compostable`, `cooking_fuel`, `brewing_fuel`, `mob_visibility`, `provides_pottery_pattern`, `sign_text_front`, `sign_text_back`, `waxed`, `cushion/color` ; dans la doc seulement : `map_color`, `swing_animation` ; ordre différent dès l'indice 5 | identifiants D ; formes d'encodage S |
| Identifiant de `level_chunk_with_light` | 0x2C sur la page "Chunk format" | 46 (0x2E) | la page "Chunk format" est périmée pour les identifiants |
| Format du paquet de chunk | la page "Chunk format" parle d'un champ de taille et de données additionnelles ; la page "Packets" décrit un tableau d'octets préfixé suivi des block entities et de la lumière | non mesurable | la page "Packets" l'emporte (S) ; sonde PN5 |
| Minimum de `max_command_forks` | 1 ("Game rule") | 0 (`commands.json`) | voir `server-game-loop.md` |

## Règles

### Identité et hiérarchie de confiance

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Version de protocole `777` (champ du handshake et `version.protocol` du statut), nom de version `26.3`, data version 5023. | D |
| R2 | Nombre de paquets par état et par sens : handshake 1 (serverbound) ; status 2 clientbound, 2 serverbound ; login 6 clientbound, 5 serverbound ; configuration 21 clientbound, 10 serverbound ; play 144 clientbound, 69 serverbound ; 260 au total. Les identifiants sont contigus de 0 à n-1 dans chaque couple (état, sens). Les tableaux du § Paquets donnent identifiant et nom. | D |
| R3 | Les champs des paquets (colonne "Champs") viennent de la documentation publique seule. Provisoire : on les implémente tels qu'écrits ; un écart avec la transcription d'un client ou d'un serveur de référence (sondes PN4, PN5) corrige la spec, jamais l'inverse. | S |
| R4 | Hiérarchie : un nom, un identifiant, un décompte ou un ordre lu dans un fichier extrait l'emporte sur la documentation publique (écarts du tableau ci-dessus). La documentation publique seule ne dépasse jamais S. | D |

### Octets, entiers et types simples

| Réf | Règle | Preuve |
|---|---|---|
| R5 | Tous les nombres sont gros-boutistes (Short, Int, Long, Float IEEE 754 binary32, Double binary64), sauf VarInt et VarLong. Boolean : 1 octet, `00` faux, `01` vrai. Provisoire : toute valeur non nulle est lue comme vraie, et le serveur n'écrit que `00` ou `01` (sonde PN7, entrée `02`). | S |
| R6 | VarInt : 1 à 5 octets, groupes de 7 bits de poids faible en premier, bit `0x80` = suite, entier 32 bits signé en complément à deux sans zigzag (les négatifs occupent toujours 5 octets). Écriture : tant que `v & ~0x7f` est non nul, écrire `(v & 0x7f) \| 0x80` puis décaler logiquement de 7 ; écrire l'octet restant. Vecteurs N01 à N12. Provisoire : implémenter tel quel ; PN5 la lève (tout VarInt de la capture se relit sans octet restant et se réécrit à l'identique), PN7 pour les cas limites. | S |
| R7 | VarLong : même schéma sur 64 bits, 1 à 10 octets. Vecteurs N13 à N19. Provisoire : implémenter tel quel ; PN5 la lève (VarLong de `section_blocks_update` et de `set_time`). | S |
| R8 | Lecture d'un VarInt : plus de 5 octets (le 5e porte encore le bit de suite) = erreur de protocole ("VarInt too big" dans la documentation), connexion fermée. Les encodages trop longs (zéros de tête, `80 80 00` pour 0) sont acceptés dans la limite de 5 octets (VarLong : 10). Provisoire : les bits du 5e octet au-delà du 32e bit sont perdus comme dans un décalage d'entier 32 bits (`ff ff ff ff 7f` = -1, vecteur N22). Sonde PN7. | S |
| R9 | String(n) : longueur en octets (VarInt) puis UTF-8, sans terminateur. n est au plus 32767. La longueur comptée en unités de code UTF-16 (un point de code au-dessus de U+FFFF compte 2) ne doit pas dépasser n, et la longueur en octets ne doit pas dépasser 3n+3 (taille maximale encodée : 3n+3 plus le préfixe). Provisoire : une chaîne trop longue, de longueur négative ou en UTF-8 invalide est une erreur de protocole (connexion fermée). Sonde PN7. | S |
| R10 | Identifier : String(32767) ; espace de noms dans `a-z 0-9 . - _`, chemin dans `a-z 0-9 . - _ /`, `:` unique séparateur, espace de noms par défaut `minecraft`. Provisoire : un identifiant invalide reçu est une erreur de protocole. PN7 (Identifier en majuscules dans `custom_payload`) la lève. | S |
| R11 | UUID : 16 octets, les 64 bits de poids fort puis les 64 bits de poids faible, chacun gros-boutiste. Provisoire : implémenter tel quel ; PN3 (UUID de `login_finished` comparé à U01 à U05) et PN5 (`player_info_update`, `add_entity`) la lèvent. | S |
| R12 | Position : un Long, x sur les 26 bits de poids fort, z sur les 26 suivants, y sur les 12 de poids faible, tous signés. Écriture : `((x & 0x3ffffff) << 38) \| ((z & 0x3ffffff) << 12) \| (y & 0xfff)`. Plages : x et z de -33554432 à 33554431, y de -2048 à 2047. Vecteurs P01 à P06. Provisoire : implémenter tel quel ; PN5 (`set_default_spawn_position`, `block_update`) et PN12 (positions de `player_action`) la lèvent. | S |
| R13 | Angle : 1 octet, un tour complet = 256 pas. Provisoire : écriture `floor(degrés * 256 / 360) & 0xff`, lecture `valeur * 360 / 256`. L'arrondi du jeu n'est pas documenté. Sonde PN5 (octets de `rotate_head`, `move_entity_pos_rot`). | S |
| R14 | Tableaux : "tableau préfixé" = VarInt longueur puis éléments (un tableau vide écrit `00`) ; "tableau" sans préfixe = longueur connue par le contexte ; "optionnel préfixé" = Boolean puis la valeur si vrai ; "optionnel" = présence connue par le contexte ; "énumération" = taille de son type de base ; "Either" = Boolean (vrai : première forme, faux : seconde). Les notations `(n)` derrière un tableau ou une chaîne sont des maximums que le destinataire refuse de dépasser (provisoire : erreur de protocole). | S |
| R15 | "ID ou X" : VarInt, 0 = la valeur X en ligne suit, sinon identifiant de registre + 1. "Ensemble d'ID" : VarInt type ; 0 = nom de tag (Identifier) ; sinon type - 1 identifiants VarInt. Provisoire : implémenter tel quel ; le serveur M8 n'émet que la forme par identifiant (valeur + 1). PN5 (`sound`) et PN13 (`disguised_chat` produit par `say`) la lèvent. | S |
| R16 | BitSet : VarInt nombre de Long, puis les Long ; le bit i vaut `(Long[i/64] >> (i%64)) & 1`. BitSet fixe(n) : `ceil(n/8)` octets, bit i = `(octet[i/8] >> (i%8)) & 1`. Les deux ne sont pas équivalents. Provisoire : implémenter tel quel (vecteur K12) ; PN5 la lève (masques de lumière d'un chunk). | S |
| R17 | Texte et NBT réseau : un composant de texte est un NBT à racine sans nom (octet de type puis charge, `core-nbt.md` R20) : tag String si le composant n'est que du texte brut sans style ni événement, sinon tag Compound. Un NBT absent est l'octet `00` (tag End) là où le champ est annoncé "peut être TAG_END". Le composant de texte JSON (seul `login_disconnect`) est une String d'au plus 262144 caractères. Sonde PN1. Provisoire : écrire une String pour un texte brut, un Compound sinon. | S |
| R18 | Vecteur à basse précision (LpVec3, `add_entity`, `set_entity_motion`, `interact`) : vecteur nul = un octet `00` ; sinon 6 octets plus, si l'échelle dépasse 3, un VarInt. Échelle s = `ceil(max des composantes en valeur absolue)` ; nul si ce maximum est inférieur à `1/32766`. Chaque composante c devient `round((c/s*0.5+0.5)*32766)` sur 15 bits. Mot de 48 bits : bits 0-1 = `s & 3`, bit 2 = drapeau de suite (s > 3), bits 3-17 = x, 18-32 = y, 33-47 = z. Octets 1 et 2 = bits 0-15 en petit-boutiste ; octets 3 à 6 = bits 16-47 en gros-boutiste ; si le drapeau est posé, VarInt `s >> 2` ensuite et `s = (mot & 3) \| (VarInt << 2)`. Décodage d'une composante : `min(q, 32766) * 2 / 32766 - 1`, fois s. Les quatre exemples d'octets de la documentation se décodent exactement avec ces règles (L01 à L05). Provisoire : implémenter tel quel ; PN5 la lève (octets de `add_entity` d'un joueur distant et de `set_entity_motion`). | S |
| R19 | Drapeaux de téléportation (Int) : bit posé = relatif, sinon absolu : `0x0001` X, `0x0002` Y, `0x0004` Z, `0x0008` lacet, `0x0010` tangage, `0x0020` vitesse X, `0x0040` vitesse Y, `0x0080` vitesse Z, `0x0100` appliquer à la vitesse la rotation du changement d'orientation avant d'ajouter la variation. Provisoire : implémenter tel quel ; le serveur M8 n'émet que `0` à l'entrée en jeu et les drapeaux déduits d'une commande `teleport` relative. PN5 (`player_position` initial) et PX9 (`teleport` avec `~`) la lèvent. | S |

### Types composés

| Réf | Règle | Preuve |
|---|---|---|
| R20 | Profil de jeu : UUID, nom String(16), propriétés (tableau préfixé, au plus 16) de {nom String(64), valeur String(32767), signature optionnelle préfixée String(1024)}. Hors ligne : tableau de propriétés vide (`00`). Provisoire : implémenter tel quel ; PN3 (propriétés vides de `login_finished` hors ligne) et PN5 (`player_info_update`) la lèvent. | S |
| R21 | Slot : VarInt nombre d'objets ; si 0, rien ne suit ; sinon VarInt identifiant d'objet (registre `minecraft:item`, 1658 entrées, D), VarInt nombre de composants à ajouter, VarInt nombre de composants à retirer, puis les composants ajoutés (VarInt type de composant puis données propres au type) puis les types retirés. Les types de composants sont les 122 identifiants de `minecraft:data_component_type` de `registries.json` (D). La forme binaire de chaque composant n'est pas spécifiée ici : seuls `custom_data` (NBT Compound), `max_stack_size` (VarInt 1-99), `max_damage`, `damage` (VarInt), `unbreakable` (aucun champ), `custom_name` et `item_name` (composant de texte), `item_model` (Identifier) sont documentés. Provisoire : le serveur n'envoie que des Slot sans composant (nombre à ajouter 0, à retirer 0) ou avec ces composants ; tout autre composant attend une spec M6. Sonde PN5 (inventaire envoyé à l'entrée). | S |
| R22 | Slot haché (`container_click` seulement) : Boolean "a un objet" ; si vrai : VarInt identifiant, VarInt nombre, tableau optionnel préfixé de {type de composant, Int somme de contrôle des données du composant (CRC32C)}, tableau optionnel préfixé de types retirés. La façon de calculer la somme de contrôle n'est pas documentée. Provisoire : le serveur lit la structure, ignore les sommes de contrôle et applique le clic d'après son propre état (il ne rejette pas une somme inconnue). Question ouverte Q9. PN12 (clics d'inventaire avec composants) la lève. | S |
| R23 | Métadonnées d'entité : suite d'entrées {index Unsigned Byte, type VarInt, valeur} terminée par l'octet `ff` (seul octet, sans type). Les champs absents ne changent pas. 44 types de valeur (0 Byte, 1 VarInt, 2 VarLong, 3 Float, 4 String, 5 composant de texte, 6 composant optionnel, 7 Slot, 8 Boolean, 9 rotations 3 Float, 10 Position, 11 Position optionnelle, 12 direction VarInt, 13 UUID optionnel, 14 état de bloc VarInt, 15 état de bloc optionnel (0 = absent), 16 particule, 17 particules, 18 villageois (3 VarInt), 19 VarInt optionnel (0 = absent, sinon valeur + 1), 20 pose VarInt, 21 à 32 variantes (chat, son de chat, vache, son de vache, loup, son de loup, grenouille, cochon, son de cochon, poule, son de poule, zombie nautile) en VarInt de registre, 33 position globale optionnelle, 34 variante de tableau (ID ou valeur), 35 à 38 états (renifleur, tatou, golem de cuivre, oxydation) VarInt, 39 vecteur 3 Float, 40 quaternion 4 Float, 41 profil résolvable, 42 bras VarInt, 43 couleur VarInt). Entité de base : index 0 masque Byte (`01` en feu, `02` accroupi, `08` sprint, `10` nage, `20` invisible, `40` brillant, `80` vol plané), 1 air VarInt (300), 2 nom personnalisé (composant optionnel), 3 nom visible Boolean, 4 silencieux Boolean, 5 sans gravité Boolean, 6 pose, 7 ticks de gel VarInt. Joueur : index 17 cœurs supplémentaires Float, 18 score VarInt, 19 et 20 épaules (VarInt optionnel) ; les index 8 à 16 (entité vivante, avatar) ne sont pas relevés. Provisoire : le serveur n'envoie que les index 0 à 7 et les index relevés ci-dessus pour le joueur ; sonde PN5 pour les index 8 à 16 et pour les valeurs par défaut. | S |
| R24 | Données de lumière (`level_chunk_with_light`, `light_update`) : quatre BitSet (ciel, bloc, ciel vide, bloc vide) de `sections + 2` bits chacun (bit 0 = 16 blocs sous la hauteur minimale, dernier bit = 16 blocs au-dessus de la hauteur maximale), puis le tableau de ciel et le tableau de bloc, chacun un tableau préfixé de tableaux préfixés de 2048 octets (un par bit posé du masque correspondant, du plus bas au plus haut), un demi-octet par bloc. Un bit n'est jamais posé à la fois dans un masque et dans son masque "vide". Vecteur K12. Provisoire : tant que le calcul de lumière n'existe pas, les masques de ciel couvrent toutes les sections non vides avec des demi-octets à 15 et les masques de bloc sont vides (hypothèse). PN5 compare ce qu'envoie le jeu et lève la règle. | S |
| R25 | Nœud de l'arbre de commandes (`commands`) : octet de drapeaux (`0x03` type : 0 racine, 1 littéral, 2 argument ; `0x04` exécutable ; `0x08` redirection ; `0x10` suggestions personnalisées, argument seulement ; `0x20` restreint) ; VarInt nombre d'enfants et leurs indices (VarInt) ; VarInt de redirection si `0x08` ; String nom (littéral et argument) ; VarInt identifiant d'analyseur et propriétés (argument) ; Identifier du type de suggestion si `0x10`. Le paquet porte le tableau de nœuds puis l'indice de la racine. Chaque enfant et chaque redirection ne désignent qu'un nœud déjà déclaré. Un analyseur inconnu du client arrête le décodage du paquet. Vecteur CT01. Provisoire : encoder comme écrit ; PN5 (octets du paquet `commands` à l'entrée en jeu) et PX1 (arbre complet) la lèvent. | S |
| R26 | Propriétés d'analyseur : `brigadier:float` `brigadier:double` `brigadier:integer` `brigadier:long` = octet de drapeaux (`01` minimum présent, `02` maximum présent) puis le minimum puis le maximum (types du nombre, gros-boutiste) ; `brigadier:string` = VarInt (0 un mot, 1 phrase citable, 2 reste de la ligne) ; `minecraft:entity` = octet (`01` une seule entité, `02` joueurs seulement) ; `minecraft:score_holder` = octet (`01` plusieurs) ; `minecraft:time` = Int minimum en ticks ; `minecraft:resource`, `resource_key`, `resource_or_tag`, `resource_or_tag_key`, `resource_selector` = Identifier du registre ; les autres analyseurs n'ont pas de propriétés. Les rapports donnent pour `commands.json` des propriétés sous forme de texte (`{"amount": "single", "type": "players"}`) qui s'encodent ainsi. Les identifiants des 62 analyseurs viennent de `registries.json` (D) : 0 `brigadier:bool` ... 61 `minecraft:uuid`. Provisoire pour `minecraft:swing_animation`, `slot_source`, `context_float_provider`, `context_int_provider`, `feature`, `dialog`, `heightmap`, `loot_*` : aucune propriété (la doc publique n'en donne pas). Sonde PX1 (`server-commands.md`). | S |

### Trames, compression, chiffrement

| Réf | Règle | Preuve |
|---|---|---|
| R27 | Trame sans compression : VarInt longueur (octets de ce qui suit), VarInt identifiant de paquet, charge. La longueur tient sur 3 octets au plus ; sa valeur maximale est 2 097 151 (2^21 - 1). Un champ de longueur de 4 octets (I01), une longueur de 0 (pas d'identifiant, I02) ou supérieure au maximum sont des erreurs de protocole : provisoire, connexion fermée sans autre réponse. Sonde PN6. Vecteurs F01 à F08. | S |
| R28 | Compression : le paquet `login_compression` (login, clientbound) porte un seuil VarInt. Seuil >= 0 : active la compression pour tous les paquets suivants dans les deux sens, y compris configuration et play. Seuil < 0 : désactive. Sans ce paquet, pas de compression. Il doit précéder `login_finished`. Le seuil ne change pas ensuite en cours de session. Provisoire : appliquer ; PN3 (seuils -1, 0, 1 et 256) la lève. | S |
| R29 | Trame avec compression : VarInt longueur du reste ; VarInt "longueur des données" ; puis, si la longueur des données est > 0, un flux zlib (RFC 1950) qui contient identifiant et charge, sinon (longueur des données = 0) l'identifiant et la charge en clair. À l'émission : taille de (identifiant + charge) >= seuil, compresser et écrire la taille décompressée ; sinon écrire 0 et envoyer en clair. Le compresseur zlib n'impose aucun octet précis (F10 est un exemple, pas une référence) ; le décompresseur doit lire tout flux zlib valide. Vecteurs F09 à F13. Provisoire : implémenter tel quel ; PN3 (corps de 255, 256 et 257 octets) la lève. | D (zlib) / S (usage) |
| R30 | Réception avec compression : (a) longueur des données = 0 avec corps de taille >= seuil : accepté (I03). (b) longueur des données entre 1 et seuil - 1 : refusé (I05) ; provisoire : erreur de protocole. (c) longueur des données différente de la taille réellement décompressée (I04) : provisoire, erreur de protocole. (d) taille décompressée supérieure à 2^23 = 8 388 608 octets pour un paquet serverbound : erreur de protocole, avant d'allouer (le client du jeu n'impose pas de limite ; notre client M9 non plus : provisoire, les mêmes 2^23). Le plafond de longueur 2^21 - 1 de R27 s'applique à la longueur compressée. Sonde PN6. | S |
| R31 | Chiffrement : AES-128 en mode CFB avec rétroaction de 8 bits (CFB8). Clé et vecteur initial valent tous deux le secret partagé de 16 octets. Un chiffreur par sens, flux continu : jamais réinitialisé entre les paquets, ni entre les états. Tout est chiffré à partir de la fin de l'échange de clés : longueur, identifiant, longueur des données, charge. L'ordre est "compresser puis chiffrer" à l'émission. Vecteurs CH01 à CH03 (C : chiffrement d'un flux en deux morceaux = chiffrement d'un morceau ; redémarrer le chiffreur change les octets). Provisoire : appliquer comme écrit. Seul le L4 en ligne exerce cette règle de bout en bout : PN9 s'arrête avant la réponse `key`. | D (AES/CFB8) / S (usage) |
| R32 | Moment : le serveur, après avoir reçu et validé `key`, active le déchiffrement de ce qu'il reçoit et le chiffrement de ce qu'il envoie ; le premier paquet chiffré qu'il envoie est `login_compression` si présent, sinon `login_finished`. Le client active ses deux chiffreurs juste après avoir envoyé `key` : tous les octets qu'il envoie ensuite sont chiffrés. Provisoire : appliquer comme écrit. Seul le L4 en ligne exerce cette règle : PN9 s'arrête avant la réponse `key`. | S |
| R33 | Un paquet de lecture partielle (trame tronquée, I11) laisse la connexion en attente ; provisoire : délai de lecture de l'état (R40, R47) puis fermeture. Un client qui n'envoie rien après le handshake est coupé par le délai de l'état. | S |

### États, transitions, séquences

| Réf | Règle | Preuve |
|---|---|---|
| R34 | Toute connexion démarre en handshake, qui n'accepte que `intention` (identifiant 0). Champs : version de protocole VarInt, adresse String(255), port Unsigned Short, intention VarInt (1 status, 2 login, 3 transfert). Les intentions 2 et 3 mènent à login ; 1 mène à status. Vecteurs F01, F04. Provisoire : implémenter tel quel ; PN2 et PN6 la lèvent. | S |
| R35 | Intention inconnue (I07), ou premier octet `fe` suivi d'un octet (ping hérité, I12) : provisoire, connexion fermée sans réponse ; le ping hérité est un trou de compatibilité assumé en M8 (Q8). Sonde PN10. | S |
| R36 | Version de protocole du handshake différente de 777 : en intention status, le serveur répond normalement, avec `version.protocol` = 777 (le client affiche "version incompatible"). En intention login, provisoire : `login_disconnect` avec un composant JSON `{"translate":"multiplayer.disconnect.outdated_client","with":["26.3"]}` si la version du client est inférieure à 777, `outdated_server` si elle est supérieure (clés D, texte "Incompatible client! Please use %s" pour les deux), puis fermeture ; le choix de clé est une hypothèse. La valeur -1 du champ (version inconnue) est admise en status. Sonde PN6. Vecteurs I08, I09. | S |
| R37 | Status : le client envoie `status_request` (une seule fois, tout de suite après le handshake) et reçoit `status_response` ; il peut envoyer `ping_request` (Long) et reçoit `pong_response` avec la même valeur, après quoi le serveur ferme la connexion. `ping_request` sans `status_request` est admis. Un second `status_request` : provisoire, connexion fermée (clé D `disconnect.ignoring_status_request`). Délai d'attente du ping : 30 s (documentation). Vecteurs F02, F03, F16. Sondes PN2. | S |
| R38 | JSON de statut : objet avec `version` {`name` String, `protocol` entier}, `players` (optionnel) {`max`, `online` entiers, `sample` optionnel tableau de {`name`, `id` UUID texte}}, `description` (composant de texte JSON), `favicon` (optionnel, `data:image/png;base64,` + PNG de 64 par 64 pixels exactement, sans saut de ligne), `enforcesSecureChat` (booléen). `sample` est omis quand personne n'est connecté ou quand `hide-online-players` est vrai. Provisoire : le serveur écrit exactement ces clés. Les noms de clés sont fixés par la documentation et par leur lecture par les clients. Sonde PN2. | S |
| R39 | Login, hors ligne (R57) : `hello` (serverbound, nom String(16) + UUID) ; [optionnel] `login_compression` ; `login_finished` ; le client répond `login_acknowledged` ; la connexion passe en configuration. Le serveur n'envoie aucun paquet de configuration avant `login_acknowledged` et ne lit les paquets suivants du client comme de la configuration qu'après l'avoir reçu. Vecteurs F05, F06. Provisoire : appliquer ; PN3 la lève. | S |
| R40 | Login, en ligne (R50 à R56) : `hello` ; `hello` clientbound (demande de chiffrement) ; `key` ; chiffrement activé ; [optionnel] `login_compression` ; `login_finished` ; `login_acknowledged`. Un délai total de connexion existe (clé D `multiplayer.disconnect.slow_login`, "Took too long to log in") ; sa durée n'est pas documentée, provisoire 30 s. Sonde PA4. | S |
| R41 | Paquets `custom_query` (clientbound) et `custom_query_answer` (serverbound) : la réponse porte le même identifiant de message. Le client du jeu répond qu'il n'a pas compris (donnée absente) ; provisoire : le serveur n'en envoie pas en M8 et traite une `custom_query_answer` inattendue comme une erreur de protocole (clé D `multiplayer.disconnect.unexpected_query_response`). PN6 (`custom_query_answer` non sollicitée en login) la lève. | S |
| R42 | Cookies (`cookie_request`, `cookie_response`, `store_cookie`) : persistent d'un transfert à l'autre ; charge d'au plus 5120 octets. Provisoire : M8 ne les envoie pas ; une `cookie_response` non sollicitée est ignorée. PN6 (`cookie_response` non sollicitée en configuration) la lève. | S |
| R43 | Configuration, séquence documentée : [client] `custom_payload` (canal `minecraft:brand`) et `client_information` ; [serveur] `custom_payload` brand, `update_enabled_features`, [optionnel] `select_known_packs`, un `registry_data` par registre, `update_tags`, puis `finish_configuration` ; [client] `finish_configuration` (accusé) ; passage en play. Le serveur attend la réponse `select_known_packs` s'il l'a demandée. Provisoire : le serveur envoie brand puis `update_enabled_features`, ne demande pas de paquets connus (R63), envoie les registres (R62) et les tags (R66), puis `finish_configuration`, et passe en play à l'accusé. Sondes PN4, PN13. | S |
| R44 | Entrée en jeu, séquence documentée (client vanilla ; le nombre et l'ordre exacts sont ceux que relève PN5) : `login` ; `change_difficulty` ; `player_abilities` ; `set_held_slot` ; `update_recipes` ; `entity_event` (niveau de permission) ; `commands` ; `recipe_book_add` ; `player_position` ; [client] `accept_teleportation` puis `move_player_pos_rot` ; `server_data` ; `player_info_update` (autres joueurs puis le joueur) ; `initialize_border` ; `set_time` ; `set_default_spawn_position` ; `game_event` (événement 13, début d'attente des chunks) ; `ticking_state` ; `ticking_step` ; `set_chunk_cache_center` ; chunks (`level_chunk_with_light`) ; [client] `player_loaded` ; puis inventaire, entités, etc. Provisoire : le serveur envoie, dans cet ordre, au moins `login`, `player_abilities`, `set_held_slot`, `commands`, `player_position`, `player_info_update`, `initialize_border`, `set_time`, `set_default_spawn_position`, `game_event` 13, `set_chunk_cache_center`, les lots de chunks, `set_health`, `set_experience`, l'inventaire. Les paquets marqués optionnels dans la documentation ne sont ajoutés que si la sonde PN5 montre qu'un client vanilla les exige. | S |
| R45 | Reconfiguration : le serveur envoie `start_configuration` (play) ; le client répond `configuration_acknowledged` ; après cet accusé, le serveur lit et envoie les paquets de configuration ; `finish_configuration` / accusé ramènent en play. Si des `registry_data` sont envoyés pendant une reconfiguration, tous les tags et registres précédents sont oubliés. Provisoire : M8 n'utilise pas la reconfiguration, mais le décodage de `configuration_acknowledged` (play, 16) et de `start_configuration` (play, 120) est implémenté. Sonde PN13. | S |
| R46 | Transfert (intention 3) : accepté seulement si `accepts-transfers` (`server.properties`) est vrai, sinon provisoire : `login_disconnect` avec la clé D `multiplayer.disconnect.transfers_disabled`. Le paquet `transfer` (configuration et play) demande au client de fermer et de rouvrir vers {hôte, port} avec l'intention 3. Sonde PN11. | S |
| R47 | Keep-alive (configuration et play, `keep_alive` des deux sens, Long) : le serveur envoie régulièrement un identifiant (le jeu utilise l'heure système en millisecondes) ; le client renvoie la même valeur. Sans réponse en 15 s le serveur expulse ; sans keep-alive reçu en 20 s le client se déconnecte ("Timed out", clé D `disconnect.timeout`). L'intervalle d'émission du serveur n'est pas documenté. Provisoire : émission toutes les 15 s, expulsion à 15 s sans réponse, une seule valeur en vol. Sonde PN8. | S |
| R48 | `ping` / `pong` (configuration et play) : `ping` (Int) du serveur, `pong` du client avec le même Int. Non utilisé par le serveur du jeu. `ping_request` / `pong_response` en play (Long) : réponse immédiate avec la même valeur ; forme du champ non relevée sur la page, supposée identique à status. Provisoire : M8 n'envoie pas `ping`, ignore un `pong` non sollicité sans erreur et répond à `ping_request` (play) par `pong_response` de même valeur. PN8 la lève (`ping_request` envoyé en jeu, `pong` non sollicité). | S |
| R49 | Déconnexion : `login_disconnect` (login, composant JSON) ; `disconnect` (configuration et play, composant de texte NBT). Le serveur envoie le paquet puis ferme après l'envoi. Les raisons du serveur sont des composants `translate` avec les clés D de `multiplayer.disconnect.*` (annexe de `net-protocol-777-vectors.md`, tableau Z). Un paquet reçu dans un mauvais état ou d'identifiant inconnu : provisoire, fermeture de la connexion sans message, dans tous les états, tant que la sonde PN6 n'a pas tranché. | S |

### Authentification et mode hors ligne

| Réf | Règle | Preuve |
|---|---|---|
| R50 | `online-mode` vrai (défaut) : le serveur envoie `hello` clientbound avec `server_id` vide, la clé publique, un jeton de vérification et `should_authenticate` vrai. Clé RSA de 1024 bits générée au démarrage, publiée en SubjectPublicKeyInfo DER (162 octets pour RSA-1024 avec l'exposant 65537, CH04). Longueur du jeton : 4 octets (provisoire, non documentée ; elle n'est vérifiée que par le serveur, qui compare ce qu'il reçoit à ce qu'il a émis). Sonde PN9. | D (RSA, X.509) / S (usage) |
| R51 | `key` : le secret partagé (16 octets aléatoires du client) et le jeton, chacun chiffré avec la clé publique du serveur en RSA PKCS#1 v1.5 : 128 octets chacun pour une clé de 1024 bits (CH05, CH06). Le serveur déchiffre avec sa clé privée, compare le jeton à celui émis, refuse un secret dont la longueur n'est pas 16 octets (provisoire). Jeton faux, déchiffrement impossible : provisoire, fermeture sans message. PN9 observe les longueurs de la demande de chiffrement ; la réponse `key` n'est exercée que par le L4 en ligne. | D (PKCS#1) / S |
| R52 | Empreinte de session (serverHash) : SHA-1 de la concaténation de l'identifiant de serveur (octets ASCII, vide), du secret partagé et de la clé publique DER ; le résultat est lu comme un entier signé de 160 bits en complément à deux, écrit en hexadécimal minuscule avec un `-` devant s'il est négatif, sans zéros de tête (le zéro de tête d'un résultat positif disparaît). Vecteurs H01 à H03 : `Notch` -> `4ed1f46bbe04bc756bcb17c0c7ce3e4632f06a48`, `jeb_` -> `-7c9d5b0044c130109a5d7b5fb5c317c02b4e28c1`, `simon` -> `88e16a1019277b15d58faf0541e11910eb756f6` (39 chiffres). | D (SHA-1) / C (format) |
| R53 | Vérification : le client prouve sa connexion en envoyant à l'API de sessions (POST `https://sessionserver.mojang.com/session/minecraft/join`, corps JSON `accessToken`, `selectedProfile` (UUID sans tirets), `serverId` (R52), type `application/json`, réponse 204). Le serveur interroge GET `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=<nom>&serverId=<R52>` (+ `&ip=<adresse>` seulement si `prevent-proxy-connections` est vrai). Réponse 200 avec JSON {`id` 32 hexadécimaux sans tirets, `name`, `properties` [{`name`, `value`, `signature`}]} ; réponse 204 : échec. Le nom de la réponse fait foi (la casse de `username` n'a pas d'importance). Provisoire : les propriétés (textures) sont relayées dans `login_finished` telles quelles. Les deux URL sont des données de configuration du serveur, pas des constantes du protocole. Sonde non automatisable (compte requis, PN9). | S |
| R54 | UUID en ligne : l'identifiant `id` de la réponse, converti en forme 8-4-4-4-12, est celui de `login_finished`. Provisoire : appliquer comme écrit. Seul le L4 en ligne la lève : PN9 s'arrête avant `hasJoined`. | S |
| R55 | Échec d'authentification : provisoire, `login_disconnect` avec la clé D `multiplayer.disconnect.unverified_username` ("Failed to verify username!") quand `hasJoined` répond 204 ; avec `multiplayer.disconnect.authservers_down` quand le service est injoignable ; dans les deux cas fermeture ensuite. Seul le L4 en ligne (compte du propriétaire) la lève : la réponse du service de sessions n'est pas simulable. | S |
| R56 | Mojang : l'accès à l'API de sessions dépend du réseau et d'un compte ; le serveur s'abstient de la contacter dès que `online-mode` est faux. La validation des signatures de chat repose sur des clés publiques de Mojang : hors périmètre M8 (Q4). Provisoire : en ligne, `enforce-secure-profile` est lu mais la vérification de signature n'est pas faite. PN13 (`chat` non signé accepté hors ligne) la lève pour la partie hors ligne ; la vérification en ligne relève du L4 en ligne et de Q4. | S |
| R57 | `online-mode` faux : aucun chiffrement, aucune requête de session. Le serveur répond à `hello` (serverbound) par [`login_compression`] puis `login_finished` directement. Le UUID du profil est le UUID "hors ligne" du nom, pas celui que le client a envoyé. Provisoire : appliquer ; PN3 la lève. | S |
| R58 | UUID hors ligne : MD5 des octets UTF-8 de `"OfflinePlayer:" + nom` (16 octets) ; le quatrième nibble de l'octet 6 devient 3 (`(b6 & 0x0f) \| 0x30`), les deux bits de poids fort de l'octet 8 deviennent `10` (`(b8 & 0x3f) \| 0x80`) ; lu comme UUID de 16 octets (R11). Vecteurs U01 à U05 : `Notch` -> `b50ad385-829d-3141-a216-7e7d7539ba7f`, `jeb_` -> `a762f560-4fce-3236-812a-b80efff0b62b`, `Steve` -> `5627dd98-e6be-3c21-b8a8-e92344183641`. Le nom est sensible à la casse (`a` et `A` donnent des UUID différents, U04, U05). Provisoire : appliquer ; PA1 et PN3 la lèvent. | D (MD5) / S (usage ; la valeur de `Notch` est celle publiée par la documentation) |
| R59 | Validité du nom en hors ligne : le champ est une String(16). Provisoire : le serveur accepte un nom de 1 à 16 unités de code UTF-16 sans espace ni caractère de contrôle ; un nom plus long est refusé par R9 (I10). La règle du jeu (jeu de caractères, longueur minimale) n'est pas documentée. Sonde PA2. | S |
| R60 | Contrôles d'accès à la connexion, ordre et messages provisoires, clés D de `multiplayer.disconnect.*` : liste blanche active et joueur absent -> `not_whitelisted` ; joueur banni -> `banned` (avec `banned.reason` et `banned.expiration` quand il y en a) ; IP bannie -> `ip_banned` ou `banned_ip.reason` ; serveur plein (max-players, sauf joueur dont `bypassesPlayerLimit` est vrai) -> `server_full` ; même UUID déjà connecté -> la connexion existante est fermée avec `duplicate_login` et la nouvelle est acceptée ; même nom avec un autre UUID -> `name_taken`. L'ordre entre ces contrôles et leur moment (avant `login_finished`, ou en configuration) ne sont pas documentés. Sonde PA3, PA4. | S |

### Registres, tags et configuration

| Réf | Règle | Preuve |
|---|---|---|
| R61 | Registres statiques (non envoyés, identifiants fixés par le jeu) : les identifiants de protocole de `registries.json` (D) : `minecraft:block` 1286 entrées, `minecraft:item` 1658, `minecraft:entity_type` 161, `minecraft:mob_effect` 40, `minecraft:attribute` 40, `minecraft:menu` 25, `minecraft:particle_type` 128, `minecraft:sound_event` 1991, `minecraft:potion` 46, `minecraft:game_event` 61, `minecraft:data_component_type` 122, `minecraft:command_argument_type` 62, `minecraft:game_rule` 59, `minecraft:debug_subscription` 16, `minecraft:recipe_display` 5, `minecraft:slot_display` 11, `minecraft:villager_profession` 15, `minecraft:block_entity_type` 49, `minecraft:map_decoration_type` 40, `minecraft:custom_stat` 78. Les identifiants d'état de bloc globaux sont contigus de 0 à 35 722 (35 723 états), rangés par identifiant de bloc croissant puis par ordre des propriétés dans `blocks.json` (D, vérifié sur l'ensemble). Commande : `cmd_ref.py registry NOM`, `python -I net_ref.py ids` pour l'état de bloc. | D |
| R62 | Registres synchronisés (un `registry_data` chacun, pendant la configuration) : les 29 registres que la documentation publique liste : `banner_pattern`, `chat_type`, `damage_type`, `dialog`, `dimension_type`, `enchantment`, `instrument`, `jukebox_song`, `painting_variant`, `sulfur_cube_archetype`, `test_environment`, `test_instance`, `timeline`, `trim_material`, `trim_pattern`, `world_clock`, `worldgen/biome`, `cat_variant`, `cat_sound_variant`, `chicken_variant`, `chicken_sound_variant`, `cow_variant`, `cow_sound_variant`, `frog_variant`, `pig_variant`, `pig_sound_variant`, `wolf_variant`, `wolf_sound_variant`, `zombie_nautilus_variant` (tous sous `minecraft:`). Recoupement avec les données (C) : les 29 sont dans `datapack.json` avec `elements: true` et `stable: false`. `datapack.json` contient 23 autres registres de même nature (`block_transformer`, `decorated_pot_pattern`, `dimension`, `enchantment_provider`, `trade_set`, `trial_spawner`, `villager_trade`, `worldgen/*` hors biome) dont la synchronisation n'est pas établie. Provisoire : on synchronise exactement les 29. Nombres d'entrées du jeu de données vanilla (D, comptage de `resources/data/minecraft/<registre>/**/*.json`) : banner_pattern 43, cat_sound_variant 2, cat_variant 11, chat_type 7, chicken_sound_variant 2, chicken_variant 3, cow_sound_variant 2, cow_variant 3, damage_type 51, dialog 3, dimension_type 4, enchantment 43, frog_variant 3, instrument 8, jukebox_song 22, painting_variant 51, pig_sound_variant 3, pig_variant 3, sulfur_cube_archetype 12, test_environment 1, test_instance 1, timeline 4, trim_material 11, trim_pattern 18, wolf_sound_variant 7, wolf_variant 9, world_clock 2, worldgen/biome 67, zombie_nautilus_variant 2. Sonde PN4. | S (liste) / D (comptes) |
| R63 | Entrées : un `registry_data` porte un registre ; l'ordre des entrées donne leurs identifiants numériques de 0 à n-1, que toutes les références ultérieures (dimension du paquet `login`, biomes des chunks, variantes en métadonnées) utilisent. Une entrée sans données (« a des données » faux) est reprise d'un pack connu du client. Les données d'une entrée sont le NBT équivalent du JSON du datapack. Un client qui reçoit la référence d'une entrée inexistante se déconnecte. Provisoire : le serveur envoie toujours les données de chaque entrée (aucune entrée sans données) et ne demande pas de paquets connus : valide quel que soit le client (« le client utilise le NBT du paquet quand il est présent »). Il choisit l'ordre des entrées (par exemple ordre lexicographique de l'identifiant) et le garde pour toute la session. Le seul pack connu du client est `minecraft:core` (identifiant `core`, espace de noms `minecraft`, version supposée `26.3`). Sonde PN4. | S |
| R64 | Conversion JSON vers NBT d'une entrée : le jeu code le JSON par un codec ; le client le relit avec le codec miroir. Provisoire : nombre entier JSON -> Int, nombre à virgule -> Double, booléen -> Byte 0/1, texte -> String, tableau -> List, objet -> Compound, sans enveloppe de liste hétérogène (une liste JSON de types mixtes est une erreur de données). Le choix Int/Double ne devrait pas gêner un codec qui lit un nombre quelconque. Sonde PN4 (compare les types d'étiquettes aux octets du serveur de référence, entrée par entrée). | S |
| R65 | `update_enabled_features` : `minecraft:vanilla` (et les identifiants des datapacks de fonctionnalités activés, `datapacks/` du dossier `resources/data/minecraft/` : `minecart_improvements`, `redstone_experiments`, `trade_rebalance`). Provisoire : `["minecraft:vanilla"]` seul. PN4 la lève. | S |
| R66 | Tags : `update_tags` porte, par registre, des tags {nom, liste d'identifiants numériques du registre}. `datapack.json` déclare 155 registres qui acceptent des tags : les 95 registres statiques de `registries.json` et 60 registres pilotés par les données (D, comptage). Les tags ne viennent jamais des packs connus : ils sont toujours envoyés en entier. Les identifiants sont ceux de R61 (statiques) ou de l'ordre de R63 (synchronisés). Provisoire : tous les tags des 95 registres statiques et des 29 registres synchronisés (R62) ; les tags des autres registres pilotés par les données (`loot_table`, `recipe`, `advancement`, `predicate`...) restent côté serveur (le jeu ne les synchronise pas : hypothèse). Sonde PN4. | S |
| R67 | Dimensions : le paquet `login` porte l'identifiant de dimension de type (indice dans le registre synchronisé `dimension_type`), le nom de la dimension, la liste des noms de dimension, la graine hachée (Long, premiers octets du SHA-256 de la graine selon la documentation), et le niveau de la mer. Les types de dimension de 26.3 (D, `dimension_type/*.json`) : `overworld` hauteur 384, y minimal -64, 24 sections ; `overworld_caves` 384, -64, 24 ; `the_nether` 256, 0, 16, échelle de coordonnées 8 ; `the_end` 256, 0, 16. Vecteur D01. Provisoire : écrire les champs dans cet ordre ; PN5 (champs de `login`) la lève. | D (types) / S (champs) |

### Monde et chunks

| Réf | Règle | Preuve |
|---|---|---|
| R68 | `level_chunk_with_light` : Int chunkX, Int chunkZ, heightmaps (tableau préfixé de {type VarInt, Long[] préfixé}), données de sections (tableau d'octets préfixé), block entities (tableau préfixé de {XZ compressé Unsigned Byte `((x&15)<<4)\|(z&15)`, y Short, type VarInt, NBT sans X, Y, Z}), données de lumière (R24). Les sections se suivent de bas en haut, sans préfixe de nombre (leur nombre = hauteur / 16 de la dimension, R67). Une section : Short nombre de blocs non air, Short nombre de fluides (champ relevé sur la page "Chunk format", absent des anciennes versions), conteneur paletté de 4096 états de blocs, conteneur paletté de 64 biomes. Provisoire : le nombre de fluides compte les blocs contenant un fluide (eau, lave, blocs gorgés d'eau) ; sonde PN5 le confirme sur une section connue. | S |
| R69 | Conteneur paletté : Unsigned Byte "bits par entrée", palette, tableau de Long. Blocs : 0 = valeur unique (palette = un VarInt, aucun Long) ; 4 à 8 = palette indirecte (tableau préfixé de VarInt, valeurs du tableau = indices) ; au-delà de 8 = direct (identifiants d'état globaux, pas de palette), avec `ceil(log2(taille du registre))` bits : 16 pour les blocs de 26.3 (35 723 états > 2^15 = 32 768, D et C ; les 15 bits de la documentation publique datent d'un registre plus petit). Biomes : 0 = unique ; 1 à 3 = indirect ; au-delà de 3 = direct avec `ceil(log2(taille du registre de biomes synchronisé))` bits : 7 pour les 67 biomes de R62. Un nombre de bits intermédiaire est relevé à la valeur supérieure. Provisoire : le serveur écrit le conteneur unique, indirect tant que la palette tient en 256 états (blocs) ou 8 biomes, direct sinon, avec les largeurs ci-dessus ; PN5 mesure l'octet de bits par entrée d'un chunk dense (section de plus de 256 états distincts, plus de 8 biomes) et lève la règle. Le tableau de Long n'est pas précédé de sa longueur depuis 1.21.5 : elle se déduit du nombre de bits et du nombre d'entrées. Entrées empaquetées du poids faible vers le poids fort, sans chevauchement de deux Long : `floor(64/bits)` entrées par Long, bits inutilisés à zéro, `ceil(n / par_long)` Long. Ordre des entrées : x varie le plus vite, puis z, puis y. Vecteurs K01 à K11. | S (format) / C (nombres) |
| R70 | Cartes de hauteur du paquet : type VarInt (1 `WORLD_SURFACE`, 4 `MOTION_BLOCKING`, 5 `MOTION_BLOCKING_NO_LEAVES` lus par le client), 256 entrées (une par colonne) de `ceil(log2(hauteur du monde + 1))` bits chacune : 9 bits pour 256 et 384, soit 7 entrées par Long et 37 Long (K08 à K10). Les cartes absentes sont initialisées au minimum. Provisoire : 9 bits par entrée pour les hauteurs 256 et 384 ; PN5 (cartes de hauteur d'un chunk) la lève. | S (format) / C (nombres) |
| R71 | Lots de chunks : `chunk_batch_start`, N paquets `level_chunk_with_light`, `chunk_batch_finished` (N en VarInt) ; le client mesure la durée du lot et répond `chunk_batch_received` avec le nombre de chunks par tick qu'il souhaite (Float). Le serveur n'envoie le lot suivant qu'après cette réponse et tolère jusqu'à 10 lots non acquittés. Provisoire : un lot par tick de serveur au plus, au plus 10 lots en vol, au plus 8 chunks par lot avant la première réponse du client (valeur arbitraire, non documentée, à lever par PN5). | S |
| R72 | Décharge : `forget_level_chunk` (Int Z puis Int X, dans cet ordre) est valide même si le chunk n'est pas chargé. Provisoire : le serveur envoie les chunks par distance croissante au centre (`set_chunk_cache_center`) ; le jeu ne documente pas l'ordre (sonde PN5). `set_chunk_cache_radius` (VarInt 2 à 32) donne la distance de vue. `set_simulation_distance` ne sert au client qu'à geler des animations de mort. | S |
| R73 | Fin du chargement : le client affiche l'écran de chargement jusqu'à `game_event` événement 13 puis jusqu'à avoir reçu des chunks, et envoie `player_loaded` ; sinon il l'envoie au bout de 60 ticks. Le serveur ne s'appuie pas sur `player_loaded` pour la simulation. Provisoire : appliquer ; PN5 la lève (avec et sans `player_loaded`). | S |
| R74 | Mise à jour de blocs : `block_update` (Position + identifiant d'état global VarInt) ; `section_blocks_update` (section = Long avec x 22 bits aux bits 42+, z 22 bits aux bits 20 à 41, y 20 bits aux bits 0 à 19 ; puis tableau préfixé de VarLong `état<<12 \| x<<8 \| z<<4 \| y`, coordonnées locales de 0 à 15). Vecteurs S01 à S03, B01. Provisoire : implémenter tel quel ; PN5 (`block_update` d'un bloc posé par `setblock`) et PN12 (`section_blocks_update` produit par un `fill` dans une section) la lèvent. | S |
| R75 | Prédiction de bloc : `player_action` (début de minage, annulation, fin, ...), `use_item_on` et `use_item` portent une séquence VarInt ; le serveur répond `block_changed_ack` avec la dernière séquence traitée, après les `block_update` des blocs modifiés ; le client montre alors l'état du serveur à la place de sa prédiction. Un refus du serveur est un `block_update` de l'état réel suivi de l'accusé. Provisoire : appliquer ; PN12 la lève. | S |
| R76 | Position du joueur : `player_position` (identifiant de téléportation, position, vitesse, lacet, tangage, drapeaux R19) du serveur ; le client répond `accept_teleportation` avec le même identifiant (et la position/orientation obtenues) ; tant que l'accusé n'est pas arrivé, le serveur ignore les paquets de déplacement du client. Mouvements du client : `move_player_pos`, `move_player_pos_rot`, `move_player_rot`, `move_player_status_only` (drapeaux : `01` au sol, `02` contre un mur). Y est la position des pieds. Le serveur valide la plausibilité du mouvement (gamerule `player_movement_check`, D) puis diffuse ; les seuils de validation ne sont pas documentés. Provisoire : M8 accepte tout mouvement dont la distance par tick est inférieure à une borne configurable, sinon renvoie `player_position` ; sonde PN12 mesure les rejets du jeu. | S |
| R77 | Diffusion de position des autres joueurs : `move_entity_pos`, `move_entity_pos_rot`, `move_entity_rot` (variations de position en Short avec 12 bits de fraction, c'est-à-dire en 1/4096 de bloc, bit 0 de la propriété = au sol, bits suivants = nombre de pas), `entity_position_sync` et `teleport_entity` (absolus), `rotate_head`. Provisoire : M8 envoie `entity_position_sync` à chaque changement de position d'un joueur distant ; les variations relatives sont une optimisation du suivi d'entités (M7). Le suivi réellement fait par le jeu (portée, intervalle) est S (rapport d'architecture, 12.3, 12.4). PN12 (un second client de sonde déplacé, paquets de diffusion reçus par le premier) la lève. | S |
| R78 | Temps : `set_time` toutes les 20 ticks (documentation, boucle serveur) : âge du monde (Long) puis tableau d'horloges {identifiant du registre `world_clock`, temps VarLong, fraction Float, vitesse Float}. 26.3 gère le temps par horloges (`overworld`, `the_end`, D). Provisoire : appliquer ; PN5 (`set_time` toutes les 20 ticks, valeurs) la lève. | S |
| R79 | Chat : le client envoie `chat` (message String(256), horodatage, sel, signature optionnelle, compteur, accusés BitSet fixe(20), somme de contrôle) ou `chat_command` (commande sans `/`, String(32767)) ou `chat_command_signed` (arguments signés). Le serveur répond `player_chat` (message signé), `disguised_chat` (sans signature : `/say`, `/msg`, `/me`) ou `system_chat` (texte système, option "barre d'action"). Provisoire : le serveur accepte `chat_session_update` sans vérifier la clé, traite `chat` comme non signé et diffuse `player_chat` sans signature ; `chat_command` et `chat_command_signed` exécutent la commande (`server-commands.md` R19, R20). Sonde PN13. | S |
| R80 | Suggestions : `command_suggestion` (identifiant de transaction VarInt, texte String(32500) jusqu'au curseur, `/` compris) -> `command_suggestions` (même identifiant, début, longueur, correspondances avec info-bulle optionnelle). Le client trie les correspondances. `server-commands.md` R18 pour le contenu. Provisoire : appliquer ; PN13 (`command_suggestion` `/ti`) et PX8 la lèvent. | S |
| R81 | Permissions côté client : `entity_event` avec un statut de niveau de permission indique le niveau du joueur à son client (les valeurs de statut sont à relever : sonde PX1) ; `commands` fixe le drapeau "restreint" de chaque nœud. Provisoire : voir `server-commands.md` R9 ; sonde PX1. | S |
| R82 | Joueurs : `player_info_update` (masque d'actions Unsigned Byte puis par joueur son UUID et une charge par bit posé, dans l'ordre des bits : ajout, session de chat, mode de jeu, listé, latence, nom affiché, priorité, chapeau) ; `player_info_remove` (UUID[]). Un joueur doit être listé avant que ses entités ne soient créées côté client. Provisoire : on envoie l'ajout (nom et propriétés), le mode de jeu, "listé" vrai et la latence. PN5 la lève. | S |
| R83 | Entité joueur distant : `add_entity` (identifiant d'entité VarInt, UUID, type VarInt du registre `entity_type`, position Double x3, vitesse LpVec3, tangage, lacet, lacet de tête (Angle), donnée VarInt) ; `set_entity_data` ; `remove_entities`. Ne pas l'utiliser pour le joueur local. Les identifiants d'entité remplacent ceux déjà connus du client. Provisoire : appliquer ; PN12 (second client de sonde) la lève. | S |
| R84 | Jeux de caractères de chat, barre de statut et textes : toute chaîne est du texte Unicode ; les composants de texte portent leur style. Hors périmètre M8 : `title`, `boss_event`, `scoreboard` (forme seulement). Provisoire : M8 n'envoie aucun de ces paquets et décode leur forme (R97, R98) ; sans effet observable sur le critère M8, acceptée telle quelle. | S |

### Cas limites de transport

| Réf | Règle | Preuve |
|---|---|---|
| R85 | Un paquet de longueur maximale 2^21 - 1 octets est valide ; le serveur n'en émet pas au-dessus (un lot de chunks se découpe en paquets). Un paquet `bundle_delimiter` ouvre et ferme un groupe de paquets traités au même tick par le client ; le client refuse un groupe de plus de 4096 paquets. Provisoire : M8 groupe `add_entity` et ses paquets liés seulement. PN5 (présence de `bundle_delimiter`) et PN6 (trame de 2^21 - 1 octets) la lèvent. | S |
| R86 | Taille des chaînes de configuration : `resource_pack_push` (URL String(32767), hachage String(40) SHA-1 hexadécimal, `forced`), `server_links` (étiquette énumérée 0 à 9 ou texte), `custom_report_details` (au plus 32 couples titre 128 / description 4096), `code_of_conduct` (String). Provisoire : M8 n'envoie aucun de ces paquets, mais décode les réponses `resource_pack` (résultat 0 à 7) et `accept_code_of_conduct`. Sans effet observable sur le critère M8 : acceptée telle quelle ; le décodage des réponses est testé en L1. | S |
| R87 | `custom_payload` (configuration et play) : canal Identifier puis données jusqu'à la fin de la trame. Limites : 1 048 576 octets clientbound et 32 767 octets serverbound, appliquées seulement aux canaux inconnus. Le canal `minecraft:brand` porte une String. Provisoire : le serveur envoie `brand` = chaîne propre au projet (pas la chaîne du jeu) et ignore les autres canaux. PN4 (`brand` envoyé par le serveur de référence) la lève. | S |
| R88 | Chaque `client_information` (configuration, et play si le joueur change ses options) porte : langue String(16), distance de vue Byte, mode de chat VarInt (0 activé, 1 commandes, 2 caché), couleurs Boolean, masque de parties de peau Unsigned Byte, main VarInt (0 gauche, 1 droite), filtrage de texte Boolean, listes de serveurs Boolean, particules VarInt. La distance de vue effective est le minimum de la valeur du client et de `view-distance`. Provisoire : appliquer ; PN4 envoie un `client_information` et PN5 le refait avec des distances de vue 2, 8 et 32 (rayon de `set_chunk_cache_radius`, distance de vue de `login`). | S |
| R89 | Limite de débit : `rate-limit` (`server.properties`, défaut 0 = aucune) expulse un client qui dépasse un nombre de paquets ; clé D `disconnect.exceeded_packet_rate`. Provisoire : M8 n'implémente pas la limite. Sans effet observable en M8 (limite par défaut 0) : acceptée telle quelle. | S |

### Sécurité du décodeur (exigences de robustesse)

| Réf | Règle | Preuve |
|---|---|---|
| R90 | Aucune allocation ne dépend d'une longueur annoncée qui n'a pas été comparée au maximum de son champ et au nombre d'octets restants dans la trame (tableaux préfixés, chaînes, NBT : quota de `core-nbt.md` R32, R60). Un paquet est décodé depuis un tableau d'octets borné à sa trame ; un décodeur qui lit au-delà de la trame, ou laisse des octets non lus, signale une erreur de protocole (provisoire, par prudence : le jeu documente que certains champs « peuvent surestimer » la longueur réelle, mais cela concerne les clients, pas le serveur). Exigence d'ingénierie du décodeur, sans effet observable sur l'oracle : acceptée telle quelle, vérifiée en L1 (fuzzing de graine fixe). | S |
| R91 | Un déni de service par fragmentation (trame jamais complétée), par décompression (rapport de taille extrême) ou par volume est borné par : le délai de lecture de l'état, la limite 2^23 de R30 appliquée avant l'allocation, et un nombre maximal de paquets en attente de traitement par connexion. Provisoire : délai de lecture 30 s en status/login/configuration, valeurs de file configurables. | S |

### Paquets de configuration et d'état : règles par paquet

| Réf | Règle | Preuve |
|---|---|---|
| R92 | `update_tags` (configuration) remplace, tag par tag, la liste de chaque tag cité ; en jeu, seuls les tags cités sont remplacés. Si `finish_configuration` arrive sans `registry_data`, les tags déjà donnés sont conservés sauf s'ils sont renvoyés. Provisoire : appliquer ; PN4 la lève. | S |
| R93 | `finish_configuration` (clientbound, configuration) : le client valide les registres et les tags, répond `finish_configuration` (serverbound, identifiant 3) et passe en play. Le serveur passe en play à la réception de l'accusé, et pas avant. Provisoire : appliquer ; PN4 la lève. | S |
| R94 | `set_default_spawn_position` (dimension, position, lacet, tangage) : valeurs par défaut du client avant réception : (8, 64, 8), angle 0. `change_difficulty` : difficulté 0 à 3 et verrou. `ticking_state` (cadence Float, gelé Boolean) et `ticking_step` (VarInt) reflètent `/tick` côté client. Provisoire : le serveur M8 envoie ces quatre paquets à l'entrée en jeu avec les valeurs du monde ; PN5 compare ordre et octets. | S |
| R95 | `game_event` : événement Unsigned Byte et valeur Float. Événements utilisés en M8 : 3 changement de mode de jeu (0 survie, 1 créatif, 2 aventure, 3 spectateur), 7 niveau de pluie (0 à 1), 8 niveau d'orage (0 à 1), 13 début d'attente des chunks (requis après chaque `login` et chaque `respawn`), 11 réapparition immédiate (0/1), 12 artisanat limité (0/1). Les autres événements (0 à 12) existent ; leur valeur est relevée dans la table de la documentation. Provisoire : M8 n'envoie que les événements 3, 7, 8, 11, 12 et 13 listés ; PN5 (13 après `login`, 3 après `gamemode`, 7 et 8 après `weather`) la lève. | S |
| R96 | `respawn` répète les champs de dimension de `login` et ajoute un masque de données conservées (`01` attributs, `02` métadonnées) ; la mort puis la réapparition passent par `client_command` (action 0). `login` : entier d'entité, hardcore, noms des dimensions, joueurs max (ignoré), distance de vue, distance de simulation, infos de debug réduites (gamerule `reduced_debug_info`, D), écran de réapparition (faux si gamerule `immediate_respawn` vrai), artisanat limité (gamerule `limited_crafting`), type de dimension, nom, graine hachée, mode, mode précédent (0 = aucun, sinon id + 1), debug, plat, lieu de mort optionnel, délai de portail, niveau de la mer, mode en ligne, sécurité du chat imposée. Provisoire : écrire les champs dans cet ordre ; PN5 la lève (`login`, puis `respawn` après un `client_command` d'action 0). | S |

### Structures référencées par les tableaux

| Réf | Règle | Preuve |
|---|---|---|
| R97 | `set_player_team` : nom String, méthode Byte. Méthode 0 (créer) et 2 (modifier) : nom affiché (composant), préfixe (composant), suffixe (composant), visibilité des noms VarInt (0 toujours, 1 jamais, 2 cacher aux autres équipes, 3 cacher à sa propre équipe), collision VarInt (0 toujours, 1 jamais, 2 pousser les autres équipes, 3 pousser sa propre équipe), couleur VarInt (0 à 15 couleurs, 16 obfusqué, 17 gras, 18 barré, 19 souligné, 20 italique, 21 réinitialiser), drapeaux Byte (`01` tir ami, `02` voir les invisibles de l'équipe) ; la méthode 0 ajoute le tableau préfixé des entités (String). Méthode 1 : aucun champ. Méthodes 3 et 4 : tableau préfixé de String. Provisoire : M8 ne crée aucune équipe d'elle-même et décode seulement la forme ; sans effet observable sur le critère M8, acceptée telle quelle jusqu'au cadre des commandes `team` (sonde à écrire alors). | S |
| R98 | Objectifs et scores : `set_objective` mode 0 ou 2 ajoute valeur (composant), type VarInt (0 entier, 1 cœurs), format de nombre optionnel préfixé ; `set_score` : nom d'entité, nom d'objectif, valeur VarInt, nom affiché optionnel, format de nombre optionnel préfixé. Format de nombre : VarInt (0 vide : aucun champ ; 1 stylé : Compound NBT de style ; 2 fixe : composant de texte). `set_display_objective` : emplacement VarInt (0 liste, 1 barre latérale, 2 sous le nom, 3 à 18 barre latérale d'équipe = 3 + couleur) et nom. Provisoire : M8 n'envoie aucun de ces paquets ; sans effet observable sur le critère M8, acceptée telle quelle jusqu'au cadre des commandes `scoreboard` (sonde à écrire alors). | S |
| R99 | Avancement (`update_advancements`) : identifiant du parent optionnel préfixé, affichage optionnel préfixé {titre, description (composants), icône (Slot), cadre VarInt (0 tâche, 1 défi, 2 objectif), drapeaux Int (`01` fond, `02` toast, `04` caché), fond Identifier optionnel si `01`, x Float, y Float}, exigences (tableau préfixé de tableaux préfixés de String : OU dans le sous-tableau, ET entre sous-tableaux), booléen télémétrie. Progrès : tableau préfixé de {critère Identifier, date optionnelle préfixée Long}. Provisoire : M8 n'envoie pas `update_advancements` ; sans effet observable sur le critère M8, acceptée telle quelle jusqu'au jalon M6 (avancements). | S |
| R100 | Marchand (`merchant_offers`) : Trade = {article d'entrée 1 (VarInt identifiant, VarInt nombre, tableau préfixé de composants {type, données}), article de sortie (Slot), article d'entrée 2 optionnel préfixé, désactivé Boolean, utilisations Int, utilisations max Int, XP Int, prix spécial Int, multiplicateur Float, demande Int}. Après le tableau : niveau VarInt (1 à 5), expérience VarInt, villageois régulier Boolean, réapprovisionnement Boolean. La position du bloc "niveau, expérience, régulier, réapprovisionnement" (après le tableau) découle de la lecture de la page publique et reste S. Provisoire : M8 n'envoie pas `merchant_offers` ; sans effet observable sur le critère M8, acceptée telle quelle jusqu'au jalon M7 (marchands). | S |
| R101 | Débogage : le registre `minecraft:debug_subscription` compte 16 entrées (D : `dedicated_server_tick_time` 0, `bees` 1, `brains` 2, `breezes` 3, `goal_selectors` 4, `entity_paths` 5, `entity_block_intersections` 6, `bee_hives` 7, `pois` 8, `redstone_wire_orientations` 9, `village_sections` 10, `raids` 11, `structures` 12, `game_event_listeners` 13, `neighbor_updates` 14, `game_events` 15). `debug_subscription_request` (VarInt[]) remplace l'ensemble demandé ; les paquets `debug/*` et `debug_sample` n'ont d'effet que pour les opérateurs. Provisoire : M8 ignore `debug_subscription_request`, n'envoie aucun paquet `debug/*` ni `debug_sample`. Formes des charges : documentation publique, non reprises. Sans effet observable en M8 : acceptée telle quelle. | D (registre) / S |
| R102 | Formes non relevées ici et hors M8 : affichage de recette et de slot (registres `recipe_display` 5 entrées et `slot_display` 11, D), données de particules (`particle_type` 128 entrées), charge de `explode`, pas de déplacement de `entity_position_sync` en mode "stepped", `move_minecart_along_track`, `map_item_data`. Provisoire : le serveur de M8 n'envoie pas ces paquets ; `update_recipes` est envoyé vide (aucun ensemble de propriétés, aucune recette de tailleur de pierre). Questions Q10, Q11. PN5 lève la partie `update_recipes` vide ; le reste est sans effet observable en M8. | S |

## Paquets

Colonnes : identifiant décimal et hexadécimal, nom, champs. L'identifiant et le nom sont D (`packets.json`, R2). Les champs sont S (R3). Notation : `Type` simple ; `Tableau<T>` = tableau préfixé ; `Optionnel<T>` = optionnel préfixé ; `(n)` = maximum ; `UByte`/`UShort` non signés ; `Varies` = forme dépendant d'un type annoncé plus tôt.

#### handshake, serverbound (1 paquet)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `intention` | protocol_version:VarInt; server_address:String(255); server_port:UShort; intent:VarInt (1 status, 2 login, 3 transfer) |

#### status, clientbound (2 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `status_response` | json:String(32767) |
| 1 | 0x01 | `pong_response` | timestamp:Long (echo) |

#### status, serverbound (2 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `status_request` | (aucun) |
| 1 | 0x01 | `ping_request` | timestamp:Long |

#### login, clientbound (6 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `login_disconnect` | reason:JsonTextComponent |
| 1 | 0x01 | `hello` | server_id:String(20) (vide); public_key:PrefixedArray<Byte>; verify_token:PrefixedArray<Byte>; should_authenticate:Boolean |
| 2 | 0x02 | `login_finished` | profile:GameProfile; session_id:UUID |
| 3 | 0x03 | `login_compression` | threshold:VarInt (negatif = desactive) |
| 4 | 0x04 | `custom_query` | message_id:VarInt; channel:Identifier; data:ByteArray (reste de la trame) |
| 5 | 0x05 | `cookie_request` | key:Identifier |

#### login, serverbound (5 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `hello` | name:String(16); player_uuid:UUID |
| 1 | 0x01 | `key` | shared_secret:PrefixedArray<Byte> (RSA); verify_token:PrefixedArray<Byte> (RSA) |
| 2 | 0x02 | `custom_query_answer` | message_id:VarInt; data:PrefixedOptional<ByteArray> |
| 3 | 0x03 | `login_acknowledged` | (aucun) |
| 4 | 0x04 | `cookie_response` | key:Identifier; payload:PrefixedOptional<PrefixedArray<Byte>(5120)> |

#### configuration, clientbound (21 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `cookie_request` | key:Identifier |
| 1 | 0x01 | `custom_payload` | channel:Identifier; data:ByteArray |
| 2 | 0x02 | `disconnect` | reason:TextComponent |
| 3 | 0x03 | `finish_configuration` | (aucun) |
| 4 | 0x04 | `keep_alive` | id:Long |
| 5 | 0x05 | `ping` | id:Int |
| 6 | 0x06 | `reset_chat` | (aucun) |
| 7 | 0x07 | `registry_data` | registry:Identifier; entries:PrefixedArray<{id:Identifier; data:PrefixedOptional<NBT>}> |
| 8 | 0x08 | `resource_pack_pop` | uuid:PrefixedOptional<UUID> |
| 9 | 0x09 | `resource_pack_push` | uuid:UUID; url:String(32767); hash:String(40); forced:Boolean; prompt:PrefixedOptional<TextComponent> |
| 10 | 0x0A | `post_effects` | effects:PrefixedArray<Identifier> |
| 11 | 0x0B | `store_cookie` | key:Identifier; payload:PrefixedArray<Byte>(5120) |
| 12 | 0x0C | `transfer` | host:String(32767); port:VarInt |
| 13 | 0x0D | `update_enabled_features` | features:PrefixedArray<Identifier> |
| 14 | 0x0E | `update_tags` | registries:PrefixedArray<{registry:Identifier; tags:PrefixedArray<{name:Identifier; entries:PrefixedArray<VarInt>}>}> |
| 15 | 0x0F | `select_known_packs` | packs:PrefixedArray<{namespace:String; id:String; version:String}> |
| 16 | 0x10 | `custom_report_details` | details:PrefixedArray(32)<{title:String(128); description:String(4096)}> |
| 17 | 0x11 | `server_links` | links:PrefixedArray<{label:Either<VarInt enum 0..9, TextComponent>; url:String(32767)}> |
| 18 | 0x12 | `clear_dialog` | (aucun) |
| 19 | 0x13 | `show_dialog` | dialog:NBT |
| 20 | 0x14 | `code_of_conduct` | text:String(32767) |

#### configuration, serverbound (10 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `client_information` | locale:String(16); view_distance:Byte; chat_mode:VarInt (0 enabled, 1 commands only, 2 hidden); chat_colors:Boolean; skin_parts:UByte (masque 0x01..0x40); main_hand:VarInt (0 left, 1 right); text_filtering:Boolean; allow_server_listings:Boolean; particle_status:VarInt (0 all, 1 decreased, 2 minimal) |
| 1 | 0x01 | `cookie_response` | key:Identifier; payload:PrefixedOptional<PrefixedArray<Byte>(5120)> |
| 2 | 0x02 | `custom_payload` | channel:Identifier; data:ByteArray |
| 3 | 0x03 | `finish_configuration` | (aucun) |
| 4 | 0x04 | `keep_alive` | id:Long |
| 5 | 0x05 | `pong` | id:Int |
| 6 | 0x06 | `resource_pack` | uuid:UUID; result:VarInt (0..7) |
| 7 | 0x07 | `select_known_packs` | packs:PrefixedArray(64)<{namespace:String; id:String; version:String}> |
| 8 | 0x08 | `custom_click_action` | id:Identifier; payload_size:VarInt (max 65536); payload:NBT |
| 9 | 0x09 | `accept_code_of_conduct` | (aucun) |

#### play, clientbound (144 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `bundle_delimiter` | (aucun) |
| 1 | 0x01 | `add_entity` | entity_id:VarInt; uuid:UUID; type:VarInt; x:Double; y:Double; z:Double; velocity:LpVec3; pitch:Angle; yaw:Angle; head_yaw:Angle; data:VarInt |
| 2 | 0x02 | `animate` | entity_id:VarInt; animation:UByte (0 wake up, 1 critical, 2 magic critical) |
| 3 | 0x03 | `award_stats` | stats:PrefixedArray<{category:VarInt; stat:VarInt; value:VarInt}> |
| 4 | 0x04 | `block_changed_ack` | sequence:VarInt |
| 5 | 0x05 | `block_destruction` | entity_id:VarInt; pos:Position; stage:UByte (0..9, autre = retire) |
| 6 | 0x06 | `block_entity_data` | pos:Position; type:VarInt; nbt:NBT |
| 7 | 0x07 | `block_event` | pos:Position; action:UByte; param:UByte; block:VarInt |
| 8 | 0x08 | `block_update` | pos:Position; state:VarInt |
| 9 | 0x09 | `boss_event` | uuid:UUID; action:VarInt (0 add: title:TextComponent, health:Float, color:VarInt, division:VarInt, flags:UByte; 1 remove; 2 health:Float; 3 title:TextComponent; 4 color:VarInt, division:VarInt; 5 flags:UByte) |
| 10 | 0x0A | `change_difficulty` | difficulty:UByte (0..3); locked:Boolean |
| 11 | 0x0B | `chunk_batch_finished` | batch_size:VarInt |
| 12 | 0x0C | `chunk_batch_start` | (aucun) |
| 13 | 0x0D | `chunks_biomes` | chunks:PrefixedArray<{chunk_z:Int; chunk_x:Int; data:PrefixedArray<Byte>}> |
| 14 | 0x0E | `clear_titles` | reset:Boolean |
| 15 | 0x0F | `command_suggestions` | id:VarInt; start:VarInt; length:VarInt; matches:PrefixedArray<{text:String; tooltip:PrefixedOptional<TextComponent>}> |
| 16 | 0x10 | `commands` | nodes:PrefixedArray<Node>; root_index:VarInt |
| 17 | 0x11 | `container_close` | window_id:VarInt |
| 18 | 0x12 | `container_set_content` | window_id:VarInt; state_id:VarInt; slots:PrefixedArray<Slot>; carried:Slot |
| 19 | 0x13 | `container_set_data` | window_id:VarInt; property:Short; value:Short |
| 20 | 0x14 | `container_set_slot` | window_id:VarInt; state_id:VarInt; slot:Short; item:Slot |
| 21 | 0x15 | `cookie_request` | key:Identifier |
| 22 | 0x16 | `cooldown` | group:Identifier; ticks:VarInt |
| 23 | 0x17 | `custom_chat_completions` | action:VarInt (0 add, 1 remove, 2 set); entries:PrefixedArray<String> |
| 24 | 0x18 | `custom_payload` | channel:Identifier; data:ByteArray |
| 25 | 0x19 | `damage_event` | entity_id:VarInt; source_type:VarInt; cause_id:VarInt (id+1, 0 aucun); direct_id:VarInt (id+1, 0 aucun); source_pos:PrefixedOptional<{x:Double; y:Double; z:Double}> |
| 26 | 0x1A | `debug/block_value` | pos:Position; update:DebugSubscriptionUpdate |
| 27 | 0x1B | `debug/chunk_value` | chunk_z:Int; chunk_x:Int; update:DebugSubscriptionUpdate |
| 28 | 0x1C | `debug/entity_value` | entity_id:VarInt; update:DebugSubscriptionUpdate |
| 29 | 0x1D | `debug/event` | event:DebugSubscriptionEvent |
| 30 | 0x1E | `debug_sample` | sample:PrefixedArray<Long>; type:VarInt (0 tick time) |
| 31 | 0x1F | `delete_chat` | message_id:VarInt (id+1); signature:Optional<ByteArray(256)> (si message_id = 0) |
| 32 | 0x20 | `disconnect` | reason:TextComponent |
| 33 | 0x21 | `disguised_chat` | message:TextComponent; chat_type:IdOr<ChatType>; sender_name:TextComponent; target_name:PrefixedOptional<TextComponent> |
| 34 | 0x22 | `entity_event` | entity_id:Int; status:Byte |
| 35 | 0x23 | `entity_position_sync` | entity_id:VarInt; path_type:VarInt (0 linear: x:Double, y:Double, z:Double; 1 stepped: steps:PrefixedArray<PositionStep>); yaw:Float; pitch:Float; on_ground:Boolean |
| 36 | 0x24 | `explode` | x:Double; y:Double; z:Double; radius:Float; block_count:Int; player_delta:PrefixedOptional<{x:Double; y:Double; z:Double}>; particle_id:VarInt; particle_data:Varies; sound:IdOr<SoundEvent>; block_particle_alternatives:{particle_ids:PrefixedArray<VarInt>; data:Varies; scaling:Float; speed:Float; weight:VarInt}; play_sound:Boolean |
| 37 | 0x25 | `add_transient_block` | pos:Position; state:VarInt |
| 38 | 0x26 | `forget_level_chunk` | chunk_z:Int; chunk_x:Int |
| 39 | 0x27 | `game_event` | event:UByte; value:Float |
| 40 | 0x28 | `game_rule_values` | rules:PrefixedArray<{name:Identifier; value:String(32767)}> |
| 41 | 0x29 | `game_test_highlight_pos` | absolute:Position; relative:Position |
| 42 | 0x2A | `mount_screen_open` | window_id:VarInt; columns:VarInt; entity_id:Int |
| 43 | 0x2B | `hurt_animation` | entity_id:VarInt; yaw:Float |
| 44 | 0x2C | `initialize_border` | center_x:Double; center_z:Double; old_diameter:Double; new_diameter:Double; speed:VarLong (ms); portal_boundary:VarInt; warning_blocks:VarInt; warning_time:VarInt |
| 45 | 0x2D | `keep_alive` | id:Long |
| 46 | 0x2E | `level_chunk_with_light` | chunk_x:Int; chunk_z:Int; heightmaps:PrefixedArray<{type:VarInt; data:PrefixedArray<Long>}>; data:PrefixedArray<Byte>; block_entities:PrefixedArray<{packed_xz:UByte; y:Short; type:VarInt; nbt:NBT}>; light:LightData |
| 47 | 0x2F | `level_event` | event:Int; pos:Position; data:Int; disable_relative_volume:Boolean |
| 48 | 0x30 | `level_particles` | particle_id:VarInt; data:Varies; override_limiter:Boolean; always_visible:Boolean; x:Double; y:Double; z:Double; dx:Float; dy:Float; dz:Float; max_speed:Float; count:VarInt; randomization:VarInt (0..2) |
| 49 | 0x31 | `light_update` | chunk_x:VarInt; chunk_z:VarInt; light:LightData |
| 50 | 0x32 | `login` | entity_id:Int; hardcore:Boolean; dimensions:PrefixedArray<Identifier>; max_players:VarInt; view_distance:VarInt; simulation_distance:VarInt; reduced_debug_info:Boolean; respawn_screen:Boolean; limited_crafting:Boolean; dimension_type:VarInt; dimension:Identifier; hashed_seed:Long; game_mode:VarInt; previous_game_mode:VarInt (0 indefini, sinon id+1); is_debug:Boolean; is_flat:Boolean; death_location:PrefixedOptional<{dimension:Identifier; pos:Position}>; portal_cooldown:VarInt; sea_level:VarInt; online_mode:Boolean; enforces_secure_chat:Boolean |
| 51 | 0x33 | `low_disk_space_warning` | (aucun) |
| 52 | 0x34 | `map_item_data` | map_id:VarInt; scale:Byte; locked:Boolean; icons:PrefixedOptional<PrefixedArray<{type:VarInt; x:Byte; z:Byte; direction:Byte; name:PrefixedOptional<TextComponent>}>>; patch:{columns:UByte; [columns>0] rows:UByte, x:UByte, z:UByte, data:PrefixedArray<UByte>} |
| 53 | 0x35 | `merchant_offers` | window_id:VarInt; trades:PrefixedArray<Trade>; villager_level:VarInt; xp:VarInt; regular:Boolean; can_restock:Boolean |
| 54 | 0x36 | `move_entity_pos` | entity_id:VarInt; properties:VarInt (bit0 on_ground, bits1+ = nombre de pas); [pas=0] dx:Short, dy:Short, dz:Short; [pas>0, par pas] tick_offset:VarInt, dx:Short, dy:Short, dz:Short |
| 55 | 0x37 | `move_entity_pos_rot` | entity_id:VarInt; properties:VarInt (idem); deltas (idem); yaw:Angle; pitch:Angle |
| 56 | 0x38 | `move_minecart_along_track` | entity_id:VarInt; steps:PrefixedArray<{pos:3xDouble; velocity:3xDouble; yaw:Angle; pitch:Angle; weight:Float}> (forme exacte S) |
| 57 | 0x39 | `move_entity_rot` | entity_id:VarInt; yaw:Angle; pitch:Angle; on_ground:Boolean |
| 58 | 0x3A | `move_vehicle` | x:Double; y:Double; z:Double; yaw:Float; pitch:Float |
| 59 | 0x3B | `open_book` | hand:VarInt (0 main, 1 off) |
| 60 | 0x3C | `open_screen` | window_id:VarInt; type:VarInt (registre menu); title:TextComponent |
| 61 | 0x3D | `open_sign_editor` | pos:Position; text_slot:VarInt (0 back, 1 front) |
| 62 | 0x3E | `ping` | id:Int |
| 63 | 0x3F | `pong_response` | payload:Long (non releve sur la page publique ; suppose identique a status) |
| 64 | 0x40 | `place_ghost_recipe` | window_id:VarInt; display:RecipeDisplay |
| 65 | 0x41 | `player_abilities` | flags:Byte (0x01 invulnerable, 0x02 flying, 0x04 allow flying, 0x08 instabuild); fly_speed:Float; fov_modifier:Float |
| 66 | 0x42 | `player_chat` | global_index:VarInt; sender:UUID; index:VarInt; signature:PrefixedOptional<ByteArray(256)>; message:String(256); timestamp:Long; salt:Long; previous:PrefixedArray(20)<{id_plus_1:VarInt; [id_plus_1=0] signature:ByteArray(256)}>; unsigned_content:PrefixedOptional<TextComponent>; filter_type:VarInt (0 pass, 1 fully, 2 partially; [2] bits:BitSet); chat_type:IdOr<ChatType>; sender_name:TextComponent; target_name:PrefixedOptional<TextComponent> |
| 67 | 0x43 | `player_combat_end` | duration:VarInt |
| 68 | 0x44 | `player_combat_enter` | (aucun) |
| 69 | 0x45 | `player_combat_kill` | player_id:VarInt; message:TextComponent |
| 70 | 0x46 | `player_info_remove` | uuids:PrefixedArray<UUID> |
| 71 | 0x47 | `player_info_update` | actions:UByte (0x01 add player, 0x02 init chat, 0x04 game mode, 0x08 listed, 0x10 latency, 0x20 display name, 0x40 list priority, 0x80 hat); entries:PrefixedArray<{uuid:UUID; une charge par bit actif}> (add: name:String(16), properties:PrefixedArray(16); chat: session:PrefixedOptional<UUID>, expiry:Long, key:PrefixedArray(512), sig:PrefixedArray(4096); game mode:VarInt; listed:Boolean; latency:VarInt; display name:PrefixedOptional<TextComponent>; priority:VarInt; hat:Boolean) |
| 72 | 0x48 | `player_look_at` | from:VarInt (0 feet, 1 eyes); x:Double; y:Double; z:Double; is_entity:Boolean; [is_entity] entity_id:VarInt, entity_from:VarInt |
| 73 | 0x49 | `player_position` | teleport_id:VarInt; x:Double; y:Double; z:Double; vx:Double; vy:Double; vz:Double; yaw:Float; pitch:Float; flags:Int (TeleportFlags) |
| 74 | 0x4A | `player_rotation` | yaw:Float; yaw_relative:Boolean; pitch:Float; pitch_relative:Boolean |
| 75 | 0x4B | `recipe_book_add` | recipes:PrefixedArray<{id:VarInt; display:RecipeDisplay; group:VarInt; category:VarInt; ingredients:PrefixedOptional<PrefixedArray<IdSet>>; flags:Byte}> ; replace:Boolean (position exacte du champ flags S) |
| 76 | 0x4C | `recipe_book_remove` | recipes:PrefixedArray<VarInt> |
| 77 | 0x4D | `recipe_book_settings` | 8 x Boolean (crafting, smelting, blast furnace, smoker : open puis filter) |
| 78 | 0x4E | `remove_entities` | ids:PrefixedArray<VarInt> |
| 79 | 0x4F | `remove_mob_effect` | entity_id:VarInt; effect:VarInt |
| 80 | 0x50 | `reset_score` | entity_name:String; objective:PrefixedOptional<String> |
| 81 | 0x51 | `resource_pack_pop` | uuid:PrefixedOptional<UUID> |
| 82 | 0x52 | `resource_pack_push` | uuid:UUID; url:String(32767); hash:String(40); forced:Boolean; prompt:PrefixedOptional<TextComponent> |
| 83 | 0x53 | `post_effects` | effects:PrefixedArray<Identifier> |
| 84 | 0x54 | `respawn` | dimension_type:VarInt; dimension:Identifier; hashed_seed:Long; game_mode:VarInt; previous_game_mode:VarInt; is_debug:Boolean; is_flat:Boolean; death_location:PrefixedOptional<{dimension:Identifier; pos:Position}>; portal_cooldown:VarInt; sea_level:VarInt; data_kept:Byte (0x01 attributs, 0x02 metadonnees) |
| 85 | 0x55 | `rotate_head` | entity_id:VarInt; head_yaw:Angle |
| 86 | 0x56 | `section_blocks_update` | section:Long (x 22 bits, y 20 bits, z 22 bits); blocks:PrefixedArray<VarLong> (state<<12 / x<<8 / z<<4 / y) |
| 87 | 0x57 | `select_advancements_tab` | tab:PrefixedOptional<Identifier> |
| 88 | 0x58 | `server_data` | motd:TextComponent; icon:PrefixedOptional<PrefixedArray<Byte>> |
| 89 | 0x59 | `set_action_bar_text` | text:TextComponent |
| 90 | 0x5A | `set_border_center` | x:Double; z:Double |
| 91 | 0x5B | `set_border_lerp_size` | old_diameter:Double; new_diameter:Double; speed:VarLong |
| 92 | 0x5C | `set_border_size` | diameter:Double |
| 93 | 0x5D | `set_border_warning_delay` | seconds:VarInt |
| 94 | 0x5E | `set_border_warning_distance` | blocks:VarInt |
| 95 | 0x5F | `set_camera` | entity_id:VarInt |
| 96 | 0x60 | `set_chunk_cache_center` | chunk_x:VarInt; chunk_z:VarInt |
| 97 | 0x61 | `set_chunk_cache_radius` | view_distance:VarInt |
| 98 | 0x62 | `set_cursor_item` | item:Slot |
| 99 | 0x63 | `set_default_spawn_position` | dimension:Identifier; pos:Position; yaw:Float; pitch:Float |
| 100 | 0x64 | `set_display_objective` | slot:VarInt; name:String |
| 101 | 0x65 | `set_entity_data` | entity_id:VarInt; metadata:EntityMetadata |
| 102 | 0x66 | `set_entity_link` | attached_id:Int; holder_id:Int (-1 detache) |
| 103 | 0x67 | `set_entity_motion` | entity_id:VarInt; velocity:LpVec3 |
| 104 | 0x68 | `set_equipment` | entity_id:VarInt; entries:Array<{slot:Byte (bit 0x80 = suite); item:Slot}> |
| 105 | 0x69 | `set_experience` | progress:Float; level:VarInt; total:VarInt |
| 106 | 0x6A | `set_health` | health:Float; food:VarInt; saturation:Float |
| 107 | 0x6B | `set_held_slot` | slot:VarInt |
| 108 | 0x6C | `set_objective` | name:String; mode:Byte (0 create, 1 remove, 2 update); [mode 0 ou 2] value:TextComponent, type:VarInt, number_format:PrefixedOptional<NumberFormat> |
| 109 | 0x6D | `set_passengers` | vehicle_id:VarInt; passengers:PrefixedArray<VarInt> |
| 110 | 0x6E | `set_player_inventory` | slot:VarInt; item:Slot |
| 111 | 0x6F | `set_player_team` | name:String; method:Byte (0 create, 1 remove, 2 update, 3 add entities, 4 remove entities); charge selon la methode (voir R-equipes) |
| 112 | 0x70 | `set_score` | entity_name:String; objective:String; value:VarInt; display_name:PrefixedOptional<TextComponent>; number_format:PrefixedOptional<NumberFormat> |
| 113 | 0x71 | `set_simulation_distance` | distance:VarInt |
| 114 | 0x72 | `set_subtitle_text` | text:TextComponent |
| 115 | 0x73 | `set_time` | world_age:Long; clocks:PrefixedArray<{clock:VarInt; time:VarLong; fractional:Float; rate:Float}> |
| 116 | 0x74 | `set_title_text` | text:TextComponent |
| 117 | 0x75 | `set_titles_animation` | fade_in:Int; stay:Int; fade_out:Int |
| 118 | 0x76 | `sound_entity` | sound:IdOr<SoundEvent>; category:VarInt; entity_id:VarInt; volume:Float; pitch:Float; seed:Long |
| 119 | 0x77 | `sound` | sound:IdOr<SoundEvent>; category:VarInt; x:Int; y:Int; z:Int (x8); volume:Float; pitch:Float; seed:Long |
| 120 | 0x78 | `start_configuration` | (aucun) |
| 121 | 0x79 | `stop_sound` | flags:Byte (0x01 source, 0x02 sound); [0x01] source:VarInt; [0x02] sound:Identifier |
| 122 | 0x7A | `store_cookie` | key:Identifier; payload:PrefixedArray<Byte>(5120) |
| 123 | 0x7B | `swing_animation` | entity_id:VarInt; hand:VarInt; type:VarInt (0 none, 1 whack, 2 stab); duration:VarInt |
| 124 | 0x7C | `system_chat` | content:TextComponent; overlay:Boolean |
| 125 | 0x7D | `tab_list` | header:TextComponent; footer:TextComponent |
| 126 | 0x7E | `tag_query` | transaction_id:VarInt; nbt:NBT |
| 127 | 0x7F | `take_item_entity` | collected_id:VarInt; collector_id:VarInt; count:VarInt |
| 128 | 0x80 | `teleport_entity` | entity_id:VarInt; x:Double; y:Double; z:Double; vx:Double; vy:Double; vz:Double; yaw:Float; pitch:Float; flags:Int (TeleportFlags); on_ground:Boolean |
| 129 | 0x81 | `test_instance_block_status` | status:TextComponent; has_size:Boolean; [has_size] x:Double, y:Double, z:Double |
| 130 | 0x82 | `ticking_state` | tick_rate:Float; is_frozen:Boolean |
| 131 | 0x83 | `ticking_step` | steps:VarInt |
| 132 | 0x84 | `transfer` | host:String(32767); port:VarInt |
| 133 | 0x85 | `update_advancements` | reset:Boolean; added:PrefixedArray<{id:Identifier; advancement:Advancement}>; removed:PrefixedArray<Identifier>; progress:PrefixedArray<{id:Identifier; progress:PrefixedArray<{criterion:Identifier; achieved:PrefixedOptional<Long>}>}>; show:Boolean |
| 134 | 0x86 | `update_attributes` | entity_id:VarInt; attributes:PrefixedArray<{id:VarInt; value:Double; modifiers:PrefixedArray<{id:Identifier; amount:Double; operation:Byte}>}> |
| 135 | 0x87 | `update_mob_effect` | entity_id:VarInt; effect:VarInt; amplifier:VarInt; duration:VarInt (-1 infini); flags:Byte (0x01 ambient, 0x02 particles, 0x04 icon, 0x08 blend) |
| 136 | 0x88 | `update_recipes` | property_sets:PrefixedArray<{id:Identifier; items:PrefixedArray<VarInt>}>; stonecutter:PrefixedArray<{ingredients:IdSet; display:SlotDisplay}> |
| 137 | 0x89 | `update_tags` | registries:PrefixedArray<{registry:Identifier; tags:PrefixedArray<{name:Identifier; entries:PrefixedArray<VarInt>}>}> |
| 138 | 0x8A | `projectile_power` | entity_id:VarInt; power:Double |
| 139 | 0x8B | `custom_report_details` | details:PrefixedArray(32)<{title:String(128); description:String(4096)}> |
| 140 | 0x8C | `server_links` | links:PrefixedArray<{label:Either<VarInt, TextComponent>; url:String(32767)}> |
| 141 | 0x8D | `waypoint` | operation:VarInt (0 track, 1 untrack, 2 update); id:Either<UUID, String>; icon_style:Identifier; color:PrefixedOptional<{r:UByte; g:UByte; b:UByte}>; type:VarInt (0 empty; 1 vec3i x,y,z:VarInt; 2 chunk x,z:VarInt; 3 azimuth angle:Float) |
| 142 | 0x8E | `clear_dialog` | (aucun) |
| 143 | 0x8F | `show_dialog` | dialog:IdOr<NBT> |

#### play, serverbound (69 paquets)

| ID | Hex | Nom | Champs (S) |
|---|---|---|---|
| 0 | 0x00 | `accept_teleportation` | teleport_id:VarInt; x:Double; y:Double; z:Double; yaw:Float; pitch:Float |
| 1 | 0x01 | `attack` | entity_id:VarInt |
| 2 | 0x02 | `block_entity_tag_query` | transaction_id:VarInt; pos:Position |
| 3 | 0x03 | `bundle_item_selected` | bundle_slot:VarInt; item_slot:VarInt |
| 4 | 0x04 | `change_difficulty` | difficulty:UByte |
| 5 | 0x05 | `change_game_mode` | mode:VarInt |
| 6 | 0x06 | `chat_ack` | message_count:VarInt |
| 7 | 0x07 | `chat_command` | command:String(32767) (sans '/') |
| 8 | 0x08 | `chat_command_signed` | command:String(32767); timestamp:Long; salt:Long; signatures:PrefixedArray(8)<{name:String(16); signature:ByteArray(256)}>; message_count:VarInt; acknowledged:FixedBitSet(20); checksum:Byte |
| 9 | 0x09 | `chat` | message:String(256); timestamp:Long; salt:Long; signature:PrefixedOptional<ByteArray(256)>; message_count:VarInt; acknowledged:FixedBitSet(20); checksum:Byte |
| 10 | 0x0A | `chat_session_update` | session_id:UUID; expires_at:Long; public_key:PrefixedArray(512)<Byte>; key_signature:PrefixedArray(4096)<Byte> |
| 11 | 0x0B | `chunk_batch_received` | chunks_per_tick:Float |
| 12 | 0x0C | `client_command` | action:VarInt (0 respawn, 1 stats, 2 game rule values) |
| 13 | 0x0D | `client_tick_end` | (aucun) |
| 14 | 0x0E | `client_information` | locale:String(16); view_distance:Byte; chat_mode:VarInt; chat_colors:Boolean; skin_parts:UByte; main_hand:VarInt; text_filtering:Boolean; allow_server_listings:Boolean; particle_status:VarInt |
| 15 | 0x0F | `command_suggestion` | transaction_id:VarInt; text:String(32500) |
| 16 | 0x10 | `configuration_acknowledged` | (aucun) |
| 17 | 0x11 | `container_button_click` | window_id:VarInt; button:VarInt |
| 18 | 0x12 | `container_click` | window_id:VarInt; state_id:VarInt; slot:Short; button:Byte; mode:VarInt (0..6); changed:PrefixedArray(128)<{slot:Short; item:HashedSlot}>; carried:HashedSlot |
| 19 | 0x13 | `container_close` | window_id:VarInt |
| 20 | 0x14 | `container_slot_state_changed` | slot:VarInt; window_id:VarInt; enabled:Boolean |
| 21 | 0x15 | `cookie_response` | key:Identifier; payload:PrefixedOptional<PrefixedArray<Byte>(5120)> |
| 22 | 0x16 | `custom_payload` | channel:Identifier; data:ByteArray |
| 23 | 0x17 | `debug_subscription_request` | subscriptions:PrefixedArray<VarInt> |
| 24 | 0x18 | `edit_book` | slot:VarInt; pages:PrefixedArray(100)<String(1024)>; title:PrefixedOptional<String(32)> |
| 25 | 0x19 | `entity_tag_query` | transaction_id:VarInt; entity_id:VarInt |
| 26 | 0x1A | `interact` | entity_id:VarInt; hand:VarInt; offset:LpVec3; sneaking:Boolean |
| 27 | 0x1B | `jigsaw_generate` | pos:Position; levels:VarInt; keep_jigsaws:Boolean |
| 28 | 0x1C | `keep_alive` | id:Long |
| 29 | 0x1D | `lock_difficulty` | locked:Boolean |
| 30 | 0x1E | `move_player_pos` | x:Double; y:Double; z:Double; flags:Byte (0x01 on ground, 0x02 contre un mur) |
| 31 | 0x1F | `move_player_pos_rot` | x:Double; y:Double; z:Double; yaw:Float; pitch:Float; flags:Byte |
| 32 | 0x20 | `move_player_rot` | yaw:Float; pitch:Float; flags:Byte |
| 33 | 0x21 | `move_player_status_only` | flags:Byte |
| 34 | 0x22 | `move_vehicle` | x:Double; y:Double; z:Double; yaw:Float; pitch:Float; on_ground:Boolean |
| 35 | 0x23 | `paddle_boat` | left:Boolean; right:Boolean |
| 36 | 0x24 | `pick_item_from_block` | pos:Position; include_data:Boolean |
| 37 | 0x25 | `pick_item_from_entity` | entity_id:VarInt; include_data:Boolean |
| 38 | 0x26 | `ping_request` | payload:Long (non releve sur la page publique ; suppose identique a status) |
| 39 | 0x27 | `place_recipe` | window_id:VarInt; recipe_id:VarInt; make_all:Boolean |
| 40 | 0x28 | `player_abilities` | flags:Byte (0x02 flying) |
| 41 | 0x29 | `player_action` | status:VarInt (0 start dig, 1 cancel, 2 finish, 3 drop stack, 4 drop item, 5 release use, 6 swap hands, 7 stab); pos:Position; face:Byte (0 -Y, 1 +Y, 2 -Z, 3 +Z, 4 -X, 5 +X); sequence:VarInt |
| 42 | 0x2A | `player_command` | entity_id:VarInt; action:VarInt (0 leave bed, 1 start sprint, 2 stop sprint, 3 start horse jump, 4 stop horse jump, 5 open vehicle inventory, 6 start elytra); jump_boost:VarInt |
| 43 | 0x2B | `player_input` | flags:UByte (0x01 forward, 0x02 backward, 0x04 left, 0x08 right, 0x10 jump, 0x20 sneak, 0x40 sprint) |
| 44 | 0x2C | `player_loaded` | (aucun) |
| 45 | 0x2D | `pong` | id:Int |
| 46 | 0x2E | `punch` | (aucun) |
| 47 | 0x2F | `recipe_book_change_settings` | book:VarInt (0 crafting, 1 furnace, 2 blast furnace, 3 smoker); open:Boolean; filter:Boolean |
| 48 | 0x30 | `recipe_book_seen_recipe` | recipe_id:VarInt |
| 49 | 0x31 | `rename_item` | name:String(32767) |
| 50 | 0x32 | `resource_pack` | uuid:UUID; result:VarInt (0..7) |
| 51 | 0x33 | `seen_advancements` | action:VarInt (0 opened tab, 1 closed screen); [action 0] tab:Identifier |
| 52 | 0x34 | `select_trade` | slot:VarInt |
| 53 | 0x35 | `set_beacon` | primary:PrefixedOptional<VarInt>; secondary:PrefixedOptional<VarInt> |
| 54 | 0x36 | `set_carried_item` | slot:Short |
| 55 | 0x37 | `set_command_block` | pos:Position; command:String(32767); mode:VarInt (0 chain, 1 repeating, 2 impulse); flags:Byte (0x01 track output, 0x02 conditional, 0x04 automatic) |
| 56 | 0x38 | `set_command_minecart` | entity_id:VarInt; command:String(32767); track_output:Boolean |
| 57 | 0x39 | `set_creative_mode_slot` | slot:Short; item:Slot |
| 58 | 0x3A | `set_game_rule` | rules:PrefixedArray<{name:Identifier; value:String(32767)}> (forme exacte S : la page publique est ambigue) |
| 59 | 0x3B | `set_jigsaw_block` | pos:Position; name:Identifier; target:Identifier; pool:Identifier; final_state:String; joint:String; selection_priority:VarInt; placement_priority:VarInt |
| 60 | 0x3C | `set_structure_block` | pos:Position; action:VarInt; mode:VarInt; name:String; offset:3xByte; size:3xByte; mirror:VarInt; rotation:VarInt; metadata:String(128); integrity:Float; seed:VarLong; flags:Byte |
| 61 | 0x3D | `set_test_block` | pos:Position; mode:VarInt (0 start, 1 log, 2 fail, 3 accept); message:String |
| 62 | 0x3E | `sign_update` | pos:Position; lines:4xString(384); text_slot:VarInt (0 back, 1 front) |
| 63 | 0x3F | `spectator_action` | entity_id:VarInt (0 aucun, sinon id+1) |
| 64 | 0x40 | `teleport_to_entity` | uuid:UUID |
| 65 | 0x41 | `test_instance_block_action` | pos:Position; action:VarInt; test:PrefixedOptional<Identifier>; size:3xVarInt; rotation:VarInt; ignore_entities:Boolean; status:VarInt; error:PrefixedOptional<TextComponent> |
| 66 | 0x42 | `use_item_on` | hand:VarInt; pos:Position; face:VarInt; cursor_x:Float; cursor_y:Float; cursor_z:Float; inside_block:Boolean; world_border_hit:Boolean; sequence:VarInt |
| 67 | 0x43 | `use_item` | hand:VarInt; sequence:VarInt; yaw:Float; pitch:Float |
| 68 | 0x44 | `custom_click_action` | id:Identifier; payload_size:VarInt; payload:NBT |

## Entrées / sorties

- **Entrée du décodeur** : octets d'une connexion TCP ; sortie : suite de {état, sens, identifiant, charge}, ou erreur de protocole portant la cause (R8, R9, R27, R30). Le décodeur est une fonction pure de (état, octets) : il ne dépend ni de l'horloge ni du réseau, ce qui le rend testable avec les vecteurs du fichier compagnon.
- **Sortie de l'encodeur** : octets d'une trame, avec ou sans compression (seuil), avec ou sans chiffrement.
- **Plages** : seuil de compression : entier 32 bits signé ; négatif = désactivé, 0 = tout compresser (F13) ; valeur par défaut du serveur 256 (`network-compression-threshold`, `server.properties`). Longueur de trame : 0 à 2 097 151. Identifiant de paquet : VarInt (valeurs utilisées 0 à 143). Coordonnées de chunk : Int 32 bits, ordre Z puis X pour `forget_level_chunk`, `chunks_biomes`, `debug/chunk_value`, X puis Z pour `level_chunk_with_light` (R68).
- **Coordonnées négatives** : Position (R12) gère les trois champs signés par complément à deux sur leur largeur ; chunk Int signé ; section : x, y, z signés sur 22, 20, 22 bits (S02 : section (-1, -4, -1)) ; variation de position en Short signé (1/4096 de bloc) ; dans un lot de chunks, la section 0 est la section de y minimal du type de dimension (y = -64 pour l'Overworld : y = -64 -> index 0, y = 319 -> index 23, vecteur D01).

## Cas limites

- VarInt : 0, 127, 128, 255, 2 097 151, 2 097 152 (4 octets : longueur de trame invalide), 2 147 483 647, -1, -2 147 483 648 ; VarLong : -1 (10 octets), minimum et maximum ; encodage trop long à zéros de tête ; 6 octets ; 5e octet avec bits au-delà du 32e (N01 à N22).
- Trames : seuil - 1 octets (en clair, longueur des données 0), seuil octets (compressé), seuil 0, seuil négatif, flux zlib dont la taille ne correspond pas à l'annonce, longueur des données sous le seuil, corps en clair au-dessus du seuil (F09 à F14, I03 à I05).
- Handshake : intention 4, protocole 776 et -1, identifiant de paquet inconnu, adresse de 255 caractères, port 0 et 65535 (I06 à I09).
- Login : nom de 17 caractères (I10), nom vide, deux connexions du même nom, UUID du client différent de l'UUID hors ligne (R57).
- Chiffrement : chiffreur redémarré à tort (CH02), clé RSA de taille inattendue, jeton de longueur autre que 4.
- Chunks : section d'une seule valeur (bits 0), bits 4 et 5 (frontière de 16 et 12 entrées par Long), 16 bits directs pour les blocs (K06), 257 états distincts dans une section (premier cas direct), 256 états (dernier cas indirect), biomes à 1, 2, 3 et 7 bits, dernier Long partiellement rempli (K02 : 4096 = 341 x 12 + 4) ; hauteur 384 et 256 (K08 à K10) ; coordonnées de chunk négatives ; `chunks_biomes` où Z est lu en premier.
- Position : x, z aux deux bornes, y aux deux bornes, -1 partout (P01 à P06).
- LpVec3 : zéro, échelle 1, 3, 4 (premier cas avec suite), 10, 123 457, composantes de signes opposés (L01 à L05).
- Mouvement : téléportation non acquittée, paquets de mouvement avec NaN ou infini (provisoire : erreur de protocole, clé D `multiplayer.disconnect.invalid_player_movement`), lacet non borné (la documentation dit "not clamped").
- Déconnexion pendant la configuration ou pendant un lot de chunks.

## Vecteurs de test

Détail, octets et commandes : `net-protocol-777-vectors.md`. En résumé :

- N01 à N22 : VarInt et VarLong (R6 à R8).
- F01 à F16 : trames de handshake, status, login, compression, chiffrement (R27 à R32, R34, R37, R39).
- I01 à I12 : entrées invalides et résultat provisoire attendu (R27, R30, R35, R36, R59).
- L01 à L05 : LpVec3 (R18).
- P01 à P06, S01 à S03, B01 : Position, section, bloc (R12, R74).
- K01 à K12 : conteneurs à palette, cartes de hauteur, lumière (R24, R69, R70).
- H01 à H03 : empreinte de session (R52) ; U01 à U05 : UUID hors ligne (R58) ; CH01 à CH06 : chiffrement (R31, R50, R51).
- CT01 : arbre de commandes miniature (R25, R26).
- D01 : dimensions (R67, R68).
- Z : clés de déconnexion (annexe, D).

## Plan de tests

| Niveau | Contenu | Règles |
|---|---|---|
| L1 | Vecteurs N, F, I, L, P, S, B, K, H, U, E, C, D : encodeur et décodeur ; tests de propriétés : aller-retour pour tout VarInt/VarLong (séquences pseudo-aléatoires de graine fixée), pour tout paquet de la table (champs générés), idempotence du découpage en trames (une trame envoyée en morceaux d'un octet, ou plusieurs trames dans un même segment, donnent les mêmes paquets) | R5 à R33, R85, R90, R91 |
| L1 | Couverture des tables : pour chacun des 260 (état, sens, identifiant) de `packets.json`, un test charge le nom attendu et vérifie que le décodeur connaît cet identifiant ; un identifiant hors table est refusé | R2 |
| L1 | Cohérence avec `registries.json` : 62 analyseurs, 122 composants, 59 gamerules, 16 abonnements de débogage ; 35 723 états de blocs contigus | R26, R61, R101 |
| L2 | Transcriptions PN2 à PN5 : rejouer côté serveur de l'oracle les mêmes entrées que notre serveur et comparer, paquet par paquet, identifiants, ordre et octets (hors champs variables : horloges, graine, identifiants d'entité, Long de keep-alive) | R34 à R49, R62 à R68, R71 à R83 |
| L2 | Comparaison de l'arbre de commandes : PX1 | R25, R26 |
| L4 | Scénario M8 : un client vanilla 26.3 réel se connecte en mode hors ligne à notre serveur (liste de serveurs : état affiché ; connexion ; chargement d'un monde généré par M4/M3 ; déplacement ; pose et casse d'un bloc ; chat ; une commande `/time set day` ; déconnexion propre) ; capture d'écran et journal, scénario `tools/diff/scenarios/m8-join.json` | critère de sortie M8 |
| L4 | Même scénario avec `online-mode` vrai, compte du propriétaire, serveur local : valide R50 à R55 (seul test possible de bout en bout, voir PN9) | R50 à R56 |

Le L4 est exécuté par une personne ou un agent qui dispose du client ; ni le `spec-analyst` ni le `test-engineer` ne le lancent.

## Questions ouvertes

| Réf | Question | Comment la lever |
|---|---|---|
| Q1 | Les champs des 260 paquets (R3) correspondent-ils à ceux du jeu ? La page publique est partiellement périmée (écarts du tableau d'ouverture). | Transcriptions PN4 (configuration) et PN5 (jeu), octet par octet, avec un client scripté indépendant de `engine/*` |
| Q2 | Quels registres sont synchronisés (R62), dans quel ordre, avec quels types de tags NBT (R64) ? | PN4 : liste ordonnée des `registry_data`, comptes, octets des 5 premières entrées de chaque registre |
| Q3 | Quel ensemble de paquets d'entrée en jeu un client vanilla exige-t-il (R44) ? | PN5, puis L4 en retirant un à un les paquets optionnels |
| Q4 | Faut-il valider les signatures de chat en ligne (clés de Mojang, `enforce-secure-profile`) ? | Décision du propriétaire ; PN13 pour le comportement hors ligne |
| Q5 | Durées : délai de connexion, intervalle de keep-alive, délai de lecture (R40, R47, R91) | PN8, PA4 |
| Q6 | Rejets de mouvement (R76) : seuils et réponses | PN12 |
| Q7 | Réaction aux entrées invalides (R5, R8, R9, R27, R30, R49) : fermeture silencieuse ou message ? | PN6, PN7 |
| Q8 | Ping hérité `0xFE` (R35) | PN10 |
| Q9 | Somme de contrôle des composants dans `container_click` (R22) | PN12 (clics créatifs et d'inventaire avec composants) ; question de format à régler au jalon M6 |
| Q10 | Formes de `RecipeDisplay`, `SlotDisplay`, particules et explosion (R102) | Jalons M6 et M9 ; PN5 pour la présence dans le flux d'entrée en jeu |
| Q11 | `update_recipes` vide suffit-il pour un client vanilla (R102) ? | PN5, puis L4 |
| Q12 | Index de métadonnées 8 à 16 du joueur et valeurs par défaut (R23) | PN5 |
| Q13 | Taille de la file et du délai (R91) : valeurs du jeu | non observable proprement ; choix d'ingénierie, à documenter dans une ADR si la valeur compte |

Règles à lever : toutes sauf R1, R2, R4, R61 (D) et les parties calculées citées en C. Tant que PN4 et PN5 n'ont pas tourné, la spec ne passe pas à `figé`.

## Proposition pour la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Protocole 777 | M8 | B | spécifié | `docs/specs/net-protocol-777.md`, `net-protocol-777-vectors.md` (R1, R2, R4, R61 en D ; reste S) |
| Commandes | M8 | B | spécifié | `docs/specs/server-commands.md`, `server-commands-vectors.md` |

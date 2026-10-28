# Simulation de blocs : ticks planifiés, ticks aléatoires, mises à jour de blocs

Statut : brouillon
Jalon : M5
Risque : A
Règles : 35 (R1 à R35). D 3, D/S 0, C 0, C sur S 0, S 32, M 0.

Noyau commun de la simulation de blocs : ce qui déclenche l'exécution du comportement d'un bloc (file de ticks planifiés, ticks aléatoires, mises à jour de voisins et de formes, événements). Les comportements eux-mêmes sont dans `sim-light.md`, `sim-fluids.md` et `sim-growth-redstone.md` ; le cadre de la boucle de tick (cadence, `/tick`, ordre des phases, gamerules) est dans `server-game-loop.md`.

## Périmètre

Couvre, pour `engine/world` (simulation de blocs, jalon M5) :

- la file des ticks planifiés de blocs et de fluides : entrée, délai, unicité, échéance, priorité, ordre d'exécution, budget par tick, persistance ;
- les ticks aléatoires : sélection, `random_tick_speed`, éligibilité des chunks ;
- les trois familles de mises à jour de blocs (forme, voisin modifié, comparateur), leur ordre d'envoi et leur propagation ;
- l'effet des modes `strict` des commandes de placement sur ces mises à jour ;
- la file des événements de blocs (phase TK4.n de `server-game-loop.md`) en tant que point d'ordre ;
- le canal d'observation commun aux sondes M5 (instantané de région sous `/tick freeze`), harnais SK0.

Ne couvre pas :

- la cadence, l'état de gel, la commande `/tick` et le séquencement des phases hors simulation de blocs : `server-game-loop.md` (R1 à R22) ;
- le contenu des comportements (fluides, lumière, croissance, redstone) : specs sœurs ;
- les entités et leurs ticks (M7), les ticks de block entities autres que ceux cités (M6/M7), la météo comme machine à états (voir Questions ouvertes) ;
- la profondeur maximale des chaînes de mises à jour (voir Questions ouvertes, aucune règle ne la fixe).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `generated/reports/blocks.json` (1286 blocs, 35 723 états), `generated/reports/registries.json` (registre `minecraft:game_rule`), `generated/reports/commands.json` (arbres de `/tick`, `/setblock`, `/fill`, `/clone`, `/save-all`, `/forceload`, `/execute`). Reproduction : annexe `sim_ref.py` de `sim-ticks-updates-vectors.md` (sous-commandes `rules`, `cmds`, `propscan`), empreintes dans ce fichier.
- **Documentation publique**, consultée le 2026-10-28 (l'outil de lecture restitue un résumé des 100 000 premiers caractères de chaque page ; les sections non lues sont signalées) : Minecraft Wiki "Tick" (https://minecraft.wiki/w/Tick : ticks planifiés, priorités, limite par tick, ordre des phases, ticks aléatoires), "Random tick" (https://minecraft.wiki/w/Random_tick : tirage avec remise, liste des blocs concernés ; page signalée comme ébauche), "Block update" (https://minecraft.wiki/w/Block_update : types, ordres d'envoi, parcours en profondeur ; la page ne dit rien de la profondeur maximale, des drapeaux de placement ni de la suppression de mises à jour), "Commands/setblock" (https://minecraft.wiki/w/Commands/setblock : mode `strict`, ajout en 25w02a), "Redstone Repeater" (https://minecraft.wiki/w/Redstone_Repeater : un seul tick planifié à la fois), "Piston" (https://minecraft.wiki/w/Piston : délai de démarrage selon la phase), "Tutorials/Quasi-connectivity" (https://minecraft.wiki/w/Tutorials/Quasi-connectivity).
- **Rapports locaux** (`docs/reference/`) : non utilisés pour cette spec ; aucune valeur, règle ou ordre n'en provient (ADR 0002, 2b).
- **Sondes de l'oracle** : aucune exécutée (EULA non acceptée). Sondes SK0 à SK16 dans `sim-ticks-updates-vectors.md`.

Convention de preuve : D donnée exacte (reproductible par une sous-commande de l'annexe), M observé sur l'oracle (aucune à ce jour), C calcul, S à vérifier. « C sur S » : calcul dont une prémisse est S. « D (valeur) / S (effet) » : la valeur est lue dans les données, son effet est S. Toute règle S donne son comportement provisoire et la sonde qui le lève ; une règle S dont la source est une page publique cite la page.

## Règles

### Cadre (données)

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Commandes de pilotage des sondes : `/tick` a 9 formes exécutables (`query`, `rate <rate>`, `freeze`, `unfreeze`, `step`, `step <time>`, `step stop`, `sprint <time>`, `sprint stop`) ; `/setblock` a 5 formes (mode omis, `destroy`, `keep`, `replace`, `strict`) ; `/fill` a 12 formes dont `strict` ; `/clone` accepte `strict` ; `/save-all flush` et `/forceload add|query|remove` existent ; `execute if|unless` offre `block`, `blocks`, `loaded`, `predicate`, `data`, `entity` entre autres. Commande : `sim_ref.py REF cmds` (SKV1). | D |
| R2 | La gamerule `minecraft:random_tick_speed` a l'identifiant de protocole 40, un argument entier de minimum 0 et sans maximum ; la valeur par défaut 3 est celle de `server-game-loop.md` R40. Commande : `sim_ref.py REF rules` (SKV2). | D |
| R3 | Le jeu de données compte 1286 blocs et 35 723 états ; les propriétés que la simulation lit (`level`, `power`, `age`, `distance`, `delay`, `lit`, `powered`, `waterlogged`, `stage`, `moisture`...) ont les effectifs et plages de SKV3. Commande : `sim_ref.py REF propscan`. | D |

### Ticks planifiés

| Réf | Règle | Preuve |
|---|---|---|
| R4 | Il y a deux files de ticks planifiés par dimension : celle des blocs (clé : identifiant de bloc) et celle des fluides (clé : identifiant de fluide). Chaque entrée porte une position, une échéance, une priorité entière et un ordre d'insertion (page "Tick" : "deux types, ticks de blocs puis ticks de fluides"). Provisoire : deux files indépendantes, entrées `(type, position, échéance, priorité, ordre)`. Sonde : SK2 ; critère de succès : la file de blocs et la file de fluides d'un chunk instantané contiennent respectivement les entrées d'un répéteur et d'une eau posés, sans mélange. | S |
| R5 | Format sauvegardé : `block_ticks` et `fluid_ticks`, entrées `{i, p, t, x, y, z}` (`world-chunk-format.md` R23, S). Provisoire : `t` est le délai restant en ticks au moment de la sauvegarde. Sonde : SK2 ; critère de succès : après `setblock` d'une eau source puis `/save-all flush`, une entrée de fluide existe à la position avec `t` égal au délai de l'eau relevé par SF2 ; après `tick step 1` et une nouvelle sauvegarde, `t` a diminué de 1 ; une entrée en retard a `t` négatif. | S |
| R6 | Un tick planifié avec un délai d reçoit l'échéance (temps de jeu courant + d). Provisoire : échéance = temps de jeu courant + d, le temps de jeu étant celui de `time query gametime`. Sonde : SK2 ; critère de succès : l'effet apparaît après exactement d pas de `tick step 1` (pas avant), pour d lu en SK2 sur trois composants (eau, répéteur, torche de redstone). | S |
| R7 | Un seul tick peut être planifié à la fois pour une même position et un même type ; toute demande de planification d'un doublon est ignorée (page "Redstone Repeater" pour le répéteur ; généralisation à tous les types : hypothèse H-K1). Provisoire : la file ne contient jamais deux entrées de même (position, type). Sonde : SK3 ; critère de succès : après trois basculements du levier d'un répéteur dans le même tick de jeu gelé, la file sauvegardée contient exactement une entrée pour ce répéteur ; idem pour l'eau (trois `setblock` d'un voisin) et pour une lampe. | S |
| R8 | Une entrée dont le bloc (ou fluide) à la position n'est plus du type enregistré à l'échéance est exécutée sans effet et retirée (hypothèse H-K2). Provisoire : à l'échéance, si le type courant diffère, l'entrée est consommée et ignorée. Sonde : SK4 ; critère de succès : après remplacement d'un répéteur planifié par de la pierre sans pas de tick, l'entrée est encore dans la file sauvegardée ; après les pas jusqu'à l'échéance, elle a disparu et aucun bloc n'a changé. | S |
| R9 | Seuls les chunks qui tiquent exécutent leurs ticks planifiés ; les entrées d'un chunk qui ne tique pas attendent, sans que leur délai restant diminue, et reprennent au rechargement (hypothèse H-K3 : le délai est stocké relatif). Provisoire : le délai restant d'une entrée est gelé tant que son chunk ne tique pas. Sonde : SK5 ; critère de succès : eau posée dans un chunk forcé, `forceload remove`, 50 ticks, `forceload add` : l'entrée est exécutée au plus (délai restant) ticks après le rechargement, jamais avant. | S |
| R10 | Phase : les ticks planifiés de blocs s'exécutent à la phase TK4.f, ceux de fluides à TK4.g, avant les ticks aléatoires (TK4.j) et les événements de blocs (TK4.n) (`server-game-loop.md` R19, S). Provisoire : blocs, puis fluides, puis ticks aléatoires, puis événements. Sonde : SK7 ; critère de succès : montage de concurrence bloc/fluide (SK7) donne le résultat prédit par l'ordre bloc puis fluide ; le montage inverse (bloc planifié un tick plus tard) donne le résultat opposé. | S |
| R11 | Ordre dans une file : par échéance croissante, puis par priorité croissante (la plus petite d'abord), puis par ordre d'insertion ; les ticks de fluides n'ont pas de priorité et suivent l'ordre d'insertion (page "Tick"). Provisoire : clé de tri (échéance, priorité, ordre d'insertion) pour les blocs, (échéance, ordre d'insertion) pour les fluides. Sonde : SK6 ; critère de succès : montage de verrouillage (SK6 a et b) donne l'état prédit (priorité -3 contre -1 à échéance égale ; échéance plus tardive malgré une priorité plus basse) et la file sauvegardée conserve l'ordre d'insertion à priorité égale (SK2 b). Vecteur de file : SKV5. | S |
| R12 | Valeurs de priorité (page "Tick") : répéteur dont la face est tournée vers l'arrière ou le côté d'un autre diode -3 ; répéteur qui perd son alimentation -2 ; autre répéteur -1 ; comparateur tourné vers l'arrière ou le côté d'un diode -1 ; tout autre tick de bloc 0. Provisoire : ces valeurs, stockées dans le champ `p` de la file sauvegardée. Sonde : SK8 ; critère de succès : `p` relevé égal à la valeur de la liste pour les cinq cas du montage. | S |
| R13 | Budget : au plus 65 536 ticks planifiés sont exécutés par tick de jeu (page "Tick", Java) ; les suivants restent en file et sont en retard au tick suivant (échéance dépassée, `t` négatif). Provisoire : le budget s'applique à la somme des deux files d'une dimension. Sonde : SK9 ; critère de succès : avec plus de 65 536 entrées dues au même tick, le nombre d'entrées restant après `tick step 1` est (total - 65 536) pour 70 000, 131 072 et 200 000 entrées, ou la sonde note le budget réel et l'affecte à chaque file. | S |
| R14 | Un tick planifié pendant l'exécution des ticks d'un tick de jeu, avec un délai d'au moins 1, n'est pas exécuté pendant ce même tick de jeu. Délai 0 : hypothèse H-K4, traité comme 1. Provisoire : un tick créé pendant la phase TK4.f n'est jamais exécuté dans la même phase. Sonde : SK6 (variante chaînée) ; critère de succès : une chaîne de n répéteurs au délai 1 (2 ticks) met exactement 2n ticks de jeu à propager, et jamais moins. | S |
| R15 | Sous gel (`/tick freeze`), une commande qui change un bloc s'exécute tout de suite et ses mises à jour de voisins (R26 à R30) se propagent dans l'appel ; les ticks planifiés qu'elle crée ne s'exécutent qu'aux pas (`server-game-loop.md` R15, R16, R22). Provisoire : mises à jour synchrones, ticks aux pas seulement. Sonde : SK1 ; critère de succès : l'état d'un bloc dont la réaction est immédiate (lampe alimentée par un bloc de redstone) est visible sans pas ; l'état d'un bloc à tick (eau étalée) n'apparaît qu'après le nombre de pas de R6. | S |
| R16 | Le comptage de pas est exact : `tick step n` fait avancer le temps de jeu de n (`time query gametime`), puis regèle. Provisoire : n ticks exacts. Sonde : SK1 ; critère de succès : écart de `gametime` avant/après égal à n pour n = 1, 2, 5, 40. | S |

### Ticks aléatoires

| Réf | Règle | Preuve |
|---|---|---|
| R17 | À chaque tick de jeu, pour chaque section de 16 x 16 x 16 d'un chunk qui tique, `random_tick_speed` positions sont tirées uniformément parmi les 4096 positions de la section, avec remise (un même bloc peut être tiré plusieurs fois dans le même tick). Page "Random tick" : "un bloc peut aussi être choisi plusieurs fois" (la page marque ce point à vérifier). Provisoire : n tirages indépendants uniformes par section et par tick. Sonde : SK10 ; critère de succès : section remplie de 4096 minerais de redstone allumés (réaction supposée à l'extinction, H-K5), vitesse 1 : probabilité q d'extinction par tirage égale à la moyenne observée sur 400 essais ; vitesse 4096, un tick : nombre de minerais éteints égal à q fois 2589,35 (écart type 19,95 pour q = 1, SKV6) à 3 écarts types de la moyenne de 400 essais, alors que des tirages sans remise donneraient 4096 q. | S |
| R18 | `random_tick_speed` 0 supprime les ticks aléatoires ; une modification prend effet au tick suivant. Provisoire : vitesse 0 : aucun tirage ; changement lu à chaque tick. Sonde : SK10 (variantes vitesse 0, puis 4096, puis 0) ; critère de succès : aucun minerai éteint à vitesse 0 sur 50 ticks, moyenne de R17 dès le premier pas après passage à 4096, aucun minerai éteint de plus au premier pas après retour à 0. | S |
| R19 | Les ticks aléatoires s'appliquent aux chunks qui tiquent, qu'un joueur soit proche ou non, y compris les chunks forcés (`server-game-loop.md` R20, R21, S). Provisoire : un chunk forcé sans joueur reçoit ses ticks aléatoires. Sonde : SK10 (serveur sans joueur connecté) ; critère de succès : moyenne de R17 observée sans joueur. | S |
| R20 | Toute position peut être tirée, y compris de l'air ; un tirage n'a d'effet que si le bloc (ou son fluide) réagit aux ticks aléatoires (page "Random tick", liste de blocs qui réagissent reprise dans `sim-growth-redstone.md` et `sim-fluids.md`). Provisoire : le tirage d'une position sans réaction ne change aucun état et ne consomme aucun aléa observable. Sonde : SK10 ; critère de succès : section remplie de pierre, vitesse 4096, 50 ticks : aucune différence de bloc. | S |
| R21 | L'ordre d'examen des chunks dans un tick est aléatoire (`server-game-loop.md` TK4.j, S) et n'est pas observable en dehors des interactions entre deux ticks aléatoires du même tick ; l'implémentation peut choisir un ordre fixe. Provisoire : l'ordre des chunks n'est pas imposé ; les comparaisons de ticks aléatoires sont statistiques (VERIFICATION, Déterminisme). Sonde : SK10 (comparaison de distributions sur 400 essais, deux ordres de chunks) ; critère de succès : moyennes égales à 3 écarts types entre les deux ordres. | S |

### Mises à jour de blocs

| Réf | Règle | Preuve |
|---|---|---|
| R22 | Trois familles de mises à jour (page "Block update") : mise à jour de forme (le voisin recalcule son état, utilisée pour les blocs qui se connectent ou qui cassent sans support), voisin modifié (utilisée surtout par la redstone), mise à jour de comparateur (envoyée aux seuls comparateurs, quand un bloc lisible par comparateur change). Provisoire : trois canaux distincts, sans appel de l'un par l'autre sauf mention. Sonde : SK11, SK12, SK13 ; critère de succès : voir R23 à R25. | S |
| R23 | Ordre des mises à jour de forme depuis un bloc modifié : ouest, est, nord, sud, bas, haut (page "Block update"). Provisoire : cet ordre. Sonde : SK11 ; critère de succès : six dalles immergées autour d'un bloc, l'ordre des six entrées de fluide planifiées lues dans la file sauvegardée (R5) suit l'ordre ci-dessus ; contrôle préalable (SK2) que la file sauvegardée conserve l'ordre d'insertion à échéance égale. | S |
| R24 | Ordre des mises à jour de voisin modifié : ouest, est, bas, haut, nord, sud (page "Block update"). Provisoire : cet ordre. Sonde : SK12 ; critère de succès : six lampes allumées autour d'un bloc de redstone retiré, l'ordre des six entrées de tick lues dans la file sauvegardée suit l'ordre ci-dessus ; contre-épreuve par deux pistons en concurrence (SK12, variante). | S |
| R25 | Ordre des mises à jour de comparateur : nord, est, sud, ouest (page "Block update"). Provisoire : cet ordre. Sonde : SK13 ; critère de succès : quatre comparateurs adossés aux quatre faces d'un coffre dont le contenu change, ordre des entrées planifiées lu dans la file sauvegardée. | S |
| R26 | Propagation en profondeur d'abord : un bloc qui réagit à une mise à jour et change à son tour envoie ses propres mises à jour avant que le bloc modifié à l'origine n'ait fini d'envoyer les siennes (page "Block update", section DFS). Provisoire : récursion synchrone, pas de file intermédiaire. Sonde : SK14 ; critère de succès : montage à trois blocs A, B, C où A met à jour B et C et B met à jour D : l'ordre d'apparition des ticks planifiés (par D puis par C) lu dans la file sauvegardée est (B, D, C) et non (B, C, D). | S |
| R27 | Un placement par commande sans `strict` (`setblock` hors `strict`, `fill`, `clone`) envoie les mises à jour de forme et de voisin comme un placement normal (la page "Commands/setblock" ne le dit que pour l'inverse : `strict` les supprime ; la page "Block update" dit que les blocs posés par commande envoient des mises à jour de voisin). Provisoire : mises à jour envoyées pour chaque bloc modifié. Sonde : SK15 ; critère de succès : escalier posé contre un escalier : la propriété `shape` est recalculée sans `strict`, inchangée avec. | S |
| R28 | Le mode `strict` (`/setblock`, `/fill`, `/clone`, ajouté en 25w02a) place les blocs tels quels, sans mise à jour de forme ni de voisin (page "Commands/setblock"). Provisoire : aucune mise à jour envoyée, ni par le bloc posé ni vers ses voisins, ni tick planifié par le placement. Sonde : SK15 ; critère de succès : file de ticks vide après `setblock water strict`, file non vide sans `strict`. | S |
| R29 | `fill` modifie les blocs de la zone dans un ordre fixe et envoie les mises à jour au fil des placements ; le résultat sur les blocs dépendants de la forme (escaliers, murs, vitres) dépend donc de l'ordre. Provisoire : ordre x croissant, puis y, puis z (hypothèse H-U1, à confirmer) avec mises à jour immédiates après chaque placement. Sonde : SK16 ; critère de succès : rangée de huit escaliers de directions alternées posée par `fill` : états finaux égaux à ceux d'une suite de huit `setblock` dans l'ordre retenu ; recherche parmi les 6 ordres d'axes et les signes pour en identifier un qui reproduit les trois zones de 8 x 8 x 8 de la sonde. | S |
| R30 | Un bloc qui reçoit une mise à jour de forme ou de voisin et qui ne peut plus survivre (support retiré, eau absente...) se remplace par de l'air (ou par son fluide) et produit ses butins (jalon M6) ; la vérification de survie est le prédicat `would_survive` du registre `block_predicate_type` (`worldgen-features-structures.md` R24). Provisoire : un bloc dont le support disparaît est détruit au plus tard à la mise à jour de forme suivante, avec son butin. Sonde : SK15 (variante support retiré) ; critère de succès : torche posée sur un bloc, bloc retiré par `setblock air` : la torche a disparu sans pas de tick. | S |
| R31 | Beaucoup de changements d'état déclenchés par le jeu n'envoient pas de mise à jour de voisin (croissance de culture, écoulement d'eau, bambou ; page "Block update") alors qu'ils envoient une mise à jour de forme que les observateurs voient (page "Observer"). Provisoire : la croissance d'une culture envoie une mise à jour de forme et aucune mise à jour de voisin. Sonde : SK14 (variante culture) ; critère de succès : observateur tourné vers une culture : impulsion à la croissance ; distributeur sous alimentation quasi-connectée adjacent à la culture : indicateur `triggered` inchangé. | S |

### Événements de blocs, phases, persistance

| Réf | Règle | Preuve |
|---|---|---|
| R32 | Les événements de blocs (pistons, blocs de notes, coffres, cloches) forment une file FIFO traitée à la phase TK4.n, après les ticks aléatoires. Provisoire : file FIFO traitée après TK4.j. Sonde : SR6 de `sim-growth-redstone-vectors.md` (variante FIFO) ; critère de succès : deux pistons alimentés par la même mise à jour de voisin et concurrents pour le même bloc : celui qui reçoit la mise à jour en premier (R24) déplace le bloc. | S |
| R33 | Le moment où un piston commence à bouger dépend de la phase pendant laquelle il reçoit l'alimentation : 0 tick d'attente si l'alimentation arrive pendant un tick planifié, un tick aléatoire ou un événement de bloc ; 1 tick si elle arrive pendant les entités, les block entities ou par action du joueur (page "Piston", "Start delay"). Provisoire : ces deux délais selon la phase. Sonde : SR6 (variante phase) ; critère de succès : début de mouvement du piston relevé au tick près pour les quatre sources d'alimentation du montage. | S |
| R34 | Les commandes reçues par RCON s'exécutent dans la phase de tâches en attente (TK5 de `server-game-loop.md`, S), donc entre deux ticks de simulation ; sous gel, avant le premier pas qui suit. Provisoire : une commande reçue avant `tick step n` est vue par la phase TK4.f du premier tick du pas. Sonde : SK1 ; critère de succès : eau posée par `setblock` puis `tick step 5` : le tick d'eau planifié avec un délai de 5 est exécuté au cinquième tick du pas, ni au quatrième ni au sixième. | S |
| R35 | Les chunks générés qui contiennent des fluides sur leur bordure reçoivent, à la conversion en `full`, des ticks planifiés de fluides pour les positions de `PostProcessing` (`worldgen-pipeline.md` R21, S). Provisoire : un tick de fluide par fluide posé sur une bordure. Sonde : SK17 ; critère de succès : lecture de `PostProcessing`, `block_ticks` et `fluid_ticks` d'un chunk à `features` puis à `full` : aucune position à `full`, un tick de fluide pour chaque fluide posé sur une bordure. | S |

## Entrées / sorties

- **Planification** : entrée = (type, position, délai >= 1, priorité) ; sortie = entrée en file ou rien (doublon, R7).
- **Exécution d'un tick de jeu** : entrée = temps de jeu, files, état du monde ; sortie = états modifiés, nouvelles entrées, mises à jour envoyées.
- **Tick aléatoire** : entrée = chunk qui tique, vitesse n ; sortie = n tirages par section (R17).
- **Mise à jour** : entrée = position modifiée, ancien et nouvel état, drapeaux (`strict` ou non) ; sortie = suite ordonnée d'appels aux voisins (R23 à R26).
- Coordonnées négatives : les sections et chunks se calculent par division par défaut (floorDiv) ; le tirage d'une section ne dépend pas du signe de ses coordonnées.

## Cas limites

- Délai 0 ou négatif demandé à la planification (R14).
- Plus de 65 536 entrées dues au même tick (R13), y compris réparties sur plusieurs chunks.
- Entrée dont la position est dans un chunk déchargé avant l'échéance (R9) ; entrée dont le bloc a changé de type (R8) ; entrée pour un fluide dont l'état est devenu un bloc plein.
- `random_tick_speed` très grand (4096 et plus) : un bloc tiré plusieurs fois par tick ; valeur entière maximale du type entier.
- Section de la hauteur du monde la plus basse (Y -64) et la plus haute (Y 304 à 319) : toutes les 24 sections de l'Overworld tirent.
- Mise à jour qui traverse une bordure de chunk non chargé : la position est ignorée (hypothèse H-U2, sonde SK17 variante).

## Vecteurs de test

`sim-ticks-updates-vectors.md` : SKV1 à SKV6 (calculs sur les données et sur les règles), harnais SK0, sondes SK1 à SK17, annexe `sim_ref.py`.

## Plan de tests

| Niveau | Test | Règles |
|---|---|---|
| L1 | File de ticks : tri (échéance, priorité, ordre) sur SKV5 ; unicité ; consommation ; budget synthétique de 65 536 | R4, R6, R7, R11, R13, R14 |
| L1 | Sélection aléatoire : n tirages par section avec remise (test statistique à seuil fixé : moyenne et variance du nombre de blocs distincts sur 400 essais) | R17, R18 |
| L1 | Ordres de mises à jour : une scène à six voisins observateurs, ordre enregistré, comparé aux listes de R23 à R25 | R23 à R25 |
| L1 | `strict` : aucune mise à jour envoyée | R28 |
| L2 | Sondes SK1 à SK17 de l'oracle puis comparaison de l'instantané de région (blocs, `block_ticks`, `fluid_ticks`) à l'état canonique de `VERIFICATION.md` | toutes |
| L4 | Scénario de bout en bout : un monde plat, trois montages redstone et fluides sous `/tick step`, instantanés comparés tick à tick | R10, R15, R16, R34 |

Les tests de niveau A sont écrits en aveugle depuis cette spec (VERIFICATION.md, règles anti-biais).

## Questions ouvertes

- **Profondeur des chaînes de mises à jour.** La page publique ne dit rien de la profondeur maximale ni de ce qui se passe au-delà. Aucune règle ne la fixe. Campagne exploratoire proposée à l'`oracle-differ` : chaînes de 100, 1000, 5000 et 20 000 maillons pour chacune de trois familles candidates (traînée de blocs dépendants du support, chaîne de comparateurs, chaîne de rails), relever si l'effet traverse toute la chaîne, si le serveur survit, si des mises à jour sont perdues. Bloquant potentiel pour la redstone avancée (suppression de mises à jour), pas pour le jalon M5 de base.
- **Délai 0 de planification (H-K4).** Traité comme 1 par défaut ; SK6 tranche si un composant du jeu planifie à 0.
- **Unicité par (position, type) pour tous les types (H-K1).** Connue pour le répéteur par la page publique ; SK3 la mesure pour l'eau et la lampe ; les autres types sont supposés alignés.
- **Ordre des chunks et des sections dans un tick.** Non imposé (R21) ; à conserver comme dette si une sonde statistique montre une dépendance.
- **Météo.** La machine à états (durées de pluie, d'orage, de ciel clair) n'est pas spécifiée ici ; les sondes forcent `/weather` et `advance_weather false`. À spécifier avec `server-commands.md` ou en M8.
- **Mises à jour sur chunk non chargé (H-U2).** Provisoire : ignorées ; SK17 variante.
- **Unité de budget (R13).** Le budget peut s'appliquer par file ou au total ; SK9 le départage.
- **Tableau des types qui réagissent aux ticks aléatoires.** Tiré de la page publique (S) ; un balayage exhaustif des 1286 blocs par SK10 est prévu pour remplacer la liste de la page (voir `sim-growth-redstone.md`, SG1).

Règles S restantes (aucune sonde exécutée, EULA non acceptée) : S : R4 à R35.

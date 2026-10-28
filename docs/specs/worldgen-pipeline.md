# Worldgen Overworld : séquencement des statuts, lumière initiale, comparaison avec l'oracle

Statut : brouillon
Jalon : M4
Risque : A
Règles : 44 (R1 à R44). D 7, D/S 1 (valeur lue, usage S), C 1, M 0, S 35.

Cette spec ordonne les trois tiers de la génération (forme du terrain, `worldgen-surface-carvers.md`, `worldgen-features-structures.md`) dans les statuts de chunk de 26.3 et définit comment comparer notre pipeline à l'oracle étape par étape, sans lire le code du jeu.

## Périmètre

Couvre :

- le registre des statuts de chunk, l'ordre, le voisinage requis par chaque statut, ce que chaque statut produit ;
- la place de chaque sous-système (biomes, densité, aquifères, règles de matière, veines, carvers, structures, features, lumière, créatures initiales) dans les statuts ;
- les heightmaps et la lumière **initiale** au sens du pipeline (propagation : jalon M5) ;
- l'indépendance vis-à-vis de l'ordre de génération, le déterminisme ;
- la méthode de comparaison à l'oracle : état canonique, empreintes, ablation par datapack, observation des statuts intermédiaires.

Ne couvre pas :

- le calcul de la lumière lui-même, les ticks, la physique des fluides (M5) ;
- les tickets de chargement, distances de vue et déchargement (spec du serveur) ;
- le format de stockage (`world-chunk-format.md`, `world-anvil-region.md`) ;
- les presets plat et débogage, le Nether, l'End.

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `generated/reports/registries.json` (registre `minecraft:chunk_status`, 10 entrées), `resources/data/minecraft/worldgen/{world_preset,noise_settings,biome,structure}/**`, `resources/data/minecraft/dimension_type/overworld.json`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki "Chunk format" (liste des 10 statuts ; ne décrit pas leur contenu ni le voisinage ; note que `noise`, `surface` et `carvers` sont remplacés par `terrain` en 26.3 snapshot 10), "Heightmap", "Aquifer", "Carver", "Material rule", "Placed feature" (voir les autres specs).
- **Rapports locaux** (indices) : rapport Overworld chapitres 1.3, 1.4, 4.1 et 9 (la table de statuts n'est pas un ordonnanceur ; les rayons de voisinage restent à établir) ; rapport 1.21.1 (pipeline pédagogique de l'ancienne version, avec statuts séparés, non retenu comme règle).
- **Sondes de l'oracle** : aucune exécutée (EULA non acceptée). Sondes WP0 à WP8 ci-dessous.

Convention de preuve : D, C, M, S comme dans les autres specs de ce jalon ; une règle S donne son comportement provisoire.

## Règles

### Statuts (données)

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le registre `minecraft:chunk_status` contient 10 statuts, `protocol_id` : `empty` 0, `structure_starts` 1, `structure_references` 2, `biomes` 3, `terrain` 4, `features` 5, `initialize_light` 6, `light` 7, `spawn` 8, `full` 9 ; défaut `minecraft:empty` (`world-chunk-format.md` R27). | D |
| R2 | Il n'existe pas de statut `noise`, `surface` ni `carvers` : le bruit, la surface et les carvers sont regroupés dans `terrain`. Un chunk sauvegardé avec un de ces statuts vient d'une version antérieure (`world-chunk-format.md` R29). | D |
| R3 | Un chunk est identifié par (x, z) en chunks de 16 x 16 blocs ; la hauteur est celle de la dimension (Overworld : `min_y` -64, `height` 384, 24 sections) (D). Un chunk n'est complet qu'au statut `full` ; les autres sont des proto-chunks. Provisoire : seul un chunk au statut `full` est servi comme chunk complet. Sonde : WP1 (statuts écrits sur disque). | D (valeurs) / S (statut complet) |
| R4 | Les données de génération viennent du préréglage du monde (`world_preset/normal.json` : trois dimensions ; l'Overworld utilise le générateur `noise`, la source de biomes `multi_noise` de préréglage `minecraft:overworld`, les réglages de bruit `minecraft:overworld`). | D |
| R5 | La source de biomes Overworld couvre 56 biomes (`biome_parameters/overworld.json`) ; les 11 listes de features de chacun sont celles de `worldgen-features-structures.md` R6. | D |
| R6 | Cinq dépendances de données précèdent toute génération : préréglage -> dimension -> générateur -> réglages de bruit (fonctions de densité, règle de matière, aquifères) -> biomes -> (carvers, features, structures). Le traitement d'une ressource manquante est la règle R44. | C |
| R7 | `legacy_random_source` vaut faux pour les réglages de l'Overworld. La famille de générateur employée pour l'aléa de placement des structures (`random_spread`) est inconnue : WT1 (`worldgen-features-structures-vectors.md`) relève les candidats et identifie la dérivation. Provisoire (non fidèle) : source dérivée de la graine et de la région sans présupposé de famille ; écart attendu avec l'oracle jusqu'à WT1. | D (valeur) / S (usage) |

### Ordre et voisinage

| Réf | Règle | Preuve |
|---|---|---|
| R8 | L'ordre des statuts est l'ordre des `protocol_id` : un chunk atteint le statut n après n - 1 ; un chunk déjà à un statut supérieur n'est pas régénéré. Provisoire. Sonde : WP1 (échelle des statuts) ; critère de succès : pour chaque chunk écrit, le statut est celui prévu par R9 et aucun chunk n'est écrit à un statut qui sauterait un échelon. | S |
| R9 | Chaque statut a un voisinage requis : pour atteindre le statut n, le chunk a besoin que des chunks voisins soient déjà à des statuts donnés dans un rayon donné. Provisoire (hypothèse de travail à confirmer par WP1) : `empty` et `structure_starts` : rayon 0 ; `structure_references` : voisins à `structure_starts` jusqu'à 8 chunks ; `biomes` : rayon 0 ; `terrain` : voisins à `structure_references` ou `biomes` jusqu'à 8 chunks (carvers : sources jusqu'à 8 chunks ; départs de structures jusqu'à 8 chunks pour l'adaptation du terrain) ; `features` : voisins à `terrain` dans un rayon de 1 ; `initialize_light` : rayon 0 après `features` ; `light` : voisins à `initialize_light` dans un rayon de 1 ; `spawn` : rayon 0 ; `full` : rayon 0. | S |
| R10 | Conséquence mesurable (WP1) : après la génération d'un seul chunk jusqu'à `full`, les chunks voisins écrits sur disque portent des statuts décroissants avec la distance de Chebyshev au chunk cible. Cette échelle est l'observation à comparer aux rayons de R9 ; elle dépend des tickets de chargement du serveur : le test impose un ticket minimal (un seul `forceload` d'un chunk, nouveau monde). Provisoire : l'échelle des rayons de R9 est reproduite telle quelle. Critère de succès de WP1 : table (distance de Chebyshev, statut) égale à celle de l'oracle pour les trois chunks cibles et pour la variante à deux chunks adjacents. | S |
| R11 | La génération d'un chunk ne modifie pas un voisin déjà au statut `full` d'une façon dépendante de l'ordre de génération (voir R30) ; elle peut modifier un voisin de statut inférieur (écritures de features dans la zone 3 x 3, `worldgen-features-structures.md` R15). Provisoire : un chunk plus récent n'écrit que dans les voisins dont le statut est inférieur à `full` ; une écriture vers un voisin déjà `full` est ignorée (`worldgen-features-structures.md` R15). Sondes : WP4 (ordre de génération) et WF7 (écritures hors du chunk). Critère de succès : états canoniques identiques entre les trois ordres de WP4. | S |

### Contenu des statuts

| Réf | Règle | Preuve |
|---|---|---|
| R12 | `structure_starts` : évalue les ensembles de structures pour le chunk (`worldgen-features-structures.md` R51 à R56) et stocke les départs ; n'écrit aucun bloc ; ne dépend ni du terrain, ni de la forme finale du sol (les structures `WORLD_SURFACE_WG` interrogent la hauteur du terrain : elles requièrent donc un calcul de hauteur de terrain disponible au moment du départ, avant `terrain`, par la fonction de densité seule). Provisoire : les départs sont évalués à partir de la source de biomes et d'une hauteur de terrain tirée de la fonction de densité seule, sans bloc. Sondes : WP1 (chunk laissé à `structure_starts` : lecture de `structures.starts`, aucun bloc écrit), WT1 et WT5. Critère de succès : départs identiques à ceux de l'oracle sur les zones de R38. | S |
| R13 | `structure_references` : pour chaque structure, enregistre dans le chunk les départs proches dont l'emprise le rencontre (`worldgen-features-structures.md` R64). Provisoire : un chunk porte les références des départs dont l'emprise le rencontre, parmi les départs situés dans le voisinage de R9. Sonde : WT7 (lecture des `References` voisines). Critère de succès : ensembles de références égaux à ceux de l'oracle pour 100 départs. | S |
| R14 | `biomes` : remplit la grille des biomes du chunk (cellules 4 x 4 x 4, 64 par section) à partir de la source de biomes. La grille est complète pour toute la hauteur. Provisoire : une grille de 64 cellules par section, 24 sections, calculée par la source de biomes (`worldgen-noise-density.md` R45). Sondes : WP1 (chunk laissé à `biomes`) et WN20 (b). Critère de succès : égalité exacte des 1 536 cellules sur les zones de R38. | S |
| R15 | `terrain` regroupe, dans cet ordre (provisoire, WP2) : (1) remplissage de densité solide/air/fluide avec l'influence des départs de structure (`terrain_adaptation`) et les aquifères ; (2) règle de matière (bedrock, veines, surface, deepslate) ; (3) carvers, dans l'ordre de la liste du biome du chunk, chacun avec son aléa propre ; (4) mise à jour des heightmaps de génération (`_WG`). Critère de succès de WP2 : positions d'or hors grottes identiques entre AB1 et AB3 (0 écart) et or absent des planchers de carvers. | S |
| R16 | Au sein de `terrain`, la règle de matière voit le terrain **avant** les carvers (`worldgen-surface-carvers.md` R24) ; les carvers voient la surface déjà posée. Preuve attendue : WP2 (règle `on_floor` -> or : l'or n'apparaît pas sur les planchers de grotte). Provisoire : la règle de matière est évaluée avant les carvers (R15). Critère de succès de WP2 : aucun bloc d'or sur un plancher de grotte de carver. | S |
| R17 | `features` : amorce les heightmaps non `_WG`, écrit les structures puis les features par étape (`worldgen-features-structures.md` R12), tient les heightmaps à jour, enregistre les positions à retraiter dans `PostProcessing`. Provisoire : ordre de `worldgen-features-structures.md` R12, heightmaps non `_WG` amorcées au début du statut. Sondes : WP3 (différence `terrain` -> `full`), WP5 et WH1. Critère de succès : égalité exacte des blocs, des block entities et des heightmaps avec l'oracle. | S |
| R18 | `initialize_light` : prépare la lumière du chunk : sections, sources de lumière du ciel et des blocs, drapeau `isLightOn` faux (`world-chunk-format.md` R21 : "non documenté"). Aucune propagation entre chunks. Provisoire ; WP7. | S |
| R19 | `light` : propage la lumière du ciel et des blocs dans le chunk et ses voisins à portée 1, écrit `SkyLight` et `BlockLight` des sections (`world-chunk-format.md` R14 et R15) ; le calcul est le sujet de M5. Provisoire : au statut `light`, la lumière du chunk est définitive pour les blocs présents ; seuls des changements de blocs ultérieurs la modifient. WP7. | S |
| R20 | `spawn` : génération initiale des créatures du chunk neuf (selon la probabilité d'apparition des créatures du biome et les règles d'apparition), après la lumière (les apparitions testent la luminosité et le sol). Elle emploie un aléa propre au chunk. Le contenu est un sujet de M7 ; ici seulement la place et la condition : exécutée une fois, à la création du chunk, pas à la relecture d'un chunk déjà généré. Provisoire ; WP8. | S |
| R21 | `full` : conversion en chunk complet : heightmaps persistantes, block entities, ticks, traitement des positions de `PostProcessing`. Ce traitement planifie des mises à jour (fluides) dont l'effet apparaît quelques ticks plus tard. Provisoire ; WP5. | S |
| R22 | Le statut intermédiaire `terrain` peut être sauvegardé tel quel (proto-chunk) : un chunk à `terrain` contient les blocs d'après carvers et avant features, des biomes complets, des départs, des références, des heightmaps `_WG`. Provisoire ; WP1, WP3. | S |

### Heightmaps pendant la génération

| Réf | Règle | Preuve |
|---|---|---|
| R23 | Les heightmaps `_WG` (`WORLD_SURFACE_WG`, `OCEAN_FLOOR_WG`) sont disponibles dès la fin de `terrain` ; elles servent aux départs de structure (25 structures avec projection `WORLD_SURFACE_WG`) et à 27 usages de features (19 `WORLD_SURFACE_WG` et 8 `OCEAN_FLOOR_WG`, dont 3 par `surface_relative_threshold_filter` ; R22 de `worldgen-features-structures.md`). Provisoire. Sonde : WH1 (chunk laissé à `terrain` : types de heightmap présents et valeurs). Critère de succès : valeur de chaque colonne égale à celle de l'oracle sur 100 chunks. | S |
| R24 | Les heightmaps non `_WG` sont celles de `features` (`MOTION_BLOCKING` 47 usages, `OCEAN_FLOOR` 37, `MOTION_BLOCKING_NO_LEAVES` 3) ; elles sont amorcées à partir des blocs au début de `features`. Provisoire. WH1. | S |
| R25 | Aucune heightmap n'est recalculée depuis zéro après `full`, hors changements de blocs. Provisoire : les heightmaps ne sont mises à jour que par les poses de blocs ultérieures. Sonde : WH1 (chunk relu après `full`) ; critère de succès : heightmaps égales à celles recalculées depuis les blocs de la colonne. | S |

### Lumière initiale

| Réf | Règle | Preuve |
|---|---|---|
| R26 | Ciel : provisoire, au-dessus du plus haut bloc opaque de la colonne (heightmap de ciel), la lumière du ciel vaut 15 ; en dessous, elle se propage depuis les colonnes voisines avec atténuation de 1 par bloc et l'opacité des blocs. Toute la discussion est M5 ; ici : les sections sans bloc au-dessus d'elles reçoivent 15 partout (`world-chunk-format.md` R15). Critère de succès de WP7 : égalité exacte des tableaux `SkyLight` des sections sur 100 chunks. | S |
| R27 | Blocs : les blocs émetteurs (torches, lave, champignons, froid...) donnent leur niveau de lumière, atténué de 1 par bloc. Les blocs émetteurs posés par les features et les structures comptent : donc `light` suit `features`. Provisoire : les blocs émetteurs posés par features et structures sont comptés avant `light`. Sonde : WP7 ; critère de succès : égalité exacte des `BlockLight` sur 100 chunks contenant des torches, de la lave ou des champignons. | S |
| R28 | La lumière écrite à `light` est celle du chunk et des chunks voisins : un bloc émetteur à la bordure éclaire le chunk voisin. Provisoire : la propagation franchit la bordure de chunk. Sonde : WP7 (source lumineuse à la bordure) ; critère de succès : valeurs de `BlockLight` du chunk voisin égales à celles de l'oracle sur les 16 positions de la bordure. | S |
| R29 | Le calcul de lumière ne requiert pas les créatures ni les entités : `spawn` est après. Provisoire : la lumière ne lit ni créature ni entité. Sonde : WP8 (chunks avec et sans `doMobSpawning`) ; critère de succès : `SkyLight` et `BlockLight` identiques entre les deux exécutions. | S |

### Ordre d'exécution, déterminisme

| Réf | Règle | Preuve |
|---|---|---|
| R30 | Le résultat d'un chunk au statut `full` ne dépend ni de l'ordre dans lequel les chunks ont été demandés, ni de la présence ou de l'absence d'autres chunks, ni d'un rechargement du monde (rapport Overworld 9.2 : l'ordre des requêtes est une dimension du corpus de test). Provisoire ; WP4. | S |
| R31 | Une exécution identique (graine, datapacks, options) produit un état canonique identique (R36). WP6. Provisoire : une exécution est déterministe. Critère de succès de WP6 : différence vide hors `LastUpdate` et `InhabitedTime`. | S |
| R32 | Les graines de chunk sont dérivées de la graine du monde et des coordonnées de chunk (jamais de l'heure, du nombre de joueurs, du nombre de threads). Provisoire : graines dérivées de la graine du monde et des coordonnées seulement. Sondes : WP4 (ordres et nombre de threads) et WP6 ; critère de succès : états canoniques identiques. | S |
| R33 | Aucune étape ne lit un chunk voisin au-delà du voisinage de R9. Une implémentation qui lit au-delà donne des résultats dépendant de l'ordre. Provisoire : aucune lecture hors du voisinage de R9. Sonde : WP4 ; critère de succès : états canoniques identiques entre les trois ordres de génération. | S |

### Comparaison avec l'oracle

| Réf | Règle | Preuve |
|---|---|---|
| R34 | État canonique d'un chunk, indépendant de la palette : (a) états de blocs par position, (b) biome par cellule 4 x 4 x 4, (c) block entities (identifiant, position, NBT comparé comme arbre, ordre des clés ignoré), (d) départs de structure (identifiant, chunk, pièces avec boîte et identifiant), (e) références de structure, (f) heightmaps, (g) listes de ticks, (h) lumière, (i) `Status`. Une empreinte SHA-256 par sous-état (VERIFICATION.md). Provisoire : ce découpage est celui des empreintes de `tools/diff`. Sonde : WP6 ; critère de succès : empreintes SHA-256 identiques par sous-état entre deux générations de l'oracle de même entrée. | S |
| R35 | La comparaison de génération est **stratifiée** : biomes -> forme du terrain (règle de matière neutre, pas de carvers, pas de structures, pas de features) -> surface -> aquifères -> carvers -> structures (départs, puis pièces) -> features -> lumière. Chaque niveau n'est lancé que si le précédent est identique sur l'échantillon. Provisoire : ordre des niveaux tel que ci-dessus. Sondes : WP2 (invariance de l'ablation) et WP3 ; critère de succès : à chaque niveau identique, le niveau suivant se lance sans régression des niveaux précédents. | S |
| R36 | Deux méthodes d'observation d'un état intermédiaire, sans lire le jeu : (A) **ablation par datapack** (AB0 à AB7 ci-dessous) : on fait générer à l'oracle un monde où les étapes suivantes sont désactivées, et on compare à notre pipeline désactivé pareillement ; (B) **statuts intermédiaires** : un chunk voisin laissé à un statut inférieur par le serveur donne, tel quel, l'état du chunk à ce statut ; on le compare à notre état à ce statut (WP3). La méthode B n'altère aucune donnée ; la méthode A suppose que la désactivation ne change pas les étapes précédentes (à vérifier : WP2). Provisoire : les deux méthodes sont employées, la méthode B en repli. Critère de succès de WP2 (méthode A) : blocs hors des zones touchées identiques entre AB0 et AB1 ; de WP3 (méthode B) : différence bloc par bloc entre `terrain` et `full` limitée aux positions des étapes suivantes. | S |
| R37 | Table d'ablation. AB0 : monde normal, rien de désactivé. AB1 : sans features (listes `features` vides sur les 56 biomes). AB2 : AB1 et sans structures (`structure_set` vides). AB3 : AB2 et sans carvers (listes `carvers` vides). AB4 : AB3 et sans aquifères (objet `aquifers` retiré). AB5 : AB4 et règle de matière réduite à pierre (aucune surface, aucune veine, aucun bedrock). AB6 : AB5 avec règle réduite à la surface seule. AB7 : AB6 avec densité finale FLAT. La suite AB7 -> AB0 isole une étape par différence : AB6 - AB7 : densité ; AB5 - AB6 : surface ; AB4 - AB5 : veines et bedrock ; AB3 - AB4 : aquifères ; AB2 - AB3 : carvers ; AB1 - AB2 : structures ; AB0 - AB1 : features. Provisoire : table telle quelle. Sonde : WP2 ; critère de succès : pour chaque couple d'ablations successives, les blocs hors de la zone de l'étape isolée sont identiques (0 écart). | S |
| R38 | Échantillons : les graines et zones sont celles de `tools/diff/src/main/resources/samples.json` (créé en M0, figé ; il contient à ce jour 7 graines et 6 zones de 16 chunks sans structure ni biome rare, les familles de relief étant sans zone). Cette spec impose qu'il contienne au moins : graines 0, -1, 12345, `Long.MIN_VALUE`, `Long.MAX_VALUE` ; zones à cheval sur x = 0 et z = 0, sur la frontière de régions de fichier (chunk 31 et 32, -1 et -32, -33) ; zones contenant un village, un monument, un manoir, une cité antique, une chambre d'épreuve, un stronghold de l'anneau 1, une mine, un océan chaud, une plage, des badlands, des champignons, des piques de glace, un marais, des grottes luxuriantes, de dripstone, sulfurées ; proposition de modification de ce fichier à passer par une PR dédiée (VERIFICATION, règle 7). Provisoire : liste minimale à respecter. Sondes : WP4 et WT1 (contrôle du contenu de chaque zone par `/locate` depuis son centre) ; critère de succès : chaque zone annoncée contient la structure ou le biome annoncé. | S |
| R39 | Le rapport de divergence (VERIFICATION.md) ajoute les valeurs d'étape : `biomes`, `density`, `surface`, `veins`, `aquifers`, `carvers`, `structure_starts`, `structure_pieces`, `features`, `light`, `entities`. Chaque rapport nomme l'ablation AB utilisée. Provisoire : ces valeurs d'étape sont celles de R35. Sonde : WP6 ; critère de succès : rapport `IDENTIQUE` pour deux générations de l'oracle de même entrée. | S |

### Mise en garde sur les presets

| Réf | Règle | Preuve |
|---|---|---|
| R40 | Les presets `amplified` et `large_biomes` reprennent ce pipeline avec d'autres fonctions de densité et de climat ; `flat` et `debug_all_block_states` ont leurs propres générateurs (`flat`, `debug`) et aucun statut de bruit. Hors M4. | D |
| R41 | Un chunk en provenance d'une ancienne version (statuts `noise`, `surface`, `carvers`, mélange `blending_data`) est hors périmètre (`world-chunk-format.md` R21 et R29). | D |
| R42 | Le serveur de référence génère les chunks en parallèle ; l'implémentation n'est pas tenue de le faire, mais ne doit pas dépendre de l'ordre (R30). Provisoire : la parallélisation est facultative. Sonde : WP4 (second serveur à nombre de threads différent) ; critère de succès : états canoniques identiques. | S |
| R43 | Une demande de génération d'un chunk déjà généré à un statut supérieur ou égal est sans effet. Provisoire : la demande est ignorée. Sonde : WP1 (variante : forcer un chunk déjà écrit) ; critère de succès : empreintes des sous-états inchangées hors `LastUpdate`. | S |
| R44 | Une erreur de génération (ressource manquante, aucune fonction de densité pour un champ du routeur) fait échouer le chunk avec le nom de la ressource, sans écrire un chunk partiel. Provisoire : une erreur de chargement nomme la ressource et n'écrit aucun chunk. Sonde : WP0 (variante ressource inexistante) ; critère de succès : message d'erreur contenant l'identifiant de la ressource et aucun fichier de région créé. | S |

## Entrées / sorties

- **Entrée** : graine (long), préréglage, ensemble de datapacks, coordonnées de chunk, statut cible.
- **Sortie** : chunk au statut cible, état canonique (R34) ; chunks voisins modifiés (écritures de features dans la zone 3 x 3) ; empreintes.
- **Coordonnées négatives** : chunk (-1, -1) est voisin de (0, 0) par un coin ; toutes les divisions pour les régions de structures utilisent floorDiv.

## Cas limites

- Chunks sur la frontière de fichier de région (31 / 32, -1 / -32) : voisinage de rayon 8 qui traverse deux fichiers.
- Chunk à (0, 0) et (-1, -1) : écritures de features vers le chunk voisin déjà `full` ou non encore généré (R11).
- Seed extrêmes (R38) : graine égale à `Long.MIN_VALUE` : les produits de graine débordent en complément à deux.
- Demande simultanée de chunks voisins : les deux statuts intermédiaires doivent produire le même résultat final qu'en séquence.
- Chunk que le serveur a déchargé puis relu : aucun changement d'état canonique (R30).

## Vecteurs de test

Le niveau L2 de ce document est entièrement constitué de sondes (WP0 à WP8) ; aucune valeur numérique n'est calculable sans oracle. Les seuls calculs C sont dans les autres specs (SV, FV).

## Sondes

**WP0 (prérequis de toutes les sondes de génération).** Monde neuf, graine 12345, datapack de sonde qui remplace `minecraft:overworld` des réglages de bruit par une règle de matière `block(gold_block)` (MR0) ; `/forceload add 0 0` ; `/save-all flush` ; arrêt ; lecture de `region/r.0.0.mca`. Attendu : le chunk (0,0) est en or partout où il y avait du solide ; aucune erreur dans les logs. Contrôles : un datapack ajouté après la création du monde ne modifie pas des chunks déjà générés ; le serveur démarre en mode `--nogui` avec `eula.txt` accepté par le propriétaire (blocage tant que ce n'est pas fait). Variante : ressource inexistante dans le datapack (erreur de chargement).

**WP1 (R9, R10, R22).** Monde neuf normal, un seul chunk forcé ; après `/save-all flush` et arrêt, lire tous les chunks écrits : pour chacun, `Status`, `xPos`, `zPos`. Répéter pour trois chunks (origine, (-1,-1), un chunk de biome océan). Attendu : un tableau (distance de Chebyshev -> statut) qui donne les rayons de R9. Critère de succès : pour chaque chunk écrit, le couple (distance, statut) est reproductible sur les trois chunks cibles ; une distance sans chunk écrit est consignée comme telle. Répéter avec deux chunks forcés adjacents. Variante `forceload` sur 5 x 5 chunks. Risque : le serveur n'écrit pas tous les chunks intermédiaires ; dans ce cas, la sonde WP3 passe par `/save-all` sans arrêt et par un ticket plus bas.

**WP2 (R15, R16, R36).** Règle de matière `condition(on_floor) -> block(gold)` sur le monde normal sans features (AB1) : si les carvers passent après la règle, l'or ne couvre que le dessus du terrain et des cavités de bruit ; s'ils passent avant, l'or couvre aussi les planchers des grottes de carvers. Comparer AB1 et AB3 : les positions d'or hors grottes doivent coïncider. Test d'invariance de l'ablation : les blocs hors des zones touchées par la différence doivent être identiques entre AB0 et AB1 (aucune étape précédente ne change quand on retire les suivantes). Critère de succès : voir R15, R16 et R37.

**WP3 (R17, R22, R36).** Pour un chunk C au statut `terrain` laissé par le serveur (voisin à distance r de WP1) et le même chunk C régénéré à `full` par un second monde de même graine et mêmes datapacks (chunk forcé) : différence bloc par bloc = effet des statuts `features` à `full` ; comparer ensuite avec notre pipeline arrêté à `terrain` et à `full`.

**WP4 (R11, R30, R32, R42).** Générer une zone de 9 x 9 chunks de trois manières : (a) un forceload de l'ensemble, (b) un chunk à la fois en spirale depuis le centre, (c) un chunk à la fois en ordre aléatoire, avec arrêt et relance du serveur entre. Attendu : état canonique identique pour tous les chunks `full` communs ; sinon la première divergence nomme l'étape. Compléter par un second serveur lancé avec un nombre de threads différent (paramètre du serveur à documenter dans `tools/oracle`). Critère de succès : état canonique identique sur les 81 chunks pour les trois montages et pour les deux nombres de threads.

**WP5 (R17, R21).** Lire `PostProcessing`, `block_ticks`, `fluid_ticks` d'un chunk proto (`features`) et du même chunk à `full` : les positions de `PostProcessing` doivent disparaître et des ticks apparaître. Critère de succès : aucune position de `PostProcessing` à `full` et un tick de fluide planifié pour chaque fluide posé sur une bordure.

**WP6 (R31).** Deux générations indépendantes de la même zone et des mêmes datapacks (deux dossiers de monde, même graine) : comparer les états canoniques ; la différence attendue est vide (hors `LastUpdate`, `InhabitedTime`).

**WP7 (R18, R19, R26 à R29).** Pour un chunk à `initialize_light` (voisin lointain) et un chunk à `light` : lire `SkyLight`, `BlockLight`, `isLightOn`, sections absentes ; comparer à `full`. Placer une source lumineuse (feature ou commande) à la bordure d'un chunk et lire le chunk voisin (propagation inter-chunks). Critère de succès : voir R26 à R28.

**WP8 (R20).** Chunks neufs en `doMobSpawning` vrai puis faux (gamerule fixée avant génération) : présence d'entités dans `entities/` ; un chunk déjà généré et relu n'en reçoit pas de nouvelles. Critère de succès : zéro entité dans `entities/` avec `doMobSpawning` faux, et nombre d'entités inchangé à la relecture.

## Questions ouvertes

- **Rayons de voisinage (R9, WP1)** : S ; une mauvaise valeur casse la dépendance à l'ordre (R30) sans bruit. Tractable par observation des statuts de voisins.
- **Ordre interne de `terrain` (R15, WP2)** : tractable par ablation.
- **Source des heightmaps pendant le départ des structures (R12)** : une structure qui projette sur `WORLD_SURFACE_WG` avant `terrain` suppose un calcul de hauteur par la fonction de densité seule ; le premier tiers doit fournir une fonction "hauteur du terrain sans carvers" cohérente. À vérifier par WT8 et WT9.
- **Hypothèse du rapport (ADR 0002, M4)** : "comparaison étape par étape par datapacks neutralisant des étapes" : la méthode A (R36) est à valider par WP2 ; si l'invariance échoue, la méthode B (statuts intermédiaires) prend le relais.
- **Pas de blocage connu pour 2a-bis dans cette spec** ; les blocages possibles des deux autres specs sont énumérés à leur fin (carvers, aquifères, filons, jigsaw, fréquences legacy).

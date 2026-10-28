# Simulation de blocs : lumière

Statut : brouillon
Jalon : M5
Risque : A
Règles : 31 (R1 à R31). D 4, D/S 2, C 1, C sur S 2, S 22, M 0.

Lumière de bloc et lumière du ciel : valeurs, sources, atténuation, propagation, mise à jour après modification, dépendance aux chunks voisins, lien avec la lumière initiale de la génération (`worldgen-pipeline.md` R18 à R29), et luminosité vue par les consommateurs (croissance, apparition, fonte).

## Périmètre

Couvre, pour `engine/world` :

- les deux canaux de lumière (bloc et ciel), leur plage, leur stockage par position (le format est dans `world-chunk-format.md` R14 et R15) ;
- les sources : émission des états de blocs, bloc `light`, ciel ;
- l'atténuation par bloc (filtrage) et par face (occlusion directionnelle) ;
- l'état stable de la lumière pour une configuration de blocs, la mise à jour après modification, le délai ;
- les bordures de chunks et de monde, les dimensions sans ciel ;
- l'égalité entre la lumière écrite à la génération et l'état stable ;
- la luminosité effective que lisent les autres sous-systèmes (heure du jour, météo).

Ne couvre pas :

- l'ordre des statuts de génération, le voisinage requis par statut, l'ordre de calcul interne : `worldgen-pipeline.md` ;
- le paquet réseau de lumière et sa diffusion (M8) ;
- l'éclairage du rendu (ombrage par face, `ambient_light`, `cardinal_light`, couleurs de lumière du ciel : M9) ;
- les seuils d'apparition des monstres (M7) : seules leurs données sont citées (R6) ;
- l'algorithme interne du moteur de lumière : seule la fonction « configuration vers valeurs stockées » est spécifiée, pas la manière de la calculer.

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `resources/data/minecraft/dimension_type/*.json` (4), `resources/data/minecraft/timeline/day.json`, `generated/reports/blocks.json`. Reproduction : annexe `sim_ref.py` de `sim-ticks-updates-vectors.md` (sous-commandes `dims`, `props`, `propscan`, `lightball`), empreintes dans `sim-light-vectors.md`.
- **Documentation publique**, consultée le 2026-10-28 (résumé des 100 000 premiers caractères sur 112 215 ; la fin de la page n'a pas été lue) : Minecraft Wiki "Light" (https://minecraft.wiki/w/Light : niveaux de 0 à 15, propagation par distance de taxi, filtres de lumière du ciel, tableau d'émission des blocs, blocs à propagation directionnelle) ; "Redstone Lamp" (https://minecraft.wiki/w/Redstone_Lamp : lampe allumée = 15). La page "Light" ne décrit ni la procédure de mise à jour, ni le comportement aux bordures, ni la valeur de `has_skylight`.
- **Rapports locaux** (`docs/reference/`) : non utilisés pour cette spec ; aucune valeur, règle ou ordre n'en provient (ADR 0002, 2b).
- **Sondes de l'oracle** : aucune exécutée (EULA non acceptée). Sondes SL1 à SL13 dans `sim-light-vectors.md` ; harnais SK0 dans `sim-ticks-updates-vectors.md`.

Convention de preuve : D, M, C, S comme dans `sim-ticks-updates.md`. Hypothèses nommées H-LU1 à H-LU7 : chacune a sa sonde.

## Règles

### Données

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Les quatre types de dimension du jeu de données : `overworld` et `overworld_caves` (`min_y` -64, `height` 384, `logical_height` 384, `has_skylight` vrai), `the_nether` (`min_y` 0, `height` 256, `logical_height` 128, `has_skylight` faux, `has_ceiling` vrai), `the_end` (`min_y` 0, `height` 256, `logical_height` 256, `has_skylight` **vrai**). Le type `the_end` porte donc un ciel malgré l'affirmation de la page publique "Light" (hors Overworld, ciel toujours 0), dont l'ancienneté est probable. Commande : `sim_ref.py REF dims` (SLV1). | D |
| R2 | Nombre de sections de lumière par colonne (hauteur / 16) : 24 pour l'Overworld, 16 pour le Nether et l'End ; plus les sections de lumière seule hors hauteur (`world-chunk-format.md` R15, S). Calcul sur SLV1 (commande : `sim_ref.py REF dims`). | C |
| R3 | Le bloc `light` a 32 états : propriété `level` de 0 à 15 (valeur par défaut 15 dans l'inventaire des états) et `waterlogged`. Commande : `sim_ref.py REF props` (SLV2). | D |
| R4 | Propriétés dont l'effet sur l'émission est à mesurer : `lit` (52 blocs), `candles` 1 à 4 (17 blocs de bougies), `pickles` 1 à 4 (cornichon de mer), `charges` 0 à 4 (ancre de réapparition), `berries` (2 blocs), `bloom` (catalyseur de sculk), `level` du bloc `light` ; les autres propriétés ne sont pas exclues (SL2 balaie tous les états). Commande : `sim_ref.py REF propscan` (SLV3). | D (effectifs) |
| R5 | Luminosité du ciel pilotée par les données : la ligne de temps `minecraft:day` (horloge `minecraft:overworld`, période 24000) porte une piste `minecraft:gameplay/sky_light_level` de modificateur `multiply` dont les repères sont (133 : 1,0), (11867 : 1,0), (13670 : 0,26666668), (22330 : 0,26666668) ; 15 x 0,26666668 = 4,0 ; l'attribut `minecraft:gameplay/sky_light_level` du type `the_nether` vaut 4,0 ; l'Overworld et l'End n'ont pas cet attribut de dimension. Commande : `sim_ref.py REF dims` (SLV1). Le sens exact est S. Provisoire : effet de R29 et R31. Sonde : SL13 (a) et SL12 (c) ; critère de succès : voir R29 et R31. | D (valeurs) / S (effet) |
| R6 | Données consommées par M7 : `monster_spawn_block_light_limit` (0 Overworld et End, 15 Nether) et `monster_spawn_light_level` (uniforme 0 à 7 Overworld, constante 7 Nether, constante 15 End). Commande : SLV1. Non spécifié ici au-delà de la citation. | D |

### Sources et canaux

| Réf | Règle | Preuve |
|---|---|---|
| R7 | Deux canaux indépendants, lumière de bloc et lumière du ciel, chacun un entier de 0 à 15 par position de bloc de la hauteur du monde (page "Light"). Provisoire : deux champs entiers de 0 à 15 par position. Sonde : SL4 et SL7 ; critère de succès : les deux canaux lus dans `BlockLight` et `SkyLight` se comportent indépendamment (une source de bloc ne modifie jamais `SkyLight`, un toit ne modifie jamais `BlockLight`) sur les montages de SL4 et SL7. | S |
| R8 | Émission : chaque état de bloc a une émission entière de 0 à 15, fonction de l'état seul. Valeurs de référence de la page "Light" (S, extraits) : torche 14, lanterne 15, lave 15, pierre lumineuse 15, lanterne marine 15, torche d'âme 10, torche de redstone allumée 7, tige de l'End 14, fournaise allumée 13, bloc de magma 3, champignon brun 1, lichen lumineux 7, grappe d'améthyste 5, minerai de redstone allumé 9, feu 15, feu d'âme 10, portail du Nether 11, lampe de redstone allumée 15, balise 15, table d'enchantement 7. Provisoire (non fidèle hors liste) : les valeurs de la liste, 0 ailleurs. Sonde : SL2 (balayage des 35 723 états) ; critère de succès : table émission(état) relevée complète et égale, pour chacun des états du jeu de données, à celle de l'oracle ; la table relevée est le livrable de référence (donnée extraite). | S |
| R9 | Le bloc `light` émet la valeur de sa propriété `level` (la page précise que le niveau est réglable de 0 à 15), quel que soit `waterlogged`. Provisoire : émission = `level`. Sonde : SL2 ; critère de succès : émission relevée égale à `level` pour les 32 états. | D (propriété) / S (effet) |
| R10 | Émissions dépendant d'une propriété (page "Light") : bougie allumée 3 par bougie (1 à 4 : 3, 6, 9, 12), cornichon de mer immergé 6, 9, 12, 15 pour 1 à 4 cornichons, ancre de réapparition 0, 3, 7, 11, 15 pour 0 à 4 charges. Provisoire : ces valeurs. Sonde : SL2 ; critère de succès : voir R8, avec relevé séparé pour chaque valeur de `candles`, `pickles`, `charges`, `lit` et `waterlogged`. | S |
| R11 | Ciel : une position qui voit le ciel verticalement (aucun bloc qui filtre ou arrête la lumière entre elle et le haut de la colonne) a une lumière du ciel de 15 (page "Light" : « exposé verticalement au ciel »). Provisoire : le ciel est une source de niveau 15 injectée au-dessus du sommet du monde dans chaque colonne, dans une dimension qui a un ciel (R1). Sonde : SL7 (a) ; critère de succès : `SkyLight` égal à 15 à toute position d'une colonne ouverte, sur 16 x 16 colonnes de quatre chunks, y compris à Y 319 et au plafond d'un tunnel ouvert. | S |

### Propagation et atténuation

| Réf | Règle | Preuve |
|---|---|---|
| R12 | État stable du canal de bloc (hypothèse H-LU1) : la valeur en une position p est le maximum de l'émission de p et, pour chaque voisin q auquel p est relié par une face qui laisse passer la lumière (R16), de la valeur de q moins le coût d'entrée dans p ; le coût d'entrée vaut au moins 1 et est le filtre de p (R14) s'il est plus grand ; le résultat est borné à 0. La page publique donne l'unité : « 1 par bloc de distance de taxi ». Provisoire : plus petit point fixe de cette relation. Sonde : SL4, SL5 ; critère de succès : valeurs de `BlockLight` égales à celles de l'oracle pour toutes les positions des scènes de SL4 et SL5. | S |
| R13 | Conséquence en espace libre (C sur S) : une source de niveau e seule dans l'air éclaire en un point à distance de taxi d la valeur max(e - d, 0). Effectifs par niveau pour e = 15, 14, 10, 7, 1 : sortie de `sim_ref.py - lightball` (SLV4) ; par exemple 4089 positions éclairées pour e = 15. Provisoire : conséquence de R12. Sonde : SL4 ; critère de succès : histogramme (niveau, nombre de positions) égal à SLV4 pour les cinq niveaux et deux chunks voisins en bordure. | C sur S |
| R14 | Filtre d'un état de bloc : entier de 0 à 15 qui s'ajoute à l'atténuation d'une unité. Valeurs publiques (Java, page "Light") : l'eau, la glace et les feuilles réduisent la lumière du ciel de 1 et « n'affectent pas la lumière de bloc » (pas de réduction supplémentaire par rapport à l'unité) ; les cubes opaques arrêtent les deux canaux. Provisoire : filtre 0 pour l'air et les blocs transparents, 1 pour l'eau, la glace et les feuilles, 15 pour les cubes opaques ; autres états : 0 (non fidèle). Sonde : SL3 (balayage des états) ; critère de succès : table filtre(état) par canal (ciel et bloc) relevée complète ; la table relevée est un livrable de référence. | S |
| R15 | Valeur dans un bloc opaque : la case d'un cube opaque contient sa propre émission (0 s'il n'émet pas) pour les deux canaux, jamais la lumière de l'air voisin. Provisoire : `BlockLight` et `SkyLight` d'un cube opaque non émetteur valent 0. Sonde : SL5 (a) ; critère de succès : valeurs stockées à la position d'un bloc de pierre contre une torche et sous un ciel ouvert égales à 0. | S |
| R16 | Occlusion par face (hypothèse H-LU2) : la lumière passe d'une position p à son voisin q si et seulement si la face de p tournée vers q et la face de q tournée vers p ne couvrent pas ensemble toute la face commune ; elle est donc directionnelle pour les dalles, escaliers, couches de neige, terre labourée, chemins de terre, pistons, capteurs de lumière du jour, tables d'enchantement, cadres de portail de l'End, pupitres, tailleurs de pierre (liste de la page "Light"). Provisoire : table de passage état x face d'entrée x face de sortie relevée par SL6 ; en attendant, un bloc est soit opaque (filtre 15), soit transparent. Sonde : SL6 ; critère de succès : table de passage relevée pour toutes les paires (entrée, sortie) de tous les états listés et pour un échantillon de 1000 états tirés par graine fixée parmi les autres ; la table relevée est un livrable de référence. | S |
| R17 | État stable du canal du ciel (H-LU3) : même relation que R12 avec la source de R11, mais descendre d'une position vers sa voisine du dessous dans un milieu de filtre 0 ne coûte rien (une lumière de 15 reste à 15 en tombant dans un puits transparent), alors que monter ou aller de côté coûte au moins 1 (page "Light") ; traverser un milieu filtrant coûte son filtre au moins 1, dans tous les sens. Provisoire : relation de R12 avec un coût vertical descendant nul pour le filtre 0. Sonde : SL7 (b) et SL3 ; critère de succès : `SkyLight` égal à l'oracle sur 20 blocs d'eau (15 - 1 par bloc), 5 blocs de feuilles et un puits de verre de 40 blocs (reste 15). | S |
| R18 | Indépendance de l'heure et de la météo : les valeurs stockées de la lumière du ciel ne changent pas entre le jour et la nuit ni selon la météo (page "Light" : « la lumière du ciel n'est pas réduite la nuit »). Provisoire : stockage indépendant de l'horloge et de la météo. Sonde : SL7 (c) ; critère de succès : `SkyLight` du montage de SL7 identique à `time set` 6000, 18000 et sous orage. | S |
| R19 | Dimension sans ciel : pour `has_skylight` faux (Nether), aucune lumière du ciel n'est injectée : `SkyLight` vaut 0 partout (ou est absent). Pour `has_skylight` vrai dans l'End (R1), le ciel est injecté. Provisoire : injection si et seulement si `has_skylight` est vrai ; le ciel de l'End obéit aux règles de R11 et R17. Sonde : SL12 ; critère de succès : `SkyLight` du Nether tout à 0 ou section absente ; `SkyLight` de l'End égal à 15 en colonne ouverte (sinon R19 est réécrite avec l'observation). | S |
| R20 | Bornes verticales : le ciel est injecté au-dessus de la section la plus haute (Y 320 pour l'Overworld) ; une source posée à Y 319 ou à Y -64 éclaire dans la hauteur du monde sans erreur ; les sections de lumière seule hors hauteur (`world-chunk-format.md` R15) portent la lumière qui les traverse. Provisoire : bords du monde traités comme de l'air pour la propagation ; rien n'est écrit hors des sections de lumière seule. Sonde : SL11 ; critère de succès : valeurs égales à celles de l'oracle sur les couches Y 317 à 319 et Y -64 à -62 d'un montage d'émetteurs, sections Y -5 et Y 20 lues. | S |

### Mise à jour, chunks voisins

| Réf | Règle | Preuve |
|---|---|---|
| R21 | Indépendance de l'historique (H-LU4) : après n'importe quelle suite de modifications de blocs, les valeurs stockées sont celles de l'état stable (R12, R17) de la configuration finale, comme si la zone avait été construite d'un coup. Provisoire : lumière = fonction de la configuration. Sonde : SL8 ; critère de succès : pour 20 suites d'insertions et de retraits d'émetteurs et d'obstacles, valeurs égales à celles d'une zone construite d'un coup par `clone` (même configuration), aucun résidu après retrait d'un émetteur. | S |
| R22 | Retrait d'un émetteur ou ajout d'un obstacle : toutes les valeurs qui n'étaient dues qu'à la source ou au chemin supprimé baissent jusqu'à l'état stable ; une valeur due à une autre source reste (H-LU4). Provisoire : conséquence de R21. Sonde : SL8 (b) ; critère de succès : deux émetteurs de niveaux 15 et 10 voisins, retrait du premier, valeurs finales égales à l'état stable du second seul (distance de taxi). | C sur S |
| R23 | Délai de mise à jour (H-LU5) : après une modification faite par commande sous gel, les valeurs stockées sont l'état stable à la prochaine sauvegarde qui suit au plus un nombre fixe de ticks. Provisoire : la lumière est à jour à la fin du tick de la modification ; une mise à jour retardée d'un tick doit être signalée. Sonde : SL9 ; critère de succès : le délai en ticks (0, 1, 2...) entre la modification et l'état stable est relevé et identique sur 20 répétitions, pour une source posée, retirée et pour un obstacle. | S |
| R24 | Chunks voisins : la lumière traverse les bordures de chunks ; une source à moins de 15 positions d'une bordure éclaire le chunk voisin. Provisoire : la propagation ignore les bordures de chunk dès que les deux chunks sont chargés. Sonde : SL10 (a) ; critère de succès : `BlockLight` du chunk voisin égal à celui de l'oracle sur les 16 positions de la bordure et 15 couches à distance, pour des bordures en x et en z, aux coordonnées positives et négatives (chunks (-1, -1), (0, 0), (-1, 0)) et au coin à quatre chunks. | S |
| R25 | Chunk voisin non chargé : aucune lumière ne passe dans un chunk non chargé ; au chargement de ce chunk, ses valeurs tiennent compte des sources de ses voisins (H-LU6). Provisoire : au chargement, état stable avec les blocs du chunk et de ses voisins chargés. Sonde : SL10 (b) ; critère de succès : source à 2 positions de la bordure d'un chunk dont le voisin n'est pas chargé, puis chargement du voisin : `BlockLight` du voisin égal à l'état stable calculé sur les deux chunks. | S |
| R26 | Génération : la lumière écrite à la fin du statut `light` est l'état stable calculé avec les blocs du chunk et de ses voisins immédiats, y compris les émetteurs posés par les features et les structures (`worldgen-pipeline.md` R19, R26 à R28, S). Provisoire : un chunk à `full` porte des valeurs égales à l'état stable de la configuration finale du chunk et de ses huit voisins à `full`. Sonde : SL13 ; critère de succès : sur 100 chunks de W-NORMAL graine 12345 pris à `full`, `BlockLight` et `SkyLight` stockés égaux à l'état stable recalculé par `tools/diff` à partir des blocs stockés (aucune différence), ou les écarts sont classés (bordure, sommet de colonne) et leur nombre consigné. | S |
| R27 | Statut `initialize_light` : le chunk a ses sources de ciel et de bloc mais aucune lumière propagée vers ses voisins ; le drapeau `isLightOn` (non documenté, `world-chunk-format.md` R21) indique si la lumière est calculée. Provisoire : `isLightOn` faux avant `light`. Sonde : SL13 (variante chunk à `initialize_light`, WP7 de `worldgen-pipeline.md`) ; critère de succès : `isLightOn` et tableaux de lumière relevés pour un chunk à `initialize_light` et à `light`, consignés. | S |

### Luminosité effective (consommateurs)

| Réf | Règle | Preuve |
|---|---|---|
| R28 | Luminosité d'une position pour les autres sous-systèmes (croissance : lumière d'au moins 9 pour un blé, `sim-growth-redstone.md` ; fonte de la glace : lumière de bloc supérieure à 11, `sim-growth-redstone.md` ; apparition : M7) : le maximum de la lumière de bloc et d'une lumière du ciel effective, la lumière du ciel effective étant la lumière du ciel stockée réduite de l'obscurité due à l'heure et à la météo. Provisoire : luminosité = max(bloc, ciel - réduction), la réduction nulle à midi. Sonde : SL13 (variante prédicats) ; critère de succès : la valeur lue par un prédicat de datapack `location_check` à champ `light`, ou un autre consommateur nommé dans la sonde, en pleine lumière du jour est 15 sous ciel ouvert et 0 dans une cavité fermée sans source, et vaut la lumière de bloc à l'intérieur d'une cavité éclairée. | S |
| R29 | Réduction de nuit : la piste `sky_light_level` de `minecraft:day` (R5) multiplie un niveau de base ; aux repères, la lumière effective du ciel est 15 jusqu'au tick 11867 et 4 de 13670 à 22330 ; entre 11867 et 13670 elle varie (interpolation et arrondi inconnus, H-LU7). Provisoire : effective = 15 x valeur de la piste, interpolée linéairement entre repères, arrondie au plus proche entier, bornée de 0 à 15. Sonde : SL13 (a) ; critère de succès : pour chaque tick de 11800 à 13700 par pas de 10, la lumière effective relevée sous ciel ouvert est consignée ; critère d'acceptation : égale à la règle retenue en tout point, sinon la règle est ajustée aux relevés (monotone non croissante attendue). | S |
| R30 | Météo : la pluie et l'orage peuvent réduire la lumière effective du ciel ; le montant est inconnu. Provisoire (non fidèle) : aucune réduction par la météo. Sonde : SL13 (b) ; critère de succès : lumière effective relevée à midi par temps clair, de pluie et d'orage (`weather clear|rain|thunder`) consignée ; réduction constante vérifiée sur 10 relevés par état météo. | S |
| R31 | L'attribut `gameplay/sky_light_level` du Nether (4,0, R5) alors que `has_skylight` y est faux : effet non documenté. Provisoire : sans effet sur le stockage ; sa portée sur la luminosité effective est mesurée. Sonde : SL12 (variante) ; critère de succès : luminosité effective relevée dans le Nether sous un plafond ouvert artificiel (retrait de la roche du plafond) égale à l'une des hypothèses {0, 4}, consignée. | S |

## Entrées / sorties

- **Lumière d'une zone** : entrée = états de blocs de la zone et de ses voisins immédiats (au plus 15 positions autour), type de dimension (`has_skylight`, `min_y`, `height`) ; sortie = `BlockLight` et `SkyLight` par position (`world-chunk-format.md` R14), sections de lumière seule comprises.
- **Mise à jour** : entrée = position modifiée, ancien et nouvel état ; sortie = ensemble des positions dont une valeur change.
- **Luminosité effective** : entrée = position, temps d'horloge, météo ; sortie = entier de 0 à 15.
- Coordonnées négatives : chunks et sections en division par défaut (floorDiv), positions locales dans une section en 0 à 15 ; l'état stable est invariant par symétrie de translation de pas 16.

## Cas limites

- Source de niveau 15 contre une bordure de chunk, au coin de quatre chunks, aux coordonnées négatives (chunk (-1, -1)).
- Source à Y 319 et à Y -64 ; colonne ouverte jusqu'à Y 319 ; sections de lumière seule à Y -5 et Y 20.
- Puits transparent de 40 blocs (ciel constant) contre 20 blocs d'eau (ciel décroissant).
- Dalle à moitié couverte, escalier dont la face avant est pleine, couche de neige, terre labourée, chemin de terre, piston étendu : propagation directionnelle (R16).
- Bloc émetteur opaque (pierre lumineuse) : sa case porte son émission (R8, R15).
- Bloc `light` à `level` 0 et à 15, avec `waterlogged` vrai.
- Eau : ciel diminue de 1 par bloc en descendant, bloc diminue de 1 par bloc dans tous les sens.
- Retrait d'un émetteur voisin d'un autre : pas de résidu (R22).
- Dimension à plafond (`has_ceiling`) : le ciel injecté par le haut est arrêté par le plafond (bedrock) ; le Nether n'a pas de ciel (R19).

## Vecteurs de test

`sim-light-vectors.md` : SLV1 (dimensions et horloge, D), SLV2 (bloc `light`), SLV3 (effectifs des propriétés), SLV4 (boule de lumière en espace libre, C sur S), sondes SL1 à SL13, annexe commune dans `sim-ticks-updates-vectors.md`.

## Plan de tests

| Niveau | Test | Règles |
|---|---|---|
| L1 | Boule de lumière en espace libre : histogramme égal à SLV4 pour e = 15, 14, 10, 7, 1 | R12, R13 |
| L1 | Propriétés (test par propriétés, graines fixées) : monotonie (ajouter un émetteur ne baisse aucune valeur), localité (aucun changement à plus de 15 positions), symétrie (scène miroir en coordonnées négatives), indépendance de l'historique (suite d'édition contre construction directe) | R12, R21, R22, R24 |
| L1 | Ciel : colonnes ouvertes à 15 ; 20 blocs d'eau à 15 - 20 borné à 0 ; puits de verre constant | R11, R17 |
| L1 | Interpolation et arrondi de la piste du ciel testés aux repères de R5 (15 et 4) | R29 |
| L2 | Tables d'émission, de filtre et de passage de l'oracle chargées comme données ; écart nul sur les scènes SL4, SL5, SL7, SL8, SL10 | R8, R10, R14, R16 |
| L2 | Instantané de région, `BlockLight` et `SkyLight`, comparé à l'oracle (état canonique de `VERIFICATION.md`, étape fautive : `light`) | R12 à R27 |
| L3 | Planche triple d'une coupe horizontale de `BlockLight` (nous, référence, écart) pour SL4 | R12, R13 |
| L4 | Scénario : tunnel avec torches posées et retirées, monde généré 3 x 3 chunks, relecture après `/tick step` | R21, R23, R26 |

Les tests de niveau A sont écrits en aveugle depuis cette spec.

## Questions ouvertes

- **Tables d'émission, de filtre et de passage (R8, R14, R16).** Elles ne sont pas dans les données extraites (les propriétés des blocs sont dans `blocks.json`, pas leurs réglages de lumière) ; elles sont lues sur l'oracle par SL2, SL3, SL6 et deviennent un jeu de données du dépôt de cache (non commité), comme les autres données de référence. Risque : une table à plusieurs dizaines de milliers de lignes ; critère de réduction : classes d'équivalence d'états.
- **Occlusion par face (H-LU2).** Le modèle « faces pleines » est une hypothèse ; si SL6 la réfute, le modèle de passage devient une table mesurée et rien de plus.
- **Délai de mise à jour (H-LU5).** Inconnu ; conditionne les tests de scénarios sous `/tick step`.
- **Ciel de l'End (R19).** Les données disent `has_skylight` vrai ; la page publique dit zéro. SL12 tranche.
- **Interpolation du ciel (H-LU7) et effet de la météo (R30).** À mesurer.
- **Rechargement.** Le moteur recalcule-t-il la lumière d'un chunk au chargement si `isLightOn` est faux ? (R27, SL13.)
- **Lien avec la génération.** Si R26 échoue (écarts de bordure), la cause peut être le voisinage requis par statut (`worldgen-pipeline.md` R9) ; la sonde WP1 le mesure.

Règles S restantes (aucune sonde exécutée, EULA non acceptée) : S : R7 et R8, R10 à R12, R14 à R21, R23 à R31 ; D/S : R5, R9 ; C sur S : R13, R22.

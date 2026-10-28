# Simulation de blocs : fluides

Statut : brouillon
Jalon : M5
Risque : A
Règles : 36 (R1 à R30, chaque sous-règle a, b... comptée). D 4, D/S 5, C 0, C sur S 0, S 27, M 0.

Eau et lave : états, niveaux, écoulement, délais, sources infinies, interactions entre fluides et avec les blocs, blocs immergés. Le déclenchement (ticks de fluides, mises à jour) est dans `sim-ticks-updates.md`, l'opacité de l'eau dans `sim-light.md`.

## Périmètre

Couvre, pour `engine/world` :

- les registres et données qui concernent les fluides (types, étiquettes, propriétés de niveau, blocs immergeables, attributs de dimension) ;
- le modèle d'état d'un fluide (source, coulant, tombant) et son lien avec la propriété `level` des blocs `water` et `lava` ;
- l'écoulement : délais, portée, choix de la direction, chute, arrêt ;
- les sources d'eau et de lave infinies (gamerules `water_source_conversion` et `lava_source_conversion`) ;
- le mélange eau-lave et les transformations qui en résultent ;
- les blocs que le fluide détruit, remplace ou ne traverse pas ; les blocs immergés (`waterlogged`) ;
- l'effet des attributs de dimension `fast_lava` et `water_evaporates` sur la simulation.

Ne couvre pas :

- les effets sur les entités (flottaison, dérive, noyade, brûlure : M7) et les seaux (M6/M7) ;
- les colonnes de bulles, les algues, les herbes marines, l'éponge, le corail, les chaudrons, la glace formée par gel : listés en Questions ouvertes (second lot) ;
- la lave qui met le feu (tick aléatoire de la lave) : `sim-growth-redstone.md` ;
- le rendu de la surface des fluides (M9).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `generated/reports/registries.json` (registre `fluid`), `generated/reports/blocks.json`, `resources/data/minecraft/tags/{fluid,block}/**`, `resources/data/minecraft/dimension_type/the_nether.json`, `generated/reports/commands.json`. Reproduction : annexe `sim_ref.py` de `sim-ticks-updates-vectors.md` (sous-commandes `tags`, `props`, `propscan`, `dims`, `rules`, `floodwater`), empreintes dans `sim-fluids-vectors.md`.
- **Documentation publique**, consultée le 2026-10-28 (résumés des 100 000 premiers caractères de chaque page) : Minecraft Wiki "Water" (https://minecraft.wiki/w/Water : portée 7, 5 ticks par bloc, recherche d'une chute dans un rayon de quatre blocs, sources infinies, mélange eau-lave, immersion) ; "Lava" (https://minecraft.wiki/w/Lava : portée 3 dans l'Overworld et l'End selon le texte, 4 selon l'encadré, 7 dans le Nether ; 30 et 10 ticks ; gamerule de conversion de lave) ; "Fluid" (https://minecraft.wiki/w/Fluid : modèle de niveaux, décrément par bloc, portée de recherche 5 ou 3, création de sources, remplacement) ; "Ice" (https://minecraft.wiki/w/Ice : fonte dans le Nether sans eau). Les trois pages se contredisent sur la portée de recherche de chute et la portée de la lave ; ces points sont des hypothèses nommées avec sonde.
- **Rapports locaux** (`docs/reference/`) : non utilisés pour cette spec ; aucune valeur, règle ou ordre n'en provient (ADR 0002, 2b).
- **Sondes de l'oracle** : aucune exécutée (EULA non acceptée). Sondes SF1 à SF13 dans `sim-fluids-vectors.md` ; harnais SK0 dans `sim-ticks-updates-vectors.md`.

Convention de preuve : D, M, C, S comme dans `sim-ticks-updates.md`. Hypothèses nommées H-F1 à H-F7, chacune avec sa sonde.

## Règles

### Données

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le registre `minecraft:fluid` compte 5 entrées : `empty`, `flowing_lava`, `flowing_water`, `lava`, `water`. Les étiquettes de fluide `water` et `lava` contiennent chacune la forme source et la forme coulante (2 entrées chacune). Commande : `sim_ref.py REF tags` (SFV1). | D |
| R2 | Les blocs `water` et `lava` ont une propriété `level` de 16 valeurs (0 à 15), valeur par défaut 0 ; il n'y a pas de bloc distinct pour la forme coulante. Commande : `sim_ref.py REF props` (SFV2). | D |
| R3 | 505 blocs sur 1286 ont la propriété `waterlogged`, avec les valeurs vrai et faux. Les blocs qui contiennent de l'eau sans cette propriété (algues, herbes marines, colonne de bulles) ne sont pas lisibles dans les données. Commande : `sim_ref.py REF propscan` (SFV3). | D |
| R4 | Étiquettes de blocs liées à l'écoulement : `washed_away_by_fluids` (134 entrées directes, 254 blocs après résolution), `replaceable` (30), `blocks_fluid_flow` (2 entrées directes, `#minecraft:blocks_motion` et `#minecraft:all_signs`, 1026 blocs après résolution), `impermeable` (19 : verres et barrière, tous aussi dans `blocks_fluid_flow`), `air` (3). Recouvrements : 28 des 30 blocs `replaceable` sont aussi lavables (les deux autres : `bubble_column`, `structure_void`) ; 1 bloc (`straw_bed`) est à la fois lavable et dans `blocks_fluid_flow` ; 5 blocs ne sont dans aucune des quatre étiquettes `air`, `replaceable`, `washed_away_by_fluids`, `blocks_fluid_flow` (`end_gateway`, `end_portal`, `ladder`, `nether_portal`, `sugar_cane`). Commandes : `sim_ref.py REF tags` (SFV1) et `sim_ref.py REF fluidclasses` (SFV7). Les effets sont S (R21a à R24c). Provisoire : effets de R21a à R24c. Sonde : SF9 et SF10 ; critère de succès : voir R21a, R21b et R24a. | D (valeurs) / S (effets) |
| R5 | Attributs de dimension : `minecraft:gameplay/fast_lava` vaut vrai et `minecraft:gameplay/water_evaporates` vaut vrai dans `the_nether` seulement ; `overworld`, `overworld_caves` et `the_end` n'ont ni l'un ni l'autre. Commande : `sim_ref.py REF dims` (SFV4). Les effets sont S (R9, R27, R28). Provisoire : effets de R9, R27 et R28. Sonde : SF11 ; critère de succès : voir R27 et R28. | D (valeurs) / S (effets) |
| R6 | Gamerules : `water_source_conversion` (identifiant 58) et `lava_source_conversion` (identifiant 19), booléennes ; défauts vrai et faux (`server-game-loop.md` R40). Commande : `sim_ref.py REF rules` (SFV5). | D |

### Modèle d'état et niveaux

| Réf | Règle | Preuve |
|---|---|---|
| R7 | Trois sortes d'état de fluide : source (niveau de bloc 0), coulant (niveau de bloc 1 à 7, un niveau de fluide qui décroît avec l'éloignement de la source), tombant (niveau de bloc 8, utilisé quand un fluide de même type est au-dessus). Un fluide coulant n'est jamais au niveau 0 (page "Fluid"). Provisoire : ces trois sortes, la propriété `level` du bloc donnant 0, 1 à 7 ou 8. Sonde : SF3 et SF4 ; critère de succès : après stabilisation, `level` relevé sur toutes les cellules d'une nappe est 0 pour la source, de 1 à 7 pour les cellules coulantes et 8 pour les cellules sous un fluide. | S |
| R8 | Valeurs de `level` de 9 à 15 : présentes dans les données (R2), non décrites par les pages publiques. Provisoire (non fidèle, hypothèse H-F7) : un état de niveau supérieur à 8 se comporte comme le niveau 8 (tombant). Sonde : SF1 ; critère de succès : comportement relevé de `water[level=n]` et `lava[level=n]` pour n de 9 à 15 (stabilité de la cellule, étalement vers les voisins, niveau des voisins) consigné, puis règle réécrite. | S |
| R9 | Décrément par bloc horizontal : eau 1 ; lave 2 dans l'Overworld et l'End, 1 dans le Nether (page "Fluid"), la lave rapide étant celle d'une dimension à attribut `fast_lava` (R5). Provisoire : décrément 1 pour l'eau et la lave rapide, 2 pour l'autre lave. Sonde : SF3 ; critère de succès : niveau de `lava` relevé en fonction de la distance de taxi (2, 4, 6) en Overworld et (1 à 7) dans le Nether. | S |

### Écoulement

| Réf | Règle | Preuve |
|---|---|---|
| R10 | Portée horizontale d'une source sur sol plat sans obstacle : eau 7 ; lave Overworld et End 3 d'après le texte de la page "Lava" et 4 d'après son encadré (hypothèse H-F1) ; lave du Nether 7 d'après le texte et 8 d'après l'encadré (même hypothèse H-F1). Provisoire : 7, 3, 7. Sonde : SF3 ; critère de succès : nombre de cellules de fluide par distance de taxi (1, 4, 8, 12, ... jusqu'à la portée) égal à la sortie de `sim_ref.py - floodwater` (SFV6) pour la portée relevée, et portée relevée consignée (3 ou 4 pour la lave de l'Overworld, 7 ou 8 pour celle du Nether). | S |
| R11 | Délai d'un pas d'écoulement : eau 5 ticks, lave de l'Overworld et de l'End 30 ticks, lave du Nether 10 ticks (pages "Water", "Lava", "Fluid"). Provisoire : ces valeurs, appliquées comme délai de planification d'un tick de fluide (`sim-ticks-updates.md` R6). Sonde : SF2 ; critère de succès : le tick du pas d'ordre k d'une source sur sol plat est exécuté k fois le délai après la pose, pour k de 1 à 3, mesuré par la file sauvegardée et par l'apparition des anneaux. | S |
| R12 | Déclencheurs : un tick de fluide est planifié quand un fluide est posé, quand un voisin d'une cellule de fluide change, et quand une cellule de fluide vient de changer ; un tick de fluide exécuté ne replanifie rien si rien n'a changé. Provisoire : planification sur pose et sur mise à jour de forme ou de voisin d'une cellule de fluide, replanification des seules cellules modifiées. Sonde : SF12 ; critère de succès : après stabilisation d'une nappe (au plus portée x délai plus 20 ticks), `fluid_ticks` vides dans l'instantané, et un seul bloc ajouté contre la nappe relance l'écoulement. | S |
| R13 | Chute : si la cellule du dessous est de l'air, remplaçable ou lavable (R21a), le fluide y coule en niveau 8, quel que soit son propre niveau, et ne s'étale pas sur les côtés dans le même pas (page "Fluid" : une source en l'air coule d'abord vers le bas, puis, 5 ticks plus tard, sur ses quatre côtés). Provisoire : le fluide tombe en priorité ; l'étalement horizontal n'a lieu que si la chute est impossible. Sonde : SF4 ; critère de succès : source posée à 10 blocs de hauteur au-dessus d'un sol : la colonne se remplit de haut en bas d'un bloc par délai, la première ligne d'anneau n'apparaît qu'après l'arrivée au sol. | S |
| R14 | À l'arrivée au sol d'un fluide tombant (niveau 8), l'étalement horizontal part avec le niveau de voisin 1 (H-F2), comme pour une source, et sa portée est celle de R10. Provisoire : voisins du pied de chute au niveau 1. Sonde : SF4 ; critère de succès : niveaux relevés sur l'anneau 1 autour du pied de chute égaux à 1 pour l'eau, à 2 pour la lave de l'Overworld (R9) ; sinon règle réécrite. | S |
| R15 | Étalement horizontal : possible si la cellule du dessous ne peut pas être remplie (bloc plein, ou source du même fluide) ; chaque voisin horizontal remplaçable ou lavable (R21a) reçoit un fluide de niveau (niveau - décrément) si ce niveau est valide (1 à 7) et meilleur que le niveau actuel de la cellule ; si les quatre voisins sont pleins ou sources, l'étalement s'arrête ; un voisin lavable perd son bloc (R21a). Provisoire : règle ci-dessus, niveau d'un voisin = min (niveau actuel du voisin, niveau de la cellule + décrément). Sonde : SF3, SF6 ; critère de succès : niveaux relevés égaux à (distance de taxi, borné par la portée) sur sol plat et sur un bassin à marche. | S |
| R16 | Recherche d'une chute (H-F3) : une cellule qui peut s'étaler regarde, pour chacune de ses directions horizontales ouvertes, s'il y a une cellule ouverte avec du vide dessous à 4 blocs ou moins (inclus ; eau, lave du Nether ; la page "Water" dit « four or fewer blocks ») ou à 2 blocs ou moins (autre lave, déduit du 3 de la page "Fluid" par le même décalage d'une unité) ; elle s'étale seulement dans la ou les directions de plus courte distance ; si aucune chute n'est trouvée dans le rayon, elle s'étale dans toutes les directions ouvertes. Les pages donnent 4 ou 5 pour l'eau, 3 pour l'autre lave. Provisoire : rayons 4 et 2, plus courtes distances égales toutes retenues. Sonde : SF5 ; critère de succès : trous à la distance d de 1 à 6 dans une direction : l'eau s'étale vers le trou si d <= le rayon relevé, dans toutes les directions sinon ; rayon relevé pour eau, lave de l'Overworld, lave du Nether. | S |
| R17 | Arrêt : un fluide qui n'a ni chute, ni voisin ouvert, ni niveau valide, ne change plus ; un fluide coulant sans fournisseur disparaît (R25). Provisoire : conséquence de R12, R15. Sonde : SF12 ; critère de succès : un bassin fermé plein de sources et un bassin dont toutes les cellules coulantes sont au niveau maximal ne produisent aucun changement de bloc ni tick de fluide en 100 ticks. | S |

### Sources infinies

| Réf | Règle | Preuve |
|---|---|---|
| R18 | Conversion en source de l'eau : une cellule capable de recevoir de l'eau (air, fluide coulant, bloc immergeable) devient une source si au moins deux de ses voisins horizontaux sont des sources d'eau et si la cellule du dessous est un bloc plein ou une source d'eau (page "Water" : « sur un bloc solide ou une autre source » ; page "Fluid" : « un bloc que le liquide ne peut pas entrer en dessous »). Si `water_source_conversion` est faux, aucune nouvelle source ne se forme. Provisoire : règle ci-dessus, vérifiée à chaque tick de fluide de la cellule. Sonde : SF6 (a à d) ; critère de succès : cellule au centre de deux sources opposées sur sol plein : devient source, avec gamerule vrai ; reste coulante, avec gamerule faux ; sur de l'air : reste coulante ; sur une source : devient source. | S |
| R19 | Conversion en source de la lave : même règle que R18 si `lava_source_conversion` est vrai, aucune nouvelle source sinon (page "Lava" pour la gamerule ; défaut faux : page "Fluid" et `server-game-loop.md` R40). Provisoire : règle de R18 pour la lave, gamerule lave. Sonde : SF6 (e, f) ; critère de succès : cellule au centre de deux sources de lave : source avec `lava_source_conversion` vrai, coulante avec faux (défaut). | S |
| R20 | Variante de la page "Water" : une cellule d'eau coulante qui a une seule source voisine et une source directement au-dessus devient source (hypothèse H-F4). Provisoire : non retenu comme règle. Sonde : SF6 (g) ; critère de succès : cellule coulante avec une source horizontale et une source au-dessus : état relevé (source ou coulant) consigné. | S |

### Remplacement et détruction

| Réf | Règle | Preuve |
|---|---|---|
| R21a | Classes de cellule face à un fluide qui coule, prédites par les étiquettes de R4 : (1) air -> rempli ; (2) dans `washed_away_by_fluids` -> détruit avec butin (jalon M6) ; (3) dans `replaceable` sans être lavable (`bubble_column`, `structure_void`) -> remplacé sans butin ; (4) dans `blocks_fluid_flow` (hors cas 2) -> bloquant ; (5) tout autre bloc -> bloquant. Provisoire : ces classes testées dans l'ordre 1 à 5, `washed_away_by_fluids` étant prioritaire sur `blocks_fluid_flow` pour `straw_bed`. Sonde : SF9 ; critère de succès : balayage des 1286 blocs : classe observée (traversé, détruit avec butin, bloquant) égale à la prédiction ci-dessus pour 100 % des blocs hors R21b, écarts listés. | S |
| R21b | Les 28 blocs à la fois remplaçables et lavables, le bloc `straw_bed` (lavable et bloquant) et les 5 blocs hors des quatre étiquettes (`end_gateway`, `end_portal`, `ladder`, `nether_portal`, `sugar_cane`) ont une classe que les étiquettes ne déterminent pas (ordre d'évaluation inconnu). Provisoire : les 28 sont détruits avec butin (R21a, cas 2), `straw_bed` est détruit, les 5 autres sont bloquants. Sonde : SF9 ; critère de succès : classe observée de ces 34 blocs consignée (butin présent ou non), règle réécrite avec l'observation. | S |
| R22 | La lave détruit les mêmes blocs que l'eau sans produire de butin (page "Lava"). Provisoire : mêmes classes que R21a, aucun butin pour la lave. Sonde : SF9 (variante lave) ; critère de succès : classes de blocs identiques à celles de l'eau, aucun objet apparu. | S |
| R23 | Étiquette `impermeable` (19 blocs : verres, barrière) : effet sur le fluide inconnu. Provisoire : aucun effet propre (ces blocs sont pleins et arrêtent le fluide comme tout bloc plein). Sonde : SF10 (c) ; critère de succès : comportement relevé des 19 blocs face à l'eau coulante, l'immersion d'un bloc voisin et l'éponge, comparé à celui de la pierre. | D (valeurs) / S (effet) |
| R24a | Un écoulement d'eau vers une cellule de bloc immergeable (`waterlogged` à faux) met `waterlogged` à vrai sur une face ouverte (page "Water" : « l'eau peut s'étendre sur les blocs immergeables, créant une source infinie »). La pose directe d'un fluide dans la cellule par seau est du jalon M6/M7. Provisoire : écoulement vers une face ouverte -> `waterlogged` vrai. Sonde : SF10 (a) ; critère de succès : dalle, escalier, trappe, clôture, échelle, panneau, chaîne, lanterne, barres de fer : pour chaque face d'entrée (4 côtés, haut), `waterlogged` obtenu après STEP(30) consigné et égal à la règle, écarts listés. | S |
| R24b | Un bloc immergé se comporte comme une source d'eau pour ses voisins : l'eau en sort vers une cellule d'air voisine. Provisoire : bloc immergé = source de niveau 0 pour R15. Sonde : SF10 (b) ; critère de succès : dalle immergée à côté d'air : l'eau apparaît dans la cellule d'air voisine, face par face. | S |
| R24c | L'escalier et la trappe immergés bloquent l'écoulement du côté de leur surface carrée (page "Fluid"). Provisoire : l'eau n'entre ni ne sort par la face carrée. Sonde : SF10 (a, b) ; critère de succès : escalier (4 orientations, 2 moitiés) et trappe (ouverte, fermée, 4 orientations) : faces d'entrée et de sortie de l'eau relevées et égales à la règle. | S |

### Mélange, retrait, dimensions

| Réf | Règle | Preuve |
|---|---|---|
| R25 | Retrait d'une source : les cellules de fluide coulant qui n'ont plus de fournisseur disparaissent jusqu'à ce qu'il ne reste que ce qui est alimenté par une autre source. Provisoire (non fidèle sur le détail temporel) : chaque cellule dépourvue d'alimentation disparaît à son tick de fluide suivant, du plus éloigné de la source au plus proche en un tick par cellule (hypothèse H-F6). Sonde : SF7 ; critère de succès : trace par pas de 1 tick de l'état de la nappe après retrait de la source (nombre de cellules par distance), consignée, et durée totale égale à la valeur de la règle retenue après ajustement. | S |
| R26a | Une source de lave touchée par de l'eau (par le haut ou un côté) devient de l'obsidienne (pages "Water", "Lava", "Fluid"). Provisoire : source de lave et eau voisine (côté ou haut) donnent `obsidian`. Sonde : SF8 (cas 1) ; critère de succès : bloc obtenu au point de rencontre égal à l'obsidienne pour l'eau par le côté et par le haut, dans les deux ordres de pose. | S |
| R26b | Une lave coulante touchée par de l'eau (pas par le bas) devient du pavé. Provisoire : lave coulante et eau voisine (côté ou haut) donnent `cobblestone`. Sonde : SF8 (cas 2) ; critère de succès : bloc obtenu égal au pavé pour l'eau par le côté et par le haut, dans les deux ordres de pose. | S |
| R26c | De la lave qui coule vers le bas sur de l'eau transforme l'eau en pierre. Provisoire : lave qui descend sur une cellule d'eau donne `stone`. Sonde : SF8 (cas 3) ; critère de succès : bloc obtenu égal à la pierre, dans les deux ordres de pose (eau d'abord, lave d'abord). | S |
| R26d | De la lave qui coule sur de la terre d'âme avec de la glace bleue voisine devient du basalte (page "Lava" ; la page "Fluid" dit « terre d'âme par le haut, glace bleue dans toutes les directions »). Provisoire : les deux conditions sont requises ensemble. Sonde : SF8 (cas 4 à 6) ; critère de succès : basalte obtenu avec terre d'âme et glace bleue, bloc inchangé avec l'une des deux seulement. | S |
| R27 | `fast_lava` : dans une dimension dont le type porte l'attribut `fast_lava` vrai, la lave se comporte comme celle du Nether (délai 10, décrément 1, portée 7). Provisoire : le comportement dépend de l'attribut et non du nom de la dimension. Sonde : SF11 (a) ; critère de succès : lave posée dans le Nether (type `the_nether`) : délai 10 et portée 7 relevés ; contrôle croisé dans l'Overworld : 30 et 3 ou 4. | D (valeur) / S (effet) |
| R28 | `water_evaporates` : l'attribut s'applique à la pose d'eau par un joueur (seau) et à la fonte de la glace (page "Ice", Nether) ; une eau posée par commande dans le Nether n'est pas filtrée. Provisoire : aucun effet sur la simulation des cellules d'eau posées par commande ; le reste est du domaine M6/M7. Sonde : SF11 (b) ; critère de succès : `setblock minecraft:water` dans le Nether : le bloc reste en place et s'étale, ou disparaît (observation consignée). | D (valeur) / S (effet) |
| R29 | Une eau ou une lave dont la cellule est dans un chunk qui ne tique pas ne s'écoule pas, et son tick reste en file (`sim-ticks-updates.md` R9). L'écoulement vers un chunk non chargé est interrompu à la bordure (hypothèse H-F5). Provisoire : fluide arrêté sur la bordure d'un chunk non chargé, replanifié à son chargement par les mises à jour de bordure. Sonde : SF13 ; critère de succès : nappe qui atteint la bordure d'un chunk déchargé : aucun bloc écrit dans le voisin ; au rechargement, l'écoulement reprend (ou observation consignée). | S |
| R30 | Opacité : l'eau diminue la lumière du ciel de 1 par bloc et n'ajoute rien à la lumière de bloc (`sim-light.md` R14) ; la lave émet 15 (`sim-light.md` R8). Provisoire : filtre du ciel de l'eau 1 pour tous ses niveaux, émission de la lave 15. Sonde : SL3 (eau, niveaux 0 à 15) et SL2 (lave) de `sim-light-vectors.md` ; critère de succès : `f_ciel` de l'eau égal à 1 pour les 16 niveaux et émission de la lave égale à 15 pour les 16 niveaux. | S |

## Entrées / sorties

- **Pas de fluide** : entrée = position d'une cellule de fluide et son voisinage, gamerules de conversion, attributs de dimension ; sortie = états de blocs modifiés et ticks de fluide planifiés.
- **Pose** : un `setblock` d'eau ou de lave planifie un tick (délai de R11) sauf en mode `strict` (`sim-ticks-updates.md` R28).
- Coordonnées négatives : aucune règle d'écoulement ne dépend du signe ; un fluide franchit la bordure de chunk en x et en z comme n'importe quelle autre limite (R29 pour le chunk non chargé).

## Cas limites

- Source au centre exact d'un plancher de 1 x 1 : chute vers le bas seulement.
- Source sur la dernière couche du monde (Y 319) et sur la première (Y -64) : rien en dessous ; hors du monde la chute est impossible (hypothèse : arrêt).
- Deux sources séparées d'une seule cellule au-dessus d'une cellule d'air : pas de conversion (R18).
- Gamerule `water_source_conversion` faux alors que des sources infinies existent déjà : les sources existantes subsistent.
- Lave rencontrant l'eau en plusieurs points dans le même pas : ordre des cas de R26a à R26d (une cellule de lave qui touche l'eau par le haut et par un côté).
- Niveau 8 posé à la main, niveaux 9 à 15 (R8).
- Fluide dans une cellule d'un bloc immergé entouré d'autres blocs immergés (chaîne de dalles).
- Fluide dans le Nether, dans l'End (lave lente ou rapide selon l'attribut) et en altitude (Y 300).

## Vecteurs de test

`sim-fluids-vectors.md` : SFV1 à SFV6 (données et calcul de nappe), sondes SF1 à SF13 ; annexe commune dans `sim-ticks-updates-vectors.md`.

## Plan de tests

| Niveau | Test | Règles |
|---|---|---|
| L1 | Nappe sur sol plat : cellules par distance égales à SFV6 (eau portée 7, lave 3 et 4, lave rapide 7) ; niveaux 1 à 7 ou 2, 4, 6 | R9, R10, R15 |
| L1 | Calendrier d'écoulement : une source au sol, ticks exécutés à k fois le délai (5, 30, 10) | R11 |
| L1 | Conversion en source : table de vérité (deux sources, sol plein ou non, gamerule vrai ou faux) | R18, R19 |
| L1 | Mélange : 4 cas x ordres de pose | R26a à R26d |
| L1 | Classification des blocs : prédiction de SFV7 contre le résultat du balayage de l'oracle | R21a, R21b, R22 |
| L2 | Sondes SF1 à SF13 puis comparaison des instantanés de région (blocs et `fluid_ticks`) | toutes |
| L3 | Coupe horizontale et verticale colorée par `level` (nous, référence, écart) pour SF3 et SF4 | R7, R9, R14 |
| L4 | Scénario : bassin à deux sources, vidange, chute en cascade sous `/tick step` | R12, R18, R25 |

Tests de niveau A en aveugle depuis cette spec.

## Questions ouvertes

- **Rayons de recherche de chute (H-F3), portée de la lave (H-F1), niveau au pied d'une chute (H-F2), variante à source au-dessus (H-F4).** Les trois pages publiques ne concordent pas ; SF3, SF4, SF5, SF6 tranchent.
- **Étiquette `blocks_fluid_flow` (R4, R21a).** Son nom suggère les blocs qui arrêtent l'écoulement ; le sens reste S et SF9 le confronte au comportement de chacun des 1286 blocs.
- **Tag `impermeable` (R23).** Sens inconnu ; si la sonde ne montre aucun effet, la règle devient D uniquement (données) et le tag est ignoré.
- **Retrait de source (R25).** Le détail temporel de la disparition n'est pas décrit ; la trace de SF7 est la référence.
- **Second lot hors périmètre.** Colonnes de bulles, algues et herbes marines (blocs qui contiennent de l'eau sans la propriété), éponge, corail, chaudrons, glace par gel et fonte (la glace est dans `sim-growth-redstone.md`), seaux et effets sur entités. Chacun demande une règle de fluide à part et une sonde.
- **Pose d'eau dans le Nether par commande (R28).** À observer.
- **Fluide et unload (H-F5).** Même remarque que `sim-ticks-updates.md` H-U2.

Règles S restantes (aucune sonde exécutée, EULA non acceptée) : S : R7 à R20, R22, R25, R29 et R30, R21a, R21b, R24a, R24b, R24c, R26a, R26b, R26c, R26d ; D/S : R4 et R5, R23, R27 et R28.

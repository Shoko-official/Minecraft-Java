# Dimensions : définitions, portails, voyage entre dimensions

Statut : brouillon
Jalon : M10
Risque : A
Règles : 62 (R1 à R62). D 7, D/S 11 (valeur lue, effet S), C 1, M 0, S 43.
Mise à jour : 2026-10-28 (première version)

Première des trois specs du jalon M10 (avec `dim-nether-generation.md` et `dim-end-generation.md`). Elle définit ce qu'est une dimension (type, hauteurs, échelle de coordonnées, drapeaux, attributs d'environnement), les trois portails (Nether, End, passerelle de l'End) et ce qui se passe quand une entité change de dimension. La génération du terrain de chaque dimension est dans les deux autres specs ; la génération de l'Overworld est dans `worldgen-*.md` et n'est pas répétée.

## Périmètre

Couvre :

- les fichiers `dimension_type` (4), leur liaison aux dimensions par les préréglages de monde, les champs, leurs valeurs, leur portée (hauteurs de génération et de construction, échelle de coordonnées, plafond, lumière du ciel, temps fixe, combat du dragon) ;
- les attributs d'environnement déclarés par les types de dimension (22 attributs distincts sur 51 enregistrés) pour la partie qui change une règle de jeu : lit, ancre de réapparition, boussole, eau, lave, feu, raids, golem de neige, cochons zombifiés ;
- le portail du Nether : cadre, allumage, bloc, délai, refroidissement, conversion 8:1, recherche du portail de destination, création, sortie ;
- le portail de l'End (cadre à 12 yeux, plateforme d'arrivée, retour) et la passerelle de l'End (bloc, destination liée, générée à la première utilisation) ;
- le transfert d'entités (état conservé, passagers, tickets de chargement) et les effets propres à une dimension (vide, bordure).

Ne couvre pas :

- la génération du terrain, des biomes, des structures et des features du Nether et de l'End (`dim-nether-generation.md`, `dim-end-generation.md`), ni la salle du portail dans la forteresse enfouie (générateur de stronghold, spec fille de `worldgen-features-structures.md` R82) ;
- l'IA, les attaques et la mort du dragon (spec d'entités, M7) ; seul l'état du monde laissé par le combat est dans la spec de l'End ;
- l'apparition naturelle des créatures et leurs limites de lumière (M7) : les valeurs de `monster_spawn_*` sont données (D), pas leur emploi ;
- le moteur de lumière (M5), les fluides (M5), le rendu du ciel, du brouillard et de la boussole (M9) ;
- les dimensions personnalisées au-delà de ce qu'il faut pour sonder (R20).

Vocabulaire : **source** et **cible** désignent la dimension de départ et d'arrivée d'un voyage ; **intérieur** d'un cadre de portail désigne la zone rectangulaire qu'il entoure ; **entité sans joueur** désigne toute entité hors joueur (objet au sol, créature, chariot, armor stand) ; **J** marque une sonde qui exige un joueur connecté (client vanilla), voir la section Montages et limites de l'oracle.

## Sources

Niveaux de preuve : **D** donnée exacte lue dans un fichier extrait ; **C** calcul exécuté sur des données extraites (scripts de l'annexe de `dim-definitions-portals-vectors.md`) ; **M** observé sur l'oracle (aucune règle aujourd'hui : oracle non exécutable, EULA non acceptée) ; **S** à vérifier. **S [Wn]** : décrite par une page publique non normative (résumée à la lecture, donc à prendre comme indice) ; elle reste S, sa sonde est prioritaire. Chaque règle S porte un comportement **provisoire**, étiqueté, non fidèle tant que la sonde n'a pas répondu, et sa sonde avec critère de succès.

Données extraites (jeu 26.3, data version 5023, `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3\`, noté `$REF`) :

- [E1] `resources/data/minecraft/dimension_type/*.json` (4 fichiers).
- [E2] `resources/data/minecraft/worldgen/world_preset/{normal,flat_all_dimensions}.json`.
- [E3] `resources/data/minecraft/tags/block/{nether_portal_frame,portals,infiniburn_*,dragon_immune}.json`, `tags/point_of_interest_type/*`, `tags/timeline/in_*.json`, `world_clock/*.json`.
- [E4] `generated/reports/registries.json` : registres `environment_attribute` (51), `ticket_type` (9), `point_of_interest_type` (21), `game_rule` (59), `block_entity_type`, `entity_type`, `trigger_type`.
- [E5] `generated/reports/blocks.json` : états de `nether_portal`, `end_portal`, `end_gateway`, `end_portal_frame`, `respawn_anchor`.
- [E6] `resources/data/minecraft/worldgen/{feature,placed_feature}/{end_platform,end_gateway_return,end_spike}.json`.
- [E7] `resources/assets/minecraft/lang/en_us.json` (messages de lit, de portail, de commande).

Documentation publique, consultée le 2026-10-28 (le contenu des pages a été lu par un résumé automatique : les valeurs sont des indices, jamais des faits) :

- [W1] https://minecraft.wiki/w/Nether_portal (cadre, délais, refroidissements, échelle, rayons de recherche, création, chargement de chunks). La page ne décrit pas clairement l'axe du bloc de portail.
- [W2] https://minecraft.wiki/w/Dimension (propriétés par dimension ; ne donne pas le transfert d'état ni les attributs détaillés).
- [W3] https://minecraft.wiki/w/End_portal (cadre, yeux, plateforme, retour, orientation).
- [W4] https://minecraft.wiki/w/End_gateway (anneau, destination liée, recherche de sortie, refroidissement).
- [W5] https://minecraft.wiki/w/The_End (plateforme (100, 49, 0), portail de sortie (0, 64, 0)).
- [W6] https://minecraft.wiki/w/Void (seuil et dégâts du vide par dimension).
- [W7] https://minecraft.wiki/w/Respawn_Anchor (dimension, explosion, charges, position de réapparition).
- [W8] https://minecraft.wiki/w/Nether (lit qui explose, boussole, eau, lave).
- [W9] https://minecraft.wiki/w/Ender_Dragon (apparition, portail de sortie, rite de réinvocation).

Relectures ciblées le 2026-10-29 : The_End ([W5], plateforme de R48), End_gateway ([W4], R54 et anneau), Void ([W6], R60).

Rapports locaux (`docs/reference/`) : non utilisés pour ces règles (ADR 0002, 2b : aucun constat de ces rapports n'est repris ici).

Relations avec les autres specs : états de chunk et dimensions `world-chunk-format.md` R1, R2 ; disposition `dimensions/<espace>/<chemin>/` `world-anvil-region.md` R1 ; règles de jeu `players_nether_portal_*` et `allow_entering_nether_using_portals`, horloges, `ender_dragon_fight.dat`, `chunk_tickets.dat` `server-game-loop.md` R30, R40, R51, R57 ; chargement des chunks et attente W `world-vectors.md` ; ancres de hauteur `worldgen-surface-carvers.md` R7, R76 ; tickets de chargement et statuts `worldgen-pipeline.md` R8 à R10.

Sondes : DP0 à DP14 (définitions, portails) et DV1 à DV21 (voyage), décrites dans [dim-definitions-portals-vectors.md](dim-definitions-portals-vectors.md) avec les montages, les vecteurs et le script de l'annexe.

## Règles

### A. Types de dimension (données)

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le dossier `dimension_type/` contient 4 fichiers : `overworld`, `overworld_caves`, `the_nether`, `the_end`. Le préréglage `normal` lie trois dimensions : `minecraft:overworld` (type `overworld`), `minecraft:the_nether` (type `the_nether`), `minecraft:the_end` (type `the_end`) ; `flat_all_dimensions` lie les mêmes trois clés aux mêmes types avec des générateurs `flat` (R19). `overworld_caves` n'est lié par aucun préréglage extrait. Commande : `dim_ref.py REF presets`. | D |
| R2 | Champs scalaires des 4 types : tableau DX1 sous cette section (commande `dim_ref.py REF dimtypes`, empreinte dans le fichier de vecteurs). | D |
| R3 | Tous les champs ci-dessus plus `attributes` sont les seules clés des 4 fichiers. Les champs absents d'un fichier sont facultatifs. Provisoire : un booléen absent vaut faux, `skybox` absent vaut l'ordinaire du jeu, `cardinal_light` absent la lumière par défaut, `default_clock` absent veut dire « pas d'horloge par défaut » (message `commands.time.no_default_clock` [E7], `server-game-loop.md`). Sonde : DP3 ; critère de succès : une copie de `overworld` sans `has_fixed_time` charge et laisse avancer le temps ; sans `min_y` elle est refusée avec le nom du champ. | D (valeurs) / S (défauts) |
| R4 | Dimensions en sections et en positions (C) : 24 sections (98 304 positions par chunk) pour l'Overworld et 16 sections (65 536 positions) pour le Nether et l'End, soit des Y de section -4 à 19 et 0 à 15 (`world-chunk-format.md` R2). Les tailles de grille de biomes en découlent : 1 536 cellules par chunk (Overworld), 1 024 (Nether, End). Commande : `dim_ref.py REF grids`, section DX18. | C |
| R5 | La plage de génération des réglages de bruit du Nether et de l'End est `min_y` 0, `height` 128 (Y 0 à 127), plus étroite que la plage de la dimension (Y 0 à 255). Les ancres `above_bottom` et `below_top` des règles de matière, des carvers et des placements se résolvent contre cette plage de **génération** (R6 de `dim-nether-generation.md`), pas contre la hauteur de la dimension. Provisoire : aucune génération n'écrit aux Y 128 à 255 de ces deux dimensions, qui sont de l'air dans un chunk neuf. Sonde : DP1 ; critère de succès : aucun bloc non air aux Y 128 à 255 dans 100 chunks neufs du Nether et de l'End normaux, hors structures. | D (valeurs) / S (effet) |
| R6 | Limites de construction : un bloc ne peut être posé qu'aux Y de `min_y` à `min_y + height - 1` (Overworld -64 à 319, Nether et End 0 à 255). Provisoire : `setblock` hors de cette plage échoue ; au-dessus de `logical_height` (128 dans le Nether) la pose reste permise. Sonde : DP1 ; critère de succès : réponse de `setblock` aux Y -1, 0, 127, 128, 255, 256 dans le Nether, identique à la prédiction. | S [W8] |
| R7 | `logical_height` (128 dans le Nether, partout ailleurs égale à `height`) borne les destinations de certains déplacements (positions générées pour un portail forcé, R39 ; téléportation aléatoire). Provisoire (H-PT4 : « hauteur du monde » de [W1] = `logical_height`) : une plateforme forcée dans le Nether a son Y borné à 128 - 10 = 118 ; aucun autre usage n'est supposé. Contre-hypothèse : la hauteur de la dimension (256), borne à 246. Sonde : DV8 ; critère de succès : Y maximal de l'obsidienne de plateforme sur 100 créations forcées demandées à des Y de 100 à 250 ; la règle est corrigée si la valeur n'est ni 118 ni 246. | S [W1] |

**DX1 (R2) : champs scalaires par type de dimension.**

| Champ | `overworld` | `overworld_caves` | `the_nether` | `the_end` |
|---|---|---|---|---|
| `min_y` | -64 | -64 | 0 | 0 |
| `height` | 384 | 384 | 256 | 256 |
| `logical_height` | 384 | 384 | 128 | 256 |
| `coordinate_scale` | 1.0 | 1.0 | 8.0 | 1.0 |
| `has_ceiling` | faux | vrai | vrai | faux |
| `has_skylight` | vrai | vrai | faux | vrai |
| `has_fixed_time` | absent | absent | vrai | vrai |
| `has_ender_dragon_fight` | faux | faux | faux | vrai |
| `ambient_light` | 0.0 | 0.0 | 0.1 | 0.25 |
| `default_clock` | `overworld` | `overworld` | absent | `the_end` |
| `infiniburn` (tag de blocs) | `infiniburn_overworld` | `infiniburn_overworld` | `infiniburn_nether` | `infiniburn_end` |
| `monster_spawn_block_light_limit` | 0 | 0 | 15 | 0 |
| `monster_spawn_light_level` | uniforme 0 à 7 | uniforme 0 à 7 | 7 | 15 |
| `skybox` | absent | absent | `none` | `end` |
| `cardinal_light` | absent | absent | `nether` | absent |
| `timelines` (tag) | `in_overworld` | `in_overworld` | `in_nether` | `in_end` |

### B. Coordonnées et commandes

| Réf | Règle | Preuve |
|---|---|---|
| R8 | `coordinate_scale` vaut 1.0 sauf pour le Nether (8.0). Il ne sert qu'au calcul de la position de destination d'un portail du Nether (R33). Les commandes ne l'appliquent pas : `execute in <dimension> run tp @s 800 70 800` place l'entité aux coordonnées écrites, sans division ni multiplication par 8 ; `/forceload`, `/locate`, `/place` et `/setblock` prennent des coordonnées de la dimension d'exécution. Provisoire : aucune mise à l'échelle par commande. Sonde : DP2 ; critère de succès : position finale égale à la position écrite, dans les deux sens. | D (valeur) / S (effet) |
| R9 | Une position non chargée dans une autre dimension fait échouer une commande avec le message `argument.pos.unloaded` (comme dans l'Overworld, `world-vectors.md`). Un `forceload` est propre à la dimension d'exécution. Provisoire : chaque dimension a ses propres chunks forcés (dans `dimensions/<espace>/<chemin>/data/minecraft/chunk_tickets.dat`, `server-game-loop.md` R51). Sonde : DP0 ; critère de succès : `forceload add 0 0` dans le Nether ne charge pas le chunk (0, 0) de l'Overworld. | S |
| R10 | La dimension d'une entité est lisible par la condition `execute if dimension <clé>`. Provisoire : les sélecteurs `@e` sans contrainte de position cherchent dans toutes les dimensions ; avec `distance` ou volume, dans la seule dimension d'exécution. Sonde : DP0 ; critère de succès : une entité marquée posée dans chacune des 3 dimensions est comptée 3 fois par `execute if entity @e[tag=T]` depuis l'Overworld, et 1 fois par `execute in <dimension> if entity @e[tag=T,distance=..1000000]`. | S |

### C. Attributs d'environnement et règles propres à une dimension

| Réf | Règle | Preuve |
|---|---|---|
| R11 | Les 4 types déclarent 22 attributs distincts sur les 51 du registre `environment_attribute`. Gameplay (commande `dimtypes`) : `bed_rule` et `straw_bed_rule` partout ; `respawn_anchor_works` partout (vrai dans le Nether seul) ; `nether_portal_spawns_piglin` vrai dans l'Overworld et `overworld_caves` seuls ; `can_start_raid` faux, `fast_lava` vrai, `piglins_zombify` faux, `snow_golem_melts` vrai, `water_evaporates` vrai, `sky_light_level` 4.0 dans le Nether seul. Le reste est audio ou visuel (`ambient_light_color`, brouillard, ciel, nuages, musique, sons d'ambiance, particule de stalactite). | D |
| R12 | `bed_rule` Overworld : `can_set_spawn` `always`, `can_sleep` `when_dark`, message `block.minecraft.bed.no_sleep` ; Nether et End : `can_set_spawn` `never`, `can_sleep` `never`, `destroy_on_use` vrai. `straw_bed_rule` Overworld : `can_set_spawn` `never`, `can_sleep` `when_dark`, `destroy_on_leave` vrai ; Nether et End : `never`, `never`, `destroy_on_use` vrai. Messages de lit disponibles : `no_sleep`, `not_safe`, `obstructed`, `occupied`, `too_far_away` [E7]. | D |
| R13 | Effet de `destroy_on_use` : utiliser un lit dans le Nether ou l'End le détruit et provoque une explosion de puissance 5 ([W8] ; la puissance 5 est annoncée comme supérieure à celle de la TNT). Le lit n'enregistre pas de point d'apparition. Provisoire : explosion de puissance 5 centrée sur le lit, lit détruit, point d'apparition inchangé. Sonde : DP11 (J) ; critère de succès : puissance mesurée par les blocs détruits dans un montage de blocs de résistance connue, égale à celle d'une explosion de puissance 5 déclenchée par commande. | S [W8] |
| R14 | Ancre de réapparition : `charges` 0 à 4 (5 états, [E5]) ; elle ne fonctionne que si l'attribut est vrai (Nether). Hors du Nether, utiliser une ancre chargée la fait exploser (puissance 5, feu) ([W7]). Une ancre vide ne peut pas fixer le point d'apparition. Provisoire : comme [W7] (lumière 3 pour une charge puis +4 par charge supplémentaire, une charge consommée par réapparition, aucune consommation pour un retour par le portail de l'End). Sondes : DP11 (J) et DV21 (J) ; critère de succès : charge consommée et position de réapparition égales à la prédiction sur 8 réapparitions. | S [W7] |
| R15 | Boussole. Aucune donnée extraite ne la lie à une dimension. Provisoire : la boussole pointe vers le point d'apparition du monde dans l'Overworld et tourne au hasard dans le Nether et l'End, sauf aimantée à un aimant de la même dimension ([W8], seule source). Sonde : captures du client (M9), 10 captures par dimension ; critère de succès : aiguille dirigée vers le point d'apparition dans l'Overworld, orientation variable dans les deux autres ; non vérifiable sans client. | S [W8] |
| R16 | `infiniburn` : le tag de blocs d'une dimension liste ce qui brûle sans fin. Valeurs : Overworld `infiniburn_overworld` (netherrack, bloc de magma) ; Nether `infiniburn_nether`, qui reprend ce tag ; End `infiniburn_end`, qui reprend ce tag et ajoute la bedrock. Provisoire : le feu au-dessus d'un bloc du tag ne s'éteint pas de lui-même. Sonde : DP12 ; critère de succès : après 6 000 ticks, le feu sur la bedrock subsiste dans l'End et disparaît dans le Nether. | D (valeurs) / S (effet) |
| R17 | `water_evaporates` vrai (Nether) : l'eau ne peut pas être posée par un seau, de la vapeur est produite ([W8]) ; l'eau posée par commande subsiste. `fast_lava` vrai : la lave s'écoule plus loin et plus vite ([W8], valeurs non fixées par la page). `snow_golem_melts`, `piglins_zombify` faux, `can_start_raid` faux : effets M7. Provisoire (fluides : M5) : eau par seau refusée, eau par commande acceptée, distance d'écoulement de la lave à fixer par DP12. Sonde : DP12 ; critère de succès : distance d'écoulement de la lave sur sol plat dans le Nether et dans l'Overworld, rapport mesuré. | S [W8] |
| R18 | Lumière. `has_skylight` faux (Nether) : le moteur ne calcule ni ne stocke de lumière du ciel. `ambient_light`, `ambient_light_color`, `sky_light_level`, `sky_light_factor` règlent la table de luminosité et l'éclairage du client (M9) ; elles n'ont pas d'effet serveur connu. Provisoire : les sections du Nether écrites n'ont pas de tableau `SkyLight` ; celles de l'End et de l'Overworld en ont un. Sonde : DP4 ; critère de succès : 0 section du Nether avec `SkyLight` sur 100 chunks ; sections de l'End avec `SkyLight` quand elles contiennent des blocs. | D (valeurs) / S (effet) |
| R19 | `has_ceiling` (Nether, `overworld_caves`) : valeur de donnée sans effet connu sur la génération (le plafond est dans la densité et la règle de matière). Provisoire : aucun effet serveur hors des règles qui le lisent explicitement ; DP5 liste les effets possibles. Sonde : DP5 (type `overworld` avec `has_ceiling` vrai en monde plat) ; critère de succès : pluie, point d'apparition du monde, position d'apparition d'un chunk neuf et lumière du ciel identiques à ceux du type sans plafond ; sinon chaque écart devient une règle. | D (valeur) / S (effet) |
| R20 | Une dimension personnalisée est un fichier de datapack `data/<espace>/dimension/<nom>.json` (`type`, `generator`) ([W2]) ; elle sert aux sondes pour isoler un champ de type (R3, R19) ou un générateur (plat, vide, biome fixe). Provisoire : un tel fichier est accepté, la dimension apparaît dans `/execute in` et dans `dimensions/<espace>/<nom>/`. Sonde : DP6 ; critère de succès : `execute in probe:x run forceload add 0 0` réussit et le dossier de dimension existe après `save-all flush`. | S [W2] |
| R21 | Les attributs audio `ambient_sounds` et `background_music` sont déclarés par le type de dimension pour l'Overworld, `overworld_caves` et l'End, et absents du type du Nether : ils y sont déclarés par chacun des 5 biomes (commande `biomes` de `dim_ref.py`, spec du Nether). | D |

### D. Portail du Nether

| Réf | Règle | Preuve |
|---|---|---|
| R22 | Données : tag `nether_portal_frame` = {`obsidian`} (l'obsidienne pleureuse n'en fait pas partie) ; bloc `nether_portal` avec propriété `axis` valeurs `x`, `z` (2 états) ; tag `portals` = {`nether_portal`, `end_portal`, `end_gateway`} ; type de point d'intérêt `nether_portal` ; types de ticket `portal`, `ender_pearl`, `dragon` ; règles `allow_entering_nether_using_portals` (booléenne), `players_nether_portal_default_delay` et `players_nether_portal_creative_delay` (entières, minimum 0) (D, `server-game-loop.md` R40). Leurs valeurs par défaut (vrai, 80 et 0) ne sont pas dans les données : S, documentation publique (`server-game-loop.md` R41). Provisoire : défauts vrai, 80 et 0. Sonde : PG7 de `server-game-loop.md` ; critère de succès : `gamerule` renvoie ces trois valeurs sur un monde neuf. Commande : `dim_ref.py REF portal`. | D (types, tags, états, minima) / S (défauts) |
| R23 | Cadre. Un cadre est un rectangle vertical d'obsidienne ; l'intérieur fait de 2 x 3 à 21 x 21 blocs (cadre de 4 x 5 à 23 x 23) ; les angles sont facultatifs ([W1]). Les portails créés par le jeu ont toujours les angles. Provisoire : un intérieur de largeur w (2 à 21) et de hauteur h (3 à 21) entouré d'obsidienne sur ses 4 côtés (angles ignorés) est un cadre valide ; w = 1, h = 2 ou plus de 21 ne l'est pas. Sonde : DP7 ; critère de succès : la table des (w, h) allumés pour w de 1 à 23, h de 2 à 23, avec et sans angles, égale à la prédiction. | S [W1] |
| R24 | Allumage. Un bloc de feu placé à l'intérieur d'un cadre valide remplit tout l'intérieur de blocs `nether_portal` ; le feu peut être placé sur n'importe quelle case de l'intérieur ([W1], depuis 1.7.2) ; le feu doit être posé en dernier, un cadre complété après le feu ne s'allume pas. Origines du feu : briquet, boule de feu, foudre, propagation. Provisoire : l'allumage se produit à la pose du feu seulement ; l'intérieur doit être de l'air ou du feu. Sonde : DP8 ; critère de succès : feu sur la ligne du bas, sur la ligne du haut, au milieu, via `setblock`, via briquet par un joueur (J), via foudre : issue identique à la prédiction pour 6 cadres 4 x 5. | S [W1] |
| R25 | Axe. Le bloc de portail a l'axe `x` quand le cadre s'étend selon X, `z` quand il s'étend selon Z ([W1] ne le dit pas clairement). Provisoire : `axis` = la direction horizontale dans laquelle la largeur du cadre est mesurée. Sonde : DP7 ; critère de succès : `axis` de tous les blocs de portail égal à l'axe du cadre pour les deux orientations. | S |
| R26 | Intégrité. Si un bloc du cadre ou un bloc de portail est enlevé, les blocs de portail qui ne forment plus un intérieur valide disparaissent à la mise à jour de bloc suivante. Provisoire : disparition de toute la colonne ou de l'intérieur entier dès qu'une case du cadre manque. Sonde : DP9 ; critère de succès : état de tous les blocs de portail après retrait d'un bloc de cadre en 4 positions (coin, côté, haut, bas) égal à la prédiction. | S |
| R27 | Délais d'un joueur : 80 ticks de présence continue dans un portail en survie, 0 en créatif (défauts des gamerules, S, R22 ; [W1] annonce 1 tick en créatif) ; quitter le portail avant la fin annule ([W1]). Provisoire : décompte de présence remis à zéro quand le joueur sort de tous les blocs de portail. Sonde : DV21 (J, étape a) ; critère de succès : tick de changement de dimension égal à `players_nether_portal_default_delay` pour les valeurs 0, 1, 80, 200. | S [W1] |
| R28 | Délais d'une entité sans joueur : changement de dimension à son premier tick dans le bloc, puis refroidissement de 300 ticks (15 s) pour les créatures et objets, 10 ticks pour chariots et bateaux, 2 ticks pour les projectiles ([W1], depuis 1.21.2). Le champ `PortalCooldown` de l'entité compte ce refroidissement. Provisoire : valeurs ci-dessus, champ lisible par `data get entity @e[...] PortalCooldown`. Sonde : DV3 ; critère de succès : `PortalCooldown` juste après le transfert pour un objet, une vache, un chariot, une flèche (valeurs entières, ±1 tick). | S [W1] |
| R29 | Entités exclues : le dragon et le wither ne peuvent pas utiliser un portail ; une entité liée à un cœur de creaking non plus ([W1], [W3]). Aucun tag d'entité extrait ne porte cette exclusion. Provisoire : ces trois types sont exclus, tous les autres passent. Sonde : DV4 ; critère de succès : transfert ou refus observé pour 40 types d'entité vivants et non vivants (liste dans la sonde). | S [W1] |
| R30 | `allow_entering_nether_using_portals` faux interdit l'entrée dans le Nether (et la création de portail) : l'entité reste dans le portail sans changer de dimension. Provisoire : transfert refusé Overworld -> Nether ; Nether -> Overworld autorisé. Sonde : DV5 ; critère de succès : issue des deux sens avec la règle à faux. | S |
| R31 | Apparition de cochons zombifiés : un bloc de portail de l'Overworld (attribut `nether_portal_spawns_piglin` vrai) peut faire apparaître un cochon zombifié sur sa base à un tick aléatoire, plus souvent selon la difficulté (x2 normal, x3 difficile par rapport à facile) ([W1]) ; aucun autre monstre. Provisoire : M7 ; ici seulement la condition (attribut vrai, absent du Nether et de l'End). Sonde : DP13 ; critère de succès : fréquence d'apparition par portail et par 100 000 ticks aux trois difficultés sous `random_tick_speed` 3. | D (condition) / S (taux) |
| R32 | Un portail actif repousse les hoglins (tag `hoglin_repellents`, effet M7). Provisoire : un hoglin ne s'approche pas d'un bloc de portail actif. Sonde : DP13 (variante : hoglin avec IA, placé à 6 blocs d'un portail actif, déplacement relevé sur 200 ticks) ; critère de succès : distance au portail non décroissante avec portail, décroissante sans. | S [W1] |

### E. Conversion, recherche, création

| Réf | Règle | Preuve |
|---|---|---|
| R33 | La position de recherche du portail de destination se calcule à partir de la position de l'entité : X et Z multipliés par le rapport des `coordinate_scale` source / cible (1/8 de l'Overworld vers le Nether, 8 du Nether vers l'Overworld), Y inchangé, puis bornés par la bordure du monde de la cible ([W1]). Rapport = 1 pour l'Overworld <-> End (non utilisé, R50). Provisoire : multiplication par le rapport, Y inchangé, bornage par la bordure de la cible, sans autre transformation. Sondes : DV1 (positions) et DV20 (bordure) ; critère de succès : voir R34 et R61. | S [W1] |
| R34 | Arrondi. Le Nether reçoit l'arrondi inférieur (plancher) de X/8 et Z/8 ([W1] décrit un arrondi inférieur de X/8 et de Z/8). Pour les coordonnées négatives la division est donc vers le bas : -1 donne -1, -8 donne -1, -9 donne -2. Provisoire : H-PT1 (plancher de l'entier de bloc ; le plancher de la position flottante donne le même résultat sauf pour l'avance sous-bloc). Vecteur C sous H-PT1 : table DX4 de `dim-definitions-portals-vectors.md`. Sonde : DV1 ; critère de succès : position de la plateforme créée dans le Nether égale à la prédiction H-PT1 pour 22 positions sources autour de 0 (de -9 à 16 en X et en Z). | S [W1] |
| R35 | Rayon de recherche (Java) : le portail est cherché dans un carré horizontal de 16 blocs de rayon autour du point quand la cible est le Nether, de 128 blocs quand la cible est l'Overworld, sur toute la hauteur ([W1]). Provisoire : « carré » = distance de Chebyshev ≤ rayon ; bornes incluses. Sonde : DV2 (portail pré-construit à distance d du point) ; critère de succès : pour d de rayon - 2 à rayon + 2 sur X, sur Z et diagonale, lien ou création identiques à la prédiction (borne incluse ou exclue relevée). | S [W1] |
| R36 | Choix du portail : le plus proche par distance 3D (Y compris) parmi les portails du rayon ([W1]) ; égalité inconnue. Provisoire : distance euclidienne au carré, égalité au premier trouvé dans l'ordre X puis Y puis Z croissants (H-PT2, sans source). Sonde : DV6 ; critère de succès : choix observé sur 20 couples de portails équidistants, ordre d'égalité identique à H-PT2 ou H-PT2 remplacée par l'ordre relevé. | S [W1] |
| R37 | Un portail de destination est un portail dont les blocs `nether_portal` sont connus du jeu (type de point d'intérêt `nether_portal`, tag `portals`) ; un portail posé par `setblock` sans cadre est trouvé s'il est enregistré. Provisoire : la recherche porte sur les blocs de portail présents (équivalents au registre de points d'intérêt). Sonde : DV7 ; critère de succès : un `nether_portal` posé par `setblock` est ou n'est pas pris pour destination, selon l'observation. | S |
| R38 | Un portail du Nether de plus de 18 blocs de large peut mener à deux portails distincts dans l'Overworld ([W1]) : la position de recherche est celle de l'entité, pas celle du centre du portail. Provisoire : la recherche part de la position de l'entité au moment du transfert. Sonde : DV6 (variante portail 21 x 5) ; critère de succès : destinations différentes selon l'extrémité d'entrée. | S [W1] |
| R39 | Création si aucun portail n'est trouvé ([W1]) : (a) chercher dans les 16 blocs horizontaux de la position une zone de 3 x 4 blocs constructibles avec 4 blocs d'air au-dessus, sans limite verticale ; (b) sinon une zone de 1 x 4 avec 4 d'air ; (c) sinon forcer : Y entre 70 et la hauteur du monde moins 10, plateforme d'obsidienne de 2 x 3 avec 3 d'air au-dessus, en écrasant les blocs ; si la plateforme repose sur de la bedrock, du netherrack est ajouté de part et d'autre. Provisoire : exactement ces trois étapes dans cet ordre ; le portail créé est un cadre 4 x 5 avec angles, de 14 blocs d'obsidienne, intérieur 2 x 3. Sonde : DV8 (zone libre, zone étroite, vide : montages FLATALL, VOID et terrain creusé par `fill`) ; critère de succès : liste des blocs écrits (nom, position relative) dans un volume de 7 x 9 x 7 autour du point, identique à la prédiction pour 3 terrains. | S [W1] |
| R40 | Orientation du portail créé : l'axe du nouveau portail est celui du portail source ([W1] : le portail créé reprend l'axe le plus long du portail source). Provisoire : le nouveau cadre garde l'axe du portail d'entrée ; sans portail d'entrée (entité traversant un `nether_portal` isolé) : axe de ce bloc. Sonde : DV8 ; critère de succès : axe du portail créé égal à l'axe du portail d'entrée pour les deux orientations. | S [W1] |
| R41 | Sortie : l'entité est placée dans l'intérieur du portail de destination, sur la ligne du bas, centrée, avec sa direction et sa vitesse transformées. Provisoire : position (x + 0,5, y du bas de l'intérieur, z + 0,5) du milieu de l'intérieur ; rotation relative à l'axe ; vitesse conservée à la rotation près. Sonde : DV9 ; critère de succès : `Pos`, `Rotation` et `Motion` d'un objet avant et après, pour 4 directions d'entrée. | S |
| R42 | Chargement. Un transfert crée un ticket de chargement de type `portal` sur la zone de destination et ses voisins pour une durée de 15 secondes (300 ticks) renouvelée par chaque nouveau passage ([W1], Java). Provisoire : ticket `portal`, durée 300 ticks, rayon 3 chunks (H-PT6, sans source ; la page parle du chunk lié et de ses voisins). Sonde : DV10 ; critère de succès : entrée `portal` de `chunk_tickets.dat` de la dimension cible juste après un transfert, rayon et durée relevés. | D (type) / S (durée, rayon) |
| R43 | Passagers. Une entité qui porte des passagers peut utiliser un portail et emmène ses passagers ; le total n'a pas de refroidissement ([W1], depuis 1.21). Provisoire : le véhicule et tous ses passagers changent ensemble de dimension, UUID conservés. Sonde : DV11 ; critère de succès : chariot + cochon + poulet : les trois arrivent dans la cible, relation de monture conservée. | S [W1] |
| R44 | Les chunks de destination non générés sont générés à la demande pour la recherche et la création. Provisoire : le transfert attend la génération au statut `full` de la zone de recherche (rayon 16 vers le Nether, 128 vers l'Overworld, `worldgen-pipeline.md` R8 à R10). Sonde : DV1 (variante : comptage de chunks générés avant / après) ; critère de succès : ensemble des chunks nouveaux écrits sur disque égal à la prédiction pour la zone de recherche, rayon de ticket inclus. | S |

### F. Portail de l'End et retour

| Réf | Règle | Preuve |
|---|---|---|
| R45 | Données : bloc `end_portal_frame` avec `eye` (vrai, faux) et `facing` (north, south, west, east) : 8 états ; bloc `end_portal` (1 état) et type de bloc-entité `end_portal` ; entité `eye_of_ender` ; déclencheur `used_ender_eye` ; la structure `stronghold` porte le portail (tag `eye_of_ender_located`). | D |
| R46 | Cadre : 12 cadres autour d'une zone centrale de 3 x 3, 3 cadres par côté, chacun tourné vers l'intérieur ; l'activation exige les 12 yeux ; le centre se remplit alors de blocs `end_portal`, en détruisant ce qui s'y trouve (sol, fluides, bedrock) sans son ni objet ([W3]). Un cadre valide peut être tourné et placé debout ([W3] : 24 orientations, zone de détection de 5 x 5 x 5). Provisoire : anneau horizontal à 12 cadres seulement ; le portail se remplit dès que le 12e œil est posé par un joueur. Sonde : DP10 (J) ; critère de succès : pour 24 orientations du cadre, remplissage ou non, identique à la prédiction. | S [W3] |
| R47 | Les cadres de la forteresse enfouie ont chacun 10 % de chance de porter déjà un œil ([W3]). Provisoire : relève de la spec fille du stronghold ; ici seulement la distribution du nombre d'yeux : binomiale (12, 0,1). Sonde : DP14 (200 salles de portail posées par `/place structure minecraft:stronghold`, comptage des yeux) ; critère de succès : fréquence de chaque nombre d'yeux, écart au binomial inférieur à 3 écarts types. | S [W3] |
| R48 | Entrée dans l'End par un `end_portal` de l'Overworld ou du Nether : l'entité est placée à (100, 49, 0) (joueur : tourné vers l'ouest) sur une plateforme d'obsidienne de 5 x 5 à Y 48 centrée en (100, 0) ([W3]), surmontée de trois couches d'air (Y 49 à 51 ; volume de 5 x 5 x 4 plateforme comprise) ([W5]). [W5] ajoute que la plateforme est régénérée à chaque entrée (joueur, ou joueur et entité selon le passage de la page) et restaurée si l'obsidienne est détruite ou si un bloc occupe les trois couches d'air ; [W3] ne mentionne ni nettoyage de couches ni régénération. La même plateforme existe à la génération (placed feature `end_platform`, position fixe (100, 49, 0), [E6]). Provisoire (H-PT5 : régénération à chaque entrée de toute entité) : toute entité qui entre est placée sur la plateforme ; à chaque entrée les 25 blocs de Y 48 sont de l'obsidienne et les 75 blocs de Y 49 à 51 de l'air, les blocs remplacés étant lâchés comme objets (à relever). Sondes : DV12 et DV13 ; critère de succès : `Pos` de l'entité après le transfert ; état du volume 5 x 5 x 4 avant (pierre posée) et après, pour une première entrée, pour une seconde entrée et après destruction de l'obsidienne ; objets lâchés. | D (position de la feature) / S |
| R49 | Retour : un `end_portal` traversé dans l'End (portail de sortie en (0, ~64, 0) ou bloc posé) envoie l'entité vers l'Overworld : joueur au point d'apparition (lit ou ancre, sinon point d'apparition du monde), autre entité au point d'apparition du monde ([W3], [W5]). Un joueur qui revient pour la première fois voit les crédits (champ `seenCredits` des données de joueur, `server-game-loop.md` R57). Provisoire : joueur : point de réapparition de ses données ; autre entité : `spawn` de `level.dat` (`server-game-loop.md` R52) dans la dimension qu'il nomme. Sondes : DV14 (entité sans joueur), DV21 (J, étape c) ; critère de succès : position et dimension d'arrivée de 3 entités, dimension du `spawn` modifiée par datapack. | S [W3] |
| R50 | Un `end_portal` traversé depuis le Nether mène à l'End comme depuis l'Overworld ; un portail du Nether ne s'allume pas dans l'End ([W1], [W3]). Aucune échelle de coordonnées ne s'applique. Provisoire : même destination (100, 49, 0). Sonde : DV12 (variante Nether) ; critère de succès : `Pos` à l'arrivée égale à celle d'une entrée depuis l'Overworld. | S [W1] |

### G. Passerelle de l'End

| Réf | Règle | Preuve |
|---|---|---|
| R51 | Données : bloc `end_gateway` (1 état), type de bloc-entité `end_gateway`, placed feature `end_gateway_return` (biome `end_highlands`, 1 chance sur 700 par chunk) dont la feature a `exact` vrai et `exit` (100, 50, 0) ([E6]). Les 20 passerelles de l'anneau central sont créées par le combat (spec de l'End, R30). | D |
| R52 | Un bloc-entité de passerelle porte une destination (`exit_portal`), un drapeau de téléportation exacte (`exact_teleport`) et un âge (`Age`, ticks depuis la création) ([W4] : noms de champs publics ; non vérifiés dans les données de 26.3). Provisoire : noms `exit_portal` (position), `ExactTeleport` (booléen), `Age` (entier) ; relus par `data get block <x> <y> <z>`. Sonde : DV15 ; critère de succès : clés relevées sur une passerelle de l'anneau et sur une passerelle naturelle. | S [W4] |
| R53 | Refroidissement : 40 ticks entre deux usages d'une même passerelle par une entité ([W4], porté de 20 à 40 en 1.11). Provisoire : 40 ticks. Sonde : DV15 ; critère de succès : nombre de passages d'un objet entrant à chaque tick pendant 200 ticks. | S [W4] |
| R54 | Passerelle sans destination liée (celles de l'anneau) : au premier passage, le jeu choisit un point extérieur sur la demi-droite qui part de (0, 0) et passe par le coin nord-ouest de la passerelle, à 1024 blocs par défaut ; si le chunk d'arrivée contient des blocs au-dessus de Y 15, la distance diminue par pas de 16 jusqu'à 768, sinon elle augmente par pas de 16 jusqu'à 1280 ; le chunk est cherché pour de la pierre d'End à Y 30 ou plus surmontée de deux blocs non pleins (sans boîte de collision pleine ; la plante de chorus compte, pas sa fleur) ; si elle existe, le coin sud-est du chunk est retenu, sinon une petite île est générée à Y 75 au bout de la ligne ; la sortie est le bloc plein le plus haut dans ±16 blocs en X et Z, le nord et l'ouest l'emportant en cas d'égalité, et la passerelle de sortie est construite 10 blocs au-dessus ([W4]). Une fois liée, la destination est définitive. Provisoire : exactement ces valeurs, hypothèse H-PT3 (une page publique, aucun recoupement ; H-PT3b : « deux blocs non pleins au-dessus » = les deux blocs directement au-dessus, la page ne le précise pas). Sonde : DV16 ; critère de succès : `exit_portal` relevé pour les 20 passerelles de l'anneau d'une graine donnée, distance au centre dans [768, 1280] et multiple de 16 près de 1024 ; écart de position entre prédiction et observation. | S [W4] |
| R55 | Position d'arrivée d'une entité qui traverse une passerelle (non exacte) : recherche d'un emplacement libre dans une zone de 11 x 11 en X et Z autour de la destination, depuis Y 255 vers le bas, en excluant les blocs en contact (y compris diagonale) avec la passerelle ; sans résultat, 2 blocs au-dessus de la passerelle ([W4]). Exacte (`ExactTeleport` vrai) : position écrite. Provisoire : règle de [W4]. Sonde : DV17 ; critère de succès : `Pos` d'un objet traversant, avec et sans `ExactTeleport`, pour 10 destinations ; égal à la prédiction ou règle remplacée par l'observation. | S [W4] |
| R56 | Les passerelles naturelles des hautes terres (R51) sont exactes et renvoient à la plateforme, sans la régénérer ([W4]). Provisoire : arrivée à (100, 50, 0) ; plateforme inchangée si intacte. Sonde : DV17 (variante `place feature minecraft:end_gateway_return`) ; critère de succès : `Pos` et volume de la plateforme avant / après. | D (exit) / S [W4] |

### H. État conservé, vide, bordure

| Réf | Règle | Preuve |
|---|---|---|
| R57 | Un transfert conserve l'identité de l'entité (UUID) et ses données (nom, étiquettes, santé, effets, équipement, inventaire d'une créature, âge, état de dressage) à l'exception de la position, de la dimension, du refroidissement de portail ; la chute (`FallDistance`) est remise à zéro. Provisoire : toutes les clés sauf `Pos`, `Dimension`, `PortalCooldown`, `FallDistance`. Sonde : DV18 ; critère de succès : différence de l'arbre NBT d'une vache, d'un objet, d'un armor stand porteur d'étiquettes avant / après (ordre des clés ignoré) : seules les clés prédites diffèrent. | S |
| R58 | État d'un joueur conservé : inventaire, expérience, santé, faim, effets, mode, points d'apparition, avancements ; champ `Dimension` mis à jour ; `seenCredits` modifié au premier retour de l'End. Provisoire : tout est conservé sauf `Pos`, `Dimension`, `FallDistance`, `PortalCooldown`. Sonde : DV21 (J, étape b) ; critère de succès : différence des données de joueur (`players/data/<uuid>.dat`) avant / après. | S |
| R59 | Entités non conservées : elles meurent ou restent derrière quand le transfert n'a pas lieu (liens de laisse entre dimensions rompus, monture d'une autre dimension refusée : message `commands.ride.mount.failure.wrong_dimension` [E7]). Provisoire : une laisse, un lien de dressage et un lien de monture s'arrêtent à la frontière de dimension. Sonde : DV19 ; critère de succès : comportement observé pour laisse, monture, chariot lié. | S |
| R60 | Vide. Une entité qui descend sous `min_y - 64` subit 4 points de dégâts toutes les 10 ticks, sans réduction par l'armure ni la résistance : seuils Overworld -128, Nether et End -64 ([W6]). Provisoire : seuil `min_y - 64` selon la dimension ; dégâts 4 par 10 ticks ; les entités non vivantes (objets) disparaissent 64 blocs plus bas dans le vide ([W6]) : H-PT7a, au même seuil `min_y - 64` ; H-PT7b, à `min_y - 128` (la page ne dit pas à partir de quel Y se compte la distance). Sonde : DV20 ; critère de succès : santé d'une vache posée à Y -63, -64, -65, -127, -128, -129 dans chaque dimension, 100 ticks d'observation ; Y de disparition d'un objet posé de 10 en 10 blocs sous le seuil (tranche H-PT7a et H-PT7b). | S [W6] |
| R61 | Bordure du monde. Chaque dimension a son fichier `world_border.dat` (`server-game-loop.md` R51). Provisoire : la bordure de la cible borne la position de destination (R33) ; `worldborder set` agit sur la dimension d'exécution. Sonde : DV20 ; critère de succès : valeur de `worldborder get` dans les trois dimensions après un `set` dans une seule. | S |
| R62 | Un joueur du Nether ou de l'End qui tombe dans le vide meurt comme dans l'Overworld ; il ne change pas de dimension. Provisoire : mort sur place. Sonde : DV21 (J, étape d) ; critère de succès : mort sur place, sans changement de dimension. | S [W6] |

### Montages et limites de l'oracle (hors règles)

Les montages communs (FLATALL, VOID, PORTAL, OBS-DIM) sont décrits dans le fichier de vecteurs. Ils ne portent pas de règle : ils servent à rendre les sondes reproductibles. Les sondes marquées **J** exigent un client vanilla connecté à l'oracle par le propriétaire (acte personnel, comme l'EULA) : sans lui, les règles à joueur (R13, R14, R24 en partie, R27, R46, R49 en partie, R58, R62) restent S avec le comportement des pages publiques. Les autres règles se sondent avec des entités sans joueur, par commandes et par lecture des fichiers de région.

## Entrées / sorties

- **Entrée** : dimension source, position, entité (type, données), état de la cible (chunks générés, portails présents), règles de jeu (R22), difficulté.
- **Sortie** : dimension cible, position et rotation, portail de destination (trouvé ou créé, liste de blocs écrits), tickets de chargement, données d'entité.
- **Coordonnées négatives** : plancher (R34) ; les rayons de recherche (R35) s'entendent en blocs autour de l'entier obtenu ; une position hors de la bordure est ramenée sur la bordure avant la recherche.

## Cas limites

- Position source dans une zone non générée de la cible (création de portail dans un chunk neuf) : génération à la demande (R44).
- Portail adjacent au bord d'un chunk : intérieur sur deux chunks, blocs de portail dans les deux (R26).
- Deux portails partageant un cadre d'obsidienne ([W1]) : disparition de l'un n'enlève pas l'autre (R26).
- Y hors plage : Y sous la plage de génération de la cible (Y négatif source Overworld -64 vers Nether 0) : Y ramené dans la plage ; au-dessus : voir R7 (DV7).
- Entité qui entre dans un portail pendant son refroidissement : aucun transfert, le compte reprend à zéro (R28).
- Joueur qui ouvre un portail vers un chunk qui échoue à se générer : le transfert est refusé avec le nom de la ressource (pipeline R44).
- Seeds extrêmes (`Long.MIN_VALUE`, `Long.MAX_VALUE`, -1, 0) : tous les tests de génération de portail les emploient.

## Plan de tests

- **L0** : empreintes de R2, R11, R12, R22, R45 par `dim_ref.py` contre `$REF`.
- **L1** (JUnit) : chargement des 4 types ; liaison par préréglage ; table de conversion DX4 sous H-PT1 ; parseur du cadre (R23, R25) sur la table des (w, h) de DX3 ; parseur du cadre de l'End (R46) ; probabilités de DX5 ; refus de champs obligatoires manquants.
- **L2** : DP7, DP8, DP9 (cadres) ; DV1 à DV11 (liaison) ; DV12 à DV17 (End) ; DV18 (état) ; comparaison des blocs écrits à la création (R39) ; états canoniques de `worldgen-pipeline.md` R34.
- **L3** : néant (pas de sortie visible avant M9).
- **L4** : scénarios scriptés `tools/diff/scenarios/` : aller-retour Overworld -> Nether -> Overworld d'un objet et d'une vache (portail construit par `fill` et `setblock`), aller-retour End.

## Vecteurs de test

Voir [dim-definitions-portals-vectors.md](dim-definitions-portals-vectors.md) : DX1 (types de dimension, empreinte), DX2 (états des blocs de portail), DX3 (cadre : table des tailles valides sous R23), DX4 (conversion 8:1 sous H-PT1), DX5 (probabilités de la distribution d'yeux), DX6 (longueurs de destination de passerelle), DX18 (sections et positions par dimension), commandes et empreintes SHA-256 des sorties de `dim_ref.py`. Les vecteurs de position de création (R39) et de sortie (R41, R55) viennent des sondes DV ; aucune valeur n'est calculable sans oracle.

## Questions ouvertes

- **Q1 (R34, R36)** : arrondi exact de la position de recherche (plancher entier ou flottant) et règle d'égalité : S ; une erreur déplace le portail créé d'un bloc ou lie à un autre portail. Levée par DV1 et DV5.
- **Q2 (R35, R39, R42)** : rayons de recherche, de création et de ticket : valeurs du wiki, non recoupées. DV2, DV7, DV9.
- **Q3 (R54, R55)** : algorithme de la destination de passerelle : une seule page publique ; les valeurs 1024, 768, 1280, 15, 30, 16, 10 sont ses chiffres. DV13, DV14.
- **Q4 (R13, R14, R27, R46, R49, R58, R62, R24)** : règles à joueur. **Bloquant pour figer** : un client vanilla relié à l'oracle (acte du propriétaire). Mitigation : entités sans joueur pour R28 à R61.
- **Q5 (R15)** : boussole : rendu seul, hors serveur ; M9.
- **Q6 (R5, R6, R7)** : plages de hauteur et ancres : levées par DP1 et DV7 ; une ancre résolue contre la hauteur de la dimension donnerait des résultats très différents (Y jusqu'à 245).
- **Q7 (R20)** : dimensions personnalisées : seule la forme minimale est requise ici ; le chargeur doit accepter toute dimension déclarée par datapack (`data-datapack-loading.md`).
- **Aucun blocage 2a-bis** dans cette spec : le comportement des portails est observable par commandes et fichiers de région ; seule la boussole est client.

## Proposition de mise à jour de la matrice (appliquée par l'orchestrateur)

Lignes à ajouter ou à passer de `non-spécifié` à `spécifié` : Types de dimension (D/S, M10, A), Portail du Nether (S, M10, A), Portail de l'End et retour (S, M10, A), Passerelle de l'End (S, M10, A), Transfert d'entités entre dimensions (S, M10, A), Lit / ancre / boussole par dimension (D/S, M10, B), Vide et bordure par dimension (S, M10, B). Les lignes « Nether » et « End » restent `non-spécifié` jusqu'aux specs de génération.

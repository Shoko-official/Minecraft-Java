# Tables de butin, contexte, conditions, modificateurs et prédicats

Statut : brouillon
Jalon : M6
Risque : A
Règles : 73 (R1 à R73)

## Périmètre

Couvre, pour `engine/loot` (ADR 0004) :

- le format des 1447 tables de butin de la version cible : table, pools, entrées (9 types enregistrés), conditions (20 types), modificateurs (42 types), fournisseurs de nombres entiers et flottants contextuels, prédicats nommés (3 fichiers) ;
- le **modèle de contexte** : paramètres, jeux de paramètres (31 `context_key_set`), ports de lecture et d'action déclarés par `loot` (ADR 0004) ;
- l'algorithme de tirage : conditions de pool, nombre de tirages, développement des entrées, poids et qualité avec la chance, application ordonnée des modificateurs, et l'ordre de consommation de l'aléa ;
- le choix de la source d'aléa (graine explicite, séquence nommée, source du niveau) et la persistance des séquences nommées ;
- les formes de prédicats utilisées par les conditions (objet, entité, bloc, position, source de dégâts) ;
- le déballage de butin des conteneurs (`LootTable`, `LootTableSeed`) et les commandes `loot` et `item modify` en tant que points d'entrée de l'oracle.

Ne couvre pas :

- les moteurs de déclenchement : destruction de bloc, mort d'entité, pêche, tonte, archéologie, troc (jalons M5 à M7 ; seules les tables et leur contexte sont décrits) ;
- l'entité "objet au sol" produite par le butin ;
- les enchantements et fournisseurs d'enchantements en tant que tels (`item-enchantments.md`) ; les fonctions `enchant_*` y sont décrites du point de vue du butin ;
- les échanges de villageois (`villager_trade`) : ils réutilisent les modificateurs et conditions de cette spec ;
- le tirage des RNG eux-mêmes (`core-random.md`).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) :
  - `resources/data/minecraft/loot_table/**` : 1447 fichiers ; `predicate/**` : 3 fichiers ; `context_int_provider/**` (26) et `context_float_provider/**` (4) ; `tags/**` ;
  - `generated/reports/registries.json` : registres `loot_pool_entry_type` (9), `loot_condition_type` (20), `loot_function_type` (42), `loot_nbt_provider_type` (2), `loot_score_provider_type` (2), `context_key_set` (31), `context_int_provider_type` (23), `context_float_provider_type` (28), `int_provider_type` (8), `float_provider_type` (4), `slot_source_type` (6), `entity_sub_predicate_type` (25), `data_component_predicate_type` (15), `block_predicate_type` (16) ;
  - `generated/reports/commands.json` : commandes `loot`, `item`, `give`, `clear` ; types d'argument `loot_modifier`, `slot_source`, `item_predicate`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Loot table", "Item modifier", "Predicate". Les pages couvrent 26.3 mais restent muettes sur les valeurs par défaut, les arrondis et l'ordre des tirages ; le contenu retenu est signalé S.
- **Rapports locaux** (indices, ADR 0002) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, chapitres 94 à 105 (inventaire des données, algorithme de butin, aléa et séquences). Il décrit des comportements observables par inspection (calcul du poids, absence de tirage avec un seul candidat, bornes d'un intervalle uniforme, priorité de la graine) : tous restent S ici.
- **Spec voisine** : `core-random.md` R12 (intervalle uniforme entier), R14 (séquences nommées), R30 à R35 (commande `/random`, séquences persistantes, choix de la source de butin) ; `data-datapack-loading.md` (chargement, empilement des packs, sondes P4, P11, P12, P16 sur `/loot`).
- **Sondes de l'oracle** : aucune exécutée. Voir PL1 à PL16.

Convention de preuve : D valeur lue dans un fichier extrait ; C calcul reproductible sur ces fichiers ; M observé sur l'oracle (non utilisé) ; S documentation publique seule ou hypothèse. Chaque règle S porte un **comportement provisoire**.

## Règles

### Format des tables

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Une table est un fichier `data/<espace>/loot_table/<chemin>.json`, d'identifiant `<espace>:<chemin>`. Les 1447 tables vanilla se décomposent par préfixe : `blocks` 1201, `entities` 109, `chests` 58, `gameplay` 26, `shearing` 22, `archaeology` 6, `charged_creeper` 6, `spawners` 5, `dispensers` 3, `equipment` 3, `harvest` 3, et 1 chacun pour `barrels`, `pots`, `brush`, `carve`, `till`. | C |
| R2 | Clés de table observées : `type` (1447), `random_sequence` (1447), `pools` (1403 ; 44 tables sont sans `pools`), `modifier` (9). Le champ `type` est un identifiant du registre `context_key_set` ; valeurs : `block` 1201, `entity` 115, `chest` 68, `shearing` 22, `gift` 21, `archaeology` 6, `block_interact` 5, `fishing` 4, `equipment` 3, `entity_interact` 1, `barter` 1. Le dossier n'implique pas le type : `chest` 68 = `chests` 58 + `spawners` 5 + `dispensers` 3 + `barrels` 1 + `pots` 1 ; `entity` 115 = `entities` 109 + `charged_creeper` 6 ; `block_interact` 5 = `harvest` 3 + `carve` 1 + `till` 1 ; `entity_interact` = `brush` ; `gameplay` 26 = `gift` 21 + `fishing` 4 + `barter` 1. `type` absent vaut `generic` (S, documentation). | D (observé) ; S (défaut) |
| R3 | Le format 26.3 utilise `type` comme discriminant de chaque condition, modificateur, entrée et fournisseur (et non `condition` ou `function`), `condition` (singulier) pour une ou plusieurs conditions, `modifier` (singulier) pour un ou plusieurs modificateurs, `terms` pour `all_of` et `any_of`, `term` pour `inverted`. Un décodeur des formes anciennes (`conditions`, `functions`, `function`, `reference`) est hors périmètre. Source documentaire : changements de 26.3 snapshot 4. | D |
| R4 | `condition` accepte : un objet de condition inline, une chaîne identifiant un prédicat nommé (référence), ou, dans `terms`, une liste de ces éléments. Références observées : `minecraft:tool/can_silk_touch` (162 usages) et `minecraft:tool/can_shear` (52). `modifier` accepte un objet inline ou une liste d'objets (les deux sont observés ; aucune référence à un modificateur nommé dans les données, le registre `item_modifier` n'a aucun fichier vanilla). Une liste de modificateurs s'applique dans l'ordre. Provisoire : une liste de conditions est un `all_of`. | D (formes) ; S (liste de conditions) |
| R5 | Clés de pool observées sur les 1589 pools : `entries` (1589), `rolls` (1589), `condition` (1029), `modifier` (10). `bonus_rolls` n'apparaît dans aucune table vanilla ; la documentation le donne optionnel de défaut 0.0 (fournisseur de nombre, multiplié par la chance, arrondi vers le bas). Provisoire : `bonus_rolls` défaut 0. Nombre de pools par table : 0 (44), 1 (1272), 2 (97), 3 (21), 4 (8), 5 (3), 6 (1), 7 (1). | D / C ; S (bonus_rolls) |
| R6 | `rolls` : un nombre (1494 pools à 1, 6 à 3, 5 à 2...) ou un fournisseur (80 pools, tous `uniform` (D) ; formes `{"type":"minecraft:uniform","min":a,"max":b}`). Provisoire : valeur négative ou nulle donne zéro tirage. | D (formes) ; S (reste) |
| R7 | Types d'entrée enregistrés (`loot_pool_entry_type`, 9) : `empty`, `item`, `loot_table`, `dynamic`, `tag`, `slots`, `alternatives`, `sequence`, `group`. Occurrences dans les données : `item` 2556, `alternatives` 87, `loot_table` 57, `empty` 39, `dynamic` 1, `tag` 1 ; `slots`, `sequence`, `group` : 0. | D |
| R8 | Champs communs des entrées : `type`, `condition` (223 `item`, 40 `loot_table`, 3 `alternatives`, 1 `dynamic`), `weight` (601 `item`, 9 `loot_table`, 35 `empty`), `quality` (3 `loot_table`, valeurs -2, 2, -1 ; toutes dans `gameplay/fishing`), `modifier` (1071 `item`, 2 `loot_table`). Défaut de `weight` : 1 ; défaut de `quality` : 0 (provisoire). Poids observés : de 2 à 148, un poids de 699. | D / S (défauts) |
| R9 | Champs propres : `item` : `name` (identifiant d'objet) ; `loot_table` : `value` (identifiant de table, ou table inline ; 57 usages) ; `dynamic` : `name` (identifiant de source dynamique, vu `minecraft:sherds`) ; `tag` : `items` (étiquette `#...`) et `expand` (booléen, vu `true` pour la table `entities/creeper`) ; `alternatives` : `children` ; `empty` : aucun. Provisoire pour `slots` : `slot_source` (documentation). | D / S |
| R10 | Les 6 tables de `trial_chambers` incluent des tables imbriquées inline dans `value`. Les entrées d'une table imbriquée ne font pas partie des entrées du pool parent ; le décodeur conserve la table imbriquée comme une valeur. 2741 noeuds d'entrée hors contenu interne (C, parcours `entries` puis `children` seulement). | D (présence) ; C (dénombrement) |

### Modèle de contexte (ADR 0004)

Le module `loot` déclare les interfaces de lecture et d'action ; `entity` et `game` les implémentent. Aucune dépendance de `loot` vers `entity`.

| Réf | Règle | Preuve |
|---|---|---|
| R11 | Un contexte de butin contient : un jeu de paramètres nommés (R12), la chance (flottant), une source d'aléa (R63), le niveau/monde en lecture seule. Il est construit par l'appelant (bloc détruit, entité tuée, coffre ouvert, commande) selon le `type` de la table (R14) et passé inchangé aux tables imbriquées. | S |
| R12 | Paramètres nommés (provisoire, noms de la documentation et des données) : `origin` (position), `this_entity`, `attacker` (entité à l'origine des dégâts), `direct_attacker` (entité qui a porté le coup, projectile par exemple), `attacking_player` (joueur crédité), `damage_source`, `tool` (pile), `block_state`, `block_entity`, `explosion_radius` (flottant), `enchantment_level`, `enchantment_active`, `target_entity`, `interacting_entity`. Cibles d'entité vues dans les données : `this`, `attacker`, `direct_attacker` (champ `entity` de `entity_properties`) ; la documentation ajoute `attacking_player`, `target_entity`, `interacting_entity`. Source de `copy_components` : `block_entity` (71 usages). | D (cibles vues) ; S (liste) |
| R13 | Registre `context_key_set` (31 jeux) : `empty`, `generic`, `chest`, `command`, `command_slot_source`, `command_compute_default`, `command_compute_position`, `command_compute_entity`, `selector`, `villager_trade`, `fishing`, `entity`, `equipment`, `archaeology`, `gift`, `barter`, `vault`, `advancement_reward`, `advancement_entity`, `advancement_location`, `block`, `block_use`, `block_interact`, `entity_interact`, `shearing`, `container_process`, `enchanted_damage`, `enchanted_item`, `enchanted_location`, `enchanted_entity`, `hit_block`. Identifiants numériques du registre de 0 à 30. Les paramètres requis et optionnels de chaque jeu ne sont pas dans les données ; ils sont S (la page "Loot table" les renvoie à une page dédiée). | D (noms) ; S (contenu) |
| R14 | Besoins par type de table, déduits des données (C, parcours des conditions et modificateurs de chaque type de table) : `block` : `origin`, `block_state` (`match_block`, `copy_state`), `tool` (`table_bonus`, `apply_bonus`, `match_tool`, références `tool/can_*`), `explosion_radius` (`survives_explosion`, `explosion_decay`), `block_entity` (`copy_components`), `this_entity` (2 usages) ; `entity` : `this_entity`, `attacker`, `direct_attacker`, `damage_source`, `attacking_player` (`killed_by_player`), niveau d'enchantement d'arme (`enchanted_count_increase`, `random_chance_with_enchanted_bonus`) ; `chest` : `origin` (`location_check`), `this_entity` (chance du joueur) ; `fishing` : `origin`, `this_entity` (le flotteur : `in_open_water`), `tool` ; `shearing` et `gift` : `this_entity` ; `archaeology`, `barter`, `equipment`, `entity_interact` : aucune lecture d'entité ; `block_interact` : `block_state` (`match_block`). | C |
| R15 | Comportement en l'absence d'un paramètre lu par une condition (documentation, S) : `damage_source_properties` et `location_check` (sans `origin`), `match_tool` (sans `tool`), `entity_scores` (sans entité), `enchantment_active_check` (hors `enchanted_location`) et `killed_by_player` (sans `attacking_player`) échouent ; `survives_explosion` sans `explosion_radius` réussit toujours ; `table_bonus` sans `tool` lit le niveau 0 ; `random_chance_with_enchanted_bonus` sans `attacker` lit le niveau 0. Le chargement de la table émet un avertissement si une condition lit un paramètre que le jeu du `type` ne fournit pas ; la table se charge quand même (S). | S |
| R16 | Ports de lecture (ADR 0004 nomme `EntityView` ; `BlockView` et `ItemView` sont des propositions de cette spec pour le découpage fin que l'ADR renvoie à M6). Surface déduite des prédicats utilisés dans les données vanilla (C, voir R57 à R60) : `EntityView` doit fournir au minimum : type d'entité (étiquettes `#` comprises), drapeaux (`is_on_fire`, `is_on_ground`, `is_flying`...), équipement par slot (pile), position et visibilité du ciel, véhicule (type), composants (`sheep/color`, `mooshroom/variant`), distance horizontale à une position, vitesse horizontale et bloc affectant le mouvement, tick périodique, taille d'un cube mob, capitaine de raid, état de tonte d'un mouton, crochet de pêche (eau libre), foudre (blocs enflammés), bloc sur lequel l'entité se tient. `BlockView` : état du bloc (propriétés), étiquettes, entité de bloc, biome, visibilité du ciel, fumée de feu de camp. `ItemView` : la pile `tool` et les piles d'équipement (R64). | C / S |
| R17 | Ports d'action (ADR 0004), nécessaires aux effets d'enchantement et non au tirage : `ActionSink` reçoit des intentions (infliger des dégâts, appliquer un effet de statut, enflammer, appliquer une impulsion, exploser, remplacer un bloc ou un disque, invoquer une entité, jouer un son, émettre des particules, exécuter une fonction, modifier des propriétés de bloc). Le tirage de butin pur ne les utilise pas. Les tests de `loot` utilisent des ports factices (liste d'intentions enregistrées). | S |
| R18 | La chance (`luck`) du contexte vaut : pour un coffre ouvert par un joueur, la valeur de l'attribut chance du joueur ; pour la pêche, chance du joueur plus bonus de pêche de la canne (`fishing_luck_bonus`, enchantement Luck of the Sea, voir `item-enchantments.md`) ; sinon 0. Valeur provisoire : flottant 32 bits. | S |

### Sélection d'un pool

| Réf | Règle | Preuve |
|---|---|---|
| R19 | Algorithme d'une table (provisoire, hypothèse H-flow) : (1) pour chaque pool dans l'ordre, évaluer `condition` du pool ; échec = pool ignoré sans tirage d'aléa lié au pool ; (2) calculer N, le nombre de tirages : `rolls` + floor(`bonus_rolls` x chance) (R5, R6) ; (3) répéter N fois : développer les entrées du pool, retirer les entrées dont la condition échoue, choisir au hasard pondéré, produire les piles de l'entrée choisie ; (4) appliquer les modificateurs du pool à chaque pile produite par le pool ; (5) appliquer les modificateurs de la table à toutes les piles à la fin. Les modificateurs d'une entrée s'appliquent à la sortie de cette entrée avant ceux du pool. Sondes PL5, PL6, PL7. | S |
| R20 | Les entrées d'un pool sont toutes évaluées à chaque tirage (les conditions sont réévaluées à chaque tirage, et peuvent consommer de l'aléa : `random_chance`). Le tirage est avec remise : une entrée peut sortir plusieurs fois. | S |
| R21 | `alternatives` : seul le premier enfant dont la condition réussit contribue (testé dans l'ordre) ; aucune sortie si aucun ne réussit. `sequence` : les enfants contribuent dans l'ordre jusqu'au premier dont la condition échoue. `group` : tous les enfants contribuent. Les composites se développent en entrées simples avant le tirage ; ils ne portent pas de poids propre. | S |
| R22 | `tag` avec `expand: true` : un enfant simple par objet de l'étiquette (quantité 1, composants par défaut), de même poids et de même condition que l'entrée ; `expand: false` : une seule entrée qui produit tous les objets de l'étiquette (une pile par objet). `expand` absent : défaut non documenté (provisoire `false`). Les modificateurs d'une entrée `tag` ne s'appliquent pas aux enfants développés (documentation signalant un défaut connu) : provisoire, ils s'appliquent. Sonde PL8. | S |
| R23 | `loot_table` : évalue la table référencée (ou inline) avec le même contexte et la même source d'aléa (pas de nouvelle séquence, voir R64) et renvoie ses piles ; pas de récursion directe (une table qui se référence elle-même est une erreur de chargement). | S |
| R24 | `empty` : candidat qui ne produit rien mais qui compte dans le total des poids. `dynamic` : produit des piles fournies par l'émetteur du contexte sous le nom donné (`minecraft:sherds` : les tessons du pot décoré cassé ; `minecraft:contents` : le contenu d'un conteneur) ; sans émetteur il ne produit rien. `item` : une pile de `name` quantité 1 avec les composants par défaut. `slots` : S, voir R71. | D (types) ; S (comportement) |
| R25 | Poids effectif d'une entrée simple : max(0, floor(poids + qualité x chance)). Calcul en flottants 32 bits selon l'hypothèse la plus prudente : f32(poids) + f32(f32(qualité) x chance), plancher, puis borne à 0. Les entrées de poids effectif 0 sont retirées des candidats. Sondes PL1, PL2. | S |
| R26 | Tirage entre candidats : si aucun candidat, aucun tirage et aucune sortie ; si un seul candidat, il est choisi sans consommer d'aléa ; si plusieurs, un entier est tiré dans [0, somme des poids effectifs[ et les candidats sont parcourus dans leur ordre de développement en retranchant leur poids ; le premier dont le reste devient négatif est choisi. Trois branches distinctes : une implémentation qui tire "pour rien" décale tous les tirages suivants. Sonde PL5. | S |
| R27 | Cas de la pêche (C sous R25 et R26, données `gameplay/fishing.json`) : trois entrées `loot_table`, poids/qualité 10/-2 (déchets), 5/+2 (trésor, conditionné à `in_open_water`), 85/-1 (poissons). Avec la chance entière L : poids effectifs 10-2L, 5+2L, 85-L (planchers à 0). En eau libre : L=0 -> 10, 5, 85 (total 100) ; L=1 -> 8, 7, 84 (99) ; L=2 -> 6, 9, 83 (98) ; L=3 -> 4, 11, 82 (97). Hors eau libre à L=0 : 10 et 85 (total 95). Poissons de `gameplay/fishing/fish` : poids 60, 25, 2, 13 (morue, saumon, tropical, globe). | C |
| R28 | Cas du puits du désert (`archaeology/desert_well`, C sous R26) : 6 entrées, deux tessons de poids 2, quatre autres de poids 1 (brique, émeraude, bâton, soupe suspecte) ; total 8 ; probabilité 25 pour cent par tesson et 12,5 pour cent par autre entrée. Cas des ruines (`trail_ruins_rare`) : 12 entrées de poids égal ; probabilité 1/12 chacune. | C |

### Conditions

Les conditions sont des données `{"type": ..., ...}` ou des références. 20 types enregistrés ; occurrences dans les tables de butin, les enchantements et les prédicats (D) et champs observés :

| Type | Occurrences | Champs observés |
|---|---|---|
| `all_of` | 52 | `terms` |
| `any_of` | 60 | `terms` |
| `damage_source_properties` | 16 | `predicate` |
| `enchantment_active_check` | 2 | `active` |
| `entity_properties` | 117 | `entity`, `predicate` |
| `entity_scores` | 0 | - |
| `environment_attribute_check` | 0 | - |
| `float_value_check` | 0 | - |
| `int_value_check` | 0 | - |
| `inverted` | 31 | `term` |
| `killed_by_player` | 24 |  |
| `location_check` | 15 | `offsetY`, `predicate` |
| `match_block` | 295 | `blocks`, `state` |
| `match_tool` | 4 | `predicate` |
| `random_chance` | 22 | `chance` |
| `random_chance_with_enchanted_bonus` | 11 | `enchanted_chance`, `enchantment`, `unenchanted_chance` |
| `survives_explosion` | 914 |  |
| `table_bonus` | 35 | `chances`, `enchantment` |
| `time_check` | 0 | - |
| `weather_check` | 2 | `thundering` |

| Réf | Règle | Preuve |
|---|---|---|
| R29 | `all_of` : vrai si tous les termes sont vrais (liste vide : vrai). `any_of` : vrai si au moins un terme est vrai (liste vide : faux). Une liste imbriquée dans `terms` se comporte comme `all_of`. `inverted` : négation de `term`. Court-circuit : les termes sont évalués dans l'ordre ; l'évaluation s'arrête au premier terme qui décide (provisoire ; ne consomme pas d'aléa pour les termes sautés). Sonde PL9. | S |
| R30 | `random_chance` : `chance` est un flottant 0 à 1 ou un fournisseur flottant contextuel (vu : `{"type":"minecraft:enchantment_level","amount":...}` dans les enchantements) ; vrai si un flottant tiré dans [0, 1[ est strictement inférieur à `chance`. Un tirage par évaluation. Valeurs observées dans les tables : 0.125 (6), 0.5 (6), 0.05 (4), 0.02, 0.25, 0.75. | D (valeurs) ; S (comparaison) |
| R31 | `random_chance_with_enchanted_bonus` : `unenchanted_chance` (flottant), `enchanted_chance` (valeur dépendante du niveau, ex. `linear` base 0.13 par niveau supplémentaire 0.02), `enchantment` ; le niveau est celui de l'enchantement sur l'arme de l'attaquant (0 sans attaquant) ; vrai si un flottant tiré est inférieur à la chance du niveau (la chance non enchantée à niveau 0). Un tirage. | D (forme) ; S (sens) |
| R32 | `table_bonus` : `enchantment`, `chances` (liste de flottants) ; la probabilité est `chances[niveau]` avec le niveau de l'enchantement sur l'outil (0 sans outil) ; un flottant tiré inférieur à la probabilité donne vrai. Listes vues : `[0.05, 0.0625, 0.083333336, 0.1]`, `[0.02, 0.022222223, 0.025, 0.033333335, 0.1]`, `[0.005, 0.0055555557, 0.00625, 0.008333334, 0.025]`, `[0.025, 0.027777778, 0.03125, 0.041666668, 0.1]`, `[0.1, 0.14285715, 0.25, 1.0]`, `[0.33, 0.55, 0.77, 1.0]` (35 usages, 6 listes). Le traitement d'un niveau hors de la liste n'est pas documenté : provisoire, l'indice est borné à la dernière case. Sonde PL10. | D (listes) ; S (sens) |
| R33 | `survives_explosion` : sans rayon d'explosion dans le contexte, vrai sans tirage ; avec un rayon r > 0, vrai avec la probabilité 1/r (un flottant tiré dans [0, 1[ inférieur à 1/r). Utilisé par 914 entrées/pools de blocs. Sonde PL11. | S |
| R34 | `match_block` : `blocks` (identifiant, étiquette ou liste) et `state` (table propriété -> valeur chaîne, correspondance exacte de chaque propriété listée), plus (documentation) `nbt`, `components`, `predicates` optionnels qui exigent une entité de bloc. Vu : `{"blocks":"minecraft:wheat","state":{"age":"7"}}`. Sans état de bloc dans le contexte : échec (provisoire). | D (forme) ; S (sens) |
| R35 | `match_tool` : `predicate` est un prédicat d'objet (R58) appliqué à la pile `tool` ; sans `tool` : échec. `entity_properties` : `entity` (R12) et `predicate` (prédicat d'entité, R57) ; entité absente : échec (provisoire). `location_check` : `predicate` (prédicat de position, R59), `offsetX/Y/Z` optionnels (vu `offsetY`) ; sans `origin` : échec. `damage_source_properties` : `predicate` (R60). `killed_by_player` : vrai si `attacking_player` est présent. | D (formes) ; S (sens) |
| R36 | `weather_check` : `raining` et `thundering` optionnels ; `raining: true` réussit si pluie ou orage, `thundering: true` seulement si orage (documentation). Vu dans les enchantements : `{"thundering": true}`. `time_check` : `clock`, `value` (nombre ou intervalle), `period` (entier optionnel) : compare 24000 x jours + heure du jour (modulo `period` si présent) ; non utilisé dans les données. | D (forme vue) ; S |
| R37 | `int_value_check`, `float_value_check` : `value` (fournisseur) et `test` (intervalle ou nombre) ; `entity_scores` : `entity`, `scores` ; `enchantment_active_check` : `active` ; `environment_attribute_check` : forme inconnue (ajout récent). Aucun usage de `int_value_check`, `float_value_check`, `entity_scores`, `environment_attribute_check`, `time_check` dans les données. | S |

### Modificateurs

42 types enregistrés ; occurrences dans les tables de butin, enchantements et prédicats (D) et champs observés :

| Type | Occurrences | Champs observés |
|---|---|---|
| `apply_bonus` | 32 | `condition`, `enchantment`, `formula`, `parameters` |
| `copy_components` | 71 | `include`, `source` |
| `copy_custom_data` | 0 | - |
| `copy_name` | 0 | - |
| `copy_state` | 10 | `block`, `properties` |
| `discard` | 18 |  |
| `enchant_randomly` | 52 | `options` |
| `enchant_with_levels` | 34 | `levels`, `options` |
| `enchanted_count_increase` | 80 | `count`, `enchantment`, `limit` |
| `exploration_map` | 18 | `decoration`, `destination`, `skip_existing_chunks`, `zoom` |
| `explosion_decay` | 190 |  |
| `fill_player_head` | 0 | - |
| `filtered` | 18 | `item_filter`, `on_fail` |
| `furnace_smelt` | 19 | `condition` |
| `limit_count` | 6 | `limit` |
| `modify_contents` | 0 | - |
| `sequence` | 0 | - |
| `set_attributes` | 0 | - |
| `set_banner_pattern` | 0 | - |
| `set_book_cover` | 0 | - |
| `set_components` | 6 | `components` |
| `set_contents` | 0 | - |
| `set_count` | 947 | `add`, `condition`, `count` |
| `set_custom_data` | 0 | - |
| `set_custom_model_data` | 0 | - |
| `set_damage` | 28 | `damage` |
| `set_enchantments` | 11 | `enchantments` |
| `set_firework_explosion` | 0 | - |
| `set_fireworks` | 0 | - |
| `set_instrument` | 1 | `options` |
| `set_item` | 0 | - |
| `set_loot_table` | 0 | - |
| `set_lore` | 0 | - |
| `set_name` | 8 | `name`, `target` |
| `set_ominous_bottle_amplifier` | 3 | `amplifier` |
| `set_potion` | 52 | `id` |
| `set_random_dyes` | 0 | - |
| `set_random_potion` | 0 | - |
| `set_stew_effect` | 3 | `effects` |
| `set_writable_book_pages` | 0 | - |
| `set_written_book_pages` | 0 | - |
| `toggle_tooltips` | 0 | - |

| Réf | Règle | Preuve |
|---|---|---|
| R38 | Tout modificateur accepte `condition` (documentation : disponible sur tous) : le modificateur ne s'applique que si la condition réussit, sinon la pile passe inchangée. Vu sur `set_count` (204 usages), `apply_bonus`, `furnace_smelt` (19, toujours conditionné). | D (usage) ; S (généralité) |
| R39 | `set_count` : `count` (fournisseur entier) fixe la quantité (provisoire : `add: true` ajoute à la quantité existante ; `add` défaut `false`). Formes de `count` : constante (395), `uniform` (534), `binomial` (18, champs `n` et `p`, par exemple tiges de melon). Arrondi : S ; la quantité résultante peut dépasser la taille de pile (R54). | D (formes) ; S (reste) |
| R40 | `limit_count` : `limit` entier ou `{min?, max?}` (fournisseurs) ; borne la quantité de chaque pile. Vu : `{"min":0}`, `{"max":9}`, `{"max":4,"min":1}`. | D / S |
| R41 | `explosion_decay` : s'il existe un rayon d'explosion dans le contexte, chaque unité de la pile est perdue avec la probabilité 1/rayon (un tirage par unité, dans l'ordre) ; sans rayon, pile inchangée. 190 usages. Sonde PL11. | S |
| R42 | `apply_bonus` : `enchantment` (vu `minecraft:fortune`), `formula`, `parameters`, niveau lu sur `tool`. Formules vues : `minecraft:ore_drops` (17, sans paramètre), `minecraft:uniform_bonus_count` (11, `{"bonusMultiplier": 1 ou 2}`), `minecraft:binomial_with_bonus_count` (4, `{"extra": 3, "probability": 0.5714286}`). Sémantique (documentation) : `ore_drops` : quantité multipliée par max(1, entier tiré dans [0, niveau + 2[) ; `uniform_bonus_count` : ajoute un entier tiré dans [0, niveau x bonusMultiplier] ; `binomial_with_bonus_count` : ajoute binomiale(n = niveau + extra, p = probability). Aux limites : `ore_drops` à niveau 0 ne change pas la quantité. Les tirages exacts (bornes inclusives ou exclusives, nombre de tirages) sont S ; provisoire : `ore_drops` = quantité x max(1, nextInt(niveau + 2)), `uniform_bonus_count` = quantité + nextInt(niveau x mult + 1), `binomial` = quantité + nombre de succès de (niveau + extra) tirages flottants < p. Sondes PL3, PL4. | D (paramètres) ; S (formules) |
| R43 | `enchanted_count_increase` : `count` (fournisseur flottant), `enchantment` (vu `minecraft:looting`), `limit` (entier, 0 = pas de limite, vu 1 sur 3 usages) : ajoute, pour chaque niveau, un tirage de `count` ; le total est arrondi après multiplication par le niveau (mode d'arrondi non documenté ; provisoire : arrondi vers le bas) ; le niveau est celui de l'arme de l'attaquant (0 sans attaquant). Vu : `uniform 0.0 à 1.0` (79), `uniform 1.0 à 2.0` (1). Sonde PL12. | D (formes) ; S (sens) |
| R44 | `set_potion` (`id`, identifiant de potion ; `empty` retire), `set_stew_effect` (`effects` liste de {`type`, `duration` fournisseur}), `set_ominous_bottle_amplifier` (`amplifier` fournisseur), `set_instrument` (`options` : étiquette, identifiant ou liste, tirage uniforme), `set_name` (`name` texte, `target` `item_name` ou `custom_name`), `set_damage` (`damage` fournisseur flottant : **fraction** de durabilité restante, 1.0 = neuf ; vu uniform 0.15 à 0.8, `add` défaut false), `set_enchantments` (`enchantments` table identifiant -> fournisseur de niveau, borné à 255 ; `add` défaut false), `set_components` (table de composants, clé `!id` pour retirer ; vu : `minecraft:trim`), `copy_state` (`block`, `properties` : copie les propriétés listées de l'état vers le composant `block_state`), `copy_components` (`source` `block_entity`, `include` liste de composants ; `exclude` optionnel), `furnace_smelt` (applique la recette de fonte à la pile sans changer la quantité ; sans recette : pile inchangée, provisoire). | D (champs) ; S (sens) |
| R45 | `enchant_randomly` : `options` (étiquette, identifiant ou liste), `only_compatible` (défaut `true`), `include_additional_cost_component` (défaut `false`) : choisit un enchantement uniformément parmi les candidats puis un niveau aléatoire ; un livre devient livre enchanté. `enchant_with_levels` : `levels` (fournisseur entier), `options` (optionnel : tous les enchantements si absent), `include_additional_cost_component` (défaut `false`) : enchante comme une table d'enchantement au niveau donné (`item-enchantments.md` R36 à R40). Vu : `enchant_randomly` 52 (`#minecraft:on_random_loot` 43), `enchant_with_levels` 34 (`levels` constante 30 ou uniforme 20 à 39, 5 à 15, 30 à 50). Sondes PL13, PE1. | D (usage) ; S (algorithme) |
| R46 | `exploration_map` : `destination` (étiquette de structure), `decoration` (type de marqueur), `zoom` (défaut 2), `search_radius` (défaut 50), `skip_existing_chunks` (défaut true) : crée une carte pointant vers la structure la plus proche (dépend de `/locate`, worldgen M4) ; 18 usages. `filtered` : `item_filter` (prédicat d'objet), `on_pass`, `on_fail` (modificateurs ; l'absent laisse la pile) ; vu : `item_filter` `{"predicates":{"minecraft:map_id":{}}}` et `on_fail` `{"type":"minecraft:discard"}` : supprime la carte si elle n'a pas pu être créée. `discard` : supprime la pile (18 usages). | D (formes) ; S (sens) |
| R47 | Modificateurs enregistrés sans usage dans les tables de butin, enchantements et prédicats (`set_random_dyes` n'apparaît que dans les échanges de villageois) : `copy_custom_data`, `copy_name`, `fill_player_head`, `modify_contents`, `sequence`, `set_attributes`, `set_banner_pattern`, `set_book_cover`, `set_contents`, `set_custom_data`, `set_custom_model_data`, `set_firework_explosion`, `set_fireworks`, `set_item`, `set_lore`, `set_loot_table`, `set_random_dyes`, `set_random_potion`, `set_writable_book_pages`, `set_written_book_pages`, `toggle_tooltips` (21). Leurs champs, d'après la documentation (S, sans comportement détaillé) : `set_item` {`item`} ; `set_custom_data` {`tag`} ; `copy_custom_data` {`source`, `ops`} ; `set_contents` {`entries`, `component`} ; `modify_contents` {`component`, `modifier`} ; `set_loot_table` {`loot_table_id`, `seed?`} ; `set_lore` {`lore`, `mode`, `offset?`, `size?`} ; `set_attributes` {`modifiers`, `replace?`} ; `set_banner_pattern` {`patterns`, `append`} ; `set_book_cover` {`author?`, `generation?`, `title?`} ; `set_custom_model_data` {`floats`, `flags`, `strings`, `colors`} ; `set_fireworks` {`explosions?`, `flight_duration?`} ; `set_firework_explosion` {`shape`, `colors`, `fade_colors`, `has_trail`, `has_twinkle`} ; `set_random_dyes` ; `set_random_potion` {`options?`} ; `set_writable_book_pages`, `set_written_book_pages` {`pages`, `mode`, `offset?`, `size?`} ; `toggle_tooltips` {`toggles`} ; `copy_name` {`source`} ; `fill_player_head` {`entity`} ; `sequence` {`functions`}. **Divergence à noter** : la documentation donne `id` pour `set_random_dyes`, alors que la donnée vanilla (échange `emerald_dyed_leather_chestplate`) utilise `number_of_dyes` avec un fournisseur `add` d'entrées `[1, binomial(n=2, p=0.75)]`. Les données priment : champ `number_of_dyes`. Ces 21 types sont à implémenter au décodage (R3) et leur comportement est reporté après les sondes PL16. | D (divergence) ; S (le reste) |
| R48 | Ordre d'application (hypothèse H-order, provisoire) : modificateurs de l'entrée (dans l'ordre de la liste), puis modificateurs du pool, puis modificateurs de la table. Pour `loot_table` imbriquée : la table imbriquée applique ses modificateurs avant de rendre les piles à l'entrée parente. Sonde PL6. | S |
| R49 | Une pile rendue par un modificateur peut être vide (quantité 0, `discard`, `limit_count` max 0) : elle est supprimée de la sortie. Un modificateur appliqué à une pile déjà vide ne fait rien. Provisoire. | S |

### Fournisseurs de nombres

| Réf | Règle | Preuve |
|---|---|---|
| R50 | Fournisseurs entiers contextuels (`context_int_provider_type`, 23) : `constant`, `uniform`, `binomial`, `weighted_list`, `conditional`, `number_dispatcher`, `score`, `storage`, `environment_attribute`, `from_float`, `abs`, `avg`, `add`, `sub`, `mul`, `div`, `mod`, `floor_div`, `floor_mod`, `negate`, `pow`, `min`, `max`. Fournisseurs flottants (`context_float_provider_type`, 28) : `constant`, `uniform`, `weighted_list`, `conditional`, `number_dispatcher`, `storage`, `environment_attribute`, `enchantment_level`, `from_int`, `length`, `abs`, `avg`, `ceil`, `floor`, `round`, `truncate`, `add`, `sub`, `mul`, `div`, `mod`, `negate`, `pow`, `sqrt`, `sin`, `cos`, `min`, `max`. Un nombre nu (`1`, `2.0`) est un `constant`. Usage dans les tables de butin : nombres nus, `uniform`, `binomial`. Usage dans les données hors butin : `div` (durées de carburant), `conditional` (vitesse de cuisson), `number_dispatcher` et `weighted_list` (compostage), `add` (échange). | D |
| R51 | `uniform` entier : bornes `min` et `max` inclusives ; si min < max, la valeur est `min + nextInt(max - min + 1)` (un tirage borné) ; si min >= max, la valeur est `min` sans tirage (spec `core-random` R12). Formes observées : constantes de 0 à 50 dans les intervalles, `{"type":"minecraft:uniform","max":3,"min":1}` pour 26 `rolls`. | D (forme) ; S (tirage, `core-random` R12) |
| R52 | `uniform` flottant : bornes `min` et `max` ; valeur = min + nextFloat() x (max - min) (provisoire ; une seule consommation). Les enchantements utilisent `min_inclusive`/`max_exclusive` pour les fournisseurs de hauteur de son (forme différente, famille `float_provider_type`). | D (forme) ; S (formule) |
| R53 | `binomial` : champs `n` et `p` ; valeur = nombre de succès de n tirages flottants indépendants strictement inférieurs à p (provisoire ; n tirages consommés dans l'ordre). Valeurs vues : n=3 avec p=0.53333336 ou 0.06666667 (tiges de melon et citrouille). | D (valeurs) ; S (algorithme) |

### Production de piles

| Réf | Règle | Preuve |
|---|---|---|
| R54 | Une pile produite avec une quantité supérieure à `max_stack_size` est fractionnée en piles de taille maximale plus un reste, avant d'être donnée à l'appelant. Provisoire. Sonde PL14 (table dont `set_count` produit 70 pierres). | S |
| R55 | Une table à zéro pool (44 tables vanilla) rend une liste vide. Une table à zéro tirage rend une liste vide. | D (44) ; S |
| R56 | Les piles de sortie sont des piles complètes (`item-components-inventory.md` R14) : identifiant, quantité, modifications de composants. Aucune normalisation autre que celle de `item-components-inventory.md` R17 n'est appliquée. | S |

### Prédicats

Le format des prédicats (objet, entité, bloc, position, source de dégâts) est utilisé par les conditions et par les enchantements. Seules les clés rencontrées dans les données sont décrites (D) ; les autres sont S.

| Réf | Règle | Preuve |
|---|---|---|
| R57 | Prédicat d'entité (`entity_properties.predicate`) : objet dont les clés sont des identifiants du registre `entity_sub_predicate_type` (25 types : `entity_type`, `location`, `stepping_on`, `movement_affected_by`, `distance`, `movement`, `effects`, `nbt`, `flags`, `equipment`, `periodic_tick`, `vehicle`, `passenger`, `targeted_entity`, `team`, `slots`, `components`, `predicates`, `entity_tags`, `type_specific/{lightning, fishing_hook, player, cube_mob, raider, sheep}`). Clés vues et occurrences : `entity_type` 154 (`minecraft:piglin`, étiquette `#minecraft:...`), `location` 71, `components` 60 (`{"minecraft:sheep/color":"white"}`), `flags` 35 (`is_on_fire`, `is_on_ground`, `is_flying`), `type_specific/sheep` 32 (`sheared`), `equipment` 30 (slot -> prédicat d'objet), `vehicle` 9, `type_specific/player` 6, `movement_affected_by` 5, `movement` 3 (`horizontal_speed` intervalle), `distance` 3 (`horizontal` intervalle), `type_specific/cube_mob` 2 (`size`), `periodic_tick` 2 (entier), `type_specific/raider`, `type_specific/fishing_hook` (`in_open_water`), `type_specific/lightning` (`blocks_set_on_fire`), `stepping_on`. Toutes les clés présentes doivent être vraies (ET). | D |
| R58 | Prédicat d'objet (`match_tool.predicate`, `filtered.item_filter`, ingrédients d'alchimie) : clés vues `items` (identifiant, étiquette ou liste), `predicates` (table identifiant de type -> valeur : `minecraft:enchantments` liste de `{enchantments, levels?}` ; `minecraft:map_id` `{}` ; `minecraft:dyed_color` `{}` ; `minecraft:jukebox_playable` `{}`). Observation D importante : `predicates` accepte des composants (`dyed_color`, `map_id`) qui ne sont pas dans les 15 types du registre `data_component_predicate_type` (`damage`, `enchantments`, `stored_enchantments`, `potion_contents`, `custom_data`, `container`, `bundle_contents`, `firework_explosion`, `fireworks`, `writable_book_content`, `written_book_content`, `attribute_modifiers`, `trim`, `jukebox_playable`, `villager/variant`) avec la valeur `{}` : sémantique S, hypothèse "le composant est présent". `components` (valeurs exactes) existe aussi (documentation) et `count` (intervalle). Un prédicat `{"items": "<id>"}` seul compare l'objet. Sondes PL15, PI11. | D / S |
| R59 | Prédicat de position (`location_check.predicate`, clé `minecraft:location`) : clés vues `block` (`{blocks, state?}`, 42), `biomes` (identifiant, étiquette ou liste, 11), `can_see_sky` (booléen), `smokey` (booléen, feu de camp). | D |
| R60 | Prédicat de source de dégâts : clés vues `tags` (liste de `{id: étiquette, expected: booléen}`, 8), `source_entity` (prédicat d'entité, 6), `direct_entity` (1), `is_direct` (booléen, 2). | D |
| R61 | Prédicats nommés (registre `predicate`, 3 fichiers, 2 sans usage dans les butins directs) : `tool/can_silk_touch` (`match_tool` avec `predicates.minecraft:enchantments` `[{enchantments:"minecraft:silk_touch", levels:{min:1}}]`), `tool/can_shear` (`match_tool` avec `items:"minecraft:shears"`), `block/fast_cooking` (`match_block` sur `smoker`, `blast_furnace`). Une référence se résout au chargement ; une référence inconnue est une erreur de chargement de la table (provisoire). | D |
| R62 | Prédicat de bloc des effets d'enchantement (registre `block_predicate_type`, 16 types dont `matching_blocks`, `matching_block_tag`, `matching_fluids`, `unobstructed`, `all_of`, `any_of`, `not`, `true`, `solid`, `replaceable`, `would_survive`, `has_sturdy_face`, `inside_world_bounds`, `height_range`, `volume_match`, `matching_biomes`) : décrit dans `item-enchantments.md`. | D (registre) |

### Aléa et déterminisme

Les règles de cette section prolongent `core-random.md` R34 et R35, qui renvoient à cette spec.

| Réf | Règle | Preuve |
|---|---|---|
| R63 | Choix de la source d'aléa d'un tirage (priorité décroissante) : (1) une graine explicite **non nulle** (paramètre de la commande ou `LootTableSeed` du conteneur) donne un générateur à état 48 bits initialisé avec cette graine ; la graine 0 signifie "absente" ; (2) sinon la séquence nommée de la table (`random_sequence`), dont la source est le générateur 128 bits de `core-random` R14 ; (3) sinon la source du niveau. Réduire (1) et (2) à un seul générateur change la correspondance graine -> contenu. Sondes PL5, PL16. | S |
| R64 | Les 1447 tables vanilla déclarent toutes un `random_sequence` (valeur = `minecraft:` + chemin de la table, ex. `minecraft:blocks/stone`) ; les 68 `trade_set` aussi. Une table imbriquée (`loot_table`) n'ouvre pas sa propre séquence : le contexte et la source de l'appelant sont passés (provisoire). | D (présence) ; S (imbrication) |
| R65 | La séquence nommée est initialisée à la première utilisation à partir de la graine du monde, du sel et de l'identifiant de la séquence (réglages R30 de `core-random`) et persiste (`random_sequences.dat`). Les tirages d'une table avec séquence avancent l'état persistant de cette séquence. Deux tables partageant un `random_sequence` partagent l'état : le contenu dépend de l'ordre d'appel. | S |
| R66 | Ordre de consommation de l'aléa (hypothèse H-draw, base des vecteurs dorés) : (a) `rolls` (si fournisseur aléatoire) avant tout autre tirage du pool ; (b) à chaque tirage, les conditions évaluées pendant le développement dans l'ordre des entrées (un tirage par `random_chance`, `survives_explosion` avec rayon, `table_bonus`, `random_chance_with_enchanted_bonus` évalués) ; (c) un entier de sélection seulement si au moins deux candidats ; (d) les modificateurs de la pile choisie, dans l'ordre, chacun consommant selon sa définition ; (e) à la fin du pool, rien. Sonde PL5 (empreinte de contenu en fonction de la graine). | S |
| R67 | Déterminisme des commandes : `/loot` sans graine utilise la source du niveau ou la séquence de la table (selon R63) : deux appels consécutifs diffèrent. Les comparaisons statistiques employées par M6 sont des tests du khi-deux (voir Plan de tests). Les tests bit à bit n'existent que pour les conteneurs à `LootTableSeed` non nul. | S |

### Conteneurs, commandes et déballage

| Réf | Règle | Preuve |
|---|---|---|
| R68 | Un conteneur portant `LootTable` (String) et éventuellement `LootTableSeed` (Long) est "déballé" à son ouverture, à sa destruction ou à l'accès d'un entonnoir ; ces champs sont alors supprimés du conteneur et `Items` est rempli. `LootTableSeed` ne peut exister sans `LootTable` ; une valeur 0 ou absente signifie graine aléatoire. Même graine et même table donnent le même contenu. | S (documentation) |
| R69 | Le contexte de déballage est de type `chest` : `origin` = centre du bloc, `this_entity` = le joueur qui ouvre (avec sa chance) si présent. Remplacer ce contexte par "graine du monde + position entière + identifiant de coffre" est faux. | S |
| R70 | Placement des piles dans les slots du conteneur : les piles produites sont réparties dans les slots libres (les piles de plus d'une unité peuvent être fractionnées pour remplir davantage de slots) puis les emplacements sont mélangés avec le même générateur ; l'algorithme et son ordre de consommation sont S (le contenu final dépend de la graine). Sonde PL16. | S |
| R71 | `slots` (type d'entrée) et `slot_source_type` (6 types : `contents`, `empty`, `filtered`, `group`, `limit_slots`, `slot_range`) lisent des piles dans des slots d'une source (`this`, `attacker`...) ; commandes `item replace/fill/override/modify` (D `commands.json`) utilisent `slot_source` et `loot_modifier`. Sémantique et formes : S, sonde PL16 b. | D (syntaxe) ; S (reste) |
| R72 | Commande `/loot` (D `commands.json`) : cibles `give <joueurs>`, `insert <pos>`, `replace block <pos> <slot> [count]`, `replace entity <entités> <slot> [count]`, `spawn <vec3>` ; sources `loot <table>`, `kill <entité>`, `mine <pos> [mainhand]`, `mine <pos> [offhand]`, `mine <pos> [<outil>]`, `fish <table> <pos>` avec les mêmes options (`mainhand`, `offhand` ou un outil). La source `loot` utilise un contexte de type `command` ; `kill`, `mine` et `fish` utilisent les contextes d'entité, de bloc et de pêche. Provisoire pour la correspondance contexte/source. | D (syntaxe) ; S (contextes) |
| R73 | Le butin d'un bloc détruit dépend aussi des règles de jeu `block_drops`, `entity_drops`, `block_explosion_drop_decay` (D registre `game_rule`, défauts true). Hors périmètre ici, rappelées pour les scénarios. | D (existence) |

## Entrées / sorties

- **Table** : arbre décodé (R1 à R10) ; entrée = (table, contexte, source d'aléa) ; sortie = liste ordonnée de piles.
- **Contexte** : jeu de paramètres selon R12 et R13, chance flottante, ports de lecture ; les ports d'action sont fournis par l'appelant.
- **Source d'aléa** : trois origines (R63) ; consommation décrite en R66.
- **Déballage de conteneur** : entrée = (table, graine, contexte de coffre, taille du conteneur) ; sortie = tableau de piles par slot.

## Cas limites

- Zéro candidat, un candidat, plusieurs candidats (R26) ; poids annulé par la chance ; poids totalement nul.
- Intervalle uniforme dégénéré (`min` = `max`) : aucun tirage ; `min` > `max` (S).
- Condition aléatoire refusée après une entrée choisie ; entrée choisie puis supprimée par un modificateur (`discard`, `filtered`).
- Table imbriquée avec graine explicite 0 et non nulle ; séquence partagée entre deux tables ; sauvegarde entre deux appels.
- `rolls` aléatoire de 0 ; pool sans entrées.
- Quantité supérieure à la taille de pile (R54) ; `set_count add` sur une pile vide.
- Table avec `type` incohérent avec les paramètres lus (R15).
- Pot décoré cassé contre intact (`dynamic` contre `item` avec `copy_components`).
- Niveau d'enchantement hors de la liste de `table_bonus` (R32).
- Coordonnées négatives du coffre : aucune dépendance de position dans le tirage (le déballage ne lit que le centre du bloc, R69).
- Tag `expand` : étiquette vide, étiquette contenant une étiquette.

## Vecteurs de test

D/C (lisibles sans oracle) :

| Vecteur | Entrée | Sortie attendue | Preuve |
|---|---|---|---|
| V-L1 | Décodage de `blocks/stone` | table `block`, 1 pool, `rolls` 1, entrée `alternatives` de 2 enfants : `item` `stone` avec condition-référence `minecraft:tool/can_silk_touch`, puis `item` `cobblestone` avec `survives_explosion` ; `random_sequence` `minecraft:blocks/stone` | D |
| V-L2 | Décodage de `blocks/oak_leaves` | 3 pools ; pool 0 : `alternatives` (`oak_leaves` si `any_of` [`tool/can_shear`, `tool/can_silk_touch`], sinon `oak_sapling` si `all_of` [`survives_explosion`, `table_bonus` fortune `[0.05, 0.0625, 0.083333336, 0.1]`]) ; pool 1 conditionné par `inverted(any_of ...)` : `stick` avec `table_bonus` `[0.02, 0.022222223, 0.025, 0.033333335, 0.1]`, modificateurs `set_count` uniform 1 à 2 puis `explosion_decay` ; pool 2 : `apple` | D |
| V-L3 | Décodage de `entities/cow` | 2 pools ; pool 0 : `leather`, `set_count` uniform 0 à 2 puis `enchanted_count_increase` looting uniform 0.0 à 1.0 ; pool 1 : `beef`, `set_count` uniform 1 à 3, `furnace_smelt` conditionnée par `any_of` [`entity_properties this flags is_on_fire`, `entity_properties direct_attacker equipment mainhand predicates enchantments #minecraft:smelts_loot`], `enchanted_count_increase` | D |
| V-L4 | Décodage de `gameplay/fishing` | 3 entrées `loot_table` (junk 10/-2, treasure 5/+2 avec `fishing_hook.in_open_water`, fish 85/-1) | D |
| V-L5 | Distribution de pêche (R27) | voir R27 | C (sous R25, R26) |
| V-L6 | `archaeology/desert_well` | 25 pour cent par tesson, 12,5 pour cent par autre entrée (R28) | C (sous R26) |
| V-L7 | Dénombrement | 1447 tables ; 1589 pools ; 2556 entrées `item` ; types de table de R2 ; 21 types de modificateur utilisés sur 42, 15 types de condition utilisés sur 20 (`time_check`, `entity_scores`, `int_value_check`, `float_value_check`, `environment_attribute_check` non utilisés) | C |
| V-L8 | `blocks/decorated_pot` | `alternatives` : `dynamic` `minecraft:sherds` sous `match_block decorated_pot cracked "true"`, sinon `item decorated_pot` avec `copy_components` source `block_entity` include `["minecraft:pot_decorations"]` | D |

Vecteurs de comportement (S, à produire par les sondes ; l'oracle donne la valeur) :

| Vecteur | Entrée | Sortie attendue (provisoire) |
|---|---|---|
| V-L9 | `/loot spawn 0 100 0 loot minecraft:blocks/stone`, N = 1000 | 1000 x `cobblestone` x1 (aucune aléa) |
| V-L10 | `/loot spawn ... mine <pos> <outil à Silk Touch>` sur `stone` | 1 x `stone` |
| V-L11 | Fortune 0 à 3 sur `oak_leaves` x 200000 | fréquence de sapling : 5, 6.25, 8.33, 10 pour cent (R32) à l'écart statistique près |
| V-L12 | `fishing` chance 0 à 3, N = 200000 | proportions de R27 |
| V-L13 | Coffre `simple_dungeon` avec `LootTableSeed` S (10 graines) | empreinte du contenu par graine (golden) |

## Plan de tests

- **Unitaire (L1)** : décoder les 1447 tables, les 3 prédicats et les 30 fournisseurs contextuels sans erreur ; réencoder et comparer à l'arbre JSON (ordre des clés indifférent) ; V-L1 à V-L8, V-L7 ; tests de propriétés : somme des probabilités de sélection = 1 pour les cas sans condition ; invariance par permutation des entrées de même poids ; ports factices pour toutes les conditions (chaque condition, chaque paramètre absent, R15).
- **Statistique (L2)** : pour chaque scénario, N >= 200000 tirages sur le moteur et sur l'oracle ; test du khi-deux à répartition identique, rejet si p < 1e-4, répété avec trois graines de session ; tolérance d'un test de proportion à 5 écarts-types pour les scénarios à une classe. Scénarios : V-L9 à V-L12 plus 50 tables tirées de `tools/diff/samples.json` par type de table (au moins 3 par type de R2).
- **Déterministe (L2)** : conteneurs à `LootTableSeed` non nul : comparaison exacte des slots sur 200 graines x 10 tables (PL5, PL16) ; c'est ce test qui valide R63, R66, R70.
- **Niveau A** : tests écrits en aveugle par le `test-engineer`, relecture par un second agent (spec `loot` = niveau de risque A).

## Sondes proposées

Cadre : serveur 26.3 headless, monde plat, RCON, `/tick freeze`. Commandes de `commands.json` : `loot spawn <x> <y> <z> loot <table>`, `loot give @s mine <pos> <outil>`, `/item modify`. Les conteneurs à graine : `setblock <pos> minecraft:chest{LootTable:"<table>",LootTableSeed:<graine>L}` suivi d'un déballage sans client (entonnoir sous le coffre, `data get block`).

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Règles levées |
|---|---|---|---|
| PL1 | Pack `sonde` : table `sonde:t1` pool 1 roll, 2 entrées `item` poids 1 et quality +1 (pierre) et poids 100 quality -1 (terre) ; chance via `loot give @s fish ... <pos>` avec effet `luck` appliqué (`effect give @s minecraft:luck 1000 N`) ; N = 0 à 5 ; 200000 `/loot spawn` | Proportions suivant max(0, floor(poids + qualité x chance)) | R18, R25 |
| PL2 | Même table avec poids fractionnaire de calcul (poids 3 quality 0.5 impossible : tester les valeurs entières 1 à 3 et la chance 1 à 5 pour détecter le plancher) | Frontières de plancher | R25 |
| PL3 | Table `sonde:ore` : `item diamond` + `apply_bonus` fortune `ore_drops`, `loot spawn ... mine <pos> <pioche avec fortune N>` 200000 fois, N=0..3 | Distribution de la quantité : support et probabilités | R42 |
| PL4 | Tables `uniform_bonus_count` (mult 1 et 2), `binomial_with_bonus_count` (extra 3, p 0.5714286) avec outil fortune 0 à 3, 200000 tirages chacune | Distributions | R42 |
| PL5 | Conteneurs à graine S pour 200 graines sur `chests/simple_dungeon`, `chests/abandoned_mineshaft`, `chests/bastion_treasure` : matérialiser et enregistrer (slot, pile) ; variante avec table à un seul candidat par pool pour isoler R26 | Empreinte par graine ; confirmation de R63 (graine non nulle), R66, R26 | R26, R63, R66 |
| PL6 | Table avec modificateurs aux trois niveaux (entrée `set_count` 2, pool `set_count add` +3, table `limit_count` max 4) ; 1 `/loot spawn` | Quantité finale 4 si ordre entrée -> pool -> table ; autre ordre donnerait une autre valeur | R19, R48 |
| PL7 | Table à `rolls` uniform 0 à 3, `bonus_rolls` 0.5, chance 2 (effet luck 2) ; 200000 | Loi du nombre de tirages | R5, R6, R19 |
| PL8 | Entrée `tag` sur `#minecraft:dyes` avec `expand` true puis false, avec `set_count` 5 en modificateur de l'entrée | Une pile ou 16 piles ; effet du modificateur | R22 |
| PL9 | `any_of`/`all_of` vides ; `any_of` avec `random_chance` d'abord pour tester le court-circuit (graine sur conteneur) | Valeurs de R29 ; consommation | R29 |
| PL10 | `table_bonus` avec liste courte (2 cases) et outil fortune 3 | Index borné ou échec | R32 |
| PL11 | `survives_explosion` + `explosion_decay` sur butin de bloc détruit par TNT à rayon connu (`/summon tnt` à distance) : N = 20000 | Proportion 1/rayon, perte par unité | R33, R41 |
| PL12 | Table `entities/cow` avec arme Looting 0 à 3, 100000 `/loot spawn ... kill @e[type=cow,limit=1]` (vache sans IA sous `/tick freeze`) | Moyenne et support du cuir et du boeuf | R43 |
| PL13 | Tables `enchant_randomly` sur livre et sur épée (`options` étiquette ou liste), 100000 | Loi des enchantements et niveaux | R45 |
| PL14 | Table `set_count` 70 `cobblestone` ; `set_count` 100 ; `loot spawn` et `loot give` | Fractionnement de pile | R54 |
| PL15 | Tables `filtered` avec `item_filter` `{"predicates":{"minecraft:dyed_color":{}}}` sur cuir teinté et non teinté, et avec `minecraft:map_id` | Sémantique de présence | R58 |
| PL16 | (a) Tables à `loot_table` imbriquée avec séquence propre contre séquence partagée, deux conteneurs à graine (graine 0 et non nulle) ; (b) `item modify`/`item fill`/`item override` avec `slot_source` de chaque type | Source d'aléa de l'imbriquée ; formes de `slot_source` | R64, R70, R71 |
| PL17 | Pack `sonde` : tables avec champs omis (`weight`, `quality`, `expand`, `type`, `bonus_rolls`), `rolls` à 0, négatif et fractionnaire, `condition` en liste, pool vide, table sans pool ; `loot spawn` 20000 fois et journal | Défauts de R2, R4 à R6, R8, R9, R22, R55 | R2, R4 à R6, R8, R9, R22, R55 |
| PL18 | Tables `sonde:ctx_*` dont une condition ou un modificateur lit un seul paramètre (`this`, `attacker`, `direct_attacker`, `attacking_player`, `tool`, `origin`, `block_state`, `explosion_radius`, chance) ; appelées par `loot spawn ... loot`, `kill`, `mine`, `fish` ; journal des avertissements au chargement pour un `type` incohérent | Quel jeu de paramètres chaque source fournit ; comportement d'un paramètre absent | R11 à R15, R69, R72 |
| PL19 | Entrées `group`, `sequence`, `alternatives` imbriquées, `dynamic` (`minecraft:contents`), `empty` avec poids, `slots` avec chaque `slot_source` ; conteneurs à graine pour distinguer les consommations | Contenu des composites ; comportement de `dynamic` sans émetteur | R20, R21, R24 |
| PL20 | Une table par condition (R29 à R37), 200000 tirages chacune : `random_chance` (0, 0.5, 1), `random_chance_with_enchanted_bonus`, `match_block`, `weather_check` (pluie et orage), `time_check`, `int_value_check`, `float_value_check`, `entity_scores`, `killed_by_player`, `entity_properties` pour chaque cible, `damage_source_properties` | Proportions et branches de R29 à R37 | R29 à R37 |
| PL21 | Une table par modificateur avec entrée d'une pile connue (`set_count` avec `add`, `limit_count`, `set_damage` avec `add`, `set_name` des deux cibles, `set_enchantments` avec `add`, `set_components` avec `!`, `copy_state`, `furnace_smelt`, `filtered`, `discard`, `set_potion`, `set_stew_effect`, `set_instrument`, `exploration_map`) ; `loot spawn` et relecture de l'objet au sol (`data get entity`) | Pile résultante exacte de R38 à R40, R44, R46, R49, R56 | R38 à R40, R44, R46, R49, R56 |
| PL22 | Tables isolées avec un seul tirage : `set_count` uniform entier (bornes 0 à 3 et 5 à 5), `set_damage` uniform flottant, `set_count` binomial (n=3, p=0.5) ; conteneurs à `LootTableSeed` sur 200 graines | Valeurs exactes par graine : formules de R51 à R53 | R51 à R53 |

## Couverture des règles S par les sondes

R2, R4, R8, R9, R55 : PL17 ; R11, R12, R13, R15, R69, R72 : PL18 ; R14 est C ; R16 et R17 sont des décisions de conception (ADR 0004), vérifiées par la règle d'architecture (ArchUnit : aucun import de `entity` dans `loot`) et par les ports factices, sans sonde ; R20, R21, R23, R24 : PL19, PL16 ; R30 à R37 : PL20 ; R38 à R40, R44, R46, R49, R56 : PL21 ; R51 à R53 : PL22, PL3, PL4 ; R65 à R68 : PL5, PL16, et sondes P1 et P2 de `core-random.md` pour la persistance ; R67 (déterminisme des commandes) : PL5 en deux appels successifs sans graine. Aucune autre règle S n'est sans sonde.

## Questions ouvertes

- Contenu exact (paramètres requis et optionnels) de chacun des 31 jeux de contexte (R13) : seuls les besoins déduits des données sont acquis (R14).
- Défauts absents de la documentation et des données : `expand` (R22), niveau hors liste de `table_bonus` (R32), arrondi de `enchanted_count_increase` (R43), `set_count`/`set_damage` (arrondis).
- Formules exactes des tirages de `apply_bonus` (bornes et nombre de tirages), `uniform` et `binomial` flottants.
- Ordre H-order (R48) et H-draw (R66), source de la graine de `LootTableSeed` et mélange des slots (R70).
- Précision flottante 32 bits du poids effectif (R25) : l'oracle ne la distingue du calcul en double que sur des frontières construites (PL2).
- Comportement de `slots`, `sequence`, `group` (aucune donnée vanilla) et de 21 types de modificateur sans usage (R47).
- Les sondes PL1 à PL16 supposent l'effet de potion `luck` et un client de commande pour la chance de pêche réelle ; pour les scénarios de pêche complets, un client protocole minimal est nécessaire (voir `item-components-inventory.md` Questions ouvertes).
- Règles S restantes : R4 (liste), R5 (`bonus_rolls`), R6, R8 (défauts), R11 à R13 (contenu), R15 à R28 (hors D/C), R29 à R37 (sens), R38 à R49, R51 à R53 (tirages), R54, R56, R63 à R73 (sauf présences D).

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Tables de butin | M6 | A | spécifié | `docs/specs/loot-tables.md` (formats et dénombrements en D/C ; tirage, ordre d'aléa, conditions et modificateurs en S avec sondes PL1 à PL16) |
| Prédicats et modificateurs d'objet (ligne à créer) | M6 | A | spécifié | idem, R38 à R49, R57 à R62 |

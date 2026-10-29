# Tables de butin, contexte, conditions, modificateurs et prédicats

Statut : brouillon
Jalon : M6
Risque : A
Règles : R1 à R80 (R4, R5, R14, R19, R44, R57, R61, R74 et R80 sont découpées en sous-règles citables : R4a, R4b, R5a...)

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
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Loot table", "Item modifier", "Predicate". Les pages couvrent 26.3 mais restent muettes sur les valeurs par défaut, les arrondis et l'ordre des tirages ; le contenu retenu est signalé S. Relues le 2026-10-29 pour les règles R22, R33, R40 à R42 et R74 à R77 : "Item modifier" (fonctions `explosion_decay`, `apply_bonus`, `limit_count`), "Loot table" (entrée `tag`, `rolls`, `bonus_rolls`, récursion), "Predicate" (condition `survives_explosion`), "Explosion" (probabilité de lâcher d'un bloc détruit, règles de jeu de décroissance) et "Number provider" (références par identifiant, `binomial`, `from_float`, `from_int`). Les pages sont lues par un outil de résumé automatique : le contenu est reformulé ici, jamais recopié, et une lecture littérale douteuse est signalée comme telle.
- **Rapports locaux** (indices, ADR 0002) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, chapitres 94 à 105 (inventaire des données, algorithme de butin, aléa et séquences). Il décrit des comportements observables par inspection (calcul du poids, absence de tirage avec un seul candidat, bornes d'un intervalle uniforme, priorité de la graine) : tous restent S ici.
- **Spec voisine** : `core-random.md` R12 (intervalle uniforme entier), R14 (séquences nommées), R30 à R35 (commande `/random`, séquences persistantes, choix de la source de butin) ; `data-datapack-loading.md` (chargement, empilement des packs, sondes P4, P11, P12, P16 sur `/loot`).
- **Sondes de l'oracle** : aucune exécutée. Voir PL1 à PL28.

Reproduction : tous les dénombrements D et C de cette spec sortent du script `loot_stats.py` de l'annexe, lancé avec `python -I loot_stats.py "$REF"` (`REF` = dossier `.cache/reference/26.3`). Les sections de sa sortie portent le numéro de la règle qu'elles étayent (`== R57 ...`). L'empreinte SHA-256 de la sortie complète est épinglée en annexe. **Base de comptage "LEP"** : sauf mention contraire, les occurrences sont comptées dans les dossiers `loot_table`, `enchantment` et `predicate` du pack vanilla ; les avancements, échanges et fournisseurs contextuels sont hors base. Une condition est comptée quand elle est en position de condition (valeur d'une clé `condition`, `requirements`, `terms` ou `term`, ou racine d'un fichier `predicate/`), un modificateur en position de modificateur (clés `modifier`, `on_pass`, `on_fail`, `functions`). Aucune table de données de référence n'est recopiée ici (AGENTS.md) : les tests lisent les fichiers extraits à l'exécution.

Convention de preuve : D valeur lue dans un fichier extrait ; C calcul reproductible sur ces fichiers ; M observé sur l'oracle (non utilisé) ; S documentation publique seule ou hypothèse. Chaque règle S porte un **comportement provisoire**.

## Règles

### Format des tables

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Une table est un fichier `data/<espace>/loot_table/<chemin>.json`, d'identifiant `<espace>:<chemin>`. Les 1447 tables vanilla se décomposent par préfixe : `blocks` 1201, `entities` 109, `chests` 58, `gameplay` 26, `shearing` 22, `archaeology` 6, `charged_creeper` 6, `spawners` 5, `dispensers` 3, `equipment` 3, `harvest` 3, et 1 chacun pour `barrels`, `pots`, `brush`, `carve`, `till`. Commande : section `R1`. | C |
| R2 | Clés de table observées : `type` (1447), `random_sequence` (1447), `pools` (1403 ; 44 tables sont sans `pools`), `modifier` (9). Le champ `type` est un identifiant du registre `context_key_set` ; valeurs : `block` 1201, `entity` 115, `chest` 68, `shearing` 22, `gift` 21, `archaeology` 6, `block_interact` 5, `fishing` 4, `equipment` 3, `entity_interact` 1, `barter` 1. Le dossier n'implique pas le type : `chest` 68 = `chests` 58 + `spawners` 5 + `dispensers` 3 + `barrels` 1 + `pots` 1 ; `entity` 115 = `entities` 109 + `charged_creeper` 6 ; `block_interact` 5 = `harvest` 3 + `carve` 1 + `till` 1 ; `entity_interact` = `brush` ; `gameplay` 26 = `gift` 21 + `fishing` 4 + `barter` 1. `type` absent vaut `generic` (S, documentation). Commande : section `R2`. | D (observé) ; S (défaut) |
| R3 | Le format 26.3 utilise `type` comme discriminant de chaque condition, modificateur, entrée et fournisseur (et non `condition` ou `function`), `condition` (singulier) pour une ou plusieurs conditions, `modifier` (singulier) pour un ou plusieurs modificateurs, `terms` pour `all_of` et `any_of`, `term` pour `inverted`. Un décodeur des formes anciennes (`conditions`, `functions`, `function`, `reference`) est hors périmètre. Source documentaire : changements de 26.3 snapshot 4. | D |
| R4a | `condition` (pool, entrée, modificateur) est un objet de condition inline (1389 occurrences dans les tables de butin) ou une chaîne identifiant un prédicat nommé (137) ; aucune occurrence n'est une liste. `terms` accepte des objets et des références. Références observées : `minecraft:tool/can_silk_touch` (162 usages) et `minecraft:tool/can_shear` (52). `modifier` accepte un objet inline (8 pools, 9 tables) ou une liste d'objets (2 pools) ; aucune référence à un modificateur nommé dans les données (le registre `item_modifier` n'a aucun fichier vanilla). Commande : sections `R4`. | D |
| R4b | Provisoire : une liste de conditions donnée à `condition` (forme absente des données) est un `all_of` ; une liste de modificateurs s'applique dans l'ordre. | S |
| R5a | Clés de pool observées sur les 1589 pools : `entries` (1589), `rolls` (1589), `condition` (1029), `modifier` (10). `bonus_rolls` n'apparaît dans aucune table vanilla. Nombre de pools par table : 0 (44), 1 (1272), 2 (97), 3 (21), 4 (8), 5 (3), 6 (1), 7 (1). Commande : section `R5-R8`. | D / C |
| R5b | `bonus_rolls` : la documentation le donne optionnel de défaut 0.0 (fournisseur de nombre, multiplié par la chance, arrondi vers le bas). Provisoire : défaut 0. Sonde PL7. | S |
| R6 | `rolls` : un nombre (1494 pools à 1, 6 pools à 3, 5 pools à 2, 2 pools à 4, 2 pools à 5) ou un fournisseur (80 pools, tous `uniform` (D) ; formes `{"type":"minecraft:uniform","min":a,"max":b}`). Provisoire : sans bonus, une valeur négative ou nulle donne zéro tirage ; avec un bonus non nul, voir R19b (H-rolls-a, b, c). | D (formes) ; S (reste) |
| R7 | Types d'entrée enregistrés (`loot_pool_entry_type`, 9) : `empty`, `item`, `loot_table`, `dynamic`, `tag`, `slots`, `alternatives`, `sequence`, `group`. Occurrences dans les données : `item` 2556, `alternatives` 87, `loot_table` 57, `empty` 39, `dynamic` 1, `tag` 1 ; `slots`, `sequence`, `group` : 0. | D |
| R8 | Champs communs des entrées : `type`, `condition` (223 `item`, 40 `loot_table`, 3 `alternatives`, 1 `dynamic`), `weight` (601 `item`, 9 `loot_table`, 35 `empty`), `quality` (3 `loot_table`, valeurs -2, 2, -1 ; toutes dans `gameplay/fishing`), `modifier` (1071 `item`, 2 `loot_table`). Défaut de `weight` : 1 ; défaut de `quality` : 0 (provisoire). Poids observés : de 2 à 699 (30 valeurs distinctes sur 645 poids). | D / S (défauts) |
| R9 | Champs propres : `item` : `name` (identifiant d'objet) ; `loot_table` : `value` (identifiant de table, ou table inline ; 57 usages) ; `dynamic` : `name` (identifiant de source dynamique, vu `minecraft:sherds`) ; `tag` : `items` (étiquette `#...`) et `expand` (booléen, vu `true` pour la table `entities/creeper`) ; `alternatives` : `children` ; `empty` : aucun. Provisoire pour `slots` : `slot_source` (documentation). | D / S |
| R10 | Une seule table vanilla porte des tables imbriquées inline dans `value` : `equipment/trial_chamber`, avec 3 entrées `loot_table` inline (section `R5-R8`). Les 17 fichiers des dossiers `trial_chambers` (chests, dispensers, pots) n'en contiennent aucune. Les entrées d'une table imbriquée ne font pas partie des entrées du pool parent ; le décodeur conserve la table imbriquée comme une valeur. 2741 noeuds d'entrée hors contenu interne (C, parcours `entries` puis `children` seulement). | D (présence) ; C (dénombrement) |

### Modèle de contexte (ADR 0004)

Le module `loot` déclare les interfaces de lecture et d'action ; `entity` et `game` les implémentent. Aucune dépendance de `loot` vers `entity`.

| Réf | Règle | Preuve |
|---|---|---|
| R11 | Un contexte de butin contient : un jeu de paramètres nommés (R12), la chance (flottant), une source d'aléa (R63), le niveau/monde en lecture seule. Il est construit par l'appelant (bloc détruit, entité tuée, coffre ouvert, commande) selon le `type` de la table (R14) et passé inchangé aux tables imbriquées. | S |
| R12 | Paramètres nommés (provisoire, noms de la documentation et des données) : `origin` (position), `this_entity`, `attacker` (entité à l'origine des dégâts), `direct_attacker` (entité qui a porté le coup, projectile par exemple), `attacking_player` (joueur crédité), `damage_source`, `tool` (pile), `block_state`, `block_entity`, `explosion_radius` (flottant), `enchantment_level`, `enchantment_active`, `target_entity`, `interacting_entity`. Cibles d'entité vues dans les données : `this`, `attacker`, `direct_attacker` (champ `entity` de `entity_properties`) ; la documentation ajoute `attacking_player`, `target_entity`, `interacting_entity`. Source de `copy_components` : `block_entity` (71 usages). | D (cibles vues) ; S (liste) |
| R13 | Registre `context_key_set` (31 jeux), dans l'ordre de ses identifiants numériques 0 à 30 : `empty`, `generic`, `chest`, `command`, `command_slot_source`, `command_compute_default`, `command_compute_position`, `command_compute_entity`, `selector`, `villager_trade`, `fishing`, `entity`, `equipment`, `archaeology`, `gift`, `barter`, `vault`, `advancement_reward`, `advancement_entity`, `advancement_location`, `block_use`, `block`, `shearing`, `entity_interact`, `block_interact`, `container_process`, `enchanted_damage`, `enchanted_item`, `enchanted_location`, `enchanted_entity`, `hit_block` (section `R13`). Les paramètres requis et optionnels de chaque jeu ne sont pas dans les données ; ils sont S (la page "Loot table" les renvoie à une page dédiée). | D (noms, ordre) ; S (contenu) |
| R14a | Types de condition et de modificateur effectivement présents dans les tables de chaque `type` de table (section `R14`, dossier `loot_table` seul) : `block` : conditions `all_of`, `any_of`, `entity_properties`, `inverted`, `location_check`, `match_block`, `match_tool`, `random_chance`, `survives_explosion`, `table_bonus`, modificateurs `apply_bonus`, `copy_components`, `copy_state`, `explosion_decay`, `limit_count`, `set_count` ; `entity` : conditions `all_of`, `any_of`, `damage_source_properties`, `entity_properties`, `inverted`, `killed_by_player`, `random_chance`, `random_chance_with_enchanted_bonus`, modificateurs `enchanted_count_increase`, `furnace_smelt`, `set_count`, `set_ominous_bottle_amplifier`, `set_potion` ; `chest` : conditions `inverted`, `location_check`, `random_chance`, 13 modificateurs dont `enchant_randomly`, `enchant_with_levels`, `exploration_map` ; `fishing` : `entity_properties`, `location_check`, modificateurs `enchant_with_levels`, `set_count`, `set_damage`, `set_potion` ; `gift` et `shearing` : `entity_properties`, `set_count` (et `set_potion` pour `gift`) ; `block_interact` : `match_block`, `set_count` ; `equipment` : `random_chance`, `set_components`, `set_enchantments` ; `archaeology` : `set_stew_effect` ; `barter` : `enchant_randomly`, `set_count`, `set_potion` ; `entity_interact` : aucun. | C |
| R14b | Paramètres que chaque jeu doit fournir, déduits par hypothèse de ce que chaque type de condition lit (non lus dans les données) : `block` : `origin`, `block_state` (`match_block`, `copy_state`), `tool` (`table_bonus`, `apply_bonus`, `match_tool`, références `tool/can_*`), `explosion_radius` (`survives_explosion`, `explosion_decay`), `block_entity` (`copy_components`), `this_entity` ; `entity` : `this_entity`, `attacker`, `direct_attacker` (cibles de `entity_properties`), `damage_source`, `attacking_player` (`killed_by_player`), niveau d'enchantement d'arme ; `chest` : `origin` (`location_check`) et, par hypothèse, `this_entity` pour la chance du joueur ; `fishing` : `origin`, `this_entity` (le flotteur : `in_open_water`) et, par hypothèse, `tool` ; `shearing` et `gift` : `this_entity` ; `archaeology`, `barter`, `equipment`, `entity_interact` : aucune lecture d'entité ; `block_interact` : `block_state`. Sonde PL18. | S |
| R15 | Comportement en l'absence d'un paramètre lu par une condition (documentation, S) : `damage_source_properties` et `location_check` (sans `origin`), `match_tool` (sans `tool`), `entity_scores` (sans entité), `enchantment_active_check` (hors `enchanted_location`) et `killed_by_player` (sans `attacking_player`) échouent ; `survives_explosion` sans `explosion_radius` réussit toujours ; `table_bonus` sans `tool` lit le niveau 0 ; `random_chance_with_enchanted_bonus` sans `attacker` lit le niveau 0. Le chargement de la table émet un avertissement si une condition lit un paramètre que le jeu du `type` ne fournit pas ; la table se charge quand même (S). | S |
| R16 | Ports de lecture : `loot` ne déclare que `EntityView` (ADR 0004). `BlockView` et `ItemView` ne sont pas retenus, car `loot` dépend directement de `world` et `item` : l'état de bloc, les étiquettes, l'entité de bloc, le biome, la visibilité du ciel et les piles (`tool`, équipement, résultats) se lisent par ces modules sans port. Surface minimale de `EntityView`, déduite des clés de prédicat d'entité de la base LEP (R57a, section `R57`) : type d'entité (étiquettes `#` comprises) ; drapeaux (`is_on_fire`, `is_on_ground`, `is_flying`...) ; équipement par slot (pile de `item`) ; composants lus sur l'entité (`sheep/color`, `mooshroom/variant`) ; position (le bloc à cette position se lit par `world`) ; véhicule ; vitesse horizontale et distance de chute (`movement`) ; tick périodique ; taille d'un cube mob ; capitaine de raid ; état de tonte d'un mouton ; crochet de pêche en eau libre ; sous-prédicat de joueur. Les clés `distance`, `stepping_on`, `type_specific/lightning` et `smokey` ne se lisent que dans des avancements (hors base LEP) et sont hors de cette surface. | C (clés) ; S (découpage) |
| R17 | Ports d'action (ADR 0004), nécessaires aux effets d'enchantement et non au tirage : `ActionSink` reçoit des intentions (infliger des dégâts, appliquer un effet de statut, enflammer, appliquer une impulsion, exploser, remplacer un bloc ou un disque, invoquer une entité, jouer un son, émettre des particules, exécuter une fonction, modifier des propriétés de bloc). Le tirage de butin pur ne les utilise pas. Les tests de `loot` utilisent des ports factices (liste d'intentions enregistrées). | S |
| R18 | La chance (`luck`) du contexte vaut : pour un coffre ouvert par un joueur, la valeur de l'attribut chance du joueur ; pour la pêche, chance du joueur plus bonus de pêche de la canne (`fishing_luck_bonus`, enchantement Luck of the Sea, voir `item-enchantments.md`) ; sinon 0. Le type numérique de la chance (flottant) n'est pas spécifié au-delà de R25. | S |

### Sélection d'un pool

| Réf | Règle | Preuve |
|---|---|---|
| R19a | Algorithme d'une table (provisoire, hypothèse H-flow) : pour chaque pool dans l'ordre, évaluer `condition` du pool ; un échec ignore le pool sans tirage d'aléa lié au pool. Sonde PL5. | S |
| R19b | Nombre de tirages N d'un pool : N = max(0, `rolls` + floor(`bonus_rolls` x chance)), puis plafonné à 4096 sans erreur (R74a). Le bonus peut être négatif : une chance négative **réduit** le nombre de tirages, et un total négatif est ramené à 0 ; aucune borne à 0 n'est appliquée au bonus seul. Un bonus NaN vaut 0. `floor` est l'arrondi vers le bas, pas la troncature (floor(-0.5) = -1). Exemples (provisoires) : `rolls` 3, `bonus_rolls` 1, chance -1 : 2 ; chance -5 : 0 ; `rolls` 3, `bonus_rolls` 0.5, chance -1 : 2 ; `rolls` -2, `bonus_rolls` 1, chance 5 : 3. La page "Loot table" (relue le 2026-10-29) donne `bonus_rolls` comme un fournisseur de tirages par niveau de chance, arrondi vers le bas après la multiplication, mais ne dit rien d'une chance négative, d'un `rolls` négatif, d'un total négatif ni d'un minimum à 0 : la réduction et le minimum à 0 sont donc une hypothèse. Hypothèses nommées : H-rolls-a (provisoire) : on additionne, puis on ramène à 0 le total négatif ; H-rolls-b : un `rolls` négatif donne zéro tirage quel que soit le bonus (le bonus ne s'ajoute qu'à un `rolls` positif ou nul) ; H-rolls-c : le jeu ne réduit pas le nombre de tirages en dessous de `rolls` (bonus négatif ramené à 0). Sonde PL7 (R5b, R6). | S |
| R19c | Répéter N fois : développer les entrées du pool, retirer celles dont la condition échoue, choisir au hasard pondéré (R25, R26), produire les piles de l'entrée choisie. Sonde PL5. | S |
| R19d | Les modificateurs de l'entrée s'appliquent à sa sortie, puis ceux du pool à chaque pile produite par le pool, puis ceux de la table à toutes les piles à la fin (R48). Sonde PL6. | S |
| R20 | Les entrées d'un pool sont toutes évaluées à chaque tirage (les conditions sont réévaluées à chaque tirage, et peuvent consommer de l'aléa : `random_chance`). Le tirage est avec remise : une entrée peut sortir plusieurs fois. | S |
| R21 | `alternatives` : seul le premier enfant dont la condition réussit contribue (testé dans l'ordre) ; aucune sortie si aucun ne réussit. `sequence` : les enfants contribuent dans l'ordre jusqu'au premier dont la condition échoue. `group` : tous les enfants contribuent. Les composites se développent en entrées simples avant le tirage ; ils ne portent pas de poids propre. | S |
| R22 | `tag` : (1) `expand: false` : une seule entrée qui produit tous les objets de l'étiquette (une pile par objet). (2) `expand: true` : un enfant simple par objet de l'étiquette (quantité 1, composants par défaut), de même poids que l'entrée, chacun portant la condition de l'entrée (page "Loot table", relue le 2026-10-29 : les enfants développés ont le même poids et les mêmes conditions que l'entrée). (3) **Évaluation de la condition d'une entrée `tag` développée**, deux hypothèses nommées. H-tag-cond-a : la condition est évaluée **une fois** par tirage, avant le développement ; les enfants sont alors tous candidats ou tous écartés, et un `random_chance` consomme un seul tirage. H-tag-cond-b : chaque enfant porte **sa propre copie** de la condition, évaluée **séparément** à chaque tirage (R20) ; un `random_chance` donne des résultats indépendants d'un enfant à l'autre et consomme un tirage par enfant, dans l'ordre de développement de l'étiquette (ordre S). Provisoire : H-tag-cond-b, parce que c'est la lecture littérale de "mêmes conditions pour chaque enfant" et que R20 réévalue la condition de chaque candidat. Observable : avec une étiquette de 2 objets, des enfants de même poids et `random_chance` 0.5, un tirage unique ne produit rien avec la probabilité 0.5 sous H-tag-cond-a et 0.25 sous H-tag-cond-b (V-L15, sonde PL8). (4) `expand` absent : défaut non documenté (provisoire `false`). (5) Modificateurs d'une entrée `tag` : la page "Loot table" signale un défaut connu (les modificateurs de l'entrée ne s'appliquent pas aux enfants développés) alors que sa section des entrées composites dit que `modifier` s'applique à toutes les piles produites ; H-tag-mod-a : ils s'appliquent aux enfants ; H-tag-mod-b : ils ne s'appliquent pas (lecture du défaut connu). Provisoire : H-tag-mod-a. Sonde PL8. | S |
| R23 | `loot_table` : évalue la table référencée (ou inline) avec le même contexte et la même source d'aléa (pas de nouvelle séquence, voir R64) et renvoie ses piles ; pas de récursion directe (une table qui se référence elle-même est une erreur de chargement ; page "Loot table" relue le 2026-10-29 : l'appel récursif est interdit). Les cycles indirects (A vers B vers A) sont traités par R76 et R77. | S |
| R24 | `empty` : candidat qui ne produit rien mais qui compte dans le total des poids. `dynamic` : produit des piles fournies par l'émetteur du contexte sous le nom donné (`minecraft:sherds` : les tessons du pot décoré cassé ; `minecraft:contents` : le contenu d'un conteneur) ; sans émetteur il ne produit rien. `item` : une pile de `name` quantité 1 avec les composants par défaut. `slots` : S, voir R71. | D (types) ; S (comportement) |
| R25 | Poids effectif d'une entrée simple : le poids de l'entrée augmenté du produit de sa qualité par la chance du contexte, tronqué vers le bas et borné à 0 (documentation publique, page "Loot table"). Les entrées de poids effectif 0 ne sont pas tirées. La précision numérique du calcul n'est pas spécifiée ; elle n'est observable qu'aux frontières entières (sondes PL1, PL2). | S |
| R26 | Tirage entre candidats : chaque candidat est choisi avec une probabilité proportionnelle à son poids effectif (R25). Sans candidat, rien n'est produit. Avec un seul candidat, le résultat ne dépend pas de l'aléa ; savoir si l'aléa est alors consommé n'est observable que par le décalage des tirages suivants à graine fixée. Hypothèse H-single : il ne l'est pas. La correspondance exacte entre la suite de nombres de la source (R63) et le candidat choisi n'est pas spécifiée ici : elle se retrouve par comparaison avec l'oracle (sonde PL5), pas par prescription. | S |
| R27 | Cas de la pêche, données (`gameplay/fishing`, section `R27`) : trois entrées `loot_table`, poids/qualité 10/-2 (déchets), 5/+2 (trésor, conditionné à `in_open_water`), 85/-1 (poissons). Poissons de `gameplay/fishing/fish` : poids 60, 25, 2, 13 (morue, saumon, tropical, globe). Valeurs attendues sous R25 et R26 (S) avec la chance entière L : poids effectifs 10-2L, 5+2L, 85-L (planchers à 0). En eau libre : L=0 -> 10, 5, 85 (total 100) ; L=1 -> 8, 7, 84 (99) ; L=2 -> 6, 9, 83 (98) ; L=3 -> 4, 11, 82 (97). Hors eau libre à L=0 : 10 et 85 (total 95). | D (poids, qualités) ; S (valeurs effectives) |
| R28 | Cas du puits du désert (`archaeology/desert_well`, section `R28`) : 6 entrées, deux tessons de poids 2, quatre autres de poids 1 (brique, émeraude, bâton, soupe suspecte), total 8. Cas des ruines (`trail_ruins_rare`) : 12 entrées de poids égal. Valeurs attendues sous R26 (S) : 25 pour cent par tesson et 12,5 pour cent par autre entrée ; 1/12 chacune pour les ruines. | D (poids) ; S (probabilités) |

### Conditions

Les conditions sont des données `{"type": ..., ...}` ou des références. 20 types enregistrés ; occurrences dans la base LEP (D, section des conditions de `loot_stats.py`, qui détaille loot_table / enchantment / predicate ; `match_block` vaut 295 hors `predicate/` et `match_tool` 4) et champs observés :

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
| `match_block` | 296 | `blocks`, `state` |
| `match_tool` | 6 | `predicate` |
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
| R33 | `survives_explosion` : sans rayon d'explosion dans le contexte, vrai sans tirage ; avec un rayon r > 0, vrai avec la probabilité 1/r (un flottant tiré dans [0, 1[ inférieur à 1/r). Source : page "Predicate", relue le 2026-10-29 (réussite avec la probabilité 1/rayon, toujours vrai sans rayon fourni). Corroboration : la page "Explosion" donne 1/puissance comme probabilité de lâcher pour un bloc détruit par une explosion autre que la dynamite (creeper de puissance 3 : 1/3 ; creeper chargé de puissance 6 : 1/6) ; le "rayon" du contexte est donc supposé égal à la puissance de l'explosion (H-rayon, sonde PL11). La page "Explosion" donne un lâcher certain pour la dynamite par défaut, ce qui correspond à la règle de jeu de décroissance de la dynamite désactivée : hypothèse H-decay-regle, provisoire : quand la règle de décroissance de l'explosion est fausse, le contexte ne contient pas de rayon (R41 et `survives_explosion` sont alors sans effet). Utilisé par 914 entrées/pools de blocs. Sonde PL11. | S |
| R34 | `match_block` : `blocks` (identifiant, étiquette ou liste) et `state` (table propriété -> valeur chaîne, correspondance exacte de chaque propriété listée), plus (documentation) `nbt`, `components`, `predicates` optionnels qui exigent une entité de bloc. Vu : `{"blocks":"minecraft:wheat","state":{"age":"7"}}`. Sans état de bloc dans le contexte : échec (provisoire). | D (forme) ; S (sens) |
| R35 | `match_tool` : `predicate` est un prédicat d'objet (R58) appliqué à la pile `tool` ; sans `tool` : échec. `entity_properties` : `entity` (R12) et `predicate` (prédicat d'entité, R57) ; entité absente : échec (provisoire). `location_check` : `predicate` (prédicat de position, R59), `offsetX/Y/Z` optionnels (vu `offsetY`) ; sans `origin` : échec. `damage_source_properties` : `predicate` (R60). `killed_by_player` : vrai si `attacking_player` est présent. | D (formes) ; S (sens) |
| R36 | `weather_check` : `raining` et `thundering` optionnels ; `raining: true` réussit si pluie ou orage, `thundering: true` seulement si orage (documentation). Vu dans les enchantements : `{"thundering": true}`. `time_check` : `clock` (identifiant d'une horloge du monde), `value` (nombre ou intervalle), `period` (entier optionnel) ; non utilisé dans les données. Quel temps est comparé : la page "Predicate" (relue le 2026-10-29) est ambiguë, elle écrit une somme de 24000 x nombre de jours et d'heure du jour, appelée aussi heure du jour courante, et précise que `period` 24000 ramène la valeur à l'heure du jour. H-time-a (lecture littérale) : valeur comparée = temps cumulé de l'horloge `clock` (24000 x jours + heure du jour), réduit modulo `period` si présent ; H-time-b (provisoire) : valeur comparée = heure du jour de l'horloge `clock` (de 0 à 23999), réduite modulo `period` si présent. Les deux lectures ne diffèrent que sans `period`, ou avec une `period` qui ne divise pas 24000. Sonde PL28. | D (forme vue) ; S |
| R37 | `int_value_check`, `float_value_check` : `value` (fournisseur) et `test` (intervalle ou nombre) ; `entity_scores` : `entity`, `scores` ; `enchantment_active_check` : `active` ; `environment_attribute_check` : forme inconnue (ajout récent). Aucun usage de `int_value_check`, `float_value_check`, `entity_scores`, `environment_attribute_check`, `time_check` dans les données. | S |

### Modificateurs

42 types enregistrés ; occurrences dans la base LEP (D, section des modificateurs de `loot_stats.py`) et champs observés :

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
| R40 | `limit_count` : `limit` entier ou `{min?, max?}` (fournisseurs) ; borne la quantité de chaque pile. Vu : `{"min":0}`, `{"max":9}`, `{"max":4,"min":1}` ; la forme entière n'apparaît pas dans les données. Page "Item modifier", relue le 2026-10-29 : un entier fixe une limite "exacte", un objet donne un minimum et un maximum facultatifs ; elle ne dit rien du cas `min` > `max`. **Cas `min` > `max`** (jamais vu dans les données), hypothèses nommées : H-limit-a : la quantité est d'abord relevée à `min`, puis ramenée à `max` ; le résultat est toujours `max` ; H-limit-b : la quantité est d'abord ramenée à `max`, puis relevée à `min` ; le résultat est toujours `min` ; H-limit-c : la table est refusée au chargement. Provisoire : H-limit-a, sans refus ; la quantité résultante 0 supprime la pile (R49). **Forme entière** : H-limit-int-a : `min` = `max` = l'entier (limite exacte, lecture de la page) ; H-limit-int-b : `max` seul. Provisoire : H-limit-int-a. Sonde PL23. | D (formes vues) ; S (reste) |
| R41 | `explosion_decay` : sans rayon d'explosion dans le contexte, pile inchangée (sans tirage). Avec un rayon r > 0, chaque unité de la pile est traitée séparément, un tirage flottant dans [0, 1[ par unité, dans l'ordre ; la quantité résultante est le nombre d'unités conservées (0 supprime la pile, R49). 190 usages. **Divergence de la documentation, à lever par sonde.** La page "Item modifier" (relue le 2026-10-29) dit que chaque unité a la chance 1/rayon d'être **perdue** (donc conservée avec 1 - 1/r), alors que `survives_explosion` (R33, page "Predicate") réussit avec la probabilité 1/r et que la page "Explosion" donne 1/puissance comme probabilité de lâcher. Pour une pile d'une unité au rayon 4, la lecture littérale de la page "Item modifier" conserve l'unité avec la probabilité 0.75, R33 avec 0.25 : les deux lectures littérales se contredisent. Hypothèses nommées : H-decay-a (lecture littérale de "Item modifier") : l'unité est perdue si le tirage est inférieur à 1/r ; H-decay-b : l'unité est conservée si le tirage est inférieur à 1/r, comme `survives_explosion`. Provisoire : H-decay-b. Ce choix n'est pas une lecture de la page "Item modifier" : c'est un choix de l'analyste, motivé par la cohérence avec R33 et avec la page "Explosion" (un bloc dont le butin est une unité sous `explosion_decay` se comporterait sinon à l'inverse d'un bloc sous `survives_explosion`) ; il est à renverser si PL11 donne H-decay-a. Vecteur V-L14 (C sur S). Sonde PL11. | S |
| R42 | `apply_bonus` : `enchantment` (vu `minecraft:fortune`), `formula`, `parameters`, niveau lu sur `tool`. Formules vues : `minecraft:ore_drops` (17, sans paramètre), `minecraft:uniform_bonus_count` (11, `{"bonusMultiplier": 1 ou 2}`), `minecraft:binomial_with_bonus_count` (4, `{"extra": 3, "probability": 0.5714286}`). Sémantique (documentation publique, page "Item modifier", fonction `apply_bonus`) : `ore_drops` : quantité multipliée par max(1, entier tiré dans [0, niveau + 2[) ; `uniform_bonus_count` : ajoute un entier tiré dans [0, niveau x bonusMultiplier] ; `binomial_with_bonus_count` : ajoute binomiale(n = niveau + extra, p = probability). La page ne dit rien du comportement au niveau 0 ni de la consommation de l'aléa. Les tirages exacts (bornes inclusives ou exclusives, nombre de tirages) sont S ; provisoire : `ore_drops` = quantité x max(1, nextInt(niveau + 2)), `uniform_bonus_count` = quantité + nextInt(niveau x mult + 1), `binomial` = quantité + nombre de succès de (niveau + extra) tirages flottants < p. **Niveau 0** (outil sans l'enchantement, ou sans outil, R15) : la formule est évaluée pour tout niveau, sans cas particulier. H-bonus0-a (provisoire) : `ore_drops` consomme un tirage `nextInt(2)` (valeur 0 ou 1, `max(1, .)` = 1, quantité inchangée) ; `uniform_bonus_count` rend un tirage borné par 1, cas `min == max` traité comme `core-random.md` R12 et R33 (H-R33b : aucun tirage ; H-R33c : un tirage `nextInt(1)`) ; `binomial` consomme `extra` tirages flottants. H-bonus0-b : au niveau 0 le modificateur ne fait rien et ne consomme aucun tirage. La quantité finale est identique sous les deux hypothèses pour `ore_drops` : l'écart n'est observable que par le décalage des tirages suivants de la même source (conteneur à `LootTableSeed`, PL3 variante b). Sondes PL3, PL4. | D (paramètres) ; S (formules) |
| R43 | `enchanted_count_increase` : `count` (fournisseur flottant), `enchantment` (vu `minecraft:looting`), `limit` (entier, 0 = pas de limite, vu 1 sur 3 usages) : pour chaque niveau de l'enchantement, un tirage de `count` ; le total des tirages (une somme) est arrondi vers le bas (mode d'arrondi non documenté ; provisoire : vers le bas), puis ajouté à la quantité de la pile (valeurs extrêmes : R74i) ; le niveau est celui de l'arme de l'attaquant (0 sans attaquant). Le nombre de tirages est donc égal au niveau, et au niveau 0 aucun tirage n'est consommé et la pile est inchangée (H-eci0-a, provisoire). Hypothèses nommées : H-eci-a (provisoire) : un tirage par niveau, somme puis arrondi ; H-eci-b (non retenue) : la page "Item modifier" (relue le 2026-10-29) parle d'un nombre fractionnaire arrondi après multiplication par le niveau, lecture qui revient à un seul tirage de `count` pour tous les niveaux ; la page ne tranche pas entre les deux. Niveau 0, autre hypothèse : H-eci0-b, un tirage est consommé puis son résultat écarté. Vu : `uniform 0.0 à 1.0` (79), `uniform 1.0 à 2.0` (1). Sonde PL12 (variante niveau 0 avec conteneur à graine : PL28). | D (formes) ; S (sens) |
| R44a | `set_potion` : `id` identifiant de potion ; `empty` retire. | D (champ) ; S (sens) |
| R44b | `set_stew_effect` : `effects` liste de {`type`, `duration` fournisseur}. | D (champs) ; S (sens) |
| R44c | `set_ominous_bottle_amplifier` : `amplifier` fournisseur. | D (champ) ; S (sens) |
| R44d | `set_instrument` : `options` étiquette, identifiant ou liste, tirage uniforme. | D (champ) ; S (sens) |
| R44e | `set_name` : `name` texte, `target` `item_name` ou `custom_name`. | D (champs) ; S (sens) |
| R44f | `set_damage` : `damage` fournisseur flottant, **fraction** de durabilité restante (1.0 = neuf ; vu uniform 0.15 à 0.8), `add` défaut false. | D (champs) ; S (sens) |
| R44g | `set_enchantments` : `enchantments` table identifiant -> fournisseur de niveau, borné à 255 ; `add` défaut false. | D (champs) ; S (sens) |
| R44h | `set_components` : table de composants, clé `!id` pour retirer (vu : `minecraft:trim`). | D (champs) ; S (sens) |
| R44i | `copy_state` : `block`, `properties` ; copie les propriétés listées de l'état vers le composant `block_state`. | D (champs) ; S (sens) |
| R44j | `copy_components` : `source` `block_entity`, `include` liste de composants ; `exclude` optionnel. | D (champs) ; S (sens) |
| R44k | `furnace_smelt` : applique la recette de fonte à la pile sans changer la quantité ; sans recette, pile inchangée (provisoire). | D (champ) ; S (sens) |
| R45 | `enchant_randomly` : `options` (étiquette, identifiant ou liste), `only_compatible` (défaut `true`), `include_additional_cost_component` (défaut `false`) : choisit un enchantement uniformément parmi les candidats puis un niveau aléatoire ; un livre devient livre enchanté. `enchant_with_levels` : `levels` (fournisseur entier), `options` (optionnel : tous les enchantements si absent), `include_additional_cost_component` (défaut `false`) : enchante comme une table d'enchantement au niveau donné (`item-enchantments.md` R36 à R40). Vu : `enchant_randomly` 52 (`#minecraft:on_random_loot` 43), `enchant_with_levels` 34 (`levels` constante 30 ou uniforme 20 à 39, 5 à 15, 30 à 50). Sondes PL13, PE1. | D (usage) ; S (algorithme) |
| R46 | `exploration_map` : `destination` (étiquette de structure), `decoration` (type de marqueur), `zoom` (défaut 2), `search_radius` (défaut 50), `skip_existing_chunks` (défaut true) : crée une carte pointant vers la structure la plus proche (dépend de `/locate`, worldgen M4) ; 18 usages. `filtered` : `item_filter` (prédicat d'objet), `on_pass`, `on_fail` (modificateurs ; l'absent laisse la pile) ; vu : `item_filter` `{"predicates":{"minecraft:map_id":{}}}` et `on_fail` `{"type":"minecraft:discard"}` : supprime la carte si elle n'a pas pu être créée. `discard` : supprime la pile (18 usages). | D (formes) ; S (sens) |
| R47 | Modificateurs enregistrés sans usage dans les tables de butin, enchantements et prédicats (`set_random_dyes` n'apparaît que dans les échanges de villageois) : `copy_custom_data`, `copy_name`, `fill_player_head`, `modify_contents`, `sequence`, `set_attributes`, `set_banner_pattern`, `set_book_cover`, `set_contents`, `set_custom_data`, `set_custom_model_data`, `set_firework_explosion`, `set_fireworks`, `set_item`, `set_lore`, `set_loot_table`, `set_random_dyes`, `set_random_potion`, `set_writable_book_pages`, `set_written_book_pages`, `toggle_tooltips` (21). Leurs champs, d'après la documentation (S, sans comportement détaillé) : `set_item` {`item`} ; `set_custom_data` {`tag`} ; `copy_custom_data` {`source`, `ops`} ; `set_contents` {`entries`, `component`} ; `modify_contents` {`component`, `modifier`} ; `set_loot_table` {`loot_table_id`, `seed?`} ; `set_lore` {`lore`, `mode`, `offset?`, `size?`} ; `set_attributes` {`modifiers`, `replace?`} ; `set_banner_pattern` {`patterns`, `append`} ; `set_book_cover` {`author?`, `generation?`, `title?`} ; `set_custom_model_data` {`floats`, `flags`, `strings`, `colors`} ; `set_fireworks` {`explosions?`, `flight_duration?`} ; `set_firework_explosion` {`shape`, `colors`, `fade_colors`, `has_trail`, `has_twinkle`} ; `set_random_dyes` ; `set_random_potion` {`options?`} ; `set_writable_book_pages`, `set_written_book_pages` {`pages`, `mode`, `offset?`, `size?`} ; `toggle_tooltips` {`toggles`} ; `copy_name` {`source`} ; `fill_player_head` {`entity`} ; `sequence` {`functions`}. **Divergence à noter** : la documentation donne `id` pour `set_random_dyes`, alors que la donnée vanilla (échange `emerald_dyed_leather_chestplate`) utilise `number_of_dyes` avec un fournisseur `add` d'entrées `[1, binomial(n=2, p=0.75)]`. Les données priment : champ `number_of_dyes`. Ces 21 types sont décodés (R3). Comportement provisoire jusqu'à PL21 : le modificateur est conservé au décodage, appliqué comme l'identité (pile inchangée), et un avertissement "non implémenté" est émis une fois par type. PL21 couvre ces 21 types (une table par type, relecture de l'objet au sol). | D (divergence, champs vus) ; S (le reste) |
| R48 | Ordre d'application (hypothèse H-order, provisoire) : modificateurs de l'entrée (dans l'ordre de la liste), puis modificateurs du pool, puis modificateurs de la table. Pour `loot_table` imbriquée : la table imbriquée applique ses modificateurs avant de rendre les piles à l'entrée parente. Sonde PL6. | S |
| R49 | Une pile rendue par un modificateur peut être vide (quantité 0, `discard`, `limit_count` max 0) : elle est supprimée de la sortie. Un modificateur appliqué à une pile déjà vide ne fait rien. Provisoire. | S |

### Fournisseurs de nombres

| Réf | Règle | Preuve |
|---|---|---|
| R50 | Fournisseurs entiers contextuels (`context_int_provider_type`, 23) : `constant`, `uniform`, `binomial`, `weighted_list`, `conditional`, `number_dispatcher`, `score`, `storage`, `environment_attribute`, `from_float`, `abs`, `avg`, `add`, `sub`, `mul`, `div`, `mod`, `floor_div`, `floor_mod`, `negate`, `pow`, `min`, `max`. Fournisseurs flottants (`context_float_provider_type`, 28) : `constant`, `uniform`, `weighted_list`, `conditional`, `number_dispatcher`, `storage`, `environment_attribute`, `enchantment_level`, `from_int`, `length`, `abs`, `avg`, `ceil`, `floor`, `round`, `truncate`, `add`, `sub`, `mul`, `div`, `mod`, `negate`, `pow`, `sqrt`, `sin`, `cos`, `min`, `max`. Un nombre nu (`1`, `2.0`) est un `constant`. Usage dans les tables de butin : nombres nus, `uniform`, `binomial`. Usage dans les données hors butin : `div` (durées de carburant), `conditional` (vitesse de cuisson), `number_dispatcher` et `weighted_list` (compostage), `add` (échange). Évaluation : ces fournisseurs et les prédicats de bloc qu'ils appellent (`match_block`) sont évalués par `loot`, y compris pour les besoins de `item` (durées de combustible, vitesse de cuisson, compostage : `item-recipes.md` DC1), avec un contexte de bloc réduit à l'état du bloc de la machine. | D (types, usages) ; S (contexte) |
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
| R57a | Prédicat d'entité (`entity_properties.predicate`) : objet dont les clés sont des identifiants du registre `entity_sub_predicate_type` (25 types : `entity_type`, `location`, `stepping_on`, `movement_affected_by`, `distance`, `movement`, `effects`, `nbt`, `flags`, `equipment`, `periodic_tick`, `vehicle`, `passenger`, `targeted_entity`, `team`, `slots`, `components`, `predicates`, `entity_tags`, `type_specific/{lightning, fishing_hook, player, cube_mob, raider, sheep}`). Occurrences de clé dans la base LEP, tous niveaux d'imbrication confondus (section `R57`, qui détaille loot_table / enchantment / predicate et, pour information, advancement) : `entity_type` 25, `components` 40 (`{"minecraft:sheep/color":"white"}`), `flags` 30 (`is_on_fire`, `is_on_ground`, `is_flying`), `type_specific/sheep` 32 (`sheared`), `equipment` 19 (slot -> prédicat d'objet), `vehicle` 6, `movement_affected_by` 5, `type_specific/player` 3, `movement` 3 (`horizontal_speed` intervalle), `type_specific/cube_mob` 2 (`size`), `periodic_tick` 2 (entier), `location` 1, `type_specific/raider` 1, `type_specific/fishing_hook` 1 (`in_open_water`). `distance`, `stepping_on` et `type_specific/lightning` valent 0 dans la base LEP (3, 1 et 1 sous `advancement/`). | D |
| R57b | Toutes les clés présentes d'un prédicat d'entité doivent être vraies (ET). Provisoire. | S |
| R58 | Prédicat d'objet (`match_tool.predicate`, `filtered.item_filter`, ingrédients d'alchimie) : clés vues dans la base LEP (section `R59-R60`) `items` (identifiant, étiquette ou liste ; 5 dans `match_tool`), `predicates` (table identifiant de type -> valeur : `minecraft:enchantments` 20, liste de `{enchantments, levels?}` ; `minecraft:map_id` 18, valeur `{}`). Observation D : `predicates` accepte `minecraft:map_id`, qui n'est pas dans les 15 types du registre `data_component_predicate_type` (`damage`, `enchantments`, `stored_enchantments`, `potion_contents`, `custom_data`, `container`, `bundle_contents`, `firework_explosion`, `fireworks`, `writable_book_content`, `written_book_content`, `attribute_modifiers`, `trim`, `jukebox_playable`, `villager/variant`), avec la valeur `{}` ; les échanges de villageois (hors base) font de même avec `minecraft:dyed_color`. Sémantique S, hypothèse "le composant est présent". `components` (valeurs exactes) existe aussi (documentation) et `count` (intervalle). Un prédicat `{"items": "<id>"}` seul compare l'objet. Sondes PL15, PI11. | D / S |
| R59 | Prédicat de position, clés vues dans la base LEP (section `R59-R60`) : sous `location_check.predicate` (les clés y sont directement, sans enveloppe) `block` 5 (`{blocks, state?}`), `biomes` 10 (identifiant, étiquette ou liste), `can_see_sky` 1 (booléen) ; sous la clé `minecraft:location` d'un prédicat d'entité (enchantements, enveloppe distincte) `can_see_sky` 1. `smokey` (feu de camp) n'apparaît que sous `advancement/` et est hors base. | D |
| R60 | Prédicat de source de dégâts : clés vues `tags` (liste de `{id: étiquette, expected: booléen}`, 8), `source_entity` (prédicat d'entité, 6), `direct_entity` (1), `is_direct` (booléen, 2). | D |
| R61a | Prédicats nommés (registre `predicate`, 3 fichiers, section `R61`) : `tool/can_silk_touch` (`match_tool` avec `predicates.minecraft:enchantments` `[{enchantments:"minecraft:silk_touch", levels:{min:1}}]`, 162 références), `tool/can_shear` (`match_tool` avec `items:"minecraft:shears"`, 52 références), `block/fast_cooking` (`match_block` sur `smoker`, `blast_furnace`, utilisé par les fournisseurs de cuisson et non par les tables de butin). | D |
| R61b | Une référence se résout au chargement ; une référence inconnue est une erreur de chargement de la table. Provisoire. Les références cycliques et les références de fournisseurs nommés sont traitées par R76 et R77. | S |
| R62 | Prédicat de bloc des effets d'enchantement (registre `block_predicate_type`, 16 types dont `matching_blocks`, `matching_block_tag`, `matching_fluids`, `unobstructed`, `all_of`, `any_of`, `not`, `true`, `solid`, `replaceable`, `would_survive`, `has_sturdy_face`, `inside_world_bounds`, `height_range`, `volume_match`, `matching_biomes`) : décrit dans `item-enchantments.md`. | D (registre) |

### Aléa et déterminisme

Les règles de cette section prolongent `core-random.md` R34 et R35, qui renvoient à cette spec.

| Réf | Règle | Preuve |
|---|---|---|
| R63 | Choix de la source d'aléa d'un tirage (priorité décroissante) : (1) une graine explicite **non nulle** (paramètre de la commande ou `LootTableSeed` du conteneur) donne un générateur à état 48 bits initialisé avec cette graine ; la graine 0 signifie "absente" ; (2) sinon la séquence nommée de la table (`random_sequence`), dont la source est le générateur 128 bits de `core-random` R14 ; (3) sinon la source du niveau. Réduire (1) et (2) à un seul générateur change la correspondance graine -> contenu. Sondes PL5, PL16. | S |
| R64 | Les 1447 tables vanilla déclarent toutes un `random_sequence` (valeur = `minecraft:` + chemin de la table, ex. `minecraft:blocks/stone`) ; les 68 `trade_set` aussi. Une table imbriquée (`loot_table`) n'ouvre pas sa propre séquence : le contexte et la source de l'appelant sont passés (provisoire). | D (présence) ; S (imbrication) |
| R65 | La séquence nommée est initialisée à la première utilisation à partir de la graine du monde, du sel et de l'identifiant de la séquence (réglages R30 de `core-random`) et persiste (`random_sequences.dat`). Les tirages d'une table avec séquence avancent l'état persistant de cette séquence. Deux tables partageant un `random_sequence` partagent l'état : le contenu dépend de l'ordre d'appel. | S |
| R66 | Ordre de consommation de l'aléa (hypothèse H-draw, base des vecteurs dorés) : la source avance à chaque décision aléatoire, dans l'ordre d'évaluation de R19 (`rolls` aléatoires, conditions aléatoires évaluées pendant le développement des entrées, sélection pondérée, puis modificateurs de la pile choisie dans l'ordre). Seul l'oracle arbitre cet ordre : sonde PL5 (empreinte du contenu en fonction de la graine). | S |
| R67 | Déterminisme des commandes : `/loot` sans graine utilise la source du niveau ou la séquence de la table (selon R63) : deux appels consécutifs diffèrent. Les comparaisons statistiques employées par M6 sont des tests du khi-deux (voir Plan de tests). Les tests bit à bit n'existent que pour les conteneurs à `LootTableSeed` non nul. | S |

### Conteneurs, commandes et déballage

| Réf | Règle | Preuve |
|---|---|---|
| R68 | Un conteneur portant `LootTable` (String) et éventuellement `LootTableSeed` (Long) est "déballé" à son ouverture, à sa destruction ou à l'accès d'un entonnoir ; ces champs sont alors supprimés du conteneur et `Items` est rempli. `LootTableSeed` ne peut exister sans `LootTable` ; une valeur 0 ou absente signifie graine aléatoire. Même graine et même table donnent le même contenu. | S (documentation) |
| R69 | Le contexte de déballage est de type `chest` : `origin` = centre du bloc, `this_entity` = le joueur qui ouvre (avec sa chance) si présent. Remplacer ce contexte par "graine du monde + position entière + identifiant de coffre" est faux. | S |
| R70 | Placement des piles dans les slots d'un conteneur déballé. Comportement provisoire : les piles produites (éventuellement fractionnées par R54) occupent des slots distincts du conteneur, sans perte ni duplication tant que le nombre de piles ne dépasse pas la taille du conteneur ; le choix des slots est aléatoire et uniforme parmi les slots libres. L'algorithme exact (fractionnement des piles de plusieurs unités, mélange, ordre de consommation) n'est pas spécifié : le test L2 de placement est statistique (multi-ensemble de piles exact, répartition des slots par khi-deux), pas une égalité slot par slot, jusqu'à ce que PL5 permette de retrouver l'algorithme. Sonde PL5 (200 graines, enregistrement slot par slot). | S |
| R71 | `slots` (type d'entrée) et `slot_source_type` (6 types : `contents`, `empty`, `filtered`, `group`, `limit_slots`, `slot_range`) lisent des piles dans des slots d'une source (`this`, `attacker`...) ; commandes `item replace/fill/override/modify` (D `commands.json`) utilisent `slot_source` et `loot_modifier`. Sémantique et formes : S, sonde PL16 b. | D (syntaxe) ; S (reste) |
| R72 | Commande `/loot` (D `commands.json`) : cibles `give <joueurs>`, `insert <pos>`, `replace block <pos> <slot> [count]`, `replace entity <entités> <slot> [count]`, `spawn <vec3>` ; sources `loot <table>`, `kill <entité>`, `mine <pos> [mainhand]`, `mine <pos> [offhand]`, `mine <pos> [<outil>]`, `fish <table> <pos>` avec les mêmes options (`mainhand`, `offhand` ou un outil). La source `loot` utilise un contexte de type `command` ; `kill`, `mine` et `fish` utilisent les contextes d'entité, de bloc et de pêche. Provisoire pour la correspondance contexte/source. | D (syntaxe) ; S (contextes) |
| R73 | Le butin d'un bloc détruit dépend aussi des règles de jeu `block_drops`, `entity_drops`, `block_explosion_drop_decay` (D registre `game_rule`, défauts true). Hors périmètre ici, rappelées pour les scénarios. Le registre `game_rule` et la commande `gamerule` (D, `commands.json`) contiennent aussi `mob_explosion_drop_decay` et `tnt_explosion_drop_decay` ; les défauts ne figurent pas dans ces fichiers. La page "Explosion" (relue le 2026-10-29) dit que seule la règle de la dynamite est fausse par défaut : cette valeur est S, et elle contredit "défauts true" ci-dessus pour `tnt_explosion_drop_decay` si cette valeur la couvrait. Les sondes de R33 et R41 fixent donc les trois règles explicitement. | D (existence) ; S (défauts) |

### Robustesse du chargeur et du tirage

Les packs de données viennent de l'extérieur : une table malformée ou hostile ne doit ni épuiser la mémoire, ni boucler, ni faire déborder la pile d'appels. Les plafonds de R74 sont des budgets du projet (décisions de conception, `S-arch`) : ils ne décrivent pas le jeu et aucune sonde ne les lève. Ce que fait le jeu au-delà de ces valeurs est une question de comportement du jeu : elle est posée par R74g (S).

| Réf | Règle | Preuve |
|---|---|---|
| R74a | Le nombre total de tirages d'un pool par évaluation, `rolls` + floor(`bonus_rolls` x chance) (R19b), est borné à 4096. Seul `rolls` lui-même au-dessus de 4096 est refusé : une valeur constante est refusée au chargement, un fournisseur dynamique qui produit une valeur au-dessus fait échouer le tirage (R74g). Un total qui dépasse 4096 à cause du bonus est ramené à 4096 sans erreur, parce que la chance est une entrée du contexte de jeu et non une donnée du pack. | S-arch |
| R74b | Un fournisseur `binomial` (entier ou flottant, R53) accepte au plus 100000 essais (`n`). Un `n` constant au-dessus est refusé au chargement avec un diagnostic de valeur hors plage ; un `n` dynamique au-dessus fait échouer le tirage (R74g). Les essais d'une binomiale comptent dans le plafond d'unités de travail de R74e : n essais valent n unités. | S-arch |
| R74c | Le poids effectif d'une entrée (R25) est plafonné à 2^20 avant la somme des poids, ce qui borne chaque terme de la somme. Un poids constant au-dessus de 2^20 est ramené à 2^20 avec un avertissement au chargement, sans erreur ; le poids effectif calculé au tirage (qualité x chance) est plafonné de même. Les poids des données vanilla vont de 2 à 699 (R8) : le plafond n'a aucun effet sur elles. | S-arch |
| R74d | Profondeur d'imbrication maximale de 255 niveaux, appliquée séparément à l'arbre des entrées (R21), à celui des conditions (`all_of`, `any_of`, `inverted`), à celui des fournisseurs de nombres et à la chaîne des tables imbriquées (R23) : 255 niveaux sont acceptés, le 256e est refusé. La limite de profondeur du lecteur JSON de la couche de données est la même (255) : un fichier plus profond est refusé dès sa lecture. Un dépassement par la chaîne de tables imbriquées apparaît à l'évaluation (R74g). **Chaîne de références nommées** : la profondeur d'une chaîne de références nommées (prédicats nommés, fournisseurs nommés entiers ou flottants, chaînes qui passent d'une famille à l'autre) est limitée à 64, comme à l'évaluation. Le premier lien compte 1 : une chaîne de 64 liens est acceptée, une chaîne de 65 est refusée. Une chaîne plus longue est refusée **au chargement** : seule la table qui la porte est refusée, avec un diagnostic de profondeur de référence ; les éléments nommés qui débutent une telle chaîne ne sont pas écartés, et évalués directement ils lèvent l'erreur d'évaluation typée de R74g. Le dépassement n'est ni rapporté seulement à l'évaluation, ni passé sous silence, ni signalé par une exception non typée. Les données vanilla n'en approchent pas. | S-arch |
| R74e | Au plus 1000000 de piles par tirage racine, comptées après le fractionnement de R54. Les tables imbriquées (R23) ne fractionnent pas leurs piles : le fractionnement a lieu une seule fois, à la sortie du tirage racine. Les contrôles de plafond (ce plafond de piles, et le plafond d'unités de travail : 10000000 d'unités, cumulées **par appel de haut niveau** (R74f), qui couvre la décroissance de R41, parcourue unité par unité, et les essais des binomiales de R74b ; une seule pile au-dessus du plafond échoue aussi. Une binomiale unique de n = 100000 reste acceptée ; 101 binomiales de n = 100000 en cascade dans un même appel dépassent 10000000 unités et échouent) s'appliquent **au fil du calcul**, et pas seulement à la sortie : ils précèdent toute allocation et toute boucle proportionnelle à la taille, de sorte que le travail fait avant l'erreur reste borné. | S-arch |
| R74f | Budget global borné **par appel de haut niveau** de chaque point d'entrée d'évaluation du module (évaluation d'une table, d'une condition, d'un fournisseur de nombres, d'un modificateur, d'un effet d'enchantement, d'une valeur dépendant du niveau, des fournisseurs de machines), jamais par contexte : aucun cumul entre appels indépendants, les appels imbriqués comptent sur l'appel en cours, et un contexte reste utilisable après une erreur de budget. Il compte **chaque noeud évalué** : un test de condition (imbriquée, ou atteinte par une référence à un prédicat nommé), une évaluation de fournisseur de nombres (imbriqué, ou atteint par une référence nommée), un tirage de pool, un tour de `rolls`, une expansion d'entrée, une application de modificateur, une évaluation de valeur dépendant du niveau (R20, R21). Les références nommées ne sont pas mémoïsées : le décompte reflète le développement complet. Plafond provisoire : environ 1000000 de noeuds par appel de haut niveau, à quelques noeuds près (le surcoût d'un pool est de l'ordre de quelques noeuds) ; c'est un ordre de grandeur de conception, pas une valeur de référence du jeu ; au-delà, erreur d'évaluation typée (R74g). Motif : un graphe de références sans cycle peut se développer de façon exponentielle (des prédicats p_i valant chacun `any_of` de deux références à p_(i+1) font 2^n évaluations pour n niveaux), et la profondeur d'une chaîne de références (R74d) ne borne pas la largeur ; seul un budget sur le nombre total de noeuds évalués borne le travail. Il couvre aussi les cas où chaque borne de R74a à R74e est respectée mais où leur produit ne l'est pas. Le contrôle s'applique au fil du calcul (R74e). | S-arch |
| R74g | Réaction du projet au dépassement. (1) Constante : `rolls` au-dessus de 4096 ou `binomial` `n` au-dessus de 100000 dans la table : la table est refusée au chargement (diagnostic de valeur hors plage), les autres tables se chargent ; un poids constant au-dessus de 2^20 est seulement plafonné, avec avertissement (R74c). (2) Dépassement constaté à l'évaluation (fournisseur dynamique, imbrication de 256 tables, R74e, budget de R74f) : erreur d'évaluation typée, qui fait échouer le tirage racine, sans pile partielle ni troncature. Aucune troncature silencieuse des tirages. | S-arch |
| R74h | Comportement du jeu au-delà de ces valeurs (exécution complète, limite propre, plantage) : inconnu, et sans effet sur les données du jeu (R6, R8, R39, R53 : `rolls` au plus 5, `n` au plus 3, poids au plus 699). Provisoire : le refus décrit par R74g. Sonde PL26. | S |
| R74i | Saturation de `enchanted_count_increase` (R43) : la somme des tirages de `count` est arrondie vers le bas avec saturation (NaN vaut 0 ; un infini ou un débordement est ramené à la plage des entiers), puis ajoutée à la quantité de la pile, qui sature à la plage des entiers sans déborder ; la pile résultante est ensuite fractionnée selon R54, et le plafond de 1000000 de piles de R74e s'applique (erreur d'évaluation typée au-delà, R74g). Le niveau d'enchantement est borné à 255 par le composant (`item-enchantments.md` R10), ce qui borne le nombre de tirages. Le jeu peut traiter ces valeurs extrêmes autrement : provisoire, la saturation décrite ici. Sonde PL28 (c). | S |
| R75 | Clé inconnue (absente de R2 à R10, du tableau des conditions, du tableau des modificateurs, de R50 à R53, de R57 à R62 et de R79) dans une table, un pool, une entrée, une condition, un modificateur ou un fournisseur de nombres. Provisoire : le décodage est **strict**, dans l'esprit de `data-datapack-loading.md` R63 (b), choix que R63 (d) laisse à chaque module : la table qui contient la clé est refusée avec le diagnostic de clé inconnue du module de butin (propre à ce module, distinct du diagnostic de la couche de données ; chemin de la clé, un diagnostic par clé), les autres tables se chargent. Hypothèse H-unknown-ignore : le jeu **ignore** les clés inconnues des tables de butin (le défaut de R63 (a)). La page "Loot table" (relue le 2026-10-29) ne dit ni l'un ni l'autre ; elle ne signale qu'un avertissement journalisé, sans rejet, pour un `type` de table incohérent avec les paramètres lus (R15). Les 1447 tables vanilla ne contiennent aucune clé inconnue (C, le décodage de toutes les tables sans diagnostic est le test L1 du plan de tests) : le choix ne change rien aux données du jeu, seulement aux packs tiers ; se tromper vers le rejet refuse une table que le jeu accepterait. Sonde PL24. | S |
| R76 | Cycles de références nommées (décision de conception) : le chargeur détecte avant toute évaluation les cycles dans les références à des prédicats nommés (A vers B vers A, y compris A vers A, à travers `all_of`, `any_of`, `inverted`), à des fournisseurs de nombres nommés, entiers et flottants (à travers tout fournisseur composite : `add`, `mul`, `min`, `max`, `avg`, `from_float`, `from_int`, `binomial`...) et à des tables de butin (`loot_table`, R23). La détection couvre aussi les cycles qui passent d'une famille à l'autre (un fournisseur entier qui appelle un fournisseur flottant par `from_float`, qui rappelle le fournisseur entier). Méthode : parcours en profondeur avec ensemble des noeuds en cours ; le diagnostic de cycle identifie l'élément concerné, c'est-à-dire l'identifiant d'un élément qui participe au cycle ; la spec ne promet pas la liste complète des identifiants de la chaîne. L'évaluation ne récurse jamais sur un cycle. Cette détection ne dépend pas des profondeurs maximales de R74d (255 niveaux d'imbrication, 64 niveaux de chaîne de références nommées). L'auto-référence d'une table (R23) reçoit un diagnostic dédié. | S-arch |
| R77 | Effet observable d'une référence non résolue ou cyclique. Une référence à un prédicat nommé (R61b) ou à un fournisseur nommé dont l'identifiant n'existe pas, ou qui entre dans un cycle (R76), est une erreur de chargement de la table qui la contient ; les autres tables se chargent. Pour un cycle entre deux tables (A appelle B, B appelle A), les deux tables sont refusées. Champs concernés : `rolls`, `bonus_rolls`, `count` de `set_count`, `n` et `p` de `binomial`, `input` de `from_float` et de `from_int` (nom du champ : R79), `chance` de `random_chance`, et tout autre champ qui reçoit un fournisseur. Sources (relues le 2026-10-29) : page "Loot table" (depuis 26.3 snapshot 4, `rolls` et `bonus_rolls` acceptent une valeur en ligne ou un identifiant) et page "Number provider" (un fournisseur se référence par identifiant ; `n` et `p` de `binomial` et l'opérande de `from_float` et `from_int` sont eux-mêmes des fournisseurs) ; ces pages ne disent rien des cycles ni du sort d'une référence inconnue. Hypothèses nommées : H-ref-a (retenue comme provisoire) : seule la table qui porte la référence est refusée ; H-ref-b : le rechargement des données échoue dans son ensemble (cf. `data-datapack-loading.md` R55). Sonde PL25. | S |
| R78 | Ports d'aléa du module (décision de conception, ADR 0004, amendement 2). Deux ports, décrits par leur rôle : un port d'état des séquences nommées, par lequel le tirage lit et avance l'état d'une séquence (R65) sans connaître son support de persistance ; un port d'observation, qui reçoit chaque décision aléatoire dans l'ordre où elle est prise (R66), sans pouvoir la modifier. R65 prescrit un état persistant, initialisé à la première utilisation et partagé entre les tables d'une même séquence : c'est ce qui justifie le premier port. R66 ne prescrit que l'ordre de consommation (S, H-draw) et aucun port : l'existence de l'observateur est une décision de conception, motivée par le besoin de comparer cet ordre à l'oracle (PL5) et de tester H-draw. L'absence d'observateur ne change aucun résultat. | S-arch |
| R79 | Noms des champs d'opérande des fournisseurs de nombres. Opérateurs binaires `sub`, `div` : `left` et `right` (D : `div` 18 occurrences dans `context_int_provider`, `left` et `right` seuls ; `sub`, `floor_div`, `mod`, `floor_mod` : S). `add` : liste `inputs` (D, échange `emerald_dyed_leather_chestplate`) ; `mul`, `min`, `max`, `avg`, `length` : `inputs` (S, page "Number provider", relue le 2026-10-29). `pow` : `base` et `exponent` (S, même page). **Opérateurs unaires** (`abs`, `ceil`, `cos`, `floor`, `from_float`, `from_int`, `negate`, `round`, `sin`, `sqrt`, `truncate`) : champ `input` selon la même page (S). Aucune donnée extraite de butin ou d'échange n'utilise un opérateur unaire ou `from_float` / `from_int` (seul le registre les nomme ; les `abs` et `negate` des fichiers extraits sont des fonctions de densité de génération du monde, hors sujet), donc aucun nom de champ unaire n'est prouvé par les données. Provisoire : `input` pour tous les opérateurs unaires. Point ouvert : un décodeur qui lirait `argument` pour les unaires autres que `from_float` et `from_int` s'écarterait de la page ; la sonde PL27 lit la forme acceptée par l'oracle. | D (binaires `div`, `add`) ; S (reste) |
| R80a | Le total des poids effectifs d'un pool se calcule sur 64 bits. | S-arch |
| R80b | Le tirage pondéré est borné par la somme quand elle tient dans un entier 32 bits signé ; au-delà, le tirage emploie un entier 64 bits réduit modulo la somme (H-poids64, provisoire). Le jeu peut consommer l'aléa autrement au-delà de 2^31 - 1 : cette somme exige plus de 2047 entrées de poids 2^20 (R74c) alors que les poids vanilla sont au plus 699 (R8), donc le cas est hors de portée des données du jeu et seule la sonde PL26 (variante à 2049 entrées de poids 2^20) peut l'observer. | S |

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
- `limit_count` avec `min` > `max` (R40) ; `apply_bonus` au niveau 0 (R42) ; `explosion_decay` sur une pile d'une unité (R41) ; clé inconnue, référence absente ou cyclique, valeur au-delà d'un plafond (R74 à R77).

## Vecteurs de test

D/C (lisibles sans oracle) :

| Vecteur | Entrée | Sortie attendue | Preuve |
|---|---|---|---|
| V-L1 | Décodage de `blocks/stone` | table `block`, 1 pool, `rolls` 1, entrée `alternatives` de 2 enfants : `item` `stone` avec condition-référence `minecraft:tool/can_silk_touch`, puis `item` `cobblestone` avec `survives_explosion` ; `random_sequence` `minecraft:blocks/stone` | D |
| V-L2 | Décodage de `blocks/oak_leaves` | 3 pools ; pool 0 : `alternatives` (`oak_leaves` si `any_of` [`tool/can_shear`, `tool/can_silk_touch`], sinon `oak_sapling` si `all_of` [`survives_explosion`, `table_bonus` fortune `[0.05, 0.0625, 0.083333336, 0.1]`]) ; pool 1 conditionné par `inverted(any_of ...)` : `stick` avec `table_bonus` `[0.02, 0.022222223, 0.025, 0.033333335, 0.1]`, modificateurs `set_count` uniform 1 à 2 puis `explosion_decay` ; pool 2 : `apple` | D |
| V-L3 | Décodage de `entities/cow` | 2 pools ; pool 0 : `leather`, `set_count` uniform 0 à 2 puis `enchanted_count_increase` looting uniform 0.0 à 1.0 ; pool 1 : `beef`, `set_count` uniform 1 à 3, `furnace_smelt` conditionnée par `any_of` [`entity_properties this flags is_on_fire`, `entity_properties direct_attacker equipment mainhand predicates enchantments #minecraft:smelts_loot`], `enchanted_count_increase` | D |
| V-L4 | Décodage de `gameplay/fishing` | 3 entrées `loot_table` (junk 10/-2, treasure 5/+2 avec `fishing_hook.in_open_water`, fish 85/-1) | D |
| V-L7 | Dénombrement | 1447 tables ; 1589 pools ; 2556 entrées `item` ; types de table de R2 ; 21 types de modificateur utilisés sur 42, 15 types de condition utilisés sur 20 (`time_check`, `entity_scores`, `int_value_check`, `float_value_check`, `environment_attribute_check` non utilisés) | C |
| V-L8 | `blocks/decorated_pot` | `alternatives` : `dynamic` `minecraft:sherds` sous `match_block decorated_pot cracked "true"`, sinon `item decorated_pot` avec `copy_components` source `block_entity` include `["minecraft:pot_decorations"]` | D |

Vecteurs de comportement (S, à produire par les sondes ; l'oracle donne la valeur) :

| Vecteur | Entrée | Sortie attendue (provisoire) |
|---|---|---|
| V-L9 | `/loot spawn 0 100 0 loot minecraft:blocks/stone`, N = 1000 | 1000 x `cobblestone` x1 (aucune aléa) |
| V-L10 | `/loot spawn ... mine <pos> <outil à Silk Touch>` sur `stone` | 1 x `stone` |
| V-L11 | Fortune 0 à 3 sur `oak_leaves` x 200000 | fréquence de sapling : 5, 6.25, 8.33, 10 pour cent (R32) à l'écart statistique près |
| V-L12 | `fishing` chance 0 à 3, N = 200000 | proportions de R27 |
| V-L13 | Coffre `simple_dungeon` avec `LootTableSeed` S (10 graines) | empreinte du multi-ensemble de piles par graine (golden ; les slots ne sont pas comparés, R70) |
| V-L5 | Distribution de pêche (R27) | proportions de R27 (PL1, V-L12) |
| V-L6 | `archaeology/desert_well` (R28) | 25 pour cent par tesson, 12,5 pour cent par autre entrée (PL5) |
| V-L14 | C sur S (R41, H-decay-a et H-decay-b) : table `set_count` constante puis `explosion_decay`, rayon r = 3, 4, 6 ; pile de 1 unité et pile de 4 unités. Commande : `python -I -c "[print(r, round(1/r,4), round(4/r,4), round(1-1/r,4), round(4-4/r,4)) for r in (3,4,6)]"` (colonnes : r, conservation par unité et moyenne d'une pile de 4 sous H-decay-b, puis les mêmes sous H-decay-a) | r=3 : 0.3333, 1.3333 (b) ; 0.6667, 2.6667 (a). r=4 : 0.25, 1.0 (b) ; 0.75, 3.0 (a). r=6 : 0.1667, 0.6667 (b) ; 0.8333, 3.3333 (a). Valeur à mesurer par PL11 |
| V-L15 | C sur S (R22, H-tag-cond-a et H-tag-cond-b) : entrée `tag` `expand: true` sur une étiquette de 2 objets, condition `random_chance` 0.5, un tirage par pool, N = 200000. Commande : `python -I -c "print(0.5, 0.5*0.5)"` (probabilité qu'aucun candidat ne subsiste) | Aucune pile avec la fréquence 0.5 sous H-tag-cond-a et 0.25 sous H-tag-cond-b ; dans les deux cas les deux objets sortent à égalité quand une pile sort. Valeur à mesurer par PL8 |
| V-L16 | C sur S (R40, H-limit-a, H-limit-b, H-limit-c) : `limit_count` `{min 5, max 2}` appliqué à des piles de 1, 3 et 9 | H-limit-a : 2, 2, 2 ; H-limit-b : 5, 5, 5 ; H-limit-c : table absente après `/reload`. Valeur à mesurer par PL23 |

## Plan de tests

- **Unitaire (L1)** : décoder les 1447 tables, les 3 prédicats et les 30 fournisseurs contextuels sans erreur ; réencoder et comparer à l'arbre JSON (ordre des clés indifférent) ; V-L1 à V-L4, V-L7, V-L8 ; tests de propriétés : somme des probabilités de sélection = 1 pour les cas sans condition ; invariance par permutation des entrées de même poids ; ports factices pour toutes les conditions (chaque condition, chaque paramètre absent, R15).
- **Statistique (L2)** : pour chaque scénario, N >= 200000 tirages sur le moteur et sur l'oracle ; test du khi-deux à répartition identique, rejet si p < 1e-4, répété avec trois graines de session ; tolérance d'un test de proportion à 5 écarts-types pour les scénarios à une classe. Scénarios : V-L9 à V-L12 plus 50 tables tirées de `tools/diff/samples.json` par type de table (au moins 3 par type de R2).
- **Déterministe (L2)** : conteneurs à `LootTableSeed` non nul : comparaison exacte du multi-ensemble de piles (identifiant, quantité, composants) sur 200 graines x 10 tables (PL5, PL16) ; c'est ce test qui valide R63 et R66. Le placement dans les slots (R70) n'est comparé que statistiquement tant que PL5 n'a pas livré l'algorithme.
- **Niveau A** : tests écrits en aveugle par le `test-engineer`, relecture par un second agent (spec `loot` = niveau de risque A).

## Sondes proposées

Cadre : serveur 26.3 headless, monde plat, RCON, `/tick freeze`. Commandes de `commands.json` : `loot spawn <x> <y> <z> loot <table>`, `loot give @s mine <pos> <outil>`, `/item modify`. Les conteneurs à graine : `setblock <pos> minecraft:chest{LootTable:"<table>",LootTableSeed:<graine>L}` suivi d'un déballage sans client (entonnoir sous le coffre, `data get block`).

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Règles levées |
|---|---|---|---|
| PL1 | Pack `sonde` : table `sonde:t1`, pool à 1 tirage, 2 entrées `item` : pierre (poids 1, qualité +1) et terre (poids 100, qualité -1). La chance vient de l'effet `luck` du joueur (`effect give @s minecraft:luck 1000 N`). Elle ne passe que par certaines sources (R18) : appeler la table par `loot give @s fish sonde:t1 <pos> mainhand` (contexte de pêche) et par `loot give @s loot sonde:t1` (contexte de commande) ; N de 0 à 5, 200000 tirages par cas ; relever les proportions de pierre et de terre. | Dans quels contextes la chance modifie les proportions ; proportions compatibles avec R25 | R18, R25 |
| PL2 | Mêmes tables avec des poids entiers de 1 à 3 et une qualité de +1 ou -1, et une chance fixée par l'attribut de chance du joueur (`attribute @s minecraft:luck base set <valeur>`, valeurs entières puis fractionnaires comme 0.5) ; 200000 tirages par cas | Frontières du plancher et de la borne à 0 de R25 : une entrée dont le poids effectif tombe à 0 ne sort jamais ; effet d'une chance fractionnaire | R25 |
| PL3 | (a) Table `sonde:ore` : `item diamond` + `apply_bonus` fortune `ore_drops`, `loot spawn ... mine <pos> <pioche avec fortune N>` 200000 fois, N=0..3 ; (b) niveau 0 et consommation (H-bonus0-a contre H-bonus0-b) : conteneur à `LootTableSeed` sur 200 graines, table à deux pools, le premier `item diamond` + `apply_bonus` `ore_drops`, le second un tirage `uniform` de 0 à 1000000 en `set_count`, l'outil sans fortune ; comparer la quantité du second pool avec celle d'une table identique sans le premier modificateur | (a) Distribution de la quantité : support et probabilités ; (b) quantités du second pool égales si le niveau 0 ne consomme rien (H-bonus0-b), décalées sinon (H-bonus0-a) ; même protocole avec `uniform_bonus_count` pour le cas `min == max` de `core-random.md` P4 | R42 |
| PL4 | Tables `uniform_bonus_count` (mult 1 et 2), `binomial_with_bonus_count` (extra 3, p 0.5714286) avec outil fortune 0 à 3, 200000 tirages chacune | Distributions | R42 |
| PL5 | Conteneurs à graine S pour 200 graines sur `chests/simple_dungeon`, `chests/abandoned_mineshaft`, `chests/bastion_treasure` : matérialiser et enregistrer (slot, pile) ; variante avec table à un seul candidat par pool pour isoler R26 | Empreinte par graine ; confirmation de R63 (graine non nulle), R66, R26 | R26, R63, R66 |
| PL6 | Table avec modificateurs aux trois niveaux (entrée `set_count` 2, pool `set_count add` +3, table `limit_count` max 4) ; 1 `/loot spawn` | Quantité finale 4 si ordre entrée -> pool -> table ; autre ordre donnerait une autre valeur | R19, R48 |
| PL7 | (a) Table à `rolls` uniform 0 à 3, `bonus_rolls` 0.5, chance 2 (effet luck 2) ; 200000 ; (b) chance négative (effet `unluck`, ou `attribute @s minecraft:luck base set` à -1, -0.5 et -5) : tables à `rolls` constant 3 et `bonus_rolls` 1, puis `bonus_rolls` 0.5, 200 `loot spawn` chacune ; (c) cas limites : `rolls` -2 avec `bonus_rolls` 1 et chance 5 ; `rolls` 3, `bonus_rolls` 1, chance -5 | (a) Loi du nombre de tirages ; (b) 2 tirages pour chance -1 et pour `bonus_rolls` 0.5 à chance -1, 0 pour chance -5, 2 pour chance -0.5 avec `bonus_rolls` 1 (floor et non troncature) si le nombre de tirages est réduit (H-rolls-a ou b), 3 partout si H-rolls-c ; (c) 3 (H-rolls-a) ou 0 (H-rolls-b) | R5, R6, R19 |
| PL8 | (a) Entrée `tag` sur `#minecraft:dyes` avec `expand` true puis false, avec `set_count` 5 en modificateur de l'entrée ; (b) étiquette `sonde:duo` de 2 objets (`stone`, `dirt`), entrée `tag` `expand: true` avec condition `random_chance` 0.5, un pool à un tirage, `loot spawn` 200000 fois ; (c) mêmes tables en conteneurs à `LootTableSeed` sur 200 graines pour distinguer un tirage de condition consommé une fois ou par enfant | (a) Une pile ou 16 piles ; effet du modificateur (H-tag-mod-a ou b) ; (b) fréquence de l'absence de pile : 0.5 (H-tag-cond-a) ou 0.25 (H-tag-cond-b), V-L15 ; (c) décalage des tirages suivants | R22 |
| PL9 | `any_of`/`all_of` vides ; `any_of` avec `random_chance` d'abord pour tester le court-circuit (graine sur conteneur) | Valeurs de R29 ; consommation | R29 |
| PL10 | `table_bonus` avec liste courte (2 cases) et outil fortune 3 | Index borné ou échec | R32 |
| PL11 | Pack `sonde` qui remplace la table d'un bloc simple par deux variantes (contrôle `survives_explosion` ; `set_count` constante 1 puis `explosion_decay`) et une troisième avec `set_count` constante 4 puis `explosion_decay`. Explosions de puissance connue : dynamite (4) après `gamerule tnt_explosion_drop_decay true` (par défaut la dynamite lâche tout, R73), creeper (3) et creeper chargé (6) avec `mob_explosion_drop_decay true`. N = 20000 blocs par cas, comptage des objets au sol. Contrôle : mêmes explosions avec la règle de décroissance à false | Fréquence de conservation d'une unité : 1/r pour le contrôle `survives_explosion` ; pour `explosion_decay`, 1/r (H-decay-b) ou 1 - 1/r (H-decay-a), V-L14 ; moyenne de la pile de 4 ; règle fausse : tout est conservé (H-decay-regle) ; rayon du contexte égal à la puissance (H-rayon) | R33, R41 |
| PL12 | Table `entities/cow` avec arme Looting 0 à 3, 100000 `/loot spawn ... kill @e[type=cow,limit=1]` (vache sans IA sous `/tick freeze`) | Moyenne et support du cuir et du boeuf | R43 |
| PL13 | Tables `enchant_randomly` sur livre et sur épée (`options` étiquette ou liste), 100000 | Loi des enchantements et niveaux | R45 |
| PL14 | Table `set_count` 70 `cobblestone` ; `set_count` 100 ; `loot spawn` et `loot give` | Fractionnement de pile | R54 |
| PL15 | Tables `filtered` avec `item_filter` `{"predicates":{"minecraft:dyed_color":{}}}` sur cuir teinté et non teinté, et avec `minecraft:map_id` | Sémantique de présence | R58 |
| PL16 | (a) Tables à `loot_table` imbriquée avec séquence propre contre séquence partagée, deux conteneurs à graine (graine 0 et non nulle) ; (b) `item modify`/`item fill`/`item override` avec `slot_source` de chaque type | Source d'aléa de l'imbriquée ; formes de `slot_source` | R64, R70, R71 |
| PL17 | Pack `sonde` : tables avec champs omis (`weight`, `quality`, `expand`, `type`, `bonus_rolls`), `rolls` à 0, négatif et fractionnaire, `condition` en liste, pool vide, table sans pool ; `loot spawn` 20000 fois et journal | Défauts de R2, R4 à R6, R8, R9, R22, R55 | R2, R4 à R6, R8, R9, R22, R55 |
| PL18 | Tables `sonde:ctx_*` dont une condition ou un modificateur lit un seul paramètre (`this`, `attacker`, `direct_attacker`, `attacking_player`, `tool`, `origin`, `block_state`, `explosion_radius`, chance) ; appelées par `loot spawn ... loot`, `kill`, `mine`, `fish` ; journal des avertissements au chargement pour un `type` incohérent | Quel jeu de paramètres chaque source fournit ; comportement d'un paramètre absent | R11 à R15, R69, R72 |
| PL19 | Entrées `group`, `sequence`, `alternatives` imbriquées, `dynamic` (`minecraft:contents`), `empty` avec poids, `slots` avec chaque `slot_source` ; entrée `tag` `expand: true` à condition aléatoire (étiquette de 3 objets) ; conteneurs à graine pour distinguer les consommations | Contenu des composites ; comportement de `dynamic` sans émetteur ; consommation du tirage de condition d'une entrée `tag` développée (H-tag-cond-a ou b) | R20, R21, R22, R24 |
| PL20 | Une table par condition (R29 à R37), 200000 tirages chacune : `random_chance` (0, 0.5, 1), `random_chance_with_enchanted_bonus`, `match_block`, `weather_check` (pluie et orage), `time_check`, `int_value_check`, `float_value_check`, `entity_scores`, `killed_by_player`, `entity_properties` pour chaque cible, `damage_source_properties` | Proportions et branches de R29 à R37 | R29 à R37 |
| PL21 | Une table par modificateur, y compris les 21 types de R47, avec entrée d'une pile connue (`set_count` avec `add`, `limit_count`, `set_damage` avec `add`, `set_name` des deux cibles, `set_enchantments` avec `add`, `set_components` avec `!`, `copy_state`, `furnace_smelt`, `filtered`, `discard`, `set_potion`, `set_stew_effect`, `set_instrument`, `exploration_map`) ; `loot spawn` et relecture de l'objet au sol (`data get entity`) | Pile résultante exacte de R38 à R40, R44a à R44k, R46, R47, R49, R56 | R38 à R40, R44a à R44k, R46, R47, R49, R56 |
| PL22 | Tables isolées avec un seul tirage : `set_count` uniform entier (bornes 0 à 3 et 5 à 5), `set_damage` uniform flottant, `set_count` binomial (n=3, p=0.5) ; conteneurs à `LootTableSeed` sur 200 graines | Valeurs exactes par graine : formules de R51 à R53 | R51 à R53 |
| PL23 | Tables `sonde:lim_*` : `limit_count` `{min 5, max 2}`, `{min 2, max 5}`, `{min 3}`, `{max 0}` et la forme entière `limit` 4, appliquées à `set_count` constante 1, 3 et 9 ; `loot spawn` ; journal du chargement et de `/reload` | Pile résultante par cas ; table acceptée ou refusée pour `min` > `max` (H-limit-a, b, c) ; sens de la forme entière (H-limit-int-a ou b) | R40 |
| PL24 | Pack `sonde` : une clé inconnue (`"zzz": 1`) dans la table, dans un pool, dans une entrée, dans une condition, dans un modificateur et dans un fournisseur, une table par emplacement ; `/reload`, journal, puis `loot spawn` | Table chargée (clé ignorée, H-unknown-ignore) ou refusée, avec quel message ; un avertissement éventuel | R75 |
| PL25 | Pack `sonde` : (a) deux prédicats nommés qui se référencent (A vers B vers A) et une table qui utilise A ; (b) deux fournisseurs nommés (`context_int_provider`) qui se référencent, utilisés par `rolls` ; (c) deux tables qui s'appellent par `loot_table` ; (d) référence à un identifiant de fournisseur absent dans `rolls`, `bonus_rolls`, `set_count count`, `binomial p` et `from_float input` ; (e) idem pour un prédicat absent ; `/reload`, journal, puis `loot spawn` sur les tables saines voisines | Table refusée ou rechargement entièrement refusé (H-ref-a ou H-ref-b) ; message ; une table saine reste-t-elle disponible | R23, R61b, R76, R77 |
| PL26 | Pack `sonde` : `rolls` constant 100000 ; `binomial` entier `n` 200000 ; entrée de poids 33554432 face à une entrée de poids 1 ; imbrication de 300 `all_of` ; chaîne de 300 tables imbriquées ; pool de 2049 entrées de poids 1048576 (somme au-delà d'un entier 32 bits, R80b) ; chacune dans une table séparée ; `loot spawn`, conteneurs à graine pour R80b | Exécution complète, plafond propre au jeu ou refus (R74h) ; consommation d'aléa de la somme au-delà de 32 bits (R80b) ; sans effet sur les plafonds du projet, qui restent des décisions de conception | R74h, R80b |
| PL27 | Pack `sonde` : une table par opérateur unaire de R79 (`abs`, `negate`, `ceil`, `floor`, `round`, `truncate`, `sqrt`, `sin`, `cos`, `from_float`, `from_int`) dans `set_count` ou `rolls`, écrite avec `input`, puis avec `argument`, puis avec une clé absente ; mêmes essais pour `pow` (`base`, `exponent`) et `length` (`inputs`) ; `/reload`, journal, puis `loot spawn` | Forme acceptée par l'oracle pour chaque opérateur ; message d'erreur du champ manquant | R79 |
| PL28 | (a) `time_check` : tables avec `value` {min 0, max 12000} sans `period`, puis avec `period` 24000, puis `period` 7000 ; `/time set` sur 6000 puis `/time add 24000` ; réussite de la condition par jour et heure ; (b) `enchanted_count_increase` au niveau 0 (arme sans l'enchantement) : conteneur à `LootTableSeed` sur 200 graines, deux pools, le premier avec ce modificateur, le second un `set_count` uniform de 0 à 1000000 ; comparer avec la même table sans le premier modificateur ; (c) valeurs extrêmes de `count` (R74i) : `div` de 1.0 par 0.0 (infini), `div` de 0.0 par 0.0 (NaN), une constante de 3000000000.0, avec une pile de `set_count` 1 | (a) Temps comparé : cumulé (H-time-a) ou heure du jour (H-time-b) ; (b) second pool identique si aucun tirage n'est consommé au niveau 0 (H-eci0-a), décalé sinon (H-eci0-b) ; (c) pile résultante pour chaque valeur extrême : NaN laisse la quantité, infini et débordement saturent à la plage des entiers, puis la pile est fractionnée selon R54 ; au-delà de 1000000 de piles, erreur d'évaluation typée (R74e, R74i) | R36, R43, R74i |

## Couverture des règles S par les sondes

R2, R4b, R8, R9, R55 : PL17 ; R5b : PL7 ; R11, R12, R13, R15, R69, R72 : PL18 ; R14a est C et R14b : PL18 ; R16 (découpage en ports) et R17 sont des décisions de conception (ADR 0004), vérifiées par la règle d'architecture (ArchUnit : aucun import de `entity` dans `loot`) et par les ports factices, sans sonde ; R20, R21, R23, R24 : PL19, PL16 ; R30 à R37 : PL20 ; R38 à R40, R44a à R44k, R46, R47, R49, R56 : PL21 ; R70 : PL5 ; R51 à R53 : PL22, PL3, PL4 ; R65 à R68 : PL5, PL16, et sondes P1 et P2 de `core-random.md` pour la persistance ; R67 (déterminisme des commandes) : PL5 en deux appels successifs sans graine. R57b : PL18, PL20 ; R61b : PL17. R22 (condition et modificateurs d'une entrée `tag`) : PL8, PL19 ; R33 et R41 (décroissance, H-decay-a et b, H-rayon, H-decay-regle) : PL11 ; R40 (H-limit) : PL23 ; R42 (niveau 0, H-bonus0) : PL3 b ; R74h : PL26 ; R74i : PL28 ; R19b (H-rolls) : PL7 ; R75 : PL24 ; R77 : PL25 ; R79 : PL27 ; R80b : PL26 ; R36 (`time_check`) : PL28 ; R43 (niveau 0) : PL12, PL28. R74a à R74g, R76, R78 et R80a sont `S-arch` : sans sonde. Aucune autre règle S n'est sans sonde.

## Questions ouvertes

- Contenu exact (paramètres requis et optionnels) de chacun des 31 jeux de contexte (R13) : seuls les besoins déduits des données sont acquis (R14).
- Défauts absents de la documentation et des données : `expand` (R22), niveau hors liste de `table_bonus` (R32), arrondi de `enchanted_count_increase` (R43), `set_count`/`set_damage` (arrondis).
- Formules exactes des tirages de `apply_bonus` (bornes et nombre de tirages), `uniform` et `binomial` flottants.
- Ordre H-order (R48) et H-draw (R66), source de la graine de `LootTableSeed` et mélange des slots (R70).
- Précision numérique du poids effectif (R25) : elle n'est observable que sur des frontières construites (PL2).
- Divergence des pages "Item modifier" (`explosion_decay`, unité perdue avec 1/r) et "Predicate" (`survives_explosion`, réussite avec 1/r) : H-decay-a ou H-decay-b, levée par PL11 (R41) ; rayon du contexte égal à la puissance de l'explosion (H-rayon).
- Condition d'une entrée `tag` développée, évaluée une fois ou par enfant (H-tag-cond-a ou b, R22) ; modificateurs de l'entrée sur les enfants (H-tag-mod-a ou b).
- `apply_bonus` au niveau 0 : consommation d'un tirage ou non (H-bonus0-a ou b, R42) ; `limit_count` avec `min` > `max` et forme entière (H-limit, R40) ; clés inconnues (H-unknown-ignore, R75) ; références non résolues ou cycliques (H-ref-a ou b, R77) ; comportement du jeu au-delà des plafonds de R74.
- Noms de champ des opérateurs unaires de fournisseurs (R79) : la page donne `input`, aucune donnée ne le prouve ; `time_check` : temps cumulé ou heure du jour (H-time-a ou b, R36) ; `enchanted_count_increase` : un tirage par niveau ou un seul tirage pour tous les niveaux (H-eci-a ou b) et niveau 0 (H-eci0-a ou b, R43).
- Comportement de `slots`, `sequence`, `group` (aucune donnée vanilla) et de 21 types de modificateur sans usage (R47 : identité provisoire, levée par PL21).
- Algorithme de placement des piles dans les slots d'un conteneur déballé (R70) : à retrouver à partir des données de PL5.
- Les sondes PL1 à PL28 supposent l'effet de potion `luck` et un client de commande pour la chance de pêche réelle ; pour les scénarios de pêche complets, un client protocole minimal est nécessaire (voir `item-components-inventory.md` Questions ouvertes).
- Règles S restantes : R4b, R5b, R6 (reste), R8 (défauts), R11 à R13 (contenu), R14b, R15 à R26, R27 et R28 (valeurs effectives), R29 à R37 (sens), R38 à R49, R51 à R53 (tirages), R54, R56, R57b, R58 (sémantique), R61b, R63 à R73 (sauf présences D), R74h, R74i, R75, R77, R79 (hors binaires `div` et `add`), R80b.

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Tables de butin | M6 | A | spécifié | `docs/specs/loot-tables.md` (formats et dénombrements en D/C ; tirage, ordre d'aléa, conditions et modificateurs en S avec sondes PL1 à PL28) |
| Prédicats et modificateurs d'objet (ligne à créer) | M6 | A | spécifié | idem, R38 à R49, R57a à R62 |

## Annexe : loot_stats.py (C)

Lit uniquement les fichiers extraits ; aucune dépendance au jeu ; bibliothèque standard seule. À copier hors du dossier de données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I loot_stats.py "$REF" | sha256sum
```

Empreinte SHA-256 attendue de la sortie complète (sortie en UTF-8, fins de ligne LF) :

```text
ef3b654f70c9b77598cc35946960436df33824e09fb022ea39c2d5222de1094a
```

```python
"""Counts and shapes of loot tables, conditions, modifiers and predicates in the 26.3 data.

Usage: python -I loot_stats.py <reference dir containing generated/ and resources/>
Reads extracted files only. Standard library only. Output is deterministic (sorted, LF).

Counting base ("LEP"): resources/data/minecraft/{loot_table,enchantment,predicate}. Advancements,
villager trades and context providers are outside the base unless a line says otherwise.
"""
import collections
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", newline="\n")
REF = sys.argv[1]
REP = os.path.join(REF, "generated", "reports")
DATA = os.path.join(REF, "resources", "data", "minecraft")


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def tree(sub):
    out = {}
    base = os.path.join(DATA, sub)
    for root, _, files in os.walk(base):
        for fn in files:
            if fn.endswith(".json"):
                rel = os.path.relpath(os.path.join(root, fn), base).replace(os.sep, "/")[:-5]
                out[rel] = load(os.path.join(root, fn))
    return dict(sorted(out.items()))


def section(name):
    print("== " + name)


def reg(name):
    return [e.split(":", 1)[1] for e in load(os.path.join(REP, "registries.json"))["minecraft:" + name]["entries"]]


def keys_anywhere(o, c):
    if isinstance(o, dict):
        for k, v in o.items():
            c[k] += 1
            keys_anywhere(v, c)
    elif isinstance(o, list):
        for v in o:
            keys_anywhere(v, c)


def as_list(v):
    return v if isinstance(v, list) else [v]


COND_T = set(reg("loot_condition_type"))
FUNC_T = set(reg("loot_function_type"))
ENTRY_T = set(reg("loot_pool_entry_type"))
tables = tree("loot_table")
enchs = tree("enchantment")
preds = tree("predicate")
BASE = [("loot_table", tables), ("enchantment", enchs), ("predicate", preds)]

section("R1 tables by folder prefix")
print("tables", len(tables))
print(sorted(collections.Counter(k.split("/")[0] for k in tables).items(), key=lambda kv: (-kv[1], kv[0])))

section("R2 table keys and type")
kc = collections.Counter()
for t in tables.values():
    kc.update(t.keys())
print(sorted(kc.items()))
print("type", sorted(collections.Counter(t["type"] for t in tables.values()).items(), key=lambda kv: (-kv[1], kv[0])))
by = collections.defaultdict(collections.Counter)
for k, t in tables.items():
    by[t["type"].split(":")[1]][k.split("/")[0]] += 1
for ty in sorted(by):
    print(ty, sorted(by[ty].items()))
print("tables without pools", sum(1 for t in tables.values() if "pools" not in t))
print("random_sequence == minecraft:+path", sum(1 for k, t in tables.items() if t.get("random_sequence") == "minecraft:" + k), "of", len(tables))
trade_sets = tree("trade_set")
print("trade_set", len(trade_sets), "with random_sequence", sum(1 for t in trade_sets.values() if "random_sequence" in t))

section("R5-R8 pools and entries (top level tree: entries then children)")
pool_keys = collections.Counter()
pools_per = collections.Counter()
rolls_form = collections.Counter()
rolls_vals = collections.Counter()
entry_types = collections.Counter()
entry_keys = collections.defaultdict(collections.Counter)
qualities = collections.Counter()
weights = []
inline_tables = collections.Counter()
nodes = 0


def walk_entry(e, fname):
    global nodes
    nodes += 1
    t = e["type"].split(":")[1]
    entry_types[t] += 1
    for k in e:
        entry_keys[t][k] += 1
    if "weight" in e:
        weights.append(e["weight"])
    if "quality" in e:
        qualities[(fname.split("/")[0], fname, e["quality"])] += 1
    if isinstance(e.get("value"), dict):
        inline_tables[fname] += 1
    for c in e.get("children", []):
        walk_entry(c, fname)


for fname, t in tables.items():
    ps = t.get("pools", [])
    pools_per[len(ps)] += 1
    for p in ps:
        pool_keys.update(p.keys())
        r = p["rolls"]
        rolls_form[r["type"] if isinstance(r, dict) else "number"] += 1
        if not isinstance(r, dict):
            rolls_vals[r] += 1
        for e in p["entries"]:
            walk_entry(e, fname)
print("pools", sum(k * v for k, v in pools_per.items()), "per table", sorted(pools_per.items()))
print("pool keys", sorted(pool_keys.items()))
print("rolls forms", sorted(rolls_form.items()), "numeric values", sorted(rolls_vals.items()))
print("entry nodes (entries/children only)", nodes)
print("entry types", sorted(entry_types.items(), key=lambda kv: (-kv[1], kv[0])), "registry", sorted(ENTRY_T))
for t in sorted(entry_keys):
    print("entry fields", t, sorted(entry_keys[t].items()))
print("weights min/max", min(weights), max(weights), "distinct", len(set(weights)), "count", len(weights))
print("qualities", sorted(qualities.items()))
print("files with inline tables in value", sorted(inline_tables.items()), "folder trial_chambers files", sorted(k for k in tables if "trial_chambers" in k))

section("R4 references")
refs = collections.Counter()


def collect_refs(o):
    if isinstance(o, dict):
        for k, v in o.items():
            if k in ("condition", "terms", "term", "requirements"):
                for x in as_list(v):
                    if isinstance(x, str):
                        refs[x] += 1
            collect_refs(v)
    elif isinstance(o, list):
        for v in o:
            collect_refs(v)


for _, d in BASE:
    for v in d.values():
        collect_refs(v)
print(sorted(refs.items()))
print("modifier forms", sorted(collections.Counter(
    type(p["modifier"]).__name__ for t in tables.values() for p in t.get("pools", []) if "modifier" in p).items()),
      "table level", sorted(collections.Counter(type(t["modifier"]).__name__ for t in tables.values() if "modifier" in t).items()))

cond_forms = collections.Counter()


def collect_cond_forms(o, where):
    if isinstance(o, dict):
        for k, v in o.items():
            if k == "condition":
                cond_forms[(where, type(v).__name__)] += 1
            collect_cond_forms(v, "loot_table" if where == "loot_table" else where)
    elif isinstance(o, list):
        for v in o:
            collect_cond_forms(v, where)


for t in tables.values():
    collect_cond_forms(t, "loot_table")
print("forms of the value of key condition in loot tables", sorted(cond_forms.items()))

COND_KEYS = ("condition", "requirements", "terms", "term")
FUNC_KEYS = ("modifier", "on_pass", "on_fail", "functions")


def count_typed(o, key_ctx, typeset, out, fields, in_ctx):
    """Count dicts whose type is in typeset and that sit under one of key_ctx."""
    if isinstance(o, dict):
        t = o.get("type", "")
        short = t.split(":", 1)[1] if isinstance(t, str) and ":" in t else None
        if in_ctx and short in typeset:
            out[short] += 1
            for k in o:
                if k != "type":
                    fields[short][k] += 1
        for k, v in o.items():
            count_typed(v, key_ctx, typeset, out, fields, k in key_ctx or (in_ctx and k in ("terms", "term")))
    elif isinstance(o, list):
        for v in o:
            count_typed(v, key_ctx, typeset, out, fields, in_ctx)


def run_typed(name, key_ctx, typeset, root_is_cond=False):
    out_by = {}
    for base, d in BASE:
        out = collections.Counter()
        fields = collections.defaultdict(collections.Counter)
        for v in d.values():
            count_typed(v, key_ctx, typeset, out, fields, root_is_cond and base == "predicate")
        out_by[base] = (out, fields)
    total = collections.Counter()
    ftotal = collections.defaultdict(collections.Counter)
    for base, (out, fields) in out_by.items():
        total.update(out)
        for k, c in fields.items():
            ftotal[k].update(c)
    print(name, "total", sum(total.values()), "used types", len([k for k in total if total[k]]), "of", len(typeset))
    for t in sorted(typeset):
        per = ",".join(str(out_by[b][0][t]) for b, _ in BASE)
        print(" ", t, total[t], "(loot_table,enchantment,predicate=" + per + ")",
              "fields=" + ",".join(k + ":" + str(v) for k, v in sorted(ftotal[t].items())))
    return total


section("conditions in condition positions (keys condition, requirements, terms, term)")
cond_total = run_typed("conditions", COND_KEYS, COND_T, root_is_cond=True)
section("modifiers in modifier positions (keys modifier, on_pass, on_fail, functions)")
func_total = run_typed("modifiers", FUNC_KEYS, FUNC_T)
print("unused conditions", sorted(c for c in COND_T if not cond_total[c]))
print("unused modifiers", sorted(f for f in FUNC_T if not func_total[f]))
print("matched with block predicate files: match_block in predicate/", sum(1 for v in preds.values() if v.get("type") == "minecraft:match_block"),
      "match_tool in predicate/", sum(1 for v in preds.values() if v.get("type") == "minecraft:match_tool"))

section("R14 condition and modifier types per table type (loot_table folder only)")
need = collections.defaultdict(lambda: [collections.Counter(), collections.Counter()])
for k, t in tables.items():
    tt = t["type"].split(":")[1]
    c1 = collections.Counter()
    c2 = collections.Counter()
    f1 = collections.defaultdict(collections.Counter)
    count_typed(t, COND_KEYS, COND_T, c1, f1, False)
    count_typed(t, FUNC_KEYS, FUNC_T, c2, f1, False)
    need[tt][0].update(c1)
    need[tt][1].update(c2)
for tt in sorted(need):
    print(tt, "conditions", sorted(need[tt][0]), "modifiers", sorted(need[tt][1]))

section("R6-R10 numbers")
set_count = collections.Counter()
lim = collections.Counter()


def collect_numbers(o):
    if isinstance(o, dict):
        t = o.get("type")
        if t == "minecraft:set_count":
            c = o["count"]
            set_count[c["type"].split(":")[1] if isinstance(c, dict) else "constant"] += 1
        if t == "minecraft:limit_count":
            lim[json.dumps(o["limit"], sort_keys=True)] += 1
        for v in o.values():
            collect_numbers(v)
    elif isinstance(o, list):
        for v in o:
            collect_numbers(v)


for v in tables.values():
    collect_numbers(v)
print("set_count count forms", sorted(set_count.items()), "limit_count", sorted(lim.items()))

section("R27 fishing")
fishing = tables["gameplay/fishing"]
for e in fishing["pools"][0]["entries"]:
    print(e["value"], e.get("weight"), e.get("quality"), json.dumps(e.get("condition"), sort_keys=True))
fish = tables["gameplay/fishing/fish"]
print("fish weights", [(e["name"], e.get("weight")) for e in fish["pools"][0]["entries"]])

section("R28 archaeology")
for k in ("archaeology/desert_well", "archaeology/trail_ruins_rare"):
    es = tables[k]["pools"][0]["entries"]
    print(k, len(es), [(e.get("name"), e.get("weight", 1)) for e in es])

section("R30-R32 random chances and table_bonus")
chances = collections.Counter()
tb = collections.Counter()
rcb = []


def collect_chances(o):
    if isinstance(o, dict):
        t = o.get("type")
        if t == "minecraft:random_chance":
            chances[json.dumps(o["chance"], sort_keys=True)] += 1
        if t == "minecraft:table_bonus":
            tb[(o["enchantment"], json.dumps(o["chances"]))] += 1
        if t == "minecraft:random_chance_with_enchanted_bonus":
            rcb.append(json.dumps({k: o[k] for k in sorted(o) if k != "type"}, sort_keys=True))
        for v in o.values():
            collect_chances(v)
    elif isinstance(o, list):
        for v in o:
            collect_chances(v)


for _, d in BASE:
    for v in d.values():
        collect_chances(v)
print("random_chance", sorted(chances.items(), key=lambda kv: (-kv[1], kv[0])))
print("table_bonus", sorted(tb.items(), key=lambda kv: (-kv[1], kv[0])))
print("random_chance_with_enchanted_bonus", sorted(collections.Counter(rcb).items()))

section("R42-R46 modifier parameters")
ab = collections.Counter()
eci = collections.Counter()
for _, d in BASE:
    def go(o):
        if isinstance(o, dict):
            t = o.get("type")
            if t == "minecraft:apply_bonus":
                ab[(o["formula"], json.dumps(o.get("parameters"), sort_keys=True))] += 1
            if t == "minecraft:enchanted_count_increase":
                eci[(json.dumps(o["count"], sort_keys=True), o.get("limit"))] += 1
            for v in o.values():
                go(v)
        elif isinstance(o, list):
            for v in o:
                go(v)
    for v in d.values():
        go(v)
print("apply_bonus", sorted(ab.items()))
print("enchanted_count_increase", sorted(eci.items(), key=str))

section("R57 entity predicate keys (key occurrences anywhere, per directory)")
KEYS = ["minecraft:entity_type", "minecraft:location", "minecraft:components", "minecraft:flags", "minecraft:equipment",
        "minecraft:vehicle", "minecraft:type_specific/player", "minecraft:distance", "minecraft:stepping_on",
        "minecraft:type_specific/lightning", "minecraft:type_specific/sheep", "minecraft:movement_affected_by",
        "minecraft:movement", "minecraft:periodic_tick", "minecraft:type_specific/cube_mob",
        "minecraft:type_specific/raider", "minecraft:type_specific/fishing_hook"]
per = {}
for base, d in BASE + [("advancement", tree("advancement"))]:
    c = collections.Counter()
    for v in d.values():
        keys_anywhere(v, c)
    per[base] = c
for k in KEYS:
    row = [per[b][k] for b in ("loot_table", "enchantment", "predicate", "advancement")]
    print(k, "LEP", sum(row[:3]), "(loot_table,enchantment,predicate,advancement=" + ",".join(map(str, row)) + ")")
print("entity_sub_predicate_type registry", len(reg("entity_sub_predicate_type")))

section("R59-R60 location and damage source predicate keys (LEP)")
loc = collections.Counter()
dmg = collections.Counter()
item_pred = collections.Counter()
predicates_sub = collections.Counter()
for _, d in BASE:
    def go2(o):
        if isinstance(o, dict):
            t = o.get("type")
            if t == "minecraft:location_check" and isinstance(o.get("predicate"), dict):
                loc.update("location_check." + k for k in o["predicate"])
            if t == "minecraft:damage_source_properties" and isinstance(o.get("predicate"), dict):
                dmg.update(o["predicate"].keys())
            if "minecraft:location" in o and isinstance(o["minecraft:location"], dict):
                loc.update("minecraft:location." + k for k in o["minecraft:location"])
            if t in ("minecraft:match_tool",) and isinstance(o.get("predicate"), dict):
                item_pred.update("match_tool." + k for k in o["predicate"])
            if "item_filter" in o and isinstance(o["item_filter"], dict):
                item_pred.update("item_filter." + k for k in o["item_filter"])
            if "predicates" in o and isinstance(o["predicates"], dict):
                predicates_sub.update(o["predicates"].keys())
            for v in o.values():
                go2(v)
        elif isinstance(o, list):
            for v in o:
                go2(v)
    for v in d.values():
        go2(v)
print("location predicate keys", sorted(loc.items()))
print("damage source predicate keys", sorted(dmg.items()))
print("item predicate keys", sorted(item_pred.items()))
print("predicates sub-keys", sorted(predicates_sub.items()))
print("data_component_predicate_type", sorted(reg("data_component_predicate_type")))
print("smokey in LEP", per["loot_table"]["smokey"] + per["enchantment"]["smokey"] + per["predicate"]["smokey"])

section("R61 named predicates")
for k, v in preds.items():
    print(k, json.dumps(v, sort_keys=True))
print("references to predicates", sorted(refs.items()))

section("R13 context_key_set order")
print(reg("context_key_set"))
ck = load(os.path.join(REP, "registries.json"))["minecraft:context_key_set"]["entries"]
print([(k.split(":")[1], v["protocol_id"]) for k, v in ck.items()])

section("R50 providers registry counts")
for r in ("context_int_provider_type", "context_float_provider_type", "int_provider_type", "float_provider_type"):
    print(r, len(reg(r)))
print("provider files", len(tree("context_int_provider")), len(tree("context_float_provider")))
print("loot_nbt_provider_type", len(reg("loot_nbt_provider_type")), "loot_score_provider_type", len(reg("loot_score_provider_type")),
      "slot_source_type", sorted(reg("slot_source_type")))
print("item_modifier files", len(tree("item_modifier")) if os.path.isdir(os.path.join(DATA, "item_modifier")) else 0)

section("R72 loot command")
cmds = load(os.path.join(REP, "commands.json"))
print(json.dumps(sorted(cmds["children"]["loot"]["children"]), sort_keys=True))
for k, v in sorted(cmds["children"]["loot"]["children"].items()):
    print(k, sorted(v.get("children", {})))
```

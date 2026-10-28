# Objets, composants de données, inventaires et menus

Statut : brouillon
Jalon : M6
Risque : B
Règles : R1 à R47 (R9 et R37 à R47 sont découpées en sous-règles citables : R9a, R37a, R40a...)

## Périmètre

Couvre, pour `engine/item` (et le contenu des conteneurs vu par `engine/game`, ADR 0005) :

- le registre des objets et leurs composants par défaut ;
- la pile d'objets : identité, quantité, composants (valeurs par défaut plus modifications propres à l'instance), égalité, fusion, fractionnement ;
- la sérialisation : forme NBT (inventaires, block entities), forme JSON des données (résultats de recette, tables de butin), forme textuelle des commandes ;
- le catalogue des 122 types de composants et la forme de chacun ;
- les conteneurs (tableaux de slots), la disposition NBT des block entities à inventaire, les espaces d'indices de slots ;
- les menus (25 types), leurs slots et les opérations de clic (déplacer, fractionner, glisser, déplacement rapide).

Ne couvre pas :

- les recettes, la cuisson, les carburants, l'alchimie (`item-recipes.md`) ;
- les tables de butin, prédicats d'objet, modificateurs (`loot-tables.md`) ;
- les enchantements, la table d'enchantement, l'enclume, la meule (`item-enchantments.md`) ;
- le codec réseau des piles et des composants (jalon M8, paquets) ;
- l'entité "objet au sol" (âge, fusion, ramassage) : spec entités, jalon M7 ;
- l'effet de jeu des composants (nourriture, outil, armure, armes) autre que leur forme de donnée : specs M5 et M7. Seule la forme et, quand elle est documentée, la sémantique de la valeur sont décrites ici ;
- le rendu (modèles, tooltips, textures) : jalon M9.

## Sources

- **Données extraites** (`.cache/reference/26.3/`, chemin du dossier de données de la machine de développement) :
  - `generated/reports/registries.json` : registres `item` (1658 entrées), `data_component_type` (122), `menu` (25), `consume_effect_type` (5), `data_component_predicate_type` (15), `slot_source_type` (6) ;
  - `generated/reports/minecraft/components/item/<id>.json` : composants par défaut de chacun des 1658 objets (23 072 entrées de composants, recomptées en C par `components_stats.py`) ;
  - `generated/reports/commands.json` : arguments `minecraft:item_stack`, `minecraft:item_predicate`, `minecraft:item_slot`, `minecraft:slot_source` des commandes `give`, `clear`, `item`, `loot` ;
  - `resources/data/minecraft/structure/**/*.nbt` (1511 fichiers) : block entities à inventaire dans les gabarits de structures, lus avec un lecteur NBT minimal indépendant ;
  - `resources/data/minecraft/{recipe,loot_table,villager_trade,advancement}/**` pour les formes JSON de piles et de composants.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Java Edition protocol/Inventory" (disposition des slots par type de fenêtre), "Inventory" (comportement des clics), "Item modifier", "Loot table", "Predicate".
- **Rapports locaux** (indices, ADR 0002) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, parties "Composants d'objets" (chapitres 134 à 136) et "Inventaires, menus et transactions" (chapitre 176). Ils ne donnent que des listes de comportements à reproduire ("à spécifier") : aucune règle S ci-dessous n'en vient sans recoupement.
- **Sondes de l'oracle** : aucune exécutée à ce jour. Les sondes proposées sont en fin de document (PI1 à PI12).

Reproduction : tous les dénombrements D et C de cette spec sortent du script `components_stats.py` de l'annexe, lancé avec `python -I components_stats.py "$REF"` (`REF` = dossier `.cache/reference/26.3`). Les sections de sa sortie portent le numéro de la règle qu'elles étayent (`== R7 ...`). L'empreinte SHA-256 de la sortie complète est épinglée en annexe : un écart signale un changement des données de référence ou du script. Aucune table de données de référence n'est recopiée ici (AGENTS.md) ; les tests lisent les fichiers extraits à l'exécution.

Convention de preuve : **D** = valeur lue dans un fichier extrait ; **C** = propriété calculée sur ces fichiers (reproductible, commande donnée) ; **M** = observé sur l'oracle (non utilisé : oracle pas encore lancé) ; **S** = documentation publique seule ou hypothèse. Chaque règle S porte un **comportement provisoire** à implémenter jusqu'à ce que la sonde nommée tranche.

## Règles

### Registre d'objets et composants par défaut

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le registre `item` compte 1658 entrées ; l'entrée par défaut est `minecraft:air`. | D |
| R2 | Chaque objet possède une table de composants par défaut ("prototype"), fixe, fournie par les données. Les 1658 fichiers `components/item/<id>.json` sont une source exacte : le moteur charge ces tables et ne les code pas en dur. | D |
| R3 | Treize composants sont présents dans le prototype des 1658 objets, `air` compris : `attack_animation`, `attribute_modifiers`, `break_sound`, `enchantments`, `interact_animation`, `item_model`, `item_name`, `lore`, `max_stack_size`, `rarity`, `repair_cost`, `tooltip_display`, `use_effects`. Aucun autre composant n'est universel. | C |
| R4 | Valeurs les plus fréquentes des treize composants universels (les exceptions sont lisibles dans les fichiers : 7 lances ont une `attack_animation` et une `use_effects` non vides, 93 objets ont des `attribute_modifiers`) : `attack_animation` `{}`, `interact_animation` `{}`, `tooltip_display` `{}`, `use_effects` `{}`, `enchantments` `{}`, `lore` `[]`, `attribute_modifiers` `[]`, `repair_cost` 0, `rarity` "common" (1543 objets ; "uncommon" 78, "rare" 18, "epic" 19), `break_sound` `minecraft:entity.item.break` (1656 objets ; `minecraft:item.shield.break` pour le bouclier et `minecraft:item.wolf_armor.break` pour l'armure de loup), `item_model` = identifiant de l'objet, `item_name` = `{"translate": "<préfixe>.minecraft.<id>"}` avec le préfixe `block` pour 1126 objets (objets-blocs, dont `air`) et `item` pour 532. Commande : section `R2-R5` de `components_stats.py`. | C |
| R5 | `max_stack_size` : 64 pour 1342 objets, 16 pour 68, 1 pour 248 (section `R2-R5`). Les 84 objets dotés de `max_damage` (et de `damage` 0) ont tous `max_stack_size` 1 ; 164 autres objets sans durabilité ont aussi 1. Invariant à tester : `max_damage` présent implique `max_stack_size` 1. | C |
| R6a | Composants présents à `{}` dans les prototypes : `consumable` `{}` (30 objets), `weapon` `{}` (16), `glider` `{}`, `bucket_entity_data` `{}`, `map_decorations` `{}` (hors les composants universels de R4). Les valeurs implicites de codec ne sont pas développées dans les fichiers. | D |
| R6b | Un composant présent à `{}` est significatif : il signale la présence du comportement avec ses valeurs implicites de codec, et n'est pas équivalent à un composant absent. Provisoire : deux états distincts, y compris pour l'égalité de piles (R17) et l'écriture NBT (R27). Sondes PI2 et PI3. | S |
| R7 | Tableau de dénombrement (C) : la commande de reproduction est `python -I components_stats.py $REF` (script de l'annexe en fin de document). Présence des composants non universels : `cooking_fuel` 347, `compostable` 122, `entity_data` 88, `equippable` 84, `damage` 84, `max_damage` 84, `enchantable` 77, `repairable` 75, `container` 45, `food` 44, `weapon` 44, `consumable` 43, `tool` 38, `sign_text_front`/`sign_text_back` 26 chacun, `provides_pottery_pattern` 23, `jukebox_playable` 22, `block_transformer` 21, `map_decorations` 17, `damage_resistant` 17, `banner_patterns` 17, `bundle_contents` 17, `cushion/color` 16, `dye` 16, `block_state` 12, `provides_trim_material` 11, `provides_banner_patterns` 10, `bucket_entity_data` 7, `use_remainder` 7, `attack_range` 7, `damage_type` 7, `kinetic_weapon` 7, `minimum_attack_charge` 7, `piercing_weapon` 7, `enchantment_glint_override` 7, `villager_food` 4, `mob_visibility` 4, `potion_contents` 4, `chicken/variant` 3, `use_cooldown` 3, `bees` 2, `potion_duration_scale` 2, et 1 objet pour chacun de `brewing_fuel`, `charged_projectiles`, `debug_stick_state`, `pot_decorations`, `glider`, `stored_enchantments`, `fireworks`, `instrument`, `recipes`, `ominous_bottle_amplifier`, `blocks_attacks`, `suspicious_stew_effects`, `death_protection`, `writable_book_content`. 69 des 122 types apparaissent dans au moins un prototype. | C |
| R8 | Les 53 types absents de tous les prototypes ne peuvent être décrits que par la documentation publique (forme) : ils portent le niveau S (R9b). Quatorze d'entre eux sont vus, entre guillemets, dans les données hors prototype (section `R8` du script : dossiers `loot_table`, `villager_trade`, `advancement`, `recipe`, `trade_set`) : `cat/variant`, `container_loot`, `custom_name`, `dyed_color`, `frog/variant`, `lock`, `map_id`, `mooshroom/variant`, `note_block_sound`, `profile`, `sheep/color`, `trim`, `villager/variant`, `wolf/variant`. La présence du nom confirme le nom du type ; la forme de la valeur reste S, sauf lecture directe dans le fichier concerné. | C |

### Catalogue des 122 types de composants

| Réf | Règle | Preuve |
|---|---|---|
| R9 | Le registre `data_component_type` compte 122 types. Mesure (section `R9` de `components_stats.py`) : 69 types sont portés par au moins un prototype, 53 par aucun. Les 69 types observés ont pour forme JSON : objet 39, chaîne 13, liste 9, entier 5, flottant 2, booléen 1. Le script imprime, pour chaque type observé, le genre de valeur et l'union des champs avec leur effectif ; ce tableau complet n'est pas recopié dans le dépôt (AGENTS.md : aucune donnée de référence dans git) et le test le relit à l'exécution dans les fichiers extraits. Le catalogue sert de liste d'acceptation du décodage (R10) et de cible de PI3. | D |
| R9a | Quatre exemples de forme (D, valeur la plus fréquente) : `damage` entier `0` (84 objets) ; `rarity` chaîne `"common"` (1658) ; `food` objet à champs `nutrition` et `saturation` (44) ; `container` liste `[]` (45). | D |
| R9b | Les 53 types absents de tous les prototypes n'ont de forme que par la documentation publique (S, à confirmer par PI3). Trois familles. Variantes d'entité, identifiant ou chaîne (21) : `axolotl/variant`, `cat/variant`, `cat/sound_variant`, `chicken/sound_variant`, `cow/variant`, `cow/sound_variant`, `fox/variant`, `frog/variant`, `horse/variant`, `llama/variant`, `mooshroom/variant`, `painting/variant`, `parrot/variant`, `pig/variant`, `pig/sound_variant`, `rabbit/variant`, `villager/variant`, `wolf/variant`, `wolf/sound_variant`, `zombie_nautilus/variant`, `salmon/size`. Couleurs et motifs, chaîne (8) : `base_color`, `cat/collar`, `wolf/collar`, `sheep/color`, `shulker/color`, `tropical_fish/base_color`, `tropical_fish/pattern`, `tropical_fish/pattern_color`. Données libres ou composées (24) : `custom_data` (compound NBT libre), `block_entity_data` (compound NBT avec `id`), `custom_model_data` (`floats`, `flags`, `strings`, `colors`), `custom_name` (texte), `profile`, `lock`, `container_loot` (`loot_table`, `seed`), `lodestone_tracker`, `trim` (`material`, `pattern`), `dyed_color`, `map_id` (entier), `firework_explosion`, `written_book_content`, `note_block_sound`, `tooltip_style`, `additional_trade_cost` (entier), `can_break`, `can_place_on` (prédicats de bloc), `unbreakable`, `creative_slot_lock`, `intangible_projectile`, `map_post_processing` (transitoire, à vérifier), `sulfur_cube_content` et `waxed` (aucune documentation publique trouvée : forme inconnue). | S |
| Réf | Règle | Preuve |
|---|---|---|
| R10 | Le décodage d'un composant se fait par son identifiant de registre : un identifiant inconnu dans une table de composants est une erreur de décodage de la donnée entière (jamais ignoré silencieusement). Provisoire. | S |
| R11 | Valeurs observées de `consumable` : `{}` ou `{"on_consume_effects":[...]}` ; les effets de consommation sont de 5 types (`apply_effects`, `remove_effects`, `clear_all_effects`, `teleport_randomly`, `play_sound`), exemple `chicken` : `apply_effects` avec `probability` 0.3 et effet `hunger` 600 ticks ; `death_protection` (totem) porte les `death_effects` `clear_all_effects` puis `apply_effects` (régénération amplificateur 1 durée 900, absorption 1 durée 100, résistance au feu durée 800). | D |
| R12 | Formes JSON observées que le décodeur strict accepte (D, union des champs vus par prototype : section `R12` du script, effectif entre parenthèses ; un champ présent dans tous les prototypes qui portent le composant est requis dans les données, les autres sont optionnels avec défaut de codec). `food` {`nutrition` 44, `saturation` 44, `can_always_eat` 5} ; `villager_food` {`nutrition`} ; `tool` {`rules` 38, `damage_per_block` 9, `can_destroy_blocks_in_creative` 9} avec règle {`blocks` 81, `speed` 53, `correct_for_drops` 64} ; `weapon` {`item_damage_per_attack` 28, `disable_blocking_for_seconds` 7} ; `consumable` {`consume_seconds` 2, `animation` 4, `sound` 4, `has_consume_particles` 4, `on_consume_effects` 11} ; `equippable` {`slot` 84, `equip_sound` 75, `asset_id` 75, `allowed_entities` 45, `can_be_sheared` 45, `shearing_sound` 45, `swappable` 9, `equip_on_interact` 22, `damage_on_hurt` 12, `camera_overlay` 1} ; `attribute_modifiers` liste de {`type`, `id`, `amount`, `operation`, `slot` (184 chacun), `display` 8} ; `repairable` {`items`} (étiquette ou liste) ; `enchantable` {`value`} (11 valeurs distinctes : 1, 5, 8, 9, 10, 12, 13, 14, 15, 22, 25) ; `damage_resistant` {`types`} ; `blocks_attacks` {`block_delay_seconds`, `block_sound`, `bypassed_by`, `disabled_sound`, `item_damage`} ; `attack_range` {`hitbox_margin`, `max_creative_reach`, `max_reach`, `min_creative_reach`, `min_reach`, `mob_factor`} ; `kinetic_weapon` {`damage_conditions`, `damage_multiplier`, `delay_ticks`, `dismount_conditions`, `forward_movement`, `hit_sound`, `knockback_conditions`, `sound`} ; `piercing_weapon` {`hit_sound`, `sound`} ; `use_effects` {`can_sprint`, `interact_vibrations`, `speed_multiplier`} (7 non vides) ; `attack_animation` {`type`, `duration`} (7 non vides) ; `death_protection` {`death_effects`} ; `mob_visibility` {`targeting_entity_types`, `visibility`} ; `use_cooldown` {`seconds`} ; `fireworks` {`flight_duration`} ; `cooking_fuel` {`burn_time`, `speed_multiplier`} (références à des fournisseurs de données : `item-recipes.md`) ; `brewing_fuel` {`speed_multiplier`, `uses`}. Opérations d'attribut vues sur les 1658 prototypes : `add_value` 176 et `add_multiplied_total` 8 seulement ; `add_multiplied_base` n'apparaît dans aucun prototype (elle est dans les enchantements, `item-enchantments.md` R22) et sa forme dans un prototype est donc S. Montants flottants conservés au bit près (`-2.799999952316284`). Les champs ayant une valeur par défaut de codec sont optionnels en entrée ; un champ absent de cette union est une erreur de décodage (provisoire, PI3). | D (champs, opérations) ; S (rejet) |
| R13 | Les nombres JSON flottants des composants se lisent en double puis se convertissent au type du champ (Float pour les montants et vitesses). Valeur sentinelle observée : `speed` 3.4028235e+38 (maximum d'un Float, "minage instantané" des épées sur certains blocs). | D |

### Pile d'objets

| Réf | Règle | Preuve |
|---|---|---|
| R14 | Une pile = (objet, quantité, modifications de composants). Les composants effectifs sont le prototype de R2 recouvert par les modifications ; une modification peut ajouter ou remplacer une valeur, ou retirer un composant du prototype. | S |
| R15 | Pile vide : objet `air` ou quantité inférieure ou égale à 0. Toutes les piles vides sont équivalentes (égalité, sérialisation en tant qu'absence). Provisoire. | S |
| R16 | Bornes de quantité, données des arguments de commandes : `give` accepte une quantité minimale de 1 ; `item replace ... with <objet> <quantité>` et `item fill ... with` bornent la quantité entre 1 et 99 (D `commands.json`). Provisoire (S) : la quantité maximale d'une pile est `max_stack_size` des composants effectifs, dont la plage valide est 1 à 99 ; `give` d'une quantité supérieure répartit en plusieurs piles. Sonde PI1. | D (bornes d'arguments) ; S (reste) |
| R17 | Composants effectifs : le remplacement d'un composant par une valeur égale à celle du prototype est permis. Provisoire : la forme normalisée d'une pile supprime la modification devenue inutile (au bit près pour les flottants), si bien que deux piles de mêmes composants effectifs ont la même forme sérialisée. Sonde PI2. | S |
| R18 | Égalité de contenu de deux piles ("peut fusionner") : même objet et mêmes composants effectifs, la quantité étant ignorée. L'égalité de composants est structurelle (valeur par valeur, flottants comparés par bits comme `core-nbt` R37), pas par représentation de la modification. Un composant retiré du prototype et un composant absent du prototype sont équivalents. Sonde PI5. | S |
| R19 | Fusion : une pile peut recevoir des unités d'une pile de contenu égal jusqu'à son `max_stack_size` ; l'excédent reste dans la pile source. Deux piles de contenus différents ne fusionnent jamais, y compris si seuls les enchantements, le nom, le lore ou la durabilité diffèrent. Sonde PI5. | S |
| R20 | Fractionnement : retirer n unités crée une pile de même contenu ; la quantité retirée est bornée par la quantité disponible. Aucun composant n'est perdu au fractionnement. | S |
| R21 | Un objet à durabilité (`max_damage` présent) a `damage` entier dans `[0, max_damage]` ; il se brise lorsque `damage` atteint `max_damage` par usure. `unbreakable` empêche l'usure. Les formules d'usure par enchantement (`item_damage`) sont dans `item-enchantments.md`. `set_damage` de butin travaille en fraction (voir `loot-tables.md`). Provisoire. | S |
| R22 | `enchantments` et `stored_enchantments` : dictionnaire identifiant d'enchantement vers niveau entier. Forme observée dans les modificateurs `set_enchantments` : `{"minecraft:sharpness": 1}` (D) ; la plage de niveau acceptée est 1 à 255, un niveau inférieur à 1 retire l'entrée (S, documentation de `set_enchantments`). | D (forme) ; S (plage) |
| R23 | `container`, `bundle_contents`, `charged_projectiles` portent des piles imbriquées. Forme de `container` : liste de `{slot, item}` (prototype `[]` pour les 45 objets conteneurs). Les piles imbriquées suivent R14 à R20. Profondeur d'imbrication maximale et traitement d'un bundle dans un bundle : S, sonde PI4. | D (prototype) ; S (reste) |

### Formes de sérialisation d'une pile

| Réf | Règle | Preuve |
|---|---|---|
| R24 | Forme NBT d'une pile dans un inventaire de block entity : Compound `{id: String, count: Int?, Slot: Byte?, components: Compound?}`. Observé sur les 1511 structures (section `NBT` du script) : 44 piles. 21 sont dans des listes `Items` (toutes avec `count` de type Int et `Slot` de type Byte), 3 sont isolées (`item` d'un pot décoré, `key_item` de deux coffres-forts, avec `count`), 20 sont des piles `{id}` sans `count` ni `Slot` (compound `sherds` des pots décorés). Soit 24 piles avec `count` et 20 sans. `components` n'est présent que sur 4 piles. Une pile `{id}` sans `count` est donc lue par le jeu (D) ; sa valeur par défaut est S (R26). Exemple : `{count: 1, Slot: 0b, components: {"minecraft:potion_contents": {potion: "minecraft:strong_healing"}}, id: "minecraft:potion"}`. | D |
| R25 | Les listes `Items` de block entity sont creuses : seules les piles non vides sont écrites, chacune avec son `Slot`, et une liste vide `[]` est écrite telle quelle (sections `NBT` : 206 des 210 coffres porteurs de `Items`, les 128 tonneaux, 27 fours sur 28, 9 alambics sur 11, tous les hauts fourneaux, fumoirs et feux de camp). Slots occupés observés : four `Slot` 1 (combustible) et 2 (sortie) ; alambic `Slot` 0, 1 et 2 ; distributeur `Slot` 4 ; coffres, indices 4 à 23. | D |
| R26 | Lecture d'une pile NBT : `count` absent vaut 1 ; `id` inconnu produit une pile vide (la pile est ignorée) ; `components` absent vaut "aucune modification". Provisoire. Sonde PI6. | S |
| R27 | Écriture d'une pile NBT : `components` n'est écrit que si la pile a au moins une modification (R17). Un composant retiré du prototype s'écrit sous la clé `!<id>` avec la valeur `{}`. Provisoire. Sonde PI2. | S |
| R28 | Forme JSON d'une pile dans les données (résultat de recette, sortie d'alchimie, décorations de pot, offre) : objet `{id, count?, components?}`. Un `count` absent signifie 1 (1294 résultats de recette ou sorties d'alchimie sans `count`, 727 avec). Un objet vide `{}` est accepté comme résultat de 2 recettes (`map_cloning`, `map_extending`) et signifie "résultat dérivé de l'entrée" (voir `item-recipes.md`). Le champ `components` suit R9 et accepte la clé préfixée `!`. | D (forme) ; S (défaut 1) |
| R29 | Forme textuelle des commandes (argument `minecraft:item_stack`) : `<id>[<composant>=<valeur>, !<composant>, ...]` avec des valeurs en SNBT (`core-nbt` R41 à R50). L'argument `minecraft:item_predicate` est une forme distincte (étiquettes `#`, motif `*`, correspondance partielle de composants) : voir `loot-tables.md`. Provisoire. Sonde PI2. | S |

### Conteneurs et block entities à inventaire

| Réf | Règle | Preuve |
|---|---|---|
| R30 | Un conteneur est un tableau de slots de taille fixe, chaque slot étant une pile (éventuellement vide). Chaque slot a une quantité maximale propre (min de `max_stack_size` de la pile et de la limite du conteneur). Provisoire pour la limite (coffre 64). | S |
| R31 | Disposition NBT des block entities à inventaire, relevée sur les gabarits de structure (D ; nombre de block entities de ce type entre parenthèses). Coffre (473), coffre piégé, tonneau (226) : `Items`, `LootTable` (`LootTableSeed` n'apparaît pas dans les gabarits) ; `components` présent sur 210 coffres et 204 tonneaux (compound, vide dans les gabarits). Four (28), haut fourneau (11), fumoir (9) : `Items` (slots 0 ingrédient, 1 combustible, 2 sortie), `lit_time_remaining` Short, `lit_total_time` Short, `cooking_time_spent` Short, `cooking_total_time` Short, `RecipesUsed` (Compound identifiant de recette vers Int). Alambic (11) : `Items` (slots 0 à 4), `BrewTime` Short, `Fuel` Byte. Feu de camp (128) : `Items`, `CookingTimes` Int_Array de 4, `CookingTotalTimes` Int_Array de 4. Entonnoir (1) : `Items`, `TransferCooldown` Int. Distributeur (7) : `Items`, `LootTable`. Pot décoré (5) : `sherds` (compound `back`, `front`, `left`, `right`, chacun une pile `{id}`), `LootTable`, `item`. Coffre-fort (2) : `config` (`key_item` pile, `loot_table`). Lutrin (11) : aucun champ au repos. | D |
| R32 | `LootTable` est un String (identifiant de table), `LootTableSeed` un Long ; leur absence signifie conteneur matérialisé ou sans butin. Dans les gabarits, 350 coffres sur 473 portent `LootTable` (et 207 tonneaux sur 226) ; 210 coffres portent `Items`, mais 206 de ces listes sont vides, de sorte que seuls 4 coffres ont un contenu écrit. `LootTableSeed` n'apparaît dans aucun gabarit. La matérialisation (déballage) est décrite dans `loot-tables.md`. | D (forme) ; S (sémantique) |
| R33 | Dans un four, la clé de `RecipesUsed` est l'identifiant de la recette et la valeur le nombre de sorties à rendre en expérience ; exemple observé : `{"minecraft:deepslate": 24}` avec 24 piles d'ardoise des abîmes en sortie. L'expérience est calculée à partir de la recette (`item-recipes.md`). | D (forme) ; S (sens) |

### Espaces d'indices de slots

| Réf | Règle | Preuve |
|---|---|---|
| R34 | Inventaire du joueur en NBT (`Inventory`) : `Slot` Byte, 0 à 8 barre rapide, 9 à 35 inventaire principal, 100 pieds, 101 jambes, 102 torse, 103 tête, -106 main secondaire. Hypothèse H-inv-a (convention historique). Hypothèse H-inv-b : en 26.3 l'équipement est écrit dans un compound `equipment` distinct et `Inventory` ne garde que 0 à 35. Sonde PI7 : `/data get entity @s Inventory` et `/data get entity @s equipment` après remplissage par `/item replace`. | S |
| R35 | Désignation des slots par les commandes (`item replace`, `item modify`, `loot replace`, arguments `minecraft:item_slot` et `minecraft:slot_source`) : `container.<0..53>`, `hotbar.<0..8>`, `inventory.<0..26>`, `enderchest.<0..26>`, `weapon.mainhand`, `weapon.offhand`, `armor.head`, `armor.chest`, `armor.legs`, `armor.feet`, `player.crafting.<0..3>`, `player.cursor`, `horse.<...>`, `villager.<0..7>` ; la liste exacte pour 26.3 est S. Le registre `slot_source_type` (D) compte 6 types : `contents`, `empty`, `filtered`, `group`, `limit_slots`, `slot_range` ; le registre chargeable `slot_source` n'a aucun fichier vanilla. Sonde PI8. | D (registre) ; S (noms) |
| R36 | Fenêtre du joueur sans conteneur externe : 0 résultat de fabrication, 1 à 4 grille 2x2 (indice 1 + x + 2y), 5 tête, 6 torse, 7 jambes, 8 pieds, 9 à 35 inventaire principal, 36 à 44 barre rapide, 45 main secondaire. | S |

### Menus

Le registre `menu` compte 25 types (D) : `generic_9x1` à `generic_9x6`, `generic_3x3`, `crafter_3x3`, `crafting`, `anvil`, `beacon`, `blast_furnace`, `brewing_stand`, `cartography_table`, `enchantment`, `furnace`, `grindstone`, `hopper`, `lectern`, `loom`, `merchant`, `shulker_box`, `smithing`, `smoker`, `stonecutter`. Il n'y a pas de menu "joueur" ni de menu de cheval dans ce registre.

| Réf | Règle | Preuve |
|---|---|---|
| R37a | Ordre des indices d'un menu : les slots propres du menu d'abord, puis 27 slots d'inventaire principal, puis 9 de barre rapide. Exception : `lectern`, qui n'inclut pas l'inventaire du joueur. Métrique : le nombre total de slots de chaque type de menu est l'égalité des indices propres (R37b à R37d) plus 36 (0 pour `lectern`). Source : page publique citée en Sources. | S |
| R37b | Indices propres des menus de coffre et de grille : `generic_9xN` 0 à 9N-1 ; `shulker_box` 0 à 26 ; `generic_3x3` 0 à 8 (x + 3y) ; `crafter_3x3` 0 à 8 puis le résultat en 45 (après le joueur) ; `crafting` 0 résultat, 1 à 9 grille (1 + x + 3y). | S |
| R37c | Indices propres des machines : `furnace`, `blast_furnace`, `smoker` 0 ingrédient, 1 combustible, 2 sortie ; `brewing_stand` 0 à 2 bouteilles, 3 ingrédient, 4 poudre de blaze ; `enchantment` 0 objet, 1 lapis-lazuli ; `anvil` 0 premier objet, 1 second, 2 résultat ; `grindstone` 0 premier, 1 second, 2 résultat ; `beacon` 0 paiement ; `hopper` 0 à 4. | S |
| R37d | Indices propres des autres menus : `merchant` 0 et 1 entrées, 2 résultat ; `lectern` 0 livre ; `loom` 0 bannière, 1 teinture, 2 motif, 3 résultat ; `smithing` 0 modèle, 1 base, 2 ajout, 3 résultat ; `cartography_table` 0 carte, 1 papier, 2 sortie ; `stonecutter` 0 entrée, 1 résultat. | S |
| R38a | Les slots de résultat (fabrication, four, enclume, meule, commerce, métier à tisser, forge, table de cartographie, tailleur de pierre) n'acceptent aucun dépôt. Métrique : pour chaque slot de résultat de R37b à R37d, `peutPlacer(pile)` est faux pour toute pile. Provisoire. | S |
| R38b | Les slots d'ingrédient et de combustible d'un four n'acceptent que ce que les recettes de cuisson ou `cooking_fuel` admettent (`item-recipes.md` R25, R32). Métrique : table (objet, slot) vers accepté ou refusé relevée par PI9 sur un échantillon d'objets (un par fournisseur de R32, un objet cuisable, un objet neutre). Provisoire. | S |
| R38c | Les slots d'armure de la fenêtre joueur n'acceptent que les objets dont `equippable.slot` correspond au slot. Métrique : pour les 84 prototypes portant `equippable`, accepté dans le slot nommé par `slot` et refusé dans les trois autres slots d'armure. Provisoire. | S |
| R39a | Seul le menu ouvert reçoit les clics. Provisoire. | S |
| R39b | Fermeture d'un menu avec une pile au curseur : la pile est rendue à l'inventaire du joueur, ou jetée au sol si l'inventaire est plein. Provisoire. Sonde PI9. | S |

### Opérations de clic

Les opérations sont décrites indépendamment des numéros de paquets (M8). Les modes de clic du protocole 777 se rapportent aux six opérations ci-dessous (clic normal, clic maj, touche numérique ou main secondaire, clic milieu en créatif, jeter, glisser, double clic) ; la table de correspondance exacte est du domaine du paquet (M8).

Métrique commune des règles R40 à R47 : chaque sous-règle est testée par des vecteurs "état initial des slots et du curseur, opération, état final" où l'égalité porte sur l'identifiant, la quantité et les composants de chaque pile (R18). Les sondes PI9 et PI10 fournissent les états de référence ; V-I10 à V-I13 sont des exemples.

| Réf | Règle | Preuve |
|---|---|---|
| R40a | Clic gauche, curseur vide : le curseur prend toute la pile du slot. | S |
| R40b | Clic gauche, curseur plein, slot vide : dépose toute la pile. | S |
| R40c | Clic gauche, curseur et slot de même contenu : fusion selon R19, le reste reste au curseur. | S |
| R40d | Clic gauche, contenus différents : échange, si le slot accepte la pile du curseur (R38). | S |
| R41a | Clic droit, curseur vide : le curseur prend la moitié supérieure de la pile (pile de 7 : 4 au curseur, 3 restent). | S |
| R41b | Clic droit, curseur plein : dépose une unité (slot vide, ou même contenu avec place). | S |
| R42a | Déplacement rapide (maj-clic) de l'inventaire du joueur vers un conteneur : plage du conteneur en ordre croissant, en fusionnant d'abord dans les piles existantes. | S |
| R42b | Déplacement rapide d'un conteneur vers l'inventaire du joueur : plage du joueur en ordre décroissant (barre rapide d'abord, de la droite vers la gauche). | S |
| R42c | Déplacement rapide dans la fenêtre joueur seule : barre rapide vers inventaire principal et inverse, en ordre croissant. | S |
| R42d | Une pile qui ne rentre pas entièrement reste partiellement en place (le reste demeure dans le slot source). | S |
| R42e | Déplacement rapide dans les menus spéciaux (four, alambic, enclume, armures, enchantement, métier à tisser, forge, résultats) : non déduit du cas général, une table par menu est à produire après PI9 (Questions ouvertes). | S |
| R43a | Touche numérique n (1 à 9) : échange le slot survolé avec le slot n de la barre rapide (si l'un est vide, déplacement). | S |
| R43b | Touche main secondaire : idem avec la main secondaire. | S |
| R44a | Jeter (Q) : une unité du slot survolé (ou de la main sélectionnée). | S |
| R44b | Jeter avec modificateur : toute la pile. | S |
| R44c | Clic hors fenêtre avec une pile au curseur : jette la pile (clic gauche) ou une unité (clic droit). | S |
| R45a | Glisser avec le clic gauche sur n slots admissibles (vides ou de même contenu avec place) : répartit la pile du curseur en parts égales de floor(quantité / n), le reste reste au curseur. Pour n = 1, équivaut à un dépôt complet. | S |
| R45b | Glisser avec le clic droit : une unité par slot admissible. | S |
| R45c | Glisser avec le clic milieu (créatif) : une pile pleine par slot admissible. | S |
| R45d | Un slot interdit (résultat, slot filtré) est ignoré par un glisser. | S |
| R46 | Double clic : rassemble au curseur, jusqu'au maximum de pile, les piles de même contenu du menu ; l'ordre de collecte et l'exclusion du slot de résultat ne sont pas documentés par la page publique. Provisoire : on parcourt les slots du menu en ordre croissant, en visitant d'abord les piles non pleines, sans toucher aux slots de résultat. Sonde PI9. | S |
| R47a | Retrait d'un résultat de fabrication : consomme les ingrédients selon la recette et rend les restes (`item-recipes.md` R22, R23). | S |
| R47b | Si le résultat ne rentre pas dans l'inventaire (maj-clic), l'opération est refusée ou partielle, sans duplication et sans perte. Provisoire. Sonde PI10. | S |

### Propriétaires de modules (décision de conception)

Décisions prises pour cette spec dans le cadre des ADR 0004 et 0005, sans niveau de preuve (elles ne décrivent pas le jeu de référence). L'ajustement de l'ADR 0004 qui les consigne est l'amendement 1 de `docs/adr/0004-module-loot-et-ports-de-contexte.md`.

| Sujet | Module propriétaire | Raison |
|---|---|---|
| Pile, composants, égalité, fusion, fractionnement, conteneur (tableau de slots), formes NBT, JSON et texte | `item` | Données et règles sans monde (ADR 0004) |
| Algorithme des clics sur une liste abstraite de slots et un curseur (R40 à R47) et reconnaissance de recette | `item` | Fonctions pures sur des piles ; testables sans monde |
| Menus liés à un bloc ou à un joueur (ouverture, fermeture, R39), block entities à inventaire, progression de cuisson et d'alchimie, objets jetés au sol | `game` (ADR 0005) | Ils lisent le monde et la durée ; `game` appelle `item` et `loot` |
| Évaluation des fournisseurs de nombres et des prédicats de bloc (durée de combustible, vitesse de cuisson, compostage) | `loot` | Ces fournisseurs appartiennent au module `loot` (ADR 0004) ; `item` ne porte que la référence lue dans le prototype |
| Table d'enchantement, enclume, meule : coûts, sélection, fusion des enchantements | `loot` (calcul), `game` (menu et échange) | Le calcul dépend des définitions d'enchantement et de l'aléa, qui vivent dans `loot` |

## Entrées / sorties

- **Pile** : (identifiant, quantité entière, modifications) ; plage de quantité 0 à 99 (R16) ; composants selon le catalogue R9.
- **Lecture** : prototype par identifiant depuis les 1658 fichiers ; pile depuis NBT (R24 à R27), JSON (R28) ou texte de commande (R29).
- **Écriture NBT** : `id`, `count`, `components` si modifications. L'ordre des clés écrites suit l'ordre d'insertion (`core-nbt` R30) ; l'égalité entre piles se fait sur l'arbre décodé.
- **Conteneur** : tableau de piles indexé ; sérialisation `Items` creuse (R25).
- **Menu** : entrée = (type de menu, indices de slots R37a à R37d, clic R40a à R47b), sortie = nouvel état des slots, du curseur et des piles jetées.

## Cas limites

- Quantité 0 ou négative à la lecture : pile vide (R15) ; quantité supérieure à `max_stack_size` : S, sonde PI1.
- Composant à valeur `{}` contre composant absent (R6b) : deux états distincts (provisoire).
- Pile à 99 d'un objet dont `max_stack_size` est 16 par modification : la pile garde sa quantité ou est ramenée au maximum : S, sonde PI1.
- Composant `max_stack_size` modifié à 1 sur une pile de 64 : S, PI1.
- Flottants -0.0 et NaN dans un composant : conservés au bit près (`core-nbt` R35).
- Imbrication : un coffre dans un coffre dans un coffre... via le composant `container` ; le bundle dans un bundle : S (PI4).
- Déplacement rapide quand deux piles partielles admissibles existent (ordre de remplissage) ; glisser avec des capacités hétérogènes ; double clic avec le slot de résultat de fabrication au milieu de la plage.
- Fermeture du menu avec une pile au curseur et un inventaire plein.

## Vecteurs de test

Tous les vecteurs suivants sont de niveau D ou C. Le script de l'annexe lit uniquement les fichiers extraits.

| Vecteur | Entrée | Sortie attendue | Preuve |
|---|---|---|---|
| V-I1 | `components/item/air.json` | 13 composants (R3), `max_stack_size` 64, `item_name` `{"translate":"block.minecraft.air"}` | D |
| V-I2 | `components/item/stone_pickaxe.json` | `max_stack_size` 1, `max_damage` 131, `damage` 0, `enchantable` `{"value":5}`, `repairable` `{"items":"#minecraft:stone_tool_materials"}`, `tool.rules` de 2 règles (`#minecraft:incorrect_for_stone_tool` `correct_for_drops` false ; `#minecraft:mineable/pickaxe` `correct_for_drops` true `speed` 4.0), `weapon` `{"item_damage_per_attack":2}`, `attribute_modifiers` : `attack_damage` 2.0 (`add_value`, `mainhand`) et `attack_speed` -2.799999952316284 | D |
| V-I3 | `components/item/bread.json` et `golden_apple` | `food` (nutrition 5, saturation 6.0 pour le pain) et `consumable` `{}` ; `villager_food` `{"nutrition":4}` | D |
| V-I4 | `components/item/milk_bucket.json` | `use_remainder` `{"id":"minecraft:bucket"}` (7 objets en ont : `beetroot_soup`, `honey_bottle`, `milk_bucket`, `mushroom_stew`, `potion`, `rabbit_stew`, `suspicious_stew`) | D |
| V-I5 | `components/item/decorated_pot.json` | `pot_decorations` `{"back":{"id":"minecraft:brick"},"front":...,"left":...,"right":...}` (piles en JSON `{id}`) | D |
| V-I6 | `components/item/white_banner.json` | `banner_patterns` `[]`, `max_stack_size` 16 | D |
| V-I7 | Structure contenant un alambic (11 occurrences) | `Items` de piles `{count: 1, Slot: 0b, id: "minecraft:potion", components: {"minecraft:potion_contents": {potion: "minecraft:strong_healing"}}}`, `BrewTime` Short 0, `Fuel` Byte 0 | D |
| V-I8 | Structure contenant un four | `Items` : `{count: 1, Slot: 1b, id: "minecraft:wooden_shovel"}`, `{count: 24, Slot: 2b, id: "minecraft:deepslate"}`, `cooking_total_time` Short 200, `RecipesUsed` `{"minecraft:deepslate": 24}` | D |
| V-I9 | Invariant sur les 1658 prototypes | `max_damage` présent implique `max_stack_size` 1 (84 sur 84) ; `damage` présent implique `max_damage` présent | C |
| V-I9b | `python -I components_stats.py "$REF" \| sha256sum` | empreinte de l'annexe ; en cas d'écart, relire la sortie section par section | C |

Vecteurs de comportement (S, à produire par les sondes) :

| Vecteur | Entrée | Sortie attendue (provisoire) |
|---|---|---|
| V-I10 | Curseur vide, slot de 7 pierres, clic droit | curseur 4, slot 3 (R41a) |
| V-I11 | Curseur 10 pierres, glisser gauche sur 3 slots vides | 3 par slot, 1 au curseur (R45a) |
| V-I12 | Curseur 10 pierres, glisser droit sur 3 slots vides | 1 par slot, 7 au curseur (R45b) |
| V-I13 | Curseur 64 pierres, clic gauche sur slot de 60 pierres | slot 64, curseur 60 (R40c, R19) |
| V-I14 | `give @s stone 100` | une pile de 64 et une de 36 (R16) |

## Plan de tests

- **Unitaire (L1)** : chargement des 1658 prototypes et comparaison de chacun avec son fichier (R2, R3, R5, V-I1 à V-I9, V-I9 invariants) ; aller-retour NBT, JSON et texte pour chacun des 122 composants avec la valeur d'un prototype (69 types observés) ou la forme de R9b (53 types) ; égalité et fusion (R17 à R20) en test de propriétés (fusionner puis fractionner conserve le total) ; tous les V-I10 à V-I14 sur le moteur de menu avec des conteneurs factices.
- **Différentiel (L2)** : sondes PI1 à PI9 ; relecture de nos piles par l'oracle via `/give` et `/data get`.
- **Niveau B** : tests écrits en aveugle par le `test-engineer` à partir de cette spec.

## Sondes proposées

Cadre : serveur 26.3 headless, monde plat `level-name=sonde`, RCON, `/tick freeze` au besoin. Joueur factice : un client protocole minimal est nécessaire aux sondes PI7 à PI9 (voir Questions ouvertes, bloquant partiel).

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Règles levées |
|---|---|---|---|
| PI1 | `give @s minecraft:stone 100`, `give @s minecraft:ender_pearl 40`, `give @s minecraft:stone[minecraft:max_stack_size=1] 3`, `give @s minecraft:stone[minecraft:max_stack_size=100]`, `give @s minecraft:diamond_pickaxe[minecraft:max_stack_size=64]`, puis `data get entity @s Inventory` | Répartition en piles de 64 et 36 ; 16 et 16 et 8 pour les perles ; pour la pile de 100 et le cas durabilité, message d'erreur ou valeur bornée | R16, R5 |
| PI2 | `give @s minecraft:stone[minecraft:rarity=common]`, `give @s minecraft:stone[!minecraft:rarity]`, `give @s minecraft:stone[minecraft:rarity=epic]`, `give @s minecraft:diamond_sword[!minecraft:attribute_modifiers]`, `data get entity @s Inventory[0]` pour chaque | Savoir si la modification égale au prototype est écrite, comment s'écrit le retrait (clé `!`) et la forme de `components` | R17, R27, R29 |
| PI3 | Pour chacun des 122 composants, `give @s minecraft:stone[<composant>=<valeur d'un prototype ou forme de R9b>]` (ou l'objet qui le porte) puis `data get entity @s Inventory[-1]` ; consigner l'acceptation, le rejet et la forme relue | Table composant -> forme NBT relue. Lève les 53 types S de R9b | R9, R9b, R10, R12 |
| PI4 | `item replace entity @s container.0 with minecraft:bundle`, puis imbriquer un bundle, un shulker box, un coffre portant `container` ; lire par `data get entity @s Inventory` | Limite d'imbrication, refus ou acceptation | R23 |
| PI5 | `give` de piles `diamond_sword` avec et sans `damage=0`, avec et sans modification égale au prototype, pour 2 pierres de rarity différentes ; puis `give` successifs d'une même pile et lecture du nombre de slots occupés. Variante : entonnoir sous un coffre, pour observer la fusion | Fusion ou non-fusion selon l'égalité de composants effectifs | R18, R19 |
| PI6 | `setblock ~ ~ ~ minecraft:chest{Items:[{Slot:0b,id:"minecraft:inconnu",count:1},{Slot:1b,id:"minecraft:stone"},{Slot:2b,id:"minecraft:stone",count:0},{Slot:3b,id:"minecraft:stone",count:-5}]}` puis `data get block ~ ~ ~ Items` | Quelles piles survivent à la lecture, count par défaut | R26, R15 |
| PI7 | `item replace entity @s hotbar.0 with minecraft:stone`, `armor.head`, `weapon.offhand`, `inventory.0`, puis `data get entity @s` complet | Clés et indices de slots écrits (H-inv-a ou H-inv-b) | R34, R35 |
| PI8 | `item replace` avec les noms de R35 un à un, y compris des noms invalides | Liste des noms valides pour 26.3 | R35 |
| PI9 | Client protocole minimal connecté au serveur, ouvrant un coffre, un four, une fenêtre joueur : clics gauche, droit, maj, touches numériques, glisser, double clic, jeter, fermeture avec curseur plein ; état des slots relevé après chaque clic (paquets du serveur) | Table opération -> états. Lève R38 à R47 et V-I10 à V-I13 | R38 à R47 |
| PI10 | Même client, menu `crafting` et `furnace` : retrait du résultat par clic et maj-clic avec inventaire plein | Atomicité du retrait de résultat | R47 |
| PI11 | `give @s minecraft:stone[minecraft:custom_data={a:1b}]` puis `/clear @s minecraft:stone[minecraft:custom_data={a:1b}]` et `/clear @s minecraft:stone` | Correspondance de `item_predicate` avec des composants (renvoi à `loot-tables.md`) | R29 |
| PI12 | `give` d'un objet à durabilité avec `damage=<max_damage - 1>`, puis usage unitaire par commande `item modify` avec `set_damage add=true` | Seuil de rupture (pile supprimée ou non) | R21 |

## Couverture des règles S par les sondes

Les règles S qui ne citent pas de sonde dans leur ligne sont levées ainsi : R9b, R12 (rejet d'un champ inconnu) : PI3 ; R14, R20, R22 : PI2, PI3, PI5 ; R28 (défaut de `count`) : PI3 ; R30 (limite de pile d'un conteneur) : PI9 ; R32 (déballage) : sondes PL5 de `loot-tables.md` ; R33 (sens de `RecipesUsed`) : PR8 de `item-recipes.md` ; R36 et R37a à R37d (indices) : PI7, PI9. Les règles R38a à R47b sont levées par PI9 et PI10. Aucune règle S n'est sans sonde.

## Questions ouvertes

- Les 53 types de composants absents des prototypes (R8, R9b) restent S : forme à confirmer (PI3). `sulfur_cube_content` et `waxed` n'ont aucune documentation publique trouvée ; leur forme est inconnue.
- H-inv-a contre H-inv-b (R34).
- Liste complète des noms de slots (R35).
- Les règles de menu R38a à R47b demandent un client protocole côté sonde (PI9, PI10). Bloquant partiel pour L2 de cette spec : sans client, seuls PI1 à PI8, PI11, PI12 sont exécutables. Le client minimal doit être écrit par le `tooling-engineer` à partir de la documentation publique du protocole ; il ne doit pas lire l'implémentation de référence.
- Le déplacement rapide par menu spécial (R42e : four, alambic, enclume, enchantement, métier à tisser, forge, armures) n'est pas décrit : ajouter une table par menu après PI9.
- Le comportement exact de `max_stack_size` supérieur à 64 (R16) et de la quantité hors plage.
- Mise à niveau des piles de versions antérieures (correcteur de données) : hors périmètre ; les mondes visés sont ceux de la data version 5023.
- Règles S restantes : R6b, R9b, R10, R12 (rejet des champs inconnus), R14 à R23 (sauf les parties D), R26 à R29, R30, R32 (sémantique), R33 (sens), R34 à R36, R37a à R47b.

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Objets et composants | M6 | B | spécifié | `docs/specs/item-components-inventory.md` (règles D/C pour le registre, les prototypes, les formes NBT et JSON ; S pour la pile et les menus) |
| Inventaires et menus (ligne à créer) | M6 | B | spécifié | idem, R30 à R47b en S, sondes PI7 à PI10 |

## Annexe : components_stats.py (C)

Lit uniquement les fichiers extraits ; aucune dépendance au jeu ; bibliothèque standard seule. À copier hors du dossier de données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I components_stats.py "$REF" | sha256sum
```

Empreinte SHA-256 attendue de la sortie complète (sortie en UTF-8, fins de ligne LF) :

```text
cf0575fc2ed4ec5c428ddaadb4fb850fe3580bbc52a0953927f577568b55d86d
```

Le script contient son propre lecteur NBT minimal (indépendant de `core-nbt`), car R24 à R33 lisent les gabarits de structure.

```python
"""Counts and shapes of item prototypes, component catalogue and container NBT in the 26.3 data.

Usage: python -I components_stats.py <reference dir containing generated/ and resources/>
Reads extracted files only. Standard library only. Output is deterministic (sorted, LF).
"""
import collections
import glob
import gzip
import json
import os
import struct
import sys

sys.stdout.reconfigure(encoding="utf-8", newline="\n")
REF = sys.argv[1]
REP = os.path.join(REF, "generated", "reports")
DATA = os.path.join(REF, "resources", "data", "minecraft")


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def kind(v):
    if isinstance(v, bool):
        return "bool"
    if isinstance(v, int):
        return "int"
    if isinstance(v, float):
        return "float"
    if isinstance(v, str):
        return "str"
    if isinstance(v, list):
        return "list"
    return "obj"


def section(name):
    print("== " + name)


registries = load(os.path.join(REP, "registries.json"))
reg_components = sorted(registries["minecraft:data_component_type"]["entries"])
items = {}
for f in glob.glob(os.path.join(REP, "minecraft", "components", "item", "*.json")):
    items[os.path.basename(f)[:-5]] = load(f)["components"]
N = len(items)

section("R1 registries")
for r in ["item", "data_component_type", "menu", "consume_effect_type",
          "data_component_predicate_type", "slot_source_type"]:
    print(r, len(registries["minecraft:" + r]["entries"]))
print("item default", registries["minecraft:item"].get("default"))

section("R2-R5 universal components and distributions")
count = collections.Counter()
for comps in items.values():
    count.update(comps.keys())
universal = sorted(k for k, v in count.items() if v == N)
print("prototypes", N, "component entries", sum(count.values()), "universal", len(universal), universal)
print("rarity", sorted(collections.Counter(c["minecraft:rarity"] for c in items.values()).items()))
print("max_stack_size", sorted(collections.Counter(c["minecraft:max_stack_size"] for c in items.values()).items()))
print("break_sound", sorted(collections.Counter(c["minecraft:break_sound"] for c in items.values()).items()))
print("repair_cost", sorted(collections.Counter(c["minecraft:repair_cost"] for c in items.values()).items()))
print("item_model==id", sum(1 for n, c in items.items() if c["minecraft:item_model"] == "minecraft:" + n))
print("attribute_modifiers non-empty", sum(1 for c in items.values() if c["minecraft:attribute_modifiers"]))
print("enchantments non-empty", sum(1 for c in items.values() if c["minecraft:enchantments"]))
print("attack_animation non-empty", sum(1 for c in items.values() if c["minecraft:attack_animation"]))
print("use_effects non-empty", sum(1 for c in items.values() if c["minecraft:use_effects"]))
md = [n for n, c in items.items() if "minecraft:max_damage" in c]
print("max_damage", len(md), "stack!=1", [n for n in md if items[n]["minecraft:max_stack_size"] != 1])
print("damage without max_damage", [n for n, c in items.items() if "minecraft:damage" in c and n not in md])
print("stack 1 without max_damage", sum(1 for n, c in items.items() if c["minecraft:max_stack_size"] == 1 and n not in md))
print("item_name translate prefixes", sorted(collections.Counter(
    c["minecraft:item_name"]["translate"].split(".")[0] for c in items.values()).items()))

section("R7 presence of non universal components")
for k, v in sorted(count.items(), key=lambda kv: (-kv[1], kv[0])):
    if v != N:
        print(v, k)
used = set(count)
absent = [c for c in reg_components if c not in used]
print("catalogue", len(reg_components), "seen", len(used), "absent", len(absent))

section("R6 empty values")
for k in sorted(used):
    n = sum(1 for c in items.values() if c.get(k) == {} and k not in universal)
    if n:
        print(n, k)

section("R9 catalogue: kind distribution and field union per seen component")
for k in sorted(used):
    kinds = collections.Counter(kind(c[k]) for c in items.values() if k in c)
    fields = collections.Counter()
    for c in items.values():
        if k in c and isinstance(c[k], dict):
            fields.update(c[k].keys())
    print(k, dict(sorted(kinds.items())), "fields=" + ",".join(f + ":" + str(n) for f, n in sorted(fields.items())))
print("absent", ",".join(absent))

section("R8 absent components seen outside prototypes")
seen = collections.Counter()
for sub in ["loot_table", "villager_trade", "advancement", "recipe", "trade_set"]:
    for root, _, files in os.walk(os.path.join(DATA, sub)):
        for fn in files:
            if not fn.endswith(".json"):
                continue
            with open(os.path.join(root, fn), encoding="utf-8") as fh:
                text = fh.read()
            for k in absent:
                needle = '"' + k + '"'
                if needle in text:
                    seen[(k, sub)] += text.count(needle)
for (k, sub), n in sorted(seen.items()):
    print(k, sub, n)

section("R11 consume effects")
eff = collections.Counter()
for c in items.values():
    for e in c.get("minecraft:consumable", {}).get("on_consume_effects", []):
        eff[e["type"]] += 1
    for e in c.get("minecraft:death_protection", {}).get("death_effects", []):
        eff["death:" + e["type"]] += 1
print(sorted(eff.items()))

section("R12 structured component fields")
for k in ["equippable", "food", "tool", "weapon", "consumable", "attack_range", "kinetic_weapon",
          "piercing_weapon", "blocks_attacks", "death_protection", "use_effects", "attack_animation",
          "cooking_fuel", "brewing_fuel", "enchantable", "repairable", "damage_resistant"]:
    fields = collections.Counter()
    n = 0
    for c in items.values():
        v = c.get("minecraft:" + k)
        if isinstance(v, dict):
            n += 1
            fields.update(v.keys())
    print(k, n, ",".join(f + ":" + str(x) for f, x in sorted(fields.items())))
ops = collections.Counter()
mfields = collections.Counter()
for c in items.values():
    for m in c["minecraft:attribute_modifiers"]:
        ops[m["operation"]] += 1
        mfields.update(m.keys())
print("attribute_modifiers operations", sorted(ops.items()), "fields", sorted(mfields.items()))
rules = collections.Counter()
for c in items.values():
    for r in c.get("minecraft:tool", {}).get("rules", []):
        rules.update(r.keys())
print("tool rules fields", sorted(rules.items()))
print("enchantable values", sorted(set(c["minecraft:enchantable"]["value"] for c in items.values() if "minecraft:enchantable" in c)))
print("speed sentinel", sorted(set(r["speed"] for c in items.values() for r in c.get("minecraft:tool", {}).get("rules", []) if "speed" in r and r["speed"] > 1e6)))
print("use_remainder", sorted((n, c["minecraft:use_remainder"]["id"]) for n, c in items.items() if "minecraft:use_remainder" in c))

section("R16 command argument bounds")
cmds = load(os.path.join(REP, "commands.json"))


def find_args(node, path, out):
    for name, child in node.get("children", {}).items():
        p = path + [name]
        if child.get("parser") == "brigadier:integer":
            out.append((" ".join(p), child.get("properties", {})))
        find_args(child, p, out)


for root in ["give", "item"]:
    out = []
    find_args(cmds["children"][root], [root], out)
    for p, props in sorted(out):
        if "count" in p or "amount" in p:
            print(p, props)

section("NBT reader over structure templates")


class Reader:
    def __init__(self, b):
        self.b, self.i = b, 0

    def take(self, n):
        v = self.b[self.i:self.i + n]
        self.i += n
        return v

    def string(self):
        n = struct.unpack(">H", self.take(2))[0]
        return self.take(n).decode("utf-8", "replace")

    def payload(self, t):
        if t == 1:
            return ("byte", struct.unpack(">b", self.take(1))[0])
        if t == 2:
            return ("short", struct.unpack(">h", self.take(2))[0])
        if t == 3:
            return ("int", struct.unpack(">i", self.take(4))[0])
        if t == 4:
            return ("long", struct.unpack(">q", self.take(8))[0])
        if t == 5:
            return ("float", struct.unpack(">f", self.take(4))[0])
        if t == 6:
            return ("double", struct.unpack(">d", self.take(8))[0])
        if t == 7:
            n = struct.unpack(">i", self.take(4))[0]
            return ("byte_array", self.take(n))
        if t == 8:
            return ("string", self.string())
        if t == 9:
            et = self.take(1)[0]
            n = struct.unpack(">i", self.take(4))[0]
            return ("list", [self.payload(et) for _ in range(n)] if n > 0 else [])
        if t == 10:
            d = {}
            while True:
                tt = self.take(1)[0]
                if tt == 0:
                    return ("compound", d)
                name = self.string()
                d[name] = self.payload(tt)
        if t == 11:
            n = struct.unpack(">i", self.take(4))[0]
            return ("int_array", struct.unpack(">%di" % n, self.take(4 * n)))
        if t == 12:
            n = struct.unpack(">i", self.take(4))[0]
            return ("long_array", struct.unpack(">%dq" % n, self.take(8 * n)))
        raise ValueError(t)


def read_nbt(path):
    with open(path, "rb") as fh:
        raw = gzip.decompress(fh.read())
    r = Reader(raw)
    t = r.take(1)[0]
    r.string()
    return r.payload(t)


be = collections.Counter()
be_fields = collections.defaultdict(collections.Counter)
be_types = collections.defaultdict(dict)
stacks = collections.Counter()
slot_by_id = collections.defaultdict(collections.Counter)
items_empty = collections.Counter()
comp_empty = collections.Counter()
files = sorted(glob.glob(os.path.join(DATA, "structure", "**", "*.nbt"), recursive=True))
print("structure files", len(files))


def visit_stack(s, where):
    d = s[1]
    stacks[("where", where)] += 1
    stacks[(where, "count" in d)] += 1
    stacks[("has_components", "components" in d)] += 1
    stacks[("has_Slot", "Slot" in d)] += 1
    if "count" in d:
        stacks[("count_type", d["count"][0])] += 1
    if "Slot" in d:
        stacks[("slot_type", d["Slot"][0])] += 1


for p in files:
    root = read_nbt(p)[1]
    for b in root.get("blocks", ("list", []))[1]:
        bd = b[1]
        if "nbt" not in bd:
            continue
        n = bd["nbt"][1]
        bid = n["id"][1] if "id" in n else "?"
        be[bid] += 1
        for k, v in n.items():
            be_fields[bid][k] += 1
            be_types[bid][k] = v[0]
        if "Items" in n and not n["Items"][1]:
            items_empty[bid] += 1
        if "components" in n and not n["components"][1]:
            comp_empty[bid] += 1
        for s in n.get("Items", ("list", []))[1]:
            visit_stack(s, "Items")
            slot_by_id[bid][s[1]["Slot"][1]] += 1
        if "item" in n:
            visit_stack(n["item"], "item")
        if "config" in n and "key_item" in n["config"][1]:
            visit_stack(n["config"][1]["key_item"], "config.key_item")
        if "sherds" in n:
            for s in n["sherds"][1].values():
                visit_stack(s, "sherds")
print("stack shapes", sorted((str(k), v) for k, v in stacks.items()))
print("empty Items lists", sorted(items_empty.items()))
print("empty components compounds", sorted(comp_empty.items()))
print("block entities (count, fields:type=count)")
for bid in sorted(be):
    print(bid, be[bid], ",".join(k + ":" + be_types[bid][k] + "=" + str(c) for k, c in sorted(be_fields[bid].items())))
print("slots by block entity", {b: sorted(c.items()) for b, c in sorted(slot_by_id.items())})
```
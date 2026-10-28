# Objets, composants de données, inventaires et menus

Statut : brouillon
Jalon : M6
Risque : B
Règles : 47 (R1 à R47)

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
  - `generated/reports/minecraft/components/item/<id>.json` : composants par défaut de chacun des 1658 objets (23 072 lignes de composants selon le rapport local, recomptées en C ci-dessous) ;
  - `generated/reports/commands.json` : arguments `minecraft:item_stack`, `minecraft:item_predicate`, `minecraft:item_slot`, `minecraft:slot_source` des commandes `give`, `clear`, `item`, `loot` ;
  - `resources/data/minecraft/structure/**/*.nbt` (1511 fichiers) : block entities à inventaire dans les gabarits de structures, lus avec un lecteur NBT minimal indépendant ;
  - `resources/data/minecraft/{recipe,loot_table,villager_trade,advancement}/**` pour les formes JSON de piles et de composants.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Java Edition protocol/Inventory" (disposition des slots par type de fenêtre), "Inventory" (comportement des clics), "Item modifier", "Loot table", "Predicate".
- **Rapports locaux** (indices, ADR 0002) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, parties "Composants d'objets" (chapitres 134 à 136) et "Inventaires, menus et transactions" (chapitre 176). Ils ne donnent que des listes de comportements à reproduire ("à spécifier") : aucune règle S ci-dessous n'en vient sans recoupement.
- **Sondes de l'oracle** : aucune exécutée à ce jour. Les sondes proposées sont en fin de document (PI1 à PI12).

Convention de preuve : **D** = valeur lue dans un fichier extrait ; **C** = propriété calculée sur ces fichiers (reproductible, commande donnée) ; **M** = observé sur l'oracle (non utilisé : oracle pas encore lancé) ; **S** = documentation publique seule ou hypothèse. Chaque règle S porte un **comportement provisoire** à implémenter jusqu'à ce que la sonde nommée tranche.

## Règles

### Registre d'objets et composants par défaut

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le registre `item` compte 1658 entrées ; l'entrée par défaut est `minecraft:air`. | D |
| R2 | Chaque objet possède une table de composants par défaut ("prototype"), fixe, fournie par les données. Les 1658 fichiers `components/item/<id>.json` sont une source exacte : le moteur charge ces tables et ne les code pas en dur. | D |
| R3 | Treize composants sont présents dans le prototype des 1658 objets, `air` compris : `attack_animation`, `attribute_modifiers`, `break_sound`, `enchantments`, `interact_animation`, `item_model`, `item_name`, `lore`, `max_stack_size`, `rarity`, `repair_cost`, `tooltip_display`, `use_effects`. Aucun autre composant n'est universel. | C |
| R4 | Valeurs les plus fréquentes des treize composants universels (les exceptions sont lisibles dans les fichiers : 7 lances ont une `attack_animation` et une `use_effects` non vides, 93 objets ont des `attribute_modifiers`) : `attack_animation` `{}`, `interact_animation` `{}`, `tooltip_display` `{}`, `use_effects` `{}`, `enchantments` `{}`, `lore` `[]`, `attribute_modifiers` `[]`, `repair_cost` 0, `rarity` "common" (1543 objets ; "uncommon" 78, "rare" 18, "epic" 19), `break_sound` `minecraft:entity.item.break` (1656 objets ; `minecraft:item.shield.break` pour le bouclier et `minecraft:item.wolf_armor.break` pour l'armure de loup), `item_model` = identifiant de l'objet, `item_name` = `{"translate": "item.minecraft.<id>"}` (ou `block.minecraft.<id>` pour les objets-blocs, ex. `air`). | C |
| R5 | `max_stack_size` : 64 pour 1342 objets, 16 pour 68, 1 pour 248. Les 84 objets dotés de `max_damage` (et de `damage` 0) ont tous `max_stack_size` 1 ; 164 autres objets sans durabilité ont aussi 1. Invariant à tester : `max_damage` présent implique `max_stack_size` 1. | C |
| R6 | Un composant vide est significatif : `consumable` `{}` (30 objets), `weapon` `{}` (16), `glider` `{}`, `bucket_entity_data` `{}`, `map_decorations` `{}` signalent la présence du comportement avec ses valeurs implicites de codec. Un composant présent à `{}` n'est pas équivalent à un composant absent. Les valeurs implicites de codec ne sont pas développées dans les fichiers. | D |
| R7 | Tableau de dénombrement (C) : la commande de reproduction est `python -I components_stats.py $REF` (script de l'annexe en fin de document). Présence des composants non universels : `cooking_fuel` 347, `compostable` 122, `entity_data` 88, `equippable` 84, `damage` 84, `max_damage` 84, `enchantable` 77, `repairable` 75, `container` 45, `food` 44, `weapon` 44, `consumable` 43, `tool` 38, `sign_text_front`/`sign_text_back` 26 chacun, `provides_pottery_pattern` 23, `jukebox_playable` 22, `block_transformer` 21, `map_decorations` 17, `damage_resistant` 17, `banner_patterns` 17, `bundle_contents` 17, `cushion/color` 16, `dye` 16, `block_state` 12, `provides_trim_material` 11, `provides_banner_patterns` 10, `bucket_entity_data` 7, `use_remainder` 7, `attack_range` 7, `damage_type` 7, `kinetic_weapon` 7, `minimum_attack_charge` 7, `piercing_weapon` 7, `enchantment_glint_override` 7, `villager_food` 4, `mob_visibility` 4, `potion_contents` 4, `chicken/variant` 3, `use_cooldown` 3, `bees` 2, `potion_duration_scale` 2, et 1 objet pour chacun de `brewing_fuel`, `charged_projectiles`, `debug_stick_state`, `pot_decorations`, `glider`, `stored_enchantments`, `fireworks`, `instrument`, `recipes`, `ominous_bottle_amplifier`, `blocks_attacks`, `suspicious_stew_effects`, `death_protection`, `writable_book_content`. 69 des 122 types apparaissent dans au moins un prototype. | C |
| R8 | Les 53 types absents de tous les prototypes ne peuvent être décrits que par la documentation publique (forme) : ils portent le niveau S dans le catalogue de R9. Neuf d'entre eux (`dyed_color`, `map_id`, `trim`, `sheep/color`, `villager/variant`, `wolf/variant`, `frog/variant`, `cat/variant`, `mooshroom/variant`) sont vus dans les données hors prototype (tables de butin, échanges, avancements), ce qui confirme leur nom et, pour `trim`, `sheep/color` et `mooshroom/variant`, leur forme JSON. | C |

### Catalogue des 122 types de composants

| Réf | Règle | Preuve |
|---|---|---|
| R9 | Catalogue complet des types du registre `data_component_type` (identifiants sans le préfixe `minecraft:`). Colonne "Objets" : nombre de prototypes qui le portent ; colonne "Preuve" : D si observé dans un prototype (forme = valeur la plus fréquente), S sinon (forme = indication de la documentation publique, à confirmer par PI3). Aucune règle de gameplay n'est portée par cette ligne. Le catalogue sert de liste d'acceptation pour le décodage (R10) et de liste de tests PI3. | D / S |

| Composant | Objets | Preuve | Forme (valeur la plus fréquente, ou indication) |
|---|---|---|---|
| `additional_trade_cost` | 0 | S | Int |
| `attack_animation` | 1658 | D | `{}` |
| `attack_range` | 7 | D | `{"hitbox_margin":0.125,"max_creative_reach":6.5,"max_reach":4.5,"min_creative_reach":2.0,"min_reach":2.0,"m...` |
| `attribute_modifiers` | 1658 | D | `[]` |
| `axolotl/variant` | 0 | S | chaîne (variante) |
| `banner_patterns` | 17 | D | `[]` |
| `base_color` | 0 | S | chaîne (couleur de teinture) |
| `bees` | 2 | D | `[]` |
| `block_entity_data` | 0 | S | compound NBT de block entity (avec id) |
| `block_state` | 12 | D | `{"copper_golem_pose":"standing"}` |
| `block_transformer` | 21 | D | `"minecraft:axe"` |
| `blocks_attacks` | 1 | D | `{"block_delay_seconds":0.25,"block_sound":"minecraft:item.shield.block","bypassed_by":"#minecraft:bypasses_...` |
| `break_sound` | 1658 | D | `"minecraft:entity.item.break"` |
| `brewing_fuel` | 1 | D | `{"speed_multiplier":"minecraft:brewing/speed_default","uses":"minecraft:brewing/uses_default"}` |
| `bucket_entity_data` | 7 | D | `{}` |
| `bundle_contents` | 17 | D | `[]` |
| `can_break` | 0 | S | prédicat(s) de bloc |
| `can_place_on` | 0 | S | prédicat(s) de bloc |
| `cat/collar` | 0 | S | chaîne (couleur) |
| `cat/sound_variant` | 0 | S | id |
| `cat/variant` | 0 | S | id |
| `charged_projectiles` | 1 | D | `[]` |
| `chicken/sound_variant` | 0 | S | id |
| `chicken/variant` | 3 | D | `"minecraft:cold"` |
| `compostable` | 122 | D | `{"layers":"minecraft:compostable/low"}` |
| `consumable` | 43 | D | `{}` |
| `container` | 45 | D | `[]` |
| `container_loot` | 0 | S | {loot_table, seed} |
| `cooking_fuel` | 347 | D | `{"burn_time":"minecraft:cooking/time_wood_blocks","speed_multiplier":"minecraft:cooking/speed_default"}` |
| `cow/sound_variant` | 0 | S | id |
| `cow/variant` | 0 | S | id |
| `creative_slot_lock` | 0 | S | {} |
| `cushion/color` | 16 | D | `"black"` |
| `custom_data` | 0 | S | compound NBT libre |
| `custom_model_data` | 0 | S | {floats, flags, strings, colors} |
| `custom_name` | 0 | S | composant de texte |
| `damage` | 84 | D | `0` |
| `damage_resistant` | 17 | D | `{"types":"#minecraft:is_fire"}` |
| `damage_type` | 7 | D | `"minecraft:spear"` |
| `death_protection` | 1 | D | `{"death_effects":[{"type":"minecraft:clear_all_effects"},{"type":"minecraft:apply_effects","effects":[{"amp...` |
| `debug_stick_state` | 1 | D | `{}` |
| `dye` | 16 | D | `"black"` |
| `dyed_color` | 0 | S | entier RGB (la forme `{}` n est vue que comme prédicat de présence) |
| `enchantable` | 77 | D | `{"value":15}` |
| `enchantment_glint_override` | 7 | D | `true` |
| `enchantments` | 1658 | D | `{}` |
| `entity_data` | 88 | D | `{"id":"minecraft:allay"}` |
| `equippable` | 84 | D | `{"slot":"head","swappable":false}` |
| `firework_explosion` | 0 | S | {shape, colors, fade_colors, has_trail, has_twinkle} |
| `fireworks` | 1 | D | `{"flight_duration":1}` |
| `food` | 44 | D | `{"nutrition":2,"saturation":0.4}` |
| `fox/variant` | 0 | S | chaîne |
| `frog/variant` | 0 | S | id |
| `glider` | 1 | D | `{}` |
| `horse/variant` | 0 | S | chaîne (robe) |
| `instrument` | 1 | D | `"minecraft:ponder_goat_horn"` |
| `intangible_projectile` | 0 | S | {} |
| `interact_animation` | 1658 | D | `{}` |
| `item_model` | 1658 | D | `"minecraft:abandoned_camp_map"` |
| `item_name` | 1658 | D | `{"translate":"item.minecraft.abandoned_camp_map"}` |
| `jukebox_playable` | 22 | D | `"minecraft:11"` |
| `kinetic_weapon` | 7 | D | `{"damage_conditions":{"max_duration_ticks":250,"min_relative_speed":4.6},"damage_multiplier":0.82,"delay_ti...` |
| `llama/variant` | 0 | S | chaîne |
| `lock` | 0 | S | prédicat d objet (clé) |
| `lodestone_tracker` | 0 | S | {target:{pos,dimension}, tracked} |
| `lore` | 1658 | D | `[]` |
| `map_decorations` | 17 | D | `{}` |
| `map_id` | 0 | S | Int (la forme `{}` n est vue que comme prédicat de présence) |
| `map_post_processing` | 0 | S | transitoire, non persistant (à vérifier) |
| `max_damage` | 84 | D | `250` |
| `max_stack_size` | 1658 | D | `64` |
| `minimum_attack_charge` | 7 | D | `1.0` |
| `mob_visibility` | 4 | D | `{"targeting_entity_types":"minecraft:creeper","visibility":0.5}` |
| `mooshroom/variant` | 0 | S | chaîne (red, brown) |
| `note_block_sound` | 0 | S | id |
| `ominous_bottle_amplifier` | 1 | D | `0` |
| `painting/variant` | 0 | S | id |
| `parrot/variant` | 0 | S | chaîne |
| `piercing_weapon` | 7 | D | `{"hit_sound":"minecraft:item.spear.hit","sound":"minecraft:item.spear.attack"}` |
| `pig/sound_variant` | 0 | S | id |
| `pig/variant` | 0 | S | id |
| `pot_decorations` | 1 | D | `{"back":{"id":"minecraft:brick"},"front":{"id":"minecraft:brick"},"left":{"id":"minecraft:brick"},"right":{...` |
| `potion_contents` | 4 | D | `{}` |
| `potion_duration_scale` | 2 | D | `0.25` |
| `profile` | 0 | S | profil de tête (nom, uuid, propriétés) |
| `provides_banner_patterns` | 10 | D | `"#minecraft:pattern_item/bordure_indented"` |
| `provides_pottery_pattern` | 23 | D | `"minecraft:angler"` |
| `provides_trim_material` | 11 | D | `"minecraft:amethyst"` |
| `rabbit/variant` | 0 | S | chaîne |
| `rarity` | 1658 | D | `"common"` |
| `recipes` | 1 | D | `[]` |
| `repair_cost` | 1658 | D | `0` |
| `repairable` | 75 | D | `{"items":"#minecraft:wooden_tool_materials"}` |
| `salmon/size` | 0 | S | chaîne |
| `sheep/color` | 0 | S | chaîne (couleur) |
| `shulker/color` | 0 | S | chaîne (couleur) |
| `sign_text_back` | 26 | D | `{"color":"black","has_glowing_text":false,"messages":["","","",""]}` |
| `sign_text_front` | 26 | D | `{"color":"black","has_glowing_text":false,"messages":["","","",""]}` |
| `stored_enchantments` | 1 | D | `{}` |
| `sulfur_cube_content` | 0 | S | inconnu (composant sans documentation trouvée) |
| `suspicious_stew_effects` | 1 | D | `[]` |
| `tool` | 38 | D | `{"can_destroy_blocks_in_creative":false,"damage_per_block":2,"rules":[{"blocks":"minecraft:cobweb","correct...` |
| `tooltip_display` | 1658 | D | `{}` |
| `tooltip_style` | 0 | S | id |
| `trim` | 0 | S | {material, pattern} |
| `tropical_fish/base_color` | 0 | S | chaîne |
| `tropical_fish/pattern` | 0 | S | chaîne |
| `tropical_fish/pattern_color` | 0 | S | chaîne |
| `unbreakable` | 0 | S | {} |
| `use_cooldown` | 3 | D | `{"seconds":1.0}` |
| `use_effects` | 1658 | D | `{}` |
| `use_remainder` | 7 | D | `{"id":"minecraft:bowl"}` |
| `villager/variant` | 0 | S | id |
| `villager_food` | 4 | D | `{"nutrition":1}` |
| `waxed` | 0 | S | inconnu (composant sans documentation trouvée) |
| `weapon` | 44 | D | `{"item_damage_per_attack":2}` |
| `wolf/collar` | 0 | S | chaîne (couleur) |
| `wolf/sound_variant` | 0 | S | id |
| `wolf/variant` | 0 | S | id |
| `writable_book_content` | 1 | D | `{}` |
| `written_book_content` | 0 | S | {title, author, generation, pages, resolved} |
| `zombie_nautilus/variant` | 0 | S | id |

| Réf | Règle | Preuve |
|---|---|---|
| R10 | Le décodage d'un composant se fait par son identifiant de registre : un identifiant inconnu dans une table de composants est une erreur de décodage de la donnée entière (jamais ignoré silencieusement). Provisoire. | S |
| R11 | Valeurs observées de `consumable` : `{}` ou `{"on_consume_effects":[...]}` ; les effets de consommation sont de 5 types (`apply_effects`, `remove_effects`, `clear_all_effects`, `teleport_randomly`, `play_sound`), exemple `chicken` : `apply_effects` avec `probability` 0.3 et effet `hunger` 600 ticks ; `death_protection` (totem) porte les `death_effects` `clear_all_effects` puis `apply_effects` (régénération amplificateur 1 durée 900, absorption 1 durée 100, résistance au feu durée 800). | D |
| R12 | Formes JSON observées à conserver telles quelles par le décodeur (D, voir le catalogue pour les valeurs) : `food` `{nutrition, saturation}` ; `villager_food` `{nutrition}` ; `tool` `{rules:[{blocks, speed?, correct_for_drops?}], damage_per_block?, can_destroy_blocks_in_creative?}` ; `weapon` `{item_damage_per_attack?}` ; `equippable` `{slot, swappable?, equip_sound?, asset_id?, allowed_entities?, can_be_sheared?, shearing_sound?}` ; `attribute_modifiers` liste de `{type, id, amount, operation, slot, display?}` (opérations vues : `add_value`, `add_multiplied_base`, `add_multiplied_total` ; montants flottants conservés au bit près : `-2.799999952316284`) ; `repairable` `{items}` (étiquette ou liste) ; `enchantable` `{value}` (11 valeurs distinctes : 1, 5, 8, 9, 10, 12, 13, 14, 15, 22, 25) ; `damage_resistant` `{types}` ; `blocks_attacks` ; `kinetic_weapon`, `piercing_weapon`, `attack_range` (lance) ; `cooking_fuel` `{burn_time, speed_multiplier}` (références à des fournisseurs de données : `item-recipes.md`). Les champs ayant une valeur par défaut de codec sont optionnels en entrée. | D |
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
| R24 | Forme NBT d'une pile dans un inventaire de block entity : Compound `{id: String, count: Int, Slot: Byte?, components: Compound?}`. Observé sur les 1511 structures : 36 piles, toutes avec `id` et `count` (type Int), `Slot` (type Byte) quand la pile est dans une liste `Items`, `components` seulement si la pile a des modifications. Aucune pile n'est observée avec `count` absent. Exemple : `{count: 1, Slot: 0b, components: {"minecraft:potion_contents": {potion: "minecraft:strong_healing"}}, id: "minecraft:potion"}`. | D |
| R25 | Les listes `Items` de block entity sont creuses : seules les piles non vides sont écrites, chacune avec son `Slot`. Exemples : un four dont l'entrée est vide écrit `Slot` 1 (combustible) et 2 (sortie) ; un alambic écrit `Slot` 0 et 2. | D |
| R26 | Lecture d'une pile NBT : `count` absent vaut 1 ; `id` inconnu produit une pile vide (la pile est ignorée) ; `components` absent vaut "aucune modification". Provisoire. Sonde PI6. | S |
| R27 | Écriture d'une pile NBT : `components` n'est écrit que si la pile a au moins une modification (R17). Un composant retiré du prototype s'écrit sous la clé `!<id>` avec la valeur `{}`. Provisoire. Sonde PI2. | S |
| R28 | Forme JSON d'une pile dans les données (résultat de recette, sortie d'alchimie, décorations de pot, offre) : objet `{id, count?, components?}`. Un `count` absent signifie 1 (1294 résultats de recette ou sorties d'alchimie sans `count`, 727 avec). Un objet vide `{}` est accepté comme résultat de 2 recettes (`map_cloning`, `map_extending`) et signifie "résultat dérivé de l'entrée" (voir `item-recipes.md`). Le champ `components` suit R9 et accepte la clé préfixée `!`. | D (forme) ; S (défaut 1) |
| R29 | Forme textuelle des commandes (argument `minecraft:item_stack`) : `<id>[<composant>=<valeur>, !<composant>, ...]` avec des valeurs en SNBT (`core-nbt` R41 à R50). L'argument `minecraft:item_predicate` est une forme distincte (étiquettes `#`, motif `*`, correspondance partielle de composants) : voir `loot-tables.md`. Provisoire. Sonde PI2. | S |

### Conteneurs et block entities à inventaire

| Réf | Règle | Preuve |
|---|---|---|
| R30 | Un conteneur est un tableau de slots de taille fixe, chaque slot étant une pile (éventuellement vide). Chaque slot a une quantité maximale propre (min de `max_stack_size` de la pile et de la limite du conteneur). Provisoire pour la limite (coffre 64). | S |
| R31 | Disposition NBT des block entities à inventaire, relevée sur les gabarits de structure (D ; nombre de block entities de ce type entre parenthèses). Coffre (473), coffre piégé, tonneau (226) : `Items`, `LootTable` (`LootTableSeed` n'apparaît pas dans les gabarits) ; `components` présent sur 210 coffres et 204 tonneaux (compound, vide dans les gabarits). Four (28), haut fourneau (11), fumoir (9) : `Items` (slots 0 ingrédient, 1 combustible, 2 sortie), `lit_time_remaining` Short, `lit_total_time` Short, `cooking_time_spent` Short, `cooking_total_time` Short, `RecipesUsed` (Compound identifiant de recette vers Int). Alambic (11) : `Items` (slots 0 à 4), `BrewTime` Short, `Fuel` Byte. Feu de camp (128) : `Items`, `CookingTimes` Int_Array de 4, `CookingTotalTimes` Int_Array de 4. Entonnoir (1) : `Items`, `TransferCooldown` Int. Distributeur (7) : `Items`, `LootTable`. Pot décoré (5) : `sherds` (compound `back`, `front`, `left`, `right`, chacun une pile `{id}`), `LootTable`, `item`. Coffre-fort (2) : `config` (`key_item` pile, `loot_table`). Lutrin (11) : aucun champ au repos. | D |
| R32 | `LootTable` est un String (identifiant de table), `LootTableSeed` un Long ; leur absence signifie conteneur matérialisé ou sans butin. Un conteneur avec `LootTable` n'a pas d'`Items` écrits dans les gabarits (350 coffres sur 473 ont `LootTable`, 210 ont `Items`). La matérialisation (déballage) est décrite dans `loot-tables.md`. | D (forme) ; S (sémantique) |
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
| R37 | Indices de slots d'un menu : les slots propres du menu d'abord, puis 27 slots d'inventaire principal, puis 9 de barre rapide (sauf `lectern`, qui n'inclut pas l'inventaire du joueur). Indices propres : `generic_9xN` 0 à 9N-1 ; `shulker_box` 0 à 26 ; `generic_3x3` 0 à 8 (x + 3y) ; `crafter_3x3` 0 à 8 puis le résultat en 45 (après le joueur) ; `crafting` 0 résultat, 1 à 9 grille (1 + x + 3y) ; `furnace`, `blast_furnace`, `smoker` 0 ingrédient, 1 combustible, 2 sortie ; `enchantment` 0 objet, 1 lapis-lazuli ; `brewing_stand` 0 à 2 bouteilles, 3 ingrédient, 4 poudre de blaze ; `anvil` 0 premier objet, 1 second, 2 résultat ; `grindstone` 0 premier, 1 second, 2 résultat ; `beacon` 0 paiement ; `hopper` 0 à 4 ; `merchant` 0 et 1 entrées, 2 résultat ; `lectern` 0 livre ; `loom` 0 bannière, 1 teinture, 2 motif, 3 résultat ; `smithing` 0 modèle, 1 base, 2 ajout, 3 résultat ; `cartography_table` 0 carte, 1 papier, 2 sortie ; `stonecutter` 0 entrée, 1 résultat. Source : page publique citée en Sources. | S |
| R38 | Les slots de résultat (fabrication, four, enclume, meule, commerce, métier à tisser, forge, table de cartographie, tailleur de pierre) n'acceptent aucun dépôt. Les slots d'ingrédient d'un four n'acceptent que ce que la recette ou le carburant admet, le slot de sortie ne se remplit pas à la main. Les slots d'armure de la fenêtre joueur n'acceptent que les objets dont `equippable.slot` correspond. Provisoire (détail par menu : PI9). | S |
| R39 | Seul le menu "ouvert" reçoit les clics. Un menu fermé rend la pile du curseur à l'inventaire, ou la jette au sol si l'inventaire est plein. Provisoire. Sonde PI9. | S |

### Opérations de clic

Les opérations sont décrites indépendamment des numéros de paquets (M8). Les modes de clic du protocole 777 se rapportent aux six opérations ci-dessous (clic normal, clic maj, touche numérique ou main secondaire, clic milieu en créatif, jeter, glisser, double clic) ; la table de correspondance exacte est du domaine du paquet (M8).

| Réf | Règle | Preuve |
|---|---|---|
| R40 | Clic gauche sur un slot, curseur vide : le curseur prend toute la pile. Curseur plein, slot vide : dépose toute la pile. Curseur et slot du même contenu : fusion selon R19, le reste reste au curseur. Contenus différents : échange (si le slot accepte la pile du curseur). | S |
| R41 | Clic droit, curseur vide : le curseur prend la moitié supérieure de la pile (pile de 7 : 4 au curseur, 3 restent). Curseur plein : dépose une unité (slot vide ou même contenu avec place). | S |
| R42 | Déplacement rapide (maj-clic) : la pile va vers l'autre région du menu. Joueur vers conteneur : plage du conteneur en ordre croissant, en fusionnant d'abord dans les piles existantes. Conteneur vers joueur : plage du joueur en ordre décroissant (barre rapide d'abord, de la droite vers la gauche). Fenêtre joueur seule : barre rapide vers inventaire principal et inverse, en ordre croissant. Une pile qui ne rentre pas entièrement reste partiellement en place. Les règles par menu spécial (four, alambic, enclume, armures, enchantement, métier à tisser, forge, résultats) sont S et ne se déduisent pas du cas général. | S |
| R43 | Touche numérique n (1 à 9) : échange le slot survolé avec le slot n de la barre rapide (si l'un est vide, déplacement). Touche main secondaire : idem avec la main secondaire. | S |
| R44 | Jeter (Q) : une unité du slot survolé (ou de la main sélectionnée) ; jeter avec modificateur : toute la pile ; clic hors fenêtre avec une pile au curseur : jette la pile (clic gauche) ou une unité (clic droit). | S |
| R45 | Glisser (drag) : clic gauche glissé sur n slots admissibles (vides ou de même contenu avec place) répartit la pile du curseur en parts égales de floor(quantité / n), le reste reste au curseur ; clic droit glissé dépose une unité par slot ; clic milieu glissé (créatif) dépose une pile pleine par slot. Un slot interdit (résultat, slot filtré) est ignoré. Pour n = 1, un glisser gauche équivaut à un dépôt complet. | S |
| R46 | Double clic : rassemble au curseur, jusqu'au maximum de pile, les piles de même contenu du menu ; l'ordre de collecte et l'exclusion du slot de résultat ne sont pas documentés par la page publique. Provisoire : on parcourt les slots du menu en ordre croissant, en visitant d'abord les piles non pleines, sans toucher aux slots de résultat. Sonde PI9. | S |
| R47 | Retrait d'un résultat de fabrication : consomme les ingrédients selon la recette et rend les restes (`item-recipes.md`). Si le résultat ne rentre pas dans l'inventaire (maj-clic), l'opération est refusée ou partielle sans duplication et sans perte. Provisoire. | S |

## Entrées / sorties

- **Pile** : (identifiant, quantité entière, modifications) ; plage de quantité 0 à 99 (R16) ; composants selon le catalogue R9.
- **Lecture** : prototype par identifiant depuis les 1658 fichiers ; pile depuis NBT (R24 à R27), JSON (R28) ou texte de commande (R29).
- **Écriture NBT** : `id`, `count`, `components` si modifications. L'ordre des clés écrites suit l'ordre d'insertion (`core-nbt` R30) ; l'égalité entre piles se fait sur l'arbre décodé.
- **Conteneur** : tableau de piles indexé ; sérialisation `Items` creuse (R25).
- **Menu** : entrée = (type de menu, indices de slots R37, clic R40 à R46), sortie = nouvel état des slots, du curseur et des piles jetées.

## Cas limites

- Quantité 0 ou négative à la lecture : pile vide (R15) ; quantité supérieure à `max_stack_size` : S, sonde PI1.
- Composant à valeur `{}` contre composant absent (R6) : deux états distincts.
- Pile à 99 d'un objet dont `max_stack_size` est 16 par modification : la pile garde sa quantité ou est ramenée au maximum : S, sonde PI1.
- Composant `max_stack_size` modifié à 1 sur une pile de 64 : S, PI1.
- Flottants -0.0 et NaN dans un composant : conservés au bit près (`core-nbt` R35).
- Imbrication : un coffre dans un coffre dans un coffre... via le composant `container` ; le bundle dans un bundle : S (PI4).
- Déplacement rapide quand deux piles partielles admissibles existent (ordre de remplissage) ; glisser avec des capacités hétérogènes ; double clic avec le slot de résultat de fabrication au milieu de la plage.
- Fermeture du menu avec une pile au curseur et un inventaire plein.

## Vecteurs de test

Tous les vecteurs suivants sont de niveau D ou C. Le script de dénombrement lit uniquement les fichiers extraits.

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

Vecteurs de comportement (S, à produire par les sondes) :

| Vecteur | Entrée | Sortie attendue (provisoire) |
|---|---|---|
| V-I10 | Curseur vide, slot de 7 pierres, clic droit | curseur 4, slot 3 (R41) |
| V-I11 | Curseur 10 pierres, glisser gauche sur 3 slots vides | 3 par slot, 1 au curseur (R45) |
| V-I12 | Curseur 10 pierres, glisser droit sur 3 slots vides | 1 par slot, 7 au curseur (R45) |
| V-I13 | Curseur 64 pierres, clic gauche sur slot de 60 pierres | slot 64, curseur 60 (R40, R19) |
| V-I14 | `give @s stone 100` | une pile de 64 et une de 36 (R16) |

## Plan de tests

- **Unitaire (L1)** : chargement des 1658 prototypes et comparaison de chacun avec son fichier (R2, R3, R5, V-I1 à V-I9, V-I9 invariants) ; aller-retour NBT, JSON et texte pour chacun des 122 composants avec la valeur du catalogue ; égalité et fusion (R17 à R20) en test de propriétés (fusionner puis fractionner conserve le total) ; tous les V-I10 à V-I14 sur le moteur de menu avec des conteneurs factices.
- **Différentiel (L2)** : sondes PI1 à PI9 ; relecture de nos piles par l'oracle via `/give` et `/data get`.
- **Niveau B** : tests écrits en aveugle par le `test-engineer` à partir de cette spec.

## Sondes proposées

Cadre : serveur 26.3 headless, monde plat `level-name=sonde`, RCON, `/tick freeze` au besoin. Joueur factice : un client protocole minimal est nécessaire aux sondes PI7 à PI9 (voir Questions ouvertes, bloquant partiel).

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Règles levées |
|---|---|---|---|
| PI1 | `give @s minecraft:stone 100`, `give @s minecraft:ender_pearl 40`, `give @s minecraft:stone[minecraft:max_stack_size=1] 3`, `give @s minecraft:stone[minecraft:max_stack_size=100]`, `give @s minecraft:diamond_pickaxe[minecraft:max_stack_size=64]`, puis `data get entity @s Inventory` | Répartition en piles de 64 et 36 ; 16 et 16 et 8 pour les perles ; pour la pile de 100 et le cas durabilité, message d'erreur ou valeur bornée | R16, R5 |
| PI2 | `give @s minecraft:stone[minecraft:rarity=common]`, `give @s minecraft:stone[!minecraft:rarity]`, `give @s minecraft:stone[minecraft:rarity=epic]`, `give @s minecraft:diamond_sword[!minecraft:attribute_modifiers]`, `data get entity @s Inventory[0]` pour chaque | Savoir si la modification égale au prototype est écrite, comment s'écrit le retrait (clé `!`) et la forme de `components` | R17, R27, R29 |
| PI3 | Pour chacun des 122 composants, `give @s minecraft:stone[<composant>=<valeur du catalogue>]` (ou l'objet qui le porte) puis `data get entity @s Inventory[-1]` ; consigner l'acceptation, le rejet et la forme relue | Table composant -> forme NBT relue. Lève les 53 lignes S du catalogue | R9, R10 |
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

Les règles S qui ne citent pas de sonde dans leur ligne sont levées ainsi : R14, R20, R22 : PI2, PI3, PI5 ; R28 (défaut de `count`) : PI3 ; R30 (limite de pile d'un conteneur) : PI9 ; R32 (déballage) : sondes PL5 de `loot-tables.md` ; R33 (sens de `RecipesUsed`) : PR8 de `item-recipes.md` ; R36 et R37 (indices) : PI7, PI9. Les règles R38 à R47 sont levées par PI9 et PI10. Aucune règle S n'est sans sonde.

## Questions ouvertes

- Les 53 types de composants absents des prototypes (R8, R9) restent S : forme à confirmer (PI3). `sulfur_cube_content` et `waxed` n'ont aucune documentation publique trouvée ; leur forme est inconnue.
- H-inv-a contre H-inv-b (R34).
- Liste complète des noms de slots (R35).
- Les règles de menu R38 à R47 demandent un client protocole côté sonde (PI9, PI10). Bloquant partiel pour L2 de cette spec : sans client, seuls PI1 à PI8, PI11, PI12 sont exécutables. Le client minimal doit être écrit par le `tooling-engineer` à partir de la documentation publique du protocole ; il ne doit pas lire l'implémentation de référence.
- Le déplacement rapide par menu spécial (four, alambic, enclume, enchantement, métier à tisser, forge, armures) n'est pas décrit : ajouter une table par menu après PI9.
- Le comportement exact de `max_stack_size` supérieur à 64 (R16) et de la quantité hors plage.
- Mise à niveau des piles de versions antérieures (correcteur de données) : hors périmètre ; les mondes visés sont ceux de la data version 5023.
- Règles S restantes : R10, R14 à R23 (sauf les parties D), R26 à R29, R30, R34 à R47.

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Objets et composants | M6 | B | spécifié | `docs/specs/item-components-inventory.md` (règles D/C pour le registre, les prototypes, les formes NBT et JSON ; S pour la pile et les menus) |
| Inventaires et menus (ligne à créer) | M6 | B | spécifié | idem, R30 à R47 en S, sondes PI7 à PI10 |

## Annexe : script de dénombrement (C)

Lit uniquement les fichiers extraits ; aucune dépendance au jeu. À copier hors du dossier de données, puis `python -I components_stats.py .cache/reference/26.3`.

```python
import glob, json, os, sys, collections

ref = sys.argv[1]
items = {}
for f in glob.glob(os.path.join(ref, "generated/reports/minecraft/components/item/*.json")):
    items[os.path.basename(f)[:-5]] = json.load(open(f))["components"]
count = collections.Counter()
for comps in items.values():
    count.update(comps.keys())
print("objets", len(items))
for k, v in sorted(count.items(), key=lambda kv: (-kv[1], kv[0])):
    print(v, k)
print("pile max", collections.Counter(c["minecraft:max_stack_size"] for c in items.values()))
bad = [n for n, c in items.items() if "minecraft:max_damage" in c and c["minecraft:max_stack_size"] != 1]
print("max_damage avec pile > 1 :", bad)
```

# Inventaire des registres et des fichiers de données 26.3

Statut : brouillon
Jalon : M2
Risque : A (comme `data-datapack-loading.md`)

## Périmètre

Compagnon de `data-datapack-loading.md` (les références Rn renvoient à ses règles). Donne, pour la version cible 26.3 (data version 5023), la liste complète des registres, le dossier de chaque registre et le nombre de fichiers mesuré dans les données extraites, ainsi que les vecteurs de résolution de tags. Ces comptes sont le critère de sortie de M2 (`docs/ROADMAP.md`).

Ne couvre pas le contenu des fichiers (décodage typé, jalons des modules propriétaires).

## Sources

- `generated/reports/registries.json` : registres statiques, entrées et `protocol_id` (D).
- `generated/reports/datapack.json` : registres chargeables par datapack, propriétés `elements`, `stable`, `tags`, et section `others` (D).
- `resources/data/minecraft/**` : fichiers du datapack vanilla et des trois packs intégrés sous `resources/data/minecraft/datapacks/` (D).
- Comptes et résolutions : script `data_inventory.py` en annexe (C), sans aucun code du jeu.

## Commande

Copier le script de l'annexe dans un dossier de scripts hors des données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I data_inventory.py "$REF"
```

Le script sort l'ensemble des tableaux ci-dessous (en anglais) et termine par `TOTAL` et `CHECK`. Toute différence avec ce document fait échouer le critère de sortie de M2 ou signale un changement des données de référence.

## Synthèse

| Mesure | Valeur | Preuve |
|---|---|---|
| Registres listés par `registries.json` (statiques) | 95, `protocol_id` 0 à 94, 7053 entrées | D |
| Registres listés par `datapack.json` | 155 : 95 sans fichiers d'éléments (exactement les 95 statiques), 60 avec fichiers (52 `stable: false`, 8 `stable: true`), tous avec tags | D |
| Autres contenus de pack (`others`) | `function` (`.mcfunction`, tags admis), `structure` (`.nbt`, tags refusés) | D |
| Fichiers JSON d'éléments du pack vanilla | 7383, répartis sur 57 registres ; 0 pour `dimension`, `item_modifier`, `slot_source` | C |
| Fichiers de tags du pack vanilla | 883, sur 20 registres (7 statiques, 13 à fichiers) ; 510 références `#` ; 0 référence manquante ; 0 cycle ; 10 tags vides | C |
| Structures `.nbt` / fonctions `.mcfunction` | 1511 / 0 | C |
| Packs intégrés | 3 (`minecart_improvements`, `redstone_experiments` : 0 fichier de données ; `trade_rebalance` : 86 éléments + 17 tags = 103) ; 3 `pack.mcmeta` | C |
| Fichiers hors registre | 1 marqueur vide `.mcassetsroot` à la racine de `data/` (et 1 dans `assets/`) | D |
| Total des fichiers extraits | 9887 = 7383 + 883 + 1511 + 1 marqueur + 106 (packs intégrés) + 3 (`assets/`) ; égal à `resourceFiles` de `state.json` | C |

Lecture des colonnes de la sortie : "vanilla files" compte les fichiers de `data/minecraft/<dossier>/**` du pack vanilla, hors `data/minecraft/datapacks/` ; "in subfolders" compte ceux dont l'identifiant contient un `/` ; "trade_rebalance files" compte les fichiers du pack intégré, remplacements compris (81 pour `villager_trade` : 1 remplacement, 80 ajouts).

Statut des vecteurs de tags : les tailles sont des ensembles d'identifiants distincts (C), indépendantes de l'ordre et du traitement des doublons ; l'ordre des entrées résolues n'est pas un vecteur ici (règle S de la spec principale). La colonne "vanilla + trade_rebalance" applique les règles provisoires d'empilement (remplacement d'un fichier entier, `replace`) : elle est C sous ces hypothèses, à confirmer par la sonde P16.

## Sortie de référence

Sortie exacte de la commande sur les données extraites de la version cible (preuve C). Les titres et en-têtes sont ceux du script.

### Static registries (registries.json)

| registry | protocol_id | entries | default | vanilla tag files |
|---|---|---|---|---|
| `minecraft:activity` | 28 | 26 | - | 0 |
| `minecraft:attribute` | 19 | 40 | - | 0 |
| `minecraft:attribute_type` | 91 | 15 | - | 0 |
| `minecraft:block` | 4 | 1286 | `minecraft:air` | 300 |
| `minecraft:block_entity_type` | 10 | 49 | - | 0 |
| `minecraft:block_predicate_type` | 39 | 16 | - | 0 |
| `minecraft:chunk_status` | 12 | 10 | `minecraft:empty` | 0 |
| `minecraft:command_argument_type` | 21 | 62 | - | 0 |
| `minecraft:consume_effect_type` | 74 | 5 | - | 0 |
| `minecraft:context_float_provider_type` | 32 | 28 | - | 0 |
| `minecraft:context_int_provider_type` | 33 | 23 | - | 0 |
| `minecraft:context_key_set` | 93 | 31 | - | 0 |
| `minecraft:creative_mode_tab` | 60 | 14 | - | 0 |
| `minecraft:custom_stat` | 11 | 78 | - | 0 |
| `minecraft:data_component_predicate_type` | 66 | 15 | - | 0 |
| `minecraft:data_component_type` | 63 | 122 | - | 0 |
| `minecraft:debug_subscription` | 5 | 16 | - | 0 |
| `minecraft:dialog_action_type` | 85 | 9 | - | 0 |
| `minecraft:dialog_body_type` | 87 | 2 | - | 0 |
| `minecraft:dialog_type` | 84 | 5 | - | 0 |
| `minecraft:enchantment_effect_component_type` | 68 | 31 | - | 0 |
| `minecraft:enchantment_entity_effect_type` | 70 | 15 | - | 0 |
| `minecraft:enchantment_level_based_value_type` | 69 | 6 | - | 0 |
| `minecraft:enchantment_location_based_effect_type` | 71 | 16 | - | 0 |
| `minecraft:enchantment_provider_type` | 73 | 3 | - | 0 |
| `minecraft:enchantment_value_effect_type` | 72 | 6 | - | 0 |
| `minecraft:entity_sub_predicate_type` | 65 | 25 | - | 0 |
| `minecraft:entity_type` | 6 | 161 | `minecraft:pig` | 49 |
| `minecraft:environment_attribute` | 90 | 51 | - | 0 |
| `minecraft:float_provider_type` | 36 | 4 | - | 0 |
| `minecraft:fluid` | 2 | 5 | `minecraft:empty` | 10 |
| `minecraft:game_event` | 0 | 61 | `minecraft:step` | 5 |
| `minecraft:game_rule` | 64 | 59 | - | 0 |
| `minecraft:height_provider_type` | 38 | 6 | - | 0 |
| `minecraft:incoming_rpc_methods` | 79 | 69 | - | 0 |
| `minecraft:input_control_type` | 86 | 4 | - | 0 |
| `minecraft:int_provider_type` | 37 | 8 | - | 0 |
| `minecraft:item` | 7 | 1658 | `minecraft:air` | 236 |
| `minecraft:loot_condition_type` | 31 | 20 | - | 0 |
| `minecraft:loot_function_type` | 30 | 42 | - | 0 |
| `minecraft:loot_nbt_provider_type` | 34 | 2 | - | 0 |
| `minecraft:loot_pool_entry_type` | 29 | 9 | - | 0 |
| `minecraft:loot_score_provider_type` | 35 | 2 | - | 0 |
| `minecraft:map_decoration_type` | 67 | 40 | - | 0 |
| `minecraft:memory_module_type` | 26 | 115 | `minecraft:dummy` | 0 |
| `minecraft:menu` | 16 | 25 | - | 0 |
| `minecraft:mob_effect` | 3 | 40 | - | 0 |
| `minecraft:number_format_type` | 62 | 3 | - | 0 |
| `minecraft:outgoing_rpc_methods` | 80 | 21 | - | 0 |
| `minecraft:particle_type` | 9 | 128 | - | 0 |
| `minecraft:permission_check_type` | 89 | 2 | - | 0 |
| `minecraft:permission_type` | 88 | 2 | - | 0 |
| `minecraft:point_of_interest_type` | 25 | 21 | - | 3 |
| `minecraft:pos_rule_test` | 15 | 3 | - | 0 |
| `minecraft:position_source_type` | 20 | 2 | - | 0 |
| `minecraft:potion` | 8 | 46 | - | 5 |
| `minecraft:recipe_book_category` | 77 | 13 | - | 0 |
| `minecraft:recipe_display` | 75 | 5 | - | 0 |
| `minecraft:recipe_serializer` | 18 | 22 | - | 0 |
| `minecraft:recipe_type` | 17 | 8 | - | 0 |
| `minecraft:rule_block_entity_modifier` | 14 | 4 | - | 0 |
| `minecraft:rule_test` | 13 | 10 | - | 0 |
| `minecraft:sensor_type` | 27 | 24 | `minecraft:dummy` | 0 |
| `minecraft:slot_display` | 76 | 11 | - | 0 |
| `minecraft:slot_source_type` | 92 | 6 | - | 0 |
| `minecraft:sound_event` | 1 | 1991 | - | 0 |
| `minecraft:spawn_condition_type` | 83 | 3 | - | 0 |
| `minecraft:stat_type` | 22 | 9 | - | 0 |
| `minecraft:test_environment_definition_type` | 81 | 7 | - | 0 |
| `minecraft:test_function` | 94 | 1 | - | 0 |
| `minecraft:test_instance_type` | 82 | 2 | - | 0 |
| `minecraft:ticket_type` | 78 | 9 | - | 0 |
| `minecraft:trigger_type` | 61 | 58 | - | 0 |
| `minecraft:villager_profession` | 24 | 15 | `minecraft:none` | 0 |
| `minecraft:villager_type` | 23 | 7 | `minecraft:plains` | 0 |
| `minecraft:worldgen/biome_source` | 52 | 4 | - | 0 |
| `minecraft:worldgen/block_state_provider_type` | 46 | 10 | - | 0 |
| `minecraft:worldgen/carver_type` | 40 | 2 | - | 0 |
| `minecraft:worldgen/chunk_generator` | 53 | 3 | - | 0 |
| `minecraft:worldgen/density_function_type` | 56 | 44 | - | 0 |
| `minecraft:worldgen/feature_size_type` | 51 | 2 | - | 0 |
| `minecraft:worldgen/feature_type` | 41 | 58 | - | 0 |
| `minecraft:worldgen/foliage_placer_type` | 47 | 12 | - | 0 |
| `minecraft:worldgen/material_condition_type` | 54 | 11 | - | 0 |
| `minecraft:worldgen/material_rule_type` | 55 | 5 | - | 0 |
| `minecraft:worldgen/placement_modifier_type` | 45 | 18 | - | 0 |
| `minecraft:worldgen/pool_alias_binding` | 59 | 3 | - | 0 |
| `minecraft:worldgen/root_placer_type` | 49 | 1 | - | 0 |
| `minecraft:worldgen/structure_piece` | 43 | 56 | - | 0 |
| `minecraft:worldgen/structure_placement` | 42 | 3 | - | 0 |
| `minecraft:worldgen/structure_pool_element` | 58 | 5 | - | 0 |
| `minecraft:worldgen/structure_processor` | 57 | 11 | - | 0 |
| `minecraft:worldgen/structure_type` | 44 | 16 | - | 0 |
| `minecraft:worldgen/tree_decorator_type` | 50 | 11 | - | 0 |
| `minecraft:worldgen/trunk_placer_type` | 48 | 10 | - | 0 |

### Datapack registries with elements (datapack.json)

| registry | folder | stable | vanilla files | in subfolders | vanilla tag files | trade_rebalance files | trade_rebalance tag files |
|---|---|---|---|---|---|---|---|
| `minecraft:advancement` | `data/<ns>/advancement/` | true | 1866 | 1866 | 0 | 0 | 0 |
| `minecraft:banner_pattern` | `data/<ns>/banner_pattern/` | false | 43 | 0 | 11 | 0 | 0 |
| `minecraft:block_transformer` | `data/<ns>/block_transformer/` | false | 3 | 0 | 0 | 0 | 0 |
| `minecraft:cat_sound_variant` | `data/<ns>/cat_sound_variant/` | false | 2 | 0 | 0 | 0 | 0 |
| `minecraft:cat_variant` | `data/<ns>/cat_variant/` | false | 11 | 0 | 0 | 0 | 0 |
| `minecraft:chat_type` | `data/<ns>/chat_type/` | false | 7 | 0 | 0 | 0 | 0 |
| `minecraft:chicken_sound_variant` | `data/<ns>/chicken_sound_variant/` | false | 2 | 0 | 0 | 0 | 0 |
| `minecraft:chicken_variant` | `data/<ns>/chicken_variant/` | false | 3 | 0 | 0 | 0 | 0 |
| `minecraft:context_float_provider` | `data/<ns>/context_float_provider/` | true | 4 | 4 | 0 | 0 | 0 |
| `minecraft:context_int_provider` | `data/<ns>/context_int_provider/` | true | 26 | 26 | 0 | 0 | 0 |
| `minecraft:cow_sound_variant` | `data/<ns>/cow_sound_variant/` | false | 2 | 0 | 0 | 0 | 0 |
| `minecraft:cow_variant` | `data/<ns>/cow_variant/` | false | 3 | 0 | 0 | 0 | 0 |
| `minecraft:damage_type` | `data/<ns>/damage_type/` | false | 51 | 0 | 36 | 0 | 0 |
| `minecraft:decorated_pot_pattern` | `data/<ns>/decorated_pot_pattern/` | false | 23 | 0 | 0 | 0 | 0 |
| `minecraft:dialog` | `data/<ns>/dialog/` | false | 3 | 0 | 2 | 0 | 0 |
| `minecraft:dimension` | `data/<ns>/dimension/` | false | 0 | 0 | 0 | 0 | 0 |
| `minecraft:dimension_type` | `data/<ns>/dimension_type/` | false | 4 | 0 | 0 | 0 | 0 |
| `minecraft:enchantment` | `data/<ns>/enchantment/` | false | 43 | 0 | 22 | 0 | 7 |
| `minecraft:enchantment_provider` | `data/<ns>/enchantment_provider/` | false | 7 | 4 | 0 | 0 | 0 |
| `minecraft:frog_variant` | `data/<ns>/frog_variant/` | false | 3 | 0 | 0 | 0 | 0 |
| `minecraft:instrument` | `data/<ns>/instrument/` | false | 8 | 0 | 3 | 0 | 0 |
| `minecraft:item_modifier` | `data/<ns>/item_modifier/` | true | 0 | 0 | 0 | 0 | 0 |
| `minecraft:jukebox_song` | `data/<ns>/jukebox_song/` | false | 22 | 0 | 0 | 0 | 0 |
| `minecraft:loot_table` | `data/<ns>/loot_table/` | true | 1447 | 1447 | 0 | 5 | 0 |
| `minecraft:painting_variant` | `data/<ns>/painting_variant/` | false | 51 | 0 | 1 | 0 | 0 |
| `minecraft:pig_sound_variant` | `data/<ns>/pig_sound_variant/` | false | 3 | 0 | 0 | 0 | 0 |
| `minecraft:pig_variant` | `data/<ns>/pig_variant/` | false | 3 | 0 | 0 | 0 | 0 |
| `minecraft:predicate` | `data/<ns>/predicate/` | true | 3 | 3 | 0 | 0 | 0 |
| `minecraft:recipe` | `data/<ns>/recipe/` | true | 2042 | 279 | 0 | 0 | 0 |
| `minecraft:slot_source` | `data/<ns>/slot_source/` | true | 0 | 0 | 0 | 0 | 0 |
| `minecraft:sulfur_cube_archetype` | `data/<ns>/sulfur_cube_archetype/` | false | 12 | 0 | 0 | 0 | 0 |
| `minecraft:test_environment` | `data/<ns>/test_environment/` | false | 1 | 0 | 0 | 0 | 0 |
| `minecraft:test_instance` | `data/<ns>/test_instance/` | false | 1 | 0 | 0 | 0 | 0 |
| `minecraft:timeline` | `data/<ns>/timeline/` | false | 4 | 0 | 4 | 0 | 0 |
| `minecraft:trade_set` | `data/<ns>/trade_set/` | false | 68 | 68 | 0 | 0 | 0 |
| `minecraft:trial_spawner` | `data/<ns>/trial_spawner/` | false | 28 | 28 | 0 | 0 | 0 |
| `minecraft:trim_material` | `data/<ns>/trim_material/` | false | 11 | 0 | 0 | 0 | 0 |
| `minecraft:trim_pattern` | `data/<ns>/trim_pattern/` | false | 18 | 0 | 0 | 0 | 0 |
| `minecraft:villager_trade` | `data/<ns>/villager_trade/` | false | 391 | 391 | 73 | 81 | 10 |
| `minecraft:wolf_sound_variant` | `data/<ns>/wolf_sound_variant/` | false | 7 | 0 | 0 | 0 | 0 |
| `minecraft:wolf_variant` | `data/<ns>/wolf_variant/` | false | 9 | 0 | 0 | 0 | 0 |
| `minecraft:world_clock` | `data/<ns>/world_clock/` | false | 2 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/biome` | `data/<ns>/worldgen/biome/` | false | 67 | 0 | 86 | 0 | 0 |
| `minecraft:worldgen/block_state_provider` | `data/<ns>/worldgen/block_state_provider/` | false | 8 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/carver` | `data/<ns>/worldgen/carver/` | false | 4 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/density_function` | `data/<ns>/worldgen/density_function/` | false | 55 | 51 | 0 | 0 | 0 |
| `minecraft:worldgen/feature` | `data/<ns>/worldgen/feature/` | false | 240 | 6 | 1 | 0 | 0 |
| `minecraft:worldgen/flat_level_generator_preset` | `data/<ns>/worldgen/flat_level_generator_preset/` | false | 9 | 0 | 1 | 0 | 0 |
| `minecraft:worldgen/material_condition` | `data/<ns>/worldgen/material_condition/` | false | 8 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/material_rule` | `data/<ns>/worldgen/material_rule/` | false | 42 | 35 | 0 | 0 | 0 |
| `minecraft:worldgen/multi_noise_biome_source_parameter_list` | `data/<ns>/worldgen/multi_noise_biome_source_parameter_list/` | false | 2 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/noise` | `data/<ns>/worldgen/noise/` | false | 64 | 2 | 0 | 0 | 0 |
| `minecraft:worldgen/noise_settings` | `data/<ns>/worldgen/noise_settings/` | false | 7 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/placed_feature` | `data/<ns>/worldgen/placed_feature/` | false | 273 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/processor_list` | `data/<ns>/worldgen/processor_list/` | false | 40 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/structure` | `data/<ns>/worldgen/structure/` | false | 52 | 0 | 33 | 0 | 0 |
| `minecraft:worldgen/structure_set` | `data/<ns>/worldgen/structure_set/` | false | 21 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/template_pool` | `data/<ns>/worldgen/template_pool/` | false | 245 | 244 | 0 | 0 | 0 |
| `minecraft:worldgen/world_preset` | `data/<ns>/worldgen/world_preset/` | false | 7 | 0 | 2 | 0 | 0 |
| `minecraft:zombie_nautilus_variant` | `data/<ns>/zombie_nautilus_variant/` | false | 2 | 0 | 0 | 0 | 0 |

### Other pack contents (datapack.json, section others)

| kind | folder | format | tags | vanilla files | vanilla tag files |
|---|---|---|---|---|---|
| `function` | `data/<ns>/function/` | mcfunction | true | 0 | 0 |
| `structure` | `data/<ns>/structure/` | structure | false | 1511 | 0 |

### Vanilla tags per registry

| registry | tag files | tag references | missing refs | cycles | empty tags | failed tags |
|---|---|---|---|---|---|---|
| `minecraft:banner_pattern` | 11 | 0 | 0 | 0 | 0 | 0 |
| `minecraft:block` | 300 | 278 | 0 | 0 | 4 | 0 |
| `minecraft:damage_type` | 36 | 8 | 0 | 0 | 1 | 0 |
| `minecraft:dialog` | 2 | 0 | 0 | 0 | 2 | 0 |
| `minecraft:enchantment` | 22 | 6 | 0 | 0 | 0 | 0 |
| `minecraft:entity_type` | 49 | 12 | 0 | 0 | 0 | 0 |
| `minecraft:fluid` | 10 | 2 | 0 | 0 | 0 | 0 |
| `minecraft:game_event` | 5 | 1 | 0 | 0 | 0 | 0 |
| `minecraft:instrument` | 3 | 2 | 0 | 0 | 0 | 0 |
| `minecraft:item` | 236 | 136 | 0 | 0 | 0 | 0 |
| `minecraft:painting_variant` | 1 | 0 | 0 | 0 | 0 | 0 |
| `minecraft:point_of_interest_type` | 3 | 1 | 0 | 0 | 0 | 0 |
| `minecraft:potion` | 5 | 0 | 0 | 0 | 0 | 0 |
| `minecraft:timeline` | 4 | 3 | 0 | 0 | 0 | 0 |
| `minecraft:villager_trade` | 73 | 15 | 0 | 0 | 3 | 0 |
| `minecraft:worldgen/biome` | 86 | 43 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/feature` | 1 | 0 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/flat_level_generator_preset` | 1 | 0 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/structure` | 33 | 2 | 0 | 0 | 0 | 0 |
| `minecraft:worldgen/world_preset` | 2 | 1 | 0 | 0 | 0 | 0 |

Empty vanilla tags: `#minecraft:default_immune_to` (block), `#minecraft:incorrect_for_diamond_tool` (block), `#minecraft:incorrect_for_netherite_tool` (block), `#minecraft:supports_frogspawn` (block), `#minecraft:bypasses_cooldown` (damage_type), `#minecraft:pause_screen_additions` (dialog), `#minecraft:quick_actions` (dialog), `#minecraft:common_smith/level_3` (villager_trade), `#minecraft:common_smith/level_4` (villager_trade), `#minecraft:common_smith/level_5` (villager_trade)

### Tag resolution vectors (distinct entries after full expansion)

| tag | registry | vanilla | vanilla + trade_rebalance |
|---|---|---|---|
| `#minecraft:logs` | `minecraft:block` | 48 | 48 |
| `#minecraft:wool` | `minecraft:block` | 16 | 16 |
| `#minecraft:mineable/pickaxe` | `minecraft:block` | 542 | 542 |
| `#minecraft:planks` | `minecraft:item` | 13 | 13 |
| `#minecraft:water` | `minecraft:fluid` | 2 | 2 |
| `#minecraft:lava` | `minecraft:fluid` | 2 | 2 |
| `#minecraft:is_overworld` | `minecraft:worldgen/biome` | 56 | 56 |
| `#minecraft:non_treasure` | `minecraft:enchantment` | 36 | 36 |
| `#minecraft:armorer/level_1` | `minecraft:villager_trade` | 5 | 2 |
| `#minecraft:librarian/level_1` | `minecraft:villager_trade` | 3 | 9 |

### Built-in packs (resources/data/minecraft/datapacks)

| pack | min_format | max_format | features.enabled | element files | tag files |
|---|---|---|---|---|---|
| `minecart_improvements` | 121 | 121 | `minecraft:minecart_improvements` | 0 | 0 |
| `redstone_experiments` | 121 | 121 | `minecraft:redstone_experiments` | 0 | 0 |
| `trade_rebalance` | 121 | 121 | `minecraft:trade_rebalance` | 86 | 17 |

### Stack vanilla then trade_rebalance

Overridden elements: `minecraft:chests/abandoned_mineshaft` (loot_table), `minecraft:chests/ancient_city` (loot_table), `minecraft:chests/desert_pyramid` (loot_table), `minecraft:chests/jungle_temple` (loot_table), `minecraft:chests/pillager_outpost` (loot_table), `minecraft:librarian/2/book_emerald` (villager_trade)

Replaced tags (replace true): `#minecraft:armorer/level_1`, `#minecraft:armorer/level_2`, `#minecraft:armorer/level_3`, `#minecraft:armorer/level_4`, `#minecraft:armorer/level_5`, `#minecraft:librarian/level_1`, `#minecraft:librarian/level_2`, `#minecraft:librarian/level_3`, `#minecraft:librarian/level_4`, `#minecraft:librarian/level_5`

New tags: `#minecraft:trades/desert_common` (enchantment), `#minecraft:trades/jungle_common` (enchantment), `#minecraft:trades/plains_common` (enchantment), `#minecraft:trades/savanna_common` (enchantment), `#minecraft:trades/snow_common` (enchantment), `#minecraft:trades/swamp_common` (enchantment), `#minecraft:trades/taiga_common` (enchantment)

- `minecraft:enchantment`: elements 43 -> 43, tags 22 -> 29

- `minecraft:loot_table`: elements 1447 -> 1447, tags 0 -> 0

- `minecraft:villager_trade`: elements 391 -> 471, tags 73 -> 73

```text
TOTAL static_registries=95 static_entries=7053 datapack_registries=155 with_elements=60 vanilla_json_elements=7383 vanilla_tag_files=883 structures=1511 functions=0 builtin_pack_files=106 assets_files=3 unmatched=['.mcassetsroot'] all_files=9887
CHECK vanilla_missing_refs=0 vanilla_cycles=0 trade_rebalance_missing_refs=0 trade_rebalance_cycles=0 builtin_unmatched=0
```

## Annexe : data_inventory.py

```python
"""Inventory of the 26.3 extracted data: registries, files per registry, tag resolution.

Usage: python -I data_inventory.py <reference dir containing generated/ and resources/>
Prints Markdown tables. Standard library only.
"""
import json
import os
import sys

REF = sys.argv[1]
REPORTS = os.path.join(REF, "generated", "reports")
RESOURCES = os.path.join(REF, "resources")
BUILTIN_DIR = os.path.join(RESOURCES, "data", "minecraft", "datapacks")


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


STATIC = load_json(os.path.join(REPORTS, "registries.json"))
DATAPACK = load_json(os.path.join(REPORTS, "datapack.json"))
EXTENSIONS = {"mcfunction": ".mcfunction", "structure": ".nbt"}

# folder (registry path) -> (accepts elements, accepts tags, element file extension)
FOLDERS = {}
for rid, prop in DATAPACK["registries"].items():
    ns, path = rid.split(":", 1)
    assert ns == "minecraft", rid
    FOLDERS[path] = (prop["elements"], prop["tags"], ".json")
for path, prop in DATAPACK["others"].items():
    FOLDERS[path] = (prop["elements"], prop["tags"], EXTENSIONS[prop["format"]])


def longest_folder(rel):
    best = None
    for folder in FOLDERS:
        if rel.startswith(folder + "/") and (best is None or len(folder) > len(best)):
            best = folder
    return best


def walk_files(root, skip=()):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames.sort()
        for name in sorted(filenames):
            rel = os.path.relpath(os.path.join(dirpath, name), root).replace(os.sep, "/")
            if not any(rel == s or rel.startswith(s + "/") for s in skip):
                yield rel


def scan_pack(pack_root, skip=()):
    """Map every file under <pack_root>/data to (folder, identifier)."""
    elements, tags, unmatched = {}, {}, []
    data = os.path.join(pack_root, "data")
    for rel in walk_files(data, skip):
        parts = rel.split("/", 1)
        if len(parts) < 2:
            unmatched.append(rel)
            continue
        namespace, inner = parts
        is_tag = inner.startswith("tags/")
        if is_tag:
            inner = inner[len("tags/"):]
        folder = longest_folder(inner)
        if folder is None:
            unmatched.append(rel)
            continue
        has_elements, has_tags, ext = FOLDERS[folder]
        if is_tag:
            ext = ".json"
        if not inner.endswith(ext) or (is_tag and not has_tags) or (not is_tag and not has_elements):
            unmatched.append(rel)
            continue
        ident = namespace + ":" + inner[len(folder) + 1:-len(ext)]
        if is_tag:
            tags.setdefault(folder, {})[ident] = load_json(os.path.join(data, rel))
        else:
            elements.setdefault(folder, {})[ident] = rel
    return elements, tags, unmatched


def merge(packs):
    """Stack packs given lowest priority first: whole-file override, tag append or replace."""
    elements, tags, overridden, replaced = {}, {}, [], []
    for pack_elements, pack_tags in packs:
        for folder, ids in pack_elements.items():
            target = elements.setdefault(folder, {})
            overridden += [(folder, i) for i in sorted(ids) if i in target]
            target.update(ids)
        for folder, files in pack_tags.items():
            target = tags.setdefault(folder, {})
            for ident, body in sorted(files.items()):
                if body.get("replace", False) and ident in target:
                    replaced.append((folder, ident))
                base = [] if body.get("replace", False) else target.get(ident, [])
                target[ident] = base + list(body["values"])
    return elements, tags, overridden, replaced


def entry_ref(entry):
    if isinstance(entry, str):
        return entry, True
    return entry["id"], entry.get("required", True)


def resolve_all(raw_tags, universe):
    """Resolve tags of one registry: {tag: [ids] or None if it fails}, missing refs, cycles."""
    resolved, missing, cycles = {}, [], []

    def visit(tag, stack):
        if tag in resolved:
            return resolved[tag]
        if tag in stack:
            cycles.append(stack[stack.index(tag):] + [tag])
            return None
        out, ok = [], True
        for entry in raw_tags[tag]:
            ref, required = entry_ref(entry)
            if ref.startswith("#"):
                known = ref[1:] in raw_tags
                values = visit(ref[1:], stack + [tag]) if known else None
                if not known and required:
                    missing.append((tag, ref))
            else:
                values = [ref] if ref in universe else None
                if values is None and required:
                    missing.append((tag, ref))
            if values is None:
                ok = ok and not required
                continue
            out += [v for v in values if v not in out]
        resolved[tag] = out if ok else None
        return resolved[tag]

    for tag in sorted(raw_tags):
        visit(tag, [])
    return resolved, missing, cycles


def universe_of(folder, elements):
    rid = "minecraft:" + folder
    if rid in STATIC:
        return set(STATIC[rid]["entries"])
    return set(elements.get(folder, {}))


def resolve_registries(elements, tags):
    return {f: resolve_all(tags[f], universe_of(f, elements)) for f in sorted(tags)}


def count_nested(ids):
    return sum(1 for i in ids if "/" in i.split(":", 1)[1])


# Vanilla pack: resources/data, without the built-in packs folder.
v_elem, v_tags, v_unmatched = scan_pack(RESOURCES, skip=("minecraft/datapacks",))
builtin = {}
for name in sorted(os.listdir(BUILTIN_DIR)):
    root = os.path.join(BUILTIN_DIR, name)
    meta = load_json(os.path.join(root, "pack.mcmeta"))
    e, t, u = scan_pack(root) if os.path.isdir(os.path.join(root, "data")) else ({}, {}, [])
    builtin[name] = (meta, e, t, u)

print("## Static registries (registries.json)\n")
print("| registry | protocol_id | entries | default | vanilla tag files |")
print("|---|---|---|---|---|")
for rid in sorted(STATIC):
    reg = STATIC[rid]
    folder = rid.split(":", 1)[1]
    print("| `%s` | %d | %d | %s | %d |" % (rid, reg["protocol_id"], len(reg["entries"]),
          "`%s`" % reg["default"] if "default" in reg else "-", len(v_tags.get(folder, {}))))

print("\n## Datapack registries with elements (datapack.json)\n")
print("| registry | folder | stable | vanilla files | in subfolders | vanilla tag files "
      "| trade_rebalance files | trade_rebalance tag files |")
print("|---|---|---|---|---|---|---|---|")
tr_elem, tr_tags = builtin["trade_rebalance"][1], builtin["trade_rebalance"][2]
for rid in sorted(DATAPACK["registries"]):
    prop = DATAPACK["registries"][rid]
    if not prop["elements"]:
        continue
    folder = rid.split(":", 1)[1]
    ids = v_elem.get(folder, {})
    print("| `%s` | `data/<ns>/%s/` | %s | %d | %d | %d | %d | %d |" % (
        rid, folder, str(prop["stable"]).lower(), len(ids), count_nested(ids),
        len(v_tags.get(folder, {})), len(tr_elem.get(folder, {})), len(tr_tags.get(folder, {}))))

print("\n## Other pack contents (datapack.json, section others)\n")
print("| kind | folder | format | tags | vanilla files | vanilla tag files |")
print("|---|---|---|---|---|---|")
for kind, prop in sorted(DATAPACK["others"].items()):
    print("| `%s` | `data/<ns>/%s/` | %s | %s | %d | %d |" % (
        kind, kind, prop["format"], str(prop["tags"]).lower(),
        len(v_elem.get(kind, {})), len(v_tags.get(kind, {}))))

vanilla_resolved = resolve_registries(v_elem, {f: {k: b["values"] for k, b in t.items()}
                                                for f, t in v_tags.items()})
print("\n## Vanilla tags per registry\n")
print("| registry | tag files | tag references | missing refs | cycles | empty tags | failed tags |")
print("|---|---|---|---|---|---|---|")
for folder, (res, missing, cycles) in vanilla_resolved.items():
    refs = sum(1 for b in v_tags[folder].values() for e in b["values"]
               if entry_ref(e)[0].startswith("#"))
    print("| `minecraft:%s` | %d | %d | %d | %d | %d | %d |" % (
        folder, len(res), refs, len(missing), len(cycles),
        sum(1 for r in res.values() if r == []), sum(1 for r in res.values() if r is None)))
print("\nEmpty vanilla tags: " + ", ".join(
    "`#%s` (%s)" % (t, f) for f, (res, _, _) in vanilla_resolved.items()
    for t, r in sorted(res.items()) if r == []))

VECTORS = [("block", "minecraft:logs"), ("block", "minecraft:wool"),
           ("block", "minecraft:mineable/pickaxe"), ("item", "minecraft:planks"),
           ("fluid", "minecraft:water"), ("fluid", "minecraft:lava"),
           ("worldgen/biome", "minecraft:is_overworld"), ("enchantment", "minecraft:non_treasure"),
           ("villager_trade", "minecraft:armorer/level_1"),
           ("villager_trade", "minecraft:librarian/level_1")]
tr_merged = merge([(v_elem, v_tags), (tr_elem, tr_tags)])
tr_resolved = resolve_registries(tr_merged[0], tr_merged[1])
print("\n## Tag resolution vectors (distinct entries after full expansion)\n")
print("| tag | registry | vanilla | vanilla + trade_rebalance |")
print("|---|---|---|---|")
for folder, tag in VECTORS:
    a = vanilla_resolved[folder][0].get(tag)
    b = tr_resolved[folder][0].get(tag)
    print("| `#%s` | `minecraft:%s` | %s | %s |" % (tag, folder, "absent" if a is None else len(a),
                                                   "absent" if b is None else len(b)))

print("\n## Built-in packs (resources/data/minecraft/datapacks)\n")
print("| pack | min_format | max_format | features.enabled | element files | tag files |")
print("|---|---|---|---|---|---|")
for name, (meta, e, t, u) in builtin.items():
    print("| `%s` | %s | %s | %s | %d | %d |" % (
        name, json.dumps(meta["pack"].get("min_format")), json.dumps(meta["pack"].get("max_format")),
        ", ".join("`%s`" % x for x in meta.get("features", {}).get("enabled", [])),
        sum(len(x) for x in e.values()), sum(len(x) for x in t.values())))

elements, tags, overridden, replaced = tr_merged
print("\n## Stack vanilla then trade_rebalance\n")
print("Overridden elements: " + ", ".join("`%s` (%s)" % (i, f) for f, i in overridden))
print("\nReplaced tags (replace true): " + ", ".join("`#%s`" % i for f, i in replaced))
new_tags = [(f, i) for f, ts in tr_tags.items() for i in sorted(ts) if i not in v_tags.get(f, {})]
print("\nNew tags: " + ", ".join("`#%s` (%s)" % (i, f) for f, i in new_tags))
for folder in sorted(set(tr_elem) | set(tr_tags)):
    print("\n- `minecraft:%s`: elements %d -> %d, tags %d -> %d" % (
        folder, len(v_elem.get(folder, {})), len(elements.get(folder, {})),
        len(v_tags.get(folder, {})), len(tags.get(folder, {}))))
tr_missing = sum(len(m) for _, m, _ in tr_resolved.values())
tr_cycles = sum(len(c) for _, _, c in tr_resolved.values())

n_elem = sum(len(x) for f, x in v_elem.items() if FOLDERS[f][2] == ".json")
n_struct = len(v_elem.get("structure", {}))
n_func = len(v_elem.get("function", {}))
n_tags = sum(len(x) for x in v_tags.values())
n_builtin = sum(1 + sum(len(x) for x in e.values()) + sum(len(x) for x in t.values()) + len(u)
                for _, e, t, u in builtin.values())
n_assets = sum(1 for _ in walk_files(os.path.join(RESOURCES, "assets")))
total = n_elem + n_struct + n_func + n_tags + len(v_unmatched) + n_builtin + n_assets
missing = sum(len(m) for _, m, _ in vanilla_resolved.values())
cycles = sum(len(c) for _, _, c in vanilla_resolved.values())
print("\nTOTAL static_registries=%d static_entries=%d datapack_registries=%d with_elements=%d "
      "vanilla_json_elements=%d vanilla_tag_files=%d structures=%d functions=%d "
      "builtin_pack_files=%d assets_files=%d unmatched=%s all_files=%d" % (
          len(STATIC), sum(len(r["entries"]) for r in STATIC.values()), len(DATAPACK["registries"]),
          sum(1 for p in DATAPACK["registries"].values() if p["elements"]), n_elem, n_tags,
          n_struct, n_func, n_builtin, n_assets, v_unmatched, total))
print("CHECK vanilla_missing_refs=%d vanilla_cycles=%d trade_rebalance_missing_refs=%d "
      "trade_rebalance_cycles=%d builtin_unmatched=%d" % (
          missing, cycles, tr_missing, tr_cycles, sum(len(u) for _, _, _, u in builtin.values())))
```

# Enchantements : définitions, effets, sélection et application

Statut : brouillon
Jalon : M6
Risque : A
Règles : R1 à R44 (R7, R9, R19 et R41 à R44 sont découpées en sous-règles citables : R7a, R7b, R19a...)

## Périmètre

Couvre, pour `engine/loot` (évaluation des valeurs d'enchantement, ADR 0004) et `engine/item` (composants) :

- le modèle de donnée d'un enchantement : coûts, poids, emplacements, objets admis, ensembles exclusifs, étiquettes de canaux (table, butin, échange, équipement de mob) ;
- le modèle d'**effets pilotés par données** : 31 composants d'effet, 6 effets de valeur, 6 valeurs dépendantes du niveau, 15 effets d'entité, 16 effets de position, prédicats d'activation ;
- les 43 enchantements vanilla : tableau de paramètres et valeurs par niveau (C) ;
- les fournisseurs d'enchantements (`enchantment_provider`, 7 fichiers) ;
- les procédures de sélection et d'application : table d'enchantement, enchantement par niveaux de butin, enchantement aléatoire, enclume, meule, réparation par expérience ;
- la forme des composants `enchantments` et `stored_enchantments`.

Ne couvre pas :

- l'exécution des effets d'entité et de position (dégâts réels, explosions, particules, sons, invocations) : jalon M7 et `ActionSink` (ADR 0004) ; ici seulement la forme, le déclenchement et le calcul de valeur ;
- le combat, l'attaque, la formule de dégâts complète (M7) ;
- les menus et clics de la table d'enchantement, de l'enclume et de la meule (`item-components-inventory.md` pour les slots) ;
- les échanges de livres (`villager_trade`).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) :
  - `resources/data/minecraft/enchantment/*.json` : 43 fichiers ; `enchantment_provider/**` : 7 ; `tags/enchantment/**` (22 fichiers dont 7 sous `exclusive_set/`) ; `tags/item/enchantable/**` (21 étiquettes) ;
  - `generated/reports/registries.json` : `enchantment_effect_component_type` (31), `enchantment_entity_effect_type` (15), `enchantment_location_based_effect_type` (16), `enchantment_value_effect_type` (6), `enchantment_level_based_value_type` (6), `enchantment_provider_type` (3), `context_key_set` (31, dont `enchanted_damage`, `enchanted_item`, `enchanted_location`, `enchanted_entity`, `hit_block`) ;
  - composant `enchantable` des prototypes d'objets (77 objets) ; composant `repair_cost` (1658 objets, valeur 0) ;
  - `generated/reports/commands.json` : commande `enchant`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Enchanting mechanics" (table d'enchantement), "Anvil mechanics", "Enchantment definition" (composants d'effet, valeurs dépendantes du niveau), "Item modifier" (fonctions `enchant_with_levels`, `enchant_randomly`), "Predicate".
- **Rapports locaux** (indices, ADR 0002) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, chapitres 137 à 139 (définitions, paramètres à ne pas confondre, fournisseurs). Il énumère les procédures à reproduire sans les décrire ; aucune règle S n'en vient.
- **Spec voisine** : `loot-tables.md` (contexte, conditions, fonctions `enchant_*`, `enchanted_count_increase`, `apply_bonus`).
- **Sondes de l'oracle** : aucune exécutée. Voir PE1 à PE12.

Reproduction : tous les dénombrements D et C de cette spec sortent du script `enchant_stats.py` de l'annexe, lancé avec `python -I enchant_stats.py "$REF"` (`REF` = dossier `.cache/reference/26.3`). Les sections de sa sortie portent le numéro de la règle qu'elles étayent (`== R5 ...`). L'empreinte SHA-256 de la sortie complète est épinglée en annexe. Aucun tableau de données de référence n'est recopié ici (AGENTS.md) : les tests lisent les fichiers extraits à l'exécution.

Convention de preuve : D valeur lue dans un fichier extrait ; C calcul reproductible sur ces fichiers (dénombrements ; les valeurs par niveau évaluées par une formule de la documentation ne sont pas C mais S, voir R21 et R22) ; M observé sur l'oracle (non utilisé) ; S documentation publique seule ou hypothèse. Chaque règle S porte un **comportement provisoire**.

## Règles

### Définition d'un enchantement

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le registre `enchantment` est chargeable (un fichier par enchantement) : 43 fichiers vanilla. Identifiant `minecraft:<nom>`. | D |
| R2 | Champs obligatoires de chaque fichier : `description` (composant de texte), `max_level` (entier), `weight` (entier), `anvil_cost` (entier), `min_cost` et `max_cost` (`{base, per_level_above_first}`), `slots` (liste de groupes d'emplacements), `supported_items` (étiquette d'objets). Champs optionnels : `primary_items` (étiquette, 5 enchantements), `exclusive_set` (étiquette ou identifiant ou liste, 18 enchantements), `effects` (table composant -> valeur, 42 enchantements : `fortune` n'en a pas). | D |
| R3 | Répartition des paramètres (C) : `max_level` 1 (9 enchantements), 2 (4), 3 (16), 4 (7), 5 (7) ; `weight` 1 (8), 2 (19), 5 (11), 10 (5) ; `anvil_cost` 1 (5), 2 (11), 4 (19), 8 (8) ; groupes de `slots` : `mainhand` 25, `armor` 6, `any` 4, `feet` 3, `head` 2, `hand` 2, `offhand` 1, `legs` 1 (un enchantement peut en lister plusieurs : `quick_charge` = `mainhand` et `offhand`). Le tableau complet se régénère (R4). Commande : section `R1-R3` de `enchant_stats.py`. | C |
| R4 | Les 43 définitions portent les colonnes `max_level`, `weight`, `anvil_cost`, `min_cost`, `max_cost`, `slots`, `supported_items`, `primary_items`, `exclusive_set` (R2, R3, R7, R8). Le tableau des 43 lignes n'est pas recopié dans le dépôt (AGENTS.md : aucune donnée de référence dans git) ; la section `R4` de `enchant_stats.py` le régénère (une ligne par enchantement, 43 lignes) et le test relit les 43 fichiers à l'exécution pour comparer chaque définition décodée à sa ligne. Quatre exemples (D, forme `max_level / weight / anvil_cost / min_cost / max_cost / slots / supported_items / primary_items / exclusive_set`) : `sharpness` 5 / 10 / 1 / 1+11 par niveau / 21+11 / `mainhand` / `#minecraft:enchantable/sharp_weapon` / `#minecraft:enchantable/melee_weapon` / `#minecraft:exclusive_set/damage` ; `mending` 1 / 2 / 4 / 25+25 / 75+25 / `any` / `#minecraft:enchantable/durability` / aucun / aucun ; `quick_charge` 3 / 5 / 2 / 12+20 / 50+0 / `mainhand` et `offhand` / `#minecraft:enchantable/crossbow` / aucun / aucun ; `fortune` 3 / 2 / 4 / 15+9 / 65+9 / `mainhand` / `#minecraft:enchantable/mining_loot` / aucun / `#minecraft:exclusive_set/mining` (seul enchantement sans `effects`). | D |

| Réf | Règle | Preuve |
|---|---|---|
| R5 | Coût d'un enchantement au niveau L (pour la table d'enchantement) : `min_cost(L)` = `base + per_level_above_first x (L - 1)`, de même pour `max_cost(L)` (même forme que la valeur dépendante du niveau `linear`, R21). Les paramètres `base` et `per_level_above_first` sont lus dans les 43 fichiers (D). Exemples évalués avec la formule (section `R5` du script) : `sharpness` min 1, 12, 23, 34, 45 et max 21, 32, 43, 54, 65 pour L = 1 à 5 ; `fortune` min 15, 24, 33 et max 65, 74, 83 ; `efficiency` min 1, 11, 21, 31, 41 et max 51, 61, 71, 81, 91 ; `mending` (niveau 1) min 25 et max 75. | D (paramètres) ; S (formule R21, usage R37) |
| R6 | `slots` : groupes d'emplacements où l'enchantement est actif quand l'objet y est porté ou tenu : `mainhand`, `offhand`, `hand` (main principale ou secondaire), `head`, `chest`, `legs`, `feet`, `armor` (les quatre pièces), `any` (tout emplacement), `body` (documentation). L'enchantement d'un objet hors de ses emplacements ne produit pas d'effet. Provisoire. | D (valeurs vues) ; S (sens) |
| R7a | `supported_items` : objets sur lesquels l'enchantement peut figurer (commandes, enclume, butin). `primary_items` : champ optionnel, présent sur 5 enchantements (`bane_of_arthropods`, `fire_aspect`, `sharpness`, `smite`, `thorns`), différent de `supported_items` sur ces 5. Exemple : `sharpness` supporte `#minecraft:enchantable/sharp_weapon` mais a pour primaires `#minecraft:enchantable/melee_weapon` ; `smite` supporte `#minecraft:enchantable/weapon`. Les étiquettes `enchantable/*` sont 21 (armor, bow, chest_armor, crossbow, durability, equippable, fire_aspect, fishing, foot_armor, head_armor, leg_armor, lunge, mace, melee_weapon, mining, mining_loot, sharp_weapon, sweeping, trident, vanishing, weapon). Commande : section `R7`. | D |
| R7b | Usage de `primary_items` : sous-ensemble tiré par la table d'enchantement (R38) ; absent signifie égal à `supported_items` (38 enchantements). Provisoire. Sonde PE3 (table, objets de plusieurs types). | S |
| R8 | `exclusive_set` : l'enchantement est incompatible avec tout autre enchantement de l'ensemble (lui-même exclu). Ensembles vanilla : `armor` (protection, blast_protection, fire_protection, projectile_protection), `boots` (frost_walker, depth_strider), `bow` (infinity, mending), `crossbow` (multishot, piercing), `damage` (sharpness, smite, bane_of_arthropods, impaling, density, breach), `mining` (fortune, silk_touch), `riptide` (loyalty, channeling ; référencé par `riptide`). Les 7 ensembles sont dans `tags/enchantment/exclusive_set/`. Observation : `riptide` a pour ensemble exclusif `#minecraft:exclusive_set/riptide` qui ne contient pas `riptide` mais ses deux incompatibles ; `loyalty` et `channeling` ne déclarent aucun `exclusive_set` ; de même `mending` pour l'ensemble `bow`. La relation n'est donc déclarée que d'un côté dans ces cas (D). Provisoire : deux enchantements sont incompatibles si l'un des deux cite l'autre dans son ensemble exclusif (relation symétrique). Sonde PE6. | D (ensembles, asymétrie des déclarations) ; S (symétrie) |
| R9a | Étiquettes d'enchantements (D, contenu ; section `R9`) : `non_treasure` (36 enchantements), `treasure` (7 : `binding_curse`, `vanishing_curse`, `swift_sneak`, `soul_speed`, `frost_walker`, `mending`, `wind_burst`), `curse` (2), `in_enchanting_table` (= `#non_treasure`), `on_random_loot` (`#non_treasure`, les deux malédictions, `frost_walker`, `mending`), `tradeable` (idem `on_random_loot`), `on_mob_spawn_equipment` et `on_traded_equipment` (`#non_treasure`), `double_trade_price` (`#treasure`), `smelts_loot` (`fire_aspect`), `prevents_bee_spawns_when_mining`, `prevents_decorated_pot_shattering`, `prevents_ice_melting`, `prevents_infested_spawns` (chacune = `silk_touch`), `tooltip_order` (43 enchantements dans l'ordre d'affichage). | D |
| R9b | Chaque canal de sélection (table, butin, échange, équipement de mob) lit sa propre étiquette ; une liste globale unique serait une erreur. Provisoire. Sondes PE3 (table), PL13 (butin), PE12 (équipement de mob) ; l'échange est hors périmètre. | S |
| R10 | Les composants `enchantments` (objet enchanté) et `stored_enchantments` (livre enchanté) sont des tables identifiant -> niveau entier, valides de 1 à 255 ; défaut `{}` (D, tous les prototypes). L'argument `level` de la commande `enchant` a pour minimum 0 (D `commands.json`). Un niveau supérieur à `max_level` est permis par les données (`give`, modificateur), mais pas par `/enchant` (provisoire). Un niveau 0 ou négatif retire l'entrée (modificateur) ou est refusé (S). Sonde PE7. | D (défaut) ; S (plage) |

### Composants d'effet

| Réf | Règle | Preuve |
|---|---|---|
| R11 | Le champ `effects` est une table composant d'effet -> valeur. 31 composants sont enregistrés (`enchantment_effect_component_type`) ; 30 sont utilisés par les 43 enchantements, `mob_experience` ne l'est pas. Nombre d'enchantements qui portent chaque composant : voir le tableau suivant (section `R11`). | D |

| Composant d'effet | Enchantements |
|---|---|
| `ammo_use` | 1 |
| `armor_effectiveness` | 1 |
| `attributes` | 8 |
| `block_experience` | 1 |
| `crossbow_charge_time` | 1 |
| `crossbow_charging_sounds` | 1 |
| `damage` | 5 |
| `damage_immunity` | 1 |
| `damage_protection` | 5 |
| `equipment_drops` | 1 |
| `fishing_luck_bonus` | 1 |
| `fishing_time_reduction` | 1 |
| `hit_block` | 1 |
| `item_damage` | 1 |
| `knockback` | 2 |
| `location_changed` | 2 |
| `mob_experience` | 0 |
| `post_attack` | 5 |
| `post_piercing_attack` | 1 |
| `prevent_armor_change` | 1 |
| `prevent_equipment_drop` | 1 |
| `projectile_count` | 1 |
| `projectile_piercing` | 1 |
| `projectile_spawned` | 1 |
| `projectile_spread` | 1 |
| `repair_with_xp` | 1 |
| `smash_damage_per_fallen_block` | 1 |
| `tick` | 1 |
| `trident_return_acceleration` | 1 |
| `trident_sound` | 1 |
| `trident_spin_attack_strength` | 1 |

| Réf | Règle | Preuve |
|---|---|---|
| R12 | Trois formes de valeur de composant : (a) liste d'entrées `{effect, requirements?, enchanted?, affected?}` (ex. `damage`, `damage_protection`, `knockback`, `post_attack`, `tick`, `location_changed`, `hit_block`, `item_damage`, `ammo_use`, `equipment_drops`, `block_experience`...) ; (b) valeur directe (effet de valeur sans liste : `crossbow_charge_time`, `trident_spin_attack_strength`, qui s'appliquent toujours) ; (c) autres formes : `{}` pour les indicateurs `prevent_armor_change`, `prevent_equipment_drop` ; liste d'identifiants de sons pour `trident_sound` ; liste de `{start, end}` pour `crossbow_charging_sounds` (entrées vues avec `start` et `end`) ; liste d'attributs `{id, attribute, operation, amount}` pour `attributes`. | D |
| R13 | Une entrée `{effect, requirements}` s'applique si ses `requirements` (condition de butin, `loot-tables.md` R29 à R37) sont vraies dans le contexte de l'effet (R15) ; `requirements` absent : toujours. Plusieurs entrées d'un même composant s'appliquent dans l'ordre de la liste, chacune sur le résultat de la précédente. Provisoire. | S |
| R14 | `enchanted` (qui doit porter l'enchantement : `attacker`, `victim`, `this`) et `affected` (qui subit l'effet : `attacker`, `victim`, `damaging_entity`) précisent l'entité concernée des composants à effet d'entité (`post_attack`) et de `equipment_drops` (`enchanted`). Valeurs vues : `affected` `victim`, `attacker` ; `enchanted` `attacker`, `victim`. | D (valeurs) ; S (sens) |
| R15 | Contextes d'évaluation des conditions (registre `context_key_set`) : `enchanted_damage` (dégâts : `damage_protection`, `damage`, `armor_effectiveness`, `knockback`, `damage_immunity`, `post_attack`), `enchanted_item` (outil, munition : `ammo_use`, `block_experience`, `item_damage`), `enchanted_entity` (projectile, mob tué : `projectile_count`, `projectile_spread`, `mob_experience`), `enchanted_location` (`location_changed`), `hit_block` (`hit_block`). Contenu exact des jeux : S (voir `loot-tables.md` R13). Une condition lit les paramètres communs plus `enchantment_level` et `enchantment_active`. | D (noms) ; S (association) |
| R16 | Déclenchements (documentation, S) : `tick` à chaque tick de l'entité porteuse ; `location_changed` quand la position en blocs change, quand l'objet est équipé, et au chargement de l'entité (condition `enchantment_active_check`) ; `hit_block` quand l'entité (ou le projectile) touche un bloc avec l'objet ; `projectile_spawned` quand un projectile est créé par le lanceur ; `post_attack` après des dégâts d'attaque ; `post_piercing_attack` après toute attaque perforante, touchée ou non ; `damage_immunity` donne l'immunité aux dégâts quand le prédicat réussit. | S |

### Effets de valeur et valeurs dépendantes du niveau

| Réf | Règle | Preuve |
|---|---|---|
| R17 | Effets de valeur (registre `enchantment_value_effect_type`, 6) : `add` {`value`}, `set` {`value`}, `multiply` {`factor`}, `remove_binomial` {`chance`}, `all_of` {`effects`}, `exponential` {`factor`} (documentation incertaine). Usage vanilla mesuré sur les composants à valeur numérique, `all_of` développé (section `R17`) : `add` 21, `set` 2, `multiply` 1, `remove_binomial` 2, `all_of` 0, `exponential` 0, plus une entrée dont `effect` est l'objet vide (`damage_immunity`). Les `all_of` vanilla sont tous des effets d'entité (R26). `all_of` exécute ses sous-effets en série. | D (usage) ; S (sens) |
| R18 | Sémantique des effets de valeur (S) : `add` ajoute `value(niveau)` à la valeur courante ; `set` la remplace ; `multiply` la multiplie par `factor(niveau)` ; `remove_binomial` fait un essai de probabilité `chance(niveau)` et retire 1 en cas de succès ; la valeur de départ est celle du composant (R19). Les arrondis et clamps sont ceux du composant consommateur (S). | S |
| R19a | Valeur de départ de `damage_protection` : 0 ; la protection vaut min(somme, 20) x 4 pour cent de réduction (plafond 80 pour cent). Dépend du consommateur (jalon M7). | S |
| R19b | Valeurs de départ de `damage` (dégâts ajoutés), `knockback` : 0 ; `armor_effectiveness` : 0, lue comme une efficacité (0 inefficace, 1 pleine ; `breach` ajoute -0.15 x niveau). | S |
| R19c | Valeurs de départ de `item_damage` : la perte de durabilité demandée (entier) ; `ammo_use` : 1 ; `repair_with_xp` : 1. | S |
| R19d | Valeurs de départ de `projectile_count` : 1 ; `projectile_piercing` : 0. | S |
| R19e | Valeurs de départ de `fishing_luck_bonus` : 0 ; `block_experience` : l'expérience du bloc ; `smash_damage_per_fallen_block` : 0. | S |
| R20 | Valeurs dépendantes du niveau (registre `enchantment_level_based_value_type`, 6) : `linear` {`base`, `per_level_above_first`} (43 usages), `levels_squared` {`added`} (1), `fraction` {`numerator`, `denominator`} (3), `clamped` {`value`, `min`, `max`} (1), `lookup` {`values`, `fallback`} (1), `exponent` {`base`, `power`} (0). Une constante nue est permise. Les coûts `min_cost` et `max_cost` ont la forme de `linear` sans le champ `type`. | D |
| R21 | Évaluation au niveau L (entier de 1 à 255), documentation, S : `linear` = base + per_level_above_first x (L - 1) ; `levels_squared` = L x L + added ; `fraction` = numerator(L) / denominator(L) (division flottante) ; `clamped` = valeur bornée à [min, max] ; `lookup` = values[L - 1] si L - 1 < taille, sinon fallback(L) ; `exponent` = base(L) puissance power(L). Les nombres sont des Float 32 bits (les constantes des données sont stockées en Float, par exemple 0.33333334) ; provisoire : les sommes et produits se font en Float. | S |
| R22 | Valeurs par niveau des effets des 43 enchantements. Les paramètres (`base`, `per_level_above_first`, `added`, `numerator`, `values`...) sont D ; les valeurs évaluées pour L = 1 à `max_level` sont obtenues avec les formules de R21 et restent donc de niveau S tant que PE10 ne les a pas confirmées. La section `R22` de `enchant_stats.py` imprime une ligne par (enchantement, composant, effet) avec les valeurs séparées par `;` (double puis 6 chiffres significatifs) : 36 lignes de valeurs, plus la ligne `lookup` de `wind_burst` et la ligne de `thorns`. Elle n'est pas recopiée dans le dépôt. Quatre exemples évalués sous R21 : `sharpness` `damage` `add` 1 ; 1.5 ; 2 ; 2.5 ; 3 ; `protection` `damage_protection` `add` 1 ; 2 ; 3 ; 4 ; `efficiency` attribut `mining_efficiency` 2 ; 5 ; 10 ; 17 ; 26 ; `aqua_affinity` attribut `submerged_mining_speed` (`add_multiplied_total`) 4. | D (paramètres) ; S (valeurs évaluées) |

| Réf | Règle | Preuve |
|---|---|---|
| R23 | Cas de la durabilité (paramètres D ; probabilités effectives sous R18 et R21, donc S) : la probabilité qu'un point de dégâts d'objet soit annulé par `unbreaking` vaut, pour une armure (objet de `#minecraft:enchantable/armor`), (2 + 2(L-1)) / (10 + 5(L-1)) = 0.2, 0.2667, 0.3 pour L = 1 à 3, et pour les autres objets L / (L + 1) = 0.5, 0.6667, 0.75. Les deux entrées sont exclusives (la seconde est la négation de la première). Commande : section `R22` (lignes `unbreaking`). Sonde PE5. | D (paramètres) ; S (probabilités) |
| R24 | Cas de `efficiency`, `sweeping_edge` et `thorns` (paramètres D ; valeurs évaluées sous R21, donc S). `efficiency` : attribut `mining_efficiency`, opération `add_value`, `levels_squared` avec `added` 1.0 : bonus de vitesse 2, 5, 10, 17, 26 pour L = 1 à 5. `sweeping_edge` : attribut `sweeping_damage_ratio`, `fraction` L / (L + 1) : 0.5, 0.6667, 0.75. `thorns` : `random_chance` 0.15 x L, dégâts 1 à 5 à l'attaquant (effet `damage_entity` type `minecraft:thorns`), perte de durabilité 2 (ligne `thorns` de la section `R22`). | D (paramètres) ; S (valeurs) |
| R25 | Les `min_cost` et `max_cost` suivent la forme `linear` (R20) ; les valeurs de `anvil_cost` (1, 2, 4, 8) sont les multiplicateurs de coût d'enclume de l'enchantement avant le facteur "livre" (R43). | D / S |

### Effets d'entité et de position

| Réf | Règle | Preuve |
|---|---|---|
| R26 | Effets d'entité (registre `enchantment_entity_effect_type`, 15) : `all_of`, `apply_exhaustion`, `apply_impulse`, `apply_mob_effect`, `change_item_damage`, `damage_entity`, `explode`, `ignite`, `play_sound`, `replace_block`, `replace_disk`, `run_function`, `set_block_properties`, `spawn_particles`, `summon_entity`. Usage vanilla mesuré sous les composants à effet d'entité et de position (section `R17`, `all_of` développé) : `all_of` 5, `play_sound` 4, `change_item_damage` 3, `summon_entity` 2, `ignite` 2, `apply_mob_effect`, `replace_disk`, `apply_exhaustion`, `apply_impulse`, `spawn_particles`, `damage_entity`, `explode` 1 chacun ; `replace_block`, `run_function`, `set_block_properties` : 0. | D |
| R27 | Effets de position (registre `enchantment_location_based_effect_type`, 16) : les 15 effets d'entité plus `attribute` {`id`, `attribute`, `operation`, `amount`} (2 usages, imbriqués dans des `all_of` : `soul_speed`). Un effet `attribute` est un modificateur d'attribut dont l'identifiant est unique par enchantement et qui est retiré quand l'enchantement cesse d'être actif. | D / S |
| R28 | Champs observés des effets d'entité et de position (D) : `ignite` {`duration` valeur ou constante} ; `play_sound` {`sound` (identifiant ou liste), `volume`, `pitch` (nombre ou `uniform` {`min_inclusive`, `max_exclusive`})} ; `change_item_damage` {`amount`} ; `apply_exhaustion` {`amount`} ; `apply_impulse` {`direction`, `coordinate_scale`, `magnitude`} ; `apply_mob_effect` {`to_apply`, `min_duration`, `max_duration`, `min_amplifier`, `max_amplifier`} ; `damage_entity` {`damage_type`, `min_damage`, `max_damage`} ; `summon_entity` {`entity`} ; `explode` {`radius`, `block_interaction`, `immune_blocks`, `knockback_multiplier`, `large_particle`, `small_particle`, `sound`} ; `replace_disk` {`block_state`, `radius`, `height`, `offset`, `predicate` (prédicat de bloc), `trigger_game_event`} ; `spawn_particles` {`particle`, `horizontal_position`, `vertical_position`, `horizontal_velocity`, `vertical_velocity`, `speed`}. | D |
| R29 | Prédicat de bloc (registre `block_predicate_type`, 16) utilisé par `replace_disk` : vu `all_of` avec `matching_block_tag` (avec `offset`), `matching_blocks`, `matching_fluids`, `unobstructed`. Les autres types (`any_of`, `not`, `true`, `solid`, `replaceable`, `would_survive`, `has_sturdy_face`, `inside_world_bounds`, `height_range`, `volume_match`, `matching_biomes`) sont enregistrés. Sémantique : S, jalon M5. | D / S |
| R30 | Prédicats d'entité utilisés dans les `requirements` des enchantements : voir `loot-tables.md` R57. Clés mesurées dans les enchantements (section `R29-R30`) : `minecraft:entity_type` 9, `minecraft:flags` 10 (`is_flying`, `is_on_ground`), `minecraft:movement_affected_by` 5 (`block`), `minecraft:movement` 3 (`horizontal_speed`, `fall_distance`), `minecraft:vehicle` 3, `minecraft:type_specific/player` 3, `minecraft:periodic_tick` 2 (entier), `minecraft:location` 1. `minecraft:stepping_on`, `minecraft:distance` et `minecraft:equipment` n'y sont pas (ils n'apparaissent que sous `advancement/`). | D |

### Évaluation d'ensemble

| Réf | Règle | Preuve |
|---|---|---|
| R31 | Pour calculer un composant d'effet d'une entité (par exemple la protection contre un dégât), on parcourt les objets de ses emplacements d'équipement ; pour chaque objet on considère uniquement les enchantements dont `slots` contient l'emplacement de l'objet ; pour chacun, on évalue les entrées de la liste du composant au niveau de l'enchantement sur cet objet. Les contributions s'accumulent dans la valeur courante. L'ordre de parcours des emplacements et des enchantements dans un objet (ordre d'insertion de la table `enchantments`) est S. Sondes PE5, PE8. | S |
| R32 | `damage_protection` : la somme des protections de toutes les pièces est limitée à 20 points ; chaque point retire 4 pour cent des dégâts restants avant ce calcul ; l'ordre par rapport à l'armure, la résistance et l'absorption est celui de la formule de dégâts (M7). Provisoire. Sonde PE5. | S |
| R33 | Les attributs (`attributes`) d'un enchantement sont des modificateurs d'attribut persistants : identifiants `minecraft:enchantment.<nom>` (vus : `aqua_affinity`, `efficiency`, `depth_strider`, `respiration`, `sweeping_edge`, `swift_sneak`, `blast_protection`, `fire_protection`, `soul_speed`...). Ils sont appliqués quand l'objet est dans un emplacement actif et retirés sinon. Provisoire. | D (identifiants) ; S (cycle de vie) |
| R34 | Fournisseurs d'enchantements (registre `enchantment_provider`, 7 fichiers ; types `single`, `by_cost`, `by_cost_with_difficulty`) : `single` {`enchantment`, `level`} (6 fichiers : `enderman_loot_drop` silk_touch 1, `pillager_spawn_crossbow` piercing 1, `raid/pillager_post_wave_3` quick_charge 1, `raid/pillager_post_wave_5` quick_charge 2, `raid/vindicator` sharpness 1, `raid/vindicator_post_wave_5` sharpness 2) ; `by_cost_with_difficulty` {`enchantments` (étiquette), `min_cost`, `max_cost_span`} (`mob_spawn_equipment` : étiquette `#minecraft:on_mob_spawn_equipment`, `min_cost` 5, `max_cost_span` 17). `by_cost` : même forme sans difficulté (non utilisé). Provisoire : le coût est tiré uniformément dans [min_cost, min_cost + max_cost_span] (le coût dépend de la difficulté pour `by_cost_with_difficulty`) puis passé à la sélection de R38 et R39. | D (formes, valeurs) ; S (tirage) |

### Application par le butin

| Réf | Règle | Preuve |
|---|---|---|
| R35 | `enchant_randomly` (loot-tables R45) : choisit un enchantement parmi les `options` compatibles avec l'objet (`only_compatible` défaut true ; un livre est toujours compatible) à probabilité égale (provisoire : indépendamment du poids), puis un niveau de 1 à `max_level` à probabilité égale (provisoire). Un livre devient `enchanted_book` avec `stored_enchantments`. Quand plusieurs enchantements sont déjà présents, l'incompatibilité de R8 s'applique. Sondes PL13, PE2. | S |
| R36 | `enchant_with_levels` (loot-tables R45) : applique la procédure de la table d'enchantement (R37 à R40) avec le coût `levels` à la place du coût de ligne ; `options` absent : tous les enchantements ; l'enchantabilité de l'objet vient du composant `enchantable.value` (D, 77 objets) ; un livre a l'enchantabilité 1 (documentation) et devient `enchanted_book`. Sonde PE1. | S |

### Propriétaires de modules (décision de conception)

Décision prise pour cette spec (ADR 0004, sans niveau de preuve : elle ne décrit pas le jeu de référence). Les définitions d'enchantement, l'évaluation de leurs valeurs, la sélection de R35 à R39 et les calculs de coût de R41 à R44 sont dans `loot`, qui possède aussi l'aléa de tirage. Les menus de la table d'enchantement, de l'enclume et de la meule (slots et clics : `item-components-inventory.md` R37c, R40a à R47b, propriétaire des algorithmes de clic `item`) sont orchestrés par `game`, qui appelle `loot` pour les coûts et les offres et applique le résultat aux slots. `item` ne dépend pas de `loot`.

Métrique commune de R37 à R44 : chaque sous-règle est testée par des vecteurs (entrées, graine ou état) vers (offres, objet de sortie, coût en niveaux) comparés à l'oracle ; l'égalité est exacte pour un coût ou un objet de sortie à graine fixée (PE9, PE2, PE4), et statistique (khi-deux, voir Plan de tests) pour une distribution (PE1, PE3).

### Table d'enchantement (documentation, S)

| Réf | Règle | Preuve |
|---|---|---|
| R37 | Etagères : au plus 15 comptent, placées à distance exacte 2 de la table sur un axe horizontal et de 0 à 2 sur l'autre, à la hauteur de la table ou à celle du dessus ; le chemin entre l'étagère et la table doit être libre (à la hauteur de l'étagère). Les blocs de l'étiquette `enchantment_power_provider` peuvent remplacer des étagères. Pour un nombre b d'étagères (0 à 15), `xp_base` = 1 + R(7) + floor(min(15, b) / 2) + R(min(15, b)), où R(n) est un entier uniforme dans [0, n] ; coût de la ligne du haut = max(1, floor(xp_base / 3)), du milieu = floor(2 x xp_base / 3) + 1, du bas = max(xp_base, 2b). Les lignes se tirent avec la graine `XpSeed` du joueur (R40). | S |
| R38 | Pour une ligne de coût c et un objet d'enchantabilité e : k = floor(e / 4) ; coût effectif `eCost` = round((c + 1 + R(k) + R(k)) x (1 + T)) où T est triangulaire dans [-0.15, 0.15[ (T = (nextFloat + nextFloat - 1) x 0.15). Candidats : enchantements de l'étiquette `in_enchanting_table` dont l'objet est dans `primary_items` (un livre est primaire pour tous) ; pour chaque candidat on retient le plus haut niveau L dont `min_cost(L) <= eCost <= max_cost(L)` ; sans niveau valide le candidat est retiré. | S |
| R39 | Sélection : tirer un candidat avec la probabilité poids / somme des poids ; l'ajouter ; retirer les candidats incompatibles (R8) ; si plus de candidats, arrêter ; sinon continuer avec la probabilité (eCost + 1) / 50 (valeur à confirmer par PE1 : le texte de la page publique est illisible sur ce point) ; en cas de continuation, eCost = floor(eCost / 2) et recommencer au tirage pondéré. Pour un livre issu de la table avec plus d'un enchantement, retirer un enchantement au hasard. Le coût en niveaux d'expérience du joueur et le nombre de lapis-lazuli (1, 2, 3 selon la ligne) sont S. | S |
| R40 | La table d'enchantement n'accepte que des objets avec `enchantable` et sans enchantement ; `XpSeed` (graine du joueur, 0 à la création d'un monde) est régénérée quand l'objet est enchanté ; à graine égale, mêmes offres. Un `XpSeed` nul est régénéré à la connexion. | S |

### Enclume, meule, réparation par expérience (documentation, S)

| Réf | Règle | Preuve |
|---|---|---|
| R41a | Le composant `repair_cost` (D, défaut 0, entier ; valeur 0 pour les 1658 prototypes) porte la pénalité de travail précédent. | D |
| R41b | Chaque opération (sauf renommage seul) donne à l'objet de sortie la valeur 2 x max(pénalités des deux entrées) + 1 (suite 0, 1, 3, 7, 15, 31...). | S |
| R41c | Coût total en niveaux = pénalités des deux entrées + coût de renommage (1 si renommé) + coût de réparation (2 si l'objet cible n'est pas intact) + coût d'enchantement. | S |
| R41d | Refus si le total dépasse 39 en survie ("Too Expensive"). | S |
| R42a | Réparation à l'enclume avec une matière : une unité restaure 25 pour cent de `max_damage` (arrondi vers le bas) pour 1 niveau (plus pénalités). | S |
| R42b | Fusion de deux objets à l'enclume : ajoute les durabilités restantes plus 12 pour cent de `max_damage`, plafonné. | S |
| R42c | Le nom d'un objet renommé est limité à 50 caractères (Java). | S |
| R43a | Fusion d'enchantements à l'enclume : pour chaque enchantement de la pile sacrifiée absent de la cible, ajout au niveau complet sauf incompatibilité avec un enchantement de la cible (l'enchantement incompatible est perdu et coûte 1 niveau). | S |
| R43b | Si la cible a déjà l'enchantement : niveau supérieur de la sacrifiée -> prend ce niveau ; niveau égal -> +1 jusqu'à `max_level` ; niveau inférieur -> rien. | S |
| R43c | Les enchantements non supportés par la cible (R7a) sont ignorés. | S |
| R43d | Coût par enchantement : niveau résultant x multiplicateur, avec multiplicateur = `anvil_cost` si la source est un objet et max(1, floor(`anvil_cost` / 2)) si c'est un livre (provisoire). `anvil_cost` est D (1, 2, 4 ou 8). Sonde PE2. | D (anvil_cost) ; S (formule) |
| R43e | Contrôle de cohérence avec la documentation publique : les paires objet/livre qu'elle cite (Sharpness 1/1, Efficiency 1/1, Unbreaking 2/1, Smite 2/1, Looting 4/2, Mending 4/2, Soul Speed 8/4) sont compatibles avec R43d et avec les `anvil_cost` des données (section `R24-R25`). | D (anvil_cost) ; S (formule) |
| R44a | Meule : retire les enchantements non maudits d'un objet (conserve `binding_curse` et `vanishing_curse`, étiquette `curse`) et rend de l'expérience selon les niveaux et les coûts ; elle supprime la pénalité de travail et conserve le nom. Formule de l'expérience : S. Sonde PE4. | S |
| R44b | Réparation par expérience (`repair_with_xp`, facteur `multiply` 2.0 pour `mending`) : chaque point d'expérience ramassé répare `2 x facteur` points de durabilité sur un objet enchanté de l'emplacement actif. Formule exacte : S. Sonde PE4. | S |

## Entrées / sorties

- **Définition** : fichier JSON décodé (R2) ; sortie = coût(niveau), poids, ensembles exclusifs, effets typés par composant.
- **Évaluation d'un composant** : entrée = (entité, contexte d'effet, composant) ; sortie = valeur flottante finale ou liste d'intentions d'action pour l'`ActionSink`.
- **Sélection** : entrée = (objet, coût, étiquette de candidats, source d'aléa) ; sortie = table enchantement -> niveau.
- **Application** : entrée = pile + table ; sortie = pile (composant `enchantments` ou `stored_enchantments`).

## Cas limites

- Niveau 0, négatif, supérieur à `max_level` ou à 255 ; enchantement inconnu dans `enchantments` ; objet non supporté.
- Livre contre objet normal : candidats, enchantabilité 1, un livre est primaire pour tous.
- `lookup` au-delà de la liste des valeurs (`wind_burst` : 1.2, 1.75, 2.2 puis `fallback` linéaire).
- Deux enchantements incompatibles présents dans les données (via `give`) : conservés, les effets s'appliquent tous deux (S).
- Protection totale supérieure à 20 points ; `fire_protection` et `blast_protection` cumulées.
- Enchantement porté dans un emplacement non actif (ex. épée dans l'inventaire) : aucun effet.
- Condition de `requirements` qui lit un paramètre absent du contexte (échec, `loot-tables.md` R15).
- `unbreaking` sur un objet de durabilité 1.
- Enchantement `mending` et `infinity` ensemble (exclusion de `#exclusive_set/bow` : permis par `give`).

## Vecteurs de test

| Vecteur | Entrée | Sortie attendue | Preuve |
|---|---|---|---|
| V-E1 | Dénombrement du dossier `enchantment/` | 43 fichiers, répartitions de R3 | C |
| V-E2 | `sharpness` : `min_cost`, `max_cost` | `{base 1, per_level_above_first 11}` et `{base 21, per_level_above_first 11}` | D |
| V-E3 | `sharpness`, composant `damage`, effet `add` `linear` | base 1.0, `per_level_above_first` 0.5 | D |
| V-E4 | `protection` : `damage_protection` `add` `linear`, condition `damage_source_properties` : étiquette `#minecraft:bypasses_invulnerability` fausse | base 1.0, `per_level_above_first` 1.0 | D |
| V-E7 | `wind_burst` `knockback_multiplier` | `lookup` de `values` [1.2, 1.75, 2.2] avec `fallback` `linear` base 1.5, `per_level_above_first` 0.35 | D |
| V-E8 | Fournisseur `mob_spawn_equipment` | type `by_cost_with_difficulty`, étiquette `on_mob_spawn_equipment`, `min_cost` 5, `max_cost_span` 17 | D |
| V-E9 | Ensemble `exclusive_set/damage` | 6 membres (R8) | D |
| V-E10 | Étiquettes | `treasure` 7, `non_treasure` 36, `in_enchanting_table` = `#non_treasure` | D |

Vecteurs de comportement (S, via sondes) :

| Vecteur | Entrée | Sortie attendue (provisoire) |
|---|---|---|
| V-E11 | Table d'enchantement, 15 étagères, `XpSeed` fixé, épée en diamant | trois offres avec coûts dans [1, 30] et lignes du haut, du milieu et du bas selon R37 (distribution, PE3) |
| V-E12 | `enchant_with_levels` niveau 30 sur un livre, N = 100000 | support = enchantements non-trésor avec `min_cost(L) <= eCost <= max_cost(L)`, fréquence des tailles de 1 à 4 enchantements (PE1) |
| V-E13 | Enclume : épée Sharpness III + épée Sharpness III | Sharpness IV, coût selon R41, R43 (PE2) |
| V-E14 | Armure `protection` IV sur un zombie sans IA, `damage` 10 type `minecraft:generic` | réduction de 16 pour cent avant armure (R32, PE5) |
| V-E15 | `sharpness` évalué par R5 et R21, L = 1 à 5 | `min_cost` 1, 12, 23, 34, 45 ; `max_cost` 21, 32, 43, 54, 65 ; `damage` 1, 1.5, 2, 2.5, 3 (PE10, PE3) |
| V-E16 | `protection` évalué par R21, L = 1 à 4 | `damage_protection` 1, 2, 3, 4 (PE5, PE10) |
| V-E5 | `unbreaking` (R23) | armure 0.2, 0.2667, 0.3 ; autres 0.5, 0.6667, 0.75 (PE5) |
| V-E6 | `efficiency` (R24) | 2, 5, 10, 17, 26 (PE10) |
| V-E17 | `wind_burst` évalué par R21 | L=1 -> 1.2, L=2 -> 1.75, L=3 -> 2.2, L=4 -> 2.55 (hors liste : 1.5 + 0.35 x 3) (PE10) |

## Plan de tests

- **Unitaire (L1)** : décodage des 43 enchantements, 7 fournisseurs, 21 étiquettes `enchantable`, 7 ensembles exclusifs ; V-E1 à V-E4, V-E7 à V-E10 ; évaluation de chaque valeur dépendante du niveau (R20, R21) pour L = 1 à `max_level`, comparée à la section `R22` du script à 6 chiffres (valeurs provisoires S, confirmées par PE10) ; propriétés : exclusion symétrique à l'intérieur d'un ensemble, `lookup` hors liste, constantes.
- **Statistique (L2)** : PE1 (distribution `enchant_with_levels`), PE3 (offres de la table). Seuil : khi-deux, N >= 100000, rejet si p < 1e-4, trois graines de session.
- **Déterministe (L2)** : PE5 (dégâts exacts d'un zombie sans IA sous `/tick freeze`) ; PE9 (coûts de ligne).
- **Niveau A** : tests en aveugle, relecture par un second agent (aléa et formules de gameplay).

## Sondes proposées

Cadre : serveur 26.3, monde plat, RCON. Les tables de butin de sondes (pack `sonde`) utilisent `enchant_with_levels` / `enchant_randomly` et sont tirées par `loot spawn`. Les entités d'essai sont des zombies `{NoAI:1b,PersistenceRequired:1b}` sous `/tick freeze`.

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Règles levées |
|---|---|---|---|
| PE1 | Pack : table `sonde:ench` pool 1 roll, `item book` + `enchant_with_levels` `levels` N, `options` absent ; N dans {1, 5, 10, 15, 20, 30, 39, 50} ; idem avec `diamond_sword`, `iron_pickaxe`, `bow`, `diamond_chestplate`, `leather_boots`. 100000 `loot spawn` par cas ; relevé des enchantements et niveaux (`data get entity` de l'objet au sol) | Distribution de (enchantement, niveau) et du nombre d'enchantements ; vérification de la continuation (eCost + 1) / 50 | R36, R38, R39 |
| PE2 | Enclume (client protocole : voir `item-components-inventory.md` PI9) : paires d'objets (Sharpness III + III, Unbreaking + livre, objets incompatibles, renommage, matière de réparation) ; relevé du coût affiché et de l'objet de sortie ; `enchant_randomly` pour les cas d'incompatibilité sans client | Coûts et résultats de R41 à R43 | R41 à R43, R35 |
| PE3 | Table d'enchantement (client protocole) : 0, 7, 15 étagères, objets d'enchantabilité 1, 10, 15, 25 ; 2000 ouvertures avec `XpSeed` régénérée ; relevé des trois coûts et des enchantements offerts | Distribution des lignes (R37), répartition des candidats (R38) | R37 à R40 |
| PE4 | Meule et réparation par expérience (client) : objets avec Mending, `/xp add`, `/summon experience_orb` ; relevé de la durabilité | Facteurs de R44 | R44 |
| PE5 | Zombie sans IA en armure `protection` I à IV, `fire_protection`, `blast_protection`, `feather_falling`, `projectile_protection`, sous `/tick freeze` ; `damage @e[type=zombie,limit=1] 10 minecraft:<type de dégât>` ; relevé de `Health` et des `Damage` des pièces. Répétition 200 fois pour `unbreaking` et lecture de la durabilité | Réduction par type de dégât ; probabilités d'annulation d'usure de R23 | R19, R23, R31, R32 |
| PE6 | `give` d'objets avec enchantements incompatibles (`protection` + `blast_protection`, `riptide` + `loyalty`, `infinity` + `mending`) ; `/enchant @s minecraft:loyalty` sur un trident `riptide` ; lecture | Symétrie de l'exclusion, rejet par `/enchant` | R8 |
| PE7 | `give @s diamond_sword[enchantments={"minecraft:sharpness":0}]`, niveaux 6, 255, 256, -1 ; `/enchant @s minecraft:sharpness 6` ; `/enchant @s minecraft:sharpness 0` | Plages acceptées, retrait des entrées | R10 |
| PE8 | Client protocole : un joueur attaque un zombie sans IA avec des épées Sharpness I à V, Smite, Bane, Knockback, Fire Aspect | Dégâts additionnels, knockback, durée du feu | R14, R16, R31 |
| PE9 | Coûts de ligne (PE3) mesurés pour 0 à 15 étagères et `XpSeed` fixe ; deux lectures par graine | Coûts déterministes pour une graine donnée | R37 |
| PE10 | Pack `sonde` avec enchantements de test `sonde:t1` à `sonde:tn` portant `damage_protection` avec des listes d'entrées combinant `add`, `multiply`, `set`, `all_of`, `linear`, `fraction`, `clamped`, `lookup` (dont niveau hors liste), `levels_squared`, `exponent`, `exponential`, ordre des entrées permuté ; zombie sans IA en armure `sonde:tN` ; `damage` 10 puis lecture de `Health` ; variantes `requirements` avec `enchantment_level` et `random_chance` | Valeur finale en fonction de l'ordre ; sémantique des effets de valeur et des valeurs dépendantes du niveau | R13, R15, R17, R18, R19, R21, R25 |
| PE11 | Pack `sonde` : enchantement `attributes` et `location_changed` avec effet `attribute` et `replace_disk` sur des bottes de zombie sans IA ; `/tick step` et déplacement par `tp` ; lecture de `attributes` de l'entité | Modificateurs d'attribut créés, retirés ; déclencheurs de `location_changed` | R16, R27, R29, R33 |
| PE12 | Observation des équipements de zombies générés avec `enchantment_provider` (spawn naturel, raid) sur 10000 entités ; dépend du jalon M7 | Distribution des coûts et enchantements du fournisseur | R34 |

## Couverture des règles S par les sondes

R5 (usage des coûts), R7b : PE1, PE3 ; R9b : PE3, PE12, PL13 ; R19a à R19e : PE5, PE8, PE10 ; R6 (emplacements actifs) : PE5, PE8 ; R13, R15, R17, R18, R19, R21, R25 : PE10 ; R16, R27, R29, R33 : PE11 ; R34 : PE12 ; R31 et R32 : PE5, PE8 ; R41b à R44b : PE2, PE4. Aucune autre règle S n'est sans sonde.

## Questions ouvertes

- Probabilité de continuation et boucle de sélection de la table d'enchantement (R39) : la page publique est garbée ; PE1 est la sonde décisive (elle teste `enchant_with_levels`, que la documentation dit équivalent).
- Valeur de départ et consommation exacte de chaque composant d'effet (R19) : dépendent des formules de combat de M7.
- Symétrie de l'exclusion (R8) et ordre des enchantements d'un objet (R31).
- Ordre d'application entre pièces d'armure (R31).
- `exponential` (effet de valeur) et `exponent` (valeur dépendante du niveau) n'ont aucun usage vanilla : sémantique incertaine.
- Formule de la meule, de l'enclume et du coût d'expérience (R41b à R44b) : à lever avec un client protocole (PE2 à PE4).
- Lien entre `anvil_cost` et le multiplicateur de livre (R43).
- Règles S restantes : R5 (formule, usage), R6, R7b, R9b, R10 (plage), R13 à R16, R17 (sens), R18, R19a à R19e, R21, R22 (valeurs évaluées), R23, R24, R25 (usage), R27 (cycle de vie), R31 à R40, R41b à R44b.

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Enchantements | M6 | A | spécifié | `docs/specs/item-enchantments.md` (43 définitions, effets, valeurs par niveau en D/C ; sélection, enclume, meule en S) |

## Annexe : enchant_stats.py (C)

Lit uniquement les fichiers extraits ; aucune dépendance au jeu ; bibliothèque standard seule. À copier hors du dossier de données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I enchant_stats.py "$REF" | sha256sum
```

Empreinte SHA-256 attendue de la sortie complète (sortie en UTF-8, fins de ligne LF) :

```text
79029525faa3df891ea7a9fb3fd2a4adf83d3735f896d5ff4fc30289323fab17
```

La section `R22` applique les formules de R21 (niveau S) aux paramètres lus dans les données ; seule sa partie "paramètres" est D.

```python
"""Definitions, effects and per-level values of the 26.3 enchantments, from the extracted data.

Usage: python -I enchant_stats.py <reference dir containing generated/ and resources/>
Reads extracted files only. Standard library only. Output is deterministic (sorted, LF).

The per-level values printed in section R22 are computed with the level-based value formulas of
rule R21 of item-enchantments.md. Those formulas are level S: only the parameters are data.
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


def short(t):
    return t.split(":", 1)[1] if isinstance(t, str) and ":" in t else t


def as_list(v):
    return v if isinstance(v, list) else [v]


ench = tree("enchantment")
tags = tree("tags/enchantment")
item_tags = tree("tags/item/enchantable")
LEVEL_T = reg("enchantment_level_based_value_type")
VALUE_T = reg("enchantment_value_effect_type")
ENTITY_T = reg("enchantment_entity_effect_type")
LOC_T = reg("enchantment_location_based_effect_type")
COMP_T = reg("enchantment_effect_component_type")
ENTITY_COMPONENTS = {"post_attack", "tick", "location_changed", "hit_block", "projectile_spawned", "post_piercing_attack"}

section("R1-R3 definitions")
print("files", len(ench), "registry counts", {r: len(reg(r)) for r in (
    "enchantment_effect_component_type", "enchantment_entity_effect_type", "enchantment_location_based_effect_type",
    "enchantment_value_effect_type", "enchantment_level_based_value_type", "enchantment_provider_type")})
kc = collections.Counter()
for e in ench.values():
    kc.update(e.keys())
print("field presence", sorted(kc.items()))
print("max_level", sorted(collections.Counter(e["max_level"] for e in ench.values()).items()))
print("weight", sorted(collections.Counter(e["weight"] for e in ench.values()).items()))
print("anvil_cost", sorted(collections.Counter(e["anvil_cost"] for e in ench.values()).items()))
slots = collections.Counter()
for e in ench.values():
    slots.update(as_list(e["slots"]))
print("slot groups", sorted(slots.items(), key=lambda kv: (-kv[1], kv[0])))
print("without effects", [k for k, e in ench.items() if "effects" not in e])
print("no primary_items", sum(1 for e in ench.values() if "primary_items" not in e), "with", sorted(k for k, e in ench.items() if "primary_items" in e))
print("min_cost and max_cost keys", sorted(collections.Counter(tuple(sorted(e["min_cost"])) for e in ench.values()).items()))

section("R4 one line per enchantment (regenerates the definition table)")
for k, e in ench.items():
    print(k, e["max_level"], e["weight"], e["anvil_cost"], "min=%s+%s" % (e["min_cost"]["base"], e["min_cost"]["per_level_above_first"]),
          "max=%s+%s" % (e["max_cost"]["base"], e["max_cost"]["per_level_above_first"]), ",".join(as_list(e["slots"])),
          e["supported_items"], e.get("primary_items", "-"), json.dumps(e.get("exclusive_set", "-")))

section("R5 costs by level")
for k in ("sharpness", "fortune", "efficiency", "mending"):
    e = ench[k]
    print(k, "min", [e["min_cost"]["base"] + e["min_cost"]["per_level_above_first"] * (l - 1) for l in range(1, e["max_level"] + 1)],
          "max", [e["max_cost"]["base"] + e["max_cost"]["per_level_above_first"] * (l - 1) for l in range(1, e["max_level"] + 1)])

section("R7 enchantable item tags and supported vs primary")
print("tags", len(item_tags), sorted(item_tags))
print("supported_items distinct", len({e["supported_items"] for e in ench.values()}), "primary distinct", len({e["primary_items"] for e in ench.values() if "primary_items" in e}))
print("supported != primary", sorted(k for k, e in ench.items() if "primary_items" in e and e["primary_items"] != e["supported_items"]))

section("R8 exclusive sets")
sets = {k.split("/")[1]: [v for v in t["values"]] for k, t in tags.items() if k.startswith("exclusive_set/")}
for k in sorted(sets):
    print(k, len(sets[k]), sets[k])
decl = collections.defaultdict(list)
for k, e in ench.items():
    if "exclusive_set" in e:
        decl[json.dumps(e["exclusive_set"])].append(k)
print("declarations", sorted((k, len(v)) for k, v in decl.items()))
member = collections.defaultdict(set)
for s, vs in sets.items():
    for v in vs:
        member[v.split(":")[1]].add(s)
asym = []
for k, e in ench.items():
    if "exclusive_set" in e:
        s = e["exclusive_set"]
        s = s[len("#minecraft:exclusive_set/"):] if isinstance(s, str) else None
        if s and s not in member.get(k, set()):
            asym.append(k)
print("declares a set it is not a member of", asym)
print("members that declare nothing", sorted(v.split(":")[1] for vs in sets.values() for v in vs if "exclusive_set" not in ench[v.split(":")[1]]))

section("R9 enchantment tags")
for k, t in tags.items():
    if not k.startswith("exclusive_set/"):
        print(k, len(t["values"]), [v for v in t["values"] if v.startswith("#")] or (t["values"] if len(t["values"]) < 8 else "..."))

section("R11 effect components in use and value shapes")
usage = collections.Counter()
shapes = collections.defaultdict(collections.Counter)
for e in ench.values():
    for c, v in e.get("effects", {}).items():
        usage[short(c)] += 1
        shapes[short(c)][type(v).__name__] += 1
for c in sorted(COMP_T):
    print(c, usage[c], dict(shapes[c]))
print("components unused", sorted(c for c in COMP_T if not usage[c]))

section("R12-R14 entry keys, enchanted, affected")
entry_keys = collections.Counter()
enchanted = collections.Counter()
affected = collections.Counter()
for e in ench.values():
    for c, v in e.get("effects", {}).items():
        if isinstance(v, list):
            for en in v:
                if isinstance(en, dict):
                    entry_keys.update((short(c), k) for k in en)
                    if "enchanted" in en:
                        enchanted[(short(c), en["enchanted"])] += 1
                    if "affected" in en:
                        affected[(short(c), en["affected"])] += 1
print("entry keys", sorted(entry_keys.items()))
print("enchanted", sorted(enchanted.items()), "affected", sorted(affected.items()))

section("R17 value effect types by consumer component (all_of expanded)")
veff = collections.Counter()
eeff = collections.Counter()
efields = collections.defaultdict(collections.Counter)


def visit_effect(eff, entity_ctx):
    t = short(eff.get("type")) or "(empty)"
    (eeff if entity_ctx else veff)[t] += 1
    for k in eff:
        if k != "type":
            efields[t][k] += 1
    if t == "all_of":
        for sub in eff["effects"]:
            visit_effect(sub, entity_ctx)


for e in ench.values():
    for c, v in e.get("effects", {}).items():
        c = short(c)
        if c == "attributes":
            continue
        if isinstance(v, list):
            for en in v:
                if isinstance(en, dict) and "effect" in en:
                    visit_effect(en["effect"], c in ENTITY_COMPONENTS)
print("value effects", sorted(veff.items()), "registry", sorted(VALUE_T))
print("entity or location effects", sorted(eeff.items()), "registry", sorted(ENTITY_T), "location", sorted(set(LOC_T) - set(ENTITY_T)))
print("attribute effects in attributes component", sum(len(v) for e in ench.values() for c, v in e.get("effects", {}).items() if short(c) == "attributes"))
print("attribute effects in location_changed", sum(1 for e in ench.values() for en in e.get("effects", {}).get("minecraft:location_changed", []) if short(en["effect"]["type"]) == "attribute"))

section("R20 level-based value type usage (typed nodes anywhere; min_cost and max_cost have no type)")
lv = collections.Counter()


def count_level(o):
    if isinstance(o, dict):
        t = short(o.get("type"))
        if t in LEVEL_T and ("base" in o or "added" in o or "numerator" in o or "values" in o or "value" in o or "power" in o):
            lv[t] += 1
        for v in o.values():
            count_level(v)
    elif isinstance(o, list):
        for v in o:
            count_level(v)


for e in ench.values():
    count_level(e.get("effects", {}))
print(sorted(lv.items()), "registry", sorted(LEVEL_T))


def ev(v, L):
    if isinstance(v, (int, float)):
        return float(v)
    t = short(v["type"])
    if t == "linear":
        return v["base"] + v["per_level_above_first"] * (L - 1)
    if t == "levels_squared":
        return L * L + v["added"]
    if t == "fraction":
        return ev(v["numerator"], L) / ev(v["denominator"], L)
    if t == "clamped":
        return min(max(ev(v["value"], L), v["min"]), v["max"])
    if t == "lookup":
        return v["values"][L - 1] if L - 1 < len(v["values"]) else ev(v["fallback"], L)
    if t == "exponent":
        return ev(v["base"], L) ** ev(v["power"], L)
    raise ValueError(t)


def fmt(x):
    return "%.6g" % x


section("R22 per-level values (formulas of R21 applied to the data parameters)")
for k, e in ench.items():
    for c, v in e.get("effects", {}).items():
        c = short(c)
        if c == "attributes":
            for en in v:
                print(k, c, short(en["attribute"]), short(en["operation"]) if "operation" in en else "", ";".join(fmt(ev(en["amount"], l)) for l in range(1, e["max_level"] + 1)))
        elif isinstance(v, list):
            for i, en in enumerate(v):
                eff = en.get("effect") if isinstance(en, dict) else None
                if not isinstance(eff, dict) or "type" not in eff:
                    continue
                t = short(eff["type"])
                field = {"add": "value", "set": "value", "multiply": "factor", "remove_binomial": "chance"}.get(t)
                if field and field in eff:
                    print(k, c, t, "entry", i, ";".join(fmt(ev(eff[field], l)) for l in range(1, e["max_level"] + 1)))
        elif isinstance(v, dict) and "type" in v and short(v["type"]) in VALUE_T:
            t = short(v["type"])
            field = {"add": "value", "set": "value", "multiply": "factor", "remove_binomial": "chance"}.get(t)
            if field:
                print(k, c, t, "direct", ";".join(fmt(ev(v[field], l)) for l in range(1, e["max_level"] + 1)))
for k in ("wind_burst",):
    kb = ench[k]["effects"]["minecraft:post_attack"][0]["effect"]["knockback_multiplier"]
    print(k, "knockback_multiplier lookup", [fmt(ev(kb, l)) for l in range(1, 5)])
print("thorns", json.dumps(ench["thorns"]["effects"], sort_keys=True))

section("R24-R25 anvil_cost of the pairs quoted by the public wiki")
print({k: ench[k]["anvil_cost"] for k in ("sharpness", "efficiency", "unbreaking", "smite", "looting", "mending", "soul_speed")})

section("R28 fields of entity and location effects")
for t in sorted(efields):
    print(t, ",".join(k + ":" + str(n) for k, n in sorted(efields[t].items())))

section("R29-R30 predicate keys in requirements")
pk = collections.Counter()
bp = collections.Counter()


def collect(o):
    if isinstance(o, dict):
        if short(o.get("type")) == "entity_properties" and isinstance(o.get("predicate"), dict):
            pk.update(o["predicate"].keys())
        if short(o.get("type")) == "replace_disk" and isinstance(o.get("predicate"), dict):
            bp.update(short(x["type"]) for x in as_list(o["predicate"].get("predicates", [o["predicate"]])))
        for v in o.values():
            collect(v)
    elif isinstance(o, list):
        for v in o:
            collect(v)


for e in ench.values():
    collect(e)
print("entity_properties predicate keys", sorted(pk.items()))
print("block predicate types in replace_disk", sorted(bp.items()), "registry", sorted(reg("block_predicate_type")))

section("R34 enchantment providers")
for k, p in tree("enchantment_provider").items():
    print(k, json.dumps(p, sort_keys=True))

section("R10 enchant command")
cmds = load(os.path.join(REP, "commands.json"))
node = cmds["children"]["enchant"]
print(json.dumps(node, sort_keys=True)[:400])
```

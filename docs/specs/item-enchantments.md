# Enchantements : définitions, effets, sélection et application

Statut : brouillon
Jalon : M6
Risque : A
Règles : 44 (R1 à R44)

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
- **Sondes de l'oracle** : aucune exécutée. Voir PE1 à PE9.

Convention de preuve : D valeur lue dans un fichier extrait ; C calcul reproductible sur ces fichiers (valeurs par niveau, évaluées en double puis affichées à 6 chiffres significatifs ; les données sont des Float 32 bits) ; M observé sur l'oracle (non utilisé) ; S documentation publique seule ou hypothèse. Chaque règle S porte un **comportement provisoire**.

## Règles

### Définition d'un enchantement

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Le registre `enchantment` est chargeable (un fichier par enchantement) : 43 fichiers vanilla. Identifiant `minecraft:<nom>`. | D |
| R2 | Champs obligatoires de chaque fichier : `description` (composant de texte), `max_level` (entier), `weight` (entier), `anvil_cost` (entier), `min_cost` et `max_cost` (`{base, per_level_above_first}`), `slots` (liste de groupes d'emplacements), `supported_items` (étiquette d'objets). Champs optionnels : `primary_items` (étiquette, 5 enchantements), `exclusive_set` (étiquette ou identifiant ou liste, 18 enchantements), `effects` (table composant -> valeur, 42 enchantements : `fortune` n'en a pas). | D |
| R3 | Répartition des paramètres (C) : `max_level` 1 (9 enchantements), 2 (4), 3 (16), 4 (7), 5 (7) ; `weight` 1 (8), 2 (19), 5 (11), 10 (5) ; `anvil_cost` 1 (5), 2 (11), 4 (19), 8 (8) ; groupes de `slots` : `mainhand` 25, `armor` 6, `any` 4, `feet` 3, `head` 2, `hand` 2, `offhand` 1, `legs` 1 (un enchantement peut en lister plusieurs : `quick_charge` = `mainhand` et `offhand`). Le tableau complet est en R4. | C |
| R4 | Tableau des 43 enchantements. Colonnes : `max_level`, `weight`, `anvil_cost`, coût minimal (`base+par_niveau`), coût maximal, `slots`, `supported_items`, `primary_items` (`-` si absent), `exclusive_set` (`-` si absent). | D |

| Enchantement | Niveau max | Poids | Coût enclume | Coût min | Coût max | Emplacements | Objets admis | Objets primaires | Ensemble exclusif |
|---|---|---|---|---|---|---|---|---|---|
| `aqua_affinity` | 1 | 2 | 4 | 1+0 | 41+0 | head | `#minecraft:enchantable/head_armor` | `-` | `-` |
| `bane_of_arthropods` | 5 | 5 | 2 | 5+8 | 25+8 | mainhand | `#minecraft:enchantable/weapon` | `#minecraft:enchantable/melee_weapon` | `#minecraft:exclusive_set/damage` |
| `binding_curse` | 1 | 1 | 8 | 25+0 | 50+0 | armor | `#minecraft:enchantable/equippable` | `-` | `-` |
| `blast_protection` | 4 | 2 | 4 | 5+8 | 13+8 | armor | `#minecraft:enchantable/armor` | `-` | `#minecraft:exclusive_set/armor` |
| `breach` | 4 | 2 | 4 | 15+9 | 65+9 | mainhand | `#minecraft:enchantable/mace` | `-` | `#minecraft:exclusive_set/damage` |
| `channeling` | 1 | 1 | 8 | 25+0 | 50+0 | mainhand | `#minecraft:enchantable/trident` | `-` | `-` |
| `density` | 5 | 5 | 2 | 5+8 | 25+8 | mainhand | `#minecraft:enchantable/mace` | `-` | `#minecraft:exclusive_set/damage` |
| `depth_strider` | 3 | 2 | 4 | 10+10 | 25+10 | feet | `#minecraft:enchantable/foot_armor` | `-` | `#minecraft:exclusive_set/boots` |
| `efficiency` | 5 | 10 | 1 | 1+10 | 51+10 | mainhand | `#minecraft:enchantable/mining` | `-` | `-` |
| `feather_falling` | 4 | 5 | 2 | 5+6 | 11+6 | armor | `#minecraft:enchantable/foot_armor` | `-` | `-` |
| `fire_aspect` | 2 | 2 | 4 | 10+20 | 60+20 | mainhand | `#minecraft:enchantable/fire_aspect` | `#minecraft:enchantable/melee_weapon` | `-` |
| `fire_protection` | 4 | 5 | 2 | 10+8 | 18+8 | armor | `#minecraft:enchantable/armor` | `-` | `#minecraft:exclusive_set/armor` |
| `flame` | 1 | 2 | 4 | 20+0 | 50+0 | mainhand | `#minecraft:enchantable/bow` | `-` | `-` |
| `fortune` | 3 | 2 | 4 | 15+9 | 65+9 | mainhand | `#minecraft:enchantable/mining_loot` | `-` | `#minecraft:exclusive_set/mining` |
| `frost_walker` | 2 | 2 | 4 | 10+10 | 25+10 | feet | `#minecraft:enchantable/foot_armor` | `-` | `#minecraft:exclusive_set/boots` |
| `impaling` | 5 | 2 | 4 | 1+8 | 21+8 | mainhand | `#minecraft:enchantable/trident` | `-` | `#minecraft:exclusive_set/damage` |
| `infinity` | 1 | 1 | 8 | 20+0 | 50+0 | mainhand | `#minecraft:enchantable/bow` | `-` | `#minecraft:exclusive_set/bow` |
| `knockback` | 2 | 5 | 2 | 5+20 | 55+20 | mainhand | `#minecraft:enchantable/melee_weapon` | `-` | `-` |
| `looting` | 3 | 2 | 4 | 15+9 | 65+9 | mainhand | `#minecraft:enchantable/melee_weapon` | `-` | `-` |
| `loyalty` | 3 | 5 | 2 | 12+7 | 50+0 | mainhand | `#minecraft:enchantable/trident` | `-` | `-` |
| `luck_of_the_sea` | 3 | 2 | 4 | 15+9 | 65+9 | mainhand | `#minecraft:enchantable/fishing` | `-` | `-` |
| `lunge` | 3 | 5 | 2 | 5+8 | 25+8 | hand | `#minecraft:enchantable/lunge` | `-` | `-` |
| `lure` | 3 | 2 | 4 | 15+9 | 65+9 | mainhand | `#minecraft:enchantable/fishing` | `-` | `-` |
| `mending` | 1 | 2 | 4 | 25+25 | 75+25 | any | `#minecraft:enchantable/durability` | `-` | `-` |
| `multishot` | 1 | 2 | 4 | 20+0 | 50+0 | mainhand | `#minecraft:enchantable/crossbow` | `-` | `#minecraft:exclusive_set/crossbow` |
| `piercing` | 4 | 10 | 1 | 1+10 | 50+0 | mainhand | `#minecraft:enchantable/crossbow` | `-` | `#minecraft:exclusive_set/crossbow` |
| `power` | 5 | 10 | 1 | 1+10 | 16+10 | mainhand | `#minecraft:enchantable/bow` | `-` | `-` |
| `projectile_protection` | 4 | 5 | 2 | 3+6 | 9+6 | armor | `#minecraft:enchantable/armor` | `-` | `#minecraft:exclusive_set/armor` |
| `protection` | 4 | 10 | 1 | 1+11 | 12+11 | armor | `#minecraft:enchantable/armor` | `-` | `#minecraft:exclusive_set/armor` |
| `punch` | 2 | 2 | 4 | 12+20 | 37+20 | mainhand | `#minecraft:enchantable/bow` | `-` | `-` |
| `quick_charge` | 3 | 5 | 2 | 12+20 | 50+0 | mainhand,offhand | `#minecraft:enchantable/crossbow` | `-` | `-` |
| `respiration` | 3 | 2 | 4 | 10+10 | 40+10 | head | `#minecraft:enchantable/head_armor` | `-` | `-` |
| `riptide` | 3 | 2 | 4 | 17+7 | 50+0 | hand | `#minecraft:enchantable/trident` | `-` | `#minecraft:exclusive_set/riptide` |
| `sharpness` | 5 | 10 | 1 | 1+11 | 21+11 | mainhand | `#minecraft:enchantable/sharp_weapon` | `#minecraft:enchantable/melee_weapon` | `#minecraft:exclusive_set/damage` |
| `silk_touch` | 1 | 1 | 8 | 15+0 | 65+0 | mainhand | `#minecraft:enchantable/mining_loot` | `-` | `#minecraft:exclusive_set/mining` |
| `smite` | 5 | 5 | 2 | 5+8 | 25+8 | mainhand | `#minecraft:enchantable/weapon` | `#minecraft:enchantable/melee_weapon` | `#minecraft:exclusive_set/damage` |
| `soul_speed` | 3 | 1 | 8 | 10+10 | 25+10 | feet | `#minecraft:enchantable/foot_armor` | `-` | `-` |
| `sweeping_edge` | 3 | 2 | 4 | 5+9 | 20+9 | mainhand | `#minecraft:enchantable/sweeping` | `-` | `-` |
| `swift_sneak` | 3 | 1 | 8 | 25+25 | 75+25 | legs | `#minecraft:enchantable/leg_armor` | `-` | `-` |
| `thorns` | 3 | 1 | 8 | 10+20 | 60+20 | any | `#minecraft:enchantable/armor` | `#minecraft:enchantable/chest_armor` | `-` |
| `unbreaking` | 3 | 5 | 2 | 5+8 | 55+8 | any | `#minecraft:enchantable/durability` | `-` | `-` |
| `vanishing_curse` | 1 | 1 | 8 | 25+0 | 50+0 | any | `#minecraft:enchantable/vanishing` | `-` | `-` |
| `wind_burst` | 3 | 2 | 4 | 15+9 | 65+9 | mainhand | `#minecraft:enchantable/mace` | `-` | `-` |

| Réf | Règle | Preuve |
|---|---|---|
| R5 | Coût d'un enchantement au niveau L (pour la table d'enchantement) : `min_cost(L)` = `base + per_level_above_first x (L - 1)`, de même pour `max_cost(L)` (même forme que la valeur dépendante du niveau `linear`, R25). Exemples (C) : `sharpness` min 1, 12, 23, 34, 45 et max 21, 32, 43, 54, 65 pour L = 1 à 5 ; `fortune` min 15, 24, 33 et max 65, 74, 83 ; `efficiency` min 1, 11, 21, 31, 41 et max 51, 61, 71, 81, 91 ; `mending` (niveau 1) min 25 et max 75. | C (valeurs) ; S (usage, R37) |
| R6 | `slots` : groupes d'emplacements où l'enchantement est actif quand l'objet y est porté ou tenu : `mainhand`, `offhand`, `hand` (main principale ou secondaire), `head`, `chest`, `legs`, `feet`, `armor` (les quatre pièces), `any` (tout emplacement), `body` (documentation). L'enchantement d'un objet hors de ses emplacements ne produit pas d'effet. Provisoire. | D (valeurs vues) ; S (sens) |
| R7 | `supported_items` : objets sur lesquels l'enchantement peut figurer (commandes, enclume, butin). `primary_items` : sous-ensemble tiré par la table d'enchantement (R38) ; absent signifie égal à `supported_items`. Exemple : `sharpness` supporte `#minecraft:enchantable/sharp_weapon` mais a pour primaires `#minecraft:enchantable/melee_weapon` ; `smite` supporte `#minecraft:enchantable/weapon`. Les étiquettes `enchantable/*` sont 21 (armor, bow, chest_armor, crossbow, durability, equippable, fire_aspect, fishing, foot_armor, head_armor, leg_armor, lunge, mace, melee_weapon, mining, mining_loot, sharp_weapon, sweeping, trident, vanishing, weapon). | D |
| R8 | `exclusive_set` : l'enchantement est incompatible avec tout autre enchantement de l'ensemble (lui-même exclu). Ensembles vanilla : `armor` (protection, blast_protection, fire_protection, projectile_protection), `boots` (frost_walker, depth_strider), `bow` (infinity, mending), `crossbow` (multishot, piercing), `damage` (sharpness, smite, bane_of_arthropods, impaling, density, breach), `mining` (fortune, silk_touch), `riptide` (loyalty, channeling ; référencé par `riptide`). Les 7 ensembles sont dans `tags/enchantment/exclusive_set/`. Observation : `riptide` a pour ensemble exclusif `#minecraft:exclusive_set/riptide` qui ne contient pas `riptide` mais ses deux incompatibles ; `loyalty` et `channeling` ne déclarent aucun `exclusive_set` ; de même `mending` pour l'ensemble `bow`. La relation n'est donc déclarée que d'un côté dans ces cas (D). Provisoire : deux enchantements sont incompatibles si l'un des deux cite l'autre dans son ensemble exclusif (relation symétrique). Sonde PE6. | D (ensembles, asymétrie des déclarations) ; S (symétrie) |
| R9 | Étiquettes d'enchantements (D, contenu) : `non_treasure` (36 enchantements), `treasure` (7 : `binding_curse`, `vanishing_curse`, `swift_sneak`, `soul_speed`, `frost_walker`, `mending`, `wind_burst`), `curse` (2), `in_enchanting_table` (= `#non_treasure`), `on_random_loot` (`#non_treasure`, les deux malédictions, `frost_walker`, `mending`), `tradeable` (idem `on_random_loot`), `on_mob_spawn_equipment` et `on_traded_equipment` (`#non_treasure`), `double_trade_price` (`#treasure`), `smelts_loot` (`fire_aspect`), `prevents_bee_spawns_when_mining`, `prevents_decorated_pot_shattering`, `prevents_ice_melting`, `prevents_infested_spawns` (chacune = `silk_touch`), `tooltip_order` (43 enchantements dans l'ordre d'affichage). Chaque canal de sélection (table, butin, échange, équipement de mob) lit sa propre étiquette ; une liste globale unique est une erreur. | D |
| R10 | Les composants `enchantments` (objet enchanté) et `stored_enchantments` (livre enchanté) sont des tables identifiant -> niveau entier, valides de 1 à 255 ; défaut `{}` (D, tous les prototypes). L'argument `level` de la commande `enchant` a pour minimum 0 (D `commands.json`). Un niveau supérieur à `max_level` est permis par les données (`give`, modificateur), mais pas par `/enchant` (provisoire). Un niveau 0 ou négatif retire l'entrée (modificateur) ou est refusé (S). Sonde PE7. | D (défaut) ; S (plage) |

### Composants d'effet

| Réf | Règle | Preuve |
|---|---|---|
| R11 | Le champ `effects` est une table composant d'effet -> valeur. 31 composants sont enregistrés (`enchantment_effect_component_type`) ; 30 sont utilisés par les 43 enchantements, `mob_experience` ne l'est pas. Nombre d'enchantements qui portent chaque composant : voir le tableau suivant. | D |

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
| R17 | Effets de valeur (registre `enchantment_value_effect_type`, 6) : `add` {`value`}, `set` {`value`}, `multiply` {`factor`}, `remove_binomial` {`chance`}, `all_of` {`effects`}, `exponential` {`factor`} (documentation incertaine). Usage vanilla : `add` 23, `all_of` 16, `set` 2, `multiply` 1, `remove_binomial` 2, `exponential` 0. `all_of` exécute ses sous-effets en série. | D (usage) ; S (sens) |
| R18 | Sémantique des effets de valeur (S) : `add` ajoute `value(niveau)` à la valeur courante ; `set` la remplace ; `multiply` la multiplie par `factor(niveau)` ; `remove_binomial` fait un essai de probabilité `chance(niveau)` et retire 1 en cas de succès ; la valeur de départ est celle du composant (R19). Les arrondis et clamps sont ceux du composant consommateur (S). | S |
| R19 | Valeurs de départ et consommateurs (S) : `damage_protection` démarre à 0 et la protection vaut min(somme, 20) x 4 pour cent de réduction (plafond 80 pour cent) ; `damage` démarre à 0 (dégâts ajoutés) ; `knockback` démarre à 0 ; `armor_effectiveness` démarre à 0 et se lit comme une efficacité (0 inefficace, 1 pleine ; `breach` ajoute -0.15 x niveau) ; `item_damage` démarre à la perte de durabilité demandée (entier) ; `ammo_use` démarre à 1 ; `repair_with_xp` démarre à 1 ; `projectile_count` démarre à 1 ; `projectile_piercing` à 0 ; `fishing_luck_bonus` à 0 ; `block_experience` à l'expérience du bloc ; `smash_damage_per_fallen_block` à 0. Valeurs de départ : dépendent du consommateur, jalon M7 ; ici seulement S. | S |
| R20 | Valeurs dépendantes du niveau (registre `enchantment_level_based_value_type`, 6) : `linear` {`base`, `per_level_above_first`} (43 usages), `levels_squared` {`added`} (1), `fraction` {`numerator`, `denominator`} (3), `clamped` {`value`, `min`, `max`} (1), `lookup` {`values`, `fallback`} (1), `exponent` {`base`, `power`} (0). Une constante nue est permise. Les coûts `min_cost` et `max_cost` ont la forme de `linear` sans le champ `type`. | D |
| R21 | Évaluation au niveau L (entier de 1 à 255), documentation, S : `linear` = base + per_level_above_first x (L - 1) ; `levels_squared` = L x L + added ; `fraction` = numerator(L) / denominator(L) (division flottante) ; `clamped` = valeur bornée à [min, max] ; `lookup` = values[L - 1] si L - 1 < taille, sinon fallback(L) ; `exponent` = base(L) puissance power(L). Les nombres sont des Float 32 bits (les constantes des données sont stockées en Float, par exemple 0.33333334) ; provisoire : les sommes et produits se font en Float. | S |
| R22 | Valeurs par niveau des enchantements vanilla (C, double puis 6 chiffres significatifs) : tableau ci-dessous. Une colonne `L1..Lmax` est séparée par ` ; `. | C |

| Enchantement | Composant | Valeur par niveau |
|---|---|---|
| `aqua_affinity` | attributes `submerged_mining_speed` (add_multiplied_total) | 4 |
| `bane_of_arthropods` | damage add | 2.5 ; 5 ; 7.5 ; 10 ; 12.5 |
| `blast_protection` | attributes `explosion_knockback_resistance` (add_value) | 0.15 ; 0.3 ; 0.45 ; 0.6 |
| `blast_protection` | damage_protection add | 2 ; 4 ; 6 ; 8 |
| `breach` | armor_effectiveness add | -0.15 ; -0.3 ; -0.45 ; -0.6 |
| `density` | smash_damage_per_fallen_block add | 0.5 ; 1 ; 1.5 ; 2 ; 2.5 |
| `depth_strider` | attributes `water_movement_efficiency` (add_value) | 0.333333 ; 0.666667 ; 1 |
| `efficiency` | attributes `mining_efficiency` (add_value) | 2 ; 5 ; 10 ; 17 ; 26 |
| `feather_falling` | damage_protection add | 3 ; 6 ; 9 ; 12 |
| `fire_protection` | attributes `burning_time` (add_multiplied_base) | -0.15 ; -0.3 ; -0.45 ; -0.6 |
| `fire_protection` | damage_protection add | 2 ; 4 ; 6 ; 8 |
| `impaling` | damage add | 2.5 ; 5 ; 7.5 ; 10 ; 12.5 |
| `infinity` | ammo_use set | 0.0 |
| `knockback` | knockback add | 1 ; 2 |
| `looting` | equipment_drops add | 0.01 ; 0.02 ; 0.03 |
| `loyalty` | trident_return_acceleration add | 1 ; 2 ; 3 |
| `luck_of_the_sea` | fishing_luck_bonus add | 1 ; 2 ; 3 |
| `lure` | fishing_time_reduction add | 5 ; 10 ; 15 |
| `mending` | repair_with_xp multiply | 2.0 |
| `multishot` | projectile_count add | 2 |
| `multishot` | projectile_spread add | 10 |
| `piercing` | projectile_piercing add | 1 ; 2 ; 3 ; 4 |
| `power` | damage add | 1 ; 1.5 ; 2 ; 2.5 ; 3 |
| `projectile_protection` | damage_protection add | 2 ; 4 ; 6 ; 8 |
| `protection` | damage_protection add | 1 ; 2 ; 3 ; 4 |
| `punch` | knockback add | 1 ; 2 |
| `quick_charge` | crossbow_charge_time add | -0.25 ; -0.5 ; -0.75 |
| `respiration` | attributes `oxygen_bonus` (add_value) | 1 ; 2 ; 3 |
| `riptide` | trident_spin_attack_strength add | 1.5 ; 2.25 ; 3 |
| `sharpness` | damage add | 1 ; 1.5 ; 2 ; 2.5 ; 3 |
| `silk_touch` | block_experience set | 0.0 |
| `smite` | damage add | 2.5 ; 5 ; 7.5 ; 10 ; 12.5 |
| `sweeping_edge` | attributes `sweeping_damage_ratio` (add_value) | 0.5 ; 0.666667 ; 0.75 |
| `swift_sneak` | attributes `sneaking_speed` (add_value) | 0.15 ; 0.3 ; 0.45 |
| `unbreaking` | item_damage remove_binomial (entree 0) | 0.2 ; 0.266667 ; 0.3 |
| `unbreaking` | item_damage remove_binomial (entree 1) | 0.5 ; 0.666667 ; 0.75 |

| Réf | Règle | Preuve |
|---|---|---|
| R23 | Cas de la durabilité (C, sous R18 et R21) : la probabilité qu'un point de dégâts d'objet soit annulé par `unbreaking` vaut, pour une armure (objet de `#minecraft:enchantable/armor`), (2 + 2(L-1)) / (10 + 5(L-1)) = 0.2, 0.2667, 0.3 pour L = 1 à 3, et pour les autres objets L / (L + 1) = 0.5, 0.6667, 0.75. Les deux entrées sont exclusives (la seconde est la négation de la première). Sonde PE5. | C |
| R24 | Cas de `efficiency` (C) : attribut `mining_efficiency`, opération `add_value`, `levels_squared` avec `added` 1.0 : bonus de vitesse 2, 5, 10, 17, 26 pour L = 1 à 5. `sweeping_edge` : attribut `sweeping_damage_ratio`, `fraction` L / (L + 1) : 0.5, 0.6667, 0.75. `thorns` : `random_chance` 0.15 x L, dégâts 1 à 5 à l'attaquant (effet `damage_entity` type `minecraft:thorns`), perte de durabilité 2. | D / C |
| R25 | Les `min_cost` et `max_cost` suivent la forme `linear` (R20) ; les valeurs de `anvil_cost` (1, 2, 4, 8) sont les multiplicateurs de coût d'enclume de l'enchantement avant le facteur "livre" (R45). | D / S |

### Effets d'entité et de position

| Réf | Règle | Preuve |
|---|---|---|
| R26 | Effets d'entité (registre `enchantment_entity_effect_type`, 15) : `all_of` (16 usages), `apply_exhaustion`, `apply_impulse`, `apply_mob_effect`, `change_item_damage`, `damage_entity`, `explode`, `ignite`, `play_sound`, `replace_block`, `replace_disk`, `run_function`, `set_block_properties`, `spawn_particles`, `summon_entity`. Usage vanilla : `all_of` 16, `play_sound` 4, `change_item_damage` 3, `summon_entity` 2, `ignite` 2, `apply_mob_effect`, `replace_disk`, `apply_exhaustion`, `apply_impulse`, `spawn_particles`, `damage_entity`, `explode` 1 chacun ; `replace_block`, `run_function`, `set_block_properties` : 0. | D |
| R27 | Effets de position (registre `enchantment_location_based_effect_type`, 16) : les 15 effets d'entité plus `attribute` {`id`, `attribute`, `operation`, `amount`} (2 usages : `soul_speed`). Un effet `attribute` est un modificateur d'attribut dont l'identifiant est unique par enchantement et qui est retiré quand l'enchantement cesse d'être actif. | D / S |
| R28 | Champs observés des effets d'entité et de position (D) : `ignite` {`duration` valeur ou constante} ; `play_sound` {`sound` (identifiant ou liste), `volume`, `pitch` (nombre ou `uniform` {`min_inclusive`, `max_exclusive`})} ; `change_item_damage` {`amount`} ; `apply_exhaustion` {`amount`} ; `apply_impulse` {`direction`, `coordinate_scale`, `magnitude`} ; `apply_mob_effect` {`to_apply`, `min_duration`, `max_duration`, `min_amplifier`, `max_amplifier`} ; `damage_entity` {`damage_type`, `min_damage`, `max_damage`} ; `summon_entity` {`entity`} ; `explode` {`radius`, `block_interaction`, `immune_blocks`, `knockback_multiplier`, `large_particle`, `small_particle`, `sound`} ; `replace_disk` {`block_state`, `radius`, `height`, `offset`, `predicate` (prédicat de bloc), `trigger_game_event`} ; `spawn_particles` {`particle`, `horizontal_position`, `vertical_position`, `horizontal_velocity`, `vertical_velocity`, `speed`}. | D |
| R29 | Prédicat de bloc (registre `block_predicate_type`, 16) utilisé par `replace_disk` : vu `all_of` avec `matching_block_tag` (avec `offset`), `matching_blocks`, `matching_fluids`, `unobstructed`. Les autres types (`any_of`, `not`, `true`, `solid`, `replaceable`, `would_survive`, `has_sturdy_face`, `inside_world_bounds`, `height_range`, `volume_match`, `matching_biomes`) sont enregistrés. Sémantique : S, jalon M5. | D / S |
| R30 | Prédicats d'entité utilisés dans les `requirements` des enchantements : voir `loot-tables.md` R57. Clés supplémentaires vues dans les enchantements : `minecraft:periodic_tick` (entier), `minecraft:movement` (`horizontal_speed`), `minecraft:movement_affected_by` (`block`), `minecraft:stepping_on`, `minecraft:vehicle`, `minecraft:flags` (`is_flying`, `is_on_ground`), `minecraft:distance`, `minecraft:equipment`, `minecraft:location`. | D |

### Évaluation d'ensemble

| Réf | Règle | Preuve |
|---|---|---|
| R31 | Pour calculer un composant d'effet d'une entité (par exemple la protection contre un dégât), on parcourt les objets de ses emplacements d'équipement ; pour chaque objet on considère uniquement les enchantements dont `slots` contient l'emplacement de l'objet ; pour chacun, on évalue les entrées de la liste du composant au niveau de l'enchantement sur cet objet. Les contributions s'accumulent dans la valeur courante. L'ordre de parcours des emplacements et des enchantements dans un objet (ordre d'insertion de la table `enchantments`) est S. Sondes PE5, PE8. | S |
| R32 | `damage_protection` : la somme des protections de toutes les pièces est limitée à 20 points ; chaque point retire 4 pour cent des dégâts restants avant ce calcul ; l'ordre par rapport à l'armure, la résistance et l'absorption est celui de la formule de dégâts (M7). Provisoire. Sonde PE5. | S |
| R33 | Les attributs (`attributes`) d'un enchantement sont des modificateurs d'attribut persistants : identifiants `minecraft:enchantment.<nom>` (vus : `aqua_affinity`, `efficiency`, `depth_strider`, `respiration`, `sweeping_edge`, `swift_sneak`, `blast_protection`, `fire_protection`, `soul_speed`...). Ils sont appliqués quand l'objet est dans un emplacement actif et retirés sinon. Provisoire. | D (identifiants) ; S (cycle de vie) |
| R34 | Fournisseurs d'enchantements (registre `enchantment_provider`, 7 fichiers ; types `single`, `by_cost`, `by_cost_with_difficulty`) : `single` {`enchantment`, `level`} (6 fichiers : `enderman_loot_drop` silk_touch 1, `pillager_spawn_crossbow` piercing 1, `raid/pillager_post_wave_3` quick_charge 1, `raid/pillager_post_wave_5` quick_charge 2, `raid/vindicator` sharpness 1, `raid/vindicator_post_wave_5` sharpness 2) ; `by_cost_with_difficulty` {`enchantments` (étiquette), `min_cost`, `max_cost_span`} (`mob_spawn_equipment` : étiquette `#minecraft:on_mob_spawn_equipment`, `min_cost` 5, `max_cost_span` 17). `by_cost` : même forme sans difficulté (non utilisé). Provisoire : le coût est tiré uniformément dans [min_cost, min_cost + max_cost_span] (le coût dépend de la difficulté pour `by_cost_with_difficulty`) puis passé à la sélection de R41. | D (formes, valeurs) ; S (tirage) |

### Application par le butin

| Réf | Règle | Preuve |
|---|---|---|
| R35 | `enchant_randomly` (loot-tables R45) : choisit un enchantement parmi les `options` compatibles avec l'objet (`only_compatible` défaut true ; un livre est toujours compatible) à probabilité égale (provisoire : indépendamment du poids), puis un niveau de 1 à `max_level` à probabilité égale (provisoire). Un livre devient `enchanted_book` avec `stored_enchantments`. Quand plusieurs enchantements sont déjà présents, l'incompatibilité de R8 s'applique. Sondes PL13, PE2. | S |
| R36 | `enchant_with_levels` (loot-tables R45) : applique la procédure de la table d'enchantement (R37 à R44) avec le coût `levels` à la place du coût de ligne ; `options` absent : tous les enchantements ; l'enchantabilité de l'objet vient du composant `enchantable.value` (D, 77 objets) ; un livre a l'enchantabilité 1 (documentation) et devient `enchanted_book`. Sonde PE1. | S |

### Table d'enchantement (documentation, S)

| Réf | Règle | Preuve |
|---|---|---|
| R37 | Etagères : au plus 15 comptent, placées à distance exacte 2 de la table sur un axe horizontal et de 0 à 2 sur l'autre, à la hauteur de la table ou à celle du dessus ; le chemin entre l'étagère et la table doit être libre (à la hauteur de l'étagère). Les blocs de l'étiquette `enchantment_power_provider` peuvent remplacer des étagères. Pour un nombre b d'étagères (0 à 15), `xp_base` = 1 + R(7) + floor(min(15, b) / 2) + R(min(15, b)), où R(n) est un entier uniforme dans [0, n] ; coût de la ligne du haut = max(1, floor(xp_base / 3)), du milieu = floor(2 x xp_base / 3) + 1, du bas = max(xp_base, 2b). Les lignes se tirent avec la graine `XpSeed` du joueur (R44). | S |
| R38 | Pour une ligne de coût c et un objet d'enchantabilité e : k = floor(e / 4) ; coût effectif `eCost` = round((c + 1 + R(k) + R(k)) x (1 + T)) où T est triangulaire dans [-0.15, 0.15[ (T = (nextFloat + nextFloat - 1) x 0.15). Candidats : enchantements de l'étiquette `in_enchanting_table` dont l'objet est dans `primary_items` (un livre est primaire pour tous) ; pour chaque candidat on retient le plus haut niveau L dont `min_cost(L) <= eCost <= max_cost(L)` ; sans niveau valide le candidat est retiré. | S |
| R39 | Sélection : tirer un candidat avec la probabilité poids / somme des poids ; l'ajouter ; retirer les candidats incompatibles (R8) ; si plus de candidats, arrêter ; sinon continuer avec la probabilité (eCost + 1) / 50 (la page publique est garbée ; valeur à confirmer par PE1) ; en cas de continuation, eCost = floor(eCost / 2) et recommencer au tirage pondéré. Pour un livre issu de la table avec plus d'un enchantement, retirer un enchantement au hasard. Le coût en niveaux d'expérience du joueur et le nombre de lapis-lazuli (1, 2, 3 selon la ligne) sont S. | S |
| R40 | La table d'enchantement n'accepte que des objets avec `enchantable` et sans enchantement ; `XpSeed` (graine du joueur, 0 à la création d'un monde) est régénérée quand l'objet est enchanté ; à graine égale, mêmes offres. Un `XpSeed` nul est régénéré à la connexion. | S |

### Enclume, meule, réparation par expérience (documentation, S)

| Réf | Règle | Preuve |
|---|---|---|
| R41 | Pénalité de travail précédent : le composant `repair_cost` (D, défaut 0, entier) porte la pénalité ; chaque opération (sauf renommage seul) donne à l'objet de sortie la valeur 2 x max(pénalités des deux entrées) + 1 (suite 0, 1, 3, 7, 15, 31...). Coût total en niveaux = pénalités des deux entrées + coût de renommage (1 si renommé) + coût de réparation (2 si l'objet cible n'est pas intact) + coût d'enchantement ; refus si le total dépasse 39 en survie ("Too Expensive"). | D (composant) ; S (formule) |
| R42 | Réparation à l'enclume : une unité de matière restaure 25 pour cent de `max_damage` (arrondi vers le bas) pour 1 niveau (plus pénalités) ; la fusion de deux objets ajoute les durabilités restantes plus 12 pour cent de `max_damage`, plafonné. Le nom d'un objet renommé est limité à 50 caractères (Java). | S |
| R43 | Fusion d'enchantements à l'enclume : pour chaque enchantement de la pile sacrifiée absent de la cible, ajout au niveau complet sauf incompatibilité avec un enchantement de la cible (l'enchantement incompatible est perdu et coûte 1 niveau) ; si la cible l'a : niveau supérieur de la sacrifiée -> prend ce niveau ; niveau égal -> +1 jusqu'à `max_level` ; niveau inférieur -> rien. Les enchantements non supportés par la cible sont ignorés. Coût par enchantement : niveau résultant x multiplicateur, avec multiplicateur = `anvil_cost` si la source est un objet et max(1, floor(`anvil_cost` / 2)) si c'est un livre (provisoire). Les paires objet/livre que cite le site public (Sharpness 1/1, Efficiency 1/1, Unbreaking 2/1, Smite 2/1, Looting 4/2, Mending 4/2, Soul Speed 8/4) sont toutes compatibles avec cette règle et avec les valeurs d'`anvil_cost` des données ; la règle générale est S. Sonde PE2. | D (anvil_cost) ; S (formule) |
| R44 | Meule : retire les enchantements non maudits d'un objet (conserve `binding_curse` et `vanishing_curse`, étiquette `curse`) et rend de l'expérience selon les niveaux et les coûts ; elle supprime la pénalité de travail et conserve le nom. Réparation par expérience (`repair_with_xp`, facteur `multiply` 2.0 pour `mending`) : chaque point d'expérience ramassé répare `2 x facteur` points de durabilité sur un objet enchanté de l'emplacement actif. Formules exactes : S. Sondes PE3, PE4. | S |

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
| V-E2 | `sharpness` : `min_cost`, `max_cost` pour L = 1 à 5 | min 1, 12, 23, 34, 45 ; max 21, 32, 43, 54, 65 | C |
| V-E3 | `sharpness`, composant `damage`, effet `add` `linear` base 1.0, par niveau 0.5, sans `requirements` | 1, 1.5, 2, 2.5, 3 | D / C |
| V-E4 | `protection` : `damage_protection` `add` `linear` 1.0 + 1.0 (L-1), condition `damage_source_properties` : étiquette `#minecraft:bypasses_invulnerability` fausse | 1, 2, 3, 4 | D / C |
| V-E5 | `unbreaking` (R23) | armure 0.2, 0.2667, 0.3 ; autres 0.5, 0.6667, 0.75 | C |
| V-E6 | `efficiency` | 2, 5, 10, 17, 26 | C |
| V-E7 | `wind_burst` `knockback_multiplier` `lookup` [1.2, 1.75, 2.2], `fallback` linéaire 1.5 + 0.35 (L-1) | L=1 -> 1.2, L=2 -> 1.75, L=3 -> 2.2, L=4 -> 2.55 (hors liste : 1.5 + 0.35 x 3) | D / C |
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

## Plan de tests

- **Unitaire (L1)** : décodage des 43 enchantements, 7 fournisseurs, 21 étiquettes `enchantable`, 7 ensembles exclusifs ; V-E1 à V-E10 ; évaluation de chaque valeur dépendante du niveau (R20, R21) pour L = 1 à `max_level`, comparée aux tables de R22 à 6 chiffres ; propriétés : exclusion symétrique à l'intérieur d'un ensemble, `lookup` hors liste, constantes.
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
| PE8 | Zombie armé d'une épée Sharpness cible un armor stand n'est pas applicable : utiliser le client protocole avec un joueur attaquant un zombie sans IA, épées Sharpness I à V, Smite, Bane, Knockback, Fire Aspect | Dégâts additionnels, knockback, durée du feu | R14, R16, R31 |
| PE9 | Coûts de ligne (PE3) mesurés pour 0 à 15 étagères et `XpSeed` fixe ; deux lectures par graine | Coûts déterministes pour une graine donnée | R37 |
| PE10 | Pack `sonde` avec enchantements de test `sonde:t1` à `sonde:tn` portant `damage_protection` avec des listes d'entrées combinant `add`, `multiply`, `set`, `all_of`, `linear`, `fraction`, `clamped`, `lookup` (dont niveau hors liste), `levels_squared`, `exponent`, `exponential`, ordre des entrées permuté ; zombie sans IA en armure `sonde:tN` ; `damage` 10 puis lecture de `Health` ; variantes `requirements` avec `enchantment_level` et `random_chance` | Valeur finale en fonction de l'ordre ; sémantique des effets de valeur et des valeurs dépendantes du niveau | R13, R15, R17, R18, R19, R21, R25 |
| PE11 | Pack `sonde` : enchantement `attributes` et `location_changed` avec effet `attribute` et `replace_disk` sur des bottes de zombie sans IA ; `/tick step` et déplacement par `tp` ; lecture de `attributes` de l'entité | Modificateurs d'attribut créés, retirés ; déclencheurs de `location_changed` | R16, R27, R29, R33 |
| PE12 | Observation des équipements de zombies générés avec `enchantment_provider` (spawn naturel, raid) sur 10000 entités ; dépend du jalon M7 | Distribution des coûts et enchantements du fournisseur | R34 |

## Couverture des règles S par les sondes

R5 (usage des coûts) : PE1, PE3 ; R6 (emplacements actifs) : PE5, PE8 ; R13, R15, R17, R18, R19, R21, R25 : PE10 ; R16, R27, R29, R33 : PE11 ; R34 : PE12 ; R31 et R32 : PE5, PE8. Aucune autre règle S n'est sans sonde.

## Questions ouvertes

- Probabilité de continuation et boucle de sélection de la table d'enchantement (R39) : la page publique est garbée ; PE1 est la sonde décisive (elle teste `enchant_with_levels`, que la documentation dit équivalent).
- Valeur de départ et consommation exacte de chaque composant d'effet (R19) : dépendent des formules de combat de M7.
- Symétrie de l'exclusion (R8) et ordre des enchantements d'un objet (R31).
- Ordre d'application entre pièces d'armure (R31).
- `exponential` (effet de valeur) et `exponent` (valeur dépendante du niveau) n'ont aucun usage vanilla : sémantique incertaine.
- Formule de la meule, de l'enclume et du coût d'expérience (R41 à R44) : à lever avec un client protocole (PE2 à PE4).
- Lien entre `anvil_cost` et le multiplicateur de livre (R43).
- Règles S restantes : R6, R10 (plage), R13 à R19, R21, R25, R27, R31 à R44.

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Enchantements | M6 | A | spécifié | `docs/specs/item-enchantments.md` (43 définitions, effets, valeurs par niveau en D/C ; sélection, enclume, meule en S) |

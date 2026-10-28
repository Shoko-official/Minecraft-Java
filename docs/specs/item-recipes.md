# Recettes, cuisson, carburants et alchimie

Statut : brouillon
Jalon : M6
Risque : B
Règles : 55 (R1 à R55)

## Périmètre

Couvre, pour `engine/item` (données et règles sans monde, ADR 0004) et les comportements de conteneurs que `engine/game` y branche (ADR 0005) :

- le format des 2042 recettes de la version cible et leurs 22 types (sérialiseurs) ;
- les ingrédients (identifiant, étiquette, liste) et les piles de résultat ;
- la reconnaissance d'une grille (forme, sans forme), le calcul du résultat, la consommation, les objets restants ;
- la cuisson (four, haut fourneau, fumoir, feu de camp), la forge (transformation, ornement), le tailleur de pierre, l'alchimie pilotée par données ;
- les recettes spéciales (douze comportements qui dépendent du contenu des piles) ;
- les carburants, le compostage, le carburant d'alchimie, tels que décrits par les composants d'objets ;
- le livre de recettes : déblocage, commandes `recipe`, règle de fabrication limitée ;
- l'ordre de résolution quand plusieurs recettes conviennent.

Ne couvre pas :

- les composants d'objets eux-mêmes et les menus (`item-components-inventory.md`) ;
- les échanges de villageois (`villager_trade`, `trade_set`) : jalon entités ;
- l'affichage client des recettes (`recipe_display`, `slot_display`, paquets) : jalon M8 ;
- les durées d'animation, sons et particules des machines.

## Sources

- **Données extraites** (`.cache/reference/26.3/`) :
  - `resources/data/minecraft/recipe/**/*.json` : 2042 fichiers ; `recipe/brewing/**` : 279 ; les identifiants de recette sont les chemins sans extension (`acacia_boat`, `brewing/potion_awkward_blaze_powder`) ;
  - `resources/data/minecraft/advancement/recipes/**` : 1740 avancements de déblocage ;
  - `generated/reports/registries.json` : `recipe_type` (8), `recipe_serializer` (22), `recipe_book_category` (13), `recipe_display` (5), `slot_display` (11) ;
  - `generated/reports/minecraft/components/item/*.json` : composants `use_remainder`, `cooking_fuel`, `compostable`, `brewing_fuel` ;
  - `resources/data/minecraft/context_int_provider/{cooking,compostable,brewing}/**`, `context_float_provider/**`, `predicate/block/fast_cooking.json` ;
  - `resources/data/minecraft/tags/item/**` (étiquettes citées par les recettes) ;
  - `generated/reports/commands.json` : commande `recipe`, gamerule `limited_crafting` (via `registries.json`, `game_rule`) ;
  - structures `.nbt` : block entities `furnace`, `blast_furnace`, `smoker`, `brewing_stand`, `campfire`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Item modifier", "Loot table" (étiquettes d'objets), "Inventory", "Java Edition protocol/Inventory".
- **Rapports locaux** (indices, ADR 0002) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md` chapitres 125 à 133 (recettes, cuisson, carburants, alchimie, compostage). Le rapport pose la liste des comportements à reproduire sans les décrire ; les valeurs de ses tableaux (durées de carburant) sont recalculées ici à partir des données.
- **Sondes de l'oracle** : aucune exécutée. Les sondes sont en fin de document (PR1 à PR12).

Convention de preuve : D valeur lue dans un fichier extrait ; C calcul reproductible sur ces fichiers ; M observé sur l'oracle (non utilisé) ; S documentation publique seule ou hypothèse. Chaque règle S porte un comportement **provisoire**.

## Règles

### Chargement et identification

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Une recette est un fichier JSON `data/<espace>/recipe/<chemin>.json`, d'identifiant `<espace>:<chemin>` (sous-dossiers permis : `brewing/`). Le champ `type` est un identifiant du registre `recipe_serializer` (22 entrées). | D |
| R2 | La version cible compte 2042 recettes réparties sur 22 sérialiseurs (tableau de R3). Les 8 types de recette (`recipe_type` : `crafting`, `smelting`, `blasting`, `smoking`, `campfire_cooking`, `stonecutting`, `smithing`, `brewing`) regroupent ces sérialiseurs par machine. | D |
| R3 | Dénombrement et champs par sérialiseur (chiffre entre parenthèses : nombre de fichiers qui portent le champ, quand il est inférieur au total). | D |

| Sérialiseur | Fichiers | Champs observés |
|---|---|---|
| `crafting_shaped` | 827 | `category`(534), `group`(413), `key`, `pattern`, `result`, `show_notification`(1) |
| `crafting_shapeless` | 375 | `category`(187), `group`(316), `ingredients`, `result` |
| `stonecutting` | 351 | `ingredient`, `result` |
| `brewing` | 279 | `input`, `output`, `reagent` |
| `smelting` | 73 | `category`(43), `cookingtime`, `experience`, `group`(20), `ingredient`, `result` |
| `crafting_transmute` | 33 | `add_material_count_to_result`(1), `category`(16), `group`, `input`, `material`, `material_count`(1), `result` |
| `blasting` | 25 | `category`(2), `cookingtime`, `experience`, `group`(20), `ingredient`, `result` |
| `smithing_trim` | 18 | `addition`, `base`, `pattern`, `template` |
| `crafting_special_bannerduplicate` | 16 | `banner`, `result` |
| `smithing_transform` | 12 | `addition`, `base`, `result`, `template` |
| `campfire_cooking` | 9 | `category`, `cookingtime`, `experience`, `ingredient`, `result` |
| `smoking` | 9 | `category`, `cookingtime`, `experience`, `ingredient`, `result` |
| `crafting_dye` | 6 | `dye`, `group`, `result`, `target` |
| `crafting_special_bookcloning` | 1 | `material`, `result`, `source` |
| `crafting_decorated_pot` | 1 | `back`, `front`, `left`, `result`, `right` |
| `crafting_special_firework_rocket` | 1 | `fuel`, `result`, `shell`, `star` |
| `crafting_special_firework_star` | 1 | `dye`, `fuel`, `result`, `shapes`, `trail`, `twinkle` |
| `crafting_special_firework_star_fade` | 1 | `dye`, `result`, `target` |
| `crafting_special_mapextending` | 1 | `map`, `material`, `result` |
| `crafting_special_repairitem` | 1 |  |
| `crafting_special_shielddecoration` | 1 | `banner`, `result`, `target` |
| `crafting_imbue` | 1 | `material`, `result`, `source` |

| Réf | Règle | Preuve |
|---|---|---|
| R4 | Association sérialiseur -> type de recette (machine) : `crafting_*` (shaped, shapeless, transmute, decorated_pot, dye, imbue, special_*) -> `crafting` ; `smelting` ; `blasting` ; `smoking` ; `campfire_cooking` ; `stonecutting` ; `smithing_transform` et `smithing_trim` -> `smithing` ; `brewing` -> `brewing`. | S |
| R5 | Un pack de données peut remplacer une recette par son identifiant et en ajouter. La résolution d'empilement des packs est celle de `data-datapack-loading.md`. Les recettes sont chargées au démarrage et à `/reload`. | S |
| R6 | Champ commun `group` (chaîne, optionnel, 113 valeurs distinctes ; absent signifie pas de groupe, provisoire chaîne vide) : regroupe des recettes dans le livre de recettes, sans effet sur la fabrication. | D (présence) ; S (défaut) |
| R7 | Champ `category` (chaîne optionnelle, rangement du livre de recettes). Valeurs observées : fabrication `building` (493), `redstone` (120), `equipment` (124, dont 16 `crafting_transmute`) ; cuisson `food` (27 : 9 fonte, 9 fumoir, 9 feu de camp), `blocks` (36 : 34 fonte, 2 haut fourneau) ; champ absent pour 293 recettes à forme, 188 sans forme, 17 `crafting_transmute`, 30 `smelting`, 23 `blasting` et tous les autres types. Absent signifie la catégorie `misc` (provisoire). Les 13 catégories de livre (`recipe_book_category`) sont : `crafting_building_blocks`, `crafting_redstone`, `crafting_equipment`, `crafting_misc`, `furnace_food`, `furnace_blocks`, `furnace_misc`, `blast_furnace_blocks`, `blast_furnace_misc`, `smoker_food`, `stonecutter`, `smithing`, `campfire`. La correspondance (type, `category`) -> catégorie de livre est S. | D (valeurs) ; S (correspondance) |
| R8 | Champ `show_notification` : une seule recette le porte (`crafting_table`, valeur `false`). Défaut `true` (provisoire) : le déblocage de la recette affiche une notification côté client. | D (présence) ; S (défaut) |

### Ingrédients et résultats

| Réf | Règle | Preuve |
|---|---|---|
| R9 | Un ingrédient de fabrication, de cuisson, de tailleur de pierre ou de forge est une chaîne : un identifiant d'objet (`minecraft:potato`), une étiquette d'objet précédée de `#` (`#minecraft:planks`), ou une liste d'identifiants d'objets. Observé : identifiant (1107 clés de recettes à forme, 744 ingrédients sans forme, 351 tailleur de pierre, 67 fonte, 22 haut fourneau, 9 fumoir, 9 feu de camp), étiquette (86 clés de formes, 50 ingrédients sans forme, 33 `input` de transmutation, 3 fonte, et les champs de forge et de recettes spéciales), liste d'identifiants (13 clés de formes, 113 ingrédients sans forme, 3 fonte, 3 haut fourneau). Aucune liste ne contient d'étiquette. Provisoire : une liste est acceptée si tous ses éléments sont des identifiants ; une liste vide est refusée. | D (formes) ; S (refus) |
| R10 | Ingrédient d'alchimie : objet `{item, potion_contents?}` pour `input` (`item` identifiant, `potion_contents` prédicat `{potions: <id de potion>}`) et pour `reagent` (`{item}`), `output` pile `{id, components?}`. Les 279 recettes d'alchimie ont toutes ces trois champs. Le champ `potion_contents` de `input` désigne le contenu de potion du composant du même nom (`potions` : identifiant de potion, `awkward` etc.). | D |
| R11 | Pile de résultat : `{id, count?, components?}` (voir `item-components-inventory.md` R28). `count` absent vaut 1. Le résultat reçoit les composants par défaut de l'objet plus les `components` du fichier (exemple : `suspicious_stew_from_allium` donne un ragoût avec `suspicious_stew_effects` `[{duration: 60, id: fire_resistance}]`). Les résultats peuvent dépasser 1 (`acacia_door` 3, `tipped_arrow` 8, `firework_rocket` 3). | D |
| R12 | Un résultat `{}` (objet vide) apparaît sur 2 recettes (`map_cloning`, `map_extending`) : le résultat est dérivé de la grille, pas de la donnée (R35). Une recette `crafting_special_repairitem` n'a ni ingrédient ni résultat. Pour `smithing_trim`, le résultat est dérivé de la base. | D (présence) ; S (sens) |
| R13 | Les étiquettes citées sont résolues à l'appel (contenu au moment du rechargement). Une étiquette absente est une erreur de chargement de la recette (la recette est ignorée, les autres se chargent). Provisoire. Voir `data-datapack-loading.md` pour la résolution et les cycles. | S |

### Recettes à forme (crafting_shaped)

| Réf | Règle | Preuve |
|---|---|---|
| R14 | Champs : `pattern` (liste de 1 à 3 chaînes), `key` (table caractère -> ingrédient), `result`. Observé sur les 827 recettes : toutes les lignes d'un motif ont la même longueur ; largeur 1 à 3 et hauteur 1 à 3 ; dimensions (hauteur x largeur) : 3x3 347, 2x3 161, 1x3 121, 2x2 83, 1x2 35, 3x2 30, 2x1 29, 3x1 21 ; nombre de clés : 1 (519), 2 (241), 3 (63), 4 (4). L'espace dénote une case vide. | D |
| R15 | Tout caractère du motif autre que l'espace est une clé de `key` ; une clé non utilisée ou un caractère sans clé est une erreur de chargement. Un motif dont toutes les cases sont vides est refusé. Provisoire. | S |
| R16 | Reconnaissance : la recette est reconnue si la grille de fabrication contient le motif à une position quelconque (translation entière) et si toutes les cases hors du rectangle du motif sont vides. Le motif est de plus reconnu en miroir horizontal (symétrie gauche-droite). Une grille 2x2 (fenêtre joueur) ne reconnaît que les motifs de largeur et hauteur au plus 2. Provisoire. Sonde PR1 (grille 3x3 du fabricateur). | S |
| R17 | Chaque case non vide est testée contre l'ingrédient de la clé : un identifiant d'objet exige cet objet ; une étiquette exige un objet de l'étiquette ; une liste exige l'un de ses éléments. Les composants de la pile d'entrée ne sont pas testés (provisoire), sauf pour les recettes spéciales. | S |

### Recettes sans forme (crafting_shapeless)

| Réf | Règle | Preuve |
|---|---|---|
| R18 | Champs : `ingredients` (liste), `result`. Tailles de la liste observées sur les 375 recettes : 1 (86), 2 (232), 3 (7), 4 (19), 5 (4), 7 (1), 8 (1), 9 (25). Maximum 9 (la grille). | D |
| R19 | Reconnaissance : le multi-ensemble des piles non vides de la grille doit correspondre un à un aux ingrédients (une pile satisfait un seul ingrédient, un ingrédient est satisfait par une seule pile) ; l'ordre et les positions sont indifférents. Une grille avec plus ou moins de piles non vides que d'ingrédients ne correspond pas. Provisoire. Sonde PR1. | S |
| R20 | Appariement quand un objet convient à plusieurs ingrédients (étiquette plus identifiant) : la recette est reconnue s'il existe une affectation complète (le résultat ne dépend pas de l'ordre d'essai). Provisoire. Sonde PR2. | S |

### Résultat, consommation, objets restants

| Réf | Règle | Preuve |
|---|---|---|
| R21 | Pour les recettes de fabrication ordinaires (à forme, sans forme), le résultat est une copie de la pile du fichier (R11) ; il ne dépend pas des composants des entrées. | S |
| R22 | Consommation : fabriquer retire une unité de chaque pile d'entrée non vide de la grille. Un objet de grille portant le composant `use_remainder` est remplacé par ce reste (D, 7 objets : `beetroot_soup`, `mushroom_stew`, `rabbit_stew`, `suspicious_stew` rendent `bowl` ; `honey_bottle` et `potion` rendent `glass_bottle` ; `milk_bucket` rend `bucket`). Aucune autre valeur de reste n'est dans les données de fabrication : pour tout autre objet de grille le reste est vide. Provisoire pour le reste d'objets qui ne sont pas dans `use_remainder` (seau de lave, etc.) ; voir Questions ouvertes. | D (liste) ; S (usage en fabrication) |
| R23 | Un reste est rendu dans la case d'origine si elle est vide après consommation, sinon à l'inventaire, sinon jeté au sol. Provisoire. Sonde PR3. | S |
| R24 | Le résultat d'une recette ne contient jamais plus de `max_stack_size` unités ; une recette dont le `count` dépasse la taille de pile est une erreur de chargement. Provisoire. | S |

### Cuisson

| Réf | Règle | Preuve |
|---|---|---|
| R25 | `smelting` (73), `blasting` (25), `smoking` (9), `campfire_cooking` (9) portent `ingredient`, `result`, `cookingtime` (entier, ticks), `experience` (flottant). `cookingtime` vaut 200 pour les 107 recettes de four, haut fourneau et fumoir, 600 pour les 9 recettes de feu de camp. Valeurs d'`experience` observées : 0.1 (40 fonte, 5 haut fourneau, 1 fumoir, 1 feu de camp), 0.15, 0.2, 0.3, 0.35 (9 fonte, 8 fumoir, 8 feu de camp), 0.7 (8 fonte, 8 haut fourneau), 1.0 (9 fonte, 8 haut fourneau), 2.0 (1 fonte, 1 haut fourneau). | D |
| R26 | Le haut fourneau et le fumoir ne divisent pas `cookingtime` dans les données : ils sont plus rapides par une vitesse. Le fournisseur `cooking/speed_default` (flottant contextuel) vaut `cooking/fast_speed_multiplier` (2.0) si le prédicat `block/fast_cooking` est vrai, `cooking/normal_speed_multiplier` (1.0) sinon ; `block/fast_cooking` est un `match_block` sur `smoker` ou `blast_furnace`. La recette est lue dans un four d'un type donné ; une recette `smelting` n'est pas lue par un haut fourneau (provisoire). | D (fournisseurs) ; S (usage) |
| R27 | Progression : par tick de combustion, `cooking_time_spent` augmente de la vitesse (1 ou 2) ; la cuisson se termine quand `cooking_time_spent` atteint `cooking_total_time` (initialement `cookingtime`). Provisoire : arrondi de la progression flottante vers un entier, et ce que devient `cooking_time_spent` quand la sortie est bloquée ou quand l'entrée change sont S. Sondes PR6, PR7. | S |
| R28 | Un tick sans combustible allumé diminue `cooking_time_spent` (régression). Provisoire : de 2 par tick, jusqu'à 0. Sonde PR6. | S |
| R29 | Un four consomme une unité de l'ingrédient et ajoute `count` unités du résultat à la sortie (même contenu ou sortie vide) ; la fonte est bloquée si la sortie contient un contenu différent ou est pleine. L'expérience cumulée est stockée dans `RecipesUsed` (identifiant de recette -> nombre de fontes), provisoire : le joueur la reçoit à la reprise du résultat, valeur `experience` x nombre, arrondie avec un tirage de la partie fractionnaire. Sonde PR8. | S |
| R30 | NBT d'un four (D, `item-components-inventory.md` R31) : `lit_time_remaining`, `lit_total_time`, `cooking_time_spent`, `cooking_total_time` (Short), `RecipesUsed`, `Items` (0 ingrédient, 1 combustible, 2 sortie). Un feu de camp porte `CookingTimes` et `CookingTotalTimes` (Int_Array de 4, une cuisson par emplacement). | D |
| R31 | La recherche de recette de cuisson est par objet d'entrée (identifiant, étiquette, liste) : plusieurs recettes peuvent partager un ingrédient (les outils en cuivre sont dans une liste de 3 recettes `smelting`, 3 `blasting`). Quand plusieurs recettes d'un même type conviennent à un ingrédient, la sélection est S (voir R50). | D (recouvrement) ; S (sélection) |

### Carburants et compostage

| Réf | Règle | Preuve |
|---|---|---|
| R32 | Les carburants sont des données portées par le composant `cooking_fuel` `{burn_time, speed_multiplier}` : 347 objets, 18 valeurs distinctes de `burn_time` (références à des fournisseurs entiers `cooking/time_*`) et une seule de `speed_multiplier` (`cooking/speed_default`). Chaque fournisseur `cooking/time_*` est `{type: div, left: <durée>, right: conditional(block/fast_cooking : 2 sinon 1)}` ; la division est entière. Durées (C) : tableau ci-dessous ; objets par fournisseur et durée normale (`left` / 1) et rapide (`left` / 2, division entière). Hypothèse S : la division entière arrondit vers le bas (4001 -> 2000, 67 -> 33). Sonde PR9. | D / C / S |

| Fournisseur | Multiplicateur de vitesse | Objets | Durée de base | Normale | Rapide | Exemples |
|---|---|---|---|---|---|---|
| `cooking/time_bamboo` | `cooking/speed_default` | 2 | 50 | 50 | 25 | bamboo, scaffolding |
| `cooking/time_blaze_rod` | `cooking/speed_default` | 1 | 2400 | 2400 | 1200 | blaze_rod |
| `cooking/time_boats` | `cooking/speed_default` | 22 | 1200 | 1200 | 600 | acacia_boat, acacia_chest_boat, bamboo_chest_raft, bamboo_raft... |
| `cooking/time_coal` | `cooking/speed_default` | 2 | 1600 | 1600 | 800 | charcoal, coal |
| `cooking/time_coal_block` | `cooking/speed_default` | 1 | 16000 | 16000 | 8000 | coal_block |
| `cooking/time_dried_kelp_block` | `cooking/speed_default` | 1 | 4001 | 4001 | 2000 | dried_kelp_block |
| `cooking/time_dry_plants` | `cooking/speed_default` | 17 | 100 | 100 | 50 | acacia_sapling, azalea, birch_sapling, cherry_sapling... |
| `cooking/time_hanging_signs` | `cooking/speed_default` | 11 | 800 | 800 | 400 | acacia_hanging_sign, bamboo_hanging_sign, birch_hanging_sign, cherry_hanging_sign... |
| `cooking/time_lava_bucket` | `cooking/speed_default` | 1 | 20000 | 20000 | 10000 | lava_bucket |
| `cooking/time_roots` | `cooking/speed_default` | 1 | 300 | 300 | 150 | mangrove_roots |
| `cooking/time_wood_blocks` | `cooking/speed_default` | 153 | 300 | 300 | 150 | acacia_fence, acacia_fence_gate, acacia_log, acacia_planks... |
| `cooking/time_wood_items_extra_small` | `cooking/speed_default` | 12 | 100 | 100 | 50 | acacia_button, bamboo_button, birch_button, bowl... |
| `cooking/time_wood_items_large` | `cooking/speed_default` | 28 | 200 | 200 | 100 | acacia_door, acacia_sign, bamboo_door, bamboo_sign... |
| `cooking/time_wood_items_small` | `cooking/speed_default` | 3 | 300 | 300 | 150 | bow, crossbow, fishing_rod |
| `cooking/time_wood_slabs` | `cooking/speed_default` | 12 | 150 | 150 | 75 | acacia_slab, bamboo_mosaic_slab, bamboo_slab, birch_slab... |
| `cooking/time_wool` | `cooking/speed_default` | 32 | 100 | 100 | 50 | black_wool, black_wool_stairs, blue_wool, blue_wool_stairs... |
| `cooking/time_wool_carpets` | `cooking/speed_default` | 16 | 67 | 67 | 33 | black_carpet, blue_carpet, brown_carpet, cyan_carpet... |
| `cooking/time_wool_slabs` | `cooking/speed_default` | 32 | 50 | 50 | 25 | black_cushion, black_wool_slab, blue_cushion, blue_wool_slab... |

| Réf | Règle | Preuve |
|---|---|---|
| R33 | Le seau de lave est un carburant de 20 000 ticks (10 000 en rapide). Il n'a pas de composant `use_remainder` ; le seau vide rendu après combustion est un comportement du four (S). Sonde PR9. | D / S |
| R34 | Compostage : le composant `compostable` `{layers}` fait référence à un fournisseur entier `compostable/{low, low_medium, medium, medium_high, always_add_one}` (objets : 50, 11, 47, 12, 2). Chaque fournisseur probabiliste vaut 1 si le composteur est au niveau 0 (`match_block composter` état `level` "0"), sinon une loi pondérée `1` (poids 30, 50, 65, 85 pour `low`, `low_medium`, `medium`, `medium_high`) contre `0` (poids 70, 50, 35, 15) ; `always_add_one` vaut 1. La valeur est le nombre de couches ajoutées. La sémantique (niveau maximal, récolte, entonnoirs) est S. Sonde PR10. | D (fournisseurs) ; S (usage) |
| R35 | Alambic : le composant `brewing_fuel` de `blaze_powder` `{speed_multiplier: brewing/speed_default (1.0), uses: brewing/uses_default (20)}` : un objet-carburant donne 20 utilisations à l'alambic (`Fuel` Byte, 0 à 20) et vitesse 1.0. | D (valeurs) ; S (sens) |

### Forge et tailleur de pierre

| Réf | Règle | Preuve |
|---|---|---|
| R36 | `smithing_transform` (12) : `template`, `base`, `addition` (ingrédients), `result` ; observé : `template` identifiant, `base` identifiant d'outil ou d'armure en diamant, `addition` étiquette `#minecraft:netherite_tool_materials` (lingot de netherite). Le résultat reprend les composants de la pile `base` (provisoire : tous, durabilité comprise). | D (forme) ; S (copie de composants) |
| R37 | `smithing_trim` (18) : `template` identifiant de modèle d'ornement, `base` étiquette `#minecraft:trimmable_armor`, `addition` étiquette `#minecraft:trim_materials` (11 matériaux), `pattern` identifiant de motif. Le résultat est la base avec le composant `trim` `{material, pattern}` (le matériau vient de l'`addition` via `provides_trim_material` D). Sonde PR5. | D / S |
| R38 | `stonecutting` (351) : `ingredient` identifiant, `result` pile. Le tailleur de pierre propose toutes les recettes dont l'ingrédient correspond à l'objet d'entrée ; l'ordre de proposition est S. Chaque retrait consomme une unité. Sonde PR5. | D (forme) ; S (usage) |

### Alchimie pilotée par données

| Réf | Règle | Preuve |
|---|---|---|
| R39 | Les 279 recettes `brewing` ont `input.item` parmi `potion` (108), `splash_potion` (108), `lingering_potion` (63), et pour sortie `lingering_potion` (108), `splash_potion` (108), `potion` (63). Réactifs (nombre de recettes) : `gunpowder` 45, `dragon_breath` 45, `redstone` 42, `fermented_spider_eye` 36, `glowstone_dust` 30, et 6 chacun pour `blaze_powder`, `breeze_rod`, `cobweb`, `ghast_tear`, `glistering_melon_slice`, `magma_cream`, `rabbit_foot`, `slime_block`, `spider_eye`, `stone`, `sugar`, et 3 chacun pour `golden_carrot`, `phantom_membrane`, `pufferfish`, `turtle_helmet`, `nether_wart`. | C |
| R40 | Une recette d'alchimie s'applique à une bouteille (slots 0 à 2) quand l'objet et son `potion_contents.potions` correspondent à `input` et que le réactif (slot 3) correspond à `reagent.item`. La sortie remplace la bouteille : id de `output` et composants de `output.components`. Le contenu de potion de l'entrée ne survit pas (provisoire : seuls les composants de `output` sont posés ; les composants de l'entrée non remplacés sont copiés). Sonde PR4. | S |
| R41 | L'infusion complète (réactif consommé une fois, 400 ticks de préparation, `BrewTime` Short, carburant 1 par infusion, 3 bouteilles traitées en même temps) est S. Le bonus d'infusion gunpowder (potion -> splash) et dragon_breath (splash -> lingering) est entièrement exprimé par les données (recettes `brewing/*_gunpowder` et `_dragon_breath` : 45 chacune). Sonde PR4. | S |
| R42 | L'étiquette `brewing_potion_inputs` (4 objets : `potion`, `splash_potion`, `lingering_potion`, `glass_bottle`) liste les objets admis dans les slots de bouteille. Provisoire : le slot de bouteille n'accepte que ces objets. | D / S |

### Recettes spéciales

Les comportements ci-dessous sont des invariants observables : entrées, sorties, restes. Le détail des algorithmes n'est pas dans les données : tout est S, avec une sonde PR1 (fabricateur) pour chacun.

| Réf | Règle | Preuve |
|---|---|---|
| R43 | `crafting_special_bannerduplicate` (16, un par couleur, champ `banner`) : une bannière de la couleur du champ portant des motifs plus une bannière de même couleur sans motif donnent une copie de la première (motifs identiques), la bannière à copier est rendue ; limite de six motifs. | S |
| R44 | `crafting_special_bookcloning` (`source` `written_book`, `material` étiquette `book_cloning_target` = `writable_book`) : un livre signé et n livres vierges donnent n copies du livre avec génération augmentée de 1 ; un livre de génération 2 ou plus ne se copie pas ; le livre source est rendu. | S |
| R45 | `crafting_special_firework_rocket` (`shell` papier, `fuel` poudre à canon, `star` étoile de feu d'artifice, `result` count 3) : papier + 1 à 3 poudres + 0 à 7 étoiles ; la durée de vol vaut le nombre de poudres ; les étoiles deviennent les explosions de la fusée. | S |
| R46 | `crafting_special_firework_star` (`fuel`, `dye` étiquette `dyes`, `shapes` {burst: plume, creeper: étiquette `skulls`, large_ball: boule de feu, star: pépite d'or}, `trail` diamant, `twinkle` poudre de glowstone) : poudre à canon + 1 à n teintures + au plus une forme + options trail, twinkle ; forme par défaut petite boule. | S |
| R47 | `crafting_special_firework_star_fade` (`target` étoile, `dye`) : une étoile + des teintures donnent une étoile dont les couleurs de fondu sont celles des teintures. | S |
| R48 | `crafting_special_mapextending` (`map` étiquette `extendable_maps` = `filled_map`, `material` papier, résultat `{}`) : une carte entourée de 8 papiers donne la même carte avec zoom augmenté de 1 (limite 4). | S |
| R49 | `crafting_special_repairitem` : deux objets à durabilité de même type, abîmés, donnent un objet réparé dont la durabilité restante vaut la somme des durabilités restantes plus 5 pour cent de `max_damage` (valeur de mémoire de la documentation publique, à confirmer par PR1), plafonnée ; les enchantements sont perdus ; condition de non-fusion S. `crafting_special_shielddecoration` (`target` bouclier, `banner` étiquette `banners`) : bouclier + bannière donnent le bouclier avec les motifs et la couleur de la bannière. `crafting_decorated_pot` (`front`, `back`, `left`, `right` étiquette `decorated_pot_ingredients`) : 4 éléments en croix donnent un pot décoré dont `pot_decorations` suit les 4 éléments. `crafting_dye` (6, `target` pièce de cuir, `dye` étiquette `dyes`) : une pièce plus n teintures donnent la pièce avec `dyed_color` mélangé. `crafting_imbue` (`source` potion persistante, `material` flèche, résultat 8) : 8 flèches autour d'une potion persistante donnent 8 flèches avec `potion_contents` de la potion (durée / 8). `crafting_transmute` (33, `input` objet ou étiquette, `material`, `result`) : le résultat prend l'objet du `result` et les composants de la pile `input` (exemple : boîte de shulker colorée garde son contenu) ; `material_count` (intervalle 1 à 8) et `add_material_count_to_result` pour le clonage de cartes (`map_cloning` : carte + 1 à 8 cartes vierges donnent n+1 cartes). | S |

### Livre de recettes

| Réf | Règle | Preuve |
|---|---|---|
| R50 | Déblocage : 1740 fichiers sous `advancement/recipes/` : la racine `recipes/root` (critère `impossible`) et 1739 avancements de déblocage, tous de parent `minecraft:recipes/root`, qui débloquent 1739 recettes distinctes (303 recettes n'ont aucun avancement, dont les 279 d'alchimie). Chaque avancement a un critère `has_the_recipe` (déclencheur `minecraft:recipe_unlocked`, `conditions.recipes` = identifiant de la recette) et au moins un critère de possession (`inventory_changed` avec la liste d'objets, 1885 critères ; `enter_block` 11 ; `tick` 1), `requirements` = une seule liste (OU) de tous les critères (2 critères pour 1659 avancements, jusqu'à 17), et `rewards.recipes` = `[identifiant]`. Exemple `blaze_powder` : critère `has_blaze_rod`. | D |
| R51 | Les recettes sont débloquées quand l'avancement est obtenu (récompense) ou par `/recipe give <joueurs> *` ou `/recipe give <joueurs> <recette>` ; `/recipe take` les retire (D, syntaxe de `commands.json` : `recipe give` et `recipe take` avec `<targets>` puis `*` ou `<recipe>` (resource_key du registre recipe)). Le joueur reçoit l'état dans ses données (`recipeBook`) : forme S (sonde PR11). | D (syntaxe) ; S (reste) |
| R52 | La règle de jeu `limited_crafting` (booléen, défaut `false`, D registre `game_rule`) : quand elle est vraie, seules les recettes débloquées sont fabricables. Provisoire. Sonde PR11. | D (existence, défaut) ; S (effet) |
| R53 | Les recettes de brasserie, de fonte et de forge sont aussi dans le livre, selon `recipe_book_category`. `recipe_display` (5 types : `crafting_shapeless`, `crafting_shaped`, `furnace`, `stonecutter`, `smithing`) et `slot_display` (11 types) décrivent l'affichage client : jalon M8. | D (registres) |

### Résolution entre plusieurs recettes

| Réf | Règle | Preuve |
|---|---|---|
| R54 | Quand plusieurs recettes d'un même type de machine conviennent à la même entrée, une seule est retenue. Hypothèse H-ord-a : la première dans l'ordre de chargement (par identifiant de recette, ordre alphabétique des chemins puis empilement des packs). Hypothèse H-ord-b : la dernière chargée l'emporte. Hypothèse H-ord-c : ordre d'une table de hachage. Provisoire : H-ord-a (à départager par PR2). Les recettes vanilla n'ont pas de conflit connu en fabrication (à vérifier par la commande de la section Vecteurs : recherche d'entrées dupliquées) ; la fonte a des conflits connus (listes qui recouvrent des recettes individuelles, R31). | S |
| R55 | Une recette remplacée par un pack (même identifiant, priorité supérieure) occupe la place de l'originale ; le rechargement ne change pas l'ordre. Provisoire. | S |

## Entrées / sorties

- **Recette** : (identifiant, type, ingrédients, résultat, métadonnées de livre, paramètres de machine).
- **Fabrication** : entrée = grille de piles (2x2 ou 3x3, dimensions) ; sortie = (pile de résultat, liste de restes, recette retenue) ou aucune.
- **Cuisson** : entrée = (machine, pile d'entrée) ; sortie = (recette, durée en ticks, expérience).
- **Alchimie** : entrée = (bouteille, réactif) ; sortie = pile de bouteille transformée.
- **Livre** : entrée = événements d'avancement et commandes `recipe` ; sortie = ensemble de recettes débloquées par joueur.

## Cas limites

- Motif à 1 cellule ; motif plus grand que la grille 2x2 (non reconnu dans la fenêtre joueur) ; motif décalé dans le coin ; motif en miroir ; ingrédients superflus hors du rectangle (refus).
- Sans forme : doublons d'ingrédient ; objet qui convient à deux ingrédients ; grille avec une seule pile ; 9 piles.
- Étiquette vide ; liste d'ingrédients vide ; étiquette absente (recette ignorée).
- Résultat de `count` supérieur à la taille de pile ; reste d'un objet qui n'est pas empilé ; reste identique à l'objet de grille.
- Fusion de deux recettes de fonte pour un ingrédient (liste et identifiant seul).
- `cookingtime` 0 ou négatif (donnée de pack) ; `experience` négative.
- Division entière des durées impaires (4001, 67) ; durée de combustible en rapide non entière.
- Alambic : bouteille vide dans un slot, potion sans recette, réactif sans recette, carburant épuisé en cours d'infusion.
- Déblocage : recette inconnue, recette déjà débloquée, `/recipe give * ` avec le jeu de recettes complet.

## Vecteurs de test

| Vecteur | Entrée | Sortie attendue | Preuve |
|---|---|---|---|
| V-R1 | Dénombrement du dossier `recipe/` | 2042 fichiers, 22 types, répartition de R3 | D |
| V-R2 | `recipe/acacia_boat.json` | motif `["# #", "###"]`, clé `#` = `minecraft:acacia_planks`, résultat `acacia_boat` x1, groupe `boat` | D |
| V-R3 | `recipe/acacia_door.json` | motif `["##","##","##"]`, catégorie `redstone`, groupe `wooden_door`, résultat x3 | D |
| V-R4 | `recipe/crafting_table.json` | `show_notification` false, motif 2x2 de `#minecraft:planks` | D |
| V-R5 | `recipe/acacia_planks.json` (sans forme) | un ingrédient d'étiquette `#minecraft:acacia_logs`, résultat x4 | D |
| V-R6 | `recipe/baked_potato.json` | fonte, `cookingtime` 200, `experience` 0.35 ; variantes fumoir (200) et feu de camp (600) | D |
| V-R7 | `recipe/netherite_axe_smithing.json` | modèle `netherite_upgrade_smithing_template`, base `diamond_axe`, ajout `#minecraft:netherite_tool_materials` | D |
| V-R8 | `recipe/brewing/potion_awkward_blaze_powder.json` | entrée `potion` + `potion_contents.potions` = `awkward`, réactif `blaze_powder`, sortie potion de force | D |
| V-R9 | `recipe/map_cloning.json` | `crafting_transmute`, `input` `#minecraft:clonable_maps`, `material` `map`, `material_count` 1 à 8, `add_material_count_to_result` true, résultat `{}` | D |
| V-R10 | Fournisseur `cooking/time_dried_kelp_block` | durée 4001 en normal, 2000 en rapide (division entière) | D / S (arrondi) |
| V-R11 | Fournisseur `compostable/low` au niveau 0 | valeur 1 sans tirage pondéré (R34) | D |
| V-R12 | Avancement `recipes/misc/blaze_powder` (chemin du dossier de groupe à relever par recherche du nom) | critères `has_blaze_rod`, `has_the_recipe`, récompense `minecraft:blaze_powder` | D |

Commandes de reproduction : lecture directe des fichiers sous `.cache/reference/26.3/resources/data/minecraft/recipe/` (tout lecteur JSON). Le dénombrement V-R1 et le tableau de R3 viennent d'un script de 20 lignes (parcours du dossier, groupement par `type`, union des clés).

Vecteurs de comportement (S, via sondes) :

| Vecteur | Entrée | Sortie attendue (provisoire) |
|---|---|---|
| V-R13 | Fabricateur, grille `[[planche,planche,vide],[planche,planche,vide],[vide,vide,vide]]` | table de fabrication (translation du 2x2) |
| V-R14 | Fabricateur, motif de la bannière en miroir | recette reconnue en miroir (R16) |
| V-R15 | Fabricateur, ragoût de champignons avec bol | bol rendu dans la case ou à la sortie (R22, R23) |
| V-R16 | Four, `cooking_time_spent` après 10 ticks allumés (fumoir) | 20 si la vitesse vaut 2 (R27) |

## Plan de tests

- **Unitaire (L1)** : décodage des 2042 recettes avec le jeu de formes de R9 à R14 sans exception ; vérifications croisées V-R1 à V-R12 ; propriétés : reconnaissance d'un motif translaté, symétrie, sans forme invariant par permutation.
- **Différentiel (L2)** : sondes PR1 à PR11 ; pour toute recette de fabrication, comparaison (reconnu / résultat) avec le fabricateur de l'oracle sur la grille canonique de la recette (échantillon de 100 recettes figé dans `tools/diff/samples.json`), puis sur 20 grilles perturbées par recette.
- **Niveau B** : tests en aveugle depuis cette spec.

## Sondes proposées

Cadre : serveur 26.3, monde plat, RCON. Le fabricateur (bloc `crafter`) évalue les recettes de fabrication sans client : il est rempli par `item replace block <pos> container.<i> with ...`, alimenté par un bloc de redstone, et son produit est éjecté dans un conteneur voisin (coffre ou entonnoir) relu par `data get block`. Les fours, fumoirs, hauts fourneaux, alambics et feux de camp sont remplis par commande et avancés par `/tick step`.

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Règles levées |
|---|---|---|---|
| PR1 | Fabricateur sur coffre ; pour chaque recette d'un échantillon, remplir les 9 slots (`container.0..8`, indice = x + 3y), bloc de redstone, `/tick step`, relecture du coffre ; variantes : translation, miroir, case superflue, étiquette (un objet de l'étiquette, tous les objets de l'étiquette) | Résultat et quantité de R11 ; miroir accepté ou refusé | R14 à R17, R21, R43 à R49 |
| PR2 | Pack à deux recettes `sonde:a` et `sonde:b` aux mêmes entrées (résultats différents), avec trois nommages d'identifiants (`a`/`b`, `b`/`a`, `z`/`a`) et un second pack plus prioritaire ; fabricateur | Quelle recette gagne (H-ord-a, b ou c) ; ordre indépendant de l'ordre alphabétique ? | R54, R55, R20 |
| PR3 | Fabricateur avec `milk_bucket`, `mushroom_stew` : lire où va le reste ; inventaire de joueur plein si un client est disponible | Case d'origine, coffre ou sol | R22, R23 |
| PR4 | Alambic : bouteille d'eau `potion_contents` + `nether_wart`, puis chaque réactif de R39 sur chaque entrée ; `/tick step` jusqu'à `BrewTime` 0 ; relecture `data get block ... Items` | Sortie de la recette ; durée de 400 ticks ; consommation du réactif et du carburant | R10, R39 à R42 |
| PR5 | Client protocole (voir `item-components-inventory.md` PI9) : tailleur de pierre, forge (transformation et ornement) ; ou, sans client, résultat dérivé par la commande de butin : `item modify` avec `set_item` non pertinent | Résultats, ordre des recettes proposées | R36 à R38 |
| PR6 | Four, haut fourneau, fumoir : `item replace block ... container.0 with minecraft:potato`, `container.1 with minecraft:coal`, `/tick step 1` x 50, relecture de `cooking_time_spent`, `cooking_total_time`, `lit_time_remaining`, `lit_total_time` ; puis retrait du combustible et nouveaux ticks | Pas de progression 1 ou 2 ; régression de 2 sans combustible | R26 à R28 |
| PR7 | Sortie bloquée (sortie remplie d'un autre objet, ou pleine) ; changement d'entrée en cours de cuisson | Progression conservée, remise à zéro ou bloquée | R27, R29 |
| PR8 | Cuire 10 pommes de terre et lire `RecipesUsed` ; puis reprise du résultat par un joueur (client ou `/xp query`) | Contenu de `RecipesUsed`, expérience (arrondi) | R29 |
| PR9 | Chaque fournisseur de carburant : un objet par fournisseur dans un four ; mesurer `lit_total_time` après allumage, dans un four, un haut fourneau, un fumoir ; seau de lave (reste) | Durées du tableau de R32 ; `bucket` rendu | R32, R33 |
| PR10 | Composteur plein de niveau 0 puis niveaux 1 à 7 ; ajout répété d'un objet de chaque catégorie (client ou entonnoir) ; 10000 essais statistiques | Loi des couches, niveau 0 déterministe | R34 |
| PR11 | `recipe give @s *`, `recipe take @s minecraft:acacia_boat`, `data get entity @s recipeBook` ; `gamerule limited_crafting true` puis fabrication d'une recette non débloquée avec client | Forme du livre de recettes, effet de la règle | R51, R52 |
| PR12 | Recherche des conflits d'entrée entre recettes du pack vanilla (script sur les fichiers extraits : mêmes ingrédients pour deux recettes du même type) | Liste vide en fabrication ; liste des conflits de fonte | R54 |
| PR13 | Chargement de recettes invalides dans un pack `sonde` : liste d'ingrédients vide, clé inutilisée ou caractère sans clé, motif vide, étiquette absente, `count` supérieur à la taille de pile, `type` inconnu, `group` et `category` omis ou invalides ; journal du serveur puis fabrication des recettes valides du même pack | Recettes ignorées ou chargement entier en échec ; message ; défauts de `group`, `category`, `show_notification` | R5 à R9, R13, R15, R24 |

## Couverture des règles S par les sondes

R4 (association machine) : PR1, PR6 ; R5 (remplacement par un pack) : PR2, PR13 et les sondes P4 et P17 de `data-datapack-loading.md` ; R6, R7, R8 (groupe, catégorie, notification) : PR11, PR13 ; R9, R13, R15, R24 (refus et défauts) : PR13 ; R12 (résultat `{}`) : PR1 (cartes) ; R31 (sélection de fonte) : PR12, PR2 ; R35 (carburant d'alambic) : PR4. Aucune règle S n'est sans sonde.

## Questions ouvertes

- Reste de fabrication des objets absents de `use_remainder` (seau de lave comme carburant, bouteille de potion dans la recette de la flèche tippée) : S, PR3, PR9.
- Symétrie : le miroir est-il accepté pour toutes les recettes ou par recette (R16) ? Le champ n'est pas dans les données observées ; hypothèse : accepté partout. PR1.
- Ordre de résolution (R54) et conflits de fonte (R31) : PR2, PR12.
- Détail de la progression des machines (R27, R28) et de l'expérience (R29).
- Les 12 recettes spéciales (R43 à R49) ne sont décrites que par leur comportement général ; chacune demande une table d'exemples issue de la sonde PR1.
- Format du livre de recettes dans les données du joueur (R51) et effet exact de `limited_crafting` (R52).
- Crafter : cases désactivées, déclenchement, éjection : spec redstone (M5) pour la mécanique ; ici seulement le test de recette.
- Règles S restantes : R4, R5, R6, R7 (correspondance), R8, R13, R15 à R17, R19 à R24, R26 à R29, R31 (sélection), R33, R34 (usage), R35, R36, R37, R38, R40 à R49, R51 à R55.

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Recettes | M6 | B | spécifié | `docs/specs/item-recipes.md` (formats et dénombrements en D ; reconnaissance, consommation, ordre en S) |
| Cuisson et carburants (ligne à créer) | M6 | B | spécifié | idem, R25 à R35 |

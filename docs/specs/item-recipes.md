# Recettes, cuisson, carburants et alchimie

Statut : brouillon
Jalon : M6
Risque : B
Règles : R1 à R55 (R11 et R49 sont découpées en sous-règles citables : R11a, R11b, R49a à R49f)

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
  - `resources/data/minecraft/advancement/recipes/**` : 1740 fichiers (la racine et 1739 avancements de déblocage) ;
  - `generated/reports/registries.json` : `recipe_type` (8), `recipe_serializer` (22), `recipe_book_category` (13), `recipe_display` (5), `slot_display` (11) ;
  - `generated/reports/minecraft/components/item/*.json` : composants `use_remainder`, `cooking_fuel`, `compostable`, `brewing_fuel` ;
  - `resources/data/minecraft/context_int_provider/{cooking,compostable,brewing}/**`, `context_float_provider/**`, `predicate/block/fast_cooking.json` ;
  - `resources/data/minecraft/tags/item/**` (étiquettes citées par les recettes) ;
  - `generated/reports/commands.json` : commande `recipe`, gamerule `limited_crafting` (via `registries.json`, `game_rule`) ;
  - structures `.nbt` : block entities `furnace`, `blast_furnace`, `smoker`, `brewing_stand`, `campfire`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Item modifier", "Loot table" (étiquettes d'objets), "Inventory", "Java Edition protocol/Inventory".
- **Rapports locaux** (indices, ADR 0002) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md` chapitres 125 à 133 (recettes, cuisson, carburants, alchimie, compostage). Le rapport pose la liste des comportements à reproduire sans les décrire ; les valeurs de ses tableaux (durées de carburant) sont recalculées ici à partir des données.
- **Sondes de l'oracle** : aucune exécutée. Les sondes sont en fin de document (PR1 à PR13).

Reproduction : tous les dénombrements D et C de cette spec sortent du script `recipes_stats.py` de l'annexe, lancé avec `python -I recipes_stats.py "$REF"` (`REF` = dossier `.cache/reference/26.3`). Les sections de sa sortie portent le numéro de la règle qu'elles étayent (`== R25 ...`). L'empreinte SHA-256 de la sortie complète est épinglée en annexe. Aucune table de données de référence n'est recopiée ici (AGENTS.md) : les tests lisent les fichiers extraits à l'exécution.

Convention de preuve : D valeur lue dans un fichier extrait ; C calcul reproductible sur ces fichiers ; M observé sur l'oracle (non utilisé) ; S documentation publique seule ou hypothèse. Chaque règle S porte un comportement **provisoire**.

## Règles

### Chargement et identification

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Une recette est un fichier JSON `data/<espace>/recipe/<chemin>.json`, d'identifiant `<espace>:<chemin>` (sous-dossiers permis : `brewing/`). Le champ `type` est un identifiant du registre `recipe_serializer` (22 entrées). | D |
| R2 | La version cible compte 2042 recettes réparties sur 22 sérialiseurs (tableau de R3). Les 8 types de recette (`recipe_type` : `crafting`, `smelting`, `blasting`, `smoking`, `campfire_cooking`, `stonecutting`, `smithing`, `brewing`) regroupent ces sérialiseurs par machine. | D |
| R3 | Dénombrement et champs par sérialiseur (chiffre entre parenthèses : nombre de fichiers qui portent le champ, quand il est inférieur au total). Commande : section `R1-R3` de `recipes_stats.py`. | D |

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
| R7 | Champ `category` (chaîne optionnelle, rangement du livre de recettes). Valeurs observées : fabrication `building` (493), `redstone` (120), `equipment` (124, dont 16 `crafting_transmute`) ; cuisson `food` (27 : 9 fonte, 9 fumoir, 9 feu de camp), `blocks` (36 : 34 fonte, 2 haut fourneau) ; champ absent pour 293 recettes à forme, 188 sans forme, 17 `crafting_transmute`, 30 `smelting`, 23 `blasting` et tous les autres types. Absent signifie la catégorie `misc` (provisoire). Les 13 catégories de livre (`recipe_book_category`) sont : `crafting_building_blocks`, `crafting_redstone`, `crafting_equipment`, `crafting_misc`, `furnace_food`, `furnace_blocks`, `furnace_misc`, `blast_furnace_blocks`, `blast_furnace_misc`, `smoker_food`, `stonecutter`, `smithing`, `campfire`. La correspondance (type, `category`) -> catégorie de livre est S. Commande : section `R6-R7`. | D (valeurs) ; S (correspondance) |
| R8 | Champ `show_notification` : une seule recette le porte (`crafting_table`, valeur `false`). Défaut `true` (provisoire) : le déblocage de la recette affiche une notification côté client. | D (présence) ; S (défaut) |

### Ingrédients et résultats

| Réf | Règle | Preuve |
|---|---|---|
| R9 | Un ingrédient de fabrication, de cuisson, de tailleur de pierre ou de forge est une chaîne : un identifiant d'objet (`minecraft:potato`), une étiquette d'objet précédée de `#` (`#minecraft:planks`), ou une liste d'identifiants d'objets. Observé : identifiant (1107 clés de recettes à forme, 744 ingrédients sans forme, 351 tailleur de pierre, 67 fonte, 22 haut fourneau, 9 fumoir, 9 feu de camp), étiquette (86 clés de formes, 50 ingrédients sans forme, 33 `input` de transmutation, 3 fonte, et les champs de forge et de recettes spéciales), liste d'identifiants (13 clés de formes, 113 ingrédients sans forme, 3 fonte, 3 haut fourneau). Aucune liste ne contient d'étiquette. Provisoire : une liste est acceptée si tous ses éléments sont des identifiants ; une liste vide est refusée. Commande : section `R9` (aucune liste vide dans les données). | D (formes) ; S (refus) |
| R10 | Ingrédient d'alchimie : objet `{item, potion_contents?}` pour `input` (`item` identifiant, `potion_contents` prédicat `{potions: <id de potion>}`) et pour `reagent` (`{item}`), `output` pile `{id, components?}`. Les 279 recettes d'alchimie ont toutes ces trois champs. Le champ `potion_contents` de `input` désigne le contenu de potion du composant du même nom (`potions` : identifiant de potion, `awkward` etc.). | D |
| R11a | Pile de résultat : `{id, count?, components?}` (voir `item-components-inventory.md` R28). Sur les 2023 recettes qui portent un résultat ou une sortie : 727 ont un `count`, 1294 n'en ont pas, 2 ont `{}` (R12) ; 296 ont des `components` (exemple : `suspicious_stew_from_allium` porte `suspicious_stew_effects` `[{duration: 60, id: fire_resistance}]`). Les `count` observés sont 2, 3, 4, 6, 8, 9 et 16 (par exemple `acacia_door` 3, `tipped_arrow` 8, `firework_rocket` 3). Commande : section `R10-R11-R12`. | D |
| R11b | Composition de la pile de résultat : le résultat reçoit les composants par défaut de l'objet (prototype), puis les `components` du fichier les remplacent ou les complètent ; un `count` absent vaut 1. Provisoire. Sonde PR1 (relecture de l'objet éjecté par le fabricateur avec `data get block`). | S |
| R12 | Un résultat `{}` (objet vide) apparaît sur 2 recettes (`map_cloning`, `map_extending`) : le résultat est dérivé de la grille, pas de la donnée (R48, R49f). 19 recettes n'ont aucun champ `result` ni `output` : `crafting_special_repairitem` (qui n'a ni ingrédient ni résultat) et les 18 `smithing_trim`, dont le résultat est dérivé de la base. | D (présence) ; S (sens) |
| R13 | Les étiquettes citées sont résolues à l'appel (contenu au moment du rechargement). Une étiquette absente est une erreur de chargement de la recette (la recette est ignorée, les autres se chargent). Provisoire. Voir `data-datapack-loading.md` pour la résolution et les cycles. | S |

### Recettes à forme (crafting_shaped)

| Réf | Règle | Preuve |
|---|---|---|
| R14 | Champs : `pattern` (liste de 1 à 3 chaînes), `key` (table caractère -> ingrédient), `result`. Observé sur les 827 recettes : toutes les lignes d'un motif ont la même longueur ; largeur 1 à 3 et hauteur 1 à 3 ; dimensions (hauteur x largeur) : 3x3 347, 2x3 161, 1x3 121, 2x2 83, 1x2 35, 3x2 30, 2x1 29, 3x1 21 ; nombre de clés : 1 (519), 2 (241), 3 (63), 4 (4). L'espace dénote une case vide. Commande : section `R14`. | D |
| R15 | Tout caractère du motif autre que l'espace est une clé de `key` ; une clé non utilisée ou un caractère sans clé est une erreur de chargement. Un motif dont toutes les cases sont vides est refusé. Provisoire. | S |
| R16 | Reconnaissance : la recette est reconnue si la grille de fabrication contient le motif à une position quelconque (translation entière) et si toutes les cases hors du rectangle du motif sont vides. Le motif est de plus reconnu en miroir horizontal (symétrie gauche-droite). Une grille 2x2 (fenêtre joueur) ne reconnaît que les motifs de largeur et hauteur au plus 2. Provisoire. Sonde PR1 (grille 3x3 du fabricateur). | S |
| R17 | Chaque case non vide est testée contre l'ingrédient de la clé : un identifiant d'objet exige cet objet ; une étiquette exige un objet de l'étiquette ; une liste exige l'un de ses éléments. Les composants de la pile d'entrée ne sont pas testés (provisoire), sauf pour les recettes spéciales. | S |

### Recettes sans forme (crafting_shapeless)

| Réf | Règle | Preuve |
|---|---|---|
| R18 | Champs : `ingredients` (liste), `result`. Tailles de la liste observées sur les 375 recettes : 1 (86), 2 (232), 3 (7), 4 (19), 5 (4), 7 (1), 8 (1), 9 (25). Maximum 9 (la grille). Commande : section `R18`. | D |
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
| R25 | `smelting` (73), `blasting` (25), `smoking` (9), `campfire_cooking` (9) portent `ingredient`, `result`, `cookingtime` (entier, ticks), `experience` (flottant). `cookingtime` vaut 200 pour les 107 recettes de four, haut fourneau et fumoir, 600 pour les 9 recettes de feu de camp. Valeurs d'`experience` observées : 0.1 (40 fonte, 5 haut fourneau, 1 fumoir, 1 feu de camp), 0.15, 0.2, 0.3, 0.35 (9 fonte, 8 fumoir, 8 feu de camp), 0.7 (8 fonte, 8 haut fourneau), 1.0 (9 fonte, 8 haut fourneau), 2.0 (1 fonte, 1 haut fourneau). Commande : section `R25`. | D |
| R26 | Le haut fourneau et le fumoir ne divisent pas `cookingtime` dans les données : ils sont plus rapides par une vitesse. Le fournisseur `cooking/speed_default` (flottant contextuel) vaut `cooking/fast_speed_multiplier` (2.0) si le prédicat `block/fast_cooking` est vrai, `cooking/normal_speed_multiplier` (1.0) sinon ; `block/fast_cooking` est un `match_block` sur `smoker` ou `blast_furnace`. Commande : section `R32` (fournisseurs). | D (fournisseurs) |
| R26a | Une recette `smelting` n'est pas lue par un haut fourneau ni un fumoir, et inversement (association machine, R4). Provisoire. Sonde PR6. | S |
| R27 | Progression : par tick de combustion, `cooking_time_spent` augmente de la vitesse (1 ou 2) ; la cuisson se termine quand `cooking_time_spent` atteint `cooking_total_time` (initialement `cookingtime`). Provisoire : arrondi de la progression flottante vers un entier, et ce que devient `cooking_time_spent` quand la sortie est bloquée ou quand l'entrée change sont S. Sondes PR6, PR7. | S |
| R28 | Un tick sans combustible allumé diminue `cooking_time_spent` (régression). Provisoire : de 2 par tick, jusqu'à 0. Sonde PR6. | S |
| R29 | Un four consomme une unité de l'ingrédient et ajoute `count` unités du résultat à la sortie (même contenu ou sortie vide) ; la fonte est bloquée si la sortie contient un contenu différent ou est pleine. L'expérience cumulée est stockée dans `RecipesUsed` (identifiant de recette -> nombre de fontes), provisoire : le joueur la reçoit à la reprise du résultat, valeur `experience` x nombre, arrondie avec un tirage de la partie fractionnaire. Sonde PR8. | S |
| R30 | NBT d'un four (D, `item-components-inventory.md` R31) : `lit_time_remaining`, `lit_total_time`, `cooking_time_spent`, `cooking_total_time` (Short), `RecipesUsed`, `Items` (0 ingrédient, 1 combustible, 2 sortie). Un feu de camp porte `CookingTimes` et `CookingTotalTimes` (Int_Array de 4, une cuisson par emplacement). | D |
| R31 | La recherche de recette de cuisson est par objet d'entrée (identifiant, étiquette, liste). Aucun objet n'est apparié par deux recettes d'un même type de cuisson, étiquettes développées (0 objet partagé pour `smelting` à 73 recettes dont 3 listes, `blasting` à 25 dont 3 listes, `smoking` et `campfire_cooking` à 9 ; les outils en cuivre sont dans une liste de 3 recettes `smelting` et de 3 recettes `blasting`). Commande : section `R31-R54`. Le cas de plusieurs recettes convenant à un ingrédient ne se produit donc pas dans les données vanilla ; sa résolution pour un pack est S (R54). | D (absence de recouvrement) ; S (pack) |

### Carburants et compostage

| Réf | Règle | Preuve |
|---|---|---|
| R32 | Les carburants sont des données portées par le composant `cooking_fuel` `{burn_time, speed_multiplier}` : 347 objets, 18 valeurs distinctes de `burn_time` (références à des fournisseurs entiers `cooking/time_*`) et une seule de `speed_multiplier` (`cooking/speed_default`). Chaque fournisseur `cooking/time_*` est `{type: div, left: <durée de base>, right: conditional(block/fast_cooking)}`, la branche vraie valant le fournisseur entier `fast_burn_time_reduction_factor` (2) et la fausse `normal_burn_time_reduction_factor` (1). Distribution (section `R32`, qui imprime une ligne par fournisseur : effectif, durée de base, durée normale, durée rapide) : durées de base de 50 à 20000 ticks ; 153 objets pour `time_wood_blocks` (300), 32 pour `time_wool` (100) et pour `time_wool_slabs` (50), 28 pour `time_wood_items_large` (200), 22 pour `time_boats` (1200) ; 7 fournisseurs n'ont qu'un ou deux objets ; deux durées de base sont impaires (4001 pour `dried_kelp_block`, 67 pour les tapis). Exemples (durée normale / rapide) : `blaze_rod` 2400 / 1200 ; `coal` 1600 / 800 ; `lava_bucket` 20000 / 10000 ; `bamboo` 50 / 25. | D / C |
| R32a | Hypothèse S : la division entière arrondit vers le bas (4001 -> 2000 en rapide, 67 -> 33). Sonde PR9. | S |
| R33 | Le seau de lave est un carburant de 20 000 ticks (10 000 en rapide) ; il n'a pas de composant `use_remainder` (D, section `R32`). Le seau vide rendu après combustion est un comportement du four (S). Sonde PR9. | D / S |
| R34 | Compostage : le composant `compostable` `{layers}` fait référence à un fournisseur entier `compostable/{low, low_medium, medium, medium_high, always_add_one}` (objets : 50, 11, 47, 12, 2). Chaque fournisseur probabiliste vaut 1 si le composteur est au niveau 0 (`match_block composter` état `level` "0"), sinon une loi pondérée `1` (poids 30, 50, 65, 85 pour `low`, `low_medium`, `medium`, `medium_high`) contre `0` (poids 70, 50, 35, 15) ; `always_add_one` vaut 1. La valeur est le nombre de couches ajoutées. La sémantique (niveau maximal, récolte, entonnoirs) est S. Évaluation par `loot` (DC1). Commande : sections `R34` et `R32` (effectifs). Sonde PR10. | D (fournisseurs) ; S (usage) |
| R35 | Alambic : le composant `brewing_fuel` de `blaze_powder` `{speed_multiplier: brewing/speed_default (1.0), uses: brewing/uses_default (20)}` : un objet-carburant donne 20 utilisations à l'alambic (`Fuel` Byte, 0 à 20) et vitesse 1.0. Évaluation par `loot` (DC1). Commande : section `R35`. | D (valeurs) ; S (sens) |

Décision de conception DC1 (ADR 0004, sans niveau de preuve : elle ne décrit pas le jeu de référence) : `item` ne porte que la référence au fournisseur (identifiant lu dans `cooking_fuel`, `compostable`, `brewing_fuel`) et les données de recette. L'évaluation des fournisseurs contextuels et du prédicat de bloc (R26, R32, R34, R35) est faite par `loot`, avec un contexte de bloc réduit à l'état du bloc de la machine. `game`, qui possède la machine et son tick (ADR 0005), appelle `loot` avec ce contexte et transmet le résultat (vitesse, durée, couches) à la logique de cuisson ou de compostage. `item` ne dépend pas de `loot` et `loot` ne dépend pas de `game`.


### Forge et tailleur de pierre

| Réf | Règle | Preuve |
|---|---|---|
| R36 | `smithing_transform` (12) : `template`, `base`, `addition` (ingrédients), `result` ; observé : `template` identifiant, `base` identifiant d'outil ou d'armure en diamant, `addition` étiquette `#minecraft:netherite_tool_materials` (lingot de netherite). Le résultat reprend les composants de la pile `base` (provisoire : tous, durabilité comprise). | D (forme) ; S (copie de composants) |
| R37 | `smithing_trim` (18) : `template` identifiant de modèle d'ornement, `base` étiquette `#minecraft:trimmable_armor`, `addition` étiquette `#minecraft:trim_materials` (11 matériaux), `pattern` identifiant de motif. Le résultat est la base avec le composant `trim` `{material, pattern}` (le matériau vient de l'`addition` via `provides_trim_material` D). Sonde PR5. | D / S |
| R38 | `stonecutting` (351) : `ingredient` identifiant, `result` pile. Le tailleur de pierre propose toutes les recettes dont l'ingrédient correspond à l'objet d'entrée ; l'ordre de proposition est S. Chaque retrait consomme une unité. Sonde PR5. | D (forme) ; S (usage) |

### Alchimie pilotée par données

| Réf | Règle | Preuve |
|---|---|---|
| R39 | Distribution des 279 recettes `brewing` (section `R39`) : `input.item` `potion` 108, `splash_potion` 108, `lingering_potion` 63 ; `output.id` `lingering_potion` 108, `splash_potion` 108, `potion` 63 ; 21 réactifs distincts : `gunpowder` 45, `dragon_breath` 45, `redstone` 42, `fermented_spider_eye` 36, `glowstone_dust` 30, onze réactifs à 6 recettes et cinq à 3. Toutes les entrées ont les clés `item` et `potion_contents`. | C |
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
| R49a | `crafting_special_repairitem` : deux objets à durabilité de même type, abîmés, donnent un objet réparé dont la durabilité restante vaut la somme des durabilités restantes plus un bonus proportionnel à `max_damage`, plafonnée ; les enchantements sont perdus ; condition de non-fusion S. Le pourcentage du bonus (5 pour cent selon la mémoire de la documentation publique) est à confirmer par PR1. | S |
| R49b | `crafting_special_shielddecoration` (`target` bouclier, `banner` étiquette `banners`) : bouclier + bannière donnent le bouclier avec les motifs et la couleur de la bannière. | S |
| R49c | `crafting_decorated_pot` (`front`, `back`, `left`, `right` étiquette `decorated_pot_ingredients`) : 4 éléments en croix donnent un pot décoré dont `pot_decorations` suit les 4 éléments. | S |
| R49d | `crafting_dye` (6, `target` pièce de cuir, `dye` étiquette `dyes`) : une pièce plus n teintures donnent la pièce avec `dyed_color` mélangé. | S |
| R49e | `crafting_imbue` (`source` potion persistante, `material` flèche, résultat 8) : 8 flèches autour d'une potion persistante donnent 8 flèches avec `potion_contents` de la potion (durée / 8). | S |
| R49f | `crafting_transmute` (33, `input` étiquette, `material`, `result`) : le résultat prend l'objet du `result` et les composants de la pile `input` (exemple : boîte de shulker colorée garde son contenu) ; `material_count` (intervalle 1 à 8) et `add_material_count_to_result` pour le clonage de cartes (`map_cloning` : carte + 1 à 8 cartes vierges donnent n+1 cartes). | S |

### Livre de recettes

| Réf | Règle | Preuve |
|---|---|---|
| R50 | Déblocage : 1740 fichiers sous `advancement/recipes/` (section `R50`) : la racine `recipes/root` (critère `impossible`, sans parent) et 1739 avancements de déblocage, tous de parent `minecraft:recipes/root`, qui débloquent 1739 recettes distinctes (303 recettes n'ont aucun avancement, dont les 279 d'alchimie). Chaque avancement de déblocage a un critère `has_the_recipe` (déclencheur `minecraft:recipe_unlocked`, `conditions.recipes` = identifiant de la recette) et au moins un critère de possession : 1886 critères `inventory_changed` (liste d'objets), 11 `enter_block`, 1 `tick`. `requirements` est une seule liste (OU) de tous les critères ; le nombre de critères par avancement est 2 pour 1660 avancements, 3 pour 61, 4 pour 12, 13 pour 4 et 17 pour 2. `rewards.recipes` = `[identifiant]`. Exemple `recipes/brewing/blaze_powder` : critère `has_blaze_rod`. | D |
| R51 | Les recettes sont débloquées quand l'avancement est obtenu (récompense) ou par `/recipe give <joueurs> *` ou `/recipe give <joueurs> <recette>` ; `/recipe take` les retire (D, syntaxe de `commands.json` : `recipe give` et `recipe take` avec `<targets>` puis `*` ou `<recipe>` (resource_key du registre recipe)). Le joueur reçoit l'état dans ses données (`recipeBook`) : forme S (sonde PR11). | D (syntaxe) ; S (reste) |
| R52 | La règle de jeu `limited_crafting` (booléen, défaut `false`, D registre `game_rule`) : quand elle est vraie, seules les recettes débloquées sont fabricables. Provisoire. Sonde PR11. | D (existence, défaut) ; S (effet) |
| R53 | Les recettes de brasserie, de fonte et de forge sont aussi dans le livre, selon `recipe_book_category`. `recipe_display` (5 types : `crafting_shapeless`, `crafting_shaped`, `furnace`, `stonecutter`, `smithing`) et `slot_display` (11 types) décrivent l'affichage client : jalon M8. | D (registres) |

### Résolution entre plusieurs recettes

| Réf | Règle | Preuve |
|---|---|---|
| R54 | Quand plusieurs recettes d'un même type de machine conviennent à la même entrée, une seule est retenue. Hypothèse H-ord-a : la première dans l'ordre de chargement (par identifiant de recette, ordre alphabétique des chemins puis empilement des packs). Hypothèse H-ord-b : la dernière chargée l'emporte. Hypothèse H-ord-c : ordre d'une table de hachage. Provisoire : H-ord-a (à départager par PR2). Les recettes de cuisson vanilla n'ont aucun conflit (D, R31). Pour la fabrication, l'absence de conflit n'est pas mesurable sans moteur de reconnaissance : un test L1 le vérifie sur les 2042 recettes (Plan de tests). | D (cuisson) ; S (ordre) |
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
| V-R12 | `advancement/recipes/brewing/blaze_powder.json` | critères `has_blaze_rod`, `has_the_recipe`, récompense `minecraft:blaze_powder` | D |
| V-R13b | `python -I recipes_stats.py "$REF" \| sha256sum` | empreinte de l'annexe | C |

Commandes de reproduction : lecture directe des fichiers sous `.cache/reference/26.3/resources/data/minecraft/recipe/` (tout lecteur JSON). Le dénombrement V-R1 et le tableau de R3 sortent de la section `R1-R3` de `recipes_stats.py` (annexe).

Vecteurs de comportement (S, via sondes) :

| Vecteur | Entrée | Sortie attendue (provisoire) |
|---|---|---|
| V-R13 | Fabricateur, grille `[[planche,planche,vide],[planche,planche,vide],[vide,vide,vide]]` | table de fabrication (translation du 2x2) |
| V-R14 | Fabricateur, motif de la bannière en miroir | recette reconnue en miroir (R16) |
| V-R15 | Fabricateur, ragoût de champignons avec bol | bol rendu dans la case ou à la sortie (R22, R23) |
| V-R16 | Four, `cooking_time_spent` après 10 ticks allumés (fumoir) | 20 si la vitesse vaut 2 (R27) |

## Plan de tests

- **Unitaire (L1)** : décodage des 2042 recettes avec le jeu de formes de R9 à R14 sans exception ; vérifications croisées V-R1 à V-R12 ; propriétés : reconnaissance d'un motif translaté, symétrie, sans forme invariant par permutation.
- **Différentiel (L2)** : sondes PR1 à PR13 ; pour toute recette de fabrication, comparaison (reconnu / résultat) avec le fabricateur de l'oracle sur la grille canonique de la recette (échantillon de 100 recettes figé dans `tools/diff/samples.json`), puis sur 20 grilles perturbées par recette.
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
| PR12 | Recherche des conflits d'entrée entre recettes du pack vanilla : cuisson par la section `R31-R54` du script (déjà vide, D) ; fabrication par un test L1 qui reconnaît chaque recette de fabrication sur sa grille canonique et vérifie qu'aucune autre recette du même type ne la reconnaît | Liste vide pour la cuisson (D) ; liste vide attendue en fabrication | R54 |
| PR13 | Chargement de recettes invalides dans un pack `sonde` : liste d'ingrédients vide, clé inutilisée ou caractère sans clé, motif vide, étiquette absente, `count` supérieur à la taille de pile, `type` inconnu, `group` et `category` omis ou invalides ; journal du serveur puis fabrication des recettes valides du même pack | Recettes ignorées ou chargement entier en échec ; message ; défauts de `group`, `category`, `show_notification` | R5 à R9, R13, R15, R24 |

## Couverture des règles S par les sondes

R4 et R26a (association machine) : PR1, PR6 ; R11b et R49a à R49f : PR1 ; R5 (remplacement par un pack) : PR2, PR13 et les sondes P4 et P17 de `data-datapack-loading.md` ; R6, R7, R8 (groupe, catégorie, notification) : PR11, PR13 ; R9, R13, R15, R24 (refus et défauts) : PR13 ; R12 (résultat `{}`) : PR1 (cartes) ; R31 et R54 (résolution en cas de conflit dans un pack) : PR12, PR2 ; R32a : PR9 ; R35 (carburant d'alambic) : PR4. Aucune règle S n'est sans sonde.

## Questions ouvertes

- Reste de fabrication des objets absents de `use_remainder` (seau de lave comme carburant, bouteille de potion dans la recette de la flèche tippée) : S, PR3, PR9.
- Symétrie : le miroir est-il accepté pour toutes les recettes ou par recette (R16) ? Le champ n'est pas dans les données observées ; hypothèse : accepté partout. PR1.
- Ordre de résolution (R54) et conflits de fonte (R31) : PR2, PR12.
- Détail de la progression des machines (R27, R28) et de l'expérience (R29).
- Les 12 recettes spéciales (R43 à R49) ne sont décrites que par leur comportement général ; chacune demande une table d'exemples issue de la sonde PR1.
- Format du livre de recettes dans les données du joueur (R51) et effet exact de `limited_crafting` (R52).
- Crafter : cases désactivées, déclenchement, éjection : spec redstone (M5) pour la mécanique ; ici seulement le test de recette.
- Règles S restantes : R4, R5, R6, R7 (correspondance), R8, R11b, R13, R15 à R17, R19 à R24, R26a, R27 à R29, R32a, R33, R34 (usage), R35, R36, R37, R38, R40 à R48, R49a à R49f, R51 à R55, et la résolution en cas de conflit de R54.

## Mise à jour proposée de la matrice

| Fonctionnalité | Jalon | Risque | Statut proposé | Preuve |
|---|---|---|---|---|
| Recettes | M6 | B | spécifié | `docs/specs/item-recipes.md` (formats et dénombrements en D ; reconnaissance, consommation, ordre en S) |
| Cuisson et carburants (ligne à créer) | M6 | B | spécifié | idem, R25 à R35 (DC1 : décision de propriété `item` / `loot` / `game`) |

## Annexe : recipes_stats.py (C)

Lit uniquement les fichiers extraits ; aucune dépendance au jeu ; bibliothèque standard seule. À copier hors du dossier de données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I recipes_stats.py "$REF" | sha256sum
```

Empreinte SHA-256 attendue de la sortie complète (sortie en UTF-8, fins de ligne LF) :

```text
c3c26744284d4979fa8c99189d0b0edbd82d5667ade884782be0e3eb5abe9229
```

```python
"""Counts and shapes of recipes, fuels, compost, brewing and recipe advancements in the 26.3 data.

Usage: python -I recipes_stats.py <reference dir containing generated/ and resources/>
Reads extracted files only. Standard library only. Output is deterministic (sorted, LF).
"""
import collections
import glob
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


def form(v):
    if isinstance(v, str):
        return "tag" if v.startswith("#") else "id"
    if isinstance(v, list):
        return "list_with_tag" if any(isinstance(x, str) and x.startswith("#") for x in v) else "list"
    return "other"


def sers(r):
    return r["type"].split(":")[1]


registries = load(os.path.join(REP, "registries.json"))
recipes = tree("recipe")
by_type = collections.defaultdict(dict)
for rid, r in recipes.items():
    by_type[sers(r)][rid] = r

section("R1-R3 recipes by serializer, field presence")
print("recipes", len(recipes), "serializers", len(by_type),
      "registry recipe_serializer", len(registries["minecraft:recipe_serializer"]["entries"]),
      "recipe_type", sorted(e.split(":")[1] for e in registries["minecraft:recipe_type"]["entries"]),
      "recipe_book_category", len(registries["minecraft:recipe_book_category"]["entries"]),
      "recipe_display", len(registries["minecraft:recipe_display"]["entries"]),
      "slot_display", len(registries["minecraft:slot_display"]["entries"]))
print("brewing recipes", len(by_type["brewing"]), "top-level folders",
      sorted(collections.Counter(k.split("/")[0] if "/" in k else "." for k in recipes).items()))
for s in sorted(by_type, key=lambda k: (-len(by_type[k]), k)):
    c = collections.Counter()
    for r in by_type[s].values():
        c.update(k for k in r if k != "type")
    n = len(by_type[s])
    print(s, n, ",".join(k + ("" if v == n else "(" + str(v) + ")") for k, v in sorted(c.items())))

section("R6-R7 group and category")
groups = set(r["group"] for r in recipes.values() if "group" in r)
print("distinct groups", len(groups))
cat = collections.Counter()
for s, d in by_type.items():
    for r in d.values():
        cat[(s, r.get("category", "(absent)"))] += 1
for k, v in sorted(cat.items()):
    print(k[0], k[1], v)
print("show_notification", [(k, r["show_notification"]) for k, r in recipes.items() if "show_notification" in r])

section("R9 ingredient forms")
forms = collections.Counter()
empty_lists = 0
for rid, r in recipes.items():
    s = sers(r)
    if s == "crafting_shaped":
        for v in r["key"].values():
            forms[(s, "key", form(v))] += 1
    elif s == "crafting_shapeless":
        for v in r["ingredients"]:
            forms[(s, "ingredients", form(v))] += 1
    elif s in ("stonecutting", "smelting", "blasting", "smoking", "campfire_cooking"):
        forms[(s, "ingredient", form(r["ingredient"]))] += 1
    elif s.startswith("smithing"):
        for f in ("template", "base", "addition"):
            forms[(s, f, form(r[f]))] += 1
    elif s == "crafting_transmute":
        for f in ("input", "material"):
            forms[(s, f, form(r[f]))] += 1
    for v in r.values():
        if v == []:
            empty_lists += 1
for k, v in sorted(forms.items()):
    print(k, v)
print("empty lists in recipes", empty_lists)

section("R10-R11-R12 results and count")
res_with = res_without = res_empty = 0
res_none = []
res_over = collections.Counter()
for rid, r in recipes.items():
    out = r.get("result", r.get("output"))
    if out is None:
        res_none.append(rid)
    elif out == {}:
        res_empty += 1
    elif "count" in out:
        res_with += 1
        res_over[out["count"]] += 1
    else:
        res_without += 1
print("with count", res_with, "without count", res_without, "empty {}", res_empty, "no result/output", res_none)
print("count values", sorted(res_over.items()))
print("empty result ids", [k for k, r in recipes.items() if r.get("result") == {}])
print("results with components", sum(1 for r in recipes.values() if "components" in (r.get("result") or r.get("output") or {})))
print("stew example", recipes["suspicious_stew_from_allium"]["result"])

section("R14 shaped dimensions and key counts")
dims = collections.Counter()
keys = collections.Counter()
ragged = 0
for r in by_type["crafting_shaped"].values():
    p = r["pattern"]
    if len({len(x) for x in p}) != 1:
        ragged += 1
    dims[(len(p), len(p[0]))] += 1
    keys[len(r["key"])] += 1
print("ragged", ragged, "dims HxW", sorted(dims.items(), key=lambda kv: (-kv[1], kv[0])), "keys", sorted(keys.items()))
print("unused keys or missing keys", sum(
    1 for r in by_type["crafting_shaped"].values()
    if set("".join(r["pattern"])) - {" "} != set(r["key"])))

section("R18 shapeless sizes")
print(sorted(collections.Counter(len(r["ingredients"]) for r in by_type["crafting_shapeless"].values()).items()))

section("R25 cooking time and experience")
for s in ("smelting", "blasting", "smoking", "campfire_cooking"):
    ct = collections.Counter(r["cookingtime"] for r in by_type[s].values())
    ex = collections.Counter(r["experience"] for r in by_type[s].values())
    print(s, "cookingtime", sorted(ct.items()), "experience", sorted(ex.items()))
print("baked_potato", {k: recipes["baked_potato"][k] for k in ("type", "cookingtime", "experience")})

section("R31-R54 cooking inputs shared by several recipes of one type (tags expanded)")
item_tags = tree("tags/item")


def expand(tag, seen=()):
    out = set()
    for v in item_tags[tag]["values"]:
        v = v["id"] if isinstance(v, dict) else v
        if v.startswith("#"):
            out |= expand(v.split(":")[1], seen)
        else:
            out.add(v)
    return out


for s in ("smelting", "blasting", "smoking", "campfire_cooking"):
    owners = collections.defaultdict(list)
    for rid, r in by_type[s].items():
        ing = r["ingredient"]
        if isinstance(ing, str) and ing.startswith("#"):
            members = expand(ing[1:].split(":")[-1])
        else:
            members = {ing} if isinstance(ing, str) else set(ing)
        for it in members:
            owners[it].append(rid)
    shared = {k: v for k, v in owners.items() if len(v) > 1}
    lists = sum(1 for r in by_type[s].values() if isinstance(r["ingredient"], list))
    print(s, "recipes", len(by_type[s]), "list ingredients", lists, "items matched by several recipes", len(shared), sorted(shared.items())[:3])

section("R32 fuels: cooking_fuel providers")
items = {}
for f in glob.glob(os.path.join(REP, "minecraft", "components", "item", "*.json")):
    items[os.path.basename(f)[:-5]] = load(f)["components"]
int_prov = tree("context_int_provider")
flt_prov = tree("context_float_provider")
fuel = collections.defaultdict(list)
speeds = collections.Counter()
for n, c in items.items():
    cf = c.get("minecraft:cooking_fuel")
    if cf:
        fuel[cf["burn_time"]].append(n)
        speeds[cf["speed_multiplier"]] += 1
print("fuel items", sum(len(v) for v in fuel.values()), "distinct burn_time", len(fuel), "speed_multiplier", sorted(speeds.items()))
shape = collections.Counter()
rows = []
for ref in sorted(fuel):
    p = int_prov[ref.split(":")[1]]
    shape[(p["type"], p["right"]["type"] if isinstance(p["right"], dict) else "ref:" + p["right"], p["right"].get("condition") if isinstance(p["right"], dict) else None)] += 1
    rows.append((ref.split(":")[1], len(fuel[ref]), p["left"], p["left"] // 1, p["left"] // 2, sorted(fuel[ref])[:3]))
print("provider shapes", sorted(shape.items(), key=str))
print("durations (provider, items, left, normal, fast, 3 first items)")
for row in rows:
    print(row)
print("odd durations", [r[0] + ":" + str(r[2]) for r in rows if r[2] % 2])
print("factor providers", {k: v for k, v in int_prov.items() if "factor" in k})
print("float providers", flt_prov)
print("fast_cooking predicate", tree("predicate")["block/fast_cooking"])
print("lava_bucket components", {k: v for k, v in items["lava_bucket"].items() if k in ("minecraft:cooking_fuel", "minecraft:use_remainder")})

section("R34 compost")
comp = collections.Counter(c["minecraft:compostable"]["layers"] for c in items.values() if "minecraft:compostable" in c)
print("compostable items", sum(comp.values()), sorted(comp.items()))
for k, p in sorted(int_prov.items()):
    if k.startswith("compostable/"):
        if isinstance(p, int):
            print(k, "constant", p)
        elif p.get("type") == "minecraft:number_dispatcher":
            print(k, "level0 value", p["cases"][0]["value"], "weights", [(d["data"], d["weight"]) for d in p["default"]["distribution"]])
        else:
            print(k, p)

section("R35 brewing fuel")
print({n: c["minecraft:brewing_fuel"] for n, c in items.items() if "minecraft:brewing_fuel" in c})
print(int_prov["brewing/uses_default"], flt_prov["brewing/speed_default"])

section("R36-R38 smithing and stonecutting")
for s in ("smithing_transform", "smithing_trim"):
    print(s, len(by_type[s]), "template ids", len({r["template"] for r in by_type[s].values()}),
          "additions", sorted({r["addition"] for r in by_type[s].values()})[:3], "bases", sorted({r["base"] for r in by_type[s].values()})[:3])
print("stonecutting", len(by_type["stonecutting"]), "ingredient forms", sorted(collections.Counter(form(r["ingredient"]) for r in by_type["stonecutting"].values()).items()))

section("R39 brewing")
b = by_type["brewing"]
print("input.item", sorted(collections.Counter(r["input"]["item"] for r in b.values()).items()))
print("output.id", sorted(collections.Counter(r["output"]["id"] for r in b.values()).items()))
print("reagent", sorted(collections.Counter(r["reagent"]["item"] for r in b.values()).items(), key=lambda kv: (-kv[1], kv[0])))
print("input keys", sorted(collections.Counter(tuple(sorted(r["input"])) for r in b.values()).items()))
print("brewing_potion_inputs", tree("tags/item")["brewing_potion_inputs"]["values"])

section("R50 recipe advancements")
adv = tree("advancement")
rec_adv = {k: v for k, v in adv.items() if k.startswith("recipes/")}
unlocked = collections.Counter()
trig = collections.Counter()
ncrit = collections.Counter()
req_shapes = collections.Counter()
parents = collections.Counter()
for k, a in rec_adv.items():
    parents[a.get("parent")] += 1
    for crit in a["criteria"].values():
        trig[crit["trigger"]] += 1
    ncrit[len(a["criteria"])] += 1
    req_shapes[tuple(len(x) for x in a.get("requirements", []))[:1] == (len(a["criteria"]),)] += 1
    for r in a.get("rewards", {}).get("recipes", []):
        unlocked[r] += 1
print("files", len(rec_adv), "parents", sorted(parents.items(), key=str))
print("criteria triggers", sorted(trig.items()), "total", sum(trig.values()))
print("criteria per advancement", sorted(ncrit.items()))
print("single OR list of all criteria", sorted(req_shapes.items()))
print("distinct recipes unlocked", len(unlocked), "max per recipe", max(unlocked.values()))
recipe_ids = {"minecraft:" + k for k in recipes}
print("recipes without advancement", len(recipe_ids - set(unlocked)), "of which brewing", len([r for r in recipe_ids - set(unlocked) if r.startswith("minecraft:brewing/")]))
print("unlocked ids not recipes", sorted(set(unlocked) - recipe_ids))
print("root", rec_adv["recipes/root"]["criteria"])

section("R51-R52-R53 commands and game rule")
cmds = load(os.path.join(REP, "commands.json"))
rc = cmds["children"]["recipe"]
print("recipe subcommands", {k: sorted(v.get("children", {})) for k, v in rc["children"].items()})
print("limited_crafting" in json.dumps(registries["minecraft:game_rule"]))
```

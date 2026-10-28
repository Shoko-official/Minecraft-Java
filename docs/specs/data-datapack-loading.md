# Chargement des datapacks, registres et tags

Statut : brouillon
Jalon : M2
Risque : A

Le niveau A (fidélité critique : spec, tests aveugles, implémentation, diff oracle, bug-hunter, reviewer) vient de ce que tout module de données dépend de ces règles : une erreur de tags, de priorité, de syntaxe d'identifiant ou de lecture JSON se propage à `world`, `item`, `loot` et `worldgen` sans être détectée par leurs propres tests. Les décisions prises sur les ambiguïtés du squelette sont tracées dans `data-decisions-log.md`.

## Périmètre

Couvre, pour `engine/data` :

- la structure d'un datapack et d'un pack de ressources : `pack.mcmeta`, versions de format de la cible (data 121.0, ressources 97.1), arborescence `data/<namespace>/<dossier de registre>/**`, extensions ;
- la syntaxe des identifiants (`namespace:path`) et leur dérivation depuis les chemins de fichiers ; le type `Identifier` appartient au module `engine/data` (et non à `engine/core`), `core` n'ayant besoin que de chaînes (voir `core-random.md` R26) ;
- l'inventaire des registres statiques et des registres chargés par datapack, avec le nombre de fichiers par registre (détail dans `data-registries-inventory.md`) ;
- les tags : format, dossiers, résolution, fusion entre packs, références manquantes, cycles ;
- l'empilement des packs, la priorité, le remplacement de fichiers, les packs intégrés et les feature flags, les overlays et les filtres de `pack.mcmeta` ;
- l'analyse JSON attendue et le modèle d'erreurs ;
- le contrat de l'infrastructure (ADR 0005) : ce que le chargeur M2 valide seul et ce qu'il laisse aux modules propriétaires.

Ne couvre pas :

- le décodage typé du contenu des éléments (biomes, butins, recettes, enchantements...) : spec de chaque module propriétaire ;
- le format NBT des structures (`core-nbt.md`) et la syntaxe des fonctions `.mcfunction` (spec des commandes, `engine/game`) ;
- le rechargement à chaud (`/reload`) au-delà des points notés en questions ouvertes ;
- la synchronisation des registres avec le client (protocole, jalon M8) et le contenu des packs de ressources au-delà de leur structure (client, jalon M9) ; l'encodage des éléments vers JSON ou NBT est hors M2 (R65).

## Sources

- **Données extraites** (`.cache/reference/26.3/`, preuve D) :
  - `version.json` : `pack_version` (`data_major` 121, `data_minor` 0, `resource_major` 97, `resource_minor` 1), `world_version` 5023, `protocol_version` 777 ;
  - `generated/reports/registries.json` (95 registres statiques) et `generated/reports/datapack.json` (155 registres chargeables, section `others`) ;
  - `generated/reports/commands.json` : arbre des commandes `datapack`, `reload`, `loot`, `execute`, `function` utilisées par les sondes ;
  - `resources/data/minecraft/**` : pack vanilla et trois packs intégrés sous `resources/data/minecraft/datapacks/` ;
  - `resources/assets/minecraft/lang/en_us.json` : textes des messages observables (clés `commands.datapack.*`, `pack.*`, `dataPack.*`, `datapackFailure.*`, `arguments.*.tag.unknown`, `argument.resource*`) ;
  - `state.json` : `resourceFiles` = 9887.
- **Calculs** (preuve C) : script `data_inventory.py` (texte intégral et sortie dans `data-registries-inventory.md`) ; contrôles ponctuels décrits dans les vecteurs ci-dessous (analyse JSON stricte de tous les fichiers, relevé des nombres).
- **Documentation publique**, consultée le 2026-10-28 (preuve S tant qu'elle n'est pas recoupée par les données ou l'oracle) : Minecraft Wiki, pages "Pack format" (https://minecraft.wiki/w/Pack_format), "Data pack" (https://minecraft.wiki/w/Data_pack), "Pack.mcmeta" (https://minecraft.wiki/w/Pack.mcmeta), "Identifier" (https://minecraft.wiki/w/Identifier), "Tag (Java Edition)" (https://minecraft.wiki/w/Tag_(Java_Edition)) ; consultées le 2026-10-28 pour les décisions de `data-decisions-log.md` : "Enchantment definition" (https://minecraft.wiki/w/Enchantment_definition : le champ `supported_items` accepte un identifiant seul, un `#tag` ou un tableau d'identifiants, R64) et "Java Edition protocol/Packets" (https://minecraft.wiki/w/Java_Edition_protocol/Packets : données d'entrée de registre facultatives dans la configuration, packs connus, R65). La page "Data pack" ne dit rien des commentaires, virgules finales, clés en double ni de l'ordre de découverte des packs d'un dossier, et note seulement que le décodeur de prédicats d'entité rejette les champs inconnus depuis 26.2 (R63).
- **Rapports locaux** (indices, ADR 0002 point 2b) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, section sur l'expérience intégrée `trade_rebalance` (103 fichiers JSON exclus des comptes vanilla) et relevé `integrated_experiments` (`minecart_improvements` 0, `redstone_experiments` 0, `trade_rebalance` 103). Recherche des mots-clés pack.mcmeta, datapack, tag, replace, required, overlay, namespace, feature flag dans les cinq rapports : aucun autre passage sur le chargement. Seuls les comptes de ce rapport sont repris, et ils sont recalculés (R42).
- **Sondes de l'oracle** : aucune exécutée (serveur non exécutable, EULA non acceptée). Elles sont décrites en fin de document (P1 à P24).

Convention de preuve : **D** = valeur lue telle quelle dans un fichier extrait ; **C** = propriété établie par un calcul reproductible sur ces fichiers ; **M** = non utilisé ; **S** = documentation publique seule, rapport local ou hypothèse. Une règle S porte un comportement **provisoire** à implémenter tant que la sonde citée n'a pas tranché : une règle S énonce elle-même le comportement provisoire. **C sur S** = règle énoncée par la documentation publique (S) et seulement recoupée par un calcul sur les données (C) ; elle reste à confirmer par une sonde, mais aucun fichier extrait ne la contredit.

## Règles

### Versions et pack.mcmeta

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Version de format de la cible : datapack 121.0 (majeure 121, mineure 0), pack de ressources 97.1. La documentation publique donne les mêmes valeurs pour 26.3. | D |
| R2 | Les trois `pack.mcmeta` des packs intégrés ont la forme `{"features": {"enabled": [<identifiant>]}, "pack": {"description": {"translate": "dataPack.<nom>.description"}, "max_format": 121, "min_format": 121}}`. Ni `pack_format`, ni `supported_formats`, ni `filter`, ni `overlays`. `description` est un composant de texte (ici un objet `translate`). | D |
| R3 | Le pack vanilla n'a pas de `pack.mcmeta` dans les données extraites : c'est un pack implicite, de description `dataPack.vanilla.description` ("The default data for Minecraft"). Provisoire : notre chargeur le construit sans fichier de métadonnées. | D (absence) ; S (traitement) |
| R4 | Champs reconnus de `pack.mcmeta` : `pack.description` (composant de texte), `pack.min_format` et `pack.max_format` (exigés en 121), `pack.pack_format` (entier, facultatif), `pack.supported_formats` (ancien), `features.enabled` (liste d'identifiants de flags), `filter.block` (liste de `{namespace?, path?}`, expressions régulières), `overlays.entries` (liste de `{directory, min_format, max_format, formats?}`), `language` (packs de ressources seulement). Provisoire : un champ inconnu, à tout niveau d'imbrication (`pack`, `features`, `filter`, entrées de `overlays`, racine), est ignoré sans diagnostic (cas particulier de R63) ; un champ connu de mauvais type rend le pack cassé (même état que R5) avec un diagnostic `CODEC_TYPE_MISMATCH` au chemin du champ. | S |
| R5 | Valeur de format : un entier `N` ou `[N]` vaut `(N, 0)` pour `min_format` et `(N, 2147483647)` pour `max_format` ; `[majeure, mineure]` est exact. Un pack est compatible si `min_format <= (121, 0) <= max_format` en ordre lexicographique (majeure puis mineure), et `(97, 1)` pour un pack de ressources. Incompatible si `max_format` est plus petit ("made for an older version") ou `min_format` plus grand ("made for a newer version"). Provisoire : `min_format > max_format`, champ mal typé ou `min_format`/`max_format` absents (même avec `pack_format` seul) donnent l'état "broken or incompatible" (`pack.incompatible.unknown`). | S |
| R6 | Un pack incompatible reste listé parmi les packs disponibles. Provisoire : il n'est jamais activé automatiquement ; une activation explicite est acceptée avec un diagnostic d'avertissement. | S |
| R7 | Un dossier sans `pack.mcmeta`, ou dont le `pack.mcmeta` n'est pas un JSON objet avec un objet `pack`, n'est pas un pack. Provisoire : il est ignoré avec un diagnostic, les autres packs se chargent. | S |

### Arborescence et fichiers

| Réf | Règle | Preuve |
|---|---|---|
| R8 | Un élément de registre est le fichier `data/<namespace>/<dossier>/<chemin>.json` ; il a l'identifiant `<namespace>:<chemin>` dans le registre `minecraft:<dossier>`. Le dossier est le chemin de l'identifiant du registre et peut contenir `/` (`worldgen/biome`). Le chemin de l'élément peut contenir des sous-dossiers (exemple : 279 recettes sur 2042, dont `recipe/brewing/`). La règle vient de la documentation publique ; aucun fichier extrait ne l'énonce. Recoupement : les 9777 fichiers du pack vanilla hors `datapacks/` et hors marqueur de R13 sont tous attribués par cette règle et R9, R10 (0 non attribué), et les identifiants ainsi dérivés satisfont toutes les références de tags (R33). | C sur S |
| R9 | Un tag est le fichier `data/<namespace>/tags/<dossier>/<chemin>.json` ; il a l'identifiant `<namespace>:<chemin>`, noté `#<namespace>:<chemin>` là où un tag est attendu. Les dossiers de tags portent le nom du registre au singulier (`tags/block`, `tags/item`, `tags/worldgen/biome`). Règle de la documentation publique, recoupée : les 883 fichiers de `tags/` tombent tous dans un dossier de registre de `datapack.json`, et les 510 références `#` ainsi dérivées sont toutes résolues (R33). | C sur S |
| R10 | Contenus hors registres (`datapack.json`, section `others`) : `data/<ns>/function/**.mcfunction` (texte, tags admis dans `tags/function/`) et `data/<ns>/structure/**.nbt` (NBT, sans tags). Pack vanilla : 0 fonction, 1511 structures. | D |
| R11 | Le dossier d'un registre est attribué par le plus long préfixe de dossier connu (aucun dossier de la cible n'est préfixe d'un autre, mais `worldgen/...` impose de comparer des chemins à plusieurs segments). | C |
| R12 | Extensions : `.json` pour les éléments et les tags, `.mcfunction`, `.nbt`, et `pack.mcmeta` à la racine du pack. Aucune autre extension dans les données de la cible (8369 `.json`, 1511 `.nbt`, 3 `.mcmeta` sous `data/`). Provisoire : un fichier d'une autre extension dans un dossier de registre est ignoré sans erreur. | D (comptes) ; S (traitement) |
| R13 | Les données extraites contiennent un fichier vide `.mcassetsroot` à la racine de `data/` et de `assets/`. C'est un marqueur d'extraction, pas un fichier de pack : le chargeur l'ignore. | D |
| R14 | Les packs intégrés sont rangés dans `data/minecraft/datapacks/<nom>/`, chacun avec son `pack.mcmeta` et son arborescence `data/`. Ce dossier n'est pas un registre : le chargeur ne le parcourt jamais comme contenu du pack vanilla (sinon les comptes de `data-registries-inventory.md` sont faux). | D |
| R15 | Seuls les registres marqués `elements: true` dans `datapack.json` (60) prennent des fichiers d'éléments ; les 95 registres statiques (`elements: false`) ne prennent que des tags. Provisoire : un fichier sous `data/<ns>/block/` (registre statique) ou sous un dossier inconnu est ignoré avec un diagnostic d'information, sans échec. | D (liste) ; S (traitement) |
| R16 | Tous les registres de `datapack.json` acceptent des tags (`tags: true` pour les 155) ; `function` aussi, `structure` non. Provisoire : un fichier sous `tags/structure/` est ignoré avec un diagnostic. | D (liste) ; S (traitement) |
| R17 | `stable: false` marque 52 registres à éléments ; `stable: true` les 8 autres (`advancement`, `context_float_provider`, `context_int_provider`, `item_modifier`, `loot_table`, `predicate`, `recipe`, `slot_source`). La documentation publique ne marque comme non expérimentaux que ces 8 dossiers, plus `function`, `structure` et `tags`, et dit qu'un fichier valide dans un dossier expérimental fait compter le pack comme "utilisant des réglages expérimentaux". Provisoire : le chargeur calcule ce drapeau par pack, sans effet sur le chargement. | D (propriété) ; S (effet) |
| R18 | Pack de ressources : `assets/<namespace>/<type>/<chemin>.<ext>` et `pack.mcmeta` (même schéma, plus `language`). Les données extraites ne contiennent que `assets/minecraft/lang/en_us.json` et `deprecated.json`. Le détail des types d'assets relève du client (M9). | D (contenu) ; S (structure) |
| R19 | Packs sous forme d'archive `.zip` (même arborescence à la racine de l'archive). Provisoire : acceptés comme les dossiers. | S |

### Identifiants

| Réf | Règle | Preuve |
|---|---|---|
| R20 | Forme : `namespace:path`, au plus un `:`. Namespace : `a-z 0-9 _ - .` ; path : les mêmes plus `/`. Pas de majuscule, et aucune normalisation : `foo:Bar` est invalide, pas réécrit. Le namespace `..` est invalide (depuis 26.1 selon la documentation). | S |
| R21 | Toutes les données de la cible respectent R20 sans `-` ni `.` : les 9883 chemins de fichiers sous `data/` n'utilisent que `a-z 0-9 _ /` avant l'extension (plus long chemin depuis `resources/`, `data/` compris : 139 caractères), et les entrées de tags ont toutes le namespace explicite `minecraft:` : 6231 dans le pack vanilla, 115 dans `trade_rebalance`, soit 6346 pour l'ensemble des données de la cible. | C |
| R22 | Lecture d'un identifiant écrit dans une valeur JSON : sans `:`, le namespace est `minecraft` (`stone` donne `minecraft:stone` ; `minecraft/villager` donne `minecraft:minecraft/villager`) ; un `:` initial aussi (`:stone` donne `minecraft:stone`). Provisoire : chemin vide (`foo:`, `:` ou chaîne vide) accepté syntaxiquement, sans élément correspondant. | S |
| R23 | Référence de tag : `#` suivi d'un identifiant lu par R22 (`#logs` vaut `#minecraft:logs`). Le `#` n'est admis que là où un tag est attendu. | S |
| R24 | L'identifiant dérivé d'un chemin (R8, R9) a le namespace du dossier sous `data/` et le chemin sans l'extension ; le séparateur est toujours `/`, quel que soit le système de fichiers. Provisoire : un fichier dont le namespace ou le chemin viole R20 (majuscule, espace, `..`) est ignoré avec un diagnostic, le reste du pack se charge. | C sur S (dérivation, voir R8) ; S (traitement) |
| R25 | Deux identifiants sont égaux si leurs chaînes complètes `namespace:path` sont égales après R22. | S |

### Registres

| Réf | Règle | Preuve |
|---|---|---|
| R26 | Registres statiques : les 95 de `registries.json`, chacun avec un `protocol_id` (0 à 94, tous distincts), ses entrées et, pour 10 d'entre eux, une entrée `default`. Dans chaque registre, les `protocol_id` des entrées sont exactement 0 à n-1 (7053 entrées en tout). Le chargeur M2 peuple ces registres depuis ce fichier, dans l'ordre des `protocol_id`. | D |
| R27 | Les 95 registres statiques sont exactement les 95 registres `elements: false` de `datapack.json`. Les 60 registres à éléments n'apparaissent pas dans `registries.json`. | C |
| R28 | Registres à éléments sans aucun fichier vanilla : `dimension`, `item_modifier`, `slot_source`. Le registre existe, vide, et un pack peut y ajouter des éléments. | C |
| R29 | Un élément de registre à fichiers peut être un JSON de n'importe quel type à la racine : 8363 objets, 3 tableaux (`block_transformer/axe`, `hoe`, `shovel`), 4 nombres décimaux (`context_float_provider/brewing/speed_default` = `1.0`, `cooking/fast_speed_multiplier` = `2.0`, `cooking/normal_speed_multiplier` = `1.0`, `worldgen/density_function/zero` = `0.0`) et 4 entiers (`context_int_provider/brewing/uses_default` = `20`, `compostable/always_add_one` = `1`, `cooking/fast_burn_time_reduction_factor` = `2`, `cooking/normal_burn_time_reduction_factor` = `1`). | C |
| R30 | Ordre des éléments d'un registre à fichiers (utile au protocole et à toute itération). Provisoire : ordre lexicographique des identifiants complets (points de code), indépendant de l'ordre des packs et du système de fichiers. | S |

### Tags

| Réf | Règle | Preuve |
|---|---|---|
| R31 | Les 883 fichiers de tags vanilla sont des objets à la seule clé `values`, liste de chaînes : 5721 identifiants d'éléments, 510 références `#`. Les 17 tags de `trade_rebalance` : 7 de même forme, 10 avec en plus `"replace": true`. Aucune entrée objet dans les données. 10 tags vanilla ont `values: []` (liste dans l'inventaire) et sont valides. | D |
| R32 | Format complet : `replace` booléen facultatif (défaut `false`) ; `values` liste d'entrées ; une entrée est une chaîne (identifiant ou `#tag`) ou un objet `{"id": <chaîne>, "required": <booléen>}`, `required` valant `true` par défaut. Provisoire : `values` absent, `values` ou `replace` de mauvais type, entrée d'un autre type que chaîne ou objet, entrée objet sans `id` de type chaîne ou avec `required` non booléen rendent le fichier invalide (diagnostic `CODEC_TYPE_MISMATCH` ou `CODEC_MISSING_FIELD`, le fichier compte comme absent). Une clé inconnue, au niveau du fichier comme dans une entrée objet, est ignorée sans diagnostic (R63 ; cette version remplace la rédaction précédente qui la refusait, voir `data-decisions-log.md` D6). Un identifiant ou une référence `#` d'une entrée qui viole R20 rend le fichier invalide (`CODEC_INVALID_IDENTIFIER`). | S |
| R33 | Univers d'un tag : pour un registre statique, les entrées de `registries.json` ; pour un registre à fichiers, les identifiants des éléments après empilement de tous les packs actifs (contenu non décodé) ; pour `function`, les fonctions chargées. Les références `#` visent un tag du même registre. Pack vanilla seul : 0 référence manquante, 0 cycle, aucun tag en échec, dans les 20 registres qui ont des tags. | S (règle) ; C (vanilla) |
| R34 | Fusion entre packs, dans l'ordre de priorité croissante (R40) : pour un même identifiant de tag, un fichier avec `replace: true` remplace toutes les entrées accumulées des packs précédents ; sinon ses entrées sont ajoutées à la suite. La fusion porte sur les entrées brutes, avant résolution. | S |
| R35 | Résolution : parcours des entrées dans l'ordre du fichier fusionné, une référence `#` étant remplacée par le contenu résolu du tag visé, en profondeur. Provisoire : un élément déjà présent n'est pas répété (la première occurrence garde sa place). L'ordre résolu est conservé : il compte pour les tags de fonctions et pour plusieurs tags d'objets ou de presets selon la documentation. | S |
| R36 | Entrée requise (`required` vrai, ou chaîne) introuvable dans l'univers, ou tag visé introuvable : le tag entier échoue et n'existe pas. Entrée `required: false` introuvable : seule cette entrée est ignorée. Provisoire : un tag qui requiert un tag en échec échoue à son tour ; le chargement des autres tags et des éléments continue ; le diagnostic liste chaque référence manquante. | S |
| R37 | Cycle de références entre tags : échec de chargement. Provisoire : chaque tag du cycle échoue, ainsi que les tags qui en dépendent par R36 ; le diagnostic donne le cycle. | S |
| R38 | Un tag vide (`values: []`) existe et ne contient rien ; il se distingue d'un tag absent (une commande sur un tag absent échoue par "Unknown block tag '%s'", "Can't find tag '%s' of type '%s'"...). | D (vanilla, messages) ; S (distinction observée) |
| R39 | Vecteurs vanilla (ensembles distincts après expansion) : `#minecraft:logs` 48 blocs, `#minecraft:wool` 16, `#minecraft:mineable/pickaxe` 542, `#minecraft:planks` (objets) 13, `#minecraft:water` 2, `#minecraft:lava` 2, `#minecraft:is_overworld` 56 biomes, `#minecraft:non_treasure` 36 enchantements. | C |

### Empilement, packs intégrés, feature flags

| Réf | Règle | Preuve |
|---|---|---|
| R40 | Ordre des packs : le pack vanilla en premier (priorité la plus basse), puis les packs activés dans l'ordre enregistré dans le monde (découverte des candidats d'un dossier `datapacks/` : R67) ; le dernier a la priorité la plus haute. `/datapack enable <nom>` admet `first`, `last`, `before <existant>`, `after <existant>`. Provisoire : identifiants de packs `vanilla`, `minecart_improvements`, `redstone_experiments`, `trade_rebalance`, et `file/<nom du dossier ou de l'archive>` pour les packs du monde ; sources affichées `built-in`, `feature`, `world`. | D (syntaxe de commande, textes `pack.source.*`) ; S (ordre, identifiants) |
| R41 | Remplacement : pour un même registre et un même identifiant, seul le fichier du pack de plus haute priorité est retenu, en entier ; il n'y a pas de fusion de contenu JSON entre packs. Vaut aussi pour `function` et `structure`. | S |
| R42 | Packs intégrés de la cible : `minecart_improvements` et `redstone_experiments` (0 fichier de données, seulement un `pack.mcmeta` qui active leur flag), `trade_rebalance` (86 éléments, 17 tags). Les "103 fichiers d'overlay expérimental" du rapport Overworld sont ces 86 + 17 fichiers de `trade_rebalance` : c'est un pack intégré ordinaire, pas un overlay au sens de `pack.mcmeta` (aucun pack de la cible n'utilise `overlays`). | C |
| R43 | Effet de `trade_rebalance` empilé au-dessus du pack vanilla : il remplace 5 tables de butin (`chests/abandoned_mineshaft`, `chests/ancient_city`, `chests/desert_pyramid`, `chests/jungle_temple`, `chests/pillager_outpost`) et 1 échange (`librarian/2/book_emerald`), ajoute 80 échanges (`villager_trade` passe de 391 à 471), remplace par `replace: true` 10 tags d'échanges (`armorer/level_1..5`, `librarian/level_1..5`) et ajoute 7 tags d'enchantements (`trades/<biome>_common`, `enchantment` passe de 22 à 29 tags). Vecteurs : `#minecraft:armorer/level_1` passe de 5 à 2 entrées, `#minecraft:librarian/level_1` de 3 à 9 ; 0 référence manquante, 0 cycle. | C (sous R34, R41) |
| R44 | Feature flags connus des données : `minecraft:minecart_improvements`, `minecraft:redstone_experiments`, `minecraft:trade_rebalance` (via `features.enabled` des packs intégrés). Aucun fichier `.json` de la cible ne porte de condition de flag. Méthode : recherche des clés `required_features`, `requires_features`, `feature_flags` (0 fichier), et relevé de toutes les clés `features` sous `data/` : 3 objets avec `enabled` (les 3 `pack.mcmeta`), et dans les `.json` seulement des listes (123), des booléens (13) ou un objet de feature worldgen (1, `worldgen/feature/nylium_bonemeal`), tous dans `worldgen/biome`, `worldgen/feature`, `worldgen/flat_level_generator_preset` et `worldgen/world_preset`. Commandes, depuis `$REF/resources` : `grep -rlE --include='*.json' --include='*.mcmeta' '"(required_features|requires_features|feature_flags)"' . \| wc -l` (attendu `0`) et `grep -rl --include='*.json' '"features"' data \| sed 's#/[^/]*$##' \| sort \| uniq -c` (attendu : 67 `worldgen/biome`, 41 `worldgen/feature`, 6 `worldgen/feature/coral`, 9 `worldgen/flat_level_generator_preset`, 2 `worldgen/world_preset`). | D (flags des `pack.mcmeta`) ; C (absence) |
| R45 | Ensemble des flags d'un monde (créé et modifié par `game` et les commandes, jalon M8 ; le chargeur M2 le reçoit en entrée, R66) : provisoire, `minecraft:vanilla` plus l'union des `features.enabled` des packs activés à la création du monde. Les packs à flags sont désactivés par défaut. Un pack dont les flags ne sont pas tous actifs ne peut pas être activé ensuite ("Pack '%s' cannot be enabled, since required flags are not enabled in this world: %s!") ; un pack qui fait partie d'un flag actif ne peut pas être désactivé ("cannot be disabled, since it is part of an enabled flag!") ; les expériences ne se désactivent pas après la création. | D (textes) ; S (règle) |
| R46 | Le filtrage d'entrées de registres statiques par flag (blocs ou objets réservés à une expérience) n'est pas visible dans `registries.json`, qui liste toutes les entrées. Il relève des modules propriétaires et de `game`, pas de M2. | D (contenu de `registries.json`) ; S (portée) |
| R47 | `filter.block` : un fichier d'un pack de priorité plus basse dont le namespace et le chemin correspondent aux deux expressions (une expression absente correspond à tout) est traité comme absent ; le filtre n'agit pas sur le pack qui le porte. Provisoire : correspondance complète de l'expression ; le chemin testé est le chemin sous `data/<namespace>/`, extension comprise (`loot_table/blocks/dirt.json`). | S |
| R48 | Overlays : chaque entrée de `overlays.entries` dont l'intervalle `[min_format, max_format]` contient `(121, 0)` est active ; son dossier, à la racine du pack, est une arborescence de même forme que la racine (`<dossier>/data/...`) dont les fichiers remplacent ceux du pack de même chemin ; les entrées s'appliquent dans l'ordre de la liste, une entrée plus loin l'emportant. `pack.mcmeta` et `pack.png` d'un dossier d'overlay sont ignorés. Nom de dossier : `a-z 0-9 _ - .` (la documentation se contredit sur `A-Z`). Provisoire : un tag d'overlay remplace le tag du pack de même chemin (pas de fusion interne au pack). | S |

### Analyse JSON

| Réf | Règle | Preuve |
|---|---|---|
| R49 | Les 8374 fichiers `.json` et `.mcmeta` extraits sont du JSON strict (RFC 8259), en UTF-8 sans BOM, fins de ligne LF, sans clé en double, sans `null`, et sans aucune chaîne vide hors `en_us.json` (2 valeurs vides : clés `potion.potency.0` et `selectWorld.gameMode.spectator.line2`). Un seul fichier contient des caractères non ASCII (`assets/minecraft/lang/en_us.json`). Profondeur maximale : 17 objets ou tableaux imbriqués, racine comprise ; plus gros fichier de données 60159 octets (`block_transformer/axe.json`). | C |
| R50 | Nombres, sur les mêmes 8374 fichiers : 15391 littéraux entiers, dont 15385 dans les `.json` et 6 dans les `.mcmeta` (`min_format` et `max_format` des 3 packs intégrés) ; valeur absolue maximale 165745296 ; 6456 décimaux, 53 en notation exponentielle (5 formes distinctes : `1.0E-4`, `1.7976931348623157E308`, `2.9802322E-8`, `5.9604645E-8`, `9.999999747378752E-6`). 45 décimaux distincts ont 16 ou 17 chiffres significatifs (25 et 20) (exemple `-0.10000000149011612`, valeur binary32 écrite en binary64). Aucun `-0`. Le lecteur conserve, pour chaque nombre, s'il est écrit en entier ou en décimal (R29 : `1` et `1.0` sont des fichiers de registres différents) et sa valeur exacte : conversion binary64 correctement arrondie pour les décimaux (`1.7976931348623157E308` donne le plus grand double fini), entier exact sur 64 bits signés. Classification, bornes, `-0` et débordements : R59. | C |
| R51 | Clé en double dans un objet. Provisoire : la dernière valeur l'emporte, avec un diagnostic d'avertissement. Détail (position du membre, nombre d'avertissements, égalité des clés) : R58. | S |
| R52 | Syntaxe hors RFC 8259 (commentaires `//` ou `/* */`, virgule finale, guillemets simples, clé ou chaîne sans guillemets, `NaN`, `Infinity`, nombres `+1`, `.5`, `01`). Provisoire : refusée, le fichier est invalide. Si la sonde montre que l'oracle en accepte, le lecteur doit accepter exactement les mêmes formes. | S |
| R53 | BOM UTF-8 en tête, fichier vide ou fait seulement de blancs, contenu non blanc après la valeur racine, octets UTF-8 invalides. Provisoire : fichier invalide. | S |
| R54 | Fichier invalide (R51 exclu) : diagnostic, et l'élément ou le tag est absent comme s'il n'avait pas été fourni. Il ne remplace pas la version d'un pack de priorité plus basse. | S |
| R55 | Gravité d'un fichier invalide ou d'un tag en échec, provisoire : au chargement d'un monde, une erreur dans un registre `stable: false` (registres de monde) ou dans un tag empêche le monde de se charger ("Errors in the currently selected data pack(s) prevented the world from loading", mode sans échec = pack vanilla seul) ; une erreur dans un registre `stable: true` (butins, recettes, avancements, prédicats...) écarte seulement cet élément. Un rechargement en échec garde les anciennes données ("Reload failed; keeping old data"). | D (textes) ; S (répartition) |
| R56 | Valeurs absentes ou par défaut : le chargeur M2 n'en connaît aucune. Les combinateurs distinguent champ requis (absent : erreur avec chemin JSON), champ facultatif avec défaut (absent : défaut) et champ présent de mauvais type (erreur, jamais le défaut). Provisoire : une valeur `null` présente est une erreur de type (aucun `null` dans les données). Les règles de type des combinateurs (entier, décimal, booléen, chaîne, liste) sont R62, celles des champs inconnus R63, celles des ensembles d'éléments R64. | S |

### Lecture JSON : compléments (décisions sur les ambiguïtés du squelette)

Ces règles précisent R49 à R56 ; la trace des décisions et des alternatives écartées est dans `data-decisions-log.md`. Aucune n'est observable sur la cible par les données extraites (les 8374 fichiers n'exercent aucun de ces cas, sauf la profondeur 17) : elles restent S, avec leur comportement provisoire, jusqu'aux sondes P18 à P20.

| Réf | Règle | Preuve |
|---|---|---|
| R57 | Profondeur maximale, provisoire : 255 niveaux, racine comprise. Chaque `{` ou `[` ouvre un niveau, conteneur vide compris. Un texte de 255 niveaux est accepté ; l'ouverture du 256e niveau rend le fichier invalide (`JSON_TOO_DEEP`) avec pour chemin celui du 256e conteneur (R61). Le refus a lieu à cette ouverture, sans lire la suite, et ne doit pas épuiser la pile quelle que soit la profondeur du texte (100 000 niveaux compris) ; un seul diagnostic. La cible n'utilise que 17 niveaux (R49) : 255 est le défaut de la bibliothèque retenue (ADR 0006), pas une mesure de l'oracle. Si P18 mesure une autre limite, elle la remplace. | S |
| R58 | Clé en double (précise R51), provisoire. (a) Les clés sont égales si leurs chaînes décodées (après échappements, R60) sont égales unité UTF-16 par unité UTF-16, sans normalisation ni pliage de casse : `"a"` et `"a"` sont la même clé, `"a"` et `"A"` non. (b) Le membre garde la **place de sa première occurrence** dans l'objet et reçoit la **valeur de la dernière** : `{"a":1,"b":2,"a":3}` donne les membres `a=3`, `b=2` dans cet ordre. (c) Un avertissement `JSON_DUPLICATE_KEY` par occurrence en trop (trois occurrences, deux avertissements), au chemin du membre (R61), dans l'ordre de lecture du fichier ; les doublons à l'intérieur d'une valeur écartée par une occurrence ultérieure sont signalés aussi, à leur place de lecture (le fichier est lu séquentiellement), donc avant l'avertissement de la clé répétée qui écarte cette valeur. (d) Le fichier reste valide. (e) Si le fichier est par ailleurs invalide, seul le diagnostic d'erreur est émis, pas les avertissements (le fichier est écarté, R54). Le choix (b) est une décision d'architecture : la place n'est observable ni dans les données, ni, à notre connaissance, par une sonde ; il reprend la convention des structures associatives à ordre d'insertion courantes (la clé garde sa place, la valeur est remplacée), et sert le déterminisme (ARCHITECTURE). La valeur retenue (dernière) reste S, sonde P11. | S |
| R59 | Nombres (précise R50), provisoire. (a) Un littéral est **entier** s'il a la forme `-?(0\|[1-9][0-9]*)` sans point ni exposant, **décimal** dans tous les autres cas valides (`1.0`, `1e5`, `1E+5`, `-0.0` sont décimaux). Un nombre mal formé (`+1`, `.5`, `1.`, `01`, `-`, `1e`) est une erreur de syntaxe `JSON_SYNTAX` (R52). (b) Un entier doit tenir dans `[-2^63, 2^63-1]` ; sinon le fichier est invalide, `JSON_NUMBER_RANGE`, au chemin du nombre. (c) `-0` entier est **valide** : entier de valeur 0 (un entier 64 bits n'a pas de zéro négatif ; le signe est perdu, `-0` et `0` sont des nœuds égaux). (d) Un décimal est converti en binary64 au plus proche (égalité vers le pair) ; si le résultat est infini (`1e999`, `-1e999`, `1.7976931348623159E308`), le fichier est invalide, `JSON_NUMBER_RANGE` ; un dépassement vers zéro (`1e-999`, `2E-324`) est **valide** et vaut `0.0` ou `-0.0` ; les dénormalisés sont valides (`4.9E-324` est le plus petit double positif). (e) Le zéro décimal garde son signe : `-0.0` et `0.0` sont des valeurs distinctes (égalité des décimaux par motif de bits, comme `core-nbt.md` R35 et R37). Un entier et un décimal ne sont jamais égaux : `1` et `1.0` sont des nœuds différents (R29). (f) Aucune limite sur le nombre de chiffres en dehors de (b) et (d) : une mantisse de 400 chiffres est convertie avec l'arrondi correct. Aucun fichier de la cible n'a un entier hors 64 bits, un `-0` ou un débordement (R50) : le risque de se tromper est nul pour M2 et s'exerce seulement sur des packs tiers ; l'option "accepter un entier trop grand comme décimal" est écartée tant que P19 n'a pas montré que l'oracle le fait (décision de repli dans le journal des décisions). | S (décision) ; C (cible sans occurrence) |
| R60 | Chaînes et échappements, provisoire. (a) Échappements admis : `\"` `\\` `\/` `\b` `\f` `\n` `\r` `\t` et `\uXXXX` (4 chiffres hexadécimaux, majuscules ou minuscules) ; tout autre échappement, `\u` tronqué ou à chiffre non hexadécimal, et tout caractère de contrôle brut (U+0000 à U+001F) dans une chaîne sont `JSON_SYNTAX`. (b) `\u0000` est valide (la chaîne contient U+0000). (c) Un substitut UTF-16 isolé écrit par échappement (`\ud800`, `\udc00`, ou une paire dans le mauvais ordre `\ude00\ud83d`) est **accepté** : la chaîne est une séquence d'unités UTF-16 et contient ces unités telles quelles, sans diagnostic. Une paire correcte (`😀`) forme le point de code U+1F600. (d) Les mêmes règles valent pour les clés. (e) À l'inverse, un substitut encodé dans les **octets** (séquence `ED A0 80` à `ED BF BF`) est de l'UTF-8 invalide (`JSON_UTF8`, R53), de même que les formes trop longues (`C0 80`), les points de code au-delà de U+10FFFF (`F4 90 80 80`) et les octets orphelins ; les points hors plan multilingue en 4 octets (`F0 9F 98 80`) et les non-caractères (`EF BF BE`) sont valides. (f) Conséquence pour tout code qui écrit ou affiche ces chaînes (diagnostics, journaux, futur encodeur, R65) : une unité isolée ne peut pas être encodée en UTF-8 ; elle est écrite en échappement `\uXXXX`, jamais remplacée silencieusement. Une chaîne à substitut isolé n'est un identifiant valide nulle part (R20). | S |
| R61 | Chemins JSON et codes de diagnostic. (a) Format du chemin d'un diagnostic : `$` pour la racine, `.nom` pour un membre d'objet (nom tel que décodé, sans échappement ni guillemets), `[i]` pour un élément de tableau (i à partir de 0) ; exemples `$`, `$.a.b`, `$.values[2].id`. Format de convention du dépôt, non observable. (b) Chaque diagnostic porte un **code** stable parmi le tableau "Codes de diagnostic" ; les tests vérifient le code, la gravité, le fichier et le chemin, jamais le texte du message. Pour `JSON_SYNTAX`, `JSON_BOM`, `JSON_EMPTY`, `JSON_TRAILING` et `JSON_UTF8` le chemin est indicatif et n'est pas vérifié par les tests (sauf mention contraire). (c) Un fichier invalide donne **un seul** diagnostic d'erreur, le premier rencontré, avec la précédence : BOM, puis octets UTF-8 invalides (sur tout le fichier), puis erreurs de lecture dans l'ordre du texte. Un fichier vide ou blanc donne `JSON_EMPTY` ; un fichier réduit à un BOM, `JSON_BOM`. (d) Si le squelette utilise d'autres noms de codes, c'est ce tableau qui fait foi pour les tests, et l'écart est signalé. | S (convention) |
| R62 | Types attendus des combinateurs scalaires (précise R56), provisoire. (a) **Décimal attendu** (double, float) : accepte un littéral entier ou décimal ; un entier est converti en binary64 au plus proche (`9007199254740993` donne `9007199254740992.0`). Pour un float (binary32) : conversion de la valeur binary64 au plus proche, résultat infini (`1e40`) = `CODEC_NUMBER_RANGE`. (b) **Entier attendu** (int 32 bits, long 64 bits) : n'accepte que la forme entière ; un décimal, même de valeur entière (`1.0`, `1e0`, `-0.0`), est `CODEC_TYPE_MISMATCH` ; une valeur hors des bornes du type (int : `[-2^31, 2^31-1]`) est `CODEC_NUMBER_RANGE` ; `-0` vaut 0. (c) **Booléen attendu** : seulement `true` ou `false` ; `0`, `1`, `"true"` sont `CODEC_TYPE_MISMATCH`. (d) **Chaîne attendue** : seulement une chaîne. (e) **Liste attendue** : seulement un tableau ; une valeur isolée n'est pas promue en liste (l'exception est R64, propre aux ensembles). (f) Type JSON différent de celui attendu, `null` compris : `CODEC_TYPE_MISMATCH` au chemin de la valeur. (g) Une erreur dans un combinateur n'arrête pas la lecture des autres champs de l'objet : tous les diagnostics du fichier sont collectés, dans l'ordre du texte. Les bornes plus fines (plages métier) sont à la charge du module propriétaire (K3). Le côté strict de (b) est choisi parce qu'une erreur sur une donnée vanilla se voit immédiatement (critère de sortie "zéro erreur" des modules propriétaires), alors qu'une tolérance de trop ne se voit pas ; P21 tranche. | S |
| R63 | Champs inconnus d'un objet, règle générale des combinateurs, provisoire. (a) Par défaut un champ inconnu est **ignoré**, sans diagnostic. (b) Le combinateur d'objet offre un mode **strict** : chaque champ inconnu donne un diagnostic d'erreur `CODEC_UNKNOWN_FIELD` (un par champ, dans l'ordre du texte, chemin `$.champ`), les autres champs étant décodés normalement. Le mode est choisi par le module propriétaire du codec dans sa spec ; le défaut reste (a). (c) Dans M2, **aucune exception** : `pack.mcmeta` (R4) et les fichiers de tags (R32, entrées objet comprises) ignorent les champs inconnus. (d) Éléments de registre : M2 ne les décode pas (K3), aucune règle ici ; chaque module propriétaire déclare ses éventuels objets stricts. Fondement : la documentation publique ne donne pas de règle générale ; elle signale seulement qu'un décodeur précis (les prédicats d'entité) a cessé d'ignorer les champs inconnus en 26.2, ce qui plaide pour une politique par décodeur et non globale ; le défaut "ignoré" est l'hypothèse la moins coûteuse (se tromper dans ce sens accepte un pack que le jeu refuserait, se tromper dans l'autre refuse un pack que le jeu accepte et, par R55, peut empêcher un monde de se charger). Aucune donnée extraite ne contient de champ inconnu pour les schémas de M2 (R2, R31). Sonde P22. | S |
| R64 | Ensemble d'éléments ("holder set") : champ qui désigne des éléments d'un registre, provisoire. Trois formes, selon le type JSON : (a) **chaîne sans `#`** : un seul élément, l'identifiant lu par R22 (`"stone"` = `minecraft:stone`) ; (b) **chaîne commençant par `#`** : référence à un tag du même registre, identifiant lu par R23 (`"#logs"` = `#minecraft:logs`) ; (c) **tableau de chaînes** : liste en ligne d'éléments ; chaque chaîne est un identifiant d'élément, jamais un `#tag` (un `#` dans un tableau donne `CODEC_INVALID_IDENTIFIER` au chemin `$[i]`) ; tableau vide valide (ensemble vide) ; un élément répété est conservé une seule fois, à la place de sa première occurrence, sans diagnostic. Tout autre type (objet, nombre, booléen, `null`) : `CODEC_TYPE_MISMATCH`. Un identifiant (a ou c) absent du registre, ou un tag (b) absent de l'univers de R33, donne `CODEC_UNRESOLVED_REFERENCE` (erreur) au chemin du champ ou de l'élément du tableau, émis quand l'univers est connu (K3 : c'est le module propriétaire qui l'appelle ; la forme du diagnostic est celle de M2). Un tag en échec (R36) compte comme absent. Le combinateur de référence unique (un seul élément attendu, "holder") n'accepte que (a) : `#tag` y est `CODEC_INVALID_IDENTIFIER`. Distinction à ne pas confondre avec les fichiers de tags (R32), où une entrée peut être `#tag` ou objet et où le tableau accepte les deux. Source : documentation publique (champ `supported_items` d'une définition d'enchantement : identifiant seul, `#tag` ou tableau d'identifiants) ; tout le reste est une hypothèse. Sonde P23. | S |

### Encodage, feature flags et découverte des packs

| Réf | Règle | Preuve |
|---|---|---|
| R65 | Encodage (écriture JSON ou NBT des éléments, synchronisation des registres en phase de configuration, jalon M8). Décision d'architecture : **les combinateurs de M2 sont à sens unique (décodage)**. M2 ne livre ni encodeur, ni écrivain JSON. Pour ne pas imposer de refactor plus tard : (a) chaque élément chargé conserve son arbre JSON immuable et l'identité de son pack d'origine (déjà exigés par "Entrées / sorties") ; (b) les combinateurs sont des valeurs (descriptions de structure) et non des fonctions de décodage opaques, de sorte qu'ajouter une capacité d'encodage soit une extension de l'interface des combinateurs et non une réécriture de leurs utilisateurs ; (c) aucun module propriétaire n'écrit d'encodeur avant qu'une spec M8 ne l'exige. Chemin prévu pour M8, à confirmer par une spec dédiée et un client vanilla (ADR 0002, 2c) : l'arbre JSON retenu est converti en NBT par une conversion générique, et, selon la documentation publique, les entrées qui viennent du pack de base que le client connaît peuvent être envoyées sans données (négociation des packs connus), seules les entrées modifiées ou ajoutées demandant la conversion ; les tags sont toujours envoyés en entier. Si ce chemin produit un écart avec un client réel, l'encodage typé par combinateur sera ajouté à ce moment. | S (architecture) |
| R66 | Rôle des feature flags dans le chargement (précise R44 à R46), provisoire. (a) L'ensemble des flags actifs (`activeFlags`) est une **entrée** du chargeur, fournie par l'appelant (`game`) ; il contient toujours `minecraft:vanilla`. Le chargeur ne le calcule pas, ne l'enregistre pas, ne l'élargit pas et ne refuse ni n'impose l'activation ou la désactivation d'un pack (R45 : règles des commandes et de la création de monde, jalon M8). (b) Flags requis d'une source : la liste `features.enabled` de son `pack.mcmeta` (liste vide sans ce champ) ; le pack vanilla implicite requiert `minecraft:vanilla`. (c) Si les flags requis d'une source ne sont pas tous dans `activeFlags`, la source est **écartée** de l'empilement et un diagnostic d'erreur `PACK_FLAGS_INACTIVE` (pack, flags manquants) est émis ; les autres sources se chargent normalement. Un identifiant de flag que personne ne définit est traité de même (il n'est pas actif) ; le chargeur n'a pas de liste de flags connus. (d) Le chargeur ne vérifie pas l'inverse (un flag actif sans pack qui le porte) : c'est l'invariant de l'appelant. (e) La sortie répète `activeFlags` triés par ordre des points de code. (f) Les flags ne filtrent aucun contenu de fichier JSON dans M2 (R44, R46). La situation de (c) ne se produit pas dans le jeu (il refuse l'activation en amont, R45) : c'est une garde défensive, non observable sur l'oracle, sans sonde. | S (architecture) |
| R67 | Découverte des packs d'un dossier `datapacks/`, provisoire. (a) Candidats : les sous-dossiers directs et les fichiers directs dont le nom se termine par `.zip` (en minuscules) ; les noms commençant par `.` et tout autre fichier sont ignorés. (b) Identifiant d'un candidat : `file/<nom>` avec le nom complet (extension `.zip` comprise, R40). (c) Ordre de découverte : croissant par nom, comparé point de code par point de code (majuscules avant minuscules, `p10` avant `p2`, indépendant du système de fichiers, de la locale et de la casse du système) ; `Z` < `a` < `é`. (d) Un candidat sans `pack.mcmeta` valide est ignoré avec un diagnostic (R7). (e) La découverte ne fixe pas la priorité : l'ordre d'activation est la liste enregistrée dans le monde (R40). Un candidat compatible (R5) absent de cette liste est, provisoirement, activé à la suite, en dernière priorité, dans l'ordre de découverte ; un candidat incompatible reste disponible, non activé (R6). L'ordre (c) est un choix de déterminisme ; l'ordre réel du jeu pour plusieurs packs nouveaux n'est pas documenté publiquement. Sonde P24. | S |

### Codes de diagnostic

Liste fermée des codes cités par les règles (R61). Un diagnostic de pack ou de fichier d'un autre genre (R6, R15, R16, R24) reçoit le code `FILE_IGNORED` ; un pack écarté par R7 reçoit `PACK_INVALID`.

| Code | Gravité | Cas |
|---|---|---|
| `JSON_SYNTAX` | erreur | syntaxe hors RFC 8259 (R52), échappement ou nombre mal formé, caractère de contrôle brut, espace hors de `espace`, `\t`, `\n`, `\r` (R59, R60) |
| `JSON_BOM` | erreur | BOM UTF-8 en tête (R53) |
| `JSON_EMPTY` | erreur | fichier vide ou blanc (R53) |
| `JSON_TRAILING` | erreur | contenu non blanc après la valeur racine (R53) |
| `JSON_UTF8` | erreur | octets UTF-8 invalides (R53, R60) |
| `JSON_TOO_DEEP` | erreur | plus de 255 niveaux (R57) |
| `JSON_NUMBER_RANGE` | erreur | entier hors 64 bits ou décimal qui déborde en infini (R59) |
| `JSON_DUPLICATE_KEY` | avertissement | clé en double (R58) |
| `CODEC_TYPE_MISMATCH` | erreur | type JSON inattendu, `null` compris (R56, R62, R64) |
| `CODEC_MISSING_FIELD` | erreur | champ requis absent (R56) |
| `CODEC_UNKNOWN_FIELD` | erreur | champ inconnu d'un objet strict (R63) |
| `CODEC_NUMBER_RANGE` | erreur | nombre hors des bornes du type attendu (R62) |
| `CODEC_INVALID_IDENTIFIER` | erreur | identifiant ou référence `#` invalide (R20, R32, R64) |
| `CODEC_UNRESOLVED_REFERENCE` | erreur | élément ou tag introuvable dans un ensemble d'éléments (R64, K3) |
| `PACK_FLAGS_INACTIVE` | erreur | flags requis non actifs (R66) |
| `PACK_INVALID` | avertissement | dossier ou archive qui n'est pas un pack (R7) |
| `FILE_IGNORED` | information | fichier ignoré (R6, R15, R16, R24) |

## Entrées / sorties

- **Entrée** : liste ordonnée de sources de packs, de priorité croissante. Une source est le pack vanilla implicite (R3, racine `resources/` des données extraites, hors `data/minecraft/datapacks/`), un pack intégré (R14) ou un pack du monde (dossier ou archive, R19). Plus la version de format attendue (121.0 ou 97.1) et, pour les feature flags, l'ensemble actif `activeFlags` (R66). Découverte d'un dossier `datapacks/` : R67.
- **Arbre JSON** : objet (paires dans l'ordre de première occurrence de chaque clé, clés uniques après R51 et R58), tableau, chaîne (Unicode), nombre (forme entière ou décimale et valeur exacte, R50), booléen, `null` (accepté par le lecteur, refusé par les combinateurs, R56). Chaque nœud connaît sa source (pack, chemin de fichier) et son chemin JSON pour les diagnostics. Le choix de conserver l'ordre des clés sert le déterminisme (`docs/ARCHITECTURE.md`), pas la fidélité.
- **Sortie** :
  - pour chaque registre (statique ou à fichiers) : ses identifiants dans un ordre déterministe (R26, R30), et pour un registre à fichiers l'arbre JSON retenu de chaque élément avec son pack d'origine ;
  - pour chaque registre (et `function`) : les tags résolus (listes ordonnées sans doublon, R35) et les tags en échec ;
  - les fichiers de fonctions (texte) et de structures (octets) retenus, non interprétés ;
  - l'ensemble des feature flags actifs et, par pack, l'état de compatibilité (R5) et le drapeau expérimental (R17) ;
  - la liste des diagnostics : gravité (erreur, avertissement, information), code (R61), pack, chemin de fichier, chemin JSON, message. Le chargeur ne s'arrête pas au premier diagnostic ; c'est l'appelant (`engine/game`) qui applique R55.

## Contrat avec les modules propriétaires (ADR 0005)

Ces points sont des décisions d'architecture du dépôt, pas des règles de fidélité ; ils s'appuient sur les règles ci-dessus.

- **K1. Fourni par `engine/data`** : sources et empilement (R40, R41, R47, R48), découverte des packs d'un dossier (R67), lecture JSON (R49 à R54, R57 à R61), le type `Identifier` (R20 à R25), registres et références par identifiant ou par tag, combinateurs de codecs en décodage seulement (R65) : champs requis ou facultatifs, entier, décimal, booléen, chaîne et liste (R62), objets lenient ou stricts (R63), unions par champ `type`, identifiants, références de registre, ensembles d'éléments (R64), résolution des tags (R31 à R37), diagnostics (R61).
- **K2. Validé par le chargeur M2 seul** :
  - `pack.mcmeta` : forme, compatibilité (R4 à R7), flags (R66) ;
  - attribution de chaque fichier à un registre et identifiant valide (R8 à R16, R20, R24) ;
  - syntaxe JSON de tous les fichiers `.json` (R49 à R54), sans connaître leur schéma ; tout type JSON est admis à la racine d'un élément (R29) ;
  - format des fichiers de tags et toutes leurs références, éléments comme tags, cycles compris (R32 à R37), contre l'univers de R33, qui ne demande que la liste des identifiants, pas le contenu décodé ;
  - comptes par registre égaux à `data-registries-inventory.md` et zéro diagnostic d'erreur sur les données de la cible (critère de sortie de M2).
- **K3. Laissé aux modules propriétaires**, chacun dans son jalon : schéma et décodage typé des éléments, défauts des champs facultatifs (R56), contraintes de valeurs (plages métier ; la distinction entier contre décimal est fournie par les combinateurs, R62), choix du mode strict d'un objet (R63), références vers d'autres registres écrites dans le corps d'un élément (identifiants et `#tags` à l'intérieur d'un biome, d'un butin, d'une recette...), filtrage par feature flag des entrées statiques (R46), interprétation des fonctions (`game`) et des structures NBT (`core` pour le format, `worldgen` pour le sens). Une référence non résolue dans un corps d'élément est un diagnostic émis par le codec propriétaire via l'infrastructure de K1, avec la même forme que ceux de M2.
- **K4. Ordre de chargement** : M2 n'impose aucun ordre de décodage entre registres ; les références sont résolues par identifiant, ce qui permet à `engine/game` d'ordonner les décodages typés librement.

## Cas limites

- Registre à fichiers sans fichier vanilla (`dimension`, `item_modifier`, `slot_source`) : registre vide mais existant (R28).
- Élément racine non objet : tableau, entier, décimal (R29) ; `1` et `1.0` dans deux registres différents.
- Tags vides (10 en vanilla) contre tags absents (R38).
- Sous-dossiers profonds dans les identifiants (`advancement`, `loot_table`, `villager_trade`, `worldgen/template_pool` : tous ou presque en sous-dossiers).
- Dossier de registre à plusieurs segments (`worldgen/...`, `tags/worldgen/biome/...`) et préfixes (R11).
- Fichier qui remplace un élément vanilla et tag `replace: true` d'un pack de priorité plus haute (R43).
- Nombres extrêmes : `1.7976931348623157E308`, `2.9802322E-8`, décimaux à 17 chiffres (R50).
- Identifiants : majuscule, espace, deux `:`, `/` dans le namespace, `..`, namespace omis, `:` initial, chemin vide (R20, R22).
- `pack.mcmeta` : entier contre `[majeure, mineure]`, `max_format` entier couvrant toutes les mineures, mineure supérieure à celle de la cible (`[121, 1]`), `min_format > max_format` (R5).
- Marqueur `.mcassetsroot` et dossier `data/minecraft/datapacks/` (R13, R14).
- JSON : profondeur 255 et 256 (R57), clé en double avec positions (R58), `-0`, entiers aux bornes 64 bits, décimaux aux bornes binary64 (R59), substitut isolé échappé contre substitut en octets (R60), un seul diagnostic par fichier invalide et sa précédence (R61).
- Combinateurs : `1` attendu décimal, `1.0` attendu entier, `null`, chaîne à la place d'un nombre (R62) ; champ inconnu lenient et strict (R63) ; `"a"`, `"#t"`, `["a"]`, `[]`, `["#t"]` comme ensembles d'éléments (R64).
- Flags : pack requérant un flag absent de `activeFlags` (R66) ; dossier de packs contenant `Z`, `a`, `p10`, `p2`, `é`, `.cache`, `x.ZIP` (R67).

## Vecteurs de test

### Inventaire (C)

Commande : `python -I data_inventory.py "$REF"` (texte et sortie complète dans `data-registries-inventory.md`). Lignes de contrôle attendues :

```text
TOTAL static_registries=95 static_entries=7053 datapack_registries=155 with_elements=60 vanilla_json_elements=7383 vanilla_tag_files=883 structures=1511 functions=0 builtin_pack_files=106 assets_files=3 unmatched=['.mcassetsroot'] all_files=9887
CHECK vanilla_missing_refs=0 vanilla_cycles=0 trade_rebalance_missing_refs=0 trade_rebalance_cycles=0 builtin_unmatched=0
```

Les comptes par registre, les vecteurs de tags (R39, R43) et la liste des tags vides sont dans les tableaux de l'inventaire.

### JSON (C)

Analyse stricte de tous les fichiers `.json` et `.mcmeta` sous `resources/` (bibliothèque standard Python, `object_pairs_hook` qui relève les clés en double, `parse_float` et `parse_int` qui relèvent les littéraux, `parse_constant` qui refuse `NaN` et `Infinity`) : 8374 fichiers, 0 échec, 0 BOM, 0 clé en double, 0 `null`, racines 8363 objets, 3 tableaux, 4 décimaux, 4 entiers (R29, R49, R50).

Vecteurs de lecture attendus de notre lecteur :

| Entrée | Résultat attendu | Preuve |
|---|---|---|
| `1.0` (fichier `context_float_provider/brewing/speed_default.json`) | nombre décimal, valeur 1.0 | C |
| `20` (fichier `context_int_provider/brewing/uses_default.json`) | nombre entier, valeur 20 | C |
| `1.7976931348623157E308` | décimal, plus grand double fini (`0x7fefffffffffffff`) | C |
| `2.9802322E-8` | décimal, double le plus proche de 2.9802322 x 10^-8 | C |
| `{"a":1,"a":2}` | objet `{"a":2}` et un avertissement (provisoire) | S |
| `{"a":1,}`, `// x` puis `{}`, `{'a':1}`, `{a:1}`, `NaN` | invalide (provisoire) | S |
| BOM + `{}`, fichier vide, `{} {}` | invalide (provisoire) | S |

### JSON : compléments (S, provisoires)

Les entrées sont des octets UTF-8 ; `x N` signifie N répétitions. Un résultat "valide" ne produit aucun diagnostic, sauf mention. Les bits sont ceux de binary64, en hexadécimal ; ils ont été calculés avec la bibliothèque standard Python (`struct.pack(">d", float(texte))`), hors du code du dépôt.

| Id | Entrée | Résultat attendu | Règle |
|---|---|---|---|
| VJ1 | `[` x 17, `]` x 17 | valide | R49, R57 |
| VJ2 | `[` x 255, `]` x 255 | valide | R57 |
| VJ3 | `[` x 256, `]` x 256 | invalide, `JSON_TOO_DEEP`, chemin `$` suivi de `[0]` x 255 | R57 |
| VJ4 | `{"a":` x 256, `0`, `}` x 256 | invalide, `JSON_TOO_DEEP`, chemin `$` suivi de `.a` x 255 | R57 |
| VJ5 | `[` x 100000, `]` x 100000 | invalide, `JSON_TOO_DEEP`, un seul diagnostic, pas d'erreur de pile | R57 |
| VJ6 | `{"a":1,"b":2,"a":3}` | membres dans l'ordre `a=3`, `b=2` ; un avertissement `JSON_DUPLICATE_KEY` à `$.a` | R58 |
| VJ7 | `{"a":1,"a":2,"a":3}` | `{a=3}` ; deux avertissements, tous deux à `$.a` | R58 |
| VJ8 | `{"a":{"x":1,"x":2},"a":{"y":1}}` | `{a={y=1}}` ; avertissements dans l'ordre : `$.a.x`, puis `$.a` | R58 |
| VJ9 | `{"a":1,"a":2}` | `{a=2}` ; un avertissement | R58 |
| VJ10 | `{"a":1,"A":2}` | deux membres `a=1`, `A=2` ; aucun avertissement | R58 |
| VJ11 | `[{"k":1,"k":2},{"k":3,"k":4}]` | `[{k=2},{k=4}]` ; avertissements `$[0].k` puis `$[1].k` | R58 |
| VJ12 | `{"a":1,"a":2,}` | invalide, `JSON_SYNTAX` seul (pas d'avertissement) | R58, R61 |
| VJ13 | `0` ; `-0` | deux nœuds entiers de valeur 0, égaux | R59 |
| VJ14 | `-0.0` ; `0.0` | décimaux de bits `8000000000000000` et `0000000000000000`, nœuds différents | R59 |
| VJ15 | `1e5` ; `1E+5` ; `1.0e0` | décimaux de bits `40f86a0000000000` (les deux premiers) et `3ff0000000000000` | R59 |
| VJ16 | `1` ; `1.0` | un entier, un décimal, nœuds différents ; dans `[1,1.0]`, forme conservée par élément | R59, R29 |
| VJ17 | `9223372036854775807` ; `-9223372036854775808` | entiers valides (bornes) | R59 |
| VJ18 | `9223372036854775808` ; `-9223372036854775809` ; `123456789012345678901234567890` | invalide, `JSON_NUMBER_RANGE` | R59 |
| VJ19 | `1e999` ; `-1e999` ; `1.7976931348623159E308` | invalide, `JSON_NUMBER_RANGE` | R59 |
| VJ20 | `1.7976931348623158E308` | décimal de bits `7fefffffffffffff` (plus grand fini) | R59 |
| VJ21 | `1e-999` ; `-1e-999` ; `2E-324` ; `4.9E-324` | décimaux valides de bits `0000000000000000`, `8000000000000000`, `0000000000000000`, `0000000000000001` | R59 |
| VJ22 | `+1` ; `.5` ; `1.` ; `01` ; `-01` ; `00` ; `-` ; `1e` ; `1e+` ; `0x10` ; `1_000` ; `Infinity` ; `-Infinity` ; `NaN` | invalide, `JSON_SYNTAX` | R52, R59 |
| VJ23 | `"\ud800"` | valide, chaîne d'une unité U+D800 | R60 |
| VJ24 | `"😀"` | valide, un point de code U+1F600 (deux unités) | R60 |
| VJ25 | `"\ude00\ud83d"` | valide, deux unités isolées dans cet ordre | R60 |
| VJ26 | `{"\ud800":1}` | valide, clé d'une unité U+D800 | R60 |
| VJ27 | `"\u0000"` ; `"\/"` | valides : une chaîne d'une unité U+0000 ; la chaîne `/` | R60 |
| VJ28 | octets `22 ED A0 80 22` | invalide, `JSON_UTF8` | R60, R53 |
| VJ29 | octets `22 C0 80 22` ; `22 F4 90 80 80 22` ; `22 80 22` | invalide, `JSON_UTF8` | R60, R53 |
| VJ30 | octets `22 F0 9F 98 80 22` ; `22 EF BF BE 22` | valides : U+1F600 ; U+FFFE | R60 |
| VJ31 | `"a` LF `b"` (saut de ligne brut) ; `"\x41"` ; `"\u12"` ; `"\u00zz"` | invalide, `JSON_SYNTAX` | R60 |
| VJ32 | octets `EF BB BF 7B 7D` ; `EF BB BF 80` ; `EF BB BF` | invalide, `JSON_BOM` (les trois) | R53, R61 |
| VJ33 | octets `80 7B` ; `7B 80` ; `7B 22 80 22 7D` | invalide, `JSON_UTF8` (les trois, précédence sur la syntaxe) | R61 |
| VJ34 | fichier vide ; `   ` LF | invalide, `JSON_EMPTY` | R53 |
| VJ35 | `{} {}` ; `{}x` ; `{}` LF (valide) | `JSON_TRAILING` pour les deux premiers ; le troisième est valide | R53 |
| VJ36 | octet `0C` (saut de page) puis `{}` ; octets `C2 A0 7B 7D` (espace insécable) | invalide, `JSON_SYNTAX` | R52, R61 |

### Combinateurs scalaires, champs inconnus, ensembles d'éléments (S, provisoires)

Codecs synthétiques de test. "Univers" : registre fictif de blocs `{minecraft:a, minecraft:b, minecraft:c}` et tag `#minecraft:t` = `[a, b]`.

| Id | Combinateur | Entrée | Résultat attendu | Règle |
|---|---|---|---|---|
| VC1 | décimal (double) | `1` ; `1.0` ; `1e2` | 1.0 ; 1.0 ; 100.0 | R62 |
| VC2 | décimal (double) | `9007199254740993` | `9007199254740992.0` | R62 |
| VC3 | décimal (double) | `"1"` ; `true` ; `null` ; `[1]` | `CODEC_TYPE_MISMATCH` (quatre diagnostics si quatre champs) | R62, R56 |
| VC4 | décimal (float 32 bits) | `0.1` ; `1` ; `1e40` | bits float `3dcccccd` ; 1.0f ; `CODEC_NUMBER_RANGE` | R62 |
| VC5 | entier 32 bits | `1` ; `-0` ; `-2147483648` ; `2147483647` | 1 ; 0 ; valeurs aux bornes | R62 |
| VC6 | entier 32 bits | `2147483648` ; `-2147483649` | `CODEC_NUMBER_RANGE` | R62 |
| VC7 | entier 32 bits ou 64 bits | `1.0` ; `1e0` ; `-0.0` ; `2.5` | `CODEC_TYPE_MISMATCH` | R62 |
| VC8 | entier 64 bits | `9223372036854775807` | valide | R62 |
| VC9 | booléen | `true` ; `false` | valides | R62 |
| VC10 | booléen | `0` ; `1` ; `"true"` | `CODEC_TYPE_MISMATCH` | R62 |
| VC11 | liste d'entiers | `[1,2]` ; `[]` | valides | R62 |
| VC12 | liste d'entiers | `1` ; `{"0":1}` | `CODEC_TYPE_MISMATCH` (pas de promotion) | R62 |
| VC13 | objet à champ requis entier `a`, mode par défaut | `{"a":1,"b":2}` | `a=1`, aucun diagnostic | R63 |
| VC14 | même objet, mode strict | `{"a":1,"b":2,"c":3}` | `a=1` décodé ; `CODEC_UNKNOWN_FIELD` à `$.b` puis `$.c` | R63 |
| VC15 | même objet | `{"b":2}` | `CODEC_MISSING_FIELD` ; en mode strict aussi `CODEC_UNKNOWN_FIELD` à `$.b` (deux diagnostics, ordre entre les deux non vérifié) | R56, R63 |
| VC16 | `pack.mcmeta` | `{"pack":{"description":"x","min_format":121,"max_format":121,"zzz":1},"zzz":{}}` | pack compatible, aucun diagnostic | R4, R63 |
| VC17 | fichier de tag | `{"values":[],"zzz":1}` ; `{"values":[{"id":"a","required":true,"zzz":1}]}` | tags valides, aucun diagnostic | R32, R63 |
| VC18 | ensemble d'éléments | `"minecraft:a"` ; `"a"` | ensemble `[a]` (forme en ligne à un élément) | R64 |
| VC19 | ensemble d'éléments | `"#minecraft:t"` ; `"#t"` | référence au tag `minecraft:t`, qui se résout en `[a, b]` | R64 |
| VC20 | ensemble d'éléments | `["a","b"]` ; `[]` ; `["a","a","b"]` | `[a, b]` ; ensemble vide ; `[a, b]` sans diagnostic | R64 |
| VC21 | ensemble d'éléments | `["a","#t"]` | `CODEC_INVALID_IDENTIFIER` à `$[1]` | R64 |
| VC22 | ensemble d'éléments | `["a","x"]` ; `"x"` ; `"#nope"` | `CODEC_UNRESOLVED_REFERENCE` à `$[1]` ; à `$` ; à `$` | R64 |
| VC23 | ensemble d'éléments | `{}` ; `1` ; `true` ; `null` | `CODEC_TYPE_MISMATCH` | R64 |
| VC24 | référence unique | `"a"` ; `"#t"` | élément `minecraft:a` ; `CODEC_INVALID_IDENTIFIER` | R64 |

### Flags et découverte de packs (S, provisoires)

| Id | Entrée | Résultat attendu | Règle |
|---|---|---|---|
| VF1 | sources : vanilla, `A` (`features.enabled` = `["minecraft:trade_rebalance"]`) ; `activeFlags` = `{minecraft:vanilla}` | `A` écartée, `PACK_FLAGS_INACTIVE` (flag manquant `minecraft:trade_rebalance`) ; vanilla chargé ; les données de `A` absentes de la sortie | R66 |
| VF2 | mêmes sources ; `activeFlags` = `{minecraft:vanilla, minecraft:trade_rebalance}` | `A` empilé, aucun diagnostic | R66 |
| VF3 | pack `B` avec `["x:y","minecraft:trade_rebalance"]` ; `activeFlags` = `{minecraft:vanilla, minecraft:trade_rebalance}` | `B` écarté, flag manquant `x:y` | R66 |
| VF4 | pack sans champ `features` ; `activeFlags` = `{minecraft:vanilla}` | chargé | R66 |
| VF5 | `activeFlags` donné dans l'ordre `[minecraft:vanilla, minecraft:trade_rebalance]` | sortie `[minecraft:trade_rebalance, minecraft:vanilla]` (tri par points de code) | R66 |
| VD1 | dossier avec : sous-dossiers valides `p2`, `p10`, `Z`, `a`, `é` ; archive valide `x.zip` ; archive `y.ZIP` ; dossier `.hidden` ; dossier `notapack` sans `pack.mcmeta` ; fichier `readme.txt` | candidats dans l'ordre `file/Z`, `file/a`, `file/p10`, `file/p2`, `file/x.zip`, `file/é` ; `notapack` : `PACK_INVALID` ; `y.ZIP`, `.hidden`, `readme.txt` ignorés | R67, R7 |

### Identifiants (S, provisoires)

| Texte | Contexte | Résultat |
|---|---|---|
| `minecraft:stone` | valeur | `minecraft:stone` |
| `stone` | valeur | `minecraft:stone` |
| `:stone` | valeur | `minecraft:stone` |
| `minecraft/villager` | valeur | `minecraft:minecraft/villager` |
| `foo:bar/baz.qux-1_2` | valeur | `foo:bar/baz.qux-1_2` |
| `Foo:bar`, `foo:Bar`, `foo:bar baz`, `foo/bar:baz`, `a:b:c`, `..:x` | valeur | invalide |
| `#logs` | entrée de tag | référence au tag `minecraft:logs` |
| `#minecraft:logs` | identifiant d'élément attendu | invalide |
| `data/sonde/loot_table/a/b.json` | chemin | élément `sonde:a/b` du registre `minecraft:loot_table` |
| `data/sonde/tags/worldgen/biome/x.json` | chemin | tag `#sonde:x` du registre `minecraft:worldgen/biome` |
| `data/sonde/loot_table/Maj.json` | chemin | ignoré, diagnostic |

### pack.mcmeta (S, provisoires ; cible data 121.0)

| `min_format` | `max_format` | Résultat |
|---|---|---|
| `121` | `121` | compatible |
| `[121, 0]` | `[121, 0]` | compatible |
| `120` | `121` | compatible |
| `[121, 1]` | `121` | incompatible, plus récent |
| `100` | `120` | incompatible, plus ancien |
| `[120, 5]` | `[121, 0]` | compatible |
| `122` | `121` | cassé (`min > max`) |
| absent (`pack_format: 121` seul) | absent | cassé |

### Tags (S, provisoires)

Avec un registre fictif d'univers `{a, b, c, d}` :

| Fichiers (priorité croissante) | Tag `t` résolu |
|---|---|
| P1 `t = [a, #u]`, `u = [c, a]` | `[a, c]` |
| P1 `t = [b]` ; P2 `t = [a]` | `[b, a]` |
| P1 `t = [b]` ; P2 `t = {replace: true, values: [a]}` | `[a]` |
| P1 `t = [a, x]` | `t` en échec (x manquant) |
| P1 `t = [a, {id: x, required: false}]` | `[a]` |
| P1 `t = [#v]`, `v = [x]` | `v` et `t` en échec |
| P1 `t = [#u]`, `u = [#t, a]` | `t` et `u` en échec (cycle) |
| P1 `t = []` | `[]` (existe) |

## Sondes de l'oracle

Cadre commun : serveur 26.3, monde neuf `level-name=sonde`, type de monde plat pour la vitesse ; pack de sonde dans `sonde/datapacks/<pack>/` avec `pack.mcmeta` `{"pack": {"description": "sonde", "min_format": 121, "max_format": 121}}` sauf mention ; commandes envoyées par RCON après démarrage (ou après `/reload` quand la sonde ne vise pas le chargement du monde) ; sortie relevée = réponse RCON et lignes de `logs/latest.log` du démarrage. Sans joueur connecté, le chunk (0, 0) n'est vraisemblablement pas chargé (indices D : le registre `ticket_type` n'a ni ticket de spawn permanent ni `start`, aucune règle de jeu de rayon de spawn n'existe, et le message `argument.pos.unloaded` "That position is not loaded" existe). Toute sonde qui agit en 0 100 0 (P4, P5, P7 à P17 ; P9 aussi, par son `loot spawn`) commence donc, après chaque démarrage ou création de monde, par `forceload add 0 0` (réponse attendue "Marked chunk [0, 0] in minecraft:overworld to be force loaded" ou équivalent), puis attend que `forceload query 0 0` réponde "Chunk at [0, 0] in minecraft:overworld is marked for force loading" et que `execute if loaded 0 100 0` réponde "Test passed" (répéter toutes les 20 ticks, abandon après 200 ticks) avant tout `setblock`, `execute if block` ou `loot spawn`. Les sorties attendues sont celles des règles provisoires. Aucune sonde ne demande de joueur connecté. Les textes cités viennent de `en_us.json` (D) ; "Dropped 1 minecraft:emerald" abrège le message `commands.drop.success.single` ("Dropped %s %s") dont le rendu exact du nom d'objet n'est pas spécifié ici : seul l'objet compte.

| Sonde | Mise en place et commandes | Sortie attendue (provisoire) | Règles levées |
|---|---|---|---|
| P1 | Packs `f_ok`, `f_minor_new` (`min [121,1]`), `f_old` (`max 120`), `f_new` (`min 122, max 122`), `f_broken` (`min 122, max 121`), `f_legacy` (`pack_format 121` seul), `f_range` (`min 100, max [121,0]`). `datapack list available`, `datapack list enabled`, puis `datapack enable "file/f_old"`. | `f_ok`, `f_range` activés au premier démarrage ; les autres listés disponibles ; message d'activation de `f_old` et avertissement au journal | R4, R5, R6 |
| P2 | `datapack create sonde_c "Sonde"`, puis lire `sonde/datapacks/sonde_c/pack.mcmeta`. | "Created new empty pack with name 'sonde_c'" ; fichier avec `min_format` et `max_format` 121 ou `[121, 0]` (forme canonique écrite par l'oracle) | R2, R4, R5 |
| P3 | Monde neuf sans pack ; `datapack list available` et `datapack list enabled`. Puis monde neuf avec `initial-enabled-packs=vanilla,trade_rebalance` dans `server.properties`, mêmes commandes, puis `datapack disable trade_rebalance` et, sur le premier monde, `datapack enable trade_rebalance`. | Identifiants `vanilla`, `minecart_improvements`, `redstone_experiments`, `trade_rebalance` ; `trade_rebalance` activé seulement dans le second monde ; désactivation refusée ("part of an enabled flag") ; activation refusée ("required flags are not enabled") | R3, R40, R45 |
| P4 | Packs `A` et `B` fournissant `data/minecraft/loot_table/blocks/dirt.json` (A : 1 diamant ; B : 1 émeraude), `B` activé après `A`. `loot spawn 0 100 0 loot minecraft:blocks/dirt` ; `datapack enable "file/A" last` (après `datapack disable "file/A"`) ; même commande. | "Dropped 1 minecraft:emerald" puis "Dropped 1 minecraft:diamond" (pas de mélange des deux contenus) | R40, R41 |
| P5 | Pack `T` avec `data/minecraft/tags/block/logs.json` = `{"values": ["minecraft:stone"]}`. `setblock 0 100 0 minecraft:stone` puis `execute if block 0 100 0 #minecraft:logs` ; idem avec `minecraft:oak_log`. Puis même fichier avec `"replace": true`. | Fusion : "Test passed" deux fois ; `replace` : "Test passed" pour la pierre, "Test failed" pour le chêne | R34, R9 |
| P6 | Fonctions `sonde:f1`..`sonde:f3` (`say 1`..`say 3`) ; `#sonde:t` = `["sonde:f2", "sonde:f1", "#sonde:u", "sonde:f2"]`, `#sonde:u` = `["sonde:f3", "sonde:f1"]` ; un second pack ajoute `["sonde:f1", "sonde:f3"]` à `#sonde:t`. `function #sonde:t`. | Messages dans l'ordre 2, 1, 3, chacun une fois ; "Executed 3 command(s) from 3 functions" | R34, R35 |
| P7 | `#sonde:manque` = `["minecraft:stone", "minecraft:pas_un_bloc"]` ; `#sonde:opt` = `["minecraft:stone", {"id": "minecraft:pas_un_bloc", "required": false}]` ; `#sonde:dep` = `["#sonde:manque", "minecraft:dirt"]` ; `#sonde:tagmanque` = `["#sonde:inexistant"]`. Pierre en 0 100 0 ; `execute if block 0 100 0 #sonde:<x>` pour chaque tag. | `manque`, `dep`, `tagmanque` : "Unknown block tag '...'" ; `opt` : "Test passed" ; journal listant les références manquantes ; le monde démarre ou non (à relever pour R55) | R32, R36, R38, R55 |
| P8 | `#sonde:a` = `["#sonde:b"]`, `#sonde:b` = `["#sonde:a", "minecraft:stone"]`, plus une table de butin `sonde:valide` dans le même pack. `execute if block 0 100 0 #sonde:a`, `loot spawn 0 100 0 loot sonde:valide`. | Tags inconnus, cycle au journal ; comportement du démarrage à relever (R55) ; table de butin chargée si le monde démarre | R37, R55 |
| P9 | Fichiers `data/sonde/loot_table/Maj.json`, `data/sonde/loot_table/es pace.json`, `data/Sonde/loot_table/x.json`, `data/sonde/loot_table/valide.json`. `loot spawn 0 100 0 loot sonde:valide`, puis le journal. | `sonde:valide` chargé ; trois diagnostics au journal | R20, R24 |
| P10 | Tag de blocs `#sonde:ns` = `["stone", ":dirt", "minecraft:oak_log"]`. `execute if block` sur pierre, terre, chêne. Variante `["minecraft:Stone"]`. | Trois "Test passed" ; la variante est refusée (tag inconnu ou pack en erreur) | R22, R20 |
| P11 | Table `sonde:double` = `{"type": "minecraft:block", "pools": [<1 diamant>], "pools": [<1 émeraude>]}`. `loot spawn 0 100 0 loot sonde:double`. | "Dropped 1 minecraft:emerald" | R51 |
| P12 | Une table par variante, toutes équivalentes à "1 diamant" : commentaire `//`, commentaire `/* */`, virgule finale, guillemets simples, clé sans guillemets, BOM, fichier vide, `{}` suivi d'un second `{}`. `loot spawn` pour chacune. | Aucune chargée (élément introuvable) ; un diagnostic par fichier ; si certaines se chargent, R52 et R53 s'alignent sur la liste observée | R52, R53, R54 |
| P13 | Monde neuf avec un pack contenant `data/minecraft/worldgen/noise/sonde.json` = `{` (JSON invalide). Puis autre monde neuf avec `data/sonde/loot_table/casse.json` = `{` et `data/sonde/loot_table/valide.json`. Puis, sur ce second monde, corriger le premier fichier en `[]` (schéma invalide) et lancer `reload`. | Premier monde : le serveur refuse de charger le monde (journal) ; second monde : démarre, `sonde:valide` chargé, `sonde:casse` introuvable ; `reload` : "Reloading!" ou "Reload failed; keeping old data", à relever | R54, R55 |
| P14 | Fichiers `data/sonde/block/x.json`, `data/sonde/inconnu/x.json`, `data/sonde/loot_table/x.txt`, `data/sonde/tags/structure/x.json`, plus `data/sonde/loot_table/valide.json`. Journal, puis `loot spawn` de `sonde:valide` et de `sonde:x`. | Démarrage sans erreur, `sonde:valide` chargé, `sonde:x` introuvable | R12, R15, R16 |
| P15 | Pack `F` de priorité plus haute que `A` (P4) avec `"filter": {"block": [{"namespace": "minecraft", "path": "<p>"}]}`, une variante par `<p>` : `loot_table/blocks/dirt.json`, `loot_table/blocks/dirt`, `blocks/dirt`, `loot_table/.*`. `loot spawn 0 100 0 loot minecraft:blocks/dirt`. | La table de `A` et celle du vanilla sont masquées pour les variantes qui correspondent : on relève lesquelles (la table devient introuvable) | R47 |
| P16 | Monde avec `initial-enabled-packs=vanilla,trade_rebalance` ; 500 fois `loot spawn 0 100 0 loot minecraft:chests/desert_pyramid`, relevé des objets ; même relevé dans un monde sans le pack. | Avec le pack : `minecraft:saddle` peut apparaître, jamais `minecraft:leather` ; sans : l'inverse (C : les ensembles d'objets possibles des deux versions du fichier ne diffèrent que par ces deux objets ; un résultat sur 500 tirages reste un indice statistique) | R41, R42, R43 |
| P17 | Pack avec base `data/sonde/loot_table/o.json` (1 diamant), overlays `ov1` (`min 121, max 121`, 1 émeraude), `ov2` (`min 122, max 122`, 1 lapis), puis variante avec `ov3` après `ov1` (`min 121, max 121`, 1 charbon). `loot spawn 0 100 0 loot sonde:o`. | "Dropped 1 minecraft:emerald", puis "Dropped 1 minecraft:coal" | R48 |
| P18 | Profondeur. Précondition : `pack.mcmeta` valide avec une clé inconnue `"zz": []` accepté (sinon utiliser seulement le second porteur). Packs `d_N` pour N dans {17, 64, 255, 256, 257, 1000, 100000} : `pack.mcmeta` valide dont la clé inconnue `"zz"` vaut N tableaux imbriqués (`[` x N, `]` x N). Second porteur, après P22 : `data/sonde/loot_table/d_N.json`, table valide d'un diamant plus la même clé `"zz"`. `datapack list available` puis `loot spawn 0 100 0 loot sonde:d_N`, journal. | Seuil N* le plus grand accepté, et nature du refus (erreur d'analyse JSON, ou autre) ; attendu provisoire : N* = 255, refus dès 256, aucune erreur de pile | R57 |
| P19 | Nombres hors bornes. Table valide d'un diamant avec une condition `random_chance` dont la clé de probabilité prend successivement `100000000000000000000`, `9223372036854775808`, `1e999`, `-1e999`, `1.7976931348623159E308`, `-0`, `-0.0`, `1e-999`, `4.9E-324` (forme de la condition copiée d'un fichier vanilla de `loot_table` qui en utilise une). 50 `loot spawn 0 100 0 loot sonde:n_<k>` par variante. | Pour chaque variante : table chargée ou introuvable, et nombre de butins. Attendu provisoire : les trois premières et `1e999`, `-1e999` introuvables (`JSON_NUMBER_RANGE`) ; `-0`, `-0.0`, `1e-999`, `4.9E-324` chargées, jamais de butin. Si le jeu charge une variante que R59 refuse, R59 s'aligne (repli décrit dans le journal des décisions) | R59, R50 |
| P20 | Substituts isolés. `pack.mcmeta` valide dont `description` vaut `"a\ud800b"`, puis `"a\ude00\ud83db"`, puis `"😀"`, puis les octets bruts `ED A0 80` dans la chaîne (variante invalide en UTF-8) ; même clé `"zz"` des tables (P22) en second porteur. `datapack list available`, `loot spawn`. | Les trois premiers acceptés, la variante en octets refusée (provisoire) | R60 |
| P21 | Types des combinateurs. Table d'un diamant dont l'entrée a `weight` valant `1`, `1.0`, `1e0`, `1.5`, `3000000000`, `-0`, `true`, `"1"` (une table par valeur) ; condition `random_chance` dont la probabilité vaut `1`, `1.0`, `"1"`, `true`, `[1]` ; un champ booléen d'entrée de tag de butin (copié d'un fichier vanilla) valant `true`, `1`, `"true"` ; `pools` écrit comme un objet au lieu d'un tableau. Les noms de champs sont vérifiés dans les fichiers vanilla extraits avant l'exécution. `loot spawn` de chaque table. | Table chargée ou introuvable pour chaque valeur. Attendu provisoire : `weight` : `1` et `-0` chargées, `1.0`, `1e0`, `1.5`, `3000000000`, `true`, `"1"` introuvables ; probabilité : `1` et `1.0` chargées, les trois autres introuvables ; booléen : seul `true` ; `pools` objet introuvable | R62 |
| P22 | Champs inconnus. (a) `pack.mcmeta` avec `"zzz": 1` à la racine, dans `pack`, dans `features` ; (b) tag de blocs `{"values": ["minecraft:stone"], "zzz": 1}` et `{"values": [{"id": "minecraft:stone", "zzz": 1}]}` testés par `setblock` puis `execute if block 0 100 0 #sonde:<x>` ; (c) table valide avec `"zzz": 1` à la racine, dans un pool, dans une entrée, dans une condition, dans le prédicat d'une condition `entity_properties` (cas cité pour 26.2). `loot spawn`, journal. | Chargé ou refusé pour chaque porteur. Attendu provisoire : tout chargé sans diagnostic ; si un porteur refuse, le noter et passer ce porteur en mode strict dans la spec du propriétaire (pour un tag, R32 et R63(c) changent) | R4, R32, R63 |
| P23 | Ensembles d'éléments. Pour chaque valeur de `supported_items`, enchantement `sonde:e<k>` copié du fichier vanilla de `sharpness` dont seule cette clé change : `"minecraft:diamond_sword"`, `"diamond_sword"`, `"#minecraft:swords"`, `["minecraft:diamond_sword","minecraft:iron_sword"]`, `[]`, `["minecraft:diamond_sword","minecraft:diamond_sword"]`, `["#minecraft:swords"]`, `["minecraft:pas_un_objet"]`, `"minecraft:pas_un_objet"`, `"#sonde:inexistant"`, `{}`, `1`, `null` (l'existence du tag `swords` et des objets cités est vérifiée dans l'extraction avant l'exécution). Après le démarrage : `enchant @e[limit=1] sonde:e<k> 1`, journal. | Réponse "Can't find element 'sonde:e<k>' of type 'minecraft:enchantment'" (texte `argument.resource.not_found`) = élément absent ; toute autre réponse (par exemple "No entity was found") = élément chargé. Attendu provisoire : chargés = les quatre premiers, `[]` et le tableau à doublon ; absents = tous les autres ; relever aussi si le monde démarre malgré l'erreur (R55) | R64 |
| P24 | Ordre de découverte. Dans `sonde/datapacks/`, packs valides (`min_format` et `max_format` 121) `é`, `p2`, `a`, `Z`, `p10`, créés dans cet ordre (sur un second monde, dans l'ordre inverse), plus une archive `x.zip`, un dossier `.hidden`, un fichier `y.ZIP` et un dossier sans `pack.mcmeta`. Premier démarrage d'un monde neuf, puis `datapack list enabled` et `datapack list available`. | Ordre des packs activés (priorité croissante), liste des packs ignorés. Attendu provisoire : `Z`, `a`, `p10`, `p2`, `x.zip`, `é` à la suite du pack de base, identique dans les deux mondes ; `.hidden`, `y.ZIP`, le dossier invalide absents | R67, R6, R7 |

## Questions ouvertes

- Toutes les règles S ci-dessus restent provisoires jusqu'à leurs sondes (tableau précédent). Sans oracle exécutable, aucune n'est levée ; R30 (ordre des éléments d'un registre à fichiers) n'a pas de sonde serveur sans joueur et sera levée au jalon M8 par le paquet de données de registres envoyé à un client vanilla.
- Forme d'un pack d'archive (R19) : à relever avec P1 et P24. L'ordre de découverte des packs du dossier `datapacks/` est provisoirement fixé par R67 ; P24 le confirme ou le remplace.
- Décisions d'architecture non observables sur l'oracle, donc sans sonde : place d'un membre à clé en double (R58 b), `-0` entier (R59 c), garde de flags (R66), encodage (R65). La règle de `docs/specs/README.md` ("une règle S bloque `figé` tant qu'une sonde ne l'a pas levée") ne peut pas s'appliquer à elles ; leur exemption est à confirmer par le propriétaire du dépôt. Elles sont consignées dans `data-decisions-log.md` avec la condition qui les ferait rouvrir (R65 : écart constaté avec un client réel en M8).
- Le résultat d'un `/reload` ou d'un démarrage avec un fichier en erreur (R55) reste à relever avec P7, P8, P13, P22 et P23 ; la répartition entre erreur bloquante et élément écarté dépend de ces sondes.
- Non tranché ici, faute d'ambiguïté signalée : la validation du contenu du composant de texte `pack.description` (forme du composant, et effet de l'absence de `description` sur la validité du pack : non décidés) et les champs de packs de ressources (`language`, R4).
- Rechargement : quels registres `/reload` recharge (la documentation cite tags, butins, recettes, avancements, modificateurs, prédicats, fonctions, structures ; `context_float_provider` et `context_int_provider`, `stable: true`, ne sont pas cités). Hors M2 ; sonde à écrire avec `engine/game`.
- Contradictions relevées entre sources, tranchées par les données :
  - le rapport Overworld parle de "103 fichiers d'overlay expérimental" : ce sont les fichiers du pack intégré `trade_rebalance`, pas des overlays de `pack.mcmeta` (R42) ; le compte 103 est confirmé ;
  - la page "Data pack" de la documentation liste encore `worldgen/configured_carver` et `worldgen/configured_feature`, alors que les données 26.3 utilisent `worldgen/carver` et `worldgen/feature` (la page "Tag" note ce renommage en 26.3) ; la même page omet de sa liste des dossiers expérimentaux `cat_sound_variant`, `chicken_sound_variant`, `cow_sound_variant`, `pig_sound_variant` et les sous-dossiers récents de `worldgen`, tous `stable: false` dans `datapack.json` ;
  - la page "Pack.mcmeta" se contredit sur les majuscules dans les noms de dossiers d'overlay (R48) ;
  - le fichier de langue contient encore des noms de packs d'expériences passées (`bundle`, `update_1_20`, `update_1_21`, `winter_drop`, `locator_bar`) absents des packs intégrés extraits : seuls les trois packs de R42 existent dans la cible.
- `version.json` des données extraites donne en D les valeurs que `reference-binaries.md` garde en S (ses règles R2 à R4 : data version 5023, protocole 777, formats 121.0 et 97.1) ; la mise à jour de cette spec est à faire par son propriétaire.
- La spec passe à `relu` après relecture, et à `figé` quand les sondes P1 à P24 ont été exécutées et R30 levée au jalon M8.

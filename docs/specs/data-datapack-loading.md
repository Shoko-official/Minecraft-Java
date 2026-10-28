# Chargement des datapacks, registres et tags

Statut : brouillon
Jalon : M2
Risque : B

## Périmètre

Couvre, pour `engine/data` :

- la structure d'un datapack et d'un pack de ressources : `pack.mcmeta`, versions de format de la cible (data 121.0, ressources 97.1), arborescence `data/<namespace>/<dossier de registre>/**`, extensions ;
- la syntaxe des identifiants (`namespace:path`) et leur dérivation depuis les chemins de fichiers ;
- l'inventaire des registres statiques et des registres chargés par datapack, avec le nombre de fichiers par registre (détail dans `data-registries-inventory.md`) ;
- les tags : format, dossiers, résolution, fusion entre packs, références manquantes, cycles ;
- l'empilement des packs, la priorité, le remplacement de fichiers, les packs intégrés et les feature flags, les overlays et les filtres de `pack.mcmeta` ;
- l'analyse JSON attendue et le modèle d'erreurs ;
- le contrat de l'infrastructure (ADR 0005) : ce que le chargeur M2 valide seul et ce qu'il laisse aux modules propriétaires.

Ne couvre pas :

- le décodage typé du contenu des éléments (biomes, butins, recettes, enchantements...) : spec de chaque module propriétaire ;
- le format NBT des structures (`core-nbt.md`) et la syntaxe des fonctions `.mcfunction` (spec des commandes, `engine/game`) ;
- le rechargement à chaud (`/reload`) au-delà des points notés en questions ouvertes ;
- la synchronisation des registres avec le client (protocole, jalon M8) et le contenu des packs de ressources au-delà de leur structure (client, jalon M9).

## Sources

- **Données extraites** (`.cache/reference/26.3/`, preuve D) :
  - `version.json` : `pack_version` (`data_major` 121, `data_minor` 0, `resource_major` 97, `resource_minor` 1), `world_version` 5023, `protocol_version` 777 ;
  - `generated/reports/registries.json` (95 registres statiques) et `generated/reports/datapack.json` (155 registres chargeables, section `others`) ;
  - `generated/reports/commands.json` : arbre des commandes `datapack`, `reload`, `loot`, `execute`, `function` utilisées par les sondes ;
  - `resources/data/minecraft/**` : pack vanilla et trois packs intégrés sous `resources/data/minecraft/datapacks/` ;
  - `resources/assets/minecraft/lang/en_us.json` : textes des messages observables (clés `commands.datapack.*`, `pack.*`, `dataPack.*`, `datapackFailure.*`, `arguments.*.tag.unknown`, `argument.resource*`) ;
  - `state.json` : `resourceFiles` = 9887.
- **Calculs** (preuve C) : script `data_inventory.py` (texte intégral et sortie dans `data-registries-inventory.md`) ; contrôles ponctuels décrits dans les vecteurs ci-dessous (analyse JSON stricte de tous les fichiers, relevé des nombres).
- **Documentation publique**, consultée le 2026-10-28 (preuve S tant qu'elle n'est pas recoupée par les données ou l'oracle) : Minecraft Wiki, pages "Pack format" (https://minecraft.wiki/w/Pack_format), "Data pack" (https://minecraft.wiki/w/Data_pack), "Pack.mcmeta" (https://minecraft.wiki/w/Pack.mcmeta), "Identifier" (https://minecraft.wiki/w/Identifier), "Tag (Java Edition)" (https://minecraft.wiki/w/Tag_(Java_Edition)).
- **Rapports locaux** (indices, ADR 0002 point 2b) : `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, section sur l'expérience intégrée `trade_rebalance` (103 fichiers JSON exclus des comptes vanilla) et relevé `integrated_experiments` (`minecart_improvements` 0, `redstone_experiments` 0, `trade_rebalance` 103). Recherche des mots-clés pack.mcmeta, datapack, tag, replace, required, overlay, namespace, feature flag dans les cinq rapports : aucun autre passage sur le chargement. Seuls les comptes de ce rapport sont repris, et ils sont recalculés (R42).
- **Sondes de l'oracle** : aucune exécutée (serveur non exécutable, EULA non acceptée). Elles sont décrites en fin de document (P1 à P17).

Convention de preuve : **D** = valeur lue telle quelle dans un fichier extrait ; **C** = propriété établie par un calcul reproductible sur ces fichiers ; **M** = non utilisé ; **S** = documentation publique seule, rapport local ou hypothèse. Une règle S porte un comportement **provisoire** à implémenter tant que la sonde citée n'a pas tranché : une règle S énonce elle-même le comportement provisoire. **C sur S** = règle énoncée par la documentation publique (S) et seulement recoupée par un calcul sur les données (C) ; elle reste à confirmer par une sonde, mais aucun fichier extrait ne la contredit.

## Règles

### Versions et pack.mcmeta

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Version de format de la cible : datapack 121.0 (majeure 121, mineure 0), pack de ressources 97.1. La documentation publique donne les mêmes valeurs pour 26.3. | D |
| R2 | Les trois `pack.mcmeta` des packs intégrés ont la forme `{"features": {"enabled": [<identifiant>]}, "pack": {"description": {"translate": "dataPack.<nom>.description"}, "max_format": 121, "min_format": 121}}`. Ni `pack_format`, ni `supported_formats`, ni `filter`, ni `overlays`. `description` est un composant de texte (ici un objet `translate`). | D |
| R3 | Le pack vanilla n'a pas de `pack.mcmeta` dans les données extraites : c'est un pack implicite, de description `dataPack.vanilla.description` ("The default data for Minecraft"). Provisoire : notre chargeur le construit sans fichier de métadonnées. | D (absence) ; S (traitement) |
| R4 | Champs reconnus de `pack.mcmeta` : `pack.description` (composant de texte), `pack.min_format` et `pack.max_format` (exigés en 121), `pack.pack_format` (entier, facultatif), `pack.supported_formats` (ancien), `features.enabled` (liste d'identifiants de flags), `filter.block` (liste de `{namespace?, path?}`, expressions régulières), `overlays.entries` (liste de `{directory, min_format, max_format, formats?}`), `language` (packs de ressources seulement). Provisoire : un champ inconnu est ignoré. | S |
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
| R32 | Format complet : `replace` booléen facultatif (défaut `false`) ; `values` liste d'entrées ; une entrée est une chaîne (identifiant ou `#tag`) ou un objet `{"id": <chaîne>, "required": <booléen>}`, `required` valant `true` par défaut. Provisoire : `values` absent, entrée d'un autre type ou clé inconnue rendent le fichier invalide (diagnostic, le fichier compte comme absent). | S |
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
| R40 | Ordre des packs : le pack vanilla en premier (priorité la plus basse), puis les packs activés dans l'ordre enregistré dans le monde ; le dernier a la priorité la plus haute. `/datapack enable <nom>` admet `first`, `last`, `before <existant>`, `after <existant>`. Provisoire : identifiants de packs `vanilla`, `minecart_improvements`, `redstone_experiments`, `trade_rebalance`, et `file/<nom du dossier ou de l'archive>` pour les packs du monde ; sources affichées `built-in`, `feature`, `world`. | D (syntaxe de commande, textes `pack.source.*`) ; S (ordre, identifiants) |
| R41 | Remplacement : pour un même registre et un même identifiant, seul le fichier du pack de plus haute priorité est retenu, en entier ; il n'y a pas de fusion de contenu JSON entre packs. Vaut aussi pour `function` et `structure`. | S |
| R42 | Packs intégrés de la cible : `minecart_improvements` et `redstone_experiments` (0 fichier de données, seulement un `pack.mcmeta` qui active leur flag), `trade_rebalance` (86 éléments, 17 tags). Les "103 fichiers d'overlay expérimental" du rapport Overworld sont ces 86 + 17 fichiers de `trade_rebalance` : c'est un pack intégré ordinaire, pas un overlay au sens de `pack.mcmeta` (aucun pack de la cible n'utilise `overlays`). | C |
| R43 | Effet de `trade_rebalance` empilé au-dessus du pack vanilla : il remplace 5 tables de butin (`chests/abandoned_mineshaft`, `chests/ancient_city`, `chests/desert_pyramid`, `chests/jungle_temple`, `chests/pillager_outpost`) et 1 échange (`librarian/2/book_emerald`), ajoute 80 échanges (`villager_trade` passe de 391 à 471), remplace par `replace: true` 10 tags d'échanges (`armorer/level_1..5`, `librarian/level_1..5`) et ajoute 7 tags d'enchantements (`trades/<biome>_common`, `enchantment` passe de 22 à 29 tags). Vecteurs : `#minecraft:armorer/level_1` passe de 5 à 2 entrées, `#minecraft:librarian/level_1` de 3 à 9 ; 0 référence manquante, 0 cycle. | C (sous R34, R41) |
| R44 | Feature flags connus des données : `minecraft:minecart_improvements`, `minecraft:redstone_experiments`, `minecraft:trade_rebalance` (via `features.enabled` des packs intégrés). Aucun fichier `.json` de la cible ne porte de condition de flag. Méthode : recherche des clés `required_features`, `requires_features`, `feature_flags` (0 fichier), et relevé de toutes les clés `features` sous `data/` : 3 objets avec `enabled` (les 3 `pack.mcmeta`), et dans les `.json` seulement des listes (123), des booléens (13) ou un objet de feature worldgen (1, `worldgen/feature/nylium_bonemeal`), tous dans `worldgen/biome`, `worldgen/feature`, `worldgen/flat_level_generator_preset` et `worldgen/world_preset`. Commandes, depuis `$REF/resources` : `grep -rlE --include='*.json' --include='*.mcmeta' '"(required_features|requires_features|feature_flags)"' . \| wc -l` (attendu `0`) et `grep -rl --include='*.json' '"features"' data \| sed 's#/[^/]*$##' \| sort \| uniq -c` (attendu : 67 `worldgen/biome`, 41 `worldgen/feature`, 6 `worldgen/feature/coral`, 9 `worldgen/flat_level_generator_preset`, 2 `worldgen/world_preset`). | D (flags des `pack.mcmeta`) ; C (absence) |
| R45 | Ensemble des flags d'un monde : provisoire, `minecraft:vanilla` plus l'union des `features.enabled` des packs activés à la création du monde. Les packs à flags sont désactivés par défaut. Un pack dont les flags ne sont pas tous actifs ne peut pas être activé ensuite ("Pack '%s' cannot be enabled, since required flags are not enabled in this world: %s!") ; un pack qui fait partie d'un flag actif ne peut pas être désactivé ("cannot be disabled, since it is part of an enabled flag!") ; les expériences ne se désactivent pas après la création. | D (textes) ; S (règle) |
| R46 | Le filtrage d'entrées de registres statiques par flag (blocs ou objets réservés à une expérience) n'est pas visible dans `registries.json`, qui liste toutes les entrées. Il relève des modules propriétaires et de `game`, pas de M2. | D (contenu de `registries.json`) ; S (portée) |
| R47 | `filter.block` : un fichier d'un pack de priorité plus basse dont le namespace et le chemin correspondent aux deux expressions (une expression absente correspond à tout) est traité comme absent ; le filtre n'agit pas sur le pack qui le porte. Provisoire : correspondance complète de l'expression ; le chemin testé est le chemin sous `data/<namespace>/`, extension comprise (`loot_table/blocks/dirt.json`). | S |
| R48 | Overlays : chaque entrée de `overlays.entries` dont l'intervalle `[min_format, max_format]` contient `(121, 0)` est active ; son dossier, à la racine du pack, est une arborescence de même forme que la racine (`<dossier>/data/...`) dont les fichiers remplacent ceux du pack de même chemin ; les entrées s'appliquent dans l'ordre de la liste, une entrée plus loin l'emportant. `pack.mcmeta` et `pack.png` d'un dossier d'overlay sont ignorés. Nom de dossier : `a-z 0-9 _ - .` (la documentation se contredit sur `A-Z`). Provisoire : un tag d'overlay remplace le tag du pack de même chemin (pas de fusion interne au pack). | S |

### Analyse JSON

| Réf | Règle | Preuve |
|---|---|---|
| R49 | Les 8374 fichiers `.json` et `.mcmeta` extraits sont du JSON strict (RFC 8259), en UTF-8 sans BOM, fins de ligne LF, sans clé en double, sans `null`, et sans aucune chaîne vide hors `en_us.json` (2 valeurs vides : clés `potion.potency.0` et `selectWorld.gameMode.spectator.line2`). Un seul fichier contient des caractères non ASCII (`assets/minecraft/lang/en_us.json`). Profondeur maximale : 17 objets ou tableaux imbriqués, racine comprise ; plus gros fichier de données 60159 octets (`block_transformer/axe.json`). | C |
| R50 | Nombres, sur les mêmes 8374 fichiers : 15391 littéraux entiers, dont 15385 dans les `.json` et 6 dans les `.mcmeta` (`min_format` et `max_format` des 3 packs intégrés) ; valeur absolue maximale 165745296 ; 6456 décimaux, 53 en notation exponentielle (5 formes distinctes : `1.0E-4`, `1.7976931348623157E308`, `2.9802322E-8`, `5.9604645E-8`, `9.999999747378752E-6`). 45 décimaux distincts ont 16 ou 17 chiffres significatifs (25 et 20) (exemple `-0.10000000149011612`, valeur binary32 écrite en binary64). Aucun `-0`. Le lecteur conserve, pour chaque nombre, s'il est écrit en entier ou en décimal (R29 : `1` et `1.0` sont des fichiers de registres différents) et sa valeur exacte : conversion binary64 correctement arrondie pour les décimaux (`1.7976931348623157E308` donne le plus grand double fini), entier exact sur 64 bits signés. | C |
| R51 | Clé en double dans un objet. Provisoire : la dernière valeur l'emporte, avec un diagnostic d'avertissement. | S |
| R52 | Syntaxe hors RFC 8259 (commentaires `//` ou `/* */`, virgule finale, guillemets simples, clé ou chaîne sans guillemets, `NaN`, `Infinity`, nombres `+1`, `.5`, `01`). Provisoire : refusée, le fichier est invalide. Si la sonde montre que l'oracle en accepte, le lecteur doit accepter exactement les mêmes formes. | S |
| R53 | BOM UTF-8 en tête, fichier vide ou fait seulement de blancs, contenu non blanc après la valeur racine, octets UTF-8 invalides. Provisoire : fichier invalide. | S |
| R54 | Fichier invalide (R51 exclu) : diagnostic, et l'élément ou le tag est absent comme s'il n'avait pas été fourni. Il ne remplace pas la version d'un pack de priorité plus basse. | S |
| R55 | Gravité d'un fichier invalide ou d'un tag en échec, provisoire : au chargement d'un monde, une erreur dans un registre `stable: false` (registres de monde) ou dans un tag empêche le monde de se charger ("Errors in the currently selected data pack(s) prevented the world from loading", mode sans échec = pack vanilla seul) ; une erreur dans un registre `stable: true` (butins, recettes, avancements, prédicats...) écarte seulement cet élément. Un rechargement en échec garde les anciennes données ("Reload failed; keeping old data"). | D (textes) ; S (répartition) |
| R56 | Valeurs absentes ou par défaut : le chargeur M2 n'en connaît aucune. Les combinateurs distinguent champ requis (absent : erreur avec chemin JSON), champ facultatif avec défaut (absent : défaut) et champ présent de mauvais type (erreur, jamais le défaut). Provisoire : une valeur `null` présente est une erreur de type (aucun `null` dans les données). | S |

## Entrées / sorties

- **Entrée** : liste ordonnée de sources de packs, de priorité croissante. Une source est le pack vanilla implicite (R3, racine `resources/` des données extraites, hors `data/minecraft/datapacks/`), un pack intégré (R14) ou un pack du monde (dossier ou archive, R19). Plus la version de format attendue (121.0 ou 97.1) et, pour les feature flags, l'ensemble actif (R45).
- **Arbre JSON** : objet (paires dans l'ordre du fichier, clés uniques après R51), tableau, chaîne (Unicode), nombre (forme entière ou décimale et valeur exacte, R50), booléen, `null` (accepté par le lecteur, refusé par les combinateurs, R56). Chaque nœud connaît sa source (pack, chemin de fichier) et son chemin JSON pour les diagnostics. Le choix de conserver l'ordre des clés sert le déterminisme (`docs/ARCHITECTURE.md`), pas la fidélité.
- **Sortie** :
  - pour chaque registre (statique ou à fichiers) : ses identifiants dans un ordre déterministe (R26, R30), et pour un registre à fichiers l'arbre JSON retenu de chaque élément avec son pack d'origine ;
  - pour chaque registre (et `function`) : les tags résolus (listes ordonnées sans doublon, R35) et les tags en échec ;
  - les fichiers de fonctions (texte) et de structures (octets) retenus, non interprétés ;
  - l'ensemble des feature flags actifs et, par pack, l'état de compatibilité (R5) et le drapeau expérimental (R17) ;
  - la liste des diagnostics : gravité (erreur, avertissement, information), pack, chemin de fichier, chemin JSON, message. Le chargeur ne s'arrête pas au premier diagnostic ; c'est l'appelant (`engine/game`) qui applique R55.

## Contrat avec les modules propriétaires (ADR 0005)

Ces points sont des décisions d'architecture du dépôt, pas des règles de fidélité ; ils s'appuient sur les règles ci-dessus.

- **K1. Fourni par `engine/data`** : sources et empilement (R40, R41, R47, R48), lecture JSON (R49 à R54), registres et références par identifiant ou par tag, combinateurs de codecs (champs requis ou facultatifs, unions par champ `type`, listes, identifiants, références de registre, références `#tag` ou liste en ligne), résolution des tags (R31 à R37), diagnostics.
- **K2. Validé par le chargeur M2 seul** :
  - `pack.mcmeta` : forme, compatibilité (R4 à R7), flags ;
  - attribution de chaque fichier à un registre et identifiant valide (R8 à R16, R20, R24) ;
  - syntaxe JSON de tous les fichiers `.json` (R49 à R54), sans connaître leur schéma ; tout type JSON est admis à la racine d'un élément (R29) ;
  - format des fichiers de tags et toutes leurs références, éléments comme tags, cycles compris (R32 à R37), contre l'univers de R33, qui ne demande que la liste des identifiants, pas le contenu décodé ;
  - comptes par registre égaux à `data-registries-inventory.md` et zéro diagnostic d'erreur sur les données de la cible (critère de sortie de M2).
- **K3. Laissé aux modules propriétaires**, chacun dans son jalon : schéma et décodage typé des éléments, défauts des champs facultatifs (R56), contraintes de valeurs (plages, entier contre décimal), références vers d'autres registres écrites dans le corps d'un élément (identifiants et `#tags` à l'intérieur d'un biome, d'un butin, d'une recette...), filtrage par feature flag des entrées statiques (R46), interprétation des fonctions (`game`) et des structures NBT (`core` pour le format, `worldgen` pour le sens). Une référence non résolue dans un corps d'élément est un diagnostic émis par le codec propriétaire via l'infrastructure de K1, avec la même forme que ceux de M2.
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

## Questions ouvertes

- Toutes les règles S ci-dessus restent provisoires jusqu'à leurs sondes (tableau précédent). Sans oracle exécutable, aucune n'est levée ; R30 (ordre des éléments d'un registre à fichiers) n'a pas de sonde serveur sans joueur et sera levée au jalon M8 par le paquet de données de registres envoyé à un client vanilla.
- Forme d'un pack d'archive (R19) et ordre de découverte des packs du dossier `datapacks/` d'un monde neuf : à relever avec P1 et P3 (ordre affiché par `datapack list enabled`).
- Rechargement : quels registres `/reload` recharge (la documentation cite tags, butins, recettes, avancements, modificateurs, prédicats, fonctions, structures ; `context_float_provider` et `context_int_provider`, `stable: true`, ne sont pas cités). Hors M2 ; sonde à écrire avec `engine/game`.
- Contradictions relevées entre sources, tranchées par les données :
  - le rapport Overworld parle de "103 fichiers d'overlay expérimental" : ce sont les fichiers du pack intégré `trade_rebalance`, pas des overlays de `pack.mcmeta` (R42) ; le compte 103 est confirmé ;
  - la page "Data pack" de la documentation liste encore `worldgen/configured_carver` et `worldgen/configured_feature`, alors que les données 26.3 utilisent `worldgen/carver` et `worldgen/feature` (la page "Tag" note ce renommage en 26.3) ; la même page omet de sa liste des dossiers expérimentaux `cat_sound_variant`, `chicken_sound_variant`, `cow_sound_variant`, `pig_sound_variant` et les sous-dossiers récents de `worldgen`, tous `stable: false` dans `datapack.json` ;
  - la page "Pack.mcmeta" se contredit sur les majuscules dans les noms de dossiers d'overlay (R48) ;
  - le fichier de langue contient encore des noms de packs d'expériences passées (`bundle`, `update_1_20`, `update_1_21`, `winter_drop`, `locator_bar`) absents des packs intégrés extraits : seuls les trois packs de R42 existent dans la cible.
- `version.json` des données extraites donne en D les valeurs que `reference-binaries.md` garde en S (ses règles R2 à R4 : data version 5023, protocole 777, formats 121.0 et 97.1) ; la mise à jour de cette spec est à faire par son propriétaire.
- La spec passe à `relu` après relecture, et à `figé` quand les sondes P1 à P17 ont été exécutées et R30 levée au jalon M8.

# États de blocs et identifiants globaux

Statut : brouillon
Jalon : M3
Risque : B

## Périmètre

Couvre, pour `engine/world` :

- la structure de `generated/reports/blocks.json` (blocs, propriétés, valeurs, état par défaut, identifiants d'état) ;
- la règle qui assigne les identifiants d'état globaux, recalculée et comparée aux 35 723 identifiants du rapport ;
- la représentation NBT d'un état de bloc dans les palettes (gabarits de structures, sections de chunk) ;
- les cas particuliers : blocs sans propriété, liquides, `waterlogged`, ordres de valeurs ;
- le classement des entrées de palette invalides : corrigées avec un avertissement, jamais cause de refus du chunk (R22).

Ne couvre pas :

- le comportement des blocs (formes, mises à jour, lumière émise, fluides dérivés de `level`) : specs M5 ;
- la syntaxe textuelle `id[prop=valeur]` des commandes, sauf pour décrire les sondes ;
- la mise à niveau des états d'anciennes data versions (correcteurs de données) ;
- le stockage en sections et en fichiers : `world-chunk-format.md` et `world-anvil-region.md`.

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `generated/reports/blocks.json` (7 330 564 octets), `generated/reports/registries.json` (registre `minecraft:block`), `version.json` (`world_version` 5023), les 1511 gabarits `resources/data/minecraft/structure/**/*.nbt`.
- **Calculs** : `world_ref.py` (annexe de `world-vectors.md`), sous-commandes `states` et `templates`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, pages "Chunk format" (historique : 26.3 Snapshot 7) et "Java Edition 26.3 Snapshot 7" (renommage `Name` -> `id`, `Properties` -> `properties`, forme compacte de l'état par défaut, data version 5009 de ce snapshot).
- **Rapports locaux** (indices) : rapport Overworld, section 1.2 et tableau de la partie I (comptes 1 286 / 35 723, que les données confirment). Aucune règle ci-dessous n'en vient seule.
- **Sondes de l'oracle** : aucune exécutée. Sondes PC2 et PB1 décrites dans `world-vectors.md`.

Niveaux : **D** valeur lue telle quelle dans un fichier extrait ; **C** propriété établie par calcul reproductible sur ces fichiers ; **M** non utilisé (oracle non lancé) ; **S** documentation publique seule, rapport local ou hypothèse ; une règle S énonce elle-même le comportement **provisoire** que le moteur applique en attendant la sonde.

## Règles

### Structure du rapport

| Réf | Règle | Preuve |
|---|---|---|
| R1 | `blocks.json` est un objet de 1286 entrées dont la clé est l'identifiant de bloc avec espace de noms. Chaque entrée a une clé `states` (liste) et, si le bloc a des propriétés, une clé `properties` (objet : nom de propriété -> liste des valeurs, toutes des chaînes). Aucune autre clé au niveau bloc. | D |
| R2 | Chaque état a `id` (entier), `properties` (objet nom -> valeur, toutes les propriétés du bloc) si le bloc en a, et `default: true` pour l'état par défaut seulement. Aucune autre clé. | D |
| R3 | Comptes : 1286 blocs, 35 723 états ; les `id` couvrent exactement 0 à 35 722, chacun une fois. 402 blocs sans propriété (un seul état, sans clé `properties`), 884 avec. Exactement un état par défaut par bloc. 93 noms de propriété distincts. Plus gros bloc : `minecraft:note_block`, 1350 états (27 x 25 x 2). | C |
| R4 | Le registre `minecraft:block` de `registries.json` contient les mêmes 1286 identifiants, `protocol_id` de 0 à 1285, valeur par défaut `minecraft:air`. | D |

### Assignation des identifiants d'état

| Réf | Règle | Preuve |
|---|---|---|
| R5 | Les blocs occupent des plages d'identifiants consécutives dans l'ordre croissant de leur `protocol_id` (R4). Le premier identifiant d'un bloc est la somme des nombres d'états des blocs qui le précèdent. | C |
| R6 | Dans un bloc, les états forment le produit cartésien des listes de valeurs. Les propriétés sont triées par nom, ordre croissant des points de code (tous les noms sont en ASCII minuscule, chiffres et `_`). Les valeurs gardent l'ordre de leur liste dans `blocks.json`. La dernière propriété varie le plus vite. Formule : `id = base + somme_k(i_k * produit_{j>k}(n_j))`, `i_k` rang de la valeur dans sa liste, `n_j` taille de la liste. | C |
| R7 | Recoupement : R5 et R6 recalculent les 35 723 identifiants à partir des seules listes de propriétés ; résultat identique à `blocks.json` pour 1286 blocs sur 1286 et 35 723 états sur 35 723 (`world_ref.py states`). | C |
| R8 | L'ordre des clés de `properties` dans `blocks.json` n'est pas l'ordre des identifiants : pour 12 blocs, la propriété `type` vient en premier (`chest`, `trapped_chest`, les 8 `*copper_chest`, `piston_head`, `moving_piston`). Utiliser l'ordre du JSON donne de faux identifiants pour ces 12 blocs. Une implémentation trie toujours par nom. | C |
| R9 | L'ordre des valeurs dépend du bloc, pas du nom de propriété : 13 noms ont plusieurs listes. Exemple `facing` : `north, south, west, east` (335 blocs), `north, east, south, west, up, down` (41 blocs), `down, north, south, west, east` (1 bloc). Les booléens sont toujours listés `true, false` (1120 occurrences) : `true` a le rang 0. Les entiers sont croissants mais ne commencent pas toujours à 0 (`level` de 1 à 3 pour les chaudrons, `distance` de 1 à 7 pour les feuilles). | D |
| R10 | L'état par défaut n'est pas toujours le premier : c'est le cas de 746 blocs. Exemples : `grass_block` plage 8-9, défaut 9 (`snowy=false`) ; `oak_log` plage 139-141, défaut 140 (`axis=y`) ; `redstone_wire` plage 5567-6862, défaut 6727 ; `chest` plage 5543-5566, défaut 5544. | D |
| R11 | Identifiants à connaître : `air` 0, `stone` 1, `bedrock` 88, `water` 89 à 104, `lava` 105 à 120, `void_air` 18 649, `cave_air` 18 650. | D |
| R12 | Taille de la palette globale : 35 723 états, soit 16 bits par identifiant (`ceil(log2(35 723))`, car 2^15 = 32 768 < 35 723). | C |

### Cas particuliers

| Réf | Règle | Preuve |
|---|---|---|
| R13 | Liquides : `water` et `lava` sont des blocs à une seule propriété `level`, valeurs `0` à `15`, défaut `level=0` (89 et 105). Aucune propriété ne distingue source, écoulement ou chute : l'état de fluide en est dérivé (spec M5). | D |
| R14 | 505 blocs ont une propriété `waterlogged` (`true, false`). Six blocs ont une propriété `level` : `water`, `lava`, `light`, `composter`, `water_cauldron`, `powder_snow_cauldron`. | D |
| R15 | Un bloc sans propriété a un seul état, sans clé `properties` dans `blocks.json` ; c'est son état par défaut. | D |

### Représentation NBT d'un état

| Réf | Règle | Preuve |
|---|---|---|
| R16 | Dans les 1511 gabarits de structures (data version 5023), les palettes sont des List de Compound (1651 palettes). Chaque entrée a une clé String `id` (identifiant de bloc) et, si le bloc a des propriétés, une clé Compound `properties` dont toutes les valeurs sont des String. Aucune clé `Name` ni `Properties`. 6472 entrées n'ont que `id`, toutes pour des blocs sans propriété ; 17 770 entrées ont `properties`, toujours avec toutes les propriétés du bloc. | C |
| R17 | Les gabarits n'omettent pas l'état par défaut : 4076 entrées égales à l'état par défaut de leur bloc portent quand même `properties` (exemple : `{id:"minecraft:grass_block",properties:{snowy:"false"}}`). | C |
| R18 | L'ordre des clés dans `properties` n'est pas fixe (non alphabétique dans 11 452 des 17 770 entrées des gabarits). Un lecteur ne dépend jamais de cet ordre. | C |
| R19 | Dans les chunks de 26.3, les entrées de la palette `block_states` utilisent les mêmes noms `id` et `properties` (renommage en 26.3 Snapshot 7, data version 5009). Une entrée peut omettre les propriétés de l'état par défaut, et une palette peut être une List de String (identifiants de blocs, état par défaut) quand toutes ses entrées sont des états par défaut. **Provisoire** : le lecteur accepte (a) List de Compound `{id, properties?}`, (b) List de String ; l'écrivain produit la forme de R16 (Compound, `properties` complet si le bloc a des propriétés). | S |
| R20 | Entrée Compound dont `properties` est absent ou partiel. **Provisoire** : chaque propriété manquante prend la valeur de l'état par défaut du bloc. | S |
| R21 | Entrée invalide. **Provisoire** : identifiant de bloc inconnu -> `air` (identifiant 0) avec un avertissement ; propriété inconnue -> ignorée ; valeur hors liste -> valeur de l'état par défaut pour cette propriété. Le fichier n'est pas réécrit tant que le chunk n'est pas modifié ; dès que le chunk est sauvegardé, la valeur corrigée est écrite à la place de l'originale, qui n'est pas conservée. Classement complet de ces cas : R22. | S |
| R22 | Classement des entrées de palette invalides (révision du 2026-10-28, décision D4 de `world-decisions-log.md`). **Provisoire** : (a) une entrée qui n'est ni String ni Compound, ou un Compound sans `id` String, est traitée comme un bloc inconnu (R21) : `air` et avertissement ; (b) aucune entrée invalide, de quelque nature que ce soit, ne fait refuser le chunk : seules les incohérences du conteneur (palette vide, longueur de `data`, index hors palette, `world-chunk-format.md` R10) le refusent ; (c) chaque correction de R20 ou R21 produit un avertissement qui nomme la section, l'entrée de palette et la propriété ou le bloc concernés ; (d) deux entrées corrigées vers le même état restent deux entrées de palette à la lecture, donc les index restent valides ; l'écriture compacte la palette (`world-chunk-format.md`, Entrées / sorties). Le classement vient du moteur : la sonde PB1 relève ce que fait le serveur, sans en déduire de refus de chunk. | S |

## Entrées / sorties

- **Chargement** : entrée = `blocks.json` et registre `minecraft:block` ; sortie = table identifiant -> (bloc, valeurs de propriétés) et table inverse, avec l'état par défaut de chaque bloc. Le chargeur vérifie R3, R5 à R7 et échoue sur toute incohérence (identifiant manquant, en double, ou qui contredit R6).
- **Identifiants** : entiers de 0 à 35 722. Aucune valeur négative. Les identifiants d'état dépendent de l'ensemble des blocs chargés : ils ne sont jamais écrits sur disque (les palettes de chunk stockent les noms, voir `world-chunk-format.md`).
- **NBT** : lecture selon R19 à R22 ; écriture selon R16. Les comparaisons avec l'oracle portent sur l'arbre décodé, pas sur les octets (comme `core-nbt.md` R30).

## Cas limites

- Les 12 blocs de R8 : un test unitaire vérifie `chest[facing=north,type=left,waterlogged=true]` = 5545 (rang 2 du bloc), alors que l'ordre du JSON (`type` d'abord) donnerait une autre valeur.
- Rang 0 d'un booléen = `true` (R9) : `acacia_fence` premier état (15 747) = toutes ses propriétés à `true`.
- Propriétés entières qui ne commencent pas à 0 (R9) : le rang est la position dans la liste, pas la valeur.
- Plus gros bloc (R3) : `note_block`, 1350 états, sert de test de débordement des produits.
- Dernier identifiant : 35 722, qui tient sur 16 bits (R12).

## Vecteurs de test

Voir `world-vectors.md`, partie BS : comptes (BS1), identifiants calculés (BS2), gabarits (BS3), sondes PC2 et PB1.

## Questions ouvertes

- R19 et R20 : forme réelle des palettes écrites par le serveur 26.3 (Compound complet, propriétés élidées, List de String) : sondes PC2 (monde de débogage `debug_all_block_states`) et PC1 (monde plat) ; lecture par le serveur de ces formes : sonde PB1, variantes `string_palette` et `partial_props`.
- R21 et R22 : comportement du serveur face à une entrée invalide : sonde PB1, variantes `unknown_block` et `bad_value` ; le cas d'une entrée qui n'est ni String ni Compound n'a pas de variante, à ajouter si PB1 montre que le serveur refuse ou régénère le chunk.
- R5 à R7 sont C sur les données ; la sonde PC2 les confirme en M en vérifiant que le monde de débogage contient les 35 722 états autres que `air`, rangés par identifiant croissant.

# NBT binaire et SNBT : vecteurs et sondes

Statut : brouillon
Jalon : M1
Risque : B

Compagnon de `core-nbt.md`. Les références Rn renvoient aux règles de cette spec.

## Outil et commandes

Tous les calculs viennent de `nbt_ref.py`, lecteur et écrivain NBT minimal sans dépendance (Python 3, bibliothèque standard), dont le texte intégral est en annexe. Il ne réutilise aucun code du jeu. Le copier dans un dossier de scripts hors des données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I nbt_ref.py scan    "$REF/resources"                                      # corpus (R1, R14, R16, R21, R26, R30)
python -I nbt_ref.py order   "$REF/resources/data/minecraft/structure"             # ordre des clés (R29)
python -I nbt_ref.py ftable  "$REF/resources/data/minecraft/structure/"{empty,igloo/top,village/plains/villagers/baby,bastion/hoglin_stable/air_base}.nbt   # tableau F
python -I nbt_ref.py hex     "$REF/resources/data/minecraft/structure/empty.nbt"   # octets de F1
python -I nbt_ref.py tree    "$REF/resources/data/minecraft/structure/village/plains/villagers/baby.nbt"   # détails de F3
python -I nbt_ref.py dump    FICHIER.nbt                                         # résumé d'un fichier quelconque
python -I nbt_ref.py vectors                                                       # V01 à V26, E01 à E09, W1 à W4, B1 à B10
python -I nbt_ref.py probes  "$REF/resources/data/minecraft/structure/empty.nbt" probe_out   # fichiers p00 à p20 de PB1
python -I nbt_ref.py quota   probe_out/p19_extra_bytearray_64MiB.nbt               # Q0 à Q9 (R32, R60)
python -I nbt_ref.py containers "$REF/resources/data/minecraft/structure/empty.nbt" containers_out   # C01 à C12 (R61, R62)
python -I nbt_ref.py snbtdepth                                                     # T53 à T59 (R58)
```

Les sorties de `nbt_ref.py` sont en anglais ; les tableaux ci-dessous les reprennent en français.

Statut des valeurs attendues : les octets de V01 à V26 sont C (calculés à partir de R1 à R11) ; leur décodage en Byte_Array, Long_Array et chaînes non ASCII dépend de règles S. Les résultats attendus de E01 à E09, W1 à W4, B1 à B10, Q1 à Q9, C01 à C12, PB1 à PS3 et T1 à T62 sont **provisoires** (S) tant que la sonde n'a pas été exécutée ou la décision confirmée ; les octets, tailles, empreintes et totaux d'unités qu'ils citent sont C.

## V : vecteurs binaires valides

Racine nommée sauf V20. Lecture puis réécriture : mêmes octets.

| Vecteur | Valeur | Octets |
|---|---|---|
| V01 | End seul (racine absente) | `00` |
| V02 | Byte -1, nom `b` | `01 00 01 62 ff` |
| V03 | Short -2 | `02 00 00 ff fe` |
| V04 | Int 16909060 (0x01020304) | `03 00 00 01 02 03 04` |
| V05 | Long -9223372036854775808 | `04 00 00 80 00 00 00 00 00 00 00` |
| V06 | Float 1.5 | `05 00 00 3f c0 00 00` |
| V07 | Double -0.0 | `06 00 00 80 00 00 00 00 00 00 00` |
| V08 | Byte_Array [1, -1] | `07 00 00 00 00 00 02 01 ff` |
| V09 | String `hi` | `08 00 00 00 02 68 69` |
| V10 | List vide, type End | `09 00 00 00 00 00 00 00` |
| V11 | List d'Int [7, 8] | `09 00 00 03 00 00 00 02 00 00 00 07 00 00 00 08` |
| V12 | Compound {a: Byte 1} | `0a 00 00 01 00 01 61 01 00` |
| V13 | Int_Array [1, 2] | `0b 00 00 00 00 00 02 00 00 00 01 00 00 00 02` |
| V14 | Long_Array [1] | `0c 00 00 00 00 00 01 00 00 00 00 00 00 00 01` |
| V15 | String U+0000 | `08 00 00 00 02 c0 80` |
| V16 | String U+00E9 | `08 00 00 00 02 c3 a9` |
| V17 | String U+20AC | `08 00 00 00 03 e2 82 ac` |
| V18 | String U+1F600 | `08 00 00 00 06 ed a0 bd ed b8 80` |
| V19 | Compound vide, nom vide | `0a 00 00 00` |
| V20 | Compound vide, racine réseau sans nom (R20) | `0a 00` |
| V21 | Compound {z: Byte 1, a: Byte 2}, ordre z puis a conservé (R30) | `0a 00 00 01 00 01 7a 01 01 00 01 61 02 00` |
| V22 | Float NaN `7fc00000` | `05 00 00 7f c0 00 00` |
| V23 | Double +infini | `06 00 00 7f f0 00 00 00 00 00 00` |
| V24 | List vide, type Byte : lue comme liste vide (R23), réécrite en type End (R22) | `09 00 00 01 00 00 00 00` |
| V25 | Float NaN de charge non canonique `7fc00001` : relu puis réécrit à l'identique (R35) | `05 00 00 7f c0 00 01` |
| V26 | Double NaN de charge non canonique `7ff0000000000001` : relu puis réécrit à l'identique (R35) | `06 00 00 7f f0 00 00 00 00 00 01` |

Écriture attendue de V24 : `09 00 00 00 00 00 00 00` (V10). C'est le seul vecteur dont la réécriture diffère de l'entrée.

## E : entrées binaires invalides

Résultat attendu provisoire, à confirmer par PB1 (S).

| Vecteur | Cas | Octets | Attendu |
|---|---|---|---|
| E01 | Int tronqué | `03 00 00 01 02 03` | échec (R33) |
| E02 | type 13 | `0d 00 00` | échec (R33) |
| E03 | List de type End, longueur 1 | `09 00 00 00 00 00 00 01` | échec (R24) |
| E04 | Byte_Array de longueur -1 | `07 00 00 ff ff ff ff` | échec (R25) |
| E05 | List de Byte de longueur -1 | `09 00 00 01 ff ff ff ff` | accepté, List vide (R25) |
| E06 | String contenant l'octet `00` | `08 00 00 00 01 00` | échec (R12) |
| E07 | String en UTF-8 standard sur 4 octets | `08 00 00 00 04 f0 9f 98 80` | échec (R12) |
| E08 | Compound sans End final | `0a 00 00 01 00 01 61 01` | échec (R33) |
| E09a | 512 List imbriquées en racine : `09 00 00` + 511 fois `09 00 00 00 01` + `00 00 00 00 00` (2563 octets, SHA-256 `c577920edeaead13ad505ca09a1859049301d88e58ecb8a53c7e10cae672d02e`) | voir cas | accepté (R31) |
| E09b | 513 List imbriquées, même construction (2568 octets, SHA-256 `c6ca4e050c4cdab8eb7f9cd9924855aba1f9de7fd2539de59a8e34b3b45c72cd`) | voir cas | échec (R31) |

## W : profondeur à l'écriture binaire (R57)

Sortie de `nbt_ref.py vectors`. Profondeur en mémoire : List et Compound imbriqués, racine comprise ; profondeur binaire : la même, enveloppes des listes hétérogènes (R63) comprises. Racine List de nom vide.

| Vecteur | Arbre | Profondeur mémoire / binaire | Attendu |
|---|---|---|---|
| W1 | 512 List imbriquées, la plus profonde vide (type End) | 512 / 512 | écrit : 2563 octets, SHA-256 `c577920edeaead13ad505ca09a1859049301d88e58ecb8a53c7e10cae672d02e` (identique à E09a) |
| W2 | 513 List imbriquées | 513 / 513 | `TOO_DEEP`, rien n'est écrit |
| W3 | 511 List imbriquées, la 512e étant la liste hétérogène `[1b, 2]` | 512 / 513 | `TOO_DEEP`, rien n'est écrit |
| W4 | 510 List imbriquées, la 511e étant la liste hétérogène `[1b, 2]` | 511 / 512 | écrit : 2571 octets, SHA-256 `a3a60b2570cb6abc9e115e64bc8579ee4f92ff42b6233f07b84edfe5425e6169` |

## B : lecture booléenne (R36)

Sortie de `nbt_ref.py vectors`. Colonne "décision" : comportement provisoire de R36 (valeur d'origine non nulle). Colonne "via Byte" : lecture de la documentation publique (non nulle après conversion en Byte), donnée pour comparaison.

| Vecteur | Valeur | Octets (racine de nom vide) | Décision (attendu) | Via Byte |
|---|---|---|---|---|
| B1 | Byte 2 | `01 00 00 02` | vrai | vrai |
| B2 | Int 256 | `03 00 00 00 00 01 00` | vrai | faux |
| B3 | Double 0.5 | `06 00 00 3f e0 00 00 00 00 00 00` | vrai | faux |
| B4 | Float NaN `7fc00000` | `05 00 00 7f c0 00 00` | vrai | faux |
| B5 | Double 0.0 | `06 00 00 00 00 00 00 00 00 00 00` | faux | faux |
| B6 | Double -0.0 | `06 00 00 80 00 00 00 00 00 00 00` | faux | faux |
| B7 | Byte 0 | `01 00 00 00` | faux | faux |
| B8 | Short -256 | `02 00 00 ff 00` | vrai | faux |
| B9 | String `true` | `08 00 00 00 04 74 72 75 65` | faux (non numérique) | faux |
| B10 | List vide | `09 00 00 00 00 00 00 00` | faux (non numérique) | faux |

## Q : quota de mémoire estimée (R32, R60)

Sortie de `nbt_ref.py quota`. Racines Compound de nom vide ; `{a:...}` et `{b:...}` désignent un Compound à une entrée. Total = octets consommés + 24 par objet balise.

| Vecteur | Entrée | Octets | Total (unités) | Quota | Attendu |
|---|---|---|---|---|---|
| Q0 | p19 (PB1), Byte_Array de 67 108 864 zéros ajouté à F1 | 67 109 020 | 67 109 452 | 134 217 728 | accepté |
| Q1 | F1 : 148 octets + 17 objets balises | 148 | 556 | 134 217 728 | accepté |
| Q2 | `{a:[B; 964 zéros]}` | 976 | 1024 | 1024 | accepté (total égal au quota) |
| Q3 | `{a:[B; 965 zéros]}` | 977 | 1025 | 1024 | `TOO_LARGE` au dernier octet (End de la racine) |
| Q4 | `{a:[34 Int à 0]}` (List d'Int) | 149 | 1013 | 1024 | accepté |
| Q5 | `{a:[35 Int à 0]}` | 153 | 1041 | 1024 | `TOO_LARGE` dès l'en-tête de la liste (60 + 35 x 28 = 1040 > 1024) |
| Q6 | `{a:[B; 134 217 668 zéros]}` | 134 217 680 | 134 217 728 | 134 217 728 | accepté |
| Q7 | `{a:[B; 134 217 669 zéros]}` | 134 217 681 | 134 217 729 | 134 217 728 | `TOO_LARGE` |
| Q8 | `{b:[20 000 000 Byte à 0]}`, 19 477 octets en gzip niveau 9 | 20 000 013 | 500 000 061 | 134 217 728 | `TOO_LARGE` dès l'en-tête de la liste |
| Q9 | Q8 tronqué juste après l'en-tête, longueur déclarée 2 147 483 647 | 12 | (entrée tronquée) | 134 217 728 | `TOO_LARGE` dès l'en-tête, pas d'erreur de fin de flux |

Q2 à Q5 permettent de tester la règle avec un quota explicite petit ; Q6 et Q7 vérifient la valeur par défaut. La taille gzip de Q8 dépend du compresseur (la relecture du code a mesuré 19 471 octets pour une construction voisine) ; seul le total d'unités est normatif.

## C : conteneurs (R61, R62)

Fichiers produits par `nbt_ref.py containers` à partir de F1 (`empty.nbt`). C01 à C07 partent du fichier compressé d'origine ; C08 à C12 utilisent des blocs DEFLATE "stockés" (sans compression), donc indépendants de la bibliothèque de compression. Inverser = complément bit à bit des octets visés.

| Vecteur | Construction | Octets | SHA-256 du fichier | Attendu |
|---|---|---|---|---|
| C01 | F1 d'origine | 124 | `97acb694f31451123cad2c8a4e4af638fbf3bc9e4282da2ea9a2a8d38cfe5022` | accepté, `DataVersion` 5023 |
| C02 | CRC-32 inversé (octets -8 à -5) | 124 | `83b61d068343916c7e6c8d28c8473aa6cad0aa704a5dfbcb6452d5aefe3af9e2` | `CORRUPT_CONTAINER` |
| C03 | ISIZE inversé (4 derniers octets) | 124 | `94b8b57a3e190a8723b3de52893ec98e1dd67dce1f8decd18a67a5ac15364b19` | `CORRUPT_CONTAINER` |
| C04 | trailer retiré (8 derniers octets) | 116 | `6084901128dff1f341fa9dd119fcd652820905de399c5b83eb219ca77e134c55` | `CORRUPT_CONTAINER` |
| C05 | trailer tronqué (4 derniers octets retirés) | 120 | `46d87a4a0938b543bff21bcf001b36e87e4f726c8a1fc2168f99155bd8991e3e` | `CORRUPT_CONTAINER` |
| C06 | bit réservé 0x20 de FLG mis à 1 | 124 | `7d116b17af0f77be85d6b75b4c78f8f082f125b1cbb610c68cf33906e621f5d7` | accepté (R62) |
| C07 | 4 octets `00` ajoutés après le membre | 128 | `cba7c4991cddca8e0cd1fee1db27a14f99514ec8ddb144d02e8966e861aaa124` | accepté (R61, octets après le conteneur ignorés) |
| C08 | gzip en bloc stocké de F1, en-tête de R16 | 171 | `df59c0fb96f2764d7e1dbda1d623a84575778d2c9ef0d740336f78054fdbf0b5` | accepté, `DataVersion` 5023 |
| C09 | C08 dont l'octet de poids faible de `DataVersion` passe de `9f` à `a0`, CRC d'origine | 171 | `85194b67e4cc2736f9ccad9766c44c43c58a5723c3304a0160d3d9fd2be87487` | `CORRUPT_CONTAINER` (sans vérification : `DataVersion` 5024) |
| C10 | zlib en bloc stocké de F1 (en-tête `78 01`) | 159 | `4373a2cd155f013bab1ad836df8050b5e79847e1793b64e071679c95908e5f37` | accepté, `DataVersion` 5023 |
| C11 | Adler-32 inversé | 159 | `caceeba85434e249d316e0ab5b4eb9e0b8b8b71aad38c95f8b299919e0b0b8d3` | `CORRUPT_CONTAINER` |
| C12 | Adler-32 retiré | 155 | `80399581468da3d89fedc7d09ee717c95c0989c2df5a312d706bf03b73ea009e` | `CORRUPT_CONTAINER` |

## F : fichiers réels

Fichiers de `resources/data/minecraft/structure/` (C). Chaque valeur du tableau (empreintes, tailles, en-tête, racine, ordre des clés, contenu de `size`, nombre et type d'éléments des listes) est imprimée par `nbt_ref.py ftable` sur ces quatre fichiers ; les octets de F1 par `nbt_ref.py hex` ; les détails de F3 par `nbt_ref.py tree`. Tous : conteneur gzip, en-tête de R16, racine Compound de nom vide, clés racines dans l'ordre `size`, `entities`, `blocks`, `palette`, `DataVersion` (Int 5023).

| Fichier | SHA-256 du fichier compressé | Octets compressés / décompressés | SHA-256 des octets décompressés | Éléments des listes racines |
|---|---|---|---|---|
| F1 `empty.nbt` | `97acb694f31451123cad2c8a4e4af638fbf3bc9e4282da2ea9a2a8d38cfe5022` | 124 / 148 | `ea73f15eba7e75a69fc179de24b07781b1db2a5637af0cc9b50bbf029a24ea3f` | size 3 Int [1,1,1] ; entities 0 (type End) ; blocks 1 Compound ; palette 1 Compound |
| F2 `igloo/top.nbt` | `013477b94c733e01453c6f1e796e3b959a79b8f33f4a364a23b6cf0eaaf3a47f` | 914 / 6298 | `2ec42e950b2dbfd2db6cc4ab440b29046c5fbebfd2952d8a5e600c20917b959a` | size 3 Int [7,5,8] ; entities 0 (type End) ; blocks 152 ; palette 11 |
| F3 `village/plains/villagers/baby.nbt` | `6fe50097ea5a52d9f1b6b083b6427dca33c084be1f9351f39888483c5571947f` | 712 / 1134 | `1de0d5f67910dfae108852eb3bf91ece28ec115136700613893513989679d251` | size 3 Int [1,2,1] ; entities 1 Compound ; blocks 2 ; palette 2 |
| F4 `bastion/hoglin_stable/air_base.nbt` | `784dd30d618243b1f7713f33a6a49b87fc677fd80c61740c94e956b81e085aa9` | 80375 / 1078387 | `2e28eafc83c37d958453de50ad9e3b76cd78ed91c1f5509ca3827e5ed846132d` | size 3 Int [30,24,48] ; entities 0 (type End) ; blocks 29938 ; palette 3 |

F1 en entier (octets décompressés, 148 octets, sortie de `nbt_ref.py hex`), vecteur de lecture et de réécriture complet :

```
0a000009000473697a650300000003000000010000000100000001090008656e7469746965730000000000090006626c6f636b730a00000001090003706f7303000000030000000000000000000000000300057374617465000000000009000770616c657474650a000000010800026964000d6d696e6563726166743a6169720003000b4461746156657273696f6e0000139f00
```

Arbre attendu de F1 : `{size: List<Int>[1,1,1], entities: List<End>[], blocks: List<Compound>[{pos: List<Int>[0,0,0], state: Int 0}], palette: List<Compound>[{id: String "minecraft:air"}], DataVersion: Int 5023}`, clés dans cet ordre.

Détails de F3 utiles aux tests de valeurs (sortie de `nbt_ref.py tree` ; premier élément de `entities`, sous-clé `nbt`, 32 clés) : `UUID` Int_Array `[895037032, -995736946, -1319125422, -1224629455]` ; `Health` Float 20.0 ; `Age` Int -21359 ; `Fire` Short -1 ; `Air` Short 300 ; `LastRestock` Long 0 ; `OnGround` Byte 1 ; `Motion` List<Double> `[0.0, -0.0784000015258789, 0.0]` ; `Rotation` List<Float> `[0.0, -25.82771110534668]` ; `Gossips`, `attributes`, `Inventory` : List vides de type End ; `HandItems` : 2 Compound vides ; `ArmorItems` : 4 Compound vides ; `VillagerData` : Compound de 3 clés.

## Corpus : 1511 fichiers de structures

Sortie de `scan` et `order` (C) :

- 1511 fichiers décodés sans erreur et réécrits à l'identique (R30) ; tous en gzip, racine Compound de nom vide, `DataVersion` 5023.
- Taille décompressée : de 148 à 1 078 387 octets.
- Occurrences par type : voir R1 ; aucun Byte_Array ni Long_Array.
- Types d'éléments de listes : Int 1 172 463, Compound 3569, End 2021, Double 862, Float 291, List 20. Toutes les listes vides (2021) sont de type End.
- Profondeur maximale : 8 conteneurs imbriqués, racine comprise (premier fichier qui l'atteint : `lava_basin.nbt`).
- Compounds d'au moins 2 clés : 1 208 339 ; en ordre lexicographique : 1 189 483 ; en ordre de `bucket_order_key` : 1 208 302 (R29).
- Aucune chaîne non ASCII, aucun NaN, aucun -0.0.

## PB1 : lecture de fichiers altérés par le serveur

Fichiers produits par `nbt_ref.py probes` à partir de F1 (octets décompressés modifiés, puis gzip). Les installer dans un datapack de test `probe_nbt` (`pack.mcmeta` au format de datapack 121.0, voir `version.json`), sous `data/probe/structure/<nom>.nbt`. Pour chaque fichier, sur le serveur de l'oracle, monde neuf, par RCON :

```
forceload add 0 0
setblock 0 100 0 minecraft:glass
place template probe:<nom> 0 100 0
execute if block 0 100 0 minecraft:air
execute if block 0 100 0 minecraft:stone
```

Enregistrer la réponse de chaque commande et les lignes du journal du serveur. Bloc d'air en (0, 100, 0) : fichier accepté ; verre et message d'échec : fichier refusé. `p00` est le témoin : s'il ne produit pas d'air, la méthode est à revoir avant d'interpréter les autres.

Profondeurs exprimées en conteneurs imbriqués, racine comprise. Décalages dans F1 : type d'élément de `entities` à l'octet 38 (0x26), fin de l'entrée de palette juste après `minecraft:air`, End de la racine au dernier octet.

| Fichier | Modification de F1 | Octets | SHA-256 des octets décompressés | Attendu provisoire |
|---|---|---|---|---|
| p00_base | aucune | 148 | `ea73f15eba7e75a69fc179de24b07781b1db2a5637af0cc9b50bbf029a24ea3f` | accepté |
| p01_root_name_x | nom de racine `x` | 149 | `646f136e7e92fbe2182c4d09387b104fe9e8685f70c0a129a30aaf77501c62ec` | accepté (R15) |
| p02_entities_end_len_minus1 | `entities` : `00 ff ff ff ff` | 148 | `f6442a15cc52fb643ae565398436d5596261e7018fa91d1f336aee435bee8642` | accepté, `entities` vide (R25) |
| p03_entities_byte_len_minus1 | `entities` : `01 ff ff ff ff` | 148 | `d8e8cf2856b6d735891a4a9f700c3b6dda58fff9d0d5cb983de8faaac977b818` | accepté, `entities` vide (R25) |
| p04_entities_end_len_1 | `entities` : `00 00 00 00 01` | 148 | `700d0952ef84b800980ef05a95049aa40977f08bc61a23aba51eb3c0d32f599e` | refusé (R24) |
| p05_entities_byte_len_0 | `entities` : `01 00 00 00 00` | 148 | `60663135c381b4e48a734b7210362d1158d03747ff3eb6198f020e469c828db0` | accepté (R23) |
| p06_trailing_byte | octet `00` ajouté à la fin | 149 | `ce63e07bbc061f31939ce2ea19478983bdb2467aaa68a1604a71d0c421e19337` | accepté (R34) |
| p07_truncated | dernier octet retiré | 147 | `ea0714ca494b98feeb75f14d79db19b54a5540f9a6606b8855e31533c69e1411` | refusé (R33) |
| p08_extra_unknown_type_13 | clé `q` de type 13 ajoutée à la racine | 152 | `a6ced3e0eb7b8ec904c24dc07384515b78d4599ccfc7459727ec93ad0e607bd5` | refusé (R33) |
| p09_extra_string_raw_nul | String `q` = `00` | 155 | `9f178baba0ee75426b3389b22d4a1b2e80afa6de9892c1ac513dcc0fabc301f2` | refusé (R12) |
| p10_extra_string_utf8_4byte | String `q` = `f0 9f 98 80` | 158 | `b4a2466ac809b4e05bb63b99e672f2381611dc7cab536e9576811e1ba8faf23d` | refusé (R12) |
| p11_extra_string_mutf8_pair | String `q` = `ed a0 bd ed b8 80` | 160 | `606a0c580a90eb38a8709b2f0cf24c02b36a05dd35354b107fe79303ea9a82c7` | accepté (R11) |
| p12_extra_string_lone_surrogate | String `q` = `ed a0 bd` | 157 | `ebd6350053c0a537daaf85be2089cfbe9a486f61dc6d0ac6d630639db45d15a4` | accepté (R12) |
| p13_extra_bytearray_len_minus1 | Byte_Array `q` de longueur -1 | 156 | `bb6d0f89375c432a61638c71a868df2a0cfe23ae1e8cf8371b33c96f25bb1d6a` | refusé (R2, R25) |
| p14_duplicate_palette_id_stone | 2e clé `id` = `minecraft:stone` dans l'entrée de palette | 170 | `e7c1f288d548c97e0e8c289f3830e0f7cb85dafd0db9df7d36aaea26d480ca21` | pierre en (0,100,0) : dernière valeur (R27) ; air : première |
| p15_extra_deep_510 | List `q` de 510 List imbriquées (511 conteneurs) | 2702 | `0b577bef3e5bc8ea3d9fe756bbdad0c2030f606c0bbd850aac54123a89d40513` | accepté (R31) |
| p16_extra_deep_511 | 512 conteneurs | 2707 | `4cb1e374ed001156f650c0e52916448fa368276b29c4b17071d77496a5e94999` | accepté (R31) |
| p17_extra_deep_512 | 513 conteneurs | 2712 | `794902ec24a490f59d86f3294f17ba31f65fb02e0c7e7f5727cf83ab304d7939` | refusé (R31) |
| p18_extra_deep_513 | 514 conteneurs | 2717 | `20893c718b06789bb103ec041fa13125ad9e529bd31bb9e1d0844e8cac4e03e8` | refusé (R31) |
| p19_extra_bytearray_64MiB | Byte_Array `q` de 67 108 864 zéros | 67 109 020 | `567fb12d47b6e603f4d13f89583d9b69fb641c6c0f3454acba2a43ab3580a2aa` | accepté (R32 ; 67 109 452 unités, R60) |
| p20_extra_list_20M_bytes | List de Byte `q` de 20 000 000 zéros (19 581 octets en gzip) | 20 000 157 | `13d8131eaf3eaba05e3cd8f2441b1d6b17504079f8eb58363e029936e73f4b53` | notre lecteur : `TOO_LARGE` (500 000 589 unités, R60) ; jeu : à relever (s'il accepte, divergence volontaire de sûreté) |

## PB2 : conteneurs altérés lus par le serveur

Copier C02 à C07 et C09 (fichiers `.bin` de `nbt_ref.py containers`) dans le datapack `probe_nbt` sous `data/probe/structure/<nom>.nbt`, puis appliquer à chacun les commandes de PB1. Attendus provisoires : C02, C03, C04, C05 et C09 refusés ; C06 et C07 acceptés. Si le jeu accepte C09, relever le `DataVersion` lu n'est pas possible par commande : noter seulement l'acceptation et le journal.

## PW1 : écriture par le serveur

Par RCON, une commande par ligne, chaque valeur dans sa propre commande pour qu'un refus n'en bloque pas d'autres :

```
data modify storage probe:nbt w set value {}
data modify storage probe:nbt w.z set value 1b
data modify storage probe:nbt w.a set value 2b
data modify storage probe:nbt w.empty set value []
data modify storage probe:nbt w.nz set value -0.0d
data modify storage probe:nbt w.nzf set value -0.0f
data modify storage probe:nbt w.s0 set value "\u0000"
data modify storage probe:nbt w.s1 set value "\u00e9"
data modify storage probe:nbt w.s2 set value "\u20ac"
data modify storage probe:nbt w.s3 set value "\U0001F600"
data modify storage probe:nbt w.het set value [1,"a"]
data modify storage probe:nbt w.ba set value [B;1b,-1b]
data modify storage probe:nbt w.la set value [L;1L]
save-all flush
data get storage probe:nbt w
```

Puis décoder avec `nbt_ref.py dump` le fichier de stockage des commandes du monde (nom exact à relever : le fichier `.dat` du dossier de données du monde modifié par `save-all flush`). Attendus provisoires dans le Compound `w` :

| Clé | Octets attendus de l'entrée (type, nom, charge) | Règle |
|---|---|---|
| `empty` | `09 00 05 65 6d 70 74 79 00 00 00 00 00` | R22 |
| `nz` | type 6, charge `80 00 00 00 00 00 00 00` | R35 |
| `nzf` | type 5, charge `80 00 00 00` | R35 |
| `s0` | type 8, charge `00 02 c0 80` | R11 |
| `s1` | type 8, charge `00 02 c3 a9` | R11 |
| `s2` | type 8, charge `00 03 e2 82 ac` | R11 |
| `s3` | type 8, charge `00 06 ed a0 bd ed b8 80` | R11 |
| `het` | `09 00 03 68 65 74 0a 00 00 00 02 03 00 00 00 00 00 01 00 08 00 00 00 01 61 00` | R47 |
| `ba` | type 7, charge `00 00 00 02 01 ff` | R2, R9 |
| `la` | type 12, charge `00 00 00 01 00 00 00 00 00 00 00 01` | R2, R9 |

Relever aussi l'ordre des clés de `w` (ordre d'insertion `z, a, empty, ...` ou ordre de R29) et l'en-tête gzip du fichier (R16, R17). Le texte de `data get storage probe:nbt w` et de `data get storage probe:nbt w.<clé>` pour chaque clé sert à R52 à R56.

Étape de redémarrage (R63) : après `save-all flush`, arrêter le serveur, relever les octets de `het` dans le fichier, redémarrer, puis `data get storage probe:nbt w.het`. Affichage `[1, "a"]` : le jeu désenveloppe à la lecture ; affichage `[{"": 1}, {"": "a"}]` : pas de désenveloppement, comme le provisoire de R63.

## PW2 : NaN à charge non canonique

Serveur arrêté après PW1 : ajouter au Compound `w` du fichier de stockage, avec un écrivain NBT de l'outil d'oracle, `n1` Float `7fc00001`, `n2` Float `ffc00000`, `n3` Float `7f800001`, `n4` Double `7ff0000000000001`, puis réécrire le fichier en gzip. Redémarrer, puis :

```
data get storage probe:nbt w.n1
data modify storage probe:nbt w.touch set value 1b
save-all flush
```

Attendu provisoire (R35) : motifs de bits inchangés dans le fichier réécrit. Relever aussi le texte de `data get` pour un NaN (R53).

## PW3 : chaîne de plus de 65535 octets

Fonction `data/probe/function/grow.mcfunction` du datapack `probe_nbt`, une ligne :

```
$data modify storage probe:nbt big set value "$(s)$(s)"
```

Par RCON : `data modify storage probe:nbt s set value "<1000 fois a>"`, puis répéter 7 fois `function probe:grow with storage probe:nbt` suivi de `data modify storage probe:nbt s set from storage probe:nbt big` (128 000 caractères). Puis `save-all flush`. Relever : réponses des commandes, longueur par `data get storage probe:nbt s` (la commande renvoie la longueur d'une chaîne), journal et contenu du fichier. Variante : même chose avec `€` (3 octets par caractère, 21 846 caractères = 65 538 octets). Attendu provisoire (R13) : échec de l'écriture du fichier, sans fichier partiel.

## PE1 : égalité

```
data modify storage probe:nbt e set value {a:1b,f:0.0d,c:{x:1,y:2}}
execute if data storage probe:nbt e{a:1b}
execute if data storage probe:nbt e{a:1}
execute if data storage probe:nbt e{f:0.0d}
execute if data storage probe:nbt e{f:-0.0d}
execute if data storage probe:nbt e{c:{y:2,x:1}}
```

Attendus provisoires (R37) : 1, 0, 1, 0, 1. Ces commandes passent par la correspondance partielle des chemins NBT (hors périmètre) ; seule l'égalité des valeurs scalaires est interprétée ici.

## PS2 : conversion de type à la lecture d'une entité

Pour chaque valeur `V` de `7.9d`, `-7.9d`, `7b`, `3000000000L`, `"5"`, avec une étiquette `pN` différente :

```
summon minecraft:armor_stand 0 100 0 {Tags:["pN"],NoGravity:1b,PortalCooldown:V}
data get entity @e[tag=pN,limit=1] PortalCooldown
```

Relever la valeur (ou le refus de `summon`). Attendu provisoire (R38) : 7, -7, 7, -1294967296, 0. Autres candidats à départager : arrondi (8, -8) au lieu de la troncature ; 2147483647 (saturation) ou 0 au lieu de -1294967296 ; `"5"` donne 0 selon la documentation publique.

Booléens (R36) : pour chaque valeur `V` de `2b`, `256`, `-256s`, `0.5d`, `0.0d`, `-0.0d`, avec une étiquette `pbN` différente :

```
summon minecraft:armor_stand 0 100 0 {Tags:["pbN"],NoGravity:V}
data get entity @e[tag=pbN,limit=1] NoGravity
```

Attendu provisoire (décision de R36, valeur d'origine non nulle) : `1b`, `1b`, `1b`, `1b`, `0b`, `0b`. Selon la documentation publique (non nulle après conversion en Byte) : `1b`, `0b`, `0b`, `0b`, `0b`, `0b`. Les cas `256`, `-256s` et `0.5d` départagent. Un NaN ne s'écrit pas en SNBT (R44) : B4 n'a pas de sonde par commande.

## PS3 : profondeur SNBT du jeu (R58, R59, R57)

Pour n = 510, 511, 512, 513, 514, 600, 1000, 2000 et 4000 : `data modify storage probe:nbt d set value <texte>`, le texte étant `[` répété n fois puis `]` répété n fois (T53 à T55). Au-delà de la longueur admise par RCON, placer la commande dans une fonction du datapack `probe_nbt`. Relever l'acceptation ou le message d'erreur, avec la position qu'il indique. Refaire avec `{a:` répété n fois, `1`, puis `}` répété n fois (T56, T57). Pour la plus grande valeur acceptée, `save-all flush`, puis décoder le fichier de stockage : la valeur y est imbriquée sous les compounds du fichier, donc sa profondeur binaire dépasse n ; relever si la sauvegarde échoue, tronque ou réussit (limite d'écriture du jeu, R57). Attendu provisoire : acceptation jusqu'à 512 niveaux, refus au-delà.


## T : SNBT (sonde PS1, et PW1 pour le texte)

Pour chaque entrée : `data modify storage probe:nbt t set value <entrée>`, puis, si elle est acceptée, `data get storage probe:nbt t`. Tous les attendus sont provisoires (S). "erreur" : la commande échoue et `t` n'est pas modifié (R51).

| # | Entrée | Valeur attendue | Texte attendu | Règles |
|---|---|---|---|---|
| T1 | `1` | Int 1 | `1` | R46, R52 |
| T2 | `1B` | Byte 1 | `1b` | R46 |
| T3 | `127b` | Byte 127 | `127b` | R46 |
| T4 | `128b` | erreur | | R46 |
| T5 | `-128b` | Byte -128 | `-128b` | R46 |
| T6 | `240ub` | Byte -16 | `-16b` | R46 |
| T7 | `256ub` | erreur | | R46 |
| T8 | `32767s` | Short 32767 | `32767s` | R46 |
| T9 | `32768s` | erreur | | R46 |
| T10 | `2147483647` | Int 2147483647 | `2147483647` | R46 |
| T11 | `2147483648` | erreur | | R46 |
| T12 | `2147483648l` | Long 2147483648 | `2147483648L` | R46, R52 |
| T13 | `1.5` | Double 1.5 | `1.5d` | R46 |
| T14 | `1.5F` | Float 1.5 | `1.5f` | R46 |
| T15 | `.5` | Double 0.5 | `0.5d` | R46, R53 |
| T16 | `5.` | Double 5.0 | `5.0d` | R46, R53 |
| T17 | `1e3` | Double 1000.0 | `1000.0d` | R46, R53 |
| T18 | `-0.0d` | Double, bits `8000000000000000` | `-0.0d` | R35, R53 |
| T19 | `0x1F` | Int 31 | `31` | R46 |
| T20 | `0b101` | Int 5 | `5` | R46 |
| T21 | `0x11sb` | Byte 17 | `17b` | R46 |
| T22 | `1_000` | Int 1000 | `1000` | R46 |
| T23 | `1_` | erreur | | R46 |
| T24 | `_1` | String `_1` | `"_1"` | R43 |
| T25 | `true` | Byte 1 | `1b` | R44, R52 |
| T26 | `"true"` | String `true` | `"true"` | R44 |
| T27 | `NaN` | String `NaN` | `"NaN"` | R44 |
| T28 | `minecraft:stone` | erreur | | R43 |
| T29 | `"a\"b"` | String `a"b` | `'a"b'` | R45, R54 |
| T30 | `'it\'s'` | String `it's` | `"it's"` | R45, R54 |
| T31 | `"a'b\"c"` | String `a'b"c` | `"a'b\"c"` | R54 |
| T32 | `'a"b\'c'` | String `a"b'c` | `'a"b\'c'` | R54 |
| T33 | `"\x41\u00e9\N{Snowman}"` | String U+0041 U+00E9 U+2603 (octets `41 c3 a9 e2 98 83`) | provisoire : `"Aé☃"` (non ASCII écrit tel quel) | R45, R54 |
| T34 | `"\q"` | erreur | | R45 |
| T35 | `[]` | List vide | `[]` | R47, R56 |
| T36 | `[1,2,]` | List d'Int [1, 2] | `[1, 2]` | R49, R56 |
| T37 | `[1,"a"]` | liste hétérogène (Int 1, String `a`) ; en binaire List de Compound `[{"":1}, {"":"a"}]` | provisoire : `[1, "a"]` | R47, R56 |
| T38 | `[1b,2]` | provisoire : liste hétérogène (Byte 1, Int 2), sans promotion ; en binaire `[{"":1b}, {"":2}]` | provisoire : `[1b, 2]` | R47, R56 |
| T39 | `[B;1b,-1b]` | Byte_Array [1, -1] | `[B; 1b, -1b]` | R48, R56 |
| T40 | `[B;true]` | erreur | | R48 |
| T41 | `[I;1b,2s,3]` | Int_Array [1, 2, 3] | `[I; 1, 2, 3]` | R48 |
| T42 | `[I;1L]` | erreur | | R48 |
| T43 | `[L;1,2L]` | Long_Array [1, 2] | `[L; 1L, 2L]` | R48 |
| T44 | `[I;]` | Int_Array vide | provisoire : `[I;]` | R48, R56 |
| T45 | `{a:1,}` | Compound {a: Int 1} | `{a: 1}` | R49, R56 |
| T46 | `{"a b":1,'k':2b}` | Compound {`a b`: Int 1, k: Byte 2} | `{"a b": 1, k: 2b}` (ordre à relever) | R42, R55 |
| T47 | `{a:1,a:2}` | Compound {a: Int 2} | `{a: 2}` | R42 |
| T48 | `bool(5)` | Byte 1 | `1b` | R50 |
| T49 | `bool("x")` | erreur | | R50 |
| T50 | `uuid("f81d4fae-7dec-11d0-a765-00a0c91e6bf6")` | Int_Array [-132296786, 2112623056, -1486552928, -920753162] | `[I; -132296786, 2112623056, -1486552928, -920753162]` | R50, R8 |
| T51 | `1.0e400` | provisoire : erreur (débordement vers l'infini) | | R46 |
| T52 | `{ a : 1 }` | Compound {a: Int 1} | `{a: 1}` | R41 |
| T53 | `[` x 512 puis `]` x 512 (1024 caractères, SHA-256 `674cf3304bf7104f5ef200c1bb17b24a9b1da199f47cc76bcdc7fd030da23491`) | 512 List imbriquées | `[[...]]` | R58 |
| T54 | `[` x 513 puis `]` x 513 (1026 caractères, SHA-256 `a9ecae6c77e13ad7240afd2d6c8aa7603375874dd33cf982251d2d533db6d32b`) | `TOO_DEEP` au décalage 512 | | R58 |
| T55 | `[` x 100 000 puis `]` x 100 000 (200 000 caractères, SHA-256 `a424233baadccd66f816eefc25b8d44bb91216d9db55b5d20653c5927ac41990`) | `TOO_DEEP` au décalage 512, sans épuisement de la pile | | R58 |
| T56 | `{a:` x 512, `1`, `}` x 512 (2049 caractères, SHA-256 `dbdcf520bf0891d2cba8d7413dca9453f81a25a0fa3c5a505d9f8ca6b4808d56`) | 512 Compound imbriqués | `{a: {a: ... 1}}` | R58 |
| T57 | `{a:` x 513, `1`, `}` x 513 (2053 caractères, SHA-256 `3a8ef8e5bc60fdcfab3c5552912012a3ccc625754e0b43a2c00519ae44870bef`) | `TOO_DEEP` au décalage 1536 | | R58 |
| T58 | `[` x 512, `[I;1]`, `]` x 512 (1029 caractères, SHA-256 `ed31304f3386ed2ec9dad2f8f8cbb18f365a05c857721ec1b7e5cd789e116858`) | `TOO_DEEP` au décalage 512 (le tableau ouvre le niveau 513) | | R58 |
| T59 | `["`, `[` x 600, `"]` (604 caractères, SHA-256 `76d6dde129a4906caca56dfb72a993aff9e8a65627bb839d9ecb7e1798e792ba`) | List d'une String de 600 `[` (crochets cités non comptés) | à relever | R58 |
| T60 | `bool(256)` | Byte 1 | `1b` | R36, R50 |
| T61 | `bool(0.5d)` | Byte 1 | `1b` | R36, R50 |
| T62 | `bool(-0.0d)` | Byte 0 | `0b` | R36, R50 |

Écriture SNBT (R59) : un arbre de 513 List imbriquées (W2) donne `TOO_DEEP` sans décalage et aucun texte ; un arbre de 512 List (W1) s'écrit `[` x 512 puis `]` x 512, soit le texte de T53.

T50 : les quatre entiers sont calculés (C) depuis les 16 octets de l'UUID, poids fort en premier ; les deux premiers coïncident avec l'exemple de la documentation publique.

## Annexe : `nbt_ref.py`

```python
"""Independent minimal NBT reader/writer used to derive spec vectors.

Usage:
  python -I nbt_ref.py dump FILE.nbt          # summary of one file
  python -I nbt_ref.py ftable FILE.nbt...     # hashes, sizes, header, root keys and list contents
  python -I nbt_ref.py tree FILE.nbt          # whole decoded tree, long lists cut after 6 elements
  python -I nbt_ref.py hex FILE.nbt           # decompressed bytes in hexadecimal
  python -I nbt_ref.py scan DIR               # statistics over all *.nbt under DIR
  python -I nbt_ref.py order DIR              # compound key order statistics over all *.nbt under DIR
  python -I nbt_ref.py vectors                # print hand-built binary vectors
  python -I nbt_ref.py probes EMPTY.nbt OUTDIR  # build patched probe structures from structure/empty.nbt
  python -I nbt_ref.py quota [P19.nbt]        # memory quota examples (optionally checks a probe file)
  python -I nbt_ref.py containers EMPTY.nbt OUTDIR  # gzip and zlib trailer vectors built from structure/empty.nbt
  python -I nbt_ref.py snbtdepth              # SNBT nesting depth vectors
"""
import gzip, hashlib, math, os, struct, sys, zlib

sys.setrecursionlimit(10000)

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)
NAMES = ["End", "Byte", "Short", "Int", "Long", "Float", "Double", "ByteArray",
         "String", "List", "Compound", "IntArray", "LongArray"]
MAX_DEPTH = 512
DEFAULT_QUOTA = 128 << 20  # memory units, see quota rule
TAG_WEIGHT = 24            # units per tag object (root, compound value, list element)
# smallest payload of one element of each type, used to pre-check a declared list length
MIN_PAYLOAD = [0, 1, 2, 4, 8, 4, 8, 4, 2, 5, 1, 4, 4]
ARRAY_ELEM = {BYTE_ARRAY: 1, INT_ARRAY: 4, LONG_ARRAY: 8}


class NbtError(Exception):
    def __init__(self, kind, message, offset=None):
        super().__init__("%s: %s%s" % (kind, message, "" if offset is None else " (offset %d)" % offset))
        self.kind, self.offset = kind, offset


class NaNBits:
    """A Float or Double NaN kept as its exact big-endian bits."""
    def __init__(self, raw): self.raw = raw
    def __repr__(self): return "NaN(0x%s)" % self.raw.hex()


def mutf8_decode(b):
    out, i = [], 0
    while i < len(b):
        c = b[i]
        if c == 0:
            raise ValueError("raw NUL byte in modified UTF-8")
        if c < 0x80:
            out.append(c); i += 1
        elif c & 0xE0 == 0xC0:
            if i + 1 >= len(b) or b[i + 1] & 0xC0 != 0x80:
                raise ValueError("bad 2-byte sequence")
            out.append(((c & 0x1F) << 6) | (b[i + 1] & 0x3F)); i += 2
        elif c & 0xF0 == 0xE0:
            if i + 2 >= len(b) or b[i + 1] & 0xC0 != 0x80 or b[i + 2] & 0xC0 != 0x80:
                raise ValueError("bad 3-byte sequence")
            out.append(((c & 0x0F) << 12) | ((b[i + 1] & 0x3F) << 6) | (b[i + 2] & 0x3F)); i += 3
        else:
            raise ValueError("byte 0x%02X not allowed in modified UTF-8" % c)
    # out holds UTF-16 code units; join surrogate pairs
    return "".join(map(chr, out)).encode("utf-16-le", "surrogatepass").decode("utf-16-le", "surrogatepass")


def mutf8_encode(s):
    units = struct.unpack("<%dH" % (len(s.encode("utf-16-le", "surrogatepass")) // 2),
                          s.encode("utf-16-le", "surrogatepass"))
    out = bytearray()
    for u in units:
        if 0x01 <= u <= 0x7F:
            out.append(u)
        elif u <= 0x7FF:
            out += bytes([0xC0 | (u >> 6), 0x80 | (u & 0x3F)])
        else:
            out += bytes([0xE0 | (u >> 12), 0x80 | ((u >> 6) & 0x3F), 0x80 | (u & 0x3F)])
    if len(out) > 0xFFFF:
        raise ValueError("encoded string longer than 65535 bytes")
    return bytes(out)


class Stats:
    def __init__(self):
        self.tags = [0] * 13
        self.list_elem = {}
        self.empty_lists = {}
        self.max_depth = 0
        self.compounds = 0
        self.sorted_compounds = 0
        self.multi_key_compounds = 0
        self.non_ascii_strings = 0
        self.nan = 0
        self.neg_zero = 0


class Reader:
    """quota=None: no memory limit. Units = bytes consumed + TAG_WEIGHT per tag object."""
    def __init__(self, data, stats=None, max_depth=MAX_DEPTH, quota=None):
        self.d, self.p, self.st, self.max_depth = data, 0, stats or Stats(), max_depth
        self.quota, self.tags = quota, 0

    def units(self): return self.p + TAG_WEIGHT * self.tags

    def charge(self, extra=0):
        if self.quota is not None and self.units() + extra > self.quota:
            raise NbtError("TOO_LARGE", "%d units over quota %d" % (self.units() + extra, self.quota), self.p)

    def take(self, n):
        if n < 0 or self.p + n > len(self.d):
            raise EOFError("truncated at offset %d (need %d)" % (self.p, n))
        r = self.d[self.p:self.p + n]; self.p += n
        self.charge()
        return r

    def u8(self): return self.take(1)[0]
    def num(self, fmt): return struct.unpack(">" + fmt, self.take(struct.calcsize(fmt)))[0]

    def string(self):
        raw = self.take(self.num("H"))
        s = mutf8_decode(raw)
        if any(b >= 0x80 for b in raw):
            self.st.non_ascii_strings += 1
        return s

    def payload(self, t, depth):
        st = self.st
        st.tags[t] += 1
        st.max_depth = max(st.max_depth, depth)
        self.tags += 1
        self.charge()
        if t == BYTE: return self.num("b")
        if t == SHORT: return self.num("h")
        if t == INT: return self.num("i")
        if t == LONG: return self.num("q")
        if t in (FLOAT, DOUBLE):
            raw = self.take(4 if t == FLOAT else 8)
            v = struct.unpack(">f" if t == FLOAT else ">d", raw)[0]
            if math.isnan(v):
                st.nan += 1
                return NaNBits(raw)
            if v == 0 and math.copysign(1, v) < 0: st.neg_zero += 1
            return v
        if t == STRING: return self.string()
        if t in (BYTE_ARRAY, INT_ARRAY, LONG_ARRAY):
            n = self.num("i")
            if n < 0: raise ValueError("negative array length %d" % n)
            self.charge(n * ARRAY_ELEM[t])  # pre-check before reading the declared length
            fmt = {BYTE_ARRAY: "b", INT_ARRAY: "i", LONG_ARRAY: "q"}[t]
            return (t, list(struct.unpack(">%d%s" % (n, fmt), self.take(n * struct.calcsize(fmt)))))
        if depth >= self.max_depth:
            raise NbtError("TOO_DEEP", "nesting deeper than %d" % self.max_depth, self.p)
        if t == LIST:
            et, n = self.u8(), self.num("i")
            if et > 12: raise ValueError("unknown list element type %d" % et)
            if et == END and n > 0: raise ValueError("non-empty list of End")
            n = max(n, 0)  # provisional rule R25: a negative list length reads as an empty list
            self.charge(n * (TAG_WEIGHT + MIN_PAYLOAD[et]))  # pre-check before reading the declared length
            st.list_elem[et] = st.list_elem.get(et, 0) + 1
            if n == 0: st.empty_lists[et] = st.empty_lists.get(et, 0) + 1
            return (LIST, et, [self.payload(et, depth + 1) for _ in range(n)])
        if t == COMPOUND:
            items = []
            while True:
                ct = self.u8()
                if ct == END: break
                if ct > 12: raise ValueError("unknown tag type %d" % ct)
                k = self.string()
                if any(k == x for x, _, _ in items): raise ValueError("duplicate key %r" % k)
                items.append((k, ct, self.payload(ct, depth + 1)))
            st.compounds += 1
            keys = [k for k, _, _ in items]
            if len(keys) > 1:
                st.multi_key_compounds += 1
                if keys == sorted(keys): st.sorted_compounds += 1
            return (COMPOUND, items)
        raise ValueError("unknown tag type %d" % t)

    def root(self, named=True):
        t = self.u8()
        if t == END: return (END, None, None)
        if t > 12: raise ValueError("unknown tag type %d" % t)
        name = self.string() if named else None
        return (t, name, self.payload(t, 0))


def write_payload(t, v, out):
    if isinstance(v, NaNBits): out += v.raw
    elif t == BYTE: out += struct.pack(">b", v)
    elif t == SHORT: out += struct.pack(">h", v)
    elif t == INT: out += struct.pack(">i", v)
    elif t == LONG: out += struct.pack(">q", v)
    elif t == FLOAT: out += struct.pack(">f", v)
    elif t == DOUBLE: out += struct.pack(">d", v)
    elif t == STRING:
        b = mutf8_encode(v); out += struct.pack(">H", len(b)) + b
    elif t in (BYTE_ARRAY, INT_ARRAY, LONG_ARRAY):
        fmt = {BYTE_ARRAY: "b", INT_ARRAY: "i", LONG_ARRAY: "q"}[t]
        out += struct.pack(">i", len(v[1])) + struct.pack(">%d%s" % (len(v[1]), fmt), *v[1])
    elif t == LIST:
        _, et, items = v
        if et is None:
            # heterogeneous list (items are (type, value) pairs): each element wrapped as {"": value}
            out += struct.pack(">bi", COMPOUND, len(items))
            for it, iv in items: write_payload(COMPOUND, (COMPOUND, [("", it, iv)]), out)
        else:
            out += struct.pack(">bi", et, len(items))
            for x in items: write_payload(et, x, out)
    elif t == COMPOUND:
        for k, ct, cv in v[1]:
            out.append(ct); write_payload(STRING, k, out); write_payload(ct, cv, out)
        out.append(END)


def depth(t, v, binary):
    """Nested List/Compound levels, root included; binary=True counts heterogeneous-list wrappers."""
    if t == COMPOUND: return 1 + max([depth(ct, cv, binary) for _, ct, cv in v[1]] + [0])
    if t == LIST:
        if v[1] is None:
            return 1 + max([(1 if binary else 0) + depth(it, iv, binary) for it, iv in v[2]] + [0])
        return 1 + max([depth(v[1], x, binary) for x in v[2]] + [0])
    return 0


def write_root(t, name, v, named=True, max_depth=None):
    # the depth check runs before any byte is produced: on failure nothing is written
    if max_depth is not None and depth(t, v, True) > max_depth:
        raise NbtError("TOO_DEEP", "binary form deeper than %d" % max_depth)
    out = bytearray([t])
    if named: write_payload(STRING, name, out)
    write_payload(t, v, out)
    return bytes(out)


def gunzip_strict(raw):
    """One gzip member (RFC 1952). Reserved FLG bits are ignored; CRC-32 and ISIZE must match.
    Bytes after the member are ignored."""
    def bad(msg): return NbtError("CORRUPT_CONTAINER", msg)
    if len(raw) < 10 or raw[:2] != b"\x1f\x8b" or raw[2] != 8: raise bad("bad gzip header")
    flg, p = raw[3], 10
    if flg & 0x04:  # FEXTRA
        if p + 2 > len(raw): raise bad("truncated header")
        p += 2 + struct.unpack("<H", raw[p:p + 2])[0]
    for bit in (0x08, 0x10):  # FNAME, FCOMMENT: zero-terminated
        if flg & bit:
            end = raw.find(b"\x00", p)
            if end < 0: raise bad("truncated header")
            p = end + 1
    if flg & 0x02: p += 2  # FHCRC
    d = zlib.decompressobj(-15)
    try:
        data = d.decompress(raw[p:]) + d.flush()
    except zlib.error as e:
        raise bad("deflate error: %s" % e)
    if not d.eof: raise bad("deflate stream not terminated")
    trailer = d.unused_data[:8]
    if len(trailer) < 8: raise bad("trailer missing or truncated (%d of 8 bytes)" % len(trailer))
    crc, isize = struct.unpack("<II", trailer)
    if crc != zlib.crc32(data): raise bad("CRC-32 mismatch")
    if isize != len(data) & 0xFFFFFFFF: raise bad("ISIZE mismatch")
    return data


def unzlib_strict(raw):
    """One zlib stream (RFC 1950); Adler-32 must be present and match. Bytes after it are ignored."""
    d = zlib.decompressobj()
    try:
        data = d.decompress(raw) + d.flush()
    except zlib.error as e:
        raise NbtError("CORRUPT_CONTAINER", "zlib error: %s" % e)
    if not d.eof: raise NbtError("CORRUPT_CONTAINER", "stream not terminated or Adler-32 missing")
    return data


def unwrap(raw):
    if raw[:2] == b"\x1f\x8b": return "gzip", gunzip_strict(raw)
    if raw[:1] == b"\x78": return "zlib", unzlib_strict(raw)
    return "none", raw


def stored_deflate(data):
    """Raw DEFLATE stream made of stored blocks only (deterministic, no compressor involved)."""
    out, chunks = bytearray(), [data[i:i + 65535] for i in range(0, len(data), 65535)] or [b""]
    for i, c in enumerate(chunks):
        out.append(1 if i == len(chunks) - 1 else 0)
        out += struct.pack("<HH", len(c), len(c) ^ 0xFFFF) + c
    return bytes(out)


def gzip_stored(data):
    return (bytes.fromhex("1f8b08000000000000ff") + stored_deflate(data)
            + struct.pack("<II", zlib.crc32(data), len(data) & 0xFFFFFFFF))


def zlib_stored(data):
    return bytes.fromhex("7801") + stored_deflate(data) + struct.pack(">I", zlib.adler32(data))


def load(path, stats=None):
    raw = open(path, "rb").read()
    kind, data = unwrap(raw)
    r = Reader(data, stats)
    t, name, v = r.root()
    if r.p != len(data): raise ValueError("%d trailing bytes" % (len(data) - r.p))
    if write_root(t, name, v) != data: raise ValueError("re-encoding differs from input")
    load.units = r.units()
    return raw, kind, data, t, name, v


def dump(path):
    raw, kind, data, t, name, v = load(path)
    print("file          :", path.replace("\\", "/").split("/data/", 1)[-1])
    print("container     :", kind, "| compressed bytes", len(raw), "| decompressed bytes", len(data))
    print("sha256 (decompressed):", hashlib.sha256(data).hexdigest())
    print("root          : type %s (%d), name %r" % (NAMES[t], t, name))
    for k, ct, cv in v[1]:
        extra = ""
        if ct == LIST: extra = " elem=%s count=%d" % (NAMES[cv[1]], len(cv[2]))
        elif ct in (INT, BYTE, SHORT, LONG, STRING): extra = " value=%r" % (cv,)
        print("  %-12s %-8s%s" % (k, NAMES[ct], extra))
    print("first 32 bytes:", data[:32].hex(" "))


def ftable(path):
    """One line per file with every value of the spec's F table."""
    raw, kind, data, t, name, v = load(path)
    parts = []
    for k, ct, cv in v[1]:
        if ct == LIST:
            items = cv[2]
            shown = " %r" % items if cv[1] in (BYTE, SHORT, INT, LONG) and len(items) <= 8 else ""
            parts.append("%s=List<%s>(%d)%s" % (k, NAMES[cv[1]], len(items), shown))
        elif ct in (BYTE, SHORT, INT, LONG, STRING):
            parts.append("%s=%s %r" % (k, NAMES[ct], cv))
        else:
            parts.append("%s=%s" % (k, NAMES[ct]))
    print(path.replace("\\", "/").split("/structure/", 1)[-1])
    print("  sha256 compressed  :", hashlib.sha256(raw).hexdigest())
    print("  bytes              : %d compressed, %d decompressed" % (len(raw), len(data)))
    print("  sha256 decompressed:", hashlib.sha256(data).hexdigest())
    print("  container          : %s, header %s" % (kind, h(raw[:10])))
    print("  root               : %s, name %r" % (NAMES[t], name))
    print("  root keys in order :", "; ".join(parts))


def tree(path, max_items=6):
    """Print the whole decoded tree; long lists are cut after max_items elements (count always shown)."""
    raw, kind, data, t, name, v = load(path)
    def show(t, v, ind, k):
        if t == COMPOUND:
            print("%s%s: Compound(%d)" % (ind, k, len(v[1])))
            for kk, ct, cv in v[1]: show(ct, cv, ind + "  ", kk)
        elif t == LIST:
            print("%s%s: List<%s>(%d)" % (ind, k, NAMES[v[1]], len(v[2])))
            if v[1] in (COMPOUND, LIST, BYTE_ARRAY, INT_ARRAY, LONG_ARRAY):
                for i, x in enumerate(v[2][:max_items]): show(v[1], x, ind + "  ", "[%d]" % i)
            elif v[2]:
                print("%s  %r" % (ind, v[2][:max_items]))
        elif t in (BYTE_ARRAY, INT_ARRAY, LONG_ARRAY):
            print("%s%s: %s %r" % (ind, k, NAMES[t], v[1]))
        else:
            print("%s%s: %s %r" % (ind, k, NAMES[t], v))
    show(t, v, "", "root %r" % name)


def hexdump(path):
    print(unwrap(open(path, "rb").read())[1].hex())


def container_depth(t, v):
    if t == COMPOUND: return 1 + max([container_depth(ct, cv) for _, ct, cv in v[1]] + [0])
    if t == LIST: return 1 + max([container_depth(v[1], x) for x in v[2]] + [0])
    return 0


def scan(root):
    st, files, kinds, roots, versions, sizes = Stats(), 0, {}, {}, {}, []
    deepest, heaviest = (0, None), (0, None)
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if not fn.endswith(".nbt"): continue
            raw, kind, data, t, name, v = load(os.path.join(dp, fn), st)
            files += 1
            kinds[kind] = kinds.get(kind, 0) + 1
            roots[(t, name)] = roots.get((t, name), 0) + 1
            dv = [cv for k, ct, cv in v[1] if k == "DataVersion"]
            versions[dv[0] if dv else None] = versions.get(dv[0] if dv else None, 0) + 1
            sizes.append(len(data))
            d = container_depth(t, v)
            rel = os.path.relpath(os.path.join(dp, fn), root).replace("\\", "/")
            if d > deepest[0]: deepest = (d, rel)
            if load.units > heaviest[0]: heaviest = (load.units, rel)
    print("files decoded and re-encoded byte-identically:", files)
    print("containers:", kinds)
    print("roots (type,name):", {(NAMES[t], n): c for (t, n), c in roots.items()})
    print("DataVersion values:", versions)
    print("decompressed size min/max:", min(sizes), max(sizes))
    print("tag counts:", {NAMES[i]: c for i, c in enumerate(st.tags) if c})
    print("list element types:", {NAMES[k]: c for k, c in st.list_elem.items()})
    print("empty lists by element type:", {NAMES[k]: c for k, c in st.empty_lists.items()})
    print("max nested containers, root included: %d (first file: %s)" % deepest)
    print("max memory quota units: %d (file: %s)" % heaviest)
    print("compounds:", st.compounds, "| with >1 key:", st.multi_key_compounds,
          "| of which keys in ascending order:", st.sorted_compounds)
    print("non-ASCII strings:", st.non_ascii_strings, "| NaN:", st.nan, "| -0.0:", st.neg_zero)


def java_string_hash(s):
    # String hash as specified by the Java SE API (String.hashCode): s[0]*31^(n-1) + ... + s[n-1]
    # over UTF-16 units, 32-bit. The spreading and bucket reduction in bucket_order_key follow the
    # OpenJDK hash table implementation; the Java SE API itself leaves iteration order unspecified.
    be = s.encode("utf-16-be", "surrogatepass")
    hv = 0
    for i in range(0, len(be), 2):
        hv = (31 * hv + int.from_bytes(be[i:i + 2], "big")) & 0xFFFFFFFF
    return hv


def bucket_order_key(k, n):
    cap = 16
    while n > cap * 0.75: cap *= 2
    hv = java_string_hash(k)
    return (hv ^ (hv >> 16)) & (cap - 1)


def order(root):
    """Check whether compound key order is lexicographic, or non-decreasing in bucket_order_key."""
    counts = {"multi_key": 0, "lexicographic": 0, "bucket": 0}
    odd = []
    def walk(t, v, path):
        if t == COMPOUND:
            keys = [k for k, _, _ in v[1]]
            if len(keys) > 1:
                counts["multi_key"] += 1
                counts["lexicographic"] += keys == sorted(keys)
                b = [bucket_order_key(k, len(keys)) for k in keys]
                if b == sorted(b): counts["bucket"] += 1
                elif len(odd) < 10: odd.append((path, keys))
            for k, ct, cv in v[1]: walk(ct, cv, path + "/" + k)
        elif t == LIST:
            for x in v[2]: walk(v[1], x, path + "[]")
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if fn.endswith(".nbt"):
                t, name, v = Reader(gzip.decompress(open(os.path.join(dp, fn), "rb").read())).root()
                walk(t, v, "")
    print(counts)
    for p, k in odd: print("not bucket-ordered:", p, k)


def h(b): return b.hex(" ")


def vectors():
    C = lambda items: (COMPOUND, items)
    V = [
        ("V01 End alone", bytes([0])),
        ("V02 Byte -1 named 'b'", write_root(BYTE, "b", -1)),
        ("V03 Short -2 empty name", write_root(SHORT, "", -2)),
        ("V04 Int 0x01020304 empty name", write_root(INT, "", 0x01020304)),
        ("V05 Long -9223372036854775808", write_root(LONG, "", -2 ** 63)),
        ("V06 Float 1.5", write_root(FLOAT, "", 1.5)),
        ("V07 Double -0.0", write_root(DOUBLE, "", -0.0)),
        ("V08 ByteArray [1,-1]", write_root(BYTE_ARRAY, "", (BYTE_ARRAY, [1, -1]))),
        ("V09 String 'hi'", write_root(STRING, "", "hi")),
        ("V10 empty List of End", write_root(LIST, "", (LIST, END, []))),
        ("V11 List Int [7,8]", write_root(LIST, "", (LIST, INT, [7, 8]))),
        ("V12 Compound {a:1b}", write_root(COMPOUND, "", C([("a", BYTE, 1)]))),
        ("V13 IntArray [1,2]", write_root(INT_ARRAY, "", (INT_ARRAY, [1, 2]))),
        ("V14 LongArray [1]", write_root(LONG_ARRAY, "", (LONG_ARRAY, [1]))),
        ("V15 String U+0000", write_root(STRING, "", "\x00")),
        ("V16 String U+00E9", write_root(STRING, "", "\u00e9")),
        ("V17 String U+20AC", write_root(STRING, "", "\u20ac")),
        ("V18 String U+1F600", write_root(STRING, "", "\U0001F600")),
        ("V19 empty Compound, named root, empty name", write_root(COMPOUND, "", C([]))),
        ("V20 empty Compound, unnamed network root", write_root(COMPOUND, "", C([]), named=False)),
        ("V21 Compound {z:1b,a:2b} order z then a", write_root(COMPOUND, "", C([("z", BYTE, 1), ("a", BYTE, 2)]))),
        ("V22 Float canonical NaN 0x7FC00000", bytes([5, 0, 0]) + bytes.fromhex("7fc00000")),
        ("V23 Double +inf", write_root(DOUBLE, "", math.inf)),
        ("V24 empty List of Byte", write_root(LIST, "", (LIST, BYTE, []))),
        ("V25 Float NaN payload 0x7FC00001", bytes([5, 0, 0]) + bytes.fromhex("7fc00001")),
        ("V26 Double NaN payload 0x7FF0000000000001", bytes([6, 0, 0]) + bytes.fromhex("7ff0000000000001")),
    ]
    for label, b in V:
        if b == bytes([0]):
            print("%-45s %s" % (label, h(b))); continue
        named = "unnamed" not in label
        r = Reader(b); t, name, v = r.root(named=named); assert r.p == len(b), label
        again = write_root(t, name, v, named=named)
        print("%-45s %s%s" % (label, h(b), "" if again == b else "  -> rewritten as " + h(again)))
    print()
    bad = [
        ("E01 truncated Int (3 of 4 bytes)", bytes.fromhex("03 00 00 01 02 03")),
        ("E02 unknown type 13", bytes.fromhex("0d 00 00")),
        ("E03 List of End, length 1", bytes.fromhex("09 00 00 00 00 00 00 01")),
        ("E04 ByteArray length -1", bytes.fromhex("07 00 00 ff ff ff ff")),
        ("E05 List of Byte, length -1", bytes.fromhex("09 00 00 01 ff ff ff ff")),
        ("E06 String with raw 0x00 byte", bytes.fromhex("08 00 00 00 01 00")),
        ("E07 String with 4-byte UTF-8 F0 9F 98 80", bytes.fromhex("08 00 00 00 04 f0 9f 98 80")),
        ("E08 Compound without final End", bytes.fromhex("0a 00 00 01 00 01 61 01")),
    ]
    for label, b in bad:
        try:
            r = Reader(b); v = r.root(); res = "accepted: %r" % (v[2],)
        except Exception as e:
            res = "REJECTED: %s" % e
        print("%-45s %-40s %s" % (label, h(b), res))
    for n in (512, 513):
        # n nested lists: root header, (n-1) times "List of List, length 1", innermost empty End list
        deep = bytes([LIST, 0, 0]) + (bytes([LIST]) + struct.pack(">i", 1)) * (n - 1) + bytes([END]) + struct.pack(">i", 0)
        try:
            Reader(deep, max_depth=512).root(); res = "accepted"
        except Exception as e:
            res = "rejected (%s)" % e
        print("E09 %d nested lists (%d bytes, sha256 %s), reader limit 512: %s"
              % (n, len(deep), hashlib.sha256(deep).hexdigest(), res))
    print()
    writer_depth_vectors()
    print()
    boolean_vectors()


def nested_tree(levels, innermost):
    """innermost is a List value placed at level `levels`; levels-1 homogeneous lists wrap it."""
    v = innermost
    for _ in range(levels - 1): v = (LIST, LIST, [v])
    return v


def writer_depth_vectors():
    het = (LIST, None, [(BYTE, 1), (INT, 2)])
    cases = [
        ("W1 512 nested lists, innermost empty", nested_tree(512, (LIST, END, []))),
        ("W2 513 nested lists, innermost empty", nested_tree(513, (LIST, END, []))),
        ("W3 511 lists + heterogeneous [1b,2] at level 512", nested_tree(512, het)),
        ("W4 510 lists + heterogeneous [1b,2] at level 511", nested_tree(511, het)),
    ]
    for label, v in cases:
        mem, binary = depth(LIST, v, False), depth(LIST, v, True)
        try:
            b = write_root(LIST, "", v, max_depth=MAX_DEPTH)
            Reader(b).root()  # what the writer accepts, the reader must accept
            res = "written, %d bytes, sha256 %s" % (len(b), hashlib.sha256(b).hexdigest())
        except NbtError as e:
            res = "refused, nothing written (%s)" % e
        print("%s: memory depth %d, binary depth %d: %s" % (label, mem, binary, res))


def to_byte_jls(t, v):
    """Java language narrowing of a numeric value to byte (used by the alternative reading)."""
    if t in (FLOAT, DOUBLE):
        if isinstance(v, NaNBits): return 0
        i = max(-2 ** 31, min(2 ** 31 - 1, int(v)))
    else:
        i = v
    return ((i + 128) & 0xFF) - 128


def boolean_vectors():
    cases = [("B1 Byte 2", BYTE, 2), ("B2 Int 256", INT, 256), ("B3 Double 0.5", DOUBLE, 0.5),
             ("B4 Float NaN 0x7FC00000", FLOAT, NaNBits(bytes.fromhex("7fc00000"))),
             ("B5 Double 0.0", DOUBLE, 0.0), ("B6 Double -0.0", DOUBLE, -0.0), ("B7 Byte 0", BYTE, 0),
             ("B8 Short -256", SHORT, -256), ("B9 String \"true\"", STRING, "true"), ("B10 empty List", LIST, (LIST, END, []))]
    for label, t, v in cases:
        numeric = t in (BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)
        decided = numeric and (isinstance(v, NaNBits) or v != 0)
        via_byte = numeric and to_byte_jls(t, v) != 0
        print("%-26s %-34s decision: %-5s | via byte conversion: %s"
              % (label, h(write_root(t, "", v)), decided, via_byte))


def nested_lists(n):
    return bytes([LIST]) + (bytes([LIST]) + struct.pack(">i", 1)) * (n - 1) + bytes([END]) + struct.pack(">i", 0)


def entry(t, key, payload_bytes):
    return bytes([t]) + write_root(STRING, "", key, named=False)[1:] + payload_bytes


def probes(empty_path, out_dir):
    """Build probe structure files by patching the decompressed bytes of structure/empty.nbt."""
    base = gzip.decompress(open(empty_path, "rb").read())
    assert hashlib.sha256(base).hexdigest().startswith("ea73f15e")
    ent = base.index(b"entities") + len("entities")  # offset of the entities list element type
    tail = len(base) - 1  # offset of the root compound End byte
    pal_end = base.index(b"minecraft:air") + len(b"minecraft:air")  # End byte of the palette entry
    ins = lambda extra: base[:tail] + extra + base[tail:]
    s = lambda raw: struct.pack(">H", len(raw)) + raw
    cases = {
        "p00_base": base,
        "p01_root_name_x": base[:1] + s(b"x") + base[3:],
        "p02_entities_end_len_minus1": base[:ent] + bytes.fromhex("00ffffffff") + base[ent + 5:],
        "p03_entities_byte_len_minus1": base[:ent] + bytes.fromhex("01ffffffff") + base[ent + 5:],
        "p04_entities_end_len_1": base[:ent] + bytes.fromhex("0000000001") + base[ent + 5:],
        "p05_entities_byte_len_0": base[:ent] + bytes.fromhex("0100000000") + base[ent + 5:],
        "p06_trailing_byte": base + b"\x00",
        "p07_truncated": base[:-1],
        "p08_extra_unknown_type_13": ins(bytes([13]) + s(b"q")),
        "p09_extra_string_raw_nul": ins(bytes([STRING]) + s(b"q") + s(b"\x00")),
        "p10_extra_string_utf8_4byte": ins(bytes([STRING]) + s(b"q") + s(bytes.fromhex("f09f9880"))),
        "p11_extra_string_mutf8_pair": ins(bytes([STRING]) + s(b"q") + s(bytes.fromhex("eda0bdedb880"))),
        "p12_extra_string_lone_surrogate": ins(bytes([STRING]) + s(b"q") + s(bytes.fromhex("eda0bd"))),
        "p13_extra_bytearray_len_minus1": ins(bytes([BYTE_ARRAY]) + s(b"q") + bytes.fromhex("ffffffff")),
        # second "id" key inside the single palette entry, just before that entry's End byte
        "p14_duplicate_palette_id_stone": base[:pal_end] + bytes([STRING]) + s(b"id") + s(b"minecraft:stone") + base[pal_end:],
        "p15_extra_deep_510": ins(bytes([LIST]) + s(b"q") + nested_lists(510)[1:]),
        "p16_extra_deep_511": ins(bytes([LIST]) + s(b"q") + nested_lists(511)[1:]),
        "p17_extra_deep_512": ins(bytes([LIST]) + s(b"q") + nested_lists(512)[1:]),
        "p18_extra_deep_513": ins(bytes([LIST]) + s(b"q") + nested_lists(513)[1:]),
        "p19_extra_bytearray_64MiB": ins(bytes([BYTE_ARRAY]) + s(b"q") + struct.pack(">i", 64 << 20) + bytes(64 << 20)),
        "p20_extra_list_20M_bytes": ins(bytes([LIST]) + s(b"q") + bytes([BYTE]) + struct.pack(">i", 20000000) + bytes(20000000)),
    }
    os.makedirs(out_dir, exist_ok=True)
    for name, data in cases.items():
        with open(os.path.join(out_dir, name + ".nbt"), "wb") as f:
            f.write(gzip.compress(data, mtime=0))
        print("%-34s %6d  %s" % (name, len(data), hashlib.sha256(data).hexdigest()))


def quota_case(label, data, quota):
    r = Reader(data, quota=quota)
    try:
        r.root()
        res = "accepted, %d units" % r.units()
    except NbtError as e:
        res = "refused %s" % e
    try:
        total = "%d units" % units_of(data)
    except EOFError:
        total = "n/a (input truncated)"
    print("%s: %d bytes, total %s, quota %d: %s" % (label, len(data), total, quota, res))


def units_of(data):
    r = Reader(data); r.root()
    return r.units()


def compound_with(key, t, payload):
    return bytes([COMPOUND, 0, 0, t]) + struct.pack(">H", len(key)) + key + payload + bytes([END])


def quota(p19_path=None):
    if p19_path:
        data = unwrap(open(p19_path, "rb").read())[1]
        quota_case("Q0 probe file " + os.path.basename(p19_path), data, DEFAULT_QUOTA)
    empty = bytes.fromhex(
        "0a000009000473697a650300000003000000010000000100000001090008656e746974696573000000000009"
        "0006626c6f636b730a00000001090003706f7303000000030000000000000000000000000300057374617465"
        "000000000009000770616c657474650a000000010800026964000d6d696e6563726166743a61697200030"
        "00b4461746156657273696f6e0000139f00")
    assert hashlib.sha256(empty).hexdigest().startswith("ea73f15e")
    r = Reader(empty); r.root()
    print("Q1 F1 empty.nbt: %d bytes + %d tags x %d = %d units" % (r.p, r.tags, TAG_WEIGHT, r.units()))
    ba = lambda n: compound_with(b"a", BYTE_ARRAY, struct.pack(">i", n) + bytes(n))
    il = lambda n: compound_with(b"a", LIST, bytes([INT]) + struct.pack(">i", n) + bytes(4 * n))
    quota_case("Q2 {a:[B; 964 zeros]}", ba(964), 1024)
    quota_case("Q3 {a:[B; 965 zeros]}", ba(965), 1024)
    quota_case("Q4 {a:[34 Int zeros]}", il(34), 1024)
    quota_case("Q5 {a:[35 Int zeros]}", il(35), 1024)
    n = DEFAULT_QUOTA - 60
    quota_case("Q6 {a:[B; %d zeros]}" % n, ba(n), DEFAULT_QUOTA)
    quota_case("Q7 {a:[B; %d zeros]}" % (n + 1), ba(n + 1), DEFAULT_QUOTA)
    bomb = compound_with(b"b", LIST, bytes([BYTE]) + struct.pack(">i", 20000000) + bytes(20000000))
    gz = gzip.compress(bomb, compresslevel=9, mtime=0)
    quota_case("Q8 {b:[20000000 Byte zeros]} (gzip level 9: %d bytes)" % len(gz), bomb, DEFAULT_QUOTA)
    # declared length far beyond the data: the pre-check refuses before reading elements
    lie = compound_with(b"b", LIST, bytes([BYTE]) + struct.pack(">i", 2 ** 31 - 1))[:-1]
    quota_case("Q9 truncated {b:List<Byte> declaring 2147483647 elements}", lie, DEFAULT_QUOTA)


def containers(empty_path, out_dir):
    raw = open(empty_path, "rb").read()
    data = gzip.decompress(raw)
    inv = lambda b: bytes(x ^ 0xFF for x in b)
    gs, zs = gzip_stored(data), zlib_stored(data)
    dv = data.index(b"DataVersion") + len(b"DataVersion") + 3  # low byte of the DataVersion Int
    gs_mod = bytearray(gs); gs_mod[gs.index(data) + dv] += 1  # 0x9f -> 0xa0, original CRC kept
    flg = bytearray(raw); flg[3] |= 0x20
    cases = [
        ("C01_gzip_original", raw),
        ("C02_gzip_crc_inverted", raw[:-8] + inv(raw[-8:-4]) + raw[-4:]),
        ("C03_gzip_isize_inverted", raw[:-4] + inv(raw[-4:])),
        ("C04_gzip_trailer_removed", raw[:-8]),
        ("C05_gzip_trailer_truncated", raw[:-4]),
        ("C06_gzip_reserved_flag_0x20", bytes(flg)),
        ("C07_gzip_bytes_after_member", raw + bytes(4)),
        ("C08_gzip_stored", gs),
        ("C09_gzip_stored_payload_byte_changed", bytes(gs_mod)),
        ("C10_zlib_stored", zs),
        ("C11_zlib_adler_inverted", zs[:-4] + inv(zs[-4:])),
        ("C12_zlib_adler_removed", zs[:-4]),
    ]
    os.makedirs(out_dir, exist_ok=True)
    for name, b in cases:
        with open(os.path.join(out_dir, name + ".bin"), "wb") as f: f.write(b)
        try:
            kind, d = unwrap(b)
            t, n, v = Reader(d).root()
            res = "accepted, DataVersion %d" % [cv for k, ct, cv in v[1] if k == "DataVersion"][0]
        except NbtError as e:
            res = "refused %s" % e
        lenient = ""
        if name.startswith("C09"):
            d = zlib.decompressobj(-15).decompress(b[10:])
            t, n, v = Reader(d).root()
            lenient = " (without trailer check: DataVersion %d)" % [cv for k, ct, cv in v[1] if k == "DataVersion"][0]
        print("%-38s %4d bytes sha256 %s: %s%s" % (name, len(b), hashlib.sha256(b).hexdigest(), res, lenient))


def snbt_depth(text, max_depth=MAX_DEPTH):
    """Offset of the '[' or '{' opening level max_depth+1 outside quoted strings, or None."""
    level, i, n = 0, 0, len(text)
    while i < n:
        c = text[i]
        if c in "\"'":
            i += 1
            while i < n and text[i] != c:
                i += 2 if text[i] == "\\" else 1
        elif c in "[{":
            level += 1
            if level > max_depth: return i
        elif c in "]}":
            level -= 1
        i += 1
    return None


def snbtdepth():
    cases = [
        ("T53 512 x '['", "[" * 512 + "]" * 512),
        ("T54 513 x '['", "[" * 513 + "]" * 513),
        ("T55 100000 x '['", "[" * 100000 + "]" * 100000),
        ("T56 512 x '{a:'", "{a:" * 512 + "1" + "}" * 512),
        ("T57 513 x '{a:'", "{a:" * 513 + "1" + "}" * 513),
        ("T58 512 x '[' + '[I;1]'", "[" * 512 + "[I;1]" + "]" * 512),
        ("T59 '[\"' + 600 x '[' + '\"]'", "[\"" + "[" * 600 + "\"]"),
    ]
    for label, text in cases:
        off = snbt_depth(text)
        res = "accepted" if off is None else "TOO_DEEP at offset %d" % off
        print("%-26s %7d chars sha256 %s: %s" % (label, len(text), hashlib.sha256(text.encode("ascii")).hexdigest(), res))


if __name__ == "__main__":
    if sys.argv[1] == "probes": probes(sys.argv[2], sys.argv[3]); sys.exit()
    cmd = sys.argv[1]
    if cmd == "dump":
        for p in sys.argv[2:]: dump(p); print()
    elif cmd == "scan": scan(sys.argv[2])
    elif cmd == "order": order(sys.argv[2])
    elif cmd == "ftable":
        for p in sys.argv[2:]: ftable(p)
    elif cmd == "tree": tree(sys.argv[2])
    elif cmd == "hex": hexdump(sys.argv[2])
    elif cmd == "vectors": vectors()
    elif cmd == "quota": quota(sys.argv[2] if len(sys.argv) > 2 else None)
    elif cmd == "containers": containers(sys.argv[2], sys.argv[3])
    elif cmd == "snbtdepth": snbtdepth()
```

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
python -I nbt_ref.py dump    "$REF/resources/data/minecraft/structure/empty.nbt"   # F1 à F4, un fichier par argument
python -I nbt_ref.py vectors                                                       # V01 à V24, E01 à E09
python -I nbt_ref.py probes  "$REF/resources/data/minecraft/structure/empty.nbt" probe_out   # fichiers p00 à p19 de PB1
```

Octets d'un fichier décompressé : `python -I -c "import gzip,sys;print(gzip.decompress(open(sys.argv[1],'rb').read()).hex())" FICHIER`.

Statut des valeurs attendues : les octets de V01 à V24 sont C (calculés à partir de R1 à R11) ; leur décodage en Byte_Array, Long_Array et chaînes non ASCII dépend de règles S. Les résultats attendus de E01 à E09, PB1 à PS2 et T1 à T52 sont **provisoires** (S) tant que la sonde n'a pas été exécutée.

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

Écriture attendue de V24 : `09 00 00 00 00 00 00 00` (V10). C'est le seul vecteur dont la réécriture diffère de l'entrée.

## E : entrées binaires invalides

Résultat attendu provisoire, à confirmer par PB1 (S).

| Vecteur | Cas | Octets | Attendu |
|---|---|---|---|
| E01 | Int tronqué | `03 00 00 01 02 03` | échec (R33) |
| E02 | type 13 | `0d 00 00` | échec (R33) |
| E03 | List de type End, longueur 1 | `09 00 00 00 00 00 00 01` | échec (R24) |
| E04 | Byte_Array de longueur -1 | `07 00 00 ff ff ff ff` | échec (R25) |
| E05 | List de Byte de longueur -1 | `09 00 00 01 ff ff ff ff` | échec (R25) |
| E06 | String contenant l'octet `00` | `08 00 00 00 01 00` | échec (R12) |
| E07 | String en UTF-8 standard sur 4 octets | `08 00 00 00 04 f0 9f 98 80` | échec (R12) |
| E08 | Compound sans End final | `0a 00 00 01 00 01 61 01` | échec (R33) |
| E09a | 512 List imbriquées en racine : `09 00 00` + 511 fois `09 00 00 00 01` + `00 00 00 00 00` (2563 octets, SHA-256 `c577920edeaead13ad505ca09a1859049301d88e58ecb8a53c7e10cae672d02e`) | voir cas | accepté (R31) |
| E09b | 513 List imbriquées, même construction (2568 octets, SHA-256 `c6ca4e050c4cdab8eb7f9cd9924855aba1f9de7fd2539de59a8e34b3b45c72cd`) | voir cas | échec (R31) |

## F : fichiers réels

Fichiers de `resources/data/minecraft/structure/`, décodés par `nbt_ref.py dump` (C). Tous : conteneur gzip, en-tête de R16, racine Compound de nom vide, clés racines dans l'ordre `size`, `entities`, `blocks`, `palette`, `DataVersion` (Int 5023).

| Fichier | SHA-256 du fichier compressé | Octets compressés / décompressés | SHA-256 des octets décompressés | Éléments des listes racines |
|---|---|---|---|---|
| F1 `empty.nbt` | `97acb694f31451123cad2c8a4e4af638fbf3bc9e4282da2ea9a2a8d38cfe5022` | 124 / 148 | `ea73f15eba7e75a69fc179de24b07781b1db2a5637af0cc9b50bbf029a24ea3f` | size 3 Int [1,1,1] ; entities 0 (type End) ; blocks 1 Compound ; palette 1 Compound |
| F2 `igloo/top.nbt` | `013477b94c733e01453c6f1e796e3b959a79b8f33f4a364a23b6cf0eaaf3a47f` | 914 / 6298 | `2ec42e950b2dbfd2db6cc4ab440b29046c5fbebfd2952d8a5e600c20917b959a` | size 3 Int [7,5,8] ; entities 0 (type End) ; blocks 152 ; palette 11 |
| F3 `village/plains/villagers/baby.nbt` | `6fe50097ea5a52d9f1b6b083b6427dca33c084be1f9351f39888483c5571947f` | 712 / 1134 | `1de0d5f67910dfae108852eb3bf91ece28ec115136700613893513989679d251` | size 3 Int [1,2,1] ; entities 1 Compound ; blocks 2 ; palette 2 |
| F4 `bastion/hoglin_stable/air_base.nbt` | `784dd30d618243b1f7713f33a6a49b87fc677fd80c61740c94e956b81e085aa9` | 80375 / 1078387 | `2e28eafc83c37d958453de50ad9e3b76cd78ed91c1f5509ca3827e5ed846132d` | size 3 Int [30,24,48] ; entities 0 (type End) ; blocks 29938 ; palette 3 |

F1 en entier (octets décompressés, 148 octets), vecteur de lecture et de réécriture complet :

```
0a000009000473697a650300000003000000010000000100000001090008656e7469746965730000000000090006626c6f636b730a00000001090003706f7303000000030000000000000000000000000300057374617465000000000009000770616c657474650a000000010800026964000d6d696e6563726166743a6169720003000b4461746156657273696f6e0000139f00
```

Arbre attendu de F1 : `{size: List<Int>[1,1,1], entities: List<End>[], blocks: List<Compound>[{pos: List<Int>[0,0,0], state: Int 0}], palette: List<Compound>[{id: String "minecraft:air"}], DataVersion: Int 5023}`, clés dans cet ordre.

Détails de F3 utiles aux tests de valeurs (premier élément de `entities`, sous-clé `nbt`, 32 clés) : `UUID` Int_Array `[895037032, -995736946, -1319125422, -1224629455]` ; `Health` Float 20.0 ; `Age` Int -21359 ; `Fire` Short -1 ; `Air` Short 300 ; `LastRestock` Long 0 ; `OnGround` Byte 1 ; `Motion` List<Double> `[0.0, -0.0784000015258789, 0.0]` ; `Rotation` List<Float> `[0.0, -25.82771110534668]` ; `Gossips`, `attributes`, `Inventory` : List vides de type End ; `HandItems` : 2 Compound vides ; `ArmorItems` : 4 Compound vides ; `VillagerData` : Compound de 3 clés.

## Corpus : 1511 fichiers de structures

Sortie de `scan` et `order` (C) :

- 1511 fichiers décodés sans erreur et réécrits à l'identique (R30) ; tous en gzip, racine Compound de nom vide, `DataVersion` 5023.
- Taille décompressée : de 148 à 1 078 387 octets.
- Occurrences par type : voir R1 ; aucun Byte_Array ni Long_Array.
- Types d'éléments de listes : Int 1 172 463, Compound 3569, End 2021, Double 862, Float 291, List 20. Toutes les listes vides (2021) sont de type End.
- Profondeur maximale : 8 conteneurs imbriqués, racine comprise (`lava_basin.nbt`).
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
| p02_entities_end_len_minus1 | `entities` : `00 ff ff ff ff` | 148 | `f6442a15cc52fb643ae565398436d5596261e7018fa91d1f336aee435bee8642` | refusé (R25) |
| p03_entities_byte_len_minus1 | `entities` : `01 ff ff ff ff` | 148 | `d8e8cf2856b6d735891a4a9f700c3b6dda58fff9d0d5cb983de8faaac977b818` | refusé (R25) |
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
| p19_extra_bytearray_64MiB | Byte_Array `q` de 67 108 864 zéros | 67 109 020 | `567fb12d47b6e603f4d13f89583d9b69fb641c6c0f3454acba2a43ab3580a2aa` | accepté (R32) |

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
data modify storage probe:nbt w.s1 set value "é"
data modify storage probe:nbt w.s2 set value "€"
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

Relever la valeur (ou le refus de `summon`). Candidats à départager (R38) : troncature (7, -7) ou arrondi (8, -8) ; 3000000000L donne 2147483647 (saturation), -1294967296 (troncature binaire) ou 0 ; `"5"` donne 0 selon la documentation publique.

Booléen (R36) : `summon minecraft:armor_stand 0 100 0 {Tags:["pb"],NoGravity:2b}` puis `data get entity @e[tag=pb,limit=1] NoGravity`. Attendu provisoire : `1b` (toute valeur non nulle vaut vrai, réécrite en 1).

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
| T33 | `"\x41é\N{Snowman}"` | String U+0041 U+00E9 U+2603 (octets `41 c3 a9 e2 98 83`) | à relever (échappement des non ASCII) | R45, R54 |
| T34 | `"\q"` | erreur | | R45 |
| T35 | `[]` | List vide | `[]` | R47, R56 |
| T36 | `[1,2,]` | List d'Int [1, 2] | `[1, 2]` | R49, R56 |
| T37 | `[1,"a"]` | List de Compound `[{"":1}, {"":"a"}]` | à relever | R47 |
| T38 | `[1b,2]` | à relever | à relever | R47 |
| T39 | `[B;1b,-1b]` | Byte_Array [1, -1] | `[B; 1b, -1b]` | R48, R56 |
| T40 | `[B;true]` | erreur | | R48 |
| T41 | `[I;1b,2s,3]` | Int_Array [1, 2, 3] | `[I; 1, 2, 3]` | R48 |
| T42 | `[I;1L]` | erreur | | R48 |
| T43 | `[L;1,2L]` | Long_Array [1, 2] | `[L; 1L, 2L]` | R48 |
| T44 | `[I;]` | Int_Array vide | à relever | R48 |
| T45 | `{a:1,}` | Compound {a: Int 1} | `{a: 1}` | R49, R56 |
| T46 | `{"a b":1,'k':2b}` | Compound {`a b`: Int 1, k: Byte 2} | `{"a b": 1, k: 2b}` (ordre à relever) | R42, R55 |
| T47 | `{a:1,a:2}` | Compound {a: Int 2} | `{a: 2}` | R42 |
| T48 | `bool(5)` | Byte 1 | `1b` | R50 |
| T49 | `bool("x")` | erreur | | R50 |
| T50 | `uuid("f81d4fae-7dec-11d0-a765-00a0c91e6bf6")` | Int_Array [-132296786, 2112623056, -1486552928, -920753162] | `[I; -132296786, 2112623056, -1486552928, -920753162]` | R50, R8 |
| T51 | `1.0e400` | à relever (erreur ou infini) | | R46, R53 |
| T52 | `{ a : 1 }` | Compound {a: Int 1} | `{a: 1}` | R41 |

T50 : les quatre entiers sont calculés (C) depuis les 16 octets de l'UUID, poids fort en premier ; les deux premiers coïncident avec l'exemple de la documentation publique.

## Annexe : `nbt_ref.py`

```python
"""Independent minimal NBT reader/writer used to derive spec vectors.

Usage:
  python -I nbt_ref.py dump FILE.nbt          # summary of one file
  python -I nbt_ref.py scan DIR               # statistics over all *.nbt under DIR
  python -I nbt_ref.py order DIR              # compound key order statistics over all *.nbt under DIR
  python -I nbt_ref.py vectors                # print hand-built binary vectors
  python -I nbt_ref.py probes EMPTY.nbt OUTDIR  # build patched probe structures from structure/empty.nbt
"""
import gzip, hashlib, math, os, struct, sys, zlib

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)
NAMES = ["End", "Byte", "Short", "Int", "Long", "Float", "Double", "ByteArray",
         "String", "List", "Compound", "IntArray", "LongArray"]


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
    def __init__(self, data, stats=None, max_depth=512):
        self.d, self.p, self.st, self.max_depth = data, 0, stats or Stats(), max_depth

    def take(self, n):
        if n < 0 or self.p + n > len(self.d):
            raise EOFError("truncated at offset %d (need %d)" % (self.p, n))
        r = self.d[self.p:self.p + n]; self.p += n
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
        if t == BYTE: return self.num("b")
        if t == SHORT: return self.num("h")
        if t == INT: return self.num("i")
        if t == LONG: return self.num("q")
        if t in (FLOAT, DOUBLE):
            v = self.num("f" if t == FLOAT else "d")
            if math.isnan(v): st.nan += 1
            if v == 0 and math.copysign(1, v) < 0: st.neg_zero += 1
            return v
        if t == STRING: return self.string()
        if t in (BYTE_ARRAY, INT_ARRAY, LONG_ARRAY):
            n = self.num("i")
            if n < 0: raise ValueError("negative array length %d" % n)
            fmt = {BYTE_ARRAY: "b", INT_ARRAY: "i", LONG_ARRAY: "q"}[t]
            return (t, list(struct.unpack(">%d%s" % (n, fmt), self.take(n * struct.calcsize(fmt)))))
        if depth >= self.max_depth:
            raise ValueError("nesting deeper than %d" % self.max_depth)
        if t == LIST:
            et, n = self.u8(), self.num("i")
            if et > 12: raise ValueError("unknown list element type %d" % et)
            if n < 0: raise ValueError("negative list length %d" % n)
            if et == END and n > 0: raise ValueError("non-empty list of End")
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
    if t == BYTE: out += struct.pack(">b", v)
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
        out += struct.pack(">bi", et, len(items))
        for x in items: write_payload(et, x, out)
    elif t == COMPOUND:
        for k, ct, cv in v[1]:
            out.append(ct); write_payload(STRING, k, out); write_payload(ct, cv, out)
        out.append(END)


def write_root(t, name, v, named=True):
    out = bytearray([t])
    if named: write_payload(STRING, name, out)
    write_payload(t, v, out)
    return bytes(out)


def unwrap(raw):
    if raw[:2] == b"\x1f\x8b": return "gzip", gzip.decompress(raw)
    if raw[:1] == b"\x78": return "zlib", zlib.decompress(raw)
    return "none", raw


def load(path, stats=None):
    raw = open(path, "rb").read()
    kind, data = unwrap(raw)
    r = Reader(data, stats)
    t, name, v = r.root()
    if r.p != len(data): raise ValueError("%d trailing bytes" % (len(data) - r.p))
    if write_root(t, name, v) != data: raise ValueError("re-encoding differs from input")
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


def scan(root):
    st, files, kinds, roots, versions, sizes = Stats(), 0, {}, {}, {}, []
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
    print("files decoded and re-encoded byte-identically:", files)
    print("containers:", kinds)
    print("roots (type,name):", {(NAMES[t], n): c for (t, n), c in roots.items()})
    print("DataVersion values:", versions)
    print("decompressed size min/max:", min(sizes), max(sizes))
    print("tag counts:", {NAMES[i]: c for i, c in enumerate(st.tags) if c})
    print("list element types:", {NAMES[k]: c for k, c in st.list_elem.items()})
    print("empty lists by element type:", {NAMES[k]: c for k, c in st.empty_lists.items()})
    print("max depth (root payload = 0):", st.max_depth)
    print("compounds:", st.compounds, "| with >1 key:", st.multi_key_compounds,
          "| of which keys in ascending order:", st.sorted_compounds)
    print("non-ASCII strings:", st.non_ascii_strings, "| NaN:", st.nan, "| -0.0:", st.neg_zero)


def java_string_hash(s):
    # String hash as specified by the Java SE API: s[0]*31^(n-1) + ... + s[n-1] over UTF-16 units, 32-bit
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
        ("V01 End seul", bytes([0])),
        ("V02 Byte -1 nomme 'b'", write_root(BYTE, "b", -1)),
        ("V03 Short -2 nom vide", write_root(SHORT, "", -2)),
        ("V04 Int 0x01020304 nom vide", write_root(INT, "", 0x01020304)),
        ("V05 Long -9223372036854775808", write_root(LONG, "", -2 ** 63)),
        ("V06 Float 1.5", write_root(FLOAT, "", 1.5)),
        ("V07 Double -0.0", write_root(DOUBLE, "", -0.0)),
        ("V08 ByteArray [1,-1]", write_root(BYTE_ARRAY, "", (BYTE_ARRAY, [1, -1]))),
        ("V09 String 'hi'", write_root(STRING, "", "hi")),
        ("V10 List vide de type End", write_root(LIST, "", (LIST, END, []))),
        ("V11 List Int [7,8]", write_root(LIST, "", (LIST, INT, [7, 8]))),
        ("V12 Compound {a:1b}", write_root(COMPOUND, "", C([("a", BYTE, 1)]))),
        ("V13 IntArray [1,2]", write_root(INT_ARRAY, "", (INT_ARRAY, [1, 2]))),
        ("V14 LongArray [1]", write_root(LONG_ARRAY, "", (LONG_ARRAY, [1]))),
        ("V15 String U+0000", write_root(STRING, "", "\x00")),
        ("V16 String 'e' accent U+00E9", write_root(STRING, "", "é")),
        ("V17 String U+20AC", write_root(STRING, "", "€")),
        ("V18 String U+1F600", write_root(STRING, "", "\U0001F600")),
        ("V19 Compound vide racine nommee vide", write_root(COMPOUND, "", C([]))),
        ("V20 Compound vide racine sans nom (reseau)", write_root(COMPOUND, "", C([]), named=False)),
        ("V21 Compound {z:1b,a:2b} ordre z puis a", write_root(COMPOUND, "", C([("z", BYTE, 1), ("a", BYTE, 2)]))),
        ("V22 Float NaN canonique 0x7FC00000", bytes([5, 0, 0]) + bytes.fromhex("7fc00000")),
        ("V23 Double +inf", write_root(DOUBLE, "", math.inf)),
        ("V24 List vide de type Byte", write_root(LIST, "", (LIST, BYTE, []))),
    ]
    for label, b in V:
        print("%-45s %s" % (label, h(b)))
        if b != bytes([0]):
            r = Reader(b); r.root(named="sans nom" not in label); assert r.p == len(b), label
    print()
    bad = [
        ("E01 tronque (Int sans 4e octet)", bytes.fromhex("03 00 00 01 02 03")),
        ("E02 type inconnu 13", bytes.fromhex("0d 00 00")),
        ("E03 List End longueur 1", bytes.fromhex("09 00 00 00 00 00 00 01")),
        ("E04 ByteArray longueur -1", bytes.fromhex("07 00 00 ff ff ff ff")),
        ("E05 List Byte longueur -1", bytes.fromhex("09 00 00 01 ff ff ff ff")),
        ("E06 String octet 0x00 brut", bytes.fromhex("08 00 00 00 01 00")),
        ("E07 String UTF-8 4 octets F0 9F 98 80", bytes.fromhex("08 00 00 00 04 f0 9f 98 80")),
        ("E08 Compound sans End final", bytes.fromhex("0a 00 00 01 00 01 61 01")),
    ]
    for label, b in bad:
        try:
            r = Reader(b); r.root(); res = "accepte"
        except Exception as e:
            res = "REJET: %s" % e
        print("%-45s %-40s %s" % (label, h(b), res))
    for n in (512, 513):
        # n nested lists: root header, (n-1) times "List of List, length 1", innermost empty End list
        deep = bytes([LIST, 0, 0]) + (bytes([LIST]) + struct.pack(">i", 1)) * (n - 1) + bytes([END]) + struct.pack(">i", 0)
        try:
            Reader(deep, max_depth=512).root(); res = "accepte"
        except Exception as e:
            res = "rejet (%s)" % e
        print("E09 %d listes imbriquees (%d octets, sha256 %s) avec la limite du lecteur de reference 512 : %s"
              % (n, len(deep), hashlib.sha256(deep).hexdigest(), res))


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
    }
    os.makedirs(out_dir, exist_ok=True)
    for name, data in cases.items():
        with open(os.path.join(out_dir, name + ".nbt"), "wb") as f:
            f.write(gzip.compress(data, mtime=0))
        print("%-34s %6d  %s" % (name, len(data), hashlib.sha256(data).hexdigest()))


if __name__ == "__main__":
    if sys.argv[1] == "probes": probes(sys.argv[2], sys.argv[3]); sys.exit()
    cmd = sys.argv[1]
    if cmd == "dump":
        for p in sys.argv[2:]: dump(p); print()
    elif cmd == "scan": scan(sys.argv[2])
    elif cmd == "order": order(sys.argv[2])
    elif cmd == "vectors": vectors()
```

# Stockage du monde : vecteurs et sondes

Statut : brouillon
Jalon : M3
Risque : B

Compagnon de `world-block-states.md` (règles notées BS-Rn), `world-chunk-format.md` (CF-Rn) et `world-anvil-region.md` (AR-Rn).

## Outil et commandes

Tous les calculs viennent de `world_ref.py` (Python 3, bibliothèque standard seule), dont le texte intégral est en annexe. Il ne contient aucun code du jeu ; le décodage LZ4 et XXH32 suit les spécifications publiques de ces formats. Le copier dans un dossier de scripts hors des données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I world_ref.py states     "$REF/generated/reports/blocks.json" "$REF/generated/reports/registries.json"  # BS1, BS2
python -I world_ref.py templates  "$REF/resources/data/minecraft/structure" "$REF/generated/reports/blocks.json" # BS3
python -I world_ref.py dims       "$REF/resources/data/minecraft"                                                # DM1
python -I world_ref.py coords                                                                                     # CO1
python -I world_ref.py pack                                                                                       # PK1 à PK5, HM1, HM2
python -I world_ref.py light                                                                                      # LI1
python -I world_ref.py lz4                                                                                        # LZ1 (et tests négatifs)
python -I world_ref.py region     OUT_DIR VARIANTE     # RG1 à RG3 ; VARIANTE : une des 19 valeurs des tableaux RG2 et RG3, zlib par défaut
python -I world_ref.py dump       FICHIER.mca          # résumé des chunks d'un fichier de région
python -I world_ref.py check      FICHIER.mca [T_MIN T_MAX]   # contrôle des règles provisoires ; bornes Unix des horodatages (AR-R9)
python -I world_ref.py debugcheck "$REF/generated/reports/blocks.json" DOSSIER_REGION   # PC2
python -I world_ref.py probe-cmds "$REF/generated/reports/blocks.json" X Y Z            # commandes de PC3
```

Les sorties sont en anglais ; les tableaux ci-dessous les reprennent. Les octets compressés (zlib, gzip) dépendent de la bibliothèque zlib locale et ne sont pas normatifs : seuls comptent les octets NBT décompressés (empreintes données) et les en-têtes de région.

## BS : états de blocs

### BS1 : comptes (`states`, C)

| Mesure | Valeur |
|---|---|
| Blocs / entrées du registre `minecraft:block` | 1286 / 1286, défaut `minecraft:air` |
| États, identifiants 0 à n-1 sans trou | 35 723, oui |
| Identifiants recalculés par BS-R5, BS-R6 et égaux au rapport | 1286 blocs sur 1286 (35 723 états) |
| Blocs dont l'ordre des clés du JSON n'est pas l'ordre des identifiants | 12 (`piston_head`, `moving_piston`, `chest`, `trapped_chest`, 8 `*copper_chest`) |
| Blocs sans / avec propriétés | 402 / 884 |
| États par défaut par bloc | exactement 1 pour les 1286 blocs |
| Blocs dont le défaut n'est pas le premier état | 746 |
| Plus gros bloc | `note_block`, 1350 états |
| Noms de propriété distincts / avec plusieurs listes de valeurs | 93 / 13 (`age`, `axis`, `distance`, `east`, `facing`, `half`, `level`, `mode`, `north`, `shape`, `south`, `type`, `west`) |
| Bits d'un identifiant global | 16 |

### BS2 : identifiants calculés (C, égaux à `blocks.json`)

| État | Base du bloc | Identifiant | Remarque |
|---|---|---|---|
| `air` | 0 | 0 | |
| `bedrock` | 88 | 88 | |
| `water[level=0]` | 89 | 89 | défaut |
| `grass_block[snowy=false]` | 8 | 9 | `true` a le rang 0 |
| `oak_log[axis=y]` | 139 | 140 | défaut |
| `note_block[instrument=harp,note=0,powered=false]` | 680 | 681 | défaut |
| `redstone_wire[east=none,north=none,power=0,south=none,west=none]` | 5567 | 6727 | `5567 + 2*432 + 2*144 + 0*9 + 2*3 + 2` |
| `chest[facing=north,type=left,waterlogged=true]` | 5543 | 5545 | l'ordre du JSON (`type` d'abord) donnerait 5551 |
| `piston_head[facing=north,short=false,type=sticky]` | 2385 | 2388 | l'ordre du JSON donnerait 2398 |
| `hopper[enabled=false,facing=east]` | 13080 | 13089 | `facing` commence par `down` |
| `water_cauldron[level=2]` | 11195 | 11196 | `level` commence à 1 |
| `acacia_fence` toutes propriétés `true` | 15747 | 15747 | |
| `firefly_bush` | 35722 | 35722 | dernier identifiant |

### BS3 : palettes des gabarits (`templates`, C)

1511 fichiers, 1651 palettes, toutes des List de Compound. Formes d'entrées : `{id}` 6472, `{id, properties}` 17 770. Entrées sans `properties` pour un bloc qui a des propriétés : 0. Entrées avec `properties` partiel : 0. Entrées égales à l'état par défaut mais écrites avec `properties` : 4076.

## DM1 : dimensions (`dims`, C)

| Type | min_y | height | Sections | Y de section | Bits de heightmap |
|---|---|---|---|---|---|
| `overworld` | -64 | 384 | 24 | -4 à 19 | 9 |
| `overworld_caves` | -64 | 384 | 24 | -4 à 19 | 9 |
| `the_nether` | 0 | 256 | 16 | 0 à 15 | 9 |
| `the_end` | 0 | 256 | 16 | 0 à 15 | 9 |

Biomes : 67 fichiers dans `worldgen/biome`, 7 bits pour un identifiant global.

## CO1 : coordonnées (`coords`, C sur CF-R3, CF-R4, CF-R5, AR-R4, AR-R5)

| Chunk (cx, cz) | Fichier | Local | Index | Octet emplacement | Octet horodatage |
|---|---|---|---|---|---|
| (0, 0) | `r.0.0.mca` | (0, 0) | 0 | 0 | 4096 |
| (31, 31) | `r.0.0.mca` | (31, 31) | 1023 | 4092 | 8188 |
| (32, 0) | `r.1.0.mca` | (0, 0) | 0 | 0 | 4096 |
| (-1, -1) | `r.-1.-1.mca` | (31, 31) | 1023 | 4092 | 8188 |
| (-32, 0) | `r.-1.0.mca` | (0, 0) | 0 | 0 | 4096 |
| (-33, 31) | `r.-2.0.mca` | (31, 31) | 1023 | 4092 | 8188 |
| (31, 32) | `r.0.1.mca` | (31, 0) | 31 | 124 | 4220 |
| (30, -3) | `r.0.-1.mca` | (30, 29) | 958 | 3832 | 7928 |
| (1500, -600) | `r.46.-19.mca` | (28, 8) | 284 | 1136 | 5232 |

| Bloc x | Chunk | Local | Région |
|---|---|---|---|
| 15 / 16 | 0 / 1 | 15 / 0 | 0 / 0 |
| -1 / -16 / -17 | -1 / -1 / -2 | 15 / 0 / 15 | -1 / -1 / -1 |
| -512 / -513 | -32 / -33 | 0 / 15 | -1 / -2 |
| 511 / 512 | 31 / 32 | 15 / 0 | 0 / 1 |

| Bloc (x, y, z) | Section | Index bloc | Index biome | Octet de lumière | Demi-octet |
|---|---|---|---|---|---|
| (0, -64, 0) | -4 | 0 | 0 | 0 | bas |
| (15, -64, 0) | -4 | 15 | 3 | 7 | haut |
| (0, -64, 1) | -4 | 16 | 0 | 8 | bas |
| (0, -63, 0) | -4 | 256 | 0 | 128 | bas |
| (-1, 319, -1) | 19 | 4095 | 63 | 2047 | haut |
| (-16, 0, 5) | 0 | 80 | 4 | 40 | bas |
| (5, 10, -3) | 0 | 2773 | 45 | 1386 | haut |

Y : -64 -> section -4 (rang 0 dans la liste) ; -49 -> -4 ; -48 -> -3 ; -1 -> -1 (rang 3) ; 0 -> 0 (rang 4) ; 319 -> 19 (rang 23).

## PK : empaquetage (`pack`, C sur CF-R7, CF-R8)

### PK1 : longs nécessaires

| Bits | Entrées par long | Bits inutilisés | Longs (4096) | Longs (64) | Longs (256) |
|---|---|---|---|---|---|
| 1 | 64 | 0 | 64 | 1 | 4 |
| 2 | 32 | 0 | 128 | 2 | 8 |
| 3 | 21 | 1 | 196 | 4 | 13 |
| 4 | 16 | 0 | 256 | 4 | 16 |
| 5 | 12 | 4 | 342 | 6 | 22 |
| 6 | 10 | 4 | 410 | 7 | 26 |
| 7 | 9 | 1 | 456 | 8 | 29 |
| 8 | 8 | 0 | 512 | 8 | 32 |
| 9 | 7 | 1 | 586 | 10 | 37 |
| 10 | 6 | 4 | 683 | 11 | 43 |
| 11 | 5 | 9 | 820 | 13 | 52 |
| 12 | 5 | 4 | 820 | 13 | 52 |
| 16 | 4 | 0 | 1024 | 16 | 64 |

Bits selon la taille de palette : blocs 1 -> 0 (pas de `data`), 2 -> 4, 16 -> 4, 17 -> 5, 32 -> 5, 33 -> 6, 256 -> 8, 257 -> 9, 4096 -> 12 ; biomes 1 -> 0, 2 -> 1, 3 -> 2, 4 -> 2, 5 -> 3, 8 -> 3, 9 -> 4, 64 -> 6.

### PK2 à PK5 : valeurs connues

| Vecteur | Entrées | Bits | Longs | Résultat |
|---|---|---|---|---|
| PK2 | 4096 blocs, valeur `i & 15` | 4 | 256 | tous les longs = `0xfedcba9876543210` (Long signé -81985529216486896) |
| PK3 | 4096 blocs, valeur `i mod 17` | 5 | 342 | long 0 `0x05a928398a418820`, long 1 `0x031483104107b9ac`, dernier `0x000000000007b9ac` ; un empaquetage continu (faux) donnerait 320 longs et long 1 `0x0731483104107b9a` |
| PK4 | 4096 blocs, 1 sur la couche y = 0, 0 ailleurs | 4 | 256 | longs 0 à 15 `0x1111111111111111`, longs 16 à 255 nuls |
| PK5a | 64 biomes, valeur `i mod 5` | 3 | 4 | `0x08d111a223444688`, `0x111a2234446888d1`, `0x22234446888d111a`, `0x0000000000000003` |
| PK5b | 64 biomes, 1 si `qy >= 2` | 1 | 1 | `0xffffffff00000000` |
| PK5c | 64 biomes, 1 si `qx < 2` | 1 | 1 | `0x3333333333333333` |

## HM : heightmaps (C sur CF-R17)

| Vecteur | Valeurs | Résultat (37 longs, 9 bits) |
|---|---|---|
| HM1 | 256 colonnes à 1 | longs 0 à 35 `0x0040201008040201`, long 36 `0x0000000008040201` |
| HM2 | colonne `i` -> `min(384, i + 128)` | long 0 `0x2190a8441a090280`, long 36 `0x0000000bfdfafb7c` |
| HM3 | 256 colonnes à 4 (monde plat, CF-R18 provisoire) | longs 0 à 35 `0x0100804020100804`, long 36 `0x0000000020100804` |

## LI1 : lumière (`light`, C sur CF-R14)

Tableau de 2048 octets nuls, puis index 0 -> 15, index 1 -> 1, index 256 -> 7, index 4095 -> 9 : octet 0 = `0x1f`, octet 1 = `0x00`, octet 128 = `0x07`, octet 2047 = `0x90` (Byte NBT signé -112).

## ST1 : références de structures (C sur CF-R25)

| Chunk (cx, cz) | Long |
|---|---|
| (0, 0) | 0 |
| (-1, -1) | -1 (`0xffffffffffffffff`) |
| (-1, 2) | 12884901887 (`0x00000002ffffffff`) |
| (3, -1) | -4294967293 (`0xffffffff00000003`) |

## RG : fichier de région synthétique (`region`, C sur AR-R4 à AR-R17)

### RG1 : `r.-1.-1.mca`, deux chunks

Deux chunks complets construits par `world_ref.py` (racine `0a 00 00`, `DataVersion` 5023, `yPos` -4, `Status` `minecraft:full`, `LastUpdate` 0, `InhabitedTime` 0, 24 sections avec palette de biomes `["minecraft:plains"]`, heightmaps `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`, `OCEAN_FLOOR`, `WORLD_SURFACE`, listes vides `block_entities`, `block_ticks`, `fluid_ticks`) :

- chunk A (-1, -1), index 1023, zlib : section -4 avec palette `[{id:"minecraft:air"}, {id:"minecraft:bedrock"}]` et `data` de PK4 (couche y = -64 en bedrock), autres sections `[{id:"minecraft:air"}]` sans `data`, heightmaps HM1. NBT : 5999 octets, SHA-256 `f9f9cdf5aa4c8084f9eef177ba5d5fe91f024241014bf2f7cedddc4947f9a6d6`.
- chunk B (-32, -32), index 0, type 3 : tout en air, heightmaps à 0. NBT : 3915 octets, SHA-256 `f4f617ec08567f3e2ea2c3e97d5b5a0ff698e3bb8406cb0b9ab3d111605dbf02`.

Fichier (variante `zlib`) : 16 384 octets (4 secteurs), SHA-256 `b7696ce4af6554e50540e51fda5d6dc631849187e29a888f4f862c5507fc5d88` avec la zlib de référence de l'outil (non normatif). Octets fixés par AR-R5 à R10 pour ce contenu (sauf la dernière ligne, qui suit l'ordre des clés de l'outil) :

| Position | Octets | Sens |
|---|---|---|
| 0 à 3 | `00 00 03 01` | index 0 : secteur 3, 1 secteur |
| 4 à 4091 | zéros | chunks absents |
| 4092 à 4095 | `00 00 02 01` | index 1023 : secteur 2, 1 secteur |
| 4096 à 4099 | `6a b1 3b 81` | horodatage index 0 = 1 790 000 001 |
| 8188 à 8191 | `6a b1 3b 80` | horodatage index 1023 = 1 790 000 000 |
| 8192 à 8196 | `00 00 01 ad 02` | chunk A : L = 429, zlib (`78 9c ...` suit) |
| 12288 à 12292 | `00 00 0f 4c 03` | chunk B : L = 3916, aucune compression |
| 12293 à 12356 | `0a 00 00 03 00 0b 44 61 74 61 56 65 72 73 69 6f 6e 00 00 13 9f 03 00 04 78 50 6f 73 ff ff ff e0 03 00 04 7a 50 6f 73 ff ff ff e0 03 00 04 79 50 6f 73 ff ff ff fc 08 00 06 53 74 61 74 75 73 00` | début du NBT de B : racine, `DataVersion` 5023, `xPos` -32, `zPos` -32, `yPos` -4, `Status` |

Les octets après chaque enregistrement jusqu'à la fin de son secteur sont nuls.

Ordre des clés : l'outil écrit la racine dans l'ordre `DataVersion`, `xPos`, `zPos`, `yPos`, `Status`, `LastUpdate`, `InhabitedTime`, `sections`, `Heightmaps`, `block_entities`, `block_ticks`, `fluid_ticks`, et chaque section dans l'ordre `Y`, `block_states`, `biomes`. Les empreintes SHA-256 ci-dessus dépendent de cet ordre, qui n'est **pas** une règle (`core-nbt.md` R28 et R30) : un lecteur accepte tout ordre, et les tests de l'écrivain comparent les arbres décodés, jamais les octets ni les empreintes. Les empreintes servent seulement à vérifier que deux exécutions de `world_ref.py` produisent les mêmes vecteurs.

### RG2 : variantes de compression du chunk A

| Variante | Octets 4092 à 4095 | En-tête du chunk A | Taille du fichier | SHA-256 du fichier (reproductibilité de l'outil ; `gzip` dépend aussi de la zlib locale) |
|---|---|---|---|---|
| `gzip` | `00 00 02 01` | `00 00 01 b9 01 1f 8b 08 ...` | 16 384 | `16adc8de54ba6366b7ba07f5f4dec4221cec0af676e43e438e8565601ef7c53c` |
| `none` | `00 00 02 02` | `00 00 17 70 03 0a 00 00` (L = 6000, 2 secteurs) ; B passe au secteur 4 (`00 00 04 01`) | 20 480 | `14b4880b0d37205296cf21c6a5de9b38fa1284e57789a1d7fcab053169b42c2e` |
| `lz4` | `00 00 02 02` | `00 00 17 9a 04 4c 5a 34 42 6c 6f 63 6b 16 ...` (L = 6042, un bloc brut de 5999 octets puis fin de flux) | 20 480 | `7715a9a5681862f8f0ed396b23864515c8219e6ba36042b03825e8b67cee14c8` |
| `external` | `00 00 02 01` | `00 00 00 01 82` puis zéros ; `c.-1.-1.mcc` = flux zlib seul (428 octets) | 16 384 | `628d1cf50f57436921067f67ad3a6bb9e37b72793c8e812f58c450feb41e9100` |

### RG3 : variantes altérées (sondes PB1, PB2, PR4, PR5)

Même fichier que RG1 (chunk A en zlib) avec une seule modification. `check` signale 9 variantes sur 14 (colonne "check") ; `unknown_block`, `bad_value`, `unknown_biome`, `string_palette` et `partial_props` sont des fichiers valides au sens de l'enveloppe et du conteneur, que seul un lecteur qui connaît les registres distingue. "Refusé" et "absent" sont définis dans `world-anvil-region.md`, partie Entrées / sorties.

| Variante | Modification | NBT du chunk A (octets, SHA-256) | check | Lecture provisoire par notre moteur |
|---|---|---|---|---|
| `string_palette` | palette de blocs de la section -4 = List de String `["minecraft:air", "minecraft:bedrock"]` | 5987, `fb537acc2817cd6c205b899561c44b18cc64a20792af67ddc740a1bb1349637d` | rien | bedrock à y = -64 (BS-R19) |
| `partial_props` | 2e entrée `{id:"minecraft:oak_log"}` sans `properties` | 5999, `e07511443902df5c6e98dbdfb8eba8a76f2315f0d815afd3196ad609f00e3f7c` | rien | `oak_log[axis=y]`, identifiant 140 (BS-R20) |
| `unknown_block` | 2e entrée `{id:"probe:unknown"}` | 5995, `98a99f57c93b1efa351f36b2790dbe5259580b5048becde32f4cdd8b865a8b61` | rien | air (BS-R21) |
| `bad_value` | 2e entrée `{id:"minecraft:oak_log",properties:{axis:"w"}}` | 6023, `c2c0ff9b72320ada5a92cb6c340c4d8bd3eaf4656b85c4dfbf95efd9aaffebc7` | rien | `oak_log[axis=y]` (BS-R21) |
| `bad_length` | `data` de la section -4 tronqué à 255 longs | 5991, `ccfece2c6c7ba5bbdbe8b4869621a7c637ba8af6d63731fa5b6414f7af030cee` | `palette 2 data 255 expected 256` | refusé (CF-R10) |
| `empty_palette` | palette de blocs de la section -4 vide (List de type End), pas de `data` | 3894, `e0cd53b0da596264d4e9bd0d8e982c8935a4806ace01ab08ab7a6fb346f3780b` | `palette is empty` | refusé (CF-R10) |
| `index_overflow` | palette de 2 entrées, index 2 sur la couche y = -64 | 5999, `ba034c0d63b322cede34e9aa4f34c49fe5c701f92fc1c91646abee726a1f4ae2` | `index beyond palette` | refusé (CF-R10) |
| `unknown_biome` | palette de biomes de la section -4 = `["probe:unknown_biome"]` | 6002, `af7f78cd7f9ebcf57f626f2f3b96349b09438c868561315b40a87e6a06f6e037` | rien | biome `minecraft:plains` (CF-R11) |
| `missing_block_states` | section -4 sans `block_states` | 3863, `035a4da0ee88fc15cda8d4531795490bbdfb48aeca4eb7ea90638d06fcfe859b` | `has no block_states` | section -4 en air (CF-R22) |
| `missing_biomes` | section -4 sans `biomes` | 5956, `4cc0f271d02c3b2a465db9dd22b7217b95c1b4c90654375b5168d424e72665ea` | `has no biomes` | biome `minecraft:plains` dans la section -4 (CF-R22) |
| `wrong_pos` | `xPos` = -2 à l'index du chunk (-1, -1) | 5999, `b156292a64a3e4c13b37ed990ee6a9dffcf358cec52a6fe93d458554c4eed0ed` | `xPos/zPos -2,-1 do not match` | absent (AR-R21) |
| `bad_offset` | emplacement de l'index 1023 = `00 00 01 01` (dans l'en-tête) | inchangé (RG1) | `invalid location (sector 1 ...)` | absent (AR-R20) |
| `offset_beyond` | emplacement de l'index 1023 = `00 00 c8 01` (secteur 200, au-delà des 4 secteurs du fichier) | inchangé (RG1) | `invalid location (sector 200 ...)` | absent (AR-R20) |
| `missing_mcc` | enregistrement externe `00 00 00 01 82` sans fichier `c.-1.-1.mcc` | inchangé (RG1) | `external chunk file c.-1.-1.mcc is missing` | refusé (AR-R18) |

## LZ1 : trame LZ4Block (`lz4`, D pour le format, C pour les octets)

Contrôle de l'implémentation XXH32 (graine 0) : `""` -> `0x02cc5d05`, `"a"` -> `0x550d7456`, `"abc"` -> `0x32d153ff`.

Données : `"minecraft"` répété 8 fois (72 octets). XXH32 graine `0x9747b28c` = `0x1f6103ad`, masqué sur 28 bits (AR-R15) = `0x0f6103ad`. Bloc LZ4 fait à la main (une séquence de 9 littéraux et une copie de 58 octets à distance 9, puis 5 littéraux) : `9f 6d 69 6e 65 63 72 61 66 74 09 00 27 50 63 72 61 66 74` (19 octets). Flux complet (61 octets) :

```
4c 5a 34 42 6c 6f 63 6b 26 13 00 00 00 48 00 00 00 ad 03 61 0f
9f 6d 69 6e 65 63 72 61 66 74 09 00 27 50 63 72 61 66 74
4c 5a 34 42 6c 6f 63 6b 16 00 00 00 00 00 00 00 00 00 00 00 00
```

Même données en bloc brut : en-tête `4c 5a 34 42 6c 6f 63 6b 16 48 00 00 00 48 00 00 00 ad 03 61 0f`, puis les 72 octets, puis la même fin de flux.

Tests négatifs (le décodeur doit lever une erreur) : distance de copie 0 (`... 74 00 00 27 ...`) ; flux sans marque de fin ; marque de fin avec une somme non nulle ; somme de contrôle fausse. Le décodeur exige aussi une méthode `0x10` ou `0x20`, des longueurs positives, une longueur décompressée au plus égale à la taille de bloc du jeton, et des longueurs égales pour un bloc brut.

## Sondes de l'oracle

Aucune n'a été exécutée (EULA non acceptée). Résultats attendus **provisoires** (S) tant que la sonde n'a pas tourné ; un écart corrige la règle citée et la fait passer à M.

Préparation commune : serveur 26.3, monde neuf, RCON actif, aucun joueur connecté. `server.properties` : `online-mode=false`, `enable-rcon=true`, `level-seed=0`, `spawn-protection=0`, plus les clés de chaque sonde. Mondes utilisés : W-FLAT (`level-type=minecraft\:flat`, `generate-structures=false`), W-DEBUG (`level-type=minecraft\:debug_all_block_states`), W-NORMAL (`level-type=minecraft\:normal`). Les trois préréglages existent dans `worldgen/world_preset/` (D) ; l'acceptation de ces valeurs par `level-type` est S. Après chaque série de commandes : `save-all flush` puis `stop`. Dossier de région attendu : `world/dimensions/minecraft/overworld/region/` (AR-R1) ; s'il n'existe pas, relever le chemin réel.

**Chargement des chunks (attente W).** Sans joueur, aucun chunk n'est garanti chargé : le registre `minecraft:ticket_type` (D) ne contient aucun ticket de spawn permanent (`dragon`, `ender_pearl`, `forced`, `player_loading`, `player_simulation`, `player_spawn`, `portal`, `spawn_search`, `unknown`), et une position non chargée fait échouer les commandes avec le message `argument.pos.unloaded` "That position is not loaded" (D, `en_us.json`). Toute sonde applique donc, après chaque `forceload add`, l'attente W avant le moindre `setblock`, `fill`, `fillbiome`, `summon` ou `execute if block` :

1. `forceload query <x> <z>` pour un bloc de chaque chunk visé : réponse attendue "Chunk at ... is marked for force loading" (D, `en_us.json`) ;
2. `execute if loaded <x> <y> <z>` pour un bloc de chaque chunk visé (condition présente dans `commands.json`, D ; sa sémantique "chunk entièrement chargé" est S), répété toutes les 100 ms jusqu'à "Test passed", 10 s au plus ;
3. si la limite est atteinte, consigner "jamais chargé" pour ce chunk (c'est une observation, attendue pour un chunk refusé) et ne pas lancer les commandes qui le visent.

Dans les blocs de commandes ci-dessous, `W(x, z)` désigne cette attente pour le chunk du bloc (x, z).

### PC1 : chunk réel d'un monde plat (CF-R4, R6 à R8, R16 à R18, R20, R22 ; BS-R19 ; AR-R4 à R10, R24, R25)

Noter `T0` = heure Unix (secondes) du poste avant le démarrage du serveur.

```
forceload add -16 -16 15 15
W(-16, -16)  W(0, -16)  W(-16, 0)  W(0, 0)
save-all flush
```

Noter `T1` = heure Unix après la réponse de `save-all flush`. Puis `world_ref.py check FICHIER T0 T1` et `dump` sur `r.-1.-1.mca`, `r.0.-1.mca`, `r.-1.0.mca`, `r.0.0.mca`. Attendus : chunk (-1, -1) à l'index 1023 de `r.-1.-1.mca` ; type de compression 2 ; aucun horodatage hors de `[T0, T1]` (AR-R9) ; `DataVersion` 5023, `xPos` -1, `zPos` -1, `yPos` -4, `Status` `minecraft:full` ; 24 sections Y -4 à 19 ; section -4 : palette de 4 entrées {air, `bedrock`, `dirt`, `grass_block[snowy=false]`} dans un ordre quelconque, 4 bits, 256 longs, décodage : index 0 à 255 bedrock, 256 à 767 dirt, 768 à 1023 grass_block, reste air (préréglage `flat.json` : 1 bedrock, 2 dirt, 1 grass_block, D) ; sections -3 à 19 : palette d'une entrée air, sans `data` ; biomes : palette `["minecraft:plains"]` sans `data` ; heightmaps `WORLD_SURFACE`, `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`, `OCEAN_FLOOR` égaux à HM3 (valeur 4) ; `SkyLight` de la section -4 : octets 0 à 511 nuls, 512 à 2047 à `0xff` ; `check` sans problème. Relever en plus : forme exacte des entrées de palette (Compound complet, propriétés élidées, List de String), présence de `isLightOn`, sections de lumière Y -5 et 20.

### PC2 : monde de débogage (BS-R5 à R7, R19 ; CF-R7)

W-DEBUG :

```
forceload add 0 0 255 255
forceload add 256 0 511 255
forceload add 0 256 255 511
forceload add 256 256 511 511
W pour chaque chunk de (0, 0) à (31, 31)
save-all flush
```

Puis `world_ref.py debugcheck "$REF/generated/reports/blocks.json" <dossier region>`. Attendus : un niveau Y de grille (Y 70 selon le wiki, S) ; 35 722 états distincts sur la grille (tous sauf `air`) ; identifiants croissants le long des rangées lues par z puis x. Si le compte est inférieur, la grille dépasse la zone chargée : étendre `forceload` (y compris en coordonnées négatives, au plus 256 chunks par commande, message `commands.forceload.toobig`) et relancer. Relever la forme des palettes (sortie `check`, ligne `block_states_palette_forms`).

### PC3 : grandes palettes (CF-R7 à R9)

W-FLAT :

```
forceload add 0 0
W(0, 0)
<les 261 lignes de : world_ref.py probe-cmds "$REF/generated/reports/blocks.json" 0 0 0>
<les 16 premières lignes de : world_ref.py probe-cmds "$REF/generated/reports/blocks.json" 0 16 0>
save-all flush
```

Les commandes posent des bûches, bois, laines, bétons, terres cuites, terres cuites émaillées, verres teintés et planches, blocs qui ne changent pas d'état selon leurs voisins, avec le mode `strict` de `setblock` (présent dans `commands.json` 26.3, D ; sa sémantique "pose sans mise à jour de voisinage" est S, et le choix de ces blocs rend la sonde valable même si `strict` se comportait comme `replace`). Chaque commande doit répondre "Changed the block at ..." (D, `en_us.json`). Attendus pour le chunk (0, 0) : section 0, palette de 262 entrées (261 états + air), 9 bits, 586 longs ; section 1, palette de 17 entrées, 5 bits, 342 longs ; état décodé à la position `k` = `k`-ième commande.

### PC4 : biomes (CF-R5, R7, R11)

W-FLAT :

```
forceload add 0 0
W(0, 0)
fillbiome 0 0 0 7 15 15 minecraft:desert
save-all flush
```

Attendus pour la section 0 du chunk (0, 0) : palette de biomes de 2 entrées (`minecraft:plains`, `minecraft:desert`), 1 bit, 1 long égal à `0x3333333333333333` si la palette est `[plains, desert]`, `0xcccccccccccccccc` si elle est `[desert, plains]` (PK5c).

### PC5 : lumière (CF-R14, R15)

W-FLAT :

```
forceload add 0 0
W(0, 0)
setblock 8 0 8 minecraft:glowstone
save-all flush
```

Attendus pour `BlockLight` de la section 0 du chunk (0, 0) : index 136 (8, 0, 8) = 15, donc octet 68 = `0xef` (index 137 = 14 en demi-octet haut) et octet 67 = `0xed` (index 134 = 13, index 135 = 14). La valeur 15 et la décroissance d'un niveau par bloc sont S (spec lumière M5) ; la sonde vise l'ordre des demi-octets.

### PC6 : statuts et champs d'un monde normal (CF-R19 à R21, R23 à R26, R28)

W-NORMAL, `generate-structures=true` :

```
forceload add 0 0
W(0, 0)
save-all flush
```

Puis `world_ref.py check` sur les quatre fichiers autour de l'origine (et, si aucun chunk n'a de `PostProcessing` ou de références, sur les fichiers d'une zone plus grande chargée de la même façon, par exemple `forceload add -256 -256 -1 -1`). Attendus :

- toutes les valeurs de `Status` dans la liste de CF-R27 ; chunks `minecraft:full` et proto-chunks à divers statuts ; pour chaque statut, clés de la racine, des sections, des heightmaps (CF-R19), des structures, nombre de listes de `PostProcessing` (24) et clés des entrées de ticks et de block entities ;
- `PostProcessing` (CF-R24) : la ligne `postprocessing as x|z<<4|y<<8` compte, pour chaque Short décodé avec le codage provisoire, le bloc désigné ; attendu : une majorité de blocs non air (fluides et blocs qui dépendent de leurs voisins, d'après le wiki), nettement plus que la ligne `postprocessing as y|z<<4|x<<8` (codage inversé) ; si c'est l'inverse, CF-R24 est fausse ;
- références (CF-R25, ST1) : la ligne `references decoded as x low` doit montrer une distance maximale de quelques chunks (provisoire : 16 au plus) et, pour les cibles situées dans le même fichier, autant de départs correspondants (`starts` du chunk ciblé avec le même identifiant de structure) que de cibles ; la ligne `x high` sert de contre-épreuve et doit en trouver moins. Relever aussi le nom exact de la clé (`References` ou `references`).

### PB1 : entrées de palette altérées (BS-R19 à R21)

Variantes `string_palette`, `partial_props`, `unknown_block`, `bad_value` de RG3. Pour chacune : copie d'un monde W-FLAT après PC1, serveur arrêté ; remplacer `r.-1.-1.mca` par le fichier de la variante ; démarrer ; RCON :

```
forceload add -16 -16
W(-16, -16)
execute if block -16 -64 -16 minecraft:bedrock
execute if block -16 -64 -16 minecraft:oak_log[axis=y]
execute if block -16 -64 -16 minecraft:air
execute if block -16 -63 -16 minecraft:dirt
```

La dernière ligne distingue notre chunk (air à y = -63) d'un chunk régénéré (dirt). Attendus provisoires : `string_palette` -> bedrock ; `partial_props` et `bad_value` -> `oak_log[axis=y]` ; `unknown_block` -> air. Puis `save-all flush` et `dump` : relever la forme réécrite par le serveur.

### PB2 : conteneurs et sections altérés (CF-R10, R11, R22)

Variantes `bad_length`, `empty_palette`, `index_overflow`, `unknown_biome`, `missing_block_states`, `missing_biomes` de RG3, même procédure que PB1, avec en plus :

```
execute if biome -16 -64 -16 minecraft:plains
```

Attendus provisoires (comportement de notre moteur) : `bad_length`, `empty_palette`, `index_overflow` -> chunk refusé, donc l'attente W expire ("jamais chargé") ; `unknown_biome` et `missing_biomes` -> bedrock à y = -64 et biome `plains` ; `missing_block_states` -> air à y = -64, pas de dirt à y = -63. Relever le comportement réel du serveur (chunk régénéré, section vidée, chunk non chargé, arrêt du serveur), son journal, puis, après `save-all flush`, ce qu'il a réécrit dans `r.-1.-1.mca`.

### PR1 : arborescence et fichiers auxiliaires (AR-R1, R2, R9, R22, R23)

W-FLAT après PC1, en plus :

```
W(0, 0)
summon minecraft:armor_stand 0 -60 0
setblock 1 -60 1 minecraft:composter
save-all flush
```

Attendus : `world/level.dat`, `world/session.lock` (octets `e2 98 83`), `world/dimensions/minecraft/overworld/{region,entities,poi,data}`, `world/players/`, aucun `world/region/` ni `DIM-1`. Fichiers de région de taille multiple de 4096. `entities/r.0.0.mca` index 0 : `DataVersion` 5023, `Position` `[0, 0]`, `Entities` avec un Compound d'id `minecraft:armor_stand`. `poi/r.0.0.mca` index 0 : `Sections` avec une clé `"-4"`, `Records` contenant `pos` `[1, -60, 1]` et un `type` de point d'intérêt (registre `minecraft:point_of_interest_type`, 21 entrées). Horodatages (AR-R9) : `check` avec les bornes `T0` (avant le démarrage du serveur de PR1) et `T1` (après `save-all flush`) sur `region/r.0.0.mca` et `entities/r.0.0.mca` (seules les lignes `timestamps` comptent pour ce dernier, qui n'a pas de `xPos`) ; attendu : l'horodatage de l'index 0 est dans `[T0, T1]` et supérieur à celui relevé dans PC1 pour le même chunk (il a été réécrit).

### PR2 : types de compression (AR-R13 à R16)

W-FLAT neuf avec `region-file-compression=lz4`, puis `forceload add 0 0`, `W(0, 0)` et `save-all flush`. Attendus : octet de type 4 ; données commençant par `4c 5a 34 42 6c 6f 63 6b` ; jeton `0x26` ou `0x16` (blocs de 65 536 octets) ; `world_ref.py dump` décode sans erreur avec la somme masquée de AR-R15. Même chose avec `region-file-compression=none` (type 3) et sans la clé (type 2).

### PR3 : relecture de nos fichiers (AR-R4 à R11, R17 ; CF-R20, R22)

Pour chaque variante de RG1 et RG2 (`zlib`, `gzip`, `none`, `lz4`, `external` ; pour `external`, copier aussi `c.-1.-1.mcc`) : même procédure que PB1, puis :

```
forceload add -16 -16
W(-16, -16)
execute if block -16 -64 -16 minecraft:bedrock
execute if block -1 -64 -1 minecraft:bedrock
execute if block -16 -63 -16 minecraft:air
forceload add -512 -512
W(-512, -512)
execute if block -512 -64 -512 minecraft:air
```

Attendus : `Test passed` pour les quatre tests (un chunk régénéré donnerait dirt à y = -63 et bedrock à (-512, -64, -512)).

### PR4 : chunk externe (AR-R11, R17, R18)

W-FLAT neuf avec `region-file-compression=none` :

```
forceload add 0 0
W(0, 0)
fill 0 0 0 15 7 15 minecraft:command_block{Command:"say <chaîne de 1000 caractères a>"}
save-all flush
```

2048 blocs de commande d'environ 1 Kio chacun dépassent 1 044 475 octets. Attendus : enregistrement de l'index 0 de `r.0.0.mca` = `00 00 00 01 83` ; fichier `c.0.0.mcc` dans le même dossier, commençant par `0a 00 00` (NBT brut, sans en-tête). Puis `fill 0 0 0 15 7 15 minecraft:air`, `save-all flush` : relever si `c.0.0.mcc` est supprimé (AR-R18). Enfin, serveur arrêté, supprimer `c.0.0.mcc` d'une copie du monde de la première étape, redémarrer, `forceload add 0 0` et `W(0, 0)` : relever le comportement (notre moteur : chunk refusé, l'attente expire). La variante `missing_mcc` de RG3 donne le même cas pour le chunk (-1, -1), procédure de PB1.

### PR5 : emplacements invalides (AR-R20, R21)

Variantes `wrong_pos`, `bad_offset` et `offset_beyond` de RG3, même procédure que PB1. Attendus provisoires (notre moteur) : chunk absent, donc régénéré : `execute if block -16 -63 -16 minecraft:dirt` réussit. Relever le comportement réel et le journal du serveur.

## Annexe : `world_ref.py`

```python
"""Reference calculations for the world storage specs (block states, chunk sections, Anvil regions).

Standalone, Python 3 standard library only. Reads extracted game data (JSON reports, dimension types,
structure templates) and builds synthetic vectors. Contains no game code.

Usage, with REF=.cache/reference/26.3:
  python -I world_ref.py states  REF/generated/reports/blocks.json REF/generated/reports/registries.json
  python -I world_ref.py dims    REF/resources/data/minecraft
  python -I world_ref.py templates REF/resources/data/minecraft/structure REF/generated/reports/blocks.json
  python -I world_ref.py pack
  python -I world_ref.py coords
  python -I world_ref.py light
  python -I world_ref.py lz4
  python -I world_ref.py region  OUT_DIR [VARIANT]
  python -I world_ref.py dump    FILE.mca
  python -I world_ref.py check   FILE.mca
  python -I world_ref.py debugcheck REF/generated/reports/blocks.json REGION_DIR
  python -I world_ref.py probe-cmds REF/generated/reports/blocks.json X Y Z
"""
import gzip
import hashlib
import itertools
import json
import os
import struct
import sys
import zlib


# ---------- block states ----------

def cmd_states(blocks_path, registries_path):
    """Recompute every global state id from the property lists alone.

    Blocks are taken in block registry order; inside a block, properties are sorted by name and their
    values keep the order listed in blocks.json; the last property varies fastest."""
    blocks = json.load(open(blocks_path, encoding='utf-8'))
    reg = json.load(open(registries_path, encoding='utf-8'))['minecraft:block']
    order = sorted(blocks, key=lambda n: reg['entries'][n]['protocol_id'])
    next_id = 0
    mismatches = []
    json_order_mismatch = []
    default_counts = {}
    no_props = 0
    max_block = (0, None)
    prop_names = {}
    default_not_first = 0
    for name in order:
        b = blocks[name]
        props = b.get('properties', {})
        keys = sorted(props)
        combos = list(itertools.product(*[props[k] for k in keys])) if keys else [()]
        expected = [(next_id + i, dict(zip(keys, c)) if keys else None) for i, c in enumerate(combos)]
        got = sorted(((s['id'], s.get('properties')) for s in b['states']), key=lambda t: t[0])
        if got != expected:
            mismatches.append(name)
        if keys:
            jkeys = list(props)
            jcombos = [dict(zip(jkeys, c)) for c in itertools.product(*[props[k] for k in jkeys])]
            if [p for _, p in got] != jcombos:
                json_order_mismatch.append(name)
        else:
            no_props += 1
        for k, vals in props.items():
            prop_names.setdefault(k, set()).add(tuple(vals))
        defaults = [s for s in b['states'] if s.get('default')]
        default_counts[len(defaults)] = default_counts.get(len(defaults), 0) + 1
        if defaults and defaults[0]['id'] != next_id:
            default_not_first += 1
        if len(combos) > max_block[0]:
            max_block = (len(combos), name)
        next_id += len(combos)
    total = sum(len(b['states']) for b in blocks.values())
    all_ids = sorted(s['id'] for b in blocks.values() for s in b['states'])
    print('blocks', len(blocks), 'registry_entries', len(reg['entries']), 'registry_default', reg.get('default'))
    print('states', total, 'ids_contiguous_0_to_n-1', all_ids == list(range(total)))
    print('recomputed_ids_match', len(order) - len(mismatches), '/', len(order), 'mismatches', mismatches[:5])
    print('blocks_whose_json_property_order_is_not_the_id_order', len(json_order_mismatch), json_order_mismatch)
    print('blocks_without_properties', no_props, 'with_properties', len(blocks) - no_props)
    print('blocks_by_number_of_defaults', default_counts, 'default_not_first_state', default_not_first)
    print('largest_block', max_block)
    print('distinct_property_names', len(prop_names))
    multi = {k: v for k, v in prop_names.items() if len(v) > 1}
    print('property_names_with_several_value_lists', len(multi), sorted(multi))
    bits = (total - 1).bit_length()
    print('global_palette_bits ceil(log2(states))', bits)
    for n in ['minecraft:air', 'minecraft:stone', 'minecraft:cave_air', 'minecraft:void_air', 'minecraft:water',
              'minecraft:lava', 'minecraft:bedrock', 'minecraft:grass_block', 'minecraft:oak_log',
              'minecraft:redstone_wire', 'minecraft:chest']:
        b = blocks[n]
        ids = [s['id'] for s in b['states']]
        d = [s for s in b['states'] if s.get('default')][0]
        print(' ', n, 'first', min(ids), 'last', max(ids), 'count', len(ids), 'default', d['id'], d.get('properties'))
    ch = blocks['minecraft:chest']
    print('  chest json property order', list(ch['properties']), 'values', ch['properties'])
    sample = {s['id']: s.get('properties') for s in ch['states']}
    base = min(sample)
    print('  chest ids base+0..3', [sample[base + i] for i in range(4)])


# ---------- dimension types ----------

def cmd_dims(data_root):
    for f in sorted(os.listdir(os.path.join(data_root, 'dimension_type'))):
        d = json.load(open(os.path.join(data_root, 'dimension_type', f), encoding='utf-8'))
        lo, h = d['min_y'], d['height']
        print(f, 'min_y', lo, 'height', h, 'max_y', lo + h - 1, 'sections', h // 16,
              'section_y', lo >> 4, 'to', (lo + h - 1) >> 4, 'heightmap_bits', h.bit_length(),
              'has_skylight', d['has_skylight'])
    n = len([f for f in os.listdir(os.path.join(data_root, 'worldgen', 'biome')) if f.endswith('.json')])
    print('biomes', n, 'global_biome_bits', (n - 1).bit_length())


# ---------- NBT (minimal) ----------

def nbt_read(b, p, t):
    if t == 1: return struct.unpack_from('>b', b, p)[0], p + 1
    if t == 2: return struct.unpack_from('>h', b, p)[0], p + 2
    if t == 3: return struct.unpack_from('>i', b, p)[0], p + 4
    if t == 4: return struct.unpack_from('>q', b, p)[0], p + 8
    if t == 5: return struct.unpack_from('>f', b, p)[0], p + 4
    if t == 6: return struct.unpack_from('>d', b, p)[0], p + 8
    if t == 8:
        n = struct.unpack_from('>H', b, p)[0]
        return b[p + 2:p + 2 + n].decode('utf-8', 'replace'), p + 2 + n
    if t in (7, 11, 12):
        n = struct.unpack_from('>i', b, p)[0]
        fmt, w = {7: ('b', 1), 11: ('i', 4), 12: ('q', 8)}[t]
        return ('A', t, list(struct.unpack_from('>' + fmt * n, b, p + 4))), p + 4 + w * n
    if t == 9:
        et = b[p]
        n = struct.unpack_from('>i', b, p + 1)[0]
        p += 5
        out = []
        for _ in range(max(n, 0)):
            v, p = nbt_read(b, p, et)
            out.append(v)
        return ('L', et, out), p
    if t == 10:
        out = {}
        while True:
            tt = b[p]
            p += 1
            if tt == 0:
                return out, p
            n = struct.unpack_from('>H', b, p)[0]
            k = b[p + 2:p + 2 + n].decode('utf-8')
            p += 2 + n
            out[k], p = nbt_read(b, p, tt)
    raise ValueError('bad tag %d at %d' % (t, p))


def nbt_root(b):
    if b[0] != 10:
        raise ValueError('root is not a compound')
    n = struct.unpack_from('>H', b, 1)[0]
    v, p = nbt_read(b, 3 + n, 10)
    return v, p


class T:
    """Typed NBT value for the writer: T(type_id, value[, element_type])."""
    def __init__(self, t, v, et=None):
        self.t, self.v, self.et = t, v, et


def _s(x):
    e = x.encode('utf-8')
    return struct.pack('>H', len(e)) + e


def nbt_payload(x):
    t, v = x.t, x.v
    if t == 1: return struct.pack('>b', v)
    if t == 2: return struct.pack('>h', v)
    if t == 3: return struct.pack('>i', v)
    if t == 4: return struct.pack('>q', v)
    if t == 8: return _s(v)
    if t == 7: return struct.pack('>i', len(v)) + bytes(b & 0xFF for b in v)
    if t == 11: return struct.pack('>i', len(v)) + b''.join(struct.pack('>i', i) for i in v)
    if t == 12: return struct.pack('>i', len(v)) + b''.join(struct.pack('>q', i) for i in v)
    if t == 9: return bytes([x.et or 0]) + struct.pack('>i', len(v)) + b''.join(nbt_payload(e) for e in v)
    if t == 10:
        return b''.join(bytes([e.t]) + _s(k) + nbt_payload(e) for k, e in v) + b'\x00'
    raise ValueError(t)


def nbt_file(root_items):
    return b'\x0a' + _s('') + nbt_payload(T(10, root_items))


# ---------- structure templates ----------

def cmd_templates(root, blocks_path):
    blocks = json.load(open(blocks_path, encoding='utf-8'))
    files = 0
    shapes = {}
    elem_types = {}
    elided = 0
    full = 0
    default_written = 0
    for dp, _, fs in os.walk(root):
        for f in fs:
            if not f.endswith('.nbt'):
                continue
            files += 1
            v, _ = nbt_root(gzip.decompress(open(os.path.join(dp, f), 'rb').read()))
            pals = [v['palette']] if 'palette' in v else v.get('palettes', ('L', 9, []))[2]
            for pal in pals:
                elem_types[pal[1]] = elem_types.get(pal[1], 0) + 1
                for e in pal[2]:
                    shape = tuple(sorted(e)) if isinstance(e, dict) else 'string'
                    shapes[shape] = shapes.get(shape, 0) + 1
                    if not isinstance(e, dict):
                        continue
                    bp = blocks[e['id']].get('properties', {})
                    if 'properties' in e:
                        if set(e['properties']) == set(bp):
                            full += 1
                        dflt = [s for s in blocks[e['id']]['states'] if s.get('default')][0].get('properties')
                        if e['properties'] == dflt:
                            default_written += 1
                    elif bp:
                        elided += 1
    print('template_files', files, 'palette_list_element_types', elem_types)
    print('entry_shapes', shapes)
    print('entries_with_all_properties', full, 'entries_without_properties_for_block_having_properties', elided)
    print('entries_equal_to_default_state_but_written_with_properties', default_written)


# ---------- packed arrays ----------

def pack(values, bits):
    per = 64 // bits
    longs = []
    for i in range(0, len(values), per):
        acc = 0
        for j, v in enumerate(values[i:i + per]):
            if v < 0 or v >= 1 << bits:
                raise ValueError('value out of range')
            acc |= v << (bits * j)
        longs.append(acc - (1 << 64) if acc >= 1 << 63 else acc)
    return longs


def unpack(longs, bits, count):
    per = 64 // bits
    mask = (1 << bits) - 1
    out = []
    for i in range(count):
        out.append(((longs[i // per] & 0xFFFFFFFFFFFFFFFF) >> (bits * (i % per))) & mask)
    return out


def block_bits(palette_len):
    return 0 if palette_len <= 1 else max(4, (palette_len - 1).bit_length())


def biome_bits(palette_len):
    return 0 if palette_len <= 1 else (palette_len - 1).bit_length()


def n_longs(count, bits):
    return 0 if bits == 0 else -(-count // (64 // bits))


def hx(v):
    return '0x%016x' % (v & 0xFFFFFFFFFFFFFFFF)


def cmd_pack():
    print('bits | per_long | unused_bits | longs_4096 | longs_64 | longs_256')
    for bits in range(1, 17):
        per = 64 // bits
        print(bits, per, 64 - per * bits, n_longs(4096, bits), n_longs(64, bits), n_longs(256, bits))
    print('block bits by palette size:', {n: block_bits(n) for n in [1, 2, 16, 17, 32, 33, 256, 257, 4096]})
    print('biome bits by palette size:', {n: biome_bits(n) for n in [1, 2, 3, 4, 5, 8, 9, 64]})
    # P1: 4 bits, value i&15 at index i
    vals = [i & 15 for i in range(4096)]
    lg = pack(vals, 4)
    print('P1 bits4 values=i&15 longs', len(lg), 'long0', hx(lg[0]), lg[0], 'all_equal', len(set(lg)) == 1)
    # P2: 5 bits, value i%17
    vals = [i % 17 for i in range(4096)]
    lg = pack(vals, 5)
    print('P2 bits5 values=i%17 longs', len(lg), 'long0', hx(lg[0]), 'long1', hx(lg[1]), 'last', hx(lg[-1]),
          'roundtrip', unpack(lg, 5, 4096) == vals)
    # P3: bottom layer index 1 (bedrock), rest 0
    vals = [1 if i < 256 else 0 for i in range(4096)]
    lg = pack(vals, 4)
    print('P3 bits4 layer0=1 longs', len(lg), 'long0..15', hx(lg[0]), 'long16', hx(lg[16]))
    # P4: biomes 3 bits, value i%5 over 64 entries
    vals = [i % 5 for i in range(64)]
    lg = pack(vals, 3)
    print('P4 biome bits3 values=i%5 longs', len(lg), [hx(x) for x in lg], 'roundtrip', unpack(lg, 3, 64) == vals)
    # P5: biomes 1 bit, value 1 where y quart >= 2
    vals = [1 if (i >> 4) >= 2 else 0 for i in range(64)]
    lg = pack(vals, 1)
    print('P5 biome bits1 upper half=1 longs', len(lg), [hx(x) for x in lg])
    # H1: heightmap 9 bits, all columns 1
    lg = pack([1] * 256, 9)
    print('H1 heightmap bits9 all=1 longs', len(lg), 'long0', hx(lg[0]), 'last', hx(lg[-1]))
    # H2: heightmap value = x + 16*z + 128 at column index z*16+x (capped below 385)
    vals = [(i % 16) + 16 * (i // 16) + 128 if (i % 16) + 16 * (i // 16) + 128 <= 384 else 384 for i in range(256)]
    lg = pack(vals, 9)
    print('H2 heightmap bits9 v=min(384,i+128) long0', hx(lg[0]), 'long36', hx(lg[36]),
          'roundtrip', unpack(lg, 9, 256) == vals)
    # Spanning counter-example: continuous packing of 5-bit values (pre-1.16 style) gives a different long1
    acc = 0
    for i, v in enumerate([i % 17 for i in range(4096)]):
        acc |= v << (5 * i)
    print('P2-continuous (wrong layout) long1', hx((acc >> 64) & 0xFFFFFFFFFFFFFFFF), 'longs', -(-4096 * 5 // 64))


# ---------- coordinates ----------

def cmd_coords():
    print('chunk(cx,cz) -> region, local, index, location_offset')
    for cx, cz in [(0, 0), (31, 31), (32, 0), (-1, -1), (-32, 0), (-33, 31), (31, 32), (1500, -600), (30, -3)]:
        rx, rz, lx, lz = cx >> 5, cz >> 5, cx & 31, cz & 31
        i = lx + 32 * lz
        print((cx, cz), 'r.%d.%d.mca' % (rx, rz), (lx, lz), i, 4 * i, 'timestamp_offset', 4096 + 4 * i)
    print('block x -> chunk, section-local')
    for x in [0, 15, 16, -1, -16, -17, -512, -513, 511, 512]:
        print(x, 'chunk', x >> 4, 'local', x & 15, 'region', x >> 9)
    print('block y -> section Y (overworld)')
    for y in [-64, -63, -49, -48, -1, 0, 15, 16, 318, 319]:
        print(y, 'section', y >> 4, 'local', y & 15, 'list_index', (y >> 4) + 4)
    print('block index and biome index')
    for x, y, z in [(0, -64, 0), (15, -64, 0), (0, -64, 1), (0, -63, 0), (-1, 319, -1), (-16, 0, 5), (5, 10, -3)]:
        bi = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15)
        qi = (((y & 15) >> 2) << 4) | (((z & 15) >> 2) << 2) | ((x & 15) >> 2)
        print((x, y, z), 'section', y >> 4, 'block_index', bi, 'biome_index', qi, 'light_byte', bi >> 1,
              'nibble', 'high' if bi & 1 else 'low')


# ---------- light ----------

def cmd_light():
    arr = bytearray(2048)

    def setl(i, v):
        arr[i >> 1] = (arr[i >> 1] & 0xF0) | v if i % 2 == 0 else (arr[i >> 1] & 0x0F) | (v << 4)
    setl(0, 15)
    setl(1, 1)
    setl(256, 7)
    setl(4095, 9)
    print('L1 bytes[0]=%02x bytes[1]=%02x bytes[128]=%02x bytes[2047]=%02x' % (arr[0], arr[1], arr[128], arr[2047]))
    print('L1 signed byte values', [(b - 256 if b > 127 else b) for b in (arr[0], arr[128], arr[2047])])


# ---------- region ----------

def section(y, palette_items, data=None, biome='minecraft:plains', with_blocks=True, with_biomes=True):
    """Section compound; keys in the order Y, block_states, biomes (an observed order, not a rule)."""
    et = 0 if not palette_items else (10 if palette_items[0].t == 10 else 8)
    bs = [('palette', T(9, palette_items, et))]
    if data is not None:
        bs.append(('data', T(12, data)))
    items = [('Y', T(1, y))]
    if with_blocks:
        items.append(('block_states', T(10, bs)))
    if with_biomes:
        items.append(('biomes', T(10, [('palette', T(9, [T(8, biome)], 8))])))
    return T(10, items)


BOTTOM_ENTRIES = {
    'bedrock': T(10, [('id', T(8, 'minecraft:bedrock'))]),
    'partial_props': T(10, [('id', T(8, 'minecraft:oak_log'))]),
    'unknown_block': T(10, [('id', T(8, 'probe:unknown'))]),
    'bad_value': T(10, [('id', T(8, 'minecraft:oak_log')), ('properties', T(10, [('axis', T(8, 'w'))]))]),
}


def chunk_nbt(cx, cz, with_bedrock, variant='bedrock'):
    """Full-status chunk, all air except, when with_bedrock, the bottom layer (y = -64) set to the
    second palette entry of section -4 (bedrock, or the altered entry named by `variant`)."""
    air = T(10, [('id', T(8, 'minecraft:air'))])
    secs = []
    for sy in range(-4, 20):
        if sy == -4 and with_bedrock:
            data = pack([(2 if variant == 'index_overflow' else 1) if i < 256 else 0 for i in range(4096)], 4)
            if variant == 'bad_length':
                data = data[:255]
            if variant == 'string_palette':
                items = [T(8, 'minecraft:air'), T(8, 'minecraft:bedrock')]
            elif variant == 'empty_palette':
                items, data = [], None
            else:
                items = [air, BOTTOM_ENTRIES.get(variant, BOTTOM_ENTRIES['bedrock'])]
            secs.append(section(sy, items, data,
                                biome='probe:unknown_biome' if variant == 'unknown_biome' else 'minecraft:plains',
                                with_blocks=variant != 'missing_block_states',
                                with_biomes=variant != 'missing_biomes'))
        else:
            secs.append(section(sy, [air]))
    hm = pack([1 if with_bedrock else 0] * 256, 9)
    heightmaps = T(10, [(k, T(12, hm)) for k in ['MOTION_BLOCKING', 'MOTION_BLOCKING_NO_LEAVES', 'OCEAN_FLOOR', 'WORLD_SURFACE']])
    return nbt_file([
        ('DataVersion', T(3, 5023)), ('xPos', T(3, cx)), ('zPos', T(3, cz)), ('yPos', T(3, -4)),
        ('Status', T(8, 'minecraft:full')), ('LastUpdate', T(4, 0)), ('InhabitedTime', T(4, 0)),
        ('sections', T(9, secs, 10)), ('Heightmaps', heightmaps),
        ('block_entities', T(9, [], 0)), ('block_ticks', T(9, [], 0)), ('fluid_ticks', T(9, [], 0)),
    ])


def encode_chunk(nbt, kind):
    """Return (compression byte, payload) for kind in zlib, gzip, none, lz4 (raw LZ4Block blocks)."""
    if kind == 'zlib':
        return 2, zlib.compress(nbt, 6)
    if kind == 'gzip':
        return 1, gzip.compress(nbt, 6, mtime=0)
    if kind == 'none':
        return 3, nbt
    if kind == 'lz4':
        out = b''
        for i in range(0, len(nbt), 1 << 16):
            part = nbt[i:i + (1 << 16)]
            out += lz4block_frame(0x10, 6, part, part)
        return 4, out + lz4block_end(6)
    raise ValueError(kind)


COMPRESSIONS = ('zlib', 'gzip', 'none', 'lz4', 'external')
NBT_VARIANTS = ('string_palette', 'partial_props', 'unknown_block', 'bad_value', 'bad_length', 'wrong_pos',
                'unknown_biome', 'missing_block_states', 'missing_biomes', 'empty_palette', 'index_overflow')


def cmd_region(out_dir, kind='zlib'):
    """Synthetic r.-1.-1.mca: chunk (-1,-1) at index 1023, chunk (-32,-32) uncompressed at index 0.

    kind: a compression of chunk (-1,-1) (COMPRESSIONS), an altered chunk (-1,-1) stored with zlib
    (NBT_VARIANTS), 'missing_mcc' (external record without its .mcc file), 'bad_offset' (location of
    chunk (-1,-1) points into the header) or 'offset_beyond' (location points past the end of the file).

    Key order of the compounds is fixed by this builder (root: DataVersion, xPos, zPos, yPos, Status,
    LastUpdate, InhabitedTime, sections, Heightmaps, block_entities, block_ticks, fluid_ticks; section:
    Y, block_states, biomes). It is not a rule: writer tests compare decoded trees, not bytes."""
    os.makedirs(out_dir, exist_ok=True)
    variant = kind if kind in NBT_VARIANTS else 'bedrock'
    a = chunk_nbt(-2 if kind == 'wrong_pos' else -1, -1, True, variant)
    b = chunk_nbt(-32, -32, False)
    if kind in ('external', 'missing_mcc'):
        ta, pa = encode_chunk(a, 'zlib')
        if kind == 'external':
            open(os.path.join(out_dir, 'c.-1.-1.mcc'), 'wb').write(pa)
        ta, pa = ta + 128, b''
    else:
        ta, pa = encode_chunk(a, kind if kind in COMPRESSIONS else 'zlib')
    pb = b
    blobs = [(1023, ta, pa, 1790000000), (0, 3, pb, 1790000001)]
    header = bytearray(8192)
    body = bytearray()
    sector = 2
    layout = []
    for idx, ctype, payload, ts in blobs:
        rec = struct.pack('>i', len(payload) + 1) + bytes([ctype]) + payload
        nsec = -(-len(rec) // 4096)
        rec += b'\x00' * (nsec * 4096 - len(rec))
        loc_sector = {'bad_offset': 1, 'offset_beyond': 200}.get(kind, sector) if idx == 1023 else sector
        struct.pack_into('>I', header, 4 * idx, (loc_sector << 8) | nsec)
        struct.pack_into('>I', header, 4096 + 4 * idx, ts)
        layout.append((idx, sector, nsec, len(payload) + 1, ctype))
        body += rec
        sector += nsec
    data = bytes(header + body)
    path = os.path.join(out_dir, 'r.-1.-1.mca')
    open(path, 'wb').write(data)
    print('file', path, 'size', len(data), 'sectors', len(data) // 4096, 'sha256', hashlib.sha256(data).hexdigest())
    print('chunkA nbt bytes', len(a), 'sha256', hashlib.sha256(a).hexdigest(), 'kind', kind, 'payload bytes', len(pa))
    print('chunkB nbt bytes', len(b), 'sha256', hashlib.sha256(b).hexdigest())
    for idx, sec, nsec, ln, ct in layout:
        print('index', idx, 'location_bytes', data[4 * idx:4 * idx + 4].hex(' '), 'timestamp_bytes',
              data[4096 + 4 * idx:4100 + 4 * idx].hex(' '), 'sector', sec, 'count', nsec, 'length', ln, 'type', ct)
        print('  chunk header+first bytes', data[sec * 4096:sec * 4096 + 16].hex(' '))
    nz = [i for i in range(1024) if data[4 * i:4 * i + 4] != b'\x00\x00\x00\x00']
    print('non-zero location entries', nz)
    print('chunkB first 64 bytes of NBT', b[:64].hex(' '))
    # external chunk header example
    ext = struct.pack('>i', 1) + bytes([2 + 128])
    print('external chunk record (zlib, stored in c.-1.-1.mcc):', ext.hex(' '), '+ zero padding to 4096')


# ---------- LZ4 (lz4-java block stream) ----------

def xxh32(data, seed):
    p1, p2, p3, p4, p5 = 2654435761, 2246822519, 3266489917, 668265263, 374761393
    m = 0xFFFFFFFF

    def rotl(x, r):
        return ((x << r) | (x >> (32 - r))) & m
    n = len(data)
    i = 0
    if n >= 16:
        v = [(seed + p1 + p2) & m, (seed + p2) & m, seed & m, (seed - p1) & m]
        while i <= n - 16:
            for k in range(4):
                lane = struct.unpack_from('<I', data, i)[0]
                v[k] = (rotl((v[k] + lane * p2) & m, 13) * p1) & m
                i += 4
        h = (rotl(v[0], 1) + rotl(v[1], 7) + rotl(v[2], 12) + rotl(v[3], 18)) & m
    else:
        h = (seed + p5) & m
    h = (h + n) & m
    while i <= n - 4:
        h = (rotl((h + struct.unpack_from('<I', data, i)[0] * p3) & m, 17) * p4) & m
        i += 4
    while i < n:
        h = (rotl((h + data[i] * p5) & m, 11) * p1) & m
        i += 1
    h ^= h >> 15
    h = (h * p2) & m
    h ^= h >> 13
    h = (h * p3) & m
    h ^= h >> 16
    return h


def lz4_block_decompress(src, out_len):
    out = bytearray()
    i = 0
    while i < len(src):
        token = src[i]
        i += 1
        lit = token >> 4
        if lit == 15:
            while True:
                b = src[i]
                i += 1
                lit += b
                if b != 255:
                    break
        if i + lit > len(src):
            raise ValueError('lz4 literals overrun the input')
        out += src[i:i + lit]
        i += lit
        if i >= len(src):
            break
        if i + 2 > len(src):
            raise ValueError('lz4 truncated match offset')
        off = src[i] | (src[i + 1] << 8)
        i += 2
        if off == 0 or off > len(out):
            raise ValueError('lz4 invalid match offset %d' % off)
        ml = token & 15
        if ml == 15:
            while True:
                b = src[i]
                i += 1
                ml += b
                if b != 255:
                    break
        ml += 4
        if len(out) + ml > out_len:
            raise ValueError('lz4 match overruns the declared length')
        for _ in range(ml):
            out.append(out[-off])
    if len(out) != out_len:
        raise ValueError('lz4 length mismatch')
    return bytes(out)


LZ4_MAGIC = b'LZ4Block'
LZ4_SEED = 0x9747B28C


def lz4block_decode(data):
    """Decode a whole lz4-java block stream; the stream must end with the end mark (zero lengths and checksum)."""
    out = bytearray()
    p = 0
    while True:
        if p + 21 > len(data):
            raise ValueError('LZ4Block stream truncated before its end mark at %d' % p)
        if data[p:p + 8] != LZ4_MAGIC:
            raise ValueError('bad LZ4Block magic at %d' % p)
        token = data[p + 8]
        method, level = token & 0xF0, token & 0x0F
        clen, dlen, chk = struct.unpack_from('<iii', data, p + 9)
        p += 21
        if method not in (0x10, 0x20):
            raise ValueError('bad LZ4Block method 0x%02x' % method)
        if clen < 0 or dlen < 0 or dlen > 1 << (level + 10) or (method == 0x10 and clen != dlen):
            raise ValueError('bad LZ4Block lengths %d/%d' % (clen, dlen))
        if dlen == 0:
            if clen != 0 or chk != 0 or method != 0x10:
                raise ValueError('bad LZ4Block end mark')
            return bytes(out), p
        if p + clen > len(data):
            raise ValueError('LZ4Block payload truncated')
        payload = data[p:p + clen]
        p += clen
        blk = payload if method == 0x10 else lz4_block_decompress(payload, dlen)
        if (xxh32(blk, LZ4_SEED) & 0x0FFFFFFF) != (chk & 0x0FFFFFFF):
            raise ValueError('bad LZ4Block checksum')
        out += blk


def lz4block_frame(method, level, payload, plain):
    chk = xxh32(plain, LZ4_SEED) & 0x0FFFFFFF
    return LZ4_MAGIC + bytes([method | level]) + struct.pack('<iii', len(payload), len(plain), chk) + payload


def lz4block_end(level):
    return LZ4_MAGIC + bytes([0x10 | level]) + struct.pack('<iii', 0, 0, 0)


def cmd_lz4():
    for d, s, want in [(b'', 0, 0x02CC5D05), (b'a', 0, 0x550D7456), (b'abc', 0, 0x32D153FF)]:
        print('xxh32 self-test', d, hex(xxh32(d, s)), 'ok' if xxh32(d, s) == want else 'FAIL')
    plain = b'minecraft' * 8
    comp = bytes([0x9F]) + b'minecraft' + bytes([0x09, 0x00, 39]) + bytes([0x50]) + b'craft'
    assert lz4_block_decompress(comp, len(plain)) == plain
    print('Z1 plain', len(plain), 'bytes, xxh32(seed 0x9747b28c)', hex(xxh32(plain, LZ4_SEED)),
          'masked', hex(xxh32(plain, LZ4_SEED) & 0x0FFFFFFF))
    stream = lz4block_frame(0x20, 6, comp, plain) + lz4block_end(6)
    print('Z1 LZ4 stream', len(stream), 'bytes:', stream.hex(' '))
    raw = lz4block_frame(0x10, 6, plain, plain) + lz4block_end(6)
    print('Z2 RAW stream', len(raw), 'bytes, header:', raw[:21].hex(' '))
    assert lz4block_decode(stream)[0] == plain and lz4block_decode(raw)[0] == plain
    print('decode round trip ok')
    bad = {
        'match offset 0': lz4block_frame(0x20, 6, comp[:10] + b'\x00\x00' + comp[12:], plain) + lz4block_end(6),
        'no end mark': lz4block_frame(0x20, 6, comp, plain),
        'end mark with checksum': lz4block_frame(0x20, 6, comp, plain) + lz4block_end(6)[:17] + b'\x01\x00\x00\x00',
        'wrong checksum': stream[:17] + b'\x00\x00\x00\x00' + stream[21:],
    }
    for label, s in bad.items():
        try:
            lz4block_decode(s)
            print('negative test', label, 'NOT rejected')
        except ValueError as e:
            print('negative test', label, 'rejected:', e)


# ---------- region reading and oracle checks ----------

def read_chunks(path):
    data = open(path, 'rb').read()
    base = os.path.basename(path)
    out = []
    for i in range(1024):
        loc = struct.unpack_from('>I', data, 4 * i)[0]
        if not loc:
            continue
        off, cnt = loc >> 8, loc & 0xFF
        ts = struct.unpack_from('>I', data, 4096 + 4 * i)[0]
        ln, ct = None, None
        try:
            if off < 2 or cnt == 0 or (off + cnt) * 4096 > len(data):
                raise ValueError('invalid location (sector %d, count %d, file %d bytes)' % (off, cnt, len(data)))
            ln = struct.unpack_from('>i', data, off * 4096)[0]
            ct = data[off * 4096 + 4]
            if ln <= 0 or ln + 4 > cnt * 4096:
                raise ValueError('invalid length %d for %d sectors' % (ln, cnt))
            raw = data[off * 4096 + 5: off * 4096 + 4 + ln]
            kind = ct & 0x7F
            if ct >= 128:
                rx, rz = [int(t) for t in base.split('.')[1:3]]
                cx, cz = rx * 32 + (i & 31), rz * 32 + (i >> 5)
                mcc = os.path.join(os.path.dirname(path), 'c.%d.%d.mcc' % (cx, cz))
                if not os.path.exists(mcc):
                    raise ValueError('external chunk file %s is missing' % os.path.basename(mcc))
                raw = open(mcc, 'rb').read()
            if kind == 1:
                nbt = gzip.decompress(raw)
            elif kind == 2:
                nbt = zlib.decompress(raw)
            elif kind == 3:
                nbt = raw
            elif kind == 4:
                nbt = lz4block_decode(raw)[0]
            else:
                raise ValueError('unknown compression %d' % ct)
            out.append((i, off, cnt, ts, ln, ct, nbt_root(nbt)[0]))
        except (ValueError, OSError, zlib.error, struct.error, IndexError) as e:
            out.append((i, off, cnt, ts, ln, ct, 'error: %s' % e))
    return data, out


def cmd_dump(path):
    data, chunks = read_chunks(path)
    print('size', len(data), 'multiple_of_4096', len(data) % 4096 == 0)
    for i, off, cnt, ts, ln, ct, v in chunks:
        if isinstance(v, str):
            print('index', i, 'sector', off, 'count', cnt, 'type', ct, v)
            continue
        print('index', i, 'local', (i & 31, i >> 5), 'sector', off, 'count', cnt, 'timestamp', ts, 'length', ln,
              'type', ct, 'xPos', v.get('xPos'), 'zPos', v.get('zPos'), 'keys', list(v))


def palette_name(e):
    return e if isinstance(e, str) else e.get('id', e.get('Name'))


def block_name_at(v, sy, index):
    """Palette name of the block at a section-local index, or None if the section cannot be decoded."""
    for s in v.get('sections', ('L', 10, []))[2]:
        if s.get('Y') != sy or 'block_states' not in s:
            continue
        pal = s['block_states']['palette'][2]
        n = len(pal)
        if n == 1:
            return palette_name(pal[0])
        bits = max(4, (n - 1).bit_length())
        if n == 0 or 'data' not in s['block_states'] or len(s['block_states']['data'][2]) != n_longs(4096, bits):
            return None
        p = unpack(s['block_states']['data'][2], bits, 4096)[index]
        return palette_name(pal[p]) if p < n else None
    return None


def postprocessing_report(chunks):
    """Probe PC6 for CF-R24: decode every PostProcessing short with the provisional packing and with the
    swapped one (y in the low bits), and count which blocks they designate."""
    tally = {'x|z<<4|y<<8': {}, 'y|z<<4|x<<8': {}}
    for _, _, _, _, _, _, v in chunks:
        if not isinstance(v, dict) or 'PostProcessing' not in v:
            continue
        for k, lst in enumerate(v['PostProcessing'][2]):
            sy = v.get('yPos', -4) + k
            for sh in lst[2]:
                sh &= 0xFFFF
                lx, lz, ly = sh & 15, (sh >> 4) & 15, (sh >> 8) & 15
                for label, idx in (('x|z<<4|y<<8', (ly << 8) | (lz << 4) | lx),
                                   ('y|z<<4|x<<8', (lx << 8) | (lz << 4) | ly)):
                    name = block_name_at(v, sy, idx)
                    tally[label][name] = tally[label].get(name, 0) + 1
    for label, t in tally.items():
        print('postprocessing as', label, sorted(t.items(), key=lambda kv: -kv[1])[:8])


def references_report(path, chunks):
    """Probe PC6 for CF-R25: decode reference longs (x low 32 bits) and look for the matching start."""
    starts = {}
    refs = []
    for _, _, _, _, _, _, v in chunks:
        if not isinstance(v, dict):
            continue
        st = v.get('structures', {})
        for sid, start in st.get('starts', {}).items():
            if isinstance(start, dict) and start.get('id') != 'INVALID':
                starts[(v['xPos'], v['zPos'], sid)] = True
        for key in ('References', 'references'):
            for sid, arr in st.get(key, {}).items():
                for lv in (arr[2] if isinstance(arr, tuple) else []):
                    refs.append((v['xPos'], v['zPos'], sid, lv))
    rx, rz = [int(t) for t in os.path.basename(path).split('.')[1:3]]
    for label, dec in (('x low', lambda l: (((l & 0xFFFFFFFF) ^ 0x80000000) - 0x80000000, l >> 32)),
                       ('x high', lambda l: (l >> 32, ((l & 0xFFFFFFFF) ^ 0x80000000) - 0x80000000))):
        found = inside = 0
        dist = 0
        for x, z, sid, lv in refs:
            cx, cz = dec(lv)
            dist = max(dist, abs(cx - x), abs(cz - z))
            if cx >> 5 == rx and cz >> 5 == rz:
                inside += 1
                found += (cx, cz, sid) in starts
        print('references decoded as', label, 'count', len(refs), 'max chunk distance', dist,
              'target in this file', inside, 'with a matching start', found)


def cmd_check(path, t_min=None, t_max=None):
    """Compare a region file written by the oracle with the provisional rules (probes PC1 to PC6, PR1).

    Optional t_min and t_max (Unix seconds) bound the expected chunk timestamps (AR-R9)."""
    data, chunks = read_chunks(path)
    problems = []
    keysets = {}
    inventory = {}
    stamps = [c[3] for c in chunks]
    if stamps:
        print('timestamps min', min(stamps), 'max', max(stamps))
        if t_min is not None:
            out = [s for s in stamps if not int(t_min) <= s <= int(t_max)]
            print('timestamps outside [%s, %s]:' % (t_min, t_max), len(out))
    for i, off, cnt, ts, ln, ct, v in chunks:
        if isinstance(v, str):
            problems.append((i, v))
            continue
        keysets.setdefault(tuple(v), []).append(i)
        rx, rz = [int(t) for t in os.path.basename(path).split('.')[1:3]]
        if (v.get('xPos'), v.get('zPos')) != (rx * 32 + (i & 31), rz * 32 + (i >> 5)):
            problems.append((i, 'xPos/zPos %s,%s do not match the index' % (v.get('xPos'), v.get('zPos'))))
        st = v.get('Status', v.get('status'))
        inv = inventory.setdefault(st, {'chunks': 0, 'heightmaps': set(), 'section_keys': set(),
                                        'structures': set(), 'postprocessing_lists': set(), 'compression': set()})
        inv['chunks'] += 1
        inv['compression'].add(ct)
        inv['heightmaps'].add(tuple(sorted(v.get('Heightmaps', {}))))
        inv['structures'].add(tuple(sorted(v.get('structures', {}))))
        if 'PostProcessing' in v:
            inv['postprocessing_lists'].add(len(v['PostProcessing'][2]))
        for s in v.get('sections', ('L', 10, []))[2]:
            inv['section_keys'].add(tuple(sorted(s)))
            for key in ('block_states', 'biomes'):
                if key in s:
                    et = s[key]['palette'][1]
                    shapes = {('string',) if not isinstance(e, dict) else tuple(sorted(e)) for e in s[key]['palette'][2]}
                    inv.setdefault(key + '_palette_forms', set()).update((et,) + sh for sh in shapes)
        for s in v.get('sections', ('L', 10, []))[2]:
            for key, minbits, count in (('block_states', 4, 4096), ('biomes', 0, 64)):
                c = s.get(key)
                if c is None:
                    # Overworld height assumed: 24 sections from yPos (CF-R2).
                    if v.get('yPos', -4) <= s.get('Y', 0) < v.get('yPos', -4) + 24:
                        problems.append((i, 'section %s has no %s' % (s.get('Y'), key)))
                    continue
                n = len(c['palette'][2])
                if n == 0:
                    problems.append((i, 'section %s %s palette is empty' % (s.get('Y'), key)))
                    continue
                bits = 0 if n <= 1 else max(minbits, (n - 1).bit_length())
                got = len(c['data'][2]) if 'data' in c else 0
                if got != n_longs(count, bits):
                    problems.append((i, 'section %s %s palette %d data %d expected %d' % (
                        s.get('Y'), key, n, got, n_longs(count, bits))))
                    continue
                if 'data' in c and max(unpack(c['data'][2], bits, count)) >= n:
                    problems.append((i, 'index beyond palette in section %s %s' % (s.get('Y'), key)))
        for name, hm in v.get('Heightmaps', {}).items():
            if len(hm[2]) != 37:
                problems.append((i, 'heightmap %s has %d longs' % (name, len(hm[2]))))
    for k, idx in keysets.items():
        print(len(idx), 'chunks with root keys', list(k))
    for st, inv in inventory.items():
        print('status', st, {k: sorted(x) if isinstance(x, set) else x for k, x in inv.items()})
    postprocessing_report(chunks)
    references_report(path, chunks)
    print('problems', len(problems), problems[:20])


def decoded_states(v, blocks):
    """Yield ((x, y, z), state id) for every non-air block of a decoded chunk, using blocks.json names."""
    by_props = {}
    for name, b in blocks.items():
        for s in b['states']:
            by_props[(name, tuple(sorted((s.get('properties') or {}).items())))] = s['id']
    defaults = {name: [s for s in b['states'] if s.get('default')][0] for name, b in blocks.items()}
    cx, cz = v['xPos'], v['zPos']
    for s in v.get('sections', ('L', 10, []))[2]:
        bs = s.get('block_states')
        if bs is None:
            continue
        ids = []
        for e in bs['palette'][2]:
            name = e if isinstance(e, str) else e['id']
            props = dict(defaults[name].get('properties') or {})
            if isinstance(e, dict) and 'properties' in e:
                props.update(e['properties'])
            ids.append(by_props[(name, tuple(sorted(props.items())))])
        n = len(ids)
        if n == 0 or (n > 1 and len(bs.get('data', ('A', 12, []))[2]) != n_longs(4096, max(4, (n - 1).bit_length()))):
            continue
        vals = unpack(bs['data'][2], max(4, (n - 1).bit_length()), 4096) if n > 1 else [0] * 4096
        for i, p in enumerate(vals):
            if p < n and ids[p] != 0:
                yield ((cx << 4) | (i & 15), (s['Y'] << 4) | (i >> 8), (cz << 4) | ((i >> 4) & 15)), ids[p]


def cmd_debugcheck(blocks_path, region_dir):
    """Probe PC2: the debug world must hold every state once, in increasing id order along its grid rows."""
    blocks = json.load(open(blocks_path, encoding='utf-8'))
    found = {}
    for f in sorted(os.listdir(region_dir)):
        if f.endswith('.mca'):
            for i, off, cnt, ts, ln, ct, v in read_chunks(os.path.join(region_dir, f))[1]:
                if isinstance(v, dict):
                    for pos, sid in decoded_states(v, blocks):
                        found[pos] = sid
    per_y = {}
    for p, s in found.items():
        per_y.setdefault(p[1], set()).add(s)
    if not per_y:
        print('no decodable non-air block in', region_dir)
        return
    grid_y = max(per_y, key=lambda y: len(per_y[y]))
    grid = {p: s for p, s in found.items() if p[1] == grid_y}
    order = [grid[p] for p in sorted(grid, key=lambda p: (p[2], p[0]))]
    total = sum(len(b['states']) for b in blocks.values())
    print('levels with blocks', {y: len(v) for y, v in sorted(per_y.items())}, 'grid level', grid_y)
    print('grid positions', len(grid), 'distinct states', len(set(order)), 'of', total - 1, 'non-air states')
    print('ids non-decreasing along rows', all(a <= b for a, b in zip(order, order[1:])))
    print('first ids', order[:8], 'last ids', order[-4:])


def cmd_probe_cmds(blocks_path, x0, y0, z0):
    """Commands placing >= 257 distinct states into one section (probe PC3), with the `strict` mode of
    setblock, from blocks that do not react to their neighbours."""
    blocks = json.load(open(blocks_path, encoding='utf-8'))
    names = [n for n in blocks if n.endswith(('_glazed_terracotta', '_wool', '_concrete', '_stained_glass', '_planks'))
             or (n.endswith('_terracotta') and 'glazed' not in n) or n.endswith(('_log', '_wood'))]
    states = []
    for n in sorted(names):
        for s in sorted(blocks[n]['states'], key=lambda s: s['id']):
            p = s.get('properties')
            states.append(n + ('[' + ','.join('%s=%s' % kv for kv in sorted(p.items())) + ']' if p else ''))
    x0, y0, z0 = int(x0), int(y0), int(z0)
    print('# distinct states', len(states))
    for k, st in enumerate(states[:16 * 16 * 16]):
        print('setblock %d %d %d %s strict' % (x0 + (k & 15), y0 + (k >> 8), z0 + ((k >> 4) & 15), st))


if __name__ == '__main__':
    cmd, args = sys.argv[1], sys.argv[2:]
    {'states': cmd_states, 'dims': cmd_dims, 'templates': cmd_templates, 'pack': cmd_pack, 'coords': cmd_coords,
     'light': cmd_light, 'region': cmd_region, 'dump': cmd_dump, 'lz4': cmd_lz4, 'check': cmd_check,
     'probe-cmds': cmd_probe_cmds, 'debugcheck': cmd_debugcheck}[cmd](*args)
```

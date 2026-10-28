# Vecteurs : générateurs pseudo-aléatoires du moteur

Statut : brouillon
Jalon : M1
Risque : A
Spec : [core-random.md](core-random.md)

Ce fichier contient les vecteurs de la spec `core-random`, les deux scripts qui les produisent et les emplacements réservés aux vecteurs de l'oracle. Les identifiants R, H et P renvoient à la spec.

## Niveau de preuve des vecteurs

| Famille | Niveau | Ce qui fait foi |
|---|---|---|
| V-L1 à V-L5 (générateur 48 bits) | D pour l'algorithme public de `java.util.Random`, S pour l'égalité avec le jeu (R8) | Sortie de `java.util.Random` (JDK 21), recoupée bit à bit par une réimplémentation Python indépendante |
| V-L7 (sensibilité de la gaussienne) | C | Mesure JShell décrite dans la section |
| V-L6 (prédiction de la sonde P5) | S | Chaîne d'hypothèses R8, R35 et ordre des tirages du butin |
| V-X1 (xoroshiro128++ publié) | D | Réimplémentation Python recoupée avec la classe xoroshiro128++ du module `jdk.random` du JDK |
| V-X2 (mélange de Stafford) | D | Recoupé avec `java.util.SplittableRandom` du JDK (voir R23) |
| V-X3 (expansion d'une graine 64 bits) | C pour la fonction R22 à R24 ; S pour son emploi par le jeu (R25) | Réimplémentation Python recoupée avec le constructeur à graine 64 bits du xoroshiro128++ du JDK (lignes `X seed64`) |
| V-X4 (MD5) | D pour l'empreinte ; S pour l'ordre des octets | `hashlib` |
| V-X5 (séquences nommées) | S | Calcul sous les hypothèses de la spec ; seule une sonde de l'oracle les lèvera |
| V-X6 (source par position provisoire) | C, provisoire, non doré | Contrat provisoire R29 ; ne prouve rien sur le jeu |
| O-1 à O-5 (oracle) | M une fois remplis | Réservés, à remplir par l'`oracle-differ` |

## Reproduire

Les deux scripts sont dans la dernière section. Copier chaque bloc dans un fichier, puis :

```sh
jshell -q --add-modules jdk.random --add-exports jdk.random/jdk.random=ALL-UNNAMED legacy.jsh > java.txt
python -I vectors.py > python.txt
```

Chaque ligne de sortie a la forme `étiquette | valeur affichée | clé`. La clé est l'entier décimal, ou le motif IEEE 754 en hexadécimal pour un flottant. Contrôle croisé : les lignes commençant par `L ` ou `X ` doivent avoir des clés identiques dans les deux fichiers (vérifié le 2026-10-28 avec OpenJDK 21.0.10 et Python 3.14.7 sous Windows x86_64 : 418 lignes communes, dont les 5 lignes `X seed64` de V-X3, et 416 identiques ; les 2 écarts sont `seed=42 gauss#0` et `gauss#1`, d'une unité du dernier chiffre binaire, voir V-L7). Les valeurs Java font foi : elles suivent la spécification publique de `java.util.Random`, qui impose `StrictMath`. Les lignes `P ` (comptage de pas) et `H ` (hypothèses) ne viennent que de Python. La classe xoroshiro128++ appelée par le script JShell est celle du module `jdk.random` d'OpenJDK (implémentation publique de [BV]), sans lien avec le jeu.

## Vecteurs du générateur 48 bits et de xoroshiro128++

### V-L1 Tirages de base par graine

Chaque ligne repart d'un générateur neuf construit avec la graine. Flottants : valeur décimale affichée par Java, puis motif IEEE 754 entre crochets (le motif fait foi).

Graine `0`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | -1155484576, -723955400, 1033096058, -1690734402, -1557280266 |
| `nextLong()` x3 | -4962768465676381896, 4437113781045784766, -6688467811848818630 |
| `nextBoolean()` x16 (1 = vrai) | 1101101011000111 |
| `nextFloat()` x3 | 0.73096776 [0x3F3B20B4], 0.831441 [0x3F54D951], 0.24053639 [0x3E764F2C] |
| `nextDouble()` x3 | 0.730967787376657 [0x3FE764168EA6CA89], 0.24053641567148587 [0x3FCEC9E5B3672E14], 0.6374174253501083 [0x3FE465B93A78EF81] |
| `nextGaussian()` x4 | 0.8025330637390305 [0x3FE9AE59D1D6F861], -0.9015460884175122 [0xBFECD9772EB2E0C8], 2.080920790428163 [0x4000A5B9CCA3A4B8], 0.7637707684364894 [0x3FE870CF65026A96] |

Graine `1`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | -1155869325, 431529176, 1761283695, 1749940626, 892128508 |
| `nextLong()` x3 | -4964420948893066024, 7564655870752979346, 3831662765844904176 |
| `nextBoolean()` x16 (1 = vrai) | 1000000111001011 |
| `nextFloat()` x3 | 0.7308782 [0x3F3B1AD5], 0.100473166 [0x3DCDC4E0], 0.4100808 [0x3ED1F61C] |
| `nextDouble()` x3 | 0.7308781907032909 [0x3FE7635AA8CDC4E6], 0.41008081149220166 [0x3FDA3EC39684DF98], 0.20771484130971707 [0x3FCA96666128D71C] |
| `nextGaussian()` x4 | 1.561581040188955 [0x3FF8FC3C669AA4C1], -0.6081826070068602 [0xBFE3763B5EE2E541], -1.0912278829447088 [0xBFF175AB5E5BB186], -0.6245401364066232 [0xBFE3FC3B989CFC86] |

Graine `42`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | -1170105035, 234785527, -1360544799, 205897768, 1325939940 |
| `nextLong()` x3 | -5025562857975149833, -5843495416241995736, 5694868678511409995 |
| `nextBoolean()` x16 (1 = vrai) | 1010010110100001 |
| `nextFloat()` x3 | 0.7275637 [0x3F3A419D], 0.054665208 [0x3D5FE8A0], 0.6832234 [0x3F2EE7BB] |
| `nextDouble()` x3 | 0.7275636800328681 [0x3FE74833A06FF457], 0.6832234717598454 [0x3FE5DCF778622E01], 0.30871945533265976 [0x3FD3C20F3F12BBB4] |
| `nextGaussian()` x4 | 1.1419053154730547 [0x3FF2453E82115D86], 0.9194079489827879 [0x3FED6BCA38120847], -0.9498666368908959 [0xBFEE654EB7A040C2], -1.1069902863993377 [0xBFF1B63B72513280] |

Graine `-1`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | 1155099827, 1887904451, 52699159, -1941176418, -1451336087 |
| `nextLong()` x3 | 4961115982468162243, 226341162490527646, -6233441030884181172 |
| `nextBoolean()` x16 (1 = vrai) | 0001110010100001 |
| `nextFloat()` x3 | 0.2689426 [0x3E89B2D8], 0.43956196 [0x3EE10E44], 0.012269974 [0x3C490800] |
| `nextDouble()` x3 | 0.26894263088050496 [0x3FD1365B2708722C], 0.012269981921235296 [0x3F8921011897FF00], 0.6620844841121951 [0x3FE52FCBCCCE6DDA] |
| `nextGaussian()` x4 | 1.7853314409882288 [0x3FFC90B7B3790A3A], -0.9204169061847902 [0xBFED740E27D7F93C], 0.4869392448030407 [0x3FDF2A0338E45A13], 0.4568888042977182 [0x3FDD3DAA8A1780ED] |

Graine `12345`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | 1553932502, -2090749135, -287790814, -355989640, -716867186 |
| `nextLong()` x3 | 6674089274190705457, -1236052134575208584, -3078921119283744887 |
| `nextBoolean()` x16 (1 = vrai) | 0111100000010110 |
| `nextFloat()` x3 | 0.36180305 [0x3EB93E40], 0.5132095 [0x3F0361B3], 0.9329935 [0x3F6ED8A9] |
| `nextDouble()` x3 | 0.3618031071604718 [0x3FD727C838361B32], 0.932993485288541 [0x3FEDDB152756403B], 0.8330913489710237 [0x3FEAA8AF304D277C] |
| `nextGaussian()` x4 | -0.187808989658912 [0xBFC80A1FFE3D6B0B], 0.5884363051154796 [0x3FE2D4785FC80083], 0.9488047804400426 [0x3FEE5C9BD7C8E902], -0.49428072062604445 [0xBFDFA24B9A887588] |

Graine `-123456789`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | 1442175866, -958985574, -148610113, -1285892480, 2058547138 |
| `nextLong()` x3 | 6194098178591492762, -638275576475756928, 8841392636790150424 |
| `nextBoolean()` x16 (1 = vrai) | 0111001100011011 |
| `nextFloat()` x3 | 0.3357827 [0x3EABEBB6], 0.77671874 [0x3F46D70A], 0.96539897 [0x3F772463] |
| `nextDouble()` x3 | 0.33578273321269103 [0x3FD57D76DC6D70A8], 0.9653990075082306 [0x3FEEE48C759AD6B4], 0.4792928609316691 [0x3FDEACBBF6BA1950] |
| `nextGaussian()` x4 | -0.07599880208814705 [0xBFB374A85180F6F2], 0.21538397122056793 [0x3FCB91B3B43CCAFB], -0.06760481075219917 [0xBFB14E8C833BA391], 1.161664702044802 [0x3FF2962DBA03333D] |

Graine `-9223372036854775808`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | -1155484576, -723955400, 1033096058, -1690734402, -1557280266 |
| `nextLong()` x3 | -4962768465676381896, 4437113781045784766, -6688467811848818630 |
| `nextBoolean()` x16 (1 = vrai) | 1101101011000111 |
| `nextFloat()` x3 | 0.73096776 [0x3F3B20B4], 0.831441 [0x3F54D951], 0.24053639 [0x3E764F2C] |
| `nextDouble()` x3 | 0.730967787376657 [0x3FE764168EA6CA89], 0.24053641567148587 [0x3FCEC9E5B3672E14], 0.6374174253501083 [0x3FE465B93A78EF81] |
| `nextGaussian()` x4 | 0.8025330637390305 [0x3FE9AE59D1D6F861], -0.9015460884175122 [0xBFECD9772EB2E0C8], 2.080920790428163 [0x4000A5B9CCA3A4B8], 0.7637707684364894 [0x3FE870CF65026A96] |

Graine `9223372036854775807`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | 1155099827, 1887904451, 52699159, -1941176418, -1451336087 |
| `nextLong()` x3 | 4961115982468162243, 226341162490527646, -6233441030884181172 |
| `nextBoolean()` x16 (1 = vrai) | 0001110010100001 |
| `nextFloat()` x3 | 0.2689426 [0x3E89B2D8], 0.43956196 [0x3EE10E44], 0.012269974 [0x3C490800] |
| `nextDouble()` x3 | 0.26894263088050496 [0x3FD1365B2708722C], 0.012269981921235296 [0x3F8921011897FF00], 0.6620844841121951 [0x3FE52FCBCCCE6DDA] |
| `nextGaussian()` x4 | 1.7853314409882288 [0x3FFC90B7B3790A3A], -0.9204169061847902 [0xBFED740E27D7F93C], 0.4869392448030407 [0x3FDF2A0338E45A13], 0.4568888042977182 [0x3FDD3DAA8A1780ED] |

Graine `281474976710656`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | -1155484576, -723955400, 1033096058, -1690734402, -1557280266 |
| `nextLong()` x3 | -4962768465676381896, 4437113781045784766, -6688467811848818630 |
| `nextBoolean()` x16 (1 = vrai) | 1101101011000111 |
| `nextFloat()` x3 | 0.73096776 [0x3F3B20B4], 0.831441 [0x3F54D951], 0.24053639 [0x3E764F2C] |
| `nextDouble()` x3 | 0.730967787376657 [0x3FE764168EA6CA89], 0.24053641567148587 [0x3FCEC9E5B3672E14], 0.6374174253501083 [0x3FE465B93A78EF81] |
| `nextGaussian()` x4 | 0.8025330637390305 [0x3FE9AE59D1D6F861], -0.9015460884175122 [0xBFECD9772EB2E0C8], 2.080920790428163 [0x4000A5B9CCA3A4B8], 0.7637707684364894 [0x3FE870CF65026A96] |

Graine `25214903917`

| Tirage | Valeurs successives |
|---|---|
| `nextInt()` x5 | 0, 4232237, 178803790, 758674372, 1565954732 |
| `nextLong()` x3 | 4232237, 767956431209526212, 6725724361348706664 |
| `nextBoolean()` x16 (1 = vrai) | 0000000010011110 |
| `nextFloat()` x3 | 0.0 [0x00000000], 9.85384E-4 [0x3A812800], 0.041630983 [0x3D2A8540] |
| `nextDouble()` x3 | 1.4683476656784933E-11 [0x3DB0250800000000], 0.0416310008860028 [0x3FA550A8969C39E0], 0.36460223930076674 [0x3FD755A4A1761716] |
| `nextGaussian()` x4 | -0.24556064950000045 [0xBFCF6E880764BD18], -0.7394188292619277 [0xBFE7A951AD374034], 0.1574777552730396 [0x3FC4283B285F629A], -1.5707675478955068 [0xBFF921DD26FD44E0] |

### V-L2 Entiers bornés

Générateur neuf par ligne : trois appels `nextInt(bound)` puis un `nextInt()` sans borne, qui révèle la consommation réelle.

Graine `0`

| bound | #0 | #1 | #2 | `nextInt()` suivant |
|---|---|---|---|---|
| 1 | 0 | 0 | 0 | -1690734402 |
| 2 | 1 | 1 | 0 | -1690734402 |
| 7 | 5 | 2 | 4 | -1690734402 |
| 10 | 0 | 8 | 9 | -1690734402 |
| 16 | 11 | 13 | 3 | -1690734402 |
| 26 | 18 | 18 | 23 | -1690734402 |
| 100 | 60 | 48 | 29 | -1690734402 |
| 1073741824 | 784870680 | 892752974 | 258274014 | -1690734402 |
| 1073741825 | 516548029 | 663681053 | 251269761 | -1728529858 |
| 2147483647 | 1569741360 | 1785505948 | 516548029 | -1690734402 |

Graine `42`

| bound | #0 | #1 | #2 | `nextInt()` suivant |
|---|---|---|---|---|
| 1 | 0 | 0 | 0 | 205897768 |
| 2 | 1 | 0 | 1 | 205897768 |
| 7 | 1 | 5 | 6 | 205897768 |
| 10 | 0 | 3 | 8 | 205897768 |
| 16 | 11 | 0 | 10 | 205897768 |
| 26 | 0 | 7 | 22 | 205897768 |
| 100 | 30 | 63 | 48 | 205897768 |
| 1073741824 | 781215565 | 58696381 | 733605624 | 205897768 |
| 1073741825 | 117392763 | 102948884 | 662969970 | -248792245 |
| 2147483647 | 1562431130 | 117392763 | 1467211248 | 205897768 |

Graine `-1`

| bound | #0 | #1 | #2 | `nextInt()` suivant |
|---|---|---|---|---|
| 1 | 0 | 0 | 0 | -1941176418 |
| 2 | 0 | 0 | 0 | -1941176418 |
| 7 | 3 | 6 | 4 | -1941176418 |
| 10 | 3 | 5 | 9 | -1941176418 |
| 16 | 4 | 7 | 0 | -1941176418 |
| 26 | 5 | 21 | 9 | -1941176418 |
| 100 | 13 | 25 | 79 | -1941176418 |
| 1073741824 | 288774956 | 471976112 | 13174789 | -1941176418 |
| 1073741825 | 577549913 | 943952225 | 26349579 | -1941176418 |
| 2147483647 | 577549913 | 943952225 | 26349579 | -1941176418 |

Graine `-9223372036854775808`

| bound | #0 | #1 | #2 | `nextInt()` suivant |
|---|---|---|---|---|
| 1 | 0 | 0 | 0 | -1690734402 |
| 2 | 1 | 1 | 0 | -1690734402 |
| 7 | 5 | 2 | 4 | -1690734402 |
| 10 | 0 | 8 | 9 | -1690734402 |
| 16 | 11 | 13 | 3 | -1690734402 |
| 26 | 18 | 18 | 23 | -1690734402 |
| 100 | 60 | 48 | 29 | -1690734402 |
| 1073741824 | 784870680 | 892752974 | 258274014 | -1690734402 |
| 1073741825 | 516548029 | 663681053 | 251269761 | -1728529858 |
| 2147483647 | 1569741360 | 1785505948 | 516548029 | -1690734402 |

### V-L3 Rejets de `nextInt(1073741825)`

Borne 2^30+1 : environ une valeur de 31 bits sur deux est rejetée. Colonne "pas" : nombre d'avancées de l'état consommées par l'appel (calcul C, script Python).

| Graine | Résultat | Pas | `nextInt()` suivant |
|---|---|---|---|
| 0 | 516548029 | 3 | -1690734402 |
| 1 | 215764588 | 2 | 1761283695 |
| 2 | 630021372 | 2 | -423279216 |
| 3 | 152454210 | 3 | -836442134 |
| 4 | 467700105 | 6 | -1376291473 |
| 5 | 379250092 | 2 | 379066948 |
| 6 | 793506876 | 2 | -1805495963 |
| 7 | 20678044 | 4 | 1495978761 |
| 8 | 32431021 | 4 | 858120744 |
| 9 | 542735596 | 2 | -1003149799 |
| 10 | 956992380 | 2 | 1107254586 |
| 11 | 917721111 | 3 | 919154311 |
| 12 | 128478812 | 2 | 1181413113 |
| 13 | 706221100 | 2 | 1909600750 |
| 14 | 730985498 | 5 | -1800026279 |
| 15 | 226612738 | 3 | 1796952534 |

### V-L4 Saut

Graine 0, saut de 1 000 000 pas (ou 1 000 000 appels `nextInt()`), puis `nextInt()` = 1257967379.

### V-L5 Séquence mixte (graine 12345)

Un seul générateur, appels enchaînés dans l'ordre. Total consommé par les appels 0 à 8 : 13 pas.

| # | Appel | Résultat |
|---|---|---|
| 0 | `nextInt()` | 1553932502 |
| 1 | `nextInt(10)` | 0 |
| 2 | `nextLong()` | -1236052134575208584 |
| 3 | `nextBoolean()` (1 = vrai) | 1 |
| 4 | `nextFloat()` | 0.037672937 [0x3D1A4EF0] |
| 5 | `nextDouble()` | 0.32647575623792624 [0x3FD4E4FA91FEA406] |
| 6 | `nextGaussian()` | -1.223411937180115 [0xBFF39318653B97F7] |
| 7 | `nextGaussian()` (valeur en cache) | -0.6979609783968826 [0xBFE655B243032927] |
| 8 | `nextInt(1073741825)` | 962239390 |
| 9 | `setSeed(12345)` puis `nextInt()` | 1553932502 |

### V-X1 xoroshiro128++ publié : sorties brutes

Sorties de 64 bits en entier signé, état initial (s0, s1) donné. Recoupé avec l'implémentation indépendante du JDK (module `jdk.random`).

| État initial (s0, s1) | 5 sorties | État après 5 sorties |
|---|---|---|
| (0x0000000000000001, 0x0000000000000002) | 393217, 669327710093319, 1732421326133921491, -7051953992050424633, -8891291296936358940 | (0x21A3060E10687053, 0xC141020330186810) |
| (0x0123456789ABCDEF, 0xFEDCBA9876543210) | 81985529216486894, -6887384918253362076, -3704330611534247336, -6724422987835305309, -251466190376645636 | (0xCAAF9AD20231BFEC, 0xD94221A27B82D871) |
| (0x0000000000000000, 0x0000000000000001) | 131072, 35459252224001, 576535519371067973, -8536533096778751167, 4866527886176355657 | (0x2080830602203021, 0x4000800010082010) |
| (0xFFFFFFFFFFFFFFFF, 0xFFFFFFFFFFFFFFFF) | -131074, -2, -274880004097, -575937384760344506, -8682935975298663105 | (0xFF0100FFF8181FC0, 0xFFFE00000808003F) |

### V-X2 Mélange de Stafford (variante 13)

| Entrée | Sortie |
|---|---|
| 0x0000000000000000 | 0x0000000000000000 |
| 0x0000000000000001 | 0x5692161D100B05E5 |
| 0x6A09E667F3BCC909 | 0x3564B439CD1E1F16 |
| 0x084160217307451E | 0x63CFC62A2B097592 |
| 0xFFFFFFFFFFFFFFFF | 0xB4D055FCF2CBBD7B |

### V-X3 Initialisation par une graine 64 bits (R22 à R24 : fonction C, emploi par le jeu S)

La colonne des trois premiers `nextLong()` est reproduite à l'identique par `java.util.random.RandomGeneratorFactory.of("Xoroshiro128PlusPlus").create(graine)` (lignes `X seed64` du contrôle croisé).


| Graine | Mots non mélangés (lo, hi) | État après mélange (s0, s1) | 3 premiers `nextLong()` |
|---|---|---|---|
| 0 | (0x6A09E667F3BCC909, 0x084160217307451E) | (0x3564B439CD1E1F16, 0x63CFC62A2B097592) | 3038984756725240190, -3694039286755638414, 4633751808701151732 |
| 1 | (0x6A09E667F3BCC908, 0x084160217307451D) | (0x492B8D6066C09227, 0x1AC046DDA8E86E2A) | -1033667707219518978, 6451672561743293322, -1821890263888393630 |
| -1 | (0x95F619980C4336F6, 0x342D93518B8DB30B) | (0xFE766AB13AFAB784, 0x28B159DEFD9C7FBA) | -8676505878415342125, -868585888688873692, -6331679347063163302 |
| 12345 | (0x6A09E667F3BCF930, 0x0841602173077545) | (0x0A2C34E6CA54DD9E, 0xCF828DADC78BBEEB) | -8118485274630516485, 8241557746459281790, 4143755034716878659 |
| -9223372036854775808 | (0xEA09E667F3BCC909, 0x884160217307451E) | (0xA76C539A9A3F0612, 0x4B9E7F42D2702803) | 5559461712809880615, 4137405319755209525, -3866801757029155188 |

### V-X4 Empreinte MD5 d'un identifiant

| Identifiant | MD5 (hex) | Mots si gros-boutiste (lo, hi) | Mots si petit-boutiste (lo, hi) |
|---|---|---|---|
| `probe:a` | 27cdd0728604f33df0084ce9792a919d | (0x27CDD0728604F33D, 0xF0084CE9792A919D) | (0x3DF3048672D0CD27, 0x9D912A79E94C08F0) |
| `minecraft:test` | 1a2b64f5d3bdb4612e96d0a422fe68a7 | (0x1A2B64F5D3BDB461, 0x2E96D0A422FE68A7) | (0x61B4BDD3F5642B1A, 0xA768FE22A4D0962E) |

### V-X6 Source par position provisoire (R29, C, non dorée)

Contrat provisoire du moteur, sans valeur de preuve sur le jeu : `hp(x, y, z) = mélange(mélange(mélange(x) XOR y) XOR z)` (coordonnées étendues en signe à 64 bits), source = `Init64(d XOR hp)`. Ces vecteurs servent seulement à tester l'implémentation provisoire ; ils seront remplacés quand Q1 sera levée.

| `d` | Position (x, y, z) | `hp` | Premier `nextLong()` |
|---|---|---|---|
| 0 | (0, 0, 0) | 0x0000000000000000 | 3038984756725240190 |
| 0 | (1, 2, 3) | 0xA55BDF37C08724B5 | 1595374214425681131 |
| 0 | (-1, -64, -1) | 0xA0C6E26C8ED33FBB | 2798568289328956981 |
| 0 | (2147483647, 319, -2147483648) | 0x32B5375958F68299 | 2052514311196887087 |
| 12345 | (0, 0, 0) | 0x0000000000000000 | -8118485274630516485 |
| 12345 | (1, 2, 3) | 0xA55BDF37C08724B5 | -1563368128130346438 |
| 12345 | (-1, -64, -1) | 0xA0C6E26C8ED33FBB | -4312487232099831939 |
| 12345 | (2147483647, 319, -2147483648) | 0x32B5375958F68299 | 1896549530065401711 |

### V-X5 Séquences nommées : prédictions à confronter à l'oracle (niveau S)

Colonnes "état" : contenu attendu du champ `source` (tableau de deux longs signés, mot bas puis mot haut) dans `data/random_sequences.dat`. Les sorties de `/random value` sont prédites sous H-X2 avec la valeur `min + nextInt(max - min + 1)`. Chaque colonne de sorties part d'une séquence fraîchement réinitialisée.

| Cas | Réglages | État initial | `/random value 0..2147483645` x5 | État après ces 5 | `/random value 1..6` x10 |
|---|---|---|---|---|---|
| N1 | graine du monde exclue, sel 0, id exclu | `[L;3847398142028685078,7192185014346937746]` | 2067245245, 390848952, 326786297, 668760460, 1894545760 | `[L;5907017683142997804,-5217483301452126061]` | 6, 2, 1, 2, 6, 6, 1, 2, 4, 3 |
| N2 | graine du monde exclue, sel 1, id exclu | `[L;5272463233947570727,1927618558350093866]` | 867282174, 418117060, 412632368, 1434538546, 383715241 | `[L;-1430254592785879641,-8290170135680512777]` | 3, 2, 2, 5, 2, 4, 6, 2, 1, 2 |
| N3 | graine du monde exclue, sel -1 étendu en signe, id exclu | `[L;-110783831392733308,2932223646667407290]` | 1421574888, 1004810129, 458210092, 2121564838, 466775805 | `[L;2173709032643078312,-2849544362869967681]` | 4, 3, 2, 6, 2, 6, 5, 6, 2, 6 |
| N3z | contre-hypothèse de N3 : sel étendu par des zéros | `[L;5368716647786959924,-554802411955493188]` | 343469287, 117407115, 946035148, 1039241215, 10099746 | `[L;-8264014475489643770,-6056685171208551996]` | 1, 1, 3, 3, 1, 3, 3, 1, 5, 1 |
| N4 | graine du monde exclue, sel 0, id `probe:a` inclus, MD5 gros-boutiste | `[L;2153501729715799201,-2652207934038623535]` | 1793465186, 323874290, 1138589083, 1914133938, 1261544791 | `[L;6887961208285858452,-2052858044374788392]` | 6, 1, 4, 6, 4, 3, 2, 1, 5, 2 |
| N4le | contre-hypothèse de N4 : MD5 petit-boutiste | `[L;1165417471098110475,-2632000392545274068]` | 790826154, 924853721, 1233154537, 1415715827, 15974204 | `[L;5762611463864902182,483216415578334690]` | 3, 3, 4, 4, 1, 2, 4, 3, 3, 1 |
| N5 | graine du monde 12345 incluse, sel 0, id exclu | `[L;733019005196230046,-3494074583369400597]` | 28592253, 1758037181, 1883544479, 39523540, 1191494888 | `[L;-3380976663154756996,7698574065656511185]` | 1, 5, 6, 1, 4, 5, 2, 4, 2, 6 |
| N6 | graine du monde 12345 incluse, sel 0, id `probe:a` inclus | `[L;-8338171165017409289,2650172299325186875]` | 2089840009, 1915533696, 1931485652, 1250613981, 1567712703 | `[L;-4285309943909617478,3450026542495718202]` | 6, 6, 6, 4, 5, 1, 5, 6, 3, 6 |
| N7 | graine du monde 12345 incluse, sel 7, id `minecraft:test` inclus | `[L;-8196287188144295939,4607975983393672058]` | 809584177, 2031271794, 356800533, 1450885541, 2027094301 | `[L;-8776242114209539175,4340599208118940372]` | 3, 6, 1, 5, 6, 5, 3, 4, 1, 6 |

Contre-hypothèses pour N1 (la sonde P2 les départage) :

- H-X2b (31 bits de poids fort, rejet à la manière du générateur 48 bits) : 353784388, 1717440875, 539439708, 251523796, 214130917.
- H-X2c (méthode de H-X2 appliquée aux 32 bits de poids fort) : 353784387, 1717440873, 539439707, 251523795, 214130916.
- Sous H-X2, `/random value 0..15` x10 depuis N1 (borne puissance de deux, sans cas particulier) : 15, 2, 2, 4, 14, 15, 1, 5, 10, 5.
- Sous H-X3, trois `nextFloat()` depuis l'état N1 : motifs 0x3E28B290, 0x3F4CBC22, 0x3E809CC8. Sous H-X4, trois `nextDouble()` : 0.16474369376959186, 0.7997457290026366, 0.2511961888876212.

### V-L6 Prédiction de la sonde P5 (niveau S)

Sous R8 et R35, un conteneur dont la graine de butin vaut `s` (non nulle) et dont la table n'a qu'un pool à `rolls` uniforme 1..27 et une seule entrée d'un objet empilable contient au total `1 + nextInt(27)` objets, tirés du générateur 48 bits neuf de graine `s` (calcul : `new java.util.Random(s)`).

| Graine `s` | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | -1 | 12345 | -9223372036854775808 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Total attendu | 25 | 23 | 24 | 27 | 27 | 26 | 26 | 2 | 2 | 1 | 1 | 4 | 5 | 3 | 4 | 16 | 15 | 23 | 25 |

Les graines consécutives donnent des premiers tirages corrélés (propriété connue des générateurs congruentiels, visible ici : 27, 27 puis 26, 26). La sonde doit donc utiliser toute la liste, graines éloignées comprises.

### V-L7 Sensibilité de la gaussienne au logarithme (calcul C, appui de R9)

Même paire polaire, graine 42, premier essai accepté (`s = 0.3414242762953298`) :

| Logarithme utilisé | `nextGaussian()` #0 | #1 (valeur en cache) |
|---|---|---|
| `StrictMath.log` (fdlibm, imposé par la spécification de `java.util.Random`) | 0x3FF2453E82115D86 | 0x3FED6BCA38120847 |
| `Math.log` de HotSpot (JDK 21.0.10, x86_64) | 0x3FF2453E82115D87 | 0x3FED6BCA38120848 |
| `math.log` de Python 3.14.7 (Windows) | 0x3FF2453E82115D87 | 0x3FED6BCA38120848 |

Mesure sur 1 571 801 paires acceptées (graine 42) : `Math.log` et `StrictMath.log` diffèrent pour 114 087 valeurs de `s` (7,3 %), et le multiplicateur final pour 54 359 paires (3,5 %). Commande (JShell, une ligne) : boucle de 2 000 000 essais polaires sur `new java.util.Random(42L)` comparant `StrictMath.sqrt(-2*StrictMath.log(s)/s)` et `Math.sqrt(-2*Math.log(s)/s)`. Le choix du logarithme change donc une gaussienne sur trente environ : il doit être fixé par la sonde ou la question ouverte Q3, pas présumé.

## Emplacements réservés aux vecteurs de l'oracle

À remplir par l'`oracle-differ` depuis `reports/probes/`, avec la date, la version (26.3, data version 5023) et la commande exacte. Une ligne remplie et conforme fait passer la règle correspondante au niveau M.

### O-1 (sonde P1) : état initial des séquences nommées

| Cas | Attendu (V-X5) | Observé | Verdict |
|---|---|---|---|
| N1 | `[L;3847398142028685078,7192185014346937746]` | à remplir | |
| N2 | `[L;5272463233947570727,1927618558350093866]` | à remplir | |
| N3 (ou N3z) | `[L;-110783831392733308,2932223646667407290]` | à remplir | |
| N4 (ou N4le) | `[L;2153501729715799201,-2652207934038623535]` | à remplir | |
| N5 | `[L;733019005196230046,-3494074583369400597]` | à remplir | |
| N6 | `[L;-8338171165017409289,2650172299325186875]` | à remplir | |
| N7 | `[L;-8196287188144295939,4607975983393672058]` | à remplir | |

### O-2 (sonde P2) : `/random value 0..2147483645` x5 et état après

| Cas | Valeurs observées | État observé après 5 | Hypothèse retenue (H-X2, H-X2b, H-X2c, autre) |
|---|---|---|---|
| N1 | à remplir | à remplir | |
| N4 | à remplir | à remplir | |
| N6 | à remplir | à remplir | |

### O-3 (sonde P3) : bornes puissance de deux et petites bornes

| Cas | `/random value 0..15` x10 | `/random value 1..6` x10 |
|---|---|---|
| N1 | à remplir | à remplir |

### O-4 (sonde P4) : limites de l'intervalle de `/random value`

| Intervalle | Taille (max - min + 1) | Succès ou message d'erreur observé |
|---|---|---|
| `0..0` | 1 | à remplir |
| `0..1` | 2 | à remplir |
| `0..2147483645` | 2147483646 | à remplir |
| `0..2147483646` | 2147483647 | à remplir |
| `-2147483648..-3` | 2147483646 | à remplir |
| `-2147483648..2147483647` | 2^32 | à remplir |
| `5..5` | 1 | à remplir |

Consommation du cas `5..5` (seulement s'il est accepté) : après `/random reset probe:a 0 false false`, `/random value 5..5 probe:a`, puis `/random value 0..2147483645 probe:a`.

| Valeur de la seconde commande | Interprétation |
|---|---|
| 2067245245 | `5..5` sans tirage (H-R33b) |
| 390848952 | `5..5` avec un tirage (H-R33c, sous H-X2) |
| observé : à remplir | |

### O-5 (sonde P5) : graine de butin d'un conteneur

| Graine `s` | Total attendu (V-L6) | Total observé |
|---|---|---|
| 1 à 16, -1, 12345, -9223372036854775808 | voir V-L6 | à remplir |

## Scripts

Script Python `vectors.py` (générateur 48 bits, xoroshiro128++, hypothèses H) :

<!-- BEGIN vectors.py -->
```python
import hashlib
import math
import struct

M32 = (1 << 32) - 1
M48 = (1 << 48) - 1
M64 = (1 << 64) - 1
LCG_MUL = 0x5DEECE66D
LCG_ADD = 0xB
SILVER = 0x6A09E667F3BCC909
GOLDEN = 0x9E3779B97F4A7C15


def s32(v):
    v &= M32
    return v - (1 << 32) if v >> 31 else v


def s64(v):
    v &= M64
    return v - (1 << 64) if v >> 63 else v


def f32_key(x):
    return "0x%08X" % struct.unpack(">I", struct.pack(">f", x))[0]


def f64_key(x):
    return "0x%016X" % struct.unpack(">Q", struct.pack(">d", x))[0]


def h64(v):
    return "0x%016X" % (v & M64)


class Lcg48:
    def __init__(self, seed):
        self.steps = 0
        self.set_seed(seed)

    def set_seed(self, seed):
        self.state = (seed ^ LCG_MUL) & M48
        self.cached_gaussian = None

    def next_bits(self, bits):
        self.state = (self.state * LCG_MUL + LCG_ADD) & M48
        self.steps += 1
        return s32(self.state >> (48 - bits))

    def next_int(self, bound=None):
        if bound is None:
            return self.next_bits(32)
        if bound <= 0:
            raise ValueError("bound must be positive")
        if bound & (bound - 1) == 0:
            return (bound * self.next_bits(31)) >> 31
        while True:
            bits = self.next_bits(31)
            val = bits % bound
            if bits - val + (bound - 1) < (1 << 31):
                return val

    def next_long(self):
        hi = self.next_bits(32)
        lo = self.next_bits(32)
        return s64((hi << 32) + lo)

    def next_boolean(self):
        return self.next_bits(1) != 0

    def next_float(self):
        return self.next_bits(24) * 2.0 ** -24

    def next_double(self):
        hi = self.next_bits(26)
        lo = self.next_bits(27)
        return ((hi << 27) + lo) * 2.0 ** -53

    def next_gaussian(self):
        if self.cached_gaussian is not None:
            g, self.cached_gaussian = self.cached_gaussian, None
            return g
        while True:
            v1 = 2 * self.next_double() - 1
            v2 = 2 * self.next_double() - 1
            s = v1 * v1 + v2 * v2
            if 0 < s < 1:
                break
        # math.log is the platform libm, not fdlibm: may differ by 1 ulp from StrictMath (V-L7).
        mult = math.sqrt(-2 * math.log(s) / s)
        self.cached_gaussian = v2 * mult
        return v1 * mult

    def skip(self, n):
        # n steps of x -> MUL*x + ADD composed by squaring (O(log n)).
        a, c = 1, 0
        ma, mc = LCG_MUL, LCG_ADD
        while n:
            if n & 1:
                a, c = (a * ma) & M48, (c * ma + mc) & M48
            ma, mc = (ma * ma) & M48, (mc * ma + mc) & M48
            n >>= 1
        self.state = (a * self.state + c) & M48


def rotl(x, k):
    return ((x << k) | (x >> (64 - k))) & M64


def stafford13(z):
    z &= M64
    z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & M64
    z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & M64
    return z ^ (z >> 31)


class Xoro:
    def __init__(self, lo, hi):
        self.lo, self.hi = lo & M64, hi & M64
        self.steps = 0

    def next_raw(self):
        s0, s1 = self.lo, self.hi
        r = (rotl((s0 + s1) & M64, 17) + s0) & M64
        s1 ^= s0
        self.lo = rotl(s0, 49) ^ s1 ^ ((s1 << 21) & M64)
        self.hi = rotl(s1, 28)
        self.steps += 1
        return r

    def next_long(self):
        return s64(self.next_raw())

    def next_int_lemire(self, bound):
        # Hypothesis H-X2 (level S).
        u = self.next_raw() & M32
        m = u * bound
        low = m & M32
        if low < bound:
            threshold = ((1 << 32) - bound) % bound
            while low < threshold:
                u = self.next_raw() & M32
                m = u * bound
                low = m & M32
        return m >> 32

    def next_int_java_style(self, bound):
        # Counter-hypothesis H-X2b: 31 high bits, rejection as in the 48-bit generator.
        while True:
            bits = self.next_raw() >> 33
            val = bits % bound
            if bits - val + (bound - 1) < (1 << 31):
                return val

    def next_int_lemire_high(self, bound):
        # Counter-hypothesis H-X2c: same as H-X2 on the 32 high bits.
        while True:
            m = (self.next_raw() >> 32) * bound
            if (m & M32) >= ((1 << 32) - bound) % bound:
                return m >> 32

    def next_float(self):
        return (self.next_raw() >> 40) * 2.0 ** -24

    def next_double(self):
        return (self.next_raw() >> 11) * 2.0 ** -53


def expand_unmixed(seed):
    lo = (seed ^ SILVER) & M64
    return lo, (lo + GOLDEN) & M64


def md5_words(text, order):
    d = hashlib.md5(text.encode("utf-8")).digest()
    byteorder = {"BE": "big", "LE": "little"}[order]
    return int.from_bytes(d[:8], byteorder), int.from_bytes(d[8:], byteorder)


def named_sequence(world_seed, salt, with_world, with_id, seq_id, order="BE", salt_ext="sign"):
    salt64 = salt if salt_ext == "sign" else salt & M32
    v = ((world_seed if with_world else 0) ^ salt64) & M64
    lo, hi = expand_unmixed(v)
    if with_id:
        mlo, mhi = md5_words(seq_id, order)
        lo, hi = lo ^ mlo, hi ^ mhi
    return Xoro(stafford13(lo), stafford13(hi))


def out(label, shown, key):
    print("%s | %s | %s" % (label, shown, key))


def out_i(label, v):
    out(label, v, v)


SEEDS = [0, 1, 42, -1, 12345, -123456789, -(1 << 63), (1 << 63) - 1, 1 << 48, 25214903917]
BOUND_SEEDS = [0, 42, -1, -(1 << 63)]
BOUNDS = [1, 2, 7, 10, 16, 26, 100, 1 << 30, (1 << 30) + 1, (1 << 31) - 1]

for s in SEEDS:
    r = Lcg48(s)
    for i in range(5):
        out_i("L seed=%d int#%d" % (s, i), r.next_int())
    r = Lcg48(s)
    for i in range(3):
        out_i("L seed=%d long#%d" % (s, i), r.next_long())
    r = Lcg48(s)
    bits = "".join("1" if r.next_boolean() else "0" for _ in range(16))
    out("L seed=%d bool#0-15" % s, bits, bits)
    r = Lcg48(s)
    for i in range(3):
        out("L seed=%d float#%d" % (s, i), "-", f32_key(r.next_float()))
    r = Lcg48(s)
    for i in range(3):
        out("L seed=%d double#%d" % (s, i), "-", f64_key(r.next_double()))
    r = Lcg48(s)
    for i in range(4):
        out("L seed=%d gauss#%d" % (s, i), "-", f64_key(r.next_gaussian()))
for s in BOUND_SEEDS:
    for bound in BOUNDS:
        r = Lcg48(s)
        for i in range(3):
            out_i("L seed=%d bound=%d #%d" % (s, bound, i), r.next_int(bound))
        out_i("L seed=%d bound=%d then-int" % (s, bound), r.next_int())
for s in range(16):
    r = Lcg48(s)
    out_i("L reject seed=%d bound=1073741825" % s, r.next_int((1 << 30) + 1))
    out("P reject seed=%d steps" % s, r.steps, r.steps)
    out_i("L reject seed=%d then-int" % s, r.next_int())
r = Lcg48(0)
r.skip(1000000)
out_i("L seed=0 skip=1000000 int", r.next_int())
r = Lcg48(12345)
out_i("L mix#0 int", r.next_int())
out_i("L mix#1 int(10)", r.next_int(10))
out_i("L mix#2 long", r.next_long())
out_i("L mix#3 bool", int(r.next_boolean()))
out("L mix#4 float", "-", f32_key(r.next_float()))
out("L mix#5 double", "-", f64_key(r.next_double()))
out("L mix#6 gauss", "-", f64_key(r.next_gaussian()))
out("L mix#7 gauss", "-", f64_key(r.next_gaussian()))
out_i("L mix#8 int(1073741825)", r.next_int((1 << 30) + 1))
out("P mix steps before setSeed", r.steps, r.steps)
r.set_seed(12345)
out_i("L mix#9 int after setSeed(12345)", r.next_int())

for lo, hi in [(1, 2), (0x0123456789ABCDEF, 0xFEDCBA9876543210), (0, 1), (M64, M64)]:
    x = Xoro(lo, hi)
    for i in range(5):
        out_i("X raw state=(%s,%s) #%d" % (h64(lo), h64(hi), i), x.next_long())
    out("P raw state=(%s,%s) after5" % (h64(lo), h64(hi)), "-", "(%s,%s)" % (h64(x.lo), h64(x.hi)))

for z in [0, 1, SILVER, (SILVER + GOLDEN) & M64, M64]:
    out("H stafford13(%s)" % h64(z), "-", h64(stafford13(z)))

for s in [0, 1, -1, 12345, -(1 << 63)]:
    lo, hi = expand_unmixed(s)
    x = Xoro(stafford13(lo), stafford13(hi))
    out("H seed64=%d unmixed" % s, "-", "(%s,%s)" % (h64(lo), h64(hi)))
    out("H seed64=%d state" % s, "-", "(%s,%s)" % (h64(x.lo), h64(x.hi)))
    v = " ".join(str(x.next_long()) for _ in range(3))
    out("H seed64=%d longs#0-2" % s, "-", v)
    out("X seed64=%d longs#0-2" % s, v, v)

# Provisional position source (spec R29): engine contract only, not game behaviour.
for d in [0, 12345]:
    for px, py, pz in [(0, 0, 0), (1, 2, 3), (-1, -64, -1), ((1 << 31) - 1, 319, -(1 << 31))]:
        hp = stafford13(stafford13(stafford13(px & M64) ^ (py & M64)) ^ (pz & M64))
        lo, hi = expand_unmixed(d ^ hp)
        x = Xoro(stafford13(lo), stafford13(hi))
        out("H pos d=%d (%d,%d,%d)" % (d, px, py, pz), h64(hp), x.next_long())

for text in ["probe:a", "minecraft:test"]:
    for order in ["BE", "LE"]:
        lo, hi = md5_words(text, order)
        out("H md5(%s) %s" % (text, order), hashlib.md5(text.encode()).hexdigest(), "(%s,%s)" % (h64(lo), h64(hi)))

CASES = [
    ("N1", 0, 0, False, False, "probe:a", "BE", "sign"),
    ("N2", 0, 1, False, False, "probe:a", "BE", "sign"),
    ("N3", 0, -1, False, False, "probe:a", "BE", "sign"),
    ("N3z", 0, -1, False, False, "probe:a", "BE", "zero"),
    ("N4", 0, 0, False, True, "probe:a", "BE", "sign"),
    ("N4le", 0, 0, False, True, "probe:a", "LE", "sign"),
    ("N5", 12345, 0, True, False, "probe:a", "BE", "sign"),
    ("N6", 12345, 0, True, True, "probe:a", "BE", "sign"),
    ("N7", 12345, 7, True, True, "minecraft:test", "BE", "sign"),
]
for name, world, salt, ww, wi, sid, order, ext in CASES:
    tag = "H %s world=%d salt=%d W=%s I=%s id=%s md5=%s salt=%s" % (name, world, salt, ww, wi, sid, order, ext)
    x = named_sequence(world, salt, ww, wi, sid, order, ext)
    out(tag + " state", "-", "[L;%d,%d]" % (s64(x.lo), s64(x.hi)))
    big = named_sequence(world, salt, ww, wi, sid, order, ext)
    out(tag + " value 0..2147483645 x5", "-", " ".join(str(big.next_int_lemire(2147483646)) for _ in range(5)))
    out(tag + " state after 5", "-", "[L;%d,%d]" % (s64(big.lo), s64(big.hi)))
    dice = named_sequence(world, salt, ww, wi, sid, order, ext)
    out(tag + " value 1..6 x10", "-", " ".join(str(1 + dice.next_int_lemire(6)) for _ in range(10)))
    if name == "N1":
        alt = named_sequence(world, salt, ww, wi, sid, order, ext)
        out(tag + " value 0..2147483645 x5 (H-X2b)", "-", " ".join(str(alt.next_int_java_style(2147483646)) for _ in range(5)))
        alt = named_sequence(world, salt, ww, wi, sid, order, ext)
        out(tag + " value 0..2147483645 x5 (H-X2c)", "-", " ".join(str(alt.next_int_lemire_high(2147483646)) for _ in range(5)))
        pw = named_sequence(world, salt, ww, wi, sid, order, ext)
        out(tag + " value 0..15 x10", "-", " ".join(str(pw.next_int_lemire(16)) for _ in range(10)))
        fl = named_sequence(world, salt, ww, wi, sid, order, ext)
        out(tag + " float x3", "-", " ".join(f32_key(fl.next_float()) for _ in range(3)))
        db = named_sequence(world, salt, ww, wi, sid, order, ext)
        out(tag + " double x3", "-", " ".join(repr(db.next_double()) for _ in range(3)))
```
<!-- END vectors.py -->

Script JShell `legacy.jsh` (`java.util.Random` du JDK, et classe xoroshiro128++ du module `jdk.random` pour V-X1) :

<!-- BEGIN legacy.jsh -->
```java
import java.util.Random;

long[] SEEDS = {0L, 1L, 42L, -1L, 12345L, -123456789L, Long.MIN_VALUE, Long.MAX_VALUE, 281474976710656L, 25214903917L};
long[] BOUND_SEEDS = {0L, 42L, -1L, Long.MIN_VALUE};
int[] BOUNDS = {1, 2, 7, 10, 16, 26, 100, 1 << 30, (1 << 30) + 1, Integer.MAX_VALUE};

void out(String label, String shown, String key) { System.out.println(label + " | " + shown + " | " + key); }
void outI(String label, long v) { out(label, Long.toString(v), Long.toString(v)); }
void outF(String label, float v) { out(label, Float.toString(v), String.format("0x%08X", Float.floatToRawIntBits(v))); }
void outD(String label, double v) { out(label, Double.toString(v), String.format("0x%016X", Double.doubleToRawLongBits(v))); }

for (long s : SEEDS) {
    Random r = new Random(s);
    for (int i = 0; i < 5; i++) outI("L seed=" + s + " int#" + i, r.nextInt());
    r = new Random(s);
    for (int i = 0; i < 3; i++) outI("L seed=" + s + " long#" + i, r.nextLong());
    r = new Random(s);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < 16; i++) b.append(r.nextBoolean() ? '1' : '0');
    out("L seed=" + s + " bool#0-15", b.toString(), b.toString());
    r = new Random(s);
    for (int i = 0; i < 3; i++) outF("L seed=" + s + " float#" + i, r.nextFloat());
    r = new Random(s);
    for (int i = 0; i < 3; i++) outD("L seed=" + s + " double#" + i, r.nextDouble());
    r = new Random(s);
    for (int i = 0; i < 4; i++) outD("L seed=" + s + " gauss#" + i, r.nextGaussian());
}
for (long s : BOUND_SEEDS) {
    for (int bound : BOUNDS) {
        Random r = new Random(s);
        for (int i = 0; i < 3; i++) outI("L seed=" + s + " bound=" + bound + " #" + i, r.nextInt(bound));
        outI("L seed=" + s + " bound=" + bound + " then-int", r.nextInt());
    }
}
for (long s = 0; s < 16; s++) {
    Random r = new Random(s);
    outI("L reject seed=" + s + " bound=1073741825", r.nextInt((1 << 30) + 1));
    outI("L reject seed=" + s + " then-int", r.nextInt());
}
{
    Random r = new Random(0L);
    for (int i = 0; i < 1000000; i++) r.nextInt();
    outI("L seed=0 skip=1000000 int", r.nextInt());
}
{
    Random r = new Random(12345L);
    outI("L mix#0 int", r.nextInt());
    outI("L mix#1 int(10)", r.nextInt(10));
    outI("L mix#2 long", r.nextLong());
    outI("L mix#3 bool", r.nextBoolean() ? 1 : 0);
    outF("L mix#4 float", r.nextFloat());
    outD("L mix#5 double", r.nextDouble());
    outD("L mix#6 gauss", r.nextGaussian());
    outD("L mix#7 gauss", r.nextGaussian());
    outI("L mix#8 int(1073741825)", r.nextInt((1 << 30) + 1));
    r.setSeed(12345L);
    outI("L mix#9 int after setSeed(12345)", r.nextInt());
}
for (long[] st : new long[][] {{1L, 2L}, {0x0123456789ABCDEFL, 0xFEDCBA9876543210L}, {0L, 1L}, {-1L, -1L}}) {
    var x = new jdk.random.Xoroshiro128PlusPlus(st[0], st[1]);
    for (int i = 0; i < 5; i++) outI(String.format("X raw state=(0x%016X,0x%016X) #%d", st[0], st[1], i), x.nextLong());
}
for (long s : new long[] {0L, 1L, -1L, 12345L, Long.MIN_VALUE}) {
    var g = java.util.random.RandomGeneratorFactory.of("Xoroshiro128PlusPlus").create(s);
    String v = g.nextLong() + " " + g.nextLong() + " " + g.nextLong();
    out("X seed64=" + s + " longs#0-2", v, v);
}
/exit
```
<!-- END legacy.jsh -->

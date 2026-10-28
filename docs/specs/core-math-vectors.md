# Vecteurs de test : utilitaires mathématiques et coordonnées

Statut : brouillon
Jalon : M1
Risque : A

Compagnon de `core-math.md`. Tous les vecteurs ci-dessous sont de niveau C : calculés en Java 21.0.10 (OpenJDK, x64, Windows) avec les seuls opérateurs du langage et `java.lang.Math`, recoupés en Python 3.14.7 pour V11. Ils fixent notre contrat ; un vecteur qui illustre une règle S ne devient une référence qu'après la sonde indiquée. Notation hexadécimale : bits IEEE 754 bruts.

## V1. `floor` et `ceil` sur `double` (R4, R6)

Candidat F1 : plancher mathématique puis conversion saturante. Candidat F2 : troncature saturante puis correction de -1 si la valeur est strictement inférieure au tronqué, sans saturation de la correction. Même construction pour `ceil` (C1, C2).

| x | floor F1 | floor F2 | ceil C1 | ceil C2 |
|---|---:|---:|---:|---:|
| 0.0 | 0 | 0 | 0 | 0 |
| -0.0 | 0 | 0 | 0 | 0 |
| 0.5 | 0 | 0 | 1 | 1 |
| -0.5 | -1 | -1 | 0 | 0 |
| -1.0 | -1 | -1 | -1 | -1 |
| 15.999999999999998 | 15 | 15 | 16 | 16 |
| -15.000000000000002 | -16 | -16 | -15 | -15 |
| -1e-300 | -1 | -1 | 0 | 0 |
| 29999999.9 | 29999999 | 29999999 | 30000000 | 30000000 |
| -29999999.1 | -30000000 | -30000000 | -29999999 | -29999999 |
| 2147483647.5 | 2147483647 | 2147483647 | 2147483647 | -2147483648 |
| -2147483648.5 | -2147483648 | 2147483647 | -2147483648 | -2147483648 |
| 1e20 | 2147483647 | 2147483647 | 2147483647 | -2147483648 |
| -1e20 | -2147483648 | 2147483647 | -2147483648 | -2147483648 |
| NaN | 0 | 0 | 0 | 0 |
| +Infinity | 2147483647 | 2147483647 | 2147483647 | -2147483648 |
| -Infinity | -2147483648 | 2147483647 | -2147483648 | -2147483648 |

Les lignes à partir de `2147483647.5` sont hors domaine (S, interdit, R4). Commande oracle : aucune en M1 (Q4) ; P4 couvre les lignes du domaine.

## V2. `floor` sur `float` (R5)

| x (float) | bits | floor |
|---|---|---:|
| -0.0f | 0x80000000 | 0 |
| -0.5f | 0xbf000000 | -1 |
| -1.0E-30f | 0x8da24260 | -1 |
| 0.99999994f | 0x3f7fffff | 0 |
| -0.99999994f | 0xbf7fffff | -1 |
| 1.6777216E7f | 0x4b800000 | 16777216 |
| -1.6777216E7f | 0xcb800000 | -16777216 |
| NaNf | 0x7fc00000 | 0 (S) |
| -3.0E9f | 0xcf32d05e | S : -2147483648 (F1) ou 2147483647 (F2) |

`floor((double) 0.99999999) = 0`, mais `(float) 0.99999999 = 1.0f`, donc `floor` en `float` = 1.

## V3. Division, modulo et décalages (R8, R9, R28)

| v | v >> 4 | v & 15 | v / 16 (Java) | v % 16 (Java) | v >> 5 | v & 31 | v >> 9 | v & 511 | v >> 2 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| -2147483648 | -134217728 | 0 | -134217728 | 0 | -67108864 | 0 | -4194304 | 0 | -536870912 |
| -30000000 | -1875000 | 0 | -1875000 | 0 | -937500 | 0 | -58594 | 128 | -7500000 |
| -513 | -33 | 15 | -32 | -1 | -17 | 31 | -2 | 511 | -129 |
| -512 | -32 | 0 | -32 | 0 | -16 | 0 | -1 | 0 | -128 |
| -511 | -32 | 1 | -31 | -15 | -16 | 1 | -1 | 1 | -128 |
| -17 | -2 | 15 | -1 | -1 | -1 | 15 | -1 | 495 | -5 |
| -16 | -1 | 0 | -1 | 0 | -1 | 16 | -1 | 496 | -4 |
| -15 | -1 | 1 | 0 | -15 | -1 | 17 | -1 | 497 | -4 |
| -1 | -1 | 15 | 0 | -1 | -1 | 31 | -1 | 511 | -1 |
| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1 | 0 | 1 | 0 | 1 | 0 | 1 | 0 | 1 | 0 |
| 15 | 0 | 15 | 0 | 15 | 0 | 15 | 0 | 15 | 3 |
| 16 | 1 | 0 | 1 | 0 | 0 | 16 | 0 | 16 | 4 |
| 17 | 1 | 1 | 1 | 1 | 0 | 17 | 0 | 17 | 4 |
| 31 | 1 | 15 | 1 | 15 | 0 | 31 | 0 | 31 | 7 |
| 32 | 2 | 0 | 2 | 0 | 1 | 0 | 0 | 32 | 8 |
| 511 | 31 | 15 | 31 | 15 | 15 | 31 | 0 | 511 | 127 |
| 512 | 32 | 0 | 32 | 0 | 16 | 0 | 1 | 0 | 128 |
| 513 | 32 | 1 | 32 | 1 | 16 | 1 | 1 | 1 | 128 |
| 29999999 | 1874999 | 15 | 1874999 | 15 | 937499 | 31 | 58593 | 383 | 7499999 |
| 2147483647 | 134217727 | 15 | 134217727 | 15 | 67108863 | 31 | 4194303 | 511 | 536870911 |

Les colonnes `v / 16` et `v % 16` montrent l'erreur à éviter (troncature vers zéro). `floorDiv(v, 16)` et `floorMod(v, 16)` égalent `v >> 4` et `v & 15` sur les 2^32 valeurs (vérification exhaustive, 0 écart ; idem pour 32).

Autres : `floorDiv(MIN_VALUE, -1) = MIN_VALUE`, `floorMod(MIN_VALUE, -1) = 0`, `floorDiv(7, -2) = -4`, `floorMod(7, -2) = -1`, `floorDiv(-7, 2) = -4`, `floorMod(-7, 2) = 1`. Chunk vers bloc : `1875000 << 4 = 30000000`, `-1 << 4 = -16`, `134217728 << 4 = -2147483648` (débordement).

Commande oracle (P9) : `/forceload add v 0` puis `/forceload query v 0`, attendu chunk `[v >> 4, 0]`.

## V4. Indices dans le chunk et la section (R25, R28, R32)

| Bloc (x, y, z) | Chunk | Section y | Indice de liste | Indice YZX | PostProcessing 0ZYX | Réseau XZY | Quart (>> 2) |
|---|---|---:|---:|---:|---:|---:|---|
| (0, 0, 0) | (0, 0) | 0 | 4 | 0 | 0 | 0 | (0, 0, 0) |
| (-1, -64, -1) | (-1, -1) | -4 | 0 | 255 | 3855 | 4080 | (-1, -16, -1) |
| (15, 319, 15) | (0, 0) | 19 | 23 | 4095 | 4095 | 4095 | (3, 79, 3) |
| (17, 63, -17) | (1, -2) | 3 | 7 | 4081 | 4081 | 511 | (4, 15, -5) |
| (-16, -1, 16) | (-1, 1) | -1 | 3 | 3840 | 240 | 15 | (-4, -1, 4) |

Commande oracle (P30) : `/setblock` au bloc, `/save-all flush`, décodage du `.mca`.

## V5. Bloc empaqueté sur 64 bits (R29)

| (x, y, z) | long (hex) | long (décimal) | relu |
|---|---|---:|---|
| (0, 0, 0) | 0x0000000000000000 | 0 | (0, 0, 0) |
| (1, 2, 3) | 0x0000004000003002 | 274877919234 | (1, 2, 3) |
| (-1, -1, -1) | 0xFFFFFFFFFFFFFFFF | -1 | (-1, -1, -1) |
| (-1, -64, -1) | 0xFFFFFFFFFFFFFFC0 | -64 | (-1, -64, -1) |
| (15, 319, -17) | 0x000003FFFFFEF13F | 4398046441791 | (15, 319, -17) |
| (-33554432, -2048, -33554432) | 0x8000002000000800 | -9223371899415820288 | identique |
| (33554431, 2047, 33554431) | 0x7FFFFFDFFFFFF7FF | 9223371899415820287 | identique |
| (30000000, 319, -30000000) | 0x7270E02363C8013F | 8246337360317907263 | identique |
| (33554432, 2048, -33554433) | 0x8000001FFFFFF800 | -9223371899415824384 | (-33554432, -2048, 33554431) (repli) |

Commande oracle : aucune en M1 ; capture réseau au jalon M8.

## V6. Chunk empaqueté (R30)

| (x, z) | long (hex) | long (décimal) |
|---|---|---:|
| (0, 0) | 0x0000000000000000 | 0 |
| (1, 0) | 0x0000000000000001 | 1 |
| (0, 1) | 0x0000000100000000 | 4294967296 |
| (-1, -1) | 0xFFFFFFFFFFFFFFFF | -1 |
| (1, -1) | 0xFFFFFFFF00000001 | -4294967295 |
| (-1, 1) | 0x00000001FFFFFFFF | 8589934591 |
| (1875000, -1875000) | 0xFFE363C8001C9C38 | -8053063678125000 |
| (MIN_VALUE, MAX_VALUE) | 0x7FFFFFFF80000000 | 9223372034707292160 |

Décodage : `x = (int) v`, `z = (int) (v >>> 32)`. Commande oracle : P30 (références de structures).

## V7. Section empaquetée, format réseau (R31)

| (sx, sy, sz) | long (hex) |
|---|---|
| (0, 0, 0) | 0x0000000000000000 |
| (-1, -4, -1) | 0xFFFFFFFFFFFFFFFC |
| (1, 19, -2) | 0x000007FFFFE00013 |
| (-2097152, -524288, 2097151) | 0x800001FFFFF80000 |

## V8. Régions (R33)

| Chunk | Fichier | Local | Indice i | Octet localisation | Octet horodatage |
|---|---|---|---:|---:|---:|
| (30, -3) | r.0.-1.mca | (30, 29) | 958 | 3832 | 7928 |
| (-1, -1) | r.-1.-1.mca | (31, 31) | 1023 | 4092 | 8188 |
| (0, 0) | r.0.0.mca | (0, 0) | 0 | 0 | 4096 |
| (31, 31) | r.0.0.mca | (31, 31) | 1023 | 4092 | 8188 |
| (32, -32) | r.1.-1.mca | (0, 0) | 0 | 0 | 4096 |
| (-33, 32) | r.-2.1.mca | (31, 0) | 31 | 124 | 4220 |
| (-1875000, 1874999) | r.-58594.58593.mca | (8, 23) | 744 | 2976 | 7072 |

La première ligne recoupe l'exemple publié [W-RF]. Commande oracle : P30.

## V9. Directions (R35, R36)

| Id 3D | Nom | Vecteur | Opposé | Id 2D | Lacet |
|---:|---|---|---|---:|---:|
| 0 | down | (0, -1, 0) | up | - | - |
| 1 | up | (0, 1, 0) | down | - | - |
| 2 | north | (0, 0, -1) | south | 2 | 180 |
| 3 | south | (0, 0, 1) | north | 0 | 0 |
| 4 | west | (-1, 0, 0) | east | 1 | 90 |
| 5 | east | (1, 0, 0) | west | 3 | -90 |

Lacet vers direction (candidat R36) : 44.999 -> south, 45 -> west, -45 -> south, -45.001 -> east, 135 -> north, 180 -> north. Commandes oracle : P35, P36.

## V10. Modulo positif réel (R10)

| (a, b) | `a - floor(a/b)*b` | `((a % b) + b) % b` |
|---|---:|---:|
| (-1e-20, 360) | 360.0 | 0.0 |
| (-1, 360) | 359.0 | 359.0 |
| (370, 360) | 10.0 | 10.0 |
| (-720, 360) | 0.0 | 0.0 |
| (-0.0, 360) | 0.0 | 0.0 |
| (5.5, -2) | -0.5 | -0.5 |
| (-1e-14, 1) | 0.99999999999999 | 0.99999999999999 |

`frac(x) = x - floor(x)` : `frac(-0.25) = 0.75`, `frac(2.75) = 0.75`, `frac(-1e-20) = 1.0`.

## V11. Interpolation linéaire (R11)

Forme A `a + t*(b-a)`, forme B `(1-t)*a + t*b`.

| (t, a, b) | précision | A | B |
|---|---|---|---|
| (0, 1, 2) | double | 1.0 | 1.0 |
| (1, 1, 2) | double | 2.0 | 2.0 |
| (0.5, 1, 2) | double | 1.5 | 1.5 |
| (1.5, 0, 10) | double | 15.0 | 15.0 |
| (-0.5, 0, 10) | double | -5.0 | -5.0 |
| (1, 1e20, 1) | double | 0.0 | 1.0 |
| (0.3, -64, 320) | double | 51.19999999999999 (0x4049999999999998) | 51.2 (0x404999999999999a) |
| (0.3, -1.5, 3.5) | double | 0.0 | 2.220446049250313E-16 |
| (1, 1e20, 1) | float | 0.0f | 1.0f |
| (0.1, 0.1, 0.3) | float | 0.120000005f (0x3df5c290) | 0.12f (0x3df5c28f) |
| (0.3, -64, 320) | float | 51.200005f (0x424cccce) | 51.2f (0x424ccccd) |
| (0.3, -1.5, 3.5) | float | 0.0f | 1.1920929E-7f |

La ligne `(0.3, -1.5, 3.5)` sert à P11 : densité nulle (air) en forme A, positive (pierre) en forme B, dans les deux précisions.

Bilinéaire, forme A, x d'abord : `lerp2(0.3, 0.7 ; 0.1, -0.9, 0.35, 0.77) = 0.2731999999999999` (identique ici quel que soit l'ordre ; l'ordre diffère sur 55 % d'un échantillon aléatoire). Trilinéaire : `lerp3(0.25, 0.5, 0.75 ; 1, 2, 3, 4, 5, 6, 7, 8) = 5.25` (coins dans l'ordre 000, 100, 010, 110, 001, 101, 011, 111).

Inverse et correspondance : `inverseLerp(5, 0, 10) = 0.5`, `inverseLerp(-5, 0, 10) = -0.5`, `inverseLerp(5, 10, 0) = 0.5`, `inverseLerp(1, 1, 1) = NaN`, `inverseLerp(2, 1, 1) = Infinity` ; `map(0.5 ; 0, 1 -> -64, 320) = 128.0`, `map(25 ; 10, 20 -> 0, 100) = 150.0` (non borné).

## V12. Bornage (R15)

Formes : MM = `max(lo, min(v, hi))` ; CMP = `v < lo ? lo : (v > hi ? hi : v)` ; MIX = `v < lo ? lo : min(v, hi)`.

| (v, lo, hi) | MM | CMP | MIX |
|---|---:|---:|---:|
| (0.5, 0, 1) | 0.5 | 0.5 | 0.5 |
| (-1, 0, 1) | 0.0 | 0.0 | 0.0 |
| (2, 0, 1) | 1.0 | 1.0 | 1.0 |
| (NaN, 0, 1) | NaN | NaN | NaN |
| (-0.0, 0.0, 1) | 0.0 | -0.0 | -0.0 |
| (0.0, -0.0, 1) | 0.0 | 0.0 | 0.0 |
| (1.0, 0.0, -0.0) | 0.0 | -0.0 | -0.0 |
| (0.5, NaN, 1) | NaN | 0.5 | 0.5 |
| (0.5, 0, NaN) | NaN | 0.5 | NaN |
| (5, 10, 0) int | 10 | 10 | 10 |
| (20, 10, 0) int | 10 | 0 | 0 |
| (-5, 10, 0) int | 10 | 10 | 10 |

`Math.clamp(5, 10, 0)` de Java lève `IllegalArgumentException`.

## V13. Fondu quintique (R16)

| t | Horner `t*t*t*(t*(t*6-15)+10)` | développé `6t^5-15t^4+10t^3` |
|---:|---|---|
| 0 | 0.0 | 0.0 |
| 0.1 | 0.008560000000000003 (0x3f8187e7c06e19bb) | 0.008560000000000002 (0x3f8187e7c06e19ba) |
| 0.25 | 0.103515625 | 0.103515625 |
| 0.5 | 0.5 | 0.5 |
| 0.75 | 0.896484375 | 0.896484375 |
| 0.9 | 0.9914399999999998 (0x3fefb9e060fe4797) | 0.9914400000000017 (0x3fefb9e060fe47a8) |
| 1 | 1.0 | 1.0 |
| -0.5 | -2.375 | -2.375 |
| 1.5 | 3.375 | 3.375 |

## V14. Table de sinus (R17)

Entrées : `table[0] = 0.0f`, `table[1] = 9.58738E-5f (0x38c90fdb)`, `table[8192] = 0.70710677f (0x3f3504f3)`, `table[16384] = 1.0f`, `table[32768] = 1.2246469E-16f (0x250d3132)`, `table[49152] = -1.0f`. Facteur : `65536 / (2*pi) = 10430.378350470453` ; `10430.378f = 10430.3779296875`. Seuil de saturation de l'indice via `int` : |x| > 205887.41614566068.

Candidats : FI = entrée `float`, indice `(int)(x * 10430.378f) & 65535`, cosinus `(int)(x * 10430.378f + 16384f) & 65535` ; DL = entrée `double`, indice via `long` puis masque ; DI = entrée `double`, indice via `int`.

| x | sin exact | sin FI | sin DL | sin DI | cos exact | cos FI | cos DL |
|---|---|---|---|---|---|---|---|
| 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 1.0 | 1.0 | 1.0 |
| -0.0 | -0.0 | 0.0 | 0.0 | 0.0 | 1.0 | 1.0 | 1.0 |
| pi/2 | 1.0 | 1.0 | 1.0 | 1.0 | 6.12e-17 | 1.2246469E-16 | 1.2246469E-16 |
| -pi/2 | -1.0 | -1.0 | -1.0 | -1.0 | 6.12e-17 | 0.0 | 0.0 |
| pi | 1.22e-16 | 1.2246469E-16 | 1.2246469E-16 | 1.2246469E-16 | -1.0 | -1.0 | -1.0 |
| 1.0 | 0.8414709848078965 | 0.8414514 | 0.8414514 | 0.8414514 | 0.5403023058681398 | 0.54033285 | 0.54033285 |
| -1.0 | -0.8414709848078965 | -0.8414514 | -0.8414514 | -0.8414514 | 0.5403023058681398 | 0.54025215 | 0.54025215 |
| 0.5 | 0.479425538604203 | 0.47940964 | 0.47940964 | 0.47940964 | 0.8775825618903728 | 0.87759125 | 0.87759125 |
| -0.5 | -0.479425538604203 | -0.47940964 | -0.47940964 | -0.47940964 | 0.8775825618903728 | 0.8775453 | 0.8775453 |
| 205887.0 | -0.40423807835004383 | -0.41642955 | -0.40427697 | -0.40427697 | 0.9146538011793663 | -9.58738E-5 | 0.9146366 |
| 205888.0 | 0.551243868984453 | -9.58738E-5 | 0.55117816 | -9.58738E-5 | 0.8343441717343337 | -9.58738E-5 | 0.8343876 |
| -205888.0 | -0.551243868984453 | 0.0 | -0.55117816 | 0.0 | 0.8343441717343337 | 0.8448536 | 0.8343876 |
| 1e6 | -0.34999350217129294 | -9.58738E-5 | -0.35003576 | -9.58738E-5 | 0.9367521275331447 | -9.58738E-5 | 0.93673635 |
| 1e10 | -0.4875060250875107 | -9.58738E-5 | -0.48755017 | -9.58738E-5 | 0.873119622676856 | -9.58738E-5 | 0.873095 |
| NaN | NaN | 0.0 | 0.0 | 0.0 | NaN | 0.0 | 0.0 |

Sonde P17, lacet 37° (radians `0.6457718232379019` en `double`, `0.6457718f` en `float`) : sinus exact `0.6018150231520483`, cosinus exact `0.7986355100472928` ; table (tous candidats) `0.6017657` et `0.7986727` ; sinus exact arrondi en `float` `0.60181504` et `0.7986355`.

## V15. Normalisation d'angle (R20, candidat, `double`)

| v | résultat |
|---:|---:|
| 0.0 | 0.0 |
| 179.9 | 179.9 |
| 180.0 | -180.0 |
| -180.0 | -180.0 |
| -180.1 | 179.9 |
| 359.5 | -0.5 |
| 540.0 | -180.0 |
| 720.0 | 0.0 |
| -190.0 | 170.0 |
| -0.0 | -0.0 |
| 1e10 | -80.0 |
| NaN | NaN |

Commande oracle : P10.

## V16. Puissances de deux (R21)

| v | plus petite puissance de deux >= v | ceilLog2 |
|---:|---:|---:|
| 1 | 1 | 0 |
| 2 | 2 | 1 |
| 3 | 4 | 2 |
| 4 | 4 | 2 |
| 5 | 8 | 3 |
| 16 | 16 | 4 |
| 17 | 32 | 5 |
| 31 | 32 | 5 |
| 32 | 32 | 5 |
| 33 | 64 | 6 |
| 2^30 | 2^30 | 30 |
| 2^30 + 1 | hors domaine (le calcul naïf déborde en -2^31) | 31 |

## V17. Sélection pondérée (R23)

Poids `[3, 0, 5, 2]`, total 10 : `r = 0, 1, 2 -> 0` ; `r = 3..7 -> 2` ; `r = 8, 9 -> 3`. L'entrée de poids 0 n'est jamais retenue. Liste `[7]` : entrée 0 sans tirage. Liste `[0, 0]` : aucun résultat, aucun tirage.

## V18. Boîtes alignées (R40 à R42, S jusqu'à P41)

| Cas | Boîte a | Boîte b | intersecte |
|---|---|---|---|
| face commune en x | [0, 0.5] x [0, 1] x [0, 1] | [-1, 0] x [0, 1] x [0, 1] | non |
| recouvrement | [0, 0.5] x [0, 1] x [0, 1] | [-1, 0.0001] x [0, 1] x [0, 1] | oui |
| arête commune | [0, 1]^3 | [1, 2] x [1, 2] x [0, 1] | non |
| incluse | [0, 1]^3 | [0.25, 0.75]^3 | oui |
| identiques | [0, 1]^3 | [0, 1]^3 | oui |
| épaisseur nulle à l'intérieur | [0, 1]^3 | [0.5, 0.5] x [0, 1] x [0, 1] | oui |
| épaisseur nulle sur la face | [0, 1]^3 | [1, 1] x [0, 1] x [0, 1] | non |

Contenance d'un point dans `[0, 1]^3` : `(0, 0, 0)` oui ; `(1, 0.5, 0.5)` non ; `(0.999999, 0.5, 0.5)` oui ; `(-0.0, 0.5, 0.5)` oui.

Opérations sur `[0, 1]^3` : déplacement de `(-16, 0, 16)` -> `[-16, -15] x [0, 1] x [16, 17]` ; gonflement de 0.5 -> `[-0.5, 1.5]^3` ; extension dirigée par `(-2, 0, 3)` -> `[-2, 1] x [0, 1] x [0, 4]`.

## V19. Longueur de vecteur (R39)

`(dx*dx + dy*dy) + dz*dz` et `dx*dx + (dy*dy + dz*dz)` diffèrent sur 23,7 % d'un échantillon aléatoire de 10^6 triplets dans `[0, 100)^3` ; pour `(0.1, 0.2, 0.3)` les deux donnent `0.14 (0x3fc1eb851eb851ec)`.

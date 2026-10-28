# Utilitaires mathématiques et coordonnées

Statut : brouillon
Jalon : M1
Risque : A

## Périmètre

Couvre le module `engine/core` :

- fonctions numériques de base : arrondis vers entier, division et modulo par défaut, bornage, interpolations, fondu quintique, table de sinus, angles, puissances de deux, tirage entier borné, sélection pondérée, hachage de position ;
- coordonnées : position de bloc, de chunk, de section, de cellule de biome, de région Anvil, leurs conversions et empaquetages ;
- directions, axes, rotations et miroirs ;
- géométrie de base : vecteur 3D double et boîte alignée sur les axes (AABB).

Ne couvre pas : les générateurs aléatoires eux-mêmes (`core-random`), le bruit et les fonctions de densité (`worldgen-*`), la transformation complète d'un gabarit de structure, le lancer de rayon, la collision des entités, le format NBT. Ces specs consomment les règles ci-dessous.

Les vecteurs de test sont dans `core-math-vectors.md`.

## Sources

Politique de niveau de preuve propre à cette spec :

- D : valeur lue dans les données extraites de 26.3 (`.cache/reference/26.3/`).
- C : conséquence de la sémantique publique de Java (JLS 17 et suivantes, Javadoc de `java.lang.Math`), du runtime déclaré par la version (Java 25), ou d'un calcul exécuté pour cette spec (Java 21.0.10, Python 3.14.7).
- S : tout ce qui vient des rapports locaux (ADR 0002, point 2b) ou de documentation publique non encore recoupée par l'oracle.
- M : aucune règle n'est M à ce jour (oracle non exécuté).

Sources utilisées :

| Réf. | Source |
|---|---|
| [V] | `.cache/reference/26.3/version.json` : `world_version` 5023, `java_version` 25 |
| [DT] | `resources/data/minecraft/dimension_type/{overworld,overworld_caves,the_nether,the_end}.json` |
| [NS] | `resources/data/minecraft/worldgen/noise_settings/overworld.json` (`noise.min_y`, `noise.height`) |
| [BL] | `generated/reports/blocks.json` (propriétés `facing`, `axis`, `rotation`) |
| [CMD] | `generated/reports/commands.json` (arguments de `/place template`, `/random`, `/execute`, `/forceload`) |
| [ST] | `resources/data/minecraft/structure/**/*.nbt` (1 511 gabarits, dont `igloo/top.nbt`) |
| [R26] | Rapport Overworld 26.3, chapitres 1.1, 1.2, 7.2 et section "Uniforme entier inspecté" (indices seulement) |
| [R121] | Rapport génération 1.21.1, section 3.1 (indices seulement) |
| [RA] | Rapport architecture, encadré sur les coordonnées négatives (indice) |
| [W-DT] | minecraft.wiki, "Java Edition protocol/Data types", type Position |
| [W-PK] | minecraft.wiki, "Java Edition protocol/Packets", Player Action (champ Face) et Update Section Blocks |
| [W-CF] | minecraft.wiki, "Chunk format" (References, YZX, PostProcessing) |
| [W-RF] | minecraft.wiki, "Region file format" |
| [W-CO] | minecraft.wiki, "Coordinates" et "Direction" (axes, boussole, lacet) |
| [W-PA] | minecraft.wiki, "Painting" (tag `facing`) |
| [W-PL] | minecraft.wiki, "Commands/place" (valeurs de rotation et miroir) |
| [W-TS] | minecraft.wiki, "Target selectors" (volume `dx dy dz`) |
| [W-WB] | minecraft.wiki, "World border" |
| [JD] | Javadoc communautaire publique des mappings Fabric Yarn 1.21.11, utilitaires mathématiques : inventaire des fonctions et descriptions en prose seulement (aucun nom repris ici ; voir Questions ouvertes, Q0) |

## Règles

### A. Arithmétique de base

- **R1 (C)** Le jeu tourne sur Java 25 [V]. Toute arithmétique `int` est en complément à deux sur 32 bits, `long` sur 64 bits, avec débordement silencieux. Le décalage `>>` est arithmétique (propage le signe), `>>>` logique. La conversion d'un `float` ou `double` vers `int` ou `long` tronque vers zéro, sature aux bornes du type et donne 0 pour NaN.
- **R2 (C)** Depuis Java 17, toute opération `float` et `double` est IEEE 754 stricte : addition, multiplication, division, `sqrt`, `floor`, `ceil` donnent le même résultat bit à bit sur toute plate-forme. Le moteur reproduit exactement l'ordre des opérations et la précision (`float` ou `double`) de chaque formule : `a + t*(b-a)` et `(1-t)*a + t*b` ne sont pas interchangeables (R11).
- **R3 (C)** Les fonctions transcendantes non strictes de Java (`sin`, `cos`, `atan2`, `pow`, `exp`, `log`) peuvent différer d'un ulp selon la plate-forme. Le moteur n'utilise que les variantes strictes pour toute valeur qui influence l'état du monde, sauf spec consommatrice contraire.

### B. Arrondis vers entier

- **R4 (C, contrat ; S hors domaine)** `floor(x)` pour `x` `double` ou `float` rend l'`int` égal au plus grand entier inférieur ou égal à `x`, pour tout `x` fini tel que `-2^31 <= floor(x) <= 2^31 - 1`. `floor(-0.0) = 0`, `floor(-1e-300) = -1`, `floor(-0.5) = -1`. Hors de ce domaine (|x| >= 2^31, infinis) et pour NaN, le résultat du jeu est S : les deux comportements candidats (V1 du fichier de vecteurs) coïncident pour NaN (0) et divergent pour `x < -2^31` (saturation à `-2^31` ou repli à `2^31 - 1`). Le moteur ne doit jamais dépendre de ce cas ; les appelants bornent leurs entrées.
- **R5 (C)** `floor(float)` s'applique à la valeur `float` déjà arrondie : `0.99999999` converti en `float` vaut `1.0f`, donc `floor` donne 1 alors que `floor(double)` donne 0. Le choix de la variante `float` ou `double` est imposé par la spec consommatrice.
- **R6 (C, contrat ; S hors domaine)** `ceil(x)` est le plus petit entier supérieur ou égal à `x`, mêmes domaine et réserve que R4 (`ceil(-0.5) = 0`, `ceil(15.999999999999998) = 16`).
- **R7 (C)** `floorLong(x)` (plancher vers `long`) suit R4 avec le domaine `long`. `frac(x) = x - floorLong(x)` en `double` ; attention, `frac(-1e-20) = 1.0` exactement (arrondi), donc `frac` n'est pas toujours strictement inférieur à 1.

### C. Division entière et modulo positif

- **R8 (C)** `floorDiv(a, b)` est le quotient arrondi vers moins l'infini ; `floorMod(a, b) = a - b * floorDiv(a, b)` a le signe de `b`. `floorDiv(MIN_VALUE, -1) = MIN_VALUE` (débordement), `b = 0` lève une erreur arithmétique. La division Java `/` tronque vers zéro et ne doit jamais servir à convertir une coordonnée ([R121], [RA] ; vecteurs V3).
- **R9 (C)** Pour `n` dans 0..31 et tout `int` `v` : `v >> n == floorDiv(v, 2^n)` et `v & (2^n - 1) == floorMod(v, 2^n)`. Vérifié exhaustivement sur les 2^32 valeurs pour n = 4 et n = 5 (0 écart). Toutes les conversions de coordonnées (R17 à R24) utilisent ces décalages et masques.
- **R10 (S)** Modulo positif réel (`float`, `double`) : deux formules candidates divergent sur de petites valeurs négatives : `a - floor(a/b)*b` donne `360.0` pour `(-1e-20, 360)`, `((a % b) + b) % b` donne `0.0` (V10). Le jeu possède des modulos réels [JD] dont la formule n'est pas connue. Chaque spec consommatrice nomme la formule ; ce module fournit les deux sous des noms distincts tant que la sonde P10 n'a pas tranché.

### D. Bornage, interpolation, correspondance

- **R11 (C pour les formes ; S pour celle du jeu)** Interpolation linéaire `lerp(t, a, b)`. Forme A : `a + t*(b - a)`. Forme B : `(1 - t)*a + t*b`. Sur 10^6 tirages aléatoires, A et B diffèrent dans 37 % des cas en `double` et 43 % en `float` ; la forme A ne rend pas exactement `b` pour `t = 1` dans 8,9 % des cas (exemple : `t = 1, a = 1e20, b = 1` donne 0 en forme A, 1 en forme B). La forme utilisée par le jeu est S (sonde P11). `t` hors de [0, 1] extrapole sans bornage. Des variantes `float` et `double` existent [JD] ; elles ne sont pas interchangeables (V11).
- **R12 (S)** `lerp2(tx, ty, v00, v10, v01, v11)` interpole d'abord selon x (deux fois), puis selon y ; `lerp3` interpole selon x (quatre fois), puis y (deux fois), puis z. Les coins sont nommés `v<x><y><z>` avec x le chiffre le plus à gauche, ordre des paramètres : `000, 100, 010, 110, 001, 101, 011, 111` [JD]. L'ordre des axes change le résultat dans 55 % des cas (calcul, forme A) : il est donc observable et S tant que P12 n'a pas tranché.
- **R13 (C, contrat)** `inverseLerp(v, a, b) = (v - a) / (b - a)`, sans bornage. Pour `a == b` : NaN si `v == a`, infini signé sinon. Le jeu fournit cette fonction avec la propriété `inverseLerp(lerp(t, a, b), a, b) == t` à l'arrondi près [JD] ; l'absence de garde contre `a == b` est S.
- **R14 (S)** `map(v, a0, a1, b0, b1) = lerp(inverseLerp(v, a0, a1), b0, b1)` sans bornage ; `clampedMap` est la même valeur bornée entre `b0` et `b1` [JD]. Composition exacte (et donc arrondi) S. `clampedLerp(t, a, b)` rend `a` si `t < 0`, `b` si `t > 1`, `lerp` sinon (S).
- **R15 (C pour les candidats ; S pour le jeu)** Bornage `clamp(v, lo, hi)` sur `int`, `long`, `float`, `double`. Pour `lo <= hi` et `v` non NaN, toutes les formes donnent `lo` si `v < lo`, `hi` si `v > hi`, `v` sinon. Les formes divergent sur (V12) :
  - `-0.0` et `0.0` : forme "min/max" rend `0.0` pour `clamp(-0.0, 0.0, 1)`, forme "comparaisons" rend `-0.0` ;
  - NaN en borne : forme "min/max" propage NaN, forme "comparaisons" l'ignore ;
  - `lo > hi` : `clamp(20, 10, 0)` vaut 10 (min/max) ou 0 (comparaisons) ; le `Math.clamp` de Java lève une exception.
  `clamp(NaN, lo, hi)` vaut NaN dans toutes les formes. Le moteur ne doit jamais appeler `clamp` avec `lo > hi` ; la forme du jeu est S (P15).
- **R16 (S)** Fondu quintique `fade(t) = 6t^5 - 15t^4 + 10t^3`, calculé en `double` sous la forme `t*t*t*(t*(t*6 - 15) + 10)`. La forme développée diffère au dernier bit pour `t = 0.1` et `t = 0.9` (V13). Valeurs exactes : `fade(0) = 0`, `fade(0.5) = 0.5`, `fade(1) = 1`. L'existence de la fonction est [JD] ; sa forme d'évaluation est S et sera levée par la spec du bruit (comparaison de terrain, M4).

### E. Trigonométrie et angles

- **R17 (S, factorisation C)** Sinus et cosinus rapides par table [JD] :
  - table de 65 536 entrées `float`, entrée `i` = arrondi `float` de `sin(2*pi*i/65536)` calculé en `double`. Calcul : les trois écritures testées de cette formule (`Math` et `StrictMath`, deux ordres de produit) donnent les 65 536 mêmes `float` (C) ;
  - indice = `trunc(x * K) & 65535` avec `K = 65536 / (2*pi) = 10430.378350470453` en `double`, ou `10430.378f` (= 10430.3779296875) en `float` ;
  - cosinus : même table, indice décalé de `+16384` avant troncature ;
  - erreur absolue maximale mesurée face au sinus exact : 9,59e-5 (C, 2·10^6 points dans [-20, 20]).
  Restent S : la taille (65 536), l'entrée `float` ou `double`, la troncature via `int` (saturation au-delà de |x| > 205 887,416) ou via `long` (période correcte pour tout `x`), l'ordre "multiplier puis ajouter 16384". Cas limites mesurés sur les candidats (V14) : `sin(-x) == -sin(x)` sauf pour l'indice 32 768, mais `cos(-x) != cos(x)` en général (`cos(1.0) = 0.54033285`, `cos(-1.0) = 0.54025215`) ; `sin(NaN) = 0`.
- **R18 (S)** Arc tangente approchée : le jeu possède une approximation de `atan2(y, x)` distincte de la fonction exacte [JD], dont la formule n'est pas dans les sources autorisées. Le moteur n'en fournit pas tant que P18 n'a pas mesuré son écart avec `StrictMath.atan2`. Si l'écart est nul sur l'échantillon, on utilise `StrictMath.atan2`.
- **R19 (S)** Racine carrée inverse rapide : présente mais dépréciée [JD] ; aucune utilisation connue. Le moteur ne l'implémente pas sans spec consommatrice.
- **R20 (S)** Normalisation d'angle en degrés vers `[-180, 180)` [JD] : candidat `r = v % 360 ; si r >= 180 alors r -= 360 ; si r < -180 alors r += 360`. Donne `180 -> -180`, `-180 -> -180`, `540 -> -180`, `-190 -> 170`, `-0.0 -> -0.0` (V15). Variantes `int`, `float`, `double` ; la variante `float` calcule en `float`.

### F. Entiers, aléa et poids

- **R21 (C)** Puissances de deux : `smallestPow2(v)` est la plus petite puissance de deux `>= v` pour `1 <= v <= 2^30` ; `ceilLog2(v)` = nombre de bits pour coder `v` valeurs distinctes, soit `ceil(log2(v))` (V16 : 1->0, 2->1, 3->2, 17->5, 33->6). Pour `v <= 0` ou `v > 2^30` : non spécifié (S), interdit.
- **R22 (S)** Tirage entier borné inclusif `nextIntBetween(rng, min, max)` : si `min >= max`, rend `min` sans consommer l'aléa ; sinon rend `min + rng.nextInt(max - min + 1)`, qui consomme exactement un tirage borné de `core-random` ([R26], confirmé en prose par [JD] pour `max < min`). Le cas `max - min + 1` débordant (`min = MIN_VALUE, max = MAX_VALUE`) est S et interdit. Ne pas confondre avec une seconde fonction de [JD] qui rejette un intervalle vide par une exception.
- **R23 (S)** Sélection pondérée [R26] : soit la liste ordonnée des entrées de poids effectif `w > 0` (les poids nuls ou négatifs sont exclus) et `W` leur somme. Si aucune entrée : aucun résultat, aucun tirage. Si une seule : elle est rendue sans tirage. Sinon `r = rng.nextInt(W)` puis on parcourt la liste dans l'ordre d'origine en retranchant chaque poids ; l'entrée retenue est la première pour laquelle `r` devient négatif. Exemple poids `[3, 0, 5, 2]` : `r` 0..2 -> indice 0, 3..7 -> 2, 8..9 -> 3. Ce contrat vaut pour le butin ; les listes pondérées de la génération ont peut-être leur propre variante (la règle "une seule entrée sans tirage" en particulier) : chaque spec consommatrice confirme.
- **R24 (S)** Hachage de position en graine `positionSeed(x, y, z) -> long` : le jeu dérive une graine déterministe d'une position de bloc (décalages aléatoires des modèles, de certaines formes de collision) [JD]. La formule n'apparaît dans aucune source autorisée. L'interface existe dans ce module, sans implémentation, jusqu'à la sonde P24.

### G. Coordonnées et empaquetages

- **R25 (D)** Overworld : `min_y = -64`, `height = 384`, `logical_height = 384` [DT], identiques dans `noise_settings` [NS]. Blocs constructibles `-64 <= y <= 319` (`min_y + height - 1`, C). Sections `-4..19`, soit 24 sections, indice de liste `sy - (min_y >> 4)` (C). `overworld_caves` : mêmes valeurs ; Nether : 0 et 256 (hauteur logique 128) ; End : 0 et 256 [DT].
- **R26 (S)** Domaine horizontal valide des blocs : `-30 000 000 <= x, z < 30 000 000` ; bordure par défaut à 29 999 984 [W-WB]. Exactitude des bornes (inclusive ou non) : P26.
- **R27 (S, conversion C)** La position de bloc contenant un point `(x, y, z)` `double` est `(floor(x), floor(y), floor(z))` [W-CO] ; un point à `-0.0001` est dans le bloc `-1`, un point à `-0.0` dans le bloc 0.
- **R28 (C)** Bloc vers chunk : `cx = x >> 4`, `cz = z >> 4` ; local `x & 15`, `z & 15` (R9). Section : `(x >> 4, y >> 4, z >> 4)`, local `y & 15`. Cellule de biome (quart) : `v >> 2`. Chunk vers premier bloc : `cx << 4` (déborde au-delà de |cx| >= 2^27, hors domaine R26). Région : `rx = cx >> 5`, local `cx & 31` ; en blocs, `x >> 9`.
- **R29 (S)** Position de bloc empaquetée sur 64 bits [W-DT] : x sur les 26 bits de poids fort (décalage 38), z sur 26 bits (décalage 12), y sur les 12 bits de poids faible ; chaque champ masqué en complément à deux ; décodage par décalages arithmétiques (`x = v >> 38`, `z = (v << 26) >> 38`, `y = (v << 52) >> 52`). Plages représentables : x, z dans `[-2^25, 2^25 - 1]`, y dans `[-2048, 2047]` ; une valeur hors plage est repliée silencieusement (V5, dernière ligne). Format réseau documenté ; l'identité avec la représentation interne du jeu est S et ne compte que si un ordre d'itération en dépend.
- **R30 (S)** Position de chunk empaquetée : x dans les 32 bits faibles (non signés), z dans les 32 bits forts [W-CF] ; `(-1, -1)` donne `-1`, `(1, -1)` donne `0xFFFFFFFF00000001`. Observable dans les références de structures des fichiers `.mca` (P30).
- **R31 (S)** Position de section empaquetée (réseau) : x sur 22 bits décalé de 42, z sur 22 bits décalé de 20, y sur les 20 bits faibles [W-PK].
- **R32 (S)** Trois ordres d'empaquetage local distincts coexistent, à ne pas confondre :
  - indice dans le tableau d'états d'une section : `(y & 15) << 8 | (z & 15) << 4 | (x & 15)` (ordre YZX) [W-CF] ;
  - `PostProcessing` des chunks : `short` de forme `0ZYX`, soit `z << 8 | y << 4 | x` [W-CF] ;
  - entrée réseau de mise à jour de section : `x << 8 | z << 4 | y` (après l'identifiant d'état décalé de 12) [W-PK].
  L'ordre des 64 cellules de biome d'une section n'est pas documenté dans les sources consultées (Q32).
- **R33 (S)** Fichiers de région Anvil [W-RF] : fichier `r.<rx>.<rz>.mca` avec `rx = cx >> 5` ; 32 x 32 chunks ; secteurs de 4 096 octets ; en-tête de deux secteurs ; indice `i = (cx & 31) + 32 * (cz & 31)` ; entrée de localisation (3 octets de décalage en secteurs, 1 octet de taille) à l'octet `4i`, horodatage à l'octet `4096 + 4i`. Exemple publié : le chunk `(30, -3)` est dans la région `(0, -1)`.

### H. Directions, axes, rotations, miroirs

- **R34 (D)** Axes : `x`, `y`, `z` ; axe horizontal : `x`, `z` [BL].
- **R35 (S)** Six directions, identifiant canonique et vecteur unitaire [W-PK] : 0 `down` (0,-1,0), 1 `up` (0,1,0), 2 `north` (0,0,-1), 3 `south` (0,0,1), 4 `west` (-1,0,0), 5 `east` (1,0,0). Boussole [W-CO] : +X est, +Z sud. Opposé : `id ^ 1` (C à partir de cet ordre).
- **R36 (S)** Directions horizontales, identifiant 2D [W-PA] : 0 `south`, 1 `west`, 2 `north`, 3 `east`. Lacet [W-CO] : sud 0°, ouest 90°, nord 180°, est -90°. Lacet vers direction horizontale, candidat : `floor(yaw / 90 + 0.5) & 3` ; la frontière à 45° tombe à l'ouest, à -45° au sud (S, P36).
- **R37 (D)** L'ordre des valeurs d'une propriété de bloc est propre à la propriété et ne suit pas R35 : `facing` des blocs à six faces (`dispenser`, `observer`, `barrel`) est `north, east, south, west, up, down` ; `facing` des escaliers est `north, south, west, east` ; `rotation` des panneaux debout est `0..15` [BL]. Cet ordre détermine les identifiants d'états et relève de la spec des états de blocs.
- **R38 (D pour les noms ; S pour les effets)** Rotations : `none`, `clockwise_90`, `180`, `counterclockwise_90` ; miroirs : `none`, `left_right`, `front_back` [W-PL], arguments de `/place template` [CMD]. Effet candidat sur une direction horizontale : `clockwise_90` envoie nord sur est, est sur sud, sud sur ouest, ouest sur nord ; `180` donne l'opposé ; `counterclockwise_90` l'inverse de `clockwise_90` ; `left_right` échange nord et sud ; `front_back` échange est et ouest ; `up` et `down` sont invariants. Sur la propriété `rotation` 0..15 : rotation horaire de 90° = `+4 mod 16` (S). Tout l'effet est S (P38).

### I. Géométrie

- **R39 (C, contrat)** Vecteur 3D : trois `double` ; addition, soustraction, produit par un scalaire composante par composante (R2). Longueur au carré `(dx*dx + dy*dy) + dz*dz` dans cet ordre (l'ordre change le dernier bit dans 24 % des cas, calcul) ; longueur = `sqrt` de cette valeur. Ordre exact du jeu : S, à confirmer par les specs consommatrices (distances de spawn, portée). L'égalité de deux vecteurs est l'égalité exacte des composantes ; le traitement de `-0.0` et NaN par le jeu n'est pas observable et reste libre.
- **R40 (S)** Boîte alignée `[minX, maxX] x [minY, maxY] x [minZ, maxZ]` en `double`. Construite à partir de deux coins quelconques, elle se normalise (min et max par axe). La boîte d'un bloc entier `(x, y, z)` est `[x, x+1] x [y, y+1] x [z, z+1]`.
- **R41 (S)** Intersection de deux boîtes : vraie si et seulement si, sur chacun des trois axes, `a.min < b.max` et `a.max > b.min` (inégalités strictes : deux boîtes qui se touchent par une face ne s'intersectent pas). Contenance d'un point : `min <= p < max` sur chaque axe. P41 mesure la frontière par le sélecteur de volume [W-TS].
- **R42 (S)** Opérations : déplacement (ajout d'un vecteur aux deux coins, C) ; gonflement symétrique de `d` (min - d, max + d ; `d` négatif contracte) ; extension dirigée par un vecteur `v` (par axe, `v < 0` étend le min, `v > 0` le max). Une boîte contractée au-delà de zéro (min > max) n'est pas renormalisée (S).

## Entrées / sorties

| Grandeur | Type | Plage valide | Hors plage |
|---|---|---|---|
| Coordonnée de bloc x, z | `int` | `[-30 000 000, 30 000 000)` (R26) | refus en amont |
| Coordonnée de bloc y (Overworld) | `int` | `[-64, 319]` | refus pour placement ; calcul permis |
| Bloc empaqueté | `long` | x, z sur 26 bits, y sur 12 bits | repli silencieux (R29) |
| Chunk x, z | `int` | `[-1 875 000, 1 875 000)` | idem |
| Section y (Overworld) | `int` | `[-4, 19]` | idem |
| Région x, z | `int` | `[-58 594, 58 593]` | idem |
| Arguments `floor`, `ceil` | `float`, `double` | résultat dans `int` | S (R4) |
| Angles | `float` ou `double`, radians pour la table, degrés pour R20 | tous | voir R17, R20 |
| Paramètre d'interpolation `t` | `float` ou `double` | tous (extrapolation) | aucun bornage implicite |

## Cas limites

| Cas | Attendu | Règle |
|---|---|---|
| `x = -1` | chunk -1, local 15 ; région -1, local 31 | R28 |
| `x = -16` / `-17` | chunk -1 local 0 / chunk -2 local 15 | R28 |
| `x = 15` / `16` / `17` | chunk 0 local 15 / chunk 1 local 0 / chunk 1 local 1 | R28 |
| `x = 511` / `512` / `-513` | région 0 / 1 / -2 (en blocs, `>> 9`) | R28 |
| `x = MIN_VALUE` | chunk -134 217 728, local 0 (hors domaine R26) | R9 |
| `floor(-0.0)`, `floor(NaN)` | 0, 0 (NaN : S) | R4 |
| `floor(-2^31 - 0.5)` | S : `-2^31` ou `2^31 - 1` selon candidat | R4 |
| `clamp(-0.0, 0.0, 1.0)` | S : `0.0` ou `-0.0` | R15 |
| `clamp(NaN, 0, 1)` | NaN | R15 |
| `clamp(20, 10, 0)` | S : 10 ou 0 ; usage interdit | R15 |
| `lerp(1, 1e20, 1)` | S : 0 (forme A) ou 1 (forme B) | R11 |
| `inverseLerp(1, 1, 1)` | NaN | R13 |
| `sin` par table, `x = 205 888.0` | S : `-9.587e-5` (indice saturé) ou `0.55117816` | R17 |
| `cos(-1.0)` par table | `0.54025215`, différent de `cos(1.0)` | R17 |
| `wrap(180.0)`, `wrap(540.0)` | `-180.0` | R20 |
| Bloc `(-1, -64, -1)` | chunk (-1,-1), section -4 (indice 0), indice YZX 255 | R25, R32 |
| Bloc empaqueté `(2^25, 2048, -2^25 - 1)` | relu `(-2^25, -2048, 2^25 - 1)` | R29 |
| Chunk `(-1, -1)` empaqueté | `0xFFFFFFFFFFFFFFFF` | R30 |
| Boîtes `[0, 0.5]` et `[-1, 0]` sur x | pas d'intersection (S) | R41 |

## Vecteurs de test

Voir `core-math-vectors.md`. Les vecteurs C sont calculés en Java 21.0.10 par les opérateurs standard du langage (reproductibles dans `jshell`). Les vecteurs S portent le numéro de la sonde qui les confirmera. Aucun vecteur M n'existe encore.

## Sondes de l'oracle

Toutes sont réalisables avec l'interface admise sans 2a-bis (ADR 0002), monde de test plat, `randomTickSpeed 0`, `/tick freeze`. Aucune n'a été exécutée.

| Sonde | Règle | Procédure | Critère |
|---|---|---|---|
| P4 | R4, R27 | `/execute positioned -0.0001 70 -0.0001 run setblock ~ ~ ~ minecraft:stone`, puis `/execute if block -1 70 -1 minecraft:stone` ; idem avec `-0.0`, `15.99999`, `-16.00001` | blocs `-1`, `0`, `15`, `-17` |
| P9 | R28 | `/forceload add X Z` puis `/forceload query X Z` pour X, Z dans {-17, -16, -1, 0, 15, 16, 17, 511, 512, -513} ; le message cite les coordonnées du chunk | chunk = `X >> 4` |
| P10 | R10, R20 | `/tp @s 0 70 0 <yaw> 0` avec yaw dans {180, -180, 540, -190, 359.5}, puis `/data get entity @s Rotation[0]` | valeur stockée comparée aux candidats V10, V15 (la normalisation au stockage est elle-même à établir) |
| P11 | R11 | datapack : `noise_settings` dont `final_density` est une fonction `minecraft:lerp` de constantes `alpha = 0.3`, `first = -1.5`, `second = 3.5` ; générer un chunk et lire un bloc | forme A : densité 0, air ; forme B : densité > 0, pierre (V11, en `float` comme en `double`). Sonde indirecte : elle tranche la forme de l'opérateur de densité, présumée partagée |
| P12 | R12, R16 | comparaison de terrain M4 sur l'échantillon figé | bit à bit |
| P15 | R15 | datapack : fonction de densité `minecraft:clamp` d'une constante `-0.0` avec `min = 0.0` ; n'est observable que si un opérateur aval distingue le signe de zéro : à combiner avec une division (`minecraft:div` si son contrat le permet) | sinon, laisser R15 en contrat interne et interdire les cas divergents |
| P17 | R17 | `/execute rotated <yaw> 0 positioned 0 70 0 run summon minecraft:marker ^ ^ ^1`, puis `/data get entity @e[type=marker,limit=1] Pos` pour yaw dans {37, -37, 90, 1e6} | `|dx|` et `|dz|` comparés au sinus exact et aux candidats de table (V14 : pour 37°, exact 0.6018150231520483, table 0.6017657) ; tranche aussi la conversion degrés vers radians |
| P18 | R18 | `/execute positioned 0 70 0 facing <x> <y> <z> run tp @s ~ ~ ~ ~ ~` puis lecture de `Rotation` pour une grille de cibles | écart à `StrictMath.atan2` converti en degrés `float` |
| P22 | R22 | `/random reset <seq> 7`, `/random value 5..5 <seq>`, `/random value 1..1000000 <seq>` ; refaire sans l'appel `5..5` | même dernière valeur = aucun tirage consommé. Présume que la commande partage le tirage borné |
| P24 | R24 | bambou (forme de collision décalée par position) posé sur une grille de positions ; entité sans IA poussée contre chaque tige par `Motion` ; lecture de `Pos` finale | ajuster une formule candidate sur au moins 64 positions dont négatives ; coûteuse, jalon M5 au plus tôt |
| P26 | R26 | `/setblock 29999999 64 0 stone`, `/setblock 30000000 64 0 stone`, idem en `-30000000`, `-30000001` ; `/setblock 0 319 0`, `0 320 0`, `0 -64 0`, `0 -65 0` | succès ou erreur "hors du monde" |
| P30 | R30, R32, R33 | `/forceload add 480 -48` (chunk (30,-3)), `/save-all flush` ; lire `r.0.-1.mca` avec le lecteur indépendant de `tools/oracle` ; pour R30, `/place structure` d'un village sur une frontière de région puis lecture des références | fichier, indice 958, décalage 3832 ; longs de références conformes à R30 ; états d'une section posés par `/setblock` relus à l'indice YZX |
| P35 | R35, R36 | `/summon minecraft:item_frame ~ ~ ~ {Facing:<n>b}` pour n = 0..5, `/summon minecraft:painting ~ ~ ~ {facing:<n>b}` pour n = 0..3 ; lire `Rotation` | lacet et tangage conformes à la direction attendue (modulo 360) |
| P36 | R36 | client réel (M9) : joueur téléporté avec lacet 44.999, 45, -45, -45.001, pose d'un fourneau | `facing` du bloc posé |
| P38 | R38 | `/place template minecraft:igloo/top <pos> <rotation> <mirror>` pour les 12 combinaisons ; lire les blocs orientés par `/execute if block ... [facing=...]` ou `.mca` | table de passage des directions |
| P41 | R40, R41 | `/summon minecraft:armor_stand 0.25 64 0.5 {NoGravity:1b,Tags:["p"]}` (boîte x de 0.0 à 0.5) ; `/execute if entity @e[tag=p,x=-1,y=64,z=0,dx=0,dy=0,dz=0]` (volume x de -1 à 0) ; témoin avec `x = 0.2499` | non sélectionné = intersection stricte |

## Questions ouvertes

- **Q0** La source [JD] est une documentation communautaire publique dont les noms dérivent des métadonnées du JAR. Seuls l'inventaire des fonctions et des phrases de comportement en ont été retenus, sans aucun nom, et toutes les règles qui en dépendent sont S. Le propriétaire confirme que cet usage reste dans l'ADR 0002 (point 2a-bis en attente) ; sinon R12 à R14, R16 à R20 et R24 perdent leur seul indice d'existence et restent des contrats internes.
- **Q4** Comportement de `floor` hors du domaine `int` : non observable par commande (coordonnées bornées par R26). Rester sur l'interdiction.
- **Q11** Forme de `lerp` (A ou B) et précision : P11, puis confirmation par P12.
- **Q17** Paramètres de la table de sinus : P17. Si l'écart observé est nul face au sinus exact, la table n'est pas utilisée par ce chemin et R17 reste S jusqu'à une sonde de génération (géométrie des filons, M4).
- **Q24** Formule du hachage de position : P24, ou extraction de vecteurs si 2a-bis est accordé.
- **Q32** Ordre des 64 cellules de biome d'une section : lecture d'un `.mca` après `/fillbiome` sur une cellule unique (P30 étendue).
- **Q41** Si P41 montre une intersection inclusive, R41 bascule, et les specs de collision reprennent le contrat.
- Tant que les sondes ne sont pas exécutées, la spec ne peut pas passer à `figé`.

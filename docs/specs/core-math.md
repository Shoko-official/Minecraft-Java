# Utilitaires mathématiques et coordonnées

Statut : brouillon
Jalon : M1
Risque : A
Révision : 3 (prose alignée sur les formules après relecture du code, PR #9 : en cas d'écart, la formule écrite fait foi et la prose décrit ses limites)

## Périmètre

Couvre le module `engine/core` :

- fonctions numériques de base : arrondis vers entier, division et modulo, bornage, interpolations, fondu quintique, sinus par table, angles, puissances de deux, sélection pondérée ;
- coordonnées : position de bloc, de chunk, de section, de cellule de biome, de région Anvil, leurs conversions et empaquetages ;
- directions, axes, rotations et miroirs ;
- géométrie de base : vecteur 3D double et boîte alignée sur les axes (AABB).

Ne couvre pas : les générateurs aléatoires, le tirage entier borné et l'empreinte de position (propriétaire : `core-random`, voir R22 et R24), le bruit et les fonctions de densité (`worldgen-*`), la transformation complète d'un gabarit de structure, le lancer de rayon, la collision des entités, le format NBT.

Les identifiants de fonctions de cette spec sont des noms neutres propres au moteur. Ils ne présument rien des noms du jeu. Les vecteurs de test sont dans `core-math-vectors.md`.

## Sources

Politique de niveau de preuve propre à cette spec :

- D : valeur lue dans les données extraites de 26.3 (`.cache/reference/26.3/`).
- C : conséquence de la sémantique publique de Java (JLS 17 et suivantes, Javadoc de `java.lang.Math` et `java.lang.StrictMath`), du runtime déclaré par la version (Java 25), ou d'un calcul exécuté pour cette spec (Java 21.0.10, Python 3.14.7).
- S : tout ce qui vient des rapports locaux (ADR 0002, point 2b), de documentation publique non encore recoupée par l'oracle, ou d'un choix de contrat interne du moteur dont l'équivalent dans le jeu reste à établir. Toute règle S porte un comportement **provisoire** explicite, que les tests en aveugle prennent pour cible jusqu'à la levée par sonde.
- M : aucune règle n'est M à ce jour (oracle non exécuté).

Sources utilisées :

| Réf. | Source |
|---|---|
| [V] | `.cache/reference/26.3/version.json` : `world_version` 5023, `java_version` 25 |
| [DT] | `resources/data/minecraft/dimension_type/{overworld,overworld_caves,the_nether,the_end}.json` |
| [NS] | `resources/data/minecraft/worldgen/noise_settings/overworld.json` (`noise.min_y`, `noise.height`) |
| [BL] | `generated/reports/blocks.json` (propriétés `facing`, `axis`, `rotation`) |
| [CMD] | `generated/reports/commands.json` (types d'arguments de `/place template`, `/random`, `/execute`, `/forceload`) |
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
| [PN] | Implémentation de référence publique du bruit de Perlin amélioré (K. Perlin, 2002) : interpolation `a + t*(b - a)` et fondu `t*t*t*(t*(t*6 - 15) + 10)` |

Aucune documentation de mappings communautaires n'est utilisée (ADR 0002, point 2a-bis non autorisé).

## Règles

### A. Arithmétique de base

- **R1 (C)** Le jeu tourne sur Java 25 [V]. Toute arithmétique `int` est en complément à deux sur 32 bits, `long` sur 64 bits, avec débordement silencieux. Le décalage `>>` est arithmétique (propage le signe), `>>>` logique. La conversion d'un `float` ou `double` vers `int` ou `long` tronque vers zéro, sature aux bornes du type et donne 0 pour NaN.
- **R2 (C)** Depuis Java 17, toute opération `float` et `double` est IEEE 754 stricte : addition, multiplication, division, `sqrt`, `floor`, `ceil` donnent le même résultat bit à bit sur toute plate-forme. Le moteur reproduit exactement l'ordre des opérations et la précision (`float` ou `double`) de chaque formule : deux écritures algébriquement égales ne sont pas interchangeables (R11).
- **R3 (C)** Les fonctions transcendantes de `java.lang.Math` (`sin`, `cos`, `atan2`, `pow`, `exp`, `log`) peuvent différer d'un ulp selon la plate-forme ; celles de `java.lang.StrictMath` sont reproductibles. Le moteur n'utilise que les variantes strictes pour toute valeur qui influence l'état du monde, et les vecteurs de cette spec sont calculés avec elles.

### B. Arrondis vers entier

- **R4 (C, contrat ; S hors domaine)** `floorToInt(x)` pour `x` `double` ou `float` rend l'`int` égal au plus grand entier inférieur ou égal à `x`, pour tout `x` fini tel que le résultat tient dans `int`. `floorToInt(-0.0) = 0`, `floorToInt(-1e-300) = -1`, `floorToInt(-0.5) = -1`. Hors de ce domaine (|x| >= 2^31, infinis) et pour NaN, le comportement du jeu est S. Provisoire : plancher mathématique puis conversion saturante (candidat F1 de V1, NaN donne 0). Les deux candidats de V1 coïncident pour NaN et divergent pour `x < -2^31`. Le moteur ne doit jamais dépendre de ce cas ; les appelants bornent leurs entrées (Q4).
- **R5 (C)** La variante `float` s'applique à la valeur `float` déjà arrondie : `0.99999999` converti en `float` vaut `1.0f`, donc le plancher `float` donne 1 alors que le plancher `double` donne 0. Le choix de la variante est imposé par la spec consommatrice.
- **R6 (C, contrat ; S hors domaine)** `ceilToInt(x)` est le plus petit entier supérieur ou égal à `x`, mêmes domaine, réserve et provisoire que R4 (`ceilToInt(-0.5) = 0`, `ceilToInt(15.999999999999998) = 16`).
- **R7 (C)** `floorToLong(x)` suit R4 avec le domaine `long`. `fractionalPart(x) = x - floorToLong(x)` en `double`. Pour |x| < 2^63 le résultat est dans [0, 1] : `fractionalPart(3.00000000025E9) = 0.25` (domaine `long`, hors domaine `int`), et `fractionalPart(-1e-20) = 1.0` exactement (arrondi), donc la borne 1 peut être atteinte. Limite connue hors domaine : pour |x| >= 2^63, `floorToLong` sature et le résultat sort de [0, 1] (`fractionalPart(1e19) = 7.766279631452242E17`).

### C. Division entière et modulo

- **R8 (C)** `floorDiv(a, b)` et `floorMod(a, b)` ont la sémantique des fonctions de même nom de la bibliothèque standard Java : quotient arrondi vers moins l'infini, reste du signe de `b`. `floorDiv(MIN_VALUE, -1) = MIN_VALUE` (débordement), `b = 0` lève une erreur arithmétique. La division Java `/` tronque vers zéro et ne doit jamais servir à convertir une coordonnée ([R121], [RA] ; V3).
- **R9 (C)** Pour `n` dans 0..31 et tout `int` `v` : `v >> n == floorDiv(v, 2^n)` et `v & (2^n - 1) == floorMod(v, 2^n)`. Vérifié exhaustivement sur les 2^32 valeurs pour n = 4 et n = 5 (0 écart). Toutes les conversions de coordonnées (R25 à R33) utilisent ces décalages et masques.
- **R10 (S)** Modulo réel (`float`, `double`), contrat interne. Le moteur fournit deux fonctions distinctes : `realModFloor(a, b) = a - floor(a / b) * b` et `realModWrap(a, b) = ((a % b) + b) % b`. Elles divergent sur de petites valeurs négatives : `(-1e-20, 360)` donne `360.0` pour la première et `0.0` pour la seconde (V10). La variante `float` calcule entièrement en `float` (quotient, plancher reconverti en `float`, produit et différence) ; un calcul mixte promu en `double` donne un autre résultat : `realModFloor(-93950.03f, 0.74358845f) = 0.140625f` en `float` pur contre `0.1383352279663086` en mixte, `realModWrap(0.14161503f, 7.8575983f) = 0.14161491f` en `float` pur contre `0.14161503314971924` en mixte. Aucune n'est imposée : chaque spec consommatrice choisit, et l'existence d'un modulo réel dans le jeu comme sa formule sont à établir par les sondes de ces specs (aucune sonde M1 ne sépare les deux formes).

### D. Bornage, interpolation, correspondance

- **R11 (C pour les formes ; S pour celle du jeu)** Interpolation linéaire `interpolate(t, a, b)`, variantes `float` et `double` non interchangeables (V11). Forme A : `a + t*(b - a)`. Forme B : `(1 - t)*a + t*b`. **Provisoire : forme A**, celle de l'implémentation publique de référence du bruit de Perlin [PN]. Sur 10^6 triplets (`java.util.Random(1)`, `t` uniforme dans [0, 1), `a` et `b` uniformes dans [-100, 100)), A et B diffèrent dans 36,6 % des cas en `double` et 43,2 % en `float` ; la forme A ne rend pas exactement `b` pour `t = 1` dans 8,9 % des cas (exemple : `t = 1, a = 1e20, b = 1` donne 0 en forme A, 1 en forme B). `t` hors de [0, 1] extrapole sans bornage. Forme du jeu : P11.
- **R12 (S, contrat interne)** `bilinear(tx, ty, v00, v10, v01, v11)` et `trilinear(tx, ty, tz, v000, v100, v010, v110, v001, v101, v011, v111)`, coins notés `v<x><y><z>`. Provisoire : interpolations selon x d'abord, puis y, puis z, chacune par R11 ; soit `bilinear = interpolate(ty, interpolate(tx, v00, v10), interpolate(tx, v01, v11))`. Exemples dans l'ordre exact des paramètres ci-dessus : `bilinear(0.3, 0.1 ; 0.1, -0.9, 0.35, 0.77) = -0.1324` (y d'abord : `-0.13239999999999996`) ; `trilinear(0.3, 0.7, 0.9 ; 0.1, -0.9, 0.35, 0.77, -0.2, 0.6, 0.45, -0.33) = 0.1742` (z, puis y, puis x : `0.17420000000000002`) (V11). L'ordre des axes change le résultat dans 55,1 % des cas (10^6 tirages, `java.util.Random(2)`, poids uniformes dans [0, 1), coins uniformes dans [-1, 1)). L'existence de ces fonctions dans le jeu et leur ordre sont à confirmer par P12.
- **R13 (S, contrat interne)** `inverseInterpolate(v, a, b) = (v - a) / (b - a)`, sans bornage ni garde : pour `a == b`, NaN si `v == a`, infini signé sinon (C). Équivalent dans le jeu : à confirmer par P12.
- **R14 (S, contrat interne)** `remap(v, a0, a1, b0, b1) = interpolate(inverseInterpolate(v, a0, a1), b0, b1)`, sans bornage. `remapClamped` est la même valeur bornée par `clamp` entre `min(b0, b1)` et `max(b0, b1)`. `clampedInterpolate(t, a, b)` rend `a` si `t < 0`, `b` si `t > 1`, `interpolate(t, a, b)` sinon ; pour `t = 1` exactement, aucun remplacement par `b` : `clampedInterpolate(1, 1e20, 1) = 0.0` en forme A. Provisoire : ces compositions exactes. Équivalents dans le jeu : P12.
- **R15 (C pour les formes ; S pour celle du jeu)** Bornage `clamp(v, lo, hi)` sur `int`, `long`, `float`, `double`. Pour `lo <= hi` et `v` non NaN, toutes les formes donnent `lo` si `v < lo`, `hi` si `v > hi`, `v` sinon. Formes candidates (V12) : MM = `max(lo, min(v, hi))`, CMP = `v < lo ? lo : (v > hi ? hi : v)`, MIX = `v < lo ? lo : min(v, hi)`. **Provisoire : MIX**, choix sans preuve ; MIX coïncide avec CMP sauf si `hi` est NaN ou si `v` et `hi` sont deux zéros de signes opposés. Divergences :
  - `-0.0` et `0.0` : `clamp(-0.0, 0.0, 1)` vaut `0.0` (MM) ou `-0.0` (CMP, MIX) ;
  - NaN en borne : MM propage NaN, CMP l'ignore, MIX le propage pour `hi` seulement ;
  - `lo > hi` : `clamp(20, 10, 0)` vaut 10 (MM) ou 0 (CMP, MIX) ; le `Math.clamp` de Java lève une exception.
  `clamp(NaN, lo, hi)` vaut NaN dans toutes les formes. Le moteur n'appelle jamais `clamp` avec `lo > hi`. Forme du jeu : P15.
- **R16 (S, contrat interne)** Fondu quintique `quinticFade(t) = 6t^5 - 15t^4 + 10t^3` en `double`. Provisoire : forme de Horner `t*t*t*(t*(t*6 - 15) + 10)` [PN]. D'autres écritures diffèrent au dernier bit pour `t = 0.1` et `t = 0.9` (V13). Valeurs exactes : 0 en 0, 0.5 en 0.5, 1 en 1. L'usage dans le jeu et la forme sont levés par la spec du bruit (comparaison de terrain, P12).

### E. Trigonométrie et angles

- **R17 (S, contrat interne ; calculs C)** Le moteur fournit `tableSin(x)` et `tableCos(x)` par table :
  - table de 65 536 entrées `float`, entrée `i` = arrondi `float` de `sin(2*pi*i/65536)` calculé en `double` ; les écritures testées de cette formule (`Math` et `StrictMath`, deux ordres de produit) donnent les 65 536 mêmes `float` (C) ;
  - facteur `K = 65536 / (2*pi) = 10430.378350470453` en `double`, `10430.378f` (= 10430.3779296875) en `float` ;
  - cosinus : même table, indice décalé de `+16384` avant troncature.
  Candidats d'indexation (V14) : FI = entrée `float`, `(int)(x * K_float) & 65535` ; DL = entrée `double`, troncature via `long` puis masque ; DI = entrée `double`, troncature via `int` (saturation au-delà de |x| > 205 887,416). **Provisoire : DL**, dont la période est correcte pour |x| < 8,84e14 (`2^63 / K`), bien au-delà du seuil de DI. Limite connue hors domaine : au-delà, la conversion vers `long` sature et l'indice vaut toujours 65 535 pour `x > 0` et 0 pour `x < 0` (`tableSin(1e15)` rend l'entrée 65 535, `-9.58738E-5`, au lieu de l'indice 22 528, `0.8314696`). Erreur absolue maximale mesurée face à `StrictMath.sin` (2·10^6 points uniformes dans [-20, 20), `java.util.Random(4)`) : 9,59e-5 pour DL et DI, 9,68e-5 pour FI (C). Cas limites des candidats : `tableSin(-x) == -tableSin(x)` sauf à l'indice 32 768, mais `tableCos(-1.0) = 0.54025215` alors que `tableCos(1.0) = 0.54033285` ; `tableSin(NaN) = 0`. L'usage d'une table par le jeu, sa taille, son facteur et l'indexation sont à confirmer par P17.
- **R18 (S, contrat interne)** Arc tangente : provisoire `StrictMath.atan2`. Le jeu peut employer une approximation ; P18 mesure l'écart des rotations observées à `StrictMath.atan2`. Un écart non nul ouvre une spec dédiée.
- **R19 (S)** Racine carrée inverse rapide : le moteur n'en fournit pas. Aucune source autorisée n'en atteste l'usage par le jeu ; à rouvrir seulement si une spec consommatrice observe un écart attribuable à une telle approximation.
- **R20 (S, contrat interne)** `normalizeDegrees(v)` ramène un angle en degrés dans `[-180, 180)`. Provisoire : `r = v % 360 ; si r >= 180 alors r -= 360 ; si r < -180 alors r += 360`, calcul dans la précision de l'argument (`int`, `float` ou `double`). Donne `180 -> -180`, `-180 -> -180`, `540 -> -180`, `-190 -> 170`, `-0.0 -> -0.0` (V15). Que le jeu normalise les lacets, et comment, est à confirmer par P10.

### F. Entiers, aléa et poids

- **R21 (C)** `ceilingPowerOfTwo(v)` est la plus petite puissance de deux `>= v` pour `1 <= v <= 2^30` ; `ceilingLog2(v) = ceil(log2(v))`, nombre de bits pour coder `v` valeurs distinctes (V16 : 1->0, 2->1, 3->2, 17->5, 33->6). Pour `v <= 0` ou `v > 2^30` : non spécifié, interdit.
- **R22 (renvoi)** Tirage entier borné inclusif, y compris le cas `min == max` et le cas `min > max` : spec propriétaire `core-random` (R12 ; sonde P4/O-4). Cette spec n'en définit rien. La sonde complémentaire P22 ci-dessous est transmise à `core-random` et ne s'exécute qu'après P4/O-4.
- **R23 (S)** Sélection pondérée `weightedPick` [R26] : soit la liste ordonnée des entrées de poids effectif `w > 0` (poids nuls ou négatifs exclus) et `W` leur somme. Si aucune entrée : aucun résultat, aucun tirage. Si une seule : elle est rendue sans tirage. Sinon `r` est un tirage borné dans `[0, W)` (`core-random`) puis on parcourt la liste dans l'ordre d'origine en retranchant chaque poids ; l'entrée retenue est la première pour laquelle `r` devient négatif. Exemple poids `[3, 0, 5, 2]` : `r` 0..2 -> indice 0, 3..7 -> 2, 8..9 -> 3. Les poids sont lus une seule fois, au moment de l'appel (instantané) : une modification ultérieure, même concurrente pendant le tirage, n'affecte pas le résultat, et une entrée de poids 0 au moment de l'appel n'est jamais retenue. Provisoire : ce contrat pour toute liste pondérée. Le rapport ne l'établit que pour le butin ; chaque spec consommatrice (génération, apparition) le confirme par sa propre sonde.
- **R24 (renvoi)** Empreinte d'une position de bloc vers une graine `long` : spec propriétaire `core-random` (R29, `h(x, y, z)`). La sonde P24 ci-dessous est transmise à `core-random`.

### G. Coordonnées et empaquetages

- **R25 (D)** Overworld : `min_y = -64`, `height = 384`, `logical_height = 384` [DT], identiques dans `noise_settings` [NS]. Blocs constructibles `-64 <= y <= 319` (`min_y + height - 1`, C). Sections `-4..19`, soit 24 sections, indice de liste `sy - (min_y >> 4)` (C). `overworld_caves` : mêmes valeurs ; Nether : 0 et 256 (hauteur logique 128) ; End : 0 et 256 [DT].
- **R26 (S)** Domaine horizontal valide des blocs, provisoire : `-30 000 000 <= x, z < 30 000 000` ; bordure par défaut à 29 999 984 [W-WB]. Bornes exactes : P26.
- **R27 (S, conversion C)** La position de bloc contenant un point `(x, y, z)` `double` est `(floorToInt(x), floorToInt(y), floorToInt(z))` [W-CO] ; un point à `-0.0001` est dans le bloc `-1`, un point à `-0.0` dans le bloc 0. Confirmation : P4.
- **R28 (C)** Bloc vers chunk : `cx = x >> 4`, `cz = z >> 4` ; local `x & 15`, `z & 15` (R9). Section : `(x >> 4, y >> 4, z >> 4)`, local `y & 15`. Cellule de biome (quart) : `v >> 2`. Chunk vers premier bloc : `cx << 4` (déborde au-delà de |cx| >= 2^27, hors domaine R26). Région : `rx = cx >> 5`, local `cx & 31` ; en blocs, `x >> 9`. Que le jeu applique cette conversion : P9.
- **R29 (S)** Position de bloc empaquetée sur 64 bits [W-DT] : x sur les 26 bits de poids fort (décalage 38), z sur 26 bits (décalage 12), y sur les 12 bits de poids faible ; chaque champ masqué en complément à deux ; décodage par décalages arithmétiques (`x = v >> 38`, `z = (v << 26) >> 38`, `y = (v << 52) >> 52`). Plages représentables : x, z dans `[-2^25, 2^25 - 1]`, y dans `[-2048, 2047]` ; une valeur hors plage est repliée silencieusement (V5, dernière ligne). Provisoire : ce format pour la représentation interne aussi. Format réseau documenté, vérifiable en M8 par capture ; l'identité avec la représentation interne du jeu ne compte que si un ordre d'itération en dépend.
- **R30 (S)** Position de chunk empaquetée : x dans les 32 bits faibles (non signés), z dans les 32 bits forts [W-CF] ; `(-1, -1)` donne `-1`, `(1, -1)` donne `0xFFFFFFFF00000001`. Confirmation : P30.
- **R31 (S)** Position de section empaquetée (réseau) : x sur 22 bits décalé de 42, z sur 22 bits décalé de 20, y sur les 20 bits faibles [W-PK]. Vérifiable en M8 par capture.
- **R32 (S)** Trois ordres d'empaquetage local distincts coexistent, à ne pas confondre :
  - indice dans le tableau d'états d'une section : `(y & 15) << 8 | (z & 15) << 4 | (x & 15)` (ordre YZX) [W-CF] ;
  - `PostProcessing` des chunks : `short` de forme `0ZYX`, soit `z << 8 | y << 4 | x` [W-CF] ;
  - entrée réseau de mise à jour de section : `x << 8 | z << 4 | y` (après l'identifiant d'état décalé de 12) [W-PK].
  L'ordre des 64 cellules de biome d'une section n'est pas documenté dans les sources consultées (Q32). Confirmation des deux premiers : P30.
- **R33 (S)** Fichiers de région Anvil [W-RF] : fichier `r.<rx>.<rz>.mca` avec `rx = cx >> 5` ; 32 x 32 chunks ; secteurs de 4 096 octets ; en-tête de deux secteurs ; indice `i = (cx & 31) + 32 * (cz & 31)` ; entrée de localisation (3 octets de décalage en secteurs, 1 octet de taille) à l'octet `4i`, horodatage à l'octet `4096 + 4i`. Exemple publié : le chunk `(30, -3)` est dans la région `(0, -1)`. Confirmation : P30.

### H. Directions, axes, rotations, miroirs

- **R34 (D)** Axes : `x`, `y`, `z` ; axe horizontal : `x`, `z` [BL].
- **R35 (S)** Six directions, identifiant canonique et vecteur unitaire [W-PK] : 0 `down` (0,-1,0), 1 `up` (0,1,0), 2 `north` (0,0,-1), 3 `south` (0,0,1), 4 `west` (-1,0,0), 5 `east` (1,0,0). Boussole [W-CO] : +X est, +Z sud. Opposé : `id ^ 1` (C à partir de cet ordre). Confirmation : P35.
- **R36 (S)** Directions horizontales, identifiant 2D [W-PA] : 0 `south`, 1 `west`, 2 `north`, 3 `east`. Lacet de chaque direction (`yaw`, provisoire) [W-CO] : sud 0°, ouest 90°, nord 180°, est -90°. Lacet vers direction horizontale, provisoire : `fromYaw(yaw) = floorToInt(yaw / 90 + 0.5) & 3`, évalué en `double`. La frontière est celle de cette formule, pas celle du réel : 45 tombe à l'ouest, -45 au sud, 44.999 au sud, mais `Math.nextDown(45.0)` (44.99999999999999) tombe à l'ouest car `0.49999999999999994 + 0.5` s'arrondit à 1.0 ; un lacet à moins d'un ulp sous une frontière peut donc tomber dans le secteur suivant. Limite connue hors domaine (voir Q4) : pour |yaw| >= 90·2^31 (environ 1,93e11), `floorToInt` sature et `fromYaw` n'est plus périodique (`fromYaw(1e12 + 90)` donne est au lieu de sud). Confirmation : P35 (identifiants), P36 (frontières).
- **R37 (D)** L'ordre des valeurs d'une propriété de bloc est propre à la propriété et ne suit pas R35 : `facing` des blocs à six faces (`dispenser`, `observer`, `barrel`) est `north, east, south, west, up, down` ; `facing` des escaliers est `north, south, west, east` ; `rotation` des panneaux debout est `0..15` [BL]. Cet ordre détermine les identifiants d'états et relève de la spec des états de blocs.
- **R38 (S)** Rotations : `none`, `clockwise_90`, `180`, `counterclockwise_90` ; miroirs : `none`, `left_right`, `front_back` [W-PL]. Les données extraites n'attestent que l'existence des types d'arguments `template_rotation` et `template_mirror` de `/place template` [CMD], pas ces valeurs. Effet provisoire sur une direction horizontale : `clockwise_90` envoie nord sur est, est sur sud, sud sur ouest, ouest sur nord ; `180` donne l'opposé ; `counterclockwise_90` l'inverse de `clockwise_90` ; `left_right` échange nord et sud ; `front_back` échange est et ouest ; `up` et `down` sont invariants. Sur la propriété `rotation` 0..15, `rotateSixteenths(r)` pour `clockwise_90` = `(r + 4) & 15` (masque, pas le reste `%` de Java : `rotateSixteenths(-10) = 10`, alors que `(-10 + 4) % 16 = -6`) ; `180` ajoute 8, `counterclockwise_90` ajoute 12, sous le même masque. Noms et effets : P38.

### I. Géométrie

- **R39 (C, contrat)** Vecteur 3D : trois `double` ; addition, soustraction, produit par un scalaire composante par composante (R2). Longueur au carré `(dx*dx + dy*dy) + dz*dz` dans cet ordre ; l'ordre inverse change le résultat dans 23,8 % des cas (10^6 triplets uniformes dans `[0, 100)^3`, `java.util.Random(3)`). Longueur = `sqrt` de cette valeur. Ordre exact du jeu : S, à confirmer par les specs consommatrices (distances de spawn, portée). L'égalité de deux vecteurs est l'égalité exacte des composantes ; le traitement de `-0.0` et NaN par le jeu n'est pas observable et reste libre.
- **R40 (S)** Boîte alignée `[minX, maxX] x [minY, maxY] x [minZ, maxZ]` en `double`. Provisoire : construite à partir de deux coins quelconques, elle se normalise (min et max par axe). La boîte d'un bloc entier `(x, y, z)` est `[x, x+1] x [y, y+1] x [z, z+1]`.
- **R41 (S)** Intersection de deux boîtes, provisoire : vraie si et seulement si, sur chacun des trois axes, `a.min < b.max` et `a.max > b.min` (inégalités strictes : deux boîtes qui se touchent par une face ne s'intersectent pas). Contenance d'un point : `min <= p < max` sur chaque axe. P41 mesure la frontière par le sélecteur de volume [W-TS].
- **R42 (S)** Opérations provisoires : déplacement (ajout d'un vecteur aux deux coins, C) ; gonflement symétrique de `d` (min - d, max + d ; `d` négatif contracte) ; extension dirigée par un vecteur `v` (par axe, `v < 0` étend le min, `v > 0` le max). Une boîte contractée au-delà de zéro (min > max) n'est pas renormalisée.

## Entrées / sorties

| Grandeur | Type | Plage valide | Hors plage |
|---|---|---|---|
| Coordonnée de bloc x, z | `int` | `[-30 000 000, 30 000 000)` (R26) | refus en amont |
| Coordonnée de bloc y (Overworld) | `int` | `[-64, 319]` | refus pour placement ; calcul permis |
| Bloc empaqueté | `long` | x, z sur 26 bits, y sur 12 bits | repli silencieux (R29) |
| Chunk x, z | `int` | `[-1 875 000, 1 875 000)` | idem |
| Section y (Overworld) | `int` | `[-4, 19]` | idem |
| Région x, z | `int` | `[-58 594, 58 593]` | idem |
| Arguments de `floorToInt`, `ceilToInt` | `float`, `double` | résultat dans `int` | S (R4) |
| Angles | `float` ou `double`, radians pour R17, degrés pour R20 | tous | voir R17, R20 |
| Paramètre d'interpolation `t` | `float` ou `double` | tous (extrapolation) | aucun bornage implicite |

## Cas limites

| Cas | Attendu (provisoire si S) | Règle |
|---|---|---|
| `x = -1` | chunk -1, local 15 ; région -1, local 31 | R28 |
| `x = -16` / `-17` | chunk -1 local 0 / chunk -2 local 15 | R28 |
| `x = 15` / `16` / `17` | chunk 0 local 15 / chunk 1 local 0 / chunk 1 local 1 | R28 |
| `x = 511` / `512` / `-513` | région 0 / 1 / -2 (en blocs, `>> 9`) | R28 |
| `x = MIN_VALUE` | chunk -134 217 728, local 0 (hors domaine R26) | R9 |
| `floorToInt(-0.0)`, `floorToInt(NaN)` | 0, 0 (NaN : provisoire) | R4 |
| `floorToInt(-2^31 - 0.5)` | provisoire `-2^31` (F1) ; F2 donnerait `2^31 - 1` | R4 |
| `clamp(-0.0, 0.0, 1.0)` | provisoire `-0.0` (MIX) | R15 |
| `clamp(NaN, 0, 1)` | NaN | R15 |
| `clamp(20, 10, 0)` | provisoire 0 (MIX) ; usage interdit | R15 |
| `interpolate(1, 1e20, 1)` | provisoire 0 (forme A) | R11 |
| `inverseInterpolate(1, 1, 1)` | NaN | R13 |
| `tableSin(205 888.0)` | provisoire `0.55117816` (DL) | R17 |
| `tableCos(-1.0)` | `0.54025215`, différent de `tableCos(1.0)` | R17 |
| `tableSin(1e15)` | entrée 65 535 (`-9.58738E-5`), limite connue hors domaine | R17 |
| `fromYaw(44.999)` / `fromYaw(Math.nextDown(45.0))` | sud / ouest (provisoire) | R36 |
| `fromYaw(1e12 + 90)` | est, limite connue hors domaine | R36 |
| `fractionalPart(3.00000000025E9)` / `fractionalPart(1e19)` | `0.25` / `7.766279631452242E17` (hors domaine) | R7 |
| `rotateSixteenths(-10)` pour `clockwise_90` | 10 (provisoire) | R38 |
| `normalizeDegrees(180.0)`, `normalizeDegrees(540.0)` | `-180.0` | R20 |
| Bloc `(-1, -64, -1)` | chunk (-1,-1), section -4 (indice 0), indice YZX 255 | R25, R32 |
| Bloc empaqueté `(2^25, 2048, -2^25 - 1)` | relu `(-2^25, -2048, 2^25 - 1)` | R29 |
| Chunk `(-1, -1)` empaqueté | `0xFFFFFFFFFFFFFFFF` | R30 |
| Boîtes `[0, 0.5]` et `[-1, 0]` sur x | pas d'intersection (provisoire) | R41 |

## Vecteurs de test

Voir `core-math-vectors.md`. Les vecteurs C sont calculés en Java 21.0.10 par les opérateurs du langage et `StrictMath` (reproductibles dans `jshell`). Les vecteurs S portent la sonde qui les confirmera et le candidat provisoire. Aucun vecteur M n'existe encore.

## Sondes de l'oracle

Toutes passent par la console RCON (aucune entité exécutante : on cible des marqueurs étiquetés), monde de test plat, `randomTickSpeed 0`, `/tick freeze`, interface admise sans 2a-bis (ADR 0002). Aucune n'a été exécutée.

| Sonde | Règle | Procédure | Critère |
|---|---|---|---|
| P4 | R4, R27 | `/forceload add 0 0` ; `/execute positioned -0.0001 70 -0.0001 run setblock ~ ~ ~ minecraft:stone`, puis `/execute if block -1 70 -1 minecraft:stone` ; idem avec `-0.0`, `15.99999`, `-16.00001` | blocs `-1`, `0`, `15`, `-17` |
| P9 | R28 | `/forceload add X Z` puis `/forceload query X Z` pour X, Z dans {-17, -16, -1, 0, 15, 16, 17, 511, 512, -513} ; le message cite les coordonnées du chunk | chunk = `X >> 4` |
| P10 | R20 | `/summon minecraft:marker 0 70 0 {Tags:["p10"]}` ; pour yaw dans {180, -180, 540, -190, 359.5} : `/execute as @e[type=marker,tag=p10,limit=1] run tp @s 0 70 0 <yaw> 0`, puis `/data get entity @e[type=marker,tag=p10,limit=1] Rotation[0]` | valeur stockée comparée à V15 ; une valeur non normalisée (540 relu 540) montre que le stockage ne normalise pas et laisse R20 en contrat interne |
| P11 | R11 | datapack : `noise_settings` dont `final_density` est une fonction `minecraft:lerp` de constantes `alpha = 0.3`, `first = -1.5`, `second = 3.5` ; générer un chunk, lire un bloc | forme A : densité 0, air ; forme B : densité > 0, pierre (V11, en `float` comme en `double`). Sonde indirecte : elle tranche la forme de l'opérateur de densité, présumée partagée |
| P12 | R12 à R14, R16 | comparaison de terrain M4 sur l'échantillon figé | bit à bit |
| P15 | R15 | datapack : fonction de densité `minecraft:clamp` d'une constante hors bornes, puis d'une constante `-0.0` avec `min = 0.0`, suivie d'un opérateur qui distingue le signe de zéro s'il en existe un dans les données | si aucun opérateur ne distingue le signe, R15 reste provisoire et les cas divergents restent interdits |
| P17 | R17 | `/execute rotated <yaw> 0 positioned 0 70 0 run summon minecraft:marker ^ ^ ^1 {Tags:["p17"]}`, puis `/data get entity @e[type=marker,tag=p17,limit=1] Pos` pour yaw dans {37, -37, 90, 1000000} | `|dx|` et `|dz|` comparés à `StrictMath` et aux candidats (V14 : pour 37°, exact 0.6018150231520483, table 0.6017657) ; tranche aussi la conversion degrés vers radians |
| P18 | R18 | `/summon minecraft:marker 0 70 0 {Tags:["p18"]}` ; pour une grille de cibles `(X, Y, Z)` : `/execute as @e[type=marker,tag=p18,limit=1] at @s facing X Y Z run tp @s ~ ~ ~ ~ ~`, puis lecture de `Rotation` | écart à `StrictMath.atan2` converti en degrés `float` |
| P22 | R22 (transmise à `core-random`) | Préalable : résultat de `core-random` P4/O-4 (la commande `/random` emploie-t-elle le tirage borné de `core-random` R12). Étape 1 : `/random value 5..5` doit réussir ; si la commande refuse un intervalle de taille 1, P22 est inapplicable et le cas `min == max` se lève par la voie de `core-random`. Étape 2 : `/random reset <seq> 7`, `/random value 5..5 <seq>`, `/random value 1..1000000 <seq>` ; refaire sans l'appel `5..5` | même dernière valeur = aucun tirage consommé pour `min == max` |
| P24 | R24 (transmise à `core-random`) | bambou (forme de collision décalée par position) posé sur une grille de positions ; entité sans IA poussée contre chaque tige par `Motion` ; lecture de `Pos` finale | ajuster une formule candidate sur au moins 64 positions dont négatives ; coûteuse, jalon M5 au plus tôt |
| P26 | R26 | pour chaque cible, d'abord `/forceload add <x> 0` (le message d'erreur éventuel est consigné : il renseigne déjà sur la borne), puis `/setblock <x> 64 0 minecraft:stone` pour x dans {29999999, 30000000, -30000000, -30000001} ; puis, dans un chunk chargé, `/setblock 0 319 0`, `0 320 0`, `0 -64 0`, `0 -65 0` | succès, ou erreur "hors du monde" distincte de l'erreur "position non chargée" |
| P30 | R30, R32, R33 | `/forceload add 480 -48` (chunk (30,-3)), `/save-all flush` ; lire `r.0.-1.mca` avec le lecteur indépendant de `tools/oracle` ; pour R30, `/place structure` d'un village sur une frontière de région puis lecture des références ; pour R32, blocs posés par `/setblock` à des positions locales connues | fichier, indice 958, décalage 3832 ; longs de références conformes à R30 ; états relus à l'indice YZX |
| P35 | R35, R36 | `/summon minecraft:item_frame 0 70 0 {Facing:<n>b,Tags:["p35"]}` pour n = 0..5, `/summon minecraft:painting 0 70 0 {facing:<n>b,Tags:["p35"]}` pour n = 0..3 ; lire `Rotation` de chaque entité étiquetée | lacet et tangage conformes à la direction attendue (modulo 360) |
| P36 | R36 | client réel (M9) : joueur téléporté avec lacet 44.999, 45, -45, -45.001, pose d'un fourneau | `facing` du bloc posé |
| P38 | R38 | `/place template minecraft:igloo/top <pos> <rotation> <mirror>` pour les 12 combinaisons ; lire les blocs orientés par `/execute if block ... [facing=...]` ou `.mca` | acceptation des noms et table de passage des directions |
| P41 | R40, R41 | `/summon minecraft:armor_stand 0.25 64 0.5 {NoGravity:1b,Tags:["p41"]}` (boîte x de 0.0 à 0.5) ; `/execute if entity @e[tag=p41,x=-1,y=64,z=0,dx=0,dy=0,dz=0]` (volume x de -1 à 0) ; témoin avec l'entité à `x = 0.2499` | non sélectionné = intersection stricte |

## Questions ouvertes

- **Q0 (fermée)** La documentation de mappings communautaires est retirée des sources (ADR 0002, point 2a-bis non autorisé, décision par défaut du propriétaire). Les règles qui en dépendaient sont devenues des contrats internes S avec comportement provisoire.
- **Q4** Comportement de `floorToInt` hors du domaine `int` : non observable par commande (coordonnées bornées par R26). Rester sur l'interdiction et le provisoire F1. Limites connues qui en découlent : `fromYaw` au-delà de 1,93e11 (R36), `fractionalPart` au-delà de 2^63 (R7), `tableSin` au-delà de 8,84e14 (R17).
- **Q10** Formule de modulo réel : à trancher par chaque spec consommatrice ; aucune sonde M1 ne la sépare.
- **Q11** Forme de l'interpolation (A ou B) et précision : P11, puis confirmation par P12.
- **Q17** Paramètres de la table de sinus : P17. Si l'écart observé à `StrictMath` est nul, ce chemin n'utilise pas de table et R17 reste S jusqu'à une sonde de génération (géométrie des filons, M4).
- **Q32** Ordre des 64 cellules de biome d'une section : lecture d'un `.mca` après `/fillbiome` sur une cellule unique (P30 étendue).
- **Q41** Si P41 montre une intersection inclusive, R41 bascule, et les specs de collision reprennent le contrat.
- Tant que les sondes ne sont pas exécutées, la spec ne peut pas passer à `figé`.

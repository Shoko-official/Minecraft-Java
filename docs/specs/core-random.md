# Générateurs pseudo-aléatoires du moteur

Statut : brouillon
Jalon : M1
Risque : A
Mise à jour : 2026-10-28, retours de relecture et de chasse aux bugs du code (PR #8) : R5, R11, R21, R26, R29, exigences d'interface

## Périmètre

Couvert :

- le générateur congruentiel 48 bits compatible avec `java.util.Random`, appelé ici **G48** ;
- le générateur xoroshiro128++, appelé ici **X128** ;
- les tirages dérivés de chacun (entiers avec et sans borne, long, booléen, float, double, gaussienne), leur consommation et le saut de `n` pas ;
- l'expansion d'une graine 64 bits en état 128 bits et l'empreinte d'un identifiant ;
- le contrat des sources dérivées (source dérivée d'une autre, source par nom, source par position), réduit à des propriétés vérifiables et à un comportement provisoire étiqueté comme tel. Le sous-ensemble « source par position » est exclu de la fidélité M1 tant que Q1 n'est pas levée (R29) ;
- les séquences aléatoires nommées du jeu et la commande `/random`, qui sont l'interface d'oracle admise au jalon M1 (ADR 0002, tableau 2c).

Non couvert : les graines propres à la génération de monde (décoration, features, placement de structures, sels, choix de famille par réglage de bruit), listées en fin de document pour la spec worldgen ; le choix de la source de butin (spec loot) ; les tables numériques (trigonométrie) et utilitaires mathématiques (autre spec M1).

Vocabulaire :

- **pas** : une transition de l'état du générateur. La **consommation** d'un appel est son nombre de pas.
- Arithmétique « mod 2^64 » : entiers 64 bits en complément à deux, débordement silencieux. `>>>` est le décalage logique, `>>` le décalage arithmétique, `rotl(x, k)` la rotation à gauche de `k` bits sur 64 bits.
- Les mots de 64 bits sont écrits en hexadécimal non signé (`0x...`) ; les valeurs lues dans les fichiers du jeu sont des longs signés.

## Sources

Niveaux de preuve employés :

- **D** : donnée exacte. Soit un fichier extrait du jeu de référence, soit une source publique qui fixe la définition d'un algorithme (spécification Java, article ou code de référence des auteurs). Une telle définition ne dit rien de son emploi par le jeu : cet emploi a son propre niveau.
- **M** : observé sur l'oracle. Aucune règle n'est à ce niveau aujourd'hui (oracle non exécutable, EULA non acceptée).
- **C** : calcul dérivé, exécuté par l'analyste (scripts dans [core-random-vectors.md](core-random-vectors.md)).
- **S** : à vérifier. Toute affirmation sur le comportement du jeu qui ne vient que d'un rapport local, de la documentation communautaire ou d'une hypothèse de l'analyste. La mention **S, documenté [Wn]** signale une règle décrite par le wiki public (non normatif) et le plus souvent recoupée par un rapport : elle reste S, mais sa sonde est prioritaire.

Sources publiques :

- [J] Spécification de `java.util.Random` (Javadoc Java SE 21 et 25) : constantes, `setSeed`, `next(bits)`, `nextInt`, `nextInt(bound)`, `nextLong`, `nextBoolean`, `nextFloat`, `nextDouble`, `nextGaussian`.
- [BV] D. Blackman, S. Vigna, code de référence `xoroshiro128plusplus.c` (domaine public), https://prng.di.unimi.it/xoroshiro128plusplus.c ; page https://prng.di.unimi.it/ (conversion d'une sortie 64 bits en double).
- [L] D. Lemire, « Fast Random Integer Generation in an Interval », ACM TOMACS 29(1), 2019 (entier borné par multiplication).
- [ST] D. Stafford, « Better Bit Mixing » (variantes Mix01 à Mix14), et la fonction de finalisation de SplitMix64 (G. Steele, D. Lea, C. Flood, OOPSLA 2014) ; recoupé par calcul avec `java.util.SplittableRandom`.
- [W1] https://minecraft.wiki/w/Commands/random (syntaxe, défauts, limites, permissions, valeurs de retour).
- [W2] https://minecraft.wiki/w/Random_sequence et format `random_sequences.dat` (algorithme, dérivation de la graine, constantes, format NBT).

Données extraites (jeu 26.3, `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3\`) :

- [E1] `generated/reports/commands.json`, nœud `random` (arbre de syntaxe et permissions).
- [E2] `resources/assets/minecraft/lang/en_us.json`, clés `commands.random.*`.
- [E3] `resources/data/minecraft/worldgen/noise_settings/*.json`, champ `legacy_random_source`.
- [E4] Champ `random_sequence` sous `resources/data/minecraft/` : 1 520 fichiers, soit les 1 447 tables de `loot_table/` (toutes), 68 fichiers de `trade_set/` et 5 tables de butin du pack intégré `datapacks/trade_rebalance/data/minecraft/loot_table/chests/`. Les `rolls` de type `minecraft:uniform` ont la forme `{"type": "minecraft:uniform", "min": ..., "max": ...}`.

Rapports locaux (indices, ADR 0002 point 2b) :

- [R1] `docs/reference/Rapport_Integral_Overworld_Minecraft_Java_26_3.md` : chapitres 018 et 019 (sections 2.1 et 2.2, familles aléatoires et transitions), chapitres 103 et 104 (choix de la source de butin, séquences nommées), annexe JSON `loot_random_priority` et `uniform_integer`, synthèse finale (paragraphes sur les deux familles).
- [R2] `docs/reference/rapport-generation-minecraft-java-1-21-1.md` : sections 2.2.1, 2.2.2, 2.3, 2.4 et 10.1.2 (version 1.21.1, pas 26.3).

Sondes : P1 à P7, décrites plus bas. Vecteurs : [core-random-vectors.md](core-random-vectors.md).

## Règles

### A. Générateur 48 bits (G48)

**R1 (D [J])** L'état est un entier `x` de 48 bits, 0 <= x < 2^48. Une transition (un pas) remplace `x` par `(0x5DEECE66D * x + 0xB) mod 2^48`, soit le multiplicateur 25214903917 et l'incrément 11.

**R2 (D [J])** Initialiser avec une graine `seed` (64 bits signée) met l'état à `(seed XOR 0x5DEECE66D) mod 2^48` et vide la gaussienne en cache (R7). Conséquence (C) : seuls les 48 bits de poids faible de la graine comptent ; deux graines congrues modulo 2^48 donnent le même flux (vecteurs : graines 0, 2^48 et -2^63 identiques ; graines -1 et 2^63-1 identiques).

**R3 (D [J])** `next(b)`, pour 1 <= b <= 32, avance d'un pas puis rend les `b` bits de poids fort du nouvel état, `x >>> (48 - b)`. Pour `b = 32` le résultat est lu comme un entier signé de 32 bits.

**R4 (D [J])** Tirages simples :

- `nextInt()` = `next(32)` ;
- `nextBoolean()` = `next(1) != 0` ;
- `nextLong()` = `(next(32) << 32) + next(32)` mod 2^64, où les deux tirages sont des entiers 32 bits signés, le premier tiré d'abord. Le second est étendu en signe avant l'addition : s'il est négatif, la moitié haute du résultat diffère du premier tirage (vecteur V-L1, graine 0).

**R5 (D [J])** `nextInt(bound)` :

- `bound <= 0` : erreur, aucun pas consommé ;
- `bound` puissance de deux (1 compris) : `(bound * next(31)) >> 31`, un pas. `nextInt(1)` rend toujours 0 mais consomme un pas ;
- sinon, boucle : `r = next(31)`, `v = r mod bound` ; `v` est accepté si `r - v + (bound - 1) < 2^31` (calcul exact, sans débordement), sinon on recommence. Forme équivalente (C) : accepté si `r < bound * floor(2^31 / bound)`. Chaque essai coûte un pas.
- Inégalité stricte (C) : le test non strict `r - v + (bound - 1) <= 2^31` ne diffère du test exact que si `bound * (q + 1) = 2^31 + 1` pour un quotient `q`, c'est-à-dire si `bound` divise 2^31 + 1 = 3 x 715827883. Les bornes 3 et 715827883 sont donc obligatoires dans les vecteurs de rejet : pour 715827883, le test exact rejette tout le tiers haut (`r >= 1431655766`, environ un essai sur trois) que le test non strict accepterait (V-L2, V-L3b) ; pour 3, seules `r = 2^31 - 2` et `2^31 - 1` sont en jeu.

**R6 (D [J])** `nextFloat()` = `next(24) * 2^-24` (un pas, résultat exact en float, dans [0, 1)). `nextDouble()` = `(next(26) * 2^27 + next(27)) * 2^-53` (deux pas, résultat exact en double, dans [0, 1)).

**R7 (D [J])** `nextGaussian()`, méthode polaire de Marsaglia avec une valeur en cache :

- si une valeur est en cache, elle est rendue et retirée, sans pas ;
- sinon répéter : `v1 = 2 * nextDouble() - 1`, `v2 = 2 * nextDouble() - 1`, `s = v1^2 + v2^2`, jusqu'à `0 < s < 1` (quatre pas par essai) ; puis `m = sqrt(-2 * ln(s) / s)` ; mettre `v2 * m` en cache et rendre `v1 * m`.
- Dans `java.util.Random`, `ln` et `sqrt` sont ceux de `StrictMath` (fdlibm).

**R8 (S)** Le générateur 48 bits du jeu suit R1 à R6 : mêmes constantes, même embrouillage de la graine, mêmes assemblages et mêmes rejets. Indices : [R1] chapitre 019 et synthèse, [R2] 2.2.1 et 10.1.2, concordants pour R1 et R2 ; R3 à R6 ne sont qu'évoqués (« reproduire nextInt borné et ses rejets, nextLong, nextFloat, nextDouble »). Sonde : P5 (et P7 en recoupement indirect).

**R9 (S)** La gaussienne du jeu suit l'enchaînement de R7. Le choix du logarithme (`StrictMath` ou `Math`) n'est établi par aucune source autorisée. Ce n'est pas un détail : sur la JVM de référence, les deux variantes diffèrent pour environ une gaussienne sur trente (vecteur V-L7, calcul C). Implémentation provisoire : `StrictMath`, seul choix indépendant de la plateforme. Sonde : aucune directe sans ADR 0002 point 2a-bis (question Q3).

**R10 (C)** Saut : `skip(n)` avance l'état de exactement `n` pas, comme `n` appels à `next(b)`. Il peut se calculer en O(log n) en composant l'application affine `x -> a*x + c` par élévation au carré : `(a1, c1)` puis `(a2, c2)` donnent `(a2*a1, a2*c1 + c2)` mod 2^48. Vecteur V-L4 (graine 0, 10^6 pas).

**R11 (C, depuis R3 à R7)** Consommation par appel, mesurée par le compteur de pas (saturant, voir les exigences d'interface) :

| Appel | Pas |
|---|---|
| `next(b)`, `nextInt()`, `nextBoolean()`, `nextFloat()` | 1 |
| `nextInt(bound)`, puissance de deux | 1 |
| `nextInt(bound)`, autre borne | k >= 1 (nombre d'essais) ; pour 2^30+1, environ 2 en moyenne (V-L3) |
| `nextLong()`, `nextDouble()` | 2 |
| `nextGaussian()` | 0 si valeur en cache, sinon 4k (k essais) |
| initialisation | 0 (et vide le cache) |

**R12 (S, [R1] annexe `uniform_integer`)** Tirage d'un entier dans un intervalle inclusif `[min, max]` tel que le jeu l'utilise pour ses fournisseurs de nombres : si `min < max`, `min + nextInt(max - min + 1)` ; si `min >= max`, rend `min` sans consommer de pas. Cette spec est propriétaire de cette règle : la spec `core-math` (sa règle de tirage dans un intervalle) y renvoie au lieu de la dupliquer. Sonde du cas `min == max` : P4. Différence avec `java.util.Random` (D [J]) : depuis Java 17, les méthodes à deux bornes de `java.util.Random` (`nextInt(origin, bound)`, `nextLong(bound)`, etc.) viennent d'une interface générique et utilisent d'autres algorithmes et d'autres consommations. Le moteur ne doit ni les appeler ni les imiter. Aucune autre différence observable entre G48 du jeu et `java.util.Random` n'est connue à ce jour (R8, R9 restent S).

### B. Générateur xoroshiro128++ (X128)

**R13 (D [BV])** L'état est un couple de mots de 64 bits `(s0, s1)`, jamais tous deux nuls. Un pas calcule la sortie puis la transition, toutes opérations mod 2^64 :

- sortie `r = rotl(s0 + s1, 17) + s0` ;
- `t = s1 XOR s0` ; `s0' = rotl(s0, 49) XOR t XOR (t << 21)` ; `s1' = rotl(t, 28)`.

La sortie est calculée sur l'état avant la transition. Vecteurs V-X1 (recoupés avec l'implémentation indépendante du JDK).

**R14 (S, documenté [W2], recoupé [R1], [R2])** Les séquences aléatoires nommées du jeu (butin, `/random`) utilisent X128. Sonde : P2. Indice associé, également S ([R1], voir hors périmètre) : le réglage `legacy_random_source` choisit la famille de générateur employée pour le bruit du monde (X128 pour `false`, 48 bits pour `true`). Valeurs du drapeau (D [E3]) : `false` pour `overworld`, `amplified`, `large_biomes` ; `true` pour `nether`, `end`, `caves`, `floating_islands`.

**R15 (S, [R2] 2.2.2)** Un état tout à zéro est remplacé à la construction par un état non nul fixe, dont la valeur n'est donnée par aucune source autorisée. Calcul (C) : les initialisations R24 et R31 ne peuvent pas produire cet état (le mélange R23 est bijectif et ne s'annule qu'en 0 ; avant mélange, `hi = lo + 0x9E3779B97F4A7C15` interdit `lo = hi = 0`, sauf empreinte MD5 particulière). Choix provisoire du moteur, faute de valeur connue : un état nul fourni directement est une erreur de programmation (exception), ce qui ne se produit dans aucun chemin connu ; à remplacer par le comportement du jeu quand Q2 sera levée.

**R16 à R20 : tirages dérivés de X128.** Aucune source autorisée ne décrit comment le jeu les assemble. Les hypothèses ci-dessous suivent les recommandations publiques [BV] et [L] ; chacune a une contre-hypothèse que la sonde indiquée départage.

**R16 (S, H-X1)** `nextLong()` rend la sortie `r` lue comme long signé. Un pas. Sonde : P1 et P2 (l'état après `n` tirages confirme un pas par tirage).

**R17 (S, H-X1)** `nextInt()` rend les 32 bits de poids faible de `r`, lus comme entier signé. Un pas.

**R18 (S, H-X2)** `nextInt(bound)`, `bound > 0`, méthode de [L] sur les 32 bits de poids faible : `u` = 32 bits faibles de `r` (non signé), `m = u * bound` (64 bits non signé) ; si `m mod 2^32 < bound`, calculer `t = (2^32 - bound) mod bound` et, tant que `m mod 2^32 < t`, retirer `u` sur une nouvelle sortie et recalculer `m` ; rendre `m >>> 32`. Pas de cas particulier pour les puissances de deux. Un pas, rarement plus. Contre-hypothèses : H-X2b (31 bits de poids fort et rejet comme R5), H-X2c (méthode de [L] sur les 32 bits de poids fort). Sondes : P2 et P3, vecteurs V-X5.

**R19 (S, H-X3 à H-X5)** `nextFloat()` = `(r >>> 40) * 2^-24` (H-X3) ; `nextDouble()` = `(r >>> 11) * 2^-53` (H-X4, conversion recommandée par [BV]) ; `nextBoolean()` = bit de poids faible de `r` (H-X5). Un pas chacun. Contre-hypothèse principale : bits de poids fort pour le booléen. Sonde : P6 pour `nextFloat` ; `nextDouble` et `nextBoolean` n'ont pas de sonde directe (Q8).

**R20 (S)** `nextGaussian()` suit l'enchaînement de R7 avec le `nextDouble` de X128 : deux pas par essai, cache vidé à toute réinitialisation. Même incertitude sur le logarithme que R9.

**R21 (C, sous R16 à R20)** Consommation : un pas pour `nextLong`, `nextInt`, `nextBoolean`, `nextFloat`, `nextDouble` ; `k` pas pour `nextInt(bound)` (k = 1 sauf rejet, probabilité inférieure à `bound / 2^32`) ; 0 ou 2k pour la gaussienne. `skip(n)` avance de `n` pas, exécutés un par un (coût O(n)). Aucun saut en forme fermée n'est requis en M1. La transition étant linéaire sur GF(2), un saut en O(log n) par élévation d'une matrice 128 x 128 à la puissance `n` est possible et pourra être ajouté ; les sauts de [BV] n'en sont que deux cas particuliers (2^64 et 2^96 pas). Limite connue : pour de très grands `n` (au-delà de quelques milliards), `skip(n)` sur X128 ne termine pas en pratique ; le compteur de pas reste soumis à la règle de saturation.

### C. Graines, empreintes et sources dérivées

**R22 (C, définition de la fonction)** Expansion non mélangée `E(v)` d'une valeur 64 bits `v` en deux mots : `lo = v XOR K_SQRT2`, `hi = lo + K_PHI` (mod 2^64), avec `K_SQRT2 = 0x6A09E667F3BCC909` et `K_PHI = 0x9E3779B97F4A7C15`. Recoupements (C) : `K_PHI = floor(2^64 / phi)` (nombre d'or) ; `K_SQRT2 = floor(frac(sqrt 2) * 2^64) + 1`, l'entier impair qui suit la partie fractionnaire de racine de 2 ; le constructeur à graine 64 bits du xoroshiro128++ du JDK (module `jdk.random`) emploie les mêmes constantes, puisqu'il reproduit V-X3 (voir R24).

**R23 (D [ST])** Mélange de Stafford, variante 13 (finaliseur de SplitMix64), sur un mot `z` : `z = (z XOR (z >>> 30)) * 0xBF58476D1CE4E5B9` ; `z = (z XOR (z >>> 27)) * 0x94D049BB133111EB` ; résultat `z XOR (z >>> 31)`, mod 2^64. Il est bijectif et vaut 0 en 0. Recoupement (C) : `new java.util.SplittableRandom(0).nextLong()` vaut le mélange de `K_PHI`, soit -2152535657050944081, identique au calcul. Vecteurs V-X2.

**R24 (C, définition de la fonction)** `Init64(seed)` : état X128 = `(mélange(lo), mélange(hi))` avec `(lo, hi) = E(seed)` (R22, R23). Recoupement (C) : `java.util.random.RandomGeneratorFactory.of("Xoroshiro128PlusPlus").create(seed)` donne les trois premiers `nextLong` de V-X3 pour les graines 0, 1, -1, 12345 et -2^63 (JDK 21.0.10).

**R25 (S, documenté [W2], recoupé [R1], [R2])** Emploi par le jeu : les séquences nommées sont initialisées par `E`, l'empreinte R26 et le mélange R23 (détail en R31) ; une source X128 initialisée ou réinitialisée par une graine 64 bits l'est par `Init64`, et la réinitialisation vide la gaussienne en cache. Sonde : P1 (le cas N1 vaut exactement `Init64(0)`).

**R26 (S, documenté [W2] pour le MD5 ; S pour le texte haché et l'ordre des octets)** L'empreinte d'un identifiant est le MD5 (128 bits) de son texte. Hypothèses à lever : le texte est la forme complète `namespace:path` encodée en UTF-8 (`test` et `minecraft:test` donnent la même empreinte, celle de `minecraft:test`) ; les 8 premiers octets, lus en gros-boutiste, forment le mot bas, les 8 suivants le mot haut. Contre-hypothèse : petit-boutiste. Normalisation avant hachage (S, comportement **provisoire**, aligné sur la règle R22 de la spec `data-datapack-loading`) : un identifiant sans `:` est préfixé par `minecraft:` ; un espace de noms vide (`:test`) devient `minecraft` (`minecraft:test`) ; les trois formes `test`, `:test` et `minecraft:test` ont donc la même empreinte (V-X4). Limite connue : cette fonction ne valide pas l'identifiant. Majuscules, deux `:`, chaîne vide, ou demi-paire de substitution isolée (encodée en `?` par l'UTF-8 de Java) sont hachés tels quels ; la validation incombe à l'appelant. Vecteurs V-X4. Sonde : P1, cas N4 contre N4le, et N7 (joué avec `minecraft:test` puis `test` ; ajouter `:test` si la commande l'accepte).

**R27 à R29 : sources dérivées.** La génération de monde a besoin de sources dérivées d'une graine : d'une autre source, d'un nom ou d'une position. Les sources autorisées n'en décrivent pas la construction dans le jeu ; cette spec n'en fixe donc que le contrat vérifiable, et un comportement **provisoire** qui rend le reste du moteur testable. Le provisoire n'est pas fidèle : aucun vecteur doré ne peut en être tiré. La construction fidèle sera établie par la spec worldgen, à partir de sondes sur le monde généré (ou par appel direct si le propriétaire accepte ADR 0002 point 2a-bis).

**R27 (S, contrat ; provisoire)** Source par nom. Contrat : fonction déterministe d'une graine de dérivation et d'un identifiant ; deux identifiants distincts donnent des flux distincts. Provisoire : appliquer R31 étapes 3 et 4 aux deux mots de la dérivation (XOR avec l'empreinte R26, puis mélange). Sonde : indirecte (worldgen).

**R28 (S, contrat ; provisoire)** Source dérivée d'une autre. Contrat : la fille est une fonction déterministe de l'état de la mère, et la dérivation avance la mère d'un nombre fixe de pas ; ce nombre fait partie de la fidélité et sera fixé par sonde ([R1] signale seulement que cette consommation doit être reproduite). Provisoire : la mère fournit un `nextLong`, la fille est `Init64` de ce long pour X128 et la graine R2 de ce long pour G48 (soit un pas X128, deux pas G48).

**R29 (S, contrat ; provisoire ; hors fidélité M1)** Source par position. Contrat : fonction déterministe d'une graine de dérivation `d` et d'une position entière `(x, y, z)`, définie pour toutes les valeurs `int`, négatives comprises, sans division ; des positions voisines donnent des flux distincts. L'empreinte de position du jeu est inconnue (Q1). Cette spec en est propriétaire : la spec `core-math` (sa règle d'empreinte de position) y renvoie, car il s'agit probablement de la même fonction ; une seule question, Q1, couvre les deux. Provisoire, étiqueté comme tel dans le code : empreinte `hp(x, y, z) = mélange(mélange(mélange(x) XOR y) XOR z)`, les coordonnées étant étendues en signe à 64 bits, et source = `Init64(d XOR hp(x, y, z))`. Vecteurs provisoires (C, non dorés) : V-X6. Limite connue du provisoire : des positions distinctes très éloignées peuvent avoir la même empreinte, donc le même flux. Exemple prouvé (V-X6) : `(23901, 0, z)` et `(52826, 683136096, z)`, pour tout `z` (empreinte -1813262085031591903 pour `z = 0`). Ce n'est pas une violation du contrat, qui n'exige des flux distincts que pour des voisins, mais un signe de plus que la construction provisoire n'est pas fidèle (Q1). Le sous-ensemble « source par position » est exclu de la fidélité M1 : il passe à `implémenté` au mieux tant que Q1 n'est pas levée.

### D. Séquences nommées et commande `/random`

**R30 (D [E1] pour la syntaxe ; S, documenté [W1], [W2], pour les défauts et la sémantique, défauts recoupés par [R1])** Le monde porte trois réglages de séquences : `salt` (entier 32 bits, défaut 0), `include_world_seed` (défaut vrai), `include_sequence_id` (défaut vrai). Syntaxe extraite [E1] :

- `/random value <range> [<sequence>]` et `/random roll <range> [<sequence>]` : `<range>` de type `minecraft:int_range`, `<sequence>` de type `minecraft:resource_location` ; l'argument `<sequence>` exige le niveau de permission `gamemasters` ;
- `/random reset (* | <sequence>) [<seed> [<includeWorldSeed> [<includeSequenceId>]]]` : `<seed>` de type `brigadier:integer`, les deux drapeaux de type `brigadier:bool` ; toute la branche `reset` exige `gamemasters`.

Selon [W1] : `value` ne répond qu'à l'émetteur, `roll` diffuse ; `reset *` supprime toutes les séquences et, avec `<seed>`, fixe les réglages du monde ; `reset <sequence>` recrée la séquence avec les réglages du monde ou ceux fournis ; une séquence inconnue citée par `value` ou `roll` est créée avec les réglages du monde ; la valeur de retour de `value` et `roll` est l'entier tiré.

**R31 (S, documenté [W2] pour les étapes 1 à 3 ; S, [R1] seul, pour la place du mélange et l'extension du sel)** État initial d'une séquence d'identifiant `id`, avec réglages `(salt, W, I)` et graine du monde `ws` :

1. `v = (W ? ws : 0) XOR salt`, le sel étant étendu en signe à 64 bits (S, [R1] « sel entier signé » ; contre-hypothèse : extension par des zéros, cas N3z) ;
2. `(lo, hi)` = expansion non mélangée de `v` (R22) ;
3. si `I`, `(lo, hi) = (lo XOR md5_lo(id), hi XOR md5_hi(id))` (R26) ;
4. état X128 = `(mélange(lo), mélange(hi))` (R23). [R1] place le mélange après le XOR de l'empreinte ; [W2] cite le mélange sans le placer (S).

Vecteurs V-X5. Sondes : P1 et P2.

**R32 (S, documenté [W2] pour le format ; S, [R1], pour le sens)** Les séquences sont enregistrées dans `data/random_sequences.dat` du monde : NBT compressé GZip, racine `{data: {salt: Int, include_world_seed: Byte, include_sequence_id: Byte, sequences: {<id>: {source: [L; mot_bas, mot_haut]}}}, DataVersion: Int}`. Hypothèse (S, [R1] « état persistant ») : `source` est l'état courant du générateur, et non la graine initiale ; il survit à la sauvegarde et au rechargement, la suite reprenant là où elle s'était arrêtée. Sonde : P1 et P2 (comparaison de `source` avant et après tirages).

**R33 (S)** `/random value min..max <sequence>` (et `roll`) suit la règle d'intervalle R12, appliquée au générateur de la séquence (R18). Cette spec est propriétaire de ce comportement, y compris du cas `min == max` que d'autres specs utilisent (`core-math` y renvoie). Hypothèses à départager par P4 :

- `max - min + 1 >= 2` : un seul tirage borné, valeur `min + nextInt(max - min + 1)` ;
- `min == max` (par exemple `5..5`) : trois issues possibles, à mesurer et non à supposer. H-R33a : commande refusée sans tirage ([W1] : « taille entre 2 et 2147483646 ») ; H-R33b : acceptée, rend `min` sans tirage (R12) ; H-R33c : acceptée, rend `min` après un tirage `nextInt(1)` (un pas sous H-X2). Les messages extraits [E2] (« must be at least 1 ») ne tranchent pas : ils peuvent viser l'écart `max - min` ou la taille ;
- limite haute : [W1] annonce 2147483646 pour la taille, [E2] dit « at most 2147483647 » ; la borne et l'objet mesuré (taille ou écart) sont fixés par P4, de même que le comportement en cas de débordement de `max - min + 1`.

L'extrait [E1] ne contraint pas l'intervalle (`minecraft:int_range` accepte `5..5`) : le refus éventuel vient de l'exécution, ce que P4 observe.

**R34 (S, [R1] chapitre 103 et annexe)** Hors commande, le butin choisit sa source ainsi : une graine explicite non nulle donne un G48 initialisé avec elle (0 signifie « absente ») ; sinon la séquence nommée de la table si elle en déclare une ; sinon la source du niveau. Utile ici pour la sonde P5 ; la règle appartient à la spec loot.

**R35 (S)** Une table de butin dont le pool a `rolls` de type `minecraft:uniform` (format vérifié dans [E4]) tire d'abord le nombre de tirages par R12, avant tout autre tirage du pool. Hypothèse nécessaire à P5, à confirmer par P5 elle-même.

## Entrées / sorties

| Élément | Type | Plage, conventions |
|---|---|---|
| Graine G48 | long signé 64 bits | seuls les 48 bits faibles comptent (R2) |
| Graine X128 | long signé 64 bits, ou état brut 2 x 64 bits | état brut tout nul refusé (R15) |
| Sel de séquence | int signé 32 bits | -2^31 à 2^31-1, étendu en signe (R31, S) |
| Identifiant de séquence | `namespace:path` | forme complète pour l'empreinte (R26, S) |
| `bound` | int | > 0, sinon erreur sans consommation |
| `nextInt()` | int | toute la plage |
| `nextInt(bound)` | int | 0 <= v < bound |
| `nextLong()` | long | toute la plage pour X128 ; pour G48, 2^48 valeurs atteignables seulement |
| `nextFloat()` | float | multiples de 2^-24 dans [0, 1) |
| `nextDouble()` | double | multiples de 2^-53 dans [0, 1) |
| `nextGaussian()` | double | non borné |
| Position (source par position) | trois int | négatifs inclus (R29) |

Exigences d'interface du moteur (pour la testabilité, pas un comportement du jeu) : chaque générateur compte ses pas depuis sa création ou sa dernière initialisation, et ce compteur `steps()` est lisible par les tests ; il va de 0 à `Long.MAX_VALUE` et **sature** à `Long.MAX_VALUE` au lieu de déborder (un `skip(Long.MAX_VALUE)` suivi d'un tirage ne le rend jamais négatif), tandis que l'état du générateur reste exact (pour G48, `skip(n)` équivaut à `skip(n mod 2^48)`, la période étant 2^48 ; vecteur V-L4) ; `skip(n)` existe pour les deux familles ; aucun générateur ne s'appuie sur `java.util.Random`, `Math.random`, `ThreadLocalRandom` ou l'horloge (`docs/ARCHITECTURE.md`). `java.util.Random` reste permis dans les tests comme référence de R1 à R7.

## Cas limites

- Graine G48 0, -1, -2^63, 2^63-1, 2^48 : -2^63 et 2^48 équivalent à 0, 2^63-1 équivaut à -1 (R2, vecteurs V-L1).
- Graine G48 `25214903917` (`0x5DEECE66D`) : état initial nul ; premier `nextInt()` = 0, premier `nextFloat()` = 0.0 exactement (V-L1). Montre que 0.0 est atteignable et que 1.0 ne l'est pas.
- `nextInt(1)` : rend 0, consomme un pas sur G48 (V-L2, colonne « `nextInt()` suivant »).
- `nextInt(2^30)` contre `nextInt(2^30 + 1)` : la première ne rejette jamais, la seconde rejette environ un essai sur deux (V-L2, V-L3 : de 2 à 6 pas sur les graines 0 à 15). `nextInt(715827883)` rejette environ un essai sur trois (V-L3b : de 2 à 6 pas).
- `nextInt(2^31 - 1)` sur G48 : rejet seulement si `next(31) = 2^31 - 1`.
- `nextLong()` G48 avec second mot négatif : la moitié haute n'est pas le premier tirage (R4).
- Gaussienne : la valeur en cache survit aux appels intercalés d'autres tirages ; une réinitialisation la supprime (V-L5, appels 6 à 9). `s = 0` est rejeté.
- Saut de 10^6 pas G48 (V-L4) : la forme fermée et la répétition doivent coïncider.
- X128 état brut `(0, 1)`, `(2^64-1, 2^64-1)` : valides, vecteurs V-X1 ; `(0, 0)` : refusé (R15).
- Mélange de 0 = 0 ; graine X128 0 donne pourtant un état non nul grâce à R22.
- Sel -1 : `0xFFFFFFFFFFFFFFFF` si étendu en signe, `0x00000000FFFFFFFF` sinon (N3 contre N3z).
- Graine du monde négative et sel non nul combinés par XOR, sans addition (R31, cas N7).
- `/random value` avec `min = -2^31` ou `max = 2^31 - 1` : le calcul de `max - min + 1` déborde en 32 bits ; le comportement est fixé par P4, pas supposé.
- `/random value 5..5` (`min == max`) : refus, valeur sans tirage ou valeur avec un tirage (R33, H-R33a à c) ; P4 tranche.
- Source par position avec coordonnées négatives et extrêmes (R29) : couvertes par les vecteurs provisoires V-X6 ; à reprendre avec l'empreinte fidèle quand Q1 sera levée. Collision provisoire connue : `(23901, 0, z)` et `(52826, 683136096, z)`.
- `nextInt(3)` et `nextInt(715827883)` sur G48 : seules bornes où un test d'acceptation non strict diverge (R5, V-L3b).
- Compteur de pas après `skip(Long.MAX_VALUE)` : saturé à `Long.MAX_VALUE`, état exact (V-L4).
- `skip(n)` X128 avec `n` très grand : ne termine pas en pratique (R21, limite connue).
- Identifiant `:test` : normalisé en `minecraft:test` ; identifiants invalides non validés par l'empreinte (R26).

## Vecteurs de test

Dans [core-random-vectors.md](core-random-vectors.md), avec les scripts et la commande de reproduction :

| Bloc | Contenu | Niveau |
|---|---|---|
| V-L1 | 10 graines : `nextInt()` x5, `nextLong()` x3, `nextBoolean()` x16, `nextFloat()` x3, `nextDouble()` x3, `nextGaussian()` x4 | D pour `java.util.Random`, S pour le jeu |
| V-L2 | 4 graines x 12 bornes (1, 2, 3, 7, 10, 16, 26, 100, 715827883, 2^30, 2^30+1, 2^31-1) | idem |
| V-L3 | rejets de `nextInt(2^30+1)` et nombre de pas, graines 0 à 15 | idem (pas : C) |
| V-L3b | rejets de `nextInt(715827883)` (borne obligatoire, R5) : résultat, pas, valeur qu'un test non strict rendrait à tort, graines 0 à 15 | idem (pas et valeur fautive : C) |
| V-L4 | saut de 10^6 pas ; saturation du compteur après `skip(Long.MAX_VALUE)` | C |
| V-L5 | séquence mixte et réinitialisation, graine 12345 (13 pas) | D / S |
| V-L6 | prédiction de P5 | S |
| V-L7 | sensibilité de la gaussienne au logarithme | C |
| V-X1 | xoroshiro128++ sur 4 états bruts, 5 sorties et état final | D |
| V-X2 | mélange R23 | D |
| V-X3 | initialisation par graine 64 bits | C pour la fonction (recoupée JDK), S pour son emploi par le jeu |
| V-X4 | empreintes MD5 et mots selon l'ordre des octets, formes `test` et `:test` normalisées | D (MD5) / S (ordre, normalisation) |
| V-X6 | source par position provisoire (R29), non dorée, et collision connue | C (provisoire) |
| V-X5 | séquences nommées N1 à N7 et contre-hypothèses : état initial, `/random value` prédites, état après 5 tirages | S |
| O-1 à O-5 | emplacements réservés aux vecteurs de l'oracle | M une fois remplis |

Commande de reproduction (calcul) : `jshell -q --add-modules jdk.random --add-exports jdk.random/jdk.random=ALL-UNNAMED legacy.jsh` et `python -I vectors.py`, scripts copiés depuis le fichier de vecteurs.

### Sondes de l'oracle

Préparation commune : serveur 26.3 lancé par `tools/oracle` (EULA acceptée par le propriétaire), monde neuf, `level-seed=12345` dans `server.properties`, RCON avec niveau d'opérateur suffisant. Vérifier d'abord `/seed` = 12345. Les fichiers NBT sont lus par le lecteur propre de `tools/oracle` (VERIFICATION, indépendance de l'instrument).

- **P1 (lève R25, R26, R30, R31, R32 ; vecteur O-1).** Pour chaque cas Nk de V-X5 : `/random reset <id> <salt> <W> <I>` (par exemple `/random reset probe:a 0 false false` pour N1), puis `/save-all flush`, puis lire `data.sequences."<id>".source` dans `world/data/random_sequences.dat`. Comparer à la colonne « état initial ». N7 se joue deux fois, avec `minecraft:test` puis `test`. Si la séquence n'apparaît pas dans le fichier tant qu'aucun tirage n'a eu lieu, passer à P2 et comparer l'état après tirages. Un écart sur N1 seul met en cause l'emploi de R22 à R24 par le jeu (R25) ; N1 juste et N3 faux tranche l'extension du sel ; N1 juste et N4 faux tranche l'ordre des octets (comparer à N4le).
- **P2 (lève R14, R16 à R18, R33 ; vecteur O-2).** Réinitialiser comme en P1, puis cinq fois `/random value 0..2147483645 probe:a` (résultat lu dans le message `Randomized value: %s` ou par `/execute store result storage probe:out v int 1 run random value 0..2147483645 probe:a`), puis `/save-all flush` et relire `source`. Comparer aux colonnes de V-X5 et aux contre-hypothèses H-X2b et H-X2c. L'état après 5 tirages confirme un pas par tirage. Jouer N1, N4 et N6.
- **P3 (lève R18 pour les puissances de deux ; vecteur O-3).** Depuis N1 réinitialisé : dix fois `/random value 0..15 probe:a` ; réinitialiser ; dix fois `/random value 1..6 probe:a`.
- **P4 (lève R33 et le cas `min == max` de R12 ; vecteur O-4).** Deux parties.
  1. Acceptation : `/random value <intervalle>` sans séquence sur chacun des intervalles de O-4 (dont `5..5`) ; noter succès ou message exact d'erreur, et la valeur de retour par `/execute store success` et `store result`.
  2. Consommation du cas `min == max`, seulement si `5..5` est accepté : `/random reset probe:a 0 false false`, puis `/random value 5..5 probe:a`, puis `/random value 0..2147483645 probe:a`. Si la seconde commande rend 2067245245 (première valeur de N1), le cas `5..5` n'a rien consommé (H-R33b) ; si elle rend 390848952 (deuxième valeur de N1), il a consommé un pas (H-R33c, sous H-X2). Confirmer par `source` après `/save-all flush` : état N1 inchangé, ou état N1 avancé d'un pas. Si `5..5` est refusé, vérifier de même que le refus ne consomme rien.
- **P5 (lève R8, R34, R35 ; vecteur O-5).** Datapack `probe` avec `data/probe/loot_table/legacy_rolls.json` = `{"pools": [{"rolls": {"type": "minecraft:uniform", "min": 1, "max": 27}, "entries": [{"type": "minecraft:item", "name": "minecraft:stick"}]}]}`. Pour chaque graine `s` de V-L6 : poser un coffre portant cette table et la graine de butin `s` (champs NBT `LootTable` et `LootTableSeed` ; le premier est présent dans les structures extraites, le second est S), déclencher le remplissage sans joueur et compter le total d'objets. Méthode de comptage : poser sous le coffre un entonnoir orienté vers le bas, au-dessus d'un bloc plein sans inventaire, pour que rien ne quitte le système coffre + entonnoir ; laisser passer au moins 8 ticks ; vérifier par `/data get block <coffre>` que le champ `LootTable` a disparu (remplissage effectué) ; puis lire `Items` du coffre et de l'entonnoir par `/data get block` et sommer les champs de quantité des deux. Ne pas utiliser `/setblock ... destroy` : les objets tomberaient en entités, plus difficiles à compter. Que l'entonnoir déclenche le remplissage est lui-même S ; s'il ne le fait pas, la sonde le montre (champ `LootTable` toujours présent) et il faudra un autre déclencheur. Comparer à V-L6. Le total ne dépend ni du mélange des emplacements ni du découpage en piles, tant qu'il reste au plus 27 piles.
- **P6 (lève R19 H-X3, exploratoire).** Table de butin `probe:float_probe` déclarant `"random_sequence": "probe:f"`, `rolls` constant 1, une entrée unique, fonction `minecraft:set_attributes` avec un seul modificateur dont le montant est `{"type": "minecraft:uniform", "min": 0, "max": 1}` ; `/random reset probe:f 0 false false`, puis `/loot spawn <x> <y> <z> loot probe:float_probe` et lecture du montant par `/data get entity` sur l'objet apparu. Le montant doit égaler l'un des `nextFloat()` prédits depuis l'état N1 (V-X5) ; sa position dans la liste révèle le nombre de tirages consommés avant lui. La sonde n'est concluante que si le montant est conservé sans arrondi supplémentaire (à vérifier sur la sortie).
- **P7 (recoupement indirect de R8, dans la spec worldgen).** Prédiction de [R2] 10.1.2 : graine 42, set `villages`, région (0, 0), candidat au chunk (9, 11). Ce n'est qu'un candidat ; la comparaison avec `/locate` relève de la spec worldgen (section hors périmètre).

## Questions ouvertes

| Id | Règle | Question | Comment la lever |
|---|---|---|---|
| Q1 | R29, R27 (et l'empreinte de position de `core-math`) | Construction fidèle des sources par position et par nom, dont l'empreinte de position | Aucune source autorisée ne la donne. Décision du propriétaire sur ADR 0002 point 2a-bis (appel direct) ; sinon déduction depuis l'état final de chunks générés (spec worldgen), coûteuse. En attendant : comportement provisoire R29, hors fidélité M1 |
| Q2 | R15 | Valeur de remplacement d'un état X128 tout nul | Inaccessible par les initialisations connues ; 2a-bis seulement. Impact pratique nul si R24 et R31 sont confirmées |
| Q3 | R9, R20 | Logarithme de la gaussienne (`StrictMath` ou `Math`) | Pas de sonde directe ; à rechercher par les consommateurs qui utilisent la gaussienne, ou 2a-bis |
| Q4 | R33, R12 | Limites exactes de l'intervalle de `/random value`, acceptation et consommation de `min == max` | P4 |
| Q5 | R26, R31 | Ordre des octets du MD5, texte haché, extension du sel, place du mélange | P1 |
| Q6 | R32 | `source` = état courant ou graine ? | P1 puis P2 |
| Q7 | R28 | Consommation et construction d'une source dérivée | Indirect (worldgen) ou 2a-bis |
| Q8 | R19 | Bits utilisés par `nextBoolean` et `nextDouble` de X128 | Indirect (consommateurs) ou 2a-bis |
| Q9 | R8, R12 | Égalité complète de G48 du jeu avec `java.util.Random` | P5, puis recoupements worldgen (P7) |

### Divergences entre sources

- [R1] chapitre 019 annonce « Statut : M pour les transitions », alors que ses sources sont des inspections du moteur et que son propre catalogue d'essais classe « WG-002 RNG xoroshiro » en `NON_EXECUTE`. Ce « M » n'est pas une observation de l'oracle : R1, R13 et R24 ne s'en prévalent pas.
- [W2] appelle `source` « la graine de la séquence » ; [R1] parle d'un état persistant qui avance. Q6.
- [W2] cite le mélange R23 sans le placer ; [R1] le place après le XOR de l'empreinte. Pas de contradiction, mais [W2] ne suffit pas à lever R31 étape 4.
- [W1] (taille entre 2 et 2147483646) et [E2] (« at least 1 », « at most 2147483647 ») ne décrivent pas la même limite de `/random value`. Q4.
- [R2] décrit la version 1.21.1, [R1] la 26.3 : leurs formules concordent (`AND (2^48-1)` et `mod 2^48` sont équivalents, C), mais [R2] seul détaille xoroshiro, MD5 et mélange, pour une autre version.

## Hors périmètre : à traiter par la spec worldgen via des sondes

Seuls figurent ici des effets observables et des données extraites ; la construction interne de ces graines n'est pas reprise. Tout est S, sauf les valeurs lues dans les fichiers (D).

- **Famille du bruit.** `legacy_random_source` (valeurs D [E3], voir R14) sélectionnerait la famille de générateur du bruit du monde (S). Sonde : comparaison bit à bit de chunks générés par l'oracle et par le moteur, pour une dimension de chaque valeur du drapeau.
- **Placement des structures `random_spread`.** Données D : chaque `worldgen/structure_set/*.json` de type `minecraft:random_spread` fournit `spacing`, `separation` et `salt` (par exemple `villages` : 34, 8, 10387312). Effet attendu (S) : la position candidate d'une structure dépend de la graine du monde, de la région de `spacing` x `spacing` chunks qui la contient (coordonnées négatives comprises) et du sel, et la famille 48 bits y interviendrait ([R1], [R2]). Prédiction vérifiable (S, [R2] 10.1.2) : graine 42, set `villages`, région (0, 0), candidat au chunk (9, 11). Sondes : `/locate structure` sur plusieurs graines ; deux mondes dont les graines diffèrent de 2^48 (par exemple 42 et 42 + 2^48) devraient proposer les mêmes candidats si la famille 48 bits est bien en jeu, sans garantir les mêmes structures acceptées (biomes et relief diffèrent).
- **Décoration et features.** Effet attendu (S, [R1], [R2]) : le résultat de la décoration d'un chunk dépend de la graine du monde, de la position du chunk, de l'étape de décoration et du rang de la feature dans la liste du biome ; changer l'ordre des features d'un biome par datapack change donc le résultat. Sonde : deux datapacks qui ne diffèrent que par l'ordre de deux features, comparaison des chunks produits.
- **Variantes d'un set de structures, sources nommées du bruit** (par exemple `minecraft:aquifer`, `minecraft:ore`, cités par [R2] 2.3) : relèvent des contrats R27 et R28 ; leur construction fidèle est à établir par la spec worldgen.
- **Non documentés par les rapports** : graines des carvers et des grandes features, chunks à slimes.
- **Consommation conditionnelle** : certains consommateurs sautent un tirage selon une probabilité de 0 ou 1 (minerais exposés à l'air, [R1] chapitre 6). Chaque spec consommatrice décrit ses propres règles de consommation ; cette spec ne fournit que le compteur de pas pour les tester.


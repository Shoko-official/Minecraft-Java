# Générateurs pseudo-aléatoires du moteur

Statut : brouillon
Jalon : M1
Risque : A

## Périmètre

Couvert :

- le générateur congruentiel 48 bits compatible avec `java.util.Random`, appelé ici **G48** ;
- le générateur xoroshiro128++, appelé ici **X128** ;
- les tirages dérivés de chacun (entiers avec et sans borne, long, booléen, float, double, gaussienne), leur consommation et le saut de `n` pas ;
- l'expansion d'une graine 64 bits en état 128 bits, l'initialisation depuis une chaîne, les dérivations (fork, fabrique positionnelle) au niveau de leur structure ;
- les séquences aléatoires nommées du jeu et la commande `/random`, qui sont l'interface d'oracle admise au jalon M1 (ADR 0002, tableau 2c).

Non couvert : les graines propres à la génération de monde (décoration, features, placement de structures, sels, choix de famille par réglage de bruit), listées en fin de document pour la spec worldgen ; le choix de la source de butin (spec loot) ; les tables numériques (trigonométrie) et utilitaires mathématiques (autre spec M1).

Vocabulaire :

- **pas** : une transition de l'état du générateur. La **consommation** d'un appel est son nombre de pas.
- Arithmétique « mod 2^64 » : entiers 64 bits en complément à deux, débordement silencieux. `>>>` est le décalage logique, `>>` le décalage arithmétique, `rotl(x, k)` la rotation à gauche de `k` bits sur 64 bits.
- Les mots de 64 bits sont écrits en hexadécimal non signé (`0x...`) ; les valeurs lues dans les fichiers du jeu sont des longs signés.

## Sources

Niveaux de preuve employés :

- **D** : donnée exacte. Soit un fichier extrait du jeu de référence, soit une source publique qui fixe l'algorithme (spécification Java, article ou code de référence des auteurs). Une règle marquée **D (W)** repose sur le wiki communautaire recoupé par un rapport local ; le wiki n'étant pas normatif, ces règles ont une sonde de confirmation prioritaire.
- **M** : observé sur l'oracle. Aucune règle n'est à ce niveau aujourd'hui (oracle non exécutable, EULA non acceptée).
- **C** : calcul dérivé, exécuté par l'analyste (scripts dans [core-random-vectors.md](core-random-vectors.md)).
- **S** : à vérifier. Toute affirmation sur le comportement du jeu qui ne vient que d'un rapport local ou d'une hypothèse de l'analyste.

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
- [E4] `resources/data/minecraft/loot_table/**.json` : 1 520 fichiers portent un champ `random_sequence`.

Rapports locaux (indices, ADR 0002 point 2b) :

- [R1] `docs/reference/Rapport_Integral_Overworld_Minecraft_Java_26_3.md` : chapitres 018 et 019 (sections 2.1 et 2.2, familles aléatoires et transitions), chapitres 103 et 104 (choix de la source de butin, séquences nommées), annexe JSON `loot_random_priority` et `uniform_integer`, synthèse finale (paragraphes sur les deux familles, `random_spread`, graines de décoration).
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

**R6 (D [J])** `nextFloat()` = `next(24) * 2^-24` (un pas, résultat exact en float, dans [0, 1)). `nextDouble()` = `(next(26) * 2^27 + next(27)) * 2^-53` (deux pas, résultat exact en double, dans [0, 1)).

**R7 (D [J])** `nextGaussian()`, méthode polaire de Marsaglia avec une valeur en cache :

- si une valeur est en cache, elle est rendue et retirée, sans pas ;
- sinon répéter : `v1 = 2 * nextDouble() - 1`, `v2 = 2 * nextDouble() - 1`, `s = v1^2 + v2^2`, jusqu'à `0 < s < 1` (quatre pas par essai) ; puis `m = sqrt(-2 * ln(s) / s)` ; mettre `v2 * m` en cache et rendre `v1 * m`.
- Dans `java.util.Random`, `ln` et `sqrt` sont ceux de `StrictMath` (fdlibm).

**R8 (S)** Le générateur 48 bits du jeu suit R1 à R6 : mêmes constantes, même embrouillage de la graine, mêmes assemblages et mêmes rejets. Indices : [R1] chapitre 019 et synthèse, [R2] 2.2.1 et 10.1.2, concordants pour R1 et R2 ; R3 à R6 ne sont qu'évoqués (« reproduire nextInt borné et ses rejets, nextLong, nextFloat, nextDouble »). Sonde : P5 (et P7 en recoupement indirect).

**R9 (S)** La gaussienne du jeu suit l'enchaînement de R7. Le choix du logarithme (`StrictMath` ou `Math`) n'est établi par aucune source autorisée. Ce n'est pas un détail : sur la JVM de référence, les deux variantes diffèrent pour environ une gaussienne sur trente (vecteur V-L7, calcul C). Implémentation provisoire : `StrictMath`, seul choix indépendant de la plateforme. Sonde : aucune directe sans ADR 0002 point 2a-bis (question Q3).

**R10 (C)** Saut : `skip(n)` avance l'état de exactement `n` pas, comme `n` appels à `next(b)`. Il peut se calculer en O(log n) en composant l'application affine `x -> a*x + c` par élévation au carré : `(a1, c1)` puis `(a2, c2)` donnent `(a2*a1, a2*c1 + c2)` mod 2^48. Vecteur V-L4 (graine 0, 10^6 pas).

**R11 (C, depuis R3 à R7)** Consommation par appel :

| Appel | Pas |
|---|---|
| `next(b)`, `nextInt()`, `nextBoolean()`, `nextFloat()` | 1 |
| `nextInt(bound)`, puissance de deux | 1 |
| `nextInt(bound)`, autre borne | k >= 1 (nombre d'essais) ; pour 2^30+1, environ 2 en moyenne (V-L3) |
| `nextLong()`, `nextDouble()` | 2 |
| `nextGaussian()` | 0 si valeur en cache, sinon 4k (k essais) |
| initialisation | 0 (et vide le cache) |

**R12 (S, [R1] annexe `uniform_integer`)** Tirage d'un entier dans un intervalle inclusif `[min, max]` tel que le jeu l'utilise pour ses fournisseurs de nombres : si `min < max`, `min + nextInt(max - min + 1)` ; si `min >= max`, rend `min` sans consommer de pas. Différence avec `java.util.Random` (D [J]) : depuis Java 17, les méthodes à deux bornes de `java.util.Random` (`nextInt(origin, bound)`, `nextLong(bound)`, etc.) viennent d'une interface générique et utilisent d'autres algorithmes et d'autres consommations. Le moteur ne doit ni les appeler ni les imiter. Aucune autre différence observable entre G48 du jeu et `java.util.Random` n'est connue à ce jour (R8, R9 restent S).

### B. Générateur xoroshiro128++ (X128)

**R13 (D [BV])** L'état est un couple de mots de 64 bits `(s0, s1)`, jamais tous deux nuls. Un pas calcule la sortie puis la transition, toutes opérations mod 2^64 :

- sortie `r = rotl(s0 + s1, 17) + s0` ;
- `t = s1 XOR s0` ; `s0' = rotl(s0, 49) XOR t XOR (t << 21)` ; `s1' = rotl(t, 28)`.

La sortie est calculée sur l'état avant la transition. Vecteurs V-X1 (recoupés avec l'implémentation indépendante du JDK).

**R14 (D (W) [W2], recoupé [R1], [R2])** Les séquences aléatoires nommées du jeu (butin, `/random`) utilisent X128. Sonde de confirmation : P2. Indice associé, de niveau S ([R1], voir hors périmètre) : l'état aléatoire du générateur de bruit utilise X128 quand `legacy_random_source` vaut `false` et la famille 48 bits sinon. Valeurs du drapeau (D [E3]) : `false` pour `overworld`, `amplified`, `large_biomes` ; `true` pour `nether`, `end`, `caves`, `floating_islands`.

**R15 (S, [R2] 2.2.2)** Un état tout à zéro est remplacé à la construction par un état non nul fixe, dont la valeur n'est donnée par aucune source autorisée. Calcul (C) : les initialisations R24 et R31 ne peuvent pas produire cet état (le mélange R23 est bijectif et ne s'annule qu'en 0 ; avant mélange, `hi = lo + 0x9E3779B97F4A7C15` interdit `lo = hi = 0`, sauf empreinte MD5 particulière). Choix provisoire du moteur, faute de valeur connue : un état nul fourni directement est une erreur de programmation (exception), ce qui ne se produit dans aucun chemin connu ; à remplacer par le comportement du jeu quand Q2 sera levée.

**R16 à R20 : tirages dérivés de X128.** Aucune source autorisée ne décrit comment le jeu les assemble. Les hypothèses ci-dessous suivent les recommandations publiques [BV] et [L] ; chacune a une contre-hypothèse que la sonde indiquée départage.

**R16 (S, H-X1)** `nextLong()` rend la sortie `r` lue comme long signé. Un pas. Sonde : P1 et P2 (l'état après `n` tirages confirme un pas par tirage).

**R17 (S, H-X1)** `nextInt()` rend les 32 bits de poids faible de `r`, lus comme entier signé. Un pas.

**R18 (S, H-X2)** `nextInt(bound)`, `bound > 0`, méthode de [L] sur les 32 bits de poids faible : `u` = 32 bits faibles de `r` (non signé), `m = u * bound` (64 bits non signé) ; si `m mod 2^32 < bound`, calculer `t = (2^32 - bound) mod bound` et, tant que `m mod 2^32 < t`, retirer `u` sur une nouvelle sortie et recalculer `m` ; rendre `m >>> 32`. Pas de cas particulier pour les puissances de deux. Un pas, rarement plus. Contre-hypothèses : H-X2b (31 bits de poids fort et rejet comme R5), H-X2c (méthode de [L] sur les 32 bits de poids fort). Sondes : P2 et P3, vecteurs V-X5.

**R19 (S, H-X3 à H-X5)** `nextFloat()` = `(r >>> 40) * 2^-24` (H-X3) ; `nextDouble()` = `(r >>> 11) * 2^-53` (H-X4, conversion recommandée par [BV]) ; `nextBoolean()` = bit de poids faible de `r` (H-X5). Un pas chacun. Contre-hypothèse principale : bits de poids fort pour le booléen. Sonde : P6 pour `nextFloat` ; `nextDouble` et `nextBoolean` n'ont pas de sonde directe (Q8).

**R20 (S)** `nextGaussian()` suit l'enchaînement de R7 avec le `nextDouble` de X128 : deux pas par essai, cache vidé à toute réinitialisation. Même incertitude sur le logarithme que R9.

**R21 (C, sous R16 à R20)** Consommation : un pas pour `nextLong`, `nextInt`, `nextBoolean`, `nextFloat`, `nextDouble` ; `k` pas pour `nextInt(bound)` (k = 1 sauf rejet, probabilité inférieure à `bound / 2^32`) ; 0 ou 2k pour la gaussienne. `skip(n)` avance de `n` pas ; contrairement à G48, il n'a pas de forme fermée simple pour un `n` quelconque, on l'exécute pas à pas (les sauts polynomiaux de [BV] ne couvrent que des longueurs fixes, 2^64 et 2^96 pas).

### C. Graines, chaînes et dérivations

**R22 (D (W) [W2], recoupé [R1], [R2] et calcul)** Expansion non mélangée d'une valeur 64 bits `v` en deux mots : `lo = v XOR 0x6A09E667F3BCC909`, `hi = lo + 0x9E3779B97F4A7C15` (mod 2^64). Recoupement (C) : `0x9E3779B97F4A7C15 = floor(2^64 / phi)` (nombre d'or) ; `0x6A09E667F3BCC909 = floor(frac(sqrt 2) * 2^64) + 1`, l'entier impair suivant la partie fractionnaire de racine de 2 (le `+ 1` ne se déduit pas, il vient de [W2]).

**R23 (D [ST], [W2])** Mélange de Stafford, variante 13 (finaliseur de SplitMix64), sur un mot `z` : `z = (z XOR (z >>> 30)) * 0xBF58476D1CE4E5B9` ; `z = (z XOR (z >>> 27)) * 0x94D049BB133111EB` ; résultat `z XOR (z >>> 31)`, mod 2^64. Il est bijectif et vaut 0 en 0. Recoupement (C) : `new java.util.SplittableRandom(0).nextLong()` vaut le mélange de `0x9E3779B97F4A7C15`, soit -2152535657050944081, identique au calcul. Vecteurs V-X2.

**R24 (S, [R2] 2.2.2, [R1] annexe)** Initialiser X128 avec une graine 64 bits `seed` : appliquer R22 à `seed`, puis R23 à chaque mot ; l'état est `(mélange(lo), mélange(hi))`. Vecteurs V-X3. Sonde : P1, cas N1, qui vaut exactement l'initialisation par la graine 0.

**R25 (S)** Réinitialiser une source X128 existante avec une graine 64 bits suit R24 et vide la gaussienne en cache.

**R26 (D (W) [W2] pour MD5 ; S pour le reste)** L'empreinte d'un identifiant est le MD5 (128 bits) de son texte. Hypothèses à lever : le texte est la forme complète `namespace:path` encodée en UTF-8 (`test` et `minecraft:test` donnent la même empreinte, celle de `minecraft:test`) ; les 8 premiers octets, lus en gros-boutiste, forment le mot bas, les 8 suivants le mot haut. Contre-hypothèse : petit-boutiste. Vecteurs V-X4. Sonde : P1, cas N4 contre N4le, et N7.

**R27 (S, [R2] 2.3)** Une source X128 peut être obtenue d'une chaîne au travers d'une fabrique (voir R29) : l'empreinte R26 est combinée avec les deux mots de la fabrique. Hypothèse : combinaison par XOR mot à mot, sans mélange ultérieur, par analogie avec R31. Sonde : indirecte, par la génération de monde (spec worldgen) ; directe seulement avec ADR 0002 point 2a-bis.

**R28 (S, [R1] synthèse « fork selon leur consommation réelle »)** Dériver (« fork ») une source neuve d'une source existante consomme des tirages de la source mère : hypothèse un `nextLong` pour G48 (nouvelle source initialisée avec ce long par R2), deux `nextLong` pour X128 (pris comme état brut). Sonde : indirecte (worldgen).

**R29 (S, [R2] 2.3)** Fabrique positionnelle : construite à partir d'une source, elle fige deux mots `(f_lo, f_hi)` (hypothèse : deux `nextLong` de la source) ; la source pour une position entière `(x, y, z)` combine une empreinte de position `h(x, y, z)` avec un seul de ces mots ([R2] : « un hash des coordonnées est combiné avec un mot de l'état »). La fonction `h`, le mot choisi et l'opération ne sont donnés par aucune source autorisée (question Q1, bloquante pour la partie « RNG positionnels » de M1). Exigence (C) : `h` doit être définie pour toutes les coordonnées `int`, négatives comprises, sans passer par une division.

### D. Séquences nommées et commande `/random`

**R30 (D [E1] pour la syntaxe ; D (W) [W1], [W2] pour les défauts et la sémantique, défauts recoupés par [R1])** Le monde porte trois réglages de séquences : `salt` (entier 32 bits, défaut 0), `include_world_seed` (défaut vrai), `include_sequence_id` (défaut vrai). Syntaxe extraite [E1] :

- `/random value <range> [<sequence>]` et `/random roll <range> [<sequence>]` : `<range>` de type `minecraft:int_range`, `<sequence>` de type `minecraft:resource_location` ; l'argument `<sequence>` exige le niveau de permission `gamemasters` ;
- `/random reset (* | <sequence>) [<seed> [<includeWorldSeed> [<includeSequenceId>]]]` : `<seed>` de type `brigadier:integer`, les deux drapeaux de type `brigadier:bool` ; toute la branche `reset` exige `gamemasters`.

Selon [W1] : `value` ne répond qu'à l'émetteur, `roll` diffuse ; `reset *` supprime toutes les séquences et, avec `<seed>`, fixe les réglages du monde ; `reset <sequence>` recrée la séquence avec les réglages du monde ou ceux fournis ; une séquence inconnue citée par `value` ou `roll` est créée avec les réglages du monde ; la valeur de retour de `value` et `roll` est l'entier tiré.

**R31 (D (W) [W2] pour les étapes 1 à 3 ; S pour l'ordre du mélange et l'extension du sel)** État initial d'une séquence d'identifiant `id`, avec réglages `(salt, W, I)` et graine du monde `ws` :

1. `v = (W ? ws : 0) XOR salt`, le sel étant étendu en signe à 64 bits (S, [R1] « sel entier signé » ; contre-hypothèse : extension par des zéros, cas N3z) ;
2. `(lo, hi)` = expansion non mélangée de `v` (R22) ;
3. si `I`, `(lo, hi) = (lo XOR md5_lo(id), hi XOR md5_hi(id))` (R26) ;
4. état X128 = `(mélange(lo), mélange(hi))` (R23). [R1] place le mélange après le XOR de l'empreinte ; [W2] cite le mélange sans le placer (S).

Vecteurs V-X5. Sondes : P1 et P2.

**R32 (D (W) [W2] pour le format ; S pour le sens)** Les séquences sont enregistrées dans `data/random_sequences.dat` du monde : NBT compressé GZip, racine `{data: {salt: Int, include_world_seed: Byte, include_sequence_id: Byte, sequences: {<id>: {source: [L; mot_bas, mot_haut]}}}, DataVersion: Int}`. Hypothèse (S, [R1] « état persistant ») : `source` est l'état courant du générateur, et non la graine initiale ; il survit à la sauvegarde et au rechargement, la suite reprenant là où elle s'était arrêtée. Sonde : P1 et P2 (comparaison de `source` avant et après tirages).

**R33 (S)** `/random value min..max <sequence>` rend `min + nextInt(max - min + 1)` tiré de la séquence (R18), un seul tirage borné par commande. Limites de l'intervalle : [W1] annonce une taille `max - min + 1` comprise entre 2 et 2147483646, mais les messages extraits [E2] disent « at least 1 » et « at most 2147483647 » ; la borne exacte et l'objet mesuré (taille ou écart) sont à fixer par P4.

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
| Position (fabrique) | trois int | négatifs inclus (R29) |

Exigences d'interface du moteur (pour la testabilité, pas un comportement du jeu) : chaque générateur compte ses pas depuis sa création ou sa dernière initialisation, et ce compteur est lisible par les tests ; `skip(n)` existe pour les deux familles ; aucun générateur ne s'appuie sur `java.util.Random`, `Math.random`, `ThreadLocalRandom` ou l'horloge (`docs/ARCHITECTURE.md`). `java.util.Random` reste permis dans les tests comme référence de R1 à R7.

## Cas limites

- Graine G48 0, -1, -2^63, 2^63-1, 2^48 : -2^63 et 2^48 équivalent à 0, 2^63-1 équivaut à -1 (R2, vecteurs V-L1).
- Graine G48 `25214903917` (`0x5DEECE66D`) : état initial nul ; premier `nextInt()` = 0, premier `nextFloat()` = 0.0 exactement (V-L1). Montre que 0.0 est atteignable et que 1.0 ne l'est pas.
- `nextInt(1)` : rend 0, consomme un pas sur G48 (V-L2, colonne « `nextInt()` suivant »).
- `nextInt(2^30)` contre `nextInt(2^30 + 1)` : la première ne rejette jamais, la seconde rejette environ un essai sur deux (V-L2, V-L3 : de 2 à 6 pas sur les graines 0 à 15).
- `nextInt(2^31 - 1)` sur G48 : rejet seulement si `next(31) = 2^31 - 1`.
- `nextLong()` G48 avec second mot négatif : la moitié haute n'est pas le premier tirage (R4).
- Gaussienne : la valeur en cache survit aux appels intercalés d'autres tirages ; une réinitialisation la supprime (V-L5, appels 6 à 9). `s = 0` est rejeté.
- Saut de 10^6 pas G48 (V-L4) : la forme fermée et la répétition doivent coïncider.
- X128 état brut `(0, 1)`, `(2^64-1, 2^64-1)` : valides, vecteurs V-X1 ; `(0, 0)` : refusé (R15).
- Mélange de 0 = 0 ; graine X128 0 donne pourtant un état non nul grâce à R22.
- Sel -1 : `0xFFFFFFFFFFFFFFFF` si étendu en signe, `0x00000000FFFFFFFF` sinon (N3 contre N3z).
- Graine du monde négative et sel non nul combinés par XOR, sans addition (R31, cas N7).
- `/random value` avec `min = -2^31` ou `max = 2^31 - 1` : le calcul de `max - min + 1` déborde en 32 bits ; le comportement est fixé par P4, pas supposé.
- Coordonnées de fabrique positionnelle négatives (R29) : à couvrir dès que `h` est connue.

## Vecteurs de test

Dans [core-random-vectors.md](core-random-vectors.md), avec les scripts et la commande de reproduction :

| Bloc | Contenu | Niveau |
|---|---|---|
| V-L1 | 10 graines : `nextInt()` x5, `nextLong()` x3, `nextBoolean()` x16, `nextFloat()` x3, `nextDouble()` x3, `nextGaussian()` x4 | D pour `java.util.Random`, S pour le jeu |
| V-L2 | 4 graines x 10 bornes (1, 2, 7, 10, 16, 26, 100, 2^30, 2^30+1, 2^31-1) | idem |
| V-L3 | rejets de `nextInt(2^30+1)` et nombre de pas, graines 0 à 15 | idem (pas : C) |
| V-L4 | saut de 10^6 pas | C |
| V-L5 | séquence mixte et réinitialisation, graine 12345 (13 pas) | D / S |
| V-L6 | prédiction de P5 | S |
| V-L7 | sensibilité de la gaussienne au logarithme | C |
| V-X1 | xoroshiro128++ sur 4 états bruts, 5 sorties et état final | D |
| V-X2 | mélange R23 | D |
| V-X3 | initialisation par graine 64 bits | S |
| V-X4 | empreintes MD5 et mots selon l'ordre des octets | D (MD5) / S (ordre) |
| V-X5 | séquences nommées N1 à N7 et contre-hypothèses : état initial, `/random value` prédites, état après 5 tirages | S |
| O-1 à O-5 | emplacements réservés aux vecteurs de l'oracle | M une fois remplis |

Commande de reproduction (calcul) : `jshell -q --add-modules jdk.random --add-exports jdk.random/jdk.random=ALL-UNNAMED legacy.jsh` et `python -I vectors.py`, scripts copiés depuis le fichier de vecteurs.

### Sondes de l'oracle

Préparation commune : serveur 26.3 lancé par `tools/oracle` (EULA acceptée par le propriétaire), monde neuf, `level-seed=12345` dans `server.properties`, RCON avec niveau d'opérateur suffisant. Vérifier d'abord `/seed` = 12345. Les fichiers NBT sont lus par le lecteur propre de `tools/oracle` (VERIFICATION, indépendance de l'instrument).

- **P1 (lève R22, R24, R26, R31, R32 ; vecteur O-1).** Pour chaque cas Nk de V-X5 : `/random reset <id> <salt> <W> <I>` (par exemple `/random reset probe:a 0 false false` pour N1), puis `/save-all flush`, puis lire `data.sequences."<id>".source` dans `world/data/random_sequences.dat`. Comparer à la colonne « état initial ». N7 se joue deux fois, avec `minecraft:test` puis `test`. Si la séquence n'apparaît pas dans le fichier tant qu'aucun tirage n'a eu lieu, passer à P2 et comparer l'état après tirages. Un écart sur N1 seul met en cause R22 à R24 ; N1 juste et N3 faux tranche l'extension du sel ; N1 juste et N4 faux tranche l'ordre des octets (comparer à N4le).
- **P2 (lève R13 dans le jeu, R16 à R18, R33 ; vecteur O-2).** Réinitialiser comme en P1, puis cinq fois `/random value 0..2147483645 probe:a` (résultat lu dans le message `Randomized value: %s` ou par `/execute store result storage probe:out v int 1 run random value 0..2147483645 probe:a`), puis `/save-all flush` et relire `source`. Comparer aux colonnes de V-X5 et aux contre-hypothèses H-X2b et H-X2c. L'état après 5 tirages confirme un pas par tirage. Jouer N1, N4 et N6.
- **P3 (lève R18 pour les puissances de deux ; vecteur O-3).** Depuis N1 réinitialisé : dix fois `/random value 0..15 probe:a` ; réinitialiser ; dix fois `/random value 1..6 probe:a`.
- **P4 (lève R33 ; vecteur O-4).** `/random value` sans séquence sur chacun des intervalles de O-4 ; noter succès ou message exact d'erreur.
- **P5 (lève R8, R34, R35 ; vecteur O-5).** Datapack `probe` avec `data/probe/loot_table/legacy_rolls.json` = `{"pools": [{"rolls": {"type": "minecraft:uniform", "min": 1, "max": 27}, "entries": [{"type": "minecraft:item", "name": "minecraft:stick"}]}]}`. Pour chaque graine `s` de V-L6 : poser un coffre portant cette table et la graine de butin `s` (champs NBT `LootTable` et `LootTableSeed` ; le premier est présent dans les structures extraites, le second est S), déclencher le remplissage sans joueur (entonnoir posé dessous, ou destruction par `/setblock ... air destroy`) et compter le total d'objets. Comparer à V-L6. Le total ne dépend ni du mélange des emplacements ni du découpage en piles, tant qu'il reste au plus 27 piles.
- **P6 (lève R19 H-X3, exploratoire).** Table de butin `probe:float_probe` déclarant `"random_sequence": "probe:f"`, `rolls` constant 1, une entrée unique, fonction `minecraft:set_attributes` avec un seul modificateur dont le montant est `{"type": "minecraft:uniform", "min": 0, "max": 1}` ; `/random reset probe:f 0 false false`, puis `/loot spawn <x> <y> <z> loot probe:float_probe` et lecture du montant par `/data get entity` sur l'objet apparu. Le montant doit égaler l'un des `nextFloat()` prédits depuis l'état N1 (V-X5) ; sa position dans la liste révèle le nombre de tirages consommés avant lui. La sonde n'est concluante que si le montant est conservé sans arrondi supplémentaire (à vérifier sur la sortie).
- **P7 (recoupement indirect de R8, dans la spec worldgen).** Le candidat `random_spread` calculé par [R2] 10.1.2 (graine 42, région (0, 0), village : chunk (9, 11)) n'est qu'un candidat ; la comparaison avec `/locate` relève de la spec worldgen.

## Questions ouvertes

| Id | Règle | Question | Comment la lever |
|---|---|---|---|
| Q1 | R29, R27 | Empreinte de position `h(x, y, z)`, mot combiné, opération ; combinaison chaîne/fabrique | Aucune source autorisée ne la donne. Décision du propriétaire sur ADR 0002 point 2a-bis (appel direct) ; sinon déduction depuis l'état final de chunks générés (spec worldgen), coûteuse |
| Q2 | R15 | Valeur de remplacement d'un état X128 tout nul | Inaccessible par les initialisations connues ; 2a-bis seulement. Impact pratique nul si R24 et R31 sont confirmées |
| Q3 | R9, R20 | Logarithme de la gaussienne (`StrictMath` ou `Math`) | Pas de sonde directe ; à rechercher par les consommateurs qui utilisent la gaussienne, ou 2a-bis |
| Q4 | R33 | Limites exactes de l'intervalle de `/random value` | P4 |
| Q5 | R26, R31 | Ordre des octets du MD5, texte haché, extension du sel, place du mélange | P1 |
| Q6 | R32 | `source` = état courant ou graine ? | P1 puis P2 |
| Q7 | R28 | Consommation et construction d'un fork | Indirect (worldgen) ou 2a-bis |
| Q8 | R19 | Bits utilisés par `nextBoolean` et `nextDouble` de X128 | Indirect (consommateurs) ou 2a-bis |
| Q9 | R8, R12 | Égalité complète de G48 du jeu avec `java.util.Random` | P5, puis recoupements worldgen (P7) |

### Divergences entre sources

- [R1] chapitre 019 annonce « Statut : M pour les transitions », alors que ses sources sont des inspections du moteur et que son propre catalogue d'essais classe « WG-002 RNG xoroshiro » en `NON_EXECUTE`. Ce « M » n'est pas une observation de l'oracle : R1, R13 et R24 ne s'en prévalent pas.
- [W2] appelle `source` « la graine de la séquence » ; [R1] parle d'un état persistant qui avance. Q6.
- [W2] cite le mélange R23 sans le placer ; [R1] le place après le XOR de l'empreinte. Pas de contradiction, mais [W2] ne suffit pas à lever R31 étape 4.
- [W1] (taille entre 2 et 2147483646) et [E2] (« at least 1 », « at most 2147483647 ») ne décrivent pas la même limite de `/random value`. Q4.
- [R2] décrit la version 1.21.1, [R1] la 26.3 : leurs formules concordent (`AND (2^48-1)` et `mod 2^48` sont équivalents, C), mais [R2] seul détaille xoroshiro, MD5 et mélange, pour une autre version.

## Hors périmètre / pour la spec worldgen

Indices des rapports, tous de niveau S, à reprendre dans la spec worldgen sans les présumer :

- Choix de famille du générateur de bruit par `legacy_random_source` (R14) ; le placement `random_spread` des structures emploie séparément la famille 48 bits, initialisée puis réinitialisée par position ([R1] chapitre 018 et synthèse).
- Graine de placement `random_spread` : graine du monde `+ 341873128712 * rX + 132897987541 * rZ + salt`, en arithmétique 64 bits, avec `rX = floorDiv(chunkX, spacing)` et `rZ = floorDiv(chunkZ, spacing)` ; position candidate = région * spacing + décalage tiré selon le type de dispersion ([R1] synthèse, [R2] 10.1.2). Calcul de contrôle de [R2] : graine 42, région (0, 0), set `villages` (spacing 34, separation 8, salt 10387312), état initial 25206888951, tirages de 31 bits 1588324201 et 1949981161, candidat chunk (9, 11). Conséquence notée par [R2] : des graines congrues modulo 2^48 donnent les mêmes candidats.
- Graine de décoration : repart de la graine du monde, tire deux longs rendus impairs, combine `x * a + z * b` puis XOR graine du monde ([R1] synthèse) ; la signification exacte de `x`, `z` est à établir chez les appelants.
- Graine de feature : graine de décoration `+ index + 10000 * étape`, le produit étant calculé en entier 32 bits avant conversion ([R1] synthèse, [R2] 2.3).
- Choix d'une variante de structure dans un set : famille 48 bits ([R2] 2.4).
- Dérivations nommées du bruit (par exemple `minecraft:aquifer`, `minecraft:ore`) par la fabrique R27 ([R2] 2.3).
- Graines des carvers et des grandes features, chunks à slimes : absents des rapports, à rechercher.
- Rejets conditionnels qui ne consomment pas de tirage (minerais exposés à l'air avec probabilité 0 ou 1, [R1] chapitre 6) : exemple de règle de consommation propre aux consommateurs.

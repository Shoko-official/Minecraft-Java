# NBT binaire et SNBT

Statut : brouillon
Jalon : M1
Risque : B

## Périmètre

Couvre, pour `engine/core` :

- le format binaire NBT de l'édition Java : 13 types de balises, en-têtes, valeurs, listes, tableaux, chaînes, compounds ;
- les conteneurs : brut, gzip, zlib ; racine nommée (fichiers) et racine sans nom (réseau) ;
- les limites et les erreurs de lecture (profondeur, longueurs, octets invalides) ;
- les valeurs particulières (NaN, -0.0, booléens), l'égalité, la lecture typée ;
- le SNBT : lecture (syntaxe complète) et écriture (forme affichée par `/data get`).

Ne couvre pas :

- les chemins NBT (`minecraft:nbt_path`) et leur correspondance partielle (`/execute if data`, filtres de sélecteurs) ;
- le format région Anvil (en-tête, secteurs, octet de compression par chunk) : spec monde, jalon M3 ;
- le schéma des fichiers (level.dat, structures, entités, stockage des commandes) et la mise à niveau entre data versions ;
- les conversions JSON vers NBT des données de jeu ;
- le NBT de l'édition Bedrock (petit-boutiste).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : les 1511 fichiers `resources/data/minecraft/structure/**/*.nbt` ; `generated/reports/commands.json` (types d'arguments des commandes) ; `version.json` (data version 5023).
- **Calculs** : lecteur et écrivain NBT minimal écrit pour cette spec, indépendant du jeu (`nbt_ref.py`, texte intégral et commandes dans `core-nbt-vectors.md`). Sous-commandes `scan`, `order`, `dump`, `ftable`, `tree`, `hex`, `vectors`, `probes`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki, page "NBT format" (https://minecraft.wiki/w/NBT_format ; sa section SNBT est marquée comme incomplète par le wiki) ; Minecraft Wiki, page "Minecraft Wiki:Projects/wiki.vg merge/NBT" (https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/NBT, reprise de l'ancienne page NBT de wiki.vg) pour la racine réseau, les listes de longueur nulle ou négative et la mesure de profondeur ; définition du "modified UTF-8" dans la documentation de l'API Java SE (`java.io.DataInput`) ; RFC 1950 (zlib) et RFC 1952 (gzip).
- **Rapports locaux** : aucun ne décrit le format NBT ni le SNBT (recherche des mots-clés nbt, snbt, utf, gzip, zlib, deflate, endian, level.dat dans les cinq rapports). Le rapport Overworld (chapitre 8.2) cite seulement l'existence des gabarits de structures `.nbt`. Aucune règle ci-dessous n'en vient.
- **Sondes de l'oracle** : aucune exécutée à ce jour. Les sondes proposées sont décrites dans `core-nbt-vectors.md`.

Convention de preuve dans cette spec : **D** = valeur lue telle quelle dans un fichier extrait ; **C** = propriété établie par le décodage et le calcul reproductibles sur ces fichiers ; **M** = non utilisé (oracle pas encore lancé) ; **S** = documentation publique seule, ou hypothèse. Une règle S porte un comportement **provisoire** à implémenter tant que la sonde n'a pas tranché.

## Règles

### Types et valeurs binaires

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Identifiants de type : 0 End, 1 Byte, 2 Short, 3 Int, 4 Long, 5 Float, 6 Double, 8 String, 9 List, 10 Compound, 11 Int_Array. Les 1511 fichiers de référence se décodent entièrement avec ces identifiants (occurrences : Byte 1813, Short 917, Int 4 694 156, Long 226, Float 913, Double 3123, String 107 073, List 1 179 226, Compound 1 222 451, Int_Array 767). | C |
| R2 | Identifiants 7 Byte_Array et 12 Long_Array. Absents des fichiers de référence. | S |
| R3 | Tous les nombres, longueurs comprises, sont gros-boutistes. Entiers signés en complément à deux ; Float et Double au format IEEE 754 binary32 et binary64. Exemple : `DataVersion` vaut `00 00 13 9f` = 5023. | C |
| R4 | Tailles des charges : Byte 1, Short 2, Int 4, Long 8, Float 4, Double 8 octets. | C |
| R5 | Balise nommée = type (1 octet) + nom (codé comme une String, R10) + charge. End est un octet `00` seul, sans nom ni charge. | C |
| R6 | Compound = suite de balises nommées terminée par `00`. Compound vide = `00`. | C |
| R7 | List = type des éléments (1 octet) + longueur (Int signé) + charges des éléments, sans type ni nom. Dans une liste de listes, chaque sous-liste porte son propre type d'élément. | C |
| R8 | Int_Array = longueur (Int signé, en éléments) + éléments de 4 octets. Un UUID est un Int_Array de 4 éléments, poids fort en premier (exemple : `[895037032, -995736946, -1319125422, -1224629455]` = `35592e68-c4a6-428e-b15f-be52b701a331`, UUID version 4 valide). | C |
| R9 | Byte_Array et Long_Array : même forme que R8 avec des éléments de 1 et 8 octets. | S |
| R10 | String = longueur en octets (entier non signé 16 bits) + octets, sans terminateur. Taille maximale encodée : 65535 octets (borne du champ). Seul de l'ASCII est observé dans les fichiers de référence. | C |
| R11 | Encodage des chaînes : "modified UTF-8" de Java SE. U+0001 à U+007F sur 1 octet ; U+0000 et U+0080 à U+07FF sur 2 octets (U+0000 = `c0 80`) ; U+0800 à U+FFFF sur 3 octets ; au-delà de U+FFFF, la paire de substitution UTF-16 est codée en deux fois 3 octets (U+1F600 = `ed a0 bd ed b8 80`). Jamais d'octet `00`, jamais de séquence de 4 octets. | S |
| R12 | Lecture d'une chaîne invalide (octet `00` brut, séquence de 4 octets, séquence tronquée) : provisoire, échec de lecture. Demi-substitut isolé (`ed a0 bd`) : provisoire, accepté tel quel. | S |
| R13 | Écriture d'une chaîne dont l'encodage dépasse 65535 octets : provisoire, échec d'écriture, aucun fichier partiel. | S |

### Racine et conteneurs

| Réf | Règle | Preuve |
|---|---|---|
| R14 | Les 1511 fichiers de structures ont une racine Compound de nom vide (`0a 00 00`), un champ `DataVersion` Int égal à 5023, et aucun octet après la racine. | D |
| R15 | Lecture d'un fichier dont la racine porte un nom non vide : provisoire, accepté, nom ignoré. | S |
| R16 | Les 1511 fichiers sont en gzip (RFC 1952), un seul membre, en-tête de 10 octets `1f 8b 08 00 00 00 00 00 00 ff` (aucun drapeau, horodatage 0, XFL 0, OS 255). | D |
| R17 | Les fichiers de sauvegarde du monde (level.dat, données de joueurs, stockage des commandes) utilisent le même conteneur gzip et une racine nommée comme R14. | S |
| R18 | Les données d'un chunk dans un fichier région sont par défaut en zlib (RFC 1950). Le core fournit le décodage zlib ; le choix par chunk relève de la spec région. | S |
| R19 | Certains fichiers sont stockés sans compression (exemples cités par la documentation : servers.dat, hotbar.nbt, côté client). | S |
| R20 | Réseau, protocole 764 et suivants (777 en 26.3) : racine sans nom, soit le type (1 octet) puis directement la charge ; la documentation précise que ce changement ne concerne que le réseau. Hypothèse non documentée par la page consultée : une valeur absente est un octet `00` seul. | S |

### Listes vides et longueurs

| Réf | Règle | Preuve |
|---|---|---|
| R21 | Dans les fichiers de référence, les 2021 listes vides ont toutes le type d'élément End, et aucune liste de type End n'est non vide. | C |
| R22 | Écriture : une liste vide s'écrit avec le type End (`00 00 00 00 00`), quel que soit le type de liste attendu par le schéma. Établi pour les fichiers de référence ; à confirmer pour les sauvegardes du serveur. | S |
| R23 | Lecture d'une liste vide d'un autre type que End : acceptée, liste vide. | S |
| R24 | Lecture d'une liste de type End de longueur strictement positive : provisoire, échec de lecture. | S |
| R25 | Longueur négative d'une List : provisoire, lue comme une liste vide, quel que soit le type d'élément (la documentation publique admet le type End pour une longueur nulle ou négative et recommande d'accepter tout type quand la longueur est inférieure ou égale à 0). Longueur négative d'un Byte_Array, Int_Array ou Long_Array : provisoire, échec de lecture. | S |

### Compounds et ordre des clés

| Réf | Règle | Preuve |
|---|---|---|
| R26 | Les clés sont uniques dans chaque Compound des fichiers de référence (1 222 451 compounds). | C |
| R27 | Lecture d'une clé en double : provisoire, la dernière valeur lue remplace la précédente. | S |
| R28 | L'ordre des clés n'a pas de sens : deux compounds qui ne diffèrent que par l'ordre sont égaux (R37), et la lecture n'en dépend pas. | S |
| R29 | Ordre observé à l'écriture dans les fichiers de référence : il n'est pas lexicographique (18 856 compounds non triés sur 1 208 339 ayant au moins 2 clés). 1 208 302 d'entre eux (99,997 %) suivent l'ordre non décroissant de `bucket_order_key` (annexe de `core-nbt-vectors.md`) : hachage de chaîne tel que le spécifie l'API Java SE, puis mélange `h ^ (h >>> 16)` et réduction à une table de taille puissance de deux au moins 16 avec un facteur de charge 0,75. Ce mélange et cette réduction reprennent l'implémentation OpenJDK d'une table de hachage ; l'API Java SE, elle, ne spécifie pas l'ordre d'itération. Il s'agit d'une fonction qui reproduit l'ordre observé, pas d'une affirmation sur la façon dont le jeu le produit. Les 37 exceptions sont toutes dans des données d'objets, de pancartes ou d'échanges de villageois. | C |
| R30 | Notre lecteur conserve l'ordre de lecture des clés et notre écrivain le restitue : décoder puis réécrire un fichier de référence redonne exactement ses octets décompressés (vérifié sur les 1511 fichiers). Pour un compound créé par notre moteur, l'ordre d'écriture est l'ordre d'insertion. Reproduire l'ordre de R29 n'est pas exigé en M1 ; les comparaisons avec l'oracle portent sur l'arbre décodé, pas sur les octets. | C |

### Profondeur, taille, erreurs

| Réf | Règle | Preuve |
|---|---|---|
| R31 | Profondeur maximale : 512 niveaux de List et Compound imbriqués. Provisoire : la racine compte comme un niveau (comme dans l'exemple de la documentation, où `{one:{two:{three:{four:{}}}}}` a une profondeur de 5) ; 512 conteneurs imbriqués sont acceptés, 513 sont refusés. La valeur 512 vient de la page "NBT format" ; la page réseau ne donne pas de valeur. Profondeur maximale observée dans les fichiers de référence : 8 conteneurs imbriqués racine comprise. | S |
| R32 | Taille maximale lue : aucune source consultée ne donne de valeur pour 26.3 ; la documentation réseau décrit un quota exprimé en mémoire estimée après décodage, et non en octets lus, sans en donner la valeur. Provisoire : pas de limite pour les fichiers ; limite configurable pour le réseau, valeur à relever au jalon M8. Plus gros fichier de référence : 1 078 387 octets décompressés. | S |
| R33 | Type inconnu (13 ou plus), fin de flux prématurée, Compound sans `00` final : échec de lecture du document entier, sans résultat partiel. | S |
| R34 | Octets après la racine d'un fichier : provisoire, ignorés. | S |

### Valeurs particulières, égalité, lecture typée

| Réf | Règle | Preuve |
|---|---|---|
| R35 | Float et Double gardent leur motif de bits exact en lecture et en réécriture : -0.0 (`80000000`, `8000000000000000`), infinis, NaN avec leur charge. Aucun NaN ni -0.0 dans les fichiers de référence. Provisoire : un NaN produit par calcul s'écrit `7fc00000` (Float) ou `7ff8000000000000` (Double). | S |
| R36 | Pas de type booléen : un booléen est un Byte 0 ou 1. Les champs booléens des fichiers de référence (`OnGround`, `Invulnerable`, `CanPickUpLoot`...) sont des Byte. Provisoire en lecture : toute valeur non nulle vaut vrai. | C (stockage) ; S (lecture) |
| R37 | Égalité de deux balises : même type exigé (Int 1 différent de Byte 1) ; Compound : mêmes clés, valeurs égales, ordre indifférent ; List : mêmes éléments dans le même ordre ; tableaux : élément par élément ; Float et Double comparés par motif de bits (0.0 différent de -0.0, un NaN égal au même motif NaN). Deux listes vides sont égales quel que soit leur type d'élément. | S |
| R38 | Lecture typée tolérante : lire un nombre sous un autre type numérique le convertit ; lire une clé absente ou d'un type incompatible donne la valeur par défaut (0, chaîne vide, liste ou compound vide). Conversion provisoire, alignée sur les conversions primitives du langage Java (spécification publique) : entier vers entier plus large, exact ; entier vers entier plus étroit, on garde les bits de poids faible (Long 3000000000 lu en Int donne -1294967296) ; Float ou Double vers Int ou Long, troncature vers zéro, NaN donne 0, saturation aux bornes ; Float ou Double vers Byte ou Short, d'abord vers Int comme ci-dessus, puis bits de poids faible ; entier vers Float ou Double, valeur la plus proche ; Double vers Float, arrondi IEEE au plus proche. | S |

### SNBT : lecture

| Réf | Règle | Preuve |
|---|---|---|
| R39 | Les commandes reçoivent du SNBT par deux types d'arguments : `minecraft:nbt_compound_tag` (`data merge` block, entity et storage ; `summon` ; arguments de `function`) et `minecraft:nbt_tag` (valeur de `data modify ... value`, 15 branches). | D |
| R40 | `minecraft:nbt_compound_tag` refuse une valeur qui n'est pas un Compound ; `minecraft:nbt_tag` accepte toute valeur. | S |
| R41 | Valeur = Compound, List, tableau typé, nombre, chaîne, booléen ou opération. Les espaces sont permis entre les jetons. Provisoire : espace, tabulation, retour chariot et saut de ligne sont des blancs. | S |
| R42 | Compound : `{}` ou `{clé:valeur, ...}`. Clé = chaîne citée ou non citée (R43). Clé en double : provisoire, la dernière l'emporte. | S |
| R43 | Chaîne non citée : uniquement `0-9 A-Z a-z _ - . +`. Une valeur non citée ne commence ni par un chiffre, ni par `-`, `.` ou `+` (elle est alors lue comme un nombre, ou refusée). Pour une clé, cette restriction du premier caractère n'est pas documentée. Le `:` n'est jamais permis sans guillemets (`minecraft:stone` doit être cité). | S |
| R44 | `true` et `false` non cités donnent Byte 1 et Byte 0 ; cités, ce sont des chaînes. Les autres mots non cités (`NaN`, `Infinity`) sont des chaînes. | S |
| R45 | Chaîne citée par `"` ou `'`. Échappements : `\\`, `\"`, `\'`, `\n`, `\s` (espace), `\t`, `\b`, `\f`, `\r`, `\xhh`, `\uhhhh`, `\Uhhhhhhhh` (au plus U+10FFFF), `\N{nom Unicode}`. Échappement inconnu : erreur. | S |
| R46 | Nombres : entier sans suffixe = Int ; avec `.` ou exposant et sans suffixe = Double. Suffixes de type insensibles à la casse : `b` Byte, `s` Short, `i` Int, `l` Long, `f` Float, `d` Double. Suffixe de signe `s` (signé) ou `u` (non signé) avant le suffixe de type, entiers seulement (`240ub` = Byte -16). Préfixes `0x` et `0b`, entiers seulement. `_` permis entre deux chiffres seulement. Formes `.5`, `5.`, `1e3`, `1.2E-3`. Valeur hors de la plage du type : erreur ; provisoire, un décimal qui déborde vers l'infini (`1.0e400`, `3.5e38f`) est aussi une erreur. Refus documentés : `82u`, `-87uI`, `30bu`, `253sb`. Un hexadécimal de type Byte exige un suffixe de signe (`0x11sb`), `b` étant un chiffre hexadécimal. | S |
| R47 | List : `[]` ou `[v, ...]`. Éléments de même type : List de ce type. Types différents : acceptée ; en binaire, chaque élément est enveloppé dans un Compound à clé vide (`[1,"a"]` devient List de Compound `[{"":1}, {"":"a"}]`). Non documentés, provisoire : `[1b, 2]` n'est pas promu, c'est une liste hétérogène (Byte 1, Int 2) ; dans une liste mixte, les éléments Compound sont enveloppés comme les autres (`[{a:1}, 2]` devient `[{"":{a:1}}, {"":2}]`). | S |
| R48 | Tableaux typés `[B; ...]`, `[I; ...]`, `[L; ...]` : `B` accepte des Byte (pas de booléen), `I` des Byte, Short, Int, `L` des Byte, Short, Int, Long. `[1,2]` est une List d'Int, `[I;1,2]` un Int_Array. Tableau vide `[I;]` : provisoire, accepté (Int_Array de 0 élément). | S |
| R49 | Virgule finale permise dans les compounds, listes et tableaux (`{a:1,}`, `[1,2,]`). | S |
| R50 | Opérations : `bool(x)` (nombre ou booléen vers Byte 0 ou 1 ; chaîne : erreur) ; `uuid("...")` vers Int_Array de 4 (R8). | S |
| R51 | Une entrée invalide fait échouer la commande sans rien modifier. Le texte du message d'erreur n'est pas spécifié en M1. | S |

### SNBT : écriture (texte renvoyé par `/data get`)

| Réf | Règle | Preuve |
|---|---|---|
| R52 | Nombres : Byte `1b`, Short `1s`, Int `1`, Long `1L`, Float `1.5f`, Double `1.5d` (suffixes minuscules sauf `L`). Jamais `true` ni `false`. | S |
| R53 | Décimale des Float et Double, provisoire : la forme de `Float.toString` et `Double.toString` de Java SE (chiffres les plus courts qui relisent la même valeur), puis le suffixe. Pour 10^-3 <= \|x\| < 10^7, écriture décimale avec au moins un chiffre après le point (`1000.0d`, `0.5d`, `5.0f`) ; sinon une mantisse entre 1 et 10 et `E` suivi de l'exposant (`1.0E10f`, `1.0E-4d`, `1.2345678E7d`) ; `-0.0d` ; `NaNd`, `Infinityd`, `-Infinityd` (et `f` pour Float). | S |
| R54 | Chaînes toujours citées. `"` par défaut. Si la chaîne contient un guillemet, le délimiteur est l'autre guillemet que le premier rencontré dans la chaîne. Sont échappés `\` et le délimiteur choisi. Provisoire : tous les autres caractères, non ASCII et contrôles compris, sont écrits tels quels (`"Aé☃"`). | S |
| R55 | Clé non citée si elle n'est faite que de `0-9 A-Z a-z _ - . +`, sinon citée comme R54. | S |
| R56 | Séparateurs : `{a: 1b, b: "x"}`, `[1, 2]`, `[I; 1, 2]` (espace après `:`, `,` et `;`), `{}` et `[]` pour les vides. Clés dans l'ordre du stockage. Provisoire : tableau vide `[I;]` (de même `[B;]`, `[L;]`) ; une liste hétérogène s'affiche sans enveloppe (`[1, "a"]`, `[1b, 2]`). | S |

## Entrées / sorties

- **Types de valeurs** : Byte (-128..127), Short (-32768..32767), Int (32 bits signé), Long (64 bits signé), Float, Double (motif de bits conservé, R35), String (texte Unicode, au plus 65535 octets une fois encodé), Byte_Array, Int_Array, Long_Array (longueur 0..2^31-1, bornée en pratique par la mémoire), List (type d'élément, éléments), Compound (paires clé-valeur, clés uniques, ordre d'insertion conservé).
- **Lecture binaire** : entrée = octets déjà décompressés et mode de racine (nommée ou sans nom) ; sortie = type de racine, nom (mode nommé) et valeur, ou erreur de format portant le décalage en octets du premier octet fautif.
- **Conteneurs** : brut, gzip, zlib, choisis par l'appelant selon le fichier (pas de détection automatique exigée). À l'écriture gzip, en-tête de R16.
- **Écriture binaire** : inverse exacte de la lecture (R30) ; liste vide en type End (R22).
- **SNBT** : lecture d'un texte vers une valeur (règles R41 à R51) ; écriture d'une valeur vers le texte de R52 à R56.

## Cas limites

- Valeurs extrêmes : Byte -128 et 127, Long -2^63 (`80 00 00 00 00 00 00 00`), Int_Array à 0 élément, String vide (`00 00`), String de 65535 octets.
- Liste vide : type End en écriture (R22), tout type en lecture (R23) ; liste de listes vides de types différents.
- Compound vide comme racine (`0a 00 00 00`), racine réseau sans nom (`0a 00`).
- Caractères U+0000, U+00E9, U+20AC, U+1F600 (1 à 6 octets, R11).
- Flottants : -0.0, infinis, NaN canonique et NaN à charge non canonique (R35).
- Profondeur 512 et 513 (R31) ; longueurs négatives (R25) ; type 13 (R33).
- SNBT : bornes `127b`/`128b`, `255ub`/`256ub`, `2147483647`/`2147483648`, chaînes ressemblant à des nombres, `minecraft:stone` non cité, liste hétérogène, tableau vide, virgule finale.

## Vecteurs de test

Détail, octets hexadécimaux et commandes exactes : `core-nbt-vectors.md`. En résumé :

- V01 à V24 : un vecteur binaire construit par type et par cas limite (R1 à R11, R20 à R22, R35).
- E01 à E09 : entrées binaires invalides et résultat provisoire attendu (R12, R24, R25, R31, R33).
- F1 à F4 : quatre fichiers réels (`empty.nbt` en entier, `igloo/top.nbt`, `village/plains/villagers/baby.nbt`, `bastion/hoglin_stable/air_base.nbt`) avec taille, SHA-256 des octets décompressés, racine, clés et nombre d'éléments par liste.
- Corpus : décodage et réécriture à l'identique des 1511 fichiers, statistiques de types et d'ordre des clés (R1, R14, R16, R21, R26, R29, R30).
- T1 à T52 : vecteurs SNBT en lecture et en écriture, tous S, levés par les sondes PS1 et PW1.

## Questions ouvertes

Chaque règle S est levée par une sonde de l'oracle, décrite avec ses commandes dans `core-nbt-vectors.md` :

| Sonde | Ce qu'elle observe | Règles levées |
|---|---|---|
| PB1 | Chargement par `/place template` de 20 fichiers de structure dérivés d'`empty.nbt` et altérés octet par octet (p00 à p19) | R2 (partiel), R12, R15, R23, R24, R25, R27, R31, R32, R33, R34 |
| PW1 | Fichier de stockage des commandes écrit par le serveur après `data modify storage` puis `save-all flush` ; texte de `/data get storage` | R2, R9, R11, R13 (avec PW3), R17, R22, R28, R35 (-0.0), R44 à R56 |
| PW2 | Relecture puis réécriture par le serveur d'un stockage contenant des NaN à charge non canonique | R35 |
| PW3 | Écriture d'une chaîne de plus de 65535 octets par macro de fonction | R13 |
| PE1 | `/execute if data storage` sur des valeurs de types voisins, sur -0.0 et sur des compounds d'ordre différent | R37, R28 |
| PS1 | Acceptation ou refus de chaque entrée SNBT T1 à T52 | R40 à R51 |
| PS2 | Conversion de types à la lecture d'une entité (`summon` avec un champ Int fourni en Double, en Long hors plage, en String ; champ booléen fourni en Byte 2) | R38, R36 (lecture) |
| PZ1 | Décodage d'un fichier `.mca` produit par l'oracle (jalon M3) | R18 |
| PN1 | Trames NBT échangées avec un client vanilla (jalon M8) | R20, R32 (réseau) |

R19 (fichiers client non compressés) n'a pas de sonde avant le travail sur le client ; elle ne bloque pas le jalon M1.

Points sans contradiction mais à surveiller : la documentation publique dit que le jeu a pu écrire des listes vides de type Byte ou End selon les versions (la page réseau précise qu'une implémentation de référence de Mojang utilise le type 1 et le jeu le type End), alors que les 1511 fichiers 26.3 n'en contiennent que de type End (R21, R22) ; elle décrit les compounds comme non ordonnés, alors que les fichiers ont un ordre déterministe non lexicographique (R29).

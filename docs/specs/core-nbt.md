# NBT binaire et SNBT

Statut : brouillon
Jalon : M1
Risque : B
Règles : 63 (R1 à R63). R57 à R63 ont été ajoutées après la relecture du code de l'implémentation ; elles sont placées dans leur section et les numéros existants ne changent pas.

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
- **Calculs** : lecteur et écrivain NBT minimal écrit pour cette spec, indépendant du jeu (`nbt_ref.py`, texte intégral et commandes dans `core-nbt-vectors.md`). Sous-commandes `scan`, `order`, `dump`, `ftable`, `tree`, `hex`, `vectors`, `probes`, `quota`, `containers`, `snbtdepth`.
- **Décisions d'architecture** issues de la relecture du code de l'implémentation : R57 à R63 et les comportements provisoires révisés de R32, R35, R36 et R56. Ce sont des choix de sûreté ou de cohérence, pas des observations du jeu : ils restent S et portent chacun leur sonde quand le jeu peut trancher.
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
| R18 | Les données d'un chunk dans un fichier région sont par défaut en zlib (RFC 1950). Le core fournit le décodage zlib ; le choix par chunk relève de la spec région. Vérification de l'Adler-32 : R61. | S |
| R61 | Vérification du conteneur : en mode gzip ou zlib, après la racine, le lecteur consomme le conteneur jusqu'à sa fin et vérifie le trailer, CRC-32 et ISIZE pour gzip (RFC 1952), Adler-32 pour zlib (RFC 1950). Trailer faux, absent ou tronqué, ou flux DEFLATE non terminé : erreur `CORRUPT_CONTAINER`, aucune valeur rendue. Compatible avec R34, qui porte sur des octets de la charge décompressée et non sur le trailer. Un seul membre gzip est lu ; provisoire : les octets qui suivent la fin du conteneur sont ignorés. Motif mesuré : sans cette vérification, C09 (un octet de charge modifié, CRC d'origine) se lit avec `DataVersion` 5024 au lieu de 5023, sans erreur. Vecteurs C01 à C12. | S |
| R62 | En-tête gzip en lecture : les drapeaux FTEXT, FEXTRA, FNAME, FCOMMENT et FHCRC sont acceptés et leurs champs sautés (le CRC d'en-tête n'est pas vérifié). Bits réservés de FLG (0x20, 0x40, 0x80) : la RFC 1952 impose une erreur ; provisoire : acceptés et ignorés, comme le fait le décodeur gzip du JDK selon la relecture du code (vecteur C06). En-tête écrit : R16. | S |
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
| R57 | Profondeur à l'écriture binaire : avant de produire le moindre octet, l'écrivain calcule la profondeur de la forme binaire (List et Compound imbriqués, racine comprise, enveloppes Compound des listes hétérogènes de R63 comprises). Au-delà de 512 (R31) : erreur `TOO_DEEP` sans décalage, et rien n'est écrit (atomicité, comme R13). Tout ce que l'écrivain produit est donc relisible par le lecteur. Un arbre de 512 niveaux en mémoire dont le niveau 512 est une liste hétérogène fait 513 niveaux en binaire et il est refusé. Motif mesuré : sans cette règle, l'écrivain produit des fichiers que le lecteur refuse, puis échoue par épuisement de la pile vers 1424 niveaux. Vecteurs W1 à W4. | S |
| R32 | Taille maximale lue, provisoire (décision d'architecture) : tout lecteur a un quota par défaut de 134 217 728 unités (128 Mio) de mémoire estimée, calculée selon R60 ; au-delà, erreur `TOO_LARGE`. Le lecteur accepte un quota explicite ; la valeur 2^63-1 signifie l'absence de limite et doit être demandée explicitement. Motif mesuré : sans quota, une liste de 20 000 000 de Byte (19 477 octets en gzip niveau 9) épuise la mémoire, avec une amplification d'environ 25 (un objet balise par élément). Tous les fichiers de référence passent le quota par défaut : le plus lourd, `bastion/hoglin_stable/air_base.nbt` (1 078 387 octets décompressés), compte 5 390 251 unités (C). La documentation réseau décrit elle aussi un quota en mémoire estimée, sans en donner la valeur (sonde PN1). Comportement du jeu pour les fichiers : sonde PB1, p19 et p20. | S |
| R60 | Calcul du quota, provisoire. Unités = 1 par octet consommé dans la charge décompressée, du type de la racine à son dernier octet (nom de la racine compris en mode nommé ; octets après la racine non comptés), + 24 par objet balise. Les objets balises sont la racine, chaque valeur d'entrée de Compound et chaque élément de List. Les éléments d'un Byte_Array, Int_Array ou Long_Array ne sont pas des objets balises : ils comptent pour leurs seuls octets (1, 4 ou 8). End n'est pas un objet balise. Le refus survient dès que le total courant dépasse strictement le quota. Contrôle anticipé : à l'en-tête d'un tableau de longueur n, coût minimal n fois la taille d'élément ; à l'en-tête d'une List de type t et de longueur n, n fois (24 + charge minimale de t), avec Byte 1, Short 2, Int 4, Long 8, Float 4, Double 8, String 2, List 5, Compound 1, tableaux 4, End 0. Si le total courant plus ce coût minimal dépasse le quota : `TOO_LARGE` immédiatement, avant de lire ou d'allouer les éléments, même si l'entrée est tronquée ensuite. Le verdict ne dépend que du total : accepté si et seulement si total <= quota. Calcul sur 64 bits signés ; le coût maximal d'un en-tête, (2^31-1) x 32, ne déborde pas. Exemples (C, `nbt_ref.py quota`) : F1 = 148 + 17 x 24 = 556 ; `{a:[B; n zéros]}` = n + 60 ; `{a:[n Int]}` = 61 + 28n ; p19 = 67 109 452 (accepté : le tableau de 64 Mio compte pour sa taille) ; `{b:[20 000 000 Byte]}` = 500 000 061 (refusé dès l'en-tête de la liste). Vecteurs Q1 à Q9. | S |
| R33 | Type inconnu (13 ou plus), fin de flux prématurée, Compound sans `00` final : échec de lecture du document entier, sans résultat partiel. | S |
| R34 | Octets de la charge décompressée situés après la racine : provisoire, ignorés. Le conteneur est quand même consommé jusqu'à sa fin et son trailer vérifié (R61). Position du flux de l'appelant après lecture : voir le contrat de flux dans Entrées / sorties. | S |

### Valeurs particulières, égalité, lecture typée

| Réf | Règle | Preuve |
|---|---|---|
| R35 | Float et Double gardent leur motif de bits exact en lecture et en réécriture : -0.0 (`80000000`, `8000000000000000`), infinis, NaN avec leur charge. Aucun NaN ni -0.0 dans les fichiers de référence. La lecture puis la réécriture conservent aussi les charges non canoniques (V25, Float `7fc00001` ; V26, Double `7ff0000000000001`). Provisoire : seule la création d'une valeur à partir d'un nombre du moteur canonise un NaN, en `7fc00000` (Float) ou `7ff8000000000000` (Double). | S |
| R36 | Pas de type booléen : un booléen est un Byte 0 ou 1. Les champs booléens des fichiers de référence (`OnGround`, `Invulnerable`, `CanPickUpLoot`...) sont des Byte. Lecture booléenne (lecture typée d'un booléen et opération SNBT `bool()`), provisoire (décision d'architecture) : vrai si la valeur numérique d'origine est différente de 0, sans conversion préalable vers Byte. Byte 2, Int 256, Short -256, Double 0.5 et tout NaN valent vrai ; 0, 0.0 et -0.0 valent faux ; une valeur non numérique (String, List...) vaut faux (défaut de R38). Contradiction : la documentation publique (page "NBT format", modification des données d'entités) dit qu'un nombre est vrai s'il est non nul après arrondi et conversion en Byte, ce qui donnerait faux pour Int 256, Short -256, Double 0.5 et NaN. Sonde PS2, vecteurs B1 à B10. | C (stockage) ; S (lecture) |
| R37 | Égalité de deux balises : même type exigé (Int 1 différent de Byte 1) ; Compound : mêmes clés, valeurs égales, ordre indifférent ; List : mêmes éléments dans le même ordre ; tableaux : élément par élément ; Float et Double comparés par motif de bits (0.0 différent de -0.0, un NaN égal au même motif NaN). Deux listes vides sont égales quel que soit leur type d'élément. | S |
| R38 | Lecture typée tolérante (hors lecture booléenne, R36) : lire un nombre sous un autre type numérique le convertit ; lire une clé absente ou d'un type incompatible donne la valeur par défaut (0, chaîne vide, liste ou compound vide). Conversion provisoire, alignée sur les conversions primitives du langage Java (spécification publique) : entier vers entier plus large, exact ; entier vers entier plus étroit, on garde les bits de poids faible (Long 3000000000 lu en Int donne -1294967296) ; Float ou Double vers Int ou Long, troncature vers zéro, NaN donne 0, saturation aux bornes ; Float ou Double vers Byte ou Short, d'abord vers Int comme ci-dessus, puis bits de poids faible ; entier vers Float ou Double, valeur la plus proche ; Double vers Float, arrondi IEEE au plus proche. | S |

### SNBT : lecture

| Réf | Règle | Preuve |
|---|---|---|
| R39 | Les commandes reçoivent du SNBT par deux types d'arguments : `minecraft:nbt_compound_tag` (`data merge` block, entity et storage ; `summon` ; arguments de `function`) et `minecraft:nbt_tag` (valeur de `data modify ... value`, 15 branches). | D |
| R40 | `minecraft:nbt_compound_tag` refuse une valeur qui n'est pas un Compound ; `minecraft:nbt_tag` accepte toute valeur. | S |
| R41 | Valeur = Compound, List, tableau typé, nombre, chaîne, booléen ou opération. Les espaces sont permis entre les jetons. Provisoire : espace, tabulation, retour chariot et saut de ligne sont des blancs. | S |
| R58 | Profondeur SNBT en lecture, provisoire : au plus 512 niveaux imbriqués, racine comprise. Hors chaînes citées, chaque `{` et chaque `[` (tableaux typés `[B;`, `[I;`, `[L;` compris) ouvre un niveau, et la `(` d'une opération `bool(` ou `uuid(` en ouvre un aussi (R50 : `bool(bool(1))` est une entrée valide, une chaîne d'opérations est donc une imbrication comme une autre). Au-delà : erreur `TOO_DEEP` au décalage du caractère qui ouvre le niveau 513 (décalage compté à partir de 0, en unités de code UTF-16 du texte). Le refus doit survenir sans épuiser la pile, quelle que soit la profondeur du texte (100 000 niveaux compris). Motif mesuré : l'analyseur sans limite échoue par épuisement de la pile vers 2076 niveaux de `[` et 2072 niveaux de `{a:` (environ 4150 caractères), longueur atteignable depuis une commande. Le refus de profondeur prime sur l'erreur de type d'une opération : `bool(` ou `uuid(` imbriqués au-delà de 512 niveaux donnent `TOO_DEEP`, pas `SNBT_INVALID_OPERATION`. Limite réelle du jeu : sonde PS3. Vecteurs T53 à T59. | S |
| R42 | Compound : `{}` ou `{clé:valeur, ...}`. Clé = chaîne citée ou non citée (R43). Clé en double : provisoire, la dernière l'emporte. | S |
| R43 | Chaîne non citée : uniquement `0-9 A-Z a-z _ - . +`. Une valeur non citée ne commence ni par un chiffre, ni par `-`, `.` ou `+` (elle est alors lue comme un nombre, ou refusée). Pour une clé, cette restriction du premier caractère n'est pas documentée. Le `:` n'est jamais permis sans guillemets (`minecraft:stone` doit être cité). | S |
| R44 | `true` et `false` non cités donnent Byte 1 et Byte 0 ; cités, ce sont des chaînes. Les autres mots non cités (`NaN`, `Infinity`) sont des chaînes. | S |
| R45 | Chaîne citée par `"` ou `'`. Échappements : `\\`, `\"`, `\'`, `\n`, `\s` (espace), `\t`, `\b`, `\f`, `\r`, `\xhh`, `\uhhhh`, `\Uhhhhhhhh` (au plus U+10FFFF), `\N{nom Unicode}`. Échappement inconnu : erreur. | S |
| R46 | Nombres : entier sans suffixe = Int ; avec `.` ou exposant et sans suffixe = Double. Suffixes de type insensibles à la casse : `b` Byte, `s` Short, `i` Int, `l` Long, `f` Float, `d` Double. Suffixe de signe `s` (signé) ou `u` (non signé) avant le suffixe de type, entiers seulement (`240ub` = Byte -16). Préfixes `0x` et `0b`, entiers seulement. `_` permis entre deux chiffres seulement. Formes `.5`, `5.`, `1e3`, `1.2E-3`. Valeur hors de la plage du type : erreur ; provisoire, un décimal qui déborde vers l'infini (`1.0e400`, `3.5e38f`) est aussi une erreur. Refus documentés : `82u`, `-87uI`, `30bu`, `253sb`. Un hexadécimal de type Byte exige un suffixe de signe (`0x11sb`), `b` étant un chiffre hexadécimal. | S |
| R47 | List : `[]` ou `[v, ...]`. Éléments de même type : List de ce type. Types différents : acceptée ; en binaire, chaque élément est enveloppé dans un Compound à clé vide (`[1,"a"]` devient List de Compound `[{"":1}, {"":"a"}]`). Non documentés, provisoire : `[1b, 2]` n'est pas promu, c'est une liste hétérogène (Byte 1, Int 2) ; dans une liste mixte, les éléments Compound sont enveloppés comme les autres (`[{a:1}, 2]` devient `[{"":{a:1}}, {"":2}]`). Forme binaire et relecture : R63. | S |
| R63 | Liste hétérogène en binaire : l'écrivain produit une List de Compound où chaque élément est enveloppé dans un Compound à clé vide (`{"": valeur}`, éléments déjà Compound compris). À la relecture, on obtient cette List de Compound enveloppés et non la liste d'origine : l'aller-retour binaire n'est pas réversible. Provisoire : pas de désenveloppement à la lecture. Question ouverte : le jeu désenveloppe-t-il à la lecture ? Sonde PW1, étape de redémarrage. Les enveloppes comptent dans la profondeur binaire (R57). | S |
| R48 | Tableaux typés `[B; ...]`, `[I; ...]`, `[L; ...]` : `B` accepte des Byte (pas de booléen), `I` des Byte, Short, Int, `L` des Byte, Short, Int, Long. `[1,2]` est une List d'Int, `[I;1,2]` un Int_Array. Tableau vide `[I;]` : provisoire, accepté (Int_Array de 0 élément). | S |
| R49 | Virgule finale permise dans les compounds, listes et tableaux (`{a:1,}`, `[1,2,]`). | S |
| R50 | Opérations : `bool(x)` (nombre ou booléen vers Byte 0 ou 1 selon la lecture booléenne de R36 ; chaîne : erreur) ; `uuid("...")` vers Int_Array de 4 (R8). | S |
| R51 | Une entrée invalide fait échouer la commande sans rien modifier. Le texte du message d'erreur n'est pas spécifié en M1. | S |

### SNBT : écriture (texte renvoyé par `/data get`)

| Réf | Règle | Preuve |
|---|---|---|
| R52 | Nombres : Byte `1b`, Short `1s`, Int `1`, Long `1L`, Float `1.5f`, Double `1.5d` (suffixes minuscules sauf `L`). Jamais `true` ni `false`. | S |
| R53 | Décimale des Float et Double, provisoire : la forme de `Float.toString` et `Double.toString` de Java SE (chiffres les plus courts qui relisent la même valeur), puis le suffixe. Pour 10^-3 <= \|x\| < 10^7, écriture décimale avec au moins un chiffre après le point (`1000.0d`, `0.5d`, `5.0f`) ; sinon une mantisse entre 1 et 10 et `E` suivi de l'exposant (`1.0E10f`, `1.0E-4d`, `1.2345678E7d`) ; `-0.0d` ; `NaNd`, `Infinityd`, `-Infinityd` (et `f` pour Float). | S |
| R54 | Chaînes toujours citées. `"` par défaut. Si la chaîne contient un guillemet, le délimiteur est l'autre guillemet que le premier rencontré dans la chaîne. Sont échappés `\` et le délimiteur choisi. Provisoire : tous les autres caractères, non ASCII et contrôles compris, sont écrits tels quels (`"Aé☃"`). | S |
| R55 | Clé non citée si elle n'est faite que de `0-9 A-Z a-z _ - . +`, sinon citée comme R54. | S |
| R56 | Séparateurs : `{a: 1b, b: "x"}`, `[1, 2]`, `[I; 1, 2]` (espace après `:`, `,` et `;`), `{}` et `[]` pour les vides. Clés dans l'ordre du stockage. Provisoire : tableau vide `[I;]` (de même `[B;]`, `[L;]`) ; une liste hétérogène en mémoire s'affiche sans enveloppe (`[1, "a"]`, `[1b, 2]`) ; après un aller-retour binaire, elle s'affiche avec ses enveloppes (`[{"": 1}, {"": "a"}]`, R63). | S |
| R59 | Profondeur SNBT en écriture, provisoire : l'écrivain SNBT refuse un arbre de plus de 512 niveaux, comptés comme en R58 (List, Compound et tableaux typés, sans enveloppes) : erreur `TOO_DEEP` sans décalage, aucun texte produit. Le texte écrit est donc toujours relisible (R58). Conséquence : un arbre binaire valide de 512 conteneurs qui porte un tableau au niveau le plus profond (513 niveaux au sens de R58) ne s'écrit pas en SNBT. | S |

## Entrées / sorties

- **Types de valeurs** : Byte (-128..127), Short (-32768..32767), Int (32 bits signé), Long (64 bits signé), Float, Double (motif de bits conservé, R35), String (texte Unicode, au plus 65535 octets une fois encodé), Byte_Array, Int_Array, Long_Array (longueur 0..2^31-1, bornée en pratique par la mémoire), List (type d'élément, éléments), Compound (paires clé-valeur, clés uniques, ordre d'insertion conservé).
- **Lecture binaire** : entrée = octets déjà décompressés, mode de racine (nommée ou sans nom) et quota (R32, R60 ; défaut 134 217 728 unités) ; sortie = type de racine, nom (mode nommé) et valeur, ou erreur de format portant le décalage en octets du premier octet fautif.
- **Conteneurs** : brut, gzip, zlib, choisis par l'appelant selon le fichier (pas de détection automatique exigée). Trailer vérifié (R61) ; en-tête lu selon R62 ; à l'écriture gzip, en-tête de R16.
- **Écriture binaire** : inverse exacte de la lecture (R30) ; liste vide en type End (R22) ; profondeur contrôlée avant toute écriture (R57) ; liste hétérogène enveloppée (R63).
- **SNBT** : lecture d'un texte vers une valeur (règles R41 à R51, R58) ; écriture d'une valeur vers le texte de R52 à R56 et R59.
- **Catégories d'erreur imposées** : `TOO_DEEP` (R31, R57, R58, R59), `TOO_LARGE` (R32, R60), `CORRUPT_CONTAINER` (R61). Les autres échecs de lecture (R12, R24, R25, R33) n'ont pas de catégorie imposée par cette spec. Le décalage est une position dans la charge décompressée (binaire) ou dans le texte (SNBT, R58) ; les erreurs d'écriture (R13, R57, R59) n'en portent pas.
- **Contrat de flux** : la lecture depuis un flux d'octets peut lire en avance (tampon) ; la position du flux de l'appelant après la lecture est indéfinie, que la lecture réussisse ou échoue. Un usage à trames (réseau, jalon M8) lit depuis un tableau d'octets ou depuis un flux borné à la trame, jamais depuis le flux partagé de la connexion.
- **Notes d'implémentation (sans valeur de règle)** : la lecture d'un tableau peut avoir un pic mémoire de deux copies (octets bruts puis éléments décodés), alors que le quota (R60) n'en compte qu'une ; aucune allocation ne doit dépendre d'une longueur déclarée qui n'a pas passé le contrôle anticipé de R60 (sinon une entrée tronquée de quelques octets annonçant 2^31-1 éléments suffit à épuiser la mémoire, même avec un quota explicite très grand).

## Cas limites

- Valeurs extrêmes : Byte -128 et 127, Long -2^63 (`80 00 00 00 00 00 00 00`), Int_Array à 0 élément, String vide (`00 00`), String de 65535 octets.
- Liste vide : type End en écriture (R22), tout type en lecture (R23) ; liste de listes vides de types différents.
- Compound vide comme racine (`0a 00 00 00`), racine réseau sans nom (`0a 00`).
- Caractères U+0000, U+00E9, U+20AC, U+1F600 (1 à 6 octets, R11).
- Flottants : -0.0, infinis, NaN canonique et NaN à charge non canonique (R35).
- Profondeur 512 et 513 (R31) ; longueurs négatives (R25) ; type 13 (R33).
- Écriture binaire à 512 et 513 niveaux, liste hétérogène au niveau 511 ou 512 (R57) ; SNBT à 512, 513 et 100 000 niveaux, tableau typé au niveau 513, crochets dans une chaîne citée (R58).
- Quota : total égal au quota (accepté), quota + 1 (refusé), longueur déclarée énorme sur une entrée tronquée (refus anticipé), tableau de 64 Mio (accepté), liste de 20 000 000 de Byte (refusée) (R60).
- Conteneurs : CRC, ISIZE et Adler-32 faux, trailer absent ou tronqué, octet de charge modifié, bit réservé de FLG, octets après le conteneur (R61, R62).
- Booléens : Int 256, Short -256, Double 0.5, NaN, -0.0 (R36).
- SNBT : bornes `127b`/`128b`, `255ub`/`256ub`, `2147483647`/`2147483648`, chaînes ressemblant à des nombres, `minecraft:stone` non cité, liste hétérogène, tableau vide, virgule finale.

## Vecteurs de test

Détail, octets hexadécimaux et commandes exactes : `core-nbt-vectors.md`. En résumé :

- V01 à V26 : un vecteur binaire construit par type et par cas limite (R1 à R11, R20 à R22, R35).
- E01 à E09 : entrées binaires invalides et résultat provisoire attendu (R12, R24, R25, R31, R33).
- F1 à F4 : quatre fichiers réels (`empty.nbt` en entier, `igloo/top.nbt`, `village/plains/villagers/baby.nbt`, `bastion/hoglin_stable/air_base.nbt`) avec taille, SHA-256 des octets décompressés, racine, clés et nombre d'éléments par liste.
- Corpus : décodage et réécriture à l'identique des 1511 fichiers, statistiques de types et d'ordre des clés (R1, R14, R16, R21, R26, R29, R30).
- T1 à T52 : vecteurs SNBT en lecture et en écriture, tous S, levés par les sondes PS1 et PW1 ; T53 à T59 : profondeur SNBT (R58), sonde PS3 ; T60 à T62 : `bool()` (R36, R50).
- W1 à W4 : profondeur à l'écriture binaire (R57).
- B1 à B10 : lecture booléenne (R36).
- Q1 à Q9 : quota (R32, R60).
- C01 à C12 : conteneurs gzip et zlib (R61, R62).

## Questions ouvertes

Chaque règle S est levée par une sonde de l'oracle, décrite avec ses commandes dans `core-nbt-vectors.md` :

| Sonde | Ce qu'elle observe | Règles levées |
|---|---|---|
| PB1 | Chargement par `/place template` de 21 fichiers de structure dérivés d'`empty.nbt` et altérés octet par octet (p00 à p20) | R2 (partiel), R12, R15, R23, R24, R25, R27, R31, R32, R33, R34, R60 (valeur du jeu) |
| PB2 | Chargement par `/place template` des conteneurs altérés C02 à C07 et C09 | R61, R62 |
| PW1 | Fichier de stockage des commandes écrit par le serveur après `data modify storage` puis `save-all flush` ; texte de `/data get storage` ; relecture après redémarrage | R2, R9, R11, R13 (avec PW3), R17, R22, R28, R35 (-0.0), R44 à R56, R63 |
| PW2 | Relecture puis réécriture par le serveur d'un stockage contenant des NaN à charge non canonique | R35 |
| PW3 | Écriture d'une chaîne de plus de 65535 octets par macro de fonction | R13 |
| PE1 | `/execute if data storage` sur des valeurs de types voisins, sur -0.0 et sur des compounds d'ordre différent | R37, R28 |
| PS1 | Acceptation ou refus de chaque entrée SNBT T1 à T62 | R40 à R51, R58, R36 (`bool()`) |
| PS2 | Conversion de types à la lecture d'une entité (`summon` avec un champ Int fourni en Double, en Long hors plage, en String ; champ booléen fourni en Byte 2, Int 256, Double 0.5) | R38, R36 (lecture) |
| PS3 | Imbrication croissante dans `data modify storage ... set value`, puis `save-all flush` | R58, R59, R57 (limite du jeu) |
| PZ1 | Décodage d'un fichier `.mca` produit par l'oracle (jalon M3) | R18 |
| PN1 | Trames NBT échangées avec un client vanilla (jalon M8) | R20, R32 (réseau) |

R19 (fichiers client non compressés) n'a pas de sonde avant le travail sur le client ; elle ne bloque pas le jalon M1.

Les règles R57, R59 et R60 décrivent des protections de notre implémentation. Si une sonde montre que le jeu accepte davantage (par exemple la liste de 20 000 000 de Byte de p20), l'écart est documenté comme divergence volontaire de sûreté, pas corrigé d'office.

Contradiction relevée : pour la lecture booléenne, la décision provisoire de R36 (valeur d'origine non nulle) s'oppose à la documentation publique (non nulle après conversion en Byte). PS2 tranche avec Int 256 et Double 0.5.

Points sans contradiction mais à surveiller : la documentation publique dit que le jeu a pu écrire des listes vides de type Byte ou End selon les versions (la page réseau précise qu'une implémentation de référence de Mojang utilise le type 1 et le jeu le type End), alors que les 1511 fichiers 26.3 n'en contiennent que de type End (R21, R22) ; elle décrit les compounds comme non ordonnés, alors que les fichiers ont un ordre déterministe non lexicographique (R29).

# Fichiers de région Anvil et dossiers d'un monde

Statut : brouillon
Jalon : M3
Risque : B

## Périmètre

Couvre, pour `engine/world` :

- l'arborescence d'un monde 26.3 et la place des fichiers de région (`region/`, `entities/`, `poi/`) ;
- le nom `r.<x>.<z>.mca` et l'indexation d'un chunk (coordonnées négatives comprises) ;
- l'en-tête de 8192 octets (emplacements, horodatages), les secteurs de 4096 octets, l'en-tête de chunk (longueur, compression) ;
- les types de compression (gzip, zlib, aucune, LZ4, personnalisée) et les chunks externes `.mcc` ;
- les chunks absents ou invalides, la taille maximale ;
- l'enveloppe des fichiers `entities/` et `poi/`.

Ne couvre pas :

- le contenu NBT d'un chunk : `world-chunk-format.md` ;
- le format NBT et les conteneurs gzip et zlib eux-mêmes : `core-nbt.md` ;
- le schéma de `level.dat`, des données de joueurs et du dossier `data/` ;
- l'ordre d'allocation des secteurs et la défragmentation (non observables, voir R12) ;
- la mise à niveau des mondes au format antérieur à 26.1 (`DIM-1`, `DIM1`, `region/` à la racine).

## Sources

- **Données extraites** (`.cache/reference/26.3/`) : `version.json` (`world_version` 5023) ; `resources/data/minecraft/dimension_type/*.json` (noms des dimensions).
- **Calculs** : `world_ref.py` (annexe de `world-vectors.md`), sous-commandes `coords`, `region`, `dump`, `check`, `lz4`.
- **Documentation publique**, consultée le 2026-10-28 : Minecraft Wiki "Region file format" (en-tête, secteurs, types de compression 1 à 4 et 127, chunks externes) ; Minecraft Wiki "Java Edition level format" et "Java Edition 26.1 Snapshot 6" (dossier `dimensions/`, data version 4774 de ce snapshot) ; Minecraft Wiki "Server.properties" (`region-file-compression`) ; Minecraft Wiki "Java Edition 24w04a" ; Minecraft Wiki "Entity format" (fichiers `entities/`) ; ancienne page wiki.vg "Map Format" (miroir `wikivg.booky.dev`, modifiée le 2024-05-30 : trame LZ4) ; code source public de la bibliothèque lz4-java, `LZ4BlockOutputStream` (format de trame "LZ4Block") ; spécification publique du format de bloc LZ4 et de XXH32 (projets lz4 et xxHash) ; RFC 1950 et RFC 1952.
- **Rapports locaux** (indices) : rapport 1.21.1 (nom `r.<x>.<z>.mca`), rapport Overworld section 1.2 (division vers le bas pour les coordonnées négatives). Aucune règle n'en vient seule.
- **Sondes de l'oracle** : aucune exécutée. Sondes PR1 à PR5 décrites dans `world-vectors.md`.

Niveaux : **D** donnée extraite, ou source publique normative qui fixe un algorithme (RFC, format LZ4 et XXH32, format de trame de lz4-java) ; **C** calcul reproductible ; **M** non utilisé ; **S** documentation publique non normative seule, rapport local ou hypothèse, avec comportement **provisoire**.

## Règles

### Dossiers d'un monde

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Depuis 26.1 Snapshot 6, chaque dimension a son dossier `dimensions/<espace de noms>/<chemin>/` : `dimensions/minecraft/overworld/`, `dimensions/minecraft/the_nether/`, `dimensions/minecraft/the_end/`. Chacun contient `region/` (chunks), `entities/` (entités), `poi/` (points d'intérêt) et `data/` (données propres à la dimension). | S |
| R2 | À la racine du monde : `level.dat` (gzip, `core-nbt.md` R17), `level.dat_old`, `session.lock` (3 octets `e2 98 83`, le caractère U+2603 en UTF-8), `data/` (données partagées, rangées par espace de noms, par exemple `data/minecraft/`), `players/` (`players/data`, `players/stats`, `players/advancements`), `datapacks/`. | S |
| R3 | L'ancienne disposition (`region/`, `entities/`, `poi/` à la racine pour l'Overworld, `DIM-1/` pour le Nether, `DIM1/` pour l'End) appartient aux versions antérieures à 26.1 Snapshot 6. **Provisoire** : le moteur ne l'écrit jamais et refuse de l'ouvrir (mise à niveau hors périmètre). | S |

### Nom et indexation

| Réf | Règle | Preuve |
|---|---|---|
| R4 | Un fichier de région couvre 32 x 32 chunks. Nom : `r.<rx>.<rz>.mca`, `rx = cx >> 5`, `rz = cz >> 5` (décalage arithmétique, donc arrondi vers le bas), écrits en décimal avec signe `-` éventuel. Depuis un bloc : `rx = x >> 9`. | S |
| R5 | Index du chunk dans l'en-tête : `i = (cx & 31) + 32 * (cz & 31)`, de 0 à 1023. Emplacement aux octets `4i` à `4i + 3`, horodatage aux octets `4096 + 4i` à `4096 + 4i + 3`. | S |
| R6 | Vecteurs (C sur R4, R5) : chunk (-1, -1) -> `r.-1.-1.mca`, local (31, 31), index 1023, octet 4092 ; chunk (-32, 0) -> `r.-1.0.mca`, index 0 ; chunk (-33, 31) -> `r.-2.0.mca`, index 1023 ; chunk (30, -3) -> `r.0.-1.mca`, local (30, 29), index 958 ; chunk (1500, -600) -> `r.46.-19.mca`, index 284. | C sur S |

### En-tête et secteurs

| Réf | Règle | Preuve |
|---|---|---|
| R7 | Un secteur fait 4096 octets. Les secteurs 0 et 1 forment l'en-tête de 8192 octets : table des emplacements (1024 entiers 32 bits gros-boutistes) puis table des horodatages (1024 entiers 32 bits gros-boutistes). La taille du fichier est un multiple de 4096. | S |
| R8 | Emplacement = `(offset << 8) | count` : `offset` sur 3 octets (numéro du premier secteur du chunk depuis le début du fichier), `count` sur 1 octet (nombre de secteurs réservés). Emplacement nul : chunk absent. | S |
| R9 | Horodatage : **provisoire** secondes depuis l'époque Unix de la dernière écriture du chunk, entier 32 bits non signé. Le moteur l'écrit mais ne s'en sert pas. | S |
| R10 | Enregistrement d'un chunk au secteur `offset` : longueur `L` (Int gros-boutiste signé, nombre d'octets qui suivent ce champ, octet de compression compris), octet de compression `t`, puis `L - 1` octets de données compressées. Le reste du dernier secteur est un bourrage (zéros à l'écriture, provisoire ; ignoré à la lecture). | S |
| R11 | Taille maximale dans le fichier : `count <= 255`, donc enregistrement de 1 044 480 octets au plus, `L <= 1 044 476`, données compressées de 1 044 475 octets au plus. Au-delà : chunk externe (R17). Taille maximale adressable d'un fichier : 2^24 secteurs (64 Gio). | C sur S |
| R12 | Allocation : un chunk occupe `ceil((L + 4) / 4096)` secteurs consécutifs, après l'en-tête, sans chevaucher un autre chunk. Le choix des secteurs (premier trou libre ou fin du fichier) n'est pas observable par le jeu : le moteur choisit librement, et les comparaisons portent sur les chunks décodés, pas sur les octets du fichier. | S |

### Compression

| Réf | Règle | Preuve |
|---|---|---|
| R13 | Valeurs de `t` : 1 gzip (RFC 1952) ; 2 zlib (RFC 1950) ; 3 aucune (NBT brut) ; 4 LZ4 (depuis 24w04a) ; 127 personnalisée (depuis 24w05a, réservée aux serveurs tiers : une chaîne avec espace de noms, précédée de sa longueur sur 16 bits non signés, nomme l'algorithme, suivie des données). | S |
| R14 | Choix à l'écriture par `region-file-compression` de `server.properties` : `deflate` (défaut) -> 2, `lz4` -> 4, `none` -> 3. Un changement de réglage ne recompresse pas les chunks existants : un même fichier peut mélanger les types. Le lecteur accepte donc 1, 2, 3 et 4 dans tout fichier. Type 127 ou inconnu : **provisoire**, chunk refusé à la lecture (erreur), fichier non modifié. | S |
| R15 | Trame LZ4Block de lz4-java : suite de blocs, chacun avec un en-tête de 21 octets : `LZ4Block` (8 octets ASCII), jeton (1 octet : méthode `0x10` brute ou `0x20` LZ4, OU niveau = `max(0, ceil(log2(taille de bloc)) - 10)`), longueur compressée, longueur décompressée, somme de contrôle (trois entiers 32 bits **petit-boutistes**), puis les données. Somme : XXH32 des données décompressées du bloc, graine `0x9747b28c`. Fin de flux : un en-tête de méthode brute dont les trois entiers valent 0. Un bloc est écrit brut si la compression ne le réduit pas. Taille de bloc par défaut 65 536 octets (niveau 6, jetons `0x16` et `0x26`). Ce format n'est pas la trame standard LZ4 Frame. | D |
| R16 | Le type 4 utilise la trame de R15. **Provisoire** : taille de bloc 65 536 octets ; somme de contrôle comparée et écrite sur ses 28 bits de poids faible (`& 0x0FFFFFFF`, d'après wiki.vg) ; le lecteur accepte toute taille de bloc et les deux méthodes. | S |

### Chunks externes

| Réf | Règle | Preuve |
|---|---|---|
| R17 | Si les données compressées dépassent R11, l'enregistrement dans le fichier de région ne contient que `L = 1` et `t + 128` (par exemple `00 00 00 01 82` pour zlib), et les données sont dans `c.<cx>.<cz>.mcc` (coordonnées absolues du chunk), dans le même dossier. **Provisoire** : le fichier `.mcc` contient seulement le flux compressé, sans longueur ni octet de type. | S |
| R18 | Fichier `.mcc` absent alors que l'enregistrement le demande : **provisoire**, chunk refusé à la lecture (erreur). À la réécriture d'un chunk redevenu assez petit, le `.mcc` est supprimé (provisoire). | S |

### Chunks absents ou invalides

| Réf | Règle | Preuve |
|---|---|---|
| R19 | Chunk absent (emplacement nul, ou fichier de région inexistant) : le chunk n'existe pas et sera généré. Ce n'est pas une erreur. | S |
| R20 | Emplacement invalide : `offset < 2` (dans l'en-tête), `count = 0` avec `offset` non nul, secteurs au-delà de la fin du fichier, `L <= 0`, `L + 4 > count * 4096`, flux qui ne se décompresse pas. **Provisoire** : chunk traité comme absent avec un avertissement, fichier non modifié avant la première réécriture du chunk. | S |
| R21 | `xPos` et `zPos` du NBT différents du chunk attendu à cet index : **provisoire**, chunk traité comme absent avec un avertissement. | S |

### Autres fichiers de région

| Réf | Règle | Preuve |
|---|---|---|
| R22 | `entities/r.<rx>.<rz>.mca` : même conteneur (R4 à R20). Racine NBT : `DataVersion` Int, `Position` Int_Array de 2 éléments `[cx, cz]`, `Entities` List de Compound (données d'entités opaques en M3). | S |
| R23 | `poi/r.<rx>.<rz>.mca` : même conteneur. Racine : `DataVersion` Int, `Sections` Compound dont les clés sont les Y de section en texte décimal (`"-4"`, `"0"`), chaque valeur avec `Valid` Byte et `Records` List de Compound (`pos` Int_Array `[x, y, z]`, `type` String, `free_tickets` Int). | S |

### Lien avec `core-nbt`

| Réf | Règle | Preuve |
|---|---|---|
| R24 | Après décompression (types 1, 2, 4) ou directement (type 3), les données forment un seul fichier NBT à racine Compound nommée (`core-nbt.md` R5, R14, R15), sans octet après la racine. **Provisoire** : nom de racine vide (`0a 00 00`). Les conteneurs zlib (R13, type 2) et gzip (type 1) sont ceux de `core-nbt.md` R16 et R18 ; le décodage LZ4 (R15) est fourni par `engine/world`. | S |
| R25 | Un chunk utilise Byte_Array (lumière) et Long_Array (palettes, heightmaps), deux types absents des gabarits de référence et encore S dans `core-nbt.md` (R2, R9). La sonde PC1 les lève en même temps. | S |

## Entrées / sorties

- **Lecture** : entrée = dossier de dimension et coordonnées de chunk `(cx, cz)` (Int signés) ; sortie = octets NBT décompressés, ou "absent" (R19, R20, R21), ou erreur (R14, R18). Le lecteur n'écrit jamais pendant une lecture.
- **Écriture** : entrée = coordonnées et octets NBT ; type de compression configurable (2 par défaut, 3 et 4 disponibles) ; le fichier reste un multiple de 4096 octets, l'emplacement et l'horodatage sont mis à jour après l'écriture des données (ordre provisoire, protège d'une coupure).
- **Coordonnées de région** : `rx`, `rz` Int signés ; plage utile `[-58 594, 58 593]` pour un monde de 30 millions de blocs de rayon (`30 000 000 >> 9`, C).

## Cas limites

- Chunks négatifs : (-1, -1) est à l'index 1023 de `r.-1.-1.mca`, pas à l'index 0 de `r.0.0.mca` (R4, R5) ; (-32, -32) est à l'index 0 de `r.-1.-1.mca`.
- Enregistrement de plus d'un secteur : `count` > 1, données qui continuent dans les secteurs suivants (vecteur RG1, variante `none`).
- `L` exactement 1 044 476 : dernier cas interne ; un octet de plus : chunk externe.
- Fichier tronqué au milieu d'un chunk : R20.
- Mélange de types de compression dans un même fichier (R14).
- Fichier de région de 8192 octets avec toutes les entrées nulles : valide, aucun chunk.

## Vecteurs de test

Voir `world-vectors.md` : index de région (CO1), fichier synthétique de 2 chunks et ses variantes de compression (RG1 à RG3), trame LZ4 (LZ1), sondes PR1 à PR5.

## Questions ouvertes

- R1 à R3 : arborescence produite par le serveur 26.3. Sonde PR1 (lister le monde après `save-all flush`). Le brief initial cite `DIM-1`, `DIM1` et `region/` à la racine : c'est l'ancienne disposition (R3), à confirmer.
- R4 à R12, R24 : PR1 (fichiers réels) et PR3 (relecture de nos fichiers par le serveur).
- R13, R14, R16 : PR2 (`region-file-compression` = `lz4` puis `none`).
- R17, R18 : PR4 (chunk de plus de 1 Mio en mode `none`).
- R19 à R21 : PR5 (fichiers altérés relus par le serveur).
- R22, R23 : PR1 (décodage de `entities/` et `poi/`).

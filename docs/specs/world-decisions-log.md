# Module world : journal des décisions de la révision du 2026-10-28

Statut : brouillon
Jalon : M3
Risque : B

Compagnon de `world-block-states.md` (BS), `world-chunk-format.md` (CF), `world-anvil-region.md` (AR) et `world-vectors.md`. Ce journal liste les ambiguïtés que l'implémentation du squelette a révélées avant l'écriture des tests en aveugle, la décision prise pour chacune, son niveau de preuve et la sonde qui la lèvera. Les règles elles-mêmes sont dans les specs ; ce fichier garde le raisonnement et l'alternative écartée.

## Méthode et limites

- Sources : les specs et ADR de l'export, le wiki public (Minecraft Wiki, pages "Chunk format" révision du 2026-10-25 et "Region file format", consultées le 2026-10-28), rien d'autre. Aucune donnée extraite n'est disponible dans l'export (`.cache` absent), et aucune ne concerne ces points (les gabarits de structures n'ont ni `PostProcessing` ni chunk).
- Aucune règle de ce journal n'est M : l'oracle n'a pas été lancé (EULA non acceptée, ADR 0002). Toutes sont S, avec un comportement provisoire écrit dans la spec et une sonde.
- Une recherche web a renvoyé des pages de documentation d'API issues de mappings communautaires (hors 2a-bis). Elles n'ont pas été ouvertes et rien n'en vient.
- Aucune décision n'a été bloquée par l'absence de 2a-bis : 2a-bis donnerait des noms et des signatures, et ces points sont des comportements. Ils ne se lèvent que par les sondes de l'oracle, donc après l'acceptation de l'EULA.

## Table des décisions

| Id | Ambiguïté | Décision | Règles | Preuve | Sonde et vecteurs |
|---|---|---|---|---|---|
| D1 | Ordre des bits de `PostProcessing` : CF-R24 disait `x \| z<<4 \| y<<8`, `core-math.md` R32 dit `z<<8 \| y<<4 \| x` | `x \| y<<4 \| z<<8` (ordre `0ZYX`). L'ancienne valeur de CF-R24 est abandonnée | CF-R24 | S | PC6 (six ordres, critère explicite) ; PP1, RG4 `postprocessing` |
| D2 | `SkyLight` absent : à quel moment s'applique la règle par défaut | À la demande, comme vue calculée : ni à la lecture, ni à l'écriture. L'absence est conservée | CF-R15, CF-R30 | S | PC7 ; LI2, RG4 `skylight_chain` |
| D3 | Sections hors de la hauteur du monde : réécriture | Les sections de Y `sMin - 1` et `sMax + 1` sont acceptées et réécrites telles quelles ; le moteur n'en crée pas. Les Y plus éloignés sont ignorés avec un avertissement et non réécrits | CF-R15, CF-R31, CF-R32 e | S | PB3 `light_sections`, `far_sections` |
| D4 | Liste de sections incomplète ou incohérente, `yPos` incohérent | Complétée si lacunaire ; refusée si `Y` manque ou est en double, ou si la clé `sections` manque ; `yPos` informatif, jamais cause de refus | CF-R22, CF-R32, CF-R33 | S | PB3 (sept variantes) ; RG4 |
| D5 | Flux décompressé mais NBT invalide, racine non Compound, nom de racine non vide | NBT invalide ou racine non Compound : ABSENT. Nom de racine non vide et octets après la racine : acceptés avec avertissement. Quota ou profondeur NBT dépassés : REFUSÉ | AR-R20, AR-R24, AR-R30 | S | PR6 ; RG4 |
| D6 | Limite de taille à la décompression | 134 217 728 octets de sortie (2^27, le quota NBT par défaut), paramétrable, contrôlée en flux ; dépassement = REFUSÉ TOO_LARGE | AR-R29 | S (justification C sur S) | aucune sonde ne donne la limite du jeu ; LIM1, RG4 `bomb`, PR6 |
| D7 | Sauvegarde d'une position refusée | Erreur explicite WRITE_ON_REFUSED_POSITION, aucun octet écrit ; marque de session | AR-R28 | S (choix du moteur) | aucune (non observable sur le serveur) |
| D8 | Forme de la lecture d'un chunk | Trois issues PRÉSENT, ABSENT, REFUSÉ, avec causes imposées et avertissements ; les pannes d'entrée-sortie sont à part (exception) | AR-R26, AR-R27, CF Entrées / sorties | S (choix du moteur) | PB2, PB3, PR4, PR5, PR6 |
| D9 | Heightmap de longueur inattendue | État INCONNU explicite, exposé par trois canaux ; jamais de zéros inventés ; non réécrit | CF-R19, CF-R34 | S (longueur attendue : C sur S) | PB3 `bad_heightmap_length`, `heightmap_overflow` ; HM4 |
| D10 | Décisions ajoutées pendant la formalisation (non demandées) | Champs obligatoires de la racine ; classement des entrées de palette invalides | CF-R35, BS-R22 | S (choix du moteur) | PC6 (inventaire des clés), PB1 |
| D11 | Comportements ajoutés par les corrections de la revue du module, absents des specs (2026-10-29) | Consignés en AR-R31 à AR-R38 : limite à l'écriture, fichiers non réguliers, plafond de 256 fichiers ouverts, noms de dimension sûrs, `/` refusé, `.mcc` atomique, borne d'expansion LZ4, mémoire de la décompression | AR-R31 à R38 | S-arch sauf AR-R35 (S) | PR7 pour AR-R35 ; aucune pour les S-arch |

## D1 : ordre des bits de `PostProcessing`

- **Contradiction.** CF-R24 écrivait `lx | (lz << 4) | (ly << 8)` en le justifiant par "même ordre que R4". `core-math.md` R32 (fusionnée) donne `z << 8 | y << 4 | x` (forme `0ZYX`). Le code de `engine/core` suit core-math selon le brief ; il n'a pas été lu.
- **Évidence.** Le wiki "Chunk format" ne donne l'ordre que par les lettres : à la section "ToBeTicked", l'ordre d'empaquetage des coordonnées est donné comme `0ZYX` ; la description de `PostProcessing` renvoie à cette section. Lu du poids fort au poids faible (convention qui donne R4 : `YZX` place y en haut), c'est x en bas, y au milieu, z en haut. Rien dans le wiki ne donne `YZX` pour ce champ, et aucun exemple chiffré n'existe. Les deux specs citent la même page : ce n'est pas une double confirmation.
- **Décision.** `x | (y << 4) | (z << 8)`. Les trois ordres coïncident quand `ly = lz`, ce qui explique qu'une erreur passe inaperçue sur des positions symétriques (le vecteur PP1 utilise (3, 5, 9) : 2387, contre 1427 pour l'index de bloc).
- **Correction à apporter à core-math.** Aucune au niveau du code ni de la formule : la spec core-math et le code décrit dans le brief sont conformes au wiki, c'est la spec world qui avait tort, et elle est corrigée ici. Deux retouches de texte sont proposées à `core-math.md`, non appliquées (hors des quatre specs de ce lot) : (1) R32, dernière phrase, "Confirmation des deux premiers : P30" ne peut pas valoir pour le deuxième point, car `PostProcessing` n'est pas produit par une commande (`setblock` ne remplit que des chunks complets) ; le renvoyer à la sonde PC6 de `world-vectors.md` et garder R32 deuxième puce en S ; (2) V4, colonne PostProcessing : les cinq valeurs sont reproduites par PP1, sans changement.
- **Si la sonde dément.** PC6 décode les Short de proto-chunks de l'oracle selon les six permutations des quartets ; la bonne est celle qui désigne des blocs non air (fluides, blocs dépendant de leurs voisins). Si ce n'est pas `x|y<<4|z<<8`, CF-R24, `core-math.md` R32 et V4, et la fonction de core changent ensemble.
- **Alternative écartée.** Garder l'ancienne valeur : elle ne reposait que sur une analogie avec R4 et contredisait la seule source citée.

## D2 : moment d'application de la lumière du ciel par défaut

- **Options.** (i) À la lecture : on remplit un tableau pour chaque section sans `SkyLight`. (ii) À l'écriture : on écrit les tableaux déduits. (iii) À la demande : une vue calculée.
- **Décision (iii).** L'absence est un état distinct de "tout à 0". Pas de matérialisation à la lecture (sinon un chunk relu puis réécrit sans changement grossirait de 2 Kio par section et ne serait plus identique) ni à l'écriture (le moteur de lumière de M5 écrira explicitement les sections qu'il calcule). La vue reflète l'état courant : si la couche du bas de la section du dessus change, la vue d'une section sans tableau change aussi, ce qui est le comportement voulu tant que le moteur de lumière n'a pas écrit de tableau.
- **Hypothèse non démontrée.** Le wiki dit de regarder la lumière de la section du dessus et de traiter le sommet comme pleinement éclairé. "Couche du bas répétée 16 fois" (valeur héritée de la spec d'origine) et "tableau entier recopié" sont deux lectures de cette phrase. La première est retenue car la lumière du ciel tombe sans atténuation dans l'air, mais rien ne la prouve. LI2 sépare les deux lectures (octet 128 : `10` ou `33`) et PC7 observe ce que le serveur réécrit.
- **Écart de périmètre.** La section de Y 20 (lumière) compte comme "section du dessus" de la 19 si elle existe (CF-R31).

## D3 et D4 : sections hors hauteur, liste incomplète, `yPos`

- **Principe suivi.** Tolérer ce qui est redondant ou reconstructible, refuser ce qui rend une position ambiguë ou le chunk méconnaissable. Une section vide manquante se reconstruit (air, plains), un `Y` manquant ou en double non.
- **Sections de lumière.** Le wiki indique qu'une section supplémentaire peut exister en haut et en bas pour stocker la lumière. La supprimer à la réécriture perdrait de la lumière que le serveur utilise ; la créer sans savoir si le serveur l'exige serait inventer. Décision : les conserver si elles existent, ne jamais en créer.
- **Y éloigné.** Ignoré avec un avertissement et non réécrit, par cohérence avec CF-R21 (le lecteur ignore ce qu'il ne connaît pas et l'écrivain n'écrit que ce qu'il sait produire). Alternative écartée : refuser le chunk, trop strict pour un monde dont le type de dimension a changé de hauteur.
- **Liste incomplète.** Alternative écartée : refuser. Le wiki dit que toutes les sections sont présentes, ce qui décrit ce que le jeu écrit, pas ce qu'il tolère ; sans observation, refuser bloquerait des mondes pour des lacunes bénignes. Le choix est réversible par PB3.
- **Asymétrie assumée.** Une liste `sections` vide est complétée, une clé `sections` absente est refusée. À revoir avec l'inventaire des proto-chunks précoces de PC6.
- **`yPos`.** Ajouté en 21w43a selon le wiki, redondant avec le type de dimension : informatif.

## D5 : NBT illisible

- **Partition.** Ce qui ne donne aucun document NBT (flux qui ne se décompresse pas, NBT mal formé, trailer faux, racine non Compound) rejoint la famille déjà existante de AR-R20 : ABSENT. Ce qui est une limite du moteur (quota, profondeur, taille de sortie) est REFUSÉ : l'écraser serait perdre peut-être des données légitimes à cause d'une politique du moteur.
- **Tolérances.** Nom de racine non vide : accepté (aligné sur `core-nbt.md` R15, alors qu'AR-R24 d'origine disait "provisoire : nom vide"). Octets après la racine : ignorés (`core-nbt.md` R34).
- **Risque.** ABSENT fait écraser, à la sauvegarde suivante, un enregistrement qu'un humain aurait peut-être réparé. Alternative : REFUSÉ pour tout. Non retenue sans observation ; PR6 relève si le serveur écrase ou conserve l'enregistrement, ce qui tranche.

## D6 : limite de décompression

- **Pourquoi explicite.** Le quota NBT (`core-nbt.md` R32) s'applique à la lecture de la charge décompressée : il ne borne pas l'inflation elle-même. Un enregistrement interne fait au plus 1 044 475 octets compressés (AR-R11, soit 255 secteurs moins l'en-tête de 5 octets), donc un rapport de 129 pour 1 suffit pour atteindre 2^27 octets ; DEFLATE le permet (mesure de l'outil : 4 Mio de zéros donnent 4 086 octets zlib). Un `.mcc` n'a aucune borne dans le format.
- **Valeur.** 2^27 = 134 217 728 octets, égal au quota NBT par défaut. Argument : chaque octet de charge compte au moins 1 unité de quota (`core-nbt.md` R60), donc aucune charge de plus de 2^27 octets ne passerait le quota NBT : la limite ne refuse rien de ce que le quota accepterait, sauf des octets après la racine. Aucune mesure sur de vrais chunks n'existe (pas de monde extrait) : 2^27 est un plafond de sécurité, pas une estimation de la taille d'un chunk. Un plafond plus bas serait possible (le NBT du chunk synthétique fait 6 Kio) ; il faudrait alors des mesures sur l'oracle.
- **Contrôle.** En flux, sans croire une taille annoncée (ISIZE gzip, longueurs d'en-tête LZ4, `L - 1` du type 3).
- **Sonde.** Aucune ne lève la limite du jeu : le wiki n'en cite pas.

## D7 : sauvegarde d'une position refusée

- **Options.** No-op silencieux ; booléen ; erreur. Un chunk refusé n'est jamais chargé, donc rien de légitime n'est à sauvegarder : une demande d'écriture est une erreur de logique, et un no-op ou un booléen ignorable la cacherait. Erreur explicite retenue.
- **Mécanisme.** Une marque en mémoire posée par la lecture REFUSÉ, retirée à la fermeture du magasin. La relecture d'une position marquée ne relit pas le fichier (évite de redécompresser une bombe à chaque demande).
- **Limite connue.** Une écriture à une position jamais lue n'est pas contrôlée : contrat de l'appelant (toute écriture suit une lecture).

## D8 : forme de la lecture

- Trois issues exclusives, des causes en catégories imposées (les messages ne le sont pas), une liste d'avertissements. Une panne d'entrée-sortie n'est aucune des trois : l'exception se propage, rien n'est généré, car transformer une panne passagère en ABSENT ferait écraser un chunk valide.
- Les causes sont énumérées dans AR-R26 ; toute cause qui n'y figure pas est un défaut de la spec.

## D9 : heightmaps

- Trois canaux pour l'appelant : un résultat "inconnu" explicite à la consultation, l'ensemble des types INCONNU, un avertissement par type présent mais invalide. Jamais de zéros inventés.
- La longueur attendue n'est plus la constante 37 mais `ceil(256 / floor(64 / b))` : un type de dimension tiers d'une autre hauteur change la longueur. 36 (ancien empaquetage continu), 38, un type NBT erroné et une valeur supérieure à `height` rendent le type INCONNU.

## D10 : décisions ajoutées

- CF-R35 (champs obligatoires) : nécessaire pour que chaque échec de lecture ait une issue (D8). `DataVersion`, `Status`, `sections` obligatoires ; `xPos`, `zPos` rattachés à AR-R21.
- BS-R22 : aucune entrée de palette invalide ne refuse le chunk ; les corrections sont des avertissements.
- Ces deux points n'étaient pas demandés. Ils peuvent être retirés sans effet sur D1 à D9.

## D11 : garde-fous du magasin consignés après la revue

- **Origine.** La revue du module `engine/world` a fait ajouter au code des protections que les specs ne mentionnaient pas. Le code étant celui du projet, la spec le décrit (AR-R31 à AR-R38) sans copier de nom de classe.
- **Partition S-arch ou S.** Une règle qui n'a pas de comportement de référence à comparer (garde, budget, sécurité du disque) est S-arch : limite à l'écriture (R31), fichiers non réguliers (R32), plafond de fichiers ouverts (R33), noms de dimension sûrs (R34), écriture atomique du `.mcc` (R36), borne d'expansion LZ4 (R37), mémoire de la décompression (R38). Le refus du `/` dans un chemin de dimension (R35) n'est pas dans ce cas : le jeu a peut-être un comportement observable sur ce point (dossiers imbriqués), donc S avec la sonde PR7.
- **R31, pourquoi refuser à l'écriture.** Sans ce refus, un chunk plus gros que la limite s'écrit, est refusé TOO_LARGE à la lecture suivante, marque sa position (R28) et la verrouille pour la session : la sauvegarde aurait créé un chunk illisible par le magasin qui l'a écrit. Alternative écartée : relever la limite à l'écriture, qui reviendrait à ne plus avoir de limite.
- **R32, pourquoi avant toute écriture.** La suppression du `.mcc` résiduel venait après l'écriture de l'enregistrement : un répertoire non vide du même nom faisait échouer l'appelant alors que le chunk était déjà commit, un répertoire vide était supprimé sans bruit. Le contrôle est déplacé avant l'écriture, comme pour les autres fichiers non réguliers.
- **R34, pourquoi ces noms.** Une dimension de datapack donne un nom de dossier : le jeu de caractères restreint et le refus des composantes `.` et `..` empêchent de sortir du monde ; le point final et les noms de périphérique DOS sont refusés parce que Windows les traite à part (documentation Microsoft consultée le 2026-10-29). Le point final est une hypothèse de normalisation non relevée. Alternative écartée : accepter tout nom valide pour le jeu et laisser le système de fichiers échouer, ce qui ferait échouer tard et de façon différente selon la plateforme.
- **R37, pourquoi 255.** C'est la borne arithmétique du format de bloc : elle ne refuse aucun bloc valide et évite d'allouer un bloc de 32 Mio annoncé par 10 octets de corps. L'ordre des contrôles d'un même en-tête invalide qui annonce aussi plus que le budget n'est pas tranché par la spec (AR, Questions ouvertes) : le comportement actuel n'est épinglé par aucun test.

## Contradictions et points repérés dans les specs fusionnées

- CF-R24 contre `core-math.md` R32 : résolu en faveur de core-math et du wiki (D1) ; le code de core n'a pas à changer sauf démenti de PC6.
- `core-math.md` R32 : la confirmation par P30 ne peut pas couvrir `PostProcessing` (D1).
- AR-R24 d'origine (nom de racine vide) contre `core-nbt.md` R15 (nom non vide accepté) : aligné, la lecture tolère, l'écriture produit un nom vide.
- AR-R24 d'origine (aucun octet après la racine) contre `core-nbt.md` R34 (octets ignorés) : aligné de la même façon.
- Le wiki "Chunk format" donne 24 listes pour `PostProcessing` et 16 pour `ToBeTicked` : le moteur ne les relie pas (CF-R24).
- Le quota NBT par défaut de `core-nbt.md` R32 ne borne pas la décompression : d'où AR-R29.
- Non spécifié et laissé ouvert : fichier de région de taille 0, de moins de 8192 octets ou non multiple de 4096 (AR, Questions ouvertes) ; champs facultatifs propres aux proto-chunks (CF-R35).

## Règles S qui restent à lever, par sonde

CF-R24 (PC6), CF-R30 (PC7), CF-R31 à R34 (PB3), CF-R35 (PC6), AR-R26 causes et AR-R30 (PR6, PB2, PB3, PR4, PR5), AR-R29 (aucune sonde pour la valeur), AR-R27 et AR-R28 (aucune : politiques du moteur), AR-R35 (PR7 ; AR-R31 à R34 et R36 à R38 sont S-arch, sans sonde), BS-R22 (PB1).

## Propositions pour la matrice des fonctionnalités

Aucun changement de statut : les lignes du module world restent "spécifié", avec des règles S. L'orchestrateur peut ajouter une ligne "limite de décompression des chunks" rattachée à AR-R29 si la matrice suit ce niveau de détail.

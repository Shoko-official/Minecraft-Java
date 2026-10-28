# Journal des décisions du module data

Statut : brouillon
Jalon : M2
Risque : A

## Périmètre

Trace, pour `data-datapack-loading.md` et `data-registries-inventory.md`, les ambiguïtés révélées par l'implémentation du squelette de `engine/data`, la décision prise, son niveau de preuve, la sonde qui la confirmera et ce qui la ferait changer. Les règles citées (R57 à R67) sont dans `data-datapack-loading.md`, qui fait foi pour le comportement ; ce journal explique les choix. Il ne contient ni code de Mojang ni nom de classe de l'implémentation.

Niveaux : S = hypothèse provisoire (sonde citée) ; S (architecture) = décision de conception que l'oracle ne peut pas observer, sans sonde. Aucune décision n'est D, M ou C : les 8374 fichiers JSON de la cible n'exercent aucun de ces cas (seule la profondeur 17 est mesurée, R49). Aucune sonde n'a été exécutée (serveur non exécutable, EULA non acceptée, ADR 0002 2a) ; le point 2a-bis n'est utilisé nulle part.

Sources consultées pour ce journal, le 2026-10-28 : documentation publique du wiki (pages "Data pack", "Tag (Java Edition)", "Enchantment definition", "Java Edition protocol/Packets"), ADR 0002, 0005, 0006 (cette dernière sur `origin/feat/data-skeleton`), spécifications existantes. Vérifications numériques (bornes binary64 et binary32, bits de `-0.0`, arrondis) recalculées avec la bibliothèque standard Python.

## Décisions

| Id | Ambiguïté | Décision recommandée | Règle | Niveau | Sonde | Alternative écartée et condition de réouverture |
|---|---|---|---|---|---|---|
| D1 | Profondeur maximale du JSON | 255 niveaux, racine comprise, conteneurs vides compris ; le 256e est `JSON_TOO_DEEP` ; refus sans lire la suite, sans épuiser la pile | R57 | S | P18 | 64 : plus strict, mais sans fondement non plus. 255 coïncide avec le défaut de la bibliothèque (ADR 0006), donc aucun changement de code. Rouvrir si P18 mesure une autre limite : elle la remplace telle quelle |
| D2 | Position d'un membre à clé en double | Place de la première occurrence, valeur de la dernière ; un avertissement par occurrence en trop ; clés égales après décodage des échappements, sensibles à la casse | R58 | S (architecture) pour la place ; S pour la valeur | P11 (valeur) ; aucune pour la place | Place de la dernière occurrence : écartée car elle suppose de retirer puis de réinsérer, et s'écarte des structures associatives courantes. La place n'est observable ni dans les données ni par une sonde connue ; rouvrir seulement si M8 montre qu'un ordre de membres dépend de cette place |
| D3a | Entier hors 64 bits | Fichier invalide, `JSON_NUMBER_RANGE` | R59 b | S | P19 | Accepter comme décimal (perte de la forme entière, R29) ou garder un entier de taille arbitraire. Repli si P19 montre que le jeu charge ces valeurs : lire l'entier trop grand comme décimal (binary64 au plus proche), forme "décimal", avertissement `JSON_NUMBER_RANGE` au lieu d'une erreur |
| D3b | Décimal qui déborde en infini | Fichier invalide, `JSON_NUMBER_RANGE` ; le dépassement vers zéro est valide (0.0 ou -0.0) | R59 d | S | P19 | Accepter l'infini : écarté, un nombre non fini n'est pas représentable en JSON strict et casserait l'égalité des nœuds |
| D3c | `-0` entier | Valide, entier de valeur 0, égal à `0` | R59 c | S (architecture) | aucune (non observable) | Garder un zéro négatif entier : impossible sur 64 bits ; le lire comme décimal `-0.0` : écarté car changerait la forme (R29) |
| D3d | `-0.0` décimal | Valide, signe conservé, différent de `0.0` | R59 e | S | P19 (indirect) | Aligné sur `core-nbt.md` R35 et R37 |
| D4 | Substitut UTF-16 isolé échappé (`\ud800`) | Accepté, l'unité est conservée telle quelle ; substitut en octets UTF-8 refusé (`JSON_UTF8`) ; toute écriture ou affichage échappe l'unité | R60 | S | P20 | Refuser : la grammaire de la RFC 8259 l'autorise et une chaîne Java peut le contenir ; un identifiant le rejette de toute façon (R20). Rouvrir si P20 montre un refus |
| D5 | Entier contre décimal dans les combinateurs | Décimal attendu : accepte entier et décimal. Entier attendu : n'accepte que la forme entière (`1.0` refusé), bornes 32 ou 64 bits. Booléen, chaîne, liste : types stricts, pas de promotion | R62 | S | P21 | Entier attendu tolérant `1.0` : écarté par défaut car une donnée vanilla mal lue se voit tout de suite (critère zéro erreur de chaque module), pas une tolérance de trop. Rouvrir par champ si P21 montre que le jeu accepte `1.0` ; le changement est local au combinateur |
| D6 | Champs inconnus | Règle générale : ignorés sans diagnostic ; mode strict disponible par objet, choisi par le module propriétaire ; aucune exception dans M2. **Changement par rapport à la rédaction précédente de R32** (tags : clé inconnue refusée) | R63, R4, R32 | S | P22 | Garder les tags stricts : la rédaction précédente n'avait pas de source ; la documentation publique n'en donne pas, et une erreur de tag peut bloquer un monde (R55) alors qu'une tolérance de trop n'a pas d'effet sur les données vanilla. Si P22 montre qu'un porteur refuse, le passer en strict dans sa spec (pour un tag : une ligne de R32 et R63 c) |
| D7 | Ensemble d'éléments ("holder set") | Chaîne sans `#` : un élément ; chaîne avec `#` : tag ; tableau : identifiants seulement (`#` dans un tableau refusé), vide valide, doublons fusionnés ; autre type refusé ; référence unique : identifiant seul | R64 | S (documentation publique pour les trois formes) | P23 | Accepter `#tag` dans un tableau : la documentation décrit un tableau d'identifiants. Rouvrir par P23 ; l'existence du comportement "tag introuvable" (erreur ou ensemble vide) est l'inconnue principale |
| D8 | Encodage (écriture JSON ou NBT pour la synchronisation, M8) | Combinateurs à décodage seulement en M2 ; arbre JSON et pack d'origine conservés par élément ; combinateurs décrits comme des valeurs ; chemin M8 prévu : conversion générique de l'arbre vers NBT, packs connus pour les entrées du pack de base, tags toujours en entier ; encodage typé ajouté seulement si un client réel détecte un écart | R65 | S (architecture) | aucune en M2 ; M8 avec un client vanilla (ADR 0002, 2c) | Combinateurs bidirectionnels dès M2 : écartés, car chaque module propriétaire écrirait pour un besoin non démontré un accesseur par champ (M6 à M7, plusieurs dizaines de types), et le chemin par arbre JSON évite ce coût si le client accepte la conversion générique. Rouvrir si la spec M8 montre une divergence entre le NBT converti et ce qu'un client réel accepte |
| D9a | Rôle de `activeFlags` | Entrée du chargeur ; source dont les flags requis ne sont pas tous actifs écartée avec `PACK_FLAGS_INACTIVE`, les autres se chargent ; le chargeur ne calcule ni ne modifie l'ensemble | R66 | S (architecture) | aucune (état inatteignable dans le jeu) | Refuser tout le chargement : écarté, R54 veut qu'un défaut local n'arrête pas les autres. Ne rien vérifier : écarté, une erreur de l'appelant deviendrait silencieuse |
| D9b | Ordre de découverte d'un dossier `datapacks/` | Candidats triés par nom complet, point de code par point de code ; identifiant `file/<nom>` ; `.zip` en minuscules ; noms commençant par `.` ignorés ; nouveaux packs compatibles activés à la suite, dans cet ordre | R67 | S | P24 | Ordre du système de fichiers : non déterministe d'un système à l'autre (ARCHITECTURE). Ordre insensible à la casse ou naturel : sans fondement. Rouvrir si P24 montre un autre ordre : le remplacer, qu'il soit reproductible ou non (si l'oracle suit le système de fichiers, le fixer dans la spec comme "ordre non spécifié, tri retenu par déterminisme") |
| D10 | Niveau de risque de `data-datapack-loading.md` | A (était B) ; idem pour `data-registries-inventory.md` | en-tête | décision de procédure | sans objet | Le tableau de risque de `docs/WORKFLOW.md` place B pour "structures de données, I/O" ; A est retenu parce que tous les modules de données en dépendent. La ligne de `docs/features/matrix.md` est à aligner (voir ci-dessous) |
| D11 | Place du type `Identifier` | Dans `engine/data` (règles R20 à R25) ; `core` n'en a pas besoin, ses fonctions prennent des chaînes (`core-random.md` R26) | périmètre | décision d'architecture | sans objet | Dans `core` : écarté, car la validation et le défaut `minecraft` sont des règles de la spec de données, et `core` doit rester sans connaissance de leur forme |

## Précisions de comportement qui découlent des décisions

- Un fichier invalide donne un seul diagnostic d'erreur, avec la précédence BOM, puis octets UTF-8 invalides, puis erreurs de lecture dans l'ordre du texte (R61) ; les avertissements de clés en double ne sont pas émis pour un fichier invalide (R58 e).
- Les codes de diagnostic sont une liste fermée (tableau "Codes de diagnostic") ; les tests vérifient code, gravité, fichier et chemin JSON, jamais le texte (R61). Si le squelette emploie d'autres noms, le tableau fait foi.
- Format de chemin JSON : `$`, `.nom`, `[i]` (R61). Les erreurs de syntaxe n'ont pas de chemin vérifié par les tests.

## Corrections proposées hors de ce dossier

À appliquer par l'orchestrateur ; ce document n'édite aucun des fichiers ci-dessous.

| Fichier | Passage | Correction |
|---|---|---|
| `docs/ARCHITECTURE.md`, tableau des modules | ligne `engine/core` : "RNG (legacy, xoroshiro, positionnel), `Mth` et tables, identifiants, NBT (binaire et SNBT), géométrie, utilitaires" | retirer "identifiants" de la ligne `core` |
| `docs/ARCHITECTURE.md`, tableau des modules | ligne `engine/data` : "Infrastructure de chargement des datapacks : sources et priorités, arbres JSON, registres et références, combinateurs de codecs, tags (ADR 0005)" | ajouter "identifiants (type `Identifier`, `data-datapack-loading.md` R20 à R25)" après "arbres JSON" |
| `docs/ROADMAP.md`, ligne M1 | "RNG legacy et xoroshiro, RNG positionnels, `Mth` et tables, NBT binaire et SNBT, identifiants" | retirer "identifiants" ; l'ajouter à la ligne M2 : "arbres JSON, identifiants, registres et références..." |
| `docs/features/matrix.md` | ligne "Chargement des datapacks et registres", risque B | passer à A (statut `spécifié` quand la spec passera à `relu`) |
| `docs/specs/reference-binaries.md` | règles R2 à R4 (formats, data version, protocole) gardées en S | déjà signalé dans `data-datapack-loading.md` (questions ouvertes) ; sans lien avec ce lot |
| `docs/specs/README.md` | "Une règle de niveau S bloque `figé` tant qu'elle n'est pas levée par une sonde" | prévoir une catégorie "S (architecture)" exemptée, ou décider que ces règles passent en ADR (R58 b, R59 c, R65, R66) |
| ADR 0006 (sur `feat/data-skeleton`) | "dont la valeur reste à fixer par la spec" ; mention de R51 provisoire | profondeur fixée par R57 à 255 (valeur déjà celle du squelette) ; ajouter que l'ordre des membres d'un objet à clé en double est la première occurrence (R58), que `-0` est valide (R59), que le substitut échappé isolé est accepté (R60) |

## Décisions impossibles sans 2a-bis

Aucune. Chaque question trouve soit une sonde d'exécution réalisable avec l'interface admise (P18 à P24, serveur et commandes RCON), soit une décision d'architecture que même un accès aux signatures publiques ne lèverait pas (place d'un membre, `-0`). Le point D8 pourrait être tranché plus tôt par 2a-bis (connaître la forme exacte que le client attend), mais le jalon M8 offre un client vanilla réel, interface admise par ADR 0002.

## Règles S restantes après ce lot

R57, R58 (valeur), R59, R60, R62, R63, R64, R67 (avec sondes P18 à P24) ; R58 (place), R59 c, R65, R66 (décisions d'architecture, sans sonde). Les règles S antérieures (R3 à R7, R12, R15 à R17, R19 à R25, R30, R32 à R38, R40 à R48, R51 à R56) restent à lever par P1 à P17, sauf R51 (aussi P11) et R32 (aussi P22).

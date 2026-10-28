# ADR 0006 : Bibliothèque JSON de `engine/data`

Statut : accepté

## Contexte

`engine/data` lit environ 8 400 fichiers JSON (éléments de registres, tags, `pack.mcmeta`, rapports de l'extraction) et doit appliquer exactement les règles R49 à R54 de `docs/specs/data-datapack-loading.md` :

- JSON strict (RFC 8259) : commentaires, virgule finale, guillemets simples, clés ou chaînes sans guillemets, `NaN`, `Infinity`, `+1`, `.5`, `01` refusés (R52) ;
- BOM, fichier vide ou blanc, contenu après la racine, UTF-8 invalide refusés (R53) ;
- clé en double : la dernière valeur l'emporte, avec un avertissement (R51, précisé par R58) ;
- nombres : la forme entière ou décimale est conservée (`1` et `1.0` sont des fichiers de registres différents, R29), décimal en binary64 correctement arrondi, entier exact sur 64 bits (R50) ;
- ordre des clés conservé, pour le déterminisme ;
- n'importe quel type JSON à la racine d'un élément (R29) ;
- profondeur : 17 niveaux au plus dans les données de la cible (R49) ; un lecteur doit avoir une limite explicite, fixée à 255 par R57 (voir la dernière section) ;
- chaque nœud connaît son fichier et son chemin JSON pour les diagnostics, et un fichier invalide ne doit pas arrêter le chargement des autres (R54).

La convention du dépôt (`docs/CONVENTIONS.md`) prévoit "une bibliothèque JSON". `tools/bootstrap` utilise déjà Gson 2.14.0 (`gradle/libs.versions.toml`).

## Décision

1. **Gson 2.14.0**, API de flux `com.google.gson.stream.JsonReader` uniquement, avec `Strictness.STRICT` posé explicitement. Les API arbre (`JsonParser`, `JsonElement`) et de liaison (`Gson.fromJson`) ne sont jamais utilisées.
2. **Confinement.** Seul le paquet `dev.shoko.minecraft.data.json` importe Gson. Les types Gson n'apparaissent dans aucune signature publique ; la dépendance est déclarée `implementation`, pas `api`. L'arbre JSON public (`JsonNode` et ses variantes) est celui du projet. Remplacer la bibliothèque, ou la supprimer au profit d'un parseur maison, ne touche donc qu'un paquet.
3. **Ce que le projet ajoute au-dessus de Gson**, parce que Gson ne le fait pas (tous les points ont été vérifiés par exécution sur 2.14.0, voir les faits ci-dessous) :
   - décodage UTF-8 strict des octets avant Gson (octets invalides refusés) et refus explicite d'un BOM, que Gson ignore silencieusement ;
   - détection des clés en double par objet, pendant la lecture, pour émettre l'avertissement de R51 ;
   - classification de chaque littéral numérique (entier ou décimal), conversion en `long` ou `double`, refus d'un décimal qui déborde en infini et d'un entier hors 64 bits ;
   - limite de profondeur explicite, indépendante du défaut de Gson ;
   - conversion de toute exception de Gson en diagnostic `Diagnostic` (jamais d'exception de Gson hors du paquet).
4. **Version.** 2.14.0, publiée sur Maven Central le 2026-04-23 (en-tête `Last-Modified` du JAR, vérifié le 2026-10-28), soit plus de six mois. La 2.15.0 existe (publiée le 2026-10-24) mais a moins de deux semaines : elle n'est pas retenue. La version est déjà centralisée dans `gradle/libs.versions.toml` (clé `gson`).

## Faits vérifiés

Sonde exécutée le 2026-10-28 avec Gson 2.14.0 (JAR du cache Gradle), `JsonReader` en `Strictness.STRICT` sauf mention :

| Entrée | Résultat |
|---|---|
| Commentaires `//`, `/* */`, `#` ; guillemets simples ; clé sans guillemets ; `NaN`, `Infinity` ; `+1`, `.5`, `01`, `1.` ; virgule finale ; `{} {}` et `{} x` ; `\q` ; caractère de contrôle brut dans une chaîne | refusés |
| Chaîne vide, blanc seul | refusés (`EOFException`) |
| BOM `U+FEFF` en tête | **accepté et ignoré** : à refuser par le projet |
| `{"a":1,"a":2}` | **accepté sans signal** en flux (les deux paires sont lues) : à détecter par le projet |
| `-0`, `1e5`, `1E+5`, `1e999`, entier de 30 chiffres | acceptés comme jetons `NUMBER` : le texte exact est lu par `nextString()`, la classification et la conversion sont à faire par le projet |
| Racine scalaire (`5`) | acceptée, comme R29 l'exige |
| `\ud800` (substitut isolé, échappé) | accepté : la chaîne contient un substitut isolé (question ouverte pour la spec) |
| Imbrication | limite par défaut 255 (`getNestingLimit()`), 255 niveaux acceptés, 256 refusés ; modifiable par `setNestingLimit` |
| Strictness par défaut | `LEGACY_STRICT` : le mode `STRICT` doit être demandé |
| `JsonParser.parseString` (API arbre) | **tolérante** : `// c` puis `{}` est accepté, et `{"a":1,"b":2,"a":3}` devient `{"a":3,"b":2}` sans signal. À proscrire |

Documentation, non exécutée ici : `JsonReader.getPath()` donne le chemin JSON courant (`$.a[0]`) ; `JsonReader` n'est pas thread-safe (une instance par lecture).

## Alternatives écartées

| Option | Pour | Contre |
|---|---|---|
| **Jackson** (`jackson-core`, API de flux) | `STRICT_DUPLICATE_DETECTION`, contraintes de lecture intégrées (profondeur, longueur des nombres), distinction des types numériques | Selon la documentation (non exécuté ici) la détection de doublon lève une exception et l'analyse ne peut pas continuer, alors que R51 demande de poursuivre avec la dernière valeur : il faudrait de toute façon suivre les clés soi-même. Le BOM UTF-8 est reconnu à l'ouverture d'un flux d'octets, donc le pré-contrôle des octets reste nécessaire. Dépendance plus lourde, et deuxième bibliothèque JSON à côté de Gson déjà utilisé par `tools/bootstrap` (ou migration de celui-ci). Aucun gain sur les règles du projet. |
| **Parseur maison** | Contrôle total : doublons, forme des nombres, profondeur itérative, positions, aucune dépendance | Environ 300 lignes de code à écrire, tester et maintenir pour une grammaire que Gson `STRICT` couvre déjà (tous les refus de R52 et R53 sauf BOM). Risque de bug de grammaire sur 8 400 fichiers critiques. Reste possible plus tard : le confinement dans `data.json` rend le remplacement local. |
| **Gson API arbre** (`JsonParser`, `JsonElement`) | Le plus court à écrire | Tolérante par défaut (commentaires acceptés) et écrase les doublons sans signal : viole R51 à R53. |

## Conséquences

- `engine/data/build.gradle.kts` déclare `implementation(libs.gson)`. Le graphe de modules est inchangé (`data` ne dépend toujours que de `core`) et ArchUnit ne voit pas Gson.
- Une règle ArchUnit "seul `data.json` dépend de `com.google.gson`" est recommandée au `test-engineer` ; elle n'est pas posée ici (hors du scope de cet ADR : les tests d'architecture ne relèvent pas de l'architecte).
- Les tests aveugles du lecteur partent des vecteurs de la spec (R49 à R54) et de la table ci-dessus ; les entrées qui ressemblent à celles de la sonde (BOM, doublons, `1e999`, substitut isolé) sont des cas obligatoires.
- Si la sonde P12 de l'oracle montre que le jeu de référence accepte des formes non strictes, le choix `STRICT` est à revoir : `LENIENT` accepterait trop de formes et ne conviendrait pas, il faudrait alors le parseur maison, qui permet de choisir exactement les formes acceptées (R52).
- `JsonReader.MAX_DEPTH` vaut 255, fixé par R57 (défaut de Gson, très au-dessus des 17 niveaux observés) ; reste S jusqu'à la sonde P18.

## Décisions de la spec sur la lecture JSON (R57 à R60)

La spec (`docs/specs/data-datapack-loading.md`, journal `data-decisions-log.md`) a tranché trois points que cet ADR laissait ouverts. Ce sont des décisions d'architecture que ni les données extraites ni une sonde de l'oracle ne peuvent observer, donc de niveau S ; elles sont portées par le lecteur du projet, pas par Gson :

- **Clé en double (R58).** Le membre garde la place de sa première occurrence et reçoit la valeur de la dernière : `{"a":1,"b":2,"a":3}` donne `a=3`, `b=2` dans cet ordre. Un avertissement `JSON_DUPLICATE_KEY` par occurrence en trop. Gson ne signale pas les doublons en flux, le projet suit les clés par objet pendant la lecture. Rouvrir seulement si un ordre de membres dépend de cette place dans un module propriétaire.
- **`-0` valide (R59).** Un entier `-0` est valide et vaut 0 (un entier 64 bits n'a pas de zéro négatif, `-0` et `0` sont des nœuds égaux) ; le décimal `-0.0` garde son signe et diffère de `0.0`. Un dépassement vers zéro (`1e-999`) est valide. Un entier hors 64 bits et un décimal qui déborde en infini restent `JSON_NUMBER_RANGE`.
- **Substitut isolé accepté (R60).** Un substitut UTF-16 isolé écrit par échappement (`\ud800`) est accepté et conservé tel quel dans la chaîne, comme Gson le fait ; un substitut encodé dans les octets reste de l'UTF-8 invalide (`JSON_UTF8`), refusé par le pré-contrôle des octets. Tout code qui écrit ou affiche une telle chaîne échappe l'unité isolée.

La profondeur maximale est fixée par R57 à 255 niveaux, valeur du défaut de Gson : `JsonReader.MAX_DEPTH` n'est plus provisoire côté spec (reste S, sonde P18). Les codes de diagnostic sont la liste fermée de R61 (`JSON_SYNTAX`, `JSON_BOM`, `JSON_EMPTY`, `JSON_TRAILING`, `JSON_UTF8`, `JSON_TOO_DEEP`, `JSON_NUMBER_RANGE`, `JSON_DUPLICATE_KEY`).

# Conventions de code

Le dépôt est jeune : ces règles sont la référence. En cas de doute, imiter le code voisin.

## Langues

- Code, identifiants, commentaires et Javadoc : anglais (la terminologie du jeu est anglaise).
- Documentation (`docs/`, README) : français.
- Descriptions de commits et de PR : français.
- Caractères : ASCII pour le code et les commentaires. Pas de tirets cadratins ou demi-cadratins (utiliser `-`).

## Style Java

- Java 25 : records, sealed interfaces, pattern matching, switch expressions, `var` quand le type est évident.
- Formatage automatique par Spotless (google-java-format). Ne pas formater à la main.
- Classes `final` par défaut. Immutabilité préférée ; état mutable confiné et documenté.
- Pas de réflexion, pas de singletons statiques mutables, pas de `static` global pour l'état de jeu.
- Pas d'`Optional` en champ ou paramètre ; `null` interdit en API publique sauf annotation explicite.
- Noms : `PascalCase` types, `camelCase` méthodes et variables, `UPPER_SNAKE` constantes. Les identifiants du jeu (`minecraft:stone`) sont des données, pas des constantes de code, sauf dans les tests.

## Budgets de taille

| Élément | Cible | Maximum |
|---|---|---|
| Fichier Java | 400 lignes | 600 lignes |
| Méthode | 40 lignes | 60 lignes |
| Paramètres de méthode | 4 | 6 |
| Paquet | 15 classes | 25 classes |
| PR | 600 lignes de diff | 1 000 lignes |

Au-delà de la cible, la question est : cette classe a-t-elle une seule responsabilité ? Contrôle automatique : la CI échoue aujourd'hui au-delà de 600 lignes par fichier Java ; les autres budgets (méthode, paramètres, paquet) s'automatisent avec le squelette Gradle, la taille de PR reste une cible relue par le `reviewer`. Un dépassement justifié est enregistré dans `scripts/hygiene-exceptions.txt` avec la raison.

## Une responsabilité par classe

On doit pouvoir dire en une phrase ce qu'une classe fait, de quoi elle dépend et comment on l'utilise. Si ce n'est pas possible, la scinder. Les classes de données (records) ne portent pas de logique métier lourde.

## Commentaires

Uniquement pour une intention non évidente, une contrainte, un cas limite, un compromis, ou la référence à la spec (`// spec: worldgen/aquifers`). Jamais pour répéter le code. Aucun commentaire décoratif, bannière ou séparateur. Un nombre magique issu du jeu est nommé et son origine est citée (donnée extraite ou spec).

## Tests

- JUnit 5 + AssertJ. Un fichier de test par classe testée, même paquet, suffixe `Test`.
- Nom explicite du comportement : `negativeCoordinatesUseFloorDivision`.
- Vecteurs dorés dans `src/test/resources/golden/` : petits, textuels (JSON ou texte), avec en-tête décrivant la commande de l'oracle qui les produit. Pas de gros binaires versionnés.
- Pas de `Thread.sleep`, pas de dépendance au temps, pas d'ordre d'exécution implicite.
- Un test rouge se corrige dans le code. Ne jamais affaiblir ou supprimer un test pour le faire passer ; un test prouvé faux est corrigé avec la raison dans le message de commit.
- Les tests L2 (oracle) sont marqués `@Tag("oracle")` et exclus de la tâche `test` par défaut ; ils tournent via une tâche dédiée.

## Dépendances externes

Minimales : LWJGL (client), une bibliothèque JSON, JUnit, AssertJ, ArchUnit, JMH (plus tard). Toute nouvelle dépendance exige une ligne justificative dans la PR. Versions centralisées dans `gradle/libs.versions.toml`.

## Interdits

- Code ou texte copié de Mojang, y compris constantes issues de décompilation.
- Données ou assets Mojang dans git.
- Attribution automatique ou signature d'outil dans le code, les docs, les commits.
- Contournement des règles ArchUnit ou des budgets sans entrée de liste d'exceptions.
- `catch` qui avale, `TODO` sans ticket.

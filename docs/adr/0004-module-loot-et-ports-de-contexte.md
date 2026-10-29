# ADR 0004 : Module `loot` et ports de contexte

Statut : accepté, amendé par les specs du jalon M6 (voir « Amendement 1 » et « Amendement 2 »)

## Contexte

Les conditions de butin (état de bloc, position, propriétés d'entité) et les effets d'enchantements (remplacer un bloc, invoquer une entité, explosion) ont besoin de notions de bloc, de monde et d'entité. Les effets de statut agissent sur des entités. Un module `item` qui ne connaît ni `world` ni `entity` ne peut pas les porter. Inversement, les entités produisent des butins : `entity` doit dépendre du butin.

## Décision

- `item` ne contient que objets, composants, inventaires et recettes (données et règles sans monde). Cette liste est élargie par l'amendement 1 : algorithmes purs de clic de menu et de reconnaissance de recettes, références aux fournisseurs et prédicats.
- Nouveau module `engine/loot` : tables de butin, prédicats, fournisseurs de nombres et évaluation des valeurs d'enchantements. Il dépend directement de `world` et `item` (donc de `core` et `data` par transitivité).
- Les effets de statut vivent dans `entity`.
- Les actions d'enchantement à effet de bord (remplacer un bloc, invoquer, exploser) sont exécutées par `game` ou `entity` via des ports : `loot` déclare de petites interfaces de lecture et d'action (`EntityView`, `ActionSink`), `entity` et `game` les implémentent et appellent `loot`. Aucune dépendance de `loot` vers `entity`.

Graphe résultant : `loot -> world, item` ; `entity -> world, item, loot` ; `game -> worldgen, entity, loot`.

## Alternatives écartées

- Tout dans `item` avec des abstractions de contexte : `item` devient un module fourre-tout.
- Loot et enchantements dans `game` : `game` dépasse vite les budgets de taille et n'est plus testable isolément.

## Conséquences

- Un module de plus, mais des responsabilités nettes et des tests de butin sans entités réelles (ports factices).
- Le découpage fin des ports est tranché avec la spec de M6, pas avant.

## Amendement 1 : propriété précisée par les specs du jalon M6

Les specs `item-components-inventory.md` (« Propriétaires de modules »), `item-recipes.md` (DC1), `loot-tables.md` (R16, R17, R50) et `item-enchantments.md` (« Propriétaires de modules ») ont tranché le découpage fin annoncé plus haut. Cet amendement consigne ces décisions de conception ; elles ne décrivent pas le jeu de référence et n'ont pas de niveau de preuve. Il ne change pas les dépendances directes du tableau des modules de `docs/ARCHITECTURE.md`, dont les colonnes « Responsabilité » sont réécrites en conséquence.

### Décisions

1. **Calculs d'enchantement dans `loot`.** `loot` porte, en plus des valeurs d'enchantements, la sélection des offres de la table d'enchantement, les calculs de coût et de fusion de l'enclume (pénalité de travail comprise) et ceux de la meule. Ces calculs dépendent des définitions d'enchantement et de l'aléa de tirage, qui vivent dans `loot`. Le calcul est une fonction de `loot` ; l'échange avec les slots ne l'est pas (point 3).
2. **Fournisseurs et prédicats évalués par `loot`.** `item` ne porte que la référence au fournisseur ou au prédicat (identifiant lu dans un prototype, par exemple `cooking_fuel`, `compostable`, `brewing_fuel`) et les données de recette. `loot` les évalue, avec un contexte de bloc réduit à l'état du bloc de la machine, pour le compte de `item` et de `game`. `item` ne dépend pas de `loot` : l'évaluation est demandée par `game`, qui possède la machine et son tick (ADR 0005), et dont le résultat (durée, vitesse, couches) est transmis à la logique de cuisson, d'alchimie ou de compostage.
3. **Algorithmes purs dans `item`, menus dans `game`.** `item` porte les algorithmes purs de clic de menu sur une liste abstraite de slots et un curseur, et la reconnaissance de recettes (fonctions sur des piles, testables sans monde). `game` possède les menus liés à un bloc ou à un joueur, les ticks et les block entities (ADR 0005) ; il appelle `item` pour les clics et la reconnaissance, et `loot` pour les coûts, les offres et les fournisseurs.
4. **Un seul port de lecture.** Le seul port de contexte de butin est `EntityView` (lecture), accompagné de `ActionSink` (intentions d'effet de bord, non utilisé par le tirage pur). `BlockView` et `ItemView` ne sont pas créés : `loot` dépend directement de `world` et de `item` et lit l'état de bloc, les étiquettes, l'entité de bloc, le biome et les piles par ces modules. `entity` et `game` implémentent les ports ; `loot` ne dépend pas de `entity`.

### Alternatives écartées

- Calculs de la table d'enchantement, de l'enclume et de la meule dans `game` : ils dupliqueraient l'accès aux définitions d'enchantement et à l'aléa de `loot`, et ne seraient testables qu'avec des menus.
- Évaluation des fournisseurs de nombres dans `item` : `item` devrait dépendre de `loot` (dépendance montante) ou recopier l'interpréteur de fournisseurs.
- Ports `BlockView` et `ItemView` : abstractions sans second usage, puisque `loot` voit déjà `world` et `item`.

### Conséquences

- `item` : objets, composants, inventaires, recettes, algorithmes de clic et reconnaissance de recettes ; références aux fournisseurs, sans leur évaluation.
- `loot` : tables de butin, prédicats, fournisseurs de nombres, valeurs d'enchantements, calculs de la table d'enchantement, de l'enclume et de la meule, ports `EntityView` et `ActionSink`.
- `game` : menus, ticks, block entities à inventaire, assemblage des appels vers `item` et `loot`. Les budgets de taille de `game` restent surveillés (ADR 0005).
- Aucune dépendance montante : `item -> data` ; `loot -> world, item` ; `game -> worldgen, entity, loot` (et `item` par transitivité via `entity` et `loot`). Le tableau des modules de `docs/ARCHITECTURE.md` reprend ces responsabilités. Si l'implémentation exige que `game` importe directement des types de `item`, l'ajout de cette dépendance directe (descendante) se fait avec le code, en même temps que la règle ArchUnit et le tableau.

## Amendement 2 : ports d'aléa du module `loot`

`loot` déclare, en plus de `EntityView` et `ActionSink`, deux ports qui ne sont pas des ports de contexte de butin : `RandomSequenceStore` (lecture et avance de l'état d'une séquence aléatoire nommée) et `DrawObserver` (réception, dans l'ordre, de chaque décision aléatoire du tirage). Justification, vérifiée dans `docs/specs/loot-tables.md` :

- `RandomSequenceStore` : la règle R65 prescrit un état de séquence persistant, initialisé à la première utilisation et partagé entre les tables qui déclarent la même séquence. La persistance appartient à `game` et `world`, vers lesquels `loot` ne peut pas dépendre vers le haut : `loot` reçoit donc un magasin qu'il lit et fait avancer, sans connaître son support.
- `DrawObserver` : la règle R66 ne prescrit que l'ordre de consommation de l'aléa (hypothèse H-draw, arbitrée par l'oracle) et aucun port. La lecture « observateur » n'est donc pas une règle du jeu : c'est une décision de conception, consignée par la règle R78 (`S-arch`), motivée par le besoin de comparer cet ordre à l'oracle (sonde PL5) et de tester H-draw. Un tirage sans observateur donne les mêmes résultats.

Conséquence : l'énumération des ports de `loot` est `EntityView`, `ActionSink`, `RandomSequenceStore`, `DrawObserver`. Le point 4 de l'amendement 1 reste vrai pour les ports de contexte : `EntityView` y reste le seul port de lecture.

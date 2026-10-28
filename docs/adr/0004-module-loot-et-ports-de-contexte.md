# ADR 0004 : Module `loot` et ports de contexte

Statut : accepté, à raffiner par le `spec-analyst` au jalon M6

## Contexte

Les conditions de butin (état de bloc, position, propriétés d'entité) et les effets d'enchantements (remplacer un bloc, invoquer une entité, explosion) ont besoin de notions de bloc, de monde et d'entité. Les effets de statut agissent sur des entités. Un module `item` qui ne connaît ni `world` ni `entity` ne peut pas les porter. Inversement, les entités produisent des butins : `entity` doit dépendre du butin.

## Décision

- `item` ne contient que objets, composants, inventaires et recettes (données et règles sans monde).
- Nouveau module `engine/loot` : tables de butin, prédicats, fournisseurs de nombres et évaluation des valeurs d'enchantements. Il dépend de `core`, `data`, `world` et `item`.
- Les effets de statut vivent dans `entity`.
- Les actions d'enchantement à effet de bord (remplacer un bloc, invoquer, exploser) sont exécutées par `game` ou `entity` via des ports : `loot` déclare de petites interfaces de lecture et d'action (`EntityView`, `ActionSink`), `entity` et `game` les implémentent et appellent `loot`. Aucune dépendance de `loot` vers `entity`.

Graphe résultant : `loot -> world, item` ; `entity -> world, item, loot` ; `game -> worldgen, entity, loot`.

## Alternatives écartées

- Tout dans `item` avec des abstractions de contexte : `item` devient un module fourre-tout.
- Loot et enchantements dans `game` : `game` dépasse vite les budgets de taille et n'est plus testable isolément.

## Conséquences

- Un module de plus, mais des responsabilités nettes et des tests de butin sans entités réelles (ports factices).
- Le découpage fin des ports est tranché avec la spec de M6, pas avant.

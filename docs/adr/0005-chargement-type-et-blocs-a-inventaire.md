# ADR 0005 : Chargement typé des données, block entities et redstone

Statut : accepté

## Contexte

Deux frontières du graphe de modules n'étaient pas tranchées.

1. `engine/data` charge les datapacks et décode les registres, mais le décodage typé des fonctions de densité, features, enchantements ou butins dépend de types définis dans `worldgen`, `loot` et `entity`, plus hauts dans le graphe.
2. `world` et `item` ne se connaissent pas, alors que des block entities à inventaire (coffres, fours, entonnoirs) vivent dans les chunks, que le comparateur lit le remplissage d'un conteneur, et que la redstone de base (M5) précède les objets (M6).

## Décision

1. **Chargement.** `engine/data` fournit l'infrastructure : empilement et priorité des sources de datapacks, arbres JSON, registres et références (`Registry`, `Holder`), combinateurs de codecs, résolution de tags. Il ne connaît aucun type métier. Chaque module qui possède un type enregistre ses codecs et ses registres auprès de `data` par une interface d'amorçage qu'il expose. `engine/game`, racine de composition, assemble l'ordre de chargement. Le jalon M2 livre l'infrastructure et le chargement de tous les fichiers en arbres JSON validés ; le décodage typé arrive avec chaque module propriétaire.
2. **Block entities.** `world` stocke les block entities comme données NBT opaques (`CompoundTag`) dans les chunks et expose un registre de types de block entities. Les comportements qui touchent à des objets (conteneurs, fours, entonnoirs) sont implémentés dans `engine/game` et enregistrés dans `world` : l'enregistrement remplace l'import.
3. **Redstone.** Les composants qui ne manipulent que des blocs (poudre, torches, répéteurs, pistons, leviers, boutons) sont des comportements de blocs dans `world`. Les composants qui lisent ou déplacent des objets (comparateur sur conteneur, entonnoir, dropper, dispenser) sont des comportements de blocs enregistrés par `game`, via un port `ContainerView` défini dans `world` qui renvoie un niveau de remplissage et des opérations de transfert, sans type d'objet.

## Alternatives écartées

- Arbres JSON non typés partout : repousse les erreurs de données à l'exécution et dilue la validation.
- Un module `registry` qui dépend de tout : crée des dépendances montantes déguisées.
- `item` dépendant de `world` : casse l'indépendance de `item` et de ses tests.

## Conséquences

- Le critère de sortie de M2 est reformulé en conséquence (voir la roadmap).
- Les ports (`ContainerView` et assimilés) sont de petites interfaces, détaillées avec la spec de M5.
- `game` porte l'assemblage de plusieurs sous-systèmes : ses budgets de taille sont surveillés, et il se découpera en sous-paquets par domaine.

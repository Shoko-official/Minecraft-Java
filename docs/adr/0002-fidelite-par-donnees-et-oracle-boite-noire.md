# ADR 0002 : Fidélité par les données et oracle en boîte noire

Statut : accepté

## Contexte

Le clone doit reproduire le jeu de référence 26.3 à l'identique. Recopier des constantes ou du code de Mojang poserait un problème juridique et rendrait le code illisible. Ne rien vérifier contre le vrai jeu rendrait la fidélité invérifiable.

## Décision

1. **Données.** Les définitions du jeu (blocs, biomes, loot, recettes, worldgen JSON) sont chargées depuis les données extraites localement du jeu de référence. Le code implémente les interprètes et algorithmes.
2. **Boîte noire.** Aucun agent ni développeur ne lit, colle ou paraphrase de code décompilé. Sources autorisées : specs du dépôt, rapports de recherche locaux, données extraites, sorties de l'oracle, documentation publique et littérature sur les algorithmes (bruit de Perlin, par exemple).
3. **Oracle.** Le vrai serveur 26.3 (et plus tard le client) est lancé localement pour produire des états de référence comparés aux nôtres (voir `docs/VERIFICATION.md`). Les écarts sont résolus par observation et expérimentation, jamais par lecture du code de référence.

## Conséquences

- Plus lent que de porter du code, mais le code est original et la fidélité est mesurée.
- Les algorithmes dont seul le comportement est observable demandent des expériences ciblées (vecteurs dorés, sondes de l'oracle) rédigées dans les specs.
- Le dépôt peut rester propre si un jour il devient public, hors marque et assets.

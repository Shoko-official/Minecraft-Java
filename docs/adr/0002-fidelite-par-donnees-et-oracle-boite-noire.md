# ADR 0002 : Fidélité par les données et oracle en boîte noire

Statut : accepté, point 2a à confirmer par le propriétaire du dépôt

## Contexte

Le clone doit reproduire le jeu de référence 26.3 à l'identique. Recopier des constantes ou du code de Mojang poserait un problème juridique et rendrait le code illisible. Ne rien vérifier contre le vrai jeu rendrait la fidélité invérifiable. Le dépôt est public.

## Décision

1. **Données.** Les définitions du jeu (blocs, biomes, loot, recettes, worldgen JSON) sont chargées depuis les données extraites localement du jeu de référence. Le code implémente les interprètes et algorithmes.
2. **Boîte noire.** Le code et les specs sont écrits à partir de comportements observés, de données extraites et de documentation publique. Précisions :
   - **2a. Exécution autorisée, lecture interdite.** On peut exécuter la distribution officielle comme une bibliothèque ou un programme (serveur, générateurs de données, appel de ses classes publiques dans un harnais isolé dans `tools/oracle`) : on fournit des entrées, on enregistre des sorties. On ne lit, ne décompile, ne colle ni ne paraphrase son code source ou son bytecode, et aucun nom de classe ou de méthode interne n'apparaît dans les specs ou le code du moteur (le harnais seul peut en contenir).
   - **2b. Statut des rapports de recherche.** Les rapports locaux contiennent des constats tirés de l'inspection du code (niveau de preuve M) et des extraits de harnais. Ils sont traités comme des indices : toute règle qui en vient est de niveau S tant qu'elle n'est pas confirmée par une sonde de l'oracle, et le `spec-analyst` n'y reprend que des comportements observables (entrées, sorties, effets), jamais de pseudo-code interne ni de noms internes.
   - **2c. Interface d'oracle par jalon.** Voir le tableau ci-dessous.
3. **Oracle.** Le vrai serveur 26.3 (et plus tard le client) est lancé localement pour produire des états de référence comparés aux nôtres (voir `docs/VERIFICATION.md`). Les écarts sont résolus par observation et expérimentation.

### Interface d'oracle par jalon

| Jalon | Ce qu'il faut observer | Interface admise |
|---|---|---|
| M1 | Séquences de RNG, tables trigonométriques, sérialisation NBT | Harnais d'appel de la distribution (2a), commande `/random`, fichiers NBT produits par le serveur |
| M2 | Registres, tags, contenu des datapacks | `--reports` et générateurs de données du serveur, fichiers du JAR |
| M3 | Blocs et chunks | Fichiers `.mca` produits par le serveur, relecture de nos mondes par le serveur (`/execute if block`, `/data get`) |
| M4 | Génération | Fichiers `.mca` après génération forcée, `/locate`, `/place` |
| M5 à M7 | Simulation, loot, entités | Commandes RCON sous `/tick freeze` et `/tick step`, `/loot`, `/data get`, scénarios sans aléa (voir VERIFICATION) |
| M8 | Protocole | Un client vanilla réel connecté à notre serveur |
| M9 | Rendu | Captures du client réel à état et caméra fixés |

## Conséquences

- Plus lent que de porter du code, mais le code est original et la fidélité est mesurée.
- Les algorithmes dont seul le comportement est observable demandent des expériences ciblées (vecteurs dorés, sondes de l'oracle) rédigées dans les specs.
- Un harnais d'appel direct reste dans une zone grise juridique (on exécute le code sans le lire) ; le propriétaire doit confirmer 2a. S'il le refuse, M1 se limite aux sorties que le serveur expose par commandes et par fichiers, et certains vecteurs seront obtenus de façon moins directe.

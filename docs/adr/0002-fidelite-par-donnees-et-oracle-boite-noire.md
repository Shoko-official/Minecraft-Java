# ADR 0002 : Fidélité par les données et oracle en boîte noire

Statut : accepté. Le point 2a (exécution) est confirmé par le propriétaire du dépôt. Le point 2a-bis (lecture des signatures publiques) attend sa décision.

## Contexte

Le clone doit reproduire le jeu de référence 26.3 à l'identique. Recopier des constantes ou du code de Mojang poserait un problème juridique et rendrait le code illisible. Ne rien vérifier contre le vrai jeu rendrait la fidélité invérifiable. Le dépôt est public.

## Décision

1. **Données.** Les définitions du jeu (blocs, biomes, loot, recettes, worldgen JSON) sont chargées depuis les données extraites localement du jeu de référence. Le code implémente les interprètes et algorithmes.
2. **Boîte noire.** Le code et les specs sont écrits à partir de comportements observés, de données extraites et de documentation publique. Précisions :
   - **2a. Exécution autorisée, lecture interdite (confirmé).** On peut exécuter la distribution officielle comme un programme : serveur, générateurs de données, et un harnais isolé dans `tools/oracle` qui lui fournit des entrées et enregistre des sorties. On ne lit, ne décompile, ne colle ni ne paraphrase son code source ou son bytecode, et aucun nom interne n'apparaît dans les specs ou le code du moteur (le harnais seul peut en contenir).
   - **2a-bis. Signatures publiques (en attente).** Écrire un harnais qui appelle des classes suppose d'en connaître les noms et les signatures. Les obtenir revient à lire des métadonnées du JAR (liste des classes, signatures, sans corps de méthode). Ce n'est pas couvert par 2a et reste interdit tant que le propriétaire ne l'a pas autorisé. Sans 2a-bis, le harnais d'appel direct n'existe pas : seules les interfaces "serveur" et "générateurs de données" du tableau sont utilisables.
   - **2b. Statut des rapports de recherche.** Les rapports locaux contiennent des constats tirés de l'inspection du code (qualifiés "moteur inspecté" dans les rapports) et des extraits de harnais. Ils sont traités comme des indices : toute règle qui en vient est de niveau S tant qu'elle n'est pas confirmée par une sonde de l'oracle. Le `spec-analyst` est la seule frontière entre ces rapports et le reste du dépôt : il n'en retient que des comportements observables (entrées, sorties, effets), jamais de pseudo-code ni de noms internes, et chaque règle reprise garde son niveau S jusqu'à confirmation.
   - **2c. Interface d'oracle par jalon.** Voir le tableau ci-dessous.
3. **Oracle.** Le vrai serveur 26.3 (et plus tard le client) est lancé localement pour produire des états de référence comparés aux nôtres (voir `docs/VERIFICATION.md`). Les écarts sont résolus par observation et expérimentation.

### Interface d'oracle par jalon

| Jalon | Ce qu'il faut observer | Interface admise sans 2a-bis | Apport de 2a-bis |
|---|---|---|---|
| M1 | Séquences de RNG, tables trigonométriques, sérialisation NBT | Commande `/random` (séquences bornées), fichiers NBT produits par le serveur ; les RNG sont des algorithmes publics vérifiés par comparaison avec leurs effets observables (positions de structures, motifs de génération) | Appel direct des RNG et de `Mth` : vecteurs dorés complets |
| M2 | Registres, tags, contenu des datapacks | `--reports` et générateurs de données du serveur, fichiers du JAR | aucun |
| M3 | Blocs et chunks | Fichiers `.mca` produits par le serveur, relecture de nos mondes par le serveur (`/execute if block`, `/data get`) | aucun |
| M4 | Génération | État final des chunks (`.mca` après génération forcée), `/locate`, `/place`. La comparaison étape par étape est une hypothèse S : elle demande des datapacks qui neutralisent des étapes (sonde à valider) ou 2a-bis | Entrées-sorties des fonctions de densité et de bruit |
| M5 à M7 | Simulation, loot, entités | Commandes RCON sous `/tick freeze` et `/tick step`, `/loot`, `/data get`, scénarios sans aléa (voir VERIFICATION) | Évaluations isolées de fonctions |
| M8 | Protocole | Un client vanilla réel connecté à notre serveur | aucun |
| M9 | Rendu | Captures du client réel à état et caméra fixés | aucun |

## Conséquences

- Plus lent que de porter du code, mais le code est original et la fidélité est mesurée.
- Les algorithmes dont seul le comportement est observable demandent des expériences ciblées (vecteurs dorés, sondes de l'oracle) rédigées dans les specs.
- Sans 2a-bis, certains vecteurs (tables numériques de `Mth`, RNG positionnels isolés) sont obtenus indirectement, par leurs effets dans le monde généré. C'est plus lent et moins précis mais reste légitime.
- Le propriétaire doit trancher 2a-bis avant que le jalon M1 ne cherche à obtenir ces vecteurs par appel direct.

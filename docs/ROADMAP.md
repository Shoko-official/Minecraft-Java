# Roadmap

Ordre : moteur sans graphique d'abord, vérifié contre le jeu de référence 26.3, puis réseau, puis client. Chaque jalon a un critère de sortie vérifiable ; un jalon n'est clos que si sa preuve est attachée (voir `docs/VERIFICATION.md`).

Niveaux de vérification cités : L1 unitaire, L2 différentiel oracle, L3 visuel, L4 fonctionnel.

| Jalon | Contenu | Critère de sortie |
|---|---|---|
| M0 Fondations | Dépôt, Gradle multi-modules, CI, hooks, `tools/bootstrap` (téléchargement vérifié, extraction des données et assets), docs, agents | `bootstrap` produit `.cache/` avec les empreintes attendues ; CI verte sur un module vide ; ArchUnit et budgets de taille actifs |
| M1 Core | RNG legacy et xoroshiro, RNG positionnels, `Mth` et tables, NBT binaire et SNBT, identifiants | Vecteurs dorés issus de l'oracle identiques (L1/L2) ; round-trip NBT sur des fichiers vanilla |
| M2 Data | Loader de datapacks, codecs, registres, tags, résolution de références | Les registres statiques et dynamiques de 26.3 se chargent ; comptes égaux à l'inventaire de référence (blocs, états, biomes, loot tables) |
| M3 World | États de blocs, sections, palettes, chunks, heightmaps, lecture et écriture Anvil | Relecture d'un monde vanilla puis réécriture : états de blocs identiques (L2) |
| M4 Worldgen Overworld | Bruit, densité, biomes, surfaces, grottes, aquifères, features, structures | Pour un jeu de seeds fixé, chaque étape (bruit, surface, grottes, features, structures) est identique à l'oracle ; cartes de biomes et de hauteurs sans écart (L2, L3) |
| M5 Simulation de blocs | Lumière, fluides, ticks planifiés et aléatoires, croissance, redstone de base | Scénarios de ticks contrôlés identiques à l'oracle (L2, L4) |
| M6 Objets | Objets et composants, inventaires, recettes, loot, enchantements | Recettes et tables de butin identiques ; distributions de loot compatibles à seed égale (L2) |
| M7 Entités | Physique, collisions, attributs, IA, spawn, dégâts, effets, combat | Traces d'entités à ticks contrôlés identiques ; spawn naturel conforme (L2, L4) |
| M8 Serveur et réseau | Boucle de jeu, commandes, gamerules, persistance, protocole 777 | Un client vanilla 26.3 se connecte, charge le monde, se déplace, pose et casse des blocs (L4) |
| M9 Client | Rendu LWJGL, modèles, animations, audio, interface, resource packs | Captures de référence comparées : terrain, mobs, interface, textures (L3) |
| M10 Dimensions et finition | Nether, End, performance, correctifs de divergences | Mêmes critères que M4 à M7 pour les deux dimensions ; budgets de performance tenus |

## Découpage en PR

Un jalon se découpe en tâches d'une PR chacune (diff idéalement sous 600 lignes). Le backlog fin de chaque jalon est créé en issues quand le jalon démarre, pas avant : les specs détaillées des jalons lointains seraient périmées.

## Ordre de démarrage proposé

1. PR `[DOCS]` : fondations documentaires et agents (présente PR).
2. PR `[CHORE]` : squelette Gradle, CI de build, ArchUnit, budgets de taille.
3. PR `[FEAT]` : `tools/bootstrap`.
4. Jalon M1.

## Suivi de l'avancement

La matrice `docs/features/matrix.md` liste chaque fonctionnalité avec son statut et sa preuve. La roadmap donne l'ordre, la matrice donne la vérité.

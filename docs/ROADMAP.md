# Roadmap

Ordre : moteur sans graphique d'abord, vérifié contre le jeu de référence 26.3, puis réseau, puis client. Chaque jalon a un critère de sortie vérifiable ; un jalon n'est clos que si sa preuve est attachée (voir `docs/VERIFICATION.md`).

Niveaux de vérification cités : L1 unitaire, L2 différentiel oracle, L3 visuel, L4 fonctionnel.

| Jalon | Contenu | Critère de sortie |
|---|---|---|
| M0 Fondations | Dépôt, Gradle multi-modules, CI, hooks, `tools/bootstrap` (téléchargement vérifié, extraction des données et assets), squelette de `tools/oracle` et `tools/diff` avec `samples.json` figé, docs, agents | `bootstrap` produit le cache avec les empreintes attendues (fait, règles de `reference-binaries` toutes à D) ; les assets graphiques sont récupérés au jalon M9 ; CI verte sur un module vide ; ArchUnit et budgets de taille actifs ; le comparateur détecte des défauts injectés dans des fixtures |
| M1 Core | RNG legacy et xoroshiro, RNG positionnels, `Mth` et tables, NBT binaire et SNBT | Vecteurs dorés issus de l'oracle (interface admise : ADR 0002) identiques (L1/L2) ; round-trip NBT sur des fichiers vanilla |
| M2 Data | Infrastructure de chargement (ADR 0005) : sources de datapacks et priorités, arbres JSON, identifiants, registres et références, codecs, tags | Tous les fichiers des datapacks 26.3 se chargent en arbres JSON validés ; comptes de fichiers par registre égaux à l'inventaire de référence ; résolution de tags identique ; le décodage typé est fait par chaque module propriétaire dans son jalon |
| M3 World | États de blocs, sections, palettes, chunks, heightmaps, lecture et écriture Anvil | Relecture d'un monde vanilla identique au lecteur indépendant de `tools/oracle` ; un monde écrit par nous est rechargé par le serveur de référence et des blocs échantillonnés (`/execute if block`) correspondent (L2) |
| M4 Worldgen Overworld | Bruit, densité, biomes, surfaces, grottes, aquifères, features, structures | Pour un jeu de seeds fixé, l'état final des chunks (blocs, biomes, block entities, starts de structures) est identique à l'oracle ; comparaison étape par étape dès qu'une sonde la rend possible (ADR 0002) ; cartes de biomes et de hauteurs sans écart (L2, L3) |
| M5 Simulation de blocs | Lumière, fluides, ticks planifiés et aléatoires, croissance, redstone de base | Scénarios déterministes (`/tick freeze` et `/tick step`, sans aléa) identiques à l'oracle ; aléa comparé statistiquement (L2, L4) |
| M6 Objets et butin | Objets et composants, inventaires, recettes, module `loot` (ADR 0004), enchantements | Recettes et tables de butin identiques ; distributions de loot compatibles avec l'oracle, comparées statistiquement (L2) |
| M7 Entités | Physique, collisions, attributs, IA, spawn, dégâts, effets, combat | Traces d'entités à ticks contrôlés (entités sans IA) identiques ; IA et spawn naturel conformes par scénarios ou statistiques (L2, L4) |
| M8 Serveur et réseau | Boucle de jeu, commandes, gamerules, persistance, protocole 777 | Un client vanilla 26.3 se connecte, charge le monde, se déplace, pose et casse des blocs (L4) |
| M9 Client | Rendu LWJGL, modèles, animations, audio, interface, resource packs | Captures de référence comparées : terrain, mobs, interface, textures (L3) |
| M10 Dimensions et finition | Nether, End, performance, correctifs de divergences | Mêmes critères que M4 à M7 pour les deux dimensions ; budgets de performance tenus |

## Découpage en PR

Un jalon se découpe en tâches d'une PR chacune (diff idéalement sous 600 lignes). Le backlog fin de chaque jalon est créé en issues quand le jalon démarre, pas avant : les specs détaillées des jalons lointains seraient périmées.

## Ordre de démarrage proposé

1. PR `[DOCS]` : fondations documentaires et agents.
2. PR `[CHORE]` : squelette Gradle, CI de build, ArchUnit, budgets de taille.
3. PR `[FEAT]` : `tools/bootstrap`.
4. Jalon M1.

## Suivi de l'avancement

La matrice `docs/features/matrix.md` liste chaque fonctionnalité avec son statut et sa preuve. La roadmap donne l'ordre, la matrice donne la vérité.

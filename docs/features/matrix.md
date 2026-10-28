# Matrice de fonctionnalités

Statuts et preuves : voir `docs/VERIFICATION.md`. La matrice est la source de vérité de l'avancement ; elle est mise à jour dans chaque PR qui change un statut.

Colonnes : fonctionnalité, jalon, risque, statut, preuve (commande, scénario ou PR).

| Fonctionnalité | Jalon | Risque | Statut | Preuve |
|---|---|---|---|---|
| RNG legacy (LCG) | M1 | A | implémenté | PR #8 ; `./gradlew :engine:core:test` (tests unitaires, aucun diff oracle) |
| RNG xoroshiro et positionnels | M1 | A | implémenté | PR #8 ; `./gradlew :engine:core:test` (tests unitaires ; règles de niveau S provisoires dans la spec, aucun diff oracle) |
| NBT binaire et SNBT | M1 | B | implémenté | PR #7 ; `./gradlew :engine:core:test` (tests unitaires, aucun diff oracle) |
| Maths et coordonnées | M1 | A | implémenté | PR #9 ; `./gradlew :engine:core:test` (tests unitaires, aucun diff oracle) |
| Chargement des datapacks et registres | M2 | B | non-spécifié | |
| Stockage Anvil (lecture et écriture) | M3 | B | non-spécifié | |
| Génération Overworld : bruit et densité | M4 | A | spécifié | `docs/specs/worldgen-noise-density.md` (brouillon, règles S, bloquant B1) |
| Génération Overworld : biomes | M4 | A | spécifié | `docs/specs/worldgen-noise-density.md` (brouillon, règles S, bloquant B3 levable par sonde ; R41 à R47) |
| Interpréteur de fonctions de densité | M4 | A | spécifié | `docs/specs/worldgen-noise-density.md` (brouillon, règles S ; R5, R12 à R35, R52) |
| Bruits nommés | M4 | A | spécifié | `docs/specs/worldgen-noise-density.md` (brouillon, règles S, bloquant B1 ; R8, R36 à R40) |
| Splines de relief | M4 | A | spécifié | `docs/specs/worldgen-noise-density.md` (brouillon, règles S ; R27, R28) |
| Source de biomes multi-bruit | M4 | A | spécifié | `docs/specs/worldgen-noise-density.md` (brouillon, règles S, bloquant B3 levable par sonde ; R41 à R47) |
| Génération Overworld : surfaces | M4 | A | spécifié | `docs/specs/worldgen-surface-carvers.md` (brouillon, règles S) |
| Génération Overworld : grottes et aquifères | M4 | A | spécifié | `docs/specs/worldgen-surface-carvers.md` (brouillon, règles S) |
| Génération Overworld : features et minerais | M4 | A | spécifié | `docs/specs/worldgen-features-structures.md` (brouillon, règles S) |
| Génération Overworld : structures | M4 | A | spécifié | `docs/specs/worldgen-features-structures.md` (brouillon, règles S) |
| Génération Overworld : séquencement des statuts | M4 | A | spécifié | `docs/specs/worldgen-pipeline.md` (brouillon, règles S) |
| Lumière | M5 | A | non-spécifié | |
| Fluides | M5 | A | non-spécifié | |
| Ticks planifiés et aléatoires | M5 | A | non-spécifié | |
| Redstone | M5 | A | non-spécifié | |
| Objets et composants | M6 | B | spécifié | `docs/specs/item-components-inventory.md` (brouillon, règles S) |
| Recettes | M6 | B | spécifié | `docs/specs/item-recipes.md` (brouillon, règles S) |
| Tables de butin | M6 | A | spécifié | `docs/specs/loot-tables.md` (brouillon, règles S) |
| Enchantements | M6 | A | spécifié | `docs/specs/item-enchantments.md` (brouillon, règles S) |
| Inventaires et menus | M6 | B | spécifié | `docs/specs/item-components-inventory.md` (brouillon, règles S) |
| Cuisson et carburants | M6 | B | spécifié | `docs/specs/item-recipes.md` (brouillon, règles S) |
| Prédicats et modificateurs d'objet | M6 | A | spécifié | `docs/specs/loot-tables.md` (brouillon, règles S) |
| Physique et collisions d'entités | M7 | A | non-spécifié | |
| IA des mobs | M7 | A | non-spécifié | |
| Spawn naturel | M7 | A | non-spécifié | |
| Dégâts, effets, combat | M7 | A | non-spécifié | |
| Commandes | M8 | B | spécifié | `docs/specs/server-commands.md` (brouillon, règles S) |
| Protocole 777 | M8 | B | spécifié | `docs/specs/net-protocol-777.md` (brouillon, règles S ; IDs de paquets D) |
| Boucle de tick, `/tick`, gamerules, temps, persistance serveur | M8 | B | spécifié | `docs/specs/server-game-loop.md` (brouillon, règles S) |
| Rendu terrain | M9 | B | non-spécifié | |
| Modèles et animations de mobs | M9 | B | non-spécifié | |
| Audio | M9 | C | non-spécifié | |
| Interface | M9 | B | non-spécifié | |
| Nether | M10 | A | non-spécifié | |
| End | M10 | A | non-spécifié | |

La liste détaillée (par bloc, mob, structure) est dérivée des registres extraits au jalon M2 et générée, pas saisie à la main.

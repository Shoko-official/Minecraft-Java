# Matrice de fonctionnalités

Statuts et preuves : voir `docs/VERIFICATION.md`. La matrice est la source de vérité de l'avancement ; elle est mise à jour dans chaque PR qui change un statut.

Colonnes : fonctionnalité, jalon, risque, statut, preuve (commande, scénario ou PR).

| Fonctionnalité | Jalon | Risque | Statut | Preuve |
|---|---|---|---|---|
| RNG legacy (LCG) | M1 | A | implémenté | PR #8 ; `./gradlew :engine:core:test` (tests unitaires, aucun diff oracle) |
| RNG xoroshiro et positionnels | M1 | A | implémenté | PR #8 ; `./gradlew :engine:core:test` (tests unitaires ; règles de niveau S provisoires dans la spec, aucun diff oracle) |
| NBT binaire et SNBT | M1 | B | implémenté | PR #7 ; `./gradlew :engine:core:test` (tests unitaires, aucun diff oracle) |
| Maths et coordonnées | M1 | A | implémenté | PR #9 ; `./gradlew :engine:core:test` (tests unitaires, aucun diff oracle) |
| Chargement des datapacks et registres | M2 | A | non-spécifié | |
| Stockage Anvil (lecture et écriture) | M3 | B | non-spécifié | |
| Génération Overworld : bruit et densité | M4 | A | non-spécifié | |
| Génération Overworld : biomes | M4 | A | non-spécifié | |
| Génération Overworld : surfaces | M4 | A | non-spécifié | |
| Génération Overworld : grottes et aquifères | M4 | A | non-spécifié | |
| Génération Overworld : features et minerais | M4 | A | non-spécifié | |
| Génération Overworld : structures | M4 | A | non-spécifié | |
| Lumière | M5 | A | non-spécifié | |
| Fluides | M5 | A | non-spécifié | |
| Ticks planifiés et aléatoires | M5 | A | non-spécifié | |
| Redstone | M5 | A | non-spécifié | |
| Objets et composants | M6 | B | non-spécifié | |
| Recettes | M6 | B | non-spécifié | |
| Tables de butin | M6 | A | non-spécifié | |
| Enchantements | M6 | A | non-spécifié | |
| Physique et collisions d'entités | M7 | A | non-spécifié | |
| IA des mobs | M7 | A | non-spécifié | |
| Spawn naturel | M7 | A | non-spécifié | |
| Dégâts, effets, combat | M7 | A | non-spécifié | |
| Commandes | M8 | B | non-spécifié | |
| Protocole 777 | M8 | B | non-spécifié | |
| Rendu terrain | M9 | B | non-spécifié | |
| Modèles et animations de mobs | M9 | B | non-spécifié | |
| Audio | M9 | C | non-spécifié | |
| Interface | M9 | B | non-spécifié | |
| Nether | M10 | A | non-spécifié | |
| End | M10 | A | non-spécifié | |

La liste détaillée (par bloc, mob, structure) est dérivée des registres extraits au jalon M2 et générée, pas saisie à la main.

# Minecraft-Java

Réimplémentation fidèle de Minecraft Java Edition 26.3 en Java 25 et LWJGL.

Objectif : reproduire le jeu de référence au plus près. À seed égale, le monde généré et les règles déterministes sont comparés bit à bit avec le vrai jeu ; les comportements qui dépendent d'un aléa de partie (butin, IA, spawn) sont comparés par scénarios contrôlés ou statistiquement (voir [la vérification](docs/VERIFICATION.md)). Le code vise la lisibilité et des modules de taille raisonnable.

Projet personnel, non affilié à Mojang ni à Microsoft. Aucun asset ni aucune donnée Mojang n'est versionné dans ce dépôt (voir [ADR 0003](docs/adr/0003-assets-et-donnees-hors-depot.md)).

## Cible figée

| Élément | Valeur |
|---|---|
| Version du jeu | 26.3 |
| Data version | 5023 (à confirmer au premier bootstrap) |
| Protocole | 777 (à confirmer au premier bootstrap) |
| Java | 25 |

## Documentation

| Document | Contenu |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, dépendances, règles de déterminisme |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Jalons M0 à M10 et critères de sortie |
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | Branches, commits, PR, merge squash |
| [docs/VERIFICATION.md](docs/VERIFICATION.md) | Oracle, niveaux de vérification, matrice de fonctionnalités |
| [docs/CONVENTIONS.md](docs/CONVENTIONS.md) | Style, budgets de taille, tests |
| [docs/AGENTS.md](docs/AGENTS.md) | Équipe de sous-agents, rôles, scopes, boucle de travail |
| [docs/adr/](docs/adr) | Décisions d'architecture |
| [docs/specs/](docs/specs) | Spécifications par sous-système |

## Mise en route

```bash
bash scripts/install-hooks.sh
```

Build, format, analyse statique, règles d'architecture et tests (Gradle télécharge un JDK 25 au besoin) :

```bash
./gradlew build
```

Formater le code : `./gradlew spotlessApply`. Le bootstrap des données de référence arrive avec la suite du jalon M0, voir la roadmap.

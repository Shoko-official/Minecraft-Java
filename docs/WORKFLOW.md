# Workflow Git

## Branches

Jamais de commit direct sur `main` : tout passe par une branche de travail et une PR. La protection de branche et les réglages de fusion cibles sont décrits plus bas ; tant qu'ils ne sont pas appliqués sur GitHub, ces règles reposent sur la discipline et les contrôles de CI.

Nom : `type/sujet-court` en minuscules avec tirets.

| Préfixe | Usage |
|---|---|
| `feat/` | Nouvelle fonctionnalité |
| `fix/` | Correction de bug |
| `refactor/` | Restructuration sans changement de comportement |
| `test/` | Ajout ou amélioration de tests |
| `docs/` | Documentation, specs, ADR |
| `chore/` | Outillage, dépendances, configuration |
| `perf/` | Optimisation mesurée |

Exemples : `feat/xoroshiro-random`, `fix/chunk-border-light`, `docs/spec-aquifers`.

## Commits

Format : `[KEYWORD] : description courte et claire`, en français, première lettre en majuscule, sans point final.

Mots-clés : `FEAT`, `FIX`, `REFACTOR`, `TEST`, `DOCS`, `CHORE`, `PERF`, `BUILD`, `CI`, `REVERT`.

```bash
git commit -m "[FEAT] : Ajout du générateur xoroshiro"
git commit -m "[FIX] : Correction du modulo sur coordonnées négatives"
git commit -m "[TEST] : Vecteurs dorés du bruit de Perlin"
```

Règles :

- Un commit = un changement cohérent. À la fusion, le résultat compile et passe les tests ; sur une branche de travail, des commits de tests écrits d'abord (rouges) sont permis, puisque le squash les absorbe.
- Première ligne : 72 caractères visés, 100 au maximum.
- Corps optionnel pour le pourquoi, séparé par une ligne vide.
- Aucune mention d'outil, d'assistant ou d'attribution automatique.
- Le hook `commit-msg` rejette les formats invalides.

## Pull requests

- Titre : même format que les commits, par exemple `[FEAT] : Ajout du générateur xoroshiro`.
- Un incrément cohérent par PR, diff idéalement sous 600 lignes (hors fichiers générés ou fixtures).
- Le template de PR demande : lien vers la spec, niveau de vérification atteint, résultat du diff oracle, images éventuelles (jointes en commentaire, pas commitées).
- Fusion : **squash uniquement**. Le message final est le titre de la PR suivi de son corps (réglages de la section Configuration GitHub cible), le corps résume le pourquoi.
- La branche est supprimée après fusion.
- Le sujet par défaut de `git revert` est accepté localement, mais le titre de la PR de revert suit le format `[REVERT] : Description`.

## Conditions de fusion

Toutes requises :

1. CI verte (hygiène, build, tests, ArchUnit, budgets).
2. Preuves du niveau de vérification exigé par le niveau de risque de la tâche (tableau ci-dessous).
3. Verdict du `reviewer` sans point bloquant ouvert.
4. Branche à jour avec `main`.

Au début du projet, la fusion est effectuée par l'orchestrateur une fois ces conditions remplies, sauf demande contraire.

## Niveaux de risque et vérifications

Pour ne pas perdre de temps, l'effort suit le risque.

| Niveau | Exemples | Parcours requis |
|---|---|---|
| A, fidélité critique | RNG, génération de monde, ordre des ticks, loot, IA | spec, tests aveugles, implémentation, diff oracle, bug-hunter, reviewer |
| B, standard | Structures de données, I/O, commandes | tests, implémentation, reviewer, oracle si le comportement est observable |
| C, plomberie | Build, outils, docs, refactor sans effet | reviewer seul |

Le niveau est choisi par l'orchestrateur dans le brief de la tâche et noté dans la PR.

## Configuration GitHub cible

À appliquer par le propriétaire (ou par l'orchestrateur avec son accord explicite) :

```bash
gh api -X PATCH repos/Shoko-official/Minecraft-Java \
  -F allow_squash_merge=true -F allow_merge_commit=false -F allow_rebase_merge=false \
  -F delete_branch_on_merge=true \
  -f squash_merge_commit_title=PR_TITLE -f squash_merge_commit_message=PR_BODY
```

Protection de `main` : checks requis `hygiene` et `check-title`, branche à jour avant fusion, pas de force-push ni de suppression.

## Hooks locaux

```bash
bash scripts/install-hooks.sh
```

Installe `core.hooksPath=.githooks`. Le hook `commit-msg` contrôle le format et l'absence d'attribution automatique ; le hook `pre-commit` lance `scripts/check-hygiene.sh --staged` sur le contenu indexé. Les hooks se contournent avec `--no-verify` : la CI refait les mêmes contrôles.

## CI

- `pr-title` : format du titre de PR, attribution dans le titre, le corps et les messages de commit de la PR.
- `ci` : hygiène du dépôt (chemins interdits, attribution, tirets, taille des fichiers Java). Build, tests, ArchUnit et contrôles de méthode s'ajoutent avec le squelette Gradle (PR `[CHORE]` suivante).
- Actions épinglées par SHA, `persist-credentials` désactivé, durée limitée.

## Fichiers jamais commités

`.cache/`, `run/`, `reports/`, `docs/reference/*` (sauf son README), assets et données Mojang, secrets. Voir `.gitignore` et l'ADR 0003.

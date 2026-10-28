# Instructions du projet

Réimplémentation de Minecraft Java 26.3 (Java 25, LWJGL). À lire avant toute tâche : `docs/ARCHITECTURE.md`, `docs/CONVENTIONS.md`, `docs/WORKFLOW.md`.

## Règles non négociables

- Fidélité : le comportement doit correspondre au jeu de référence 26.3. Une valeur n'est pas "vérifiée" sans preuve (test, diff oracle, image). Voir `docs/VERIFICATION.md`.
- Boîte noire : ne jamais lire, coller ni paraphraser de code décompilé de Mojang. Sources autorisées : specs du dépôt, données extraites dans `.cache/`, sorties de l'oracle, documentation publique.
- Aucun asset ni donnée Mojang dans git. `.cache/`, `run/`, `reports/` et `docs/reference/` sont ignorés.
- Dépendances de modules uniquement vers le bas (voir architecture). Ne pas contourner ArchUnit.
- Budgets : fichier Java 400 lignes (600 max), méthode 60 lignes.
- Ne jamais affaiblir ni supprimer un test pour le faire passer.

## Git

- Branche de travail `type/sujet-court`, jamais de commit direct sur `main`.
- Commit et titre de PR : `[KEYWORD] : description courte en français`. Mots-clés : FEAT, FIX, REFACTOR, TEST, DOCS, CHORE, PERF, BUILD, CI, REVERT.
- Merge squash uniquement.
- Aucune mention d'outil ou d'attribution automatique dans les commits, PR, code ou docs.
- Activer les hooks : `bash scripts/install-hooks.sh`.

## Langues

Docs en français, code et commentaires en anglais, descriptions de commit et de PR en français.

## Agents

Rôles, scopes et boucle de travail : `docs/AGENTS.md`. Un vérificateur ne reçoit jamais le résumé ni les hypothèses de l'auteur.

## Vérification rapide

```bash
bash scripts/check-hygiene.sh
```

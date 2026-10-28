# Équipe de sous-agents

Les définitions exécutables des rôles sont versionnées dans le dossier d'agents du dépôt (`.claude/agents/`, un fichier par rôle). Ce document donne le principe, la répartition et la boucle de travail. Les agents ont un scope étroit, un contexte minimal et des sorties typées pour éviter les biais.

## Principes

1. **Séparation des rôles.** Celui qui spécifie, celui qui écrit les tests, celui qui implémente, celui qui relit et celui qui attaque sont des agents distincts avec des contextes distincts.
2. **Contexte minimal.** Un agent reçoit uniquement ce dont son rôle a besoin : spec, fichiers du scope, critères de fin. Pas l'historique de la conversation, pas les conclusions d'un autre agent.
3. **Aucune hypothèse transmise.** Un vérificateur ne reçoit jamais "je pense que le bug vient de X". Il reçoit le diff, la spec et les résultats bruts.
4. **Scope d'écriture strict.** Chaque rôle liste les chemins qu'il peut modifier. Hors scope, il signale au lieu de corriger.
5. **Preuve ou rien.** Toute affirmation est accompagnée d'une commande, d'un fichier ou d'une image reproductibles.
6. **Pas de travail inutile.** Le niveau de risque de la tâche (voir `docs/WORKFLOW.md`) détermine quels rôles interviennent.

## Ce qui est contraint techniquement, et ce qui ne l'est pas

Le champ `tools` d'un agent limite ses outils, mais `Bash` peut écrire et `Read` lit tout le dépôt. Les consignes de scope sont donc déclaratives. Pour les rôles où l'isolation compte, on la rend structurelle avec `scripts/agent-worktree.sh` :

| Rôle | Isolation |
|---|---|
| `spec-analyst` | worktree `--docs-only` : il ne voit que `docs/` et `AGENTS.md`, jamais le code source |
| `test-engineer` | worktree sur le commit qui ne contient que le squelette d'API (signatures levant `UnsupportedOperationException`), avant toute implémentation |
| `reviewer`, `bug-hunter` | worktree jetable sur la branche à relire, supprimé ensuite ; le reviewer lit `git log --format=%s`, jamais les corps de commit |

Risques résiduels connus, assumés : le `spec-analyst` garde l'accès web pour la documentation publique et doit s'en tenir aux sources autorisées par l'ADR 0002 ; le `reviewer` vérifie que les specs ne contiennent pas de détails internes. Un contrôle plus dur (hooks sur les outils, restriction de domaines) pourra être ajouté dans les réglages des agents si ces règles sont violées en pratique.

## Rôles

| Rôle | Peut écrire | Mission | Ne fait jamais |
|---|---|---|---|
| `architect` | `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/CONVENTIONS.md`, `docs/adr/`, `*.gradle.kts`, `gradle/` | ADR, frontières de modules, contrats d'API, revue de structure | écrire du code métier |
| `spec-analyst` | `docs/specs/` | Distiller rapports, données et oracle en spec testable avec niveaux de preuve | lire ou citer l'implémentation |
| `implementer` | `src/main` du module nommé dans le brief | Poser le squelette d'API, puis implémenter une tâche à partir de la spec | modifier les tests, élargir son scope |
| `test-engineer` | `src/test` du module nommé | Écrire tests et vecteurs dorés depuis la spec, en aveugle | lire l'implémentation |
| `tooling-engineer` | `tools/`, `scripts/`, `.github/`, `.githooks/` | Construire et tester l'outillage : bootstrap, pilotage de l'oracle, comparateurs, scénarios, CI | modifier le moteur, exécuter une comparaison pour en juger le résultat |
| `oracle-differ` | `.cache/oracle/`, `reports/diff/` | Lancer l'oracle, comparer avec les outils existants, localiser le premier écart | corriger le code, modifier un comparateur ou `samples.json`, formuler une cause supposée comme un fait |
| `visual-analyst` | `reports/visual/` | Produire et lire des planches d'images, décrire les écarts | juger sans l'entrée écrite dans la légende |
| `reviewer` | rien (verdict en sortie) | Relecture non biaisée : justesse, propreté, taille, conventions, sécurité | recevoir le résumé de l'auteur, corriger lui-même |
| `bug-hunter` | tests de reproduction dans `src/test` | Attaque adverse : coordonnées négatives, bordures, overflow, déterminisme, concurrence | corriger le code |

L'orchestrateur (la session principale) crée les branches, rédige les briefs, route les livrables, ouvre et fusionne les PR, et met à jour `docs/features/matrix.md` sur la base de rapports de vérification favorables. Il ne s'auto-valide pas : le verdict du `reviewer` est requis. `.claude/agents/` est modifié par PR `[DOCS]` ou `[CHORE]` relue comme le reste.

## Format d'un brief

Tout brief contient, dans cet ordre :

```
Objectif       : une phrase
Niveau de risque : A | B | C
Spec           : chemin(s) dans docs/specs/
Scope écriture : chemins autorisés
Hors scope     : ce qu'il ne doit pas toucher
Entrées        : diff, résultats de CI, rapports (bruts)
Critères de fin : liste vérifiable
Livrable       : format attendu
```

Interdit dans un brief : hypothèses sur la cause, jugement sur la qualité du code, résumé de ce que l'auteur "a voulu faire".

## Boucle de travail d'une tâche

```mermaid
flowchart TD
    S[spec-analyst : spec] --> K[implementer : squelette d'API, commit séparé]
    K --> T[test-engineer : tests aveugles sur le squelette, rouges]
    T --> I[implementer : code jusqu'au vert]
    I --> O[oracle-differ : diff L2]
    O -->|écarts| I
    O -->|ok| V[visual-analyst : L3 si visible]
    V --> B[bug-hunter : tests adverses]
    B -->|bug| I
    B -->|ok| R[reviewer : verdict]
    R -->|bloquant| I
    R -->|ok| M[orchestrateur : PR et merge squash]
```

Parcours selon le risque :

- **A** : tout le diagramme.
- **B** : squelette, `test-engineer` (non nécessairement en aveugle), `implementer`, `reviewer`, oracle si le comportement est observable.
- **C** : `implementer`, `tooling-engineer` ou `architect` selon le chemin, puis `reviewer`.

L'`implementer` n'écrit jamais les tests qu'il doit satisfaire, y compris en niveau B : l'auteur ne se vérifie pas.

Plafond de boucle : au bout de trois allers-retours implementer / vérificateur sans convergence, l'orchestrateur s'arrête, relit la spec et le diagnostic bruts, et reformule la tâche ou la spec au lieu de relancer.

## Parallélisme

Les tâches indépendantes (modules différents, fichiers disjoints) tournent en parallèle, chacune dans sa branche et idéalement son worktree. Deux écrivains ne touchent jamais les mêmes fichiers.

## Rapports attendus des vérificateurs

- `reviewer` : liste de constats classés `BLOQUANT` / `MAJEUR` / `MINEUR`, chacun avec fichier, ligne, preuve et correction suggérée ; conclusion `APPROUVÉ` ou `CHANGEMENTS REQUIS`.
- `bug-hunter` : pour chaque bug, un test de reproduction qui échoue et la commande pour le lancer ; sinon la liste des attaques tentées.
- `oracle-differ` : rapport au format de `docs/VERIFICATION.md`.
- `visual-analyst` : planche, légende complète, liste d'écarts observés (descriptifs, sans cause supposée).

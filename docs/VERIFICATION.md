# Vérification

Le développement est mené par des agents : la fidélité ne se présume pas, elle se prouve. Ce document définit l'oracle, les niveaux de preuve et les règles qui évitent les biais de l'auteur.

## L'oracle

Le jeu de référence 26.3 installé localement sert de boîte noire :

- **Serveur** : `server.jar` officiel (empreintes dans `docs/specs/reference-binaries.md`), lancé en headless par `tools/oracle` avec une seed, des gamerules et un datapack fixés. Les sorties exploitables sont les fichiers de région `.mca`, les rapports de données (`--reports`), les sorties de commandes via RCON (`/loot`, `/data get`, `/locate`, `/place`) et les logs.
- **Client** (plus tard) : captures d'écran du vrai client à caméra et état fixés, pour comparer rendus, mobs, textures et interface.

Les sorties de l'oracle sont mises en cache dans `.cache/oracle/<version>/<seed>/<zone>/` et ne sont jamais commitées. Aucune lecture de code décompilé : l'oracle est observé, pas ouvert.

## Niveaux de preuve

| Niveau | Nom | Quoi | Coût | Quand |
|---|---|---|---|---|
| L0 | Statique | Format, Error Prone, ArchUnit, budgets de taille, hygiène | secondes | chaque PR |
| L1 | Unitaire | JUnit 5, vecteurs dorés issus de l'oracle | secondes | chaque PR |
| L2 | Différentiel | Comparaison de notre état avec celui de l'oracle, même entrée | minutes (cache) | échantillon à chaque PR de niveau A ou B, complet chaque nuit |
| L3 | Visuel | Images : cartes de biomes et de hauteurs, vue de dessus, planche "nous / référence / écart" ; plus tard mobs, textures, UI | minutes | dès qu'un sous-système a une sortie visible |
| L4 | Fonctionnel | Scénarios scriptés et matrice de fonctionnalités | variable | par jalon |

## État canonique et rapports de divergence

Chaque sous-système définit un **état canonique** comparable : par exemple, pour un chunk, la liste ordonnée des états de blocs par section, les biomes par cellule, les block entities et les starts de structures. Les comparateurs de `tools/diff` produisent :

- un empreinte par sous-état (pour un verdict rapide) ;
- au premier écart, la localisation exacte (chunk, section, coordonnées, valeur attendue, valeur obtenue) ;
- pour la génération, l'étape fautive (bruit, surface, grottes, features, structures, lumière).

Format d'un rapport (`reports/diff/<sujet>.md`, non commité, résumé collé dans la PR) :

```
Sujet      : worldgen/surface seed=12345 zone=(-4..4,-4..4)
Référence  : 26.3 oracle, cache 2026-10-28
Verdict    : DIVERGENT (3 blocs sur 1 048 576)
Premier écart : chunk (2,-1) section 4 bloc (34,70,-12) attendu minecraft:stone obtenu minecraft:dirt
Étape      : surface
Hypothèses : (vide ; l'analyste décrit, le reviewer ne reçoit pas d'hypothèse de l'auteur)
```

## Vérification visuelle

Produite par `tools/diff` et lue par le `visual-analyst` :

- vue de dessus colorée par bloc de surface, par biome, par hauteur ;
- coupes verticales (grottes, aquifères, structures) ;
- planche triple : notre rendu, la référence, la carte d'écart (rouge = différent) ;
- plus tard : planche de mobs (modèles, textures, animations à frames fixes), atlas de textures, captures d'interface.

Une image n'est une preuve que si l'entrée (seed, zone, caméra, version) est écrite dans sa légende.

## Vérification fonctionnelle

`docs/features/matrix.md` liste chaque fonctionnalité avec :

| Statut | Signification |
|---|---|
| `non-spécifié` | Connu, pas encore décrit |
| `spécifié` | Spec écrite avec niveau de preuve (D, M, C ou S) |
| `implémenté` | Code présent, tests unitaires verts |
| `vérifié-L2` | Identique à l'oracle sur le jeu d'échantillons nommé |
| `vérifié-L3` | Identique visuellement, planche jointe |
| `vérifié-L4` | Scénario de bout en bout passé |
| `divergent` | Écart connu, ticket ouvert |

Chaque ligne cite sa preuve (commande, fichier de scénario, PR). Une fonctionnalité sans preuve reste `implémenté` au mieux.

Les scénarios de conformité de la référence sont stockés dans `tools/diff/scenarios/` (format JSON : entrées, ticks, état attendu à comparer) et exécutés par l'`oracle-differ`.

## Règles anti-biais

1. **L'auteur ne se vérifie pas.** Le code est relu et attaqué par des agents qui n'ont pas écrit le code.
2. **Tests en aveugle.** Le `test-engineer` écrit depuis la spec et l'oracle, sans lire l'implémentation. Si la spec est ambiguë, il le dit au lieu de deviner.
3. **Pas d'hypothèse transmise.** Le brief d'un vérificateur contient le diff, la spec et les résultats de CI, jamais le résumé ni les conclusions de l'auteur.
4. **Les écarts ne se contournent pas.** Un test rouge se corrige dans le code, pas dans le test, sauf test prouvé faux (raison écrite dans le commit).
5. **Faits et hypothèses séparés.** Tout rapport distingue ce qui est mesuré de ce qui est supposé.
6. **Pas de "vérifié" sans commande ni fichier.** Une affirmation non reproductible est ignorée.
7. **Échantillonnage sans biais.** Les seeds et zones de test sont fixées dans `tools/diff/samples.json` avant de coder, incluant coordonnées négatives, bordures de régions et biomes rares ; on n'ajuste pas l'échantillon après un échec.

## Définition de "terminé" pour une tâche

- La spec existe et est citée.
- Les tests L1 sont verts, écrits en aveugle pour le niveau A.
- Le diff oracle (L2) est vert sur l'échantillon requis, ou chaque écart est documenté.
- Les images L3 sont jointes si la sortie est visible.
- Le `reviewer` n'a plus de point bloquant.
- La matrice de fonctionnalités est mise à jour avec la preuve.

## Gestion du temps

- Les caches oracle sont réutilisés ; on ne relance l'oracle que si la version, la seed ou la zone change.
- Les niveaux lourds (L2 complet, L3 large) tournent la nuit ; une PR exécute l'échantillon réduit défini pour son niveau de risque (voir `docs/WORKFLOW.md`).
- Une tâche de plomberie (niveau C) ne déclenche pas d'oracle.

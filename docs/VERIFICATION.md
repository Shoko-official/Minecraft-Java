# Vérification

Le développement est mené par des agents : la fidélité ne se présume pas, elle se prouve. Ce document définit l'oracle, les niveaux de preuve et les règles qui évitent les biais de l'auteur.

## L'oracle

Le jeu de référence 26.3 installé localement sert de boîte noire :

- **Serveur** : `server.jar` officiel (empreintes dans `docs/specs/reference-binaries.md`), lancé en headless par `tools/oracle` avec une seed, des gamerules et un datapack fixés. Les sorties exploitables sont les fichiers de région `.mca`, les rapports de données (`--reports`), les sorties de commandes via RCON (`/loot`, `/data get`, `/locate`, `/place`) et les logs.
- **Client** (plus tard) : captures d'écran du vrai client à caméra et état fixés, pour comparer rendus, mobs, textures et interface.

Les sorties de l'oracle sont mises en cache dans `.cache/oracle/<version>/<seed>/<zone>/` et ne sont jamais commitées. L'oracle est observé et exécuté, pas lu (ADR 0002, qui fixe aussi l'interface admise pour chaque jalon).

**Indépendance de l'instrument.** `tools/oracle` lit les fichiers de référence avec son propre lecteur minimal, sans importer `engine/*`. Sinon un bug symétrique de lecture (palette, bits par entrée) serait invisible. `tools/diff` est testé avec des fixtures contenant des défauts injectés : le comparateur doit les détecter avant d'être cru.

## Niveaux de preuve

| Niveau | Nom | Quoi | Coût | Quand |
|---|---|---|---|---|
| L0 | Statique | Format, Error Prone, ArchUnit, budgets de taille, hygiène | secondes | chaque PR |
| L1 | Unitaire | JUnit 5, vecteurs dorés issus de l'oracle | secondes | chaque PR |
| L2 | Différentiel | Comparaison de notre état avec celui de l'oracle, même entrée | minutes (cache) | échantillon à chaque PR de niveau A ou B, complet avant chaque jalon |
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
Hypothèses : aucune, ou étiquetées HYPOTHÈSE (l'opérateur décrit ; une cause supposée n'est jamais présentée comme un fait)
```

## Déterminisme de l'oracle

Hypothèse S, à lever par une sonde au jalon M5 : l'aléa en cours de partie (random ticks, IA, spawn, drops) n'est pas rejouable à l'identique entre deux exécutions de l'oracle, et les commandes RCON s'exécutent à un tick non maîtrisé. Tant que ce n'est pas levé :

- les comparaisons de simulation se font sous `/tick freeze` et `/tick step`, avec `randomTickSpeed 0`, entités sans IA et `doMobSpawning false` quand l'aléa n'est pas l'objet du test ;
- quand l'aléa est l'objet (loot, spawn, croissance), on compare des distributions sur de grands échantillons (test statistique à seuil fixé dans la spec), jamais des traces tick à tick ;
- la génération de monde, déterminée par la seed, reste comparée bit à bit.

## Où tournent les niveaux lourds

L2 et L3 tournent en local sur la machine du développeur (tâche planifiée ou lancement manuel), pas en CI GitHub : le JAR de référence n'est pas redistribué et l'acceptation de l'EULA est personnelle. La PR reçoit en commentaire le rapport résumé, avec les commandes pour le reproduire.

## Vérification visuelle

Produite par `tools/diff` et lue par le `visual-analyst` :

- vue de dessus colorée par bloc de surface, par biome, par hauteur ;
- coupes verticales (grottes, aquifères, structures) ;
- planche triple : notre rendu, la référence, la carte d'écart (rouge = différent) ;
- plus tard : planche de mobs (modèles, textures, animations à frames fixes), atlas de textures, captures d'interface.

Une image n'est une preuve que si l'entrée (seed, zone, caméra, version) est écrite dans sa légende.

## Vérification fonctionnelle

`docs/features/matrix.md` liste chaque fonctionnalité. Seul l'orchestrateur change un statut, sur la base d'un rapport de vérification (`oracle-differ`, `visual-analyst`) et d'un verdict `reviewer` favorable. Colonnes :

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
7. **Échantillonnage sans biais.** Les seeds et zones de test sont fixées dans `tools/diff/samples.json` (créé au jalon M0, puis figé) avant de coder, incluant coordonnées négatives, bordures de régions et biomes rares. Toute modification passe par une PR dédiée, jamais par l'agent qui vient d'essuyer un échec.
8. **Celui qui mesure ne construit pas l'instrument.** `oracle-differ` exécute et rapporte ; les comparateurs et scénarios sont écrits et testés par le `tooling-engineer`.

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

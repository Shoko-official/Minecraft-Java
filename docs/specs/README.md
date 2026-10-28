# Spécifications

Une spec par sous-système, courte, testable, écrite par le `spec-analyst` dans ses propres mots. Elle ne reproduit pas de code de Mojang.

Nom de fichier : `<domaine>-<sujet>.md`, par exemple `core-random.md`, `worldgen-aquifers.md`.

## Gabarit

```markdown
# <Sujet>

Statut : brouillon | relu | figé
Jalon : M<n>
Risque : A | B | C

## Périmètre
Ce que la spec couvre et ne couvre pas.

## Sources
Rapport et section, fichiers de données extraits (chemin dans la version cible), sondes de l'oracle.

## Règles
Numérotées R1, R2... Chacune porte un niveau de preuve :
D donnée exacte, M comportement observé sur l'oracle, C calcul dérivé, S à vérifier (C sur S : voir Règles).
Une règle de conception sans comportement de référence à comparer (choix du projet) porte l'étiquette `S-arch` (voir Règles).

## Entrées / sorties
Types, unités, plages, coordonnées négatives.

## Cas limites
Bordures de chunk, valeurs extrêmes, échecs reproduits.

## Vecteurs de test
Entrées et sorties attendues, avec la commande de l'oracle qui les produit.

## Questions ouvertes
Ce qui reste S et comment le lever.
```

## Règles

- Aucune règle sans niveau de preuve.
- Une règle de niveau S bloque le passage à `figé` tant qu'elle n'est pas levée par une sonde de l'oracle.
- `S-arch` : règle de conception (architecture) sans équivalent dans le jeu de référence, donc sans sonde possible : découpage en modules, ports, ordre de chargement, budgets, choix de représentation interne. Elle ne couvre jamais un comportement du jeu : une valeur de repli ou un comportement provisoire qui remplace un comportement inconnu du jeu est du S avec sa sonde. Elle est exemptée de l'exigence de sonde et ne bloque pas le passage à `figé`. Elle exprime une décision : elle renvoie à l'ADR ou à la section de `docs/ARCHITECTURE.md` qui la porte, ou s'écrit comme décision de conception dans la spec. Une règle qui décrit un comportement observable du jeu n'est jamais `S-arch`, même si son comportement provisoire est un choix de l'analyste : elle reste S avec sa sonde.
- `C sur S` : calcul reproductible (commande ou script d'annexe) fait sur une valeur de niveau S, ou sous une règle S citée. C'est une prédiction de l'analyste, pas une observation : elle porte, comme une règle S, un comportement provisoire, une sonde et un critère de succès, et elle bloque le passage à `figé` tant que la règle S dont elle dépend n'est pas levée.
- Une spec ne mentionne ni classe ni méthode de l'implémentation.

## Sources sous licence

Les specs citent des pages du Minecraft Wiki, publiées sous CC BY-NC-SA 3.0 (attribution, partage dans les mêmes conditions, pas d'usage commercial). Les pages utilisées par chaque spec, la portée de la licence et ce qu'elle implique sont dans `docs/specs/SOURCES.md`. Toute nouvelle spec qui cite une page du wiki y ajoute une ligne et cite la page et la date de consultation dans sa section « Sources ». Le dépôt n'a pas de fichier de licence : le choix de la licence du dépôt appartient au propriétaire.

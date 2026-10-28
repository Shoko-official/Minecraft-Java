# Binaires de référence

Statut : brouillon
Jalon : M0
Risque : C

## Périmètre

Identité exacte de la version de référence : versions, empreintes et sources des binaires. Ne couvre pas leur contenu.

## Sources

- Rapport de recherche `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, partie I (indice, voir ADR 0002).
- Fichier de version `26.3.json` du lanceur local, vérifié contre le JSON de version officiel (référencé par le manifeste `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`, SHA-1 `5df1795e16eea19d75b923d66ecca9ce183353c5`). Vérification faite le 2026-10-28 sur les champs `downloads.server`, `downloads.client`, `javaVersion`, `assetIndex` et `releaseTime`.

## Règles

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Version de jeu cible : `26.3`, publiée le 2026-09-15T11:23:02Z | D |
| R2 | Data version du monde : `5023` | S |
| R3 | Version de protocole : `777` | S |
| R4 | Version de datapack : `121.0`, de resource pack : `97.1` | S |
| R5 | Java requis : 25 (composant `java-runtime-epsilon`) | D |
| R6 | Taille du bundle serveur : `62 294 556` octets | D |
| R7 | SHA-1 du bundle serveur : `33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c` | D |
| R8 | SHA-256 du bundle serveur : `d052f14d7a173734fba553711e5b570162e2f2a313267ee31a21b975a679be64` | S |
| R9 | SHA-256 du JAR serveur interne : `a362163eec5d1612d520772bc16e5b39c09e3b234fdc045f56bf544284ee8ae6` | S |
| R10 | SHA-1 du client : `e877b6a07acd633fb3bb475002175cec036e7b87` | D |
| R11 | Index d'assets : id `34`, SHA-1 `917ff6d74b9f635c46bac0d28857206bbcc1ffe4` | D |

## Entrées / sorties

Téléchargement des bundles : `https://piston-data.mojang.com/v1/objects/<sha1>/server.jar` (et `client.jar`). Le JSON de version ne publie que la taille et le SHA-1 : les SHA-256 (R8, R9) ne sont pas vérifiables par lui.

## Cas limites

- Un manifeste modifié ou une réponse tronquée doivent faire échouer `bootstrap`, pas produire un cache partiel.

## Vecteurs de test

Pour R6 et R7 : télécharger `server.jar`, comparer la taille et le SHA-1 aux valeurs ci-dessus.

## Questions ouvertes

- R2, R3, R4 : lever en lisant le fichier de version embarqué dans le JAR (champs de version du monde, de protocole et de packs) lors du premier `bootstrap`.
- R8, R9 : `bootstrap` calcule les SHA-256 sur le fichier téléchargé et les enregistre dans `tools/bootstrap/pins.json` ; la valeur du rapport sert seulement de contrôle de cohérence. Ces règles passent à D (empreinte épinglée au premier téléchargement vérifié par R7) ou sont corrigées si elles divergent.
- La spec passe à `relu` quand R2, R3, R4, R8 et R9 sont levées.

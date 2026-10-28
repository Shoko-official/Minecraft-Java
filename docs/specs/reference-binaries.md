# Binaires de référence

Statut : relu
Jalon : M0
Risque : C

## Périmètre

Identité exacte de la version de référence : versions, empreintes et sources des binaires. Ne couvre pas leur contenu.

## Sources

- Rapport de recherche `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, partie I (indice de départ, voir ADR 0002).
- JSON de version officiel `26.3.json` (référencé par le manifeste `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`, SHA-1 `5df1795e16eea19d75b923d66ecca9ce183353c5`), vérifié le 2026-10-28 sur les champs `downloads.server`, `downloads.client`, `javaVersion`, `assetIndex` et `releaseTime`.
- Bundle serveur téléchargé et mesuré le 2026-10-28 : taille, SHA-1, SHA-256, fichier `version.json` du bundle, liste `META-INF/versions.list` et SHA-256 de l'archive interne.

## Règles

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Version de jeu cible : `26.3`, publiée le 2026-09-15T11:23:02Z | D |
| R2 | Data version du monde : `5023` (`world_version` du `version.json` du bundle) | D |
| R3 | Version de protocole : `777` (`protocol_version` du `version.json` du bundle) | D |
| R4 | Version de pack de données : `121.0`, de pack de ressources : `97.1` (`pack_version` du `version.json` du bundle, champs `data_major.data_minor` et `resource_major.resource_minor`) | D |
| R5 | Java requis : 25 (composant `java-runtime-epsilon`) | D |
| R6 | Taille du bundle serveur : `62 294 556` octets | D |
| R7 | SHA-1 du bundle serveur : `33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c` | D |
| R8 | SHA-256 du bundle serveur : `d052f14d7a173734fba553711e5b570162e2f2a313267ee31a21b975a679be64` (calculé sur le fichier téléchargé) | D |
| R9 | SHA-256 de l'archive serveur interne : `a362163eec5d1612d520772bc16e5b39c09e3b234fdc045f56bf544284ee8ae6` (calculé, et égal à la valeur de `versions.list`) | D |
| R10 | SHA-1 du client : `e877b6a07acd633fb3bb475002175cec036e7b87` | D |
| R11 | Index d'assets : id `34`, SHA-1 `917ff6d74b9f635c46bac0d28857206bbcc1ffe4` | D |

## Entrées / sorties

Téléchargement des bundles : `https://piston-data.mojang.com/v1/objects/<sha1>/server.jar` (et `client.jar`). Le JSON de version ne publie que la taille et le SHA-1 ; les SHA-256 (R8, R9) sont calculés au premier téléchargement et épinglés dans `tools/bootstrap/src/main/resources/pins.json`. L'identifiant de R1, R2 à R4 et R6 à R9 y sont épinglés et vérifiés à chaque reconstruction : le SHA-256 du bundle avant sa publication dans `downloads/`, puis `id`, `world_version`, `protocol_version` et `pack_version` du `version.json` du bundle, et l'archive interne contre `versions.list` et contre l'épingle.

Disposition du cache produit par `tools/bootstrap` (tout est sous un dossier ignoré par git) :

| Chemin | Contenu |
|---|---|
| `downloads/26.3.json` | JSON de version officiel |
| `downloads/server-26.3.jar` | Bundle serveur officiel |
| `reference/26.3/version.json` | Identité lue dans le bundle |
| `reference/26.3/resources/` | Ressources `data/` et `assets/` de l'archive serveur, sans aucun code |
| `reference/26.3/generated/` | Sortie du générateur de données officiel (`--reports`) |
| `reference/26.3/state.json` | Ce qui a été vérifié : empreintes épinglées, et pour `resources/` et `generated/` le nombre de fichiers, un SHA-256 de la liste triée des couples (chemin relatif, taille) et un SHA-256 du contenu |

## Cas limites

- Un JSON de version modifié, une réponse tronquée, une empreinte différente ou une entrée d'archive qui sort du dossier cible font échouer `bootstrap` sans laisser de fichier partiel publié.
- Une seconde exécution sur un cache vérifié ne télécharge ni n'extrait rien.
- `state.json` est supprimé avant que `resources/` ou `generated/` ne soit touché et écrit en dernier (fichier temporaire puis déplacement atomique) : une exécution interrompue à n'importe quel moment est refaite entièrement par la suivante.
- Un `state.json` absent, illisible, incomplet ou enregistré pour d'autres épingles ne fait jamais échouer l'outil : il provoque une reconstruction.
- Un fichier extrait supprimé, ajouté ou dont la taille change provoque une reconstruction. Un fichier réécrit avec des octets différents de même taille n'est détecté qu'avec `--verify-content`, qui relit et hache tous les fichiers extraits.
- Le nettoyage ne suit jamais un lien symbolique ni une jonction NTFS : le lien est supprimé, sa cible reste intacte. Si le chemin réel de `resources/` ou `generated/` sort du cache, l'outil refuse de nettoyer.

## Vecteurs de test

Pour l'identifiant de R1, R2 à R4 et R6 à R9 : télécharger `server.jar`, comparer taille, SHA-1 et SHA-256 aux valeurs ci-dessus, puis l'identité et l'archive interne du bundle (ce que fait `bootstrap`). `PinsTest` vérifie les valeurs épinglées. `DownloaderTest` couvre le refus d'une taille, d'un SHA-1 ou d'un SHA-256 faux avant publication (`rejectsAWrongSize`, `rejectsAWrongDigestAndLeavesNothingBehind`, `rejectsAWrongSha256BeforePublishing`). `BootstrapTest`, sur un faux bundle construit en mémoire, couvre le SHA-256 du bundle (`rejectsABundleWithAnotherSha256AndDoesNotKeepIt`), l'identité y compris `pack_version` (`rejectsABundleWithAnotherIdentity`) et l'archive interne (`rejectsAnInnerArchiveThatDiffersFromTheList`, `rejectsAnInnerArchiveThatDiffersFromThePin`).

## Questions ouvertes

Aucune. Le générateur de données produit 1 674 fichiers : les 1 666 fichiers de rapports du rapport de recherche et 8 fichiers de suivi du générateur dans `generated/.cache/`. Les ressources extraites comptent 9 887 fichiers.

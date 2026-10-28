# Binaires de référence

Statut : relu
Jalon : M0
Risque : C

## Sources

Rapport `Rapport_Integral_Overworld_Minecraft_Java_26_3.md`, partie I (niveau de preuve D), recoupé avec le fichier de version du lanceur local `26.3.json` pour Java 25.

## Règles

| Réf | Règle | Preuve |
|---|---|---|
| R1 | Version de jeu cible : `26.3`, publiée le 2026-09-15 | D |
| R2 | Data version du monde : `5023` | D |
| R3 | Version de protocole : `777` | D |
| R4 | Version de datapack : `121.0`, de resource pack : `97.1` | D |
| R5 | Java requis : 25 (`java-runtime-epsilon`) | D |
| R6 | Taille du bundle serveur : `62 294 556` octets | D |
| R7 | SHA-1 du bundle serveur : `33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c` | D |
| R8 | SHA-256 du bundle serveur : `d052f14d7a173734fba553711e5b570162e2f2a313267ee31a21b975a679be64` | D |
| R9 | SHA-256 du JAR serveur interne : `a362163eec5d1612d520772bc16e5b39c09e3b234fdc045f56bf544284ee8ae6` | D |
| R10 | SHA-1 du client : `e877b6a07acd633fb3bb475002175cec036e7b87` | D |

## Téléchargement

Manifeste officiel : `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`. Le bundle serveur se récupère par son SHA-1 sous `https://piston-data.mojang.com/v1/objects/<sha1>/server.jar`.

## Questions ouvertes

- Les empreintes viennent du rapport de recherche. `tools/bootstrap` les recalcule depuis le manifeste officiel au jalon M0 et échoue en cas de différence.

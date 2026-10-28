# Rapports de recherche locaux

Ce dossier accueille, en local seulement, les rapports de recherche qui servent de matière première aux specs. Tout sauf ce README est ignoré par git (ADR 0003).

Rapports connus (hors dépôt, dans le dossier parent du clone) :

| Fichier | Contenu |
|---|---|
| `Rapport_Integral_Overworld_Minecraft_Java_26_3.md` | Overworld 26.3 : génération, entités, butin, simulation, validation |
| `rapport-generation-minecraft-java-1-21-1.md` | Génération du monde (version antérieure, pédagogique) |
| `rapport_architecture_minecraft.md` | Architecture interne : ticks, blocs, entités, réseau, rendu |
| `nether_clone_specification.md` | Spécification Nether (valeurs à revérifier) |
| `rapport_end.md` | Dimension End (valeurs à revérifier) |

Pour les rendre disponibles aux agents, les copier ici :

```bash
cp ../Rapport_*.md ../rapport*.md ../nether_clone_specification.md docs/reference/
```

Ces fichiers sont très gros (le rapport Overworld dépasse 1 million de lignes) : ne jamais les lire en entier. Chercher par titre ou mot-clé (`grep -n '^#'`, puis lecture ciblée d'une plage de lignes).

Les valeurs des rapports sont des points de départ. Chaque règle reprise dans une spec garde son niveau de preuve (D, M, C, S) et les règles S sont levées par l'oracle avant d'être figées.

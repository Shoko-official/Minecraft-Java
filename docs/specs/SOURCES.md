# Sources sous licence

Les specs de ce dossier s'appuient sur de la documentation publique. Ce fichier recense les pages du Minecraft Wiki utilisées, la licence qui s'y applique et ce que cela implique pour les specs. Il ne choisit pas la licence du dépôt : c'est une décision du propriétaire, non prise à ce jour. Ce fichier n'est pas un avis juridique ; en cas de doute sur une reprise de contenu, le propriétaire consulte le texte de la licence ou un conseil juridique.

## Licence du Minecraft Wiki

Le texte du Minecraft Wiki (`https://minecraft.wiki`) est publié sous licence Creative Commons Attribution - NonCommercial - ShareAlike 3.0 (CC BY-NC-SA 3.0), sauf mention contraire sur une page (`https://minecraft.wiki/w/Minecraft_Wiki:Copyrights`, vérifié le 2026-10-28 : « Content on this site is licensed under CC BY-NC-SA 3.0 unless otherwise noted; additional terms may apply »). Le texte de la licence est à `https://creativecommons.org/licenses/by-nc-sa/3.0/`.

Obligations, selon le texte standard de la licence (pas selon la page du wiki, qui ne les détaille pas) :

- **Attribution** : citer la source (page du wiki, avec son adresse) et la licence, et indiquer que le contenu a été modifié. Chaque spec le fait dans sa section « Sources » ; ce fichier regroupe les pages.
- **Partage dans les mêmes conditions** : une adaptation d'un contenu du wiki ne peut être diffusée que sous (i) les termes de cette licence, (ii) une version ultérieure de cette licence avec les mêmes éléments de licence, ou (iii) une licence Creative Commons d'une autre juridiction (cette version ou une ultérieure) contenant les mêmes éléments, par exemple BY-NC-SA 3.0 Unported (section 4(b) du texte de la licence, dont le texte complet fait foi). Cela concerne les passages d'une spec qui reprennent le contenu d'une page, pas les valeurs lues dans les fichiers extraits du jeu.
- **Pas d'usage commercial** : la licence interdit un usage commercial des parties reprises.

Portée dans le dépôt :

- La colonne « Champs » de `net-protocol-777.md` reprend la structure (types et ordre des champs) de la page « Java Edition protocol/Packets ». C'est la reprise la plus directe ; elle reste sous CC BY-NC-SA 3.0 (voir aussi la note « Licence et attribution » de cette spec).
- Les autres specs ne reprennent du wiki que des valeurs, des syntaxes, des noms et des comportements reformulés, signalés au niveau de preuve S (`README.md`). Aucune règle n'est recopiée mot pour mot ; un doute sur le caractère dérivé d'une reformulation relève aussi du propriétaire.
- Les valeurs de niveau D viennent des fichiers extraits du jeu de référence (conservés hors du dépôt, ADR 0003), pas du wiki.

Les specs ont été rédigées à partir de lectures faites le 2026-10-28 ; plusieurs pages ont été lues par un outil de résumé automatique ou tronquées, ce que leur section « Sources » indique.

## Pages du Minecraft Wiki par spec

| Spec | Pages utilisées |
|---|---|
| `core-math.md` | Java Edition protocol/Data types, Java Edition protocol/Packets, Chunk format, Region file format, Coordinates, Direction, Painting, Commands/place, Target selectors, World border |
| `core-nbt.md` | NBT format, Minecraft Wiki:Projects/wiki.vg merge/NBT |
| `core-random.md` | Commands/random, Random sequence |
| `data-datapack-loading.md` | Pack format, Data pack, Pack.mcmeta, Identifier, Tag (Java Edition) |
| `dim-definitions-portals.md` | Nether portal, Dimension, End portal, End gateway, The End, Void, Respawn Anchor, Nether, Ender Dragon |
| `dim-end-generation.md` | The End, Obsidian Pillar, Ender Dragon, End gateway, End City, End Highlands, World generation, Biome, Configured feature, Density function |
| `dim-nether-generation.md` | Nether, World generation, Nether Fortress, Bastion Remnant, Biome, Configured feature, Density function |
| `item-components-inventory.md` | Java Edition protocol/Inventory, Inventory, Item modifier, Loot table, Predicate |
| `item-enchantments.md` | Enchanting mechanics, Anvil mechanics, Enchantment definition, Item modifier, Predicate |
| `item-recipes.md` | Item modifier, Loot table, Inventory, Java Edition protocol/Inventory |
| `loot-tables.md` | Loot table, Item modifier, Predicate |
| `net-protocol-777.md` | Java Edition protocol/Packets, Java Edition protocol/FAQ, Java Edition protocol/Encryption, Java Edition protocol/Server List Ping, Java Edition protocol/Registry data, Java Edition protocol/Command data, Java Edition protocol/Slot data, Java Edition protocol/Chunk format, Java Edition protocol/Entity metadata, Java Edition protocol/Data types, Server.properties |
| `server-commands.md` | Commands, Commands/tick, Commands/teleport, Commands/weather, Commands/list, Weather, Argument types, Target selectors, Permission level, Ops.json format, Java Edition protocol/Command data, Server.properties |
| `server-game-loop.md` | Tick, Commands/tick, Game rule, Java Edition level format, Player.dat format, Server.properties, Ops.json format, Whitelist.json, Permission level |
| `world-anvil-region.md` | Region file format, Java Edition level format, Java Edition 26.1 Snapshot 6, Server.properties, Java Edition 24w04a, Entity format |
| `world-block-states.md` | Chunk format, Java Edition 26.3 Snapshot 7 |
| `world-chunk-format.md` | Chunk format, Minecraft Wiki:Projects/wiki.vg merge/Chunk Format, Heightmap, Java Edition 26.3 Snapshot 7, Java Edition 26.3 Snapshot 10 |
| `worldgen-noise-density.md` | Density function, Noise settings, Noise router, Noise, Noise generator, World generation, Dimension definition |
| `worldgen-surface-carvers.md` | Material rule, Noise settings, Aquifer, Carver |
| `worldgen-features-structures.md` | Placed feature, Structure set, Jigsaw structure, Heightmap, Stronghold, Chunk format |
| `world-vectors.md` | Debug mode (niveau Y de la grille du monde de débogage), Chunk format (`PostProcessing`) |
| `worldgen-features-structures-vectors.md` | Stronghold (plages des anneaux) |
| `worldgen-pipeline.md` | Chunk format, Heightmap, Aquifer, Carver, Material rule, Placed feature |

Les adresses suivent le schéma `https://minecraft.wiki/w/<Titre_de_la_page>`. Les vecteurs non listés ci-dessus, `data-registries-inventory.md` et `reference-binaries.md` ne citent aucune page du wiki.

## Autres sources citées

Ces sources ne sont pas sous CC BY-NC-SA 3.0 ; leurs conditions propres ne sont pas établies par ce dépôt et sont à vérifier avant toute reprise de texte.

- `world-anvil-region.md` : ancienne page « Map Format » de wiki.vg (miroir `wikivg.booky.dev`), bibliothèque lz4-java (code source public, consulté pour le format de trame), spécifications publiques de LZ4 et de XXH32, RFC 1950 et RFC 1952.
- `core-nbt.md` : documentation de l'API Java SE (`java.io.DataInput`), RFC 1950 et RFC 1952.
- Les données extraites du jeu de référence (D) appartiennent à leur titulaire et restent hors du dépôt (ADR 0003).

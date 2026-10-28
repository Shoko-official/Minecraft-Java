# Vecteurs et sondes : ticks planifiés et aléatoires, mises à jour de blocs, harnais M5

Complète `sim-ticks-updates.md`. Les vecteurs SKV sont des calculs sur les données extraites ou sur les règles (preuve D, C, ou C sur S). Les sondes SK sont des expériences sur l'oracle, **non exécutées** (EULA non acceptée par le propriétaire) ; le harnais SK0 est partagé par les quatre specs M5 (`sim-light.md`, `sim-fluids.md`, `sim-ticks-updates.md`, `sim-growth-redstone.md`).

Pour ne pas recopier de données de Mojang, les tableaux volumineux ne sont pas reproduits : la commande qui les produit est donnée, avec l'empreinte SHA-256 (16 premiers caractères) de la sortie complète pour détecter une dérive des données extraites, et quelques lignes d'exemple.

## Outil et commandes

L'annexe en fin de fichier est le texte intégral de `sim_ref.py` (SHA-256 du texte de l'annexe, fins de ligne LF : `6225c9977eefdb7ba00f88dcfee81ed313944572f570aea986090bc4eb3e7d4a`). `REF` = `C:\Users\shoko\Downloads\Minecraft\.cache\reference\26.3`. Sous Windows, la sortie de `python -I` est en CRLF : les empreintes de ce fichier et les sorties reproduites sont celles de la sortie normalisée en LF (par exemple `python -I sim_ref.py REF tags | tr -d '\r' | sha256sum`, 16 premiers caractères), et l'empreinte de l'annexe porte sur son texte en LF. Les sous-commandes de données sont `python -I sim_ref.py REF <sous-commande>` ; les calculateurs sont `python -I sim_ref.py - <sous-commande>`. Le script ne lit que le cache d'extraction, n'écrit rien et ne lance pas le jeu.

| Sous-commande | Sortie | Empreinte | Vecteur |
|---|---|---|---|
| `cmds` | formes exécutables de `/tick`, `/setblock`, `/fill`, `/save-all`, `/forceload`, littéraux de `/clone`, conditions de `/execute` | f9b4a6e54b374ba4 | SKV1 |
| `rules` | identifiants, types et bornes de 14 gamerules M5 | 5e9475808ba24ed4 | SKV2 |
| `propscan` | effectifs et plages de 29 propriétés lues par la simulation | ac278bdf0a53564a | SKV3 |
| `props` | propriétés de 78 blocs choisis (état par bloc) | 602def4ad2e6590c | SKV4 |
| `tickorder` | tri d'une file de test (échéance, priorité, ordre) | 3807ed7953ef55f6 | SKV5 |
| `randtick` | minerais éteints (moyenne, écart type) pour q = 1, 0,5 et 0,1 et n tirages, avec et sans remise | 8db16f5a16467627 | SKV6 |
| `dims`, `tags`, `lightball`, `floodwater`, `comparator`, `container` | voir `sim-light-vectors.md`, `sim-fluids-vectors.md`, `sim-growth-redstone-vectors.md` | 26744735e26248d5, 421e04a44c01fd4a, a87c56dab5d0d42b, bf49316d0b095508, 3db3e6feedb49d34, 477cdd543bd266f7 | |

## SKV : vecteurs calculés

**SKV1 (R1) : formes de commandes.** Sortie de `cmds` (extrait ; la sortie complète a 45 lignes) :

```
tick 9 executable forms
   tick freeze
   tick query
   tick rate <rate>
   tick sprint stop
   tick sprint <time>
   tick step
   tick step stop
   tick step <time>
   tick unfreeze
...
   forceload remove all
   forceload remove <from>
   forceload remove <from> <to>
execute if/unless conditions ['biome', 'block', 'blocks', 'data', 'dimension', 'entity', 'function', 'items', 'loaded', 'predicate', 'score', 'slots', 'stopwatch']
execute store targets ['block', 'bossbar', 'entity', 'score', 'storage']
commands present ['test', 'debug', 'data', 'loot', 'schedule', 'place']
```

**SKV2 (R2) : gamerules.** Sortie de `rules` :

```
game_rule registry entries 59
random_tick_speed protocol_id=40 parser=brigadier:integer props={"min": 0}
water_source_conversion protocol_id=58 parser=brigadier:bool props={}
lava_source_conversion protocol_id=19 parser=brigadier:bool props={}
fire_spread_radius_around_player protocol_id=13 parser=brigadier:integer props={"min": -1}
spread_vines protocol_id=54 parser=brigadier:bool props={}
max_snow_accumulation_height protocol_id=28 parser=brigadier:integer props={"max": 8, "min": 0}
max_block_modifications protocol_id=23 parser=brigadier:integer props={"min": 1}
tnt_explodes protocol_id=55 parser=brigadier:bool props={}
mob_griefing protocol_id=31 parser=brigadier:bool props={}
advance_time protocol_id=0 parser=brigadier:bool props={}
advance_weather protocol_id=1 parser=brigadier:bool props={}
command_blocks_work protocol_id=5 parser=brigadier:bool props={}
spawn_mobs protocol_id=47 parser=brigadier:bool props={}
spawn_monsters protocol_id=48 parser=brigadier:bool props={}
```

**SKV3 (R3) : propriétés lues par la simulation.** Extrait de `propscan` (30 lignes au total) :

```
blocks 1286 states 35723
age blocks=23 value_sets=['{0,1}', '{0,1,2}', '{0,1,2,3}', '0..4(5)', '0..5(6)', '0..7(8)', '0..15(16)', '0..25(26)']
level blocks=6 value_sets=['{1,2,3}', '0..8(9)', '0..15(16)']
power blocks=7 value_sets=['0..15(16)']
lit blocks=52 value_sets=['{true,false}']
powered blocks=142 value_sets=['{true,false}']
waterlogged blocks=505 value_sets=['{true,false}']
distance blocks=15 value_sets=['1..7(7)', '0..7(8)']
persistent blocks=14 value_sets=['{true,false}']
moisture blocks=1 value_sets=['0..7(8)']
stage blocks=11 value_sets=['{0,1}']
snowy blocks=3 value_sets=['{true,false}']
```

**SKV4 : propriétés de blocs choisis.** `props` (78 lignes) ; exemples :

```
water states=16 level=0..15(16)
lava states=16 level=0..15(16)
light states=32 level=0..15(16) waterlogged={true,false}
repeater states=64 delay={1,2,3,4} facing={north,south,west,east} locked={true,false} powered={true,false}
comparator states=16 facing={north,south,west,east} mode={compare,subtract} powered={true,false}
piston states=12 extended={true,false} facing=north..down(6)
farmland states=8 moisture=0..7(8)
oak_leaves states=28 distance=1..7(7) persistent={true,false} waterlogged={true,false}
fire states=512 age=0..15(16) east={true,false} north={true,false} south={true,false} up={true,false} west={true,false}
```

**SKV5 (R11) : file de test.** Sortie de `tickorder` (calcul sur la règle de tri de R11, C sur S) :

```
input (id, due, priority, seq): [('a', 10, 0, 0), ('b', 10, -1, 1), ('c', 9, 0, 2), ('d', 10, 0, 3), ('e', 10, -3, 4), ('f', 10, -1, 5), ('g', 11, -3, 6)]
order: ['c', 'e', 'b', 'f', 'a', 'd', 'g']
order at due 10: ['e', 'b', 'f', 'a', 'd']
```

**SKV6 (R17) : tirages avec et sans remise.** Sortie de `randtick` (C sur S : prémisses = tirage uniforme parmi 4096 positions et extinction d'un minerai allumé avec la probabilité q à chaque tirage qui le touche) :

```
q 1.0 draws 1 extinguished mean 1.0000 sd 0.0000 without_replacement mean 1.0
q 1.0 draws 3 extinguished mean 2.9993 sd 0.0271 without_replacement mean 3.0
q 1.0 draws 100 extinguished mean 98.8011 sd 1.0775 without_replacement mean 100.0
q 1.0 draws 4096 extinguished mean 2589.3498 sd 19.9546 without_replacement mean 4096.0
q 0.5 draws 1 extinguished mean 0.5000 sd 0.5000 without_replacement mean 0.5
q 0.5 draws 3 extinguished mean 1.4998 sd 0.8659 without_replacement mean 1.5
q 0.5 draws 100 extinguished mean 49.6991 sd 4.9698 without_replacement mean 50.0
q 0.5 draws 4096 extinguished mean 1611.7262 sd 24.5124 without_replacement mean 2048.0
q 0.1 draws 1 extinguished mean 0.1000 sd 0.3000 without_replacement mean 0.1
q 0.1 draws 3 extinguished mean 0.3000 sd 0.5196 without_replacement mean 0.3
q 0.1 draws 100 extinguished mean 9.9879 sd 2.9948 without_replacement mean 10.0
q 0.1 draws 4096 extinguished mean 389.7905 sd 17.8653 without_replacement mean 409.6
```

## Harnais commun SK0

Prérequis de toutes les sondes M5. Les commandes citées existent dans `commands.json` (SKV1) ; leur sémantique en simulation est ce que les sondes mesurent.

- **Serveur.** Jeu de référence 26.3 lancé par `tools/oracle` (`acceptEula` donné par le propriétaire). `server.properties` : `online-mode=false`, `enable-rcon=true`, `spawn-protection=0`, `level-type=minecraft\:flat`, `generate-structures=false`, `region-file-compression=deflate`, `max-tick-time=-1`, `pause-when-empty-seconds=0` (désactive la pause selon `server-game-loop.md` R7, S : vérifier qu'un tick avance sans joueur). Aucun joueur connecté ; RCON seul.
- **Monde.** W-FLAT (voir `world-vectors.md`, préparation commune). Zone de sonde `P(cx, cz, r)` : carré de (2r+1)^2 chunks centré sur le chunk (cx, cz), tous forcés par `forceload add`, attente W de `world-vectors.md` (condition `execute if loaded` répétée jusqu'à 10 s). Au départ de chaque sonde : `gamerule max_block_modifications 2000000`, `fill` de toute la zone en air entre Y 60 et Y 150 (par lots sous la limite), `fill` d'un plancher de pierre à Y 59 (le plancher de W-FLAT est relevé par `execute if block` et la valeur 59 ajustée si besoin). Dans les montages ci-dessous, Y vaut 59 : Y+1 est la première couche libre.
- **Règles de partie.** `gamerule random_tick_speed 0`, `gamerule advance_time false`, `gamerule advance_weather false`, `gamerule spawn_mobs false`, `gamerule spawn_monsters false`, `difficulty peaceful`, `weather clear`, `time set noon`. Les gamerules à tester sont posées par chaque sonde.
- **Gel et pas.** `tick freeze` avant toute action. Pour avancer de n ticks : (1) `time query gametime` -> G0 ; (2) `tick step n` ; (3) répéter `time query gametime` toutes les 50 ms jusqu'à G0 + n (5 s au plus, sinon la sonde échoue avec "pas non terminé") ; (4) `tick query` doit dire que le jeu est gelé (clé `commands.tick.status.frozen`, `server-game-loop.md` R12). Une sonde dont l'écart de `gametime` n'est pas exactement n est invalide (cf. R16).
- **Canal d'observation O1 (état d'un bloc).** `execute if block x y z <prédicat de bloc>` (D : condition `block` présente) ; résultat 1 ou 0 lu dans la réponse RCON. Un balayage de zone se fait par `execute if blocks` (D) contre une zone de référence construite par `clone`.
- **Canal O3 (instantané de région).** `save-all flush`, puis lecture des fichiers `.mca` de `world/dimensions/minecraft/overworld/region/` (chemin à relever, `world-vectors.md`) par le lecteur indépendant de `tools/oracle`. Extraits utilisés : `block_states` (blocs), `BlockLight` et `SkyLight` (`world-chunk-format.md` R14), `block_ticks` et `fluid_ticks` (R23), `block_entities`. Contrôle de fraîcheur : le `LastUpdate` du chunk instantané est supérieur ou égal au `gametime` relevé avant la sauvegarde.
- **Canal O4 (comptes).** `execute store result score <cible> <objectif> run execute if blocks ...` ou `data get` : nombre de blocs correspondants.
- **Écriture des résultats.** `reports/probes/<id>/` (non commité) : commandes exactes, sortie brute, `gametime` à chaque pas, instantané décodé. Les vecteurs dorés sont ensuite copiés par l'orchestrateur dans `src/test/resources/golden/`.
- **Variantes de chargement.** Sans joueur, un chunk forcé doit tiquer (H-K6, levée par SK10 avec le serveur sans joueur).

## Sondes SK : ticks planifiés, ticks aléatoires, mises à jour

Notation : `S(x, y, z, B)` = `setblock x y z B`, `F(p1, p2, B)` = `fill p1 p2 B`, `STEP(n)` = pas contrôlé de SK0, `SNAP(chunk)` = `save-all flush` puis lecture O3.

**SK1 (R15, R16, R34) : gel, pas, commandes.** Zone P(0, 0, 0). Gel. (a) `time query gametime` -> G0. (b) `S(2,Y+1,2, minecraft:water)` (source posée sur le plancher). (c) Sans pas : O1 sur les quatre voisins horizontaux en `water[level=1]`, attendu : tous à 0 (R15 : le tick d'eau n'a pas eu lieu). (d) STEP(n) pour n = 1, 2, 5 : après chaque pas, O1 sur les quatre voisins, jusqu'à la première apparition de l'eau ; relever le nombre de pas k. Répéter (a) à (d) cinq fois, à des G0 différents. (e) Lampe : `S(6,Y+1,2, minecraft:redstone_lamp)`, `S(7,Y+1,2, minecraft:redstone_block)`, O1 de la lampe en `redstone_lamp[lit=true]` sans pas (R15) ; si 0, relever après STEP(1). (f) STEP(40) : `gametime` G0 + 40 exactement (R16). Critère de succès : (c) tous à 0 ; (d) k identique dans les cinq répétitions et égal au délai de SF2 pour l'eau (R6, R34) ; (f) écart égal à 40.

**SK2 (R4, R5, R6, R23) : file sauvegardée.** Zone P(0, 0, 0). (a) `S(2,Y+1,2, minecraft:water)` ; SNAP(0,0) : lister `fluid_ticks` (identifiant `i`, `p`, `t`, position) et `block_ticks` (attendu vide). (b) Trois `S(...)` de blocs à tick distincts dans le même chunk dans un ordre connu (répéteur, eau, torche de redstone au sol, lampe qui perd son courant) : SNAP : l'ordre des entrées dans la liste est-il l'ordre d'insertion à échéance égale ? Le contrôle est de répéter avec l'ordre inverse. (c) STEP(1) ; SNAP : `t` a diminué de 1 ; STEP(n) jusqu'à l'échéance + 3 sans traiter : sans objet (les ticks sont traités) ; pour un tick en retard : SK9. Critère de succès : (a) une entrée de fluide, aucune entrée de bloc ; (b) ordre de la liste égal à l'ordre d'insertion dans les deux sens (ou la sonde note que l'ordre sauvegardé est autre : alors SK11, SK12, SK13 utilisent les `t` et `p` et la position plutôt que l'ordre de liste) ; (c) `t` décroît de 1 par pas.

**SK3 (R7) : unicité.** Gel. Un répéteur (`repeater[delay=1,facing=east]`) avec un levier en entrée : trois bascules du levier (`S` de `lever[powered=true]` puis `false` puis `true` au même emplacement) sans pas ; SNAP : nombre d'entrées de `block_ticks` à la position du répéteur. Même chose pour l'eau (trois `S(3,Y+1,2, air)` puis `S(3,Y+1,2, stone)` alternés à côté d'une source) et pour la lampe (trois cycles de retrait/pose du bloc de redstone qui l'alimente). Critère de succès : exactement 1 entrée par position et par type dans les trois cas (sinon R7 est réécrite avec le nombre observé).

**SK4 (R8) : entrée périmée.** Gel. `S(2,Y+1,2, repeater)` alimenté (tick planifié en file), puis `S(2,Y+1,2, stone)` sans pas ; SNAP : l'entrée est-elle encore listée ? STEP jusqu'à l'échéance + 2 ; SNAP. Critère de succès : voir R8 (entrée présente avant l'échéance, absente après, bloc de pierre inchangé).

**SK5 (R9) : chunk qui ne tique pas.** Zone P(0, 0, 1). Eau posée dans le chunk (4, 4) ; STEP(1) ; SNAP ; `forceload remove` du chunk (4, 4) ; STEP(50) ; `forceload add` ; attente W ; SNAP ; STEP(1..6) jusqu'à l'étalement. Critère de succès : voir R9 (aucun étalement pendant l'absence ; reprise après le délai restant relevé par SNAP).

**SK6 (R11, R14) : verrouillage, ordre, chaînes.** Montage de concurrence : un répéteur R1 (`delay=1`) au repos dont l'entrée devient active deux ticks avant T (son tick de mise en route est dû en T) et un répéteur R2 (`delay=1`) dont la face de sortie touche le côté de R1 (il verrouille R1 quand il est allumé), R2 aussi en attente d'allumage dû en T. La page "Redstone Repeater" dit qu'un répéteur verrouillé ignore ses ticks. Si R2 s'exécute avant R1 (priorité -3 contre -1, R12), R1 est verrouillé avant son tick et reste éteint ; sinon R1 s'allume puis se verrouille allumé. (a) Relever l'état de R1 après STEP jusqu'à T. (b) R2 programmé avec une échéance un tick plus tard (T+2) : R1 doit s'allumer, quelle que soit la priorité. (c) Chaîne de n répéteurs au délai 1 (n = 1, 4, 16) : nombre de ticks de jeu entre la bascule du levier et le dernier répéteur. Critère de succès : (a) R1 éteint et verrouillé ; (b) R1 allumé ; (c) 2n ticks exactement (R14).

**SK7 (R10) : tick de bloc contre tick de fluide.** Zone P(0, 0, 0). Colonne de pierre en (2,Y+1,2) ; source d'eau W en (2,Y+2,2) (voisine horizontale de la cellule C) ; cellule C = (3,Y+2,2) occupée par du sable ; (3,Y+1,2) est de l'air sous C, donc le sable posé en C reçoit un tick de bloc (vérification de chute) et W un tick de fluide. À l'échéance commune T, deux issues : tick de bloc d'abord, le sable devient une entité qui tombe, C est de l'air quand le tick de fluide de W s'exécute et l'eau entre dans C ; tick de fluide d'abord, C est encore du sable, l'eau n'entre pas, et ne coulera dans C qu'environ 5 ticks plus tard après la mise à jour issue de la chute. Alignement des échéances : poser en premier celui dont le délai est le plus long, avancer de l'écart, poser l'autre, vérifier par SNAP que les deux entrées ont le même `t` ; essayer tous les écarts de 0 à 6 pas. Mesure : STEP jusqu'à l'échéance commune, puis O1 de C en `water`. Critère de succès : même résultat dans 10 répétitions à échéance alignée ; résultat contraire quand le tick de bloc est dû un tick après celui du fluide. Présupposé non sourcé : le sable posé sans appui reçoit un tick de bloc ; SNAP le vérifie, et si les délais du sable et de l'eau ne peuvent pas être alignés ou si le sable ne planifie rien, la sonde est invalidée et remplacée par un autre couple (bloc, fluide) consigné.

**SK8 (R12) : valeurs de priorité.** Gel. Six montages, SNAP après chacun : (a) répéteur qui s'allume, face de sortie contre l'arrière d'un autre répéteur ; (b) répéteur qui s'allume, face de sortie contre le côté d'un autre répéteur ; (c) répéteur qui perd son entrée ; (d) répéteur isolé qui s'allume ; (e) comparateur contre l'arrière d'un répéteur ; (f) lampe qui perd son courant. Critère de succès : champ `p` relevé = -3, -3, -2, -1, -1, 0.

**SK9 (R13) : budget par tick.** `gamerule max_block_modifications 1000000`. Zone P(0, 0, 8) (289 chunks), serveur à `max-tick-time=-1`. Remplir par `fill` de lots de 32 768 positions `water[level=7]` à échéance commune jusqu'à N = 70 000, 131 072 et 200 000 entrées de fluide dues au même tick (SNAP de contrôle sur le décompte et les `t`) ; STEP(1) ; SNAP : nombre d'entrées restantes. Variante avec des répéteurs pour les entrées de blocs. Critère de succès : R13 (restant = N - 65 536, ou budget réel consigné).

**SK10 (R17 à R21) : ticks aléatoires.** Zone P(0, 0, 0), serveur sans joueur. Montage : section Y -48..-33 du chunk (0, 0), remplie de `redstone_ore[lit=true]` (4096 blocs, réaction attendue à l'extinction au tirage, H-K5) ; décompte O4 des blocs `lit=true`. (a) Vitesse 0 : STEP(50), décompte constant. (b) Vitesse 1 : STEP(1), décompte des éteints ; 400 répétitions (remise à zéro par `fill`) ; la moyenne est q. (c) Vitesse 4096 : STEP(1), 400 répétitions, comparées à la ligne de SKV6 de même q (la formule N(1 - (1 - q/N)^n) vaut pour tout q, un minerai éteint ne pouvant plus l'être). (d) Vitesse 3 : 400 répétitions, comparées à la ligne n = 3 de SKV6 de même q (2,9993 pour q = 1). (e) Section de pierre, vitesse 4096, STEP(50) : aucun changement. (f) Deux sections superposées avec 4096 minerais chacune : les moyennes des deux sections sont indépendantes (corrélation entre les comptes des deux sections à 3 écarts types de 0). (g) Même mesure avec deux ordres de chunks (chunks forcés dans l'ordre croissant puis décroissant de leurs coordonnées). Critère de succès : (b) q relevé (1 attendu) ; (c) moyenne égale à la valeur de SKV6 pour ce q à 3 écarts types ; (a), (e) aucun changement ; (f) corrélation nulle ; (g) moyennes égales à 3 écarts types.

**SK11 (R23) : ordre des mises à jour de forme.** Gel. Un bloc central X (`stone`) ; six dalles immergées (`oak_slab[type=bottom,waterlogged=true]` ou, si la dalle ne planifie pas de tick, escaliers immergés, puis tout bloc immergé qui planifie un tick de fluide quand on l'actualise) occupent les six voisins de X ; STEP(8) pour que la file soit vide (SNAP : `fluid_ticks` vide). Puis `S(X, glass)` sans `strict`, SNAP : l'ordre des entrées de `fluid_ticks` pour les six positions. Critère de succès : ordre = ouest, est, nord, sud, bas, haut (R23), ou, si la file n'est pas ordonnée par insertion (SK2), ordre de lecture reconstruit par le plus petit `t` et la position ; sinon la sonde échoue et R23 est marquée non observable par ce canal (utiliser alors la contre-épreuve par deux pistons de SK12).

**SK12 (R24) : ordre des mises à jour de voisin.** Gel. Un bloc de redstone X entouré de six lampes allumées (une par face). `S(X, air)` (ou `S(X, stone)`) ; SNAP : ordre des six entrées de `block_ticks` (une par lampe qui planifie son extinction). Critère de succès : ordre = ouest, est, bas, haut, nord, sud (R24). Présupposé : la lampe planifie un tick d'extinction quand elle perd son courant (R72 de `sim-growth-redstone.md`, page "Redstone Lamp" ambiguë) ; sinon on s'en tient à la contre-épreuve. Contre-épreuve : un bloc B encadré par deux pistons orientés l'un vers l'autre, alimentés par la même mise à jour d'un bloc X voisin des deux pistons ; le piston mis à jour en premier déplace B ; essayer les 15 paires de directions (positions des pistons autour de X) ; la relation « gagne avant » doit être un ordre total égal à R24.

**SK13 (R25) : ordre des mises à jour de comparateur.** Gel. Un coffre vide X ; quatre comparateurs adossés aux quatre faces horizontales de X ; `item replace block X container.0 with minecraft:stone 64` ; SNAP : l'ordre des quatre entrées de `block_ticks` des comparateurs. Critère de succès : ordre = nord, est, sud, ouest (R25).

**SK14 (R26, R31) : parcours en profondeur, croissance.** (a) Montage à quatre blocs réactifs A, B, C, D : A est un bloc de redstone dont la disparition met à jour B (lampe, voisin ouest) et C (lampe, voisin est) ; B est un répéteur qui, en s'éteignant, met à jour D (lampe voisine de B seule) ; les quatre planifient un tick à la mise à jour. `S(A, air)` ; SNAP : ordre des entrées planifiées par (B, D, C) ou (B, C, D). Critère de succès (a) : (B, D, C) (R26). (b) Culture : blé planté sur terre labourée hydratée (`farmland[moisture=7]`) ; vitesse 4096 ; observateur tourné vers le blé ; distributeur quasi-alimenté voisin du blé (bloc de redstone en diagonale haute, `triggered=false`) ; STEP(1) répété jusqu'à la croissance. Critère de succès (b) : l'observateur émet une impulsion à chaque croissance et `triggered` du distributeur reste `false` (R31).

**SK15 (R27, R28, R30) : `strict`, survie.** Gel. (a) `S(2,Y+1,2, oak_stairs[facing=east,half=bottom])`, `S(2,Y+1,3, oak_stairs[facing=east,half=bottom])` ; puis `S(3,Y+1,3, oak_stairs[facing=north,half=bottom])` sans `strict` : la propriété `shape` du voisin recalculée ? Répéter avec `strict`. (b) `S(5,Y+1,5, water strict)` puis SNAP : `fluid_ticks` ; sans `strict`, idem. (c) Torche posée sur pierre (`S(7,Y+1,7, stone)`, `S(7,Y+2,7, torch)`), puis `S(7,Y+1,7, air)` sans pas : O1 de la torche. Critère de succès : (a) `shape` recalculée sans `strict`, inchangée avec ; (b) file vide avec `strict`, non vide sans ; (c) torche absente sans pas (R30).

**SK16 (R29) : ordre de `fill`.** Gel. Zone de 8 x 1 x 8 d'escaliers `oak_stairs` à `facing` alterné (nord/est), posée par un seul `fill` avec `facing` fixe, puis par une suite de `S` dans chacun des 6 ordres d'axes et 2 signes ; comparer les `shape` finales. Critère de succès : au moins un ordre de suite de `S` reproduit exactement le `fill` sur trois jeux de 8 x 8 x 8 (le `fill` d'un volume plein d'escaliers distincts) ; sinon consigner que `fill` ne se ramène à aucune suite de `S`.

**SK17 (R35, H-U2) : post-traitement et bordure.** Monde W-NORMAL graine 0, un chunk forcé à une position où l'eau ou la lave touche une bordure : lecture de `PostProcessing`, `block_ticks`, `fluid_ticks` à `features` (`worldgen-pipeline.md` WP5) puis à `full`. Variante H-U2 : bloc posé à l'extrémité d'un chunk chargé dont le voisin n'est pas chargé : le voisin non chargé reçoit-il une mise à jour à son chargement (comparer le bloc réactif voisin avant et après chargement) ? Critère de succès : R35 ; H-U2 : réponse consignée.

## Récapitulatif

- Sondes : SK0 (harnais) + 17 sondes SK1 à SK17 ; vecteurs SKV1 à SKV6.
- Prérequis : EULA acceptée, lecteur `.mca` indépendant, `tools/oracle` capable d'enchaîner RCON et lecture de région sans arrêt du serveur.
- Hypothèses nommées : H-K1 (unicité), H-K2 (entrée périmée), H-K3 (délai gelé), H-K4 (délai 0), H-K5 (extinction du minerai au tirage), H-K6 (chunk forcé sans joueur), H-K7 (l'échéance précède la priorité), H-U1 (ordre de `fill`), H-U2 (mises à jour au chargement).

## Annexe : `sim_ref.py`

Le texte intégral du script suit. Il ne lit que des fichiers du cache d'extraction et ne copie aucune donnée dans git.

```python
#!/usr/bin/env python3
"""Read-only inventory of the extracted 26.3 data used by the M5 block simulation specs
(sim-light.md, sim-fluids.md, sim-ticks-updates.md, sim-growth-redstone.md) and small
calculators for rule-derived vectors.

Usage: python -I sim_ref.py REF_DIR SUBCOMMAND     (data subcommands)
       python -I sim_ref.py - SUBCOMMAND            (calculators, no data read)
  REF_DIR = .cache/reference/26.3

Data subcommands:  dims props propscan tags rules cmds fluidclasses
Calculators:       lightball floodwater comparator container tickorder randtick
The script never writes a file and never launches the game.
"""
import hashlib
import itertools
import json
import os
import sys


def load(ref, rel):
    with open(os.path.join(ref, rel), encoding="utf-8") as fh:
        return json.load(fh)


def blocks(ref):
    return load(ref, "generated/reports/blocks.json")


def data(ref, rel):
    return load(ref, "resources/data/minecraft/" + rel)


def rng(values):
    if len(values) <= 4:
        return "{" + ",".join(values) + "}"
    return values[0] + ".." + values[-1] + "(" + str(len(values)) + ")"


def cmd_dims(ref):
    for name in ("overworld", "overworld_caves", "the_nether", "the_end"):
        d = data(ref, "dimension_type/%s.json" % name)
        keep = ("has_skylight", "has_ceiling", "min_y", "height", "logical_height", "infiniburn",
                "ambient_light", "monster_spawn_block_light_limit", "monster_spawn_light_level")
        row = ["%s=%s" % (k, json.dumps(d[k], sort_keys=True)) for k in keep if k in d]
        print(name, " ".join(row))
        att = d.get("attributes", {})
        for k in sorted(att):
            if k.startswith("minecraft:gameplay/") and k.split("/")[1] in (
                    "fast_lava", "water_evaporates", "sky_light_level", "snow_golem_melts"):
                print("  attribute", k, json.dumps(att[k]))
    end = data(ref, "dimension_type/the_end.json")
    print("the_end skybox", json.dumps(end.get("skybox")), "visual sky attributes",
          {k: v for k, v in sorted(end["attributes"].items()) if "sky_light" in k})
    day = data(ref, "timeline/day.json")
    tr = day["tracks"]["minecraft:gameplay/sky_light_level"]
    print("timeline day clock", day["clock"], "period_ticks", day["period_ticks"])
    print("  track sky_light_level modifier", tr.get("modifier"), "ease", tr.get("ease"))
    print("  keyframes", [(k["ticks"], k["value"]) for k in tr["keyframes"]])
    print("  markers", sorted((k, v if isinstance(v, int) else v["ticks"])
                              for k, v in day["time_markers"].items()))
    print("  other gameplay tracks", sorted(k for k in day["tracks"] if "gameplay" in k))


CURATED = [
    "water", "lava", "light", "redstone_wire", "repeater", "comparator", "redstone_torch",
    "redstone_wall_torch", "redstone_lamp", "redstone_block", "observer", "piston",
    "sticky_piston", "piston_head", "moving_piston", "lever", "stone_button", "oak_button",
    "stone_pressure_plate", "heavy_weighted_pressure_plate", "light_weighted_pressure_plate",
    "daylight_detector", "target", "dispenser", "dropper", "hopper", "note_block", "tripwire",
    "tripwire_hook", "copper_bulb", "crafter", "powered_rail", "wheat", "carrots", "potatoes",
    "beetroots", "nether_wart", "cocoa", "sweet_berry_bush", "sugar_cane", "cactus", "kelp",
    "bamboo", "oak_sapling", "farmland", "grass_block", "mycelium", "oak_leaves", "fire",
    "soul_fire", "vine", "snow", "ice", "melon_stem", "pumpkin_stem", "chorus_flower",
    "weeping_vines", "twisting_vines", "cave_vines", "pitcher_crop", "torchflower_crop",
    "sea_pickle", "candle", "campfire", "redstone_ore", "respawn_anchor", "bubble_column",
    "water_cauldron", "big_dripleaf", "pointed_dripstone", "dried_ghast", "sculk_catalyst",
    "sculk_sensor", "oak_stairs", "oak_slab", "oak_trapdoor", "oak_door", "oak_fence_gate",
]


def cmd_props(ref):
    b = blocks(ref)
    for n in CURATED:
        k = "minecraft:" + n
        if k not in b:
            print(n, "MISSING")
            continue
        p = b[k].get("properties", {})
        n_states = len(b[k]["states"])
        print(n, "states=%d" % n_states, " ".join("%s=%s" % (a, rng(p[a])) for a in sorted(p)))


SCAN = ["age", "level", "power", "lit", "powered", "waterlogged", "distance", "persistent",
        "moisture", "stage", "snowy", "layers", "delay", "locked", "mode", "extended",
        "triggered", "inverted", "candles", "pickles", "charges", "hydration", "berries",
        "drag", "enabled", "short", "attached", "disarmed", "bloom"]


def cmd_propscan(ref):
    b = blocks(ref)
    total_states = sum(len(v["states"]) for v in b.values())
    print("blocks", len(b), "states", total_states)
    for p in SCAN:
        holders = [n for n, v in b.items() if p in v.get("properties", {})]
        shapes = sorted({tuple(b[n]["properties"][p]) for n in holders}, key=lambda t: (len(t), t))
        print(p, "blocks=%d" % len(holders), "value_sets=%s" % [rng(list(s)) for s in shapes])


TAGS = [
    "block/air", "block/replaceable", "block/washed_away_by_fluids", "block/impermeable",
    "block/blocks_motion", "block/blocks_motion_no_leaves", "block/blocks_lava_fire_spread",
    "block/infiniburn_overworld", "block/infiniburn_nether", "block/infiniburn_end",
    "block/fire", "block/ice", "block/blocks_fluid_flow", "block/all_signs",
    "block/ice_melts_when_destroyed_above", "block/supports_melon_stem_fruit", "block/supports_pumpkin_stem_fruit", "block/snow", "block/leaves", "block/logs",
    "block/prevents_nearby_leaf_decay", "block/crops", "block/saplings", "block/supports_crops",
    "block/maintains_farmland", "block/supports_sugar_cane", "block/supports_sugar_cane_adjacently",
    "block/substrate_overworld", "block/sand", "block/supports_cactus", "block/supports_bamboo",
    "block/supports_vegetation", "block/supports_nether_wart", "block/supports_stem_crops",
    "block/supports_melon_stem", "block/supports_pumpkin_stem", "block/supports_cocoa",
    "block/dirt", "block/grass_blocks", "block/turns_into_farmland", "block/replaceable_by_trees",
    "block/cannot_support_snow_layer", "block/support_override_snow_layer", "block/buttons",
    "block/pressure_plates", "block/rails", "block/doors", "block/trapdoors", "block/fence_gates",
    "block/wool", "block/candles", "block/climbable", "block/lightning_rods",
    "block/mineable/pickaxe", "block/dampens_vibrations", "block/beacon_base_blocks",
    "fluid/water", "fluid/lava", "fluid/supports_sugar_cane_adjacently", "fluid/supports_lily_pad",
    "fluid/bubble_column_can_occupy", "game_event/vibrations",
]


def resolve(ref, tag, seen=None):
    seen = seen or set()
    if tag in seen:
        return set()
    seen.add(tag)
    path = "tags/%s.json" % tag
    if not os.path.exists(os.path.join(ref, "resources/data/minecraft", path)):
        return {"MISSING:" + tag}
    out = set()
    for v in data(ref, path)["values"]:
        entry = v if isinstance(v, str) else v["id"]
        if entry.startswith("#minecraft:"):
            out |= resolve(ref, tag.split("/")[0] + "/" + entry[len("#minecraft:"):], seen)
        else:
            out.add(entry)
    return out


def cmd_tags(ref):
    for t in TAGS:
        path = os.path.join(ref, "resources/data/minecraft/tags/%s.json" % t)
        if not os.path.exists(path):
            print(t, "MISSING")
            continue
        direct = data(ref, "tags/%s.json" % t)["values"]
        full = resolve(ref, t)
        sample = sorted(full)[:3]
        print(t, "direct=%d" % len(direct), "resolved=%d" % len(full), "first=%s" % sample)
    names = sorted(f[:-5] for f in os.listdir(os.path.join(ref, "resources/data/minecraft/tags/block"))
                   if f.endswith(".json"))
    pat = ("conduct", "piston", "push", "movable", "immovable")
    print("block tags whose name mentions", pat, "->", [n for n in names if any(w in n for w in pat)])
    print("block tag files", len(names))
    feats = sorted(f[:-5] for f in os.listdir(os.path.join(ref, "resources/data/minecraft/worldgen/feature"))
                   if f in ("oak.json", "fancy_oak.json", "birch.json", "spruce.json", "pine.json",
                            "jungle_tree.json", "mega_jungle_tree.json", "mega_spruce.json",
                            "mega_pine.json", "acacia.json", "dark_oak.json", "cherry.json",
                            "pale_oak.json", "mangrove.json", "azalea_tree.json",
                            "orange_poplar.json", "red_poplar.json", "yellow_poplar.json"))
    print("tree configured features present", len(feats))
    for i in range(0, len(feats), 6):
        print("  ", feats[i:i + 6])


RULES = ["random_tick_speed", "water_source_conversion", "lava_source_conversion",
         "fire_spread_radius_around_player", "spread_vines", "max_snow_accumulation_height",
         "max_block_modifications", "tnt_explodes", "mob_griefing", "advance_time",
         "advance_weather", "command_blocks_work", "spawn_mobs", "spawn_monsters"]


def cmd_rules(ref):
    reg = load(ref, "generated/reports/registries.json")["minecraft:game_rule"]["entries"]
    cmds = load(ref, "generated/reports/commands.json")["children"]["gamerule"]["children"]
    print("game_rule registry entries", len(reg))
    for r in RULES:
        key = "minecraft:" + r
        pid = reg[key]["protocol_id"] if key in reg else None
        node = cmds.get(r, {}).get("children", {}).get("value", {})
        print(r, "protocol_id=%s" % pid, "parser=%s" % node.get("parser"),
              "props=%s" % json.dumps(node.get("properties", {}), sort_keys=True))


def walk(node, path, out):
    if node.get("executable"):
        out.append(" ".join(path))
    for k, v in sorted(node.get("children", {}).items()):
        walk(v, path + [k if v.get("type") == "literal" else "<%s>" % k], out)


def cmd_cmds(ref):
    root = load(ref, "generated/reports/commands.json")["children"]
    for name in ("tick", "setblock", "fill", "clone", "save-all", "forceload"):
        out = []
        walk(root[name], [name], out)
        print(name, len(out), "executable forms")
        if name == "clone":
            words = sorted({w for line in out for w in line.split() if w[0] not in "<c"})
            print("   literals", words)
            continue
        for line in out:
            print("  ", line)
    ex = root["execute"]["children"]
    print("execute if/unless conditions",
          sorted(root["execute"]["children"]["if"]["children"]))
    print("execute store targets", sorted(ex["store"]["children"]["result"]["children"]))
    print("commands present", [c for c in ("test", "debug", "data", "loot", "schedule", "place")
                                 if c in root])


def cmd_lightball(_):
    # Taxicab ball in open space: cells at distance d from a single emitter of level e.
    def shell(d):
        return sum(1 for x, y, z in itertools.product(range(-d, d + 1), repeat=3)
                   if abs(x) + abs(y) + abs(z) == d)
    for e in (15, 14, 10, 7, 1):
        cells = {e - d: shell(d) for d in range(0, e)}
        print("emitter", e, "cells_by_level", dict(sorted(cells.items(), reverse=True)),
              "lit_cells", sum(cells.values()))


def cmd_floodwater(_):
    # Flat floor, one source, no obstacle, step of one level per block (water) and of two (lava).
    def ring(d):
        return 1 if d == 0 else 4 * d
    print("water reach 7:", {d: ring(d) for d in range(0, 8)}, "total", sum(ring(d) for d in range(8)))
    print("lava reach 3 (levels 2,4,6):", {d: ring(d) for d in range(0, 4)},
          "total", sum(ring(d) for d in range(4)))
    print("lava reach 4 (alternative reading of the public infobox):", {d: ring(d) for d in range(0, 5)},
          "total", sum(ring(d) for d in range(5)))
    print("nether lava reach 7:", {d: ring(d) for d in range(0, 8)}, "total", sum(ring(d) for d in range(8)))


def comparator(rear, left, right, mode):
    side = max(left, right)
    if mode == "compare":
        return rear if side <= rear else 0
    return max(rear - side, 0)


def cmd_comparator(_):
    for mode in ("compare", "subtract"):
        table = [(r, l, rt, comparator(r, l, rt, mode))
                 for r, l, rt in itertools.product(range(16), repeat=3)]
        nonzero = sum(1 for t in table if t[3] > 0)
        digest = hashlib.sha256(repr(table).encode()).hexdigest()[:16]
        print(mode, "triples", len(table), "nonzero", nonzero, "table_sha256_16", digest)
    print("examples", [("subtract", 9, 5, 2, comparator(9, 5, 2, "subtract")),
                       ("compare", 9, 5, 2, comparator(9, 5, 2, "compare")),
                       ("compare", 9, 10, 0, comparator(9, 10, 0, "compare")),
                       ("subtract", 3, 15, 0, comparator(3, 15, 0, "subtract"))])


def container(items, slots, stack):
    if items == 0:
        return 0
    return int(1 + (items / (slots * stack)) * 14)


def cmd_container(_):
    # Public rule: 0 if empty, else floor(1 + mean slot fullness * 14).
    rows = [("dispenser 9 slots, 300 items of stack 64", container(300, 9, 64)),
            ("chest 27 slots, 1 item", container(1, 27, 64)),
            ("chest 27 slots, full of stack 64", container(27 * 64, 27, 64)),
            ("chest 27 slots, 14 of 27 stacks full", container(14 * 64, 27, 64)),
            ("hopper 5 slots, 1 item of stack 16", container(1, 5, 16)),
            ("double chest 54 slots, 54 stacks", container(54 * 64, 54, 64))]
    for r in rows:
        print(r[0], "->", r[1])
    steps = [container(n, 27, 64) for n in range(0, 27 * 64 + 1, 64)]
    print("chest 27 slots, n full stacks 0..27 ->", steps)


def cmd_tickorder(_):
    # Candidate total order for scheduled block ticks (public wiki): due time, then priority
    # (lower first), then scheduling order. Vectors for a test of a queue, not an observation.
    events = [("a", 10, 0, 0), ("b", 10, -1, 1), ("c", 9, 0, 2), ("d", 10, 0, 3),
              ("e", 10, -3, 4), ("f", 10, -1, 5), ("g", 11, -3, 6)]
    order = sorted(events, key=lambda e: (e[1], e[2], e[3]))
    print("input (id, due, priority, seq):", events)
    print("order:", [e[0] for e in order])
    same_time = [e[0] for e in order if e[1] == 10]
    print("order at due 10:", same_time)


def cmd_randtick(_):
    # An ore is extinguished by a draw that hits it with probability q (a lit ore cannot be
    # extinguished twice). Distinct-cell count N(1 - (1 - 1/N)^n) is the q = 1 case.
    N = 4096
    for q in (1.0, 0.5, 0.1):
        for n in (1, 3, 100, 4096):
            p_lit = (1 - q / N) ** n
            both = (1 - 2 * q / N) ** n
            mean = N * (1 - p_lit)
            var = N * p_lit * (1 - p_lit) + N * (N - 1) * (both - p_lit * p_lit)
            print("q %.1f draws %d extinguished mean %.4f sd %.4f" % (q, n, mean, var ** 0.5),
                  "without_replacement mean %.1f" % (min(n, N) * q))


def cmd_fluidclasses(ref):
    b = blocks(ref)
    names = set(b)
    sets = {
        "air": resolve(ref, "block/air"),
        "replaceable": resolve(ref, "block/replaceable"),
        "washed_away_by_fluids": resolve(ref, "block/washed_away_by_fluids"),
        "blocks_fluid_flow": resolve(ref, "block/blocks_fluid_flow"),
        "impermeable": resolve(ref, "block/impermeable"),
    }
    for k, v in sets.items():
        print(k, "resolved", len(v), "unknown_to_registry", len(v - names))
    W, F, R = sets["washed_away_by_fluids"], sets["blocks_fluid_flow"], sets["replaceable"]
    print("washed and blocks_fluid_flow", len(W & F), "replaceable and blocks_fluid_flow", len(R & F),
          "washed and replaceable", len(W & R))
    print("washed and blocks_fluid_flow examples", sorted(W & F)[:3])
    print("replaceable only (not washed)", sorted(R - W))
    print("impermeable and blocks_fluid_flow", len(sets["impermeable"] & F))
    none = names - W - F - R - sets["air"]
    print("blocks in none of air, replaceable, washed_away, blocks_fluid_flow", len(none), "of", len(names))
    print("examples", sorted(none)[:5])


COMMANDS = {
    "dims": (cmd_dims, True), "fluidclasses": (cmd_fluidclasses, True), "props": (cmd_props, True), "propscan": (cmd_propscan, True),
    "tags": (cmd_tags, True), "rules": (cmd_rules, True), "cmds": (cmd_cmds, True),
    "lightball": (cmd_lightball, False), "floodwater": (cmd_floodwater, False),
    "comparator": (cmd_comparator, False), "container": (cmd_container, False),
    "tickorder": (cmd_tickorder, False), "randtick": (cmd_randtick, False),
}


def main(argv):
    if len(argv) != 3 or argv[2] not in COMMANDS:
        sys.stderr.write("usage: sim_ref.py REF_DIR|- SUBCOMMAND ; subcommands: %s\n"
                         % " ".join(sorted(COMMANDS)))
        return 2
    func, needs_ref = COMMANDS[argv[2]]
    if needs_ref and argv[1] == "-":
        sys.stderr.write("this subcommand needs REF_DIR\n")
        return 2
    func(argv[1])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
```

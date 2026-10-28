# Vecteurs : bruits, fonctions de densité, climat et forme du terrain

Statut : brouillon
Jalon : M4
Risque : A
Spec : [worldgen-noise-density.md](worldgen-noise-density.md)
Mise à jour : 2026-10-28 (première version)

Ce fichier contient les calculs de la spec `worldgen-noise-density`, les scripts qui les produisent et les emplacements réservés aux vecteurs de l'oracle. Les identifiants R, H, PW et V-W renvoient à la spec. Aucune donnée de Mojang n'est recopiée : les scripts lisent les fichiers extraits (`$REF`), et les sorties ne contiennent que des comptes, des valeurs calculées et des indices de table.

## Niveau de preuve des vecteurs

| Famille | Niveau | Ce qui fait foi |
|---|---|---|
| V-W1 (comptes du graphe), V-W9 (recouvrements de la table), V-W10 (typage des constantes) | C sur D | Lecture des fichiers extraits ; aucune hypothèse sur le jeu |
| V-W2 (rampe de profondeur, plateaux de bord, grilles) | C sur D | Arithmétique sur les constantes des fichiers ; l'hypothèse « float32 par nœud » ne change pas les signes |
| V-W3 (relief des splines) | C **sous H-Sp1, H-Sp2 et H-Bl** | Hermite cubique, extrapolation par la dérivée, `blend_alpha` = 1 (R27, R29) ; tolérance 1e-6 ; **S** tant que WN12 et WN6 n'ont pas répondu |
| V-W4 (weirdness repliée), V-W7 (discriminants float32) | C | Émulation float32 par nœud (R11) ; **S** pour l'égalité avec le jeu |
| V-W5 (quantification) | C | Trois définitions concurrentes H-Q1, H-Q2, H-Q3 (R41) ; laquelle est celle du jeu est **S** (WN15) |
| V-W6 (recherche de biome) | C **sous H-Q1 et la distance de R43** | Sans égalité sauf C12 : indépendant de R44 ; **S** (WN16) |
| ON-1 à ON-8 (oracle) | M une fois remplis | Réservés, à remplir par l'`oracle-differ` |

Les identifiants de biomes ne sont pas recopiés : on désigne une entrée de la table par son **indice** (R9) dans `generated/reports/biome_parameters/minecraft/overworld.json` (SHA-256 `ca63573e405dba0ef1010d6b324feeaa7d6b5c4dc895ffb6daf0988f1e44ef79`).

## Reproduire

Python 3 (bibliothèque standard seule). Recopier chaque script de la dernière section dans un fichier du même nom, puis, avec `REF` = `C:/Users/shoko/Downloads/Minecraft/.cache/reference/26.3` :

```sh
for s in census quant splines biomes f32census overlaps derived; do python -I $s.py "$REF" > $s.txt; done
python -I f32cases.py > f32cases.txt
```

Les scripts ont été exécutés tels quels le 2026-10-28 pour produire les sorties ci-dessous (`overlaps.py` prend quelques secondes, les autres moins d'une seconde).

## V-W1 Comptes du graphe atteignable (R6, R8)

Script S1. Résultat :

```
functions reachable (router + aquifers.surface_level): 24
distinct node types: 29 {'minecraft:find_top_surface': 1, 'minecraft:add': 32, 'minecraft:lerp': 7, 'minecraft:gradient': 9, 'minecraft:clamp': 6, 'minecraft:mul': 36, 'minecraft:quarter_negative': 2, 'minecraft:sub': 1, 'minecraft:div': 1, 'minecraft:cache': 17, 'minecraft:blend_alpha': 3, 'minecraft:blend_offset': 1, 'minecraft:spline': 3, 'minecraft:abs': 9, 'minecraft:noise': 37, 'minecraft:shift_b': 1, 'minecraft:shift_a': 1, 'minecraft:min': 5, 'minecraft:squeeze': 1, 'minecraft:interpolated': 6, 'minecraft:blend_density': 1, 'minecraft:range_choice': 7, 'minecraft:max': 4, 'minecraft:square': 1, 'minecraft:beardifier': 1, 'minecraft:cube': 2, 'minecraft:interval_select': 3, 'minecraft:half_negative': 1, 'minecraft:old_blended_noise': 1}
distinct noise ids: 27
interpolated (cell_size_xz, cell_size_y): {(4, 8): 5, (16, 1): 1}
spline body nodes: 118 points: 430 {'minecraft:overworld/continents': 3, 'minecraft:overworld/erosion': 11, 'minecraft:overworld/ridges_folded': 63, 'minecraft:overworld/ridges': 41}
noise definition files: 64
density function files: 55
```

## V-W2 Rampe de profondeur, plateaux de bord, grilles (R17, R25, R34, R45)

Script S7. Les constantes lues dans les fichiers ne sont pas affichées ; seuls les produits et les signes le sont. Résultat :

```
final_density grid: cells 4 48 4 corners 5 49 5 -> 1225
biome cells per chunk: 4 96 4 -> 1536 ; per section: 64; sections: 24
blocks per chunk: 98304
depth ramp slope per block: -0.0078125 = -1/128 ? True
ramp at y=-64, 63, 128, 320: [1.5, 0.5078125, 0.0, -1.5]
bottom plateau (y<=-64) constant*multiplier = 0.07499999552965164 squeeze = 0.037482419642969035 sign +
top plateau (y>=256) constant*multiplier = -0.04999999701976776 squeeze = -0.02499479017748187 sign -
```

Lecture : la rampe descend de 1/128 par bloc ; le plateau haut est négatif (blocs y >= 256 non solides sans structure) et le plateau bas positif (blocs y = -64 solides sans structure).

## V-W3 Relief : `offset`, `factor`, `jaggedness` (R27, R29, R35)

Script S3. Colonnes : continentalité `c`, érosion `e`, weirdness `w` ; `offset` = constante des données + spline (avec `blend_alpha` = 1) ; `factor` et `jaggedness` = leur spline ; `y0` = altitude où la profondeur s'annule si l'on ignore la dentelure, le bruit 3D et les grottes (R35). Sous H-Sp1 (Hermite) et H-Sp2 (extrapolation par la dérivée de l'extrémité). Précision : double ; comparer avec une tolérance de 1e-6. Résultat :

```
continents erosion ridges | offset factor jaggedness | y0 = 128 + 128*offset
  -1.2      0      0 | -0.459750 3.950000 0.000000 | 69.15
  -1.0      0      0 | -0.725950 3.950000 0.000000 | 35.08
  -0.8      0      0 | -0.725950 3.950000 0.000000 | 35.08
 -0.45      0   -0.5 | -0.629411 3.950000 0.000000 | 47.44
  -0.3      0      0 | -0.623750 3.950000 0.000000 | 48.16
 -0.15      0    0.3 | -0.503750 6.256759 0.000000 | 63.52
     0      0      0 | -0.603750 5.549900 0.000000 | 50.72
   0.1   -0.5    0.5 | -0.126962 2.670000 0.002433 | 111.75
   0.2    0.3   -0.9 | -0.479187 6.300000 0.000000 | 66.66
   0.5    0.9    0.2 | -0.523380 4.690000 0.000000 | 61.01
   0.7   -0.9   0.95 | +0.430193 4.690000 0.000000 | 183.06
   1.0   -1.0      0 | -0.155824 5.495000 0.000000 | 108.05
```

Cas à propriétés connues : pour `c` = -1,2 (sous le premier point de la spline racine), la valeur dépend de H-Sp2 (contre-hypothèse H-Sp3 : valeur constante de l'extrémité, ce qui change ici `offset`) ; pour `c` entre -1,02 et -0,51 la spline racine est constante (valeurs et dérivées identiques aux deux bouts), donc `offset` ne dépend pas de `c` dans cet intervalle (lignes `c` = -1,0 et -0,8 identiques).

## V-W4 Weirdness repliée (R28)

Script S8. Pour chaque `w` : valeur idéale `1 - |3|w| - 2|`, valeur en double avec les décimales du fichier, valeur en float32 nœud par nœud (R11). Les trois colonnes diffèrent de l'ordre de 1e-7 ; aux points -1/3 et 1/3 la valeur float32 est nulle ou voisine de zéro (signe indéterminé), ce qui compte si un seuil s'appuie sur zéro. Résultat :

```
w | ideal 1-|3|w|-2| | double with file decimals | float32 per node
+0.0000000 | -1.000000000 | -1.000000080 | -1.000000000
+0.3333333 | +0.000000000 | -0.000000080 | -0.000000000
-0.3333333 | +0.000000000 | -0.000000080 | -0.000000000
+0.6666667 | +1.000000000 | +0.999999920 | +1.000000000
-0.6666667 | +1.000000000 | +0.999999920 | +1.000000000
+1.0000000 | +0.000000000 | +0.000000120 | +0.000000089
-1.0000000 | +0.000000000 | +0.000000120 | +0.000000089
+0.5000000 | +0.500000000 | +0.499999920 | +0.499999970
-0.1000000 | -0.700000000 | -0.700000080 | -0.699999928
+0.9000000 | +0.300000000 | +0.300000120 | +0.300000161
```

## V-W5 Quantification des extrémités de la table (R41)

Script S2. Les trois colonnes sont les trois définitions H-Q1 (produit en float32 puis troncature), H-Q2 (produit en double) et H-Q3 (valeur widened en double, produit en double). Résultat :

```
value    H-Q1(float32 product)  H-Q2(double)  H-Q3(float32 value, double product)
    -1.2   -12000   -12000   -12000  
    -1.1   -11000   -11000   -11000  
   -1.05   -10500   -10500   -10499  <- Q1 != Q3
    -1.0   -10000   -10000   -10000  
 -0.9333    -9333    -9333    -9333  
   -0.85    -8500    -8500    -8500  
 -0.7799    -7799    -7799    -7799  
 -0.7666    -7666    -7665    -7666 <- Q1 != Q2 
 -0.5666    -5666    -5666    -5666  
  -0.455    -4550    -4550    -4550  
   -0.45    -4500    -4500    -4499  <- Q1 != Q3
    -0.4    -4000    -4000    -4000  
  -0.375    -3750    -3750    -3750  
   -0.35    -3500    -3500    -3499  <- Q1 != Q3
 -0.2666    -2666    -2666    -2666  
 -0.2225    -2225    -2225    -2224  <- Q1 != Q3
   -0.19    -1900    -1900    -1899  <- Q1 != Q3
   -0.15    -1500    -1500    -1500  
   -0.11    -1100    -1100    -1099  <- Q1 != Q3
    -0.1    -1000    -1000    -1000  
   -0.05     -500     -500     -500  
     0.0        0        0        0  
    0.03      300      300      299  <- Q1 != Q3
    0.05      500      500      500  
     0.1     1000     1000     1000  
     0.2     2000     2000     2000  
  0.2666     2666     2666     2666  
     0.3     3000     3000     3000  
     0.4     4000     4000     4000  
    0.45     4500     4500     4499  <- Q1 != Q3
    0.55     5500     5500     5500  
  0.5666     5666     5666     5666  
     0.7     7000     7000     6999  <- Q1 != Q3
  0.7666     7666     7665     7666 <- Q1 != Q2 
     0.8     8000     8000     8000  
     0.9     9000     9000     8999  <- Q1 != Q3
  0.9333     9333     9333     9333  
     1.0    10000    10000    10000  
     1.1    11000    11000    11000  
distinct endpoints: 39 Q1!=Q2: 2 Q1!=Q3: 10
```

Le produit de deux float32 est exact en double ; `f32()` arrondit ensuite au float32 le plus proche, ce qui émule le produit float32 sans fusion. Discriminants à lire par WN15 : -0,7666 et 0,7666 (H-Q1 contre H-Q2) ; 0,7, 0,9, 0,45, -0,35, -1,05 (H-Q1 contre H-Q3).

## V-W6 Recherche de biome (R43, R44)

Script S4, sous H-Q1 et la distance de R43. Pour chaque vecteur (température, humidité, continentalité, érosion, weirdness, profondeur) : indice de l'entrée de plus petite distance (plus petit indice en cas d'égalité, comportement provisoire de R44), distance, nombre d'entrées à égalité, distance suivante. Les vecteurs C1 à C11 n'ont **aucune égalité** (leur résultat ne dépend pas de R44) ; **C12 est une égalité** voulue (deux biomes souterrains à distance nulle : R44 la tranche) ; C13 et C11 testent la distance à un intervalle de profondeur. Résultat :

```
sha256 ca63573e405dba0ef1010d6b324feeaa7d6b5c4dc895ffb6daf0988f1e44ef79
entries 7594 biomes 56 offsets [0]
name  vector | winning entry index (lowest index among ties) | fitness | tied entries | next fitness
C1 (0.0, 0.0, 0.5, 0.0, 0.3, 0.0) | 4750 | 0 | 1  | 111556
C2 (0.8, -0.5, 0.5, 0.8, 0.3, 0.0) | 4412 | 0 | 1  | 111556
C3 (0.0, 0.0, -0.7, 0.0, 0.0, 0.0) | 10 | 0 | 1  | 2250000
C4 (0.0, 0.0, -1.1, 0.0, 0.0, 0.0) | 0 | 0 | 1  | 250000
C5 (-0.8, 0.0, 0.7, -0.9, -0.8, 0.0) | 794 | 0 | 1  | 111556
C6 (0.3, 0.0, -0.15, 0.8, 0.0, 0.0) | 3834 | 0 | 1  | 160000
C7 (0.0, 0.0, 0.9, 0.0, 0.0, 0.5) | 7590 | 0 | 1  | 25000000
C8 (0.0, 0.9, 0.0, 0.0, 0.0, 0.5) | 7591 | 0 | 1  | 25000000
C9 (0.0, 0.0, 0.2, 0.8, -0.9, 0.5) | 7592 | 0 | 1  | 25000000
C10 (0.0, 0.0, 0.0, -0.8, 0.0, 1.1) | 7593 | 0 | 1  | 1000000
C11 (0.0, 0.0, 0.0, -0.8, 0.0, 1.5) | 7593 | 16000000 | 1  | 25000000
C12 (0.0, 0.9, 0.9, 0.0, 0.0, 0.5) | 7590 | 0 | 2 [7590, 7591] | 25000000
C13 (0.0, 0.0, 0.5, 0.0, 0.3, 0.5) | 7590 | 9000000 | 1  | 25000000
```

Pour WN17 (R44), le choix de 200 égalités se fait avec le même script en prenant des vecteurs dont une coordonnée vaut exactement une extrémité commune de deux boîtes (par exemple les valeurs de la ligne « distinct endpoints » de V-W5), puis en filtrant les cas où le nombre d'entrées à égalité dépasse 1 et où les biomes diffèrent.

## V-W7 Discriminants float32 contre double (R11, R15, WN4)

Script S8. Les cas A et B sont **solides** en double et **non solides** en float32 par nœud (le signe de la sortie lit la précision). Le cas C montre que les deux ordres d'évaluation de `lerp` donnent des résultats différents pour 23 valeurs de `alpha` sur 99 avec `first` = `second` : une fonction constante `v` ne reste donc pas exactement `v` sous la forme `first * (1 - alpha) + second * alpha` en float32 (elle le reste sous la forme `first + alpha * (second - first)`). Résultat :

```
case | double | float32 per node | solid(double) | solid(float32)
A sub(add(1000000, 0.01), 1000000) | 0.010000000009313226 | 0.0 | True | False
B sub(mul(0.1, 3), 0.3) | 5.551115123125783e-17 | 0.0 | True | False
C lerp(alpha=k/100, 0.1, 0.1) - 0.1: orders differ for 23 of 99 alphas; first three (alpha, form1 - v, form2 - v): [(0.10000000149011612, -7.450580596923828e-09, 0.0), (0.1599999964237213, 7.450580596923828e-09, 0.0), (0.17000000178813934, -7.450580596923828e-09, 0.0)]
```

## V-W9 Recouvrements de la table (R10)

Script S6. Résultat :

```
entries 7594 touching-or-overlapping pairs 336654 interior-overlapping pairs [(7590, 7591), (7591, 7592)] identical boxes 0
```

## V-W10 Typage des constantes (R7)

Script S5. Résultat :

```
xz_scale     total    70  float32-text    62  double-only   8
y_scale      total    70  float32-text    56  double-only  14
location     total  1290  float32-text  1290  double-only   0
derivative   total  1290  float32-text  1290  double-only   0
value        total   945  float32-text   945  double-only   0
left         total    14  float32-text    14  double-only   0
right        total   105  float32-text   105  double-only   0
from_value   total    37  float32-text    37  double-only   0
to_value     total    37  float32-text    37  double-only   0
min          total    18  float32-text    18  double-only   0
max          total    18  float32-text    18  double-only   0
numeric fields with at least one double-only value: ['xz_scale', 'y_scale']
```

## Emplacements réservés à l'oracle

| Réf. | Contenu à remplir | Sonde |
|---|---|---|
| ON-1 | Masque de WN1 (constantes) et de WN2 (rampes) | WN1, WN2 |
| ON-2 | Sorties de WN3 (une ligne par type, dont les cas exacts sur seuil) | WN3 |
| ON-3 | Résultats de WN4 (discriminants) : cas A, B, C, motif de précision | WN4 |
| ON-4 | Tentes d'interpolation de WN5 (coins, négatifs, 16 x 1) | WN5 |
| ON-5 | Courbes de WN12 (spline) et valeur de `find_top_surface` de WN13 | WN12, WN13 |
| ON-6 | Mesures de bruit de WN8 à WN11 (écarts types, zéros, décalages, corrélations) | WN8 à WN11 |
| ON-7 | Biome par extrémité (WN15) et par égalité (WN17), positions de frontière (WN18) | WN15 à WN18 |
| ON-8 | Résumé de WN20 : nombre de chunks, blocs comparés, écarts, étape fautive | WN20 |

## Scripts

Un script par section ; ils lisent `$REF` passé en premier argument (sauf S8) et n'écrivent rien. Ils ne contiennent aucune donnée de Mojang.

### S1 Comptes du graphe (V-W1)

`census.py`

```python
import collections, glob, json, sys

REF = sys.argv[1]  # .../reference/26.3
WG = REF + "/resources/data/minecraft/worldgen/"
load = lambda p: json.load(open(p))

fns = {}
for f in glob.glob(WG + "density_function/**/*.json", recursive=True):
    key = "minecraft:" + f.replace("\\", "/").split("density_function/")[1][:-5]
    fns[key] = load(f)
ns = load(WG + "noise_settings/overworld.json")

types, refs_seen, noises, interp = collections.Counter(), set(), collections.Counter(), collections.Counter()
spl = {"nodes": 0, "points": 0, "coords": collections.Counter()}

def ref(s, todo):
    s = s if ":" in s else "minecraft:" + s
    if s in fns:
        todo.append(s)

def walk(n, todo):
    if isinstance(n, str):
        ref(n, todo)
    elif isinstance(n, list):
        for x in n:
            walk(x, todo)
    elif isinstance(n, dict):
        if "points" in n and "coordinate" in n:  # spline body
            spl["nodes"] += 1
            spl["points"] += len(n["points"])
            c = n["coordinate"]
            if isinstance(c, str):
                spl["coords"][c] += 1
                ref(c, todo)
            else:
                walk(c, todo)
            for p in n["points"]:
                walk(p["value"], todo)
            return
        t = n.get("type")
        if t:
            types[t] += 1
            if t == "minecraft:interpolated":
                interp[(n["cell_size_xz"], n["cell_size_y"])] += 1
        for k, v in n.items():
            if k in ("noise",) and isinstance(v, str) and t in ("minecraft:noise", "minecraft:shift_a", "minecraft:shift_b", "minecraft:shift"):
                noises[v] += 1
            elif k != "type":
                walk(v, todo)

roots = list(ns["noise_router"].values()) + [ns["aquifers"]["surface_level"]]
todo = list(roots)
while todo:
    k = todo.pop()
    k = k if ":" in k else "minecraft:" + k
    if k in refs_seen:
        continue
    refs_seen.add(k)
    walk(fns[k], todo)

print("functions reachable (router + aquifers.surface_level):", len(refs_seen))
print("distinct node types:", len(types), dict(types))
print("distinct noise ids:", len(noises))
print("interpolated (cell_size_xz, cell_size_y):", dict(interp))
print("spline body nodes:", spl["nodes"], "points:", spl["points"], dict(spl["coords"]))
print("noise definition files:", len(glob.glob(WG + "noise/**/*.json", recursive=True)))
print("density function files:", len(fns))
```

### S2 Quantification (V-W5)

`quant.py`

```python
import json, struct, sys

def f32(x):
    return struct.unpack("<f", struct.pack("<f", x))[0]

REF = sys.argv[1]
d = json.load(open(REF + "/generated/reports/biome_parameters/minecraft/overworld.json"))["biomes"]
ends = set()
for e in d:
    for v in e["parameters"].values():
        for x in (v if isinstance(v, list) else [v]):
            ends.add(x)
print("value    H-Q1(float32 product)  H-Q2(double)  H-Q3(float32 value, double product)")
n12 = n13 = 0
for v in sorted(ends):
    a = int(f32(f32(v) * f32(10000.0)))   # product of two float32 is exact in double, then rounded to float32; int() truncates toward zero
    b = int(v * 10000.0)
    c = int(f32(v) * 10000.0)
    n12 += a != b
    n13 += a != c
    print(f"{v:8} {a:8} {b:8} {c:8}", "<- Q1 != Q2" if a != b else "", "<- Q1 != Q3" if a != c else "")
print("distinct endpoints:", len(ends), "Q1!=Q2:", n12, "Q1!=Q3:", n13)
```

### S3 Splines de relief (V-W3)

`splines.py`

```python
import json, sys

REF = sys.argv[1]
FD = REF + "/resources/data/minecraft/worldgen/density_function/overworld/"
load = lambda n: json.load(open(FD + n + ".json"))

def hermite(x0, v0, m0, x1, v1, m1, x):
    dx = x1 - x0
    t = (x - x0) / dx
    return ((2*t**3 - 3*t**2 + 1) * v0 + (t**3 - 2*t**2 + t) * dx * m0
            + (-2*t**3 + 3*t**2) * v1 + (t**3 - t**2) * dx * m1)

def folded(w):  # root "ridges_folded" with the float32 decimals of the file, evaluated here in double
    return (abs(abs(w) - 0.6666667) - 0.33333334) * -3.0

def coord(name, c, e, w):
    return {"minecraft:overworld/continents": c, "minecraft:overworld/erosion": e,
            "minecraft:overworld/ridges": w, "minecraft:overworld/ridges_folded": folded(w)}[name]

def ev(s, c, e, w):
    if isinstance(s, (int, float)):
        return float(s)
    x = coord(s["coordinate"], c, e, w)
    P = s["points"]
    val = lambda p: ev(p["value"], c, e, w)
    if x <= P[0]["location"]:                      # H-Sp2: linear extrapolation with the end derivative
        return val(P[0]) + P[0]["derivative"] * (x - P[0]["location"])
    if x >= P[-1]["location"]:
        return val(P[-1]) + P[-1]["derivative"] * (x - P[-1]["location"])
    for a, b in zip(P, P[1:]):
        if a["location"] <= x <= b["location"]:    # H-Sp1: cubic Hermite on [a, b]
            return hermite(a["location"], val(a), a["derivative"], b["location"], val(b), b["derivative"], x)

def peel(n):  # strip cache / lerp(second) / add(right) wrappers down to the spline body
    while n["type"] != "minecraft:spline":
        n = n["input"] if n["type"] == "minecraft:cache" else n["second"] if n["type"] == "minecraft:lerp" else n["right"]
    return n["spline"]

off, fac, jag = (peel(load(n)) for n in ("offset", "factor", "jaggedness"))
off_const = load("offset")["input"]["second"]["left"]   # constant added to the offset spline (H-Bl: blend_alpha = 1)
print("continents erosion ridges | offset factor jaggedness | y0 = 128 + 128*offset")
for c, e, w in [(-1.2, 0, 0), (-1.0, 0, 0), (-0.8, 0, 0), (-0.45, 0, -0.5), (-0.3, 0, 0), (-0.15, 0, 0.3), (0, 0, 0),
                (0.1, -0.5, 0.5), (0.2, 0.3, -0.9), (0.5, 0.9, 0.2), (0.7, -0.9, 0.95), (1.0, -1.0, 0)]:
    o = off_const + ev(off, c, e, w)
    print(f"{c:6} {e:6} {w:6} | {o:+.6f} {ev(fac, c, e, w):.6f} {ev(jag, c, e, w):.6f} | {128 + 128*o:.2f}")
```

### S4 Recherche de biome (V-W6)

`biomes.py`

```python
import hashlib, json, struct, sys

def f32(x):
    return struct.unpack("<f", struct.pack("<f", x))[0]

def Q(x):  # H-Q1
    return int(f32(f32(x) * f32(10000.0)))

REF = sys.argv[1]
path = REF + "/generated/reports/biome_parameters/minecraft/overworld.json"
raw = open(path, "rb").read()
print("sha256", hashlib.sha256(raw).hexdigest())
E = json.loads(raw)["biomes"]
AX = ["temperature", "humidity", "continentalness", "erosion", "weirdness", "depth"]
iv = lambda v: (v, v) if not isinstance(v, list) else tuple(v)
LO = [[Q(iv(e["parameters"][k])[0]) for k in AX] for e in E]
HI = [[Q(iv(e["parameters"][k])[1]) for k in AX] for e in E]
OFF = [Q(e["parameters"]["offset"]) for e in E]
print("entries", len(E), "biomes", len({e["biome"] for e in E}), "offsets", sorted(set(OFF)))

def fitness(i, p):  # H-Cl1: sum over the 6 axes of the squared distance to the interval, plus offset squared
    s = OFF[i] ** 2
    for a in range(6):
        d = max(LO[i][a] - p[a], 0, p[a] - HI[i][a])
        s += d * d
    return s

def lookup(vec):
    p = [Q(x) for x in vec]
    best = sorted((fitness(i, p), i) for i in range(len(E)))
    ties = [i for f, i in best if f == best[0][0]]
    return best[0][0], ties, best[len(ties)][0] if len(ties) < len(E) else None

# (name, temperature, humidity, continentalness, erosion, weirdness, depth)
V = [("C1", 0.0, 0.0, 0.5, 0.0, 0.3, 0.0), ("C2", 0.8, -0.5, 0.5, 0.8, 0.3, 0.0), ("C3", 0.0, 0.0, -0.7, 0.0, 0.0, 0.0),
     ("C4", 0.0, 0.0, -1.1, 0.0, 0.0, 0.0), ("C5", -0.8, 0.0, 0.7, -0.9, -0.8, 0.0), ("C6", 0.3, 0.0, -0.15, 0.8, 0.0, 0.0),
     ("C7", 0.0, 0.0, 0.9, 0.0, 0.0, 0.5), ("C8", 0.0, 0.9, 0.0, 0.0, 0.0, 0.5), ("C9", 0.0, 0.0, 0.2, 0.8, -0.9, 0.5),
     ("C10", 0.0, 0.0, 0.0, -0.8, 0.0, 1.1), ("C11", 0.0, 0.0, 0.0, -0.8, 0.0, 1.5), ("C12", 0.0, 0.9, 0.9, 0.0, 0.0, 0.5),
     ("C13", 0.0, 0.0, 0.5, 0.0, 0.3, 0.5)]
print("name  vector | winning entry index (lowest index among ties) | fitness | tied entries | next fitness")
for v in V:
    f, ties, nxt = lookup(v[1:])
    print(v[0], v[1:], "|", ties[0], "|", f, "|", len(ties), ties[:4] if len(ties) > 1 else "", "|", nxt)
```

### S5 Typage des constantes (V-W10)

`f32census.py`

```python
import collections, glob, json, struct, sys

def f32(x):
    return struct.unpack("<f", struct.pack("<f", x))[0]

def shortest_f32_text_roundtrips(x):
    # True if x is the decimal value of the shortest text that identifies some float32
    for digits in range(1, 10):
        t = float(f"{x:.{digits}g}")
        if f32(t) == f32(x) and t == x:
            return True
    return False

REF = sys.argv[1]
WG = REF + "/resources/data/minecraft/worldgen/"
stat = collections.defaultdict(lambda: [0, 0])

def walk(n, key=None):
    if isinstance(n, dict):
        for k, v in n.items():
            walk(v, k)
    elif isinstance(n, list):
        for v in n:
            walk(v, key)
    elif isinstance(n, (int, float)) and not isinstance(n, bool):
        s = stat[key]
        s[0] += 1
        s[1] += shortest_f32_text_roundtrips(float(n))

for f in glob.glob(WG + "density_function/**/*.json", recursive=True) + glob.glob(WG + "noise_settings/*.json"):
    walk(json.load(open(f)))
for k in ("xz_scale", "y_scale", "location", "derivative", "value", "left", "right", "from_value", "to_value", "min", "max"):
    print(f"{k:12} total {stat[k][0]:5}  float32-text {stat[k][1]:5}  double-only {stat[k][0] - stat[k][1]:3}")
print("numeric fields with at least one double-only value:", sorted(k for k, v in stat.items() if v[0] != v[1]))
```

### S6 Recouvrements de la table (V-W9)

`overlaps.py`

```python
import json, struct, sys

def f32(x):
    return struct.unpack("<f", struct.pack("<f", x))[0]

Q = lambda x: int(f32(f32(x) * f32(10000.0)))
REF = sys.argv[1]
E = json.load(open(REF + "/generated/reports/biome_parameters/minecraft/overworld.json"))["biomes"]
H = ["temperature", "humidity", "continentalness", "erosion", "weirdness"]
iv = lambda v: (v, v) if not isinstance(v, list) else tuple(v)
lo = [[Q(iv(e["parameters"][k])[0]) for k in H] for e in E]
hi = [[Q(iv(e["parameters"][k])[1]) for k in H] for e in E]
dl = [Q(iv(e["parameters"]["depth"])[0]) for e in E]
dh = [Q(iv(e["parameters"]["depth"])[1]) for e in E]
n = len(E)
strict = []
touch = 0
ident = 0
for i in range(n):
    for j in range(i + 1, n):
        if dl[j] > dh[i] or dl[i] > dh[j]:
            continue
        if any(lo[j][a] > hi[i][a] or lo[i][a] > hi[j][a] for a in range(5)):
            continue
        touch += 1
        if all(lo[j][a] < hi[i][a] and lo[i][a] < hi[j][a] for a in range(5)):
            strict.append((i, j))
        if lo[i] == lo[j] and hi[i] == hi[j] and dl[i] == dl[j] and dh[i] == dh[j]:
            ident += 1
print("entries", n, "touching-or-overlapping pairs", touch, "interior-overlapping pairs", strict, "identical boxes", ident)
```

### S7 Rampes, plateaux et grilles (V-W2)

`derived.py`

```python
import json, struct, sys

def f32(x):
    return struct.unpack("<f", struct.pack("<f", x))[0]

REF = sys.argv[1]
WG = REF + "/resources/data/minecraft/worldgen/"
load = lambda p: json.load(open(WG + p))
ns = load("noise_settings/overworld.json")
H, MINY = ns["noise"]["height"], ns["noise"]["min_y"]

# Grids (C)
print("final_density grid: cells", 16 // 4, H // 8, 16 // 4, "corners", 16 // 4 + 1, H // 8 + 1, 16 // 4 + 1, "->", 5 * (H // 8 + 1) * 5)
print("biome cells per chunk:", 4, H // 4, 4, "->", 4 * (H // 4) * 4, "; per section: 64; sections:", H // 16)
print("blocks per chunk:", 16 * 16 * H)

# Depth ramp (C)
dep = load("density_function/overworld/depth.json")["left"]
g = lambda y: f32(dep["from_value"] + (dep["to_value"] - dep["from_value"]) * (y - dep["from_coordinate"]) / (dep["to_coordinate"] - dep["from_coordinate"]))
print("depth ramp slope per block:", (dep["to_value"] - dep["from_value"]) / (dep["to_coordinate"] - dep["from_coordinate"]), "= -1/128 ?", (dep["to_value"] - dep["from_value"]) / (dep["to_coordinate"] - dep["from_coordinate"]) == -1 / 128)
print("ramp at y=-64, 63, 128, 320:", [g(y) for y in (-64, 63, 128, 320)])

# Plateaus of the ramps in final_density (C): bottom and top constants times the final multiplier
fd = load("density_function/overworld/final_density.json")
sq = fd["left"]["left"]                              # squeeze(interpolated(mul(...)))
mul = sq["input"]["input"]
mult = mul["right"]
low = mul["left"]["input"]["first"]                  # blend_density(lerp(alpha_low, first, second))
high = mul["left"]["input"]["second"]["first"]      # inner lerp(alpha_high, first, second)
sqz = lambda x: x / 2 - x * x * x / 24 if -1 <= x <= 1 else (0.5 - 1 / 24 if x > 1 else -(0.5 - 1 / 24))
for name, c in (("bottom plateau (y<=-64)", low), ("top plateau (y>=256)", high)):
    v = f32(f32(c) * f32(mult))
    print(name, "constant*multiplier =", v, "squeeze =", sqz(v), "sign", "+" if sqz(v) > 0 else "-")
```

### S8 Weirdness repliée et discriminants (V-W4, V-W7), ne lit pas `$REF`

`f32cases.py`

```python
import struct

def f32(x):
    return struct.unpack("<f", struct.pack("<f", x))[0]

# --- ridges_folded: (|(|w| - a)| - b) * -3 with the float32 decimals of the data file (a = 0.6666667, b = 0.33333334)
a32, b32 = f32(0.6666667), f32(0.33333334)
print("w | ideal 1-|3|w|-2| | double with file decimals | float32 per node")
for w in (0.0, 1/3, -1/3, 2/3, -2/3, 1.0, -1.0, 0.5, -0.1, 0.9):
    ideal = 1 - abs(3 * abs(w) - 2)
    dbl = (abs(abs(w) - 0.6666667) - 0.33333334) * -3.0
    wf = f32(w)
    t = f32(abs(wf))
    t = f32(t - a32)
    t = f32(abs(t))
    t = f32(t - b32)
    t = f32(t * f32(-3.0))
    print(f"{w:+.7f} | {ideal:+.9f} | {dbl:+.9f} | {t:+.9f}")

# --- float32 discriminators for probe WN4 (sign of the result decides solid when used as final_density)
print()
print("case | double | float32 per node | solid(double) | solid(float32)")
r = f32(f32(1000000.0) + f32(0.01)) - 1000000.0
print("A sub(add(1000000, 0.01), 1000000) |", (1000000.0 + 0.01) - 1000000.0, "|", r, "|", (1000000.0 + 0.01) - 1000000.0 > 0, "|", r > 0)
r = f32(f32(f32(0.1) * f32(3.0)) - f32(0.3))
d = 0.1 * 3.0 - 0.3
print("B sub(mul(0.1, 3), 0.3) |", d, "|", r, "|", d > 0, "|", r > 0)
# lerp(alpha, first, second) with first == second == v: exact answer is v; two evaluation orders
found = []
for k in range(1, 100):
    t = f32(k / 100.0)
    v = f32(0.1)
    f1 = f32(f32(v * f32(1.0 - t)) + f32(v * t))      # first*(1-alpha) + second*alpha
    f2 = f32(v + f32(t * f32(v - v)))                  # first + alpha*(second-first)
    if f1 != f2:
        found.append((float(t), f1 - v, f2 - v))
print("C lerp(alpha=k/100, 0.1, 0.1) - 0.1: orders differ for", len(found), "of 99 alphas; first three (alpha, form1 - v, form2 - v):", found[:3])
```

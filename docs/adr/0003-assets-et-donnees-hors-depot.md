# ADR 0003 : Assets et données de référence hors du dépôt

Statut : accepté

## Contexte

Textures, sons, modèles, langues et données de jeu appartiennent à Mojang. Le projet en a besoin à l'exécution et pour la vérification, mais ne doit pas les redistribuer. Le dépôt est public.

## Décision

- Aucun asset ni aucune donnée Mojang n'est commité. `.cache/`, `run/`, `reports/` et `docs/reference/*` sont ignorés, et `scripts/check-hygiene.sh` refuse les chemins `assets/minecraft/`, `data/minecraft/` et les formats `.jar`, `.mca`, `.ogg`, `.nbt` (exceptions listées et justifiées dans `scripts/hygiene-exceptions.txt`).
- `tools/bootstrap` télécharge et vérifie :
  - le `server.jar` officiel de la version cible, contrôlé par la taille et le SHA-1 du manifeste officiel, puis son SHA-256 enregistré dans `tools/bootstrap/src/main/resources/pins.json` ;
  - les assets (implémentés au jalon M9, le moteur sans graphique n'en a pas besoin), par deux fournisseurs interchangeables choisis par option : le dépôt GitHub `InventivetalentDev/minecraft-assets`, branche `26.3`, épinglé par commit et sommes de contrôle (choix du propriétaire, par défaut), ou le canal officiel (`client.jar` et index d'assets avec SHA-1 du fichier de version) ;
  - les données et rapports générés par le serveur de référence (`--all` / `--reports`).
- Les rapports de recherche personnels restent en local dans `docs/reference/` ; les agents en tirent des specs courtes et originales dans `docs/specs/`.
- Le nom "Minecraft" désigne la cible. Le nom du dépôt le contient (choix du propriétaire) : risque de marque accepté, à réexaminer avant toute promotion publique. Pas de logo ni d'asset de marque.

## Conséquences

- Un clone neuf doit exécuter `bootstrap` avant de lancer quoi que ce soit.
- Les épingles empêchent qu'une mise à jour du dépôt d'assets change silencieusement les résultats.
- Le miroir GitHub n'a pas de licence déclarée et sa couverture de version n'est pas garantie : le canal officiel est le repli supporté dès le départ, et peut devenir le défaut si le miroir pose problème.

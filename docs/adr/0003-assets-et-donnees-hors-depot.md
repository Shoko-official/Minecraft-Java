# ADR 0003 : Assets et données de référence hors du dépôt

Statut : accepté

## Contexte

Textures, sons, modèles, langues et données de jeu appartiennent à Mojang. Le projet en a besoin à l'exécution et pour la vérification, mais ne doit pas les redistribuer.

## Décision

- Aucun asset ni aucune donnée Mojang n'est commité. `.cache/`, `run/`, `reports/` et `docs/reference/*` sont ignorés.
- `tools/bootstrap` télécharge et vérifie :
  - le `server.jar` officiel de la version cible, contrôlé par SHA-1 puis SHA-256 ;
  - les assets depuis le dépôt GitHub `InventivetalentDev/minecraft-assets`, branche `26.3`, épinglés par commit et sommes de contrôle écrits dans `tools/bootstrap/pins.json` ;
  - les données et rapports générés par le serveur de référence (`--all` / `--reports`) ;
  - à défaut, les assets du lanceur installé (`.minecraft`) avec les mêmes contrôles.
- Les rapports de recherche personnels restent en local dans `docs/reference/` ; les agents en tirent des specs courtes et originales dans `docs/specs/`.
- Le nom "Minecraft" ne figure dans le dépôt que pour décrire la cible. Pas de logo ni de marque.

## Conséquences

- Un clone neuf doit exécuter `bootstrap` avant de lancer quoi que ce soit.
- Les épingles empêchent qu'une mise à jour du dépôt d'assets change silencieusement les résultats.
- Si le dépôt d'assets cesse de couvrir la version, l'extraction depuis l'installation locale prend le relais.

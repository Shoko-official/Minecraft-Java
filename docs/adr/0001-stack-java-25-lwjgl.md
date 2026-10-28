# ADR 0001 : Java 25, LWJGL, Gradle multi-modules

Statut : accepté

## Contexte

L'objectif est une fidélité exacte à Minecraft Java 26.3. Le jeu de référence exige Java 25. La fidélité dépend de détails de langage : arithmétique `float` / `double`, division entière, décalages, génération pseudo-aléatoire, ordre d'itération.

## Décision

- Langage : Java 25.
- Rendu, audio, fenêtre : LWJGL (choix OpenGL ou Vulkan reporté au jalon M9).
- Build : Gradle (Kotlin DSL), multi-modules, catalogue de versions `gradle/libs.versions.toml`, toolchain Java 25.
- Qualité : Spotless, Error Prone, ArchUnit, JUnit 6, AssertJ.

## Alternatives écartées

- Rust + wgpu : performances et sûreté mémoire, mais chaque sémantique Java doit être réimplémentée à la main, avec un risque d'écarts subtils.
- C++ : mêmes réserves, plus une dette de sûreté mémoire.

## Conséquences

- Les sémantiques numériques de référence sont celles du langage ; les écarts éventuels viennent de nos algorithmes, plus du langage.
- Un JDK 25 est requis en local et en CI (provisionné par la toolchain Gradle).

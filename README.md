# Gamilife 0.1.5

Application Android hors ligne qui gamifie les actions réelles : valider une compétence, obtenir de l'expérience, combattre, gagner un butin et équiper le personnage.

## Compilation

Prérequis : JDK 17, Android SDK Platform 35 et Build Tools 35, connexion aux dépôts Google/Maven Central lors du premier build.

```bash
./gradlew testDebugUnitTest assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk Gamilife-0.1.5.apk
```

L'APK de débogage est signé avec la clé de débogage standard Android et s'installe indépendamment.

## Fonctionnalités V0.1.5

- Refonte native complète de l’onglet Personnage : panneaux bleu nuit, liserés dorés, progression, statistiques et grille d’équipements cohérentes avec la navigation gaming.
- Les emplacements répondent au toucher et ouvrent la gestion d’équipement ; les valeurs visibles proviennent de la base persistante.

- Nouvelle fiche Personnage complète fondée sur la référence fournie, avec les valeurs persistantes superposées, l’accès aux équipements et une animation d’entrée discrète.
- Paramètres avec mode administrateur persistant. Une fois activé, il permet d’éditer et d’enregistrer le profil complet (niveau, rang, XP, PV, statistiques de combat et AME/RME).
- Navigation gaming native avec retours tactiles animés et accent doré de l’onglet actif.

- Navigation gaming native : tuiles bleu nuit, surbrillance dorée de l'onglet actif, rebond et enfoncement tactile.

- Barre de navigation RPG fidèle à la référence, fixe et utilisable sur toutes les vues : l'onglet actif reçoit son accent jaune doré.

- L'onglet Personnage utilise la fiche visuelle fournie : valeurs de profil et statistiques réelles superposées, zones Équipements et barre RPG directement cliquables.

- Navigation mobile vers Personnage, Compétences, Donjons, Bestiaire, Combat, Inventaire, Équipement, Effets, Collection, Forge et Administration.
- Room/SQLite persistante, initialisée une seule fois avec des données de démonstration.
- Cycle réel : validation de compétence → XP → combat au tour par tour → butin → équipement → statistiques recalculées.
- Moteurs métier indépendants : statistiques, dégâts min/max, critique, AME/RME, effets et progression.
- Horodatages persistants de réutilisation de compétences ; les CAR sont représentés par une échéance persistante.
- Tests unitaires déterministes du calcul et de la progression.

## Limites assumées de cette première itération

- Les illustrations sont volontairement des icônes originales afin de ne pas employer de ressources NosTale protégées.
- Forge et Collection constituent un socle de lecture ; les recettes et l'économie seront une version ultérieure.
- L'interface Administration teste les règles d'irrégularité et expose les données ; un éditeur formulaire exhaustif pour chaque entité reste à étendre.
- Le tour de riposte du monstre, le chaînage automatique de donjon et les migrations de schéma sont prévus par le modèle mais pas encore activés.

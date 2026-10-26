# Gamilife 0.1.0

Application Android hors ligne qui gamifie les actions réelles : valider une compétence, obtenir de l'expérience, combattre, gagner un butin et équiper le personnage.

## Compilation

Prérequis : JDK 17, Android SDK Platform 35 et Build Tools 35, connexion aux dépôts Google/Maven Central lors du premier build.

```bash
./gradlew testDebugUnitTest assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk Gamilife-0.1.0.apk
```

L'APK de débogage est signé avec la clé de débogage standard Android et s'installe indépendamment.

## Fonctionnalités V0.1.0

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

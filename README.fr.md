# PulseSMS 💬

Une application SMS rapide et privée pour Android, construite avec Kotlin.

## Fonctionnalités

- **Rapide** — expérience de messagerie légère et réactive
- **Privé** — vos messages restent sur votre appareil
- **Interface épurée** — design minimal centré sur l'essentiel
- **Support MMS** — envoyez et recevez des photos avec redimensionnement automatique
- **Appareil photo intégré** — prenez des photos directement depuis le composeur
- **Sélecteur de photos** — galerie native avec sélection multiple (tapez plusieurs photos, puis confirmez)
- **Visualiseur d'images MMS** — tapez une image MMS pour la voir en plein écran ; un bouton de téléchargement l'enregistre dans votre galerie et l'ouvre automatiquement
- **Messages vocaux** — enregistrez et envoyez des messages audio en pièce jointe MMS ; tapez pour lire les messages vocaux reçus
- **Conversations groupées** — messages regroupés par contact avec filtres (Tous, Personnel, Pro, OTP)
- **Sélection de SIM** — choisissez quelle carte SIM utiliser pour envoyer (double SIM)
- **Verrouillage biométrique** — protégez l'application avec empreinte ou mot de passe
- **Copie automatique des codes** — les codes OTP et de vérification sont détectés et copiés automatiquement
- **Synchronisation** — module optionnel pour l'historique des messages multi-appareils
- **Thèmes** — couleur dynamique (Material You), thèmes clair/sombre/noir, préférences linguistiques (anglais, français)

## Stack technique

- **Langage :** Kotlin
- **Plateforme :** Android (min SDK 26, target SDK 36)
- **Build :** Gradle (Kotlin DSL)
- **Architecture :** Multi-module (`app`, `core`, `feature`) avec MVVM
- **UI :** Jetpack Compose + Material 3
- **Chargement d'images :** Coil
- **Gestion d'état :** ViewModels, StateFlow, état Compose
- **Persistance :** Room (SQLite), ContentProvider SMS/MMS Android
- **DI :** Injection manuelle par constructeur
- **Tâches en arrière-plan :** WorkManager, coroutines
- **Tests :** JUnit 5, kotlinx.coroutines test

## Structure du projet

```
PulseSMS/
├── app/          # Module principal (UI, ViewModels, intégration SMS)
│   ├── ui/       # Écrans Compose (boîte de réception, conversation, paramètres, etc.)
│   ├── sms/      # Lecture et envoi SMS/MMS, résolution des pièces jointes MMS
│   └── contact/  # Recherche et affichage des contacts
├── core/         # Utilitaires partagés (système de design, base de données, sécurité)
│   ├── design/   # Tokens de design, thème, composants communs
│   ├── database/ # Base de données Room, DAO, migrations
│   └── security/ # Verrouillage biométrique, mot de passe, conformité
├── feature/      # Modules fonctionnels
│   ├── messaging/  # Dépôt de conversations, logique de synchronisation
│   └── sync/       # Synchronisation multi-appareils
└── docs/         # Documentation et skills
```

## Premiers pas

### Prérequis

- Android Studio (dernière version stable)
- JDK 11 ou supérieur
- Android SDK

### Compiler et exécuter

1. Clonez le dépôt :
   ```bash
   git clone https://github.com/Azyrn/PulseSMS.git
   cd PulseSMS
   ```

2. Ouvrez le projet dans Android Studio.

3. Synchronisez Gradle et exécutez sur un appareil ou un émulateur :
   ```bash
   ./gradlew assembleDebug
   ```

## Licence

Ce projet est open source. Voir [LICENSE](LICENSE) pour plus de détails.

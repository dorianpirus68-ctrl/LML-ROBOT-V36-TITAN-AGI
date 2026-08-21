# LML Control

LML Control est une application Android minimale qui réunit dans une seule interface un tableau de bord, un assistant d’information et des réglages de sécurité.

## Principes

L’application est conçue pour rester simple et contrôlée par son utilisateur. Elle ne demande pas d’autorisation d’accessibilité, ne crée pas de superposition d’écran, ne pilote pas d’application tierce et ne déclenche pas d’action sans interaction de l’utilisateur.

## Fonctions

| Fonction | Description |
|---|---|
| Tableau de bord | Affiche l’état de l’assistant et permet de l’activer ou le désactiver. |
| Assistant | Explique le fonctionnement et les limites de l’application. |
| Sécurité | Rappelle que les actions sont locales, visibles et manuelles. |

## Construire l’APK

Le workflow GitHub Actions construit un APK de débogage à chaque envoi vers `main` et lors d’un lancement manuel. L’artefact produit porte le nom `LML-Control-APK` et est conservé pendant 30 jours.

## Structure

```text
app/
  src/main/java/com/lml/control/MainActivity.java
  src/main/res/layout/activity_main.xml
  src/main/res/values/
.github/workflows/build.yml
```

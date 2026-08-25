# LML Action Assistant

**LML Action Assistant** est une application Android de conseil IA à **validation humaine**. Elle accepte un fournisseur compatible OpenAI — distant ou local — et propose deux modes : une analyse simple ou un collectif **multi-cerveaux**. Dans les deux cas, l’application produit du texte et des vérifications manuelles ; elle ne contrôle aucune autre application et n’exécute pas d’action au nom de l’utilisateur.

> Chaque synthèse doit être lue et décidée par l’utilisateur. L’application ne prétend jamais avoir accompli une action externe.

| Élément | Implémentation |
|---|---|
| Fournisseur distant | URL HTTPS compatible OpenAI, modèle et clé API renseignés dans l’application. |
| Modèle local | Serveur compatible OpenAI exposé sur le réseau local, par exemple Ollama via `/v1/chat/completions`. [1] |
| Secret | La clé API est chiffrée au repos avec Android Keystore, jamais ajoutée à l’APK, aux journaux ou aux sauvegardes Android. |
| Collectif multi-cerveaux | Planificateur, contradicteur et cerveau sécurité analysent séparément la demande ; une synthèse explicable est ensuite proposée. |
| Actions locales | Copier, partager via le sélecteur Android, confirmer la revue et effacer le résultat. Aucune de ces actions ne contrôle une autre application. |
| Distribution | Un workflow GitHub Actions fabrique un APK debug téléchargeable comme artefact. |

## Collectif multi-cerveaux

Le collectif fonctionne **à la demande** depuis l’écran de l’application. Les cerveaux sont des rôles d’analyse distincts, pas des processus autonomes : ils n’ont aucun accès aux gestes Android, aux surcouches, aux services Accessibilité ou au contenu d’autres applications.

| Rôle | Fonction | Résultat affiché |
|---|---|---|
| **Planificateur** | Formule un chemin court, réversible et vérifiable. | Objectif, prérequis et étapes manuelles. |
| **Contradicteur** | Recherche les hypothèses fragiles, risques et alternatives. | Contrôles et informations à vérifier. |
| **Sécurité** | Repère les données sensibles, conséquences et confirmations nécessaires. | Niveau de prudence et garde-fous. |
| **Synthèse** | Présente les accords, réserves et vérifications manuelles. | Recommandation à lire avant toute décision. |

La première version exécute les rôles séquentiellement avec le profil IA choisi. Cette approche réduit la complexité, respecte le budget du fournisseur choisi et évite toute exécution permanente en arrière-plan. Si le cerveau Sécurité signale un risque élevé, une vérification humaine renforcée est explicitement demandée.

## Configuration dans l’application

Ouvrez l’APK, complétez les paramètres du fournisseur, puis choisissez **Enregistrer la configuration**. Utilisez **Tester la connexion** avant d’envoyer une demande. La clé saisie est chiffrée sur le téléphone ; elle peut rester vide pour un serveur local ne demandant pas d’authentification.

| Cas d’usage | URL de base | Modèle | Clé API |
|---|---|---|---|
| Service OpenAI-compatible distant | `https://<fournisseur>/v1` | Nom fourni par le service | Requise selon le fournisseur |
| Ollama sur un ordinateur du même Wi-Fi | `http://<IP-LAN-ordinateur>:11434/v1` | Par exemple `llama3.2` | Facultative |
| Serveur interne compatible OpenAI | `https://<nom-ou-IP>/v1` | Nom du modèle du serveur | Selon la configuration serveur |

Pour Ollama, lancez le serveur sur l’ordinateur hôte, téléchargez au préalable le modèle souhaité, puis renseignez **l’adresse IP locale de l’ordinateur**, et non `localhost`, dans le téléphone. La documentation Ollama confirme la compatibilité de l’endpoint `/v1/chat/completions` et le format de requête utilisé par cette application. [1] [2]

L’application accepte une URL `http://` uniquement lorsque l’hôte est une adresse IPv4 privée RFC 1918 (`10.x.x.x`, `172.16.x.x` à `172.31.x.x` ou `192.168.x.x`). Les fournisseurs distants doivent impérativement être configurés en `https://` ; ce contrôle évite l’envoi accidentel d’une clé vers un hôte HTTP externe.

## Actions disponibles dans l’application

Après une analyse simple ou collective, le panneau **Actions locales** apparaît. **Copier** place uniquement la synthèse dans le presse-papiers. **Partager** ouvre le sélecteur standard Android : l’utilisateur choisit alors lui-même s’il souhaite partager et à quelle application. **Je confirme avoir revu la synthèse** enregistre uniquement cet état dans l’interface et n’exécute aucune opération externe. **Effacer** retire les résultats affichés de l’écran.

## Sécurité et limites

Cette version est volontairement conçue comme un **assistant de planification**. Elle ne permet pas de simuler une présence humaine, de contourner les règles d’un service, de supprimer des contacts en masse, de lire le contenu d’autres applications ou d’exécuter des interactions à l’insu de l’utilisateur. Le collectif ne change pas cette frontière : il améliore la qualité de l’analyse, pas la capacité d’agir automatiquement.

Lors d’un changement d’URL de fournisseur sans nouvelle clé, la clé enregistrée pour l’ancien fournisseur est supprimée afin qu’elle ne soit pas envoyée par erreur à un nouveau serveur. En cas de sauvegarde locale du même fournisseur, la clé conservée reste chiffrée et n’apparaît pas dans l’interface.

## Construire l’APK

Le projet contient un wrapper Gradle et un workflow GitHub Actions. Après un envoi sur la branche `main`, ouvrez l’exécution **Build LML Action Assistant APK**, puis téléchargez l’artefact `LML-Action-Assistant-debug-APK`. Il contient `app-debug.apk`, installable pour un test sur un téléphone Android compatible.

En local, installez Android SDK Platform 35 et Build Tools 35.0.0, puis exécutez :

```bash
./gradlew clean assembleDebug lintDebug
```

L’APK est produit ici :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Cette version est distribuée en **debug**. Une version de publication nécessite un identifiant de signature détenu et protégé par son propriétaire.

## Structure du projet

```text
app/
  src/main/java/com/lml/actionassistant/
    MainActivity.java             Interface, actions locales et revue humaine
    OpenAiCompatibleClient.java   Client HTTP Chat Completions
    SecureConfigStore.java        Chiffrement de la clé dans Android Keystore
    AgentRole.java                Rôles du collectif
    MultiBrainOrchestrator.java   Orchestration supervisée et synthèse
    MultiBrainResult.java         Résultats par rôle et synthèse
  src/main/res/                   Interface et ressources Android
.github/workflows/build.yml       Compilation et artefact APK
```

## Références

[1] [Documentation Ollama — compatibilité OpenAI](https://docs.ollama.com/api/openai-compatibility)

[2] [Ollama — annonce et exemples `chat/completions`](https://ollama.com/blog/openai-compatibility)

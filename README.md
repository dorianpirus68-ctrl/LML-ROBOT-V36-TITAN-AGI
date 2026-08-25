# LML Action Assistant

**LML Action Assistant** est une application Android de conseil IA à **validation humaine**. Elle permet de choisir un fournisseur compatible avec l’API *Chat Completions* d’OpenAI — service distant ou serveur local — puis d’obtenir une proposition d’étapes. L’application ne contrôle pas Snapchat ni aucune autre application, ne demande pas le service Accessibilité, et n’exécute pas des actions à la place de la personne utilisant le téléphone.

> Chaque étape suggérée doit être examinée puis réalisée manuellement par l’utilisateur. L’assistant ne prétend jamais avoir accompli une action.

| Élément | Implémentation |
|---|---|
| Fournisseur distant | URL de base compatible OpenAI, modèle et clé API renseignés dans l’application. |
| Modèle local | Serveur compatible OpenAI exposé sur le réseau local, par exemple Ollama via son endpoint `/v1/chat/completions`. [1] |
| Secret | La clé API est chiffrée au repos avec une clé Android Keystore, jamais ajoutée à l’APK, aux journaux ou aux sauvegardes Android. |
| Contrôle humain | L’application génère uniquement du texte ; elle n’utilise ni Accessibilité, ni superposition, ni geste automatisé. |
| Distribution | Un workflow GitHub Actions fabrique un APK **debug** téléchargeable comme artefact. |

## Configuration dans l’application

Ouvrez l’APK, complétez les paramètres du fournisseur, puis choisissez **Enregistrer la configuration**. Utilisez **Tester la connexion** avant d’envoyer une demande à analyser. La clé saisie est chiffrée sur le téléphone ; elle reste vide pour les serveurs locaux qui n’exigent pas d’authentification.

| Cas d’usage | URL de base | Modèle | Clé API |
|---|---|---|---|
| Service OpenAI-compatible distant | `https://<fournisseur>/v1` | Nom fourni par le service | Requise selon le fournisseur |
| Ollama sur un ordinateur du même Wi-Fi | `http://<IP-LAN-ordinateur>:11434/v1` | Par exemple `llama3.2` | Facultative |
| Serveur interne compatible OpenAI | `https://<nom-ou-IP>/v1` | Nom du modèle du serveur | Selon la configuration serveur |

Pour Ollama, lancez le serveur sur l’ordinateur hôte, téléchargez au préalable le modèle souhaité, puis renseignez **l’adresse IP locale de l’ordinateur**, et non `localhost`, dans le téléphone. La documentation Ollama confirme la compatibilité de l’endpoint `/v1/chat/completions` et le format de requête utilisé par cette application. [1] [2]

L’application accepte une URL `http://` uniquement lorsque l’hôte est une adresse IPv4 privée RFC 1918 (`10.x.x.x`, `172.16.x.x` à `172.31.x.x` ou `192.168.x.x`). Les fournisseurs distants doivent impérativement être configurés en `https://` ; ce contrôle évite l’envoi accidentel d’une clé vers un hôte HTTP externe.

## Sécurité et limites

Cette version est volontairement conçue comme un **assistant de planification**. Elle ne permet pas de simuler une présence humaine, de contourner les règles d’un service, de supprimer des contacts en masse, de lire le contenu d’autres applications ou d’exécuter des interactions à votre insu. Les paramètres d’instruction imposent également l’identification des étapes irréversibles et la demande de confirmation avant leur réalisation manuelle.

Lors d’un changement d’URL de fournisseur sans nouvelle clé, la clé enregistrée pour l’ancien fournisseur est supprimée afin qu’elle ne soit pas envoyée par erreur à un nouveau serveur. En cas de sauvegarde locale du même fournisseur, la clé conservée reste chiffrée et n’apparaît pas dans l’interface.

## Construire l’APK

Le projet contient un wrapper Gradle et un workflow GitHub Actions. Après un envoi sur la branche `main`, ouvrez l’exécution **Build LML Action Assistant APK**, puis téléchargez l’artefact `LML-Action-Assistant-debug-APK`. Il contient `app-debug.apk`, installable pour un test sur un téléphone Android compatible.

En local, installez Android SDK Platform 35 et Build Tools 35.0.0, puis exécutez :

```bash
./gradlew clean assembleDebug
```

L’APK sera disponible ici :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Cette version est distribuée en **debug**. Elle ne doit pas être utilisée comme version de production : une version de publication nécessite un identifiant de signature détenu et protégé par son propriétaire.

## Structure du projet

```text
app/
  src/main/java/com/lml/actionassistant/
    MainActivity.java             Interface et flux de validation humaine
    OpenAiCompatibleClient.java   Client HTTP Chat Completions
    SecureConfigStore.java        Chiffrement de la clé dans Android Keystore
  src/main/res/                   Interface et ressources Android
.github/workflows/build.yml       Compilation et artefact APK
```

## Références

[1] [Documentation Ollama — compatibilité OpenAI](https://docs.ollama.com/api/openai-compatibility)

[2] [Ollama — annonce et exemples `chat/completions`](https://ollama.com/blog/openai-compatibility)

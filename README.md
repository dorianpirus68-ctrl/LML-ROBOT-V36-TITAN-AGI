# LML Action Assistant

**LML Action Assistant** est une application Android de conversation IA à **validation humaine**. Elle fonctionne avec un fournisseur compatible OpenAI, distant ou local. L’utilisateur échange avec le modèle dans un chat, peut demander une analyse collective multi-cerveaux, puis déclenche lui-même des actions Android locales à partir de la réponse.

> L’application analyse et prépare des actions. Elle ne pilote jamais l’interface d’autres applications, ne simule pas de gestes humains et ne réalise aucun effet externe sans une validation visible de l’utilisateur.

| Capacité | Fonctionnement |
|---|---|
| **Chat IA** | Historique de conversation temporaire, conservé uniquement pendant la session ouverte. |
| **Collectif multi-cerveaux** | Planificateur, contradicteur et cerveau Sécurité analysent le dernier message, puis une synthèse est affichée. |
| **Modèle distant** | URL HTTPS compatible OpenAI, modèle et clé API configurés dans l’application. |
| **Modèle local** | Serveur compatible OpenAI sur le réseau local, par exemple Ollama via `/v1/chat/completions`. [1] |
| **Secrets** | Clé API chiffrée avec Android Keystore ; la clé n’est ni écrite dans l’APK ni sauvegardée dans les journaux de l’application. |
| **Actions locales** | Copier, partager avec le sélecteur Android, exporter Markdown et préparer un brouillon de calendrier. |

## Utiliser le chat

Configurez l’URL de base, le modèle et, si nécessaire, une clé API. Utilisez **Tester la connexion** une première fois. Écrivez ensuite votre demande dans le champ de conversation et appuyez sur **Envoyer au modèle**. La réponse est ajoutée à l’historique de la session. Le bouton **Analyser le dernier message avec le collectif** ajoute une lecture séparée du plan, des risques et des conséquences.

L’historique visible peut être effacé avec **Effacer la conversation locale**. Cette action retire les messages affichés et invalide toute autorisation d’export momentanément en attente.

## Actions proposées et validation humaine

Les actions se trouvent sous la dernière réponse. Elles sont toutes initiées par un bouton appuyé par l’utilisateur.

| Action | Ce que fait l’application | Contrôle conservé par l’utilisateur |
|---|---|---|
| **Copier la réponse** | Place le texte dans le presse-papiers Android. | Vous choisissez ce que vous en faites ensuite. |
| **Partager avec confirmation** | Ouvre le sélecteur de partage Android avec le texte. | Vous sélectionnez l’application destinataire et confirmez dans son interface. |
| **Préparer un export Markdown** | Affiche le contenu exact, puis ouvre le sélecteur de création de document Android. | Vous approuvez l’aperçu puis choisissez l’emplacement du fichier. L’autorisation expire après cinq minutes et ne sert qu’une fois. |
| **Préparer un brouillon de calendrier** | Affiche un aperçu, puis ouvre l’application Calendrier avec un événement éditable. | Vous vérifiez, modifiez et enregistrez — ou annulez — dans votre calendrier. |

L’export Markdown est la première action HITL concrète : l’application ne crée aucun document avant qu’un aperçu soit affiché et qu’une confirmation ait été donnée. Si le sélecteur de document est annulé, si l’autorisation expire ou si la conversation est effacée, aucun fichier n’est écrit.

## Configuration des fournisseurs

| Cas d’usage | URL de base | Modèle | Clé API |
|---|---|---|---|
| Service OpenAI-compatible distant | `https://<fournisseur>/v1` | Nom fourni par le service | Selon le fournisseur |
| Ollama sur ordinateur local | `http://<IP-LAN-ordinateur>:11434/v1` | Par exemple `llama3.2` | Souvent facultative |
| Serveur interne compatible OpenAI | `https://<nom-ou-IP>/v1` | Nom défini sur le serveur | Selon la configuration |

Pour un modèle Ollama, l’adresse entrée dans le téléphone doit être l’IP privée de l’ordinateur sur le Wi-Fi, et non `localhost`. L’API OpenAI-compatible d’Ollama utilise l’endpoint `/v1/chat/completions`. [1]

L’application autorise `http://` seulement pour les adresses IPv4 privées du réseau local. Les fournisseurs distants doivent utiliser `https://`, afin de réduire le risque d’envoi accidentel d’une clé API sur une connexion non chiffrée.

## Garde-fous

Cette application est un assistant de conversation et de préparation. Elle ne possède aucun service Accessibilité, aucune superposition, aucune simulation de geste, aucune permission de contrôle d’autres applications et aucun automatisme en arrière-plan. Les actions à conséquences importantes — publication, suppression, paiement, transaction, modification d’accès ou envoi irréversible — ne font pas partie de cette version.

Le chat peut proposer une action sous forme de texte, mais le modèle ne reçoit pas une capacité directe d’exécution. Le code Android ne lance que les quatre actions explicitement prévues dans l’interface et aucune instruction non reconnue ne peut devenir une action système.

## Construire l’APK

Après un envoi sur la branche `main`, le workflow GitHub Actions fabrique un artefact debug téléchargeable. En local, installez Android SDK Platform 35 et Build Tools 35.0.0, puis exécutez :

```bash
./gradlew clean assembleDebug lintDebug
```

L’APK se trouve ici :

```text
app/build/outputs/apk/debug/app-debug.apk
```

La version fournie est une version **debug**. Une publication de production nécessite un certificat de signature détenu et protégé par son propriétaire.

## Structure du projet

```text
app/
  src/main/java/com/lml/actionassistant/
    MainActivity.java             Chat, confirmations HITL et actions locales
    OpenAiCompatibleClient.java   Client Chat Completions compatible OpenAI
    SecureConfigStore.java        Chiffrement de la clé avec Android Keystore
    AgentRole.java                Rôles du collectif
    MultiBrainOrchestrator.java   Analyse collective supervisée
    MultiBrainResult.java         Résultats par rôle et synthèse
  src/main/res/                   Écran Android et ressources visuelles
.github/workflows/build.yml       Compilation et artefact APK
```

## Référence

[1] [Ollama — compatibilité OpenAI](https://docs.ollama.com/api/openai-compatibility)

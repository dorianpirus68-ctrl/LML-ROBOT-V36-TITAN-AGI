# LML Control

LML Control est un assistant Android **local et supervisé**. Il regroupe un tableau de bord, une bulle flottante facultative, une liste d’applications explicitement autorisées, des scénarios guidés, des rappels et un chatbot local d’orientation.

## Principes de sécurité

L’application privilégie la transparence et le contrôle humain. Elle ne transmet pas de données par réseau, ne demande pas de caméra ou de microphone, et n’automatise pas les messages, suppressions, achats, publications ou actions de compte.

La bulle flottante et le service de supervision sont désactivés par défaut. Android demande à l’utilisateur d’autoriser explicitement la bulle et d’activer le service dans ses réglages. Le service de supervision ne lit pas le contenu des fenêtres. Après une validation visible, il peut exécuter uniquement un retour ou un défilement unique dans une application autorisée.

## Fonctions

| Fonction | Description |
|---|---|
| Tableau de bord | Affiche le mode de supervision, les permissions et l’activité récente. |
| Bulle flottante | Contrôle visible et déplaçable, activé uniquement par l’utilisateur. |
| Applications autorisées | L’utilisateur sélectionne les applications pouvant être associées à des scénarios. |
| Scénarios locaux | Enregistre une application, un objectif et des étapes à suivre ou à valider. |
| Rappels d’objectifs | Affiche une notification de rappel ; aucune action inter-applications n’est exécutée automatiquement. |
| Chat local | Répond aux questions sur les réglages, applications autorisées, scénarios et limites. |
| Actions approuvées | Après votre confirmation, ouvre une application choisie ou exécute une navigation simple — retour ou défilement unique — dans cette application, puis inscrit le résultat dans un journal local. |
| Journal et résumé | Conserve une activité locale limitée et permet de copier un résumé. |

## Limites actuelles

Un scénario enregistré est un **guide supervisé**, pas une capacité de pilotage autonome des autres applications. Pour toute évolution vers une assistance inter-applications, l’utilisateur doit sélectionner les applications concernées, comprendre les permissions demandées et valider chaque action sensible.

## Construire l’APK

Le workflow GitHub Actions construit un APK de débogage à chaque envoi vers le dépôt et lors d’un lancement manuel. L’artefact produit porte le nom `LML-Control-APK` et est conservé pendant 30 jours.

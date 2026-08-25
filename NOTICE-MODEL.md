# Notice du modèle neuronal local

## Objet

LML Action Assistant embarque un **petit réseau neuronal local de classification d’intentions**. Son rôle est strictement limité à proposer une action déjà présente dans le catalogue de l’application. Il ne répond pas à la conversation, ne collecte pas de données d’usage, ne transmet aucun texte et ne peut ni créer ni exécuter une intention Android arbitraire.

| Élément | Description |
|---|---|
| Entrée | Dernier message de l’utilisateur, traité localement. |
| Prétraitement | Minuscules, décomposition Unicode, retrait des accents combinés, mots et fragments de mots de 3 à 4 caractères. |
| Réseau | Perceptron multicouche : 768 caractéristiques hachées, couche ReLU de 48 neurones, sortie softmax à 5 classes. |
| Classes | `NONE`, `WEB`, `MAPS`, `EMAIL`, `CALENDAR`. |
| Seuil de proposition | 62 %. En dessous du seuil ou pour `NONE`, aucune suggestion n’est affichée. |
| Exécution | Aucune. Une suggestion lance au plus le parcours HITL existant, avec aperçu et confirmation utilisateur. |

## Données et entraînement

Les poids embarqués dans `app/src/main/assets/lml_intent_model.json` ont été entraînés de façon reproductible par `tools/train_intent_model.py` sur le corpus public **MASSIVE 1.0**, sous-ensemble `fr-FR`. MASSIVE est un corpus multilingue d’énoncés d’assistant virtuel avec étiquettes d’intention ; sa documentation indique notamment 51 langues, 60 intentions et 18 domaines. [1] [2]

Seules des intentions compatibles avec le catalogue supervisé de LML ont été mappées : recherche d’information vers `WEB`, recherche de lieu ou transport vers `MAPS`, demande d’e-mail vers `EMAIL`, et demande de calendrier vers `CALENDAR`. Toutes les autres intentions, dont les demandes sociales, de publication, d’objets connectés, d’achat et de suppression, sont agrégées à `NONE` ou écartées de l’exécution. Le corpus est distribué sous licence **CC BY 4.0** ; cette notice conserve l’attribution et les liens source. [2]

| Mesure hors échantillon | Valeur de la version embarquée |
|---|---:|
| Exemples français utilisés pour l’entraînement pondéré | 11 514 |
| Exemples français de test | 2 974 |
| Seuil de proposition évalué | 62 % |
| Exactitude de test au seuil | 76,23 % |
| Exactitude équilibrée au seuil | 72,64 % |
| Précision / rappel Web au seuil | 75,00 % / 40,97 % |
| Précision / rappel Cartes au seuil | 84,62 % / 63,87 % |
| Précision / rappel e-mail au seuil | 76,69 % / 89,47 % |
| Précision / rappel calendrier au seuil | 77,10 % / 78,95 % |

## Limites

Ces mesures portent sur des intentions du corpus MASSIVE, pas sur tous les messages réels ni sur une validation de produit. Le classifieur peut se tromper, notamment pour les formulations très longues, ambiguës ou hors domaine. La confiance affichée est une sortie de softmax et **n’est pas une garantie**. L’utilisateur doit vérifier l’aperçu de chaque action et peut toujours annuler dans LML ou dans l’application Android destinataire.

Le modèle ne permet pas le contrôle de Snapchat, le contrôle des interfaces d’autres applications, la simulation de geste, l’Accessibilité, les surcouches ou les actions persistantes.

## Reproduire les poids

Après avoir téléchargé l’archive officielle MASSIVE 1.0 et extrait `1.0/data/fr-FR.jsonl`, exécutez :

```bash
python3 tools/train_intent_model.py \
  --input /chemin/vers/fr-FR.jsonl \
  --output app/src/main/assets/lml_intent_model.json
```

## Références

[1] [Amazon Science — Amazon releases 51-language dataset for language understanding](https://www.amazon.science/blog/amazon-releases-51-language-dataset-for-language-understanding)

[2] [Hugging Face — qanastek/MASSIVE](https://huggingface.co/datasets/qanastek/MASSIVE)

package com.bidiws.dto.appareiliot;

// Une ligne du CSV d'import qui a echoue — ligne = numero dans le fichier
// (1 = en-tete, les donnees commencent a 2, pour matcher ce que l'admin
// voit dans son tableur), identifiantMateriel = tel que lu sur la ligne
// (peut etre vide si la ligne etait incomplete), raison = message precis
// (pas juste "erreur"), repris de ResponseStatusException.getReason()
// quand la ligne echoue sur la meme validation que la creation unitaire.
public record AppareilIotImportLigneErreurDto(
        int ligne,
        String identifiantMateriel,
        String raison
) {}

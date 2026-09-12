package com.bidiws.dto.appareiliot;

import java.util.List;

// Resume detaille d'un import CSV — jamais un simple compteur : crees
// contient chaque cle API en clair (meme principe de prudence que la
// creation unitaire, visible une seule fois), echecs detaille chaque
// ligne en erreur avec sa raison precise plutot qu'un total global.
public record AppareilIotImportResultatDto(
        List<AppareilIotCreeResponseDto> crees,
        List<AppareilIotImportLigneErreurDto> echecs
) {}

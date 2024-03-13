/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.probativevalue;

import fr.xelians.esafe.archive.domain.atr.BinaryDataObjectReply;
import java.time.LocalDateTime;

public record AtrBinaryObject(LocalDateTime grantDate, BinaryDataObjectReply bdoReply) {}

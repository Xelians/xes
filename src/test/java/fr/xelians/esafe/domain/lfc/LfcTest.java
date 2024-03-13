/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.domain.lfc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.TestInit;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestInit.class)
class LfcTest {

  @Test
  void checkArchiveUnitId() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ArchiveUnit unitSrc;

    String tpl =
        """
                [
                  {"op":"replace","path":"/DocumentType","value":"Patched Document Type %s"},
                  {"op":"replace","path":"/Title","value":"Patched Title %s"}
                ]
                """;

    for (int i = 0; i < 100; i++) {
      JsonNode jsonPatch = mapper.readValue(tpl.formatted(i, i), JsonNode.class);
      unitSrc = DtoFactory.createSmallUnit("500");
      JsonNode jsonSrc = JsonService.toJson(unitSrc);
      JsonNode jsonPatched = JsonPatch.apply(jsonPatch, jsonSrc);
      JsonService.toArchiveUnit(jsonPatched);
      JsonDiff.asJson(jsonPatched, jsonSrc);
    }
  }
}

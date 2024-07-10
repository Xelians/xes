/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2.ontology;

import fr.xelians.esafe.archive.domain.ingest.BasicOntologyMap;
import fr.xelians.esafe.archive.domain.ingest.Mapping;
import fr.xelians.esafe.archive.domain.ingest.OntologyMap;
import fr.xelians.esafe.search.domain.field.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XamOntology {

  public static final List<String> EXT_KEYS =
      createKeys(
          List.of(
              "PhysicalType=keyword",
              "PhysicalStatus=keyword",
              "PhysicalAgency=keyword",
              "PhysicalIdentifier=keyword",
              "PhysicalReference=keyword",
              "PhysicalBarcode=keyword",
              "KeyName1=keyword",
              "KeyValue1=keyword",
              "KeyName2=keyword",
              "KeyValue2=keyword",
              "KeyName3=keyword",
              "KeyValue3=keyword",
              "KeyName4=keyword",
              "KeyValue4=keyword",
              "KeyName5=keyword",
              "KeyValue5=keyword",
              "KeyName6=keyword",
              "KeyValue6=keyword",
              "KeyName7=keyword",
              "KeyValue7=keyword",
              "KeyName8=keyword",
              "KeyValue8=keyword",
              "KeyName9=keyword",
              "KeyValue9=keyword",
              "KeyName10=keyword",
              "KeyValue10=keyword",
              "KeyName11=keyword",
              "KeyValue11=keyword",
              "KeyName12=keyword",
              "KeyValue12=keyword",
              "KeyName13=keyword",
              "KeyValue13=keyword",
              "KeyName14=keyword",
              "KeyValue14=keyword",
              "KeyName15=keyword",
              "KeyValue15=keyword",
              "KeyName16=keyword",
              "KeyValue16=keyword",
              "KeyName17=keyword",
              "KeyValue17=keyword",
              "KeyName18=keyword",
              "KeyValue18=keyword",
              "KeyName19=keyword",
              "KeyValue19=keyword",
              "KeyName20=keyword",
              "KeyValue20=keyword",
              "KeyName21=keyword",
              "KeyValue21=keyword",
              "KeyName22=keyword",
              "KeyValue22=keyword",
              "KeyName23=keyword",
              "KeyValue23=keyword",
              "KeyName24=keyword",
              "KeyValue24=keyword",
              "KeyName25=keyword",
              "KeyValue25=keyword",
              "TextName1=keyword",
              "TextValue1=text",
              "TextName2=keyword",
              "TextValue2=text",
              "TextName3=keyword",
              "TextValue3=text",
              "TextName4=keyword",
              "TextValue4=text",
              "TextName5=keyword",
              "TextValue5=text",
              "TextName6=keyword",
              "TextValue6=text",
              "TextName7=keyword",
              "TextValue7=text",
              "TextName8=keyword",
              "TextValue8=text",
              "TextName9=keyword",
              "TextValue9=text",
              "TextName10=keyword",
              "TextValue10=text",
              "DateName1=keyword",
              "DateValue1=date",
              "DateName2=keyword",
              "DateValue2=date",
              "DateName3=keyword",
              "DateValue3=date",
              "DateName4=keyword",
              "DateValue4=date",
              "DateName5=keyword",
              "DateValue5=date",
              "NumberName1=keyword",
              "NumberValue1=long",
              "NumberName2=keyword",
              "NumberValue2=long",
              "NumberName3=keyword",
              "NumberValue3=long",
              "NumberName4=keyword",
              "NumberValue4=long",
              "NumberName5=keyword",
              "NumberValue5=long"));

  public static final List<Mapping> MAPPINGS = OntologyUtils.createMappings(EXT_KEYS);

  public static final OntologyMap MAPPING = new BasicOntologyMap(MAPPINGS);

  private static List<String> createKeys(List<String> keys) {
    List<String> k = new ArrayList<>(Sedav2Ontology.EXT_KEYS);
    k.addAll(keys);
    return Collections.unmodifiableList(k);
  }

  private XamOntology() {}
}

package com.petpet.c3po.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceOutput;
import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.api.model.Property;
import com.petpet.c3po.api.model.helper.Filter;
import com.petpet.c3po.api.model.helper.FilterCondition;
import com.petpet.c3po.api.model.helper.MetadataRecord;
import com.petpet.c3po.api.model.helper.MetadataRecord.Status;
import com.petpet.c3po.api.model.helper.NumericStatistics;
import com.petpet.c3po.api.model.helper.PropertyType;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.dao.mongo.mapreduce.HistogramJob;
import com.petpet.c3po.dao.mongo.mapreduce.NumericAggregationJob;
import com.petpet.c3po.utils.DataHelper;

public class ProfileGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileGenerator.class);

  private static final Class<com.petpet.c3po.api.model.Element> ELEMENT_CLASS = com.petpet.c3po.api.model.Element.class;

  private static final String[] PROPERTIES = { "format", "format_version", "puid", "mimetype", "charset", "linebreak",
      "compressionscheme", "creating_os", "byteorder", "compression_scheme", "colorspace", "icc_profile_name",
      "icc_profile_version", "created", "creating.application.name" };

  private PersistenceLayer persistence;

  private final RepresentativeGenerator sampleSelector;

  public ProfileGenerator(final PersistenceLayer persistence, RepresentativeGenerator generator) {
    this.persistence = persistence;
    this.sampleSelector = generator;
  }

  public void write(final String xml) {
    try {
      final Document doc = DocumentHelper.parseText(xml);
      this.write(doc);

    } catch (final DocumentException e) {
      e.printStackTrace();
    }
  }

  public void write(final Document doc) {
    this.write(doc, "profiles/output.xml");
  }

  public void write(final Document doc, final String path) {
    try {
      final OutputFormat format = OutputFormat.createPrettyPrint();
      final File file = new File(path);

      LOG.info("Will create profile in {}", file.getAbsolutePath());

      if (file.getParentFile() != null && !file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }

      file.createNewFile();

      final XMLWriter writer = new XMLWriter(new FileWriter(path), format);
      writer.write(doc);
      writer.close();

    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  public Document generateProfile(Filter filter) {
    return this.generateProfile(filter, false);
  }

  public Document generateProfile(Filter filter, boolean includeelements) {
    // TODO check if subFilter is changed
    // if not then it does not have a collection filter...
    // and the collection name should be different...
    Filter subFilter = filter.subFilter("collection");
    final long count = this.persistence.count(ELEMENT_CLASS, subFilter);

    final Document document = DocumentHelper.createDocument();
    final String name = this.getCollectionNameFromFilter(filter);
    final Element root = this.createRootElement(document, name, count);
    final Element partition = this.createPartition(root, filter);
    this.genereateFilterElement(partition, filter);
    final Element properties = this.createPropertiesElement(partition);
    this.generateProperties(filter, properties);
    this.createSamples(filter, partition);
    this.createElements(filter, partition, includeelements);

    return document;
  }

  private String getCollectionNameFromFilter(Filter filter) {

    if (filter == null) {
      return "all-data";
    }

    String result = "";
    List<FilterCondition> conditions = filter.getConditions();
    for (FilterCondition fc : conditions) {
      if (fc.getField().equals("collection")) {
        result += fc.getValue().toString() + " ";
      }
    }

    return result;
  }

  private void genereateFilterElement(Element partition, Filter filter) {
    Element elmntFilter = partition.addElement("filter");
    // TODO get rid of id
    elmntFilter.addAttribute("id", UUID.randomUUID().toString());
    Element parameters = elmntFilter.addElement("parameters");

    for (FilterCondition fc : filter.getConditions()) {
      Element parameter = parameters.addElement("parameter");
      parameter.addElement("name").addText(fc.getField());
      parameter.addElement("value").addText(fc.getValue().toString());
    }

  }

  // TODO serialize the filter better, not with the temp id.
  private Element createPartition(Element root, Filter filter) {

    long count = this.persistence.count(ELEMENT_CLASS, filter);

    final Element partition = root.addElement("partition").addAttribute("count", count + "");
    return partition;
  }

  private void generateProperties(final Filter filter, final Element properties) {
    Iterator<Property> allprops = this.persistence.find(Property.class, null);
    // final BasicDBObject query = new BasicDBObject("_id", null);

    while (allprops.hasNext()) {
      Property p = allprops.next();

      Filter copy = new Filter(filter);

      if (!copy.contains(p.getId())) {
        copy.addFilterCondition(new FilterCondition(p.getId(), null));
      }

      long count = this.persistence.count(ELEMENT_CLASS, copy);

      if (count != 0) {
        this.createPropertyElement(filter, properties, p, count);
      }
    }
  }

  private void createPropertyElement(final Filter filter, final Element properties, final Property p, long count) {
    final Element prop = properties.addElement("property").addAttribute("id", p.getKey())
        .addAttribute("type", p.getType()).addAttribute("count", count + "");

    final PropertyType type = PropertyType.valueOf(p.getType());

    switch (type) {
      case STRING:
        this.processStringProperty(filter, prop, p);
        break;
      case BOOL:
        this.processBoolProperty(filter, prop, p);
        break;
      case INTEGER:
      case FLOAT:
        this.processNumericProperty(filter, prop, p);
        break;
      case DATE:
        this.processDateProperty(filter, prop, p);
        break;
    }
  }

  private Element createRootElement(final Document doc, final String collection, final long count) {
    final Element profile = doc.addElement("profile", "http://ifs.tuwien.ac.at/dp/c3po")
        .addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance").addAttribute("collection", collection)
        .addAttribute("date", new Date() + "").addAttribute("count", count + "");

    return profile;
  }

  private Element createPropertiesElement(final Element partition) {
    return partition.addElement("properties");
  }

  private void createSamples(final Filter filter, final Element partition) {
    final Element samples = partition.addElement("samples");
    samples.addAttribute("type", this.sampleSelector.getType());
    this.sampleSelector.setFilter(filter);
    final List<String> output = this.sampleSelector.execute(5);

    LOG.debug("Found {} representatives", output.size());
    for (String s : output) {
      LOG.debug("Processing sample {}", s);
      createSampleElement(samples, s);
    }
  }

  private void createSampleElement(final Element samples, final String uid) {
    Iterator<com.petpet.c3po.api.model.Element> iter = this.persistence.find(ELEMENT_CLASS, new Filter(
        new FilterCondition("uid", uid)));

    assert iter.hasNext();

    com.petpet.c3po.api.model.Element element = iter.next();

    Element sample = samples.addElement("sample").addAttribute("uid", uid);
    for (MetadataRecord mr : element.getMetadata()) {
      LOG.debug("Metadata record: {}", mr.getProperty().getKey());
      if (mr.getStatus().equals(Status.CONFLICT.toString())) {
        for (int i = 0; i < mr.getValues().size(); i++) {
          sample.addElement("record").addAttribute("name", mr.getProperty().getKey())
              .addAttribute("value", mr.getValues().get(i).toString()).addAttribute("tool", mr.getSources().get(i));
          // TODO read source out of db/cache...
        }

      } else {
        sample.addElement("record").addAttribute("name", mr.getProperty().getKey())
            .addAttribute("value", mr.getValue().toString()).addAttribute("tool", mr.getSources().get(0));
      }
    }
  }

  private void createElements(final Filter filter, final Element partition, boolean includeelements) {
    final Element elements = partition.addElement("elements");

    if (includeelements) {

      Iterator<com.petpet.c3po.api.model.Element> iter = this.persistence.find(ELEMENT_CLASS, filter);

      while (iter.hasNext()) {
        com.petpet.c3po.api.model.Element element = iter.next();
        elements.addElement("element").addAttribute("uid", element.getUid());
      }
    }
  }

  private void processStringProperty(final Filter filter, final Element prop, final Property p) {
    for (final String s : PROPERTIES) {
      if (p.getKey().equals(s)) {

        Map<String, Long> histogram = this.persistence.getValueHistogramFor(p, filter);

        //TODO sort these according to value?
        for (String key : histogram.keySet()) {
          Long val = histogram.get(key);
          prop.addElement("item").addAttribute("id", key).addAttribute("value", val + "");
        }

        break;
      }
    }
  }

  private void processBoolProperty(final Filter filter, final Element prop, final Property p) {
//    final BasicDBObject query = new BasicDBObject("_id", null);
//    final BasicDBObject ref = DataHelper.getFilterQuery(filter);
//    final String key = "metadata." + p.getId() + ".value";
//    long yes = 0;
//    long no = 0;

    // when it equals uknown remove the elment.
    // TODO when it equals conflicted.
//    if (ref.get(key) != null && ref.get(key).toString().equals("{ \"$exists\" : false}")) {
//      prop.getParent().remove(prop);
//      return;
//
//    } else 
      
//    if (ref.get(key) != null) {
//      if (ref.getBoolean(key)) {
//        yes = this.persistence.find(Constants.TBL_ELEMENTS, ref, query).count();
//      } else {
//        no = this.persistence.find(Constants.TBL_ELEMENTS, ref, query).count();
//      }
//    } else {
//      ref.put(key, true);
//      yes = this.persistence.find(Constants.TBL_ELEMENTS, ref, query).count();
//
//      ref.put(key, false);
//      no = this.persistence.find(Constants.TBL_ELEMENTS, ref, query).count();
//
//    }
    
//    Filter copy1 = new Filter(filter);
//    Filter copy2 = new Filter(filter);
//    
//    //TODO check if copy has filter for this boolean prop and deal with it.
//    copy1.addFilterCondition(new FilterCondition(p.getId(), true));
//    copy2.addFilterCondition(new FilterCondition(p.getId(), false));
//    yes = this.persistence.count(ELEMENT_CLASS, copy1);
//    no = this.persistence.count(ELEMENT_CLASS, copy2);
    Map<String, Long> histogram = this.persistence.getValueHistogramFor(p, filter);
    for (String key : histogram.keySet()) {
      Long val = histogram.get(key);
      prop.addElement("item").addAttribute("value", key).addAttribute("count", val + "");
    }
    
//    prop.addElement("item").addAttribute("value", "true").addAttribute("count", yes + "");
//    prop.addElement("item").addAttribute("value", "false").addAttribute("count", no + "");

  }

  // if also a histogram is done, do not forget the bin_width...
  private void processNumericProperty(final Filter filter, final Element prop, final Property p) {
    NumericStatistics numericStatistics = this.persistence.getNumericStatistics(p, filter);


    prop.addAttribute("count", numericStatistics.getCount() + "");
    prop.addAttribute("sum", numericStatistics.getSum() + "");
    prop.addAttribute("min", numericStatistics.getMin() + "");
    prop.addAttribute("max", numericStatistics.getMax() + "");
    prop.addAttribute("avg", numericStatistics.getAverage() + "");
    prop.addAttribute("var", numericStatistics.getVariance() +"");
    prop.addAttribute("sd", numericStatistics.getStandardDeviation() + "");
  }

  private void processDateProperty(Filter filter, Element prop, Property p) {
    Map<String, Long> histogram = this.persistence.getValueHistogramFor(p, filter);

    for (String key : histogram.keySet()) {
      Long val = histogram.get(key);
      prop.addElement("item").addAttribute("id", key).addAttribute("value", val + "");
    }
  }

}

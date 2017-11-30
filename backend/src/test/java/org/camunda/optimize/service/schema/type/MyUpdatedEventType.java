package org.camunda.optimize.service.schema.type;

import org.camunda.optimize.service.es.schema.TypeMappingCreator;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class MyUpdatedEventType implements TypeMappingCreator{

  private Logger logger = LoggerFactory.getLogger(MyUpdatedEventType.class);

  private ConfigurationService configurationService;

  public MyUpdatedEventType(ConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  public static final String MY_NEW_FIELD = "myAwesomeNewField";

  @Override
  public String getType() {
    return configurationService.getEventType();
  }

  @Override
  public XContentBuilder getSource() {
    XContentBuilder source = null;
    try {
      XContentBuilder content = jsonBuilder()
        .startObject()
          .startObject("properties")
            .startObject(MY_NEW_FIELD)
              .field("type", "keyword")
            .endObject()
          .endObject()
        .endObject();
      source = content;
    } catch (IOException e) {
      String message = "Could not add mapping to the index '" + configurationService.getOptimizeIndex(getType()) +
        "' , type '" + getType() + "'!";
      logger.error(message, e);
    }
    return source;
  }
}

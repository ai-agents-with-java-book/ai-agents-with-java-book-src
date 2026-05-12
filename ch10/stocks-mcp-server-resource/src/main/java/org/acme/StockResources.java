package org.acme;


import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceTemplate;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

public class StockResources {

  private static Map<String, Double> stocks = new HashMap<>();

  static {
    stocks.put("IBM", 262.0);
  }

  @Inject
  Logger logger;

  @Inject
  ResourceManager resourceManager;

  @Resource(
      name = "stocks",
      description = "Get IBM stock value",
      uri = "http://stocks/IBM")
  public TextResourceContents getStock() {
    return TextResourceContents.create(
        "http://stocks/IBM",
        stocks.getOrDefault("IBM", -1d)
            .toString()
    );
  }

  @Scheduled(every = "15s")
  public void updateStockValue() {
    logger.info("Updating Stocks");

    Double ibm = stocks.get("IBM");
    stocks.put("IBM", ibm + 10);
    resourceManager.getResource("http://stocks/IBM").sendUpdateAndForget();
  }
}

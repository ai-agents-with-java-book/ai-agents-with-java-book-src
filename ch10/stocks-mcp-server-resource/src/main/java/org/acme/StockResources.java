package org.acme;


import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceTemplate;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;

import java.util.Map;

public class StockResources {

  private Map<String, String> stocks = Map.of("IBM", "262.0");

  @Inject
  ResourceManager resourceManager;

  @ResourceTemplate(
      name = "stocks",
      description = "Get stock value by ticker",
      uriTemplate = "http://stocks/{ticker}")
  public TextResourceContents getStocks(String ticker) {
    return TextResourceContents.create(
        "http://stocks/" + ticker,
        stocks.getOrDefault(ticker, "-1")
    );
  }

  public void updateStockValue(String ticker, String value) {
    stocks.put(ticker, value);
    resourceManager.getResource("http://stocks/" + ticker).sendUpdateAndForget();
  }
}

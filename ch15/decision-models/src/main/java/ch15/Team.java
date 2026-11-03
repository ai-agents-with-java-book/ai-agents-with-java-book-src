package ch15;

import dev.langchain4j.model.output.structured.Description;

public enum Team {
    @Description("Payments, payouts, invoices, refunds and charges") BILLING,
    @Description("Errors, outages, bugs and problems using the product") TECHNICAL,
    @Description("Login, passwords, permissions and user management") ACCOUNT,
    @Description("Pricing, plans, upgrades and new contracts") SALES
}

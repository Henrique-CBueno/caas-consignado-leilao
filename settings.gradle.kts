rootProject.name = "caas-consignado-leilao"

include("services:tenant-service")
include("services:api-gateway")
include("services:proposal-service")
include("services:credit-analysis-service")
include("services:auction-service")
include("services:notification-gateway-service")
include("services:funder-bot-service")
include("services:contract-service")
include("services:disbursement-service")
include("libs:event-schemas")
include("libs:observability")
include("libs:openapi-testing")

# Mesmo schema que DynamoDbSchemaInitializer (auction-service) já cria em
# runtime, de forma idempotente — Terraform provisiona a infra "de verdade"
# (o que existiria numa conta AWS real); o app continua se auto-provisionando
# de forma defensiva independente disso (mesma redundância consciente do
# ADR-0002).
resource "aws_dynamodb_table" "auctions" {
  name         = "auctions"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "proposal_id"

  attribute {
    name = "proposal_id"
    type = "S"
  }
}

resource "aws_dynamodb_table" "outbox_events" {
  name         = "outbox_events"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }
}

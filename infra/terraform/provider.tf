# Provider AWS apontando para o LocalStack — ver ADR-0002: LocalStack Community
# só emula DynamoDB e S3 de verdade (RDS/Cognito retornam 501). Este módulo só
# provisiona o que o LocalStack de fato suporta; nada de aws_db_instance/Cognito
# aqui, o apply falharia.
terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

variable "localstack_endpoint" {
  description = "Endpoint do LocalStack"
  type        = string
  default     = "http://localhost:4566"
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    dynamodb = var.localstack_endpoint
  }
}

terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "planmate-terraform-state"
    key            = "infrastructure/terraform.tfstate"
    region         = "eu-central-1"
    dynamodb_table = "planmate-terraform-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
}

module "eks" {
  source = "./modules/eks"

  cluster_name    = "planmate-${var.environment}"
  cluster_version = "1.28"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnets
}

module "rds" {
  source = "./modules/rds"

  identifier          = "planmate-${var.environment}"
  engine_version      = "16.1"
  instance_class      = var.rds_instance_class
  allocated_storage   = 100
  multi_az            = var.environment == "prod" ? true : false
  vpc_id              = module.vpc.vpc_id
  subnet_ids          = module.vpc.database_subnets
  backup_retention    = var.environment == "prod" ? 30 : 7
}

module "s3" {
  source = "./modules/s3"

  bucket_name = "planmate-artifacts-${var.environment}"
  environment = var.environment
}

module "ecr" {
  source = "./modules/ecr"

  repository_name = "planmate-api"
}

module "vpc" {
  source = "terraform-aws-modules/vpc/aws"

  name = "planmate-${var.environment}"
  cidr = "10.0.0.0/16"

  azs              = ["eu-central-1a", "eu-central-1b", "eu-central-1c"]
  private_subnets  = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets   = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
  database_subnets = ["10.0.201.0/24", "10.0.202.0/24", "10.0.203.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = var.environment != "prod"
  enable_dns_hostnames = true
  enable_dns_support   = true
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "20.8.5"

  cluster_name    = var.project_name
  cluster_version = var.cluster_version

  cluster_endpoint_public_access = true
  enable_cluster_creator_admin_permissions = true

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    app = {
      instance_types = ["t3.medium"]
      min_size       = 2
      max_size       = 4
      desired_size   = 2
      ami_type       = "AL2_x86_64"
      capacity_type  = "ON_DEMAND"
    }
  }

  tags = {
    Project = var.project_name
    Env     = "dev"
  }
}

# CloudCart on AWS EKS

A learning project that demonstrates deploying a small e-commerce application built with Spring Boot services and a static frontend to Amazon Elastic Kubernetes Service (EKS). The infrastructure is defined with Terraform, container images are stored in Amazon Elastic Container Registry (ECR), and cluster monitoring is provided by Prometheus and Grafana.

> **Important:** This repository is intended for learning and experimentation, not production use. Review the configuration before deploying it to your AWS account. EKS, EC2 instances, NAT Gateways, load balancers, and other AWS resources can incur charges.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square&logo=springboot)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue?style=flat-square&logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Amazon%20EKS-326CE5?style=flat-square&logo=kubernetes)
![Terraform](https://img.shields.io/badge/Terraform-IaC-7B42BC?style=flat-square&logo=terraform)
![AWS](https://img.shields.io/badge/AWS-EKS%20%7C%20ECR%20%7C%20VPC-FF9900?style=flat-square&logo=amazonaws)
![Prometheus](https://img.shields.io/badge/Prometheus-Monitoring-E6522C?style=flat-square&logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-Dashboards-F46800?style=flat-square&logo=grafana)

## Overview

CloudCart is a sample e-commerce application made up of five Spring Boot backend services and a static frontend served by NGINX. Kubernetes Services provide internal service discovery, while the NGINX Ingress Controller routes incoming HTTP requests to the frontend and selected backend APIs.

The repository also includes Terraform configuration for the AWS VPC and EKS cluster, Kubernetes manifests for the application, and instructions for adding Prometheus and Grafana monitoring.

## Architecture

```text
                         Browser
                            |
                    AWS Load Balancer
                            |
                   NGINX Ingress Controller
                            |
             +--------------+----------------+
             |              |                |
           /            /api/catalog       /api/cart
             |              |                |
         Frontend         Catalog            Cart
        (NGINX)        (Spring Boot)     (Spring Boot)
                            |
                      /api/order
                            |
                      Order Service
                      (Spring Boot)
                       /         \
                      /           \
             Payment Mock      Notification
             (Spring Boot)     (Spring Boot)

                 Amazon EKS Cluster

          Prometheus <---- metrics ----> Grafana
                 (monitoring namespace)
```

The diagram is a conceptual overview. The exact runtime interactions depend on the implementation of each service.

## Services

| Component | Technology | Purpose |
|---|---|---|
| `frontend` | HTML, NGINX | Serves the product listing UI and calls the catalog API |
| `catalog` | Java, Spring Boot | Returns product information |
| `cart` | Java, Spring Boot | Adds, lists, and clears cart items |
| `order` | Java, Spring Boot | Handles order creation and calls the payment and notification services |
| `payment-mock` | Java, Spring Boot | Simulates payment outcomes; it is not a real payment processor |
| `notification` | Java, Spring Boot | Simulates notifications after an order is placed |

The backend services listen on port `8080`; the frontend container serves HTTP on port `80`.

## Technology stack

| Area | Technology |
|---|---|
| Language and framework | Java 17, Spring Boot 3.2.5 |
| Build | Maven |
| Containers | Docker |
| Container registry | Amazon ECR |
| Orchestration | Kubernetes on Amazon EKS |
| Infrastructure as code | Terraform |
| Networking | Amazon VPC, subnets, NAT Gateway, AWS load balancer, NGINX Ingress |
| Monitoring | Prometheus and Grafana via `kube-prometheus-stack` |
| Package management | Helm |

## Repository layout

```text
cloudcart-on-eks/
├── services/
│   ├── frontend/
│   ├── catalog/
│   ├── cart/
│   ├── order/
│   ├── payment-mock/
│   └── notification/
├── k8s/
│   ├── namespace.yaml
│   ├── apps.yaml
│   └── ingress.yaml
└── infra/
    └── terraform/
        ├── vpc.tf
        ├── eks.tf
        ├── provider.tf
        ├── variables.tf
        ├── outputs.tf
        └── version.tf
```

## Prerequisites

Install the following tools:

- [Docker](https://docs.docker.com/get-docker/)
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
- [Terraform](https://developer.hashicorp.com/terraform/install)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/)

Configure AWS credentials with an IAM identity that has the permissions needed to create and manage the resources in this project. Avoid hard-coding access keys in files or committing credentials to Git.

## Before deploying

Review these repository-specific settings first:

1. **EKS Kubernetes version:** `infra/terraform/variables.tf` currently defaults to Kubernetes `1.29`. This version is outdated. Change `cluster_version` to a version currently supported by Amazon EKS and compatible with the Terraform module and node AMI configuration, then validate the plan before applying it.
2. **AWS region:** Terraform currently defaults to `us-east-1`, while the image references in `k8s/apps.yaml` currently point to ECR in `ap-south-1`. Choose one region and use it consistently for the EKS cluster, ECR repositories, and image references.
3. **ECR image URIs:** Replace the existing image URIs in `k8s/apps.yaml` with the URIs for repositories in your own AWS account. The YAML file does not automatically substitute shell variables.
4. **Costs:** Check current AWS pricing for EKS, EC2, NAT Gateway, load balancers, ECR storage, and data transfer. Do not leave resources running when you are finished.

You can override Terraform defaults with a local `terraform.tfvars` file. Do not put credentials or other secrets in that file. If you create it, check that it is ignored by Git before committing.

## Deployment guide

### 1. Build the container images

Run these commands from the repository root:

```bash
docker build -t catalog:latest ./services/catalog
docker build -t cart:latest ./services/cart
docker build -t order:latest ./services/order
docker build -t payment-mock:latest ./services/payment-mock
docker build -t notification:latest ./services/notification
docker build -t frontend:latest ./services/frontend
```

### 2. Create ECR repositories and push the images

Set the account ID and region for your AWS environment. Make sure the region matches the region you will use for EKS.

```bash
export ACCOUNT="<your-aws-account-id>"
export REGION="ap-south-1"

for service in catalog cart order payment-mock notification frontend; do
  aws ecr describe-repositories \
    --repository-name "$service" \
    --region "$REGION" >/dev/null 2>&1 || \
  aws ecr create-repository \
    --repository-name "$service" \
    --region "$REGION"

  docker tag "$service:latest" \
    "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/$service:latest"
done

aws ecr get-login-password --region "$REGION" |
  docker login --username AWS --password-stdin \
    "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"

for service in catalog cart order payment-mock notification frontend; do
  docker push "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/$service:latest"
done
```

Before applying the Kubernetes manifests, update every `image:` value in `k8s/apps.yaml` to match the account ID, region, repository name, and tag you used above.

### 3. Configure and provision AWS infrastructure

Update the Terraform variables so that `region` matches your ECR region and `cluster_version` uses a currently supported EKS version. Check that the configured node AMI type is supported for that Kubernetes version.

```bash
cd infra/terraform
terraform init
terraform fmt -check
terraform validate
terraform plan
```

Review the plan carefully, including resource counts, regions, and estimated cost. If it is correct, apply it:

```bash
terraform apply
```

Terraform provisions the VPC and EKS infrastructure described in the configuration. It does not manage the ECR repositories created in the previous step.

### 4. Configure kubectl

Use the cluster name configured in Terraform. With the default project name, run:

```bash
aws eks update-kubeconfig --region "$REGION" --name cloudcart-eks
kubectl get nodes
```

Confirm the nodes are ready before deploying the application.

### 5. Install the NGINX Ingress Controller

Install the controller using the official [ingress-nginx installation guide](https://kubernetes.github.io/ingress-nginx/deploy/). Choose the instructions appropriate for your AWS environment and pin a tested release rather than relying on a moving branch.

Wait for the controller to become ready before continuing. Its cloud load-balancer Service may provision an AWS load balancer and incur charges.

### 6. Deploy the application

From the repository root:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/apps.yaml
kubectl apply -f k8s/ingress.yaml

kubectl get pods -n cloudcart
kubectl get services -n cloudcart
kubectl get ingress -n cloudcart
```

Wait for the deployments and pods to become ready. If a pod cannot start, inspect it with `kubectl describe pod` and check its logs with `kubectl logs`. An `ImagePullBackOff` error commonly indicates an incorrect image URI, missing image, or ECR access issue.

When the ingress has an address, open that address in a browser. The frontend and API routes should be reachable if the ingress controller, manifests, images, and application routes are configured correctly.

## Monitoring with Prometheus and Grafana

Install the Prometheus community chart:

```bash
kubectl create namespace monitoring

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm upgrade --install monitoring \
  prometheus-community/kube-prometheus-stack \
  --namespace monitoring
```

Wait for the monitoring pods to become ready. Forward the Grafana service to your local machine:

```bash
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
```

In another terminal, retrieve the chart's generated Grafana admin password:

```bash
kubectl get secret -n monitoring monitoring-grafana \
  -o jsonpath='{.data.admin-password}' | base64 --decode
echo
```

Open [http://localhost:3000](http://localhost:3000) and sign in with username `admin` and the retrieved password. Keep the password private and do not commit it to the repository.

Example Kubernetes metrics to explore in Grafana include:

- `node_cpu_seconds_total`
- `node_memory_MemAvailable_bytes`
- `kube_pod_info`
- `kube_deployment_status_replicas_available`

Metric availability depends on the installed chart and scrape configuration.

## API routes

These are the routes configured in the Ingress manifest:

| Method | Path | Destination |
|---|---|---|
| GET | `/` | Frontend |
| API routes | `/api/catalog` | Catalog service |
| API routes | `/api/cart` | Cart service |
| API routes | `/api/order` | Order service |

The application code defines the exact methods and endpoints supported beneath each API path. Check the corresponding controller classes for the full API contract.

## Cleanup

**Do not leave cloud resources running after experimenting.** The EKS cluster, EC2 nodes, NAT Gateway, and load balancer can continue to incur charges.

First remove the Kubernetes resources created for this project. Run from the repository root while the cluster is still reachable:

```bash
kubectl delete namespace cloudcart --ignore-not-found
kubectl delete namespace monitoring --ignore-not-found
```

If you installed the ingress controller in the `ingress-nginx` namespace, remove it too:

```bash
kubectl delete namespace ingress-nginx --ignore-not-found
```

Wait for any AWS load balancers created by Kubernetes Services to be deleted. Then destroy the Terraform-managed infrastructure:

```bash
cd infra/terraform
terraform plan -destroy
```

Review the destroy plan carefully. If it targets only the resources you intend to remove, run:

```bash
terraform destroy
```

The ECR repositories created manually are not managed by this Terraform configuration. Delete them separately only if you no longer need the images:

```bash
for service in catalog cart order payment-mock notification frontend; do
  aws ecr delete-repository \
    --repository-name "$service" \
    --region "$REGION" \
    --force
done
```

Finally, check the AWS console for remaining billable resources. Do not use broad local cleanup commands such as `docker system prune -a --volumes` unless you intend to remove unused Docker images, containers, networks, and volumes beyond this project.

## What this project demonstrates

- Packaging Java Spring Boot services and a static frontend as Docker images.
- Deploying application workloads and Kubernetes Services to Amazon EKS.
- Routing HTTP traffic with an NGINX Ingress Controller.
- Provisioning VPC and EKS infrastructure with Terraform.
- Publishing and pulling container images from Amazon ECR.
- Installing Kubernetes monitoring with Prometheus and Grafana.
- Troubleshooting common deployment issues such as image pull failures and pod readiness problems.

## Author

[ka-png on GitHub](https://github.com/ka-png)

## License

No license is currently specified. Unless a license file is added, others should not assume they have permission to reuse, modify, or redistribute this project's code.

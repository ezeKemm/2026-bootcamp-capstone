# One AWS instance per entry in var.instances, each running single-node k3s (server and agent together), reached by its own Elastic IP.
# Decision: docs/architecture/adrs/0003-k3s.md. No Terraform state, keys or tfvars are committed (see .gitignore).

# Latest Canonical Ubuntu 24.04 image for the selected region (an AMI id is region-specific, so it is looked up, never hard-coded).
data "aws_ssm_parameter" "ubuntu" {
  name = "/aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_key_pair" "this" {
  key_name   = "${var.name}-k3s"
  public_key = var.ssh_public_key
}

# Allocated first so its address can be baked into the k3s API certificate (--tls-san) at first boot.
resource "aws_eip" "this" {
  for_each = var.instances
  domain   = "vpc"
  tags     = { Name = "${var.name}-${each.key}" }
}

# One security group shared by all instances.
resource "aws_security_group" "this" {
  name        = "${var.name}-k3s"
  description = "Northstar CRM k3s: web, Kubernetes API, optional SSH"
  vpc_id      = data.aws_vpc.default.id
  tags        = { Name = "${var.name}-k3s" }
}

resource "aws_vpc_security_group_ingress_rule" "http" {
  for_each          = toset(var.web_allowed_cidrs)
  security_group_id = aws_security_group.this.id
  description       = "Application over HTTP"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = each.value
}

# Open now so TLS can be added later without touching the firewall. Nothing listens on 443 until Traefik gets a certificate.
resource "aws_vpc_security_group_ingress_rule" "https" {
  for_each          = toset(var.web_allowed_cidrs)
  security_group_id = aws_security_group.this.id
  description       = "Application over HTTPS (later)"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_ingress_rule" "kube_api" {
  for_each          = toset(var.api_allowed_cidrs)
  security_group_id = aws_security_group.this.id
  description       = "Kubernetes API for CD"
  ip_protocol       = "tcp"
  from_port         = 6443
  to_port           = 6443
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each          = toset(var.ssh_allowed_cidrs)
  security_group_id = aws_security_group.this.id
  description       = "SSH for the team"
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.this.id
  description       = "Outbound (image pulls, packages)"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_instance" "this" {
  for_each                    = var.instances
  ami                         = data.aws_ssm_parameter.ubuntu.value
  instance_type               = each.value.instance_type
  subnet_id                   = sort(data.aws_subnets.default.ids)[0]
  vpc_security_group_ids      = [aws_security_group.this.id]
  key_name                    = aws_key_pair.this.key_name
  associate_public_ip_address = true

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required" # IMDSv2 only
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_gb
    encrypted   = true
  }

  user_data = templatefile("${path.module}/cloud-init.yaml.tftpl", {
    k3s_version = var.k3s_version
    public_ip   = aws_eip.this[each.key].public_ip
    namespaces  = each.value.namespaces
  })
  user_data_replace_on_change = false # changing the script must not silently destroy the cluster; replace deliberately

  tags = { Name = "${var.name}-${each.key}" }

  lifecycle {
    ignore_changes = [ami] # a newer Ubuntu image must not replace a running cluster
  }
}

resource "aws_eip_association" "this" {
  for_each      = var.instances
  instance_id   = aws_instance.this[each.key].id
  allocation_id = aws_eip.this[each.key].id
}

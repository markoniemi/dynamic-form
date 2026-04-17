# Application Load Balancer
resource "aws_lb" "main" {
  name_prefix        = "df"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [aws_subnet.public_1.id, aws_subnet.public_2.id]

  enable_deletion_protection = false

  tags = {
    Name        = "${var.environment}-alb"
    Environment = var.environment
  }
}

# Target Group for ECS
resource "aws_lb_target_group" "app" {
  name_prefix = "app"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 3
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200"
  }

  tags = {
    Name        = "${var.environment}-tg"
    Environment = var.environment
  }
}

# ALB Listener
resource "aws_lb_listener" "app" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

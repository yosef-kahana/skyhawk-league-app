aws rds create-db-instance \
  --db-instance-identifier league-db-instance \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username yourusername \
  --master-user-password yourpassword \
  --allocated-storage 20 \
  --publicly-accessible \
  --vpc-security-group-ids sg-xxxxxxxx

aws cloudformation deploy \
  --template-file ecs-league-app.yaml \
  --stack-name league-app-ecs \
  --capabilities CAPABILITY_IAM
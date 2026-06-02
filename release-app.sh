# Pipe the JSON string into jq
echo "$SECRETS_CONTEXT" |
# Convert JSON object into an array of key-value pairs
jq -r 'to_entries |
# Map over each key-value pair
.[] |
# Format each pair as "KEY=VALUE" and append it all to the environment file
"\(.key)=\(.value)"' >> .env

env $(cat .env | xargs) chmod +x gradlew && gradlew clean build
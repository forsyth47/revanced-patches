#!/bin/bash
set -e

# Get the latest tag (used as release version)
release_tag=$(git describe --tags --abbrev=0)

# Check if the release already exists, create it if not
response=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}/releases/tags/$release_tag")

if echo "$response" | grep -q "not found"; then
  response=$(curl -s -X POST -H "Authorization: token $GITHUB_TOKEN" \
    -d '{"tag_name": "'"$release_tag"'","name": "'"$release_tag"'","body": "Release description"}' \
    "https://api.github.com/repos/${GITHUB_REPOSITORY}/releases")
fi

# Extract the upload URL
upload_url=$(echo $response | jq -r .upload_url | sed -e "s/{?name,label}//")

# Find the desired file
selected_file=""
for file in patches/build/libs/*; do
  if [[ -f "$file" && ! "$file" =~ "sources" && ! "$file" =~ "javadoc" ]]; then
    selected_file="$file"
    break
  fi
done

if [[ -z "$selected_file" ]]; then
  echo "No matching file found for upload!"
  exit 1
fi

curl -X POST -H "Authorization: token $GITHUB_TOKEN" \
            -H "Content-Type: application/octet-stream" \
            --data-binary @"$selected_file" \
            "$upload_url?name=$("rv_$selected_file")"

# Upload the selected file
curl -X POST -H "Authorization: token $GITHUB_TOKEN" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @"$selected_file" \
  "$upload_url?name=$(basename "$selected_file")"

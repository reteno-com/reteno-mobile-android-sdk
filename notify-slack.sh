curl -s -X POST --data-urlencode "payload={
      \"channel\": \"android_builds\",
      \"username\": \"GitLab Android\",
      \"icon_url\": \"https://about.gitlab.com/images/press/logo/png/gitlab-icon-rgb.png\",
      \"attachments\": [{
      \"author_name\": \"$GITLAB_USER_EMAIL\",
      \"color\": \"good\",
      \"text\": \"$1\"
  }]}" $ANDROID_SLACK_WEBHOOK > /dev/null
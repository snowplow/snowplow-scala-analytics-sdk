#!/bin/bash

# This script is used for initializing the gh-pages branch

PROJECT_NAME=snowplow-scala-analytics-sdk
PROJECT_REPO=git@github.com:snowplow/snowplow-scala-analytics-sdk.git

# Using a fresh, temporary clone is safest for this procedure
pushd /tmp
git clone $PROJECT_REPO
cd $PROJECT_NAME

# Create branch with no history or content
git checkout --orphan gh-pages
git rm -rf .

# Create index.html file in order to redirect main page
# to /latest/api
cat > index.html <<- EOM
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Project Documentation</title>
    <script language="JavaScript">
    <!--
    function doRedirect()
    {
        window.location.replace("latest/api");
    }

    doRedirect();
    //-->
    </script>
</head>
<body>
<a href="latest/api">Go to the project documentation
</a>
</body>
</html>
EOM

git add index.html

# Establish the branch existence
git commit --allow-empty -m "Initialize gh-pages branch"
git push origin gh-pages

# Return to original working copy clone, we're finished with the /tmp one
popd
rm -rf /tmp/$PROJECT_NAME

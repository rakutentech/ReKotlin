#!/usr/bin/env bash

# install brew (if necessary)
which brew &> /dev/null
if [ $? -ne 0 ]; then
    echo "🍺 installing homebrew"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# install ktlint (if necessary)
which ktlint &> /dev/null
if [ $? -ne 0 ]; then
    echo "⚙️  installing $tool..."
    brew install $tool
fi

# install git pre commit hook
echo "🎣 installing pre commit hook..."
hook='.git/hooks/pre-commit'

if [[ -f $hook ]];then
    echo "$hook exists, making a backup"
    mv $hook "$hook.$(date +'%y-%m-%d %H:%M').bak"
fi
cp githooks/precommit $hook
